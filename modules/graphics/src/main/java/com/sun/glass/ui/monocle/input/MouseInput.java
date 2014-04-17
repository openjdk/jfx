/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.input;

import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.monocle.MonocleSettings;
import com.sun.glass.ui.monocle.MonocleTrace;
import com.sun.glass.ui.monocle.MonocleView;
import com.sun.glass.ui.monocle.MonocleWindow;
import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.glass.ui.monocle.NativeScreen;
import com.sun.glass.ui.monocle.util.IntSet;

/**
 * Processes mouse input events based on changes to mouse state. Not
 * thread-safe.
 */
public class MouseInput {
    private static MouseInput instance = new MouseInput();

    private MouseState state = new MouseState();
    private IntSet buttons = new IntSet();

    public static MouseInput getInstance() {
        return instance;
    }

    public void getState(MouseState result) {
        state.copyTo(result);
    }

    public void setState(MouseState newState, boolean synthesized) {
        if (MonocleSettings.settings.traceEvents) {
            MonocleTrace.traceEvent("Set %s", newState);
        }
        // Restrict new state coordinates to screen bounds
        NativeScreen screen = NativePlatformFactory.getNativePlatform().getScreen();
        int x = Math.min(newState.getX(), screen.getWidth() - 1);
        int y = Math.min(newState.getY(), screen.getHeight() - 1);
        x = Math.max(x, 0);
        y = Math.max(y, 0);
        newState.setX(x);
        newState.setY(y);
        // Get the cached window for the old state and compute the window for
        // the new state
        MonocleWindow oldWindow = state.getWindow(false);
        MonocleWindow window = newState.getWindow(true);
        MonocleView view = (window == null) ? null : (MonocleView) window.getView();
        // send exit event
        if (oldWindow != window && oldWindow != null) {
            MonocleView oldView = (MonocleView) oldWindow.getView();
            if (oldView != null) {
                // send exit event
                int modifiers = state.getModifiers(); // TODO: include key modifiers
                int button = state.getButton();
                boolean isPopupTrigger = false; // TODO
                int oldX = state.getX();
                int oldY = state.getY();
                int oldRelX = oldX - oldWindow.getX();
                int oldRelY = oldY - oldWindow.getY();
                oldView.notifyMouse(MouseEvent.EXIT, button,
                                    oldRelX, oldRelY, oldX, oldY,
                                    modifiers, isPopupTrigger, synthesized);
            }
        }
        boolean newAbsoluteLocation = state.getX() != x || state.getY() != y;
        if (newAbsoluteLocation) {
            NativePlatformFactory.getNativePlatform()
                    .getCursor().setLocation(x, y);
        }
        if (view == null) {
            newState.copyTo(state);
            return;
        }
        int relX = x - window.getX();
        int relY = y - window.getY();
        // send enter event
        if (oldWindow != window && view != null) {
            int modifiers = state.getModifiers(); // TODO: include key modifiers
            int button = state.getButton();
            boolean isPopupTrigger = false; // TODO
            view.notifyMouse(MouseEvent.ENTER, button,
                             relX, relY, x, y,
                             modifiers, isPopupTrigger, synthesized);
        }
        // send motion events
        if (oldWindow != window | newAbsoluteLocation) {
            boolean isDrag = !state.getButtonsPressed().isEmpty();
            int eventType = isDrag ? MouseEvent.DRAG : MouseEvent.MOVE;
            int modifiers = state.getModifiers(); // TODO: include key modifiers
            int button = state.getButton();
            boolean isPopupTrigger = false; // TODO
            view.notifyMouse(eventType, button,
                             relX, relY, x, y,
                             modifiers, isPopupTrigger, synthesized);
        }
        // send press events
        newState.getButtonsPressed().difference(buttons, state.getButtonsPressed());
        if (!buttons.isEmpty()) {
            MouseState pressState = new MouseState();
            state.copyTo(pressState);
            for (int i = 0; i < buttons.size(); i++) {
                int button = buttons.get(i);
                pressState.pressButton(button);
                // send press event
                boolean isPopupTrigger = false; // TODO
                view.notifyMouse(MouseEvent.DOWN, button,
                                 relX, relY, x, y,
                                 pressState.getModifiers(), isPopupTrigger,
                                 synthesized);
            }
        }
        buttons.clear();
        // send release events
        state.getButtonsPressed().difference(buttons,
                                             newState.getButtonsPressed());
        if (!buttons.isEmpty()) {
            MouseState releaseState = new MouseState();
            state.copyTo(releaseState);
            for (int i = 0; i < buttons.size(); i++) {
                int button = buttons.get(i);
                releaseState.releaseButton(button);
                // send release event
                boolean isPopupTrigger = false; // TODO
                view.notifyMouse(MouseEvent.UP, button,
                                 relX, relY, x, y,
                                 releaseState.getModifiers(), isPopupTrigger,
                                 synthesized);

            }
        }
        buttons.clear();
        // send scroll events
        if (newState.getWheel() != state.getWheel()) {
            double dY;
            switch (newState.getWheel()) {
                case MouseState.WHEEL_DOWN: dY = -1.0; break;
                case MouseState.WHEEL_UP: dY = 1.0; break;
                default: dY = 0.0; break;
            }
            if (dY != 0.0) {
                view.notifyScroll(relX, relY, x, y, 0.0, dY,
                                  newState.getModifiers(), 1, 0, 0, 0, 1.0, 1.0);
            }
        }
        newState.copyTo(state);
    }

}
