/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.transform;

import test.com.sun.javafx.test.TransformHelper;
import javafx.scene.transform.Transform;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.scene.transform.TransformUtils;
import com.sun.javafx.scene.transform.TransformUtilsShim;
import java.util.LinkedList;
import java.util.List;
import test.javafx.scene.transform.TransformOperationsTest;
import javafx.scene.transform.Translate;
import static org.junit.Assert.*;

import org.junit.Test;

public class TransformUtilsTest {
    @Test
    public void shouldCreateCorrectImmutableTransform() {
        Transform t = TransformUtilsShim.getImmutableTransform(
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

        Transform t = TransformUtilsShim.getImmutableTransform(
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
        Transform src = TransformUtilsShim.getImmutableTransform(
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
        Transform trans = TransformUtilsShim.getImmutableTransform(
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        String s = trans.toString();

        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test public void testImmutableTransformState() {
        int counter = 0;
        for (Object o : TransformOperationsTest.getParams()) {
            Object[] arr = (Object[]) o;
            if (arr[0] instanceof TransformUtilsShim.ImmutableTransformShim) {
                TransformUtilsShim.ImmutableTransformShim t =
                        (TransformUtilsShim.ImmutableTransformShim) arr[0];

                TransformHelper.assertStateOk("Checking state of transform #" +
                        (counter++) + " of TransformOperationsTest", t,
                        TransformUtilsShim.getImmutableState3d(t),
                        TransformUtilsShim.getImmutableState2d(t));
            }
        }
    }

    @Test public void testReusedImmutableTransform() {
        int counter = 0;
        for (Object o : TransformOperationsTest.getParams()) {
            Object[] arr = (Object[]) o;
            if (arr[0] instanceof TransformUtilsShim.ImmutableTransformShim) {

                Transform t = (Transform) arr[0];


                // reusing
                Transform reuse = TransformUtils.immutableTransform(
                        new Translate(10, 20));

                Transform returned = TransformUtils.immutableTransform(reuse, t);

                assertSame("Checking reusing immutable transform to values of #"
                        + counter + " of TransformOperationsTest", reuse, returned);

                TransformHelper.assertStateOk(
                        "Checking reusing immutable transform to values of #"
                        + counter + " of TransformOperationsTest",
                        returned,
                        (TransformUtilsShim.getImmutableState3d(returned)),
                        (TransformUtilsShim.getImmutableState2d(returned)));

                TransformHelper.assertMatrix("Checking reusing immutable "
                        + "transform to values of #" + counter
                        + " of TransformOperationsTest", returned, t);

                // creating new
                Transform returned2 = TransformUtils.immutableTransform(null, t);

                assertNotSame("Checking reusing immutable transform to values of #"
                        + counter + " of TransformOperationsTest", returned2, t);

                TransformHelper.assertStateOk(
                        "Checking reusing immutable transform to values of #"
                        + counter + " of TransformOperationsTest",
                        returned2,
                        TransformUtilsShim.getImmutableState3d(returned),
                        TransformUtilsShim.getImmutableState2d(returned));

                TransformHelper.assertMatrix("Checking reusing immutable "
                        + "transform to values of #" + counter
                        + " of TransformOperationsTest", returned2, t);

                counter++;
            }
        }
    }

    @Test public void testConcatenatedImmutableTransform() {

        List<TransformUtilsShim.ImmutableTransformShim> ts = new LinkedList<>();
        for (Object o : TransformOperationsTest.getParams()) {
            Object[] arr = (Object[]) o;
            if (arr[0] instanceof TransformUtilsShim.ImmutableTransformShim) {
                ts.add((TransformUtilsShim.ImmutableTransformShim) arr[0]);
            }
        }

        int outer = 0;
        for (TransformUtilsShim.ImmutableTransformShim t1 : ts) {
            int inner = 0;
            for (TransformUtilsShim.ImmutableTransformShim t2 : ts) {
                int orig = 0;
                // reusing
                for (TransformUtilsShim.ImmutableTransformShim t3 : ts) {
                    Transform clone = t3.clone();
                    Transform conc = TransformUtils.immutableTransform(
                            clone, t1, t2);

                    assertSame("Checking state of concatenation of "
                            + "transform #" + outer + " and #" + inner +
                            " reusing #" + orig +
                            " of TransformOperationsTest", clone, conc);
                    TransformHelper.assertStateOk(
                            "Checking state of concatenation of "
                            + "transform #" + outer + " and #" + inner +
                            " reusing #" + orig +
                            " of TransformOperationsTest",
                            (TransformUtilsShim.ImmutableTransformShim) conc,
                                TransformUtilsShim.getImmutableState3d(conc),
                                TransformUtilsShim.getImmutableState2d(conc));
                    TransformHelper.assertMatrix(
                            "Checking state of concatenation of "
                            + "transform #" + outer + " and #" + inner +
                            " reusing #" + orig +
                            " of TransformOperationsTest", conc,
                            TransformHelper.concatenate(t1, t2));
                    orig++;
                }

                // creating new
                Transform conc2 = TransformUtils.immutableTransform(
                        null, t1, t2);

                assertNotSame("Checking state of concatenation of "
                        + "transform #" + outer + " and #" + inner +
                        " of TransformOperationsTest", conc2, t1);
                assertNotSame("Checking state of concatenation of "
                        + "transform #" + outer + " and #" + inner +
                        " of TransformOperationsTest", conc2, t2);
                TransformHelper.assertStateOk(
                        "Checking state of concatenation of "
                        + "transform #" + outer + " and #" + inner +
                        " of TransformOperationsTest",
                        (TransformUtilsShim.ImmutableTransformShim) conc2,
                        TransformUtilsShim.getImmutableState3d(conc2),
                        TransformUtilsShim.getImmutableState2d(conc2));
                TransformHelper.assertMatrix(
                        "Checking state of concatenation of "
                        + "transform #" + outer + " and #" + inner +
                        " of TransformOperationsTest", conc2,
                        TransformHelper.concatenate(t1, t2));
                inner++;
            }
            outer++;
        }
    }
}
