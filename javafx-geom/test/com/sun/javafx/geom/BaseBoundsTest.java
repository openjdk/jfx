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
package com.sun.javafx.geom;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class BaseBoundsTest {

    public @Test
    void testBounds_MakeEmptyTest() {
        RectBounds rectBounds = new RectBounds(10, 20, 20, 30);
        assertFalse(rectBounds.isEmpty());
        rectBounds.makeEmpty();
        assertTrue(rectBounds.isEmpty());
        assertEquals(new RectBounds(), rectBounds);

        BoxBounds boxBounds = new BoxBounds(10, 20, 10, 40, 50, 20);
        assertFalse(boxBounds.isEmpty());
        boxBounds.makeEmpty();
        assertTrue(boxBounds.isEmpty());
        assertEquals(new BoxBounds(), boxBounds);
    }

    public @Test
    void testRectangle_ZeroArea() {
        RectBounds rectBounds = new RectBounds(new Rectangle());
        assertFalse(rectBounds.isEmpty());
        RectBounds rectBounds2 = new RectBounds(0, 0, 0, 0);
        assertEquals(rectBounds, rectBounds2);
    }

    public @Test
    void testRectangle_Offset() {
        // the arguments are (x, y, width, height)
        Rectangle rect = new Rectangle(10, 20, 40, 50);
        RectBounds rectBounds = new RectBounds(rect);
        assertFalse(rectBounds.isEmpty());

        // the arguments are (minX, minY, maxX, maxY)
        RectBounds rectBounds2 = new RectBounds(10, 20, 50, 70);
        assertEquals(rectBounds, rectBounds2);
    }

    public @Test
    void testBounds_IntersectsTest1() {
        RectBounds rectBounds = new RectBounds(10, 20, 40, 50);
        assertTrue(rectBounds.is2D());
        assertFalse(rectBounds.isEmpty());

        BoxBounds boxBounds = new BoxBounds(10, 20, 0, 20, 30, 0);
        assertFalse(boxBounds.is2D());
        assertTrue(boxBounds.intersects(rectBounds));
    }

    public @Test
    void testBounds_IntersectsTest2() {
        RectBounds rectBounds = new RectBounds(10, 20, 40, 50);
        assertFalse(rectBounds.isEmpty());

        BoxBounds boxBounds = new BoxBounds(10, 20, 1, 20, 30, 1);
        assertFalse(boxBounds.intersects(rectBounds));
    }

    public @Test
    void testBounds_IntersectsTest3() {
        BoxBounds boxBounds = new BoxBounds(10, 20, 10, 40, 50, 20);
        assertFalse(boxBounds.isEmpty());

        BoxBounds boxBounds2 = new BoxBounds(10, 20, 0, 20, 30, 5);
        assertFalse(boxBounds2.intersects(boxBounds));
    }

    public @Test
    void testBounds_IntersectsTest4() {
        BoxBounds boxBounds = new BoxBounds(10, 20, 10, 40, 50, 20);
        assertFalse(boxBounds.isEmpty());

        BoxBounds boxBounds2 = new BoxBounds(10, 20, 0, 20, 30, 10);
        assertTrue(boxBounds2.intersects(boxBounds));
    }

    public @Test
    void testBounds_SetBoundsAndSortTest() {
        RectBounds rectBounds = new RectBounds();
        assertTrue(rectBounds.isEmpty());
        rectBounds.setBoundsAndSort(20, 30, 10, 20);
        assertFalse(rectBounds.isEmpty());
        assertEquals(new RectBounds(10, 20, 20, 30), rectBounds);

        BoxBounds boxBounds = new BoxBounds();
        assertTrue(boxBounds.isEmpty());
        boxBounds.setBoundsAndSort(40, 50, 20, 10, 20, 10);
        assertFalse(boxBounds.isEmpty());
        assertEquals(new BoxBounds(10, 20, 10, 40, 50, 20), boxBounds);
    }

    public @Test
    void testBounds_UnionWithTest() {
        RectBounds rectBounds = new RectBounds();
        assertTrue(rectBounds.isEmpty());
        RectBounds rectBounds2 = new RectBounds(0, 1, 2, 4);
        rectBounds.unionWith(rectBounds2);
        assertFalse(rectBounds.isEmpty());
        assertEquals(new RectBounds(0, 1, 2, 4), rectBounds);
        RectBounds rectBounds3 = new RectBounds(-1, -2, 2, 3);
        rectBounds.unionWith(rectBounds3);
        assertEquals(new RectBounds(-1, -2, 2, 4), rectBounds);

        BoxBounds boxBounds = new BoxBounds();
        assertTrue(boxBounds.isEmpty());
        BoxBounds boxBounds2 = new BoxBounds(0, 1, 2, 2, 3, 4);
        boxBounds.unionWith(boxBounds2);
        assertFalse(boxBounds.isEmpty());
        assertEquals(new BoxBounds(0, 1, 2, 2, 3, 4), boxBounds);
        BoxBounds boxBounds3 = new BoxBounds(-1, -2, -3, 1, 2, 3);
        boxBounds.unionWith(boxBounds3);
        assertEquals(new BoxBounds(-1, -2, -3, 2, 3, 4), boxBounds);
    }

}
