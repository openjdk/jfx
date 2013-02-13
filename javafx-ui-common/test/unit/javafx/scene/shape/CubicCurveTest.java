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

import javafx.scene.NodeTest;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import static org.junit.Assert.*;

public class CubicCurveTest {

    @Test
    public void testFullConstructor() {
        final CubicCurve curve = new CubicCurve(1, 2, 3, 4, 5, 6, 7, 8);
        assertEquals(1, curve.getStartX(), 0.00001);
        assertEquals(2, curve.getStartY(), 0.00001);
        assertEquals(3, curve.getControlX1(), 0.00001);
        assertEquals(4, curve.getControlY1(), 0.00001);
        assertEquals(5, curve.getControlX2(), 0.00001);
        assertEquals(6, curve.getControlY2(), 0.00001);
        assertEquals(7, curve.getEndX(), 0.00001);
        assertEquals(8, curve.getEndY(), 0.00001);
    }

    @Test
    public void testPropertyPropagation_visible() throws Exception {
        final CubicCurve node = new CubicCurve();
        NodeTest.testBooleanPropertyPropagation(node, "visible", false, true);
    }

    @Test
    public void testPropertyPropagation_startX() throws Exception {
        final CubicCurve node = new CubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "startX", "x1", 100, 200);
    }

    @Test
    public void testPropertyPropagation_startY() throws Exception {
        final CubicCurve node = new CubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "startY", "y1", 100, 200);
    }

    @Test
    public void testPropertyPropagation_controlX1() throws Exception {
        final CubicCurve node = new CubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "controlX1", "ctrlX1", 100, 200);
    }

    @Test
    public void testPropertyPropagation_controlY1() throws Exception {
        final CubicCurve node = new CubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "controlY1", "ctrlY1", 100, 200);
    }

    @Test
    public void testPropertyPropagation_controlX2() throws Exception {
        final CubicCurve node = new CubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "controlX2", "ctrlX2", 100, 200);
    }

    @Test
    public void testPropertyPropagation_controlY2() throws Exception {
        final CubicCurve node = new CubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "controlY2", "ctrlY2", 100, 200);
    }

    @Test
    public void testPropertyPropagation_endX() throws Exception {
        final CubicCurve node = new CubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "endX", "x2", 100, 200);
    }

    @Test
    public void testPropertyPropagation_endY() throws Exception {
        final CubicCurve node = new CubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "endY", "y2", 100, 200);
    }

    @Test public void testBoundPropertySync_startX() throws Exception {
        NodeTest.assertDoublePropertySynced(new CubicCurve(),
                "startX", "x1", 50.0);
    }

    @Test public void testBoundPropertySync_startY() throws Exception {
        NodeTest.assertDoublePropertySynced(new CubicCurve(),
                "startY", "y1", 50.0);
    }

    @Test public void testBoundPropertySync_controlX1() throws Exception {
        NodeTest.assertDoublePropertySynced(new CubicCurve(),
                "controlX1", "ctrlX1", 100.0);
    }

    @Test public void testBoundPropertySync_controlY1() throws Exception {
        NodeTest.assertDoublePropertySynced(new CubicCurve(),
                "controlY1", "ctrlY1", 100.0);
    }

    @Test public void testBoundPropertySync_controlX2() throws Exception {
        NodeTest.assertDoublePropertySynced(new CubicCurve(),
                "controlX2", "ctrlX2", 200.0);
    }

    @Test public void testBoundPropertySync_controlY2() throws Exception {
        NodeTest.assertDoublePropertySynced(new CubicCurve(),
                "controlY2", "ctrlY2", 123.0);
    }

    @Test public void testBoundPropertySync_endX() throws Exception {
        NodeTest.assertDoublePropertySynced(new CubicCurve(),
                "endX", "x2", 300.0);
    }

    @Test public void testBoundPropertySync_endY() throws Exception {
        NodeTest.assertDoublePropertySynced(new CubicCurve(),
                "endY", "y2", 300.0);
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new CubicCurve().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }
}
