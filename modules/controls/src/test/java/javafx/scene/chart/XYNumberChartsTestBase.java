/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.infrastructure.ControlTestUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import static org.junit.Assert.assertEquals;

abstract public class XYNumberChartsTestBase extends XYChartTestBase {
    XYChart<Number, Number> chart;
    ObservableList<XYChart.Data<Number, Number>> seriesData = FXCollections.observableArrayList(
            new XYChart.Data<>(10, 10),
            new XYChart.Data<>(20, 20)
    );
    XYChart.Series<Number, Number> series = new XYChart.Series<>(seriesData);

    protected void startAppWithSeries() {
        chart.getData().addAll(series);
        startApp();
    }

    void checkSeriesClearAnimated_rt_40632() {
        startAppWithSeries();
        chart.setAnimated(true);
        ControlTestUtils.runWithExceptionHandler(() -> {
            series.getData().clear();
        });
    }

    void checkSeriesRemove(int expectedNodesCount) {
        startAppWithSeries();
        assertEquals(expectedNodesCount, chart.getPlotChildren().size());
        chart.getData().remove(0);
        pulse();
        assertEquals(0, chart.getPlotChildren().size());
    }
}
