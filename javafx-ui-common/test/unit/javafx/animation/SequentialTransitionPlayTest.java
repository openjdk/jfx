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
package javafx.animation;

import com.sun.javafx.animation.TickCalculation;
import javafx.animation.Animation.Status;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.util.Duration;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SequentialTransitionPlayTest {

    LongProperty xProperty = new SimpleLongProperty();
    LongProperty yProperty = new SimpleLongProperty();

    AbstractMasterTimerMock amt;
    SequentialTransition st;

    Transition child1X;
    Transition child2X;
    Transition child1Y;

    @Before
    public void setUp() {
        amt = new AbstractMasterTimerMock();
        st = new SequentialTransition(amt);
        child1X = new Transition() {

            {
                setCycleDuration(Duration.minutes(1));
                setInterpolator(Interpolator.LINEAR);
            }

            @Override
            protected void interpolate(double d) {
                xProperty.set(Math.round(d * 60000));
            }
        };
        child2X = new Transition() {


            {
                setCycleDuration(Duration.seconds(30));
                setInterpolator(Interpolator.LINEAR);
            }

            @Override
            protected void interpolate(double d) {
                xProperty.set(10000 + Math.round(d * 30000));
            }
        };
        child1Y = new Transition() {

            {
                setCycleDuration(Duration.seconds(10));
                setInterpolator(Interpolator.LINEAR);
            }

            @Override
            protected void interpolate(double d) {
                yProperty.set(Math.round(d * 10000));
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
        assertEquals(Math.round(TickCalculation.toMillis(100)), xProperty.get());
        assertEquals(0, yProperty.get());

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());

        st.jumpTo(Duration.minutes(1).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.RUNNING, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000 - Math.round(TickCalculation.toMillis(100)), xProperty.get());
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
        assertEquals(Math.round(TickCalculation.toMillis(100)), yProperty.get());

        st.jumpTo(Duration.minutes(1).add(Duration.seconds(10)).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TickCalculation.toMillis(100)), yProperty.get());

        amt.pulse();

        assertEquals(Status.STOPPED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());
    }

    @Test
    public void testCycle() {
        st.getChildren().addAll(child1X, child1Y);
        st.setCycleCount(2);

        st.play();

        st.jumpTo(Duration.minutes(1).add(Duration.seconds(10)).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 - Math.round(TickCalculation.toMillis(100)), yProperty.get());

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
        assertEquals(Math.round(TickCalculation.toMillis(100)), xProperty.get());
        assertEquals(0, yProperty.get());

        st.jumpTo(Duration.minutes(2).add(Duration.seconds(20)).subtract(TickCalculation.toDuration(100)));

        assertEquals(Status.RUNNING, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.RUNNING, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000 -  Math.round(TickCalculation.toMillis(100)), yProperty.get());

        amt.pulse();

        assertEquals(Status.STOPPED, st.getStatus());
        assertEquals(Status.STOPPED, child1X.getStatus());
        assertEquals(Status.STOPPED, child1Y.getStatus());
        assertEquals(60000, xProperty.get());
        assertEquals(10000, yProperty.get());

    }
}
