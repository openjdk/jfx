/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;


public class BorderStyle {


    /** placeholder for border-style 'none' */
    static final public BorderStyle NONE = new BorderStyle();

    /** placeholder for border-style 'solid' */
    static final public BorderStyle SOLID = new BorderStyle(
                StrokeType.CENTERED,
                StrokeLineJoin.MITER,
                StrokeLineCap.BUTT,
                10.0,
                0.0,
                new double[] {}
        );

    /**
     * Defines the direction (inside, outside, or both) that the strokeWidth
     * is applied to the boundary of the shape.
     *
     * @profile desktop
     * @defaultValue CENTERED
     */
    public StrokeType getStrokeType() { return strokeType; }
    final private StrokeType strokeType;

    /**
     * Defines the decoration applied where path segments meet.
     * The value must have one of the following values:
     * {@code StrokeLineJoin.BEVEL}, {@code StrokeLineJoin.MITER},
     * and {@code StrokeLineJoin.ROUND}.
     *
     * @defaultValue MITER
     */
    public StrokeLineJoin getStrokeLineJoin() { return strokeLineJoin; }
    final private StrokeLineJoin strokeLineJoin;

    /**
     * The end cap style of this {@code Shape} as one of the following
     * values that define possible end cap styles:
     * {@code StrokeLineCap.BUTT}, {@code StrokeLineCap.ROUND},
     * and  {@code StrokeLineCap.SQUARE}.
     *
     * @defaultValue SQUARE
     */
    public StrokeLineCap getStrokeLineCap() { return strokeLineCap; }
    final private StrokeLineCap strokeLineCap;

    /**
     * Defines the limit for the {@code StrokeLineJoin.MITER} line join style.
     *
     * @defaultValue 10
     */
    public double getStrokeMiterLimit() { return strokeMiterLimit; }
    final private double strokeMiterLimit;

    /**
     * Defines a distance specified in user coordinates that represents
     * an offset into the dashing pattern. In other words, the dash phase
     * defines the point in the dashing pattern that will correspond
     * to the beginning of the stroke.
     *
     * @defaultValue 0
     */
    public double getStrokeDashOffset() { return strokeDashOffset; }
    final private double strokeDashOffset;

    /**
     * Defines the array representing the lengths of the dash segments.
     * Alternate entries in the array represent the user space lengths
     * of the opaque and transparent segments of the dashes.
     * As the pen moves along the outline of the {@code Shape} to be stroked,
     * the user space distance that the pen travels is accumulated.
     * The distance value is used to index into the dash array.
     * The pen is opaque when its current cumulative distance maps
     * to an even element of the dash array and transparent otherwise.
     * An empty strokeDashArray indicates a solid line with no spaces.
     * @defaultValue empty
     */
    public double[] getStrokeDashArray() { return strokeDashArray; }
    final private double[] strokeDashArray;

    public BorderStyle(StrokeType strokeType, StrokeLineJoin strokeLineJoin,
            StrokeLineCap strokeLineCap, Double strokeMiterLimit,
            Double strokeDashOffset, double[] strokeDashArray) {
        this.strokeType = (strokeType != null) ?
                strokeType : StrokeType.CENTERED;
        this.strokeLineJoin = (strokeLineJoin != null) ?
                strokeLineJoin : StrokeLineJoin.MITER;
        this.strokeLineCap = (strokeLineCap != null) ?
                strokeLineCap : StrokeLineCap.BUTT;
        this.strokeMiterLimit = (strokeMiterLimit != null) ?
                strokeMiterLimit : 10.0f;
        this.strokeDashOffset = (strokeDashOffset != null) ?
                strokeDashOffset : 0.0f;
        this.strokeDashArray = (strokeDashArray != null) ?
                strokeDashArray : null;
    }

    private BorderStyle() {
        strokeType = StrokeType.CENTERED;
        strokeLineJoin = StrokeLineJoin.MITER;
        strokeLineCap = StrokeLineCap.BUTT;
        strokeMiterLimit = 10.0f;
        strokeDashOffset = 0.0f;
        strokeDashArray = null;
    }

    boolean adjusted = false;
    void adjustForStrokeWidth(double width) {
        if (width > 1 && !adjusted) {
            if (strokeDashArray != null && strokeDashArray.length > 0) {
                double factor = width - 1;
                for (int n=0; n<strokeDashArray.length; n++)
                    strokeDashArray[n] *= factor;
            }
            adjusted = true;
        }
    }

    @Override
    public String toString() {
        String s = "BorderStyle.NONE";
        if (this == BorderStyle.SOLID) {
            s = "BorderStyle.SOLID";
        } else if(this != BorderStyle.NONE) {
            StringBuilder sbuf = new StringBuilder();
            sbuf.append("BorderStyle: ");
            sbuf.append(strokeType);
            sbuf.append(", ");
            sbuf.append(strokeLineJoin);
            sbuf.append(", ");
            sbuf.append(strokeLineCap);
            sbuf.append(", ");
            sbuf.append(strokeMiterLimit);
            sbuf.append(", ");
            sbuf.append(strokeDashOffset);
            sbuf.append(", [");
            if (strokeDashArray != null) {
            for(int n=0; n<strokeDashArray.length-1; n++) {
                sbuf.append(strokeDashArray[n]);
                sbuf.append(", ");
            }
            if (strokeDashArray.length > 0) {
                sbuf.append(strokeDashArray[strokeDashArray.length-1]);
            }
            }
            sbuf.append("]");
            s = sbuf.toString();
        }
        return s;
    }

    /** Primarily for unit testing */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof BorderStyle) {
            BorderStyle other = (BorderStyle)obj;
            if ((this.strokeType == other.strokeType) &&
                 (this.strokeLineJoin == other.strokeLineJoin) &&
                 (this.strokeLineCap == other.strokeLineCap) &&
                 (this.strokeMiterLimit == other.strokeMiterLimit) &&
                 (this.strokeDashOffset == other.strokeDashOffset)) {

                if ((this.strokeDashArray != null && other.strokeDashArray != null) &&
                    (this.strokeDashArray.length == other.strokeDashArray.length)) {

                    return java.util.Arrays.equals(strokeDashArray, other.strokeDashArray);
                } else {
                    return (this.strokeDashArray == null && other.strokeDashArray == null);
                }
            }
        }
        return false;
    }

}

