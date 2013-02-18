/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.embed.swing;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import com.sun.javafx.embed.AbstractEvents;

/**
 * An utility class to translate cursor types between embedded
 * application and Swing.
 */
class SwingEvents {

    static int mouseIDToEmbedMouseType(int id) {
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
            case MouseWheelEvent.MOUSE_WHEEL:
                return AbstractEvents.MOUSEEVENT_WHEEL;
        }
        return 0;
    }

    static int mouseButtonToEmbedMouseButton(int button, int extModifiers) {
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
            default:
                break;
        }
        // Fix for RT-15457: we should report mouse buttons for mouse drags
        if ((extModifiers & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            abstractButton = AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
        } else if ((extModifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
            abstractButton = AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
        } else if ((extModifiers & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
            abstractButton = AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
        }
        return abstractButton;
    }

    static int getWheelRotation(MouseEvent e) {
        if (e instanceof MouseWheelEvent) {
            return ((MouseWheelEvent)e).getWheelRotation();
        }
        return 0;
    }
    
    static int keyIDToEmbedKeyType(int id) {
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

    static int keyModifiersToEmbedKeyModifiers(int extModifiers) {
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
}
