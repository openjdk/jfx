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

package com.sun.javafx.font.directwrite;

/**
 * JFX implementation for {@code IDWriteTextRenderer}, the object {@code IDWriteTextLayout::Draw}
 * calls back into to report the glyph runs of the fallback path.
 * <p>
 * The object is built by {@link DWNative}: one synthesized vtable over a block of memory whose
 * glyph runs are recorded on the Java side. <b>The inherited {@code ptr} is a registry id, not a
 * COM pointer</b> - {@link #rendererPointer} is what {@code Draw} is handed - and, as with
 * {@link JFXTextAnalysisSink}, {@link #dispose} is mandatory on the creating thread because the
 * C++ object's {@code delete this} at refcount zero has no Java equivalent.
 */
class JFXTextRenderer extends IUnknown {

    private JFXTextRenderer(long id) {
        super(id);
    }

    /** The replacement for {@code OS._NewJFXTextRenderer}; {@code null} when it could not be made. */
    static JFXTextRenderer create() {
        long id = DWNative.newTextRenderer();
        return id != 0 ? new JFXTextRenderer(id) : null;
    }

    /** The {@code IDWriteTextRenderer*} to hand to {@code IDWriteTextLayout::Draw}. */
    long rendererPointer() {
        return DWNative.textRendererPointer(ptr);
    }

    @Override
    int AddRef() {
        return DWNative.textRendererAddRef(ptr);
    }

    /** The count DirectWrite shares. Reaching zero frees nothing; {@link #dispose} does. */
    @Override
    int Release() {
        return ptr != 0 ? DWNative.textRendererRelease(ptr) : 0;
    }

    /** Frees the object. Idempotent, and must run on the thread that called {@link #create}. */
    void dispose() {
        if (ptr != 0) {
            DWNative.disposeTextRenderer(ptr);
            ptr = 0;
        }
    }

    boolean Next() {
        return DWNative.textRendererNext(ptr);
    }

    int GetStart() {
        return DWNative.textRendererGetStart(ptr);
    }

    int GetLength() {
        return DWNative.textRendererGetLength(ptr);
    }

    int GetGlyphCount() {
        return DWNative.textRendererGetGlyphCount(ptr);
    }

    int GetTotalGlyphCount() {
        return DWNative.textRendererGetTotalGlyphCount(ptr);
    }

    /** The run's font face, <b>borrowed</b>: the C did not AddRef it and no caller releases it. */
    IDWriteFontFace GetFontFace() {
        long result = DWNative.textRendererGetFontFace(ptr);
        return result != 0 ? new IDWriteFontFace(result) : null;
    }

    int GetGlyphIndices(int[] glyphs, int start, int slot) {
        return DWNative.textRendererGetGlyphIndices(ptr, glyphs, start, slot);
    }

    int GetGlyphAdvances(float[] advances, int start) {
        return DWNative.textRendererGetGlyphAdvances(ptr, advances, start);
    }

    int GetGlyphOffsets(float[] offsets, int start) {
        return DWNative.textRendererGetGlyphOffsets(ptr, offsets, start);
    }

    int GetClusterMap(short[] clusterMap, int textStart, int glyphStart) {
        return DWNative.textRendererGetClusterMap(ptr, clusterMap, textStart, glyphStart);
    }
}
