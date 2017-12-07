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
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.ranges.Range;

public class RangeImpl implements Range {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
        public void dispose() {
            RangeImpl.dispose(peer);
        }
    }

    RangeImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static Range create(long peer) {
        if (peer == 0L) return null;
        return new RangeImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof RangeImpl) && (peer == ((RangeImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(Range arg) {
        return (arg == null) ? 0L : ((RangeImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    static Range getImpl(long peer) {
        return (Range)create(peer);
    }


// Constants
    public static final int START_TO_START = 0;
    public static final int START_TO_END = 1;
    public static final int END_TO_END = 2;
    public static final int END_TO_START = 3;
    public static final int NODE_BEFORE = 0;
    public static final int NODE_AFTER = 1;
    public static final int NODE_BEFORE_AND_AFTER = 2;
    public static final int NODE_INSIDE = 3;

// Attributes
    public Node getStartContainer() {
        return NodeImpl.getImpl(getStartContainerImpl(getPeer()));
    }
    native static long getStartContainerImpl(long peer);

    public int getStartOffset() {
        return getStartOffsetImpl(getPeer());
    }
    native static int getStartOffsetImpl(long peer);

    public Node getEndContainer() {
        return NodeImpl.getImpl(getEndContainerImpl(getPeer()));
    }
    native static long getEndContainerImpl(long peer);

    public int getEndOffset() {
        return getEndOffsetImpl(getPeer());
    }
    native static int getEndOffsetImpl(long peer);

    public boolean getCollapsed() {
        return getCollapsedImpl(getPeer());
    }
    native static boolean getCollapsedImpl(long peer);

    public Node getCommonAncestorContainer() {
        return NodeImpl.getImpl(getCommonAncestorContainerImpl(getPeer()));
    }
    native static long getCommonAncestorContainerImpl(long peer);

    public String getText() {
        return getTextImpl(getPeer());
    }
    native static String getTextImpl(long peer);


// Functions
    public void setStart(Node refNode
        , int offset) throws DOMException
    {
        setStartImpl(getPeer()
            , NodeImpl.getPeer(refNode)
            , offset);
    }
    native static void setStartImpl(long peer
        , long refNode
        , int offset);


    public void setEnd(Node refNode
        , int offset) throws DOMException
    {
        setEndImpl(getPeer()
            , NodeImpl.getPeer(refNode)
            , offset);
    }
    native static void setEndImpl(long peer
        , long refNode
        , int offset);


    public void setStartBefore(Node refNode) throws DOMException
    {
        setStartBeforeImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    native static void setStartBeforeImpl(long peer
        , long refNode);


    public void setStartAfter(Node refNode) throws DOMException
    {
        setStartAfterImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    native static void setStartAfterImpl(long peer
        , long refNode);


    public void setEndBefore(Node refNode) throws DOMException
    {
        setEndBeforeImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    native static void setEndBeforeImpl(long peer
        , long refNode);


    public void setEndAfter(Node refNode) throws DOMException
    {
        setEndAfterImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    native static void setEndAfterImpl(long peer
        , long refNode);


    public void collapse(boolean toStart)
    {
        collapseImpl(getPeer()
            , toStart);
    }
    native static void collapseImpl(long peer
        , boolean toStart);


    public void selectNode(Node refNode) throws DOMException
    {
        selectNodeImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    native static void selectNodeImpl(long peer
        , long refNode);


    public void selectNodeContents(Node refNode) throws DOMException
    {
        selectNodeContentsImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    native static void selectNodeContentsImpl(long peer
        , long refNode);


    public short compareBoundaryPoints(short how
        , Range sourceRange) throws DOMException
    {
        return compareBoundaryPointsImpl(getPeer()
            , how
            , RangeImpl.getPeer(sourceRange));
    }
    native static short compareBoundaryPointsImpl(long peer
        , short how
        , long sourceRange);


    public void deleteContents() throws DOMException
    {
        deleteContentsImpl(getPeer());
    }
    native static void deleteContentsImpl(long peer);


    public DocumentFragment extractContents() throws DOMException
    {
        return DocumentFragmentImpl.getImpl(extractContentsImpl(getPeer()));
    }
    native static long extractContentsImpl(long peer);


    public DocumentFragment cloneContents() throws DOMException
    {
        return DocumentFragmentImpl.getImpl(cloneContentsImpl(getPeer()));
    }
    native static long cloneContentsImpl(long peer);


    public void insertNode(Node newNode) throws DOMException
    {
        insertNodeImpl(getPeer()
            , NodeImpl.getPeer(newNode));
    }
    native static void insertNodeImpl(long peer
        , long newNode);


    public void surroundContents(Node newParent) throws DOMException
    {
        surroundContentsImpl(getPeer()
            , NodeImpl.getPeer(newParent));
    }
    native static void surroundContentsImpl(long peer
        , long newParent);


    public Range cloneRange()
    {
        return RangeImpl.getImpl(cloneRangeImpl(getPeer()));
    }
    native static long cloneRangeImpl(long peer);


    public String toString()
    {
        return toStringImpl(getPeer());
    }
    native static String toStringImpl(long peer);


    public void detach()
    {
        detachImpl(getPeer());
    }
    native static void detachImpl(long peer);


    public DocumentFragment createContextualFragment(String html) throws DOMException
    {
        return DocumentFragmentImpl.getImpl(createContextualFragmentImpl(getPeer()
            , html));
    }
    native static long createContextualFragmentImpl(long peer
        , String html);


    public short compareNode(Node refNode) throws DOMException
    {
        return compareNodeImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    native static short compareNodeImpl(long peer
        , long refNode);


    public short comparePoint(Node refNode
        , int offset) throws DOMException
    {
        return comparePointImpl(getPeer()
            , NodeImpl.getPeer(refNode)
            , offset);
    }
    native static short comparePointImpl(long peer
        , long refNode
        , int offset);


    public boolean intersectsNode(Node refNode) throws DOMException
    {
        return intersectsNodeImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    native static boolean intersectsNodeImpl(long peer
        , long refNode);


    public boolean isPointInRange(Node refNode
        , int offset) throws DOMException
    {
        return isPointInRangeImpl(getPeer()
            , NodeImpl.getPeer(refNode)
            , offset);
    }
    native static boolean isPointInRangeImpl(long peer
        , long refNode
        , int offset);


    public void expand(String unit) throws DOMException
    {
        expandImpl(getPeer()
            , unit);
    }
    native static void expandImpl(long peer
        , String unit);


}

