/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.glass.ui.monocle.TestLogShim;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevices;

public final class FuzzyTapTest extends ParameterizedTestBase {

    private static Collection<TestTouchDevice> parameters() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    /** Touch down, touch up at a slightly different location*/
    @ParameterizedTest
    @MethodSource("parameters")
    public void tap1(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final int x = (int) width / 2;
        final int y = (int) height / 2;
        final int tapRadius = device.getTapRadius();
        final int x1 = x + tapRadius / 2;
        final int y1 = y + tapRadius / 2;
        int p = device.addPoint(x, y);
        device.sync();
        device.setAndRemovePoint(p, x1, y1);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: " + x + ", " + y, 3000);
        TestLogShim.waitForLog("Mouse clicked: " + x + ", " + y, 3000);
        TestLogShim.waitForLog("Mouse released: " + x + ", " + y, 3000);
        Assertions.assertEquals(0, TestLogShim.countLogContaining("Mouse dragged:"));
        Assertions.assertEquals(0, TestLogShim.countLogContaining("Touch moved:"));
    }

    /** Touch down, small move, touch up */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tap1a(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final int x = (int) width / 2;
        final int y = (int) height / 2;
        final int tapRadius = device.getTapRadius();
        final int x1 = x + tapRadius / 2;
        final int y1 = y + tapRadius / 2;
        int p = device.addPoint(x, y);
        device.sync();
        device.setPoint(p, x1, y1);
        device.sync();
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse clicked: " + x + ", " + y, 3000);
        TestLogShim.waitForLog("Mouse released: " + x + ", " + y, 3000);
        Assertions.assertEquals(0, TestLogShim.countLogContaining("Mouse dragged:"));
        Assertions.assertEquals(0, TestLogShim.countLogContaining("Touch moved:"));
    }

    /** Touch down, touch up outside the tap radius */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tap2(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final int x = (int) width / 2;
        final int y = (int) height / 2;
        final int tapRadius = device.getTapRadius();
        final int x1 = x + tapRadius;
        final int y1 = y + tapRadius;
        int p = device.addPoint(x, y);
        device.sync();
        device.setAndRemovePoint(p, x1, y1);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: " + x + ", " + y, 3000);
        TestLogShim.waitForLog("Mouse released: " + x + ", " + y, 3000);
        TestLogShim.waitForLog("Mouse clicked: " + x + ", " + y, 3000);
        TestLogShim.waitForLog("Touch pressed: " + x + ", " + y, 3000);
        TestLogShim.waitForLog("Touch released: " + x + ", " + y, 3000);
        Assertions.assertEquals(1, TestLogShim.countLogContaining("Mouse clicked:"));
    }

    /** Touch down, move outside touch radius, touch up */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tap2a(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final int x = (int) width / 2;
        final int y = (int) height / 2;
        final int tapRadius = device.getTapRadius();
        final int x1 = x + tapRadius;
        final int y1 = y + tapRadius;
        int p = device.addPoint(x, y);
        device.sync();
        device.setPoint(p, x1, y1);
        device.sync();
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: " + x + ", " + y, 3000);
        TestLogShim.waitForLog("Mouse dragged: " + x1 + ", " + y1, 3000);
        TestLogShim.waitForLog("Mouse released: " + x1 + ", " + y1, 3000);
        TestLogShim.waitForLog("Mouse clicked: " + x1 + ", " + y1, 3000);
        TestLogShim.waitForLog("Touch pressed: " + x + ", " + y, 3000);
        TestLogShim.waitForLog("Touch moved: " + x1 + ", " + y1, 3000);
        TestLogShim.waitForLog("Touch released: " + x1 + ", " + y1, 3000);
        Assertions.assertEquals(1, TestLogShim.countLogContaining("Mouse clicked: " + x1 + ", " + y1));
    }

    /** Touch down, drift outside touch radius, touch up */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tap3b(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final int x = (int) width / 2;
        final int y = (int) height / 2;
        final int tapRadius = device.getTapRadius();
        final int x1 = x + tapRadius * 2;
        final int y1 = y + tapRadius * 2;
        final int x2 = x + tapRadius * 3;
        final int y2 = y + tapRadius * 3;
        int p = device.addPoint(x, y);
        device.sync();
        // drift out of the tap radius
        for (int i = 0; i < tapRadius * 2; i++) {
            device.setPoint(p, x + i, y + i);
            device.sync();
        }
        // extra moves to make sure the final move is not filtered out
        device.setPoint(p, 0, 0);
        device.sync();
        device.setPoint(p, x2, y2);
        device.sync();
        // and release
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: " + x + ", " + y, 3000);
        TestLogShim.waitForLog("Mouse dragged: " + x2 + ", " + y2, 3000);
        TestLogShim.waitForLog("Mouse released: " + x2 + ", " + y2, 3000);
        TestLogShim.waitForLog("Mouse clicked: " + x2 + ", " + y2, 3000);
        TestLogShim.waitForLog("Touch pressed: " + x + ", " + y, 3000);
        TestLogShim.waitForLog("Touch moved: " + x2 + ", " + y2, 3000);
        TestLogShim.waitForLog("Touch released: " + x2 + ", " + y2, 3000);
        Assertions.assertEquals(1, TestLogShim.countLogContaining("Mouse clicked: " + x2 + ", " + y2));
    }
}
