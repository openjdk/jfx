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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

public class ObservableValueTest {
    private static final int[] PATTERN = new int[] {1, 2, 0, 50, 3, 7};

    /*
     * ObservableValue cases to test:
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
            Case.of(new SimpleObjectProperty<>(), "A!", "B!", p -> Bindings.createObjectBinding(() -> (p.get() + "!").intern(), p), (p, v) -> p.setValue(v.substring(0, 1).intern())),  // intern() used to make sure ObjectBinding equality check works for this test

            // cases for lazy bindings:
            Case.of(new SimpleObjectProperty<>(), 10, 12, p -> p.map(x -> x * 2), (p, v) -> p.setValue(v / 2))
        );

        return cases.stream().map(c -> Arguments.of(new Action<>(c.observableValue), c.primaryValue, c.alternativeValue, c.valueSetter));
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
        for(int[] counts : new Combinations(PATTERN)) {
            int invalidationListenerCount = counts[0];
            int changeListenerCount = counts[1];

            action.setListenerCounts(invalidationListenerCount, changeListenerCount);

            action.removeListener(obs -> {});
            action.removeListener((obs, old, current) -> {});
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
        for(int[] counts : new Combinations(PATTERN)) {
            int invalidationListenerCount = counts[0];
            int changeListenerCount = counts[1];

            action.setListenerCounts(invalidationListenerCount, changeListenerCount);

            valueSetter.accept(value2);
            action.assertEvents(
                changeListenerCount == 0 ? null : "Change of %c from " + value1 + " to " + value2,
                invalidationListenerCount == 0 ? null : "Invalidation of %i"
            );

            if(changeListenerCount == 0) {
                valueSetter.accept(value1);
                action.assertEvents();  // when there are no change listeners, setting a different value (while invalid) should not trigger any events
            }

            valueSetter.accept(value2);
            action.assertEvents();

            assertEquals(value2, action.getValue());
            action.assertEvents();

            valueSetter.accept(value2);
            action.assertEvents();

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
        AtomicInteger exceptions = new AtomicInteger();
        UncaughtExceptionHandler oldExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();

        try {
            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> exceptions.addAndGet(1));

            for(int[] counts : new Combinations(PATTERN)) {
                int invalidationListenerCount = counts[0];
                int changeListenerCount = counts[1];

                exceptions.set(0);

                action.setThrowingListenerCounts(invalidationListenerCount, changeListenerCount);

                assertEquals(0, exceptions.getAndSet(0));

                valueSetter.accept(value2);
                action.assertEvents(
                    changeListenerCount == 0 ? null : "Change of %c from " + value1 + " to " + value2,
                    invalidationListenerCount == 0 ? null : "Invalidation of %i"
                );
                assertEquals(changeListenerCount + invalidationListenerCount, exceptions.getAndSet(0));

                if(changeListenerCount == 0) {
                    valueSetter.accept(value1);
                    action.assertEvents();  // when there are no change listeners, setting a different value (while invalid) should not trigger any events
                    assertEquals(0, exceptions.getAndSet(0));
                }

                valueSetter.accept(value2);
                action.assertEvents();
                assertEquals(0, exceptions.getAndSet(0));

                assertEquals(value2, action.getValue());
                action.assertEvents();
                assertEquals(0, exceptions.getAndSet(0));

                valueSetter.accept(value2);
                action.assertEvents();
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
        List<Record> records = new ArrayList<>();

        /*
         * Create three listeners, with the "middle" one modifying the value back to value1.
         */

        action.addListener((obs, old, current) -> records.add(new Record.Change("A", old, current)));
        action.addListener((obs, old, current) -> {
            records.add(new Record.Change("B", old, current));
            if(current.equals(value2)) {
                valueSetter.accept(value1);
            }
        });
        action.addListener((obs, old, current) -> records.add(new Record.Change("C", old, current)));

        /*
         * Start test:
         */

        valueSetter.accept(value2);

        assertConsistentChangeSequence(records, value1, value1);
    }

    /*
     * Tests if the embedded ObservableValue sends sensible change events when a nested change occurs
     * when there is only one listener.
     */
    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldSendCorrectNestedEventsWithOneListener(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        List<Record> records = new ArrayList<>();

        /*
         * Create one listener, which modifies the value back to value1.
         */

        action.addListener((obs, old, current) -> {
            records.add(new Record.Change("B", old, current));
            if(current.equals(value2)) {
                valueSetter.accept(value1);
            }
        });

        /*
         * Start test:
         */

        valueSetter.accept(value2);

        assertConsistentChangeSequence(records, value1, value2);
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

        for(int i = 0; i < 100; i++) {
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

        for(int i = 0; i < 100; i++) {
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

        for(int i = 0; i < 12; i++) {
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

        for(int i = 0; i < 100; i++) {
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

        for(int i = 0; i < 100; i++) {
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

        for(int i = 0; i < 12; i++) {
            action.addListener(listener);
        }

        assertCalls(
            i -> valueSetter.accept(action.getValue().equals(value1) ? value2 : value1),
            calls,
            new int[] {10, 7, 4, 1, 0}
        );
    }

    private void assertCalls(Consumer<Integer> step, AtomicInteger calls, int... expectedCalls) {
        for(int i = 0; i < expectedCalls.length; i++) {
            step.accept(i);
            assertEquals(expectedCalls[i], calls.getAndSet(0));
        }
    }

    static class Action<T> implements ObservableValue<T> {
        private final List<InvalidationListener> invalidationListeners = new ArrayList<>();
        private final List<ChangeListener<Object>> changeListeners = new ArrayList<>();
        private final List<String> records = new ArrayList<>();
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

        void setListenerCounts(int invalidationListenerCount, int changeListenerCount) {
            for(int i = invalidationListeners.size() - 1; i >= invalidationListenerCount; --i) {
                InvalidationListener invalidationListener = invalidationListeners.get(i);

                invalidationListeners.remove(invalidationListener);
                observableValue.removeListener(invalidationListener);
            }

            for(int i = changeListeners.size() - 1; i >= changeListenerCount; --i) {
                ChangeListener<Object> changeListener = changeListeners.get(i);

                changeListeners.remove(changeListener);
                observableValue.removeListener(changeListener);
            }

            for(int i = invalidationListeners.size(); i < invalidationListenerCount; i++) {
                int j = i;

                InvalidationListener invalidationListener = obs -> {
                    records.add("Invalidation of " + j);
                };

                invalidationListeners.add(invalidationListener);
                observableValue.addListener(invalidationListener);
            }

            for(int i = changeListeners.size(); i < changeListenerCount; i++) {
                int j = i;

                ChangeListener<Object> changeListener = (obs, old, current) -> {
                    records.add("Change of " + j + " from " + old + " to " + current);
                };

                changeListeners.add(changeListener);
                observableValue.addListener(changeListener);
            }
        }

        void setThrowingListenerCounts(int invalidationListenerCount, int changeListenerCount) {
            for(int i = invalidationListeners.size() - 1; i >= invalidationListenerCount; --i) {
                InvalidationListener invalidationListener = invalidationListeners.get(i);

                invalidationListeners.remove(invalidationListener);
                observableValue.removeListener(invalidationListener);
            }

            for(int i = changeListeners.size() - 1; i >= changeListenerCount; --i) {
                ChangeListener<Object> changeListener = changeListeners.get(i);

                changeListeners.remove(changeListener);
                observableValue.removeListener(changeListener);
            }

            for(int i = invalidationListeners.size(); i < invalidationListenerCount; i++) {
                int j = i;

                InvalidationListener invalidationListener = obs -> {
                    records.add("Invalidation of " + j);
                    throw new RuntimeException("boo");
                };

                invalidationListeners.add(invalidationListener);
                observableValue.addListener(invalidationListener);
            }

            for(int i = changeListeners.size(); i < changeListenerCount; i++) {
                int j = i;

                ChangeListener<Object> changeListener = (obs, old, current) -> {
                    records.add("Change of " + j + " from " + old + " to " + current);
                    throw new RuntimeException("boo");
                };

                changeListeners.add(changeListener);
                observableValue.addListener(changeListener);
            }
        }

        void assertEvents(String... expectedTemplates) {
            for(String expectedTemplate : expectedTemplates) {
                if(expectedTemplate == null) {
                    continue;
                }

                if(expectedTemplate.contains("%c")) {
                    if(changeListeners.isEmpty()) {
                        fail("Expected \"" + expectedTemplate + "\" to match at least once, but it didn't for: " + this);
                    }

                    for(int i = 0; i < changeListeners.size(); i++) {
                        String expected = expectedTemplate.replaceAll("%c", "" + i);
                        assertTrue(records.remove(expected), () -> "Expected \"" + expected + "\" but found none for: " + this);
                    }
                }
                else if(expectedTemplate.contains("%i")) {
                    if(invalidationListeners.isEmpty()) {
                        fail("Expected \"" + expectedTemplate + "\" to match at least once, but it didn't for: " + this);
                    }

                    for(int i = 0; i < invalidationListeners.size(); i++) {
                        String expected = expectedTemplate.replaceAll("%i", "" + i);
                        assertTrue(records.remove(expected), () -> "Expected \"" + expected + "\" but found none for: " + this);
                    }
                }
            }

            if(!records.isEmpty()) {
                fail("Did not expect: " + records + " for: " + this);
            }
        }

        @Override
        public String toString() {
            return observableValue + "[il=" + invalidationListeners.size() + ", cl=" + changeListeners.size() + "]";
        }
    }

    static class Case<P extends Property<T>, T> {
        final T primaryValue;
        final T alternativeValue;
        final Consumer<T> valueSetter;
        final ObservableValue<? super T> observableValue;

        public Case(P property, T primaryValue, T alternativeValue, Function<P, ObservableValue<? super T>> modifier, BiConsumer<P, T> valueSetter) {
            this.primaryValue = primaryValue;
            this.alternativeValue = alternativeValue;
            this.valueSetter = (T v) -> valueSetter.accept(property, v);
            this.observableValue = modifier.apply(property);

            this.valueSetter.accept(primaryValue);
        }

        static <P extends Property<T>, T> Case<P, T> of(P property, T primaryValue, T alternativeValue, Function<P, ObservableValue<? super T>> modifier, BiConsumer<P, T> valueSetter) {
            return new Case<>(property, primaryValue, alternativeValue, modifier, valueSetter);
        }

        static <P extends Property<T>, T> Case<P, T> of(P property, T primaryValue, T alternativeValue) {
            return new Case<>(property, primaryValue, alternativeValue, p -> p, (p, v) -> p.setValue(v));
        }
    }

    /**
     * Takes a list of values and creates an iterator of pairs of those values. The iterator
     * does not only return all possible pair combinations, but also different transitions between
     * two pairs. Effectively, given x values it returns x^3 combinations.
     */
    private static class Combinations implements Iterable<int[]> {
        private final int[] values;

        public Combinations(int[] values) {
            this.values = values;
        }

        @Override
        public Iterator<int[]> iterator() {
            return new Combinator(values);
        }
    }

    private static class Combinator implements Iterator<int[]> {
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
        public int[] next() {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }

            int[] next = new int[] {values[x], values[(y + s) % m]};

            if(++s == m) {
                s = 0;

                if(++x == m) {
                    x = 0;
                    y++;
                }
            }

            return next;
        }
    }

    sealed interface Record {
        String identifier();

        record Added(String identifier) implements Record {}
        record Removed(String identifier) implements Record {}
        record Change(String identifier, Object old, Object current) implements Record {
            @Override
            public String toString() {
                return "[" + identifier + ": Changed from " + old + " to " + current + "]";
            }
        }
    }

    private static void assertConsistentChangeSequence(List<Record> records, Object expectedFirstValue, Object expectedLastValue) {
        for(String identifier : records.stream().map(Record::identifier).distinct().toList()) {
            List<Record> filtered = records.stream().filter(c -> c.identifier().equals(identifier)).toList();

            // ensure they are actual changes, not same values:
            for (Record record : filtered) {
                if (record instanceof Record.Change c) {
                    assertNotEquals(c.old, c.current, c + " was not a change!");
                }
            }

            Record previous = null;

            // ensure previous new value is next old value:
            for (Record record : filtered) {
                if (previous != null) {
                    if (record instanceof Record.Change c) {
                        if(previous instanceof Record.Change pc) {
                            assertEquals(c.old, pc.current, pc + " was followed by " + c + " with incorrect old value");
                        }
                    }
                }

                previous = record;
            }

            List<Record.Change> changesOnly = filtered.stream()
                .filter(Record.Change.class::isInstance)
                .map(Record.Change.class::cast)
                .toList();

            changesOnly.stream()
                .findFirst()
                .ifPresent(c -> c.old.equals(expectedFirstValue));

            changesOnly.stream()
                .reduce((first, second) -> second)
                .ifPresent(c -> c.current.equals(expectedLastValue));

            if(!Objects.equals(expectedFirstValue, expectedLastValue) && changesOnly.isEmpty()) {
                fail("Records for " + identifier + " did not contain any changes, but did expect a change from " + expectedFirstValue + " to " + expectedLastValue);
            }
        }
    }
}