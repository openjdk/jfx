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

package test.javafx.scene.shape;

import static org.junit.Assert.*;

import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.PredefinedMeshManagerShim;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;

import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.scene.NodeHelper;

public class PredefinedMeshManagerTest {

    @Before
    public void clearCaches() {
        PredefinedMeshManagerShim.clearCaches();
    }

    private void testShapeAddition(Shape3D shape, int correctSize) {
        NodeHelper.updatePeer(shape);
        int size = -1;
        String name = null;
        if (shape instanceof Box) {
            size = PredefinedMeshManagerShim.getBoxCacheSize();
            name = "box";
        }
        else if (shape instanceof Sphere) {
            size = PredefinedMeshManagerShim.getSphereCacheSize();
            name = "sphere";
        }
        else if (shape instanceof Cylinder) {
            size = PredefinedMeshManagerShim.getCylinderCacheSize();
            name = "cylinder";
        }
        assertEquals("Added a " + name + " - cache should contain " + correctSize + " mesh.", correctSize, size);
    }

    @Test
    public void boxCacheTest() {
        Box box1 = new Box(9, 1, 12);
        testShapeAddition(box1 ,1);

        // JDK-8180151: size will stay 1 without the fix (due to hash collision)
        // new dimensions to cause a collision are any w/2, h*4, d/2
        Box box2 = new Box(4.5, 4, 6);
        testShapeAddition(box2 ,2);

        Box box1again = new Box(9, 1, 12);
        testShapeAddition(box1again, 2);
    }

    @Test
    public void sphereCacheTest() {
        Sphere sphere1 = new Sphere(10, 50);
        testShapeAddition(sphere1, 1);

        // JDK-8180151: size will stay 1 without the fix (due to hash collision)
        // From the old hash function:
        // div2 = 23 * (Float.floatToIntBits(r1) - Float.floatToIntBits(r2)) + div1
        Sphere sphere2 = new Sphere(9.999998, 96);
        testShapeAddition(sphere2, 2);

        Sphere sphere1again = new Sphere(10, 50);
        testShapeAddition(sphere1again, 2);
    }

    @Test
    public void cylinderCacheTest() {
        Cylinder cylinder1 = new Cylinder(10, 20, 100);
        testShapeAddition(cylinder1, 1);

        // JDK-8180151: size will stay 1 without the fix (due to hash collision)
        // From the old hash function:
        // div2 = 47*47 * (Float.floatToIntBits(h1) - Float.floatToIntBits(h2)) +
        //           47 * (Float.floatToIntBits(r1) - Float.floatToIntBits(r2)) + div1;
        Cylinder cylinder2 = new Cylinder(30560, 31072, 100);
        testShapeAddition(cylinder2, 2);

        Cylinder cylinder1again = new Cylinder(10, 20, 100);
        testShapeAddition(cylinder1again, 2);
    }
}
