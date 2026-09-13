/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.scene;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.sun.javafx.PlatformUtil;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class MouseWheelScaleTest {

    private static final double SCALE = 2.0;
    private static final double EPSILON = 0.0001;

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static final CountDownLatch scrollLatch = new CountDownLatch(1);
    private static final AtomicReference<ScrollEvent> scrollEvent = new AtomicReference<>();

    private static volatile Stage stage;
    private static volatile StackPane root;
    private static Robot robot;

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            root = new StackPane();
            Scene scene = new Scene(root, 300, 200);
            scene.setOnScroll(event -> {
                scrollEvent.compareAndSet(null, event);
                scrollLatch.countDown();
            });

            stage = primaryStage;
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setScene(scene);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, _ -> Platform.runLater(startupLatch::countDown));
            robot = new Robot();
            stage.show();
        }
    }

    @BeforeAll
    public static void setupOnce() {
        System.setProperty("glass.win.uiScale", String.valueOf(SCALE));
        System.setProperty("glass.gtk.uiScale", String.valueOf(SCALE));

        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void teardown() {
        Util.shutdown();
    }

    @Test
    public void testWheelRotationIsNotScaled() {
        assumeTrue(PlatformUtil.isLinux() || PlatformUtil.isWindows());
        assertEquals(SCALE, stage.getOutputScaleY(), EPSILON, "Wrong output scale");

        Util.runAndWait(() -> {
            Point2D center = root.localToScreen(root.getWidth() / 2, root.getHeight() / 2);
            robot.mouseMove(center);
            robot.mouseWheel(1);
        });

        Util.waitForLatch(scrollLatch, 5, "Timeout while waiting for a scroll event");

        ScrollEvent event = scrollEvent.get();
        assumeTrue(event.getMultiplierY() != 0, "Mouse-wheel scrolling is disabled");

        // Dividing the pixel delta by its platform multiplier recovers the
        // dimensionless wheel rotation, which must remain 1 at any scale.
        double wheelRotation = event.getDeltaY() / event.getMultiplierY();
        assertEquals(1.0, Math.abs(wheelRotation), EPSILON);
    }
}
