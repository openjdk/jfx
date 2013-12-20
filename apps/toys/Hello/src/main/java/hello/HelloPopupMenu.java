/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

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
        final Scene scene = new Scene(new Group());
        final ContextMenu popupMenu = new ContextMenu();
        popupMenu.setOnShowing(new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent t) {
                System.out.println("showing");
            }
        });
        popupMenu.setOnShown(new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent t) {
                System.out.println("shown");
            }
        });
        popupMenu.setOnHiding(new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent t) {
                System.out.println("hiding");
            }
        });
        popupMenu.setOnHidden(new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent t) {
                System.out.println("hidden");
            }
        });
        popupMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) {
                System.out.println("on Action: " + t.getTarget());
            }
        });

        MenuItem item1 = new MenuItem("About");
        item1.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println("About");
            }
        });

        MenuItem item2 = new MenuItem("Preferences");
        item2.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println("Preferences");
            }
        });

        MenuItem item3 = new MenuItem("Templates");
        item3.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println("Templates");
            }
        });

        popupMenu.getItems().add(item1);
        popupMenu.getItems().add(item2);
        popupMenu.getItems().add(item3);

        final Button button = new Button("Click me");
        button.setContextMenu(popupMenu);

        Group root = (Group) scene.getRoot();
        root.getChildren().clear();
        root.getChildren().add(button);
        return scene;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
