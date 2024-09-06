/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.editor;

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
import com.oracle.demo.richtext.util.FX;

/**
 * Part of the Save As dialog.
 *
 * @author Andy Goryachev
 */
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
