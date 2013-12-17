/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

public class HelloAreaChart extends Application {

    @Override public void start(Stage stage) {
        stage.setTitle("Hello AreaChart");
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        final AreaChart<Number,Number> ac = new AreaChart<Number,Number>(xAxis,yAxis);
         xAxis.setLabel("X Axis");
        yAxis.setLabel("Y Axis");
        ac.setTitle("HelloAreaChart");
//      // add starting data
        ObservableList<XYChart.Data> data = FXCollections.observableArrayList();
        XYChart.Series series = new XYChart.Series();
        series.setName("Data Series 1");
//        for (int i=0; i<10; i++) series.getData().add(new XYChart.Data(Math.random()*100, Math.random()*100));
        series.getData().add(new XYChart.Data(20d, 50d));
        series.getData().add(new XYChart.Data(40d, 80d));
        series.getData().add(new XYChart.Data(50d, 90d));
        series.getData().add(new XYChart.Data(70d, 30d));
        series.getData().add(new XYChart.Data(90d, 20d));
        Scene scene  = new Scene(ac,800,600);
        ac.getData().add(series);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
