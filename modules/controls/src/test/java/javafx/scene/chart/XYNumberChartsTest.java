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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class XYNumberChartsTest extends XYNumberChartsTestBase {
    private Class chartClass;
    int nodesPerSeries;

    @Parameterized.Parameters
    public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { AreaChart.class, 1, },
            { BubbleChart.class, 0, },
            { LineChart.class, 1, },
            { ScatterChart.class, 0, },
            { StackedAreaChart.class, 1, },
        });
    }

    public XYNumberChartsTest(Class chartClass, int nodesPerSeries) {
        this.chartClass = chartClass;
        this.nodesPerSeries = nodesPerSeries;
    }

    @Override
    protected Chart createChart() {
        try {
            chart = (XYChart<Number, Number>) chartClass.getConstructor(Axis.class, Axis.class).
                newInstance(new NumberAxis(), new NumberAxis());
        } catch (InvocationTargetException e) {
            throw new AssertionError(e.getCause());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return chart;
    }

    @Test
    public void testSeriesClearAnimated_rt_40632() {
        checkSeriesClearAnimated_rt_40632();
    }

    @Test
    public void testSeriesRemove() {
        checkSeriesRemove(2 + nodesPerSeries);
    }
}
