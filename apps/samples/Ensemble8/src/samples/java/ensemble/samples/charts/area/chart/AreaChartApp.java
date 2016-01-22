/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ensemble.samples.charts.area.chart;


import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.stage.Stage;


/**
 * A chart that fills in the area between a line of data points and the axes.
 * Good for comparing accumulated totals over time.
 *
 * @sampleName Area Chart
 * @preview preview.png
 * @see javafx.scene.chart.AreaChart
 * @see javafx.scene.chart.NumberAxis
 * @related /Charts/Line/Line Chart
 * @related /Charts/Scatter/Scatter Chart
 * @docUrl https://docs.oracle.com/javafx/2/charts/jfxpub-charts.htm Using JavaFX Charts Tutorial
 * @playground chart.data
 * @playground - (name="xAxis")
 * @playground xAxis.autoRanging
 * @playground xAxis.forceZeroInRange
 * @playground xAxis.lowerBound (min=-10, max=10, step=1)
 * @playground xAxis.upperBound (min=-10, max=10, step=1)
 * @playground xAxis.tickUnit (max=10, step=0.5)
 * @playground xAxis.minorTickCount (max=16)
 * @playground xAxis.minorTickLength (max=15)
 * @playground xAxis.minorTickVisible
 * @playground xAxis.animated
 * @playground xAxis.label
 * @playground xAxis.side
 * @playground xAxis.tickLabelFill
 * @playground xAxis.tickLabelGap
 * @playground xAxis.tickLabelRotation (min=-180,max=180,step=1)
 * @playground xAxis.tickLabelsVisible
 * @playground xAxis.tickLength
 * @playground xAxis.tickMarkVisible
 * @playground - (name="yAxis")
 * @playground yAxis.autoRanging
 * @playground yAxis.forceZeroInRange
 * @playground yAxis.lowerBound (min=-10, max=10, step=1)
 * @playground yAxis.upperBound (min=-10, max=10, step=1)
 * @playground yAxis.tickUnit (max=10, step=1)
 * @playground yAxis.minorTickCount (max=16)
 * @playground yAxis.minorTickLength (max=15)
 * @playground yAxis.minorTickVisible
 * @playground yAxis.animated
 * @playground yAxis.label
 * @playground yAxis.side
 * @playground yAxis.tickLabelFill
 * @playground yAxis.tickLabelGap
 * @playground yAxis.tickLabelRotation (min=-180,max=180)
 * @playground yAxis.tickLabelsVisible
 * @playground yAxis.tickLength
 * @playground yAxis.tickMarkVisible
 * @playground - (name="chart")
 * @playground chart.horizontalGridLinesVisible
 * @playground chart.horizontalZeroLineVisible
 * @playground chart.verticalGridLinesVisible
 * @playground chart.verticalZeroLineVisible
 * @playground chart.animated
 * @playground chart.legendSide
 * @playground chart.legendVisible
 * @playground chart.title
 * @playground chart.titleSide
 * @embedded
 */
public class AreaChartApp extends Application {

    private AreaChart chart;
    private NumberAxis xAxis;
    private NumberAxis yAxis;

    public Parent createContent() {
        xAxis = new NumberAxis();
        xAxis.setLabel("X Values");
        yAxis = new NumberAxis();
        yAxis.setLabel("Y Values");
        ObservableList<AreaChart.Series> areaChartData = FXCollections.observableArrayList(
                new AreaChart.Series("Series 1",FXCollections.observableArrayList(
                    new AreaChart.Data(0,4),
                    new AreaChart.Data(2,5),
                    new AreaChart.Data(4,4),
                    new AreaChart.Data(6,2),
                    new AreaChart.Data(8,6),
                    new AreaChart.Data(10,8)
                )),
                new AreaChart.Series("Series 2", FXCollections.observableArrayList(
                    new AreaChart.Data(0,8),
                    new AreaChart.Data(2,2),
                    new AreaChart.Data(4,9),
                    new AreaChart.Data(6,7),
                    new AreaChart.Data(8,5),
                    new AreaChart.Data(10,7)
                )),
                new AreaChart.Series("Series 3", FXCollections.observableArrayList(
                    new AreaChart.Data(0,2),
                    new AreaChart.Data(2,5),
                    new AreaChart.Data(4,8),
                    new AreaChart.Data(6,6),
                    new AreaChart.Data(8,9),
                    new AreaChart.Data(10,7)
                ))
        );
        chart = new AreaChart(xAxis, yAxis, areaChartData);
        return chart;
    }


    @Override public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
