/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.Vec3f;

/**
 * A general-purpose 4x4 transform that may or may not be affine. The
 * GeneralTransform is typically used only for projection transforms.
 *
 */
public final class GeneralTransform3D implements CanTransformVec3d {

    //The 4x4 double-precision floating point matrix.  The mathematical
    //representation is row major, as in traditional matrix mathematics.
    protected double[] mat = new double[16];

    //flag if this is an identity transformation.
    private boolean identity;

    /**
     * Constructs and initializes a transform to the identity matrix.
     */
    public GeneralTransform3D() {
        setIdentity();
    }

    /**
     * Returns true if the transform is affine. A transform is considered
     * to be affine if the last row of its matrix is (0,0,0,1). Note that
     * an instance of AffineTransform3D is always affine.
     */
    public boolean isAffine() {
        if (!isInfOrNaN() &&
                almostZero(mat[12]) &&
                almostZero(mat[13]) &&
                almostZero(mat[14]) &&
                almostOne(mat[15])) {

            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the value of this transform to the specified transform.
     *
     * @param t1 the transform to copy into this transform.
     *
     * @return this transform
     */
    public GeneralTransform3D set(GeneralTransform3D t1) {
        System.arraycopy(t1.mat, 0, mat, 0, mat.length);
        updateState();
        return this;
    }

    /**
     * Sets the matrix values of this transform to the values in the
     * specified array.
     *
     * @param m an array of 16 values to copy into this transform in
     * row major order.
     *
     * @return this transform
     */
    public GeneralTransform3D set(double[] m) {
        System.arraycopy(m, 0, mat, 0, mat.length);
        updateState();
        return this;
    }

    /**
     * Returns a copy of an array of 16 values that contains the 4x4 matrix
     * of this transform. The first four elements of the array will contain
     * the top row of the transform matrix, etc.
     *
     * @param rv the return value, or null
     *
     * @return an array of 16 values
     */
    public double[] get(double[] rv) {
        if (rv == null) {
            rv = new double[mat.length];
        }
        System.arraycopy(mat, 0, rv, 0, mat.length);

        return rv;
    }

    public double get(int index) {
        assert ((index >= 0) && (index < mat.length));
        return mat[index];
    }

    private Vec3d tempV3d;

    public BaseBounds transform(BaseBounds src, BaseBounds dst) {
        if (tempV3d == null) {
            tempV3d = new Vec3d();
        }
        return TransformHelper.general3dBoundsTransform(this, src, dst, tempV3d);
    }

    /**
     * Transform 2D point (with z == 0)
     * @param point
     * @param pointOut
     * @return
     */
    public Point2D transform(Point2D point, Point2D pointOut) {
        if (pointOut == null) {
            pointOut = new Point2D();
        }

        double w = mat[12] * point.x + mat[13] * point.y + mat[15];
        float outX = (float) (mat[0] * point.x + mat[1] * point.y + mat[3]);
        pointOut.y = (float) (mat[4] * point.x + mat[5] * point.y + mat[7]);

        pointOut.x = outX;
        if (w != 0.0) {
            pointOut.x /= w;
            pointOut.y /= w;
        }

        return pointOut;
    }

    /**
     * Transforms the point parameter with this transform and
     * places the result into pointOut.  The fourth element of the
     * point input parameter is assumed to be one.
     *
     * @param point the input point to be transformed
     *
     * @param pointOut the transformed point
     *
     * @return the transformed point
     */
    public Vec3d transform(Vec3d point, Vec3d pointOut)  {
        if (pointOut == null) {
            pointOut = new Vec3d();
        }

        double w = mat[12] * point.x + mat[13] * point.y
                + mat[14] * point.z + mat[15];
        double outX = mat[0] * point.x + mat[1] * point.y
                + mat[2] * point.z + mat[3];
        double outY = mat[4] * point.x + mat[5] * point.y
                + mat[6] * point.z + mat[7];
        pointOut.z = mat[8] * point.x + mat[9] * point.y
                + mat[10] * point.z + mat[11];

        pointOut.x = outX;
        pointOut.y = outY;
        if (w != 0.0) {
            pointOut.x /= w;
            pointOut.y /= w;
            pointOut.z /= w;
        }

        return pointOut;
    }


    /**
     * Transforms the point parameter with this transform and
     * places the result back into point.  The fourth element of the
     * point input parameter is assumed to be one.
     *
     * @param point the input point to be transformed
     *
     * @return the transformed point
     */
    public Vec3d transform(Vec3d point) {
        return transform(point, point);
    }

    /**
     * Transforms the normal parameter by this transform and places the value
     * into normalOut.  The fourth element of the normal is assumed to be zero.
     * Note: For correct lighting results, if a transform has uneven scaling
     * surface normals should transformed by the inverse transpose of
     * the transform. This the responsibility of the application and is not
     * done automatically by this method.
     *
     * @param normal the input normal to be transformed
     *
     * @param normalOut the transformed normal
     *
     * @return the transformed normal
     */
    public Vec3f transformNormal(Vec3f normal, Vec3f normalOut) {
        if (normalOut == null) {
            normalOut = new Vec3f();
        }

        float outX = (float) (mat[0] * normal.x + mat[1] * normal.y +
                            mat[2] * normal.z);
        float outY = (float) (mat[4] * normal.x + mat[5] * normal.y +
                            mat[6] * normal.z);
        normalOut.z = (float) (mat[8] * normal.x + mat[9] * normal.y +
                            mat[10] * normal.z);

        normalOut.x = outX;
        normalOut.y = outY;
        return normalOut;
    }

    /**
     * Transforms the normal parameter by this transform and places the value
     * back into normal.  The fourth element of the normal is assumed to be zero.
     * Note: For correct lighting results, if a transform has uneven scaling
     * surface normals should transformed by the inverse transpose of
     * the transform. This the responsibility of the application and is not
     * done automatically by this method.
     *
     * @param normal the input normal to be transformed
     *
     * @return the transformed normal
     */
    public Vec3f transformNormal(Vec3f normal) {
        return transformNormal(normal, normal);
    }

    /**
     * Sets the value of this transform to a perspective projection transform.
     * This transform maps points from Eye Coordinates (EC)
     * to Clipping Coordinates (CC).
     * Note that the field of view is specified in radians.
     *
     * @param verticalFOV specifies whether the fov is vertical (Y direction).
     *
     * @param fov specifies the field of view in radians
     *
     * @param aspect specifies the aspect ratio. The aspect ratio is the ratio
     * of width to height.
     *
     * @param zNear the distance to the frustum's near clipping plane.
     * This value must be positive, (the value -zNear is the location of the
     * near clip plane).
     *
     * @param zFar the distance to the frustum's far clipping plane
     *
     * @return this transform
     */
    public GeneralTransform3D perspective(boolean verticalFOV,
            double fov, double aspect, double zNear, double zFar) {
        double sine;
        double cotangent;
        double deltaZ;
        double half_fov = fov * 0.5;

        deltaZ = zFar - zNear;
        sine = Math.sin(half_fov);

        cotangent = Math.cos(half_fov) / sine;

        mat[0] = verticalFOV ? cotangent / aspect : cotangent;
        mat[5] = verticalFOV ? cotangent : cotangent * aspect;
        mat[10] = -(zFar + zNear) / deltaZ;
        mat[11] = -2.0 * zNear * zFar / deltaZ;
        mat[14] = -1.0;
        mat[1] = mat[2] = mat[3] = mat[4] = mat[6] = mat[7] = mat[8] = mat[9] = mat[12] = mat[13] = mat[15] = 0;

        updateState();
        return this;
    }

    /**
     * Sets the value of this transform to an orthographic (parallel)
     * projection transform.
     * This transform maps coordinates from Eye Coordinates (EC)
     * to Clipping Coordinates (CC).  Note that unlike the similar function
     * in OpenGL, the clipping coordinates generated by the resulting
     * transform are in a right-handed coordinate system.
     * @param left the vertical line on the left edge of the near
     * clipping plane mapped to the left edge of the graphics window
     * @param right the vertical line on the right edge of the near
     * clipping plane mapped to the right edge of the graphics window
     * @param bottom the horizontal line on the bottom edge of the near
     * clipping plane mapped to the bottom edge of the graphics window
     * @param top the horizontal line on the top edge of the near
     * clipping plane mapped to the top edge of the graphics window
     * @param near the distance to the frustum's near clipping plane
     * (the value -near is the location of the near clip plane)
     * @param far the distance to the frustum's far clipping plane
     *
     * @return this transform
     */
    public GeneralTransform3D ortho(double left, double right, double bottom,
                                    double top, double near, double far) {
        double deltax = 1 / (right - left);
        double deltay = 1 / (top - bottom);
        double deltaz = 1 / (far - near);

        mat[0] = 2.0 * deltax;
        mat[3] = -(right + left) * deltax;
        mat[5] = 2.0 * deltay;
        mat[7] = -(top + bottom) * deltay;
        mat[10] = 2.0 * deltaz;
        mat[11] = (far + near) * deltaz;
        mat[1] = mat[2] = mat[4] = mat[6] = mat[8] =
                mat[9] = mat[12] = mat[13] = mat[14] = 0;
        mat[15] = 1;

        updateState();
        return this;
    }

    public double computeClipZCoord() {
        double zEc = (1.0 - mat[15]) / mat[14];
        double zCc = mat[10] * zEc + mat[11];
        return zCc;
    }

    /**
     * Computes the determinant of this transform.
     *
     * @return the determinant
     */
    public double determinant() {
         // cofactor exapainsion along first row
         return mat[0]*(mat[5]*(mat[10]*mat[15] - mat[11]*mat[14]) -
                        mat[6]*(mat[ 9]*mat[15] - mat[11]*mat[13]) +
                        mat[7]*(mat[ 9]*mat[14] - mat[10]*mat[13])) -
                mat[1]*(mat[4]*(mat[10]*mat[15] - mat[11]*mat[14]) -
                        mat[6]*(mat[ 8]*mat[15] - mat[11]*mat[12]) +
                        mat[7]*(mat[ 8]*mat[14] - mat[10]*mat[12])) +
                mat[2]*(mat[4]*(mat[ 9]*mat[15] - mat[11]*mat[13]) -
                        mat[5]*(mat[ 8]*mat[15] - mat[11]*mat[12]) +
                        mat[7]*(mat[ 8]*mat[13] - mat[ 9]*mat[12])) -
                mat[3]*(mat[4]*(mat[ 9]*mat[14] - mat[10]*mat[13]) -
                        mat[5]*(mat[ 8]*mat[14] - mat[10]*mat[12]) +
                        mat[6]*(mat[ 8]*mat[13] - mat[ 9]*mat[12]));
    }

    /**
     * Inverts this transform in place.
     *
     * @return this transform
     */
    public GeneralTransform3D invert() {
        return invert(this);
    }

    /**
     * General invert routine.  Inverts t1 and places the result in "this".
     * Note that this routine handles both the "this" version and the
     * non-"this" version.
     *
     * Also note that since this routine is slow anyway, we won't worry
     * about allocating a little bit of garbage.
     */
    private GeneralTransform3D invert(GeneralTransform3D t1) {
        double[] tmp = new double[16];
        int[] row_perm = new int[4];

        // Use LU decomposition and backsubstitution code specifically
        // for floating-point 4x4 matrices.
        // Copy source matrix to tmp
        System.arraycopy(t1.mat, 0, tmp, 0, tmp.length);

        // Calculate LU decomposition: Is the matrix singular?
        if (!luDecomposition(tmp, row_perm)) {
            // Matrix has no inverse
            throw new SingularMatrixException();
        }

        // Perform back substitution on the identity matrix
        // luDecomposition will set rot[] & scales[] for use
        // in luBacksubstituation
        mat[0] = 1.0;  mat[1] = 0.0;  mat[2] = 0.0;  mat[3] = 0.0;
        mat[4] = 0.0;  mat[5] = 1.0;  mat[6] = 0.0;  mat[7] = 0.0;
        mat[8] = 0.0;  mat[9] = 0.0;  mat[10] = 1.0; mat[11] = 0.0;
        mat[12] = 0.0; mat[13] = 0.0; mat[14] = 0.0; mat[15] = 1.0;
        luBacksubstitution(tmp, row_perm, this.mat);

        updateState();
        return this;
    }

    /**
     * Given a 4x4 array "matrix0", this function replaces it with the
     * LU decomposition of a row-wise permutation of itself.  The input
     * parameters are "matrix0" and "dimen".  The array "matrix0" is also
     * an output parameter.  The vector "row_perm[4]" is an output
     * parameter that contains the row permutations resulting from partial
     * pivoting.  The output parameter "even_row_xchg" is 1 when the
     * number of row exchanges is even, or -1 otherwise.  Assumes data
     * type is always double.
     *
     * This function is similar to luDecomposition, except that it
     * is tuned specifically for 4x4 matrices.
     *
     * @return true if the matrix is nonsingular, or false otherwise.
     */
    private static boolean luDecomposition(double[] matrix0,
            int[] row_perm) {

        // Reference: Press, Flannery, Teukolsky, Vetterling,
        //            _Numerical_Recipes_in_C_, Cambridge University Press,
        //            1988, pp 40-45.
        //

        // Can't re-use this temporary since the method is static.
        double row_scale[] = new double[4];

        // Determine implicit scaling information by looping over rows
        {
            int i, j;
            int ptr, rs;
            double big, temp;

            ptr = 0;
            rs = 0;

            // For each row ...
            i = 4;
            while (i-- != 0) {
                big = 0.0;

                // For each column, find the largest element in the row
                j = 4;
                while (j-- != 0) {
                    temp = matrix0[ptr++];
                    temp = Math.abs(temp);
                    if (temp > big) {
                        big = temp;
                    }
                }

                // Is the matrix singular?
                if (big == 0.0) {
                    return false;
                }
                row_scale[rs++] = 1.0 / big;
            }
        }

        {
            int j;
            int mtx;

            mtx = 0;

            // For all columns, execute Crout's method
            for (j = 0; j < 4; j++) {
                int i, imax, k;
                int target, p1, p2;
                double sum, big, temp;

                // Determine elements of upper diagonal matrix U
                for (i = 0; i < j; i++) {
                    target = mtx + (4*i) + j;
                    sum = matrix0[target];
                    k = i;
                    p1 = mtx + (4*i);
                    p2 = mtx + j;
                    while (k-- != 0) {
                        sum -= matrix0[p1] * matrix0[p2];
                        p1++;
                        p2 += 4;
                    }
                    matrix0[target] = sum;
                }

                // Search for largest pivot element and calculate
                // intermediate elements of lower diagonal matrix L.
                big = 0.0;
                imax = -1;
                for (i = j; i < 4; i++) {
                    target = mtx + (4*i) + j;
                    sum = matrix0[target];
                    k = j;
                    p1 = mtx + (4*i);
                    p2 = mtx + j;
                    while (k-- != 0) {
                        sum -= matrix0[p1] * matrix0[p2];
                        p1++;
                        p2 += 4;
                    }
                    matrix0[target] = sum;

                    // Is this the best pivot so far?
                    if ((temp = row_scale[i] * Math.abs(sum)) >= big) {
                        big = temp;
                        imax = i;
                    }
                }

                if (imax < 0) {
                    return false;
                }

                // Is a row exchange necessary?
                if (j != imax) {
                    // Yes: exchange rows
                    k = 4;
                    p1 = mtx + (4*imax);
                    p2 = mtx + (4*j);
                    while (k-- != 0) {
                        temp = matrix0[p1];
                        matrix0[p1++] = matrix0[p2];
                        matrix0[p2++] = temp;
                    }

                    // Record change in scale factor
                    row_scale[imax] = row_scale[j];
                }

                // Record row permutation
                row_perm[j] = imax;

                // Is the matrix singular
                if (matrix0[(mtx + (4*j) + j)] == 0.0) {
                    return false;
                }

                // Divide elements of lower diagonal matrix L by pivot
                if (j != (4-1)) {
                    temp = 1.0 / (matrix0[(mtx + (4*j) + j)]);
                    target = mtx + (4*(j+1)) + j;
                    i = 3 - j;
                    while (i-- != 0) {
                        matrix0[target] *= temp;
                        target += 4;
                    }
                }
            }
        }

        return true;
    }


    /**
     * Solves a set of linear equations.  The input parameters "matrix1",
     * and "row_perm" come from luDecompostionD4x4 and do not change
     * here.  The parameter "matrix2" is a set of column vectors assembled
     * into a 4x4 matrix of floating-point values.  The procedure takes each
     * column of "matrix2" in turn and treats it as the right-hand side of the
     * matrix equation Ax = LUx = b.  The solution vector replaces the
     * original column of the matrix.
     *
     * If "matrix2" is the identity matrix, the procedure replaces its contents
     * with the inverse of the matrix from which "matrix1" was originally
     * derived.
     */
    //
    // Reference: Press, Flannery, Teukolsky, Vetterling,
    //        _Numerical_Recipes_in_C_, Cambridge University Press,
    //        1988, pp 44-45.
    //
    private static void luBacksubstitution(double[] matrix1,
            int[] row_perm,
            double[] matrix2) {

        int i, ii, ip, j, k;
        int rp;
        int cv, rv;

        //      rp = row_perm;
        rp = 0;

        // For each column vector of matrix2 ...
        for (k = 0; k < 4; k++) {
            //      cv = &(matrix2[0][k]);
            cv = k;
            ii = -1;

            // Forward substitution
            for (i = 0; i < 4; i++) {
                double sum;

                ip = row_perm[rp+i];
                sum = matrix2[cv+4*ip];
                matrix2[cv+4*ip] = matrix2[cv+4*i];
                if (ii >= 0) {
                    //              rv = &(matrix1[i][0]);
                    rv = i*4;
                    for (j = ii; j <= i-1; j++) {
                        sum -= matrix1[rv+j] * matrix2[cv+4*j];
                    }
                }
                else if (sum != 0.0) {
                    ii = i;
                }
                matrix2[cv+4*i] = sum;
            }

            // Backsubstitution
            //      rv = &(matrix1[3][0]);
            rv = 3*4;
            matrix2[cv+4*3] /= matrix1[rv+3];

            rv -= 4;
            matrix2[cv+4*2] = (matrix2[cv+4*2] -
                            matrix1[rv+3] * matrix2[cv+4*3]) / matrix1[rv+2];

            rv -= 4;
            matrix2[cv+4*1] = (matrix2[cv+4*1] -
                            matrix1[rv+2] * matrix2[cv+4*2] -
                            matrix1[rv+3] * matrix2[cv+4*3]) / matrix1[rv+1];

            rv -= 4;
            matrix2[cv+4*0] = (matrix2[cv+4*0] -
                            matrix1[rv+1] * matrix2[cv+4*1] -
                            matrix1[rv+2] * matrix2[cv+4*2] -
                            matrix1[rv+3] * matrix2[cv+4*3]) / matrix1[rv+0];
        }
    }


    /**
     * Sets the value of this transform to the result of multiplying itself
     * with transform t1 : this = this * t1.
      *
     * @param t1 the other transform
     *
     * @return this transform
     */
    public GeneralTransform3D mul(BaseTransform t1) {
        if (t1.isIdentity()) {
            return this;
        }

        double tmp0, tmp1, tmp2, tmp3;
        double tmp4, tmp5, tmp6, tmp7;
        double tmp8, tmp9, tmp10, tmp11;
        double tmp12, tmp13, tmp14, tmp15;

        double mxx = t1.getMxx();
        double mxy = t1.getMxy();
        double mxz = t1.getMxz();
        double mxt = t1.getMxt();
        double myx = t1.getMyx();
        double myy = t1.getMyy();
        double myz = t1.getMyz();
        double myt = t1.getMyt();
        double mzx = t1.getMzx();
        double mzy = t1.getMzy();
        double mzz = t1.getMzz();
        double mzt = t1.getMzt();

        tmp0 = mat[0] * mxx + mat[1] * myx + mat[2] * mzx;
        tmp1 = mat[0] * mxy + mat[1] * myy + mat[2] * mzy;
        tmp2 = mat[0] * mxz + mat[1] * myz + mat[2] * mzz;
        tmp3 = mat[0] * mxt + mat[1] * myt + mat[2] * mzt + mat[3];
        tmp4 = mat[4] * mxx + mat[5] * myx + mat[6] * mzx;
        tmp5 = mat[4] * mxy + mat[5] * myy + mat[6] * mzy;
        tmp6 = mat[4] * mxz + mat[5] * myz + mat[6] * mzz;
        tmp7 = mat[4] * mxt + mat[5] * myt + mat[6] * mzt + mat[7];
        tmp8 = mat[8] * mxx + mat[9] * myx + mat[10] * mzx;
        tmp9 = mat[8] * mxy + mat[9] * myy + mat[10] * mzy;
        tmp10 = mat[8] * mxz + mat[9] * myz + mat[10] * mzz;
        tmp11 = mat[8] * mxt + mat[9] * myt + mat[10] * mzt + mat[11];
        if (isAffine()) {
            tmp12 = tmp13 = tmp14 = 0;
            tmp15 = 1;
        }
        else {
            tmp12 = mat[12] * mxx + mat[13] * myx + mat[14] * mzx;
            tmp13 = mat[12] * mxy + mat[13] * myy + mat[14] * mzy;
            tmp14 = mat[12] * mxz + mat[13] * myz + mat[14] * mzz;
            tmp15 = mat[12] * mxt + mat[13] * myt + mat[14] * mzt + mat[15];
        }

        mat[0] = tmp0;
        mat[1] = tmp1;
        mat[2] = tmp2;
        mat[3] = tmp3;
        mat[4] = tmp4;
        mat[5] = tmp5;
        mat[6] = tmp6;
        mat[7] = tmp7;
        mat[8] = tmp8;
        mat[9] = tmp9;
        mat[10] = tmp10;
        mat[11] = tmp11;
        mat[12] = tmp12;
        mat[13] = tmp13;
        mat[14] = tmp14;
        mat[15] = tmp15;

        updateState();
        return this;
    }

    /**
     * Sets the value of this transform to the result of multiplying itself
     * with a scale transform:
     * <pre>
     * scaletx =
     *     [ sx  0  0  0 ]
     *     [  0 sy  0  0 ]
     *     [  0  0 sz  0 ]
     *     [  0  0  0  1 ].
     * this = this * scaletx.
     * </pre>
     *
     * @param sx the X coordinate scale factor
     * @param sy the Y coordinate scale factor
     * @param sz the Z coordinate scale factor
     *
     * @return this transform
     */
    public GeneralTransform3D scale(double sx, double sy, double sz) {
        boolean update = false;

        if (sx != 1.0) {
            mat[0]  *= sx;
            mat[4]  *= sx;
            mat[8]  *= sx;
            mat[12] *= sx;
            update = true;
        }
        if (sy != 1.0) {
            mat[1]  *= sy;
            mat[5]  *= sy;
            mat[9]  *= sy;
            mat[13] *= sy;
            update = true;
        }
        if (sz != 1.0) {
            mat[2]  *= sz;
            mat[6]  *= sz;
            mat[10] *= sz;
            mat[14] *= sz;
            update = true;
        }

        if (update) {
            updateState();
        }
        return this;
    }

    /**
     * Sets the value of this transform to the result of multiplying itself
     * with transform t1 : this = this * t1.
      *
     * @param t1 the other transform
     *
     * @return this transform
     */
    public GeneralTransform3D mul(GeneralTransform3D t1) {
        if (t1.isIdentity()) {
            return this;
        }

        double tmp0, tmp1, tmp2, tmp3;
        double tmp4, tmp5, tmp6, tmp7;
        double tmp8, tmp9, tmp10, tmp11;
        double tmp12, tmp13, tmp14, tmp15;

        if (t1.isAffine()) {
            tmp0 = mat[0] * t1.mat[0] + mat[1] * t1.mat[4] + mat[2] * t1.mat[8];
            tmp1 = mat[0] * t1.mat[1] + mat[1] * t1.mat[5] + mat[2] * t1.mat[9];
            tmp2 = mat[0] * t1.mat[2] + mat[1] * t1.mat[6] + mat[2] * t1.mat[10];
            tmp3 = mat[0] * t1.mat[3] + mat[1] * t1.mat[7] + mat[2] * t1.mat[11] + mat[3];
            tmp4 = mat[4] * t1.mat[0] + mat[5] * t1.mat[4] + mat[6] * t1.mat[8];
            tmp5 = mat[4] * t1.mat[1] + mat[5] * t1.mat[5] + mat[6] * t1.mat[9];
            tmp6 = mat[4] * t1.mat[2] + mat[5] * t1.mat[6] + mat[6] * t1.mat[10];
            tmp7 = mat[4] * t1.mat[3] + mat[5] * t1.mat[7] + mat[6] * t1.mat[11] + mat[7];
            tmp8 = mat[8] * t1.mat[0] + mat[9] * t1.mat[4] + mat[10] * t1.mat[8];
            tmp9 = mat[8] * t1.mat[1] + mat[9] * t1.mat[5] + mat[10] * t1.mat[9];
            tmp10 = mat[8] * t1.mat[2] + mat[9] * t1.mat[6] + mat[10] * t1.mat[10];
            tmp11 = mat[8] * t1.mat[3] + mat[9] * t1.mat[7] + mat[10] * t1.mat[11] + mat[11];
            if (isAffine()) {
                tmp12 = tmp13 = tmp14 = 0;
                tmp15 = 1;
            }
            else {
                tmp12 = mat[12] * t1.mat[0] + mat[13] * t1.mat[4] +
                        mat[14] * t1.mat[8];
                tmp13 = mat[12] * t1.mat[1] + mat[13] * t1.mat[5] +
                        mat[14] * t1.mat[9];
                tmp14 = mat[12] * t1.mat[2] + mat[13] * t1.mat[6] +
                        mat[14] * t1.mat[10];
                tmp15 = mat[12] * t1.mat[3] + mat[13] * t1.mat[7] +
                        mat[14] * t1.mat[11] + mat[15];
            }
        } else {
            tmp0 = mat[0] * t1.mat[0] + mat[1] * t1.mat[4] + mat[2] * t1.mat[8] +
                    mat[3] * t1.mat[12];
            tmp1 = mat[0] * t1.mat[1] + mat[1] * t1.mat[5] + mat[2] * t1.mat[9] +
                    mat[3] * t1.mat[13];
            tmp2 = mat[0] * t1.mat[2] + mat[1] * t1.mat[6] + mat[2] * t1.mat[10] +
                    mat[3] * t1.mat[14];
            tmp3 = mat[0] * t1.mat[3] + mat[1] * t1.mat[7] + mat[2] * t1.mat[11] +
                    mat[3] * t1.mat[15];
            tmp4 = mat[4] * t1.mat[0] + mat[5] * t1.mat[4] + mat[6] * t1.mat[8] +
                    mat[7] * t1.mat[12];
            tmp5 = mat[4] * t1.mat[1] + mat[5] * t1.mat[5] + mat[6] * t1.mat[9] +
                    mat[7] * t1.mat[13];
            tmp6 = mat[4] * t1.mat[2] + mat[5] * t1.mat[6] + mat[6] * t1.mat[10] +
                    mat[7] * t1.mat[14];
            tmp7 = mat[4] * t1.mat[3] + mat[5] * t1.mat[7] + mat[6] * t1.mat[11] +
                    mat[7] * t1.mat[15];
            tmp8 = mat[8] * t1.mat[0] + mat[9] * t1.mat[4] + mat[10] * t1.mat[8] +
                    mat[11] * t1.mat[12];
            tmp9 = mat[8] * t1.mat[1] + mat[9] * t1.mat[5] + mat[10] * t1.mat[9] +
                    mat[11] * t1.mat[13];
            tmp10 = mat[8] * t1.mat[2] + mat[9] * t1.mat[6] +
                    mat[10] * t1.mat[10] + mat[11] * t1.mat[14];

            tmp11 = mat[8] * t1.mat[3] + mat[9] * t1.mat[7] +
                    mat[10] * t1.mat[11] + mat[11] * t1.mat[15];
            if (isAffine()) {
                tmp12 = t1.mat[12];
                tmp13 = t1.mat[13];
                tmp14 = t1.mat[14];
                tmp15 = t1.mat[15];
            } else {
                tmp12 = mat[12] * t1.mat[0] + mat[13] * t1.mat[4] +
                        mat[14] * t1.mat[8] + mat[15] * t1.mat[12];
                tmp13 = mat[12] * t1.mat[1] + mat[13] * t1.mat[5] +
                        mat[14] * t1.mat[9] + mat[15] * t1.mat[13];
                tmp14 = mat[12] * t1.mat[2] + mat[13] * t1.mat[6] +
                        mat[14] * t1.mat[10] + mat[15] * t1.mat[14];
                tmp15 = mat[12] * t1.mat[3] + mat[13] * t1.mat[7] +
                        mat[14] * t1.mat[11] + mat[15] * t1.mat[15];
            }
        }

        mat[0] = tmp0;
        mat[1] = tmp1;
        mat[2] = tmp2;
        mat[3] = tmp3;
        mat[4] = tmp4;
        mat[5] = tmp5;
        mat[6] = tmp6;
        mat[7] = tmp7;
        mat[8] = tmp8;
        mat[9] = tmp9;
        mat[10] = tmp10;
        mat[11] = tmp11;
        mat[12] = tmp12;
        mat[13] = tmp13;
        mat[14] = tmp14;
        mat[15] = tmp15;

        updateState();
        return this;
    }

    /**
     * Sets this transform to the identity matrix.
     *
     * @return this transform
     */
    public GeneralTransform3D setIdentity() {
        mat[0] = 1.0;  mat[1] = 0.0;  mat[2] = 0.0;  mat[3] = 0.0;
        mat[4] = 0.0;  mat[5] = 1.0;  mat[6] = 0.0;  mat[7] = 0.0;
        mat[8] = 0.0;  mat[9] = 0.0;  mat[10] = 1.0; mat[11] = 0.0;
        mat[12] = 0.0; mat[13] = 0.0; mat[14] = 0.0; mat[15] = 1.0;
        identity = true;
        return this;
    }

    /**
     * Returns true if the transform is identity. A transform is considered
     * to be identity if the diagonal elements of its matrix is all 1s
     * otherwise 0s.
     */
    public boolean isIdentity() {
        return identity;
    }

    private void updateState() {
        //set the identity flag.
        identity =
            mat[0]  == 1.0 && mat[5]  == 1.0 && mat[10] == 1.0 && mat[15] == 1.0 &&
            mat[1]  == 0.0 && mat[2]  == 0.0 && mat[3]  == 0.0 &&
            mat[4]  == 0.0 && mat[6]  == 0.0 && mat[7]  == 0.0 &&
            mat[8]  == 0.0 && mat[9]  == 0.0 && mat[11] == 0.0 &&
            mat[12] == 0.0 && mat[13] == 0.0 && mat[14] == 0.0;
    }

    // Check whether matrix has an Infinity or NaN value. If so, don't treat it
    // as affine.
    boolean isInfOrNaN() {
        // The following is a faster version of the check.
        // Instead of 3 tests per array element (Double.isInfinite is 2 tests),
        // for a total of 48 tests, we will do 16 multiplies and 1 test.
        double d = 0.0;
        for (int i = 0; i < mat.length; i++) {
            d *= mat[i];
        }

        return d != 0.0;
    }

    private static final double EPSILON_ABSOLUTE = 1.0e-5;

    static boolean almostZero(double a) {
        return ((a < EPSILON_ABSOLUTE) && (a > -EPSILON_ABSOLUTE));
    }

    static boolean almostOne(double a) {
        return ((a < 1+EPSILON_ABSOLUTE) && (a > 1-EPSILON_ABSOLUTE));
    }

    public GeneralTransform3D copy() {
        GeneralTransform3D newTransform = new GeneralTransform3D();
        newTransform.set(this);
        return newTransform;
    }

    /**
     * Returns the matrix elements of this transform as a string.
     * @return  the matrix elements of this transform
     */
    @Override
    public String toString() {
        return mat[0] + ", " + mat[1] + ", " + mat[2] + ", " + mat[3] + "\n" +
                mat[4] + ", " + mat[5] + ", " + mat[6] + ", " + mat[7] + "\n" +
                mat[8] + ", " + mat[9] + ", " + mat[10] + ", " + mat[11] + "\n" +
                mat[12] + ", " + mat[13] + ", " + mat[14] + ", " + mat[15] + "\n";
    }

}
