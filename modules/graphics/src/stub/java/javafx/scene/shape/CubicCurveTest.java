/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.sg.prism.NGCubicCurve;
import com.sun.javafx.sg.prism.NGNode;
import javafx.scene.NodeTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class CubicCurveTest {

    @Test
    public void testFullConstructor() {
        final CubicCurve curve = new StubCubicCurve(1, 2, 3, 4, 5, 6, 7, 8);
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
        final CubicCurve node = new StubCubicCurve();
        NodeTest.testBooleanPropertyPropagation(node, "visible", false, true);
    }

    @Test
    public void testPropertyPropagation_startX() throws Exception {
        final CubicCurve node = new StubCubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "startX", "x1", 100, 200);
    }

    @Test
    public void testPropertyPropagation_startY() throws Exception {
        final CubicCurve node = new StubCubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "startY", "y1", 100, 200);
    }

    @Test
    public void testPropertyPropagation_controlX1() throws Exception {
        final CubicCurve node = new StubCubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "controlX1", "ctrlX1", 100, 200);
    }

    @Test
    public void testPropertyPropagation_controlY1() throws Exception {
        final CubicCurve node = new StubCubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "controlY1", "ctrlY1", 100, 200);
    }

    @Test
    public void testPropertyPropagation_controlX2() throws Exception {
        final CubicCurve node = new StubCubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "controlX2", "ctrlX2", 100, 200);
    }

    @Test
    public void testPropertyPropagation_controlY2() throws Exception {
        final CubicCurve node = new StubCubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "controlY2", "ctrlY2", 100, 200);
    }

    @Test
    public void testPropertyPropagation_endX() throws Exception {
        final CubicCurve node = new StubCubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "endX", "x2", 100, 200);
    }

    @Test
    public void testPropertyPropagation_endY() throws Exception {
        final CubicCurve node = new StubCubicCurve();
        NodeTest.testDoublePropertyPropagation(node, "endY", "y2", 100, 200);
    }

    @Test public void testBoundPropertySync_startX() throws Exception {
        NodeTest.assertDoublePropertySynced(new StubCubicCurve(),
                "startX", "x1", 50.0);
    }

    @Test public void testBoundPropertySync_startY() throws Exception {
        NodeTest.assertDoublePropertySynced(new StubCubicCurve(),
                "startY", "y1", 50.0);
    }

    @Test public void testBoundPropertySync_controlX1() throws Exception {
        NodeTest.assertDoublePropertySynced(new StubCubicCurve(),
                "controlX1", "ctrlX1", 100.0);
    }

    @Test public void testBoundPropertySync_controlY1() throws Exception {
        NodeTest.assertDoublePropertySynced(new StubCubicCurve(),
                "controlY1", "ctrlY1", 100.0);
    }

    @Test public void testBoundPropertySync_controlX2() throws Exception {
        NodeTest.assertDoublePropertySynced(new StubCubicCurve(),
                "controlX2", "ctrlX2", 200.0);
    }

    @Test public void testBoundPropertySync_controlY2() throws Exception {
        NodeTest.assertDoublePropertySynced(new StubCubicCurve(),
                "controlY2", "ctrlY2", 123.0);
    }

    @Test public void testBoundPropertySync_endX() throws Exception {
        NodeTest.assertDoublePropertySynced(new StubCubicCurve(),
                "endX", "x2", 300.0);
    }

    @Test public void testBoundPropertySync_endY() throws Exception {
        NodeTest.assertDoublePropertySynced(new StubCubicCurve(),
                "endY", "y2", 300.0);
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new StubCubicCurve().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    public class StubCubicCurve extends CubicCurve {
        public StubCubicCurve() {
            super();
        }

        public StubCubicCurve(double startX, double startY, double controlX1, double controlY1, double controlX2, double controlY2, double endX, double endY) {
            super(startX, startY, controlX1, controlY1, controlX2, controlY2, endX, endY);
        }

        @Override
        protected NGNode impl_createPeer() {
            return new StubNGCubicCurve();
        }
    }

    public class StubNGCubicCurve extends NGCubicCurve {
        private float x1;
        private float y1;
        private float x2;
        private float y2;
        private float ctrlX1;
        private float ctrlY1;
        private float ctrlX2;
        private float ctrlY2;

        public float getCtrlX1() {return ctrlX1;}
        public float getCtrlX2() {return ctrlX2;}
        public float getCtrlY1() {return ctrlY1;}
        public float getCtrlY2() {return ctrlY2;}
        public float getX1() {return x1;}
        public float getX2() {return x2;}
        public float getY1() {return y1;}
        public float getY2() {return y2;}

        @Override
        public void updateCubicCurve(float x1, float y1, float x2, float y2, float ctrlx1, float ctrly1, float ctrlx2, float ctrly2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.ctrlX1 = ctrlx1;
            this.ctrlY1 = ctrly1;
            this.ctrlX2 = ctrlx2;
            this.ctrlY2 = ctrly2;
        }
    }

}
