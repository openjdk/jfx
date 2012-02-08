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
package javafx.scene.shape;

import static com.sun.javafx.test.TestHelper.assertSimilar;
import static com.sun.javafx.test.TestHelper.box;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BoundsTest {

    /***************************************************************************
     *                                                                         *
     *                      Simple Bounds Sanity Tests                         *
     *                                                                         *
     *  These are simple tests that basic geometry works. These are simple     *
     *  tests for the basic primitives in the scene graph to make sure that    *
     *  they all compute their geometric bounds properly.                      *
     *                                                                         *
    /**************************************************************************/
/*
    @Test
    public void testBoundsForArc() {
        Arc arc = new Arc(0, 0, 25, 25, 90, 90);
        arc.setType(ArcType.ROUND);

        assertEquals(box(-25, -25, 25, 25), arc.getBoundsInLocal());
        assertEquals(arc.getBoundsInLocal(), arc.getLayoutBounds());
        assertEquals(arc.getBoundsInLocal(), arc.getBoundsInParent());
        
        arc.setStroke(Color.BLACK);
        assertSimilar(box(-26, -26, 27, 27), arc.getBoundsInLocal());
        assertEquals(arc.getBoundsInLocal(), arc.getLayoutBounds());
        assertEquals(arc.getBoundsInLocal(), arc.getBoundsInParent());
    }

    public @Test void testBoundsForCircle() {
        Circle circle = new Circle(50);
        
        assertEquals(box(-50, -50, 100, 100), circle.getBoundsInLocal());
        assertEquals(circle.getBoundsInLocal(), circle.getLayoutBounds());
        assertEquals(circle.getBoundsInLocal(), circle.getBoundsInParent());
        
        circle.setStroke(Color.BLACK);
        assertSimilar(box(-51, -51, 102, 102), circle.getBoundsInLocal());
        assertEquals(circle.getBoundsInLocal(), circle.getLayoutBounds());
        assertEquals(circle.getBoundsInLocal(), circle.getBoundsInParent());
    }
    
    public @Test void testBoundsForCubicCurve() {
        CubicCurve cubic = new CubicCurve();
        cubic.setStartX(0);
        cubic.setStartY(50);
        cubic.setControlX1(25);
        cubic.setControlX2(75);
        cubic.setControlY1(0);
        cubic.setControlY2(100);
        cubic.setEndX(100);
        cubic.setEndY(50);

        assertSimilar(box(0, 36, 100, 28), cubic.getBoundsInLocal());
        assertEquals(cubic.getBoundsInLocal(), cubic.getBoundsInParent());
    }

    public @Test void testBoundsForEllipse() {
        Ellipse ellipse = new Ellipse(50, 100);

        assertEquals(box(-50, -100, 100, 200), ellipse.getBoundsInLocal());
        assertEquals(ellipse.getBoundsInLocal(), ellipse.getLayoutBounds());
        assertEquals(ellipse.getBoundsInLocal(), ellipse.getBoundsInParent());
        
        ellipse.setStroke(Color.BLACK);
        assertSimilar(box(-51, -101, 102, 202), ellipse.getBoundsInLocal());
        assertEquals(ellipse.getBoundsInLocal(), ellipse.getLayoutBounds());
        assertEquals(ellipse.getBoundsInLocal(), ellipse.getBoundsInParent());
    }

    public @Test void testBoundsForLine() {
        Line line = new Line(-10, -10, 10, 10);

        assertSimilar(box(-11, -11, 22, 22), line.getBoundsInLocal());
        assertEquals(line.getBoundsInLocal(), line.getLayoutBounds());
        assertEquals(line.getBoundsInLocal(), line.getBoundsInParent());
    }
    */

    public @Test void testBoundsForPath() {
        Path path = new Path();
        path.getElements().add(new MoveTo(10, 50));
        path.getElements().add(new HLineTo(70));
        path.getElements().add(new QuadCurveTo(100, 0, 120, 60));
        path.getElements().add(new LineTo(175, 55));
        path.getElements().add(new ArcTo(100, 100, 0, 10, 50, false, true));

        assertSimilar(box(9, 26, 167, 71), path.getBoundsInLocal());
        assertEquals(path.getBoundsInLocal(), path.getBoundsInParent());
    }
/*
    public @Test void testBoundsForPolygon() {
        Polygon polygon = new Polygon(new double[] {0,0,20,10,10,20});

        assertEquals(box(0, 0, 20, 20), polygon.getBoundsInLocal());
        assertEquals(polygon.getBoundsInLocal(), polygon.getLayoutBounds());
        assertEquals(polygon.getBoundsInLocal(), polygon.getBoundsInParent());
        
        polygon.setStroke(Color.BLACK);
        assertSimilar(box(-1, -1, 22, 22), polygon.getBoundsInLocal());
        assertEquals(polygon.getBoundsInLocal(), polygon.getLayoutBounds());
        assertEquals(polygon.getBoundsInLocal(), polygon.getBoundsInParent());
    }

    public @Test void testBoundsForPolyline() {
        Polyline polyline = new Polyline(new double[] {0,0,20,10,10,20});

        assertSimilar(box(-1, -1, 22, 22), polyline.getBoundsInLocal());
        assertEquals(polyline.getBoundsInLocal(), polyline.getLayoutBounds());
        assertEquals(polyline.getBoundsInLocal(), polyline.getBoundsInParent());
    }

    public @Test void testBoundsForQuadCurve() {
        QuadCurve quad = new QuadCurve(0, 50, 25, 0, 50, 50);

        assertEquals(box(0, 25, 50, 25), quad.getBoundsInLocal());
        assertEquals(quad.getBoundsInLocal(), quad.getBoundsInParent());
    }
    
    public @Test void testBoundsForRectangle() {
        Rectangle rect = new Rectangle(100, 100);
        
        assertEquals(box(0, 0, 100, 100), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
        
        rect.setX(50);
        rect.setY(50);
        assertEquals(box(50, 50, 100, 100), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
        
        rect.setStroke(Color.BLACK);
        assertSimilar(box(49, 49, 102, 102), rect.getBoundsInLocal());
        assertEquals(rect.getBoundsInLocal(), rect.getLayoutBounds());
        assertEquals(rect.getBoundsInLocal(), rect.getBoundsInParent());
    }
    
    // SVGPath
    
    // we don't have a default impl of this is tests yet, so disabling for
    // now
    /*
    public function testBoundsForText() {
text = Text {
            100
 x            100
 y            TextOrigin.TOP
 textOrigin            Font font { 42 size }
            "Testing"
 content        }
        
        // this is hard to test. All I'm going to do is a brief sanity check
        // to make sure the bounds seem somewhat reasonable.
        
        // check that the difference is less than 5 pixels here
        assertTrue(Math.abs(100 - text.getBoundsInLocal.minX) < 5);
        assertTrue(Math.abs(100 - text.getBoundsInLocal.minY) < 5);
        assertTrue(text.getBoundsInLocal.width > 120);
        assertTrue(text.getBoundsInLocal.height > 40);
        assertEquals(text.getBoundsInLocal, text.getLayoutBounds);
        assertEquals(text.getBoundsInLocal, text.getBoundsInParent);
    }*/
}
