/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.win;

import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Size;

/**
 * MS Windows platform implementation class for Cursor.
 * <p>
 * All four of this class's natives are gone: {@code GlassCursor.cpp}'s {@code _initIDs} cached JNI
 * ids only, {@code _setVisible} and {@code _getBestSize} were one Win32 call each, and
 * {@code _createCursor} was the {@code ICONINFO} sequence of {@code Pixels::CreateIcon}, which
 * {@link WinGlassNative} now builds from the same five GDI and user32 entry points.
 * <p>
 * Since the window peer was flipped this class also owns the other half of {@code GlassCursor.cpp}:
 * {@code JCursorToHCURSOR} and {@code GetNativeCursor}, the {@code Cursor} to {@code HCURSOR}
 * resolution that {@code GlassWindow._setCursor} made through two JNI upcalls, transcribed as
 * {@link #nativeHandleFor}. It lives here and not in the facade because {@code Cursor.getNativeCursor()}
 * is {@code protected}: only a {@code Cursor} subclass can read it.
 * <p>
 * Line numbers into {@code GlassCursor.cpp} and {@code Pixels.cpp} refer to those files at commit
 * {@code 8492cb03b0} ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/<file>}).
 */
final class WinCursor extends Cursor {

    /**
     * The cursor visibility latch {@code Java_com_sun_glass_ui_win_WinCursor__1setVisible} kept in a
     * C static ({@code GlassCursor.cpp:168}), moved here unchanged - including its "XXX: not thread
     * safe" ({@code GlassCursor.cpp:171}), which is tolerable for the same reason it was there: every
     * caller arrives through {@code Cursor.setVisible}, which calls
     * {@code Application.checkEventThread()} ({@code Cursor.java:109-112}).
     * <p>
     * It is not a cache of the OS state but the thing that keeps the OS state sane:
     * {@code ShowCursor} maintains a counter rather than a flag, so calling it on every request would
     * push that counter one step further from zero per call and the pointer would eventually stop
     * coming back. Skipping the call when nothing changed is what bounds it to two adjacent values.
     */
    private static boolean visible = true;

    protected WinCursor(int type) {
        super(type);
    }

    protected WinCursor(int x, int y, Pixels pixels) {
        super(x, y, pixels);
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinCursor__1createCursor} ({@code GlassCursor.cpp:154-158}).
     * <p>
     * {@code getWidthUnsafe} / {@code getHeightUnsafe} and {@code getBuffer} are deliberate:
     * {@code getWidth} / {@code getHeight} add an {@code Application.checkEventThread()}
     * ({@code Pixels.java:155-167}) that the JNI path did not make - it read the {@code Pixels}
     * fields through {@code attachData} - and {@code getPixels} would rewind the caller's buffer
     * ({@code Pixels.java:184-194}) where {@code getBuffer} hands it over untouched.
     * <p>
     * A null {@code pixels} throws {@link NullPointerException} here where the JNI crashed the JVM
     * ({@code CallVoidMethod} on a null object, {@code Pixels.cpp:155}). No caller passes one:
     * {@code Cursor}'s constructor is reached from {@code Application.createCursor(x, y, pixels)}.
     */
    @Override
    protected long _createCursor(int x, int y, Pixels pixels) {
        return WinGlassNative.cursorCreateCustom(pixels.getWidthUnsafe(), pixels.getHeightUnsafe(),
                pixels.getBuffer(), x, y);
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinCursor__1setVisible} ({@code GlassCursor.cpp:165-176}):
     * the same comparison, the same order - call {@code ShowCursor} first, then move the latch - and
     * the same disregard for the display counter it returns.
     */
    static void setVisible_impl(boolean show) {
        if (show != visible) {
            WinGlassNative.showCursor(show);
            visible = show;
        }
    }

    /**
     * {@code Java_com_sun_glass_ui_win_WinCursor__1getBestSize} ({@code GlassCursor.cpp:183-190}):
     * the system cursor size, with both arguments ignored exactly as the C ignored them. There is no
     * rounding of the requested size to a supported one on Windows, and adding some here would be a
     * behaviour change, not a migration.
     */
    static Size getBestSize_impl(int width, int height) {
        return new Size(WinGlassNative.getSystemMetrics(WinGlassNative.SM_CXCURSOR),
                WinGlassNative.getSystemMetrics(WinGlassNative.SM_CYCURSOR));
    }

    /**
     * {@code JCursorToHCURSOR} ({@code GlassCursor.cpp}) in Java, for {@code WinWindow._setCursor}:
     * a null cursor is no cursor; a {@code CURSOR_CUSTOM} answers the {@code HCURSOR} its constructor
     * built ({@code Cursor.getNativeCursor()}); every other type goes through
     * {@link #systemCursorHandle}. The two accessors check the event thread, as they did when the C
     * called them - inside the {@code SendMessage} then, before it now, the same thread on Windows.
     *
     * @return the {@code HCURSOR} as a {@code long}, 0 for none
     */
    static long nativeHandleFor(Cursor cursor) {
        if (cursor == null) {
            return 0L;
        }
        int type = cursor.getType();
        if (type == CURSOR_CUSTOM) {
            // getNativeCursor() is protected on Cursor: readable through a WinCursor reference only,
            // and every custom cursor of this platform is one (WinApplication.createCursor).
            return cursor instanceof WinCursor custom ? custom.getNativeCursor() : 0L;
        }
        return systemCursorHandle(type);
    }

    /**
     * {@code GetNativeCursor(type)} ({@code GlassCursor.cpp}): {@code CURSOR_NONE} is no cursor,
     * before any load; the two {@code GlassResources.rc} cursors load by name and everything else
     * by {@code IDC_*} ordinal, through the unconditional three-step
     * {@link WinGlassNative#loadSystemCursor(int)}.
     */
    static long systemCursorHandle(int type) {
        if (type == CURSOR_NONE) {
            return 0L;
        }
        String name = systemCursorName(type);
        return name != null ? WinGlassNative.loadSystemCursor(name)
                : WinGlassNative.loadSystemCursor(systemCursorOrdinal(type));
    }

    /**
     * The {@code switch} of {@code GetNativeCursor}, ordinal half: the {@code IDC_*} resource for a
     * system cursor type. {@code CURSOR_DISAPPEAR} is "not implemented, using CURSOR_DEFAULT instead",
     * and any type the switch does not name - {@code CURSOR_CUSTOM} included, which never gets here
     * from {@link #nativeHandleFor} - is the arrow too. 0 for the two named cursors and for
     * {@code CURSOR_NONE}, which have no ordinal.
     */
    static int systemCursorOrdinal(int type) {
        return switch (type) {
            case CURSOR_DEFAULT, CURSOR_DISAPPEAR -> WinGlassNative.IDC_ARROW;
            case CURSOR_TEXT -> WinGlassNative.IDC_IBEAM;
            case CURSOR_CROSSHAIR -> WinGlassNative.IDC_CROSS;
            case CURSOR_POINTING_HAND -> WinGlassNative.IDC_HAND;
            case CURSOR_RESIZE_UP, CURSOR_RESIZE_DOWN, CURSOR_RESIZE_UPDOWN -> WinGlassNative.IDC_SIZENS;
            case CURSOR_RESIZE_LEFT, CURSOR_RESIZE_RIGHT, CURSOR_RESIZE_LEFTRIGHT -> WinGlassNative.IDC_SIZEWE;
            case CURSOR_RESIZE_SOUTHWEST, CURSOR_RESIZE_NORTHEAST -> WinGlassNative.IDC_SIZENESW;
            case CURSOR_RESIZE_SOUTHEAST, CURSOR_RESIZE_NORTHWEST -> WinGlassNative.IDC_SIZENWSE;
            case CURSOR_MOVE -> WinGlassNative.IDC_SIZEALL;
            case CURSOR_WAIT -> WinGlassNative.IDC_WAIT;
            case CURSOR_CLOSED_HAND, CURSOR_OPEN_HAND, CURSOR_NONE -> 0;
            default -> WinGlassNative.IDC_ARROW;
        };
    }

    /**
     * The {@code switch} of {@code GetNativeCursor}, name half: the resource name for the two hand
     * cursors, else null.
     */
    static String systemCursorName(int type) {
        return switch (type) {
            case CURSOR_CLOSED_HAND -> WinGlassNative.IDC_CLOSED_HAND;
            case CURSOR_OPEN_HAND -> WinGlassNative.IDC_OPEN_HAND;
            default -> null;
        };
    }
}
