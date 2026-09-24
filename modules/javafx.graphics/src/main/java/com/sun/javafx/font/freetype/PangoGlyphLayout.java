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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import com.sun.javafx.font.CompositeFontResource;
import com.sun.javafx.font.CompositeGlyphMapper;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.text.GlyphLayout;
import com.sun.javafx.text.GlyphLayoutManager;
import com.sun.javafx.text.TextRun;

class PangoGlyphLayout extends GlyphLayout {
    private static final long fontmap;

    static {
        fontmap = PangoNative.pango_ft2_font_map_new();
    }

    private int getSlot(PGFont font, PangoGlyphString glyphString) {
        CompositeFontResource fr = (CompositeFontResource)font.getFontResource();
        long fallbackFont = glyphString.font;
        long fallbackFd = PangoNative.pango_font_describe(fallbackFont);
        String fallbackFamily = PangoNative.pango_font_description_get_family(fallbackFd);
        int fallbackStyle = PangoNative.pango_font_description_get_style(fallbackFd);
        int fallbackWeight = PangoNative.pango_font_description_get_weight(fallbackFd);
        PangoNative.pango_font_description_free(fallbackFd);
        boolean bold = fallbackWeight == OSPango.PANGO_WEIGHT_BOLD;
        boolean italic = fallbackStyle != OSPango.PANGO_STYLE_NORMAL;

        PrismFontFactory prismFactory = PrismFontFactory.getFontFactory();
        PGFont fallbackPGFont = prismFactory.createFont(fallbackFamily, bold,
                                                        italic, font.getSize());
        String fallbackFullname =  fallbackPGFont.getFullName();
        String primaryFullname = fr.getSlotResource(0).getFullName();

        int slot = 0;
        if (!fallbackFullname.equalsIgnoreCase(primaryFullname)) {
            slot = fr.getSlotForFont(fallbackFullname);
            if (PrismFontFactory.debugFonts) {
                System.err.println("\tFallback font= "+ fallbackFullname + " slot=" + (slot>>24));
            }
        }
        return slot;
    }

    private boolean check(long checkValue, String message, long context, long desc, long attrList) {
        if (checkValue != 0) return false;
        if (message != null && PrismFontFactory.debugFonts) {
            System.err.println(message);
        }
        /* pango_attr_list_unref() also frees the attributes it contains */
        if (attrList != 0) PangoNative.pango_attr_list_unref(attrList);
        if (desc != 0) PangoNative.pango_font_description_free(desc);
        if (context != 0) PangoNative.g_object_unref(context);
        return true;
    }

    private Map<TextRun, Long> runUtf8 = new LinkedHashMap<>();
    @Override
    public void layout(TextRun run, PGFont font, FontStrike strike, char[] text) {
        /* Create the pango font and attribute list */
        FontResource fr = font.getFontResource();
        boolean composite = fr instanceof CompositeFontResource;
        if (composite) {
            fr = ((CompositeFontResource)fr).getSlotResource(0);
        }
        if (check(fontmap, "Failed allocating PangoFontMap.", 0, 0, 0)) {
            return;
        }
        long context = PangoNative.pango_font_map_create_context(fontmap);
        if (check(context, "Failed allocating PangoContext.", 0, 0, 0)) {
            return;
        }
        boolean rtl = (run.getLevel() & 1) != 0;
        if (rtl) {
            PangoNative.pango_context_set_base_dir(context, OSPango.PANGO_DIRECTION_RTL);
        }
        float size = font.getSize();
        int style = fr.isItalic() ? OSPango.PANGO_STYLE_ITALIC : OSPango.PANGO_STYLE_NORMAL;
        int weight = fr.isBold() ? OSPango.PANGO_WEIGHT_BOLD : OSPango.PANGO_WEIGHT_NORMAL;
        long desc = PangoNative.pango_font_description_new();
        if (check(desc, "Failed allocating FontDescription.", context, 0, 0)) {
            return;
        }
        PangoNative.pango_font_description_set_family(desc, fr.getFamilyName());
        PangoNative.pango_font_description_set_absolute_size(desc, size * OSPango.PANGO_SCALE);
        PangoNative.pango_font_description_set_stretch(desc, OSPango.PANGO_STRETCH_NORMAL);
        PangoNative.pango_font_description_set_style(desc, style);
        PangoNative.pango_font_description_set_weight(desc, weight);
        long attrList = PangoNative.pango_attr_list_new();
        if (check(attrList, "Failed allocating PangoAttributeList.", context, desc, 0)) {
            return;
        }
        long attr = PangoNative.pango_attr_font_desc_new(desc);
        if (check(attr, "Failed allocating PangoAttribute.", context, desc, attrList)) {
            return;
        }
        PangoNative.pango_attr_list_insert(attrList, attr);
        if (!composite) {
            attr = PangoNative.pango_attr_fallback_new(false);
            PangoNative.pango_attr_list_insert(attrList, attr);
        }

        Long str = runUtf8.get(run);
        if (str == null) {
            char[] rtext = Arrays.copyOfRange(text, run.getStart(), run.getEnd());
            str = PangoNative.g_utf16_to_utf8(rtext);
            if (check(str, "Failed allocating UTF-8 buffer.", context, desc, attrList)) {
                return;
            }
            runUtf8.put(run, str);
        }

        /* Itemize */
        long utflen = PangoNative.g_utf8_strlen(str,-1);
        long end = PangoNative.g_utf8_offset_to_pointer(str, utflen);
        long runs = PangoNative.pango_itemize(context, str, 0, (int)(end - str), attrList, 0);

        if (runs != 0) {
            /* Shape all PangoItem into PangoGlyphString */
            int runsCount = PangoNative.g_list_length(runs);
            PangoGlyphString[] pangoGlyphs = new PangoGlyphString[runsCount];
            for (int i = 0; i < runsCount; i++) {
                long pangoItem = PangoNative.g_list_nth_data(runs, i);
                if (pangoItem != 0) {
                    pangoGlyphs[i] = PangoNative.pango_shape(str, pangoItem);
                    PangoNative.pango_item_free(pangoItem);
                }
            }
            PangoNative.g_list_free(runs);

            int glyphCount = 0;
            for (PangoGlyphString g : pangoGlyphs) {
                if (g != null) {
                    glyphCount += g.num_glyphs;
                }
            }
            int[] glyphs = new int[glyphCount];
            float[] pos = new float[glyphCount * 2 + 2];
            int[] indices = new int[glyphCount];
            int gi = 0;
            int ci = rtl ? run.getLength() : 0;
            int width = 0;
            for (PangoGlyphString g : pangoGlyphs) {
                if (g != null) {
                    int slot = composite ? getSlot(font, g) : 0;
                    if (rtl) ci -= g.num_chars;
                    for (int i = 0; i < g.num_glyphs; i++) {
                        int gii = gi + i;
                        if (slot != -1) {
                            int gg = g.glyphs[i];

                            /* Ignoring any glyphs outside the GLYPHMASK range.
                             * Note that Pango uses PANGO_GLYPH_EMPTY (0x0FFFFFFF), PANGO_GLYPH_INVALID_INPUT (0xFFFFFFFF),
                             * and other values with special meaning.
                             */
                            if (0 <= gg && gg <= CompositeGlyphMapper.GLYPHMASK) {
                                glyphs[gii] = (slot << 24) | gg;
                            }
                        }
                        if (size != 0) {
                            width += g.widths[i];
                            pos[2 + (gii << 1)] = ((float)width) / OSPango.PANGO_SCALE;
                        }
                        indices[gii] = g.log_clusters[i] + ci;
                    }
                    if (!rtl) ci += g.num_chars;
                    gi += g.num_glyphs;
                }
            }
            run.shape(glyphCount, glyphs, pos, indices);
        }

        check(0, null, context, desc, attrList);
    }

    @Override
    public void dispose() {
        GlyphLayoutManager.dispose(this);

        for (Long str: runUtf8.values()) {
            PangoNative.g_free(str);
        }
        runUtf8.clear();
    }
}
