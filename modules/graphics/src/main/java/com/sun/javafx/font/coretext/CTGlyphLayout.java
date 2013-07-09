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
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.text.GlyphLayout;
import com.sun.javafx.text.PrismTextLayout;
import com.sun.javafx.text.TextRun;

import java.util.Arrays;

class CTGlyphLayout extends GlyphLayout {

    protected TextRun addTextRun(PrismTextLayout layout, char[] chars, int start,
                                 int length, PGFont font, TextSpan span, byte level) {

        TextRun textRun = null;

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
        return textRun;
    }

    public void layout(TextRun run, PGFont font, FontStrike strike, char[] text) {
        // Nothing - complex run are analyzed by CoreText during break run
    }
}
