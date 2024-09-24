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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import javafx.scene.input.GestureEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.glass.ui.monocle.TestLogShim;
import com.sun.javafx.PlatformUtil;
import test.com.sun.glass.ui.monocle.TestRunnable;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevices;

public final class SwipeTest extends ParameterizedTestBase {

    static {
        System.setProperty("com.sun.javafx.isEmbedded", "true");
    }

    private static final SwipeTestCase[] TEST_CASES = {
        new SwipeTestCase(200.0, Math.PI * 0.5, 10l, 1000.0, 0.0, 200.0, "SWIPE_RIGHT"),
        new SwipeTestCase(200.0, Math.PI * 0.5, 50l, 1000.0, 0.0, 200.0, "SWIPE_RIGHT"),

        new SwipeTestCase(200.0, Math.PI * 0.4, 200l, 100.0, 0.0, 200.0, "SWIPE_RIGHT"),
        new SwipeTestCase(200.0, Math.PI * 0.5, 200l, 100.0, 0.0, 200.0, "SWIPE_RIGHT"),
        new SwipeTestCase(200.0, Math.PI * 0.6, 200l, 100.0, 0.0, 200.0, "SWIPE_RIGHT"),
        new SwipeTestCase(200.0, Math.PI * 0.4, 200l, 100.0, 30.0, 200.0, "SWIPE_RIGHT"),
        new SwipeTestCase(200.0, Math.PI * 0.5, 200l, 100.0, 30.0, 200.0, "SWIPE_RIGHT"),
        new SwipeTestCase(200.0, Math.PI * 0.6, 200l, 100.0, 30.0, 200.0, "SWIPE_RIGHT"),

        new SwipeTestCase(200.0, Math.PI * 1.4, 200l, 100.0, 0.0, 200.0, "SWIPE_LEFT"),
        new SwipeTestCase(200.0, Math.PI * 1.5, 200l, 100.0, 0.0, 200.0, "SWIPE_LEFT"),
        new SwipeTestCase(200.0, Math.PI * 1.6, 200l, 100.0, 0.0, 200.0, "SWIPE_LEFT"),
        new SwipeTestCase(200.0, Math.PI * 1.4, 200l, 100.0, 30.0, 200.0, "SWIPE_LEFT"),
        new SwipeTestCase(200.0, Math.PI * 1.5, 200l, 100.0, 30.0, 200.0, "SWIPE_LEFT"),
        new SwipeTestCase(200.0, Math.PI * 1.6, 200l, 100.0, 30.0, 200.0, "SWIPE_LEFT"),

        new SwipeTestCase(200.0, Math.PI * 1.9, 200l, 100.0, 0.0, 200.0, "SWIPE_UP"),
        new SwipeTestCase(200.0, Math.PI * 0.0, 200l, 100.0, 0.0, 200.0, "SWIPE_UP"),
        new SwipeTestCase(200.0, Math.PI * 0.1, 200l, 100.0, 0.0, 200.0, "SWIPE_UP"),
        new SwipeTestCase(200.0, Math.PI * 1.9, 200l, 100.0, 30.0, 200.0, "SWIPE_UP"),
        new SwipeTestCase(200.0, Math.PI * 0.0, 200l, 100.0, 30.0, 200.0, "SWIPE_UP"),
        new SwipeTestCase(200.0, Math.PI * 0.1, 200l, 100.0, 30.0, 200.0, "SWIPE_UP"),

        new SwipeTestCase(200.0, Math.PI * 0.9, 200l, 100.0, 0.0, 200.0, "SWIPE_DOWN"),
        new SwipeTestCase(200.0, Math.PI * 1.0, 200l, 100.0, 0.0, 200.0, "SWIPE_DOWN"),
        new SwipeTestCase(200.0, Math.PI * 1.1, 200l, 100.0, 0.0, 200.0, "SWIPE_DOWN"),
        new SwipeTestCase(200.0, Math.PI * 0.9, 200l, 100.0, 30.0, 200.0, "SWIPE_DOWN"),
        new SwipeTestCase(200.0, Math.PI * 1.0, 200l, 100.0, 30.0, 200.0, "SWIPE_DOWN"),
        new SwipeTestCase(200.0, Math.PI * 1.1, 200l, 100.0, 30.0, 200.0, "SWIPE_DOWN"),
    };

    private SwipeTestCase testCase;

    static class SwipeTestCase {
        double length;
        double theta;
        long time;
        double density;
        double amplitude;
        double wavelength;
        String expectedSwipe;
        SwipeTestCase(double length, double theta, long time, double density,
                      double amplitude, double wavelength, String expectedSwipe) {
            this.length = length;
            this.theta = theta;
            this.time = time;
            this.density = density;
            this.amplitude = amplitude;
            this.wavelength = wavelength;
            this.expectedSwipe = expectedSwipe;
        }

        @Override
        public String toString() {
            return "SwipeTestCase["
                    + "length=" + length
                    + ",theta=" + theta
                    + ",time=" + time
                    + ",density=" + density
                    + ",amplitude=" + amplitude
                    + ",wavelength=" + wavelength
                    + ",expectedSwipe=" + expectedSwipe + "]";
        }
    }

    private static Collection<Arguments> parameters() {
        List<Arguments> params = new ArrayList<>();
        List<TestTouchDevice> devices = TestTouchDevices.getTouchDevices();
        for (TestTouchDevice device : devices) {
            for (SwipeTestCase testCase : TEST_CASES) {
                params.add(Arguments.of(device, testCase));
            }
        }
        return params;
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void init(TestTouchDevice device, SwipeTestCase testCase) throws Exception {
        createDevice(device, null);

        TestLogShim.format("Starting test with %s, %s", device, testCase);
        TestApplication.getStage();
        TestRunnable.invokeAndWait(() -> {
            Assumptions.assumeTrue(TestApplication.isMonocle() || TestApplication.isLens());
            Assumptions.assumeTrue(PlatformUtil.isEmbedded());
        });

        TestApplication.getStage().getScene().addEventHandler(
                GestureEvent.ANY,
                e -> TestLogShim.format("%s at %.0f, %.0f",
                                    e.getEventType(),
                                    e.getScreenX(),
                                    e.getScreenY()));
    }

    /**
     * Sends a series of points as a sine wave
     *
     * @param p The point ID to move
     * @param x1 Starting X
     * @param y1 Starting Y
     * @param length length of the vector from the start point to the end point
     * @param theta Direction of the sine wave, measured in radians
     *              clockwise from the upwards Y axis
     * @param time Time to send all points, in milliseconds
     * @param density number of points to send per second
     * @param amplitude of the sine wave, in pixels
     * @param wavelength of the sine wave, in pixels
     */
    private CountDownLatch generatePoints(
                                TestTouchDevice device,
                                int p,
                                int x1, int y1,
                                double length,
                                double theta,
                                long time,
                                double density,
                                double amplitude,
                                double wavelength) {
        long startTime = System.currentTimeMillis();
        double deltaX = length / (time * density / 1000.0);
        CountDownLatch latch = new CountDownLatch(1);
        TimerTask task = new TimerTask() {
            private double x = 0;
            @Override
            public void run() {
                try {
                    double targetX =
                            (System.currentTimeMillis() - startTime) * length
                                    / time;
                    if (targetX > length) {
                        cancel();
                        latch.countDown();
                        return;
                    }
                    if (x > targetX) {
                        return;
                    }
                    do {
                        x += deltaX;
                        double y = amplitude * Math.sin(
                                (x * Math.PI * 2.0) / wavelength);
                        double phi = Math.atan2(x, y);
                        double h = Math.sqrt(x * x + y * y);
                        double rotatedX = h * Math.cos(theta - phi);
                        double rotatedY = h * Math.sin(theta - phi);
                        device.setPoint(p, x1 + rotatedX, y1 + rotatedY);
                        device.sync();
                    } while (x < targetX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Timer("Touch point generator", true)
                .scheduleAtFixedRate(task, 0, (int) Math.max(1, time / density));
        return latch;
    }

    @Disabled("RT-37709")
    @ParameterizedTest
    @MethodSource("parameters")
    public void testSwipe(TestTouchDevice device, SwipeTestCase testCase) throws Exception {
        init(device, testCase);
        final int x = (int) Math.round(width * 0.5);
        final int y = (int) Math.round(height * 0.5);
        // tap
        int p = device.addPoint(x, y);
        device.sync();
        // swipe
        generatePoints(device, p, x, y,
                       testCase.length,
                       testCase.theta,
                       testCase.time,
                       testCase.density,
                       testCase.amplitude,
                       testCase.wavelength).await();
        // release
        device.removePoint(p);
        device.sync();
        TestLogShim.waitForLog("Mouse pressed: %d, %d", x, y);
        TestLogShim.waitForLogContaining("Mouse released");
        TestLogShim.waitForLogContaining("Mouse clicked");
        TestLogShim.waitForLogContaining("Touch pressed");
        TestLogShim.waitForLogContaining("Touch released");
        if (testCase.expectedSwipe == null) {
            Assertions.assertEquals(0, TestLogShim.countLogContaining("SWIPE"));
        } else {
            TestLogShim.waitForLogContaining(testCase.expectedSwipe);
            Assertions.assertEquals(1, TestLogShim.countLogContaining("SWIPE"));
        }
    }

}
