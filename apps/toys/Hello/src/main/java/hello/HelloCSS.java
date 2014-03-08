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
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class HelloCSS extends Application {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("Hello CSS");
        Scene scene = new Scene(new Group(), 600, 450);
        scene.setFill(Color.LIGHTGREEN);
        scene.getStylesheets().add("hello/hello.css");
        Rectangle rect = new Rectangle();
        rect.getStyleClass().add("rect");
        rect.setX(25);
        rect.setY(40);
        rect.setWidth(100);
        rect.setHeight(50);
        rect.setFill(Color.GREEN);
        Rectangle rect2 = new Rectangle();
        rect2.getStyleClass().add("rect");
        rect2.setX(135);
        rect2.setY(40);
        rect2.setWidth(100);
        rect2.setHeight(50);
        rect2.setStyle(
                "-fx-stroke: yellow;"
              + "-fx-stroke-width: 3;"
              + "-fx-stroke-dash-array: 5 7;"
        );

        Node swapTest = createSwapTest();
        swapTest.setLayoutX(25);
        swapTest.setLayoutY(110);

        ((Group)scene.getRoot()).getChildren().addAll(rect,rect2,swapTest);
        stage.setScene(scene);
        stage.show();
    }

    private Node createSwapTest() {

        final StackPane r1 = new StackPane();
        r1.setPrefSize(100,50);
        r1.setStyle("-fx-base: red; -fx-border-color: red;");

        final StackPane r2 = new StackPane();
        r2.setPrefSize(100,50);
        r2.setStyle("-fx-base: yellow; -fx-border-color: yellow;");

        final Button swapButton = new Button("Move");
        swapButton.setOnAction(actionEvent -> {
            if (swapButton.getParent() == r1) {
                r1.getChildren().remove(swapButton);
                r2.getChildren().add(swapButton);
            } else if (swapButton.getParent() == r2) {
                r2.getChildren().remove(swapButton);
                r1.getChildren().add(swapButton);
            }
        });
        r1.getChildren().add(swapButton);

        FlowPane hBox = new FlowPane(Orientation.HORIZONTAL, 5, 5);
        hBox.getChildren().addAll(r1, r2, new Text("Click button to move.\nButton's base color should match surrounding border."));

        return hBox;
    }
}
