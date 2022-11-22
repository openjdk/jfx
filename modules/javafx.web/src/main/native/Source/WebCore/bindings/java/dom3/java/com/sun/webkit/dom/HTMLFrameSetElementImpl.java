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
    public String getCols() {
        return getColsImpl(getPeer());
    }
    native static String getColsImpl(long peer);

    public void setCols(String value) {
        setColsImpl(getPeer(), value);
    }
    native static void setColsImpl(long peer, String value);

    public String getRows() {
        return getRowsImpl(getPeer());
    }
    native static String getRowsImpl(long peer);

    public void setRows(String value) {
        setRowsImpl(getPeer(), value);
    }
    native static void setRowsImpl(long peer, String value);

    public EventListener getOnblur() {
        return EventListenerImpl.getImpl(getOnblurImpl(getPeer()));
    }
    native static long getOnblurImpl(long peer);

    public void setOnblur(EventListener value) {
        setOnblurImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnblurImpl(long peer, long value);

    public EventListener getOnerror() {
        return EventListenerImpl.getImpl(getOnerrorImpl(getPeer()));
    }
    native static long getOnerrorImpl(long peer);

    public void setOnerror(EventListener value) {
        setOnerrorImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnerrorImpl(long peer, long value);

    public EventListener getOnfocus() {
        return EventListenerImpl.getImpl(getOnfocusImpl(getPeer()));
    }
    native static long getOnfocusImpl(long peer);

    public void setOnfocus(EventListener value) {
        setOnfocusImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnfocusImpl(long peer, long value);

    public EventListener getOnfocusin() {
        return EventListenerImpl.getImpl(getOnfocusinImpl(getPeer()));
    }
    native static long getOnfocusinImpl(long peer);

    public void setOnfocusin(EventListener value) {
        setOnfocusinImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnfocusinImpl(long peer, long value);

    public EventListener getOnfocusout() {
        return EventListenerImpl.getImpl(getOnfocusoutImpl(getPeer()));
    }
    native static long getOnfocusoutImpl(long peer);

    public void setOnfocusout(EventListener value) {
        setOnfocusoutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnfocusoutImpl(long peer, long value);

    public EventListener getOnload() {
        return EventListenerImpl.getImpl(getOnloadImpl(getPeer()));
    }
    native static long getOnloadImpl(long peer);

    public void setOnload(EventListener value) {
        setOnloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnloadImpl(long peer, long value);

    public EventListener getOnresize() {
        return EventListenerImpl.getImpl(getOnresizeImpl(getPeer()));
    }
    native static long getOnresizeImpl(long peer);

    public void setOnresize(EventListener value) {
        setOnresizeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnresizeImpl(long peer, long value);

    public EventListener getOnscroll() {
        return EventListenerImpl.getImpl(getOnscrollImpl(getPeer()));
    }
    native static long getOnscrollImpl(long peer);

    public void setOnscroll(EventListener value) {
        setOnscrollImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnscrollImpl(long peer, long value);

    public EventListener getOnbeforeunload() {
        return EventListenerImpl.getImpl(getOnbeforeunloadImpl(getPeer()));
    }
    native static long getOnbeforeunloadImpl(long peer);

    public void setOnbeforeunload(EventListener value) {
        setOnbeforeunloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnbeforeunloadImpl(long peer, long value);

    public EventListener getOnhashchange() {
        return EventListenerImpl.getImpl(getOnhashchangeImpl(getPeer()));
    }
    native static long getOnhashchangeImpl(long peer);

    public void setOnhashchange(EventListener value) {
        setOnhashchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnhashchangeImpl(long peer, long value);

    public EventListener getOnmessage() {
        return EventListenerImpl.getImpl(getOnmessageImpl(getPeer()));
    }
    native static long getOnmessageImpl(long peer);

    public void setOnmessage(EventListener value) {
        setOnmessageImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmessageImpl(long peer, long value);

    public EventListener getOnoffline() {
        return EventListenerImpl.getImpl(getOnofflineImpl(getPeer()));
    }
    native static long getOnofflineImpl(long peer);

    public void setOnoffline(EventListener value) {
        setOnofflineImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnofflineImpl(long peer, long value);

    public EventListener getOnonline() {
        return EventListenerImpl.getImpl(getOnonlineImpl(getPeer()));
    }
    native static long getOnonlineImpl(long peer);

    public void setOnonline(EventListener value) {
        setOnonlineImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnonlineImpl(long peer, long value);

    public EventListener getOnpagehide() {
        return EventListenerImpl.getImpl(getOnpagehideImpl(getPeer()));
    }
    native static long getOnpagehideImpl(long peer);

    public void setOnpagehide(EventListener value) {
        setOnpagehideImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnpagehideImpl(long peer, long value);

    public EventListener getOnpageshow() {
        return EventListenerImpl.getImpl(getOnpageshowImpl(getPeer()));
    }
    native static long getOnpageshowImpl(long peer);

    public void setOnpageshow(EventListener value) {
        setOnpageshowImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnpageshowImpl(long peer, long value);

    public EventListener getOnpopstate() {
        return EventListenerImpl.getImpl(getOnpopstateImpl(getPeer()));
    }
    native static long getOnpopstateImpl(long peer);

    public void setOnpopstate(EventListener value) {
        setOnpopstateImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnpopstateImpl(long peer, long value);

    public EventListener getOnstorage() {
        return EventListenerImpl.getImpl(getOnstorageImpl(getPeer()));
    }
    native static long getOnstorageImpl(long peer);

    public void setOnstorage(EventListener value) {
        setOnstorageImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnstorageImpl(long peer, long value);

    public EventListener getOnunload() {
        return EventListenerImpl.getImpl(getOnunloadImpl(getPeer()));
    }
    native static long getOnunloadImpl(long peer);

    public void setOnunload(EventListener value) {
        setOnunloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnunloadImpl(long peer, long value);

}

