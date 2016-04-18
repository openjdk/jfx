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

package hello;

import javafx.application.Application;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class HelloViewOrder extends Application {

    private boolean added = false;

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("View Order Test");

        final Slider slider = new Slider(-4, 0, -2);
        slider.setDisable(true);

        final Rectangle wheatRect = new Rectangle(150, 150, Color.WHEAT);
        wheatRect.setArcHeight(10);
        wheatRect.setArcWidth(10);
        wheatRect.setId("Wheat");
        wheatRect.setOnMousePressed(e -> System.out.println("Mouse Pressed: Wheat"));
        wheatRect.relocate(50, 30);
        final Pane rectsPane = new Pane();

        final Circle redCircle = new Circle(80, Color.RED);
        redCircle.setId("Red");
        redCircle.setOnMousePressed(e -> System.out.println("Mouse Pressed: Red"));
        redCircle.relocate(20, 10);

        final Circle greenCircle = new Circle(80, Color.GREEN);
        greenCircle.setId("Green");
        greenCircle.setOnMousePressed(e -> System.out.println("Mouse Pressed: Green"));
        greenCircle.relocate(100, 50);

        final Ellipse blueEllipse = new Ellipse(100, 80);
        blueEllipse.setFill(Color.BLUE);
        blueEllipse.setId("Blue");
        blueEllipse.setOnMousePressed(e -> System.out.println("Mouse Pressed: Blue"));
        blueEllipse.relocate(60, 100);

        rectsPane.getChildren().addAll(redCircle, greenCircle, blueEllipse);
        SubScene subScene = new SubScene(rectsPane, 300, 300, true, SceneAntialiasing.DISABLED);
        subScene.setCamera(new PerspectiveCamera());

        final HBox rootPane = new HBox(10);

        final CheckBox translateBtn = new CheckBox("Translate");
        translateBtn.setOnAction((javafx.event.ActionEvent event) -> {
            if (translateBtn.isSelected()) {
                System.err.println("translateBtn is selected");
                wheatRect.translateZProperty().bind(slider.valueProperty());
                redCircle.setTranslateZ(-2);
                greenCircle.setTranslateZ(-3);
                blueEllipse.setTranslateZ(0);
            } else {
                System.err.println("translateBtn is unselected");
                wheatRect.translateZProperty().unbind();
                wheatRect.setTranslateZ(0);
                redCircle.setTranslateZ(0);
                greenCircle.setTranslateZ(0);
                blueEllipse.setTranslateZ(0);
            }
        });

        final CheckBox viewOrderBtn = new CheckBox("viewOrder");
        viewOrderBtn.setOnAction((javafx.event.ActionEvent event) -> {
            if (viewOrderBtn.isSelected()) {
                System.err.println("viewOrderBtn is selected");
                wheatRect.viewOrderProperty().bind(slider.valueProperty());
                redCircle.setViewOrder(-2);
                // Test styling
                greenCircle.setStyle("-fx-view-order: -3;");
                blueEllipse.setViewOrder(0);
            } else {
                System.err.println("viewOrderBtn is unselected");
                wheatRect.viewOrderProperty().unbind();
                wheatRect.setViewOrder(0);
                redCircle.setViewOrder(0);
                // Test styling
                greenCircle.setStyle("-fx-view-order: 0;");
                blueEllipse.setViewOrder(0);
            }
        });

        final CheckBox opacityBtn = new CheckBox("0.5 opacity");
        opacityBtn.setOnAction((javafx.event.ActionEvent event) -> {
            if (opacityBtn.isSelected()) {
                wheatRect.setOpacity(0.5);
                redCircle.setOpacity(0.5);
                greenCircle.setOpacity(0.5);
                blueEllipse.setOpacity(0.5);
            } else {
                wheatRect.setOpacity(1.0);
                redCircle.setOpacity(1.0);
                greenCircle.setOpacity(1.0);
                blueEllipse.setOpacity(1.0);
            }
        });

        final Button removeBtn = new Button();
        final Button addBtn = new Button();
        addBtn.setText("Add");
        addBtn.setOnAction((javafx.event.ActionEvent event) -> {
            if (!added) {
                rectsPane.getChildren().add(wheatRect);
                slider.setDisable(false);
                removeBtn.setDisable(false);
                addBtn.setDisable(true);
                added = true;
            }
        });

        removeBtn.setText("Remove");
        removeBtn.setDisable(true);
        removeBtn.setOnAction((javafx.event.ActionEvent event) -> {
            if (added) {
                rectsPane.getChildren().remove(wheatRect);
                slider.setDisable(true);
                removeBtn.setDisable(true);
                addBtn.setDisable(false);
                added = false;
            }
        });

        VBox buttonsPane = new VBox(5);

        // This set of labels is to prove that viewOrder has no impact on
        // layout and focus traversal order.
        Label layoutLabel = new Label("Test Layout");
        Button b1 = new Button("First");
        b1.setViewOrder(-1);
        Button b2 = new Button("Second");
        b2.setViewOrder(1);
        Button b3 = new Button("Third");
        b3.setViewOrder(0);

        VBox layoutPane = new VBox(10);
        layoutPane.getChildren().addAll(layoutLabel, b1, b2, b3);

        buttonsPane.getChildren().addAll(opacityBtn, viewOrderBtn, translateBtn,
                addBtn, removeBtn, slider, new Separator(), layoutPane);
        rootPane.getChildren().addAll(buttonsPane, subScene);
        Scene scene = new Scene(rootPane);
        rectsPane.setStyle("-fx-border-color: RED;");

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws Exception {
        launch(args);
    }

}
