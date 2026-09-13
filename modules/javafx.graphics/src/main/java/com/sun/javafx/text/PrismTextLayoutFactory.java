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

import java.util.concurrent.atomic.AtomicBoolean;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLayoutFactory;

public class PrismTextLayoutFactory implements TextLayoutFactory {
    private static final PrismTextLayoutFactory FACTORY = new PrismTextLayoutFactory();
    /* Same strategy as GlyphLayout */
    private static final TextLayout REUSABLE_INSTANCE = FACTORY.createLayout();
    private static final AtomicBoolean IN_USE = new AtomicBoolean(false);

    private PrismTextLayoutFactory() {
    }

    @Override
    public TextLayout createLayout() {
        return new PrismTextLayout(PrismFontFactory.cacheLayoutSize);
    }

    @Override
    public TextLayout getLayout() {
        if (IN_USE.compareAndSet(false, true)) {
            REUSABLE_INSTANCE.setAlignment(0);
            REUSABLE_INSTANCE.setWrapWidth(0);
            REUSABLE_INSTANCE.setDirection(0);
            REUSABLE_INSTANCE.setContent(null);
            return REUSABLE_INSTANCE;
        } else {
            return createLayout();
        }
    }

    @Override
    public void disposeLayout(TextLayout layout) {
        if (layout == REUSABLE_INSTANCE) {
            IN_USE.set(false);
        }
    }

    public static PrismTextLayoutFactory getFactory() {
        return FACTORY;
    }
}
