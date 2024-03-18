/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.pages;

import java.util.Random;
import javafx.collections.ObservableList;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Base Class For XYCharts.
 */
public abstract class XYChartPageBase extends TestPaneBase {

    public abstract XYChart<?, Number> chart();

    protected static final Random rnd = new Random();
    private int seq;

    public XYChartPageBase(String name) {
        super(name);
    }
    
    protected CategoryAxis createCategoryAxis(String text) {
        CategoryAxis a = new CategoryAxis();
        a.setLabel(text);
        return a;
    }

    protected NumberAxis createNumberAxis(String text) {
        NumberAxis a = new NumberAxis();
        a.setLabel(text);
        return a;
    }

    protected Series<Number, Number> createNumberSeries() {
        String name = "S" + (seq++);
        XYChart.Series s = new XYChart.Series();
        s.setName(name);
        for (int i = 0; i < 12; i++) {
            int v = rnd.nextInt(50);
            s.getData().add(new XYChart.Data(i, v));
        }
        return s;
    }

    protected Series<String, Number> createBarSeries() {
        String name = "S" + (seq++);
        XYChart.Series s = new XYChart.Series();
        s.setName(name);
        for (int i = 0; i < 12; i++) {
            int v = rnd.nextInt(50);
            s.getData().add(new XYChart.Data("c" + i, v));
        }
        return s;
    }

    public final void addSeries() {
        if (chart() instanceof BarChart b) {
            Series s = createBarSeries();
            b.getData().add(s);
        } else if (chart() instanceof StackedBarChart b) {
            Series s = createBarSeries();
            b.getData().add(s);
        } else {
            Series s = createNumberSeries();
            chart().getData().add(s);
        }
    }

    public final void removeSeries() {
        if (chart().getData().size() > 0) {
            chart().getData().remove(0);
        }
    }

    public final void addRemoveSeries() {
        var data = chart().getData();
        if (data.size() > 0) {
            var first = data.remove(0);
            chart().getData().add((Series)first);
        }
    }

    public final void addPoint() {
        var list = chart().getData();
        if (list.size() == 0) {
            chart().getData().add(new XYChart.Series());
        }
        XYChart.Series s = list.get(0);
        int sz = s.getData().size();
        boolean atIndexZero = rnd.nextBoolean();

        if (chart() instanceof BarChart b) {
            int v = rnd.nextInt(50);
            add(s.getData(), atIndexZero, new XYChart.Data("c" + sz, v));
        } else if (chart() instanceof StackedBarChart b) {
            int v = rnd.nextInt(50);
            add(s.getData(), atIndexZero, new XYChart.Data("c" + sz, v));
        } else {
            int i = rnd.nextInt(100);
            int v = rnd.nextInt(50);
            add(s.getData(), atIndexZero, new XYChart.Data(i, v));
        }
    }

    public final void removePoint() {
        var data = chart().getData();
        if (data.size() > 0) {
            XYChart.Series s = data.get(0);
            int sz = s.getData().size();
            if (sz > 0) {
                int ix = rnd.nextBoolean() ? 0 : sz - 1;
                s.getData().remove(ix);
            }
        }
    }

    public final void clearPoints() {
        var data = chart().getData();
        if (data.size() > 0) {
            XYChart.Series s = data.get(0);
            s.getData().clear();
        }
    }

    public final void changePointX() {
        var data = chart().getData();
        if (data.size() == 0) {
            return;
        }
        XYChart.Series s = data.get(0);
        int sz = s.getData().size();
        if(sz < 3) {
            return;
        }

        if (chart() instanceof BarChart b) {
            int v = rnd.nextInt(50);
            ((XYChart.Data)s.getData().get(1)).setXValue("c" + v);
        } else if (chart() instanceof StackedBarChart b) {
            int v = rnd.nextInt(50);
            ((XYChart.Data)s.getData().get(1)).setXValue("c" + v);
        } else {
            int v = rnd.nextInt(50);
            ((XYChart.Data)s.getData().get(1)).setXValue(v);
        }
    }

    public final void changePointY() {
        var list = chart().getData();
        if (list.size() == 0) {
            return;
        }
        XYChart.Series s = list.get(0);
        int sz = s.getData().size();
        if(sz < 3) {
            return;
        }

        if (chart() instanceof BarChart b) {
            int v = rnd.nextInt(50);
            ((XYChart.Data)s.getData().get(1)).setYValue(v);
        } else if (chart() instanceof StackedBarChart b) {
            int v = rnd.nextInt(50);
            ((XYChart.Data)s.getData().get(1)).setYValue(v);
        } else {
            int v = rnd.nextInt(50);
            ((XYChart.Data)s.getData().get(1)).setYValue(v);
        }
    }

    private void add(ObservableList list, boolean atIndexZero, XYChart.Data d) {
        if (atIndexZero) {
            list.add(0, d);
        } else {
            list.add(d);
        }
    }
}
