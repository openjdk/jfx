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

import org.w3c.dom.events.EventListener;
import org.w3c.dom.html.HTMLFrameSetElement;

public class HTMLFrameSetElementImpl extends HTMLElementImpl implements HTMLFrameSetElement {
    HTMLFrameSetElementImpl(long peer) {
        super(peer);
    }

    static HTMLFrameSetElement getImpl(long peer) {
        return (HTMLFrameSetElement)create(peer);
    }


// Attributes
    @Override
    public String getCols() {
        return getColsImpl(getPeer());
    }
    static String getColsImpl(long peer) {
        return HTMLFrameSetElementNative.getCols(peer);
    }

    @Override
    public void setCols(String value) {
        setColsImpl(getPeer(), value);
    }
    static void setColsImpl(long peer, String value) {
        HTMLFrameSetElementNative.setCols(peer, value);
    }

    @Override
    public String getRows() {
        return getRowsImpl(getPeer());
    }
    static String getRowsImpl(long peer) {
        return HTMLFrameSetElementNative.getRows(peer);
    }

    @Override
    public void setRows(String value) {
        setRowsImpl(getPeer(), value);
    }
    static void setRowsImpl(long peer, String value) {
        HTMLFrameSetElementNative.setRows(peer, value);
    }

    @Override
    public EventListener getOnblur() {
        return EventListenerImpl.getImpl(getOnblurImpl(getPeer()));
    }
    static long getOnblurImpl(long peer) {
        return HTMLFrameSetElementNative.getOnblur(peer);
    }

    @Override
    public void setOnblur(EventListener value) {
        setOnblurImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnblurImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnblur(peer, value);
    }

    @Override
    public EventListener getOnerror() {
        return EventListenerImpl.getImpl(getOnerrorImpl(getPeer()));
    }
    static long getOnerrorImpl(long peer) {
        return HTMLFrameSetElementNative.getOnerror(peer);
    }

    @Override
    public void setOnerror(EventListener value) {
        setOnerrorImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnerrorImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnerror(peer, value);
    }

    @Override
    public EventListener getOnfocus() {
        return EventListenerImpl.getImpl(getOnfocusImpl(getPeer()));
    }
    static long getOnfocusImpl(long peer) {
        return HTMLFrameSetElementNative.getOnfocus(peer);
    }

    @Override
    public void setOnfocus(EventListener value) {
        setOnfocusImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnfocusImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnfocus(peer, value);
    }

    @Override
    public EventListener getOnfocusin() {
        return EventListenerImpl.getImpl(getOnfocusinImpl(getPeer()));
    }
    static long getOnfocusinImpl(long peer) {
        return HTMLFrameSetElementNative.getOnfocusin(peer);
    }

    @Override
    public void setOnfocusin(EventListener value) {
        setOnfocusinImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnfocusinImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnfocusin(peer, value);
    }

    @Override
    public EventListener getOnfocusout() {
        return EventListenerImpl.getImpl(getOnfocusoutImpl(getPeer()));
    }
    static long getOnfocusoutImpl(long peer) {
        return HTMLFrameSetElementNative.getOnfocusout(peer);
    }

    @Override
    public void setOnfocusout(EventListener value) {
        setOnfocusoutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnfocusoutImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnfocusout(peer, value);
    }

    @Override
    public EventListener getOnload() {
        return EventListenerImpl.getImpl(getOnloadImpl(getPeer()));
    }
    static long getOnloadImpl(long peer) {
        return HTMLFrameSetElementNative.getOnload(peer);
    }

    @Override
    public void setOnload(EventListener value) {
        setOnloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnloadImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnload(peer, value);
    }

    @Override
    public EventListener getOnresize() {
        return EventListenerImpl.getImpl(getOnresizeImpl(getPeer()));
    }
    static long getOnresizeImpl(long peer) {
        return HTMLFrameSetElementNative.getOnresize(peer);
    }

    @Override
    public void setOnresize(EventListener value) {
        setOnresizeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnresizeImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnresize(peer, value);
    }

    @Override
    public EventListener getOnscroll() {
        return EventListenerImpl.getImpl(getOnscrollImpl(getPeer()));
    }
    static long getOnscrollImpl(long peer) {
        return HTMLFrameSetElementNative.getOnscroll(peer);
    }

    @Override
    public void setOnscroll(EventListener value) {
        setOnscrollImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnscrollImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnscroll(peer, value);
    }

    public EventListener getOnbeforeunload() {
        return EventListenerImpl.getImpl(getOnbeforeunloadImpl(getPeer()));
    }
    static long getOnbeforeunloadImpl(long peer) {
        return HTMLFrameSetElementNative.getOnbeforeunload(peer);
    }

    public void setOnbeforeunload(EventListener value) {
        setOnbeforeunloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnbeforeunloadImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnbeforeunload(peer, value);
    }

    public EventListener getOnhashchange() {
        return EventListenerImpl.getImpl(getOnhashchangeImpl(getPeer()));
    }
    static long getOnhashchangeImpl(long peer) {
        return HTMLFrameSetElementNative.getOnhashchange(peer);
    }

    public void setOnhashchange(EventListener value) {
        setOnhashchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnhashchangeImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnhashchange(peer, value);
    }

    public EventListener getOnmessage() {
        return EventListenerImpl.getImpl(getOnmessageImpl(getPeer()));
    }
    static long getOnmessageImpl(long peer) {
        return HTMLFrameSetElementNative.getOnmessage(peer);
    }

    public void setOnmessage(EventListener value) {
        setOnmessageImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmessageImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnmessage(peer, value);
    }

    public EventListener getOnoffline() {
        return EventListenerImpl.getImpl(getOnofflineImpl(getPeer()));
    }
    static long getOnofflineImpl(long peer) {
        return HTMLFrameSetElementNative.getOnoffline(peer);
    }

    public void setOnoffline(EventListener value) {
        setOnofflineImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnofflineImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnoffline(peer, value);
    }

    public EventListener getOnonline() {
        return EventListenerImpl.getImpl(getOnonlineImpl(getPeer()));
    }
    static long getOnonlineImpl(long peer) {
        return HTMLFrameSetElementNative.getOnonline(peer);
    }

    public void setOnonline(EventListener value) {
        setOnonlineImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnonlineImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnonline(peer, value);
    }

    public EventListener getOnpagehide() {
        return EventListenerImpl.getImpl(getOnpagehideImpl(getPeer()));
    }
    static long getOnpagehideImpl(long peer) {
        return HTMLFrameSetElementNative.getOnpagehide(peer);
    }

    public void setOnpagehide(EventListener value) {
        setOnpagehideImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnpagehideImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnpagehide(peer, value);
    }

    public EventListener getOnpageshow() {
        return EventListenerImpl.getImpl(getOnpageshowImpl(getPeer()));
    }
    static long getOnpageshowImpl(long peer) {
        return HTMLFrameSetElementNative.getOnpageshow(peer);
    }

    public void setOnpageshow(EventListener value) {
        setOnpageshowImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnpageshowImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnpageshow(peer, value);
    }

    public EventListener getOnpopstate() {
        return EventListenerImpl.getImpl(getOnpopstateImpl(getPeer()));
    }
    static long getOnpopstateImpl(long peer) {
        return HTMLFrameSetElementNative.getOnpopstate(peer);
    }

    public void setOnpopstate(EventListener value) {
        setOnpopstateImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnpopstateImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnpopstate(peer, value);
    }

    public EventListener getOnstorage() {
        return EventListenerImpl.getImpl(getOnstorageImpl(getPeer()));
    }
    static long getOnstorageImpl(long peer) {
        return HTMLFrameSetElementNative.getOnstorage(peer);
    }

    public void setOnstorage(EventListener value) {
        setOnstorageImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnstorageImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnstorage(peer, value);
    }

    public EventListener getOnunload() {
        return EventListenerImpl.getImpl(getOnunloadImpl(getPeer()));
    }
    static long getOnunloadImpl(long peer) {
        return HTMLFrameSetElementNative.getOnunload(peer);
    }

    public void setOnunload(EventListener value) {
        setOnunloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnunloadImpl(long peer, long value) {
        HTMLFrameSetElementNative.setOnunload(peer, value);
    }

}

