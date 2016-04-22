/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.application.PlatformImplShim;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import junit.framework.AssertionFailedError;
import test.util.Util;

import static org.junit.Assert.*;
import static test.util.Util.TIMEOUT;

/**
 * Test program for Platform startup.
 * Each of the tests must be run in a separate JVM which is why each
 * is in its own subclass.
 */
public class PlatformStartupCommon {

    // Sleep time showing/hiding window in milliseconds
    private static final int SLEEP_TIME = 1000;

    // Used to start the toolkit before running any test
    private final CountDownLatch startupLatch = new CountDownLatch(1);

    private Stage mainStage;

    private void createMainStage() {
        mainStage = new Stage();
        mainStage.setTitle("Primary stage");
        Group root = new Group();
        Scene scene = new Scene(root);
        scene.setFill(Color.LIGHTYELLOW);
        mainStage.setScene(scene);
        mainStage.setX(0);
        mainStage.setY(0);
        mainStage.setWidth(210);
        mainStage.setHeight(180);
    }

    private void doTestCommon(final boolean implicitExit) {
        final Throwable[] testError = new Throwable[1];
        final Thread testThread = Thread.currentThread();

        // Start the Toolkit
        assertFalse(Platform.isFxApplicationThread());
        assertEquals(1, startupLatch.getCount());
        Platform.setImplicitExit(implicitExit);
        Platform.startup(() -> {
            try {
                assertTrue(Platform.isFxApplicationThread());
                startupLatch.countDown();
                assertEquals(0, startupLatch.getCount());
            } catch (Throwable th) {
                testError[0] = th;
                testThread.interrupt();
            }
        });
        assertFalse(Platform.isFxApplicationThread());

        try {
            if (!startupLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Toolkit to start");
            }

            final CountDownLatch rDone = new CountDownLatch(1);
            // Test that we can do a runLater that throws exception
            Platform.runLater(() -> {
                try {
                    throw new RuntimeException("this exception is expected");
                } finally {
                    rDone.countDown();
                }
            });
            if (!rDone.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for runLater with Exception");
            }

            // Create and show main stage
            Util.runAndWait(() -> {
                createMainStage();
                mainStage.show();
            });

            // Hide the primary stage after a short delay
            Thread.sleep(SLEEP_TIME);
            Util.runAndWait(mainStage::hide);

            // Test exit behavior after another short delay
            Thread.sleep(SLEEP_TIME);

            final CountDownLatch exitLatch = PlatformImplShim.test_getPlatformExitLatch();

            if (implicitExit) {
                // Verify that that the runtime has exited
                assertEquals(0, exitLatch.getCount());

                // Verify that that a runLater is a no-op
                final AtomicBoolean isAlive = new AtomicBoolean(false);
                Platform.runLater(() -> isAlive.set(true));
                Thread.sleep(SLEEP_TIME);
                assertFalse(isAlive.get());
            } else {
                // Verify that the FX runtime has not exited
                assertEquals(1, exitLatch.getCount());

                // Make sure Toolkit is still alive and running
                AtomicBoolean isAlive = new AtomicBoolean(false);
                Util.runAndWait(() -> isAlive.set(true));
                assertTrue(isAlive.get());

                // Shutdown the FX runtime and wait for toolkit to exit
                Platform.exit();

                if (!exitLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    throw new AssertionFailedError("Timeout waiting for Platform to exit");
                }
            }
        } catch (InterruptedException ex) {
            if (testError[0] != null) {
                Util.throwError(testError[0]);
            } else {
                fail("Unexpected exception: " + ex);
            }
        }
    }

    // ========================== TEST CASES ==========================

    protected void doTestStartupExplicitExit() {
        doTestCommon(false);
    }

    protected void doTestStartupImplicitExit() {
        doTestCommon(true);
    }

}
