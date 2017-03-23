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
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * The fill and associated properties that direct how to fill the background of a
 * {@link Region}. Because BackgroundFill is an immutable object, it can safely be
 * used in any cache, and can safely be reused among multiple Regions or multiple
 * times in the same Region.
 * <p>
 * All BackgroundFills are drawn in order.
 * <p>
 * When applied to a Region with a defined shape, the corner radii are ignored.
 * @since JavaFX 8.0
 */
public final class BackgroundFill {
    /**
     * The Paint to use for filling the background of the {@link Region}.
     * This value will never be null.
     * @return the Paint to use for filling the background of the {@link Region}
     */
    public final Paint getFill() { return fill; }
    final Paint fill;

    /**
     * The Radii to use for representing the four radii of the
     * BackgroundFill. Each corner can therefore be independently
     * specified. This will never be null. The radii values will
     * never be negative.
     * @return the Radii to use for representing the four radii of the BackgroundFill
     */
    public final CornerRadii getRadii() { return radii; }
    final CornerRadii radii;

    /**
     * The Insets to use for this fill. Each inset indicates at what
     * distance from the Region's bounds the drawing should begin.
     * The insets will never be null, but the values may be negative
     * in order to position the border beyond the natural bounds
     * (that is, (0, 0, width, height)) of the Region.
     * @return the Insets to use for this fill
     */
    public final Insets getInsets() { return insets; }
    final Insets insets;

    /**
     * A cached hash for improved performance on subsequent hash or
     * equality look ups. It is expected that BackgroundFill will in
     * many instances be loaded from a CSS cache, such that storing
     * this value is beneficial.
     */
    private final int hash;

    /**
     * Creates a new BackgroundFill with the specified fill, radii, and
     * insets. Null values are acceptable, but default values will be
     * used in place of any null value.
     *
     * @param fill   Any Paint. If null, the value Color.TRANSPARENT is used.
     * @param radii  The corner Radii. If null, the value Radii.EMPTY is used.
     * @param insets The insets. If null, the value Insets.EMPTY is used.
     */
    public BackgroundFill(@NamedArg("fill") Paint fill, @NamedArg("radii") CornerRadii radii, @NamedArg("insets") Insets insets) {
        // As per the CSS Spec (section 3.2): initial value of background-color is TRANSPARENT
        this.fill = fill == null ? Color.TRANSPARENT : fill;
        this.radii = radii == null ? CornerRadii.EMPTY : radii;
        this.insets = insets == null ? Insets.EMPTY : insets;

        // Pre-compute the hash code. NOTE: all variables are prefixed with "this" so that we
        // do not accidentally compute the hash based on the constructor arguments rather than
        // based on the fields themselves!
        int result = this.fill.hashCode();
        result = 31 * result + this.radii.hashCode();
        result = 31 * result + this.insets.hashCode();
        hash = result;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackgroundFill that = (BackgroundFill) o;
        // Because the hash is cached, this can be a very fast check
        if (hash != that.hash) return false;
        if (!fill.equals(that.fill)) return false;
        if (!insets.equals(that.insets)) return false;
        if (!radii.equals(that.radii)) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int hashCode() {
        return hash;
    }
}
