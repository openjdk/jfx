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

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGRectangle;
import com.sun.javafx.test.TestHelper;
import javafx.geometry.Bounds;
import javafx.scene.NodeTest;
import javafx.scene.paint.Paint;
import org.junit.Test;

import static com.sun.javafx.test.TestHelper.assertSimilar;
import static org.junit.Assert.*;

public class RectangleTest {

    @Test public void testPropertyPropagation_visible() throws Exception {
        final Rectangle node = new StubRectangle();
        NodeTest.testBooleanPropertyPropagation(node, "visible", false, true);
    }

    @Test public void testPropertyPropagation_x() throws Exception {
        final Rectangle node = new StubRectangle();
        NodeTest.testDoublePropertyPropagation(node, "x", 100, 200);
    }

    @Test public void testPropertyPropagation_y() throws Exception {
        final Rectangle node = new StubRectangle();
        NodeTest.testDoublePropertyPropagation(node, "y", 100, 200);
    }

    @Test public void testPropertyPropagation_width() throws Exception {
        final Rectangle node = new StubRectangle();
        NodeTest.testDoublePropertyPropagation(node, "width", 100, 200);
    }

    @Test public void testPropertyPropagation_height() throws Exception {
        final Rectangle node = new StubRectangle();
        NodeTest.testDoublePropertyPropagation(node, "height", 100, 200);
    }

    @Test public void testPropertyPropagation_arcWidth() throws Exception {
        final Rectangle node = new StubRectangle();
        NodeTest.testDoublePropertyPropagation(node, "arcWidth", 100, 200);
    }

    @Test public void testPropertyPropagation_arcHeight() throws Exception {
        final Rectangle node = new StubRectangle();
        NodeTest.testDoublePropertyPropagation(node, "arcHeight", 100, 200);
    }

    @Test public void testBoundPropertySync_X() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubRectangle(200.0, 100.0),
                "x", "x", 10.0);
    }

    @Test public void testBoundPropertySync_Y() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubRectangle(200.0, 100.0),
                "y", "y", 20.0);
    }

    @Test public void testBoundPropertySync_Width() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubRectangle(200.0, 100.0),
                "width", "width", 300.0);
    }

    @Test public void testBoundPropertySync_Height() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubRectangle(200.0, 100.0),
                "height", "height", 200.0);
    }

    @Test public void testBoundPropertySync_ArcWidth() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubRectangle(200.0, 100.0),
                "arcWidth", "arcWidth", 10.0);
    }

    @Test public void testBoundPropertySync_ArcHeight() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubRectangle(200.0, 100.0),
                "arcHeight", "arcHeight", 30.0);
    }


    @Test
    public void testTransformedBounds_rotation() {
        Rectangle r = new StubRectangle(50, 100, 10, 20);
        r.setArcHeight(5);
        r.setArcWidth(10);
        Bounds original = r.getBoundsInParent();
        r.setRotate(90);
        assertSimilar(TestHelper.box(45, 105,
                original.getHeight(), original.getWidth()), r.getBoundsInParent());
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new StubRectangle().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    public static final class StubRectangle extends Rectangle {
        public StubRectangle() {
            super();
        }

        public StubRectangle(double width, double height) {
            super(width, height);
        }

        public StubRectangle(double width, double height, Paint fill) {
            super(width, height, fill);
        }

        public StubRectangle(double x, double y, double width, double height) {
            super(x, y, width, height);
        }

        @Override
        protected NGNode impl_createPeer() {
            return new StubNGRectangle();
        }
    }

    public static final class StubNGRectangle extends NGRectangle {
        // for tests
        private float x;
        private float y;
        private float width;
        private float height;
        private float arcWidth;
        private float arcHeight;

        public void setX(float x) {this.x = x;}
        public void setY(float y) {this.y = y;}
        public void setWidth(float width) {this.width = width;}
        public void setHeight(float height) {this.height = height;}
        public void setArcWidth(float arcWidth) {this.arcWidth = arcWidth;}
        public void setArcHeight(float arcHeight) {this.arcHeight = arcHeight;}
        public float getArcHeight() {return arcHeight;}
        public float getArcWidth() {return arcWidth;}
        public float getHeight() {return height;}
        public float getWidth() {return width;}
        public float getX() {return x;}
        public float getY() {return y;}

        @Override
        public void updateRectangle(float x, float y, float width, float height, float arcWidth, float arcHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.arcWidth = arcWidth;
            this.arcHeight = arcHeight;
        }

        private BaseTransform transformMatrix;
        @Override
        public void setTransformMatrix(BaseTransform tx) {
            super.setTransformMatrix(tx);
            this.transformMatrix = tx;
        }

        public BaseTransform getTransformMatrix() {
            return transformMatrix;
        }
    }
}
