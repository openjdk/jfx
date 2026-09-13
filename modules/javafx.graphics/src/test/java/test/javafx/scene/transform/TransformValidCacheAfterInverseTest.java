/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Point2D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TransformValidCacheAfterInverseTest {
    private static final double EPSILON = 1e-6;

    private void checkTransform(Transform t) throws Exception {
        Point2D orig = new Point2D(1.3, 3.9);

        Point2D p = t.transform(orig);
        Point2D pi = t.inverseTransform(p);

        assertEquals(orig.getX(), pi.getX(), EPSILON);
        assertEquals(orig.getY(), pi.getY(), EPSILON);
    }

    @Test
    public void testTransformInverseCache_Affine() throws Exception {
        Affine affine = new Affine();

        affine.appendRotation(20);
        affine.appendScale(2.0, 1.3);
        checkTransform(affine);

        affine.appendRotation(40);
        affine.appendScale(2.6, 3.2);
        checkTransform(affine);
    }

    @Test
    public void testTransformInverseCache_Rotate() throws Exception {
        Rotate rotate = new Rotate();

        rotate.setAngle(20);
        checkTransform(rotate);

        rotate.setAngle(40);
        checkTransform(rotate);
    }

    @Test
    public void testTransformInverseCache_Scale() throws Exception {
        Scale scale = new Scale();

        scale.setX(2.0);
        scale.setY(3.3);
        checkTransform(scale);

        scale.setX(5.0);
        scale.setY(6.8);
        checkTransform(scale);
    }

    @Test
    public void testTransformInverseCache_Shear() throws Exception {
        Shear shear = new Shear();

        shear.setX(2.0);
        shear.setY(5.3);
        checkTransform(shear);

        shear.setX(1.2);
        shear.setY(4.3);
        checkTransform(shear);
    }

    @Test
    public void testTransformInverseCache_Translate() throws Exception {
        Translate translate = new Translate();

        translate.setX(1.4);
        translate.setY(3.5);
        checkTransform(translate);

        translate.setX(0.3);
        translate.setY(20.1);
        checkTransform(translate);
    }
}
