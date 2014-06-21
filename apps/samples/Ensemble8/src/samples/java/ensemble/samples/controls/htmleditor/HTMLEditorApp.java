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
package ensemble.samples.controls.htmleditor;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;

/**
 * A sample that demonstrates the HTML Editor. You can make changes to the
 * example text, and the resulting generated HTML is displayed.
 *
 * @sampleName HTML Editor
 * @preview preview.png
 * @see javafx.scene.web.HTMLEditor
 * @see javafx.scene.control.ScrollPane
 * @see javafx.scene.control.ScrollPane.ScrollBarPolicy
 * @see javafx.scene.control.Button
 * @see javafx.event.ActionEvent
 * @see javafx.event.EventHandler
 * @see javafx.geometry.Pos
 * @see javafx.scene.control.Label
 * @see javafx.scene.layout.VBox
 * @related /Controls/Text/Simple Label
 * @related /Controls/WebView
 * @conditionalFeatures WEB
 */
public class HTMLEditorApp extends Application {

    private HTMLEditor htmlEditor = null;
    private final String INITIAL_TEXT = "<html><body>Lorem ipsum dolor sit "
            + "amet, consectetur adipiscing elit."
            + "Nam tortor felis, pulvinar in scelerisque cursus, pulvinar "
            + "at ante. Nulla consequat "
            + "congue lectus in sodales. </body></html> ";

    public Parent createContent() {

        htmlEditor = new HTMLEditor();
        htmlEditor.setHtmlText(INITIAL_TEXT);

        ScrollPane htmlSP = new ScrollPane();
        htmlSP.setFitToWidth(true);
        htmlSP.setPrefWidth(htmlEditor.prefWidth(-1)); // Workaround of RT-21495
        htmlSP.setPrefHeight(245);
        htmlSP.setVbarPolicy(ScrollBarPolicy.NEVER);
        htmlSP.setContent(htmlEditor);

        final Label htmlLabel = new Label();
        htmlLabel.setWrapText(true);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("noborder-scroll-pane");
        scrollPane.setContent(htmlLabel);
        scrollPane.setFitToWidth(true);

        Button showHTMLButton = new Button("Show the HTML below");
        showHTMLButton.setOnAction((ActionEvent arg0) -> {
            htmlLabel.setText(htmlEditor.getHtmlText());
        });

        VBox vRoot = new VBox();
        vRoot.setAlignment(Pos.CENTER);
        vRoot.setSpacing(5);
        vRoot.getChildren().addAll(htmlSP, showHTMLButton, scrollPane);

        return vRoot;
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
