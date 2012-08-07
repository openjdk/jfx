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
    public void testToString() {
        Point2D p1 = new Point2D(0, 0);
        assertNotNull(p1.toString());
    }
}
