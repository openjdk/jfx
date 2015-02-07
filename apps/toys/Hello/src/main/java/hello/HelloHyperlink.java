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
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloHyperlink extends Application {

    private static final String animImageURL = "hello/animated_89_c.gif";

    public static void main(String[] args) {
        Application.launch(args);
    }


    @Override public void start(Stage stage) {
        stage.setTitle("Hello Hyperlink");
        Scene scene = new Scene(new Group(), 600, 450);
        scene.setFill(Color.GHOSTWHITE);

        Hyperlink link = new Hyperlink();
        link.setLayoutX(25);
        link.setLayoutY(40);
        link.setText("I am a hyperlink!");
        ((Group)scene.getRoot()).getChildren().add(link);

        Hyperlink animatedLink = new Hyperlink();
        animatedLink.setLayoutX(25);
        animatedLink.setLayoutY(100);
        animatedLink.setText("I am a hyperlink with an animated Image!");
        animatedLink.setGraphic(imageView(animImageURL, 0, 0, 16, 16));
        ((Group)scene.getRoot()).getChildren().add(animatedLink);

        stage.setScene(scene);
        stage.show();
    }

    private static ImageView imageView(String url, int x, int y, int w, int h) {
        ImageView imageView = new ImageView();
        imageView.setX(x);
        imageView.setY(y);
        imageView.setFitWidth(w);
        imageView.setFitHeight(h);
        imageView.setPreserveRatio(true);
        imageView.setImage(new Image(url));
        return imageView;
    }
}
