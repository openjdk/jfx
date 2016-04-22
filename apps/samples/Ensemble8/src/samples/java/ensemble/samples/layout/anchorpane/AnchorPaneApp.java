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
package ensemble.samples.layout.anchorpane;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * A simple example of an AnchorPane layout.
 *
 * @sampleName AnchorPane
 * @preview preview.png
 * @docUrl http://docs.oracle.com/javase/8/javafx/layout-tutorial/index.html JavaFX Layouts
 * @see javafx.scene.control.Button
 * @see javafx.scene.control.Label
 * @see javafx.scene.layout.AnchorPane
 * @see javafx.scene.image.ImageView
 * @embedded
 *
 * @related /Layout/BorderPane
 * @related /Layout/FlowPane
 * @related /Layout/GridPane
 * @related /Layout/HBox
 * @related /Graphics 2d/Images/Image Creation
 * @related /Controls/Text/Simple Label
 * @related /Layout/StackPane
 * @related /Layout/TilePane
 * @related /Layout/VBox
 */
public class AnchorPaneApp extends Application {

    public Parent createContent() {
        AnchorPane anchorPane = new AnchorPane();

        Label label1 = new Label("We are all in an AnchorPane.");
        String IMAGE = "/ensemble/samples/shared-resources/icon-48x48.png";
        Image ICON_48 = new Image(getClass().getResourceAsStream(IMAGE));
        ImageView imageView = new ImageView(ICON_48);
        Button button1 = new Button("Submit");

        anchorPane.getChildren().addAll(label1, imageView, button1);

        AnchorPane.setTopAnchor(label1, Double.valueOf(2));
        AnchorPane.setLeftAnchor(label1, Double.valueOf(20));
        AnchorPane.setTopAnchor(button1, Double.valueOf(40));
        AnchorPane.setLeftAnchor(button1, Double.valueOf(20));
        AnchorPane.setTopAnchor(imageView, Double.valueOf(75));
        AnchorPane.setLeftAnchor(imageView, Double.valueOf(20));
        return anchorPane;
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
