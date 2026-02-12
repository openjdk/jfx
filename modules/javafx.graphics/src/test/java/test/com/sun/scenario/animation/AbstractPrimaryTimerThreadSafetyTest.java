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

package test.com.sun.scenario.animation;

import com.sun.scenario.animation.shared.TimerReceiver;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for thread-safety of AbstractPrimaryTimer.addAnimationTimer().
 *
 * Without synchronization, concurrent calls to addAnimationTimer() corrupt
 * the internal array, causing lost timers (that never fire) or
 * NullPointerException during pulse iteration.
 */
public class AbstractPrimaryTimerThreadSafetyTest {

    /**
     * Multiple threads simultaneously call addAnimationTimer(). Without
     * synchronization, some timers are silently lost due to array corruption.
     *
     * Repeated 100 times to reliably trigger the race condition.
     */
    @Test
    public void testConcurrentAddAnimationTimer() throws Exception {
        final int ATTEMPTS = 100;
        int failures = 0;

        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            final int THREAD_COUNT = 8;
            final var timer = new AbstractPrimaryTimerTest.AbstractPrimaryTimerStub();
            final AtomicInteger firedCount = new AtomicInteger(0);
            final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);

            TimerReceiver[] receivers = new TimerReceiver[THREAD_COUNT];
            Thread[] threads = new Thread[THREAD_COUNT];

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int index = i;
                receivers[i] = now -> firedCount.incrementAndGet();
                threads[i] = new Thread(() -> {
                    try {
                        barrier.await();
                        timer.addAnimationTimer(receivers[index]);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                threads[i].start();
            }

            for (Thread t : threads) {
                t.join(5000);
            }

            timer.simulatePulse();

            if (firedCount.get() != THREAD_COUNT) {
                failures++;
            }
        }

        assertEquals(0, failures,
                "Some AnimationTimers were lost due to concurrent addAnimationTimer(). " +
                        "Failed " + failures + " out of " + ATTEMPTS + " attempts."
        );
    }
}
