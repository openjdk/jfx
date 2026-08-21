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

package test.javafx.scene.control;

import com.sun.glass.ui.mac.MacApplicationShim;
import com.sun.javafx.PlatformUtil;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import test.util.Util;

public class CommonSystemMenusTest {
    @BeforeAll
    public static void initFX() throws Exception {
        assumeTrue(PlatformUtil.isMac());

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

    // Two stages use the system menu bar and one stage uses a standard menu
    // bar. For each stage that has a menu bar we track a menu item that is
    // unique to that stage.
    static class StageData {
        Stage       stage;
        MenuBar     menuBar;
        MenuItem    menuItem;
    };

    static StageData systemMenuBarData;
    static StageData additionalSystemMenuBarData;
    static StageData standardMenuBarData;

    // The list of common system menus
    static ObservableList<Menu> commonMenus;
    // A menu item that is always available in the common system menus
    static MenuItem commonMenuItem;

    // A menu item with the same title as one in the default application menu
    static MenuItem defaultApplicationMenuItem;

    private static StageData initStage(String title, boolean useSystemMenuBar) {
        var menuItem = new MenuItem(title + " item");

        var menu = new Menu(title + " menu");
        menu.getItems().add(menuItem);

        var menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(useSystemMenuBar);
        menuBar.getMenus().add(menu);

        var scene = new Scene(menuBar);
        var stage = new Stage();
        stage.setScene(scene);
        stage.show();

        StageData data = new StageData();
        data.stage = stage;
        data.menuBar = menuBar;
        data.menuItem = menuItem;

        return data;
    }

    private static void initTest() {
        Util.runAndWait(() -> {
            systemMenuBarData = initStage("System", true);
            additionalSystemMenuBarData = initStage("Additional", true);
            standardMenuBarData = initStage("Standard", false);

            commonMenuItem = new MenuItem("Common item");
            var menu = new Menu("Common menu");
            menu.getItems().add(commonMenuItem);
            commonMenus = FXCollections.<Menu>observableArrayList();
            commonMenus.add(menu);

            defaultApplicationMenuItem = new MenuItem("Hide Others");
        });
    }

    private boolean menuItemPresent(MenuItem item) {
        String title = item.getText();
        var path = MacApplicationShim.findItemInSystemMenuBar(title);
        assertNotNull(path);
        if (path.length > 0) {
            assertEquals(title, path[path.length - 1]);
        }
        return path.length > 0;
    }

    private boolean systemMenuBarPresent() {
        return menuItemPresent(systemMenuBarData.menuItem);
    }

    private boolean additionalSystemMenuBarPresent() {
        return menuItemPresent(additionalSystemMenuBarData.menuItem);
    }

    // This should never show up in the system menu bar
    private boolean standardMenuBarPresent() {
        return menuItemPresent(standardMenuBarData.menuItem);
    }

    private boolean commonMenuPresent() {
        return menuItemPresent(commonMenuItem);
    }

    private boolean defaultApplicationMenuPresent() {
        return menuItemPresent(defaultApplicationMenuItem);
    }

    private void focusStage(StageData data) {
        Util.runAndWait(() -> {
            data.stage.show();
            data.stage.requestFocus();
        });
    }

    // Test that we can add and remove a menu to the list of menus
    // and toggle it's visibility
    private void testAddRemoveMenu(ObservableList<Menu> menuList) {
        var menuItem = new MenuItem("Transient");
        var transientMenu = new Menu("Transient");
        transientMenu.getItems().add(menuItem);

        assertFalse(menuItemPresent(menuItem), "Transient menu present at start of test");

        Util.runAndWait(() -> menuList.add(transientMenu));
        boolean added = menuItemPresent(menuItem);
        Util.runAndWait(() -> transientMenu.setVisible(false));
        boolean hidden = !menuItemPresent(menuItem);
        Util.runAndWait(() -> transientMenu.setVisible(true));
        boolean restored = menuItemPresent(menuItem);
        Util.runAndWait(() -> menuList.remove(transientMenu));
        boolean removed = !menuItemPresent(menuItem);

        assertTrue(added, "Transient menu not added");
        assertTrue(hidden, "Transient menu not hidden");
        assertTrue(restored, "Transient menu still hidden");
        assertTrue(removed, "Transient menu not removed");
    }

    private void testRemoveCommonMenus(StageData data, ObservableList<Menu> newCommon) {
        Util.runAndWait(() -> MenuBar.setCommonSystemMenus(newCommon));
        boolean menuRemoved = !commonMenuPresent();
        boolean defaultAppAdded = defaultApplicationMenuPresent();
        boolean stageMenuPresent = menuItemPresent(data.menuItem);
        Util.runAndWait(() -> MenuBar.setCommonSystemMenus(commonMenus));
        boolean menuRestored = commonMenuPresent();
        boolean defaultAppRemoved = !defaultApplicationMenuPresent();

        assertTrue(menuRemoved, "Common menus not removed");
        assertTrue(defaultAppAdded, "Default application menu did not appear");
        assertTrue(menuRestored, "Common menus not restored");
        assertTrue(defaultAppRemoved, "Default application menu was not removed");

        if (data.menuBar.isUseSystemMenuBar()) {
            assertTrue(stageMenuPresent, "Stage's menu bar removed with common menus");
        }
    }

    // Test that we can set the common menus to null dynamically. This should
    // bring the default application menu back.
    private void testNullCommonMenus(StageData data) {
        testRemoveCommonMenus(data, null);
    }

    // Test that we can set the common menus to an empty list dynamically.
    // This should bring the default application menu back.
    private void testEmptyCommonMenus(StageData data) {
        testRemoveCommonMenus(data, FXCollections.<Menu>observableArrayList());
    }

    // Verify that we can add and remove an item in one of the menus and
    // toggle its visibility.
    private void testAddRemoveMenuItem(Menu menu) {
        var menuItem = new MenuItem("Temporary");
        assertFalse(menuItemPresent(menuItem));

        Util.runAndWait(() -> menu.getItems().add(menuItem));
        boolean menuItemAdded = menuItemPresent(menuItem);

        boolean menuItemHidden = true;
        boolean menuItemVisible = true;
        Util.runAndWait(() -> menuItem.setVisible(false));
        menuItemHidden = !menuItemPresent(menuItem);
        Util.runAndWait(() -> menuItem.setVisible(true));
        menuItemVisible = menuItemPresent(menuItem);

        Util.runAndWait(() -> menu.getItems().remove(menuItem));
        boolean menuItemRemoved = !menuItemPresent(menuItem);

        assertTrue(menuItemAdded, "Temporary menu item not added");
        assertTrue(menuItemHidden, "Temporary menu item not hidden");
        assertTrue(menuItemVisible, "Temporary menu item still hidden");
        assertTrue(menuItemRemoved, "Temporary menu item not removed");
    }

    @BeforeEach
    public void setupTest() {
        Util.runAndWait(() -> {
            systemMenuBarData.stage.setIconified(false);
            additionalSystemMenuBarData.stage.setIconified(false);
            standardMenuBarData.stage.setIconified(false);
            var currentCommon = MenuBar.getCommonSystemMenus();
            if (currentCommon == null) {
                MenuBar.setCommonSystemMenus(commonMenus);
            } else if (currentCommon.size() == 0) {
                MenuBar.setCommonSystemMenus(commonMenus);
            }
        });
    }

    private void runTestForStage(StageData stageData) {
        focusStage(stageData);
        assertTrue(commonMenuPresent(), "Common menu not present at start");
        assertFalse(defaultApplicationMenuPresent(), "Default application menu present at start");
        testAddRemoveMenu(commonMenus);
        testAddRemoveMenuItem(commonMenus.get(0));
        testNullCommonMenus(stageData);
        testEmptyCommonMenus(stageData);

        if (stageData.menuBar != null && stageData.menuBar.isUseSystemMenuBar()) {
            testAddRemoveMenu(stageData.menuBar.getMenus());
            testAddRemoveMenuItem(stageData.menuBar.getMenus().get(0));
        }
    }

    @Test
    public void commonMenusWithSystemMenuBar() {
        assumeTrue(PlatformUtil.isMac());
        runTestForStage(systemMenuBarData);
        assertTrue(systemMenuBarPresent(), "System menu bar absent for focused window");
        assertFalse(additionalSystemMenuBarPresent(), "System menu bar present for non-focused window");
        assertFalse(standardMenuBarPresent(), "Standard menu bar present in system menu bar");
    }

    @Test
    public void commonMenusWithStandardMenuBar() {
        assumeTrue(PlatformUtil.isMac());
        runTestForStage(standardMenuBarData);
        assertFalse(systemMenuBarPresent(), "System menu bar present for non-focused window");
        assertFalse(additionalSystemMenuBarPresent(), "System menu bar present for non-focused window");
        assertFalse(standardMenuBarPresent(), "Standard menu bar present in system menu bar");
    }

    @Test
    public void switchStagesWithSystemMenuBar() {
        assumeTrue(PlatformUtil.isMac());
        focusStage(systemMenuBarData);
        assertTrue(systemMenuBarPresent(), "System menu bar absent for focused window");
        assertFalse(additionalSystemMenuBarPresent(), "System menu bar present for non-focused window");
        focusStage(additionalSystemMenuBarData);
        assertFalse(systemMenuBarPresent(), "System menu bar present for non-focused window");
        assertTrue(additionalSystemMenuBarPresent(), "System menu bar absent for focused window");
    }

    @Test
    public void allIconifiedTest() {
        focusStage(systemMenuBarData);
        Util.runAndWait(() -> {
            systemMenuBarData.stage.setIconified(true);
            standardMenuBarData.stage.setIconified(true);
            // The menu associated with the last focused window should be
            // active. And iconifying a stage focuses it so this is the
            // expected active menu.
            additionalSystemMenuBarData.stage.setIconified(true);
        });
        assertFalse(systemMenuBarPresent(), "System menu bar present for non-focused window");
        assertTrue(additionalSystemMenuBarPresent(), "System menu bar absent for focused window");
        assertTrue(commonMenuPresent(), "Common menus went away when windows were iconified");
        assertFalse(defaultApplicationMenuPresent(), "Default application menu appeared when windows were iconified");
    }
}
