package com.dias.services.reports.export;

import com.dias.services.reports.dto.reports.ReportDTO;
import com.dias.services.reports.query.NoGroupByQueryBuilder;
import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.Calculation;
import com.dias.services.reports.report.query.Condition;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import com.dias.services.reports.service.ReportService;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.dias.services.reports.translation.Translator;
import com.dias.services.reports.utils.ExcelExportUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.charts.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private static final String FORMAT_DATE = "dd/mm/yyyy";
    private static final String FORMAT_TIME = "hh:mm;@";
    private static final short HEADER_HEIGHT = (short) 1200;
    private static final short GROUP_HEIGHT = (short) 500;
    private static final short TOTAL_HEIGHT = (short) 400;
    private static final int START_COLUMN_INDEX = 1;
    private static final String DATA_SHEET_DEFAULT_NAME = "Данные";
    private static final int CHART_WIDTH = 15;
    private static final int CHART_HEIGHT = 20;
    private static final Map<String, String> FUNCTIONS_MAP;
    public static final String ROW_NUMBER_TITLE = "№";

    static {
        FUNCTIONS_MAP = new HashMap<>();
        FUNCTIONS_MAP.put("max", "max");
        FUNCTIONS_MAP.put("min", "min");
        FUNCTIONS_MAP.put("sum", "sum");
        FUNCTIONS_MAP.put("avg", "average");
    }

    private final ReportService tablesService;
    private final Translator translator;
    private XSSFWorkbook workbook;
    private XSSFCellStyle defaultStyle;
    private XSSFCellStyle bigDecimalStyle;
    private XSSFCellStyle bigDecimalTotalStyle;
    private XSSFCellStyle titleStyle;
    private XSSFCellStyle groupStyle;
    private XSSFCellStyle dateStyle;
    private XSSFCellStyle timeStyle;
    private XSSFCellStyle totalHeaderStyle;
    private XSSFCellStyle headerStyle;
    private XSSFCellStyle paramsLabelStyle;
    private XSSFCellStyle paramsDetailsStyle;
    private XSSFCellStyle totalValueStyle;
    private XSSFCellStyle groupLastCellStyle;
    private XSSFCellStyle groupFirstCellStyle;

    public ReportExcelWriter(ReportService tablesService, Translator translator) {
        super();
        this.tablesService = tablesService;
        this.translator = translator;
    }

    protected XSSFWorkbook getWorkbook() {
        return workbook;
    }

    public void writeExcel(ReportDTO report, ResultSetWithTotal rs, OutputStream out) throws IOException {
        init();
        ReportType repType = ReportType.byNameOrDefaultForUnknown(report.getType());
        XSSFSheet sheet = workbook.createSheet(ReportType.table == repType ? report.getName() : DATA_SHEET_DEFAULT_NAME);
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
            Map<String, Integer> columnMap = getColumnMap(START_COLUMN_INDEX + 1 + (rs.containsTotal() ? 1 : 0), rs);
            if (ReportType.hbar == repType) {
                addBarChartWithType(STBarDir.BAR,
                        sheet,
                        firstRowWithData,
                        rs.getRows().size(),
                        columnMap,
                        chartDescriptor,
                        report);
            } else if (ReportType.bar == repType) {
                addBarChartWithType(STBarDir.COL,
                        sheet,
                        firstRowWithData,
                        rs.getRows().size(),
                        columnMap,
                        chartDescriptor,
                        report);
            } else {
                addLineChart(sheet,
                        firstRowWithData,
                        rs.getRows().size(),
                        columnMap,
                        chartDescriptor,
                        report);
            }
        }
    }

    private int writeTableReport(ReportDTO report, ResultSetWithTotal rs, XSSFSheet sheet) {

        int firstDataRowIndex;
        int numberOfColumns = rs.getHeaders().size();
        sheet.setColumnWidth(0, INITIAL_COLUMN);
        IntStream.range(START_COLUMN_INDEX, START_COLUMN_INDEX + 1 + numberOfColumns + (rs.containsTotal() ? 1 : 0)).forEach(i -> sheet.setColumnWidth(i, COLUMN_WIDTH));

        //первая колонка будет содержать номер строки
        sheet.setColumnWidth(START_COLUMN_INDEX + (rs.containsTotal() ? 1 : 0), N_COLUMN);

        int rowNum = writeTitle(report, sheet, 0);
        rowNum = writeParameters(report, sheet, rowNum);
        rowNum = writeHeaders(rs, sheet, rowNum);
        firstDataRowIndex = rowNum;
        rowNum = writeRows(rs, sheet, rowNum);
        writeTotal(report, rs, sheet, numberOfColumns, rowNum, firstDataRowIndex);
        return firstDataRowIndex;
    }

    private void addBarChartWithType(
            STBarDir.Enum barChartType,
            XSSFSheet sheet,
            int firstDataRow,
            int rowsNumber,
            Map<String, Integer> columnMap,
            ChartDescriptor chartDescriptor,
            ReportDTO report) {

        int xColumn = columnMap.get(chartDescriptor.getAxisXColumn());
        String xColumnName = CellReference.convertNumToColString(xColumn);
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        String title = chartDescriptor.getTitle();

        XSSFSheet dataSheet = workbook.createSheet(title != null ? title : report.getTitle());

        XSSFDrawing drawing = dataSheet.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 0, CHART_WIDTH, CHART_HEIGHT);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);

        CTChart ctChart = chart.getCTChart();
        CTPlotArea ctPlotArea = ctChart.getPlotArea();
        CTBarChart ctBarChart = ctPlotArea.addNewBarChart();
        ctBarChart.addNewBarDir().setVal(barChartType);
        ctBarChart.addNewVaryColors().setVal(false);

        for (int i = 0; i < series.size(); i++) {
            ChartDescriptor.Series s = series.get(i);
            CTBarSer ctBarSer = ctBarChart.addNewSer();
            ctBarSer.addNewIdx().setVal(i);
            CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
            CTStrRef ctStrRef = cttAxDataSource.addNewStrRef();
            int from = s.getStartRow() != null && s.getStartRow() > 0 ? firstDataRow + s.getStartRow() : firstDataRow + 1;
            int to = s.getEndRow() != null && s.getEndRow() > 0 ? firstDataRow + s.getEndRow() : firstDataRow + rowsNumber;
            ctStrRef.setF(sheet.getSheetName() + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to);
            CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
            CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
            int valueColumnIndex = columnMap.get(s.getValueColumn());
            String valueColumnName = CellReference.convertNumToColString(valueColumnIndex);
            ctNumRef.setF(sheet.getSheetName() + "!$" + valueColumnName + "$" + from + ":$" + valueColumnName + "$" + to);
        }

        //telling the BarChart that it has axes and giving them Ids
        ctBarChart.addNewAxId().setVal(1);
        ctBarChart.addNewAxId().setVal(2);

        //cat axis
        CTCatAx ctCatAx = ctPlotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(1); //id of the cat axis
        CTScaling ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctCatAx.addNewDelete().setVal(false);
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(2); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);


        //val axis
        CTValAx ctValAx = ctPlotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(2); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.L);
        ctValAx.addNewCrossAx().setVal(1); //id of the cat axis
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //legend
        if (chartDescriptor.getShowLegend()) {
            CTLegend ctLegend = ctChart.addNewLegend();
            ctLegend.addNewLegendPos().setVal(STLegendPos.R);
            ctLegend.addNewOverlay().setVal(false);
        }

        nameAxis(chartDescriptor, chart);

        // line style of the series
        for (int i = 0; i < series.size(); i++) {
            java.awt.Color color = series.get(i).getAwtColor();
            if (color != null) {
                CTShapeProperties seriesShapeProperties = chart.getCTChart().getPlotArea().getBarChartArray(0).getSerArray(i).addNewSpPr();
                seriesShapeProperties.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
            }
        }

    }

    private void nameAxis(ChartDescriptor chartDescriptor, XSSFChart chart) {
        if (!StringUtils.isEmpty(chartDescriptor.getAxisYTitle())) {
            setValuesAxisTitle(chart, chartDescriptor.getAxisYTitle());
        }

        if (!StringUtils.isEmpty(chartDescriptor.getAxisXTitle())) {
            setCatAxisTitle(chart, chartDescriptor.getAxisXTitle());
        }
    }

    private static Map<String, Integer> getColumnMap(int startColumn, ResultSetWithTotal rs) {
        Map<String, Integer> result = new HashMap<>();
        List<ColumnWithType> headers = rs.getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            ColumnWithType columnWithType = headers.get(i);
            String column = columnWithType.getColumn();
            result.put(column, startColumn + i);
            result.put(columnWithType.getTitle(), startColumn + i);
            if (column.contains(".")) {
                result.put(column.substring(column.indexOf(".") + 1), startColumn + i);
            }
        }
        return result;
    }

    private void addLineChart(XSSFSheet sheet,
                              int firstDataRow,
                              int rowsNumber,
                              Map<String, Integer> columnMap,
                              ChartDescriptor chartDescriptor,
                              ReportDTO report) {

        int xColumn = columnMap.get(chartDescriptor.getAxisXColumn());
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        String title = chartDescriptor.getTitle();

        XSSFSheet dataSheet = workbook.createSheet(title != null ? title : report.getTitle());

        XSSFDrawing drawing = dataSheet.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 0, CHART_WIDTH, CHART_HEIGHT);

        XSSFChart chart = drawing.createChart(anchor);

        chart.setTitleText(title);

        ChartLegend legend = chart.getOrCreateLegend();
        legend.setPosition(LegendPosition.RIGHT);

        LineChartData data = chart.getChartDataFactory().createLineChartData();


        ChartAxis bottomAxis = chart.getChartAxisFactory().createCategoryAxis(AxisPosition.BOTTOM);
        ValueAxis leftAxis = chart.getChartAxisFactory().createValueAxis(AxisPosition.LEFT);


        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);


        List<Integer> startRows = series.stream().map(ChartDescriptor.Series::getStartRow).collect(Collectors.toList());
        List<Integer> endRows = series.stream().map(ChartDescriptor.Series::getEndRow).collect(Collectors.toList());
        startRows = startRows.stream().map(integer -> integer != null ? integer : 0).collect(Collectors.toList());
        endRows = endRows.stream().map(integer -> integer != null ? integer : 0).collect(Collectors.toList());

        Integer minStartRow = startRows.stream().min(Comparator.comparingInt(o -> o)).get();
        Integer maxEndRow = endRows.stream().max(Comparator.comparingInt(o -> o)).get();

        maxEndRow = maxEndRow > 0 ? Math.min(maxEndRow, rowsNumber): rowsNumber;



        ChartDataSource<Number> xs = DataSources.fromNumericCellRange(sheet, new CellRangeAddress(firstDataRow + minStartRow - 1, firstDataRow + maxEndRow - 1, xColumn, xColumn));
        for (ChartDescriptor.Series s : series) {
            int from = s.getStartRow() != null && s.getStartRow() > 0 ? firstDataRow + s.getStartRow() - 1 : firstDataRow;
            int to = s.getEndRow() != null && s.getEndRow() > 0 ? firstDataRow + s.getEndRow() - 1 : firstDataRow + rowsNumber - 1;
            Integer yColumn = columnMap.get(s.getValueColumn());
            ChartDataSource<Number> ys = DataSources.fromNumericCellRange(sheet, new CellRangeAddress(from, to, yColumn, yColumn));
            LineChartSeries lineChartSeries = data.addSeries(xs, ys);
            lineChartSeries.setTitle(s.getTitle());
        }
        chart.plot(data, bottomAxis, leftAxis);

        nameAxis(chartDescriptor, chart);

        // line style of the series
        for (int i = 0; i < series.size(); i++) {
            java.awt.Color color = series.get(i).getAwtColor();
            if (color != null) {
                CTShapeProperties seriesShapeProperties = chart.getCTChart().getPlotArea().getLineChartArray(0).getSerArray(i).addNewSpPr();
                seriesShapeProperties.addNewLn();
                seriesShapeProperties.getLn().setW(Units.pixelToEMU(3));
                seriesShapeProperties.getLn().addNewSolidFill();
                seriesShapeProperties.getLn().getSolidFill().addNewSrgbClr();
                seriesShapeProperties.getLn().getSolidFill().getSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
            }
        }

    }

    private void setValuesAxisTitle(XSSFChart chart, String title) {

        CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        CTValAx valAx = plotArea.getValAxArray(0);
        CTTitle ctTitle = valAx.addNewTitle();
        ctTitle.addNewLayout();
        ctTitle.addNewOverlay().setVal(false);
        CTTextBody rich = ctTitle.addNewTx().addNewRich();
        rich.addNewBodyPr();
        rich.addNewLstStyle();
        CTTextParagraph p = rich.addNewP();
        p.addNewPPr().addNewDefRPr();
        p.addNewR().setT(title);
        p.addNewEndParaRPr();
    }

    private void setCatAxisTitle(XSSFChart chart, String title) {
        CTCatAx valAx = chart.getCTChart().getPlotArea().getCatAxArray(0);
        CTTitle ctTitle = valAx.addNewTitle();
        ctTitle.addNewLayout();
        ctTitle.addNewOverlay().setVal(false);
        CTTextBody rich = ctTitle.addNewTx().addNewRich();
        rich.addNewBodyPr();
        rich.addNewLstStyle();
        CTTextParagraph p = rich.addNewP();
        p.addNewPPr().addNewDefRPr();
        p.addNewR().setT(title);
        p.addNewEndParaRPr();
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

    private int writeHeaders(ResultSetWithTotal rs, XSSFSheet sheet, int rowNum) {
        XSSFRow row = sheet.createRow(rowNum++);
        row.setHeight(HEADER_HEIGHT);
        List<ColumnWithType> headers = rs.getHeaders();
        int cellNum = START_COLUMN_INDEX + (rs.containsTotal() ? 1 : 0);
        Cell cell = createCell(row, cellNum++, headerStyle, ROW_NUMBER_TITLE);
        addMediumBordersToStyle(cell.getCellStyle());
        for(ColumnWithType header : headers) {
            cell = createCell(row, cellNum++, headerStyle, StringUtils.isEmpty(header.getTitle()) ? header.getColumn() : header.getTitle());
            addMediumBordersToStyle(cell.getCellStyle());
        }
        return rowNum;
    }

    private int writeRows(ResultSetWithTotal rs, XSSFSheet sheet, int rowNum) {
        List<Integer> groupRowsIndexes = rs.getGroupRowsIndexes();
        List<List<Object>> rows = rs.getRows();
        for (int i = 0; i < rows.size(); i++) {
            List<Object> r = rows.get(i);
            XSSFRow xlsRow = sheet.createRow(rowNum++);
            int dataRowCellNum = START_COLUMN_INDEX + (rs.containsTotal() ? 1 : 0);
            //выводим номер строки
            writeObject(xlsRow, dataRowCellNum++, i + 1, defaultStyle);

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
                }
                writeObject(xlsRow, dataRowCellNum++, data, style);
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

    private void writeObject(XSSFRow row, int cellNum, Object value, CellStyle cellStyle) {
        String cellValue = value != null ? value.toString() : "";
        Cell cell = createCell(row, cellNum, cellStyle, cellValue);
        if (value != null && Number.class.isAssignableFrom(value.getClass())) {
            cell.setCellType(CellType.NUMERIC);
            if (value instanceof Integer) {
                cell.setCellValue((Integer)value);
            } else {
                cell.setCellValue(Double.valueOf(cellValue));
            }
        }

    }

    private void init() {
        this.workbook = new XSSFWorkbook();

        XSSFFont defaultFont = createFont(workbook, (short) 10, FONT_ARIAL);

        XSSFFont boldItalicFont = createFont(workbook, (short) 12, FONT_TIMES_NEW_ROMAN, true);
        boldItalicFont.setItalic(true);

        XSSFDataFormat dataFormat = workbook.createDataFormat();

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


        this.dateStyle = createCellStyle(workbook, defaultFont, dataFormat.getFormat(FORMAT_DATE));
        this.timeStyle = createCellStyle(workbook, defaultFont, dataFormat.getFormat(FORMAT_TIME));
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
