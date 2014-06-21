/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.sun.javafx.perf.PerformanceTracker;

public class HelloFPS extends Application {

    private static final Color colors[] = {
        Color.RED,
        Color.ORANGE,
        Color.YELLOW,
        Color.GREEN,
        Color.BLUE,
        Color.INDIGO,
        Color.VIOLET
    };

    private int curColorIdx = 0;

    @Override public void start(Stage stage) {
        stage.setTitle("Hello FPS");
        final Scene scene = new Scene(new Group(), 600, 450);
        scene.setFill(Color.color(0.8, 0.8, 0.7));

        final Rectangle rect = new Rectangle(5, 5, Color.GRAY);
        rect.setLayoutX(5);
        rect.setLayoutY(5);

        final Text text = new Text("??? fps");
        text.setFont(new Font(30));
        text.setFill(colors[curColorIdx]);
        text.setLayoutX(5);
        text.setLayoutY(45);

        ((Group)scene.getRoot()).getChildren().addAll(rect, text);
        stage.setScene(scene);
        stage.show();

        final Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(true);
        final KeyValue kv = new KeyValue (rect.layoutXProperty(), 25);
        final KeyFrame kf = new KeyFrame(Duration.millis(5000), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();

        final PerformanceTracker tracker = PerformanceTracker.getSceneTracker(scene);

        final Timeline tlTracker = new Timeline();
        tlTracker.setCycleCount(Timeline.INDEFINITE);
        final KeyFrame kfTracker = new KeyFrame(
                Duration.millis(500),
                event -> {
                    int fps = (int) Math.round(tracker.getInstantFPS());
                    text.setText("" + fps + " fps");
                    curColorIdx = (curColorIdx + 1) % colors.length;
                    text.setFill(colors[curColorIdx]);
                });
        tlTracker.getKeyFrames().add(kfTracker);
        tlTracker.play();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
