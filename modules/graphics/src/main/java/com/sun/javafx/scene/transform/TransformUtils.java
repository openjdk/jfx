/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.transform;

import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Affine;

/**
 * Internal utilities for transformations
 */
public class TransformUtils {

    /**
     * Creates an immutable arbitrary transformation.
     * This method is not intended for public use, users should use the Affine
     * class.
     */
    public static Transform immutableTransform(
                double mxx, double mxy, double mxz, double tx,
                double myx, double myy, double myz, double ty,
                double mzx, double mzy, double mzz, double tz) {
        return new ImmutableTransform(
                mxx, mxy, mxz, tx,
                myx, myy, myz, ty,
                mzx, mzy, mzz, tz);
    }

    /**
     * Creates an immutable transformation filled with current values
     * from the given transformation.
     * This method is not intended for public use, users should use the Affine
     * class.
     */
    public static Transform immutableTransform(Transform t) {
        return new ImmutableTransform(
                t.getMxx(), t.getMxy(), t.getMxz(), t.getTx(),
                t.getMyx(), t.getMyy(), t.getMyz(), t.getTy(),
                t.getMzx(), t.getMzy(), t.getMzz(), t.getTz());
    }

    /**
     * Creates an immutable arbitrary transformation.
     * If the given instance is not null, it is reused.
     * This method is not intended for public use, users should use the Affine
     * class.
     * @throws ClassCastException if the given transform to be reused
     *                            is not instance of ImmutableTransform
     */
    public static Transform immutableTransform(Transform reuse,
                double mxx, double mxy, double mxz, double tx,
                double myx, double myy, double myz, double ty,
                double mzx, double mzy, double mzz, double tz) {

        if (reuse == null) {
            return new ImmutableTransform(
                mxx, mxy, mxz, tx,
                myx, myy, myz, ty,
                mzx, mzy, mzz, tz);
        }

        ((ImmutableTransform) reuse).setToTransform(
                mxx, mxy, mxz, tx,
                myx, myy, myz, ty,
                mzx, mzy, mzz, tz);
        return reuse;
    }

    /**
     * Creates an immutable transformation filled with current values
     * from the given transformation.
     * If the given instance is not null, it is reused.
     * This method is not intended for public use, users should use the Affine
     * class.
     * @throws ClassCastException if the given transform to be reused
     *                            is not instance of ImmutableTransform
     */
    public static Transform immutableTransform(Transform reuse,
                Transform t) {
        return immutableTransform((ImmutableTransform) reuse,
                t.getMxx(), t.getMxy(), t.getMxz(), t.getTx(),
                t.getMyx(), t.getMyy(), t.getMyz(), t.getTy(),
                t.getMzx(), t.getMzy(), t.getMzz(), t.getTz());
    }

    /**
     * Creates an immutable transformation filled with concatenation
     * of the given transformations.
     * If the given instance is not null, it is reused.
     * This method is not intended for public use, users should use the Affine
     * class.
     * @throws ClassCastException if one of the given transforms
     *                            is not instance of ImmutableTransform
     */
    public static Transform immutableTransform(Transform reuse,
            Transform left, Transform right) {

        if (reuse == null) {
            reuse = new ImmutableTransform();
        }

        ((ImmutableTransform) reuse).setToConcatenation(
                (ImmutableTransform) left, ((ImmutableTransform) right));

        return reuse;
    }

    /**
     * Immutable transformation with performance optimizations based on Affine.
     *
     * From user's perspective, this transform is immutable. However, we can
     * modify it internally. This allows for reusing instances that were
     * not handed to users. The caller is responsible for not modifying
     * user-visible instances.
     *
     * Note: can't override Transform's package private methods so they cannot
     * be optimized. Currently not a big deal.
     */
    static class ImmutableTransform extends Transform {

        private static final int APPLY_IDENTITY = 0;
        private static final int APPLY_TRANSLATE = 1;
        private static final int APPLY_SCALE = 2;
        private static final int APPLY_SHEAR = 4;
        private static final int APPLY_NON_3D = 0;
        private static final int APPLY_3D_COMPLEX = 4;
        private transient int state2d;
        private transient int state3d;

        private double xx;
        private double xy;
        private double xz;
        private double yx;
        private double yy;
        private double yz;
        private double zx;
        private double zy;
        private double zz;
        private double xt;
        private double yt;
        private double zt;

        public ImmutableTransform() {
            xx = yy = zz = 1.0;
        }

        public ImmutableTransform(Transform transform) {
            this(transform.getMxx(), transform.getMxy(), transform.getMxz(),
                                                                 transform.getTx(),
                 transform.getMyx(), transform.getMyy(), transform.getMyz(),
                                                                 transform.getTy(),
                 transform.getMzx(), transform.getMzy(), transform.getMzz(),
                                                                 transform.getTz());
        }

        public ImmutableTransform(double mxx, double mxy, double mxz, double tx,
                      double myx, double myy, double myz, double ty,
                      double mzx, double mzy, double mzz, double tz) {
            xx = mxx;
            xy = mxy;
            xz = mxz;
            xt = tx;

            yx = myx;
            yy = myy;
            yz = myz;
            yt = ty;

            zx = mzx;
            zy = mzy;
            zz = mzz;
            zt = tz;

            updateState();
        }

        // Beware: this is modifying immutable transform!
        // It is private and it is there just for the purpose of reusing
        // instances not given to users
        private void setToTransform(double mxx, double mxy, double mxz, double tx,
                                    double myx, double myy, double myz, double ty,
                                    double mzx, double mzy, double mzz, double tz)
        {
            xx = mxx;
            xy = mxy;
            xz = mxz;
            xt = tx;
            yx = myx;
            yy = myy;
            yz = myz;
            yt = ty;
            zx = mzx;
            zy = mzy;
            zz = mzz;
            zt = tz;
            updateState();
        }

        // Beware: this is modifying immutable transform!
        // It is private and it is there just for the purpose of reusing
        // instances not given to users
        private void setToConcatenation(ImmutableTransform left, ImmutableTransform right) {
            if (left.state3d == APPLY_NON_3D && right.state3d == APPLY_NON_3D) {
                xx = left.xx * right.xx + left.xy * right.yx;
                xy = left.xx * right.xy + left.xy * right.yy;
                xt = left.xx * right.xt + left.xy * right.yt + left.xt;
                yx = left.yx * right.xx + left.yy * right.yx;
                yy = left.yx * right.xy + left.yy * right.yy;
                yt = left.yx * right.xt + left.yy * right.yt + left.yt;
                if (state3d != APPLY_NON_3D) {
                    xz = yz = zx = zy = zt = 0.0;
                    zz = 1.0;
                    state3d = APPLY_NON_3D;
                }
                updateState2D();
            } else {
                xx = left.xx * right.xx + left.xy * right.yx + left.xz * right.zx;
                xy = left.xx * right.xy + left.xy * right.yy + left.xz * right.zy;
                xz = left.xx * right.xz + left.xy * right.yz + left.xz * right.zz;
                xt = left.xx * right.xt + left.xy * right.yt + left.xz * right.zt + left.xt;
                yx = left.yx * right.xx + left.yy * right.yx + left.yz * right.zx;
                yy = left.yx * right.xy + left.yy * right.yy + left.yz * right.zy;
                yz = left.yx * right.xz + left.yy * right.yz + left.yz * right.zz;
                yt = left.yx * right.xt + left.yy * right.yt + left.yz * right.zt + left.yt;
                zx = left.zx * right.xx + left.zy * right.yx + left.zz * right.zx;
                zy = left.zx * right.xy + left.zy * right.yy + left.zz * right.zy;
                zz = left.zx * right.xz + left.zy * right.yz + left.zz * right.zz;
                zt = left.zx * right.xt + left.zy * right.yt + left.zz * right.zt + left.zt;
                updateState();
            }
            // could be further optimized using the states, but that would
            // require a lot of code (see Affine and all its append* methods)
        }

        @Override
        public double getMxx() {
            return xx;
        }

        @Override
        public double getMxy() {
            return xy;
        }

        @Override
        public double getMxz() {
            return xz;
        }

        @Override
        public double getTx() {
            return xt;
        }

        @Override
        public double getMyx() {
            return yx;
        }

        @Override
        public double getMyy() {
            return yy;
        }

        @Override
        public double getMyz() {
            return yz;
        }

        @Override
        public double getTy() {
            return yt;
        }

        @Override
        public double getMzx() {
            return zx;
        }

        @Override
        public double getMzy() {
            return zy;
        }

        @Override
        public double getMzz() {
            return zz;
        }

        @Override
        public double getTz() {
            return zt;
        }

    /* *************************************************************************
     *                                                                         *
     *                           State getters                                 *
     *                                                                         *
     **************************************************************************/

        @Override
        public double determinant() {
            switch(state3d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_NON_3D:
                    switch (state2d) {
                        default:
                            stateError();
                            // cannot reach
                        case APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE:
                        case APPLY_SHEAR | APPLY_SCALE:
                            return xx * yy - xy * yx;
                        case APPLY_SHEAR | APPLY_TRANSLATE:
                        case APPLY_SHEAR:
                            return -(xy* yx);
                        case APPLY_SCALE | APPLY_TRANSLATE:
                        case APPLY_SCALE:
                            return xx * yy;
                        case APPLY_TRANSLATE:
                        case APPLY_IDENTITY:
                            return 1.0;
                    }
                case APPLY_TRANSLATE:
                    return 1.0;
                case APPLY_SCALE:
                case APPLY_SCALE | APPLY_TRANSLATE:
                    return xx * yy * zz;
                case APPLY_3D_COMPLEX:
                    return (xx* (yy * zz - zy * yz) +
                            xy* (yz * zx - zz * yx) +
                            xz* (yx * zy - zx * yy));
            }
        }

        @Override
        public Transform createConcatenation(Transform transform) {
            javafx.scene.transform.Affine a = new Affine(this);
            a.append(transform);
            return a;
        }

        @Override
        public javafx.scene.transform.Affine createInverse() throws NonInvertibleTransformException {
            javafx.scene.transform.Affine t = new Affine(this);
            t.invert();
            return t;
        }

        @Override
        public Transform clone() {
            return new ImmutableTransform(this);
        }

        /* *************************************************************************
         *                                                                         *
         *                     Transform, Inverse Transform                        *
         *                                                                         *
         **************************************************************************/

        @Override
        public Point2D transform(double x, double y) {
            ensureCanTransform2DPoint();

            switch (state2d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE:
                    return new Point2D(
                        xx * x + xy * y + xt,
                        yx * x + yy * y + yt);
                case APPLY_SHEAR | APPLY_SCALE:
                    return new Point2D(
                        xx * x + xy * y,
                        yx * x + yy * y);
                case APPLY_SHEAR | APPLY_TRANSLATE:
                    return new Point2D(
                            xy * y + xt,
                            yx * x + yt);
                case APPLY_SHEAR:
                    return new Point2D(xy * y, yx * x);
                case APPLY_SCALE | APPLY_TRANSLATE:
                    return new Point2D(
                            xx * x + xt,
                            yy * y + yt);
                case APPLY_SCALE:
                    return new Point2D(xx * x, yy * y);
                case APPLY_TRANSLATE:
                    return new Point2D(x + xt, y + yt);
                case APPLY_IDENTITY:
                    return new Point2D(x, y);
            }
        }

        @Override
        public Point3D transform(double x, double y, double z) {
            switch (state3d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_NON_3D:
                    switch (state2d) {
                        default:
                            stateError();
                            // cannot reach
                        case APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE:
                            return new Point3D(
                                xx * x + xy * y + xt,
                                yx * x + yy * y + yt, z);
                        case APPLY_SHEAR | APPLY_SCALE:
                            return new Point3D(
                                xx * x + xy * y,
                                yx * x + yy * y, z);
                        case APPLY_SHEAR | APPLY_TRANSLATE:
                            return new Point3D(
                                    xy * y + xt, yx * x + yt,
                                    z);
                        case APPLY_SHEAR:
                            return new Point3D(xy * y, yx * x, z);
                        case APPLY_SCALE | APPLY_TRANSLATE:
                            return new Point3D(
                                    xx * x + xt, yy * y + yt,
                                    z);
                        case APPLY_SCALE:
                            return new Point3D(xx * x, yy * y, z);
                        case APPLY_TRANSLATE:
                            return new Point3D(x + xt, y + yt, z);
                        case APPLY_IDENTITY:
                            return new Point3D(x, y, z);
                    }
                case APPLY_TRANSLATE:
                    return new Point3D(x + xt, y + yt, z + zt);
                case APPLY_SCALE:
                    return new Point3D(xx * x, yy * y, zz * z);
                case APPLY_SCALE | APPLY_TRANSLATE:
                    return new Point3D(
                            xx * x + xt,
                            yy * y + yt,
                            zz * z + zt);
                case APPLY_3D_COMPLEX:
                    return new Point3D(
                        xx * x + xy * y + xz * z + xt,
                        yx * x + yy * y + yz * z + yt,
                        zx * x + zy * y + zz * z + zt);
            }
        }

        @Override
        public Point2D deltaTransform(double x, double y) {
            ensureCanTransform2DPoint();

            switch (state2d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE:
                case APPLY_SHEAR | APPLY_SCALE:
                    return new Point2D(
                        xx * x + xy * y,
                        yx * x + yy * y);
                case APPLY_SHEAR | APPLY_TRANSLATE:
                case APPLY_SHEAR:
                    return new Point2D(xy * y, yx * x);
                case APPLY_SCALE | APPLY_TRANSLATE:
                case APPLY_SCALE:
                    return new Point2D(xx * x, yy * y);
                case APPLY_TRANSLATE:
                case APPLY_IDENTITY:
                    return new Point2D(x, y);
            }
        }

        @Override
        public Point3D deltaTransform(double x, double y, double z) {
            switch (state3d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_NON_3D:
                    switch (state2d) {
                        default:
                            stateError();
                            // cannot reach
                        case APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE:
                        case APPLY_SHEAR | APPLY_SCALE:
                            return new Point3D(
                                xx * x + xy * y,
                                yx * x + yy * y, z);
                        case APPLY_SHEAR | APPLY_TRANSLATE:
                        case APPLY_SHEAR:
                            return new Point3D(xy * y, yx * x, z);
                        case APPLY_SCALE | APPLY_TRANSLATE:
                        case APPLY_SCALE:
                            return new Point3D(xx * x, yy * y, z);
                        case APPLY_TRANSLATE:
                        case APPLY_IDENTITY:
                            return new Point3D(x, y, z);
                    }
                case APPLY_TRANSLATE:
                    return new Point3D(x, y, z);
                case APPLY_SCALE:
                case APPLY_SCALE | APPLY_TRANSLATE:
                    return new Point3D(xx * x, yy * y, zz * z);
                case APPLY_3D_COMPLEX:
                    return new Point3D(
                        xx * x + xy * y + xz * z,
                        yx * x + yy * y + yz * z,
                        zx * x + zy * y + zz * z);
            }
        }

        @Override
        public Point2D inverseTransform(double x, double y)
                throws NonInvertibleTransformException {
            ensureCanTransform2DPoint();

            switch (state2d) {
                default:
                    return super.inverseTransform(x, y);
                case APPLY_SHEAR | APPLY_TRANSLATE:
                    if (xy == 0.0 || yx == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D(
                            (1.0 / yx) * y - yt / yx,
                            (1.0 / xy) * x - xt / xy);
                case APPLY_SHEAR:
                    if (xy == 0.0 || yx == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D((1.0 / yx) * y, (1.0 / xy) * x);
                case APPLY_SCALE | APPLY_TRANSLATE:
                    if (xx == 0.0 || yy == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D(
                            (1.0 / xx) * x - xt / xx,
                            (1.0 / yy) * y - yt / yy);
                case APPLY_SCALE:
                    if (xx == 0.0 || yy == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D((1.0 / xx) * x, (1.0 / yy) * y);
                case APPLY_TRANSLATE:
                    return new Point2D(x - xt, y - yt);
                case APPLY_IDENTITY:
                    return new Point2D(x, y);
            }
        }

        @Override
        public Point3D inverseTransform(double x, double y, double z)
                throws NonInvertibleTransformException {
            switch(state3d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_NON_3D:
                    switch (state2d) {
                        default:
                            return super.inverseTransform(x, y, z);
                        case APPLY_SHEAR | APPLY_TRANSLATE:
                            if (xy == 0.0 || yx == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D(
                                    (1.0 / yx) * y - yt / yx,
                                    (1.0 / xy) * x - xt / xy, z);
                        case APPLY_SHEAR:
                            if (xy == 0.0 || yx == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D(
                                    (1.0 / yx) * y,
                                    (1.0 / xy) * x, z);
                        case APPLY_SCALE | APPLY_TRANSLATE:
                            if (xx == 0.0 || yy == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D(
                                    (1.0 / xx) * x - xt / xx,
                                    (1.0 / yy) * y - yt / yy, z);
                        case APPLY_SCALE:
                            if (xx == 0.0 || yy == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D((1.0 / xx) * x, (1.0 / yy) * y, z);
                        case APPLY_TRANSLATE:
                            return new Point3D(x - xt, y - yt, z);
                        case APPLY_IDENTITY:
                            return new Point3D(x, y, z);
                    }
                case APPLY_TRANSLATE:
                    return new Point3D(x - xt, y - yt, z - zt);
                case APPLY_SCALE:
                    if (xx == 0.0 || yy == 0.0 || zz == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point3D(
                            (1.0 / xx) * x,
                            (1.0 / yy) * y,
                            (1.0 / zz) * z);
                case APPLY_SCALE | APPLY_TRANSLATE:
                    if (xx == 0.0 || yy == 0.0 || zz == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point3D(
                            (1.0 / xx) * x - xt / xx,
                            (1.0 / yy) * y - yt / yy,
                            (1.0 / zz) * z - zt / zz);
                case APPLY_3D_COMPLEX:
                    return super.inverseTransform(x, y, z);
            }
        }

        @Override
        public Point2D inverseDeltaTransform(double x, double y)
                throws NonInvertibleTransformException {
            ensureCanTransform2DPoint();

            switch (state2d) {
                default:
                    return super.inverseDeltaTransform(x, y);
                case APPLY_SHEAR | APPLY_TRANSLATE:
                case APPLY_SHEAR:
                    if (xy == 0.0 || yx == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D((1.0 / yx) * y, (1.0 / xy) * x);
                case APPLY_SCALE | APPLY_TRANSLATE:
                case APPLY_SCALE:
                    if (xx == 0.0 || yy == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D((1.0 / xx) * x, (1.0 / yy) * y);
                case APPLY_TRANSLATE:
                case APPLY_IDENTITY:
                    return new Point2D(x, y);
            }
        }

        @Override
        public Point3D inverseDeltaTransform(double x, double y, double z)
                throws NonInvertibleTransformException {
            switch(state3d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_NON_3D:
                    switch (state2d) {
                        default:
                            return super.inverseDeltaTransform(x, y, z);
                        case APPLY_SHEAR | APPLY_TRANSLATE:
                        case APPLY_SHEAR:
                            if (xy == 0.0 || yx == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D(
                                    (1.0 / yx) * y,
                                    (1.0 / xy) * x, z);
                        case APPLY_SCALE | APPLY_TRANSLATE:
                        case APPLY_SCALE:
                            if (xx == 0.0 || yy == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D(
                                    (1.0 / xx) * x,
                                    (1.0 / yy) * y, z);
                        case APPLY_TRANSLATE:
                        case APPLY_IDENTITY:
                            return new Point3D(x, y, z);
                    }

                case APPLY_TRANSLATE:
                    return new Point3D(x, y, z);
                case APPLY_SCALE | APPLY_TRANSLATE:
                case APPLY_SCALE:
                    if (xx == 0.0 || yy == 0.0 || zz == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point3D(
                            (1.0 / xx) * x,
                            (1.0 / yy) * y,
                            (1.0 / zz) * z);
                case APPLY_3D_COMPLEX:
                    return super.inverseDeltaTransform(x, y, z);
            }
        }

        /* *************************************************************************
         *                                                                         *
         *                               Other API                                 *
         *                                                                         *
         **************************************************************************/

        @Override
        public String toString() {
           final StringBuilder sb = new StringBuilder("Transform [\n");

            sb.append("\t").append(xx);
            sb.append(", ").append(xy);
            sb.append(", ").append(xz);
            sb.append(", ").append(xt);
            sb.append('\n');
            sb.append("\t").append(yx);
            sb.append(", ").append(yy);
            sb.append(", ").append(yz);
            sb.append(", ").append(yt);
            sb.append('\n');
            sb.append("\t").append(zx);
            sb.append(", ").append(zy);
            sb.append(", ").append(zz);
            sb.append(", ").append(zt);

            return sb.append("\n]").toString();
        }

        /* *************************************************************************
         *                                                                         *
         *                    Internal implementation stuff                        *
         *                                                                         *
         **************************************************************************/

        private void updateState() {
            updateState2D();

            state3d = APPLY_NON_3D;

            if (xz != 0.0 ||
                yz != 0.0 ||
                zx != 0.0 ||
                zy != 0.0)
            {
                state3d = APPLY_3D_COMPLEX;
            } else {
                if ((state2d & APPLY_SHEAR) == 0) {
                    if (zt != 0.0) {
                        state3d |= APPLY_TRANSLATE;
                    }
                    if (zz != 1.0) {
                        state3d |= APPLY_SCALE;
                    }
                    if (state3d != APPLY_NON_3D) {
                        state3d |= (state2d & (APPLY_SCALE | APPLY_TRANSLATE));
                    }
                } else {
                    if (zz != 1.0 || zt != 0.0) {
                        state3d = APPLY_3D_COMPLEX;
                    }
                }
            }
        }

        private void updateState2D() {
            if (xy == 0.0 && yx == 0.0) {
                if (xx == 1.0 && yy == 1.0) {
                    if (xt == 0.0 && yt == 0.0) {
                        state2d = APPLY_IDENTITY;
                    } else {
                        state2d = APPLY_TRANSLATE;
                    }
                } else {
                    if (xt == 0.0 && yt == 0.0) {
                        state2d = APPLY_SCALE;
                    } else {
                        state2d = (APPLY_SCALE | APPLY_TRANSLATE);
                    }
                }
            } else {
                if (xx == 0.0 && yy == 0.0) {
                    if (xt == 0.0 && yt == 0.0) {
                        state2d = APPLY_SHEAR;
                    } else {
                        state2d = (APPLY_SHEAR | APPLY_TRANSLATE);
                    }
                } else {
                    if (xt == 0.0 && yt == 0.0) {
                        state2d = (APPLY_SHEAR | APPLY_SCALE);
                    } else {
                        state2d = (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE);
                    }
                }
            }
        }

        void ensureCanTransform2DPoint() throws IllegalStateException {
            if (state3d != APPLY_NON_3D) {
                throw new IllegalStateException("Cannot transform 2D point "
                        + "with a 3D transform");
            }
        }

        private static void stateError() {
            throw new InternalError("missing case in a switch");
        }

        /**
         * @treatAsPrivate implementation detail
         * @deprecated This is an internal API that is not intended for use and will be removed in the next version
         */
        @Deprecated
        @Override
        public void impl_apply(final Affine3D trans) {
            trans.concatenate(xx, xy, xz, xt,
                              yx, yy, yz, yt,
                              zx, zy, zz, zt);
        }

        /**
         * @treatAsPrivate implementation detail
         * @deprecated This is an internal API that is not intended for use and will be removed in the next version
         */
        @Deprecated
        @Override
        public BaseTransform impl_derive(final BaseTransform trans) {
            return trans.deriveWithConcatenation(xx, xy, xz, xt,
                                                 yx, yy, yz, yt,
                                                 zx, zy, zz, zt);
        }

        /**
         * Used only by tests to check the 2d matrix state
         */
        int getState2d() {
            return state2d;
        }

        /**
         * Used only by tests to check the 3d matrix state
         */
        int getState3d() {
            return state3d;
        }

    }
}

