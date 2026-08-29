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
import org.w3c.dom.html.HTMLBodyElement;

public class HTMLBodyElementImpl extends HTMLElementImpl implements HTMLBodyElement {
    HTMLBodyElementImpl(long peer) {
        super(peer);
    }

    static HTMLBodyElement getImpl(long peer) {
        return (HTMLBodyElement)create(peer);
    }


// Attributes
    @Override
    public String getALink() {
        return getALinkImpl(getPeer());
    }
    static String getALinkImpl(long peer) {
        return HTMLBodyElementNative.getALink(peer);
    }

    @Override
    public void setALink(String value) {
        setALinkImpl(getPeer(), value);
    }
    static void setALinkImpl(long peer, String value) {
        HTMLBodyElementNative.setALink(peer, value);
    }

    @Override
    public String getBackground() {
        return getBackgroundImpl(getPeer());
    }
    static String getBackgroundImpl(long peer) {
        return HTMLBodyElementNative.getBackground(peer);
    }

    @Override
    public void setBackground(String value) {
        setBackgroundImpl(getPeer(), value);
    }
    static void setBackgroundImpl(long peer, String value) {
        HTMLBodyElementNative.setBackground(peer, value);
    }

    @Override
    public String getBgColor() {
        return getBgColorImpl(getPeer());
    }
    static String getBgColorImpl(long peer) {
        return HTMLBodyElementNative.getBgColor(peer);
    }

    @Override
    public void setBgColor(String value) {
        setBgColorImpl(getPeer(), value);
    }
    static void setBgColorImpl(long peer, String value) {
        HTMLBodyElementNative.setBgColor(peer, value);
    }

    @Override
    public String getLink() {
        return getLinkImpl(getPeer());
    }
    static String getLinkImpl(long peer) {
        return HTMLBodyElementNative.getLink(peer);
    }

    @Override
    public void setLink(String value) {
        setLinkImpl(getPeer(), value);
    }
    static void setLinkImpl(long peer, String value) {
        HTMLBodyElementNative.setLink(peer, value);
    }

    @Override
    public String getText() {
        return getTextImpl(getPeer());
    }
    static String getTextImpl(long peer) {
        return HTMLBodyElementNative.getText(peer);
    }

    @Override
    public void setText(String value) {
        setTextImpl(getPeer(), value);
    }
    static void setTextImpl(long peer, String value) {
        HTMLBodyElementNative.setText(peer, value);
    }

    @Override
    public String getVLink() {
        return getVLinkImpl(getPeer());
    }
    static String getVLinkImpl(long peer) {
        return HTMLBodyElementNative.getVLink(peer);
    }

    @Override
    public void setVLink(String value) {
        setVLinkImpl(getPeer(), value);
    }
    static void setVLinkImpl(long peer, String value) {
        HTMLBodyElementNative.setVLink(peer, value);
    }

    @Override
    public EventListener getOnblur() {
        return EventListenerImpl.getImpl(getOnblurImpl(getPeer()));
    }
    static long getOnblurImpl(long peer) {
        return HTMLBodyElementNative.getOnblur(peer);
    }

    @Override
    public void setOnblur(EventListener value) {
        setOnblurImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnblurImpl(long peer, long value) {
        HTMLBodyElementNative.setOnblur(peer, value);
    }

    @Override
    public EventListener getOnerror() {
        return EventListenerImpl.getImpl(getOnerrorImpl(getPeer()));
    }
    static long getOnerrorImpl(long peer) {
        return HTMLBodyElementNative.getOnerror(peer);
    }

    @Override
    public void setOnerror(EventListener value) {
        setOnerrorImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnerrorImpl(long peer, long value) {
        HTMLBodyElementNative.setOnerror(peer, value);
    }

    @Override
    public EventListener getOnfocus() {
        return EventListenerImpl.getImpl(getOnfocusImpl(getPeer()));
    }
    static long getOnfocusImpl(long peer) {
        return HTMLBodyElementNative.getOnfocus(peer);
    }

    @Override
    public void setOnfocus(EventListener value) {
        setOnfocusImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnfocusImpl(long peer, long value) {
        HTMLBodyElementNative.setOnfocus(peer, value);
    }

    @Override
    public EventListener getOnfocusin() {
        return EventListenerImpl.getImpl(getOnfocusinImpl(getPeer()));
    }
    static long getOnfocusinImpl(long peer) {
        return HTMLBodyElementNative.getOnfocusin(peer);
    }

    @Override
    public void setOnfocusin(EventListener value) {
        setOnfocusinImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnfocusinImpl(long peer, long value) {
        HTMLBodyElementNative.setOnfocusin(peer, value);
    }

    @Override
    public EventListener getOnfocusout() {
        return EventListenerImpl.getImpl(getOnfocusoutImpl(getPeer()));
    }
    static long getOnfocusoutImpl(long peer) {
        return HTMLBodyElementNative.getOnfocusout(peer);
    }

    @Override
    public void setOnfocusout(EventListener value) {
        setOnfocusoutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnfocusoutImpl(long peer, long value) {
        HTMLBodyElementNative.setOnfocusout(peer, value);
    }

    @Override
    public EventListener getOnload() {
        return EventListenerImpl.getImpl(getOnloadImpl(getPeer()));
    }
    static long getOnloadImpl(long peer) {
        return HTMLBodyElementNative.getOnload(peer);
    }

    @Override
    public void setOnload(EventListener value) {
        setOnloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnloadImpl(long peer, long value) {
        HTMLBodyElementNative.setOnload(peer, value);
    }

    @Override
    public EventListener getOnresize() {
        return EventListenerImpl.getImpl(getOnresizeImpl(getPeer()));
    }
    static long getOnresizeImpl(long peer) {
        return HTMLBodyElementNative.getOnresize(peer);
    }

    @Override
    public void setOnresize(EventListener value) {
        setOnresizeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnresizeImpl(long peer, long value) {
        HTMLBodyElementNative.setOnresize(peer, value);
    }

    @Override
    public EventListener getOnscroll() {
        return EventListenerImpl.getImpl(getOnscrollImpl(getPeer()));
    }
    static long getOnscrollImpl(long peer) {
        return HTMLBodyElementNative.getOnscroll(peer);
    }

    @Override
    public void setOnscroll(EventListener value) {
        setOnscrollImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnscrollImpl(long peer, long value) {
        HTMLBodyElementNative.setOnscroll(peer, value);
    }

    public EventListener getOnselectionchange() {
        return EventListenerImpl.getImpl(getOnselectionchangeImpl(getPeer()));
    }
    static long getOnselectionchangeImpl(long peer) {
        return HTMLBodyElementNative.getOnselectionchange(peer);
    }

    public void setOnselectionchange(EventListener value) {
        setOnselectionchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnselectionchangeImpl(long peer, long value) {
        HTMLBodyElementNative.setOnselectionchange(peer, value);
    }

    public EventListener getOnbeforeunload() {
        return EventListenerImpl.getImpl(getOnbeforeunloadImpl(getPeer()));
    }
    static long getOnbeforeunloadImpl(long peer) {
        return HTMLBodyElementNative.getOnbeforeunload(peer);
    }

    public void setOnbeforeunload(EventListener value) {
        setOnbeforeunloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnbeforeunloadImpl(long peer, long value) {
        HTMLBodyElementNative.setOnbeforeunload(peer, value);
    }

    public EventListener getOnhashchange() {
        return EventListenerImpl.getImpl(getOnhashchangeImpl(getPeer()));
    }
    static long getOnhashchangeImpl(long peer) {
        return HTMLBodyElementNative.getOnhashchange(peer);
    }

    public void setOnhashchange(EventListener value) {
        setOnhashchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnhashchangeImpl(long peer, long value) {
        HTMLBodyElementNative.setOnhashchange(peer, value);
    }

    public EventListener getOnmessage() {
        return EventListenerImpl.getImpl(getOnmessageImpl(getPeer()));
    }
    static long getOnmessageImpl(long peer) {
        return HTMLBodyElementNative.getOnmessage(peer);
    }

    public void setOnmessage(EventListener value) {
        setOnmessageImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmessageImpl(long peer, long value) {
        HTMLBodyElementNative.setOnmessage(peer, value);
    }

    public EventListener getOnoffline() {
        return EventListenerImpl.getImpl(getOnofflineImpl(getPeer()));
    }
    static long getOnofflineImpl(long peer) {
        return HTMLBodyElementNative.getOnoffline(peer);
    }

    public void setOnoffline(EventListener value) {
        setOnofflineImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnofflineImpl(long peer, long value) {
        HTMLBodyElementNative.setOnoffline(peer, value);
    }

    public EventListener getOnonline() {
        return EventListenerImpl.getImpl(getOnonlineImpl(getPeer()));
    }
    static long getOnonlineImpl(long peer) {
        return HTMLBodyElementNative.getOnonline(peer);
    }

    public void setOnonline(EventListener value) {
        setOnonlineImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnonlineImpl(long peer, long value) {
        HTMLBodyElementNative.setOnonline(peer, value);
    }

    public EventListener getOnpagehide() {
        return EventListenerImpl.getImpl(getOnpagehideImpl(getPeer()));
    }
    static long getOnpagehideImpl(long peer) {
        return HTMLBodyElementNative.getOnpagehide(peer);
    }

    public void setOnpagehide(EventListener value) {
        setOnpagehideImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnpagehideImpl(long peer, long value) {
        HTMLBodyElementNative.setOnpagehide(peer, value);
    }

    public EventListener getOnpageshow() {
        return EventListenerImpl.getImpl(getOnpageshowImpl(getPeer()));
    }
    static long getOnpageshowImpl(long peer) {
        return HTMLBodyElementNative.getOnpageshow(peer);
    }

    public void setOnpageshow(EventListener value) {
        setOnpageshowImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnpageshowImpl(long peer, long value) {
        HTMLBodyElementNative.setOnpageshow(peer, value);
    }

    public EventListener getOnpopstate() {
        return EventListenerImpl.getImpl(getOnpopstateImpl(getPeer()));
    }
    static long getOnpopstateImpl(long peer) {
        return HTMLBodyElementNative.getOnpopstate(peer);
    }

    public void setOnpopstate(EventListener value) {
        setOnpopstateImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnpopstateImpl(long peer, long value) {
        HTMLBodyElementNative.setOnpopstate(peer, value);
    }

    public EventListener getOnstorage() {
        return EventListenerImpl.getImpl(getOnstorageImpl(getPeer()));
    }
    static long getOnstorageImpl(long peer) {
        return HTMLBodyElementNative.getOnstorage(peer);
    }

    public void setOnstorage(EventListener value) {
        setOnstorageImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnstorageImpl(long peer, long value) {
        HTMLBodyElementNative.setOnstorage(peer, value);
    }

    public EventListener getOnunload() {
        return EventListenerImpl.getImpl(getOnunloadImpl(getPeer()));
    }
    static long getOnunloadImpl(long peer) {
        return HTMLBodyElementNative.getOnunload(peer);
    }

    public void setOnunload(EventListener value) {
        setOnunloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnunloadImpl(long peer, long value) {
        HTMLBodyElementNative.setOnunload(peer, value);
    }

}

