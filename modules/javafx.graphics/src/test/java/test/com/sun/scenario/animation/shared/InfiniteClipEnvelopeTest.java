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

package test.com.sun.scenario.animation.shared;


import com.sun.javafx.tk.Toolkit;
import com.sun.scenario.animation.shared.ClipEnvelope;
import com.sun.scenario.animation.shared.FiniteClipEnvelope;
import com.sun.scenario.animation.shared.InfiniteClipEnvelopeShim;
import com.sun.scenario.animation.shared.SingleLoopClipEnvelope;
import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import test.javafx.animation.AnimationMock;
import test.javafx.animation.AnimationMock.Command;
import javafx.util.Duration;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InfiniteClipEnvelopeTest {

    private final long CYCLE_TICKS = Math.round(6.0 * AnimationMock.DEFAULT_DURATION.toMillis());

    private ClipEnvelope clip;
    private AnimationMock animation;

    @Before
    public void setUp() {
        animation = new AnimationMock(Toolkit.getToolkit().getPrimaryTimer(), AnimationMock.DEFAULT_DURATION, AnimationMock.DEFAULT_RATE, Animation.INDEFINITE, AnimationMock.DEFAULT_AUTOREVERSE);
        clip = new InfiniteClipEnvelopeShim(animation);
    }

    @Test
    public void testSetValues() {
        ClipEnvelope c;

        // Setting cycleCount to 2
        animation.setCycleCount(2);
        animation.mockCycleDuration(AnimationMock.DEFAULT_DURATION);
        c = clip.setCycleCount(2);
        assertNotSame(clip, c);
        assertTrue(c instanceof FiniteClipEnvelope);

        // Setting cycleDuration to INDEFINITE
        animation.setCycleCount(Animation.INDEFINITE);
        animation.mockCycleDuration(Duration.INDEFINITE);
        c = clip.setCycleDuration(Duration.INDEFINITE);
        assertNotSame(clip, c);
        assertTrue(c instanceof SingleLoopClipEnvelope);

    }

    @Test
    public void testJump() {
        clip.jumpTo(6 * 300);
        animation.check(Command.JUMP, 6 * 300, CYCLE_TICKS);

        clip.jumpTo(0);
        animation.check(Command.JUMP, 0, CYCLE_TICKS);

        clip.jumpTo(6 * 1000);
        animation.check(Command.JUMP, 6 * 1000, CYCLE_TICKS);

        clip.jumpTo(-1);
        animation.check(Command.JUMP, 0, CYCLE_TICKS);

        clip.jumpTo(6 * 1000 + 1);
        animation.check(Command.JUMP, 1, CYCLE_TICKS);
    }

    @Test
    public void testTimePulseForward() {
        animation.mockStatus(Status.RUNNING);
        clip.start();

        clip.timePulse(1);
        animation.check(Command.PLAY, 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 500);
        animation.check(Command.PLAY, 6 * 500, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 700);
        animation.check(Command.PLAY, 6 * 700, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000 - 1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000 + 1);
        animation.check(Command.PLAY, 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 2200);
        animation.check(Command.PLAY, 6 * 200, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 4300);
        animation.check(Command.PLAY, 6 * 300, CYCLE_TICKS);
        assertFalse(animation.finishCalled());
    }

    @Test
    public void testTimePulseBackward() {
        clip.setRate(-1.0);
        animation.mockStatus(Status.RUNNING);
        clip.start();

        clip.timePulse(1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 500);
        animation.check(Command.PLAY, 6 * 500, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 700);
        animation.check(Command.PLAY, 6 * 300, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000 - 1);
        animation.check(Command.PLAY, 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000 + 1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 2200);
        animation.check(Command.PLAY, 6 * 800, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 4300);
        animation.check(Command.PLAY, 6 * 700, CYCLE_TICKS);
        assertFalse(animation.finishCalled());
    }

    @Test
    public void testTimePulseForwardAutoReverse() {
        animation.mockStatus(Status.RUNNING);
        clip.setAutoReverse(true);
        clip.start();

        clip.timePulse(1);
        animation.check(Command.PLAY, 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 200);
        animation.check(Command.PLAY, 6 * 200, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000 - 1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000 + 1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // pulse one cycle ahead
        clip.timePulse(6 * 2200);
        animation.check(Command.PLAY, 6 * 200, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // pulse two cycles ahead
        clip.timePulse(6 * 4300);
        animation.check(Command.PLAY, 6 * 300, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // another pulse one cycle ahead
        clip.timePulse(6 * 5400);
        animation.check(Command.PLAY, 6 * 600, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // another pulse two cycles ahead
        clip.timePulse(6 * 7100);
        animation.check(Command.PLAY, 6 * 900, CYCLE_TICKS);
        assertFalse(animation.finishCalled());
    }

    @Test
    public void testTimePulseBackwardAutoReverse() {
        animation.mockStatus(Status.RUNNING);
        clip.setAutoReverse(true);
        clip.setRate(-1);
        clip.start();

        clip.timePulse(1);
        animation.check(Command.PLAY, 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 200);
        animation.check(Command.PLAY, 6 * 200, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000 - 1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000 + 1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // pulse one cycle ahead
        clip.timePulse(6 * 2200);
        animation.check(Command.PLAY, 6 * 200, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // pulse two cycles ahead
        clip.timePulse(6 * 4300);
        animation.check(Command.PLAY, 6 * 300, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // another pulse one cycle ahead
        clip.timePulse(6 * 5400);
        animation.check(Command.PLAY, 6 * 600, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // another pulse two cycles ahead
        clip.timePulse(6 * 7100);
        animation.check(Command.PLAY, 6 * 900, CYCLE_TICKS);
        assertFalse(animation.finishCalled());
    }

    @Test
    public void testJumpAndPulseForward() {
        animation.mockStatus(Status.RUNNING);
        clip.start();

        clip.jumpTo(6 * 300);
        animation.check(Command.JUMP, 6 * 300, CYCLE_TICKS);

        clip.timePulse(6 * 800);
        animation.check(Command.PLAY, 6 * 100, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.jumpTo(6 * 50);
        animation.check(Command.JUMP, 6 * 50, CYCLE_TICKS);

        clip.timePulse(6 * 1850);
        animation.check(Command.PLAY, 6 * 100, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.jumpTo(6 * 0);
        animation.check(Command.JUMP, 6 * 0, CYCLE_TICKS);

        clip.timePulse(6 * 2000);
        animation.check(Command.PLAY, 6 * 150, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.jumpTo(6 * 1000);
        animation.check(Command.JUMP, 6 * 1000, CYCLE_TICKS);

        clip.timePulse(6 * 2200);
        animation.check(Command.PLAY, 6 * 200, CYCLE_TICKS);
        assertFalse(animation.finishCalled());
    }

    @Test
    public void testJumpAndPulseBackward() {
        animation.mockStatus(Status.RUNNING);
        clip.setRate(-1);
        clip.start();

        // jump forward
        clip.jumpTo(6 * 300);
        animation.check(Command.JUMP, 6 * 300, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // pulse in next cycle
        clip.timePulse(6 * 700);
        animation.check(Command.PLAY, 6 * 600, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // jump backward
        clip.jumpTo(6 * 900);
        animation.check(Command.JUMP, 6 * 900, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // pulse one cycle ahead
        clip.timePulse(6 * 1750);
        animation.check(Command.PLAY, 6 * 850, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // jump to same position at end
        clip.jumpTo(6 * 1000);
        animation.check(Command.JUMP, 6 * 1000, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // normal pulse
        clip.timePulse(6 * 2000);
        animation.check(Command.PLAY, 6 * 750, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // jump to start
        clip.jumpTo(0);
        animation.check(Command.JUMP, 0, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // normal pulse
        clip.timePulse(6 * 2200);
        animation.check(Command.PLAY, 6 * 800, CYCLE_TICKS);
        assertFalse(animation.finishCalled());
    }

    @Test
    public void testJumpAndPulseForwardAutoReverse() {
        animation.mockStatus(Status.RUNNING);
        clip.setAutoReverse(true);
        clip.start();

        // jump forward
        clip.jumpTo(6 * 300);
        animation.check(Command.JUMP, 6 * 300, CYCLE_TICKS);

        // pulse in next cycle
        clip.timePulse(6 * 900);
        animation.check(Command.PLAY, 6 * 800, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // jump backward
        clip.jumpTo(6 * 900);
        animation.check(Command.JUMP, 6 * 900, CYCLE_TICKS);

        // pulse one cycle ahead
        clip.timePulse(6 * 1850);
        animation.check(Command.PLAY, 6 * 150, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // jump to start
        clip.jumpTo(6 * 0);
        animation.check(Command.JUMP, 0, CYCLE_TICKS);

        // normal pulse
        clip.timePulse(6 * 2000);
        animation.check(Command.PLAY, 6 * 150, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // jump to end
        clip.jumpTo(6 * 1000);
        animation.check(Command.JUMP, 6 * 1000, CYCLE_TICKS);

        // normal pulse
        clip.timePulse(6 * 2200);
        animation.check(Command.PLAY, 6 * 800, CYCLE_TICKS);
        assertFalse(animation.finishCalled());
    }

    @Test
    public void testJumpAndPulseBackwardAutoReverse() {
        animation.mockStatus(Status.RUNNING);
        clip.setAutoReverse(true);
        clip.setRate(-1);
        clip.start();

        // jump forward
        clip.jumpTo(6 * 300);
        animation.check(Command.JUMP, 6 * 300, CYCLE_TICKS);

        // pulse in next cycle
        clip.timePulse(6 * 700);
        animation.check(Command.PLAY, 6 * 400, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // jump backward
        clip.jumpTo(6 * 100);
        animation.check(Command.JUMP, 6 * 100, CYCLE_TICKS);

        // pulse one cycle ahead
        clip.timePulse(6 * 1800 - 1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());
        clip.timePulse(6 * 1800 + 1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // jump to end
        clip.jumpTo(6 * 1000);
        animation.check(Command.JUMP, 6 * 1000, CYCLE_TICKS);

        // normal pulse
        clip.timePulse(6 * 2000);
        animation.check(Command.PLAY, 6 * 800 + 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        // jump to start
        clip.jumpTo(0);
        animation.check(Command.JUMP, 0, CYCLE_TICKS);

        // normal pulse
        clip.timePulse(6 * 2200);
        animation.check(Command.PLAY, 6 * 200, CYCLE_TICKS);
        assertFalse(animation.finishCalled());
    }

    @Test
    public void testRate() {
        animation.mockStatus(Status.RUNNING);
        clip.setRate(0.5);
        clip.start();

        clip.timePulse(6 * 200);
        animation.check(Command.PLAY, 6 * 100, CYCLE_TICKS);

        clip.setRate(3.0);
        clip.timePulse(6 * 300);
        animation.check(Command.PLAY, 6 * 400, CYCLE_TICKS);

        clip.setRate(2.0);
        clip.timePulse(6 * 500);
        animation.check(Command.PLAY, 6 * 800, CYCLE_TICKS);

        clip.setRate(-0.5);
        clip.timePulse(6 * 1100);
        animation.check(Command.PLAY, 6 * 500, CYCLE_TICKS);

        clip.setRate(-3.0);
        clip.timePulse(6 * 1200);
        animation.check(Command.PLAY, 6 * 200, CYCLE_TICKS);

        clip.setRate(0.5);
        clip.timePulse(6 * 2100);
        animation.check(Command.PLAY, 6 * 650, CYCLE_TICKS);

        clip.setRate(1.5);
        clip.timePulse(6 * 2600);
        animation.check(Command.PLAY, 6 * 400, CYCLE_TICKS);

        clip.setRate(-2.0);
        clip.timePulse(6 * 3000);
        animation.check(Command.PLAY, 6 * 600, CYCLE_TICKS);
    }

    @Test
    public void testRateAutoReverse() {
        animation.mockStatus(Status.RUNNING);
        clip.setAutoReverse(true);
        clip.setRate(0.5);
        clip.start();

        clip.timePulse(6 * 200);
        animation.check(Command.PLAY, 6 * 100, CYCLE_TICKS);

        clip.setRate(3.0);
        clip.timePulse(6 * 300);
        animation.check(Command.PLAY, 6 * 400, CYCLE_TICKS);

        clip.setRate(2.0);
        clip.timePulse(6 * 500);
        animation.check(Command.PLAY, 6 * 800, CYCLE_TICKS);

        clip.setRate(-0.5);
        clip.timePulse(6 * 1100);
        animation.check(Command.PLAY, 6 * 500, CYCLE_TICKS);

        clip.setRate(-3.0);
        clip.timePulse(6 * 1200);
        animation.check(Command.PLAY, 6 * 200, CYCLE_TICKS);

        clip.setRate(0.5);
        clip.timePulse(6 * 2100);
        animation.check(Command.PLAY, 6 * 650, CYCLE_TICKS);

        clip.setRate(1.5);
        clip.timePulse(6 * 2600);
        animation.check(Command.PLAY, 6 * 600, CYCLE_TICKS);

        clip.setRate(-2.0);
        clip.timePulse(6 * 3000);
        animation.check(Command.PLAY, 6 * 600, CYCLE_TICKS);
    }
}
