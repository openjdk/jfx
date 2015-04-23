/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.font.Disposer;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrikeDesc;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontFile;
import com.sun.javafx.font.PrismFontStrike;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.transform.BaseTransform;

class FTFontFile extends PrismFontFile {
    /*
     * Font files can be accessed by several threads. In general this are the
     * JFX thread (measuring) and the Prism thread (rendering). But, if a Text
     * node is no connected a Scene it can be used in any user thread, thus a
     * font resource can be accessed from any thread.
     *
     * Freetype resources are not thread safe. In this implementation each font
     * resources has its own FT_Face and FT_Library, and while it permits these
     * resources be used by different threads it does not allow concurrent access.
     * This is enforced by converging all operation (from FTFontStrike and
     * FTGlyph) to this object and synchronizing the access to the native
     * resources using the same lock.
     */
    private long library;
    private long face;
    private FTDisposer disposer;

    FTFontFile(String name, String filename, int fIndex, boolean register,
               boolean embedded, boolean copy, boolean tracked) throws Exception {
        super(name, filename, fIndex, register, embedded, copy, tracked);
        init();
    }

    private synchronized void init() throws Exception {
        long[] ptr = new long[1];
        int error = OSFreetype.FT_Init_FreeType(ptr);
        if (error != 0) {
            throw new Exception("FT_Init_FreeType Failed error " + error);
        }
        library = ptr[0];
        if (FTFactory.LCD_SUPPORT) {
            OSFreetype.FT_Library_SetLcdFilter(library, OSFreetype.FT_LCD_FILTER_DEFAULT);
        }

        String file = getFileName();
        int fontIndex = getFontIndex();
        /* Freetype expects 'a standard C string' */
        byte[] buffer = (file+"\0").getBytes();
        error = OSFreetype.FT_New_Face(library, buffer, fontIndex, ptr);
        if (error != 0) {
            throw new Exception("FT_New_Face Failed error " + error +
                                " Font File " + file +
                                " Font Index " + fontIndex);
        }
        face = ptr[0];

        if (!isRegistered()) {
            disposer = new FTDisposer(library, face);
            Disposer.addRecord(this, disposer);
        }
    }

    @Override
    protected PrismFontStrike<?> createStrike(float size, BaseTransform transform,
                                              int aaMode, FontStrikeDesc desc) {
        return new FTFontStrike(this, size, transform, aaMode, desc);
    }

    @Override
    protected synchronized int[] createGlyphBoundingBox(int gc) {
        int flags = OSFreetype.FT_LOAD_NO_SCALE;
        OSFreetype.FT_Load_Glyph(face, gc, flags);
        int[] bbox = new int[4];
        FT_GlyphSlotRec glyphRec = OSFreetype.getGlyphSlot(face);
        if (glyphRec != null && glyphRec.metrics != null) {
            FT_Glyph_Metrics gm = glyphRec.metrics;
            bbox[0] = (int)gm.horiBearingX;
            bbox[1] = (int)(gm.horiBearingY - gm.height);
            bbox[2] = (int)(gm.horiBearingX + gm.width);
            bbox[3] = (int)gm.horiBearingY;
        }
        return bbox;
    }

    synchronized Path2D createGlyphOutline(int gc, float size) {
        int size26dot6 = (int)(size * 64);
        OSFreetype.FT_Set_Char_Size(face, 0, size26dot6, 72, 72);
        int flags = OSFreetype.FT_LOAD_NO_HINTING | OSFreetype.FT_LOAD_NO_BITMAP | OSFreetype.FT_LOAD_IGNORE_TRANSFORM;
        OSFreetype.FT_Load_Glyph(face, gc, flags);
        return OSFreetype.FT_Outline_Decompose(face);
    }

    synchronized void initGlyph(FTGlyph glyph, FTFontStrike strike) {
        float size = strike.getSize();
        if (size == 0) {
            glyph.buffer = new byte[0];
            glyph.bitmap = new FT_Bitmap();
            return;
        }
        int size26dot6 = (int)(size * 64);
        OSFreetype.FT_Set_Char_Size(face, 0, size26dot6, 72, 72);

        boolean lcd = strike.getAAMode() == FontResource.AA_LCD &&
                      FTFactory.LCD_SUPPORT;

        int flags = OSFreetype.FT_LOAD_RENDER | OSFreetype.FT_LOAD_NO_HINTING | OSFreetype.FT_LOAD_NO_BITMAP;
        FT_Matrix matrix = strike.matrix;
        if (matrix != null) {
            OSFreetype.FT_Set_Transform(face, matrix, 0, 0);
        } else {
            flags |= OSFreetype.FT_LOAD_IGNORE_TRANSFORM;
        }
        if (lcd) {
            flags |= OSFreetype.FT_LOAD_TARGET_LCD;
        } else {
            flags |= OSFreetype.FT_LOAD_TARGET_NORMAL;
        }

        int glyphCode = glyph.getGlyphCode();
        int error = OSFreetype.FT_Load_Glyph(face, glyphCode, flags);
        if (error != 0) {
            if (PrismFontFactory.debugFonts) {
                System.err.println("FT_Load_Glyph failed " + error +
                                   " glyph code " + glyphCode +
                                   " load falgs " + flags);
            }
            return;
        }

        FT_GlyphSlotRec glyphRec = OSFreetype.getGlyphSlot(face);
        if (glyphRec == null) return;
        FT_Bitmap bitmap = glyphRec.bitmap;
        if (bitmap == null) return;
        int pixelMode = bitmap.pixel_mode;
        int width = bitmap.width;
        int height = bitmap.rows;
        int pitch = bitmap.pitch;
        if (pixelMode != OSFreetype.FT_PIXEL_MODE_GRAY && pixelMode != OSFreetype.FT_PIXEL_MODE_LCD) {
            /* This procedure only requests FT_RENDER_MODE_NORMAL and FT_RENDER_MODE_LCD,
             * and for its output is expects FT_PIXEL_MODE_GRAY and FT_PIXEL_MODE_LCD, respectively.
             * But it is possible the requested rendering mode is not supported and a different
             * output is returned by Freetype. For example, a FT_PIXEL_MODE_MONO can be returned
             * if the font contains a bitmap for the given glyph code.
             */
            if (PrismFontFactory.debugFonts) {
                System.err.println("Unexpected pixel mode: " + pixelMode +
                                   " glyph code " + glyphCode +
                                   " load falgs " + flags);
            }
            return;
        }
        byte[] buffer;
        if (width != 0 && height != 0) {
            buffer = OSFreetype.getBitmapData(face);
            if (buffer != null && pitch != width) {
                /* Common for LCD glyphs */
                byte[] newBuffer = new byte[width * height];
                int src = 0, dst = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        newBuffer[dst + x] = buffer[src + x];
                    }
                    dst += width;
                    src += pitch;
                }
                buffer = newBuffer;
            }
        } else {
            /* white space */
            buffer = new byte[0];
        }

        glyph.buffer = buffer;
        glyph.bitmap = bitmap;
        glyph.bitmap_left = glyphRec.bitmap_left;
        glyph.bitmap_top = glyphRec.bitmap_top;
        glyph.advanceX = glyphRec.advance_x / 64f;    /* Fixed 26.6*/
        glyph.advanceY = glyphRec.advance_y / 64f;
        glyph.userAdvance = glyphRec.linearHoriAdvance / 65536.0f; /* Fixed 16.16 */
        glyph.lcd = lcd;
    }
}
