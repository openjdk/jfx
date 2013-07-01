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
import static com.sun.javafx.scene.text.TextLayout.*;

public class DWGlyphLayout extends GlyphLayout {

    private static final String LOCALE = "en-us";

    private TextRun addTextRun(PrismTextLayout layout, char[] chars, int start, int length,
                               PGFont font, TextSpan span, byte level, boolean complex) {
        TextRun textRun = null;
        if (complex) {
            int dir = (level & 1) != 0 ? OS.DWRITE_READING_DIRECTION_RIGHT_TO_LEFT :
                                         OS.DWRITE_READING_DIRECTION_LEFT_TO_RIGHT;
            JFXTextAnalysisSink sink = OS.NewJFXTextAnalysisSink(chars, start, length, LOCALE, dir);
            sink.AddRef();

            IDWriteFactory factory = DWFactory.getDWriteFactory();
            IDWriteTextAnalyzer analyzer = factory.CreateTextAnalyzer();
            analyzer.AnalyzeScript(sink, 0, length, sink);

            while (sink.Next()) {
                int runStart = sink.GetStart();
                int runLength = sink.GetLength();
                DWRITE_SCRIPT_ANALYSIS analysis = sink.GetAnalysis();
                textRun = new TextRun(start + runStart, runLength, level, true,
                                      analysis.script, span,
                                      analysis.shapes, false);
                layout.addTextRun(textRun) ;
            }

            analyzer.Release();
            sink.Release();
        } else {
            textRun = new TextRun(start, length, level, false, 0, span, 0, false);
            layout.addTextRun(textRun);
        }
        return textRun;
    }

    public void layout(TextRun run, PGFont font, FontStrike strike, char[] text) {
        /* ignore typographic feature for now */
        long[] features = null;
        int[] featuresRangeLengths = null;
        int featuresCount = 0;

        int length = run.getLength();
        short[] clusterMap = new short[length];
        short[] textProps = new short[length];
        int maxGlyphs = (length * 3 / 2) + 16;
        short[] glyphs = new short[maxGlyphs];
        short[] glyphProps = new short[maxGlyphs];
        int[] retGlyphcount = new int[1];
        boolean rtl = !run.isLeftToRight();

        IDWriteFactory factory = DWFactory.getDWriteFactory();
        IDWriteTextAnalyzer analyzer = factory.CreateTextAnalyzer();
        FontResource fr = font.getFontResource();
        boolean composite = fr instanceof CompositeFontResource;
        if (composite) {
            fr = ((CompositeFontResource)fr).getSlotResource(0);
        }
        IDWriteFontFace face = ((DWFontFile)fr).getFontFace();
        DWRITE_SCRIPT_ANALYSIS analysis = new DWRITE_SCRIPT_ANALYSIS();
        analysis.script = (short)run.getScript();
        analysis.shapes = run.getSlot();

        int start = run.getStart();
        int hr = analyzer.GetGlyphs(text, start, length, face, false, rtl, analysis, null,
                                    0, features, featuresRangeLengths, featuresCount,
                                    maxGlyphs, clusterMap, textProps, glyphs, glyphProps, retGlyphcount);

        if (hr == OS.E_NOT_SUFFICIENT_BUFFER) {
            /* double the buffer size and try again */
            maxGlyphs *= 2;
            glyphs = new short[maxGlyphs];
            glyphProps = new short[maxGlyphs];
            hr = analyzer.GetGlyphs(text, start, length, face, false, rtl, analysis, null,
                                    0, features, featuresRangeLengths, featuresCount,
                                    maxGlyphs, clusterMap, textProps, glyphs, glyphProps, retGlyphcount);
        }

        if (hr != OS.S_OK) {
            analyzer.Release();
            return;
        }
        int glyphCount = retGlyphcount[0];

        /* Adjust glyphs & checking for missing glyphs */
        int step = rtl ? -1 : 1;
        int i, j;
        int[] iglyphs = new int[glyphCount];
        boolean missingGlyph = false;
        i = 0; j = rtl ? glyphCount - 1 : 0;
        while (i < glyphCount) {
            iglyphs[i] = glyphs[j];
            if (iglyphs[i] == 0) {
                missingGlyph = true;
                break;
            }
            i++;
            j+=step;
        }
        if (missingGlyph && composite) {
            analyzer.Release();
            renderShape(text, run, font);
            return;
        }

        float size = font.getSize();
        float[] advances = new float[glyphCount];
        float[] glyphOffsets = new float[glyphCount * 2];
        analyzer.GetGlyphPlacements(text, clusterMap, textProps, start, length, glyphs,
                                    glyphProps, glyphCount, face, size, false, rtl,
                                    analysis, null, features, featuresRangeLengths,
                                    featuresCount, advances, glyphOffsets);

        /* Adjust glyph indices */
        int[] indices = new int[length];
        i = 0; j = rtl ? length - 1 : 0;
        while (i < length) {
            indices[i] = clusterMap[j];
            i++;
            j+=step;
        }

        /* Adjust glyphs positions */
        float[] pos = new float[glyphCount * 2 + 2];
        i = 0; j = rtl ? glyphCount - 1 : 0;
        float x = 0;
        while (i < pos.length - 2) {
            pos[i++] = x;
            pos[i++] = 0;
            x += advances[j];
            j+=step;
        }
        pos[i++] = x;
        pos[i++] = 0;

        analyzer.Release();
        run.shape(glyphCount, iglyphs, pos, indices);
    }

    private void renderShape(char[] text, TextRun run, PGFont font) {
        String family = font.getFamilyName();
        CompositeFontResource fr = (CompositeFontResource)font.getFontResource();
        int weight = fr.isBold() ? OS.DWRITE_FONT_WEIGHT_BOLD :
                                   OS.DWRITE_FONT_WEIGHT_NORMAL;
        int stretch = OS.DWRITE_FONT_STRETCH_NORMAL;
        int style = fr.isItalic() ? OS.DWRITE_FONT_STYLE_ITALIC :
                                    OS.DWRITE_FONT_STYLE_NORMAL;
        float size = font.getSize();

        IDWriteFactory factory = DWFactory.getDWriteFactory();
        /* Note this collection is not correct for embedded fonts,
         * currently this is not a problem since embedded fonts do
         * not have fallbacks.
         */
        IDWriteFontCollection collection = DWFactory.getFontCollection();

        IDWriteTextFormat format = factory.CreateTextFormat(family,
                                                            collection,
                                                            weight,
                                                            style,
                                                            stretch,
                                                            size,
                                                            LOCALE);

        int start = run.getStart();
        int length = run.getLength();
        IDWriteTextLayout layout = factory.CreateTextLayout(text, start, length, format, 100000, 100000);
        JFXTextRenderer renderer = OS.NewJFXTextRenderer();
        renderer.AddRef();
        layout.Draw(0, renderer, 0, 0);

        int totalGlyphCount = renderer.GetTotalGlyphCount();
        int[] glyphs = new int[totalGlyphCount];
        float[] advances = new float[totalGlyphCount];
        int[] clusterMap = new int[length];
        int glyphStart = 0;
        int textStart = 0;
        while (renderer.Next()) {
            IDWriteFontFace fallback = renderer.GetFontFace();
            IDWriteFont fallbackFont = collection.GetFontFromFontFace(fallback);
            IDWriteFontFamily fallbackFontFamily = fallbackFont.GetFontFamily();
            IDWriteLocalizedStrings names = fallbackFontFamily.GetFamilyNames();
            int localeIndex = names.FindLocaleName(LOCALE);
            int nameSize = names.GetStringLength(localeIndex);
            String fallbackFamily = names.GetString(localeIndex, nameSize);
            boolean italic = fallbackFont.GetStyle() != OS.DWRITE_FONT_STYLE_NORMAL;
            boolean bold = fallbackFont.GetWeight() > OS.DWRITE_FONT_WEIGHT_NORMAL;
            names.Release();
            fallbackFontFamily.Release();
            fallbackFont.Release();
            PrismFontFactory prismFactory = PrismFontFactory.getFontFactory();
            PGFont fallbackPGFont = prismFactory.createFont(fallbackFamily, bold, italic, size);
            String fallbackFullname =  fallbackPGFont.getFullName();

            int slot = 0;
            if (!fallbackFullname.equalsIgnoreCase(fr.getFullName())) {
                slot = fr.getSlotForFont(fallbackFullname) << 24;
                if (PrismFontFactory.debugFonts) {
                    System.err.println("\tFallback front= "+ fallbackFullname + " slot=" + (slot>>24));
                }
            }
            renderer.GetGlyphIndices(glyphs, glyphStart, slot);
            renderer.GetGlyphAdvances(advances, glyphStart);
            renderer.GetClusterMap(clusterMap, textStart);
            glyphStart += renderer.GetGlyphCount();
            textStart += renderer.GetLength();
        }
        renderer.Release();
        layout.Release();
        format.Release();

        /* Adjust glyphs positions */
        float[] pos = new float[totalGlyphCount * 2 + 2];
        boolean rtl = !run.isLeftToRight();
        int i = 0, j = rtl ? totalGlyphCount - 1 : 0;
        int step = rtl ? -1 : 1;
        float x = 0;
        while (i < pos.length - 2) {
            pos[i++] = x;
            pos[i++] = 0;
            x += advances[j];
            j+=step;
        }
        pos[i++] = x;
        pos[i++] = 0;
        if (rtl) {
            /* Adjust glyphs */
            for (i = 0; i < totalGlyphCount / 2; i++) {
                int tmp = glyphs[i];
                glyphs[i] = glyphs[totalGlyphCount - i - 1];
                glyphs[totalGlyphCount - i - 1] = tmp;
            }
            /* Adjust glyph indices */
            for (i = 0; i < length / 2; i++) {
                int tmp = clusterMap[i];
                clusterMap[i] = clusterMap[length - i - 1];
                clusterMap[length - i - 1] = tmp;
            }
        }
        run.shape(totalGlyphCount, glyphs, pos, clusterMap);
    }

    /*
    * Ideally DirectWrite could be used to do the entire job.
    * Still need to verify if JFX handling of non-complex is indeed faster
    * than DirectWrite.
    *
    * (this method was copied from the CoreText implementation).
    */
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
