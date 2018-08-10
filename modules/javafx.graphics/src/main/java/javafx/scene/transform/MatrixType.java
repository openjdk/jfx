/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.transform;

/**
 * Specifies type of transformation matrix.
 * @since JavaFX 8.0
 */
public enum MatrixType {
    /**
     * A 2D affine transformation matrix of 2 rows and 3 columns. Contains
     * the following values:
     * <pre>
     * mxx, mxy, tx,
     * myx, myy, ty
     * </pre>
     */
    MT_2D_2x3(2, 3),

    /**
     * A 2D transformation matrix of 3 rows and 3 columns. For affine transforms
     * the last line is constant, so the matrix contains the following values:
     * <pre>
     * mxx, mxy, tx,
     * myx, myy, ty,
     *   0,   0,  1
     * </pre>
     */
    MT_2D_3x3(3, 3),

    /**
     * A 3D affine transformation matrix of 3 rows and 4 columns. Contains
     * the following values:
     * <pre>
     * mxx, mxy, mxz, tx,
     * myx, myy, myz, ty,
     * mzx, mzy, mzz, tz
     * </pre>
     */
    MT_3D_3x4(3, 4),

    /**
     * A 3D transformation matrix of 4 rows and 4 columns. For affine transforms
     * the last line is constant, so the matrix contains the following values:
     * <pre>
     * mxx, mxy, mxz, tx,
     * myx, myy, myz, ty,
     * mzx, mzy, mzz, tz,
     *   0,   0,   0,  1
     * </pre>
     */
    MT_3D_4x4(4, 4);

    private int rows;
    private int cols;

    private MatrixType(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    /**
     * Returns the number of elements in the matrix of this type.
     * @return the number of elements in the matrix of this type
     */
    public int elements() {
        return rows * cols;
    }

    /**
     * Returns the number of rows in the matrix of this type.
     * @return the number of rows in the matrix of this type
     */
    public int rows() {
        return rows;
    }

    /**
     * Returns the number of columns in the matrix of this type.
     * @return the number of columns in the matrix of this type
     */
    public int columns() {
        return cols;
    }

    /**
     * Specifies if this is a 2D transformation matrix
     * @return true if this is a 2D transformation matrix, false if this
     *         is a 3D transformation matrix
     */
    public boolean is2D() {
        return this == MT_2D_2x3 || this == MT_2D_3x3;
    }
}
