/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.css;

import com.sun.javafx.css.TransitionTimer;
import org.junit.jupiter.api.Test;
import javafx.css.TransitionDefinition;
import javafx.util.Duration;
import java.util.ArrayList;

import static javafx.animation.Interpolator.*;
import static javafx.css.TransitionPropertySelector.*;
import static javafx.util.Duration.*;
import static org.junit.jupiter.api.Assertions.*;

public class TransitionTimerTest {

    @Test
    public void testTimerEndsWithProgressExactlyOne() {
        var trace = new ArrayList<Double>();
        var transition = new TransitionDefinition(BEAN, "test", seconds(1), ZERO, LINEAR);
        var timer = new TransitionTimerMock(transition) {
            @Override
            protected void onUpdate(double progress) {
                trace.add(progress);
            }
        };

        timer.start();

        timer.fire(Duration.seconds(0.4));
        assertEquals(1, trace.size());
        assertTrue(trace.get(0) > 0.3 && trace.get(0) < 0.5);

        timer.fire(Duration.seconds(0.7));
        assertEquals(2, trace.size());
        assertTrue(trace.get(1) == 1); // must be exactly 1
    }

    @Test
    public void testTimerStopsWhenProgressIsOne() {
        var flag = new boolean[1];
        var transition = new TransitionDefinition(BEAN, "test", seconds(1), ZERO, LINEAR);
        var timer = new TransitionTimerMock(transition) {
            @Override
            protected void onUpdate(double progress) {}

            @Override
            public void stop() {
                flag[0] = true;
                super.stop();
            }
        };

        timer.start();
        timer.fire(Duration.seconds(0.9));
        assertFalse(flag[0]);
        timer.fire(Duration.seconds(0.2));
        assertTrue(flag[0]);
    }

    @Test
    public void testNullTimerIsTriviallyStopped() {
        assertTrue(TransitionTimer.tryStop(null));
    }

    @Test
    public void testRunningTimerCanBeStopped() {
        var transition = new TransitionDefinition(BEAN, "test", seconds(1), ZERO, LINEAR);
        var timer = new TransitionTimerMock(transition) {
            @Override protected void onUpdate(double progress) {}
        };

        timer.start();
        timer.fire(Duration.seconds(0.2));
        assertTrue(TransitionTimer.tryStop(timer));
    }

    @Test
    public void testTimerCannotStopItself() {
        var flag = new boolean[1];
        var transition = new TransitionDefinition(BEAN, "test", seconds(1), ZERO, LINEAR);
        var timer = new TransitionTimerMock(transition) {
            @Override protected void onUpdate(double progress) {
                flag[0] = TransitionTimer.tryStop(this);
            }
        };

        timer.start();
        timer.fire(Duration.seconds(0.2));
        assertFalse(flag[0]);
    }

    private static abstract class TransitionTimerMock extends TransitionTimer {
        long now = System.nanoTime();

        TransitionTimerMock(TransitionDefinition transition) {
            super(transition);
        }

        public void fire(Duration elapsedTime) {
            now += (long)(elapsedTime.toMillis() * 1000000);
            handle(now);
        }
    }

}
