/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.shape;

/**
 * The fill rule for determining the interior of the path.
 * @since JavaFX 2.0
 */
public enum FillRule {

    /**
     * Defines an even-odd filling rule for determining the interior of a path.
     *
     * The even-odd rule specifies that a point lies inside the path if a ray
     * drawn in any direction from that point to infinity is crossed by path
     * segments an odd number of times.
     * <p> <img src="doc-files/fillrule-evenodd.png" alt="A visual illustration
     * of how EVEN_ODD works"> </p>
     */
    EVEN_ODD, //(GeneralPath.WIND_EVEN_ODD),

    /**
     * Defines a non-zero filling rule for determining the interior of a path.
     *
     * The non-zero rule specifies that a point lies inside the path if a ray
     * drawn in any direction from that point to infinity is crossed by path
     * segments a different number of times in the counter-clockwise direction
     * than the clockwise direction.
     * <p> <img src="doc-files/fillrule-nonzero.png" alt="A visual illustration
     * of how NON_ZERO works"> </p>
     */
    NON_ZERO //(GeneralPath.WIND_NON_ZERO)
}
