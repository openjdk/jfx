/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.scenario.animation.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.animation.Animation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.scenario.ToolkitAccessor;
import com.sun.scenario.animation.AbstractMasterTimerMock;
import javafx.animation.AnimationMock;
import javafx.util.Duration;

public class AnimationPulseReceiverTest {

    private static final int DEFAULT_RESOLUTION = ToolkitAccessor.getMasterTimer().getDefaultResolution();
    private static final double TICKS_2_NANOS = 1.0 / 6e-6;
    private AbstractMasterTimerMock timer;
    private AnimationMock animation;
    private AnimationPulseReceiver defaultPR;

    @Before
    public void setUp() {
        timer = new AbstractMasterTimerMock();
        animation = new AnimationMock(Duration.INDEFINITE, 1.0, 1, false);
        defaultPR = new AnimationPulseReceiver(animation, timer);
    }

    @After
    public void tearDown() {
        defaultPR.stop();
        ToolkitAccessor.setInstance(null);
    }

    @Test
    public void testPlay_DefaultResolution() {
        // start animatiom
        timer.setNanos(Math.round(3 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        defaultPR.start(0);
        assertTrue(timer.containsPulseReceiver(defaultPR));

        // send pulse
        defaultPR.timePulse(7 * DEFAULT_RESOLUTION);
        assertEquals(4 * DEFAULT_RESOLUTION, animation.getLastTimePulse());

        // another pulse
        defaultPR.timePulse(16 * DEFAULT_RESOLUTION);
        assertEquals(13 * DEFAULT_RESOLUTION, animation.getLastTimePulse());

        // stop animation
        defaultPR.stop();
        assertFalse(timer.containsPulseReceiver(defaultPR));

        // stop again
        defaultPR.stop();
        assertFalse(timer.containsPulseReceiver(defaultPR));

        // start again
        timer.setNanos(Math.round(30 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        defaultPR.start(0);
        assertTrue(timer.containsPulseReceiver(defaultPR));

        // send pulse
        defaultPR.timePulse(43 * DEFAULT_RESOLUTION);
        assertEquals(13 * DEFAULT_RESOLUTION, animation.getLastTimePulse());
    }

    @Test
    public void testPause_DefaultResolution() {
        // start animation
        timer.setNanos(Math.round(3 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        defaultPR.start(0);
        assertTrue(timer.containsPulseReceiver(defaultPR));

        // pause animation
        timer.setNanos(Math.round(18 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        defaultPR.pause();
        assertFalse(timer.containsPulseReceiver(defaultPR));

        // pause again
        timer.setNanos(Math.round(27 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        defaultPR.pause();
        assertFalse(timer.containsPulseReceiver(defaultPR));

        // resume
        timer.setNanos(Math.round(36 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        defaultPR.resume();
        assertTrue(timer.containsPulseReceiver(defaultPR));

        // resume again
        timer.setNanos(Math.round(42 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        defaultPR.resume();
        assertTrue(timer.containsPulseReceiver(defaultPR));

        // send pulse
        defaultPR.timePulse(51 * DEFAULT_RESOLUTION);
        assertEquals(30 * DEFAULT_RESOLUTION, animation.getLastTimePulse());
    }

    @Test
    public void testDelay() {
        // start animatiom
        timer.setNanos(Math.round(3 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        defaultPR.start(17 * DEFAULT_RESOLUTION);
        assertTrue(timer.containsPulseReceiver(defaultPR));

        // send pulse during delay
        defaultPR.timePulse(5 * DEFAULT_RESOLUTION);
        assertEquals(0, animation.getLastTimePulse());

        // pause & resume
        timer.setNanos(Math.round(10 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        defaultPR.pause();
        timer.setNanos(Math.round(37 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        defaultPR.resume();

        // send pulse during delay
        defaultPR.timePulse(41 * DEFAULT_RESOLUTION);
        assertEquals(0, animation.getLastTimePulse());

        // send pulse after delay
        defaultPR.timePulse(48 * DEFAULT_RESOLUTION);
        assertEquals(1 * DEFAULT_RESOLUTION, animation.getLastTimePulse());
    }
}
