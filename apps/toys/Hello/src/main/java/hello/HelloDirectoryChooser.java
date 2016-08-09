/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public final class HelloDirectoryChooser extends Application {

    final CheckBox useTitle = new CheckBox("Use dialog title");

    DirectoryChooser directoryChooser;

    final TextField directory = new TextField("");

    final TextArea text = new TextArea();

    Scene scene;

    @Override
    public void start(final Stage stage) {
        stage.setTitle("Directory Chooser Sample");

        VBox fcTypeBox = new VBox();
        fcTypeBox.setSpacing(10);

        HBox dirBox = new HBox();
        dirBox.getChildren().addAll(
                new Label("Starting Directory:"),
                directory
        );
        directory.setPrefColumnCount(50);

        HBox nameBox = new HBox();

        VBox fcOptionsBox = new VBox();
        fcOptionsBox.getChildren().addAll(
                new Label("Dialog Options:"),
                useTitle,
                dirBox,
                nameBox
        );

        final Button openButton = new Button("Make it so...");

        text.setPrefColumnCount(60);
        text.setPrefRowCount(10);

        openButton.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                        directoryChooser = new DirectoryChooser();

                        if (useTitle.isSelected()) {
                            System.out.println("Adding title");
                            directoryChooser.setTitle("Alternate Title - Open Directory");
                        }

                        String dir = directory.getText();
                        if (!dir.equals("")) {
                            System.out.println("Directory:" + dir);
                            directoryChooser.setInitialDirectory(new File(dir));
                        }

                        File selectedDir = directoryChooser.showDialog(scene.getWindow());

                        System.out.println("selectedDir = "+selectedDir);

                        StringBuilder sb = new StringBuilder();
                        sb.append("DirectoryChooser dialog returns:\n");
                        sb.append(selectedDir == null ? "null" : selectedDir);
                        text.setText(sb.toString());}
                });

        final Pane rootGroup = new VBox(12);
        rootGroup.getChildren().addAll(
                fcTypeBox,
                fcOptionsBox,
                openButton,
                text);
        rootGroup.setPadding(new Insets(12, 12, 12, 12));

        scene = new Scene(rootGroup);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
