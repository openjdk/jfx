/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.pages;

import java.io.File;
import java.util.ArrayList;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * FileChooser Page.
 */
public class FileChooserPage extends TestPaneBase {

    private final SimpleObjectProperty<FileChooser.ExtensionFilter> extensionFilter = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty extJpg = new SimpleBooleanProperty();
    private final SimpleBooleanProperty extTxt = new SimpleBooleanProperty();
    private final SimpleBooleanProperty extUnk = new SimpleBooleanProperty();
    private final SimpleObjectProperty<File> initialDirectory = new SimpleObjectProperty<>();
    private final SimpleStringProperty initialFileName = new SimpleStringProperty();
    private final SimpleBooleanProperty owner = new SimpleBooleanProperty(true);
    private final SimpleStringProperty title = new SimpleStringProperty();
    private static final FileChooser.ExtensionFilter EF_JPG = new FileChooser.ExtensionFilter("*.jpg", "*.jpg", "*.jpeg");
    private static final FileChooser.ExtensionFilter EF_TXT = new FileChooser.ExtensionFilter("*.txt", "*.txt");
    private static final FileChooser.ExtensionFilter EF_UNK = new FileChooser.ExtensionFilter("*.unk", "*.unk");

    @FunctionalInterface
    interface FCMethod {
        public Object call(FileChooser fc, Window w);
    }

    public FileChooserPage() {
        super("FileChooserPage");

        Button showOpenButton = new Button("Show Open");
        showOpenButton.setOnAction((ev) -> {
            showFileChooser(FileChooser::showOpenDialog);
        });

        Button showMultipleOpenButton = new Button("Show Multiple Open");
        showMultipleOpenButton.setOnAction((ev) -> {
            showFileChooser(FileChooser::showOpenMultipleDialog);
        });

        Button showSaveButton = new Button("Show Save");
        showSaveButton.setOnAction((ev) -> {
            showFileChooser(FileChooser::showSaveDialog);
        });

        OptionPane op = createOptionPane();

        HBox p = new HBox(4, showOpenButton, showMultipleOpenButton, showSaveButton);
        p.setPadding(new Insets(4));

        setContent(p);
        setOptions(op);
    }

    private OptionPane createOptionPane() {
        OptionPane op = new OptionPane();
        op.section("FileChooser");
        op.option("Initial Directory:", dirOption("initialDirectory", initialDirectory));
        op.option("Initial File Name:", Options.textOption("initialFileName", false, true, initialFileName));
        op.option("Extension Filter:", new BooleanOption("extJpg", "*.jpg", extJpg));
        op.option("", new BooleanOption("extTxt", "*.txt", extTxt));
        op.option("", new BooleanOption("extUnk", "*.unk", extUnk));
        op.option("Selected Filter:", extensionFilter("extensionFilter", extensionFilter));
        op.option("Title:", Options.textOption("title", true, true, title));
        op.separator();
        op.option(new BooleanOption("owner", "set owner", owner));
        return op;
    }

    private Node extensionFilter(String name, SimpleObjectProperty<FileChooser.ExtensionFilter> p) {
        ObjectOption<FileChooser.ExtensionFilter> op = new ObjectOption<>(name, p);
        op.addChoice("<null>", null);
        op.addChoice("*.jpg", EF_JPG);
        op.addChoice("*.txt", EF_TXT);
        op.addChoice("*.unk", EF_UNK);
        return op;
    }

    private Node dirOption(String name, ObjectProperty<File> p) {
        ObjectOption<File> op = new ObjectOption<>(name, p);
        op.addChoice("<null>", null);
        op.addChoiceSupplier("Current Directory", () -> {
            return new File("").getAbsoluteFile();
        });
        op.addChoiceSupplier("Parent Directory", () -> {
            File f = new File("").getAbsoluteFile();
            if (f.getParentFile() != null) {
                return f.getParentFile();
            }
            return f;
        });
        return op;
    }

    private FileChooser createFileChooser() {
        File dir = initialDirectory.get();
        String name = initialFileName.get();
        FileChooser.ExtensionFilter ext = extensionFilter.get();
        ArrayList<FileChooser.ExtensionFilter> fs = new ArrayList<>();
        if (extJpg.get()) {
            fs.add(EF_JPG);
        }
        if (extTxt.get()) {
            fs.add(EF_TXT);
        }
        if (extUnk.get()) {
            fs.add(EF_UNK);
        }

        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(dir);
        fc.setInitialFileName(name);
        fc.getExtensionFilters().setAll(fs);
        fc.setSelectedExtensionFilter(ext);
        fc.titleProperty().bind(title);
        return fc;
    }

    private Window parentWindow() {
        if (owner.get()) {
            return FX.getParentWindow(this);
        }
        return null;
    }

    private void showFileChooser(FCMethod func) {
        FileChooser fc = createFileChooser();
        Window w = parentWindow();
        Object v = func.call(fc, w);
        System.out.println(v);
    }
}
