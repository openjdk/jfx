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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestName;
import org.junit.rules.TestWatchman;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;
import com.sun.glass.ui.monocle.TestLogShim;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;

@RunWith(Parameterized.class)
public abstract class ParameterizedTestBase {

    protected final TestTouchDevice device;
    protected final Rectangle2D stageBounds;
    private Throwable exception;
    protected double width;
    protected double height;

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

    @Rule public TestName name = new TestName();


    public ParameterizedTestBase(TestTouchDevice device, Rectangle2D stageBounds) {
        this.device = device;
        this.stageBounds = stageBounds;
    }

    public ParameterizedTestBase(TestTouchDevice device) {
        this(device, null);
    }

    @Before
    public void createDevice() throws Exception {
        TestApplication.showScene(stageBounds);
        TestLogShim.log("Starting " + name.getMethodName() + "[" + device + "]");
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

    @After
    public void destroyDevice() throws Throwable {
        if (device != null) {
            device.destroy();
        }
        TestApplication.waitForNextPulse();
        if (exception != null) {
            RuntimeException rte = new RuntimeException("Uncaught exception");
            rte.setStackTrace(new StackTraceElement[0]);
            rte.initCause(exception);
            throw rte;
        }
    }

}
