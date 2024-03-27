/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.demo.rich.editor;

import java.io.File;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import com.oracle.demo.rich.util.FX;

public class SavePane extends GridPane {
    private final TextField nameField;
    private final ComboBox<File> folderField;
    private final ComboBox<DataFormat> formatField;

    public SavePane() {
        nameField = new TextField();
        setHgrow(nameField, Priority.ALWAYS);
        setFillWidth(nameField, Boolean.TRUE);

        folderField = new ComboBox<>();
        setHgrow(folderField, Priority.ALWAYS);
        setFillWidth(folderField, Boolean.TRUE);

        formatField = new ComboBox<>();

        Button browse = new Button("Browse");
        setFillWidth(browse, Boolean.TRUE);
        browse.setOnAction((ev) -> {
            browse();
        });

        int r = 0;
        add(label("Save As:"), 0, r);
        add(nameField, 1, r, 3, 1);
        r++;
        add(label("Where:"), 0, r);
        add(folderField, 1, r);
        add(browse, 2, r);
        r++;
        add(label("File Format:"), 0, r);
        add(formatField, 1, r, 2, 1);

        setHgap(10);
        setVgap(5);
        setPadding(new Insets(10));

        Platform.runLater(() -> {
            nameField.selectAll();
            nameField.requestFocus();
        });
    }

    private static Label label(String text) {
        Label t = new Label(text);
        setHalignment(t, HPos.RIGHT);
        return t;
    }

    public void setFile(File f) {
        if (f == null) {
            nameField.setText("Untitled.rich");
            setDir(null);
        } else {
            nameField.setText(f.getName());
            setDir(f.getParentFile());
        }
    }

    private void setDir(File dir) {
        if (dir == null) {
            dir = new File(System.getProperty("user.home"));
        }
        ArrayList<File> fs = new ArrayList<>();
        File f = dir;
        do {
            fs.add(f);
            f = f.getParentFile();
        } while (f != null);
        folderField.getItems().setAll(fs);
        folderField.getSelectionModel().select(dir);
    }

    public void setFormat(DataFormat f) {
        // TODO
    }

    public File getFile() {
        File dir = getDir();
        // TODO extension based on data format
        return new File(dir, nameField.getText());
    }

    public DataFormat getFileFormat() {
        return null; // FIX
    }

    private File getDir() {
        return folderField.getSelectionModel().getSelectedItem();
    }

    private void browse() {
        DirectoryChooser ch = new DirectoryChooser();
        ch.setTitle("Choose Folder");
        ch.setInitialDirectory(getDir());
        Window w = FX.getParentWindow(this);
        File f = ch.showDialog(w);
        if (f != null) {
            setDir(f);
        }
    }
}
