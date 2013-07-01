/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;

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

    @Before
    public void setUp() {
        timer = new AbstractMasterTimerMock();
        animation = new AnimationMock(timer, Duration.INDEFINITE, 1.0, 1, false);
    }

    @After
    public void tearDown() {
        animation.impl_stop();
        ToolkitAccessor.setInstance(null);
    }

    @Test
    public void testPlay_DefaultResolution() {
        // start animatiom
        timer.setNanos(Math.round(3 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        animation.startReceiver(0);
        assertTrue(timer.containsPulseReceiver(animation.pulseReceiver));

        // send pulse
        animation.pulseReceiver.timePulse(7 * DEFAULT_RESOLUTION);
        assertEquals(4 * DEFAULT_RESOLUTION, animation.getLastTimePulse());

        // another pulse
        animation.pulseReceiver.timePulse(16 * DEFAULT_RESOLUTION);
        assertEquals(13 * DEFAULT_RESOLUTION, animation.getLastTimePulse());

        // stop animation
        animation.impl_stop();
        assertFalse(timer.containsPulseReceiver(animation.pulseReceiver));

        // stop again
        animation.impl_stop();
        assertFalse(timer.containsPulseReceiver(animation.pulseReceiver));

        // start again
        timer.setNanos(Math.round(30 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        animation.startReceiver(0);
        assertTrue(timer.containsPulseReceiver(animation.pulseReceiver));

        // send pulse
        animation.pulseReceiver.timePulse(43 * DEFAULT_RESOLUTION);
        assertEquals(13 * DEFAULT_RESOLUTION, animation.getLastTimePulse());
    }

    @Test
    public void testPause_DefaultResolution() {
        // start animation
        timer.setNanos(Math.round(3 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        animation.startReceiver(0);
        assertTrue(timer.containsPulseReceiver(animation.pulseReceiver));

        // pause animation
        timer.setNanos(Math.round(18 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        animation.pauseReceiver();
        assertFalse(timer.containsPulseReceiver(animation.pulseReceiver));

        // pause again
        timer.setNanos(Math.round(27 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        animation.pauseReceiver();
        assertFalse(timer.containsPulseReceiver(animation.pulseReceiver));

        // resume
        timer.setNanos(Math.round(36 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        animation.resumeReceiver();
        assertTrue(timer.containsPulseReceiver(animation.pulseReceiver));

        // resume again
        timer.setNanos(Math.round(42 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        animation.resumeReceiver();
        assertTrue(timer.containsPulseReceiver(animation.pulseReceiver));

        // send pulse
        animation.pulseReceiver.timePulse(51 * DEFAULT_RESOLUTION);
        assertEquals(30 * DEFAULT_RESOLUTION, animation.getLastTimePulse());
    }

    @Test
    public void testDelay() {
        // start animatiom
        timer.setNanos(Math.round(3 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        animation.startReceiver(17 * DEFAULT_RESOLUTION);
        assertTrue(timer.containsPulseReceiver(animation.pulseReceiver));

        // send pulse during delay
        animation.pulseReceiver.timePulse(5 * DEFAULT_RESOLUTION);
        assertEquals(0, animation.getLastTimePulse());

        // pause & resume
        timer.setNanos(Math.round(10 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        animation.pauseReceiver();
        timer.setNanos(Math.round(37 * DEFAULT_RESOLUTION * TICKS_2_NANOS));
        animation.resumeReceiver();

        // send pulse during delay
        animation.pulseReceiver.timePulse(41 * DEFAULT_RESOLUTION);
        assertEquals(0, animation.getLastTimePulse());

        // send pulse after delay
        animation.pulseReceiver.timePulse(48 * DEFAULT_RESOLUTION);
        assertEquals(1 * DEFAULT_RESOLUTION, animation.getLastTimePulse());
    }
}
