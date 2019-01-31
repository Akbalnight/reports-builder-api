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
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dias.services.reports.utils.PdfExportUtils.*;

import java.util.List;

public class ReportPdfWriter {

    private static final int CHART_WIDTH = (int) PageSize.A4.getHeight() - 40;
    private static final int CHART_HEIGHT = (int) PageSize.A4.getWidth() - 40;
    private final ReportService tablesService;
    private final Translator translator;

    public ReportPdfWriter(ReportService tablesService, Translator translator){
        this.tablesService = tablesService;
        this.translator = translator;
    }

    public void writePdf(ReportDTO report, ResultSetWithTotal rs, OutputStream out) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();
        Calculation[] aggregations = report.getQueryDescriptor().getAggregations();
        boolean withSummary = aggregations != null && aggregations.length > 0;
        if (withSummary) {
            rs.getRows().add(rs.generateTableReadyTotalRow());
        }
        ReportType reportType = ReportType.byNameOrDefaultForUnknown(report.getType());
        if (ReportType.table == reportType) {
            writeTableReport(report, rs, document, withSummary);
        } else {
            writeChartReport(report, rs, document, withSummary, reportType);
        }
        document.close();

    }

    private void writeChartReport(ReportDTO report, ResultSetWithTotal rs, Document document, boolean withSummary, ReportType reportType) throws IOException, DocumentException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ChartDescriptor chartDescriptor = tablesService.extractChartDescriptor(report);
        if (chartDescriptor != null) {
            DefaultCategoryDataset defaultCategoryDataset = getDefaultCategoryDataset(rs, chartDescriptor, withSummary);
            JFreeChart chart;
            if (ReportType.bar == reportType) {
                chart = addColumnChart(chartDescriptor, defaultCategoryDataset);
                rotateCategoryLabelsIfNeeded(defaultCategoryDataset, chart);
            } else if (ReportType.hbar == reportType) {
                chart = addBarChart(chartDescriptor, defaultCategoryDataset);
            } else {
                chart = addLineChart(chartDescriptor, defaultCategoryDataset);
                rotateCategoryLabelsIfNeeded(defaultCategoryDataset, chart);
            }
            colorize(chart, chartDescriptor);
            setPositionToLegend(chart);
            ChartUtilities.writeChartAsPNG(os, chart, CHART_WIDTH, CHART_HEIGHT);
        }
        if (os.size() > 0) {
            Image image = Image.getInstance(os.toByteArray());
            image.setAlignment(Image.MIDDLE);
            document.add(image);
            os.close();
        }
    }

    private void writeTableReport(ReportDTO report, ResultSetWithTotal rs, Document document, boolean withSummary) throws DocumentException {
        writeHeader(document, report.getTitle());
        writeHeaderSpacing(document);
        writeParameters(report, document);
        //Основная часть отчёта
        int columns = rs.getHeaders().size();
        PdfPTable table = new PdfPTable(withSummary ? columns + 2 : columns + 1);
        table.setTotalWidth(PAGE_VISIBLE_WIDTH);
        table.setLockedWidth(true);
        writeTableHeader(rs, table, withSummary);
        writeTableBody(report, rs, table, withSummary);
        document.add(table);
    }

    private JFreeChart addColumnChart(ChartDescriptor chartDescriptor, DefaultCategoryDataset ds) throws IOException {

        return ChartFactory.createBarChart(
                chartDescriptor.getTitle(),
                chartDescriptor.getAxisXTitle(),
                chartDescriptor.getAxisYTitle(),
                ds,
                PlotOrientation.VERTICAL,
                chartDescriptor.getShowLegend(),
                false,
                false);
    }

    private void rotateCategoryLabelsIfNeeded(DefaultCategoryDataset ds, JFreeChart chart) {
        org.jfree.chart.axis.CategoryAxis domain = chart.getCategoryPlot().getDomainAxis();
        //если подписи к категориям не умещаются в ширину грфика, расположим подписи вертикально
        int lengthOfAllKeys = StringUtils.join(ds.getColumnKeys()).length();
        if (lengthOfAllKeys > CHART_WIDTH/NORMAL_FONT.getSize()) {
            domain.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        }
    }

    private JFreeChart addBarChart(ChartDescriptor chartDescriptor, DefaultCategoryDataset ds) throws IOException {

        JFreeChart chart = ChartFactory.createBarChart(
                chartDescriptor.getTitle(),
                chartDescriptor.getAxisXTitle(),
                chartDescriptor.getAxisYTitle(),
                ds,
                PlotOrientation.HORIZONTAL,
                chartDescriptor.getShowLegend(),
                false,
                false);
        chart.getCategoryPlot().setRangeAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);

        return chart;
    }

    private DefaultCategoryDataset getDefaultCategoryDataset(ResultSetWithTotal rs, ChartDescriptor chartDescriptor, boolean withSummary) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        List<List<Object>> rows = rs.getRows();
        Map<String, Integer> columnMap = rs.getColumnsMap();
        Integer categoryColumnIndex = columnMap.get(chartDescriptor.getAxisXColumn());
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        //последняя строка - итоговая в случае withSummary = true
        int sizeOfRows = withSummary ? rows.size() - 1 : rows.size();

        for (ChartDescriptor.Series s : series) {
            String valueColumn = s.getValueColumn();
            Integer seriesVaueIndex = columnMap.get(valueColumn);
            int from = s.getStartRow() != null ? s.getStartRow() - 1 : 0;
            int to = Math.min(s.getEndRow() != null && s.getEndRow() > 0 ? s.getEndRow(): sizeOfRows, sizeOfRows);
            if (from < sizeOfRows) {
                for (int i = from; i < to; i++) {
                    List<Object> row = rows.get(i);
                    Object categoryValue = row.get(categoryColumnIndex);
                    categoryValue = categoryValue == null ? "" : categoryValue;
                    Object value = row.get(seriesVaueIndex);
                    Number seriesValueNumber = value != null && Number.class.isAssignableFrom(value.getClass()) ? (Number) value : 0;
                    ds.addValue(seriesValueNumber, s.getTitle(), (Comparable) categoryValue);
                }
            }
        }
        return ds;
    }

    private JFreeChart addLineChart(ChartDescriptor chartDescriptor, DefaultCategoryDataset ds) throws IOException {

        JFreeChart chart = ChartFactory.createLineChart(
                chartDescriptor.getTitle(),
                chartDescriptor.getAxisXTitle(),
                chartDescriptor.getAxisYTitle(),
                ds,
                PlotOrientation.VERTICAL,
                chartDescriptor.getShowLegend(),
                false,
                false);

        return chart;

    }

    private void colorize(JFreeChart chart, ChartDescriptor chartDescriptor) {
        chart.getPlot().setBackgroundPaint(Color.white);
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        for (int i = 0; i < series.size(); i++) {
            ChartDescriptor.Series s = series.get(i);
            chart.getCategoryPlot().getRenderer().setSeriesPaint(i, new Color(Integer.parseInt( s.getColor().substring(1),16)));
        }

    }

    private void setPositionToLegend(JFreeChart chart) {
        LegendTitle legend = chart.getLegend();
        if (legend != null) {
            legend.setPosition(RectangleEdge.RIGHT);
        }
    }

    private void writeTableBody(ReportDTO report, ResultSetWithTotal rs, PdfPTable table, boolean withSummary) {

        List<List<Object>> rows = rs.getRows();
        List<ColumnWithType> headers = rs.getHeaders();
        int columns = headers.size();
        Calculation[] aggregations = report.getQueryDescriptor().getAggregations();
        List<Integer> calculatedColumnsIndicies = new ArrayList<>();
        if (aggregations != null) {
            for (int j = 0; j < aggregations.length; j++) {
                for (int i = 0; i < columns; i++) {
                    ColumnWithType column = headers.get(i);
                    if (Objects.equals(column.getColumn(),aggregations[j].getColumn()) || Objects.equals(column.getTitle(), aggregations[j].getTitle())) {
                        calculatedColumnsIndicies.add(i);
                        break;
                    }
                }
            }
        }

        List<Integer> groupRowIndexes = rs.getGroupRowsIndexes();
        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);

            Font font = NORMAL_FONT;

            if (withSummary) {
                if (i == rows.size() - 1) {
                    font = BOLD_FONT;
                    PdfPCell cell = cell("Итого:", font);
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPaddingBottom(3);
                    cell.setBorder(0);
                    table.addCell(cell);
                } else {
                    addEmptyCellWithNoBorder(table, font);
                }
            }

            //колонка с номером строки
            if (!withSummary || i < rows.size() - 1) {
                addIntegerCell(table, i + 1, font);
            } else {
                addCell(table, "", font);
            }

            if (groupRowIndexes.remove(Integer.valueOf(i))) {
                addGroupRow(table, row);
            } else {
                for (int j = 0; j < row.size(); j++) {
                    Object cellValue = row.get(j);
                    String value = cellValue != null ? cellValue.toString() : "";
                    if (calculatedColumnsIndicies.contains(j) && !value.isEmpty()) {
                        addBigDecimalCell(table, BigDecimal.valueOf(Double.valueOf(value)), font);
                    } else {
                        addCell(table, value, font);
                    }
                }
            }

        }
    }

    private void addGroupRow(PdfPTable table, List<Object> row) {
        Font font = BOLD_FONT;
        PdfPCell cell;
        for (int j = 0; j < row.size(); j++) {
            //первая колонка содержит значение группы
            if (j == 0) {
                Object cellValue = row.get(j);
                String value = cellValue != null ? " " + cellValue.toString() : "";
                cell = cell(value, font);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            } else {
                cell = cell("", font);
            }

            cell.setBorderWidth(0);
            cell.setBorderWidthBottom(0.5F);
            if (j == row.size() - 1) {
                cell.setBorderWidthRight(0.5F);
            }
            cell.setPaddingBottom(3);
            table.addCell(cell);
        }
    }

    private void writeTableHeader(ResultSetWithTotal rs, PdfPTable table, boolean withCalculations) {
        if (withCalculations) {
            addEmptyCellWithNoBorder(table, BOLD_FONT);
        }
        addCell(table, "№", BOLD_FONT);
        rs.getHeaders().forEach(header -> addCell(table, StringUtils.isEmpty(header.getTitle()) ? header.getColumn() : header.getTitle(), BOLD_FONT));

    }

    private void writeParameters(ReportDTO report, Document document) throws DocumentException {
        //Записываем параметры отчета
        Condition[] conditions = report.getQueryDescriptor().getWhere();
        if (conditions != null && conditions.length > 0) {
            String strConditions = NoGroupByQueryBuilder.buildWhereStatement(conditions, tablesService.getTablesColumnTypesMap(report.getQueryDescriptor()), true, translator);

            String[] parts = strConditions.split("\n");
            String[][] headerTableData = new String[parts.length + 1][3];
            headerTableData[0][0] = "Параметры:";
            for (int i = 0; i < parts.length; i++) {
                headerTableData[i + 1][0] = parts[i];
            }
            writeHeaderTable(document, new float[]{20f, 20f, 60f}, headerTableData);
            document.add(Chunk.NEWLINE);
        }
    }
}
