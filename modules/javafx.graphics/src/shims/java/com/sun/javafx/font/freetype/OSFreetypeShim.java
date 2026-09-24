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

/**
 * Test seam onto the FreeType entry points of the Linux font layer: the {@link FTNative} methods that
 * {@link FTFactory}, {@code FTFontFile} and {@code FTDisposer} call.
 * <p>
 * The Linux font goldens ({@code test.com.sun.javafx.font.LinuxFreetypeGoldenTest}) were captured through
 * this class from the JNI build of commit {@code 7b43255b30}, when it delegated to the {@code OSFreetype}
 * natives of {@code freetype.c}; it now delegates to {@code FTNative}, the {@code java.lang.foreign} binding
 * of {@code libfreetype.so.6} that replaced them, so the goldens compare the replacement with that capture.
 * It is a pure delegate with no {@code java.lang.foreign} of its own: only this class was re-pointed, and a
 * signature change fails here at compile time, which is the intended signal. The test classes and the golden
 * files stay byte-unchanged.
 * <p>
 * The FreeType record mirrors are not exposed; {@link #glyphSlot(long)} returns exactly the fields that
 * production Java reads, so a replacement that drops the unread fields ({@code format},
 * {@code linearVertAdvance}, {@code num_grays}, ...) changes nothing the goldens see.
 */
public final class OSFreetypeShim {

    private OSFreetypeShim() {
    }

    /**
     * Initialises the FreeType binding class. {@code FTNative} binds {@code libfreetype.so.6} on the first call
     * into FreeType, where the JNI build loaded {@code libjavafx_font_freetype.so}; so does the next call here.
     */
    public static void ensureLoaded() {
        try {
            Class.forName(FTNative.class.getName(), true, FTNative.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int ftInitFreeType(long[] alibrary) {
        return FTNative.FT_Init_FreeType(alibrary);
    }

    public static int ftDoneFreeType(long library) {
        return FTNative.FT_Done_FreeType(library);
    }

    public static void ftLibraryVersion(long library, int[] major, int[] minor, int[] patch) {
        FTNative.FT_Library_Version(library, major, minor, patch);
    }

    public static int ftLibrarySetLcdFilter(long library, int filter) {
        return FTNative.FT_Library_SetLcdFilter(library, filter);
    }

    /** {@code FT_New_Face}; {@code path} must carry its own NUL terminator, as the production callers pass it. */
    public static int ftNewFace(long library, byte[] path, long faceIndex, long[] aface) {
        return FTNative.FT_New_Face(library, path, faceIndex, aface);
    }

    public static int ftDoneFace(long face) {
        return FTNative.FT_Done_Face(face);
    }

    public static int ftSetCharSize(long face, long charWidth, long charHeight, int horzResolution,
                                    int vertResolution) {
        return FTNative.FT_Set_Char_Size(face, charWidth, charHeight, horzResolution, vertResolution);
    }

    public static int ftLoadGlyph(long face, int glyphIndex, int loadFlags) {
        return FTNative.FT_Load_Glyph(face, glyphIndex, loadFlags);
    }

    /**
     * {@code FT_Set_Transform} with the matrix given as {@code {xx, xy, yx, yy}} in 16.16; {@code null} passes a
     * null {@code FT_Matrix}, for which the binding makes no call at all.
     */
    public static void ftSetTransform(long face, long[] matrix, long deltaX, long deltaY) {
        FT_Matrix m = null;
        if (matrix != null) {
            m = new FT_Matrix();
            m.xx = matrix[0];
            m.xy = matrix[1];
            m.yx = matrix[2];
            m.yy = matrix[3];
        }
        FTNative.FT_Set_Transform(face, m, deltaX, deltaY);
    }

    /**
     * The glyph slot fields production Java reads, or {@code null} when the binding returns no slot: {@code
     * metrics.width, metrics.height, metrics.horiBearingX, metrics.horiBearingY, linearHoriAdvance, advance.x,
     * advance.y, bitmap.rows, bitmap.width, bitmap.pitch, bitmap.pixel_mode, bitmap_left, bitmap_top}.
     */
    public static long[] glyphSlot(long face) {
        FT_GlyphSlotRec slot = FTNative.getGlyphSlot(face);
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

    public static byte[] bitmapData(long face) {
        return FTNative.getBitmapData(face);
    }

    public static Path2D outlineDecompose(long face) {
        return FTNative.FT_Outline_Decompose(face);
    }

    public static boolean pangoEnabled() {
        return FTNative.isPangoEnabled();
    }

    public static boolean harfbuzzEnabled() {
        return FTNative.isHarfbuzzEnabled();
    }

    /** A FreeType constant as {@code FTNative} defines it. */
    public static int constant(String name) {
        return switch (name) {
            case "FT_LOAD_DEFAULT" -> FTNative.FT_LOAD_DEFAULT;
            case "FT_LOAD_NO_SCALE" -> FTNative.FT_LOAD_NO_SCALE;
            case "FT_LOAD_NO_HINTING" -> FTNative.FT_LOAD_NO_HINTING;
            case "FT_LOAD_RENDER" -> FTNative.FT_LOAD_RENDER;
            case "FT_LOAD_NO_BITMAP" -> FTNative.FT_LOAD_NO_BITMAP;
            case "FT_LOAD_VERTICAL_LAYOUT" -> FTNative.FT_LOAD_VERTICAL_LAYOUT;
            case "FT_LOAD_FORCE_AUTOHINT" -> FTNative.FT_LOAD_FORCE_AUTOHINT;
            case "FT_LOAD_IGNORE_TRANSFORM" -> FTNative.FT_LOAD_IGNORE_TRANSFORM;
            case "FT_LOAD_TARGET_NORMAL" -> FTNative.FT_LOAD_TARGET_NORMAL;
            case "FT_LOAD_TARGET_LIGHT" -> FTNative.FT_LOAD_TARGET_LIGHT;
            case "FT_LOAD_TARGET_MONO" -> FTNative.FT_LOAD_TARGET_MONO;
            case "FT_LOAD_TARGET_LCD" -> FTNative.FT_LOAD_TARGET_LCD;
            case "FT_LOAD_TARGET_LCD_V" -> FTNative.FT_LOAD_TARGET_LCD_V;
            case "FT_PIXEL_MODE_MONO" -> FTNative.FT_PIXEL_MODE_MONO;
            case "FT_PIXEL_MODE_GRAY" -> FTNative.FT_PIXEL_MODE_GRAY;
            case "FT_PIXEL_MODE_LCD" -> FTNative.FT_PIXEL_MODE_LCD;
            case "FT_PIXEL_MODE_LCD_V" -> FTNative.FT_PIXEL_MODE_LCD_V;
            case "FT_LCD_FILTER_NONE" -> FTNative.FT_LCD_FILTER_NONE;
            case "FT_LCD_FILTER_DEFAULT" -> FTNative.FT_LCD_FILTER_DEFAULT;
            case "FT_LCD_FILTER_LIGHT" -> FTNative.FT_LCD_FILTER_LIGHT;
            case "FT_LCD_FILTER_LEGACY" -> FTNative.FT_LCD_FILTER_LEGACY;
            default -> throw new IllegalArgumentException("no FreeType constant " + name);
        };
    }
}
