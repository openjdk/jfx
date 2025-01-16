/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.events;

import com.sun.glass.ui.WindowControlsOverlay;
import javafx.stage.StageStyle;
import java.lang.annotation.Native;

public class MouseEvent {
    @Native final static public int BUTTON_NONE     = 211;
    @Native final static public int BUTTON_LEFT     = 212;
    @Native final static public int BUTTON_RIGHT    = 213;
    @Native final static public int BUTTON_OTHER    = 214;
    @Native final static public int BUTTON_BACK     = 215;
    @Native final static public int BUTTON_FORWARD  = 216;

    @Native final static public int DOWN            = 221;
    @Native final static public int UP              = 222;
    @Native final static public int DRAG            = 223;
    @Native final static public int MOVE            = 224;
    @Native final static public int ENTER           = 225;
    @Native final static public int EXIT            = 226;
    @Native final static public int CLICK           = 227; // synthetic

    /**
     * Artificial WHEEL event type.
     * This kind of mouse event is NEVER sent to an app.
     * The app must listen to Scroll events instead.
     * This identifier is required for internal purposes.
     */
    @Native final static public int WHEEL           = 228;

    /**
     * Non-client events are only natively produced on the Windows platform as a result of
     * handling the {@code WM_NCHITTEST} message for an {@link StageStyle#EXTENDED} window.
     * <p>
     * They are never sent to applications, but are processed by {@link WindowControlsOverlay}.
     */
    @Native final static public int NC_DOWN         = 230;
    @Native final static public int NC_UP           = 231;
    @Native final static public int NC_DRAG         = 232;
    @Native final static public int NC_MOVE         = 233;
    @Native final static public int NC_ENTER        = 234;
    @Native final static public int NC_EXIT         = 235;

    public static boolean isNonClientEvent(int event) {
        return event >= NC_DOWN && event <= NC_EXIT;
    }

    public static int toNonClientEvent(int event) {
        return switch (event) {
            case DOWN -> NC_DOWN;
            case UP -> NC_UP;
            case DRAG -> NC_DRAG;
            case MOVE -> NC_MOVE;
            case ENTER -> NC_ENTER;
            case EXIT -> NC_EXIT;
            default -> event;
        };
    }
}
