/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package picktest;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * Test application verifying correct 2D picking with depth-buffer on.
 */
public class PickTest2DDepthBuffer extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Detph buffer 2D picking");


        final Rectangle red = new Rectangle(100, 100, 200, 200);
        red.setFill(Color.RED);
        red.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                red.setFill(Color.ORANGE);
            }
        });
        red.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                red.setFill(Color.RED);
            }
        });

        final Rectangle blue = new Rectangle(200, 200, 200, 200);
        blue.setFill(Color.BLUE);
        blue.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                blue.setFill(Color.LIGHTBLUE);
            }
        });
        blue.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                blue.setFill(Color.BLUE);
            }
        });

        red.setTranslateZ(-1);

        Group root = new Group(red, blue);
        Scene scene = new Scene(root, 500, 500, true);

        root.setDepthTest(DepthTest.ENABLE);
        scene.setCamera(new PerspectiveCamera());

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
