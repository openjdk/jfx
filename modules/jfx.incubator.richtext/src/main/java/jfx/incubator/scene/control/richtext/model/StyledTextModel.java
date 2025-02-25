/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.richtext.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.Region;
import com.sun.javafx.ModuleUtil;
import com.sun.jfx.incubator.scene.control.richtext.Markers;
import com.sun.jfx.incubator.scene.control.richtext.StyleAttributeMapHelper;
import com.sun.jfx.incubator.scene.control.richtext.UndoableChange;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.richtext.Marker;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * The base class for styled text models used by the
 * {@link jfx.incubator.scene.control.richtext.RichTextArea RichTextArea}.
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
 * The model supports editing when {@link #isWritable()} returns {@code true}.
 * Three methods participate in modification of the content:
 * {@link #replace(StyleResolver, TextPos, TextPos, String, boolean)},
 * {@link #replace(StyleResolver, TextPos, TextPos, StyledInput, boolean)},
 * {@link #applyStyle(TextPos, TextPos, StyleAttributeMap, boolean)}.
 * These methods decompose the main modification into operations with individual paragraphs
 * and delegate these to subclasses.
 * <p>
 * At the end of this process, an event is sent to all the {@link Listener}s, followed by the
 * skin requesting the updated paragraphs when required.
 *
 * <h2>Creating a Paragraph</h2>
 * The model presents its content to the view(s) via immutable {@link RichParagraph}.
 * There are two ways of adding styles to the model:
 * <ul>
 * <li>Using the style names in the stylesheet or inline styles (example: {@code "-fx-font-size:200%;"}), or
 * <li>Using attributes defined in {@link StyleAttributeMap}, such as bold typeface, italic, and so on.
 *     In this case, the resulting paragraph appearance is decoupled from the stylesheet and will look the same
 *     regardless of the active stylesheet.
 * </ul>
 * The latter method is intended for applications where an editable control is needed, such as general purpose
 * rich text editor, the former is designed for view-only informational controls that must follow the application
 * theme and therefore are coupled to the stylesheet.
 *
 * <h2>Extending the Model</h2>
 * The subclasses are free to choose how the data is stored, the only limitation is that the model neither
 * stores nor caches any {@link javafx.scene.Node Node}s, since multiple skins might be attached to the same
 * model.  When required, the model may contain properties which can be bound to the Nodes created in
 * {@link #getParagraph(int)}.  It is the responsibility of the model to store and restore the values
 * of such properties.
 *
 * @since 24
 */
public abstract class StyledTextModel {
    /**
     * Receives information about modifications of the model.
     */
    @FunctionalInterface
    public interface Listener {
        /**
         * Informs the listener that the model content has changed.
         * @param ch the change
         */
        public void onContentChange(ContentChange ch);
    }

    /**
     * Indicates whether the model supports content modifications made via
     * {@code applyStyle()},
     * {@code replace()},
     * {@code undo()},
     * {@code redo()}
     * methods.
     * <p>
     * Note that even when this method returns {@code false}, the model itself may still update its content
     * and fire the change events as a response, for example, to changes in its backing data storage.
     *
     * @return true if the model supports content modifications
     */
    public abstract boolean isWritable();

    /**
     * Returns the number of paragraphs in the model.
     *
     * @return number of paragraphs
     */
    public abstract int size();

    /**
     * Returns the plain text string for the specified paragraph.  The returned text string cannot be null
     * and must not contain any control characters other than TAB.
     * The callers must ensure that the value of {@code index} is within the valid document range,
     * since doing otherwise might result in an exception or undetermined behavior.
     *
     * @param index the paragraph index in the range (0...{@link #size()})
     * @return the non-null paragraph text string
     */
    public abstract String getPlainText(int index);

    /**
     * Returns a {@link RichParagraph} at the given model index.
     * The callers must ensure that the value of {@code index} is within the valid document range,
     * since doing otherwise might result in an exception or undetermined behavior.
     * <p>
     * This method makes no guarantees that the same paragraph instance will be returned for the same model index.
     *
     * @param index the paragraph index in the range (0...{@link #size()})
     * @return the instance of {@code RichParagraph}
     */
    public abstract RichParagraph getParagraph(int index);

    /**
     * Removes the specified text range.
     * This method gets called only if the model is editable.
     * The caller guarantees that {@code start} precedes {@code end}.
     *
     * @param start the start of the range to be removed
     * @param end the end of the range to be removed, expected to be greater than the start position
     * @throws UnsupportedOperationException if the model is not {@link #isWritable() writable}
     */
    protected abstract void removeRange(TextPos start, TextPos end);

    /**
     * This method is called to insert a single styled text segment at the given position.
     *
     * @param index the paragraph index
     * @param offset the insertion offset within the paragraph
     * @param text the text to insert
     * @param attrs the style attributes
     * @return the number of characters inserted
     * @throws UnsupportedOperationException if the model is not {@link #isWritable() writable}
     */
    protected abstract int insertTextSegment(int index, int offset, String text, StyleAttributeMap attrs);

    /**
     * Inserts a line break at the specified position.
     *
     * @param index the model index
     * @param offset the text offset
     * @throws UnsupportedOperationException if the model is not {@link #isWritable() writable}
     */
    protected abstract void insertLineBreak(int index, int offset);

    /**
     * Inserts a paragraph that contains a single {@link Region}.
     * <p>
     * The model should not cache or otherwise retain references to the created {@code Region}s,
     * as they might be requested multiple times during the lifetime of the model, or by different views.
     * <p>
     * This method allows for embedding {@link javafx.scene.control.Control Control}s that handle user input.
     * In this case, the model should declare necessary properties and provide bidirectional bindings between
     * the properties in the model and the corresponding properties in the control, as well as handle copy, paste,
     * writing to and reading from I/O streams.
     *
     * @param index model index
     * @param generator code that will be used to create a Node instance
     * @throws UnsupportedOperationException if the model is not {@link #isWritable() writable}
     */
    protected abstract void insertParagraph(int index, Supplier<Region> generator);

    /**
     * Replaces the paragraph styles in the specified paragraph.
     *
     * @param index the paragraph index
     * @param paragraphAttrs the paragraph attributes
     * @throws UnsupportedOperationException if the model is not {@link #isWritable() writable}
     */
    protected abstract void setParagraphStyle(int index, StyleAttributeMap paragraphAttrs);

    /**
     * Applies style to the specified text range within a single paragraph.
     * The new attributes override any existing attributes.
     * The {@code end} argument may exceed the paragraph length, in which case the outcome should be the same
     * as supplying the paragraph length value.
     *
     * @param index the paragraph index
     * @param start the start offset
     * @param end the end offset
     * @param a the character attributes
     * @param merge determines whether to merge with or overwrite the existing attributes
     * @throws UnsupportedOperationException if the model is not {@link #isWritable() writable}
     */
    protected abstract void applyStyle(int index, int start, int end, StyleAttributeMap a, boolean merge);

    /**
     * Returns the {@link StyleAttributeMap} of the character at the specified position's {@code charIndex}.
     * When at the end of the document, returns the attributes of the last character.
     *
     * @param resolver the style resolver
     * @param pos the text position
     * @return the style attributes, non-null
     */
    public abstract StyleAttributeMap getStyleAttributeMap(StyleResolver resolver, TextPos pos);

    /**
     * Returns the set of attributes supported attributes.  When this method returns a non-null set,
     * it will be used by the methods which handle the
     * external input for the purpose of filtering out the attributes the model cannot understand, preventing
     * the attributes from being added to the model (for example, as a result of pasting from the system
     * clipboard).
     * <p>
     * The methods that utilize the filtering are:
     * {@link #applyStyle(TextPos, TextPos, StyleAttributeMap, boolean)},
     * {@link #replace(StyleResolver, TextPos, TextPos, StyledInput, boolean)}, and
     * {@link #replace(StyleResolver, TextPos, TextPos, String, boolean)}.
     * <p>
     * When this method returns {@code null}, no filtering is performed.
     * <p>
     * This method might be overridden by certain models.  The base class implementation returns {@code null}.
     *
     * @return the set of supported attributes, or null if the model requires no filtering
     */
    protected Set<StyleAttribute<?>> getSupportedAttributes() {
        return null;
    }

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
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList();
    private final HashMap<FHKey,FHPriority> handlers = new HashMap<>(2);
    private final Markers markers = new Markers();
    private final UndoableChange head = UndoableChange.createHead();
    private final ReadOnlyBooleanWrapper undoable = new ReadOnlyBooleanWrapper(this, "undoable", false);
    private final ReadOnlyBooleanWrapper redoable = new ReadOnlyBooleanWrapper(this, "redoable", false);
    private UndoableChange undo = head;

    /**
     * Constructs the instance of the model.
     * <p>
     * This constructor registers data handlers for RTF, HTML (export only), and plain text.
     */
    public StyledTextModel() {
        registerDataFormatHandler(RtfFormatHandler.getInstance(), true, false, 1000);
        registerDataFormatHandler(HtmlExportFormatHandler.getInstance(), true, false, 100);
        registerDataFormatHandler(PlainTextFormatHandler.getInstance(), true, false, 0);
    }

    /**
     * Adds a {@link Listener} to this model.
     *
     * @param listener a non-null listener
     */
    public final void addListener(StyledTextModel.Listener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a {@link Listener} from this model.
     * <p>
     * This method does nothing if this listener has never been added.
     *
     * @param listener a non-null listener
     */
    public final void removeListener(StyledTextModel.Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Registers a format handler for export and/or import operations.
     * The priority determines the format chosen for operations with the {@link javafx.scene.input.Clipboard}
     * when input data is available in more than one supported format.
     * The handler with the highest priority will be used by
     * {@link jfx.incubator.scene.control.richtext.RichTextArea#read(InputStream)} and
     * {@link jfx.incubator.scene.control.richtext.RichTextArea#write(OutputStream)} methods.
     * <p>
     * The same handler can be registered for input and export.  When registering multiple handlers
     * for the same data handler and import/export, the last registered one wins.
     * <p>
     * This method is expected to be called from a {@code StyledTextModel} child class constructor.
     *
     * @param h data format handler
     * @param forExport true if the handler supports export operations
     * @param forImport true if the handler supports import operations
     * @param priority from 0 (lowest, usually plain text) to {@code Integer.MAX_VALUE}
     */
    // TODO this method could be made public to allow for adding new handlers to existing models
    // such as RichTextModel.
    protected final void registerDataFormatHandler(DataFormatHandler h, boolean forExport, boolean forImport, int priority) {
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
     * @param forExport whether to remove the export handler
     * @param forImport whether to remove the import handler
     */
    protected final void removeDataFormatHandler(DataFormat f, boolean forExport, boolean forImport) {
        if (forExport) {
            handlers.remove(new FHKey(f, true));
        }
        if (forImport) {
            handlers.remove(new FHKey(f, false));
        }
    }

    /**
     * Returns an immutable list of supported data formats for either export or import operations,
     * in the order of priority - from high to low.
     * <p>
     * The top priority format will be used by
     * {@link jfx.incubator.scene.control.richtext.RichTextArea#read(InputStream)} and
     * {@link jfx.incubator.scene.control.richtext.RichTextArea#write(OutputStream)} methods.
     *
     * @param forExport determines whether the operation is export (true) or import (false)
     * @return the immutable list of supported formats
     */
    public final List<DataFormat> getSupportedDataFormats(boolean forExport) {
        ArrayList<FHPriority> fs = new ArrayList<>(handlers.size());
        handlers.forEach((k, p) -> {
            if (k.forExport == forExport) {
                fs.add(p);
            }
        });
        Collections.sort(fs);
        return fs.stream().
            map((h) -> h.handler().getDataFormat()).
            collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns a {@link DataFormatHandler} instance corresponding to the given {@link DataFormat}.
     * This method will return {@code null} if the data format is not supported.
     *
     * @param format data format
     * @param forExport for export (true) or for input (false)
     * @return DataFormatHandler or null
     */
    public final DataFormatHandler getDataFormatHandler(DataFormat format, boolean forExport) {
        FHKey k = new FHKey(format, forExport);
        FHPriority p = handlers.get(k);
        return p == null ? null : p.handler();
    }

    /**
     * Fires a text modification event for the given range.
     *
     * @param start start of the affected range
     * @param end end of the affected range
     * @param charsTop number of characters added before any added paragraphs
     * @param linesAdded number of paragraphs inserted
     * @param charsBottom number of characters added after any inserted paragraphs
     */
    public void fireChangeEvent(TextPos start, TextPos end, int charsTop, int linesAdded, int charsBottom) {
        ContentChange ch = ContentChange.ofEdit(start, end, charsTop, linesAdded, charsBottom);
        markers.update(start, end, charsTop, linesAdded, charsBottom);
        for (Listener li : listeners) {
            li.onContentChange(ch);
        }
    }

    /**
     * Fires a style change event for the given range.
     * This event indicates that only the styling has changed, with no  changes to any text positions.
     *
     * @param start the start position
     * @param end the end position, must be greater than the start position
     */
    public void fireStyleChangeEvent(TextPos start, TextPos end) {
        ContentChange ch = ContentChange.ofStyleChange(start, end);
        for (Listener li : listeners) {
            li.onContentChange(ch);
        }
    }

    /**
     * Returns the length of text in a paragraph at the specified index.
     *
     * @param index the paragraph index
     * @return the length
     */
    public int getParagraphLength(int index) {
        return getPlainText(index).length();
    }

    /**
     * Exports the stream of {@code StyledSegment}s in the given range to the specified
     * {@code StyledOutput}.
     * This method does not close the {@code StyledOutput}.
     *
     * @param start start of the range
     * @param end end of the range
     * @param out {@link StyledOutput} to receive the stream
     * @throws IOException when an I/O error occurs
     * @see #replace(StyleResolver, TextPos, TextPos, StyledInput, boolean)
     */
    public final void export(TextPos start, TextPos end, StyledOutput out) throws IOException {
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
            int len = getParagraphLength(ix0);
            boolean withParAttrs = ((soff == 0) && ((eoff >= len) || (eoff < 0)));
            exportParagraph(ix0, soff, eoff, withParAttrs, out);
        } else {
            // multi-line
            boolean lineBreak = false;
            for (int ix = ix0; ix <= ix1; ix++) {
                if (lineBreak) {
                    out.consume(StyledSegment.LINE_BREAK);
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
    protected final void exportParagraph(int index, int start, int end, boolean withParAttrs, StyledOutput out) throws IOException {
        RichParagraph par = getParagraph(index);
        par.export(start, end, out);
        if (withParAttrs) {
            // sent last after the paragraph has been created
            StyleAttributeMap pa = par.getParagraphAttributes();
            out.consume(StyledSegment.ofParagraphAttributes(pa));
        }
    }

    /**
     * Returns the {@link Marker} at the specified position.
     * The actual text position tracked by the marker will always be within the document boundaries.
     *
     * @param pos text position
     * @return Marker instance
     */
    public final Marker getMarker(TextPos pos) {
        TextPos p = clamp(pos);
        return markers.getMarker(p);
    }

    /**
     * Returns the text position guaranteed to be within the document and paragraph limits.
     *
     * @param p the text position, cannot be null
     * @return the text position within the document and paragraph limits
     */
    public final TextPos clamp(TextPos p) {
        Objects.nonNull(p);
        int len;
        int ct = size();
        int ix = p.index();
        if (ix < 0) {
            return TextPos.ZERO;
        } else if (ix < ct) {
            len = getParagraphLength(ix);
            if (p.offset() < len) {
                return p;
            }
        } else {
            if (ct == 0) {
                return TextPos.ZERO;
            } else {
                ix = ct - 1;
                len = getParagraphLength(ix);
            }
        }

        int cix = len - 1;
        if (cix < 0) {
            return TextPos.ofLeading(ix, len);
        } else {
            return new TextPos(ix, len, cix, false);
        }
    }

    /**
     * Returns the text position corresponding to the end of the document.
     * The start of the document can be referenced by the {@link TextPos#ZERO} constant.
     *
     * @return the text position
     * @see TextPos#ZERO
     */
    public final TextPos getDocumentEnd() {
        int ix = size() - 1;
        if (ix < 0) {
            return TextPos.ZERO;
        } else {
            return getEndOfParagraphTextPos(ix);
        }
    }

    /**
     * Returns a TextPos corresponding to the end of paragraph at the given index.
     *
     * @param index the paragraph index
     * @return the text position
     */
    public final TextPos getEndOfParagraphTextPos(int index) {
        int off = getParagraphLength(index);
        int cix = off - 1;
        if (cix < 0) {
            return TextPos.ofLeading(index, off);
        } else {
            return new TextPos(index, off, cix, false);
        }
    }

    /**
     * Replaces the given range with the provided plain text.
     * <p>
     * This is a convenience method which eventually calls
     * {@link #replace(StyleResolver, TextPos, TextPos, StyledInput, boolean)}
     * with the attributes provided by {@link #getStyleAttributeMap(StyleResolver, TextPos)} at the
     * {@code start} position.
     *
     * @param resolver the StyleResolver to use
     * @param start start text position
     * @param end end text position
     * @param text text string to insert
     * @param allowUndo when true, creates an undo-redo entry
     * @return the text position at the end of the inserted text, or null if the model is read only
     * @throws UnsupportedOperationException if the model is not {@link #isWritable() writable}
     */
    public final TextPos replace(StyleResolver resolver, TextPos start, TextPos end, String text, boolean allowUndo) {
        checkWritable();

        // TODO pick the lowest from start,end.  Possibly add (end) argument to getStyleAttributes?
        StyleAttributeMap a = getStyleAttributeMap(resolver, start);
        StyledInput in = StyledInput.of(text, a);
        return replace(resolver, start, end, in, allowUndo);
    }

    /**
     * Replaces the given range with the provided styled text input.
     * When inserting plain text, the style is taken from the preceding text segment, or, if the text is being
     * inserted in the beginning of the document, the style is taken from the following text segment.
     * <p>
     * After the model applies the requested changes, an event is sent to all the registered listeners.
     *
     * @param resolver the StyleResolver to use, can be null
     * @param start the start text position
     * @param end the end text position
     * @param input the input content stream
     * @param allowUndo when true, creates an undo-redo entry
     * @return the text position at the end of the inserted text, or null if the model is read only
     * @throws UnsupportedOperationException if the model is not {@link #isWritable() writable}
     */
    public final TextPos replace(StyleResolver resolver, TextPos start, TextPos end, StyledInput input, boolean allowUndo) {
        checkWritable();

        // TODO clamp to document boundaries
        int cmp = start.compareTo(end);
        if (cmp > 0) {
            TextPos p = start;
            start = end;
            end = p;
        }

        UndoableChange ch = allowUndo ? UndoableChange.create(this, start, end) : null;

        if (cmp != 0) {
            removeRange(start, end);
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
                StyleAttributeMap pa = seg.getStyleAttributeMap(resolver);
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
                StyleAttributeMap a = seg.getStyleAttributeMap(resolver);
                if (a == null) {
                    a = StyleAttributeMap.EMPTY;
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

        TextPos newEnd = TextPos.ofLeading(index, offset);
        if (allowUndo) {
            add(ch, newEnd);
        }
        return newEnd;
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
     * @throws UnsupportedOperationException if the model is not {@link #isWritable() writable}
     */
    public final void applyStyle(TextPos start, TextPos end, StyleAttributeMap attrs, boolean mergeAttributes) {
        checkWritable();

        if (start.compareTo(end) > 0) {
            TextPos p = start;
            start = end;
            end = p;
        }

        attrs = filterUnsupportedAttributes(attrs);

        TextPos evStart;
        TextPos evEnd;
        boolean changed;

        StyleAttributeMap pa = StyleAttributeMapHelper.getParagraphAttrs(attrs);
        if (pa == null) {
            evStart = start;
            evEnd = end;
            changed = false;
        } else {
            evStart = TextPos.ofLeading(start.index(), 0);
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
        StyleAttributeMap ca = StyleAttributeMapHelper.getCharacterAttrs(attrs);
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

    /**
     * Removes unsupported attributes per {@link #getSupportedAttributes()}.
     *
     * @param attrs the input attributes
     * @return the attributes that exclude unsupported ones
     */
    private StyleAttributeMap filterUnsupportedAttributes(StyleAttributeMap attrs) {
        Set<StyleAttribute<?>> supported = getSupportedAttributes();
        if (supported == null) {
            return attrs;
        }

        StyleAttributeMap.Builder b = StyleAttributeMap.builder();
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
    public final void clearUndoRedo() {
        undo = head;
        updateUndoRedo();
    }

    /**
     * Adds an {@code UndoableChange} to the undo/redo buffer.
     *
     * @param ch the change
     * @param end the caret position after the change
     */
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
        updateUndoRedo();
    }

    /**
     * Undoes the recent change, if possible, returning an array comprising [start, end] text positions
     * prior to the change.
     * Returns null when the undo operation is not possible.
     *
     * @param resolver the StyleResolver to use
     * @return the [start, end] text positions prior to the change
     * @throws UnsupportedOperationException if the model is not {@link #isWritable() writable}
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
     * @throws UnsupportedOperationException if the model is not {@link #isWritable() writable}
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
                public void consume(StyledSegment seg) throws IOException {
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
     * using the specified {@code DataFormat}.  This operation clears the undo/redo stack.
     *
     * @param r the style resolver
     * @param f the data format
     * @param input the input stream
     * @throws IOException in case of an I/O error
     * @throws UnsupportedOperationException when the data format is not supported by the model,
     *         or the model is not {@link StyledTextModel#isWritable() writable}
     */
    public final void read(StyleResolver r, DataFormat f, InputStream input) throws IOException {
        clearUndoRedo();
        TextPos end = getDocumentEnd();
        DataFormatHandler h = getDataFormatHandler(f, false);
        if (h == null) {
            throw new UnsupportedOperationException("format not supported: " + f);
        }
        String text = RichUtils.readString(input);
        StyledInput in = h.createStyledInput(text, null);
        replace(r, TextPos.ZERO, end, in, false);
    }

    /**
     * Writes the model content to the output stream using the specified {@code DataFormat}.
     *
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

    private void checkWritable() {
        if (!isWritable()) {
            throw new UnsupportedOperationException("the model is not writeable");
        }
    }
}
