/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import com.sun.javafx.font.CompositeFontResource;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.text.GlyphLayout;
import com.sun.javafx.text.PrismTextLayout;
import com.sun.javafx.text.TextRun;

public class DWGlyphLayout extends GlyphLayout {

    private static final String LOCALE = "en-us";

    @Override
    protected TextRun addTextRun(PrismTextLayout layout, char[] chars, int start,
                                 int length, PGFont font, TextSpan span, byte level) {

        IDWriteFactory factory = DWFactory.getDWriteFactory();
        IDWriteTextAnalyzer analyzer = factory.CreateTextAnalyzer();
        if (analyzer == null) {
            return new TextRun(start, length, level, false, 0, span, 0, false);
        }

        int dir = (level & 1) != 0 ? OS.DWRITE_READING_DIRECTION_RIGHT_TO_LEFT :
                                     OS.DWRITE_READING_DIRECTION_LEFT_TO_RIGHT;
        JFXTextAnalysisSink sink = OS.NewJFXTextAnalysisSink(chars, start, length, LOCALE, dir);
        if (sink == null) {
            return new TextRun(start, length, level, false, 0, span, 0, false);
        }
        sink.AddRef();

        TextRun textRun = null;
        int hr = analyzer.AnalyzeScript(sink, 0, length, sink);
        if (hr == OS.S_OK) {
            while (sink.Next()) {
                int runStart = sink.GetStart();
                int runLength = sink.GetLength();
                DWRITE_SCRIPT_ANALYSIS analysis = sink.GetAnalysis();
                textRun = new TextRun(start + runStart, runLength, level, true,
                                      analysis.script, span,
                                      analysis.shapes, false);
                layout.addTextRun(textRun);
            }
        }

        analyzer.Release();
        sink.Release();
        return textRun;
    }

    public void layout(TextRun run, PGFont font, FontStrike strike, char[] text) {

        int slot = 0;
        FontResource fr = font.getFontResource();
        boolean composite = fr instanceof CompositeFontResource;
        if (composite) {
            slot = getInitialSlot(fr);
            fr = ((CompositeFontResource)fr).getSlotResource(slot);
        }
        IDWriteFontFace face = ((DWFontFile)fr).getFontFace();
        if (face == null) return;

        IDWriteFactory factory = DWFactory.getDWriteFactory();
        IDWriteTextAnalyzer analyzer = factory.CreateTextAnalyzer();
        if (analyzer == null) return;

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
        int slotMask = slot << 24;
        boolean missingGlyph = false;
        i = 0; j = rtl ? glyphCount - 1 : 0;
        while (i < glyphCount) {
            if (glyphs[i] == 0) {
                missingGlyph = true;
                if (composite) break;
            }
            iglyphs[i] = (glyphs[j] & DWGlyph.SHORTMASK) | slotMask;
            i++;
            j+=step;
        }
        if (missingGlyph && composite) {
            analyzer.Release();
            renderShape(text, run, font, slot);
            return;
        }

        float size = font.getSize();
        float[] advances = new float[glyphCount];
        float[] offsets = new float[glyphCount * 2];
        analyzer.GetGlyphPlacements(text, clusterMap, textProps, start, length, glyphs,
                                    glyphProps, glyphCount, face, size, false, rtl,
                                    analysis, null, features, featuresRangeLengths,
                                    featuresCount, advances, offsets);
        analyzer.Release();

        float[] pos = getPositions(advances, offsets, glyphCount, rtl);
        int[] indices = getIndices(clusterMap, glyphCount, rtl);
        run.shape(glyphCount, iglyphs, pos, indices);
    }

    private float[] getPositions(float[] advances, float[] offsets, int glyphCount, boolean rtl) {
        float[] pos = new float[glyphCount * 2 + 2];
        int i = 0;
        int j = rtl ? glyphCount - 1 : 0;
        int step = rtl ? -1 : 1;
        float x = 0;
        while (i < pos.length - 2) {
            int g = j << 1;
            pos[i++] = (rtl ? -offsets[g] : offsets[g]) + x;
            pos[i++] = -offsets[g + 1];
            x += advances[j];
            j+=step;
        }
        pos[i++] = x;
        pos[i++] = 0;
        return pos;
    }

    private int[] getIndices(short[] clusterMap, int glyphCount, boolean rtl) {
        /* The cluster map array produced by DirectWrite is character offset
         * to glyph index mapping. TextRun internally requires a glyph index
         * to character offset map table. */
        int[] indices = new int[glyphCount];
        Arrays.fill(indices, -1);
        for (int i = 0; i < clusterMap.length; i++) {
            int index = clusterMap[i];
            /* keep character offset for the first glyph in the cluster */
            if (0 <= index && index < glyphCount && indices[index] == -1) {
                indices[index] = i;
            }
        }
        if (indices.length > 0) {
            if (indices[0] == -1) indices[0] = 0;
            /* use the character offset of the preceding element */
            for (int i = 1; i < indices.length; i++) {
                if (indices[i] == -1) indices[i] = indices[i - 1];
            }
        }

        if (rtl) {
            /* Flip the array for RTL */
            for (int i = 0; i < indices.length / 2; i++) {
                int tmp = indices[i];
                indices[i] = indices[indices.length - i - 1];
                indices[indices.length - i - 1] = tmp;
            }
        }
        return indices;
    }

    private String getName(IDWriteLocalizedStrings localizedStrings) {
        if (localizedStrings == null) return null;
        int index = localizedStrings.FindLocaleName(LOCALE);
        String name = null;
        if (index >= 0) {
            int size = localizedStrings.GetStringLength(index);
            name = localizedStrings.GetString(index, size);
        }
        localizedStrings.Release();
        return name;
    }

    private FontResource checkFontResource(FontResource fr, String psName,
                                           String win32Name) {
        if (fr == null) return null;

        /* Postscript name is only available on newer version of DirectWrite */
        if (psName != null) {
            if (psName.equals(fr.getPSName())) return fr;
            /* In some case the postscript name returned by DirectWrite
             * and Prism are different. For example, Leelawadee Bold is
             * reported as Leelawadee-Bold by DirectWrite and LeelawadeeBold
             * by JFX. */
        }

        if (win32Name != null) {
            if (win32Name.equals(fr.getFullName())) return fr;
            /* JFX generally omits the style name in the full name for regular
             * style. */
            String name = fr.getFamilyName() + " " + fr.getStyleName();
            if (win32Name.equals(name)) return fr;
        }
        return null;
    }

    private int getFontSlot(IDWriteFontFace face, CompositeFontResource composite,
                            String primaryFont, int slot) {
        if (face == null) return -1;
        IDWriteFontCollection collection = DWFactory.getFontCollection();
        PrismFontFactory prismFactory = PrismFontFactory.getFontFactory();


        /* Collecting information about the font */
        IDWriteFont font = collection.GetFontFromFontFace(face);
        if (font == null) return -1;
        IDWriteFontFamily fallbackFamily = font.GetFontFamily();
        String family = getName(fallbackFamily.GetFamilyNames());
        fallbackFamily.Release();
        boolean italic = font.GetStyle() != OS.DWRITE_FONT_STYLE_NORMAL;
        boolean bold = font.GetWeight() > OS.DWRITE_FONT_WEIGHT_NORMAL;
        int simulation = font.GetSimulations();
        int info = OS.DWRITE_INFORMATIONAL_STRING_POSTSCRIPT_NAME;
        String psName = getName(font.GetInformationalStrings(info));
        info = OS.DWRITE_INFORMATIONAL_STRING_WIN32_FAMILY_NAMES;
        String win32Family = getName(font.GetInformationalStrings(info));
        info = OS.DWRITE_INFORMATIONAL_STRING_WIN32_SUBFAMILY_NAMES;
        String win32SubFamily = getName(font.GetInformationalStrings(info));
        String win32Name = win32Family + " " + win32SubFamily;

        if (PrismFontFactory.debugFonts) {
            String styleName = getName(font.GetFaceNames());
            System.err.println("Mapping IDWriteFont=\"" + (family + " " + styleName) +
                               "\" Postscript name=\"" + psName +
                               "\" Win32 name=\"" + win32Name + "\"");
        }
        font.Release();

        /* Map the IDWriteFont to a Prism font and check */
        FontResource fr = prismFactory.getFontResource(family, bold, italic, false);
        fr = checkFontResource(fr, psName, win32Name);
        if (fr == null) {
            /* The most common case for the lookup to fail is due to font
             * simulations. */
            italic &= (simulation & OS.DWRITE_FONT_SIMULATIONS_OBLIQUE) == 0;
            bold &= (simulation & OS.DWRITE_FONT_SIMULATIONS_BOLD) == 0;
            fr = prismFactory.getFontResource(family, bold, italic, false);
            fr = checkFontResource(fr, psName, win32Name);
        }
        if (fr == null) {
            /* Look up by name */
            fr = prismFactory.getFontResource(win32Name, null, false);
            fr = checkFontResource(fr, psName, win32Name);
        }
        if (fr == null) {
            if (PrismFontFactory.debugFonts) {
                System.err.println("\t**** Failed to map IDWriteFont to Prism ****");
            }
            return -1;
        }

        String fallbackName = fr.getFullName();
        if (!primaryFont.equalsIgnoreCase(fallbackName)) {
            slot = composite.getSlotForFont(fallbackName);
        }
        if (PrismFontFactory.debugFonts) {
            System.err.println("\tFallback full name=\""+ fallbackName +
                               "\" Postscript name=\"" + fr.getPSName() +
                               "\" Style name=\"" + fr.getStyleName() +
                               "\" slot=" + slot);
        }
        return slot;
    }

    private void renderShape(char[] text, TextRun run, PGFont font, int baseSlot) {
        CompositeFontResource composite = (CompositeFontResource)font.getFontResource();
        FontResource fr = composite.getSlotResource(baseSlot);
        String family = fr.getFamilyName();
        String fullName = fr.getFullName();
        int weight = fr.isBold() ? OS.DWRITE_FONT_WEIGHT_BOLD :
                                   OS.DWRITE_FONT_WEIGHT_NORMAL;
        int stretch = OS.DWRITE_FONT_STRETCH_NORMAL;
        int style = fr.isItalic() ? OS.DWRITE_FONT_STYLE_ITALIC :
                                    OS.DWRITE_FONT_STYLE_NORMAL;
        float size = font.getSize();

        /* zero is not a valid size for IDWriteTextFormat */
        float fontsize = size > 0 ? size : 1;

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
                                                            fontsize,
                                                            LOCALE);
        if (format == null) return;

        int start = run.getStart();
        int length = run.getLength();
        IDWriteTextLayout layout = factory.CreateTextLayout(text, start, length, format, 100000, 100000);
        if (layout != null) {
            JFXTextRenderer renderer = OS.NewJFXTextRenderer();
            if (renderer != null) {
                renderer.AddRef();

                /* Use renderer to produce glyph information */
                layout.Draw(0, renderer, 0, 0);

                /* Read data from renderer */
                int glyphCount = renderer.GetTotalGlyphCount();
                int[] glyphs = new int[glyphCount];
                float[] advances = new float[glyphCount];
                float[] offsets = new float[glyphCount * 2];
                short[] clusterMap = new short[length];
                int glyphStart = 0;
                int textStart = 0;
                while (renderer.Next()) {
                    IDWriteFontFace fallback = renderer.GetFontFace();
                    int slot = getFontSlot(fallback, composite, fullName, baseSlot);
                    if (slot >= 0) {
                        renderer.GetGlyphIndices(glyphs, glyphStart, slot << 24);
                        renderer.GetGlyphOffsets(offsets, glyphStart * 2);
                    }
                    if (size > 0) {
                        /* Keep advances to zero if font size is zero */
                        renderer.GetGlyphAdvances(advances, glyphStart);
                    }
                    renderer.GetClusterMap(clusterMap, textStart, glyphStart);
                    glyphStart += renderer.GetGlyphCount();
                    textStart += renderer.GetLength();
                }
                renderer.Release();

                /* Converting data to be used by the JavaFX run */
                boolean rtl = !run.isLeftToRight();
                if (rtl) {
                    for (int i = 0; i < glyphCount / 2; i++) {
                        int tmp = glyphs[i];
                        glyphs[i] = glyphs[glyphCount - i - 1];
                        glyphs[glyphCount - i - 1] = tmp;
                    }
                }
                float[] pos = getPositions(advances, offsets, glyphCount, rtl);
                int[] indices = getIndices(clusterMap, glyphCount, rtl);
                run.shape(glyphCount, glyphs, pos, indices);
            }
            layout.Release();
        }
        format.Release();
    }
}
