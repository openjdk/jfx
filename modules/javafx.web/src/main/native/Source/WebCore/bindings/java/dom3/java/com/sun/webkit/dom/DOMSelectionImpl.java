/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.dom;

import com.sun.webkit.Disposer;
import com.sun.webkit.DisposerRecord;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.ranges.Range;

public class DOMSelectionImpl {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }

        @Override
        public void dispose() {
            DOMSelectionImpl.dispose(peer);
        }
    }

    DOMSelectionImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static DOMSelectionImpl create(long peer) {
        if (peer == 0L) return null;
        return new DOMSelectionImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof DOMSelectionImpl) && (peer == ((DOMSelectionImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(DOMSelectionImpl arg) {
        return (arg == null) ? 0L : ((DOMSelectionImpl)arg).getPeer();
    }

    private static void dispose(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.dispose: JavaDOMSelection.cpp is not"
                + " compiled into jfxwebkit");
    }

    static DOMSelectionImpl getImpl(long peer) {
        return (DOMSelectionImpl)create(peer);
    }


// Attributes
    public Node getAnchorNode() {
        return NodeImpl.getImpl(getAnchorNodeImpl(getPeer()));
    }
    static long getAnchorNodeImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getAnchorNodeImpl: JavaDOMSelection.cpp is"
                + " not compiled into jfxwebkit");
    }

    public int getAnchorOffset() {
        return getAnchorOffsetImpl(getPeer());
    }
    static int getAnchorOffsetImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getAnchorOffsetImpl: JavaDOMSelection.cpp"
                + " is not compiled into jfxwebkit");
    }

    public Node getFocusNode() {
        return NodeImpl.getImpl(getFocusNodeImpl(getPeer()));
    }
    static long getFocusNodeImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getFocusNodeImpl: JavaDOMSelection.cpp is"
                + " not compiled into jfxwebkit");
    }

    public int getFocusOffset() {
        return getFocusOffsetImpl(getPeer());
    }
    static int getFocusOffsetImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getFocusOffsetImpl: JavaDOMSelection.cpp is"
                + " not compiled into jfxwebkit");
    }

    public boolean getIsCollapsed() {
        return getIsCollapsedImpl(getPeer());
    }
    static boolean getIsCollapsedImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getIsCollapsedImpl: JavaDOMSelection.cpp is"
                + " not compiled into jfxwebkit");
    }

    public int getRangeCount() {
        return getRangeCountImpl(getPeer());
    }
    static int getRangeCountImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getRangeCountImpl: JavaDOMSelection.cpp is"
                + " not compiled into jfxwebkit");
    }

    public Node getBaseNode() {
        return NodeImpl.getImpl(getBaseNodeImpl(getPeer()));
    }
    static long getBaseNodeImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getBaseNodeImpl: JavaDOMSelection.cpp is"
                + " not compiled into jfxwebkit");
    }

    public int getBaseOffset() {
        return getBaseOffsetImpl(getPeer());
    }
    static int getBaseOffsetImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getBaseOffsetImpl: JavaDOMSelection.cpp is"
                + " not compiled into jfxwebkit");
    }

    public Node getExtentNode() {
        return NodeImpl.getImpl(getExtentNodeImpl(getPeer()));
    }
    static long getExtentNodeImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getExtentNodeImpl: JavaDOMSelection.cpp is"
                + " not compiled into jfxwebkit");
    }

    public int getExtentOffset() {
        return getExtentOffsetImpl(getPeer());
    }
    static int getExtentOffsetImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getExtentOffsetImpl: JavaDOMSelection.cpp"
                + " is not compiled into jfxwebkit");
    }

    public String getType() {
        return getTypeImpl(getPeer());
    }
    static String getTypeImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getTypeImpl: JavaDOMSelection.cpp is not"
                + " compiled into jfxwebkit");
    }


// Functions
    public void collapse(Node node
        , int index) throws DOMException
    {
        collapseImpl(getPeer()
            , NodeImpl.getPeer(node)
            , index);
    }
    static void collapseImpl(long peer
        , long node
        , int index) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.collapseImpl: JavaDOMSelection.cpp is not"
                + " compiled into jfxwebkit");
    }


    public void collapseToEnd() throws DOMException
    {
        collapseToEndImpl(getPeer());
    }
    static void collapseToEndImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.collapseToEndImpl: JavaDOMSelection.cpp is"
                + " not compiled into jfxwebkit");
    }


    public void collapseToStart() throws DOMException
    {
        collapseToStartImpl(getPeer());
    }
    static void collapseToStartImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.collapseToStartImpl: JavaDOMSelection.cpp"
                + " is not compiled into jfxwebkit");
    }


    public void deleteFromDocument()
    {
        deleteFromDocumentImpl(getPeer());
    }
    static void deleteFromDocumentImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.deleteFromDocumentImpl:"
                + " JavaDOMSelection.cpp is not compiled into jfxwebkit");
    }


    public boolean containsNode(Node node
        , boolean allowPartial)
    {
        return containsNodeImpl(getPeer()
            , NodeImpl.getPeer(node)
            , allowPartial);
    }
    static boolean containsNodeImpl(long peer
        , long node
        , boolean allowPartial) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.containsNodeImpl: JavaDOMSelection.cpp is"
                + " not compiled into jfxwebkit");
    }


    public void selectAllChildren(Node node) throws DOMException
    {
        selectAllChildrenImpl(getPeer()
            , NodeImpl.getPeer(node));
    }
    static void selectAllChildrenImpl(long peer
        , long node) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.selectAllChildrenImpl: JavaDOMSelection.cpp"
                + " is not compiled into jfxwebkit");
    }


    public void extend(Node node
        , int offset) throws DOMException
    {
        extendImpl(getPeer()
            , NodeImpl.getPeer(node)
            , offset);
    }
    static void extendImpl(long peer
        , long node
        , int offset) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.extendImpl: JavaDOMSelection.cpp is not"
                + " compiled into jfxwebkit");
    }


    public Range getRangeAt(int index) throws DOMException
    {
        return RangeImpl.getImpl(getRangeAtImpl(getPeer()
            , index));
    }
    static long getRangeAtImpl(long peer
        , int index) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.getRangeAtImpl: JavaDOMSelection.cpp is not"
                + " compiled into jfxwebkit");
    }


    public void removeAllRanges()
    {
        removeAllRangesImpl(getPeer());
    }
    static void removeAllRangesImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.removeAllRangesImpl: JavaDOMSelection.cpp"
                + " is not compiled into jfxwebkit");
    }


    public void addRange(Range range)
    {
        addRangeImpl(getPeer()
            , RangeImpl.getPeer(range));
    }
    static void addRangeImpl(long peer
        , long range) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.addRangeImpl: JavaDOMSelection.cpp is not"
                + " compiled into jfxwebkit");
    }


    public void modify(String alter
        , String direction
        , String granularity)
    {
        modifyImpl(getPeer()
            , alter
            , direction
            , granularity);
    }
    static void modifyImpl(long peer
        , String alter
        , String direction
        , String granularity) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.modifyImpl: JavaDOMSelection.cpp is not"
                + " compiled into jfxwebkit");
    }


    public void setBaseAndExtent(Node baseNode
        , int baseOffset
        , Node extentNode
        , int extentOffset) throws DOMException
    {
        setBaseAndExtentImpl(getPeer()
            , NodeImpl.getPeer(baseNode)
            , baseOffset
            , NodeImpl.getPeer(extentNode)
            , extentOffset);
    }
    static void setBaseAndExtentImpl(long peer
        , long baseNode
        , int baseOffset
        , long extentNode
        , int extentOffset) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.setBaseAndExtentImpl: JavaDOMSelection.cpp"
                + " is not compiled into jfxwebkit");
    }


    public void setPosition(Node node
        , int offset) throws DOMException
    {
        setPositionImpl(getPeer()
            , NodeImpl.getPeer(node)
            , offset);
    }
    static void setPositionImpl(long peer
        , long node
        , int offset) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.setPositionImpl: JavaDOMSelection.cpp is"
                + " not compiled into jfxwebkit");
    }


    public void empty()
    {
        emptyImpl(getPeer());
    }
    static void emptyImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.DOMSelectionImpl.emptyImpl: JavaDOMSelection.cpp is not"
                + " compiled into jfxwebkit");
    }


}

