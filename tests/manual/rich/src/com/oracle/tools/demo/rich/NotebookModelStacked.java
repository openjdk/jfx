/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.demo.rich;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.rich.RichTextArea;
import javafx.scene.control.rich.StyleResolver;
import javafx.scene.control.rich.TextCell;
import javafx.scene.control.rich.TextPos;
import javafx.scene.control.rich.model.EditableDecoratedModel;
import javafx.scene.control.rich.model.StyleAttrs;
import javafx.scene.control.rich.model.StyleInfo;
import javafx.scene.control.rich.model.StyledOutput;
import javafx.scene.control.rich.model.StyledSegment;
import javafx.scene.control.rich.model.StyledTextModel;
import javafx.scene.control.rich.model.SyntaxDecorator;

public class NotebookModelStacked extends StyledTextModel {
    enum Type {
        CODE,
        COMMENT,
        TEXTAREA,
    }
    
    private final ArrayList<StyledTextModel> paragraphs = new ArrayList<>();

    public NotebookModelStacked() {
        add(Type.COMMENT, "This is", "a comment cell.");
        add(Type.TEXTAREA);
        add(Type.CODE, "x = 5;", "print(x);"); 
    }
    
    public void add(Type type, String ... text) {
        EditableDecoratedModel m = new EditableDecoratedModel();
        switch(type) {
        case CODE:
            m.setDecorator(new SyntaxDecorator() {
                @Override
                public TextCell createTextCell(int index, String text) {
                    TextCell c = new TextCell(index);
                    c.addSegment(text, "-fx-text-fill:darkgreen; -fx-font-family:Monospace;", null);
                    return c;
                }
            });
            break;
        case COMMENT:
            m.setDecorator(new SyntaxDecorator() {
                @Override
                public TextCell createTextCell(int index, String text) {
                    TextCell c = new TextCell(index);
                    c.addSegment(text, "-fx-text-fill:gray;", null);
                    return c;
                }
            });
            break;
        case TEXTAREA:
            paragraphs.add(null);
            return;
        }
        for(String s: text) {
            m.addParagraph(s);
        }
        paragraphs.add(m);
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public int size() {
        return paragraphs.size();
    }

    @Override
    public String getPlainText(int index) {
        return null;
    }

    private boolean addli = true; // FIX
    @Override
    public TextCell createTextCell(int index) {
        StyledTextModel m = paragraphs.get(index);
        if(m == null) {
            TextArea t = new TextArea();
            t.setMaxWidth(Double.POSITIVE_INFINITY);
            t.setWrapText(true);
            return new TextCell(index, t);
        }
        RichTextArea t = new RichTextArea(m);
        if(addli) {
            t.widthProperty().addListener((s,p,c) -> {
                double ww = c.doubleValue();
                System.out.println("TextCell.width=" + c); // FIX
            });
            addli = false;
        }
        t.setMaxWidth(Double.POSITIVE_INFINITY);
        t.setWrapText(true);
        t.setUseContentHeight(true);
        return new TextCell(index, t);
    }

    @Override
    protected void removeRegion(TextPos start, TextPos end) {
    }

    @Override
    protected int insertTextSegment(StyleResolver resolver, int index, int offset, StyledSegment text) {
        return 0;
    }

    @Override
    protected void insertLineBreak(int index, int offset) {
    }

    @Override
    protected void insertParagraph(int index, Supplier<Node> generator) {
    }

    @Override
    protected void exportParagraph(int index, int startOffset, int endOffset, StyledOutput out) throws IOException {
    }

    @Override
    protected boolean applyStyleImpl(TextPos start, TextPos end, StyleAttrs attrs) {
        return false;
    }

    @Override
    public StyleInfo getStyleInfo(TextPos pos) {
        return null;
    }
}
