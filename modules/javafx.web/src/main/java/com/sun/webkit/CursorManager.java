/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import java.util.HashMap;
import java.util.Map;

import com.sun.webkit.graphics.WCImage;
import com.sun.webkit.graphics.WCImageFrame;

public abstract class CursorManager<T> {

    public static final int POINTER                      =  0;
    public static final int CROSS                        =  1;
    public static final int HAND                         =  2;
    public static final int MOVE                         =  3;
    public static final int TEXT                         =  4;
    public static final int WAIT                         =  5;
    public static final int HELP                         =  6;
    public static final int EAST_RESIZE                  =  7;
    public static final int NORTH_RESIZE                 =  8;
    public static final int NORTH_EAST_RESIZE            =  9;
    public static final int NORTH_WEST_RESIZE            = 10;
    public static final int SOUTH_RESIZE                 = 11;
    public static final int SOUTH_EAST_RESIZE            = 12;
    public static final int SOUTH_WEST_RESIZE            = 13;
    public static final int WEST_RESIZE                  = 14;
    public static final int NORTH_SOUTH_RESIZE           = 15;
    public static final int EAST_WEST_RESIZE             = 16;
    public static final int NORTH_EAST_SOUTH_WEST_RESIZE = 17;
    public static final int NORTH_WEST_SOUTH_EAST_RESIZE = 18;
    public static final int COLUMN_RESIZE                = 19;
    public static final int ROW_RESIZE                   = 20;
    public static final int MIDDLE_PANNING               = 21;
    public static final int EAST_PANNING                 = 22;
    public static final int NORTH_PANNING                = 23;
    public static final int NORTH_EAST_PANNING           = 24;
    public static final int NORTH_WEST_PANNING           = 25;
    public static final int SOUTH_PANNING                = 26;
    public static final int SOUTH_EAST_PANNING           = 27;
    public static final int SOUTH_WEST_PANNING           = 28;
    public static final int WEST_PANNING                 = 29;
    public static final int VERTICAL_TEXT                = 30;
    public static final int CELL                         = 31;
    public static final int CONTEXT_MENU                 = 32;
    public static final int NO_DROP                      = 33;
    public static final int NOT_ALLOWED                  = 34;
    public static final int PROGRESS                     = 35;
    public static final int ALIAS                        = 36;
    public static final int ZOOM_IN                      = 37;
    public static final int ZOOM_OUT                     = 38;
    public static final int COPY                         = 39;
    public static final int NONE                         = 40;
    public static final int GRAB                         = 41;
    public static final int GRABBING                     = 42;

    private static CursorManager instance;

    public static void setCursorManager(CursorManager manager) {
        instance = manager;
    }

    public static CursorManager getCursorManager() {
        return instance;
    }

    private final Map<Long, T> map = new HashMap<Long, T>();

    protected abstract T getCustomCursor(WCImage image, int hotspotX, int hotspotY);

    protected abstract T getPredefinedCursor(int type);

    private long getCustomCursorID(WCImageFrame frame, int hotspotX, int hotspotY) {
        return putCursor(getCustomCursor(frame.getFrame(), hotspotX, hotspotY));
    }

    private long getPredefinedCursorID(int type) {
        return putCursor(getPredefinedCursor(type));
    }

    public final T getCursor(long id) {
        return this.map.get(id);
    }

    private long putCursor(T cursor) {
        long id = cursor.hashCode();
        this.map.put(id, cursor);
        return id;
    }
}
