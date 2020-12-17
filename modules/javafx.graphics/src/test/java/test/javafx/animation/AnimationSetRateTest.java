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

package test.javafx.animation;


import javafx.animation.Animation.Status;
import javafx.util.Duration;
import test.com.sun.scenario.animation.shared.ClipEnvelopeMock;
import org.junit.Before;
import org.junit.Test;

import com.sun.scenario.animation.shared.SingleLoopClipEnvelopeShim;

import static org.junit.Assert.*;

public class AnimationSetRateTest {

    private static final double EPSILON = 1e-12;

    private AbstractPrimaryTimerMock timer;
    private AnimationImpl animation;
    private ClipEnvelopeMock clipEnvelope;

    @Before
    public void setUp() throws Exception {
        timer = new AbstractPrimaryTimerMock();
        clipEnvelope = new ClipEnvelopeMock();
        animation = new AnimationImpl(timer, clipEnvelope, 1);
        animation.shim_setCycleDuration(Duration.millis(1000));
        clipEnvelope.setAnimation(animation);
    }

    private void assertAnimation(double rate, double currentRate, Status status, boolean addedToPrimaryTimer) {
        assertEquals(rate, animation.getRate(), EPSILON);
        assertEquals(currentRate, animation.getCurrentRate(), EPSILON);
        assertEquals(status, animation.getStatus());
        assertEquals(addedToPrimaryTimer, timer.containsPulseReceiver(animation.shim_pulseReceiver()));
    }

    @Test
    public void testSetRate() {
        // changing the rate of a playing animation
        animation.play();
        animation.setRate(3.0);
        assertAnimation(3.0, 3.0, Status.RUNNING, true);

        // toggling a playing animation
        animation.setRate(-2.0);
        assertAnimation(-2.0, -2.0, Status.RUNNING, true);

        // changing the rate
        animation.setRate(-2.5);
        assertAnimation(-2.5, -2.5, Status.RUNNING, true);

        // toggling back
        animation.setRate(1.5);
        assertAnimation(1.5, 1.5, Status.RUNNING, true);

        // changing the rate of a animation playing in reverse
        animation.setCurrentRate(-1.5);
        animation.setRate(2.2);
        assertAnimation(2.2, -2.2, Status.RUNNING, true);

        // toggling a animation playing in reverse
        animation.setRate(-1.8);
        assertAnimation(-1.8, 1.8, Status.RUNNING, true);

        // changing the rate
        animation.setRate(-1.3);
        assertAnimation(-1.3, 1.3, Status.RUNNING, true);

        // toggling back
        animation.setRate(0.5);
        assertAnimation(0.5, -0.5, Status.RUNNING, true);
    }

    @Test
    public void testSetRateOfStoppedAnimation() {
        // changing the rate
        animation.setRate(2.0);
        assertAnimation(2.0, 0.0, Status.STOPPED, false);
        animation.play();
        assertAnimation(2.0, 2.0, Status.RUNNING, true);

        // toggling the rate of a stopped animation
        animation.stop();
        animation.setRate(-1.0);
        assertAnimation(-1.0, 0.0, Status.STOPPED, false);
        animation.play();
        assertAnimation(-1.0, -1.0, Status.RUNNING, true);

        // toggling back
        animation.stop();
        animation.setRate(3.0);
        assertAnimation(3.0, 0.0, Status.STOPPED, false);
        animation.play();
        assertAnimation(3.0, 3.0, Status.RUNNING, true);

        // setting rate of stopped animation to zero
        animation.stop();
        animation.setRate(0);
        assertAnimation(0.0, 0.0, Status.STOPPED, false);
        animation.play();
        assertAnimation(0.0, 0.0, Status.RUNNING, false);

        // setting rate of stopped animation to non-zero
        animation.stop();
        animation.setRate(1.5);
        assertAnimation(1.5, 0.0, Status.STOPPED, false);
        animation.play();
        assertAnimation(1.5, 1.5, Status.RUNNING, true);

        // setting rate of stopped animation to zero
        animation.stop();
        animation.setRate(0);
        assertAnimation(0.0, 0.0, Status.STOPPED, false);
        animation.play();
        assertAnimation(0.0, 0.0, Status.RUNNING, false);

        // toggling rate of stopped animation to non-zero
        animation.stop();
        animation.setRate(-0.5);
        assertAnimation(-0.5, 0.0, Status.STOPPED, false);
        animation.play();
        assertAnimation(-0.5, -0.5, Status.RUNNING, true);

        // setting rate of stopped animation to zero
        animation.stop();
        animation.setRate(0);
        assertAnimation(0.0, 0.0, Status.STOPPED, false);
        animation.play();
        assertAnimation(0.0, 0.0, Status.RUNNING, false);

        // setting rate of stopped animation to non-zero
        animation.stop();
        animation.setRate(-2.3);
        assertAnimation(-2.3, 0.0, Status.STOPPED, false);
        animation.play();
        assertAnimation(-2.3, -2.3, Status.RUNNING, true);

        // setting rate of stopped animation to zero
        animation.stop();
        animation.setRate(0);
        assertAnimation(0.0, 0.0, Status.STOPPED, false);
        animation.play();
        assertAnimation(0.0, 0.0, Status.RUNNING, false);

        // toggling rate of stopped animation to non-zero
        animation.stop();
        animation.setRate(1.7);
        assertAnimation(1.7, 0.0, Status.STOPPED, false);
        animation.play();
        assertAnimation(1.7, 1.7, Status.RUNNING, true);
    }

    @Test
    public void testSetRateToZeroForRunningAnimation() {
        // changing the rate of a playing animation
        animation.play();
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.RUNNING, false);
        animation.setRate(3.0);
        assertAnimation(3.0, 3.0, Status.RUNNING, true);

        // toggling a playing animation
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.RUNNING, false);
        animation.setRate(-2.0);
        assertAnimation(-2.0, -2.0, Status.RUNNING, true);

        // changing the rate
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.RUNNING, false);
        animation.setRate(-2.5);
        assertAnimation(-2.5, -2.5, Status.RUNNING, true);

        // toggling back
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.RUNNING, false);
        animation.setRate(1.5);
        assertAnimation(1.5, 1.5, Status.RUNNING, true);

        // changing the rate of a animation playing in reverse
        animation.setCurrentRate(-1.5);
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.RUNNING, false);
        animation.setRate(2.2);
        assertAnimation(2.2, -2.2, Status.RUNNING, true);

        // toggling a animation playing in reverse
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.RUNNING, false);
        animation.setRate(-1.8);
        assertAnimation(-1.8, 1.8, Status.RUNNING, true);

        // changing the rate
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.RUNNING, false);
        animation.setRate(-1.3);
        assertAnimation(-1.3, 1.3, Status.RUNNING, true);

        // toggling back
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.RUNNING, false);
        animation.setRate(0.5);
        assertAnimation(0.5, -0.5, Status.RUNNING, true);
    }

    @Test
    public void testSetRateOfPausedAnimation() {
        // changing the rate of a paused animation
        animation.play();
        animation.pause();
        animation.setRate(3.0);
        assertAnimation(3.0, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(3.0, 3.0, Status.RUNNING, true);

        // toggling a pausing animation
        animation.pause();
        animation.setRate(-2.0);
        assertAnimation(-2.0, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(-2.0, -2.0, Status.RUNNING, true);

        // changing the rate
        animation.pause();
        animation.setRate(-2.5);
        assertAnimation(-2.5, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(-2.5, -2.5, Status.RUNNING, true);

        // toggling back
        animation.pause();
        animation.setRate(1.5);
        assertAnimation(1.5, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(1.5, 1.5, Status.RUNNING, true);

        // changing the rate of a paused animation pointing in reverse
        animation.setCurrentRate(-1.5);
        animation.pause();
        animation.setRate(2.2);
        assertAnimation(2.2, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(2.2, -2.2, Status.RUNNING, true);

        // toggling a paused playing pointing in reverse
        animation.pause();
        animation.setRate(-1.8);
        assertAnimation(-1.8, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(-1.8, 1.8, Status.RUNNING, true);

        // changing the rate
        animation.pause();
        animation.setRate(-1.3);
        assertAnimation(-1.3, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(-1.3, 1.3, Status.RUNNING, true);

        // toggling back
        animation.pause();
        animation.setRate(0.5);
        assertAnimation(0.5, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(0.5, -0.5, Status.RUNNING, true);
    }

    @Test
    public void testSetRateToZeroForPausedAnimation() {
        // starting a paused animation with rate 0
        animation.play();
        animation.pause();
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(0.0, 0.0, Status.RUNNING, false);

        // changing the rate of a paused animation
        animation.pause();
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.PAUSED, false);
        animation.setRate(3.0);
        assertAnimation(3.0, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(3.0, 3.0, Status.RUNNING, true);

        // toggling a paused animation
        animation.pause();
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.PAUSED, false);
        animation.setRate(-2.0);
        assertAnimation(-2.0, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(-2.0, -2.0, Status.RUNNING, true);

        // changing the rate
        animation.pause();
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.PAUSED, false);
        animation.setRate(-2.5);
        assertAnimation(-2.5, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(-2.5, -2.5, Status.RUNNING, true);

        // toggling back
        animation.pause();
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.PAUSED, false);
        animation.setRate(1.5);
        assertAnimation(1.5, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(1.5, 1.5, Status.RUNNING, true);

        // changing the rate of a paused animation pointing in reverse
        animation.setCurrentRate(-1.5);
        animation.pause();
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.PAUSED, false);
        animation.setRate(2.2);
        assertAnimation(2.2, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(2.2, -2.2, Status.RUNNING, true);

        // toggling a paused animation pointing in reverse
        animation.pause();
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.PAUSED, false);
        animation.setRate(-1.8);
        assertAnimation(-1.8, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(-1.8, 1.8, Status.RUNNING, true);

        // changing the rate
        animation.pause();
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.PAUSED, false);
        animation.setRate(-1.3);
        assertAnimation(-1.3, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(-1.3, 1.3, Status.RUNNING, true);

        // toggling back
        animation.pause();
        animation.setRate(0.0);
        assertAnimation(0.0, 0.0, Status.PAUSED, false);
        animation.setRate(0.5);
        assertAnimation(0.5, 0.0, Status.PAUSED, false);
        animation.play();
        assertAnimation(0.5, -0.5, Status.RUNNING, true);
    }

    @Test
    public void testFlipRateAndPlayForPausedNonEmbeddedAnimation() {
        var clip = new SingleLoopClipEnvelopeShim(animation);
        animation.setClipEnvelope(clip);
        animation.setRate(0.2);
        animation.play();
        clip.timePulse(10);
        animation.pause();
        long timeBefore = clip.getTicks();
        animation.setRate(-0.2);
        animation.play();
        clip.timePulse(5);
        animation.pause();
        long timeAfter = clip.getTicks();
        assertEquals("A pulse to 10 at rate 0.2 with deltaTicks = 0 should reach 10 * 0.2 = 2", 2, timeBefore);
        assertEquals("A pulse to 5 at rate -0.2 with deltaTicks = 4 should reach 4 + 5 * (-0.2) = 3", 3, timeAfter);
    }
}
