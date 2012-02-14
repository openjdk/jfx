/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.binding.ExpressionHelper;
import com.sun.javafx.collections.ListListenerHelper;
import com.sun.javafx.collections.NonIterableChange;

/**
 * Text input component that allows a user to enter multiple lines of
 * plain text. Unlike in previous releases of JavaFX, support for single line
 * input is not available as part of the TextArea control, however this is 
 * the sole-purpose of the {@link TextField} control. Additionally, if you want
 * a form of rich-text editing, there is also the
 * {@link javafx.scene.web.HTMLEditor HTMLEditor} control.
 * 
 * @see TextField
 */
public class TextArea extends TextInputControl {
    // Text area content model
    private static final class TextAreaContent implements Content {
        private ExpressionHelper<String> helper = null;
        private ArrayList<StringBuilder> paragraphs = new ArrayList<StringBuilder>();
        private int contentLength = 0;
        private ParagraphList paragraphList = new ParagraphList();
        private ListListenerHelper<CharSequence> listenerHelper;

        private TextAreaContent() {
            paragraphs.add(new StringBuilder(DEFAULT_PARAGRAPH_CAPACITY));
            paragraphList.content = this;
        }

        @Override public String get(int start, int end) {
            int length = end - start;
            StringBuilder textBuilder = new StringBuilder(length);

            int paragraphCount = paragraphs.size();

            int paragraphIndex = 0;
            int offset = start;

            while (paragraphIndex < paragraphCount) {
                StringBuilder paragraph = paragraphs.get(paragraphIndex);
                int count = paragraph.length() + 1;

                if (offset < count) {
                    break;
                }

                offset -= count;
                paragraphIndex++;
            }

            // Read characters until end is reached, appending to text builder
            // and moving to next paragraph as needed
            StringBuilder paragraph = paragraphs.get(paragraphIndex);

            int i = 0;
            while (i < length) {
                if (offset == paragraph.length()
                    && i < contentLength) {
                    textBuilder.append('\n');
                    paragraph = paragraphs.get(++paragraphIndex);
                    offset = 0;
                } else {
                    textBuilder.append(paragraph.charAt(offset++));
                }

                i++;
            }

            return textBuilder.toString();
        }

        @Override
        @SuppressWarnings("unchecked")
        public void insert(int index, String text, boolean notifyListeners) {
            if (index < 0
                || index > contentLength) {
                throw new IndexOutOfBoundsException();
            }

            if (text == null) {
                throw new IllegalArgumentException();
            }
            text = TextInputControl.filterInput(text, false, false);
            int length = text.length();
            if (length > 0) {
                // Split the text into lines
                ArrayList<StringBuilder> lines = new ArrayList<StringBuilder>();

                StringBuilder line = new StringBuilder(DEFAULT_PARAGRAPH_CAPACITY);
                for (int i = 0; i < length; i++) {
                    char c = text.charAt(i);

                    if (c == '\n') {
                        lines.add(line);
                        line = new StringBuilder(DEFAULT_PARAGRAPH_CAPACITY);
                    } else {
                        line.append(c);
                    }
                }

                lines.add(line);

                // Merge the text into the existing content
                // Merge the text into the existing content
                int paragraphIndex = paragraphs.size();
                int offset = contentLength + 1;

                StringBuilder paragraph = null;

                do {
                    paragraph = paragraphs.get(--paragraphIndex);
                    offset -= paragraph.length() + 1;
                } while (index < offset);

                int start = index - offset;

                int n = lines.size();
                if (n == 1) {
                    // The text contains only a single line; insert it into the
                    // intersecting paragraph
                    paragraph.insert(start, line);
                    fireParagraphListChangeEvent(paragraphIndex, paragraphIndex + 1,
                        Collections.singletonList((CharSequence)paragraph));
                } else {
                    // The text contains multiple line; split the intersecting
                    // paragraph
                    int end = paragraph.length();
                    CharSequence trailingText = paragraph.subSequence(start, end);
                    paragraph.delete(start, end);

                    // Append the first line to the intersecting paragraph and
                    // append the trailing text to the last line
                    StringBuilder first = lines.get(0);
                    paragraph.insert(start, first);
                    line.append(trailingText);
                    fireParagraphListChangeEvent(paragraphIndex, paragraphIndex + 1,
                        Collections.singletonList((CharSequence)paragraph));

                    // Insert the remaining lines into the paragraph list
                    paragraphs.addAll(paragraphIndex + 1, lines.subList(1, n));
                    fireParagraphListChangeEvent(paragraphIndex + 1, paragraphIndex + n,
                        Collections.EMPTY_LIST);
                }

                // Update content length
                contentLength += length;
                if (notifyListeners) {
                    ExpressionHelper.fireValueChangedEvent(helper);
                }
            }
        }

        @Override public void delete(int start, int end, boolean notifyListeners) {
            if (start > end) {
                throw new IllegalArgumentException();
            }

            if (start < 0
                || end > contentLength) {
                throw new IndexOutOfBoundsException();
            }

            int length = end - start;

            if (length > 0) {
                // Identify the trailing paragraph index
                int paragraphIndex = paragraphs.size();
                int offset = contentLength + 1;

                StringBuilder paragraph = null;

                do {
                    paragraph = paragraphs.get(--paragraphIndex);
                    offset -= paragraph.length() + 1;
                } while (end < offset);

                int trailingParagraphIndex = paragraphIndex;
                int trailingOffset = offset;
                StringBuilder trailingParagraph = paragraph;

                // Identify the leading paragraph index
                paragraphIndex++;
                offset += paragraph.length() + 1;

                do {
                    paragraph = paragraphs.get(--paragraphIndex);
                    offset -= paragraph.length() + 1;
                } while (start < offset);

                int leadingParagraphIndex = paragraphIndex;
                int leadingOffset = offset;
                StringBuilder leadingParagraph = paragraph;

                // Remove the text
                if (leadingParagraphIndex == trailingParagraphIndex) {
                    // The removal affects only a single paragraph
                    leadingParagraph.delete(start - leadingOffset,
                        end - leadingOffset);

                    fireParagraphListChangeEvent(leadingParagraphIndex, leadingParagraphIndex + 1,
                        Collections.singletonList((CharSequence)leadingParagraph));
                } else {
                    // The removal spans paragraphs; remove any intervening paragraphs and
                    // merge the leading and trailing segments
                    CharSequence leadingSegment = leadingParagraph.subSequence(0,
                        start - leadingOffset);
                    int trailingSegmentLength = (start + length) - trailingOffset;

                    trailingParagraph.delete(0, trailingSegmentLength);
                    fireParagraphListChangeEvent(trailingParagraphIndex, trailingParagraphIndex + 1,
                        Collections.singletonList((CharSequence)trailingParagraph));

                    if (trailingParagraphIndex - leadingParagraphIndex > 0) {
                        List<CharSequence> removed = new ArrayList<CharSequence>(paragraphs.subList(leadingParagraphIndex,
                            trailingParagraphIndex));
                        paragraphs.subList(leadingParagraphIndex,
                            trailingParagraphIndex).clear();
                        fireParagraphListChangeEvent(leadingParagraphIndex, leadingParagraphIndex,
                            removed);
                    }

                    // Trailing paragraph is now at the former leading paragraph's index
                    trailingOffset = leadingOffset;
                    trailingParagraph.insert(0, leadingSegment);
                    fireParagraphListChangeEvent(leadingParagraphIndex, leadingParagraphIndex + 1,
                        Collections.singletonList((CharSequence)leadingParagraph));
                }

                // Update content length
                contentLength -= length;
                if (notifyListeners) {
                    ExpressionHelper.fireValueChangedEvent(helper);
                }
            }
        }

        @Override public int length() {
            return contentLength;
        }

        @Override public String get() {
            return get(0, length());
        }

        @Override public void addListener(ChangeListener<? super String> changeListener) {
            helper = ExpressionHelper.addListener(helper, this, changeListener);
        }

        @Override public void removeListener(ChangeListener<? super String> changeListener) {
            helper = ExpressionHelper.removeListener(helper, changeListener);
        }

        @Override public String getValue() {
            return get();
        }

        @Override public void addListener(InvalidationListener listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override public void removeListener(InvalidationListener listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }

        private void fireParagraphListChangeEvent(int from, int to, List<CharSequence> removed) {
            ParagraphListChange change = new ParagraphListChange(paragraphList, from, to, removed);
            ListListenerHelper.fireValueChangedEvent(listenerHelper, change);
        }
    }

    // Observable list of paragraphs
    private static final class ParagraphList extends AbstractList<CharSequence>
            implements ObservableList<CharSequence> {

        private TextAreaContent content;

        @Override
        public CharSequence get(int index) {
            return content.paragraphs.get(index);
        }

        @Override
        public boolean addAll(Collection<? extends CharSequence> paragraphs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(CharSequence... paragraphs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean setAll(Collection<? extends CharSequence> paragraphs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean setAll(CharSequence... paragraphs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return content.paragraphs.size();
        }

        @Override
        public void addListener(ListChangeListener<? super CharSequence> listener) {
            content.listenerHelper = ListListenerHelper.addListener(content.listenerHelper, listener);
        }

        @Override
        public void removeListener(ListChangeListener<? super CharSequence> listener) {
            content.listenerHelper = ListListenerHelper.removeListener(content.listenerHelper, listener);
        }

        @Override
        public boolean removeAll(CharSequence... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(CharSequence... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(int from, int to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addListener(InvalidationListener listener) {
            content.listenerHelper = ListListenerHelper.addListener(content.listenerHelper, listener);
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            content.listenerHelper = ListListenerHelper.removeListener(content.listenerHelper, listener);
        }
    }

    private static final class ParagraphListChange extends NonIterableChange<CharSequence>  {

        private List<CharSequence> removed;

        protected ParagraphListChange(ObservableList<CharSequence> list, int from, int to,
            List<CharSequence> removed) {
            super(from, to, list);

            this.removed = removed;
        }

        @Override
        public List<CharSequence> getRemoved() {
            return removed;
        }

        @Override
        protected int[] getPermutation() {
            return new int[0];
        }
    };

    /**
     * The default value for {@link #prefColumnCount}.
     */
    public static final int DEFAULT_PREF_COLUMN_COUNT = 40;

    /**
     * The default value for {@link #prefRowCount}.
     */
    public static final int DEFAULT_PREF_ROW_COUNT = 10;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    public static final int DEFAULT_PARAGRAPH_CAPACITY = 32;

    /**
     * Creates a {@code TextArea} with empty text content.
     */
    public TextArea() {
        this("");
    }

    /**
     * Creates a {@code TextArea} with initial text content.
     *
     * @param text A string for text content.
     */
    public TextArea(String text) {
        super(new TextAreaContent());

        getStyleClass().add("text-area");
        setText(text);
    }

    @Override final void textUpdated() {
        setScrollTop(0);
        setScrollLeft(0);
    }

    /**
     * Returns an unmodifiable list of the character sequences that back the
     * text area's content.
     */
    public ObservableList<CharSequence> getParagraphs() {
        return ((TextAreaContent)getContent()).paragraphList;
    }


    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * If a run of text exceeds the width of the {@code TextArea},
     * then this variable indicates whether the text should wrap onto
     * another line.
     */
    private BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText");
    public final BooleanProperty wrapTextProperty() { return wrapText; }
    public final boolean isWrapText() { return wrapText.getValue(); }
    public final void setWrapText(boolean value) { wrapText.setValue(value); }


    /**
     * The preferred number of text columns. This is used for
     * calculating the {@code TextArea}'s preferred width.
     */
    private IntegerProperty prefColumnCount = new IntegerPropertyBase(DEFAULT_PREF_COLUMN_COUNT) {
        @Override
        public void set(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("value cannot be negative.");
            }

            super.set(value);
        }

        @Override
        public Object getBean() {
            return TextArea.this;
        }

        @Override
        public String getName() {
            return "prefColumnCount";
        }
    };
    public final IntegerProperty prefColumnCountProperty() { return prefColumnCount; }
    public final int getPrefColumnCount() { return prefColumnCount.getValue(); }
    public final void setPrefColumnCount(int value) { prefColumnCount.setValue(value); }


    /**
     * The preferred number of text rows. This is used for calculating
     * the {@code TextArea}'s preferred height.
     */
    private IntegerProperty prefRowCount = new IntegerPropertyBase(DEFAULT_PREF_ROW_COUNT) {
        @Override
        public void set(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("value cannot be negative.");
            }

            super.set(value);
        }

        @Override
        public Object getBean() {
            return TextArea.this;
        }

        @Override
        public String getName() {
            return "prefRowCount";
        }
    };
    public final IntegerProperty prefRowCountProperty() { return prefRowCount; }
    public final int getPrefRowCount() { return prefRowCount.getValue(); }
    public final void setPrefRowCount(int value) { prefRowCount.setValue(value); }


    /**
     * The number of pixels by which the content is vertically
     * scrolled.
     */
    private DoubleProperty scrollTop = new SimpleDoubleProperty(this, "scrollTop", 0);
    public final DoubleProperty scrollTopProperty() { return scrollTop; }
    public final double getScrollTop() { return scrollTop.getValue(); }
    public final void setScrollTop(double value) { scrollTop.setValue(value); }


    /**
     * The number of pixels by which the content is horizontally
     * scrolled.
     */
    private DoubleProperty scrollLeft = new SimpleDoubleProperty(this, "scrollLeft", 0);
    public final DoubleProperty scrollLeftProperty() { return scrollLeft; }
    public final double getScrollLeft() { return scrollLeft.getValue(); }
    public final void setScrollLeft(double value) { scrollLeft.setValue(value); }
}
