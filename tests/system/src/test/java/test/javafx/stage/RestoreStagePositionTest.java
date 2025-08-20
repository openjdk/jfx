/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.sun.javafx.PlatformUtil;
import test.util.Util;

public class RestoreStagePositionTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Stage stage;

    public static void main(String[] args) throws Exception {
        initFX();
        try {
            RestoreStagePositionTest test = new RestoreStagePositionTest();
            test.testUnfullscreenPosition();
            test.testDemaximizedPosition();
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
            stage.setX(300);
            stage.setY(400);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                                    Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }

    @BeforeAll
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void teardown() {
        Util.shutdown();
    }

    @Test
    public void testUnfullscreenPosition() throws Exception {
        Thread.sleep(200);
        Assertions.assertTrue(stage.isShowing());
        Assertions.assertFalse(stage.isFullScreen());

        double x = stage.getX();
        double y = stage.getY();

        Platform.runLater(() -> stage.setFullScreen(true));
        Thread.sleep(800);
        Assertions.assertTrue(stage.isFullScreen());
        CountDownLatch latch = new CountDownLatch(2);

        ChangeListener<Number> listenerX = (observable, oldValue, newValue) -> {
            if (Math.abs((Double) newValue - x) < 0.1) {
                latch.countDown();
            };
        };
        ChangeListener<Number> listenerY = (observable, oldValue, newValue) -> {
            if (Math.abs((Double) newValue - y) < 0.1) {
                latch.countDown();
            };
        };
        stage.xProperty().addListener(listenerX);
        stage.yProperty().addListener(listenerY);
        Platform.runLater(() -> stage.setFullScreen(false));
        latch.await(5, TimeUnit.SECONDS);
        stage.xProperty().removeListener(listenerX);
        stage.xProperty().removeListener(listenerY);

        Assertions.assertEquals(x, stage.getX(), 0.1, "Window was moved");
        Assertions.assertEquals(y, stage.getY(), 0.1, "Window was moved");
    }

    @Test
    public void testDemaximizedPosition() throws Exception {
        // Disable on Mac until JDK-8089230 is fixed
        assumeTrue(!PlatformUtil.isMac());

        Thread.sleep(200);
        Assertions.assertTrue(stage.isShowing());
        Assertions.assertFalse(stage.isMaximized());

        double x = stage.getX();
        double y = stage.getY();

        Platform.runLater(() -> stage.setMaximized(true));
        Thread.sleep(200);
        Assertions.assertTrue(stage.isMaximized());
        CountDownLatch latch = new CountDownLatch(2);

        ChangeListener<Number> listenerX = (observable, oldValue, newValue) -> {
            if (Math.abs((Double) newValue - x) < 0.1) {
                latch.countDown();
            };
        };
        ChangeListener<Number> listenerY = (observable, oldValue, newValue) -> {
            if (Math.abs((Double) newValue - y) < 0.1) {
                latch.countDown();
            };
        };
        stage.xProperty().addListener(listenerX);
        stage.yProperty().addListener(listenerY);
        Platform.runLater(() -> stage.setMaximized(false));
        latch.await(5, TimeUnit.SECONDS);
        stage.xProperty().removeListener(listenerX);
        stage.xProperty().removeListener(listenerY);

        Assertions.assertEquals(x, stage.getX(), 0.1, "Window was moved");
        Assertions.assertEquals(y, stage.getY(), 0.1, "Window was moved");
    }
}
