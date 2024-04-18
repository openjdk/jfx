/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.demo.rich.notebook;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import com.oracle.demo.rich.codearea.JavaSyntaxDecorator;
import com.oracle.demo.rich.notebook.data.CellInfo;
import com.oracle.demo.rich.util.FX;
import jfx.incubator.scene.control.rich.CodeArea;
import jfx.incubator.scene.control.rich.CodeTextModel;
import jfx.incubator.scene.control.rich.RichTextArea;
import jfx.incubator.scene.control.rich.TextPos;
import jfx.incubator.scene.control.rich.model.SegmentBuffer;

/**
 * Pane holds the visuals for the cell: source editor, output pane, execution label, current cell highlight.
 */
public class CellPane extends GridPane {
    private static final PseudoClass EXECUTING = PseudoClass.getPseudoClass("executing");
    private static final Font FONT = new Font("Iosevka Fixed SS16", 12);
    private static final Insets OUTPUT_PADDING = new Insets(0, 0, 3, 0);
    private final CellInfo cell;
    private final Region codeBar;
    private final Label execLabel;
    private final BorderPane sourcePane;
    private final BorderPane outputPane;
    private final SimpleBooleanProperty active = new SimpleBooleanProperty();

    // TODO the side bar and exec label turn orange when source has been edited and the old output is still present
    // also exec label shows an asterisk *[N]
    public CellPane(CellInfo c) {
        super(3, 0);

        this.cell = c;
        FX.style(this, "cell-pane");

        codeBar = new Region();
        codeBar.setMinWidth(6);
        codeBar.setMaxWidth(6);
        FX.style(codeBar, "code-bar");

        execLabel = new Label(cell.isCode() ? "[ ]:" : null);
        execLabel.setAlignment(Pos.TOP_RIGHT);
        execLabel.setMinWidth(50);
        FX.style(execLabel, "exec-label");
        // TODO bind to font property, set preferred width
        setValignment(execLabel, VPos.TOP);

        sourcePane = new BorderPane();
        setHgrow(sourcePane, Priority.ALWAYS);
        setVgrow(sourcePane, Priority.NEVER);

        outputPane = new BorderPane();
        FX.style(outputPane, "output-pane");
        outputPane.setMaxHeight(200);
        setHgrow(outputPane, Priority.ALWAYS);
        setVgrow(outputPane, Priority.NEVER);
        setMargin(outputPane, OUTPUT_PADDING);

        int r = 0;
        add(codeBar, 0, r, 1, 2);
        add(execLabel, 1, r);
        add(sourcePane, 2, r);
        r++;
        add(outputPane, 2, r);

        updateContent();

        active.addListener((s,p,v) -> {
            FX.style(this, "active-cell", v);
        });
    }

    private void updateContent() {
        RichTextArea ed = createEditor();
        sourcePane.setCenter(ed);
        outputPane.setCenter(null);
        ed.applyCss();
    }

    private RichTextArea createEditor() {
        CellType t = cell.getCellType();
        switch (t) {
        case CODE:
            CodeArea c = new CodeArea();
            FX.style(c, "code-cell");
            c.setFont(FONT);
            c.setModel(cell.getModel());
            c.setSyntaxDecorator(new JavaSyntaxDecorator());
            c.setUseContentHeight(true);
            c.setWrapText(true);
            return c;
        case TEXT:
            RichTextArea r = new RichTextArea();
            FX.style(r, "text-cell");
            r.setModel(cell.getModel());
            r.setUseContentHeight(true);
            r.setWrapText(true);
            return r;
        }
        return null;
    }

    public final CellInfo getCellInfo() {
        return cell;
    }

    public final CellType getCellType() {
        return cell.getCellType();
    }

    public void setExecuting() {
        execLabel.setText("[*]:");
        FX.style(execLabel, EXECUTING, true);

        outputPane.setCenter(null);
    }

    public void setResult(Object result, int execCount) {
        String s = (execCount <= 0) ? " " : String.valueOf(execCount);
        execLabel.setText("[" + s + "]:");
        FX.style(execLabel, EXECUTING, false);

        Node n = createResultNode(result);
        outputPane.setCenter(n);
    }

    private Node createResultNode(Object result) {
        if(result != null) {
            if(result instanceof Supplier gen) {
                Object v = gen.get();
                if(v instanceof Node n) {
                    return n;
                }
            } else if(result instanceof Throwable err) {
                StringWriter sw = new StringWriter();
                PrintWriter wr = new PrintWriter(sw);
                err.printStackTrace(wr);
                String text = sw.toString();
                return textViewer(text, true);
            } else if(result instanceof Image im) {
                ImageView v = new ImageView(im);
                ScrollPane sp = new ScrollPane(v);
                FX.style(sp, "image-result");
                return sp;
            } else if(result instanceof CodeTextModel m) {
                CodeArea t = new CodeArea(m);
                t.setMinHeight(300);
                t.setSyntaxDecorator(new SimpleJsonDecorator());
                t.setFont(FONT);
                t.setWrapText(false);
                t.setEditable(false);
                t.setLineNumbersEnabled(true);
                FX.style(t, "output-text");
                return t;
            } else {
                String text = result.toString();
                return textViewer(text, false);
            }
        }
        return null;
    }

    private static CodeTextModel from(String text) throws IOException {
        CodeTextModel m = new CodeTextModel();
        m.insertText(TextPos.ZERO, text);
        return m;
    }

    private Node textViewer(String text, boolean error) {
        try {
            CodeTextModel m = from(text);

            CodeArea t = new CodeArea();
            t.setFont(FONT);
            t.setModel(m);
            t.setUseContentHeight(true);
            t.setWrapText(false);
            t.setEditable(false);
            FX.style(t, error ? "output-error" : "output-text");
            return t;
        } catch (IOException wontHappen) {
            return null;
        }
    }

    // FIX does not work!
    public void focusLater() {
        Platform.runLater(() -> {
            Node n = sourcePane.getCenter();
            if (n instanceof RichTextArea a) {
                a.requestFocus();
            }
        });
    }

    public RichTextArea getSourceEditor() {
        Node n = sourcePane.getCenter();
        if (n instanceof RichTextArea r) {
            return r;
        }
        return null;
    }

    /**
     * Splits the cell at the current source editor caret position.
     * TODO split into three parts when non-empty selection exists.
     * @return the list of cells resulting from the split
     */
    public List<CellPane> split() {
        RichTextArea r = getSourceEditor();
        if (r != null) {
            TextPos start = r.getAnchorPosition();
            if (start != null) {
                TextPos end = r.getCaretPosition();
                if (start.equals(end)) {
                    return splitInTwo(start);
                } else {
                    // TODO split into 3 parts?
                }
            }
        }
        return null;
    }

    private List<CellPane> splitInTwo(TextPos p) {
        RichTextArea ed = getSourceEditor();
        CellType t = getCellType();

        try {
            CellPane cell1 = new CellPane(new CellInfo(t));
            insert(ed, TextPos.ZERO, p, cell1.getSourceEditor(), TextPos.ZERO);

            CellPane cell2 = new CellPane(new CellInfo(t));
            insert(ed, p, ed.getEndTextPos(), cell2.getSourceEditor(), TextPos.ZERO);

            return List.of(cell1, cell2);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void insert(RichTextArea src, TextPos start, TextPos end, RichTextArea tgt, TextPos pos) throws IOException {
        SegmentBuffer b = new SegmentBuffer();
        src.getModel().export(start, end, b.getStyledOutput());
        tgt.insertText(pos, b.getStyledInput());
    }

    public void setActive(boolean on) {
        active.set(on);
    }
}
