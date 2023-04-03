/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.NamedArg;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

/**
 * Defines the style of the stroke to use on one side of a BorderStroke. There are
 * several predefined styles, although the properties of these predefined styles may
 * not match the settings used to ultimately draw them. Or you may create a new
 * BorderStrokeStyle and define each of the stroke settings manually, similar
 * to any {@link javafx.scene.shape.Shape}.
 * @since JavaFX 8.0
 */
public final class BorderStrokeStyle {
    private static final List<Double> DOTTED_LIST = Collections.unmodifiableList(asList(0, 2));
    private static final List<Double> DASHED_LIST = Collections.unmodifiableList(asList(2, 1.4));

    /**
     * Indicates that no stroke should be drawn.
     */
    public static final BorderStrokeStyle NONE = new BorderStrokeStyle(
            StrokeType.INSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT, 0, 0, null);

    /**
     * A predefined dotted pattern to be used for stroking
     */
    public static final BorderStrokeStyle DOTTED = new BorderStrokeStyle(
            StrokeType.INSIDE, StrokeLineJoin.MITER, StrokeLineCap.ROUND, 10, 0, DOTTED_LIST);

    /**
     * A predefined dashed pattern to be used for stroking
     */
    public static final BorderStrokeStyle DASHED = new BorderStrokeStyle(
            StrokeType.INSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT, 10, 0, DASHED_LIST);

    /**
     * A predefined solid line to be used for stroking
     */
    public static final BorderStrokeStyle SOLID = new BorderStrokeStyle(
            StrokeType.INSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT, 10, 0, null);

    /**
     * Defines the direction (inside, outside, or both) that the strokeWidth
     * is applied to the boundary of the shape.
     *
     * @return the the direction that the strokeWidth is applied to the boundary
     * of the shape
     * @defaultValue CENTERED
     */
    public final StrokeType getType() { return type; }
    private final StrokeType type;

    /**
     * Defines the decoration applied where path segments meet.
     * The value must have one of the following values:
     * {@code StrokeLineJoin.BEVEL}, {@code StrokeLineJoin.MITER},
     * and {@code StrokeLineJoin.ROUND}.
     *
     * @return the decoration applied where path segments meet
     * @defaultValue MITER
     */
    public final StrokeLineJoin getLineJoin() { return lineJoin; }
    private final StrokeLineJoin lineJoin;

    /**
     * The end cap style of this {@code Shape} as one of the following
     * values that define possible end cap styles:
     * {@code StrokeLineCap.BUTT}, {@code StrokeLineCap.ROUND},
     * and  {@code StrokeLineCap.SQUARE}.
     *
     * @return the end cap style
     * @defaultValue SQUARE
     */
    public final StrokeLineCap getLineCap() { return lineCap; }
    private final StrokeLineCap lineCap;

    /**
     * Defines the limit for the {@code StrokeLineJoin.MITER} line join style.
     *
     * @return the limit for the StrokeLineJoin.MITER line join style
     * @defaultValue 10
     */
    public final double getMiterLimit() { return miterLimit; }
    private final double miterLimit;

    /**
     * Defines a distance specified in user coordinates that represents
     * an offset into the dashing pattern. In other words, the dash phase
     * defines the point in the dashing pattern that will correspond
     * to the beginning of the stroke.
     *
     * @return the offset into the dashing pattern
     * @defaultValue 0
     */
    public final double getDashOffset() { return dashOffset; }
    private final double dashOffset;

    /**
     * Defines the array representing the lengths of the dash segments.
     * Alternate entries in the array represent the user space lengths
     * of the opaque and transparent segments of the dashes.
     * As the pen moves along the outline of the {@code Shape} to be stroked,
     * the user space distance that the pen travels is accumulated.
     * The distance value is used to index into the dash array.
     * The pen is opaque when its current cumulative distance maps
     * to an even element of the dash array and transparent otherwise.
     * An empty dashArray indicates a solid line with no spaces.
     * @return the array representing the lengths of the dash segments
     * @defaultValue empty
     */
    public final List<Double> getDashArray() { return dashArray; }
    private final List<Double> dashArray;

    /**
     * A cached hash code
     */
    private final int hash;

    /**
     * Creates a new BorderStrokeStyle.
     *
     * @param type    The type of stroke, whether rendered OUTSIDE, INSIDE, or CENTERED on the
     *                border line. If null, defaults to CENTERED.
     * @param lineJoin  The line join. If null, defaults to MITER
     * @param lineCap   The line cap. If null, defaults to BUTT.
     * @param miterLimit    The miter limit. 10 is a good default value.
     * @param dashOffset    The dashOffset. 0 is a good default value.
     * @param dashArray    The dash array. If null, defaults to an empty list.
     */
    public BorderStrokeStyle(@NamedArg("type") StrokeType type, @NamedArg("lineJoin") StrokeLineJoin lineJoin,
                       @NamedArg("lineCap") StrokeLineCap lineCap, @NamedArg("miterLimit") double miterLimit,
                       @NamedArg("dashOffset") double dashOffset, @NamedArg("dashArray") List<Double> dashArray) {
        this.type = (type != null) ?
                type : StrokeType.CENTERED;
        this.lineJoin = (lineJoin != null) ?
                lineJoin : StrokeLineJoin.MITER;
        this.lineCap = (lineCap != null) ?
                lineCap : StrokeLineCap.BUTT;
        this.miterLimit = miterLimit;
        this.dashOffset = dashOffset;

        if (dashArray == null) {
            this.dashArray = Collections.emptyList();
        } else {
            if (dashArray == DASHED_LIST || dashArray == DOTTED_LIST) {
                // We want to use the SAME EXACT LIST in the case of DASHED_LIST or DOTTED_LIST
                // so that code in NGRegion can execute specialized code paths for such cases.
                this.dashArray = dashArray;
            } else {
                // Must not allow the passed in array to inadvertently mutate the
                // state of this BorderStrokeStyle!
                List<Double> list = new ArrayList<>(dashArray);
                this.dashArray = Collections.unmodifiableList(list);
            }
        }

        // Pre-compute the hash code. NOTE: all variables are prefixed with "this" so that we
        // do not accidentally compute the hash based on the constructor arguments rather than
        // based on the fields themselves!
        int result;
        long temp;
        result = this.type.hashCode();
//        result = 31 * result + (this == NONE ? 0 : 1); // TODO OH NO. NONE hasn't been assigned yet.
        result = 31 * result + this.lineJoin.hashCode();
        result = 31 * result + this.lineCap.hashCode();
        temp = this.miterLimit != +0.0d ? Double.doubleToLongBits(this.miterLimit) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = this.dashOffset != +0.0d ? Double.doubleToLongBits(this.dashOffset) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + this.dashArray.hashCode();
        hash = result;
    }

    /**
     * {@inheritDoc}
     */
    @Override public String toString() {
        if (this == NONE) {
            return "BorderStyle.NONE";
        } else if (this == DASHED) {
            return "BorderStyle.DASHED";
        } else if (this == DOTTED) {
            return "BorderStyle.DOTTED";
        } else if (this == SOLID) {
            return "BorderStyle.SOLID";
        } else {
            StringBuilder buffer = new StringBuilder();
            buffer.append("BorderStyle: ");
            buffer.append(type);
            buffer.append(", ");
            buffer.append(lineJoin);
            buffer.append(", ");
            buffer.append(lineCap);
            buffer.append(", ");
            buffer.append(miterLimit);
            buffer.append(", ");
            buffer.append(dashOffset);
            buffer.append(", [");
            if (dashArray != null) {
                buffer.append(dashArray);
            }
            buffer.append("]");
            return buffer.toString();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if ((this == NONE && o != NONE) || (o == NONE && this != NONE)) return false;
        if (o == null || getClass() != o.getClass()) return false;
        BorderStrokeStyle that = (BorderStrokeStyle) o;
        if (this.hash != that.hash) return false;
        if (Double.compare(that.dashOffset, dashOffset) != 0) return false;
        if (Double.compare(that.miterLimit, miterLimit) != 0) return false;
        if (!dashArray.equals(that.dashArray)) return false;
        if (lineCap != that.lineCap) return false;
        if (lineJoin != that.lineJoin) return false;
        if (type != that.type) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int hashCode() {
        return hash;
    }

    private static List<Double> asList(double... items) {
        List<Double> list = new ArrayList<>(items.length);
        for (int i=0; i<items.length; i++) {
            list.add(items[i]);
        }
        return list;
    }
}
