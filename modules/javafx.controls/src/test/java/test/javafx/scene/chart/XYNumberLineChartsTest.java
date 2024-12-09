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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChartShim;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils;

public class XYNumberLineChartsTest extends XYNumberChartsTestBase {

    private static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(AreaChart.class, 400, 800),
            Arguments.of(LineChart.class, 900, 500),
            Arguments.of(StackedAreaChart.class, 400, 800)
        );
    }

    @Override
    protected void createChart() {
        // will be using createChart() below
    }

    protected void createChart(Class<?> chartClass, int seriesFadeOutTime, int dataFadeOutTime) {
        try {
            chart = (XYChart<Number, Number>) chartClass.getConstructor(Axis.class, Axis.class).newInstance(new NumberAxis(), new NumberAxis());
            Method setCreateSymbolsMethod = chartClass.getMethod("setCreateSymbols", Boolean.TYPE);
            setCreateSymbolsMethod.invoke(chart, false);
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
    public void testSeriesClearAnimatedWithoutSymbols_rt_40632(Class<?> chartClass, int seriesFadeOutTime, int dataFadeOutTime) {
        createChart(chartClass, seriesFadeOutTime, dataFadeOutTime);
        checkSeriesClearAnimated_rt_40632();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSeriesRemoveWithoutSymbols(Class<?> chartClass, int seriesFadeOutTime, int dataFadeOutTime) {
        createChart(chartClass, seriesFadeOutTime, dataFadeOutTime);
        // 1 area group
        checkSeriesRemove(1);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSeriesRemoveWithoutSymbolsAnimated_rt_22124(Class<?> chartClass, int seriesFadeOutTime, int dataFadeOutTime) {
        createChart(chartClass, seriesFadeOutTime, dataFadeOutTime);
        startAppWithSeries();
        // 1 area group
        assertEquals(1, XYChartShim.getPlotChildren(chart).size());

        chart.setAnimated(true);
        ControlTestUtils.runWithExceptionHandler(() -> {
            // tests JDK-8117583
            chart.getData().remove(0);
        });
        toolkit.setAnimationTime(seriesFadeOutTime/2);
        assertEquals(1, XYChartShim.getPlotChildren(chart).size());
        // tests JDK-8095927
        assertEquals(0.5, XYChartShim.getPlotChildren(chart).get(0).getOpacity(), 0.0);
        toolkit.setAnimationTime(seriesFadeOutTime);
        assertEquals(0, XYChartShim.getPlotChildren(chart).size());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testDataWithoutSymbolsAddWithAnimation_rt_39353(Class<?> chartClass, int seriesFadeOutTime, int dataFadeOutTime) {
        createChart(chartClass, seriesFadeOutTime, dataFadeOutTime);
        startAppWithSeries();
        chart.setAnimated(true);
        series.getData().add(new XYChart.Data<>(30, 30));
        ControlTestUtils.runWithExceptionHandler(() -> {
            toolkit.setAnimationTime(0);
        });
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSeriesClearWithoutSymbolsAnimated_8150264(Class<?> chartClass, int seriesFadeOutTime, int dataFadeOutTime) {
        createChart(chartClass, seriesFadeOutTime, dataFadeOutTime);
        startAppWithSeries();
        assertEquals(3, XYChartShim.Series_getDataSize(series));

        chart.setAnimated(true);
        series.getData().remove(0);
        toolkit.setAnimationTime(dataFadeOutTime/2);
        assertEquals(3, XYChartShim.Series_getDataSize(series));
        toolkit.setAnimationTime(dataFadeOutTime);
        assertEquals(2, XYChartShim.Series_getDataSize(series));

        series.getData().clear();
        toolkit.setAnimationTime(dataFadeOutTime);
        // removed instantly
        assertEquals(0, XYChartShim.Series_getDataSize(series));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testMinorTicksMatchMajorTicksAfterAnimation(Class<?> chartClass, int seriesFadeOutTime, int dataFadeOutTime) {
        createChart(chartClass, seriesFadeOutTime, dataFadeOutTime);
        startAppWithSeries();
        chart.setAnimated(true);
        // increase ranges, starting axis animation
        chart.getData().getFirst().getData().add(new XYChart.Data<>(100, 100));
        pulse();
        // forward time until after animation is finished
        toolkit.setAnimationTime(1000);
        NumberAxis yAxis = (NumberAxis)chart.getYAxis();

        List<Double> majorTickYValues = yAxis.getChildrenUnmodifiable().stream()
                .filter(obj -> obj instanceof Path && obj.getStyleClass().contains("axis-tick-mark"))
                .flatMap(obj -> ((Path) obj).getElements().stream())
                .filter(path -> path instanceof MoveTo)
                .map(moveTo -> ((MoveTo) moveTo).getY())
                .toList();

        List<Double> minorTickYValues = yAxis.getChildrenUnmodifiable().stream()
                .filter(obj -> obj instanceof Path && obj.getStyleClass().contains("axis-minor-tick-mark"))
                .flatMap(obj -> ((Path) obj).getElements().stream())
                .filter(path -> path instanceof MoveTo)
                .map(moveTo -> ((MoveTo) moveTo).getY())
                .toList();

        double majorTickSpacing = majorTickYValues.get(1) - majorTickYValues.get(0);
        double minorTickSpacing = minorTickYValues.get(1) - minorTickYValues.get(0);

        double delta = 0.001;
        assertEquals(5, yAxis.getMinorTickCount());
        assertEquals(5, majorTickSpacing / minorTickSpacing, delta);
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
