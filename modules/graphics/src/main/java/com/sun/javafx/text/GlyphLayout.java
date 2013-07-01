/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;

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

    public abstract int breakRuns(PrismTextLayout layout, char[] text, int flags);

    public abstract void layout(TextRun run, PGFont font,
                                FontStrike strike, char[] text);

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
}
