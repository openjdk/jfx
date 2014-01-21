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
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;


public class HelloScrollPane extends Application {

    public Image createImage(String filename, float width, float height) {
        String file = getClass().getResource(filename).toExternalForm();
        return new Image(file, width, height, true, true, false);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Scene scene = new Scene(tabPane, 600, 450);

        tabPane.getTabs().addAll(
                buildSimpleTab(),
                buildControlsTab()
        );

        stage.setTitle("Hello ScrollPane");
        stage.setScene(scene);
        stage.show();
    }

    private Tab buildSimpleTab() {
        Image image1 = createImage("duke.jpg", 200f, 200f);
        ImageView imageView1 = new ImageView();
        imageView1.setImage(image1);

        Image image2 = createImage("duke.jpg", 200f, 200f);
        ImageView imageView2 = new ImageView();
        imageView2.setImage(image2);

        Image image3 = createImage("duke.jpg", 200f, 200f);
        ImageView imageView3 = new ImageView();
        imageView3.setImage(image3);

        Pane content = new Pane();
        content.setStyle("-fx-background-color: chocolate");
        content.setPadding(new Insets(10));

        ScrollPane sView1 = new ScrollPane();
        sView1.setContent(imageView1);
        sView1.setPrefSize(150, 150);
        sView1.setLayoutX(20);
        sView1.setLayoutY(40);
        sView1.setPannable(true);
        sView1.setVisible(true);
        content.getChildren().add(sView1);

        ScrollPane sView2 = new ScrollPane();
        sView2.setContent(imageView2);
        sView2.setPrefSize(214, 150);
        sView2.setLayoutX(200);
        sView2.setLayoutY(40);
        sView2.setPannable(true);
        sView2.setVisible(true);
        /*
        **  set focus traversable to allow keyboard input.
        */
        sView2.setFocusTraversable(true);
        content.getChildren().add(sView2);

        ScrollPane sView3 = new ScrollPane();
        sView3.setContent(imageView3);
        sView3.setPrefSize(150, 214);
        sView3.setLayoutX(430);
        sView3.setLayoutY(40);
        sView3.setPannable(true);
        sView3.setVisible(true);
        content.getChildren().add(sView3);

        Tab tab = new Tab("Simple");
        tab.setContent(content);
        return tab;
    }

    private Tab buildControlsTab() {
        VBox vbox = new VBox(10);
        vbox.setStyle("-fx-background-color: chocolate");
        vbox.setPadding(new Insets(10));

        Label introLabel = new Label("Mouse wheel should always scroll ScrollPane,\r\n" +
                "even when over controls,\r\nas long as they don't have scrollbars themselves.");
        introLabel.setWrapText(true);
        introLabel.setFont(Font.font(18));
        vbox.getChildren().addAll(introLabel);

        // Button
        TilePane btnTilePane = new TilePane(50, 50);
        for (int i = 0; i < 5; i++) {
            Button btn = new Button("Button " + i);
            btn.setMinSize(100, 100);
            btnTilePane.getChildren().add(btn);
        }

        // TextField / TextArea
        TilePane textTilePane = new TilePane(50, 10);
        for (int i = 0; i < 5; i++) {
            TextField textField = new TextField("TextField");
            textTilePane.getChildren().add(textField);
        }
        for (int i = 0; i < 5; i++) {
            TextArea textArea = new TextArea("TextArea");
            textTilePane.getChildren().add(textArea);
        }

        // ListView
        TilePane listTilePane = new TilePane(50, 50);
        for (int i = 0; i < 5; i++) {
            ListView listView = new ListView();
            listView.setPlaceholder(new Label("No content - scroll should be ignored"));
            listTilePane.getChildren().add(listView);
        }

        for (int i = 0; i < 5; i++) {
            ListView listView = new ListView();
            listView.getItems().addAll("item 1", "item 2", "item 3");
            listTilePane.getChildren().add(listView);
        }

        // TreeView
        TilePane treeTilePane = new TilePane(50, 50);
        for (int i = 0; i < 5; i++) {
            TreeView treeView = new TreeView();
            treeTilePane.getChildren().add(treeView);
        }
        for (int i = 0; i < 5; i++) {
            TreeView treeView = new TreeView();
            TreeItem root = new TreeItem("Root");
            treeView.setRoot(root);
            treeTilePane.getChildren().add(treeView);
        }

        vbox.getChildren().addAll(btnTilePane, listTilePane, treeTilePane, textTilePane);

        ScrollPane sp1 = new ScrollPane();
        sp1.setFitToWidth(true);
        sp1.setContent(vbox);

        Tab tab = new Tab("Scroll Test");
        tab.setContent(sp1);
        return tab;
    }
}
