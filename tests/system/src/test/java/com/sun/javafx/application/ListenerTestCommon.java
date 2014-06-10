/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.application;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import junit.framework.AssertionFailedError;
import util.Util;

import static org.junit.Assert.*;
import static util.Util.TIMEOUT;

/**
 * Test program for Platform finishListener
 * Each of the tests must be run in a separate JVM which is why each
 * is in its own subclass.
 */
public class ListenerTestCommon {

    // Short delay in milliseconds
    private static final int DELAY = 10;

    // Sleep time showing/hiding window in milliseconds
    private static final int SLEEP_TIME = 1000;

    // Used to launch the platform before running any test
    private final CountDownLatch launchLatch = new CountDownLatch(1);

    // Used to determine when the toolkit is shutdown
    private CountDownLatch exitLatch;

    // Finish listener used by the various tests
    private PlatformImpl.FinishListener listener = null;

    private final CountDownLatch idleNotification = new CountDownLatch(1);
    private final CountDownLatch exitNotification = new CountDownLatch(1);
    private final AtomicBoolean implicitExit = new AtomicBoolean();

    private Stage stage;

    public enum ThrowableType {
        NONE,
        EXCEPTION,
        ERROR
    }

    private void setup() {
        // Start the FX Platform
        new Thread(() -> PlatformImpl.startup(() -> {
            assertTrue(Platform.isFxApplicationThread());
            launchLatch.countDown();
        })).start();

        try {
            if (!launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Platform to start");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }
        exitLatch = PlatformImpl.test_getPlatformExitLatch();
        assertEquals(1, exitLatch.getCount());
        assertEquals(0, launchLatch.getCount());
        assertEquals(1, exitLatch.getCount());
        assertNull(listener);

        listener = new PlatformImpl.FinishListener() {
            public void idle(boolean flag) {
                implicitExit.set(flag);
                idleNotification.countDown();
            }
            public void exitCalled() {
                exitNotification.countDown();
            }
        };
        PlatformImpl.addListener(listener);
    }

    private Stage makeStage() {
        Stage stg = new Stage();
        stg.setTitle("Primary stage");
        Group root = new Group();
        Scene scene = new Scene(root);
        scene.setFill(Color.LIGHTYELLOW);
        stg.setScene(scene);
        stg.setX(0);
        stg.setY(0);
        stg.setWidth(210);
        stg.setHeight(180);
        return stg;
    }

    // ========================== TEST CASES ==========================

    public void doTestExit() {
        setup();
        assertNotNull(listener);

        Util.runAndWait(() -> {
            assertTrue(Platform.isFxApplicationThread());
            assertTrue(Platform.isImplicitExit());
        });

        Util.sleep(DELAY);
        assertEquals(1, exitNotification.getCount());
        assertEquals(1, idleNotification.getCount());

        Platform.exit();
        try {
            if (!exitNotification.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for exit notification");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }
        assertEquals(0, exitNotification.getCount());

        Util.sleep(DELAY);
        assertEquals(1, idleNotification.getCount());
        assertEquals(1, exitLatch.getCount());

        PlatformImpl.removeListener(listener);
        listener = null;
    }

    public void doTestIdleImplicit(final boolean implicit,
            final ThrowableType throwableType) {

        setup();
        assertNotNull(listener);

        Util.runAndWait(() -> {
            assertTrue(Platform.isFxApplicationThread());
            assertTrue(Platform.isImplicitExit());
            if (!implicit) {
                Platform.setImplicitExit(false);
            }
            PlatformImpl.addListener(listener);
        });

        Util.sleep(DELAY);
        assertEquals(1, exitNotification.getCount());
        assertEquals(1, idleNotification.getCount());

        Util.runAndWait(() -> {
            stage = makeStage();
            stage.show();
        });

        Util.sleep(SLEEP_TIME);
        assertEquals(1, exitNotification.getCount());
        assertEquals(1, idleNotification.getCount());

        final CountDownLatch rDone = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                if (throwableType == ThrowableType.EXCEPTION) {
                    throw new RuntimeException("this exception is expected");
                } else if (throwableType == ThrowableType.ERROR) {
                    throw new InternalError("this error is expected");
                }
            } finally {
                rDone.countDown();
            }
        });

        try {
            if (!rDone.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for runLater, throwableType = "
                        + throwableType);
            }
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected exception waiting for runLater, throwableType = "
                        + throwableType);
        }

        Util.runAndWait(stage::hide);

        try {
            if (!idleNotification.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for exit notification");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }
        assertEquals(0, idleNotification.getCount());
        assertEquals(implicit, implicitExit.get());

        Util.sleep(DELAY);
        assertEquals(1, exitNotification.getCount());
        assertEquals(1, exitLatch.getCount());

        PlatformImpl.removeListener(listener);
        listener = null;
    }

}
