/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import com.sun.glass.ui.monocle.input.devices.TestTouchDevices;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.TouchEvent;
import org.junit.*;
import org.junit.runners.Parameterized;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SingleTouchNonFullScreenTest extends ParameterizedTestBase {

    private static final TestCase[] TEST_CASES = {
            new TestCase(200, 100, 400, 300, 200, 100, 599, 399),
            new TestCase(100, 200, 400, 300, 100, 200, 499, 499),
    };

    private TestCase testCase;

    static class TestCase {
        Rectangle2D stageBounds;
        int x1, y1, x2, y2;
        TestCase(double winX, double winY, double width,double height, int x1, int y1, int x2, int y2) {
            this.stageBounds = new Rectangle2D(winX, winY, width, height);
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public String toString() {
            return "TestCase[stage bounds=("
                    + stageBounds.getMinX()
                    + "," + stageBounds.getMinY()
                    + "," + stageBounds.getWidth()
                    + "," + stageBounds.getHeight() + ")"
                    + ", x1=" + x1
                    + ", y1=" + y1
                    + ", x2=" + x2
                    + ", y2=" + y2 + "]";
        }
    }

    public SingleTouchNonFullScreenTest(TestTouchDevice device, TestCase testCase)
    {
        super(device, testCase.stageBounds);
        this.testCase = testCase;
        TestLog.format("Starting test with %s, %s", device, testCase);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> params = new ArrayList<>();
        List<TestTouchDevice> devices = TestTouchDevices.getTouchDevices();
        for (TestTouchDevice device : devices) {
            for (TestCase testCase : TEST_CASES) {
                params.add(new Object[]{device, testCase});
            }
        }
        return params;
    }

    @Before
    public void addListener() throws Exception {
        TestApplication.getStage().getScene().addEventHandler(
                TouchEvent.TOUCH_PRESSED,
                e -> TestLog.format("Touch pressed [relative]: %.0f, %.0f",
                        e.getTouchPoint().getX(),
                        e.getTouchPoint().getY())
        );

        TestApplication.getStage().getScene().addEventHandler(
                TouchEvent.TOUCH_RELEASED,
                        e -> TestLog.format("Touch released [relative]: %.0f, %.0f",
                                e.getTouchPoint().getX(),
                                e.getTouchPoint().getY()));

        TestApplication.getStage().getScene().addEventHandler(
                TouchEvent.TOUCH_MOVED,
                        e -> TestLog.format("Touch moved [relative]: %.0f, %.0f",
                                e.getTouchPoint().getX(),
                                e.getTouchPoint().getY()));

    }

    /**
     * Touch down and up
     */
    @Test
    public void tap() throws Exception {
        final int x1 = testCase.x1;
        final int y1 = testCase.y1;

        final int relX1 = x1 - (int) stageBounds.getMinX();
        final int relY1 = y1 - (int) stageBounds.getMinY();
        // tap
        int p = device.addPoint(x1, y1);
        device.sync();
        // release
        device.removePoint(p);
        device.sync();
        TestLog.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Mouse released: %d, %d", x1, y1);
        TestLog.waitForLog("Mouse clicked: %d, %d", x1, y1);
        TestLog.waitForLog("Touch pressed [relative]: %d, %d", relX1, relY1);
        TestLog.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Touch released [relative]: %d, %d", relX1, relY1);
        TestLog.waitForLog("Touch released: %d, %d", x1, y1);

        // Check that the touch event has one touch point.
        Assert.assertEquals("Expected only one touch point", 0,
                            TestLog.getLog().stream()
                            .filter(s -> s.startsWith("Touch points count"))
                            .filter(s -> !s.startsWith("Touch points count: [1]")).count());
    }

    /**
     * Touch down, drag, touch up
     */
    @Test
    public void tapAndDrag() throws Exception {
        final int x1 = testCase.x1;
        final int y1 = testCase.y1;
        final int x2 = testCase.x2;
        final int y2 = testCase.y2;
        final int relX1 = x1 - (int) stageBounds.getMinX();
        final int relY1 = y1 - (int) stageBounds.getMinY();
        final int relX2 = x2 - (int) stageBounds.getMinX();
        final int relY2 = y2 - (int) stageBounds.getMinY();
        // tap
        int p = device.addPoint(x1, y1);
        device.sync();
        // drag
        device.setPoint(p, x2, y2);
        device.sync();
        // release
        device.removePoint(p);
        device.sync();
        TestLog.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Mouse dragged: %d, %d", x2, y2);
        TestLog.waitForLog("Mouse released: %d, %d", x2, y2);
        TestLog.waitForLog("Mouse clicked: %d, %d", x2, y2);
        TestLog.waitForLog("Touch pressed [relative]: %d, %d", relX1, relY1);
        TestLog.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Touch moved [relative]: %d, %d", relX2, relY2);
        TestLog.waitForLog("Touch moved: %d, %d", x2, y2);
        TestLog.waitForLog("Touch released [relative]: %d, %d", relX2, relY2);
        TestLog.waitForLog("Touch released: %d, %d", x2, y2);
        // Check that the touch event has one touch point.
        Assert.assertEquals("Expected only one touch point", 0,
                            TestLog.getLog().stream()
                            .filter(s -> s.startsWith("Touch points count"))
                            .filter(s -> !s.startsWith("Touch points count: [1]")).count());
    }
}
