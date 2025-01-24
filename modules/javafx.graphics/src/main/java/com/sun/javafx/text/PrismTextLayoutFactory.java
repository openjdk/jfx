/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.text;

import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLayoutFactory;

public class PrismTextLayoutFactory implements TextLayoutFactory {
    private static final PrismTextLayoutFactory factory = new PrismTextLayoutFactory();
    /* Same strategy as GlyphLayout */
    private static final TextLayout reusableTL = factory.createLayout();
    private static boolean inUse;

    private PrismTextLayoutFactory() {
    }

    @Override
    public TextLayout createLayout() {
        return new PrismTextLayout(PrismFontFactory.cacheLayoutSize) {
            @Override
            protected GlyphLayout glyphLayout() {
                return GlyphLayoutManager.getInstance();
            }
        };
    }

    @Override
    public TextLayout getLayout() {
        if (inUse) {
            return createLayout();
        } else {
            synchronized(PrismTextLayoutFactory.class) {
                if (inUse) {
                    return createLayout();
                } else {
                    inUse = true;
                    reusableTL.setAlignment(0);
                    reusableTL.setWrapWidth(0);
                    reusableTL.setDirection(0);
                    reusableTL.setContent(null);
                    return reusableTL;
                }
            }
        }
    }

    @Override
    public void disposeLayout(TextLayout layout) {
        if (layout == reusableTL) {
            inUse = false;
        }
    }

    public static PrismTextLayoutFactory getFactory() {
        return factory;
    }
}
