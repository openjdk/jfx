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

package com.sun.javafx.font.freetype;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import com.sun.javafx.font.CompositeFontResource;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.text.GlyphLayout;
import com.sun.javafx.text.PrismTextLayout;
import com.sun.javafx.text.TextRun;

class PangoGlyphLayout extends GlyphLayout {

    private int getSlot(PGFont font, PangoGlyphString glyphString) {
        CompositeFontResource fr = (CompositeFontResource)font.getFontResource();
        long fallbackFont = glyphString.font;
        long fallbackFd = OSPango.pango_font_describe(fallbackFont);
        String fallbackFamily = OSPango.pango_font_description_get_family(fallbackFd);
        int fallbackStyle = OSPango.pango_font_description_get_style(fallbackFd);
        int fallbackWeight = OSPango.pango_font_description_get_weight(fallbackFd);
        OSPango.pango_font_description_free(fallbackFd);
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

    protected TextRun addTextRun(PrismTextLayout layout, char[] chars, int start,
                                 int length, PGFont font, TextSpan span, byte level) {

        TextRun textRun = null;
        Charset utf8 = StandardCharsets.UTF_8;
        CharsetEncoder encoder = utf8.newEncoder();
        CharBuffer in = CharBuffer.wrap(chars, start, length);
        int capacity = (int)(length * (double)encoder.averageBytesPerChar());
        ByteBuffer out = ByteBuffer.allocateDirect(capacity);
        CoderResult result = encoder.encode(in, out, true);
        if (result.isOverflow()) {
            capacity = (int)(length * (double)encoder.maxBytesPerChar());
            in = CharBuffer.wrap(chars, start, length);
            out = ByteBuffer.allocateDirect(capacity);
            encoder.encode(in, out, true);
            if (PrismFontFactory.debugFonts) {
                System.err.println("[PANGO] ByteBuffer capacity increased " + out);
            }
        }

        FontResource fr = font.getFontResource();
        boolean composite = fr instanceof CompositeFontResource;
        if (composite) {
            fr = ((CompositeFontResource)fr).getSlotResource(0);
        }

        long fontmap = OSPango.pango_ft2_font_map_new();
        long context = OSPango.pango_font_map_create_context(fontmap);
        float size = font.getSize();
        int style = fr.isItalic() ? OSPango.PANGO_STYLE_ITALIC : OSPango.PANGO_STYLE_NORMAL;
        int weight = fr.isBold() ? OSPango.PANGO_WEIGHT_BOLD : OSPango.PANGO_WEIGHT_NORMAL;
        long desc = OSPango.pango_font_description_new();
        OSPango.pango_font_description_set_family(desc, fr.getFamilyName());
        OSPango.pango_font_description_set_absolute_size(desc, size * OSPango.PANGO_SCALE);
        OSPango.pango_font_description_set_stretch(desc, OSPango.PANGO_STRETCH_NORMAL);
        OSPango.pango_font_description_set_style(desc, style);
        OSPango.pango_font_description_set_weight(desc, weight);
        long attrList = OSPango.pango_attr_list_new();
        long attr = OSPango.pango_attr_font_desc_new(desc);
        OSPango.pango_attr_list_insert(attrList, attr);
        if (!composite) {
            attr = OSPango.pango_attr_fallback_new(false);
            OSPango.pango_attr_list_insert(attrList, attr);
        }
        long runs = OSPango.pango_itemize(context, out, 0, out.position(), attrList, 0);
        if (runs != 0) {
            int runsCount = OSPango.g_list_length(runs);
            int runStart = start;
            for (int i = 0; i < runsCount; i++) {
                long pangoItem = OSPango.g_list_nth_data(runs, i);
                PangoGlyphString glyphString = OSPango.pango_shape(out, pangoItem);
                OSPango.pango_item_free(pangoItem);
                if (glyphString != null) {
                    int slot = composite ? getSlot(font, glyphString) : 0;
                    int glyphCount = glyphString.num_glyphs;
                    int[] glyphs = new int[glyphCount];
                    float[] pos = new float[glyphCount*2+2];
                    PangoGlyphInfo info = null;
                    int k = 2;
                    int width = 0;
                    for (int j = 0; j < glyphCount; j++) {
                        info = glyphString.glyphs[j];
                        if (slot != -1) {
                            glyphs[j] = (slot << 24) | info.glyph;
                        }
                        if (size != 0) width += info.width;
                        pos[k] = ((float)width) / OSPango.PANGO_SCALE;
                        k += 2;
                    }

                    int runLength = glyphString.num_chars;
                    textRun = new TextRun(runStart, runLength, level, true, 0, span, 0, false);
                    textRun.shape(glyphCount, glyphs, pos, glyphString.log_clusters);
                    layout.addTextRun(textRun);
                    runStart += runLength;
                }
            }
            OSPango.g_list_free(runs);
        }
        /* pango_attr_list_unref() also frees the attributes it contains */
        OSPango.pango_attr_list_unref(attrList);
        OSPango.pango_font_description_free(desc);
        OSPango.g_object_unref(context);
        OSPango.g_object_unref(fontmap);
        return textRun;
    }

    public void layout(TextRun run, PGFont font, FontStrike strike, char[] text) {
        // Nothing - complex run are analyzed by Pango during break run
    }
}
