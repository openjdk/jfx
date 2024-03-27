/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.rich.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.Region;
import com.sun.javafx.ModuleUtil;
import com.sun.jfx.incubator.scene.control.rich.Markers;
import com.sun.jfx.incubator.scene.control.rich.UndoableChange;
import com.sun.jfx.incubator.scene.control.rich.util.RichUtils;
import jfx.incubator.scene.control.rich.Marker;
import jfx.incubator.scene.control.rich.StyleResolver;
import jfx.incubator.scene.control.rich.TextPos;

/**
 * The base class for styled text models used by the
 * {@link jfx.incubator.scene.control.rich.RichTextArea RichTextArea}.
 * <p>
 * This class handles the following functionality with the intent
 * to simplify custom models:
 * <ul>
 * <li>managing listeners
 * <li>firing events
 * <li>decomposing the edits into multiple operations performed on individual paragraphs
 * <li>managing {@link Marker}s
 * </ul>
 *
 * <h2>Editing</h2>
 * The model supports editing when {@link #isUserEditable()} returns {@code true}.
 * Three methods participate in modification of the content:
 * {@link #replace(StyleResolver, TextPos, TextPos, String, boolean)},
 * {@link #replace(StyleResolver, TextPos, TextPos, StyledInput, boolean)},
 * {@link #applyStyle(TextPos, TextPos, StyleAttrs, boolean)}.
 * These methods decompose the main modification into operations with individual paragraphs
 * and delegate these to subclasses.
 * <p>
 * At the end of this process, an event is sent to all the {@link ChangeListener}s, followed by the
 * skin requesting the updated paragraphs when required.
 *
 * <h2>Creating a Paragraph</h2>
 * The model presents its content to the view(s) via immutable {@link RichParagraph}.
 * There are three ways of styling: using inline {@link StyleAttrs attributes}, relying on
 * style names in the application style sheet, or using direct styles.
 *
 * <h2>Extending the Model</h2>
 * The subclasses are free to choose how the data is stored, the only limitation is that the model neither
 * stores nor caches any {@link javafx.scene.Node Node}s, since multiple skins might be attached to the same
 * model.  When required, the model may contain properties which can be bound to the Nodes created in
 * {@link #getParagraph(int)}.  It is the responsibility of the model to serialize and deserialize the value
 * of such properties.
 *
 * @since 999 TODO
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
     * Indicates whether the model supports editing by the user.
     *
     * @return true if the model supports editing by the user
     */
    public abstract boolean isUserEditable();

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
     * This method makes no guarantees that the same paragraph instance will be returned for the same model index.
     *
     * @param index paragraph index in the range (0...{@link #size()})
     * @return a new instance of TextCell created
     */
    public abstract RichParagraph getParagraph(int index);

    /**
     * This method gets called only if the model is editable.
     * The caller guarantees that {@code start} precedes {@code end}.
     *
     * @param start the start of the region to be removed
     * @param end the end of the region to be removed, expected to be greater than the start position
     */
    protected abstract void removeRegion(TextPos start, TextPos end);

    /**
     * This method is called to insert a single text segment at the given position.
     *
     * @param index the paragraph index
     * @param offset the insertion offset within the paragraph
     * @param text the text to insert
     * @param attrs the style attributes
     * @return the number of characters inserted
     */
    protected abstract int insertTextSegment(int index, int offset, String text, StyleAttrs attrs);

    /**
     * Inserts a line break.
     *
     * @param index the model index
     * @param offset the text offset
     */
    protected abstract void insertLineBreak(int index, int offset);

    /**
     * Inserts a paragraph that contains a single {@link javafx.scene.Node}.
     * @param index model index
     * @param generator code that will be used to create a Node instance
     */
    protected abstract void insertParagraph(int index, Supplier<Region> generator);

    /**
     * Replaces the paragraph styles in the specified paragraph.
     *
     * @param ix the paragraph index
     * @param paragraphAttrs the paragraph attributes
     */
    protected abstract void setParagraphStyle(int ix, StyleAttrs paragraphAttrs);

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
     * @param merge determines whether to merge with or overwrite the existing attributes
     */
    protected abstract void applyStyle(int ix, int start, int end, StyleAttrs a, boolean merge);

    /**
     * Returns the {@link StyleAttrs} of the first character at the specified position.
     * When at the end of the document, returns the attributes of the last character.
     *
     * @param resolver the style resolver
     * @param pos the text position
     * @return the style attributes, non-null
     */
    public abstract StyleAttrs getStyleAttrs(StyleResolver resolver, TextPos pos);

    /**
     * Returns a set of supported attributes to be used for filtering in
     * {@link #applyStyle(TextPos, TextPos, StyleAttrs, boolean)},
     * {@link #replace(StyleResolver, TextPos, TextPos, StyledInput, boolean)}, and
     * {@link #replace(StyleResolver, TextPos, TextPos, String, boolean)}.
     * <p>
     * A {@code null} value disables filtering.
     *
     * @return the supported attributes, or null
     */
    protected Set<StyleAttribute<?>> getSupportedAttributes() { return null; }

    /** stores the handler and its priority */
    private static record FHPriority(DataFormatHandler handler, int priority) implements Comparable<FHPriority>{
        @Override
        public int compareTo(FHPriority x) {
            int d = x.priority - priority;
            if (d == 0) {
                // compare formats to guard against someone adding two different formats under the same priority
                String us = handler().getDataFormat().toString();
                String them = x.handler().getDataFormat().toString();
                d = them.compareTo(us);
            }
            return d;
        }
    }

    private record FHKey(DataFormat format, boolean forExport) { }

    static { ModuleUtil.incubatorWarning(); }

    // TODO should it hold WeakReferences?
    private final CopyOnWriteArrayList<ChangeListener> listeners = new CopyOnWriteArrayList();
    private final HashMap<FHKey,FHPriority> handlers = new HashMap<>(2);
    private final Markers markers = new Markers();
    private final UndoableChange head = UndoableChange.createHead();
    private final ReadOnlyBooleanWrapper undoable = new ReadOnlyBooleanWrapper(this, "undoable", false);
    private final ReadOnlyBooleanWrapper redoable = new ReadOnlyBooleanWrapper(this, "redoable", false);
    private UndoableChange undo = head;

    /** The constructor. */
    public StyledTextModel() {
        registerDataFormatHandler(new RtfFormatHandler(), true, false, 1000);
        registerDataFormatHandler(new HtmlExportFormatHandler(), true, false, 100);
        registerDataFormatHandler(new PlainTextFormatHandler(), true, false, 0);
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
     * Registers a format handler for either export and/or import operations.
     * Priority determines the format chosen for operations with the {@link javafx.scene.input.Clipboard}
     * when input data is available in more than one supported format.
     * <p>
     * This method is expected to be called by a StyledTextModel implementation constructor.
     *
     * @param h data format handler
     * @param forExport true if the handler supports export operations
     * @param forImport true if the handler supports import operations
     * @param priority from 0 (lowest, usually plain text) to {@code Integer.MAX_VALUE}
     */
    protected void registerDataFormatHandler(DataFormatHandler h, boolean forExport, boolean forImport, int priority) {
        FHPriority p = new FHPriority(h, priority);
        if (forExport) {
            handlers.put(new FHKey(h.getDataFormat(), true), p);
        }
        if (forImport) {
            handlers.put(new FHKey(h.getDataFormat(), false), p);
        }
    }

    /**
     * Removes the data format handler registered previously with
     * {@link #registerDataFormatHandler(DataFormatHandler, boolean, boolean, int)}.
     *
     * @param f the data format
     * @param forExport whether to remove an export handler
     * @param forImport whether to remove an import handler
     */
    protected void removeDataFormatHandler(DataFormat f, boolean forExport, boolean forImport) {
        if (forExport) {
            handlers.remove(new FHKey(f, true));
        }
        if (forImport) {
            handlers.remove(new FHKey(f, false));
        }
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
     * Exports the stream of {@code StyledSegment}s in the given range to the specified
     * {@code StyledOutput}.
     * @param start start of the range
     * @param end end of the range
     * @param out {@link StyledOutput} to receive the stream
     * @throws IOException when an I/O error occurs
     */
    public void export(TextPos start, TextPos end, StyledOutput out) throws IOException {
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
            // sent last after the paragraph has been created
            StyleAttrs pa = par.getParagraphAttributes();
            out.append(StyledSegment.ofParagraphAttributes(pa));
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
     * <p>
     * This is a convenience method which eventually calls
     * {@link #replace(StyleResolver, TextPos, TextPos, StyledInput, boolean)}
     * with the attributes provided by {@link #getStyleAttrs(StyleResolver, TextPos)} at the
     * {@code start} position.
     *
     * @param resolver the StyleResolver to use
     * @param start start text position
     * @param end end text position
     * @param text text string to insert
     * @param allowUndo when true, creates an undo-redo entry
     * @return the text position at the end of the inserted text, or null if the model is read only
     */
    public TextPos replace(StyleResolver resolver, TextPos start, TextPos end, String text, boolean allowUndo) {
        if (isUserEditable()) {
            StyleAttrs a = getStyleAttrs(resolver, start);
            StyledInput in = StyledInput.of(text, a);
            return replace(resolver, start, end, in, allowUndo);
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
     * @param start the start text position
     * @param end the end text position
     * @param input the input content stream
     * @param allowUndo when true, creates an undo-redo entry
     * @return the text position at the end of the inserted text, or null if the model is read only
     */
    public TextPos replace(StyleResolver resolver, TextPos start, TextPos end, StyledInput input, boolean allowUndo) {
        if (isUserEditable()) {
            // TODO clamp to document boundaries
            int cmp = start.compareTo(end);
            if (cmp > 0) {
                TextPos p = start;
                start = end;
                end = p;
            }

            UndoableChange ch = allowUndo ? UndoableChange.create(this, start, end) : null;

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
                    setParagraphStyle(index, pa);
                    break;
                case REGION:
                    offset = 0;
                    btm = 0;
                    index++;
                    Supplier<Region> gen = seg.getParagraphNodeGenerator();
                    insertParagraph(index, gen);
                    break;
                case TEXT:
                    String text = seg.getText();
                    StyleAttrs a = seg.getStyleAttrs(resolver);
                    if (a == null) {
                        a = StyleAttrs.EMPTY;
                    } else {
                        a = filterUnsupportedAttributes(a);
                    }
                    int len = insertTextSegment(index, offset, text, a);
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
            if (allowUndo) {
                add(ch, newEnd);
            }
            return newEnd;
        }
        return null;
    }

    /**
     * Applies the style attributes to the specified range in the document.<p>
     * Depending on {@code mergeAttributes} parameter, the attributes will either be merged with (true) or completely
     * replace the existing attributes within the range.  The affected range might be wider than the range specified
     * when applying the paragraph attributes.
     * <p>
     * This operation is undoable.
     *
     * @param start the start of text range
     * @param end the end of text range
     * @param attrs the style attributes to set
     * @param mergeAttributes whether to merge or replace the attributes
     */
    public final void applyStyle(TextPos start, TextPos end, StyleAttrs attrs, boolean mergeAttributes) {
        if (isUserEditable()) {
            if (start.compareTo(end) > 0) {
                TextPos p = start;
                start = end;
                end = p;
            }

            attrs = filterUnsupportedAttributes(attrs);

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
                // set paragraph attributes
                for (int ix = start.index(); ix <= end.index(); ix++) {
                    setParagraphStyle(ix, pa);
                }
            }

            // apply character styles
            StyleAttrs ca = attrs.getCharacterAttrs();
            if (ca != null) {
                int ix = start.index();
                if (ix == end.index()) {
                    applyStyle(ix, start.offset(), end.offset(), attrs, mergeAttributes);
                } else {
                    applyStyle(ix, start.offset(), Integer.MAX_VALUE, attrs, mergeAttributes);
                    ix++;
                    while (ix < end.index()) {
                        applyStyle(ix, 0, Integer.MAX_VALUE, attrs, mergeAttributes);
                        ix++;
                    }
                    applyStyle(ix, 0, end.offset(), attrs, mergeAttributes);
                }
                changed = true;
            }

            if (changed) {
                fireStyleChangeEvent(evStart, evEnd);
                add(ch, end);
            }
        }
    }

    /**
     * Removes unsupported attributes per {@link #getSupportedAttributes()}.
     * @param attrs the input attributes
     * @return the attributes that exclude unsupported ones
     */
    private StyleAttrs filterUnsupportedAttributes(StyleAttrs attrs) {
        Set<StyleAttribute<?>> supported = getSupportedAttributes();
        if (supported == null) {
            return attrs;
        }

        StyleAttrs.Builder b = StyleAttrs.builder();
        for (StyleAttribute a : attrs.getAttributes()) {
            if (supported.contains(a)) {
                b.set(a, attrs.get(a));
            }
        }
        return b.build();
    }

    /**
     * Clears the undo-redo stack.
     */
    public void clearUndoRedo() {
        undo = head;
        updateUndoRedo();
    }

    /**
     * Adds an {@code UndoableChange} to the undo/redo buffer.
     * @param ch the change
     * @param end the caret position after the change
     */
    protected void add(UndoableChange ch, TextPos end) {
        if (ch == null) {
            // the undo-redo system is in inconsistent state, let's drop everything
            clearUndoRedo();
            return;
        }

        ch.setEndAfter(end);
        ch.setPrev(undo);
        undo.setNext(ch);
        undo = ch;
        updateUndoRedo();
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
                updateUndoRedo();
                return sel;
            } catch (IOException e) {
                // undo-redo is in inconsistent state, clear
                clearUndoRedo();
            }
        }
        return null;
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
                updateUndoRedo();
                return sel;
            } catch (IOException e) {
                // undo-redo is in inconsistent state, clear
                clearUndoRedo();
            }
        }
        return null;
    }

    /**
     * The property describes if it's currently possible to undo the latest change of the content that was done.
     * @return the read-only property
     * @defaultValue false
     */
    public final ReadOnlyBooleanProperty undoableProperty() {
        return undoable.getReadOnlyProperty();
    }

    public final boolean isUndoable() {
        return undoable.get();
    }

    private void setUndoable(boolean on) {
        undoable.set(on);
    }

    /**
     * The property describes if it's currently possible to redo the latest change of the content that was undone.
     * @return the read-only property
     * @defaultValue false
     */
    public final ReadOnlyBooleanProperty redoableProperty() {
        return redoable.getReadOnlyProperty();
    }

    public final boolean isRedoable() {
        return redoable.get();
    }

    private void setRedoable(boolean on) {
        redoable.set(on);
    }

    private void updateUndoRedo() {
        setUndoable(undo != head);
        setRedoable(undo.getNext() != null);
    }

    /** for debugging */
    private String dump() {
        StringBuilder sb = new StringBuilder(2048);
        try {
            sb.append("\n");
            TextPos end = getDocumentEnd();
            export(TextPos.ZERO, end, new StyledOutput() {
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

    /**
     * Replaces the content of the model with the data read from the input stream,
     * using the specified {@code DataFormat}.
     * @param r the style resolver
     * @param f the data format
     * @param input the input stream
     * @throws IOException in case of an I/O error
     * @throws UnsupportedOperationException when the data format is not supported by the model
     */
    public final void read(StyleResolver r, DataFormat f, InputStream input) throws IOException {
        clearUndoRedo();
        TextPos end = getDocumentEnd();
        DataFormatHandler h = getDataFormatHandler(f, false);
        if (h == null) {
            throw new UnsupportedOperationException("format not supported: " + f);
        }
        String text = RichUtils.readString(input);
        StyledInput in = h.createStyledInput(text);
        replace(r, TextPos.ZERO, end, in, false);
    }

    /**
     * Writes the model content to the output stream using the specified {@code DataFormat}.
     * @param r the style resolver
     * @param f the data format
     * @param out the output stream
     * @throws IOException in case of an I/O error
     * @throws UnsupportedOperationException when the data format is not supported by the model
     */
    public final void write(StyleResolver r, DataFormat f, OutputStream out) throws IOException {
        TextPos end = getDocumentEnd();
        DataFormatHandler h = getDataFormatHandler(f, true);
        if (h == null) {
            throw new UnsupportedOperationException("format not supported: " + f);
        }
        h.save(this, r, TextPos.ZERO, end, out);
    }
}
