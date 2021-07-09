/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.chart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class ChartYAxisLayoutTest {

    private static CountDownLatch startupLatch;
    private static Stage stage;
    private static VBox rootPane;
    private static CategoryAxis yAxis;

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            stage = primaryStage;
            rootPane = new VBox();
            stage.setScene(new Scene(rootPane, 800, 600));
            stage.setX(0);
            stage.setY(0);

            var categories = List.of(
                    "1\ns\nt\nv\ne\nr\ny\nt\na\nl",
                    "2\nn\nd\nv\ne\nr\ny\nt\na\nl",
                    "3\nr\nd\nv\ne\nr\ny\nt\na\nl"
            );

            yAxis = new CategoryAxis();
            yAxis.setCategories(FXCollections.observableList(categories));

            BarChart<Number, String> chart = new BarChart<>(new NumberAxis(), yAxis);
            chart.prefWidthProperty().bind(rootPane.widthProperty());
            chart.prefHeightProperty().bind(rootPane.heightProperty());
            chart.setAnimated(false);
            rootPane.getChildren().add(chart);

            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                Platform.runLater(() -> startupLatch.countDown());
            });
            stage.show();
        }
    }

    @BeforeClass
    public static void initFX() throws Exception {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(ChartYAxisLayoutTest.TestApp.class, (String[]) null)).start();

        assertTrue("Timeout waiting for FX runtime to start", startupLatch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void ensureThatLabelsAreRotatedWhenLackingSpace() {
        Util.runAndWait(() -> stage.setHeight(300));
        Util.sleep(10);
        Util.runAndWait(() -> assertEquals(90.0, yAxis.getTickLabelRotation(), 0.0001));
    }

    @Test
    public void ensureThatLabelsAreNotRotatedWhenEnoughSpace() {
        Util.runAndWait(() -> stage.setHeight(900));
        Util.sleep(10);
        Util.runAndWait(() -> assertEquals(0.0, yAxis.getTickLabelRotation(), 0.0001));
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.runLater(() -> {
            stage.hide();
            Platform.exit();
        });
    }
}
