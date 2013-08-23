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
import static org.junit.Assert.assertTrue;

/**
 * Tests for NGRectangle class
 */
public class NGRectangleTest extends NGTestBase {
    NGRectangleMock r;

    @Before
    public void setup() {
        r = new NGRectangleMock();
        r.updateRectangle(0, 0, 100, 100, 0, 0);
        r.setFillPaint(Color.BLACK);
    }

    /**
     * NGRectangle supports opaque regions
     */
    @Test
    public void testSupportsOpaqueRegions() {
        assertTrue(r.supportsOpaqueRegions());
    }

    /**
     * The default rectangle with a fill and size should have an opaque region
     */
    @Test
    public void testHasOpaqueRegion() {
        assertTrue(r.hasOpaqueRegion());
    }

    /**
     * If there's no fill, there is no opaque region because
     * we don't yet support strokes as being part of our
     * opaque region computation.
     */
    @Test
    public void testHasOpaqueRegion_NoFill() {
        r.setFillPaint(null);
        assertFalse(r.hasOpaqueRegion());
    }

    /**
     * If we have no width, we won't have any opaque region.
     */
    @Test
    public void testHasOpaqueRegion_NoWidth() {
        r.updateRectangle(0, 0, 0, 100, 0, 0);
        assertFalse(r.hasOpaqueRegion());
    }

    /**
     * If we have no height, we won't have any opaque region.
     */
    @Test
    public void testHasOpaqueRegion_NoHeight() {
        r.updateRectangle(0, 0, 100, 0, 0, 0);
        assertFalse(r.hasOpaqueRegion());
    }

    /**
     * In this case we still compute opaque insets,
     * based on the same logic as we'd use with
     * an ellipse.
     */
    @Test
    public void testHasOpaqueRegion_ArcWidthSoBig() {
        r.updateRectangle(0, 0, 100, 100, 100, 100);
        assertTrue(r.hasOpaqueRegion());
    }

    @Test
    public void computeOpaqueRegion_NoArc() {
        assertEquals(new RectBounds(0, 0, 100, 100), r.computeOpaqueRegion(new RectBounds()));
    }

    class NGRectangleMock extends NGRectangle {
        boolean opaqueRegionRecomputed = false;

        @Override
        protected boolean hasOpaqueRegion() {
            opaqueRegionRecomputed = true;
            return super.hasOpaqueRegion();
        }

        @Override
        protected RectBounds computeOpaqueRegion(RectBounds opaqueRegion) {
            opaqueRegionRecomputed = true;
            return super.computeOpaqueRegion(opaqueRegion);
        }
    }
}
