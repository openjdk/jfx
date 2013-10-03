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

package com.sun.javafx.font.pango;

import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontFile;
import com.sun.javafx.text.GlyphLayout;

public class PangoFactory extends PrismFontFactory {

    static boolean LCD_SUPPORT;

    public static PrismFontFactory getFactory() {
        PrismFontFactory factory = null;
        long[] ptr = new long[1];
        int error = OS.FT_Init_FreeType(ptr);
        long library = ptr[0];
        int[] major = new int[1], minor = new int[1], patch = new int[1];
        if (error == 0) {
            factory = new PangoFactory();
            OS.FT_Library_Version(library, major, minor, patch);

            /* This implementation only supports LDC if freetype has support. */
            error = OS.FT_Library_SetLcdFilter(library, OS.FT_LCD_FILTER_DEFAULT);
            LCD_SUPPORT = error == 0;
            OS.FT_Done_FreeType(library);
        }
        if (PrismFontFactory.debugFonts) {
            if (factory != null) {
                String version = major[0] + "." + minor[0] + "." + patch[0];
                System.err.println("Freetype2 Loaded (version " + version + ")");
                String lcdSupport = LCD_SUPPORT ? "Enabled" : "Disabled";
                System.err.println("LCD support " + lcdSupport);
            } else {
                System.err.println("Freetype2 Failed (error " + error + ")");
            }
        }
        return factory;
    }

    private PangoFactory() {
    }

    @Override
    protected PrismFontFile createFontFile(String name, String filename,
            int fIndex, boolean register, boolean embedded, boolean copy,
            boolean tracked) throws Exception {
        return new FTFontFile(name, filename, fIndex, register,
                                 embedded, copy, tracked);
    }

    @Override
    public GlyphLayout createGlyphLayout() {
        /* Implementing a HarfbuzzGlyphLayout is needed to support
         * font features and non-hinted glyph advances.  */
        return new PangoGlyphLayout();
    }

    @Override
    public boolean isLCDTextSupported() {
        return LCD_SUPPORT && super.isLCDTextSupported();
    }

    @Override
    protected boolean registerEmbeddedFont(String path) {
        boolean result = false;

        /* Test if freetype supports the file */
        long[] ptr = new long[1];
        int error = OS.FT_Init_FreeType(ptr);
        if (error == 0) {
            long library = ptr[0];
            byte[] buffer = (path+"\0").getBytes();
            error = OS.FT_New_Face(library, buffer, 0, ptr);
            if (error == 0) {
                result = true;
                OS.FT_Done_Face(ptr[0]);
            }
            OS.FT_Done_FreeType(library);
        }
        /* Test if fontconfig supports the file  */
        if (result) {
            /* Adding the file to fontconfig so it can be used by pango */
            result = OS.FcConfigAppFontAddFile(0, path);
        }
        if (debugFonts) {
            if (result) {
                System.err.println("[PANGO] Font registration succeeded:" + path);
            } else {
                System.err.println("[PANGO] Font registration failed:" + path);
            }
        }
        return result;
    }
}
