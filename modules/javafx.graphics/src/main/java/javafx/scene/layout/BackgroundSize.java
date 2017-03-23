/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Defines the size of the area that a BackgroundImage should fill relative
 * to the Region it is styling. There are several properties whose values
 * take precedence over the others. In particular there are 4 key properties,
 * {@code width}, {@code height}, {@code contain}, and {@code cover}. Both width
 * and height are independent of each other, however both interact with
 * contain and cover.
 * <p>
 * From the CSS Specification, {@code cover} is defined to:
 * <blockquote>
 * Scale the image, while preserving its intrinsic aspect ratio (if any), to the smallest size such that both
 * its width and its height can completely cover the background positioning area.
 * </blockquote>
 * {@code contain} is defined to:
 * <blockquote>
 * Scale the image, while preserving its intrinsic aspect ratio (if any), to the largest size such that both its
 * width and its height can fit inside the background positioning area.
 * </blockquote>
 * And width and height both specify (in absolute values or percentages) the size to use. These
 * two properties only apply if both cover and contain are false. If both cover and contain are true,
 * then {@code cover} will be used.
 * <p>
 * The width and height may also be set to {@code AUTO}, indicating that the area should be sized
 * so as to use the image's intrinsic size, or if it cannot be determined, 100%.
 * @since JavaFX 8.0
 */
public final class BackgroundSize {
    /**
     * From the CSS Specification:
     * <blockquote>
     *   An "auto" value for one dimension is resolved by using the image's intrinsic ratio and the size of the other
     *   dimension, or failing that, using the image's intrinsic size, or failing that, treating it as 100%.
     *   <p>
     *   If both values are "auto" then the intrinsic width and/or height of the image should be used, if any,
     *   the missing dimension (if any) behaving as "auto" as described above. If the image has neither an intrinsic
     *   width nor an intrinsic height, its size is determined as for "contain".
     * </blockquote>
     *
     * When set to AUTO, the values for widthAsPercentage and heightAsPercentage are ignored.
     */
    public static final double AUTO = -1;

    /**
     * The default BackgroundSize used by BackgroundImages when an explicit size is not defined.
     * By default, the BackgroundSize is AUTO, AUTO for the width and height, and is neither
     * cover nor contain.
     */
    public static final BackgroundSize DEFAULT = new BackgroundSize(AUTO, AUTO, true, true, false, false);

    /**
     * The width of the area within the Region where the associated BackgroundImage should
     * render. If set to AUTO, then {@code widthAsPercentage} is ignored. This value has
     * no meaning if either {@code contain} or {@code cover} are specified. This value
     * cannot be negative, except when set to the value of AUTO.
     * @return the width of the area within the Region where the associated
     * BackgroundImage should render
     */
    public final double getWidth() { return width; }
    final double width;

    /**
     * The height of the area within the Region where the associated BackgroundImage should
     * render. If set to AUTO, then {@code heightAsPercentage} is ignored. This value has
     * no meaning if either {@code contain} or {@code cover} are specified. This value
     * cannot be negative, except when set to the value of AUTO.
     * @return the height of the area within the Region where the associated
     * BackgroundImage should render
     */
    public final double getHeight() { return height; }
    final double height;

    /**
     * Specifies whether the value contained in {@code width} should be interpreted
     * as a percentage or as a normal value.
     * @return true if width should be interpreted as a percentage
     */
    public final boolean isWidthAsPercentage() { return widthAsPercentage; }
    final boolean widthAsPercentage;

    /**
     * Specifies whether the value contained in {@code height} should be interpreted
     * as a percentage or as a normal value.
     * @return true if height should be interpreted as a percentage
     */
    public final boolean isHeightAsPercentage() { return heightAsPercentage; }
    final boolean heightAsPercentage;

    /**
     * If true, scale the image, while preserving its intrinsic aspect ratio (if any), to the
     * largest size such that both its width and its height can fit inside the background
     * positioning area.
     * @return true if the image can fit inside the background positioning area
     */
    public final boolean isContain() { return contain; }
    final boolean contain;

    /**
     * If true, scale the image, while preserving its intrinsic aspect ratio (if any), to the
     * smallest size such that both its width and its height can completely cover the background
     * positioning area.
     * @return true if image can completely cover the background positioning area
     */
    public final boolean isCover() { return cover; }
    final boolean cover;

    /**
     * A cached hash code value
     */
    private final int hash;

    /**
     * Create a new BackgroundSize.
     *
     * @param width                 The width. Cannot be less than 0, except for the value of AUTO.
     * @param height                The height. Cannot be less than 0, except for the value of AUTO.
     * @param widthAsPercentage     Whether the width is to be interpreted as a percentage
     * @param heightAsPercentage    Whether the height is to be interpreted as a percentage
     * @param contain               Whether the image should be sized to fit within the Region maximally
     * @param cover                 Whether the image should be sized to "cover" the Region
     */
    public BackgroundSize(@NamedArg("width") double width, @NamedArg("height") double height,
                          @NamedArg("widthAsPercentage") boolean widthAsPercentage, @NamedArg("heightAsPercentage") boolean heightAsPercentage,
                          @NamedArg("contain") boolean contain, @NamedArg("cover") boolean cover) {
        // TODO Should deal with NaN and Infinity values as well
        if (width < 0 && width != AUTO)
            throw new IllegalArgumentException("Width cannot be < 0, except when AUTO");
        if (height < 0 && height != AUTO)
            throw new IllegalArgumentException("Height cannot be < 0, except when AUTO");

        this.width = width;
        this.height = height;
        this.widthAsPercentage = widthAsPercentage;
        this.heightAsPercentage = heightAsPercentage;
        this.contain = contain;
        this.cover = cover;

        // Pre-compute the hash code. NOTE: all variables are prefixed with "this" so that we
        // do not accidentally compute the hash based on the constructor arguments rather than
        // based on the fields themselves!
        int result;
        long temp;
        result = (this.widthAsPercentage ? 1 : 0);
        result = 31 * result + (this.heightAsPercentage ? 1 : 0);
        temp = this.width != +0.0d ? Double.doubleToLongBits(this.width) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = this.height != +0.0d ? Double.doubleToLongBits(this.height) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (this.cover ? 1 : 0);
        result = 31 * result + (this.contain ? 1 : 0);
        hash = result;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackgroundSize that = (BackgroundSize) o;
        // Because the hash is cached, this can be very fast
        if (this.hash != that.hash) return false;
        if (contain != that.contain) return false;
        if (cover != that.cover) return false;
        if (Double.compare(that.height, height) != 0) return false;
        if (heightAsPercentage != that.heightAsPercentage) return false;
        if (widthAsPercentage != that.widthAsPercentage) return false;
        if (Double.compare(that.width, width) != 0) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int hashCode() {
        return hash;
    }
}
