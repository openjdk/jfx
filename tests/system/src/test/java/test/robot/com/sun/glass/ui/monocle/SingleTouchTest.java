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
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.glass.ui.monocle.TestLogShim;
import test.com.sun.glass.ui.monocle.TestRunnable;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevices;

public final class SingleTouchTest extends ParameterizedTestBase {

    private static Collection<TestTouchDevice> parameters() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    /**
     * Touch down and up
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tap(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final int x = (int) Math.round(width * 0.5);
        final int y = (int) Math.round(height * 0.5);
        // tap
        int p = device.addPoint(x, y);
        device.sync();
        // release
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: %d, %d", x, y);
        TestLogShim.waitForLog("Mouse released: %d, %d", x, y);
        TestLogShim.waitForLog("Mouse clicked: %d, %d", x, y);
        TestLogShim.waitForLog("Touch pressed: %d, %d", x, y);
        TestLogShim.waitForLog("Touch released: %d, %d", x, y);

        // Check that the touch event has one touch point.
        Assertions.assertEquals(
            0,
            TestLogShim.getLog().stream()
                .filter(s -> s.startsWith("Touch points count"))
                .filter(s -> !s.startsWith("Touch points count: [1]")).count(),
            "Expected only one touch point");
    }

    /**
     * Touch down, send repeat events in the same location, touch up
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tapHoldRelease(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final int x = (int) Math.round(width * 0.5);
        final int y = (int) Math.round(height * 0.5);
        // tap
        int p = device.addPoint(x, y);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: %d, %d", x, y);
        TestLogShim.waitForLog("Touch pressed: %d, %d", x, y);
        TestLogShim.reset();
        // hold
        device.resendStateAndSync();
        device.sync();
        // release
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse released: %d, %d", x, y);
        TestLogShim.waitForLog("Mouse clicked: %d, %d", x, y);
        TestLogShim.waitForLog("Touch released: %d, %d", x, y);
        // We don't have anything sensible to do with repeat events in the
        // same location, so make sure they are filtered out.
        Assertions.assertEquals(0, TestLogShim.countLogContaining("Mouse pressed:"));
        Assertions.assertEquals(0, TestLogShim.countLogContaining("Touch pressed:"));
        // Check that the touch event has one touch point.
        Assertions.assertEquals(
            0,
            TestLogShim.getLog().stream()
                .filter(s -> s.startsWith("Touch points count"))
                .filter(s -> !s.startsWith("Touch points count: [1]")).count(),
            "Expected only one touch point");
    }

    /**
     * Touch down, drag, touch up
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tapAndDrag1(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final int x1 = (int) Math.round(width * 0.5);
        final int y1 = (int) Math.round(height * 0.5);
        final int x2 = (int) Math.round(width * 0.75);
        final int y2 = (int) Math.round(height * 0.75);
        // tap
        int p = device.addPoint(x1, y1);
        device.sync();
        // drag
        device.setPoint(p, x2, y2);
        device.sync();
        // release
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLogShim.waitForLog("Mouse dragged: %d, %d", x2, y2);
        TestLogShim.waitForLog("Mouse released: %d, %d", x2, y2);
        TestLogShim.waitForLog("Mouse clicked: %d, %d", x2, y2);
        TestLogShim.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLogShim.waitForLog("Touch moved: %d, %d", x2, y2);
        TestLogShim.waitForLog("Touch released: %d, %d", x2, y2);
        // Check that the touch event has one touch point.
        Assertions.assertEquals(
            0,
            TestLogShim.getLog().stream()
                .filter(s -> s.startsWith("Touch points count"))
                .filter(s -> !s.startsWith("Touch points count: [1]")).count(),
            "Expected only one touch point");
    }

    /**
     * Touch down, drag, touch up, with no change in Y coordinate
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tapAndDrag2(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final int x1 = (int) Math.round(width * 0.5);
        final int y1 = (int) Math.round(height * 0.5);
        final int x2 = (int) Math.round(width * 0.75);
        // tap
        int p = device.addPoint(x1, y1);
        device.sync();
        // drag
        device.setPoint(p, x2, y1);
        device.sync();
        // release
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLogShim.waitForLog("Mouse dragged: %d, %d", x2, y1);
        TestLogShim.waitForLog("Mouse released: %d, %d", x2, y1);
        TestLogShim.waitForLog("Mouse clicked: %d, %d", x2, y1);
        TestLogShim.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLogShim.waitForLog("Touch moved: %d, %d", x2, y1);
        TestLogShim.waitForLog("Touch released: %d, %d", x2, y1);
        // Check that the touch event has one touch point.
        Assertions.assertEquals(
            0,
            TestLogShim.getLog().stream()
                .filter(s -> s.startsWith("Touch points count"))
                .filter(s -> !s.startsWith("Touch points count: [1]")).count(),
            "Expected only one touch point");
    }

    /**
     * Touch down, drag, touch up, no change in X coordinate
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tapAndDrag3(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final int x1 = (int) Math.round(width * 0.5);
        final int y1 = (int) Math.round(height * 0.5);
        final int y2 = (int) Math.round(height * 0.75);
        // tap
        int p = device.addPoint(x1, y1);
        device.sync();
        // drag
        device.setPoint(p, x1, y2);
        device.sync();
        // release
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLogShim.waitForLog("Mouse dragged: %d, %d", x1, y2);
        TestLogShim.waitForLog("Mouse released: %d, %d", x1, y2);
        TestLogShim.waitForLog("Mouse clicked: %d, %d", x1, y2);
        TestLogShim.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLogShim.waitForLog("Touch moved: %d, %d", x1, y2);
        TestLogShim.waitForLog("Touch released: %d, %d", x1, y2);
        // Check that the touch event has one touch point.
        Assertions.assertEquals(
            0,
            TestLogShim.getLog().stream()
                .filter(s -> s.startsWith("Touch points count"))
                .filter(s -> !s.startsWith("Touch points count: [1]")).count(),
            "Expected only one touch point");
    }

    /**
     * Touch down, small drag, release. The drag should be filtered out.
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tapWithTinyDrag(TestTouchDevice device) throws Exception {
        Assumptions.assumeTrue(device.getTapRadius() > 1);
        createDevice(device, null);
        final int x1 = (int) Math.round(width * 0.5);
        final int y1 = (int) Math.round(height * 0.5);
        final int x2 = x1 + 1;
        final int y2 = y1 + 1;
        // tap
        int p = device.addPoint(x1, y1);
        device.sync();
        // drag
        device.setPoint(p, x2, y2);
        device.sync();
        // release
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLogShim.waitForLog("Mouse released: %d, %d", x1, y1);
        TestLogShim.waitForLog("Mouse clicked: %d, %d", x1, y1);
        TestLogShim.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLogShim.waitForLog("Touch released: %d, %d", x1, y1);
        Assertions.assertEquals(0l, TestLogShim.countLogContaining("Mouse dragged"));
        Assertions.assertEquals(0l, TestLogShim.countLogContaining("Touch moved"));
        // Check that the touch event has one touch point.
        Assertions.assertEquals(
            0,
            TestLogShim.getLog().stream()
                .filter(s -> s.startsWith("Touch points count"))
                .filter(s -> !s.startsWith("Touch points count: [1]")).count(),
            "Expected only one touch point");
    }

    /**
     * Touch down, drag, release, tap again
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tapDragReleaseTapAgain(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        Assumptions.assumeTrue(device.getTapRadius() < width * 0.2);
        final int x1 = (int) Math.round(width * 0.5);
        final int y1 = (int) Math.round(height * 0.5);
        final int x2 = (int) Math.round(width * 0.7);
        final int y2 = (int) Math.round(height * 0.7);
        // tap
        int p = device.addPoint(x1, y1);
        device.sync();
        // drag
        device.setPoint(p, x2, y2);
        device.sync();
        // release
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLogShim.waitForLog("Mouse released: %d, %d", x2, y2);
        TestLogShim.waitForLog("Mouse clicked: %d, %d", x2, y2);
        TestLogShim.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLogShim.waitForLog("Touch released: %d, %d", x2, y2);
        TestLogShim.clear();
        // tap again and release
        p = device.addPoint(x1, y1);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLogShim.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLogShim.clear();
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse released: %d, %d", x1, y1);
        TestLogShim.waitForLog("Mouse clicked: %d, %d", x1, y1);
        TestLogShim.waitForLog("Touch released: %d, %d", x1, y1);
    }

    /**
     * Touch down, change scene, release finger.
     */
    @Disabled("JDK-8093836")
    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeSceneDuringTap(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final int x1 = (int) Math.round(width * 0.3);
        final int y1 = (int) Math.round(height * 0.3);
        int p1 = device.addPoint(x1, y1);
        device.sync();
        TestLogShim.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestRunnable.invokeAndWait(() ->
        {
            Rectangle r = new Rectangle(0, 0, width, height);
            Group g = new Group();
            g.getChildren().add(r);
            Scene scene = new Scene(g);
            TestApplication.getStage().setScene(scene);
        });
        device.removePoint(p1);
        device.sync();
        Assertions.assertEquals(1, TestLogShim.countLogContaining("Mouse clicked: " + x1 +", " + y1));
    }
}
