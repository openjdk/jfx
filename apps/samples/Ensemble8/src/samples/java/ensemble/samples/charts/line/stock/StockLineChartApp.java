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
package ensemble.samples.charts.line.stock;


import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import javafx.util.Duration;


/**
 * A simulated stock line chart.
 *
 * @sampleName Stock Line Chart
 * @preview preview.png
 * @see javafx.scene.chart.LineChart
 * @see javafx.scene.chart.NumberAxis
 * @docUrl https://docs.oracle.com/javafx/2/charts/jfxpub-charts.htm Using JavaFX Charts Tutorial
 */
public class StockLineChartApp extends Application {

    private LineChart<Number, Number> chart;
    private XYChart.Series<Number, Number> hourDataSeries;
    private XYChart.Series<Number, Number> minuteDataSeries;
    private NumberAxis xAxis;
    private Timeline animation;
    private double hours = 0;
    private double minutes = 0;
    private double timeInHours = 0;
    private double prevY = 10;
    private double y = 10;

    public StockLineChartApp() {
        // create timeline to add new data every 60th of second
        animation = new Timeline();
        animation.getKeyFrames()
                .add(new KeyFrame(Duration.millis(1000 / 60), (ActionEvent actionEvent) -> {
                     // 6 minutes data per frame
                    for (int count = 0; count < 6; count++) {
                        nextTime();
                        plotTime();
                    }
        }));
        animation.setCycleCount(Animation.INDEFINITE);
    }

    public Parent createContent() {
        xAxis = new NumberAxis(0, 24, 3);
        final NumberAxis yAxis = new NumberAxis(0, 100, 10);
        chart = new LineChart<>(xAxis, yAxis);
        // setup chart
        chart.getStylesheets().add(StockLineChartApp.class.getResource("StockLineChart.css").toExternalForm());
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setTitle("ACME Company Stock");
        xAxis.setLabel("Time");
        xAxis.setForceZeroInRange(false);
        yAxis.setLabel("Share Price");
        yAxis
                .setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, "$", null));
        // add starting data
        hourDataSeries = new XYChart.Series<>();
        hourDataSeries.setName("Hourly Data");
        minuteDataSeries = new XYChart.Series<>();
        minuteDataSeries.setName("Minute Data");
        // create some starting data
        hourDataSeries.getData()
                .add(new XYChart.Data<Number, Number>(timeInHours, prevY));
        minuteDataSeries.getData()
                .add(new XYChart.Data<Number, Number>(timeInHours, prevY));
        for (double m = 0; m < (60); m++) {
            nextTime();
            plotTime();
        }
        chart.getData().add(minuteDataSeries);
        chart.getData().add(hourDataSeries);
        return chart;
    }

    private void nextTime() {
        if (minutes == 59) {
            hours++;
            minutes = 0;
        } else {
            minutes++;
        }
        timeInHours = hours + ((1d / 60d) * minutes);
    }

    private void plotTime() {
        if ((timeInHours % 1) == 0) {
            // change of hour
            double oldY = y;
            y = prevY - 10 + (Math.random() * 20);
            prevY = oldY;
            while (y < 10 || y > 90) {
                y = y - 10 + (Math.random() * 20);
            }
            hourDataSeries.getData()
                    .add(new XYChart.Data<Number, Number>(timeInHours, prevY));
            // after 25hours delete old data
            if (timeInHours > 25) {
                hourDataSeries.getData().remove(0);
            }
            // every hour after 24 move range 1 hour
            if (timeInHours > 24) {
                xAxis.setLowerBound(xAxis.getLowerBound() + 1);
                xAxis.setUpperBound(xAxis.getUpperBound() + 1);
            }
        }
        double min = (timeInHours % 1);
        double randomPickVariance = Math.random();
        if (randomPickVariance < 0.3) {
            double minY = prevY + ((y - prevY) * min) - 4 + (Math.random() * 8);
            minuteDataSeries.getData().add(new XYChart.Data<Number, Number>(timeInHours, minY));
        } else if (randomPickVariance < 0.7) {
            double minY = prevY + ((y - prevY) * min) - 6 + (Math.random() * 12);
            minuteDataSeries.getData().add(new XYChart.Data<Number, Number>(timeInHours, minY));
        } else if (randomPickVariance < 0.95) {
            double minY = prevY + ((y - prevY) * min) - 10 + (Math.random() * 20);
            minuteDataSeries.getData().add(new XYChart.Data<Number, Number>(timeInHours, minY));
        } else {
            double minY = prevY + ((y - prevY) * min) - 15 + (Math.random() * 30);
            minuteDataSeries.getData().add(new XYChart.Data<Number, Number>(timeInHours, minY));
        }
        // after 25hours delete old data
        if (timeInHours > 25) {
            minuteDataSeries.getData().remove(0);
        }
    }

    public void play() {
        animation.play();
    }

    @Override
    public void stop() {
        animation.pause();
    }

    @Override public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
        play();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
