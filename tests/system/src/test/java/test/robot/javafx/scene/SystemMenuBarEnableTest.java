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
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.javafx.PlatformUtil;

import test.util.Util;

/**
 * Tests that system menu bar items are correctly re-enabled after their
 * parent menu is disabled and then re-enabled.
 */
public class SystemMenuBarEnableTest {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static Menu menu;

    private static final int DELAY = 100;

    // Estimated x, y offset for the menu in the macOS system menu bar (after the Apple menu and java menu).
    private static final int MENU_BAR_X = 140;
    private static final int MENU_BAR_Y = 10;

    private static Robot robot;
    private static final AtomicBoolean enabledItemFired = new AtomicBoolean(false);
    private static final AtomicBoolean disabledItemFired = new AtomicBoolean(false);

    @BeforeAll
    static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    static void exit() {
        Util.shutdown();
    }

    /**
     * Verifies that after disabling and re-enabling a parent menu, its child
     * menu item can still be selected via the system menu bar.
     */
    @Test
    void testMenuItemEnabledAfterParentReEnabled() {
        Assumptions.assumeTrue(PlatformUtil.isMac(), "System menu bar tests only apply to macOS");

        // open and close the menu first, first item is enabled, second is disabled
        robotMenu(false, false);

        // Disable the parent menu
        Util.runAndWait(() -> menu.setDisable(true));

        // open and close the menu again, all items should be disabled
        robotMenu(false, false);

        // Re-enable the parent menu
        Util.runAndWait(() -> menu.setDisable(false));

        // Open the menu and select the first item, as it should be enabled again
        robotMenu(true, false);

        Assertions.assertTrue(enabledItemFired.get(),
                "Menu item action should fire after parent menu is re-enabled");

        // Open the menu and try to select the second item, it should still be disabled and not fire
        robotMenu(false, true);
        Assertions.assertFalse(disabledItemFired.get(),
                "Menu item action should fire after parent menu is re-enabled");
    }

    private void robotMenu(boolean selectFirstItem, boolean selectSecondItem) {
        // Click on the system menu bar, in order to show the menu
        Util.runAndWait(() -> {
            robot.mouseMove(MENU_BAR_X, MENU_BAR_Y);
            robot.mouseClick(MouseButton.PRIMARY);
        });
        Util.sleep(DELAY);

        if (selectFirstItem) {
            // Select the first item via keyboard
            Util.runAndWait(() -> {
                robot.keyType(KeyCode.DOWN);
                robot.keyType(KeyCode.ENTER);
            });
            Util.sleep(DELAY);
        } else if (selectSecondItem) {
            // Select the second item via keyboard
            Util.runAndWait(() -> {
                robot.keyType(KeyCode.DOWN);
                robot.keyType(KeyCode.DOWN);
                robot.keyType(KeyCode.ENTER);
            });
            Util.sleep(DELAY);
        }

        // hide menu
        Util.runAndWait(() -> robot.keyType(KeyCode.ESCAPE));
        Util.sleep(DELAY);
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) {
            robot = new Robot();

            MenuItem enabledItem = new MenuItem("Enabled Item");
            enabledItem.setOnAction(_ -> enabledItemFired.set(true));

            MenuItem disabledItem = new MenuItem("Disabled Item");
            disabledItem.setOnAction(_ -> disabledItemFired.set(true));
            disabledItem.setDisable(true);

            menu = new Menu("TestMenu", null, enabledItem, disabledItem);
            MenuBar menuBar = new MenuBar(menu);
            menuBar.setUseSystemMenuBar(true);

            Label label = new Label("System Menu Bar Enable Test");
            VBox root = new VBox(menuBar, label);
            root.setAlignment(Pos.CENTER);

            Scene scene = new Scene(root, 300, 200);
            stage.setScene(scene);
            stage.setOnShown(_ -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }

    }
}

