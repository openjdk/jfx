/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.marlin;


import com.sun.javafx.geom.transform.BaseTransform;

public final class DTransformingPathConsumer2D {

    DTransformingPathConsumer2D() {
        // used by DRendererContext
    }

    // recycled DPathConsumer2D instances from deltaTransformConsumer()
    private final DeltaScaleFilter     dt_DeltaScaleFilter     = new DeltaScaleFilter();
    private final DeltaTransformFilter dt_DeltaTransformFilter = new DeltaTransformFilter();

    public DPathConsumer2D deltaTransformConsumer(DPathConsumer2D out,
                                                 BaseTransform at)
    {
        if (at == null) {
            return out;
        }
        double mxx = at.getMxx();
        double mxy = at.getMxy();
        double myx = at.getMyx();
        double myy = at.getMyy();

        if (mxy == 0.0d && myx == 0.0d) {
            if (mxx == 1.0d && myy == 1.0d) {
                return out;
            } else {
                return dt_DeltaScaleFilter.init(out, mxx, myy);
            }
        } else {
            return dt_DeltaTransformFilter.init(out, mxx, mxy, myx, myy);
        }
    }

    // recycled DPathConsumer2D instances from inverseDeltaTransformConsumer()
    private final DeltaScaleFilter     iv_DeltaScaleFilter     = new DeltaScaleFilter();
    private final DeltaTransformFilter iv_DeltaTransformFilter = new DeltaTransformFilter();

    public DPathConsumer2D inverseDeltaTransformConsumer(DPathConsumer2D out,
                                                        BaseTransform at)
    {
        if (at == null) {
            return out;
        }
        double mxx = at.getMxx();
        double mxy = at.getMxy();
        double myx = at.getMyx();
        double myy = at.getMyy();

        if (mxy == 0.0d && myx == 0.0d) {
            if (mxx == 1.0d && myy == 1.0d) {
                return out;
            } else {
                return iv_DeltaScaleFilter.init(out, 1.0d/mxx, 1.0d/myy);
            }
        } else {
            double det = mxx * myy - mxy * myx;
            return iv_DeltaTransformFilter.init(out,
                                                myy / det,
                                               -mxy / det,
                                               -myx / det,
                                                mxx / det);
        }
    }


    static final class DeltaScaleFilter implements DPathConsumer2D {
        private DPathConsumer2D out;
        private double sx, sy;

        DeltaScaleFilter() {}

        DeltaScaleFilter init(DPathConsumer2D out,
                              double mxx, double myy)
        {
            this.out = out;
            sx = mxx;
            sy = myy;
            return this; // fluent API
        }

        @Override
        public void moveTo(double x0, double y0) {
            out.moveTo(x0 * sx, y0 * sy);
        }

        @Override
        public void lineTo(double x1, double y1) {
            out.lineTo(x1 * sx, y1 * sy);
        }

        @Override
        public void quadTo(double x1, double y1,
                           double x2, double y2)
        {
            out.quadTo(x1 * sx, y1 * sy,
                       x2 * sx, y2 * sy);
        }

        @Override
        public void curveTo(double x1, double y1,
                            double x2, double y2,
                            double x3, double y3)
        {
            out.curveTo(x1 * sx, y1 * sy,
                        x2 * sx, y2 * sy,
                        x3 * sx, y3 * sy);
        }

        @Override
        public void closePath() {
            out.closePath();
        }

        @Override
        public void pathDone() {
            out.pathDone();
        }
    }

    static final class DeltaTransformFilter implements DPathConsumer2D {
        private DPathConsumer2D out;
        private double mxx, mxy, myx, myy;

        DeltaTransformFilter() {}

        DeltaTransformFilter init(DPathConsumer2D out,
                                  double mxx, double mxy,
                                  double myx, double myy)
        {
            this.out = out;
            this.mxx = mxx;
            this.mxy = mxy;
            this.myx = myx;
            this.myy = myy;
            return this; // fluent API
        }

        @Override
        public void moveTo(double x0, double y0) {
            out.moveTo(x0 * mxx + y0 * mxy,
                       x0 * myx + y0 * myy);
        }

        @Override
        public void lineTo(double x1, double y1) {
            out.lineTo(x1 * mxx + y1 * mxy,
                       x1 * myx + y1 * myy);
        }

        @Override
        public void quadTo(double x1, double y1,
                           double x2, double y2)
        {
            out.quadTo(x1 * mxx + y1 * mxy,
                       x1 * myx + y1 * myy,
                       x2 * mxx + y2 * mxy,
                       x2 * myx + y2 * myy);
        }

        @Override
        public void curveTo(double x1, double y1,
                            double x2, double y2,
                            double x3, double y3)
        {
            out.curveTo(x1 * mxx + y1 * mxy,
                        x1 * myx + y1 * myy,
                        x2 * mxx + y2 * mxy,
                        x2 * myx + y2 * myy,
                        x3 * mxx + y3 * mxy,
                        x3 * myx + y3 * myy);
        }

        @Override
        public void closePath() {
            out.closePath();
        }

        @Override
        public void pathDone() {
            out.pathDone();
        }
    }
}
