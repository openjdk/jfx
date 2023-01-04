/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HostServicesShowDocumentTest extends Application {

    private static final String testHtmlUri = new File("test.html").toURI().toString();
    private static final String testTxtUri  = new File("test.txt").toURI().toString();
    private static final String testCsvUri  = new File("test.csv").toURI().toString();

    @Override
    public void start(Stage primaryStage) throws Exception {
        VBox instructions =  new VBox(
            new Label(" This test can open and show three different document types (test.html, test.txt, test.csv)"),
            new Label(" via the platform default application registered for the respective types."),
            new Label(""),
            new Label(" STEPS:"),
            new Label("  1. Click on all the buttons for the file types."),
            new Label("  2. Check whether the corresponding document is shown."),
            new Label("  3. When all three documents are shown press Pass, otherwise press Fail."));

        Button showHtmlButton = new Button("HTML");
        showHtmlButton.setOnAction(e -> {
            this.getHostServices().showDocument(testHtmlUri);
        });

        Button showTxtButton = new Button("TXT");
        showTxtButton.setOnAction(e -> {
            this.getHostServices().showDocument(testTxtUri);
        });

        Button showCsvButton = new Button("CSV");
        showCsvButton.setOnAction(e -> {
            this.getHostServices().showDocument(testCsvUri);
        });

        Button passButton = new Button("Pass");
        passButton.setOnAction(e -> {
            Platform.exit();
        });

        Button failButton = new Button("Fail");
        failButton.setOnAction(e -> {
            Platform.exit();
            throw new AssertionError("Documents could not be shown.");
        });

        HBox testButtons = new HBox(20, showHtmlButton, showTxtButton, showCsvButton);
        testButtons.setPadding(new Insets(10));
        HBox resultButtons = new HBox(20, passButton, failButton);
        resultButtons.setPadding(new Insets(10));
        VBox rootNode = new VBox(20, new HBox(instructions), testButtons, resultButtons);
        rootNode.setPadding(new Insets(10));
        Scene scene = new Scene(rootNode, 1000, 450);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
