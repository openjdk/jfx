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

import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.application.ApplicationServices;
import javafx.application.Platform;
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

public class DefaultAppMenu extends Application {
    private final TextArea messageArea = new TextArea("");
    private final MenuBar menuBar = new MenuBar();
    private final Menu applicationMenu = new Menu("Ignored");
    private final Menu testMenu = new Menu("Test");
    private final MenuItem toggleSystemMenus = new MenuItem();
    private final MenuItem toggleDefaultMenus = new MenuItem();

    private Scene scene = null;

    public static void main(String[] args) {
        Application.launch(DefaultAppMenu.class, args);
    }

    private void updateUI() {
        if (menuBar.isUseSystemMenuBar()) {
            toggleSystemMenus.setText("Turn off system menu bar");
        } else {
            toggleSystemMenus.setText("Turn on system menu bar");
        }

        if (menuBar.isUseDefaultMenus()) {
            toggleDefaultMenus.setText("Hide default menus");
        } else {
            toggleDefaultMenus.setText("Show default menus");
        }

        if (menuBar.isUseSystemMenuBar()) {
            messageArea.appendText("System menu bar is ON and ");
        } else {
            messageArea.appendText("System menu bar is OFF and ");
        }

        if (menuBar.isUseDefaultMenus()) {
            messageArea.appendText("default menus are ON\n");
        } else {
            messageArea.appendText("default menus are OFF\n");
        }

        if (!menuBar.isUseSystemMenuBar() || menuBar.isUseDefaultMenus()) {
            messageArea.appendText("Using default application menu\n");
            menuBar.getMenus().remove(applicationMenu);
        } else {
            messageArea.appendText("Using custom application menu\n");
            if (!menuBar.getMenus().contains(applicationMenu)) {
                var toPrepend = new ArrayList<Menu>();
                toPrepend.add(applicationMenu);
                menuBar.getMenus().addAll(0, toPrepend);
            }
        }
    }

    private MenuItem addItem(Menu menu, String title) {
        var item = new MenuItem(title);
        item.setOnAction(e -> {
            messageArea.appendText(title + "\n");
        });
        menu.getItems().add(item);
        return item;
    }

    private void buildApplicationMenu() {
        var item = addItem(applicationMenu, "Custom menu item");
        item.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN));
        item.setOnAction(e -> {
            messageArea.appendText("Custom menu item\n");
        });

        applicationMenu.getItems().add(new SeparatorMenuItem());

        item = addItem(applicationMenu, "Hide DefaultAppMenu");
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

        item = addItem(applicationMenu, "Quit DefaultAppMenu");
        item.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        item.setOnAction(e -> {
            Platform.exit();
        });
    }

    private Menu buildTestMenu() {
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
        testMenu.getItems().add(toggleSystemMenus);

        toggleDefaultMenus.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN));
        toggleDefaultMenus.setOnAction(e -> {
            if (menuBar.isUseDefaultMenus()) {
                menuBar.setUseDefaultMenus(false);
                updateUI();
            } else {
                menuBar.setUseDefaultMenus(true);
                updateUI();
            }
        });
        testMenu.getItems().add(toggleDefaultMenus);

        testMenu.getItems().add(new SeparatorMenuItem());

        addItem(testMenu, "Test item one");
        addItem(testMenu, "Test item two");
        addItem(testMenu, "Test item three");
        addItem(testMenu, "Test item four");

        return testMenu;
    }

    @Override
    public void start(Stage stage) {
        messageArea.setEditable(false);
        messageArea.appendText("Use items in the Test menu to test the system menu bar\n");

        buildApplicationMenu();
        buildTestMenu();

        menuBar.getMenus().add(testMenu);
        menuBar.setUseSystemMenuBar(true);
        updateUI();

        var box = new VBox(menuBar, messageArea);
        box.setVgrow(messageArea, Priority.ALWAYS);
        scene = new Scene(box, 640, 640);

        stage.setScene(scene);
        stage.setTitle("Menu Key Test");
        stage.show();
    }
}
