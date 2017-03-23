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

import com.sun.javafx.scene.layout.region.BorderImageSlices;
import javafx.beans.NamedArg;
import javafx.geometry.Insets;
import javafx.scene.image.Image;

/**
 * Defines properties describing how to render an image as the border of
 * some Region. A BorderImage must have an Image specified (it cannot be
 * null). The {@code repeatX} and {@code repeatY} properties define how the
 * image is to be repeated in each direction. The {@code slices} property
 * defines how to slice up the image such that it can be stretched across
 * the Region, while the {@code widths} defines the area on the Region to
 * fill with the border image. Finally, the {@code outsets} define the distance
 * outward from the edge of the border over which the border extends. The
 * outsets of the BorderImage contribute to the outsets of the Border, which
 * in turn contribute to the bounds of the Region.
 * <p>
 * Because the BorderImage is immutable, it can safely be used in any
 * cache, and can safely be reused among multiple Regions.
 * <p>
 * When applied to a Region with a defined shape, a BorderImage is ignored.
 * @since JavaFX 8.0
 */
public class BorderImage {
    /**
     * The image to be used. This will never be null. If this
     * image fails to load, then the entire BorderImage will
     * be skipped at rendering time and will not contribute to
     * any bounds or other computations.
     * @return the image to be used
     */
    public final Image getImage() { return image; }
    final Image image;

    /**
     * Indicates in what manner (if at all) the border image
     * is to be repeated along the x-axis of the region. If not specified,
     * the default value is STRETCH.
     * @return the BorderRepeat that indicates if the border image
     * is to be repeated along the x-axis of the region
     */
    public final BorderRepeat getRepeatX() { return repeatX; }
    final BorderRepeat repeatX;

    /**
     * Indicates in what manner (if at all) the border image
     * is to be repeated along the y-axis of the region. If not specified,
     * the default value is STRETCH.
     * @return the BorderRepeat that indicates if the border image
     * is to be repeated along the y-axis of the region
     */
    public final BorderRepeat getRepeatY() { return repeatY; }
    final BorderRepeat repeatY;

    /**
     * The widths of the border on each side. These can be defined
     * as either to be absolute widths or percentages of the size of
     * the Region, {@link BorderWidths} for more details. If null,
     * this will default to being 1 pixel wide.
     * @return the BorderWidths of the border on each side
     */
    public final BorderWidths getWidths() { return widths; }
    final BorderWidths widths;

    /**
     * Defines the slices of the image. JavaFX uses a 4-slice scheme where
     * the slices each divide up an image into 9 patches. The top-left patch
     * defines the top-left corner of the border. The top patch defines the top
     * border and the image making up this patch is stretched horizontally
     * (or whatever is defined for repeatX) to fill all the required space. The
     * top-right patch goes in the top-right corner, and the right patch is
     * stretched vertically (or whatever is defined for repeatY) to fill all the
     * required space. And so on. The center patch is stretched (or whatever is
     * defined for repeatX, repeatY) in each dimension. By default the center is
     * omitted (ie: not drawn), although a BorderImageSlices value of {@code true}
     * for the {@code filled} property will cause the center to be drawn. A
     * default value for this property will result in BorderImageSlices.DEFAULT, which
     * is a border-image-slice of 100%
     * @return the BorderWidths that defines the slices of the image
     * @see <a href="http://www.w3.org/TR/css3-background/#the-border-image-slice">border-image-slice</a>
     */
    public final BorderWidths getSlices() { return slices; }
    final BorderWidths slices;

    /**
     * Specifies whether or not the center patch (as defined by the left, right, top, and bottom slices)
     * should be drawn.
     * @return true if the center patch should be drawn
     */
    public final boolean isFilled() { return filled; }
    final boolean filled;

    /**
     * The insets of the BorderImage define where the border should be positioned
     * relative to the edge of the Region. This value will never be null.
     * @return the insets of the BorderImage
     */
    public final Insets getInsets() { return insets; }
    final Insets insets;

    // These two are used by Border to compute the insets and outsets of the border
    final Insets innerEdge;
    final Insets outerEdge;

    /**
     * A cached hash code for faster secondary usage. It is expected
     * that BorderImage will be pulled from a cache in many cases.
     */
    private final int hash;

    /**
     * Creates a new BorderImage. The image must be specified or a NullPointerException will
     * be thrown.
     *
     * @param image    The image to use. This must not be null.
     * @param widths    The widths of the border in each dimension. A null value results in Insets.EMPTY.
     * @param insets    The insets at which to place the border relative to the region.
     *                  A null value results in Insets.EMPTY.
     * @param slices    The slices for the image. If null, defaults to BorderImageSlices.DEFAULT
     * @param filled    A flag indicating whether the center patch should be drawn
     * @param repeatX    The repeat value for the border image in the x direction. If null, defaults to STRETCH.
     * @param repeatY    The repeat value for the border image in the y direction. If null, defaults to the same
     *                   value as repeatX.
     */
    public BorderImage(
            @NamedArg("image") Image image, @NamedArg("widths") BorderWidths widths, @NamedArg("insets") Insets insets, @NamedArg("slices") BorderWidths slices, @NamedArg("filled") boolean filled,
            @NamedArg("repeatX") BorderRepeat repeatX, @NamedArg("repeatY") BorderRepeat repeatY) {
        if (image == null) throw new NullPointerException("Image cannot be null");
        this.image = image;
        this.widths = widths == null ? BorderWidths.DEFAULT : widths;
        this.insets = insets == null ? Insets.EMPTY : insets;
        this.slices = slices == null ? BorderImageSlices.DEFAULT.widths : slices;
        this.filled = filled;
        this.repeatX = repeatX == null ? BorderRepeat.STRETCH : repeatX;
        this.repeatY = repeatY == null ? this.repeatX : repeatY;

        // Compute the inner & outer edge. The outer edge is insets.top,
        // while the inner edge is insets.top + widths.top
        outerEdge = new Insets(
                Math.max(0, -this.insets.getTop()),
                Math.max(0, -this.insets.getRight()),
                Math.max(0, -this.insets.getBottom()),
                Math.max(0, -this.insets.getLeft()));
        innerEdge = new Insets(
                this.insets.getTop() + this.widths.getTop(),
                this.insets.getRight() + this.widths.getRight(),
                this.insets.getBottom() + this.widths.getBottom(),
                this.insets.getLeft() + this.widths.getLeft());

        // Pre-compute the hash code. NOTE: all variables are prefixed with "this" so that we
        // do not accidentally compute the hash based on the constructor arguments rather than
        // based on the fields themselves!
        int result = this.image.hashCode();
        result = 31 * result + this.widths.hashCode();
        result = 31 * result + this.slices.hashCode();
        result = 31 * result + this.repeatX.hashCode();
        result = 31 * result + this.repeatY.hashCode();
        result = 31 * result + (this.filled ? 1 : 0);
        hash = result;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BorderImage that = (BorderImage) o;
        if (this.hash != that.hash) return false;
        if (filled != that.filled) return false;
        if (!image.equals(that.image)) return false;
        if (repeatX != that.repeatX) return false;
        if (repeatY != that.repeatY) return false;
        if (!slices.equals(that.slices)) return false;
        if (!widths.equals(that.widths)) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int hashCode() {
        return hash;
    }
}
