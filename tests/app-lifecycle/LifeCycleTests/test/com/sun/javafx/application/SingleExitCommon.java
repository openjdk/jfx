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
import javafx.application.Application;
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
 * Test program for Platform implicit exit behavior using the primary stage.
 * Each of the tests must be run in a separate JVM which is why each
 * is in its own subclass.
 */
public class SingleExitCommon {

    // Sleep time showing/hiding window in milliseconds
    private static final int SLEEP_TIME = 1000;

    // Used to launch the application before running any test
    private static final CountDownLatch initialized = new CountDownLatch(1);
    private static final CountDownLatch started = new CountDownLatch(1);
    private static final CountDownLatch stopped = new CountDownLatch(1);

    // Value of the implicit exit flag for the given test
    private static boolean implicitExit;

    // Flag indicating that the stage should be shown/hidden for the given test
    private static boolean stageShown;

    // Flag indicating that the app is expected to exit for the given test
    private static volatile boolean appShouldExit;

    // Singleton Application instance
    private static MyApp myApp;

    public enum ThrowableType {
        NONE,
        EXCEPTION,
        ERROR
    }

    // Application class. An instance is created and initialized as part of
    // running the test.
    public static class MyApp extends Application {
        private Stage primaryStage;

        @Override public void init() {
            assertEquals(1, initialized.getCount());
            assertEquals(1, started.getCount());
            assertEquals(1, stopped.getCount());

            SingleExitCommon.myApp = this;
            assertTrue(Platform.isImplicitExit());
            if (!implicitExit) {
                Platform.setImplicitExit(false);
                assertFalse(Platform.isImplicitExit());
            }
            initialized.countDown();
            assertEquals(0, initialized.getCount());
        }

        @Override public void start(Stage primaryStage) throws Exception {
            assertEquals(0, initialized.getCount());
            assertEquals(1, started.getCount());
            assertEquals(1, stopped.getCount());

            this.primaryStage = primaryStage;
            primaryStage.setTitle("Primary stage");
            Group root = new Group();
            Scene scene = new Scene(root);
            scene.setFill(Color.LIGHTYELLOW);
            primaryStage.setScene(scene);
            primaryStage.setX(0);
            primaryStage.setY(0);
            primaryStage.setWidth(210);
            primaryStage.setHeight(180);

            if (stageShown) {
                primaryStage.show();
            }

            started.countDown();
            assertEquals(0, started.getCount());
        }

        @Override public void stop() {
            if (appShouldExit) {
                assertEquals(0, initialized.getCount());
                assertEquals(0, started.getCount());
                assertEquals(1, stopped.getCount());

                stopped.countDown();
                assertEquals(0, stopped.getCount());
            } else {
                stopped.countDown();
                throw new AssertionFailedError("Unexpected call to stop method");
            }
        }
    }

    private void doTestCommon(boolean implicitExit,
            boolean reEnableImplicitExit, boolean stageShown,
            boolean appShouldExit) {

        doTestCommon(implicitExit, reEnableImplicitExit, stageShown,
                ThrowableType.NONE, appShouldExit);
    }

    private void doTestCommon(boolean implicitExit,
            boolean reEnableImplicitExit, boolean stageShown,
            final ThrowableType throwableType, boolean appShouldExit) {

        SingleExitCommon.implicitExit = implicitExit;
        SingleExitCommon.stageShown = stageShown;
        SingleExitCommon.appShouldExit = appShouldExit;

        final Throwable[] testError = new Throwable[1];
        final CountDownLatch latch = new CountDownLatch(1);
        final Thread testThread = Thread.currentThread();

        // Start the Application
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Application.launch(MyApp.class, (String[])null);
                    latch.countDown();
                } catch (Throwable th) {
                    testError[0] = th;
                    testThread.interrupt();
                }
            }
        }).start();

        try {
            if (!initialized.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to launch and initialize");
            }

            if (!started.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to start");
            }

            final CountDownLatch rDone = new CountDownLatch(1);
            Platform.runLater(new Runnable() {
                public void run() {
                    try {
                        if (throwableType == ThrowableType.EXCEPTION) {
                            throw new RuntimeException("this exception is expected");
                        } else if (throwableType == ThrowableType.ERROR) {
                            throw new InternalError("this error is expected");
                        }
                    } finally {
                        rDone.countDown();
                    }
                }
            });

            if (!rDone.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for runLater, throwableType = "
                        + throwableType);
            }

            if (stageShown) {
                Thread.sleep(SLEEP_TIME);
                Util.runAndWait(new Runnable() {
                    public void run() {
                        myApp.primaryStage.hide();
                    }
                });
            }

            final CountDownLatch exitLatch = PlatformImpl.test_getPlatformExitLatch();

            if (reEnableImplicitExit) {
                Thread.sleep(SLEEP_TIME);
                assertEquals(1, stopped.getCount());
                assertEquals(1, exitLatch.getCount());
                assertEquals(1, latch.getCount());
                assertFalse(Platform.isImplicitExit());
                Platform.setImplicitExit(true);
                assertTrue(Platform.isImplicitExit());
            }

            if (!appShouldExit) {
                Thread.sleep(SLEEP_TIME);
                assertEquals(1, stopped.getCount());
                assertEquals(1, exitLatch.getCount());
                assertEquals(1, latch.getCount());
                SingleExitCommon.appShouldExit = true;
                Platform.exit();
            }

            if (!stopped.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to stop");
            }

            if (!exitLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Platform to exit");
            }

            if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for launch to return");
            }
        } catch (InterruptedException ex) {
            Util.throwError(testError[0]);
        }
    }

    // ========================== TEST CASES ==========================

    // Implementation of SingleImplicitTest.testImplicitExit
    public void doTestImplicitExit() {
        // implicitExit, no re-enable, show stage, should exit
        doTestCommon(true, false, true, true);
    }

    // Implementation of testExplicitExit
    public void doTestExplicitExit() {
        // no implicitExit, no re-enable, show stage, should not exit
        doTestCommon(false, false, true, false);
    }

    // Implementation of testExplicitExitReEnable
    public void doTestExplicitExitReEnable() {
        // no implicitExit, re-enable, show stage, should exit
        doTestCommon(false, true, true, true);
    }

    // Implementation of testNoShowImplicit
    public void doTestNoShowImplicit() {
        // implicitExit, no re-enable, do not show stage, should not exit
        doTestCommon(true, false, false, false);
    }

    // Implementation of testNoShowExplicit
    public void doTestNoShowExplicit() {
        // no implicitExit, no re-enable, do not show stage, should not exit
        doTestCommon(false, false, false, false);
    }

    // Implementation of testNoShowExplicitReEnable
    public void doTestNoShowExplicitReEnable() {
        // no implicitExit, re-enable, do not show stage, should not exit
        doTestCommon(false, true, false, false);
    }

    // Implementation of SingleImplicitTest.testImplicitExit
    public void doTestImplicitExitWithException() {
        // implicitExit, no re-enable, show stage, throw exception, should exit
        doTestCommon(true, false, true, ThrowableType.EXCEPTION, true);
    }

    // Implementation of testExplicitExit
    public void doTestExplicitExitWithException() {
        // no implicitExit, no re-enable, show stage, throw exception, should not exit
        doTestCommon(false, false, true, ThrowableType.EXCEPTION, false);
    }

    // Implementation of testExplicitExitReEnable
    public void doTestExplicitExitReEnableWithException() {
        // no implicitExit, re-enable, show stage, throw exception, should exit
        doTestCommon(false, true, true, ThrowableType.EXCEPTION, true);
    }

    // Implementation of testNoShowImplicit
    public void doTestNoShowImplicitWithException() {
        // implicitExit, no re-enable, do not show stage, throw exception, should not exit
        doTestCommon(true, false, false, ThrowableType.EXCEPTION, false);
    }

    // Implementation of testNoShowExplicit
    public void doTestNoShowExplicitWithException() {
        // no implicitExit, no re-enable, do not show stage, throw exception, should not exit
        doTestCommon(false, false, false, ThrowableType.EXCEPTION, false);
    }

    // Implementation of testNoShowExplicitReEnable
    public void doTestNoShowExplicitReEnableWithException() {
        // no implicitExit, re-enable, do not show stage, throw exception, should not exit
        doTestCommon(false, true, false, ThrowableType.EXCEPTION, false);
    }

    // Implementation of SingleImplicitTest.testImplicitExit
    public void doTestImplicitExitWithError() {
        // implicitExit, no re-enable, show stage, throw exception, should exit
        doTestCommon(true, false, true, ThrowableType.ERROR, true);
    }

    // Implementation of testExplicitExit
    public void doTestExplicitExitWithError() {
        // no implicitExit, no re-enable, show stage, throw exception, should not exit
        doTestCommon(false, false, true, ThrowableType.ERROR, false);
    }

    // Implementation of testExplicitExitReEnable
    public void doTestExplicitExitReEnableWithError() {
        // no implicitExit, re-enable, show stage, throw exception, should exit
        doTestCommon(false, true, true, ThrowableType.ERROR, true);
    }

    // Implementation of testNoShowImplicit
    public void doTestNoShowImplicitWithError() {
        // implicitExit, no re-enable, do not show stage, throw exception, should not exit
        doTestCommon(true, false, false, ThrowableType.ERROR, false);
    }

    // Implementation of testNoShowExplicit
    public void doTestNoShowExplicitWithError() {
        // no implicitExit, no re-enable, do not show stage, throw exception, should not exit
        doTestCommon(false, false, false, ThrowableType.ERROR, false);
    }

    // Implementation of testNoShowExplicitReEnable
    public void doTestNoShowExplicitReEnableWithError() {
        // no implicitExit, re-enable, do not show stage, throw exception, should not exit
        doTestCommon(false, true, false, ThrowableType.ERROR, false);
    }

}
