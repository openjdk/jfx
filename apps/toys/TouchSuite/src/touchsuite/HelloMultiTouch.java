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

package touchsuite;

import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class HelloMultiTouch extends Application {

    @Override public void start(Stage stage) {
        stage.setTitle("Touch Test");

        Group root = new Group();
        Scene scene = new Scene(root, 600, 600);
        scene.setFill(Color.LIGHTGREEN);

        root.getChildren().add(new TouchCircle(100, 100, Color.RED));
        root.getChildren().add(new TouchCircle(500, 100, Color.GREEN));
        root.getChildren().add(new TouchCircle(100, 500, Color.BLUE));
        root.getChildren().add(new TouchCircle(500, 500, Color.YELLOW));

        stage.setScene(scene);
        stage.show();
    }


    private static class TouchCircle extends Circle {
        private long touchId = -1;
        double touchx, touchy;
        boolean playing = false;

        public TouchCircle(int x, int y, Color color) {
            super(75, color);
            setTranslateX(x);
            setTranslateY(y);

            setOnTouchPressed(new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent event) {
                    if (touchId == -1) {
                        touchId = event.getTouchPoint().getId();
                        touchx = event.getTouchPoint().getSceneX() - getTranslateX();
                        touchy = event.getTouchPoint().getSceneY() - getTranslateY();
                    }
                    event.consume();
                }
            });

            setOnTouchReleased(new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent event) {
                    if (event.getTouchPoint().getId() == touchId) {
                        touchId = -1;
                    }
                    event.consume();
                }
            });

            setOnTouchMoved(new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent event) {
                    if (!playing && event.getTouchPoint().getId() == touchId) {
                        setTranslateX(event.getTouchPoint().getSceneX() - touchx);
                        setTranslateY(event.getTouchPoint().getSceneY() - touchy);
                    }
                    event.consume();
                }
            });

            // Here I have to use ANY, or register the same handler to both
            // MOVED and STATIONARY
            addEventHandler(TouchEvent.ANY, new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent event) {
                    if (event.getEventType() != TouchEvent.TOUCH_MOVED
                            && event.getEventType() != TouchEvent.TOUCH_STATIONARY) {
                        return;
                    }

                    if (event.getTouchPoint().getId() != touchId) {
                        return;
                    }

                    if (event.getTouchCount() == 2) {
                        TouchPoint other = event.getTouchPoints().get(0);
                        if (other.getId() == event.getTouchPoint().getId()) {
                            other = event.getTouchPoints().get(1);
                        }

                        if (other.getState() == TouchPoint.State.PRESSED
                                && !(other.belongsTo(TouchCircle.this))
                                && !(other.getTarget() instanceof TouchCircle)) {
                            ScaleTransition hide = new ScaleTransition(Duration.millis(100), TouchCircle.this);
                            hide.setToX(0.0001);
                            hide.setToY(0.0001);

                            TranslateTransition move = new TranslateTransition(Duration.ONE, TouchCircle.this);
                            move.setToX(other.getSceneX());
                            move.setToY(other.getSceneY());

                            move.setOnFinished(new EventHandler<ActionEvent>() {
                                @Override public void handle(ActionEvent event) {
                                    playing = false;
                                }
                            });

                            ScaleTransition show = new ScaleTransition(Duration.millis(100), TouchCircle.this);
                            show.setToX(1);
                            show.setToY(1);

                            SequentialTransition t = new SequentialTransition(hide, move, show);
                            playing = true;
                            t.play();

                            touchx = 0;
                            touchy = 0;
                            touchId = other.getId();

                            event.getTouchPoint().ungrab();
                            other.grab(TouchCircle.this);
                        }
                    }
                    event.consume();
                }
            });
        }
    }

    public static String info() {
        return
                "This application demonstrates independent usage of particular "
                + "touch points and grabbing changes.\n\n"
                + "Each of the circles can be dragged independenty "
                + "and simultaneously around the board.\n\n"
                + "When you touch only one circle and touch second finger "
                + "somewhere on the empty space, the touchced circle jumps "
                + "there and can be further dragged by the new finger, or "
                + "even jumped again.";
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
