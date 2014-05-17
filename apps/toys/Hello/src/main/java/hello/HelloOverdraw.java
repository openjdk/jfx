/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Has some rectangles with the intention of showing dirty regions and overdraw. Set these:
 * -Djavafx.pulseLogger=true
 * -Dprism.showoverdraw=true
 * -Dprism.printrendergraph=true
 * -Djavafx.pulseLogger.threshold=0
 */
public class HelloOverdraw extends Application {
    @Override public void start(Stage stage) {
        stage.setTitle("Hello Overdraw");

        Rectangle clipped = createRectangle("clipped", 400, 50, 100, 100, Color.LIGHTGRAY);
        Rectangle clip = new Rectangle(400, 50, 50, 50);
        clipped.setClip(clip);

        final Rectangle russianDolls0 = createRectangle("russianDoll0", 300, 200, 200, 200, Color.LIGHTGRAY);
        final Rectangle russianDolls1 = createRectangle("russianDoll1", 310, 210, 180, 180, Color.LIGHTGRAY.darker());
        final Rectangle russianDolls2 = createRectangle("russianDoll2", 320, 220, 160, 160, Color.LIGHTGRAY.darker().darker());
        final Rectangle russianDolls3 = createRectangle("russianDoll3", 330, 230, 140, 140, Color.LIGHTGRAY.darker().darker().darker());
        final Rectangle russianDolls4 = createRectangle("russianDoll4", 340, 240, 120, 120, Color.LIGHTGRAY.darker().darker().darker().darker());
        final Rectangle russianDolls5 = createRectangle("russianDoll5", 350, 250, 100, 100, Color.LIGHTGRAY.darker().darker().darker().darker().darker());
        final Rectangle russianDolls6 = createRectangle("russianDoll6", 360, 260, 80, 80, Color.LIGHTGRAY.darker().darker().darker().darker().darker().darker());
        final Rectangle russianDolls7 = createRectangle("russianDoll7", 370, 270, 60, 60, Color.LIGHTGRAY.darker().darker().darker().darker().darker().darker().darker());

        Group root = new Group(
                clipped,
                russianDolls0,
                russianDolls1,
                russianDolls2,
                russianDolls3,
                russianDolls4,
                russianDolls5,
                russianDolls6,
                russianDolls7,
                createRectangle("bottom", 50, 50, 100, 100, Color.LIGHTGRAY),
                createRectangle("top", 100, 100, 100, 100, Color.LIGHTGRAY),
                createRectangle("loner", 100, 300, 100, 100, Color.LIGHTGRAY));
        Scene scene = new Scene(root, 600, 450);
        scene.setCamera(new PerspectiveCamera());
        stage.setScene(scene);
        stage.show();

        scene.setOnKeyReleased(event -> {
            switch (event.getCode()) {
                case DIGIT0: toggle(russianDolls0); break;
                case DIGIT1: toggle(russianDolls1); break;
                case DIGIT2: toggle(russianDolls2); break;
                case DIGIT3: toggle(russianDolls3); break;
                case DIGIT4: toggle(russianDolls4); break;
                case DIGIT5: toggle(russianDolls5); break;
                case DIGIT6: toggle(russianDolls6); break;
                case DIGIT7: toggle(russianDolls7); break;
            }
        });
    }

    private void toggle(Rectangle r) {
        if (r.getArcHeight() == 0) {
            round(r);
        } else {
            squareUp(r);
        }
    }

    private void rotate(Rectangle r) {
        RotateTransition tx = new RotateTransition(Duration.seconds(10), r);
        tx.setFromAngle(0);
        tx.setToAngle(360);
        tx.setAxis(Rotate.Z_AXIS);
        tx.play();
    }

    private void rotate3D(Rectangle r) {
        RotateTransition tx = new RotateTransition(Duration.seconds(10), r);
        tx.setFromAngle(0);
        tx.setToAngle(360);
        tx.setAxis(Rotate.Y_AXIS);
        tx.play();
    }

    private void squareUp(Rectangle r) {
        r.setArcHeight(0);
        r.setArcWidth(0);
    }

    private void round(Rectangle r) {
        r.setArcHeight(20);
        r.setArcWidth(20);
    }

    private Rectangle createRectangle(String id, int x, int y, int w, int h, Color c) {
        final Rectangle r = new Rectangle(x, y, w, h);
        r.setId(id);
        r.setArcHeight(20);
        r.setArcWidth(20);
        r.setFill(c);
        r.setOnMouseEntered(event -> squareUp(r));
        r.setOnMouseExited(event -> round(r));
        r.setOnMouseClicked(event -> {
            if (event.isMetaDown()) {
                rotate3D(r);
            } else {
                rotate(r);
            }
        });
        return r;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
