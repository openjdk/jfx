/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sun.javafx.PlatformUtil;

import test.util.Util;

public class MenuDoubleShortcutTest {

    static CountDownLatch startupLatch = new CountDownLatch(1);

    static volatile TestApp testApp;
    static private final int delayMilliseconds = 100;

    private enum TestResult {
        // We provide an explanation of what happened. Since we only see this
        // explanation on failure it is worded accordingly.
        IGNORED("Key press event triggered no actions"),
        FIREDTWICE("Key press event fired scene action and also a menu bar item"),
        FIREDMENUITEM("Key press event fired menu bar item instead of scene action"),
        FIREDSCENE("Key press event fired scene action instead of menu bar item");

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
    private static final KeyCode menuBarAndSceneKeyCode = KeyCode.A;
    private static final KeyCode menuBarOnlyKeyCode = KeyCode.B;
    private static final KeyCode sceneOnlyKeyCode = KeyCode.C;
    private static final KeyCode noAcceleratorKeyCode = KeyCode.D;

    private static final KeyCombination menuBarAndSceneAccelerator = new KeyCodeCombination(menuBarAndSceneKeyCode, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination menuBarOnlyAccelerator = new KeyCodeCombination(menuBarOnlyKeyCode, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination sceneOnlyAccelerator = new KeyCodeCombination(sceneOnlyKeyCode, KeyCombination.SHORTCUT_DOWN);

    // On Mac the scene should process the event and it should
    // not trigger a system menu bar item.
    //
    // https://bugs.openjdk.org/browse/JDK-8087863
    // https://bugs.openjdk.org/browse/JDK-8088897
    @Disabled("JDK-8364405")
    @Test
    void macSceneComesBeforeMenuBar() {
        Assumptions.assumeTrue(PlatformUtil.isMac());
        testApp.testKey(menuBarAndSceneKeyCode);
        Util.sleep(delayMilliseconds);
        TestResult result = testApp.testResult();
        Assertions.assertEquals(TestResult.FIREDSCENE, result, result.errorExplanation());
    }

    // On platforms other than Mac the menu bar should process the event
    // and the scene should not.
    @Disabled("JDK-8364405")
    @Test
    void nonMacMenuBarComesBeforeScene() {
        Assumptions.assumeFalse(PlatformUtil.isMac());
        testApp.testKey(menuBarAndSceneKeyCode);
        Util.sleep(delayMilliseconds);
        TestResult result = testApp.testResult();
        Assertions.assertEquals(TestResult.FIREDMENUITEM, result, result.errorExplanation());
    }

    @Test
    void acceleratorOnlyInMenuBar() {
        Util.sleep(delayMilliseconds);
        testApp.testKey(menuBarOnlyKeyCode);
        Util.sleep(delayMilliseconds);
        TestResult result = testApp.testResult();
        Assertions.assertEquals(TestResult.FIREDMENUITEM, result, result.errorExplanation());
    }

    @Test
    void acceleratorOnlyInScene() {
        testApp.testKey(sceneOnlyKeyCode);
        Util.sleep(delayMilliseconds);
        TestResult result = testApp.testResult();
        Assertions.assertEquals(TestResult.FIREDSCENE, result, result.errorExplanation());
    }

    @Test
    void acceleratorAbsent() {
        testApp.testKey(noAcceleratorKeyCode);
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
            MenuItem menuItem = new MenuItem(accelerator.getName() + " menu item");
            menuItem.setAccelerator(accelerator);
            menuItem.setOnAction(e -> {
                menuBarItemFired = true;
                e.consume();
            });
            return menuItem;
        }

        @Override
        public void start(Stage stage) {

            testApp = this;

            Label label = new Label("Testing accelerator double processing");

            MenuBar menuBar = new MenuBar();
            menuBar.setUseSystemMenuBar(true);

            Menu menu = new Menu("Top menu");
            menu.getItems().add(createMenuItem(menuBarAndSceneAccelerator));
            menu.getItems().add(createMenuItem(menuBarOnlyAccelerator));
            menuBar.getMenus().add(menu);

            Scene scene = new Scene(new VBox(menuBar, label), 200, 200);
            scene.getAccelerators().put(menuBarAndSceneAccelerator, () -> {
                sceneAcceleratorFired = true;
            });
            scene.getAccelerators().put(sceneOnlyAccelerator, () -> {
                sceneAcceleratorFired = true;
            });

            stage.setScene(scene);
            stage.setOnShown(e -> {
                Platform.runLater(() -> {
                    startupLatch.countDown();
                });
            });
            stage.show();
        }

        public void testKey(KeyCode code) {
            sceneAcceleratorFired = false;
            menuBarItemFired = false;
            Platform.runLater(() -> {
                KeyCode shortcutCode = (PlatformUtil.isMac() ? KeyCode.COMMAND : KeyCode.CONTROL);
                Robot robot = new Robot();
                robot.keyPress(shortcutCode);
                robot.keyPress(code);
                robot.keyRelease(code);
                robot.keyRelease(shortcutCode);
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
