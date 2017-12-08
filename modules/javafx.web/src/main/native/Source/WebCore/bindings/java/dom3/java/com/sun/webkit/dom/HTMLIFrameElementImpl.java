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

import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLIFrameElement;
import org.w3c.dom.views.AbstractView;

public class HTMLIFrameElementImpl extends HTMLElementImpl implements HTMLIFrameElement {
    HTMLIFrameElementImpl(long peer) {
        super(peer);
    }

    static HTMLIFrameElement getImpl(long peer) {
        return (HTMLIFrameElement)create(peer);
    }


// Attributes
    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    native static String getAlignImpl(long peer);

    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    native static void setAlignImpl(long peer, String value);

    public String getFrameBorder() {
        return getFrameBorderImpl(getPeer());
    }
    native static String getFrameBorderImpl(long peer);

    public void setFrameBorder(String value) {
        setFrameBorderImpl(getPeer(), value);
    }
    native static void setFrameBorderImpl(long peer, String value);

    public String getHeight() {
        return getHeightImpl(getPeer());
    }
    native static String getHeightImpl(long peer);

    public void setHeight(String value) {
        setHeightImpl(getPeer(), value);
    }
    native static void setHeightImpl(long peer, String value);

    public String getLongDesc() {
        return getLongDescImpl(getPeer());
    }
    native static String getLongDescImpl(long peer);

    public void setLongDesc(String value) {
        setLongDescImpl(getPeer(), value);
    }
    native static void setLongDescImpl(long peer, String value);

    public String getMarginHeight() {
        return getMarginHeightImpl(getPeer());
    }
    native static String getMarginHeightImpl(long peer);

    public void setMarginHeight(String value) {
        setMarginHeightImpl(getPeer(), value);
    }
    native static void setMarginHeightImpl(long peer, String value);

    public String getMarginWidth() {
        return getMarginWidthImpl(getPeer());
    }
    native static String getMarginWidthImpl(long peer);

    public void setMarginWidth(String value) {
        setMarginWidthImpl(getPeer(), value);
    }
    native static void setMarginWidthImpl(long peer, String value);

    public String getName() {
        return getNameImpl(getPeer());
    }
    native static String getNameImpl(long peer);

    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    native static void setNameImpl(long peer, String value);

    public String getScrolling() {
        return getScrollingImpl(getPeer());
    }
    native static String getScrollingImpl(long peer);

    public void setScrolling(String value) {
        setScrollingImpl(getPeer(), value);
    }
    native static void setScrollingImpl(long peer, String value);

    public String getSrc() {
        return getSrcImpl(getPeer());
    }
    native static String getSrcImpl(long peer);

    public void setSrc(String value) {
        setSrcImpl(getPeer(), value);
    }
    native static void setSrcImpl(long peer, String value);

    public String getSrcdoc() {
        return getSrcdocImpl(getPeer());
    }
    native static String getSrcdocImpl(long peer);

    public void setSrcdoc(String value) {
        setSrcdocImpl(getPeer(), value);
    }
    native static void setSrcdocImpl(long peer, String value);

    public String getWidth() {
        return getWidthImpl(getPeer());
    }
    native static String getWidthImpl(long peer);

    public void setWidth(String value) {
        setWidthImpl(getPeer(), value);
    }
    native static void setWidthImpl(long peer, String value);

    public Document getContentDocument() {
        return DocumentImpl.getImpl(getContentDocumentImpl(getPeer()));
    }
    native static long getContentDocumentImpl(long peer);

    public AbstractView getContentWindow() {
        return DOMWindowImpl.getImpl(getContentWindowImpl(getPeer()));
    }
    native static long getContentWindowImpl(long peer);

}

