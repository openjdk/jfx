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

import static javafx.scene.transform.TransformTest.assertTx;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

import com.sun.javafx.test.TransformHelper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.NodeTest;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;

import org.junit.Test;

import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.pgstub.StubRectangle;

public class RotateTest {

    @Test
    public void testRotate() {
        final Rotate trans = new Rotate() {{
            setAngle(90);
        }};
        final Rectangle n = new Rectangle();
        n.getTransforms().add(trans);

        Affine2D expTx1 = new Affine2D();
        expTx1.quadrantRotate(1);
        assertTx(n, expTx1);
        TransformHelper.assertMatrix(trans,
                Math.cos(Math.PI / 2.0), -Math.sin(Math.PI / 2.0), 0, 0,
                Math.sin(Math.PI / 2.0),  Math.cos(Math.PI / 2.0), 0, 0,
                         0,                          0,            1, 0);

        trans.setAngle(180);
        Affine2D expTx2 = new Affine2D();
        expTx2.quadrantRotate(2);
        assertTx(n, expTx2);

        trans.setPivotX(66);
        Affine2D expTx3 = new Affine2D();
        expTx3.setToRotation(Math.toRadians(trans.getAngle()), trans.getPivotX(), trans.getPivotY());
        assertTx(n, expTx3);

        trans.setPivotY(77);
        Affine2D expTx4 = new Affine2D();
        expTx4.setToRotation(Math.toRadians(trans.getAngle()), trans.getPivotX(), trans.getPivotY());
        assertTx(n, expTx4);

        trans.setAngle(45);
        trans.setPivotZ(88);
        trans.setAxis(new Point3D(20, 30, 40));

        final Point3D a = new Point3D(20.0 / Math.sqrt(2900.0),
                                30.0 / Math.sqrt(2900.0),
                                40.0 / Math.sqrt(2900.0));
        double sin = Math.sin(Math.PI / 4);
        double cos = Math.cos(Math.PI / 4);

        TransformHelper.assertMatrix(trans,
                cos + a.getX() * a.getX() * (1 - cos), //mxx
                a.getX() * a.getY() * (1 - cos) - a.getZ() * sin, //mxy
                a.getX() * a.getZ() * (1 - cos) + a.getY() * sin, //mxz
                66
                    - 66 * (cos + a.getX() * a.getX() * (1 - cos))
                    - 77 * (a.getX() * a.getY() * (1 - cos) - a.getZ() * sin)
                    - 88 * (a.getX() * a.getZ() * (1 - cos) + a.getY() * sin), //tx
                a.getY() * a.getX() * (1 - cos) + a.getZ() * sin, //myx
                cos + a.getY() * a.getY() * (1 - cos), //myy
                a.getY() * a.getZ() * (1 - cos) - a.getX() * sin, //myz
                77
                    - 66 * (a.getY() * a.getX() * (1 - cos) + a.getZ() * sin)
                    - 77 * (cos + a.getY() * a.getY() * (1 - cos))
                    - 88 * (a.getY() * a.getZ() * (1 - cos) - a.getX() * sin), //ty
                a.getZ() * a.getX() * (1 - cos) - a.getY() * sin, //mzx
                a.getZ() * a.getY() * (1 - cos) + a.getX() * sin, //mzy
                cos + a.getZ() * a.getZ() * (1 - cos), //mzz
                88
                    - 66 * (a.getZ() * a.getX() * (1 - cos) - a.getY() * sin)
                    - 77 * (a.getZ() * a.getY() * (1 - cos) + a.getX() * sin)
                    - 88 * (cos + a.getZ() * a.getZ() * (1 - cos)) //tz

                );
    }

    @Test public void testRotateAxisCtor() {
        final Rotate trans = new Rotate(11, new Point3D(22, 33, 44));
        final Rectangle n = new Rectangle();
        n.getTransforms().add(trans);

        Affine3D expT = new Affine3D();
        expT.rotate(Math.toRadians(11), 22, 33, 44);
        assertTx(n, expT);
    }

    @Test public void testRotate3DPivotCtor() {
        final Rotate trans = new Rotate(11, 22, 33, 44);
        final Rectangle n = new Rectangle();
        n.getTransforms().add(trans);

        Affine3D expT = new Affine3D();
        expT.translate(22, 33, 44);
        expT.rotate(Math.toRadians(11));
        expT.translate(-22, -33, -44);
        assertTx(n, expT);
    }

   @Test public void testRotate3DPivotAxisCtor() {
        final Rotate trans = new Rotate(11, 22, 33, 44, new Point3D(55, 66, 77));
        final Rectangle n = new Rectangle();
        n.getTransforms().add(trans);

        Affine3D expT = new Affine3D();
        expT.translate(22, 33, 44);
        expT.rotate(Math.toRadians(11), 55, 66, 77);
        expT.translate(-22, -33, -44);
        assertTx(n, expT);
    }

    @Test public void testBoundPropertySynced_Angle() throws Exception {
        TransformTest.checkDoublePropertySynced(new Rotate(300, 300, 0), "angle", 30.0);
    }

    @Test public void testBoundPropertySynced_PivotX() throws Exception {
        TransformTest.checkDoublePropertySynced(new Rotate(300, 300, 0), "pivotX", 200.0);
    }

    @Test public void testBoundPropertySynced_PivotY() throws Exception {
        TransformTest.checkDoublePropertySynced(new Rotate(300, 300, 0), "pivotY", 200.0);
    }

    @Test public void testBoundPropertySynced_PivotZ() throws Exception {
        TransformTest.checkDoublePropertySynced(new Rotate(300, 300, 0), "pivotZ", 20.0);
    }

    @Test public void testBoundPropertySynced_Axis() throws Exception {
        checkObjectPropertySynced("axis", new Point3D(1, 0, 0));
    }

    @Test
    public void testCopying() {
        final Rotate trans = new Rotate();

        trans.setAngle(45);
        trans.setPivotX(66);
        trans.setPivotY(77);
        trans.setPivotZ(88);
        trans.setAxis(new Point3D(20, 30, 40));

        Transform copy = trans.impl_copy();

        final Point3D a = new Point3D(20.0 / Math.sqrt(2900.0),
                                30.0 / Math.sqrt(2900.0),
                                40.0 / Math.sqrt(2900.0));
        double sin = Math.sin(Math.PI / 4);
        double cos = Math.cos(Math.PI / 4);

        TransformHelper.assertMatrix(copy,
                cos + a.getX() * a.getX() * (1 - cos), //mxx
                a.getX() * a.getY() * (1 - cos) - a.getZ() * sin, //mxy
                a.getX() * a.getZ() * (1 - cos) + a.getY() * sin, //mxz
                66
                    - 66 * (cos + a.getX() * a.getX() * (1 - cos))
                    - 77 * (a.getX() * a.getY() * (1 - cos) - a.getZ() * sin)
                    - 88 * (a.getX() * a.getZ() * (1 - cos) + a.getY() * sin), //tx
                a.getY() * a.getX() * (1 - cos) + a.getZ() * sin, //myx
                cos + a.getY() * a.getY() * (1 - cos), //myy
                a.getY() * a.getZ() * (1 - cos) - a.getX() * sin, //myz
                77
                    - 66 * (a.getY() * a.getX() * (1 - cos) + a.getZ() * sin)
                    - 77 * (cos + a.getY() * a.getY() * (1 - cos))
                    - 88 * (a.getY() * a.getZ() * (1 - cos) - a.getX() * sin), //ty
                a.getZ() * a.getX() * (1 - cos) - a.getY() * sin, //mzx
                a.getZ() * a.getY() * (1 - cos) + a.getX() * sin, //mzy
                cos + a.getZ() * a.getZ() * (1 - cos), //mzz
                88
                    - 66 * (a.getZ() * a.getX() * (1 - cos) - a.getY() * sin)
                    - 77 * (a.getZ() * a.getY() * (1 - cos) + a.getX() * sin)
                    - 88 * (cos + a.getZ() * a.getZ() * (1 - cos)) //tz

                );
    }

    @Test public void testToString() {
        final Rotate trans = new Rotate(40);

        String s = trans.toString();

        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    private void checkObjectPropertySynced(String propertyName, Object val)
            throws Exception {

        final Rectangle r = new Rectangle(200, 200, 200, 200);
        final Rotate rotate = new Rotate(300, 300, 0);

        ObjectProperty v = new SimpleObjectProperty(0.0);
        Method m = Rotate.class.getMethod(propertyName + "Property", new Class[] {});
        ((ObjectProperty)m.invoke(rotate)).bind(v);

        r.getTransforms().add(rotate);
        ((Group)new Scene(new Group()).getRoot()).getChildren().add(r);

        v.set(val);
        NodeTest.syncNode(r);

        //check
        StubRectangle pgR = (StubRectangle)r.impl_getPGNode();
        assertTx(r, pgR.getTransformMatrix());
    }
}
