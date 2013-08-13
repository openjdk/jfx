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

import com.sun.javafx.sg.prism.NGEllipse;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.test.TestHelper;
import javafx.geometry.Bounds;
import javafx.scene.NodeTest;
import org.junit.Test;

import static com.sun.javafx.test.TestHelper.assertSimilar;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;


public class EllipseTest {

    @Test public void testPropertyPropagation_visible() throws Exception {
        final Ellipse node = new StubEllipse();
        NodeTest.testBooleanPropertyPropagation(node, "visible", false, true);
    }

    @Test public void testPropertyPropagation_centerX() throws Exception {
        final Ellipse node = new StubEllipse();
        NodeTest.testDoublePropertyPropagation(node, "centerX", 100, 200);
    }

    @Test public void testPropertyPropagation_centerY() throws Exception {
        final Ellipse node = new StubEllipse();
        NodeTest.testDoublePropertyPropagation(node, "centerY", 100, 200);
    }

    @Test public void testPropertyPropagation_radiusX() throws Exception {
        final Ellipse node = new StubEllipse();
        NodeTest.testDoublePropertyPropagation(node, "radiusX", 100, 200);
    }

    @Test public void testPropertyPropagation_radiusY() throws Exception {
        final Ellipse node = new StubEllipse();
        NodeTest.testDoublePropertyPropagation(node, "radiusY", 100, 200);
    }

    @Test public void testBoundPropertySync_radiusX() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubEllipse(300.0, 300.0, 100.0, 100.0),
                "radiusX", "radiusX", 150.0);
    }

    @Test public void testBoundPropertySync_radiusY() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubEllipse(300.0, 300.0, 100.0, 100.0),
                "radiusY", "radiusY", 150.0);
    }

    @Test public void testBoundPropertySync_centerX() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubEllipse(300.0, 300.0, 100.0, 100.0),
                "centerX", "centerX", 10.0);
    }

    @Test public void testBoundPropertySync_centerY() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubEllipse(300.0, 300.0, 100.0, 100.0),
                "centerY", "centerY", 10.0);
    }

    @Test
    public void testTransformedBounds_rotation() {
        Ellipse e = new StubEllipse(50, 100, 10, 20);
        Bounds original = e.getBoundsInParent();
        e.setRotate(90);
        assertSimilar(TestHelper.box(30, 90,
                original.getHeight(), original.getWidth()), e.getBoundsInParent());
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new StubEllipse().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    public class StubEllipse extends Ellipse {
        public StubEllipse() {
            super();
        }

        public StubEllipse(double radiusX, double radiusY) {
            super(radiusX, radiusY);
        }

        public StubEllipse(double centerX, double centerY, double radiusX, double radiusY) {
            super(centerX, centerY, radiusX, radiusY);
        }

        @Override
        protected NGNode impl_createPeer() {
            return new StubNGEllipse();
        }
    }

    public class StubNGEllipse extends NGEllipse {
        private float centerX;
        private float centerY;
        private float radiusX;
        private float radiusY;

        public float getCenterX() {return centerX;}
        public float getCenterY() {return centerY;}
        public float getRadiusX() {return radiusX;}
        public float getRadiusY() {return radiusY;}

        @Override
        public void updateEllipse(float cx, float cy, float rx, float ry) {
            this.centerX = cx;
            this.centerY = cy;
            this.radiusX = rx;
            this.radiusY = ry;
        }
    }

}
