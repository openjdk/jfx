/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javafx.animation.AnimationTimer;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractPrimaryTimer;
import com.sun.scenario.animation.AbstractPrimaryTimerShim;
import com.sun.scenario.animation.shared.PulseReceiver;
import com.sun.scenario.animation.shared.TimerReceiver;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AbstractPrimaryTimerTest {

    private AbstractPrimaryTimerStub timer;


    @Before
    public void setUp() {
        timer = new AbstractPrimaryTimerStub();
    }

    @Test
    public void testPauseResume() {
        // pause timer
        timer.setNanos(2L);
        assertEquals(2L, timer.nanos());
        timer.pause();
        assertEquals(2L, timer.nanos());

        // test nanos during pause
        timer.setNanos(5L);
        assertEquals(2L, timer.nanos());

        // pause again
        timer.setNanos(10L);
        timer.pause();
        assertEquals(2L, timer.nanos());

        // resume
        timer.setNanos(17L);
        timer.resume();
        assertEquals(2L, timer.nanos());
        timer.setNanos(28L);
        assertEquals(13L, timer.nanos());

        // resume again
        timer.setNanos(41L);
        timer.resume();
        assertEquals(26L, timer.nanos());

        // pause again
        timer.setNanos(58L);
        assertEquals(43L, timer.nanos());
        timer.pause();
        assertEquals(43L, timer.nanos());

        // test nanos during pause
        timer.setNanos(77L);
        assertEquals(43L, timer.nanos());

        // resume
        timer.setNanos(100L);
        timer.resume();
        assertEquals(43L, timer.nanos());
        timer.setNanos(129L);
        assertEquals(72L, timer.nanos());
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

        private long nanos;
        private DelayedRunnable animationRunnable;

        public void setNanos(long nanos) {
            this.nanos = nanos;
        }

        public void simulatePulse() {
            if (animationRunnable != null) {
                animationRunnable.run();
            }
        }

        @Override public long nanos() {
            return AbstractPrimaryTimerShim.isPaused(this) ?
                    AbstractPrimaryTimerShim.getStartPauseTime(this) :
                    nanos - AbstractPrimaryTimerShim.getTotalPausedTime(this);
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

    };
}
