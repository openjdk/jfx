/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates.
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

/**
 * The <code>Affine2D</code> class represents a 2D affine transform
 * that performs a linear mapping from 2D coordinates to other 2D
 * coordinates that preserves the "straightness" and
 * "parallelness" of lines.  Affine transformations can be constructed
 * using sequences of translations, scales, flips, rotations, and shears.
 * <p>
 * Such a coordinate transformation can be represented by a 3 row by
 * 3 column matrix with an implied last row of [ 0 0 1 ].  This matrix
 * transforms source coordinates {@code (x, y)} into
 * destination coordinates {@code (x', y')} by considering
 * them to be a column vector and multiplying the coordinate vector
 * by the matrix according to the following process:
 * <pre>
 *  [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
 *  [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
 *  [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
 * </pre>
 * <p>
 * <a name="quadrantapproximation"><h4>Handling 90-Degree Rotations</h4></a>
 * <p>
 * In some variations of the <code>rotate</code> methods in the
 * <code>Affine2D</code> class, a double-precision argument
 * specifies the angle of rotation in radians.
 * These methods have special handling for rotations of approximately
 * 90 degrees (including multiples such as 180, 270, and 360 degrees),
 * so that the common case of quadrant rotation is handled more
 * efficiently.
 * This special handling can cause angles very close to multiples of
 * 90 degrees to be treated as if they were exact multiples of
 * 90 degrees.
 * For small multiples of 90 degrees the range of angles treated
 * as a quadrant rotation is approximately 0.00000121 degrees wide.
 * This section explains why such special care is needed and how
 * it is implemented.
 * <p>
 * Since 90 degrees is represented as <code>PI/2</code> in radians,
 * and since PI is a transcendental (and therefore irrational) number,
 * it is not possible to exactly represent a multiple of 90 degrees as
 * an exact double precision value measured in radians.
 * As a result it is theoretically impossible to describe quadrant
 * rotations (90, 180, 270 or 360 degrees) using these values.
 * Double precision floating point values can get very close to
 * non-zero multiples of <code>PI/2</code> but never close enough
 * for the sine or cosine to be exactly 0.0, 1.0 or -1.0.
 * The implementations of <code>Math.sin()</code> and
 * <code>Math.cos()</code> correspondingly never return 0.0
 * for any case other than <code>Math.sin(0.0)</code>.
 * These same implementations do, however, return exactly 1.0 and
 * -1.0 for some range of numbers around each multiple of 90
 * degrees since the correct answer is so close to 1.0 or -1.0 that
 * the double precision significand cannot represent the difference
 * as accurately as it can for numbers that are near 0.0.
 * <p>
 * The net result of these issues is that if the
 * <code>Math.sin()</code> and <code>Math.cos()</code> methods
 * are used to directly generate the values for the matrix modifications
 * during these radian-based rotation operations then the resulting
 * transform is never strictly classifiable as a quadrant rotation
 * even for a simple case like <code>rotate(Math.PI/2.0)</code>,
 * due to minor variations in the matrix caused by the non-0.0 values
 * obtained for the sine and cosine.
 * If these transforms are not classified as quadrant rotations then
 * subsequent code which attempts to optimize further operations based
 * upon the type of the transform will be relegated to its most general
 * implementation.
 * <p>
 * Because quadrant rotations are fairly common,
 * this class should handle these cases reasonably quickly, both in
 * applying the rotations to the transform and in applying the resulting
 * transform to the coordinates.
 * To facilitate this optimal handling, the methods which take an angle
 * of rotation measured in radians attempt to detect angles that are
 * intended to be quadrant rotations and treat them as such.
 * These methods therefore treat an angle <em>theta</em> as a quadrant
 * rotation if either <code>Math.sin(<em>theta</em>)</code> or
 * <code>Math.cos(<em>theta</em>)</code> returns exactly 1.0 or -1.0.
 * As a rule of thumb, this property holds true for a range of
 * approximately 0.0000000211 radians (or 0.00000121 degrees) around
 * small multiples of <code>Math.PI/2.0</code>.
 *
 * @version 1.83, 05/05/07
 */
public class Affine2D extends AffineBase {
    private Affine2D(double mxx, double myx,
                     double mxy, double myy,
                     double mxt, double myt,
                     int state) {
        this.mxx = mxx;
        this.myx = myx;
        this.mxy = mxy;
        this.myy = myy;
        this.mxt = mxt;
        this.myt = myt;
        this.state = state;
        this.type = TYPE_UNKNOWN;
    }

    /**
     * Constructs a new <code>Affine2D</code> representing the
     * Identity transformation.
     */
    public Affine2D() {
        mxx = myy = 1.0;
        // m01 = m10 = m02 = m12 = 0.0;     /* Not needed. */
        // state = APPLY_IDENTITY;      /* Not needed. */
        // type = TYPE_IDENTITY;        /* Not needed. */
    }

    /**
     * Constructs a new <code>Affine2D</code> that uses the same transform
     * as the specified <code>BaseTransform</code> object.
     *
     * @param Tx the <code>BaseTransform</code> object to copy
     */
    public Affine2D(BaseTransform Tx) {
        setTransform(Tx);
    }

    /**
     * Constructs a new <code>Affine2D</code> from 6 floating point
     * values representing the 6 specifiable entries of the 3x3
     * transformation matrix.
     *
     * @param mxx the X coordinate scaling element of the 3x3 matrix
     * @param myx the Y coordinate shearing element of the 3x3 matrix
     * @param mxy the X coordinate shearing element of the 3x3 matrix
     * @param myy the Y coordinate scaling element of the 3x3 matrix
     * @param mxt the X coordinate translation element of the 3x3 matrix
     * @param myt the Y coordinate translation element of the 3x3 matrix
     */
    public Affine2D(float mxx, float myx,
                    float mxy, float myy,
                    float mxt, float myt) {
        this.mxx = mxx;
        this.myx = myx;
        this.mxy = mxy;
        this.myy = myy;
        this.mxt = mxt;
        this.myt = myt;
        updateState2D();
    }

    /**
     * Constructs a new <code>Affine2D</code> from 6 double
     * precision values representing the 6 specifiable entries of the 3x3
     * transformation matrix.
     *
     * @param mxx the X coordinate scaling element of the 3x3 matrix
     * @param myx the Y coordinate shearing element of the 3x3 matrix
     * @param mxy the X coordinate shearing element of the 3x3 matrix
     * @param myy the Y coordinate scaling element of the 3x3 matrix
     * @param mxt the X coordinate translation element of the 3x3 matrix
     * @param myt the Y coordinate translation element of the 3x3 matrix
     */
    public Affine2D(double mxx, double myx,
                    double mxy, double myy,
                    double mxt, double myt) {
        this.mxx = mxx;
        this.myx = myx;
        this.mxy = mxy;
        this.myy = myy;
        this.mxt = mxt;
        this.myt = myt;
        updateState2D();
    }

    @Override
    public Degree getDegree() {
        return Degree.AFFINE_2D;
    }

    @Override
    protected void reset3Delements() { /* NOP for Affine2D */ }

    /**
     * Concatenates this transform with a transform that rotates
     * coordinates around an anchor point.
     * This operation is equivalent to translating the coordinates so
     * that the anchor point is at the origin (S1), then rotating them
     * about the new origin (S2), and finally translating so that the
     * intermediate origin is restored to the coordinates of the original
     * anchor point (S3).
     * <p>
     * This operation is equivalent to the following sequence of calls:
     * <pre>
     *     translate(anchorx, anchory);      // S3: final translation
     *     rotate(theta);                    // S2: rotate around anchor
     *     translate(-anchorx, -anchory);    // S1: translate anchor to origin
     * </pre>
     * Rotating by a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     * Note also the discussion of
     * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
     * above.
     *
     * @param theta   the angle of rotation measured in radians
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     */
    public void rotate(double theta, double anchorx, double anchory) {
        // REMIND: Simple for now - optimize later
        translate(anchorx, anchory);
        rotate(theta);
        translate(-anchorx, -anchory);
    }

    /**
     * Concatenates this transform with a transform that rotates
     * coordinates according to a rotation vector.
     * All coordinates rotate about the origin by the same amount.
     * The amount of rotation is such that coordinates along the former
     * positive X axis will subsequently align with the vector pointing
     * from the origin to the specified vector coordinates.
     * If both <code>vecx</code> and <code>vecy</code> are 0.0,
     * no additional rotation is added to this transform.
     * This operation is equivalent to calling:
     * <pre>
     *          rotate(Math.atan2(vecy, vecx));
     * </pre>
     *
     * @param vecx the X coordinate of the rotation vector
     * @param vecy the Y coordinate of the rotation vector
     */
    public void rotate(double vecx, double vecy) {
        if (vecy == 0.0) {
            if (vecx < 0.0) {
                rotate180();
            }
            // If vecx > 0.0 - no rotation
            // If vecx == 0.0 - undefined rotation - treat as no rotation
        } else if (vecx == 0.0) {
            if (vecy > 0.0) {
                rotate90();
            } else {  // vecy must be < 0.0
                rotate270();
            }
        } else {
            double len = Math.sqrt(vecx * vecx + vecy * vecy);
            double sin = vecy / len;
            double cos = vecx / len;
            double M0, M1;
            M0 = mxx;
            M1 = mxy;
            mxx = cos * M0 + sin * M1;
            mxy = -sin * M0 + cos * M1;
            M0 = myx;
            M1 = myy;
            myx = cos * M0 + sin * M1;
            myy = -sin * M0 + cos * M1;
            updateState2D();
        }
    }

    /**
     * Concatenates this transform with a transform that rotates
     * coordinates around an anchor point according to a rotation
     * vector.
     * All coordinates rotate about the specified anchor coordinates
     * by the same amount.
     * The amount of rotation is such that coordinates along the former
     * positive X axis will subsequently align with the vector pointing
     * from the origin to the specified vector coordinates.
     * If both <code>vecx</code> and <code>vecy</code> are 0.0,
     * the transform is not modified in any way.
     * This method is equivalent to calling:
     * <pre>
     *     rotate(Math.atan2(vecy, vecx), anchorx, anchory);
     * </pre>
     *
     * @param vecx    the X coordinate of the rotation vector
     * @param vecy    the Y coordinate of the rotation vector
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     */
    public void rotate(double vecx, double vecy,
                       double anchorx, double anchory) {
        // REMIND: Simple for now - optimize later
        translate(anchorx, anchory);
        rotate(vecx, vecy);
        translate(-anchorx, -anchory);
    }

    /**
     * Concatenates this transform with a transform that rotates
     * coordinates by the specified number of quadrants.
     * This is equivalent to calling:
     * <pre>
     *     rotate(numquadrants * Math.PI / 2.0);
     * </pre>
     * Rotating by a positive number of quadrants rotates points on
     * the positive X axis toward the positive Y axis.
     *
     * @param numquadrants the number of 90 degree arcs to rotate by
     */
    public void quadrantRotate(int numquadrants) {
        switch (numquadrants & 3) {
            case 0:
                break;
            case 1:
                rotate90();
                break;
            case 2:
                rotate180();
                break;
            case 3:
                rotate270();
                break;
        }
    }

    /**
     * Concatenates this transform with a transform that rotates
     * coordinates by the specified number of quadrants around
     * the specified anchor point.
     * This method is equivalent to calling:
     * <pre>
     *     rotate(numquadrants * Math.PI / 2.0, anchorx, anchory);
     * </pre>
     * Rotating by a positive number of quadrants rotates points on
     * the positive X axis toward the positive Y axis.
     *
     * @param numquadrants the number of 90 degree arcs to rotate by
     * @param anchorx      the X coordinate of the rotation anchor point
     * @param anchory      the Y coordinate of the rotation anchor point
     */
    public void quadrantRotate(int numquadrants,
                               double anchorx, double anchory) {
        switch (numquadrants & 3) {
            case 0:
                return;
            case 1:
                mxt += anchorx * (mxx - mxy) + anchory * (mxy + mxx);
                myt += anchorx * (myx - myy) + anchory * (myy + myx);
                rotate90();
                break;
            case 2:
                mxt += anchorx * (mxx + mxx) + anchory * (mxy + mxy);
                myt += anchorx * (myx + myx) + anchory * (myy + myy);
                rotate180();
                break;
            case 3:
                mxt += anchorx * (mxx + mxy) + anchory * (mxy - mxx);
                myt += anchorx * (myx + myy) + anchory * (myy - myx);
                rotate270();
                break;
        }
        if (mxt == 0.0 && myt == 0.0) {
            state &= ~APPLY_TRANSLATE;
            if (type != TYPE_UNKNOWN) {
                type &= ~TYPE_TRANSLATION;
            }
        } else {
            state |= APPLY_TRANSLATE;
            type |= TYPE_TRANSLATION;
        }
    }

    /**
     * Sets this transform to a translation transformation.
     * The matrix representing this transform becomes:
     * <pre>
     *      [   1    0    tx  ]
     *      [   0    1    ty  ]
     *      [   0    0    1   ]
     * </pre>
     *
     * @param tx the distance by which coordinates are translated in the
     *           X axis direction
     * @param ty the distance by which coordinates are translated in the
     *           Y axis direction
     */
    public void setToTranslation(double tx, double ty) {
        mxx = 1.0;
        myx = 0.0;
        mxy = 0.0;
        myy = 1.0;
        mxt = tx;
        myt = ty;
        if (tx != 0.0 || ty != 0.0) {
            state = APPLY_TRANSLATE;
            type = TYPE_TRANSLATION;
        } else {
            state = APPLY_IDENTITY;
            type = TYPE_IDENTITY;
        }
    }

    /**
     * Sets this transform to a rotation transformation.
     * The matrix representing this transform becomes:
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
     *
     * @param theta the angle of rotation measured in radians
     */
    public void setToRotation(double theta) {
        double sin = Math.sin(theta);
        double cos;
        if (sin == 1.0 || sin == -1.0) {
            cos = 0.0;
            state = APPLY_SHEAR;
            type = TYPE_QUADRANT_ROTATION;
        } else {
            cos = Math.cos(theta);
            if (cos == -1.0) {
                sin = 0.0;
                state = APPLY_SCALE;
                type = TYPE_QUADRANT_ROTATION;
            } else if (cos == 1.0) {
                sin = 0.0;
                state = APPLY_IDENTITY;
                type = TYPE_IDENTITY;
            } else {
                state = APPLY_SHEAR | APPLY_SCALE;
                type = TYPE_GENERAL_ROTATION;
            }
        }
        mxx = cos;
        myx = sin;
        mxy = -sin;
        myy = cos;
        mxt = 0.0;
        myt = 0.0;
    }

    /**
     * Sets this transform to a translated rotation transformation.
     * This operation is equivalent to translating the coordinates so
     * that the anchor point is at the origin (S1), then rotating them
     * about the new origin (S2), and finally translating so that the
     * intermediate origin is restored to the coordinates of the original
     * anchor point (S3).
     * <p>
     * This operation is equivalent to the following sequence of calls:
     * <pre>
     *     setToTranslation(anchorx, anchory); // S3: final translation
     *     rotate(theta);                      // S2: rotate around anchor
     *     translate(-anchorx, -anchory);      // S1: translate anchor to origin
     * </pre>
     * The matrix representing this transform becomes:
     * <pre>
     *      [   cos(theta)    -sin(theta)    x-x*cos+y*sin  ]
     *      [   sin(theta)     cos(theta)    y-x*sin-y*cos  ]
     *      [       0              0               1        ]
     * </pre>
     * Rotating by a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     * Note also the discussion of
     * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
     * above.
     *
     * @param theta   the angle of rotation measured in radians
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     */
    public void setToRotation(double theta, double anchorx, double anchory) {
        setToRotation(theta);
        double sin = myx;
        double oneMinusCos = 1.0 - mxx;
        mxt = anchorx * oneMinusCos + anchory * sin;
        myt = anchory * oneMinusCos - anchorx * sin;
        if (mxt != 0.0 || myt != 0.0) {
            state |= APPLY_TRANSLATE;
            type |= TYPE_TRANSLATION;
        }
    }

    /**
     * Sets this transform to a rotation transformation that rotates
     * coordinates according to a rotation vector.
     * All coordinates rotate about the origin by the same amount.
     * The amount of rotation is such that coordinates along the former
     * positive X axis will subsequently align with the vector pointing
     * from the origin to the specified vector coordinates.
     * If both <code>vecx</code> and <code>vecy</code> are 0.0,
     * the transform is set to an identity transform.
     * This operation is equivalent to calling:
     * <pre>
     *     setToRotation(Math.atan2(vecy, vecx));
     * </pre>
     *
     * @param vecx the X coordinate of the rotation vector
     * @param vecy the Y coordinate of the rotation vector
     */
    public void setToRotation(double vecx, double vecy) {
        double sin, cos;
        if (vecy == 0) {
            sin = 0.0;
            if (vecx < 0.0) {
                cos = -1.0;
                state = APPLY_SCALE;
                type = TYPE_QUADRANT_ROTATION;
            } else {
                cos = 1.0;
                state = APPLY_IDENTITY;
                type = TYPE_IDENTITY;
            }
        } else if (vecx == 0) {
            cos = 0.0;
            sin = (vecy > 0.0) ? 1.0 : -1.0;
            state = APPLY_SHEAR;
            type = TYPE_QUADRANT_ROTATION;
        } else {
            double len = Math.sqrt(vecx * vecx + vecy * vecy);
            cos = vecx / len;
            sin = vecy / len;
            state = APPLY_SHEAR | APPLY_SCALE;
            type = TYPE_GENERAL_ROTATION;
        }
        mxx = cos;
        myx = sin;
        mxy = -sin;
        myy = cos;
        mxt = 0.0;
        myt = 0.0;
    }

    /**
     * Sets this transform to a rotation transformation that rotates
     * coordinates around an anchor point according to a rotation
     * vector.
     * All coordinates rotate about the specified anchor coordinates
     * by the same amount.
     * The amount of rotation is such that coordinates along the former
     * positive X axis will subsequently align with the vector pointing
     * from the origin to the specified vector coordinates.
     * If both <code>vecx</code> and <code>vecy</code> are 0.0,
     * the transform is set to an identity transform.
     * This operation is equivalent to calling:
     * <pre>
     *     setToTranslation(Math.atan2(vecy, vecx), anchorx, anchory);
     * </pre>
     *
     * @param vecx    the X coordinate of the rotation vector
     * @param vecy    the Y coordinate of the rotation vector
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     */
    public void setToRotation(double vecx, double vecy,
                              double anchorx, double anchory) {
        setToRotation(vecx, vecy);
        double sin = myx;
        double oneMinusCos = 1.0 - mxx;
        mxt = anchorx * oneMinusCos + anchory * sin;
        myt = anchory * oneMinusCos - anchorx * sin;
        if (mxt != 0.0 || myt != 0.0) {
            state |= APPLY_TRANSLATE;
            type |= TYPE_TRANSLATION;
        }
    }

    /**
     * Sets this transform to a rotation transformation that rotates
     * coordinates by the specified number of quadrants.
     * This operation is equivalent to calling:
     * <pre>
     *     setToRotation(numquadrants * Math.PI / 2.0);
     * </pre>
     * Rotating by a positive number of quadrants rotates points on
     * the positive X axis toward the positive Y axis.
     *
     * @param numquadrants the number of 90 degree arcs to rotate by
     */
    public void setToQuadrantRotation(int numquadrants) {
        switch (numquadrants & 3) {
            case 0:
                mxx = 1.0;
                myx = 0.0;
                mxy = 0.0;
                myy = 1.0;
                mxt = 0.0;
                myt = 0.0;
                state = APPLY_IDENTITY;
                type = TYPE_IDENTITY;
                break;
            case 1:
                mxx = 0.0;
                myx = 1.0;
                mxy = -1.0;
                myy = 0.0;
                mxt = 0.0;
                myt = 0.0;
                state = APPLY_SHEAR;
                type = TYPE_QUADRANT_ROTATION;
                break;
            case 2:
                mxx = -1.0;
                myx = 0.0;
                mxy = 0.0;
                myy = -1.0;
                mxt = 0.0;
                myt = 0.0;
                state = APPLY_SCALE;
                type = TYPE_QUADRANT_ROTATION;
                break;
            case 3:
                mxx = 0.0;
                myx = -1.0;
                mxy = 1.0;
                myy = 0.0;
                mxt = 0.0;
                myt = 0.0;
                state = APPLY_SHEAR;
                type = TYPE_QUADRANT_ROTATION;
                break;
        }
    }

    /**
     * Sets this transform to a translated rotation transformation
     * that rotates coordinates by the specified number of quadrants
     * around the specified anchor point.
     * This operation is equivalent to calling:
     * <pre>
     *     setToRotation(numquadrants * Math.PI / 2.0, anchorx, anchory);
     * </pre>
     * Rotating by a positive number of quadrants rotates points on
     * the positive X axis toward the positive Y axis.
     *
     * @param numquadrants the number of 90 degree arcs to rotate by
     * @param anchorx      the X coordinate of the rotation anchor point
     * @param anchory      the Y coordinate of the rotation anchor point
     */
    public void setToQuadrantRotation(int numquadrants,
                                      double anchorx, double anchory) {
        switch (numquadrants & 3) {
            case 0:
                mxx = 1.0;
                myx = 0.0;
                mxy = 0.0;
                myy = 1.0;
                mxt = 0.0;
                myt = 0.0;
                state = APPLY_IDENTITY;
                type = TYPE_IDENTITY;
                break;
            case 1:
                mxx = 0.0;
                myx = 1.0;
                mxy = -1.0;
                myy = 0.0;
                mxt = anchorx + anchory;
                myt = anchory - anchorx;
                if (mxt == 0.0 && myt == 0.0) {
                    state = APPLY_SHEAR;
                    type = TYPE_QUADRANT_ROTATION;
                } else {
                    state = APPLY_SHEAR | APPLY_TRANSLATE;
                    type = TYPE_QUADRANT_ROTATION | TYPE_TRANSLATION;
                }
                break;
            case 2:
                mxx = -1.0;
                myx = 0.0;
                mxy = 0.0;
                myy = -1.0;
                mxt = anchorx + anchorx;
                myt = anchory + anchory;
                if (mxt == 0.0 && myt == 0.0) {
                    state = APPLY_SCALE;
                    type = TYPE_QUADRANT_ROTATION;
                } else {
                    state = APPLY_SCALE | APPLY_TRANSLATE;
                    type = TYPE_QUADRANT_ROTATION | TYPE_TRANSLATION;
                }
                break;
            case 3:
                mxx = 0.0;
                myx = -1.0;
                mxy = 1.0;
                myy = 0.0;
                mxt = anchorx - anchory;
                myt = anchory + anchorx;
                if (mxt == 0.0 && myt == 0.0) {
                    state = APPLY_SHEAR;
                    type = TYPE_QUADRANT_ROTATION;
                } else {
                    state = APPLY_SHEAR | APPLY_TRANSLATE;
                    type = TYPE_QUADRANT_ROTATION | TYPE_TRANSLATION;
                }
                break;
        }
    }

    /**
     * Sets this transform to a scaling transformation.
     * The matrix representing this transform becomes:
     * <pre>
     *      [   sx   0    0   ]
     *      [   0    sy   0   ]
     *      [   0    0    1   ]
     * </pre>
     *
     * @param sx the factor by which coordinates are scaled along the
     *           X axis direction
     * @param sy the factor by which coordinates are scaled along the
     *           Y axis direction
     */
    public void setToScale(double sx, double sy) {
        mxx = sx;
        myx = 0.0;
        mxy = 0.0;
        myy = sy;
        mxt = 0.0;
        myt = 0.0;
        if (sx != 1.0 || sy != 1.0) {
            state = APPLY_SCALE;
            type = TYPE_UNKNOWN;
        } else {
            state = APPLY_IDENTITY;
            type = TYPE_IDENTITY;
        }
    }

    /**
     * Sets this transform to a copy of the transform in the specified
     * <code>BaseTransform</code> object.
     *
     * @param Tx the <code>BaseTransform</code> object from which to
     *           copy the transform
     */
    public void setTransform(BaseTransform Tx) {
        switch (Tx.getDegree()) {
            case IDENTITY:
                setToIdentity();
                break;
            case TRANSLATE_2D:
                setToTranslation(Tx.getMxt(), Tx.getMyt());
                break;
            default:
                if (Tx.getType() > TYPE_AFFINE2D_MASK) {
                    System.out.println(Tx + " is " + Tx.getType());
                    System.out.print("  " + Tx.getMxx());
                    System.out.print(", " + Tx.getMxy());
                    System.out.print(", " + Tx.getMxz());
                    System.out.print(", " + Tx.getMxt());
                    System.out.println();
                    System.out.print("  " + Tx.getMyx());
                    System.out.print(", " + Tx.getMyy());
                    System.out.print(", " + Tx.getMyz());
                    System.out.print(", " + Tx.getMyt());
                    System.out.println();
                    System.out.print("  " + Tx.getMzx());
                    System.out.print(", " + Tx.getMzy());
                    System.out.print(", " + Tx.getMzz());
                    System.out.print(", " + Tx.getMzt());
                    System.out.println();
                    // TODO: Should this be thrown before we modify anything?
                    // (RT-26801)
                    degreeError(Degree.AFFINE_2D);
                }
                /* No Break */
            case AFFINE_2D:
                this.mxx = Tx.getMxx();
                this.myx = Tx.getMyx();
                this.mxy = Tx.getMxy();
                this.myy = Tx.getMyy();
                this.mxt = Tx.getMxt();
                this.myt = Tx.getMyt();
                if (Tx instanceof AffineBase) {
                    this.state = ((AffineBase) Tx).state;
                    this.type = ((AffineBase) Tx).type;
                } else {
                    updateState2D();
                }
                break;
        }
    }

    /**
     * Concatenates a <code>BaseTransform</code> <code>Tx</code> to
     * this <code>Affine2D</code> Cx
     * in a less commonly used way such that <code>Tx</code> modifies the
     * coordinate transformation relative to the absolute pixel
     * space rather than relative to the existing user space.
     * Cx is updated to perform the combined transformation.
     * Transforming a point p by the updated transform Cx' is
     * equivalent to first transforming p by the original transform
     * Cx and then transforming the result by
     * <code>Tx</code> like this:
     * Cx'(p) = Tx(Cx(p))
     * In matrix notation, if this transform Cx
     * is represented by the matrix [this] and <code>Tx</code> is
     * represented by the matrix [Tx] then this method does the
     * following:
     * <pre>
     *      [this] = [Tx] x [this]
     * </pre>
     *
     * @param Tx the <code>BaseTransform</code> object to be
     *           concatenated with this <code>Affine2D</code> object.
     * @see #concatenate
     */
    public void preConcatenate(BaseTransform Tx) {
        switch (Tx.getDegree()) {
            case IDENTITY:
                return;
            case TRANSLATE_2D:
                translate(Tx.getMxt(), Tx.getMyt());
                return;
            case AFFINE_2D:
                break;
            default:
                degreeError(Degree.AFFINE_2D);
        }
        double M0, M1;
        double Txx, Txy, Tyx, Tyy;
        double Txt, Tyt;
        int mystate = state;
        Affine2D at = (Affine2D) Tx;
        int txstate = at.state;
        switch ((txstate << HI_SHIFT) | mystate) {
            case (HI_IDENTITY | APPLY_IDENTITY):
            case (HI_IDENTITY | APPLY_TRANSLATE):
            case (HI_IDENTITY | APPLY_SCALE):
            case (HI_IDENTITY | APPLY_SCALE | APPLY_TRANSLATE):
            case (HI_IDENTITY | APPLY_SHEAR):
            case (HI_IDENTITY | APPLY_SHEAR | APPLY_TRANSLATE):
            case (HI_IDENTITY | APPLY_SHEAR | APPLY_SCALE):
            case (HI_IDENTITY | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
                // Tx is IDENTITY...
                return;

            case (HI_TRANSLATE | APPLY_IDENTITY):
            case (HI_TRANSLATE | APPLY_SCALE):
            case (HI_TRANSLATE | APPLY_SHEAR):
            case (HI_TRANSLATE | APPLY_SHEAR | APPLY_SCALE):
                // Tx is TRANSLATE, this has no TRANSLATE
                mxt = at.mxt;
                myt = at.myt;
                state = mystate | APPLY_TRANSLATE;
                type |= TYPE_TRANSLATION;
                return;

            case (HI_TRANSLATE | APPLY_TRANSLATE):
            case (HI_TRANSLATE | APPLY_SCALE | APPLY_TRANSLATE):
            case (HI_TRANSLATE | APPLY_SHEAR | APPLY_TRANSLATE):
            case (HI_TRANSLATE | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
                // Tx is TRANSLATE, this has one too
                mxt = mxt + at.mxt;
                myt = myt + at.myt;
                return;

            case (HI_SCALE | APPLY_TRANSLATE):
            case (HI_SCALE | APPLY_IDENTITY):
                // Only these two existing states need a new state
                state = mystate | APPLY_SCALE;
            /* NOBREAK */
            case (HI_SCALE | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            case (HI_SCALE | APPLY_SHEAR | APPLY_SCALE):
            case (HI_SCALE | APPLY_SHEAR | APPLY_TRANSLATE):
            case (HI_SCALE | APPLY_SHEAR):
            case (HI_SCALE | APPLY_SCALE | APPLY_TRANSLATE):
            case (HI_SCALE | APPLY_SCALE):
                // Tx is SCALE, this is anything
                Txx = at.mxx;
                Tyy = at.myy;
                if ((mystate & APPLY_SHEAR) != 0) {
                    mxy = mxy * Txx;
                    myx = myx * Tyy;
                    if ((mystate & APPLY_SCALE) != 0) {
                        mxx = mxx * Txx;
                        myy = myy * Tyy;
                    }
                } else {
                    mxx = mxx * Txx;
                    myy = myy * Tyy;
                }
                if ((mystate & APPLY_TRANSLATE) != 0) {
                    mxt = mxt * Txx;
                    myt = myt * Tyy;
                }
                type = TYPE_UNKNOWN;
                return;
            case (HI_SHEAR | APPLY_SHEAR | APPLY_TRANSLATE):
            case (HI_SHEAR | APPLY_SHEAR):
                mystate = mystate | APPLY_SCALE;
            /* NOBREAK */
            case (HI_SHEAR | APPLY_TRANSLATE):
            case (HI_SHEAR | APPLY_IDENTITY):
            case (HI_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            case (HI_SHEAR | APPLY_SCALE):
                state = mystate ^ APPLY_SHEAR;
            /* NOBREAK */
            case (HI_SHEAR | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            case (HI_SHEAR | APPLY_SHEAR | APPLY_SCALE):
                // Tx is SHEAR, this is anything
                Txy = at.mxy;
                Tyx = at.myx;

                M0 = mxx;
                mxx = myx * Txy;
                myx = M0 * Tyx;

                M0 = mxy;
                mxy = myy * Txy;
                myy = M0 * Tyx;

                M0 = mxt;
                mxt = myt * Txy;
                myt = M0 * Tyx;
                type = TYPE_UNKNOWN;
                return;
        }
        // If Tx has more than one attribute, it is not worth optimizing
        // all of those cases...
        Txx = at.mxx;
        Txy = at.mxy;
        Txt = at.mxt;
        Tyx = at.myx;
        Tyy = at.myy;
        Tyt = at.myt;
        switch (mystate) {
            default:
                stateError();
            /* NOTREACHED */
            case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
                M0 = mxt;
                M1 = myt;
                Txt += M0 * Txx + M1 * Txy;
                Tyt += M0 * Tyx + M1 * Tyy;

            /* NOBREAK */
            case (APPLY_SHEAR | APPLY_SCALE):
                mxt = Txt;
                myt = Tyt;

                M0 = mxx;
                M1 = myx;
                mxx = M0 * Txx + M1 * Txy;
                myx = M0 * Tyx + M1 * Tyy;

                M0 = mxy;
                M1 = myy;
                mxy = M0 * Txx + M1 * Txy;
                myy = M0 * Tyx + M1 * Tyy;
                break;

            case (APPLY_SHEAR | APPLY_TRANSLATE):
                M0 = mxt;
                M1 = myt;
                Txt += M0 * Txx + M1 * Txy;
                Tyt += M0 * Tyx + M1 * Tyy;

            /* NOBREAK */
            case (APPLY_SHEAR):
                mxt = Txt;
                myt = Tyt;

                M0 = myx;
                mxx = M0 * Txy;
                myx = M0 * Tyy;

                M0 = mxy;
                mxy = M0 * Txx;
                myy = M0 * Tyx;
                break;

            case (APPLY_SCALE | APPLY_TRANSLATE):
                M0 = mxt;
                M1 = myt;
                Txt += M0 * Txx + M1 * Txy;
                Tyt += M0 * Tyx + M1 * Tyy;

            /* NOBREAK */
            case (APPLY_SCALE):
                mxt = Txt;
                myt = Tyt;

                M0 = mxx;
                mxx = M0 * Txx;
                myx = M0 * Tyx;

                M0 = myy;
                mxy = M0 * Txy;
                myy = M0 * Tyy;
                break;

            case (APPLY_TRANSLATE):
                M0 = mxt;
                M1 = myt;
                Txt += M0 * Txx + M1 * Txy;
                Tyt += M0 * Tyx + M1 * Tyy;

            /* NOBREAK */
            case (APPLY_IDENTITY):
                mxt = Txt;
                myt = Tyt;

                mxx = Txx;
                myx = Tyx;

                mxy = Txy;
                myy = Tyy;

                state = mystate | txstate;
                type = TYPE_UNKNOWN;
                return;
        }
        updateState2D();
    }

    /**
     * Returns an <code>Affine2D</code> object representing the
     * inverse transformation.
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
     * thrown if the <code>createInverse</code> method is called.
     *
     * @return a new <code>Affine2D</code> object representing the
     * inverse transformation.
     * @throws NoninvertibleTransformException if the matrix cannot be inverted.
     * @see #getDeterminant
     */
    public Affine2D createInverse()
            throws NoninvertibleTransformException {
        double det;
        switch (state) {
            default:
                stateError();
            /* NOTREACHED */
            case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
                det = mxx * myy - mxy * myx;
                if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                    throw new NoninvertibleTransformException("Determinant is " +
                            det);
                }
                return new Affine2D(myy / det, -myx / det,
                        -mxy / det, mxx / det,
                        (mxy * myt - myy * mxt) / det,
                        (myx * mxt - mxx * myt) / det,
                        (APPLY_SHEAR |
                                APPLY_SCALE |
                                APPLY_TRANSLATE));
            case (APPLY_SHEAR | APPLY_SCALE):
                det = mxx * myy - mxy * myx;
                if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
                    throw new NoninvertibleTransformException("Determinant is " +
                            det);
                }
                return new Affine2D(myy / det, -myx / det,
                        -mxy / det, mxx / det,
                        0.0, 0.0,
                        (APPLY_SHEAR | APPLY_SCALE));
            case (APPLY_SHEAR | APPLY_TRANSLATE):
                if (mxy == 0.0 || myx == 0.0) {
                    throw new NoninvertibleTransformException("Determinant is 0");
                }
                return new Affine2D(0.0, 1.0 / mxy,
                        1.0 / myx, 0.0,
                        -myt / myx, -mxt / mxy,
                        (APPLY_SHEAR | APPLY_TRANSLATE));
            case (APPLY_SHEAR):
                if (mxy == 0.0 || myx == 0.0) {
                    throw new NoninvertibleTransformException("Determinant is 0");
                }
                return new Affine2D(0.0, 1.0 / mxy,
                        1.0 / myx, 0.0,
                        0.0, 0.0,
                        (APPLY_SHEAR));
            case (APPLY_SCALE | APPLY_TRANSLATE):
                if (mxx == 0.0 || myy == 0.0) {
                    throw new NoninvertibleTransformException("Determinant is 0");
                }
                return new Affine2D(1.0 / mxx, 0.0,
                        0.0, 1.0 / myy,
                        -mxt / mxx, -myt / myy,
                        (APPLY_SCALE | APPLY_TRANSLATE));
            case (APPLY_SCALE):
                if (mxx == 0.0 || myy == 0.0) {
                    throw new NoninvertibleTransformException("Determinant is 0");
                }
                return new Affine2D(1.0 / mxx, 0.0,
                        0.0, 1.0 / myy,
                        0.0, 0.0,
                        (APPLY_SCALE));
            case (APPLY_TRANSLATE):
                return new Affine2D(1.0, 0.0,
                        0.0, 1.0,
                        -mxt, -myt,
                        (APPLY_TRANSLATE));
            case (APPLY_IDENTITY):
                return new Affine2D();
        }

        /* NOTREACHED */
    }

    /**
     * Transforms an array of point objects by this transform.
     * If any element of the <code>ptDst</code> array is
     * <code>null</code>, a new <code>Point2D</code> object is allocated
     * and stored into that element before storing the results of the
     * transformation.
     * <p>
     * Note that this method does not take any precautions to
     * avoid problems caused by storing results into <code>Point2D</code>
     * objects that will be used as the source for calculations
     * further down the source array.
     * This method does guarantee that if a specified <code>Point2D</code>
     * object is both the source and destination for the same single point
     * transform operation then the results will not be stored until
     * the calculations are complete to avoid storing the results on
     * top of the operands.
     * If, however, the destination <code>Point2D</code> object for one
     * operation is the same object as the source <code>Point2D</code>
     * object for another operation further down the source array then
     * the original coordinates in that point are overwritten before
     * they can be converted.
     *
     * @param ptSrc  the array containing the source point objects
     * @param ptDst  the array into which the transform point objects are
     *               returned
     * @param srcOff the offset to the first point object to be
     *               transformed in the source array
     * @param dstOff the offset to the location of the first
     *               transformed point object that is stored in the destination array
     * @param numPts the number of point objects to be transformed
     */
    public void transform(Point2D[] ptSrc, int srcOff,
                          Point2D[] ptDst, int dstOff,
                          int numPts) {
        int mystate = this.state;
        while (--numPts >= 0) {
            // Copy source coords into local variables in case src == dst
            Point2D src = ptSrc[srcOff++];
            double x = src.x;
            double y = src.y;
            Point2D dst = ptDst[dstOff++];
            if (dst == null) {
                dst = new Point2D();
                ptDst[dstOff - 1] = dst;
            }
            switch (mystate) {
                default:
                    stateError();
                /* NOTREACHED */
                case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
                    dst.setLocation((float) (x * mxx + y * mxy + mxt),
                            (float) (x * myx + y * myy + myt));
                    break;
                case (APPLY_SHEAR | APPLY_SCALE):
                    dst.setLocation((float) (x * mxx + y * mxy),
                            (float) (x * myx + y * myy));
                    break;
                case (APPLY_SHEAR | APPLY_TRANSLATE):
                    dst.setLocation((float) (y * mxy + mxt),
                            (float) (x * myx + myt));
                    break;
                case (APPLY_SHEAR):
                    dst.setLocation((float) (y * mxy), (float) (x * myx));
                    break;
                case (APPLY_SCALE | APPLY_TRANSLATE):
                    dst.setLocation((float) (x * mxx + mxt), (float) (y * myy + myt));
                    break;
                case (APPLY_SCALE):
                    dst.setLocation((float) (x * mxx), (float) (y * myy));
                    break;
                case (APPLY_TRANSLATE):
                    dst.setLocation((float) (x + mxt), (float) (y + myt));
                    break;
                case (APPLY_IDENTITY):
                    dst.setLocation((float) x, (float) y);
                    break;
            }
        }

        /* NOTREACHED */
    }

    /**
     * Transforms the relative distance vector specified by
     * <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     * A relative distance vector is transformed without applying the
     * translation components of the affine transformation matrix
     * using the following equations:
     * <pre>
     *  [  x' ]   [  m00  m01 (m02) ] [  x  ]   [ m00x + m01y ]
     *  [  y' ] = [  m10  m11 (m12) ] [  y  ] = [ m10x + m11y ]
     *  [ (1) ]   [  (0)  (0) ( 1 ) ] [ (1) ]   [     (1)     ]
     * </pre>
     * If <code>ptDst</code> is <code>null</code>, a new
     * <code>Point2D</code> object is allocated and then the result of the
     * transform is stored in this object.
     * In either case, <code>ptDst</code>, which contains the
     * transformed point, is returned for convenience.
     * If <code>ptSrc</code> and <code>ptDst</code> are the same object,
     * the input point is correctly overwritten with the transformed
     * point.
     *
     * @param ptSrc the distance vector to be delta transformed
     * @param ptDst the resulting transformed distance vector
     * @return <code>ptDst</code>, which contains the result of the
     * transformation.
     */
    public Point2D deltaTransform(Point2D ptSrc, Point2D ptDst) {
        if (ptDst == null) {
            ptDst = new Point2D();
        }
        // Copy source coords into local variables in case src == dst
        double x = ptSrc.x;
        double y = ptSrc.y;
        switch (state) {
            default:
                stateError();
            /* NOTREACHED */
            case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            case (APPLY_SHEAR | APPLY_SCALE):
                ptDst.setLocation((float) (x * mxx + y * mxy), (float) (x * myx + y * myy));
                return ptDst;
            case (APPLY_SHEAR | APPLY_TRANSLATE):
            case (APPLY_SHEAR):
                ptDst.setLocation((float) (y * mxy), (float) (x * myx));
                return ptDst;
            case (APPLY_SCALE | APPLY_TRANSLATE):
            case (APPLY_SCALE):
                ptDst.setLocation((float) (x * mxx), (float) (y * myy));
                return ptDst;
            case (APPLY_TRANSLATE):
            case (APPLY_IDENTITY):
                ptDst.setLocation((float) x, (float) y);
                return ptDst;
        }

        /* NOTREACHED */
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
        return ("Affine2D[["
                + _matround(mxx) + ", "
                + _matround(mxy) + ", "
                + _matround(mxt) + "], ["
                + _matround(myx) + ", "
                + _matround(myy) + ", "
                + _matround(myt) + "]]");
    }

    @Override
    public boolean is2D() {
        return true;
    }

    @Override
    public void restoreTransform(double mxx, double myx,
                                 double mxy, double myy,
                                 double mxt, double myt) {
        setTransform(mxx, myx, mxy, myy, mxt, myt);
    }

    @Override
    public void restoreTransform(double mxx, double mxy, double mxz, double mxt,
                                 double myx, double myy, double myz, double myt,
                                 double mzx, double mzy, double mzz, double mzt) {
        if (mxz != 0 ||
                myz != 0 ||
                mzx != 0 || mzy != 0 || mzz != 1 || mzt != 0.0) {
            degreeError(Degree.AFFINE_2D);
        }
        setTransform(mxx, myx, mxy, myy, mxt, myt);
    }

    @Override
    public BaseTransform deriveWithTranslation(double mxt, double myt) {
        translate(mxt, myt);
        return this;
    }

    @Override
    public BaseTransform deriveWithTranslation(double mxt, double myt, double mzt) {
        if (mzt == 0.0) {
            translate(mxt, myt);
            return this;
        }
        Affine3D a = new Affine3D(this);
        a.translate(mxt, myt, mzt);
        return a;
    }

    @Override
    public BaseTransform deriveWithScale(double mxx, double myy, double mzz) {
        if (mzz == 1.0) {
            scale(mxx, myy);
            return this;
        }
        Affine3D a = new Affine3D(this);
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
            if (axisZ > 0) {
                rotate(theta);
            } else if (axisZ < 0) {
                rotate(-theta);
            } // else rotating about zero vector - NOP
            return this;
        }
        Affine3D a = new Affine3D(this);
        a.rotate(theta, axisX, axisY, axisZ);
        return a;
    }

    @Override
    public BaseTransform deriveWithPreTranslation(double mxt, double myt) {
        this.mxt += mxt;
        this.myt += myt;
        if (this.mxt != 0.0 || this.myt != 0.0) {
            state |= APPLY_TRANSLATE;
//            if (type != TYPE_UNKNOWN) {
            type |= TYPE_TRANSLATION;
//            }
        } else {
            state &= ~APPLY_TRANSLATE;
            if (type != TYPE_UNKNOWN) {
                type &= ~TYPE_TRANSLATION;
            }
        }
        return this;
    }

    @Override
    public BaseTransform deriveWithConcatenation(double mxx, double myx,
                                                 double mxy, double myy,
                                                 double mxt, double myt) {
        // TODO: Simplify this (RT-26801)
        BaseTransform tmpTx = getInstance(mxx, myx,
                mxy, myy,
                mxt, myt);
        concatenate(tmpTx);
        return this;
    }

    @Override
    public BaseTransform deriveWithConcatenation(
            double mxx, double mxy, double mxz, double mxt,
            double myx, double myy, double myz, double myt,
            double mzx, double mzy, double mzz, double mzt) {
        if (mxz == 0.0
                && myz == 0.0
                && mzx == 0.0 && mzy == 0.0 && mzz == 1.0 && mzt == 0.0) {
            concatenate(mxx, mxy,
                    mxt, myx,
                    myy, myt);
            return this;
        }

        Affine3D t3d = new Affine3D(this);
        t3d.concatenate(mxx, mxy, mxz, mxt,
                myx, myy, myz, myt,
                mzx, mzy, mzz, mzt);
        return t3d;
    }

    @Override
    public BaseTransform deriveWithConcatenation(BaseTransform tx) {
        if (tx.is2D()) {
            concatenate(tx);
            return this;
        }
        Affine3D t3d = new Affine3D(this);
        t3d.concatenate(tx);
        return t3d;
    }

    @Override
    public BaseTransform deriveWithPreConcatenation(BaseTransform tx) {
        if (tx.is2D()) {
            preConcatenate(tx);
            return this;
        }
        Affine3D t3d = new Affine3D(this);
        t3d.preConcatenate(tx);
        return t3d;
    }

    @Override
    public BaseTransform deriveWithNewTransform(BaseTransform tx) {
        if (tx.is2D()) {
            setTransform(tx);
            return this;
        }
        return getInstance(tx);
    }

    @Override
    public BaseTransform copy() {
        return new Affine2D(this);
    }

    private static final long BASE_HASH;

    static {
        long bits = 0;
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMzz());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMzy());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMzx());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMyz());
        bits = bits * 31 + Double.doubleToLongBits(IDENTITY_TRANSFORM.getMxz());
        BASE_HASH = bits;
    }

    /**
     * Returns the hashcode for this transform.  The base algorithm for
     * computing the hashcode is defined by the implementation in
     * the {@code BaseTransform} class.  This implementation is just a
     * faster way of computing the same value knowing which elements of
     * the transform matrix are populated.
     *
     * @return a hash code for this transform.
     */
    @Override
    public int hashCode() {
        if (isIdentity()) return 0;
        long bits = BASE_HASH;
        bits = bits * 31 + Double.doubleToLongBits(getMyy());
        bits = bits * 31 + Double.doubleToLongBits(getMyx());
        bits = bits * 31 + Double.doubleToLongBits(getMxy());
        bits = bits * 31 + Double.doubleToLongBits(getMxx());
        bits = bits * 31 + Double.doubleToLongBits(0.0); // mzt
        bits = bits * 31 + Double.doubleToLongBits(getMyt());
        bits = bits * 31 + Double.doubleToLongBits(getMxt());
        return (((int) bits) ^ ((int) (bits >> 32)));
    }

    /**
     * Returns <code>true</code> if this <code>Affine2D</code>
     * represents the same coordinate transform as the specified
     * argument.
     *
     * @param obj the <code>Object</code> to test for equality with this
     *            <code>Affine2D</code>
     * @return <code>true</code> if <code>obj</code> equals this
     * <code>Affine2D</code> object; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseTransform) {
            BaseTransform a = (BaseTransform) obj;
            return (a.getType() <= TYPE_AFFINE2D_MASK &&
                    a.getMxx() == this.mxx &&
                    a.getMxy() == this.mxy &&
                    a.getMxt() == this.mxt &&
                    a.getMyx() == this.myx &&
                    a.getMyy() == this.myy &&
                    a.getMyt() == this.myt);
        }
        return false;
    }
}
