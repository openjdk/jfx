/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.openpisces;

import com.sun.javafx.geom.PathConsumer2D;
import com.sun.javafx.geom.transform.BaseTransform;

public abstract class TransformingPathConsumer2D implements PathConsumer2D {
    protected PathConsumer2D out;

    public TransformingPathConsumer2D(PathConsumer2D out) {
        this.out = out;
    }

    public void setConsumer(PathConsumer2D out) {
        this.out = out;
    }
/*
    public static PathConsumer2D
        transformConsumer(PathConsumer2D out,
                          AffineTransform at)
    {
        if (at == null) {
            return out;
        }
        float Mxx = (float) at.getScaleX();
        float Mxy = (float) at.getShearX();
        float Mxt = (float) at.getTranslateX();
        float Myx = (float) at.getShearY();
        float Myy = (float) at.getScaleY();
        float Myt = (float) at.getTranslateY();
        if (Mxy == 0f && Myx == 0f) {
            if (Mxx == 1f && Myy == 1f) {
                if (Mxt == 0f && Myt == 0f) {
                    return out;
                } else {
                    return new TranslateFilter(out, Mxt, Myt);
                }
            } else {
                if (Mxt == 0f && Myt == 0f) {
                    return new DeltaScaleFilter(out, Mxx, Myy);
                } else {
                    return new ScaleTranslateFilter(out, Mxx, Myy, Mxt, Myt);
                }
            }
        } else if (Mxt == 0f && Myt == 0f) {
            return new DeltaTransformFilter(out, Mxx, Mxy, Myx, Myy);
        } else {
            return new TransformFilter(out, Mxx, Mxy, Mxt, Myx, Myy, Myt);
        }
    }

    public static PathConsumer2D
        deltaTransformConsumer(PathConsumer2D out,
                               AffineTransform at)
    {
        if (at == null) {
            return out;
        }
        float Mxx = (float) at.getScaleX();
        float Mxy = (float) at.getShearX();
        float Myx = (float) at.getShearY();
        float Myy = (float) at.getScaleY();
        if (Mxy == 0f && Myx == 0f) {
            if (Mxx == 1f && Myy == 1f) {
                return out;
            } else {
                return new DeltaScaleFilter(out, Mxx, Myy);
            }
        } else {
            return new DeltaTransformFilter(out, Mxx, Mxy, Myx, Myy);
        }
    }

    public static PathConsumer2D
        inverseDeltaTransformConsumer(PathConsumer2D out,
                                      AffineTransform at)
    {
        if (at == null) {
            return out;
        }
        float Mxx = (float) at.getScaleX();
        float Mxy = (float) at.getShearX();
        float Myx = (float) at.getShearY();
        float Myy = (float) at.getScaleY();
        if (Mxy == 0f && Myx == 0f) {
            if (Mxx == 1f && Myy == 1f) {
                return out;
            } else {
                return new DeltaScaleFilter(out, 1.0f/Mxx, 1.0f/Myy);
            }
        } else {
            float det = Mxx * Myy - Mxy * Myx;
            return new DeltaTransformFilter(out,
                                            Myy / det,
                                            -Mxy / det,
                                            -Myx / det,
                                            Mxx / det);
        }
    }
*/

    public static final class FilterSet {
        private TranslateFilter translater;
        private DeltaScaleFilter deltascaler;
        private ScaleTranslateFilter scaletranslater;
        private DeltaTransformFilter deltatransformer;
        private TransformFilter transformer;

        public FilterSet() {
        }

        public PathConsumer2D getConsumer(PathConsumer2D out,
                                          BaseTransform tx)
        {
            if (tx == null) {
                return out;
            }
            float Mxx = (float) tx.getMxx();
            float Mxy = (float) tx.getMxy();
            float Mxt = (float) tx.getMxt();
            float Myx = (float) tx.getMyx();
            float Myy = (float) tx.getMyy();
            float Myt = (float) tx.getMyt();
            if (Mxy == 0f && Myx == 0f) {
                if (Mxx == 1f && Myy == 1f) {
                    if (Mxt == 0f && Myt == 0f) {
                        return out;
                    } else {
                        if (translater == null) {
                            translater = new TranslateFilter(out, Mxt, Myt);
                        } else {
                            translater.set(Mxt, Myt);
                        }
                        return translater;
                    }
                } else {
                    if (Mxt == 0f && Myt == 0f) {
                        if (deltascaler == null) {
                            deltascaler = new DeltaScaleFilter(out, Mxx, Myy);
                        } else {
                            deltascaler.set(Mxx, Myy);
                        }
                        return deltascaler;
                    } else {
                        if (scaletranslater == null) {
                            scaletranslater =
                                new ScaleTranslateFilter(out, Mxx, Myy, Mxt, Myt);
                        } else {
                            scaletranslater.set(Mxx, Myy, Mxt, Myt);
                        }
                        return scaletranslater;
                    }
                }
            } else if (Mxt == 0f && Myt == 0f) {
                if (deltatransformer == null) {
                    deltatransformer =
                        new DeltaTransformFilter(out, Mxx, Mxy, Myx, Myy);
                } else {
                    deltatransformer.set(Mxx, Mxy, Myx, Myy);
                }
                return deltatransformer;
            } else {
                if (transformer == null) {
                    transformer =
                        new TransformFilter(out, Mxx, Mxy, Mxt, Myx, Myy, Myt);
                } else {
                    transformer.set(Mxx, Mxy, Mxt, Myx, Myy, Myt);
                }
                return transformer;
            }
        }
    }

    static final class TranslateFilter extends TransformingPathConsumer2D {
        private float tx;
        private float ty;

        TranslateFilter(PathConsumer2D out,
                        float tx, float ty)
        {
            super(out);
            set(tx, ty);
        }

        public void set(float tx, float ty) {
            this.tx = tx;
            this.ty = ty;
        }

        public void moveTo(float x0, float y0) {
            out.moveTo(x0 + tx, y0 + ty);
        }

        public void lineTo(float x1, float y1) {
            out.lineTo(x1 + tx, y1 + ty);
        }

        public void quadTo(float x1, float y1,
                           float x2, float y2)
        {
            out.quadTo(x1 + tx, y1 + ty,
                       x2 + tx, y2 + ty);
        }

        public void curveTo(float x1, float y1,
                            float x2, float y2,
                            float x3, float y3)
        {
            out.curveTo(x1 + tx, y1 + ty,
                        x2 + tx, y2 + ty,
                        x3 + tx, y3 + ty);
        }

        public void closePath() {
            out.closePath();
        }

        public void pathDone() {
            out.pathDone();
        }

        public long getNativeConsumer() {
            return 0;
        }
    }

    static final class ScaleTranslateFilter extends TransformingPathConsumer2D {
        private float sx;
        private float sy;
        private float tx;
        private float ty;

        ScaleTranslateFilter(PathConsumer2D out,
                    float sx, float sy, float tx, float ty)
        {
            super(out);
            set(sx, sy, tx, ty);
        }

        public void set(float sx, float sy, float tx, float ty) {
            this.sx = sx;
            this.sy = sy;
            this.tx = tx;
            this.ty = ty;
        }

        public void moveTo(float x0, float y0) {
            out.moveTo(x0 * sx + tx, y0 * sy + ty);
        }

        public void lineTo(float x1, float y1) {
            out.lineTo(x1 * sx + tx, y1 * sy + ty);
        }

        public void quadTo(float x1, float y1,
                           float x2, float y2)
        {
            out.quadTo(x1 * sx + tx, y1 * sy + ty,
                       x2 * sx + tx, y2 * sy + ty);
        }

        public void curveTo(float x1, float y1,
                            float x2, float y2,
                            float x3, float y3)
        {
            out.curveTo(x1 * sx + tx, y1 * sy + ty,
                        x2 * sx + tx, y2 * sy + ty,
                        x3 * sx + tx, y3 * sy + ty);
        }

        public void closePath() {
            out.closePath();
        }

        public void pathDone() {
            out.pathDone();
        }

        public long getNativeConsumer() {
            return 0;
        }
    }

    static final class TransformFilter extends TransformingPathConsumer2D {
        private float Mxx;
        private float Mxy;
        private float Mxt;
        private float Myx;
        private float Myy;
        private float Myt;

        TransformFilter(PathConsumer2D out,
                        float Mxx, float Mxy, float Mxt,
                        float Myx, float Myy, float Myt)
        {
            super(out);
            set(Mxx, Mxy, Mxt, Myx, Myy, Myt);
        }

        public void set(float Mxx, float Mxy, float Mxt,
                        float Myx, float Myy, float Myt)
        {
            this.Mxx = Mxx;
            this.Mxy = Mxy;
            this.Mxt = Mxt;
            this.Myx = Myx;
            this.Myy = Myy;
            this.Myt = Myt;
        }

        public void moveTo(float x0, float y0) {
            out.moveTo(x0 * Mxx + y0 * Mxy + Mxt,
                       x0 * Myx + y0 * Myy + Myt);
        }

        public void lineTo(float x1, float y1) {
            out.lineTo(x1 * Mxx + y1 * Mxy + Mxt,
                       x1 * Myx + y1 * Myy + Myt);
        }

        public void quadTo(float x1, float y1,
                           float x2, float y2)
        {
            out.quadTo(x1 * Mxx + y1 * Mxy + Mxt,
                       x1 * Myx + y1 * Myy + Myt,
                       x2 * Mxx + y2 * Mxy + Mxt,
                       x2 * Myx + y2 * Myy + Myt);
        }

        public void curveTo(float x1, float y1,
                            float x2, float y2,
                            float x3, float y3)
        {
            out.curveTo(x1 * Mxx + y1 * Mxy + Mxt,
                        x1 * Myx + y1 * Myy + Myt,
                        x2 * Mxx + y2 * Mxy + Mxt,
                        x2 * Myx + y2 * Myy + Myt,
                        x3 * Mxx + y3 * Mxy + Mxt,
                        x3 * Myx + y3 * Myy + Myt);
        }

        public void closePath() {
            out.closePath();
        }

        public void pathDone() {
            out.pathDone();
        }

        public long getNativeConsumer() {
            return 0;
        }
    }

    static final class DeltaScaleFilter extends TransformingPathConsumer2D {
        private float sx;
        private float sy;

        public DeltaScaleFilter(PathConsumer2D out, float Mxx, float Myy) {
            super(out);
            set(Mxx, Myy);
        }

        public void set(float Mxx, float Myy) {
            sx = Mxx;
            sy = Myy;
        }

        public void moveTo(float x0, float y0) {
            out.moveTo(x0 * sx, y0 * sy);
        }

        public void lineTo(float x1, float y1) {
            out.lineTo(x1 * sx, y1 * sy);
        }

        public void quadTo(float x1, float y1,
                           float x2, float y2)
        {
            out.quadTo(x1 * sx, y1 * sy,
                       x2 * sx, y2 * sy);
        }

        public void curveTo(float x1, float y1,
                            float x2, float y2,
                            float x3, float y3)
        {
            out.curveTo(x1 * sx, y1 * sy,
                        x2 * sx, y2 * sy,
                        x3 * sx, y3 * sy);
        }

        public void closePath() {
            out.closePath();
        }

        public void pathDone() {
            out.pathDone();
        }

        public long getNativeConsumer() {
            return 0;
        }
    }

    static final class DeltaTransformFilter extends TransformingPathConsumer2D {
        private float Mxx;
        private float Mxy;
        private float Myx;
        private float Myy;

        DeltaTransformFilter(PathConsumer2D out,
                             float Mxx, float Mxy,
                             float Myx, float Myy)
        {
            super(out);
            set(Mxx, Mxy, Myx, Myy);
        }

        public void set(float Mxx, float Mxy,
                        float Myx, float Myy)
        {
            this.Mxx = Mxx;
            this.Mxy = Mxy;
            this.Myx = Myx;
            this.Myy = Myy;
        }

        public void moveTo(float x0, float y0) {
            out.moveTo(x0 * Mxx + y0 * Mxy,
                       x0 * Myx + y0 * Myy);
        }

        public void lineTo(float x1, float y1) {
            out.lineTo(x1 * Mxx + y1 * Mxy,
                       x1 * Myx + y1 * Myy);
        }

        public void quadTo(float x1, float y1,
                           float x2, float y2)
        {
            out.quadTo(x1 * Mxx + y1 * Mxy,
                       x1 * Myx + y1 * Myy,
                       x2 * Mxx + y2 * Mxy,
                       x2 * Myx + y2 * Myy);
        }

        public void curveTo(float x1, float y1,
                            float x2, float y2,
                            float x3, float y3)
        {
            out.curveTo(x1 * Mxx + y1 * Mxy,
                        x1 * Myx + y1 * Myy,
                        x2 * Mxx + y2 * Mxy,
                        x2 * Myx + y2 * Myy,
                        x3 * Mxx + y3 * Mxy,
                        x3 * Myx + y3 * Myy);
        }

        public void closePath() {
            out.closePath();
        }

        public void pathDone() {
            out.pathDone();
        }

        public long getNativeConsumer() {
            return 0;
        }
    }
}
