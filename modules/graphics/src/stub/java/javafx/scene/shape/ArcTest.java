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

import com.sun.javafx.sg.prism.NGArc;
import com.sun.javafx.sg.prism.NGNode;
import javafx.scene.NodeTest;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArcTest {

    @Test public void testPropertyPropagation_visible() throws Exception {
        final Arc node = new StubArc();
        NodeTest.testBooleanPropertyPropagation(node, "visible", false, true);
    }

    @Test public void testPropertyPropagation_centerX() throws Exception {
        final Arc node = new StubArc();
        NodeTest.testDoublePropertyPropagation(node, "centerX", 100, 200);
    }

    @Test public void testPropertyPropagation_centerY() throws Exception {
        final Arc node = new StubArc();
        NodeTest.testDoublePropertyPropagation(node, "centerY", 100, 200);
    }

    @Test public void testPropertyPropagation_radiusX() throws Exception {
        final Arc node = new StubArc();
        NodeTest.testDoublePropertyPropagation(node, "radiusX", 100, 200);
    }

    @Test public void testPropertyPropagation_radiusY() throws Exception {
        final Arc node = new StubArc();
        NodeTest.testDoublePropertyPropagation(node, "radiusY", 100, 200);
    }

    @Test public void testPropertyPropagation_startAngle() throws Exception {
        final Arc node = new StubArc();
        NodeTest.testDoublePropertyPropagation(node, "startAngle", "angleStart", 30, 60);
    }

    @Test public void testPropertyPropagation_length() throws Exception {
        final Arc node = new StubArc();
        NodeTest.testDoublePropertyPropagation(node, "length", "angleExtent", 30, 45);
    }

    @Test public void testBoundPropertySync_length() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubArc(10.0, 10.0, 100.0, 100.0, 0.0, 0.0),
                "length", "angleExtent", 100.0);
    }

    @Test public void testBoundProperySync_startAngle() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubArc(10.0, 10.0, 100.0, 100.0, 0.0, 0.0),
                "startAngle", "angleStart", 270.0);
    }

    @Test public void testBoundPropertySync_radiusY() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubArc(10.0, 10.0, 100.0, 100.0, 0.0, 0.0),
                "radiusY", "radiusY", 200.0);
    }

    @Test public void testBoundPropertySync_radiusX() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubArc(10.0, 10.0, 100.0, 100.0, 0.0, 0.0),
                "radiusX", "radiusX", 150.0);
    }

    @Test public void testBoundPropertySync_centerY() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubArc(10.0, 10.0, 100.0, 100.0, 0.0, 0.0),
                "centerY", "centerY", 250.0);
    }

    @Test public void testBoundPropertySync_centerX() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubArc(10.0, 10.0, 100.0, 100.0, 0.0, 0.0),
                "centerX", "centerX", 350.0);
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new StubArc().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test public void testNullType() {
        // null type should not throw NPE
        Arc arc = new StubArc(10.0, 10.0, 100.0, 100.0, 0.0, 0.0);
        arc.setType(null);
        assertNull(arc.getType());
        assertNull(arc.typeProperty().get());
    }

    public final class StubArc extends Arc {
        public StubArc() {
            super();
        }

        public StubArc(double centerX, double centerY, double radiusX, double radiusY, double startAngle, double length) {
            super(centerX, centerY, radiusX, radiusY, startAngle, length);
        }

        @Override
        protected NGNode impl_createPeer() {
            return new StubNGArc();
        }
    }

    public final class StubNGArc extends NGArc {
        private float cx, cy, rx, ry, start, extent;
        private ArcType type;

        @Override
        public void updateArc(float cx, float cy, float rx, float ry, float start, float extent, ArcType type) {
            super.updateArc(cx, cy, rx, ry, start, extent, type);
            this.cx = cx;
            this.cy = cy;
            this.rx = rx;
            this.ry = ry;
            this.start = start;
            this.extent = extent;
            this.type = type;
        }

        // all called via reflection
        public float getAngleExtent() {return extent;}
        public float getAngleStart() {return start;}
        public float getCenterX() {return cx;}
        public float getCenterY() {return cy;}
        public float getRadiusX() {return rx;}
        public float getRadiusY() {return ry;}
        public ArcType getArcType() { return type;}
    }
}
