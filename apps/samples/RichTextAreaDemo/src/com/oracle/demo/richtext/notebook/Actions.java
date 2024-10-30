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

package com.oracle.demo.richtext.notebook;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.DataFormat;
import com.oracle.demo.richtext.common.Styles;
import com.oracle.demo.richtext.common.TextStyle;
import com.oracle.demo.richtext.notebook.data.CellInfo;
import com.oracle.demo.richtext.notebook.data.Notebook;
import com.oracle.demo.richtext.util.FX;
import com.oracle.demo.richtext.util.FxAction;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.ContentChange;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * This class reacts to changes in application state such as currently active cell,
 * caret, selection, model, etc.; then updates the actions disabled and selected properties.
 * <p>
 * JavaFX has no actions (yet), so here we are using FxActions from
 * https://github.com/andy-goryachev/AppFramework (with permission from the author).
 *
 * @author Andy Goryachev
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
    // cells
    public final FxAction copyCell = new FxAction(this::copyCell);
    public final FxAction cutCell = new FxAction(this::cutCell);
    public final FxAction deleteCell = new FxAction(this::deleteCell);
    public final FxAction insertCellBelow = new FxAction(this::insertCellBelow);
    public final FxAction mergeCellAbove = new FxAction(this::mergeCellAbove);
    public final FxAction mergeCellBelow = new FxAction(this::mergeCellBelow);
    public final FxAction moveCellDown = new FxAction(this::moveCellDown);
    public final FxAction moveCellUp = new FxAction(this::moveCellUp);
    public final FxAction pasteCellBelow = new FxAction(this::pasteCellBelow);
    public final FxAction runAndAdvance = new FxAction(this::runAndAdvance);
    public final FxAction runAll = new FxAction(this::runAll);
    public final FxAction splitCell = new FxAction(this::splitCell);

    private enum EditorType {
        CODE,
        NONE,
        OUTPUT,
        TEXT,
    }

    private final NotebookWindow window;
    private final DemoScriptEngine engine;
    private final ObservableList<CellPane> cellPanes = FXCollections.observableArrayList();
    private final ReadOnlyObjectWrapper<CellPane> activeCellPane = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper modified = new ReadOnlyBooleanWrapper();
    private final ReadOnlyObjectWrapper<File> file = new ReadOnlyObjectWrapper<>();
    private final SimpleBooleanProperty executing = new SimpleBooleanProperty();
    private final SimpleObjectProperty<RichTextArea> editor = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<EditorType> editorType = new SimpleObjectProperty<>(EditorType.NONE);
    private final SimpleObjectProperty<StyleAttributeMap> styles = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<TextStyle> textStyle = new SimpleObjectProperty<>();
    private final BooleanBinding disabledStyleEditing;
    private int sequenceNumber;

    public Actions(NotebookWindow w) {
        this.window = w;

        engine = new DemoScriptEngine();

        BooleanBinding disabledEditing = Bindings.createBooleanBinding(
            () -> {
                if (isExecuting()) {
                    return true;
                }
                RichTextArea r = editor.get();
                if (r == null) {
                    return true;
                }
                if (r.isEditable() && (r.getModel() != null) && (r.getModel().isWritable())) {
                    return false;
                }
                return true;
            },
            editor,
            executing
        );

        disabledStyleEditing = Bindings.createBooleanBinding(
            () -> {
                if (isExecuting()) {
                    return true;
                }
                return (editorType.get() != EditorType.TEXT);
            },
            editorType,
            executing
        );

        BooleanBinding cellActionsDisabled = Bindings.createBooleanBinding(
            () -> {
                if(isExecuting()) {
                    return true;
                }
                CellPane p = getActiveCellPane();
                return p == null;
            },
            activeCellPane,
            executing
        );

        BooleanBinding runDisabled = Bindings.createBooleanBinding(
            () -> {
                if(isExecuting()) {
                    return true;
                }
                CellType p = getActiveCellType();
                return p != CellType.CODE;
            },
            activeCellPane,
            executing
        );

        SimpleBooleanProperty redoDisabled = new SimpleBooleanProperty();
        SimpleBooleanProperty undoDisabled = new SimpleBooleanProperty();

        // file actions
        open.setDisabled(true);
        save.setDisabled(true);

        // style actions
        bold.disabledProperty().bind(disabledStyleEditing);
        italic.disabledProperty().bind(disabledStyleEditing);
        strikeThrough.disabledProperty().bind(disabledStyleEditing);
        underline.disabledProperty().bind(disabledStyleEditing);

        // editing actions
        copy.setEnabled(true); // always
        cut.disabledProperty().bind(disabledEditing);
        paste.disabledProperty().bind(disabledEditing);
        pasteUnformatted.disabledProperty().bind(disabledEditing);
        selectAll.setEnabled(true); // always

        // undo/redo actions
        redo.disabledProperty().bind(redoDisabled);
        undo.disabledProperty().bind(undoDisabled);

        // cell actions
        copyCell.setDisabled(true);
        cutCell.setDisabled(true);
        deleteCell.disabledProperty().bind(cellActionsDisabled);
        insertCellBelow.disabledProperty().bind(cellActionsDisabled);
        mergeCellAbove.setDisabled(true);
        mergeCellBelow.setDisabled(true);
        moveCellDown.disabledProperty().bind(cellActionsDisabled);
        moveCellUp.disabledProperty().bind(cellActionsDisabled);
        pasteCellBelow.setDisabled(true);
        runAndAdvance.disabledProperty().bind(runDisabled);
        runAll.setDisabled(true);
        splitCell.disabledProperty().bind(disabledEditing);

        // listeners

        styles.addListener((s,p,a) -> {
            bold.setSelected(hasStyle(a, StyleAttributeMap.BOLD), false);
            italic.setSelected(hasStyle(a, StyleAttributeMap.ITALIC), false);
            strikeThrough.setSelected(hasStyle(a, StyleAttributeMap.STRIKE_THROUGH), false);
            underline.setSelected(hasStyle(a, StyleAttributeMap.UNDERLINE), false);
        });

        ChangeListener<Node> focusOwnerListener = (src, old, node) -> {
            CellPane p = FX.findParentOf(CellPane.class, node);
            if (p == null) {
                return;
            }

            RichTextArea r = FX.findParentOf(RichTextArea.class, node);
            editor.set(r);

            EditorType t = getEditorType(r);
            editorType.set(t);
            updateSourceStyles();
        };

        window.sceneProperty().addListener((src, old, cur) -> {
           if(old != null) {
               old.focusOwnerProperty().removeListener(focusOwnerListener);
           }
           if(cur != null) {
               cur.focusOwnerProperty().addListener(focusOwnerListener);
           }
        });

        StyledTextModel.Listener changeListener = new StyledTextModel.Listener() {
            @Override
            public void onContentChange(ContentChange ch) {
                if (ch.isEdit()) {
                    handleEdit();
                } else {
                    if (editorType.get() == EditorType.TEXT) {
                        handleEdit();
                    }
                }
            }
        };

        InvalidationListener selectionListener = (p) -> {
            updateSourceStyles();
        };

        editor.addListener((src, old, ed) -> {
            if (old != null) {
                if (isSourceEditor(old)) {
                    old.getModel().removeListener(changeListener);
                    old.selectionProperty().removeListener(selectionListener);
                }
            }

            redoDisabled.unbind();
            redoDisabled.set(true);
            undoDisabled.unbind();
            undoDisabled.set(true);

            if (ed != null) {
                if (isSourceEditor(ed)) {
                    ed.getModel().addListener(changeListener);
                    ed.selectionProperty().addListener(selectionListener);
                    redoDisabled.bind(executing.or(ed.redoableProperty().not()));
                    undoDisabled.bind(executing.or(ed.undoableProperty().not()));
                }
            }
        });

        updateSourceStyles();

        activeCellPane.addListener((src, prev, cur) -> {
            if (prev != null) {
                prev.setActive(false);
            }
            if (cur != null) {
                cur.setActive(true);
            }
        });
    }

    private EditorType getEditorType(RichTextArea r) {
        if (r == null) {
            return EditorType.NONE;
        } else if (r instanceof CodeArea) {
            if (r.isEditable()) {
                return EditorType.CODE;
            } else {
                return EditorType.OUTPUT;
            }
        }
        return EditorType.TEXT;
    }

    private boolean isSourceEditor(RichTextArea r) {
        EditorType t = getEditorType(r);
        switch (t) {
        case CODE:
        case TEXT:
            return true;
        }
        return false;
    }

    private void updateSourceStyles() {
        StyleAttributeMap a = getSourceStyleAttrs();
        if (a != null) {
            styles.set(a);

            TextStyle st = Styles.guessTextStyle(a);
            textStyle.set(st);
        }
    }

    public final ObjectProperty<TextStyle> textStyleProperty() {
        return textStyle;
    }

    private StyleAttributeMap getSourceStyleAttrs() {
        RichTextArea r = editor.get();
        EditorType t = getEditorType(r);
        switch (t) {
        case TEXT:
            SelectionSegment sel = r.getSelection();
            if ((sel == null) || (!sel.isCollapsed())) {
                return null;
            }
            return r.getActiveStyleAttributeMap();
        }
        return null;
    }

    private boolean hasStyle(StyleAttributeMap attrs, StyleAttribute<Boolean> a) {
        return attrs == null ? false : Boolean.TRUE.equals(attrs.get(a));
    }

    private final boolean isExecuting() {
        return executing.get();
    }

    private void setExecuting(boolean on) {
        executing.set(on);
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

    public void newDocument() {
//        if (askToSave()) {
//            return;
//        }
        Notebook n = new Notebook();
        n.add(new CellInfo(CellType.CODE));
        window.setNotebook(n);
    }

    private void open() {
//        if (askToSave()) {
//            return;
//        }
//
//        FileChooser ch = new FileChooser();
//        ch.setTitle("Open File");
//        // TODO add extensions
//        Window w = FX.getParentWindow(control);
//        File f = ch.showOpenDialog(w);
//        if (f != null) {
//            try {
//                readFile(f, RichTextFormatHandler.DATA_FORMAT);
//            } catch (Exception e) {
//                new ExceptionDialog(control, e).open();
//            }
//        }
    }

    // FIX this is too simplistic, need save() and save as...
    private void save() {
//        File f = getFile();
//        if (f == null) {
//            FileChooser ch = new FileChooser();
//            ch.setTitle("Save File");
//            // TODO add extensions
//            Window w = FX.getParentWindow(control);
//            f = ch.showSaveDialog(w);
//            if (f == null) {
//                return;
//            }
//        }
//        try {
//            writeFile(f, RichTextFormatHandler.DATA_FORMAT);
//        } catch (Exception e) {
//            new ExceptionDialog(control, e).open();
//        }
    }

    // returns true if the user chose to Cancel
    private boolean askToSave() {
//        if (isModified()) {
//            // alert: has been modified. do you want to save?
//            Alert alert = new Alert(AlertType.CONFIRMATION);
//            alert.initOwner(FX.getParentWindow(control));
//            alert.setTitle("Document is Modified");
//            alert.setHeaderText("Do you want to save this document?");
//            ButtonType delete = new ButtonType("Delete");
//            ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
//            ButtonType save = new ButtonType("Save", ButtonData.APPLY);
//            alert.getButtonTypes().setAll(
//                delete,
//                cancel,
//                save
//            );
//
//            File f = getFile();
//            // FIX format selector is not needed!
//            SavePane sp = new SavePane();
//            sp.setFile(f);
//            alert.getDialogPane().setContent(sp);
//            Optional<ButtonType> result = alert.showAndWait();
//            if (result.isPresent()) {
//                ButtonType t = result.get();
//                if (t == delete) {
//                    return false;
//                } else if (t == cancel) {
//                    return true;
//                } else {
//                    // save using info in the panel
//                    f = sp.getFile();
//                    DataFormat fmt = sp.getFileFormat();
//                    // FIX
//                    fmt = RichTextFormatHandler.DATA_FORMAT;
//
//                    try {
//                        writeFile(f, fmt);
//                    } catch (Exception e) {
//                        new ExceptionDialog(control, e).open();
//                        return true;
//                    }
//                }
//            } else {
//                return true;
//            }
//        }
        return false;
    }

    private void readFile(File f, DataFormat fmt) throws Exception {
//        try (FileInputStream in = new FileInputStream(f)) {
//            control.read(fmt, in);
//            file.set(f);
//            modified.set(false);
//        }
    }

    private void writeFile(File f, DataFormat fmt) throws Exception {
//        try (FileOutputStream out = new FileOutputStream(f)) {
//            control.write(fmt, out);
//            file.set(f);
//            modified.set(false);
//        }
    }

    private void runAll() {
        // TODO
    }

    public void runAndAdvance() {
        CellPane p = getActiveCellPane();
        if (p != null) {
            CellInfo cell = p.getCellInfo();
            if (cell.isCode()) {
                String src = cell.getSource();
                runScript(p, src, true);
            }
        }
    }

    private void runScript(CellPane p, String src, boolean advance) {
        setExecuting(true);
        p.setExecuting();

        Thread t = new Thread("executing script [" + (sequenceNumber + 1) + "]") {
            @Override
            public void run() {
                Object r;
                try {
                    r = engine.executeScript(src);
                } catch (Throwable e) {
                    r = e;
                }
                handleCompletion(p, r, advance);
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /** this method is called from a background thread */
    private void handleCompletion(CellPane p, Object result, boolean advance) {
        Platform.runLater(() -> {
            setExecuting(false);
            p.setResult(result, ++sequenceNumber);
            if (advance) {
                int ix = getActiveCellIndex();
                ix++;
                if (ix < cellPanes.size()) {
                    CellPane next = cellPanes.get(ix);
                    setActiveCellPane(next);
                    next.focusLater();
                }
            }
        });
    }

    public final ObservableList<CellPane> getCellPanes() {
        return cellPanes;
    }

    public final void setActiveCellPane(CellPane p) {
        activeCellPane.set(p);
    }

    public final CellPane getActiveCellPane() {
        return activeCellPane.get();
    }

    private CellType getActiveCellType() {
        CellPane p = getActiveCellPane();
        return (p == null ? null : p.getCellType());
    }

    private RichTextArea getSourceEditor() {
        CellPane p = getActiveCellPane();
        return (p == null ? null : p.getSourceEditor());
    }

    public void setNotebook(Notebook b) {
        int sz = b == null ? 0 : b.size();
        ArrayList<CellPane> ps = new ArrayList<>(sz);
        if (b != null) {
            for (int i = 0; i < sz; i++) {
                CellInfo cell = b.getCell(i);
                ps.add(new CellPane(cell));
            }
        }
        cellPanes.setAll(ps);
        setModified(false);
    }

    public void copy() {
        whenCell((t) -> t.copy());
    }

    public void cut() {
        whenCell((t) -> t.cut());
    }

    public void paste() {
        whenCell((t) -> t.paste());
    }

    public void pasteUnformatted() {
        whenCell((t) -> t.pastePlainText());
    }

    private void whenCell(Consumer<RichTextArea> c) {
        whenCell(null, c);
    }

    private void whenCell(CellType type, Consumer<RichTextArea> c) {
        CellPane p = getActiveCellPane();
        if (p != null) {
            if (type != null) {
                if (type != p.getCellType()) {
                    return;
                }
            }
            RichTextArea r = p.getSourceEditor();
            if (r != null) {
                c.accept(r);
            }
        }
    }

    public int getActiveCellIndex() {
        CellPane p = getActiveCellPane();
        return cellPanes.indexOf(p);
    }

    public void insertCellBelow() {
        int ix = getActiveCellIndex();
        if (ix < 0) {
            ix = 0;
        }
        CellInfo cell = new CellInfo(CellType.CODE);
        CellPane p = new CellPane(cell);
        add(ix + 1, p);
        p.focusLater();
    }

    public void moveCellDown() {
        int ix = getActiveCellIndex();
        if (ix >= 0) {
            if (ix + 1 < cellPanes.size()) {
                CellPane p = cellPanes.remove(ix);
                add(ix + 1, p);
            }
        }
    }

    public void moveCellUp() {
        int ix = getActiveCellIndex();
        if (ix > 0) {
            CellPane p = cellPanes.remove(ix);
            add(ix - 1, p);
        }
    }

    private void add(int ix, CellPane p) {
        if (ix < cellPanes.size()) {
            cellPanes.add(ix, p);
        } else {
            cellPanes.add(p);
        }
    }

    public void deleteCell() {
        if (cellPanes.size() > 1) {
            int ix = getActiveCellIndex();
            if (ix >= 0) {
                cellPanes.remove(ix);
            }
        }
    }

    public void selectAll() {
        whenCell((c) -> {
            c.selectAll();
         });
    }

    public void redo() {
        whenCell((c) -> {
           c.redo();
        });
    }

    public void undo() {
        whenCell((c) -> {
            c.undo();
        });
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
        whenCell(CellType.TEXT, (c) -> {
            TextPos start = c.getAnchorPosition();
            TextPos end = c.getCaretPosition();
            if (start == null) {
                return;
            } else if (start.equals(end)) {
                // apply to the whole paragraph
                int ix = start.index();
                start = TextPos.ofLeading(ix, 0);
                end = c.getParagraphEnd(ix);
            }

            StyleAttributeMap a = c.getActiveStyleAttributeMap();
            boolean on = !a.getBoolean(attr);
            a = StyleAttributeMap.builder().set(attr, on).build();
            c.applyStyle(start, end, a);
            updateSourceStyles();
        });
    }

    public void setTextStyle(TextStyle st) {
        whenCell(CellType.TEXT, (c) -> {
            TextPos start = c.getAnchorPosition();
            TextPos end = c.getCaretPosition();
            if (start == null) {
                return;
            } else if (start.equals(end)) {
                TextStyle cur = Styles.guessTextStyle(c.getActiveStyleAttributeMap());
                if (cur == st) {
                    return;
                }
                // apply to the whole paragraph
                int ix = start.index();
                start = TextPos.ofLeading(ix, 0);
                end = c.getParagraphEnd(ix);
            }

            StyleAttributeMap a = Styles.getStyleAttributeMap(st);
            c.applyStyle(start, end, a);
            updateSourceStyles();
        });
    }

    public void setActiveCellType(CellType t) {
        if (t != null) {
            CellPane p = getActiveCellPane();
            int ix = cellPanes.indexOf(p);
            if (ix >= 0) {
                CellInfo cell = p.getCellInfo();
                if (t != cell.getCellType()) {
                    cell.setCellType(t);
                    p = new CellPane(cell);
                    cellPanes.set(ix, p);
                    p.focusLater();
                }
            }
        }
    }

    private void copyCell() {
        // TODO
    }

    private void cutCell() {
        // TODO
    }

    private void mergeCellAbove() {
        // TODO
    }

    private void mergeCellBelow() {
        // TODO
    }

    private void pasteCellBelow() {
        // TODO
    }

    private void splitCell() {
        whenCell((c) -> {
            int ix = getActiveCellIndex();
            if(ix < 0) {
                return;
            }
            CellPane p = cellPanes.get(ix);
            List<CellPane> ps = p.split();
            if(ps == null) {
                return;
            }
            cellPanes.remove(ix);
            cellPanes.addAll(ix, ps);
        });
    }

    public BooleanBinding disabledStyleEditingProperty() {
        return disabledStyleEditing;
    }
}
