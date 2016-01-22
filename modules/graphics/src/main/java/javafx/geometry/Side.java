/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.geometry;

/**
 * Enum for which side of a rectangle something should be. This is used for chart titles, axis etc.
 * @since JavaFX 2.0
 */
public enum Side {
    /**
     * Represents top side of a rectangle.
     */
    TOP,

    /**
     * Represents bottom side of a rectangle.
     */
    BOTTOM,

    /**
     * Represents left side of a rectangle.
     */
    LEFT,

    /**
     * Represents right side of a rectangle.
     */
    RIGHT;

    /**
     * Indicates whether this is vertical side of a rectangle (returns
     * {@code true} for {@code LEFT} and {@code RIGHT}.
     * @return {@code true} if this represents a vertical side of a rectangle
     */
    public boolean isVertical() {
        return this == LEFT || this == RIGHT;
    }

    /**
     * Indicates whether this is horizontal side of a rectangle (returns
     * {@code true} for {@code TOP} and {@code BOTTOM}.
     * @return {@code true} if this represents a horizontal side of a rectangle
     */
    public boolean isHorizontal() {
        return this == TOP || this == BOTTOM;
    }
}
