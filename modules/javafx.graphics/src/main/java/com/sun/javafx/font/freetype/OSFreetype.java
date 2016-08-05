/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.geom.Path2D;

class OSFreetype {

    static {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            NativeLibLoader.loadLibrary("javafx_font_freetype");
            return null;
        });
    }

    /* Freetype */
    static final int FT_FACE_FLAG_SCALABLE          = 1 <<  0;
    static final int FT_FACE_FLAG_FIXED_SIZES       = 1 <<  1;
    static final int FT_FACE_FLAG_FIXED_WIDTH       = 1 <<  2;
    static final int FT_FACE_FLAG_SFNT              = 1 <<  3;
    static final int FT_FACE_FLAG_HORIZONTAL        = 1 <<  4;
    static final int FT_FACE_FLAG_VERTICAL          = 1 <<  5;
    static final int FT_FACE_FLAG_KERNING           = 1 <<  6;
    static final int FT_FACE_FLAG_FAST_GLYPHS       = 1 <<  7;
    static final int FT_FACE_FLAG_MULTIPLE_MASTERS  = 1 <<  8;
    static final int FT_FACE_FLAG_GLYPH_NAMES       = 1 <<  9;
    static final int FT_FACE_FLAG_EXTERNAL_STREAM   = 1 << 10;
    static final int FT_FACE_FLAG_HINTER            = 1 << 11;
    static final int FT_FACE_FLAG_CID_KEYED         = 1 << 12;
    static final int FT_FACE_FLAG_TRICKY            = 1 << 13;
    static final int FT_STYLE_FLAG_ITALIC  = 1 << 0;
    static final int FT_STYLE_FLAG_BOLD    = 1 << 1;
    static final int FT_RENDER_MODE_NORMAL = 0;
    static final int FT_RENDER_MODE_LIGHT = 1;
    static final int FT_RENDER_MODE_MONO = 2;
    static final int FT_RENDER_MODE_LCD = 3;
    static final int FT_RENDER_MODE_LCD_V = 4;
    static final int FT_PIXEL_MODE_NONE = 0;
    static final int FT_PIXEL_MODE_MONO = 1;
    static final int FT_PIXEL_MODE_GRAY = 2;
    static final int FT_PIXEL_MODE_GRAY2 = 3;
    static final int FT_PIXEL_MODE_GRAY4 = 4;
    static final int FT_PIXEL_MODE_LCD = 5;
    static final int FT_PIXEL_MODE_LCD_V = 6;
    static final int FT_LOAD_DEFAULT                      = 0x0;
    static final int FT_LOAD_NO_SCALE                     = 1 << 0;
    static final int FT_LOAD_NO_HINTING                   = 1 << 1;
    static final int FT_LOAD_RENDER                       = 1 << 2;
    static final int FT_LOAD_NO_BITMAP                    = 1 << 3;
    static final int FT_LOAD_VERTICAL_LAYOUT              = 1 << 4;
    static final int FT_LOAD_FORCE_AUTOHINT               = 1 << 5;
    static final int FT_LOAD_CROP_BITMAP                  = 1 << 6;
    static final int FT_LOAD_PEDANTIC                     = 1 << 7;
    static final int FT_LOAD_IGNORE_GLOBAL_ADVANCE_WIDTH  = 1 << 9;
    static final int FT_LOAD_NO_RECURSE                   = 1 << 10;
    static final int FT_LOAD_IGNORE_TRANSFORM             = 1 << 11;
    static final int FT_LOAD_MONOCHROME                   = 1 << 12;
    static final int FT_LOAD_LINEAR_DESIGN                = 1 << 13;
    static final int FT_LOAD_NO_AUTOHINT                  = 1 << 15;
    static final int FT_LOAD_TARGET_NORMAL  = (FT_RENDER_MODE_NORMAL & 15 ) << 16;
    static final int FT_LOAD_TARGET_LIGHT   = (FT_RENDER_MODE_LIGHT  & 15 ) << 16;
    static final int FT_LOAD_TARGET_MONO    = (FT_RENDER_MODE_MONO   & 15 ) << 16;
    static final int FT_LOAD_TARGET_LCD     = (FT_RENDER_MODE_LCD    & 15 ) << 16;
    static final int FT_LOAD_TARGET_LCD_V   = (FT_RENDER_MODE_LCD_V  & 15 ) << 16;
    static final int FT_LCD_FILTER_NONE    = 0;
    static final int FT_LCD_FILTER_DEFAULT = 1;
    static final int FT_LCD_FILTER_LIGHT   = 2;
    static final int FT_LCD_FILTER_LEGACY  = 16;

    static final int FT_LOAD_TARGET_MODE(int x) {
        return (x >> 16 ) & 15;
    }

    static final native Path2D FT_Outline_Decompose(long face);
    static final native int FT_Init_FreeType(long[] alibrary);
    static final native int FT_Done_FreeType(long library);
    static final native void FT_Library_Version(long library, int[] amajor, int[] aminor, int[] apatch);
    static final native int FT_Library_SetLcdFilter(long library, int filter);
    static final native int FT_New_Face(long library, byte[] filepathname, long face_index, long[] aface);
    static final native int FT_Done_Face(long face);
    static final native int FT_Get_Char_Index(long face, long charcode);
    static final native int FT_Set_Char_Size(long face, long char_width, long char_height, int horz_resolution, int vert_resolution);
    static final native int FT_Load_Glyph(long face, int glyph_index, int load_flags);
    static final native void FT_Set_Transform(long face, FT_Matrix matrix, long delta_x, long delta_y);
    static final native FT_GlyphSlotRec getGlyphSlot(long face);
    static final native byte[] getBitmapData(long face);
    static final native boolean isPangoEnabled();
    static final native boolean isHarfbuzzEnabled();
}
