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

import com.sun.glass.events.KeyEvent;
import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.monocle.MonocleWindow;
import com.sun.glass.ui.monocle.MonocleWindowManager;
import com.sun.glass.ui.monocle.util.IntSet;

public class MouseState {

    private int x;
    private int y;
    private MonocleWindow window;

    private IntSet buttonsPressed = new IntSet();

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void pressButton(int button) {
        buttonsPressed.addInt(button);
    }

    public void releaseButton(int button) {
        buttonsPressed.removeInt(button);
    }

    /** Returns the Glass window on which this event state is located . */
    MonocleWindow getWindow(boolean recalculateCache) {
        if (window == null || recalculateCache) {
            window = (MonocleWindow)
                    MonocleWindowManager.getInstance().getWindowForLocation(x, y);
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
            }
        }
        return modifiers;
    }

    void copyTo(MouseState target) {
        target.x = x;
        target.y = y;
        buttonsPressed.copyTo(target.buttonsPressed);
    }

    IntSet getButtonsPressed() {
        return buttonsPressed;
    }

    public String toString() {
        return "MouseState[x="
                + x + ",y=" + y
                + ",buttonsPressed=" + buttonsPressed + "]";
    }
}
