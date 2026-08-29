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
    @Override
    public int getScreenX() {
        return getScreenXImpl(getPeer());
    }
    static int getScreenXImpl(long peer) {
        return MouseEventNative.getScreenX(peer);
    }

    @Override
    public int getScreenY() {
        return getScreenYImpl(getPeer());
    }
    static int getScreenYImpl(long peer) {
        return MouseEventNative.getScreenY(peer);
    }

    @Override
    public int getClientX() {
        return getClientXImpl(getPeer());
    }
    static int getClientXImpl(long peer) {
        return MouseEventNative.getClientX(peer);
    }

    @Override
    public int getClientY() {
        return getClientYImpl(getPeer());
    }
    static int getClientYImpl(long peer) {
        return MouseEventNative.getClientY(peer);
    }

    @Override
    public boolean getCtrlKey() {
        return getCtrlKeyImpl(getPeer());
    }
    static boolean getCtrlKeyImpl(long peer) {
        return MouseEventNative.getCtrlKey(peer);
    }

    @Override
    public boolean getShiftKey() {
        return getShiftKeyImpl(getPeer());
    }
    static boolean getShiftKeyImpl(long peer) {
        return MouseEventNative.getShiftKey(peer);
    }

    @Override
    public boolean getAltKey() {
        return getAltKeyImpl(getPeer());
    }
    static boolean getAltKeyImpl(long peer) {
        return MouseEventNative.getAltKey(peer);
    }

    @Override
    public boolean getMetaKey() {
        return getMetaKeyImpl(getPeer());
    }
    static boolean getMetaKeyImpl(long peer) {
        return MouseEventNative.getMetaKey(peer);
    }

    @Override
    public short getButton() {
        return getButtonImpl(getPeer());
    }
    static short getButtonImpl(long peer) {
        return MouseEventNative.getButton(peer);
    }

    @Override
    public EventTarget getRelatedTarget() {
        return (EventTarget)NodeImpl.getImpl(getRelatedTargetImpl(getPeer()));
    }
    static long getRelatedTargetImpl(long peer) {
        return MouseEventNative.getRelatedTarget(peer);
    }

    public int getOffsetX() {
        return getOffsetXImpl(getPeer());
    }
    static int getOffsetXImpl(long peer) {
        return MouseEventNative.getOffsetX(peer);
    }

    public int getOffsetY() {
        return getOffsetYImpl(getPeer());
    }
    static int getOffsetYImpl(long peer) {
        return MouseEventNative.getOffsetY(peer);
    }

    public int getX() {
        return getXImpl(getPeer());
    }
    static int getXImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.MouseEventImpl.getXImpl: no wkj_* function exists for it in"
                + " any jfxwebkit build");
    }

    public int getY() {
        return getYImpl(getPeer());
    }
    static int getYImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.MouseEventImpl.getYImpl: no wkj_* function exists for it in"
                + " any jfxwebkit build");
    }

    public Node getFromElement() {
        return NodeImpl.getImpl(getFromElementImpl(getPeer()));
    }
    static long getFromElementImpl(long peer) {
        return MouseEventNative.getFromElement(peer);
    }

    public Node getToElement() {
        return NodeImpl.getImpl(getToElementImpl(getPeer()));
    }
    static long getToElementImpl(long peer) {
        return MouseEventNative.getToElement(peer);
    }


// Functions
    @Override
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
    static void initMouseEventImpl(long peer
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
        , long relatedTarget) {
        MouseEventNative.initMouseEvent(peer, type, canBubble, cancelable, view, detail, screenX, screenY, clientX,
                clientY, ctrlKey, altKey, shiftKey, metaKey, button, relatedTarget);
    }


}

