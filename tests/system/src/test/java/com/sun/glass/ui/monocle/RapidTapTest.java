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
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

public class RapidTapTest extends ParameterizedTestBase {

    public RapidTapTest(TestTouchDevice device) {
        super(device);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    /** 20 quick taps */
    @Test
    public void tapTwentyTimes() throws Exception {
        for (int i = 0; i < 20; i++) {
            int p = device.addPoint(width / 2, height / 2);
            device.sync();
            TestLog.waitForLogContaining("TouchPoint: PRESSED", 3000);
            TestLog.waitForLogContaining("Mouse pressed", 3000);
            device.removePoint(p);
            device.sync();
        }
        TestRunnable.invokeAndWaitUntilSuccess(() -> {
            Assert.assertEquals(20, TestLog.countLogContaining(
                    "TouchPoint: PRESSED"));
            Assert.assertEquals(20, TestLog.countLogContaining(
                    "TouchPoint: RELEASED"));
            Assert.assertEquals(20,
                                TestLog.countLogContaining("Mouse pressed"));
            Assert.assertEquals(20,
                                TestLog.countLogContaining("Mouse released"));
            Assert.assertEquals(20,
                                TestLog.countLogContaining("Mouse clicked"));
        }, 3000);
    }

    /** 20 quick taps while the application thread is busy */
    @Test
    public void tapTwentyTimesUnderStress() throws Exception {
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
                TestLog.waitForLogContaining("TouchPoint: PRESSED", 3000);
                TestLog.waitForLogContaining("Mouse pressed", 3000);
                device.removePoint(p);
                device.sync();
            }
            TestRunnable.invokeAndWaitUntilSuccess(() -> {
                Assert.assertEquals(20, TestLog.countLogContaining("TouchPoint: PRESSED"));
                Assert.assertEquals(20, TestLog.countLogContaining("TouchPoint: RELEASED"));
                Assert.assertEquals(20, TestLog.countLogContaining("Mouse pressed"));
                Assert.assertEquals(20, TestLog.countLogContaining("Mouse released"));
                Assert.assertEquals(20, TestLog.countLogContaining("Mouse clicked"));
            }, 10000);
        } finally {
            Platform.runLater(a::stop);
        }
    }

}
