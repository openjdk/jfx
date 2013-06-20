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

package com.sun.javafx.font.coretext;

import com.sun.javafx.font.CompositeFontResource;
import com.sun.javafx.font.CompositeStrike;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.text.GlyphLayout;
import com.sun.javafx.text.ScriptMapper;
import com.sun.javafx.text.PrismTextLayout;
import com.sun.javafx.text.TextRun;

import java.text.Bidi;
import java.util.Arrays;

import static com.sun.javafx.scene.text.TextLayout.FLAGS_ANALYSIS_VALID;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_COMPLEX;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_BIDI;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_TABS;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_EMBEDDED;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_RTL_BASE;

class CTGlyphLayout extends GlyphLayout {

    private TextRun addTextRun(PrismTextLayout layout, char[] chars, int start, int length,
                               PGFont font, TextSpan span, byte level, boolean complex) {

        TextRun textRun = null;
        if (complex) {
            /* Use CoreText to analize the run */
            long alloc = OS.kCFAllocatorDefault();
            long textRef = OS.CFStringCreateWithCharacters(alloc, chars, start, length);

            FontStrike fontStrike = font.getStrike(BaseTransform.IDENTITY_TRANSFORM);
            boolean composite = fontStrike instanceof CompositeStrike;
            if (composite) {
                fontStrike = ((CompositeStrike)fontStrike).getStrikeSlot(0);
            }
            CTFontStrike strike = (CTFontStrike)fontStrike;
            long fontRef = strike.getFontRef();

            long attributes = OS.CFDictionaryCreateMutable(alloc, 4, OS.kCFTypeDictionaryKeyCallBacks(), OS.kCFTypeDictionaryValueCallBacks());
            OS.CFDictionaryAddValue(attributes, OS.kCTFontAttributeName(), fontRef);
            /* Note that by default CoreText will apply kerning depending on the font*/
            long attString = OS.CFAttributedStringCreate(alloc, textRef, attributes);
            long lineRef = OS.CTLineCreateWithAttributedString(attString);
            OS.CFRelease(attributes);
            OS.CFRelease(attString);
            OS.CFRelease(textRef);

            long runs = OS.CTLineGetGlyphRuns(lineRef);
            long runCount = OS.CFArrayGetCount(runs);

            /*
             * Need to undo the bidi reordering done by CoreText as it will be
             * done again in text layout after wrapping.
             */
            boolean bidi = (level & 1) != 0;
            int i = bidi ? (int)runCount - 1 : 0;
            int step = bidi ? -1 : 1;
            for (; 0 <= i && i < runCount; i += step) {
                long run = OS.CFArrayGetValueAtIndex(runs, i);
                int status = OS.CTRunGetStatus(run);
                boolean runBidi = (status & OS.kCTRunStatusRightToLeft) != 0;
                if (bidi != runBidi) {
                    if (PrismFontFactory.debugFonts) {
                        System.err.println("[CoreText] not expecing bidi level to differ.");
                    }
                }
                CFRange range = OS.CTRunGetStringRange(run);
                int glyphCount = (int)OS.CTRunGetGlyphCount(run);
                int[] glyphs = OS.CTRunGetGlyphsPtr(run);

                /*
                 * The positions and indices returned by core text are
                 * relative to the line, the following native methods
                 * are custom to return values relative to the run.
                 */
                float[] positions = OS.CTRunGetPositionsPtr(run);
                int[] indices = OS.CTRunGetStringIndicesPtr(run);

                if (PrismFontFactory.debugFonts) {
                    System.err.println("Run " + i + " range " + (start+range.location) + ", " + range.length);
                    System.err.println("\tText=[" + new String(chars, start + (int)range.location, (int)range.length) + "]");
                    System.err.println("\tFont=" +strike.getFontResource());
                    System.err.println("\tStatus="+status);
                    System.err.println("\tGlyphs="+Arrays.toString(glyphs));
                    System.err.println("\tPositions="+Arrays.toString(positions));
                    System.err.println("\tIndices="+Arrays.toString(indices));
                }
                if (composite) {
                    long runAttrs = OS.CTRunGetAttributes(run);
                    long actualFont = OS.CFDictionaryGetValue(runAttrs, OS.kCTFontAttributeName());
                    String fontName = OS.CTFontCopyDisplayName(actualFont);
                    if (!fontName.equalsIgnoreCase(strike.getFontResource().getFullName())) {
                        CompositeFontResource fr = (CompositeFontResource)font.getFontResource();
                        int slot = fr.getSlotForFont(fontName) << 24;
                        if (PrismFontFactory.debugFonts) {
                            System.err.println("\tFallback front= "+ fontName + " slot=" + (slot>>24));
                        }
                        for (int j = 0; j < glyphs.length; j++) {
                            glyphs[j] |= slot;
                        }
                    }
                }
                textRun = new TextRun(start + (int)range.location, (int)range.length, level, true, 0, span, 0, false);
                textRun.shape(glyphCount, glyphs, positions, indices);
                layout.addTextRun(textRun);
            }
            OS.CFRelease(lineRef);
        } else {
            textRun = new TextRun(start, length, level, false, 0, span, 0, false);
            layout.addTextRun(textRun);
        }
        return textRun;
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

    public void layout(TextRun run, PGFont font, FontStrike strike, char[] text) {
        // Nothing - complex run are analyzed by CoreText during break run
    }
}
