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
package ensemble.samples.controls.pagination;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * A sample that demonstrates pagination.
 *
 * @sampleName Pagination
 * @preview preview.png
 * @docUrl http://www.oracle.com/pls/topic/lookup?ctx=javase80&id=JFXUI336 Using JavaFX UI Controls
 * @see javafx.scene.control.Button
 * @see javafx.scene.control.Pagination
 * @see javafx.scene.image.ImageView
 * @embedded
 *
 * @related /Graphics 2d/Display Shelf
 * @related /Graphics 2d/Images/Image Creation
 * @related /Controls/Button/Pill Button
 */
public class PaginationApp extends Application {

    private static final Image[] images = new Image[7];
    private static final String[] urls = {
        "/ensemble/samples/shared-resources/Animal1.jpg",
        "/ensemble/samples/shared-resources/Animal2.jpg",
        "/ensemble/samples/shared-resources/Animal3.jpg",
        "/ensemble/samples/shared-resources/Animal4.jpg",
        "/ensemble/samples/shared-resources/Animal5.jpg",
        "/ensemble/samples/shared-resources/Animal6.jpg",
        "/ensemble/samples/shared-resources/Animal7.jpg"
    };
    private Pagination pagination;

    public Parent createContent() {
        VBox outerBox = new VBox();
        outerBox.setAlignment(Pos.CENTER);
        // Images for our pages
        for (int i = 0; i < images.length; i++) {
            String url = getClass().getResource(urls[i]).toExternalForm();
            images[i] = new Image(url, false);
        }

        pagination = new Pagination(7);
        pagination.setPageFactory((Integer pageIndex) ->
                                  createAnimalPage(pageIndex));
        // Style can be numeric page indicators or bullet indicators
        Button styleButton = new Button("Toggle pagination style");
        styleButton.setOnAction((ActionEvent me) -> {
            ObservableList<String> styleClass = pagination.getStyleClass();
            if (!styleClass.contains(Pagination.STYLE_CLASS_BULLET)) {
                styleClass.add(Pagination.STYLE_CLASS_BULLET);
            } else {
                styleClass.remove(Pagination.STYLE_CLASS_BULLET);
            }
        });

        outerBox.getChildren().addAll(pagination, styleButton);
        return outerBox;
    }
    // Creates the content for a single page
    private VBox createAnimalPage(int pageIndex) {
        VBox box = new VBox();
        ImageView iv = new ImageView(images[pageIndex]);
        box.setAlignment(Pos.CENTER);
        Label desc = new Label("PAGE " + (pageIndex + 1));
        box.getChildren().addAll(iv, desc);
        return box;
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
