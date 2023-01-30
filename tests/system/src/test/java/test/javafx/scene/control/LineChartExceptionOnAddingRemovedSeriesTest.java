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
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class LineChartExceptionOnAddingRemovedSeriesTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static CountDownLatch addSeriesLatch = new CountDownLatch(1);
    static VBox vBox;

    static volatile Stage stage;
    static volatile Scene scene;
    static volatile Throwable exception;

    static final int SCENE_WIDTH = 250;
    static final int SCENE_HEIGHT = SCENE_WIDTH;

    static NumberAxis xAxis;
    static NumberAxis yAxis;
    static LineChart<Number,Number> lineChart;
    static XYChart.Series series;

    @Test
    public void testLineChartExceptionOnAddingRemovedSeries() throws Throwable {
        Thread.sleep(1000); // Wait for stage to layout

        Assert.assertEquals(1, lineChart.getData().size());

        addSeriesLatch.countDown();
        addRemovedSeries();
        Util.waitForLatch(addSeriesLatch, 5, "Timeout waiting for series to be added.");

        if (exception != null) {
            exception.printStackTrace();
            throw exception;
        }

        Assert.assertEquals(1, lineChart.getData().size());
    }

    private void addRemovedSeries() {
        Util.runAndWait(() -> {
            ObservableList<Series<Number, Number>> data = lineChart.getData();
            Series<Number, Number> removedSeries = data.remove(0);
            data.add(removedSeries);
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

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;

            xAxis = new NumberAxis();
            yAxis = new NumberAxis();
            lineChart = new LineChart<Number,Number>(xAxis,yAxis);
            series = new XYChart.Series();

            series.getData().add(new XYChart.Data(1, 14));
            series.getData().add(new XYChart.Data(2, 15));
            lineChart.getData().add(series);

            vBox = new VBox(lineChart);
            scene = new Scene(vBox, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.show();

            Thread.currentThread().setUncaughtExceptionHandler((t2, e) -> {
                exception = e;
            });
        }
    }
}
