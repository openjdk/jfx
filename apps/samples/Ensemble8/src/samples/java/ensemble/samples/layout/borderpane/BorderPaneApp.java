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
package ensemble.samples.layout.borderpane;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * An example of  a BorderPane layout, with placement of children in the top,
 * left, center, right, and bottom positions.
 *
 * @sampleName BorderPane
 * @preview preview.png
 * @see javafx.scene.layout.BorderPane
 * @related /Controls/Text/Simple Label
 * @related /Graphics 2d/Images/Image Creation
 * @embedded
 */
public class BorderPaneApp extends Application {

    private static final Image ICON_48 = new Image(BorderPaneApp.class.getResourceAsStream("/ensemble/samples/shared-resources/icon-48x48.png"));

    public Parent createContent() {
         BorderPane borderPane = new BorderPane();

        //Top content
        ToolBar toolbar = new ToolBar();
        toolbar.getItems().add(new Button("Home"));
        toolbar.getItems().add(new Button("Options"));
        toolbar.getItems().add(new Button("Help"));
        borderPane.setTop(toolbar);

        //Left content
        Label label1 = new Label("Left hand");
        Button leftButton = new Button("left");
        VBox leftVbox = new VBox();
        leftVbox.getChildren().addAll(label1, leftButton);
        borderPane.setLeft(leftVbox);

        //Right content
        Label rightlabel1 = new Label("Right hand");
        Button rightButton = new Button("right");

        VBox rightVbox = new VBox();
        rightVbox.getChildren().addAll(rightlabel1, rightButton);
        borderPane.setRight(rightVbox);

        //Center content
        Label centerLabel = new Label("Center area.");
        centerLabel.setWrapText(true);
        ImageView imageView = new ImageView(ICON_48);

        //Using AnchorPane only to position items in the center
        AnchorPane centerAP = new AnchorPane();
        AnchorPane.setTopAnchor(centerLabel, Double.valueOf(5));
        AnchorPane.setLeftAnchor(centerLabel, Double.valueOf(20));
        AnchorPane.setTopAnchor(imageView, Double.valueOf(40));
        AnchorPane.setLeftAnchor(imageView, Double.valueOf(30));
        centerAP.getChildren().addAll(centerLabel, imageView);
        borderPane.setCenter(centerAP);

        //Bottom content
        Label bottomLabel = new Label("At the bottom.");
        borderPane.setBottom(bottomLabel);
        return borderPane;
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
