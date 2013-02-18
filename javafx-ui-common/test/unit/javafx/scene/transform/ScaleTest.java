/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.transform.TransformTest.assertTx;
import javafx.scene.shape.Rectangle;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;
import com.sun.javafx.test.TransformHelper;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;

public class ScaleTest {

    @Test
    public void testScale() {
        final Scale trans = new Scale() {{
            setX(25);
            setY(52);
        }};
        final Rectangle n = new Rectangle();
        n.getTransforms().add(trans);

        assertTx(n, BaseTransform.getScaleInstance(25, 52));
        TransformHelper.assertMatrix(trans,
                25,  0, 0, 0,
                 0, 52, 0, 0,
                 0,  0, 1, 0);


        trans.setX(34);
        Assert.assertEquals(34, trans.getX(), 1e-100);
        assertTx(n, BaseTransform.getScaleInstance(34, 52));
        TransformHelper.assertMatrix(trans,
                34,  0, 0, 0,
                 0, 52, 0, 0,
                 0,  0, 1, 0);


        trans.setY(67);
        assertTx(n, BaseTransform.getScaleInstance(34, 67));
        TransformHelper.assertMatrix(trans,
                34,  0, 0, 0,
                 0, 67, 0, 0,
                 0,  0, 1, 0);


        trans.setPivotX(66);

        Affine2D expTx = new Affine2D();
        expTx.setToTranslation(trans.getPivotX(), trans.getPivotY());
        expTx.scale(trans.getX(), trans.getY());
        expTx.translate(-trans.getPivotX(), -trans.getPivotY());
        assertTx(n, expTx);
        TransformHelper.assertMatrix(trans,
                34,  0, 0, -2178,
                 0, 67, 0,     0,
                 0,  0, 1,     0);


        trans.setPivotY(77);

        expTx.setToTranslation(trans.getPivotX(), trans.getPivotY());
        expTx.scale(trans.getX(), trans.getY());
        expTx.translate(-trans.getPivotX(), -trans.getPivotY());
        assertTx(n, expTx);
        TransformHelper.assertMatrix(trans,
                34,  0, 0, -2178,
                 0, 67, 0, -5082,
                 0,  0, 1,     0);

        trans.setZ(10);
        trans.setPivotZ(5);
        TransformHelper.assertMatrix(trans,
                34,  0,  0, -2178,
                 0, 67,  0, -5082,
                 0,  0, 10,   -45);
    }
    
    @Test public void testScalePivotCtor() {
        final Scale trans = new Scale(11, 22, 33, 44);
        final Rectangle n = new Rectangle();
        n.getTransforms().add(trans);

        Affine2D expT = new Affine2D();
        expT.translate(33, 44);
        expT.scale(11, 22);
        expT.translate(-33, -44);
        assertTx(n, expT);
    }

    @Test public void testScalePivotCtor3D() {
        final Scale trans = new Scale(11, 22, 33, 44, 55, 66);
        final Rectangle n = new Rectangle();
        n.getTransforms().add(trans);

        Affine3D expT = new Affine3D();
        expT.translate(44, 55, 66);
        expT.scale(11, 22, 33);
        expT.translate(-44, -55, -66);
        assertTx(n, expT);
    }

    @Test
    public void testCopying() {
        final Scale trans = new Scale(34, 67, 10, 66, 77, 5);

        Transform copy = trans.clone();

        TransformHelper.assertMatrix(copy,
                34,  0,  0, -2178,
                 0, 67,  0, -5082,
                 0,  0, 10,   -45);
    }

    @Test public void testToString() {
        final Scale trans = new Scale(5, 8);

        String s = trans.toString();

        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test public void testBoundPropertySynced_PivotX() throws Exception {
        TransformTest.checkDoublePropertySynced(new Scale(3, 3, 0), "pivotX", 200.0);
    }

    @Test public void testBoundPropertySynced_PivotY() throws Exception {
        TransformTest.checkDoublePropertySynced(new Scale(3, 3, 0), "pivotY", 200.0);
    }

    @Test public void testBoundPropertySynced_PivotZ() throws Exception {
        TransformTest.checkDoublePropertySynced(new Scale(3, 3, 0), "pivotZ", 20.0);
    }

    @Test public void testBoundPropertySynced_X() throws Exception {
        TransformTest.checkDoublePropertySynced(new Scale(3, 3, 0), "x", 123.0);
    }

    @Test public void testBoundPropertySynced_Y() throws Exception {
        TransformTest.checkDoublePropertySynced(new Scale(3, 3, 0), "y", 123.0);
    }

    @Test public void testBoundPropertySynced_Z() throws Exception {
        TransformTest.checkDoublePropertySynced(new Scale(3, 3, 0), "z", 123.0);
    }
}
