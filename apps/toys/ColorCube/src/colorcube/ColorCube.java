/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package colorcube;

import javafx.event.EventHandler;
import java.util.ArrayList;
import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import static javafx.scene.transform.Rotate.*;
import javafx.stage.Stage;

public class ColorCube extends Application {
    private static final Color[] colors = {
        Color.color(1, 0, 0), // RED
        Color.color(0, 1, 0), // GREEN
        Color.color(0, 0, 1), // BLUE
        Color.color(1, 1, 0), // YELLOW
        Color.color(1, 0, 1), // MAGENTA
        Color.color(0, 1, 1)  // CYAN
    };

    private static final int SIZE = 300;

    private static final float OFFSET = SIZE * 0.5F;

    private static final float EXPLODED_OFFSET = SIZE * 0.6F;

    private boolean exploded = false;

    // FRONT, LEFT, BACK, RIGHT, TOP, BOTTOM
    private static final int[] rotates = {
        0, -90, -180, -270, -90, 90
    };

    private static final Point3D[] axes = {
        Y_AXIS, Y_AXIS, Y_AXIS, Y_AXIS, X_AXIS, X_AXIS
    };

    private static final int[] translates = {
        0, 0, -1,   // FRONT
        1, 0, 0,    // LEFT
        0, 0, 1,    // BACK
        -1, 0, 0,   // RIGHT
        0, -1, 0,   // TOP
        0, 1, 0     // BOTTOM
    };
    private final Group mainGroup = new Group();
    private Timeline timeline;

    private Rectangle cubeFace(int i) {
        Rectangle rect = new Rectangle( SIZE, SIZE, colors[i]);
        rect.setTranslateX(translates[i * 3 + 0] * OFFSET);
        rect.setTranslateY(translates[i * 3 + 1] * OFFSET);
        rect.setTranslateZ(translates[i * 3 + 2] * OFFSET);
        rect.setRotate(rotates[i]);
        rect.setRotationAxis(axes[i]);
        return rect;
    }

    private Scene createColorCube(Stage stage) {
        stage.setTitle("Color Cube");
        final ArrayList<Node> cube = new ArrayList<Node>();
        for (int i = 0; i < colors.length; i++) {
            cube.add(cubeFace(i));
        }
        Group group = new Group();
        group.setTranslateX(250);
        group.setTranslateY(150);
        group.setRotate(30);
        group.setRotationAxis(new Point3D(1, 1, 1));
        group.getChildren().addAll(cube);
        mainGroup.getChildren().add(group);
        mainGroup.setRotationAxis(Y_AXIS);
        mainGroup.setOnKeyTyped(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent e) {
                if (e.getCharacter().equals("x")) {
                    mainGroup.setRotationAxis(X_AXIS);
                } else if (e.getCharacter().equals("y")) {
                    mainGroup.setRotationAxis(Y_AXIS);
                } else if (e.getCharacter().equals("z")) {
                    mainGroup.setRotationAxis(Z_AXIS);
                } else if (e.getCharacter().equals("e")) {
                    exploded = !exploded;
                    float offset = (exploded) ? (EXPLODED_OFFSET) : (OFFSET);
                    int i = 0;
                    for (Node face : cube) {
                        face.setTranslateX(translates[i * 3 + 0] * offset);
                        face.setTranslateY(translates[i * 3 + 1] * offset);
                        face.setTranslateZ(translates[i * 3 + 2] * offset);
                        ++i;
                    }
                } else if (e.getCharacter().equals("p")) {
                    if (timeline.getStatus() == Status.RUNNING) {
                        timeline.pause();
                    } else {
                        timeline.play();
                    }
                }
            }
        });
        Scene scene = new Scene(mainGroup, 800, 600, true);
        PerspectiveCamera camera = new PerspectiveCamera();
        camera.setFieldOfView(30);
        scene.setCamera(camera);
        return scene;
    }

    private Scene createColorRect(Stage stage) {
        stage.setTitle("Color Rect Demo");
        System.out.println("*************************************************************");
        System.out.println("*    WARNING: common conditional SCENE3D isn\'t supported    *");
        System.out.println("*************************************************************");
        final Rectangle rect = new Rectangle(SIZE, SIZE, Color.RED);
        rect.setTranslateX(250);
        rect.setTranslateY(150);
        mainGroup.getChildren().add(rect);
        Scene scene = new Scene(mainGroup, 800, 600);
        return scene;
    }

    @Override public void start(Stage stage) {
        Scene scene = null;

        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            scene = createColorRect(stage);
        } else {
            scene = createColorCube(stage);
        }

        LinearGradient sceneFill = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop[] {
                    new Stop(0, Color.web("#e0e0e0")),
                    new Stop(1, Color.web("#a0a0a0")) });
        scene.setFill(sceneFill);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
        
        mainGroup.requestFocus();
        KeyValue kv = new KeyValue (mainGroup.rotateProperty(), Float.valueOf(-360));
        KeyFrame kf = new KeyFrame(Duration.seconds(4), kv);
        timeline = new Timeline(kf);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
