/* 
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved. 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER. 
 * 
 * This code is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License version 2 only, as 
 * published by the Free Software Foundation.  Oracle designates this 
 * particular file as subject to the "Classpath" exception as provided 
 * by Oracle in the LICENSE file that accompanied this code. * 
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

package fx83dfeatures;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;

public class RetinaDisplayTest extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("RetinaDisplayLightBug");

        PhongMaterial material = new PhongMaterial();
        material.setSpecularColor(Color.AQUA);
        material.setSpecularPower(1.5);

        final Sphere sphere = new Sphere(150);
        sphere.setMaterial(material);

        final Group parent = new Group(sphere);
        parent.setTranslateX(200);
        parent.setTranslateY(200);

        final Group root = new Group();
        root.getChildren().add(parent);

        final Scene scene = new Scene(root, 400, 400, true);
        scene.setFill(Color.BLACK);

        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(200);
        pointLight.setTranslateY(200);
        pointLight.setTranslateZ(-1500);

        scene.setCamera(new PerspectiveCamera(false));

        root.getChildren().addAll(pointLight);

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}

