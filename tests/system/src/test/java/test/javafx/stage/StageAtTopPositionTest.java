/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.javafx.PlatformUtil;

import test.util.Util;

public class StageAtTopPositionTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Stage stage;

    public static void main(String[] args) throws Exception {
        initFX();
        try {
            StageAtTopPositionTest test = new StageAtTopPositionTest();
            test.testMoveToTopPosition();
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

    @BeforeClass
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void teardown() {
        Util.shutdown(stage);
    }

    @Test
    public void testMoveToTopPosition() throws Exception {
        // Only on Mac
        assumeTrue(PlatformUtil.isMac());

        Thread.sleep(200);
        Assert.assertTrue(stage.isShowing());
        Assert.assertFalse(stage.isFullScreen());

        final double minY = Screen.getPrimary().getVisualBounds().getMinY(); // Mac's system menubar height

        CountDownLatch latch = new CountDownLatch(2);
        ChangeListener<Number> listenerY = (observable, oldValue, newValue) -> {
            if (Math.abs((Double) newValue - minY) < 0.1) {
                latch.countDown();
            };
        };
        stage.yProperty().addListener(listenerY);

        // move once to y=0, gets moved to yMin
        Platform.runLater(() -> stage.setY(0));
        Thread.sleep(200);
        Assert.assertEquals("Window was moved once", minY, stage.getY(), 0.1);

        // move again to y=0, remains at yMin
        Platform.runLater(() -> stage.setY(0));
        latch.await(5, TimeUnit.SECONDS);
        stage.xProperty().removeListener(listenerY);

        Assert.assertEquals("Window was moved twice", minY, stage.getY(), 0.1);
    }
}
