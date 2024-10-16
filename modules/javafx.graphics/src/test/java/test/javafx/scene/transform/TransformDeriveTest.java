/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.Identity;
import com.sun.javafx.geom.transform.Translate2D;
import java.util.stream.Stream;
import test.com.sun.javafx.test.TransformHelper;
import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TransformDeriveTest {

    private static final Identity identity = new Identity();
    private static final Translate2D translate2d = new Translate2D(10, 20);
    private static final Affine2D affine2d = new Affine2D();
    static { affine2d.scale(2, 4); }
    private static final Affine3D affine3d = new Affine3D();
    static { affine3d.scale(2, 4, 8); }

    private static final Translate translate_identity = new Translate();
    private static final Translate translate_translate2d = new Translate(10, 20);
    private static final Translate translate_translate3d = new Translate(10, 20, 30);

    private static final Scale scale_identity = new Scale();
    private static final Scale scale_pivotedidentity = new Scale(1, 1, 20, 30);
    private static final Scale scale_scale2d = new Scale(2, 4);
    private static final Scale scale_pivotedscale2d = new Scale(2, 4, 20, 40);
    private static final Scale scale_scale3d = new Scale(2, 4, 8);
    private static final Scale scale_pivotedscale3d = new Scale(2, 4, 8, 20, 40, 60);

    private static final Rotate rotate_identity = new Rotate();
    private static final Rotate rotate_pivotedidentity = new Rotate(0.0, 10, 20, 30, Rotate.X_AXIS);
    private static final Rotate rotate_rotate2d = new Rotate(90);
    private static final Rotate rotate_negative2d = new Rotate(90, 0, 0, 0, new Point3D(0, 0, -1));
    private static final Rotate rotate_pivotedrotate2d = new Rotate(90, 20, 30);
    private static final Rotate rotate_rotate3d = new Rotate(90, 0, 0, 0, Rotate.X_AXIS);
    private static final Rotate rotate_pivotedrotate3d = new Rotate(90, 20, 30, 0, Rotate.X_AXIS);

    private static final Shear shear_identity = new Shear();
    private static final Shear shear_shear = new Shear(2, 3);

    private static final Affine affine_identity = new Affine();
    private static final Affine affine_translate2d = new Affine();
    static { affine_translate2d.appendTranslation(10, 20); }
    private static final Affine affine_translate3d = new Affine();
    static { affine_translate3d.appendTranslation(10, 20, 30); }
    private static final Affine affine_scale2d = new Affine();
    static { affine_scale2d.appendScale(2, 4); }
    private static final Affine affine_scale3d = new Affine();
    static { affine_scale3d.appendScale(2, 4, 8); }
    private static final Affine affine_affine2d = new Affine();
    static { affine_affine2d.appendRotation(45); }
    private static final Affine affine_affine3d = new Affine();
    static { affine_affine3d.appendRotation(45, 10, 20, 30, 1, 1, 1); }

    public static Stream<Arguments> getParams() {
        return Stream.of(
            Arguments.of( identity, translate_identity, Identity.class ),           //  0
            Arguments.of( identity, translate_translate2d, Translate2D.class ),
            Arguments.of( identity, translate_translate3d, Affine3D.class ),
            Arguments.of( identity, scale_identity, Identity.class ),
            Arguments.of( identity, scale_pivotedidentity, Identity.class ),
            Arguments.of( identity, scale_scale2d, Affine2D.class ),
            Arguments.of( identity, scale_pivotedscale2d, Affine2D.class ),
            Arguments.of( identity, scale_scale3d, Affine3D.class ),
            Arguments.of( identity, scale_pivotedscale3d, Affine3D.class ),
            Arguments.of( identity, rotate_identity, Identity.class ),
            Arguments.of( identity, rotate_pivotedidentity, Identity.class ),       // 10
            Arguments.of( identity, rotate_rotate2d, Affine2D.class ),
            Arguments.of( identity, rotate_negative2d, Affine2D.class ),
            Arguments.of( identity, rotate_pivotedrotate2d, Affine2D.class ),
            Arguments.of( identity, rotate_rotate3d, Affine3D.class ),
            Arguments.of( identity, rotate_pivotedrotate3d, Affine3D.class ),
            Arguments.of( identity, shear_identity, Identity.class ),
            Arguments.of( identity, shear_shear, Affine2D.class ),
            Arguments.of( identity, affine_identity, Identity.class ),
            Arguments.of( identity, affine_translate2d, Translate2D.class ),
            Arguments.of( identity, affine_translate3d, Affine3D.class ),           // 20
            Arguments.of( identity, affine_scale2d, Affine2D.class ),
            Arguments.of( identity, affine_scale3d, Affine3D.class ),
            Arguments.of( identity, affine_affine2d, Affine2D.class ),
            Arguments.of( identity, affine_affine3d, Affine3D.class ),

            Arguments.of( translate2d, translate_identity, Translate2D.class ),
            Arguments.of( translate2d, translate_translate2d, Translate2D.class ),
            Arguments.of( translate2d, translate_translate3d, Affine3D.class ),
            Arguments.of( translate2d, scale_identity, Translate2D.class ),
            Arguments.of( translate2d, scale_pivotedidentity, Translate2D.class ),
            Arguments.of( translate2d, scale_scale2d, Affine2D.class ),             // 30
            Arguments.of( translate2d, scale_pivotedscale2d, Affine2D.class ),
            Arguments.of( translate2d, scale_scale3d, Affine3D.class ),
            Arguments.of( translate2d, scale_pivotedscale3d, Affine3D.class ),
            Arguments.of( translate2d, rotate_identity, Translate2D.class ),
            Arguments.of( translate2d, rotate_pivotedidentity, Translate2D.class ),
            Arguments.of( translate2d, rotate_rotate2d, Affine2D.class ),
            Arguments.of( translate2d, rotate_negative2d, Affine2D.class ),
            Arguments.of( translate2d, rotate_pivotedrotate2d, Affine2D.class ),
            Arguments.of( translate2d, rotate_rotate3d, Affine3D.class ),
            Arguments.of( translate2d, rotate_pivotedrotate3d, Affine3D.class ),    // 40
            Arguments.of( translate2d, shear_identity, Translate2D.class ),
            Arguments.of( translate2d, shear_shear, Affine2D.class ),
            Arguments.of( translate2d, affine_identity, Translate2D.class ),
            Arguments.of( translate2d, affine_translate2d, Translate2D.class ),
            Arguments.of( translate2d, affine_translate3d, Affine3D.class ),
            Arguments.of( translate2d, affine_scale2d, Affine2D.class ),
            Arguments.of( translate2d, affine_scale3d, Affine3D.class ),
            Arguments.of( translate2d, affine_affine2d, Affine2D.class ),
            Arguments.of( translate2d, affine_affine3d, Affine3D.class ),

            Arguments.of( affine2d, translate_identity, Affine2D.class ),           // 50
            Arguments.of( affine2d, translate_translate2d, Affine2D.class ),
            Arguments.of( affine2d, translate_translate3d, Affine3D.class ),
            Arguments.of( affine2d, scale_identity, Affine2D.class ),
            Arguments.of( affine2d, scale_pivotedidentity, Affine2D.class ),
            Arguments.of( affine2d, scale_scale2d, Affine2D.class ),
            Arguments.of( affine2d, scale_pivotedscale2d, Affine2D.class ),
            Arguments.of( affine2d, scale_scale3d, Affine3D.class ),
            Arguments.of( affine2d, scale_pivotedscale3d, Affine3D.class ),
            Arguments.of( affine2d, rotate_identity, Affine2D.class ),
            Arguments.of( affine2d, rotate_pivotedidentity, Affine2D.class ),       // 60
            Arguments.of( affine2d, rotate_rotate2d, Affine2D.class ),
            Arguments.of( affine2d, rotate_negative2d, Affine2D.class ),
            Arguments.of( affine2d, rotate_pivotedrotate2d, Affine2D.class ),
            Arguments.of( affine2d, rotate_rotate3d, Affine3D.class ),
            Arguments.of( affine2d, rotate_pivotedrotate3d, Affine3D.class ),
            Arguments.of( affine2d, shear_identity, Affine2D.class ),
            Arguments.of( affine2d, shear_shear, Affine2D.class ),
            Arguments.of( affine2d, affine_identity, Affine2D.class ),
            Arguments.of( affine2d, affine_translate2d, Affine2D.class ),
            Arguments.of( affine2d, affine_translate3d, Affine3D.class ),           // 70
            Arguments.of( affine2d, affine_scale2d, Affine2D.class ),
            Arguments.of( affine2d, affine_scale3d, Affine3D.class ),
            Arguments.of( affine2d, affine_affine2d, Affine2D.class ),
            Arguments.of( affine2d, affine_affine3d, Affine3D.class ),

            Arguments.of( affine3d, translate_identity, Affine3D.class ),
            Arguments.of( affine3d, translate_translate2d, Affine3D.class ),
            Arguments.of( affine3d, translate_translate3d, Affine3D.class ),
            Arguments.of( affine3d, scale_identity, Affine3D.class ),
            Arguments.of( affine3d, scale_pivotedidentity, Affine3D.class ),
            Arguments.of( affine3d, scale_scale2d, Affine3D.class ),                // 80
            Arguments.of( affine3d, scale_pivotedscale2d, Affine3D.class ),
            Arguments.of( affine3d, scale_scale3d, Affine3D.class ),
            Arguments.of( affine3d, scale_pivotedscale3d, Affine3D.class ),
            Arguments.of( affine3d, rotate_identity, Affine3D.class ),
            Arguments.of( affine3d, rotate_pivotedidentity, Affine3D.class ),
            Arguments.of( affine3d, rotate_rotate2d, Affine3D.class ),
            Arguments.of( affine3d, rotate_negative2d, Affine3D.class ),
            Arguments.of( affine3d, rotate_pivotedrotate2d, Affine3D.class ),
            Arguments.of( affine3d, rotate_rotate3d, Affine3D.class ),
            Arguments.of( affine3d, rotate_pivotedrotate3d, Affine3D.class ),       // 90
            Arguments.of( affine3d, shear_identity, Affine3D.class ),
            Arguments.of( affine3d, shear_shear, Affine3D.class ),
            Arguments.of( affine3d, affine_identity, Affine3D.class ),
            Arguments.of( affine3d, affine_translate2d, Affine3D.class ),
            Arguments.of( affine3d, affine_translate3d, Affine3D.class ),
            Arguments.of( affine3d, affine_scale2d, Affine3D.class ),
            Arguments.of( affine3d, affine_scale3d, Affine3D.class ),
            Arguments.of( affine3d, affine_affine2d, Affine3D.class ),
            Arguments.of( affine3d, affine_affine3d, Affine3D.class )              // 99
        );
    }

    @ParameterizedTest
    @MethodSource("getParams")
    public void testDerive(BaseTransform f, Transform deriver, Class deriveType) {
        BaseTransform from = f.copy();
        Transform conc = TransformHelper.concatenate(from, deriver);

        BaseTransform res = com.sun.javafx.scene.transform.TransformHelper.derive(deriver, from);

        assertSame(deriveType, res.getClass());
        TransformHelper.assertMatrix(res,
                conc.getMxx(), conc.getMxy(), conc.getMxz(), conc.getTx(),
                conc.getMyx(), conc.getMyy(), conc.getMyz(), conc.getTy(),
                conc.getMzx(), conc.getMzy(), conc.getMzz(), conc.getTz());
   }
}
