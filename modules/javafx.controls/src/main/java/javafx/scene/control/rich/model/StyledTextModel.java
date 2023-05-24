/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxEditor
package javafx.scene.control.rich.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.rich.Marker;
import javafx.scene.control.rich.RichTextArea;
import javafx.scene.control.rich.StyleResolver;
import javafx.scene.control.rich.TextCell;
import javafx.scene.control.rich.TextPos;
import javafx.scene.control.rich.impl.Markers;
import javafx.scene.control.rich.util.Util;
import javafx.scene.input.DataFormat;

/**
 * Base class for a styled text model for use with {@link RichTextArea}.
 * 
 * TODO printing
 */
public abstract class StyledTextModel {
    public interface ChangeListener {
        /**
         * Indicates a change in the model text.
         * The listeners are updated *after* the corresponding changes have been made to the model.
         * 
         * @param start start of the affected text block
         * @param end end of the affected text block
         * @param charsAddedTop number of characters inserted on the same line as start
         * @param linesAdded the number of paragraphs inserted between start and end
         * @param charsAddedBottom number of characters inserted on the same line as end
         */
        public void eventTextUpdated(TextPos start, TextPos end, int charsAddedTop, int linesAdded, int charsAddedBottom);
        
        /**
         * Indicates a change in styles only, with no change in the model text.
         * @param start start of the affected text block
         * @param end end of the affected text block
         */
        public void eventStyleUpdated(TextPos start, TextPos end);
    }
    
    /**
     * Indicates whether the model is editable.
     */
    public abstract boolean isEditable();

    /**
     * Returns the number of paragraphs in the model.
     */
    public abstract int size();

    /**
     * Returns the plain text string for the specified paragraph.  The text string must not
     * contain any line separators.
     * The caller should never attempt to ask for a paragraph outside of the valid range.
     * This method might return null if no text is associated with the paragraph.
     *
     * @param index paragraph index in the range (0...{@link #size()})
     */
    public abstract String getPlainText(int index);

    /**
     * Creates a TextCell which provides a visual representation of the paragraph.
     * This method must create new instance each time, in order to support multiple RichTextArea instances
     * connected to the same model.
     * The nodes are not reused, and might be created repeatedly,
     * so the model must not keep strong references to these nodes.
     *
     * @param index paragraph index in the range (0...{@link #size()})
     */
    public abstract TextCell createTextCell(int index);
    
    /**
     * This method gets called only if the model is editable.
     * The caller guarantees that {@code start} precedes {@code end}.
     * 
     * @param start start of the region to be removed
     * @param end end of the region to be removed, guaranteed to be > start.
     */
    protected abstract void removeRegion(TextPos start, TextPos end);

    /**
     * This method is called to insert a single text segment at the given position.
     * @return the character count of the inserted text
     */
    protected abstract int insertTextSegment(StyleResolver resolver, int index, int offset, StyledSegment text);

    /** inserts a line break */
    protected abstract void insertLineBreak(int index, int offset);
    
    /** inserts a paragraph node */
    protected abstract void insertParagraph(int index, Supplier<Node> generator);
    
    /**
     * Exports part of the paragraph as a sequence of styled segments.
     * The caller guarantees that the start position precedes the end.
     * 
     * @param index paragraph's model index
     * @param startOffset start offset
     * @param endOffset end offset (may exceed the paragraph text length)
     * @param out
     * @throws IOException 
     */
    protected abstract void exportParagraph(int index, int startOffset, int endOffset, StyledOutput out) throws IOException;
    
    /**
     * Applies a style to the specified text range, where {@code start} is guaranteed to precede {@code end}.
     * 
     * @param start start position
     * @param end end position
     * @param attrs non-null attribute map
     * @return true if the model has changed as a result of this call
     */
    protected abstract boolean applyStyleImpl(TextPos start, TextPos end, StyleAttrs attrs);
    
    /**
     * Returns the {@link StyleInfo} of the first character at the specified position.
     * When at the end of the document, returns the attributes of the last character.
     *
     * @return non-null {@link StyleInfo}
     */
    public abstract StyleInfo getStyleInfo(TextPos pos);
    
    /** stores the handler and its priority */
    private static record FHPriority(DataFormatHandler handler, int priority) implements Comparable<FHPriority>{
        @Override
        public int compareTo(FHPriority x) {
            int d = x.priority - priority;
            if(d == 0) {
                // compare formats to guard against someone adding two different formats under the same priority
                String us = handler().getDataFormat().toString();
                String them = x.handler().getDataFormat().toString();
                d = them.compareTo(us);
            }
            return d;
        }
    }
    
    private final CopyOnWriteArrayList<ChangeListener> listeners = new CopyOnWriteArrayList();
    private final HashMap<DataFormat,FHPriority> handlers = new HashMap<>(4);
    private final Markers markers = new Markers();

    public StyledTextModel() {
    }
    
    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Registers a format handler for input/output operation such as copy(), paste(), and save().
     * Priority determines the format chosen for operations with the {@link javafx.scene.input.Clipboard}
     * when input data is available in more than one supported format.
     * 
     * @param h data format handler
     * @param priority from 0 (lowest, usually plain text) to {@code Integer.MAX_VALUE}, for a native model format.
     */
    public void registerDataFormatHandler(DataFormatHandler h, int priority) {
        handlers.put(h.getDataFormat(), new FHPriority(h, priority));
    }

    /** returns supported data formats, in the order of priority - from high to low */
    public DataFormat[] getSupportedDataFormats() {
        ArrayList<FHPriority> fs = new ArrayList<>(handlers.values());
        Collections.sort(fs);
        int sz = fs.size();
        DataFormat[] formats = new DataFormat[sz];
        for(int i=0; i<sz; i++) {
            formats[i] = fs.get(i).handler().getDataFormat();
        }
        return formats;
    }

    public DataFormatHandler getDataFormatHandler(DataFormat format) {
        FHPriority p = handlers.get(format);
        return p == null ? null : p.handler();
    }

    /**
     * Replaces the given range with the provided plain text.
     * This is a convenience method that calls
     * {@link #replace(StyleResolver,TextPos,TextPos,StyledInput)}.
     * The caller must ensure that the start position precedes the end.
     *
     * @param resolver
     * @param start
     * @param end
     * @param text
     * @return
     */
    public TextPos replace(StyleResolver resolver, TextPos start, TextPos end, String text) {
        if (isEditable()) {
            StyleInfo si = getStyleInfo(start);
            return replace(resolver, start, end, StyledInput.of(text, si));
        }
        return null;
    }
    
    /**
     * Replaces the given range with the provided styled text.
     * When inserting a plain text, the style is taken from preceding text segment, or, if the text is being
     * inserted into the beginning of the document, the style is taken from the following text segment.
     * 
     * The caller must ensure that the start position precedes the end.
     * 
     * After the model applies the requested changes, an event is sent to all the registered ChangeListeners.
     * 
     * @param resolver
     * @param start start position
     * @param end end position
     * @param input StyledInput
     * @return text position at the end of the inserted text, or null if the model is read only
     */
    public TextPos replace(StyleResolver resolver, TextPos start, TextPos end, StyledInput input) {
        if (isEditable()) {
            int cmp = start.compareTo(end);
            if(cmp != 0) {
                removeRegion(start, end);
            }

            int index = start.index();
            int offset = start.offset();
            int top = 0;
            int btm = 0;
            
            StyledSegment seg;
            while ((seg = input.nextSegment()) != null) {
                if(seg.isParagraph()) {
                    offset = 0;
                    btm = 0;
                    index++;
                    Supplier<Node> gen = seg.getNodeGenerator();
                    insertParagraph(index, gen);
                } else if(seg.isText()) {
                    int len = insertTextSegment(resolver, index, offset, seg);
                    if(index == start.index()) {
                        top += len;
                    }
                    offset += len;
                    btm += len;
                } else if(seg.isLineBreak()) {
                    insertLineBreak(index, offset);
                    index++;
                    offset = 0;
                    btm = 0;
                }
            }

            int lines = index - start.index();
            if (lines == 0) {
                btm = 0;
            }

            fireChangeEvent(start, end, top, lines, btm);

            return new TextPos(index, offset);
        }
        return null;
    }
    
    protected void fireChangeEvent(TextPos start, TextPos end, int charsTop, int linesAdded, int charsBottom) {
        markers.update(start, end, charsTop, linesAdded, charsBottom);

        for (ChangeListener li : listeners) {
            li.eventTextUpdated(start, end, charsTop, linesAdded, charsBottom);
        }
    }
    
    protected void fireStyleChangeEvent(TextPos start, TextPos end) {
        for (ChangeListener li : listeners) {
            li.eventStyleUpdated(start, end);
        }
    }

    public void exportText(TextPos start, TextPos end, StyledOutput out) throws IOException {
        int cmp = start.compareTo(end);
        if (cmp > 0) {
            // make sure start < end
            TextPos p = start;
            start = end;
            end = p;
        }

        int ix0 = start.index();
        int ix1 = end.index();
        if (ix0 == ix1) {
            // part of one line
            exportParagraph(start.index(), start.offset(), end.offset(), out);
        } else {
            // multi-line
            boolean lineBreak = false;
            for(int ix=start.index(); ix<=end.index(); ix++) {
                if(lineBreak) {
                    out.append(StyledSegment.LINE_BREAK);
                } else {
                    lineBreak = true;
                }
                
                int off0;
                int off1;
                if(ix == ix0) {
                    off0 = start.offset();
                    off1 = Integer.MAX_VALUE;
                } else if(ix == ix1) {
                    off0 = 0;
                    off1 = end.offset();
                } else {
                    off0 = 0;
                    off1 = Integer.MAX_VALUE;
                }
                
                exportParagraph(ix, off0, off1, out);
            }
        }
    }

    public Marker getMarker(TextPos pos) {
        TextPos p = clamp(pos);
        return markers.getMarker(p);
    }

    protected TextPos clamp(TextPos p) {
        int ct = size();
        int ix = p.index();
        if (ix < 0) {
            return TextPos.ZERO;
        } else if (ix < ct) {
            // TODO not to exceed paragraph length?
            return p;
        } else {
            if (ct == 0) {
                return TextPos.ZERO;
            } else {
                ix = ct - 1;
                String s = getPlainText(ix);
                int len = (s == null) ? 0 : s.length();
                return new TextPos(ct - 1, len);
            }
        }
    }

    /**
     * exports plain text segments only 
     * @throws IOException
     */
    protected void exportPlaintextSegments(int index, int start, int end, StyledOutput out) throws IOException {
        String text = getPlainText(index);
        text = Util.substring(text, start, end);
        StyledSegment seg = StyledSegment.of(text);
        out.append(seg);
    }

    /** Returns a TextPos corresponding to the end of the document */
    public TextPos getEndTextPos() {
        int ix = size() - 1;
        if (ix < 0) {
            return TextPos.ZERO;
        } else {
            return getEndOfParagraphTextPos(ix);
        }
    }

    /** Returns a TextPos corresponding to the end of paragraph at the given index */
    public TextPos getEndOfParagraphTextPos(int index) {
        String text = getPlainText(index);
        if (text == null) {
            return new TextPos(index, 0);
        }

        int off = text.length();
        int cix = off - 1;
        if (cix < 0) {
            return new TextPos(index, off);
        } else {
            return new TextPos(index, off, cix, false);
        }
    }

    /**
     * Applies a style to the specified text range.
     * 
     * @param start start position
     * @param end end position
     * @param attrs non-null attribute map
     */
    public final void applyStyle(TextPos start, TextPos end, StyleAttrs attrs) {
        boolean changed = applyStyleImpl(start, end, attrs);
        if (changed) {
            fireStyleChangeEvent(start, end);
        }
    }
}
