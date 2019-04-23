package com.dias.services.reports.export;

import com.dias.services.reports.dto.reports.ReportDTO;
import com.dias.services.reports.query.NoGroupByQueryBuilder;
import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.Calculation;
import com.dias.services.reports.report.query.Column;
import com.dias.services.reports.report.query.Condition;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import com.dias.services.reports.service.ReportService;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.dias.services.reports.translation.Translator;
import com.dias.services.reports.utils.ExcelExportUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.IntStream;

import static com.dias.services.reports.utils.ExcelExportUtils.createCell;
import static com.dias.services.reports.utils.ExcelExportUtils.createFont;

public class ReportExcelWriter {

    private static final int INITIAL_COLUMN = 200;
    private static final int N_COLUMN = 2000;
    private static final int COLUMN_WIDTH = 6000;
    private static final String TOTAL_LABEL = "Итого:";
    private static final String TOTAL_FORMULA_TEMPLATE = "%s(%s:%s)";
    private static final String PARAMETERS_LABEL = "Параметры:";
    private static final String FONT_ARIAL = "Arial";
    private static final String FONT_TIMES_NEW_ROMAN = "Times New Roman";
    private static final String FORMAT_DECIMAL = "#,##0.00";

    private static final short HEADER_HEIGHT = (short) 1200;
    private static final short GROUP_HEIGHT = (short) 500;
    private static final short TOTAL_HEIGHT = (short) 400;
    private static final int START_COLUMN_INDEX = 1;
    private static final String DATA_SHEET_DEFAULT_NAME = "Данные";
    private static final Map<String, String> FUNCTIONS_MAP;
    private static final String ROW_NUMBER_TITLE = "№";

    static {
        FUNCTIONS_MAP = new HashMap<>();
        FUNCTIONS_MAP.put("max", "max");
        FUNCTIONS_MAP.put("min", "min");
        FUNCTIONS_MAP.put("sum", "sum");
        FUNCTIONS_MAP.put("avg", "average");
    }

    private final ReportService tablesService;
    private final Translator translator;
    private final String nullSymbol;

    private XSSFWorkbook workbook;
    private XSSFCellStyle defaultStyle;
    private XSSFCellStyle bigDecimalStyle;
    private XSSFCellStyle bigDecimalTotalStyle;
    private XSSFCellStyle titleStyle;
    private XSSFCellStyle groupStyle;
    private XSSFCellStyle totalHeaderStyle;
    private XSSFCellStyle headerStyle;
    private XSSFCellStyle paramsLabelStyle;
    private XSSFCellStyle paramsDetailsStyle;
    private XSSFCellStyle totalValueStyle;
    private XSSFCellStyle groupLastCellStyle;
    private XSSFCellStyle groupFirstCellStyle;

    private XSSFDataFormat dataFormat;
    private XSSFFont defaultFont;
    private XSSFSheet sheet;
    private int titleEndRowIndex;
    private int parametersEndRowIndex;
    private int headersEndRowIndex;
    private int rowsEndRowIndex;

    public ReportExcelWriter(ReportService tablesService, Translator translator, String nullSymbol) {
        this.tablesService = tablesService;
        this.translator = translator;
        this.nullSymbol = nullSymbol;
    }

    protected XSSFWorkbook getWorkbook() {
        return workbook;
    }

    public void writeExcel(ReportDTO report, ResultSetWithTotal rs, OutputStream out) throws IOException {
        init();
        ReportType repType = ReportType.byNameOrDefaultForUnknown(report.getType());
        sheet = workbook.createSheet(ReportType.table == repType ? report.getName() : DATA_SHEET_DEFAULT_NAME);
        boolean isChart = ReportType.table != repType;
        if (!isChart) {
            //преобразуем резалтсет в резалтсет с группировками только
            //в случае типа отчета = таблица
            //в противном случае собьются индексы данных
            //для вывода серий
            rs = rs.convertToGroupped(report.getQueryDescriptor().getGroupBy(), report.getQueryDescriptor().getOrderBy());
        }
        int firstRowWithData = writeTableReport(report, rs, sheet);
        if (isChart) {
            writeChartReport(report, rs, sheet, repType, firstRowWithData);
        }
        workbook.write(out);
        out.close();
    }

    private void writeChartReport(ReportDTO report, ResultSetWithTotal rs, XSSFSheet sheet, ReportType repType, int firstRowWithData) throws IOException {
        ChartDescriptor chartDescriptor = tablesService.extractChartDescriptor(report);
        if (chartDescriptor != null) {
            int excelStartColumn = START_COLUMN_INDEX + 1 + (rs.containsTotal() ? 1 : 0);
            Map<String, Integer> excelColumnsMap = getColumnMap(excelStartColumn, rs);
            ExcelChartsFactory.addChartToWorkbook(workbook, chartDescriptor, report, repType, firstRowWithData, rs, sheet, excelColumnsMap, this);
        }
    }

    private int writeTableReport(ReportDTO report, ResultSetWithTotal rs, XSSFSheet sheet) {

        int firstDataRowIndex;
        int numberOfColumns = rs.getHeaders().size();
        sheet.setColumnWidth(0, INITIAL_COLUMN);
        IntStream.range(START_COLUMN_INDEX, START_COLUMN_INDEX + 1 + numberOfColumns + (rs.containsTotal() ? 1 : 0)).forEach(i -> sheet.setColumnWidth(i, COLUMN_WIDTH));

        //первая колонка будет содержать номер строки
        sheet.setColumnWidth(START_COLUMN_INDEX + (rs.containsTotal() ? 1 : 0), N_COLUMN);

        titleEndRowIndex = writeTitle(report, sheet, 0);
        parametersEndRowIndex = writeParameters(report, sheet, titleEndRowIndex);
        headersEndRowIndex = writeHeaders(rs, sheet, parametersEndRowIndex, 0, false);
        firstDataRowIndex = headersEndRowIndex;
        rowsEndRowIndex = writeRows(rs, sheet, headersEndRowIndex, 0, false);
        writeTotal(report, rs, sheet, numberOfColumns, rowsEndRowIndex, firstDataRowIndex);
        return firstDataRowIndex;
    }

    public int joinTable(ResultSetWithTotal initialResultSet, ResultSetWithTotal rs) {

        int firstDataRowIndex;
        int numberOfInitialColumns = initialResultSet.getHeaders().size();
        int numberOfNewColumns = rs.getHeaders().size();
        int firstColumnOfNewData = START_COLUMN_INDEX + 1 + numberOfInitialColumns + (initialResultSet.containsTotal() ? 1 : 0);

        IntStream.range(firstColumnOfNewData, firstColumnOfNewData + numberOfNewColumns).forEach(i -> {
            sheet.setColumnWidth(i, COLUMN_WIDTH);
        });

        //первая колонка будет содержать номер строки
        firstDataRowIndex = writeHeaders(rs, sheet, parametersEndRowIndex, START_COLUMN_INDEX + numberOfInitialColumns + 1, true);
        writeRows(rs, sheet, firstDataRowIndex, START_COLUMN_INDEX + numberOfInitialColumns + 1, true);
        return firstDataRowIndex;
    }

    private static Map<String, Integer> getColumnMap(int startColumn, ResultSetWithTotal rs) {
        Map<String, Integer> result = new HashMap<>();
        List<ColumnWithType> headers = rs.getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            ColumnWithType columnWithType = headers.get(i);
            Column column1 = new Column(columnWithType.getColumn());
            String column = column1.getColumnName();
            result.put(column, startColumn + i);
            result.put(columnWithType.getTitle(), startColumn + i);
            result.put(column1.toSQL(), startColumn + i);
        }
        return result;
    }

    private void writeTotal(ReportDTO report, ResultSetWithTotal rs, XSSFSheet sheet, int numberOfColumns, int rowNum, int titleAndHeadersHeight) {
        Calculation[] calculations = report.getQueryDescriptor().getAggregations();

        if (calculations != null && calculations.length > 0) {
            Map<String, Integer> columnsMap = rs.getColumnsMap();
            XSSFRow row = sheet.createRow(rowNum++);
            row.setHeight(TOTAL_HEIGHT);
            if ((numberOfColumns - calculations.length) >= 0) {
                createCell(row, START_COLUMN_INDEX, totalHeaderStyle, TOTAL_LABEL);
            }

            if (!rs.getRows().isEmpty()) {
                int dataStartsFrom = START_COLUMN_INDEX + 1 + 1; //добавляем одну колонку для вывода номеров строк и вторую колонку для строки ИТОГО
                for (Calculation calculation : calculations) {
                    Integer colIndexInRs = columnsMap.get(calculation.getTitle());
                    Cell cell = createCell(row, dataStartsFrom + colIndexInRs, totalValueStyle);
                    cell.setCellType(CellType.FORMULA);
                    String wantedRef = (new CellReference(cell)).formatAsString();
                    String cellName = wantedRef.substring(0, wantedRef.indexOf(Integer.toString(rowNum)));
                    String cellFirst = cellName + (titleAndHeadersHeight + 1);
                    String cellLast = cellName + (rowNum - 1);
                    cell.setCellFormula(String.format(TOTAL_FORMULA_TEMPLATE, FUNCTIONS_MAP.get(calculation.getFunction().toLowerCase()), cellFirst, cellLast));
                }
            }
        }
    }

    private int writeHeaders(ResultSetWithTotal rs, XSSFSheet sheet, int rowNum, int shift, boolean rowsCreated) {
        XSSFRow row = rowsCreated ? sheet.getRow(rowNum++) : sheet.createRow(rowNum++);
        row.setHeight(HEADER_HEIGHT);
        List<ColumnWithType> headers = rs.getHeaders();
        int cellNum = START_COLUMN_INDEX + (rs.containsTotal() ? 1 : 0) + shift;

        if (!rowsCreated) {
            Cell cell = createCell(row, cellNum++, headerStyle, ROW_NUMBER_TITLE);
            addMediumBordersToStyle(cell.getCellStyle());
        }

        for(ColumnWithType header : headers) {
            Cell cell = createCell(row, cellNum++, headerStyle, StringUtils.isEmpty(header.getTitle()) ? header.getColumn() : header.getTitle());
            addMediumBordersToStyle(cell.getCellStyle());
        }
        return rowNum;
    }

    private int writeRows(ResultSetWithTotal rs, XSSFSheet sheet, int rowNum, int shift, boolean rowsCreated) {
        List<Integer> groupRowsIndexes = rs.getGroupRowsIndexes();
        List<Integer> dateTypeColumnsIndexes = rs.getDateColumnsIndexes();
        List<Integer> numericTypeColumnsIndexes = rs.getNumericColumnsIndexes();
        List<List<Object>> rows = rs.getRows();
        String dateFormatPattern = null;
        for (int i = 0; i < rows.size(); i++) {
            List<Object> r = rows.get(i);
            XSSFRow xlsRow = rowsCreated ? sheet.getRow(rowNum++) : sheet.createRow(rowNum++);
            int dataRowCellNum = START_COLUMN_INDEX + (rs.containsTotal() ? 1 : 0) + shift;

            if (!rowsCreated) {
                //выводим номер строки
                writeNumericOrStringCell(xlsRow, dataRowCellNum++, i + 1, defaultStyle, true);
            }

            //определим стиль вывода данных
            //в случае выводы группы - используем специальный стиль
            XSSFCellStyle style = defaultStyle;
            boolean isGroup = groupRowsIndexes != null && groupRowsIndexes.remove(Integer.valueOf(i));

            if (isGroup) {
                xlsRow.setHeight(GROUP_HEIGHT);
            }

            for (int j = 0; j < r.size(); j++) {
                Object data = r.get(j);
                if (isGroup) {
                    style = groupStyle;
                    if (j == r.size() - 1) {
                        style = groupLastCellStyle;
                    } else if (j == 0) {
                        style = groupFirstCellStyle;
                    }
                    writeNumericOrStringCell(xlsRow, dataRowCellNum++, data, style, false);
                } else {
                    boolean isDate = dateTypeColumnsIndexes.indexOf(j) >= 0;
                    boolean isNumeric = !isDate && numericTypeColumnsIndexes.indexOf(j) >= 0;
                    if (isDate) {
                        if (dateFormatPattern == null && data != null) {
                            dateFormatPattern = ExportChartsHelper.calculateDateFormatPattern(data.toString());
                        }
                        if (dateFormatPattern != null) {
                            writeDateValue(xlsRow, dataRowCellNum++, data, style, dateFormatPattern);
                        } else {
                            writeNumericOrStringCell(xlsRow, dataRowCellNum++, data, style, false);
                        }
                    } else {
                        writeNumericOrStringCell(xlsRow, dataRowCellNum++, data, style, isNumeric);
                    }

                }

            }

        }
        return rowNum;
    }

    /**
     * Вывод условий отчета
     *
     * @param report
     * @param sheet
     * @param rowNum
     * @return Количество строк в условиях
     */
    private int writeParameters(ReportDTO report, XSSFSheet sheet, int rowNum) {
        Condition[] conditions = report.getQueryDescriptor().getWhere();
        if (conditions != null) {
            String strConditions = NoGroupByQueryBuilder.buildWhereStatement(conditions, tablesService.getTablesColumnTypesMap(report.getQueryDescriptor()), true, translator);
            String[] parts = strConditions.split("\n");
            boolean labelCreated = false;
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].trim().isEmpty()) {
                    XSSFRow row = sheet.createRow(rowNum++);
                    if (!labelCreated) {
                        createCell(row, START_COLUMN_INDEX, paramsLabelStyle, PARAMETERS_LABEL);
                        labelCreated = true;
                    }
                    createCell(row, START_COLUMN_INDEX + 1, paramsDetailsStyle, parts[i]);
                }
            }
            sheet.createRow(rowNum++);
        }
        return rowNum;
    }

    /**
     * Вывод заголовка отчета
     *
     * @param report
     * @param sheet
     * @param rowNumber
     * @return Количество строк в заголовке
     */
    private int writeTitle(ReportDTO report, XSSFSheet sheet, int rowNumber) {
        sheet.createRow(rowNumber++);
        XSSFRow row = sheet.createRow(rowNumber++);
        createCell(row, START_COLUMN_INDEX, titleStyle, report.getTitle());
        row.setHeight(HEADER_HEIGHT);
        sheet.createRow(rowNumber++);
        return rowNumber;
    }

    private void writeNumericOrStringCell(XSSFRow row, int cellNum, Object value, CellStyle cellStyle, boolean isNumeric) {
        String stringValue = value != null ? value.toString() : nullSymbol;
        Cell cell = createCell(row, cellNum, cellStyle, stringValue);
        if (isNumeric) {
            cell.setCellType(CellType.NUMERIC);
            if (value != null) {
                if (value instanceof Integer) {
                    cell.setCellValue((Integer) value);
                } else {
                    cell.setCellValue(Double.valueOf(stringValue));
                }
            } else {
                // необходимо явно задать значение, иначе не получим ожидаемого
                cell.setCellValue(nullSymbol);
                // также зададим стиль для выравнивания по правому краю
                cell.setCellStyle(bigDecimalStyle);
            }
        }
    }

    private void writeDateValue(XSSFRow row, int cellNum, Object value, CellStyle defaultStyle, String dateFormatPattern) {
        String cellValue = value != null ? value.toString() : "";
        Cell cell = createCell(row, cellNum, defaultStyle, cellValue);
        if (value != null && value instanceof LocalDateTime) {
            // данные из базы приходят в формате LocalDateTime
            // POI же работает со строковыми типами, Календарем и с Date из util
            // наша задача преобразовать в Календарь
            long timemills = ((LocalDateTime) value).toInstant(ZoneOffset.UTC).toEpochMilli();
            Calendar cal = new GregorianCalendar();
            cal.setTimeInMillis(timemills);
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
            cell.setCellValue(cal);
        }


        if (dateFormatPattern == null) {
            dateFormatPattern = ExportChartsHelper.calculateDateFormatPattern(cellValue);
        }
        if (dateFormatPattern != null) {
            XSSFCellStyle style = createCellStyle(workbook, defaultFont, dataFormat.getFormat(dateFormatPattern));
            addThinBordersToStyle(style);
            cell.setCellStyle(style);
        }
    }

    private void init() {
        this.workbook = new XSSFWorkbook();

        this.defaultFont = createFont(workbook, (short) 10, FONT_ARIAL);

        XSSFFont boldItalicFont = createFont(workbook, (short) 12, FONT_TIMES_NEW_ROMAN, true);
        boldItalicFont.setItalic(true);

        this.dataFormat = workbook.createDataFormat();

        this.defaultStyle = createCellStyle(workbook, defaultFont);
        addThinBordersToStyle(this.defaultStyle);

        this.paramsDetailsStyle = createCellStyle(workbook, defaultFont);

        this.paramsLabelStyle = createCellStyle(workbook, defaultFont);
        this.paramsLabelStyle.setAlignment(HorizontalAlignment.RIGHT);


        this.bigDecimalStyle = createCellStyle(workbook, defaultFont, dataFormat.getFormat(FORMAT_DECIMAL),  HorizontalAlignment.RIGHT);
        this.bigDecimalTotalStyle = createCellStyle(workbook, createFont(workbook, (short) 10, FONT_ARIAL, true), dataFormat.getFormat(FORMAT_DECIMAL),  HorizontalAlignment.RIGHT);
        addThinBordersToStyle(this.bigDecimalStyle);
        addThinBordersToStyle(this.bigDecimalTotalStyle);


        XSSFFont titleFont = createFont(workbook, (short) 24, FONT_TIMES_NEW_ROMAN, true);
        titleFont.setItalic(true);
        this.titleStyle = createCellStyle(workbook, titleFont);
        this.titleStyle.setVerticalAlignment(VerticalAlignment.BOTTOM);

        XSSFFont headerFont = createFont(workbook, (short) 16, FONT_TIMES_NEW_ROMAN, true);
        this.headerStyle = createCellStyle(workbook, headerFont);
        this.headerStyle.setAlignment(HorizontalAlignment.CENTER);
        this.headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.headerStyle.setWrapText(true);

        XSSFFont groupFont = createFont(workbook, (short) 16, FONT_TIMES_NEW_ROMAN, true);
        groupFont.setItalic(true);
        this.groupStyle = createCellStyle(workbook, groupFont);
        this.groupStyle.setAlignment(HorizontalAlignment.LEFT);
        this.groupStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.groupStyle.setWrapText(false);
        this.groupStyle.setBorderBottom(BorderStyle.THIN);
        this.groupStyle.setBorderTop(BorderStyle.THIN);

        this.groupLastCellStyle = createCellStyle(workbook, groupFont);
        this.groupLastCellStyle.setBorderTop(BorderStyle.THIN);
        this.groupLastCellStyle.setBorderBottom(BorderStyle.THIN);
        this.groupLastCellStyle.setBorderRight(BorderStyle.THIN);

        this.groupFirstCellStyle = createCellStyle(workbook, groupFont);
        this.groupFirstCellStyle.setBorderTop(BorderStyle.THIN);
        this.groupFirstCellStyle.setBorderBottom(BorderStyle.THIN);
        this.groupFirstCellStyle.setBorderLeft(BorderStyle.THIN);



        XSSFFont totalFont = createFont(workbook, (short) 16, FONT_TIMES_NEW_ROMAN, true);
        this.totalHeaderStyle = createCellStyle(workbook, totalFont);
        this.totalHeaderStyle.setAlignment(HorizontalAlignment.RIGHT);

        this.totalValueStyle = createCellStyle(workbook, headerFont);
        this.totalValueStyle.setAlignment(HorizontalAlignment.RIGHT);
        addMediumBordersToStyle(this.totalValueStyle);;
    }

    private void addThinBordersToStyle(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private void addMediumBordersToStyle(CellStyle style) {
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
    }

    private static XSSFCellStyle createCellStyle(XSSFWorkbook workbook, Font font) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private static XSSFCellStyle createCellStyle(XSSFWorkbook workbook, Font font, short format) {
        XSSFCellStyle style = createCellStyle(workbook, font);
        style.setDataFormat(format);
        return style;
    }

    private static XSSFCellStyle createCellStyle(XSSFWorkbook workbook, Font font, short format, HorizontalAlignment alignment) {
        XSSFCellStyle style = ExcelExportUtils.createCellStyle(workbook, font, format);
        style.setAlignment(alignment);
        return style;
    }

}
