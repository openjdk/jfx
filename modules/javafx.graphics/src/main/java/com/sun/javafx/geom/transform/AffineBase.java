/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.geom.transform;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.Vec3d;

/**
 *
 */
public abstract class AffineBase extends BaseTransform {
    /**
     * This constant is used for the internal state variable to indicate
     * that no calculations need to be performed and that the source
     * coordinates only need to be copied to their destinations to
     * complete the transformation equation of this transform.
     * @see #APPLY_TRANSLATE
     * @see #APPLY_SCALE
     * @see #APPLY_SHEAR
     * @see #APPLY_3D
     * @see #state
     */
    protected static final int APPLY_IDENTITY = 0;

    /**
     * This constant is used for the internal state variable to indicate
     * that the translation components of the matrix (m02 and m12) need
     * to be added to complete the transformation equation of this transform.
     * @see #APPLY_IDENTITY
     * @see #APPLY_SCALE
     * @see #APPLY_SHEAR
     * @see #APPLY_3D
     * @see #state
     */
    protected static final int APPLY_TRANSLATE = 1;

    /**
     * This constant is used for the internal state variable to indicate
     * that the scaling components of the matrix (m00 and m11) need
     * to be factored in to complete the transformation equation of
     * this transform.  If the APPLY_SHEAR bit is also set then it
     * indicates that the scaling components are not both 0.0.  If the
     * APPLY_SHEAR bit is not also set then it indicates that the
     * scaling components are not both 1.0.  If neither the APPLY_SHEAR
     * nor the APPLY_SCALE bits are set then the scaling components
     * are both 1.0, which means that the x and y components contribute
     * to the transformed coordinate, but they are not multiplied by
     * any scaling factor.
     * @see #APPLY_IDENTITY
     * @see #APPLY_TRANSLATE
     * @see #APPLY_SHEAR
     * @see #APPLY_3D
     * @see #state
     */
    protected static final int APPLY_SCALE = 2;

    /**
     * This constant is used for the internal state variable to indicate
     * that the shearing components of the matrix (m01 and m10) need
     * to be factored in to complete the transformation equation of this
     * transform.  The presence of this bit in the state variable changes
     * the interpretation of the APPLY_SCALE bit as indicated in its
     * documentation.
     * @see #APPLY_IDENTITY
     * @see #APPLY_TRANSLATE
     * @see #APPLY_SCALE
     * @see #APPLY_3D
     * @see #state
     */
    protected static final int APPLY_SHEAR = 4;

    /**
     * This constant is used for the internal state variable to indicate
     * that the 3D (Z) components of the matrix (m*z and mz*) need
     * to be factored in to complete the transformation equation of this
     * transform.
     * @see #APPLY_IDENTITY
     * @see #APPLY_TRANSLATE
     * @see #APPLY_SCALE
     * @see #APPLY_SHEAR
     * @see #state
     */
    protected static final int APPLY_3D = 8;

    /*
     * The following mask can be used to extract the 2D state constants from
     * a state variable for cases where we know we can ignore the 3D matrix
     * elements (such as in the 2D coordinate transform methods).
     */
    protected static final int APPLY_2D_MASK = (APPLY_TRANSLATE | APPLY_SCALE | APPLY_SHEAR);
    protected static final int APPLY_2D_DELTA_MASK = (APPLY_SCALE | APPLY_SHEAR);

    /*
     * For methods which combine together the state of two separate
     * transforms and dispatch based upon the combination, these constants
     * specify how far to shift one of the states so that the two states
     * are mutually non-interfering and provide constants for testing the
     * bits of the shifted (HI) state.  The methods in this class use
     * the convention that the state of "this" transform is unshifted and
     * the state of the "other" or "argument" transform is shifted (HI).
     */
    protected static final int HI_SHIFT = 4;
    protected static final int HI_IDENTITY = APPLY_IDENTITY << HI_SHIFT;
    protected static final int HI_TRANSLATE = APPLY_TRANSLATE << HI_SHIFT;
    protected static final int HI_SCALE = APPLY_SCALE << HI_SHIFT;
    protected static final int HI_SHEAR = APPLY_SHEAR << HI_SHIFT;
    protected static final int HI_3D = APPLY_3D << HI_SHIFT;

    /**
     * The X coordinate scaling element of the 3x3
     * affine transformation matrix.
     */
    protected double mxx;

    /**
     * The Y coordinate shearing element of the 3x3
     * affine transformation matrix.
     */
    protected double myx;

    /**
     * The X coordinate shearing element of the 3x3
     * affine transformation matrix.
     */
    protected double mxy;

    /**
     * The Y coordinate scaling element of the 3x3
     * affine transformation matrix.
     */
    protected double myy;

    /**
     * The X coordinate of the translation element of the
     * 3x3 affine transformation matrix.
     */
    protected double mxt;

    /**
     * The Y coordinate of the translation element of the
     * 3x3 affine transformation matrix.
     */
    protected double myt;

    /**
     * This field keeps track of which components of the matrix need to
     * be applied when performing a transformation.
     * @see #APPLY_IDENTITY
     * @see #APPLY_TRANSLATE
     * @see #APPLY_SCALE
     * @see #APPLY_SHEAR
     * @see #APPLY_3D
     */
    protected transient int state;

    /**
     * This field caches the current transformation type of the matrix.
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #TYPE_UNKNOWN
     * @see #getType
     */
    protected transient int type;

    /*
     * Convenience method used internally to throw exceptions when
     * a case was forgotten in a switch statement.
     */
    protected static void stateError() {
        throw new InternalError("missing case in transform state switch");
    }

    /**
     * Manually recalculates the state of the transform when the matrix
     * changes too much to predict the effects on the state.
     * The following table specifies what the various settings of the
     * state field say about the values of the corresponding matrix
     * element fields.
     * Note that the rules governing the SCALE fields are slightly
     * different depending on whether the SHEAR flag is also set.
     * <pre>
     *                     SCALE            SHEAR          TRANSLATE
     *                    m00/m11          m01/m10          m02/m12
     *
     * IDENTITY             1.0              0.0              0.0
     * TRANSLATE (TR)       1.0              0.0          not both 0.0
     * SCALE (SC)       not both 1.0         0.0              0.0
     * TR | SC          not both 1.0         0.0          not both 0.0
     * SHEAR (SH)           0.0          not both 0.0         0.0
     * TR | SH              0.0          not both 0.0     not both 0.0
     * SC | SH          not both 0.0     not both 0.0         0.0
     * TR | SC | SH     not both 0.0     not both 0.0     not both 0.0
     * </pre>
     */
    protected void updateState() {
        updateState2D();
    }

    /*
     * This variant of the method is for cases where we know the 3D elements
     * are set to identity...
     */
    protected void updateState2D() {
        if (mxy == 0.0 && myx == 0.0) {
            if (mxx == 1.0 && myy == 1.0) {
                if (mxt == 0.0 && myt == 0.0) {
                    state = APPLY_IDENTITY;
                    type = TYPE_IDENTITY;
                } else {
                    state = APPLY_TRANSLATE;
                    type = TYPE_TRANSLATION;
                }
            } else {
                if (mxt == 0.0 && myt == 0.0) {
                    state = APPLY_SCALE;
                } else {
                    state = (APPLY_SCALE | APPLY_TRANSLATE);
                }
                type = TYPE_UNKNOWN;
            }
        } else {
            if (mxx == 0.0 && myy == 0.0) {
                if (mxt == 0.0 && myt == 0.0) {
                    state = APPLY_SHEAR;
                } else {
                    state = (APPLY_SHEAR | APPLY_TRANSLATE);
                }
            } else {
                if (mxt == 0.0 && myt == 0.0) {
                    state = (APPLY_SHEAR | APPLY_SCALE);
                } else {
                    state = (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE);
                }
            }
            type = TYPE_UNKNOWN;
        }
    }

    public int getType() {
        if (type == TYPE_UNKNOWN) {
            updateState(); // TODO: Is this really needed? (RT-26884)
            if (type == TYPE_UNKNOWN) {
                type = calculateType();
            }
        }
        return type;
    }

    protected int calculateType() {
        int ret = ((state & APPLY_3D) == 0) ? TYPE_IDENTITY : TYPE_AFFINE_3D;
        boolean sgn0, sgn1;
        switch (state & APPLY_2D_MASK) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            ret |= TYPE_TRANSLATION;
            /* NOBREAK */
        case (APPLY_SHEAR | APPLY_SCALE):
            if (mxx * mxy + myx * myy != 0) {
                // Transformed unit vectors are not perpendicular...
                ret |= TYPE_GENERAL_TRANSFORM;
                break;
            }
            sgn0 = (mxx >= 0.0);
            sgn1 = (myy >= 0.0);
            if (sgn0 == sgn1) {
                // sgn(mxx) == sgn(myy) therefore sgn(mxy) == -sgn(myx)
                // This is the "unflipped" (right-handed) state
                if (mxx != myy || mxy != -myx) {
                    ret |= (TYPE_GENERAL_ROTATION | TYPE_GENERAL_SCALE);
                } else if (mxx * myy - mxy * myx != 1.0) {
                    ret |= (TYPE_GENERAL_ROTATION | TYPE_UNIFORM_SCALE);
                } else {
                    ret |= TYPE_GENERAL_ROTATION;
                }
            } else {
                // sgn(mxx) == -sgn(myy) therefore sgn(mxy) == sgn(myx)
                // This is the "flipped" (left-handed) state
                if (mxx != -myy || mxy != myx) {
                    ret |= (TYPE_GENERAL_ROTATION |
                            TYPE_FLIP |
                            TYPE_GENERAL_SCALE);
                } else if (mxx * myy - mxy * myx != 1.0) {
                    ret |= (TYPE_GENERAL_ROTATION |
                            TYPE_FLIP |
                            TYPE_UNIFORM_SCALE);
                } else {
                    ret |= (TYPE_GENERAL_ROTATION | TYPE_FLIP);
                }
            }
            break;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            ret |= TYPE_TRANSLATION;
            /* NOBREAK */
        case (APPLY_SHEAR):
            sgn0 = (mxy >= 0.0);
            sgn1 = (myx >= 0.0);
            if (sgn0 != sgn1) {
                // Different signs - simple 90 degree rotation
                if (mxy != -myx) {
                    ret |= (TYPE_QUADRANT_ROTATION | TYPE_GENERAL_SCALE);
                } else if (mxy != 1.0 && mxy != -1.0) {
                    ret |= (TYPE_QUADRANT_ROTATION | TYPE_UNIFORM_SCALE);
                } else {
                    ret |= TYPE_QUADRANT_ROTATION;
                }
            } else {
                // Same signs - 90 degree rotation plus an axis flip too
                if (mxy == myx) {
                    ret |= (TYPE_QUADRANT_ROTATION |
                            TYPE_FLIP |
                            TYPE_UNIFORM_SCALE);
                } else {
                    ret |= (TYPE_QUADRANT_ROTATION |
                            TYPE_FLIP |
                            TYPE_GENERAL_SCALE);
                }
            }
            break;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            ret |= TYPE_TRANSLATION;
            /* NOBREAK */
        case (APPLY_SCALE):
            sgn0 = (mxx >= 0.0);
            sgn1 = (myy >= 0.0);
            if (sgn0 == sgn1) {
                if (sgn0) {
                    // Both scaling factors non-negative - simple scale
                    // Note: APPLY_SCALE implies M0, M1 are not both 1
                    if (mxx == myy) {
                        ret |= TYPE_UNIFORM_SCALE;
                    } else {
                        ret |= TYPE_GENERAL_SCALE;
                    }
                } else {
                    // Both scaling factors negative - 180 degree rotation
                    if (mxx != myy) {
                        ret |= (TYPE_QUADRANT_ROTATION | TYPE_GENERAL_SCALE);
                    } else if (mxx != -1.0) {
                        ret |= (TYPE_QUADRANT_ROTATION | TYPE_UNIFORM_SCALE);
                    } else {
                        ret |= TYPE_QUADRANT_ROTATION;
                    }
                }
            } else {
                // Scaling factor signs different - flip about some axis
                if (mxx == -myy) {
                    if (mxx == 1.0 || mxx == -1.0) {
                        ret |= TYPE_FLIP;
                    } else {
                        ret |= (TYPE_FLIP | TYPE_UNIFORM_SCALE);
                    }
                } else {
                    ret |= (TYPE_FLIP | TYPE_GENERAL_SCALE);
                }
            }
            break;
        case (APPLY_TRANSLATE):
            ret |= TYPE_TRANSLATION;
            break;
        case (APPLY_IDENTITY):
            break;
        }
        return ret;
    }

    /**
     * Returns the X coordinate scaling element (mxx) of the 3x3
     * affine transformation matrix.
     * @return a double value that is the X coordinate of the scaling
     *  element of the affine transformation matrix.
     * @see #getMatrix
     */
    @Override
    public double getMxx() {
        return mxx;
    }

    /**
     * Returns the Y coordinate scaling element (myy) of the 3x3
     * affine transformation matrix.
     * @return a double value that is the Y coordinate of the scaling
     *  element of the affine transformation matrix.
     * @see #getMatrix
     */
    @Override
    public double getMyy() {
        return myy;
    }

    /**
     * Returns the X coordinate shearing element (mxy) of the 3x3
     * affine transformation matrix.
     * @return a double value that is the X coordinate of the shearing
     *  element of the affine transformation matrix.
     * @see #getMatrix
     */
    @Override
    public double getMxy() {
        return mxy;
    }

    /**
     * Returns the Y coordinate shearing element (myx) of the 3x3
     * affine transformation matrix.
     * @return a double value that is the Y coordinate of the shearing
     *  element of the affine transformation matrix.
     * @see #getMatrix
     */
    @Override
    public double getMyx() {
        return myx;
    }

    /**
     * Returns the X coordinate of the translation element (mxt) of the
     * 3x3 affine transformation matrix.
     * @return a double value that is the X coordinate of the translation
     *  element of the affine transformation matrix.
     * @see #getMatrix
     */
    @Override
    public double getMxt() {
        return mxt;
    }

    /**
     * Returns the Y coordinate of the translation element (myt) of the
     * 3x3 affine transformation matrix.
     * @return a double value that is the Y coordinate of the translation
     *  element of the affine transformation matrix.
     * @see #getMatrix
     */
    @Override
    public double getMyt() {
        return myt;
    }

    /**
     * Returns <code>true</code> if this <code>Affine2D</code> is
     * an identity transform.
     * @return <code>true</code> if this <code>Affine2D</code> is
     * an identity transform; <code>false</code> otherwise.
     */
    public boolean isIdentity() {
        return (state == APPLY_IDENTITY || (getType() == TYPE_IDENTITY));
    }

    @Override
    public boolean isTranslateOrIdentity() {
        return (state <= APPLY_TRANSLATE || (getType() <= TYPE_TRANSLATION));
    }

    @Override
    public boolean is2D() {
        return (state < APPLY_3D || getType() <= TYPE_AFFINE2D_MASK);
    }

    /**
     * Returns the determinant of the matrix representation of the transform.
     * The determinant is useful both to determine if the transform can
     * be inverted and to get a single value representing the
     * combined X and Y scaling of the transform.
     * <p>
     * If the determinant is non-zero, then this transform is
     * invertible and the various methods that depend on the inverse
     * transform do not need to throw a
     * {@link NoninvertibleTransformException}.
     * If the determinant is zero then this transform can not be
     * inverted since the transform maps all input coordinates onto
     * a line or a point.
     * If the determinant is near enough to zero then inverse transform
     * operations might not carry enough precision to produce meaningful
     * results.
     * <p>
     * If this transform represents a uniform scale, as indicated by
     * the <code>getType</code> method then the determinant also
     * represents the square of the uniform scale factor by which all of
     * the points are expanded from or contracted towards the origin.
     * If this transform represents a non-uniform scale or more general
     * transform then the determinant is not likely to represent a
     * value useful for any purpose other than determining if inverse
     * transforms are possible.
     * <p>
     * Mathematically, the determinant is calculated using the formula:
     * <pre>
     *      |  mxx  mxy  mxt  |
     *      |  myx  myy  myt  |  =  mxx * myy - mxy * myx
     *      |   0    0    1   |
     * </pre>
     *
     * @return the determinant of the matrix used to transform the
     * coordinates.
     * @see #getType
     * @see #createInverse
     * @see #inverseTransform
     * @see #TYPE_UNIFORM_SCALE
     */
    public double getDeterminant() {
        // assert(APPLY_3D was dealt with at a higher level)
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
            return mxx * myy - mxy * myx;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            return -(mxy * myx);
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            return mxx * myy;
        case (APPLY_TRANSLATE):
        case (APPLY_IDENTITY):
            return 1.0;
        }
    }

    /**
     * Resets the 3D (Z) components of the matrix to identity settings
     * (if they are present).
     * This is a NOP unless the transform is Affine3D in which case it
     * needs to reset its added fields.
     */
    protected abstract void reset3Delements();

    /**
     * Resets this transform to the Identity transform.
     */
    public void setToIdentity() {
        mxx = myy = 1.0;
        myx = mxy = mxt = myt = 0.0;
        reset3Delements();
        state = APPLY_IDENTITY;
        type = TYPE_IDENTITY;
    }

    /**
     * Sets this transform to the matrix specified by the 6
     * double precision values.
     *
     * @param mxx the X coordinate scaling element of the 3x3 matrix
     * @param myx the Y coordinate shearing element of the 3x3 matrix
     * @param mxy the X coordinate shearing element of the 3x3 matrix
     * @param myy the Y coordinate scaling element of the 3x3 matrix
     * @param mxt the X coordinate translation element of the 3x3 matrix
     * @param myt the Y coordinate translation element of the 3x3 matrix
     */
    public void setTransform(double mxx, double myx,
                             double mxy, double myy,
                             double mxt, double myt) {
        this.mxx = mxx;
        this.myx = myx;
        this.mxy = mxy;
        this.myy = myy;
        this.mxt = mxt;
        this.myt = myt;
        reset3Delements();
        updateState2D();
    }

    /**
     * Sets this transform to a shearing transformation.
     * The matrix representing this transform becomes:
     * <pre>
     *      [   1   shx   0   ]
     *      [  shy   1    0   ]
     *      [   0    0    1   ]
     * </pre>
     * @param shx the multiplier by which coordinates are shifted in the
     * direction of the positive X axis as a factor of their Y coordinate
     * @param shy the multiplier by which coordinates are shifted in the
     * direction of the positive Y axis as a factor of their X coordinate
     */
    public void setToShear(double shx, double shy) {
        mxx = 1.0;
        mxy = shx;
        myx = shy;
        myy = 1.0;
        mxt = 0.0;
        myt = 0.0;
        reset3Delements();
        if (shx != 0.0 || shy != 0.0) {
            state = (APPLY_SHEAR | APPLY_SCALE);
            type = TYPE_UNKNOWN;
        } else {
            state = APPLY_IDENTITY;
            type = TYPE_IDENTITY;
        }
    }

    public Point2D transform(Point2D pt) {
        return transform(pt, pt);
    }

    /**
     * Transforms the specified <code>ptSrc</code> and stores the result
     * in <code>ptDst</code>.
     * If <code>ptDst</code> is <code>null</code>, a new {@link Point2D}
     * object is allocated and then the result of the transformation is
     * stored in this object.
     * In either case, <code>ptDst</code>, which contains the
     * transformed point, is returned for convenience.
     * If <code>ptSrc</code> and <code>ptDst</code> are the same
     * object, the input point is correctly overwritten with
     * the transformed point.
     * @param ptSrc the specified <code>Point2D</code> to be transformed
     * @param ptDst the specified <code>Point2D</code> that stores the
     * result of transforming <code>ptSrc</code>
     * @return the <code>ptDst</code> after transforming
     * <code>ptSrc</code> and stroring the result in <code>ptDst</code>.
     */
    public Point2D transform(Point2D ptSrc, Point2D ptDst) {
        if (ptDst == null) {
            ptDst = new Point2D();
        }
        // Copy source coords into local variables in case src == dst
        double x = ptSrc.x;
        double y = ptSrc.y;
        // double z = 0.0
        // Note that this method also works for 3D transforms since the
        // mxz and myz matrix elements get multiplied by z (0.0) and the
        // mzx, mzy, mzz, and mzt elements only get used to calculate
        // the resulting Z coordinate, which we drop (ignore).
        switch (state & APPLY_2D_MASK) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            ptDst.setLocation((float)(x * mxx + y * mxy + mxt),
                              (float)(x * myx + y * myy + myt));
            return ptDst;
        case (APPLY_SHEAR | APPLY_SCALE):
            ptDst.setLocation((float)(x * mxx + y * mxy),
                              (float)(x * myx + y * myy));
            return ptDst;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            ptDst.setLocation((float)(y * mxy + mxt),
                              (float)(x * myx + myt));
            return ptDst;
        case (APPLY_SHEAR):
            ptDst.setLocation((float)(y * mxy), (float)(x * myx));
            return ptDst;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            ptDst.setLocation((float)(x * mxx + mxt), (float)(y * myy + myt));
            return ptDst;
        case (APPLY_SCALE):
            ptDst.setLocation((float)(x * mxx), (float)(y * myy));
            return ptDst;
        case (APPLY_TRANSLATE):
            ptDst.setLocation((float)(x + mxt), (float)(y + myt));
            return ptDst;
        case (APPLY_IDENTITY):
            ptDst.setLocation((float) x, (float) y);
            return ptDst;
        }

        /* NOTREACHED */
    }

    public Vec3d transform(Vec3d src, Vec3d dst) {
        if (dst == null) {
            dst = new Vec3d();
        }
        // Copy source coords into local variables in case src == dst
        double x = src.x;
        double y = src.y;
        double z = src.z;
        // assert(APPLY_3D was dealt with at a higher level)
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            dst.x = x * mxx + y * mxy + mxt;
            dst.y = x * myx + y * myy + myt;
            dst.z = z;
            return dst;
        case (APPLY_SHEAR | APPLY_SCALE):
            dst.x = x * mxx + y * mxy;
            dst.y = x * myx + y * myy;
            dst.z = z;
            return dst;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            dst.x = y * mxy + mxt;
            dst.y = x * myx + myt;
            dst.z = z;
            return dst;
        case (APPLY_SHEAR):
            dst.x = y * mxy;
            dst.y = x * myx;
            dst.z = z;
            return dst;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            dst.x = x * mxx + mxt;
            dst.y = y * myy + myt;
            dst.z = z;
            return dst;
        case (APPLY_SCALE):
            dst.x = x * mxx;
            dst.y = y * myy;
            dst.z = z;
            return dst;
        case (APPLY_TRANSLATE):
            dst.x = x + mxt;
            dst.y = y + myt;
            dst.z = z;
            return dst;
        case (APPLY_IDENTITY):
            dst.x = x;
            dst.y = y;
            dst.z = z;
            return dst;
        }

        /* NOTREACHED */
    }

    /**
     * Transforms the specified <code>src</code> vector and stores the result
     * in <code>dst</code> vector, without applying the translation elements.
     * If <code>dst</code> is <code>null</code>, a new {@link Vec3d}
     * object is allocated and then the result of the transformation is
     * stored in this object.
     * In either case, <code>dst</code>, which contains the
     * transformed vector, is returned for convenience.
     * If <code>src</code> and <code>dst</code> are the same
     * object, the input vector is correctly overwritten with
     * the transformed vector.
     * @param src the specified <code>Vec3d</code> to be transformed
     * @param dst the specified <code>Vec3d</code> that stores the
     * result of transforming <code>src</code>
     * @return the <code>dst</code> vector after transforming
     * <code>src</code> and storing the result in <code>dst</code>.
     * @since JavaFX 8.0
     */
    public Vec3d deltaTransform(Vec3d src, Vec3d dst) {
        if (dst == null) {
            dst = new Vec3d();
        }
        // Copy source coords into local variables in case src == dst
        double x = src.x;
        double y = src.y;
        double z = src.z;
        // assert(APPLY_3D was dealt with at a higher level)
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
            dst.x = x * mxx + y * mxy ;
            dst.y = x * myx + y * myy;
            dst.z = z;
            return dst;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            dst.x = y * mxy;
            dst.y = x * myx;
            dst.z = z;
            return dst;
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            dst.x = x * mxx;
            dst.y = y * myy;
            dst.z = z;
            return dst;
        case (APPLY_TRANSLATE):
        case (APPLY_IDENTITY):
            dst.x = x;
            dst.y = y;
            dst.z = z;
            return dst;
        }

        /* NOTREACHED */
    }

    private BaseBounds transform2DBounds(RectBounds src, RectBounds dst) {
        switch (state & APPLY_2D_MASK) {
        default:
            stateError();
            /* NOTREACHED */
            case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            /* NOBREAK */
            case (APPLY_SHEAR | APPLY_SCALE):
                double x1 = src.getMinX();
                double y1 = src.getMinY();
                double x2 = src.getMaxX();
                double y2 = src.getMaxY();
                dst.setBoundsAndSort((float) (x1 * mxx + y1 * mxy),
                        (float) (x1 * myx + y1 * myy),
                        (float) (x2 * mxx + y2 * mxy),
                        (float) (x2 * myx + y2 * myy));
                dst.add((float) (x1 * mxx + y2 * mxy),
                        (float) (x1 * myx + y2 * myy));
                dst.add((float) (x2 * mxx + y1 * mxy),
                        (float) (x2 * myx + y1 * myy));
                dst.setBounds((float) (dst.getMinX() + mxt),
                        (float) (dst.getMinY() + myt),
                        (float) (dst.getMaxX() + mxt),
                        (float) (dst.getMaxY() + myt));
                break;
            case (APPLY_SHEAR | APPLY_TRANSLATE):
                dst.setBoundsAndSort((float) (src.getMinY() * mxy + mxt),
                        (float) (src.getMinX() * myx + myt),
                        (float) (src.getMaxY() * mxy + mxt),
                        (float) (src.getMaxX() * myx + myt));
                break;
            case (APPLY_SHEAR):
                dst.setBoundsAndSort((float) (src.getMinY() * mxy),
                        (float) (src.getMinX() * myx),
                        (float) (src.getMaxY() * mxy),
                        (float) (src.getMaxX() * myx));
                break;
            case (APPLY_SCALE | APPLY_TRANSLATE):
                dst.setBoundsAndSort((float) (src.getMinX() * mxx + mxt),
                        (float) (src.getMinY() * myy + myt),
                        (float) (src.getMaxX() * mxx + mxt),
                        (float) (src.getMaxY() * myy + myt));
                break;
            case (APPLY_SCALE):
                dst.setBoundsAndSort((float) (src.getMinX() * mxx),
                        (float) (src.getMinY() * myy),
                        (float) (src.getMaxX() * mxx),
                        (float) (src.getMaxY() * myy));
                break;
            case (APPLY_TRANSLATE):
                dst.setBounds((float) (src.getMinX() + mxt),
                        (float) (src.getMinY() + myt),
                        (float) (src.getMaxX() + mxt),
                        (float) (src.getMaxY() + myt));
                break;
            case (APPLY_IDENTITY):
                if (src != dst) {
                    dst.setBounds(src);
                }
                break;
        }
        return dst;
    }

    // Note: Only use this method if src or dst is a 3D bounds
    private BaseBounds transform3DBounds(BaseBounds src, BaseBounds dst) {
        switch (state & APPLY_2D_MASK) {
            default:
                stateError();
            /* NOTREACHED */
            // Note: Assuming mxz = myz = mzx = mzy = mzt 0
            case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            /* NOBREAK */
            case (APPLY_SHEAR | APPLY_SCALE):
                double x1 = src.getMinX();
                double y1 = src.getMinY();
                double z1 = src.getMinZ();
                double x2 = src.getMaxX();
                double y2 = src.getMaxY();
                double z2 = src.getMaxZ();
                dst.setBoundsAndSort((float) (x1 * mxx + y1 * mxy),
                        (float) (x1 * myx + y1 * myy),
                        (float) z1,
                        (float) (x2 * mxx + y2 * mxy),
                        (float) (x2 * myx + y2 * myy),
                        (float) z2);
                dst.add((float) (x1 * mxx + y2 * mxy),
                        (float) (x1 * myx + y2 * myy), 0);
                dst.add((float) (x2 * mxx + y1 * mxy),
                        (float) (x2 * myx + y1 * myy), 0);
                dst.deriveWithNewBounds((float) (dst.getMinX() + mxt),
                        (float) (dst.getMinY() + myt),
                        (float) dst.getMinZ(),
                        (float) (dst.getMaxX() + mxt),
                        (float) (dst.getMaxY() + myt),
                        (float) dst.getMaxZ());
                break;
            case (APPLY_SHEAR | APPLY_TRANSLATE):
                dst = dst.deriveWithNewBoundsAndSort((float) (src.getMinY() * mxy + mxt),
                        (float) (src.getMinX() * myx + myt),
                        (float) src.getMinZ(),
                        (float) (src.getMaxY() * mxy + mxt),
                        (float) (src.getMaxX() * myx + myt),
                        (float) src.getMaxZ());
                break;
            case (APPLY_SHEAR):
                dst = dst.deriveWithNewBoundsAndSort((float) (src.getMinY() * mxy),
                        (float) (src.getMinX() * myx),
                        (float) src.getMinZ(),
                        (float) (src.getMaxY() * mxy),
                        (float) (src.getMaxX() * myx),
                        (float) src.getMaxZ());
                break;
            case (APPLY_SCALE | APPLY_TRANSLATE):
                dst = dst.deriveWithNewBoundsAndSort((float) (src.getMinX() * mxx + mxt),
                        (float) (src.getMinY() * myy + myt),
                        (float) src.getMinZ(),
                        (float) (src.getMaxX() * mxx + mxt),
                        (float) (src.getMaxY() * myy + myt),
                        (float) src.getMaxZ());
                break;
            case (APPLY_SCALE):
                dst = dst.deriveWithNewBoundsAndSort((float) (src.getMinX() * mxx),
                        (float) (src.getMinY() * myy),
                        (float) src.getMinZ(),
                        (float) (src.getMaxX() * mxx),
                        (float) (src.getMaxY() * myy),
                        (float) src.getMaxZ());
                break;
            case (APPLY_TRANSLATE):
                dst = dst.deriveWithNewBounds((float) (src.getMinX() + mxt),
                        (float) (src.getMinY() + myt),
                        (float) src.getMinZ(),
                        (float) (src.getMaxX() + mxt),
                        (float) (src.getMaxY() + myt),
                        (float) src.getMaxZ());
                break;
            case (APPLY_IDENTITY):
                if (src != dst) {
                    dst = dst.deriveWithNewBounds(src);
                }
                break;
        }
        return dst;
    }

    public BaseBounds transform(BaseBounds src, BaseBounds dst) {
        // assert(APPLY_3D was dealt with at a higher level)
        if (src.getBoundsType() != BaseBounds.BoundsType.RECTANGLE ||
                dst.getBoundsType() != BaseBounds.BoundsType.RECTANGLE) {
            return transform3DBounds(src, dst);
        }
        return transform2DBounds((RectBounds)src, (RectBounds)dst);
    }

    public void transform(Rectangle src, Rectangle dst) {
        // assert(APPLY_3D was dealt with at a higher level)
        switch (state & APPLY_2D_MASK) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            RectBounds b = new RectBounds(src);
            //TODO: Need to verify that this is a safe cast ... (RT-26885)
            b = (RectBounds) transform(b, b);
            dst.setBounds(b);
            return;
        case (APPLY_TRANSLATE):
            Translate2D.transform(src, dst, mxt, myt);
            return;
        case (APPLY_IDENTITY):
            if (dst != src) {
                dst.setBounds(src);
            }
            return;
        }
    }

    /**
     * Transforms an array of floating point coordinates by this transform.
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are overwritten by a
     * previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order <code>[x0, y0, x1, y1, ..., xn, yn]</code>.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed point coordinates
     * are returned.  Each point is stored as a pair of x,&nbsp;y
     * coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of points to be transformed
     */
    public void transform(float[] srcPts, int srcOff,
                          float[] dstPts, int dstOff,
                          int numPts)
    {
        doTransform(srcPts, srcOff, dstPts, dstOff, numPts,
                    (this.state & APPLY_2D_MASK));
    }

        /**
     * Transforms an array of relative distance vectors by this
     * transform.
     * A relative distance vector is transformed without applying the
     * translation components of the affine transformation matrix
     * using the following equations:
     * <pre>
     *  [  x' ]   [  m00  m01 (m02) ] [  x  ]   [ m00x + m01y ]
     *  [  y' ] = [  m10  m11 (m12) ] [  y  ] = [ m10x + m11y ]
     *  [ (1) ]   [  (0)  (0) ( 1 ) ] [ (1) ]   [     (1)     ]
     * </pre>
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are
     * overwritten by a previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the indicated
     * offset in the order <code>[x0, y0, x1, y1, ..., xn, yn]</code>.
     * @param srcPts the array containing the source distance vectors.
     * Each vector is stored as a pair of relative x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed distance vectors
     * are returned.  Each vector is stored as a pair of relative
     * x,&nbsp;y coordinates.
     * @param srcOff the offset to the first vector to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed vector that is stored in the destination array
     * @param numPts the number of vector coordinate pairs to be
     * transformed
     */
    public void deltaTransform(float[] srcPts, int srcOff,
                               float[] dstPts, int dstOff,
                               int numPts)
    {
        doTransform(srcPts, srcOff, dstPts, dstOff, numPts,
                    (this.state & APPLY_2D_DELTA_MASK));
    }

    private void doTransform(float[] srcPts, int srcOff,
                             float[] dstPts, int dstOff,
                             int numPts, int thestate)
    {
        double Mxx, Mxy, Mxt, Myx, Myy, Myt;    // For caching
        if (dstPts == srcPts &&
            dstOff > srcOff && dstOff < srcOff + numPts * 2)
        {
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
        // Note that this method also works for 3D transforms since the
        // mxz and myz matrix elements get multiplied by z (0.0) and the
        // mzx, mzy, mzz, and mzt elements only get used to calculate
        // the resulting Z coordinate, which we drop (ignore).
        switch (thestate) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxy = mxy; Mxt = mxt;
            Myx = myx; Myy = myy; Myt = myt;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (Mxx * x + Mxy * y + Mxt);
                dstPts[dstOff++] = (float) (Myx * x + Myy * y + Myt);
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            Mxx = mxx; Mxy = mxy;
            Myx = myx; Myy = myy;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (Mxx * x + Mxy * y);
                dstPts[dstOff++] = (float) (Myx * x + Myy * y);
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            Mxy = mxy; Mxt = mxt;
            Myx = myx; Myt = myt;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (Mxy * srcPts[srcOff++] + Mxt);
                dstPts[dstOff++] = (float) (Myx * x + Myt);
            }
            return;
        case (APPLY_SHEAR):
            Mxy = mxy; Myx = myx;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (Mxy * srcPts[srcOff++]);
                dstPts[dstOff++] = (float) (Myx * x);
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxt = mxt;
            Myy = myy; Myt = myt;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (Mxx * srcPts[srcOff++] + Mxt);
                dstPts[dstOff++] = (float) (Myy * srcPts[srcOff++] + Myt);
            }
            return;
        case (APPLY_SCALE):
            Mxx = mxx; Myy = myy;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (Mxx * srcPts[srcOff++]);
                dstPts[dstOff++] = (float) (Myy * srcPts[srcOff++]);
            }
            return;
        case (APPLY_TRANSLATE):
            Mxt = mxt; Myt = myt;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++] + Mxt);
                dstPts[dstOff++] = (float) (srcPts[srcOff++] + Myt);
            }
            return;
        case (APPLY_IDENTITY):
            if (srcPts != dstPts || srcOff != dstOff) {
                System.arraycopy(srcPts, srcOff, dstPts, dstOff,
                                 numPts * 2);
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Transforms an array of double precision coordinates by this transform.
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are
     * overwritten by a previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the indicated
     * offset in the order <code>[x0, y0, x1, y1, ..., xn, yn]</code>.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed point
     * coordinates are returned.  Each point is stored as a pair of
     * x,&nbsp;y coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of point objects to be transformed
     */
    public void transform(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts)
    {
        doTransform(srcPts, srcOff, dstPts, dstOff, numPts,
                    (this.state & APPLY_2D_MASK));
    }

    /**
     * Transforms an array of relative distance vectors by this
     * transform.
     * A relative distance vector is transformed without applying the
     * translation components of the affine transformation matrix
     * using the following equations:
     * <pre>
     *  [  x' ]   [  m00  m01 (m02) ] [  x  ]   [ m00x + m01y ]
     *  [  y' ] = [  m10  m11 (m12) ] [  y  ] = [ m10x + m11y ]
     *  [ (1) ]   [  (0)  (0) ( 1 ) ] [ (1) ]   [     (1)     ]
     * </pre>
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are
     * overwritten by a previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the indicated
     * offset in the order <code>[x0, y0, x1, y1, ..., xn, yn]</code>.
     * @param srcPts the array containing the source distance vectors.
     * Each vector is stored as a pair of relative x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed distance vectors
     * are returned.  Each vector is stored as a pair of relative
     * x,&nbsp;y coordinates.
     * @param srcOff the offset to the first vector to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed vector that is stored in the destination array
     * @param numPts the number of vector coordinate pairs to be
     * transformed
     */
    public void deltaTransform(double[] srcPts, int srcOff,
                               double[] dstPts, int dstOff,
                               int numPts)
    {
        doTransform(srcPts, srcOff, dstPts, dstOff, numPts,
                    (this.state & APPLY_2D_DELTA_MASK));
    }

    private void doTransform(double[] srcPts, int srcOff,
                             double[] dstPts, int dstOff,
                             int numPts, int thestate)
    {
        double Mxx, Mxy, Mxt, Myx, Myy, Myt;    // For caching
        if (dstPts == srcPts &&
            dstOff > srcOff && dstOff < srcOff + numPts * 2)
        {
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
        // Note that this method also works for 3D transforms since the
        // mxz and myz matrix elements get multiplied by z (0.0) and the
        // mzx, mzy, mzz, and mzt elements only get used to calculate
        // the resulting Z coordinate, which we drop (ignore).
        switch (thestate) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxy = mxy; Mxt = mxt;
            Myx = myx; Myy = myy; Myt = myt;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = Mxx * x + Mxy * y + Mxt;
                dstPts[dstOff++] = Myx * x + Myy * y + Myt;
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            Mxx = mxx; Mxy = mxy;
            Myx = myx; Myy = myy;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = Mxx * x + Mxy * y;
                dstPts[dstOff++] = Myx * x + Myy * y;
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            Mxy = mxy; Mxt = mxt;
            Myx = myx; Myt = myt;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = Mxy * srcPts[srcOff++] + Mxt;
                dstPts[dstOff++] = Myx * x + Myt;
            }
            return;
        case (APPLY_SHEAR):
            Mxy = mxy; Myx = myx;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = Mxy * srcPts[srcOff++];
                dstPts[dstOff++] = Myx * x;
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxt = mxt;
            Myy = myy; Myt = myt;
            while (--numPts >= 0) {
                dstPts[dstOff++] = Mxx * srcPts[srcOff++] + Mxt;
                dstPts[dstOff++] = Myy * srcPts[srcOff++] + Myt;
            }
            return;
        case (APPLY_SCALE):
            Mxx = mxx; Myy = myy;
            while (--numPts >= 0) {
                dstPts[dstOff++] = Mxx * srcPts[srcOff++];
                dstPts[dstOff++] = Myy * srcPts[srcOff++];
            }
            return;
        case (APPLY_TRANSLATE):
            Mxt = mxt; Myt = myt;
            while (--numPts >= 0) {
                dstPts[dstOff++] = srcPts[srcOff++] + Mxt;
                dstPts[dstOff++] = srcPts[srcOff++] + Myt;
            }
            return;
        case (APPLY_IDENTITY):
            if (srcPts != dstPts || srcOff != dstOff) {
                System.arraycopy(srcPts, srcOff, dstPts, dstOff,
                                 numPts * 2);
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Transforms an array of floating point coordinates by this transform
     * and stores the results into an array of doubles.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order <code>[x0, y0, x1, y1, ..., xn, yn]</code>.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed point coordinates
     * are returned.  Each point is stored as a pair of x,&nbsp;y
     * coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of points to be transformed
     */
    public void transform(float[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) {
        double Mxx, Mxy, Mxt, Myx, Myy, Myt;    // For caching
        // Note that this method also works for 3D transforms since the
        // mxz and myz matrix elements get multiplied by z (0.0) and the
        // mzx, mzy, mzz, and mzt elements only get used to calculate
        // the resulting Z coordinate, which we drop (ignore).
        switch (state & APPLY_2D_MASK) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxy = mxy; Mxt = mxt;
            Myx = myx; Myy = myy; Myt = myt;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = Mxx * x + Mxy * y + Mxt;
                dstPts[dstOff++] = Myx * x + Myy * y + Myt;
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            Mxx = mxx; Mxy = mxy;
            Myx = myx; Myy = myy;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = Mxx * x + Mxy * y;
                dstPts[dstOff++] = Myx * x + Myy * y;
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            Mxy = mxy; Mxt = mxt;
            Myx = myx; Myt = myt;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = Mxy * srcPts[srcOff++] + Mxt;
                dstPts[dstOff++] = Myx * x + Myt;
            }
            return;
        case (APPLY_SHEAR):
            Mxy = mxy; Myx = myx;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = Mxy * srcPts[srcOff++];
                dstPts[dstOff++] = Myx * x;
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxt = mxt;
            Myy = myy; Myt = myt;
            while (--numPts >= 0) {
                dstPts[dstOff++] = Mxx * srcPts[srcOff++] + Mxt;
                dstPts[dstOff++] = Myy * srcPts[srcOff++] + Myt;
            }
            return;
        case (APPLY_SCALE):
            Mxx = mxx; Myy = myy;
            while (--numPts >= 0) {
                dstPts[dstOff++] = Mxx * srcPts[srcOff++];
                dstPts[dstOff++] = Myy * srcPts[srcOff++];
            }
            return;
        case (APPLY_TRANSLATE):
            Mxt = mxt; Myt = myt;
            while (--numPts >= 0) {
                dstPts[dstOff++] = srcPts[srcOff++] + Mxt;
                dstPts[dstOff++] = srcPts[srcOff++] + Myt;
            }
            return;
        case (APPLY_IDENTITY):
            while (--numPts >= 0) {
                dstPts[dstOff++] = srcPts[srcOff++];
                dstPts[dstOff++] = srcPts[srcOff++];
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Transforms an array of double precision coordinates by this transform
     * and stores the results into an array of floats.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order <code>[x0, y0, x1, y1, ..., xn, yn]</code>.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed point
     * coordinates are returned.  Each point is stored as a pair of
     * x,&nbsp;y coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of point objects to be transformed
     */
    public void transform(double[] srcPts, int srcOff,
                          float[] dstPts, int dstOff,
                          int numPts) {
        double Mxx, Mxy, Mxt, Myx, Myy, Myt;    // For caching
        // Note that this method also works for 3D transforms since the
        // mxz and myz matrix elements get multiplied by z (0.0) and the
        // mzx, mzy, mzz, and mzt elements only get used to calculate
        // the resulting Z coordinate, which we drop (ignore).
        switch (state & APPLY_2D_MASK) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxy = mxy; Mxt = mxt;
            Myx = myx; Myy = myy; Myt = myt;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (Mxx * x + Mxy * y + Mxt);
                dstPts[dstOff++] = (float) (Myx * x + Myy * y + Myt);
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            Mxx = mxx; Mxy = mxy;
            Myx = myx; Myy = myy;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (Mxx * x + Mxy * y);
                dstPts[dstOff++] = (float) (Myx * x + Myy * y);
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            Mxy = mxy; Mxt = mxt;
            Myx = myx; Myt = myt;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (Mxy * srcPts[srcOff++] + Mxt);
                dstPts[dstOff++] = (float) (Myx * x + Myt);
            }
            return;
        case (APPLY_SHEAR):
            Mxy = mxy; Myx = myx;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (Mxy * srcPts[srcOff++]);
                dstPts[dstOff++] = (float) (Myx * x);
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxt = mxt;
            Myy = myy; Myt = myt;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (Mxx * srcPts[srcOff++] + Mxt);
                dstPts[dstOff++] = (float) (Myy * srcPts[srcOff++] + Myt);
            }
            return;
        case (APPLY_SCALE):
            Mxx = mxx; Myy = myy;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (Mxx * srcPts[srcOff++]);
                dstPts[dstOff++] = (float) (Myy * srcPts[srcOff++]);
            }
            return;
        case (APPLY_TRANSLATE):
            Mxt = mxt; Myt = myt;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++] + Mxt);
                dstPts[dstOff++] = (float) (srcPts[srcOff++] + Myt);
            }
            return;
        case (APPLY_IDENTITY):
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++]);
                dstPts[dstOff++] = (float) (srcPts[srcOff++]);
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Inverse transforms the specified <code>ptSrc</code> and stores the
     * result in <code>ptDst</code>.
     * If <code>ptDst</code> is <code>null</code>, a new
     * <code>Point2D</code> object is allocated and then the result of the
     * transform is stored in this object.
     * In either case, <code>ptDst</code>, which contains the transformed
     * point, is returned for convenience.
     * If <code>ptSrc</code> and <code>ptDst</code> are the same
     * object, the input point is correctly overwritten with the
     * transformed point.
     * @param ptSrc the point to be inverse transformed
     * @param ptDst the resulting transformed point
     * @return <code>ptDst</code>, which contains the result of the
     * inverse transform.
     * @exception NoninvertibleTransformException  if the matrix cannot be
     *                                         inverted.
     */
    public Point2D inverseTransform(Point2D ptSrc, Point2D ptDst)
        throws NoninvertibleTransformException
    {
        if (ptDst == null) {
            ptDst = new Point2D();
        }
        // Copy source coords into local variables in case src == dst
        double x = ptSrc.x;
        double y = ptSrc.y;
        // assert(APPLY_3D was dealt with at a higher level)
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            x -= mxt;
            y -= myt;
            /* NOBREAK */
        case (APPLY_SHEAR | APPLY_SCALE):
            double det = mxx * myy - mxy * myx;
            if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            ptDst.setLocation((float)((x * myy - y * mxy) / det),
                              (float)((y * mxx - x * myx) / det));
            return ptDst;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            x -= mxt;
            y -= myt;
            /* NOBREAK */
        case (APPLY_SHEAR):
            if (mxy == 0.0 || myx == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            ptDst.setLocation((float)(y / myx), (float)(x / mxy));
            return ptDst;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            x -= mxt;
            y -= myt;
            /* NOBREAK */
        case (APPLY_SCALE):
            if (mxx == 0.0 || myy == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            ptDst.setLocation((float)(x / mxx), (float)(y / myy));
            return ptDst;
        case (APPLY_TRANSLATE):
            ptDst.setLocation((float)(x - mxt), (float)(y - myt));
            return ptDst;
        case (APPLY_IDENTITY):
            ptDst.setLocation((float) x, (float) y);
            return ptDst;
        }

        /* NOTREACHED */
    }

    /**
     * Inverse transforms the specified <code>src</code> and stores the
     * result in <code>dst</code>.
     * If <code>dst</code> is <code>null</code>, a new
     * <code>Vec3d</code> object is allocated and then the result of the
     * transform is stored in this object.
     * In either case, <code>dst</code>, which contains the transformed
     * point, is returned for convenience.
     * If <code>src</code> and <code>dst</code> are the same
     * object, the input point is correctly overwritten with the
     * transformed point.
     * @param src the point to be inverse transformed
     * @param dst the resulting transformed point
     * @return <code>dst</code>, which contains the result of the
     * inverse transform.
     * @exception NoninvertibleTransformException  if the matrix cannot be
     *                                         inverted.
     */
    @Override
    public Vec3d inverseTransform(Vec3d src, Vec3d dst)
        throws NoninvertibleTransformException
    {
        if (dst == null) {
            dst = new Vec3d();
        }
        // Copy source coords into local variables in case src == dst
        double x = src.x;
        double y = src.y;
        double z = src.z;
        // assert(APPLY_3D was dealt with at a higher level)
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            x -= mxt;
            y -= myt;
            /* NOBREAK */
        case (APPLY_SHEAR | APPLY_SCALE):
            double det = mxx * myy - mxy * myx;
            if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            dst.set(((x * myy - y * mxy) / det), ((y * mxx - x * myx) / det), z);
            return dst;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            x -= mxt;
            y -= myt;
            /* NOBREAK */
        case (APPLY_SHEAR):
            if (mxy == 0.0 || myx == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            dst.set((y / myx), (x / mxy), z);
            return dst;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            x -= mxt;
            y -= myt;
            /* NOBREAK */
        case (APPLY_SCALE):
            if (mxx == 0.0 || myy == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            dst.set((x / mxx), (y / myy), z);
            return dst;
        case (APPLY_TRANSLATE):
            dst.set((x - mxt), (y - myt), z);
            return dst;
        case (APPLY_IDENTITY):
            dst.set(x, y, z);
            return dst;
        }

        /* NOTREACHED */
    }

    /**
     * Inverse transforms the specified <code>src</code> vector and stores the
     * result in <code>dst</code> vector (without applying the translation
     * elements).
     * If <code>dst</code> is <code>null</code>, a new
     * <code>Vec3d</code> object is allocated and then the result of the
     * transform is stored in this object.
     * In either case, <code>dst</code>, which contains the transformed
     * vector, is returned for convenience.
     * If <code>src</code> and <code>dst</code> are the same
     * object, the input vector is correctly overwritten with the
     * transformed vector.
     * @param src the vector to be inverse transformed
     * @param dst the resulting transformed vector
     * @return <code>dst</code>, which contains the result of the
     * inverse transform.
     * @exception NoninvertibleTransformException  if the matrix cannot be
     *                                         inverted.
     * @since JavaFX 8.0
     */
    @Override
    public Vec3d inverseDeltaTransform(Vec3d src, Vec3d dst)
        throws NoninvertibleTransformException
    {
        if (dst == null) {
            dst = new Vec3d();
        }
        // Copy source coords into local variables in case src == dst
        double x = src.x;
        double y = src.y;
        double z = src.z;
        // assert(APPLY_3D was dealt with at a higher level)
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
            double det = mxx * myy - mxy * myx;
            if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            dst.set(((x * myy - y * mxy) / det), ((y * mxx - x * myx) / det), z);
            return dst;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            if (mxy == 0.0 || myx == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            dst.set((y / myx), (x / mxy), z);
            return dst;
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            if (mxx == 0.0 || myy == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            dst.set((x / mxx), (y / myy), z);
            return dst;
        case (APPLY_TRANSLATE):
        case (APPLY_IDENTITY):
            dst.set(x, y, z);
            return dst;
        }

        /* NOTREACHED */
    }

    private BaseBounds inversTransform2DBounds(RectBounds src, RectBounds dst)
        throws NoninvertibleTransformException
    {
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            /* NOBREAK */
        case (APPLY_SHEAR | APPLY_SCALE):
            double det = mxx * myy - mxy * myx;
            if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            double x1 = src.getMinX() - mxt;
            double y1 = src.getMinY() - myt;
            double x2 = src.getMaxX() - mxt;
            double y2 = src.getMaxY() - myt;
            dst.setBoundsAndSort((float) ((x1 * myy - y1 * mxy) / det),
                                (float) ((y1 * mxx - x1 * myx) / det),
                                (float) ((x2 * myy - y2 * mxy) / det),
                                (float) ((y2 * mxx - x2 * myx) / det));
            dst.add((float) ((x2 * myy - y1 * mxy) / det),
                    (float) ((y1 * mxx - x2 * myx) / det));
            dst.add((float) ((x1 * myy - y2 * mxy) / det),
                    (float) ((y2 * mxx - x1 * myx) / det));
            return dst;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            if (mxy == 0.0 || myx == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            dst.setBoundsAndSort((float) ((src.getMinY() - myt) / myx),
                                (float) ((src.getMinX() - mxt) / mxy),
                                (float) ((src.getMaxY() - myt) / myx),
                                (float) ((src.getMaxX() - mxt) / mxy));
            break;
        case (APPLY_SHEAR):
            if (mxy == 0.0 || myx == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            dst.setBoundsAndSort((float) (src.getMinY() / myx),
                                (float) (src.getMinX() / mxy),
                                (float) (src.getMaxY() / myx),
                                (float) (src.getMaxX() / mxy));
            break;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            if (mxx == 0.0 || myy == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            dst.setBoundsAndSort((float) ((src.getMinX() - mxt) / mxx),
                                (float) ((src.getMinY() - myt) / myy),
                                (float) ((src.getMaxX() - mxt) / mxx),
                                (float) ((src.getMaxY() - myt) / myy));
            break;
        case (APPLY_SCALE):
            if (mxx == 0.0 || myy == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            dst.setBoundsAndSort((float) (src.getMinX() / mxx),
                                (float) (src.getMinY() / myy),
                                (float) (src.getMaxX() / mxx),
                                (float) (src.getMaxY() / myy));
            break;
        case (APPLY_TRANSLATE):
            dst.setBounds((float) (src.getMinX() - mxt),
                          (float) (src.getMinY() - myt),
                          (float) (src.getMaxX() - mxt),
                          (float) (src.getMaxY() - myt));
            break;
        case (APPLY_IDENTITY):
            if (dst != src) {
                ((RectBounds) dst).setBounds((RectBounds) src);
            }
            break;
        }
        return dst;
    }

    // Note: Only use this method if src or dst is a 3D bounds
    private BaseBounds inversTransform3DBounds(BaseBounds src, BaseBounds dst)
            throws NoninvertibleTransformException
    {
        switch (state) {
            default:
                stateError();
            /* NOTREACHED */
            case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            /* NOBREAK */
            case (APPLY_SHEAR | APPLY_SCALE):
            /* NOBREAK */
            case (APPLY_SHEAR | APPLY_TRANSLATE):
            /* NOBREAK */
            case (APPLY_SHEAR):
                double det = mxx * myy - mxy * myx;
                if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                    throw new NoninvertibleTransformException("Determinant is "
                            + det);
                }
                double x1 = src.getMinX() - mxt;
                double y1 = src.getMinY() - myt;
                double z1 = src.getMinZ();
                double x2 = src.getMaxX() - mxt;
                double y2 = src.getMaxY() - myt;
                double z2 = src.getMaxZ();
                dst = dst.deriveWithNewBoundsAndSort(
                        (float) ((x1 * myy - y1 * mxy) / det),
                        (float) ((y1 * mxx - x1 * myx) / det),
                        (float) (z1 / det),
                        (float) ((x2 * myy - y2 * mxy) / det),
                        (float) ((y2 * mxx - x2 * myx) / det),
                        (float) (z2 / det));
                dst.add((float) ((x2 * myy - y1 * mxy) / det),
                        (float) ((y1 * mxx - x2 * myx) / det), 0);
                dst.add((float) ((x1 * myy - y2 * mxy) / det),
                        (float) ((y2 * mxx - x1 * myx) / det), 0);
                return dst;
            case (APPLY_SCALE | APPLY_TRANSLATE):
                if (mxx == 0.0 || myy == 0.0) {
                    throw new NoninvertibleTransformException("Determinant is 0");
                }
                dst = dst.deriveWithNewBoundsAndSort((float) ((src.getMinX() - mxt) / mxx),
                        (float) ((src.getMinY() - myt) / myy),
                        (float) src.getMinZ(),
                        (float) ((src.getMaxX() - mxt) / mxx),
                        (float) ((src.getMaxY() - myt) / myy),
                        (float) src.getMaxZ());
                break;
            case (APPLY_SCALE):
                if (mxx == 0.0 || myy == 0.0) {
                    throw new NoninvertibleTransformException("Determinant is 0");
                }
                dst = dst.deriveWithNewBoundsAndSort((float) (src.getMinX() / mxx),
                        (float) (src.getMinY() / myy),
                        (float) src.getMinZ(),
                        (float) (src.getMaxX() / mxx),
                                (float) (src.getMaxY() / myy),
                                (float) src.getMaxZ());
                break;
            case (APPLY_TRANSLATE):
                dst = dst.deriveWithNewBounds((float) (src.getMinX() - mxt),
                        (float) (src.getMinY() - myt),
                        (float) src.getMinZ(),
                        (float) (src.getMaxX() - mxt),
                        (float) (src.getMaxY() - myt),
                        (float) src.getMaxZ());
                break;
            case (APPLY_IDENTITY):
                if (dst != src) {
                    dst = dst.deriveWithNewBounds(src);
                }
                break;
        }
        return dst;
    }

    public BaseBounds inverseTransform(BaseBounds src, BaseBounds dst)
        throws NoninvertibleTransformException
    {
        // assert(APPLY_3D was dealt with at a higher level)
        if (src.getBoundsType() != BaseBounds.BoundsType.RECTANGLE ||
                dst.getBoundsType() != BaseBounds.BoundsType.RECTANGLE) {
            return inversTransform3DBounds(src, dst);
        }
        return inversTransform2DBounds((RectBounds)src, (RectBounds)dst);
    }

    public void inverseTransform(Rectangle src, Rectangle dst)
        throws NoninvertibleTransformException
    {
        // assert(APPLY_3D was dealt with at a higher level)
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            RectBounds b = new RectBounds(src);
            //TODO: Need to verify this casting is safe .... (RT-26885)
            b = (RectBounds) inverseTransform(b, b);
            dst.setBounds(b);
            return;
        case (APPLY_TRANSLATE):
            Translate2D.transform(src, dst, -mxt, -myt);
            return;
        case (APPLY_IDENTITY):
            if (dst != src) {
                dst.setBounds(src);
            }
            return;
        }
    }

    /**
     * Inverse transforms an array of single precision coordinates by
     * this transform.
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are
     * overwritten by a previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order <code>[x0, y0, x1, y1, ..., xn, yn]</code>.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed point
     * coordinates are returned.  Each point is stored as a pair of
     * x,&nbsp;y coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of point objects to be transformed
     * @exception NoninvertibleTransformException  if the matrix cannot be
     *                                         inverted.
     */
    public void inverseTransform(float[] srcPts, int srcOff,
                                 float[] dstPts, int dstOff,
                                 int numPts)
        throws NoninvertibleTransformException
    {
        doInverseTransform(srcPts, srcOff, dstPts, dstOff, numPts, state);
    }

    /**
     * Inverse transforms an array of single precision relative coordinates by
     * this transform.
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are
     * overwritten by a previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order <code>[x0, y0, x1, y1, ..., xn, yn]</code>.
     * @param srcPts the array containing the relative source coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the relative transformed point
     * coordinates are returned.  Each point is stored as a pair of
     * x,&nbsp;y coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of point objects to be transformed
     * @exception NoninvertibleTransformException  if the matrix cannot be
     *                                         inverted.
     */
    public void inverseDeltaTransform(float[] srcPts, int srcOff,
                                      float[] dstPts, int dstOff,
                                      int numPts)
        throws NoninvertibleTransformException
    {
        doInverseTransform(srcPts, srcOff, dstPts, dstOff, numPts,
                           state & ~APPLY_TRANSLATE);
    }

    /**
     * Inverse transforms an array of single precision coordinates by
     * this transform using the specified state type.
     */
    private void doInverseTransform(float[] srcPts, int srcOff,
                                    float[] dstPts, int dstOff,
                                    int numPts, int thestate)
        throws NoninvertibleTransformException
    {
        double Mxx, Mxy, Mxt, Myx, Myy, Myt;    // For caching
        double det;
        if (dstPts == srcPts &&
            dstOff > srcOff && dstOff < srcOff + numPts * 2)
        {
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
        // assert(APPLY_3D was dealt with at a higher level)
        switch (thestate) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxy = mxy; Mxt = mxt;
            Myx = myx; Myy = myy; Myt = myt;
            det = Mxx * Myy - Mxy * Myx;
            if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++] - Mxt;
                double y = srcPts[srcOff++] - Myt;
                dstPts[dstOff++] = (float) ((x * Myy - y * Mxy) / det);
                dstPts[dstOff++] = (float) ((y * Mxx - x * Myx) / det);
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            Mxx = mxx; Mxy = mxy;
            Myx = myx; Myy = myy;
            det = Mxx * Myy - Mxy * Myx;
            if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = (float) ((x * Myy - y * Mxy) / det);
                dstPts[dstOff++] = (float) ((y * Mxx - x * Myx) / det);
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            Mxy = mxy; Mxt = mxt;
            Myx = myx; Myt = myt;
            if (Mxy == 0.0 || Myx == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++] - Mxt;
                dstPts[dstOff++] = (float) ((srcPts[srcOff++] - Myt) / Myx);
                dstPts[dstOff++] = (float) (x / Mxy);
            }
            return;
        case (APPLY_SHEAR):
            Mxy = mxy; Myx = myx;
            if (Mxy == 0.0 || Myx == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (srcPts[srcOff++] / Myx);
                dstPts[dstOff++] = (float) (x / Mxy);
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxt = mxt;
            Myy = myy; Myt = myt;
            if (Mxx == 0.0 || Myy == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) ((srcPts[srcOff++] - Mxt) / Mxx);
                dstPts[dstOff++] = (float) ((srcPts[srcOff++] - Myt) / Myy);
            }
            return;
        case (APPLY_SCALE):
            Mxx = mxx; Myy = myy;
            if (Mxx == 0.0 || Myy == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++] / Mxx);
                dstPts[dstOff++] = (float) (srcPts[srcOff++] / Myy);
            }
            return;
        case (APPLY_TRANSLATE):
            Mxt = mxt; Myt = myt;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++] - Mxt);
                dstPts[dstOff++] = (float) (srcPts[srcOff++] - Myt);
            }
            return;
        case (APPLY_IDENTITY):
            if (srcPts != dstPts || srcOff != dstOff) {
                System.arraycopy(srcPts, srcOff, dstPts, dstOff,
                                 numPts * 2);
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Inverse transforms an array of double precision coordinates by
     * this transform.
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are
     * overwritten by a previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order <code>[x0, y0, x1, y1, ..., xn, yn]</code>.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed point
     * coordinates are returned.  Each point is stored as a pair of
     * x,&nbsp;y coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of point objects to be transformed
     * @exception NoninvertibleTransformException  if the matrix cannot be
     *                                         inverted.
     */
    public void inverseTransform(double[] srcPts, int srcOff,
                                 double[] dstPts, int dstOff,
                                 int numPts)
        throws NoninvertibleTransformException
    {
        double Mxx, Mxy, Mxt, Myx, Myy, Myt;    // For caching
        double det;
        if (dstPts == srcPts &&
            dstOff > srcOff && dstOff < srcOff + numPts * 2)
        {
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
        // assert(APPLY_3D was dealt with at a higher level)
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxy = mxy; Mxt = mxt;
            Myx = myx; Myy = myy; Myt = myt;
            det = Mxx * Myy - Mxy * Myx;
            if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++] - Mxt;
                double y = srcPts[srcOff++] - Myt;
                dstPts[dstOff++] = (x * Myy - y * Mxy) / det;
                dstPts[dstOff++] = (y * Mxx - x * Myx) / det;
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            Mxx = mxx; Mxy = mxy;
            Myx = myx; Myy = myy;
            det = Mxx * Myy - Mxy * Myx;
            if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = (x * Myy - y * Mxy) / det;
                dstPts[dstOff++] = (y * Mxx - x * Myx) / det;
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            Mxy = mxy; Mxt = mxt;
            Myx = myx; Myt = myt;
            if (Mxy == 0.0 || Myx == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++] - Mxt;
                dstPts[dstOff++] = (srcPts[srcOff++] - Myt) / Myx;
                dstPts[dstOff++] = x / Mxy;
            }
            return;
        case (APPLY_SHEAR):
            Mxy = mxy; Myx = myx;
            if (Mxy == 0.0 || Myx == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = srcPts[srcOff++] / Myx;
                dstPts[dstOff++] = x / Mxy;
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxt = mxt;
            Myy = myy; Myt = myt;
            if (Mxx == 0.0 || Myy == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                dstPts[dstOff++] = (srcPts[srcOff++] - Mxt) / Mxx;
                dstPts[dstOff++] = (srcPts[srcOff++] - Myt) / Myy;
            }
            return;
        case (APPLY_SCALE):
            Mxx = mxx; Myy = myy;
            if (Mxx == 0.0 || Myy == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                dstPts[dstOff++] = srcPts[srcOff++] / Mxx;
                dstPts[dstOff++] = srcPts[srcOff++] / Myy;
            }
            return;
        case (APPLY_TRANSLATE):
            Mxt = mxt; Myt = myt;
            while (--numPts >= 0) {
                dstPts[dstOff++] = srcPts[srcOff++] - Mxt;
                dstPts[dstOff++] = srcPts[srcOff++] - Myt;
            }
            return;
        case (APPLY_IDENTITY):
            if (srcPts != dstPts || srcOff != dstOff) {
                System.arraycopy(srcPts, srcOff, dstPts, dstOff,
                                 numPts * 2);
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Returns a new {@link Shape} object defined by the geometry of the
     * specified <code>Shape</code> after it has been transformed by
     * this transform.
     * @param pSrc the specified <code>Shape</code> object to be
     * transformed by this transform.
     * @return a new <code>Shape</code> object that defines the geometry
     * of the transformed <code>Shape</code>, or null if {@code pSrc} is null.
     */
    public Shape createTransformedShape(Shape s) {
        if (s == null) {
            return null;
        }
        return new Path2D(s, this);
    }

    /**
     * Concatenates this transform with a translation transformation.
     * This is equivalent to calling concatenate(T), where T is an
     * <code>Affine2D</code> represented by the following matrix:
     * <pre>
     *      [   1    0    tx  ]
     *      [   0    1    ty  ]
     *      [   0    0    1   ]
     * </pre>
     * @param tx the distance by which coordinates are translated in the
     * X axis direction
     * @param ty the distance by which coordinates are translated in the
     * Y axis direction
     */
    public void translate(double tx, double ty) {
        // assert(APPLY_3D was dealt with at a higher level)
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            mxt = tx * mxx + ty * mxy + mxt;
            myt = tx * myx + ty * myy + myt;
            if (mxt == 0.0 && myt == 0.0) {
                state = APPLY_SHEAR | APPLY_SCALE;
                if (type != TYPE_UNKNOWN) {
                    type &= ~TYPE_TRANSLATION;
                }
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            mxt = tx * mxx + ty * mxy;
            myt = tx * myx + ty * myy;
            if (mxt != 0.0 || myt != 0.0) {
                state = APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE;
                type |= TYPE_TRANSLATION;
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            mxt = ty * mxy + mxt;
            myt = tx * myx + myt;
            if (mxt == 0.0 && myt == 0.0) {
                state = APPLY_SHEAR;
                if (type != TYPE_UNKNOWN) {
                    type &= ~TYPE_TRANSLATION;
                }
            }
            return;
        case (APPLY_SHEAR):
            mxt = ty * mxy;
            myt = tx * myx;
            if (mxt != 0.0 || myt != 0.0) {
                state = APPLY_SHEAR | APPLY_TRANSLATE;
                type |= TYPE_TRANSLATION;
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            mxt = tx * mxx + mxt;
            myt = ty * myy + myt;
            if (mxt == 0.0 && myt == 0.0) {
                state = APPLY_SCALE;
                if (type != TYPE_UNKNOWN) {
                    type &= ~TYPE_TRANSLATION;
                }
            }
            return;
        case (APPLY_SCALE):
            mxt = tx * mxx;
            myt = ty * myy;
            if (mxt != 0.0 || myt != 0.0) {
                state = APPLY_SCALE | APPLY_TRANSLATE;
                type |= TYPE_TRANSLATION;
            }
            return;
        case (APPLY_TRANSLATE):
            mxt = tx + mxt;
            myt = ty + myt;
            if (mxt == 0.0 && myt == 0.0) {
                state = APPLY_IDENTITY;
                type = TYPE_IDENTITY;
            }
            return;
        case (APPLY_IDENTITY):
            mxt = tx;
            myt = ty;
            if (tx != 0.0 || ty != 0.0) {
                state = APPLY_TRANSLATE;
                type = TYPE_TRANSLATION;
            }
            return;
        }
    }

    // Utility methods to optimize rotate methods.
    // These tables translate the flags during predictable quadrant
    // rotations where the shear and scale values are swapped and negated.
    private static final int rot90conversion[] = {
        /* IDENTITY => */        APPLY_SHEAR,
        /* TRANSLATE (TR) => */  APPLY_SHEAR | APPLY_TRANSLATE,
        /* SCALE (SC) => */      APPLY_SHEAR,
        /* SC | TR => */         APPLY_SHEAR | APPLY_TRANSLATE,
        /* SHEAR (SH) => */      APPLY_SCALE,
        /* SH | TR => */         APPLY_SCALE | APPLY_TRANSLATE,
        /* SH | SC => */         APPLY_SHEAR | APPLY_SCALE,
        /* SH | SC | TR => */    APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE,
    };
    protected final void rotate90() {
        double M0 = mxx;
        mxx = mxy;
        mxy = -M0;
        M0 = myx;
        myx = myy;
        myy = -M0;
        int newstate = rot90conversion[this.state];
        if ((newstate & (APPLY_SHEAR | APPLY_SCALE)) == APPLY_SCALE &&
            mxx == 1.0 && myy == 1.0)
        {
            newstate -= APPLY_SCALE;
        }
        this.state = newstate;
        type = TYPE_UNKNOWN;
    }
    protected final void rotate180() {
        mxx = -mxx;
        myy = -myy;
        int oldstate = this.state;
        if ((oldstate & (APPLY_SHEAR)) != 0) {
            // If there was a shear, then this rotation has no
            // effect on the state.
            mxy = -mxy;
            myx = -myx;
        } else {
            // No shear means the SCALE state may toggle when
            // m00 and m11 are negated.
            if (mxx == 1.0 && myy == 1.0) {
                this.state = oldstate & ~APPLY_SCALE;
            } else {
                this.state = oldstate | APPLY_SCALE;
            }
        }
        type = TYPE_UNKNOWN;
    }
    protected final void rotate270() {
        double M0 = mxx;
        mxx = -mxy;
        mxy = M0;
        M0 = myx;
        myx = -myy;
        myy = M0;
        int newstate = rot90conversion[this.state];
        if ((newstate & (APPLY_SHEAR | APPLY_SCALE)) == APPLY_SCALE &&
            mxx == 1.0 && myy == 1.0)
        {
            newstate -= APPLY_SCALE;
        }
        this.state = newstate;
        type = TYPE_UNKNOWN;
    }

    /**
     * Concatenates this transform with a rotation transformation.
     * This is equivalent to calling concatenate(R), where R is an
     * <code>Affine2D</code> represented by the following matrix:
     * <pre>
     *      [   cos(theta)    -sin(theta)    0   ]
     *      [   sin(theta)     cos(theta)    0   ]
     *      [       0              0         1   ]
     * </pre>
     * Rotating by a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     * Note also the discussion of
     * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
     * above.
     * @param theta the angle of rotation measured in radians
     */
    public void rotate(double theta) {
        // assert(APPLY_3D was dealt with at a higher level)
        double sin = Math.sin(theta);
        if (sin == 1.0) {
            rotate90();
        } else if (sin == -1.0) {
            rotate270();
        } else {
            double cos = Math.cos(theta);
            if (cos == -1.0) {
                rotate180();
            } else if (cos != 1.0) {
                double M0, M1;
                M0 = mxx;
                M1 = mxy;
                mxx =  cos * M0 + sin * M1;
                mxy = -sin * M0 + cos * M1;
                M0 = myx;
                M1 = myy;
                myx =  cos * M0 + sin * M1;
                myy = -sin * M0 + cos * M1;
                updateState2D();
            }
        }
    }

    /**
     * Concatenates this transform with a scaling transformation.
     * This is equivalent to calling concatenate(S), where S is an
     * <code>Affine2D</code> represented by the following matrix:
     * <pre>
     *      [   sx   0    0   ]
     *      [   0    sy   0   ]
     *      [   0    0    1   ]
     * </pre>
     * @param sx the factor by which coordinates are scaled along the
     * X axis direction
     * @param sy the factor by which coordinates are scaled along the
     * Y axis direction
     */
    public void scale(double sx, double sy) {
        int mystate = this.state;
        // assert(APPLY_3D was dealt with at a higher level)
        switch (mystate) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
            mxx *= sx;
            myy *= sy;
            /* NOBREAK */
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            mxy *= sy;
            myx *= sx;
            if (mxy == 0 && myx == 0) {
                mystate &= APPLY_TRANSLATE;
                if (mxx == 1.0 && myy == 1.0) {
                    this.type = (mystate == APPLY_IDENTITY
                                 ? TYPE_IDENTITY
                                 : TYPE_TRANSLATION);
                } else {
                    mystate |= APPLY_SCALE;
                    this.type = TYPE_UNKNOWN;
                }
                this.state = mystate;
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            mxx *= sx;
            myy *= sy;
            if (mxx == 1.0 && myy == 1.0) {
                this.state = (mystate &= APPLY_TRANSLATE);
                this.type = (mystate == APPLY_IDENTITY
                             ? TYPE_IDENTITY
                             : TYPE_TRANSLATION);
            } else {
                this.type = TYPE_UNKNOWN;
            }
            return;
        case (APPLY_TRANSLATE):
        case (APPLY_IDENTITY):
            mxx = sx;
            myy = sy;
            if (sx != 1.0 || sy != 1.0) {
                this.state = mystate | APPLY_SCALE;
                this.type = TYPE_UNKNOWN;
            }
            return;
        }
    }

    /**
     * Concatenates this transform with a shearing transformation.
     * This is equivalent to calling concatenate(SH), where SH is an
     * <code>Affine2D</code> represented by the following matrix:
     * <pre>
     *      [   1   shx   0   ]
     *      [  shy   1    0   ]
     *      [   0    0    1   ]
     * </pre>
     * @param shx the multiplier by which coordinates are shifted in the
     * direction of the positive X axis as a factor of their Y coordinate
     * @param shy the multiplier by which coordinates are shifted in the
     * direction of the positive Y axis as a factor of their X coordinate
     */
    public void shear(double shx, double shy) {
        int mystate = this.state;
        // assert(APPLY_3D was dealt with at a higher level)
        switch (mystate) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
            double M0, M1;
            M0 = mxx;
            M1 = mxy;
            mxx = M0 + M1 * shy;
            mxy = M0 * shx + M1;

            M0 = myx;
            M1 = myy;
            myx = M0 + M1 * shy;
            myy = M0 * shx + M1;
            updateState2D();
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            mxx = mxy * shy;
            myy = myx * shx;
            if (mxx != 0.0 || myy != 0.0) {
                this.state = mystate | APPLY_SCALE;
            }
            this.type = TYPE_UNKNOWN;
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            mxy = mxx * shx;
            myx = myy * shy;
            if (mxy != 0.0 || myx != 0.0) {
                this.state = mystate | APPLY_SHEAR;
            }
            this.type = TYPE_UNKNOWN;
            return;
        case (APPLY_TRANSLATE):
        case (APPLY_IDENTITY):
            mxy = shx;
            myx = shy;
            if (mxy != 0.0 || myx != 0.0) {
                this.state = mystate | APPLY_SCALE | APPLY_SHEAR;
                this.type = TYPE_UNKNOWN;
            }
            return;
        }
    }

    /**
     * Concatenates a <code>BaseTransform</code> <code>Tx</code> to
     * this <code>Affine2D</code> Cx in the most commonly useful
     * way to provide a new user space
     * that is mapped to the former user space by <code>Tx</code>.
     * Cx is updated to perform the combined transformation.
     * Transforming a point p by the updated transform Cx' is
     * equivalent to first transforming p by <code>Tx</code> and then
     * transforming the result by the original transform Cx like this:
     * Cx'(p) = Cx(Tx(p))
     * In matrix notation, if this transform Cx is
     * represented by the matrix [this] and <code>Tx</code> is represented
     * by the matrix [Tx] then this method does the following:
     * <pre>
     *      [this] = [this] x [Tx]
     * </pre>
     * @param Tx the <code>BaseTransform</code> object to be
     * concatenated with this <code>Affine2D</code> object.
     * @see #preConcatenate
     */
    public void concatenate(BaseTransform Tx) {
        switch (Tx.getDegree()) {
            case IDENTITY:
                return;
            case TRANSLATE_2D:
                translate(Tx.getMxt(), Tx.getMyt());
                return;
            case AFFINE_2D:
                break;
            default:
                if (!Tx.is2D()) {
                    degreeError(Degree.AFFINE_2D);
                }
                // TODO: Optimize - we need an AffineBase below due to the cast
                // For now, there is no other kind of transform that will get
                // here so we are already essentially optimal, but if we have
                // a different type of transform that reaches here we should
                // try to avoid this garbage... (RT-26884)
                if (!(Tx instanceof AffineBase)) {
                    Tx = new Affine2D(Tx);
                }
                break;
        }
        double M0, M1;
        double Txx, Txy, Tyx, Tyy;
        double Txt, Tyt;
        int mystate = state;
        AffineBase at = (AffineBase) Tx;
        int txstate = at.state;
        switch ((txstate << HI_SHIFT) | mystate) {

            /* ---------- Tx == IDENTITY cases ---------- */
        case (HI_IDENTITY | APPLY_IDENTITY):
        case (HI_IDENTITY | APPLY_TRANSLATE):
        case (HI_IDENTITY | APPLY_SCALE):
        case (HI_IDENTITY | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_IDENTITY | APPLY_SHEAR):
        case (HI_IDENTITY | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_IDENTITY | APPLY_SHEAR | APPLY_SCALE):
        case (HI_IDENTITY | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            return;

            /* ---------- this == IDENTITY cases ---------- */
        case (HI_SHEAR | HI_SCALE | HI_TRANSLATE | APPLY_IDENTITY):
            mxy = at.mxy;
            myx = at.myx;
            /* NOBREAK */
        case (HI_SCALE | HI_TRANSLATE | APPLY_IDENTITY):
            mxx = at.mxx;
            myy = at.myy;
            /* NOBREAK */
        case (HI_TRANSLATE | APPLY_IDENTITY):
            mxt = at.mxt;
            myt = at.myt;
            state = txstate;
            type = at.type;
            return;
        case (HI_SHEAR | HI_SCALE | APPLY_IDENTITY):
            mxy = at.mxy;
            myx = at.myx;
            /* NOBREAK */
        case (HI_SCALE | APPLY_IDENTITY):
            mxx = at.mxx;
            myy = at.myy;
            state = txstate;
            type = at.type;
            return;
        case (HI_SHEAR | HI_TRANSLATE | APPLY_IDENTITY):
            mxt = at.mxt;
            myt = at.myt;
            /* NOBREAK */
        case (HI_SHEAR | APPLY_IDENTITY):
            mxy = at.mxy;
            myx = at.myx;
            mxx = myy = 0.0;
            state = txstate;
            type = at.type;
            return;

            /* ---------- Tx == TRANSLATE cases ---------- */
        case (HI_TRANSLATE | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_TRANSLATE | APPLY_SHEAR | APPLY_SCALE):
        case (HI_TRANSLATE | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_TRANSLATE | APPLY_SHEAR):
        case (HI_TRANSLATE | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_TRANSLATE | APPLY_SCALE):
        case (HI_TRANSLATE | APPLY_TRANSLATE):
            translate(at.mxt, at.myt);
            return;

            /* ---------- Tx == SCALE cases ---------- */
        case (HI_SCALE | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SCALE | APPLY_SHEAR | APPLY_SCALE):
        case (HI_SCALE | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_SCALE | APPLY_SHEAR):
        case (HI_SCALE | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SCALE | APPLY_SCALE):
        case (HI_SCALE | APPLY_TRANSLATE):
            scale(at.mxx, at.myy);
            return;

            /* ---------- Tx == SHEAR cases ---------- */
        case (HI_SHEAR | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SHEAR | APPLY_SHEAR | APPLY_SCALE):
            Txy = at.mxy; Tyx = at.myx;
            M0 = mxx;
            mxx = mxy * Tyx;
            mxy = M0 * Txy;
            M0 = myx;
            myx = myy * Tyx;
            myy = M0 * Txy;
            type = TYPE_UNKNOWN;
            return;
        case (HI_SHEAR | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_SHEAR | APPLY_SHEAR):
            mxx = mxy * at.myx;
            mxy = 0.0;
            myy = myx * at.mxy;
            myx = 0.0;
            state = mystate ^ (APPLY_SHEAR | APPLY_SCALE);
            type = TYPE_UNKNOWN;
            return;
        case (HI_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SHEAR | APPLY_SCALE):
            mxy = mxx * at.mxy;
            mxx = 0.0;
            myx = myy * at.myx;
            myy = 0.0;
            state = mystate ^ (APPLY_SHEAR | APPLY_SCALE);
            type = TYPE_UNKNOWN;
            return;
        case (HI_SHEAR | APPLY_TRANSLATE):
            mxx = 0.0;
            mxy = at.mxy;
            myx = at.myx;
            myy = 0.0;
            state = APPLY_TRANSLATE | APPLY_SHEAR;
            type = TYPE_UNKNOWN;
            return;
        }
        // If Tx has more than one attribute, it is not worth optimizing
        // all of those cases...
        Txx = at.mxx; Txy = at.mxy; Txt = at.mxt;
        Tyx = at.myx; Tyy = at.myy; Tyt = at.myt;
        switch (mystate) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE):
            state = mystate | txstate;
            /* NOBREAK */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            M0 = mxx;
            M1 = mxy;
            mxx  = Txx * M0 + Tyx * M1;
            mxy  = Txy * M0 + Tyy * M1;
            mxt += Txt * M0 + Tyt * M1;

            M0 = myx;
            M1 = myy;
            myx  = Txx * M0 + Tyx * M1;
            myy  = Txy * M0 + Tyy * M1;
            myt += Txt * M0 + Tyt * M1;
            type = TYPE_UNKNOWN;
            return;

        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            M0 = mxy;
            mxx  = Tyx * M0;
            mxy  = Tyy * M0;
            mxt += Tyt * M0;

            M0 = myx;
            myx  = Txx * M0;
            myy  = Txy * M0;
            myt += Txt * M0;
            break;

        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            M0 = mxx;
            mxx  = Txx * M0;
            mxy  = Txy * M0;
            mxt += Txt * M0;

            M0 = myy;
            myx  = Tyx * M0;
            myy  = Tyy * M0;
            myt += Tyt * M0;
            break;

        case (APPLY_TRANSLATE):
            mxx  = Txx;
            mxy  = Txy;
            mxt += Txt;

            myx  = Tyx;
            myy  = Tyy;
            myt += Tyt;
            state = txstate | APPLY_TRANSLATE;
            type = TYPE_UNKNOWN;
            return;
        }
        updateState2D();
    }

    /**
     * Similar to {@link #concatenate(com.sun.javafx.geom.transform.BaseTransform)},
     * passing the individual elements of the transformation.
     */
    public void concatenate(double Txx, double Txy, double Txt,
                            double Tyx, double Tyy, double Tyt)
    {
        double rxx = (mxx * Txx + mxy * Tyx /* + mxt * 0.0 */);
        double rxy = (mxx * Txy + mxy * Tyy /* + mxt * 0.0 */);
        double rxt = (mxx * Txt + mxy * Tyt + mxt /* * 1.0 */);
        double ryx = (myx * Txx + myy * Tyx /* + myt * 0.0 */);
        double ryy = (myx * Txy + myy * Tyy /* + myt * 0.0 */);
        double ryt = (myx * Txt + myy * Tyt + myt /* * 1.0 */);
        this.mxx = rxx;
        this.mxy = rxy;
        this.mxt = rxt;
        this.myx = ryx;
        this.myy = ryy;
        this.myt = ryt;
        updateState();
    }

    /**
     * Sets this transform to the inverse of itself.
     * The inverse transform Tx' of this transform Tx
     * maps coordinates transformed by Tx back
     * to their original coordinates.
     * In other words, Tx'(Tx(p)) = p = Tx(Tx'(p)).
     * <p>
     * If this transform maps all coordinates onto a point or a line
     * then it will not have an inverse, since coordinates that do
     * not lie on the destination point or line will not have an inverse
     * mapping.
     * The <code>getDeterminant</code> method can be used to determine if this
     * transform has no inverse, in which case an exception will be
     * thrown if the <code>invert</code> method is called.
     * @see #getDeterminant
     * @exception NoninvertibleTransformException
     * if the matrix cannot be inverted.
     */
    public void invert()
        throws NoninvertibleTransformException
    {
        double Mxx, Mxy, Mxt;
        double Myx, Myy, Myt;
        double det;
        // assert(APPLY_3D was dealt with at a higher level)
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxy = mxy; Mxt = mxt;
            Myx = myx; Myy = myy; Myt = myt;
            det = Mxx * Myy - Mxy * Myx;
            if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            mxx =  Myy / det;
            myx = -Myx / det;
            mxy = -Mxy / det;
            myy =  Mxx / det;
            mxt = (Mxy * Myt - Myy * Mxt) / det;
            myt = (Myx * Mxt - Mxx * Myt) / det;
            break;
        case (APPLY_SHEAR | APPLY_SCALE):
            Mxx = mxx; Mxy = mxy;
            Myx = myx; Myy = myy;
            det = Mxx * Myy - Mxy * Myx;
            if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            mxx =  Myy / det;
            myx = -Myx / det;
            mxy = -Mxy / det;
            myy =  Mxx / det;
            // m02 = 0.0;
            // m12 = 0.0;
            break;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            Mxy = mxy; Mxt = mxt;
            Myx = myx; Myt = myt;
            if (Mxy == 0.0 || Myx == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            // m00 = 0.0;
            myx = 1.0 / Mxy;
            mxy = 1.0 / Myx;
            // m11 = 0.0;
            mxt = -Myt / Myx;
            myt = -Mxt / Mxy;
            break;
        case (APPLY_SHEAR):
            Mxy = mxy;
            Myx = myx;
            if (Mxy == 0.0 || Myx == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            // m00 = 0.0;
            myx = 1.0 / Mxy;
            mxy = 1.0 / Myx;
            // m11 = 0.0;
            // m02 = 0.0;
            // m12 = 0.0;
            break;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            Mxx = mxx; Mxt = mxt;
            Myy = myy; Myt = myt;
            if (Mxx == 0.0 || Myy == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            mxx = 1.0 / Mxx;
            // m10 = 0.0;
            // m01 = 0.0;
            myy = 1.0 / Myy;
            mxt = -Mxt / Mxx;
            myt = -Myt / Myy;
            break;
        case (APPLY_SCALE):
            Mxx = mxx;
            Myy = myy;
            if (Mxx == 0.0 || Myy == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            mxx = 1.0 / Mxx;
            // m10 = 0.0;
            // m01 = 0.0;
            myy = 1.0 / Myy;
            // m02 = 0.0;
            // m12 = 0.0;
            break;
        case (APPLY_TRANSLATE):
            // m00 = 1.0;
            // m10 = 0.0;
            // m01 = 0.0;
            // m11 = 1.0;
            mxt = -mxt;
            myt = -myt;
            break;
        case (APPLY_IDENTITY):
            // m00 = 1.0;
            // m10 = 0.0;
            // m01 = 0.0;
            // m11 = 1.0;
            // m02 = 0.0;
            // m12 = 0.0;
            break;
        }
    }

}
