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
import java.util.List;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public final class HelloFileChooser extends Application {

    final ToggleGroup fctypeGroup = new ToggleGroup();
    final RadioButton fcOpenSingle = new RadioButton("Select Single Files");
    final RadioButton fcOpenMultiple = new RadioButton("Select Multiple Files");
    final RadioButton fcSave = new RadioButton("Save File");

    final CheckBox filterText = new CheckBox("Text *.txt");
    final CheckBox filterImage = new CheckBox("Images *.jpg, *.png, *.gif");

    final CheckBox useTitle = new CheckBox("Use dialog title");

    FileChooser fileChooser;

    final TextField directory = new TextField("");
    final TextField filename = new TextField("");

    final TextArea text = new TextArea();

    @Override
    public void start(final Stage stage) {
        stage.setTitle("File Chooser Sample");

        fcOpenSingle.setToggleGroup(fctypeGroup);
        fcOpenSingle.setSelected(true);

        fcOpenMultiple.setToggleGroup(fctypeGroup);

        fcSave.setToggleGroup(fctypeGroup);

        VBox fcTypeBox = new VBox();
        fcTypeBox.setSpacing(10);

        fcTypeBox.getChildren().addAll(
                new Label("FileChooser Type:"),
                fcOpenSingle, fcOpenMultiple, fcSave);

        HBox dirBox = new HBox();
        dirBox.getChildren().addAll(
                new Label("Starting Directory:"),
                directory
        );
        directory.setPrefColumnCount(50);

        HBox nameBox = new HBox();
        nameBox.getChildren().addAll(
                new Label("File name:   "),
                filename
        );
        filename.setPrefColumnCount(50);

        VBox fcOptionsBox = new VBox();
        fcOptionsBox.getChildren().addAll(
                new Label("Dialog Options:"),
                useTitle,
                dirBox,
                nameBox
        );

        VBox fcFilterBox = new VBox();
        fcTypeBox.getChildren().addAll(
                new Label("File Filters:"),
                filterText,
                filterImage);

        final Button openButton = new Button("Make it so...");

        text.setPrefColumnCount(60);
        text.setPrefRowCount(10);

        openButton.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                        fileChooser = new FileChooser();

                        if (filterText.isSelected()) {
                            System.out.println("Adding text filter");
                            fileChooser.getExtensionFilters().addAll(
                                    new ExtensionFilter("Text Files", "*.txt")
                            );
                        }
                        if (filterImage.isSelected()) {
                            System.out.println("Adding image filter");
                            fileChooser.getExtensionFilters().addAll(
                                    new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
                            );
                        }

                        if (useTitle.isSelected()) {
                            System.out.println("Adding title");
                            fileChooser.setTitle("Open Resource File");
                        }

                        String fn = filename.getText();
                        if (!fn.equals("")) {
                            System.out.println("Filename:" + fn);
                            fileChooser.setInitialFileName(fn);
                        }

                        String dir = directory.getText();
                        if (!dir.equals("")) {
                            System.out.println("Directory:" + dir);
                            fileChooser.setInitialDirectory(new File(dir));
                        }

                        Toggle t = fctypeGroup.getSelectedToggle();
                        try {
                            if (t == fcOpenSingle) {
                                System.out.println("OpenSingle");
                                File file = fileChooser.showOpenDialog(stage);
                                if (file == null) {
                                    text.setText("Open dialog returns NULL");
                                } else {
                                    text.setText("Open dialog returns\n"
                                            + file.getPath());
                                }
                            } else if (t == fcOpenMultiple) {
                                List<File> files = fileChooser.showOpenMultipleDialog(stage);
                                StringBuilder sb = new StringBuilder();
                                if (files == null) {
                                    text.setText("Open dialog returns NULL");
                                } else {
                                    sb.append("Open dialog returns:\n");
                                    files.forEach((f) -> {
                                        sb.append(f);
                                        sb.append("\n");
                                    });
                                }
                                text.setText(sb.toString());
                            } else {
                                File file = fileChooser.showSaveDialog(stage);
                                if (file == null) {
                                    text.setText("Save dialog returns NULL");
                                } else {
                                    text.setText("Save dialog returns\n"
                                            + file.getPath());
                                }
                            }
                        } catch (IllegalArgumentException ex) {
                            text.setText(ex.toString());
                        }
                    }
                });

        final Pane rootGroup = new VBox(12);
        rootGroup.getChildren().addAll(
                fcTypeBox,
                fcOptionsBox,
                fcFilterBox,
                openButton,
                text);
        rootGroup.setPadding(new Insets(12, 12, 12, 12));

        stage.setScene(new Scene(rootGroup));
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
