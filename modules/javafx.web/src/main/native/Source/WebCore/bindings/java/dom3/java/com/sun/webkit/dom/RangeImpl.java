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
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.ranges.Range;

public class RangeImpl implements Range {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }

        @Override
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

    private static void dispose(long peer) {
        RangeNative.dispose(peer);
    }

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
    @Override
    public Node getStartContainer() {
        return NodeImpl.getImpl(getStartContainerImpl(getPeer()));
    }
    static long getStartContainerImpl(long peer) {
        return RangeNative.getStartContainer(peer);
    }

    @Override
    public int getStartOffset() {
        return getStartOffsetImpl(getPeer());
    }
    static int getStartOffsetImpl(long peer) {
        return RangeNative.getStartOffset(peer);
    }

    @Override
    public Node getEndContainer() {
        return NodeImpl.getImpl(getEndContainerImpl(getPeer()));
    }
    static long getEndContainerImpl(long peer) {
        return RangeNative.getEndContainer(peer);
    }

    @Override
    public int getEndOffset() {
        return getEndOffsetImpl(getPeer());
    }
    static int getEndOffsetImpl(long peer) {
        return RangeNative.getEndOffset(peer);
    }

    @Override
    public boolean getCollapsed() {
        return getCollapsedImpl(getPeer());
    }
    static boolean getCollapsedImpl(long peer) {
        return RangeNative.getCollapsed(peer);
    }

    @Override
    public Node getCommonAncestorContainer() {
        return NodeImpl.getImpl(getCommonAncestorContainerImpl(getPeer()));
    }
    static long getCommonAncestorContainerImpl(long peer) {
        return RangeNative.getCommonAncestorContainer(peer);
    }

    public String getText() {
        return getTextImpl(getPeer());
    }
    static String getTextImpl(long peer) {
        return RangeNative.getText(peer);
    }


// Functions
    @Override
    public void setStart(Node refNode
        , int offset) throws DOMException
    {
        setStartImpl(getPeer()
            , NodeImpl.getPeer(refNode)
            , offset);
    }
    static void setStartImpl(long peer
        , long refNode
        , int offset) {
        RangeNative.setStart(peer, refNode, offset);
    }


    @Override
    public void setEnd(Node refNode
        , int offset) throws DOMException
    {
        setEndImpl(getPeer()
            , NodeImpl.getPeer(refNode)
            , offset);
    }
    static void setEndImpl(long peer
        , long refNode
        , int offset) {
        RangeNative.setEnd(peer, refNode, offset);
    }


    @Override
    public void setStartBefore(Node refNode) throws DOMException
    {
        setStartBeforeImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    static void setStartBeforeImpl(long peer
        , long refNode) {
        RangeNative.setStartBefore(peer, refNode);
    }


    @Override
    public void setStartAfter(Node refNode) throws DOMException
    {
        setStartAfterImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    static void setStartAfterImpl(long peer
        , long refNode) {
        RangeNative.setStartAfter(peer, refNode);
    }


    @Override
    public void setEndBefore(Node refNode) throws DOMException
    {
        setEndBeforeImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    static void setEndBeforeImpl(long peer
        , long refNode) {
        RangeNative.setEndBefore(peer, refNode);
    }


    @Override
    public void setEndAfter(Node refNode) throws DOMException
    {
        setEndAfterImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    static void setEndAfterImpl(long peer
        , long refNode) {
        RangeNative.setEndAfter(peer, refNode);
    }


    @Override
    public void collapse(boolean toStart)
    {
        collapseImpl(getPeer()
            , toStart);
    }
    static void collapseImpl(long peer
        , boolean toStart) {
        RangeNative.collapse(peer, toStart);
    }


    @Override
    public void selectNode(Node refNode) throws DOMException
    {
        selectNodeImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    static void selectNodeImpl(long peer
        , long refNode) {
        RangeNative.selectNode(peer, refNode);
    }


    @Override
    public void selectNodeContents(Node refNode) throws DOMException
    {
        selectNodeContentsImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    static void selectNodeContentsImpl(long peer
        , long refNode) {
        RangeNative.selectNodeContents(peer, refNode);
    }


    @Override
    public short compareBoundaryPoints(short how
        , Range sourceRange) throws DOMException
    {
        return compareBoundaryPointsImpl(getPeer()
            , how
            , RangeImpl.getPeer(sourceRange));
    }
    static short compareBoundaryPointsImpl(long peer
        , short how
        , long sourceRange) {
        return RangeNative.compareBoundaryPoints(peer, how, sourceRange);
    }


    @Override
    public void deleteContents() throws DOMException
    {
        deleteContentsImpl(getPeer());
    }
    static void deleteContentsImpl(long peer) {
        RangeNative.deleteContents(peer);
    }


    @Override
    public DocumentFragment extractContents() throws DOMException
    {
        return DocumentFragmentImpl.getImpl(extractContentsImpl(getPeer()));
    }
    static long extractContentsImpl(long peer) {
        return RangeNative.extractContents(peer);
    }


    @Override
    public DocumentFragment cloneContents() throws DOMException
    {
        return DocumentFragmentImpl.getImpl(cloneContentsImpl(getPeer()));
    }
    static long cloneContentsImpl(long peer) {
        return RangeNative.cloneContents(peer);
    }


    @Override
    public void insertNode(Node newNode) throws DOMException
    {
        insertNodeImpl(getPeer()
            , NodeImpl.getPeer(newNode));
    }
    static void insertNodeImpl(long peer
        , long newNode) {
        RangeNative.insertNode(peer, newNode);
    }


    @Override
    public void surroundContents(Node newParent) throws DOMException
    {
        surroundContentsImpl(getPeer()
            , NodeImpl.getPeer(newParent));
    }
    static void surroundContentsImpl(long peer
        , long newParent) {
        RangeNative.surroundContents(peer, newParent);
    }


    @Override
    public Range cloneRange()
    {
        return RangeImpl.getImpl(cloneRangeImpl(getPeer()));
    }
    static long cloneRangeImpl(long peer) {
        return RangeNative.cloneRange(peer);
    }


    @Override
    public String toString()
    {
        return toStringImpl(getPeer());
    }
    static String toStringImpl(long peer) {
        return RangeNative.toString(peer);
    }


    @Override
    public void detach()
    {
        detachImpl(getPeer());
    }
    static void detachImpl(long peer) {
        RangeNative.detach(peer);
    }


    public DocumentFragment createContextualFragment(String html) throws DOMException
    {
        return DocumentFragmentImpl.getImpl(createContextualFragmentImpl(getPeer()
            , html));
    }
    static long createContextualFragmentImpl(long peer
        , String html) {
        return RangeNative.createContextualFragment(peer, html);
    }


    public short compareNode(Node refNode) throws DOMException
    {
        return compareNodeImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    static short compareNodeImpl(long peer
        , long refNode) {
        return RangeNative.compareNode(peer, refNode);
    }


    public short comparePoint(Node refNode
        , int offset) throws DOMException
    {
        return comparePointImpl(getPeer()
            , NodeImpl.getPeer(refNode)
            , offset);
    }
    static short comparePointImpl(long peer
        , long refNode
        , int offset) {
        return RangeNative.comparePoint(peer, refNode, offset);
    }


    public boolean intersectsNode(Node refNode) throws DOMException
    {
        return intersectsNodeImpl(getPeer()
            , NodeImpl.getPeer(refNode));
    }
    static boolean intersectsNodeImpl(long peer
        , long refNode) {
        return RangeNative.intersectsNode(peer, refNode);
    }


    public boolean isPointInRange(Node refNode
        , int offset) throws DOMException
    {
        return isPointInRangeImpl(getPeer()
            , NodeImpl.getPeer(refNode)
            , offset);
    }
    static boolean isPointInRangeImpl(long peer
        , long refNode
        , int offset) {
        return RangeNative.isPointInRange(peer, refNode, offset);
    }


    public void expand(String unit) throws DOMException
    {
        expandImpl(getPeer()
            , unit);
    }
    static void expandImpl(long peer
        , String unit) {
        RangeNative.expand(peer, unit);
    }


}

