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
import org.junit.*;
import org.junit.runners.Parameterized;

import java.util.Collection;

/** Zoom tests with two touch points */
public class ZoomTest extends ParameterizedTestBase {

    public ZoomTest(TestTouchDevice device) {
        super(device);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return TestTouchDevices.getTouchDeviceParameters(2);
    }

    @Before
    public void verifyZoomEnabled() {
        Assume.assumeTrue(Boolean.getBoolean("com.sun.javafx.gestures.zoom"));
    }

    private void tapToStopInertia() throws Exception {
        int point1X = (int) Math.round(width * 0.1);
        int point1Y = (int) Math.round(height * 0.3);
        Assert.assertEquals(0, device.getPressedPoints());
        TestLog.reset();
        int p = device.addPoint(point1X, point1Y);
        device.sync();
        device.removePoint(p);
        device.sync();
        TestLog.waitForLog("Mouse clicked: %d, %d", point1X, point1Y);
    }

    /**
     * Touch down two fingers,
     * drag upper finger up in order move but not enough for zooming,
     * drag again to zoom in (make it twice bigger)
     */
    @Test
    public void testZoomInSmallStepBigStep() throws Exception {
        int x1 = (int) Math.round(width / 2);
        int y1 = (int) Math.round(height * 0.3);
        int x2 = (int) Math.round(width / 2);
        int y2 = (int) Math.round(height * 0.7);
        int step = (int) Math.round(height * 0.1);
        int smallStep = (int) device.getTapRadius() + 1;
        double threshold = 0;
        String s = System.getProperty("com.sun.javafx.gestures.zoom.threshold");
        if (s != null) {
            threshold = Double.valueOf(s);
        } else {
            threshold = 0.1;
        }
        Assume.assumeTrue(((y2 - y1) * threshold) > smallStep);

        TestLog.reset();
        //tap two fingers
        int p1 = device.addPoint(x1, y1);
        int p2 = device.addPoint(x2, y2);
        device.sync();

        //verify pressing two fingers
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", x1, y1);
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", x2, y2);

        //drag upper finger up in order move but not enough for zooming
        int newy1 = y1 - smallStep;
        int newy2 = 0;
        TestLog.reset();
        device.setPoint(p1, x1, newy1);
        device.sync();

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d", x2, y2);
        Assert.assertEquals(0, TestLog.countLogContaining("Zoom started"));

        //drag upper finger up and lower down in order to zoom in
        newy1 = y1 - step;
        newy2 = y2 + step;
        TestLog.reset();
        device.setPoint(p1, x1, newy1);
        device.setPoint(p2, x2, newy2);
        device.sync();
        double factor0 = 1.0;
        double factor1 = (double) (newy2 - newy1)/(y2 - y1);

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, newy2);
        TestLog.waitForLogContaining("Zoom started, factor: " + factor0
                + ", total factor: " + factor0  + ", inertia value: false");
        TestLog.waitForLogContaining("Zoom, factor: " + factor1
                + ", total factor: " + factor1 + ", inertia value: false");

        step = step * 2;
        newy1 = y1 - step;
        newy2 = y2 + step;
        TestLog.reset();
        device.setPoint(p1, x1, newy1);
        device.setPoint(p2, x2, newy2);
        device.sync();
        double factor2 = (double) (newy2 - newy1)/(y2 - y1 + step);

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, newy2);
        TestLog.waitForLogContaining("Zoom, factor: " + factor2
                + ", total factor: " + (factor1 * factor2)
                + ", inertia value: false");

        //release first finger
        TestLog.reset();
        device.removePoint(p1);
        device.sync();
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d", x2, newy2);
        TestLog.waitForLogContaining("Zoom finished, factor: " + factor0
                + ", total factor: " + (factor1 * factor2)
                + ", inertia value: false");
        //release second finger
        TestLog.reset();
        device.removePoint(p2);
        device.sync();
        TestLog.waitForLogContaining("Touch released: %d, %d", x2, newy2);
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d", x2, newy2);
        TestLog.waitForLog("Mouse released: %d, %d", x2, newy2);
        TestLog.waitForLog("Mouse clicked: %d, %d", x2, newy2);
        Assert.assertEquals(1, TestLog.countLogContaining("Mouse clicked: "
                + x2 +", " + newy2));
        tapToStopInertia();
    }

    /**
     * Touch down two fingers, zoom in (make it twice bigger)
     */
    @Test
    public void testZoomIn() throws Exception {
        int x1 = (int) Math.round(width / 2);
        int y1 = (int) Math.round(height * 0.3);
        int x2 = (int) Math.round(width / 2);
        int y2 = (int) Math.round(height * 0.7);
        int step = (int) Math.round(height * 0.1);

        TestLog.reset();
        //tap two fingers
        int p1 = device.addPoint(x1, y1);
        int p2 = device.addPoint(x2, y2);
        device.sync();

        //verify pressing two fingers
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", x1, y1);
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", x2, y2);

        //drag upper finger up and lower down in order to zoom in
        int newy1 = y1 - step;
        int newy2 = y2 + step;
        TestLog.reset();
        device.setPoint(p1, x1, newy1);
        device.setPoint(p2, x2, newy2);
        device.sync();
        double factor0 = 1.0;
        double factor1 = (double) (newy2 - newy1)/(y2 - y1);

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, newy2);
        TestLog.waitForLogContaining("Zoom started, factor: " + factor0
                + ", total factor: " + factor0 + ", inertia value: false");
        TestLog.waitForLogContaining("Zoom, factor: " + factor1
                + ", total factor: " + factor1  + ", inertia value: false");

        step = step * 2;
        newy1 = y1 - step;
        newy2 = y2 + step;
        TestLog.reset();
        device.setPoint(p1, x1, newy1);
        device.setPoint(p2, x2, newy2);
        device.sync();
        double factor2 = (double) (newy2 - newy1)/(y2 - y1 + step);

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, newy2);
        TestLog.waitForLogContaining("Zoom, factor: " + factor2
                + ", total factor: " + (factor1 * factor2)
                + ", inertia value: false");

        //release first finger
        TestLog.reset();
        device.removePoint(p1);
        device.sync();
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d",
                x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d",
                x2, newy2);
        TestLog.waitForLogContaining("Zoom finished, factor: " + factor0
                + ", total factor: " + (factor1 * factor2)
                + ", inertia value: false");

        //release second finger
        TestLog.reset();
        device.removePoint(p2);
        device.sync();
        TestLog.waitForLogContaining("Touch released: %d, %d", x2, newy2);
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d",
                x2, newy2);
        TestLog.waitForLog("Mouse released: %d, %d", x2, newy2);
        TestLog.waitForLog("Mouse clicked: %d, %d", x2, newy2);
        Assert.assertEquals(1, TestLog.countLogContaining("Mouse clicked: "
                + x2 +", " + newy2));
        tapToStopInertia();
    }

    /**
     * Touch down two fingers, zoom out (make it quarter of original size)
     */
    @Test
    public void testZoomOut() throws Exception {
        int x1 = (int) Math.round(width / 2);
        int y1 = (int) Math.round(height * 0.1);
        int x2 = (int) Math.round(width / 2);
        int y2 = (int) Math.round(height * 0.9);
        int step = (int) Math.round(height * 0.1);
        double factor0 = 1.0;

        //tap two fingers
        TestLog.reset();
        int p1 = device.addPoint(x1, y1);
        int p2 = device.addPoint(x2, y2);
        device.sync();

        //verify pressing two fingers
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", x1, y1);
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", x2, y2);

        //drag upper finger down and lower - up, in order to zoom out
        TestLog.reset();
        int newy1 = y1 + step;
        int newy2 = y2 - step;
        device.setPoint(p1, x1, newy1);
        device.setPoint(p2, x2, newy2);
        device.sync();
        double factor1 = (double) (newy2 - newy1)/(y2 - y1);

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, newy2);
        TestLog.waitForLogContaining("Zoom started, factor: " + factor0
                + ", total factor: " + factor0 + ", inertia value: false");
        TestLog.waitForLogContaining("Zoom, factor: " + factor1
                + ", total factor: " + factor1 + ", inertia value: false");

        TestLog.reset();
        y1 = y1 + step;
        y2 = y2 - step;
        newy1 = y1 + step;
        newy2 = y2 - step;

        device.setPoint(p1, x1, newy1);
        device.setPoint(p2, x2, newy2);
        device.sync();
        double factor2 = (double) (newy2 - newy1)/(y2 - y1);

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, newy2);
        TestLog.waitForLogContaining("Zoom, factor: " + factor2
                + ", total factor: " + (factor1 * factor2)
                + ", inertia value: false");

        TestLog.reset();
        y1 = y1 + step;
        y2 = y2 - step;
        newy1 = y1 + step;
        newy2 = y2 - step;

        device.setPoint(p1, x1, newy1);
        device.setPoint(p2, x2, newy2);
        device.sync();
        double factor3 = (double) (newy2 - newy1)/(y2 - y1);

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, newy2);
        TestLog.waitForLogContaining("Zoom, factor: " + factor3
                + ", total factor: " + (factor1 * factor2 * factor3)
                + ", inertia value: false");

        //release first finger
        TestLog.reset();
        device.removePoint(p1);
        device.sync();
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d",
                x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d",
                x2, newy2);
        TestLog.waitForLogContaining("Zoom finished, factor: " + factor0
                + ", total factor: " + (factor1 * factor2 * factor3)
                + ", inertia value: false");

        //release second finger
        TestLog.reset();
        device.removePoint(p2);
        device.sync();
        TestLog.waitForLogContaining("Touch released: %d, %d", x2, newy2);
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d",
                x2, newy2);
        TestLog.waitForLog("Mouse released: %d, %d", x2, newy2);
        TestLog.waitForLog("Mouse clicked: %d, %d", x2, newy2);
        Assert.assertEquals(1, TestLog.countLogContaining("Mouse clicked: "
                + x2 +", " + newy2));
        tapToStopInertia();
    }

    /**
     * Touch down two fingers,
     * drag upper finger down in order move but not enough for zooming,
     * drag again to zoom out (quarter of original size)
     */
    @Test
    public void testZoomOutSmallStepBigStep() throws Exception {
        int x1 = (int) Math.round(width / 2);
        int y1 = (int) Math.round(height * 0.1);
        int x2 = (int) Math.round(width / 2);
        int y2 = (int) Math.round(height * 0.9);
        int step = (int) Math.round(height * 0.1);
        int smallStep = (int) device.getTapRadius() + 1;
        double threshold = 0;
        String s = System.getProperty("com.sun.javafx.gestures.zoom.threshold");
        if (s != null) {
            threshold = Double.valueOf(s);
        } else {
            threshold = 0.1;
        }
        Assume.assumeTrue(((y2 - y1) * threshold) > smallStep);
        double factor0 = 1.0;

        TestLog.reset();
        //tap two fingers
        int p1 = device.addPoint(x1, y1);
        int p2 = device.addPoint(x2, y2);
        device.sync();

        //verify pressing two fingers
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", x1, y1);
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", x2, y2);

        //drag upper finger down in order move but not enough for zooming
        TestLog.reset();
        int newy1 = y1 + smallStep;
        int newy2 = 0;

        device.setPoint(p1, x1, newy1);
        device.sync();

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d", x2, y2);
        Assert.assertEquals(0, TestLog.countLogContaining("Zoom started"));

        //drag upper finger up and lower down in order to zoom in
        TestLog.reset();
        newy1 = y1 + step;
        newy2 = y2 - step;
        device.setPoint(p1, x1, newy1);
        device.setPoint(p2, x2, newy2);
        device.sync();
        double factor1 = (double) (newy2 - newy1)/(y2 - y1);

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, newy2);
        TestLog.waitForLogContaining("Zoom started, factor: " + factor0
                + ", total factor: " + factor0 + ", inertia value: false");
        TestLog.waitForLogContaining("Zoom, factor: " + factor1
                + ", total factor: " + factor1 + ", inertia value: false");
        TestLog.reset();
        y1 = y1 + step;
        y2 = y2 - step;
        newy1 = y1 + step;
        newy2 = y2 - step;
        device.setPoint(p1, x1, newy1);
        device.setPoint(p2, x2, newy2);
        device.sync();
        double factor2 = (double) (newy2 - newy1)/(y2 - y1);

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, newy2);
        TestLog.waitForLogContaining("Zoom, factor: " + factor2
                + ", total factor: " + (factor1 * factor2)
                + ", inertia value: false");

        TestLog.reset();
        y1 = y1 + step;
        y2 = y2 - step;
        newy1 = y1 + step;
        newy2 = y2 - step;
        device.setPoint(p1, x1, newy1);
        device.setPoint(p2, x2, newy2);
        device.sync();
        double factor3 = (double) (newy2 - newy1)/(y2 - y1);

        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, newy2);
        TestLog.waitForLogContaining("Zoom, factor: " + factor3
                + ", total factor: " + (factor1 * factor2 * factor3)
                + ", inertia value: false");

        //release first finger
        TestLog.reset();
        device.removePoint(p1);
        device.sync();
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d",
                x1, newy1);
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d",
                x2, newy2);
        TestLog.waitForLogContaining("Zoom finished, factor: " + factor0
                + ", total factor: " + (factor1 * factor2 * factor3)
                + ", inertia value: false");

        //release second finger
        TestLog.reset();
        device.removePoint(p2);
        device.sync();
        TestLog.waitForLogContaining("Touch released: %d, %d", x2, newy2);
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d",
                x2, newy2);
        TestLog.waitForLog("Mouse released: %d, %d", x2, newy2);
        TestLog.waitForLog("Mouse clicked: %d, %d", x2, newy2);
        Assert.assertEquals(1, TestLog.countLogContaining("Mouse clicked: "
                + x2 +", " + newy2));
        tapToStopInertia();
    }
}