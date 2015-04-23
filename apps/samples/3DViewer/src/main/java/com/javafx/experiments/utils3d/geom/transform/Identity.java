/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.javafx.experiments.utils3d.geom.transform;

import com.javafx.experiments.utils3d.geom.Point2D;
import com.javafx.experiments.utils3d.geom.Vec3d;
import com.javafx.experiments.utils3d.geom.BaseBounds;
import com.javafx.experiments.utils3d.geom.Rectangle;

public final class Identity extends BaseTransform {
    @Override
    public Degree getDegree() {
        return Degree.IDENTITY;
    }

    @Override
    public int getType() {
        return TYPE_IDENTITY;
    }

    @Override
    public boolean isIdentity() {
        return true;
    }

    @Override
    public boolean isTranslateOrIdentity() {
        return true;
    }

    @Override
    public boolean is2D() {
        return true;
    }

    @Override
    public double getDeterminant() {
        return 1.0;
    }

    @Override
    public Point2D transform(Point2D src, Point2D dst) {
        if (dst == null) dst = makePoint(src, dst);
        dst.setLocation(src);
        return dst;
    }

    @Override
    public Point2D inverseTransform(Point2D src, Point2D dst) {
        if (dst == null) dst = makePoint(src, dst);
        dst.setLocation(src);
        return dst;
    }

    @Override
    public Vec3d transform(Vec3d src, Vec3d dst) {
        if (dst == null) return new Vec3d(src);
        dst.set(src);
        return dst;
    }

    @Override
    public Vec3d deltaTransform(Vec3d src, Vec3d dst) {
        if (dst == null) return new Vec3d(src);
        dst.set(src);
        return dst;
    }

    @Override
    public Vec3d inverseTransform(Vec3d src, Vec3d dst) {
        if (dst == null) return new Vec3d(src);
        dst.set(src);
        return dst;
    }

    @Override
    public Vec3d inverseDeltaTransform(Vec3d src, Vec3d dst) {
        if (dst == null) return new Vec3d(src);
        dst.set(src);
        return dst;
    }

    public void transform(float[] srcPts, int srcOff,
                          float[] dstPts, int dstOff,
                          int numPts) {
        if (srcPts != dstPts || srcOff != dstOff) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
        }
    }

    public void transform(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) {
        if (srcPts != dstPts || srcOff != dstOff) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
        }
    }

    public void transform(float[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) {
        for (int i = 0; i < numPts; i++) {
            dstPts[dstOff++] = srcPts[srcOff++];
            dstPts[dstOff++] = srcPts[srcOff++];
        }
    }

    public void transform(double[] srcPts, int srcOff,
                          float[] dstPts, int dstOff,
                          int numPts) {
        for (int i = 0; i < numPts; i++) {
            dstPts[dstOff++] = (float) srcPts[srcOff++];
            dstPts[dstOff++] = (float) srcPts[srcOff++];
        }
    }

    @Override
    public void deltaTransform(float[] srcPts, int srcOff,
                               float[] dstPts, int dstOff,
                               int numPts) {
        if (srcPts != dstPts || srcOff != dstOff) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
        }
    }

    @Override
    public void deltaTransform(double[] srcPts, int srcOff,
                               double[] dstPts, int dstOff,
                               int numPts) {
        if (srcPts != dstPts || srcOff != dstOff) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
        }
    }

    public void inverseTransform(float[] srcPts, int srcOff,
                                 float[] dstPts, int dstOff,
                                 int numPts) {
        if (srcPts != dstPts || srcOff != dstOff) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
        }
    }

    public void inverseDeltaTransform(float[] srcPts, int srcOff,
                                      float[] dstPts, int dstOff,
                                      int numPts) {
        if (srcPts != dstPts || srcOff != dstOff) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
        }
    }

    public void inverseTransform(double[] srcPts, int srcOff,
                                 double[] dstPts, int dstOff,
                                 int numPts) {
        if (srcPts != dstPts || srcOff != dstOff) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
        }
    }

    @Override
    public BaseBounds transform(BaseBounds bounds, BaseBounds result) {
        if (result != bounds) {
            result = result.deriveWithNewBounds(bounds);
        }
        return result;
    }

    @Override
    public void transform(Rectangle rect, Rectangle result) {
        if (result != rect) {
            result.setBounds(rect);
        }
    }

    @Override
    public BaseBounds inverseTransform(BaseBounds bounds, BaseBounds result) {
        if (result != bounds) {
            result = result.deriveWithNewBounds(bounds);
        }
        return result;
    }

    @Override
    public void inverseTransform(Rectangle rect, Rectangle result) {
        if (result != rect) {
            result.setBounds(rect);
        }
    }

    @Override
    public void setToIdentity() {
    }

    @Override
    public void setTransform(BaseTransform xform) {
        if (!xform.isIdentity()) {
            degreeError(Degree.IDENTITY);
        }
    }

    @Override
    public void invert() {
    }

    @Override
    public void restoreTransform(double mxx, double myx,
                                 double mxy, double myy,
                                 double mxt, double myt) {
        if (mxx != 1.0 || myx != 0.0 ||
                mxy != 0.0 || myy != 1.0 ||
                mxt != 0.0 || myt != 0.0) {
            degreeError(Degree.IDENTITY);
        }
    }

    @Override
    public void restoreTransform(double mxx, double mxy, double mxz, double mxt,
                                 double myx, double myy, double myz, double myt,
                                 double mzx, double mzy, double mzz, double mzt) {
        if (mxx != 1.0 || mxy != 0.0 || mxz != 0.0 || mxt != 0.0 ||
                myx != 0.0 || myy != 1.0 || myz != 0.0 || myt != 0.0 ||
                mzx != 0.0 || mzy != 0.0 || mzz != 1.0 || mzt != 0.0) {
            degreeError(Degree.IDENTITY);
        }
    }

    @Override
    public BaseTransform deriveWithTranslation(double mxt, double myt) {
        return Translate2D.getInstance(mxt, myt);
    }

    @Override
    public BaseTransform deriveWithPreTranslation(double mxt, double myt) {
        return Translate2D.getInstance(mxt, myt);
    }

    @Override
    public BaseTransform deriveWithTranslation(double mxt, double myt, double mzt) {
        if (mzt == 0.0) {
            if (mxt == 0.0 && myt == 0.0) {
                return this;
            }
            return new Translate2D(mxt, myt);
        }
        Affine3D a = new Affine3D();
        a.translate(mxt, myt, mzt);
        return a;
    }

    @Override
    public BaseTransform deriveWithScale(double mxx, double myy, double mzz) {
        if (mzz == 1.0) {
            if (mxx == 1.0 && myy == 1.0) {
                return this;
            }
            Affine2D a = new Affine2D();
            a.scale(mxx, myy);
            return a;
        }
        Affine3D a = new Affine3D();
        a.scale(mxx, myy, mzz);
        return a;

    }

    @Override
    public BaseTransform deriveWithRotation(double theta,
                                            double axisX, double axisY, double axisZ) {
        if (theta == 0.0) {
            return this;
        }
        if (almostZero(axisX) && almostZero(axisY)) {
            if (axisZ == 0.0) {
                return this;
            }
            Affine2D a = new Affine2D();
            if (axisZ > 0) {
                a.rotate(theta);
            } else if (axisZ < 0) {
                a.rotate(-theta);
            }
            return a;
        }
        Affine3D a = new Affine3D();
        a.rotate(theta, axisX, axisY, axisZ);
        return a;
    }

    @Override
    public BaseTransform deriveWithConcatenation(double mxx, double myx,
                                                 double mxy, double myy,
                                                 double mxt, double myt) {
        return getInstance(mxx, myx,
                mxy, myy,
                mxt, myt);
    }

    @Override
    public BaseTransform deriveWithConcatenation(
            double mxx, double mxy, double mxz, double mxt,
            double myx, double myy, double myz, double myt,
            double mzx, double mzy, double mzz, double mzt) {
        return getInstance(mxx, mxy, mxz, mxt,
                myx, myy, myz, myt,
                mzx, mzy, mzz, mzt);
    }

    @Override
    public BaseTransform deriveWithConcatenation(BaseTransform tx) {
        return getInstance(tx);
    }

    @Override
    public BaseTransform deriveWithPreConcatenation(BaseTransform tx) {
        return getInstance(tx);
    }

    @Override
    public BaseTransform deriveWithNewTransform(BaseTransform tx) {
        return getInstance(tx);
    }

    @Override
    public BaseTransform createInverse() {
        return this;
    }

    @Override
    public String toString() {
        return ("Identity[]");
    }

    @Override
    public BaseTransform copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof BaseTransform &&
                ((BaseTransform) obj).isIdentity());
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
