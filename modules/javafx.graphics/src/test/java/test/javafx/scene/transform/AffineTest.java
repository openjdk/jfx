/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.transform;

import javafx.beans.Observable;
import static test.javafx.scene.transform.TransformTest.assertTx;
import javafx.scene.shape.Rectangle;

import test.com.sun.javafx.test.TransformHelper;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.scene.transform.TransformUtils;
import javafx.geometry.Point3D;
import javafx.beans.InvalidationListener;
import javafx.scene.transform.Affine;
import javafx.scene.transform.MatrixType;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AffineTest {

    private static final double[] array2d = new double[] {
         0, 1,
         2,  3,  4,
         6,  7,  8,
         0,  0,  1 };

    private static final double[] array3d = new double[] {
         0, 1,
         2,  3,  4,  5,
         6,  7,  8,  9,
        10, 11, 12, 13,
         0,  0,  0,  1 };

    private static final double[] arrayZeros = new double[] {
         0, 0, 0, 0,
         0, 0, 0, 0,
         0, 0, 0, 0,
         0, 0, 0, 0, 1, 0, 0, 0, 0 };

    @Test
    public void testTransformConstructor() {
        Rotate r = new Rotate(23, 11, 12, 13, new Point3D(10, 20, 30));
        Affine a = new Affine(r);

        TransformHelper.assertMatrix(a, r);
    }

    @Test
    public void testTransformConstructorNull() {
        assertThrows(NullPointerException.class, () -> {
            Affine a = new Affine(null);
        });
    }

    @Test
    public void test2DConstructor() {
        Affine a = new Affine(2, 3, 4, 5, 6, 7);

        TransformHelper.assertMatrix(a,
                2, 3, 0, 4,
                5, 6, 0, 7,
                0, 0, 1, 0);
    }

    @Test
    public void test3DConstructor() {
        Affine a = new Affine( 2,  3,  4,  5,
                               6,  7,  8,  9,
                              10, 11, 12, 13);

        TransformHelper.assertMatrix(a,
                 2,  3,  4,  5,
                 6,  7,  8,  9,
                10, 11, 12, 13);
    }

    @Test
    public void testArrayConstructor() {

        Affine a = new Affine(array2d, MatrixType.MT_2D_2x3, 2);
        TransformHelper.assertMatrix(a,
                2,  3,  0,  4,
                6,  7,  0,  8,
                0,  0,  1,  0);

        a = new Affine(array2d, MatrixType.MT_2D_3x3, 2);
        TransformHelper.assertMatrix(a,
                2,  3,  0,  4,
                6,  7,  0,  8,
                0,  0,  1,  0);

        a = new Affine(array3d, MatrixType.MT_3D_3x4, 2);
        TransformHelper.assertMatrix(a,
                2,  3,  4,  5,
                6,  7,  8,  9,
               10, 11, 12, 13);

        a = new Affine(array3d, MatrixType.MT_3D_4x4, 2);
        TransformHelper.assertMatrix(a,
                2,  3,  4,  5,
                6,  7,  8,  9,
               10, 11, 12, 13);
    }

    @Test
    public void testArrayConstructorNullMatrix() {
        assertThrows(NullPointerException.class, () -> {
            Affine a = new Affine(null, MatrixType.MT_2D_2x3, 6);
        });
    }

    @Test
    public void testArrayConstructorNullType() {
        assertThrows(NullPointerException.class, () -> {
            Affine a = new Affine(array2d, null, 6);
        });
    }

    @Test
    public void testArrayConstructor2x3ShortArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = new Affine(array2d, MatrixType.MT_2D_2x3, 6);
        });
    }

    @Test
    public void testArrayConstructor3x3ShortArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = new Affine(array2d, MatrixType.MT_2D_3x3, 4);
        });
    }

    @Test
    public void testArrayConstructor3x4ShortArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = new Affine(array3d, MatrixType.MT_3D_3x4, 7);
        });
    }

    @Test
    public void testArrayConstructor4x4ShortArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = new Affine(array3d, MatrixType.MT_3D_4x4, 4);
        });
    }

    @Test
    public void testArrayConstructor3x3NotAffineX() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(arrayZeros, MatrixType.MT_2D_3x3, 10);
        });
    }

    @Test
    public void testArrayConstructor3x3NotAffineY() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(arrayZeros, MatrixType.MT_2D_3x3, 9);
        });
    }

    @Test
    public void testArrayConstructor3x3NotAffineT() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(arrayZeros, MatrixType.MT_2D_3x3, 0);
        });
    }

    @Test
    public void testArrayConstructor4x4NotAffineX() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(arrayZeros, MatrixType.MT_3D_4x4, 4);
        });
    }

    @Test
    public void testArrayConstructor4x4NotAffineY() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(arrayZeros, MatrixType.MT_3D_4x4, 3);
        });
    }

    @Test
    public void testArrayConstructor4x4NotAffineZ() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(arrayZeros, MatrixType.MT_3D_4x4, 2);
        });
    }

    @Test
    public void testArrayConstructor4x4NotAffineT() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(arrayZeros, MatrixType.MT_3D_4x4, 0);
        });
    }

    @Test
    public void testTransformSetToTransform() {
        Rotate r = new Rotate(23, 11, 12, 13, new Point3D(10, 20, 30));
        Affine a = new Affine(1, 2, 3, 6, 7, 128);

        a.setToTransform(r);
        TransformHelper.assertMatrix(a, r);
    }

    @Test
    public void testTransformSetToTransformNull() {
        assertThrows(NullPointerException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            a.setToTransform(null);
        });
    }

    @Test
    public void test2DSetToTransform() {
        Affine a = new Affine(1, 2, 3, 6, 7, 128);
        a.setToTransform(2, 3, 4, 5, 6, 7);

        TransformHelper.assertMatrix(a,
                2, 3, 0, 4,
                5, 6, 0, 7,
                0, 0, 1, 0);
    }

    @Test
    public void test3DSetToTransform() {
        Affine a = new Affine(1, 2, 3, 6, 7, 128);
        a.setToTransform( 2,  3,  4,  5,
                          6,  7,  8,  9,
                         10, 11, 12, 13);

        TransformHelper.assertMatrix(a,
                 2,  3,  4,  5,
                 6,  7,  8,  9,
                10, 11, 12, 13);
    }

    @Test
    public void testArraySetToTransform() {

        Affine a = new Affine(1, 2, 3, 6, 7, 128);
        a.setToTransform(array2d, MatrixType.MT_2D_2x3, 2);
        TransformHelper.assertMatrix(a,
                2,  3,  0,  4,
                6,  7,  0,  8,
                0,  0,  1,  0);

        a = new Affine(1, 2, 3, 6, 7, 128);
        a.setToTransform(array2d, MatrixType.MT_2D_3x3, 2);
        TransformHelper.assertMatrix(a,
                2,  3,  0,  4,
                6,  7,  0,  8,
                0,  0,  1,  0);

        a = new Affine(1, 2, 3, 6, 7, 128);
        a.setToTransform(array3d, MatrixType.MT_3D_3x4, 2);
        TransformHelper.assertMatrix(a,
                2,  3,  4,  5,
                6,  7,  8,  9,
               10, 11, 12, 13);

        a = new Affine(1, 2, 3, 6, 7, 128);
        a.setToTransform(array3d, MatrixType.MT_3D_4x4, 2);
        TransformHelper.assertMatrix(a,
                2,  3,  4,  5,
                6,  7,  8,  9,
               10, 11, 12, 13);
    }

    @Test
    public void testArraySetToTransformNullMatrix() {
        assertThrows(NullPointerException.class, () -> {
            Affine a = new Affine();
            a.setToTransform(new double[] { 1, 2, 3 }, null, 0);
        });
    }

    @Test
    public void testArraySetToTransformNullType() {
        assertThrows(NullPointerException.class, () -> {
            Affine a = new Affine();
            a.setToTransform(null, MatrixType.MT_2D_2x3, 0);
        });
    }

    @Test
    public void testArraySetToTransform2x3ShortArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            try {
                a.setToTransform(array2d, MatrixType.MT_2D_2x3, 6);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a,
                        1, 2, 0, 3,
                        6, 7, 0, 128,
                        0, 0, 1, 0);
                throw e;
            }
        });
    }

    @Test
    public void testArraySetToTransform3x3ShortArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            try {
                a.setToTransform(array2d, MatrixType.MT_2D_3x3, 4);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a,
                        1, 2, 0, 3,
                        6, 7, 0, 128,
                        0, 0, 1, 0);
                throw e;
            }
        });
    }

    @Test
    public void testArraySetToTransform3x4ShortArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            try {
                a.setToTransform(array3d, MatrixType.MT_3D_3x4, 7);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a,
                        1, 2, 0, 3,
                        6, 7, 0, 128,
                        0, 0, 1, 0);
                throw e;
            }
        });
    }

    @Test
    public void testArraySetToTransform4x4ShortArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            try {
                a.setToTransform(array3d, MatrixType.MT_3D_4x4, 4);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a,
                        1, 2, 0, 3,
                        6, 7, 0, 128,
                        0, 0, 1, 0);
                throw e;
            }
        });
    }

    @Test
    public void testArraySetToTransform3x3NotAffineX() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            try {
                a.setToTransform(arrayZeros, MatrixType.MT_2D_3x3, 10);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a,
                        1, 2, 0, 3,
                        6, 7, 0, 128,
                        0, 0, 1, 0);
                throw e;
            }
        });
    }

    @Test
    public void testArraySetToTransform3x3NotAffineY() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            try {
                a.setToTransform(arrayZeros, MatrixType.MT_2D_3x3, 9);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a,
                        1, 2, 0, 3,
                        6, 7, 0, 128,
                        0, 0, 1, 0);
                throw e;
            }
        });
    }

    @Test
    public void testArraySetToTransform3x3NotAffineT() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            try {
                a.setToTransform(arrayZeros, MatrixType.MT_2D_3x3, 0);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a,
                        1, 2, 0, 3,
                        6, 7, 0, 128,
                        0, 0, 1, 0);
                throw e;
            }
        });
    }

    @Test
    public void testArraySetToTransform4x4NotAffineX() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            try {
                a.setToTransform(arrayZeros, MatrixType.MT_3D_4x4, 4);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a,
                        1, 2, 0, 3,
                        6, 7, 0, 128,
                        0, 0, 1, 0);
                throw e;
            }
        });
    }

    @Test
    public void testArraySetToTransform4x4NotAffineY() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            try {
                a.setToTransform(arrayZeros, MatrixType.MT_3D_4x4, 3);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a,
                        1, 2, 0, 3,
                        6, 7, 0, 128,
                        0, 0, 1, 0);
                throw e;
            }
        });
    }

    @Test
    public void testArraySetToTransform4x4NotAffineZ() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            try {
                a.setToTransform(arrayZeros, MatrixType.MT_3D_4x4, 2);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a,
                        1, 2, 0, 3,
                        6, 7, 0, 128,
                        0, 0, 1, 0);
                throw e;
            }
        });
    }

    @Test
    public void testArraySetToTransform4x4NotAffineT() {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = new Affine(1, 2, 3, 6, 7, 128);
            try {
                a.setToTransform(arrayZeros, MatrixType.MT_3D_4x4, 0);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a,
                        1, 2, 0, 3,
                        6, 7, 0, 128,
                        0, 0, 1, 0);
                throw e;
            }
        });
    }

    private int listenerCounter;
    private class ElementListener implements InvalidationListener {

        private boolean called = false;

        @Override
        public void invalidated(Observable observable) {
            if (called) {
                fail("Element listener notified twice");
            }
            called = true;
            listenerCounter++;
        }
    }

    @Test
    public void everybodyShouldBeNotifiedAfterAtomicChange() {
        final Affine a = new Affine();

        a.mxxProperty().addListener(new ElementListener());
        a.mxyProperty().addListener(new ElementListener());
        a.mxzProperty().addListener(new ElementListener());
        a.txProperty().addListener(new ElementListener());
        a.myxProperty().addListener(new ElementListener());
        a.myyProperty().addListener(new ElementListener());
        a.myzProperty().addListener(new ElementListener());
        a.tyProperty().addListener(new ElementListener());
        a.mzxProperty().addListener(new ElementListener());
        a.mzyProperty().addListener(new ElementListener());
        a.mzzProperty().addListener(new ElementListener());
        a.tzProperty().addListener(new ElementListener());

        listenerCounter = 0;
        a.setToTransform( 2,  3,  4,  5,
                          6,  7,  8,  9,
                         10, 11, 12, 13);

        assertEquals(12, listenerCounter);
    }

    @Test
    public void atomicChangeShouldnotifyOnlyChangedMembers() {
        final Affine a = new Affine();

        a.mxxProperty().addListener(new ElementListener());
        a.mxyProperty().addListener(new ElementListener());
        a.mxzProperty().addListener(new ElementListener());
        a.txProperty().addListener(new ElementListener());
        a.myxProperty().addListener(new ElementListener());
        a.myyProperty().addListener(new ElementListener());
        a.myzProperty().addListener(new ElementListener());
        a.tyProperty().addListener(new ElementListener());
        a.mzxProperty().addListener(new ElementListener());
        a.mzyProperty().addListener(new ElementListener());
        a.mzzProperty().addListener(new ElementListener());
        a.tzProperty().addListener(new ElementListener());

        listenerCounter = 0;
        a.setToTransform( 2,  3,  0,  5,
                          6,  7,  8,  0,
                         10, 11,  1, 13);

        assertEquals(9, listenerCounter);
    }

    @Test
    public void testRepeatedAtomicChangeNotifications() {
        final Affine a = new Affine();

        a.mxxProperty().addListener(observable -> {
            a.getMxx();
            listenerCounter++;
        });

        a.tyProperty().addListener(observable -> {
            a.getTy();
            listenerCounter++;
        });

        listenerCounter = 0;
        a.setToTransform( 2,  3,  4,  5,
                          6,  7,  8,  9,
                         10, 11, 12, 13);
        assertEquals(2, listenerCounter);

        a.setToTransform( 3,  5,  6,  5,
                          6,  5,  6,  9,
                         10, 11,  9, 13);
        assertEquals(3, listenerCounter);

        a.setToTransform( 3,  5,  6,  5,
                          6,  7,  6, 11,
                         10, 11,  9, 13);
        assertEquals(4, listenerCounter);

        a.setToTransform( 3,  5,  7,  5,
                          6,  8,  6, 11,
                         10, 52,  9, 13);
        assertEquals(4, listenerCounter);
    }

    @Test
    public void atomicChangeBackAndForthShouldNotNotify() {
        final Affine a = new Affine();

        a.txProperty().addListener(observable -> {
            a.getTx();
            listenerCounter++;
        });

        listenerCounter = 0;
        a.appendScale(1.0, 2.0, 1.0, 45, 0, 60);

        assertEquals(0, listenerCounter);
    }

    @Test
    public void recursiveAtomicChangeShouldBeFine() {
        final Affine a = new Affine();

        a.tyProperty().addListener(observable -> a.setToTransform( 3,  4,  5,  6,
                          7,  8, 10,  9,
                         11, 12, 13, 14));

        listenerCounter = 0;
        a.setToTransform( 2,  3,  4,  5,
                          6,  7,  8,  9,
                         10, 11, 12, 13);

        TransformHelper.assertMatrix(a,  3,  4,  5,  6,
                                         7,  8, 10,  9,
                                        11, 12, 13, 14);
    }

    @Test
    public void testAffine() {
        final Affine trans = new Affine() {{
            setMxx(11);
            setMyx(22);
            setMxy(33);
            setMyy(44);
            setTx(55);
            setTy(66);
        }};
        assertEquals(1.0f, trans.getMzz(), 1e-50);

        final Rectangle n = new Rectangle();
        n.getTransforms().add(trans);

        final Affine2D affine2D = new Affine2D(11, 22, 33, 44, 55, 66);
        assertEquals(1, affine2D.getMzz(), 1e-50);
        final Affine3D affine3D = new Affine3D(affine2D);
        assertEquals(1, affine3D.getMzz(), 1e-50);
        assertTx(n, affine3D);

        trans.setMxx(10);
        assertTx(n, new Affine2D(10, 22, 33, 44, 55, 66));

        trans.setMyx(21);
        assertTx(n, new Affine2D(10, 21, 33, 44, 55, 66));

        trans.setMxy(32);
        assertTx(n, new Affine2D(10, 21, 32, 44, 55, 66));

        trans.setMyy(43);
        assertTx(n, new Affine2D(10, 21, 32, 43, 55, 66));

        trans.setTx(54);
        assertTx(n, new Affine2D(10, 21, 32, 43, 54, 66));

        trans.setTy(65);
        assertTx(n, new Affine2D(10, 21, 32, 43, 54, 65));
    }

    @Test
    public void testGetters() {
        final Affine trans = new Affine(
                0.5,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);
        TransformHelper.assertMatrix(trans,
                0.5,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        trans.setMxx(12);
        trans.setMxy(11);
        trans.setMxz(10);
        trans.setTx(9);
        trans.setMyx(8);
        trans.setMyy(7);
        trans.setMyz(6);
        trans.setTy(5);
        trans.setMzx(4);
        trans.setMzy(3);
        trans.setMzz(2);
        trans.setTz(1);

        TransformHelper.assertMatrix(trans,
                12, 11, 10, 9,
                 8,  7,  6, 5,
                 4,  3,  2, 1);
    }

    @Test
    public void testConstructingIdentityTransform() {
        final Affine trans = new Affine(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0);

        TransformHelper.assertMatrix(trans,
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0);

        trans.setMxx(12);
        trans.setMxy(11);
        trans.setMxz(10);
        trans.setTx(9);
        trans.setMyx(8);
        trans.setMyy(7);
        trans.setMyz(6);
        trans.setTy(5);
        trans.setMzx(4);
        trans.setMzy(3);
        trans.setMzz(2);
        trans.setTz(1);

        TransformHelper.assertMatrix(trans,
                12, 11, 10, 9,
                 8,  7,  6, 5,
                 4,  3,  2, 1);
    }

    @Test
    public void testSettingTransform() {
        final Affine trans = new Affine(
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        TransformHelper.assertMatrix(trans,
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        Transform it = TransformUtils.immutableTransform(
                12, 11, 10, 9,
                 8,  7,  6, 5,
                 4,  3,  2, 1);

        trans.setToTransform(it);

        TransformHelper.assertMatrix(trans,
                12, 11, 10, 9,
                 8,  7,  6, 5,
                 4,  3,  2, 1);
    }

    @Test
    public void testCopying() {
        final Affine trans = new Affine(
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        Transform copy = trans.clone();

        TransformHelper.assertMatrix(copy,
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        trans.setMxz(1234);
        assertEquals(3.0, copy.getMxz(), 0.0001);
    }

    @Test
    public void testToString() {
        final Affine trans = new Affine(
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        String s = trans.toString();

        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test
    public void testBoundPropertySynced() throws Exception {

        TransformTest.checkDoublePropertySynced(createTransform(), "mxx" , 2.0);
        TransformTest.checkDoublePropertySynced(createTransform(), "myx" , 2.0);
        TransformTest.checkDoublePropertySynced(createTransform(), "mxy" , 2.0);
        TransformTest.checkDoublePropertySynced(createTransform(), "mxz" , 2.0);
        TransformTest.checkDoublePropertySynced(createTransform(), "myy" , 2.0);
        TransformTest.checkDoublePropertySynced(createTransform(), "myz" , 2.0);
        TransformTest.checkDoublePropertySynced(createTransform(), "mzx" , 2.0);
        TransformTest.checkDoublePropertySynced(createTransform(), "mzy" , 2.0);
        TransformTest.checkDoublePropertySynced(createTransform(), "mzz" , 2.0);
        TransformTest.checkDoublePropertySynced(createTransform(), "tx" , 2.0);
        TransformTest.checkDoublePropertySynced(createTransform(), "ty" , 2.0);
        TransformTest.checkDoublePropertySynced(createTransform(), "tz" , 2.0);
    }

    private Affine createTransform() {
        final Affine trans = new Affine() {{
            setMxx(11);
            setMyx(22);
            setMxy(33);
            setMyy(44);
            setTx(55);
            setTy(66);
        }};
        return trans;
    }
}
