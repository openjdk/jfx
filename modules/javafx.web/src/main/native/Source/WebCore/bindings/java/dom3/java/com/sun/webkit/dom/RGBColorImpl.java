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
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.RGBColor;

public class RGBColorImpl implements RGBColor {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
        public void dispose() {
            RGBColorImpl.dispose(peer);
        }
    }

    RGBColorImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static RGBColor create(long peer) {
        if (peer == 0L) return null;
        return new RGBColorImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof RGBColorImpl) && (peer == ((RGBColorImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(RGBColor arg) {
        return (arg == null) ? 0L : ((RGBColorImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    static RGBColor getImpl(long peer) {
        return (RGBColor)create(peer);
    }


// Attributes
    public CSSPrimitiveValue getRed() {
        return CSSPrimitiveValueImpl.getImpl(getRedImpl(getPeer()));
    }
    native static long getRedImpl(long peer);

    public CSSPrimitiveValue getGreen() {
        return CSSPrimitiveValueImpl.getImpl(getGreenImpl(getPeer()));
    }
    native static long getGreenImpl(long peer);

    public CSSPrimitiveValue getBlue() {
        return CSSPrimitiveValueImpl.getImpl(getBlueImpl(getPeer()));
    }
    native static long getBlueImpl(long peer);

    public CSSPrimitiveValue getAlpha() {
        return CSSPrimitiveValueImpl.getImpl(getAlphaImpl(getPeer()));
    }
    native static long getAlphaImpl(long peer);

}

