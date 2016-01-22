/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.samples.graphics2d.images.imageproperties;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * A sample that demonstrates how to resize images and use the Viewport
 * property.
 *
 * @sampleName Image Properties
 * @preview preview.png
 * @related /Graphics 2d/Images/Image Creation
 * @see javafx.scene.image.Image
 * @see javafx.scene.image.ImageView
 * @embedded
 */
public class ImagePropertiesApp extends Application {

    private static final String url = ImagePropertiesApp.class.getResource("/ensemble/samples/shared-resources/sanfran.jpg").toString();

    public Parent createContent() {
        //we can set image properties directly during creation
        ImageView sample1 = new ImageView(new Image(url, 30, 70, false, true));

        ImageView sample2 = new ImageView(new Image(url));
        //image can be resized to preferred width
        sample2.setFitWidth(200);
        sample2.setPreserveRatio(true);

        ImageView sample3 = new ImageView(new Image(url));
        //image can be resized to preferred height
        sample3.setFitHeight(20);
        sample3.setPreserveRatio(true);

        ImageView sample4 = new ImageView(new Image(url));
        //one can resize image without preserving ratio between height and width
        sample4.setFitWidth(40);
        sample4.setFitHeight(80);
        sample4.setPreserveRatio(false);
        sample4.setSmooth(true); //the usage of the better filter

        ImageView sample5 = new ImageView(new Image(url));
        sample5.setFitHeight(60);
        sample5.setPreserveRatio(true);
        //viewport is used for displaying the part of image
        Rectangle2D rectangle2D = new Rectangle2D(50, 200, 120, 60);
        sample5.setViewport(rectangle2D);
        //add the imageviews to layout
        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.getChildren().addAll(sample1, sample3, sample4, sample5);
        //show the layout
        VBox vb = new VBox(10);
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll(hBox, sample2);
        vb.setMinSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        vb.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);

        return vb;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
