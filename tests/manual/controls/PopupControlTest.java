/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class PopupControlTest extends Application {

    public Parent createContent() {
        VBox root = new VBox(2);
        root.setPrefSize(800, 600);

        // Create a menu bar with 2 menu items each with 2 sub-items
        VBox menuBarPane = new VBox();
        final MenuBar menuBar = new MenuBar();
        Menu m1 = new Menu("one");
        m1.getItems().addAll(new MenuItem("111"), new MenuItem("222"));
        Menu m2 = new Menu("two");
        m2.getItems().addAll(new MenuItem("aaa"), new MenuItem("bbb"));
        Menu menu = new Menu("Menu");
        menu.getItems().addAll(m1, m2);
        menuBar.getMenus().addAll(menu);
        menuBarPane.getChildren().addAll(menuBar);

        // Create button with tooltip (autofix=false)
        Button button1 = new Button("Button 1 w/ Tooltip");
        Tooltip tooltip1 = new Tooltip("1 (autoFix=no)");
        tooltip1.setAutoFix(false);
        button1.setTooltip(tooltip1);

        // Create button with tooltip (autofix=true)
        Button button2 = new Button("Button 2 w/ Tooltip");
        Tooltip tooltip2 = new Tooltip("2 (autoFix=yes)");
        button2.setTooltip(tooltip2);

        // create a context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem1 = new MenuItem("item 1");
        MenuItem menuItem2 = new MenuItem("item 2");
        MenuItem menuItem3 = new MenuItem("item 3");
        contextMenu.getItems().add(menuItem1);
        contextMenu.getItems().add(menuItem2);
        contextMenu.getItems().add(menuItem3);

        // Create button with content menu
        Button button3 = new Button("ContextMenu (right click)");
        button3.setContextMenu(contextMenu);

        // Instructions for running the test
        String message =
            "Setup: This test is intended to be run on a dual-monitor setup on Windows,\n" +
            "with each monitor having a different HiDPI screen scale. For example,\n" +
            "a primary screen with a scale of 1.25 and a secondary screen with a\n" +
            "scale of 1.75. The secondary screen should be aligned to the left of\n" +
            "the primary screen.\n" +
            "\n" +
            "Test instructions:\n" +
            "\n" +
            "1. Drag the stage to the left such that the menu bar and the three buttons\n" +
            "are on the second screen, but the majority of the window is still on\n" +
            "the  primary screen. The window should not rescale to the scale of the\n" +
            "secondary screen.\n" +
            "\n" +
            "2. Click the menu and verify that the menu items and sub-menu items are\n" +
            "drawn below the menu with the same scale as the main window.\n" +
            "\n" +
            "3. Mouse over button 1 and verify that the tooltip is drawn near the\n" +
            "mouse location with the same scale as the main window.\n" +
            "\n" +
            "4. Mouse over button 2 and verify that the tooltip is drawn near the\n" +
            "mouse location, entirely on the secondary screen, with the same scale\n" +
            "as the main window.\n" +
            "\n" +
            "5. Right click on the third button and verify that the context menu is\n" +
            "drawn near the mouse location with the same scale as the main window.\n" +
            "\n" +
            "6. Drag the main window further to the left such that it changes scale\n" +
            "to that of the secondary screen\n" +
            "\n" +
            "7. repeat steps 2-5";
        TextArea instructions = new TextArea(message);
        instructions.setEditable(false);
        VBox.setVgrow(instructions, Priority.ALWAYS);

        root.getChildren().addAll(menuBarPane, button1, button2, button3, instructions);

        return root;
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setScene(new Scene(createContent()));
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
