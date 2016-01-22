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

package ensemble.samples.charts.scatter.chart;


import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.NumberAxis;
import javafx.stage.Stage;


/**
 * A chart that displays plotted symbols for a series of data points. Useful
 * for viewing the distribution of data to see if there is any pattern,
 * indicating a correlation.
 *
 * @sampleName Scatter Chart
 * @preview preview.png
 * @see javafx.scene.chart.ScatterChart
 * @see javafx.scene.chart.NumberAxis
 * @related /Charts/Line/Line Chart
 * @related /Charts/Area/Area Chart
 * @docUrl https://docs.oracle.com/javafx/2/charts/jfxpub-charts.htm Using JavaFX Charts Tutorial
 * @playground chart.data
 * @playground - (name="xAxis")
 * @playground xAxis.autoRanging
 * @playground xAxis.forceZeroInRange
 * @playground xAxis.lowerBound (min=-10, max=10, step=0.25)
 * @playground xAxis.upperBound (max=20, step=0.25)
 * @playground xAxis.tickUnit (max=10, step=0.25)
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
 * @playground yAxis.lowerBound (min=-5,max=5,step=0.25)
 * @playground yAxis.upperBound (max=10,step=0.25)
 * @playground yAxis.tickUnit (max=10,step=0.25)
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
 * @embedded
 */
public class ScatterChartApp extends Application {

    private ScatterChart chart;
    private NumberAxis xAxis;
    private NumberAxis yAxis;

    public Parent createContent() {
        xAxis = new NumberAxis("X-Axis", 0d, 8.0d, 1.0d);
        yAxis = new NumberAxis("Y-Axis", 0.0d, 5.0d, 1.0d);
        ObservableList<XYChart.Series> data = FXCollections.observableArrayList(
            new ScatterChart.Series("Series 1", FXCollections.<ScatterChart.Data>observableArrayList(
                new XYChart.Data(0.2, 3.5),
                new XYChart.Data(0.7, 4.6),
                new XYChart.Data(1.8, 1.7),
                new XYChart.Data(2.1, 2.8),
                new XYChart.Data(4.0, 2.2),
                new XYChart.Data(4.1, 2.6),
                new XYChart.Data(4.5, 2.0),
                new XYChart.Data(6.0, 3.0),
                new XYChart.Data(7.0, 2.0),
                new XYChart.Data(7.8, 4.0)
            ))
        );
        chart = new ScatterChart(xAxis, yAxis, data);
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
