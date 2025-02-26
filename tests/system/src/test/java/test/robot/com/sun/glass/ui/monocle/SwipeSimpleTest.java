/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.com.sun.glass.ui.monocle;

import java.util.Collection;
import javafx.scene.input.GestureEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.glass.ui.monocle.TestLogShim;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevices;

public final class SwipeSimpleTest extends ParameterizedTestBase {

    private final int SWIPE_THRESHOLD = 10;
    int startPointX;
    int startPointY;

    static {
        System.setProperty("com.sun.javafx.isEmbedded", "true");
    }

    private static Collection<TestTouchDevice> parameters() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    private int getDelta() throws Exception {
        int max = Math.max(SWIPE_THRESHOLD, device.getTapRadius());
        return max + 1;
    }

    @BeforeAll
    public static void beforeInit() {
        System.setProperty("com.sun.javafx.gestures.swipe.maxduration", "1200");
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void init(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        TestApplication.getStage().getScene().addEventHandler(
                GestureEvent.ANY,
                e -> TestLogShim.format("%s at %.0f, %.0f",
                        e.getEventType(),
                        e.getScreenX(),
                        e.getScreenY()));
        startPointX = (int) Math.round(width * 0.5);
        startPointY = (int) Math.round(height * 0.5);
    }

    private void swipe(Point[] points, String expectedSwipe) throws Exception {
        Assertions.assertTrue(points.length > 1);
        int x = points[0].getX();
        int y = points[0].getY();
        TestLogShim.reset();
        int p1 = device.addPoint(x, y);
        device.sync();
        for (int i = 1; i < points.length; i++) {
            device.setPoint(p1, points[i].getX(), points[i].getY());
            device.sync();
        }
        device.removePoint(p1);
        device.sync();
        int finalX = points[points.length - 1].getX();
        int finalY = points[points.length - 1].getY();
        TestLogShim.waitForLogContaining("Touch released: %d, %d", finalX, finalY);
        TestLogShim.waitForLogContaining("Mouse released: %d, %d", finalX, finalY);
        TestLogShim.waitForLogContaining("Mouse clicked: %d, %d", finalX, finalY);
        if (expectedSwipe != null) {
            TestLogShim.waitForLogContaining(expectedSwipe);
        } else {
            Assertions.assertEquals(0, TestLogShim.countLogContaining("SWIPE"));
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeRight1(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY),
                new Point(startPointX + getDelta(), startPointY)};
        swipe(path, "SWIPE_RIGHT");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeRight2(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(65,
                getDelta(), startPointX, startPointY)};
        swipe(path, "SWIPE_RIGHT");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeRight3(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(80,
                getDelta(), startPointX, startPointY)};
        swipe(path, "SWIPE_RIGHT");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeRight4(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(115,
                getDelta(), startPointX, startPointY)};
        swipe(path, "SWIPE_RIGHT");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeLeft1(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY),
                new Point(startPointX - getDelta(), startPointY)};
        swipe(path, "SWIPE_LEFT");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeLeft2(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(-65,
                getDelta(), startPointX, startPointY)};
        swipe(path, "SWIPE_LEFT");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeLeft3(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(-80,
                getDelta(), startPointX, startPointY)};
        swipe(path, "SWIPE_LEFT");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeLeft4(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(-115,
                getDelta(), startPointX, startPointY)};
        swipe(path, "SWIPE_LEFT");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeUp1(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY),
                new Point(startPointX, startPointY - getDelta())};
        swipe(path, "SWIPE_UP");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeUp2(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(25,
                getDelta(), startPointX, startPointY)};
        swipe(path, "SWIPE_UP");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeUp3(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(-25,
                getDelta(), startPointX, startPointY)};
        swipe(path, "SWIPE_UP");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeDown1(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY),
                new Point(startPointX, startPointY + getDelta())};
        swipe(path, "SWIPE_DOWN");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeDown2(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(155,
                getDelta(), startPointX, startPointY)};
        swipe(path, "SWIPE_DOWN");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeDown3(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(-155,
                getDelta(), startPointX, startPointY)};
        swipe(path, "SWIPE_DOWN");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testNoSwipeUp(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(31,
                getDelta(), startPointX, startPointY)};
        swipe(path, null);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testNoSwipeRight(TestTouchDevice device) throws Exception {
        init(device);
        Point[] path = {new Point(startPointX, startPointY), new Point(59,
                getDelta(), startPointX, startPointY)};
        swipe(path, null);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipeUp4Points(TestTouchDevice device) throws Exception {
        init(device);
        int delta = getDelta();
        Point p1 = new Point(startPointX, startPointY);
        Point p2 = new Point(45, delta, p1);
        Point p3 = new Point(22, delta, p2);
        Point p4 = new Point(10, delta, p3);
        Point[] path = {p1, p2, p3, p4};
        swipe(path, "SWIPE_UP");
    }

    private class Point {
        int x;
        int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Creation of new point that located a "distance" from (centerX, centerY),
         * and defined by "angle" variable, when 0 degrees position is on axis y.
         */
        Point(int angle, int distance, int centerX, int centerY) {
            int transformedAngle = 90 - angle;
            this.x = centerX + (int) Math.round(distance * Math.cos(Math
                    .toRadians(transformedAngle)));
            this.y = centerY - (int) Math.round(distance * Math.sin(Math
                    .toRadians(transformedAngle)));
        }

        Point(int angle, int radius, Point p) {
            this(angle, radius, p.getX(), p.getY());
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}
