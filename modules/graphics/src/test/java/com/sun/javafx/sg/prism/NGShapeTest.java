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

import java.util.Arrays;
import com.sun.javafx.geom.Ellipse2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.LinearGradient;
import com.sun.prism.paint.Stop;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class NGShapeTest extends NGTestBase {
    private NGShape shape;

    @Before
    public void setup() {
        shape = new NGShape() {
            @Override
            public Shape getShape() {
                return new Ellipse2D(10, 10, 10, 10);
            }

            @Override
            protected boolean supportsOpaqueRegions() {
                return true;
            }

            @Override
            protected RectBounds computeOpaqueRegion(RectBounds opaqueRegion) {
                // For the sake of the tests in this class we actually don't care
                // what the opaque region is, so I just make something up
                opaqueRegion.setBounds(0, 0, 20, 20);
                return opaqueRegion;
            }
        };
        shape.setDrawPaint(Color.WHITE);
        shape.setFillPaint(Color.BLACK);
    }

    @Test
    public void hasOpaqueRegionReturnsFalseIfModeIsStroke() {
        shape.setMode(NGShape.Mode.STROKE);
        assertFalse(shape.hasOpaqueRegion());
    }

    @Test
    public void hasOpaqueRegionReturnsFalseIfModeIsEmpty() {
        shape.setMode(NGShape.Mode.EMPTY);
        assertFalse(shape.hasOpaqueRegion());
    }

    @Test
    public void hasOpaqueRegionReturnsFalseIfFillPaintIsNull() {
        shape.setFillPaint(null);
        assertFalse(shape.hasOpaqueRegion());
    }

    @Test
    public void hasOpaqueRegionReturnsFalseIfFillPaintIsNotOpaque() {
        shape.setFillPaint(new LinearGradient(0, 0, 1, 1, BaseTransform.IDENTITY_TRANSFORM, true, 0, Arrays.asList(
                new Stop(Color.BLACK, 0), new Stop(Color.TRANSPARENT, 1))));
        assertFalse(shape.hasOpaqueRegion());
    }

    @Test
    public void hasOpaqueRegionReturnsTrueIfModeIsSTROKE_FILE() {
        shape.setMode(NGShape.Mode.STROKE_FILL);
        assertTrue(shape.hasOpaqueRegion());
    }

    @Test
    public void hasOpaqueRegionReturnsTrueIfModeIsFILL() {
        assertTrue(shape.hasOpaqueRegion());
    }

    @Test
    public void getOpaqueRegionChangesWhenFillChanged() {
        RectBounds or = shape.getOpaqueRegion();
        assertNotNull(or);

        shape.setFillPaint(null);
        assertNull(shape.getOpaqueRegion());

        shape.setFillPaint(Color.BLACK);
        assertNotNull(shape.getOpaqueRegion());
        assertEquals(or, shape.getOpaqueRegion());
    }

    @Test
    public void getOpaqueRegionChangesWhenModeChanged() {
        RectBounds or = shape.getOpaqueRegion();
        assertNotNull(or);

        shape.setMode(NGShape.Mode.EMPTY);
        assertNull(shape.getOpaqueRegion());

        shape.setMode(NGShape.Mode.FILL);
        assertNotNull(shape.getOpaqueRegion());
        assertEquals(or, shape.getOpaqueRegion());

        shape.setMode(NGShape.Mode.STROKE);
        assertNull(shape.getOpaqueRegion());

        shape.setMode(NGShape.Mode.STROKE_FILL);
        assertNotNull(shape.getOpaqueRegion());
        assertEquals(or, shape.getOpaqueRegion());
    }
}
