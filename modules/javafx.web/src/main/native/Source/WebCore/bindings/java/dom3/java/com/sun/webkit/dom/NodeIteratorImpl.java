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
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

public class NodeIteratorImpl implements NodeIterator {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }

        @Override
        public void dispose() {
            NodeIteratorImpl.dispose(peer);
        }
    }

    NodeIteratorImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static NodeIterator create(long peer) {
        if (peer == 0L) return null;
        return new NodeIteratorImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof NodeIteratorImpl) && (peer == ((NodeIteratorImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(NodeIterator arg) {
        return (arg == null) ? 0L : ((NodeIteratorImpl)arg).getPeer();
    }

    private static void dispose(long peer) {
        NodeIteratorNative.dispose(peer);
    }

    static NodeIterator getImpl(long peer) {
        return (NodeIterator)create(peer);
    }


// Attributes
    @Override
    public Node getRoot() {
        return NodeImpl.getImpl(getRootImpl(getPeer()));
    }
    static long getRootImpl(long peer) {
        return NodeIteratorNative.getRoot(peer);
    }

    @Override
    public int getWhatToShow() {
        return getWhatToShowImpl(getPeer());
    }
    static int getWhatToShowImpl(long peer) {
        return NodeIteratorNative.getWhatToShow(peer);
    }

    @Override
    public NodeFilter getFilter() {
        return NodeFilterImpl.getImpl(getFilterImpl(getPeer()));
    }
    static long getFilterImpl(long peer) {
        return NodeIteratorNative.getFilter(peer);
    }

    @Override
    public boolean getExpandEntityReferences() {
        return getExpandEntityReferencesImpl(getPeer());
    }
    static boolean getExpandEntityReferencesImpl(long peer) {
        return NodeIteratorNative.getExpandEntityReferences(peer);
    }

    public Node getReferenceNode() {
        return NodeImpl.getImpl(getReferenceNodeImpl(getPeer()));
    }
    static long getReferenceNodeImpl(long peer) {
        return NodeIteratorNative.getReferenceNode(peer);
    }

    public boolean getPointerBeforeReferenceNode() {
        return getPointerBeforeReferenceNodeImpl(getPeer());
    }
    static boolean getPointerBeforeReferenceNodeImpl(long peer) {
        return NodeIteratorNative.getPointerBeforeReferenceNode(peer);
    }


// Functions
    @Override
    public Node nextNode()
    {
        return NodeImpl.getImpl(nextNodeImpl(getPeer()));
    }
    static long nextNodeImpl(long peer) {
        return NodeIteratorNative.nextNode(peer);
    }


    @Override
    public Node previousNode()
    {
        return NodeImpl.getImpl(previousNodeImpl(getPeer()));
    }
    static long previousNodeImpl(long peer) {
        return NodeIteratorNative.previousNode(peer);
    }


    @Override
    public void detach()
    {
        detachImpl(getPeer());
    }
    static void detachImpl(long peer) {
        NodeIteratorNative.detach(peer);
    }


}

