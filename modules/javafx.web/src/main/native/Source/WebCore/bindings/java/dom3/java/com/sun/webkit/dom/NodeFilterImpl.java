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

public class NodeFilterImpl implements NodeFilter {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
        public void dispose() {
            NodeFilterImpl.dispose(peer);
        }
    }

    NodeFilterImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static NodeFilter create(long peer) {
        if (peer == 0L) return null;
        return new NodeFilterImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof NodeFilterImpl) && (peer == ((NodeFilterImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(NodeFilter arg) {
        return (arg == null) ? 0L : ((NodeFilterImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    static NodeFilter getImpl(long peer) {
        return (NodeFilter)create(peer);
    }


// Constants
    public static final int FILTER_ACCEPT = 1;
    public static final int FILTER_REJECT = 2;
    public static final int FILTER_SKIP = 3;
    public static final int SHOW_ALL = 0xFFFFFFFF;
    public static final int SHOW_ELEMENT = 0x00000001;
    public static final int SHOW_ATTRIBUTE = 0x00000002;
    public static final int SHOW_TEXT = 0x00000004;
    public static final int SHOW_CDATA_SECTION = 0x00000008;
    public static final int SHOW_ENTITY_REFERENCE = 0x00000010;
    public static final int SHOW_ENTITY = 0x00000020;
    public static final int SHOW_PROCESSING_INSTRUCTION = 0x00000040;
    public static final int SHOW_COMMENT = 0x00000080;
    public static final int SHOW_DOCUMENT = 0x00000100;
    public static final int SHOW_DOCUMENT_TYPE = 0x00000200;
    public static final int SHOW_DOCUMENT_FRAGMENT = 0x00000400;
    public static final int SHOW_NOTATION = 0x00000800;

// Functions
    public short acceptNode(Node n)
    {
        return acceptNodeImpl(getPeer()
            , NodeImpl.getPeer(n));
    }
    native static short acceptNodeImpl(long peer
        , long n);


}

