package com.dias.services.reports.export;

import com.dias.services.reports.dto.reports.ReportDTO;
import com.dias.services.reports.export.charts.*;
import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.STBarDir;
import org.openxmlformats.schemas.drawingml.x2006.chart.STScatterStyle;

import java.util.Map;

/**
 * Хелпер для построения диаграмм в Excel.
 */
class ExcelChartsFactory {

    private static final int CHART_WIDTH = 15;
    private static final int CHART_HEIGHT = 20;
    /**
     * Добавление диаграммы в рабочую книгу excel
     * @param workbook рабочая книга excel
     * @param chartDescriptor описание диаграммы
     * @param report отчет
     * @param repType тип отчета
     * @param firstRowWithData номер первой строки, содержащей данные
     * @param rs набор данных
     * @param sheet страница для добавления диагрммы
     * @param excelColumnsMap соответствие имен колонок индексам на странице excel
     * @param reportExcelWriter
     */
    static void addChartToWorkbook(XSSFWorkbook workbook, ChartDescriptor chartDescriptor, ReportDTO report, ReportType repType, int firstRowWithData, ResultSetWithTotal rs, XSSFSheet sheet, Map<String, Integer> excelColumnsMap, ReportExcelWriter reportExcelWriter) {

        String title = chartDescriptor.getTitle();
        XSSFSheet dataSheet = workbook.createSheet(title != null ? title : report.getTitle());
        XSSFDrawing drawing = dataSheet.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 0, CHART_WIDTH, CHART_HEIGHT);
        XSSFChart xssfChart = drawing.createChart(anchor);
        xssfChart.setTitleText(chartDescriptor.getTitle());
        createByType(reportExcelWriter, repType, xssfChart.getCTChart(), chartDescriptor, rs, firstRowWithData, excelColumnsMap, sheet.getSheetName());
    }

    private static void createByType(ReportExcelWriter reportExcelWriter, ReportType repType, CTChart ctChart, ChartDescriptor chartDescriptor, ResultSetWithTotal rs, int firstDataRow, Map<String, Integer> excelColumnsMap, String dataSheetName) {
        BaseChart chart = null;

        if (repType == ReportType.Wpie) {

            chart = new PieChart(rs, ctChart, chartDescriptor);

        } else if (repType == ReportType.Wcascade) {

            chart = new CascadeChart(reportExcelWriter, rs, ctChart, chartDescriptor);

        } else if (repType == ReportType.hbar) {

            chart = new BarChart(rs, ctChart, chartDescriptor, STBarDir.BAR);

        } else if (repType == ReportType.bar) {

            chart = new BarChart(rs, ctChart, chartDescriptor, STBarDir.COL);

        } else if (repType == ReportType.Wscatter) {

            chart = new ScatterMarkerChart(rs, ctChart, chartDescriptor);

        } else if (repType == ReportType.linear) {

            Integer categoryRsColumnIndex = rs.getColumnsMap().get(chartDescriptor.getAxisXColumn());

            if (rs.getNumericColumnsIndexes().contains(categoryRsColumnIndex) || rs.getDateColumnsIndexes().contains(categoryRsColumnIndex)) {

                chart = new ScatterChart(rs, ctChart, chartDescriptor, STScatterStyle.SMOOTH_MARKER);

            } else {

                chart =  new LineChart(rs, ctChart, chartDescriptor);

            }

        }

        if (chart != null) {
            chart.addSeries(firstDataRow, excelColumnsMap, dataSheetName);
            chart.addXY();
            chart.addLegend(chartDescriptor);
        }
    }

}
