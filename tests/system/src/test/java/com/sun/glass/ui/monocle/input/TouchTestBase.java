/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import org.junit.After;
import org.junit.Before;

import java.io.OutputStream;
import java.io.PrintStream;

public class TouchTestBase {

    protected UInput ui;
    private static final PrintStream systemErr = System.err;
    private SystemErrFilter systemErrFilter;
    protected Rectangle2D screen;
    private double absXMax, absYMax;
    /** UNDEFINED is an arbitrary marker to use for when we want to avoid
     *  setting a value for a coordinate.
     */
    protected static final double UNDEFINED = Double.MAX_VALUE / Math.PI;

    static class SystemErrFilter extends PrintStream {
        private boolean foundException = false;

        public SystemErrFilter(OutputStream out) {
            super(out);
        }

        @Override
        public synchronized void print(String s) {
            System.out.flush();
            if (s.indexOf("Exception") >= 0) {
                foundException = true;
            }
            super.print(s);
        }

        void checkException() throws InterruptedException {
            // wait in case an exception trace is about to be printed
            TestApplication.waitForNextPulse();
            synchronized (this) {
                if (!foundException) {
                    return;
                }
            }
            throw new AssertionError("Found exception");
        }
    }

    protected void setAbsScale(int absXMax, int absYMax) {
        this.absXMax = (double) absXMax;
        this.absYMax = (double) absYMax;
    }

    @Before
    public void initDevice() throws Exception {
        TestApplication.getStage();
        ui = new UInput();
        systemErrFilter = new SystemErrFilter(System.err);
        System.setErr(systemErrFilter);
        TestRunnable.invokeAndWait(() -> {
            screen = Screen.getPrimary().getBounds();
        });
    }

    @After
    public void destroyDevice() throws InterruptedException {
        try {
            ui.processLine("DESTROY");
        } catch (RuntimeException e) {
        }
        try {
            ui.processLine("CLOSE");
        } catch (RuntimeException e) {
        }
        ui.dispose();
        System.setErr(systemErr);
        if (systemErrFilter != null) {
            systemErrFilter.checkException();
        }
    }

    protected void absMTPosition(double x, double y) {
        if (x != UNDEFINED) {
            ui.processLine("EV_ABS ABS_MT_POSITION_X "
                    + Math.round(x * absXMax / screen.getWidth()));
        }
        if (y != UNDEFINED) {
            ui.processLine("EV_ABS ABS_MT_POSITION_Y "
                    + Math.round(y * absYMax / screen.getHeight()));
        }
    }

    protected void absPosition(double x, double y) {
        if (x != UNDEFINED) {
            ui.processLine("EV_ABS ABS_X "
                    + Math.round(x * absXMax / screen.getWidth()));
        }
        if (y != UNDEFINED) {
            ui.processLine("EV_ABS ABS_Y "
                    + Math.round(y * absYMax / screen.getHeight()));
        }
    }

}

