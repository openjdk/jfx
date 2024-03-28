/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

import org.w3c.dom.html.HTMLLinkElement;
import org.w3c.dom.stylesheets.StyleSheet;

public class HTMLLinkElementImpl extends HTMLElementImpl implements HTMLLinkElement {
    HTMLLinkElementImpl(long peer) {
        super(peer);
    }

    static HTMLLinkElement getImpl(long peer) {
        return (HTMLLinkElement)create(peer);
    }


// Attributes
    @Override
    public boolean getDisabled() {
        return getDisabledImpl(getPeer());
    }
    native static boolean getDisabledImpl(long peer);

    @Override
    public void setDisabled(boolean value) {
        setDisabledImpl(getPeer(), value);
    }
    native static void setDisabledImpl(long peer, boolean value);

    @Override
    public String getCharset() {
        return getCharsetImpl(getPeer());
    }
    native static String getCharsetImpl(long peer);

    @Override
    public void setCharset(String value) {
        setCharsetImpl(getPeer(), value);
    }
    native static void setCharsetImpl(long peer, String value);

    @Override
    public String getHref() {
        return getHrefImpl(getPeer());
    }
    native static String getHrefImpl(long peer);

    @Override
    public void setHref(String value) {
        setHrefImpl(getPeer(), value);
    }
    native static void setHrefImpl(long peer, String value);

    @Override
    public String getHreflang() {
        return getHreflangImpl(getPeer());
    }
    native static String getHreflangImpl(long peer);

    @Override
    public void setHreflang(String value) {
        setHreflangImpl(getPeer(), value);
    }
    native static void setHreflangImpl(long peer, String value);

    @Override
    public String getMedia() {
        return getMediaImpl(getPeer());
    }
    native static String getMediaImpl(long peer);

    @Override
    public void setMedia(String value) {
        setMediaImpl(getPeer(), value);
    }
    native static void setMediaImpl(long peer, String value);

    @Override
    public String getRel() {
        return getRelImpl(getPeer());
    }
    native static String getRelImpl(long peer);

    @Override
    public void setRel(String value) {
        setRelImpl(getPeer(), value);
    }
    native static void setRelImpl(long peer, String value);

    @Override
    public String getRev() {
        return getRevImpl(getPeer());
    }
    native static String getRevImpl(long peer);

    @Override
    public void setRev(String value) {
        setRevImpl(getPeer(), value);
    }
    native static void setRevImpl(long peer, String value);

    @Override
    public String getTarget() {
        return getTargetImpl(getPeer());
    }
    native static String getTargetImpl(long peer);

    @Override
    public void setTarget(String value) {
        setTargetImpl(getPeer(), value);
    }
    native static void setTargetImpl(long peer, String value);

    @Override
    public String getType() {
        return getTypeImpl(getPeer());
    }
    native static String getTypeImpl(long peer);

    @Override
    public void setType(String value) {
        setTypeImpl(getPeer(), value);
    }
    native static void setTypeImpl(long peer, String value);

    public StyleSheet getSheet() {
        return StyleSheetImpl.getImpl(getSheetImpl(getPeer()));
    }
    native static long getSheetImpl(long peer);

}

