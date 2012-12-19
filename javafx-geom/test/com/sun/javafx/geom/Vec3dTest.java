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

package com.sun.javafx.geom;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * PRELIMINARY unit tests for Vec3d.
 *
 * TODO: expand this to cover the entire class (RT-26882)
 */
public class Vec3dTest {

    private static double EPSILON = 1e-10;

    @Test
    public void testDefaultContructor() {
        Vec3d v3d = new Vec3d();
        assertEquals(0, v3d.x, 0);
        assertEquals(0, v3d.y, 0);
        assertEquals(0, v3d.z, 0);
    }

    @Test
    public void testContructor1() {
        Vec3d v3d = new Vec3d(1.0, 2.0, 3.0);
        assertEquals(1, v3d.x, 0);
        assertEquals(2, v3d.y, 0);
        assertEquals(3, v3d.z, 0);
    }

    @Test
    public void testContructor2() {
        Vec3f v3f = new Vec3f(1f, 2f, 3f);
        Vec3d v3d = new Vec3d(v3f);
        assertEquals(1, v3d.x, 0);
        assertEquals(2, v3d.y, 0);
        assertEquals(3, v3d.z, 0);
    }

    @Test
    public void testLength() {
        Vec3d v3d = new Vec3d();
        double len = v3d.length();
        assertEquals(0, len, 0);

        v3d = new Vec3d(1, 2, 3);
        len = v3d.length();
        assertEquals(Math.sqrt(14.0), len, EPSILON);

        v3d = new Vec3d(-1, 2, 3);
        len = v3d.length();
        assertEquals(Math.sqrt(14.0), len, EPSILON);

        v3d = new Vec3d(1, -0.2, -0.03);
        len = v3d.length();
        assertEquals(Math.sqrt(1.0409), len, EPSILON);

        v3d = new Vec3d(-0.1, -0.2, -0.3);
        len = v3d.length();
        assertEquals(Math.sqrt(0.14), len, EPSILON);
    }

}
