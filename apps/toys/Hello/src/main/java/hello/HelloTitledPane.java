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
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class HelloTitledPane extends Application {
    
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("TitledPane");
        Scene scene = new Scene(new Group(), 1000, 250);
        scene.setFill(Color.GHOSTWHITE);
        
        // --- Simple grid test
        TitledPane gridTitlePane = new TitledPane();
        GridPane grid = new GridPane();
        grid.setVgap(4);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.add(new Label("First Name: "), 0, 0);
        grid.add(new TextField(), 1, 0);
        grid.add(new Label("Last Name: "), 0, 1);
        grid.add(new TextField(), 1, 1);
        grid.add(new Label("Email: "), 0, 2);
        grid.add(new TextField(), 1, 2);
        gridTitlePane.setText("Hello World!");
        gridTitlePane.setContent(grid);
        
        // --- Label test
        TitledPane normalText = new TitledPane();
        Label lbl = new Label("This is a collapsible TitledPane\nthat allows for text to be wrapped.\n\nIt should be the perfect height to fit all text provided.\n\nIs it?");
        normalText.setText("Hello World!");
        normalText.setFont(Font.font(20));
        normalText.setContent(lbl);
        
        // --- Big button test
        TitledPane normal = new TitledPane();
        Button bn = new Button("Button");
        bn.setPrefSize(75, 50);
        StackPane pane = new StackPane(bn);
        pane.setPadding(new Insets(5));
        normal.setText("Hello World!");
        normal.setFont(Font.font(5));
        normal.setContent(pane);

        TitledPane unanimated = new TitledPane();
        unanimated.setAnimated(false);
        unanimated.setText("Not Animated");
        Button bs = new Button("Button");
        bs.setPrefSize(75, 50);
        unanimated.setContent(bs);

        TitledPane uncollapsible = new TitledPane();
        uncollapsible.setCollapsible(false);
        uncollapsible.setText("Not Collapsible");
        Button bf = new Button("Button");
        bf.setPrefSize(75, 50);
        uncollapsible.setContent(bf);
        
        HBox hbox = new HBox(10);
        hbox.setPadding(new Insets(20, 0, 0, 20));
        hbox.getChildren().setAll(normal, gridTitlePane, normalText, unanimated, uncollapsible);

        Group root = (Group)scene.getRoot();
        root.getChildren().add(hbox);
        stage.setScene(scene);
        stage.show();
    }
}
