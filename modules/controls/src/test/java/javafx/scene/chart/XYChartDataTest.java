/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.chart;

import java.util.Comparator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class XYChartDataTest {

    @Test public void creatingDataShouldSetValuesAndCurrentValues() {
        XYChart.Data<Number,Number> data = new XYChart.Data<Number,Number>(10, 20);
        assertEquals(10, data.getXValue().longValue());
        assertEquals(10, data.getCurrentX().longValue());
        assertEquals(20, data.getYValue().longValue());
        assertEquals(20, data.getCurrentY().longValue());
    }

    @Ignore("Waiting on fix for RT-13478")
    @Test public void updatingValuesBeforeAddingToASeriesShouldUpdateValuesAndCurrentValues() {
        XYChart.Data<Number,Number> data = new XYChart.Data<Number,Number>(10, 20);
        data.setXValue(100);
        data.setYValue(200);
        assertEquals(100, data.getXValue().longValue());
        assertEquals(100, data.getCurrentX().longValue());
        assertEquals(200, data.getYValue().longValue());
        assertEquals(200, data.getCurrentY().longValue());
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
        FXCollections.sort(list, new Comparator<XYChart.Data<Number, Number>>() {
            @Override
            public int compare(XYChart.Data<Number, Number> o1, XYChart.Data<Number, Number> o2) {
                return Double.compare(o1.getXValue().intValue(), o2.getXValue().intValue());
            }
        });
        ObservableList<XYChart.Data<Number, Number>> data = series.getData();
        // check sorted data 
        assertEquals(1, data.get(0).getXValue());
        assertEquals(2, data.get(1).getXValue());
        assertEquals(3, data.get(2).getXValue());
        assertEquals(4, data.get(3).getXValue());
        
    }
}
