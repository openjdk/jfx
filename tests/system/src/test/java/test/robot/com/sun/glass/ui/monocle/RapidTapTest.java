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
import java.util.concurrent.CountDownLatch;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.glass.ui.monocle.TestLogShim;
import test.com.sun.glass.ui.monocle.TestRunnable;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevices;

public final class RapidTapTest extends ParameterizedTestBase {

    private static Collection<TestTouchDevice> parameters() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    /** 20 quick taps */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tapTwentyTimes(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        for (int i = 0; i < 20; i++) {
            int p = device.addPoint(width / 2, height / 2);
            device.sync();
            TestLogShim.waitForLogContaining("TouchPoint: PRESSED", 3000);
            TestLogShim.waitForLogContaining("Mouse pressed", 3000);
            device.removePoint(p);
            device.sync();
        }
        TestRunnable.invokeAndWaitUntilSuccess(() -> {
            Assertions.assertEquals(20, TestLogShim.countLogContaining(
                    "TouchPoint: PRESSED"));
            Assertions.assertEquals(20, TestLogShim.countLogContaining(
                    "TouchPoint: RELEASED"));
            Assertions.assertEquals(20,
                                TestLogShim.countLogContaining("Mouse pressed"));
            Assertions.assertEquals(20,
                                TestLogShim.countLogContaining("Mouse released"));
            Assertions.assertEquals(20,
                                TestLogShim.countLogContaining("Mouse clicked"));
        }, 3000);
    }

    /** 20 quick taps while the application thread is busy */
    @ParameterizedTest
    @MethodSource("parameters")
    public void tapTwentyTimesUnderStress(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        final CountDownLatch latch = new CountDownLatch(1);
        // throttle the application thread
        final AnimationTimer a = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Spin for 50ms adjusted for time scale
                double spinTime = Math.round(50000000.0 * TestApplication.getTimeScale());
                long end = now + Math.round(spinTime);
                latch.countDown();
                while (System.nanoTime() < end) { } // spin
            }
        };
        Platform.runLater(a::start);
        latch.await();
        try {
            for (int i = 0; i < 20; i++) {
                int p = device.addPoint(width / 2, height / 2);
                device.sync();
                TestLogShim.waitForLogContaining("TouchPoint: PRESSED", 3000);
                TestLogShim.waitForLogContaining("Mouse pressed", 3000);
                device.removePoint(p);
                device.sync();
            }
            TestRunnable.invokeAndWaitUntilSuccess(() -> {
                Assertions.assertEquals(20, TestLogShim.countLogContaining("TouchPoint: PRESSED"));
                Assertions.assertEquals(20, TestLogShim.countLogContaining("TouchPoint: RELEASED"));
                Assertions.assertEquals(20, TestLogShim.countLogContaining("Mouse pressed"));
                Assertions.assertEquals(20, TestLogShim.countLogContaining("Mouse released"));
                Assertions.assertEquals(20, TestLogShim.countLogContaining("Mouse clicked"));
            }, 10000);
        } finally {
            Platform.runLater(a::stop);
        }
    }

}
