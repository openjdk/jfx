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

import static com.sun.javafx.scene.text.TextLayout.FLAGS_ANALYSIS_VALID;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_BIDI;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_COMPLEX;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_EMBEDDED;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_TABS;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_RTL_BASE;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.text.Bidi;

import com.sun.javafx.font.CompositeFontResource;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.text.GlyphLayout;
import com.sun.javafx.text.PrismTextLayout;
import com.sun.javafx.text.ScriptMapper;
import com.sun.javafx.text.TextRun;

class PangoGlyphLayout extends GlyphLayout {

    private int getSlot(PGFont font, PangoGlyphString glyphString) {
        CompositeFontResource fr = (CompositeFontResource)font.getFontResource();
        long fallbackFont = glyphString.font;
        long fallbackFd = OS.pango_font_describe(fallbackFont);
        String fallbackFamily = OS.pango_font_description_get_family(fallbackFd);
        int fallbackStyle = OS.pango_font_description_get_style(fallbackFd);
        int fallbackWeight = OS.pango_font_description_get_weight(fallbackFd);
        OS.pango_font_description_free(fallbackFd);
        boolean bold = fallbackWeight == OS.PANGO_WEIGHT_BOLD;
        boolean italic = fallbackStyle != OS.PANGO_STYLE_NORMAL;

        PrismFontFactory prismFactory = PrismFontFactory.getFontFactory();
        PGFont fallbackPGFont = prismFactory.createFont(fallbackFamily, bold,
                                                        italic, font.getSize());
        String fallbackFullname =  fallbackPGFont.getFullName();
        String primaryFullname = fr.getSlotResource(0).getFullName();

        int slot = 0;
        if (!fallbackFullname.equalsIgnoreCase(primaryFullname)) {
            slot = fr.getSlotForFont(fallbackFullname) << 24;
            if (PrismFontFactory.debugFonts) {
                System.err.println("\tFallback front= "+ fallbackFullname + " slot=" + (slot>>24));
            }
        }
        return slot;
    }

    private TextRun addTextRun(PrismTextLayout layout, char[] chars, int start, int length,
                               PGFont font, TextSpan span, byte level, boolean complex) {

        TextRun textRun = null;
        if (complex) {
            Charset utf8 = StandardCharsets.UTF_8;
            CharsetEncoder encoder = utf8.newEncoder();
            CharBuffer in = CharBuffer.wrap(chars, start, length);
            int capacity = (int)(length * (double)encoder.averageBytesPerChar());
            ByteBuffer out = ByteBuffer.allocateDirect(capacity);
            CoderResult result = encoder.encode(in, out, true);
            if (result.isOverflow()) {
                capacity = (int)(length * (double)encoder.maxBytesPerChar());
                in.rewind();
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

            long fontmap = OS.pango_ft2_font_map_new();
            long context = OS.pango_font_map_create_context(fontmap);
            float size = font.getSize();
            int style = fr.isItalic() ? OS.PANGO_STYLE_ITALIC : OS.PANGO_STYLE_NORMAL;
            int weight = fr.isBold() ? OS.PANGO_WEIGHT_BOLD : OS.PANGO_WEIGHT_NORMAL;
            long desc = OS.pango_font_description_new();
            OS.pango_font_description_set_family(desc, fr.getFamilyName());
            OS.pango_font_description_set_absolute_size(desc, size * OS.PANGO_SCALE);
            OS.pango_font_description_set_stretch(desc, OS.PANGO_STRETCH_NORMAL);
            OS.pango_font_description_set_style(desc, style);
            OS.pango_font_description_set_weight(desc, weight);
            long attrList = OS.pango_attr_list_new();
            long attr = OS.pango_attr_font_desc_new(desc);
            OS.pango_attr_list_insert(attrList, attr);
            if (!composite) {
                attr = OS.pango_attr_fallback_new(false);
                OS.pango_attr_list_insert(attrList, attr);
            }
            long runs = OS.pango_itemize(context, out, 0, out.position(), attrList, 0);
            int runsCount = OS.g_list_length(runs);
            int runStart = start;
            for (int i = 0; i < runsCount; i++) {
                long pangoItem = OS.g_list_nth_data(runs, i);
                PangoGlyphString glyphString = OS.pango_shape(out, pangoItem);
                OS.pango_item_free(pangoItem);
                int slot = composite ? getSlot(font, glyphString) : 0;
                int glyphCount = glyphString.num_glyphs;
                int[] glyphs = new int[glyphCount];
                float[] pos = new float[glyphCount*2+2];
                PangoGlyphInfo info = null;
                int k = 2;
                int width = 0;
                for (int j = 0; j < glyphCount; j++) {
                    info = glyphString.glyphs[j];
                    glyphs[j] = slot | info.glyph;
                    width += info.width;
                    pos[k] = ((float)width) / OS.PANGO_SCALE;
                    k += 2;
                }

                int runLength = glyphString.num_chars;
                textRun = new TextRun(runStart, runLength, level, true, 0, span, 0, false);
                textRun.shape(glyphCount, glyphs, pos, glyphString.log_clusters);
                layout.addTextRun(textRun);
                runStart += runLength;
            }
            OS.g_list_free(runs);
            /* pango_attr_list_unref() also frees the attributes it contains */
            OS.pango_attr_list_unref(attrList);
            OS.pango_font_description_free(desc);
            OS.g_object_unref(context);
            OS.g_object_unref(fontmap);
        } else {
            textRun = new TextRun(start, length, level, false, 0, span, 0, false);
            layout.addTextRun(textRun);
        }
        return textRun;
    }

    public void layout(TextRun run, PGFont font, FontStrike strike, char[] text) {
        // Nothing - complex run are analyzed by Pango during break run
    }

    public int breakRuns(PrismTextLayout layout, char[] chars, int flags) {
        int length = chars.length;
        boolean complexRun = false;
        boolean complex = false;
        boolean feature = false;

        boolean checkComplex = true;
        boolean checkBidi = true;
        if ((flags & FLAGS_ANALYSIS_VALID) != 0) {
            /* Avoid work when it is known neither complex
             * text nor bidi are not present. */
            checkComplex = (flags & FLAGS_HAS_COMPLEX) != 0;
            checkBidi = (flags & FLAGS_HAS_BIDI) != 0;
        }

        TextRun run = null;
        Bidi bidi = null;
        byte bidiLevel = 0;
        int bidiEnd = length;
        int bidiIndex = 0;
        int spanIndex = 0;
        TextSpan span = null;
        int spanEnd = length;
        PGFont font = null;
        TextSpan[] spans = layout.getTextSpans();
        if (spans != null) {
            if (spans.length > 0) {
                span = spans[spanIndex];
                spanEnd = span.getText().length();
                font = (PGFont)span.getFont();
                if (font == null) {
                    flags |= FLAGS_HAS_EMBEDDED;
                }
            }
        } else {
            font = layout.getFont();
        }
        if (font != null) {
            FontResource fr = font.getFontResource();
            int requestedFeatures = font.getFeatures();
            int supportedFeatures = fr.getFeatures();
            feature = (requestedFeatures & supportedFeatures) != 0;
        }
        if (checkBidi && length > 0) {
            int direction = layout.getDirection();
            bidi = new Bidi(chars, 0, null, 0, length, direction);
            /* Temporary Code: See RT-26997 */
//            bidiLevel = (byte)bidi.getRunLevel(bidiIndex);
            bidiLevel = (byte)bidi.getLevelAt(bidi.getRunStart(bidiIndex));
            bidiEnd = bidi.getRunLimit(bidiIndex);
            if ((bidiLevel & 1) != 0) {
                flags |= FLAGS_HAS_BIDI;
            }
        }

        int start = 0;
        int i = 0;
        while (i < length) {
            char ch = chars[i];
            int codePoint = ch;

            boolean delimiterChanged = ch == '\t' || ch == '\n' || ch == '\r';
            boolean spanChanged = i >= spanEnd;
            boolean levelChanged = i >= bidiEnd;
            boolean complexChanged = false;

            if (checkComplex) {
                if (Character.isHighSurrogate(ch)) {
                    /* Only merge surrogate when the pair is in the same span. */
                    if (i + 1 < spanEnd && Character.isLowSurrogate(chars[i + 1])) {
                        codePoint = Character.toCodePoint(ch, chars[++i]);
                    }
                }
                if (Character.isWhitespace(codePoint)) {
                    complex = feature || complexRun;
                } else {
                    complex = feature || ScriptMapper.isComplexCharCode(codePoint);
                }
                complexChanged = complex != complexRun;
            }

            if (delimiterChanged || spanChanged || levelChanged || complexChanged) {

                /* Create text run */
                if (i != start) {
                    run = addTextRun(layout, chars, start, i - start,
                                     font, span, bidiLevel, complexRun);
                    if (complexRun) {
                        flags |= FLAGS_HAS_COMPLEX;
                    }
                    start = i;
                }

                if (delimiterChanged) {
                    i++;
                    /* Only merge \r\n when the are in the same text span */
                    if (ch == '\r' && i < spanEnd && chars[i] == '\n') {
                        i++;
                    }

                    /* Create delimiter run */
                    run = new TextRun(start, i - start, bidiLevel, false,
                                      ScriptMapper.COMMON, span, 0, false);
                    if (ch == '\t') {
                        run.setTab();
                        flags |= FLAGS_HAS_TABS;
                    } else {
                        run.setLinebreak();
                    }
                    layout.addTextRun(run);

                    start = i;
                    if (i == length) break;
                    spanChanged = i >= spanEnd;
                    levelChanged = i >= bidiEnd;
                }
                if (spanChanged) {
                    /* Only true for rich text (spans != null) */
                    span = spans[++spanIndex];
                    spanEnd += span.getText().length();
                    font = (PGFont)span.getFont();
                    if (font == null) {
                        flags |= FLAGS_HAS_EMBEDDED;
                    } else {
                        FontResource fr = font.getFontResource();
                        int requestedFeatures = font.getFeatures();
                        int supportedFeatures = fr.getFeatures();
                        feature = (requestedFeatures & supportedFeatures) != 0;
                    }
                }
                if (levelChanged) {
                    bidiIndex++;
                    /* Temporary Code: See RT-26997 */
//                    bidiLevel = (byte)bidi.getRunLevel(bidiIndex);
                    bidiLevel = (byte)bidi.getLevelAt(bidi.getRunStart(bidiIndex));
                    bidiEnd = bidi.getRunLimit(bidiIndex);
                    if ((bidiLevel & 1) != 0) {
                        flags |= FLAGS_HAS_BIDI;
                    }
                }

                if (complexChanged) {
                    if (delimiterChanged) {
                        ch = chars[i]; /* update ch because of delimiterChanged */
                        if (Character.isHighSurrogate(ch)) {
                            /* Only merge surrogate when the pair is in the same span */
                            if (i + 1 < spanEnd && Character.isLowSurrogate(chars[i + 1])) {
                                codePoint = Character.toCodePoint(ch, chars[++i]);
                            }
                        }
                        if (Character.isWhitespace(codePoint)) {
                            complex = feature || complexRun;
                        } else {
                            complex = feature || ScriptMapper.isComplexCharCode(codePoint);
                        }
                    }
                    complexRun = complex;
                }
            }
            if (!delimiterChanged) i++;
        }

        /* Create final text run */
        if (start < length) {
            addTextRun(layout, chars, start, length - start,
                       font, span, bidiLevel, complexRun);
            if (complexRun) {
                flags |= FLAGS_HAS_COMPLEX;
            }
        } else {
            /* Ensure every lines has at least one run */
            if (run == null || run.isLinebreak()) {
                run = new TextRun(start, 0, (byte)0, false,
                                  ScriptMapper.COMMON, span, 0, false);
                layout.addTextRun(run);
            }
        }
        if (bidi != null) {
            if (!bidi.baseIsLeftToRight()) {
                flags |= FLAGS_RTL_BASE;
            }
        }
        flags |= FLAGS_ANALYSIS_VALID;
        return flags;
    }
}
