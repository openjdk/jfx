/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.prism;

import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.text.TextRun;
import com.sun.webkit.graphics.WCTextRun;

public final class WCTextRunImpl implements WCTextRun {
    private final TextRun run;

    public WCTextRunImpl(GlyphList run) {
        this.run = (TextRun) run;
    }

    @Override
    public int getGlyphCount() {
        return run.getGlyphCount();
    }

    @Override
    public boolean isLeftToRight() {
        return run.isLeftToRight();
    }

    @Override
    public int getGlyph(int index) {
        return index < run.getGlyphCount() ? run.getGlyphCode(index) : 0;
    }

    // Avoid repeated allocation
    private static float POS_AND_ADVANCE[] = new float[4];

    @Override
    public float[] getGlyphPosAndAdvance(int glyphIndex) {
        POS_AND_ADVANCE[0] = run.getPosX(glyphIndex);
        POS_AND_ADVANCE[1] = run.getPosY(glyphIndex);
        POS_AND_ADVANCE[2] = run.getAdvance(glyphIndex);
        // FIXME: We don't yet support Y advance from prism.
        POS_AND_ADVANCE[3] = 0;
        return POS_AND_ADVANCE;
    }

    @Override
    public int getStart() {
        return run.getStart();
    }

    @Override
    public int getEnd() {
        return run.getEnd();
    }

    @Override
    public int getCharOffset(int index) {
        return run.getCharOffset(index);
    }
}
