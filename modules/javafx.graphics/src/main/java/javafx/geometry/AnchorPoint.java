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

package javafx.geometry;

/**
 * Represents a reference point within a target area, used for anchoring geometry-dependent calculations
 * such as positioning a window relative to another location.
 * <p>
 * An {@code AnchorPoint} provides a {@code (x, y)} coordinate together with a flag indicating
 * how those coordinates should be interpreted:
 * <ul>
 *   <li><b>Proportional</b>: {@code x} and {@code y} are expressed as fractions of the target width and height.
 *       In this coordinate system, {@code (0, 0)} refers to the top-left corner, and {@code (1, 1)} refers to
 *       the bottom-right corner. Values outside the {@code [0..1]} range represent points outside the bounds.
 *   <li><b>Absolute</b>: {@code x} and {@code y} are expressed as offsets in pixels from the top-left corner
 *       of the target area.
 * </ul>
 *
 * @since 26
 */
public final class AnchorPoint {

    private final double x;
    private final double y;
    private final boolean proportional;

    private AnchorPoint(double x, double y, boolean proportional) {
        this.x = x;
        this.y = y;
        this.proportional = proportional;
    }

    /**
     * Creates a proportional anchor point, expressed as fractions of the target area's width and height.
     * <p>
     * In proportional coordinates, {@code (0, 0)} refers to the top-left corner of the target area and
     * {@code (1, 1)} refers to the bottom-right corner. Values outside the {@code [0..1]} range represent
     * points outside the bounds.
     *
     * @param x the horizontal fraction of the target width
     * @param y the vertical fraction of the target height
     * @return a proportional {@code AnchorPoint}
     */
    public static AnchorPoint proportional(double x, double y) {
        return new AnchorPoint(x, y, true);
    }

    /**
     * Creates an absolute anchor point, expressed as pixel offsets from the top-left corner of the target area.
     *
     * @param x the horizontal offset in pixels from the left edge of the target area
     * @param y the vertical offset in pixels from the top edge of the target area
     * @return an absolute {@code AnchorPoint}
     */
    public static AnchorPoint absolute(double x, double y) {
        return new AnchorPoint(x, y, false);
    }

    /**
     * Returns the horizontal coordinate of this anchor point.
     *
     * @return the horizontal coordinate of this anchor point
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the vertical coordinate of this anchor point.
     *
     * @return the vertical coordinate of this anchor point
     */
    public double getY() {
        return y;
    }

    /**
     * Indicates whether the {@code x} and {@code y} coordinates are proportional to the size of the target area.
     *
     * @return {@code true} if the coordinates are proportional, {@code false} otherwise
     */
    public boolean isProportional() {
        return proportional;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AnchorPoint other
            && x == other.x
            && y == other.y
            && proportional == other.proportional;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Double.hashCode(this.x);
        hash = 37 * hash + Double.hashCode(this.y);
        hash = 37 * hash + Boolean.hashCode(this.proportional);
        return hash;
    }

    /**
     * Anchor at the top-left corner of the target area.
     * <p>
     * This constant is equivalent to {@code AnchorPoint.proportional(0, 0)}.
     */
    public static final AnchorPoint TOP_LEFT = new AnchorPoint(0, 0, true);

    /**
     * Anchor at the top-center midpoint of the target area.
     * <p>
     * This constant is equivalent to {@code AnchorPoint.proportional(0.5, 0)}.
     */
    public static final AnchorPoint TOP_CENTER = new AnchorPoint(0.5, 0, true);

    /**
     * Anchor at the top-right corner of the target area.
     * <p>
     * This constant is equivalent to {@code AnchorPoint.proportional(1, 0)}.
     */
    public static final AnchorPoint TOP_RIGHT = new AnchorPoint(1, 0, true);

    /**
     * Anchor at the center-left midpoint of the target area.
     * <p>
     * This constant is equivalent to {@code AnchorPoint.proportional(0, 0.5)}.
     */
    public static final AnchorPoint CENTER_LEFT = new AnchorPoint(0, 0.5, true);

    /**
     * Anchor at the center of the target area.
     * <p>
     * This constant is equivalent to {@code AnchorPoint.proportional(0.5, 0.5)}.
     */
    public static final AnchorPoint CENTER = new AnchorPoint(0.5, 0.5, true);

    /**
     * Anchor at the center-right midpoint of the target area.
     * <p>
     * This constant is equivalent to {@code AnchorPoint.proportional(1, 0.5)}.
     */
    public static final AnchorPoint CENTER_RIGHT = new AnchorPoint(1, 0.5, true);

    /**
     * Anchor at the bottom-left corner of the target area.
     * <p>
     * This constant is equivalent to {@code AnchorPoint.proportional(0, 1)}.
     */
    public static final AnchorPoint BOTTOM_LEFT = new AnchorPoint(0, 1, true);

    /**
     * Anchor at the bottom-center midpoint of the target area.
     * <p>
     * This constant is equivalent to {@code AnchorPoint.proportional(0.5, 1)}.
     */
    public static final AnchorPoint BOTTOM_CENTER = new AnchorPoint(0.5, 1, true);

    /**
     * Anchor at the bottom-right corner of the target area.
     * <p>
     * This constant is equivalent to {@code AnchorPoint.proportional(1, 1)}.
     */
    public static final AnchorPoint BOTTOM_RIGHT = new AnchorPoint(1, 1, true);
}
