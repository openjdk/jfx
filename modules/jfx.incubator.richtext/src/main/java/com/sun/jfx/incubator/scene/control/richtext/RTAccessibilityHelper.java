/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * The purpose of this class is to maintain a small String segment
 * around the RichTextArea caret to use with the accessibility API.
 * <p>
 * These APIs generally accept a single String and integer offsets for caret position
 * and selection, which is totally impossible to implement in the context of a large
 * virtualized text model, as it a) uses TextPos for encapsulating the text position
 * instead of an int, and it may not be possible to represent selected text as a
 * String for large models.
 */
public class RTAccessibilityHelper {
    private final RichTextArea control;
    private TextPos start;
    private TextPos end;

    public RTAccessibilityHelper(RichTextArea t) {
        this.control = t;
    }

    // FIX to be removed later
    private void p(String fmt, Object... args) {
        if (false) {
            System.out.println(String.format(fmt, args));
        }
    }

    /** clear a11y cache */
    public void handleModelChange() {
        p("handleModelChange");
        start = null;
        end = null;
    }

    /** returns true if update is within the a11y window */
    public boolean handleTextUpdate(TextPos p0, TextPos p1) {
        p("handleTextUpdate %s %s", start, end);

        if ((start != null) && (end != null)) {
            if (p0.compareTo(end) >= 0) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Handles selection changes.
     * @return true if a11y window has shifted and TEXT attribute needs to be sent to the platform
     */
    public boolean handleSelectionChange(SelectionSegment sel) {
        if (sel == null) {
            return false;
        }

        // no start/end: create window, return true
        // selection outside of the window: change window, return true
        if((start == null) || (end == null) || isOutside(sel.getMin(), sel.getMax())) {
            createWindow();
            return true;
        }

        // selection within the window: no-op, return false
        return false;
    }

    private boolean isOutside(TextPos p0, TextPos p1) {
        if ((start.compareTo(p1) >= 0) || (end.compareTo(p0) <= 0)) {
            return true;
        }
        return false;
    }

    private void createWindow() {
        // selection is small: create window around selection
        // selection is large: create window around the caret
        SelectionSegment sel = control.getSelection();
        TextPos cp = sel.getCaret();
        int ix0 = Math.max(0, cp.index() - (Params.ACCESSIBILITY_WINDOW_SIZE / 2));
        int ix1 = Math.min(control.getParagraphCount() - 1, ix0 + Params.ACCESSIBILITY_WINDOW_SIZE);

        start = TextPos.ofLeading(ix0, 0);
        end = control.getParagraphEnd(ix1);
    }

    public String getText() {
        if ((start == null) && (end == null)) {
            return null;
        }

        // alternative: use model.export()
        StringBuilder sb = new StringBuilder();
        int mx = end.index();
        for (int ix = start.index(); ix <= mx; ix++) {
            String s = control.getPlainText(ix);
            sb.append(s).append("\n");
        }
        return sb.toString();
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

    private Integer computeOffset(TextPos p) {
        if (
            (start == null) ||
            (end == null) ||
            (p.compareTo(start) < 0) ||
            (p.compareTo(end) > 0)
        ) {
            return null;
        }

        int off = 0;
        int index = start.index();
        while (index < p.index()) {
            String s = control.getPlainText(index);
            off += (s.length() + 1); // plus newline character
            index++;
        }
        return off + p.offset();
    }
}
