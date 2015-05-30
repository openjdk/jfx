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

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ImageBoard extends Application {

    @Override public void start(Stage stage) {
        Group root = new Group();
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Image Board");
        stage.setScene(scene);
        scene.setFill(Color.SILVER);
        stage.show();

        for (int i=1; i<=10; i++) {
            Image img = new Image(ImageBoard.class.getResource(
                    "images/flower_" + i + ".jpg").toExternalForm(), false);
            MultiTouchImageView view = new MultiTouchImageView(img);
            double w = view.getBoundsInParent().getWidth();
            double h = view.getBoundsInParent().getHeight();
            view.setTranslateX((scene.getWidth()-w)*Math.random());
            view.setTranslateY((scene.getHeight()-h)*Math.random());
            view.setRotate(-20+(40*Math.random()));
            root.getChildren().add(view);
        }
    }


    public static class MultiTouchImageView extends ImageView {
        private int eventSet = -1;
        private int touchPointCounter = 0;
        private TouchPoint firstTouchPoint;
        private double rotateAnchor, scaleAnchor;
        private Point2D translateAnchor;
        private boolean needsReset = true;
        private boolean singleFinger;

        public MultiTouchImageView(Image img) {
            super(img);
            setSmooth(true);

            addEventHandler(TouchEvent.ANY, new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent event) {
                    final TouchPoint touchPoint = event.getTouchPoint();

                    if (eventSet == event.getEventSetId()) {
                        touchPointCounter++;
                    } else {
                        eventSet = event.getEventSetId();
                        touchPointCounter = 0;
                    }

                    switch(touchPointCounter) {
                    case 0:
                        // single-finger dragging
                        if (event.getEventType() == TouchEvent.TOUCH_PRESSED) {
                            singleFinger = true;

                            translateAnchor = new Point2D(
                                    getTranslateX() - touchPoint.getSceneX(),
                                    getTranslateY() - touchPoint.getSceneY());

                            toFront();
                        } else if (singleFinger) {
                            setTranslateX(translateAnchor.getX() + touchPoint.getSceneX());
                            setTranslateY(translateAnchor.getY() + touchPoint.getSceneY());
                        }

                        firstTouchPoint = touchPoint;
                        break;
                    case 1:
                        // two-finger gestures
                        double distance = getDistance(firstTouchPoint, touchPoint);
                        double angle = getAngle(firstTouchPoint, touchPoint);
                        Point2D center = getCenter(firstTouchPoint, touchPoint);

                        if (needsReset) {
                            singleFinger = false;
                            needsReset = false;
                            rotateAnchor = getRotate() - angle;
                            scaleAnchor = getScaleX() / distance;
                            translateAnchor = new Point2D(
                                    getTranslateX() - center.getX(),
                                    getTranslateY() - center.getY());
                        } else {
                            setRotate(rotateAnchor + angle);
                            setScaleX(scaleAnchor * distance);
                            setScaleY(scaleAnchor * distance);
                            setTranslateX(translateAnchor.getX() + center.getX());
                            setTranslateY(translateAnchor.getY() + center.getY());
                        }
                    }
                }
            });

            setOnTouchReleased(new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent event) {
                    needsReset = true;
                }
            });
            setOnTouchPressed(new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent event) {
                    needsReset = true;
                }
            });
        }

        private double getAngle(TouchPoint tp1, TouchPoint tp2) {
            return Math.toDegrees(Math.atan2(
                    tp2.getSceneY() - tp1.getSceneY(),
                    tp2.getSceneX() - tp1.getSceneX()));
        }

        private double getDistance(TouchPoint tp1, TouchPoint tp2) {
            final double dx = tp2.getSceneX() - tp1.getSceneX();
            final double dy = tp2.getSceneY() - tp1.getSceneY();
            return Math.sqrt((dx*dx) + (dy*dy));
        }

        private Point2D getCenter(TouchPoint tp1, TouchPoint tp2) {
            return new Point2D(
                    tp1.getSceneX() + (tp2.getSceneX() - tp1.getSceneX()) / 2,
                    tp1.getSceneY() + (tp2.getSceneY() - tp1.getSceneY()) / 2);
        }
    }

    public static String info() {
        return
                "This is a simple image board application. "
                + "You can drag the images around, zoom and rotate them, "
                + "all of that simultaneously on more images.";
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
