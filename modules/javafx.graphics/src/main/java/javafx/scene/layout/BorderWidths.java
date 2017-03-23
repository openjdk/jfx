/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import javafx.beans.NamedArg;


/**
 * Defines widths for four components (top, right, bottom, and left).
 * Each width is defined as a non-negative
 * value. This value might be interpreted either as an literal value, or as a
 * percentage of the width or height of the Region, depending on the values
 * for {@code topAsPercentage}, {@code rightAsPercentage}, {@code bottomAsPercentage},
 * {@code leftAsPercentage}. The only allowable negative value for top, right,
 * bottom, and left is {@code AUTO}.
 * <p>
 * Because the BorderWidths is immutable, it can safely be used in any
 * cache, and can safely be reused among multiple Regions.
 * @since JavaFX 8.0
 */
public final class BorderWidths {
    /**
     * When used by a BorderStroke, the value of AUTO is interpreted as the
     * value of {@link BorderStroke#MEDIUM} for the corresponding side. When
     * used with a BorderImage, the value of AUTO means to read the corresponding
     * value from the BorderStroke(s), and not to specify it manually.
     */
    public static final double AUTO = -1;

    /**
     * The default BorderWidths that is used by a BorderImage when null is specified. This
     * width is a single 1 pixel top, right, bottom, and left, all interpreted as literal values.
     */
    public static final BorderWidths DEFAULT = new BorderWidths(1, 1, 1, 1, false, false, false, false);

    /**
     * An empty set of widths, such that all values are 0 and are literal values.
     */
    public static final BorderWidths EMPTY = new BorderWidths(0, 0, 0, 0, false, false, false, false);

    /**
     * A set of widths representing 100% on each side.
     */
    public static final BorderWidths FULL = new BorderWidths(1d, 1d, 1d, 1d, true, true, true, true);


    /**
     * A non-negative value (with the exception of {@link #AUTO}) indicating the border
     * thickness on the top of the border. This value can be a literal value, or can be
     * treated as a percentage, based on the value of the
     * {@link #isTopAsPercentage() topAsPercentage} property.
     * @return the border thickness on the top of the border
     */
    public final double getTop() { return top; }
    final double top;

    /**
     * The non-negative value (with the exception of {@link #AUTO}) indicating the border
     * thickness on the right of the border. This value can be a literal value, or can be
     * treated as a percentage, based on the value of the
     * {@link #isRightAsPercentage() rightAsPercentage} property.
     * @return the border thickness on the right of the border
     */
    public final double getRight() { return right; }
    final double right;

    /**
     * The non-negative value (with the exception of {@link #AUTO}) indicating the border
     * thickness on the bottom of the border. This value can be a literal value, or can be
     * treated as a percentage, based on the value of the
     * {@link #isBottomAsPercentage() bottomAsPercentage} property.
     * @return the border thickness on the bottom of the border
     */
    public final double getBottom() { return bottom; }
    final double bottom;

    /**
     * The non-negative value (with the exception of {@link #AUTO}) indicating the border
     * thickness on the left of the border. This value can be an literal value, or can be
     * treated as a percentage, based on the value of the
     * {@link #isLeftAsPercentage() leftAsPercentage} property.
     * @return the border thickness on the left of the border
     */
    public final double getLeft() { return left; }
    final double left;

    /**
     * Specifies whether the {@link #getTop() top} property should be interpreted as a percentage ({@code true})
     * of the region height or not ({@code false}).
     * @return true if top should be interpreted as a percentage of the region height, otherwise false
     */
    public final boolean isTopAsPercentage() { return topAsPercentage; }
    final boolean topAsPercentage;

    /**
     * Specifies whether the {@link #getRight() right} property should be interpreted as a percentage ({@code true})
     * of the region width or not ({@code false}).
     * @return true if right should be interpreted as a percentage of the region width, otherwise false
     */
    public final boolean isRightAsPercentage() { return rightAsPercentage; }
    final boolean rightAsPercentage;

    /**
     * Specifies whether the {@link #getBottom() bottom} property should be interpreted as a percentage ({@code true})
     * of the region height or not ({@code false}).
     * @return true if bottom should be interpreted as a percentage of the region height, otherwise false
     */
    public final boolean isBottomAsPercentage() { return bottomAsPercentage; }
    final boolean bottomAsPercentage;

    /**
     * Specifies whether the {@link #getLeft() left} property should be interpreted as a percentage ({@code true})
     * of the region width or not ({@code false}).
     * @return true if left should be interpreted as a percentage of the region width, otherwise false
     */
    public final boolean isLeftAsPercentage() { return leftAsPercentage; }
    final boolean leftAsPercentage;

    /**
     * A cached hash code for faster secondary usage. It is expected
     * that BorderWidths will be pulled from a cache in many cases.
     */
    private final int hash;

    /**
     * Creates a new BorderWidths using the given width for all four borders,
     * and treating this width as a literal value, and not a percentage.
     *
     * @param width The border width. This cannot be negative.
     */
    public BorderWidths(@NamedArg("width") double width) {
        this(width, width, width, width, false, false, false, false);
    }

    /**
     * Creates a new BorderWidths with the specified widths for top, right,
     * bottom, and left. None of these values may be negative. Each of these
     * values is interpreted as a literal value, not as a percentage.
     *
     * @param top    The thickness of the border on the top. Must be non-negative.
     * @param right    The thickness of the border on the right. Must be non-negative.
     * @param bottom    The thickness of the border on the bottom. Must be non-negative.
     * @param left    The thickness of the border on the left. Must be non-negative.
     */
    public BorderWidths(@NamedArg("top") double top, @NamedArg("right") double right, @NamedArg("bottom") double bottom, @NamedArg("left") double left) {
        this(top, right, bottom, left, false, false, false, false);
    }

    /**
     * Creates a new BorderWidths. None of the values for {@code top}, {@code right}, {@code bottom},
     * or {@code left} can be non-negative.
     *
     * @param top    The thickness of the border on the top. Must be non-negative.
     * @param right    The thickness of the border on the right. Must be non-negative.
     * @param bottom    The thickness of the border on the bottom. Must be non-negative.
     * @param left    The thickness of the border on the left. Must be non-negative.
     * @param topAsPercentage    Whether the top should be treated as a percentage.
     * @param rightAsPercentage    Whether the right should be treated as a percentage.
     * @param bottomAsPercentage    Whether the bottom should be treated as a percentage.
     * @param leftAsPercentage        Whether the left should be treated as a percentage.
     */
    public BorderWidths(
            @NamedArg("top") double top, @NamedArg("right") double right, @NamedArg("bottom") double bottom, @NamedArg("left") double left, @NamedArg("topAsPercentage") boolean topAsPercentage,
            @NamedArg("rightAsPercentage") boolean rightAsPercentage, @NamedArg("bottomAsPercentage") boolean bottomAsPercentage, @NamedArg("leftAsPercentage") boolean leftAsPercentage) {

        // As per CSS 3 Spec (4.3), cannot be negative
        if ((top != AUTO && top < 0) ||
                (right != AUTO && right < 0) ||
                (bottom != AUTO && bottom < 0) ||
                (left != AUTO && left < 0)) {
            throw new IllegalArgumentException("None of the widths can be < 0");
        }

        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
        this.topAsPercentage = topAsPercentage;
        this.rightAsPercentage = rightAsPercentage;
        this.bottomAsPercentage = bottomAsPercentage;
        this.leftAsPercentage = leftAsPercentage;

        // Pre-compute the hash code. NOTE: all variables are prefixed with "this" so that we
        // do not accidentally compute the hash based on the constructor arguments rather than
        // based on the fields themselves!
        int result;
        long temp;
        temp = this.top != +0.0d ? Double.doubleToLongBits(this.top) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = this.right != +0.0d ? Double.doubleToLongBits(this.right) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = this.bottom != +0.0d ? Double.doubleToLongBits(this.bottom) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = this.left != +0.0d ? Double.doubleToLongBits(this.left) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (this.topAsPercentage ? 1 : 0);
        result = 31 * result + (this.rightAsPercentage ? 1 : 0);
        result = 31 * result + (this.bottomAsPercentage ? 1 : 0);
        result = 31 * result + (this.leftAsPercentage ? 1 : 0);
        hash = result;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BorderWidths that = (BorderWidths) o;

        if (this.hash != that.hash) return false;
        if (Double.compare(that.bottom, bottom) != 0) return false;
        if (bottomAsPercentage != that.bottomAsPercentage) return false;
        if (Double.compare(that.left, left) != 0) return false;
        if (leftAsPercentage != that.leftAsPercentage) return false;
        if (Double.compare(that.right, right) != 0) return false;
        if (rightAsPercentage != that.rightAsPercentage) return false;
        if (Double.compare(that.top, top) != 0) return false;
        if (topAsPercentage != that.topAsPercentage) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int hashCode() {
        return hash;
    }
}
