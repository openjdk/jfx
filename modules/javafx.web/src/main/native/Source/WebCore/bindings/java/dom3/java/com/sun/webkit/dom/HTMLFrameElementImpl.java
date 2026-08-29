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
import org.w3c.dom.html.HTMLFrameElement;
import org.w3c.dom.views.AbstractView;

public class HTMLFrameElementImpl extends HTMLElementImpl implements HTMLFrameElement {
    HTMLFrameElementImpl(long peer) {
        super(peer);
    }

    static HTMLFrameElement getImpl(long peer) {
        return (HTMLFrameElement)create(peer);
    }


// Attributes
    @Override
    public String getFrameBorder() {
        return getFrameBorderImpl(getPeer());
    }
    static String getFrameBorderImpl(long peer) {
        return HTMLFrameElementNative.getFrameBorder(peer);
    }

    @Override
    public void setFrameBorder(String value) {
        setFrameBorderImpl(getPeer(), value);
    }
    static void setFrameBorderImpl(long peer, String value) {
        HTMLFrameElementNative.setFrameBorder(peer, value);
    }

    @Override
    public String getLongDesc() {
        return getLongDescImpl(getPeer());
    }
    static String getLongDescImpl(long peer) {
        return HTMLFrameElementNative.getLongDesc(peer);
    }

    @Override
    public void setLongDesc(String value) {
        setLongDescImpl(getPeer(), value);
    }
    static void setLongDescImpl(long peer, String value) {
        HTMLFrameElementNative.setLongDesc(peer, value);
    }

    @Override
    public String getMarginHeight() {
        return getMarginHeightImpl(getPeer());
    }
    static String getMarginHeightImpl(long peer) {
        return HTMLFrameElementNative.getMarginHeight(peer);
    }

    @Override
    public void setMarginHeight(String value) {
        setMarginHeightImpl(getPeer(), value);
    }
    static void setMarginHeightImpl(long peer, String value) {
        HTMLFrameElementNative.setMarginHeight(peer, value);
    }

    @Override
    public String getMarginWidth() {
        return getMarginWidthImpl(getPeer());
    }
    static String getMarginWidthImpl(long peer) {
        return HTMLFrameElementNative.getMarginWidth(peer);
    }

    @Override
    public void setMarginWidth(String value) {
        setMarginWidthImpl(getPeer(), value);
    }
    static void setMarginWidthImpl(long peer, String value) {
        HTMLFrameElementNative.setMarginWidth(peer, value);
    }

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLFrameElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLFrameElementNative.setName(peer, value);
    }

    @Override
    public boolean getNoResize() {
        return getNoResizeImpl(getPeer());
    }
    static boolean getNoResizeImpl(long peer) {
        return HTMLFrameElementNative.getNoResize(peer);
    }

    @Override
    public void setNoResize(boolean value) {
        setNoResizeImpl(getPeer(), value);
    }
    static void setNoResizeImpl(long peer, boolean value) {
        HTMLFrameElementNative.setNoResize(peer, value);
    }

    @Override
    public String getScrolling() {
        return getScrollingImpl(getPeer());
    }
    static String getScrollingImpl(long peer) {
        return HTMLFrameElementNative.getScrolling(peer);
    }

    @Override
    public void setScrolling(String value) {
        setScrollingImpl(getPeer(), value);
    }
    static void setScrollingImpl(long peer, String value) {
        HTMLFrameElementNative.setScrolling(peer, value);
    }

    @Override
    public String getSrc() {
        return getSrcImpl(getPeer());
    }
    static String getSrcImpl(long peer) {
        return HTMLFrameElementNative.getSrc(peer);
    }

    @Override
    public void setSrc(String value) {
        setSrcImpl(getPeer(), value);
    }
    static void setSrcImpl(long peer, String value) {
        HTMLFrameElementNative.setSrc(peer, value);
    }

    @Override
    public Document getContentDocument() {
        return DocumentImpl.getImpl(getContentDocumentImpl(getPeer()));
    }
    static long getContentDocumentImpl(long peer) {
        return HTMLFrameElementNative.getContentDocument(peer);
    }

    public AbstractView getContentWindow() {
        return DOMWindowImpl.getImpl(getContentWindowImpl(getPeer()));
    }
    static long getContentWindowImpl(long peer) {
        return HTMLFrameElementNative.getContentWindow(peer);
    }

    public String getLocation() {
        return getLocationImpl(getPeer());
    }
    static String getLocationImpl(long peer) {
        return HTMLFrameElementNative.getLocation(peer);
    }

    public void setLocation(String value) {
        setLocationImpl(getPeer(), value);
    }
    static void setLocationImpl(long peer, String value) {
        HTMLFrameElementNative.setLocation(peer, value);
    }

    public int getWidth() {
        return getWidthImpl(getPeer());
    }
    static int getWidthImpl(long peer) {
        return HTMLFrameElementNative.getWidth(peer);
    }

    public int getHeight() {
        return getHeightImpl(getPeer());
    }
    static int getHeightImpl(long peer) {
        return HTMLFrameElementNative.getHeight(peer);
    }

}

