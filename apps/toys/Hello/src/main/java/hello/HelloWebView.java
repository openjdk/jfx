/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package hello;

import java.util.List;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * Simple WebView application.
 */
public class HelloWebView extends Application {

    private static final String DEFAULT_URL = "http://www.oracle.com/java/";

    @Override
    public void start(Stage stage) {
        List<String> args = getParameters().getRaw();
        final String initialURL = args.size() > 0 ? args.get(0) : DEFAULT_URL;

        final WebView webView = new WebView();
        final WebEngine webEngine = webView.getEngine();

        final TextField urlBox = new TextField();
        urlBox.setText(initialURL);
        HBox.setHgrow(urlBox, Priority.ALWAYS);
        urlBox.setOnAction(e -> webEngine.load(urlBox.getText()));

        Button goButton = new Button("Go");
        goButton.setOnAction(e -> webEngine.load(urlBox.getText()));

        HBox naviBar = new HBox();
        naviBar.getChildren().addAll(urlBox, goButton);

        BorderPane root = new BorderPane();
        root.setTop(naviBar);
        root.setCenter(webView);

        webEngine.locationProperty().addListener((obs, oVal, nVal)
                -> urlBox.setText(nVal));

        webEngine.load(initialURL);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        SimpleStringProperty titleProp = new SimpleStringProperty("HelloWebView: ");
        stage.titleProperty().bind(titleProp.concat(urlBox.textProperty()));
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
