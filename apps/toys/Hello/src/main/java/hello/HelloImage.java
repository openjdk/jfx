/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloImage extends Application {

    private static final String imageURL = "hello/JavaFX.png";
    //3301x4000 size of duke
    private static final String slowImageURL =
            "https://cr.openjdk.org/~jeff/Duke/png/Hips.png";
    private static final String animImageURL = "hello/animated_89_c.gif";
    private static final String animCursorURL = "hello/javafx-loading-32x32.gif";

    @Override public void start(Stage stage) {
        stage.setTitle("Hello Image");

        Scene scene = new Scene(new Group(), 600, 450);
        scene.setFill(Color.LIGHTGRAY);
        ObservableList<Node> seq = ((Group)scene.getRoot()).getChildren();

        Dimension2D d = ImageCursor.getBestSize(1,1);

        System.err.println("BestCursor Size ="+d);

        Image animImage = new Image(animImageURL);

        addImageToObservableList(seq, 160, 20, 420, 120, new Image(imageURL),
                           createImageCursor(animImageURL, 16, 16));
//        addImageToObservableList(seq, 20, 20, 120, 120, animImage,
//                           createImageCursor(animCursorURL, 0, 0));

        final Image slowImage = new Image(slowImageURL, true);
        addImageToObservableList(seq, 20, 160, 560, 250, slowImage, Cursor.CROSSHAIR);

        addImageToObservableList(seq, 20, 20, 120, 120, animImage,
                           createImageCursor(slowImageURL, 3301*0.2f, 0));

        stage.getIcons().add(slowImage);
        stage.setScene(scene);
        stage.show();
    }

    private static void addImageToObservableList(ObservableList<Node> seq,
                                           int x, int y,
                                           int w, int h,
                                           Image image,
                                           Cursor cursor) {
        ImageView imageView = new ImageView();
        imageView.setX(x);
        imageView.setY(y);
        imageView.setFitWidth(w);
        imageView.setFitHeight(h);
        imageView.setPreserveRatio(true);
        imageView.setImage(image);
        imageView.setCursor(cursor);
        seq.add(imageView);
    }

    private static Cursor createImageCursor(final String url,
                                            final float hotspotX,
                                            final float hotspotY) {

        final Image cursorImage = new Image(url, 32, 32, false, true, true);
        return new ImageCursor(cursorImage, hotspotX, hotspotY);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
