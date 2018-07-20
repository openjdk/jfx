/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.geom.transform;

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.Vec3f;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This Unit Test covers GeneralTransform3D and some of its calculations.
 */
public class GeneralTransform3DTest {

    static GeneralTransform3D generalTransform3D = new GeneralTransform3D();
    final float delta = 0.001f;

    static double[] mat = new double[] {
        3.7, 3.1, 4.7, 3.5,
        2.9, 1.0, 5.9, 4.7,
        9.1, 5.1, 1.0, 5.9,
        0.0, 0.0, 0.0, 1.0
    };

    private void equals(Point2D point1, Point2D point2) {
        assertEquals(point1.x, point2.x, delta);
        assertEquals(point1.y, point2.y, delta);
    }

    private void equals(Vec3d vec1, Vec3d vec2) {
        assertEquals(vec1.x, vec2.x, delta);
        assertEquals(vec1.y, vec2.y, delta);
        assertEquals(vec1.z, vec2.z, delta);
    }

    private void equals(Vec3f vec1, Vec3f vec2) {
        assertEquals(vec1.x, vec2.x, delta);
        assertEquals(vec1.y, vec2.y, delta);
        assertEquals(vec1.z, vec2.z, delta);
    }

    @BeforeClass
    public static void setUpClass() {
        generalTransform3D.set(mat);
    }

    @Test
    public void testTransformPoint2D() {

        float x = 10.5f;
        float y = 15.7f;

        Point2D point1 = generalTransform3D.transform(new Point2D(x, y), null);
        Point2D point2 = new Point2D();
        Point2D point3 = generalTransform3D.transform(new Point2D(x, y), point2);
        equals(point1, point2);
        assertSame(point2, point3);

        Point2D point4 = new Point2D(x, y);
        Point2D point5 = generalTransform3D.transform(point4, point4);
        equals(point1, point4);
        assertSame(point4, point5);
    }

    @Test
    public void testTransformVec3d() {

        double x = 10.5;
        double y = 15.7;
        double z = 20.9;

        Vec3d vec1 = generalTransform3D.transform(new Vec3d(x, y, z), null);
        Vec3d vec2 = new Vec3d();
        Vec3d vec3 = generalTransform3D.transform(new Vec3d(x, y, z), vec2);
        equals(vec1, vec2);
        assertSame(vec2, vec3);

        Vec3d vec4 = new Vec3d(x, y, z);
        Vec3d vec5 = generalTransform3D.transform(vec4);
        equals(vec3, vec5);
        assertSame(vec4, vec5);

        Vec3d vec6 = new Vec3d(x, y, z);
        Vec3d vec7 = generalTransform3D.transform(vec6, vec6);
        equals(vec3, vec6);
        assertSame(vec6, vec7);
    }

    @Test
    public void testTransformNormal() {

        float x = 1.0f;
        float y = 1.0f;
        float z = 1.0f;

        Vec3f vec1 = generalTransform3D.transformNormal(new Vec3f(x, y, z), null);
        Vec3f vec2 = new Vec3f();
        Vec3f vec3 = generalTransform3D.transformNormal(new Vec3f(x, y, z), vec2);
        equals(vec1, vec2);
        assertSame(vec2, vec3);

        Vec3f vec4 = new Vec3f(x, y, z);
        Vec3f vec5 = generalTransform3D.transformNormal(vec4);
        equals(vec3, vec5);
        assertSame(vec4, vec5);

        Vec3f vec6 = new Vec3f(x, y, z);
        Vec3f vec7 = generalTransform3D.transformNormal(vec6, vec6);
        equals(vec3, vec6);
        assertSame(vec6, vec7);
    }
}
