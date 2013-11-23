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
package fx83dfeatures;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.stage.Stage;

public class RepeatTextureTest extends Application {

    public static final float D = 200;
    final TriangleMesh mesh = new TriangleMesh();

    @Override
    public void start(Stage primaryStage) throws Exception {


        mesh.getPoints().setAll(new float[]{
            0, 0, 0,
            D, 0, 0,
            D, D, 0,
            0, D, 0,});
        mesh.getTexCoords().setAll(new float[]{
            0, 0,
            1, 0,
            1, 1,
            0, 1,});
        mesh.getFaces().setAll(new int[]{
            0, 0, 2, 2, 1, 1,
            0, 0, 3, 3, 2, 2,});

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(new Image("resources/cone-stripes.jpg"));

        MeshView meshView = new MeshView(mesh);
        meshView.setMaterial(material);

        Group root = new Group();
        root.getChildren().addAll(meshView);

        Scene scene = new Scene(root, D, D, true);
        scene.setOnKeyTyped(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent e) {
                switch (e.getCharacter()) {
                    case "1":
                        mesh.getTexCoords().setAll(new float[]{
                            0, 0,
                            1, 0,
                            1, 1,
                            0, 1,});
                        break;
                    case "2":
                        mesh.getTexCoords().setAll(new float[]{
                            0, 0,
                            2, 0,
                            2, 2,
                            0, 2,});
                        break;
                    case "3":
                        mesh.getTexCoords().setAll(new float[]{
                            -1, -1,
                            2, -1,
                            2, 2,
                            -1, 2,});
                        break;
                    case "4":
                        mesh.getTexCoords().setAll(new float[]{
                            -2, -2,
                            2, -2,
                            2, 2,
                            -2, 2,});
                        break;
                }
            }
        });

        primaryStage.setScene(scene);
        primaryStage.show();

    }

    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {
        launch(args);
    }
}
