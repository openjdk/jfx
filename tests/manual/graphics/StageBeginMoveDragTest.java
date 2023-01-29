/*
 *  Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Map;

public class StageBeginMoveDragTest extends Application {
    public static void main(String[] args) {
        launch(StageBeginMoveDragTest.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        var rect = new Rectangle(100, 100, Color.TOMATO);
        var pane = new StackPane(rect);
        var scene = new Scene(pane, 400, 200, Color.TRANSPARENT);

        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        pane.setStyle("-fx-background-color: linear-gradient(to bottom, derive(cadetblue, 20%), cadetblue);" +
                      "-fx-border-color: derive(cadetblue, -20%);" +
                      "-fx-effect: dropshadow(three-pass-box, derive(cadetblue, -20%), 10, 0, 4, 4);" +
                      "-fx-border-width: 5;" +
                      "-fx-background-insets: 12;" +
                      "-fx-border-insets: 10;" +
                      "-fx-border-radius: 6;" +
                      "-fx-background-radius: 6;");

        rect.setOnMouseDragged(e -> primaryStage.beginMoveDrag(e.getButton(), e.getScreenX(), e.getScreenY()));

        SceneResizer sceneResizer = new SceneResizer(scene, 3);
        sceneResizer.watch();

        primaryStage.show();
    }

    static class SceneResizer {
        private final Scene scene;
        private final int thresholdPixels;

        private WindowEdge currentEdge;
        private double x;
        private double y;

        private static final Map<WindowEdge, Cursor> CURSOR_MAP = Map.of(WindowEdge.LEFT, Cursor.W_RESIZE,
                WindowEdge.RIGHT, Cursor.E_RESIZE,
                WindowEdge.BOTTOM, Cursor.S_RESIZE,
                WindowEdge.TOP, Cursor.N_RESIZE,
                WindowEdge.BOTTOM_LEFT, Cursor.SW_RESIZE,
                WindowEdge.BOTTOM_RIGHT, Cursor.SE_RESIZE,
                WindowEdge.TOP_LEFT, Cursor.NW_RESIZE,
                WindowEdge.TOP_RIGHT, Cursor.NE_RESIZE);

        private SceneResizer(final Scene scene, int thresholdPixels) {
            this.scene = scene;
            this.thresholdPixels = thresholdPixels;
        }

        public void watch() {
            scene.addEventHandler(MouseEvent.MOUSE_MOVED, this::detect);
            scene.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
                x = e.getScreenX();
                y = e.getScreenY();
            });
            scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::resize);

//            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F8, KeyCombination.ALT_DOWN),
//                                        this::keyboardResize);
        }

//        private void keyboardResize() {
//            if (scene.getWindow() != null) {
//                double middleX = scene.getWindow().getX() + scene.getX() + (scene.getWidth() / 2);
//                double middleY = scene.getWindow().getY() + scene.getY() + (scene.getHeight() / 2);
//
//                scene.getWindow().beginResizeDrag(WindowEdge.BOTTOM, MouseButton.NONE, middleX, middleY);
//            }
//        }

        private void resize(MouseEvent e) {
            if (currentEdge != null && scene.getWindow() != null) {
                scene.getWindow().beginResizeDrag(currentEdge, e.getButton(), x, y);
            }
        }

        private void detect(MouseEvent e) {
            //mouse is on left side
            if (e.getX() <= thresholdPixels) {
                if (e.getY() >= scene.getHeight() - thresholdPixels) {
                    setEdge(WindowEdge.BOTTOM_LEFT);
                    return;
                }

                if (e.getY() <= thresholdPixels) {
                    setEdge(WindowEdge.TOP_LEFT);
                    return;
                }

                setEdge(WindowEdge.LEFT);
            } else if (e.getX() >= scene.getWidth() - thresholdPixels) {
                if (e.getY() >= scene.getHeight() - thresholdPixels) {
                    setEdge(WindowEdge.BOTTOM_RIGHT);
                    return;
                }

                if (e.getY() <= thresholdPixels) {
                    setEdge(WindowEdge.TOP_RIGHT);
                    return;
                }

                setEdge(WindowEdge.RIGHT);
            } else if (e.getY() <= thresholdPixels) {
                setEdge(WindowEdge.TOP);
            } else if (e.getY() >= scene.getHeight() - thresholdPixels) {
                setEdge(WindowEdge.BOTTOM);
            } else {
                setEdge(null);
            }
        }

        private void setEdge(WindowEdge edge) {
            currentEdge = edge;
            scene.setCursor((edge == null) ? null : CURSOR_MAP.get(edge));
        }
    }
}
