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

import com.javafx.experiments.utils3d.geom.BaseBounds;
import com.javafx.experiments.utils3d.geom.Point2D;
import com.javafx.experiments.utils3d.geom.Rectangle;
import com.javafx.experiments.utils3d.geom.Vec3d;

public class Affine3D extends AffineBase {
    private double mxz;
    private double myz;
    private double mzx;
    private double mzy;
    private double mzz;
    private double mzt;

    public Affine3D() {
        mxx = myy = mzz = 1.0;
//        mxy = mxz = mxt = 0.0;  /* Not needed. */
//        myx = myz = myt = 0.0;  /* Not needed. */
//        mzx = mzy = mzt = 0.0;  /* Not needed. */
//        type = TYPE_IDENTITY;   /* Not needed. */
    }

    public Affine3D(BaseTransform transform) {
        setTransform(transform);
    }

    public Affine3D(double mxx, double mxy, double mxz, double mxt,
                    double myx, double myy, double myz, double myt,
                    double mzx, double mzy, double mzz, double mzt) {
        this.mxx = mxx;
        this.mxy = mxy;
        this.mxz = mxz;
        this.mxt = mxt;

        this.myx = myx;
        this.myy = myy;
        this.myz = myz;
        this.myt = myt;

        this.mzx = mzx;
        this.mzy = mzy;
        this.mzz = mzz;
        this.mzt = mzt;

        updateState();
    }

    public Affine3D(Affine3D other) {
        this.mxx = other.mxx;
        this.mxy = other.mxy;
        this.mxz = other.mxz;
        this.mxt = other.mxt;

        this.myx = other.myx;
        this.myy = other.myy;
        this.myz = other.myz;
        this.myt = other.myt;

        this.mzx = other.mzx;
        this.mzy = other.mzy;
        this.mzz = other.mzz;
        this.mzt = other.mzt;

        this.state = other.state;
        this.type = other.type;
    }

    @Override
    public BaseTransform copy() {
        return new Affine3D(this);
    }

    @Override
    public Degree getDegree() {
        return Degree.AFFINE_3D;
    }

    @Override
    protected void reset3Delements() {
        this.mxz = 0.0;
        this.myz = 0.0;
        this.mzx = 0.0;
        this.mzy = 0.0;
        this.mzz = 1.0;
        this.mzt = 0.0;
    }

    @Override
    protected void updateState() {
        super.updateState();
        if (!almostZero(mxz) ||
                !almostZero(myz) ||
                !almostZero(mzx) ||
                !almostZero(mzy) ||
                !almostOne(mzz) ||
                !almostZero(mzt)) {
            state |= APPLY_3D;
            if (type != TYPE_UNKNOWN) {
                type |= TYPE_AFFINE_3D;
            }
        }
    }

    @Override
    public double getMxz() {
        return mxz;
    }

    @Override
    public double getMyz() {
        return myz;
    }

    @Override
    public double getMzx() {
        return mzx;
    }

    @Override
    public double getMzy() {
        return mzy;
    }

    @Override
    public double getMzz() {
        return mzz;
    }

    @Override
    public double getMzt() {
        return mzt;
    }

    @Override
    public double getDeterminant() {
        if ((state & APPLY_3D) == 0) {
            return super.getDeterminant();
        }
//        D=a11{a22a33-a32a23}
//         +a12{a23a31-a33a21}
//         +a13{a21a32-a31a22}
        return (mxx * (myy * mzz - mzy * myz) +
                mxy * (myz * mzx - mzz * myx) +
                mxz * (myx * mzy - mzx * myy));
    }

    public void setTransform(BaseTransform transform) {
        this.mxx = transform.getMxx();
        this.mxy = transform.getMxy();
        this.mxz = transform.getMxz();
        this.mxt = transform.getMxt();
        this.myx = transform.getMyx();
        this.myy = transform.getMyy();
        this.myz = transform.getMyz();
        this.myt = transform.getMyt();
        this.mzx = transform.getMzx();
        this.mzy = transform.getMzy();
        this.mzz = transform.getMzz();
        this.mzt = transform.getMzt();
        updateState();
    }

    public void setTransform(double mxx, double mxy, double mxz, double mxt,
                             double myx, double myy, double myz, double myt,
                             double mzx, double mzy, double mzz, double mzt) {
        this.mxx = mxx;
        this.mxy = mxy;
        this.mxz = mxz;
        this.mxt = mxt;

        this.myx = myx;
        this.myy = myy;
        this.myz = myz;
        this.myt = myt;

        this.mzx = mzx;
        this.mzy = mzy;
        this.mzz = mzz;
        this.mzt = mzt;

        updateState();
    }

    public void setToTranslation(double tx, double ty, double tz) {
        this.mxx = 1.0;
        this.mxy = 0.0;
        this.mxz = 0.0;
        this.mxt = tx;

        this.myx = 0.0;
        this.myy = 1.0;
        this.myz = 0.0;
        this.myt = ty;

        this.mzx = 0.0;
        this.mzy = 0.0;
        this.mzz = 1.0;
        this.mzt = tz;

        if (tz == 0.0) {
            if (tx == 0.0 && ty == 0.0) {
                state = APPLY_IDENTITY;
                type = TYPE_IDENTITY;
            } else {
                state = APPLY_TRANSLATE;
                type = TYPE_TRANSLATION;
            }
        } else {
            if (tx == 0.0 && ty == 0.0) {
                state = APPLY_3D;
                type = TYPE_AFFINE_3D;
            } else {
                state = APPLY_TRANSLATE | APPLY_3D;
                type = TYPE_TRANSLATION | TYPE_AFFINE_3D;
            }
        }
    }

    public void setToScale(double sx, double sy, double sz) {
        this.mxx = sx;
        this.mxy = 0.0;
        this.mxz = 0.0;
        this.mxt = 0.0;

        this.myx = 0.0;
        this.myy = sy;
        this.myz = 0.0;
        this.myt = 0.0;

        this.mzx = 0.0;
        this.mzy = 0.0;
        this.mzz = sz;
        this.mzt = 0.0;

        if (sz == 1.0) {
            if (sx == 1.0 && sy == 1.0) {
                state = APPLY_IDENTITY;
                type = TYPE_IDENTITY;
            } else {
                state = APPLY_SCALE;
                type = TYPE_UNKNOWN;
            }
        } else {
            if (sx == 1.0 && sy == 1.0) {
                state = APPLY_3D;
                type = TYPE_AFFINE_3D;
            } else {
                state = APPLY_SCALE | APPLY_3D;
                type = TYPE_UNKNOWN;
            }
        }
    }

    public void setToRotation(double theta,
                              double axisX, double axisY, double axisZ,
                              double pivotX, double pivotY, double pivotZ) {
        setToRotation(theta, axisX, axisY, axisZ);
        if (pivotX != 0.0 || pivotY != 0.0 || pivotZ != 0.0) {
            preTranslate(pivotX, pivotY, pivotZ);
            translate(-pivotX, -pivotY, -pivotZ);
        }
    }

    public void setToRotation(double theta, double axisX, double axisY, double axisZ) {
        double mag = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

        if (almostZero(mag)) {
            setToIdentity();
            return;
        }
        mag = 1.0 / mag;
        double ax = axisX * mag;
        double ay = axisY * mag;
        double az = axisZ * mag;

        double sinTheta = Math.sin(theta);
        double cosTheta = Math.cos(theta);
        double t = 1.0 - cosTheta;

        double xz = ax * az;
        double xy = ax * ay;
        double yz = ay * az;

        this.mxx = t * ax * ax + cosTheta;
        this.mxy = t * xy - sinTheta * az;
        this.mxz = t * xz + sinTheta * ay;
        this.mxt = 0.0;

        this.myx = t * xy + sinTheta * az;
        this.myy = t * ay * ay + cosTheta;
        this.myz = t * yz - sinTheta * ax;
        this.myt = 0.0;

        this.mzx = t * xz - sinTheta * ay;
        this.mzy = t * yz + sinTheta * ax;
        this.mzz = t * az * az + cosTheta;
        this.mzt = 0.0;

        updateState();
    }

    @Override
    public BaseBounds transform(BaseBounds src, BaseBounds dst) {
        if ((state & APPLY_3D) == 0) {
            return dst = super.transform(src, dst);
        }

        switch (state) {
            default:
            /* NOBREAK */
                // TODO: Optimize these cases ... (RT-26800)
            case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            /* NOBREAK */
            case (APPLY_SHEAR | APPLY_SCALE):
            /* NOBREAK */
            case (APPLY_SHEAR | APPLY_TRANSLATE):
            /* NOBREAK */
            case (APPLY_SHEAR):
                Vec3d tempV3d = new Vec3d();
                dst = TransformHelper.general3dBoundsTransform(this, src, dst, tempV3d);
                break;
            case (APPLY_SCALE | APPLY_TRANSLATE):
                dst = dst.deriveWithNewBoundsAndSort(
                        (float) (src.getMinX() * mxx + mxt),
                        (float) (src.getMinY() * myy + myt),
                        (float) (src.getMinZ() * mzz + mzt),
                        (float) (src.getMaxX() * mxx + mxt),
                        (float) (src.getMaxY() * myy + myt),
                        (float) (src.getMaxZ() * mzz + mzt));
                break;
            case (APPLY_SCALE):
                dst = dst.deriveWithNewBoundsAndSort(
                        (float) (src.getMinX() * mxx),
                        (float) (src.getMinY() * myy),
                        (float) (src.getMinZ() * mzz),
                        (float) (src.getMaxX() * mxx),
                        (float) (src.getMaxY() * myy),
                        (float) (src.getMaxZ() * mzz));
                break;
            case (APPLY_TRANSLATE):
                dst = dst.deriveWithNewBounds(
                        (float) (src.getMinX() + mxt),
                        (float) (src.getMinY() + myt),
                        (float) (src.getMinZ() + mzt),
                        (float) (src.getMaxX() + mxt),
                        (float) (src.getMaxY() + myt),
                        (float) (src.getMaxZ() + mzt));
                break;
            case (APPLY_IDENTITY):
                if (src != dst) {
                    dst = dst.deriveWithNewBounds(src);
                }

                break;
        }
        return dst;
    }


    @Override
    public Vec3d transform(Vec3d src, Vec3d dst) {
        if ((state & APPLY_3D) == 0) {
            return super.transform(src, dst);
        }
        if (dst == null) {
            dst = new Vec3d();
        }
        double x = src.x;
        double y = src.y;
        double z = src.z;
        dst.x = mxx * x + mxy * y + mxz * z + mxt;
        dst.y = myx * x + myy * y + myz * z + myt;
        dst.z = mzx * x + mzy * y + mzz * z + mzt;
        return dst;
    }

    @Override
    public Vec3d deltaTransform(Vec3d src, Vec3d dst) {
        if ((state & APPLY_3D) == 0) {
            return super.deltaTransform(src, dst);
        }
        if (dst == null) {
            dst = new Vec3d();
        }
        double x = src.x;
        double y = src.y;
        double z = src.z;
        dst.x = mxx * x + mxy * y + mxz * z;
        dst.y = myx * x + myy * y + myz * z;
        dst.z = mzx * x + mzy * y + mzz * z;
        return dst;
    }

    @Override
    public void inverseTransform(float[] srcPts, int srcOff,
                                 float[] dstPts, int dstOff,
                                 int numPts)
            throws NoninvertibleTransformException {
        if ((state & APPLY_3D) == 0) {
            super.inverseTransform(srcPts, srcOff, dstPts, dstOff, numPts);
        } else {
            // TODO: Optimize... (RT-26800)
            createInverse().transform(srcPts, srcOff, dstPts, dstOff, numPts);
        }
    }

    @Override
    public void inverseDeltaTransform(float[] srcPts, int srcOff,
                                      float[] dstPts, int dstOff,
                                      int numPts)
            throws NoninvertibleTransformException {
        if ((state & APPLY_3D) == 0) {
            super.inverseDeltaTransform(srcPts, srcOff, dstPts, dstOff, numPts);
        } else {
            // TODO: Optimize... (RT-26800)
            createInverse().deltaTransform(srcPts, srcOff, dstPts, dstOff, numPts);
        }
    }

    @Override
    public void inverseTransform(double[] srcPts, int srcOff,
                                 double[] dstPts, int dstOff,
                                 int numPts)
            throws NoninvertibleTransformException {
        if ((state & APPLY_3D) == 0) {
            super.inverseTransform(srcPts, srcOff, dstPts, dstOff, numPts);
        } else {
            // TODO: Optimize... (RT-26800)
            createInverse().transform(srcPts, srcOff, dstPts, dstOff, numPts);
        }
    }

    @Override
    public Point2D inverseTransform(Point2D src, Point2D dst)
            throws NoninvertibleTransformException {
        if ((state & APPLY_3D) == 0) {
            return super.inverseTransform(src, dst);
        } else {
            // TODO: Optimize... (RT-26800)
            return createInverse().transform(src, dst);
        }
    }

    @Override
    public Vec3d inverseTransform(Vec3d src, Vec3d dst)
            throws NoninvertibleTransformException {
        if ((state & APPLY_3D) == 0) {
            return super.inverseTransform(src, dst);
        } else {
            // TODO: Optimize... (RT-26800)
            return createInverse().transform(src, dst);
        }
    }

    @Override
    public Vec3d inverseDeltaTransform(Vec3d src, Vec3d dst)
            throws NoninvertibleTransformException {
        if ((state & APPLY_3D) == 0) {
            return super.inverseDeltaTransform(src, dst);
        } else {
            // TODO: Optimize... (RT-26800)
            return createInverse().deltaTransform(src, dst);
        }
    }

    @Override
    public BaseBounds inverseTransform(BaseBounds bounds, BaseBounds result)
            throws NoninvertibleTransformException {
        if ((state & APPLY_3D) == 0) {
            result = super.inverseTransform(bounds, result);
        } else {
            // TODO: Optimize... (RT-26800)
            result = createInverse().transform(bounds, result);
        }
        return result;
    }

    @Override
    public void inverseTransform(Rectangle bounds, Rectangle result)
            throws NoninvertibleTransformException {
        if ((state & APPLY_3D) == 0) {
            super.inverseTransform(bounds, result);
        } else {
            // TODO: Optimize... (RT-26800)
            createInverse().transform(bounds, result);
        }
    }

    @Override
    public BaseTransform createInverse()
            throws NoninvertibleTransformException {
        BaseTransform t = copy();
        t.invert();
        return t;
    }

    @Override
    public void invert()
            throws NoninvertibleTransformException {
        if ((state & APPLY_3D) == 0) {
            super.invert();
            return;
        }
        // InvM = Transpose(Cofactor(M)) / det(M)
        // Cofactor(M) = matrix of cofactors(0..3,0..3)
        // cofactor(r,c) = (-1 if r+c is odd) * minor(r,c)
        // minor(r,c) = det(M with row r and col c removed)
        // For an Affine3D matrix, minor(r, 3) is {0, 0, 0, det}
        // which generates {0, 0, 0, 1} and so can be ignored.

        // TODO: Inlining the minor calculations should allow them
        // to be simplified... (RT-26800)
        double cxx = minor(0, 0);
        double cyx = -minor(0, 1);
        double czx = minor(0, 2);
        double cxy = -minor(1, 0);
        double cyy = minor(1, 1);
        double czy = -minor(1, 2);
        double cxz = minor(2, 0);
        double cyz = -minor(2, 1);
        double czz = minor(2, 2);
        double cxt = -minor(3, 0);
        double cyt = minor(3, 1);
        double czt = -minor(3, 2);
        double det = getDeterminant();
        mxx = cxx / det;
        mxy = cxy / det;
        mxz = cxz / det;
        mxt = cxt / det;
        myx = cyx / det;
        myy = cyy / det;
        myz = cyz / det;
        myt = cyt / det;
        mzx = czx / det;
        mzy = czy / det;
        mzz = czz / det;
        mzt = czt / det;
        updateState();
    }

    private double minor(int row, int col) {
        double m00 = mxx, m01 = mxy, m02 = mxz;
        double m10 = myx, m11 = myy, m12 = myz;
        double m20 = mzx, m21 = mzy, m22 = mzz;
        switch (col) {
            case 0:
                m00 = m01;
                m10 = m11;
                m20 = m21;
            case 1:
                m01 = m02;
                m11 = m12;
                m21 = m22;
            case 2:
                m02 = mxt;
                m12 = myt;
                m22 = mzt;
        }
        switch (row) {
            case 0:
                m00 = m10;
                m01 = m11;
//                m02 = m12;
            case 1:
                m10 = m20;
                m11 = m21;
//                m12 = m22;
            case 2:
//                m20 = 0.0;
//                m21 = 0.0;
//                m22 = 1.0;
                break;
            case 3:
                // This is the only row that requires a full 3x3 determinant
                return (m00 * (m11 * m22 - m21 * m12) +
                        m01 * (m12 * m20 - m22 * m10) +
                        m02 * (m10 * m21 - m20 * m11));
        }
//        return (m00 * (m11 * 1.0 - 0.0 * m12) +
//                m01 * (m12 * 0.0 - 1.0 * m10) +
//                m02 * (m10 * 0.0 - 0.0 * m11));
        return (m00 * m11 - m01 * m10);
    }

    @Override
    public Affine3D deriveWithNewTransform(BaseTransform tx) {
        setTransform(tx);
        return this;
    }

    @Override
    public Affine3D deriveWithTranslation(double tx, double ty) {
        translate(tx, ty, 0.0);
        return this;
    }

    @Override
    public void translate(double tx, double ty) {
        if ((state & APPLY_3D) == 0) {
            super.translate(tx, ty);
        } else {
            translate(tx, ty, 0.0);
        }
    }

    public void translate(double tx, double ty, double tz) {
        if ((state & APPLY_3D) == 0) {
            super.translate(tx, ty);
            if (tz != 0.0) {
                this.mzt = tz;
                state |= APPLY_3D;
                if (type != TYPE_UNKNOWN) {
                    type |= TYPE_AFFINE_3D;
                }
            }
            return;
        }
        this.mxt = tx * mxx + ty * mxy + tz * mxz + mxt;
        this.myt = tx * myx + ty * myy + tz * myz + myt;
        this.mzt = tx * mzx + ty * mzy + tz * mzz + mzt;
        updateState();
    }

    @Override
    public Affine3D deriveWithPreTranslation(double mxt, double myt) {
        preTranslate(mxt, myt, 0.0);
        return this;
    }

    @Override
    public BaseTransform deriveWithTranslation(double mxt, double myt, double mzt) {
        translate(mxt, myt, mzt);
        return this;
    }

    @Override
    public BaseTransform deriveWithScale(double mxx, double myy, double mzz) {
        scale(mxx, myy, mzz);
        return this;
    }

    @Override
    public BaseTransform deriveWithRotation(double theta,
                                            double axisX, double axisY, double axisZ) {
        rotate(theta, axisX, axisY, axisZ);
        return this;
    }

    public void preTranslate(double mxt, double myt, double mzt) {
        this.mxt += mxt;
        this.myt += myt;
        this.mzt += mzt;
        int clearflags = 0;
        int setflags = 0;
        if (this.mzt == 0.0) {
            if ((state & APPLY_3D) != 0) {
                // Might have become non-3D...
                updateState();
                return;
            }
        } else {
            state |= APPLY_3D;
            setflags = TYPE_AFFINE_3D;
        }
        if (this.mxt == 0.0 && this.myt == 0.0) {
            state &= ~APPLY_TRANSLATE;
            clearflags = TYPE_TRANSLATION;
        } else {
            state |= APPLY_TRANSLATE;
            setflags |= TYPE_TRANSLATION;
        }
        if (type != TYPE_UNKNOWN) {
            type = ((type & ~clearflags) | setflags);
        }
    }

    @Override
    public void scale(double sx, double sy) {
        if ((state & APPLY_3D) == 0) {
            super.scale(sx, sy);
        } else {
            scale(sx, sy, 1.0);
        }
    }

    public void scale(double sx, double sy, double sz) {
        if ((state & APPLY_3D) == 0) {
            super.scale(sx, sy);
            if (sz != 1.0) {
                this.mzz = sz;
                state |= APPLY_3D;
                if (type != TYPE_UNKNOWN) {
                    type |= TYPE_AFFINE_3D;
                }
            }
            return;
        }
        this.mxx *= sx;
        this.mxy *= sy;
        this.mxz *= sz;

        this.myx *= sx;
        this.myy *= sy;
        this.myz *= sz;

        this.mzx *= sx;
        this.mzy *= sy;
        this.mzz *= sz;

        // TODO: Optimize the state... (RT-26800)
        updateState();
    }

    @Override
    public void rotate(double theta) {
        if ((state & APPLY_3D) == 0) {
            super.rotate(theta);
        } else {
            rotate(theta, 0, 0, 1);
        }
    }

    public void rotate(double theta, double axisX, double axisY, double axisZ) {
        if ((state & APPLY_3D) == 0 && almostZero(axisX) && almostZero(axisY)) {
            if (axisZ > 0) {
                super.rotate(theta);
            } else if (axisZ < 0) {
                super.rotate(-theta);
            } // else rotating about zero vector - NOP
            return;
        }
        double mag = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

        if (almostZero(mag)) {
            return;
        }
        mag = 1.0 / mag;
        double ax = axisX * mag;
        double ay = axisY * mag;
        double az = axisZ * mag;

        double sinTheta = Math.sin(theta);
        double cosTheta = Math.cos(theta);
        double t = 1.0 - cosTheta;

        double xz = ax * az;
        double xy = ax * ay;
        double yz = ay * az;

        double Txx = t * ax * ax + cosTheta;
        double Txy = t * xy - sinTheta * az;
        double Txz = t * xz + sinTheta * ay;

        double Tyx = t * xy + sinTheta * az;
        double Tyy = t * ay * ay + cosTheta;
        double Tyz = t * yz - sinTheta * ax;

        double Tzx = t * xz - sinTheta * ay;
        double Tzy = t * yz + sinTheta * ax;
        double Tzz = t * az * az + cosTheta;

        double rxx = (mxx * Txx + mxy * Tyx + mxz * Tzx /* + mxt * 0.0 */);
        double rxy = (mxx * Txy + mxy * Tyy + mxz * Tzy /* + mxt * 0.0 */);
        double rxz = (mxx * Txz + mxy * Tyz + mxz * Tzz /* + mxt * 0.0 */);
        double ryx = (myx * Txx + myy * Tyx + myz * Tzx /* + myt * 0.0 */);
        double ryy = (myx * Txy + myy * Tyy + myz * Tzy /* + myt * 0.0 */);
        double ryz = (myx * Txz + myy * Tyz + myz * Tzz /* + myt * 0.0 */);
        double rzx = (mzx * Txx + mzy * Tyx + mzz * Tzx /* + mzt * 0.0 */);
        double rzy = (mzx * Txy + mzy * Tyy + mzz * Tzy /* + mzt * 0.0 */);
        double rzz = (mzx * Txz + mzy * Tyz + mzz * Tzz /* + mzt * 0.0 */);
        this.mxx = rxx;
        this.mxy = rxy;
        this.mxz = rxz;
        this.myx = ryx;
        this.myy = ryy;
        this.myz = ryz;
        this.mzx = rzx;
        this.mzy = rzy;
        this.mzz = rzz;
        updateState();
    }

    @Override
    public void shear(double shx, double shy) {
        if ((state & APPLY_3D) == 0) {
            super.shear(shx, shy);
            return;
        }
        double rxx = (mxx + mxy * shy);
        double rxy = (mxy + mxx * shx);
        double ryx = (myx + myy * shy);
        double ryy = (myy + myx * shx);
        double rzx = (mzx + mzy * shy);
        double rzy = (mzy + mzx * shx);
        this.mxx = rxx;
        this.mxy = rxy;
        this.myx = ryx;
        this.myy = ryy;
        this.mzx = rzx;
        this.mzy = rzy;
        updateState();
    }

    @Override
    public Affine3D deriveWithConcatenation(BaseTransform transform) {
        concatenate(transform);
        return this;
    }

    @Override
    public Affine3D deriveWithPreConcatenation(BaseTransform transform) {
        preConcatenate(transform);
        return this;
    }

    @Override
    public void concatenate(BaseTransform transform) {
        switch (transform.getDegree()) {
            case IDENTITY:
                return;
            case TRANSLATE_2D:
                translate(transform.getMxt(), transform.getMyt());
                return;
            case TRANSLATE_3D:
                translate(transform.getMxt(), transform.getMyt(), transform.getMzt());
                return;
            case AFFINE_3D:
                if (!transform.is2D()) {
                    break;
                }
                /* No Break */
            case AFFINE_2D:
                if ((state & APPLY_3D) == 0) {
                    super.concatenate(transform);
                    return;
                }
                break;
        }
        double Txx = transform.getMxx();
        double Txy = transform.getMxy();
        double Txz = transform.getMxz();
        double Txt = transform.getMxt();
        double Tyx = transform.getMyx();
        double Tyy = transform.getMyy();
        double Tyz = transform.getMyz();
        double Tyt = transform.getMyt();
        double Tzx = transform.getMzx();
        double Tzy = transform.getMzy();
        double Tzz = transform.getMzz();
        double Tzt = transform.getMzt();
        double rxx = (mxx * Txx + mxy * Tyx + mxz * Tzx /* + mxt * 0.0 */);
        double rxy = (mxx * Txy + mxy * Tyy + mxz * Tzy /* + mxt * 0.0 */);
        double rxz = (mxx * Txz + mxy * Tyz + mxz * Tzz /* + mxt * 0.0 */);
        double rxt = (mxx * Txt + mxy * Tyt + mxz * Tzt + mxt /* * 1.0 */);
        double ryx = (myx * Txx + myy * Tyx + myz * Tzx /* + myt * 0.0 */);
        double ryy = (myx * Txy + myy * Tyy + myz * Tzy /* + myt * 0.0 */);
        double ryz = (myx * Txz + myy * Tyz + myz * Tzz /* + myt * 0.0 */);
        double ryt = (myx * Txt + myy * Tyt + myz * Tzt + myt /* * 1.0 */);
        double rzx = (mzx * Txx + mzy * Tyx + mzz * Tzx /* + mzt * 0.0 */);
        double rzy = (mzx * Txy + mzy * Tyy + mzz * Tzy /* + mzt * 0.0 */);
        double rzz = (mzx * Txz + mzy * Tyz + mzz * Tzz /* + mzt * 0.0 */);
        double rzt = (mzx * Txt + mzy * Tyt + mzz * Tzt + mzt /* * 1.0 */);
        this.mxx = rxx;
        this.mxy = rxy;
        this.mxz = rxz;
        this.mxt = rxt;
        this.myx = ryx;
        this.myy = ryy;
        this.myz = ryz;
        this.myt = ryt;
        this.mzx = rzx;
        this.mzy = rzy;
        this.mzz = rzz;
        this.mzt = rzt;
        updateState();
    }

    public void concatenate(double Txx, double Txy, double Txz, double Txt,
                            double Tyx, double Tyy, double Tyz, double Tyt,
                            double Tzx, double Tzy, double Tzz, double Tzt) {
        double rxx = (mxx * Txx + mxy * Tyx + mxz * Tzx /* + mxt * 0.0 */);
        double rxy = (mxx * Txy + mxy * Tyy + mxz * Tzy /* + mxt * 0.0 */);
        double rxz = (mxx * Txz + mxy * Tyz + mxz * Tzz /* + mxt * 0.0 */);
        double rxt = (mxx * Txt + mxy * Tyt + mxz * Tzt + mxt /* * 1.0 */);
        double ryx = (myx * Txx + myy * Tyx + myz * Tzx /* + myt * 0.0 */);
        double ryy = (myx * Txy + myy * Tyy + myz * Tzy /* + myt * 0.0 */);
        double ryz = (myx * Txz + myy * Tyz + myz * Tzz /* + myt * 0.0 */);
        double ryt = (myx * Txt + myy * Tyt + myz * Tzt + myt /* * 1.0 */);
        double rzx = (mzx * Txx + mzy * Tyx + mzz * Tzx /* + mzt * 0.0 */);
        double rzy = (mzx * Txy + mzy * Tyy + mzz * Tzy /* + mzt * 0.0 */);
        double rzz = (mzx * Txz + mzy * Tyz + mzz * Tzz /* + mzt * 0.0 */);
        double rzt = (mzx * Txt + mzy * Tyt + mzz * Tzt + mzt /* * 1.0 */);
        this.mxx = rxx;
        this.mxy = rxy;
        this.mxz = rxz;
        this.mxt = rxt;
        this.myx = ryx;
        this.myy = ryy;
        this.myz = ryz;
        this.myt = ryt;
        this.mzx = rzx;
        this.mzy = rzy;
        this.mzz = rzz;
        this.mzt = rzt;
        updateState();
    }

    @Override
    public Affine3D deriveWithConcatenation(double Txx, double Tyx,
                                            double Txy, double Tyy,
                                            double Txt, double Tyt) {
        double rxx = (mxx * Txx + mxy * Tyx /* + mxz * 0.0 + mxt * 0.0 */);
        double rxy = (mxx * Txy + mxy * Tyy /* + mxz * 0.0 + mxt * 0.0 */);
//        double rxz = (mxz /* * 1.0 + mxx * 0.0 + mxy * 0.0 + mxt * 0.0 */);
        double rxt = (mxx * Txt + mxy * Tyt + mxt /* + mxz * 0.0 * 1.0 */);
        double ryx = (myx * Txx + myy * Tyx /* + myz * 0.0 + myt * 0.0 */);
        double ryy = (myx * Txy + myy * Tyy /* + myz * 0.0 + myt * 0.0 */);
//        double ryz = (myz /* * 1.0 + myx * 0.0 + myy * 0.0 + myt * 0.0 */);
        double ryt = (myx * Txt + myy * Tyt + myt /* * 1.0 + myz * 0.0 */);
        double rzx = (mzx * Txx + mzy * Tyx /* + mzz * 0.0 + mzt * 0.0 */);
        double rzy = (mzx * Txy + mzy * Tyy /* + mzz * 0.0 + mzt * 0.0 */);
//        double rzz = (mzz /* * 1.0 + mzx * 0.0 + mzy * 0.0 + mzt * 0.0 */);
        double rzt = (mzx * Txt + mzy * Tyt + mzt /* * 1.0 + mzz * 0.0 */);
        this.mxx = rxx;
        this.mxy = rxy;
//        this.mxz = rxz; // == mxz anyway
        this.mxt = rxt;
        this.myx = ryx;
        this.myy = ryy;
//        this.myz = ryz; // == myz anyway
        this.myt = ryt;
        this.mzx = rzx;
        this.mzy = rzy;
//        this.mzz = rzz; // == mzz anyway
        this.mzt = rzt;
        updateState();
        return this;
    }

    @Override
    public BaseTransform deriveWithConcatenation(
            double mxx, double mxy, double mxz, double mxt,
            double myx, double myy, double myz, double myt,
            double mzx, double mzy, double mzz, double mzt) {
        concatenate(mxx, mxy, mxz, mxt,
                myx, myy, myz, myt,
                mzx, mzy, mzz, mzt);
        return this;
    }

    public void preConcatenate(BaseTransform transform) {
        switch (transform.getDegree()) {
            case IDENTITY:
                return;
            case TRANSLATE_2D:
                preTranslate(transform.getMxt(), transform.getMyt(), 0.0);
                return;
            case TRANSLATE_3D:
                preTranslate(transform.getMxt(), transform.getMyt(), transform.getMzt());
                return;
        }
        double Txx = transform.getMxx();
        double Txy = transform.getMxy();
        double Txz = transform.getMxz();
        double Txt = transform.getMxt();
        double Tyx = transform.getMyx();
        double Tyy = transform.getMyy();
        double Tyz = transform.getMyz();
        double Tyt = transform.getMyt();
        double Tzx = transform.getMzx();
        double Tzy = transform.getMzy();
        double Tzz = transform.getMzz();
        double Tzt = transform.getMzt();
        double rxx = (Txx * mxx + Txy * myx + Txz * mzx /* + Txt * 0.0 */);
        double rxy = (Txx * mxy + Txy * myy + Txz * mzy /* + Txt * 0.0 */);
        double rxz = (Txx * mxz + Txy * myz + Txz * mzz /* + Txt * 0.0 */);
        double rxt = (Txx * mxt + Txy * myt + Txz * mzt + Txt /* * 1.0 */);
        double ryx = (Tyx * mxx + Tyy * myx + Tyz * mzx /* + Tyt * 0.0 */);
        double ryy = (Tyx * mxy + Tyy * myy + Tyz * mzy /* + Tyt * 0.0 */);
        double ryz = (Tyx * mxz + Tyy * myz + Tyz * mzz /* + Tyt * 0.0 */);
        double ryt = (Tyx * mxt + Tyy * myt + Tyz * mzt + Tyt /* * 1.0 */);
        double rzx = (Tzx * mxx + Tzy * myx + Tzz * mzx /* + Tzt * 0.0 */);
        double rzy = (Tzx * mxy + Tzy * myy + Tzz * mzy /* + Tzt * 0.0 */);
        double rzz = (Tzx * mxz + Tzy * myz + Tzz * mzz /* + Tzt * 0.0 */);
        double rzt = (Tzx * mxt + Tzy * myt + Tzz * mzt + Tzt /* * 1.0 */);
        this.mxx = rxx;
        this.mxy = rxy;
        this.mxz = rxz;
        this.mxt = rxt;
        this.myx = ryx;
        this.myy = ryy;
        this.myz = ryz;
        this.myt = ryt;
        this.mzx = rzx;
        this.mzy = rzy;
        this.mzz = rzz;
        this.mzt = rzt;
        updateState();
    }

    @Override
    public void restoreTransform(double mxx, double myx,
                                 double mxy, double myy,
                                 double mxt, double myt) {
        throw new InternalError("must use Affine3D restore method " +
                "to prevent loss of information");
    }

    @Override
    public void restoreTransform(double mxx, double mxy, double mxz, double mxt,
                                 double myx, double myy, double myz, double myt,
                                 double mzx, double mzy, double mzz, double mzt) {
        this.mxx = mxx;
        this.mxy = mxy;
        this.mxz = mxz;
        this.mxt = mxt;

        this.myx = myx;
        this.myy = myy;
        this.myz = myz;
        this.myt = myt;

        this.mzx = mzx;
        this.mzy = mzy;
        this.mzz = mzz;
        this.mzt = mzt;

        updateState();
    }

    /**
     * Sets this transform to a viewing transform computed from the specified
     * eye point, center point, and up vector.
     * The resulting transform can be used as the view transform
     * in a 3D camera.
     *
     * @param eye    the eye point
     * @param center the center point
     * @param up     the up vector
     * @return this transform
     */
    public Affine3D lookAt(Vec3d eye, Vec3d center, Vec3d up) {
        double forwardx, forwardy, forwardz, invMag;
        double upx, upy, upz;
        double sidex, sidey, sidez;

        forwardx = eye.x - center.x;
        forwardy = eye.y - center.y;
        forwardz = eye.z - center.z;

        invMag = 1.0 / Math.sqrt(forwardx * forwardx + forwardy * forwardy + forwardz * forwardz);
        forwardx = forwardx * invMag;
        forwardy = forwardy * invMag;
        forwardz = forwardz * invMag;

        invMag = 1.0 / Math.sqrt(up.x * up.x + up.y * up.y + up.z * up.z);
        upx = up.x * invMag;
        upy = up.y * invMag;
        upz = up.z * invMag;

        // side = Up cross forward
        sidex = upy * forwardz - forwardy * upz;
        sidey = upz * forwardx - upx * forwardz;
        sidez = upx * forwardy - upy * forwardx;

        invMag = 1.0 / Math.sqrt(sidex * sidex + sidey * sidey + sidez * sidez);
        sidex *= invMag;
        sidey *= invMag;
        sidez *= invMag;

        // recompute up = forward cross side
        upx = forwardy * sidez - sidey * forwardz;
        upy = forwardz * sidex - forwardx * sidez;
        upz = forwardx * sidey - forwardy * sidex;

        // transpose because we calculated the inverse of what we want
        mxx = sidex;
        mxy = sidey;
        mxz = sidez;

        myx = upx;
        myy = upy;
        myz = upz;

        mzx = forwardx;
        mzy = forwardy;
        mzz = forwardz;

        mxt = -eye.x * mxx + -eye.y * mxy + -eye.z * mxz;
        myt = -eye.x * myx + -eye.y * myy + -eye.z * myz;
        mzt = -eye.x * mzx + -eye.y * mzy + -eye.z * mzz;

        updateState();
        return this;
    }

    static boolean almostOne(double a) {
        return ((a < 1 + EPSILON_ABSOLUTE) && (a > 1 - EPSILON_ABSOLUTE));
    }

    // Round values to sane precision for printing
    // Note that Math.sin(Math.PI) has an error of about 10^-16
    private static double _matround(double matval) {
        return Math.rint(matval * 1E15) / 1E15;
    }

    /**
     * Returns a <code>String</code> that represents the value of this
     * {@link Object}.
     *
     * @return a <code>String</code> representing the value of this
     * <code>Object</code>.
     */
    @Override
    public String toString() {
        return ("Affine3D[["
                + _matround(mxx) + ", "
                + _matround(mxy) + ", "
                + _matround(mxz) + ", "
                + _matround(mxt) + "], ["
                + _matround(myx) + ", "
                + _matround(myy) + ", "
                + _matround(myz) + ", "
                + _matround(myt) + "], ["
                + _matround(mzx) + ", "
                + _matround(mzy) + ", "
                + _matround(mzz) + ", "
                + _matround(mzt) + "]]");
    }
}
