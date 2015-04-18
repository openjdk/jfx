/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package picktest;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

public class PickTestFixedEye extends Application {

    double ax, ay;

    @Override public void start(Stage stage) {
        stage.setTitle("Pick test fixed eye");

        Group root = new Group();
        Scene scene = new Scene(root, 600, 450);
        scene.setFill(Color.LIGHTGREEN);

        final PerspectiveCamera cam = new PerspectiveCamera(true);
        scene.setCamera(cam);

        PhongMaterial pm = new PhongMaterial();
        pm.setDiffuseColor(Color.RED);
        pm.setSpecularColor(Color.ORANGE);

        final Box b = new Box(20, 20, 20);
        b.setMaterial(pm);
        b.setTranslateZ(70);
        b.setRotationAxis(Rotate.Y_AXIS);
        b.setRotate(25);
        root.getChildren().add(b);

        scene.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                ay = event.getSceneY();
            }
        });
        
        scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                b.setTranslateZ(b.getTranslateZ() + (event.getSceneY() - ay) / 3);
                ay = event.getSceneY();
            }
        });

        b.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                ((PhongMaterial) b.getMaterial()).setDiffuseColor(Color.YELLOW);
                ((PhongMaterial) b.getMaterial()).setSpecularColor(Color.WHITE);
            }
        });

        b.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                ((PhongMaterial) b.getMaterial()).setDiffuseColor(Color.RED);
                ((PhongMaterial) b.getMaterial()).setSpecularColor(Color.ORANGE);
            }
        });

        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
