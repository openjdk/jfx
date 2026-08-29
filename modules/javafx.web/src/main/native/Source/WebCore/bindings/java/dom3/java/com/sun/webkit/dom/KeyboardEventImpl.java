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

import org.w3c.dom.views.AbstractView;

public class KeyboardEventImpl extends UIEventImpl {
    KeyboardEventImpl(long peer) {
        super(peer);
    }

    static KeyboardEventImpl getImpl(long peer) {
        return (KeyboardEventImpl)create(peer);
    }


// Constants
    public static final int KEY_LOCATION_STANDARD = 0x00;
    public static final int KEY_LOCATION_LEFT = 0x01;
    public static final int KEY_LOCATION_RIGHT = 0x02;
    public static final int KEY_LOCATION_NUMPAD = 0x03;

// Attributes
    public String getKeyIdentifier() {
        return getKeyIdentifierImpl(getPeer());
    }
    static String getKeyIdentifierImpl(long peer) {
        return KeyboardEventNative.getKeyIdentifier(peer);
    }

    public int getLocation() {
        return getLocationImpl(getPeer());
    }
    static int getLocationImpl(long peer) {
        return KeyboardEventNative.getLocation(peer);
    }

    public int getKeyLocation() {
        return getKeyLocationImpl(getPeer());
    }
    static int getKeyLocationImpl(long peer) {
        return KeyboardEventNative.getKeyLocation(peer);
    }

    public boolean getCtrlKey() {
        return getCtrlKeyImpl(getPeer());
    }
    static boolean getCtrlKeyImpl(long peer) {
        return KeyboardEventNative.getCtrlKey(peer);
    }

    public boolean getShiftKey() {
        return getShiftKeyImpl(getPeer());
    }
    static boolean getShiftKeyImpl(long peer) {
        return KeyboardEventNative.getShiftKey(peer);
    }

    public boolean getAltKey() {
        return getAltKeyImpl(getPeer());
    }
    static boolean getAltKeyImpl(long peer) {
        return KeyboardEventNative.getAltKey(peer);
    }

    public boolean getMetaKey() {
        return getMetaKeyImpl(getPeer());
    }
    static boolean getMetaKeyImpl(long peer) {
        return KeyboardEventNative.getMetaKey(peer);
    }

    public boolean getAltGraphKey() {
        return getAltGraphKeyImpl(getPeer());
    }
    static boolean getAltGraphKeyImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.KeyboardEventImpl.getAltGraphKeyImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    @Override
    public int getKeyCode() {
        return getKeyCodeImpl(getPeer());
    }
    static int getKeyCodeImpl(long peer) {
        return KeyboardEventNative.getKeyCode(peer);
    }

    @Override
    public int getCharCode() {
        return getCharCodeImpl(getPeer());
    }
    static int getCharCodeImpl(long peer) {
        return KeyboardEventNative.getCharCode(peer);
    }


// Functions
    public boolean getModifierState(String keyIdentifierArg)
    {
        return getModifierStateImpl(getPeer()
            , keyIdentifierArg);
    }
    static boolean getModifierStateImpl(long peer
        , String keyIdentifierArg) {
        return KeyboardEventNative.getModifierState(peer, keyIdentifierArg);
    }


    public void initKeyboardEvent(String type
        , boolean canBubble
        , boolean cancelable
        , AbstractView view
        , String keyIdentifier
        , int location
        , boolean ctrlKey
        , boolean altKey
        , boolean shiftKey
        , boolean metaKey
        , boolean altGraphKey)
    {
        initKeyboardEventImpl(getPeer()
            , type
            , canBubble
            , cancelable
            , DOMWindowImpl.getPeer(view)
            , keyIdentifier
            , location
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey
            , altGraphKey);
    }
    static void initKeyboardEventImpl(long peer
        , String type
        , boolean canBubble
        , boolean cancelable
        , long view
        , String keyIdentifier
        , int location
        , boolean ctrlKey
        , boolean altKey
        , boolean shiftKey
        , boolean metaKey
        , boolean altGraphKey) {
        KeyboardEventNative.initKeyboardEvent(peer, type, canBubble, cancelable, view, keyIdentifier, location, ctrlKey,
                altKey, shiftKey, metaKey, altGraphKey);
    }


    public void initKeyboardEventEx(String type
        , boolean canBubble
        , boolean cancelable
        , AbstractView view
        , String keyIdentifier
        , int location
        , boolean ctrlKey
        , boolean altKey
        , boolean shiftKey
        , boolean metaKey)
    {
        initKeyboardEventExImpl(getPeer()
            , type
            , canBubble
            , cancelable
            , DOMWindowImpl.getPeer(view)
            , keyIdentifier
            , location
            , ctrlKey
            , altKey
            , shiftKey
            , metaKey);
    }
    static void initKeyboardEventExImpl(long peer
        , String type
        , boolean canBubble
        , boolean cancelable
        , long view
        , String keyIdentifier
        , int location
        , boolean ctrlKey
        , boolean altKey
        , boolean shiftKey
        , boolean metaKey) {
        KeyboardEventNative.initKeyboardEventEx(peer, type, canBubble, cancelable, view, keyIdentifier, location,
                ctrlKey, altKey, shiftKey, metaKey);
    }


}

