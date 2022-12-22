/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.stage;

import static org.junit.Assume.assumeTrue;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class ChildStageLocationTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Timer timer;
    static Runnable runNext;
    static volatile Stage stage;
    static volatile Stage childStage;
    static double x, y;

    public static void main(String[] args) throws Exception {
        initFX();
        new ChildStageLocationTest().testLocation();
        teardown();
    }

    @BeforeClass
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void teardown() {
        Util.shutdown();
    }

    @Test
    public void testLocation() {
        assumeTrue(Boolean.getBoolean("unstable.test")); // JDK-8176776
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        Window window = stage.getScene().getWindow();
        double minX = bounds.getMinX() + (bounds.getWidth() - window.getWidth())/2 - 50;
        double minY = bounds.getMinY() + (bounds.getHeight() - window.getHeight())/3 - 100;
        System.out.println("Primary stage location: " + stage.getX() + " " + stage.getY());
        Assert.assertTrue("Primary stage X location should be >" + minX, stage.getX() > minX);
        Assert.assertTrue("Primary stage Y location should be >" + minY, stage.getY() > minY);

        window = childStage.getScene().getWindow();
        minX = bounds.getMinX() + (bounds.getWidth() - window.getWidth())/2 - 50;
        minY = bounds.getMinY() + (bounds.getHeight() - window.getHeight())/3 - 100;
        System.out.println("Child stage location: " + childStage.getX() + " " + childStage.getY());
        Assert.assertTrue("Child stage X location should be >" + minX, childStage.getX() > minX);
        Assert.assertTrue("Child stage Y location should be >" + minY, childStage.getY() > minY);
    }

    public static class TestApp extends Application implements ChangeListener {

        @Override
        public void start(Stage stage) throws Exception {
            stage.setScene(new Scene(new Label("Label")));
            stage.xProperty().addListener(this);
            stage.yProperty().addListener(this);
            stage.widthProperty().addListener(this);
            stage.heightProperty().addListener(this);
            ChildStageLocationTest.stage = stage;
            runNext = () -> {
                // FIX unsynchronized access from multiple threads
                runNext = () -> {};
                Platform.runLater(this::createChildStage);
            };
            stage.show();
        }

        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue) {
            if (timer != null) {
                timer.cancel();
            }
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runNext.run();
                }
            }, 1500);
        }

        void createChildStage() {
            childStage = new Stage();
            childStage.setScene(new Scene(new Label("Label")));
            childStage.sizeToScene();
            childStage.xProperty().addListener(this);
            childStage.yProperty().addListener(this);
            childStage.widthProperty().addListener(this);
            childStage.heightProperty().addListener(this);
            runNext = startupLatch::countDown;
            childStage.showAndWait();
        }
    }
}
