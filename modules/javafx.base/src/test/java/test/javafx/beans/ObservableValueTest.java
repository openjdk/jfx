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

package test.javafx.beans;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ObservableValueTest {

    /**
     * These are counts for the number of listeners added to a property
     * or binding. The test code switches between these numbers, and checks
     * if all listeners function as expected. Various combinations are used
     * here as the implementation also switches between various states
     * depending on the number of listeners attached, so some exhaustive
     * checking is needed to ensure all works correct.
     */
    private static final int[] LISTENER_COUNTS = new int[] {1, 2, 0, 50, 3, 7};

    /*
     * ObservableValue cases to test. There is a case for each type of
     * property and binding (for good coverage and to ensure they all
     * work correctly and nothing was missed). Each case uses two
     * values that it switches between to see if a change and/or
     * invalidation occurs as expected.
     *
     * The binding variants also have two helper functions, one that
     * creates a binding derived from the main observable (which will
     * be tested against) and one that modifies the original observable
     * (as the binding can't be modified directly).
     */
    static Stream<Arguments> inputs() {
        List<Case<?, ?>> cases = List.of(
            // cases for properties:
            Case.of(new SimpleBooleanProperty(), true, false),
            Case.of(new SimpleIntegerProperty(), 42, 47),
            Case.of(new SimpleLongProperty(), 42L, 47L),
            Case.of(new SimpleFloatProperty(), 0.5f, 1.0f),
            Case.of(new SimpleDoubleProperty(), 0.5, 1.0),
            Case.of(new SimpleStringProperty(), "A", "B"),
            Case.of(new SimpleObjectProperty<>(), "A", "B"),

            // cases for bindings:
            Case.of(new SimpleBooleanProperty(), false, true, p -> p.not(), (p, v) -> p.setValue(!v)),
            Case.of(new SimpleIntegerProperty(), 42, 47, p -> p.add(2), (p, v) -> p.setValue(v.intValue() - 2)),
            Case.of(new SimpleLongProperty(), 42L, 47L, p -> p.add(2), (p, v) -> p.setValue(v.longValue() - 2)),
            Case.of(new SimpleFloatProperty(), 0.5f, 1.0f, p -> p.add(2), (p, v) -> p.setValue(v.floatValue() - 2)),
            Case.of(new SimpleDoubleProperty(), 0.5, 1.0, p -> p.add(2), (p, v) -> p.setValue(v.doubleValue() - 2)),
            Case.of(new SimpleStringProperty(), "A!", "B!", p -> p.concat("!"), (p, v) -> p.setValue(v.substring(0, 1))),
            Case.of(
                new SimpleObjectProperty<>(),
                "A!",
                "B!",
                p -> Bindings.createObjectBinding(() -> (p.get() + "!").intern(), p),
                (p, v) -> p.setValue(v.substring(0, 1).intern()) // intern() used to make sure ObjectBinding equality check works for this test
            ),

            // cases for lazy bindings:
            Case.of(new SimpleObjectProperty<>(), 10, 12, p -> p.map(x -> x * 2), (p, v) -> p.setValue(v / 2))
        );

        return cases.stream()
            .map(c -> Arguments.of(new Action<>(c.observableValue), c.primaryValue, c.alternativeValue, c.valueSetter));
    }

    /*
     * Tests if the embedded ObservableValue correctly rejects null listener add/removals.
     */
    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldRejectNullListener(Action<T> action) {
        assertThrows(NullPointerException.class, () -> action.addListener((InvalidationListener)null));
        assertThrows(NullPointerException.class, () -> action.addListener((ChangeListener<T>)null));
        assertThrows(NullPointerException.class, () -> action.removeListener((InvalidationListener)null));
        assertThrows(NullPointerException.class, () -> action.removeListener((ChangeListener<T>)null));
    }

    /*
     * Tests if the embedded ObservableValue correctly ignores removal of non-existing listeners, regardless
     * of how many listeners are currently registered. As the same ObservableValue is used throughout the
     * test, also stresses the add/remove listener code.
     */
    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldIgnoreRemovingNonExistingListener(Action<T> action) {
        for (ListenerCounts counts : new Combinations(LISTENER_COUNTS)) {
            // set up a specific combination of invalidation and change listeners:
            action.setListenerCounts(counts);

            /*
             * Remove some listeners that were never added; nothing is expected, so only an
             * exception thrown here is a failure.
             */

            assertDoesNotThrow(() -> action.removeListener(obs -> {}));
            assertDoesNotThrow(() -> action.removeListener((obs, old, current) -> {}));
        }
    }

    /*
     * Tests if the embedded ObservableValue sends the correct Change and Invalidation events with various
     * combinations of invalidation and change listeners. As the same ObservableValue is used throughout the
     * test, also stresses the add/remove listener code.
     */
    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldSendCorrectEventsWithSeveralInvalidationAndChangeListeners(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        for (ListenerCounts counts : new Combinations(LISTENER_COUNTS)) {
            int invalidationListenerCount = counts.invalidationListeners;
            int changeListenerCount = counts.changeListeners;

            // set up a specific combination of invalidation and change listeners:
            action.setListenerCounts(counts);

            valueSetter.accept(value2);
            action.assertEvents(
                changeListenerCount == 0 ? null : "Change of %c from " + value1 + " to " + value2,
                invalidationListenerCount == 0 ? null : "Invalidation of %i"
            );

            if (changeListenerCount == 0) {
                valueSetter.accept(value1);
                action.assertNoEvents();  // no change listeners, and already invalid, expect nothing
            }

            valueSetter.accept(value2);
            action.assertNoEvents();

            assertEquals(value2, action.getValue());
            action.assertNoEvents();

            valueSetter.accept(value2);
            action.assertNoEvents();

            valueSetter.accept(value1);
            action.assertEvents(
                changeListenerCount == 0 ? null : "Change of %c from " + value2 + " to " + value1,
                invalidationListenerCount == 0 ? null : "Invalidation of %i"
            );
        }
    }

    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldSendCorrectEventsWithSeveralInvalidationAndChangeListenersThatThrowExceptions(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        UncaughtExceptionHandler oldExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();
        AtomicInteger exceptions = new AtomicInteger();

        try {
            // Temporarily replace exception handler in order to check for thrown exceptions:
            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> exceptions.addAndGet(1));

            for (ListenerCounts counts : new Combinations(LISTENER_COUNTS)) {
                int invalidationListenerCount = counts.invalidationListeners;
                int changeListenerCount = counts.changeListeners;

                exceptions.set(0);

                // set up a specific combination of invalidation and change listeners:
                action.setThrowingListenerCounts(counts);

                assertEquals(0, exceptions.getAndSet(0));

                valueSetter.accept(value2);
                action.assertEvents(
                    changeListenerCount == 0 ? null : "Change of %c from " + value1 + " to " + value2,
                    invalidationListenerCount == 0 ? null : "Invalidation of %i"
                );
                assertEquals(changeListenerCount + invalidationListenerCount, exceptions.getAndSet(0));

                if (changeListenerCount == 0) {
                    valueSetter.accept(value1);
                    action.assertNoEvents();  // no change listeners, and already invalid, expect nothing
                    assertEquals(0, exceptions.getAndSet(0));
                }

                valueSetter.accept(value2);
                action.assertNoEvents();
                assertEquals(0, exceptions.getAndSet(0));

                assertEquals(value2, action.getValue());
                action.assertNoEvents();
                assertEquals(0, exceptions.getAndSet(0));

                valueSetter.accept(value2);
                action.assertNoEvents();
                assertEquals(0, exceptions.getAndSet(0));

                valueSetter.accept(value1);
                action.assertEvents(
                    changeListenerCount == 0 ? null : "Change of %c from " + value2 + " to " + value1,
                    invalidationListenerCount == 0 ? null : "Invalidation of %i"
                );
                assertEquals(changeListenerCount + invalidationListenerCount, exceptions.getAndSet(0));
            }
        }
        finally {
            Thread.currentThread().setUncaughtExceptionHandler(oldExceptionHandler);
        }
    }

    /*
     * Tests if the embedded ObservableValue sends sensible change events when a nested change occurs.
     */
    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldSendCorrectNestedEvents(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        List<Change> changes = new ArrayList<>();

        /*
         * Create three listeners, with the "middle" one modifying the value back to value1.
         */

        action.addListener((obs, old, current) -> changes.add(new Change("A", old, current)));
        action.addListener((obs, old, current) -> {
            changes.add(new Change("B", old, current));

            if (current.equals(value2)) {
                valueSetter.accept(value1);
            }
        });
        action.addListener((obs, old, current) -> changes.add(new Change("C", old, current)));

        /*
         * Start test:
         */

        valueSetter.accept(value2);

        assertConsistentChangeSequence(changes, value1, value1, Set.of(value1, value2));
    }

    /*
     * Tests if the embedded ObservableValue sends sensible change events when a nested change occurs
     * when there is only one listener.
     */
    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldSendCorrectNestedEventsWithOneListener(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        List<Change> changes = new ArrayList<>();

        /*
         * Create one listener, which modifies the value back to value1.
         */

        action.addListener((obs, old, current) -> {
            changes.add(new Change("B", old, current));

            if (current.equals(value2)) {
                valueSetter.accept(value1);
            }
        });

        /*
         * Start test:
         */

        valueSetter.accept(value2);

        assertConsistentChangeSequence(changes, value1, value2, Set.of(value1, value2));
    }

    /*
     * Tests if the embedded ObservableValue sends sensible change events when a nested change occurs
     * and the first listener was removed.
     */
    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldSendCorrectNestedEventsWhenFirstListenerRemoved(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        List<Change> changes = new ArrayList<>();

        /*
         * Create three listeners, with the "middle" one removing the first listener and modifying the
         * value back to value1.
         */

        ChangeListener<? super T> firstListener = (obs, old, current) -> changes.add(new Change("A", old, current));

        action.addListener(firstListener);
        action.addListener((obs, old, current) -> {
            changes.add(new Change("B", old, current));

            if (Objects.equals(current, value2)) {
                action.removeListener(firstListener);
                valueSetter.accept(value1);
            }
        });
        action.addListener((obs, old, current) -> changes.add(new Change("C", old, current)));

        /*
         * Start test:
         */

        valueSetter.accept(value2);

        assertConsistentChangeSequence(changes, value1, value1, Set.of(value1, value2));
    }

    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldOnlyNotifyNonRemovedChangeListeners(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        AtomicInteger calls = new AtomicInteger();

        ChangeListener<T> changeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends T> obs, T old, T current) {
                calls.addAndGet(1);

                action.removeListener(this);
                action.removeListener(this);
                action.addListener(this);
            }
        };

        for (int i = 0; i < 100; i++) {
            action.addListener(changeListener);
        }

        assertCalls(
            i -> valueSetter.accept(action.getValue().equals(value1) ? value2 : value1),
            calls,
            new int[] {50, 25, 13, 6, 3, 2, 1, 1, 1}  // won't reach 0 as a new listener is added each time
        );
    }

    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldOnlyNotifyNonRemovedChangeListeners_2(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        AtomicInteger calls = new AtomicInteger();

        ChangeListener<T> changeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends T> obs, T old, T current) {
                if (calls.getAndAdd(1) % 2 == 0) {
                    action.removeListener(this);
                }
            }
        };

        for (int i = 0; i < 100; i++) {
            action.addListener(changeListener);
        }

        assertCalls(
            i -> valueSetter.accept(action.getValue().equals(value1) ? value2 : value1),
            calls,
            new int[] {100, 50, 25, 12, 6, 3, 1, 0}
        );
    }

    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldOnlyNotifyNonRemovedChangeListeners_3(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        AtomicInteger calls = new AtomicInteger();

        ChangeListener<T> changeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends T> obs, T old, T current) {
                if (calls.getAndAdd(1) == 0) {
                    action.removeListener(this);
                    action.removeListener(this);
                    action.removeListener(this);
                }
            }
        };

        for (int i = 0; i < 12; i++) {
            action.addListener(changeListener);
        }

        assertCalls(
            i -> valueSetter.accept(action.getValue().equals(value1) ? value2 : value1),
            calls,
            new int[] {10, 7, 4, 1, 0}
        );
    }

    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldOnlyNotifyNonRemovedInvalidationListeners(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        AtomicInteger calls = new AtomicInteger();

        InvalidationListener invalidationListener = new InvalidationListener() {
            @Override
            public void invalidated(Observable obs) {
                calls.addAndGet(1);

                action.removeListener(this);
                action.removeListener(this);
                action.addListener(this);
            }
        };

        for (int i = 0; i < 100; i++) {
            action.addListener(invalidationListener);
        }

        assertCalls(
            i -> valueSetter.accept(action.getValue().equals(value1) ? value2 : value1),
            calls,
            new int[] {50, 25, 13, 6, 3, 2, 1, 1, 1}  // won't reach 0 as a new listener is added each time
        );
    }

    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldOnlyNotifyNonRemovedInvalidationListeners_2(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        AtomicInteger calls = new AtomicInteger();

        InvalidationListener invalidationListener = new InvalidationListener() {
            @Override
            public void invalidated(Observable obs) {
                if (calls.getAndAdd(1) % 2 == 0) {
                    action.removeListener(this);
                }
            }
        };

        for (int i = 0; i < 100; i++) {
            action.addListener(invalidationListener);
        }

        assertCalls(
            i -> valueSetter.accept(action.getValue().equals(value1) ? value2 : value1),
            calls,
            new int[] {100, 50, 25, 12, 6, 3, 1, 0}
        );
    }

    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldOnlyNotifyNonRemovedInvalidationListeners_3(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        AtomicInteger calls = new AtomicInteger();
        InvalidationListener listener = new InvalidationListener() {
            @Override
            public void invalidated(Observable obs) {
                if (calls.getAndAdd(1) == 0) {
                    action.removeListener(this);
                    action.removeListener(this);
                    action.removeListener(this);
                }
            }
        };

        for (int i = 0; i < 12; i++) {
            action.addListener(listener);
        }

        assertCalls(
            i -> valueSetter.accept(action.getValue().equals(value1) ? value2 : value1),
            calls,
            new int[] {10, 7, 4, 1, 0}
        );
    }

    private static void assertCalls(Consumer<Integer> step, AtomicInteger calls, int... expectedCalls) {
        for (int i = 0; i < expectedCalls.length; i++) {
            step.accept(i);
            assertEquals(expectedCalls[i], calls.getAndSet(0));
        }
    }

    static class Action<T> implements ObservableValue<T> {
        private final List<InvalidationListener> invalidationListeners = new ArrayList<>();
        private final List<ChangeListener<Object>> changeListeners = new ArrayList<>();
        private final List<String> eventRecords = new ArrayList<>();
        private final ObservableValue<T> observableValue;

        Action(ObservableValue<T> observableValue) {
            this.observableValue = observableValue;
        }

        @Override
        public void addListener(InvalidationListener listener) {
            observableValue.addListener(listener);
        }

        @Override
        public void addListener(ChangeListener<? super T> listener) {
            observableValue.addListener(listener);
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            observableValue.removeListener(listener);
        }

        @Override
        public void removeListener(ChangeListener<? super T> listener) {
            observableValue.removeListener(listener);
        }

        @Override
        public T getValue() {
            return observableValue.getValue();
        }

        /**
         * Sets the number of listeners to the given values. This function will add
         * more listeners if the number of listeners is lower than the given value, and
         * will remove listeners if the number of listeners is higher than the given value.
         * The end result is that the exact number of listeners given is present on the
         * {@link ObservableValue} currently under test.
         *
         * @param counts the number of invalidation and change listeners, cannot be {@code null}
         */
        void setListenerCounts(ListenerCounts counts) {
            int invalidationListenerCount = counts.invalidationListeners;
            int changeListenerCount = counts.changeListeners;

            for (int i = invalidationListeners.size() - 1; i >= invalidationListenerCount; --i) {
                InvalidationListener invalidationListener = invalidationListeners.get(i);

                invalidationListeners.remove(invalidationListener);
                observableValue.removeListener(invalidationListener);
            }

            for (int i = changeListeners.size() - 1; i >= changeListenerCount; --i) {
                ChangeListener<Object> changeListener = changeListeners.get(i);

                changeListeners.remove(changeListener);
                observableValue.removeListener(changeListener);
            }

            for (int i = invalidationListeners.size(); i < invalidationListenerCount; i++) {
                int j = i;

                InvalidationListener invalidationListener = obs -> eventRecords.add("Invalidation of " + j);

                invalidationListeners.add(invalidationListener);
                observableValue.addListener(invalidationListener);
            }

            for (int i = changeListeners.size(); i < changeListenerCount; i++) {
                int j = i;

                ChangeListener<Object> changeListener = (obs, old, current) ->
                    eventRecords.add("Change of " + j + " from " + old + " to " + current);

                changeListeners.add(changeListener);
                observableValue.addListener(changeListener);
            }
        }

        /**
         * Sets the number of throwing listeners to the given values. This function will add
         * more listeners if the number of listeners is lower than the given value, and
         * will remove listeners if the number of listeners is higher than the given value.
         * The end result is that the exact number of listeners given is present on the
         * {@link ObservableValue} currently under test.
         *
         * @param counts the number of invalidation and change listeners, cannot be {@code null}
         */
        void setThrowingListenerCounts(ListenerCounts counts) {
            int invalidationListenerCount = counts.invalidationListeners;
            int changeListenerCount = counts.changeListeners;

            for (int i = invalidationListeners.size() - 1; i >= invalidationListenerCount; --i) {
                InvalidationListener invalidationListener = invalidationListeners.get(i);

                invalidationListeners.remove(invalidationListener);
                observableValue.removeListener(invalidationListener);
            }

            for (int i = changeListeners.size() - 1; i >= changeListenerCount; --i) {
                ChangeListener<Object> changeListener = changeListeners.get(i);

                changeListeners.remove(changeListener);
                observableValue.removeListener(changeListener);
            }

            for (int i = invalidationListeners.size(); i < invalidationListenerCount; i++) {
                int j = i;

                InvalidationListener invalidationListener = obs -> {
                    eventRecords.add("Invalidation of " + j);
                    throw new RuntimeException("this listener throws an exception");
                };

                invalidationListeners.add(invalidationListener);
                observableValue.addListener(invalidationListener);
            }

            for (int i = changeListeners.size(); i < changeListenerCount; i++) {
                int j = i;

                ChangeListener<Object> changeListener = (obs, old, current) -> {
                    eventRecords.add("Change of " + j + " from " + old + " to " + current);
                    throw new RuntimeException("this listener throws an exception");
                };

                changeListeners.add(changeListener);
                observableValue.addListener(changeListener);
            }
        }

        /**
         * Asserts that no events occurred at all since the last
         * check.
         */
        void assertNoEvents() {
            assertEvents();  // called without any expected templates
        }

        /**
         * Asserts that the given events occurred. If no events are given, then
         * checks if no events occurred. The events are stored as strings and follow
         * the formats:
         *
         * <ul>
         * <li>{@code Change of (property) from (old value) to (new value)}</li>
         * <li>{@code Invalidation of (property)}</li>
         * </ul>
         *
         * <p>The place holders {@code %c} and {@code %i} indicate the property
         * involved, so to verify that <b>all</b> change listeners changed from 2 to 5, pass
         * the template: {@code Change of %c from 2 to 5}
         *
         * @param expectedTemplates expected event templates, cannot be {@code null} but can be empty
         */
        void assertEvents(String... expectedTemplates) {
            for (String expectedTemplate : expectedTemplates) {
                if (expectedTemplate == null) {
                    continue;
                }

                if (expectedTemplate.contains("%c")) {
                    if (changeListeners.isEmpty()) {
                        fail("Expected \"" + expectedTemplate + "\" to match at least once, but it didn't for: " + this);
                    }

                    for (int i = 0; i < changeListeners.size(); i++) {
                        String expected = expectedTemplate.replaceAll("%c", "" + i);
                        assertTrue(eventRecords.remove(expected), () -> "Expected \"" + expected + "\" but found none for: " + this);
                    }
                }
                else if (expectedTemplate.contains("%i")) {
                    if (invalidationListeners.isEmpty()) {
                        fail("Expected \"" + expectedTemplate + "\" to match at least once, but it didn't for: " + this);
                    }

                    for (int i = 0; i < invalidationListeners.size(); i++) {
                        String expected = expectedTemplate.replaceAll("%i", "" + i);
                        assertTrue(eventRecords.remove(expected), () -> "Expected \"" + expected + "\" but found none for: " + this);
                    }
                }
            }

            if (!eventRecords.isEmpty()) {
                fail("Did not expect: " + eventRecords + " for: " + this);
            }
        }

        @Override
        public String toString() {
            return observableValue + "[il=" + invalidationListeners.size() + ", cl=" + changeListeners.size() + "]";
        }
    }

    /**
     * Defines a test case for a specific property or binding type. To make a valid
     * test case there are two different valid values needed (to trigger a change)
     * and a way to change the value of the property or binding involved. For properties
     * this is a straight-forward setter call, while for bindings this involves creating
     * a helper base property that can be modified to affect the binding's value.
     *
     * @param <P> the property or binding type
     * @param <T> the value type it can hold
     */
    static class Case<P extends Property<T>, T> {
        final T primaryValue;
        final T alternativeValue;
        final Consumer<T> valueSetter;
        final ObservableValue<? super T> observableValue;

        /**
         * Creates a new test case.
         *
         * @param property a (base) property to hold values, for property tests this is the property tested against;
         *   for bindings, this is the property bound against and manipulated to change the binding value
         * @param primaryValue a valid value for the property
         * @param alternativeValue an alternative valid value for the property
         * @param creator creates the observable value to manipulate; this will be the same as the property for
         *   property tests, for bindings it will be a derived value from the base property given
         * @param valueSetter a function that changes the value of the property; this is just the setter for
         *   property tests, while for bindings it changes the base property in such a way that it undoes the
         *   effect of the binding (ie. if the binding added 2 to an integer property, the setter must subtract 2)
         */
        Case(P property, T primaryValue, T alternativeValue, Function<P, ObservableValue<? super T>> creator, BiConsumer<P, T> valueSetter) {
            this.primaryValue = primaryValue;
            this.alternativeValue = alternativeValue;
            this.valueSetter = (T v) -> valueSetter.accept(property, v);
            this.observableValue = creator.apply(property);

            this.valueSetter.accept(primaryValue);
        }

        /*
         * Static convenience method to create a binding test case:
         */
        static <P extends Property<T>, T> Case<P, T> of(P property, T primaryValue, T alternativeValue, Function<P, ObservableValue<? super T>> modifier, BiConsumer<P, T> valueSetter) {
            return new Case<>(property, primaryValue, alternativeValue, modifier, valueSetter);
        }

        /*
         * Static convenience method to create a property test case:
         */
        static <P extends Property<T>, T> Case<P, T> of(P property, T primaryValue, T alternativeValue) {
            return new Case<>(property, primaryValue, alternativeValue, p -> p, (p, v) -> p.setValue(v));
        }
    }

    /**
     * Takes a list of values and creates an iterator of pairs of those values. The iterator
     * does not only return all possible pair combinations, but also different transitions between
     * two pairs. Effectively, given x values it returns x^3 combinations.
     */
    record Combinations(int[] values) implements Iterable<ListenerCounts> {
        @Override
        public Iterator<ListenerCounts> iterator() {
            return new Combinator(values);
        }
    }

    private static class Combinator implements Iterator<ListenerCounts> {
        private final int[] values;
        private final int m;

        private int s;
        private int x;
        private int y;

        public Combinator(int[] values) {
            this.values = values;
            this.m = values.length;
        }

        @Override
        public boolean hasNext() {
            return y < m;
        }

        @Override
        public ListenerCounts next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            ListenerCounts next = new ListenerCounts(values[x], values[(y + s) % m]);

            if (++s == m) {
                s = 0;

                if (++x == m) {
                    x = 0;
                    y++;
                }
            }

            return next;
        }
    }

    record ListenerCounts(int invalidationListeners, int changeListeners) {}

    /**
     * A record of a change event received by a specific listener.
     *
     * @param identifier a listener identifier
     * @param old the old (previous) value received
     * @param current the current value (new value) received
     */
    record Change(String identifier, Object old, Object current) {
        @Override
        public String toString() {
            return "[" + identifier + ": Changed from " + old + " to " + current + "]";
        }
    }

    /**
     * Given a list of changes that occurred, verifies that these changes follow the following
     * rules:
     *
     * <ul>
     * <li>Each change changes the value to a different value (old value != new value)</li>
     * <li>Each change has an old value equal to the previous' change new value</li>
     * <li>Each change changes to one of the expected valid input values given</li>
     * </ul>
     *
     * @param changes a list of changes, cannot be {@code null}
     * @param expectedFirstValue first value the property held
     * @param expectedLastValue last value the property should hold
     * @param inputValidValues allowed valid values
     */
    private static void assertConsistentChangeSequence(List<Change> changes, Object expectedFirstValue, Object expectedLastValue, Set<Object> inputValidValues) {
        Set<Object> validValues = new HashSet<>(inputValidValues);  // convert to regular set as Set#of is being obnoxious about calling contains(null)

        // loop over all unique listeners (given by identifier):
        for (String identifier : changes.stream().map(Change::identifier).distinct().toList()) {
            // filter change list for the specific listener being checked:
            List<Change> filtered = changes.stream().filter(c -> c.identifier().equals(identifier)).toList();

            // ensure they are actual changes, not same values:
            for (Change c : filtered) {
                assertNotEquals(c.old, c.current, c + " was not a change!");
            }

            Change previous = null;

            // ensure previous new value is next old value:
            for (Change c : filtered) {
                // Checks if values make sense at all:
                assertTrue(validValues.contains(c.old), c + " has an unexpected old value; valid values are " + validValues);
                assertTrue(validValues.contains(c.current), c + " has an unexpected current value; valid values are " + validValues);

                if (previous != null) {
                    assertEquals(c.old, previous.current, previous + " was followed by " + c + " with incorrect old value");
                }

                previous = c;
            }

            // ensure old value of first change matches expected initial value:
            changes.stream()
                .findFirst()
                .ifPresent(c -> assertEquals(c.old, expectedFirstValue));

            // ensure new value of last change matches expected last value:
            changes.stream()
                .reduce((first, second) -> second)
                .ifPresent(c -> assertEquals(c.current, expectedLastValue));

            // ensure there were changes if the first and last value were different:
            if (!Objects.equals(expectedFirstValue, expectedLastValue) && changes.isEmpty()) {
                fail("Records for " + identifier + " did not contain any changes, but did expect a change from " + expectedFirstValue + " to " + expectedLastValue);
            }
        }
    }
}