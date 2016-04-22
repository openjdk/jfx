/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates.
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
package ensemble.samples.controls.text.advancedlabel;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Several Label controls, displayed in various alignments with respect to an
 * image.
 *
 * @sampleName Advanced Label
 * @preview preview.png
 * @docUrl http://docs.oracle.com/javase/8/javafx/user-interface-tutorial/text.htm#JFXUI734 Using JavaFX Text
 * @see javafx.scene.control.ContentDisplay
 * @see javafx.scene.control.Label
 * @see javafx.scene.image.Image
 * @see javafx.scene.image.ImageView
 * @see javafx.scene.layout.VBox
 *
 * @related /Controls/Text/Bidi
 * @related /Controls/Text/Inset Text
 * @related /Controls/Button/Graphic Button
 * @related /Controls/Text/Search Box
 * @related /Controls/Text/Simple Label
 * @related /Controls/Text/Text Field
 * @related /Controls/Text/TextFlow
 * @related /Controls/Text/Text Formatter
 * @related /Controls/Text/Text Validator
 */
public class AdvancedLabelApp extends Application {

    public Parent createContent() {
        String URL = "/ensemble/samples/shared-resources/icon-48x48.png";
        Image ICON_48 = new Image(getClass().getResourceAsStream(URL));
        ImageView imageView = new ImageView(ICON_48);
        Label above = new Label("Image above", imageView);
        above.setContentDisplay(ContentDisplay.TOP);

        imageView = new ImageView(ICON_48);
        Label right = new Label("Image on the right", imageView);
        right.setContentDisplay(ContentDisplay.RIGHT);

        imageView = new ImageView(ICON_48);
        Label below = new Label("Image below", imageView);
        below.setContentDisplay(ContentDisplay.BOTTOM);

        imageView = new ImageView(ICON_48);
        Label left = new Label("Image on the left", imageView);
        left.setContentDisplay(ContentDisplay.LEFT);

        imageView = new ImageView(ICON_48);
        Label centered = new Label("Image centered", imageView);
        centered.setContentDisplay(ContentDisplay.CENTER);

        final VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(above, right, below, left, centered);
        return box;
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
