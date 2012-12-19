/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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


import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * This Unit Test covers BaseTransform and some of its calculations.
 * TODO: cover more of BaseTransform's functionality. (RT-26882)
 */
public class BaseTransformTest {

    public void assertEqual(BaseTransform src1, BaseTransform src2) {
        assertEquals(src1.getMxx(), src2.getMxx(), 0.001);
        assertEquals(src1.getMxy(), src2.getMxy(), 0.001);
        assertEquals(src1.getMxz(), src2.getMxz(), 0.001);
        assertEquals(src1.getMyx(), src2.getMyx(), 0.001);
        assertEquals(src1.getMyy(), src2.getMyy(), 0.001);
        assertEquals(src1.getMyz(), src2.getMyz(), 0.001);
        assertEquals(src1.getMzx(), src2.getMzx(), 0.001);
        assertEquals(src1.getMzy(), src2.getMzy(), 0.001);
        assertEquals(src1.getMzz(), src2.getMzz(), 0.001);
        assertEquals(src1.getMxt(), src2.getMxt(), 0.001);
        assertEquals(src1.getMyt(), src2.getMyt(), 0.001);
        assertEquals(src1.getMzt(), src2.getMzt(), 0.001);
    }

    BaseTransform getTest3DTransform() {
        return new Affine3D(10, 0, 0, 2,
                            0, 10, 0, 2,
                            0, 0, 10, 2);
    }

    BaseTransform getTest2DTransform() {
        return new Affine2D(2, 0, 0, 2, 5, 5);
    }

    Translate2D getTestTranslateTransform() {
        return new Translate2D(8,8);
    }

    @Test
    public void testCompare() {
        BaseTransform test1 = new Identity();
        BaseTransform test2 = new Identity();
        assertEqual(test1, test2);
    }

    @Test
    public void testConcatenation() {
        BaseTransform test1 = new Identity();
        BaseTransform test2 = getTest3DTransform();
        BaseTransform result1 = test1.deriveWithConcatenation(test2);
        assertEqual(result1, getTest3DTransform());
    }

    @Test
    public void testDeriveWithPreConcatenation() {
        BaseTransform test1 = new Identity();
        test1.deriveWithPreConcatenation(test1);
        assertEqual(test1, BaseTransform.IDENTITY_TRANSFORM);
    }

    @Test
    public void testAffine3DPreConcatenation() {
        Affine3D test1 = (Affine3D)getTest3DTransform();
        BaseTransform test2 = getTest2DTransform();
        BaseTransform result1 = test1.deriveWithPreConcatenation(test2);
        test1 = (Affine3D)getTest3DTransform();
        test2 = getTest2DTransform();
        BaseTransform result2 = test2.deriveWithConcatenation(test1);
        assertEqual(result1, result2);
    }

    @Test
    public void testTranslatePreConcatenation() {
        Affine3D test1 = (Affine3D)getTest3DTransform();
        BaseTransform test2 = getTestTranslateTransform();
        BaseTransform result1 = test1.deriveWithPreConcatenation(test2);
        test1 = (Affine3D)getTest3DTransform();
        test2 = getTestTranslateTransform();
        BaseTransform result2 = test2.deriveWithConcatenation(test1);
        assertEqual(result1, result2);
    }

}
