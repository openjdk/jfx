/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.events.MouseEvent;

/**
 * MouseState is a snapshot of mouse coordinates and which buttons are pressed.
 * MouseState is used both to store the current state of mouse input and to
 * describe changes to that state.
 */
class MouseState {

    static final int WHEEL_NONE = 0;
    static final int WHEEL_UP = 1;
    static final int WHEEL_DOWN = -1 ;

    private int x;
    private int y;
    private int wheel;
    private MonocleWindow window;

    private IntSet buttonsPressed = new IntSet();

    int getX() {
        return x;
    }

    void setX(int x) {
        this.x = x;
    }

    int getY() {
        return y;
    }

    void setY(int y) {
        this.y = y;
    }

    int getWheel() {
        return wheel;
    }

    void setWheel(int wheel) {
        this.wheel = wheel;
    }

    void pressButton(int button) {
        buttonsPressed.addInt(button);
    }

    void releaseButton(int button) {
        buttonsPressed.removeInt(button);
    }

    /**
     * Returns the Glass window on which the coordinates of this state are located.
     * @param recalculateCache true if the cached value for the target window
     *                         should be recalculated; false if the cached
     *                         value should be used to determine the result
     *                         of this method.
     * @param fallback if the original window is null, or if no window can
     * be found, return the fallback window
     * @return the MonocleWindow at the top of the stack at the coordinates
     * described by this state object, or the fallback window in case the
     * current window is null or no window can be found for the supplied coordinates.
     */
    MonocleWindow getWindow(boolean recalculateCache, MonocleWindow fallback) {
        if (recalculateCache) {
            window = (MonocleWindow)
                    MonocleWindowManager.getInstance().getWindowForLocation(x, y);
        }
        if (window == null) {
            window = fallback;
        }
        return window;
    }

    /** Returns the Glass button ID used for this state. */
    int getButton() {
        return buttonsPressed.isEmpty()
               ? MouseEvent.BUTTON_NONE
               : buttonsPressed.get(0);
    }

    /** Returns the Glass event modifiers for this state */
    int getModifiers() {
        int modifiers = KeyEvent.MODIFIER_NONE;
        for (int i = 0; i < buttonsPressed.size(); i++) {
            switch(buttonsPressed.get(i)) {
                case MouseEvent.BUTTON_LEFT:
                    modifiers |= KeyEvent.MODIFIER_BUTTON_PRIMARY;
                    break;
                case MouseEvent.BUTTON_OTHER:
                    modifiers |= KeyEvent.MODIFIER_BUTTON_MIDDLE;
                    break;
                case MouseEvent.BUTTON_RIGHT:
                    modifiers |= KeyEvent.MODIFIER_BUTTON_SECONDARY;
                    break;
                case MouseEvent.BUTTON_BACK:
                    modifiers |= KeyEvent.MODIFIER_BUTTON_BACK;
                    break;
                case MouseEvent.BUTTON_FORWARD:
                    modifiers |= KeyEvent.MODIFIER_BUTTON_FORWARD;
                    break;
            }
        }
        return modifiers;
    }

    /** Copies the contents of this state object to another.
     *
     * @param target The MouseState to which to copy this state's data.
     */
    void copyTo(MouseState target) {
        target.x = x;
        target.y = y;
        target.wheel = wheel;
        buttonsPressed.copyTo(target.buttonsPressed);
        target.window = window;
    }

    IntSet getButtonsPressed() {
        return buttonsPressed;
    }

    public String toString() {
        return "MouseState[x="
                + x + ",y=" + y
                + ",wheel=" + wheel
                + ",buttonsPressed=" + buttonsPressed + "]";
    }

    /** Finds out whether two non-null states are identical in everything but
     * their coordinates
     *
     * @param ms the MouseState to compare to
     */
    boolean canBeFoldedWith(MouseState ms) {
        return ms.buttonsPressed.equals(buttonsPressed) && ms.wheel == wheel;
    }

}
