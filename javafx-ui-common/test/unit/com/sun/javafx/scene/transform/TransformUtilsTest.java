/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.transform;

import com.sun.javafx.test.TransformHelper;
import javafx.scene.transform.Transform;
import com.sun.javafx.geom.transform.Affine3D;
import static org.junit.Assert.*;

import org.junit.Test;

public class TransformUtilsTest {
    @Test
    public void shouldCreateCorrectImmutableTransform() {
        Transform t = TransformUtils.immutableTransform(
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        TransformHelper.assertMatrix(t,
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);
    }

    @Test
    public void immutableTransformShouldApplyCorrectly() {
        Affine3D a = new Affine3D();
        a.translate(10, 20);

        Transform t = TransformUtils.immutableTransform(
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        t.impl_apply(a);

        TransformHelper.assertMatrix(a,
                1,  2,  3, 14,
                5,  6,  7, 28,
                9, 10, 11, 12);
    }

    @Test
    public void immutableTransformShouldCopyCorrectly() {
        Transform src = TransformUtils.immutableTransform(
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        Transform t = src.clone();

        TransformHelper.assertMatrix(t,
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);
    }

    @Test public void testImmutableTransformToString() {
        final Transform trans = TransformUtils.immutableTransform(
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);
        
        String s = trans.toString();

        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

}
