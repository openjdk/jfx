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

import org.w3c.dom.html.HTMLFontElement;

public class HTMLFontElementImpl extends HTMLElementImpl implements HTMLFontElement {
    HTMLFontElementImpl(long peer) {
        super(peer);
    }

    static HTMLFontElement getImpl(long peer) {
        return (HTMLFontElement)create(peer);
    }


// Attributes
    public String getColor() {
        return getColorImpl(getPeer());
    }
    native static String getColorImpl(long peer);

    public void setColor(String value) {
        setColorImpl(getPeer(), value);
    }
    native static void setColorImpl(long peer, String value);

    public String getFace() {
        return getFaceImpl(getPeer());
    }
    native static String getFaceImpl(long peer);

    public void setFace(String value) {
        setFaceImpl(getPeer(), value);
    }
    native static void setFaceImpl(long peer, String value);

    public String getSize() {
        return getSizeImpl(getPeer());
    }
    native static String getSizeImpl(long peer);

    public void setSize(String value) {
        setSizeImpl(getPeer(), value);
    }
    native static void setSizeImpl(long peer, String value);

}

