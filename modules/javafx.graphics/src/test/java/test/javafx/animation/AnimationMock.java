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

import javafx.util.Duration;
import com.sun.scenario.animation.AbstractPrimaryTimer;

import static org.junit.Assert.*;

public class AnimationMock extends AnimationImpl {

    public static final Duration DEFAULT_DURATION = Duration.seconds(1);
    public static final double DEFAULT_RATE = 1.0;
    public static final int DEFAULT_CYCLE_COUNT = 1;
    public static final boolean DEFAULT_AUTOREVERSE = false;
    private long lastTimePulse;

    public enum Command {PLAY, JUMP, NONE};

    private Command lastCommand = Command.NONE;
    private long lastCurrentTicks = -1;
    private long lastCycleTicks = -1;
    private boolean finishFlag;

    public void mockStatus(Status status) {
        this.setStatus(status);
    }

    public void mockCycleDuration(Duration duration) {
        shim_setCycleDuration(duration);
    }

    public AnimationMock(AbstractPrimaryTimer timer, Duration cycleDuration, double rate, int cycleCount, boolean autoReverse) {
        super(timer);
        shim_setCycleDuration(cycleDuration);
        setRate(rate);
        setCycleCount(cycleCount);
        setAutoReverse(autoReverse);
        super.setOnFinished(event -> {
            finishFlag = true;
        });
    }

    public void check(Command lastCommand, long lastCurrentTicks, long lastCycleTicks) {
        assertEquals(lastCommand, this.lastCommand);
        if (lastCommand != Command.NONE) {
            assertEquals(lastCurrentTicks, this.lastCurrentTicks);
            assertEquals(lastCycleTicks, this.lastCycleTicks);
        }
        this.lastCommand = Command.NONE;
        this.lastCurrentTicks = -1;
        this.lastCycleTicks = -1;
    }

    public boolean finishCalled() {
        final boolean result = finishFlag;
        finishFlag = false;
        return result;
    }

    public long getLastTimePulse() {
            final long p = lastTimePulse;
            lastTimePulse = 0L;
            return p;
        }


    @Override
    public void doPlayTo(long currentTicks, long cycleTicks) {
        lastCommand = Command.PLAY;
        lastCurrentTicks = currentTicks;
        lastCycleTicks = cycleTicks;
    }

    @Override
    public void doJumpTo(long currentTicks, long cycleTicks, boolean forceJump) {
        lastCommand = Command.JUMP;
        lastCurrentTicks = currentTicks;
        lastCycleTicks = cycleTicks;
    }

    @Override
    public void doTimePulse(long elapsedTime) {
        super.doTimePulse(elapsedTime);
        lastTimePulse = elapsedTime;
    }

}
