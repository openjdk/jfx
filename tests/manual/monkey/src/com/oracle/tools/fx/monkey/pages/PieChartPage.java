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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * PieChart Page
 */
public class PieChartPage extends TestPaneBase {
    public enum Model {
        SMALL("Small"),
        LARGE("Large"),
        EMPTY("Empty"),
        ;
        private final String text;
        Model(String text) { this.text = text; }
        public String toString() { return text; }
    }

    private final ObservableList<PieChart.Data> data;
    private final ComboBox<Model> modelSelector;
    private PieChart chart;
    protected static Random rnd = new Random();

    public PieChartPage() {
        FX.name(this, "PieChartPage");

        data = FXCollections.observableArrayList();
        chart = new PieChart(data);

        modelSelector = new ComboBox<>();
        FX.name(modelSelector, "modelSelector");
        modelSelector.getItems().addAll(Model.values());
        modelSelector.setEditable(false);
        modelSelector.getSelectionModel().selectedItemProperty().addListener((x) -> {
            updateChart();
        });

        CheckBox animated = new CheckBox("animation");
        FX.name(animated, "animated");
        animated.selectedProperty().bindBidirectional(chart.animatedProperty());

        OptionPane p = new OptionPane();
        p.label("Model:");
        p.option(modelSelector);
        p.option(animated);
        setOptions(p);

        BorderPane bp = new BorderPane();
        bp.setCenter(chart);
        setContent(bp);

        modelSelector.getSelectionModel().selectFirst();
    }

    protected void updateChart() {
        Model m = modelSelector.getSelectionModel().getSelectedItem();
        List<PieChart.Data> d = createData(m);
        chart.getData().setAll(d);
    }

    private List<PieChart.Data> createData(Model m) {
        ArrayList<PieChart.Data> a = new ArrayList<>();
        switch (m) {
        case SMALL:
            addRandom(a, 30);
            break;
        case LARGE:
            addRandom(a, 3000);
            break;
        case EMPTY:
            break;
        default:
            throw new Error("?" + m);
        }
        return a;
    }

    private void addRandom(ArrayList<Data> a, int max) {
        Random r = new Random();
        int sz = r.nextInt(max);
        for (int i = 0; i < sz; i++) {
            a.add(new PieChart.Data("N" + i, r.nextDouble()));
        }
    }
}
