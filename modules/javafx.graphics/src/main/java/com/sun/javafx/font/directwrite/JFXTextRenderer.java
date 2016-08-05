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

/* JFX implementation for IDWriteTextRenderer */
class JFXTextRenderer extends IUnknown {
    JFXTextRenderer(long ptr) {
        super(ptr);
    }

    boolean Next() {
        return OS.JFXTextRendererNext(ptr);
    }

    int GetStart() {
        return OS.JFXTextRendererGetStart(ptr);
    }

    int GetLength() {
        return OS.JFXTextRendererGetLength(ptr);
    }

    int GetGlyphCount() {
        return OS.JFXTextRendererGetGlyphCount(ptr);
    }

    int GetTotalGlyphCount() {
        return OS.JFXTextRendererGetTotalGlyphCount(ptr);
    }

    IDWriteFontFace GetFontFace() {
        long result = OS.JFXTextRendererGetFontFace(ptr);
        return result != 0 ? new IDWriteFontFace(result) : null;
    }

    int GetGlyphIndices(int[] glyphs, int start, int slot) {
        return OS.JFXTextRendererGetGlyphIndices(ptr, glyphs, start, slot);
    }

    int GetGlyphAdvances(float[] advances, int start) {
        return OS.JFXTextRendererGetGlyphAdvances(ptr, advances, start);
    }

    int GetGlyphOffsets(float[] offsets, int start) {
        return OS.JFXTextRendererGetGlyphOffsets(ptr, offsets, start);
    }

    int GetClusterMap(short[] clusterMap, int textStart, int glyphStart) {
        return OS.JFXTextRendererGetClusterMap(ptr, clusterMap, textStart, glyphStart);
    }
}
