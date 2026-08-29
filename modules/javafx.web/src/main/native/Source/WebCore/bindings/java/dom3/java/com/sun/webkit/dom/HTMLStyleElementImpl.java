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

import org.w3c.dom.html.HTMLStyleElement;
import org.w3c.dom.stylesheets.StyleSheet;

public class HTMLStyleElementImpl extends HTMLElementImpl implements HTMLStyleElement {
    HTMLStyleElementImpl(long peer) {
        super(peer);
    }

    static HTMLStyleElement getImpl(long peer) {
        return (HTMLStyleElement)create(peer);
    }


// Attributes
    @Override
    public boolean getDisabled() {
        return getDisabledImpl(getPeer());
    }
    static boolean getDisabledImpl(long peer) {
        return HTMLStyleElementNative.getDisabled(peer);
    }

    @Override
    public void setDisabled(boolean value) {
        setDisabledImpl(getPeer(), value);
    }
    static void setDisabledImpl(long peer, boolean value) {
        HTMLStyleElementNative.setDisabled(peer, value);
    }

    @Override
    public String getMedia() {
        return getMediaImpl(getPeer());
    }
    static String getMediaImpl(long peer) {
        return HTMLStyleElementNative.getMedia(peer);
    }

    @Override
    public void setMedia(String value) {
        setMediaImpl(getPeer(), value);
    }
    static void setMediaImpl(long peer, String value) {
        HTMLStyleElementNative.setMedia(peer, value);
    }

    @Override
    public String getType() {
        return getTypeImpl(getPeer());
    }
    static String getTypeImpl(long peer) {
        return HTMLStyleElementNative.getType(peer);
    }

    @Override
    public void setType(String value) {
        setTypeImpl(getPeer(), value);
    }
    static void setTypeImpl(long peer, String value) {
        HTMLStyleElementNative.setType(peer, value);
    }

    public StyleSheet getSheet() {
        return StyleSheetImpl.getImpl(getSheetImpl(getPeer()));
    }
    static long getSheetImpl(long peer) {
        return HTMLStyleElementNative.getSheet(peer);
    }

}

