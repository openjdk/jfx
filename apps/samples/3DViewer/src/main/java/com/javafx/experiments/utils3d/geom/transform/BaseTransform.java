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

import com.javafx.experiments.utils3d.geom.Vec3d;
import com.javafx.experiments.utils3d.geom.BaseBounds;
import com.javafx.experiments.utils3d.geom.Point2D;
import com.javafx.experiments.utils3d.geom.Rectangle;

public abstract class BaseTransform implements CanTransformVec3d {
    public static final BaseTransform IDENTITY_TRANSFORM = new Identity();

    public static enum Degree {
        IDENTITY,
        TRANSLATE_2D,
        AFFINE_2D,
        TRANSLATE_3D,
        AFFINE_3D,
    }

    /*
     * This constant is only useful for a cached type field.
     * It indicates that the type has been decached and must be recalculated.
     */
    protected static final int TYPE_UNKNOWN = -1;

    /**
     * This constant indicates that the transform defined by this object
     * is an identity transform.
     * An identity transform is one in which the output coordinates are
     * always the same as the input coordinates.
     * If this transform is anything other than the identity transform,
     * the type will either be the constant GENERAL_TRANSFORM or a
     * combination of the appropriate flag bits for the various coordinate
     * conversions that this transform performs.
     *
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     */
    public static final int TYPE_IDENTITY = 0;

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a translation in addition to the conversions indicated
     * by other flag bits.
     * A translation moves the coordinates by a constant amount in x
     * and y without changing the length or angle of vectors.
     *
     * @see #TYPE_IDENTITY
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     */
    public static final int TYPE_TRANSLATION = 1;

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a uniform scale in addition to the conversions indicated
     * by other flag bits.
     * A uniform scale multiplies the length of vectors by the same amount
     * in both the x and y directions without changing the angle between
     * vectors.
     * This flag bit is mutually exclusive with the TYPE_GENERAL_SCALE flag.
     *
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     */
    public static final int TYPE_UNIFORM_SCALE = 2;

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a general scale in addition to the conversions indicated
     * by other flag bits.
     * A general scale multiplies the length of vectors by different
     * amounts in the x and y directions without changing the angle
     * between perpendicular vectors.
     * This flag bit is mutually exclusive with the TYPE_UNIFORM_SCALE flag.
     *
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     */
    public static final int TYPE_GENERAL_SCALE = 4;

    /**
     * This constant is a bit mask for any of the scale flag bits.
     *
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     */
    public static final int TYPE_MASK_SCALE = (TYPE_UNIFORM_SCALE |
            TYPE_GENERAL_SCALE);

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a mirror image flip about some axis which changes the
     * normally right handed coordinate system into a left handed
     * system in addition to the conversions indicated by other flag bits.
     * A right handed coordinate system is one where the positive X
     * axis rotates counterclockwise to overlay the positive Y axis
     * similar to the direction that the fingers on your right hand
     * curl when you stare end on at your thumb.
     * A left handed coordinate system is one where the positive X
     * axis rotates clockwise to overlay the positive Y axis similar
     * to the direction that the fingers on your left hand curl.
     * There is no mathematical way to determine the angle of the
     * original flipping or mirroring transformation since all angles
     * of flip are identical given an appropriate adjusting rotation.
     *
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     */
    public static final int TYPE_FLIP = 64;
    /* NOTE: TYPE_FLIP was added after GENERAL_TRANSFORM was in public
     * circulation and the flag bits could no longer be conveniently
     * renumbered without introducing binary incompatibility in outside
     * code.
     */

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a quadrant rotation by some multiple of 90 degrees in
     * addition to the conversions indicated by other flag bits.
     * A rotation changes the angles of vectors by the same amount
     * regardless of the original direction of the vector and without
     * changing the length of the vector.
     * This flag bit is mutually exclusive with the TYPE_GENERAL_ROTATION flag.
     *
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     */
    public static final int TYPE_QUADRANT_ROTATION = 8;

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a rotation by an arbitrary angle in addition to the
     * conversions indicated by other flag bits.
     * A rotation changes the angles of vectors by the same amount
     * regardless of the original direction of the vector and without
     * changing the length of the vector.
     * This flag bit is mutually exclusive with the
     * TYPE_QUADRANT_ROTATION flag.
     *
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     */
    public static final int TYPE_GENERAL_ROTATION = 16;

    /**
     * This constant is a bit mask for any of the rotation flag bits.
     *
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     */
    public static final int TYPE_MASK_ROTATION = (TYPE_QUADRANT_ROTATION |
            TYPE_GENERAL_ROTATION);

    /**
     * This constant indicates that the transform defined by this object
     * performs an arbitrary conversion of the input coordinates.
     * If this transform can be classified by any of the above constants,
     * the type will either be the constant TYPE_IDENTITY or a
     * combination of the appropriate flag bits for the various coordinate
     * conversions that this transform performs.
     *
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #getType
     */
    public static final int TYPE_GENERAL_TRANSFORM = 32;

    public static final int TYPE_AFFINE2D_MASK =
            (TYPE_TRANSLATION |
                    TYPE_UNIFORM_SCALE |
                    TYPE_GENERAL_SCALE |
                    TYPE_QUADRANT_ROTATION |
                    TYPE_GENERAL_ROTATION |
                    TYPE_GENERAL_TRANSFORM |
                    TYPE_FLIP);

    public static final int TYPE_AFFINE_3D = 128;

    /*
     * Convenience method used internally to throw exceptions when
     * an operation of degree higher than AFFINE_2D is attempted.
     */
    static void degreeError(Degree maxSupported) {
        throw new InternalError("does not support higher than " +
                maxSupported + " operations");
    }

    public static BaseTransform getInstance(BaseTransform tx) {
        if (tx.isIdentity()) {
            return IDENTITY_TRANSFORM;
        } else if (tx.isTranslateOrIdentity()) {
            return new Translate2D(tx);
        } else if (tx.is2D()) {
            return new Affine2D(tx);
        }
        return new Affine3D(tx);
    }

    public static BaseTransform getInstance(double mxx, double mxy, double mxz, double mxt,
                                            double myx, double myy, double myz, double myt,
                                            double mzx, double mzy, double mzz, double mzt) {
        if (mxz == 0.0 && myz == 0.0 &&
                mzx == 0.0 && mzy == 0.0 && mzz == 1.0 && mzt == 0.0) {
            return getInstance(mxx, myx, mxy, myy, mxt, myt);
        } else {
            return new Affine3D(mxx, mxy, mxz, mxt,
                    myx, myy, myz, myt,
                    mzx, mzy, mzz, mzt);
        }
    }

    public static BaseTransform getInstance(double mxx, double myx,
                                            double mxy, double myy,
                                            double mxt, double myt) {
        if (mxx == 1.0 && myx == 0.0 && mxy == 0.0 && myy == 1.0) {
            return getTranslateInstance(mxt, myt);
        } else {
            return new Affine2D(mxx, myx, mxy, myy, mxt, myt);
        }
    }

    public static BaseTransform getTranslateInstance(double mxt, double myt) {
        if (mxt == 0.0 && myt == 0.0) {
            return IDENTITY_TRANSFORM;
        } else {
            return new Translate2D(mxt, myt);
        }
    }

    public static BaseTransform getScaleInstance(double mxx, double myy) {
        return getInstance(mxx, 0, 0, myy, 0, 0);
    }

    public static BaseTransform getRotateInstance(double theta, double x, double y) {
        Affine2D a = new Affine2D();
        a.setToRotation(theta, x, y);
        return a;
    }

    public abstract Degree getDegree();

    /**
     * Retrieves the flag bits describing the conversion properties of
     * this transform.
     * The return value is either one of the constants TYPE_IDENTITY
     * or TYPE_GENERAL_TRANSFORM, or a combination of the
     * appriopriate flag bits.
     * A valid combination of flag bits is an exclusive OR operation
     * that can combine
     * the TYPE_TRANSLATION flag bit
     * in addition to either of the
     * TYPE_UNIFORM_SCALE or TYPE_GENERAL_SCALE flag bits
     * as well as either of the
     * TYPE_QUADRANT_ROTATION or TYPE_GENERAL_ROTATION flag bits.
     *
     * @return the OR combination of any of the indicated flags that
     * apply to this transform
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     */
    public abstract int getType();

    public abstract boolean isIdentity();

    public abstract boolean isTranslateOrIdentity();

    public abstract boolean is2D();

    public abstract double getDeterminant();

    public double getMxx() {
        return 1.0;
    }

    public double getMxy() {
        return 0.0;
    }

    public double getMxz() {
        return 0.0;
    }

    public double getMxt() {
        return 0.0;
    }

    public double getMyx() {
        return 0.0;
    }

    public double getMyy() {
        return 1.0;
    }

    public double getMyz() {
        return 0.0;
    }

    public double getMyt() {
        return 0.0;
    }

    public double getMzx() {
        return 0.0;
    }

    public double getMzy() {
        return 0.0;
    }

    public double getMzz() {
        return 1.0;
    }

    public double getMzt() {
        return 0.0;
    }

    public abstract Point2D transform(Point2D src, Point2D dst);

    public abstract Point2D inverseTransform(Point2D src, Point2D dst)
            throws NoninvertibleTransformException;

    public abstract Vec3d transform(Vec3d src, Vec3d dst);

    public abstract Vec3d deltaTransform(Vec3d src, Vec3d dst);

    public abstract Vec3d inverseTransform(Vec3d src, Vec3d dst)
            throws NoninvertibleTransformException;

    public abstract Vec3d inverseDeltaTransform(Vec3d src, Vec3d dst)
            throws NoninvertibleTransformException;

    public abstract void transform(float[] srcPts, int srcOff,
                                   float[] dstPts, int dstOff,
                                   int numPts);

    public abstract void transform(double[] srcPts, int srcOff,
                                   double[] dstPts, int dstOff,
                                   int numPts);

    public abstract void transform(float[] srcPts, int srcOff,
                                   double[] dstPts, int dstOff,
                                   int numPts);

    public abstract void transform(double[] srcPts, int srcOff,
                                   float[] dstPts, int dstOff,
                                   int numPts);

    public abstract void deltaTransform(float[] srcPts, int srcOff,
                                        float[] dstPts, int dstOff,
                                        int numPts);

    public abstract void deltaTransform(double[] srcPts, int srcOff,
                                        double[] dstPts, int dstOff,
                                        int numPts);

    public abstract void inverseTransform(float[] srcPts, int srcOff,
                                          float[] dstPts, int dstOff,
                                          int numPts)
            throws NoninvertibleTransformException;

    public abstract void inverseDeltaTransform(float[] srcPts, int srcOff,
                                               float[] dstPts, int dstOff,
                                               int numPts)
            throws NoninvertibleTransformException;

    public abstract void inverseTransform(double[] srcPts, int srcOff,
                                          double[] dstPts, int dstOff,
                                          int numPts)
            throws NoninvertibleTransformException;

    public abstract BaseBounds transform(BaseBounds bounds, BaseBounds result);

    public abstract void transform(Rectangle rect, Rectangle result);

    public abstract BaseBounds inverseTransform(BaseBounds bounds, BaseBounds result)
            throws NoninvertibleTransformException;

    public abstract void inverseTransform(Rectangle rect, Rectangle result)
            throws NoninvertibleTransformException;

    public abstract void setToIdentity();

    public abstract void setTransform(BaseTransform xform);

    /**
     * This function inverts the {@code BaseTransform} in place.  All
     * current implementations can support their own inverted form, and
     * that should likely remain true in the future as well.
     */
    public abstract void invert() throws NoninvertibleTransformException;

    /**
     * This function is only guaranteed to succeed if the transform is
     * of degree AFFINE2D or less and the matrix
     * parameters specified came from this same instance.  In practice,
     * they will also tend to succeed if they specify a transform of
     * Degree less than or equal to the Degree of this instance as well,
     * but the intent of this method is to restore the transform that
     * had been read out of this transform into local variables.
     */
    public abstract void restoreTransform(double mxx, double myx,
                                          double mxy, double myy,
                                          double mxt, double myt);

    /**
     * This function is only guaranteed to succeed if the matrix
     * parameters specified came from this same instance.  In practice,
     * they will also tend to succeed if they specify a transform of
     * Degree less than or equal to the Degree of this instance as well,
     * but the intent of this method is to restore the transform that
     * had been read out of this transform into local variables.
     */
    public abstract void restoreTransform(double mxx, double mxy, double mxz, double mxt,
                                          double myx, double myy, double myz, double myt,
                                          double mzx, double mzy, double mzz, double mzt);

    public abstract BaseTransform deriveWithTranslation(double mxt, double myt);

    public abstract BaseTransform deriveWithTranslation(double mxt, double myt, double mzt);

    public abstract BaseTransform deriveWithScale(double mxx, double myy, double mzz);

    public abstract BaseTransform deriveWithRotation(double theta, double axisX, double axisY, double axisZ);

    public abstract BaseTransform deriveWithPreTranslation(double mxt, double myt);

    public abstract BaseTransform deriveWithConcatenation(double mxx, double myx,
                                                          double mxy, double myy,
                                                          double mxt, double myt);

    public abstract BaseTransform deriveWithConcatenation(
            double mxx, double mxy, double mxz, double mxt,
            double myx, double myy, double myz, double myt,
            double mzx, double mzy, double mzz, double mzt);

    public abstract BaseTransform deriveWithPreConcatenation(BaseTransform transform);

    public abstract BaseTransform deriveWithConcatenation(BaseTransform tx);

    public abstract BaseTransform deriveWithNewTransform(BaseTransform tx);

    /**
     * This function always returns a new object, unless the transform
     * is an identity transform in which case it might return the
     * {@code Identity} singleton.
     *
     * @return a new transform representing the inverse of this transform.
     */
    public abstract BaseTransform createInverse()
            throws NoninvertibleTransformException;

    public abstract BaseTransform copy();

    /**
     * Returns the hashcode for this transform.
     *
     * @return a hash code for this transform.
     */
    @Override
    public int hashCode() {
        if (isIdentity()) return 0;
        long bits = 0;
        bits = bits * 31 + Double.doubleToLongBits(getMzz());
        bits = bits * 31 + Double.doubleToLongBits(getMzy());
        bits = bits * 31 + Double.doubleToLongBits(getMzx());
        bits = bits * 31 + Double.doubleToLongBits(getMyz());
        bits = bits * 31 + Double.doubleToLongBits(getMxz());
        bits = bits * 31 + Double.doubleToLongBits(getMyy());
        bits = bits * 31 + Double.doubleToLongBits(getMyx());
        bits = bits * 31 + Double.doubleToLongBits(getMxy());
        bits = bits * 31 + Double.doubleToLongBits(getMxx());
        bits = bits * 31 + Double.doubleToLongBits(getMzt());
        bits = bits * 31 + Double.doubleToLongBits(getMyt());
        bits = bits * 31 + Double.doubleToLongBits(getMxt());
        return (((int) bits) ^ ((int) (bits >> 32)));
    }

    /**
     * Returns <code>true</code> if this <code>BaseTransform</code>
     * represents the same coordinate transform as the specified
     * argument.
     *
     * @param obj the <code>Object</code> to test for equality with this
     *            <code>BaseTransform</code>
     * @return <code>true</code> if <code>obj</code> equals this
     * <code>BaseTransform</code> object; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BaseTransform)) {
            return false;
        }

        BaseTransform a = (BaseTransform) obj;

        return (getMxx() == a.getMxx() &&
                getMxy() == a.getMxy() &&
                getMxz() == a.getMxz() &&
                getMxt() == a.getMxt() &&
                getMyx() == a.getMyx() &&
                getMyy() == a.getMyy() &&
                getMyz() == a.getMyz() &&
                getMyt() == a.getMyt() &&
                getMzx() == a.getMzx() &&
                getMzy() == a.getMzy() &&
                getMzz() == a.getMzz() &&
                getMzt() == a.getMzt());
    }

    static Point2D makePoint(Point2D src, Point2D dst) {
        if (dst == null) {
            dst = new Point2D();
        }
        return dst;
    }

    static final double EPSILON_ABSOLUTE = 1.0e-5;

    public static boolean almostZero(double a) {
        return ((a < EPSILON_ABSOLUTE) && (a > -EPSILON_ABSOLUTE));
    }

    /**
     * Returns the matrix elements and degree of this transform as a string.
     *
     * @return the matrix elements and degree of this transform
     */
    @Override
    public String toString() {
        return "Matrix: degree " + getDegree() + "\n" +
                getMxx() + ", " + getMxy() + ", " + getMxz() + ", " + getMxt() + "\n" +
                getMyx() + ", " + getMyy() + ", " + getMyz() + ", " + getMyt() + "\n" +
                getMzx() + ", " + getMzy() + ", " + getMzz() + ", " + getMzt() + "\n";
    }
}
