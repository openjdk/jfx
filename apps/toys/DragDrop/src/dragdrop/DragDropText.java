/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
package dragdrop;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DragDropText extends Application {

    private Point2D pressedCoords;
    private Timeline timeout;
    boolean timeoutPassed;

    @Override public void start(final Stage stage) {

        final DndTextEdit textEdit = new DndTextEdit();
        textEdit.setTranslateX(50);
        textEdit.setTranslateY(50);
        textEdit.setText("This one features default DnD");


        final DndTextEdit macLike = new DndTextEdit();
        macLike.setTranslateX(50);
        macLike.setTranslateY(130);
        macLike.setText("This is forced to behave like Mac");

        macLike.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                pressedCoords = new Point2D(event.getSceneX(), event.getSceneY());
                timeout = new Timeline();
                timeout.getKeyFrames().add(new KeyFrame(new Duration(500),
                        new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent t) {
                                timeoutPassed = true;
                                timeout = null;
                            }
                        }));
                timeout.play();
                timeoutPassed = false;
            }
        });

        macLike.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                event.setDragDetect(timeoutPassed);
                timeoutPassed = false;
                if (timeout != null &&
                        (event.getSceneX() != pressedCoords.getX() ||
                        event.getSceneY() != pressedCoords.getY())) {
                    timeout.stop();
                    macLike.clearSelection();
                    timeout = null;
                }
            }
        });

        final Group root = new Group();
        root.getChildren().add(textEdit);
        root.getChildren().add(macLike);
        final Scene scene = new Scene(root);

        stage.setTitle("Drag and Drop Text");
        stage.setWidth(500);
        stage.setHeight(250);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        textEdit.requestFocus();
    }

    public static String info() {
        return
                "This application contains two text drag/drop" +
                "boxes. Drag and drop text to/from them";
    }

    public static void main(String[] args) {
        Application.launch(DragDropText.class, args);
    }
}
