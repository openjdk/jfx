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

package javafx.incubator.scene.control.rich.model;

import java.util.ArrayList;
import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.incubator.scene.control.rich.StyleResolver;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.scene.layout.Region;

/**
 * A simple, editable, in-memory StyledTextModel which manages plain text paragraphs.
 * <p>
 * This class provides no styling.  Subclasses might override {@link #getParagraph(int)} to provide
 * syntax highlighting based on the model content.
 */
public class PlainTextModel extends StyledTextModel {
    private final ArrayList<String> paragraphs = new ArrayList<>();
    private final SimpleBooleanProperty editable = new SimpleBooleanProperty(true);

    /**
     * Constructs an empty model.
     */
    public PlainTextModel() {
        registerDataFormatHandler(new PlainTextFormatHandler(), true, true, 0);
    }

    @Override
    public int size() {
        int sz = paragraphs.size();
        // empty model always have one line
        return sz == 0 ? 1 : sz;
    }

    @Override
    public String getPlainText(int index) {
        if (index < paragraphs.size()) {
            return paragraphs.get(index);
        }
        return "";
    }

    @Override
    public RichParagraph getParagraph(int index) {
        String text = getPlainText(index);
        return RichParagraph.builder().
            addSegment(text).
            build();
    }

    @Override
    public boolean isUserEditable() {
        return editable.get();
    }

    public void setEditable(boolean on) {
        editable.set(on);
    }

    /**
     * Determines whether the model is editable.
     * @return the editable property
     */
    public BooleanProperty editableProperty() {
        return editable;
    }

    @Override
    protected int insertTextSegment(int index, int offset, String text, StyleAttrs attrs) {
        String s = getPlainText(index);
        String s2 = insertText(s, offset, text);
        setText(index, s2);
        return text.length();
    }

    private static String insertText(String text, int offset, String toInsert) {
        if (offset >= text.length()) {
            return text + toInsert;
        } else {
            return text.substring(0, offset) + toInsert + text.substring(offset);
        }
    }

    @Override
    protected void insertLineBreak(int index, int offset) {
        if (index >= paragraphs.size()) {
            paragraphs.add("");
        } else {
            String s = paragraphs.get(index);
            if (offset >= s.length()) {
                paragraphs.add(index + 1, "");
            } else {
                setText(index, s.substring(0, offset));
                paragraphs.add(index + 1, s.substring(offset));
            }
        }
    }

    @Override
    protected void removeRegion(TextPos start, TextPos end) {
        int ix = start.index();
        String text = getPlainText(ix);
        String newText;

        if (ix == end.index()) {
            int len = text.length();
            if (end.offset() >= len) {
                newText = text.substring(0, start.offset());
            } else {
                newText = text.substring(0, start.offset()) + text.substring(end.offset());
            }
            setText(ix, newText);
        } else {
            newText = text.substring(0, start.offset()) + paragraphs.get(end.index()).substring(end.offset());
            setText(ix, newText);

            int ct = end.index() - ix;
            ix++;
            for (int i = 0; i < ct; i++) {
                paragraphs.remove(ix);
            }
        }
    }

    private void setText(int index, String text) {
        if (index < paragraphs.size()) {
            paragraphs.set(index, text);
        } else {
            // due to emulated empty paragraph in an empty model
            paragraphs.add(text);
        }
    }

    @Override
    protected void insertParagraph(int index, Supplier<Region> generator) {
        // no-op
    }

    @Override
    public StyleAttrs getStyleAttrs(StyleResolver resolver, TextPos pos) {
        return StyleAttrs.EMPTY;
    }

    /**
     * Adds a paragraph to the end of the document.
     * @param text text to add.  must not contain newlines and other control characters except for TAB.
     */
    public void addParagraph(String text) {
        paragraphs.add(text);
    }

    @Override
    protected final void setParagraphStyle(int ix, StyleAttrs a) {
        // no-op
    }

    @Override
    protected final void applyStyle(int ix, int start, int end, StyleAttrs a, boolean merge) {
        // no-op
    }
}
