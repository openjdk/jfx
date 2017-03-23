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
import javafx.scene.image.Image;

/**
 * Defines properties describing how to render an image as the background to
 * some {@link Region}. A BackgroundImage must have an Image specified (it cannot be
 * null). The {@link #getRepeatX() repeatX} and {@link #getRepeatY() repeatY}
 * properties define how the image is to be repeated in each direction. The
 * {@link #getPosition() position} property defines how to position the image on the
 * Region while the {@link #getSize() size} property defines the size of the image
 * on the Region. For example, the {@code size} might be defined with
 * {@link javafx.scene.layout.BackgroundSize#isCover() cover = true}, meaning the image
 * should be stretched to cover the entire rendering surface of the Region.
 * <p>
 * Because the BackgroundImage is immutable, it can safely be used in any
 * cache, and can safely be reused among multiple Regions.
 * @since JavaFX 8.0
 */
public final class BackgroundImage {

    // TODO we need to attach a listener to the Image such that when the image loads
    // we cause the Region to repaint (probably the Region itself needs to handle this).

    /**
     * The image to be used. This will never be null. If this
     * image fails to load, then the entire BackgroundImage will
     * be skipped at rendering time.
     * @return the image to be used
     */
    public final Image getImage() { return image; }
    final Image image;

    /**
     * Indicates in what manner (if at all) the background image
     * is to be repeated along the x-axis of the region. This
     * will never be null.
     * @return the BackgroundRepeat that indicates if the background image
     * is to be repeated along the x-axis of the region
     */
    public final BackgroundRepeat getRepeatX() { return repeatX; }
    final BackgroundRepeat repeatX;

    /**
     * Indicates in what manner (if at all) the background image
     * is to be repeated along the y-axis of the region. This will
     * never be null.
     * @return the BackgroundRepeat that indicates if the background image
     * is to be repeated along the y-axis of the region
     */
    public final BackgroundRepeat getRepeatY() { return repeatY; }
    final BackgroundRepeat repeatY;

    /**
     * The position of this BackgroundImage relative to the Region. Note that any
     * position outside the background area of the region will be clipped.
     * @return the position of this BackgroundImage relative to the Region
     */
    public final BackgroundPosition getPosition() { return position; }
    final BackgroundPosition position;

    /**
     * The size of this image relative to the Region.
     * @return the size of this image relative to the Region
     */
    public final BackgroundSize getSize() { return size; }
    final BackgroundSize size;

    /**
     * The Background will cache here whether this BackgroundImage is opaque. If this
     * value is null, then it means we don't yet know. If it is non null, then we
     * know for certain it either is, or is not, opaque. Opaque in this case means
     * that the BackgroundImage's image is opaque.
     */
    Boolean opaque = null;

    /**
     * A cached hash code for faster secondary usage. It is expected
     * that BackgroundImage will be pulled from a cache in many cases.
     */
    private final int hash;

    /**
     * Creates a new BackgroundImage. The {@code image} must be specified.
     *
     * @param image       The image to use. This cannot be null.
     * @param repeatX     The BackgroundRepeat for the x axis. If null, this value defaults to REPEAT.
     * @param repeatY     The BackgroundRepeat for the y axis. If null, this value defaults to REPEAT.
     * @param position    The BackgroundPosition to use. If null, defaults to BackgroundPosition.DEFAULT.
     * @param size        The BackgroundSize. If null, defaults to BackgroundSize.DEFAULT.
     */
    public BackgroundImage(
            @NamedArg("image") Image image, @NamedArg("repeatX") BackgroundRepeat repeatX, @NamedArg("repeatY") BackgroundRepeat repeatY,
            @NamedArg("position") BackgroundPosition position, @NamedArg("size") BackgroundSize size)
    {
        if (image == null) throw new NullPointerException("Image cannot be null");
        this.image = image;
        // As per the CSS 3 Spec (section 3.4): default to REPEAT
        this.repeatX = repeatX == null ? BackgroundRepeat.REPEAT : repeatX;
        this.repeatY = repeatY == null ? BackgroundRepeat.REPEAT : repeatY;
        this.position = position == null ? BackgroundPosition.DEFAULT : position;
        this.size = size == null ? BackgroundSize.DEFAULT : size;

        // Pre-compute the hash code. NOTE: all variables are prefixed with "this" so that we
        // do not accidentally compute the hash based on the constructor arguments rather than
        // based on the fields themselves!
        int result = this.image.hashCode();
        result = 31 * result + this.repeatX.hashCode();
        result = 31 * result + this.repeatY.hashCode();
        result = 31 * result + this.position.hashCode();
        result = 31 * result + this.size.hashCode();
        hash = result;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackgroundImage that = (BackgroundImage) o;
        // Because the hash is cached, this can be very fast
        if (hash != that.hash) return false;
        if (!image.equals(that.image)) return false;
        if (!position.equals(that.position)) return false;
        if (repeatX != that.repeatX) return false;
        if (repeatY != that.repeatY) return false;
        if (!size.equals(that.size)) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int hashCode() {
        return hash;
    }
}
