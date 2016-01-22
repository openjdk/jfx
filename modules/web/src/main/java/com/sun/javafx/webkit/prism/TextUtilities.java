/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.text.PrismTextLayoutFactory;
import com.sun.javafx.text.TextRun;

final class TextUtilities {
    static TextLayout createLayout(String text, Object font) {
        TextLayout layout = PrismTextLayoutFactory.getFactory().createLayout();
        layout.setContent(text, font);
        return layout;
    }

    static BaseBounds getLayoutBounds(String str, Object font) {
        return createLayout(str, font).getBounds();
    }

    static float getLayoutWidth(String str, Object font) {
        return getLayoutBounds(str, font).getWidth();
    }

    static TextRun createGlyphList(int[] glyphs, float[] advances, float x, float y) {
        TextRun run = new TextRun(0, glyphs.length, (byte) 0, true, 0, null, 0, false) {
            @Override public RectBounds getLineBounds() {
                return new RectBounds();
            }
        };
        run.shape(glyphs.length, glyphs, advances);
        run.setLocation(x, y);
        return run;
    }
}
