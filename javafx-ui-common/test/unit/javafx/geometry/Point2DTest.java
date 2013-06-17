/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class Point2DTest {

    @Test
    public void testConstruction() {
        Point2D p = new Point2D(1f, 2f);
        assertEquals(1f, p.getX(), 1e-100);
        assertEquals(2f, p.getY(), 1e-100);
    }
    
    @Test
    public void testDistance() {
        Point2D p1 = new Point2D(0, 0);
        Point2D p2 = new Point2D(1, 0);
        Point2D p3 = new Point2D(1, 1);
        
        assertEquals(1, p2.distance(p1), 1e-100);
        assertEquals(1, p2.distance(0, 0), 1e-100);
        assertEquals(1, p2.distance(p3), 1e-100);
        assertEquals(1.41421356, p1.distance(p3), 1e-5);
    }

    @Test
    public void testEquals() {
        Point2D p1 = new Point2D(0, 0);
        Point2D p2 = new Point2D(0, 1);
        Point2D p3 = new Point2D(1, 0);

        assertTrue(p1.equals(p1));
        assertTrue(p1.equals(new Point2D(0, 0)));
        assertFalse(p1.equals(new Object()));
        assertFalse(p1.equals(p2));
        assertFalse(p1.equals(p3));
    }

    @Test
    public void testHash() {
        Point2D p1 = new Point2D(0, 0);
        Point2D p2 = new Point2D(0, 1);
        Point2D p3 = new Point2D(0, 1);

        assertEquals(p3.hashCode(), p2.hashCode());
        assertEquals(p3.hashCode(), p2.hashCode());
        assertFalse(p1.hashCode() == p2.hashCode());
    }

    @Test
    public void testAdd() {
        Point2D p1 = new Point2D(2, 4);
        Point2D p2 = new Point2D(-1, 3);

        assertEquals(new Point2D(1, 7), p1.add(p2));
        assertEquals(new Point2D(1, 7), p1.add(-1, 3));
    }

    @Test(expected=NullPointerException.class)
    public void testAddNull() {
        Point2D point = new Point2D(1, 2);
        point.add(null);
    }

    @Test
    public void testSubtract() {
        Point2D p1 = new Point2D(2, 4);
        Point2D p2 = new Point2D(-1, 3);

        assertEquals(new Point2D(3, 1), p1.subtract(p2));
        assertEquals(new Point2D(3, 1), p1.subtract(-1, 3));
    }

    @Test(expected=NullPointerException.class)
    public void testSubtractNull() {
        Point2D point = new Point2D(1, 2);
        point.subtract(null);
    }

    @Test
    public void testMultiplyByNumber() {
        Point2D p1 = new Point2D(2, 4);
        Point2D p2 = new Point2D(-1, 3);

        assertEquals(new Point2D(4, 8), p1.multiply(2));
        assertEquals(new Point2D(1, -3), p2.multiply(-1));
        assertEquals(new Point2D(0, 0), p1.multiply(0));
    }

    @Test
    public void testNormalize() {
        Point2D p1 = new Point2D(0, 0);
        Point2D p2 = new Point2D(0, 1);
        Point2D p3 = new Point2D(1, 1);
        Point2D p4 = new Point2D(120, -350);

        double sqrt3 = Math.sqrt(2);
        double sqrt4 = Math.sqrt(136900);

        assertEquals(new Point2D(0, 0), p1.normalize());
        assertEquals(new Point2D(0, 1), p2.normalize());
        assertEquals(new Point2D(1 / sqrt3, 1 / sqrt3), p3.normalize());
        assertEquals(new Point2D(120 / sqrt4, -350 / sqrt4), p4.normalize());
    }

    @Test
    public void testMidpoint() {
        Point2D p1 = new Point2D(0, 0);
        Point2D p2 = new Point2D(1, -2);

        assertEquals(new Point2D(0.5, -1), p1.midpoint(p2));
        assertEquals(new Point2D(0.5, -1), p1.midpoint(1, -2));
    }

    @Test(expected=NullPointerException.class)
    public void testMidpointNull() {
        Point2D point = new Point2D(1, 2);
        point.midpoint(null);
    }

    @Test
    public void testVectorAngle() {
        Point2D p1 = new Point2D(0, 0);
        Point2D p2 = new Point2D(0, 1);
        Point2D p3 = new Point2D(1, 1);
        Point2D p4 = new Point2D(-1, 0);
        Point2D p5 = new Point2D(10, 10);

        assertEquals(Double.NaN, p1.angle(p2), 0.000001);
        assertEquals(0, p3.angle(p5), 0.000001);
        assertEquals(0, p3.angle(p3), 0.000001);
        assertEquals(45, p2.angle(p3), 0.000001);
        assertEquals(90, p2.angle(p4), 0.000001);
        assertEquals(135, p2.angle(-1, -1), 0.000001);
        assertEquals(Double.NaN, p2.angle(p2, p4), 0.000001);
    }

    @Test(expected=NullPointerException.class)
    public void testVectorAngleNull() {
        Point2D point = new Point2D(1, 2);
        point.angle(null);
    }

    @Test
    public void testPointAngle() {
        Point2D p1 = new Point2D(2, 2);
        Point2D p2 = new Point2D(0, 2);
        Point2D p3 = new Point2D(-3, 2);
        Point2D p4 = new Point2D(-7, 2);
        Point2D p5 = new Point2D(-3, 4);
        Point2D p6 = new Point2D(-5, 2);

        assertEquals(180, p2.angle(p1, p3), 0.000001);
        assertEquals(90, p3.angle(p5, p6), 0.000001);
        assertEquals(0, p2.angle(p3, p4), 0.000001);
    }

    @Test(expected=NullPointerException.class)
    public void testPointAngle1Null() {
        Point2D point = new Point2D(1, 2);
        point.angle(null, new Point2D(2, 8));
    }

    @Test(expected=NullPointerException.class)
    public void testPointAngle2Null() {
        Point2D point = new Point2D(2, 3);
        point.angle(new Point2D(5, 3), null);
    }

    @Test
    public void testPointAngleTooClose() {
        Point2D p1 = new Point2D(-0.8944271909999159, 0.4472135954999579);
        Point2D v = new Point2D(0.0, 0.0);
        Point2D p2 = new Point2D(-0.894427190999924, 0.4472135954999417);
        assertEquals(0.0, v.angle(p1, p2), 0.000001);
        assertEquals(0.0, v.angle(p2, p1), 0.000001);
    }

    @Test
    public void testPointAngleTooOpposite() {
        Point2D p1 = new Point2D(-0.8944271909999159, 0.4472135954999579);
        Point2D v = new Point2D(0.0, 0.0);
        Point2D p2 = new Point2D(0.894427190999924, -0.4472135954999417);
        assertEquals(180.0, v.angle(p1, p2), 0.000001);
        assertEquals(180.0, v.angle(p2, p1), 0.000001);
    }

    @Test
    public void testMagnitude() {
        Point2D p1 = new Point2D(0, 0);
        Point2D p2 = new Point2D(0, 1);
        Point2D p3 = new Point2D(-10, 20);

        assertEquals(0, p1.magnitude(), 0.000001);
        assertEquals(1, p2.magnitude(), 0.000001);
        assertEquals(Math.sqrt(500), p3.magnitude(), 0.000001);
    }

    @Test
    public void testDotProduct() {
        Point2D p1 = new Point2D(0, 0);
        Point2D p2 = new Point2D(1, 1);
        Point2D p3 = new Point2D(2, -2);
        Point2D p4 = new Point2D(-4, 5);

        assertEquals(0, p1.dotProduct(p4), 0.000001);
        assertEquals(1, p2.dotProduct(p4), 0.000001);
        assertEquals(-18, p3.dotProduct(p4), 0.000001);
        assertEquals(2, p3.dotProduct(-4, -5), 0.000001);
    }

    @Test(expected=NullPointerException.class)
    public void testDotProductNull() {
        Point2D point = new Point2D(1, 2);
        point.dotProduct(null);
    }

    @Test
    public void testCrossProduct() {
        Point2D p1 = new Point2D(0, 0);
        Point2D p2 = new Point2D(0, 3);
        Point2D p3 = new Point2D(2, 0);

        assertEquals(new Point3D(0, 0, 0), p1.crossProduct(p3));
        assertEquals(new Point3D(0, 0, 0), p2.crossProduct(p1));
        assertEquals(new Point3D(0, 0, -6), p2.crossProduct(p3));
        assertEquals(new Point3D(0, 0, 6), p3.crossProduct(p2));
    }

    @Test(expected=NullPointerException.class)
    public void testCrossProductNull() {
        Point2D point = new Point2D(1, 2);
        point.crossProduct(null);
    }

    @Test
    public void testAngleTooClose() {
        Point2D p1 = new Point2D(-0.8944271909999159, 0.4472135954999579);
        Point2D p2 = new Point2D(-0.894427190999924, 0.4472135954999417);
        assertEquals(0.0, p1.angle(p2), 0.000001);
        assertEquals(0.0, p2.angle(p1), 0.000001);
    }

    @Test
    public void testAngleTooOpposite() {
        Point2D p1 = new Point2D(-0.8944271909999159, 0.4472135954999579);
        Point2D p2 = new Point2D(0.894427190999924, -0.4472135954999417);
        assertEquals(180.0, p1.angle(p2), 0.000001);
        assertEquals(180.0, p2.angle(p1), 0.000001);
    }

    @Test
    public void testToString() {
        Point2D p1 = new Point2D(0, 0);
        assertNotNull(p1.toString());
    }
}
