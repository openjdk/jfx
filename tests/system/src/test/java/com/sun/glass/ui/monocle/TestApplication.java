/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import com.sun.glass.ui.Robot;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.Assert;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class TestApplication extends Application {

    private static final boolean verbose = Boolean.getBoolean("verbose");
    private static final double timeScale = Double.parseDouble(
            System.getProperty("timeScale", "1"));
    private static Stage stage;
    static final Semaphore ready = new Semaphore(1);
    private static int tapRadius;
    private static Group root;
    private static String glassPlatform;
    private static boolean isMonocle;
    private static boolean isLens;
    private static AtomicReference<Rectangle2D> screen = new AtomicReference<>();

    private static void initGlassPlatform() {
        if (glassPlatform == null) {
            try {
                TestRunnable.invokeAndWait(
                        () -> glassPlatform = System.getProperty(
                                "glass.platform"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        isMonocle = "Monocle".equals(glassPlatform);
        isLens = "Lens".equals(glassPlatform);
    }

    public static boolean isMonocle() {
        initGlassPlatform();
        return isMonocle;
    }

    public static boolean isLens() {
        initGlassPlatform();
        return isLens;
    }

    public void start(Stage stage) throws Exception {
        TestApplication.stage = stage;
        stage.initStyle(StageStyle.UNDECORATED);
        ready.release();
    }

    public static Stage getStage() throws InterruptedException {
        if (stage == null) {
            ready.acquire();
            UInput.setup();
            new Thread(() -> Application.launch(TestApplication.class)).start();
            ready.acquire();
            Platform.runLater(() -> {
                if (isMonocle()) {
                    tapRadius = TouchInput.getInstance().getTouchRadius();
                } else {
                    tapRadius = Integer.getInteger("lens.input.touch.TapRadius", 20);
                }
                ready.release();
            });
            ready.acquire();          
        }
        return stage;
    }

    public static Group getRootGroup() {
        return root;
    }

    public static void showFullScreenScene() throws Exception {
        TestApplication.getStage();
        frameWait(2);
        new TestRunnable() {
            @Override
            public void test() throws Exception {
                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                stage.setX(0.0);
                stage.setY(0.0);
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());
                Rectangle r = new Rectangle(bounds.getWidth(), bounds.getHeight());
                r.setFill(Color.BLUE);
                root = new Group();
                root.getChildren().add(r);
                Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
                stage.setScene(scene);
                stage.show();
                stage.requestFocus();
            }
        }.invokeAndWait();
        frameWait(2);
    }

    public static void showInMiddleOfScreen() throws Exception {
        TestApplication.getStage();
        // wait for events to finish being delivered to the previous scene
        frameWait(2);
        new TestRunnable() {
            @Override
            public void test() throws Exception {
                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                stage.setX(bounds.getWidth() / 4);
                stage.setY(bounds.getHeight() / 4);
                stage.setWidth(bounds.getWidth() / 2);
                stage.setHeight(bounds.getHeight() / 2);
                Rectangle r = new Rectangle(bounds.getWidth() / 2,
                                            bounds.getHeight() / 2);
                r.setFill(Color.BLUE);
                root = new Group();
                root.getChildren().add(r);
                Scene scene = new Scene(root, bounds.getWidth() / 2,
                                        bounds.getHeight() / 2);
                stage.setScene(scene);

                stage.show();
                stage.requestFocus();
            }
        }.invokeAndWait();
        frameWait(2);
    }

    public static void waitForNextPulse() throws InterruptedException {
        frameWait(1);
    }

    public static void waitForLayout() throws InterruptedException {
        frameWait(5);
    }

    private static void frameWait(int n) {
        final CountDownLatch frameCounter = new CountDownLatch(n);
        Platform.runLater(() -> new AnimationTimer() {
            @Override
            public void handle(long now) {
                frameCounter.countDown();
                if (frameCounter.getCount() == 0l) {
                    stop();
                }
            }
        }.start());
        try {
            frameCounter.await();
        } catch (InterruptedException ex) {
            Assert.fail("Unexpected exception: " + ex);
        }
    }

    public static void addKeyListeners() throws Exception {
        getStage().getScene().setOnKeyTyped((e) -> TestLog.log(
                "Key typed: " + e.getCharacter()));
        getStage().getScene().setOnKeyPressed((e) -> TestLog.log("Key pressed: " + e.getCode()));
        getStage().getScene().setOnKeyReleased((e) -> TestLog.log("Key released: " + e.getCode()));
    }

    public static void addMouseListeners() throws Exception {
        getStage().getScene().setOnMousePressed((e) -> TestLog.log("Mouse pressed: "
                + (int) e.getScreenX() + ", " + (int) e.getScreenY()));
        getStage().getScene().setOnMouseMoved((e) -> TestLog.log("Mouse moved: "
                + (int) e.getScreenX() + ", " + (int) e.getScreenY()));
        getStage().getScene().setOnMouseDragged((e) -> TestLog.log("Mouse dragged: "
                + (int) e.getScreenX() + ", " + (int) e.getScreenY()));
        getStage().getScene().setOnMouseReleased((e) -> TestLog.log("Mouse released: "
                + (int) e.getScreenX() + ", " + (int) e.getScreenY()));
        getStage().getScene().setOnMouseClicked((e) -> TestLog.log("Mouse clicked: "
                + (int) e.getScreenX() + ", " + (int) e.getScreenY()));
        getStage().getScene().setOnMouseEntered((e) -> TestLog.log("Mouse entered: "
                            + (int) e.getScreenX() + ", " + (int) e.getScreenY()));
        getStage().getScene().setOnMouseExited((e) -> TestLog.log("Mouse exited: "
                            + (int) e.getScreenX() + ", " + (int) e.getScreenY()));
    }

    public static void addTouchListeners() throws Exception {
        Consumer<List<TouchPoint>> logTouchPoints = (tps) -> {
            TestLog.log("Touch points count: [" + tps.size() +"]"); 
            for (TouchPoint tp : tps) {
                TestLog.log("TouchPoint: " + tp.getState() + " "
                        + (int) tp.getScreenX() + ", " + (int) tp.getScreenY()
                        + " id=" + tp.getId());
            }
        };
        getStage().getScene().setOnTouchPressed((e) -> {
            TestLog.log("Touch pressed: "
                    + (int) e.getTouchPoint().getScreenX()
                    + ", "
                    + (int) e.getTouchPoint().getScreenY());
            logTouchPoints.accept(e.getTouchPoints());
        });
        getStage().getScene().setOnTouchReleased((e) -> {
            TestLog.log("Touch released: "
                    + (int) e.getTouchPoint().getScreenX()
                    + ", "
                    + (int) e.getTouchPoint().getScreenY());
            logTouchPoints.accept(e.getTouchPoints());
        });
        getStage().getScene().setOnTouchMoved((e) -> {
            TestLog.log("Touch moved: "
                    + (int) e.getTouchPoint().getScreenX()
                    + ", "
                    + (int) e.getTouchPoint().getScreenY());
            logTouchPoints.accept(e.getTouchPoints());
        });
        getStage().getScene().setOnTouchStationary((e) -> {
            TestLog.log("Touch stationary: "
                    + (int) e.getTouchPoint().getScreenX()
                    + ", "
                    + (int) e.getTouchPoint().getScreenY());
            logTouchPoints.accept(e.getTouchPoints());
        });
    }

    public static void addGestureListeners() throws Exception {
        //Zoom
        getStage().getScene().setOnZoom((e) -> TestLog.log("Zoom, factor: " + e.getZoomFactor()
                + ", total factor: " + e.getTotalZoomFactor()
                + ", inertia value: " + e.isInertia()));

        getStage().getScene().setOnZoomStarted((e) -> TestLog.log("Zoom started, factor: " + e.getZoomFactor()
                + ", total factor: " + e.getTotalZoomFactor()
                + ", inertia value: " + e.isInertia()));

        getStage().getScene().setOnZoomFinished((e) -> TestLog.log("Zoom finished, factor: " + e.getZoomFactor()
                + ", total factor: " + e.getTotalZoomFactor()
                + ", inertia value: " + e.isInertia()));

        //Rotate
        getStage().getScene().setOnRotate((e) -> TestLog.log("Rotation, angle: " + Math.round(e.getAngle())
                + ", total angle: " + Math.round(e.getTotalAngle())
                + ", inertia value: " + e.isInertia()));

        getStage().getScene().setOnRotationStarted((e) -> TestLog.log("Rotation started, angle: " + Math.round(e.getAngle())
                + ", total angle: " + Math.round(e.getTotalAngle())
                + ", inertia value: " + e.isInertia()));

        getStage().getScene().setOnRotationFinished((e) -> TestLog.log("Rotation finished, angle: " + Math.round(e.getAngle())
                + ", total angle: " + Math.round(e.getTotalAngle())
                + ", inertia value: " + e.isInertia()));

        //Scroll
        getStage().getScene().setOnScroll((e) -> TestLog.log("Scroll, DeltaX: " + Math.round(e.getDeltaX())
                + ", DeltaY: " + Math.round(e.getDeltaY())
                + ", totalDeltaX: " + Math.round(e.getTotalDeltaX())
                + ", totalDeltaY: " + Math.round(e.getTotalDeltaY())
                + ", touch points: " + e.getTouchCount()
                + ", inertia value: " + e.isInertia()));

        getStage().getScene().setOnScrollStarted((e) -> TestLog.log("Scroll started, DeltaX: " + Math.round(e.getDeltaX())
                + ", DeltaY: " + Math.round(e.getDeltaY())
                + ", totalDeltaX: " + Math.round(e.getTotalDeltaX())
                + ", totalDeltaY: " + Math.round(e.getTotalDeltaY())
                + ", touch points: " + e.getTouchCount()
                + ", inertia value: " + e.isInertia()));

        getStage().getScene().setOnScrollFinished((e) -> TestLog.log("Scroll finished, DeltaX: " + Math.round(e.getDeltaX())
                + ", DeltaY: " + Math.round(e.getDeltaY())
                + ", totalDeltaX: " + Math.round(e.getTotalDeltaX())
                + ", totalDeltaY: " + Math.round(e.getTotalDeltaY())
                + ", touch points: " + e.getTouchCount()
                + ", inertia value: " + e.isInertia()));
    }

    public static void movePointerTo(final int targetX, final int targetY) throws Exception {
        final Semaphore released = new Semaphore(0);
        EventHandler<TouchEvent> touchHandler = (e) -> released.release();
        getStage().addEventHandler(TouchEvent.TOUCH_RELEASED, touchHandler);
        final UInput ui = new UInput();
        ui.processLine("OPEN");
        ui.processLine("VENDOR 0x596");
        ui.processLine("PRODUCT 0x502");
        ui.processLine("VERSION 1");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("KEYBIT BTN_TOUCH");
        ui.processLine("EVBIT EV_ABS");
        ui.processLine("ABSBIT ABS_PRESSURE");
        ui.processLine("ABSBIT ABS_X");
        ui.processLine("ABSBIT ABS_Y");
        ui.processLine("ABSMIN ABS_X 0");
        ui.processLine("ABSMAX ABS_X 4095");
        ui.processLine("ABSMIN ABS_Y 0");
        ui.processLine("ABSMAX ABS_Y 4095");
        ui.processLine("ABSMIN ABS_PRESSURE 0");
        ui.processLine("ABSMAX ABS_PRESSURE 1");
        ui.processLine("PROPBIT INPUT_PROP_POINTER");
        ui.processLine("PROPBIT INPUT_PROP_DIRECT");
        ui.processLine("PROPERTY ID_INPUT_TOUCHSCREEN 1");
        ui.processLine("CREATE");
        Rectangle2D r = Screen.getPrimary().getBounds();
        int x = (int) ((targetX * 4096.0) / r.getWidth());
        int y = (int) ((targetY * 4096.0) / r.getHeight());
        ui.processLine("EV_ABS ABS_X " + x);
        ui.processLine("EV_ABS ABS_Y " + y);
        ui.processLine("EV_ABS ABS_PRESSURE 1");
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_ABS ABS_X " + x);
        ui.processLine("EV_ABS ABS_Y " + y);
        ui.processLine("EV_ABS ABS_PRESSURE 0");
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_SYN");
        try {
            Assert.assertTrue(released.tryAcquire(3, TimeUnit.SECONDS));
            TestRunnable.invokeAndWait(() -> {
                Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
                try {
                    TestLog.log("x = " + robot.getMouseX());
                    TestLog.log("y = " + robot.getMouseY());
                    TestLog.log("targetX = " + targetX);
                    TestLog.log("targetY = " + targetY);
                    Assert.assertEquals(targetX, robot.getMouseX());
                    Assert.assertEquals(targetY, robot.getMouseY());
                } finally {
                    robot.destroy();
                }
            });
            frameWait(1);
        } finally {
            getStage().removeEventHandler(TouchEvent.TOUCH_RELEASED, touchHandler);
            ui.processLine("DESTROY");
            ui.processLine("CLOSE");
            ui.dispose();
        }
        frameWait(1);
    }

    public static int getTapRadius() {
        return tapRadius;
    }


    public static boolean isVerbose() {
        return verbose;
    }

    public static double getTimeScale() {
        return timeScale;
    }


    private static void fetchScreenBounds() {
        if (Platform.isFxApplicationThread()) {
            screen.set(Screen.getPrimary().getBounds());
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                screen.set(Screen.getPrimary().getBounds());
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static Rectangle2D getScreenBounds() {
        Rectangle2D r = screen.get();
        if (r == null) {
            fetchScreenBounds();
            r = screen.get();
        }
        return r;
    }

}
