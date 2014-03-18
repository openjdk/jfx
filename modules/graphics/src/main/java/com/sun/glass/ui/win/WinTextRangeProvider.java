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

    @Override public String toString() {
        return "Range(start: "+start+", end: "+end+", id: " + id + ")";
    }

    /***********************************************/
    /*            ITextRangeProvider               */
    /***********************************************/
    long Clone() {
        WinTextRangeProvider clone = new WinTextRangeProvider(accessible);
        clone.setRange(start, end);
        System.out.println("Cloning " + id + " to " + clone.id);

        /* Note: Currently Clone() natively does not call AddRef() no the returned object.
         * This mean we don't keep a reference of our own and we don't 
         * need to free it.
         * TODO make sure this works...
         */
        return clone.getNativeProvider();
    }

    boolean Compare(WinTextRangeProvider range) {
        System.out.println("Compare " + this + " to " + range);
        if (range == null) return false;
        return accessible == range.accessible && start == range.start && end == range.end;
    }

    int CompareEndpoints(int endpoint, WinTextRangeProvider targetRange, int targetEndpoint) {
        System.out.println("CompareEndpoints " + endpoint + " " + this + " " + targetRange + " " + targetEndpoint);
        int offset = endpoint == TextPatternRangeEndpoint_Start ? start : end;
        int targetOffset = targetEndpoint == TextPatternRangeEndpoint_Start ? targetRange.start : targetRange.end;
        return targetOffset - offset;
    }

    void ExpandToEnclosingUnit(int unit) {
        System.out.println("ExpandToEnclosingUnit " + this + " unit " + unit);
    }

    long FindAttribute(int attributeId, WinVariant val, boolean backward) {
        System.out.println("FindAttribute");
        return 0;
    }

    long FindText(String text, boolean backward, boolean ignoreCase) {
        System.out.println("FindText");
        return 0;
    }

    WinVariant GetAttributeValue(int attributeId) {
        System.out.println("GetAttributeValue");
        return null;
    }

    double[] GetBoundingRectangles() {
        System.out.println("GetBoundingRectangles");
        return null;
    }

    long GetEnclosingElement() {
        System.out.println("GetEnclosingElement");
        return 0;
    }

    String GetText(int maxLength) {
        System.out.println("GetText");
        return null;
    }

    int Move(int unit, int count) {
        System.out.println("Move");
        return 0;
    }

    int MoveEndpointByUnit(int endpoint, int unit, int count) {
        System.out.println("MoveEndpointByUnit");
        return 0;
    }

    void MoveEndpointByRange(int endpoint, WinTextRangeProvider targetRange, int targetEndpoint) {
        System.out.println("MoveEndpointByRange");
    }

    void Select() {
        System.out.println("Select");
    }

    void AddToSelection() {
        System.out.println("AddToSelection");
    }

    void RemoveFromSelection() {
        System.out.println("RemoveFromSelection");
    }

    void ScrollIntoView(boolean alignToTop) {
        System.out.println("ScrollIntoView");
    }

    long[] GetChildren() {
        System.out.println("GetChildren");
        return null;
    }

}
