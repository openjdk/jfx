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
import org.w3c.dom.DOMStringList;

public class DOMStringListImpl implements DOMStringList {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
        public void dispose() {
            DOMStringListImpl.dispose(peer);
        }
    }

    DOMStringListImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static DOMStringList create(long peer) {
        if (peer == 0L) return null;
        return new DOMStringListImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof DOMStringListImpl) && (peer == ((DOMStringListImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(DOMStringList arg) {
        return (arg == null) ? 0L : ((DOMStringListImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    static DOMStringList getImpl(long peer) {
        return (DOMStringList)create(peer);
    }


// Attributes
    public int getLength() {
        return getLengthImpl(getPeer());
    }
    native static int getLengthImpl(long peer);


// Functions
    public String item(int index)
    {
        return itemImpl(getPeer()
            , index);
    }
    native static String itemImpl(long peer
        , int index);


    public boolean contains(String string)
    {
        return containsImpl(getPeer()
            , string);
    }
    native static boolean containsImpl(long peer
        , String string);


}

