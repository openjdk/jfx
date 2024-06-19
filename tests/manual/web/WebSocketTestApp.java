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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;

/* WebSocketTestApp is an test app for Websocket ,
It simply test websocket channel , sending connection request
to wss://echo.websocket.org, once connection established  socket.onmessage
callback should invoked
 */
public class WebSocketTestApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        WebView webView = new WebView();

        Button loadButton = new Button("RunWebSocketTest");

        TextField messageField = new TextField();
        messageField.setEditable(false);
        messageField.setText("Click RunWebSocketTest button to test WebSocket");

        loadButton.setOnAction(e -> {
            URL url = this.getClass().getResource("websocket.html");
            System.out.println(url);
            webView.getEngine().load(url.toString());

            messageField.setText(" if There is message like Data received from server! on WebView, Test Pass");
        });

        // Create a VBox to hold the button and text field
        VBox vbox = new VBox(10, loadButton, messageField);
        vbox.setSpacing(10);

        BorderPane layout = new BorderPane();
        layout.setCenter(vbox);
        layout.setBottom(webView);

        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setTitle("JavaFX WebView WebSocket Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
