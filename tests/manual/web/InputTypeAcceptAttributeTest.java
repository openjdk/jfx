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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;

public class InputTypeAcceptAttributeTest extends Application {

    private String fileNames[] = {"TEXT.txt", "PNG.png", "PDF.pdf", "JPG.jpg"};
    private File file = null;

    @Override
    public void start(Stage primaryStage) throws Exception {
        for (int i = 0; i < fileNames.length; ++i) {
            file = new File(fileNames[i]);
            file.createNewFile();
        }

        String currentDirPath = file.getCanonicalPath();
        currentDirPath = currentDirPath.substring(0, currentDirPath.lastIndexOf(file.separator));
        VBox instructions =  new VBox(
            new Label(" This test creates four files (TEXT.txt, PNG.png, PDF.pdf, JPG.jpg) at below mentioned path:"),
            new Label("  " + currentDirPath),
            new Label(" There are five different scenarios, follow below steps for each scenario."),
            new Label(""),
            new Label(" STEPS:"),
            new Label("  1. Click Choose File, it will display file chooser dialog."),
            new Label("  2. Navigate to above mentioned path."),
            new Label(" Expected behaviour: File Chooser dialog should show only the files of specified type."),
            new Label(" On Mac, the behaviour is little different than windows/linux. " +
                        "It shows all files, but user can select files of specified type only."));

        Button passButton = new Button("Pass");
        passButton.setOnAction(e -> {
            cleanup();
            Platform.exit();
        });

        Button failButton = new Button("Fail");
        failButton.setOnAction(e -> {
            cleanup();
            Platform.exit();
            throw new AssertionError("Displayed wrong type file.");
        });

        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.loadContent("<html>\n" +
                " <body>\n" +
                " 1. <input type = file /> should show all files. If not press Fail. <br/>" +
                " 2. <input type = file accept = image/* /> should show only (PNG.png, JPG.jpg) files. If not press Fail. <br/>" +
                " 3. <input type = file accept = text/* /> should show only TEXT.txt file. If not press Fail. <br/>" +
                " 4. <input type = file accept = image/png /> should show only PNG.png file. If not press Fail. <br/>" +
                " 5. <input type = file accept = image/jpg /> should show only JPG.jpg file. If not press Fail. <br/>" +
                " </body>\n" +
                "</html>");

        HBox buttons = new HBox(20, passButton, failButton);
        buttons.setPadding(new Insets(10));
        VBox rootNode = new VBox(20, new HBox(instructions), webView, buttons);
        rootNode.setPadding(new Insets(10));
        Scene scene = new Scene(rootNode, 1000, 450);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void cleanup() {
        file = null;
        for (int i = 0; i < fileNames.length; ++i) {
            file = new File(fileNames[i]);
            file.delete();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
