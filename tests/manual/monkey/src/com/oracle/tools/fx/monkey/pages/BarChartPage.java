/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.PickResult;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.sheets.PropertiesMonitor;
import com.oracle.tools.fx.monkey.sheets.XYChartPropertySheet;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * Bar Chart Page.
 */
public class BarChartPage extends XYChartPageBase {
    private final BarChart<String, Number> chart;

    public BarChartPage() {
        super("BarChartPage");

        chart = new BarChart<>(createCategoryAxis("X Axis"), createNumberAxis("Y Axis")) {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };
        chart.setTitle("Bar Chart");
        FX.setPopupMenu(chart, this::createMenu);
        addSeries();

        OptionPane op = new OptionPane();
        op.section("BarChart");
        op.option("Bar Gap:", Options.gaps("barGap", chart.barGapProperty()));
        op.option("Category Gap:", Options.gaps("categoryGap", chart.categoryGapProperty()));
        XYChartPropertySheet.appendTo(this, op, chart);

        setContent(chart);
        setOptions(op);
    }

    ContextMenu createMenu(PickResult p) {
        Node nd = p.getIntersectedNode();

        Series<String, Number> s = findSeries(nd);
        XYChart.Data<String, Number> d = findData(s, nd);
        // FIX this is incorrect - styles remain after modifying the list
        // we may need to iterate over all the data (?) and query data.getNode() perhaps?
        // or maybe add a listener to each node??
        //System.out.println("s=" + s + " d=" + d + " p=" + p);// FIX

        ContextMenu m = new ContextMenu();

//        if ((s != null) && (d != null)) {
//            FX.item(m, "Delete Point", () -> {
//                s.getData().remove(d);
//            });
//            FX.separator(m);
//        }

        FX.item(m, "Add Duplicate Category", this::addDuplicateCategory);
        FX.item(m, "Add Series with Duplicate Category", this::addDuplicateSeries);
        FX.separator(m);
        FX.item(m, "Properties...", () -> PropertiesMonitor.open(nd));
        return m;
    }

    private Series<String,Number> findSeries(Node n) {
        try {
            if (n != null) {
                for (String s: n.getStyleClass()) {
                    if (s.startsWith("series")) {
                        s = s.substring("series".length());
                        int ix = Integer.parseInt(s);
                        return chart.getData().get(ix);
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private XYChart.Data<String,Number> findData(Series<String,Number> series, Node n) {
        if (series != null) {
            try {
                if (n != null) {
                    for (String s: n.getStyleClass()) {
                        if (s.startsWith("data")) {
                            s = s.substring("data".length());
                            int ix = Integer.parseInt(s);
                            return series.getData().get(ix);
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    void addDuplicateCategory() {
        var d = chart.getData();
        if (d.size() > 0) {
            var dd = d.get(0).getData();
            if (dd.size() > 0) {
                var v = dd.get(0);
                dd.add(new XYChart.Data(v.getXValue(), randomValue()));
            }
        }
    }

    void addDuplicateSeries() {
        ObservableList<XYChart.Data<String, Number>> list = FXCollections.observableArrayList();
        list.add(new XYChart.Data<>("1", randomValue()));
        list.add(new XYChart.Data<>("1", randomValue()));
        list.add(new XYChart.Data<>("2", randomValue()));
        if (chart.getData().size() > 0) {
            chart.getData().getFirst().setData(list);
        }
    }

    @Override
    public XYChart<?, Number> chart() {
        return chart;
    }
}
