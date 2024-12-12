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

package jfx.incubator.scene.control.richtext.model;

import java.util.ArrayList;
import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.Region;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * A StyledTextModel based on plain text paragraphs.
 * <p>
 * This class provides no styling.  Subclasses might override {@link #getParagraph(int)} to provide
 * syntax highlighting based on the model content.
 * <p>
 * This model supports custom content storage mechanism via {@link BasicTextModel.Content}.  By default,
 * the model provides an in-memory storage via its {@link BasicTextModel.InMemoryContent} implementation.
 *
 * @since 24
 */
public class BasicTextModel extends StyledTextModel {
    /**
     * This interface describes the underlying storage mechanism for the BasicTextModel.
     */
    public interface Content {
        /**
         * Returns the number of paragraphs in this content.
         * @return the number paragraphs
         */
        public int size();

        /**
         * Returns the text string for the specified paragraph index.  The returned text string cannot be null
         * and must not contain any control characters other than TAB.
         * The caller should never attempt to ask for a paragraph outside of the valid range.
         *
         * @param index the paragraph index in the range (0...{@link #size()})
         * @return the text string or null
         */
        public String getText(int index);

        /**
         * This method is called to insert a single text segment at the given position.
         * The {@code BasicTextModel} guarantees that this method is only called when the content is writable.
         *
         * @param index the paragraph index
         * @param offset the insertion offset within the paragraph
         * @param text the text to insert
         * @param attrs the style attributes
         * @return the number of characters inserted
         */
        public int insertTextSegment(int index, int offset, String text, StyleAttributeMap attrs);

        /**
         * Inserts a line break.
         * The {@code BasicTextModel} guarantees that this method is only called when the content is writable.
         *
         * @param index the model index
         * @param offset the text offset
         */
        public void insertLineBreak(int index, int offset);

        /**
         * Removes the specified range.
         * The {@code BasicTextModel} guarantees that this method is only called when the content is writable,
         * and that {@code start} precedes {@code end}.
         *
         * @param start the start of the region to be removed
         * @param end the end of the region to be removed, expected to be greater than the start position
         */
        public void removeRange(TextPos start, TextPos end);

        /**
         * Determines whether this content is writable (i.e. supports modification).
         * @return true if writable
         */
        public boolean isWritable();
    }

    private final Content content;

    /**
     * Constructs an empty model with the specified {@code Content}.
     * @param c the content to use
     */
    public BasicTextModel(Content c) {
        this.content = c;
        registerDataFormatHandler(PlainTextFormatHandler.getInstance(), true, true, 0);
    }

    /**
     * Constructs an empty model with the in-memory {@code Content}.
     */
    public BasicTextModel() {
        this(new InMemoryContent());
    }

    /**
     * Inserts text at the specified position.
     * This is a convenience shortcut for {@link #replace(StyleResolver, TextPos, TextPos, String, boolean)}.
     *
     * @param p the insertion position
     * @param text the text to insert
     * @throws NullPointerException if the model is {@code null}
     * @throws UnsupportedOperationException if the model is not {@link StyledTextModel#isWritable() writable}
     */
    public void insertText(TextPos p, String text) {
        replace(null, p, p, text, false);
    }

    @Override
    public int size() {
        return content.size();
    }

    @Override
    public String getPlainText(int index) {
        return content.getText(index);
    }

    @Override
    public RichParagraph getParagraph(int index) {
        String text = getPlainText(index);
        return RichParagraph.builder().
            addSegment(text).
            build();
    }

    /**
     * Determines whether the model is writable.
     * <p>
     * This method calls {@link BasicTextModel.Content#isWritable()}.
     *
     * @return true if the model is writable
     */
    @Override
    public final boolean isWritable() {
        return content.isWritable();
    }

    @Override
    protected int insertTextSegment(int index, int offset, String text, StyleAttributeMap attrs) {
        return content.insertTextSegment(index, offset, text, attrs);
    }

    @Override
    protected void insertLineBreak(int index, int offset) {
        content.insertLineBreak(index, offset);
    }

    @Override
    protected void removeRange(TextPos start, TextPos end) {
        content.removeRange(start, end);
    }

    @Override
    protected void insertParagraph(int index, Supplier<Region> generator) {
        // no-op
    }

    @Override
    public StyleAttributeMap getStyleAttributeMap(StyleResolver resolver, TextPos pos) {
        return StyleAttributeMap.EMPTY;
    }

    @Override
    protected final void setParagraphStyle(int index, StyleAttributeMap a) {
        // no-op
    }

    @Override
    protected final void applyStyle(int index, int start, int end, StyleAttributeMap a, boolean merge) {
        // no-op
    }

    /**
     * This content provides in-memory storage in an {@code ArrayList} of {@code String}s.
     */
    public static class InMemoryContent implements Content {
        private final ArrayList<String> paragraphs = new ArrayList<>();

        /** The constructor. */
        public InMemoryContent() {
        }

        @Override
        public int size() {
            int sz = paragraphs.size();
            // empty model always have one line
            return sz == 0 ? 1 : sz;
        }

        @Override
        public String getText(int index) {
            if (index < paragraphs.size()) {
                return paragraphs.get(index);
            }
            return "";
        }

        @Override
        public int insertTextSegment(int index, int offset, String text, StyleAttributeMap attrs) {
            String s = getText(index);
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
        public void insertLineBreak(int index, int offset) {
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
        public void removeRange(TextPos start, TextPos end) {
            int ix = start.index();
            String text = getText(ix);
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
        public boolean isWritable() {
            return true;
        }
    }
}
