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
import com.sun.scenario.animation.shared.SingleLoopClipEnvelopeShim;
import javafx.animation.Animation.Status;
import test.javafx.animation.AnimationMock;
import test.javafx.animation.AnimationMock.Command;
import javafx.util.Duration;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SingleLoopClipEnvelopeTest {


    private final long CYCLE_TICKS = Math.round(6.0 * AnimationMock.DEFAULT_DURATION.toMillis());

    private ClipEnvelope clip;
    private AnimationMock animation;

    @Before
    public void setUp() {
        animation = new AnimationMock(Toolkit.getToolkit().getPrimaryTimer(), AnimationMock.DEFAULT_DURATION, AnimationMock.DEFAULT_RATE, 1, AnimationMock.DEFAULT_AUTOREVERSE);
        clip = new SingleLoopClipEnvelopeShim(animation);
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
        animation.setCycleCount(1);
        animation.mockCycleDuration(Duration.INDEFINITE);
        c = clip.setCycleDuration(Duration.INDEFINITE);
        assertSame(clip, c);

        // Setting cycleCount to 2
        animation.setCycleCount(2);
        animation.mockCycleDuration(Duration.INDEFINITE);
        c = clip.setCycleCount(2);
        assertSame(clip, c);

        // Setting cycleDuration to < INDEFINITE
        animation.setCycleCount(2);
        animation.mockCycleDuration(AnimationMock.DEFAULT_DURATION);
        c = clip.setCycleDuration(AnimationMock.DEFAULT_DURATION);
        assertNotSame(clip, c);
        assertTrue(c instanceof FiniteClipEnvelope);

        // Setting cycleCount to 1
        animation.setCycleCount(1);
        animation.mockCycleDuration(AnimationMock.DEFAULT_DURATION);
        c = clip.setCycleCount(1);
        assertSame(clip, c);
    }

    @Test
    public void testJump() {
        clip.jumpTo(0);
        animation.check(Command.JUMP, 0, CYCLE_TICKS);

        clip.jumpTo(6 * 300);
        animation.check(Command.JUMP, 6 * 300, CYCLE_TICKS);

        clip.jumpTo(6 * 300);
        animation.check(Command.JUMP, 6 * 300, CYCLE_TICKS);

        clip.jumpTo(0);
        animation.check(Command.JUMP, 0, CYCLE_TICKS);

        clip.jumpTo(6 * 1000);
        animation.check(Command.JUMP, 6 * 1000, CYCLE_TICKS);

        clip.jumpTo(-1);
        animation.check(Command.JUMP, 0, CYCLE_TICKS);

        clip.jumpTo(6 * 1000 + 1);
        animation.check(Command.JUMP, 6 * 1000, CYCLE_TICKS);
    }

    @Test
    public void testTimePulseForward() {
        animation.mockStatus(Status.RUNNING);
        clip.start();

        clip.timePulse(1);
        animation.check(Command.PLAY, 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000 - 1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000);
        animation.check(Command.PLAY, 6 * 1000, CYCLE_TICKS);
        assertTrue(animation.finishCalled());
    }

    @Test
    public void testTimePulseBackward() {
        clip.jumpTo(6 * 1000);
        clip.setRate(-1.0);
        animation.mockStatus(Status.RUNNING);
        clip.start();

        clip.timePulse(1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000 - 1);
        animation.check(Command.PLAY, 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 1000);
        animation.check(Command.PLAY, 0, CYCLE_TICKS);
        assertTrue(animation.finishCalled());
    }

    @Test
    public void testJumpAndPulseForward() {
        animation.mockStatus(Status.RUNNING);
        clip.start();

        clip.jumpTo(6 * 300);
        animation.check(Command.JUMP, 6 * 300, CYCLE_TICKS);

        clip.timePulse(6 * 700 - 1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.jumpTo(6 * 500);
        animation.check(Command.JUMP, 6 * 500, CYCLE_TICKS);

        clip.timePulse(6 * 700 - 1 + 6 * 500 - 1);
        animation.check(Command.PLAY, 6 * 1000 - 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 700 - 1 + 6 * 500 - 1 + 1);
        animation.check(Command.PLAY, 6 * 1000, CYCLE_TICKS);
        assertTrue(animation.finishCalled());
    }

    @Test
    public void testJumpAndPulseBackward() {
        clip.jumpTo(6 * 1000);
        clip.setRate(-1.0);
        animation.mockStatus(Status.RUNNING);
        clip.start();

        clip.jumpTo(6 * 300);
        animation.check(Command.JUMP, 6 * 300, CYCLE_TICKS);

        clip.timePulse(6 * 300 - 1);
        animation.check(Command.PLAY, 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.jumpTo(6 * 500);
        animation.check(Command.JUMP, 6 * 500, CYCLE_TICKS);

        clip.timePulse(6 * 300 - 1 + 6 * 500 - 1);
        animation.check(Command.PLAY, 1, CYCLE_TICKS);
        assertFalse(animation.finishCalled());

        clip.timePulse(6 * 300 - 1 + 6 * 500 - 1 + 1);
        animation.check(Command.PLAY, 0, CYCLE_TICKS);
        assertTrue(animation.finishCalled());
    }

    @Test
    public void testRate() {
        clip.setRate(0.5);
        animation.mockStatus(Status.RUNNING);
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
    }

}
