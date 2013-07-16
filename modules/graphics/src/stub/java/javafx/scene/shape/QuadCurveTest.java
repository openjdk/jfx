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

import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGQuadCurve;
import javafx.scene.NodeTest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;


public class QuadCurveTest {

    @Test public void testSetGetStartX() throws Exception {
        TestUtils.testDoublePropertyGetterSetter(new StubQuadCurve(), "startX", 123.2, 0.0);
    }

    @Test public void testSetGetStartY() throws Exception {
        TestUtils.testDoublePropertyGetterSetter(new StubQuadCurve(), "startY", 123.2, 0.0);
    }

    @Test public void testSetGetEndX() throws Exception {
        TestUtils.testDoublePropertyGetterSetter(new StubQuadCurve(), "endX", 123.2, 0.0);
    }

    @Test public void testSetGetEndY() throws Exception {
        TestUtils.testDoublePropertyGetterSetter(new StubQuadCurve(), "endY", 123.2, 0.0);
    }

    @Test public void testSetGetControlX() throws Exception {
        TestUtils.testDoublePropertyGetterSetter(new StubQuadCurve(), "controlX", 123.2, 0.0);
    }

    @Test public void testSetGetControlY() throws Exception {
        TestUtils.testDoublePropertyGetterSetter(new StubQuadCurve(), "controlY", 123.2, 0.0);
    } 
    
    @Test public void testPropertyPropagation_visible() throws Exception {
        NodeTest.testBooleanPropertyPropagation(new StubQuadCurve(), "visible", false, true);
    }

    @Test public void testBoundPropertySync_startX() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubQuadCurve(0.0 ,0.0, 10.0, 10.0, 100.0, 100.0),
                "startX", "x1", 30.0);
    }

    @Test public void testBoundPropertySync_startY() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubQuadCurve(0.0 ,0.0, 10.0, 10.0, 100.0, 100.0),
                "startY", "y1", 30.0);
    }

    @Test public void testBoundPropertySync_controlX() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubQuadCurve(0.0 ,0.0, 10.0, 10.0, 100.0, 100.0),
                "controlX", "ctrlX", 50.0);
    }

    @Test public void testBoundPropertySync_controlY() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubQuadCurve(0.0 ,0.0, 10.0, 10.0, 100.0, 100.0),
                "controlY", "ctrlY", 50.0);
    }

    @Test public void testBoundPropertySync_endX() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubQuadCurve(0.0 ,0.0, 10.0, 10.0, 100.0, 100.0),
                "endX", "x2", 300.0);
    }

    @Test public void testBoundPropertySync_endY() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubQuadCurve(0.0 ,0.0, 10.0, 10.0, 100.0, 100.0),
                "endY", "y2", 50.0);
    }

    @Test public void toStringShouldReturnNonEmptyString() {
        String s = new StubQuadCurve().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    public class StubQuadCurve extends QuadCurve {
        public StubQuadCurve() {
            super();
        }

        public StubQuadCurve(double startX, double startY, double controlX, double controlY, double endX, double endY) {
            super(startX, startY, controlX, controlY, endX, endY);
        }

        @Override
        protected NGNode impl_createPeer() {
            return new StubNGQuadCurve();
        }
    }

    public class StubNGQuadCurve extends NGQuadCurve {
        private float x1, y1, x2, y2, ctrlX, ctrlY;

        public float getCtrlX() { return ctrlX; }
        public float getCtrlY() { return ctrlY; }
        public float getX1() { return x1; }
        public float getX2() { return x2; }
        public float getY1() { return y1; }
        public float getY2() { return y2; }

        @Override
        public void updateQuadCurve(float x1, float y1, float x2, float y2, float ctrlx, float ctrly) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.ctrlX = ctrlx;
            this.ctrlY = ctrly;
        }

        public void setX1(float x1) {this.x1 = x1; }
        public void setY1(float y1) {this.y1 = y1;}
        public void setX2(float x2) {this.x2 = x2; }
        public void setY2(float y2) {this.y2 = y2; }
        public void setCtrlX(float ctrlx) {this.ctrlX = ctrlx; }
        public void setCtrlY(float ctrly) {this.ctrlY = ctrly; }
    }

}
