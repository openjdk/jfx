/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.paint.Color.MAGENTA;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.sun.javafx.PlatformUtil;
import test.util.Util;

/**
 * Tests to verify that the native robot implementations all work correctly.
 */
public class RobotTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static volatile Stage stage;
    static volatile Scene scene;
    static Robot robot;
    static Set<MouseButton> pressedButtons = new HashSet<>();

    // A tolerance is needed because on macOS the pixel colors are affected by the configured "color profile"
    // of the display.
    private static final double TOLERANCE = 0.07;
    private static final int SIZE = 400;
    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;

    public static void main(String[] args) throws Exception {
        RobotTest test = new RobotTest();
        test.testKeyPress();
        test.testKeyType();
        test.testKeyPressThrowsISEOnWrongThread();
        test.testKeyPressThrowsNPEForNullArgument();
        test.testKeyReleaseThrowsISEOnWrongThread();
        test.testKeyReleaseThrowsNPEForNullArgument();
        test.testMouseMoveDouble();
        test.testMouseMovePoint2D();
        test.testMouseMoveThrowsISEOnWrongThread();
        test.testMouseMoveThrowsNPEForNullArgument();
        test.testMousePressPrimary();
        test.testMousePressSecondary();
        test.testMousePressMiddle();
        test.testMousePressBack();
        test.testMousePressForward();
        test.testMouseClickPrimary();
        test.testMouseClickSecondary();
        test.testMouseClickMiddle();
        test.testMouseClickForward();
        test.testMouseClickBack();
        test.testMousePressThrowsISEOnWrongThread();
        test.testMousePressThrowsNPEForNullArgument();
        test.testMouseReleaseThrowsISEOnWrongThread();
        test.testMouseReleaseThrowsNPEForNullArgument();
        test.testMouseClickThrowsISEOnWrongThread();
        test.testMouseClickThrowsNPEForNullArgument();
        test.testMouseDragPrimary();
        test.testMouseDragSecondary();
        test.testMouseDragMiddle();
        test.testMouseDragForward();
        test.testMouseDragBack();
        test.testMouseWheelPositiveAmount();
        test.testMouseWheelNegativeAmount();
        test.testMouseWheelThrowsISEOnWrongThread();
        test.testPixelCaptureDouble();
        test.testPixelCapturePoint2D();
        test.testPixelCaptureAverage();
        test.testPixelCaptureThrowsISEOnWrongThread();
        test.testPixelCaptureThrowsNPEForNullArgument();
        test.testScreenCapture();
        test.testScreenCaptureThrowsISEOnWrongThread();
        test.testScreenCaptureThrowsNPEForNullArgument();
        exit();
    }

    private enum KeyAction {
        PRESSED,
        TYPED
    }

    @BeforeEach
    public void before() {
        Util.parkCursor(robot);
    }

    @Test
    public void testKeyPress() {
        testKeyboard(KeyAction.PRESSED);
    }

    @Test
    public void testKeyType() {
        testKeyboard(KeyAction.TYPED);
    }

    private static void testKeyboard(KeyAction keyAction) {
        CountDownLatch keyActionLatch = new CountDownLatch(1);
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        TextField textField = new TextField();
        InvalidationListener invalidationListener = observable -> setSceneLatch.countDown();
        Util.runAndWait(() -> {
            switch (keyAction) {
                case PRESSED:
                    textField.setOnKeyPressed(event -> keyActionLatch.countDown());
                    break;
                case TYPED:
                    textField.setOnKeyTyped(event -> keyActionLatch.countDown());
                    break;
            }
            scene = new Scene(new HBox(textField));
            stage.sceneProperty().addListener(observable -> {
                setSceneLatch.countDown();
                stage.sceneProperty().removeListener(invalidationListener);
            });
            stage.setScene(scene);
        });
        Util.waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        Util.runAndWait(() -> {
            int mouseX = (int) (scene.getWindow().getX() + scene.getX() +
                    textField.getLayoutX() + textField.getLayoutBounds().getWidth() / 2);
            int mouseY = (int) (scene.getWindow().getY() + scene.getY() +
                    textField.getLayoutY() + textField.getLayoutBounds().getHeight() / 2);
            robot.mouseMove(mouseX, mouseY);
            robot.mouseClick(MouseButton.PRIMARY);
            switch (keyAction) {
                case PRESSED:
                    robot.keyPress(KeyCode.A);
                    break;
                case TYPED:
                    robot.keyPress(KeyCode.A);
                    robot.keyRelease(KeyCode.A);
                    break;
            }
        });
        Util.waitForLatch(keyActionLatch, 5, "Timeout while waiting for textField.onKey" +
                capFirst(keyAction.name()) + "().");
        Assertions.assertEquals(
            "a", textField.getText(),
            "letter 'a' should be " + keyAction.name().toLowerCase() + " by Robot");
    }

    @Test
    public void testKeyPressThrowsISEOnWrongThread() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            robot.keyPress(KeyCode.A);
        });
    }

    @Test
    public void testKeyPressThrowsNPEForNullArgument() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            robot.keyPress(null);
        });
    }

    @Test
    public void testKeyReleaseThrowsISEOnWrongThread() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            robot.keyRelease(KeyCode.A);
        });
    }

    @Test
    public void testKeyReleaseThrowsNPEForNullArgument() {
        Util.runAndWait(() -> {
            Assertions.assertThrows(NullPointerException.class, () -> {
                robot.keyRelease(null);
            });
        });
    }

    @Test
    public void testMouseMoveDouble() {
        testMouseMove(50, 50, true);
    }

    @Test
    public void testMouseMovePoint2D() {
        testMouseMove(30, 30, false);
    }

    private static void testMouseMove(int x, int y, boolean primitiveArg) {
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
        Util.waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        AtomicReference<Point2D> mousePosition = new AtomicReference<>();
        Util.runAndWait(() -> {
            if (primitiveArg) {
                robot.mouseMove(x, y);
            } else {
                robot.mouseMove(new Point2D(x, y));
            }
            mousePosition.set(robot.getMousePosition());
        });
        Assertions.assertEquals(x, (int) mousePosition.get().getX());
        Assertions.assertEquals(y, (int) mousePosition.get().getY());
    }

    @Test
    public void testMouseMoveThrowsISEOnWrongThread() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            robot.mouseMove(0, 0);
        });
    }

    @Test
    public void testMouseMoveThrowsNPEForNullArgument() {
        Util.runAndWait(() -> {
            Assertions.assertThrows(NullPointerException.class, () -> {
                robot.mouseMove(null);
            });
        });
    }

    @Test
    public void testMousePressPrimary() {
        testMouseAction(MouseAction.PRESSED, MouseButton.PRIMARY);
    }

    @Test
    public void testMousePressSecondary() {
        testMouseAction(MouseAction.PRESSED, MouseButton.SECONDARY);
    }

    @Test
    public void testMousePressMiddle() {
        testMouseAction(MouseAction.PRESSED, MouseButton.MIDDLE);
    }

    @Test
    public void testMousePressBack() {
        testMouseAction(MouseAction.PRESSED, MouseButton.BACK);
    }

    @Test
    public void testMousePressForward() {
        testMouseAction(MouseAction.PRESSED, MouseButton.FORWARD);
    }

    @Test
    public void testMouseClickPrimary() {
        testMouseAction(MouseAction.CLICKED, MouseButton.PRIMARY);
    }

    @Test
    public void testMouseClickSecondary() {
        testMouseAction(MouseAction.CLICKED, MouseButton.SECONDARY);
    }

    @Test
    public void testMouseClickMiddle() {
        testMouseAction(MouseAction.CLICKED, MouseButton.MIDDLE);
    }

    @Test
    public void testMouseClickForward() {
        testMouseAction(MouseAction.CLICKED, MouseButton.FORWARD);
    }

    @Test
    public void testMouseClickBack() {
        testMouseAction(MouseAction.CLICKED, MouseButton.BACK);
    }

    private enum MouseAction {
        PRESSED,
        CLICKED
    }

    private static class MouseActionHandler implements EventHandler<MouseEvent> {
        private final Button button;
        private final MouseAction mouseAction;
        private final MouseButton mouseButton;
        private final CountDownLatch onClickLatch;

        MouseActionHandler(Button button, MouseAction mouseAction,
                           MouseButton mouseButton, CountDownLatch onClickLatch) {
            this.button = button;
            this.mouseAction = mouseAction;
            this.mouseButton = mouseButton;
            this.onClickLatch = onClickLatch;
        }

        @Override
        public void handle(MouseEvent event) {
            String expectedText = mouseAction + " " + mouseButton;
            if (event.getButton() == mouseButton) {
                button.setText(expectedText);
            } else {
                button.setText(String.format(mouseAction + " wrong button (expected \"%s\" but got \"%s\")",
                        mouseButton, event.getButton()));
            }
            onClickLatch.countDown();
        }
    }

    private static void testMouseAction(MouseAction mouseAction, MouseButton mouseButton) {
        CountDownLatch onClickLatch = new CountDownLatch(1);
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        Button button = new Button("Click me");
        InvalidationListener invalidationListener = observable -> setSceneLatch.countDown();
        String expectedText = mouseAction + " " + mouseButton;
        Util.runAndWait(() -> {
            MouseActionHandler mouseActionHandler = new MouseActionHandler(button, mouseAction, mouseButton, onClickLatch);
            switch (mouseAction) {
                case PRESSED:
                    button.setOnMousePressed(mouseActionHandler);
                    break;
                case CLICKED:
                    button.setOnMouseClicked(mouseActionHandler);
                    break;
            }
            scene = new Scene(new HBox(button), SIZE, SIZE);
            stage.sceneProperty().addListener(observable -> {
                setSceneLatch.countDown();
                stage.sceneProperty().removeListener(invalidationListener);
            });
            stage.setScene(scene);
        });
        Util.waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        int mouseX = (int) (scene.getWindow().getX() + scene.getX() +
                button.getLayoutX() + button.getLayoutBounds().getWidth() / 2);
        int mouseY = (int) (scene.getWindow().getY() + scene.getY() +
                button.getLayoutY() + button.getLayoutBounds().getHeight() / 2);
        Util.runAndWait(() -> {
            robot.mouseMove(mouseX, mouseY);
            switch (mouseAction) {
                case PRESSED:
                    robot.mousePress(mouseButton);
                    pressedButtons.add(mouseButton);
                    break;
                case CLICKED:
                    robot.mousePress(mouseButton);
                    robot.mouseRelease(mouseButton);
                    break;
            }
        });
        Util.waitForLatch(onClickLatch, 5, "Timeout while waiting for button.onMouse" +
                capFirst(mouseAction.name()) + "().");
        Assertions.assertEquals(
            expectedText, button.getText(),
            mouseButton + " mouse button should be " + mouseAction.name().toLowerCase() + " by Robot");
    }

    @Test
    public void testMousePressThrowsISEOnWrongThread() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            robot.mousePress(MouseButton.PRIMARY);
        });
    }

    @Test
    public void testMousePressThrowsNPEForNullArgument() {
        Util.runAndWait(() -> {
            Assertions.assertThrows(NullPointerException.class, () -> {
                robot.mousePress((MouseButton[])null);
            });
        });
    }

    @Test
    public void testMouseReleaseThrowsISEOnWrongThread() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    @Test
    public void testMouseReleaseThrowsNPEForNullArgument() {
        Util.runAndWait(() -> {
            Assertions.assertThrows(NullPointerException.class, () -> {
                robot.mouseRelease((MouseButton[])null);
            });
        });
    }

    @Test
    public void testMouseClickThrowsISEOnWrongThread() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            robot.mouseClick(MouseButton.PRIMARY);
        });
    }

    @Test
    public void testMouseClickThrowsNPEForNullArgument() {
        Util.runAndWait(() -> {
            Assertions.assertThrows(NullPointerException.class, () -> {
                robot.mouseClick((MouseButton[])null);
            });
        });
    }

    @Test
    @Disabled("Flaky - see JDK-8215376")
    public void testMouseDragPrimary() {
        testMouseDrag(MouseButton.PRIMARY);
    }

    @Test
    @Disabled("Flaky - see JDK-8215376")
    public void testMouseDragSecondary() {
        testMouseDrag(MouseButton.SECONDARY);
    }

    @Test
    @Disabled("Flaky - see JDK-8215376")
    public void testMouseDragMiddle() {
        Assumptions.assumeTrue(!PlatformUtil.isMac() ); // See JDK-8215376
        testMouseDrag(MouseButton.MIDDLE);
    }

    @Test
    @Disabled("Flaky - see JDK-8215376")
    public void testMouseDragForward() {
        Assumptions.assumeTrue(!PlatformUtil.isMac()); // See JDK-8215376
        testMouseDrag(MouseButton.FORWARD);
    }

    @Test
    @Disabled("Flaky - see JDK-8215376")
    public void testMouseDragBack() {
        Assumptions.assumeTrue(!PlatformUtil.isMac()); // See JDK-8215376
        testMouseDrag(MouseButton.BACK);
    }

    public void testMouseDrag(MouseButton mouseButton) {
        CountDownLatch mouseDragLatch = new CountDownLatch(1);
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        Label label = new Label("Source");
        InvalidationListener invalidationListener = observable -> setSceneLatch.countDown();
        Util.runAndWait(() -> {
            label.setOnMouseDragged(event -> {
                if (event.getButton() == mouseButton) {
                    mouseDragLatch.countDown();
                }
            });
            scene = new Scene(new HBox(label));
            stage.sceneProperty().addListener(observable -> {
                setSceneLatch.countDown();
                stage.sceneProperty().removeListener(invalidationListener);
            });
            stage.setScene(scene);
        });
        Util.waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        Util.runAndWait(() -> {
            int mouseX = (int) (scene.getWindow().getX() + scene.getX() +
                    label.getLayoutX() + label.getLayoutBounds().getWidth() / 2);
            int mouseY = (int) (scene.getWindow().getY() + scene.getY() +
                    label.getLayoutY() + label.getLayoutBounds().getHeight() / 2);
            robot.mouseMove(mouseX, mouseY);
            robot.mousePress(mouseButton);
            for (int i = 1; i <= 50; i++) {
                robot.mouseMove(mouseX + i, mouseY);
            }
            robot.mouseRelease(mouseButton);
        });
        Util.waitForLatch(mouseDragLatch, 5, "Timeout while waiting for button.onMouseDragged().");
    }

    @Test
    public void testMouseWheelPositiveAmount() {
        testMouseWheel(5);
    }

    @Test
    public void testMouseWheelNegativeAmount() {
        testMouseWheel(-5);
    }

    private static void testMouseWheel(int amount) {
        Assumptions.assumeTrue(!PlatformUtil.isMac()); // See JDK-8214580
        CountDownLatch onScrollLatch = new CountDownLatch(1);
        CountDownLatch setSceneLatch = new CountDownLatch(1);
        Button button = new Button("Scroll me");
        InvalidationListener invalidationListener = observable -> setSceneLatch.countDown();
        int[] totalScroll = new int[]{0};
        long[] firstScrollMillis = new long[]{0};
        // The scroll wheel amount is multiplied by 40 on Linux and Windows, but not on macOS. The
        // directions are also reversed. This difference in behavior is unexplained and might be a bug.
        // See JDK-8214580.
        int scrollMultiplier = PlatformUtil.isMac() ? -1 : 40;
        Util.runAndWait(() -> {
            button.setOnScroll(event -> {
                totalScroll[0] += event.getDeltaY() * Screen.getPrimary().getOutputScaleY();
                if (firstScrollMillis[0] == 0) {
                    firstScrollMillis[0] = System.currentTimeMillis();
                } else {
                    if (System.currentTimeMillis() - firstScrollMillis[0] > 1000) {
                        button.setText("Scrolled " + totalScroll[0]);
                        onScrollLatch.countDown();
                    }
                }
                if (Math.abs(totalScroll[0] / scrollMultiplier) >= Math.abs(amount)) {
                    button.setText("Scrolled " + -(totalScroll[0] / scrollMultiplier));
                    onScrollLatch.countDown();
                }
            });
            scene = new Scene(new HBox(button), SIZE, SIZE);
            stage.sceneProperty().addListener(observable -> {
                setSceneLatch.countDown();
                stage.sceneProperty().removeListener(invalidationListener);
            });
            stage.setScene(scene);
        });
        Util.waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        Util.runAndWait(() -> {
            int mouseX = (int) (scene.getWindow().getX() + scene.getX() +
                    button.getLayoutX() + button.getLayoutBounds().getWidth() / 2);
            int mouseY = (int) (scene.getWindow().getY() + scene.getY() +
                    button.getLayoutY() + button.getLayoutBounds().getHeight() / 2);
            robot.mouseMove(mouseX, mouseY);
            robot.mouseWheel(amount);
        });
        Util.waitForLatch(onScrollLatch, 5, "Timeout while waiting for button.onScroll().");
        Assertions.assertEquals(
            "Scrolled " + amount, button.getText(),
            "mouse wheel should be scrolled " + amount + " vertical units by Robot");
    }

    @Test
    public void testMouseWheelThrowsISEOnWrongThread() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            robot.mouseWheel(1);
        });
    }

    @Test
    public void testPixelCaptureDouble() throws Exception {
        testPixelCapture(true);
    }

    @Test
    public void testPixelCapturePoint2D() throws Exception {
        testPixelCapture(false);
    }

    private static void testPixelCapture(boolean primitiveArg)
            throws InterruptedException {
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
        Util.waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        AtomicReference<Color> captureColor = new AtomicReference<>();
        Thread.sleep(1000);
        Util.runAndWait(() -> {
            int x = (int) stage.getX();
            int y = (int) stage.getY();
            if (primitiveArg) {
                captureColor.set(robot.getPixelColor(x + SIZE / 2, y + SIZE / 2));
            } else {
                captureColor.set(robot.getPixelColor(new Point2D(x + SIZE / 2, y + SIZE / 2)));
            }
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
        Util.waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
        AtomicReference<Color> captureColor = new AtomicReference<>();
        Thread.sleep(1000);
        Util.runAndWait(() -> {
            int x = (int)(Math.round(stage.getX()));
            int y = (int)(Math.round(stage.getY()));
            // Subtracting one pixel from x makes the result RED, so we are on the border.
            // If the implementation of getPixelColor is ever chaged to interpolate the
            // colors on HiDPI screens, this test will fail and the resulting color will
            // be some combination of RED and BLUE (purple?).
            captureColor.set(robot.getPixelColor(x + SIZE / 2, y + SIZE / 2));
        });
        assertColorEquals(Color.BLUE, captureColor.get(), TOLERANCE);
    }

    @Test
    public void testPixelCaptureThrowsISEOnWrongThread() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            robot.getPixelColor(20, 20);
        });
    }

    @Test
    public void testPixelCaptureThrowsNPEForNullArgument() {
        Util.runAndWait(() -> {
            Assertions.assertThrows(NullPointerException.class, () -> {
                robot.getPixelColor(null);
            });
        });
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
        Util.waitForLatch(setSceneLatch, 5, "Timeout while waiting for scene to be set on stage.");
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
        // Below calculations follow how getScreenCapture should calculate screen capture dimensions. This
        // is to make this code consistent and stable on HiDPI systems.
        int stageX = (int) stage.getX();
        int stageY = (int) stage.getY();
        int shouldBeMinX = (int) Math.floor(stageX * screenScaleX);
        int shouldBeMinY = (int) Math.floor(stageY * screenScaleY);
        int shouldBeMaxX = (int) Math.ceil((stageX + WIDTH) * screenScaleX);
        int shouldBeMaxY = (int) Math.ceil((stageY + HEIGHT) * screenScaleY);
        int shouldBeWidth = shouldBeMaxX - shouldBeMinX;
        int shouldBeHeight = shouldBeMaxY - shouldBeMinY;
        Assertions.assertEquals((double) shouldBeWidth, screenCaptureNotScaledToFit.get().getWidth(), 0.0001);
        Assertions.assertEquals((double) shouldBeHeight, screenCaptureNotScaledToFit.get().getHeight(), 0.0001);

        // To verify the color we're going to skip the "1-pixel outside border" of the capture. On HiDPI systems
        // (especially on Windows) stage's position might have fractional element, which will mean the capture will
        // average those values with whatever is behind the stage. This will make their values invalid.
        for (int x = 1; x < shouldBeWidth - 1; x++) {
            for (int y = 1; y < shouldBeHeight - 1; y++) {
                assertColorEquals(MAGENTA, screenCaptureNotScaledToFit.get().getPixelReader().getColor(x, y), TOLERANCE);
            }
        }

        // Should have been shrunk to fit the requested size, but still contain the same thing (all magenta pixels).
        Assertions.assertEquals((double) WIDTH, screenCaptureScaledToFit.get().getWidth(), 0.0001);
        Assertions.assertEquals((double) HEIGHT, screenCaptureScaledToFit.get().getHeight(), 0.0001);
        // Because of scaling, similar estimate has to be done like above.
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int y = 1; y < HEIGHT - 1; y++) {
                assertColorEquals(MAGENTA, screenCaptureScaledToFit.get().getPixelReader().getColor(x, y), TOLERANCE);
            }
        }
    }

    @Test
    public void testScreenCaptureThrowsISEOnWrongThread() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            robot.getScreenCapture(null, 0, 0, 10, 10);
        });
    }

    @Test
    public void testScreenCaptureThrowsNPEForNullArgument() {
        Util.runAndWait(() -> {
            Assertions.assertThrows(NullPointerException.class, () -> {
                robot.getScreenCapture(null, null);
            });
        });
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

    @BeforeAll
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown();
    }

    @AfterEach
    public void cleanup() {
        Util.runAndWait(() -> {
            if (!pressedButtons.isEmpty()) {
                robot.mouseRelease(pressedButtons.toArray(new MouseButton[]{}));
                pressedButtons.clear();
            }
            robot.keyRelease(KeyCode.A);
        });
    }

    private static void assertColorEquals(Color expected, Color actual, double delta) {
        if (!testColorEquals(expected, actual, delta)) {
            Assertions.fail("expected: " + colorToString(expected)
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

    private static String capFirst(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }
}
