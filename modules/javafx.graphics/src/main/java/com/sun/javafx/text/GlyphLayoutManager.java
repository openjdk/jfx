/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

/* This class creates a singleton GlyphLayout which is checked out
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
public class GlyphLayoutManager {
    private static GlyphLayout reusableGL = newInstance();
    private static volatile boolean inUse;

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

    public static void dispose(GlyphLayout la) {
        if (la == reusableGL) {
            inUse = false;
        }
    }
}
