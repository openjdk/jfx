/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
package dnd;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class DNDWebViewTest extends Application {

    private static long time;
    private static long initialTime;

    @Override
    public void start(Stage primaryStage) throws Exception {
        final WebView webView = new WebView();

        final Button offlineButton = new Button("Offline test");
        final Button onlineButton = new Button("Online test");
        offlineButton.setOnAction(e -> webView.getEngine().load(getClass().getResource("drag.html").toExternalForm()));
        onlineButton.setOnAction(e -> webView.getEngine().load("https://openjdk.org"));

        final Label instructions = new Label("Select a test and drag the images");
        final Label readTime = new Label("");

        webView.addEventHandler(DragEvent.DRAG_ENTERED, e -> {
            time = System.currentTimeMillis();
            initialTime = -1;
        });
        webView.addEventHandler(DragEvent.DRAG_OVER, e -> {
            long newTime = System.currentTimeMillis();
            if (initialTime == -1) {
                initialTime = newTime - time;
            }
            readTime.setText("DND image read interval = " + (newTime - time) + " ms, initial delay = " + initialTime + " ms");
            time = newTime;
        });

        VBox root = new VBox(20, instructions, new HBox(20, offlineButton, onlineButton), readTime, webView);
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }
}
