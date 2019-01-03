/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import com.sun.glass.events.MouseEvent;
import com.sun.glass.events.ViewEvent;
import com.sun.glass.events.WindowEvent;


class GlassEventUtils {

    private GlassEventUtils() {
    }

    public static String getMouseEventString(int type) {
        switch (type) {
            case MouseEvent.BUTTON_NONE:
                return "BUTTON_NONE";
            case MouseEvent.BUTTON_LEFT:
                return "BUTTON_LEFT";
            case MouseEvent.BUTTON_RIGHT:
                return "BUTTON_RIGHT";
            case MouseEvent.BUTTON_OTHER:
                return "BUTTON_OTHER";
            case MouseEvent.BUTTON_BACK:
                return "BUTTON_BACK";
            case MouseEvent.BUTTON_FORWARD:
                return "BUTTON_FORWARD";
            case MouseEvent.DOWN:
                return "DOWN";
            case MouseEvent.UP:
                return "UP";
            case MouseEvent.DRAG:
                return "DRAG";
            case MouseEvent.MOVE:
                return "MOVE";
            case MouseEvent.ENTER:
                return "ENTER";
            case MouseEvent.EXIT:
                return "EXIT";
            case MouseEvent.CLICK:
                return "CLICK";
            case MouseEvent.WHEEL:
                return "WHEEL";
            default:
                return "UNKNOWN";
        }
    }

    public static String getViewEventString(int type) {
        switch (type) {
            case ViewEvent.ADD:
                return "ADD";
            case ViewEvent.REMOVE:
                return "REMOVE";
            case ViewEvent.REPAINT:
                return "REPAINT";
            case ViewEvent.RESIZE:
                return "RESIZE";
            case ViewEvent.MOVE:
                return "MOVE";
            case ViewEvent.FULLSCREEN_ENTER:
                return "FULLSCREEN_ENTER";
            case ViewEvent.FULLSCREEN_EXIT:
                return "FULLSCREEN_EXIT";
            default:
                return "UNKNOWN";
        }
    }

    public static String getWindowEventString(int type) {
        switch (type) {
            case WindowEvent.RESIZE:
                return "RESIZE";
            case WindowEvent.MOVE:
                return "MOVE";
            case WindowEvent.CLOSE:
                return "CLOSE";
            case WindowEvent.DESTROY:
                return "DESTROY";
            case WindowEvent.MINIMIZE:
                return "MINIMIZE";
            case WindowEvent.MAXIMIZE:
                return "MAXIMIZE";
            case WindowEvent.RESTORE:
                return "RESTORE";
            case WindowEvent.FOCUS_LOST:
                return "FOCUS_LOST";
            case WindowEvent.FOCUS_GAINED:
                return "FOCUS_GAINED";
            case WindowEvent.FOCUS_GAINED_FORWARD:
                return "FOCUS_GAINED_FORWARD";
            case WindowEvent.FOCUS_GAINED_BACKWARD:
                return "FOCUS_GAINED_BACKWARD";
            case WindowEvent.FOCUS_DISABLED:
                return "FOCUS_DISABLED";
            case WindowEvent.FOCUS_UNGRAB:
                return "FOCUS_UNGRAB";
            default:
                return "UNKNOWN";
        }
    }
}
