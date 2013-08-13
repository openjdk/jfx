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

import javafx.animation.AnimationTimer;
import javafx.animation.FillTransition;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.sun.javafx.perf.PerformanceTracker;

/**
 * Spins the tiger!
 */
public class HelloTiger extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        final double sceneWidth = 1024;
        final double sceneHeight = 768;

        Rectangle background = new Rectangle(0, 0, sceneWidth, sceneHeight);

        Tiger tiger = new Tiger();

        Group tigerGroup = new Group(tiger);
        tigerGroup.setTranslateX(400);
        tigerGroup.setTranslateY(200);

        Group root = new Group(background, tigerGroup);
        Scene scene = new Scene(root, sceneWidth, sceneHeight);
        stage.setScene(scene);
        stage.show();

        FillTransition tx = new FillTransition(Duration.seconds(5), background, Color.BLACK, Color.RED);
        tx.setCycleCount(FillTransition.INDEFINITE);
        tx.setAutoReverse(true);
//        tx.play();

        RotateTransition rot = new RotateTransition(Duration.seconds(5), tigerGroup);
        rot.setCycleCount(RotateTransition.INDEFINITE);
        rot.setToAngle(360);
        rot.play();

        final PerformanceTracker tracker = PerformanceTracker.getSceneTracker(scene);
        AnimationTimer trackerTimer = new AnimationTimer() {
            long ticks = 0;
            @Override
            public void handle(long now) {
                ticks++;
                if (ticks % 60 == 0) {
                    System.out.println(tracker.getInstantFPS());
                }
            }
        };
        trackerTimer.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}