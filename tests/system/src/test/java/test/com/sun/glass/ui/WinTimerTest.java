/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.glass.ui;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Timer;
import com.sun.javafx.PlatformUtil;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class WinTimerTest {

    private static final int TIMEOUT = 10000;
    private static final int TIMED_WAIT = 4000;

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    private final CountDownLatch callbackEntered = new CountDownLatch(1);
    private final CountDownLatch releaseCallback = new CountDownLatch(1);
    private final CountDownLatch aboutToStop = new CountDownLatch(1);
    private final CountDownLatch stopped = new CountDownLatch(1);

    private final AtomicBoolean firstTime = new AtomicBoolean(true);
    private final AtomicBoolean checkPause = new AtomicBoolean(false);
    private Timer timer;

    final AtomicReference<Throwable> throwable = new AtomicReference<>(null);

    @BeforeAll
    public static void setup() throws Exception {
        assumeTrue(PlatformUtil.isWindows());
        Util.startup(startupLatch, () -> startupLatch.countDown());
    }

    @AfterAll
    public static void cleanup() {
        assumeTrue(PlatformUtil.isWindows());
        Util.shutdown();
    }

    // Sets the throwable if not already set
    private void setThrowable(Throwable throwable) {
        this.throwable.compareAndSet(null, throwable);
    }

    /*
     * Method that is called by the timer as a callback on the timer thread.
     * Do the following:
     *   1. Signal that the timer thread has entered its callback
     *   2. wait for the main thread to release the callback latch
     *   3. call pause (skipped if an earlier error is detected)
     *
     * This method records an exception or error if caught
     */
    private void timerCallback() {
        if (!firstTime.getAndSet(false)) {
            return;
        }

        callbackEntered.countDown();
        try {
            assertTrue(releaseCallback.await(TIMEOUT, TimeUnit.MILLISECONDS),
                    "Callback: timeout waiting for release notification");
            // pause() is a no-op on Windows, but we call it to ensure no deadlock
            if (checkPause.get()) {
                timer.pause();
            }
        } catch (Throwable t) {
            setThrowable(t);
        }
    }

    /*
     * @test
     * @bug 8389783
     *
     * Tests the Windows native Timer life-cycle. The test does the following:
     *   1. Create a new Timer object with a callback
     *   2. Start the timer with the minimum timer period
     *   3. Wait for the callback to be called
     *   4. Start a new "stopper" thread that stops the timer via timer.stop()
     *   5. Wait for up to 4 seconds for a signal that timer.stop() has returned
     *      (it should not return until the callback does)
     *   6. release the callback
     *   7. Wait for a signal that timer.stop() has returned (this time it should)
     *   8. Join the "stopper" thread and verify that it is terminated
     *   9. Check for any recorded exceptions from the callback or the stopper thread
     */
    @Test
    public void timerLifeCycleTest() throws Exception {
        Application application = Application.GetApplication();
        timer = application.createTimer(this::timerCallback);
        timer.start(Timer.getMinPeriod());
        assertTrue(timer.isRunning());

        /*
         * Thread to stop the timer. It does the following:
         *   1. Signal that the thread is about to call timer.stop()
         *   2. Call timer.stop();
         *   3. Signal that timer.stop() has completed.
         *
         * This thread records an exception or error if caught
         */
        Thread stopperThread = new Thread(() -> {
            aboutToStop.countDown();
            try {
                timer.stop();
            } catch (Throwable t) {
                setThrowable(t);
            } finally {
                stopped.countDown();
            }
        });
        stopperThread.setDaemon(true);

        try {
            try {
                assertTrue(callbackEntered.await(TIMEOUT, TimeUnit.MILLISECONDS),
                        "Timeout waiting for callback to run");
                stopperThread.start();
                assertTrue(aboutToStop.await(TIMEOUT, TimeUnit.MILLISECONDS),
                        "Timeout waiting for stopper thread to run");
                // Check for early release
                boolean stoppedEarly = stopped.await(TIMED_WAIT, TimeUnit.MILLISECONDS);
                assertNull(throwable.get());
                assertFalse(stoppedEarly,
                        "Timer.stop completed while callback still running");
                checkPause.set(true);
            } finally {
                releaseCallback.countDown();
            }
            boolean stoppedAfterRelease = stopped.await(TIMEOUT, TimeUnit.MILLISECONDS);
            assertNull(throwable.get());
            assertTrue(stoppedAfterRelease, "Timeout waiting for stop to return");
            assertFalse(timer.isRunning());
        } finally {
            stopperThread.join(TIMEOUT);
        }
        assertNull(throwable.get());
        assertFalse(stopperThread.isAlive(), "Stopper thread is still running");
    }
}
