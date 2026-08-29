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

import org.w3c.dom.html.HTMLFontElement;

public class HTMLFontElementImpl extends HTMLElementImpl implements HTMLFontElement {
    HTMLFontElementImpl(long peer) {
        super(peer);
    }

    static HTMLFontElement getImpl(long peer) {
        return (HTMLFontElement)create(peer);
    }


// Attributes
    @Override
    public String getColor() {
        return getColorImpl(getPeer());
    }
    static String getColorImpl(long peer) {
        return HTMLFontElementNative.getColor(peer);
    }

    @Override
    public void setColor(String value) {
        setColorImpl(getPeer(), value);
    }
    static void setColorImpl(long peer, String value) {
        HTMLFontElementNative.setColor(peer, value);
    }

    @Override
    public String getFace() {
        return getFaceImpl(getPeer());
    }
    static String getFaceImpl(long peer) {
        return HTMLFontElementNative.getFace(peer);
    }

    @Override
    public void setFace(String value) {
        setFaceImpl(getPeer(), value);
    }
    static void setFaceImpl(long peer, String value) {
        HTMLFontElementNative.setFace(peer, value);
    }

    @Override
    public String getSize() {
        return getSizeImpl(getPeer());
    }
    static String getSizeImpl(long peer) {
        return HTMLFontElementNative.getSize(peer);
    }

    @Override
    public void setSize(String value) {
        setSizeImpl(getPeer(), value);
    }
    static void setSizeImpl(long peer, String value) {
        HTMLFontElementNative.setSize(peer, value);
    }

}

