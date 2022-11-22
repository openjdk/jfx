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
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

public class NodeIteratorImpl implements NodeIterator {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
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

    native private static void dispose(long peer);

    static NodeIterator getImpl(long peer) {
        return (NodeIterator)create(peer);
    }


// Attributes
    public Node getRoot() {
        return NodeImpl.getImpl(getRootImpl(getPeer()));
    }
    native static long getRootImpl(long peer);

    public int getWhatToShow() {
        return getWhatToShowImpl(getPeer());
    }
    native static int getWhatToShowImpl(long peer);

    public NodeFilter getFilter() {
        return NodeFilterImpl.getImpl(getFilterImpl(getPeer()));
    }
    native static long getFilterImpl(long peer);

    public boolean getExpandEntityReferences() {
        return getExpandEntityReferencesImpl(getPeer());
    }
    native static boolean getExpandEntityReferencesImpl(long peer);

    public Node getReferenceNode() {
        return NodeImpl.getImpl(getReferenceNodeImpl(getPeer()));
    }
    native static long getReferenceNodeImpl(long peer);

    public boolean getPointerBeforeReferenceNode() {
        return getPointerBeforeReferenceNodeImpl(getPeer());
    }
    native static boolean getPointerBeforeReferenceNodeImpl(long peer);


// Functions
    public Node nextNode()
    {
        return NodeImpl.getImpl(nextNodeImpl(getPeer()));
    }
    native static long nextNodeImpl(long peer);


    public Node previousNode()
    {
        return NodeImpl.getImpl(previousNodeImpl(getPeer()));
    }
    native static long previousNodeImpl(long peer);


    public void detach()
    {
        detachImpl(getPeer());
    }
    native static void detachImpl(long peer);


}

