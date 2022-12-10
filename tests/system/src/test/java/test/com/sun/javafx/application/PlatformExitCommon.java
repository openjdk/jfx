/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.application;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;

import org.junit.BeforeClass;

import com.sun.javafx.application.PlatformImplShim;

import test.util.Util;

/**
 * Tests calling Platform.exit() after starting the FX runtime
 */
public class PlatformExitCommon {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    private final CountDownLatch exitLatch = PlatformImplShim.test_getPlatformExitLatch();

    // Short delay to allow runtime thread to execute
    private static final int DELAY = 200;

    @BeforeClass
    public static void initFX() throws Exception {
        Util.startup(startupLatch, startupLatch::countDown);
    }

    protected void doTestPlatformExit(boolean again) {
        Util.sleep(DELAY);
        assertEquals(1, exitLatch.getCount());

        Platform.exit();
        Util.sleep(DELAY);
        assertEquals(0, exitLatch.getCount());

        if (again) {
            Platform.exit();
            assertEquals(0, exitLatch.getCount());
        }
    }

    protected void doTestPlatformExitOnAppThread(boolean again) {
        Util.sleep(DELAY);
        assertEquals(1, exitLatch.getCount());

        // Call exit on JavaFX application thread
        Util.runAndWait(Platform::exit);
        assertEquals(0, exitLatch.getCount());

        if (again) {
            // Call exit on test thread
            Platform.exit();
            assertEquals(0, exitLatch.getCount());
        }
    }

}
