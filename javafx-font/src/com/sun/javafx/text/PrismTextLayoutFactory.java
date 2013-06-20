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

package com.sun.javafx.text;

import com.sun.javafx.scene.text.TextLayoutFactory;

public class PrismTextLayoutFactory implements TextLayoutFactory {

    /* Same strategy as GlyphLayout */
    private static final PrismTextLayout reusableTL = new PrismTextLayout();
    private static boolean inUse;

    private PrismTextLayoutFactory() {
    }

    public com.sun.javafx.scene.text.TextLayout createLayout() {
        return new PrismTextLayout();
    }

    public com.sun.javafx.scene.text.TextLayout getLayout() {
        if (inUse) {
            return new PrismTextLayout();
        } else {
            synchronized(PrismTextLayoutFactory.class) {
                if (inUse) {
                    return new PrismTextLayout();
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

    public void disposeLayout(com.sun.javafx.scene.text.TextLayout layout) {
        if (layout == reusableTL) {
            inUse = false;
        }
    }

    private static final PrismTextLayoutFactory factory = new PrismTextLayoutFactory();
    public static PrismTextLayoutFactory getFactory() {
        return factory;
    }
}
