/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class HelloTranspStageAnim extends Application {

    private static final double SIZE = 400;
    private static final double HALF_SIZE = SIZE / 2.0;

    @Override public void start(Stage stage) {
        stage.setTitle("Hello Transparent Stage");
        stage.initStyle(StageStyle.TRANSPARENT);
        Group root = new Group();

        final Scene scene = new Scene(root, SIZE, SIZE);
        scene.setFill(Color.TRANSPARENT);
        Color bgColor = Color.LIGHTGREEN.deriveColor(0, 1, 1, 0.7);
        Circle bg = new Circle(HALF_SIZE, HALF_SIZE, HALF_SIZE, bgColor);
        final Rectangle rect = new Rectangle();
        rect.setX(HALF_SIZE / 2.0);
        rect.setY(40);
        rect.setWidth(HALF_SIZE / 2.0);
        rect.setHeight(50);
        rect.setFill(Color.RED);

        Button button = new Button("Close");
        button.setOnAction(e -> stage.close());
        button.setLayoutX(HALF_SIZE - 20);
        button.setLayoutY(HALF_SIZE - 10);

        root.getChildren().addAll(bg, rect, button);
        stage.setScene(scene);
        stage.show();

        final Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(true);
        final KeyValue kv = new KeyValue(rect.xProperty(), HALF_SIZE);
        final KeyFrame kf = new KeyFrame(Duration.millis(500), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
