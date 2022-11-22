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
import org.w3c.dom.stylesheets.MediaList;
import org.w3c.dom.stylesheets.StyleSheet;

public class StyleSheetImpl implements StyleSheet {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
        public void dispose() {
            StyleSheetImpl.dispose(peer);
        }
    }

    StyleSheetImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static StyleSheet create(long peer) {
        if (peer == 0L) return null;
        switch (StyleSheetImpl.getCPPTypeImpl(peer)) {
        case TYPE_CSSStyleSheet: return new CSSStyleSheetImpl(peer);
        }
        return new StyleSheetImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof StyleSheetImpl) && (peer == ((StyleSheetImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(StyleSheet arg) {
        return (arg == null) ? 0L : ((StyleSheetImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    private static final int TYPE_CSSStyleSheet = 1;
    native private static int getCPPTypeImpl(long peer);

    static StyleSheet getImpl(long peer) {
        return (StyleSheet)create(peer);
    }


// Attributes
    public String getType() {
        return getTypeImpl(getPeer());
    }
    native static String getTypeImpl(long peer);

    public boolean getDisabled() {
        return getDisabledImpl(getPeer());
    }
    native static boolean getDisabledImpl(long peer);

    public void setDisabled(boolean value) {
        setDisabledImpl(getPeer(), value);
    }
    native static void setDisabledImpl(long peer, boolean value);

    public Node getOwnerNode() {
        return NodeImpl.getImpl(getOwnerNodeImpl(getPeer()));
    }
    native static long getOwnerNodeImpl(long peer);

    public StyleSheet getParentStyleSheet() {
        return StyleSheetImpl.getImpl(getParentStyleSheetImpl(getPeer()));
    }
    native static long getParentStyleSheetImpl(long peer);

    public String getHref() {
        return getHrefImpl(getPeer());
    }
    native static String getHrefImpl(long peer);

    public String getTitle() {
        return getTitleImpl(getPeer());
    }
    native static String getTitleImpl(long peer);

    public MediaList getMedia() {
        return MediaListImpl.getImpl(getMediaImpl(getPeer()));
    }
    native static long getMediaImpl(long peer);

}

