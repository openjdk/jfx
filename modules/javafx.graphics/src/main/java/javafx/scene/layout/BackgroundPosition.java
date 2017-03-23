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
import javafx.geometry.Side;

/**
 * Represents the position of a {@link BackgroundImage} within the
 * {@link Region}'s drawing area.
 * <p>
 * The BackgroundImage can be positioned either from the left or right side
 * along the horizontal axis, and from either the top or bottom side along
 * the vertical axis. The {@link #getHorizontalSide() horizontalSide} and
 * {@link #getVerticalSide() verticalSide} properties define to which side the
 * remaining properties pertain. The {@link #getHorizontalPosition() horizontalPosition}
 * specifies the distance of the BackgroundImage from the corresponding side of the Region,
 * and {@link #isHorizontalAsPercentage() horizontalAsPercentage} indicates whether
 * this is as a literal value or a percentage. Similar properties exist for
 * specifying the size relative to the vertical axis.
 * <p>
 * For example, suppose I had a BackgroundPosition with a {@code horizontalSide}
 * of {@code Side.RIGHT}, a {@code horizontalPosition} of .05, and a
 * {@code horizontalAsPercentage} of {@code true}. In this case, the right
 * side of the BackgroundImage will be 5% of the width of the Region from
 * the Region's right edge.
 * @since JavaFX 8.0
 */
public class BackgroundPosition {
    /**
     * The default BackgroundPosition for any BackgroundImage. The default
     * is to have no insets and to be defined as 0% and 0%. That is, the
     * position is in the top-left corner.
     */
    public static final BackgroundPosition DEFAULT = new BackgroundPosition(
            Side.LEFT, 0, true, Side.TOP, 0, true);
    // As per the CSS 3 Spec (3.6), 0% 0% is the default

    /**
     * A BackgroundPosition which will center a BackgroundImage.
     */
    public static final BackgroundPosition CENTER = new BackgroundPosition(
            Side.LEFT, .5, true, Side.TOP, .5, true);

    /**
     * The side along the horizontal axis to which the BackgroundImage is
     * anchored. This will only be LEFT or RIGHT and never null.
     * @return the Side along the horizontal axis to which the BackgroundImage is
     * anchored
     */
    public final Side getHorizontalSide() { return horizontalSide; }
    final Side horizontalSide;

    /**
     * The side along the vertical axis to which the BackgroundImage is
     * anchored. This will only be TOP or BOTTOM and never null.
     * @return the Side along the vertical axis to which the BackgroundImage is
     * anchored
     */
    public final Side getVerticalSide() { return verticalSide; }
    final Side verticalSide;

    /**
     * The value indicating the position of the BackgroundImage relative
     * to the Region along the side indicated by the
     * {@link #getHorizontalSide() horizontalSide} property. This value
     * is either a literal or a percentage, depending on the
     * {@link #isHorizontalAsPercentage() horizontalAsPercentage} property.
     * Negative values are acceptable.
     * @return the horizontal position of the BackgroundImage
     */
    public final double getHorizontalPosition() { return horizontalPosition; }
    final double horizontalPosition;

    /**
     * The value indicating the position of the BackgroundImage relative
     * to the Region along the side indicated by the {@link #getVerticalSide() verticalSide}
     * property. This value is either a literal or a percentage, depending on the
     * {@link #isVerticalAsPercentage() verticalAsPercentage} property. Negative
     * values are acceptable.
     * @return the vertical position of the BackgroundImage
     */
    public final double getVerticalPosition() { return verticalPosition; }
    final double verticalPosition;

    /**
     * Specifies whether the {@link #getHorizontalPosition() horizontalPosition} should
     * be interpreted as a literal number or as a percentage.
     * @return true if horizontalPosition should be interpreted as a percentage
     */
    public final boolean isHorizontalAsPercentage() { return horizontalAsPercentage; }
    final boolean horizontalAsPercentage;

    /**
     * Specifies whether the {@link #getVerticalPosition() verticalPosition} should
     * be interpreted as a literal number or as a percentage.
     * @return true if verticalPosition should be interpreted as a percentage
     */
    public final boolean isVerticalAsPercentage() { return verticalAsPercentage; }
    final boolean verticalAsPercentage;

    /**
     * A cached has code value.
     */
    private final int hash;

    /**
     * Creates a new BackgroundPosition.
     *
     * @param horizontalSide            The horizontal side, must be either null, LEFT, or RIGHT. If null, LEFT
     *                                  will be used. If TOP or BOTTOM is specified, an IllegalArgumentException
     *                                  is thrown.
     * @param horizontalPosition        The horizontal position value.
     * @param horizontalAsPercentage    Whether to interpret the horizontal position as a decimal or percentage
     * @param verticalSide              The vertical side, must be either null, TOP, or BOTTOM. If null, TOP
     *                                  will be used. If LEFT or RIGHT is specified, an IllegalArgumentException
     *                                  is thrown.
     * @param verticalPosition          The vertical position value.
     * @param verticalAsPercentage      Whether to interpret the vertical position as a decimal or percentage
     */
    public BackgroundPosition(@NamedArg("horizontalSide") Side horizontalSide, @NamedArg("horizontalPosition") double horizontalPosition, @NamedArg("horizontalAsPercentage") boolean horizontalAsPercentage,
                              @NamedArg("verticalSide") Side verticalSide, @NamedArg("verticalPosition") double verticalPosition, @NamedArg("verticalAsPercentage") boolean verticalAsPercentage) {

        if (horizontalSide == Side.TOP || horizontalSide == Side.BOTTOM) {
            throw new IllegalArgumentException("The horizontalSide must be LEFT or RIGHT");
        }

        if (verticalSide == Side.LEFT || verticalSide == Side.RIGHT) {
            throw new IllegalArgumentException("The verticalSide must be TOP or BOTTOM");
        }

        this.horizontalSide = horizontalSide == null ? Side.LEFT : horizontalSide;
        this.verticalSide = verticalSide == null ? Side.TOP : verticalSide;
        this.horizontalPosition = horizontalPosition;
        this.verticalPosition = verticalPosition;
        this.horizontalAsPercentage = horizontalAsPercentage;
        this.verticalAsPercentage = verticalAsPercentage;

        // Pre-compute the hash code. NOTE: all variables are prefixed with "this" so that we
        // do not accidentally compute the hash based on the constructor arguments rather than
        // based on the fields themselves!
        int result;
        long temp;
        result = this.horizontalSide.hashCode();
        result = 31 * result + this.verticalSide.hashCode();
        temp = this.horizontalPosition != +0.0d ? Double.doubleToLongBits(this.horizontalPosition) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = this.verticalPosition != +0.0d ? Double.doubleToLongBits(this.verticalPosition) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (this.horizontalAsPercentage ? 1 : 0);
        result = 31 * result + (this.verticalAsPercentage ? 1 : 0);
        hash = result;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackgroundPosition that = (BackgroundPosition) o;
        if (hash != that.hash) return false;
        if (horizontalAsPercentage != that.horizontalAsPercentage) return false;
        if (Double.compare(that.horizontalPosition, horizontalPosition) != 0) return false;
        if (verticalAsPercentage != that.verticalAsPercentage) return false;
        if (Double.compare(that.verticalPosition, verticalPosition) != 0) return false;
        if (horizontalSide != that.horizontalSide) return false;
        if (verticalSide != that.verticalSide) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int hashCode() {
        return hash;
    }
}
