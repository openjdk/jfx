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

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MovingStones extends Application {

    long start = -1;
    long end = -1;
    Line line;
    long id1, id2;

    @Override public void start(Stage stage) {
        stage.setTitle("Moving Stones");

        final Group root = new Group();
        final Scene scene = new Scene(root, 500, 500);
        scene.setFill(Color.LIGHTBLUE);

        root.getChildren().addAll(
                new Stone(Math.random() * 150 + 50, Math.random() * 150 + 50, Color.RED),
                new Stone(Math.random() * 150 + 300, Math.random() * 150 + 50, Color.GREEN),
                new Stone(Math.random() * 150 + 50, Math.random() * 150 + 300, Color.BLUE),
                new Stone(Math.random() * 150 + 300, Math.random() * 150 + 300, Color.BLACK));

        stage.setScene(scene);
        stage.show();
    }

    private static class Stone extends Circle {

        public Stone(double x, double y, Color color) {
            super(50, color);
            setTranslateX(x);
            setTranslateY(y);

            addEventHandler(TouchEvent.ANY, new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent event) {
                    event.getTouchPoint().ungrab();

                    if (event.getTouchCount() != 2) {
                        return;
                    }

                    TouchPoint mine = event.getTouchPoint();
                    TouchPoint other = event.getTouchPoints().get(0);
                    if (other == mine) {
                        other = event.getTouchPoints().get(1);
                    }

                    if (other.belongsTo(Stone.this)) {
                        return;
                    }

                    if (other.getState() == TouchPoint.State.PRESSED
                            || mine.getState() == TouchPoint.State.PRESSED) {

                        other.ungrab();

                        TranslateTransition t =
                                new TranslateTransition(Duration.millis(200), Stone.this);
                        t.setToX(other.getSceneX());
                        t.setToY(other.getSceneY());
                        t.play();
                    }
                    event.consume();
                }
            });
        }
    }

    public static String info() {
        return
                "This application demonstrates cooperating between nodes on "
                + "multi-touch gestures. You can press a circle, hold it, and "
                + "tap somewhere on the scene, the node will jump there. "
                + "You can also press a circle, hold it, and tap a different "
                + "circle, in this case the two circles switch their positions.";
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
