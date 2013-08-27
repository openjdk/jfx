/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 */
public class HelloRectangle extends Application {
    @Override public void start(Stage stage) {
        stage.setTitle("Hello Rectangle");

        Group root = new Group();
        Scene scene = new Scene(root, 600, 450);

        Slider aw = new Slider(-300, 300, 0);
        Slider ah = new Slider(-300, 300, 0);

        Rectangle rect = new Rectangle();
        rect.setX(25);
        rect.setY(40);
        rect.setWidth(300);
        rect.setHeight(300);
        rect.setFill(Color.RED);
        rect.arcWidthProperty().bind(aw.valueProperty());
        rect.arcHeightProperty().bind(ah.valueProperty());

        Circle circle = new Circle();
        circle.setCenterX(450);
        circle.setCenterY(200);
        circle.setRadius(50);
        circle.setFill(Color.RED);
        circle.radiusProperty().bind(aw.valueProperty());

        Ellipse ellipse = new Ellipse();
        ellipse.setCenterX(450);
        ellipse.setCenterY(375);
        ellipse.radiusXProperty().bind(aw.valueProperty());
        ellipse.radiusYProperty().bind(ah.valueProperty());
        ellipse.setFill(Color.RED);

        VBox box = new VBox(aw, ah);
        box.relocate(350, 20);

//        root.getChildren().addAll(rect, circle, ellipse, box);

        Rectangle behind = new Rectangle(100, 100, new Color(1, 0, 0, .1));
        Rectangle front = new Rectangle(10, 10, Color.GREEN);
        front.setBlendMode(BlendMode.SRC_ATOP);

        root.getChildren().addAll(behind, front);

//        FillTransition tx = new FillTransition(Duration.seconds(3), behind, Color.RED, Color.BLUE);
//        tx.setAutoReverse(true);
//        tx.setCycleCount(FillTransition.INDEFINITE);
//        tx.play();

        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
