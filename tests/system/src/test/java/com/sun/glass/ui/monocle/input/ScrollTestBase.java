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

package com.sun.glass.ui.monocle.input;

import com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import com.sun.glass.ui.monocle.input.devices.TestTouchDevices;
import org.junit.*;
import org.junit.runners.Parameterized;
import java.util.Collection;

/**
 * Base class, intended for extending and creation of different types of scroll tests
 *  */
public abstract class ScrollTestBase extends ParameterizedTestBase {

    protected int point1X;
    protected int point1Y;
    protected int point2X;
    protected int point2Y;
    protected int p1;
    protected int p2;
    protected int totalDeltaX = 0;
    protected int totalDeltaY = 0;

    public ScrollTestBase(TestTouchDevice device) {
        super(device);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    @Before
    public void init() {
        Assume.assumeTrue(TestApplication.isMonocle());
        //Scroll tests should be running only on platforms that support current feature
        Assume.assumeTrue(Boolean.getBoolean("com.sun.javafx.gestures.scroll"));
        point1X = (int) Math.round(width * 0.5);
        point1Y = (int) Math.round(height * 0.5);
        point2X = point1X + 40;
        point2Y = point1Y;
    }

    @After
    public void releaseAll() throws Exception {
        if (device.getPressedPoints() == 1) {
            releaseFirstFinger();
        } else if (device.getPressedPoints() == 2){
            releaseAllFingers();
        }
    }

    protected int getScrollThreshold() {
        String s = System.getProperty("com.sun.javafx.gestures.scroll.threshold");
        if (s != null) {
            return Integer.valueOf(s);
        } else {
            return 10;
        }
    }

    protected boolean paramsValid(int dX, int dY, int num, int x, int y) {
        if ((0 < x + (dX * num))
           && (x + (dX * num) < (int) Math.round(width))
           && (0 < y + (dY * num))
           && (y + (dY * num) < (int) Math.round(height))) {
            return true;
        } else {
            return false;
        }
    }

    protected void pressFirstFinger() throws Exception {
        Assert.assertEquals(0, device.getPressedPoints());
        TestLog.reset();
        p1 = device.addPoint(point1X, point1Y);
        device.sync();
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", point1X, point1Y);
    }

    protected void pressSecondFinger() throws Exception {
        Assert.assertEquals(1, device.getPressedPoints());
        TestLog.reset();
        point2X = point1X + 40;
        point2Y = point1Y;
        p2 = device.addPoint(point2X, point2Y);
        device.sync();
        //verify fingers pressed
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d",
                point1X, point1Y);
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d",
                point2X, point2Y);
    }

    /**
     * The method drags one finger
     * @param firstMove - reflects if it's the first action in the drag sequence.
     */
    protected void moveOneFinger(int deltaX, int deltaY, int numOfIterations,
                               boolean firstMove) throws Exception {
        TestLog.reset();
        Assert.assertEquals(1, device.getPressedPoints());
        Assert.assertTrue(paramsValid(deltaX, deltaY, numOfIterations,
                point1X, point1Y));
        point1X += deltaX;
        point1Y += deltaY;
        device.setPoint(p1, point1X, point1Y);
        device.sync();
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", point1X, 
                point1Y);
        if (firstMove) {
            totalDeltaX = deltaX;
            totalDeltaY = deltaY;
            if (Math.abs(deltaX) > getScrollThreshold()
                    || Math.abs(deltaY) > getScrollThreshold()) {
                TestLog.waitForLogContaining("Scroll started, DeltaX: " + 0
                        + ", DeltaY: " + 0
                        + ", totalDeltaX: " + 0
                        + ", totalDeltaY: " + 0
                        + ", touch points: " + 1
                        + ", inertia value: false");
                TestLog.waitForLogContaining("Scroll, DeltaX: " + deltaX
                        + ", DeltaY: " + deltaY
                        + ", totalDeltaX: " + totalDeltaX
                        + ", totalDeltaY: " + totalDeltaY
                        + ", touch points: " + 1
                        + ", inertia value: false");
            } else {
                Assert.assertEquals(0, TestLog.countLogContaining("Scroll started"));
                Assert.assertEquals(0, TestLog.countLogContaining("Scroll, DeltaX:"));
            }
        } else {
            totalDeltaX += deltaX;
            totalDeltaY += deltaY;
            TestLog.waitForLogContaining("Scroll, DeltaX: " + deltaX
                    + ", DeltaY: " + deltaY
                    + ", totalDeltaX: " + totalDeltaX
                    + ", totalDeltaY: " + totalDeltaY
                    + ", touch points: " + 1
                    + ", inertia value: false");
        }
        String expectedLog;
        boolean passedTheThreshold =false;
        if (numOfIterations >= 2) {
            for (int i = 2; i <= numOfIterations; i++) {
                point1X += deltaX;
                point1Y += deltaY;
                TestLog.reset();
                device.setPoint(p1, point1X, point1Y);
                device.sync();
                TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                        point1X, point1Y);
                totalDeltaX += deltaX;
                totalDeltaY += deltaY;
                expectedLog = "Scroll, DeltaX: " + deltaX + ", DeltaY: " + deltaY
                        + ", totalDeltaX: " + totalDeltaX
                        + ", totalDeltaY: " + totalDeltaY
                        + ", touch points: " + 1
                        + ", inertia value: false";
                if (Math.abs(deltaX) < getScrollThreshold()
                        && Math.abs(deltaY) < getScrollThreshold()) {
                    if(Math.abs(totalDeltaX) > getScrollThreshold()
                            || Math.abs(totalDeltaY) > getScrollThreshold()) {
                        if (!passedTheThreshold) {
                            expectedLog = "Scroll, DeltaX: " + totalDeltaX
                                    + ", DeltaY: " + totalDeltaY
                                    + ", totalDeltaX: " + totalDeltaX
                                    + ", totalDeltaY: " + totalDeltaY
                                    + ", touch points: " + 1
                                    + ", inertia value: false";
                            passedTheThreshold = true;
                        }
                    } else {
                        expectedLog = "sync";
                    }
                }
                TestLog.waitForLogContaining(expectedLog);
            }
        }
    }

    /**
     * The method drags two-fingers
     * @param firstMove - reflects if it's the first action in the drag sequence.
     * @param fingersChanged - reflects if previous move/drag action used
     *                       different number of touch-points
     */
    protected void moveTwoFingers(int deltaX, int deltaY, int numOfIterations,
                                boolean firstMove, boolean fingersChanged)
                                throws Exception {
        TestLog.reset();
        Assert.assertEquals(2, device.getPressedPoints());
        Assert.assertTrue(paramsValid(deltaX, deltaY, numOfIterations,
                point1X, point1Y) && paramsValid(deltaX, deltaY, numOfIterations,
                point2X, point2Y));
        point1X += deltaX;
        point1Y += deltaY;
        point2X += deltaX;
        point2Y += deltaY;
        device.setPoint(p1, point1X, point1Y);
        device.setPoint(p2, point2X, point2Y);
        device.sync();
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", point1X, point1Y);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", point2X, point2Y);
        boolean passedTheThreshold = false;
        if (firstMove) {
            totalDeltaX = deltaX;
            totalDeltaY = deltaY;
            if (Math.abs(deltaX) > getScrollThreshold()
                    || Math.abs(deltaY) > getScrollThreshold()) {
                TestLog.waitForLogContaining("Scroll started, DeltaX: " + 0
                            + ", DeltaY: " + 0
                            + ", totalDeltaX: " + 0
                            + ", totalDeltaY: " + 0
                            + ", touch points: " + 2
                            + ", inertia value: false");
                TestLog.waitForLogContaining("Scroll, DeltaX: " + deltaX
                        + ", DeltaY: " + deltaY
                        + ", totalDeltaX: " + totalDeltaX
                        + ", totalDeltaY: " + totalDeltaY
                        + ", touch points: " + 2
                        + ", inertia value: false");
            } else {
                Assert.assertEquals(0, TestLog.countLogContaining("Scroll " +
                        "started"));
                Assert.assertEquals(0, TestLog.countLogContaining("Scroll, DeltaX:"));
            }
        } else {
            if (fingersChanged) {
                totalDeltaX = deltaX;
                totalDeltaY = deltaY;
            } else {
                totalDeltaX += deltaX;
                totalDeltaY += deltaY;
            }
            TestLog.waitForLogContaining("Scroll, DeltaX: " + deltaX
                    + ", DeltaY: " + deltaY
                    + ", totalDeltaX: " + totalDeltaX
                    + ", totalDeltaY: " + totalDeltaY
                    + ", touch points: " + 2
                    + ", inertia value: false");
            passedTheThreshold = true;
        }
        String expectedLog;
        if (numOfIterations >= 2) {
            for (int i = 2; i <= numOfIterations; i++) {
                point1X += deltaX;
                point1Y += deltaY;
                point2X += deltaX;
                point2Y += deltaY;
                TestLog.reset();
                device.setPoint(p1, point1X, point1Y);
                device.setPoint(p2, point2X, point2Y);
                device.sync();
                TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                        point1X, point1Y);
                TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                        point2X, point2Y);
                totalDeltaX += deltaX;
                totalDeltaY += deltaY;
                expectedLog = "Scroll, DeltaX: " + deltaX + ", DeltaY: " + deltaY
                        + ", totalDeltaX: " + totalDeltaX
                        + ", totalDeltaY: " + totalDeltaY
                        + ", touch points: " + 2
                        + ", inertia value: false";
                if (firstMove && Math.abs(deltaX) < getScrollThreshold()
                        && Math.abs(deltaY) < getScrollThreshold()) {
                    if(Math.abs(totalDeltaX) > getScrollThreshold()
                            || Math.abs(totalDeltaY) > getScrollThreshold()) {
                        if (!passedTheThreshold) {
                            expectedLog = "Scroll, DeltaX: " + totalDeltaX
                                    + ", DeltaY: " + totalDeltaY
                                    + ", totalDeltaX: " + totalDeltaX
                                    + ", totalDeltaY: " + totalDeltaY
                                    + ", touch points: " + 2
                                    + ", inertia value: false";
                            passedTheThreshold = true;
                        }
                    } else {
                        expectedLog = "sync";
                    }
                }
                TestLog.waitForLogContaining(expectedLog);
            }
        }
    }

    /**
     * The method releases one finger that is currently pressing on the screen
     */
    protected void releaseFirstFinger() throws Exception {
        Assert.assertEquals(1, device.getPressedPoints());
        String expectedLog;
        TestLog.reset();
        device.removePoint(p1);
        device.sync();
        //verify finger release
        int expectedValue = 0;
        expectedLog = "Scroll finished, DeltaX: " + 0
                + ", DeltaY: " + 0
                + ", totalDeltaX: " + totalDeltaX
                + ", totalDeltaY: " + totalDeltaY
                + ", touch points: " + 1
                + ", inertia value: false";
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d",
                point1X, point1Y);
        if (Math.abs(totalDeltaX) > getScrollThreshold()
                || Math.abs(totalDeltaY) > getScrollThreshold()) {
            expectedValue = 1;
            TestLog.waitForLogContaining(expectedLog);
        }
        totalDeltaX = 0;
        totalDeltaY = 0;
        Assert.assertEquals(expectedValue, TestLog.countLogContaining(expectedLog));
        if (TestLog.countLogContaining("Scroll finished") > 0) {
            TestLog.waitForLogContainingSubstrings("Scroll", "inertia value: true");
        }
    }

    /**
     * The method releases second of two fingers that are currently
     * pressing on the screen
     */
    protected void releaseSecondFinger() throws Exception {
        Assert.assertEquals(2, device.getPressedPoints());
        String expectedLog;
        TestLog.reset();
        device.removePoint(p2);
        device.sync();
        //verify finger release
        int expectedValue = 0;
        expectedLog = "Scroll finished, DeltaX: " + 0
                + ", DeltaY: " + 0
                + ", totalDeltaX: " + totalDeltaX
                + ", totalDeltaY: " + totalDeltaY
                + ", touch points: " + 2
                + ", inertia value: false";
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d",
                point2X, point2Y);
        if (Math.abs(totalDeltaX) > getScrollThreshold()
                || Math.abs(totalDeltaY) > getScrollThreshold()) {
            expectedValue = 1;
            TestLog.waitForLogContaining(expectedLog);
        }
        totalDeltaX = 0;
        totalDeltaY = 0;
        Assert.assertEquals(expectedValue, TestLog.countLogContaining(expectedLog));
    }

    /**
     * The method releases two fingers that are currently pressing on the screen
     */
    protected void releaseAllFingers() throws Exception {
        Assert.assertEquals(2, device.getPressedPoints());
        String expectedLog;
        TestLog.reset();
        device.removePoint(p1);
        device.removePoint(p2);
        device.sync();
        //verify finger release
        int expectedValue = 0;
        expectedLog = "Scroll finished, DeltaX: " + 0
                + ", DeltaY: " + 0
                + ", totalDeltaX: " + totalDeltaX
                + ", totalDeltaY: " + totalDeltaY
                + ", touch points: " + 2
                + ", inertia value: false";
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d", point1X, point1Y);
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d", point2X, point2Y);
        if (Math.abs(totalDeltaX) > getScrollThreshold() ||
                Math.abs(totalDeltaY) > getScrollThreshold()) {
            expectedValue = 1;
            TestLog.waitForLogContaining(expectedLog);
        }
        totalDeltaX = 0;
        totalDeltaY = 0;
        Assert.assertEquals(expectedValue, TestLog.countLogContaining(expectedLog));
        if (TestLog.countLogContaining("Scroll finished") > 0) {
            TestLog.waitForLogContainingSubstrings("Scroll", "inertia value: true");
        }
    }

    protected void tapToStopInertia() throws Exception {
        Assert.assertEquals(0, device.getPressedPoints());
        TestLog.reset();
        int p = device.addPoint(point1X, point1Y);
        device.sync();
        device.removePoint(p);
        device.sync();
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d", point1X, point1Y);
    }
}
