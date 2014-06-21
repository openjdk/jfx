/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloPopupMenu extends Application {

    @Override public void start(Stage stage) {
        stage.setTitle("Hello PopupMenu");
        stage.setWidth(500);
        stage.setHeight(500);
        Scene scene = createScene();
        scene.setFill(Color.WHITE);
        stage.setScene(scene);
        stage.show();
    }

    private Scene createScene() {
        ContextMenu popupMenu = new ContextMenu();
        popupMenu.addEventHandler(Menu.ON_SHOWN, t -> System.out.println("menu shown"));
        popupMenu.addEventHandler(Menu.ON_HIDDEN, t -> System.out.println("menu hidden"));
        popupMenu.addEventHandler(ActionEvent.ACTION, t -> System.out.println("action " + t.getTarget()));

        MenuItem item = new MenuItem("About");
        popupMenu.getItems().add(item);
        item = new MenuItem("Preferences");
        popupMenu.getItems().add(item);
        item = new MenuItem("Templates");
        popupMenu.getItems().add(item);

        Menu menu = new Menu("Weekdays");
        menu.addEventHandler(ActionEvent.ACTION, t -> System.out.println("Weekdays action " + t.getTarget()));
        popupMenu.getItems().add(menu);
        item = new RadioMenuItem("Monday");
        menu.getItems().add(item);
        item = new RadioMenuItem("Tuesday");
        menu.getItems().add(item);
        item = new RadioMenuItem("Wednesday");
        menu.getItems().add(item);
        item = new SeparatorMenuItem();
        popupMenu.getItems().add(item);

        menu = new Menu("Types");
        menu.addEventHandler(ActionEvent.ACTION, t -> System.out.println("Types action " + t.getTarget()));
        popupMenu.getItems().add(menu);
        item = new MenuItem("Push");
        menu.getItems().add(item);
        item = new RadioMenuItem("Radio1");
        menu.getItems().add(item);
        item = new RadioMenuItem("Radio2");
        menu.getItems().add(item);
        item = new MenuItem("Mnem_onic");
        menu.getItems().add(item);
        item = new MenuItem("Disabled");
        item.setDisable(true);
        menu.getItems().add(item);
        item = new SeparatorMenuItem();
        menu.getItems().add(item);
        item = new CheckMenuItem("Check1");
        menu.getItems().add(item);
        item = new CheckMenuItem("Check2");
        ((CheckMenuItem)item).setSelected(true);
        menu.getItems().add(item);
        item = new CheckMenuItem("Shortcut1");
        item.setAccelerator(KeyCombination.keyCombination("Shortcut + s"));
        menu.getItems().add(item);
        item = new CheckMenuItem("Shortcut2");
        item.setAccelerator(KeyCombination.keyCombination("Shortcut + Shift + W"));
        menu.getItems().add(item);
        item = new CheckMenuItem("Shortcut3");
        item.setAccelerator(KeyCombination.keyCombination("Shortcut + Shift + Ctrl + R"));
        menu.getItems().add(item);
        item = new CheckMenuItem("Shortcut4");
        item.setAccelerator(KeyCombination.keyCombination("F1"));
        menu.getItems().add(item);
        item = new CheckMenuItem("Shortcut5");
        item.setAccelerator(KeyCombination.keyCombination("Shortcut + 1"));
        menu.getItems().add(item);
        item = new CheckMenuItem("Shortcut6");
        item.setAccelerator(KeyCombination.keyCombination("Shortcut + UP"));
        menu.getItems().add(item);
        item = new CheckMenuItem("Shortcut7");
        item.setAccelerator(KeyCombination.keyCombination("Shortcut + DOWN"));
        menu.getItems().add(item);
        
        Button button = new Button("Click me");
        button.setContextMenu(popupMenu);
        button.setOnAction(e -> {
            Bounds b = button.getBoundsInLocal();
            Point2D pt = button.localToScreen(b.getMaxX(), b.getMaxY());
            popupMenu.show(button, pt.getX(), pt.getY());
        });
        return new Scene(new Group(button));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
