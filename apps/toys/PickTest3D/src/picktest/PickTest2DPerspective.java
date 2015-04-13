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
import javafx.scene.ParallelCamera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

/**
 * Test application verifying the local coordinates are correct with perspective
 * camera projection.
 */
public class PickTest2DPerspective extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Perspective 2D picking");

        final Circle dot = new Circle(10, Color.YELLOW);
        dot.setTranslateX(100);
        dot.setTranslateY(100);
        dot.setMouseTransparent(true);

        final Rectangle green = new Rectangle(300, 300, Color.DARKGREEN);
        green.setTranslateX(100);
        green.setTranslateY(100);
        green.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                dot.setTranslateX(100 + event.getX());
                dot.setTranslateY(100 + event.getY());
            }
        });

        final Circle sceneDot1 = new Circle(5, Color.BLACK);
        sceneDot1.setTranslateZ(2);
        sceneDot1.setMouseTransparent(true);

        final Circle sceneDot2 = new Circle(3, Color.DARKGRAY);
        sceneDot2.setTranslateZ(1);
        sceneDot2.setMouseTransparent(true);

        final Group rotated = new Group(green, dot);
        rotated.getTransforms().add(new Rotate(60, 250, 250, 0, Rotate.Y_AXIS));

        final Group root = new Group(rotated, sceneDot1, sceneDot2);
        final Scene scene = new Scene(root, 500, 500);
        scene.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                sceneDot1.setTranslateX(event.getSceneX());
                sceneDot1.setTranslateY(event.getSceneY());
                sceneDot2.setTranslateX(event.getX());
                sceneDot2.setTranslateY(event.getY());
                sceneDot2.setTranslateZ(event.getZ());
            }
        });

        scene.setOnKeyTyped(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent event) {
                if (event.getEventType() == KeyEvent.KEY_TYPED &&
                        event.getCharacter().equals(" ")) {
                    if (scene.getCamera() instanceof PerspectiveCamera) {
                        scene.setCamera(new ParallelCamera());
                    } else {
                        scene.setCamera(new PerspectiveCamera());
                    }
                }
            }
        });

        root.setDepthTest(DepthTest.ENABLE);
        scene.setCamera(new PerspectiveCamera());

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
