/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.shape;

import com.sun.javafx.sg.prism.NGShape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

public class StubNGShape extends NGShape {

    private StrokeType pgStrokeType;
    private StrokeLineCap pgStrokeLineCap;
    private StrokeLineJoin pgStrokeLineJoin;
    private float strokeWidth;
    private float strokeMiterLimit;
    private float[] strokeDashArray;
    private float strokeDashOffset;
    private Object stroke;
    private NGShape.Mode mode;
    private boolean smooth;
    private Object fill;

    public Object getFill() {
        return fill;
    }

    @Override
    public boolean isSmooth() {
        return smooth;
    }

    @Override
    public NGShape.Mode getMode() {
        return mode;
    }

    public Object getStroke() {
        return stroke;
    }

    public float getStrokeDashOffset() {
        return strokeDashOffset;
    }

    public float getStrokeMiterLimit() {
        return strokeMiterLimit;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public StrokeType getStrokeType() {
        return pgStrokeType;
    }

    public StrokeLineCap getStrokeLineCap() {
        return pgStrokeLineCap;
    }

    public StrokeLineJoin getStrokeLineJoin() {
        return pgStrokeLineJoin;
    }

    @Override
    public void setMode(NGShape.Mode mode) {
        this.mode = mode;
    }

    @Override
    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }

    @Override
    public void setFillPaint(Object fillPaint) {
        this.fill = fillPaint;
    }

    @Override
    public void setDrawPaint(Object drawPaint) {
        this.stroke = drawPaint;
    }

    @Override
    public void setDrawStroke(float strokeWidth,
            StrokeType type,
            StrokeLineCap lineCap,
            StrokeLineJoin lineJoin,
            float strokeMiterLimit,
            float[] strokeDashArray,
            float strokeDashOffset) {
        this.pgStrokeType = type;
        this.pgStrokeLineCap = lineCap;
        this.pgStrokeLineJoin = lineJoin;
        this.strokeWidth = strokeWidth;
        this.strokeMiterLimit = strokeMiterLimit;
        this.strokeDashOffset = strokeDashOffset;
        this.strokeDashArray = new float[strokeDashArray == null ? 0 : strokeDashArray.length];
        System.arraycopy(strokeDashArray, 0, this.strokeDashArray, 0, this.strokeDashArray.length);
    }

    @Override
    public com.sun.javafx.geom.Shape getShape() {
        return null;
    }
}
