/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates.
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
package ensemble.samples.charts.scatter.animated;


import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.stage.Stage;
import javafx.util.Duration;


/**
 * A live scatter chart.
 *
 * @sampleName Live Scatter Chart
 * @preview preview.png
 * @docUrl https://docs.oracle.com/javafx/2/charts/jfxpub-charts.htm Using JavaFX Charts Tutorial
 * @see javafx.animation.SequentialTransition
 * @see javafx.animation.Timeline
 * @see javafx.scene.chart.NumberAxis
 * @see javafx.scene.chart.ScatterChart
 *
 * @related /Charts/Scatter/Scatter Chart
 */
public class LiveScatterChartApp extends Application {

    private ScatterChart.Series<Number,Number> series;
    private double nextX = 0;
    private SequentialTransition animation;

    public LiveScatterChartApp() {
        // create initial frames
        final KeyFrame initialFrames =
            new KeyFrame(Duration.millis(60),
                         (ActionEvent actionEvent) -> {
                             final Number nextY =
                                Math.sin(Math.toRadians(nextX)) * 100;
                             final Data<Number,Number> data =
                                new Data<Number, Number>(nextX, nextY);
                             series.getData().add(data);
                             nextX += 10;
                         });
        Timeline initial = new Timeline(initialFrames);
        initial.setCycleCount(200);

        // create follow-on frames
        final KeyFrame followingFrames =
            new KeyFrame(Duration.millis(60),
                         (ActionEvent actionEvent) -> {
                             final Number nextY =
                                 Math.sin(Math.toRadians(nextX)) * 100;
                             final Data<Number,Number> data =
                             new Data<Number, Number>(nextX, nextY);
                             series.getData().add(data);
                             if (series.getData().size() > 54) {
                                 series.getData().remove(0);
                             }
                             nextX += 10;
                         });
        Timeline following = new Timeline(followingFrames);
        following.setCycleCount(Animation.INDEFINITE);

        // create animation
        animation = new SequentialTransition(initial, following);
    }

    public Parent createContent() {
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis(-100, 100, 10);
        xAxis.setForceZeroInRange(false);
        xAxis.setLabel("X Axis");
        xAxis.setAnimated(false);
        yAxis.setLabel("Y Axis");
        yAxis.setAutoRanging(false);
        // add starting data
        series = new ScatterChart.Series<>();
        series.setName("Sine Wave");
        series.getData().add(new ScatterChart.Data<Number, Number>(5d, 5d));
        // setup chart
        final String liveScatterChartCss =
            getClass().getResource("LiveScatterChart.css").toExternalForm();
        final ScatterChart<Number, Number> sc =
            new ScatterChart<>(xAxis, yAxis);
        sc.getData().add(series);
        sc.getStylesheets().add(liveScatterChartCss);
        sc.setTitle("Animated Sine Wave ScatterChart");

        return sc;
    }

    public void play() {
        animation.play();
    }

    @Override public void stop() {
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
