/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sun.javafx.PlatformUtil;

import test.util.Util;

// Test a series of KeyCodes verifying that they at least generate a
// KEY_PRESSED event with the matching code. If the key generates a character
// we can also verify that the KEY_PRESSED event for that character matches
// the expected KeyCharacterCombination.
public class ShortcutKeyboardTest {

    static CountDownLatch startupLatch = new CountDownLatch(1);

    static volatile TestApp testApp;
    static volatile Stage stage;
    static volatile boolean isLatin = false;

    private enum KeyData {
        // These two keys are special-cased by macOS and can lead to multiple
        // calls to performKeyEquivalent. The platform code has logic to
        // prevent multiple KeyEvents from firing.
        EQUALS(KeyCode.EQUALS, "="),
        PERIOD(KeyCode.PERIOD, "."),

        PLUS(KeyCode.PLUS, "+"),
        MINUS(KeyCode.MINUS, "-"),
        COMMA(KeyCode.COMMA, ","),

        ADD(KeyCode.ADD, "+"),
        SUBTRACT(KeyCode.SUBTRACT, "-"),

        A(KeyCode.A, "a"),
        Q(KeyCode.Q, "q"),
        Y(KeyCode.Y, "y"),
        Z(KeyCode.Z, "z");

        final public KeyCode code;
        final public String combinationChar;

        KeyData(KeyCode k, String c) {
            code = k;
            combinationChar = c;
        }
    };

    @ParameterizedTest(name = "{0}")
    @EnumSource(KeyData.class)
    @Timeout(value = 3)
    void testKey(KeyData keyData) {
        Assumptions.assumeTrue(PlatformUtil.isMac(), "Mac-only test");
        Assumptions.assumeTrue(isLatin, "Non-Latin layout");
        Util.runAndWait(() -> testApp.testShortcutKey(keyData.code, keyData.combinationChar));
        String result = testApp.getTestResult();
        if (result != null) {
            Assertions.fail(result);
        }
    }

    @BeforeAll
    @Timeout(value = 15)
    static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);

        // When run from the command line Windows does not want to
        // activate the window.
        if (PlatformUtil.isWindows()) {
            Util.runAndWait(() -> {
                var robot = new Robot();
                var oldPosition = robot.getMousePosition();
                var root = stage.getScene().getRoot();
                var bounds = root.getBoundsInLocal();
                var mouseX = (bounds.getMinX() + bounds.getMaxX()) / 2.0;
                var mouseY = (bounds.getMinY() + bounds.getMaxY()) / 2.0;
                var clickPoint = root.localToScreen(mouseX, mouseY);
                robot.mouseMove(clickPoint);
                robot.mouseClick(MouseButton.PRIMARY);
                robot.mouseMove(oldPosition);
            });
        }

        Util.runAndWait(() -> testApp.testLatin());
    }

    @AfterAll
    static void exit() {
        Util.shutdown();
    }

    public static class TestApp extends Application {
        // We throw key events at a TextArea to ensure that the input method
        // logic is active.
        private final TextArea focusNode = new TextArea();
        private final AtomicReference<String> testResult = new AtomicReference<String>(null);

        @Override
        public void start(Stage primaryStage) {
            testApp = this;
            stage = primaryStage;

            focusNode.setEditable(false);
            Scene scene = new Scene(focusNode, 200, 200);
            primaryStage.setScene(scene);
            primaryStage.setOnShown(event -> {
                Platform.runLater(startupLatch::countDown);
            });
            primaryStage.show();
        }

        // At the end of the test getTestResult() will return null on success.
        // Otherwise it will return a string describing what failed.
        private void testShortcutKey(KeyCode characterKeyCode, String character) {
            focusNode.requestFocus();

            final var modifierKeyCode = PlatformUtil.isMac() ? KeyCode.COMMAND : KeyCode.CONTROL;
            final var combination = new KeyCharacterCombination(character, KeyCombination.SHORTCUT_DOWN);

            // We assume failure until we see the modifier key arrive.
            testResult.set("Did not see the initial modifer PRESSED event");

            Object eventLoop = new Object();

            // If we never see the modifier released something has gone wrong.
            var timeoutTask = new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        testResult.set("Timeout waiting for modifier RELEASED event");
                        Platform.exitNestedEventLoop(eventLoop, null);
                    });
                }
            };

            // First we should see the modifier pressed, then the accelerator character.
            final EventHandler<KeyEvent> pressedHandler = (e -> {
                if (e.getCode() == modifierKeyCode) {
                    // So far so good. For a letter key we expect another
                    // PRESSED event and assume failure until it arrives.
                    // Other codes may not be present on this layout so it's
                    // not an error if no events arrive.
                    if (characterKeyCode.isLetterKey()) {
                        testResult.set("Did not see character PRESSED event");
                    }
                    else {
                        testResult.set(null);
                    }
                }
                else if (e.getCode() == characterKeyCode) {
                    testResult.set(null);
                    if (!combination.match(e)) {
                        testResult.set("Character key " + e.getCode() + " did not match " + combination);
                    }
                }
                else {
                    testResult.set("Unexpected character key " + e.getCode());
                }
                e.consume();
            });

            // The test is over when the modifier is released.
            final EventHandler<KeyEvent> releasedHandler = (e -> {
                if (e.getCode() == modifierKeyCode) {
                    timeoutTask.cancel();
                    Platform.exitNestedEventLoop(eventLoop, null);
                }
                e.consume();
            });

            focusNode.addEventFilter(KeyEvent.KEY_PRESSED, pressedHandler);
            focusNode.addEventFilter(KeyEvent.KEY_RELEASED, releasedHandler);
            final var timer = new Timer();
            timer.schedule(timeoutTask, 100);

            final var robot = new Robot();
            robot.keyPress(modifierKeyCode);
            robot.keyPress(characterKeyCode);
            robot.keyRelease(characterKeyCode);
            robot.keyRelease(modifierKeyCode);

            // Wait for the final event to arrive or the timout to fire
            Platform.enterNestedEventLoop(eventLoop);

            focusNode.removeEventFilter(KeyEvent.KEY_PRESSED, pressedHandler);
            focusNode.removeEventFilter(KeyEvent.KEY_RELEASED, releasedHandler);
            timeoutTask.cancel();
            timer.cancel();
        }

        // Send KeyCode.A and verify we get an "a" back.
        private void testLatin() {
            focusNode.requestFocus();

            Object eventLoop = new Object();

            // In case we don't see the release event
            var timeoutTask = new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        isLatin = false;
                        Platform.exitNestedEventLoop(eventLoop, null);
                    });
                }
            };

            final EventHandler<KeyEvent> typedHandler = (e -> {
                if (e.getCharacter().equals("a")) {
                    isLatin = true;
                }
                e.consume();
            });

            final EventHandler<KeyEvent> releasedHandler = (e -> {
                e.consume();
                Platform.exitNestedEventLoop(eventLoop, null);
            });

            focusNode.addEventFilter(KeyEvent.KEY_TYPED, typedHandler);
            focusNode.addEventFilter(KeyEvent.KEY_RELEASED, releasedHandler);
            final var timer = new Timer();
            timer.schedule(timeoutTask, 100);

            final Robot robot = new Robot();
            robot.keyPress(KeyCode.A);
            robot.keyRelease(KeyCode.A);

            Platform.enterNestedEventLoop(eventLoop);

            focusNode.removeEventFilter(KeyEvent.KEY_TYPED, typedHandler);
            focusNode.removeEventFilter(KeyEvent.KEY_RELEASED, releasedHandler);
            timeoutTask.cancel();
            timer.cancel();
        }

        public String getTestResult() {
            return testResult.get();
        }
    }
}
