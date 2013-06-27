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

import com.sun.javafx.functions.Function0;

import javafx.util.Duration;

public abstract class AnimationFunctionalTestBase {
    private Thread delayedThread;
    private Error failError;

    private String name = "";

    protected AnimationFunctionalTestBase() {}

    protected AnimationFunctionalTestBase(String name) {
        this.name = name;
    }

    // used by subclasses for delayed function invocation
    protected final void delay(long ms) {
        delayedThread = Thread.currentThread();
        failError = null;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignore) {}
        finally {
            delayedThread = null;
            if (failError != null) {
                throw failError; // re-throw the error on the main thread
            }
        }
    }

    protected final void delayFor(Timeline t) {
        delay((long) t.getTotalDuration().toMillis() + 300);
    }

    protected final void fail(String message) {
        failError = new AssertionError(name + " FAILED: " + message);
        if (delayedThread != null && delayedThread != Thread.currentThread()) {
            // error happened on the timeline thread: should re-throw it on the main thread
            // so that JUnit could catch it
            delayedThread.interrupt();
        } else {
            throw failError;
        }
    }

    protected class SimpleKeyFrame extends KeyFrame {
        public SimpleKeyFrame(double time) {
            this(time, false);
        }

        public SimpleKeyFrame(double time, boolean canSkip, KeyValue... values) {
            super(Duration.valueOf(time), values);
            setAction(
                    new Function0<Void>() {
                        @Override public Void invoke() {
                            action();
                            return null;
                        }
                    }
            );
            setCanSkip(canSkip);
        }

        protected void action() {}
    }

    protected class SimpleTimeline extends Timeline {
        public SimpleTimeline(KeyFrame... frames) {
            this(1, frames);
        }

        public SimpleTimeline(int repeatCount, KeyFrame... frames) {
            init(repeatCount, frames);
        }
        public SimpleTimeline(double framerate, int repeatCount, KeyFrame... frames) {
            super(framerate);
            init(repeatCount, frames);
        }

        public void playBackward() {
            setRate(-Math.abs(getRate()));
            playFromEnd();
        }

        public void playFromEnd() {
            getCurrentTime(getCycleDuration());
            play();
        }


        private void init(int repeatCount, KeyFrame... frames) {
            setCycleCount(repeatCount);
            if (frames != null) {
                getKeyFrames().addAll(frames);
            }
        }
    }

    public class SimpleKeyValueTarget implements KeyValueTarget {
        private Object value;
        private Type type;

        public SimpleKeyValueTarget(Object value) {
            this.value = value;
            final Class<?> clazz = value.getClass();
            type = ((clazz == boolean.class) || (clazz == Boolean.class))? Type.BOOLEAN :
            ((clazz == byte.class) || (clazz == Byte.class))? Type.BYTE :
            ((clazz == double.class) || (clazz == Double.class))? Type.DOUBLE :
            ((clazz == float.class) || (clazz == Float.class))? Type.FLOAT :
            ((clazz == int.class) || (clazz == Integer.class))? Type.INTEGER :
            ((clazz == long.class) || (clazz == Long.class))? Type.LONG :
            ((clazz == short.class) || (clazz == Short.class))? Type.SHORT :
            Type.OBJECT;
        }
        @Override public Object get() {return getValue();}
        @Override public KeyValueTarget.Type getType() {return type;}
        @Override public Object getValue() {return value;}
        @Override public void set(Object value) {setValue(value);}
        @Override public void setValue(Object o) {value = o;}
        @Override public KeyValueTarget unwrap() {return this;}
    };


    public class TestSet {
        public final double TARGET_VALUE = 700;
        public final double TIME_VALUE = 1000;
        public final Duration TIME_DURATION = Duration.valueOf(TIME_VALUE);
        protected SimpleKeyValueTarget target = new SimpleKeyValueTarget(0);
        protected SimpleTimeline timeline;
        protected int[] count = {0, 0};

        public TestSet() {
            this(Double.MAX_VALUE);
        }

        public TestSet(double frameRate) {
            timeline = new SimpleTimeline(frameRate, 1,
                    new SimpleKeyFrame(0) {
                        @Override protected void action() {
                            count[0]++;
                        }
                    },
                    new SimpleKeyFrame(TIME_VALUE, false, new KeyValue(target, TARGET_VALUE)) {
                        @Override protected void action() {
                            count[1]++;
                        }
                    }
            );
        }

        protected void resetCount() {
            count[0] = 0;
            count[1] = 0;
        }

        protected final void check(int count0, int count1, boolean running) {
            checkVisitCount(0, count0);
            checkVisitCount(1, count1);
            checkRunning(running);
        }

        protected final void check(int count0, int count1, boolean running, double time, double target) {
            check(count0, count1, running);
            checkTime(time);
            checkTarget(target);
        }

        protected void checkVisitCount(int frameIndex, int expected) {
            if (count[frameIndex] != expected) {
                fail("KeyFrame #" + frameIndex + " visited " + count[frameIndex]
                        + " times instead of " + expected + signature());
            }
        }

        protected final void checkRunning(boolean running) {
            if (running && !timeline.isRunning()) {
                fail("should be running");
            } else if (!running && timeline.isRunning()) {
                fail("should not be running");
            }
        }

        protected void checkValue(String message, double value, double expected) {
            if (value != expected) {
                fail(message + " actual: " + value + ", expected: " + expected + signature());
            }
        }

        protected void checkTime(double expected) {
            checkValue("time ", timeline.getCurrentTime().toMillis(), expected);
        }

        protected void checkTarget(double expected) {
            checkValue("target ", Double.valueOf(target.getValue().toString()), expected);
        }

        String signature() {
            return ", rate=" + timeline.getRate() + ", repeatCount=" + timeline.getRepeatCount();
        }

    }

}
