/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.test;

import javafx.scene.transform.Transform;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.scene.transform.TransformUtils;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.transform.TransformChangedEvent;
import javafx.scene.transform.Translate;

/**
 * Helper class for transform tests.
 */
public class TransformHelper {

    /**
     * Asserts the {@code matrix} equals to the specified expected values
     */
    public static void assertMatrix(Transform matrix,
            double mxx, double mxy, double mxz, double tx,
            double myx, double myy, double myz, double ty,
            double mzx, double mzy, double mzz, double tz) {
        assertEquals(mxx, matrix.getMxx(), 0.00001);
        assertEquals(mxy, matrix.getMxy(), 0.00001);
        assertEquals(mxz, matrix.getMxz(), 0.00001);
        assertEquals(tx, matrix.getTx(), 0.00001);
        assertEquals(myx, matrix.getMyx(), 0.00001);
        assertEquals(myy, matrix.getMyy(), 0.00001);
        assertEquals(myz, matrix.getMyz(), 0.00001);
        assertEquals(ty, matrix.getTy(), 0.00001);
        assertEquals(mzx, matrix.getMzx(), 0.00001);
        assertEquals(mzy, matrix.getMzy(), 0.00001);
        assertEquals(mzz, matrix.getMzz(), 0.00001);
        assertEquals(tz, matrix.getTz(), 0.00001);
    }

    /**
     * Asserts the {@code matrix} equals to the specified expected values
     */
    public static void assertMatrix(Affine3D matrix,
            double mxx, double mxy, double mxz, double tx,
            double myx, double myy, double myz, double ty,
            double mzx, double mzy, double mzz, double tz) {
        assertEquals(mxx, matrix.getMxx(), 0.00001);
        assertEquals(mxy, matrix.getMxy(), 0.00001);
        assertEquals(mxz, matrix.getMxz(), 0.00001);
        assertEquals(tx, matrix.getMxt(), 0.00001);
        assertEquals(myx, matrix.getMyx(), 0.00001);
        assertEquals(myy, matrix.getMyy(), 0.00001);
        assertEquals(myz, matrix.getMyz(), 0.00001);
        assertEquals(ty, matrix.getMyt(), 0.00001);
        assertEquals(mzx, matrix.getMzx(), 0.00001);
        assertEquals(mzy, matrix.getMzy(), 0.00001);
        assertEquals(mzz, matrix.getMzz(), 0.00001);
        assertEquals(tz, matrix.getMzt(), 0.00001);
    }

    /**
     * Asserts the {@code matrix} elements equal to the expected values
     * specified by {@code reference}
     */
    public static void assertMatrix(Transform matrix,
            Transform reference) {
        assertEquals(reference.getMxx(), matrix.getMxx(), 0.00001);
        assertEquals(reference.getMxy(), matrix.getMxy(), 0.00001);
        assertEquals(reference.getMxz(), matrix.getMxz(), 0.00001);
        assertEquals(reference.getTx(), matrix.getTx(), 0.00001);
        assertEquals(reference.getMyx(), matrix.getMyx(), 0.00001);
        assertEquals(reference.getMyy(), matrix.getMyy(), 0.00001);
        assertEquals(reference.getMyz(), matrix.getMyz(), 0.00001);
        assertEquals(reference.getTy(), matrix.getTy(), 0.00001);
        assertEquals(reference.getMzx(), matrix.getMzx(), 0.00001);
        assertEquals(reference.getMzy(), matrix.getMzy(), 0.00001);
        assertEquals(reference.getMzz(), matrix.getMzz(), 0.00001);
        assertEquals(reference.getTz(), matrix.getTz(), 0.00001);
    }

    /**
     * Asserts the {@code matrix} elements equal to the expected values
     * specified by {@code reference}
     */
    public static void assertMatrix(String message, Transform matrix,
            Transform reference) {
        assertEquals(message, reference.getMxx(), matrix.getMxx(), 0.00001);
        assertEquals(message, reference.getMxy(), matrix.getMxy(), 0.00001);
        assertEquals(message, reference.getMxz(), matrix.getMxz(), 0.00001);
        assertEquals(message, reference.getTx(), matrix.getTx(), 0.00001);
        assertEquals(message, reference.getMyx(), matrix.getMyx(), 0.00001);
        assertEquals(message, reference.getMyy(), matrix.getMyy(), 0.00001);
        assertEquals(message, reference.getMyz(), matrix.getMyz(), 0.00001);
        assertEquals(message, reference.getTy(), matrix.getTy(), 0.00001);
        assertEquals(message, reference.getMzx(), matrix.getMzx(), 0.00001);
        assertEquals(message, reference.getMzy(), matrix.getMzy(), 0.00001);
        assertEquals(message, reference.getMzz(), matrix.getMzz(), 0.00001);
        assertEquals(message, reference.getTz(), matrix.getTz(), 0.00001);
    }

    /**
     * Asserts the {@code matrix} is NOT equal to the specified values
     */
    public static void assertMatrixDiffers(Transform matrix,
            double mxx, double mxy, double mxz, double tx,
            double myx, double myy, double myz, double ty,
            double mzx, double mzy, double mzz, double tz) {
        assertTrue(
                mxx != matrix.getMxx() ||
                mxy != matrix.getMxy() ||
                mxz != matrix.getMxz() ||
                tx != matrix.getTx() ||
                myx != matrix.getMyx() ||
                myy != matrix.getMyy() ||
                myz != matrix.getMyz() ||
                ty != matrix.getTy() ||
                mzx != matrix.getMzx() ||
                mzy != matrix.getMzy() ||
                mzz != matrix.getMzz() ||
                tz != matrix.getTz());
    }

    /**
     * Inverts the given transform.
     * @throws NonInvertibleTransformException
     */
    public static Transform invert(Transform t)
            throws NonInvertibleTransformException {

        final double det = determinant(t);
        if (det == 0 || Math.abs(det) <= Double.MIN_VALUE) {
            throw new NonInvertibleTransformException("Det is 0");
        }

        final double cxx =   minor(t, 0, 0);
        final double cyx = - minor(t, 0, 1);
        final double czx =   minor(t, 0, 2);
        final double cxy = - minor(t, 1, 0);
        final double cyy =   minor(t, 1, 1);
        final double czy = - minor(t, 1, 2);
        final double cxz =   minor(t, 2, 0);
        final double cyz = - minor(t, 2, 1);
        final double czz =   minor(t, 2, 2);
        final double cxt = - minor(t, 3, 0);
        final double cyt =   minor(t, 3, 1);
        final double czt = - minor(t, 3, 2);

        return TransformUtils.immutableTransform(
            cxx / det, cxy / det, cxz / det, cxt / det,
            cyx / det, cyy / det, cyz / det, cyt / det,
            czx / det, czy / det, czz / det, czt / det);
    }

    /**
     * Concatenates the two transforms.
     */
    public static Transform concatenate(Transform t1, Transform t2) {

        final double txx = t2.getMxx();
        final double txy = t2.getMxy();
        final double txz = t2.getMxz();
        final double ttx = t2.getTx();
        final double tyx = t2.getMyx();
        final double tyy = t2.getMyy();
        final double tyz = t2.getMyz();
        final double tty = t2.getTy();
        final double tzx = t2.getMzx();
        final double tzy = t2.getMzy();
        final double tzz = t2.getMzz();
        final double ttz = t2.getTz();
        final double rxx = (t1.getMxx() * txx + t1.getMxy() * tyx + t1.getMxz() * tzx /* + getMxt * 0.0 */);
        final double rxy = (t1.getMxx() * txy + t1.getMxy() * tyy + t1.getMxz() * tzy /* + getMxt * 0.0 */);
        final double rxz = (t1.getMxx() * txz + t1.getMxy() * tyz + t1.getMxz() * tzz /* + getMxt * 0.0 */);
        final double rxt = (t1.getMxx() * ttx + t1.getMxy() * tty + t1.getMxz() * ttz + t1.getTx() /* * 1.0 */);
        final double ryx = (t1.getMyx() * txx + t1.getMyy() * tyx + t1.getMyz() * tzx /* + getMyt * 0.0 */);
        final double ryy = (t1.getMyx() * txy + t1.getMyy() * tyy + t1.getMyz() * tzy /* + getMyt * 0.0 */);
        final double ryz = (t1.getMyx() * txz + t1.getMyy() * tyz + t1.getMyz() * tzz /* + getMyt * 0.0 */);
        final double ryt = (t1.getMyx() * ttx + t1.getMyy() * tty + t1.getMyz() * ttz + t1.getTy() /* * 1.0 */);
        final double rzx = (t1.getMzx() * txx + t1.getMzy() * tyx + t1.getMzz() * tzx /* + getMzt * 0.0 */);
        final double rzy = (t1.getMzx() * txy + t1.getMzy() * tyy + t1.getMzz() * tzy /* + getMzt * 0.0 */);
        final double rzz = (t1.getMzx() * txz + t1.getMzy() * tyz + t1.getMzz() * tzz /* + getMzt * 0.0 */);
        final double rzt = (t1.getMzx() * ttx + t1.getMzy() * tty + t1.getMzz() * ttz + t1.getTz() /* * 1.0 */);

        return TransformUtils.immutableTransform(
                rxx, rxy, rxz, rxt,
                ryx, ryy, ryz, ryt,
                rzx, rzy, rzz, rzt);
    }


    /**
     * Computes determinant of the specified transform.
     */
    public static double determinant(Transform t) {
        return
                t.getMxx() * (t.getMyy() * t.getMzz() - t.getMzy() * t.getMyz()) +
                t.getMxy() * (t.getMyz() * t.getMzx() - t.getMzz() * t.getMyx()) +
                t.getMxz() * (t.getMyx() * t.getMzy() - t.getMzx() * t.getMyy());
    }

    /**
     * Needed for inversion.
     */
    private static double minor(Transform t, int row, int col) {
        double m00 = t.getMxx(), m01 = t.getMxy(), m02 = t.getMxz();
        double m10 = t.getMyx(), m11 = t.getMyy(), m12 = t.getMyz();
        double m20 = t.getMzx(), m21 = t.getMzy(), m22 = t.getMzz();
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
                m02 = t.getTx();
                m12 = t.getTy();
                m22 = t.getTz();
        }
        switch (row) {
            case 0:
                m00 = m10;
                m01 = m11;
                // m02 = m12;
            case 1:
                m10 = m20;
                m11 = m21;
                // m12 = m22;
            case 2:
                // m20 = 0.0;
                // m21 = 0.0;
                // m22 = 1.0;
                break;
            case 3:
                // This is the only row that requires a full 3x3 determinant
                return (m00 * (m11 * m22 - m21 * m12) +
                        m01 * (m12 * m20 - m22 * m10) +
                        m02 * (m10 * m21 - m20 * m11));
        }
        // return (m00 * (m11 * 1.0 - 0.0 * m12) +
        //         m01 * (m12 * 0.0 - 1.0 * m10) +
        //         m02 * (m10 * 0.0 - 0.0 * m11));
        return (m00 * m11 - m01 * m10);
    }

    /**
     * Modifies the given transform, if possible.
     * @param t Transform to modify
     * @param value Value to set to one of transform's properties
     * @return true if the transform was modified
     */
    public static boolean modify(Transform t, double value) {
        if (t instanceof Translate) {
            ((Translate) t).setY(value);
        } else if (t instanceof Scale) {
            ((Scale) t).setY(value);
        } else if (t instanceof Shear) {
            ((Shear) t).setY(value);
        } else if (t instanceof Rotate) {
            Rotate r = (Rotate) t;
            if (r.getAxis().equals(new Point3D(0, 0, 0))) {
                r.setAxis(Rotate.Z_AXIS);
            }
            if ((r.getAxis().getX() != 0 || r.getAxis().getY() != 0) && r.getAngle() == 0) {
                r.setAxis(Rotate.Z_AXIS);
            }
            r.setAngle(value);
        } else if (t instanceof Affine) {
            ((Affine) t).setMyx(value);
        } else {
            return false;
        }

        return true;
    }

    /**
     * Makes a small modification to the transform, if possible.
     * @param t Transform to modify
     * @return true if the transform was modified
     */
    public static boolean tinyModify(Transform t) {
        if (t instanceof Translate) {
            Translate tr = (Translate) t;
            tr.setY(tr.getY() + 1);
            if (tr.getZ() != 0) {
                tr.setZ(tr.getZ() + 3);
            }
        } else if (t instanceof Scale) {
            Scale sc = (Scale) t;
            sc.setY(sc.getY() + 0.01);
            if (sc.getZ() != 1) {
                sc.setZ(sc.getZ() + 0.01);
            }
        } else if (t instanceof Shear) {
            ((Shear) t).setY(((Shear) t).getY() + 0.01);
        } else if (t instanceof Rotate) {
            Rotate r = (Rotate) t;
            if (r.getAxis().getX() == 0 &&
                    r.getAxis().getY() == 0 &&
                    r.getAxis().getZ() == 0) {
                return false;
            }
            if ((r.getAxis().getX() != 0 || r.getAxis().getY() != 0) && r.getAngle() == 0) {
                return false;
            }
            r.setAngle(r.getAngle() + 0.2);
        } else if (t instanceof Affine) {
            Affine a = (Affine) t;
            a.setTy(a.getTy() + 1);
            if (!a.isType2D()) {
                a.setMzz(a.getMzz() + 2);
            }
        } else {
            return false;
        }

        return true;
    }

    /**
     * Makes a small modification to one of the 3D (Z-axis-effective) properties
     * of the transform, if possible.
     * @param t Transform to modify
     * @return true if the transform was modified
     */
    public static boolean tinyModify3D(Transform t) {
        if (t instanceof Translate) {
            ((Translate) t).setZ(((Translate) t).getZ() + 1);
        } else if (t instanceof Scale) {
            ((Scale) t).setZ(((Scale) t).getZ() + 0.01);
        } else if (t instanceof Shear) {
            return false;
        } else if (t instanceof Rotate) {
            Rotate r = (Rotate) t;
            if (r.getAxis().getX() == 0 &&
                    r.getAxis().getY() == 0 &&
                    r.getAxis().getZ() == 0) {
                return false;
            }
            r.setAngle(r.getAngle() + 0.2);
        } else if (t instanceof Affine) {
            ((Affine) t).setTy(((Affine) t).getTy() + 1);
        } else {
            return false;
        }

        return true;
    }

    /**
     * Makes the transform 3D, if possible.
     * @param t Transform to modify
     * @return true if the transform was modified to a 3D state
     */
    public static boolean make3D(Transform t) {
        if (t instanceof Translate) {
            ((Translate) t).setZ(42);
        } else if (t instanceof Scale) {
            ((Scale) t).setZ(42);
        } else if (t instanceof Shear) {
            return false;
        } else if (t instanceof Rotate) {
            Rotate r = (Rotate) t;
            if (r.getAxis().getX() == 0 && r.getAxis().getY() == 0) {
                r.setAxis(Rotate.Y_AXIS);
            }
            if (r.getAngle() == 0) {
                r.setAngle(23);
            }
        } else if (t instanceof Affine) {
            ((Affine) t).setMyz(42);
        } else {
            return false;
        }

        return true;
    }

    /**
     * Makes the transform 2D, if possible.
     * @param t Transform to modify
     * @return true if the transform was modified to a 2D state
     */
    public static boolean make2D(Transform t) {
        if (t instanceof Translate) {
            ((Translate) t).setZ(0);
        } else if (t instanceof Scale) {
            ((Scale) t).setZ(1);
        } else if (t instanceof Shear) {
            return true;
        } else if (t instanceof Rotate) {
            ((Rotate) t).setAxis(Rotate.Z_AXIS);
        } else if (t instanceof Affine) {
            ((Affine) t).setToIdentity();
        } else {
            return false;
        }

        return true;
    }

    /**
     * Makes the transform identity, if possible.
     * @param t Transform to modify
     * @return true if the transform was modified to an identity state
     */
    public static boolean makeIdentity(Transform t) {
        if (t instanceof Translate) {
            ((Translate) t).setX(0);
            ((Translate) t).setY(0);
            ((Translate) t).setZ(0);
        } else if (t instanceof Scale) {
            ((Scale) t).setX(1);
            ((Scale) t).setY(1);
            ((Scale) t).setZ(1);
        } else if (t instanceof Shear) {
            ((Shear) t).setX(0);
            ((Shear) t).setY(0);
        } else if (t instanceof Rotate) {
            ((Rotate) t).setAngle(0);
        } else if (t instanceof Affine) {
            ((Affine) t).setToIdentity();
        } else {
            return false;
        }

        return true;
    }
}
