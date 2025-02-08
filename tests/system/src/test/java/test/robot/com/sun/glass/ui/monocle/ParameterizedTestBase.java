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

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import com.sun.glass.ui.monocle.TestLogShim;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;

public abstract class ParameterizedTestBase {

    protected TestTouchDevice device;
    private String testName;
    private Throwable exception;
    protected double width;
    protected double height;

    /**
     * There seems to be no need to handle assumptions this way in junit5.
     *
    @Rule
    public TestWatchman monitor = new TestWatchman() {
        @Override
        public void failed(Throwable e, FrameworkMethod method) {
            if (!(e instanceof AssumptionViolatedException)) {
                // Ignore silently a failed Assume
                System.err.format("Failed %s.%s[%s]\n",
                                  method.getMethod().getDeclaringClass().getName(),
                                  method.getName(),
                                  device);
            }
        }
    };
    */

    // gets test name from the junit5 system
    @BeforeEach
    void getTestName(TestInfo t) {
        testName = t.getDisplayName();
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    protected void createDevice(TestTouchDevice device, Rectangle2D stageBounds) throws Exception {
        this.device = device;
        TestApplication.showScene(stageBounds);
        TestLogShim.log("Starting " + testName + "[" + device + "]");
        Rectangle2D r = TestApplication.getScreenBounds();
        width = r.getWidth();
        height = r.getHeight();
        TestLogShim.reset();
        device.create();
        TestApplication.addMouseListeners();
        TestApplication.addTouchListeners();
        TestApplication.addGestureListeners();
        TestLogShim.reset();
        Platform.runLater(
                () -> Thread.currentThread().setUncaughtExceptionHandler(
                        (t, e) -> exception = e));
    }

    @AfterEach
    public void destroyDevice() throws Throwable {
        if (device != null) {
            device.destroy();
        }
        // junit5: ignored tests do not initialize the toolkit
        if (device != null) {
            TestApplication.waitForNextPulse();
            if (exception != null) {
                RuntimeException rte = new RuntimeException("Uncaught exception");
                rte.setStackTrace(new StackTraceElement[0]);
                rte.initCause(exception);
                throw rte;
            }
        }
    }
}
