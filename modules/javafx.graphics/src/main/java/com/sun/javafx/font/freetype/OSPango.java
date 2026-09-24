/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font.freetype;

/**
 * The Pango constants of {@code pango-font.h} and {@code pango-types.h} that {@link PangoGlyphLayout} passes to
 * {@link PangoNative}. At commit {@code 7b43255b30} this class still declared the thirty-three JNI natives of
 * {@code pango.c} ({@code libjavafx_font_pango.so}) and loaded that library from its static initializer; they
 * were removed with {@code pango.c} once {@code PangoNative} bound the system libraries directly, so nothing
 * here is native and loading the class runs nothing.
 */
class OSPango {

    /* Pango */
    static final int PANGO_SCALE = 1024;
    static final int PANGO_STRETCH_ULTRA_CONDENSED = 0x0;
    static final int PANGO_STRETCH_EXTRA_CONDENSED = 0x1;
    static final int PANGO_STRETCH_CONDENSED = 0x2;
    static final int PANGO_STRETCH_SEMI_CONDENSED = 0x3;
    static final int PANGO_STRETCH_NORMAL = 0x4;
    static final int PANGO_STRETCH_SEMI_EXPANDED = 0x5;
    static final int PANGO_STRETCH_EXPANDED = 0x6;
    static final int PANGO_STRETCH_EXTRA_EXPANDED = 0x7;
    static final int PANGO_STRETCH_ULTRA_EXPANDED = 0x8;
    static final int PANGO_STYLE_ITALIC = 0x2;
    static final int PANGO_STYLE_NORMAL = 0x0;
    static final int PANGO_STYLE_OBLIQUE = 0x1;
    static final int PANGO_WEIGHT_BOLD = 0x2bc;
    static final int PANGO_WEIGHT_NORMAL = 0x190;
    static final int PANGO_DIRECTION_RTL = 1;
}
