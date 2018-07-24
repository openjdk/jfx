/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
import junit.framework.AssertionFailedError;
import test.util.Util;

import static javafx.scene.paint.Color.MAGENTA;
import static org.junit.Assert.fail;

/**
 * Tests to verify that the native robot implementations all work correctly.
 */
public class RobotTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static volatile Stage stage;
    static volatile Scene scene;
    static Robot robot;
    // A tolerance is needed because on macOS the pixel colors are affected by the configured "color profile"
    // of the display.
    private static final double TOLERANCE = 0.07;
    private static final int SIZE = 400;
    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;

    public static void main(String[] args) throws Exception {
        RobotTest test = new RobotTest();
        test.testKeyboard();
        test.testMouseMove();
        test.testMouseClick();
        test.testMouseWheel();
        test.testPixelCapture();
        test.testPixelCaptureAverage();
        test.testScreenCapture();
        exit();
    }

    @Test
    public void testKeyboard() {
        CountDownLatch onKeyTypedLatch = new CountDownLatch(1);
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        TextField textField = new TextField();
        InvalidationListener invalidationListener = observable -> setSceneLatch.countDown();
        Util.runAndWait(() -> {
            textField.setOnKeyTyped(event -> onKeyTypedLatch.countDown());
            scene = new Scene(new HBox(textField));
            stage.sceneProperty().addListener(observable -> {
                setSceneLatch.countDown();
                stage.sceneProperty().removeListener(invalidationListener);
            });
            stage.setScene(scene);
        });
        waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        Util.runAndWait(() -> {
            int mouseX = (int) (scene.getWindow().getX() + scene.getX() +
                    textField.getLayoutX() + textField.getLayoutBounds().getWidth() / 2);
            int mouseY = (int) (scene.getWindow().getY() + scene.getY() +
                    textField.getLayoutY() + textField.getLayoutBounds().getHeight() / 2);
            robot.mouseMove(mouseX, mouseY);
            robot.mouseClick(MouseButton.PRIMARY);
            robot.keyPress(KeyCode.A);
            robot.keyRelease(KeyCode.A);
        });
        waitForLatch(onKeyTypedLatch, 5, "Timeout while waiting for textField.onKeyTyped().");
        Assert.assertEquals("letter 'a' should be typed by Robot", "a", textField.getText());
    }

    @Test
    public void testMouseMove() {
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        InvalidationListener invalidationListener = observable -> setSceneLatch.countDown();
        Util.runAndWait(() -> {
            scene = new Scene(new HBox(), SIZE, SIZE);
            stage.sceneProperty().addListener(observable -> {
                setSceneLatch.countDown();
                stage.sceneProperty().removeListener(invalidationListener);
            });
            stage.setScene(scene);
        });
        waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        AtomicReference<Point2D> mousePosition = new AtomicReference<>();
        Util.runAndWait(() -> {
            robot.mouseMove(50, 50);
            mousePosition.set(robot.getMousePosition());
        });
        Assert.assertEquals(50, (int) mousePosition.get().getX());
        Assert.assertEquals(50, (int) mousePosition.get().getY());
    }

    @Test
    public void testMouseClick() {
        CountDownLatch onClickLatch = new CountDownLatch(1);
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        Button button = new Button("Click me");
        InvalidationListener invalidationListener = observable -> setSceneLatch.countDown();
        Util.runAndWait(() -> {
            button.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    button.setText("Clicked");
                    onClickLatch.countDown();
                }
            });
            scene = new Scene(new HBox(button), SIZE, SIZE);
            stage.sceneProperty().addListener(observable -> {
                setSceneLatch.countDown();
                stage.sceneProperty().removeListener(invalidationListener);
            });
            stage.setScene(scene);
        });
        waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        int mouseX = (int) (scene.getWindow().getX() + scene.getX() +
                button.getLayoutX() + button.getLayoutBounds().getWidth() / 2);
        int mouseY = (int) (scene.getWindow().getY() + scene.getY() +
                button.getLayoutY() + button.getLayoutBounds().getHeight() / 2);
        Util.runAndWait(() -> {
            robot.mouseMove(mouseX, mouseY);
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
        waitForLatch(onClickLatch, 5, "Timeout while waiting for button.onMouseClicked().");
        Assert.assertEquals("primary mouse button should be clicked by Robot", "Clicked", button.getText());
    }

    @Test
    public void testMouseWheel() {
        CountDownLatch onScrollLatch = new CountDownLatch(1);
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        Button button = new Button("Scroll me");
        InvalidationListener invalidationListener = observable -> setSceneLatch.countDown();
        Util.runAndWait(() -> {
            button.setOnScroll(event -> {
                button.setText("Scrolled");
                onScrollLatch.countDown();
            });
            scene = new Scene(new HBox(button), SIZE, SIZE);
            stage.sceneProperty().addListener(observable -> {
                setSceneLatch.countDown();
                stage.sceneProperty().removeListener(invalidationListener);
            });
            stage.setScene(scene);
        });
        waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        Util.runAndWait(() -> {
            int mouseX = (int) (scene.getWindow().getX() + scene.getX() +
                    button.getLayoutX() + button.getLayoutBounds().getWidth() / 2);
            int mouseY = (int) (scene.getWindow().getY() + scene.getY() +
                    button.getLayoutY() + button.getLayoutBounds().getHeight() / 2);
            robot.mouseMove(mouseX, mouseY);
            robot.mouseWheel(5);
        });
        waitForLatch(onScrollLatch, 5, "Timeout while waiting for button.onScroll().");
        Assert.assertEquals("mouse wheel should be scrolled 5 vertical units by Robot", "Scrolled", button.getText());
    }

    @Test
    public void testPixelCapture() throws Exception {
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        Pane pane = new StackPane();
        InvalidationListener invalidationListener = observable -> setSceneLatch.countDown();
        Util.runAndWait(() -> {
            pane.setBackground(new Background(new BackgroundFill(Color.CORNFLOWERBLUE, null, null)));
            scene = new Scene(pane, SIZE, SIZE);
            stage.sceneProperty().addListener(observable -> {
                setSceneLatch.countDown();
                stage.sceneProperty().removeListener(invalidationListener);
            });
            stage.setScene(scene);
        });
        waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        AtomicReference<Color> captureColor = new AtomicReference<>();
        Thread.sleep(1000);
        Util.runAndWait(() -> {
            int x = (int) stage.getX();
            int y = (int) stage.getY();
            captureColor.set(robot.getPixelColor(x + SIZE / 2, y + SIZE / 2));
        });
        assertColorEquals(Color.CORNFLOWERBLUE, captureColor.get(), TOLERANCE);
    }

    @Test
    public void testPixelCaptureAverage() throws Exception {
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        Pane pane = new StackPane();
        InvalidationListener invalidationListener = observable -> setSceneLatch.countDown();
        Util.runAndWait(() -> {
            pane.setBackground(new Background(new BackgroundFill(Color.RED, null, new Insets(0, 0, 0, 0)),
                    new BackgroundFill(Color.BLUE, null, new Insets(0, 0, 0, SIZE / 2))));
            scene = new Scene(pane, SIZE, SIZE);
            stage.sceneProperty().addListener(observable -> {
                setSceneLatch.countDown();
                stage.sceneProperty().removeListener(invalidationListener);
            });
            stage.setScene(scene);
        });
        waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        AtomicReference<Color> captureColor = new AtomicReference<>();
        Thread.sleep(1000);
        Util.runAndWait(() -> {
            int x = (int) stage.getX();
            int y = (int) stage.getY();
            // Subtracting one pixel from x makes the result RED, so we are on the border.
            // If the implementation of getPixelColor is ever chaged to interpolate the
            // colors on HiDPI screens, this test will fail and the resulting color will
            // be some combination of RED and BLUE (purple?).
            captureColor.set(robot.getPixelColor(x + SIZE / 2, y + SIZE / 2));
        });
        assertColorEquals(Color.BLUE, captureColor.get(), TOLERANCE);
    }

    @Test
    public void testScreenCapture() throws Exception {
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        Pane pane = new StackPane();
        InvalidationListener invalidationListener = observable -> setSceneLatch.countDown();
        Util.runAndWait(() -> {
            pane.setBackground(new Background(new BackgroundFill(MAGENTA, null, null)));
            scene = new Scene(pane, WIDTH, HEIGHT);
            stage.sceneProperty().addListener(observable -> {
                setSceneLatch.countDown();
                stage.sceneProperty().removeListener(invalidationListener);
            });
            stage.setScene(scene);
        });
        waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        AtomicReference<WritableImage> screenCaptureNotScaledToFit = new AtomicReference<>();
        AtomicReference<WritableImage> screenCaptureScaledToFit = new AtomicReference<>();
        Thread.sleep(1000);
        Util.runAndWait(() -> {
            int x = (int) stage.getX();
            int y = (int) stage.getY();
            screenCaptureNotScaledToFit.set(robot.getScreenCapture(null, x, y, WIDTH, HEIGHT, false));
            screenCaptureScaledToFit.set(robot.getScreenCapture(null, x, y, WIDTH, HEIGHT, true));
        });
        double screenScaleX = Screen.getPrimary().getOutputScaleX();
        double screenScaleY = Screen.getPrimary().getOutputScaleY();

        // Should be scaled to the primary screen x and y scales. Note that screenCaptureScaledToFit and
        // screenCaptureNotScaledToFit will be the same if screenScaleX = screenScaleY = 1.0 and in that case
        // this is redundant.
        Assert.assertEquals((double) WIDTH * screenScaleX, screenCaptureNotScaledToFit.get().getWidth(), 0.0001);
        Assert.assertEquals((double) HEIGHT * screenScaleY, screenCaptureNotScaledToFit.get().getHeight(), 0.0001);
        for (int x = 0; x < WIDTH * screenScaleX; x++) {
            for (int y = 0; y < HEIGHT * screenScaleY; y++) {
                assertColorEquals(MAGENTA, screenCaptureNotScaledToFit.get().getPixelReader().getColor(x, y), TOLERANCE);
            }
        }

        // Should have been shrunk to fit the requested size, but still contain the same thing (all magenta pixels).
        Assert.assertEquals((double) WIDTH, screenCaptureScaledToFit.get().getWidth(), 0.0001);
        Assert.assertEquals((double) HEIGHT, screenCaptureScaledToFit.get().getHeight(), 0.0001);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                assertColorEquals(MAGENTA, screenCaptureScaledToFit.get().getPixelReader().getColor(x, y), TOLERANCE);
            }
        }
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));
            robot = new Robot();
            stage.show();
        }
    }

    @BeforeClass
    public static void initFX() {
        new Thread(() -> Application.launch(TestApp.class, (String[])null)).start();
        waitForLatch(startupLatch, 10, "Timeout waiting for FX runtime to start");
    }

    @AfterClass
    public static void exit() {
        Platform.runLater(() -> stage.hide());
        Platform.exit();
    }

    public static void waitForLatch(CountDownLatch latch, int seconds, String msg) {
        try {
            if (!latch.await(seconds, TimeUnit.SECONDS)) {
                fail(msg);
            }
        } catch (Exception ex) {
            fail("Unexpected exception: " + ex);
        }
    }

    private static void assertColorEquals(Color expected, Color actual, double delta) {
        if (!testColorEquals(expected, actual, delta)) {
            throw new AssertionFailedError("expected: " + colorToString(expected)
                    + " but was: " + colorToString(actual));
        }
    }

    private static boolean testColorEquals(Color expected, Color actual, double delta) {
        double deltaRed = Math.abs(expected.getRed() - actual.getRed());
        double deltaGreen = Math.abs(expected.getGreen() - actual.getGreen());
        double deltaBlue = Math.abs(expected.getBlue() - actual.getBlue());
        double deltaOpacity = Math.abs(expected.getOpacity() - actual.getOpacity());
        return (deltaRed <= delta && deltaGreen <= delta && deltaBlue <= delta && deltaOpacity <= delta);
    }

    private static String colorToString(Color c) {
        int r = (int)(c.getRed() * 255.0);
        int g = (int)(c.getGreen() * 255.0);
        int b = (int)(c.getBlue() * 255.0);
        int a = (int)(c.getOpacity() * 255.0);
        return "rgba(" + r + "," + g + "," + b + "," + a + ")";
    }
}
