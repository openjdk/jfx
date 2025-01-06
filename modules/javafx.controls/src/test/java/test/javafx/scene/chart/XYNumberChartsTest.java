/*
 * Copyright (c) 2015, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.provider.Arguments;

public class XYNumberChartsTest extends XYNumberChartsTestBase {

    private static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(AreaChart.class, 1),
            Arguments.of(BubbleChart.class, 0),
            Arguments.of(LineChart.class, 1),
            Arguments.of(ScatterChart.class, 0),
            Arguments.of(StackedAreaChart.class, 1)
        );
    }

    @Override
    protected void createChart() {
        // will be using createChart() below
    }

    protected void createChart(Class<?> chartClass, int nodesPerSeries) {
        try {
            chart = (XYChart<Number, Number>)chartClass.getConstructor(Axis.class, Axis.class).newInstance(new NumberAxis(), new NumberAxis());
            chart.setAnimated(false);
        } catch (InvocationTargetException e) {
            throw new AssertionError(e.getCause());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected Chart getChart() {
        return chart;
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSeriesClearAnimated_rt_40632(Class<?> chartClass, int nodesPerSeries) {
        createChart(chartClass, nodesPerSeries);
        checkSeriesClearAnimated_rt_40632();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSeriesRemove(Class<?> chartClass, int nodesPerSeries) {
        createChart(chartClass, nodesPerSeries);
        checkSeriesRemove(seriesData.size() + nodesPerSeries);
    }

    @Override
    void checkSeriesStyleClasses(XYChart.Series<?, ?> series, int seriesIndex, int colorIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    void checkDataStyleClasses(XYChart.Data<?, ?> data, int seriesIndex, int dataIndex, int colorIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
