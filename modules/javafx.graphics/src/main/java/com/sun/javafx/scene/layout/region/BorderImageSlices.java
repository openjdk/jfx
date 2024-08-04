/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.layout.region;

import com.sun.javafx.util.InterpolationUtils;
import javafx.animation.Interpolatable;
import javafx.scene.layout.BorderWidths;
import java.util.Objects;

/**
 * A helper class during the conversion process.
 */
public final class BorderImageSlices implements Interpolatable<BorderImageSlices> {

    /**
     * Using EMPTY results in no border-image being drawn since the slices are zero. You probably
     * want to use {@link BorderImageSlices#DEFAULT}
     */
    public static final BorderImageSlices EMPTY = new BorderImageSlices(BorderWidths.EMPTY, false);

    /**
     * Default border-image-slice is 100%
     * @see <a href="http://www.w3.org/TR/css3-background/#the-border-image-slice">border-image-slice</a>
     */
    public static final BorderImageSlices DEFAULT = new BorderImageSlices(BorderWidths.FULL, false);

    public BorderWidths widths;
    public boolean filled;

    public BorderImageSlices(BorderWidths widths, boolean filled) {
        this.widths = widths;
        this.filled = filled;
    }

    @Override
    public BorderImageSlices interpolate(BorderImageSlices endValue, double t) {
        Objects.requireNonNull(endValue, "endValue cannot be null");

        if (t <= 0 || equals(endValue)) {
            return this;
        }

        if (t >= 1) {
            return endValue;
        }

        return new BorderImageSlices(
            widths.interpolate(endValue.widths, t),
            InterpolationUtils.interpolateDiscrete(filled, endValue.filled, t));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BorderImageSlices that)) return false;
        return filled == that.filled && Objects.equals(widths, that.widths);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(widths);
        result = 31 * result + Boolean.hashCode(filled);
        return result;
    }
}
