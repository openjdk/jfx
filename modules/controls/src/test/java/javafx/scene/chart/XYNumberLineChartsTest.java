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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class XYNumberLineChartsTest extends XYNumberChartsTestBase {
    private Class chartClass;
    private int seriesFadeOutTime;

    @Parameterized.Parameters
    public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { AreaChart.class, 400 },
            { LineChart.class, 900 },
            { StackedAreaChart.class, 400 }
        });
    }

    public XYNumberLineChartsTest(Class chartClass, int seriesFadeOutTime) {
        this.chartClass = chartClass;
        this.seriesFadeOutTime = seriesFadeOutTime;
    }

    @Override
    protected Chart createChart() {
        try {
            chart = (XYChart<Number, Number>) chartClass.getConstructor(Axis.class, Axis.class).
                newInstance(new NumberAxis(), new NumberAxis());
            Method setCreateSymbolsMethod = chartClass.getMethod("setCreateSymbols", Boolean.TYPE);
            setCreateSymbolsMethod.invoke(chart, false);
        } catch (InvocationTargetException e) {
            throw new AssertionError(e.getCause());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return chart;
    }

    @Test
    public void testSeriesClearAnimatedWithoutSymbols_rt_40632() {
        checkSeriesClearAnimated_rt_40632();
    }

    @Test
    public void testSeriesRemoveWithoutSymbols() {
        // 1 area group
        checkSeriesRemove(1);
    }

    @Test
    public void testSeriesRemoveWithoutSymbolsAnimated_rt_22124() {
        startAppWithSeries();
        // 1 area group
        assertEquals(1, chart.getPlotChildren().size());

        chart.setAnimated(true);
        ControlTestUtils.runWithExceptionHandler(() -> {
            // tests RT-22124
            chart.getData().remove(0);
        });
        toolkit.setAnimationTime(seriesFadeOutTime/2);
        assertEquals(1, chart.getPlotChildren().size());
        // tests RT-46086
        assertEquals(0.5, chart.getPlotChildren().get(0).getOpacity(), 0.0);
        toolkit.setAnimationTime(seriesFadeOutTime);
        assertEquals(0, chart.getPlotChildren().size());
    }

    @Test
    public void testDataWithoutSymbolsAddWithAnimation_rt_39353() {
        startAppWithSeries();
        chart.setAnimated(true);
        series.getData().add(new XYChart.Data<>(30, 30));
        ControlTestUtils.runWithExceptionHandler(() -> {
            toolkit.setAnimationTime(0);
        });
    }
}
