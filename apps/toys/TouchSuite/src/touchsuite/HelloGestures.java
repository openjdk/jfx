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

import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.effect.Lighting;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

public class HelloGestures extends Application {

    private Rectangle rect;
    private Group swipeRotator;
    private int gestureCount;
    private boolean rectScrolled = false;
    private boolean playing = false;
    private Scene scene;

    @Override public void start(Stage stage) {
        stage.setTitle("Hello Gestures");

        Group root = new Group();
        scene = new Scene(root, 600, 600);
        scene.setFill(Color.LIGHTGREEN);

        rect = new Rectangle(200, 200, 200, 200);
        rect.setFill(Color.RED);

        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                rect.setTranslateX(rect.getTranslateX() + event.getDeltaX());
                rect.setTranslateY(rect.getTranslateY() + event.getDeltaY());
                event.consume();
            }
        });

        rect.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                rect.setScaleX(rect.getScaleX() * event.getZoomFactor());
                rect.setScaleY(rect.getScaleY() * event.getZoomFactor());
                event.consume();
            }
        });

        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rect.setRotate(rect.getRotate() + event.getAngle());
                event.consume();
            }
        });

        rect.setOnScrollStarted(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                inc();
                event.consume();
            }
        });

        rect.setOnScrollFinished(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                dec();
                event.consume();
            }
        });

        rect.setOnZoomStarted(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                inc();
                event.consume();
            }
        });

        rect.setOnZoomFinished(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                dec();
                event.consume();
            }
        });

        rect.setOnRotationStarted(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                inc();
                event.consume();
            }
        });

        rect.setOnRotationFinished(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                dec();
                event.consume();
            }
        });

        rect.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                event.consume();
            }
        });

        scene.addEventFilter(ScrollEvent.SCROLL, new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                // react only on swipes that didn't move the rectangle
                rectScrolled = event.isDirect() && event.getTarget() == rect;
            }
        });

        rect.setOnSwipeLeft(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                if (!rectScrolled) {
                    rotate3d(event.getTouchCount(), Rotate.Y_AXIS);
                    event.consume();
                }
            }
        });

        rect.setOnSwipeRight(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                if (!rectScrolled) {
                    rotate3d(-event.getTouchCount(), Rotate.Y_AXIS);
                    event.consume();
                }
            }
        });

        rect.setOnSwipeUp(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                if (!rectScrolled) {
                    rotate3d(-event.getTouchCount(), Rotate.X_AXIS);
                    event.consume();
                }
            }
        });

        rect.setOnSwipeDown(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                if (!rectScrolled) {
                    rotate3d(event.getTouchCount(), Rotate.X_AXIS);
                    event.consume();
                }
            }
        });

        scene.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (event.isStillSincePress()) {
                    rect.setTranslateX(0);
                    rect.setTranslateY(0);
                    rect.setRotate(0);
                    rect.setScaleX(1);
                    rect.setScaleY(1);
                }
            }
        });

        swipeRotator = new Group(rect);
        root.getChildren().addAll(swipeRotator);
        stage.setScene(scene);
        stage.show();
    }

    private void inc() {
        if (gestureCount == 0) {
            rect.setEffect(new Lighting());
        }
        gestureCount++;
    }

    private void dec() {
        gestureCount--;
        if (gestureCount == 0) {
            rect.setEffect(null);
        }
    }

    private void rotate3d(final int count, Point3D axis) {
        if (playing) {
            return;
        }

        playing = true;

        final RotateTransition rt = new RotateTransition(
                Duration.millis(Math.abs(500 * count)), swipeRotator);
        rt.setAxis(axis);
        rt.setFromAngle(0);
        rt.setToAngle(count * 180);

        rt.setOnFinished(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                playing = false;
                if (Math.abs(count) % 2 == 1) {
                    // flip the whole thing to avoid inverse rotation
                    swipeRotator.setRotate(0);
                    rect.setRotate(-rect.getRotate());
                }
                scene.setCamera(null);
            }
        });


        PerspectiveCamera pc = new PerspectiveCamera();
        pc.setFieldOfView(50);
        scene.setCamera(pc);

        rt.play();
    }

    public static String info() {
        return
                "This application demonstrates simple usage of common gestures. "
                + "The rectangle can be scrolled, rotated and zoomed, all "
                + "at the same time. Swipe over the rectangle rotates it in "
                + "the swipe direction as many times as many fingers were used "
                + "for the gesture. Tap the scene to return the square to its "
                + "original position.";
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
