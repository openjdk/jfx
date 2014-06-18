/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.shape;

import javafx.geometry.BoundingBox;
import static org.junit.Assert.*;
import org.junit.Test;

public class CylinderTest {

    @Test
    public void testGetDivisionsClamp() {
        Cylinder cylinder = new Cylinder(10, 10, 1);
        assertEquals(cylinder.getDivisions(), 3);

        cylinder = new Cylinder(10, 10, -1);
        assertEquals(cylinder.getDivisions(), 3);
    }

    @Test
    public void testImpl_computeGeomBoundsOnNegDimension() {
        Cylinder cylinder = new Cylinder(10, -10);
        cylinder.impl_updatePeer(); // should not throw NPE
        assertTrue(cylinder.getBoundsInLocal().isEmpty());
        assertEquals(cylinder.getHeight(), -10, 0.00001);

        cylinder = new Cylinder(-10, 10);
        cylinder.impl_updatePeer(); // should not throw NPE
        assertTrue(cylinder.getBoundsInLocal().isEmpty());
        assertEquals(cylinder.getRadius(), -10, 0.00001);
    }

    @Test
    public void testImpl_computeGeomBoundsOnDegeneratedShape() {
        Cylinder cylinder = new Cylinder(0, 0);
        cylinder.impl_updatePeer(); // should not throw NPE
        assertEquals(cylinder.getBoundsInLocal(), new BoundingBox(0, 0, 0, 0, 0, 0));
        
        cylinder = new Cylinder(10, 0);
        cylinder.impl_updatePeer(); // should not throw NPE
        assertEquals(cylinder.getBoundsInLocal(), new BoundingBox(-10.0, 0, -10.0, 20, 0, 20));
        
        cylinder = new Cylinder(0, 10);
        cylinder.impl_updatePeer(); // should not throw NPE
        assertEquals(cylinder.getBoundsInLocal(), new BoundingBox(0, -5.0, 0, 0, 10, 0));
    }
}

