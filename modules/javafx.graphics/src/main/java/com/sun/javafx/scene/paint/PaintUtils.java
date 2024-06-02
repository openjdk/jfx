/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.paint;

import javafx.animation.Interpolatable;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import java.util.List;

public final class PaintUtils {

    private PaintUtils() {}

    /**
     * Interpolates between potentially different types of paint.
     * <p>
     * In addition to homogeneous interpolations between paints of the same type, the following
     * heterogeneous interpolations are supported:
     * <ul>
     *     <li>Color ↔ LinearGradient
     *     <li>Color ↔ RadialGradient
     * </ul>
     * If a paint is not interpolatable, {@code startValue} is returned for {@code t == 0},
     * and {@code endValue} is returned otherwise.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Paint interpolate(Paint startValue, Paint endValue, double t) {
        if (startValue instanceof Color start) {
            if (endValue instanceof Color end) {
                return start.interpolate(end, t);
            }

            if (endValue instanceof LinearGradient end) {
                return newSolidGradient(end, start).interpolate(end, t);
            }

            if (endValue instanceof RadialGradient end) {
                return newSolidGradient(end, start).interpolate(end, t);
            }
        }

        if (startValue instanceof LinearGradient start) {
            if (endValue instanceof LinearGradient end) {
                return start.interpolate(end, t);
            }

            if (endValue instanceof Color end) {
                return start.interpolate(newSolidGradient(start, end), t);
            }
        }

        if (startValue instanceof RadialGradient start) {
            if (endValue instanceof RadialGradient end) {
                return start.interpolate(end, t);
            }

            if (endValue instanceof Color end) {
                return start.interpolate(newSolidGradient(start, end), t);
            }
        }

        if (startValue instanceof Interpolatable start
                && endValue instanceof Interpolatable end
                && startValue.getClass().isInstance(endValue)) {
            return (Paint)start.interpolate(end, t);
        }

        return t > 0 ? endValue : startValue;
    }

    /**
     * Creates a new linear gradient that consists of two stops with the same color.
     */
    public static LinearGradient newSolidGradient(LinearGradient source, Color color) {
        return new LinearGradient(
                source.getStartX(), source.getStartY(),
                source.getEndX(), source.getEndY(),
                source.isProportional(),
                source.getCycleMethod(),
                List.of(new Stop(0, color), new Stop(1, color)));
    }

    /**
     * Creates a new radial gradient that consists of two stops with the same color.
     */
    public static RadialGradient newSolidGradient(RadialGradient source, Color color) {
        return new RadialGradient(
                source.getFocusAngle(), source.getFocusDistance(),
                source.getCenterX(), source.getCenterY(),
                source.getRadius(),
                source.isProportional(),
                source.getCycleMethod(),
                List.of(new Stop(0, color), new Stop(1, color)));
    }
}
