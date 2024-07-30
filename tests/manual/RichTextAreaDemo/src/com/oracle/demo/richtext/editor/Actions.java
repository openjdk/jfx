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

package com.oracle.demo.richtext.editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Optional;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import com.oracle.demo.richtext.common.Styles;
import com.oracle.demo.richtext.common.TextStyle;
import com.oracle.demo.richtext.util.FX;
import com.oracle.demo.richtext.util.FxAction;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.ContentChange;
import jfx.incubator.scene.control.richtext.model.RichTextFormatHandler;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

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
    // file
    public final FxAction newDocument = new FxAction(this::newDocument);
    public final FxAction open = new FxAction(this::open);
    public final FxAction save = new FxAction(this::save);
    // style
    public final FxAction bold = new FxAction(this::bold);
    public final FxAction italic = new FxAction(this::italic);
    public final FxAction strikeThrough = new FxAction(this::strikeThrough);
    public final FxAction underline = new FxAction(this::underline);
    // editing
    public final FxAction copy = new FxAction(this::copy);
    public final FxAction cut = new FxAction(this::cut);
    public final FxAction paste = new FxAction(this::paste);
    public final FxAction pasteUnformatted = new FxAction(this::pasteUnformatted);
    public final FxAction redo = new FxAction(this::redo);
    public final FxAction selectAll = new FxAction(this::selectAll);
    public final FxAction undo = new FxAction(this::undo);
    // view
    public final FxAction wrapText = new FxAction();

    private final RichTextArea control;
    private final ReadOnlyBooleanWrapper modified = new ReadOnlyBooleanWrapper();
    private final ReadOnlyObjectWrapper<File> file = new ReadOnlyObjectWrapper<>();
    private final SimpleObjectProperty<StyleAttributeMap> styles = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<TextStyle> textStyle = new SimpleObjectProperty<>();

    public Actions(RichTextArea control) {
        this.control = control;

        // undo/redo actions
        redo.disabledProperty().bind(control.redoableProperty().not());
        undo.disabledProperty().bind(control.undoableProperty().not());

        undo.disabledProperty().bind(Bindings.createBooleanBinding(() -> {
            return !control.isUndoable();
        }, control.undoableProperty()));

        redo.disabledProperty().bind(Bindings.createBooleanBinding(() -> {
            return !control.isRedoable();
        }, control.redoableProperty()));

        wrapText.selectedProperty().bindBidirectional(control.wrapTextProperty());

        control.getModel().addListener(new StyledTextModel.Listener() {
            @Override
            public void onContentChange(ContentChange ch) {
                handleEdit();
            }
        });

        control.caretPositionProperty().addListener((x) -> {
            handleCaret();
        });

        control.selectionProperty().addListener((p) -> {
            updateSourceStyles();
        });

        styles.addListener((s,p,a) -> {
            bold.setSelected(hasStyle(a, StyleAttributeMap.BOLD), false);
            italic.setSelected(hasStyle(a, StyleAttributeMap.ITALIC), false);
            strikeThrough.setSelected(hasStyle(a, StyleAttributeMap.STRIKE_THROUGH), false);
            underline.setSelected(hasStyle(a, StyleAttributeMap.UNDERLINE), false);
        });

        updateSourceStyles();

        handleEdit();
        handleCaret();
        setModified(false);
    }

    private boolean hasStyle(StyleAttributeMap attrs, StyleAttribute<Boolean> a) {
        return attrs == null ? false : Boolean.TRUE.equals(attrs.get(a));
    }

    public final ObjectProperty<TextStyle> textStyleProperty() {
        return textStyle;
    }

    public final ReadOnlyBooleanProperty modifiedProperty() {
        return modified.getReadOnlyProperty();
    }

    public final boolean isModified() {
        return modified.get();
    }

    private void setModified(boolean on) {
        modified.set(on);
    }

    public final ReadOnlyObjectProperty<File> fileNameProperty() {
        return file.getReadOnlyProperty();
    }

    public final File getFile() {
        return file.get();
    }

    private void handleEdit() {
        setModified(true);
    }

    private void handleCaret() {
        boolean sel = control.hasNonEmptySelection();
        StyleAttributeMap a = control.getActiveStyleAttributeMap();

        cut.setEnabled(sel);
        copy.setEnabled(sel);

        bold.setSelected(a.getBoolean(StyleAttributeMap.BOLD), false);
        italic.setSelected(a.getBoolean(StyleAttributeMap.ITALIC), false);
        underline.setSelected(a.getBoolean(StyleAttributeMap.UNDERLINE), false);
        strikeThrough.setSelected(a.getBoolean(StyleAttributeMap.STRIKE_THROUGH), false);
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
            end = control.getParagraphEnd(ix);
        }

        StyleAttributeMap a = control.getActiveStyleAttributeMap();
        boolean on = !a.getBoolean(attr);
        a = StyleAttributeMap.builder().set(attr, on).build();
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
            end = control.getParagraphEnd(ix);
        }

        StyleAttributeMap a = control.getActiveStyleAttributeMap();
        a = StyleAttributeMap.builder().set(attr, value).build();
        control.applyStyle(start, end, a);
    }

    // TODO need to bind selected item in the combo
    public void setFontSize(Integer size) {
        apply(StyleAttributeMap.FONT_SIZE, size.doubleValue());
    }

    // TODO need to bind selected item in the combo
    public void setFontName(String name) {
        apply(StyleAttributeMap.FONT_FAMILY, name);
    }

    public void setTextColor(Color color) {
        apply(StyleAttributeMap.TEXT_COLOR, color);
    }

    private void newDocument() {
        if (askToSave()) {
            return;
        }
        control.setModel(new RichTextModel());
        setModified(false);
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
            setModified(false);
        }
    }

    private void writeFile(File f, DataFormat fmt) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f)) {
            control.write(fmt, out);
            file.set(f);
            setModified(false);
        }
    }

    public void copy() {
        control.copy();
    }

    public void cut() {
        control.cut();
    }

    public void paste() {
        control.paste();
    }

    public void pasteUnformatted() {
        control.pastePlainText();
    }

    public void selectAll() {
        control.selectAll();
    }

    public void redo() {
       control.redo();
    }

    public void undo() {
        control.undo();
    }

    public void bold() {
        toggleStyle(StyleAttributeMap.BOLD);
    }

    public void italic() {
        toggleStyle(StyleAttributeMap.ITALIC);
    }

    public void strikeThrough() {
        toggleStyle(StyleAttributeMap.STRIKE_THROUGH);
    }

    public void underline() {
        toggleStyle(StyleAttributeMap.UNDERLINE);
    }

    private void toggleStyle(StyleAttribute<Boolean> attr) {
        TextPos start = control.getAnchorPosition();
        TextPos end = control.getCaretPosition();
        if (start == null) {
            return;
        } else if (start.equals(end)) {
            // apply to the whole paragraph
            int ix = start.index();
            start = new TextPos(ix, 0);
            end = control.getParagraphEnd(ix);
        }

        StyleAttributeMap a = control.getActiveStyleAttributeMap();
        boolean on = !a.getBoolean(attr);
        a = StyleAttributeMap.builder().set(attr, on).build();
        control.applyStyle(start, end, a);
        updateSourceStyles();
    }

    public void setTextStyle(TextStyle st) {
        TextPos start = control.getAnchorPosition();
        TextPos end = control.getCaretPosition();
        if (start == null) {
            return;
        } else if (start.equals(end)) {
            TextStyle cur = Styles.guessTextStyle(control.getActiveStyleAttributeMap());
            if (cur == st) {
                return;
            }
            // apply to the whole paragraph
            int ix = start.index();
            start = new TextPos(ix, 0);
            end = control.getParagraphEnd(ix);
        }

        StyleAttributeMap a = Styles.getStyleAttributeMap(st);
        control.applyStyle(start, end, a);
        updateSourceStyles();
    }

    private void updateSourceStyles() {
        StyleAttributeMap a = getSourceStyleAttrs();
        if (a != null) {
            styles.set(a);

            TextStyle st = Styles.guessTextStyle(a);
            textStyle.set(st);
        }
    }

    private StyleAttributeMap getSourceStyleAttrs() {
        SelectionSegment sel = control.getSelection();
        if ((sel == null) || (!sel.isCollapsed())) {
            return null;
        }
        return control.getActiveStyleAttributeMap();
    }
}
