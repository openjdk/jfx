/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.binding;

import com.sun.javafx.binding.ListenerList;
import com.sun.javafx.binding.ListenerListBase;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Base test class suitable for testing {@link ListenerList} and {@link com.sun.javafx.binding.OldValueCachingListenerList}.
 *
 * @param <L> the class to test
 */
public abstract class ListenerListTestBase<L extends ListenerListBase> {
    private final List<String> records = new ArrayList<>();
    private final InvalidationListener il1 = obs -> records.add("IL1: invalidated");
    private final InvalidationListener il2 = obs -> records.add("IL2: invalidated");
    private final ChangeListener<?> cl1 = (obs, o, n) -> records.add("CL1: changed from " + o + " to " + n);
    private final ChangeListener<?> cl2 = (obs, o, n) -> records.add("CL2: changed from " + o + " to " + n);

    private int getterCalled;

    private final StringProperty property = new SimpleStringProperty("-") {
        @Override
        public String get() {
            getterCalled++;

            return super.get();
        }
    };

    protected abstract L create(Object listener1, Object listener2);
    protected abstract <T> void notifyListeners(L list, ObservableValue<? extends T> property, T oldValue);

    @Test
    void shouldNotifyAllListeners() {
        L list = create(cl1, il1);

        list.add(cl2);
        list.add(il2);

        property.set("A");
        notifyListeners(list, property, "-");

        assertRecords(
            "IL1: invalidated",
            "IL2: invalidated",
            "CL1: changed from - to A",
            "CL2: changed from - to A"
        );
    }

    @Test
    void shouldNotifyAllListenersEvenWhenTheyThrowExceptions() {
        InvalidationListener il1 = obs -> { records.add("IL1: invalidated"); throw new RuntimeException(); };
        InvalidationListener il2 = obs -> { records.add("IL2: invalidated"); throw new RuntimeException(); };
        ChangeListener<?> cl1 = (obs, o, n) -> { records.add("CL1: changed from " + o + " to " + n); throw new RuntimeException(); };
        ChangeListener<?> cl2 = (obs, o, n) -> { records.add("CL2: changed from " + o + " to " + n); throw new RuntimeException(); };

        L list = create(cl1, il1);

        list.add(cl2);
        list.add(il2);

        // Replace exception handler to reduce log spam during test:
        UncaughtExceptionHandler oldExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();

        try {
            AtomicInteger exceptions = new AtomicInteger();
            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> exceptions.addAndGet(1));

            property.set("A");
            notifyListeners(list, property, "-");

            assertRecords(
                "IL1: invalidated",
                "IL2: invalidated",
                "CL1: changed from - to A",
                "CL2: changed from - to A"
            );

            assertEquals(4, exceptions.get());
        }
        finally {
            Thread.currentThread().setUncaughtExceptionHandler(oldExceptionHandler);
        }
    }

    @Nested
    class WhenListenersAreAddedOrRemovedDuringANotification {

        @Test
        void shouldNotNotifyListenersThatWereRemovedBeforeBeingCalled() {
            L list = create(cl1, il1);

            list.add((InvalidationListener) obs -> list.remove(cl2));
            list.add((InvalidationListener) obs -> list.remove(il2));
            list.add(cl2);
            list.add(il2);

            property.set("A");
            notifyListeners(list, property, "-");

            assertRecords(
                "IL1: invalidated",
                "CL1: changed from - to A"
            );
        }

        @Test
        void shouldNotNotifyListenersNextTimeWhenTheyWereRemovedAfterTheyWereCalled() {
            L list = create(cl1, il1);

            list.add(cl2);
            list.add(il2);
            list.add((ChangeListener<String>) (obs, o, n) -> list.remove(cl2));
            list.add((ChangeListener<String>) (obs, o, n) -> list.remove(il2));

            property.set("A");
            notifyListeners(list, property, "-");

            assertRecords(
                "IL1: invalidated",
                "IL2: invalidated",
                "CL1: changed from - to A",
                "CL2: changed from - to A"
            );

            property.set("B");
            notifyListeners(list, property, "A");

            assertRecords(
                "IL1: invalidated",
                "CL1: changed from A to B"
            );
        }

        @Test
        void shouldNotNotifyAnyListenersAddedUntilNextNotification() {
            L list = create(cl1, il1);

            list.add((InvalidationListener) obs -> list.add((ChangeListener<String>) (obs2, o, n) -> records.add("CL3: changed from " + o + " to " + n)));
            list.add((InvalidationListener) obs -> list.add((InvalidationListener) obs2 -> records.add("IL3: invalidated")));
            list.add(cl2);
            list.add(il2);

            property.set("A");
            notifyListeners(list, property, "-");

            assertRecords(
                "IL1: invalidated",
                "IL2: invalidated",
                "CL1: changed from - to A",
                "CL2: changed from - to A"
            );

            property.set("B");
            notifyListeners(list, property, "A");

            assertRecords(
                "IL1: invalidated",
                "IL2: invalidated",
                "IL3: invalidated",
                "CL1: changed from A to B",
                "CL2: changed from A to B",
                "CL3: changed from A to B"
            );
        }
    }

    @Test
    void shouldNotGetCurrentValueWhenThereAreNoChangeListeners() {
        L list = create(il1, il2);

        assertEquals(0, getterCalled);

        notifyListeners(list, property, "-");

        assertRecords(
            "IL1: invalidated",
            "IL2: invalidated"
        );

        assertEquals(0, getterCalled);
    }

    @Test
    void shouldNotGetCurrentValueWhenChangeListenersWereRemovedBeforeBeingCalled() {
        L list = create(il1, cl1);

        list.add((InvalidationListener) obs -> list.remove(cl1));

        assertEquals(0, getterCalled);

        notifyListeners(list, property, "-");

        assertRecords(
            "IL1: invalidated"
        );

        assertEquals(0, getterCalled);
    }

    @Nested
    class WhenANestedChangeOccurs {

        @Test
        void shouldAllowSimpleVetoingOfValue() {
            BooleanProperty property = new SimpleBooleanProperty(true);

            L list = create(il1, il2);

            list.add((ChangeListener<Boolean>) (obs, o, n) -> {
                records.add("CL1: changed from " + o + " to " + n);
            });

            list.add((ChangeListener<Boolean>) (obs, o, n) -> {
                records.add("CL2: changed from " + o + " to " + n);

                if (n) {  // This check is normally done by the property
                    property.set(false);
                    notifyListeners(list, property, n);
                }
            });

            list.add((ChangeListener<Boolean>) (obs, o, n) -> {
                records.add("CL3: changed from " + o + " to " + n);
            });

            list.remove(il1);
            list.remove(il2);

            notifyListeners(list, property, false);  // triggers change from false to true

            //assertConsistentChangeSequence(false);
            assertEquals(4, records.size());  // 2 at top level, 2 nested, third listener not notified (unchanged)
        }

        @Test
        void shouldAllowSimpleVetoingOfValue_2() {
            StringProperty property = new SimpleStringProperty("B");

            L list = create(il1, il2);

            list.add((ChangeListener<String>) (obs, o, n) -> {
                records.add("CL1: changed from " + o + " to " + n);
            });

            list.add((ChangeListener<String>) (obs, o, n) -> {
                records.add("CL2: changed from " + o + " to " + n);

                if (!n.equals("A")) {  // This check is normally done by the property
                    property.set("A");
                    notifyListeners(list, property, n);
                }
            });

            list.add((ChangeListener<String>) (obs, o, n) -> {
                records.add("CL3: changed from " + o + " to " + n);
            });

            list.remove(il1);
            list.remove(il2);

            notifyListeners(list, property, "A");  // triggers change from A to B

            assertConsistentChangeSequence("A");
            assertEquals(4, records.size());  // 2 at top level, 2 nested, third listener not notified (unchanged)
        }

        /**
         * This tests add two change listeners that will never agree
         * on the final value (one sets the value to 2, the other to 3).
         * In previous implementations which would notify listeners in
         * all cases, even when their value didn't change, this would
         * automatically result in a StackOverflowError, alerting the user
         * of serious problems.
         *
         * This implementation has to specifically detect problematic use of
         * multiple value modifying listeners in order to alert the user.
         */
        @Test
        void shouldDetectNonConvergence() {
            LongProperty property = new SimpleLongProperty(1);

            L list = create(il1, il2);

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL1: changed from " + o + " to " + n);

                if (v != 2) {  // This check is normally done by the property
                    property.set(2);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL2: changed from " + o + " to " + n);

                if (v != 3) {  // This check is normally done by the property
                    property.set(3);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                records.add("CL3: changed from " + o + " to " + n);
            });

            StackOverflowError e = assertThrows(StackOverflowError.class, () -> notifyListeners(list, property, 0));

            assertEquals("non-converging value detected in value modifying listeners on LongProperty [value: 2]; original value was: 0", e.getMessage());
        }

        /**
         * A variant of the non-convergence test with three listeners
         * that will never agree.
         */
        @Test
        void shouldDetectNonConvergence_2() {
            LongProperty property = new SimpleLongProperty(1);

            L list = create(il1, il2);

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL1: changed from " + o + " to " + n);

                if (v < 20) {
                    property.set(20);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL2: changed from " + o + " to " + n);

                if (v < 30) {
                    property.set(30);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL3: changed from " + o + " to " + n);

                if (v > 10) {
                    property.set(10);
                    notifyListeners(list, property, v);
                }
            });

            StackOverflowError e = assertThrows(StackOverflowError.class, () -> notifyListeners(list, property, 0));

            assertEquals("non-converging value detected in value modifying listeners on LongProperty [value: 30]; original value was: 0", e.getMessage());
        }

        /**
         * A variant of the non-convergence test with three listeners
         * that will never agree and an additional observing listener.
         */
        @Test
        void shouldDetectNonConvergence_3() {
            LongProperty property = new SimpleLongProperty(1);

            L list = create(il1, il2);

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL1: changed from " + o + " to " + n);

                if (v < 20) {
                    property.set(20);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL2: changed from " + o + " to " + n);

                if (v < 30) {
                    property.set(30);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL3: changed from " + o + " to " + n);

                if (v > 10) {
                    property.set(10);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                records.add("CL4: changed from " + o + " to " + n);
            });

            StackOverflowError e = assertThrows(StackOverflowError.class, () -> notifyListeners(list, property, 0));

            assertEquals("non-converging value detected in value modifying listeners on LongProperty [value: 30]; original value was: 0", e.getMessage());
        }

        /**
         * A single listener that keeps changing values. This variant results in an actual
         * StackOverflowError.
         */
        @Test
        void shouldDetectNonConvergence_4() {
            LongProperty property = new SimpleLongProperty(1);

            L list = create(il1, il2);

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL1: changed from " + o + " to " + n);

                property.set(5 + (v % 3));
                notifyListeners(list, property, v);
            });

            StackOverflowError e = assertThrows(StackOverflowError.class, () -> notifyListeners(list, property, 0));

            assertNull(e.getMessage());  // this was an actual StackOverflowError, and so has no message
        }

        /**
         * This test adds four change listeners that can't easily agree on their final
         * value. They're looking for a number divisible by 2, 3, 5 and 7, and will keep
         * incrementing the value by 1 until they all agree. The final value should be
         * 210, which matches the above criteria.
         */
        @Test
        void shouldSendChangesThatMakeSense() {
            LongProperty property = new SimpleLongProperty(1);
            L list = create(il1, il2);

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL1: changed from " + o + " to " + n);

                if (v % 5 != 0) {
                    property.set(v + 1);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL2: changed from " + o + " to " + n);

                if (v % 2 != 0) {
                    property.set(v + 1);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL3: changed from " + o + " to " + n);

                if (v % 3 != 0) {
                    property.set(v + 1);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL4: changed from " + o + " to " + n);

                if (v % 7 != 0) {
                    property.set(v + 1);
                    notifyListeners(list, property, v);
                }
            });

            notifyListeners(list, property, 0);

            assertConsistentChangeSequence("210");
            assertEquals(700, records.size());  // it should be 280 changes + 420 invalidations; 420 because 210 changes * 2 invalidation listeners
        }

        /**
         * This test adds a mix of listeners that can't easily agree on their final
         * value. They're looking for a number divisible by 2, 3, 5 and 7, and will keep
         * incrementing the value by 1 until they all agree. The final value should be
         * 210, which matches the above criteria.
         */
        @Test
        void shouldSendChangesThatMakeSense_VariantAMixOfListeners() {
            LongProperty property = new SimpleLongProperty(1);
            L list = create(il1, il2);

            list.add((InvalidationListener) obs -> {
                long v = property.get();  // act as change listener

                records.add("IL3: current value " + v);

                if (v % 5 != 0) {
                    property.set(v + 1);
                    notifyListeners(list, property, v);
                }
            });

            list.add((InvalidationListener) obs -> {
                long v = property.get();  // act as change listener

                records.add("IL4: current value " + v);

                if (v % 2 != 0) {
                    property.set(v + 1);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL3: changed from " + o + " to " + n);

                if (v % 3 != 0) {
                    property.set(v + 1);
                    notifyListeners(list, property, v);
                }
            });

            list.add((ChangeListener<Number>) (obs, o, n) -> {
                long v = n.longValue();

                records.add("CL4: changed from " + o + " to " + n);

                if (v % 7 != 0) {
                    property.set(v + 1);
                    notifyListeners(list, property, v);
                }
            });

            notifyListeners(list, property, 0);

            assertConsistentChangeSequence("210");

            /*
             * The total of 700 comes from:
             * - 210 changes per invalidation listener for the first two listeners that do nothing (il1, il2)
             * - 280 changes total for all listeners that trigger nested changes (regardless of listener type)
             */

            assertEquals(700, records.size());
        }
    }


    private void assertRecords(String... expected) {
        assertEquals(Arrays.asList(expected), records);
        records.clear();
    }

    private Pattern PATTERN = Pattern.compile("(?<listener>.*): changed from (?<old>.*) to (?<new>.*)");
    private Pattern INVALIDATION_PATTERN = Pattern.compile("(?<listener>.*): current value (?<current>.*)");

    /**
     * This method checks that all changes follow the following rules:
     * - The old and new value cannot be equal
     * - The previous new value must be equal to the old value
     * - The final value is the one that is expected
     *
     * @param expectedFinalValue the final value that is expected
     */
    private void assertConsistentChangeSequence(String expectedFinalValue) {
        Map<String, String> oldValues = new HashMap<>();

        for (String record : records) {
            Matcher matcher = PATTERN.matcher(record);

            if (matcher.matches()) {
                String name = matcher.group("listener");
                String o = matcher.group("old");
                String n = matcher.group("new");

                assertNotEquals(o, n, "Listener " + name + " received a change that wasn't a change: " + record);

                if (oldValues.containsKey(name)) {
                    assertEquals(o, oldValues.get(name), "Listener " + name + " received an incorrect old value; expected " + oldValues.get(name) + " but was: " + record);
                }

                oldValues.put(name, n);
            }
        }

        for (String record : records) {
            Matcher matcher = INVALIDATION_PATTERN.matcher(record);

            if (matcher.matches()) {
                String name = matcher.group("listener");
                String c = matcher.group("current");

                oldValues.put(name, c);
            }
        }

        for (Entry<String, String> entry : oldValues.entrySet()) {
            assertEquals(expectedFinalValue, entry.getValue(), "Listener " + entry.getKey() + " had as final value: " + entry.getValue() + ", but expected was: " + expectedFinalValue);
        }
    }
}
