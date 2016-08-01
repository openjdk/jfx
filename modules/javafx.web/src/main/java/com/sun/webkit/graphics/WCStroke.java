/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.graphics;

import java.util.Arrays;

public abstract class WCStroke<P, S> {

    /* The StrokeStyle should be compliant with
     * WebCore/platform/graphics/GraphicsContext.h
     */
    public static final int NO_STROKE = 0;
    public static final int SOLID_STROKE = 1;
    public static final int DOTTED_STROKE = 2;
    public static final int DASHED_STROKE = 3;

    /* The LineCap should be compliant with
     * WebCore/platform/graphics/GraphicsTypes.h
     */
    public static final int BUTT_CAP = 0;
    public static final int ROUND_CAP = 1;
    public static final int SQUARE_CAP = 2;

    /* The LineJoin should be compliant with
     * WebCore/platform/graphics/GraphicsTypes.h
     */
    public static final int MITER_JOIN = 0;
    public static final int ROUND_JOIN = 1;
    public static final int BEVEL_JOIN = 2;

    private int style = SOLID_STROKE;
    private int lineCap = BUTT_CAP;
    private int lineJoin = MITER_JOIN;
    private float miterLimit = 10.0f;
    private float thickness = 1.0f;
    private float offset;
    private float[] sizes;
    private P paint;

    protected abstract void invalidate();

    public abstract S getPlatformStroke();

    public void copyFrom(WCStroke<P, S> stroke) {
        this.style = stroke.style;
        this.lineCap = stroke.lineCap;
        this.lineJoin = stroke.lineJoin;
        this.miterLimit = stroke.miterLimit;
        this.thickness = stroke.thickness;
        this.offset = stroke.offset;
        this.sizes = stroke.sizes;
        this.paint = stroke.paint;
    }

    public void setStyle(int style) {
        if (style != SOLID_STROKE && style != DOTTED_STROKE && style != DASHED_STROKE) {
            style = NO_STROKE;
        }
        if (this.style != style) {
            this.style = style;
            invalidate();
        }
    }

    public void setLineCap(int lineCap) {
        if (lineCap != ROUND_CAP && lineCap != SQUARE_CAP) {
            lineCap = BUTT_CAP;
        }
        if (this.lineCap != lineCap) {
            this.lineCap = lineCap;
            invalidate();
        }
    }

    public void setLineJoin(int lineJoin) {
        if (lineJoin != ROUND_JOIN && lineJoin != BEVEL_JOIN) {
            lineJoin = MITER_JOIN;
        }
        if (this.lineJoin != lineJoin) {
            this.lineJoin = lineJoin;
            invalidate();
        }
    }

    public void setMiterLimit(float miterLimit) {
        if (miterLimit < 1.0f) {
            miterLimit = 1.0f;
        }
        if (this.miterLimit != miterLimit) {
            this.miterLimit = miterLimit;
            invalidate();
        }
    }

    public void setThickness(float thickness) {
        if (thickness < 0.0f) {
            thickness = 1.0f;
        }
        if (this.thickness != thickness) {
            this.thickness = thickness;
            invalidate();
        }
    }

    public void setDashOffset(float offset) {
        if (this.offset != offset) {
            this.offset = offset;
            invalidate();
        }
    }

    public void setDashSizes(float... sizes) {
        if ((sizes == null) || (sizes.length == 0)) {
            if (this.sizes != null) {
                this.sizes = null;
                invalidate();
            }
        }
        else if (!Arrays.equals(this.sizes, sizes)) {
            this.sizes = sizes.clone();
            invalidate();
        }
    }

    public void setPaint(P paint) {
        this.paint = paint;
    }

    public int getStyle() {
        return this.style;
    }

    public int getLineCap() {
        return this.lineCap;
    }

    public int getLineJoin() {
        return this.lineJoin;
    }

    public float getMiterLimit() {
        return this.miterLimit;
    }

    public float getThickness() {
        return this.thickness;
    }

    public float getDashOffset() {
        return this.offset;
    }

    public float[] getDashSizes() {
        return (this.sizes != null)
                ? this.sizes.clone()
                : null;
    }

    public P getPaint() {
        return this.paint;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("[style=").append(this.style);
        sb.append(", lineCap=").append(this.lineCap);
        sb.append(", lineJoin=").append(this.lineJoin);
        sb.append(", miterLimit=").append(this.miterLimit);
        sb.append(", thickness=").append(this.thickness);
        sb.append(", offset=").append(this.offset);
        sb.append(", sizes=").append(Arrays.toString(this.sizes));
        sb.append(", paint=").append(this.paint);
        return sb.append("]").toString();
    }
}
