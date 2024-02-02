/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.animation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import test.util.Util;

// Based on https://bugs.openjdk.org/browse/JDK-8159048
public class SynchronizationTest {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static Stage primaryStage;

    public static class TestApp extends Application {

        @Override
        public void start(Stage stage) throws Exception {
            primaryStage = stage;
            startupLatch.countDown();
        }
    }

    @BeforeAll
    public static void setup() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void shutdown() {
        Util.shutdown(primaryStage);
    }

    /**
     * Number of seconds to wait for a failure. If an exception is not thrown in this time, it's assumed it won't be
     * thrown later too.
     */
    private static final int GRACE_PERIOD = 15;

    final private AtomicBoolean failed = new AtomicBoolean(false);
    final private CountDownLatch waiter = new CountDownLatch(1);
    final private ExecutorService executor = Executors.newCachedThreadPool();

    private final AtomicReference<Thread> thread = new AtomicReference<>();
    private final AtomicReference<Throwable> throwable = new AtomicReference<>();

    protected void runTest(Runnable runnable) throws InterruptedException {
        Platform.runLater(() -> Thread.currentThread().setUncaughtExceptionHandler(this::handleThrowable));

        Runnable wrappedRunnable = wrap(runnable);

        for (int i = 0; i < 10; i++) {
            executor.submit(wrappedRunnable);
        }

        // If no exception is thrown after GRACE_PERIOD seconds, await completes and the test will succeed.
        // If an exception is thrown, await completes via countDown() instead and the test will fail.
        waiter.await(GRACE_PERIOD, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertFalse(failed.get(), "<" + throwable.get() + "> was thrown on " + thread.get());
    }

    private Runnable wrap(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                handleThrowable(Thread.currentThread(), e);
            }
        };
    }

    private void handleThrowable(Thread t, Throwable e) {
        thread.set(t);
        throwable.set(e);
        failed.set(true);
        waiter.countDown();
    }
}
