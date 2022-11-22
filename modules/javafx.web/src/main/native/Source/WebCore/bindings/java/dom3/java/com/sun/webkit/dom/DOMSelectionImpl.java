/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

    native private static void dispose(long peer);

    static DOMSelectionImpl getImpl(long peer) {
        return (DOMSelectionImpl)create(peer);
    }


// Attributes
    public Node getAnchorNode() {
        return NodeImpl.getImpl(getAnchorNodeImpl(getPeer()));
    }
    native static long getAnchorNodeImpl(long peer);

    public int getAnchorOffset() {
        return getAnchorOffsetImpl(getPeer());
    }
    native static int getAnchorOffsetImpl(long peer);

    public Node getFocusNode() {
        return NodeImpl.getImpl(getFocusNodeImpl(getPeer()));
    }
    native static long getFocusNodeImpl(long peer);

    public int getFocusOffset() {
        return getFocusOffsetImpl(getPeer());
    }
    native static int getFocusOffsetImpl(long peer);

    public boolean getIsCollapsed() {
        return getIsCollapsedImpl(getPeer());
    }
    native static boolean getIsCollapsedImpl(long peer);

    public int getRangeCount() {
        return getRangeCountImpl(getPeer());
    }
    native static int getRangeCountImpl(long peer);

    public Node getBaseNode() {
        return NodeImpl.getImpl(getBaseNodeImpl(getPeer()));
    }
    native static long getBaseNodeImpl(long peer);

    public int getBaseOffset() {
        return getBaseOffsetImpl(getPeer());
    }
    native static int getBaseOffsetImpl(long peer);

    public Node getExtentNode() {
        return NodeImpl.getImpl(getExtentNodeImpl(getPeer()));
    }
    native static long getExtentNodeImpl(long peer);

    public int getExtentOffset() {
        return getExtentOffsetImpl(getPeer());
    }
    native static int getExtentOffsetImpl(long peer);

    public String getType() {
        return getTypeImpl(getPeer());
    }
    native static String getTypeImpl(long peer);


// Functions
    public void collapse(Node node
        , int index) throws DOMException
    {
        collapseImpl(getPeer()
            , NodeImpl.getPeer(node)
            , index);
    }
    native static void collapseImpl(long peer
        , long node
        , int index);


    public void collapseToEnd() throws DOMException
    {
        collapseToEndImpl(getPeer());
    }
    native static void collapseToEndImpl(long peer);


    public void collapseToStart() throws DOMException
    {
        collapseToStartImpl(getPeer());
    }
    native static void collapseToStartImpl(long peer);


    public void deleteFromDocument()
    {
        deleteFromDocumentImpl(getPeer());
    }
    native static void deleteFromDocumentImpl(long peer);


    public boolean containsNode(Node node
        , boolean allowPartial)
    {
        return containsNodeImpl(getPeer()
            , NodeImpl.getPeer(node)
            , allowPartial);
    }
    native static boolean containsNodeImpl(long peer
        , long node
        , boolean allowPartial);


    public void selectAllChildren(Node node) throws DOMException
    {
        selectAllChildrenImpl(getPeer()
            , NodeImpl.getPeer(node));
    }
    native static void selectAllChildrenImpl(long peer
        , long node);


    public void extend(Node node
        , int offset) throws DOMException
    {
        extendImpl(getPeer()
            , NodeImpl.getPeer(node)
            , offset);
    }
    native static void extendImpl(long peer
        , long node
        , int offset);


    public Range getRangeAt(int index) throws DOMException
    {
        return RangeImpl.getImpl(getRangeAtImpl(getPeer()
            , index));
    }
    native static long getRangeAtImpl(long peer
        , int index);


    public void removeAllRanges()
    {
        removeAllRangesImpl(getPeer());
    }
    native static void removeAllRangesImpl(long peer);


    public void addRange(Range range)
    {
        addRangeImpl(getPeer()
            , RangeImpl.getPeer(range));
    }
    native static void addRangeImpl(long peer
        , long range);


    public void modify(String alter
        , String direction
        , String granularity)
    {
        modifyImpl(getPeer()
            , alter
            , direction
            , granularity);
    }
    native static void modifyImpl(long peer
        , String alter
        , String direction
        , String granularity);


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
    native static void setBaseAndExtentImpl(long peer
        , long baseNode
        , int baseOffset
        , long extentNode
        , int extentOffset);


    public void setPosition(Node node
        , int offset) throws DOMException
    {
        setPositionImpl(getPeer()
            , NodeImpl.getPeer(node)
            , offset);
    }
    native static void setPositionImpl(long peer
        , long node
        , int offset);


    public void empty()
    {
        emptyImpl(getPeer());
    }
    native static void emptyImpl(long peer);


}

