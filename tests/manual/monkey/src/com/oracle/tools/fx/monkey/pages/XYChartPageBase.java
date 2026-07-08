/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Side;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.FontOption;
import com.oracle.tools.fx.monkey.options.IntOption;
import com.oracle.tools.fx.monkey.options.PaintOption;
import com.oracle.tools.fx.monkey.options.TextOption;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Base Class For XYCharts.
 */
public abstract class XYChartPageBase extends TestPaneBase {

    public abstract XYChart<?, Number> chart();

    protected static final Random random = new Random();
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

    protected double randomValue() {
        return random.nextInt(50) - 25;
    }

    protected Series<Number, Number> createNumberSeries() {
        String name = "S" + (seq++);
        XYChart.Series s = new XYChart.Series();
        s.setName(name);
        for (int i = 0; i < 12; i++) {
            double v = randomValue();
            s.getData().add(new XYChart.Data(i, v));
        }
        return s;
    }

    protected Series<String, Number> createBarSeries() {
        String name = "S" + (seq++);
        XYChart.Series s = new XYChart.Series();
        s.setName(name);
        for (int i = 0; i < 12; i++) {
            double v = randomValue();
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
        boolean atIndexZero = random.nextBoolean();

        if (chart() instanceof BarChart b) {
            double v = randomValue();
            add(s.getData(), atIndexZero, new XYChart.Data("c" + sz, v));
        } else if (chart() instanceof StackedBarChart b) {
            double v = randomValue();
            add(s.getData(), atIndexZero, new XYChart.Data("c" + sz, v));
        } else {
            int i = random.nextInt(100);
            double v = randomValue();
            add(s.getData(), atIndexZero, new XYChart.Data(i, v));
        }
    }

    public final void removePoint() {
        var data = chart().getData();
        if (data.size() > 0) {
            XYChart.Series s = data.get(0);
            int sz = s.getData().size();
            if (sz > 0) {
                int ix = random.nextBoolean() ? 0 : sz - 1;
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
            double v = randomValue();
            ((XYChart.Data)s.getData().get(1)).setXValue("c" + v);
        } else if (chart() instanceof StackedBarChart b) {
            double v = randomValue();
            ((XYChart.Data)s.getData().get(1)).setXValue("c" + v);
        } else {
            double v = randomValue();
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
            double v = randomValue();
            ((XYChart.Data)s.getData().get(1)).setYValue(v);
        } else if (chart() instanceof StackedBarChart b) {
            double v = randomValue();
            ((XYChart.Data)s.getData().get(1)).setYValue(v);
        } else {
            double v = randomValue();
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

    protected void categoryAxisOptions(String title, String prefix, OptionPane op, CategoryAxis a) {
        op.section(title);
        op.option("End Margin:", Options.doubleOption(prefix + "EndMargin", a.endMarginProperty()));
        op.option(new BooleanOption(prefix + "GapStartAndEnd", "gap start and end", a.gapStartAndEndProperty()));
        axisOptions(prefix, op, a);
    }

    protected void numberAxisOptions(String title, String prefix, OptionPane op, NumberAxis a) {
        op.section(title);
        op.option(new BooleanOption(prefix + "ForceZeroInRange", "force zero in range", a.forceZeroInRangeProperty()));
        op.option("Lower Bound:", Options.doubleOption(prefix + "LowerBound", a.lowerBoundProperty()));
        op.option("Minor Tick Count:", new IntOption(prefix + "MinorTickCount", 0, Integer.MAX_VALUE, a.minorTickCountProperty()));
        op.option("Minor Tick Length:", Options.doubleOption(prefix + "MinorTickLength", a.minorTickLengthProperty()));
        op.option("Tick Unit:", Options.doubleOption(prefix + "TickUnit", a.tickUnitProperty()));
        op.option(new BooleanOption(prefix + "MinorTickVisible", "minor tick visible", a.minorTickVisibleProperty()));
        // TODO setTickLabelFormatter(StringConverter<T>)
        op.option("Upper Bound:", Options.doubleOption(prefix + "UpperBound", a.upperBoundProperty()));
        axisOptions(prefix, op, a);
    }

    private void axisOptions(String prefix, OptionPane op, Axis<?> a) {
        op.option(new BooleanOption(prefix + "Animated", "animated", a.animatedProperty()));
        op.option(new BooleanOption(prefix + "AutoRanging", "auto ranging", a.autoRangingProperty()));
        op.option("Label:", new TextOption(prefix + "Label", a.labelProperty()));
        op.option("Side:", new EnumOption<>(prefix + "Side", Side.class, a.sideProperty()));
        op.option("Tick Label Fill:", new PaintOption(prefix + "TickLabelFill", a.tickLabelFillProperty()));
        op.option("Tick Label Font:", new FontOption(prefix + "TickLabelFont", false, a.tickLabelFontProperty()));
        op.option("Tick Label Gap:", Options.doubleOption(prefix + "TickLabelGap", a.tickLabelGapProperty()));
        op.option("Tick Label Rotation:", Options.doubleOption(prefix + "TickLabelRotation", a.tickLabelRotationProperty()));
        op.option(new BooleanOption(prefix + "TickLabelVisible", "tick label visible", a.tickLabelsVisibleProperty()));
        op.option("Tick Length:", Options.doubleOption(prefix + "TickLength", a.tickLengthProperty()));
        op.option(new BooleanOption(prefix + "TickMarkVisible", "tick mark visible", a.tickMarkVisibleProperty()));
    }
}
