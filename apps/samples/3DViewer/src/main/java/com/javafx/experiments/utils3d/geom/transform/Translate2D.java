/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates.
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

import com.javafx.experiments.utils3d.geom.Rectangle;
import com.javafx.experiments.utils3d.geom.BaseBounds;
import com.javafx.experiments.utils3d.geom.Vec3d;
import com.javafx.experiments.utils3d.geom.Point2D;

public class Translate2D extends BaseTransform {
    private double mxt;
    private double myt;

    public static BaseTransform getInstance(double mxt, double myt) {
        if (mxt == 0.0 && myt == 0.0) {
            return IDENTITY_TRANSFORM;
        } else {
            return new Translate2D(mxt, myt);
        }
    }

    public Translate2D(double tx, double ty) {
        this.mxt = tx;
        this.myt = ty;
    }

    public Translate2D(BaseTransform tx) {
        if (!tx.isTranslateOrIdentity()) {
            degreeError(Degree.TRANSLATE_2D);
        }
        this.mxt = tx.getMxt();
        this.myt = tx.getMyt();
    }

    @Override
    public Degree getDegree() {
        return Degree.TRANSLATE_2D;
    }

    @Override
    public double getDeterminant() {
        return 1.0;
    }

    @Override
    public double getMxt() {
        return mxt;
    }

    @Override
    public double getMyt() {
        return myt;
    }

    @Override
    public int getType() {
        return (mxt == 0.0 && myt == 0.0) ? TYPE_IDENTITY : TYPE_TRANSLATION;
    }

    @Override
    public boolean isIdentity() {
        return (mxt == 0.0 && myt == 0.0);
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
    public Point2D transform(Point2D src, Point2D dst) {
        if (dst == null) dst = makePoint(src, dst);
        dst.setLocation(
                (float) (src.x + mxt),
                (float) (src.y + myt));
        return dst;
    }

    @Override
    public Point2D inverseTransform(Point2D src, Point2D dst) {
        if (dst == null) dst = makePoint(src, dst);
        dst.setLocation(
                (float) (src.x - mxt),
                (float) (src.y - myt));
        return dst;
    }

    @Override
    public Vec3d transform(Vec3d src, Vec3d dst) {
        if (dst == null) {
            dst = new Vec3d();
        }
        dst.x = src.x + mxt;
        dst.y = src.y + myt;
        dst.z = src.z;
        return dst;
    }

    @Override
    public Vec3d deltaTransform(Vec3d src, Vec3d dst) {
        if (dst == null) {
            dst = new Vec3d();
        }
        dst.set(src);
        return dst;
    }

    @Override
    public Vec3d inverseTransform(Vec3d src, Vec3d dst) {
        if (dst == null) {
            dst = new Vec3d();
        }
        dst.x = src.x - mxt;
        dst.y = src.y - myt;
        dst.z = src.z;
        return dst;
    }

    @Override
    public Vec3d inverseDeltaTransform(Vec3d src, Vec3d dst) {
        if (dst == null) {
            dst = new Vec3d();
        }
        dst.set(src);
        return dst;
    }

    @Override
    public void transform(float[] srcPts, int srcOff,
                          float[] dstPts, int dstOff,
                          int numPts) {
        float tx = (float) this.mxt;
        float ty = (float) this.myt;
        if (dstPts == srcPts) {
            if (dstOff > srcOff && dstOff < srcOff + numPts * 2) {
                // If the arrays overlap partially with the destination higher
                // than the source and we transform the coordinates normally
                // we would overwrite some of the later source coordinates
                // with results of previous transformations.
                // To get around this we use arraycopy to copy the points
                // to their final destination with correct overwrite
                // handling and then transform them in place in the new
                // safer location.
                System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
                // srcPts = dstPts;     // They are known to be equal.
                srcOff = dstOff;
            }
            if (dstOff == srcOff && tx == 0.0f && ty == 0.0f) {
                return;
            }
        }
        for (int i = 0; i < numPts; i++) {
            dstPts[dstOff++] = srcPts[srcOff++] + tx;
            dstPts[dstOff++] = srcPts[srcOff++] + ty;
        }
    }

    @Override
    public void transform(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) {
        double tx = this.mxt;
        double ty = this.myt;
        if (dstPts == srcPts) {
            if (dstOff > srcOff && dstOff < srcOff + numPts * 2) {
                // If the arrays overlap partially with the destination higher
                // than the source and we transform the coordinates normally
                // we would overwrite some of the later source coordinates
                // with results of previous transformations.
                // To get around this we use arraycopy to copy the points
                // to their final destination with correct overwrite
                // handling and then transform them in place in the new
                // safer location.
                System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
                // srcPts = dstPts;     // They are known to be equal.
                srcOff = dstOff;
            }
            if (dstOff == srcOff && tx == 0.0 && ty == 0.0) {
                return;
            }
        }
        for (int i = 0; i < numPts; i++) {
            dstPts[dstOff++] = srcPts[srcOff++] + tx;
            dstPts[dstOff++] = srcPts[srcOff++] + ty;
        }
    }

    @Override
    public void transform(float[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) {
        double tx = this.mxt;
        double ty = this.myt;
        for (int i = 0; i < numPts; i++) {
            dstPts[dstOff++] = srcPts[srcOff++] + tx;
            dstPts[dstOff++] = srcPts[srcOff++] + ty;
        }
    }

    @Override
    public void transform(double[] srcPts, int srcOff,
                          float[] dstPts, int dstOff,
                          int numPts) {
        double tx = this.mxt;
        double ty = this.myt;
        for (int i = 0; i < numPts; i++) {
            dstPts[dstOff++] = (float) (srcPts[srcOff++] + tx);
            dstPts[dstOff++] = (float) (srcPts[srcOff++] + ty);
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

    @Override
    public void inverseTransform(float[] srcPts, int srcOff,
                                 float[] dstPts, int dstOff,
                                 int numPts) {
        float tx = (float) this.mxt;
        float ty = (float) this.myt;
        if (dstPts == srcPts) {
            if (dstOff > srcOff && dstOff < srcOff + numPts * 2) {
                // If the arrays overlap partially with the destination higher
                // than the source and we transform the coordinates normally
                // we would overwrite some of the later source coordinates
                // with results of previous transformations.
                // To get around this we use arraycopy to copy the points
                // to their final destination with correct overwrite
                // handling and then transform them in place in the new
                // safer location.
                System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
                // srcPts = dstPts;     // They are known to be equal.
                srcOff = dstOff;
            }
            if (dstOff == srcOff && tx == 0.0f && ty == 0.0f) {
                return;
            }
        }
        for (int i = 0; i < numPts; i++) {
            dstPts[dstOff++] = srcPts[srcOff++] - tx;
            dstPts[dstOff++] = srcPts[srcOff++] - ty;
        }
    }

    @Override
    public void inverseDeltaTransform(float[] srcPts, int srcOff,
                                      float[] dstPts, int dstOff,
                                      int numPts) {
        if (srcPts != dstPts || srcOff != dstOff) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
        }
    }

    @Override
    public void inverseTransform(double[] srcPts, int srcOff,
                                 double[] dstPts, int dstOff,
                                 int numPts) {
        double tx = this.mxt;
        double ty = this.myt;
        if (dstPts == srcPts) {
            if (dstOff > srcOff && dstOff < srcOff + numPts * 2) {
                // If the arrays overlap partially with the destination higher
                // than the source and we transform the coordinates normally
                // we would overwrite some of the later source coordinates
                // with results of previous transformations.
                // To get around this we use arraycopy to copy the points
                // to their final destination with correct overwrite
                // handling and then transform them in place in the new
                // safer location.
                System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
                // srcPts = dstPts;     // They are known to be equal.
                srcOff = dstOff;
            }
            if (dstOff == srcOff && tx == 0f && ty == 0f) {
                return;
            }
        }
        for (int i = 0; i < numPts; i++) {
            dstPts[dstOff++] = srcPts[srcOff++] - tx;
            dstPts[dstOff++] = srcPts[srcOff++] - ty;
        }
    }

    @Override
    public BaseBounds transform(BaseBounds bounds, BaseBounds result) {
        float minX = (float) (bounds.getMinX() + mxt);
        float minY = (float) (bounds.getMinY() + myt);
        float minZ = bounds.getMinZ();
        float maxX = (float) (bounds.getMaxX() + mxt);
        float maxY = (float) (bounds.getMaxY() + myt);
        float maxZ = bounds.getMaxZ();
        return result.deriveWithNewBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void transform(Rectangle rect, Rectangle result) {
        transform(rect, result, mxt, myt);
    }

    @Override
    public BaseBounds inverseTransform(BaseBounds bounds, BaseBounds result) {
        float minX = (float) (bounds.getMinX() - mxt);
        float minY = (float) (bounds.getMinY() - myt);
        float minZ = bounds.getMinZ();
        float maxX = (float) (bounds.getMaxX() - mxt);
        float maxY = (float) (bounds.getMaxY() - myt);
        float maxZ = bounds.getMaxZ();
        return result.deriveWithNewBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void inverseTransform(Rectangle rect, Rectangle result) {
        transform(rect, result, -mxt, -myt);
    }

    static void transform(Rectangle rect, Rectangle result,
                          double mxt, double myt) {
        int imxt = (int) mxt;
        int imyt = (int) myt;
        if (imxt == mxt && imyt == myt) {
            result.setBounds(rect);
            result.translate(imxt, imyt);
        } else {
            double x1 = rect.x + mxt;
            double y1 = rect.y + myt;
            double x2 = Math.ceil(x1 + rect.width);
            double y2 = Math.ceil(y1 + rect.height);
            x1 = Math.floor(x1);
            y1 = Math.floor(y1);
            result.setBounds((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
        }
    }

    @Override
    public void setToIdentity() {
        this.mxt = this.myt = 0.0;
    }

    @Override
    public void setTransform(BaseTransform xform) {
        if (!xform.isTranslateOrIdentity()) {
            degreeError(Degree.TRANSLATE_2D);
        }
        this.mxt = xform.getMxt();
        this.myt = xform.getMyt();
    }

    @Override
    public void invert() {
        this.mxt = -this.mxt;
        this.myt = -this.myt;
    }

    @Override
    public void restoreTransform(double mxx, double myx,
                                 double mxy, double myy,
                                 double mxt, double myt) {
        if (mxx != 1.0 || myx != 0.0 ||
                mxy != 0.0 || myy != 1.0) {
            degreeError(Degree.TRANSLATE_2D);
        }
        this.mxt = mxt;
        this.myt = myt;
    }

    @Override
    public void restoreTransform(double mxx, double mxy, double mxz, double mxt,
                                 double myx, double myy, double myz, double myt,
                                 double mzx, double mzy, double mzz, double mzt) {
        if (mxx != 1.0 || mxy != 0.0 || mxz != 0.0 ||
                myx != 0.0 || myy != 1.0 || myz != 0.0 ||
                mzx != 0.0 || mzy != 0.0 || mzz != 1.0 || mzt != 0.0) {
            degreeError(Degree.TRANSLATE_2D);
        }
        this.mxt = mxt;
        this.myt = myt;
    }

    @Override
    public BaseTransform deriveWithTranslation(double mxt, double myt) {
        this.mxt += mxt;
        this.myt += myt;
        return this;
    }

    @Override
    public BaseTransform deriveWithTranslation(double mxt, double myt, double mzt) {
        if (mzt == 0.0) {
            this.mxt += mxt;
            this.myt += myt;
            return this;
        }
        Affine3D a = new Affine3D();
        a.translate(this.mxt + mxt, this.myt + myt, mzt);
        return a;
    }

    @Override
    public BaseTransform deriveWithScale(double mxx, double myy, double mzz) {
        if (mzz == 1.0) {
            if (mxx == 1.0 && myy == 1.0) {
                return this;
            }
            Affine2D a = new Affine2D();
            a.translate(this.mxt, this.myt);
            a.scale(mxx, myy);
            return a;
        }
        Affine3D a = new Affine3D();
        a.translate(this.mxt, this.myt);
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
            a.translate(this.mxt, this.myt);
            if (axisZ > 0) {
                a.rotate(theta);
            } else if (axisZ < 0) {
                a.rotate(-theta);
            }
            return a;
        }
        Affine3D a = new Affine3D();
        a.translate(this.mxt, this.myt);
        a.rotate(theta, axisX, axisY, axisZ);
        return a;
    }

    @Override
    public BaseTransform deriveWithPreTranslation(double mxt, double myt) {
        this.mxt += mxt;
        this.myt += myt;
        return this;
    }

    @Override
    public BaseTransform deriveWithConcatenation(double mxx, double myx,
                                                 double mxy, double myy,
                                                 double mxt, double myt) {
        if (mxx == 1.0 && myx == 0.0 && mxy == 0.0 && myy == 1.0) {
            this.mxt += mxt;
            this.myt += myt;
            return this;
        } else {
            return new Affine2D(mxx, myx,
                    mxy, myy,
                    this.mxt + mxt, this.myt + myt);
        }
    }

    @Override
    public BaseTransform deriveWithConcatenation(
            double mxx, double mxy, double mxz, double mxt,
            double myx, double myy, double myz, double myt,
            double mzx, double mzy, double mzz, double mzt) {
        if (mxz == 0.0
                && myz == 0.0
                && mzx == 0.0 && mzy == 0.0 && mzz == 1.0 && mzt == 0.0) {
            return deriveWithConcatenation(mxx, myx,
                    mxy, myy,
                    mxt, myt);
        }

        return new Affine3D(mxx, mxy, mxz, mxt + this.mxt,
                myx, myy, myz, myt + this.myt,
                mzx, mzy, mzz, mzt);
    }

    @Override
    public BaseTransform deriveWithConcatenation(BaseTransform tx) {
        if (tx.isTranslateOrIdentity()) {
            this.mxt += tx.getMxt();
            this.myt += tx.getMyt();
            return this;
        } else if (tx.is2D()) {
            return getInstance(tx.getMxx(), tx.getMyx(),
                    tx.getMxy(), tx.getMyy(),
                    this.mxt + tx.getMxt(), this.myt + tx.getMyt());
        } else {
            Affine3D t3d = new Affine3D(tx);
            t3d.preTranslate(this.mxt, this.myt, 0.0);
            return t3d;
        }
    }

    @Override
    public BaseTransform deriveWithPreConcatenation(BaseTransform tx) {
        if (tx.isTranslateOrIdentity()) {
            this.mxt += tx.getMxt();
            this.myt += tx.getMyt();
            return this;
        } else if (tx.is2D()) {
            Affine2D t2d = new Affine2D(tx);
            t2d.translate(this.mxt, this.myt);
            return t2d;
        } else {
            Affine3D t3d = new Affine3D(tx);
            t3d.translate(this.mxt, this.myt, 0.0);
            return t3d;
        }
    }

    @Override
    public BaseTransform deriveWithNewTransform(BaseTransform tx) {
        if (tx.isTranslateOrIdentity()) {
            this.mxt = tx.getMxt();
            this.myt = tx.getMyt();
            return this;
        } else {
            return getInstance(tx);
        }
    }

    @Override
    public BaseTransform createInverse() {
        if (isIdentity()) {
            return IDENTITY_TRANSFORM;
        } else {
            return new Translate2D(-this.mxt, -this.myt);
        }
    }

    // Round values to sane precision for printing
    // Note that Math.sin(Math.PI) has an error of about 10^-16
    private static double _matround(double matval) {
        return Math.rint(matval * 1E15) / 1E15;
    }

    @Override
    public String toString() {
        return ("Translate2D["
                + _matround(mxt) + ", "
                + _matround(myt) + "]");
    }

    @Override
    public BaseTransform copy() {
        return new Translate2D(this.mxt, this.myt);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseTransform) {
            BaseTransform tx = (BaseTransform) obj;
            return (tx.isTranslateOrIdentity() &&
                    tx.getMxt() == this.mxt &&
                    tx.getMyt() == this.myt);
        }
        return false;
    }

    private static final long BASE_HASH;

    static {
        long bits = 0;
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMzz());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMzy());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMzx());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMyz());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMxz());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMyy());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMyx());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMxy());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMxx());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMzt());
        BASE_HASH = bits;
    }

    @Override
    public int hashCode() {
        if (isIdentity()) return 0;
        long bits = BASE_HASH;
        bits = bits * 31 + Double.doubleToLongBits(getMyt());
        bits = bits * 31 + Double.doubleToLongBits(getMxt());
        return (((int) bits) ^ ((int) (bits >> 32)));
    }
}
