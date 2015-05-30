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
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class ConsumeTouches extends Application {

    long start = -1;
    long end = -1;
    Line line;
    long id1, id2;

    @Override public void start(Stage stage) {
        stage.setTitle("Consume Touches");

        final Group root = new Group();
        final Scene scene = new Scene(root, 500, 200);

        final Rectangle bg = new Rectangle(500, 200, Color.LIGHTYELLOW);

        final Circle circle = new Circle(50, Color.GREEN);
        circle.setTranslateX(100);
        circle.setTranslateY(100);

        final Line axis = new Line(0, 100, 500, 100);

        final Line hand = new Line(250, 0, 250, 200);
        hand.setStroke(Color.RED);
        hand.setStrokeWidth(3);

        root.getChildren().addAll(bg, axis, hand, circle);

        root.addEventHandler(TouchEvent.ANY, new EventHandler<TouchEvent>() {
            @Override public void handle(TouchEvent event) {
                for (TouchPoint tp : event.getTouchPoints()) {
                    hand.setStartX(tp.getSceneX());
                    hand.setEndX(tp.getSceneX());
                }
            }
        });

        circle.addEventHandler(TouchEvent.ANY, new EventHandler<TouchEvent>() {
            @Override public void handle(TouchEvent event) {
                circle.setTranslateX(event.getTouchPoint().getSceneX());
                event.consume();
            }
        });

        stage.setScene(scene);
        stage.show();
    }

    public static String info() {
        return
                "This application demonstrates touch event consuming. When "
                + "you touch the scene anywhere, the red line updates its "
                + "position. But when you touch the circle, you can move it "
                + "around, it consumes the events so they don't cause the "
                + "red line to change its position.";
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
