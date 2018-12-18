/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class WindowResizableTest extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Button passButton = new Button("Pass");
        Button failButton = new Button("Fail");
        passButton.setOnAction(e -> this.quit());
        failButton.setOnAction(e -> {
            this.quit();
            throw new AssertionError("The window is no longer resizable");
        });

        VBox rootNode = new VBox(5,
                new Label("1. This is a MacOs specific test. If you run the test on some other Platform, please click Pass."),
                new Label("2. Verify that the window is resizable in the beginning by dragging the edges of this Window."),
                new Label("3. Click the green maximize button on window to enter full-screen mode and again click it to come back to normal mode."),
                new Label("4. If the window is still resizable, click Pass otherwise Fail."),
                new Label(""),
                new HBox(10, passButton, failButton));

        rootNode.setPadding(new Insets(8));
        Scene scene = new Scene(rootNode);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    private void quit() {
        Platform.exit();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
