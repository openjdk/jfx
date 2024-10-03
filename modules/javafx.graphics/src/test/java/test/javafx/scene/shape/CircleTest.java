/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.shape;

import test.com.sun.javafx.scene.shape.StubCircleHelper;
import com.sun.javafx.sg.prism.NGCircle;
import com.sun.javafx.sg.prism.NGNode;
import test.com.sun.javafx.test.TestHelper;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import test.javafx.scene.NodeTest;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static test.com.sun.javafx.test.TestHelper.assertSimilar;


public class CircleTest {

    @Test
    public void testPropertyPropagation_visible() throws Exception {
        final Circle node = new StubCircle();
        NodeTest.testBooleanPropertyPropagation(node, "visible", false, true);
    }

    @Test
    public void testPropertyPropagation_centerX() throws Exception {
        final Circle node = new StubCircle();
        NodeTest.testDoublePropertyPropagation(node, "centerX", 100, 200);
    }

    @Test
    public void testPropertyPropagation_centerY() throws Exception {
        final Circle node = new StubCircle();
        NodeTest.testDoublePropertyPropagation(node, "centerY", 100, 200);
    }

    @Test
    public void testPropertyPropagation_radius() throws Exception {
        final Circle node = new StubCircle();
        NodeTest.testDoublePropertyPropagation(node, "radius", 100, 200);
    }

    @Test
    public void testBoundPropertySync_centerX() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubCircle(100, 200, 50),
                "centerX", "centerX",
                350.0);
    }

    @Test
    public void testBoundPropertySync_centerY() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubCircle(100, 200, 50),
                "centerY", "centerY",
                250.0);
    }

    @Test
    public void testBoundPropertySync_radius() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubCircle(100, 200, 50),
                "radius", "radius",
                100.0);
    }

    @Test
    public void testTransformedBounds_rotation() {
        Circle c = new StubCircle(50, 100, 10, Color.RED);
        Bounds original = c.getBoundsInParent();
        c.setRotate(15);
        assertSimilar(original, c.getBoundsInParent());
    }

    @Test
    public void testTransformedBounds_rotation2() {
        final int centerX = 50;
        final int centerY = 200;
        Circle c = new StubCircle(centerX, centerY, 10, Color.RED);
        Bounds original = c.getBoundsInParent();
        // Using integer isosceles triangle of (38, 181, 181)
        Rotate r = new Rotate();
        final double angle = Math.asin((38.0/2)/181)*2;
        r.setAngle(Math.toDegrees(angle));
        r.setPivotX(centerX + 181);
        r.setPivotY(centerY);
        c.getTransforms().add(r);

        final double centerXDelta = Math.cos((Math.PI - angle)/2) * 38;
        final double centerYDelta = - (Math.sin((Math.PI - angle)/2) * 38);

        assertSimilar(TestHelper.box(original.getMinX() + centerXDelta, original.getMinY() + centerYDelta,
                original.getWidth(), original.getHeight()), c.getBoundsInParent());
    }

    @Test
    public void testTransformedBounds_translate() {
        Circle c = new StubCircle(50, 100, 10, Color.RED);
        Bounds original = c.getBoundsInParent();
        c.setTranslateX(10);
        c.setTranslateY(20);
        assertSimilar(TestHelper.box(original.getMinX() + 10, original.getMinY() + 20,
                original.getWidth(), original.getHeight()), c.getBoundsInParent());
    }


    @Test
    public void testTransformedBounds_scale() {
        Circle c = new StubCircle(50, 100, 10, Color.RED);
        double scalePivotX = (c.getCenterX() + c.getRadius()) / 2;
        double scalePivotY = (c.getCenterY() + c.getRadius()) / 2;
        Bounds original = c.getBoundsInParent();
        Scale s = new Scale(2.0, 1.5, scalePivotX, scalePivotY);
        c.getTransforms().setAll(s);
        assertSimilar(TestHelper.box(2 * original.getMinX() - scalePivotX, 1.5 * original.getMinY() - 0.5 * scalePivotY,
                2 * original.getWidth(), 1.5 * original.getHeight()), c.getBoundsInParent());
    }

    @Test
    public void toStringShouldReturnNonEmptyString() {
        String s = new StubCircle().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    public static final class StubCircle extends Circle {
        static {
            StubCircleHelper.setStubCircleAccessor(new StubCircleHelper.StubCircleAccessor() {
                @Override
                public NGNode doCreatePeer(Node node) {
                    return ((StubCircle) node).doCreatePeer();
                }
            });
        }

        {
            // To initialize the class helper at the begining each constructor of this class
            StubCircleHelper.initHelper(this);
        }
        public StubCircle() {
            super();
        }

        public StubCircle(double radius) {
            super(radius);
        }

        public StubCircle(double centerX, double centerY, double radius) {
            super(centerX, centerY, radius);
        }

        public StubCircle(double centerX, double centerY, double radius, Paint fill) {
            super(centerX, centerY, radius, fill);
        }

        private NGNode doCreatePeer() {
            return new StubNGCircle();
        }
    }

    public static final class StubNGCircle extends NGCircle {
        private float centerX;
        private float centerY;
        private float radius;

        public float getCenterX() {return centerX;}
        public float getCenterY() {return centerY;}
        public float getRadius() {return radius;}

        @Override
        public void updateCircle(float cx, float cy, float radius) {
            this.centerX = cx;
            this.centerY = cy;
            this.radius = radius;
        }
    }
}
