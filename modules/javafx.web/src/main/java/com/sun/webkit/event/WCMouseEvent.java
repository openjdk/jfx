/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.event;

import java.lang.annotation.Native;

public final class WCMouseEvent {

    // id
    @Native public final static int MOUSE_PRESSED = 0;
    @Native public final static int MOUSE_RELEASED = 1;
    @Native public final static int MOUSE_MOVED = 2;
    @Native public final static int MOUSE_DRAGGED = 3;
    @Native public final static int MOUSE_WHEEL = 4;

    // button
    @Native public final static int NOBUTTON = 0;
    @Native public final static int BUTTON1 = 1; // Left Button
    @Native public final static int BUTTON2 = 2; // Middle Button
    @Native public final static int BUTTON3 = 4; // Right Button

    private final int id;
    private final long when;

    private final int button;
    private final int buttonMask;
    private final int clickCount;

    private final int x;
    private final int y;
    private final int screenX;
    private final int screenY;

    private final boolean shift;
    private final boolean control;
    private final boolean alt;
    private final boolean meta;

    private final boolean popupTrigger;

    public WCMouseEvent(int id, int button, int clickCount, int x, int y, int screenX, int screenY,
                        long when, boolean shift, boolean control, boolean alt, boolean meta, boolean popupTrigger, int buttonMask) {
        this.id = id;
        this.button = button;
        this.clickCount = clickCount;
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.when = when;
        this.shift = shift;
        this.control = control;
        this.alt = alt;
        this.meta = meta;
        this.popupTrigger = popupTrigger;
        this.buttonMask = buttonMask;
    }

    public WCMouseEvent(int id, int button, int clickCount, int x, int y, int screenX, int screenY,
                        long when, boolean shift, boolean control, boolean alt, boolean meta, boolean popupTrigger) {
         this(id, button, clickCount, x, y, screenX, screenY, when, shift, control, alt, meta, popupTrigger, 0);
     }

    public int getID() { return id; }
    public long getWhen() { return when; }

    public int getButton() { return button; }
    public int getClickCount() { return clickCount; }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getScreenX() { return screenX; }
    public int getScreenY() { return screenY; }

    public boolean isShiftDown() { return shift; }
    public boolean isControlDown() { return control; }
    public boolean isAltDown() { return alt; }
    public boolean isMetaDown() { return meta; }

    public boolean isPopupTrigger() { return popupTrigger; }

    public int getButtonMask() { return buttonMask; }
}
