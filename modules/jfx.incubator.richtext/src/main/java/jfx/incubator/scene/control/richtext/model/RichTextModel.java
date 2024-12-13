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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.layout.Region;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * Editable, in-memory {@link StyledTextModel} based on a collection of styled segments.
 * <p>
 * This model is suitable for relatively small documents as it has neither disk storage backing
 * nor storage of incremental changes.
 *
 * @since 24
 */
public class RichTextModel extends StyledTextModel {
    private final ArrayList<RParagraph> paragraphs = new ArrayList<>();
    private final HashMap<StyleAttributeMap,StyleAttributeMap> styleCache = new HashMap<>();

    /**
     * Constructs the empty model.
     */
    public RichTextModel() {
        registerDataFormatHandler(RichTextFormatHandler.getInstance(), true, true, 2000);
        registerDataFormatHandler(RtfFormatHandler.getInstance(), true, true, 1000);
        registerDataFormatHandler(HtmlExportFormatHandler.getInstance(), true, false, 100);
        registerDataFormatHandler(PlainTextFormatHandler.getInstance(), true, true, 0);
        // always has at least one paragraph
        paragraphs.add(new RParagraph());
    }

    @Override
    public final boolean isWritable() {
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
    public int getParagraphLength(int index) {
        return paragraphs.get(index).getTextLength();
    }

    @Override
    public RichParagraph getParagraph(int index) {
        RParagraph p = paragraphs.get(index);
        return p.createRichParagraph();
    }

    @Override
    protected int insertTextSegment(int index, int offset, String text, StyleAttributeMap attrs) {
        attrs = dedup(attrs);
        RParagraph par = paragraphs.get(index);
        par.insertText(offset, text, attrs);
        return text.length();
    }

    @Override
    protected void insertLineBreak(int index, int offset) {
        if (index >= size()) {
            // unlikely to happen
            RParagraph par = new RParagraph();
            paragraphs.add(par);
        } else {
            RParagraph par = paragraphs.get(index);
            RParagraph par2 = par.insertLineBreak(offset);
            paragraphs.add(index + 1, par2);
        }
    }

    @Override
    protected void removeRange(TextPos start, TextPos end) {
        int ix = start.index();
        RParagraph par = paragraphs.get(ix);

        if (ix == end.index()) {
            par.removeSpan(start.offset(), end.offset());
        } else {
            RParagraph last = paragraphs.get(end.index());
            last.removeSpan(0, end.offset());

            par.removeSpan(start.offset(), Integer.MAX_VALUE);
            par.append(last);

            int ct = end.index() - ix;
            ix++;
            for (int i = 0; i < ct; i++) {
                paragraphs.remove(ix);
            }
        }
    }

    @Override
    protected void insertParagraph(int index, Supplier<Region> generator) {
        throw new UnsupportedOperationException();
    }

    /** deduplicates style attributes. */
    private StyleAttributeMap dedup(StyleAttributeMap a) {
        // the expectation is that the number of different style combinations is relatively low
        // but the number of instances can be large
        // the drawback is that there is no way to clear the cache
        StyleAttributeMap cached = styleCache.get(a);
        if (cached != null) {
            return cached;
        }
        styleCache.put(a, a);
        return a;
    }

    @Override
    protected void setParagraphStyle(int index, StyleAttributeMap attrs) {
        paragraphs.get(index).setParagraphAttributes(attrs);
    }

    @Override
    protected void applyStyle(int index, int start, int end, StyleAttributeMap attrs, boolean merge) {
        paragraphs.get(index).applyStyle(start, end, attrs, merge, this::dedup);
    }

    @Override
    public StyleAttributeMap getStyleAttributeMap(StyleResolver resolver, TextPos pos) {
        int index = pos.index();
        if (index < paragraphs.size()) {
            int off = pos.offset();
            RParagraph par = paragraphs.get(index);
            StyleAttributeMap pa = par.getParagraphAttributes();
            StyleAttributeMap a = par.getStyleAttributeMap(off);
            if (pa == null) {
                return a;
            } else {
                return pa.combine(a);
            }
        }
        return StyleAttributeMap.EMPTY;
    }

    /**
     * Temporary method added for testing; will be removed in production.
     * @param model the model
     * @param out the output
     */
    private static void dump(StyledTextModel model, PrintStream out) {
        if (model instanceof RichTextModel m) {
            m.dump(out);
        }
    }

    private void dump(PrintStream out) {
        out.println("[");
        for (RParagraph p : paragraphs) {
            dump(p, out);
        }
        out.println("]");
    }

    private void dump(RParagraph p, PrintStream out) {
        out.println("  {paragraphAttrs=" + p.getParagraphAttributes() + ", segments=[");
        for(RSegment s: p) {
            out.println("    {text=\"" + s.text() + "\", attr=" + s.getStyleAttributeMap() + "},");
        }
        out.println("  ]}");
    }

    /**
     * Represents a rich text segment having the same style attributes.
     */
    private static class RSegment {
        private String text;
        private StyleAttributeMap attrs;

        public RSegment(String text, StyleAttributeMap attrs) {
            this.text = text;
            this.attrs = attrs;
        }

        public String text() {
            return text;
        }

        public StyleAttributeMap attrs() {
            return attrs;
        }

        public void setAttrs(StyleAttributeMap a) {
            attrs = a;
        }

        public StyleAttributeMap getStyleAttributeMap() {
            return attrs;
        }

        public int getTextLength() {
            return text.length();
        }

        /** returns true if this segment becomes empty as a result */
        // TODO unit test
        public boolean removeRegion(int start, int end) {
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

            return (text.length() == 0);
        }

        public void append(String s) {
            text = text + s;
        }

        public void setText(String s) {
            text = s;
        }
    }

    /**
     * Model paragraph is a list of RSegments.
     */
    static class RParagraph extends ArrayList<RSegment> {

        private StyleAttributeMap paragraphAttrs;

        /** Creates an instance */
        public RParagraph() {
        }

        public StyleAttributeMap getParagraphAttributes() {
            return paragraphAttrs;
        }

        public void setParagraphAttributes(StyleAttributeMap a) {
            paragraphAttrs = a;
        }

        public String getPlainText() {
            StringBuilder sb = new StringBuilder();
            for(RSegment s: this) {
                sb.append(s.text());
            }
            return sb.toString();
        }

        public int getTextLength() {
            int len = 0;
            for(RSegment s: this) {
                len += s.getTextLength();
            }
            return len;
        }

        /**
         * Retrieves the style attributes from the previous character (or next, if at the beginning).
         * @param offset the offset
         * @return the style info
         */
        public StyleAttributeMap getStyleAttributeMap(int offset) {
            int off = 0;
            int ct = size();
            for (int i = 0; i < ct; i++) {
                RSegment seg = get(i);
                int len = seg.getTextLength();
                if (offset < (off + len) || (i == ct - 1)) {
                    return seg.getStyleAttributeMap();
                }
                off += len;
            }
            return StyleAttributeMap.EMPTY;
        }

        /**
         * Inserts styled text at the specified offset.
         * @param offset the insertion offset
         * @param text the plain text
         * @param attrs the style attributes
         */
        public void insertText(int offset, String text, StyleAttributeMap attrs) {
            int off = 0;
            int ct = size();
            for (int i = 0; i < ct; i++) {
                if (offset == off) {
                    // insert at the beginning
                    insertSegment2(i, text, attrs);
                    return;
                } else {
                    RSegment seg = get(i);
                    int len = seg.getTextLength();
                    if ((offset > off) && (offset <= off + len)) {
                        // split segment
                        StyleAttributeMap a = seg.attrs();
                        String toSplit = seg.text();
                        int ix = offset - off;

                        String s1 = toSplit.substring(0, ix);
                        set(i++, new RSegment(s1, a));
                        if (insertSegment2(i, text, attrs)) {
                            i++;
                        }
                        if (ix < toSplit.length()) {
                            String s2 = toSplit.substring(ix);
                            insertSegment2(i, s2, a);
                        }
                        return;
                    }

                    off += len;
                }
            }

            // insert at the end
            insertSegment2(ct, text, attrs);
        }

        /**
         * Inserts a new segment, or merges with adjacent segment if styles are the same.
         * Returns true if a segment has been added.
         * @param ix the segment index
         * @param text the plain text
         * @param a the style attributes
         * @return true if a segment has been added.
         */
        private boolean insertSegment2(int ix, String text, StyleAttributeMap a) {
            if (ix == 0) {
                // FIX aaaa combine with insertSegment
                if (ix < size()) {
                    RSegment seg = get(ix);
                    if (seg.getTextLength() == 0) {
                        // replace zero width segment
                        seg.setText(text);
                        seg.setAttrs(a);
                        return false;
                    } else if (a.equals(seg.attrs())) {
                        // combine
                        seg.setText(text + seg.text());
                        return false;
                    }
                }
            } else if (ix > 0) {
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

        /**
         * inserts a new segment with the specified, deduplicated attributes.
         * if the new style is the same as the previous segment, merges text with the previous segment instead.
         * @return true if the new segment has been merged with the previous segment
         */
        // TODO should it also merge with the next segment if the styles are the same?
        // in this case it's better to return an int which is the amount of segments added/removed
        private boolean insertSegment(int ix, String text, StyleAttributeMap a) {
            // TODO deal with zero width segment
            // FIX aaaa combine with insertSegment2
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
         * Trims this paragraph and returns the remaining text to be inserted after the line break.
         * @param offset the offset
         * @return the remaining portion of paragraph
         */
        public RParagraph insertLineBreak(int offset) {
            RParagraph next = new RParagraph();
            next.setParagraphAttributes(getParagraphAttributes());

            int off = 0;
            int i;
            int ct = size();
            for (i = 0; i < ct; i++) {
                RSegment seg = get(i);
                int len = seg.getTextLength();
                if (offset < (off + len)) {
                    if (offset != off) {
                        // split segment
                        StyleAttributeMap a = seg.attrs();
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

            // preserve attributes using zero width segment
            if (size() == 0) {
                if (next.size() > 0) {
                    StyleAttributeMap a = next.get(0).getStyleAttributeMap();
                    add(new RSegment("", a));
                }
            }
            if (next.size() == 0) {
                if (size() > 0) {
                    StyleAttributeMap a = get(size() - 1).getStyleAttributeMap();
                    next.add(new RSegment("", a));
                }
            }

            return next;
        }

        /**
         * Appends the specified paragraph by adding all of its segments.
         * @param p the paragraph to append
         */
        public void append(RParagraph p) {
            if (isMerge(p)) {
                int sz = p.size();
                for(int i=0; i<sz; i++) {
                    RSegment seg = p.get(i);
                    if(i == 0) {
                        // merge
                        RSegment last = get(size() - 1);
                        last.append(seg.text());
                    } else {
                        add(seg);
                    }
                }
                return;
            } else if (isZeroWidth()) {
                // remove zero width paragraph as it's no longer needed
                clear();
            }
            // TODO merge with previous?
            addAll(p);
        }

        private boolean isMerge(RParagraph p) {
            if(size() == 0) {
                return false; // should never happen
            } else if(p.size() == 0) {
                return false; // should never happen
            }
            return get(size() - 1).getStyleAttributeMap().equals(p.get(0).getStyleAttributeMap());
        }

        private boolean isZeroWidth() {
            if (size() == 1) {
                if (get(0).getTextLength() == 0) {
                    return true;
                }
            }
            return false;
        }

        // TODO keep zero width segment with attributes
        public void removeSpan(int start, int end) {
            if (start == end) {
                // no change
                return;
            }

            int ix0 = -1;
            int off0 = 0;
            int off = 0;
            int ct = size();

            // find start segment
            int i = 0;
            for (; i < ct; i++) {
                RSegment seg = get(i);
                int len = seg.getTextLength();
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
                int len = seg.getTextLength();
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
                if (seg.removeRegion(off0, off1)) {
                    remove(ix0);
                    if (size() == 0) {
                        // keep attributes in a zero width segment
                        add(new RSegment("", seg.getStyleAttributeMap()));
                    }
                }
            } else {
                // spans multiple segments
                // first segment
                RSegment seg = get(ix0);
                if (seg.removeRegion(off0, Integer.MAX_VALUE)) {
                    remove(ix0);
                    ix1--;
                    ct--;
                } else {
                    ix0++;
                }
                // last segment
                if (ix1 < 0) {
                    ix1 = ct;
                } else {
                    RSegment seg2 = get(ix1);
                    if (seg2.removeRegion(0, off1)) {
                        remove(ix1);
                        // TODO check for zero segment
                    }
                }
                // remove in-between segments
                removeRange(ix0, ix1);
                if (size() == 0) {
                    // keep attributes in a zero width segment
                    add(new RSegment("", seg.getStyleAttributeMap()));
                }
            }
        }

        public void applyStyle(int start, int end, StyleAttributeMap attrs, boolean merge, Function<StyleAttributeMap,StyleAttributeMap> dedup) {
            int off = 0;
            int i = 0;
            for ( ; i < size(); i++) {
                RSegment seg = get(i);
                int len = seg.getTextLength();
                int cs = whichCase(off, off + len, start, end);
                switch (cs) {
                case 0:
                    break;
                case 1:
                case 2:
                    if (applyStyle(i, seg, attrs, merge, dedup)) {
                        i--;
                    }
                    break;
                case 3:
                case 9:
                    applyStyle(i, seg, attrs, merge, dedup);
                    return;
                case 4:
                case 8:
                    // split
                    {
                        StyleAttributeMap a = seg.attrs();
                        StyleAttributeMap newAttrs = dedup.apply(merge ? a.combine(attrs) : attrs);
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
                        StyleAttributeMap a = seg.attrs();
                        StyleAttributeMap newAttrs = dedup.apply(merge ? a.combine(attrs) : attrs);
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
                        StyleAttributeMap a = seg.attrs();
                        StyleAttributeMap newAttrs = dedup.apply(merge ? a.combine(attrs) : attrs);
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
         * Applies style to the segment.
         * If the new style is exactly the same as the style of the previous segment,
         * it simply merges the two segments.
         * @param ix the paragraph index
         * @param seg the segment
         * @param a the attributes
         * @param merge whether to merge or set
         * @param dedup the deduplicator
         * @return true if this segment has been merged with the previous segment
         */
        private boolean applyStyle(int ix, RSegment seg, StyleAttributeMap a, boolean merge, Function<StyleAttributeMap,StyleAttributeMap> dedup) {
            StyleAttributeMap newAttrs = dedup.apply(merge ? seg.attrs().combine(a) : a);
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
         * <pre>
         * paragraph:    start [=============] end
         *      case:
         *               0:                  |-
         *               0:                      |-
         *               1:  -------------------->
         *               2:    |----------------->
         *               3:    |-------------|
         *               4:    |--------|
         *               5:        |------------->
         *               6:        |---------|
         *               7:        |----|
         *               8:  -----------|
         *               9:  ----------------|
         */
        private static int whichCase(int off, int max, int start, int end) {
            // TODO unit test!
            if (start >= max) {
                if ((start == max) && (off == max)) {
                    // empty paragraph
                    return 3;
                }
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

        private RichParagraph createRichParagraph() {
            RichParagraph.Builder b = RichParagraph.builder();
            for (RSegment seg : this) {
                String text = seg.text();
                StyleAttributeMap a = seg.attrs();
                b.addSegment(text, a);
            }
            b.setParagraphAttributes(paragraphAttrs);
            return b.build();
        }
    }
}
