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

import com.sun.javafx.scene.transform.TransformUtils;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import java.util.Collection;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized;
import java.util.Arrays;
import javafx.geometry.Point3D;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.sun.javafx.test.TransformHelper;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TransformOperationsTest {
    private static final Affine affine_identity = new Affine();
    private static final Affine affine_translate_only = new Affine(0, 0, 2,
                                                                   0, 0, 3);
    private static final Affine affine_translate = new Affine(1, 0, 2,
                                                              0, 1, 3);
    private static final Affine affine_scale = new Affine(4, 0, 0,
                                                          0, 5, 0);
    private static final Affine affine_sc_tr = new Affine(6, 0, 8,
                                                          0, 7, 9);
    private static final Affine affine_shear = new Affine( 0, 10, 0,
                                                          11,  0, 0);
    private static final Affine affine_sh_tr = new Affine( 0, 12, 14,
                                                          13,  0, 15);
    private static final Affine affine_sh_sc_simple = new Affine( 1, 18, 0,
                                                                 19,  1, 0);
    private static final Affine affine_sh_sc = new Affine(16, 18, 0,
                                                          19, 17, 0);
    private static final Affine affine_sh_sc_tr = new Affine(20, 21, 22,
                                                             23, 24, 25);
    private static final Affine affine_3d_tr = new Affine(1, 0, 0, 0,
                                                    0, 1, 0, 0,
                                                    0, 0, 1, 30);
    private static final Affine affine_3d_sc = new Affine(1, 0, 0, 0,
                                                    0, 1, 0, 0,
                                                    0, 0, 3, 0);
    private static final Affine affine_3d_sc_tr = new Affine(1, 0, 0, 0,
                                                       0, 1, 0, 0,
                                                       0, 0, 3, 30);
    private static final Affine affine_3d_sc2_tr3 = new Affine(1, 0, 0, 0,
                                                         0, 3, 0, 0,
                                                         0, 0, 1, 30);
    private static final Affine affine_3d_sc3_tr2 = new Affine(1, 0, 0, 25,
                                                         0, 1, 0, 0,
                                                         0, 0, 3, 0);
    private static final Affine affine_3d_withShear = new Affine(1, 5, 0, 0,
                                                           0, 1, 0, 0,
                                                           0, 0, 3, 30);
    private static final Affine affine_3d_only3d = new Affine( 1,  0, 20, 0,
                                                         0,  1, 30, 0,
                                                        11, 12, 13, 0);
    private static final Affine affine_3d_translate_only = new Affine(0, 0, 0, 10,
                                                                       0, 0, 0, 20,
                                                                       0, 0, 0, 30);
    private static final Affine affine_3d_complex = new Affine( 7,  3,  4,  5,
                                                          6,  7,  5,  9,
                                                         10, 11, 12, 13);
    private static final Affine affine_3d_complex_noninvertible =
                                                     new Affine( 2,  3,  4,  5,
                                                                 6,  7,  8,  9,
                                                                10, 11, 12, 13);
    private static final Affine affine_empty = new Affine(0, 0, 0, 0,
                                                          0, 0, 0, 0,
                                                          0, 0, 0, 0);
    private static final Affine affine_emptyZ = new Affine(1, 0, 0, 0,
                                                           0, 1, 0, 0,
                                                           0, 0, 0, 0);
    private static final Affine affine_emptyXY = new Affine(0, 0, 0, 0,
                                                            0, 0, 0, 0,
                                                            0, 0, 1, 0);
    private static final Affine affine_nonInv_translate_x = new Affine(0, 0, 2,
                                                                       0, 0, 0);
    private static final Affine affine_nonInv_translate_y = new Affine(0, 0, 0,
                                                                       0, 0, 4);
    private static final Affine affine_nonInv_translate_z = new Affine(0, 0, 0, 0,
                                                                       0, 0, 0, 0,
                                                                       0, 0, 0, 4);
    private static final Affine affine_nonInv_scale_x = new Affine(2, 0, 0,
                                                          0, 0, 0);
    private static final Affine affine_nonInv_scale_y = new Affine(0, 0, 0,
                                                          0, 2, 0);
    private static final Affine affine_nonInv_scale_xy = new Affine(2, 0, 0, 0,
                                                                    0, 2, 0, 0,
                                                                    0, 0, 0, 0);
    private static final Affine affine_nonInv_scale_z = new Affine(0, 0, 0, 0,
                                                                    0, 0, 0, 0,
                                                                    0, 0, 4, 0);
    private static final Affine affine_nonInv_shear_x = new Affine(0, 3, 0,
                                                          0, 0, 0);
    private static final Affine affine_nonInv_shear_y = new Affine(0, 0, 0,
                                                          3, 0, 0);
    private static final Affine affine_nonInv_sh_tr_x = new Affine(0, 3, 4,
                                                          0, 0, 0);
    private static final Affine affine_nonInv_sh_tr_y = new Affine(0, 0, 0,
                                                          3, 0, 4);
    private static final Affine affine_nonInv_sh_sc_tr = new Affine(0, 0, 0,
                                                             2, 3, 4);
    private static final Affine affine_nonInv_sh_sc = new Affine(0, 0, 0,
                                                          2, 3, 0);
    private static final Affine affine_nonInv_sh_tr = new Affine(0, 0, 0,
                                                          2, 0, 5);
    private static final Affine affine_nonInv_sc_tr = new Affine(0, 0, 0,
                                                          0, 6, 5);
    private static final Affine affine_nonInv_sc_tr_x = new Affine(2, 0, 4,
                                                          0, 0, 0);
    private static final Affine affine_nonInv_sc_tr_y = new Affine(0, 0, 0,
                                                          0, 2, 7);
    private static final Translate translate2d = new Translate(120, 225);
    private static final Translate translate3d = new Translate(120, 225, 346);
    private static final Translate translate3d_only = new Translate(0, 0, 346);
    private static final Translate noTranslate = new Translate(0, 0);
    private static final Scale scale2d = new Scale(0.5, 2.5, 35, 46);
    private static final Scale scale2d_x = new Scale(1.0, 2.5, 35, 46);
    private static final Scale scale2d_y = new Scale(0.5, 1.0, 35, 46);
    private static final Scale scale3d = new Scale(0.5, 2.5, 3.6, 35, 46, 55);
    private static final Scale scale3dOnly = new Scale(1.0, 1.0, 3.6);
    private static final Scale scale2dNoPivot = new Scale(0.5, 2.5);
    private static final Scale scale2dUslessPivots = new Scale(0.5, 1.0, 0.0, 45);
    private static final Scale scale2dPivot3d = new Scale(0.5, 2.5, 1.0, 35, 46, 52);
    private static final Scale scale3dNoPivot = new Scale(0.5, 2.5, 3.6);
    private static final Scale noScale = new Scale(1, 1);
    private static final Scale nonInvertible3dScale = new Scale(0.0, 2.5, 3.6, 35, 46, 55);
    private static final Scale nonInvertible2dScale = new Scale(1.3, 0.0, 35, 45);
    private static final Shear shear = new Shear(3.2, 4.3, 75, 84);
    private static final Shear shearX = new Shear(3.2, 0, 75, 84);
    private static final Shear shearY = new Shear(0, 4.3, 75, 84);
    private static final Shear shearNoPivot = new Shear(3.5, 4.3);
    private static final Shear noShear = new Shear(0, 0, 75, 84);
    private static final Rotate simpleRotate3d = new Rotate(97.5, Rotate.Y_AXIS);
    private static final Rotate rotate2d = new Rotate(97.5, 123, 456);
    private static final Rotate rotate3d = new Rotate(97.5, 33, 44, 55, new Point3D(66, 77, 88));
    private static final Rotate rotate3d2d = new Rotate(97.5, 33, 44, 55, new Point3D(0, 0, 10));
    private static final Rotate rotateZeroAxis = new Rotate(97.5, 33, 44, 55, new Point3D(0, 0, 0));
    private static final Rotate rotate3dUpsideDown2d = new Rotate(97.5, 33, 44, 55, new Point3D(0, 0, -10));
    private static final Rotate rotate2dNoPivot = new Rotate(97.5);
    private static final Rotate rotate3dNoPivot = new Rotate(97.5, new Point3D(66, 77, 88));
    private static final Rotate rotate2dPivot3d = new Rotate(97.5, 125, 126, 127, Rotate.Z_AXIS);
    private static final Rotate noRotate = new Rotate(0, Rotate.Y_AXIS);
    private static final Transform immutable_identity =
            TransformHelper.immutableTransform(1, 0, 0, 0, 1, 0);
    private static final Transform immutable_translate_only =
            TransformHelper.immutableTransform(0, 0, 2, 0, 0, 3);
    private static final Transform immutable_translate =
            TransformHelper.immutableTransform(1, 0, 2, 0, 1, 3);
    private static final Transform immutable_scale =
            TransformHelper.immutableTransform(4, 0, 0, 0, 5, 0);
    private static final Transform immutable_sc_tr =
            TransformHelper.immutableTransform(6, 0, 8, 0, 7, 9);
    private static final Transform immutable_shear =
            TransformHelper.immutableTransform( 0, 10, 0, 11,  0, 0);
    private static final Transform immutable_sh_tr =
            TransformHelper.immutableTransform( 0, 12, 14, 13,  0, 15);
    private static final Transform immutable_sh_sc_simple =
            TransformHelper.immutableTransform( 1, 18, 0, 19,  1, 0);
    private static final Transform immutable_sh_sc =
            TransformHelper.immutableTransform(16, 18, 0, 19, 17, 0);
    private static final Transform immutable_sh_sc_tr =
            TransformHelper.immutableTransform(20, 21, 22, 23, 24, 25);
    private static final Transform immutable_3d_tr =
            TransformUtils.immutableTransform(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 30);
    private static final Transform immutable_3d_sc =
            TransformUtils.immutableTransform(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 3, 0);
    private static final Transform immutable_3d_sc_tr =
            TransformUtils.immutableTransform(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 3, 30);
    private static final Transform immutable_3d_sc2_tr3 =
            TransformUtils.immutableTransform(1, 0, 0, 0, 0, 3, 0, 0, 0, 0, 1, 30);
    private static final Transform immutable_3d_sc3_tr2 =
            TransformUtils.immutableTransform(1, 0, 0, 25, 0, 1, 0, 0, 0, 0, 3, 0);
    private static final Transform immutable_3d_withShear =
            TransformUtils.immutableTransform(1, 5, 0, 0, 0, 1, 0, 0, 0, 0, 3, 30);
    private static final Transform immutable_3d_only3d =
            TransformUtils.immutableTransform(1, 0, 20, 0, 0, 1, 30, 0, 11, 12, 13, 0);
    private static final Transform immutable_3d_translate_only =
            TransformUtils.immutableTransform(0, 0, 0, 10, 0, 0, 0, 20, 0, 0, 0, 30);
    private static final Transform immutable_3d_complex =
            TransformUtils.immutableTransform(7, 3, 4, 5, 5, 7, 8, 9, 10, 11, 12, 13);
    private static final Transform immutable_3d_complex_noninvertible =
            TransformUtils.immutableTransform(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
    private static final Transform immutable_empty =
            TransformUtils.immutableTransform(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    private static final Transform immutable_emptyZ =
            TransformUtils.immutableTransform(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0);
    private static final Transform immutable_emptyXY =
            TransformUtils.immutableTransform(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0);
    private static final Transform immutable_nonInv_translate_x =
            TransformHelper.immutableTransform(0, 0, 2, 0, 0, 0);
    private static final Transform immutable_nonInv_translate_y =
            TransformHelper.immutableTransform(0, 0, 0, 0, 0, 4);
    private static final Transform immutable_nonInv_translate_z =
            TransformUtils.immutableTransform(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4);
    private static final Transform immutable_nonInv_scale_x =
            TransformHelper.immutableTransform(2, 0, 0, 0, 0, 0);
    private static final Transform immutable_nonInv_scale_y =
            TransformHelper.immutableTransform(0, 0, 0, 0, 2, 0);
    private static final Transform immutable_nonInv_scale_xy =
            TransformUtils.immutableTransform(2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0);
    private static final Transform immutable_nonInv_scale_z =
            TransformUtils.immutableTransform(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0);
    private static final Transform immutable_nonInv_shear_x =
            TransformHelper.immutableTransform(0, 3, 0, 0, 0, 0);
    private static final Transform immutable_nonInv_shear_y =
            TransformHelper.immutableTransform(0, 0, 0, 3, 0, 0);
    private static final Transform immutable_nonInv_sh_tr_x =
            TransformHelper.immutableTransform(0, 3, 4, 0, 0, 0);
    private static final Transform immutable_nonInv_sh_tr_y =
            TransformHelper.immutableTransform(0, 0, 0, 3, 0, 4);
    private static final Transform immutable_nonInv_sh_sc_tr =
            TransformHelper.immutableTransform(0, 0, 0, 2, 3, 4);
    private static final Transform immutable_nonInv_sh_sc =
            TransformHelper.immutableTransform(0, 0, 0, 2, 3, 0);
    private static final Transform immutable_nonInv_sh_tr =
            TransformHelper.immutableTransform(0, 0, 0, 2, 0, 5);
    private static final Transform immutable_nonInv_sc_tr =
            TransformHelper.immutableTransform(0, 0, 0, 0, 6, 5);
    private static final Transform immutable_nonInv_sc_tr_x =
            TransformHelper.immutableTransform(2, 0, 4, 0, 0, 0);
    private static final Transform immutable_nonInv_sc_tr_y =
            TransformHelper.immutableTransform(0, 0, 0, 0, 2, 7);
    private static final Transform raw_arbitrary_nonInvertible =
            TransformHelper.rawTransform( 5,  6,  7,  8,
                                         10, 11, 12, 13,
                                         15, 16, 17, 18);
    private static final Transform raw_arbitrary =
            TransformHelper.rawTransform( 5,  6, 13,  8,
                                         10,  4, 12, 13,
                                         15, 16, 26, 18);
    private static final Transform raw_empty =
            TransformHelper.rawTransform(0, 0, 0, 0,
                                         0, 0, 0, 0,
                                         0, 0, 0, 0);
    private static final Transform raw_emptyZ =
            TransformHelper.rawTransform(1, 0, 0, 0,
                                         0, 1, 0, 0,
                                         0, 0, 0, 0);
    private static final Transform raw_emptyXY =
            TransformHelper.rawTransform(0, 0, 0, 0,
                                         0, 0, 0, 0,
                                         0, 0, 1, 0);

    private boolean listenerCalled;
    private int eventCounter;

    //BEWARE: used also in AffineOperationsTest
    @Parameters
    public static Collection getParams() {
        return Arrays.asList(new Object[][] {
            { affine_identity, true, Affine.class },            //  0
            { affine_translate, true, Affine.class },           //  1
            { affine_translate_only, true, Affine.class },      //  2
            { affine_scale, true, Affine.class },               //  3
            { affine_sc_tr, true, Affine.class },               //  4
            { affine_shear, true, Affine.class },               //  5
            { affine_sh_tr, true, Affine.class },               //  6
            { affine_sh_sc_simple, true, Affine.class },        //  7
            { affine_sh_sc, true, Affine.class },               //  8
            { affine_sh_sc_tr, true, Affine.class },            //  9
            { affine_3d_tr, false, Affine.class },              // 10
            { affine_3d_sc, false, Affine.class },              // 11
            { affine_3d_sc_tr, false, Affine.class },           // 12
            { affine_3d_sc2_tr3, false, Affine.class },         // 13
            { affine_3d_sc3_tr2, false, Affine.class },         // 14
            { affine_3d_withShear, false, Affine.class },       // 15
            { affine_3d_only3d, false, Affine.class },          // 16
            { affine_3d_translate_only, false, null },          // 17
            { affine_3d_complex, false, Affine.class },         // 18
            { affine_3d_complex_noninvertible, false, null },   // 19
            { affine_empty, false, null },                      // 20
            { affine_emptyZ, false, null },                     // 21
            { affine_emptyXY, true, null },                     // 22
            { affine_nonInv_translate_x, true, null },          // 23
            { affine_nonInv_translate_y, true, null },          // 24
            { affine_nonInv_translate_z, false, null },         // 25
            { affine_nonInv_scale_x, true, null },              // 26
            { affine_nonInv_scale_y, true, null },              // 27
            { affine_nonInv_scale_xy, false, null },            // 28
            { affine_nonInv_scale_z, false, null },             // 29
            { affine_nonInv_shear_x, true, null },              // 30
            { affine_nonInv_shear_y, true, null },              // 31
            { affine_nonInv_sh_tr_x, true, null },              // 32
            { affine_nonInv_sh_tr_y, true, null },              // 33
            { affine_nonInv_sh_sc_tr, true, null },             // 34
            { affine_nonInv_sh_sc, true, null },                // 35
            { affine_nonInv_sh_tr, true, null },                // 36
            { affine_nonInv_sc_tr, true, null },                // 37
            { affine_nonInv_sc_tr_x, true, null },              // 38
            { affine_nonInv_sc_tr_y, true, null },              // 39
            { translate2d, true, Translate.class },             // 40
            { translate3d, false, Translate.class },            // 41
            { translate3d_only, false, Translate.class },       // 42
            { noTranslate, true, Translate.class },             // 43
            { scale2d, true, Scale.class },                     // 44
            { scale2d_x, true, Scale.class },                   // 45
            { scale2d_y, true, Scale.class },                   // 46
            { scale3d, false, Scale.class },                    // 47
            { scale3dOnly, false, Scale.class },                // 48
            { scale2dNoPivot, true, Scale.class },              // 49
            { scale2dUslessPivots, true, Scale.class },         // 50
            { scale3dNoPivot, false, Scale.class },             // 51
            { scale2dPivot3d, true, Scale.class },              // 52
            { noScale, true, Scale.class },                     // 53
            { nonInvertible2dScale, true, null },               // 54
            { nonInvertible3dScale, false, null },              // 55
            { shear, true, Affine.class },                      // 56
            { shearX, true, Shear.class },                      // 57
            { shearY, true, Shear.class },                      // 58
            { shearNoPivot, true, Affine.class },               // 59
            { noShear, true, Shear.class },                     // 60
            { simpleRotate3d, false, Rotate.class},             // 61
            { rotate2d, true, Rotate.class },                   // 62
            { rotate3d, false, Rotate.class },                  // 63
            { rotate3d2d, true, Rotate.class },                 // 64
            { rotate3dUpsideDown2d, true, Rotate.class },       // 65
            { rotateZeroAxis, true, Rotate.class },             // 66
            { rotate2dNoPivot, true, Rotate.class },            // 67
            { rotate3dNoPivot, false, Rotate.class },           // 68
            { rotate2dPivot3d, true, Rotate.class },            // 69
            { noRotate, true, Rotate.class },                   // 70
            { immutable_identity, true, Affine.class },         // 71
            { immutable_translate, true, Affine.class },        // 72
            { immutable_translate_only, true, Affine.class },   // 73
            { immutable_scale, true, Affine.class },            // 74
            { immutable_sc_tr, true, Affine.class },            // 75
            { immutable_shear, true, Affine.class },            // 76
            { immutable_sh_tr, true, Affine.class },            // 77
            { immutable_sh_sc_simple, true, Affine.class },     // 78
            { immutable_sh_sc, true, Affine.class },            // 79
            { immutable_sh_sc_tr, true, Affine.class },         // 80
            { immutable_3d_tr, false, Affine.class },           // 81
            { immutable_3d_sc, false, Affine.class },           // 82
            { immutable_3d_sc_tr, false, Affine.class },        // 83
            { immutable_3d_sc2_tr3, false, Affine.class },      // 84
            { immutable_3d_sc3_tr2, false, Affine.class },      // 85
            { immutable_3d_withShear, false, Affine.class },    // 86
            { immutable_3d_only3d, false, Affine.class },       // 87
            { immutable_3d_translate_only, false, null },       // 88
            { immutable_3d_complex, false, Affine.class },      // 89
            { immutable_3d_complex_noninvertible, false, null },// 90
            { immutable_empty, false, null },                   // 91
            { immutable_emptyZ, false, null },                  // 92
            { immutable_emptyXY, true, null },                  // 93
            { immutable_nonInv_translate_x, true, null },       // 94
            { immutable_nonInv_translate_y, true, null },       // 95
            { immutable_nonInv_translate_z, false, null },      // 96
            { immutable_nonInv_scale_x, true, null },           // 97
            { immutable_nonInv_scale_y, true, null },           // 98
            { immutable_nonInv_scale_xy, false, null },         // 99
            { immutable_nonInv_scale_z, false, null },          //100
            { immutable_nonInv_shear_x, true, null },           //101
            { immutable_nonInv_shear_y, true, null },           //102
            { immutable_nonInv_sh_tr_x, true, null },           //103
            { immutable_nonInv_sh_tr_y, true, null },           //104
            { immutable_nonInv_sh_sc_tr, true, null },          //105
            { immutable_nonInv_sh_sc, true, null },             //106
            { immutable_nonInv_sh_tr, true, null },             //107
            { immutable_nonInv_sc_tr, true, null },             //108
            { immutable_nonInv_sc_tr_x, true, null },           //109
            { immutable_nonInv_sc_tr_y, true, null },           //110
            { raw_arbitrary, false, Affine.class },             //111
            { raw_arbitrary_nonInvertible, false, null },       //112
            { raw_empty, false, null },                         //113
            { raw_emptyZ, false, null },                        //114
            { raw_emptyXY, true, null },                        //115
        });
    }

    private Transform t;
    private Transform it;
    private boolean is2d, isIdentity;
    private boolean isInvertible;
    private Class inverseType;

    public TransformOperationsTest(Transform t, boolean twoDee, Class inverseType) {
        this.t = t;
        this.is2d = twoDee;
        this.isIdentity =
               (t.getMxx() == 1 && t.getMxy() == 0 && t.getMxz() == 0 && t.getTx() == 0
             && t.getMyx() == 0 && t.getMyy() == 1 && t.getMyz() == 0 && t.getTy() == 0
             && t.getMzx() == 0 && t.getMzy() == 0 && t.getMzz() == 1 && t.getTz() == 0);

        this.it = null;
        this.inverseType = inverseType;
        this.isInvertible = (TransformHelper.determinant(t) != 0);
        if (isInvertible) {
            try {
                it = TransformHelper.invert(t);
            } catch (NonInvertibleTransformException e) {
                // error in test
                throw new RuntimeException("Test is wrong, it must be invertible");
            }
        } else {
            // to avoid non-null checks everywhere
            it = new Affine();
        }
    }

    @Test
    public void testClone() {
        final double mxx = t.getMxx();
        final double mxy = t.getMxy();
        final double mxz = t.getMxz();
        final double tx = t.getTx();
        final double myx = t.getMyx();
        final double myy = t.getMyy();
        final double myz = t.getMyz();
        final double ty = t.getTy();
        final double mzx = t.getMzx();
        final double mzy = t.getMzy();
        final double mzz = t.getMzz();
        final double tz = t.getTz();

        Transform clone = t.clone();

        TransformHelper.assertMatrix(clone,
                mxx, mxy, mxz, tx, myx, myy, myz, ty, mzx, mzy, mzz, tz);

        if (!TransformHelper.modify(clone, 42)) {
            // cannot modify, nothing else to test
            return;
        }

        TransformHelper.assertMatrixDiffers(clone,
                mxx, mxy, mxz, tx, myx, myy, myz, ty, mzx, mzy, mzz, tz);

        TransformHelper.assertMatrix(t,
                mxx, mxy, mxz, tx, myx, myy, myz, ty, mzx, mzy, mzz, tz);
    }

    private Class getExpectedConcatenationClass(Transform t1, Transform t2) {
        Class c1 = t1.getClass();
        Class c2 = t2.getClass();

        if (c1 == Translate.class && c2 == Translate.class) {
            return Translate.class;
        }

        if (c1 == Translate.class && c2 == Scale.class) {
            Translate t = (Translate) t1;
            Scale s = (Scale) t2;

            if ((t.getX() == 0.0 || s.getX() != 1.0) &&
                    (t.getY() == 0.0 || s.getY() != 1.0) &&
                    (t.getZ() == 0.0 || s.getZ() != 1.0)) {
                return Scale.class;
            }
        }

        if (c1 == Scale.class && c2 == Translate.class) {
            Scale s = (Scale) t1;
            Translate tr = (Translate) t2;

            if ((tr.getX() == 0.0 || (s.getX() != 1.0 && s.getX() != 0.0)) &&
                    (tr.getY() == 0.0 || (s.getY() != 1.0 && s.getY() != 0.0)) &&
                    (tr.getZ() == 0.0 || (s.getZ() != 1.0 && s.getY() != 0.0))) {
                return Scale.class;
            }
        }

        if (c1 == Scale.class && c2 == Scale.class) {
            Scale s1 = (Scale) t1;
            Scale s2 = (Scale) t2;

            if (s1.getPivotX() == s2.getPivotX() &&
                    s1.getPivotY() == s2.getPivotY() &&
                    s1.getPivotZ() == s2.getPivotZ()) {
                return Scale.class;
            }
        }

        if (c1 == Rotate.class && c2 == Rotate.class) {
            Rotate r1 = (Rotate) t1;
            Rotate r2 = (Rotate) t2;
            if (r1.getAxis().normalize().equals(r2.getAxis().normalize()) &&
                    r1.getPivotX() == r2.getPivotX() &&
                    r1.getPivotY() == r2.getPivotY() &&
                    r1.getPivotZ() == r2.getPivotZ()) {
                return Rotate.class;
            }
        }

        return Affine.class;
    }

    @Test
    public void testCreateConcatenation() {
        int counter = 0;
        for (Object o : TransformOperationsTest.getParams()) {
            Object[] arr = (Object[]) o;
            Transform other = (Transform) arr[0];

            Transform res = TransformHelper.concatenate(t, other);
            Transform conc = t.createConcatenation(other);

            TransformHelper.assertMatrix("Concatenating with #" + counter,
                    conc, res);
            assertSame("Concatenating with #" + counter,
                    getExpectedConcatenationClass(t, other),
                    conc.getClass());
            counter++;
        }
    }

    @Test(expected=NullPointerException.class)
    public void testCreateConcatenationNullTransform() {
        t.createConcatenation(null);
    }

    @Test
    public void testCreateInverse() {
        Transform res = null;
        try {
            res = t.createInverse();
        } catch(NonInvertibleTransformException e) {
            if (isInvertible) {
                e.printStackTrace();
                fail("NonInvertibleTransformException thrown for invertible transform");
            } else {
                // ok
                return;
            }
        }

        if (!isInvertible) {
            fail("Should have thrown NonInvertibleTransformException");
        }

        assertNotNull(res);
        assertSame(inverseType, res.getClass());
        TransformHelper.assertMatrix(res, it);
    }

    @Test
    public void createInverseShouldUpdateCache() {
        Transform ct = t.clone();
        Transform res = null;
        boolean canInvert = isInvertible;
        try {
            res = ct.createInverse();
        } catch(NonInvertibleTransformException e) {
            if (canInvert) {
                e.printStackTrace();
                fail("NonInvertibleTransformException thrown for invertible transform");
            } else {
                // ok
                return;
            }
        }

        if (!canInvert) {
            fail("Should have thrown NonInvertibleTransformException");
        }

        assertNotNull(res);
        assertSame(inverseType, res.getClass());
        TransformHelper.assertMatrix(res, it);

        // modify the matrix to check the cache keeps up

        TransformHelper.modify(ct, 43);
        Transform inv = null;
        try {
            inv = TransformHelper.invert(ct);
            canInvert = true;
        } catch (NonInvertibleTransformException e) {
            canInvert = false;
        }

        try {
            res = ct.createInverse();
        } catch(NonInvertibleTransformException e) {
            if (canInvert) {
                e.printStackTrace();
                fail("NonInvertibleTransformException thrown for invertible transform");
            } else {
                // ok
                return;
            }
        }

        if (!isInvertible) {
            fail("Should have thrown NonInvertibleTransformException");
        }

        assertNotNull(res);
        TransformHelper.assertMatrix(res, inv);

        // emulate garbage collection of the cache to check it's renewed
        ct.clearInverseCache();

        try {
            res = ct.createInverse();
        } catch(NonInvertibleTransformException e) {
            if (canInvert) {
                e.printStackTrace();
                fail("NonInvertibleTransformException thrown for invertible transform");
            } else {
                // ok
                return;
            }
        }

        if (!isInvertible) {
            fail("Should have thrown NonInvertibleTransformException");
        }

        assertNotNull(res);
        TransformHelper.assertMatrix(res, inv);
    }

    @Test
    public void testTransformPoint3d() {
        Point3D p = new Point3D(12, -18, 30);
        Point3D expected = new Point3D(
            t.getMxx() * 12 - t.getMxy() * 18 + t.getMxz() * 30 + t.getTx(),
            t.getMyx() * 12 - t.getMyy() * 18 + t.getMyz() * 30 + t.getTy(),
            t.getMzx() * 12 - t.getMzy() * 18 + t.getMzz() * 30 + t.getTz());


        Point3D result = t.transform(p);
        assertEquals(expected.getX(), result.getX(), 0.00001);
        assertEquals(expected.getY(), result.getY(), 0.00001);
        assertEquals(expected.getZ(), result.getZ(), 0.00001);

        result = t.transform(12, -18, 30);
        assertEquals(expected.getX(), result.getX(), 0.00001);
        assertEquals(expected.getY(), result.getY(), 0.00001);
        assertEquals(expected.getZ(), result.getZ(), 0.00001);
    }

    @Test(expected=NullPointerException.class)
    public void testTransformNullPoint3D() {
        t.transform((Point3D) null);
    }

    @Test
    public void testTransformPoint2d() {

        Point2D p = new Point2D(12, -18);
        Point2D expected = new Point2D(
            t.getMxx() * 12 - t.getMxy() * 18 + t.getTx(),
            t.getMyx() * 12 - t.getMyy() * 18 + t.getTy());

        try {
            Point2D result = t.transform(p);
            if (!is2d) {
                fail("Should have thrown ISE");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        }

        try {
            Point2D result = t.transform(12, -18);
            if (!is2d) {
                fail("Should have thrown ISE");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=NullPointerException.class)
    public void testTransformNullPoint2D() {
        t.transform((Point2D) null);
    }

    @Test
    public void testDeltaTransformPoint3d() {
        Point3D p = new Point3D(12, -18, 30);
        Point3D expected = new Point3D(
            t.getMxx() * 12 - t.getMxy() * 18 + t.getMxz() * 30,
            t.getMyx() * 12 - t.getMyy() * 18 + t.getMyz() * 30,
            t.getMzx() * 12 - t.getMzy() * 18 + t.getMzz() * 30);


        Point3D result = t.deltaTransform(p);
        assertEquals(expected.getX(), result.getX(), 0.00001);
        assertEquals(expected.getY(), result.getY(), 0.00001);
        assertEquals(expected.getZ(), result.getZ(), 0.00001);

        result = t.deltaTransform(12, -18, 30);
        assertEquals(expected.getX(), result.getX(), 0.00001);
        assertEquals(expected.getY(), result.getY(), 0.00001);
        assertEquals(expected.getZ(), result.getZ(), 0.00001);
    }

    @Test(expected=NullPointerException.class)
    public void testDeltaTransformNullPoint3D() {
        t.deltaTransform((Point3D) null);
    }

    @Test
    public void testDeltaTransformPoint2d() {

        Point2D p = new Point2D(12, -18);
        Point2D expected = new Point2D(
            t.getMxx() * 12 - t.getMxy() * 18,
            t.getMyx() * 12 - t.getMyy() * 18);

        try {
            Point2D result = t.deltaTransform(p);
            if (!is2d) {
                fail("Should have thrown ISE");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        }

        try {
            Point2D result = t.deltaTransform(12, -18);
            if (!is2d) {
                fail("Should have thrown ISE");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=NullPointerException.class)
    public void testDeltaTransformNullPoint2D() {
        t.deltaTransform((Point2D) null);
    }

    @Test
    public void testTransformBounds() {
        Bounds result = t.transform(new BoundingBox(10, 11, 12, 13, 14, 15));

        Point3D expected1 = new Point3D(
            t.getMxx() * 10 + t.getMxy() * 11 + t.getMxz() * 12 + t.getTx(),
            t.getMyx() * 10 + t.getMyy() * 11 + t.getMyz() * 12 + t.getTy(),
            t.getMzx() * 10 + t.getMzy() * 11 + t.getMzz() * 12 + t.getTz());

        Point3D expected2 = new Point3D(
            t.getMxx() * 23 + t.getMxy() * 25 + t.getMxz() * 27 + t.getTx(),
            t.getMyx() * 23 + t.getMyy() * 25 + t.getMyz() * 27 + t.getTy(),
            t.getMzx() * 23 + t.getMzy() * 25 + t.getMzz() * 27 + t.getTz());

        assertEquals(expected1.getX(), result.getMinX(), 0.00001);
        assertEquals(expected1.getY(), result.getMinY(), 0.00001);
        assertEquals(expected1.getZ(), result.getMinZ(), 0.00001);
        assertEquals(expected2.getX(), result.getMaxX(), 0.00001);
        assertEquals(expected2.getY(), result.getMaxY(), 0.00001);
        assertEquals(expected2.getZ(), result.getMaxZ(), 0.00001);
    }

    @Test(expected=NullPointerException.class)
    public void testTransformNullBounds() {
        t.transform((Bounds) null);
    }

    @Test
    public void testTransform2DPoints() {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6 };
        double[] dstPts = new double[] { 1, 2, 3, 4, 5, 6 };

        Point2D expected1 = new Point2D(
            t.getMxx() * 3 + t.getMxy() * 4 + t.getTx(),
            t.getMyx() * 3 + t.getMyy() * 4 + t.getTy());

        Point2D expected2 = new Point2D(
            t.getMxx() * 5 + t.getMxy() * 6 + t.getTx(),
            t.getMyx() * 5 + t.getMyy() * 6 + t.getTy());

        try {
            t.transform2DPoints(srcPts, 3, dstPts, 1, 2);
            if (!is2d) {
                fail("Should have thrown ISE");
            }

            assertEquals(1, dstPts[0], 0.00001);
            assertEquals(expected1.getX(), dstPts[1], 0.00001);
            assertEquals(expected1.getY(), dstPts[2], 0.00001);
            assertEquals(expected2.getX(), dstPts[3], 0.00001);
            assertEquals(expected2.getY(), dstPts[4], 0.00001);
            assertEquals(6, dstPts[5], 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=NullPointerException.class)
    public void testTransform2DPointsBothPtsNull() {
        t.transform2DPoints(null, 2, null, 0, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testTransform2DPointsSrcPtsNull() {
        t.transform2DPoints(null, 2, new double[] { 1, 2 }, 0, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testTransform2DPointsDstPtsNull() {
        t.transform2DPoints(new double[] { 1, 2, 3, 4 }, 2, null, 0, 0);
    }

    @Test
    public void testTransform2DPointsWithOverlap() {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };

        Point2D expected1 = new Point2D(
            t.getMxx() * 2 + t.getMxy() * 3 + t.getTx(),
            t.getMyx() * 2 + t.getMyy() * 3 + t.getTy());

        Point2D expected2 = new Point2D(
            t.getMxx() * 4 + t.getMxy() * 5 + t.getTx(),
            t.getMyx() * 4 + t.getMyy() * 5 + t.getTy());

        try {
            t.transform2DPoints(srcPts, 2, srcPts, 4, 2);
            if (!is2d) {
                fail("Should have thrown ISE");
            }

            assertEquals(0, srcPts[0], 0.00001);
            assertEquals(1, srcPts[1], 0.00001);
            assertEquals(2, srcPts[2], 0.00001);
            assertEquals(3, srcPts[3], 0.00001);
            assertEquals(expected1.getX(), srcPts[4], 0.00001);
            assertEquals(expected1.getY(), srcPts[5], 0.00001);
            assertEquals(expected2.getX(), srcPts[6], 0.00001);
            assertEquals(expected2.getY(), srcPts[7], 0.00001);
            assertEquals(8, srcPts[8], 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testTransform2DPointsSrcOut() {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        double[] dstPts = new double[] { 1, 2, 3, 4, 5, 6 };

        try {
            t.transform2DPoints(srcPts, 3, dstPts, 0, 3);
        } catch (IllegalStateException e) {
            if (!is2d) {
                throw new IndexOutOfBoundsException("expected result");
            }
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testTransform2DPointsDstOut() {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        double[] dstPts = new double[] { 1 };

        try {
            t.transform2DPoints(srcPts, 1, dstPts, 0, 2);
        } catch (IllegalStateException e) {
            if (!is2d) {
                throw new IndexOutOfBoundsException("expected result");
            }
        }
    }

    @Test
    public void testTransform3DPoints() {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
        double[] dstPts = new double[] { 1, 2, 3, 4, 5, 6, 7, 8 };

        Point3D expected1 = new Point3D(
            t.getMxx() * 3 + t.getMxy() * 4 + t.getMxz() * 5 + t.getTx(),
            t.getMyx() * 3 + t.getMyy() * 4 + t.getMyz() * 5 + t.getTy(),
            t.getMzx() * 3 + t.getMzy() * 4 + t.getMzz() * 5 + t.getTz());

        Point3D expected2 = new Point3D(
            t.getMxx() * 6 + t.getMxy() * 7 + t.getMxz() * 8 + t.getTx(),
            t.getMyx() * 6 + t.getMyy() * 7 + t.getMyz() * 8 + t.getTy(),
            t.getMzx() * 6 + t.getMzy() * 7 + t.getMzz() * 8 + t.getTz());

        t.transform3DPoints(srcPts, 3, dstPts, 1, 2);

        assertEquals(1, dstPts[0], 0.00001);
        assertEquals(expected1.getX(), dstPts[1], 0.00001);
        assertEquals(expected1.getY(), dstPts[2], 0.00001);
        assertEquals(expected1.getZ(), dstPts[3], 0.00001);
        assertEquals(expected2.getX(), dstPts[4], 0.00001);
        assertEquals(expected2.getY(), dstPts[5], 0.00001);
        assertEquals(expected2.getZ(), dstPts[6], 0.00001);
        assertEquals(8, dstPts[7], 0.00001);
    }

    @Test(expected=NullPointerException.class)
    public void testTransform3DPointsBothPtsNull() {
        t.transform3DPoints(null, 2, null, 0, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testTransform3DPointsSrcPtsNull() {
        t.transform3DPoints(null, 2, new double[] { 1, 2, 3 }, 0, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testTransform3DPointsDstPtsNull() {
        t.transform3DPoints(new double[] { 1, 2, 3, 4 }, 2, null, 0, 0);
    }

    @Test
    public void testTransform3DPointsWithOverlap() {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

        Point3D expected1 = new Point3D(
            t.getMxx() * 2 + t.getMxy() * 3 + t.getMxz() * 4 + t.getTx(),
            t.getMyx() * 2 + t.getMyy() * 3 + t.getMyz() * 4 + t.getTy(),
            t.getMzx() * 2 + t.getMzy() * 3 + t.getMzz() * 4 + t.getTz());

        Point3D expected2 = new Point3D(
            t.getMxx() * 5 + t.getMxy() * 6 + t.getMxz() * 7 + t.getTx(),
            t.getMyx() * 5 + t.getMyy() * 6 + t.getMyz() * 7 + t.getTy(),
            t.getMzx() * 5 + t.getMzy() * 6 + t.getMzz() * 7 + t.getTz());

        t.transform3DPoints(srcPts, 2, srcPts, 3, 2);

        assertEquals(0, srcPts[0], 0.00001);
        assertEquals(1, srcPts[1], 0.00001);
        assertEquals(2, srcPts[2], 0.00001);
        assertEquals(expected1.getX(), srcPts[3], 0.00001);
        assertEquals(expected1.getY(), srcPts[4], 0.00001);
        assertEquals(expected1.getZ(), srcPts[5], 0.00001);
        assertEquals(expected2.getX(), srcPts[6], 0.00001);
        assertEquals(expected2.getY(), srcPts[7], 0.00001);
        assertEquals(expected2.getZ(), srcPts[8], 0.00001);
        assertEquals(9, srcPts[9], 0.00001);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testTransform3DPointsSrcOut() {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        double[] dstPts = new double[] { 1, 2, 3, 4, 5, 6 };

        t.transform3DPoints(srcPts, 6, dstPts, 0, 1);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testTransform3DPointsDstOut() {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        double[] dstPts = new double[] { 1 };

        t.transform3DPoints(srcPts, 1, dstPts, 0, 1);
    }

    @Test
    public void testInverseTransformPoint3d() throws Exception {
        Point3D p = new Point3D(12, -18, 30);

        Point3D expected = new Point3D(
            it.getMxx() * 12 - it.getMxy() * 18 + it.getMxz() * 30 + it.getTx(),
            it.getMyx() * 12 - it.getMyy() * 18 + it.getMyz() * 30 + it.getTy(),
            it.getMzx() * 12 - it.getMzy() * 18 + it.getMzz() * 30 + it.getTz());

        try {
            Point3D result = t.inverseTransform(p);
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
            assertEquals(expected.getZ(), result.getZ(), 0.00001);
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }

        try {
            Point3D result = t.inverseTransform(12, -18, 30);
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
            assertEquals(expected.getZ(), result.getZ(), 0.00001);
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=NullPointerException.class)
    public void testInverseTransformNullPoint3D()
            throws NonInvertibleTransformException {
        t.inverseTransform((Point3D) null);
    }

    @Test
    public void testInverseTransformPoint2d() throws Exception {

        Point2D p = new Point2D(12, -18);
        Point2D expected = new Point2D(
            it.getMxx() * 12 - it.getMxy() * 18 + it.getTx(),
            it.getMyx() * 12 - it.getMyy() * 18 + it.getTy());

        try {
            Point2D result = t.inverseTransform(p);
            if (!is2d) {
                fail("Should have thrown ISE");
            }
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }

        try {
            Point2D result = t.inverseTransform(12, -18);
            if (!is2d) {
                fail("Should have thrown ISE");
            }
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=NullPointerException.class)
    public void testInverseTransformNullPoint2D()
            throws NonInvertibleTransformException {
        t.inverseTransform((Point2D) null);
    }

    @Test
    public void testInverseDeltaTransformPoint3d() throws Exception {
        Point3D p = new Point3D(12, -18, 30);
        Point3D expected = new Point3D(
            it.getMxx() * 12 - it.getMxy() * 18 + it.getMxz() * 30,
            it.getMyx() * 12 - it.getMyy() * 18 + it.getMyz() * 30,
            it.getMzx() * 12 - it.getMzy() * 18 + it.getMzz() * 30);

        try {
            Point3D result = t.inverseDeltaTransform(p);
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
            assertEquals(expected.getZ(), result.getZ(), 0.00001);
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }

        try {
            Point3D result = t.inverseDeltaTransform(12, -18, 30);
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
            assertEquals(expected.getZ(), result.getZ(), 0.00001);
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=NullPointerException.class)
    public void testInverseDeltaTransformNullPoint3D()
            throws NonInvertibleTransformException {
        t.inverseDeltaTransform((Point3D) null);
    }

    @Test
    public void testInverseDeltaTransformPoint2d() throws Exception {

        Point2D p = new Point2D(12, -18);
        Point2D expected = new Point2D(
            it.getMxx() * 12 - it.getMxy() * 18,
            it.getMyx() * 12 - it.getMyy() * 18);

        try {
            Point2D result = t.inverseDeltaTransform(p);
            if (!is2d) {
                fail("Should have thrown ISE");
            }
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }

        try {
            Point2D result = t.inverseDeltaTransform(12, -18);
            if (!is2d) {
                fail("Should have thrown ISE");
            }
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }
            assertEquals(expected.getX(), result.getX(), 0.00001);
            assertEquals(expected.getY(), result.getY(), 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=NullPointerException.class)
    public void testInverseDeltaTransformNullPoint2D()
            throws NonInvertibleTransformException {
        t.inverseDeltaTransform((Point2D) null);
    }

    @Test
    public void testInverseTransformBounds() throws Exception {
        Bounds result = null;
        try {
            result = t.inverseTransform(new BoundingBox(10, 11, 12, 13, 14, 15));
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }

            Point3D expected1 = new Point3D(
                it.getMxx() * 10 + it.getMxy() * 11 + it.getMxz() * 12 + it.getTx(),
                it.getMyx() * 10 + it.getMyy() * 11 + it.getMyz() * 12 + it.getTy(),
                it.getMzx() * 10 + it.getMzy() * 11 + it.getMzz() * 12 + it.getTz());

            Point3D expected2 = new Point3D(
                it.getMxx() * 23 + it.getMxy() * 25 + it.getMxz() * 27 + it.getTx(),
                it.getMyx() * 23 + it.getMyy() * 25 + it.getMyz() * 27 + it.getTy(),
                it.getMzx() * 23 + it.getMzy() * 25 + it.getMzz() * 27 + it.getTz());

            assertEquals(expected1.getX(), result.getMinX(), 0.00001);
            assertEquals(expected1.getY(), result.getMinY(), 0.00001);
            assertEquals(expected1.getZ(), result.getMinZ(), 0.00001);
            assertEquals(expected2.getX(), result.getMaxX(), 0.00001);
            assertEquals(expected2.getY(), result.getMaxY(), 0.00001);
            assertEquals(expected2.getZ(), result.getMaxZ(), 0.00001);

        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
            return;
        }
    }

    @Test(expected=NullPointerException.class)
    public void testInverseTransformNullBounds()
            throws NonInvertibleTransformException {
        t.inverseTransform((Bounds) null);
    }

    @Test
    public void testInverseTransform2DPoints() throws Exception {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6 };
        double[] dstPts = new double[] { 1, 2, 3, 4, 5, 6 };

        Point2D expected1 = new Point2D(
            it.getMxx() * 3 + it.getMxy() * 4 + it.getTx(),
            it.getMyx() * 3 + it.getMyy() * 4 + it.getTy());

        Point2D expected2 = new Point2D(
            it.getMxx() * 5 + it.getMxy() * 6 + it.getTx(),
            it.getMyx() * 5 + it.getMyy() * 6 + it.getTy());

        try {
            t.inverseTransform2DPoints(srcPts, 3, dstPts, 1, 2);
            if (!is2d) {
                fail("Should have thrown ISE");
            }
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }

            assertEquals(1, dstPts[0], 0.00001);
            assertEquals(expected1.getX(), dstPts[1], 0.00001);
            assertEquals(expected1.getY(), dstPts[2], 0.00001);
            assertEquals(expected2.getX(), dstPts[3], 0.00001);
            assertEquals(expected2.getY(), dstPts[4], 0.00001);
            assertEquals(6, dstPts[5], 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=NullPointerException.class)
    public void testInverseTransform2DPointsBothPtsNull()
            throws NonInvertibleTransformException {
        t.inverseTransform2DPoints(null, 2, null, 0, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testInverseTransform2DPointsSrcPtsNull()
            throws NonInvertibleTransformException {
        t.inverseTransform2DPoints(null, 2, new double[] { 1, 2, 3 }, 0, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testInverseTransform2DPointsDstPtsNull()
            throws NonInvertibleTransformException {
        t.inverseTransform2DPoints(new double[] { 1, 2, 3, 4 }, 2, null, 0, 0);
    }

    @Test
    public void testInverseTransform2DPointsWithOverlap() throws Exception {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };

        Point2D expected1 = new Point2D(
            it.getMxx() * 2 + it.getMxy() * 3 + it.getTx(),
            it.getMyx() * 2 + it.getMyy() * 3 + it.getTy());

        Point2D expected2 = new Point2D(
            it.getMxx() * 4 + it.getMxy() * 5 + it.getTx(),
            it.getMyx() * 4 + it.getMyy() * 5 + it.getTy());

        try {
            t.inverseTransform2DPoints(srcPts, 2, srcPts, 4, 2);
            if (!is2d) {
                fail("Should have thrown ISE");
            }
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }
            assertEquals(0, srcPts[0], 0.00001);
            assertEquals(1, srcPts[1], 0.00001);
            assertEquals(2, srcPts[2], 0.00001);
            assertEquals(3, srcPts[3], 0.00001);
            assertEquals(expected1.getX(), srcPts[4], 0.00001);
            assertEquals(expected1.getY(), srcPts[5], 0.00001);
            assertEquals(expected2.getX(), srcPts[6], 0.00001);
            assertEquals(expected2.getY(), srcPts[7], 0.00001);
            assertEquals(8, srcPts[8], 0.00001);
        } catch (IllegalStateException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testInverseTransform2DPointsSrcOut() throws Exception {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        double[] dstPts = new double[] { 1, 2, 3, 4, 5, 6 };

        try {
            t.inverseTransform2DPoints(srcPts, 3, dstPts, 0, 3);
        } catch (IllegalStateException e) {
            if (!is2d) {
                throw new IndexOutOfBoundsException("expected result");
            }
        } catch (NonInvertibleTransformException e) {
            if (!isInvertible) {
                throw new IndexOutOfBoundsException("expected result");
            }
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testInverseTransform2DPointsDstOut() throws Exception {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        double[] dstPts = new double[] { 1 };

        try {
            t.inverseTransform2DPoints(srcPts, 1, dstPts, 0, 2);
        } catch (IllegalStateException e) {
            if (!is2d) {
                throw new IndexOutOfBoundsException("expected result");
            }
        } catch (NonInvertibleTransformException e) {
            if (!isInvertible) {
                throw new IndexOutOfBoundsException("expected result");
            }
        }
    }

    @Test
    public void testInverseTransform3DPoints() throws Exception {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
        double[] dstPts = new double[] { 1, 2, 3, 4, 5, 6, 7, 8 };

        Point3D expected1 = new Point3D(
            it.getMxx() * 3 + it.getMxy() * 4 + it.getMxz() * 5 + it.getTx(),
            it.getMyx() * 3 + it.getMyy() * 4 + it.getMyz() * 5 + it.getTy(),
            it.getMzx() * 3 + it.getMzy() * 4 + it.getMzz() * 5 + it.getTz());

        Point3D expected2 = new Point3D(
            it.getMxx() * 6 + it.getMxy() * 7 + it.getMxz() * 8 + it.getTx(),
            it.getMyx() * 6 + it.getMyy() * 7 + it.getMyz() * 8 + it.getTy(),
            it.getMzx() * 6 + it.getMzy() * 7 + it.getMzz() * 8 + it.getTz());

        try {
            t.inverseTransform3DPoints(srcPts, 3, dstPts, 1, 2);
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }

            assertEquals(1, dstPts[0], 0.00001);
            assertEquals(expected1.getX(), dstPts[1], 0.00001);
            assertEquals(expected1.getY(), dstPts[2], 0.00001);
            assertEquals(expected1.getZ(), dstPts[3], 0.00001);
            assertEquals(expected2.getX(), dstPts[4], 0.00001);
            assertEquals(expected2.getY(), dstPts[5], 0.00001);
            assertEquals(expected2.getZ(), dstPts[6], 0.00001);
            assertEquals(8, dstPts[7], 0.00001);
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=NullPointerException.class)
    public void testInverseTransform3DPointsBothPtsNull()
            throws NonInvertibleTransformException {
        t.inverseTransform3DPoints(null, 2, null, 0, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testInverseTransform3DPointsSrcPtsNull()
            throws NonInvertibleTransformException {
        t.inverseTransform3DPoints(null, 2, new double[] { 1, 2, 3 }, 0, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testInverseTransform3DPointsDstPtsNull()
            throws NonInvertibleTransformException {
        t.inverseTransform3DPoints(new double[] { 1, 2, 3, 4 }, 2, null, 0, 0);
    }

    @Test
    public void testInverseTransform3DPointsWithOverlap() throws Exception {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

        Point3D expected1 = new Point3D(
            it.getMxx() * 2 + it.getMxy() * 3 + it.getMxz() * 4 + it.getTx(),
            it.getMyx() * 2 + it.getMyy() * 3 + it.getMyz() * 4 + it.getTy(),
            it.getMzx() * 2 + it.getMzy() * 3 + it.getMzz() * 4 + it.getTz());

        Point3D expected2 = new Point3D(
            it.getMxx() * 5 + it.getMxy() * 6 + it.getMxz() * 7 + it.getTx(),
            it.getMyx() * 5 + it.getMyy() * 6 + it.getMyz() * 7 + it.getTy(),
            it.getMzx() * 5 + it.getMzy() * 6 + it.getMzz() * 7 + it.getTz());

        try {
            t.inverseTransform3DPoints(srcPts, 2, srcPts, 3, 2);
            if (!isInvertible) {
                fail("Should have thrown NonInvertibleTransformException");
            }

            assertEquals(0, srcPts[0], 0.00001);
            assertEquals(1, srcPts[1], 0.00001);
            assertEquals(2, srcPts[2], 0.00001);
            assertEquals(expected1.getX(), srcPts[3], 0.00001);
            assertEquals(expected1.getY(), srcPts[4], 0.00001);
            assertEquals(expected1.getZ(), srcPts[5], 0.00001);
            assertEquals(expected2.getX(), srcPts[6], 0.00001);
            assertEquals(expected2.getY(), srcPts[7], 0.00001);
            assertEquals(expected2.getZ(), srcPts[8], 0.00001);
            assertEquals(9, srcPts[9], 0.00001);
        } catch (NonInvertibleTransformException e) {
            if (isInvertible) {
                fail("Wrong exception thrown");
            }
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testInverseTransform3DPointsSrcOut() throws Exception {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        double[] dstPts = new double[] { 1, 2, 3, 4, 5, 6 };

        try {
            t.inverseTransform3DPoints(srcPts, 6, dstPts, 0, 1);
        } catch (NonInvertibleTransformException e) {
            if (!isInvertible) {
                throw new IndexOutOfBoundsException("expected result");
            }
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testInverseTransform3DPointsDstOut() throws Exception {
        double[] srcPts = new double[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        double[] dstPts = new double[] { 1 };

        try {
            t.inverseTransform3DPoints(srcPts, 1, dstPts, 0, 1);
        } catch (NonInvertibleTransformException e) {
            if (!isInvertible) {
                throw new IndexOutOfBoundsException("expected result");
            }
        }
    }

    @Test
    public void testDeterminant() {
        assertEquals(TransformHelper.determinant(t), t.determinant(), 0.00001);
    }

    @Test
    public void testIsType2D() {
        Transform clone = t.clone();

        if (is2d) {
            assertTrue(t.isType2D());
            if (TransformHelper.make3D(clone)) {
                assertFalse(clone.isType2D());
            }
        } else {
            assertFalse(t.isType2D());
            if (TransformHelper.make2D(clone)) {
                assertTrue(clone.isType2D());
            }
        }
    }

    @Test
    public void testType2DProperty() {
        Transform clone = t.clone();

        assertEquals("type2D", clone.type2DProperty().getName());
        assertSame(clone, clone.type2DProperty().getBean());
    }

    @Test
    public void testType2DPropertyGetter() {
        Transform clone = t.clone();

        if (is2d) {
            assertTrue(clone.type2DProperty().get());
            assertTrue(clone.isType2D());
            if (TransformHelper.make3D(clone)) {
                assertFalse(clone.type2DProperty().get());
                assertFalse(clone.isType2D());
            }
        } else {
            assertFalse(clone.type2DProperty().get());
            assertFalse(clone.isType2D());
            if (TransformHelper.make2D(clone)) {
                assertTrue(clone.type2DProperty().get());
                assertTrue(clone.isType2D());
            }
        }
    }

    @Test
    public void testType2DPropertyInvalidation() {
        final Transform clone = t.clone();

        InvalidationListener l =
                new InvalidationListener() {
                    @Override public void invalidated(Observable valueModel) {
                         if (is2d) {
                             assertFalse(clone.type2DProperty().get());
                         } else {
                             assertTrue(clone.type2DProperty().get());
                         }
                         listenerCalled = true;
                    }
                 };

        clone.type2DProperty().addListener(l);

        listenerCalled = false;

        if (is2d) {
            if (TransformHelper.make3D(clone)) {
                assertTrue(listenerCalled);
            }
        } else {
            if (TransformHelper.make2D(clone)) {
                assertTrue(listenerCalled);
            }
        }

        listenerCalled = false;
        clone.type2DProperty().removeListener(l);

        if (is2d) {
            TransformHelper.make2D(clone);
            assertFalse(listenerCalled);
        } else {
            TransformHelper.make3D(clone);
            assertFalse(listenerCalled);
        }
    }

    @Test
    public void testType2DPropertyChange() {
        final Transform clone = t.clone();

        ChangeListener<Boolean> l =
            new ChangeListener<Boolean>() {
                @Override public void changed(
                        ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {

                    if ((is2d && (eventCounter == 0 || eventCounter == 2))
                            || (!is2d && eventCounter == 1)) {
                        assertTrue(oldValue);
                        assertFalse(newValue);
                        assertFalse(clone.type2DProperty().get());
                    } else {
                        assertFalse(oldValue);
                        assertTrue(newValue);
                        assertTrue(clone.type2DProperty().get());
                    }

                    listenerCalled = true;
                    eventCounter++;
                }
            };

        clone.type2DProperty().addListener(l);

        listenerCalled = false;
        eventCounter = 0;
        TransformHelper.modify(clone, 42);
        assertFalse(listenerCalled);

        if (is2d) {
            if (TransformHelper.make3D(clone)) {
                assertTrue(listenerCalled);
                listenerCalled = false;
                TransformHelper.make3D(clone);
                assertFalse(listenerCalled);
                TransformHelper.make2D(clone);
                assertTrue(listenerCalled);
                listenerCalled = false;
                clone.type2DProperty().removeListener(l);
                TransformHelper.make3D(clone);
                assertFalse(listenerCalled);
            }
        } else {
            if (TransformHelper.make2D(clone)) {
                assertTrue(listenerCalled);
                listenerCalled = false;
                TransformHelper.make2D(clone);
                assertFalse(listenerCalled);
                TransformHelper.make3D(clone);
                assertTrue(listenerCalled);
                listenerCalled = false;
                clone.type2DProperty().removeListener(l);
                TransformHelper.make2D(clone);
                assertFalse(listenerCalled);
            }
        }
    }

    @Test
    public void testIsIdentity() {
        Transform clone = t.clone();

        if (isIdentity) {
            assertTrue(t.isIdentity());
            if (TransformHelper.modify(clone, 42)) {
                assertFalse(clone.isIdentity());
            }
        } else {
            assertFalse(t.isIdentity());
            if (TransformHelper.makeIdentity(clone)) {
                assertTrue(clone.isIdentity());
            }
        }
    }

    @Test
    public void testIdentityProperty() {
        Transform clone = t.clone();

        assertEquals("identity", clone.identityProperty().getName());
        assertSame(clone, clone.identityProperty().getBean());
    }

    @Test
    public void testIdentityPropertyGetter() {
        Transform clone = t.clone();

        if (isIdentity) {
            assertTrue(clone.identityProperty().get());
            assertTrue(clone.isIdentity());
            if (TransformHelper.modify(clone, 42)) {
                assertFalse(clone.identityProperty().get());
                assertFalse(clone.isIdentity());
            }
        } else {
            assertFalse(clone.identityProperty().get());
            assertFalse(clone.isIdentity());
            if (TransformHelper.makeIdentity(clone)) {
                assertTrue(clone.identityProperty().get());
                assertTrue(clone.isIdentity());
            }
        }
    }

    @Test
    public void testIdentityPropertyInvalidation() {
        final Transform clone = t.clone();

        InvalidationListener l =
                new InvalidationListener() {
                    @Override public void invalidated(Observable valueModel) {
                         if (isIdentity) {
                             if (!clone.identityProperty().get()) {
                                listenerCalled = true;
                             }
                         } else {
                             if (clone.identityProperty().get()) {
                                listenerCalled = true;
                             }
                         }
                    }
                 };

        clone.identityProperty().addListener(l);

        listenerCalled = false;

        if (isIdentity) {
            if (TransformHelper.modify(clone, 42)) {
                assertTrue(listenerCalled);
            }
        } else {
            if (TransformHelper.makeIdentity(clone)) {
                assertTrue(listenerCalled);
            }
        }

        listenerCalled = false;
        clone.identityProperty().removeListener(l);

        if (isIdentity) {
            TransformHelper.makeIdentity(clone);
            TransformHelper.modify(clone, 42);
            assertFalse(listenerCalled);
        } else {
            TransformHelper.modify(clone, 42);
            TransformHelper.makeIdentity(clone);
            assertFalse(listenerCalled);
        }
    }

    @Test
    public void testIdentityPropertyChange() {
        final Transform clone = t.clone();

        ChangeListener<Boolean> l =
            new ChangeListener<Boolean>() {
                @Override public void changed(
                        ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                    if (isIdentity) {
                        if (oldValue == true && newValue == false
                                && clone.identityProperty().get() == false) {
                            listenerCalled = true;
                        }
                    } else {
                        if (oldValue == false && newValue == true
                                && clone.identityProperty().get() == true) {
                            listenerCalled = true;
                        }
                    }

                }
            };

        clone.identityProperty().addListener(l);

        listenerCalled = false;

        if (isIdentity) {
            if (TransformHelper.modify(clone, 42)) {
                assertTrue(listenerCalled);
                listenerCalled = false;
                TransformHelper.modify(clone, 43);
                assertFalse(listenerCalled);

                clone.identityProperty().removeListener(l);
                TransformHelper.makeIdentity(clone);
                TransformHelper.modify(clone, 42);
                assertFalse(listenerCalled);
            }
        } else {
            TransformHelper.modify(clone, 42);
            assertFalse(listenerCalled);
            if (TransformHelper.makeIdentity(clone)) {
                assertTrue(listenerCalled);
                listenerCalled = false;
                TransformHelper.makeIdentity(clone);
                assertFalse(listenerCalled);

                clone.identityProperty().removeListener(l);
                TransformHelper.modify(clone, 42);
                TransformHelper.makeIdentity(clone);
                assertFalse(listenerCalled);
            }
        }
    }

    @Test
    public void testSimilarTo() {
        Transform clone = t.clone();

        assertTrue(t.similarTo(clone, new BoundingBox(-10000, -10000, 10000, 10000), 1e-10));

        if (TransformHelper.tinyModify(clone)) {
            assertTrue(t.similarTo(clone, new BoundingBox(0, 0, 1, 1), 5));
            if (is2d) {
                assertTrue(t.similarTo(clone, new BoundingBox(0, 0, 0, 0.1, 0.1, 10000), 2));
            } else {
                assertFalse(t.similarTo(clone, new BoundingBox(0, 0, 0, 0.1, 0.1, 10000), 2));
            }
            assertFalse(t.similarTo(clone, new BoundingBox(0, 0, 1000, 1000), 0.5));

            if (t instanceof Translate) {
                assertTrue(t.similarTo(clone, new BoundingBox(0, 0, 1, 1), 4));
                assertFalse(t.similarTo(clone, new BoundingBox(0, 0, 1, 1), 0.5));
            }

            if (t instanceof Scale) {
                assertTrue(t.similarTo(clone, new BoundingBox(0, 0, 1, 1), 5));
                assertFalse(t.similarTo(clone, new BoundingBox(0, 0, 1000, 1000), 5));
            }

        }
    }

    @Test(expected=NullPointerException.class)
    public void testSimilarToNullTransform() {
        t.similarTo(null, new BoundingBox(0, 0, 0, 1, 1, 1), 0);
    }

    @Test(expected=NullPointerException.class)
    public void testSimilarToNullRange() {
        t.similarTo(t, null, 0);
    }

    private void assertGetElement(MatrixType type, int row, int col,
            double expected, boolean iae, boolean iob) {
        double res = Double.MIN_VALUE;

        try {
            res = t.getElement(type, row, col);
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

        assertEquals(expected, res, 1e-100);
    }

    @Test
    public void testGetElement() {
        assertGetElement(MatrixType.MT_2D_2x3, 0, 0, t.getMxx(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_2x3, 0, 1, t.getMxy(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_2x3, 0, 2, t.getTx(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_2x3, 1, 0, t.getMyx(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_2x3, 1, 1, t.getMyy(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_2x3, 1, 2, t.getTy(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_2x3, -1, 0, 0, !is2d, true);
        assertGetElement(MatrixType.MT_2D_2x3, 2, 1, 0, !is2d, true);
        assertGetElement(MatrixType.MT_2D_2x3, 1, 3, 0, !is2d, true);
        assertGetElement(MatrixType.MT_2D_2x3, 1, -1, 0, !is2d, true);
        assertGetElement(MatrixType.MT_2D_3x3, 0, 0, t.getMxx(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_3x3, 0, 1, t.getMxy(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_3x3, 0, 2, t.getTx(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_3x3, 1, 0, t.getMyx(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_3x3, 1, 1, t.getMyy(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_3x3, 1, 2, t.getTy(), !is2d, false);
        assertGetElement(MatrixType.MT_2D_3x3, 2, 0, 0, !is2d, false);
        assertGetElement(MatrixType.MT_2D_3x3, 2, 1, 0, !is2d, false);
        assertGetElement(MatrixType.MT_2D_3x3, 2, 2, 1, !is2d, false);
        assertGetElement(MatrixType.MT_2D_3x3, -1, 0, 0, !is2d, true);
        assertGetElement(MatrixType.MT_2D_3x3, 3, 1, 0, !is2d, true);
        assertGetElement(MatrixType.MT_2D_3x3, 1, 3, 0, !is2d, true);
        assertGetElement(MatrixType.MT_2D_3x3, 1, -1, 0, !is2d, true);
        assertGetElement(MatrixType.MT_3D_3x4, 0, 0, t.getMxx(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, 0, 1, t.getMxy(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, 0, 2, t.getMxz(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, 0, 3, t.getTx(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, 1, 0, t.getMyx(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, 1, 1, t.getMyy(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, 1, 2, t.getMyz(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, 1, 3, t.getTy(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, 2, 0, t.getMzx(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, 2, 1, t.getMzy(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, 2, 2, t.getMzz(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, 2, 3, t.getTz(), false, false);
        assertGetElement(MatrixType.MT_3D_3x4, -1, 0, 0, false, true);
        assertGetElement(MatrixType.MT_3D_3x4, 3, 1, 0, false, true);
        assertGetElement(MatrixType.MT_3D_3x4, 1, 4, 0, false, true);
        assertGetElement(MatrixType.MT_3D_3x4, 1, -1, 0, false, true);
        assertGetElement(MatrixType.MT_3D_4x4, 0, 0, t.getMxx(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 0, 1, t.getMxy(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 0, 2, t.getMxz(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 0, 3, t.getTx(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 1, 0, t.getMyx(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 1, 1, t.getMyy(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 1, 2, t.getMyz(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 1, 3, t.getTy(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 2, 0, t.getMzx(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 2, 1, t.getMzy(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 2, 2, t.getMzz(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 2, 3, t.getTz(), false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 3, 0, 0, false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 3, 1, 0, false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 3, 2, 0, false, false);
        assertGetElement(MatrixType.MT_3D_4x4, 3, 3, 1, false, false);
        assertGetElement(MatrixType.MT_3D_4x4, -1, 0, 0, false, true);
        assertGetElement(MatrixType.MT_3D_4x4, 4, 1, 0, false, true);
        assertGetElement(MatrixType.MT_3D_4x4, 1, 4, 0, false, true);
        assertGetElement(MatrixType.MT_3D_4x4, 1, -1, 0, false, true);
    }

    @Test(expected=NullPointerException.class)
    public void testGetElementNullType() {
        t.getElement(null, 0, 0);
    }

    private void assertArray(MatrixType type, double[] a, Transform t) {
        switch (type) {
            case MT_2D_3x3:
                assertEquals(0, a[6], 1e-100);
                assertEquals(0, a[7], 1e-100);
                assertEquals(1, a[8], 1e-100);
                // fallthrough
            case MT_2D_2x3:
                assertEquals(t.getMxx(), a[0], 1e-100);
                assertEquals(t.getMxy(), a[1], 1e-100);
                assertEquals(t.getTx(),  a[2], 1e-100);
                assertEquals(t.getMyx(), a[3], 1e-100);
                assertEquals(t.getMyy(), a[4], 1e-100);
                assertEquals(t.getTy(),  a[5], 1e-100);
                break;
            case MT_3D_4x4:
                assertEquals(0, a[12], 1e-100);
                assertEquals(0, a[13], 1e-100);
                assertEquals(0, a[14], 1e-100);
                assertEquals(1, a[15], 1e-100);
                // fallthrough
            case MT_3D_3x4:
                assertEquals(t.getMxx(), a[0], 1e-100);
                assertEquals(t.getMxy(), a[1], 1e-100);
                assertEquals(t.getMxz(), a[2], 1e-100);
                assertEquals(t.getTx(),  a[3], 1e-100);
                assertEquals(t.getMyx(), a[4], 1e-100);
                assertEquals(t.getMyy(), a[5], 1e-100);
                assertEquals(t.getMyz(), a[6], 1e-100);
                assertEquals(t.getTy(),  a[7], 1e-100);
                assertEquals(t.getMzx(), a[8], 1e-100);
                assertEquals(t.getMzy(), a[9], 1e-100);
                assertEquals(t.getMzz(), a[10], 1e-100);
                assertEquals(t.getTz(),  a[11], 1e-100);
                break;
        }
    }

    private void assertToArray2D(MatrixType type, double[] tmp,
            boolean shouldPass, boolean shouldUse) {
        double[] a = null;
        try {
            if (shouldPass) {
                a = t.toArray(type, tmp);
            } else {
                a = t.toArray(type);
            }
        } catch (IllegalArgumentException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
            return;
        }

        if (!is2d) {
            fail("Should have thrown IAE");
        } else {
            assertNotNull(a);
            if (shouldUse) {
                assertSame(tmp, a);
            }
            assertArray(type, a, t);
        }
    }

    private void assertToArray3D(MatrixType type, double[] tmp,
            boolean shouldPass, boolean shouldUse) {
        double[] a = null;

        if (shouldPass) {
            a = t.toArray(type, tmp);
        } else {
            a = t.toArray(type);
        }

        assertNotNull(a);
        if (shouldUse) {
            assertSame(tmp, a);
        }
        assertArray(type, a, t);
    }

    @Test
    public void testToArray() {

        assertToArray2D(MatrixType.MT_2D_2x3, null, false, false);
        assertToArray2D(MatrixType.MT_2D_2x3, null, true, false);
        assertToArray2D(MatrixType.MT_2D_2x3, new double[4], true, false);
        assertToArray2D(MatrixType.MT_2D_2x3, new double[6], true, true);

        assertToArray2D(MatrixType.MT_2D_3x3, null, false, false);
        assertToArray2D(MatrixType.MT_2D_3x3, null, true, false);
        assertToArray2D(MatrixType.MT_2D_3x3, new double[8], true, false);
        assertToArray2D(MatrixType.MT_2D_3x3, new double[9], true, true);

        assertToArray3D(MatrixType.MT_3D_3x4, null, false, false);
        assertToArray3D(MatrixType.MT_3D_3x4, null, true, false);
        assertToArray3D(MatrixType.MT_3D_3x4, new double[11], true, false);
        assertToArray3D(MatrixType.MT_3D_3x4, new double[12], true, true);

        assertToArray3D(MatrixType.MT_3D_4x4, null, false, false);
        assertToArray3D(MatrixType.MT_3D_4x4, null, true, false);
        assertToArray3D(MatrixType.MT_3D_4x4, new double[15], true, false);
        assertToArray3D(MatrixType.MT_3D_4x4, new double[16], true, true);
    }

    @Test(expected=NullPointerException.class)
    public void testToArrayNullType1() {
        t.toArray(null);
    }

    @Test(expected=NullPointerException.class)
    public void testToArrayNullType2() {
        t.toArray(null, new double[] {});
    }

    private void assertRow(MatrixType type, int row, double[] a, Transform t) {
        switch (type) {
            case MT_2D_3x3:
                if (row == 2) {
                    assertEquals(0, a[0], 1e-100);
                    assertEquals(0, a[1], 1e-100);
                    assertEquals(1, a[2], 1e-100);
                    return;
                }
                // fallthrough
            case MT_2D_2x3:
                if (row == 0) {
                    assertEquals(t.getMxx(), a[0], 1e-100);
                    assertEquals(t.getMxy(), a[1], 1e-100);
                    assertEquals(t.getTx(),  a[2], 1e-100);
                    return;
                } else if (row == 1) {
                    assertEquals(t.getMyx(), a[0], 1e-100);
                    assertEquals(t.getMyy(), a[1], 1e-100);
                    assertEquals(t.getTy(),  a[2], 1e-100);
                    return;
                }
                break;
            case MT_3D_4x4:
                if (row == 3) {
                    assertEquals(0, a[0], 1e-100);
                    assertEquals(0, a[1], 1e-100);
                    assertEquals(0, a[2], 1e-100);
                    assertEquals(1, a[3], 1e-100);
                    return;
                }
                // fallthrough
            case MT_3D_3x4:
                if (row == 0) {
                    assertEquals(t.getMxx(), a[0], 1e-100);
                    assertEquals(t.getMxy(), a[1], 1e-100);
                    assertEquals(t.getMxz(), a[2], 1e-100);
                    assertEquals(t.getTx(),  a[3], 1e-100);
                    return;
                } else if (row == 1) {
                    assertEquals(t.getMyx(), a[0], 1e-100);
                    assertEquals(t.getMyy(), a[1], 1e-100);
                    assertEquals(t.getMyz(), a[2], 1e-100);
                    assertEquals(t.getTy(),  a[3], 1e-100);
                    return;
                } else if (row == 2) {
                    assertEquals(t.getMzx(), a[0], 1e-100);
                    assertEquals(t.getMzy(), a[1], 1e-100);
                    assertEquals(t.getMzz(), a[2], 1e-100);
                    assertEquals(t.getTz(),  a[3], 1e-100);
                    return;
                }
                break;
        }

        fail("Should have thrown IOB");
    }

    private void assertRow2D(MatrixType type, int row, double[] tmp,
            boolean shouldPass, boolean shouldUse, boolean iob) {
        double[] a = null;
        try {
            if (shouldPass) {
                a = t.row(type, row, tmp);
            } else {
                a = t.row(type, row);
            }
        } catch (IllegalArgumentException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
            return;
        } catch (IndexOutOfBoundsException e) {
            if (!iob) {
                fail("Wrong exception thrown");
            }
            return;
        }
        if (!is2d) {
            fail("Should have thrown IAE");
        } else if (iob) {
            fail("Should have thrown IOB");
        } else {
            assertNotNull(a);
            if (shouldUse) {
                assertSame(tmp, a);
            }
            assertRow(type, row, a, t);
        }
    }

    private void assertRow3D(MatrixType type, int row, double[] tmp,
            boolean shouldPass, boolean shouldUse, boolean iob) {
        double[] a = null;
        try {
            if (shouldPass) {
                a = t.row(type, row, tmp);
            } else {
                a = t.row(type, row);
            }
        } catch (IndexOutOfBoundsException e) {
            if (!iob) {
                fail("Wrong exception thrown");
            }
            return;
        }
        if (iob) {
            fail("Should have thrown IOB");
        } else {
            assertNotNull(a);
            if (shouldUse) {
                assertSame(tmp, a);
            }
            assertRow(type, row, a, t);
        }
    }

    @Test
    public void testRow() {
        assertRow2D(MatrixType.MT_2D_2x3, 0, null, false, false, false);
        assertRow2D(MatrixType.MT_2D_2x3, 0, null, true, false, false);
        assertRow2D(MatrixType.MT_2D_2x3, 0, new double[2], true, false, false);
        assertRow2D(MatrixType.MT_2D_2x3, 0, new double[3], true, true, false);
        assertRow2D(MatrixType.MT_2D_2x3, 1, null, false, false, false);
        assertRow2D(MatrixType.MT_2D_2x3, 1, null, true, false, false);
        assertRow2D(MatrixType.MT_2D_2x3, 1, new double[2], true, false, false);
        assertRow2D(MatrixType.MT_2D_2x3, 1, new double[3], true, true, false);
        assertRow2D(MatrixType.MT_2D_2x3, -1, null, true, false, true);
        assertRow2D(MatrixType.MT_2D_2x3, 2, null, false, false, true);

        assertRow2D(MatrixType.MT_2D_3x3, 0, null, false, false, false);
        assertRow2D(MatrixType.MT_2D_3x3, 0, null, true, false, false);
        assertRow2D(MatrixType.MT_2D_3x3, 0, new double[2], true, false, false);
        assertRow2D(MatrixType.MT_2D_3x3, 0, new double[3], true, true, false);
        assertRow2D(MatrixType.MT_2D_3x3, 1, null, false, false, false);
        assertRow2D(MatrixType.MT_2D_3x3, 1, null, true, false, false);
        assertRow2D(MatrixType.MT_2D_3x3, 1, new double[2], true, false, false);
        assertRow2D(MatrixType.MT_2D_3x3, 1, new double[3], true, true, false);
        assertRow2D(MatrixType.MT_2D_3x3, 2, null, false, false, false);
        assertRow2D(MatrixType.MT_2D_3x3, 2, null, true, false, false);
        assertRow2D(MatrixType.MT_2D_3x3, 2, new double[2], true, false, false);
        assertRow2D(MatrixType.MT_2D_3x3, 2, new double[3], true, true, false);
        assertRow2D(MatrixType.MT_2D_3x3, -1, null, true, false, true);
        assertRow2D(MatrixType.MT_2D_3x3, 3, null, false, false, true);

        assertRow3D(MatrixType.MT_3D_3x4, 0, null, false, false, false);
        assertRow3D(MatrixType.MT_3D_3x4, 0, null, true, false, false);
        assertRow3D(MatrixType.MT_3D_3x4, 0, new double[3], true, false, false);
        assertRow3D(MatrixType.MT_3D_3x4, 0, new double[4], true, true, false);
        assertRow3D(MatrixType.MT_3D_3x4, 1, null, false, false, false);
        assertRow3D(MatrixType.MT_3D_3x4, 1, null, true, false, false);
        assertRow3D(MatrixType.MT_3D_3x4, 1, new double[3], true, false, false);
        assertRow3D(MatrixType.MT_3D_3x4, 1, new double[4], true, true, false);
        assertRow3D(MatrixType.MT_3D_3x4, 2, null, false, false, false);
        assertRow3D(MatrixType.MT_3D_3x4, 2, null, true, false, false);
        assertRow3D(MatrixType.MT_3D_3x4, 2, new double[3], true, false, false);
        assertRow3D(MatrixType.MT_3D_3x4, 2, new double[4], true, true, false);
        assertRow3D(MatrixType.MT_3D_3x4, -1, null, true, false, true);
        assertRow3D(MatrixType.MT_3D_3x4, 3, null, false, false, true);

        assertRow3D(MatrixType.MT_3D_4x4, 0, null, false, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 0, null, true, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 0, new double[3], true, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 0, new double[4], true, true, false);
        assertRow3D(MatrixType.MT_3D_4x4, 1, null, false, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 1, null, true, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 1, new double[3], true, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 1, new double[4], true, true, false);
        assertRow3D(MatrixType.MT_3D_4x4, 2, null, false, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 2, null, true, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 2, new double[3], true, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 2, new double[4], true, true, false);
        assertRow3D(MatrixType.MT_3D_4x4, 3, null, false, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 3, null, true, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 3, new double[3], true, false, false);
        assertRow3D(MatrixType.MT_3D_4x4, 3, new double[4], true, true, false);
        assertRow3D(MatrixType.MT_3D_4x4, -1, null, true, false, true);
        assertRow3D(MatrixType.MT_3D_4x4, 4, null, false, false, true);
    }

    @Test(expected=NullPointerException.class)
    public void testRowNullType1() {
        t.row(null, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testRowNullType2() {
        t.row(null, 0, new double[] {});
    }

    private void assertCol(MatrixType type, int col, double[] a, Transform t) {
        switch (type) {
            case MT_2D_2x3:
                if (col == 0) {
                    assertEquals(t.getMxx(), a[0], 1e-100);
                    assertEquals(t.getMyx(), a[1], 1e-100);
                    return;
                } else if (col == 1) {
                    assertEquals(t.getMxy(), a[0], 1e-100);
                    assertEquals(t.getMyy(), a[1], 1e-100);
                    return;
                } else if (col == 2) {
                    assertEquals(t.getTx(), a[0], 1e-100);
                    assertEquals(t.getTy(), a[1], 1e-100);
                    return;
                }
                break;
            case MT_2D_3x3:
                if (col == 0) {
                    assertEquals(t.getMxx(), a[0], 1e-100);
                    assertEquals(t.getMyx(), a[1], 1e-100);
                    assertEquals(0, a[2], 1e-100);
                    return;
                } else if (col == 1) {
                    assertEquals(t.getMxy(), a[0], 1e-100);
                    assertEquals(t.getMyy(), a[1], 1e-100);
                    assertEquals(0, a[2], 1e-100);
                    return;
                } else if (col == 2) {
                    assertEquals(t.getTx(), a[0], 1e-100);
                    assertEquals(t.getTy(), a[1], 1e-100);
                    assertEquals(1, a[2], 1e-100);
                    return;
                }
                break;
            case MT_3D_3x4:
                if (col == 0) {
                    assertEquals(t.getMxx(), a[0], 1e-100);
                    assertEquals(t.getMyx(), a[1], 1e-100);
                    assertEquals(t.getMzx(), a[2], 1e-100);
                    return;
                } else if (col == 1) {
                    assertEquals(t.getMxy(), a[0], 1e-100);
                    assertEquals(t.getMyy(), a[1], 1e-100);
                    assertEquals(t.getMzy(), a[2], 1e-100);
                    return;
                } else if (col == 2) {
                    assertEquals(t.getMxz(), a[0], 1e-100);
                    assertEquals(t.getMyz(), a[1], 1e-100);
                    assertEquals(t.getMzz(), a[2], 1e-100);
                    return;
                } else if (col == 3) {
                    assertEquals(t.getTx(), a[0], 1e-100);
                    assertEquals(t.getTy(), a[1], 1e-100);
                    assertEquals(t.getTz(), a[2], 1e-100);
                    return;
                }
                break;
            case MT_3D_4x4:
                if (col == 0) {
                    assertEquals(t.getMxx(), a[0], 1e-100);
                    assertEquals(t.getMyx(), a[1], 1e-100);
                    assertEquals(t.getMzx(), a[2], 1e-100);
                    assertEquals(0, a[3], 1e-100);
                    return;
                } else if (col == 1) {
                    assertEquals(t.getMxy(), a[0], 1e-100);
                    assertEquals(t.getMyy(), a[1], 1e-100);
                    assertEquals(t.getMzy(), a[2], 1e-100);
                    assertEquals(0, a[3], 1e-100);
                    return;
                } else if (col == 2) {
                    assertEquals(t.getMxz(), a[0], 1e-100);
                    assertEquals(t.getMyz(), a[1], 1e-100);
                    assertEquals(t.getMzz(), a[2], 1e-100);
                    assertEquals(0, a[3], 1e-100);
                    return;
                } else if (col == 3) {
                    assertEquals(t.getTx(), a[0], 1e-100);
                    assertEquals(t.getTy(), a[1], 1e-100);
                    assertEquals(t.getTz(), a[2], 1e-100);
                    assertEquals(1, a[3], 1e-100);
                    return;
                }
                break;
        }

        fail("Should have thrown IOB");
    }

    private void assertCol2D(MatrixType type, int col, double[] tmp,
            boolean shouldPass, boolean shouldUse, boolean iob) {
        double[] a = null;
        try {
            if (shouldPass) {
                a = t.column(type, col, tmp);
            } else {
                a = t.column(type, col);
            }
        } catch (IllegalArgumentException e) {
            if (is2d) {
                fail("Wrong exception thrown");
            }
            return;
        } catch (IndexOutOfBoundsException e) {
            if (!iob) {
                fail("Wrong exception thrown");
            }
            return;
        }
        if (!is2d) {
            fail("Should have thrown IAE");
        } else if (iob) {
            fail("Should have thrown IOB");
        } else {
            assertNotNull(a);
            if (shouldUse) {
                assertSame(tmp, a);
            }
            assertCol(type, col, a, t);
        }
    }

    private void assertCol3D(MatrixType type, int col, double[] tmp,
            boolean shouldPass, boolean shouldUse, boolean iob) {
        double[] a = null;
        try {
            if (shouldPass) {
                a = t.column(type, col, tmp);
            } else {
                a = t.column(type, col);
            }
        } catch (IndexOutOfBoundsException e) {
            if (!iob) {
                fail("Wrong exception thrown");
            }
            return;
        }
        if (iob) {
            fail("Should have thrown IOB");
        } else {
            assertNotNull(a);
            if (shouldUse) {
                assertSame(tmp, a);
            }
            assertCol(type, col, a, t);
        }
    }

    @Test
    public void testColumn() {
        assertCol2D(MatrixType.MT_2D_2x3, 0, null, false, false, false);
        assertCol2D(MatrixType.MT_2D_2x3, 0, null, true, false, false);
        assertCol2D(MatrixType.MT_2D_2x3, 0, new double[1], true, false, false);
        assertCol2D(MatrixType.MT_2D_2x3, 0, new double[2], true, true, false);
        assertCol2D(MatrixType.MT_2D_2x3, 1, null, false, false, false);
        assertCol2D(MatrixType.MT_2D_2x3, 1, null, true, false, false);
        assertCol2D(MatrixType.MT_2D_2x3, 1, new double[1], true, false, false);
        assertCol2D(MatrixType.MT_2D_2x3, 1, new double[2], true, true, false);
        assertCol2D(MatrixType.MT_2D_2x3, 2, null, false, false, false);
        assertCol2D(MatrixType.MT_2D_2x3, 2, null, true, false, false);
        assertCol2D(MatrixType.MT_2D_2x3, 2, new double[1], true, false, false);
        assertCol2D(MatrixType.MT_2D_2x3, 2, new double[2], true, true, false);
        assertCol2D(MatrixType.MT_2D_2x3, -1, null, true, false, true);
        assertCol2D(MatrixType.MT_2D_2x3, 3, null, false, false, true);

        assertCol2D(MatrixType.MT_2D_3x3, 0, null, false, false, false);
        assertCol2D(MatrixType.MT_2D_3x3, 0, null, true, false, false);
        assertCol2D(MatrixType.MT_2D_3x3, 0, new double[2], true, false, false);
        assertCol2D(MatrixType.MT_2D_3x3, 0, new double[3], true, true, false);
        assertCol2D(MatrixType.MT_2D_3x3, 1, null, false, false, false);
        assertCol2D(MatrixType.MT_2D_3x3, 1, null, true, false, false);
        assertCol2D(MatrixType.MT_2D_3x3, 1, new double[2], true, false, false);
        assertCol2D(MatrixType.MT_2D_3x3, 1, new double[3], true, true, false);
        assertCol2D(MatrixType.MT_2D_3x3, 2, null, false, false, false);
        assertCol2D(MatrixType.MT_2D_3x3, 2, null, true, false, false);
        assertCol2D(MatrixType.MT_2D_3x3, 2, new double[2], true, false, false);
        assertCol2D(MatrixType.MT_2D_3x3, 2, new double[3], true, true, false);
        assertCol2D(MatrixType.MT_2D_3x3, -1, null, true, false, true);
        assertCol2D(MatrixType.MT_2D_3x3, 3, null, false, false, true);

        assertCol3D(MatrixType.MT_3D_3x4, 0, null, false, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 0, null, true, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 0, new double[2], true, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 0, new double[3], true, true, false);
        assertCol3D(MatrixType.MT_3D_3x4, 1, null, false, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 1, null, true, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 1, new double[2], true, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 1, new double[3], true, true, false);
        assertCol3D(MatrixType.MT_3D_3x4, 2, null, false, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 2, null, true, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 2, new double[2], true, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 2, new double[3], true, true, false);
        assertCol3D(MatrixType.MT_3D_3x4, 3, null, false, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 3, null, true, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 3, new double[2], true, false, false);
        assertCol3D(MatrixType.MT_3D_3x4, 3, new double[3], true, true, false);
        assertCol3D(MatrixType.MT_3D_3x4, -1, null, true, false, true);
        assertCol3D(MatrixType.MT_3D_3x4, 4, null, false, false, true);

        assertCol3D(MatrixType.MT_3D_4x4, 0, null, false, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 0, null, true, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 0, new double[3], true, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 0, new double[4], true, true, false);
        assertCol3D(MatrixType.MT_3D_4x4, 1, null, false, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 1, null, true, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 1, new double[3], true, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 1, new double[4], true, true, false);
        assertCol3D(MatrixType.MT_3D_4x4, 2, null, false, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 2, null, true, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 2, new double[3], true, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 2, new double[4], true, true, false);
        assertCol3D(MatrixType.MT_3D_4x4, 3, null, false, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 3, null, true, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 3, new double[3], true, false, false);
        assertCol3D(MatrixType.MT_3D_4x4, 3, new double[4], true, true, false);
        assertCol3D(MatrixType.MT_3D_4x4, -1, null, true, false, true);
        assertCol3D(MatrixType.MT_3D_4x4, 4, null, false, false, true);
    }

    @Test(expected=NullPointerException.class)
    public void testColumnNullType1() {
        t.column(null, 0);
    }

    @Test(expected=NullPointerException.class)
    public void testColumnNullType2() {
        t.column(null, 0, new double[] {});
    }

    @Test
    public void testSetOnTtransformChanged() {
        Transform clone = t.clone();

        EventHandler<TransformChangedEvent> ontc =
            new EventHandler<TransformChangedEvent>() {
                @Override public void handle(TransformChangedEvent event) {
                    eventCounter++;
                }
            };

        assertSame(null, clone.getOnTransformChanged());
        clone.setOnTransformChanged(ontc);
        assertSame(ontc, clone.getOnTransformChanged());

        eventCounter = 0;
        if (TransformHelper.modify(clone, 42)) {
            if (t == rotateZeroAxis || t == noRotate) {
                // needs two changes to be further usable
                eventCounter--;
            }
            assertEquals(1, eventCounter);
            TransformHelper.modify(clone, 42);
            assertEquals(1, eventCounter);
            TransformHelper.modify(clone, 43);
            assertEquals(2, eventCounter);

            clone.setOnTransformChanged(null);
            assertNull(clone.getOnTransformChanged());
            TransformHelper.modify(clone, 44);
            assertEquals(2, eventCounter);

        } else {
            assertEquals(0, eventCounter);
        }
    }

    @Test
    public void testAddRemoveEventHandler() {
        Transform clone = t.clone();

        EventHandler<TransformChangedEvent> counting =
            new EventHandler<TransformChangedEvent>() {
                @Override public void handle(TransformChangedEvent event) {
                    eventCounter++;
                }
            };

        EventHandler<TransformChangedEvent> checking =
            new EventHandler<TransformChangedEvent>() {
                @Override public void handle(TransformChangedEvent event) {
                    listenerCalled = true;
                }
            };

        clone.addEventHandler(TransformChangedEvent.TRANSFORM_CHANGED, counting);
        clone.addEventHandler(TransformChangedEvent.TRANSFORM_CHANGED, checking);

        eventCounter = 0;
        listenerCalled = false;
        if (TransformHelper.modify(clone, 42)) {
            if (t == rotateZeroAxis || t == noRotate) {
                // needs two changes to be further usable
                eventCounter--;
            }
            assertEquals(1, eventCounter);
            assertTrue(listenerCalled);

            listenerCalled = false;
            TransformHelper.modify(clone, 42);
            assertEquals(1, eventCounter);
            assertFalse(listenerCalled);

            clone.removeEventHandler(TransformChangedEvent.TRANSFORM_CHANGED, checking);

            TransformHelper.modify(clone, 43);
            assertEquals(2, eventCounter);
            assertFalse(listenerCalled);
        } else {
            assertEquals(0, eventCounter);
        }
    }

    @Test
    public void testAddRemoveEventFilter() {
        Transform clone = t.clone();

        EventHandler<TransformChangedEvent> counting =
            new EventHandler<TransformChangedEvent>() {
                @Override public void handle(TransformChangedEvent event) {
                    eventCounter++;
                    event.consume();
                }
            };

        EventHandler<TransformChangedEvent> checking =
            new EventHandler<TransformChangedEvent>() {
                @Override public void handle(TransformChangedEvent event) {
                    listenerCalled = true;
                }
            };

        clone.addEventFilter(TransformChangedEvent.TRANSFORM_CHANGED, counting);
        clone.addEventHandler(TransformChangedEvent.TRANSFORM_CHANGED, checking);

        eventCounter = 0;
        listenerCalled = false;
        if (TransformHelper.modify(clone, 42)) {
            if (t == rotateZeroAxis || t == noRotate) {
                // needs two changes to be further usable
                eventCounter--;
            }
            assertEquals(1, eventCounter);
            assertFalse(listenerCalled); // consumed by filter

            listenerCalled = false;
            TransformHelper.modify(clone, 42);
            assertEquals(1, eventCounter);
            assertFalse(listenerCalled);

            clone.removeEventFilter(TransformChangedEvent.TRANSFORM_CHANGED, counting);

            TransformHelper.modify(clone, 43);
            assertEquals(1, eventCounter);
            assertTrue(listenerCalled);
        } else {
            assertEquals(0, eventCounter);
        }
    }
}
