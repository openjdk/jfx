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
package ensemble.samples.controls.button.colorbutton;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * An example of Buttons with different background colors.
 *
 * @sampleName Colored Buttons
 * @preview preview.png
 * @docUrl http://www.oracle.com/pls/topic/lookup?ctx=javase80&id=JFXUI336 Using JavaFX UI Controls
 * @see javafx.scene.control.Button
 * @see javafx.scene.paint.Color
 * @embedded
 *
 * @related /Controls/Button/Graphic Button
 * @related /Controls/Button/Pill Button
 */
public class ColorButtonApp extends Application {

    public Parent createContent() {
        HBox hBox = new HBox();
        hBox.setSpacing(5);
        for (int i = 0; i < 7; i++) {
            Button b = new Button("Color");
            b.setStyle(String.format("-fx-base: rgb(%d,%d,%d);",
                                     (10 * i), (20 * i), (10 * i)));
            hBox.getChildren().add(b);
        }
        HBox hBox2 = new HBox();
        hBox2.setSpacing(5);
        hBox2.setTranslateY(30);
        hBox2.getChildren().addAll(getButton("Red"), getButton("Orange"),
                                   getButton("Yellow"), getButton("Green"),
                                   getButton("Blue"), getButton("Indigo"),
                                   getButton("Violet"));

        VBox vBox = new VBox(20);
        vBox.getChildren().addAll(hBox, hBox2);
        vBox.setPrefHeight(140);
        vBox.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        vBox.setMinSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        return vBox;
    }

    protected Button getButton(String name) {
        Button button = new Button(name);
        button.setStyle(String.format("-fx-base: %s;", name.toLowerCase()));
        return button;
    }

    @Override public void start(Stage primaryStage) throws Exception {
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
