/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Path2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test access to what {@link FTNative} keeps package-private beyond the entry points {@link OSFreetypeShim}
 * delegates to: the record layouts it dereferences, the library loader's error mapping, the outline
 * accumulator's failure path, and the complete glyph slot mirror. Compiled into the module by the
 * {@code compile-shims} execution of the javafx.graphics pom; reached by the tests through the
 * {@code --add-exports javafx.graphics/com.sun.javafx.font.freetype=ALL-UNNAMED} line of
 * {@code src/test/addExports}. No restricted {@code java.lang.foreign} call is made here: every one stays in
 * {@code FTNative}.
 */
public final class FTNativeShim {

    private FTNativeShim() {
    }

    /**
     * The byte sizes of the record layouts and the offsets {@code FTNative} dereferences, keyed
     * {@code sizeof.<struct>} and {@code offsetof.<struct>.<field>} in the terms of {@code freetype.h}.
     */
    public static Map<String, Long> layout() {
        Map<String, Long> values = new LinkedHashMap<>();
        values.put("sizeof.FT_Vector", FTNative.FT_VECTOR.byteSize());
        values.put("sizeof.FT_Matrix", FTNative.FT_MATRIX.byteSize());
        values.put("sizeof.FT_Glyph_Metrics", FTNative.FT_GLYPH_METRICS.byteSize());
        values.put("sizeof.FT_Bitmap", FTNative.FT_BITMAP.byteSize());
        values.put("sizeof.FT_Outline", FTNative.FT_OUTLINE.byteSize());
        values.put("sizeof.FT_Outline_Funcs", FTNative.FT_OUTLINE_FUNCS.byteSize());
        values.put("sizeof.FT_GlyphSlotRec", FTNative.FT_GLYPH_SLOT_REC.byteSize());
        values.put("sizeof.FT_FaceRec", FTNative.FT_FACE_REC.byteSize());
        values.put("offsetof.FT_Vector.x", FTNative.VECTOR_X);
        values.put("offsetof.FT_Vector.y", FTNative.VECTOR_Y);
        values.put("offsetof.FT_FaceRec.glyph", FTNative.FACE_GLYPH);
        values.put("offsetof.FT_GlyphSlotRec.metrics.width", FTNative.SLOT_METRICS_WIDTH);
        values.put("offsetof.FT_GlyphSlotRec.metrics.height", FTNative.SLOT_METRICS_HEIGHT);
        values.put("offsetof.FT_GlyphSlotRec.metrics.horiBearingX", FTNative.SLOT_METRICS_HORI_BEARING_X);
        values.put("offsetof.FT_GlyphSlotRec.metrics.horiBearingY", FTNative.SLOT_METRICS_HORI_BEARING_Y);
        values.put("offsetof.FT_GlyphSlotRec.metrics.horiAdvance", FTNative.SLOT_METRICS_HORI_ADVANCE);
        values.put("offsetof.FT_GlyphSlotRec.metrics.vertBearingX", FTNative.SLOT_METRICS_VERT_BEARING_X);
        values.put("offsetof.FT_GlyphSlotRec.metrics.vertBearingY", FTNative.SLOT_METRICS_VERT_BEARING_Y);
        values.put("offsetof.FT_GlyphSlotRec.metrics.vertAdvance", FTNative.SLOT_METRICS_VERT_ADVANCE);
        values.put("offsetof.FT_GlyphSlotRec.linearHoriAdvance", FTNative.SLOT_LINEAR_HORI_ADVANCE);
        values.put("offsetof.FT_GlyphSlotRec.linearVertAdvance", FTNative.SLOT_LINEAR_VERT_ADVANCE);
        values.put("offsetof.FT_GlyphSlotRec.advance.x", FTNative.SLOT_ADVANCE_X);
        values.put("offsetof.FT_GlyphSlotRec.advance.y", FTNative.SLOT_ADVANCE_Y);
        values.put("offsetof.FT_GlyphSlotRec.format", FTNative.SLOT_FORMAT);
        values.put("offsetof.FT_GlyphSlotRec.bitmap.rows", FTNative.SLOT_BITMAP_ROWS);
        values.put("offsetof.FT_GlyphSlotRec.bitmap.width", FTNative.SLOT_BITMAP_WIDTH);
        values.put("offsetof.FT_GlyphSlotRec.bitmap.pitch", FTNative.SLOT_BITMAP_PITCH);
        values.put("offsetof.FT_GlyphSlotRec.bitmap.buffer", FTNative.SLOT_BITMAP_BUFFER);
        values.put("offsetof.FT_GlyphSlotRec.bitmap.num_grays", FTNative.SLOT_BITMAP_NUM_GRAYS);
        values.put("offsetof.FT_GlyphSlotRec.bitmap.pixel_mode", FTNative.SLOT_BITMAP_PIXEL_MODE);
        values.put("offsetof.FT_GlyphSlotRec.bitmap.palette_mode", FTNative.SLOT_BITMAP_PALETTE_MODE);
        values.put("offsetof.FT_GlyphSlotRec.bitmap.palette", FTNative.SLOT_BITMAP_PALETTE);
        values.put("offsetof.FT_GlyphSlotRec.bitmap_left", FTNative.SLOT_BITMAP_LEFT);
        values.put("offsetof.FT_GlyphSlotRec.bitmap_top", FTNative.SLOT_BITMAP_TOP);
        values.put("offsetof.FT_GlyphSlotRec.outline", FTNative.SLOT_OUTLINE);
        values.put("offsetof.FT_Outline_Funcs.move_to", FTNative.FUNCS_MOVE_TO);
        values.put("offsetof.FT_Outline_Funcs.line_to", FTNative.FUNCS_LINE_TO);
        values.put("offsetof.FT_Outline_Funcs.conic_to", FTNative.FUNCS_CONIC_TO);
        values.put("offsetof.FT_Outline_Funcs.cubic_to", FTNative.FUNCS_CUBIC_TO);
        values.put("offsetof.FT_Outline_Funcs.shift", FTNative.FUNCS_SHIFT);
        values.put("offsetof.FT_Outline_Funcs.delta", FTNative.FUNCS_DELTA);
        return values;
    }

    /** The soname {@code FTNative} binds. */
    public static String library() {
        return FTNative.LIBRARY;
    }

    /**
     * {@code FTNative}'s library loader on an arbitrary soname: returns normally when the library loads, and
     * throws what a missing {@code libfreetype.so.6} would make the first FreeType call throw.
     */
    public static void load(String soname) {
        FTNative.load(soname);
    }

    /** The outcome of an {@code FT_Outline_Decompose} whose accumulator refuses segments beyond a limit. */
    public record LimitedDecompose(Path2D path, int segmentsAccepted, String failure) {
    }

    /**
     * {@code FT_Outline_Decompose} of the face's glyph slot into an accumulator that throws once it holds
     * {@code maxSegments}: how the binding treats a callback that fails while FreeType is still calling.
     */
    public static LimitedDecompose decomposeLimited(long face, int maxSegments) {
        FTOutlineSink sink = new FTOutlineSink(maxSegments);
        Path2D path = FTNative.decompose(face, sink);
        Throwable failure = sink.failure();
        return new LimitedDecompose(path, sink.numTypes(),
                failure == null ? null : failure.getClass().getName() + ": " + failure.getMessage());
    }

    /**
     * Every field {@code getGlyphSlot} fills, or {@code null} when it returns no slot: the eight metrics,
     * {@code linearHoriAdvance}, {@code linearVertAdvance}, {@code advance.x}, {@code advance.y},
     * {@code format}, {@code bitmap.rows}, {@code bitmap.width}, {@code bitmap.pitch}, whether
     * {@code bitmap.buffer} is non-null, {@code bitmap.num_grays}, {@code bitmap.pixel_mode},
     * {@code bitmap.palette_mode}, whether {@code bitmap.palette} is non-null, {@code bitmap_left},
     * {@code bitmap_top}. The two pointers differ between libraries, so only their nullness is comparable.
     */
    public static long[] fullGlyphSlot(long face) {
        return allFields(FTNative.getGlyphSlot(face));
    }

    static long[] allFields(FT_GlyphSlotRec slot) {
        if (slot == null) {
            return null;
        }
        return new long[] {
            slot.metrics.width, slot.metrics.height, slot.metrics.horiBearingX, slot.metrics.horiBearingY,
            slot.metrics.horiAdvance, slot.metrics.vertBearingX, slot.metrics.vertBearingY,
            slot.metrics.vertAdvance, slot.linearHoriAdvance, slot.linearVertAdvance, slot.advance_x,
            slot.advance_y, slot.format, slot.bitmap.rows, slot.bitmap.width, slot.bitmap.pitch,
            slot.bitmap.buffer != 0 ? 1 : 0, slot.bitmap.num_grays, slot.bitmap.pixel_mode,
            slot.bitmap.palette_mode, slot.bitmap.palette != 0 ? 1 : 0, slot.bitmap_left, slot.bitmap_top
        };
    }

    static long[] productionFields(FT_GlyphSlotRec slot) {
        if (slot == null) {
            return null;
        }
        return new long[] {
            slot.metrics.width, slot.metrics.height, slot.metrics.horiBearingX, slot.metrics.horiBearingY,
            slot.linearHoriAdvance, slot.advance_x, slot.advance_y,
            slot.bitmap.rows, slot.bitmap.width, slot.bitmap.pitch, slot.bitmap.pixel_mode,
            slot.bitmap_left, slot.bitmap_top
        };
    }
}
