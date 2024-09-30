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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import com.oracle.demo.richtext.common.Styles;
import com.oracle.demo.richtext.common.TextStyle;
import com.oracle.demo.richtext.util.ExceptionDialog;
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
 *
 * @author Andy Goryachev
 */
public class Actions {
    // file
    public final FxAction newDocument = new FxAction(this::newDocument);
    public final FxAction open = new FxAction(this::open);
    public final FxAction save = new FxAction(this::save);
    public final FxAction saveAs = new FxAction(this::saveAs);
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

    void newDocument() {
        if (askToSave()) {
            return;
        }
        control.setModel(new RichTextModel());
        setModified(false);
    }

    void open() {
        if (askToSave()) {
            return;
        }

        FileChooser ch = new FileChooser();
        ch.setTitle("Open File");
        ch.getExtensionFilters().addAll(
            filterAll(),
            filterRich(),
            filterRtf()
        );

        Window w = FX.getParentWindow(control);
        File f = ch.showOpenDialog(w);
        if (f != null) {
            try {
                DataFormat fmt = guessFormat(f);
                readFile(f, fmt);
            } catch (Exception e) {
                new ExceptionDialog(control, e).open();
            }
        }
    }

    void save() {
        File f = getFile();
        if (f == null) {
            f = chooseFileForSave();
            if (f != null) {
                return;
            }
        }

        try {
            writeFile(f);
        } catch (Exception e) {
            new ExceptionDialog(control, e).open();
        }
    }

    void saveAs() {
        File f = chooseFileForSave();
        if (f != null) {
            try {
                writeFile(f);
            } catch(Exception e) {
                new ExceptionDialog(control, e).open();
            }
        }
    }

    private File chooseFileForSave() {
        File f = getFile();
        FileChooser ch = new FileChooser();
        if (f != null) {
            ch.setInitialDirectory(f.getParentFile());
            ch.setInitialFileName(f.getName());
        }
        ch.setTitle("Save File");
        ch.getExtensionFilters().addAll(
            filterRich(),
            filterRtf(),
            filterTxt()
            //filterAll()
        );
        Window w = FX.getParentWindow(control);
        return ch.showSaveDialog(w);
    }

    /**
     * @return true if the user chose to Cancel
     */
    private boolean askToSave() {
        if (isModified()) {
            saveAs();
            return true;
        }
        return false;
    }

    private void readFile(File f, DataFormat fmt) throws Exception {
        try (FileInputStream in = new FileInputStream(f)) {
            control.read(fmt, in);
            file.set(f);
            control.setEditable(f.canWrite());
            setModified(false);
        }
    }

    private void writeFile(File f) throws Exception {
        DataFormat fmt = guessFormat(f);
        try (FileOutputStream out = new FileOutputStream(f)) {
            control.write(fmt, out);
            file.set(f);
            setModified(false);
        }
    }

    void copy() {
        control.copy();
    }

    void cut() {
        control.cut();
    }

    void paste() {
        control.paste();
    }

    void pasteUnformatted() {
        control.pastePlainText();
    }

    void selectAll() {
        control.selectAll();
    }

    void redo() {
       control.redo();
    }

    void undo() {
        control.undo();
    }

    void bold() {
        toggleStyle(StyleAttributeMap.BOLD);
    }

    void italic() {
        toggleStyle(StyleAttributeMap.ITALIC);
    }

    void strikeThrough() {
        toggleStyle(StyleAttributeMap.STRIKE_THROUGH);
    }

    void underline() {
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

    static FileChooser.ExtensionFilter filterAll() {
        return new FileChooser.ExtensionFilter("All Files", "*.*");
    }

    static FileChooser.ExtensionFilter filterRich() {
        return new FileChooser.ExtensionFilter("Rich Text Files", "*.rich");
    }

    static FileChooser.ExtensionFilter filterRtf() {
        return new FileChooser.ExtensionFilter("RTF Files", "*.rtf");
    }

    static FileChooser.ExtensionFilter filterTxt() {
        return new FileChooser.ExtensionFilter("Text Files", "*.txt");
    }

    private static DataFormat guessFormat(File f) {
        String name = f.getName().toLowerCase(Locale.ENGLISH);
        if (name.endsWith(".rich")) {
            return RichTextFormatHandler.DATA_FORMAT;
        } else if (name.endsWith(".rtf")) {
            return DataFormat.RTF;
        }
        return DataFormat.PLAIN_TEXT;
    }

    public void quit() {
        if (isModified()) {
            if (askToSave()) {
                return;
            }
        }
        Platform.exit();
    }
}
