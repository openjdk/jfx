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

package com.sun.javafx.application;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import junit.framework.AssertionFailedError;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import util.Util;

import static org.junit.Assert.*;
import static util.Util.TIMEOUT;

/**
 * Unit tests for Platform runLater.
 */
public class RunLaterTest {

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {
        @Override public void start(Stage primaryStage) throws Exception {
            Platform.setImplicitExit(false);
            launchLatch.countDown();
        }
    }


    @BeforeClass
    public static void setupOnce() {
        // Start the Application
        new Thread(() -> Application.launch(MyApp.class, (String[])null)).start();

        try {
            if (!launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to launch");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.exit();
    }

    private AtomicInteger seqNum = new AtomicInteger(0);

    protected void doTestRunLater(final int numRunnables) {
        final boolean DELAY = true;

        Runnable[] runnables = new Runnable[numRunnables];
        for (int i = 0; i < numRunnables; i++) {
            final int idx = i;
            runnables[idx] = () -> {
                if (idx == 0) {
                    Util.sleep(100);
                }
                int seq = seqNum.getAndIncrement();
                assertEquals(idx, seq);
            };
        }
        Util.runAndWait(DELAY, runnables);
        assertEquals(numRunnables, seqNum.get());
    }

    @Test
    public void testRunLater1() {
       doTestRunLater(1);
    }

    @Test
    public void testRunLater2() {
       doTestRunLater(2);
    }

    @Test
    public void testRunLater10() {
       doTestRunLater(10);
    }

    @Test
    public void testRunLater100() {
       doTestRunLater(100);
    }

    @Test
    public void testRunLater1000() {
       doTestRunLater(1000);
    }

    @Test
    public void testRunLater10000() {
       doTestRunLater(10000);
    }

    @Test
    public void testRunLater15000() {
       doTestRunLater(15000);
    }

    @Test
    public void testRunLater20000() {
       doTestRunLater(20000);
    }

}
