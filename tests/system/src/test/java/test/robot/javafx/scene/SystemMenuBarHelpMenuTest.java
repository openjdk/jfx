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
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests that the Help menu in the system menu bar behaves correctly when a dialog
 * is shown while the menu was opened.
 */
public class SystemMenuBarHelpMenuTest {

    // Changing this to anything different than "Help" will avoid the insertion of the Spotlight search field
    private static final String HELP_LITERAL = "Help";

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static Menu menu;
    private static Stage stage;

    private static final int DELAY = 100;

    // Estimated x, y offset for the Help menu in the macOS system menu bar (after the Apple menu and java menu).
    private static final int MENU_BAR_X = 120;
    private static final int MENU_BAR_Y = 10;

    // Estimated y offset for the Help -> About menu item, after the Spotlight search field that macOS inserts to
    // this menu.
    private static final int MENU_ABOUT_Y = 80;

    private static Robot robot;
    private static final AtomicBoolean aboutItemFired = new AtomicBoolean(false);
    private static final CountDownLatch aboutItemLatch = new CountDownLatch(1);

    @BeforeAll
    static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    static void exit() {
        Util.shutdown();
    }

    /**
     * Verifies that after opening the Help menu includes the Spotlight search field, which is 
     * included seamlessly by the system.
     */
    @Test
    void testHelpMenuHasSpotlight() {
        Assumptions.assumeTrue(PlatformUtil.isMac(), "System menu bar tests only apply to macOS");

        // Step 1: open the menu via the Robot
        Util.runAndWait(() -> {
            robot.mouseMove(MENU_BAR_X, MENU_BAR_Y);
            robot.mouseClick(MouseButton.PRIMARY);
        });
        Util.sleep(DELAY);

        // Verify the menu is showing
        Assertions.assertTrue(menu.isShowing(),
                "Menu should be showing after robot click");

        // Step 2: click on About
        Util.runAndWait(() -> {
            robot.mouseMove(MENU_BAR_X, MENU_ABOUT_Y);
            robot.mouseClick(MouseButton.PRIMARY);
        });
        Util.waitForLatch(aboutItemLatch, 5, "About menu item action should fire");

        Assertions.assertTrue(aboutItemFired.get(),
                "Menu item action should fire when clicking on About menu item");

        // Clean up: close the menu
        Util.runAndWait(() -> robot.keyType(KeyCode.ESCAPE));
        Util.sleep(DELAY);
    }

    /**
     * Verifies that after opening the system menu and then showing a modal dialog
     * the menu can still be opened again after the dialog is closed.
     */
    @Test
    void testMenuCanBeReopenedAfterDialogClosed() {
        Assumptions.assumeTrue(PlatformUtil.isMac(), "System menu bar tests only apply to macOS");

        // Step 1: open the menu via the Robot
        Util.runAndWait(() -> {
            robot.mouseMove(MENU_BAR_X, MENU_BAR_Y);
            robot.mouseClick(MouseButton.PRIMARY);
        });
        Util.sleep(DELAY);

        // Verify the menu is showing
        Assertions.assertTrue(menu.isShowing(), "Menu should be showing after robot click");

        // Step 2: open a modal dialog while the menu is showing
        CountDownLatch dialogShownLatch = new CountDownLatch(1);
        CountDownLatch dialogHiddenLatch = new CountDownLatch(1);
        List<Dialog<ButtonType>> dialogHolder = new ArrayList<>();
        Util.runAndWait(() -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialogHolder.add(dialog);
            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK);
            dialog.initOwner(stage);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.getDialogPane().setContent(new Label("Dialog"));
            dialog.resultProperty().subscribe(result -> {
                    if (result == ButtonType.OK) {
                        dialog.close();
                    }
                });
            dialog.setOnHidden(_ -> dialogHiddenLatch.countDown());
            dialog.setOnShown(_ -> dialogShownLatch.countDown());
            dialog.show();
        });
        Util.waitForLatch(dialogShownLatch, 5, "Dialog should be shown");

        // Verify the menu remains opened. The Spotlight search field is also visible
        // (thought this can only be verified visually)
        Assertions.assertTrue(menu.isShowing(), "Menu should remain opened");

        // Step 3: close the menu
        Util.runAndWait(() -> {
            // Move mouse to the dialog and click on it. Since the menu is still showing,
            // the mouse event should just hide the menu
            Node pane = dialogHolder.getFirst().getDialogPane();
            Point2D origin = pane.localToScreen(pane.getBoundsInLocal().getMinX(),
                    pane.getBoundsInLocal().getMinY());
            robot.mouseMove(origin.getX() + 5 , origin.getY() + 5);
            // hide menu
            robot.mouseClick(MouseButton.PRIMARY);
        });
        Util.sleep(2 * DELAY); // wait for menu hiding animation to complete

        // Verify the menu is closed.
        Assertions.assertFalse(menu.isShowing(), "Menu should be closed");

        // Step 4: close the dialog
        Util.runAndWait(() -> {
            // robot move mouse to OK button and press
            Node button = dialogHolder.getFirst().getDialogPane().lookupButton(ButtonType.OK);
            Point2D center = button.localToScreen(button.getBoundsInLocal().getCenterX(),
                    button.getBoundsInLocal().getCenterY());
            robot.mouseMove(center.getX(), center.getY());
            // hide dialog
            robot.mouseClick(MouseButton.PRIMARY);
        });
        Util.waitForLatch(dialogHiddenLatch, 5, "Dialog should be hidden");

        // Step 5: click the menu to open it again
        Util.runAndWait(() -> {
            robot.mouseMove(MENU_BAR_X, MENU_BAR_Y);
            robot.mouseClick(MouseButton.PRIMARY);
        });
        Util.sleep(DELAY);

        // Verify the menu opens again
        Assertions.assertTrue(menu.isShowing(), "Menu should open after dialog is closed");

        // Clean up: close the menu
        Util.runAndWait(() -> robot.keyType(KeyCode.ESCAPE));
        Util.sleep(DELAY);
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) {
            robot = new Robot();
            SystemMenuBarHelpMenuTest.stage = stage;

            MenuItem aboutItem = new MenuItem("About");
            aboutItem.setOnAction(_ -> {
                aboutItemFired.set(true);
                aboutItemLatch.countDown();
            });

            menu = new Menu(HELP_LITERAL, null, aboutItem);
            MenuBar menuBar = new MenuBar(menu);
            menuBar.setUseSystemMenuBar(true);

            Label label = new Label("System Menu Bar Help Menu Test");
            VBox root = new VBox(menuBar, label);
            root.setAlignment(Pos.CENTER);

            Scene scene = new Scene(root, 300, 200);
            stage.setScene(scene);
            stage.setOnShown(_ -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }

    }
}

