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
import javafx.scene.Scene;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

public class HelloBubbleChart extends Application {

    @Override public void start(Stage stage) {
        stage.setTitle("Hello BubbleChart");
        Scene scene = new Scene(createBubbleChart(), 500, 500);
        stage.setScene(scene);
        stage.show();
    }

    private Chart createBubbleChart() {
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        final BubbleChart<Number,Number> bc = new BubbleChart<Number,Number>(xAxis,yAxis);
        bc.setTitle("Bubble Chart Example");
        xAxis.setLabel("X Axis");
        yAxis.setLabel("Y Axis");
        // add starting data
        XYChart.Series series1 = new XYChart.Series();
        series1.setName("Data Series 1");
        XYChart.Series series2 = new XYChart.Series();
        series2.setName("Data Series 2");
        for (int i=0; i<10; i++) series1.getData().add(new XYChart.Data(Math.random()*100, Math.random()*100, (Math.random()*10)));
        bc.getData().add(series1);
        return bc;
    }

     /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
