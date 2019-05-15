package com.dias.services.reports.export.excel.charts;

import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.util.CellReference;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;

import java.util.List;
import java.util.Map;

public abstract class BaseChart implements IChartWithSeries {

    static final STDLblPos.Enum LABEL_POSITION_OUTSIDE_TOP = STDLblPos.OUT_END;
    static final STDLblPos.Enum LABEL_POSITION_TOP = STDLblPos.T;
    protected static final int AXIS_Y_ID = 2;
    protected static final int AXIS_X_ID = 1;

    private final CTChart ctChart;
    protected final ChartDescriptor chartDescriptor;
    private final boolean isCategoryAxisNumeric;
    private final boolean isCategoryAxisDate;
    protected final ResultSetWithTotal rs;
    private final Integer categoryRsColumnIndex;
    private double[] xMinMax = new double[]{Double.MAX_VALUE, Double.MIN_NORMAL};
    double[] yMinMax = new double[]{Double.MAX_VALUE, Double.MIN_NORMAL};
    protected int[] xFromTo = new int[]{-1, 0};



    BaseChart(ResultSetWithTotal rs, CTChart ctChart, ChartDescriptor chartDescriptor) {
        this.ctChart = ctChart;
        this.chartDescriptor = chartDescriptor;
        this.rs = rs;
        Map<String, Integer> rsColumnsMap = rs.getColumnsMap();
        this.categoryRsColumnIndex = rsColumnsMap.get(chartDescriptor.getAxisXColumn());
        this.isCategoryAxisNumeric = rs.getNumericColumnsIndexes().contains(categoryRsColumnIndex);
        this.isCategoryAxisDate = rs.getDateColumnsIndexes().contains(categoryRsColumnIndex);
    }

    public void addSeries(int firstDataRow,
                          Map<String, Integer> excelColumnsMap,
                          String dataSheetName) {

        Integer rowsNumber = rs.getRows().size();
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        Map<String, Integer> rsColumnsMap = rs.getColumnsMap();
        int xColumn = excelColumnsMap.get(chartDescriptor.getAxisXColumn());
        String xColumnName = CellReference.convertNumToColString(xColumn);


        for (int i = 0; i < series.size(); i++) {
            ChartDescriptor.Series s = series.get(i);
            ISeries chartSeries = addNewSeries(s);
            chartSeries.colorize(s.getAwtColor());
            chartSeries.addNewIdx().setVal(i);
            int fromRowIndex = (s.getStartRow() != null && s.getStartRow() > 0) ? s.getStartRow() - 1 : 0;
            int toRowIndex = (s.getEndRow() != null && s.getEndRow() < rowsNumber) ? s.getEndRow(): rowsNumber;
            int from = firstDataRow + fromRowIndex + 1;
            int to = firstDataRow + toRowIndex;
            chartSeries.doWithRange(from, to);
            chartSeries.setFforX(dataSheetName + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to);
            CTNumDataSource ctNumDataSource = chartSeries.addNewVal();
            CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
            int valueColumnIndex = excelColumnsMap.get(s.getValueColumn());
            if (chartDescriptor.isCalculatedXRange()) {
                if (xFromTo[0] > fromRowIndex || xFromTo[0] == -1){
                    xFromTo[0] = fromRowIndex;
                }
                if (xFromTo[1] < toRowIndex) {
                    xFromTo[1] = toRowIndex;
                }
            }
            if (chartDescriptor.isCalculatedYRange()) {
                defineMinMax(yMinMax, rsColumnsMap.get(s.getValueColumn()), fromRowIndex, toRowIndex);
            }

            String valueColumnName = CellReference.convertNumToColString(valueColumnIndex);
            if (chartDescriptor.getShowLegend()) {
                //если необходимо показывать легенду, указываем ячейку с наименованием колонки
                if (s.getTitle() != null) {
                    chartSeries.addNewTx().setV(s.getTitle());
                } else {
                    chartSeries.addNewTx().addNewStrRef().setF(dataSheetName + "!$" + valueColumnName + "$" + firstDataRow);
                }

            }

            chartSeries.addDotValues(chartDescriptor, getValueLabelsLocation());

            ctNumRef.setF(dataSheetName + "!$" + valueColumnName + "$" + from + ":$" + valueColumnName + "$" + to);
        }
    }

    protected STDLblPos.Enum getValueLabelsLocation() {
        return LABEL_POSITION_OUTSIDE_TOP;
    }

    protected void defineMinMax(double[] minmax, int column, int from, int to) {
        List<List<Object>> rows = rs.getRows();
        for (int i = from; i >= 0 && i < to; i++) {
            List<Object> row = rows.get(i);
            Object value = row.get(column);
            if (value != null) {
                try {
                    Double doubleValue = Double.valueOf(value.toString());
                    if (minmax[0] > doubleValue || minmax[0] == Double.MIN_NORMAL) {
                        minmax[0] = doubleValue;
                    }
                    if (minmax[1] < doubleValue || minmax[1] == Double.MIN_NORMAL) {
                        minmax[1] = doubleValue;
                    }
                } catch (Exception ignore) {
                }
            }
        }
    }

    public void addLegend(ChartDescriptor chartDescriptor) {
        //legend
        if (chartDescriptor.getShowLegend()) {
            CTLegend ctLegend = ctChart.addNewLegend();
            ctLegend.addNewLegendPos().setVal(STLegendPos.R);
            ctLegend.addNewOverlay().setVal(false);
        }
    }

    public void addXY() {

        CTUnsignedInt axId = addNewAxId();
        if (axId != null) {
            axId.setVal(AXIS_X_ID);
        }
        axId = addNewAxId();
        if (axId != null) {
            axId.setVal(AXIS_Y_ID);
        }

        //val axis
        CTValAx ctValAx = addNewValAx();
        if (ctValAx != null) {
            // не все диаграммы поддерживают ось Y
            ctValAx.addNewAxId().setVal(AXIS_Y_ID); //id of the val axis
            ctValAx.addNewDelete().setVal(false);
            ctValAx.addNewAxPos().setVal(STAxPos.L);
            ctValAx.addNewCrossAx().setVal(AXIS_X_ID); //id of the cat axis
            ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
            if (!StringUtils.isEmpty(chartDescriptor.getAxisYTitle())) {
                setAxisTitle(ctValAx.addNewTitle(), chartDescriptor.getAxisYTitle());
            }
        }
        IAxisX axisX = addAxisX(ctChart.getPlotArea(), isCategoryAxisNumeric || isCategoryAxisDate);
        if (axisX != null) {
            // не все диаграммы поддерживают ось X
            axisX.addNewAxId().setVal(AXIS_X_ID); //id of the cat axis
            axisX.addNewDelete().setVal(false);
            axisX.addNewAxPos().setVal(STAxPos.B);
            axisX.addNewCrossAx().setVal(AXIS_Y_ID); //id of the val axis
            axisX.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
            if (!StringUtils.isEmpty(chartDescriptor.getAxisXTitle())) {
                setAxisTitle(axisX.addTitle(), chartDescriptor.getAxisXTitle());
            }

            CTScaling xctScaling = axisX.addNewScaling();
            xctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);


            if (axisX.supportsMinMax()) {
                // гистограмма и колонки не поддерживают минимум/максимум
                if (chartDescriptor.isCalculatedXRange() && isCategoryAxisNumeric) {
                    defineMinMax(xMinMax, categoryRsColumnIndex, xFromTo[0], xFromTo[1]);
                    fixRange(xMinMax);

                    // блок определения минимума/максимума оси
                    xctScaling.addNewMin().setVal(xMinMax[0]);
                    xctScaling.addNewMax().setVal(xMinMax[1]);

                    if (ctValAx != null) {
                        // укажем, что ось значений должна пересечь ось категорий в минимуме
                        ctValAx.addNewCrossesAt().setVal(xMinMax[0]);
                    }
                } else if (isCategoryAxisNumeric) {
                    xctScaling.addNewMin().setVal(0);
                    if (ctValAx != null) {
                        ctValAx.addNewCrossesAt().setVal(0);
                    }
                }
            }
        }

        if (ctValAx != null) {
            CTScaling yctScaling = ctValAx.addNewScaling();
            yctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
            if (chartDescriptor.isCalculatedYRange()) {

                fixRange(yMinMax);

                // блок определения минимума/максимума оси
                yctScaling.addNewMin().setVal(yMinMax[0]);
                yctScaling.addNewMax().setVal(yMinMax[1]);
                if (axisX != null) {
                    // укажем, что ось значений должна пересечь ось значений в минимуме
                    axisX.addNewCrossesAt().setVal(yMinMax[0]);
                }
            }
        }

    }

    private static void setAxisTitle(CTTitle ctTitle, String title) {
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

    private void fixRange(double[] minmax) {
        // подгон минимума и максимума значений - алогоритм аналогичен конструктору отчета (UI)
        if (minmax[0] > Double.MIN_NORMAL) {
            double margin = Math.abs((minmax[1] - minmax[0]) / 15);
            double lower = Math.floor((minmax[0] - margin) * 100) / 100;
            double upper = Math.floor((minmax[1] + margin) * 100) / 100;
            minmax[0] = lower;
            minmax[1] = upper;
        }
    }
}
