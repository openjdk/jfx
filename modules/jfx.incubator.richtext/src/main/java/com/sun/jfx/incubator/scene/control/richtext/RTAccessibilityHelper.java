/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jfx.incubator.scene.control.richtext;

import java.util.Objects;
import javafx.scene.AccessibleAttribute;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * The purpose of this class is to link the RichTextArea to the accessibility API.
 * <p>
 * These APIs generally accept a single String and integer offsets for caret position
 * and selection, which is totally impossible to implement in the context of a large
 * virtualized text model, as it
 * a) uses TextPos for encapsulating the text position instead of an int, and
 * b) it may not be possible to represent selected text as a single String for large models.
 */
public class RTAccessibilityHelper {
    private final RichTextArea control;
    private final StyledTextModel.Listener modelListener;
    private AccessibilitySegment segment;

    public RTAccessibilityHelper(RichTextArea t) {
        this.control = t;

        // we can get rid of this listener pointer by making RTAccessibilityHelper extend StyledTextModel.Listener
        modelListener = (ch) -> {
            if (ch.isEdit()) {
                if (isAccSegmentAffected(ch.getStart(), ch.getEnd())) {
                    segment = null;
                    control.notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
                }
            }
        };

        t.selectionProperty().addListener((s, old, cur) -> {
            handleSelectionChange(old, cur);
        });
    }

    public String getText() {
        AccessibilitySegment a = segment();
        if (a == null) {
            return null;
        }
        return a.text();
    }

    public Integer selectionStart() {
        SelectionSegment sel = control.getSelection();
        if (sel == null) {
            return null;
        }
        return computeOffset(sel.getMin());
    }

    public Integer selectionEnd() {
        SelectionSegment sel = control.getSelection();
        if (sel == null) {
            return null;
        }
        return computeOffset(sel.getMax());
    }

    public Integer caretOffset() {
        SelectionSegment sel = control.getSelection();
        if (sel == null) {
            return null;
        }
        return computeOffset(sel.getCaret());
    }

    public void registerModel(StyledTextModel m) {
        m.addListener(modelListener);
    }

    public void unregisterModel(StyledTextModel m) {
        m.removeListener(modelListener);
    }

    public void handleModelChange() {
        control.notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
    }

    private AccessibilitySegment segment() {
        SelectionSegment sel = control.getSelection();
        if (sel == null) {
            segment = null;
            return null;
        }

        TextPos start;
        TextPos end;
        if (sel.isCollapsed()) {
            // collapsed selection: report paragraph
            int ix = sel.getMin().index();
            start = TextPos.ofLeading(ix, 0);
            end = control.getParagraphEnd(ix);
        } else {
            // selection: report selection with limit?
            start = sel.getMin();
            end = sel.getMax();
            int ct = end.index() - start.index();
            if (ct > Params.ACCESSIBILITY_TEXT_MAX_PARAGRAPHS) {
                end = control.getParagraphEnd(start.index() + Params.ACCESSIBILITY_TEXT_MAX_PARAGRAPHS);
            }
        }

        if (segment != null) {
            // check if can reuse the existing segment
            if (start.equals(segment.start) && end.equals(segment.end)) {
                return segment;
            }
        }

        return (segment = new AccessibilitySegment(start, end));
    }

    // returns true if the edit is within the a11y segment
    private boolean isAccSegmentAffected(TextPos start, TextPos end) {
        return (segment == null) ? false : segment.isAffected(start, end);
    }

    private void handleSelectionChange(SelectionSegment old, SelectionSegment cur) {
        TextPos min0 = old == null ? null : old.getMin();
        TextPos max0 = old == null ? null : old.getMax();
        TextPos min2 = cur == null ? null : cur.getMin();
        TextPos max2 = cur == null ? null : cur.getMax();

        if (shouldUpdateTextAttribute(cur)) {
            control.notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
        }

        if (!Objects.equals(min0, min2)) {
            control.notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTION_START);
        }

        if (!Objects.equals(max0, max2)) {
            control.notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTION_END);
        }
    }

    private boolean shouldUpdateTextAttribute(SelectionSegment sel) {
        if (sel == null) {
            // was there a segment before?
            if (segment != null) {
                segment = null;
                return true;
            }
            return false;
        }
        return segment().isAffected(sel.getMin(), sel.getMax());
    }

    private Integer computeOffset(TextPos p) {
        AccessibilitySegment seg = segment();
        return seg == null ? null : seg.computeOffset(p);
    }

    // accessibility segment: either selected text, or the current paragraph
    private class AccessibilitySegment {
        private final TextPos start;
        private final TextPos end;
        private String text;

        public AccessibilitySegment(TextPos start, TextPos end) {
            this.start = start;
            this.end = end;
        }

        public String text() {
            if (text == null) {
                // alternative: use model.export()
                StringBuilder sb = new StringBuilder();
                String nl = control.getLineEnding().getText();
                int mx = end.index();
                for (int ix = start.index(); ix <= mx; ix++) {
                    String s = control.getPlainText(ix);
                    sb.append(s);
                    sb.append(nl);
                }
                text = sb.toString();
            }
            return text;
        }

        public boolean isAffected(TextPos updStart, TextPos updEnd) {
            if (updEnd.index() < start.index()) {
                return false;
            } else if (updStart.index() > end.index()) {
                return false;
            }
            return true;
        }

        private Integer computeOffset(TextPos p) {
            if ((p.compareTo(start) < 0) || (p.compareTo(end) > 0)) {
                return null;
            }

            int nl = control.getLineEnding().getText().length();
            int off = 0;
            int index = start.index();
            while (index < p.index()) {
                String s = control.getPlainText(index);
                off += (s.length() + nl);
                index++;
            }
            return off + p.offset();
        }
    }
}
