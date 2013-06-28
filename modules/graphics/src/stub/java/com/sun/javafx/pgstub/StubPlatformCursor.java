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

package com.sun.javafx.pgstub;

import javafx.scene.Cursor;

public class StubPlatformCursor {
    public static final StubPlatformCursor DEFAULT = new StubPlatformCursor();

    public static final StubPlatformCursor CROSSHAIR = new StubPlatformCursor();

    public static final StubPlatformCursor TEXT = new StubPlatformCursor();

    public static final StubPlatformCursor WAIT = new StubPlatformCursor();

    public static final StubPlatformCursor SW_RESIZE = new StubPlatformCursor();

    public static final StubPlatformCursor SE_RESIZE = new StubPlatformCursor();

    public static final StubPlatformCursor NW_RESIZE = new StubPlatformCursor();

    public static final StubPlatformCursor NE_RESIZE = new StubPlatformCursor();

    public static final StubPlatformCursor N_RESIZE = new StubPlatformCursor();

    public static final StubPlatformCursor S_RESIZE = new StubPlatformCursor();

    public static final StubPlatformCursor W_RESIZE = new StubPlatformCursor();

    public static final StubPlatformCursor E_RESIZE = new StubPlatformCursor();

    public static final StubPlatformCursor HAND = new StubPlatformCursor();

    public static final StubPlatformCursor MOVE = new StubPlatformCursor();

    public static final StubPlatformCursor NONE = new StubPlatformCursor();

    public static StubPlatformCursor getInstance(final Cursor cursor) {
        StubPlatformCursor platformCursor;

        if (cursor == Cursor.CROSSHAIR) {
            platformCursor = CROSSHAIR;
        } else if (cursor == Cursor.TEXT) {
            platformCursor = TEXT;
        } else if (cursor == Cursor.WAIT) {
            platformCursor = WAIT;
        } else if (cursor == Cursor.SW_RESIZE) {
            platformCursor = SW_RESIZE;
        } else if (cursor == Cursor.SE_RESIZE) {
            platformCursor = SE_RESIZE;
        } else if (cursor == Cursor.NW_RESIZE) {
            platformCursor = NW_RESIZE;
        } else if (cursor == Cursor.NE_RESIZE) {
            platformCursor = NE_RESIZE;
        } else if ((cursor == Cursor.N_RESIZE) || (cursor == Cursor.V_RESIZE)) {
            platformCursor = N_RESIZE;
        } else if (cursor == Cursor.S_RESIZE) {
            platformCursor = S_RESIZE;
        } else if ((cursor == Cursor.W_RESIZE) || (cursor == Cursor.H_RESIZE)) {
            platformCursor = W_RESIZE;
        } else if (cursor == Cursor.E_RESIZE) {
            platformCursor = E_RESIZE;
        } else if (cursor == Cursor.HAND) {
            platformCursor = HAND;
        } else if (cursor == Cursor.MOVE) {
            platformCursor = MOVE;
        } else if (cursor == Cursor.NONE) {
            platformCursor = NONE;
        } else {
            platformCursor = DEFAULT;
        }

        return platformCursor;
    }
}
