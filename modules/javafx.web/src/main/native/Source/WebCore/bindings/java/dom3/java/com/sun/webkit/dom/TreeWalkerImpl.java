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
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

public class TreeWalkerImpl implements TreeWalker {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
        public void dispose() {
            TreeWalkerImpl.dispose(peer);
        }
    }

    TreeWalkerImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static TreeWalker create(long peer) {
        if (peer == 0L) return null;
        return new TreeWalkerImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof TreeWalkerImpl) && (peer == ((TreeWalkerImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(TreeWalker arg) {
        return (arg == null) ? 0L : ((TreeWalkerImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    static TreeWalker getImpl(long peer) {
        return (TreeWalker)create(peer);
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

    public Node getCurrentNode() {
        return NodeImpl.getImpl(getCurrentNodeImpl(getPeer()));
    }
    native static long getCurrentNodeImpl(long peer);

    public void setCurrentNode(Node value) throws DOMException {
        setCurrentNodeImpl(getPeer(), NodeImpl.getPeer(value));
    }
    native static void setCurrentNodeImpl(long peer, long value);


// Functions
    public Node parentNode()
    {
        return NodeImpl.getImpl(parentNodeImpl(getPeer()));
    }
    native static long parentNodeImpl(long peer);


    public Node firstChild()
    {
        return NodeImpl.getImpl(firstChildImpl(getPeer()));
    }
    native static long firstChildImpl(long peer);


    public Node lastChild()
    {
        return NodeImpl.getImpl(lastChildImpl(getPeer()));
    }
    native static long lastChildImpl(long peer);


    public Node previousSibling()
    {
        return NodeImpl.getImpl(previousSiblingImpl(getPeer()));
    }
    native static long previousSiblingImpl(long peer);


    public Node nextSibling()
    {
        return NodeImpl.getImpl(nextSiblingImpl(getPeer()));
    }
    native static long nextSiblingImpl(long peer);


    public Node previousNode()
    {
        return NodeImpl.getImpl(previousNodeImpl(getPeer()));
    }
    native static long previousNodeImpl(long peer);


    public Node nextNode()
    {
        return NodeImpl.getImpl(nextNodeImpl(getPeer()));
    }
    native static long nextNodeImpl(long peer);


}

