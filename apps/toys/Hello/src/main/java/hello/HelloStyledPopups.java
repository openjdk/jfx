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
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


/*
 * 
 */
public class HelloStyledPopups extends Application {

    @Override public void start(Stage stage) {

        stage.setTitle("HelloStyledPopups");

        BorderPane root = new BorderPane();

        Label lbl = new Label("Controls on the left have styled popups, those on the right do not.");
        root.setTop(lbl);
        BorderPane.setMargin(lbl, new Insets(10, 0, 10, 0));
        BorderPane.setAlignment(lbl, Pos.CENTER);

        VBox center = new VBox(5d);
        root.setCenter(center);
        BorderPane.setMargin(center, new Insets(0, 10, 0, 10));

        center.getChildren().add(new Separator());

        Menu menu1 = new Menu("Yellow");
        menu1.setId("menu1");
        menu1.getItems().addAll(new MenuItem("One.1"), new MenuItem("One.2"));

        Menu menu2 = new Menu("Theme");
        menu2.getItems().addAll(new MenuItem("Two.1"), new MenuItem("Two.2"));

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(menu1, menu2);

        center.getChildren().add(menuBar);

        HBox hBox = new HBox(5);
        center.getChildren().add(hBox);

        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("One", "Two", "Three is pretty long"));
        comboBox.setId("cbox");
        hBox.getChildren().add(comboBox);

        comboBox = new ComboBox<>(FXCollections.observableArrayList("One", "Two", "Three"));
        hBox.getChildren().add(comboBox);

        hBox = new HBox(5);
        center.getChildren().add(hBox);

        Button button = new Button("Hover for yellow tooltip");
        button.setId("yellow-tooltip");
        Tooltip tooltip = new Tooltip("The background-color should be yellow");
        button.setTooltip(tooltip);
        hBox.getChildren().add(button);

        button = new Button("Hover for normal tooltip");
        tooltip = new Tooltip("The background-color should be the theme default");
        button.setTooltip(tooltip);
        hBox.getChildren().add(button);

        hBox = new HBox(5);
        center.getChildren().add(hBox);

        MenuButton menuButton = new MenuButton("Yellow popup");
        menuButton.setId("mbutton");
        menuButton.getItems().addAll(new MenuItem("One"), new MenuItem("Two"), new MenuItem("Three"));
        hBox.getChildren().add(menuButton);

        menuButton = new MenuButton("Theme popup");
        menuButton.getItems().addAll(new MenuItem("One"), new MenuItem("Two"), new MenuItem("Three"));
        hBox.getChildren().add(menuButton);

        Scene scene = new Scene(root, 500, 700);
        scene.getStylesheets().add("hello/hello.css");
        stage.setScene(scene);
        stage.show();

    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(HelloStyledPopups.class, args);
    }

}
