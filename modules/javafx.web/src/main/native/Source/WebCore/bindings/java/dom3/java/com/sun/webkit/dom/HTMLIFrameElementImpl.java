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
    @Override
    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    static String getAlignImpl(long peer) {
        return HTMLIFrameElementNative.getAlign(peer);
    }

    @Override
    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    static void setAlignImpl(long peer, String value) {
        HTMLIFrameElementNative.setAlign(peer, value);
    }

    @Override
    public String getFrameBorder() {
        return getFrameBorderImpl(getPeer());
    }
    static String getFrameBorderImpl(long peer) {
        return HTMLIFrameElementNative.getFrameBorder(peer);
    }

    @Override
    public void setFrameBorder(String value) {
        setFrameBorderImpl(getPeer(), value);
    }
    static void setFrameBorderImpl(long peer, String value) {
        HTMLIFrameElementNative.setFrameBorder(peer, value);
    }

    @Override
    public String getHeight() {
        return getHeightImpl(getPeer());
    }
    static String getHeightImpl(long peer) {
        return HTMLIFrameElementNative.getHeight(peer);
    }

    @Override
    public void setHeight(String value) {
        setHeightImpl(getPeer(), value);
    }
    static void setHeightImpl(long peer, String value) {
        HTMLIFrameElementNative.setHeight(peer, value);
    }

    @Override
    public String getLongDesc() {
        return getLongDescImpl(getPeer());
    }
    static String getLongDescImpl(long peer) {
        return HTMLIFrameElementNative.getLongDesc(peer);
    }

    @Override
    public void setLongDesc(String value) {
        setLongDescImpl(getPeer(), value);
    }
    static void setLongDescImpl(long peer, String value) {
        HTMLIFrameElementNative.setLongDesc(peer, value);
    }

    @Override
    public String getMarginHeight() {
        return getMarginHeightImpl(getPeer());
    }
    static String getMarginHeightImpl(long peer) {
        return HTMLIFrameElementNative.getMarginHeight(peer);
    }

    @Override
    public void setMarginHeight(String value) {
        setMarginHeightImpl(getPeer(), value);
    }
    static void setMarginHeightImpl(long peer, String value) {
        HTMLIFrameElementNative.setMarginHeight(peer, value);
    }

    @Override
    public String getMarginWidth() {
        return getMarginWidthImpl(getPeer());
    }
    static String getMarginWidthImpl(long peer) {
        return HTMLIFrameElementNative.getMarginWidth(peer);
    }

    @Override
    public void setMarginWidth(String value) {
        setMarginWidthImpl(getPeer(), value);
    }
    static void setMarginWidthImpl(long peer, String value) {
        HTMLIFrameElementNative.setMarginWidth(peer, value);
    }

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLIFrameElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLIFrameElementNative.setName(peer, value);
    }

    @Override
    public String getScrolling() {
        return getScrollingImpl(getPeer());
    }
    static String getScrollingImpl(long peer) {
        return HTMLIFrameElementNative.getScrolling(peer);
    }

    @Override
    public void setScrolling(String value) {
        setScrollingImpl(getPeer(), value);
    }
    static void setScrollingImpl(long peer, String value) {
        HTMLIFrameElementNative.setScrolling(peer, value);
    }

    @Override
    public String getSrc() {
        return getSrcImpl(getPeer());
    }
    static String getSrcImpl(long peer) {
        return HTMLIFrameElementNative.getSrc(peer);
    }

    @Override
    public void setSrc(String value) {
        setSrcImpl(getPeer(), value);
    }
    static void setSrcImpl(long peer, String value) {
        HTMLIFrameElementNative.setSrc(peer, value);
    }

    public String getSrcdoc() {
        return getSrcdocImpl(getPeer());
    }
    static String getSrcdocImpl(long peer) {
        return HTMLIFrameElementNative.getSrcdoc(peer);
    }

    public void setSrcdoc(String value) {
        setSrcdocImpl(getPeer(), value);
    }
    static void setSrcdocImpl(long peer, String value) {
        HTMLIFrameElementNative.setSrcdoc(peer, value);
    }

    @Override
    public String getWidth() {
        return getWidthImpl(getPeer());
    }
    static String getWidthImpl(long peer) {
        return HTMLIFrameElementNative.getWidth(peer);
    }

    @Override
    public void setWidth(String value) {
        setWidthImpl(getPeer(), value);
    }
    static void setWidthImpl(long peer, String value) {
        HTMLIFrameElementNative.setWidth(peer, value);
    }

    @Override
    public Document getContentDocument() {
        return DocumentImpl.getImpl(getContentDocumentImpl(getPeer()));
    }
    static long getContentDocumentImpl(long peer) {
        return HTMLIFrameElementNative.getContentDocument(peer);
    }

    public AbstractView getContentWindow() {
        return DOMWindowImpl.getImpl(getContentWindowImpl(getPeer()));
    }
    static long getContentWindowImpl(long peer) {
        return HTMLIFrameElementNative.getContentWindow(peer);
    }

}

