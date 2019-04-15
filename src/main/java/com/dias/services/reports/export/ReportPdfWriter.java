package com.dias.services.reports.export;

import com.dias.services.reports.dto.reports.ReportDTO;
import com.dias.services.reports.query.NoGroupByQueryBuilder;
import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.Calculation;
import com.dias.services.reports.report.query.Column;
import com.dias.services.reports.report.query.Condition;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import com.dias.services.reports.service.ReportService;
import com.dias.services.reports.translation.Translator;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.dias.services.reports.utils.PdfExportUtils.*;

import java.util.List;

public class ReportPdfWriter {

    /**
     * Ключ для категорий. Служит для различения категорий по индексу, а не по имени как по умолчанию
     */
    static class CategoryKey implements Comparable<CategoryKey> {
        private String key;
        private int index;

        CategoryKey(String key, int index) {
            this.key = key;
            this.index = index;
        }

        @Override
        public int compareTo(CategoryKey o) {
            return Integer.compare(o.index, this.index);
        }

        @Override
        public String toString() {
            //переопределяем метод для вывода на графике имени
            return key;
        }
    }

    private static final int CHART_WIDTH = (int) PageSize.A4.getHeight() - 40;
    private static final int CHART_HEIGHT = (int) PageSize.A4.getWidth() - 40;
    private static ChartTheme currentTheme = new StandardChartTheme("JFree");
    private final ReportService tablesService;
    private final Translator translator;
    private final String nullSymbol;

    public ReportPdfWriter(ReportService tablesService, Translator translator, String nullSymbol) {
        this.tablesService = tablesService;
        this.translator = translator;
        this.nullSymbol = nullSymbol;
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

            List<Integer> dateColumnIndexes = rs.getDateColumnsIndexes();
            List<Integer> numericColumnIndexes = rs.getNumericColumnsIndexes();
            Map<String, Integer> columnMap = rs.getColumnsMap();
            Integer categoryColumnIndex = columnMap.get(chartDescriptor.getAxisXColumn());
            boolean isDateXAxis = dateColumnIndexes.contains(categoryColumnIndex);
            boolean isNumericXAxis = !isDateXAxis && numericColumnIndexes.contains(categoryColumnIndex);

            //Определим, какого типа датасет нужно будет использовать
            DefaultCategoryDataset defaultCategoryDataset = null;
            DateFormatWithPattern[] dateFormat = {null};
            XYDataset xyDataSet = null;
            double[] xMinMax = new double[]{Double.MIN_NORMAL, Double.MIN_NORMAL};
            double[] yMinMax = new double[]{Double.MIN_NORMAL, Double.MIN_NORMAL};
            if ((!isDateXAxis && !isNumericXAxis) || (reportType == ReportType.Wcascade)) {
                defaultCategoryDataset = getDefaultCategoryDataset(rs, chartDescriptor, withSummary, yMinMax);
            } else {
                xyDataSet = getXYDataset(rs, columnMap, chartDescriptor, withSummary, dateFormat, isDateXAxis, xMinMax, yMinMax);
            }

            JFreeChart chart;
            if (ReportType.bar == reportType || ReportType.hbar == reportType) {
                if (xyDataSet != null) {
                    chart = createXYBarChart(
                            chartDescriptor.getTitle(),
                            chartDescriptor.getAxisXTitle(),
                            isDateXAxis,
                            chartDescriptor.getAxisYTitle(),
                            xyDataSet,
                            (ReportType.bar == reportType ? PlotOrientation.VERTICAL : PlotOrientation.HORIZONTAL),
                            chartDescriptor.getShowLegend(),
                            dateFormat);
                } else {
                    chart = ChartFactory.createBarChart(
                            chartDescriptor.getTitle(),
                            chartDescriptor.getAxisXTitle(),
                            chartDescriptor.getAxisYTitle(),
                            defaultCategoryDataset,
                            (ReportType.bar == reportType ? PlotOrientation.VERTICAL : PlotOrientation.HORIZONTAL),
                            chartDescriptor.getShowLegend(),
                            false,
                            false);

                    if (ReportType.hbar == reportType) {
                        chart.getCategoryPlot().setRangeAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
                    }

                }

                if (ReportType.bar == reportType) {
                    // Поворачиваем подписи по оси X только в случае вертикальных баров
                    if (isDateXAxis) {
                        //В случае XY оси подписи (их периодичность) устанавливаются автоматически
                        //Для дат поворот делаем безусловно, поскольку метки длинные
                        rotateXAxisLabels(chart);
                    } else if (!isNumericXAxis){
                        //Для оси категорий делаем поворот условный, в зависимости от общей длины меток
                        rotateCategoryLabelsIfNeeded(defaultCategoryDataset, chart);
                    }
                }

            } else if (ReportType.Wpie == reportType) {
                PieDataset dataset = getPieDataset(rs, columnMap, chartDescriptor);
                chart = ChartFactory.createPieChart(chartDescriptor.getTitle(),
                        dataset,
                        chartDescriptor.getShowLegend(),
                        false,
                        false);
            } else if (ReportType.Wcascade == reportType) {
                chart = ChartFactory.createWaterfallChart(chartDescriptor.getTitle(),
                        chartDescriptor.getAxisXTitle(),
                        chartDescriptor.getAxisYTitle(),
                        defaultCategoryDataset,
                        PlotOrientation.VERTICAL,
                        chartDescriptor.getShowLegend(),
                        false,
                        false);
            } else {

                if (xyDataSet != null) {
                    chart = createXYLineChart(
                            chartDescriptor.getTitle(),
                            chartDescriptor.getAxisXTitle(),
                            chartDescriptor.getAxisYTitle(),
                            xyDataSet,
                            PlotOrientation.VERTICAL,
                            chartDescriptor.getShowLegend(),
                            isDateXAxis,
                            dateFormat);

                    if (isDateXAxis) {
                        rotateXAxisLabels(chart);
                    }
                } else {

                    chart = ChartFactory.createLineChart(
                            chartDescriptor.getTitle(),
                            chartDescriptor.getAxisXTitle(),
                            chartDescriptor.getAxisYTitle(),
                            defaultCategoryDataset,
                            PlotOrientation.VERTICAL,
                            chartDescriptor.getShowLegend(),
                            false,
                            false);

                    rotateCategoryLabelsIfNeeded(defaultCategoryDataset, chart);

                }
            }


            if (chart != null) {

                colorize(chart, chartDescriptor, defaultCategoryDataset != null);
                setPositionToLegend(chart);

                Plot plot = chart.getPlot();

                if (xyDataSet != null && plot instanceof XYPlot) {
                    XYItemRenderer renderer = chart.getXYPlot().getRenderer();
                    renderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
                    renderer.setBaseItemLabelsVisible(chartDescriptor.isShowDotValues());
                    if (chartDescriptor.isCalculatedXRange() && !isDateXAxis) {
                        // применяем аналогичный в конструкторе отчетов алгорит расчета отступа
                        // для минимального и максимального значений
                        chart.getXYPlot().getDomainAxis().setRange(xMinMax[0], xMinMax[1]);
                    } else if (!isDateXAxis) {
                        // для совместимости с UI делаем нижнюю границу оси в 0, если ось числовая
                        chart.getXYPlot().getDomainAxis().setLowerBound(0);
                    }


                    if (chartDescriptor.isCalculatedYRange()) {
                        chart.getXYPlot().getRangeAxis().setRange(yMinMax[0], yMinMax[1]);
                    } else if (chartDescriptor.isShowDotValues() && ReportType.hbar == reportType) {
                        Double max = calculateAxisUpperBoundToIncludeLabelsForColumnChart(yMinMax[1]);
                        chart.getXYPlot().getRangeAxis().setUpperBound(max.intValue());
                    }

                } else if (plot instanceof CategoryPlot) {
                    CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer();
                    renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
                    renderer.setBaseItemLabelsVisible(chartDescriptor.isShowDotValues());
                    if (chartDescriptor.isCalculatedYRange()) {
                        chart.getCategoryPlot().getRangeAxis().setRange(yMinMax[0], yMinMax[1]);
                    } else if (chartDescriptor.isShowDotValues() && ReportType.hbar == reportType) {
                        Double max = calculateAxisUpperBoundToIncludeLabelsForColumnChart(yMinMax[1]);
                        chart.getCategoryPlot().getRangeAxis().setUpperBound(max.intValue());
                    }
                }

                ChartUtilities.writeChartAsPNG(os, chart, CHART_WIDTH, CHART_HEIGHT);
            }
        }

        if (os.size() > 0) {
            Image image = Image.getInstance(os.toByteArray());
            image.setAlignment(Image.MIDDLE);
            document.add(image);
            os.close();
        }
    }

    private PieDataset getPieDataset(ResultSetWithTotal rs, Map<String, Integer> columnMap, ChartDescriptor chartDescriptor) {
        DefaultPieDataset ds = new DefaultPieDataset();
        List<List<Object>> rows = rs.getRows();
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        int sizeOfRows = rows.size();

        for (ChartDescriptor.Series s : series) {
            String valueColumn = new Column(s.getValueColumn()).getColumnName();
            Integer seriesVaueIndex = columnMap.get(valueColumn);
            int from = s.getStartRow() != null ? s.getStartRow() - 1 : 0;
            int to = Math.min(s.getEndRow() != null && s.getEndRow() > 0 ? s.getEndRow() : sizeOfRows, sizeOfRows);
            if (from < sizeOfRows) {
                int i = 0;
                for (List<Object> row : rows.subList(from, to)) {
                    i++;
                    Object value = row.get(seriesVaueIndex);
                    Number seriesValueNumber = value != null && Number.class.isAssignableFrom(value.getClass()) ? (Number) value : 0;
                    ds.setValue(Integer.toString(i), seriesValueNumber);
                }
            }
        }
        return ds;
    }

    private Double calculateAxisUpperBoundToIncludeLabelsForColumnChart(double maxValue) {
        // рассчитаем величину, на которую необходимо расширить график, чтобы уместились подписи к меткам
        // добавляем 5 символов чтобы учесть надписи к категориям, наименование серий
        // 5 выбрано эмпирически - пока не найден способ подсчитать точно
        return maxValue + NORMAL_FONT.getSize() * (Double.toString(maxValue).length() + 5) / CHART_WIDTH * maxValue;
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

    private void rotateCategoryLabelsIfNeeded(DefaultCategoryDataset ds, JFreeChart chart) {
        org.jfree.chart.axis.CategoryAxis domain = chart.getCategoryPlot().getDomainAxis();
        //если подписи к категориям не умещаются в ширину грфика, расположим подписи вертикально
        int lengthOfAllKeys = StringUtils.join(ds.getColumnKeys()).length();
        if (lengthOfAllKeys > CHART_WIDTH / NORMAL_FONT.getSize()) {
            domain.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        }
    }

    private void rotateXAxisLabels(JFreeChart chart) {
        ValueAxis domain = chart.getXYPlot().getDomainAxis();
        domain.setVerticalTickLabels(true);
    }


    private XYDataset getXYDataset(
            ResultSetWithTotal rs,
            Map<String, Integer> columnMap,
            ChartDescriptor chartDescriptor,
            boolean withSummary,
            final DateFormatWithPattern[] dateFormat,
            boolean categoryIsDate,
            double[] xMinMax,
            double[] yMinMax) {
        XYSeriesCollection ds = new XYSeriesCollection();
        List<List<Object>> rows = rs.getRows();
        Integer categoryColumnIndex = columnMap.get(chartDescriptor.getAxisXColumn());
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();

        //последняя строка - итоговая в случае withSummary = true
        int sizeOfRows = withSummary ? rows.size() - 1 : rows.size();

        for (ChartDescriptor.Series s : series) {

            XYSeries xySeries = new XYSeries(s.getTitle());
            ds.addSeries(xySeries);
            String valueColumn = s.getValueColumn();
            Integer seriesVaueIndex = columnMap.get(valueColumn);
            int from = s.getStartRow() != null ? s.getStartRow() - 1 : 0;
            int to = Math.min(s.getEndRow() != null && s.getEndRow() > 0 ? s.getEndRow() : sizeOfRows, sizeOfRows);
            if (from < sizeOfRows) {
                for (List<Object> row : rows.subList(from, to)) {
                    Object categoryValue = row.get(categoryColumnIndex);
                    Number categoryNumber = null;
                    if (categoryIsDate) {
                        resolveDateFormat(dateFormat, categoryValue);
                        if (categoryValue != null && categoryValue instanceof LocalDateTime) {
                            categoryNumber = ((LocalDateTime) categoryValue).toInstant(ZoneOffset.UTC).toEpochMilli();
                        } else {
                            categoryNumber = new Date(0).getTime();
                        }

                    } else if (Number.class.isAssignableFrom(categoryValue.getClass())) {
                        categoryNumber = (Number) categoryValue;
                    } else {
                        continue;
                    }
                    Object value = row.get(seriesVaueIndex);
                    Number seriesValueNumber = value != null && Number.class.isAssignableFrom(value.getClass()) ? (Number) value : 0;
                    xySeries.add(categoryNumber, seriesValueNumber);

                    if (!categoryIsDate) {
                        //только для числовой оси определяем минимальное и максимальное значения
                        calculateMinMaxForValue(xMinMax, categoryNumber);
                    }

                    calculateMinMaxForValue(yMinMax, seriesValueNumber);

                }
            }
        }
        fixRange(xMinMax);
        fixRange(yMinMax);
        return ds;
    }

    private void fixRange(double[] minmax) {
        if (minmax[0] > Double.MIN_NORMAL) {
            double margin = Math.abs((minmax[1] - minmax[0]) / 15);
            double lower = Math.floor((minmax[0] - margin) * 100) / 100;
            double upper = Math.floor((minmax[1] + margin) * 100) / 100;
            minmax[0] = lower;
            minmax[1] = upper;
        }
    }

    private void calculateMinMaxForValue(double[] range, Number number) {
        double doubleValue = number.doubleValue();
        if (doubleValue > range[1] || range[1] == Double.MIN_NORMAL) {
            range[1] = doubleValue;
        }
        if (doubleValue < range[0] || range[0] == Double.MIN_NORMAL) {
            range[0] = doubleValue;
        }
    }


    private DefaultCategoryDataset getDefaultCategoryDataset(ResultSetWithTotal rs, ChartDescriptor chartDescriptor, boolean withSummary, double[] yMinMax) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        List<List<Object>> rows = rs.getRows();
        Map<String, Integer> columnMap = rs.getColumnsMap();
        Integer categoryColumnIndex = columnMap.get(new Column(chartDescriptor.getAxisXColumn()).getColumnName());
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        //последняя строка - итоговая в случае withSummary = true
        int sizeOfRows = withSummary ? rows.size() - 1 : rows.size();

        for (ChartDescriptor.Series s : series) {
            String valueColumn = new Column(s.getValueColumn()).getColumnName();
            Integer seriesVaueIndex = columnMap.get(valueColumn);
            int from = s.getStartRow() != null ? s.getStartRow() - 1 : 0;
            int to = Math.min(s.getEndRow() != null && s.getEndRow() > 0 ? s.getEndRow() : sizeOfRows, sizeOfRows);
            if (from < sizeOfRows) {
                int i = 0;
                for (List<Object> row : rows.subList(from, to)) {
                    i++;
                    Object categoryValue = row.get(categoryColumnIndex);
                    categoryValue = categoryValue == null ? "" : categoryValue;
                    Object value = row.get(seriesVaueIndex);
                    Number seriesValueNumber = value != null && Number.class.isAssignableFrom(value.getClass()) ? (Number) value : 0;
                    ds.addValue(seriesValueNumber, s.getTitle(), new CategoryKey(categoryValue.toString(), i));
                    calculateMinMaxForValue(yMinMax, seriesValueNumber);
                }
            }
        }
        fixRange(yMinMax);
        return ds;
    }

    private static void resolveDateFormat(DateFormatWithPattern[] dateFormat, Object value) {
        if (dateFormat[0] == null && value != null) {
            String pattern = ExportChartsHelper.calculateDateFormatPattern(value.toString());
            if (pattern != null) {
                dateFormat[0] = new DateFormatWithPattern(pattern);
            }
        }
    }

    private void colorize(JFreeChart chart, ChartDescriptor chartDescriptor, boolean isCategory) {
        chart.getPlot().setBackgroundPaint(Color.white);
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        for (int i = 0; i < series.size(); i++) {
            ChartDescriptor.Series s = series.get(i);
            String color = s.getColor();
            if (color != null) {
                Plot plot = chart.getPlot();
                if (plot instanceof CategoryPlot) {
                    ((CategoryPlot) plot).getRenderer().setSeriesPaint(i, new Color(Integer.parseInt(color.substring(1), 16)));
                } else if (plot instanceof XYPlot) {
                    ((XYPlot) plot).getRenderer().setSeriesPaint(i, new Color(Integer.parseInt(color.substring(1), 16)));
                }
            }
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
        List<Integer> numericIndexes = rs.getNumericColumnsIndexes();
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

            if (groupRowIndexes != null && groupRowIndexes.remove(Integer.valueOf(i))) {
                addGroupRow(table, row);
            } else {
                for (int j = 0; j < row.size(); j++) {
                    Object cellValue = row.get(j);
                    if (numericIndexes.contains(j)) {
                        addBigDecimalCell(table, cellValue, font, nullSymbol);
                    } else {
                        addCell(table, cellValue != null ? cellValue.toString() : nullSymbol, font);
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

    private static JFreeChart createXYLineChart(
            String title,
            String xAxisLabel,
            String yAxisLabel,
            XYDataset dataset,
            PlotOrientation orientation,
            boolean legend,
            boolean categoryIsDate,
            DateFormatWithPattern[] dateFormat) {

        if (orientation == null) {
            throw new IllegalArgumentException("Null 'orientation' argument.");
        } else {
            ValueAxis xAxis = getValueAxisForXYChart(xAxisLabel, categoryIsDate, dateFormat);
            NumberAxis yAxis = new NumberAxis(yAxisLabel);
            XYItemRenderer renderer = new XYLineAndShapeRenderer(true, true);
            XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
            plot.setOrientation(orientation);
            JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
            currentTheme.apply(chart);
            return chart;
        }
    }

    private static JFreeChart createXYBarChart(String title,
                                               String xAxisLabel,
                                               boolean dateAxis,
                                               String yAxisLabel,
                                               XYDataset dataset,
                                               PlotOrientation orientation,
                                               boolean legend,
                                               DateFormatWithPattern[] dateFormat) {
        if (orientation == null) {
            throw new IllegalArgumentException("Null 'orientation' argument.");
        } else {
            ValueAxis domainAxis = getValueAxisForXYChart(xAxisLabel, dateAxis, dateFormat);
            NumberAxis valueAxis = new NumberAxis(yAxisLabel);
            XYBarRenderer renderer = new XYBarRenderer();
            XYPlot plot = new XYPlot(dataset, domainAxis, valueAxis, renderer);
            plot.setOrientation(orientation);
            JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
            currentTheme.apply(chart);
            return chart;
        }
    }

    private static ValueAxis getValueAxisForXYChart(String xAxisLabel, boolean dateAxis, DateFormatWithPattern[] dateFormat) {
        ValueAxis domainAxis;
        if (dateAxis) {
            domainAxis = new DateAxis(xAxisLabel);
            ((DateAxis) domainAxis).setDateFormatOverride(dateFormat[0].toClassicFormat());
        } else {
            domainAxis = new NumberAxis(xAxisLabel);
            ((NumberAxis)domainAxis).setAutoRangeIncludesZero(false);
        }
        return domainAxis;
    }


}
