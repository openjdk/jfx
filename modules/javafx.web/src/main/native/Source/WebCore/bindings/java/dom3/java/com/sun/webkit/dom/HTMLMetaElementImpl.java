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

import org.w3c.dom.html.HTMLMetaElement;

public class HTMLMetaElementImpl extends HTMLElementImpl implements HTMLMetaElement {
    HTMLMetaElementImpl(long peer) {
        super(peer);
    }

    static HTMLMetaElement getImpl(long peer) {
        return (HTMLMetaElement)create(peer);
    }


// Attributes
    @Override
    public String getContent() {
        return getContentImpl(getPeer());
    }
    static String getContentImpl(long peer) {
        return HTMLMetaElementNative.getContent(peer);
    }

    @Override
    public void setContent(String value) {
        setContentImpl(getPeer(), value);
    }
    static void setContentImpl(long peer, String value) {
        HTMLMetaElementNative.setContent(peer, value);
    }

    @Override
    public String getHttpEquiv() {
        return getHttpEquivImpl(getPeer());
    }
    static String getHttpEquivImpl(long peer) {
        return HTMLMetaElementNative.getHttpEquiv(peer);
    }

    @Override
    public void setHttpEquiv(String value) {
        setHttpEquivImpl(getPeer(), value);
    }
    static void setHttpEquivImpl(long peer, String value) {
        HTMLMetaElementNative.setHttpEquiv(peer, value);
    }

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLMetaElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLMetaElementNative.setName(peer, value);
    }

    @Override
    public String getScheme() {
        return getSchemeImpl(getPeer());
    }
    static String getSchemeImpl(long peer) {
        return HTMLMetaElementNative.getScheme(peer);
    }

    @Override
    public void setScheme(String value) {
        setSchemeImpl(getPeer(), value);
    }
    static void setSchemeImpl(long peer, String value) {
        HTMLMetaElementNative.setScheme(peer, value);
    }

}

