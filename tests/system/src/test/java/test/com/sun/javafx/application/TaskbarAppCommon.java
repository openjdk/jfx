/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static test.util.Util.TIMEOUT;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.application.PlatformImplShim;

/**
 * Test program for PlatformImpl
 * Each of the tests must be run in a separate JVM which is why each
 * is in its own subclass.
 */
public class TaskbarAppCommon {

    // Used to launch the platform before running any test
    private final CountDownLatch launchLatch = new CountDownLatch(1);

    private void startup() {
        // Start the FX Platform
        new Thread(() -> PlatformImpl.startup(() -> {
            assertTrue(Platform.isFxApplicationThread());
            launchLatch.countDown();
        })).start();

        try {
            if (!launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                fail("Timeout waiting for Platform to start");
            }
        } catch (InterruptedException ex) {
            fail(ex);
        }
        assertEquals(0, launchLatch.getCount());
        final CountDownLatch exitLatch = PlatformImplShim.test_getPlatformExitLatch();
        assertEquals(1, exitLatch.getCount());
    }

    // ========================== TEST CASES ==========================

    public void doTestTaskbarAppDefault() {
        assertTrue(PlatformImpl.isTaskbarApplication());
    }

    public void doTestTaskbarAppSetGet() {
        PlatformImpl.setTaskbarApplication(false);
        assertFalse(PlatformImpl.isTaskbarApplication());
        PlatformImpl.setTaskbarApplication(true);
        assertTrue(PlatformImpl.isTaskbarApplication());
    }

    public void doTestTaskbarAppStartDefault() {
        assertTrue(PlatformImpl.isTaskbarApplication());
        String taskbarAppProp = System.getProperty("glass.taskbarApplication");
        assertNull(taskbarAppProp);
        startup();
        taskbarAppProp = System.getProperty("glass.taskbarApplication");
        assertNull(taskbarAppProp);
        boolean isTaskbarApp = !"false".equalsIgnoreCase(taskbarAppProp);
        assertTrue(isTaskbarApp);
        taskbarAppProp = System.getProperty("glass.taskbarApplication", "true");
        isTaskbarApp = !"false".equalsIgnoreCase(taskbarAppProp);
        assertTrue(isTaskbarApp);
        Platform.exit();
    }

    public void doTestTaskbarAppStartFalse() {
        PlatformImpl.setTaskbarApplication(false);
        assertFalse(PlatformImpl.isTaskbarApplication());
        String taskbarAppProp = System.getProperty("glass.taskbarApplication");
        assertNull(taskbarAppProp);
        startup();
        taskbarAppProp = System.getProperty("glass.taskbarApplication");
        assertNotNull(taskbarAppProp);
        boolean isTaskbarApp = !"false".equalsIgnoreCase(taskbarAppProp);
        assertFalse(isTaskbarApp);
        taskbarAppProp = System.getProperty("glass.taskbarApplication", "true");
        isTaskbarApp = !"false".equalsIgnoreCase(taskbarAppProp);
        assertFalse(isTaskbarApp);
        Platform.exit();
    }

    public void doTestTaskbarAppStartTrue() {
        PlatformImpl.setTaskbarApplication(true);
        assertTrue(PlatformImpl.isTaskbarApplication());
        String taskbarAppProp = System.getProperty("glass.taskbarApplication");
        assertNull(taskbarAppProp);
        startup();
        taskbarAppProp = System.getProperty("glass.taskbarApplication");
        assertNull(taskbarAppProp);
        boolean isTaskbarApp = !"false".equalsIgnoreCase(taskbarAppProp);
        assertTrue(isTaskbarApp);
        taskbarAppProp = System.getProperty("glass.taskbarApplication", "true");
        isTaskbarApp = !"false".equalsIgnoreCase(taskbarAppProp);
        assertTrue(isTaskbarApp);
        Platform.exit();
    }

}
