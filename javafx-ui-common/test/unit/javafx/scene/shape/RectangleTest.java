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

package javafx.scene.shape;

import com.sun.javafx.test.TestHelper;
import javafx.geometry.Bounds;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import javafx.scene.NodeTest;
import javafx.scene.paint.Color;
import static org.junit.Assert.*;
import static com.sun.javafx.test.TestHelper.*;

import org.junit.Test;

public class RectangleTest {

    @Test public void testPropertyPropagation_visible() throws Exception {
        final Rectangle node = new Rectangle();
        NodeTest.testBooleanPropertyPropagation(node, "visible", false, true);
    }

    @Test public void testPropertyPropagation_x() throws Exception {
        final Rectangle node = new Rectangle();
        NodeTest.testDoublePropertyPropagation(node, "x", 100, 200);
    }

    @Test public void testPropertyPropagation_y() throws Exception {
        final Rectangle node = new Rectangle();
        NodeTest.testDoublePropertyPropagation(node, "y", 100, 200);
    }

    @Test public void testPropertyPropagation_width() throws Exception {
        final Rectangle node = new Rectangle();
        NodeTest.testDoublePropertyPropagation(node, "width", 100, 200);
    }

    @Test public void testPropertyPropagation_height() throws Exception {
        final Rectangle node = new Rectangle();
        NodeTest.testDoublePropertyPropagation(node, "height", 100, 200);
    }

    @Test public void testPropertyPropagation_arcWidth() throws Exception {
        final Rectangle node = new Rectangle();
        NodeTest.testDoublePropertyPropagation(node, "arcWidth", 100, 200);
    }

    @Test public void testPropertyPropagation_arcHeight() throws Exception {
        final Rectangle node = new Rectangle();
        NodeTest.testDoublePropertyPropagation(node, "arcHeight", 100, 200);
    }

    @Test public void testBuilder() {
        final Rectangle r = RectangleBuilder.create().x(23).y(17).arcHeight(10).height(25).opacity(0.5).build();
        assertEquals(23.0, r.getX(), 0);
        assertEquals(17.0, r.getY(), 0);
        assertEquals(10.0, r.getArcHeight(), 0);
        assertEquals(25.0, r.getHeight(), 0);
        assertEquals(0.5, r.getOpacity(), 0);
    }

    @Test public void testReuseBuilder() {
        final RectangleBuilder<?> fatPinkGhost = RectangleBuilder.create().height(10).width(200).fill(Color.PINK).opacity(0.5);
        final Rectangle r0 = fatPinkGhost.build();
        final Rectangle r1 = fatPinkGhost.build();
        final Rectangle r2 = new Rectangle();
        fatPinkGhost.applyTo(r2);
        final Rectangle r3 = new Rectangle();
        fatPinkGhost.applyTo(r3);
        for (Rectangle r : new Rectangle[] {r0, r1, r2, r3}) {
            assertEquals(10, r.getHeight(), 0);
            assertEquals(200, r.getWidth(), 0);
            assertSame(Color.PINK, r.getFill());
            assertEquals(0.5, r.getOpacity(), 0);
        }
    }

    @Test public void testBoundPropertySync_X() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Rectangle(200.0, 100.0),
                "x", "x", 10.0);
    }

    @Test public void testBoundPropertySync_Y() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Rectangle(200.0, 100.0),
                "y", "y", 20.0);
    }

    @Test public void testBoundPropertySync_Width() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Rectangle(200.0, 100.0),
                "width", "width", 300.0);
    }

    @Test public void testBoundPropertySync_Height() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Rectangle(200.0, 100.0),
                "height", "height", 200.0);
    }

    @Test public void testBoundPropertySync_ArcWidth() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Rectangle(200.0, 100.0),
                "arcWidth", "arcWidth", 10.0);
    }

    @Test public void testBoundPropertySync_ArcHeight() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Rectangle(200.0, 100.0),
                "arcHeight", "arcHeight", 30.0);
    }


    @Test
    public void testTransformedBounds_rotation() {
        Rectangle r = new Rectangle(50, 100, 10, 20);
        r.setArcHeight(5);
        r.setArcWidth(10);
        Bounds original = r.getBoundsInParent();
        r.setRotate(90);
        assertSimilar(TestHelper.box(45, 105,
                original.getHeight(), original.getWidth()), r.getBoundsInParent());
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new Rectangle().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }
}
