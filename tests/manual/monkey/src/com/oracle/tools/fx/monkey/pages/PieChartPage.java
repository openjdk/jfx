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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.layout.BorderPane;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.sheets.ChartPropertySheet;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * PieChart Page
 */
public class PieChartPage extends TestPaneBase {
    private final PieChart chart;

    public PieChartPage() {
        super("PieChartPage");

        chart = new PieChart() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };

        OptionPane op = new OptionPane();
        op.section("PieChart");
        op.option(new BooleanOption("clockwise", "clockwise", chart.clockwiseProperty()));
        op.option("Data:", createDataOptions("data", chart.getData()));
        // TODO add/remove buttons
        op.option("Label Line Length:", DoubleOption.of("labelLineLength", chart.labelLineLengthProperty(), -100, 0, 100));
        op.option(new BooleanOption("labelsVisible", "labels visible", chart.labelsVisibleProperty()));
        // TODO make this editable spinner?
        op.option("Start Angle:", DoubleOption.of("startAngle", chart.startAngleProperty(), -100, 0, 30, 45, 90, 120, 180, 270, 360));
        ChartPropertySheet.appendTo(op, chart);

        BorderPane bp = new BorderPane();
        bp.setCenter(chart);

        setContent(bp);
        setOptions(op);
    }

    private List<PieChart.Data> createData(int max) {
        Random rnd = new Random();
        int sz = rnd.nextInt(max);
        ArrayList<Data> a = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            a.add(new PieChart.Data("N" + i, rnd.nextDouble()));
        }
        return a;
    }

    private Node createDataOptions(String name, ObservableList<PieChart.Data> data) {
        ObjectSelector<List<PieChart.Data>> s = new ObjectSelector<>(name, (v) -> {
            data.setAll(v);
        });
        s.addChoiceSupplier("<30 Elements", () -> createData(30));
        s.addChoiceSupplier("<100 Elements", () -> createData(100));
        s.addChoiceSupplier("<3,000 Elements", () -> createData(3_000));
        s.addChoice("<empty>", List.of());
        s.selectFirst();
        return s;
    }
}
