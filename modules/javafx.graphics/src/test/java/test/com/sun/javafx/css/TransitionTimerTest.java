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
import com.sun.javafx.tk.Toolkit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import test.util.memory.JMemoryBuddy;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.beans.property.Property;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableFloatProperty;
import javafx.css.SimpleStyleableIntegerProperty;
import javafx.css.SimpleStyleableLongProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.StyleOrigin;
import javafx.css.StyleableProperty;
import javafx.css.TransitionDefinition;
import javafx.css.TransitionPropertySelector;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static javafx.animation.Interpolator.*;
import static javafx.css.TransitionPropertySelector.*;
import static javafx.util.Duration.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"unchecked", "rawtypes"})
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

        timer.fire(seconds(0.4));
        assertEquals(1, trace.size());
        assertTrue(trace.get(0) > 0.3 && trace.get(0) < 0.5);

        timer.fire(seconds(0.7));
        assertEquals(2, trace.size());
        assertTrue(trace.get(1) == 1.0); // must be exactly 1
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
        timer.fire(seconds(0.9));
        assertFalse(flag[0]);
        timer.fire(seconds(0.2));
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
        timer.fire(seconds(0.2));
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
        timer.fire(seconds(0.2));
        assertFalse(flag[0]);
    }

    private static abstract class TransitionTimerMock extends TransitionTimer {
        long now = Toolkit.getToolkit().getPrimaryTimer().nanos();

        TransitionTimerMock(TransitionDefinition transition) {
            super(transition);
        }

        public void fire(Duration elapsedTime) {
            now += (long)(elapsedTime.toMillis() * 1000000);
            handle(now);
        }

        @Override
        protected Property<?> getProperty() {
            return null;
        }
    }

    record TestArgs(Node node, StyleableProperty property, Object initialValue, Object newValue) {}
    record TestRun(Supplier<TestArgs> args) {}

    private static List<TestRun> testTimerIsStoppedWhenPropertyIsCollected_parameters() {
        class DoubleTestNode extends Group {
            StyleableProperty testProperty = new SimpleStyleableDoubleProperty(METADATA, this, "testProperty", 0D);
            static CssMetaData<DoubleTestNode, Number> METADATA = new CssMetaData<>(
                    "testProperty", StyleConverter.getSizeConverter()) {
                @Override public boolean isSettable(DoubleTestNode styleable) { return true; }
                @Override public StyleableProperty<Number> getStyleableProperty(DoubleTestNode n) { return n.testProperty; }
            };
        }

        class FloatTestNode extends Group {
            StyleableProperty testProperty = new SimpleStyleableFloatProperty(METADATA, this, "testProperty");
            static CssMetaData<FloatTestNode, Number> METADATA = new CssMetaData<>(
                    "testProperty", StyleConverter.getSizeConverter()) {
                @Override public boolean isSettable(FloatTestNode styleable) { return true; }
                @Override public StyleableProperty<Number> getStyleableProperty(FloatTestNode n) { return n.testProperty; }
            };
        }

        class IntegerTestNode extends Group {
            StyleableProperty testProperty = new SimpleStyleableIntegerProperty(METADATA, this, "testProperty");
            static CssMetaData<IntegerTestNode, Number> METADATA = new CssMetaData<>(
                    "testProperty", StyleConverter.getSizeConverter()) {
                @Override public boolean isSettable(IntegerTestNode styleable) { return true; }
                @Override public StyleableProperty<Number> getStyleableProperty(IntegerTestNode n) { return n.testProperty; }
            };
        }

        class LongTestNode extends Group {
            StyleableProperty testProperty = new SimpleStyleableLongProperty(METADATA, this, "testProperty");
            static CssMetaData<LongTestNode, Number> METADATA = new CssMetaData<>(
                    "testProperty", StyleConverter.getSizeConverter()) {
                @Override public boolean isSettable(LongTestNode styleable) { return true; }
                @Override public StyleableProperty<Number> getStyleableProperty(LongTestNode n) { return n.testProperty; }
            };
        }

        class ObjectTestNode extends Group {
            StyleableProperty testProperty = new SimpleStyleableObjectProperty<>(METADATA, this, "testProperty");
            static CssMetaData<ObjectTestNode, Color> METADATA = new CssMetaData<>(
                    "testProperty", StyleConverter.getColorConverter()) {
                @Override public boolean isSettable(ObjectTestNode styleable) { return true; }
                @Override public StyleableProperty<Color> getStyleableProperty(ObjectTestNode n) { return n.testProperty; }
            };
        }

        return List.of(
            new TestRun(() -> {
                var node = new DoubleTestNode();
                return new TestArgs(node, node.testProperty, 0D, 1D);
            }),
            new TestRun(() -> {
                var node = new FloatTestNode();
                return new TestArgs(node, node.testProperty, 0F, 1F);
            }),
            new TestRun(() -> {
                var node = new IntegerTestNode();
                return new TestArgs(node, node.testProperty, 0, 1);
            }),
            new TestRun(() -> {
                var node = new LongTestNode();
                return new TestArgs(node, node.testProperty, 0L, 1L);
            }),
            new TestRun(() -> {
                var node = new ObjectTestNode();
                return new TestArgs(node, node.testProperty, Color.RED, Color.GREEN);
            })
        );
    }

    @ParameterizedTest
    @MethodSource("testTimerIsStoppedWhenPropertyIsCollected_parameters")
    public void testTimerIsStoppedWhenPropertyIsCollected(TestRun testRun) {
        JMemoryBuddy.memoryTest(test -> {
            Object timer = null;
            Method handleMethod = null;
            WeakReference<?> propertyRef = null;

            try {
                TestArgs args = testRun.args.get();
                args.node.getTransitions().add(
                    new TransitionDefinition(
                        TransitionPropertySelector.BEAN, "testProperty",
                        Duration.seconds(1), Duration.ZERO, Interpolator.LINEAR));

                handleMethod = AnimationTimer.class.getDeclaredMethod("handle", long.class);
                Field timerField = null;

                StyleableProperty property = args.property;
                Class<?> clazz = property.getClass();
                while (clazz != null) {
                    try {
                        timerField = clazz.getDeclaredField("timer");
                        timerField.setAccessible(true);
                        break;
                    } catch (NoSuchFieldException ignored) {
                        clazz = clazz.getSuperclass();
                    }
                }

                propertyRef = new WeakReference<>(property);
                timer = timerField.get(property);
                assertNull(timer);

                // Applies the initial value, which is never animated
                property.applyStyle(StyleOrigin.USER, args.initialValue);
                timer = timerField.get(property);
                assertNull(timer);

                // Applying the next value creates a timer
                property.applyStyle(StyleOrigin.USER, args.newValue);
                timer = timerField.get(property);
                assertNotNull(timer);
            } catch (ReflectiveOperationException ex) {
                fail(ex);
            }

            try {
                long now = System.nanoTime();

                // Wait until the property instance has been collected.
                while (propertyRef.get() != null) {
                    if (System.nanoTime() - now > 10_000_000_000L) {
                        fail("Test timed out");
                    }

                    System.gc();
                    Thread.sleep(100);
                }

                // Since the property has been collected, calling the timer will stop it
                // and make it eligible for collection.
                handleMethod.invoke(timer, System.nanoTime() + 10000);
                test.assertCollectable(timer);
            } catch (ReflectiveOperationException | InterruptedException ex) {
                fail(ex);
            }
        });
    }

}
