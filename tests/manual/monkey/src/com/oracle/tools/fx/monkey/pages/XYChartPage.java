/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Tests various XYCharts
 */
public class XYChartPage extends TestPaneBase {
    public enum Mode {
        AREA,
        BAR,
        BUBBLE,
        LINE,
        SCATTER,
        STACKED_AREA,
        STACKED_BAR,
    }

    private ComboBox<Mode> modeSelector;
    private XYChart<?, Number> chart;
    protected static Random rnd = new Random();

    public XYChartPage() {
        FX.name(this, "XYChartPage");

        modeSelector = new ComboBox<>();
        FX.name(modeSelector, "modeSelector");
        modeSelector.getItems().addAll(Mode.values());
        modeSelector.setEditable(false);
        modeSelector.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updateChart();
        });

        Button addButton = new Button("Add Series");
        addButton.setOnAction((ev) -> addSeries());

        Button removeButton = new Button("Remove Series");
        removeButton.setOnAction((ev) -> removeSeries());

        Button addRemoveButton = new Button("Add/Remove Series");
        addRemoveButton.setOnAction((ev) -> addRemoveSeries());
        
        Button addPointButton = new Button("Add Point");
        addPointButton.setOnAction((ev) -> addPoint());
        
        Button removePointButton = new Button("Remove Point");
        removePointButton.setOnAction((ev) -> removePoint());
        
        Button clearPointsButton = new Button("Clear Points");
        clearPointsButton.setOnAction((ev) -> clearPoints());

        OptionPane p = new OptionPane();
        p.label("Chart Type:");
        p.option(modeSelector);
        p.option(addButton);
        p.option(removeButton);
        p.option(addRemoveButton);
        p.option(addPointButton);
        p.option(removePointButton);
        p.option(clearPointsButton);
        setOptions(p);

        modeSelector.getSelectionModel().selectFirst();
    }

    private void updateChart() {
        Mode m = modeSelector.getSelectionModel().getSelectedItem();
        chart = createChart(m);

        BorderPane bp = new BorderPane();
        bp.setCenter(chart);
        setContent(bp);

        addSeries();
    }

    private void addSeries() {
        if (chart != null) {
            if (chart instanceof BarChart b) {
                Series s = createBarSeries();
                b.getData().add(s);
            } else if (chart instanceof StackedBarChart b) {
                Series s = createBarSeries();
                b.getData().add(s);
            } else {
                Series s = createNumberSeries();
                chart.getData().add(s);
            }
        }
    }

    private void removeSeries() {
        if (chart != null) {
            if (chart.getData().size() > 0) {
                chart.getData().remove(0);
            }
        }
    }

    private void addRemoveSeries() {
        if (chart != null) {
            if (chart.getData().size() > 0) {
                var first = chart.getData().remove(0);
                chart.getData().add((Series)first);
            }
        }
    }

    private XYChart<?, Number> createChart(Mode m) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("X");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Y");

        switch (m) {
        case AREA:
            {
                AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
                chart.setTitle("Area Chart");
                return chart;
            }
        case BAR:
            {
                CategoryAxis x = new CategoryAxis();
                BarChart<String, Number> chart = new BarChart<>(x, yAxis);
                chart.setTitle("Bar Chart");
                return chart;
            }
        case BUBBLE:
            {
                BubbleChart<Number, Number> chart = new BubbleChart<>(xAxis, yAxis);
                chart.setTitle("Bubble Chart");
                return chart;
            }
        case LINE:
            {
                LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
                chart.setTitle("Line Chart");
                return chart;
            }
        case SCATTER:
            {
                ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);
                chart.setTitle("Scatter Chart");
                return chart;
            }
        case STACKED_AREA:
            {
                StackedAreaChart<Number, Number> chart = new StackedAreaChart<>(xAxis, yAxis);
                chart.setTitle("Stacked Area Chart");
                return chart;
            }
        case STACKED_BAR:
            {
                CategoryAxis x = new CategoryAxis();
                StackedBarChart<String, Number> chart = new StackedBarChart<>(x, yAxis);
                chart.setTitle("Stacked Bar Chart");
                return chart;
            }
        }

        return null;
    }

    private Series<Number, Number> createNumberSeries() {
        String name = Long.toString(System.currentTimeMillis(), 16);

        XYChart.Series s = new XYChart.Series();
        s.setName(name);
        for (int i = 0; i < 12; i++) {
            int v = rnd.nextInt(50);
            s.getData().add(new XYChart.Data(i, v));
        }
        return s;
    }

    private Series<String, Number> createBarSeries() {
        String name = Long.toString(System.currentTimeMillis(), 16);

        XYChart.Series s = new XYChart.Series();
        s.setName(name);
        for (int i = 0; i < 12; i++) {
            int v = rnd.nextInt(50);
            s.getData().add(new XYChart.Data("c" + i, v));
        }
        return s;
    }

    private void addPoint() {
        var list = chart.getData();
        if (list.size() == 0) {
            chart.getData().add(new XYChart.Series());
        }
        XYChart.Series s = list.get(0);
        int sz = s.getData().size();
        boolean atIndexZero = rnd.nextBoolean();
        
        if (chart instanceof BarChart b) {
            int v = rnd.nextInt(50);
            add(s.getData(), atIndexZero, new XYChart.Data("c" + sz, v));
        } else if (chart instanceof StackedBarChart b) {
            int v = rnd.nextInt(50);
            add(s.getData(), atIndexZero, new XYChart.Data("c" + sz, v));
        } else {
            int i = rnd.nextInt(100);
            int v = rnd.nextInt(50);
            add(s.getData(), atIndexZero, new XYChart.Data(i, v));
        }
    }
    
    private void add(ObservableList list, boolean atIndexZero, XYChart.Data d) {
        if(atIndexZero) {
            list.add(0, d);
        } else {
            list.add(d);
        }
    }

    private void removePoint() {
        var list = chart.getData();
        if (list.size() > 0) {
            XYChart.Series s = list.get(0);
            int sz = s.getData().size();
            if (sz > 0) {
                int ix = rnd.nextBoolean() ? 0 : sz - 1;
                s.getData().remove(ix);
            }
        }
    }

    private void clearPoints() {
        var list = chart.getData();
        if (list.size() > 0) {
            XYChart.Series s = list.get(0);
            s.getData().clear();
        }
    }
}
