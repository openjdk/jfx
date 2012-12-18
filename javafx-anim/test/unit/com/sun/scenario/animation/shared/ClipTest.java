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
//<<<<<<< local
//package com.sun.scenario.animation.shared;
//
//import com.sun.javafx.animation.TimingTarget;
//import com.sun.scenario.ToolkitAccessorStub;
//import com.sun.scenario.ToolkitAccessor;
//import javafx.animation.KeyFrame;
//import javafx.util.Duration;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import static org.junit.Assert.*;
//
//public class ClipTest {
//
//    private static final double EPSILON = 1e-12;
//
//    private static final KeyFrame[] DEFAULT_KEY_FRAMES = new KeyFrame[0];
//    private static final int DEFAULT_MILLIS = 0;
//    private static final double DEFAULT_RATE = 1.0;
//    private static final int DEFAULT_REPEAT_COUNT = 1;
//    private static final boolean DEFAULT_AUTOREVERSE = false;
//    private static final boolean DEFAULT_INTERPOLATE = true;
//
//    private ToolkitAccessorStub accessor;
//    private Clip clip;
//    private TimingTargetMock target;
//    private ClipEnvelopeMock clipEnvelope;
//    private ClipCoreMock clipCore;
//
//    @Before
//    public void setUp() {
//        accessor = new ToolkitAccessorStub();
//        ToolkitAccessor.setInstance(accessor);
//        target = new TimingTargetMock();
//        clip = new Clip(target, null, DEFAULT_RATE, DEFAULT_REPEAT_COUNT, DEFAULT_AUTOREVERSE, DEFAULT_INTERPOLATE);
//        clipCore = new ClipCoreMock(clip);
//        clipEnvelope = new ClipEnvelopeMock(clip, clipCore);
//        clip.clipCore = clipCore;
//        clip.clipEnvelope = clipEnvelope;
//    }
//
//    @After
//    public void tearDown() {
//        ToolkitAccessor.setInstance(null);
//    }
//
//    @Test
//    public void testSetKeyFrames() {
//        final KeyFrame[] keyFrames42 = new KeyFrame[] {new KeyFrame(Duration.millis(42))};
//        clip.setKeyFrames(keyFrames42);
//        assertEquals(42L, clipEnvelope.timelineMillis);
//        assertEquals(42L, clipCore.timelineMillis);
//        assertArrayEquals(keyFrames42, clipCore.keyFrames);
//
//        final KeyFrame[] keyFramesIndef = new KeyFrame[] {new KeyFrame(Duration.INDEFINITE)};
//        clip.setKeyFrames(keyFramesIndef);
//        assertEquals((long)Duration.INDEFINITE.toMillis(), clipEnvelope.timelineMillis);
//        assertEquals((long)Duration.INDEFINITE.toMillis(), clipCore.timelineMillis);
//        assertArrayEquals(keyFramesIndef, clipCore.keyFrames);
//
//        final KeyFrame[] twoKeyFrames = new KeyFrame[] {new KeyFrame(Duration.ZERO), new KeyFrame(Duration.millis(7))};
//        clip.setKeyFrames(twoKeyFrames);
//        assertEquals(7L, clipEnvelope.timelineMillis);
//        assertEquals(7L, clipCore.timelineMillis);
//        assertArrayEquals(twoKeyFrames, clipCore.keyFrames);
//
//        clip.setKeyFrames(new KeyFrame[0]);
//        assertEquals(1L, clipEnvelope.timelineMillis);
//        assertEquals(1L, clipCore.timelineMillis);
//        assertArrayEquals(new KeyFrame[0], clipCore.keyFrames);
//
//        final KeyFrame[] keyFrames0 = new KeyFrame[] {new KeyFrame(Duration.ZERO)};
//        clip.setKeyFrames(keyFrames0);
//        assertEquals(1L, clipEnvelope.timelineMillis);
//        assertEquals(1L, clipCore.timelineMillis);
//        assertArrayEquals(keyFrames0, clipCore.keyFrames);
//
//        clip.setKeyFrames(null);
//        assertEquals(1L, clipEnvelope.timelineMillis);
//        assertEquals(1L, clipCore.timelineMillis);
//        assertArrayEquals(new KeyFrame[0], clipCore.keyFrames);
//
//    }
//
//    @Test
//    public void testEvaluateKeyValues() {
//        clip.evaluateKeyValues();
//        assertEquals(1, clipCore.evaluateKeyValuesCount);
//    }
//
//    @Test
//    public void testSetRate() {
//        clip.play();
//
//        clip.setRate(42);
//        assertEquals(42, clipEnvelope.rate, EPSILON);
//
//        assertTrue(clip.isRunning());
//        clip.setRate(0);
//        assertEquals(0, clipEnvelope.rate, EPSILON);
//        assertFalse(clip.isRunning());
//    }
//
//    @Test
//    public void testSetRepeatCount() {
//        assertEquals(DEFAULT_REPEAT_COUNT, clipEnvelope.repeatCount, EPSILON);
//
//        clip.setRepeatCount(3);
//        assertEquals(3, clipEnvelope.repeatCount, EPSILON);
//
//        clip.setRepeatCount(Clip.INDEFINITE);
//        assertEquals(Clip.INDEFINITE, clipEnvelope.repeatCount, EPSILON);
//    }
//
//    @Test(expected=IllegalArgumentException.class)
//    public void testIllegalSetRepeatCount0() {
//        clip.setRepeatCount(0);
//    }
//
//    @Test(expected=IllegalArgumentException.class)
//    public void testIllegalSetRepeatCount1() {
//        clip.setRepeatCount(-2);
//    }
//
//    @Test
//    public void testSetAutoReverse() {
//        assertEquals(DEFAULT_AUTOREVERSE, clipEnvelope.autoReverse);
//
//        clip.setAutoReverse(!DEFAULT_AUTOREVERSE);
//        assertEquals(!DEFAULT_AUTOREVERSE, clipEnvelope.autoReverse);
//
//        clip.setAutoReverse(DEFAULT_AUTOREVERSE);
//        assertEquals(DEFAULT_AUTOREVERSE, clipEnvelope.autoReverse);
//    }
//
//    @Test
//    public void testSetInterpolate() {
//        assertEquals(DEFAULT_INTERPOLATE, clipCore.interpolate);
//
//        clip.setInterpolate(!DEFAULT_INTERPOLATE);
//        assertEquals(!DEFAULT_INTERPOLATE, clipCore.interpolate);
//
//        clip.setInterpolate(DEFAULT_INTERPOLATE);
//        assertEquals(DEFAULT_INTERPOLATE, clipCore.interpolate);
//    }
//
//    @Test
//    public void testIsRunning() {
//        // testing stop-state
//        assertFalse(clip.isRunning());
//
//        // still testing stop-state
//        clip.pause();
//        assertFalse(clip.isRunning());
//
//        // scheduled state
//        clip.play();
//        assertTrue(clip.isRunning());
//
//        // scheduled-paused state
//        clip.pause();
//        assertTrue(clip.isRunning());
//
//        // playing state
//        clip.play();
//        clip.timePulse(1);
//        assertTrue(clip.isRunning());
//
//        // paused state
//        clip.pause();
//        assertTrue(clip.isRunning());
//
//        // stopped state (from pause)
//        clip.stop();
//        assertFalse(clip.isRunning());
//
//        // stopped state (from playing)
//        clip.play();
//        clip.stop();
//        assertFalse(clip.isRunning());
//    }
//
//    @Test
//    public void testJumpTo() {
//        final long indefinite =(long)Duration.INDEFINITE.toMillis();
//        clipEnvelope.setTimelineMillis(indefinite);
//
//        // jump to the end
//        clip.jumpTo(clipEnvelope.timelineMillis, true);
//        assertEquals(indefinite, clipEnvelope.time);
//
//        // jump in the middle
//        clip.jumpTo(42L, true);
//        assertEquals(42L, clipEnvelope.time);
//
//        // jump to the start
//        clip.jumpTo(0L, true);
//        assertEquals(0L, clipEnvelope.time);
//    }
//
//    @Test(expected=IllegalArgumentException.class)
//    public void testIllegalJumpTo0() {
//        clip.jumpTo(-1, true);
//    }
//
//    @Test(expected=IllegalArgumentException.class)
//    public void testIllegalJumpTo1() {
//        clip.jumpTo(1, true);
//    }
//
//    @Test
//    public void testPlay() {
//        assertFalse(clip.isRunning());
//
//        // regular play
//        clip.play();
//        assertTrue(clip.isRunning());
//        assertEquals(1, clipEnvelope.startCount);
//        assertEquals(1, clipCore.startCount);
//        assertEquals(0, target.beginCount);
//        assertEquals(0, target.resumeCount);
//        clipEnvelope.reset();
//        clipCore.reset();
//
//        // calling play on a scheduled clip
//        clip.play();
//        assertTrue(clip.isRunning());
//        assertEquals(0, clipEnvelope.startCount);
//        assertEquals(0, clipCore.startCount);
//        assertEquals(0, target.beginCount);
//        assertEquals(0, target.resumeCount);
//
//        // calling resume on a scheduled-paused clip
//        clip.pause();
//        clip.play();
//        assertTrue(clip.isRunning());
//        assertEquals(0, clipEnvelope.startCount);
//        assertEquals(0, clipCore.startCount);
//        assertEquals(0, target.beginCount);
//        assertEquals(0, target.resumeCount);
//
//        // actually starting the clip
//        clip.timePulse(1);
//        assertEquals(1, target.beginCount);
//        target.reset();
//
//        // calling play on a playing clip
//        clip.play();
//        assertTrue(clip.isRunning());
//        assertEquals(0, clipEnvelope.startCount);
//        assertEquals(0, clipCore.startCount);
//        assertEquals(0, target.beginCount);
//        assertEquals(0, target.resumeCount);
//
//        // calling resume on a playing clip
//        clip.pause();
//        clip.play();
//        assertTrue(clip.isRunning());
//        assertEquals(0, clipEnvelope.startCount);
//        assertEquals(0, clipCore.startCount);
//        assertEquals(1, target.resumeCount);
//        assertEquals(0, target.beginCount);
//    }
//
//    @Test
//    public void testStop() {
//        final long indefinite =(long)Duration.INDEFINITE.toMillis();
//        clipEnvelope.setTimelineMillis(indefinite);
//
//        // stop a stopped timeline
//        clip.jumpTo(42L, true);
//        clip.stop();
//        assertFalse(clip.isRunning());
//        assertEquals(0L, clipEnvelope.time);
//        target.reset();
//
//        // stop a scheduled timeline
//        clip.jumpTo(42L, true);
//        clip.play();
//        clip.stop();
//        assertFalse(clip.isRunning());
//        assertEquals(0L, clipEnvelope.time);
//        assertEquals(1, clipEnvelope.stopCount);
//        assertEquals(1, target.endCount);
//        target.reset();
//        clipEnvelope.reset();
//
//        // stop a playing timeline
//        clip.jumpTo(42L, true);
//        clip.play();
//        clip.timePulse(1);
//        clip.stop();
//        assertFalse(clip.isRunning());
//        assertEquals(0L, clipEnvelope.time);
//        assertEquals(1, clipEnvelope.stopCount);
//        assertEquals(1, target.endCount);
//        target.reset();
//        clipEnvelope.reset();
//
//        // stop a scheduled-paused timeline
//        clip.jumpTo(42L, true);
//        clip.play();
//        clip.pause();
//        clip.stop();
//        assertFalse(clip.isRunning());
//        assertEquals(0L, clipEnvelope.time);
//        assertEquals(1, clipEnvelope.stopCount);
//        assertEquals(1, target.endCount);
//        target.reset();
//        clipEnvelope.reset();
//
//        // stop a paused timeline
//        clip.jumpTo(42L, true);
//        clip.play();
//        clip.timePulse(1);
//        clip.pause();
//        clip.stop();
//        assertFalse(clip.isRunning());
//        assertEquals(0L, clipEnvelope.time);
//        assertEquals(1, clipEnvelope.stopCount);
//        assertEquals(1, target.endCount);
//    }
//
//    @Test
//    public void testReachedEnd() {
//        // stop a stopped timeline
//        clip.reachedEnd();
//        assertFalse(clip.isRunning());
//        target.reset();
//
//        // stop a scheduled timeline
//        clip.play();
//        clip.reachedEnd();
//        assertFalse(clip.isRunning());
//        assertEquals(1, target.endCount);
//        target.reset();
//
//        // stop a playing timeline
//        clip.play();
//        clip.timePulse(1);
//        clip.reachedEnd();
//        assertFalse(clip.isRunning());
//        assertEquals(1, target.endCount);
//        target.reset();
//
//        // stop a scheduled-paused timeline
//        clip.play();
//        clip.pause();
//        clip.reachedEnd();
//        assertFalse(clip.isRunning());
//        assertEquals(1, target.endCount);
//        target.reset();
//
//        // stop a paused timeline
//        clip.play();
//        clip.timePulse(1);
//        clip.pause();
//        clip.reachedEnd();
//        assertFalse(clip.isRunning());
//        assertEquals(1, target.endCount);
//    }
//
//    @Test
//    public void testToggle() {
//        clip.toggle();
//        assertEquals(1, target.toggleCount);
//    }
//
//    @Test
//    public void testPause() {
//        // pause a stopped timeline
//        clip.pause();
//        assertFalse(clip.isRunning());
//        assertEquals(0, target.pauseCount);
//
//        // pause a scheduled timeline
//        clip.play();
//        clip.pause();
//        assertTrue(clip.isRunning());
//        clip.timePulse(1);
//        assertEquals(1, target.pauseCount);
//        target.reset();
//
//        // pause a playing timeline
//        clip.stop();
//        clip.play();
//        clip.timePulse(1);
//        clip.pause();
//        assertTrue(clip.isRunning());
//        assertEquals(1, target.pauseCount);
//        target.reset();
//
//        // pause a scheduled-paused timeline
//        clip.stop();
//        clip.play();
//        clip.pause();
//        clip.pause();
//        clip.timePulse(1);
//        assertTrue(clip.isRunning());
//        assertEquals(1, target.pauseCount);
//        target.reset();
//
//        // pause a paused timeline
//        clip.stop();
//        clip.play();
//        clip.timePulse(1);
//        clip.pause();
//        clip.pause();
//        assertTrue(clip.isRunning());
//        assertEquals(1, target.pauseCount);
//    }
//
//    @Test
//    public void testResume() {
//        // resume a stopped timeline
//        clip.resume();
//        assertFalse(clip.isRunning());
//        assertEquals(0, target.resumeCount);
//
//        // resume a scheduled timeline
//        clip.play();
//        clip.resume();
//        assertTrue(clip.isRunning());
//        clip.timePulse(1);
//        assertEquals(0, target.resumeCount);
//
//        // resume a playing timeline
//        clip.stop();
//        clip.play();
//        clip.timePulse(1);
//        clip.resume();
//        assertTrue(clip.isRunning());
//        assertEquals(0, target.resumeCount);
//
//        // resume a scheduled-paused timeline
//        clip.stop();
//        clip.play();
//        clip.pause();
//        clip.resume();
//        clip.timePulse(1);
//        assertTrue(clip.isRunning());
//        assertEquals(0, target.resumeCount);
//        target.reset();
//
//        // resume a paused timeline
//        clip.stop();
//        clip.play();
//        clip.timePulse(1);
//        clip.pause();
//        clip.resume();
//        assertTrue(clip.isRunning());
//        assertEquals(1, target.resumeCount);
//    }
//
//    @Test
//    public void testSetTime() {
//        clip.setTime(7L);
//        assertEquals(7L, target.timingEvent);
//    }
//
//    @Test
//    public void testTimePulseNotPlaying() {
//        accessor.setShouldUseNanoTime(false);
//
//        // stopped clip
//        assertTrue(clip.timePulse(1));
//
//        // scheduled paused clip
//        clip.play();
//        clip.pause();
//        assertTrue(clip.timePulse(1));
//        assertEquals(1, target.beginCount);
//        assertEquals(0, clipEnvelope.timePulse);
//
//        // paused clip
//        clip.play();
//        clip.timePulse(1);
//        target.reset();
//        clipEnvelope.reset();
//        clip.pause();
//        assertTrue(clip.timePulse(2));
//        assertEquals(0, target.beginCount);
//        assertEquals(0, clipEnvelope.timePulse);
//
//        // scheduled clip
//        clip.stop();
//        clip.play();
//        assertFalse(clip.timePulse(1));
//        assertEquals(1, target.beginCount);
//        assertEquals(1, clipEnvelope.timePulse);
//        target.reset();
//        clipEnvelope.reset();
//    }
//
//    @Test
//    public void testTimePulseNormalPlaying() {
//        // normal play
//        clip.play();
//        clip.timePulse(1);
//        target.reset();
//        assertFalse(clip.timePulse(2));
//        assertEquals(0, target.beginCount);
//        assertEquals(2, clipEnvelope.timePulse);
//
//        // play with pause
//        accessor.setMillis(3);
//        clip.pause();
//        accessor.setMillis(4);
//        clip.play();
//        assertFalse(clip.timePulse(5));
//        assertEquals(4, clipEnvelope.timePulse);
//    }
//
//    @Test
//    public void testTimePulseCustomResolutionPlaying() {
//        final Clip customClip = new Clip(target, 10, null, DEFAULT_RATE, DEFAULT_REPEAT_COUNT, DEFAULT_AUTOREVERSE, DEFAULT_INTERPOLATE);
//        customClip.clipCore = clipCore;
//        customClip.clipEnvelope = clipEnvelope;
//
//        // pulse smaller resolution
//        customClip.play();
//        customClip.timePulse(1);
//        assertFalse(customClip.timePulse(2));
//        assertEquals(0, clipEnvelope.timePulse);
//
//        // first pulse
//        assertFalse(customClip.timePulse(19));
//        assertEquals(19, clipEnvelope.timePulse);
//
//        // second pulse
//        assertFalse(customClip.timePulse(20));
//        assertEquals(20, clipEnvelope.timePulse);
//
//        // play with pause
//        accessor.setMillis(21);
//        customClip.pause();
//        accessor.setMillis(22);
//        customClip.play();
//        assertFalse(customClip.timePulse(30));
//        assertEquals(20, clipEnvelope.timePulse);
//        assertFalse(customClip.timePulse(31));
//        assertEquals(30, clipEnvelope.timePulse);
//    }
//
//    private static class TimingTargetMock implements TimingTarget {
//
//        private long timingEvent = 0;
//
//        private int beginCount = 0;
//        private int endCount = 0;
//        private int toggleCount = 0;
//        private int pauseCount = 0;
//        private int resumeCount = 0;
//
//        public TimingTargetMock() {
//        }
//
//        private void reset() {
//            beginCount = 0;
//            endCount = 0;
//            toggleCount = 0;
//            pauseCount = 0;
//            resumeCount = 0;
//        }
//
//        @Override
//        public void timingEvent(long timingEvent) {this.timingEvent = timingEvent;}
//
//        @Override
//        public void begin() {beginCount++;}
//
//        @Override
//        public void end() {endCount++;}
//
//        @Override
//        public void toggle() {toggleCount++;}
//
//        @Override
//        public void pause() {pauseCount++;}
//
//        @Override
//        public void resume() {resumeCount++;}
//    }
//
//    private static class ClipEnvelopeMock extends ClipEnvelope {
//
//        private long time = 0;
//        private long timePulse = 0;
//
//        private int startCount = 0;
//        private int stopCount = 0;
//
//        public ClipEnvelopeMock(Clip clip, ClipCore clipCore) {
//            super(clip, clipCore, DEFAULT_MILLIS, DEFAULT_RATE, DEFAULT_REPEAT_COUNT, DEFAULT_AUTOREVERSE);
//        }
//
//        private void reset() {
//            time = 0;
//            timePulse = 0;
//            startCount = 0;
//            stopCount = 0;
//        }
//
//        @Override
//        public ClipEnvelope setTimelineMillis(long millis) {
//            this.timelineMillis = millis;
//            return this;
//        }
//
//        @Override
//        public void setRate(double rate) {this.rate = rate;}
//
//        @Override
//        public ClipEnvelope setRepeatCount(int repeatCount) {
//            this.repeatCount = repeatCount;
//            return this;
//        }
//
//        @Override
//        public void setAutoReverse(boolean autoReverse) {this.autoReverse = autoReverse;}
//
//        @Override
//        protected void setPositionAtStart() {
//            // not needed
//        }
//
//        @Override
//        public void timePulse(long timePulse) {this.timePulse = timePulse;}
//
//        @Override
//        public void jumpTo(long time, boolean ignore) {
//            this.time = time;
//        }
//
//        @Override
//        public void start() {startCount++;}
//
//        @Override
//        public void stop() {stopCount++;}
//    }
//
//    private static class ClipCoreMock extends ClipCore {
//
//        private KeyFrame[] keyFrames = DEFAULT_KEY_FRAMES;
//        private long timelineMillis = DEFAULT_MILLIS;
//        private boolean interpolate = DEFAULT_INTERPOLATE;
//
//        private int evaluateKeyValuesCount = 0;
//        private int startCount = 0;
//
//        public ClipCoreMock(Clip clip) {
//            super(clip, DEFAULT_KEY_FRAMES, DEFAULT_MILLIS, DEFAULT_INTERPOLATE);
//        }
//
//        private void reset() {
//            evaluateKeyValuesCount = 0;
//            startCount = 0;
//        }
//
//        @Override
//        public void setKeyFrames(KeyFrame[] keyFrames, long timelineMillis) {
//            this.keyFrames = keyFrames;
//            this.timelineMillis = timelineMillis;
//        }
//
//        @Override
//        public void setInterpolate(boolean interpolate) {this.interpolate = interpolate;}
//
//        @Override
//        public void evaluateKeyValues() {evaluateKeyValuesCount++;}
//
//        @Override
//        public void start() {startCount++;}
//    }
//
//
//}

