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

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.sun.javafx.perf.PerformanceTracker;

/**
 */
public class HelloRectangle3D extends Application {
    @Override public void start(Stage stage) throws Exception {
        final double sceneWidth = 640;
        final double sceneHeight = 480;

        Rectangle background = new Rectangle((sceneWidth - 300) / 2, (sceneHeight - 300) / 2, 300, 300);
        background.setFill(Color.PURPLE);
        background.setRotationAxis(Rotate.Y_AXIS);
        background.setRotate(10);

        final Rectangle rect = rect((sceneWidth - 200) / 2, (sceneHeight - 200) / 2, 200, 200, Color.LIME);
        rect.setRotationAxis(Rotate.Y_AXIS);
        rect.setCache(true);
        rect.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                // This is used to test that the texture gets recreated as needed
                if (event.isShiftDown()) {
                    rect.setStrokeWidth(Math.random() * 10);
                } else {
                    rect.setFill(new Color(Math.random(), Math.random(), Math.random(), 1));
                }
            }
        });

        Group stack = new Group(background, rect);

        Scene scene = new Scene(stack, sceneWidth, sceneHeight, true);
        scene.setCamera(new PerspectiveCamera());
        scene.setFill(Color.BLACK);
        stage.setScene(scene);
        stage.setTitle("HelloRectangle3D");
        stage.show();

        RotateTransition tx = new RotateTransition(Duration.seconds(20), rect);
        tx.setToAngle(360);
        tx.setCycleCount(RotateTransition.INDEFINITE);
        tx.setInterpolator(Interpolator.LINEAR);
        tx.play();

        PerformanceTracker tracker = PerformanceTracker.getSceneTracker(scene);
        Timeline t = new Timeline(
            new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    System.out.println("" + tracker.getAverageFPS() + " average fps, " + tracker.getInstantFPS() + " instant fps");
                }
            })
        );
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
    }

    private Rectangle rect(double x, double y, double width, double height, Color color) {
        Rectangle rect = new Rectangle(x, y, width, height);
        rect.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, color), new Stop(1, color.darker().darker())));
        rect.setArcHeight(42);
        rect.setArcWidth(42);
        rect.setStroke(Color.WHITE);
        rect.setStrokeWidth(5);
        rect.setStrokeType(StrokeType.OUTSIDE);
        return rect;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
