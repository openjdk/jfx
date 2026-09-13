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

import com.sun.javafx.PlatformUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

import java.util.concurrent.CountDownLatch;

/**
 * Tests that the system menu bar consumes a mouse click event when there is an auto-hide popup window
 * showing in the active stage, and processes the event when there is a non-auto-hide popup window showing.
 */
public class SystemMenuBarClickEventsTest {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static volatile Menu menu;
    private static volatile ContextMenu contextMenu;
    private static volatile Label label;

    private static final int DELAY = 100;

    // Estimated x, y offset for the menu in the macOS system menu bar (after the Apple menu and java menu).
    private static final int MENU_BAR_X = 140;
    private static final int MENU_BAR_Y = 10;

    private static Robot robot;

    @BeforeAll
    static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    static void exit() {
        Util.shutdown();
    }

    /**
     * Verifies that the system menu bar consumes a mouse click event when there is an
     * auto-hide popup window showing in the active stage: the popup window gets dismissed,
     * and the system menu bar doesn't show the menu.
     */
    @Test
    void testSystemMenuBarConsumesClickEvent() {
        Assumptions.assumeTrue(PlatformUtil.isMac(), "System menu bar tests only apply to macOS");

        // set auto-hide popup
        Util.runAndWait(() -> contextMenu.setAutoHide(true));

        // Right-click on the label, the context menu shows up
        Util.runAndWait(() -> {
            Bounds bounds = label.localToScreen(label.getBoundsInLocal());
            double x = bounds.getMinX() + bounds.getWidth() / 2;
            double y = bounds.getMinY() + bounds.getHeight() / 2;
            robot.mouseMove((int) x, (int) y);
            robot.mouseClick(MouseButton.SECONDARY);
        });

        Assertions.assertTrue(contextMenu.isShowing(), "Context menu should be showing after right-click");

        // Click on the system menu bar, in order to try to show the menu
        Util.runAndWait(() -> {
            robot.mouseMove(MENU_BAR_X, MENU_BAR_Y);
            robot.mouseClick(MouseButton.PRIMARY);
        });
        Util.sleep(DELAY);

        Assertions.assertFalse(menu.isShowing(), "System menu should not be showing");
        Assertions.assertFalse(contextMenu.isShowing(), "Context menu should not be showing");
    }

    /**
     * Verifies that the system menu bar processes a mouse click event when there is a
     * non-auto-hide popup window showing in the active stage: the popup remains visible,
     * and the system menu bar shows the menu.
     */
    @Test
    void testSystemMenuBarProcessesClickEvent() {
        Assumptions.assumeTrue(PlatformUtil.isMac(), "System menu bar tests only apply to macOS");

        // set non-auto-hide popup
        Util.runAndWait(() -> contextMenu.setAutoHide(false));

        // Right-click on the label, the context menu shows up
        Util.runAndWait(() -> {
            Bounds bounds = label.localToScreen(label.getBoundsInLocal());
            double x = bounds.getMinX() + bounds.getWidth() / 2;
            double y = bounds.getMinY() + bounds.getHeight() / 2;
            robot.mouseMove((int) x, (int) y);
            robot.mouseClick(MouseButton.SECONDARY);
        });

        Assertions.assertTrue(contextMenu.isShowing(), "Context menu should be showing after right-click");

        // Click on the system menu bar, in order to open the menu
        Util.runAndWait(() -> {
            robot.mouseMove(MENU_BAR_X, MENU_BAR_Y);
            robot.mouseClick(MouseButton.PRIMARY);
        });
        Util.sleep(DELAY);

        Assertions.assertTrue(menu.isShowing(), "System menu should be showing");
        Assertions.assertTrue(contextMenu.isShowing(), "Context menu should be showing after system menu bar click");

        // hide system menu
        Util.runAndWait(() -> menu.hide());
        Util.sleep(DELAY);
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) {
            robot = new Robot();

            // System menu
            MenuItem fileItem1 = new MenuItem("file item 1");
            MenuItem fileItem2 = new MenuItem("file item 2");
            MenuItem fileItem3 = new MenuItem("file item 3");
            menu = new Menu("TestMenu", null, fileItem1, fileItem2, fileItem3);
            MenuBar menuBar = new MenuBar(menu);
            menuBar.setUseSystemMenuBar(true);

            // Context menu
            MenuItem menuItem1 = new MenuItem("item 1");
            MenuItem menuItem2 = new MenuItem("item 2");
            MenuItem menuItem3 = new MenuItem("item 3");
            contextMenu = new ContextMenu(menuItem1, menuItem2, menuItem3);

            label = new Label("System Menu Bar Mouse Click Test");
            label.setContextMenu(contextMenu);

            VBox root = new VBox(menuBar, label);
            root.setAlignment(Pos.CENTER);

            Scene scene = new Scene(root, 300, 200);
            stage.setScene(scene);
            stage.setOnShown(_ -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }

    }
}

