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

package com.javafx.experiments.dukepad.cubeGame;

import com.javafx.experiments.dukepad.cubeGame.utils.DragSupport;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

/**
 * CubeGame UI
 */
public class CubeGameUI extends Pane {

    private DragSupport dragSupport1;
    private DragSupport dragSupport2;

    private final Translate translate = new Translate(0, 0, 0);
    private final Translate translateZ = new Translate(0, 0, 0);
    private final Rotate rotateX = new Rotate(-150, 0, 0, 0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-40, 0, 0, 0, Rotate.Y_AXIS);
    private final Translate translateY = new Translate(0, 0, 0);
    private final MagicCube magicCube;

    public CubeGameUI() {
        setBackground(new Background(new BackgroundFill[0]));
        magicCube = new MagicCube();
        Group root = new Group(magicCube);
        getChildren().add(root);

//        magicCube.setMouseTransparent(true);
//        scene.setCamera(new PerspectiveCamera());

        Translate centerTranslate = new Translate();
        centerTranslate.xProperty().bind(this.widthProperty().divide(2));
        centerTranslate.yProperty().bind(this.heightProperty().divide(2));

        root.getTransforms().addAll(centerTranslate, translate, translateZ, rotateX, rotateY);
        root.setDepthTest(DepthTest.ENABLE);

        dragSupport1 = new DragSupport(this, null, Orientation.HORIZONTAL, rotateY.angleProperty());
        dragSupport2 = new DragSupport(this, null, Orientation.VERTICAL, rotateX.angleProperty());

        translateZ.setZ(-1050);
//        translateZ.zProperty().addListener(new ChangeListener<Number>() {
//            @Override
//            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
//                System.out.println("number2 = " + number2);
//            }
//        });

        this.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // consuming any mouse events that happen over the app area
                mouseEvent.consume();
            }
        });

        this.setPickOnBounds(false);
    }

    public void demo() {
        final MagicCubePlayer mcPlayer = new MagicCubePlayer(magicCube);
        final Runnable demo = new Runnable() {

            @Override
            public void run() {
                mcPlayer.generateRandom();
                mcPlayer.play();
            }
        };
        final Runnable pauseAndDemo = new Runnable() {

            @Override
            public void run() {
                Timeline p = new Timeline(new KeyFrame(Duration.seconds(10), new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent t) {
                        demo.run();
                    }
                }));
                p.play();
            }
        };
        mcPlayer.setOnPlayEnd(pauseAndDemo);
        demo.run();
        final Rotate rX = new Rotate(0, Rotate.X_AXIS);
        final Rotate rY = new Rotate(0, Rotate.Y_AXIS);
        final Rotate rZ = new Rotate(0, Rotate.Z_AXIS);

        magicCube.getTransforms().setAll(rX, rY, rZ);

        final long s = System.nanoTime();

        AnimationTimer animation = new AnimationTimer() {

            @Override
            public void handle(long l) {
                l -= s;
                double p = l * 1e-9;
                rX.setAngle(Math.sin(p / 13) * 360);
                rY.setAngle(Math.sin(p / 23) * 360);
                rZ.setAngle(Math.sin(p / 27) * 720);
            }
        };
        animation.start();
    }
}
