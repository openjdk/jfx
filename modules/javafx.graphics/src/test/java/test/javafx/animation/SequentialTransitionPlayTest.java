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
import javafx.animation.AnimationShim;
import javafx.animation.Interpolator;
import javafx.animation.SequentialTransition;
import javafx.animation.SequentialTransitionShim;
import javafx.animation.Transition;
import javafx.animation.TransitionShim;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.util.Duration;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SequentialTransitionPlayTest {

    public static final double TICK_MILLIS = TickCalculation.toMillis(100);
    public static final long TICK_STEP = Math.round(TICK_MILLIS);

    LongProperty xProperty = new SimpleLongProperty();
    LongProperty yProperty = new SimpleLongProperty();
    AbstractPrimaryTimerMock amt;
    SequentialTransition st;
    Transition child1X;
    Transition child1Y;
    Transition childByX;
    Transition childByX2;

    @Before
    public void setUp() {
        amt = new AbstractPrimaryTimerMock();
        st = SequentialTransitionShim.getSequentialTransition(amt);
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
        childByX = createByXChild();
        childByX2 = createByXChild();
    }

    private Transition createByXChild() {
        return new TransitionShim() {
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
        st.getChildren().addAll(child1X, child1Y);

        st.play();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        amt.pulse();
        assertEquals(TickCalculation.toDuration(100), st.getCurrentTime());
        assertEquals(TickCalculation.toDuration(100), child1X.getCurrentTime());
        assertEquals(Duration.ZERO, child1Y.getCurrentTime());
        assertEquals(Math.round(TICK_MILLIS), xProperty.get());
        assertEquals(0, yProperty.get());

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        st.jumpTo(Duration.minutes(1).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - Math.round(TICK_MILLIS), xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(Math.round(TICK_MILLIS), yProperty.get());

        st.jumpTo(Duration.minutes(1).add(Duration.seconds(10)).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TICK_MILLIS), yProperty.get());

        amt.pulse();

        assertEquals(Status.STOPPED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());
    }


    @Test
    public void testSimplePlayReversed() {
        st.getChildren().addAll(child1X, child1Y);
        st.setRate(-1.0);
        st.jumpTo(Duration.seconds(70));

        st.play();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(Duration.seconds(70), st.getCurrentTime());
        assertEquals(Duration.seconds(60), child1X.getCurrentTime());
        assertEquals(Duration.seconds(10), child1Y.getCurrentTime());

        amt.pulse();
        assertEquals(Duration.seconds(70).subtract(TickCalculation.toDuration(100)), st.getCurrentTime());
        assertEquals(Duration.seconds(60), child1X.getCurrentTime());
        assertEquals(Duration.seconds(10).subtract(TickCalculation.toDuration(100)), child1Y.getCurrentTime());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TICK_MILLIS), yProperty.get());

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());

        st.jumpTo(Duration.minutes(1).add(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(Math.round(TICK_MILLIS), yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - Math.round(TICK_MILLIS), xProperty.get());
        assertEquals(0, yProperty.get());

        st.jumpTo(TickCalculation.toDuration(100));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(Math.round(TICK_MILLIS), xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();

        assertEquals(Status.STOPPED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());
    }

    @Test
    public void testPauseAndJump() {
        st.getChildren().addAll(child1X, child1Y);

        st.play();
        st.jumpTo(Duration.seconds(10));
        st.pause();
        assertEquals(Status.PAUSED, st.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();
        assertEquals(Duration.seconds(10), st.getCurrentTime());
        assertEquals(Duration.seconds(10), child1X.getCurrentTime());
        assertEquals(Duration.ZERO, child1Y.getCurrentTime());
        assertEquals(10000, xProperty.get());
        assertEquals(0, yProperty.get());

        st.play();
        st.jumpTo(Duration.seconds(50));
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(Duration.seconds(50), st.getCurrentTime());
        assertEquals(Duration.seconds(50), child1X.getCurrentTime());
        assertEquals(Duration.seconds(0), child1Y.getCurrentTime());
        assertEquals(50000, xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(50000 + Math.round(TickCalculation.toMillis(100)), xProperty.get());
        assertEquals(0, yProperty.get());

        st.pause();
        st.jumpTo(Duration.seconds(65));
        assertEquals(Duration.seconds(65), st.getCurrentTime());
        assertEquals(Duration.seconds(60), child1X.getCurrentTime());
        assertEquals(Duration.seconds(5), child1Y.getCurrentTime());
        assertEquals(Status.PAUSED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(5000, yProperty.get());

        amt.pulse();
        assertEquals(Status.PAUSED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(5000, yProperty.get());

        st.play();
        st.jumpTo(Duration.minutes(1).add(Duration.seconds(10)).subtract(TickCalculation.toDuration(100)));
        assertEquals(Duration.seconds(70).subtract(TickCalculation.toDuration(100)), st.getCurrentTime());
        assertEquals(Duration.seconds(60), child1X.getCurrentTime());
        assertEquals(Duration.seconds(10).subtract(TickCalculation.toDuration(100)), child1Y.getCurrentTime());
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TickCalculation.toMillis(100)), yProperty.get());


        amt.pulse();
        assertEquals(Duration.seconds(70), st.getCurrentTime());
        assertEquals(Duration.seconds(60), child1X.getCurrentTime());
        assertEquals(Duration.seconds(10), child1Y.getCurrentTime());
        assertEquals(Status.STOPPED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());
    }

    @Test
    public void testPauseAndJumpReversed1() {
        st.getChildren().addAll(child1X, child1Y);
        st.setRate(-1.0);

        st.jumpTo(Duration.seconds(70));
        st.play();
        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TickCalculation.toMillis(100)), yProperty.get());

        st.pause();
        st.jumpTo(Duration.minutes(1).add(Duration.seconds(5)).add(TickCalculation.toDuration(100)));
        assertEquals(Status.PAUSED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(Duration.seconds(65).add(TickCalculation.toDuration(100)), st.getCurrentTime());
        assertEquals(60000, xProperty.get());
        assertEquals(5000 + Math.round(TickCalculation.toMillis(100)), yProperty.get());

        st.play();
        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(5000, yProperty.get());

        st.pause();
        st.jumpTo(Duration.seconds(10));
        assertEquals(Status.PAUSED, st.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(0, yProperty.get());

        st.play();
        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000 - Math.round(TickCalculation.toMillis(100)), xProperty.get());
        assertEquals(0, yProperty.get());

        st.pause();
        st.jumpTo(Duration.seconds(0).add(TickCalculation.toDuration(100)));
        assertEquals(TickCalculation.toDuration(100), st.getCurrentTime());
        assertEquals(TickCalculation.toDuration(100), child1X.getCurrentTime());
        assertEquals(Duration.seconds(0), child1Y.getCurrentTime());
        assertEquals(Status.PAUSED, st.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(Math.round(TickCalculation.toMillis(100)), xProperty.get());
        assertEquals(0, yProperty.get());

        st.play();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(Math.round(TickCalculation.toMillis(100)), xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();
        assertEquals(Duration.seconds(0), st.getCurrentTime());
        assertEquals(Duration.seconds(0), child1X.getCurrentTime());
        assertEquals(Duration.seconds(0), child1Y.getCurrentTime());
        assertEquals(Status.STOPPED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());
    }

    @Test
    public void testPauseAndJumpReversed2() {
        st.getChildren().addAll(child1X, child1Y);
        st.setRate(-1.0);

        st.jumpTo(Duration.seconds(70));
        st.play();
        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TickCalculation.toMillis(100)), yProperty.get());

        st.pause();
        st.jumpTo(Duration.seconds(50));
        assertEquals(Status.PAUSED, st.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(Duration.seconds(50), st.getCurrentTime());
        assertEquals(50000, xProperty.get());
        assertEquals(0, yProperty.get());

        st.play();
        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(50000 - Math.round(TickCalculation.toMillis(100)), xProperty.get());
        assertEquals(0, yProperty.get());

        st.pause();
        st.jumpTo(Duration.minutes(1).add(Duration.seconds(5)).add(TickCalculation.toDuration(100)));
        assertEquals(Status.PAUSED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(5000 + Math.round(TickCalculation.toMillis(100)), yProperty.get());

        st.play();
        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(5000, yProperty.get());
    }

    @Test
    public void testPauseAndJumpAutoReverse() {
        st.getChildren().addAll(child1X, child1Y);
        st.setAutoReverse(true);
        st.setCycleCount(2);

        st.jumpTo(Duration.minutes(1).add(Duration.seconds(10)).subtract(TickCalculation.toDuration(100)));
        st.play();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TickCalculation.toMillis(100)), yProperty.get());

        amt.pulse();
        assertEquals(Duration.seconds(70), st.getCurrentTime());
        assertEquals(Duration.seconds(60), child1X.getCurrentTime());
        assertEquals(Duration.seconds(10), child1Y.getCurrentTime());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

        st.pause();
        st.jumpTo(Duration.minutes(1).add(Duration.seconds(10)).add(TickCalculation.toDuration(100)));
        assertEquals(Status.PAUSED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.PAUSED, child1Y.getStatus());
        assertEquals(Duration.seconds(70).subtract(TickCalculation.toDuration(100)), st.getCurrentTime());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TickCalculation.toMillis(100)), yProperty.get());

        st.play();
        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TickCalculation.toMillis(2 * 100)), yProperty.get());

        st.pause();
        st.jumpTo(Duration.seconds(100));
        assertEquals(Duration.seconds(40), st.getCurrentTime());
        assertEquals(Duration.seconds(40), child1X.getCurrentTime());
        assertEquals(Duration.seconds(0), child1Y.getCurrentTime());
        assertEquals(Status.PAUSED, st.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(40000, xProperty.get());
        assertEquals(0, yProperty.get());

        st.play();
        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(40000  - Math.round(TickCalculation.toMillis(100)), xProperty.get());
        assertEquals(0, yProperty.get());

        st.pause();
        st.jumpTo(Duration.minutes(2).add(Duration.seconds(20)).subtract(TickCalculation.toDuration(100)));
        assertEquals(TickCalculation.toDuration(100), st.getCurrentTime());
        assertEquals(TickCalculation.toDuration(100), child1X.getCurrentTime());
        assertEquals(Duration.seconds(0), child1Y.getCurrentTime());
        assertEquals(Status.PAUSED, st.getStatus());
        assertEquals(Status.PAUSED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(Math.round(TickCalculation.toMillis(100)), xProperty.get());
        assertEquals(0, yProperty.get());

        st.play();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(Math.round(TickCalculation.toMillis(100)), xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();
        assertEquals(Duration.seconds(0), st.getCurrentTime());
        assertEquals(Duration.seconds(0), child1X.getCurrentTime());
        assertEquals(Duration.seconds(0), child1Y.getCurrentTime());
        assertEquals(Status.STOPPED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());
    }

    @Test
    public void testJumpAndPlay() {
        st.getChildren().addAll(child1X, child1Y);

        st.jumpTo(Duration.seconds(65));
        st.play();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(5000, yProperty.get());


        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(5000 + Math.round(TICK_MILLIS), yProperty.get());

    }

    @Test
    public void testJumpAndPlayReversed() {
        st.getChildren().addAll(child1X, child1Y);
        st.setRate(-1.0);

        st.jumpTo(Duration.seconds(65));
        st.play();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(5000, yProperty.get());


        amt.pulse();
        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(5000 - Math.round(TICK_MILLIS), yProperty.get());

    }


    @Test
    public void testCycle() {
        st.getChildren().addAll(child1X, child1Y);
        st.setCycleCount(2);

        st.play();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());

        st.jumpTo(Duration.minutes(1).add(Duration.seconds(10)).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TICK_MILLIS), yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();

        assertEquals(TickCalculation.toDuration(100), st.getCurrentTime());
        assertEquals(TickCalculation.toDuration(100), child1X.getCurrentTime());
        assertEquals(Duration.ZERO, child1Y.getCurrentTime());
        assertEquals(Math.round(TICK_MILLIS), xProperty.get());
        assertEquals(0, yProperty.get());

        st.jumpTo(Duration.minutes(2).add(Duration.seconds(20)).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TICK_MILLIS), yProperty.get());

        amt.pulse();

        assertEquals(Status.STOPPED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

    }

    @Test
    public void testCycleReverse() {
        st.getChildren().addAll(child1X, child1Y);
        st.setCycleCount(-1);
        st.setRate(-1.0);

        st.play();

//        assertEquals(Status.RUNNING, st.getStatus());
//        assertEquals(Status.STOPPED, child1X.getStatus());
//        assertEquals(Status.RUNNING, child1Y.getStatus());
//        assertEquals(60000, xProperty.get());
//        assertTrue(0 < yProperty.get() && yProperty.get() < 10000);

        st.jumpTo(TickCalculation.toDuration(100));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(Math.round(TICK_MILLIS), xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TICK_MILLIS), yProperty.get());

        st.jumpTo(Duration.minutes(1).add(Duration.seconds(10)).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TICK_MILLIS), yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TickCalculation.toMillis(200)), yProperty.get());

    }

    @Test
    public void testJump() {
        st.getChildren().addAll(child1X, child1Y);

        assertEquals(Status.STOPPED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());

        st.jumpTo(Duration.seconds(10));

        assertEquals(Status.STOPPED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(0, xProperty.get());
        assertEquals(0, yProperty.get());

        st.play();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());  //Note: Not sure if we need to have also child1X running at this point
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(0, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000 + Math.round(TICK_MILLIS), xProperty.get());
        assertEquals(0, yProperty.get());

        st.jumpTo(Duration.seconds(65));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(5000, yProperty.get());

        st.jumpTo(Duration.seconds(10));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(0, yProperty.get());

        st.stop();

        assertEquals(Status.STOPPED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(10000, xProperty.get());
        assertEquals(0, yProperty.get());

    }

    @Test
    public void testAutoReverse() {
        st.getChildren().addAll(child1X, child1Y);
        st.setAutoReverse(true);
        st.setCycleCount(-1);

        st.play();

        for (int i = 0; i < TickCalculation.fromDuration(Duration.seconds(70)) / 100 - 1; ++i) {
            amt.pulse();
        }

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());

        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TICK_MILLIS), yProperty.get());

    }

    @Test
    public void testAutoReverseWithJump() {
        st.getChildren().addAll(child1X, child1Y);
        st.setAutoReverse(true);
        st.setCycleCount(-1);

        st.play();

        st.jumpTo(Duration.seconds(70).subtract(TickCalculation.toDuration(100)));

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());

        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TICK_MILLIS), yProperty.get());

    }

    @Test
    public void testChildWithDifferentRate() {
        st.getChildren().addAll(child1X, child1Y);
        child1X.setRate(2.0);

        st.play();

        amt.pulse();

        assertEquals(Math.round(TICK_MILLIS * 2), xProperty.get());

        st.jumpTo(Duration.seconds(30));

        assertEquals(60000, xProperty.get());
        assertEquals(0, yProperty.get());

        st.jumpTo(Duration.seconds(40));

        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());


        st.jumpTo(Duration.seconds(5));
        amt.pulse();

        st.setRate(-1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(10000 - Math.round(TICK_MILLIS * 2), xProperty.get());
        assertEquals(0, yProperty.get());

        st.setRate(1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(10000 + Math.round(TICK_MILLIS * 2), xProperty.get());
        assertEquals(0, yProperty.get());

    }

    @Test
    public void testToggleRate() {
        st.getChildren().addAll(child1X, child1Y);

        st.play();

        st.jumpTo(Duration.seconds(60));

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());

        assertEquals(60000, xProperty.get());
        assertEquals(Math.round(TICK_MILLIS), yProperty.get());

        st.setRate(-1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(60000 - Math.round(TICK_MILLIS), xProperty.get());
        assertEquals(0, yProperty.get());

        st.setRate(1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());

        assertEquals(60000, xProperty.get());
        assertEquals(Math.round(TICK_MILLIS), yProperty.get());

    }

    @Test
    public void testToggleRate_2() {
        st.getChildren().addAll(child1X, child1Y);

        st.play();

        st.jumpTo(Duration.seconds(10));

        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(10000 + Math.round(TICK_MILLIS), xProperty.get());
        assertEquals(0, yProperty.get());

        st.setRate(-1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(10000 - Math.round(TICK_MILLIS), xProperty.get());
        assertEquals(0, yProperty.get());

        st.setRate(1.0);

        amt.pulse();
        amt.pulse();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        assertEquals(10000 + Math.round(TICK_MILLIS), xProperty.get());
        assertEquals(0, yProperty.get());

    }

    @Test
    public void testPlayFromStartSynchronization() {
        st.getChildren().addAll(child1Y, childByX);

        st.play();

        assertEquals(0, yProperty.get());
        assertEquals(0, xProperty.get());

        st.jumpTo(Duration.seconds(11));
        amt.pulse();

        st.play();
        assertEquals(0, yProperty.get());
        assertEquals(1000, xProperty.get());

        st.jumpTo(Duration.seconds(11));
        amt.pulse();


        assertEquals(10000, yProperty.get());
        assertEquals(2000, xProperty.get());

    }

    @Test
    public void testCycleSynchronization() {
        st.getChildren().addAll(childByX, childByX2);

        st.play();

        assertEquals(0, xProperty.get());

        st.jumpTo(Duration.seconds(11));
        amt.pulse();

        st.play();
        assertEquals(2000, xProperty.get());

        st.jumpTo(Duration.seconds(11));
        amt.pulse();

        assertEquals(4000, xProperty.get());

    }

    @Test
    public void testJumpToDelay() {
        child1X.setDelay(Duration.seconds(2));
        st.getChildren().addAll(child1X);

        st.jumpTo(Duration.seconds(2).subtract(TickCalculation.toDuration(100)));
        st.play();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());

        amt.pulse(); amt.pulse();

        assertEquals(Math.round(TICK_MILLIS), xProperty.get(), 1e-10);
    }

    @Test
    public void testJumpToSecondDelay() {
        child1Y.setDelay(Duration.seconds(2));
        st.getChildren().addAll(child1X, child1Y);

        st.jumpTo(Duration.seconds(62).subtract(TickCalculation.toDuration(100)));
        st.play();

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        amt.pulse(); amt.pulse();

        assertEquals(Math.round(TICK_MILLIS), yProperty.get(), 1e-10);
    }

}
