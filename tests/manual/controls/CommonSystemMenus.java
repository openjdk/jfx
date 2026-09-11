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

import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.application.ApplicationServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CommonSystemMenus extends Application {
    private final TextArea messageArea = new TextArea("");

    private final MenuBar commonMenuBar = new MenuBar();
    private final MenuBar testMenuBar = new MenuBar();

    private final MenuItem toggleSystemMenus = new MenuItem();
    private final MenuItem toggleCommonMenus = new MenuItem();

    private Scene scene = null;

    public static void main(String[] args) {
        Application.launch(CommonSystemMenus.class, args);
    }

    private boolean commonSystemMenusVisible() {
        var current = MenuBar.getCommonSystemMenus();
        if (current == null) return false;
        if (current.size() == 0) return false;
        for (var menu : current) {
            if (menu.isVisible()) return true;
        }
        return false;
    }

    private void updateUI() {
        if (testMenuBar.isUseSystemMenuBar()) {
            toggleSystemMenus.setText("Turn off system menu bar");
        } else {
            toggleSystemMenus.setText("Turn on system menu bar");
        }

        if (!commonSystemMenusVisible()) {
            toggleCommonMenus.setText("Set common system menus");
        } else {
            toggleCommonMenus.setText("Remove common system menus");
        }

        if (testMenuBar.isUseSystemMenuBar()) {
            messageArea.appendText("System menu bar is ON, ");
        } else {
            messageArea.appendText("System menu bar is OFF, ");
        }

        if (!commonSystemMenusVisible()) {
            messageArea.appendText("common system menus are OFF\n");
        } else {
            messageArea.appendText("common system menus are ON\n");
        }
    }

    private MenuItem addItem(Menu menu, String title) {
        var item = new MenuItem(title);
        menu.getItems().add(item);
        return item;
    }

    private Menu buildTestMenu(boolean common, String title, MenuBar menuBar, boolean allowRemoval) {
        if (common) {
            title = "(" + title + ")";
        }

        var testMenu = new Menu(title);

        // Necessary to ensure the individual menu items get validated.
        testMenu.setOnMenuValidation(f -> {
        });

        var addBefore = addItem(testMenu, "Add menu <");
        addBefore.setOnAction(f -> {
            var newMenu = buildTestMenu(common, "Added left", menuBar, true);
            var menus = menuBar.getMenus();
            menus.add(menus.indexOf(testMenu), newMenu);
        });

        var addAfter = addItem(testMenu, "Add menu >");
        addAfter.setOnAction(f -> {
            var newMenu = buildTestMenu(common, "Added right", menuBar, true);
            var menus = menuBar.getMenus();
            menus.add(menus.indexOf(testMenu) + 1, newMenu);
        });

        if (allowRemoval) {
            var removeThisMenu = new MenuItem("Remove this menu");
            removeThisMenu.setOnAction(f -> {
                menuBar.getMenus().remove(testMenu);
            });
            testMenu.getItems().addAll(removeThisMenu);
        }

        var hideRight = addItem(testMenu, "Hide/Show >");
        hideRight.setOnMenuValidation(f -> {
            var menus = menuBar.getMenus();
            var index = menus.indexOf(testMenu);
            if (index < menus.size() - 1) {
                var nextDoor = menus.get(index + 1);
                if (nextDoor.isVisible()) {
                    hideRight.setText("Hide >");
                } else {
                    hideRight.setText("Show >");
                }
                hideRight.setDisable(false);
            } else {
                hideRight.setText("Hide/Show >");
                hideRight.setDisable(true);
            }
        });
        hideRight.setOnAction(f -> {
            var menus = menuBar.getMenus();
            var index = menus.indexOf(testMenu);
            if (index < menus.size() - 1) {
                var nextDoor = menus.get(index + 1);
                nextDoor.setVisible(!nextDoor.isVisible());
            }
        });

        var hideLeft = addItem(testMenu, "Hide/Show <");
        hideLeft.setOnMenuValidation(f -> {
            var menus = menuBar.getMenus();
            var index = menus.indexOf(testMenu);
            if (index > 0) {
                var nextDoor = menus.get(index - 1);
                if (nextDoor.isVisible()) {
                    hideLeft.setText("Hide <");
                } else {
                    hideLeft.setText("Show <");
                }
                hideLeft.setDisable(false);
            } else {
                hideLeft.setText("Hide/Show <");
                hideLeft.setDisable(true);
            }
        });
        hideLeft.setOnAction(f -> {
            var menus = menuBar.getMenus();
            var index = menus.indexOf(testMenu);
            if (index > 0) {
                var nextDoor = menus.get(index - 1);
                nextDoor.setVisible(!nextDoor.isVisible());
            }
        });

        return testMenu;
    }

    private Menu buildInitialTestMenu(MenuBar menuBar) {
        var menu = buildTestMenu(false, "Test", menuBar, false);

        toggleSystemMenus.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        toggleSystemMenus.setOnAction(e -> {
            if (menuBar.isUseSystemMenuBar()) {
                menuBar.setUseSystemMenuBar(false);
                updateUI();
            } else {
                menuBar.setUseSystemMenuBar(true);
                updateUI();
            }
        });

        toggleCommonMenus.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN));
        toggleCommonMenus.setOnAction(e -> {
            if (commonSystemMenusVisible()) {
                MenuBar.setCommonSystemMenus(null);
                updateUI();
            } else {
                MenuBar.setCommonSystemMenus(commonMenuBar.getMenus());
                updateUI();
            }
        });

        var setCommonSystemMenusEmpty = new MenuItem("Set Common System Menus to empty list");
        setCommonSystemMenusEmpty.setOnAction(e -> {
            MenuBar.setCommonSystemMenus(FXCollections.<Menu>observableArrayList());
            updateUI();
        });

        var setCommonSystemMenusHidden = new MenuItem("Set Common System Menus to hidden menus");
        setCommonSystemMenusHidden.setOnAction(e -> {
            var menuList = FXCollections.<Menu>observableArrayList();
            var hiddenMenu = new Menu("Hidden 1");
            hiddenMenu.setVisible(false);
            menuList.add(hiddenMenu);
            hiddenMenu = new Menu("Hidden 2");
            hiddenMenu.setVisible(false);
            menuList.add(hiddenMenu);
            MenuBar.setCommonSystemMenus(menuList);
            updateUI();
        });

        menu.getItems().add(0, new SeparatorMenuItem());
        menu.getItems().add(0, setCommonSystemMenusEmpty);
        menu.getItems().add(0, setCommonSystemMenusHidden);
        menu.getItems().add(0, new SeparatorMenuItem());
        menu.getItems().add(0, toggleCommonMenus);
        menu.getItems().add(0, toggleSystemMenus);

        return menu;
    }

    private Menu buildApplicationMenu() {
        var applicationMenu = buildTestMenu(true, "App", commonMenuBar, false);

        applicationMenu.getItems().add(new SeparatorMenuItem());

        var item = addItem(applicationMenu, "Custom application menu item");
        item.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN));
        item.setOnAction(e -> {
            messageArea.appendText("Custom menu item\n");
        });

        applicationMenu.getItems().add(new SeparatorMenuItem());

        item = addItem(applicationMenu, "Hide CommonSystemMenus");
        item.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN));
        item.setOnAction(e -> {
            ApplicationServices.hideApplication();
        });

        item = addItem(applicationMenu, "Hide Others");
        item.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        item.setOnAction(e -> {
            ApplicationServices.hideOtherApplications();
        });

        item = addItem(applicationMenu, "Show All");
        item.setOnAction(e -> {
            ApplicationServices.showAllApplications();
        });

        applicationMenu.getItems().add(new SeparatorMenuItem());

        item = addItem(applicationMenu, "Quit CommonSystemMenus");
        item.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        item.setOnAction(e -> {
            Platform.exit();
        });

        return applicationMenu;
    }

    @Override
    public void start(Stage stage) {
        messageArea.setEditable(false);
        messageArea.appendText("Use items in the Test menu to test the system menu bar\n");

        commonMenuBar.getMenus().add(buildApplicationMenu());
        testMenuBar.getMenus().add(buildInitialTestMenu(testMenuBar));
        testMenuBar.setUseSystemMenuBar(true);
        MenuBar.setCommonSystemMenus(commonMenuBar.getMenus());

        updateUI();

        var box = new VBox(testMenuBar, messageArea);
        box.setVgrow(messageArea, Priority.ALWAYS);
        scene = new Scene(box, 640, 640);

        stage.setScene(scene);
        stage.setTitle("Common Menu Test");
        stage.show();
    }
}
