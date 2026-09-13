/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import javafx.animation.AnimationTimer;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractPrimaryTimer;
import com.sun.scenario.animation.shared.PulseReceiver;
import com.sun.scenario.animation.shared.TimerReceiver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractPrimaryTimerTest {

    private AbstractPrimaryTimerStub timer;


    @BeforeEach
    public void setUp() {
        timer = new AbstractPrimaryTimerStub();
    }

    @Test
    public void testPulseReceiver() {
        final Flag flag = new Flag();

        final PulseReceiver pulseReceiver = now -> flag.flag();

        // add PulseReceiver
        timer.addPulseReceiver(pulseReceiver);
        timer.simulatePulse();
        assertTrue(flag.isFlagged());

        // remove PulseReceiver
        flag.unflag();
        timer.removePulseReceiver(pulseReceiver);
        timer.simulatePulse();
        assertFalse(flag.isFlagged());
    }

    @Test
    public void testAnimationTimers() {
        final Flag flag = new Flag();

        final AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                flag.flag();
            }
        };

        final TimerReceiver timerReceiver = l -> animationTimer.handle(l);

        // add AnimationTimer
        timer.addAnimationTimer(timerReceiver);
        timer.simulatePulse();
        assertTrue(flag.isFlagged());

        // remove AnimationTimer
        flag.unflag();
        timer.removeAnimationTimer(timerReceiver);
        timer.simulatePulse();
        assertFalse(flag.isFlagged());
    }

    @Test
    public void testExceptionInAnimationTimerIsHandledInPrimaryTimer() {
        Thread currentThread = Thread.currentThread();
        String currentMethodName = currentThread.getStackTrace()[0].getMethodName();
        Throwable[] uncaughtException = new Exception[1];
        Thread.UncaughtExceptionHandler exceptionHandler = currentThread.getUncaughtExceptionHandler();
        currentThread.setUncaughtExceptionHandler((_, e) -> uncaughtException[0] = e);

        try {
            var timer1 = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    throw new RuntimeException(currentMethodName);
                }
            };

            var flag = new Flag();
            var timer2 = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    flag.flag();
                }
            };

            timer.addAnimationTimer(timer1::handle);
            timer.addAnimationTimer(timer2::handle);
            assertFalse(flag.isFlagged());

            timer.simulatePulse();
            assertTrue(flag.isFlagged());
            assertEquals(currentMethodName, uncaughtException[0].getMessage());
        } finally {
            currentThread.setUncaughtExceptionHandler(exceptionHandler);
        }
    }

    @Test
    public void testExceptionInPulseReceiverIsHandledInPrimaryTimer() {
        Thread currentThread = Thread.currentThread();
        String currentMethodName = currentThread.getStackTrace()[0].getMethodName();
        Throwable[] uncaughtException = new Exception[1];
        Thread.UncaughtExceptionHandler exceptionHandler = currentThread.getUncaughtExceptionHandler();
        currentThread.setUncaughtExceptionHandler((_, e) -> uncaughtException[0] = e);

        try {
            var receiver1 = new PulseReceiver() {
                @Override
                public void timePulse(long now) {
                    throw new RuntimeException(currentMethodName);
                }
            };

            var flag = new Flag();
            var receiver2 = new PulseReceiver() {
                @Override
                public void timePulse(long now) {
                    flag.flag();
                }
            };

            timer.addPulseReceiver(receiver1);
            timer.addPulseReceiver(receiver2);
            assertFalse(flag.isFlagged());

            timer.simulatePulse();
            assertTrue(flag.isFlagged());
            assertEquals(currentMethodName, uncaughtException[0].getMessage());
        } finally {
            currentThread.setUncaughtExceptionHandler(exceptionHandler);
        }
    }

    @Test
    public void testExceptionsInNoisyFailingAnimationTimerAreNotReported() {
        Thread currentThread = Thread.currentThread();
        String currentMethodName = currentThread.getStackTrace()[0].getMethodName();
        List<Throwable> uncaughtExceptions = new ArrayList<>();
        Thread.UncaughtExceptionHandler exceptionHandler = currentThread.getUncaughtExceptionHandler();
        currentThread.setUncaughtExceptionHandler((_, e) -> uncaughtExceptions.add(e));

        try {
            var animTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    throw new RuntimeException(currentMethodName);
                }
            };

            timer.addAnimationTimer(animTimer::handle);

            for (int i = 0; i < AbstractPrimaryTimer.FAILING_TIMER_THRESHOLD; ++i) {
                timer.simulatePulse();
            }

            assertEquals(AbstractPrimaryTimer.FAILING_TIMER_THRESHOLD, uncaughtExceptions.size());
            assertTrue(uncaughtExceptions.stream().allMatch(e -> e.getMessage().equals(currentMethodName)));

            // The following exceptions are not reported
            for (int i = 0; i < 3; ++i) {
                timer.simulatePulse();
                assertEquals(AbstractPrimaryTimer.FAILING_TIMER_THRESHOLD, uncaughtExceptions.size());
            }
        } finally {
            currentThread.setUncaughtExceptionHandler(exceptionHandler);
        }
    }

    @Test
    public void testExceptionsInNoisyFailingPulseReceiverAreNotReported() {
        Thread currentThread = Thread.currentThread();
        String currentMethodName = currentThread.getStackTrace()[0].getMethodName();
        List<Throwable> uncaughtExceptions = new ArrayList<>();
        Thread.UncaughtExceptionHandler exceptionHandler = currentThread.getUncaughtExceptionHandler();
        currentThread.setUncaughtExceptionHandler((_, e) -> uncaughtExceptions.add(e));

        try {
            var receiver = new PulseReceiver() {
                @Override
                public void timePulse(long now) {
                    throw new RuntimeException(currentMethodName);
                }
            };

            timer.addPulseReceiver(receiver);

            for (int i = 0; i < AbstractPrimaryTimer.FAILING_TIMER_THRESHOLD; ++i) {
                timer.simulatePulse();
            }

            assertEquals(AbstractPrimaryTimer.FAILING_TIMER_THRESHOLD, uncaughtExceptions.size());
            assertTrue(uncaughtExceptions.stream().allMatch(e -> e.getMessage().equals(currentMethodName)));

            // The following exceptions are not reported
            for (int i = 0; i < 3; ++i) {
                timer.simulatePulse();
                assertEquals(AbstractPrimaryTimer.FAILING_TIMER_THRESHOLD, uncaughtExceptions.size());
            }
        } finally {
            currentThread.setUncaughtExceptionHandler(exceptionHandler);
        }
    }

    private static class Flag {

        private boolean flagged;

        public void flag() {
            flagged = true;
        }

        public void unflag() {
            flagged = false;
        }

        public boolean isFlagged() {
            return flagged;
        }
    }

    private static class AbstractPrimaryTimerStub extends AbstractPrimaryTimer {

        private DelayedRunnable animationRunnable;

        public void simulatePulse() {
            if (animationRunnable != null) {
                animationRunnable.run();
            }
        }

        @Override
        protected void postUpdateAnimationRunnable(
                DelayedRunnable animationRunnable) {
            this.animationRunnable = animationRunnable;
        }

        @Override
        protected int getPulseDuration(int precision) {
            return precision / 60;
        }

    }
}
