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
import javafx.geometry.Pos;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloPopupMenu extends Application {

    @Override public void start(Stage stage) {
        stage.setTitle("Hello PopupMenu");
        stage.setWidth(400);
        stage.setHeight(300);
        Scene scene = createScene();
        scene.setFill(Color.WHITE);
        stage.setScene(scene);
        stage.show();
    }

    private Scene createScene() {
        Group group = new Group();
        ContextMenu simpleMenu = new ContextMenu();
        simpleMenu.addEventHandler(Menu.ON_SHOWN, t -> System.out.println("menu shown"));
        simpleMenu.addEventHandler(Menu.ON_HIDDEN, t -> System.out.println("menu hidden"));
        simpleMenu.addEventHandler(ActionEvent.ACTION, t -> System.out.println("action " + t.getTarget()));

        MenuItem item = new MenuItem("About");
        simpleMenu.getItems().add(item);
        item = new MenuItem("Preferences");
        simpleMenu.getItems().add(item);
        item = new MenuItem("Templates");
        simpleMenu.getItems().add(item);

        ContextMenu weekdaysMenu = new ContextMenu();
        Menu menu = new Menu("Weekdays");
        menu.addEventHandler(ActionEvent.ACTION, t -> System.out.println("Weekdays action " + t.getTarget()));
        weekdaysMenu.getItems().add(menu);
        item = new RadioMenuItem("Monday");
        menu.getItems().add(item);
        item = new RadioMenuItem("Tuesday");
        menu.getItems().add(item);
        item = new RadioMenuItem("Wednesday");
        menu.getItems().add(item);
        weekdaysMenu.getItems().add(item);

        ContextMenu complexMenu = new ContextMenu();
        menu = new Menu("Types");
        menu.addEventHandler(ActionEvent.ACTION, t -> System.out.println("Types action " + t.getTarget()));
        complexMenu.getItems().add(menu);
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
        
        VBox vbox = new VBox(20);
        HBox hbox = new HBox(10);
        vbox.setAlignment(Pos.CENTER);
        hbox.setAlignment(Pos.CENTER);
        
        Button simple = new Button("Simple ContextMenu");
        Button weekdays = new Button("Weekdays ContextMenu");
        Button complex = new Button("Complex ContextMenu");
        
        simple.setContextMenu(simpleMenu);
        simple.setOnAction(e -> {
            Bounds b = simple.getBoundsInLocal();
            Point2D pt = simple.localToScreen(b.getMaxX(), b.getMaxY());
            simpleMenu.show(simple, pt.getX(), pt.getY());
        });
        
        weekdays.setOnAction(e -> {
            Bounds b = weekdays.getBoundsInLocal();
            Point2D pt = weekdays.localToScreen(b.getMaxX(), b.getMaxY());
            weekdaysMenu.show(weekdays, pt.getX(), pt.getY());
        });
        
        complex.setOnAction(e -> {
            Bounds b = complex.getBoundsInLocal();
            Point2D pt = complex.localToScreen(b.getMaxX(), b.getMaxY());
            complexMenu.show(complex, pt.getX(), pt.getY());
        });
        
        
        vbox.getChildren().addAll(simple, weekdays, complex);
        hbox.getChildren().addAll(vbox);
        hbox.setLayoutX(20);
        hbox.setLayoutY(20);
        group.getChildren().add(hbox);
        
        return new Scene(group);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
