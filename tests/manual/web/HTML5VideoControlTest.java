/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class HTML5VideoControlTest extends Application {
    @Override
    public void start(Stage primaryStage) {
        WebView webView = new WebView();
        webView.getEngine().loadContent(
            "<video width=\"320\" height=\"240\" controls>\n" +
            "  <source src=\"https://download.oracle.com/otndocs/products/javafx/oow2010-2.mp4\">\n" +
            "  Your browser does not support the video tag.\n" +
            "</video>"
        );

        VBox root = new VBox();
        root.setSpacing(20);

        root.getChildren().addAll(createInstructionsBox(), webView);

        Scene scene = new Scene(root, 800, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("HTML5 Video Player with Instructions");
        primaryStage.show();
    }

    private VBox createInstructionsBox() {
        VBox instructionsBox = new VBox();
        instructionsBox.setStyle("-fx-background-color: #f0f0f0;");
        instructionsBox.setPrefWidth(400);

        instructionsBox.getChildren().addAll(
            new Label("Instructions:"),
            new Label("1. Click 'Play' to start the video."),
            new Label("2. The media controls should be visible once the video starts playing."),
            new Label("3. Use the media controls to play/pause/seek the video.")
        );

        return instructionsBox;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
