/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.embed.swing;

import com.sun.javafx.embed.AbstractEvents;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javafx.event.EventType;
import javafx.scene.input.ScrollEvent;

/**
 * A utility class to translate event types and data between embedded
 * application and Swing.
 */
public class SwingEvents {

    public static int mouseIDToEmbedMouseType(int id) {
        switch (id) {
            case MouseEvent.MOUSE_PRESSED:
                return AbstractEvents.MOUSEEVENT_PRESSED;
            case MouseEvent.MOUSE_RELEASED:
                return AbstractEvents.MOUSEEVENT_RELEASED;
            case MouseEvent.MOUSE_CLICKED:
                return AbstractEvents.MOUSEEVENT_CLICKED;
            case MouseEvent.MOUSE_MOVED:
                return AbstractEvents.MOUSEEVENT_MOVED;
            case MouseEvent.MOUSE_DRAGGED:
                return AbstractEvents.MOUSEEVENT_DRAGGED;
            case MouseEvent.MOUSE_ENTERED:
                return AbstractEvents.MOUSEEVENT_ENTERED;
            case MouseEvent.MOUSE_EXITED:
                return AbstractEvents.MOUSEEVENT_EXITED;
        }
        return 0;
    }

    public static int mouseButtonToEmbedMouseButton(int button, int extModifiers) {
        int abstractButton = AbstractEvents.MOUSEEVENT_NONE_BUTTON;
        switch (button) {
            case MouseEvent.BUTTON1:
                abstractButton = AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
                break;
            case MouseEvent.BUTTON2:
                abstractButton = AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
                break;
            case MouseEvent.BUTTON3:
                abstractButton = AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
                break;
            case 4:
                abstractButton = AbstractEvents.MOUSEEVENT_BACK_BUTTON;
                break;
            case 5:
                abstractButton = AbstractEvents.MOUSEEVENT_FORWARD_BUTTON;
                break;
            default:
                break;
        }
        if (abstractButton == AbstractEvents.MOUSEEVENT_NONE_BUTTON) {
            // Fix for RT-15457: we should report mouse buttons for mouse drags
            if ((extModifiers & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
                abstractButton = AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
            } else if ((extModifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
                abstractButton = AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
            } else if ((extModifiers & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
                abstractButton = AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
            } else if ((extModifiers & MouseEvent.getMaskForButton(4)) != 0) {
                abstractButton = AbstractEvents.MOUSEEVENT_BACK_BUTTON;
            } else if ((extModifiers & MouseEvent.getMaskForButton(5)) != 0) {
                abstractButton = AbstractEvents.MOUSEEVENT_FORWARD_BUTTON;
            }
        }
        return abstractButton;
    }

    public static int getWheelRotation(MouseEvent e) {
        if (e instanceof MouseWheelEvent) {
            return ((MouseWheelEvent)e).getWheelRotation();
        }
        return 0;
    }

    public static int keyIDToEmbedKeyType(int id) {
        switch (id) {
            case KeyEvent.KEY_PRESSED:
                return AbstractEvents.KEYEVENT_PRESSED;
            case KeyEvent.KEY_RELEASED:
                return AbstractEvents.KEYEVENT_RELEASED;
            case KeyEvent.KEY_TYPED:
                return AbstractEvents.KEYEVENT_TYPED;
        }
        return 0;
    }

    public static int keyModifiersToEmbedKeyModifiers(int extModifiers) {
        int embedModifiers = 0;
        if ((extModifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            embedModifiers |= AbstractEvents.MODIFIER_SHIFT;
        }
        if ((extModifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            embedModifiers |= AbstractEvents.MODIFIER_CONTROL;
        }
        if ((extModifiers & InputEvent.ALT_DOWN_MASK) != 0) {
            embedModifiers |= AbstractEvents.MODIFIER_ALT;
        }
        if ((extModifiers & InputEvent.META_DOWN_MASK) != 0) {
            embedModifiers |= AbstractEvents.MODIFIER_META;
        }
        return embedModifiers;
    }

    public static char keyCharToEmbedKeyChar(char ch) {
        // Convert Swing LF character to Fx CR character.
        return ch == '\n' ? '\r' : ch;
    }

    // FX -> Swing conversion methods

    public static int fxMouseEventTypeToMouseID(javafx.scene.input.MouseEvent event) {
        EventType<?> type = event.getEventType();
        if (type == javafx.scene.input.MouseEvent.MOUSE_MOVED) {
            return MouseEvent.MOUSE_MOVED;
        }
        if (type == javafx.scene.input.MouseEvent.MOUSE_PRESSED) {
            return MouseEvent.MOUSE_PRESSED;
        }
        if (type == javafx.scene.input.MouseEvent.MOUSE_RELEASED) {
            return MouseEvent.MOUSE_RELEASED;
        }
        if (type == javafx.scene.input.MouseEvent.MOUSE_CLICKED) {
            return MouseEvent.MOUSE_CLICKED;
        }
        if (type == javafx.scene.input.MouseEvent.MOUSE_ENTERED) {
            return MouseEvent.MOUSE_ENTERED;
        }
        if (type == javafx.scene.input.MouseEvent.MOUSE_EXITED) {
            return MouseEvent.MOUSE_EXITED;
        }
        if (type == javafx.scene.input.MouseEvent.MOUSE_DRAGGED) {
            return MouseEvent.MOUSE_DRAGGED;
        }
        if (type == javafx.scene.input.MouseEvent.DRAG_DETECTED) {
            return -1;
        }
        throw new RuntimeException("Unknown MouseEvent type: " + type);
    }

    public static int fxMouseModsToMouseMods(javafx.scene.input.MouseEvent event) {
        int mods = 0;
        if (event.isAltDown()) {
            mods |= InputEvent.ALT_DOWN_MASK;
        }
        if (event.isControlDown()) {
            mods |= InputEvent.CTRL_DOWN_MASK;
        }
        if (event.isMetaDown()) {
            mods |= InputEvent.META_DOWN_MASK;
        }
        if (event.isShiftDown()) {
            mods |= InputEvent.SHIFT_DOWN_MASK;
        }
        if (event.isPrimaryButtonDown()) {
            mods |= MouseEvent.BUTTON1_DOWN_MASK;
        }
        if (event.isSecondaryButtonDown()) {
            mods |= MouseEvent.BUTTON3_DOWN_MASK;
        }
        if (event.isMiddleButtonDown()) {
            mods |= MouseEvent.BUTTON2_DOWN_MASK;
        }
        if (event.isBackButtonDown()) {
            mods |= MouseEvent.getMaskForButton(4);
        }
        if (event.isForwardButtonDown()) {
            mods |= MouseEvent.getMaskForButton(5);
        }
        return mods;
    }

    public static int fxMouseButtonToMouseButton(javafx.scene.input.MouseEvent event) {
        switch (event.getButton()) {
            case PRIMARY:
                return MouseEvent.BUTTON1;
            case SECONDARY:
                return MouseEvent.BUTTON3;
            case MIDDLE:
                return MouseEvent.BUTTON2;
            case BACK:
                return 4;
            case FORWARD:
                return 5;
        }
        return 0;
    }

    public static int fxKeyEventTypeToKeyID(javafx.scene.input.KeyEvent event) {
        EventType<?> eventType = event.getEventType();
        if (eventType == javafx.scene.input.KeyEvent.KEY_PRESSED) {
            return KeyEvent.KEY_PRESSED;
        }
        if (eventType == javafx.scene.input.KeyEvent.KEY_RELEASED) {
            return KeyEvent.KEY_RELEASED;
        }
        if (eventType == javafx.scene.input.KeyEvent.KEY_TYPED) {
            return KeyEvent.KEY_TYPED;
        }
        throw new RuntimeException("Unknown KeyEvent type: " + eventType);
    }

    public static int fxKeyModsToKeyMods(javafx.scene.input.KeyEvent event) {
        int mods = 0;
        if (event.isAltDown()) {
            mods |= InputEvent.ALT_DOWN_MASK;
        }
        if (event.isControlDown()) {
            mods |= InputEvent.CTRL_DOWN_MASK;
        }
        if (event.isMetaDown()) {
            mods |= InputEvent.META_DOWN_MASK;
        }
        if (event.isShiftDown()) {
            mods |= InputEvent.SHIFT_DOWN_MASK;
        }
        return mods;
    }

    public static int fxScrollModsToMouseWheelMods(ScrollEvent event) {
        int mods = 0;
        if (event.isAltDown()) {
            mods |= InputEvent.ALT_DOWN_MASK;
        }
        if (event.isControlDown()) {
            mods |= InputEvent.CTRL_DOWN_MASK;
        }
        if (event.isMetaDown()) {
            mods |= InputEvent.META_DOWN_MASK;
        }
        if (event.isShiftDown()) {
            mods |= InputEvent.SHIFT_DOWN_MASK;
        }
        return mods;
    }
}
