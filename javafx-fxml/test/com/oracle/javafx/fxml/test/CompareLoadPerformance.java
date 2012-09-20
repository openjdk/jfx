/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.javafx.fxml.test;

import java.io.IOException;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class CompareLoadPerformance extends Application {
    private static final Image ICON_48 = new Image(CompareLoadPerformance.class.getResourceAsStream("icon-48x48.png"));
    private static final boolean USE_FXML = true;

    @Override
    public void start(Stage primaryStage) throws Exception {
        TabPane tabPane = new TabPane();

        Tab borderPaneTab = new Tab("BorderPane");
        borderPaneTab.setContent(USE_FXML ? createBorderPaneFXML() : createBorderPane());
        tabPane.getTabs().add(borderPaneTab);

        Tab flowPaneTab = new Tab("FlowPane");
        flowPaneTab.setContent(USE_FXML ? createFlowPaneFXML() : createFlowPane());
        tabPane.getTabs().add(flowPaneTab);

        Tab gridPaneTab = new Tab("GridPane");
        gridPaneTab.setContent(USE_FXML ? createGridPaneFXML() : createGridPane());
        tabPane.getTabs().add(gridPaneTab);

        Tab tilePaneTab = new Tab("TilePane");
        tilePaneTab.setContent(USE_FXML ? createTilePaneFXML() : createTilePane());
        tabPane.getTabs().add(tilePaneTab);

        primaryStage.setScene(new Scene(tabPane));
        primaryStage.show();
    }

    private Node createBorderPane() {
        long t0 = System.currentTimeMillis();

        BorderPane borderPane = new BorderPane();

        //Top content
        Rectangle topRectangle = new Rectangle(400, 23, Color.DARKSEAGREEN);
        topRectangle.setStroke(Color.BLACK);
        borderPane.setTop(topRectangle);

        //Left content
        Label label1 = new Label("Left hand");
        Label label2 = new Label("Choice One");
        Label label3 = new Label("Choice Two");
        Label label4 = new Label("Choice Three");
        VBox leftVbox = new VBox();
        leftVbox.getChildren().addAll(label1, label2, label3, label4);
        borderPane.setLeft(leftVbox);

        //Right content
        Label rightlabel1 = new Label("Right hand");
        Label rightlabel2 = new Label("Thing A");
        Label rightlabel3 = new Label("Thing B");
        VBox rightVbox = new VBox();
        rightVbox.getChildren().addAll(rightlabel1, rightlabel2, rightlabel3);
        borderPane.setRight(rightVbox);

        //Center content
        Label centerLabel = new Label("We're in the center area.");
        ImageView imageView = new ImageView(ICON_48);

        //Using AnchorPane only to position items in the center
        AnchorPane centerAP = new AnchorPane();
        AnchorPane.setTopAnchor(centerLabel, Double.valueOf(5));
        AnchorPane.setLeftAnchor(centerLabel, Double.valueOf(20));
        AnchorPane.setTopAnchor(imageView, Double.valueOf(40));
        AnchorPane.setLeftAnchor(imageView, Double.valueOf(30));
        centerAP.getChildren().addAll(centerLabel, imageView);
        borderPane.setCenter(centerAP);

        //Bottom content
        Label bottomLabel = new Label("I am a status message, and I am at the bottom.");
        borderPane.setBottom(bottomLabel);

        long t1 = System.currentTimeMillis();

        System.out.println("BorderPane " + (t1 - t0) + "ms");

        return borderPane;
    }

    private Node createBorderPaneFXML() {
        long t0 = System.currentTimeMillis();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("border_pane.fxml"));
        fxmlLoader.getNamespace().put("ICON_48", ICON_48);

        BorderPane borderPane;
        try {
            borderPane = (BorderPane)fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        long t1 = System.currentTimeMillis();

        System.out.println("BorderPane FXML " + (t1 - t0) + "ms");

        return borderPane;
    }

    private Node createFlowPane() {
        long t0 = System.currentTimeMillis();

        final int ITEMS = 5;
        FlowPane flowPane = new FlowPane(2, 4);
        flowPane.setPrefWrapLength(200); //preferred wraplength
        Label[] shortLabels = new Label[ITEMS];
        Label[] longLabels = new Label[ITEMS];
        ImageView[] imageViews = new ImageView[ITEMS];

        for (int i = 0; i < ITEMS; i++) {
            shortLabels[i] = new Label("Short label.");
            longLabels[i] = new Label("I am a slightly longer label.");
            imageViews[i] = new ImageView(ICON_48);
            flowPane.getChildren().addAll(shortLabels[i], longLabels[i], imageViews[i]);
        }

        long t1 = System.currentTimeMillis();

        System.out.println("FlowPane " + (t1 - t0) + "ms");

        return flowPane;
    }

    private Node createFlowPaneFXML() {
        long t0 = System.currentTimeMillis();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("flow_pane.fxml"));
        fxmlLoader.getNamespace().put("ICON_48", ICON_48);

        FlowPane flowPane;
        try {
            flowPane = (FlowPane)fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        long t1 = System.currentTimeMillis();

        System.out.println("FlowPane FXML " + (t1 - t0) + "ms");

        return flowPane;
    }

    private Node createGridPane() {
        long t0 = System.currentTimeMillis();

        VBox vbox = new VBox();

        //grid1 places the child by specifying the rows and columns in
        // GridPane.setContraints()
        Label grid1Caption = new Label("The example below shows GridPane content placement by specifying rows and columns:");
        grid1Caption.setWrapText(true);
        GridPane grid1 = new GridPane();
        grid1.setHgap(4);
        grid1.setVgap(6);
        grid1.setPadding(new Insets(18, 18, 18, 18));
        ObservableList<Node> content = grid1.getChildren();

        Label label = new Label("Name:");
        GridPane.setConstraints(label, 0, 0);
        GridPane.setHalignment(label, HPos.RIGHT);
        content.add(label);

        label = new Label("John Q. Public");
        GridPane.setConstraints(label, 1, 0, 2, 1);
        GridPane.setHalignment(label, HPos.LEFT);
        content.add(label);

        label = new Label("Address:");
        GridPane.setConstraints(label, 0, 1);
        GridPane.setHalignment(label, HPos.RIGHT);
        content.add(label);

        label = new Label("12345 Main Street, Some City, CA");
        GridPane.setConstraints(label, 1, 1, 5, 1);
        GridPane.setHalignment(label, HPos.LEFT);
        content.add(label);

        vbox.getChildren().addAll(grid1Caption, grid1, new Separator());

        //grid2 places the child by influencing the rows and columns themselves
        //via GridRowInfo and GridColumnInfo. This grid uses the preferred
        //width/height and max/min width/height.
        Label grid2Caption = new Label("The example below shows GridPane content placement by influencing the rows and columns themselves.");
        grid2Caption.setWrapText(true);
        grid2Caption.setWrapText(true);
        GridPane grid2 = new GridPane();
        grid2.setPadding(new Insets(18, 18, 18, 18));
        RowConstraints rowinfo = new RowConstraints(40, 40, 40);
        ColumnConstraints colinfo = new ColumnConstraints(90, 90, 90);

        for (int i = 0; i <= 2; i++) {
            grid2.getRowConstraints().add(rowinfo);
        }

        for (int j = 0; j <= 2; j++) {
            grid2.getColumnConstraints().add(colinfo);
        }

        Label category = new Label("Category:");
        GridPane.setHalignment(category, HPos.RIGHT);
        Label categoryValue = new Label("Wines");
        Label company = new Label("Company:");
        GridPane.setHalignment(company, HPos.RIGHT);
        Label companyValue = new Label("Acme Winery");
        Label rating = new Label("Rating:");
        GridPane.setHalignment(rating, HPos.RIGHT);
        Label ratingValue = new Label("Excellent");

        ImageView imageView = new ImageView(ICON_48);
        GridPane.setHalignment(imageView, HPos.CENTER);

        //Place content
        GridPane.setConstraints(category, 0, 0);
        GridPane.setConstraints(categoryValue, 1, 0);
        GridPane.setConstraints(company, 0, 1);
        GridPane.setConstraints(companyValue, 1, 1);
        GridPane.setConstraints(imageView, 2, 1);
        GridPane.setConstraints(rating, 0, 2);
        GridPane.setConstraints(ratingValue, 1, 2);
        grid2.getChildren().addAll(category, categoryValue, company, companyValue, imageView, rating, ratingValue);

        vbox.getChildren().addAll(grid2Caption, grid2, new Separator());

        //grid3 places the child by influencing the rows and columns
        //via GridRowInfo and GridColumnInfo. This grid uses the percentages
        Label grid3Caption = new Label("The example below shows GridPane content placement by influencing row and column percentages.  Also, grid lines are made visible in this example.  The lines can be helpful in debugging.");
        grid3Caption.setWrapText(true);
        GridPane grid3 = new GridPane();
        grid3.setPadding(new Insets(18, 18, 18, 18));
        grid3.setGridLinesVisible(true);
        RowConstraints rowinfo3 = new RowConstraints();
        rowinfo3.setPercentHeight(50);

        ColumnConstraints colInfo2 = new ColumnConstraints();
        colInfo2.setPercentWidth(25);

        ColumnConstraints colInfo3 = new ColumnConstraints();
        colInfo3.setPercentWidth(50);

        grid3.getRowConstraints().add(rowinfo3);//2*50 percent
        grid3.getRowConstraints().add(rowinfo3);

        grid3.getColumnConstraints().add(colInfo2); //25 percent
        grid3.getColumnConstraints().add(colInfo3); //50 percent
        grid3.getColumnConstraints().add(colInfo2); //25 percent

        Label condLabel = new Label(" Member Name:");
        GridPane.setHalignment(condLabel, HPos.RIGHT);
        GridPane.setConstraints(condLabel, 0, 0);
        Label condValue = new Label("MyName");
        GridPane.setMargin(condValue, new Insets(0, 0, 0, 10));
        GridPane.setConstraints(condValue, 1, 0);

        Label acctLabel = new Label("Member Number:");
        GridPane.setHalignment(acctLabel, HPos.RIGHT);
        GridPane.setConstraints(acctLabel, 0, 1);
        TextField textBox = new TextField("Your number");
        GridPane.setMargin(textBox, new Insets(10, 10, 10, 10));
        GridPane.setConstraints(textBox, 1, 1);

        Button button = new Button("Help");
        GridPane.setConstraints(button, 2, 1);
        GridPane.setMargin(button, new Insets(10, 10, 10, 10));
        GridPane.setHalignment(button, HPos.CENTER);

        GridPane.setConstraints(condValue, 1, 0);
        grid3.getChildren().addAll(condLabel, condValue, button, acctLabel, textBox);

        vbox.getChildren().addAll(grid3Caption, grid3);

        long t1 = System.currentTimeMillis();

        System.out.println("GridPane " + (t1 - t0) + "ms");

        return vbox;
    }

    private Node createGridPaneFXML() {
        long t0 = System.currentTimeMillis();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("grid_pane.fxml"));
        fxmlLoader.getNamespace().put("ICON_48", ICON_48);

        VBox vBox;
        try {
            vBox = (VBox)fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        long t1 = System.currentTimeMillis();

        System.out.println("GridPane FXML " + (t1 - t0) + "ms");

        return vBox;
    }

    private Node createTilePane() {
        long t0 = System.currentTimeMillis();

        TilePane tilePane = new TilePane();
        tilePane.setPrefColumns(3); //preferred columns

        Button[] buttons = new Button[18];
        for (int j = 0; j < buttons.length; j++) {
            buttons[j] = new Button("button" + (j + 1), new ImageView(ICON_48));
            tilePane.getChildren().add(buttons[j]);
        }

        long t1 = System.currentTimeMillis();

        System.out.println("TilePane " + (t1 - t0) + "ms");

        return tilePane;
    }

    private Node createTilePaneFXML() {
        long t0 = System.currentTimeMillis();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("tile_pane.fxml"));
        fxmlLoader.getNamespace().put("ICON_48", ICON_48);

        TilePane tilePane;
        try {
            tilePane = (TilePane)fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        long t1 = System.currentTimeMillis();

        System.out.println("TilePane FXML " + (t1 - t0) + "ms");

        return tilePane;
    }

    public static void main(String[] args) throws Exception {
        launch(args);
    }
}
