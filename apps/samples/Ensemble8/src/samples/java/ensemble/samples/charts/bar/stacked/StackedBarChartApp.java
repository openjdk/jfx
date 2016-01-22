/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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
package ensemble.samples.charts.bar.stacked;


import javafx.application.Application;
import static javafx.collections.FXCollections.*;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.stage.Stage;


/**
 * A sample that displays data in a stacked bar chart.
 *
 * @sampleName Stacked Bar Chart
 * @preview preview.png
 * @see javafx.scene.chart.StackedBarChart
 * @see javafx.scene.chart.CategoryAxis
 * @see javafx.scene.chart.NumberAxis
 * @related /Charts/Bar/Bar Chart
 * @playground chart.data
 * @playground - (name="xAxis")
 * @playground xAxis.autoRanging
 * @playground xAxis.gapStartAndEnd
 * @playground xAxis.startMargin
 * @playground xAxis.endMargin
 * @playground xAxis.animated
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
 * @playground yAxis.lowerBound (min=-3000,max=0,step=1)
 * @playground yAxis.upperBound (min=0,max=8000,step=1)
 * @playground yAxis.tickUnit (max=5000,step=1)
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
 */
public class StackedBarChartApp extends Application {

    private StackedBarChart chart;
    private CategoryAxis xAxis;
    private NumberAxis yAxis;

    public Parent createContent() {
        String[] years = {"2007", "2008", "2009"};
        xAxis = new CategoryAxis(observableArrayList(years));
        yAxis = new NumberAxis("Units Sold", 0.0d, 10000.0d, 1000.0d);

        ObservableList<StackedBarChart.Series> barChartData =
                observableArrayList(
                    new StackedBarChart.Series("Region 1",
                        observableArrayList(
                            new StackedBarChart.Data(years[0], 567d),
                            new StackedBarChart.Data(years[1], 1292d),
                            new StackedBarChart.Data(years[2], 1292d)
                        )
                    ),
                    new StackedBarChart.Series("Region 2",
                        observableArrayList(
                            new StackedBarChart.Data(years[0], 956),
                            new StackedBarChart.Data(years[1], 1665),
                            new StackedBarChart.Data(years[2], 2559)
                        )
                    ),
                    new StackedBarChart.Series("Region 3",
                        observableArrayList(
                            new StackedBarChart.Data(years[0], 1154),
                            new StackedBarChart.Data(years[1], 1927),
                            new StackedBarChart.Data(years[2], 2774)
                        )
                    )
                );

        chart = new StackedBarChart(xAxis, yAxis, barChartData, 25.0d);
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
