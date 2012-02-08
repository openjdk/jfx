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

import java.lang.reflect.Method;

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
