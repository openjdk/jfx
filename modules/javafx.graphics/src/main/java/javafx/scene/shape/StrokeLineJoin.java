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
 * Defines the line join style of a {@code Shape}.
 * @since JavaFX 2.0
 */
public enum StrokeLineJoin {

    /**
     * Joins path segments by extending their outside edges until they meet.
     *
     * <p> <img src="doc-files/strokelinejoin-miter.png" alt="A visual rendering
     * of StrokeLineJoin.MITER"> </p>
     */
    MITER,//(BasicStroke.JOIN_MITER),

    /**
     * Joins path segments by connecting the outer corners
     * of their wide outlines with a straight segment.
     *
     * <p> <img src="doc-files/strokelinejoin-bevel.png" alt="A visual rendering
     * of StrokeLineJoin.BEVEL"> </p>
     */
    BEVEL,//(BasicStroke.JOIN_BEVEL),

    /**
     * Joins path segments by rounding off the corner
     * at a radius of half the line width.
     *
     * <p> <img src="doc-files/strokelinejoin-round.png" alt="A visual rendering
     * of StrokeLineJoin.ROUND"> </p>
     */
    ROUND//(BasicStroke.JOIN_ROUND);
}
