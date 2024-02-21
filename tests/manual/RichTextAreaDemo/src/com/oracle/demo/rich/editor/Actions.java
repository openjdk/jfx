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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Optional;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.incubator.scene.control.rich.RichTextArea;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.EditableRichTextModel;
import javafx.incubator.scene.control.rich.model.RichTextFormatHandler;
import javafx.incubator.scene.control.rich.model.StyleAttribute;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.incubator.scene.control.rich.model.StyledTextModel;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import com.oracle.demo.rich.util.FX;
import com.oracle.demo.rich.util.FxAction;

/**
 * This is a bit of hack.  JavaFX has no actions (yet), so here we are using FxActions from
 * https://github.com/andy-goryachev/AppFramework (with permission from the author).
 * Ideally, these actions should be created upon demand and managed by the control, because
 * control knows when the enabled state of each action changes.
 * <p>
 * This class adds a listener to the model and updates the states of all the actions.
 * (The model does not change in this application).
 */
public class Actions {
    public final FxAction bold;
    public final FxAction copy;
    public final FxAction cut;
    public final FxAction italic;
    public final FxAction newDocument;
    public final FxAction open;
    public final FxAction paste;
    public final FxAction pasteUnformatted;
    public final FxAction redo;
    public final FxAction save;
    public final FxAction selectAll;
    public final FxAction strikeThrough;
    public final FxAction underline;
    public final FxAction undo;
    public final FxAction wrapText;

    private final RichTextArea control;
    private final ReadOnlyBooleanWrapper modified = new ReadOnlyBooleanWrapper();
    private final ReadOnlyObjectWrapper<File> file = new ReadOnlyObjectWrapper<>();

    public Actions(RichTextArea control) {
        this.control = control;

        newDocument = new FxAction(this::newDocument);

        open = new FxAction(this::open);

        save = new FxAction(this::save);

        undo = new FxAction(control::undo);
        undo.disabledProperty().bind(Bindings.createBooleanBinding(() -> {
            return !control.isUndoable();
        }, control.undoableProperty()));

        redo = new FxAction(control::redo);
        redo.disabledProperty().bind(Bindings.createBooleanBinding(() -> {
            return !control.isRedoable();
        }, control.redoableProperty()));

        cut = new FxAction(control::cut);

        copy = new FxAction(control::copy);

        paste = new FxAction(control::paste);

        pasteUnformatted = new FxAction(control::pastePlainText);

        selectAll = new FxAction(control::selectAll);

        bold = new FxAction(() -> toggle(StyleAttrs.BOLD));

        italic = new FxAction(() -> toggle(StyleAttrs.ITALIC));

        underline = new FxAction(() -> toggle(StyleAttrs.UNDERLINE));

        strikeThrough = new FxAction(() -> toggle(StyleAttrs.STRIKE_THROUGH));

        wrapText = new FxAction();
        wrapText.selectedProperty().bindBidirectional(control.wrapTextProperty());

        control.getModel().addChangeListener(new StyledTextModel.ChangeListener() {
            @Override
            public void eventTextUpdated(TextPos start, TextPos end, int top, int added, int bottom) {
                handleEdit();
            }

            @Override
            public void eventStyleUpdated(TextPos start, TextPos end) {
                handleEdit();
            }
        });

        control.caretPositionProperty().addListener((x) -> {
            handleCaret();
        });

        handleEdit();
        handleCaret();
        modified.set(false);
    }

    public final ReadOnlyBooleanProperty modifiedProperty() {
        return modified.getReadOnlyProperty();
    }

    public final boolean isModified() {
        return modified.get();
    }

    public final ReadOnlyObjectProperty<File> fileNameProperty() {
        return file.getReadOnlyProperty();
    }

    public final File getFile() {
        return file.get();
    }

    private void handleEdit() {
        modified.set(true);
    }

    private void handleCaret() {
        boolean sel = control.hasNonEmptySelection();
        StyleAttrs a = control.getActiveStyleAttrs();

        cut.setEnabled(sel);
        copy.setEnabled(sel);

        bold.setSelected(a.getBoolean(StyleAttrs.BOLD), false);
        italic.setSelected(a.getBoolean(StyleAttrs.ITALIC), false);
        underline.setSelected(a.getBoolean(StyleAttrs.UNDERLINE), false);
        strikeThrough.setSelected(a.getBoolean(StyleAttrs.STRIKE_THROUGH), false);
    }

    private void toggle(StyleAttribute<Boolean> attr) {
        TextPos start = control.getAnchorPosition();
        TextPos end = control.getCaretPosition();
        if (start == null) {
            return;
        } else if (start.equals(end)) {
            // apply to the whole paragraph
            int ix = start.index();
            start = new TextPos(ix, 0);
            end = control.getEndOfParagraph(ix);
        }

        StyleAttrs a = control.getActiveStyleAttrs();
        boolean on = !a.getBoolean(attr);
        a = StyleAttrs.builder().set(attr, on).build();
        control.applyStyle(start, end, a);
    }

    private <T> void apply(StyleAttribute<T> attr, T value) {
        TextPos start = control.getAnchorPosition();
        TextPos end = control.getCaretPosition();
        if (start == null) {
            return;
        } else if (start.equals(end)) {
            // apply to the whole paragraph
            int ix = start.index();
            start = new TextPos(ix, 0);
            end = control.getEndOfParagraph(ix);
        }

        StyleAttrs a = control.getActiveStyleAttrs();
        a = StyleAttrs.builder().set(attr, value).build();
        control.applyStyle(start, end, a);
    }

    // TODO need to bind selected item in the combo
    public void setFontSize(Integer size) {
        apply(StyleAttrs.FONT_SIZE, size.doubleValue());
    }

    // TODO need to bind selected item in the combo
    public void setFontName(String name) {
        apply(StyleAttrs.FONT_FAMILY, name);
    }

    public void setTextColor(Color color) {
        apply(StyleAttrs.TEXT_COLOR, color);
    }

    private void newDocument() {
        if (askToSave()) {
            return;
        }
        control.setModel(new EditableRichTextModel());
        modified.set(false);
    }

    private void open() {
        if (askToSave()) {
            return;
        }

        FileChooser ch = new FileChooser();
        ch.setTitle("Open File");
        // TODO add extensions
        Window w = FX.getParentWindow(control);
        File f = ch.showOpenDialog(w);
        if (f != null) {
            try {
                readFile(f, RichTextFormatHandler.DATA_FORMAT);
            } catch (Exception e) {
                new ExceptionDialog(control, e).open();
            }
        }
    }

    // FIX this is too simplistic, need save() and save as...
    private void save() {
        File f = getFile();
        if (f == null) {
            FileChooser ch = new FileChooser();
            ch.setTitle("Save File");
            // TODO add extensions
            Window w = FX.getParentWindow(control);
            f = ch.showSaveDialog(w);
            if (f == null) {
                return;
            }
        }
        try {
            writeFile(f, RichTextFormatHandler.DATA_FORMAT);
        } catch (Exception e) {
            new ExceptionDialog(control, e).open();
        }
    }

    // returns true if the user chose to Cancel
    private boolean askToSave() {
        if (isModified()) {
            // alert: has been modified. do you want to save?
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.initOwner(FX.getParentWindow(control));
            alert.setTitle("Document is Modified");
            alert.setHeaderText("Do you want to save this document?");
            ButtonType delete = new ButtonType("Delete");
            ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
            ButtonType save = new ButtonType("Save", ButtonData.APPLY);
            alert.getButtonTypes().setAll(
                delete,
                cancel,
                save
            );

            File f = getFile();
            SavePane sp = new SavePane();
            sp.setFile(f);
            alert.getDialogPane().setContent(sp);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                ButtonType t = result.get();
                if (t == delete) {
                    return false;
                } else if (t == cancel) {
                    return true;
                } else {
                    // save using info in the panel
                    f = sp.getFile();
                    DataFormat fmt = sp.getFileFormat();
                    // FIX
                    fmt = RichTextFormatHandler.DATA_FORMAT;

                    try {
                        writeFile(f, fmt);
                    } catch (Exception e) {
                        new ExceptionDialog(control, e).open();
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private void readFile(File f, DataFormat fmt) throws Exception {
        try (FileInputStream in = new FileInputStream(f)) {
            control.read(fmt, in);
            file.set(f);
            modified.set(false);
        }
    }

    private void writeFile(File f, DataFormat fmt) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f)) {
            control.write(fmt, out);
            file.set(f);
            modified.set(false);
        }
    }
}
