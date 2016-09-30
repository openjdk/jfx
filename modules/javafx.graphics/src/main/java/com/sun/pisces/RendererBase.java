/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.pisces;

import java.lang.annotation.Native;

public final class RendererBase {

    /**
     * @defgroup CompositingRules Compositing rules supported by PISCES
     * When drawing two objects to one pixel area, there are several possibilities
     * how composite color is made of source and destination contributions.
     * Objects can overlap pixel fully and/or partialy. One object could be above
     * the second one and they both can be partialy or fully transparent (alpha).
     * The way, we count composite color and alpha from theirs contributions is
     * called compositing rule (Porter-Duff).
     * @def COMPOSITE_CLEAR
     * @ingroup CompositingRules
     * Compositing rule COMPOSITE_CLEAR. This rule applied to destination pixel sets
     * its color to 0x00000000 - transparent black - regardless to source color.
     * @see setCompositeRule(int), setComposite(int, float)
     * @def COMPOSITE_SRC
     * @ingroup CompositingRules
     * Compositing rule COMPOSITE_SRC. This rule applied to destination pixel sets
     * its color to source color - regardless to previous color of destination
     * pixel.
     * @see setCompositeRule(int), setComposite(int, float)
     * @def COMPOSITE_SRC_OVER
     * @ingroup CompositingRules
     * Compositing rule COMPOSITE_SRC_OVER. This rule is kind of intuitive. When we
     * look through transparent green glass bottle at some object, we can see
     * mixture of glass and objects colors. Composite color is alpha-weigth average
     * of source and destination.
     * @see setCompositeRule(int), PiscesRenderer.setComposite(int, float)
     */
    @Native public static final int COMPOSITE_CLEAR    = 0;
    @Native public static final int COMPOSITE_SRC      = 1;
    @Native public static final int COMPOSITE_SRC_OVER = 2;

    /**
     * Constant indicating 8/8/8/8 ARGB alpha-premultiplied pixel data stored
     * in a <code>int</code> array.
     */
    @Native public static final int TYPE_INT_ARGB_PRE = 1;

    @Native public static final int IMAGE_MODE_NORMAL = 1;
    @Native public static final int IMAGE_MODE_MULTIPLY = 2;

    @Native public static final int IMAGE_FRAC_EDGE_KEEP = 0;
    @Native public static final int IMAGE_FRAC_EDGE_PAD  = 1;
    @Native public static final int IMAGE_FRAC_EDGE_TRIM = 2;
}
