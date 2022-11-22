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

import org.w3c.dom.Node;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MouseEvent;
import org.w3c.dom.views.AbstractView;

public class MouseEventImpl extends UIEventImpl implements MouseEvent {
    MouseEventImpl(long peer) {
        super(peer);
    }

    static MouseEvent getImpl(long peer) {
        return (MouseEvent)create(peer);
    }


// Attributes
    public int getScreenX() {
        return getScreenXImpl(getPeer());
    }
    native static int getScreenXImpl(long peer);

    public int getScreenY() {
        return getScreenYImpl(getPeer());
    }
    native static int getScreenYImpl(long peer);

    public int getClientX() {
        return getClientXImpl(getPeer());
    }
    native static int getClientXImpl(long peer);

    public int getClientY() {
        return getClientYImpl(getPeer());
    }
    native static int getClientYImpl(long peer);

    public boolean getCtrlKey() {
        return getCtrlKeyImpl(getPeer());
    }
    native static boolean getCtrlKeyImpl(long peer);

    public boolean getShiftKey() {
        return getShiftKeyImpl(getPeer());
    }
    native static boolean getShiftKeyImpl(long peer);

    public boolean getAltKey() {
        return getAltKeyImpl(getPeer());
    }
    native static boolean getAltKeyImpl(long peer);

    public boolean getMetaKey() {
        return getMetaKeyImpl(getPeer());
    }
    native static boolean getMetaKeyImpl(long peer);

    public short getButton() {
        return getButtonImpl(getPeer());
    }
    native static short getButtonImpl(long peer);

    public EventTarget getRelatedTarget() {
        return (EventTarget)NodeImpl.getImpl(getRelatedTargetImpl(getPeer()));
    }
    native static long getRelatedTargetImpl(long peer);

    public int getOffsetX() {
        return getOffsetXImpl(getPeer());
    }
    native static int getOffsetXImpl(long peer);

    public int getOffsetY() {
        return getOffsetYImpl(getPeer());
    }
    native static int getOffsetYImpl(long peer);

    public int getX() {
        return getXImpl(getPeer());
    }
    native static int getXImpl(long peer);

    public int getY() {
        return getYImpl(getPeer());
    }
    native static int getYImpl(long peer);

    public Node getFromElement() {
        return NodeImpl.getImpl(getFromElementImpl(getPeer()));
    }
    native static long getFromElementImpl(long peer);

    public Node getToElement() {
        return NodeImpl.getImpl(getToElementImpl(getPeer()));
    }
    native static long getToElementImpl(long peer);


// Functions
    public void initMouseEvent(String type
        , boolean canBubble
        , boolean cancelable
        , AbstractView view
        , int detail
        , int screenX
        , int screenY
        , int clientX
        , int clientY
        , boolean ctrlKey
        , boolean altKey
        , boolean shiftKey
        , boolean metaKey
        , short button
        , EventTarget relatedTarget)
    {
        initMouseEventImpl(getPeer()
            , type
            , canBubble
            , cancelable
            , DOMWindowImpl.getPeer(view)
            , detail
            , screenX
            , screenY
            , clientX
            , clientY
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey
            , button
            , NodeImpl.getPeer((NodeImpl)relatedTarget));
    }
    native static void initMouseEventImpl(long peer
        , String type
        , boolean canBubble
        , boolean cancelable
        , long view
        , int detail
        , int screenX
        , int screenY
        , int clientX
        , int clientY
        , boolean ctrlKey
        , boolean altKey
        , boolean shiftKey
        , boolean metaKey
        , short button
        , long relatedTarget);


}

