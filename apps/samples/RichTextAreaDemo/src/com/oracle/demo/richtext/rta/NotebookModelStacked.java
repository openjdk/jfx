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

package com.oracle.demo.richtext.rta;

import java.util.ArrayList;
import java.util.function.Supplier;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.BasicTextModel;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * Another test model.
 *
 * @author Andy Goryachev
 */
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
        return create(Type.COMMENT, "██This is\na comment cell.██p");
    }

    public static StyledTextModel m2() {
        return create(Type.CODE, "x = 5;\nprint(x);");
    }

    public static StyledTextModel create(Type type, String text) {
        BasicTextModel m;
        switch(type) {
        case CODE:
            m = new BasicTextModel() {
                @Override
                public RichParagraph getParagraph(int index) {
                    String text = getPlainText(index);
                    RichParagraph.Builder b = RichParagraph.builder();
                    b.addWithInlineStyle(text, "-fx-text-fill:darkgreen; -fx-font-family:Monospace;");
                    return b.build();
                }
            };
            break;
        case COMMENT:
            m = new BasicTextModel() {
                @Override
                public RichParagraph getParagraph(int index) {
                    String text = getPlainText(index);
                    RichParagraph.Builder b = RichParagraph.builder();
                    b.addWithInlineStyle(text, "-fx-text-fill:gray;");
                    return b.build();
                }
            };
            break;
        default:
            throw new Error("?" + type);
        }

        m.insertText(TextPos.ZERO, text);
        return m;
    }

    @Override
    public boolean isWritable() {
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
                t.setHighlightCurrentParagraph(true);
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
    protected void removeRange(TextPos start, TextPos end) {
    }

    @Override
    protected int insertTextSegment(int index, int offset, String text, StyleAttributeMap attrs) {
        return 0;
    }

    @Override
    protected void insertLineBreak(int index, int offset) {
    }

    @Override
    protected void insertParagraph(int index, Supplier<Region> generator) {
    }

    @Override
    public StyleAttributeMap getStyleAttributeMap(StyleResolver r, TextPos pos) {
        return StyleAttributeMap.EMPTY;
    }

    @Override
    protected void setParagraphStyle(int ix, StyleAttributeMap paragraphAttrs) {
    }

    @Override
    protected void applyStyle(int ix, int start, int end, StyleAttributeMap a, boolean merge) {
    }
}
