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
package ensemble.samples.graphics2d.displayshelf;

import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * A display shelf of images using the PerspectiveTransform effect.
 *
 * @sampleName Display Shelf
 * @preview preview.png
 * @see javafx.scene.effect.PerspectiveTransform
 * @see javafx.scene.effect.Reflection
 * @see javafx.scene.control.ScrollBar
 * @see javafx.scene.input.MouseEvent
 * @see javafx.scene.input.KeyEvent
 */
public class DisplayShelfApp extends Application {
    private static final double WIDTH = 450, HEIGHT = 480;
    private Timeline animation;

    public Parent createContent() {
         // load images
        Image[] images = new Image[14];
       images[0] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal1.jpg").toExternalForm(), false);
        images[1] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal2.jpg").toExternalForm(), false);
        images[2] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal3.jpg").toExternalForm(), false);
        images[3] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal4.jpg").toExternalForm(), false);
        images[4] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal5.jpg").toExternalForm(), false);
        images[5] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal6.jpg").toExternalForm(), false);
        images[6] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal7.jpg").toExternalForm(), false);
        images[7] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal8.jpg").toExternalForm(), false);
        images[8] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal9.jpg").toExternalForm(), false);
        images[9] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal10.jpg").toExternalForm(), false);
        images[10] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal11.jpg").toExternalForm(), false);
        images[11] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal12.jpg").toExternalForm(), false);
        images[12] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal13.jpg").toExternalForm(), false);
        images[13] = new Image(DisplayShelfApp.class.getResource("/ensemble/samples/shared-resources/Animal14.jpg").toExternalForm(), false);
        // create display shelf
        DisplayShelf displayShelf = new DisplayShelf(images);
        displayShelf.setPrefSize(WIDTH, HEIGHT);

        String displayShelfCss = DisplayShelfApp.class.getResource("DisplayShelf.css").toExternalForm();
        displayShelf.getStylesheets().add(displayShelfCss);
        return displayShelf;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setResizable(false);
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
