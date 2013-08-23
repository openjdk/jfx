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

package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.RectBounds;
import com.sun.prism.paint.Color;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class NGCircleTest extends NGTestBase {

    NGCircle circle;

    @Before public void setup() {
        circle = new NGCircle();
        circle.setFillPaint(Color.RED);
        circle.updateCircle(10, 10, 5);
    }

    @Test
    public void testSupportsOpaqueRegion() {
        assertTrue(circle.supportsOpaqueRegions());
    }

    @Test
    public void hasOpaqueRegionIfRadiusIsGreaterThanZero() {
        assertTrue(circle.hasOpaqueRegion());
        circle.updateCircle(10, 10, 0);
        assertFalse(circle.hasOpaqueRegion());
        circle.updateCircle(10, 10, .0001f);
        assertTrue(circle.hasOpaqueRegion());
    }

    @Test
    public void opaqueRegionLiesWithinCircle() {
        RectBounds or = new RectBounds();
        // Just sort of try a range of values. They should all be good.
        final float[] radiusValues = new float[] {
                .001f,
                1f/3f,
                (float) Math.E,
                (float) Math.PI,
                10f,
                13.321f // some random number
        };
        for (float r : radiusValues) {
            circle.updateCircle(10, 10, r);
            or = circle.computeOpaqueRegion(or);
            assertNotNull(or);
            assertTrue(circle.getShape().contains(or.getMinX(), or.getMinY(), or.getWidth(), or.getHeight()));
        }
    }

    /**
     * Perform a simple test to be sure that the opaque region is just
     * about as large as it could possibly be without going outside the
     * bounds of the ellipse. This is basically using the same math as
     * the implementation, except that we use the more precise math here
     * than what the implementation uses. So I will compute the wider box
     * and a narrower box and make sure the implementation is between the two.
     */
    @Test
    public void testComputeOpaqueRegion() {
        RectBounds or = circle.computeOpaqueRegion(new RectBounds());

        // First we will compute with the highest precision we can.
        float r = 5; // same as the ellipse
        float side = 2*r / (float) Math.sqrt(2);
        float halfSide = side / 2f;
        float x1 = 10 - halfSide; // centerX = 10
        float y1 = 10 - halfSide; // centerY = 10
        float x2 = 10 + halfSide;
        float y2 = 10 + halfSide;
        // Less than really accurate
        assertTrue(x1 < or.getMinX());
        assertTrue(y1 < or.getMinY());
        assertTrue(x2 > or.getMaxX());
        assertTrue(y2 > or.getMaxY());
        // But not too far off
        assertEquals(x1, or.getMinX(), .1f);
        assertEquals(y1, or.getMinY(), .1f);
        assertEquals(x2, or.getMaxX(), .1f);
        assertEquals(y2, or.getMaxY(), .1f);

    }
}
