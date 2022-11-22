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
import org.w3c.dom.xpath.XPathResult;

public class XPathResultImpl implements XPathResult {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
        public void dispose() {
            XPathResultImpl.dispose(peer);
        }
    }

    XPathResultImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static XPathResult create(long peer) {
        if (peer == 0L) return null;
        return new XPathResultImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof XPathResultImpl) && (peer == ((XPathResultImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(XPathResult arg) {
        return (arg == null) ? 0L : ((XPathResultImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    static XPathResult getImpl(long peer) {
        return (XPathResult)create(peer);
    }


// Constants
    public static final int ANY_TYPE = 0;
    public static final int NUMBER_TYPE = 1;
    public static final int STRING_TYPE = 2;
    public static final int BOOLEAN_TYPE = 3;
    public static final int UNORDERED_NODE_ITERATOR_TYPE = 4;
    public static final int ORDERED_NODE_ITERATOR_TYPE = 5;
    public static final int UNORDERED_NODE_SNAPSHOT_TYPE = 6;
    public static final int ORDERED_NODE_SNAPSHOT_TYPE = 7;
    public static final int ANY_UNORDERED_NODE_TYPE = 8;
    public static final int FIRST_ORDERED_NODE_TYPE = 9;

// Attributes
    public short getResultType() {
        return getResultTypeImpl(getPeer());
    }
    native static short getResultTypeImpl(long peer);

    public double getNumberValue() throws DOMException {
        return getNumberValueImpl(getPeer());
    }
    native static double getNumberValueImpl(long peer);

    public String getStringValue() throws DOMException {
        return getStringValueImpl(getPeer());
    }
    native static String getStringValueImpl(long peer);

    public boolean getBooleanValue() throws DOMException {
        return getBooleanValueImpl(getPeer());
    }
    native static boolean getBooleanValueImpl(long peer);

    public Node getSingleNodeValue() throws DOMException {
        return NodeImpl.getImpl(getSingleNodeValueImpl(getPeer()));
    }
    native static long getSingleNodeValueImpl(long peer);

    public boolean getInvalidIteratorState() {
        return getInvalidIteratorStateImpl(getPeer());
    }
    native static boolean getInvalidIteratorStateImpl(long peer);

    public int getSnapshotLength() throws DOMException {
        return getSnapshotLengthImpl(getPeer());
    }
    native static int getSnapshotLengthImpl(long peer);


// Functions
    public Node iterateNext() throws DOMException
    {
        return NodeImpl.getImpl(iterateNextImpl(getPeer()));
    }
    native static long iterateNextImpl(long peer);


    public Node snapshotItem(int index) throws DOMException
    {
        return NodeImpl.getImpl(snapshotItemImpl(getPeer()
            , index));
    }
    native static long snapshotItemImpl(long peer
        , int index);


}

