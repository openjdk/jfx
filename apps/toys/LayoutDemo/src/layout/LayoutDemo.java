/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package layout;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class LayoutDemo extends Application {

    final double STAGE_WIDTH = 1244;
    final double STAGE_HEIGHT = 700;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {

        ResizableTab resizableTab = new ResizableTab("Resizable");
        PaneTab paneTab = new PaneTab("Pane");
        HBoxTab hboxTab = new HBoxTab("HBox");
        VBoxTab vboxTab = new VBoxTab("VBox");
        FlowPaneTab flowPaneTab = new FlowPaneTab("FlowPane");
        BorderPaneTab borderPaneTab = new BorderPaneTab("BorderPane");
        StackPaneTab stackPaneTab = new StackPaneTab("StackPane");
        TilePaneTab tilePaneTab = new TilePaneTab("TilePane");
        AnchorPaneTab anchorTab = new AnchorPaneTab("AnchorPane");
        GridPaneTab gridPane = new GridPaneTab("GridPane");
        CustomPaneTab customPane = new CustomPaneTab("CustomPane");;
        CustomTilePaneTab customTilePane = new CustomTilePaneTab("CustomTilePane");

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(resizableTab, paneTab, hboxTab, vboxTab,
                flowPaneTab, borderPaneTab, stackPaneTab, tilePaneTab,
                anchorTab, gridPane, customPane, customTilePane);

        // Set the size of selected tab to its scene
        tabPane.getSelectionModel().selectedIndexProperty()
                .addListener((o, oldValue, newValue) -> {
        stage.setWidth(STAGE_WIDTH);
        stage.setHeight(STAGE_HEIGHT);
//                    System.err.println("Stage : "
//                            + stage.getWidth() + ", " + stage.getHeight());

                });

        BorderPane root = new BorderPane();
        root.setCenter(tabPane);

        Scene scene = new Scene(root);
        // Add a style sheet to the scene
        scene.getStylesheets().addAll("resources/css/layoutdemos.css");
        stage.setScene(scene);
        stage.setWidth(STAGE_WIDTH);
        stage.setHeight(STAGE_HEIGHT);
        stage.setTitle("JavaOne Layout Demo");
        stage.show();
    }
}
