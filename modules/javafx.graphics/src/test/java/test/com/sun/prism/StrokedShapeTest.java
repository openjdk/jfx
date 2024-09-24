/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.prism;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.BasicStroke;
import com.sun.prism.BasicStrokeShim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class StrokedShapeTest {

    BasicStroke stroke;
    Path2D path;

    @BeforeEach
    public void setUp() {
        stroke = new BasicStroke();
        BasicStrokeShim.set_width(stroke, 10);
        path = new Path2D();
        path.moveTo(0, 0);
    }

    @Test
    public void lineTo() {
        path.lineTo(10, 0);
        assertBounds(-5, -5, 15, 5);
    }

    @Test
    public void lineTo_cap() {
        path.lineTo(3, 4);
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_SQUARE);
        assertBounds(-7, -7, 10, 11);
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_BUTT);
        assertBounds(-4, -3, 7, 7);
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_ROUND);
        assertBounds(-5, -5, 8, 9);
    }

    @Test
    public void quadTo() {
        path.quadTo(3, 4, 6, 0);
        assertBounds(-7, -7, 13, 7);
    }

    @Test
    public void quadTo_cap() {
        path.quadTo(3, 4, 6, 0);
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_SQUARE);
        assertBounds(-7, -7, 13, 7);
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_BUTT);
        assertBounds(-4, -3, 10, 7);
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_ROUND);
        assertBounds(-5, -5, 11, 7);
    }

    @Test
    public void curveTo() {
        path.curveTo(10, 0, 0, 10, 20, 10);
        assertBounds(-5, -5, 25, 15);

    }

    @Test
    public void curveTo_cap() {
        path.curveTo(3, 4, 6, 0, 9, 4);
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_SQUARE);
        assertBounds(-7, -7, 16, 11);
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_BUTT);
        assertBounds(-4, -3, 13, 7);
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_ROUND);
        assertBounds(-5, -5, 14, 9);
    }

    @Test
    public void lineLine_join() {
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_ROUND); // for easy computation
        path.lineTo(3, 4);
        path.lineTo(6, 0);
        BasicStrokeShim.set_join(stroke, BasicStroke.JOIN_MITER);
        assertBounds(-5, -5, 11, 12.33f);
        BasicStrokeShim.set_join(stroke, BasicStroke.JOIN_BEVEL);
        assertBounds(-5, -5, 11, 7);
        BasicStrokeShim.set_join(stroke, BasicStroke.JOIN_ROUND);
        assertBounds(-5, -5, 11, 9);
    }

    @Test
    public void lineQuad_join() {
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_ROUND); // for easy computation
        path.lineTo(3, 4);
        path.quadTo(6, 0, 10, 4);
        BasicStrokeShim.set_join(stroke, BasicStroke.JOIN_MITER);
        assertBounds(-5, -5, 15, 12.33f);
        BasicStrokeShim.set_join(stroke, BasicStroke.JOIN_BEVEL);
        assertBounds(-5, -5, 15, 9); //a better test here? (width accumulates more than join)
        BasicStrokeShim.set_join(stroke, BasicStroke.JOIN_ROUND);
        assertBounds(-5, -5, 15, 9);
    }


    @Test
    public void lineCurve_join() {
        BasicStrokeShim.set_cap(stroke, BasicStroke.CAP_ROUND); // for easy computation
        path.lineTo(3, 4);
        path.curveTo(6, 0, 0, 0, 10, 4);
        BasicStrokeShim.set_join(stroke, BasicStroke.JOIN_MITER);
        assertBounds(-5, -5, 15, 12.33f);
        BasicStrokeShim.set_join(stroke, BasicStroke.JOIN_BEVEL);
        assertBounds(-5, -5, 15, 9); //a better test here? (width accumulates more than join)
        BasicStrokeShim.set_join(stroke, BasicStroke.JOIN_ROUND);
        assertBounds(-5, -5, 15, 9);
    }

    private void assertBounds(float x0, float y0, float x1, float y1) {
        float[] bbox = new float[]{0, 0, 0, 0};
        stroke.accumulateShapeBounds(bbox, path, BaseTransform.IDENTITY_TRANSFORM);
        assertArrayEquals(new float[]{x0, y0, x1, y1}, bbox, 0.01f);
    }
}
