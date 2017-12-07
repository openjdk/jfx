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

import org.w3c.dom.views.AbstractView;

public class WheelEventImpl extends MouseEventImpl {
    WheelEventImpl(long peer) {
        super(peer);
    }

    static WheelEventImpl getImpl(long peer) {
        return (WheelEventImpl)create(peer);
    }


// Constants
    public static final int DOM_DELTA_PIXEL = 0x00;
    public static final int DOM_DELTA_LINE = 0x01;
    public static final int DOM_DELTA_PAGE = 0x02;

// Attributes
    public double getDeltaX() {
        return getDeltaXImpl(getPeer());
    }
    native static double getDeltaXImpl(long peer);

    public double getDeltaY() {
        return getDeltaYImpl(getPeer());
    }
    native static double getDeltaYImpl(long peer);

    public double getDeltaZ() {
        return getDeltaZImpl(getPeer());
    }
    native static double getDeltaZImpl(long peer);

    public int getDeltaMode() {
        return getDeltaModeImpl(getPeer());
    }
    native static int getDeltaModeImpl(long peer);

    public int getWheelDeltaX() {
        return getWheelDeltaXImpl(getPeer());
    }
    native static int getWheelDeltaXImpl(long peer);

    public int getWheelDeltaY() {
        return getWheelDeltaYImpl(getPeer());
    }
    native static int getWheelDeltaYImpl(long peer);

    public int getWheelDelta() {
        return getWheelDeltaImpl(getPeer());
    }
    native static int getWheelDeltaImpl(long peer);

    public boolean getWebkitDirectionInvertedFromDevice() {
        return getWebkitDirectionInvertedFromDeviceImpl(getPeer());
    }
    native static boolean getWebkitDirectionInvertedFromDeviceImpl(long peer);


// Functions
    public void initWheelEvent(int wheelDeltaX
        , int wheelDeltaY
        , AbstractView view
        , int screenX
        , int screenY
        , int clientX
        , int clientY
        , boolean ctrlKey
        , boolean altKey
        , boolean shiftKey
        , boolean metaKey)
    {
        initWheelEventImpl(getPeer()
            , wheelDeltaX
            , wheelDeltaY
            , DOMWindowImpl.getPeer(view)
            , screenX
            , screenY
            , clientX
            , clientY
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey);
    }
    native static void initWheelEventImpl(long peer
        , int wheelDeltaX
        , int wheelDeltaY
        , long view
        , int screenX
        , int screenY
        , int clientX
        , int clientY
        , boolean ctrlKey
        , boolean altKey
        , boolean shiftKey
        , boolean metaKey);


}

