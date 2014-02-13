/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.SnapshotResult;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.util.Callback;

public class HelloSnapshot2 extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("HelloSnapshot2");

        Group g = new Group();
        Ellipse ellipse = new Ellipse(25, 20);
        ellipse.setTranslateX(25);
        ellipse.setTranslateY(25);
        ellipse.setFill(Color.PALEGREEN);
        g.getChildren().add(ellipse);
        Group root = new Group(g);
//        root.getTransforms().add(new Rotate(30));
//        root.setRotate(30);

        final Scene ellipseScene = new Scene(root);
        ellipseScene.setFill(Color.DARKBLUE);
        SnapshotParameters params = new SnapshotParameters();
        params.setCamera(new PerspectiveCamera());
        params.setFill(Color.DARKBLUE);
        params.setTransform(Transform.rotate(30, 25, 25));
//        params.setViewport(new Rectangle2D(20, 15, 100, 75));

        final Image image2 = ellipse.snapshot(params, null);
        final Image image1 = ellipseScene.snapshot(null);

        Scene scene = new Scene(new Group(), 400, 300);
        scene.setFill(Color.BROWN);

        final HBox container = new HBox();
        container.getChildren().add(new ImageView(image1));
        container.getChildren().add(new ImageView(image2));

        ellipse.snapshot(new Callback<SnapshotResult, Void>() {
            public Void call(SnapshotResult r) {
                System.err.println("callback: image = " + r.getImage()
                        + "  source = " + r.getSource()
                        + "  params = " + r.getSnapshotParameters());
                container.getChildren().add(new ImageView(r.getImage()));
                return null;
            }
        }, params, null);

        params.setFill(Color.YELLOW);  // this should not affect the rendering
        ellipse.setStroke(Color.RED); // this should
        ellipse.setStrokeWidth(3);

        scene.setRoot(container);
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        javafx.application.Application.launch(args);
    }

}
