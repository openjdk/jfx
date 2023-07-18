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
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.rich.StyleResolver;
import javafx.scene.control.rich.TextCell;
import javafx.scene.control.rich.TextPos;
import javafx.scene.input.DataFormat;
import com.sun.javafx.scene.control.rich.RichTextFormatHandler;

/**
 * Editable, in-memory {@link StyledTextModel} based on a collection of styled segments.
 * <p>
 * This model is suitable for relatively small documents as has neither disk storage backing nor
 * incremental storage of the changes.
 */
public class EditableRichTextModel extends StyledTextModel {
    /** Represents a styled text */
    public static final DataFormat DATA_FORMAT = new DataFormat("application/x-com-oracle-editable-rich-text-model");
    private final ArrayList<RParagraph> paragraphs = new ArrayList<>();
    private final HashMap<StyleAttrs,StyleAttrs> styleCache = new HashMap<>();
    
    public EditableRichTextModel() {
        paragraphs.add(new RParagraph());

        registerDataFormatHandler(new RichTextFormatHandler(), Integer.MAX_VALUE);
        registerDataFormatHandler(new RtfFormatHandler(), 1000);
        registerDataFormatHandler(new HtmlExportFormatHandler(), true, 100);
        registerDataFormatHandler(new PlainTextFormatHandler(), 0);
    }

    @Override
    public final boolean isEditable() {
        return true;
    }

    @Override
    public int size() {
        return paragraphs.size();
    }
    
    @Override
    public String getPlainText(int index) {
        return paragraphs.get(index).getPlainText();
    }

    @Override
    public TextCell createTextCell(int index) {
        // TODO empty paragraph?
        TextCell c = new TextCell(index);
        RParagraph p = paragraphs.get(index);
        for (RSegment seg: p) {
            String text = seg.text();
            String style = seg.attrs().getStyle();
            c.addSegment(text, style, null);
        }
        return c;
    }

    @Override
    protected int insertTextSegment(StyleResolver resolver, int index, int offset, StyledSegment segment) {
        String text = segment.getText();
        StyleAttrs a = getStyleAttrs(resolver, segment);

        RParagraph par = paragraphs.get(index);
        par.insertText(offset, text, a);
        return text.length();
    }
    
    private StyleAttrs getStyleAttrs(StyleResolver resolver, StyledSegment segment) {
        StyleAttrs a = segment.getStyleAttrs(resolver);
        if(a == null) {
            a = StyleAttrs.EMPTY;
        }
        return dedup(a);
    }

    @Override
    protected void insertLineBreak(int index, int offset) {
        if(index >= size()) {
            paragraphs.add(new RParagraph());
        } else {
            RParagraph par = paragraphs.get(index);
            RParagraph par2 = par.insertLineBreak(offset);
            paragraphs.add(index + 1, par2);
        }
    }

    @Override
    protected void removeRegion(TextPos start, TextPos end) {
        int ix = start.index();
        RParagraph par = paragraphs.get(ix);

        if (ix == end.index()) {
            par.removeRegion(start.offset(), end.offset());
        } else {
            RParagraph last = paragraphs.get(end.index());
            last.removeRegion(0, end.offset());
            
            par.removeRegion(start.offset(), Integer.MAX_VALUE);
            par.append(last);

            int ct = end.index() - ix;
            ix++;
            for (int i = 0; i < ct; i++) {
                paragraphs.remove(ix);
            }
        }
    }

    @Override
    protected void insertParagraph(int index, Supplier<Node> generator) {
        // TODO
    }

    @Override
    protected void exportParagraph(int index, int start, int end, StyledOutput out) throws IOException {
        RParagraph par = paragraphs.get(index);
        par.export(start, end, out);
    }

    @Override
    protected boolean applyStyleImpl(TextPos start, TextPos end, StyleAttrs a) {
        a = dedup(a);
        int ix = start.index();
        RParagraph par = paragraphs.get(ix);

        if (ix == end.index()) {
            par.applyStyle(start.offset(), end.offset(), a, this::dedup);
        } else {
            par.applyStyle(start.offset(), Integer.MAX_VALUE, a, this::dedup);
            ix++;
            while (ix < end.index()) {
                par = paragraphs.get(ix);
                par.applyStyle(0, Integer.MAX_VALUE, a, this::dedup);
                ix++;
            }
            par = paragraphs.get(ix);
            par.applyStyle(0, end.offset(), a, this::dedup);
        }
        return true;
    }

    /** deduplicates style attributes. */
    private StyleAttrs dedup(StyleAttrs a) {
        // the expectation is that the number of different style combinations is relatively low
        // but the number of instances can be large
        // the drawback is that there is no way to clear the cache
        StyleAttrs cached = styleCache.get(a);
        if (cached != null) {
            return cached;
        }
        styleCache.put(a, a);
        return a;
    }

    @Override
    public StyleInfo getStyleInfo(TextPos pos) {
        int index = pos.index();
        if(index < paragraphs.size()) {
            int off = pos.offset();
            RParagraph par = paragraphs.get(index);
            return par.getStyleInfo(off);
        }
        return StyleInfo.NONE;
    }

    /**
     * Model rich text segment.
     * TODO add paragraph segment
     */
    private static class RSegment {
        private String text;
        private StyleAttrs attrs;
        
        public RSegment(String text, StyleAttrs attrs) {
            this.text = text;
            this.attrs = attrs;
        }
        
        public String text() {
            return text;
        }
        
        public StyleAttrs attrs() {
            return attrs;
        }
        
        public void setAttrs(StyleAttrs a) {
            attrs = a;
        }
        
        public StyleInfo getStyleInfo() {
            return StyleInfo.of(attrs);
        }
        
        public int length() {
            return text.length();
        }

        // TODO unit test
        public void removeRegion(int start, int end) {
            int len = text.length();
            if (end > len) {
                end = len;
            }

            if (start == 0) {
                if (end < len) {
                    text = text.substring(end);
                } else {
                    text = "";
                }
            } else {
                if (end < len) {
                    text = text.substring(0, start) + text.substring(end, len);
                } else {
                    text = text.substring(0, start);
                }
            }
        }

        public void append(String s) {
            text = text + s;
        }

        public StyledSegment createStyledSegment(int start, int end) {
            String s;
            if ((start == 0) && (end == text.length())) {
                s = text;
            } else {
                s = text.substring(start, end);
            }
            return StyledSegment.of(s, getStyleInfo());
        }
    }

    /**
     * Model paragraph is a list of RSegments.
     */
    protected static class RParagraph extends ArrayList<RSegment> {
        public String getPlainText() {
            StringBuilder sb = new StringBuilder();
            for(RSegment s: this) {
                sb.append(s.text());
            }
            return sb.toString();
        }

        public int length() {
            return getPlainText().length();
        }

        /** retrieves the style info from the previous character (or next, if at the beginning) */
        public StyleInfo getStyleInfo(int offset) {
            int off = 0;
            int ct = size();
            for (int i = 0; i < ct; i++) {
                RSegment seg = get(i);
                int len = seg.length();
                if (offset < (off + len) || (i == ct - 1)) {
                    return seg.getStyleInfo();
                }
                off += len;
            }
            return StyleInfo.NONE;
        }

        public void export(int start, int end, StyledOutput out) throws IOException {
            int off = 0;
            int ct = size();
            for (int i = 0; i < ct; i++) {
                if(off >= end) {
                    return;
                }
                
                RSegment seg = get(i);
                int len = seg.length();
                if(start <= (off + len)) {
                    int ix0 = Math.max(0, start - off);
                    int ix1 = Math.min(len, end - off);
                    StyledSegment ss = seg.createStyledSegment(ix0, ix1);
                    out.append(ss);
                }
                off += len;
            }
        }

        public void insertText(int offset, String text, StyleAttrs attrs) {
            int off = 0;
            int ct = size();
            for (int i = 0; i < ct; i++) {
                if (offset == off) {
                    // insert at the beginning
                    insert(i, text, attrs);
                    return;
                } else {
                    RSegment seg = get(i);
                    int len = seg.length();
                    if ((offset > off) && (offset <= off + len)) {
                        // split segment
                        StyleAttrs a = seg.attrs();
                        String toSplit = seg.text();
                        int ix = offset - off;

                        String s1 = toSplit.substring(0, ix);
                        set(i++, new RSegment(s1, a));
                        if (insert(i, text, attrs)) {
                            i++;
                        }
                        if (ix < toSplit.length()) {
                            String s2 = toSplit.substring(ix);
                            insert(i, s2, a);
                        }
                        return;
                    }

                    off += len;
                }
            }

            // insert at the end
            insert(ct, text, attrs);
        }

        /**
         * Inserts a new segment, or appends to the previous segment if style is the same.
         * Returns true if a segment has been added.
         */
        private boolean insert(int ix, String text, StyleAttrs a) {
            if (ix > 0) {
                RSegment prev = get(ix - 1);
                if (a.equals(prev.attrs())) {
                    // combine
                    prev.append(text);
                    return false;
                }
            }

            RSegment seg = new RSegment(text, a);
            if (ix < size()) {
                add(ix, seg);
            } else {
                add(seg);
            }
            return true;
        }

        /** trims this paragraph and returns the remainder to be inserted next */
        public RParagraph insertLineBreak(int offset) {
            int off = 0;
            // FIX styles!
            // problem: has no segments to store style info, initially.
            // perhaps it needs a zero width segment with styles
            RParagraph next = new RParagraph();
            int i;
            int ct = size();
            for (i = 0; i < ct; i++) {
                RSegment seg = get(i);
                int len = seg.length();
                if (offset < (off + len)) {
                    if (offset != off) {
                        // split segment
                        StyleAttrs a = seg.attrs();
                        String toSplit = seg.text();
                        int ix = offset - off;
                        String s1 = toSplit.substring(0, ix);
                        String s2 = toSplit.substring(ix);
                        set(i, new RSegment(s1, a));

                        next.add(new RSegment(s2, a));
                        i++;
                    }
                    break;
                }
                off += len;
            }

            // move remaining segments to the next paragraph
            while (i < size()) {
                RSegment seg = remove(i);
                next.add(seg);
            }

            return next;
        }

        public void append(RParagraph p) {
            addAll(p);
        }

        public void removeRegion(int start, int end) {
            int ix0 = -1;
            int off0 = 0;
            int off = 0;
            int ct = size();

            // find start segment
            int i = 0;
            for (; i < ct; i++) {
                RSegment seg = get(i);
                int len = seg.length();
                if (start < (off + len)) {
                    ix0 = i;
                    off0 = start - off;
                    break;
                }
                off += len;
            }

            if (ix0 < 0) {
                // start not found
                return;
            }

            // find end segment
            int ix1 = -1;
            int off1 = -1;
            for (; i < ct; i++) {
                RSegment seg = get(i);
                int len = seg.length();
                if (end <= (off + len)) {
                    ix1 = i;
                    off1 = end - off;
                    break;
                }
                off += len;
            }

            if (ix0 == ix1) {
                // same segment
                RSegment seg = get(ix0);
                seg.removeRegion(off0, off1);
            } else {
                // spans multiple segments
                // first segment
                if (off0 > 0) {
                    RSegment seg = get(ix0);
                    seg.removeRegion(off0, Integer.MAX_VALUE);
                    ix0++;
                }
                // last segment
                if(ix1 < 0) {
                    ix1 = ct;
                } else {
                    RSegment seg = get(ix1);
                    seg.removeRegion(0, off1);
                }
                // remove in-between segments
                removeRange(ix0, ix1);
            }
        }

        public void applyStyle(int start, int end, StyleAttrs attrs, Function<StyleAttrs,StyleAttrs> dedup) {
            int off = 0;
            int i = 0;
            for ( ; i < size(); i++) {
                RSegment seg = get(i);
                int len = seg.length();
                int cs = whichCase(off, off + len, start, end);
                switch (cs) {
                case 0:
                    break;
                case 1:
                case 2:
                    if (applyStyle(i, seg, attrs, dedup)) {
                        i--;
                    }
                    break;
                case 3:
                case 9:
                    applyStyle(i, seg, attrs, dedup);
                    return;
                case 4:
                case 8:
                    // split
                    {
                        StyleAttrs a = seg.attrs();
                        StyleAttrs newAttrs = dedup.apply(a.combine(attrs));
                        int ix = end - off;
                        String s1 = seg.text().substring(0, ix);
                        String s2 = seg.text().substring(ix);
                        remove(i);
                        if (insertSegment(i++, s1, newAttrs)) {
                            i--;
                        }
                        if (insertSegment(i, s2, a)) {
                            i--;
                        }
                    }
                    return;
                case 5:
                case 6:
                    // split
                    {
                        StyleAttrs a = seg.attrs();
                        StyleAttrs newAttrs = dedup.apply(a.combine(attrs));
                        int ix = start - off;
                        String s1 = seg.text().substring(0, ix);
                        String s2 = seg.text().substring(ix);
                        remove(i);
                        if (insertSegment(i++, s1, a)) {
                            i--;
                        }
                        if (insertSegment(i, s2, newAttrs)) {
                            i--;
                        }
                    }
                    if (cs == 6) {
                        return;
                    }
                    break;
                case 7:
                    {
                        StyleAttrs a = seg.attrs();
                        StyleAttrs newAttrs = dedup.apply(a.combine(attrs));
                        String text = seg.text();
                        int ix0 = start - off;
                        int ix1 = end - off;
                        String s1 = text.substring(0, ix0);
                        String s2 = text.substring(ix0, ix1);
                        String s3 = text.substring(ix1);
                        remove(i);
                        if (insertSegment(i++, s1, a)) {
                            i--;
                        }
                        if (insertSegment(i++, s2, newAttrs)) {
                            i--;
                        }
                        if (insertSegment(i, s3, a)) {
                            i--;
                        }
                    }
                    return;
                default:
                    throw new Error("?" + cs);
                }
                
                off += len;
            }
        }

        /** 
         * applies style to the segment.
         * if the new style is the same as the previous segment, merges text with the previous segment.
         * @return true if this segment has been merged with the previous segment
         */
        private boolean applyStyle(int ix, RSegment seg, StyleAttrs a, Function<StyleAttrs,StyleAttrs> dedup) {
            StyleAttrs newAttrs = dedup.apply(seg.attrs().combine(a));
            if (ix > 0) {
                RSegment prev = get(ix - 1);
                if (prev.attrs().equals(newAttrs)) {
                    // merge
                    prev.append(seg.text());
                    remove(ix);
                    return true;
                }
            }
            seg.setAttrs(newAttrs);
            return false;
        }
        
        /** 
         * inserts a new segment with the specified, deduplicated attributes.
         * if the new style is the same as the previous segment, merges text with the previous segment instead.
         * @return true if the new segment has been merged with the previous segment
         */
        // TODO should it also merge with the next segment if the styles are the same?
        // in this case it's better to return an int which is the amount of segments added/removed
        private boolean insertSegment(int ix, String text, StyleAttrs a) {
            if (ix > 0) {
                RSegment prev = get(ix - 1);
                if (prev.attrs().equals(a)) {
                    // merge
                    prev.append(text);
                    return true;
                }
            }
            RSegment seg = new RSegment(text, a);
            if (ix >= size()) {
                add(seg);
            } else {
                add(ix, seg);
            }
            return false;
        }

        /**
         * <pre>
         * paragraph:    [=============]
         * case:
         *         0:                      |-
         *         1:  -------------------->
         *         2:    |----------------->
         *         3:    |-------------|
         *         4:    |--------|
         *         5:        |------------->
         *         6:        |---------|
         *         7:        |----|
         *         8:  -----------|
         *         9:  ----------------|
         */
        private static int whichCase(int off, int max, int start, int end) {
            // TODO unit test!
            if (start >= max) {
                return 0;
            } else if (start < off) {
                if (end > max) {
                    return 1;
                } else if (end < max) {
                    return 8;
                } else {
                    return 9;
                }
            } else if (start > off) {
                if (end > max) {
                    return 5;
                } else if (end < max) {
                    return 7;
                } else {
                    return 6;
                }
            } else {
                if (end > max) {
                    return 2;
                } else if (end < max) {
                    return 4;
                } else {
                    return 3;
                }
            }
        }
    }
}
