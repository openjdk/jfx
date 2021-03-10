/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.javafx.scene.chart;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChartShim;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class XYChartDataTest {

    @Test public void creatingDataShouldSetValuesAndCurrentValues() {
        XYChart.Data<Number,Number> data = new XYChart.Data<Number,Number>(10, 20);
        assertEquals(10, data.getXValue().longValue());
        assertEquals(10, XYChartShim.Data_getCurrentX(data).longValue());
        assertEquals(20, data.getYValue().longValue());
        assertEquals(20, XYChartShim.Data_getCurrentY(data).longValue());
    }

    @Test public void updatingValuesBeforeAddingToASeriesShouldUpdateValuesAndCurrentValues() {
        XYChart.Data<Number,Number> data = new XYChart.Data<Number,Number>(10, 20);
        data.setXValue(100);
        data.setYValue(200);
        assertEquals(100, data.getXValue().longValue());
        assertEquals(100, XYChartShim.Data_getCurrentX(data).longValue());
        assertEquals(200, data.getYValue().longValue());
        assertEquals(200, XYChartShim.Data_getCurrentY(data).longValue());
    }

    @Test public void testSortXYChartData() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        ObservableList<XYChart.Data<Number, Number>> list =
                FXCollections.observableArrayList(new XYChart.Data<Number,
                Number>(4, 4), new XYChart.Data<Number, Number>(1, 1),
                new XYChart.Data<Number, Number>(3, 3),
                new XYChart.Data<Number, Number>(2, 2));

        LineChart<Number, Number> lineChart = new LineChart<Number, Number>(xAxis, yAxis);
        XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
        series.setData(list);

        lineChart.getData().add(series);
        FXCollections.sort(list, (o1, o2) -> Double.compare(o1.getXValue().intValue(), o2.getXValue().intValue()));
        ObservableList<XYChart.Data<Number, Number>> data = series.getData();
        // check sorted data
        assertEquals(1, data.get(0).getXValue());
        assertEquals(2, data.get(1).getXValue());
        assertEquals(3, data.get(2).getXValue());
        assertEquals(4, data.get(3).getXValue());

    }

    @Test
    public void testSeriesAddDelete() {
        XYChart.Series<String, Number> series = new XYChart.Series<String, Number>();
        Number value1 = 5;
        Number value2 = 6;
        XYChart.Data<String, Number> point1 = new XYChart.Data<String, Number>("Something", value1);
        XYChart.Data<String, Number> point2 = new XYChart.Data<String, Number>("Something", value2);
        series.getData().add(point1);
        series.getData().add(point2);
        assertEquals(0, XYChartShim.Series_getDataSize(series));

        XYChart<String, Number> chart = new StringNumberXYChart();

        chart.setData(FXCollections.singletonObservableList(series));

        assertEquals(2, XYChartShim.Series_getDataSize(series));

        series.getData().clear();

        assertEquals(0, XYChartShim.Series_getDataSize(series));

        series.getData().add(point1);

        assertEquals(1, XYChartShim.Series_getDataSize(series));

        chart.setData(null);

        assertEquals(0, XYChartShim.Series_getDataSize(series));

    }

    private static class StringNumberXYChart extends XYChart<String, Number> {
        public StringNumberXYChart() {
            super(new CategoryAxis(), new NumberAxis());
        }

        @Override
        protected void dataItemAdded(Series<String, Number> series, int itemIndex, Data<String, Number> item) {
        }

        @Override
        protected void dataItemRemoved(Data<String, Number> item, Series<String, Number> series) {
            XYChartShim.removeDataItemFromDisplay(this, series, item);
        }

        @Override
        protected void dataItemChanged(Data<String, Number> item) {
        }

        @Override
        protected void seriesAdded(Series<String, Number> series, int seriesIndex) {
        }

        @Override
        protected void seriesRemoved(Series<String, Number> series) {
            XYChartShim.removeSeriesFromDisplay(this, series);
        }

        @Override
        protected void layoutPlotChildren() {
        }
    }

    @Test
    public void testModifyData() {
        LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());
        lineChart.getData().add(new XYChart.Series<>());
        XYChart.Data<Number, Number> data = new XYChart.Data<>(1, 1, 1);
        lineChart.getData().get(0).getData().add(data);
        assertEquals(1, data.getXValue());
        assertEquals(1, XYChartShim.Data_getCurrentX(data));
        assertEquals(1, data.getYValue());
        assertEquals(1, XYChartShim.Data_getCurrentY(data));
        assertEquals(1, data.getExtraValue());
        assertEquals(1, XYChartShim.Data_getCurrentExtraValue(data));
        data.setXValue(2);
        data.setYValue(2);
        data.setExtraValue(2);
        assertEquals(2, data.getXValue());
        assertEquals(2, XYChartShim.Data_getCurrentX(data));
        assertEquals(2, data.getYValue());
        assertEquals(2, XYChartShim.Data_getCurrentY(data));
        assertEquals(2, data.getExtraValue());
        assertEquals(2, XYChartShim.Data_getCurrentExtraValue(data));
    }
}
