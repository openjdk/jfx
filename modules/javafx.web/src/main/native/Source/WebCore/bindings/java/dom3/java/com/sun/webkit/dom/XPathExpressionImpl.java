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
import org.w3c.dom.xpath.XPathExpression;
import org.w3c.dom.xpath.XPathResult;

public class XPathExpressionImpl implements XPathExpression {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
        public void dispose() {
            XPathExpressionImpl.dispose(peer);
        }
    }

    XPathExpressionImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static XPathExpression create(long peer) {
        if (peer == 0L) return null;
        return new XPathExpressionImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof XPathExpressionImpl) && (peer == ((XPathExpressionImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(XPathExpression arg) {
        return (arg == null) ? 0L : ((XPathExpressionImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    static XPathExpression getImpl(long peer) {
        return (XPathExpression)create(peer);
    }

    @Override public Object evaluate(Node contextNode, short type, Object result) throws DOMException {
        return evaluate(contextNode, type, (XPathResult)result);
    }

// Functions
    public XPathResult evaluate(Node contextNode
        , short type
        , XPathResult inResult) throws DOMException
    {
        return XPathResultImpl.getImpl(evaluateImpl(getPeer()
            , NodeImpl.getPeer(contextNode)
            , type
            , XPathResultImpl.getPeer(inResult)));
    }
    native static long evaluateImpl(long peer
        , long contextNode
        , short type
        , long inResult);


}

