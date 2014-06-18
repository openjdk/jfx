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
package ensemble.samples.layout.flowpane;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

/**
 * A simple example of a FlowPane layout.
 *
 * @sampleName FlowPane
 * @preview preview.png
 * @see javafx.scene.layout.FlowPane
 * @related /Graphics 2d/Images/Image Creation
 * @embedded
 */
public class FlowPaneApp extends Application {

    private static final Image ICON_48 = new Image(FlowPaneApp.class.getResourceAsStream("/ensemble/samples/shared-resources/icon-48x48.png"));
    private static final Image ICON_68 = new Image(FlowPaneApp.class.getResourceAsStream("/ensemble/samples/shared-resources/icon-68x68.png"));
    private static final Image ICON_88 = new Image(FlowPaneApp.class.getResourceAsStream("/ensemble/samples/shared-resources/icon-88x88.png"));
    private static int ITEMS = 3;

    public Parent createContent() {
        FlowPane flowPane = new FlowPane(Orientation.HORIZONTAL, 4, 2);
        flowPane.setPrefWrapLength(240); //preferred wraplength

        ImageView[] imageViews48 = new ImageView[ITEMS];
        ImageView[] imageViews68 = new ImageView[ITEMS];
        ImageView[] imageViews88 = new ImageView[ITEMS];

        for (int i = 0; i < ITEMS; i++) {
            imageViews48[i] = new ImageView(ICON_48);
            imageViews68[i] = new ImageView(ICON_68);
            imageViews88[i] = new ImageView(ICON_88);
            flowPane.getChildren().addAll(imageViews48[i], imageViews68[i], imageViews88[i]);
        }
        return flowPane;
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
