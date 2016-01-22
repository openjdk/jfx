/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.win;

import static javafx.scene.AccessibleAttribute.*;
import java.text.BreakIterator;
import javafx.geometry.Bounds;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/*
 * This class is the Java peer for GlassTextRangeProvider.
 * GlassTextRangeProvider implements ITextRangeProvider.
 */
class WinTextRangeProvider {

    private native static void _initIDs();
    static {
        _initIDs();
    }

    /* TextPatternRangeEndpoint */
    private static final int TextPatternRangeEndpoint_Start    = 0;
    private static final int TextPatternRangeEndpoint_End      = 1;

    /* TextUnit enumeration */
    private static final int TextUnit_Character = 0;
    private static final int TextUnit_Format = 1;
    private static final int TextUnit_Word = 2;
    private static final int TextUnit_Line = 3;
    private static final int TextUnit_Paragraph = 4;
    private static final int TextUnit_Page = 5;
    private static final int TextUnit_Document = 6;

    /* Text Attribute Identifiers */
    private static final int UIA_FontNameAttributeId = 40005;
    private static final int UIA_FontSizeAttributeId = 40006;
    private static final int UIA_FontWeightAttributeId = 40007;
    private static final int UIA_IsHiddenAttributeId = 40013;
    private static final int UIA_IsItalicAttributeId = 40014;
    private static final int UIA_IsReadOnlyAttributeId = 40015;

    private static int idCount = 1;
    private int id;
    private int start, end;
    private WinAccessible accessible;
    private long peer;
    /* Creates a GlassTextRangeProvider linked to the caller (GlobalRef) */
    private native long _createTextRangeProvider(long accessible);

    /* Releases the GlassTextRangeProvider and deletes the GlobalRef */
    private native void _destroyTextRangeProvider(long textRangeProvider);

    WinTextRangeProvider(WinAccessible accessible) {
        this.accessible = accessible;
        peer = _createTextRangeProvider(accessible.getNativeAccessible());
        id = idCount++;
    }

    long getNativeProvider() {
        return peer;
    }

    void dispose() {
        _destroyTextRangeProvider(peer);
        peer = 0L;
    }

    void setRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    int getStart() {
        return start;
    }

    int getEnd() {
        return end;
    }

    @Override public String toString() {
        return "Range(start: "+start+", end: "+end+", id: " + id + ")";
    }

    private Object getAttribute(AccessibleAttribute attribute, Object... parameters) {
        return accessible.getAttribute(attribute, parameters);
    }

    private boolean isWordStart(BreakIterator bi, String text, int offset) {
        if (offset == 0) return true;
        if (offset == text.length()) return true;
        if (offset == BreakIterator.DONE) return true;
        return bi.isBoundary(offset) && Character.isLetterOrDigit(text.charAt(offset));
    }

    /***********************************************/
    /*            ITextRangeProvider               */
    /***********************************************/
    private long Clone() {
        WinTextRangeProvider clone = new WinTextRangeProvider(accessible);
        clone.setRange(start, end);

        /* Note: Currently Clone() natively does not call AddRef() on the returned object.
         * This mean JFX does not keep a reference to this object, consequently it does not
         * need to free it.
         */
        return clone.getNativeProvider();
    }

    private boolean Compare(WinTextRangeProvider range) {
        if (range == null) return false;
        return accessible == range.accessible && start == range.start && end == range.end;
    }

    private int CompareEndpoints(int endpoint, WinTextRangeProvider targetRange, int targetEndpoint) {
        int offset = endpoint == TextPatternRangeEndpoint_Start ? start : end;
        int targetOffset = targetEndpoint == TextPatternRangeEndpoint_Start ? targetRange.start : targetRange.end;
        return offset - targetOffset;
    }

    private void ExpandToEnclosingUnit(int unit) {
        String text = (String)getAttribute(TEXT);
        if (text == null) return;
        int length = text.length();
        if (length == 0) return;

        switch (unit) {
            case TextUnit_Character: {
                if (start == length) start--;
                end = start + 1;
                break;
            }
            case TextUnit_Format:
            case TextUnit_Word: {
                BreakIterator bi = BreakIterator.getWordInstance();
                bi.setText(text);
                if (!isWordStart(bi, text, start)) {
                    int offset = bi.preceding(start);
                    while (!isWordStart(bi, text, offset)) {
                        offset = bi.previous();
                    }
                    start = offset != BreakIterator.DONE ? offset : 0;
                }
                if (!isWordStart(bi, text, end)) {
                    int offset = bi.following(end);
                    while (!isWordStart(bi, text, offset)) {
                        offset = bi.next();
                    }
                    end = offset != BreakIterator.DONE ? offset : length;
                }
                break;
            }
            case TextUnit_Line: {
                Integer lineIndex = (Integer)getAttribute(LINE_FOR_OFFSET, start);
                Integer lineStart = (Integer)getAttribute(LINE_START, lineIndex);
                Integer lineEnd = (Integer)getAttribute(LINE_END, lineIndex);
                if (lineIndex == null || lineEnd == null || lineStart == null) {
                    /* Text Field */
                    start = 0;
                    end = length;
                } else {
                    /* Text Area */
                    start = lineStart;
                    end = lineEnd;
                }

                break;
            }
            case TextUnit_Paragraph: {
                Integer lineIndex = (Integer)getAttribute(LINE_FOR_OFFSET, start);
                if (lineIndex == null) {
                    /* Text Field */
                    start = 0;
                    end = length;
                } else {
                    /* Text Area */
                    BreakIterator bi = BreakIterator.getSentenceInstance();
                    bi.setText(text);
                    if (!bi.isBoundary(start)) {
                        int offset = bi.preceding(start);
                        start = offset != BreakIterator.DONE ? offset : 0;
                    }
                    int offset = bi.following(start);
                    end = offset != BreakIterator.DONE ? offset : length;
                }
                break;
            }
            case TextUnit_Page:
            case TextUnit_Document: {
                start = 0;
                end = length;
                break;
            }
        }

        /* Always ensure range consistency */
        start = Math.max(0, Math.min(start, length));
        end = Math.max(start, Math.min(end, length));
    }

    private long FindAttribute(int attributeId, WinVariant val, boolean backward) {
        System.err.println("FindAttribute NOT IMPLEMENTED");
        return 0;
    }

    private long FindText(String text, boolean backward, boolean ignoreCase) {
        if (text == null) return 0;
        String documentText = (String)getAttribute(TEXT);
        if (documentText == null) return 0;
        String rangeText = documentText.substring(start, end);
        if (ignoreCase) {
            rangeText = rangeText.toLowerCase();
            text = text.toLowerCase();
        }
        int index = -1;
        if (backward) {
            index = rangeText.lastIndexOf(text);
        } else {
            index = rangeText.indexOf(text);
        }
        if (index == -1) return 0;
        WinTextRangeProvider result = new WinTextRangeProvider(accessible);
        result.setRange(start + index, start + index + text.length());
        return result.getNativeProvider();
    }

    private WinVariant GetAttributeValue(int attributeId) {
        WinVariant variant = null;
        switch (attributeId) {
            case UIA_FontNameAttributeId: {
                Font font = (Font)getAttribute(FONT);
                if (font != null) {
                    variant = new WinVariant();
                    variant.vt = WinVariant.VT_BSTR;
                    variant.bstrVal = font.getName();
                }
                break;
            }
            case UIA_FontSizeAttributeId: {
                Font font = (Font)getAttribute(FONT);
                if (font != null) {
                    variant = new WinVariant();
                    variant.vt = WinVariant.VT_R8;
                    variant.dblVal = font.getSize();
                }
                break;
            }
            case UIA_FontWeightAttributeId: {
                Font font = (Font)getAttribute(FONT);
                if (font != null) {
                    boolean bold = font.getStyle().toLowerCase().contains("bold");
                    variant = new WinVariant();
                    variant.vt = WinVariant.VT_I4;
                    variant.lVal = bold ? FontWeight.BOLD.getWeight() : FontWeight.NORMAL.getWeight();
                }
                break;
            }
            case UIA_IsHiddenAttributeId:
            case UIA_IsReadOnlyAttributeId:
                variant = new WinVariant();
                variant.vt = WinVariant.VT_BOOL;
                variant.boolVal = false;
                break;
            case UIA_IsItalicAttributeId: {
                Font font = (Font)getAttribute(FONT);
                if (font != null) {
                    boolean italic = font.getStyle().toLowerCase().contains("italic");
                    variant = new WinVariant();
                    variant.vt = WinVariant.VT_BOOL;
                    variant.boolVal = italic;
                }
                break;
            }
            default:
//                System.out.println("GetAttributeValue " + attributeId + " Not implemented");
        }
        return variant;
    }

    private double[] GetBoundingRectangles() {
        String text = (String)getAttribute(TEXT);
        if (text == null) return null;
        int length = text.length();

        /* Narrator will not focus an empty text control if the bounds are NULL */
        if (length == 0) return new double[0];
        int endOffset = end;
        if (endOffset > 0 && endOffset > start && text.charAt(endOffset - 1) == '\n') {
            endOffset--;
        }
        if (endOffset > 0 && endOffset > start && text.charAt(endOffset - 1) == '\r') {
            endOffset--;
        }
        if (endOffset > 0 && endOffset > start && endOffset == length) {
            endOffset--;
        }
        Bounds[] bounds = (Bounds[])getAttribute(BOUNDS_FOR_RANGE, start, endOffset);
        if (bounds != null) {
            double[] result = new double[bounds.length * 4];
            int index = 0;
            for (int i = 0; i < bounds.length; i++) {
                Bounds b = bounds[i];
                result[index++] = b.getMinX();
                result[index++] = b.getMinY();
                result[index++] = b.getWidth();
                result[index++] = b.getHeight();
            }
            return result;
        }
        return null;
    }

    private long GetEnclosingElement() {
        return accessible.getNativeAccessible();
    }

    private String GetText(int maxLength) {
        String text = (String)getAttribute(TEXT);
        if (text == null) return null;
        int endOffset = maxLength != -1 ? Math.min(end, start + maxLength) : end;
//        System.out.println("+GetText [" + text.substring(start, endOffset)+"]");
        return text.substring(start, endOffset);
    }

    private int Move(int unit, final int requestedCount) {
        if (requestedCount == 0) return 0;
        String text = (String)getAttribute(TEXT);
        if (text == null) return 0;
        int length = text.length();
        if (length == 0) return 0;

        int actualCount = 0;
        switch (unit) {
            case TextUnit_Character: {
                int oldStart = start;
                start = Math.max(0, Math.min(start + requestedCount, length - 1));
                end = start + 1;
                actualCount = start - oldStart;
                break;
            }
            case TextUnit_Format:
            case TextUnit_Word: {
                BreakIterator bi = BreakIterator.getWordInstance();
                bi.setText(text);
                int offset = start;
                while (!isWordStart(bi, text, offset)) {
                    offset = bi.preceding(start);
                }
                while (offset != BreakIterator.DONE && actualCount != requestedCount) {
                    if (requestedCount > 0) {
                        offset = bi.following(offset);
                        while (!isWordStart(bi, text, offset)) {
                            offset = bi.next();
                        }
                        actualCount++;
                    } else {
                        offset = bi.preceding(offset);
                        while (!isWordStart(bi, text, offset)) {
                            offset = bi.previous();
                        }
                        actualCount--;
                    }
                }
                if (actualCount != 0) {
                    if (offset != BreakIterator.DONE) {
                        start = offset;
                    } else {
                        start = requestedCount > 0 ? length : 0;
                    }
                    offset = bi.following(start);
                    while (!isWordStart(bi, text, offset)) {
                        offset = bi.next();
                    }
                    end = offset != BreakIterator.DONE ? offset : length;
                }
                break;
            }
            case TextUnit_Line: {
                Integer lineIndex = (Integer)getAttribute(LINE_FOR_OFFSET, start);
                if (lineIndex == null) return 0;
                int step = requestedCount > 0 ? + 1 : -1;
                while (requestedCount != actualCount) {
                    if (getAttribute(LINE_START, lineIndex + step) == null) break;
                    lineIndex += step;
                    actualCount += step;
                }
                if (actualCount != 0) {
                    Integer lineStart = (Integer)getAttribute(LINE_START, lineIndex);
                    Integer lineEnd = (Integer)getAttribute(LINE_END, lineIndex);
                    if (lineStart == null || lineEnd == null) return 0;
                    start = lineStart;
                    end = lineEnd;
                }
                break;
            }
            case TextUnit_Paragraph: {
                BreakIterator bi = BreakIterator.getSentenceInstance();
                bi.setText(text);
                int offset = bi.isBoundary(start) ? start : bi.preceding(start);
                while (offset != BreakIterator.DONE && actualCount != requestedCount) {
                    if (requestedCount > 0) {
                        offset = bi.following(offset);
                        actualCount++;
                    } else {
                        offset = bi.preceding(offset);
                        actualCount--;
                    }
                }
                if (actualCount != 0) {
                    start = offset != BreakIterator.DONE ? offset : 0;
                    offset = bi.following(start);
                    end = offset != BreakIterator.DONE ? offset : length;
                }
                break;
            }
            case TextUnit_Page:
            case TextUnit_Document: {
                /* Move not allowed/implemented - do not alter the range */
                return 0;
            }
        }

        /* Always ensure range consistency */
        start = Math.max(0, Math.min(start, length));
        end = Math.max(start, Math.min(end, length));
        return actualCount;
    }

    private int MoveEndpointByUnit(int endpoint, int unit, final int requestedCount) {
        if (requestedCount == 0) return 0;
        String text = (String)getAttribute(TEXT);
        if (text == null) return 0;
        int length = text.length();

        int actualCount = 0;
        int offset = endpoint == TextPatternRangeEndpoint_Start ? start : end;
        switch (unit) {
            case TextUnit_Character: {
                int oldOffset = offset;
                offset = Math.max(0, Math.min(offset + requestedCount, length));
                actualCount = offset - oldOffset;
                break;
            }
            case TextUnit_Format:
            case TextUnit_Word: {
                BreakIterator bi = BreakIterator.getWordInstance();
                bi.setText(text);
                while (offset != BreakIterator.DONE && actualCount != requestedCount) {
                    if (requestedCount > 0) {
                        offset = bi.following(offset);
                        while (!isWordStart(bi, text, offset)) {
                            offset = bi.next();
                        }
                        actualCount++;
                    } else {
                        offset = bi.preceding(offset);
                        while (!isWordStart(bi, text, offset)) {
                            offset = bi.previous();
                        }
                        actualCount--;
                    }
                }
                if (offset == BreakIterator.DONE) {
                    offset = requestedCount > 0 ? length : 0;
                }
                break;
            }
            case TextUnit_Line: {
                Integer lineIndex = (Integer)getAttribute(LINE_FOR_OFFSET, offset);
                Integer lineStart = (Integer)getAttribute(LINE_START, lineIndex);
                Integer lineEnd = (Integer)getAttribute(LINE_END, lineIndex);
                if (lineIndex == null || lineStart == null || lineEnd == null) {
                    /* Text field - move within the text */
                    offset = requestedCount > 0 ? length : 0;
                    break;
                }
                int step = requestedCount > 0 ? + 1 : -1;
                int endOffset = requestedCount > 0 ? lineEnd : lineStart;
                if (offset != endOffset) {
                    /* Count current line when not traversal start in middle of the line */
                    actualCount += step;
                }
                while (requestedCount != actualCount) {
                    if (getAttribute(LINE_START, lineIndex + step) == null) break;
                    lineIndex += step;
                    actualCount += step;
                }
                if (actualCount != 0) {
                    lineStart = (Integer)getAttribute(LINE_START, lineIndex);
                    lineEnd = (Integer)getAttribute(LINE_END, lineIndex);
                    if (lineStart == null || lineEnd == null) return 0;
                    offset = requestedCount > 0 ? lineEnd : lineStart;
                }
                break;
            }
            case TextUnit_Paragraph: {
                BreakIterator bi = BreakIterator.getSentenceInstance();
                bi.setText(text);
                while (offset != BreakIterator.DONE && actualCount != requestedCount) {
                    if (requestedCount > 0) {
                        offset = bi.following(offset);
                        actualCount++;
                    } else {
                        offset = bi.preceding(offset);
                        actualCount--;
                    }
                }
                if (offset == BreakIterator.DONE) {
                    offset = requestedCount > 0 ? length : 0;
                }
                break;
            }
            case TextUnit_Page:
            case TextUnit_Document: {
                /* Move not allowed/implemented - do not alter the range */
                return 0;
            }
        }
        if (endpoint == TextPatternRangeEndpoint_Start) {
            start = offset;
        } else {
            end = offset;
        }
        if (start > end) {
            start = end = offset;
        }

        /* Always ensure range consistency */
        start = Math.max(0, Math.min(start, length));
        end = Math.max(start, Math.min(end, length));
        return actualCount;
    }

    private void MoveEndpointByRange(int endpoint, WinTextRangeProvider targetRange, int targetEndpoint) {
        String text = (String)getAttribute(TEXT);
        if (text == null) return;
        int length = text.length();

        int offset = targetEndpoint == TextPatternRangeEndpoint_Start ? targetRange.start : targetRange.end;
        if (endpoint == TextPatternRangeEndpoint_Start) {
            start = offset;
        } else {
            end = offset;
        }
        if (start > end) {
            start = end = offset;
        }

        /* Always ensure range consistency */
        start = Math.max(0, Math.min(start, length));
        end = Math.max(start, Math.min(end, length));
    }

    private void Select() {
        accessible.executeAction(AccessibleAction.SET_TEXT_SELECTION, start, end);
    }

    private void AddToSelection() {
        /* Only possible for multi selection text view */
//        accessible.executeAction(Action.ADD_TO_SELECTION, start, end);
    }

    private void RemoveFromSelection() {
        /* Only possible for multi selection text view */
//        accessible.executeAction(Action.REMOVE_FROM_SELECTION, start, end);
    }

    private void ScrollIntoView(boolean alignToTop) {
        accessible.executeAction(AccessibleAction.SHOW_TEXT_RANGE, start, end);
    }

    private long[] GetChildren() {
        /* Not embedded object support currently */
        return new long[0];
    }

}
