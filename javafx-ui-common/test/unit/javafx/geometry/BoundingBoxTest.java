/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BoundingBoxTest {
    @Test
    public void testIsEmpty() {
        assertFalse(new BoundingBox(0, 0, 0, 0).isEmpty());
        assertFalse(new BoundingBox(0, 0, 1, 0).isEmpty());
        assertFalse(new BoundingBox(0, 0, 0, 1).isEmpty());
        assertTrue(new BoundingBox(0, 0, -1, 0).isEmpty());
        assertTrue(new BoundingBox(0, 0, 0, -1).isEmpty());
        assertTrue(new BoundingBox(0, 0, -1, -1).isEmpty());
        assertFalse(new BoundingBox(0, 0, 1, 1).isEmpty());
    }

    @Test
    public void testIsEmpty3D() {
        assertFalse(new BoundingBox(0, 0, 0, 0, 0, 0).isEmpty());
        assertFalse(new BoundingBox(0, 0, 0, 1, 0, 0).isEmpty());
        assertFalse(new BoundingBox(0, 0, 0, 0, 1, 0).isEmpty());
        assertFalse(new BoundingBox(0, 0, 0, 0, 0, 1).isEmpty());
        assertFalse(new BoundingBox(0, 0, 0, 1, 1, 1).isEmpty());
        assertTrue(new BoundingBox(0, 0, 0, -1, 0, 0).isEmpty());
        assertTrue(new BoundingBox(0, 0, 0, 0, -1, 0).isEmpty());
        assertTrue(new BoundingBox(0, 0, 0, 0, 0, -1).isEmpty());
        assertTrue(new BoundingBox(0, 0, 0, -1, -1, -1).isEmpty());
    }

    @Test
    public void testContains() {
        BoundingBox bb = new BoundingBox(0, 0, 2, 2);
        assertFalse(bb.contains((Point2D)null));
        assertTrue(bb.contains(new Point2D(1, 1)));

        assertTrue(bb.contains(1, 1));
        assertFalse(bb.contains(3, 0));
        assertFalse(bb.contains(1, -1));
        assertFalse(bb.contains(1, 3));

        assertFalse(bb.contains((Bounds) null));
        assertTrue(bb.contains(new BoundingBox(1, 1, 1, 1)));
        assertFalse(bb.contains(new BoundingBox(-1, 1, 1, 1)));
        assertFalse(bb.contains(new BoundingBox(1, -1, 1, 1)));
        assertFalse(bb.contains(new BoundingBox(1, 1, 2, 1)));
        assertFalse(bb.contains(new BoundingBox(1, 1, 1, 2)));

        assertTrue(bb.contains(1, 1, 1, 1));
        assertFalse(bb.contains(-1, 1, 1, 1));
        assertFalse(bb.contains(1, -1, 1, 1));
        assertFalse(bb.contains(1, 1, 2, 1));
        assertFalse(bb.contains(1, 1, 1, 2));
    }

    @Test
    public void testContains3D() {
        BoundingBox bb = new BoundingBox(0, 0, 0, 2, 2, 1);
        assertFalse(bb.contains((Point2D)null));
        assertTrue(bb.contains(new Point2D(1, 1)));

        assertTrue(bb.contains(1, 1));
        assertFalse(bb.contains(3, 0));
        assertFalse(bb.contains(1, -1));
        assertFalse(bb.contains(1, 3));

        bb = new BoundingBox(0, 0, 0, 2, 2, 2);
        assertFalse(bb.contains(1, 1, 3));
        assertTrue(bb.contains(1, 1, 1));

        assertFalse(bb.contains((Bounds) null));
        assertTrue(bb.contains(new BoundingBox(1, 1, 1, 1, 1, 1)));
        assertFalse(bb.contains(new BoundingBox(-1, 1, 1, 1, 1, 1)));
        assertFalse(bb.contains(new BoundingBox(1, -1, 1, 1, 1, 1)));
        assertFalse(bb.contains(new BoundingBox(1, 1, 1, 2, 1, 1)));
        assertFalse(bb.contains(new BoundingBox(1, 1, 1, 1, 2, 1)));

        assertTrue(bb.contains(1, 1, 0, 1, 1, 2));
        assertFalse(bb.contains(-1, 1, 0, 1, 1, 2));
        assertFalse(bb.contains(1, -1, 0, 1, 1, 2));
        assertFalse(bb.contains(1, 1, 0, 2, 1, 2));
        assertFalse(bb.contains(1, 1, 0, 1, 2, 2));
        assertFalse(bb.contains(1, 1, 1, 1, 2, 3));
    }

    @Test
    public void testIntersects() {
        BoundingBox bb = new BoundingBox(0, 0, 2, 2);
        assertFalse(bb.intersects((Bounds) null));
        assertTrue(new BoundingBox(0, 0, 0, 0).intersects(bb));
        assertTrue(new BoundingBox(0, 0, 0, 0).intersects(0, 0, 0, 0));
        assertTrue(bb.intersects(1, 1, 1, 1));
        assertTrue(bb.intersects(new BoundingBox(1, 1, 1, 1)));
        assertFalse(bb.intersects(3, 3, 3, 3));
        assertFalse(bb.intersects(new BoundingBox(-2, -2, 1, 1)));
    }

    @Test
    public void testIntersects3D() {
        BoundingBox bb = new BoundingBox(0, 0, 0, 2, 2, 1);
        assertFalse(bb.intersects((Bounds) null));
        assertTrue(new BoundingBox(0, 0, 0, 0, 0, 1).intersects(bb));
        assertTrue(new BoundingBox(0, 0, 1, 0, 0, 1).intersects(0, 0, 1, 0, 0, 1));
        assertTrue(bb.intersects(1, 1, 1, 1, 1, 1));
        assertTrue(bb.intersects(new BoundingBox(1, 1, 1, 1, 1, 1)));
        assertFalse(bb.intersects(3, 3, 0, 3, 3, 1));
        assertFalse(bb.intersects(new BoundingBox(-2, -2, 0, 1, 1, 1)));
    }

    @Test
    public void testEquals() {
        BoundingBox p1 = new BoundingBox(0, 0, 0, 0);
        BoundingBox p2 = new BoundingBox(0, 1, 1, 1);
        BoundingBox p3 = new BoundingBox(1, 0, 1, 1);

        assertTrue(p1.equals(p1));
        assertTrue(p1.equals(new BoundingBox(0, 0, 0, 0)));
        assertFalse(p1.equals(new Object()));
        assertFalse(p1.equals(p2));
        assertFalse(p1.equals(p3));
    }

    @Test
    public void testEquals3D() {
        BoundingBox p1 = new BoundingBox(0, 0, 0, 0, 0, 0);
        BoundingBox p2 = new BoundingBox(0, 1, 1, 1, 1, 1);
        BoundingBox p3 = new BoundingBox(1, 0, 1, 1, 1, 1);
        BoundingBox p4 = new BoundingBox(0, 0, 0, 1, 1, 1);
        BoundingBox p5 = new BoundingBox(0, 0, 0, 1, 1, 1);

        assertTrue(p1.equals(p1));
        assertTrue(p1.equals(new BoundingBox(0, 0, 0, 0, 0, 0)));
        assertFalse(p1.equals(new Object()));
        assertFalse(p1.equals(p2));
        assertFalse(p1.equals(p3));
        assertTrue(p4.equals(p5));
    }

    @Test
    public void testToString() {
        BoundingBox p1 = new BoundingBox(0, 0, 0, 0);
        assertNotNull(p1.toString());
    }
}
