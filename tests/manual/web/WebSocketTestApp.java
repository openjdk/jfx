/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.application.Platform;
import javafx.geometry.Insets;
import java.net.URL;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class WebSocketTestApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        VBox instructions =  new VBox(
                new Label(" This test is for manual websocket callback test , please follow below steps"),
                new Label(" "),
                new Label(" STEPS:"),
                new Label("  1. Click on RunTest button"),
                new Label(" "),
                new Label("  2. Expected behaviour: Data received from server message should appear on webview"));

        Button loadButton = new Button("RunTest");

        Button passButton = new Button("Pass");
        passButton.setOnAction(e -> {
            Platform.exit();
        });

        Button failButton = new Button("Fail");
        failButton.setOnAction(e -> {
            Platform.exit();
            throw new AssertionError("!Unable to receive message data from server, something is wrong");
        });

        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        loadButton.setOnAction(e -> {
            URL url = this.getClass().getResource("websocket.html");
            System.out.println(url);
            webView.getEngine().load(url.toString());
        });

        HBox buttons = new HBox(20, passButton, failButton);
        HBox run_test = new HBox(20, loadButton);
        buttons.setPadding(new Insets(10));
        VBox rootNode = new VBox(20, new HBox(instructions), webView, buttons);
        instructions.getChildren().add(run_test);
        rootNode.setPadding(new Insets(10));
        Scene scene = new Scene(rootNode, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
