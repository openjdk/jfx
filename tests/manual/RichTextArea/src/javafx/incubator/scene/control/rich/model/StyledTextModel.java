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

package javafx.incubator.scene.control.rich.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.incubator.scene.control.rich.Marker;
import javafx.incubator.scene.control.rich.RichTextArea;
import javafx.incubator.scene.control.rich.StyleResolver;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.Region;
import com.sun.javafx.scene.control.rich.Markers;
import com.sun.javafx.scene.control.rich.UndoableChange;

/**
 * Base class for a styled text model for {@link RichTextArea}.
 * <p>
 * This class implements the following functionality with the intent
 * to simplify custom models that extend this class:
 * <ul>
 * <li>managing listeners
 * <li>firing events
 * <li>decomposing of edits into multiple operations performed upon individual paragraphs
 * <li>managing {@link Marker}s
 * </ul>
 */
public abstract class StyledTextModel {
    /**
     * Receives information about modifications of the model.
     */
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
     * <p>
     * When this method returns false, the model must silently ignore any modification attempts.
     *
     * @return true if the model is editable
     */
    public abstract boolean isEditable();

    /**
     * Returns the number of paragraphs in the model.
     *
     * @return number of paragraphs
     */
    public abstract int size();

    /**
     * Returns the plain text string for the specified paragraph.  The returned text string cannot be null
     * and must not contain any control characters other than TAB.
     * The caller should never attempt to ask for a paragraph outside of the valid range.
     *
     * @param index paragraph index in the range (0...{@link #size()})
     * @return paragraph text string or null
     */
    public abstract String getPlainText(int index);

    /**
     * Returns a {@link RichParagraph} at the given model index.
     *
     * @param index paragraph index in the range (0...{@link #size()})
     * @return a new instance of TextCell created
     */
    public abstract RichParagraph getParagraph(int index);
    
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
     *
     * @param resolver style resolver to use
     * @param index paragraph index
     * @param offset insertion offset within the paragraph
     * @param text segment to insert
     * @return the number of characters inserted
     */
    protected abstract int insertTextSegment(StyleResolver resolver, int index, int offset, StyledSegment text);

    /**
     * Inserts a line break.
     * @param index the model index
     * @param offset the text offset
     */
    protected abstract void insertLineBreak(int index, int offset);
    
    /**
     * Inserts a paragraph that contains a single {@link Node}.
     * @param index model index
     * @param generator code that will be used to create a Node instance
     */
    protected abstract void insertParagraph(int index, Supplier<Region> generator);
    
    /**
     * Applies the paragraph styles to the specified paragraph.
     * The new attributes override any existing attributes.
     *
     * @param ix the paragraph index
     * @param paragraphAttrs the paragraph attributes
     */
    protected abstract void applyStyle(int ix, StyleAttrs paragraphAttrs);

    /**
     * Applies style to the specified text range within a single paragraph.
     * The new attributes override any existing attributes.
     * The {@code end} argument may exceed the paragraph length, in which case the outcome should be the same
     * as supplying the paragraph length value.
     *
     * @param ix the paragraph index
     * @param start the start offset
     * @param end the end offset
     * @param a the character attributes
     */
    protected abstract void applyStyle(int ix, int start, int end, StyleAttrs a);

    /**
     * Returns the {@link StyleAttrs} of the first character at the specified position.
     * When at the end of the document, returns the attributes of the last character.
     *
     * @param pos text position
     * @return the style attributes, non-null
     */
    // TODO move implementation here
    public abstract StyleAttrs getStyleAttrs(TextPos pos);
    
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

    private record FHKey(DataFormat format, boolean forExport) { }
    private final CopyOnWriteArrayList<ChangeListener> listeners = new CopyOnWriteArrayList();
    private final HashMap<FHKey,FHPriority> handlers = new HashMap<>(2);
    private final Markers markers = new Markers();
    private final UndoableChange head = UndoableChange.createHead();
    private UndoableChange undo = head;

    /** The constructor. */
    public StyledTextModel() {
        registerDataFormatHandler(new RtfFormatHandler(), true, 1000);
        registerDataFormatHandler(new HtmlExportFormatHandler(), true, 100);
        registerDataFormatHandler(new PlainTextFormatHandler(), true, 0);
    }

    /**
     * Adds a {@link ChangeListener} to this model.
     * @param listener a non-null listener
     */
    public void addChangeListener(StyledTextModel.ChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a {@link ChangeListener} from this model.
     * This method does nothing if this listener has never been added.
     * @param listener a non-null listener
     */
    public void removeChangeListener(StyledTextModel.ChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Registers a format handler for export and import operations.
     * Priority determines the format chosen for operations with the {@link javafx.scene.input.Clipboard}
     * when input data is available in more than one supported format.
     * <p>
     * This method is expected to be called by a StyledTextModel implementation constructor.
     *
     * @param h data format handler
     * @param priority from 0 (lowest, usually plain text) to {@code Integer.MAX_VALUE}, for a native model format.
     */
    protected void registerDataFormatHandler(DataFormatHandler h, int priority) {
        FHPriority p = new FHPriority(h, priority);
        handlers.put(new FHKey(h.getDataFormat(), true), p);
        handlers.put(new FHKey(h.getDataFormat(), false), p);
    }
    
    /**
     * Registers a format handler for either export or import operations.
     * Priority determines the format chosen for operations with the {@link javafx.scene.input.Clipboard}
     * when input data is available in more than one supported format.
     * <p>
     * This method is expected to be called by a StyledTextModel implementation constructor.
     *
     * @param h data format handler
     * @param forExport determines the class of operations this handler supports
     * @param priority from 0 (lowest, usually plain text) to {@code Integer.MAX_VALUE}
     */
    protected void registerDataFormatHandler(DataFormatHandler h, boolean forExport, int priority) {
        FHPriority p = new FHPriority(h, priority);
        handlers.put(new FHKey(h.getDataFormat(), forExport), p);
    }

    /**
     * Returns an array of supported data formats for either export or import operations,
     * in the order of priority - from high to low.
     * @param forExport determines whether the operation is export (true) or import (false)
     * @return supported formats
     */
    public DataFormat[] getSupportedDataFormats(boolean forExport) {
        ArrayList<FHPriority> fs = new ArrayList<>(handlers.size());
        handlers.forEach((k, p) -> {
            if (k.forExport == forExport) {
                fs.add(p);
            }
        });
        Collections.sort(fs);
        int sz = fs.size();
        DataFormat[] formats = new DataFormat[sz];
        for (int i = 0; i < sz; i++) {
            formats[i] = fs.get(i).handler().getDataFormat();
        }
        return formats;
    }

    /**
     * Returns a {@link DataFormatHandler} instance corresponding to the given {@link DataFormat}.
     * This method will return {@code null} if the data format is not supported.
     * @param format data format
     * @param forExport for export (true) or for input (false)
     * @return DataFormatHandler or null
     */
    public DataFormatHandler getDataFormatHandler(DataFormat format, boolean forExport) {
        FHKey k = new FHKey(format, forExport);
        FHPriority p = handlers.get(k);
        return p == null ? null : p.handler();
    }

    /**
     * Fires a text modification event for the given range.
     * @param start start of the affected range
     * @param end end of the affected range
     * @param charsTop number of characters added before any added paragraphs
     * @param linesAdded number of paragraphs inserted
     * @param charsBottom number of characters added after any inserted paragraphs
     */
    protected void fireChangeEvent(TextPos start, TextPos end, int charsTop, int linesAdded, int charsBottom) {
        markers.update(start, end, charsTop, linesAdded, charsBottom);
        for (ChangeListener li : listeners) {
            li.eventTextUpdated(start, end, charsTop, linesAdded, charsBottom);
        }
    }

    /**
     * Fires a style change event for the given range.
     * This event indicates that only the styling has changed, with no  changes to any text positions.
     * @param start start position
     * @param end end position, must be greater than the start position
     */
    protected void fireStyleChangeEvent(TextPos start, TextPos end) {
        for (ChangeListener li : listeners) {
            li.eventStyleUpdated(start, end);
        }
    }

    /**
     * Fires a style change event for the entire document.
     * This event indicates that only the styling has changed, with no  changes to any text positions.
     */
    protected void fireStylingUpdate() {
        TextPos end = getDocumentEnd();
        fireStyleChangeEvent(TextPos.ZERO, end);
    }

    /**
     * Returns the length of text in a paragraph at the specified index.
     * @param ix the paragraph index in the model
     * @return the length
     */
    public int getTextLength(int ix) {
        String s = getPlainText(ix);
        return (s == null) ? 0 : s.length();
    }

    /**
     * Sends the styled text in the given range to the specified output.
     * @param start start of the range
     * @param end end of the range
     * @param out {@link StyledOutput} to receive the stream
     * @throws IOException when an I/O error occurs
     */
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
            // single line
            int soff = start.offset();
            int eoff = end.offset();
            int len = getTextLength(ix0);
            boolean withParAttrs = ((soff == 0) && ((eoff >= len) || (eoff < 0)));
            exportParagraph(ix0, soff, eoff, withParAttrs, out);
        } else {
            // multi-line
            boolean lineBreak = false;
            for (int ix = ix0; ix <= ix1; ix++) {
                if (lineBreak) {
                    out.append(StyledSegment.LINE_BREAK);
                } else {
                    lineBreak = true;
                }

                int off0;
                int off1;
                if (ix == ix0) {
                    off0 = start.offset();
                    off1 = Integer.MAX_VALUE;
                } else if (ix == ix1) {
                    off0 = 0;
                    off1 = end.offset();
                } else {
                    off0 = 0;
                    off1 = Integer.MAX_VALUE;
                }

                exportParagraph(ix, off0, off1, true, out);
            }
        }

        out.flush();
    }

    /**
     * Exports part of the paragraph as a sequence of styled segments.
     * The caller guarantees that the start position precedes the end.
     * The subclass may override this method to provide a more performant implementation.
     * The paragraph end argument may exceed the actual length of the paragraph, in which case it
     * should be treated as equal to the paragraph text length.
     *
     * @param index the paragraph index in the model
     * @param start the start offset
     * @param end the end offset (may exceed the paragraph length)
     * @param withParAttrs determines whether to emit paragraph attributes
     * @param out the target StyledOutput
     * @throws IOException when an I/O error occurs
     */
    protected void exportParagraph(int index, int start, int end, boolean withParAttrs, StyledOutput out) throws IOException {
        RichParagraph par = getParagraph(index);
        par.export(start, end, out);
        if (withParAttrs) {
            // or get and add conditionally
            StyleAttrs pa = par.getParagraphAttributes();
            if ((pa != null) && !pa.isEmpty()) {
                out.append(StyledSegment.ofParagraphAttributes(pa));
            }
        }
    }

    /**
     * Returns a {@link Marker} at the specified position.
     *
     * @param pos text position
     * @return Marker instance
     */
    public Marker getMarker(TextPos pos) {
        TextPos p = clamp(pos);
        return markers.getMarker(p);
    }

    /**
     * Returns a text position guaranteed to be within the document and paragraph limits.
     * @param p text position, cannot be null
     * @return text position
     */
    public TextPos clamp(TextPos p) {
        Objects.nonNull(p);
        int ct = size();
        int ix = p.index();
        if (ix < 0) {
            return TextPos.ZERO;
        } else if (ix < ct) {
            // clamp to paragraph length
            int len = getTextLength(ix);
            if (p.offset() < len) {
                return p;
            }
            return new TextPos(ix, len);
        } else {
            if (ct == 0) {
                return TextPos.ZERO;
            } else {
                ix = ct - 1;
                int len = getTextLength(ix);
                return new TextPos(ct - 1, len);
            }
        }
    }

    /**
     * Returns a TextPos corresponding to the end of the document.
     * @return the text position
     */
    public TextPos getDocumentEnd() {
        int ix = size() - 1;
        if (ix < 0) {
            return TextPos.ZERO;
        } else {
            return getEndOfParagraphTextPos(ix);
        }
    }

    /**
     * Returns a TextPos corresponding to the end of paragraph at the given index.
     * @param index the paragraph index
     * @return the text position
     */
    public TextPos getEndOfParagraphTextPos(int index) {
        int off = getTextLength(index);
        int cix = off - 1;
        if (cix < 0) {
            return new TextPos(index, off);
        } else {
            return new TextPos(index, off, cix, false);
        }
    }

    /**
     * Replaces the given range with the provided plain text.
     *
     * @param resolver the StyleResolver to use
     * @param start start text position
     * @param end end text position
     * @param text text string to insert
     * @param createUndo when true, creates an undo-redo entry
     * @return the text position at the end of the inserted text, or null if the model is read only
     */
    public TextPos replace(StyleResolver resolver, TextPos start, TextPos end, String text, boolean createUndo) {
        // TODO check for nulls
        if (isEditable()) {
            StyleAttrs a = getStyleAttrs(start);
            StyledInput in = StyledInput.of(text, a);
            return replace(resolver, start, end, in, createUndo);
        }
        return null;
    }
    
    /**
     * <p>
     * Replaces the given range with the provided styled text input.
     * When inserting plain text, the style is taken from the preceding text segment, or, if the text is being
     * inserted in the beginning of the document, the style is taken from the following text segment.
     * </p>
     * <p>
     * After the model applies the requested changes, an event is sent to all the registered ChangeListeners.
     * </p>
     * @param resolver the StyleResolver to use, can be null
     * @param start start text position
     * @param end end text position
     * @param input StyledInput
     * @param createUndo when true, creates an undo-redo entry
     * @return the text position at the end of the inserted text, or null if the model is read only
     */
    public TextPos replace(StyleResolver resolver, TextPos start, TextPos end, StyledInput input, boolean createUndo) {
        // TODO check for nulls
        if (isEditable()) {
            int cmp = start.compareTo(end);
            if (cmp > 0) {
                TextPos p = start;
                start = end;
                end = p;
            }

            UndoableChange ch = createUndo ? UndoableChange.create(this, start, end) : null;

            if (cmp != 0) {
                removeRegion(start, end);
            }

            int index = start.index();
            int offset = start.offset();
            int top = 0;
            int btm = 0;

            StyledSegment seg;
            while ((seg = input.nextSegment()) != null) {
                switch (seg.getType()) {
                case LINE_BREAK:
                    insertLineBreak(index, offset);
                    index++;
                    offset = 0;
                    btm = 0;
                    break;
                case PARAGRAPH_ATTRIBUTES:
                    StyleAttrs pa = seg.getStyleAttrs(resolver);
                    if (pa != null) {
                        applyStyle(index, pa);
                    }
                    break;
                case REGION:
                    offset = 0;
                    btm = 0;
                    index++;
                    Supplier<Region> gen = seg.getParagraphNodeGenerator();
                    insertParagraph(index, gen);
                    break;
                case TEXT:
                    int len = insertTextSegment(resolver, index, offset, seg);
                    if (index == start.index()) {
                        top += len;
                    }
                    offset += len;
                    btm += len;
                    break;
                }
            }

            int lines = index - start.index();
            if (lines == 0) {
                btm = 0;
            }

            fireChangeEvent(start, end, top, lines, btm);

            TextPos newEnd = new TextPos(index, offset);
            if (createUndo) {
                add(ch, newEnd);
            }
            return newEnd;
        }
        return null;
    }

    /**
     * Applies the specified style to the specified range.
     * The specified attributes will override any existing attributes.
     * When applying paragraph attributes, the affected range
     * might be wider than specified.
     * 
     * @param start the start of text range
     * @param end the end of text range
     * @param attrs the style attributes to apply
     */
    public final void applyStyle(TextPos start, TextPos end, StyleAttrs attrs) {
        if (isEditable()) {
            if (start.compareTo(end) > 0) {
                TextPos p = start;
                start = end;
                end = p;
            }

            TextPos evStart;
            TextPos evEnd;
            boolean changed;

            StyleAttrs pa = attrs.getParagraphAttrs();
            if (pa == null) {
                evStart = start;
                evEnd = end;
                changed = false;
            } else {
                evStart = new TextPos(start.index(), 0, 0, true);
                evEnd = getEndOfParagraphTextPos(end.index());
                changed = true;
            }

            UndoableChange ch = UndoableChange.create(this, evStart, evEnd);

            if (pa != null) {
                // apply paragraph attributes
                for (int ix = start.index(); ix <= end.index(); ix++) {
                    applyStyle(ix, pa);
                }
            }

            // apply character styles
            StyleAttrs ca = attrs.getCharacterAttrs();
            if(ca != null) {
                int ix = start.index();
                if (ix == end.index()) {
                    applyStyle(ix, start.offset(), end.offset(), attrs);
                } else {
                    applyStyle(ix, start.offset(), Integer.MAX_VALUE, attrs);
                    ix++;
                    while (ix < end.index()) {
                        applyStyle(ix, 0, Integer.MAX_VALUE, attrs);
                        ix++;
                    }
                    applyStyle(ix, 0, end.offset(), attrs);
                }
                changed = true;
            }

            if (changed) {
                fireStyleChangeEvent(evStart, evEnd);
                add(ch, end);
            }
        }
    }

    private void clearUndoRedo() {
        undo = head;
    }

    // we are going to try putting undo management in the model
    private void add(UndoableChange ch, TextPos end) {
        if (ch == null) {
            // the undo-redo system is in inconsistent state, let's drop everything
            clearUndoRedo();
            return;
        }

        ch.setEndAfter(end);
        ch.setPrev(undo);
        undo.setNext(ch);
        undo = ch;
    }

    /**
     * Returns true if the model can undo the most recent change.
     * @return true if undoable
     */
    public final boolean isUndoable() {
        return (undo != head);
    }

    /**
     * Undoes the recent change, if possible, returning an array comprising [start, end] text positions
     * prior to the change.
     * Returns null when the undo operation is not possible. 
     * @param resolver the StyleResolver to use
     * @return the [start, end] text positions prior to the change
     */
    public final TextPos[] undo(StyleResolver resolver) {
        if (undo != head) {
            try {
                undo.undo(resolver);
                TextPos[] sel = undo.getSelectionBefore();
                undo = undo.getPrev();
                return sel;
            } catch (IOException e) {
                // undo-redo is in inconsistent state, clear
                clearUndoRedo();
            }
        }
        return null;
    }

    /**
     * Returns true if the model can redo the most recent change.
     * @return true if redoable
     */
    public final boolean isRedoable() {
        return (undo.getNext() != null);
    }

    /**
     * Redoes the recent change, if possible, returning an array comprising [start, end] text positions
     * prior to the change.
     * Returns null when the redo operation is not possible. 
     * @param resolver the StyleResolver to use
     * @return the [start, end] text positions prior to the change
     */
    public final TextPos[] redo(StyleResolver resolver) {
        if (undo.getNext() != null) {
            try {
                undo.getNext().redo(resolver);
                TextPos[] sel = undo.getNext().getSelectionAfter();
                undo = undo.getNext();
                return sel;
            } catch (IOException e) {
                // undo-redo is in inconsistent state, clear
                clearUndoRedo();
            }
        }
        return null;
    }

    /** for debugging */
    private String dump() {
        StringBuilder sb = new StringBuilder(2048);
        try {
            sb.append("\n");
            TextPos end = getDocumentEnd();
            exportText(TextPos.ZERO, end, new StyledOutput() {
                @Override
                public void append(StyledSegment seg) throws IOException {
                    sb.append(" ");
                    sb.append(seg);
                    sb.append("\n");
                }

                @Override
                public void flush() throws IOException {
                }

                @Override
                public void close() throws IOException {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
