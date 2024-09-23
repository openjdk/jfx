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
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.glass.ui.monocle.TestLogShim;
import test.com.sun.glass.ui.monocle.TestRunnable;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevices;

public final class TouchEventLookaheadTest extends ParameterizedTestBase {

    private static Collection<TestTouchDevice> parameters() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    /** Merge together similar moves */
    @ParameterizedTest
    @MethodSource("parameters")
    public void mergeMoves(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        Assumptions.assumeTrue(TestApplication.isMonocle());
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestApplication.addTouchListeners();
        TestLogShim.reset();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x1 = (int) Math.round(width * 0.1);
        final int y1 = (int) Math.round(height * 0.1);
        final int x2 = (int) Math.round(width * 0.9);
        final int y2 = (int) Math.round(height * 0.9);
        final int x3 = (int) Math.round(width * 0.5);
        final int y3 = (int) Math.round(height * 0.5);
        // Push events while on the event thread, making sure that events
        // will be buffered up and enabling filtering to take place
        TestRunnable.invokeAndWait(() -> {
            int p = device.addPoint(x1, y1);
            device.sync();
            for (int x = x1; x <= x2; x += (x2 - x1) / 100) {
                device.setPoint(p, x, y1);
                device.sync();
            }
            for (int y = y1; y <= y2; y += (y2 - y1) / 100) {
                device.setPoint(p, x2, y);
                device.sync();
            }
            device.setPoint(p, x3, y3);
            device.sync();
            device.removePoint(p);
            device.sync();
        });
        // Check that the initial point reported is correct
        TestLogShim.waitForLog("Mouse pressed: " + x1 + ", " + y1, 3000);
        TestLogShim.waitForLog("Touch pressed: " + x1 + ", " + y1, 3000);
        // Check that the final point reported is correct
        TestLogShim.waitForLog("Mouse released: " + x3 + ", " + y3, 3000);
        TestLogShim.waitForLog("Touch released: " + x3 + ", " + y3, 3000);
        // Check that moves in between were filtered
        TestLogShim.waitForLog("Mouse dragged: " + x3 + ", " + y3, 3000);
        TestLogShim.waitForLog("Touch moved: " + x3 + ", " + y3, 3000);
        Assertions.assertTrue(TestLogShim.countLogContaining("Mouse dragged") <= 3);
        Assertions.assertTrue(TestLogShim.countLogContaining("Touch moved") <= 3);
    }
}
