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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.glass.ui.monocle.TestLogShim;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevices;

public final class DoubleClickTest extends ParameterizedTestBase {

    private static Collection<TestTouchDevice> parameters() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    /** Test that double taps in the same area generate synthesized
     * multi-click mouse events. */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testDoubleClick1(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        int x = (int) Math.round(width / 2.0);
        int y = (int) Math.round(height / 2.0);
        TestApplication.getStage().getScene().setOnMouseClicked((e) -> TestLogShim.format("Mouse clicked: %d, %d: clickCount %d",
                       (int) e.getScreenX(), (int) e.getScreenY(),
                       e.getClickCount()));
        TestLogShim.reset();
        int p = device.addPoint(x, y);
        device.sync();
        device.removePoint(p);
        device.sync();
        p = device.addPoint(x, y);
        device.sync();
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse clicked: " + x + ", " + y + ": clickCount 1", 3000l);
        TestLogShim.waitForLog("Mouse clicked: " + x + ", " + y + ": clickCount 2", 3000l);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testDoubleClick2(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        int x1 = (int) Math.round(width / 2.0);
        int y1 = (int) Math.round(height / 2.0);
        int x2 = x1 + device.getTapRadius();
        int y2 = y1 + device.getTapRadius();

        TestApplication.getStage().getScene().setOnMouseClicked((e) -> TestLogShim.format("Mouse clicked: %d, %d: clickCount %d",
                       (int) e.getScreenX(), (int) e.getScreenY(),
                       e.getClickCount()));
        TestLogShim.reset();
        int p = device.addPoint(x1, y1);
        device.sync();
        device.removePoint(p);
        device.sync();
        p = device.addPoint(x2, y2);
        device.sync();
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse clicked: " + x1 + ", " + y1 + ": clickCount 1", 3000l);
        TestLogShim.waitForLog("Mouse clicked: " + x2 + ", " + y2 + ": clickCount 2", 3000l);
    }
}
