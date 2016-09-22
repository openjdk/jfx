/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.embed;

import javafx.event.EventType;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.ZoomEvent;

import com.sun.javafx.tk.FocusCause;

/**
 * An utility class to translate input events between embedded
 * application and FX.
 *
 */
public class AbstractEvents {

    public final static int MOUSEEVENT_PRESSED = 0;
    public final static int MOUSEEVENT_RELEASED = 1;
    public final static int MOUSEEVENT_CLICKED = 2;
    public final static int MOUSEEVENT_ENTERED = 3;
    public final static int MOUSEEVENT_EXITED = 4;
    public final static int MOUSEEVENT_MOVED = 5;
    public final static int MOUSEEVENT_DRAGGED = 6;
    public final static int MOUSEEVENT_VERTICAL_WHEEL = 7;
    public final static int MOUSEEVENT_HORIZONTAL_WHEEL = 8;

    public final static int MOUSEEVENT_NONE_BUTTON = 0;
    public final static int MOUSEEVENT_PRIMARY_BUTTON = 1;
    public final static int MOUSEEVENT_SECONDARY_BUTTON = 2;
    public final static int MOUSEEVENT_MIDDLE_BUTTON = 4;

    public final static int KEYEVENT_PRESSED = 0;
    public final static int KEYEVENT_RELEASED = 1;
    public final static int KEYEVENT_TYPED = 2;

    public final static int ZOOMEVENT_STARTED = 0;
    public final static int ZOOMEVENT_ZOOM = 1;
    public final static int ZOOMEVENT_FINISHED = 2;

    public final static int ROTATEEVENT_STARTED = 0;
    public final static int ROTATEEVENT_ROTATE = 1;
    public final static int ROTATEEVENT_FINISHED = 2;

    public final static int SCROLLEVENT_STARTED = 0;
    public final static int SCROLLEVENT_SCROLL = 1;
    public final static int SCROLLEVENT_FINISHED = 2;

    public final static int SWIPEEVENT_DOWN = 0;
    public final static int SWIPEEVENT_UP = 1;
    public final static int SWIPEEVENT_LEFT = 2;
    public final static int SWIPEEVENT_RIGHT = 3;

    public final static int FOCUSEVENT_ACTIVATED = 0;
    public final static int FOCUSEVENT_TRAVERSED_FORWARD = 1;
    public final static int FOCUSEVENT_TRAVERSED_BACKWARD = 2;
    public final static int FOCUSEVENT_DEACTIVATED = 3;

    public final static int MODIFIER_SHIFT = 1;
    public final static int MODIFIER_CONTROL = 2;
    public final static int MODIFIER_ALT = 4;
    public final static int MODIFIER_META = 8;

    public static EventType<MouseEvent> mouseIDToFXEventID(int embedMouseID) {
        switch (embedMouseID) {
            case MOUSEEVENT_PRESSED:
                return MouseEvent.MOUSE_PRESSED;
            case MOUSEEVENT_RELEASED:
                return MouseEvent.MOUSE_RELEASED;
            case MOUSEEVENT_CLICKED:
                return MouseEvent.MOUSE_CLICKED;
            case MOUSEEVENT_ENTERED:
                return MouseEvent.MOUSE_ENTERED;
            case MOUSEEVENT_EXITED:
                return MouseEvent.MOUSE_EXITED;
            case MOUSEEVENT_MOVED:
                return MouseEvent.MOUSE_MOVED;
            case MOUSEEVENT_DRAGGED:
                return MouseEvent.MOUSE_DRAGGED;
        }
        // Should never reach here
        return MouseEvent.MOUSE_MOVED;
    }

    public static MouseButton mouseButtonToFXMouseButton(int embedButton) {
        switch (embedButton) {
            case MOUSEEVENT_PRIMARY_BUTTON:
                return MouseButton.PRIMARY;
            case MOUSEEVENT_SECONDARY_BUTTON:
                return MouseButton.SECONDARY;
            case MOUSEEVENT_MIDDLE_BUTTON:
                return MouseButton.MIDDLE;
        }
        // Should never reach here
        return MouseButton.NONE;
    }

    public static EventType<KeyEvent> keyIDToFXEventType(int embedKeyID) {
        switch (embedKeyID) {
            case KEYEVENT_PRESSED:
                return KeyEvent.KEY_PRESSED;
            case KEYEVENT_RELEASED:
                return KeyEvent.KEY_RELEASED;
            case KEYEVENT_TYPED:
                return KeyEvent.KEY_TYPED;
        }
        // Should never reach here
        return KeyEvent.KEY_TYPED;
    }

    public static EventType<ZoomEvent> zoomIDToFXEventType(int zoomID) {
        switch(zoomID) {
            case ZOOMEVENT_STARTED:
                return ZoomEvent.ZOOM_STARTED;
            case ZOOMEVENT_ZOOM:
                return ZoomEvent.ZOOM;
            case ZOOMEVENT_FINISHED:
                return ZoomEvent.ZOOM_FINISHED;
        }
        // Should never reach here
        return ZoomEvent.ZOOM;
    }

    public static EventType<RotateEvent> rotateIDToFXEventType(int rotateID) {
        switch(rotateID) {
            case ROTATEEVENT_STARTED:
                return RotateEvent.ROTATION_STARTED;
            case ROTATEEVENT_ROTATE:
                return RotateEvent.ROTATE;
            case ROTATEEVENT_FINISHED:
                return RotateEvent.ROTATION_FINISHED;
        }
        // Should never reach here
        return RotateEvent.ROTATE;
    }

    public static EventType<SwipeEvent> swipeIDToFXEventType(int swipeID) {
        switch(swipeID) {
            case SWIPEEVENT_UP:
                return SwipeEvent.SWIPE_UP;
            case SWIPEEVENT_DOWN:
                return SwipeEvent.SWIPE_DOWN;
            case SWIPEEVENT_LEFT:
                return SwipeEvent.SWIPE_LEFT;
            case SWIPEEVENT_RIGHT:
                return SwipeEvent.SWIPE_RIGHT;
        }
        // Should never reach here
        return SwipeEvent.SWIPE_DOWN;
    }

    public static EventType<ScrollEvent> scrollIDToFXEventType(int scrollID) {
        switch(scrollID) {
            case SCROLLEVENT_STARTED:
                return ScrollEvent.SCROLL_STARTED;
            case SCROLLEVENT_FINISHED:
                return ScrollEvent.SCROLL_FINISHED;
            case MOUSEEVENT_VERTICAL_WHEEL:
            case MOUSEEVENT_HORIZONTAL_WHEEL:
            case SCROLLEVENT_SCROLL:
                return ScrollEvent.SCROLL;
        }
        // Should never reach here
        return ScrollEvent.SCROLL;
    }

    public static FocusCause focusCauseToPeerFocusCause(int focusCause) {
        switch (focusCause) {
            case FOCUSEVENT_ACTIVATED:
                return FocusCause.ACTIVATED;
            case FOCUSEVENT_TRAVERSED_FORWARD:
                return FocusCause.TRAVERSED_FORWARD;
            case FOCUSEVENT_TRAVERSED_BACKWARD:
                return FocusCause.TRAVERSED_BACKWARD;
            case FOCUSEVENT_DEACTIVATED:
                return FocusCause.DEACTIVATED;
        }
        // Should never reach here
        return FocusCause.ACTIVATED;
    }
}
