/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.javafx.PlatformUtil;

import test.util.Util;

// When a key equivalent is sent it may be processed by
// JavaFX and also trigger a menu item if the system menu
// bar is in use.
public class MenuDoubleShortcutTest {

    static CountDownLatch startupLatch = new CountDownLatch(1);

    static volatile Stage stage;
    static volatile TestApp testApp;
    static private final int delayMilliseconds = 100;

    private enum TestResult {
        IGNORED("Key press event triggered no actions"),
        FIREDTWICE("Key press event consumed by scene also fired menu bar item"),
        FIREDMENUITEM("Key press event fired menu bar item instead of scene"),
        FIREDSCENE("Key press event fired scene action instead of menu bar item");

        // We provide an explanation of what happened. Since we only see this
        // explanation on failure it is worded accordingly.
        private String explanation;
        TestResult(String e) {
            explanation = e;
        }

        public String errorExplanation() {
            return explanation;
        }
    };

    // KeyCode.A will be added to the menu bar and the scene
    // KeyCode.B will be added to the menu bar only
    // KeyCode.C will be added to the scene only.

    @Test
    void sceneComesBeforeMenuBar() {
        // Assumptions.assumeTrue(PlatformUtil.isMac());

        // KeyCode.A is in the menu bar and scene
        testApp.testKey(KeyCode.A);
        Util.sleep(delayMilliseconds);
        TestResult result = testApp.testResult();
        Assertions.assertEquals(TestResult.FIREDSCENE, result, result.errorExplanation());
    }

    @Test
    void acceleratorOnlyInMenuBar() {
        // Assumptions.assumeTrue(PlatformUtil.isMac());

        // KeyCode.B is only in the menu bar.
        testApp.testKey(KeyCode.B);
        Util.sleep(delayMilliseconds);
        TestResult result = testApp.testResult();
        Assertions.assertEquals(TestResult.FIREDMENUITEM, result, result.errorExplanation());
    }

    @Test
    void acceleratorOnlyInScene() {
        // Assumptions.assumeTrue(PlatformUtil.isMac());

        // KeyCode.C is only in the scene.
        testApp.testKey(KeyCode.C);
        Util.sleep(delayMilliseconds);
        TestResult result = testApp.testResult();
        Assertions.assertEquals(TestResult.FIREDSCENE, result, result.errorExplanation());
    }

    @Test
    void acceleratorAbsent() {
        // Assumptions.assumeTrue(PlatformUtil.isMac());

        // KeyCode.D is not registered as an accelerator
        testApp.testKey(KeyCode.D);
        Util.sleep(delayMilliseconds);
        TestResult result = testApp.testResult();
        Assertions.assertEquals(TestResult.IGNORED, result, result.errorExplanation());
    }

    @BeforeAll
    static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    static void exit() {
        Util.shutdown();
    }

    public static class TestApp extends Application {

        private boolean sceneAcceleratorFired = false;
        private boolean menuBarItemFired = false;

        private MenuItem createMenuItem(KeyCombination accelerator) {
            MenuItem menuItem = new MenuItem("Cmd+A menu item");
            menuItem.setAccelerator(accelerator);
            menuItem.setOnAction(e -> {
                menuBarItemFired = true;
                e.consume();
            });
            return menuItem;
        }

        @Override
        public void start(Stage primaryStage) {

            testApp = this;
            stage = primaryStage;

            Label label = new Label("Testing accelerator double processing");

            MenuBar menuBar = new MenuBar();
            menuBar.setUseSystemMenuBar(true);

            // KeyCode.A will be added to the menu bar and the scene
            // KeyCode.B will be added to the menu bar only
            // KeyCode.C will be added to the scene only.
            KeyCombination acceleratorA = new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN);
            KeyCombination acceleratorB = new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN);
            KeyCombination acceleratorC = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

            Menu menu = new Menu("Top menu");
            menu.getItems().add(createMenuItem(acceleratorA));
            menu.getItems().add(createMenuItem(acceleratorB));
            menuBar.getMenus().add(menu);

            Scene scene = new Scene(new VBox(menuBar, label), 200, 200);
            scene.getAccelerators().put(acceleratorA, () -> {
                sceneAcceleratorFired = true;
            });
            scene.getAccelerators().put(acceleratorC, () -> {
                sceneAcceleratorFired = true;
            });

            stage.setScene(scene);
            stage.setOnShown(e -> { startupLatch.countDown(); });
            stage.show();
        }

        public void testKey(KeyCode code) {
            sceneAcceleratorFired = false;
            menuBarItemFired = false;
            Platform.runLater(() -> {
                // Need to ensure Cmd is present so this is handled
                // as a key equivalent.
                Robot robot = new Robot();
                robot.keyPress(KeyCode.COMMAND);
                robot.keyPress(code);
                robot.keyRelease(code);
                robot.keyRelease(KeyCode.COMMAND);
            });
        }

        public TestResult testResult() {
            if (sceneAcceleratorFired && menuBarItemFired) {
                return TestResult.FIREDTWICE;
            } else if (sceneAcceleratorFired) {
                return TestResult.FIREDSCENE;
            } else if (menuBarItemFired) {
                return TestResult.FIREDMENUITEM;
            }
            return TestResult.IGNORED;
        }
    }
}
