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
package ensemble.samples.scenegraph.stage;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.Lighting;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * A sample with a control that creates a decorated stage that is centered on
 * your desktop.
 *
 * @sampleName Stage
 * @preview preview.png
 * @see javafx.stage.Stage
 * @see javafx.scene.Scene
 * @related /Scenegraph/Advanced Stage
 */
public class StageApp extends Application {

    public Parent createContent() {

        //create a button for initializing our new stage
        Button button = new Button("Create a Stage");
        button.setStyle("-fx-font-size: 24;");
        button.setDefaultButton(true);
        button.setOnAction((ActionEvent t) -> {
            final Stage stage = new Stage();

            //create root node of scene, i.e. group
            Group rootGroup = new Group();

            //create scene with set width, height and color
            Scene scene = new Scene(rootGroup, 200, 200, Color.WHITESMOKE);

            //set scene to stage
            stage.setScene(scene);

            //set title to stage
            stage.setTitle("New stage");

            //center stage on screen
            stage.centerOnScreen();

            //show the stage
            stage.show();

            //add some node to scene
            Text text = new Text(20, 110, "JavaFX");
            text.setFill(Color.DODGERBLUE);
            text.setEffect(new Lighting());
            text.setFont(Font.font(Font.getDefault().getFamily(), 50));

            //add text to the main root group
            rootGroup.getChildren().add(text);
        });
        return button;
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
