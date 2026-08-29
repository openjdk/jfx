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
import org.w3c.dom.xpath.XPathResult;

public class XPathResultImpl implements XPathResult {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }

        @Override
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

    private static void dispose(long peer) {
        XPathResultNative.dispose(peer);
    }

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
    @Override
    public short getResultType() {
        return getResultTypeImpl(getPeer());
    }
    static short getResultTypeImpl(long peer) {
        return XPathResultNative.getResultType(peer);
    }

    @Override
    public double getNumberValue() throws DOMException {
        return getNumberValueImpl(getPeer());
    }
    static double getNumberValueImpl(long peer) {
        return XPathResultNative.getNumberValue(peer);
    }

    @Override
    public String getStringValue() throws DOMException {
        return getStringValueImpl(getPeer());
    }
    static String getStringValueImpl(long peer) {
        return XPathResultNative.getStringValue(peer);
    }

    @Override
    public boolean getBooleanValue() throws DOMException {
        return getBooleanValueImpl(getPeer());
    }
    static boolean getBooleanValueImpl(long peer) {
        return XPathResultNative.getBooleanValue(peer);
    }

    @Override
    public Node getSingleNodeValue() throws DOMException {
        return NodeImpl.getImpl(getSingleNodeValueImpl(getPeer()));
    }
    static long getSingleNodeValueImpl(long peer) {
        return XPathResultNative.getSingleNodeValue(peer);
    }

    @Override
    public boolean getInvalidIteratorState() {
        return getInvalidIteratorStateImpl(getPeer());
    }
    static boolean getInvalidIteratorStateImpl(long peer) {
        return XPathResultNative.getInvalidIteratorState(peer);
    }

    @Override
    public int getSnapshotLength() throws DOMException {
        return getSnapshotLengthImpl(getPeer());
    }
    static int getSnapshotLengthImpl(long peer) {
        return XPathResultNative.getSnapshotLength(peer);
    }


// Functions
    @Override
    public Node iterateNext() throws DOMException
    {
        return NodeImpl.getImpl(iterateNextImpl(getPeer()));
    }
    static long iterateNextImpl(long peer) {
        return XPathResultNative.iterateNext(peer);
    }


    @Override
    public Node snapshotItem(int index) throws DOMException
    {
        return NodeImpl.getImpl(snapshotItemImpl(getPeer()
            , index));
    }
    static long snapshotItemImpl(long peer
        , int index) {
        return XPathResultNative.snapshotItem(peer, index);
    }


}

