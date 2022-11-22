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
    public AbstractView getView() {
        return DOMWindowImpl.getImpl(getViewImpl(getPeer()));
    }
    native static long getViewImpl(long peer);

    public int getDetail() {
        return getDetailImpl(getPeer());
    }
    native static int getDetailImpl(long peer);

    public int getKeyCode() {
        return getKeyCodeImpl(getPeer());
    }
    native static int getKeyCodeImpl(long peer);

    public int getCharCode() {
        return getCharCodeImpl(getPeer());
    }
    native static int getCharCodeImpl(long peer);

    public int getLayerX() {
        return getLayerXImpl(getPeer());
    }
    native static int getLayerXImpl(long peer);

    public int getLayerY() {
        return getLayerYImpl(getPeer());
    }
    native static int getLayerYImpl(long peer);

    public int getPageX() {
        return getPageXImpl(getPeer());
    }
    native static int getPageXImpl(long peer);

    public int getPageY() {
        return getPageYImpl(getPeer());
    }
    native static int getPageYImpl(long peer);

    public int getWhich() {
        return getWhichImpl(getPeer());
    }
    native static int getWhichImpl(long peer);


// Functions
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
    native static void initUIEventImpl(long peer
        , String type
        , boolean canBubble
        , boolean cancelable
        , long view
        , int detail);


}

