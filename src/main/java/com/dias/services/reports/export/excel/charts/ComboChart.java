package com.dias.services.reports.export.excel.charts;

import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import org.apache.poi.ss.util.CellReference;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

import java.awt.*;
import java.util.Map;

public class ComboChart extends BaseChart {

    private final CTPlotArea plot;

    public ComboChart(ResultSetWithTotal rs, CTChart ctChart, ChartDescriptor chartDescriptor) {
        super(rs, ctChart, chartDescriptor);
        this.plot = ctChart.getPlotArea();
    }

    @Override
    public void addSeries(int firstDataRow, Map<String, Integer> excelColumnsMap, String dataSheetName) {
        int xColumn = excelColumnsMap.get(chartDescriptor.getAxisXColumn());
        String xColumnName = CellReference.convertNumToColString(xColumn);
        Map<String, Integer> rsColumnsMap = rs.getColumnsMap();
        for (int i = 0; i < chartDescriptor.getSeries().size(); i++) {
            ChartDescriptor.Series series = chartDescriptor.getSeries().get(i);
            String type = series.getType();

            int fromRowIndex = (series.getStartRow() != null && series.getStartRow() > 0) ? series.getStartRow() - 1 : 0;
            int rowsNumber = rs.getRows().size();
            int toRowIndex = (series.getEndRow() != null && series.getEndRow() < rowsNumber) ? series.getEndRow() : rowsNumber;
            int from = firstDataRow + fromRowIndex + 1;
            int to = firstDataRow + toRowIndex;

            if (type != null && ChartDescriptor.SERIES_TYPE_LINEAR.equals(type)) {
                addLinearSeries(i, firstDataRow, series, dataSheetName, xColumnName, excelColumnsMap, from, to);
            }

            if (type != null && ChartDescriptor.SERIES_TYPE_AREA.equals(type)) {
                addAreaSeries(i, firstDataRow, series, dataSheetName, xColumnName, excelColumnsMap, from, to);
            }

            if (type != null && ChartDescriptor.SERIES_TYPE_BAR.equals(type)) {
                addBarSeries(i, firstDataRow, series, dataSheetName, xColumnName, excelColumnsMap, from, to);
            }

            if (chartDescriptor.isCalculatedXRange()) {
                if (xFromTo[0] > fromRowIndex || xFromTo[0] == -1){
                    xFromTo[0] = fromRowIndex;
                }
                if (xFromTo[1] < toRowIndex) {
                    xFromTo[1] = toRowIndex;
                }
            }
            if (chartDescriptor.isCalculatedYRange()) {
                defineMinMax(yMinMax, rsColumnsMap.get(series.getValueColumn()), fromRowIndex, toRowIndex);
            }

        }
    }

    private void addAreaSeries(int index, int firstDataRow, ChartDescriptor.Series series, String dataSheetName, String xColumnName, Map<String, Integer> excelColumnsMap, int from, int to) {
        CTAreaChart chart = plot.addNewAreaChart();

        chart.addNewAxId().setVal(AXIS_X_ID);
        chart.addNewAxId().setVal(AXIS_Y_ID);

        CTAreaSer ser = chart.addNewSer();
        ser.addNewIdx().setVal(index);

        if (series.getColor() != null) {
            Color color = series.toAwtColor(series.getColor());
            CTShapeProperties sp = ser.addNewSpPr();
            sp.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
        }

        String xFormula = dataSheetName + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to;
        ser.addNewCat().addNewStrRef().setF(xFormula);

        addSeriesYFormula(series, dataSheetName, excelColumnsMap, ser.addNewVal(), from, to);

        addSeriesTtitle(firstDataRow, series, dataSheetName, excelColumnsMap, ser.addNewTx());

        if (chartDescriptor.isShowDotValues()) {
            showDotValues(ser.addNewDLbls(), null);
        }


    }

    private void addBarSeries(int index, int firstDataRow, ChartDescriptor.Series series, String dataSheetName, String xColumnName, Map<String, Integer> excelColumnsMap, int from, int to) {
        CTBarChart chart = plot.addNewBarChart();
        chart.addNewBarDir().setVal(STBarDir.COL);
        chart.addNewAxId().setVal(AXIS_X_ID);
        chart.addNewAxId().setVal(AXIS_Y_ID);
        CTBarSer ser = chart.addNewSer();
        ser.addNewIdx().setVal(index);
        if (series.getColor() != null) {
            Color color = series.toAwtColor(series.getColor());
            CTShapeProperties sp = ser.addNewSpPr();
            sp.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
        }

        String xFormula = dataSheetName + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to;
        ser.addNewCat().addNewStrRef().setF(xFormula);
        addSeriesYFormula(series, dataSheetName, excelColumnsMap, ser.addNewVal(), from, to);
        addSeriesTtitle(firstDataRow, series, dataSheetName, excelColumnsMap, ser.addNewTx());

        if (chartDescriptor.isShowDotValues()) {
            showDotValues(ser.addNewDLbls(), STDLblPos.IN_END);
        }
    }

    private void addLinearSeries(int index, int firstDataRow, ChartDescriptor.Series series, String dataSheetName, String xColumnName, Map<String, Integer> excelColumnsMap, int from, int to) {
        CTLineChart chart = plot.addNewLineChart();

        chart.addNewAxId().setVal(AXIS_X_ID);
        chart.addNewAxId().setVal(AXIS_Y_ID);

        CTLineSer ser = chart.addNewSer();
        ser.addNewIdx().setVal(index);

        if (series.getColor() != null) {
            Color color = series.toAwtColor(series.getColor());
            CTShapeProperties sp = ser.addNewSpPr();
            sp.addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
            CTMarker ctMarker = ser.addNewMarker();
            CTShapeProperties markerSp = ctMarker.addNewSpPr();
            markerSp.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
            markerSp.addNewLn().addNewNoFill();
        }

        String xFormula = dataSheetName + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to;
        ser.addNewCat().addNewStrRef().setF(xFormula);

        addSeriesYFormula(series, dataSheetName, excelColumnsMap, ser.addNewVal(), from, to);

        addSeriesTtitle(firstDataRow, series, dataSheetName, excelColumnsMap, ser.addNewTx());

        showDotValues(ser.addNewDLbls(), STDLblPos.T);

    }

    private void addSeriesTtitle(int firstDataRow, ChartDescriptor.Series series, String dataSheetName, Map<String, Integer> excelColumnsMap, CTSerTx tx) {
        //если необходимо показывать легенду, указываем ячейку с наименованием колонки
        if (series.getTitle() != null) {
            tx.setV(series.getTitle());
        } else {
            int valueColumnIndex = excelColumnsMap.get(series.getValueColumn());
            String valueColumnName = CellReference.convertNumToColString(valueColumnIndex);
            tx.addNewStrRef().setF(dataSheetName + "!$" + valueColumnName + "$" + firstDataRow);
        }
    }

    private void showDotValues(CTDLbls dLbls, STDLblPos.Enum pos) {
        if (pos != null) {
            dLbls.addNewDLblPos().setVal(pos);
        }
        dLbls.addNewShowVal().setVal(chartDescriptor.isShowDotValues());
        //отключим отображение всего лишнего
        dLbls.addNewShowSerName().setVal(false);
        dLbls.addNewShowCatName().setVal(false);
        dLbls.addNewShowBubbleSize().setVal(false);
        dLbls.addNewShowLeaderLines().setVal(false);
        dLbls.addNewShowLegendKey().setVal(false);
    }

    private void addSeriesYFormula(ChartDescriptor.Series series, String dataSheetName, Map<String, Integer> excelColumnsMap, CTNumDataSource ctNumDataSource, int from, int to) {
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        int valueColumnIndex = excelColumnsMap.get(series.getValueColumn());
        String valueColumnName = CellReference.convertNumToColString(valueColumnIndex);
        ctNumRef.setF(dataSheetName + "!$" + valueColumnName + "$" + from + ":$" + valueColumnName + "$" + to);
    }

    @Override
    public ISeries addNewSeries(ChartDescriptor.Series s) {
        return null;
    }

    @Override
    public CTUnsignedInt addNewAxId() {
        return null;
    }

    @Override
    public CTShapeProperties addNewShapeProperties(int seriesIndex) {
        return null;
    }

    @Override
    public IAxisX addAxisX(CTPlotArea plotArea, boolean isCategoryAxisNumeric) {
        return new CategoryAxis(plotArea.addNewCatAx());
    }

    @Override
    public CTValAx addNewValAx() {
        return plot.addNewValAx();
    }
}
