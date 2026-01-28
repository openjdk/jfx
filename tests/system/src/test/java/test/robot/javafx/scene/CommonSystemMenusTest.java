/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import com.sun.javafx.PlatformUtil;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import test.util.Util;

public class CommonSystemMenusTest {
    @BeforeAll
    public static void initFX() throws Exception {
        CountDownLatch startupLatch = new CountDownLatch(1);

        Util.startup(startupLatch, () -> {
            startupLatch.countDown();
        });

        initTest();
    }

    @AfterAll
    public static void teardownOnce() {
        Util.shutdown();
    }

    // One stage uses the system menu bar, one stage uses a standard menu bar.
    // For each stage we track a node we can use to focus the stage and a
    // menu item that is unique to that stage.
    static class StageData {
        Stage       stage;
        Node        focusItem;
        MenuBar     menuBar;
        MenuItem    menuItem;
        KeyCode     acceleratorCode;
    };

    static StageData systemMenuBarData;
    static StageData standardMenuBarData;

    // The list of common system menus
    static ObservableList<Menu> commonMenus;
    // A menu item that is always available in the common system menus
    static MenuItem commonMenuItem;

    // Accelerator codes used to test the presence of menu items and their
    static final KeyCode SYSTEM_MENU_ACCELERATOR = KeyCode.G;
    static final KeyCode STANDARD_MENU_ACCELERATOR = KeyCode.B;
    static final KeyCode COMMON_MENU_ACCELERATOR = KeyCode.L;
    static final KeyCode TRANSIENT_MENU_ACCELERATOR = KeyCode.J;
    static final KeyCode TEMPORARY_MENU_ITEM_ACCELERATOR = KeyCode.K;

    private static StageData initStage(boolean useSystemMenuBar, KeyCode stageAccelCode) {
        var menuItem = new MenuItem("Stage Item");
        menuItem.setAccelerator(new KeyCodeCombination(stageAccelCode, KeyCombination.SHORTCUT_DOWN));

        var menu = new Menu("Stage Menu");
        menu.getItems().add(menuItem);

        var menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(useSystemMenuBar);
        menuBar.getMenus().add(menu);

        var button = new Button("Focus Button");
        var vbox = new VBox(menuBar, button);
        var scene = new Scene(vbox);
        var stage = new Stage();
        stage.setScene(scene);
        stage.show();

        StageData data = new StageData();
        data.stage = stage;
        data.focusItem = button;
        data.menuBar = menuBar;
        data.menuItem = menuItem;
        data.acceleratorCode = stageAccelCode;

        return data;
    }

    private static void initTest() {
        Util.runAndWait(() -> {
            systemMenuBarData = initStage(true, SYSTEM_MENU_ACCELERATOR);
            standardMenuBarData = initStage(false, STANDARD_MENU_ACCELERATOR);

            commonMenuItem = new MenuItem("Common Item");
            commonMenuItem.setAccelerator(new KeyCodeCombination(COMMON_MENU_ACCELERATOR, KeyCombination.SHORTCUT_DOWN));

            var menu = new Menu("Common Menu");
            menu.getItems().add(commonMenuItem);

            commonMenus = FXCollections.<Menu>observableArrayList();
            commonMenus.add(menu);
            MenuBar.setCommonSystemMenus(commonMenus);
        });
    }

    // We test the presence or absence of a menu by attempting to trigger a
    // menu item in that menu via its accelerator.
    private boolean sendAccelerator(MenuItem menuItem, KeyCode code) {
        CountDownLatch acceleratorLatch = new CountDownLatch(1);
        AtomicBoolean acceleratorFired = new AtomicBoolean(false);

        Util.runAndWait(() -> {
            menuItem.setOnAction(a -> {
                acceleratorFired.set(true);
                acceleratorLatch.countDown();
            });

            var robot = new Robot();
            robot.keyPress(KeyCode.COMMAND);
            robot.keyPress(code);
            robot.keyRelease(code);
            robot.keyRelease(KeyCode.COMMAND);
        });

        try {
            acceleratorLatch.await(50, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
        }

        Util.runAndWait(() -> {
            menuItem.setOnAction(null);
        });

        return acceleratorFired.get();
    }

    private boolean systemMenuBarPresent() {
        return sendAccelerator(systemMenuBarData.menuItem, SYSTEM_MENU_ACCELERATOR);
    }

    private boolean standardMenuBarPresent() {
        return sendAccelerator(standardMenuBarData.menuItem, STANDARD_MENU_ACCELERATOR);
    }

    private boolean commonMenuPresent() {
        return sendAccelerator(commonMenuItem, COMMON_MENU_ACCELERATOR);
    }

    private boolean transientMenuPresent(MenuItem menuItem) {
        return sendAccelerator(menuItem, TRANSIENT_MENU_ACCELERATOR);
    }

    private boolean temporaryMenuItemPresent(MenuItem menuItem) {
        return sendAccelerator(menuItem, TEMPORARY_MENU_ITEM_ACCELERATOR);
    }

    private void focusStage(StageData data) {
        Util.runAndWait(() -> {
            data.focusItem.requestFocus();
            data.stage.requestFocus();
        });
    }

    // Test that we can add and remove a menu to the list of menus
    // and toggle it's visibility
    private void testAddRemoveMenu(ObservableList<Menu> menuList) {
        var menuItem = new MenuItem("Transient");
        menuItem.setAccelerator(new KeyCodeCombination(TRANSIENT_MENU_ACCELERATOR, KeyCombination.SHORTCUT_DOWN));
        var transientMenu = new Menu("Transient");
        transientMenu.getItems().add(menuItem);

        assertFalse(transientMenuPresent(menuItem), "Transient menu present at start of test");

        Util.runAndWait(() -> menuList.add(transientMenu));
        boolean added = transientMenuPresent(menuItem);
        Util.runAndWait(() -> transientMenu.setVisible(false));
        boolean hidden = !transientMenuPresent(menuItem);
        Util.runAndWait(() -> transientMenu.setVisible(true));
        boolean restored = transientMenuPresent(menuItem);
        Util.runAndWait(() -> menuList.remove(transientMenu));
        boolean removed = !transientMenuPresent(menuItem);

        assertTrue(added, "Transient menu not added");
        assertTrue(hidden, "Transient menu not hidden");
        assertTrue(restored, "Transient menu still hidden");
        assertTrue(removed, "Transient menu not removed");
    }

    // Test that we can remove and restore the entire list of common menus.
    // Even with the common menus removed the stage-specific menu items
    // should still work.
    private void testRemoveAllCommonMenus(StageData data) {
        Util.runAndWait(() -> MenuBar.setCommonSystemMenus(null));
        boolean menuRemoved = !commonMenuPresent();
        boolean stageMenuPresent = sendAccelerator(data.menuItem, data.acceleratorCode);
        Util.runAndWait(() -> MenuBar.setCommonSystemMenus(commonMenus));
        boolean menuRestored = commonMenuPresent();

        assertTrue(menuRemoved, "Common menus not removed");
        assertTrue(stageMenuPresent, "Stage's menu bar removed with common menus");
        assertTrue(menuRestored, "Common menus not restored");
    }

    // Verify that we can add and remove an item in one of the menus and
    // toggle its visibility.
    //
    // Menu item visibility behaves differently in the system menus. Normally
    // hiding a menu item does not disable its accelerator but if it's in a
    // system menu it might due to platform limitations. So we only test for
    // visibility if we know the menu item is not in a system menu.
    private void testAddRemoveMenuItem(Menu menu, boolean testVisibility) {
        var menuItem = new MenuItem("Temporary");
        menuItem.setAccelerator(new KeyCodeCombination(TEMPORARY_MENU_ITEM_ACCELERATOR, KeyCombination.SHORTCUT_DOWN));
        assertFalse(temporaryMenuItemPresent(menuItem));

        Util.runAndWait(() -> menu.getItems().add(menuItem));
        boolean menuItemAdded = temporaryMenuItemPresent(menuItem);

        boolean menuItemHidden = true;
        boolean menuItemVisible = true;
        if (testVisibility) {
            Util.runAndWait(() -> menuItem.setVisible(false));
            menuItemHidden = !temporaryMenuItemPresent(menuItem);
            Util.runAndWait(() -> menuItem.setVisible(true));
            menuItemVisible = temporaryMenuItemPresent(menuItem);
        }

        Util.runAndWait(() -> menu.getItems().remove(menuItem));
        boolean menuItemRemoved = !temporaryMenuItemPresent(menuItem);

        assertTrue(menuItemAdded, "Temporary menu item not added");
        assertTrue(menuItemHidden, "Temporary menu item not hidden");
        assertTrue(menuItemVisible, "Temporary menu item still hidden");
        assertTrue(menuItemRemoved, "Temporary menu item not removed");
    }

    // Run all the tests for a stage.
    private void runTest(StageData stageData) {
        focusStage(stageData);
        assertTrue(commonMenuPresent(), "Common menu not present at start");
        testAddRemoveMenu(commonMenus);
        testAddRemoveMenu(stageData.menuBar.getMenus());
        testRemoveAllCommonMenus(stageData);
        testAddRemoveMenuItem(commonMenus.get(0), true);
        testAddRemoveMenuItem(stageData.menuBar.getMenus().get(0), stageData.menuBar.isUseSystemMenuBar());
    }

    @Test
    public void commonMenusWithSystemMenuBar() {
        assumeTrue(PlatformUtil.isMac());
        runTest(systemMenuBarData);
        assertTrue(systemMenuBarPresent(), "System menu bar not present");
        assertFalse(standardMenuBarPresent(), "Standard menu bar present");
    }

    @Test
    public void commonMenusWithStandardMenuBar() {
        assumeTrue(PlatformUtil.isMac());
        runTest(standardMenuBarData);
        assertFalse(systemMenuBarPresent(), "System menu bar present");
        assertTrue(standardMenuBarPresent(), "Standard menu bar not present");
    }
}
