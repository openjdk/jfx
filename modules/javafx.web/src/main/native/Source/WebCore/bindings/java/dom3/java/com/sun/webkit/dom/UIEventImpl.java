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

import org.w3c.dom.events.UIEvent;
import org.w3c.dom.views.AbstractView;

public class UIEventImpl extends EventImpl implements UIEvent {
    UIEventImpl(long peer) {
        super(peer);
    }

    static UIEvent getImpl(long peer) {
        return (UIEvent)create(peer);
    }


// Attributes
    @Override
    public AbstractView getView() {
        return DOMWindowImpl.getImpl(getViewImpl(getPeer()));
    }
    static long getViewImpl(long peer) {
        return UIEventNative.getView(peer);
    }

    @Override
    public int getDetail() {
        return getDetailImpl(getPeer());
    }
    static int getDetailImpl(long peer) {
        return UIEventNative.getDetail(peer);
    }

    public int getKeyCode() {
        return getKeyCodeImpl(getPeer());
    }
    static int getKeyCodeImpl(long peer) {
        return UIEventNative.getKeyCode(peer);
    }

    public int getCharCode() {
        return getCharCodeImpl(getPeer());
    }
    static int getCharCodeImpl(long peer) {
        return UIEventNative.getCharCode(peer);
    }

    public int getLayerX() {
        return getLayerXImpl(getPeer());
    }
    static int getLayerXImpl(long peer) {
        return UIEventNative.getLayerX(peer);
    }

    public int getLayerY() {
        return getLayerYImpl(getPeer());
    }
    static int getLayerYImpl(long peer) {
        return UIEventNative.getLayerY(peer);
    }

    public int getPageX() {
        return getPageXImpl(getPeer());
    }
    static int getPageXImpl(long peer) {
        return UIEventNative.getPageX(peer);
    }

    public int getPageY() {
        return getPageYImpl(getPeer());
    }
    static int getPageYImpl(long peer) {
        return UIEventNative.getPageY(peer);
    }

    public int getWhich() {
        return getWhichImpl(getPeer());
    }
    static int getWhichImpl(long peer) {
        return UIEventNative.getWhich(peer);
    }


// Functions
    @Override
    public void initUIEvent(String type
        , boolean canBubble
        , boolean cancelable
        , AbstractView view
        , int detail)
    {
        initUIEventImpl(getPeer()
            , type
            , canBubble
            , cancelable
            , DOMWindowImpl.getPeer(view)
            , detail);
    }
    static void initUIEventImpl(long peer
        , String type
        , boolean canBubble
        , boolean cancelable
        , long view
        , int detail) {
        UIEventNative.initUIEvent(peer, type, canBubble, cancelable, view, detail);
    }


}

