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

import java.util.ArrayList;
import java.util.function.Supplier;
import javafx.scene.control.TextArea;
import javafx.incubator.scene.control.rich.RichTextArea;
import javafx.incubator.scene.control.rich.StyleResolver;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.BasePlainTextModel;
import javafx.incubator.scene.control.rich.model.RichParagraph;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.incubator.scene.control.rich.model.StyledSegment;
import javafx.incubator.scene.control.rich.model.StyledTextModel;
import javafx.scene.layout.Region;

public class NotebookModelStacked extends StyledTextModel {
    enum Type {
        CODE,
        COMMENT,
        TEXTAREA,
    }
    
    private final ArrayList<Object> paragraphs = new ArrayList<>();

    public NotebookModelStacked() {
        paragraphs.add(m1());
        paragraphs.add(Type.TEXTAREA);
        paragraphs.add(m2());
    }

    public static StyledTextModel m1() {
        return create(Type.COMMENT, "This is", "a comment cell.");
    }

    public static StyledTextModel m2() {
        return create(Type.CODE, "x = 5;", "print(x);");
    }

    public static StyledTextModel create(Type type, String ... text) {
        BasePlainTextModel m;
        switch(type) {
        case CODE:
            m = new BasePlainTextModel() {
                @Override
                public RichParagraph getParagraph(int index) {
                    String text = getPlainText(index);
                    RichParagraph p = new RichParagraph();
                    p.addSegment(text, "-fx-text-fill:darkgreen; -fx-font-family:Monospace;", null);
                    return p;
                }
            };
            break;
        case COMMENT:
            m = new BasePlainTextModel() {
                @Override
                public RichParagraph getParagraph(int index) {
                    String text = getPlainText(index);
                    RichParagraph p = new RichParagraph();
                    p.addSegment(text, "-fx-text-fill:gray;", null);
                    return p;
                }
            };
            break;
        default:
            throw new Error("?" + type);
        }

        for (String s : text) {
            m.addParagraph(s);
        }
        return m;
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
        return "";
    }

    @Override
    public RichParagraph getParagraph(int index) {
        Object x = paragraphs.get(index);
        if(x instanceof StyledTextModel m) {
            return RichParagraph.of(() -> {
                RichTextArea t = new RichTextArea(m);
                t.setMaxWidth(Double.POSITIVE_INFINITY);
                t.setWrapText(true);
                t.setUseContentHeight(true);
                return t;
            });
        } else if(x instanceof Type type) {
            switch(type) {
            case TEXTAREA:
                return RichParagraph.of(() -> {
                    TextArea t = new TextArea();
                    t.setMaxWidth(Double.POSITIVE_INFINITY);
                    t.setWrapText(true);
                    return t;
                });
            }
        }
        throw new Error("?" + x);
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
    protected void insertParagraph(int index, Supplier<Region> generator) {
    }

    @Override
    public StyleAttrs getStyleAttrs(TextPos pos) {
        return StyleAttrs.EMPTY;
    }

    @Override
    protected void applyStyle(int ix, StyleAttrs paragraphAttrs) {
    }

    @Override
    protected void applyStyle(int ix, int start, int end, StyleAttrs a) {
    }
}
