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

package test.javafx.scene.control;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.chart.AreaChart;
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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class XYChartExceptionOnAddingRemovedSeriesTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static CountDownLatch lineSeriesLatch = new CountDownLatch(1);
    static CountDownLatch areaChartLatch = new CountDownLatch(1);
    static CountDownLatch bubbleChartLatch = new CountDownLatch(1);
    static CountDownLatch scatterChartLatch = new CountDownLatch(1);
    static CountDownLatch stackedAreaChartLatch = new CountDownLatch(1);
    static CountDownLatch stackedBarChartLatch = new CountDownLatch(1);
    static VBox vBox;

    static volatile Stage stage;
    static volatile Scene scene;
    static volatile Throwable exception;

    static final int SCENE_WIDTH = 250;
    static final int SCENE_HEIGHT = SCENE_WIDTH;

    static NumberAxis xAxis;
    static NumberAxis yAxis;
    static CategoryAxis categoryAxis;
    static LineChart<Number,Number> lineChart;
    static AreaChart<Number,Number> areaChart;
    static BubbleChart<Number,Number> bubbleChart;
    static ScatterChart<Number,Number> scatterChart;
    static StackedAreaChart<Number,Number> stackedAreaChart;
    static StackedBarChart<String, Number> stackedBarChart;
    static XYChart.Series seriesLineChart;
    static XYChart.Series seriesAreaChart;
    static XYChart.Series seriesBubbleChart;
    static XYChart.Series seriesScatterChart;
    static XYChart.Series seriesStackedAreaChart;
    static XYChart.Series seriesStackedBarChart;

    @Test
    public void testLineChartExceptionOnAddingRemovedSeries() throws Throwable {
        Util.waitForLatch(startupLatch, 5, "Timeout waiting for stage to layout.");

        Assert.assertEquals(1, lineChart.getData().size());

        lineSeriesLatch.countDown();
        addRemovedSeriesLineChart();
        Util.waitForLatch(lineSeriesLatch, 5, "Timeout waiting for series to be added.");

        Assert.assertEquals(1, lineChart.getData().size());
    }

    private void addRemovedSeriesLineChart() {
        Util.runAndWait(() -> {
            vBox.getChildren().clear();
            vBox.getChildren().add(lineChart);
            Series<Number, Number> removedSeries = lineChart.getData().remove(0);
            Assert.assertEquals(0, lineChart.getData().size());
            lineChart.getData().add(removedSeries);

        });
    }

    @Test
    public void testAreaChartExceptionOnAddingRemovedSeries() throws Throwable {
        Assert.assertEquals(1, areaChart.getData().size());

        areaChartLatch.countDown();
        addRemovedSeriesAreaChart();
        Util.waitForLatch(areaChartLatch, 5, "Timeout waiting for series to be added.");

        Assert.assertEquals(1, areaChart.getData().size());
    }

    private void addRemovedSeriesAreaChart() {
        Util.runAndWait(() -> {
            vBox.getChildren().clear();
            vBox.getChildren().add(areaChart);
            Series<Number, Number> removedSeries = areaChart.getData().remove(0);
            Assert.assertEquals(0, areaChart.getData().size());
            areaChart.getData().add(removedSeries);
        });
    }

    @Test
    public void testBubbleChartExceptionOnAddingRemovedSeries() throws Throwable {
        Assert.assertEquals(1, bubbleChart.getData().size());

        bubbleChartLatch.countDown();
        addRemovedSeriesBubbleChart();
        Util.waitForLatch(bubbleChartLatch, 5, "Timeout waiting for series to be added.");

        Assert.assertEquals(1, bubbleChart.getData().size());
    }

    private void addRemovedSeriesBubbleChart() {
        Util.runAndWait(() -> {
            vBox.getChildren().clear();
            vBox.getChildren().add(bubbleChart);
            Series<Number, Number> removedSeries = bubbleChart.getData().remove(0);
            Assert.assertEquals(0, bubbleChart.getData().size());
            bubbleChart.getData().add(removedSeries);
        });
    }

    @Test
    public void testScatterChartExceptionOnAddingRemovedSeries() throws Throwable {
        Assert.assertEquals(1, scatterChart.getData().size());

        scatterChartLatch.countDown();
        addRemovedSeriesScatterChart();
        Util.waitForLatch(scatterChartLatch, 5, "Timeout waiting for series to be added.");

        Assert.assertEquals(1, scatterChart.getData().size());
    }

    private void addRemovedSeriesScatterChart() {
        Util.runAndWait(() -> {
            vBox.getChildren().clear();
            vBox.getChildren().add(scatterChart);
            Series<Number, Number> removedSeries = scatterChart.getData().remove(0);
            Assert.assertEquals(0, scatterChart.getData().size());
            scatterChart.getData().add(removedSeries);
        });
    }

    @Test
    public void testStackedAreaChartExceptionOnAddingRemovedSeries() throws Throwable {
        Assert.assertEquals(1, stackedAreaChart.getData().size());

        stackedAreaChartLatch.countDown();
        addRemovedSeriesStackedAreaChart();
        Util.waitForLatch(stackedAreaChartLatch, 5, "Timeout waiting for series to be added.");

        Assert.assertEquals(1, stackedAreaChart.getData().size());
    }

    private void addRemovedSeriesStackedAreaChart() {
        Util.runAndWait(() -> {
            vBox.getChildren().clear();
            vBox.getChildren().add(stackedAreaChart);
            Series<Number, Number> removedSeries = stackedAreaChart.getData().remove(0);
            Assert.assertEquals(0, stackedAreaChart.getData().size());
            stackedAreaChart.getData().add(removedSeries);
        });
    }

    @Test
    public void testStackedBarChartExceptionOnAddingRemovedSeries() throws Throwable {
        Assert.assertEquals(1, stackedBarChart.getData().size());

        stackedBarChartLatch.countDown();
        addRemovedSeriesStackedBarChart();
        Util.waitForLatch(stackedBarChartLatch, 5, "Timeout waiting for series to be added.");

        Assert.assertEquals(1, stackedBarChart.getData().size());
    }

    private void addRemovedSeriesStackedBarChart() {
        Util.runAndWait(() -> {
            vBox.getChildren().clear();
            vBox.getChildren().add(stackedBarChart);
            Series<String, Number> removedSeries = stackedBarChart.getData().remove(0);
            Assert.assertEquals(0, stackedBarChart.getData().size());
            stackedBarChart.getData().add(removedSeries);
        });
    }

    @BeforeClass
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(stage);
    }

    @Before
    public void setup() {
        Util.runAndWait(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException)throwable;
                } else {
                    Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
                }
            });
        });
    }

    @After
    public void cleanup() {
        Util.runAndWait(() -> {
            Thread.currentThread().setUncaughtExceptionHandler(null);
        });
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;

            xAxis = new NumberAxis();
            yAxis = new NumberAxis();
            categoryAxis = new CategoryAxis();

            seriesLineChart = new XYChart.Series();
            seriesAreaChart = new XYChart.Series();
            seriesBubbleChart = new XYChart.Series();
            seriesScatterChart = new XYChart.Series();
            seriesStackedAreaChart = new XYChart.Series();
            seriesStackedBarChart = new XYChart.Series();

            seriesLineChart.getData().add(new XYChart.Data(1, 14));
            seriesLineChart.getData().add(new XYChart.Data(2, 15));
            lineChart = new LineChart<Number,Number>(xAxis,yAxis);
            lineChart.getData().add(seriesLineChart);

            seriesAreaChart.getData().add(new XYChart.Data(1, 14));
            seriesAreaChart.getData().add(new XYChart.Data(2, 15));
            areaChart = new AreaChart<Number,Number>(xAxis,yAxis);
            areaChart.getData().add(seriesAreaChart);

            seriesBubbleChart.getData().add(new XYChart.Data(1, 14));
            seriesBubbleChart.getData().add(new XYChart.Data(2, 15));
            bubbleChart = new BubbleChart<Number,Number>(xAxis,yAxis);
            bubbleChart.getData().addAll(seriesBubbleChart);

            seriesScatterChart.getData().add(new XYChart.Data(1, 14));
            seriesScatterChart.getData().add(new XYChart.Data(2, 15));
            scatterChart = new ScatterChart<Number,Number>(xAxis,yAxis);
            scatterChart.getData().add(seriesScatterChart);

            seriesStackedAreaChart.getData().add(new XYChart.Data(1, 14));
            seriesStackedAreaChart.getData().add(new XYChart.Data(2, 15));
            stackedAreaChart = new StackedAreaChart<Number,Number>(xAxis,yAxis);
            stackedAreaChart.getData().add(seriesStackedAreaChart);

            seriesStackedBarChart.getData().add(new XYChart.Data("Categoty1", 14));
            seriesStackedBarChart.getData().add(new XYChart.Data("Categoty2", 15));
            stackedBarChart = new StackedBarChart<String, Number>(categoryAxis,yAxis);
            stackedBarChart.getData().add(seriesStackedBarChart);

            vBox = new VBox();
            scene = new Scene(vBox, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }
}
