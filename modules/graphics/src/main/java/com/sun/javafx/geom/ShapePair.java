/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.geom;

/**
 * A Shape composed of an Outer {@link Shape} combined in
 * some manner with an Inner {@link Shape}.
 * <p>
 * The resulting combined shape is either the intersection of the two
 * component shapes, or the Inner shape subtracted from the Outer shape.
 *
 */
public abstract class ShapePair extends Shape {
    public static final int INCLUDES_O_NOT_I = 1;
    public static final int INCLUDES_I_NOT_O = 2;
    public static final int INCLUDES_O_AND_I = 4;
    public static final int INCLUDES_NEITHER = 8;

    /**
     * Constant used to indicate that the Inner shape should be
     * subtracted from the Outer shape.
     */
    public static final int TYPE_SUBTRACT = (INCLUDES_O_NOT_I);

    /**
     * Constant used to indicate that the Inner shape should be
     * intersected with the Outer shape.
     */
    public static final int TYPE_INTERSECT = (INCLUDES_O_AND_I);

    // UNION = (O_NOT_I | O_AND_I | I_NOT_O);
    // XOR = (O_NOT_I | I_NOT_O);
    // INVERT_OUTER = (NEITHER | I_NOT_O);
    // etc.

    /**
     * Returns the method with which to combine the two shapes.
     * For now, only {@code TYPE_SUBTRACT} and {@code TYPE_INTERSECT}
     * are supported.
     *
     * @return the method type to use to combine the shapes.
     */
    public abstract int getCombinationType();

    /**
     * Returns the outer {@link Shape}.
     *
     * @return the outer {@link Shape}.
     */
    public abstract Shape getOuterShape();

    /**
     * Returns the inner {@link Shape}.
     *
     * @return the inner {@link Shape}.
     */
    public abstract Shape getInnerShape();
}
