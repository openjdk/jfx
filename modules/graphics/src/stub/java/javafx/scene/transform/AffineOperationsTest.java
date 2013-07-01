/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

//import java.awt.geom.AffineTransform;
//import com.sun.javafx.geom.transform.Affine3D;
import java.util.Collection;
import javafx.beans.Observable;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized;
import java.util.Arrays;
import javafx.geometry.Point3D;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.sun.javafx.test.TransformHelper;
import javafx.beans.InvalidationListener;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
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

    @Parameters
    public static Collection getParams() {
        return Arrays.asList(new Object[][] {
            { identity },                   //  0
            { translate },                  //  1
            { scale },                      //  2
            { sc_tr },                      //  3
            { shear },                      //  4
            { sh_tr },                      //  5
            { sh_sc },                      //  6
            { sh_sc_simple },               //  7
            { sh_sc_tr },                   //  8
            { a3d_tr },                     //  9
            { a3d_sc },                     // 10
            { a3d_sc_tr },                  // 11
            { a3d_sc2_tr3 },                // 12
            { a3d_sc3_tr2 },                // 13
            { a3d_withShear },              // 14
            { a3d_only3d },                 // 15
            { a3d_translate_only },         // 16
            { a3d_complex },                // 17
            { a3d_complex_noninvertible },  // 18
            { shearRotatesToIdentity1 },    // 19
            { shearRotatesToIdentity2 },    // 20
            { scaleRotatesToIdentity },     // 21
            { scr_tr_rotatesToTr },         // 22
            { translate_only },             // 23
            { nonInv_translate },           // 24
            { nonInv_scale },               // 25
            { nonInv_shear },               // 26
            { nonInv_sh_sc_tr },            // 27
            { nonInv_sh_sc },               // 28
            { nonInv_sh_tr },               // 29
            { nonInv_sc_tr },               // 30
            { zero },                       // 31
        });
    }

    private Affine affine;

    public AffineOperationsTest(Affine a) {
        this.affine = a;
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
        TransformHelper.assertStateOk(a, a.getState3d(), a.getState2d());
    }

    private void assertStateOk(String message, Affine a) {
        TransformHelper.assertStateOk(message, a, a.getState3d(), a.getState2d());
    }

    private void testOperationIsAtomic(Affine a,
            final Runnable op, final Runnable check) {

        a.setOnTransformChanged(new EventHandler<TransformChangedEvent>() {
            @Override public void handle(TransformChangedEvent event) {
                eventCounter++;
                check.run();
            }
        });

        a.myxProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                listener1Counter++;
                check.run();
            }
        });

        a.tyProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                listener2Counter++;
                check.run();
            }
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
        assertFalse(a.atomicChangeRuns());
    }

    @Test
    public void SetToTransformShouldBeAtomic() {
        final Affine a = affine.clone();
        final Shear sh = new Shear(12, 15);

        testOperationIsAtomic(a,
            new Runnable() {
                @Override public void run() {
                    a.setToTransform(sh);
                }},
            new Runnable() {
                @Override public void run() {
                    assertAffineOk(sh, a);
                }
            });
    }

    @Test
    public void SetToTransform2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Shear sh = new Shear(12, 15);

        testOperationIsAtomic(a,
            new Runnable() {
                @Override public void run() {
                    a.setToTransform(1, 12, 0, 15, 1, 0);
                }},
            new Runnable() {
                @Override public void run() {
                    assertAffineOk(sh, a);
                }
            });
    }

    @Test
    public void testSetToIdentity() {
        final Affine a = affine.clone();
        a.setToIdentity();
        TransformHelper.assertMatrix(a, new Affine());
    }

    @Test
    public void SetToIdentityShouldBeAtomic() {
        final Affine a = affine.clone();

        testOperationIsAtomic(a,
            new Runnable() {
                @Override public void run() {
                    a.setToIdentity();
                }},
            new Runnable() {
                @Override public void run() {
                    assertAffineOk(new Affine(), a);
                }
            });
    }

    @Test
    public void testAppendTranslation2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Translate(8, 9));
        a.appendTranslation(8, 9);

        assertAffineOk(res, a);

        a = affine.clone();
        a.append(new Translate(8, 9));
        assertAffineOk(res, a);
    }

    @Test
    public void testAppendZeroTranslation2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Translate(0, 0));
        a.appendTranslation(0, 0);

        assertAffineOk(res, a);

        a = affine.clone();
        a.append(new Translate(0, 0));
        assertAffineOk(res, a);
    }

    @Test
    public void testAppendTranslation2DWhichEliminatesTranslation() {
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

    @Test
    public void testAppendTranslation3DWhichMakesTranslation2D() {
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

    @Test
    public void appendTranslation2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Translate(8, 9));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendTranslation(8, 9);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependTranslation2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Translate(8, 9), a);
        a.prependTranslation(8, 9);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependZeroTranslation() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Translate(0, 0), a);
        a.prependTranslation(0, 0);

        assertAffineOk(res, a);
    }

    @Test
    public void prependTranslation2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Translate(8, 9), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prependTranslation(8, 9);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendTranslation3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Translate(8, 9, 10));
        a.appendTranslation(8, 9, 10);

        assertAffineOk(res, a);
    }

    @Test
    public void appendTranslation3DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Translate(8, 9, 10));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendTranslation(8, 9, 10);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependTranslation3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Translate(8, 9, 10), a);
        a.prependTranslation(8, 9, 10);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependTranslation3DWhichMakesIt2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Translate(3, 5, -a.getTz()), a);
        a.prependTranslation(3, 5, -a.getTz());

        assertAffineOk(res, a);
    }

    @Test
    public void prependTranslation3DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Translate(8, 9, 10), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prependTranslation(8, 9, 10);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendScale2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9));
        a.appendScale(8, 9);

        assertAffineOk(res, a);
    }

    @Test(expected=NullPointerException.class)
    public void testAppendNullPivotedScale2D() {
        Affine a = affine.clone();
        a.appendScale(8, 9, null);
    }

    @Test
    public void testAppendZeroPivotScale2D() {
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

    @Test
    public void testAppendZeroScale2D() {
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

    @Test
    public void testAppendNoScale2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(1, 1));
        a.appendScale(1, 1);

        assertAffineOk(res, a);
    }

    @Test
    public void testAppendScale2DWhichMayEliminateScale() {
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

    @Test
    public void testAppendScale3DWhichMakesIt2D() {
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

    @Test
    public void appendScale2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Scale(8, 9));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendScale(8, 9);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependScale2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9), a);
        a.prependScale(8, 9);

        assertAffineOk(res, a);
    }

    @Test(expected=NullPointerException.class)
    public void testPrependNullPivotedScale2D() {
        Affine a = affine.clone();
        a.appendScale(8, 9, null);
    }

    @Test
    public void testPrependZeroPivotScale2D() {
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

    @Test
    public void testPrependZeroScale2D() {
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

    @Test
    public void testPrependHalfZeroScale2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(4, 0), a);
        a.prependScale(4, 0);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependOtherHalfZeroScale2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(0, 5), a);
        a.prependScale(0, 5);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependNoScale2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(1, 1), a);
        a.prependScale(1, 1);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependOppositeScale2D() {
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

    @Test
    public void testPrependOppositeScale3D() {
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

    @Test
    public void prependScale2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Scale(8, 9), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prependScale(8, 9);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendPivotedScale2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11));
        a.appendScale(8, 9, 10, 11);

        assertAffineOk(res, a);
    }

    @Test
    public void testAppendPointPivotedScale2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11));
        a.appendScale(8, 9, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @Test
    public void appendPivotedScale2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
            a.appendScale(8, 9, 10, 11);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependPivotedScale2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11), a);
        a.prependScale(8, 9, 10, 11);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependPointPivotedScale2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11), a);
        a.prependScale(8, 9, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @Test
    public void prependPivotedScale2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prependScale(8, 9, 10, 11);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendScale3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10));
        a.appendScale(8, 9, 10);

        assertAffineOk(res, a);
    }

    @Test(expected=NullPointerException.class)
    public void testAppendNullPivotedScale3D() {
        Affine a = affine.clone();
        a.appendScale(8, 9, 10, null);
    }

    @Test
    public void testAppendOppositeScale2D() {
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

    @Test
    public void testAppendOppositeScale3D() {
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

    @Test
    public void appendScale3DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendScale(8, 9, 10);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependScale3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9, 10), a);
        a.prependScale(8, 9, 10);

        assertAffineOk(res, a);
    }

    @Test(expected=NullPointerException.class)
    public void testPrependNullPivotedScale3D() {
        Affine a = affine.clone();
        a.prependScale(8, 9, 10, null);
    }

    @Test
    public void testPrependScale3DWichMakesIt2D() {
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

    @Test
    public void prependScale3DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Scale(8, 9, 10), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prependScale(8, 9, 10);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendPivotedScale3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11, 12, 13));
        a.appendScale(8, 9, 10, 11, 12, 13);

        assertAffineOk(res, a);
    }

    @Test
    public void testAppendPointPivotedScale3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11, 12, 13));
        a.appendScale(8, 9, 10, new Point3D(11, 12, 13));

        assertAffineOk(res, a);
    }

    @Test
    public void appendPivotedScale3DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Scale(8, 9, 10, 11, 12, 13));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendScale(8, 9, 10, 11, 12, 13);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependPivotedScale3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11, 12, 13), a);
        a.prependScale(8, 9, 10, 11, 12, 13);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependPointPivotedScale3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11, 12, 13), a);
        a.prependScale(8, 9, 10, new Point3D(11, 12, 13));

        assertAffineOk(res, a);
    }

    @Test
    public void prependPivotedScale3DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Scale(8, 9, 10, 11, 12, 13), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prependScale(8, 9, 10, 11, 12, 13);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendShear2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Shear(8, 9));
        a.appendShear(8, 9);

        assertAffineOk(res, a);
    }

    @Test(expected=NullPointerException.class)
    public void testAppendNullPivotedShear2D() {
        Affine a = affine.clone();
        a.appendShear(8, 9, null);
    }

    @Test(expected=NullPointerException.class)
    public void testPrependNullPivotedShear2D() {
        Affine a = affine.clone();
        a.prependShear(8, 9, null);
    }

    @Test
    public void testAppendZeroPivotedShear2D() {
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

    @Test
    public void testPrependZeroPivotedShear2D() {
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

    @Test
    public void testAppendZeroShear2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Shear(0, 0));
        a.appendShear(0, 0);

        assertAffineOk(res, a);
    }

    @Test
    public void appendShear2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Shear(8, 9));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendShear(8, 9);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependShear2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Shear(8, 9), a);
        a.prependShear(8, 9);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependShearWhichMayEliminateTranslation() {
        Affine a = affine.clone();
        assertStateOk(a);

        final double shx = a.getTy() == 0 ? 0 : - a.getTx() / a.getTy();
        final double shy = a.getTx() == 0 ? 0 : - a.getTy() / a.getTx();

        Transform res = TransformHelper.concatenate(new Shear(shx, shy), a);
        a.prependShear(shx, shy);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependZeroShear2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Shear(0, 0), a);
        a.prependShear(0, 0);

        assertAffineOk(res, a);
    }

    @Test
    public void prependShear2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Shear(8, 9), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prependShear(8, 9);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendPivotedShear2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Shear(8, 9, 10, 11));
        a.appendShear(8, 9, 10, 11);

        assertAffineOk(res, a);
    }

    @Test
    public void testAppendPointPivotedShear2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Shear(8, 9, 10, 11));
        a.appendShear(8, 9, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @Test
    public void appendPivotedShear2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Shear(8, 9, 10, 11));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendShear(8, 9, 10, 11);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependPivotedShear2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Shear(8, 9, 10, 11), a);
        a.prependShear(8, 9, 10, 11);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependPointPivotedShear2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Shear(8, 9, 10, 11), a);
        a.prependShear(8, 9, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @Test
    public void prependPivotedShear2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Shear(8, 9, 10, 11), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prependShear(8, 9, 10, 11);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendRotation2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(37));
        a.appendRotation(37);

        assertAffineOk(res, a);
    }

    @Test
    public void appendRotate2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(37));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendRotation(37);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependRotation2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(37), a);
        a.prependRotation(37);

        assertAffineOk(res, a);
    }

    @Test
    public void prependRotate2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Rotate(37), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prependRotation(37);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendRotation90() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(90));
        a.appendRotation(90);

        assertAffineOk(res, a);
    }

    @Test
    public void appendRotate90ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(90));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendRotation(90);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependRotation90() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(90), a);
        a.prependRotation(90);

        assertAffineOk(res, a);
    }

    @Test
    public void prependRotate90ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(90));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendRotation(90);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendRotation180() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(180));
        a.appendRotation(180);

        assertAffineOk(res, a);
    }

    @Test
    public void appendRotate180ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(180));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendRotation(180);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependRotation180() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(180), a);
        a.prependRotation(180);

        assertAffineOk(res, a);
    }

    @Test
    public void prependRotate180ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(180));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendRotation(180);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendRotation270() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(270));
        a.appendRotation(270);

        assertAffineOk(res, a);
    }

    @Test
    public void appendRotate270ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(270));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendRotation(270);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependRotation270() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(270), a);
        a.prependRotation(270);

        assertAffineOk(res, a);
    }

    @Test
    public void prependRotate270ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(270));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendRotation(270);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendRotationMinus450() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(-450));
        a.appendRotation(-450);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependRotationMinus450() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(-450), a);
        a.prependRotation(-450);

        assertAffineOk(res, a);
    }


    @Test
    public void testAppendPivotedRotate2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(37, 10, 11));
        a.appendRotation(37, 10, 11);

        assertAffineOk(res, a);
    }

    @Test(expected=NullPointerException.class)
    public void testAppendNullPivotedRotate2D() {
        Affine a = affine.clone();
        a.appendRotation(8, null);
    }

    @Test(expected=NullPointerException.class)
    public void testPrependNullPivotedRotate2D() {
        Affine a = affine.clone();
        a.prependRotation(8, null);
    }

    @Test
    public void testAppendZeroPivotedRotate2D() {
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

    @Test
    public void testAppendPointPivotedRotate2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a, new Rotate(37, 10, 11));
        a.appendRotation(37, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @Test
    public void appendPivotedRotate2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a, new Rotate(37, 10, 11));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendRotation(37, 10, 11);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependPivotedRotate2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(37, 10, 11), a);
        a.prependRotation(37, 10, 11);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependZeroPivotedRotate2D() {
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

    @Test
    public void testPrependPointPivotedRotate2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(37, 10, 11), a);
        a.prependRotation(37, new Point2D(10, 11));

        assertAffineOk(res, a);
    }

    @Test
    public void prependPivotedRotate2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(new Rotate(37, 10, 11), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prependRotation(37, 10, 11);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendNoRotation() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(0));
        a.appendRotation(0);

        assertAffineOk(res, a);
    }

    @Test
    public void testAppendRotation3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)));
        a.appendRotation(37, 8, 9, 10, 12, 123, 521);

        assertAffineOk(res, a);
    }

    @Test(expected=NullPointerException.class)
    public void testAppendNullAxisRotation3D() {
        Affine a = affine.clone();
        a.appendRotation(8, 100, 110, 120, null);
    }

    @Test(expected=NullPointerException.class)
    public void testPrependNullAxisRotation3D() {
        Affine a = affine.clone();
        a.prependRotation(8, 100, 110, 120, null);
    }

    @Test(expected=NullPointerException.class)
    public void testAppendNullAxisPointPivotRotation3D() {
        Affine a = affine.clone();
        a.appendRotation(8, new Point3D(100, 110, 120), null);
    }

    @Test(expected=NullPointerException.class)
    public void testPrependNullAxisPointPivotRotation3D() {
        Affine a = affine.clone();
        a.prependRotation(8, new Point3D(100, 110, 120), null);
    }

    @Test(expected=NullPointerException.class)
    public void testAppendNullPivotRotation3D() {
        Affine a = affine.clone();
        a.appendRotation(8, null, Rotate.Z_AXIS);
    }

    @Test(expected=NullPointerException.class)
    public void testPrependNullPivotRotation3D() {
        Affine a = affine.clone();
        a.prependRotation(8, null, Rotate.Z_AXIS);
    }

    @Test
    public void testAppendRotation3Dbeing2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 0, new Point3D(0, 0, 10)));
        a.appendRotation(37, 8, 9, 0, 0, 0, 10);

        assertAffineOk(res, a);
    }

    @Test
    public void testAppendRotation3DbeingUpsideDown2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 0, new Point3D(0, 0,- 10)));
        a.appendRotation(37, 8, 9, 0, 0, 0, -10);

        assertAffineOk(res, a);
    }

    @Test
    public void testAppendRotationWithZeroAxis() {
        Affine a = affine.clone();
        assertStateOk(a);

        a.appendRotation(37, 8, 9, 10, 0, 0, 0);

        assertAffineOk(affine, a);
    }

    @Test
    public void testAppendRotationWithAlmostZeroAxis() {
        Affine a = affine.clone();
        assertStateOk(a);

        a.appendRotation(37, 8, 9, 10, 0, Double.MIN_VALUE, 0);

        assertAffineOk(affine, a);
    }

    @Test
    public void testAppendPointedAxisRotation3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)));
        a.appendRotation(37, 8, 9, 10, new Point3D(12, 123, 521));

        assertAffineOk(res, a);
    }

    @Test
    public void testAppendPointedAxisPointedPivotRotation3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)));
        a.appendRotation(37, new Point3D(8, 9, 10), new Point3D(12, 123, 521));

        assertAffineOk(res, a);
    }

    @Test
    public void appendRotate3DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.appendRotation(37, 8, 9, 10, 12, 123, 521);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependRotation3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)), a);
        a.prependRotation(37, 8, 9, 10, 12, 123, 521);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependNoRotation() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(new Rotate(0), a);
        a.prependRotation(0);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependRotation3Dbeing2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 0, new Point3D(0, 0, 10)), a);
        a.prependRotation(37, 8, 9, 0, 0, 0, 10);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependRotation3DbeingUpsideDown2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 0, new Point3D(0, 0, -10)), a);
        a.prependRotation(37, 8, 9, 0, 0, 0, -10);

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependRotationWithZeroAxis() {
        Affine a = affine.clone();
        assertStateOk(a);

        a.prependRotation(37, 8, 9, 10, 0, 0, 0);

        assertAffineOk(affine, a);
    }

    @Test
    public void testPrependRotationWithAlmostZeroAxis() {
        Affine a = affine.clone();
        assertStateOk(a);

        a.prependRotation(37, 8, 9, 10, 0, Double.MIN_VALUE, 0);

        assertAffineOk(affine, a);
    }

    @Test
    public void testPrependPointedAxisRotation3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)), a);
        a.prependRotation(37, 8, 9, 10, new Point3D(12, 123, 521));

        assertAffineOk(res, a);
    }

    @Test
    public void testPrependPointedAxisPointedPivotRotation3D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)), a);
        a.prependRotation(37, new Point3D(8, 9, 10), new Point3D(12, 123, 521));

        assertAffineOk(res, a);
    }


    @Test
    public void prependRotate3DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Rotate(37, 8, 9, 10, new Point3D(12, 123, 521)), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prependRotation(37, 8, 9, 10, 12, 123, 521);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppend2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Affine other = new Affine(20, 22, 24,
                                  28, 30, 32);

        Transform res = TransformHelper.concatenate(a, other);
        a.append(20, 22, 24,
                  28, 30, 32);

        assertAffineOk(res, a);
    }

    @Test
    public void append2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(20, 22, 24,
                           28, 30, 32));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.append(20, 22, 24,
                             28, 30, 32);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrepend2D() {
        Affine a = affine.clone();
        assertStateOk(a);

        Affine other = new Affine(20, 22, 24,
                                  28, 30, 32);

        Transform res = TransformHelper.concatenate(other, a);
        a.prepend(20, 22, 24,
                 28, 30, 32);

        assertAffineOk(res, a);
    }

    @Test
    public void prepend2DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(20, 22, 24,
                           28, 30, 32), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prepend(20, 22, 24,
                             28, 30, 32);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppend3D() {
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

    @Test
    public void append3DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(20, 22, 24, 26,
                           28, 30, 32, 34,
                           36, 38, 40, 42));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.append(20, 22, 24, 26,
                             28, 30, 32, 34,
                             36, 38, 40, 42);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrepend3D() {
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

    @Test
    public void prepend3DShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(20, 22, 24, 26,
                           28, 30, 32, 34,
                           36, 38, 40, 42), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prepend(20, 22, 24, 26,
                             28, 30, 32, 34,
                             36, 38, 40, 42);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendTransform() {
        int counter = 0;
        for (Object o : TransformOperationsTest.getParams()) {
            Object[] arr = (Object[]) o;
            Transform other = (Transform) arr[0];

            Affine a = affine.clone();
            Transform res = TransformHelper.concatenate(a, other);
            a.append(other);

            assertAffineOk("Appending #" + (counter++) +
                    " from TransformOperationsTest", res, a);
        }
    }

    @Test(expected=NullPointerException.class)
    public void testAppendNullTransform() {
        Affine a = affine.clone();
        a.append((Transform) null);
    }

    @Test
    public void appendTransformShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(20, 22, 24, 26,
                           28, 30, 32, 34,
                           36, 38, 40, 42));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.append(new Affine(
                             20, 22, 24, 26,
                             28, 30, 32, 34,
                             36, 38, 40, 42));
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testPrependTransform() {
        int counter = 0;
        for (Object o : TransformOperationsTest.getParams()) {
            Object[] arr = (Object[]) o;
            Transform other = (Transform) arr[0];

            Affine a = affine.clone();
            assertStateOk(a);

            Transform res = TransformHelper.concatenate(other, a);
            a.prepend(other);

            assertAffineOk("Prepending #" + (counter++) +
                    " from TransformOperationsTest", res, a);
        }
    }

    @Test(expected=NullPointerException.class)
    public void testPrependNullTransform() {
        Affine a = affine.clone();
        a.prepend((Transform) null);
    }

    @Test
    public void prependTransformShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(20, 22, 24, 26,
                           28, 30, 32, 34,
                           36, 38, 40, 42), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prepend(new Affine(
                             20, 22, 24, 26,
                             28, 30, 32, 34,
                             36, 38, 40, 42));
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void testAppendArray() {

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

    @Test(expected=NullPointerException.class)
    public void testAppendArrayNullMatrix() {
        Affine a = new Affine();
        a.append(new double[] { 1, 2, 3 }, null, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testAppendArrayNullType() {
        Affine a = new Affine();
        a.append(null, MatrixType.MT_2D_2x3, 0);
    }

    @Test
    public void appendArray2x3ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(2,  3,  0,  4,
                           6,  7,  0,  8,
                           0,  0,  1,  0));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.append(array2d, MatrixType.MT_2D_2x3, 2);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void appendArray3x3ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(2,  3,  0,  4,
                           6,  7,  0,  8,
                           0,  0,  1,  0));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.append(array2d, MatrixType.MT_2D_3x3, 2);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void appendArray3x4ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(2,  3,  4,  5,
                           6,  7,  8,  9,
                          10, 11, 12, 13));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.append(array3d, MatrixType.MT_3D_3x4, 2);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void appendArray4x4ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(a,
                new Affine(2,  3,  4,  5,
                           6,  7,  8,  9,
                          10, 11, 12, 13));

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.append(array3d, MatrixType.MT_3D_4x4, 2);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testAppendArray2x3ShortArray() {
        Affine a = affine.clone();
        try {
            a.append(array2d, MatrixType.MT_2D_2x3, 6);
        } catch(IndexOutOfBoundsException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testAppendArray3x3ShortArray() {
        Affine a = affine.clone();
        try {
            a.append(array2d, MatrixType.MT_2D_3x3, 4);
        } catch(IndexOutOfBoundsException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testAppendArray3x4ShortArray() {
        Affine a = affine.clone();
        try {
            a.append(array3d, MatrixType.MT_3D_3x4, 7);
        } catch(IndexOutOfBoundsException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testAppendArray4x4ShortArray() {
        Affine a = affine.clone();
        try {
            a.append(array3d, MatrixType.MT_3D_4x4, 4);
        } catch(IndexOutOfBoundsException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAppendArray3x3NotAffineX() {
        Affine a = affine.clone();
        try {
            a.append(arrayZeros, MatrixType.MT_2D_3x3, 10);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAppendArray3x3NotAffineY() {
        Affine a = affine.clone();
        try {
            a.append(arrayZeros, MatrixType.MT_2D_3x3, 9);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAppendArray3x3NotAffineT() {
        Affine a = affine.clone();
        try {
            a.append(arrayZeros, MatrixType.MT_2D_3x3, 0);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAppendArray4x4NotAffineX() {
        Affine a = affine.clone();
        try {
            a.append(arrayZeros, MatrixType.MT_3D_4x4, 4);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAppendArray4x4NotAffineY() {
        Affine a = affine.clone();
        try {
            a.append(arrayZeros, MatrixType.MT_3D_4x4, 3);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAppendArray4x4NotAffineZ() {
        Affine a = affine.clone();
        try {
            a.append(arrayZeros, MatrixType.MT_3D_4x4, 2);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAppendArray4x4NotAffineT() {
        Affine a = affine.clone();
        try {
            a.append(arrayZeros, MatrixType.MT_3D_4x4, 0);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test
    public void testPrependArray() {

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

    @Test(expected=NullPointerException.class)
    public void testPrependArrayNullMatrix() {
        Affine a = new Affine();
        a.prepend(new double[] { 1, 2, 3 }, null, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testPrependArrayNullType() {
        Affine a = new Affine();
        a.prepend(null, MatrixType.MT_2D_2x3, 0);
    }

    @Test
    public void prependArray2x3ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(2,  3,  0,  4,
                           6,  7,  0,  8,
                           0,  0,  1,  0), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prepend(array2d, MatrixType.MT_2D_2x3, 2);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void prependArray3x3ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(2,  3,  0,  4,
                           6,  7,  0,  8,
                           0,  0,  1,  0), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prepend(array2d, MatrixType.MT_2D_3x3, 2);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void prependArray3x4ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(2,  3,  4,  5,
                           6,  7,  8,  9,
                          10, 11, 12, 13), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prepend(array3d, MatrixType.MT_3D_3x4, 2);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test
    public void prependArray4x4ShouldBeAtomic() {
        final Affine a = affine.clone();
        final Transform res = TransformHelper.concatenate(
                new Affine(2,  3,  4,  5,
                           6,  7,  8,  9,
                          10, 11, 12, 13), a);

        testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                    a.prepend(array3d, MatrixType.MT_3D_4x4, 2);
                }}, new Runnable() { @Override public void run() {
                    assertAffineOk(res, a);
                }});
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testPrependArray2x3ShortArray() {
        Affine a = affine.clone();
        try {
            a.prepend(array2d, MatrixType.MT_2D_2x3, 6);
        } catch(IndexOutOfBoundsException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testPrependdArray3x3ShortArray() {
        Affine a = affine.clone();
        try {
            a.prepend(array2d, MatrixType.MT_2D_3x3, 4);
        } catch(IndexOutOfBoundsException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testPrependArray3x4ShortArray() {
        Affine a = affine.clone();
        try {
            a.prepend(array3d, MatrixType.MT_3D_3x4, 7);
        } catch(IndexOutOfBoundsException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testPrependArray4x4ShortArray() {
        Affine a = affine.clone();
        try {
            a.prepend(array3d, MatrixType.MT_3D_4x4, 4);
        } catch(IndexOutOfBoundsException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void prestPrependArray3x3NotAffineX() {
        Affine a = affine.clone();
        try {
            a.prepend(arrayZeros, MatrixType.MT_2D_3x3, 10);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void prestPrependArray3x3NotAffineY() {
        Affine a = affine.clone();
        try {
            a.prepend(arrayZeros, MatrixType.MT_2D_3x3, 9);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void prestPrependArray3x3NotAffineT() {
        Affine a = affine.clone();
        try {
            a.prepend(arrayZeros, MatrixType.MT_2D_3x3, 0);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void prestPrependArray4x4NotAffineX() {
        Affine a = affine.clone();
        try {
            a.prepend(arrayZeros, MatrixType.MT_3D_4x4, 4);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void prestPrependArray4x4NotAffineY() {
        Affine a = affine.clone();
        try {
            a.prepend(arrayZeros, MatrixType.MT_3D_4x4, 3);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void prestPrependArray4x4NotAffineZ() {
        Affine a = affine.clone();
        try {
            a.prepend(arrayZeros, MatrixType.MT_3D_4x4, 2);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void prestPrependArray4x4NotAffineT() {
        Affine a = affine.clone();
        try {
            a.prepend(arrayZeros, MatrixType.MT_3D_4x4, 0);
        } catch(IllegalArgumentException e) {
            TransformHelper.assertMatrix(a, affine);
            throw e;
        }
    }

    @Test
    public void testInvert() {
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

    @Test
    public void invertShouldBeAtomic() {
        final Affine a = affine.clone();

        try {
            final Transform res = TransformHelper.invert(a);
            testOperationIsAtomic(a, new Runnable() { @Override public void run() {
                        try {
                            a.invert();
                        } catch (NonInvertibleTransformException e) {
                            fail("Should be invertible");
                        }
                    }}, new Runnable() { @Override public void run() {
                        assertAffineOk(res, a);
                    }});
        } catch (NonInvertibleTransformException e) {
                    try {
                        a.invert();
                    } catch (NonInvertibleTransformException ee) {
                        assertAffineOk(affine, a);
                        return;
                    }
        }

    }

    @Test
    public void testAppendInverse() {
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

    @Test
    public void testSetElement() {
        Affine a = affine.clone();
        boolean is2d = affine.computeIs2D();

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

    @Test(expected=NullPointerException.class)
    public void testSetElementNullType() {
        Affine a = affine.clone();
        a.setElement(null, 0, 0, 0);
    }

    @Test public void nonInvertibleExceptionShoudCancelAtomicOperation() {
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
