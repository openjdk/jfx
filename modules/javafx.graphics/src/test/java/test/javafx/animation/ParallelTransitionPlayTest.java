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

import com.sun.javafx.animation.TickCalculation;
import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ParallelTransitionShim;
import javafx.animation.Transition;
import javafx.animation.TransitionShim;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.util.Duration;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class ParallelTransitionPlayTest {
    public static final double TICK_MILLIS = TickCalculation.toMillis(100);
    public static final long TICK_STEP = Math.round(TICK_MILLIS);

    LongProperty xProperty = new SimpleLongProperty();
    LongProperty yProperty = new SimpleLongProperty();

    AbstractPrimaryTimerMock amt;
    ParallelTransition pt;

    Transition child1X;
    Transition child2X;
    Transition child1Y;
    Transition childByX;

    @Before
    public void setUp() {
        amt = new AbstractPrimaryTimerMock();
        pt = ParallelTransitionShim.getParallelTransition(amt);
        child1X = new TransitionShim() {

            {
                setCycleDuration(Duration.minutes(1));
                setInterpolator(Interpolator.LINEAR);
            }

            @Override
            protected void interpolate(double d) {
                xProperty.set(Math.round(d * 60000));
            }
        };
        child1Y = new TransitionShim() {

            {
                setCycleDuration(Duration.seconds(10));
                setInterpolator(Interpolator.LINEAR);
            }

            @Override
            protected void interpolate(double d) {
                yProperty.set(Math.round(d * 10000));
            }
        };
        childByX = new TransitionShim() {
            {
                setCycleDuration(Duration.seconds(1));
                setInterpolator(Interpolator.LINEAR);
            }

            long lastX;

            @Override
            protected void interpolate(double frac) {
                xProperty.set(Math.round(lastX + frac * 1000));
            }

            @Override
            public void sync(boolean forceSync) {
                super.sync(forceSync);
                if (forceSync) {
                    lastX = xProperty.get();
                }
            }


        };
    }


    @Test
    public void testSimplePlay() {
        pt.getChildren().addAll(child1X, child1Y);

        pt.play();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        amt.pulse();
        assertEquals(TickCalculation.toDuration(100), pt.getCurrentTime());
        assertEquals(TickCalculation.toDuration(100), child1X.getCurrentTime());
        assertEquals(TickCalculation.toDuration(100), child1Y.getCurrentTime());
        assertEquals(TICK_STEP, xProperty.get());
        assertEquals(TICK_STEP, yProperty.get());

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());

        pt.jumpTo(Duration.seconds(10).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(10000 - TICK_STEP, xProperty.get());
        assertEquals(10000 - TICK_STEP, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000 + TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.jumpTo(Duration.minutes(1).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();
        assertEquals(Status.STOPPED, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());
    }

    @Test
    public void testSimplePlayReversed() {
        pt.getChildren().addAll(child1X, child1Y);

        pt.setRate(-1.0);
        pt.jumpTo(Duration.seconds(60));

        pt.play();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        amt.pulse();
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        pt.jumpTo(Duration.seconds(10).add(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000 + TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(10000 - TICK_STEP, xProperty.get());
        assertEquals(10000 - TICK_STEP, yProperty.get());

        pt.jumpTo(TickCalculation.toDuration(100));

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(TICK_STEP, xProperty.get());
        assertEquals(TICK_STEP, yProperty.get());

        amt.pulse();
        assertEquals(Status.STOPPED, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());
    }

    @Test
    public void testCycle() {
        pt.getChildren().addAll(child1X, child1Y);
        pt.setCycleCount(2);

        pt.play();

        pt.jumpTo(Duration.minutes(1).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());

        pt.jumpTo(Duration.seconds(65).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(5000 - TICK_STEP, xProperty.get());
        assertEquals(5000 - TICK_STEP, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(5000, xProperty.get());
        assertEquals(5000, yProperty.get());

        pt.jumpTo(Duration.minutes(2).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();
        assertEquals(Status.STOPPED, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

    }

    @Test
    public void testCycleReversed() {
        pt.getChildren().addAll(child1X, child1Y);
        pt.setCycleCount(2);
        pt.setRate(-1.0);
        pt.jumpTo(Duration.seconds(60));

        pt.play();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.jumpTo(Duration.seconds(60).add(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(TICK_STEP, xProperty.get());
        assertEquals(TICK_STEP, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.jumpTo(Duration.minutes(2).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - Math.round(2 * TICK_MILLIS), xProperty.get());
        assertEquals(10000, yProperty.get());

    }

    @Test
    public void testAutoReverse() {
        pt.getChildren().addAll(child1X, child1Y);
        pt.setAutoReverse(true);
        pt.setCycleCount(-1);

        pt.play();

        for (int i = 0; i < TickCalculation.fromDuration(Duration.seconds(60)) / 100 - 1; ++i) {
            amt.pulse();
        }

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

    }

    @Test
    public void testAutoReverseWithJump() {
        pt.getChildren().addAll(child1X, child1Y);
        pt.setAutoReverse(true);
        pt.setCycleCount(-1);

        pt.play();

        pt.jumpTo(Duration.seconds(60).subtract(TickCalculation.toDuration(100)));

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

    }

    @Test
    public void testJump() {
        pt.getChildren().addAll(child1X, child1Y);

        assertEquals(Status.STOPPED, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());

        pt.jumpTo(Duration.seconds(10));

        assertEquals(Status.STOPPED, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());

        pt.play();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());  //Note: Not sure if we need to have also child1X running at this point
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000 + TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.jumpTo(Duration.seconds(55));

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(55000, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.jumpTo(Duration.seconds(10));

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.stop();

        assertEquals(Status.STOPPED, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(10000, yProperty.get());

    }

    @Test
    public void testToggleRate() {
        pt.getChildren().addAll(child1X, child1Y);

        pt.play();

        pt.jumpTo(Duration.seconds(10));

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(10000 + TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.setRate(-1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());

        assertEquals(10000 - TICK_STEP, xProperty.get());
        assertEquals(10000 - TICK_STEP, yProperty.get());

        pt.setRate(1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(10000 + TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());
    }

    @Test
    public void testToggleRate_2() {
        pt.getChildren().addAll(child1X, child1Y);

        pt.play();

        pt.jumpTo(Duration.seconds(20));

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(20000 + TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.setRate(-1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(20000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.setRate(1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(20000 + TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());
    }


    @Test
    public void testChildWithDifferentRate() {
        pt.getChildren().addAll(child1X, child1Y);
        child1X.setRate(2.0);

        pt.play();

        amt.pulse();

        assertEquals(Math.round(TICK_MILLIS * 2), xProperty.get());

        pt.jumpTo(Duration.seconds(30));

        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.jumpTo(Duration.seconds(40));

        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.jumpTo(Duration.seconds(5));
        amt.pulse();

        pt.setRate(-1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(10000 - Math.round(TICK_MILLIS * 2), xProperty.get());
        assertEquals(5000 - TICK_STEP, yProperty.get());

        pt.setRate(1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(10000 + Math.round(TICK_MILLIS * 2), xProperty.get());
        assertEquals(5000 + TICK_STEP, yProperty.get());

    }

    @Test
    public void testPauseForward1() {
        pt.getChildren().addAll(child1X, child1Y);

        pt.play();
        pt.jumpTo(Duration.seconds(5));
        amt.pulse();
        pt.pause();
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(5000 + TICK_STEP, xProperty.get());
        assertEquals(5000 + TICK_STEP, yProperty.get());

        pt.jumpTo(Duration.seconds(6));
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(6000, xProperty.get());
        assertEquals(6000, yProperty.get());

        pt.play();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(6000, xProperty.get());
        assertEquals(6000, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(6000 + TICK_STEP, xProperty.get());
        assertEquals(6000 + TICK_STEP, yProperty.get());

        pt.pause();
        pt.jumpTo(Duration.seconds(7));
        pt.jumpTo(Duration.seconds(9));
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(9000, xProperty.get());
        assertEquals(9000, yProperty.get());

        pt.play();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(9000, xProperty.get());
        assertEquals(9000, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(9000 + TICK_STEP, xProperty.get());
        assertEquals(9000 + TICK_STEP, yProperty.get());

        pt.pause();
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(9000 + TICK_STEP, xProperty.get());
        assertEquals(9000 + TICK_STEP, yProperty.get());

        pt.jumpTo(Duration.seconds(10).subtract(TickCalculation.toDuration(100)));
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(10000 - TICK_STEP, xProperty.get());
        assertEquals(10000 - TICK_STEP, yProperty.get());

        pt.play();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(10000 - TICK_STEP, xProperty.get());
        assertEquals(10000 - TICK_STEP, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.pause();
        pt.jumpTo(Duration.seconds(60).subtract(TickCalculation.toDuration(100)));
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.play();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();
        assertEquals(Status.STOPPED, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());
    }

    @Test
    public void testPauseForward2() {
        pt.getChildren().addAll(child1X, child1Y);

        pt.play();
        pt.jumpTo(Duration.seconds(5));
        amt.pulse();
        pt.pause();
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(5000 + TICK_STEP, xProperty.get());
        assertEquals(5000 + TICK_STEP, yProperty.get());

        pt.jumpTo(Duration.seconds(30));
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(30000, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.play();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(30000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(30000 + TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.pause();
        pt.jumpTo(Duration.seconds(60).subtract(TickCalculation.toDuration(100)));
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.play();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();
        assertEquals(Status.STOPPED, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());
    }

    @Test
    public void testPauseAutoReverse() {
        pt.getChildren().addAll(child1X, child1Y);
        pt.setAutoReverse(true);
        pt.setCycleCount(-1);

        pt.play();
        pt.jumpTo(Duration.seconds(5));
        amt.pulse();
        pt.pause();
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(5000 + TICK_STEP, xProperty.get());
        assertEquals(5000 + TICK_STEP, yProperty.get());

        pt.jumpTo(Duration.seconds(60).subtract(TickCalculation.toDuration(100)));
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.play();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.pause();
        pt.jumpTo(Duration.seconds(110).subtract(TickCalculation.toDuration(100)));
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000 + TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        pt.play();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000 + TICK_STEP, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(10000 - TICK_STEP, xProperty.get());
        assertEquals(10000 - TICK_STEP, yProperty.get());

        pt.pause();
        pt.jumpTo(Duration.seconds(120).subtract(TickCalculation.toDuration(100)));
        assertEquals(Status.PAUSED, pt.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(0 + TICK_STEP, xProperty.get());
        assertEquals(0 + TICK_STEP, yProperty.get());

        pt.play();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(0 + TICK_STEP, xProperty.get());
        assertEquals(0 + TICK_STEP, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(0 + TICK_STEP, xProperty.get());
        assertEquals(0 + TICK_STEP, yProperty.get());
    }


    @Test public void testNestedParallelTransition() {
        ParallelTransition pt2 = new ParallelTransition();

        pt.getChildren().addAll(pt2, child1X);
        pt2.getChildren().add(child1Y);

        pt.play();

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, pt2.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(TICK_STEP, xProperty.get());
        assertEquals(TICK_STEP, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, pt2.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(Math.round(TICK_MILLIS * 2), xProperty.get());
        assertEquals(Math.round(TICK_MILLIS * 2), yProperty.get());


        pt.jumpTo(Duration.seconds(60).subtract(TickCalculation.toDuration(100)));

        amt.pulse();

        assertEquals(Status.STOPPED, pt.getStatus());
        assertEquals(Status.STOPPED, pt2.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        pt.play();


        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, pt2.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(TICK_STEP, xProperty.get());
        assertEquals(TICK_STEP, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, pt.getStatus());
        assertEquals(Status.RUNNING, pt2.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(Math.round(TICK_MILLIS * 2), xProperty.get());
        assertEquals(Math.round(TICK_MILLIS * 2), yProperty.get());


        pt.jumpTo(Duration.seconds(60).subtract(TickCalculation.toDuration(100)));

        amt.pulse();

        assertEquals(Status.STOPPED, pt.getStatus());
        assertEquals(Status.STOPPED, pt2.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
    }

    @Test
    public void testPlayFromStartSynchronization() {
        pt.getChildren().addAll(child1Y, childByX);

        pt.play();

        assertEquals(0, yProperty.get());
        assertEquals(0, xProperty.get());

        pt.jumpTo(Duration.seconds(10));
        amt.pulse();

        pt.play();
        assertEquals(0, yProperty.get());
        assertEquals(1000, xProperty.get());

        pt.jumpTo(Duration.seconds(10));
        amt.pulse();

        assertEquals(10000, yProperty.get());
        assertEquals(2000, xProperty.get());

    }

    @Test
    public void testCycleSynchronization() {
        pt.setCycleCount(2);
        pt.getChildren().addAll(childByX);

        pt.play();

        assertEquals(0, xProperty.get());

        pt.jumpTo(Duration.seconds(1));
        amt.pulse();

        assertEquals(TICK_STEP, xProperty.get());

        pt.jumpTo(Duration.seconds(2));
        amt.pulse();

        assertEquals(1000, xProperty.get());

    }

}
