/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.binding.ListenerListBase;
import com.sun.javafx.binding.ListenerManagerBase;
import com.sun.javafx.binding.Logging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractListenerManagerTest {
    private final List<String> notifications = new ArrayList<>();

    private SimpleObservableValue<String> ov;
    private ListenerManagerBase<String, SimpleObservableValue<String>> helper;

    protected abstract SimpleObservableValue<String> getTestObservableValue();
    protected abstract ListenerManagerBase<String, SimpleObservableValue<String>> getListenerManager();

    @BeforeEach
    void beforeEach() {
        this.ov = getTestObservableValue();
        this.helper = getListenerManager();
    }

    @Test
    void shouldNotifyChangeListeners() {
        ChangeListener<String> cl1 = (_, o, n) -> notifications.add("CL1: " + o + " -> " + n);
        ChangeListener<String> cl2 = (_, o, n) -> notifications.add("CL2: " + o + " -> " + n);

        ov.setValue("A");
        ov.fireValueChanged();

        assertEquals(List.of(), notifications);  // expect nothing, as there are no listeners

        helper.addListener(ov, cl1);

        assertNotNull(ov.data);

        ov.setValue("B");
        ov.fireValueChanged();

        assertEquals(List.of("CL1: A -> B"), notifications);

        helper.addListener(ov, cl2);

        notifications.clear();

        ov.setValue("C");
        ov.fireValueChanged();

        assertEquals(List.of("CL1: B -> C", "CL2: B -> C"), notifications);

        helper.removeListener(ov, cl1);

        notifications.clear();

        ov.setValue("D");
        ov.fireValueChanged();

        assertEquals(List.of("CL2: C -> D"), notifications);

        notifications.clear();

        ov.setValue("E");
        ov.fireValueChanged();

        assertEquals(List.of("CL2: D -> E"), notifications);

        helper.removeListener(ov, cl2);

        notifications.clear();

        ov.setValue("F");
        ov.fireValueChanged();

        assertEquals(List.of(), notifications);
    }

    @Test
    void shouldNotMixUpDualPurposeListeners() {
        DualPurposeListener listener1 = new DualPurposeListener("1");
        InvalidationListener listener2 = _ -> notifications.add("2: invalidated");
        ChangeListener<String> listener3 = (_, o, n) -> notifications.add("3: " + o + " -> " + n);

        helper.addListener(ov, (ChangeListener<String>)listener1);
        helper.addListener(ov, listener2);
        helper.addListener(ov, listener3);

        helper.removeListener(ov, (InvalidationListener)listener1);  // should have no effect

        notifications.clear();

        ov.setValue("A");
        ov.fireValueChanged();

        assertEquals(List.of("2: invalidated", "1: null -> A", "3: null -> A"), notifications);

        helper.removeListener(ov, (ChangeListener<String>)listener1);  // should have effect

        notifications.clear();

        ov.setValue("B");
        ov.fireValueChanged();

        assertEquals(List.of("2: invalidated", "3: A -> B"), notifications);
    }

    class DualPurposeListener implements InvalidationListener, ChangeListener<String> {
        private final String name;

        public DualPurposeListener(String name) {
            this.name = name;
        }

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            notifications.add(name + ": " + oldValue + " -> " + newValue);
        }

        @Override
        public void invalidated(Observable observable) {
            notifications.add(name + ": invalidated");
        }
    }

    /*
     * Adding a second change listener from a nested change listener callback must not cause the
     * listener list to be unlocked twice: once when the nested notification completes, and again
     * when the top level notification that caused the list to be created completes.
     */
    @Test
    void shouldNotUnlockListenerListTwiceWhenAddingChangeListenerFromNestedChangeListener() {
        ChangeListener<String> cl2 = (_, o, n) -> notifications.add("CL2: " + o + " -> " + n);

        helper.addListener(ov, (ChangeListener<String>) (_, o, n) -> {
            notifications.add("CL1: " + o + " -> " + n);

            if (n.equals("B")) {
                ov.setValue("C");
                ov.fireValueChanged();  // nested change
            }
            else if (n.equals("C")) {
                helper.addListener(ov, cl2);
            }
        });

        ov.setValue("B");
        ov.fireValueChanged();  // must not unlock the listener list twice

        /*
         * The value was changed to "C" by the nested change, and cl2 was added while the nested
         * notification was in progress. A further top level change must notify it:
         */

        notifications.clear();

        ov.setValue("D");
        ov.fireValueChanged();

        assertEquals(List.of("CL1: C -> D", "CL2: C -> D"), notifications);
    }

    /*
     * Removing a listener while a notification is in progress must not consolidate the listener list
     * (reassign the data field to a single listener or to null), because the size methods return the
     * locked-time shape rather than the live one. Consolidation is deferred until the outermost unlock.
     */
    @Test
    void shouldNotConsolidateListenerListWhileLocked() {
        class VetoingListener implements InvalidationListener {
            @Override
            public void invalidated(Observable observable) {
                // force the data field to become a list while the notification is in progress:
                helper.addListener(ov, (ChangeListener<String>) (_, _, _) -> {});

                Object dataWhileLocked = ov.data;

                // removing the only listener that existed before the notification would normally
                // collapse the list to a single listener (or to null); it must not do so while locked:
                helper.removeListener(ov, this);

                assertSame(dataWhileLocked, ov.data);
                assertTrue(ov.data instanceof ListenerListBase);
            }
        }

        helper.addListener(ov, new VetoingListener());

        ov.setValue("A");
        ov.fireValueChanged();
    }

    /*
     * Exercises a "nasty" listener (an invalidation listener or change listener, inserted at every
     * position within each seed mix) that, on the first notification, does one of several problematic
     * things. Whatever it does, all assertions must hold (correct changes, correct call counts,
     * correct data field shape, no exceptions).
     */
    @ParameterizedTest
    @MethodSource("nastyScenarios")
    void shouldKeepDataShapeConsistentWhenANastyListenerRuns(List<ListenerType> mix, ListenerType listenerType, NastyBehaviour behaviour) {
        List<Recorder> seededListeners = new ArrayList<>();
        List<Object> notificationOrder = new ArrayList<>();
        AtomicBoolean first = new AtomicBoolean();
        AtomicInteger nastyCalls = new AtomicInteger();

        Consumer<Object> nastyAction = self -> {
            nastyCalls.incrementAndGet();

            if (first.compareAndSet(false, true)) {
                switch (behaviour) {
                    case REMOVE_ALL_OTHERS -> seededListeners.forEach(this::removeListener);
                    case ADD_CL_AND_REMOVE -> {
                        ChangeListener<String> extra = (_, _, _) -> {};

                        helper.addListener(ov, extra);
                        helper.removeListener(ov, extra);
                    }
                    case ADD_CL_AND_KEEP -> helper.addListener(ov, (ChangeListener<String>) (_, _, _) -> {});
                    case REMOVE_SELF -> removeListener(self);
                    case REPLACE_SELF_WITH_CL -> {
                        removeListener(self);
                        helper.addListener(ov, (ChangeListener<String>) (_, _, _) -> {});
                    }
                    case REMOVE_AND_RE_ADD_SELF -> {
                        removeListener(self);
                        addListener(self);
                    }
                    case REPLACE_SELF_WITH_CL_AND_VETO -> {
                        removeListener(self);
                        helper.addListener(ov, (ChangeListener<String>) (_, _, _) -> {});

                        ov.setValue(null);  // revert to the value the observable held before the change
                        ov.fireValueChanged();
                    }
                    case ADD_CL_THEN_REMOVE_SELF_AND_VETO -> {
                        helper.addListener(ov, (ChangeListener<String>) (_, _, _) -> {});
                        removeListener(self);

                        ov.setValue(null);
                        ov.fireValueChanged();
                    }
                    case ADD_CL_AND_VETO -> {
                        helper.addListener(ov, (ChangeListener<String>) (_, _, _) -> {});

                        ov.setValue(null);
                        ov.fireValueChanged();
                    }
                    case NESTED_CHANGE -> {
                        ov.setValue("C");
                        ov.fireValueChanged();
                    }
                    case NESTED_CHANGE_TWICE -> {
                        ov.setValue("C");
                        ov.fireValueChanged();

                        ov.setValue("D");
                        ov.fireValueChanged();
                    }
                    case VETO -> {
                        ov.setValue(null);  // revert to the value the observable held before the change
                        ov.fireValueChanged();
                    }
                }
            }
        };

        Object nastyListener = switch (listenerType) {
            case INVALIDATION -> new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    nastyAction.accept(this);
                }
            };
            case CHANGE -> new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String old, String current) {
                    nastyAction.accept(this);
                }
            };
        };

        /*
         * Add listeners and track notification order:
         */

        int invalidationListenerCount = 0;

        for (ListenerType type : mix) {
            Object listener = switch (type) {
                case null -> addListener(nastyListener);
                default -> addTypedListener(type);
            };

            if (listener instanceof Recorder r) {
                seededListeners.add(r);
            }

            switch (listener) {
                case InvalidationListener il -> notificationOrder.add(invalidationListenerCount++, il);
                default -> notificationOrder.add(listener);
            }
        }

        /*
         * Initial state check:
         */

        Logging.setKeepLastLogRecord(true);
        Logging.getLogger().setErrorLogRecord(null);

        assertDataShapeMatchesListenerCount(mix.size());
        assertCacheConsistentWithShape(ov.data);

        /*
         * Setup completed, now trigger the listener with the nasty behavior:
         */

        ov.setValue("A");
        ov.fireValueChanged();

        assertDataShapeMatchesListenerCount(expectedRemaining(behaviour, mix.size()));
        assertCacheConsistentWithShape(ov.data);

        /*
         * Trigger a 2nd change:
         */

        ov.setValue("B");
        ov.fireValueChanged();

        assertDataShapeMatchesListenerCount(expectedRemaining(behaviour, mix.size()));
        assertCacheConsistentWithShape(ov.data);

        /*
         * Assert call counts, change consistency, and ensure no non-convergence happened:
         */

        for (Recorder recorder : seededListeners) {
            if (recorder instanceof RecordingChangeListener changeRecorder) {
                changeRecorder.assertConsistentSequence();
            }

            boolean beforeNastyListener = notificationOrder.indexOf(recorder) < notificationOrder.indexOf(nastyListener);

            assertEquals(
                expectedCallCount(behaviour, beforeNastyListener, recorder instanceof RecordingChangeListener),
                recorder.callCount(),
                "unexpected call count for a listener " + (beforeNastyListener ? "before" : "after") + " the insertion point"
            );
        }

        assertEquals(
            expectedNastyCallCount(behaviour),
            nastyCalls.get(),
            "unexpected call count for the nasty listener"
        );

        assertNull(Logging.getLogger().getErrorLogRecord(), "no non-convergence warning was expected");
    }

    /*
     * The number of calls a seed listener is expected to receive. Whether a listener is notified before
     * being removed (or re-notified for a nested change) depends on whether it comes before the "nasty"
     * listener in the notification order.
     */
    private static int expectedCallCount(NastyBehaviour behaviour, boolean beforeNastyListener, boolean changeListener) {
        return switch (behaviour) {
            case REMOVE_ALL_OTHERS -> beforeNastyListener ? 1 : 0;
            case NESTED_CHANGE -> beforeNastyListener ? 3 : 2;  // later listeners only received collapsed change
            case NESTED_CHANGE_TWICE -> beforeNastyListener ? 4 : 2;  // later listeners only received collapsed change
            // A veto aborts the first notification before reaching change listeners that have not been
            // notified yet; invalidation listeners are never aborted as they have no value:
            case VETO, REPLACE_SELF_WITH_CL_AND_VETO, ADD_CL_THEN_REMOVE_SELF_AND_VETO, ADD_CL_AND_VETO ->
                beforeNastyListener ? 3 : (changeListener ? 1 : 2);
            case ADD_CL_AND_REMOVE, ADD_CL_AND_KEEP, REMOVE_SELF, REPLACE_SELF_WITH_CL, REMOVE_AND_RE_ADD_SELF -> 2;
        };
    }

    /*
     * The number of calls the "nasty" listener itself is expected to receive. Behaviors that remove the
     * listener from within its own callback stop it from being called for the subsequent change.
     */
    private static int expectedNastyCallCount(NastyBehaviour behaviour) {
        return switch (behaviour) {
            case REMOVE_SELF, REPLACE_SELF_WITH_CL, REPLACE_SELF_WITH_CL_AND_VETO, ADD_CL_THEN_REMOVE_SELF_AND_VETO -> 1;
            case REMOVE_ALL_OTHERS, ADD_CL_AND_REMOVE, ADD_CL_AND_KEEP, REMOVE_AND_RE_ADD_SELF -> 2;
            case NESTED_CHANGE, VETO, ADD_CL_AND_VETO -> 3;
            case NESTED_CHANGE_TWICE -> 4;
        };
    }

    private static int expectedRemaining(NastyBehaviour behaviour, int total) {
        return switch (behaviour) {
            case REMOVE_ALL_OTHERS -> 1;
            case REMOVE_SELF -> total - 1;
            case ADD_CL_AND_KEEP, ADD_CL_AND_VETO -> total + 1;
            default -> total;
        };
    }

    private Recorder addTypedListener(ListenerType type) {
        return switch (type) {
            case INVALIDATION -> {
                RecordingInvalidationListener il = new RecordingInvalidationListener();

                helper.addListener(ov, il);

                yield il;
            }
            case CHANGE -> {
                RecordingChangeListener cl = new RecordingChangeListener();

                helper.addListener(ov, cl);

                yield cl;
            }
            default -> throw new IllegalArgumentException("listener type must not be null");
        };
    }

    @SuppressWarnings("unchecked")
    private void removeListener(Object listener) {
        switch (listener) {
            case InvalidationListener il -> helper.removeListener(ov, il);
            case ChangeListener<?> cl -> helper.removeListener(ov, (ChangeListener<String>) cl);
            default -> throw new IllegalArgumentException("expected listener to be an invalidation or change listener: " + listener);
        }
    }

    @SuppressWarnings("unchecked")
    private Object addListener(Object listener) {
        switch (listener) {
            case InvalidationListener il -> helper.addListener(ov, il);
            case ChangeListener<?> cl -> helper.addListener(ov, (ChangeListener<String>) cl);
            default -> throw new IllegalArgumentException("expected listener to be an invalidation or change listener: " + listener);
        }

        return listener;
    }

    private void assertDataShapeMatchesListenerCount(int count) {
        switch (count) {
            case 0 -> assertNull(ov.data);
            case 1 -> {
                assertNotNull(ov.data, "a single listener was expected");
                assertFalse(ov.data instanceof ListenerListBase, "a single listener should not be held in a list");
            }
            default -> assertTrue(ov.data instanceof ListenerListBase, "multiple listeners should be held in a list");
        }
    }

    /**
     * Overridden by OldValueCachingListenerManagerTest to check that the cached value
     * is consistent with the current data shape.
     *
     * @param data the data read
     */
    protected void assertCacheConsistentWithShape(Object data) {
    }

    interface Recorder {
        int callCount();
    }

    static class RecordingChangeListener implements ChangeListener<String>, Recorder {
        private final List<String[]> changes = new ArrayList<>();

        @Override
        public void changed(ObservableValue<? extends String> observable, String old, String current) {
            changes.add(new String[] {old, current});
        }

        @Override
        public int callCount() {
            return changes.size();
        }

        void assertConsistentSequence() {
            String previousNew = null;

            for (int i = 0; i < changes.size(); i++) {
                String[] change = changes.get(i);

                assertNotEquals(change[0], change[1], "a change should report different old and new values");

                if (i > 0) {
                    assertEquals(previousNew, change[0], "the old value should equal the previous reported new value");
                }

                previousNew = change[1];
            }
        }
    }

    static class RecordingInvalidationListener implements InvalidationListener, Recorder {
        private int calls;

        @Override
        public void invalidated(Observable observable) {
            calls++;
        }

        @Override
        public int callCount() {
            return calls;
        }
    }

    private enum ListenerType {INVALIDATION, CHANGE}

    private enum NastyBehaviour {
        REMOVE_ALL_OTHERS,
        REMOVE_SELF,  // not really nasty, this is pretty common
        REMOVE_AND_RE_ADD_SELF,
        REPLACE_SELF_WITH_CL,
        REPLACE_SELF_WITH_CL_AND_VETO,
        ADD_CL_AND_KEEP,
        ADD_CL_AND_REMOVE,
        ADD_CL_AND_VETO,
        ADD_CL_THEN_REMOVE_SELF_AND_VETO,
        NESTED_CHANGE,
        NESTED_CHANGE_TWICE,
        VETO  // common enough
    }

    static Stream<Arguments> nastyScenarios() {
        List<Arguments> scenarios = new ArrayList<>();

        for (List<ListenerType> mix : listenerMixLists()) {
            for (int i = 0; i <= mix.size(); i++) {
                List<ListenerType> withInsertionPoint = new ArrayList<>(mix);

                withInsertionPoint.add(i, null);

                for (ListenerType modifierType : ListenerType.values()) {
                    for (NastyBehaviour behaviour : NastyBehaviour.values()) {
                        scenarios.add(Arguments.of(withInsertionPoint, modifierType, behaviour));
                    }
                }
            }
        }

        return scenarios.stream();
    }

    static List<List<ListenerType>> listenerMixLists() {
        return List.of(
            List.<ListenerType>of(),
            List.of(ListenerType.INVALIDATION),
            List.of(ListenerType.CHANGE),
            List.of(ListenerType.INVALIDATION, ListenerType.INVALIDATION),
            List.of(ListenerType.INVALIDATION, ListenerType.CHANGE),
            List.of(ListenerType.CHANGE, ListenerType.INVALIDATION),
            List.of(ListenerType.CHANGE, ListenerType.CHANGE)
        );
    }

    /*
     * The shouldKeepPropertyValid tests ensure that if there was a change listener present
     * at the start of a notification that, regardless of the end state, the property will
     * be valid.
     */

    @Test
    void shouldKeepPropertyValidWhenExistingChangeListenerIsRemovedDuringNotification() {
        ObjectProperty<String> p = new SimpleObjectProperty<>("A");
        List<String> changes = new ArrayList<>();
        ChangeListener<String> cl = (_, old, val) -> changes.add("1: " + old + " -> " + val);

        AtomicBoolean replace = new AtomicBoolean(true);

        p.addListener(_ -> {
            changes.add("invalidated");

            if (replace.getAndSet(false)) {
                p.removeListener(cl);
            }
        });

        p.addListener(cl);
        p.set("B");
        p.set("C");

        assertEquals(List.of("invalidated", "invalidated"), changes);
    }

    @Test
    void shouldKeepPropertyValidWhenChangeListenerIsReplacedDuringNotification() {
        ObjectProperty<String> p = new SimpleObjectProperty<>("A");
        List<String> changes = new ArrayList<>();
        ChangeListener<String> cl1 = (_, old, val) -> changes.add("1: " + old + " -> " + val);
        ChangeListener<String> cl2 = (_, old, val) -> changes.add("2: " + old + " -> " + val);

        AtomicBoolean replace = new AtomicBoolean(true);

        p.addListener(_ -> {
            changes.add("invalidated");

            if (replace.getAndSet(false)) {
                p.removeListener(cl1);
                p.addListener(cl2);
            }
        });

        p.addListener(cl1);
        p.set("B");
        p.set("C");

        assertEquals(List.of("invalidated", "invalidated", "2: B -> C"), changes);
    }

    @Test
    void shouldKeepPropertyValidWhenNewChangeListenerIsAddedBeforeExistingOneIsRemovedDuringNotification() {
        ObjectProperty<String> p = new SimpleObjectProperty<>("A");
        List<String> changes = new ArrayList<>();
        ChangeListener<String> cl1 = (_, old, val) -> changes.add("1: " + old + " -> " + val);
        ChangeListener<String> cl2 = (_, old, val) -> changes.add("2: " + old + " -> " + val);

        AtomicBoolean replace = new AtomicBoolean(true);

        p.addListener(_ -> {
            changes.add("invalidated");

            if (replace.getAndSet(false)) {
                p.addListener(cl2);
                p.removeListener(cl1);
            }
        });

        p.addListener(cl1);
        p.set("B");
        p.set("C");

        assertEquals(List.of("invalidated", "invalidated", "2: B -> C"), changes);
    }
}
