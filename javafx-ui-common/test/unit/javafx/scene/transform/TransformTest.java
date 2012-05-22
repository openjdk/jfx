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
package javafx.scene.transform;

import java.lang.reflect.Method;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.NodeTest;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.pgstub.StubRectangle;
import com.sun.javafx.scene.transform.TransformUtils;
import com.sun.javafx.test.TransformHelper;
import javafx.geometry.Point3D;


public class TransformTest {

    static void assertTx(Node n, BaseTransform trans) {
        Assert.assertEquals(trans, n.impl_getLeafTransform());
    }


    @Test
    public void testTranslate() {
        Transform tx = Transform.translate(25, 52);
        final Rectangle n = new Rectangle();
        n.getTransforms().add(tx);

        assertTx(n, BaseTransform.getTranslateInstance(25, 52));
    }


    @Test
    public void testScale1() {
        Transform tx = Transform.scale(25, 52);
        final Rectangle n = new Rectangle();
        n.getTransforms().add(tx);

        assertTx(n, BaseTransform.getScaleInstance(25, 52));
    }


    @Test
    public void testScale2() {
        Transform tx = Transform.scale(25, 52, 66, 77);
        final Rectangle n = new Rectangle();
        n.getTransforms().add(tx);

        Affine2D expTx = new Affine2D();
        expTx.setToTranslation(66, 77);
        expTx.scale(25, 52);
        expTx.translate(-66, -77);
        assertTx(n, expTx);
    }


    @Test
    public void testRotate() {
        Rotate tx = Transform.rotate(90, 3, 5);
        final Rectangle n = new Rectangle();
        n.getTransforms().add(tx);

        Affine2D expTx = new Affine2D();
        expTx.quadrantRotate(1, 3, 5);
        assertTx(n, expTx);
    }


    @Test
    public void testShear() {
        Transform tx = Transform.shear(25, 52);
        final Rectangle n = new Rectangle();
        n.getTransforms().add(tx);

        Affine2D expTx1 = new Affine2D();
        expTx1.setToShear(25, 52);
        assertTx(n, expTx1);
    }

    @Test
    public void testShearWithPivot() {
        Transform tx = Transform.shear(25, 52, 35, 53);
        final Rectangle n = new Rectangle();
        n.getTransforms().add(tx);

        Affine2D expTx1 = new Affine2D();

        expTx1.translate(35, 53);
        expTx1.shear(25, 52);
        expTx1.translate(-35, -53);

        assertTx(n, expTx1);
    }


    @Test
    public void testAffine() {
        final Transform trans = Transform.affine(11, 22, 33, 44, 55, 66);
        final Rectangle n = new Rectangle();
        n.getTransforms().add(trans);

        final Affine2D affine2D = new Affine2D(11, 22, 33, 44, 55, 66);
        final Affine3D affine3D = new Affine3D(affine2D);
        assertTx(n, affine3D);
    }

    @Test
    public void testAffine3D() {
        final Transform trans = Transform.affine(11, 22, 33, 44, 55, 66, 77, 88, 99, 111, 222, 333);
        final Rectangle n = new Rectangle();
        n.getTransforms().add(trans);

        final Affine3D affine3D = new Affine3D(11, 22, 33, 44, 55, 66, 77, 88, 99, 111, 222, 333);
        assertTx(n, affine3D);
    }

    @Test
    public void defaultTransformShouldBeIdentity() {
        final Transform t = new Transform() {
            @Override
            public void impl_apply(com.sun.javafx.geom.transform.Affine3D ad) {}
            @Override
            public Transform impl_copy() { return null; }
        };

        TransformHelper.assertMatrix(t,
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0);
    }
    
    @Test
    public void testTransform() {
        Transform t = TransformUtils.immutableTransform(
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        Point3D point = new Point3D(2, 5, 7);

        Point3D transformed = t.impl_transform(point);

        assertEquals(37, transformed.getX(), 0.0001);
        assertEquals(97, transformed.getY(), 0.0001);
        assertEquals(157, transformed.getZ(), 0.0001);
    }

    static void checkDoublePropertySynced(Transform tr, String propertyName, double val)
        throws Exception {

        final Rectangle r = new Rectangle(200, 200, 200, 200);

        DoubleProperty v = new SimpleDoubleProperty(0.0);
        Method m = tr.getClass().getMethod(propertyName + "Property", new Class[] {});
        ((DoubleProperty)m.invoke(tr)).bind(v);

        r.getTransforms().add(tr);
        ((Group)new Scene(new Group()).getRoot()).getChildren().add(r);

        v.set(val);
        NodeTest.syncNode(r);

        //check
        StubRectangle pgR = (StubRectangle)r.impl_getPGNode();
        assertTx(r, pgR.getTransformMatrix());
    }
}
