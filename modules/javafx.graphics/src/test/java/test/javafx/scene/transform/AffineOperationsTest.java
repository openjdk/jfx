/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

//import java.awt.geom.AffineTransform;
//import com.sun.javafx.geom.transform.Affine3D;
import java.util.List;
import java.util.stream.Stream;
import javafx.geometry.Point3D;
import test.com.sun.javafx.test.TransformHelper;
import javafx.geometry.Point2D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.AffineShim;
import javafx.scene.transform.MatrixType;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.transform.Transform;
import javafx.scene.transform.TransformShim;
import javafx.scene.transform.Translate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AffineOperationsTest {

    private static final Affine identity = new Affine();
    private static final Affine translate = new Affine(1, 0, 2,
                                                       0, 1, 3);
    private static final Affine scale = new Affine(4, 0, 0,
                                                   0, 5, 0);
    private static final Affine sc_tr = new Affine(6, 0, 8,
                                                   0, 7, 9);
    private static final Affine shear = new Affine( 0, 10, 0,
                                                   11,  0, 0);
    private static final Affine sh_tr = new Affine( 0, 12, 14,
                                                   13,  0, 15);
    private static final Affine sh_sc_simple = new Affine( 1, 18, 0,
                                                          19, 1, 0);
    private static final Affine sh_sc = new Affine(16, 18, 0,
                                                   19, 17, 0);
    private static final Affine sh_sc_tr = new Affine(20, 21, 22,
                                                      23, 24, 25);
    private static final Affine a3d_tr = new Affine(1, 0, 0, 0,
                                                    0, 1, 0, 0,
                                                    0, 0, 1, 30);
    private static final Affine a3d_sc = new Affine(1, 0, 0, 0,
                                                    0, 1, 0, 0,
                                                    0, 0, 3, 0);
    private static final Affine a3d_sc_tr = new Affine(1, 0, 0, 0,
                                                       0, 1, 0, 0,
                                                       0, 0, 3, 30);
    private static final Affine a3d_sc2_tr3 = new Affine(1, 0, 0, 0,
                                                         0, 3, 0, 0,
                                                         0, 0, 1, 30);
    private static final Affine a3d_sc3_tr2 = new Affine(1, 0, 0, 25,
                                                         0, 1, 0, 0,
                                                         0, 0, 3, 0);
    private static final Affine a3d_withShear = new Affine(1, 5, 0, 0,
                                                           0, 1, 0, 0,
                                                           0, 0, 3, 30);
    private static final Affine a3d_only3d = new Affine( 1,  0, 20, 0,
                                                         0,  1, 30, 0,
                                                        11, 12, 13, 0);
    private static final Affine a3d_translate_only = new Affine(0, 0, 0, 10,
                                                                0, 0, 0, 20,
                                                                0, 0, 0, 30);
    private static final Affine a3d_complex = new Affine( 2,  3,  4,  5,
                                                          6,  7,  8,  9,
                                                         10, 11, 12, 13);
    private static final Affine a3d_complex_noninvertible =
                                                     new Affine( 2,  3,  4,  5,
                                                                 6,  7,  8,  9,
                                                                10, 11, 12, 13);
    private static final Affine shearRotatesToIdentity1 = new Affine(0, -1, 0,
                                                                     1,  0, 0);
    private static final Affine shearRotatesToIdentity2 = new Affine(0, 1, 0,
                                                                    -1,  0, 0);
    private static final Affine scaleRotatesToIdentity = new Affine(-1,  0, 0,
                                                                     0, -1, 0);
    private static final Affine scr_tr_rotatesToTr = new Affine(-1,  0, 0, 0,
                                                                 0, -1, 0, 0,
                                                                 0,  0, 1, 12);
    private static final Affine translate_only = new Affine(0, 0, 2,
                                                            0, 0, 3);
    private static final Affine nonInv_translate = new Affine(0, 0, 2,
                                                              0, 0, 0);
    private static final Affine nonInv_scale = new Affine(0, 0, 0,
                                                          0, 2, 0);
    private static final Affine nonInv_shear = new Affine(0, 3, 0,
                                                          0, 0, 0);
    private static final Affine nonInv_sh_sc_tr = new Affine(0, 0, 0,
                                                             2, 3, 4);
    private static final Affine nonInv_sh_sc = new Affine(0, 0, 0,
                                                          2, 3, 0);
    private static final Affine nonInv_sh_tr = new Affine(0, 2, 0,
                                                          0, 0, 5);
    private static final Affine nonInv_sc_tr = new Affine(0, 0, 0,
                                                          0, 6, 5);
    private static final Affine zero = new Affine(0, 0, 0,
                                                  0, 0, 0);


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

    public static Stream<Arguments> getParams() {
        return Stream.of(
            Arguments.of( identity ),                   //  0
            Arguments.of( translate ),                  //  1
            Arguments.of( scale ),                      //  2
            Arguments.of( sc_tr ),                      //  3
            Arguments.of( shear ),                      //  4
            Arguments.of( sh_tr ),                      //  5
            Arguments.of( sh_sc ),                      //  6
            Arguments.of( sh_sc_simple ),               //  7
            Arguments.of( sh_sc_tr ),                   //  8
            Arguments.of( a3d_tr ),                     //  9
            Arguments.of( a3d_sc ),                     // 10
            Arguments.of( a3d_sc_tr ),                  // 11
            Arguments.of( a3d_sc2_tr3 ),                // 12
            Arguments.of( a3d_sc3_tr2 ),                // 13
            Arguments.of( a3d_withShear ),              // 14
            Arguments.of( a3d_only3d ),                 // 15
            Arguments.of( a3d_translate_only ),         // 16
            Arguments.of( a3d_complex ),                // 17
            Arguments.of( a3d_complex_noninvertible ),  // 18
            Arguments.of( shearRotatesToIdentity1 ),    // 19
            Arguments.of( shearRotatesToIdentity2 ),    // 20
            Arguments.of( scaleRotatesToIdentity ),     // 21
            Arguments.of( scr_tr_rotatesToTr ),         // 22
            Arguments.of( translate_only ),             // 23
            Arguments.of( nonInv_translate ),           // 24
            Arguments.of( nonInv_scale ),               // 25
            Arguments.of( nonInv_shear ),               // 26
            Arguments.of( nonInv_sh_sc_tr ),            // 27
            Arguments.of( nonInv_sh_sc ),               // 28
            Arguments.of( nonInv_sh_tr ),               // 29
            Arguments.of( nonInv_sc_tr ),               // 30
            Arguments.of( zero )                       // 31
        );
    }

    private int eventCounter;
    private int listener1Counter;
    private int listener2Counter;
    private double memMyx, memTy;

    private void assertAffineOk(Transform expected, Affine a) {
        TransformHelper.assertMatrix(a, expected);
        assertStateOk(a);
    }

    private void assertAffineOk(String message, Transform expected, Affine a) {
        TransformHelper.assertMatrix(message, a, expected);
        assertStateOk(message, a);
    }

    private void assertStateOk(Affine a) {
        TransformHelper.assertStateOk(a, AffineShim.getState3d(a), AffineShim.getState2d(a));
    }

    private void assertStateOk(String message, Affine a) {
        TransformHelper.assertStateOk(message, a, AffineShim.getState3d(a), AffineShim.getState2d(a));
    }

    private void testOperationIsAtomic(Affine a,
            final Runnable op, final Runnable check) {

        a.setOnTransformChanged(event -> {
            eventCounter++;
            check.run();
        });

        a.myxProperty().addListener(observable -> {
            listener1Counter++;
            check.run();
        });

        a.tyProperty().addListener(observable -> {
            listener2Counter++;
            check.run();
        });

        memMyx = a.getMyx();
        memTy = a.getTy();
        eventCounter = 0;
        listener1Counter = 0;
        listener2Counter = 0;
        op.run();

        assertTrue(listener1Counter == (memMyx == a.getMyx() ? 0 : 1));
        assertTrue(listener2Counter == (memTy == a.getTy() ? 0 : 1));
        assertEquals(1, eventCounter);
        assertFalse(AffineShim.atomicChangeRuns(a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void SetToTransformShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Shear sh = new Shear(12, 15);

        testOperationIsAtomic(a,
                () -> a.setToTransform(sh),
                () -> assertAffineOk(sh, a)
        );
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void SetToTransform2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Shear sh = new Shear(12, 15);

        testOperationIsAtomic(a,
                () -> a.setToTransform(1, 12, 0, 15, 1, 0),
                () -> assertAffineOk(sh, a)
        );
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testSetToIdentity(Affine affine) {
        final Affine a = affine.clone();
        a.setToIdentity();
        TransformHelper.assertMatrix(a, new Affine());
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void SetToIdentityShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();

        testOperationIsAtomic(a,
                () -> a.setToIdentity(),
                () -> assertAffineOk(new Affine(), a)
        );
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendTranslation2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Translate(8, 9));
        a.appendTranslation(8, 9);

        assertAffineOk(res, a);

        a = affine.clone();
        a.append(new Translate(8, 9));
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendZeroTranslation2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Translate(0, 0));
        a.appendTranslation(0, 0);

        assertAffineOk(res, a);

        a = affine.clone();
        a.append(new Translate(0, 0));
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendTranslation2DWhichEliminatesTranslation(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        if (!a.isType2D()) {
            // not interested
            return;
        }

        final double ty = (a.getTx() * a.getMyx() / a.getMxx() - a.getTy())
                / (a.getMyy() - a.getMyx() * a.getMxy() / a.getMxx());
        final double tx = (- ty * a.getMxy() - a.getTx()) / a.getMxx();

        if (Double.isNaN(tx) || Double.isNaN(ty)) {
            // impossible to eliminate translation
            return;
        }

        Transform res = TransformHelper.concatenate(a, new Translate(tx, ty));
        a.appendTranslation(tx, ty);

        assertEquals(0, a.getTx(), 1e-10);
        assertEquals(0, a.getTy(), 1e-10);
        assertAffineOk(res, a);

        a = affine.clone();
        a.append(new Translate(tx, ty));
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendTranslation3DWhichMakesTranslation2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        if (a.getMzz() == 0.0) {
            return;
        }

        final double tz = - a.getTz() / a.getMzz();

        Transform res = TransformHelper.concatenate(a, new Translate(0, 0, tz));
        a.appendTranslation(0, 0, tz);

        assertEquals(0, a.getTz(), 1e-10);
        assertAffineOk(res, a);

        a = affine.clone();
        a.append(new Translate(0, 0, tz));
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendTranslation2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Translate(8, 9));

        testOperationIsAtomic(a, () -> a.appendTranslation(8, 9), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependTranslation2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Translate(8, 9), a);
        a.prependTranslation(8, 9);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependZeroTranslation(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Translate(0, 0), a);
        a.prependTranslation(0, 0);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependTranslation2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Translate(8, 9), a);

        testOperationIsAtomic(a, () -> a.prependTranslation(8, 9), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendTranslation3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Translate(8, 9, 10));
        a.appendTranslation(8, 9, 10);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendTranslation3DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Translate(8, 9, 10));

        testOperationIsAtomic(a, () -> a.appendTranslation(8, 9, 10), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependTranslation3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Translate(8, 9, 10), a);
        a.prependTranslation(8, 9, 10);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependTranslation3DWhichMakesIt2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Translate(3, 5, -a.getTz()), a);
        a.prependTranslation(3, 5, -a.getTz());

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependTranslation3DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Translate(8, 9, 10), a);

        testOperationIsAtomic(a, () -> a.prependTranslation(8, 9, 10), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9));
        a.appendScale(8, 9);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendNullPivotedScale2D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.appendScale(8, 9, null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendZeroPivotScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9));
        a.appendScale(8, 9, 0, 0);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(a, new Scale(8, 9, 0, 120));
        a.appendScale(8, 9, 0, 120);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(a, new Scale(8, 9, 150, 0));
        a.appendScale(8, 9, 150, 0);
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendZeroScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(0, 0));
        a.appendScale(0, 0);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(a, new Scale(0, 3));
        a.appendScale(0, 3);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(a, new Scale(3, 0));
        a.appendScale(3, 0);
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendNoScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(1, 1));
        a.appendScale(1, 1);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendScale2DWhichMayEliminateScale(Affine affine) {
        Affine a = affine.clone();

        if (a.getMxx() == 0 || a.getMyy() == 0) {
            // doesn't make sense for this one
            return;
        }

        final double sx = 1.0 / a.getMxx();
        final double sy = 1.0 / a.getMyy();

        Transform res = TransformHelper.concatenate(a, new Scale(sx, sy));
        a.appendScale(sx, sy);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendScale3DWhichMakesIt2D(Affine affine) {
        Affine a = affine.clone();

        if (a.getMzz() == 0.0) {
            // doesn't make sense for this one
            return;
        }

        final double sz = 1.0 / a.getMzz();

        Transform res = TransformHelper.concatenate(a, new Scale(2, 3, sz));
        a.appendScale(2, 3, sz);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendScale2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Scale(8, 9));

        testOperationIsAtomic(a, () -> a.appendScale(8, 9), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9), a);
        a.prependScale(8, 9);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependNullPivotedScale2D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.appendScale(8, 9, null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependZeroPivotScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9), a);
        a.prependScale(8, 9, 0, 0);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(new Scale(8, 9, 0, 120), a);
        a.prependScale(8, 9, 0, 120);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(new Scale(8, 9, 150, 0), a);
        a.prependScale(8, 9, 150, 0);
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependZeroScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(0, 0), a);
        a.prependScale(0, 0);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(new Scale(0, 3), a);
        a.prependScale(0, 3);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(new Scale(3, 0), a);
        a.prependScale(3, 0);
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependHalfZeroScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(4, 0), a);
        a.prependScale(4, 0);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependOtherHalfZeroScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(0, 5), a);
        a.prependScale(0, 5);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependNoScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(1, 1), a);
        a.prependScale(1, 1);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependOppositeScale2D(Affine affine) {
        Affine a = affine.clone();

        final double sx = a.getMxx() == 0 ? 0 : 1 / a.getMxx();
        final double sy = a.getMyy() == 0 ? 0 : 1 / a.getMyy();

        Transform res = TransformHelper.concatenate(new Scale(sx, sy), a);
        a.prependScale(sx, sy);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(new Scale(1.0, sy), a);
        a.prependScale(1.0, sy);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(new Scale(sx, 1.0), a);
        a.prependScale(sx, 1.0);
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependOppositeScale3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        final double sx = a.getMxx() == 0 ? 0 : 1 / a.getMxx();
        final double sy = a.getMyy() == 0 ? 0 : 1 / a.getMyy();
        final double sz = a.getMzz() == 0 ? 0 : 1 / a.getMzz();

        Transform res = TransformHelper.concatenate(new Scale(sx, sy, sz), a);
        a.prependScale(sx, sy, sz);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(new Scale(1.0, 1.0, sz), a);
        a.prependScale(1.0, 1.0, sz);
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependScale2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Scale(8, 9), a);

        testOperationIsAtomic(a, () -> a.prependScale(8, 9), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendPivotedScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11));
        a.appendScale(8, 9, 10, 11);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendPointPivotedScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11));
        a.appendScale(8, 9, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendPivotedScale2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11));

        testOperationIsAtomic(a, () -> a.appendScale(8, 9, 10, 11), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependPivotedScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11), a);
        a.prependScale(8, 9, 10, 11);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependPointPivotedScale2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11), a);
        a.prependScale(8, 9, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependPivotedScale2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11), a);

        testOperationIsAtomic(a, () -> a.prependScale(8, 9, 10, 11), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendScale3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10));
        a.appendScale(8, 9, 10);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendNullPivotedScale3D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.appendScale(8, 9, 10, null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendOppositeScale2D(Affine affine) {
        Affine a = affine.clone();

        final double sx = a.getMxx() == 0 ? 0 : 1 / a.getMxx();
        final double sy = a.getMyy() == 0 ? 0 : 1 / a.getMyy();

        Transform res = TransformHelper.concatenate(a, new Scale(sx, sy));
        a.appendScale(sx, sy);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(a, new Scale(1.0, sy));
        a.appendScale(1.0, sy);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(a, new Scale(sx, 1.0));
        a.appendScale(sx, 1.0);
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendOppositeScale3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        if (a.getMxx() == 0.0 || a.getMyy() == 0.0 || a.getMzz() == 0.0) {
            // doesn't make sense
            return;
        }

        final double sx = 1.0 / a.getMxx();
        final double sy = 1.0 / a.getMyy();
        final double sz = 1.0 / a.getMzz();

        Transform res = TransformHelper.concatenate(a, new Scale(sx, sy, sz));
        a.appendScale(sx, sy, sz);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(new Scale(1.0, 1.0, sz), a);
        a.prependScale(1.0, 1.0, sz);
        assertAffineOk(res, a);

    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendScale3DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10));

        testOperationIsAtomic(a, () -> a.appendScale(8, 9, 10), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependScale3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9, 10), a);
        a.prependScale(8, 9, 10);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependNullPivotedScale3D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.prependScale(8, 9, 10, null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependScale3DWichMakesIt2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        if (a.getMzz() == 1.0 || a.getMzz() == 0.0) {
            // dosen't make sense for this one
            return;
        }

        final double sz = 1.0 / a.getMzz();

        Transform res = TransformHelper.concatenate(new Scale(2, 3, sz), a);
        a.prependScale(2, 3, sz);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependScale3DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Scale(8, 9, 10), a);

        testOperationIsAtomic(a, () -> a.prependScale(8, 9, 10), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendPivotedScale3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11, 12, 13));
        a.appendScale(8, 9, 10, 11, 12, 13);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendPointPivotedScale3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11, 12, 13));
        a.appendScale(8, 9, 10, new Point3D(11, 12, 13));

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendPivotedScale3DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11, 12, 13));

        testOperationIsAtomic(a, () -> a.appendScale(8, 9, 10, 11, 12, 13), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependPivotedScale3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11, 12, 13), a);
        a.prependScale(8, 9, 10, 11, 12, 13);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependPointPivotedScale3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11, 12, 13), a);
        a.prependScale(8, 9, 10, new Point3D(11, 12, 13));

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependPivotedScale3DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11, 12, 13), a);

        testOperationIsAtomic(a, () -> a.prependScale(8, 9, 10, 11, 12, 13), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendShear2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Shear(8, 9));
        a.appendShear(8, 9);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendNullPivotedShear2D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.appendShear(8, 9, null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependNullPivotedShear2D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.prependShear(8, 9, null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendZeroPivotedShear2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Shear(8, 9));
        a.appendShear(8, 9, 0, 0);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(a, new Shear(8, 9, 0, 120));
        a.appendShear(8, 9, 0, 120);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(a, new Shear(8, 9, 150, 0));
        a.appendShear(8, 9, 150, 0);
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependZeroPivotedShear2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Shear(8, 9), a);
        a.prependShear(8, 9, 0, 0);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(new Shear(8, 9, 0, 120), a);
        a.prependShear(8, 9, 0, 120);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(new Shear(8, 9, 150, 0), a);
        a.prependShear(8, 9, 150, 0);
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendZeroShear2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Shear(0, 0));
        a.appendShear(0, 0);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendShear2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Shear(8, 9));

        testOperationIsAtomic(a, () -> a.appendShear(8, 9), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependShear2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Shear(8, 9), a);
        a.prependShear(8, 9);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependShearWhichMayEliminateTranslation(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        final double shx = a.getTy() == 0 ? 0 : - a.getTx() / a.getTy();
        final double shy = a.getTx() == 0 ? 0 : - a.getTy() / a.getTx();

        Transform res = TransformHelper.concatenate(new Shear(shx, shy), a);
        a.prependShear(shx, shy);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependZeroShear2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Shear(0, 0), a);
        a.prependShear(0, 0);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependShear2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Shear(8, 9), a);

        testOperationIsAtomic(a, () -> a.prependShear(8, 9), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendPivotedShear2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Shear(8, 9, 10, 11));
        a.appendShear(8, 9, 10, 11);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendPointPivotedShear2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Shear(8, 9, 10, 11));
        a.appendShear(8, 9, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendPivotedShear2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Shear(8, 9, 10, 11));

        testOperationIsAtomic(a, () -> a.appendShear(8, 9, 10, 11), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependPivotedShear2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Shear(8, 9, 10, 11), a);
        a.prependShear(8, 9, 10, 11);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependPointPivotedShear2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Shear(8, 9, 10, 11), a);
        a.prependShear(8, 9, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependPivotedShear2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Shear(8, 9, 10, 11), a);

        testOperationIsAtomic(a, () -> a.prependShear(8, 9, 10, 11), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendRotation2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(37));
        a.appendRotation(37);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendRotate2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(37));

        testOperationIsAtomic(a, () -> a.appendRotation(37), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependRotation2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(37), a);
        a.prependRotation(37);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependRotate2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Rotate(37), a);

        testOperationIsAtomic(a, () -> a.prependRotation(37), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendRotation90(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(90));
        a.appendRotation(90);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendRotate90ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(90));

        testOperationIsAtomic(a, () -> a.appendRotation(90), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependRotation90(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(90), a);
        a.prependRotation(90);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependRotate90ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(90));

        testOperationIsAtomic(a, () -> a.appendRotation(90), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendRotation180(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(180));
        a.appendRotation(180);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendRotate180ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(180));

        testOperationIsAtomic(a, () -> a.appendRotation(180), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependRotation180(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(180), a);
        a.prependRotation(180);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependRotate180ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(180));

        testOperationIsAtomic(a, () -> a.appendRotation(180), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendRotation270(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(270));
        a.appendRotation(270);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendRotate270ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(270));

        testOperationIsAtomic(a, () -> a.appendRotation(270), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependRotation270(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(270), a);
        a.prependRotation(270);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependRotate270ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(270));

        testOperationIsAtomic(a, () -> a.appendRotation(270), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendRotationMinus450(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(-450));
        a.appendRotation(-450);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependRotationMinus450(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(-450), a);
        a.prependRotation(-450);

        assertAffineOk(res, a);
    }


    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendPivotedRotate2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(37, 10, 11));
        a.appendRotation(37, 10, 11);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendNullPivotedRotate2D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.appendRotation(8, null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependNullPivotedRotate2D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.prependRotation(8, null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendZeroPivotedRotate2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(37));
        a.appendRotation(37, 0, 0);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(a, new Rotate(37, 120, 0));
        a.appendRotation(37, 120, 0);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(a, new Rotate(37, 0, 150));
        a.appendRotation(37, 0, 150);
        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendPointPivotedRotate2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(37, 10, 11));
        a.appendRotation(37, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendPivotedRotate2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(37, 10, 11));

        testOperationIsAtomic(a, () -> a.appendRotation(37, 10, 11), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependPivotedRotate2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(37, 10, 11), a);
        a.prependRotation(37, 10, 11);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependZeroPivotedRotate2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(37), a);
        a.prependRotation(37, 0, 0);

        a = affine.clone();
        res = TransformHelper.concatenate(new Rotate(37, 120, 0), a);
        a.prependRotation(37, 120, 0);
        assertAffineOk(res, a);

        a = affine.clone();
        res = TransformHelper.concatenate(new Rotate(37, 0, 150), a);
        a.prependRotation(37, 0, 150);
        assertAffineOk(res, a);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependPointPivotedRotate2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(37, 10, 11), a);
        a.prependRotation(37, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependPivotedRotate2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Rotate(37, 10, 11), a);

        testOperationIsAtomic(a, () -> a.prependRotation(37, 10, 11), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendNoRotation(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(0));
        a.appendRotation(0);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendRotation3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)));
        a.appendRotation(37, 8, 9, 10, 12, 123, 521);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendNullAxisRotation3D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.appendRotation(8, 100, 110, 120, null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependNullAxisRotation3D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.prependRotation(8, 100, 110, 120, null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendNullAxisPointPivotRotation3D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.appendRotation(8, new Point3D(100, 110, 120), null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependNullAxisPointPivotRotation3D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.prependRotation(8, new Point3D(100, 110, 120), null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendNullPivotRotation3D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.appendRotation(8, null, Rotate.Z_AXIS);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependNullPivotRotation3D(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.prependRotation(8, null, Rotate.Z_AXIS);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendRotation3Dbeing2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 0, new Point3D(0, 0, 10)));
        a.appendRotation(37, 8, 9, 0, 0, 0, 10);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendRotation3DbeingUpsideDown2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 0, new Point3D(0, 0,- 10)));
        a.appendRotation(37, 8, 9, 0, 0, 0, -10);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendRotationWithZeroAxis(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        a.appendRotation(37, 8, 9, 10, 0, 0, 0);

        assertAffineOk(affine, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendRotationWithAlmostZeroAxis(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        a.appendRotation(37, 8, 9, 10, 0, Double.MIN_VALUE, 0);

        assertAffineOk(affine, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendPointedAxisRotation3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)));
        a.appendRotation(37, 8, 9, 10, new Point3D(12, 123, 521));

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendPointedAxisPointedPivotRotation3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)));
        a.appendRotation(37, new Point3D(8, 9, 10), new Point3D(12, 123, 521));

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendRotate3DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)));

        testOperationIsAtomic(a, () -> a.appendRotation(37, 8, 9, 10, 12, 123, 521), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependRotation3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)), a);
        a.prependRotation(37, 8, 9, 10, 12, 123, 521);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependNoRotation(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(0), a);
        a.prependRotation(0);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependRotation3Dbeing2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 0, new Point3D(0, 0, 10)), a);
        a.prependRotation(37, 8, 9, 0, 0, 0, 10);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependRotation3DbeingUpsideDown2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 0, new Point3D(0, 0, -10)), a);
        a.prependRotation(37, 8, 9, 0, 0, 0, -10);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependRotationWithZeroAxis(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        a.prependRotation(37, 8, 9, 10, 0, 0, 0);

        assertAffineOk(affine, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependRotationWithAlmostZeroAxis(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        a.prependRotation(37, 8, 9, 10, 0, Double.MIN_VALUE, 0);

        assertAffineOk(affine, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependPointedAxisRotation3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)), a);
        a.prependRotation(37, 8, 9, 10, new Point3D(12, 123, 521));

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependPointedAxisPointedPivotRotation3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)), a);
        a.prependRotation(37, new Point3D(8, 9, 10), new Point3D(12, 123, 521));

        assertAffineOk(res, a);
    }


    @ParameterizedTest
    @MethodSource("getParams")
    public void prependRotate3DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)), a);

        testOperationIsAtomic(a, () -> a.prependRotation(37, 8, 9, 10, 12, 123, 521), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppend2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Affine other = new Affine(20, 22, 24,
                                  28, 30, 32);

        Transform res = TransformHelper.concatenate(a, other);
        a.append(20, 22, 24,
                  28, 30, 32);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void append2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(20, 22, 24,
                           28, 30, 32));

        testOperationIsAtomic(a, () -> a.append(20, 22, 24,
                 28, 30, 32), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrepend2D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Affine other = new Affine(20, 22, 24,
                                  28, 30, 32);

        Transform res = TransformHelper.concatenate(other, a);
        a.prepend(20, 22, 24,
                 28, 30, 32);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prepend2DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(20, 22, 24,
                           28, 30, 32), a);

        testOperationIsAtomic(a, () -> a.prepend(20, 22, 24,
                 28, 30, 32), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppend3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Affine other = new Affine(20, 22, 24, 26,
                                  28, 30, 32, 34,
                                  36, 38, 40, 42);

        Transform res = TransformHelper.concatenate(a, other);
        a.append(20, 22, 24, 26,
                  28, 30, 32, 34,
                  36, 38, 40, 42);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void append3DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(20, 22, 24, 26,
                           28, 30, 32, 34,
                           36, 38, 40, 42));

        testOperationIsAtomic(a, () -> a.append(20, 22, 24, 26,
                 28, 30, 32, 34,
                 36, 38, 40, 42), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrepend3D(Affine affine) {
        Affine a = affine.clone();
        assertStateOk(a);

        Affine other = new Affine(20, 22, 24, 26,
                                  28, 30, 32, 34,
                                  36, 38, 40, 42);

        Transform res = TransformHelper.concatenate(other, a);
        a.prepend(20, 22, 24, 26,
                  28, 30, 32, 34,
                  36, 38, 40, 42);

        assertAffineOk(res, a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prepend3DShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(20, 22, 24, 26,
                           28, 30, 32, 34,
                           36, 38, 40, 42), a);

        testOperationIsAtomic(a, () -> a.prepend(20, 22, 24, 26,
                 28, 30, 32, 34,
                 36, 38, 40, 42), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendTransform(Affine affine) {
        int counter = 0;
        List<Arguments> arguments = TransformOperationsTest.getParams().toList();
        for (Arguments arg : arguments) {
            Object[] arr = arg.get();
            Transform other = (Transform) arr[0];

            Affine a = affine.clone();
            Transform res = TransformHelper.concatenate(a, other);
            a.append(other);

            assertAffineOk("Appending #" + (counter++) +
                    " from TransformOperationsTest", res, a);
        }
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendNullTransform(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.append((Transform) null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendTransformShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(20, 22, 24, 26,
                           28, 30, 32, 34,
                           36, 38, 40, 42));

        testOperationIsAtomic(a, () -> a.append(new Affine(
                 20, 22, 24, 26,
                 28, 30, 32, 34,
                 36, 38, 40, 42)), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependTransform(Affine affine) {
        int counter = 0;
        List<Arguments> arguments = TransformOperationsTest.getParams().toList();
        for (Arguments arg : arguments) {
            Object[] arr = arg.get();
            Transform other = (Transform) arr[0];

            Affine a = affine.clone();
            assertStateOk(a);

            Transform res = TransformHelper.concatenate(other, a);
            a.prepend(other);

            assertAffineOk("Prepending #" + (counter++) +
                    " from TransformOperationsTest", res, a);
        }
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependNullTransform(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.prepend((Transform) null);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependTransformShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(20, 22, 24, 26,
                           28, 30, 32, 34,
                           36, 38, 40, 42), a);

        testOperationIsAtomic(a, () -> a.prepend(new Affine(
                 20, 22, 24, 26,
                 28, 30, 32, 34,
                 36, 38, 40, 42)), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray(Affine affine) {

        Affine a = affine.clone();
        a.append(array2d, MatrixType.MT_2D_2x3, 2);
        TransformHelper.assertMatrix(a, TransformHelper.concatenate(affine,
                new Affine(
                    2,  3,  0,  4,
                    6,  7,  0,  8,
                    0,  0,  1,  0)));

        a = affine.clone();
        a.append(array2d, MatrixType.MT_2D_3x3, 2);
        TransformHelper.assertMatrix(a, TransformHelper.concatenate(affine,
                new Affine(
                    2,  3,  0,  4,
                    6,  7,  0,  8,
                    0,  0,  1,  0)));

        a = affine.clone();
        a.append(array3d, MatrixType.MT_3D_3x4, 2);
        TransformHelper.assertMatrix(a, TransformHelper.concatenate(affine,
                new Affine(
                    2,  3,  4,  5,
                    6,  7,  8,  9,
                   10, 11, 12, 13)));

        a = affine.clone();
        a.append(array3d, MatrixType.MT_3D_4x4, 2);
        TransformHelper.assertMatrix(a, TransformHelper.concatenate(affine,
                new Affine(
                    2,  3,  4,  5,
                    6,  7,  8,  9,
                   10, 11, 12, 13)));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArrayNullMatrix(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = new Affine();
            a.append(new double[] { 1, 2, 3 }, null, 0);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArrayNullType(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = new Affine();
            a.append(null, MatrixType.MT_2D_2x3, 0);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendArray2x3ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(2,  3,  0,  4,
                           6,  7,  0,  8,
                           0,  0,  1,  0));

        testOperationIsAtomic(a, () -> a.append(array2d, MatrixType.MT_2D_2x3, 2), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendArray3x3ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(2,  3,  0,  4,
                           6,  7,  0,  8,
                           0,  0,  1,  0));

        testOperationIsAtomic(a, () -> a.append(array2d, MatrixType.MT_2D_3x3, 2), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendArray3x4ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(2,  3,  4,  5,
                           6,  7,  8,  9,
                          10, 11, 12, 13));

        testOperationIsAtomic(a, () -> a.append(array3d, MatrixType.MT_3D_3x4, 2), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void appendArray4x4ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(2,  3,  4,  5,
                           6,  7,  8,  9,
                          10, 11, 12, 13));

        testOperationIsAtomic(a, () -> a.append(array3d, MatrixType.MT_3D_4x4, 2), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray2x3ShortArray(Affine affine) {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = affine.clone();
            try {
                a.append(array2d, MatrixType.MT_2D_2x3, 6);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray3x3ShortArray(Affine affine) {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = affine.clone();
            try {
                a.append(array2d, MatrixType.MT_2D_3x3, 4);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray3x4ShortArray(Affine affine) {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = affine.clone();
            try {
                a.append(array3d, MatrixType.MT_3D_3x4, 7);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray4x4ShortArray(Affine affine) {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = affine.clone();
            try {
                a.append(array3d, MatrixType.MT_3D_4x4, 4);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray3x3NotAffineX(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.append(arrayZeros, MatrixType.MT_2D_3x3, 10);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray3x3NotAffineY(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.append(arrayZeros, MatrixType.MT_2D_3x3, 9);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray3x3NotAffineT(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.append(arrayZeros, MatrixType.MT_2D_3x3, 0);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray4x4NotAffineX(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.append(arrayZeros, MatrixType.MT_3D_4x4, 4);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray4x4NotAffineY(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.append(arrayZeros, MatrixType.MT_3D_4x4, 3);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray4x4NotAffineZ(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.append(arrayZeros, MatrixType.MT_3D_4x4, 2);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendArray4x4NotAffineT(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.append(arrayZeros, MatrixType.MT_3D_4x4, 0);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependArray(Affine affine) {

        Affine a = affine.clone();
        a.prepend(array2d, MatrixType.MT_2D_2x3, 2);
        TransformHelper.assertMatrix(a, TransformHelper.concatenate(
                new Affine(
                    2,  3,  0,  4,
                    6,  7,  0,  8,
                    0,  0,  1,  0), affine));

        a = affine.clone();
        a.prepend(array2d, MatrixType.MT_2D_3x3, 2);
        TransformHelper.assertMatrix(a, TransformHelper.concatenate(
                new Affine(
                    2,  3,  0,  4,
                    6,  7,  0,  8,
                    0,  0,  1,  0), affine));

        a = affine.clone();
        a.prepend(array3d, MatrixType.MT_3D_3x4, 2);
        TransformHelper.assertMatrix(a, TransformHelper.concatenate(
                new Affine(
                    2,  3,  4,  5,
                    6,  7,  8,  9,
                   10, 11, 12, 13), affine));

        a = affine.clone();
        a.prepend(array3d, MatrixType.MT_3D_4x4, 2);
        TransformHelper.assertMatrix(a, TransformHelper.concatenate(
                new Affine(
                    2,  3,  4,  5,
                    6,  7,  8,  9,
                   10, 11, 12, 13), affine));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependArrayNullMatrix(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = new Affine();
            a.prepend(new double[] { 1, 2, 3 }, null, 0);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependArrayNullType(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = new Affine();
            a.prepend(null, MatrixType.MT_2D_2x3, 0);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependArray2x3ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(2,  3,  0,  4,
                           6,  7,  0,  8,
                           0,  0,  1,  0), a);

        testOperationIsAtomic(a, () -> a.prepend(array2d, MatrixType.MT_2D_2x3, 2), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependArray3x3ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(2,  3,  0,  4,
                           6,  7,  0,  8,
                           0,  0,  1,  0), a);

        testOperationIsAtomic(a, () -> a.prepend(array2d, MatrixType.MT_2D_3x3, 2), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependArray3x4ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(2,  3,  4,  5,
                           6,  7,  8,  9,
                          10, 11, 12, 13), a);

        testOperationIsAtomic(a, () -> a.prepend(array3d, MatrixType.MT_3D_3x4, 2), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prependArray4x4ShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(2,  3,  4,  5,
                           6,  7,  8,  9,
                          10, 11, 12, 13), a);

        testOperationIsAtomic(a, () -> a.prepend(array3d, MatrixType.MT_3D_4x4, 2), () -> assertAffineOk(res, a));
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependArray2x3ShortArray(Affine affine) {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = affine.clone();
            try {
                a.prepend(array2d, MatrixType.MT_2D_2x3, 6);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependdArray3x3ShortArray(Affine affine) {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = affine.clone();
            try {
                a.prepend(array2d, MatrixType.MT_2D_3x3, 4);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependArray3x4ShortArray(Affine affine) {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = affine.clone();
            try {
                a.prepend(array3d, MatrixType.MT_3D_3x4, 7);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testPrependArray4x4ShortArray(Affine affine) {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Affine a = affine.clone();
            try {
                a.prepend(array3d, MatrixType.MT_3D_4x4, 4);
            } catch(IndexOutOfBoundsException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prestPrependArray3x3NotAffineX(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.prepend(arrayZeros, MatrixType.MT_2D_3x3, 10);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prestPrependArray3x3NotAffineY(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.prepend(arrayZeros, MatrixType.MT_2D_3x3, 9);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prestPrependArray3x3NotAffineT(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.prepend(arrayZeros, MatrixType.MT_2D_3x3, 0);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prestPrependArray4x4NotAffineX(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.prepend(arrayZeros, MatrixType.MT_3D_4x4, 4);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prestPrependArray4x4NotAffineY(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.prepend(arrayZeros, MatrixType.MT_3D_4x4, 3);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prestPrependArray4x4NotAffineZ(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.prepend(arrayZeros, MatrixType.MT_3D_4x4, 2);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void prestPrependArray4x4NotAffineT(Affine affine) {
        assertThrows(IllegalArgumentException.class, () -> {
            Affine a = affine.clone();
            try {
                a.prepend(arrayZeros, MatrixType.MT_3D_4x4, 0);
            } catch(IllegalArgumentException e) {
                TransformHelper.assertMatrix(a, affine);
                throw e;
            }
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testInvert(Affine affine) {
        Affine a = affine.clone();
        Transform expected = null;

        boolean exception = false;

        try {
            expected = TransformHelper.invert(a);
        } catch (NonInvertibleTransformException e) {
            exception = true;
        }

        try {
            a.invert();
        } catch (NonInvertibleTransformException e) {
            if (!exception) {
                fail("Should not have thrown NonInvertibleTransformException");
            }
            return;
        }

        if (exception) {
            fail("Should have thrown NonInvertibleTransformException");
            return;
        }

        TransformHelper.assertMatrix(a, expected);
        assertStateOk(a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void invertShouldBeAtomic(Affine affine) {
        final Affine a = affine.clone();

        try {
            final Transform res = TransformHelper.invert(a);
            testOperationIsAtomic(a, () -> {
                        try {
                            a.invert();
                        } catch (NonInvertibleTransformException e) {
                            fail("Should be invertible");
                        }
                    }, () -> assertAffineOk(res, a));
        } catch (NonInvertibleTransformException e) {
                    try {
                        a.invert();
                    } catch (NonInvertibleTransformException ee) {
                        assertAffineOk(affine, a);
                        return;
                    }
        }

    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testAppendInverse(Affine affine) {
        final Affine a = affine.clone();
        final Affine i = affine.clone();

        try {
            i.invert();
        } catch (NonInvertibleTransformException e) {
            // nothing to test
            return;
        }

        a.append(i);
        assertAffineOk(new Affine(1, 0, 0, 0,
                                  0, 1, 0, 0,
                                  0, 0, 1, 0), a);
    }

    private void assertSetElement(Affine a, MatrixType type, int row, int col,
            double previous, boolean iae, boolean iob) {
        double res = Double.MIN_VALUE;

        try {
            a.setElement(type, row, col, previous + 1000);
        } catch (IllegalArgumentException e) {
            if (iae) {
                // ok
                return;
            }
        } catch (IndexOutOfBoundsException e) {
            if (iob) {
                // ok
                return;
            }
        }

        if (iae) {
            fail("Should have thrown IAE");
        }
        if (iob) {
            fail("Should have thrown IOB");
        }
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testSetElement(Affine affine) {
        Affine a = affine.clone();
        boolean is2d = TransformShim.computeIs2D(affine);

        assertSetElement(a, MatrixType.MT_2D_2x3, 0, 0, a.getMxx(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_2x3, 0, 1, a.getMxy(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_2x3, 0, 2, a.getTx(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_2x3, 1, 0, a.getMyx(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_2x3, 1, 1, a.getMyy(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_2x3, 1, 2, a.getTy(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_2x3, -1, 0, 0, !is2d, true);
        assertSetElement(a, MatrixType.MT_2D_2x3, 2, 0, -1000, !is2d, true);
        assertSetElement(a, MatrixType.MT_2D_2x3, 2, 1, -1000, !is2d, true);
        assertSetElement(a, MatrixType.MT_2D_2x3, 1, 3, 0, !is2d, true);
        assertSetElement(a, MatrixType.MT_2D_2x3, 0, -1, 0, !is2d, true);
        if (is2d) {
            assertAffineOk(new Affine(
                    affine.getMxx() + 1000,
                    affine.getMxy() + 1000,
                    affine.getMxz(),
                    affine.getTx() + 1000,
                    affine.getMyx() + 1000,
                    affine.getMyy() + 1000,
                    affine.getMyz(),
                    affine.getTy() + 1000,
                    affine.getMzx(),
                    affine.getMzy(),
                    affine.getMzz(),
                    affine.getTz()), a);
        }

        a = affine.clone();
        assertSetElement(a, MatrixType.MT_2D_3x3, 0, 0, a.getMxx(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 0, 1, a.getMxy(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 0, 2, a.getTx(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 1, 0, a.getMyx(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 1, 1, a.getMyy(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 1, 2, a.getTy(), !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 2, 0, 0, true, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 2, 0, -1000, !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 2, 0, -999, true, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 2, 0, 0, true, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 2, 1, -1000, !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 2, 1, -999, true, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 2, 1, 0, true, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 2, 2, -1000, true, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, 2, 2, -999, !is2d, false);
        assertSetElement(a, MatrixType.MT_2D_3x3, -1, 0, 0, !is2d, true);
        assertSetElement(a, MatrixType.MT_2D_3x3, 3, 1, 0, !is2d, true);
        assertSetElement(a, MatrixType.MT_2D_3x3, 1, 3, 0, !is2d, true);
        assertSetElement(a, MatrixType.MT_2D_3x3, 1, -1, 0, !is2d, true);
        if (is2d) {
            assertAffineOk(new Affine(
                    affine.getMxx() + 1000,
                    affine.getMxy() + 1000,
                    affine.getMxz(),
                    affine.getTx() + 1000,
                    affine.getMyx() + 1000,
                    affine.getMyy() + 1000,
                    affine.getMyz(),
                    affine.getTy() + 1000,
                    affine.getMzx(),
                    affine.getMzy(),
                    affine.getMzz(),
                    affine.getTz()), a);
        }

        a = affine.clone();
        assertSetElement(a, MatrixType.MT_3D_3x4, 0, 0, a.getMxx(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, 0, 1, a.getMxy(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, 0, 2, a.getMxz(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, 0, 3, a.getTx(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, 1, 0, a.getMyx(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, 1, 1, a.getMyy(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, 1, 2, a.getMyz(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, 1, 3, a.getTy(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, 2, 0, a.getMzx(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, 2, 1, a.getMzy(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, 2, 2, a.getMzz(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, 2, 3, a.getTz(), false, false);
        assertSetElement(a, MatrixType.MT_3D_3x4, -1, 0, 0, false, true);
        assertSetElement(a, MatrixType.MT_3D_3x4, 3, 1, 0, false, true);
        assertSetElement(a, MatrixType.MT_3D_3x4, 1, 4, 0, false, true);
        assertSetElement(a, MatrixType.MT_3D_3x4, 1, -1, 0, false, true);
        assertAffineOk(new Affine(
                affine.getMxx() + 1000,
                affine.getMxy() + 1000,
                affine.getMxz() + 1000,
                affine.getTx() + 1000,
                affine.getMyx() + 1000,
                affine.getMyy() + 1000,
                affine.getMyz() + 1000,
                affine.getTy() + 1000,
                affine.getMzx() + 1000,
                affine.getMzy() + 1000,
                affine.getMzz() + 1000,
                affine.getTz() + 1000), a);

        a = affine.clone();
        assertSetElement(a, MatrixType.MT_3D_4x4, 0, 0, a.getMxx(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 0, 1, a.getMxy(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 0, 2, a.getMxz(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 0, 3, a.getTx(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 1, 0, a.getMyx(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 1, 1, a.getMyy(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 1, 2, a.getMyz(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 1, 3, a.getTy(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 2, 0, a.getMzx(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 2, 1, a.getMzy(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 2, 2, a.getMzz(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 2, 3, a.getTz(), false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 0, 0, true, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 0, -1000, false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 0, -999, true, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 1, 0, true, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 1, -1000, false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 1, -999, true, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 2, 0, true, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 2, -1000, false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 2, -999, true, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 3, 0, true, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 3, -1000, true, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, 3, 3, -999, false, false);
        assertSetElement(a, MatrixType.MT_3D_4x4, -1, 0, 0, false, true);
        assertSetElement(a, MatrixType.MT_3D_4x4, 4, 1, 0, false, true);
        assertSetElement(a, MatrixType.MT_3D_4x4, 1, 4, 0, false, true);
        assertSetElement(a, MatrixType.MT_3D_4x4, 0, -1, 0, false, true);

        assertAffineOk(new Affine(
                affine.getMxx() + 1000,
                affine.getMxy() + 1000,
                affine.getMxz() + 1000,
                affine.getTx() + 1000,
                affine.getMyx() + 1000,
                affine.getMyy() + 1000,
                affine.getMyz() + 1000,
                affine.getTy() + 1000,
                affine.getMzx() + 1000,
                affine.getMzy() + 1000,
                affine.getMzz() + 1000,
                affine.getTz() + 1000), a);
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testSetElementNullType(Affine affine) {
        assertThrows(NullPointerException.class, () -> {
            Affine a = affine.clone();
            a.setElement(null, 0, 0, 0);
        });
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void nonInvertibleExceptionShoudCancelAtomicOperation(Affine affine) {
        Affine a = affine.clone();
        try {
            a.invert();
        } catch (NonInvertibleTransformException e) {
            try {
                a.append(a);
            } catch (Throwable t) {
                fail("Internal state broken: " + t);
            }
        }
    }
}
