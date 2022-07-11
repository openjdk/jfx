package test.javafx.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javafx.beans.InvalidationListener;
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

    /*
     * Tests if the embedded ObservableValue sends sensible change events when a nested change occurs.
     */
    @ParameterizedTest
    @MethodSource("inputs")
    <T> void shouldSendCorrectNestedEvents(Action<T> action, T value1, T value2, Consumer<T> valueSetter) {
        List<String> records = new ArrayList<>();

        /*
         * Create three listeners, with the "middle" one modifying the value back to value1.
         */

        action.addListener((obs, old, current) -> records.add("Change of 0 from " + old + " to " + current));
        action.addListener((obs, old, current) -> {
            records.add("Change of 1 from " + old + " to " + current);
            if(current.equals(value2)) {
                valueSetter.accept(value1);
            }
        });
        action.addListener((obs, old, current) -> records.add("Change of 2 from " + old + " to " + current));

        /*
         * Start test:
         */

        valueSetter.accept(value2);

        /*
         * Verify the current implementation specific result (there are more combinations that could be considered
         * correct, but the test case simplifies this for now):
         */

        assertEquals(
            List.of(
                "Change of 0 from " + value1 + " to " + value2,
                "Change of 1 from " + value1 + " to " + value2,
                "Change of 2 from " + value1 + " to " + value2,
                "Change of 0 from " + value2 + " to " + value1,
                "Change of 1 from " + value2 + " to " + value1,
                "Change of 2 from " + value2 + " to " + value1
            ),
            records
        );
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
}
