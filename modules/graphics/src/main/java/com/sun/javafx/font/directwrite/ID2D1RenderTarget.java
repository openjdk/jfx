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

package com.sun.javafx.font.directwrite;

class ID2D1RenderTarget extends IUnknown {
    ID2D1RenderTarget(long ptr) {
        super(ptr);
    }

    void BeginDraw() {
        OS.BeginDraw(ptr);
    }

    int EndDraw() {
        return OS.EndDraw(ptr);
    }

    void Clear(D2D1_COLOR_F clearColor) {
        OS.Clear(ptr, clearColor);
    }

    void SetTransform(D2D1_MATRIX_3X2_F transform) {
        OS.SetTransform(ptr, transform);
    }

    void SetTextAntialiasMode(int textAntialiasMode) {
        OS.SetTextAntialiasMode(ptr, textAntialiasMode);
    }

    void DrawGlyphRun(D2D1_POINT_2F baselineOrigin, DWRITE_GLYPH_RUN glyphRun, ID2D1Brush foregroundBrush, int measuringMode) {
        OS.DrawGlyphRun(ptr, baselineOrigin, glyphRun, foregroundBrush.ptr, measuringMode);
    }

    ID2D1Brush CreateSolidColorBrush(D2D1_COLOR_F color) {
        long result = OS.CreateSolidColorBrush(ptr, color);
        return result != 0 ? new ID2D1Brush(result) : null;
    }
}
