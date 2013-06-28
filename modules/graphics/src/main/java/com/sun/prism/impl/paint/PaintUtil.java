/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.paint;

import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Gradient;
import com.sun.prism.paint.LinearGradient;
import com.sun.prism.paint.Paint;
import com.sun.prism.paint.RadialGradient;
import com.sun.prism.paint.Stop;

public class PaintUtil {

    private static final Affine2D gradXform = new Affine2D();

    public static void fillImageWithGradient(int[] pixels,
                                             Gradient grad, BaseTransform xform,
                                             int px, int py,
                                             int pw, int ph,
                                             float bx, float by,
                                             float bw, float bh)
    {
        Gradient mgrad = (Gradient)grad;
        int numStops = mgrad.getNumStops();
        float[] fractions = new float[numStops];
        Color[] colors = new Color[numStops];
        for (int i = 0; i < numStops; i++) {
            Stop stop = mgrad.getStops().get(i);
            fractions[i] = stop.getOffset();
            colors[i] = stop.getColor();
        }

        MultipleGradientContext context;
        if (grad.getType() == Paint.Type.LINEAR_GRADIENT) {
            LinearGradient lgrad = (LinearGradient)grad;
            float x1, y1, x2, y2;
            if (lgrad.isProportional()) {
                x1 = (lgrad.getX1() * bw) + bx;
                y1 = (lgrad.getY1() * bh) + by;
                x2 = (lgrad.getX2() * bw) + bx;
                y2 = (lgrad.getY2() * bh) + by;
            } else {
                x1 = lgrad.getX1();
                y1 = lgrad.getY1();
                x2 = lgrad.getX2();
                y2 = lgrad.getY2();
            }
            if (x1 == x2 && y1 == y2) {
                // prevent identical start and end points
                x1 -= 0.000001f;
                x2 += 0.000001f;
            }
            context = new LinearGradientContext(lgrad, xform,
                                                x1, y1, x2, y2,
                                                fractions, colors,
                                                lgrad.getSpreadMethod());
        } else {
            RadialGradient rgrad = (RadialGradient)grad;
            gradXform.setTransform(xform);
            float radius = rgrad.getRadius();
            float cx = rgrad.getCenterX();
            float cy = rgrad.getCenterY();
            double fa = Math.toRadians(rgrad.getFocusAngle());
            float fd = rgrad.getFocusDistance();
            if (rgrad.isProportional()) {
                float bcx = bx + (bw / 2f);
                float bcy = by + (bh / 2f);
                float scale = Math.min(bw, bh);
                cx = (cx - 0.5f) * scale + bcx;
                cy = (cy - 0.5f) * scale + bcy;
                if (bw != bh && bw != 0f && bh != 0f) {
                    gradXform.translate(bcx, bcy);
                    gradXform.scale(bw / scale, bh / scale);
                    gradXform.translate(-bcx, -bcy);
                }
                radius = radius * scale;
            }
            if (radius <= 0f) {
                radius = 0.001f;
            }
            fd *= radius;
            float fx = (float) (cx + fd * Math.cos(fa));
            float fy = (float) (cy + fd * Math.sin(fa));
            context = new RadialGradientContext(rgrad, gradXform,
                                                cx, cy, radius, fx, fy,
                                                fractions, colors,
                                                rgrad.getSpreadMethod());
        }

        context.fillRaster(pixels, 0, 0, px, py, pw, ph);
    }
}
