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
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TimelineShim;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import javafx.util.Pair;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TimelinePlayTest {

    private AbstractPrimaryTimerMock amt;
    private Timeline timeline;
    private LongProperty property = new SimpleLongProperty();


    private static final double EPSILON = 1e-12;


    @Before
    public void setUp() {
        amt = new AbstractPrimaryTimerMock();
        timeline = TimelineShim.getTimeline(amt);
    }

    public void setupTimeline(Timeline a, LongProperty property, Pair<Duration, Long>... values) {
        ObservableList<KeyFrame> keyFrames = ((Timeline) a).getKeyFrames();
        for (Pair<Duration, Long> v : values) {
            keyFrames.add(new KeyFrame(v.getKey(), new KeyValue(property, v.getValue())));
        }
    }

    @Test
    public void testJump() {
        setupTimeline(timeline, property, new Pair(Duration.minutes(1), 60000));

        timeline.jumpTo(Duration.seconds(10));
        assertEquals(Duration.seconds(10), timeline.getCurrentTime());
        assertEquals(0, property.get());

        timeline.play();
        assertEquals(Duration.seconds(10), timeline.getCurrentTime());
        assertEquals(10000, property.get());

        timeline.jumpTo(Duration.seconds(30));
        assertEquals(Duration.seconds(30), timeline.getCurrentTime());
        assertEquals(30000, property.get());
        amt.pulse();
        assertEquals(Duration.seconds(30).add(TickCalculation.toDuration(100)), timeline.getCurrentTime());
        assertEquals(30000 + Math.round(TickCalculation.toMillis(100)), property.get());

        timeline.jumpTo("start");
        assertEquals(Duration.ZERO, timeline.getCurrentTime());
        assertEquals(0, property.get());

        timeline.jumpTo("end");
        assertEquals(Duration.minutes(1), timeline.getCurrentTime());
        assertEquals(60000, property.get());

        amt.pulse();
        assertEquals(Animation.Status.STOPPED, timeline.getStatus());
        assertEquals(Duration.minutes(1), timeline.getCurrentTime());
        assertEquals(60000, property.get());

    }

    @Test
    public void testPlay() {
        // play animation without keyframes
        timeline.play();
        assertEquals(Animation.Status.STOPPED, timeline.getStatus());
        assertEquals(0, (long)timeline.getCurrentTime().toMillis());

        setupTimeline(timeline, property, new Pair(Duration.ZERO, 0), new Pair(Duration.minutes(1), 60000));

        // play animation with rate == 0
        timeline.setRate(0.0);
        timeline.play();
        assertEquals(Animation.Status.RUNNING, timeline.getStatus());
        assertEquals(0, (long)timeline.getCurrentTime().toMillis());
        assertEquals(0, property.get());
        amt.pulse();
        assertEquals(0, (long)timeline.getCurrentTime().toMillis());
        assertEquals(0, property.get());
        timeline.stop();

        // normal play
        timeline.setRate(1.0);
        timeline.play();
        assertEquals(Animation.Status.RUNNING, timeline.getStatus());
        assertEquals(0, (long)timeline.getCurrentTime().toMillis());
        assertEquals(0, property.get());
        amt.pulse();
        assertEquals(TickCalculation.toDuration(100), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(100)), property.get());

        // calling play on a playing animation
        timeline.play();
        assertEquals(Animation.Status.RUNNING, timeline.getStatus());
        assertEquals(TickCalculation.toDuration(100), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(100)), property.get());
        amt.pulse();
        assertEquals(TickCalculation.toDuration(200), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(200)), property.get());

        // calling play on a paused animation
        timeline.pause();
        assertEquals(Animation.Status.PAUSED, timeline.getStatus());
        amt.pulse();
        assertEquals(TickCalculation.toDuration(200), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(200)), property.get());
        timeline.play();
        assertEquals(Animation.Status.RUNNING, timeline.getStatus());
        amt.pulse();
        assertEquals(TickCalculation.toDuration(300), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(300)), property.get());

        // jump and play
        timeline.stop();
        timeline.jumpTo(Duration.seconds(4));
        assertEquals(Duration.seconds(4), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(300)), property.get()); // Previous value
        timeline.play();
        assertEquals(Duration.seconds(4), timeline.getCurrentTime());
        assertEquals(4000, property.get());
        amt.pulse();
        assertEquals(Duration.seconds(4).add(TickCalculation.toDuration(100)), timeline.getCurrentTime());
        assertEquals(4000 + Math.round(TickCalculation.toMillis(100)), property.get());

        // calling play on a toggled animation
        timeline.jumpTo(Duration.minutes(1));
        timeline.setRate(-1.0);
        timeline.play();
        assertEquals(-1.0, timeline.getRate(), EPSILON);
        assertEquals(-1.0, timeline.getCurrentRate(), EPSILON);
        assertEquals(TickCalculation.toDuration(TickCalculation.fromMillis(60 * 1000)), timeline.getCurrentTime());
        assertEquals(60000, property.get());
        amt.pulse();
        assertEquals(TickCalculation.toDuration(TickCalculation.fromMillis(60 * 1000)).subtract(TickCalculation.toDuration(100)), timeline.getCurrentTime());
        assertEquals(60000 - Math.round(TickCalculation.toMillis(100)), property.get());

        //stop and play (no jump)
        timeline.setRate(1.0);
        timeline.stop();
        timeline.play();
        assertEquals(Duration.ZERO, timeline.getCurrentTime());
        assertEquals(60000 - Math.round(TickCalculation.toMillis(100)), property.get()); // previous value just after play
        amt.pulse();
        assertEquals(TickCalculation.toDuration(100), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(100)), property.get());

        // double play

        timeline.stop();
        timeline.play();
        assertEquals(Duration.ZERO, timeline.getCurrentTime());
        for (int i = 0 ; i < Duration.minutes(1).toSeconds() * 60 + 1; ++i) {
            amt.pulse();
        }
        assertEquals(Duration.minutes(1), timeline.getCurrentTime());
        assertEquals(60000, property.get());
        timeline.stop();
        timeline.play();
        assertEquals(Duration.ZERO, timeline.getCurrentTime());
        assertEquals(60000, property.get()); // previous value just after play
        amt.pulse();
        assertEquals(TickCalculation.toDuration(100), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(100)), property.get());

    }

    @Test
    public void testPlayFromStart() {
        final Duration oneSec = Duration.millis(1000);

        // play animation without keyframes
        timeline.setRate(-1.0);
        timeline.playFromStart();
        assertEquals(Animation.Status.STOPPED, timeline.getStatus());
        assertEquals(1.0, timeline.getRate(), EPSILON);
        assertEquals(0.0, timeline.getCurrentRate(), EPSILON);
        assertEquals(Duration.ZERO, timeline.getCurrentTime());

        // play animation with rate == 0
        setupTimeline(timeline, property, new Pair(Duration.ZERO, 0), new Pair(Duration.minutes(1), 60000));
        timeline.jumpTo(oneSec);

        assertEquals(oneSec, timeline.getCurrentTime());
        assertEquals(0, property.get()); //not running

        timeline.setRate(0.0);
        timeline.playFromStart();
        amt.pulse();
        assertEquals(Animation.Status.RUNNING, timeline.getStatus());
        assertEquals(0.0, timeline.getRate(), EPSILON);
        assertEquals(0.0, timeline.getCurrentRate(), EPSILON);
        amt.pulse();
        assertEquals(Duration.ZERO, timeline.getCurrentTime());
        assertEquals(0, property.get());
        timeline.stop();

        // normal play
        timeline.setRate(1.0);
        timeline.play();
        assertEquals(Duration.ZERO, timeline.getCurrentTime());
        assertEquals(0, property.get());
        amt.pulse();
        assertEquals(TickCalculation.toDuration(100), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(100)), property.get());
        timeline.playFromStart();
        assertEquals(Animation.Status.RUNNING, timeline.getStatus());
        assertEquals(1.0, timeline.getRate(), EPSILON);
        assertEquals(1.0, timeline.getCurrentRate(), EPSILON);
        assertEquals(Duration.ZERO, timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(100)), property.get()); // still old value
        amt.pulse();
        assertEquals(TickCalculation.toDuration(100), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(100)), property.get());

        // calling playFromStart on a stopped
        timeline.stop();
        timeline.playFromStart();
        assertEquals(1.0, timeline.getRate(), EPSILON);
        assertEquals(1.0, timeline.getCurrentRate(), EPSILON);
        assertEquals(Duration.ZERO, timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(100)), property.get()); // still old value
        amt.pulse();
        assertEquals(TickCalculation.toDuration(100), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(100)), property.get());

        // calling playFromStart on a toggled animation
        timeline.jumpTo(Duration.minutes(1));
        timeline.setRate(-1.0);
        amt.pulse();
        assertEquals(Duration.minutes(1).subtract(TickCalculation.toDuration(100)), timeline.getCurrentTime());
        assertEquals(60000 - Math.round(TickCalculation.toMillis(100)), property.get());
        timeline.playFromStart();
        assertEquals(1.0, timeline.getRate(), EPSILON);
        assertEquals(1.0, timeline.getCurrentRate(), EPSILON);
        assertEquals(Duration.ZERO, timeline.getCurrentTime());
        assertEquals(60000 - Math.round(TickCalculation.toMillis(100)), property.get()); // still old value

        // jump and playFromStart
        timeline.stop();
        timeline.setRate(1.0);
        timeline.jumpTo(Duration.seconds(4));
        assertEquals(Duration.seconds(4), timeline.getCurrentTime());
        assertEquals(60000 - Math.round(TickCalculation.toMillis(100)), property.get()); // still old value, not playing
        timeline.playFromStart();
        assertEquals(Duration.ZERO, timeline.getCurrentTime());
        assertEquals(60000 - Math.round(TickCalculation.toMillis(100)), property.get()); // still old value
        amt.pulse();
        assertEquals(TickCalculation.toDuration(100), timeline.getCurrentTime());
        assertEquals(Math.round(TickCalculation.toMillis(100)), property.get());

    }

    @Test
    public void testAutoReverse() {
        setupTimeline(timeline, property, new Pair(Duration.minutes(1), 60000));
        timeline.setAutoReverse(true);
        timeline.setCycleCount(-1);

        timeline.play();

        for (int i = 0; i < TickCalculation.fromDuration(Duration.seconds(60)) / 100 - 1; ++i) {
            amt.pulse();
        }

        amt.pulse();

        assertEquals(Animation.Status.RUNNING, timeline.getStatus());

        assertEquals(60000, property.get());

        amt.pulse();

        assertEquals(Animation.Status.RUNNING, timeline.getStatus());

        assertEquals(60000 - Math.round(TickCalculation.toMillis(100)), property.get());

    }

    @Test
    public void testAutoReverseWithJump() {
        setupTimeline(timeline, property, new Pair(Duration.minutes(1), 60000));
        timeline.setAutoReverse(true);
        timeline.setCycleCount(-1);

        timeline.play();

        timeline.jumpTo(Duration.seconds(60).subtract(TickCalculation.toDuration(100)));

        amt.pulse();

        assertEquals(Animation.Status.RUNNING, timeline.getStatus());

        assertEquals(60000, property.get());

        amt.pulse();

        assertEquals(Animation.Status.RUNNING, timeline.getStatus());

        assertEquals(60000 - Math.round(TickCalculation.toMillis(100)), property.get());

    }

}
