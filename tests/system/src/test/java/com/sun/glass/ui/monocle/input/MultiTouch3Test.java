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
import javafx.geometry.Rectangle2D;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Collection;

/** Multitouch tests with three points */
public class MultiTouch3Test extends ParameterizedTestBase {

    public MultiTouch3Test(TestTouchDevice device) {
        super(device);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return TestTouchDevices.getTouchDeviceParameters(3);
    }

    @Test
//    @Ignore("RT-35546")
    /** This test follows the sequence described in touch event documentation:
     * 1. Touch the screen with two fingers
     * 2. Move both fingers
     * 3. Touch the screen with a third finger
     * 4. Move all fingers
     * 5. Remove all fingers
     */
    public void touchSequence() throws Exception {
        final int x1 = (int) Math.round(width * 0.5f);
        final int y1 = (int) Math.round(height * 0.5f);
        final int x2 = (int) Math.round(width * 0.75f);
        final int y2 = (int) Math.round(height * 0.75f);
        final int x3 = (int) Math.round(width * 0.25f);
        final int y3 = (int) Math.round(height * 0.25f);
        final int dx = device.getTapRadius();
        final int dy = device.getTapRadius();
        // first finger
        int p1 = device.addPoint(x1, y1);
        device.sync();
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", x1, y1);
        // add a second finger
        int p2 = device.addPoint(x2, y2);
        device.sync();
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d", x1, y1);
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", x2, y2);
        // drag both fingers
        for (int i = 1; i < 10; i++) {
            TestLog.reset();
            device.setPoint(p1, x1 + dx * i, y1 + dy * i);
            device.setPoint(p2, x2 + dx * i, y2 + dy * i);
            device.sync();
            TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                                         x1 + dx * i, y1 + dy * i);
            TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                                         x2 + dx * i, y2 + dy * i);
        }
        for (int i = 8; i >= 0; i--) {
            TestLog.reset();
            device.setPoint(p1, x1 + dx * i, y1 + dy * i);
            device.setPoint(p2, x2 + dx * i, y2 + dy * i);
            device.sync();
            TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                                         x1 + dx * i, y1 + dy * i);
            TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                                         x2 + dx * i, y2 + dy * i);
        }
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, y1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, y2);
        // add a third finger
        int p3 = device.addPoint(x3, y3);
        device.sync();
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d", x1, y1);
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d", x2, y2);
        TestLog.waitForLogContaining("TouchPoint: PRESSED %d, %d", x3, y3);

        // drag three fingers
        for (int i = 1; i < 10; i++) {
            TestLog.reset();
            device.setPoint(p1, x1 + dx * i, y1 + dy * i);
            device.setPoint(p2, x2 + dx * i, y2 + dy * i);
            device.setPoint(p3, x3 + dx * i, y3 + dy * i);
            device.sync();
            TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                                         x1 + dx * i, y1 + dy * i);
            TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                                         x2 + dx * i, y2 + dy * i);
            TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                                         x3 + dx * i, y3 + dy * i);
        }
        for (int i = 8; i >= 0; i--) {
            TestLog.reset();
            device.setPoint(p1, x1 + dx * i, y1 + dy * i);
            device.setPoint(p2, x2 + dx * i, y2 + dy * i);
            device.setPoint(p3, x3 + dx * i, y3 + dy * i);
            device.sync();
            TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                                         x1 + dx * i, y1 + dy * i);
            TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                                         x2 + dx * i, y2 + dy * i);
            TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d",
                                         x3 + dx * i, y3 + dy * i);
        }
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x1, y1);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x2, y2);
        TestLog.waitForLogContaining("TouchPoint: MOVED %d, %d", x3, y3);

        //release first finger
        TestLog.reset();
        device.removePoint(p1);
        device.sync();
        TestLog.waitForLogContaining("Touch released: %d, %d", x1, y1);
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d", x1, y1);
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d", x2, y2);
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d", x3, y3);
        //release second finger
        TestLog.reset();
        device.removePoint(p2);
        device.sync();
        TestLog.waitForLogContaining("Touch released: %d, %d", x2, y2);
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d", x2, y2);
        TestLog.waitForLogContaining("TouchPoint: STATIONARY %d, %d", x3, y3);
        //release third finger
        device.removePoint(p3);
        device.sync();
        TestLog.waitForLog("Touch released: %d, %d", x3, y3);
        TestLog.waitForLogContaining("TouchPoint: RELEASED %d, %d", x3, y3);
    }

}
