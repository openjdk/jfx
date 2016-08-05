/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * GlyphLayout is used to process a run of text into a run of run of
 * glyphs, optionally with position and char mapping info.
 *
 * The text has already been processed for numeric shaping and bidi.
 * The run of text that layout works on has a single bidi level.  It
 * also has a single font/style.  Some operations need context to work
 * on (shaping, script resolution) so context for the text run text is
 * provided.  It is assumed that the text array contains sufficient
 * context, and the offset and count delimit the portion of the text
 * that needs to actually be processed.
 *
 * The font might be a composite font.  Layout generally requires
 * tables from a single physical font to operate, and so it must
 * resolve the 'single' font run into runs of physical fonts.
 *
 * Some characters are supported by several fonts of a composite, and
 * in order to properly emulate the glyph substitution behavior of a
 * single physical font, these characters might need to be mapped to
 * different physical fonts.  The script code that is assigned
 * characters normally considered 'common script' can be used to
 * resolve which physical font to use for these characters. The input
 * to the char to glyph mapper (which assigns physical fonts as it
 * processes the glyphs) should include the script code, and the
 * mapper should operate on runs of a single script.
 *
 * To perform layout, call get() to get a new (or reuse an old)
 * GlyphLayout, call layout on it, then call done(GlyphLayout) when
 * finished.  There's no particular problem if you don't call done,
 * but it assists in reuse of the GlyphLayout.
 */
package com.sun.javafx.text;

import static com.sun.javafx.scene.text.TextLayout.FLAGS_ANALYSIS_VALID;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_BIDI;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_COMPLEX;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_EMBEDDED;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_TABS;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_HAS_CJK;
import static com.sun.javafx.scene.text.TextLayout.FLAGS_RTL_BASE;

import java.text.Bidi;

import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.scene.text.TextSpan;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class GlyphLayout {

    public static final int CANONICAL_SUBSTITUTION = 1 << 30;

    /**
     * A flag bit indicating text direction as determined by Bidi analysis.
     */
    public static final int LAYOUT_LEFT_TO_RIGHT = 1 << 0;
    public static final int LAYOUT_RIGHT_TO_LEFT = 1 << 1;

    /**
     * A flag bit indicating that text in the char array
     * before the indicated start should not be examined.
     */
    public static final int LAYOUT_NO_START_CONTEXT = 1 << 2;

    /**
     * A flag bit indicating that text in the char array
     * after the indicated limit should not be examined.
     */
    public static final int LAYOUT_NO_LIMIT_CONTEXT = 1 << 3;

    public static final int HINTING = 1 << 4;

    /**
     * Android versions that still run a dalvik based on JDK 6 (API level 18 and
     * before) don't have the method Character.isIdeographic.
     * On devices with a JVM that does not have Character.isIdeographic, there will
     * be non-optimal line breaking for CJKV.
     * The reflection-based approach should be removed in a later version,
     * when the Android base version moves to API level 19.
     */
    private static Method isIdeographicMethod = null;
    static {
        try {
            isIdeographicMethod = Character.class.getMethod("isIdeographic", int.class);
        } catch (NoSuchMethodException | SecurityException e) {
            isIdeographicMethod = null;
        }
    }

    protected TextRun addTextRun(PrismTextLayout layout, char[] chars,
                                 int start, int length,
                                 PGFont font, TextSpan span, byte level) {
        /* subclass can overwrite this method in order to handle complex text */
        TextRun run = new TextRun(start, length, level, true, 0, span, 0, false);
        layout.addTextRun(run);
        return run;
    }

    private TextRun addTextRun(PrismTextLayout layout, char[] chars,
                               int start, int length, PGFont font,
                               TextSpan span, byte level, boolean complex) {

        /* The complex flag indicates complex script, and in general all
         * bidi scripts are consider complex. That said, using directional
         * control (RLO) is possible to force RTL direction on non-complex
         * scripts. Thus, odd level must be threat as complex.
         */
        if (complex || (level & 1) != 0) {
            return addTextRun(layout, chars, start, length, font, span, level);
        }
        TextRun run = new TextRun(start, length, level, false, 0, span, 0, false);
        layout.addTextRun(run);
        return run;
    }

    public int breakRuns(PrismTextLayout layout, char[] chars, int flags) {
        int length = chars.length;
        boolean complex = false;
        boolean feature = false;
        int scriptRun = ScriptMapper.COMMON;
        int script = ScriptMapper.COMMON;

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
                flags |= FLAGS_HAS_BIDI | FLAGS_HAS_COMPLEX;
            }
        }

        int start = 0;
        int i = 0;
        while (i < length) {
            char ch = chars[i];
            int codePoint = ch;
            boolean delimiter = ch == '\t' || ch == '\n' || ch == '\r';

            /* special handling for delimiters */
            if (delimiter) {
                if (i != start) {
                    run = addTextRun(layout, chars, start, i - start,
                                     font, span, bidiLevel, complex);
                    if (complex) {
                        flags |= FLAGS_HAS_COMPLEX;
                        complex = false;
                    }
                    start = i;
                }
            }
            boolean spanChanged = i >= spanEnd && i < length;
            boolean levelChanged = i >= bidiEnd && i < length;
            boolean scriptChanged = false;
            if (!delimiter) {
                boolean oldComplex = complex;
                if (checkComplex) {
                    if (Character.isHighSurrogate(ch)) {
                        /* Only merge surrogate when the pair is in the same span. */
                        if (i + 1 < spanEnd && Character.isLowSurrogate(chars[i + 1])) {
                            codePoint = Character.toCodePoint(ch, chars[++i]);
                        }
                    }

                    if (isIdeographic(codePoint)) {
                        flags |= FLAGS_HAS_CJK;
                    }

                    /* Check for script changes */
                    script = ScriptMapper.getScript(codePoint);
                    if (scriptRun > ScriptMapper.INHERITED  &&
                        script > ScriptMapper.INHERITED &&
                        script != scriptRun) {
                        scriptChanged = true;
                    }
                    if (!complex) {
                        complex = feature || ScriptMapper.isComplexCharCode(codePoint);
                    }
                }

                if (spanChanged || levelChanged || scriptChanged) {
                    if (start != i) {
                        /* Create text run */
                        run = addTextRun(layout, chars, start, i - start,
                                         font, span, bidiLevel, oldComplex);
                       if (complex) {
                           flags |= FLAGS_HAS_COMPLEX;
                           complex = false;
                       }
                       start = i;
                    }
                }
                i++;
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
//                bidiLevel = (byte)bidi.getRunLevel(bidiIndex);
                bidiLevel = (byte)bidi.getLevelAt(bidi.getRunStart(bidiIndex));
                bidiEnd = bidi.getRunLimit(bidiIndex);
                if ((bidiLevel & 1) != 0) {
                    flags |= FLAGS_HAS_BIDI | FLAGS_HAS_COMPLEX;
                }
            }
            if (scriptChanged) {
                scriptRun = script;
            }
            if (delimiter) {
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
            }
        }

        /* Create final text run */
        if (start < length) {
            addTextRun(layout, chars, start, length - start,
                       font, span, bidiLevel, complex);
            if (complex) {
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

    public abstract void layout(TextRun run, PGFont font,
                                FontStrike strike, char[] text);

    protected int getInitialSlot(FontResource fr) {
        /* For some reason, DirectWrite and CoreText do not work with the JRE
         * fonts (Lucida Sans). For example, with Arabic text the glyphs
         * do not have ligatures (as if all glyphs were generated using just
         * the CMAP table). Possible reasons for this failure is the
         * presence of a system version of Lucida Sans, which does not include
         * Arabic, and that causes some internal cache to fail (since both fonts
         * would have the same postscript name); or possibly the JRE fonts
         * have some internal settings that causes DirectWrite and
         * CoreText to fail. Pango and ICU do not present the same problem.
         * The fix is to use a different font.
         * This fix relies that a CompositeFontResource has at least one
         * fallback, and that is not a JRE font, and this method is used
         * only to process complex text.
         */
        if (PrismFontFactory.isJreFont(fr)) {
            if (PrismFontFactory.debugFonts) {
                System.err.println("Avoiding JRE Font: " + fr.getFullName());
            }
            return 1;
        }
        return 0;
    }

    /* This scheme creates a singleton GlyphLayout which is checked out
     * for use. Callers who find its checked out create one that after use
     * is discarded. This means that in a MT-rendering environment,
     * there's no need to synchronise except for that one instance.
     * Fewer threads will then need to synchronise, perhaps helping
     * throughput on a MP system. If for some reason the reusable
     * GlyphLayout is checked out for a long time (or never returned?) then
     * we would end up always creating new ones. That situation should not
     * occur and if if did, it would just lead to some extra garbage being
     * created.
     */
    private static GlyphLayout reusableGL = newInstance();
    private static boolean inUse;

    private static GlyphLayout newInstance() {
        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        return factory.createGlyphLayout();
    }

    public static GlyphLayout getInstance() {
        /* The following heuristic is that if the reusable instance is
         * in use, it probably still will be in a micro-second, so avoid
         * synchronising on the class and just allocate a new instance.
         * The cost is one extra boolean test for the normal case, and some
         * small number of cases where we allocate an extra object when
         * in fact the reusable one would be freed very soon.
         */
        if (inUse) {
            return newInstance();
        } else {
            synchronized(GlyphLayout.class) {
                if (inUse) {
                    return newInstance();
                } else {
                    inUse = true;
                    return reusableGL;
                }
            }
        }
    }

    public void dispose() {
        if (this == reusableGL) {
            inUse = false;
        }
    }

    private static boolean isIdeographic(int codePoint) {
        if (isIdeographicMethod != null) {
            try {
                return (boolean) isIdeographicMethod.invoke(null, codePoint);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                return false;
            }
        }
        return false;
    }

}
