/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.scene;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.Assert;
import test.util.Util;

public class NewSceneSizeTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static volatile Stage stage;

    private static double scaleX, scaleY;

    public static void main(String[] args) throws Exception {
        initFX();
        try {
            NewSceneSizeTest test = new NewSceneSizeTest();
            test.testNewSceneSize();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            teardown();
        }
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            primaryStage.setScene(new Scene(new VBox()));
            stage = primaryStage;
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                scaleX = stage.getOutputScaleX();
                scaleY = stage.getOutputScaleY();

                Platform.runLater(startupLatch::countDown);
            });
            stage.show();
        }
    }

    @BeforeClass
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void teardown() {
        Util.shutdown(stage);
    }

    @Test
    public void testNewSceneSize() throws Exception {
        Thread.sleep(200);
        final int nTries = 100;
        Stage childStage[] = new Stage[nTries];
        double w[] = new double[nTries];
        double h[] = new double[nTries];
        CountDownLatch latch = new CountDownLatch(2 * nTries);
        for (int i = 0; i < nTries; i++) {
            int fI = i;
            Platform.runLater(new Runnable() {
                ChangeListener<Number> listenerW;
                ChangeListener<Number> listenerH;

                @Override
                public void run() {
                    Stage stage = new Stage();
                    childStage[fI] = stage;
                    stage.setResizable(fI % 2 == 0);
                    Scene scene = new Scene(new VBox(), 300 - fI, 200 - fI);
                    stage.setScene(scene);
                    w[fI] = (Math.ceil((300 - fI) * scaleX)) / scaleX;
                    h[fI] = (Math.ceil((200 - fI) * scaleY)) / scaleY;
                    Assert.assertTrue(w[fI] > 1);
                    Assert.assertTrue(h[fI] > 1);
                    stage.widthProperty().addListener(listenerW = (v, o, n) -> {
                        if (Math.abs((Double) n - w[fI]) < 0.1) {
                            stage.widthProperty().removeListener(listenerW);
                            Platform.runLater(latch::countDown);
                        }
                    });
                    stage.heightProperty().addListener(listenerH = (v, o, n) -> {
                        if (Math.abs((Double) n - h[fI]) < 0.1) {
                            stage.heightProperty().removeListener(listenerH);
                            Platform.runLater(latch::countDown);
                        }
                    });
                    stage.show();
                }
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        Thread.sleep(200);
        for (int i = 0; i < nTries; i++) {
            Assert.assertEquals("Wrong scene " + i + " width", w[i],
                                    childStage[i].getScene().getWidth(), 0.1);
            Assert.assertEquals("Wrong scene " + i + " height", h[i],
                                    childStage[i].getScene().getHeight(), 0.1);
        }
    }
}
