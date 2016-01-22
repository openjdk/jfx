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
package ensemble.samples.charts.area.stacked;


import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.stage.Stage;


/**
 * A sample that displays data in a stacked area chart.
 *
 * @sampleName Stacked Area Chart
 * @preview preview.png
 * @see javafx.scene.chart.StackedAreaChart
 * @see javafx.scene.chart.NumberAxis
 * @related /Charts/Area/Area Chart
 * @docUrl https://docs.oracle.com/javafx/2/charts/jfxpub-charts.htm Using JavaFX Charts Tutorial
 * @playground chart.data
 * @playground - (name="xAxis")
 * @playground xAxis.autoRanging
 * @playground xAxis.forceZeroInRange
 * @playground xAxis.lowerBound (min=-10, max=10,step=1)
 * @playground xAxis.upperBound (min=-10, max=10,step=1)
 * @playground xAxis.tickUnit (max=10)
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
 * @playground yAxis.lowerBound (min=-10, max=20,step=1)
 * @playground yAxis.upperBound (min=-10, max=40,step=1)
 * @playground yAxis.tickUnit (max=10)
 * @playground yAxis.minorTickCount (max=16)
 * @playground yAxis.minorTickLength (max=15)
 * @playground yAxis.minorTickVisible
 * @playground yAxis.animated
 * @playground yAxis.label
 * @playground yAxis.side
 * @playground yAxis.tickLabelFill
 * @playground yAxis.tickLabelGap
 * @playground yAxis.tickLabelRotation (min=-180,max=180,step=1)
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
 * @highlight
 */
public class StackedAreaChartApp extends Application {

    private StackedAreaChart chart;
    private NumberAxis xAxis;
    private NumberAxis yAxis;

    public Parent createContent() {
        xAxis = new NumberAxis("X Values", 1.0d, 9.0d, 2.0d);
        yAxis = new NumberAxis("Y Values", 0.0d, 30.0d, 2.0d);

        ObservableList<StackedAreaChart.Series> areaChartData = FXCollections.observableArrayList(
                new StackedAreaChart.Series("Series 1",FXCollections.observableArrayList(
                    new StackedAreaChart.Data(0,4),
                    new StackedAreaChart.Data(2,5),
                    new StackedAreaChart.Data(4,4),
                    new StackedAreaChart.Data(6,2),
                    new StackedAreaChart.Data(8,6),
                    new StackedAreaChart.Data(10,8)
                )),
                new StackedAreaChart.Series("Series 2", FXCollections.observableArrayList(
                    new StackedAreaChart.Data(0,8),
                    new StackedAreaChart.Data(2,2),
                    new StackedAreaChart.Data(4,9),
                    new StackedAreaChart.Data(6,7),
                    new StackedAreaChart.Data(8,5),
                    new StackedAreaChart.Data(10,7)
                )),
                new StackedAreaChart.Series("Series 3", FXCollections.observableArrayList(
                    new StackedAreaChart.Data(0,2),
                    new StackedAreaChart.Data(2,5),
                    new StackedAreaChart.Data(4,8),
                    new StackedAreaChart.Data(6,6),
                    new StackedAreaChart.Data(8,9),
                    new StackedAreaChart.Data(10,7)
                ))
        );
        chart = new StackedAreaChart(xAxis, yAxis, areaChartData);
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
