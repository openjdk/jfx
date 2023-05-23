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
package javafx.scene.control.rich.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.rich.StyleResolver;
import javafx.scene.control.rich.TextPos;

/**
 * Editable decorated plain text model.
 */
public class EditableDecoratedModel extends BaseDecoratedModel {
    private final ArrayList<String> paragraphs = new ArrayList<>();

    public EditableDecoratedModel() {
        paragraphs.add("");
        registerDataFormatHandler(new PlainTextFormatHandler(), 0);
    }

    @Override
    public int size() {
        return paragraphs.size();
    }

    @Override
    public String getPlainText(int index) {
        return paragraphs.get(index);
    }

    @Override
    protected int insertTextSegment(StyleResolver resolver, int index, int offset, StyledSegment segment) {
        String s = paragraphs.get(index);
        String text = segment.getText();

        String s2 = insertText(s, offset, text);
        paragraphs.set(index, s2);
        return text.length();
    }

    protected static String insertText(String text, int offset, String toInsert) {
        if (offset >= text.length()) {
            return text + toInsert;
        } else {
            return text.substring(0, offset) + toInsert + text.substring(offset);
        }
    }
    
    @Override
    protected void insertLineBreak(int index, int offset) {
        if(index >= size()) {
            paragraphs.add("");
        } else {
            String s = paragraphs.get(index);
            if(offset >= s.length()) {
                paragraphs.add(index + 1, "");
            } else {
                paragraphs.set(index, s.substring(0, offset));
                paragraphs.add(index + 1, s.substring(offset));
            }
        }
    }

    @Override
    protected void removeRegion(TextPos start, TextPos end) {
        int ix = start.index();
        String text = paragraphs.get(ix);
        String newText;

        if (ix == end.index()) {
            int len = text.length();
            if (end.offset() >= len) {
                newText = text.substring(0, start.offset());
            } else {
                newText = text.substring(0, start.offset()) + text.substring(end.offset());
            }
            paragraphs.set(ix, newText);
        } else {
            newText = text.substring(0, start.offset()) + paragraphs.get(end.index()).substring(end.offset());
            paragraphs.set(ix, newText);

            int ct = end.index() - ix;
            ix++;
            for (int i = 0; i < ct; i++) {
                paragraphs.remove(ix);
            }
        }
    }

    @Override
    protected void insertParagraph(int index, Supplier<Node> generator) {
        // no-op
    }

    @Override
    protected void exportParagraph(int index, int startOffset, int endOffset, StyledOutput out) throws IOException {
        String text = getPlainText(index);
        int len = text.length();
        if (endOffset > len) {
            endOffset = len;
        }

        if ((startOffset != 0) || (endOffset != len)) {
            text = text.substring(startOffset, endOffset);
        }

        StyledSegment seg = StyledSegment.of(text);
        out.append(seg);
    }

    @Override
    protected boolean applyStyleImpl(TextPos start, TextPos end, StyleAttrs attrs) {
        // no-op
        return false;
    }
    
    @Override
    public StyleInfo getStyleInfo(TextPos pos) {
        return StyleInfo.NONE;
    }
}
