/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates.
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
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.scene.text.TabStopPolicy;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import com.oracle.demo.richtext.common.Styles;
import com.oracle.demo.richtext.common.TextStyle;
import com.oracle.demo.richtext.editor.settings.EndKey;
import com.oracle.demo.richtext.util.ExceptionDialog;
import com.oracle.demo.richtext.util.FX;
import com.oracle.demo.richtext.util.FxAction;
import com.oracle.demo.richtext.util.Utils;
import jfx.incubator.scene.control.richtext.LineNumberDecorator;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.EmbeddedImage;
import jfx.incubator.scene.control.richtext.model.RichTextFormatHandler;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import jfx.incubator.scene.control.richtext.model.TabStops;

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
    public final FxAction paragraphStyle = new FxAction(this::showParagraphDialog);
    // editing
    public final FxAction copy = new FxAction(this::copy);
    public final FxAction cut = new FxAction(this::cut);
    public final FxAction paste = new FxAction(this::paste);
    public final FxAction pasteUnformatted = new FxAction(this::pasteUnformatted);
    public final FxAction redo = new FxAction(this::redo);
    public final FxAction selectAll = new FxAction(this::selectAll);
    public final FxAction undo = new FxAction(this::undo);
    // view
    public final FxAction highlightCurrentLine = new FxAction();
    public final FxAction lineNumbers = new FxAction();
    public final FxAction wrapText = new FxAction();

    private final ReadOnlyBooleanWrapper modified = new ReadOnlyBooleanWrapper();
    private final ReadOnlyObjectWrapper<File> file = new ReadOnlyObjectWrapper<>();
    private final SimpleObjectProperty<StyleAttributeMap> styles = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<TextStyle> textStyle = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty rulerVisible = new SimpleBooleanProperty(true);

    private final RichEditorToolbar toolbar;
    private final RichTextArea editor;
    private final TabStopPolicy tabPolicy = new TabStopPolicy();
    private final StyledTextModel.Listener changeListener = (ch) -> handleEdit();

    public Actions(RichEditorToolbar tb, RichTextArea ed) {
        this.toolbar = tb;
        this.editor = ed;

        // undo/redo actions

        redo.disabledProperty().bind(editor.redoableProperty().not());
        undo.disabledProperty().bind(editor.undoableProperty().not());

        undo.disabledProperty().bind(Bindings.createBooleanBinding(() -> {
            return !editor.isUndoable();
        }, editor.undoableProperty()));

        redo.disabledProperty().bind(Bindings.createBooleanBinding(() -> {
            return !editor.isRedoable();
        }, editor.redoableProperty()));

        highlightCurrentLine.selectedProperty().bindBidirectional(editor.highlightCurrentParagraphProperty());

        // editor

        editor.modelProperty().subscribe((prev, m) -> {
            if (prev != null) {
                prev.removeListener(changeListener);
            }
            if (m != null) {
                m.addListener(changeListener);
            }
        });

        editor.insertStylesProperty().bind(Bindings.createObjectBinding(
            this::getInsertStyles,
            toolbar.bold.selectedProperty(),
            toolbar.fontFamily.getSelectionModel().selectedItemProperty(),
            toolbar.fontSize.getSelectionModel().selectedItemProperty(),
            toolbar.italic.selectedProperty(),
            toolbar.strikeThrough.selectedProperty(),
            toolbar.underline.selectedProperty(),
            toolbar.textColor.valueProperty()
        ));

        editor.selectionProperty().addListener((p) -> {
            updateSourceStyles();
            handleSelection();
        });
        updateSourceStyles();

        // toolbar

        toolbar.bold.selectedProperty().bindBidirectional(bold.selectedProperty());
        toolbar.bold.setOnAction((_) -> bold());

        toolbar.italic.selectedProperty().bindBidirectional(italic.selectedProperty());
        toolbar.italic.setOnAction((_) -> italic());

        toolbar.strikeThrough.selectedProperty().bindBidirectional(strikeThrough.selectedProperty());
        toolbar.strikeThrough.setOnAction((_) -> strikeThrough());

        toolbar.underline.selectedProperty().bindBidirectional(underline.selectedProperty());
        toolbar.underline.setOnAction((_) -> underline());

        paragraphStyle.attach(toolbar.paragraphButton);
        paragraphStyle.disabledProperty().bind(Bindings.createBooleanBinding(() -> {
            return editor.getSelection() == null;
        }, editor.selectionProperty()));

        toolbar.lineNumbers.selectedProperty().bindBidirectional(lineNumbers.selectedProperty());
        toolbar.lineNumbers.setOnAction((_) -> focusEditor());
        lineNumbers.selectedProperty().addListener((s,p,on) -> {
            editor.setLeftDecorator(on ? new LineNumberDecorator() : null);
        });

        toolbar.wrapText.selectedProperty().bindBidirectional(wrapText.selectedProperty());
        toolbar.wrapText.setOnAction((_) -> focusEditor());
        wrapText.selectedProperty().bindBidirectional(editor.wrapTextProperty());

        toolbar.fontFamily.setOnAction((ev) -> {
            setFontFamily(toolbar.fontFamily.getSelectionModel().getSelectedItem());
            editor.requestFocus();
        });

        toolbar.fontSize.setOnAction((ev) -> {
            setFontSize(toolbar.fontSize.getSelectionModel().getSelectedItem());
            editor.requestFocus();
        });

        toolbar.textColor.setOnAction((ev) -> {
            setTextColor(toolbar.textColor.getValue());
            editor.requestFocus();
        });

        toolbar.textStyle.setOnAction((ev) -> {
            updateTextStyle();
            editor.requestFocus();
        });

        textStyleProperty().addListener((s,p,c) -> {
            toolbar.setTextStyle(c);
        });

        rulerVisible.subscribe(this::updateRuler);

        // settings
        Settings.endKey.subscribe(this::setEndKey);
        Settings.contentPadding.bindBidirectional(editor.contentPaddingProperty());

        // defaults
        highlightCurrentLine.setSelected(true, false);
        wrapText.setSelected(true, false);

        handleEdit();
        handleSelection();
        setModified(false);
    }

    public MenuBar createMenu() {
        MenuBar m = new MenuBar();
        // file
        FX.menu(m, "File");
        FX.item(m, "New", newDocument).setAccelerator(KeyCombination.keyCombination("shortcut+N"));
        FX.item(m, "Open...", open);
        FX.separator(m);
        FX.item(m, "Save", save).setAccelerator(KeyCombination.keyCombination("shortcut+S"));
        FX.item(m, "Save As...", saveAs).setAccelerator(KeyCombination.keyCombination("shortcut+A"));
        FX.item(m, "Quit", this::quit);

        // edit
        FX.menu(m, "Edit");
        FX.item(m, "Undo", undo);
        FX.item(m, "Redo", redo);
        FX.separator(m);
        FX.item(m, "Cut", cut);
        FX.item(m, "Copy", copy);
        FX.item(m, "Paste", paste);
        FX.item(m, "Paste and Retain Style", pasteUnformatted);

        // format
        FX.menu(m, "Format");
        FX.item(m, "Bold", bold).setAccelerator(KeyCombination.keyCombination("shortcut+B"));
        FX.item(m, "Italic", italic).setAccelerator(KeyCombination.keyCombination("shortcut+I"));
        FX.item(m, "Strike Through", strikeThrough);
        FX.item(m, "Underline", underline).setAccelerator(KeyCombination.keyCombination("shortcut+U"));
        FX.separator(m);
        FX.item(m, "Paragraph...", paragraphStyle);
        FX.item(m, "Tabs...", this::openTabs);

        // view
        FX.menu(m, "View");
        FX.checkItem(m, "Highlight Current Paragraph", highlightCurrentLine);
        FX.checkItem(m, "Show Line Numbers", lineNumbers);
        FX.checkItem(m, "Show Ruler", rulerVisible);
        FX.checkItem(m, "Wrap Text", wrapText);

        // tools
        FX.menu(m, "Tools");
        FX.item(m, "Settings", this::openSettings);

        // help
        FX.menu(m, "Help");
        FX.item(m, "About"); // TODO

        return m;
    }

    public ContextMenu createContextMenu() {
        ContextMenu m = new ContextMenu();

        // otherwise the menu won't be shown
        FX.item(m, "Dummy");

        m.setOnShowing((ev) -> {
            populate(m);
        });
        return m;
    }

    private void populate(ContextMenu m) {
        m.getItems().clear();

        TextPos p = caretAtPopup();
        EmbeddedImage im = embeddedImageAt(p);

        FX.item(m, "Undo", undo);
        FX.item(m, "Redo", redo);
        FX.separator(m);
        FX.item(m, "Cut", cut);
        FX.item(m, "Copy", copy);
        FX.item(m, "Paste", paste);
        FX.item(m, "Paste and Retain Style", pasteUnformatted);
        FX.separator(m);
        FX.item(m, "Select All", selectAll);
        FX.separator(m);
        // TODO Font...
        Menu m2 = FX.menu(m, "Highlight");
        FX.item(m2, "Color 1", () -> toggleBooleanAttribute(StyleAttributeMap.TEXT_HIGHLIGHT_1));
        FX.item(m2, "Color 2", () -> toggleBooleanAttribute(StyleAttributeMap.TEXT_HIGHLIGHT_2));
        FX.item(m2, "Color 3", () -> toggleBooleanAttribute(StyleAttributeMap.TEXT_HIGHLIGHT_3));
        FX.item(m2, "Color 4", () -> toggleBooleanAttribute(StyleAttributeMap.TEXT_HIGHLIGHT_4));
        FX.item(m2, "Color 5", () -> toggleBooleanAttribute(StyleAttributeMap.TEXT_HIGHLIGHT_5));
        m2 = FX.menu(m, "Wavy Underline");
        FX.item(m2, "Color 1", () -> toggleBooleanAttribute(StyleAttributeMap.UNDERLINE_WAVY_1));
        FX.item(m2, "Color 2", () -> toggleBooleanAttribute(StyleAttributeMap.UNDERLINE_WAVY_2));
        FX.item(m2, "Color 3", () -> toggleBooleanAttribute(StyleAttributeMap.UNDERLINE_WAVY_3));
        FX.item(m, "Paragraph...", paragraphStyle);
        if (im != null) {
            m2 = FX.menu(m, "Image");
            Menu m3 = FX.menu(m2, "Width");
            FX.item(m3, "AUTO", () -> formatImage(p, im, EmbeddedImage.AUTO, im.getTargetHeight(), im.isKeepAspectRatio()));
            FX.item(m3, "FIT_WIDTH", () -> formatImage(p, im, EmbeddedImage.FIT_WIDTH, im.getTargetHeight(), im.isKeepAspectRatio()));
            FX.item(m3, "FIT_WIDTH_ALWAYS", () -> formatImage(p, im, EmbeddedImage.FIT_WIDTH_ALWAYS, im.getTargetHeight(), im.isKeepAspectRatio()));
            FX.item(m3, "50", () -> formatImage(p, im, 50, im.getTargetHeight(), im.isKeepAspectRatio()));
            FX.item(m3, "500", () -> formatImage(p, im, 500, im.getTargetHeight(), im.isKeepAspectRatio()));
            m3 = FX.menu(m2, "Height");
            FX.item(m3, "AUTO", () -> formatImage(p, im, im.getTargetWidth(), EmbeddedImage.AUTO, im.isKeepAspectRatio()));
            FX.item(m3, "50", () -> formatImage(p, im, im.getTargetWidth(), 50, im.isKeepAspectRatio()));
            FX.item(m3, "500", () -> formatImage(p, im, im.getTargetWidth(), 500, im.isKeepAspectRatio()));
            FX.checkItem(m2, "Keep Aspect Ratio", im.isKeepAspectRatio(), (v) -> formatImage(p, im, im.getTargetWidth(), im.getTargetHeight(), !im.isKeepAspectRatio()));
            FX.separator(m2);
            FX.item(m2, "Fit to Width", () -> formatImage(p, im, EmbeddedImage.FIT_WIDTH, EmbeddedImage.AUTO, true));
            FX.item(m2, "Fit to Width Always", () -> formatImage(p, im, EmbeddedImage.FIT_WIDTH_ALWAYS, EmbeddedImage.AUTO, true));
            FX.item(m2, "Original Size", () -> formatImage(p, im, EmbeddedImage.AUTO, EmbeddedImage.AUTO, true));
        }
    }

    private void toggleBooleanAttribute(StyleAttribute<Boolean> a) {
        SelectionSegment sel = editor.getSelection();
        if (sel != null) {
            if (!sel.isCollapsed()) {
                StyleAttributeMap attr = editor.getStyleAttributeMap(sel.getMin(), false);
                Boolean on = attr.get(a);
                on = (on == null) ? Boolean.TRUE : !on;
                editor.applyStyle(sel.getMin(), sel.getMax(), StyleAttributeMap.of(a, on));
            }
        }
    }

    // returns caret position only if selection exists and collapsed, otherwise null
    private TextPos caretAtPopup() {
        SelectionSegment sel = editor.getSelection();
        if (sel != null) {
            if (sel.isCollapsed()) {
                return sel.getMin();
            }
        }
        return null;
    }

    private EmbeddedImage embeddedImageAt(TextPos p) {
        if (p != null) {
            StyleAttributeMap a = editor.getStyleAttributeMap(p, false);
            return a.get(StyleAttributeMap.EMBEDDED_IMAGE);
        }
        return null;
    }

    private void formatImage(TextPos p, EmbeddedImage im, double targetWidth, double targetHeight, boolean keepAspectRatio) {
        TextPos start = Utils.leading(p);
        TextPos end = Utils.trailing(p);
        EmbeddedImage updated = im.copy(targetWidth, targetHeight, keepAspectRatio);
        StyleAttributeMap a = StyleAttributeMap.of(StyleAttributeMap.EMBEDDED_IMAGE, updated);
        editor.applyStyle(start, end, a);
    }

    private ContextMenu createRulerPopupMenu() {
        ContextMenu m = new ContextMenu();
        FX.item(m, "Clear Tabs", this::clearTabs);
        FX.separator(m);
        FX.item(m, "Hide Ruler", this::hideRuler);
        return m;
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

    private final void setFile(File f) {
        file.set(f);
    }

    private void handleEdit() {
        setModified(true);
    }

    private void handleSelection() {
        boolean sel = editor.hasNonEmptySelection();
        cut.setEnabled(sel);
        copy.setEnabled(sel);

        if (!sel) {
            StyleAttributeMap a = editor.getActiveStyleAttributeMap();
            toolbar.updateStyles(a);
        }
    }

    public void setFontSize(Double size) {
        apply(StyleAttributeMap.FONT_SIZE, size);
    }

    public void setFontFamily(String name) {
        apply(StyleAttributeMap.FONT_FAMILY, name);
    }

    public void setTextColor(Color color) {
        apply(StyleAttributeMap.TEXT_COLOR, color);
    }

    public void newDocument() {
        if (askToSave()) {
            return;
        }
        setFile(null);
        RichTextModel m = new RichTextModel();
        m.setDefaultTabStops(Settings.DEFAULT_TAB_STOPS);
        editor.setModel(m);
        setModified(false);
    }

    private void open() {
        if (askToSave()) {
            return;
        }

        FileChooser ch = new FileChooser();
        ch.setTitle("Open File");
        ch.getExtensionFilters().addAll(
            filterRich(),
            filterRtf(),
            filterAll()
        );

        File f = ch.showOpenDialog(parentWindow());
        if (f != null) {
            doOpen(f);
        }
    }

    public void openFile(File f) {
        if (askToSave()) {
            return;
        }
        doOpen(f);
    }

    private void doOpen(File f) {
        try {
            setModified(false);
            newDocument();
            DataFormat fmt = guessFormat(f);
            readFile(f, fmt);
        } catch (Exception e) {
            new ExceptionDialog(editor, e).open();
        }
    }

    private void save() {
        File f = getFile();
        if (f == null) {
            f = chooseFileForSave();
            if (f == null) {
                return;
            }
        }

        setFile(f);
        try {
            writeFile(f);
        } catch (Exception e) {
            new ExceptionDialog(editor, e).open();
        }
    }

    private boolean saveAs() {
        File f = chooseFileForSave();
        if (f != null) {
            setFile(f);
            try {
                writeFile(f);
                return true;
            } catch(Exception e) {
                new ExceptionDialog(editor, e).open();
            }
        }
        return false;
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
            filterHtml(),
            filterRtf(),
            filterTxt()
        );

        return ch.showSaveDialog(parentWindow());
    }

    private void readFile(File f, DataFormat fmt) throws Exception {
        try (FileInputStream in = new FileInputStream(f)) {
            editor.read(fmt, in);
            setFile(f);
            editor.setEditable(f.canWrite());
            setModified(false);
        }
    }

    private void writeFile(File f) throws Exception {
        DataFormat fmt = guessFormat(f);
        try (FileOutputStream out = new FileOutputStream(f)) {
            editor.write(fmt, out);
            setFile(f);
            setModified(false);
        }
    }

    private void copy() {
        editor.copy();
    }

    private void cut() {
        editor.cut();
    }

    private void paste() {
        editor.paste();
    }

    private void pasteUnformatted() {
        editor.pastePlainText();
    }

    private void selectAll() {
        editor.selectAll();
    }

    private void redo() {
       editor.redo();
    }

    private void undo() {
        editor.undo();
    }

    private void bold() {
        toggleStyle(bold, StyleAttributeMap.BOLD);
    }

    private void italic() {
        toggleStyle(italic, StyleAttributeMap.ITALIC);
    }

    private void strikeThrough() {
        toggleStyle(strikeThrough, StyleAttributeMap.STRIKE_THROUGH);
    }

    private void underline() {
        toggleStyle(underline, StyleAttributeMap.UNDERLINE);
    }

    private <T> void apply(StyleAttribute<T> attr, T value) {
        TextPos start = editor.getAnchorPosition();
        TextPos end = editor.getCaretPosition();
        if (start == null) {
            return;
        } else if (start.equals(end)) {
            return;
        }

        StyleAttributeMap a = editor.getActiveStyleAttributeMap();
        a = StyleAttributeMap.builder().set(attr, value).build();
        editor.applyStyle(start, end, a);
    }

    private void toggleStyle(FxAction action, StyleAttribute<Boolean> attr) {
        TextPos start = editor.getAnchorPosition();
        TextPos end = editor.getCaretPosition();
        if (start == null) {
            return;
        }

        StyleAttributeMap a = editor.getActiveStyleAttributeMap();
        boolean on = !a.getBoolean(attr);

        if (start.equals(end)) {
            if(on != action.isSelected()) {
                action.setSelected(on, false);
            }
        } else {
            a = StyleAttributeMap.builder().set(attr, on).build();
            editor.applyStyle(start, end, a);
            updateSourceStyles();
        }
        focusEditor();
    }

    public void setTextStyle(TextStyle st) {
        TextPos start = editor.getAnchorPosition();
        TextPos end = editor.getCaretPosition();
        if (start == null) {
            return;
        } else if (start.equals(end)) {
            TextStyle cur = Styles.guessTextStyle(editor.getActiveStyleAttributeMap());
            if (cur == st) {
                return;
            }
            // apply to the whole paragraph
            int ix = start.index();
            start = TextPos.ofLeading(ix, 0);
            end = editor.getParagraphEnd(ix);
        }

        StyleAttributeMap a = Styles.getStyleAttributeMap(st);
        editor.applyStyle(start, end, a);
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
        SelectionSegment sel = editor.getSelection();
        if ((sel == null) || (!sel.isCollapsed())) {
            return null;
        }
        return editor.getActiveStyleAttributeMap();
    }

    private static FileChooser.ExtensionFilter filterAll() {
        return new FileChooser.ExtensionFilter("All Files", "*.*");
    }

    private static FileChooser.ExtensionFilter filterRich() {
        return new FileChooser.ExtensionFilter("Rich Text Files", "*.rich");
    }

    private static FileChooser.ExtensionFilter filterHtml() {
        return new FileChooser.ExtensionFilter("HTML Files", "*.html");
    }

    private static FileChooser.ExtensionFilter filterRtf() {
        return new FileChooser.ExtensionFilter("RTF Files", "*.rtf");
    }

    private static FileChooser.ExtensionFilter filterTxt() {
        return new FileChooser.ExtensionFilter("Text Files", "*.txt");
    }

    private static DataFormat guessFormat(File f) {
        String name = f.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".rich")) {
            return RichTextFormatHandler.DATA_FORMAT;
        } else if (name.endsWith(".rtf")) {
            return DataFormat.RTF;
        } else if (name.endsWith(".html")) {
            return DataFormat.HTML;
        }
        return DataFormat.PLAIN_TEXT;
    }

    public File fileToOpen(List<File> files) {
        if (files.size() == 1) {
            File f = files.get(0);
            String name = f.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".rich")) {
                return f;
            }
        }
        return null;
    }

    public void quit() {
        if (askToSave()) {
            return;
        }
        Platform.exit();
    }

    enum UserChoiceToSave {
        DISCARD_CHANGES,
        RETURN_TO_EDITING,
        SAVE
    }

    private UserChoiceToSave askSaveChanges() {
        Dialog<UserChoiceToSave> d = new Dialog<>();
        d.initOwner(parentWindow());
        d.setTitle("Save Changes?");
        d.setContentText("Do you want to save changes?");

        ButtonType bSave = new ButtonType("Save", ButtonData.YES);
        d.getDialogPane().getButtonTypes().add(bSave);
        ButtonType bReturn = new ButtonType("Return to Editing", ButtonData.CANCEL_CLOSE);
        d.getDialogPane().getButtonTypes().add(bReturn);
        ButtonType bDiscard = new ButtonType("Discard Changes", ButtonData.NO);
        d.getDialogPane().getButtonTypes().add(bDiscard);
        d.showAndWait();

        Object v = d.getResult();
        if (v == bSave) {
            return UserChoiceToSave.SAVE;
        } else if (v == bDiscard) {
            return UserChoiceToSave.DISCARD_CHANGES;
        } else {
            return UserChoiceToSave.RETURN_TO_EDITING;
        }
    }

    /**
     * Checks whether the document has been modified and if so, asks to Save, Discard or Cancel.
     * @return true if the user chose to Cancel
     */
    public boolean askToSave() {
        if (isModified()) {
            switch(askSaveChanges()) {
            case DISCARD_CHANGES:
                return false;
            case SAVE:
                return saveAs();
            case RETURN_TO_EDITING:
            default:
                return true;
            }
        }
        return false;
    }

    private Window parentWindow() {
        return FX.getParentWindow(editor);
    }

    private void updateTextStyle() {
        TextStyle st = toolbar.textStyle.getSelectionModel().getSelectedItem();
        if (st != null) {
            setTextStyle(st);
        }
    }

    private void setEndKey(EndKey v) {
        switch(v) {
        case END_OF_LINE:
            editor.getInputMap().restoreDefaultFunction(RichTextArea.Tag.MOVE_TO_LINE_END);
            break;
        case END_OF_TEXT:
            editor.getInputMap().registerFunction(RichTextArea.Tag.MOVE_TO_LINE_END, this::moveToEndOfText);
            break;
        }
    }

    // this is an illustration.  we could publish the MOVE_TO_END_OF_TEXT_ON_LINE function tag
    private void moveToEndOfText() {
        TextPos p = editor.getCaretPosition();
        if (p != null) {
            editor.executeDefault(RichTextArea.Tag.MOVE_TO_LINE_END);
            TextPos p2 = editor.getCaretPosition();
            if (p2 != null) {
                String text = editor.getPlainText(p2.index());
                int ix = findLastText(text, p2.charIndex());
                if (ix > p.charIndex()) {
                    editor.select(TextPos.ofLeading(p2.index(), ix));
                }
            }
        }
    }

    private static int findLastText(String text, int start) {
        int i = start - 1;
        while (i >= 0) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) {
                return i + 1;
            }
            --i;
        }
        return i;
    }

    private StyleAttributeMap getInsertStyles() {
        StyleAttributeMap.Builder b = StyleAttributeMap.builder();
        if (bold.isSelected()) {
            b.setBold(true);
        }
        String s = toolbar.fontFamily.getSelectionModel().getSelectedItem();
        if (s != null) {
            b.setFontFamily(s);
        }
        if (italic.isSelected()) {
            b.setItalic(true);
        }
        if (strikeThrough.isSelected()) {
            b.setStrikeThrough(true);
        }
        Color c = toolbar.textColor.getValue();
        if (c != null) {
            b.setTextColor(c);
        }
        if (underline.isSelected()) {
            b.setUnderline(true);
        }
        Double v = toolbar.fontSize.getSelectionModel().getSelectedItem();
        if (v != null) {
            b.setFontSize(v);
        }
        return b.build();
    }

    private void focusEditor() {
        editor.requestFocus();
    }

    private void showParagraphDialog() {
        new ParagraphDialog(editor).show();
    }

    private void openSettings() {
        Window w = FX.getParentWindow(editor);
        new SettingsWindow(w).show();
    }

    private void openTabs() {
        new TabsDialog(editor).show();
    }

    private void updateRuler(boolean on) {
        Ruler r = toolbar.setRulerFor(on ? editor : null);
        if (r != null) {
            r.setOnChange(this::handleTabStopChange);
            r.setTabStopPolicy(tabPolicy);
            FX.setPopupMenu(r, this::createRulerPopupMenu);
        }
    }

    private void handleTabStopChange() {
        SelectionSegment sel = editor.getSelection();
        if (sel != null) {
            TabStops stops = new TabStops(tabPolicy.tabStops());
            StyleAttributeMap a = StyleAttributeMap.builder().set(StyleAttributeMap.TAB_STOPS, stops).build();
            int min = sel.getMin().index();
            int max = sel.getMax().index();
            for (int ix = min; ix <= max; ix++) {
                TextPos p = TextPos.ofLeading(ix, 0);
                editor.applyStyle(p, p, a);
            }
        }
    }

    private void clearTabs() {
        tabPolicy.tabStops().clear();
        handleTabStopChange();
    }

    private void hideRuler() {
        rulerVisible.set(false);
    }
}
