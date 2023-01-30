/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.event;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.event.EventHandlerManager;

import static org.junit.Assert.*;

import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.event.WeakEventHandler;
import javafx.event.WeakEventHandlerUtil;

public final class EventHandlerManagerTest {
    private EventHandlerManager eventHandlerManager;

    @Before
    public void setUp() {
        eventHandlerManager = new EventHandlerManager(this);
    }

    /**
     * JDK-8092352: Skip dispatch if there are no handlers/filters
     * sanity test: freshly instantiated empty EventHandlerManager returns
     * same instance of event
     */
    @Test
    public void testEmptyHandler() {
        assertDispatch(new ValueEvent(0), 0);
    }

    /**
     * JDK-8092352: Skip dispatch if there are no handlers/filters
     * Test cycle set/null singleton
     */
    @Test
    public void testShouldNotCopyEventWithoutSingletonHandler() {
        EventChangingHandler eventHandler = new EventChangingHandler(Operation.add(5));
        // add handler
        eventHandlerManager.setEventHandler(
                ValueEvent.VALUE_A,
                eventHandler);
        ValueEvent sent = new ValueEvent(0);
        assertDispatch(sent, 5);
        // remove handler
        eventHandlerManager.setEventHandler(ValueEvent.VALUE_A, null);
        assertDispatch(sent, 0);
    }

    /**
     * JDK-8092352: Skip dispatch if there are no handlers/filters
     * Test cycle add/remove handler
     */
    @Test
    public void testShouldNotCopyEventWithoutHandler() {
        EventChangingHandler eventHandler = new EventChangingHandler(Operation.add(5));
        // add handler
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_A,
                eventHandler);
        ValueEvent sent = new ValueEvent(0);
        assertDispatch(sent, 5);
        // remove handler
        eventHandlerManager.removeEventHandler(ValueEvent.VALUE_A, eventHandler);
        assertDispatch(sent, 0);
    }

    /**
     * JDK-8092352: Skip dispatch if there are no handlers/filters
     * Test cycle add/remove filter
     */
    @Test
    public void testShouldNotCopyEventWithoutFilter() {
        EventChangingHandler eventFilter = new EventChangingHandler(Operation.add(5));
        // add filter
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_A,
                eventFilter);
        ValueEvent sent = new ValueEvent(0);
        assertDispatch(sent, 5);
        // remove filter
        eventHandlerManager.removeEventFilter(ValueEvent.VALUE_A, eventFilter);
        assertDispatch(sent, 0);
    }

    /**
     * JDK-8092352: Skip dispatch if there are no handlers/filters
     * Test cycle add/clear weak handler
     */
    @Test
    public void testShouldNotCopyEventWeakHandlerCleared() {
        EventChangingHandler eventHandler = new EventChangingHandler(Operation.add(5));
        WeakEventHandler<ValueEvent> weakHandler = new WeakEventHandler<>(eventHandler);
        // add weak handler
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_A,
                weakHandler);
        ValueEvent sent = new ValueEvent(0);
        assertDispatch(sent, 5);
        // clear weak handler
        WeakEventHandlerUtil.clear(weakHandler);
        assertDispatch(sent, 0);
    }

    /**
     * JDK-8092352: Skip dispatch if there are no handlers/filters
     * Test cycle add/clear weak filter
     */
    @Test
    public void testShouldNotCopyEventWeakFilterCleared() {
        EventChangingHandler eventFilter = new EventChangingHandler(Operation.add(5));
        WeakEventHandler<ValueEvent> weakFilter = new WeakEventHandler<>(eventFilter);
        // add filter
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_A,
                weakFilter);

        ValueEvent sent = new ValueEvent(0);
        assertDispatch(sent, 5);
        // clear weak filter
        WeakEventHandlerUtil.clear(weakFilter);
        assertDispatch(sent, 0);
    }

    /**
     * Helper for JDK-8092352 testing: dispatches the given event and
     * asserts its value and identity. If the given expected value is the
     * same as the event's initial value, the received event is expected
     * to be the same instance as the sent.
     */
    private void assertDispatch(ValueEvent sent, int expected) {
        boolean same = sent.getValue() == expected;
        ValueEvent received = (ValueEvent)
                eventHandlerManager.dispatchEvent(sent, StubEventDispatchChain.EMPTY_CHAIN);
        String message = "value must be " + (same ? "unchanged " : "changed ");
        assertEquals(message, expected, received.getValue());
        if (same) {
            assertSame("received event", sent, received);
        } else {
            assertNotSame("received event", sent, received);
        }
    }

    @Test
    public void shouldForwardEventsToChain() {
        final EventDispatchChain eventDispatchChain =
                StubEventDispatchChain.EMPTY_CHAIN
                                      .append(eventHandlerManager)
                                      .append(new EventChangingDispatcher(
                                                      Operation.add(4),
                                                      Operation.div(3)));

        ValueEvent valueEvent;

        valueEvent = (ValueEvent) eventDispatchChain.dispatchEvent(
                                          new ValueEvent(2));
        Assert.assertEquals(2, valueEvent.getValue());

        valueEvent = (ValueEvent) eventDispatchChain.dispatchEvent(
                                          new ValueEvent(5));
        Assert.assertEquals(3, valueEvent.getValue());
    }

    @Test
    public void shouldCallCorrectSingletonHandlers() {
        eventHandlerManager.setEventHandler(
                ValueEvent.VALUE_A,
                new EventChangingHandler(Operation.add(5)));
        eventHandlerManager.setEventHandler(
                ValueEvent.VALUE_B,
                new EventChangingHandler(Operation.mul(3)));
        eventHandlerManager.setEventHandler(
                ValueEvent.VALUE_C,
                new EventChangingHandler(Operation.mul(7)));

        final EventCountingHandler<EmptyEvent> emptyEventCountingHandler =
                new EventCountingHandler<>();
        eventHandlerManager.setEventHandler(
                EmptyEvent.EMPTY,
                emptyEventCountingHandler);

        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_A, 11, 16);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_C, 3, 21);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_B, 6, 18);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_C, 5, 35);

        Assert.assertEquals(0, emptyEventCountingHandler.getEventCount());
        dispatchEmptyEvent(eventHandlerManager);
        Assert.assertEquals(1, emptyEventCountingHandler.getEventCount());
        dispatchEmptyEvent(eventHandlerManager);
        Assert.assertEquals(2, emptyEventCountingHandler.getEventCount());
    }

    @Test
    public void shouldAllowReplaceSingletonHandlers() {
        eventHandlerManager.setEventHandler(
                ValueEvent.VALUE_B,
                new EventChangingHandler(Operation.add(5)));
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_B, 3, 8);

        eventHandlerManager.setEventHandler(
                ValueEvent.VALUE_B,
                new EventChangingHandler(Operation.mul(3)));
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_B, 7, 21);

        eventHandlerManager.setEventHandler(
                ValueEvent.VALUE_B,
                null);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_B, 5, 5);
    }

    @Test
    public void shouldCallCorrectAddedHandlers() {
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_A,
                new EventChangingHandler(Operation.add(5)));
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_B,
                new EventChangingHandler(Operation.mul(3)));
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_C,
                new EventChangingHandler(Operation.mul(7)));
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_A,
                new EventChangingHandler(Operation.add(2)));
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_B,
                new EventChangingHandler(Operation.mul(4)));
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_C,
                new EventChangingHandler(Operation.mul(6)));

        final EventCountingHandler<EmptyEvent> emptyEventCountingHandler =
                new EventCountingHandler<>();
        eventHandlerManager.addEventHandler(
                EmptyEvent.EMPTY,
                emptyEventCountingHandler);

        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_A, 11, 18);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_C, 3, 126);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_B, 6, 72);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_C, 5, 210);

        Assert.assertEquals(0, emptyEventCountingHandler.getEventCount());
        dispatchEmptyEvent(eventHandlerManager);
        Assert.assertEquals(1, emptyEventCountingHandler.getEventCount());
        dispatchEmptyEvent(eventHandlerManager);
        Assert.assertEquals(2, emptyEventCountingHandler.getEventCount());
    }

    @Test
    public void shouldCallCorrectAddedFilters() {
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_A,
                new EventChangingHandler(Operation.add(5)));
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_B,
                new EventChangingHandler(Operation.mul(3)));
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_C,
                new EventChangingHandler(Operation.mul(7)));
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_A,
                new EventChangingHandler(Operation.add(2)));
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_B,
                new EventChangingHandler(Operation.mul(4)));
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_C,
                new EventChangingHandler(Operation.mul(6)));

        final EventCountingHandler<EmptyEvent> emptyEventCountingHandler =
                new EventCountingHandler<>();
        eventHandlerManager.addEventFilter(
                EmptyEvent.EMPTY,
                emptyEventCountingHandler);

        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_A, 11, 18);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_C, 3, 126);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_B, 6, 72);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_C, 5, 210);

        Assert.assertEquals(0, emptyEventCountingHandler.getEventCount());
        dispatchEmptyEvent(eventHandlerManager);
        Assert.assertEquals(1, emptyEventCountingHandler.getEventCount());
        dispatchEmptyEvent(eventHandlerManager);
        Assert.assertEquals(2, emptyEventCountingHandler.getEventCount());
    }

    @Test
    public void shouldAllowRemoveHandlersAndFilters() {
        final EventHandler<ValueEvent> handlerFilterToRemove =
                new EventChangingHandler(Operation.add(5));

        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_A,
                handlerFilterToRemove);
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_A,
                new EventChangingHandler(Operation.add(2)));
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_A,
                handlerFilterToRemove);

        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_A, 11, 23);
        eventHandlerManager.removeEventHandler(
                ValueEvent.VALUE_A, handlerFilterToRemove);
        eventHandlerManager.removeEventFilter(
                ValueEvent.VALUE_B, handlerFilterToRemove);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_A, 11, 18);
        eventHandlerManager.removeEventFilter(
                ValueEvent.VALUE_A, handlerFilterToRemove);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_A, 11, 13);
    }

    @Test
    public void shouldNotAddHandlerTwice() {
        final EventHandler<ValueEvent> eventHandler =
                new EventChangingHandler(Operation.mul(2));

        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_A,
                eventHandler);
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_B,
                eventHandler);
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_A,
                eventHandler);

        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_A, 1, 2);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_B, 1, 2);

        eventHandlerManager.removeEventHandler(
                ValueEvent.VALUE_A, eventHandler);

        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_A, 1, 1);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_B, 1, 2);
    }

    @Test
    public void shouldNotAddFilterTwice() {
        final EventHandler<ValueEvent> eventFilter =
                new EventChangingHandler(Operation.mul(2));

        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_A,
                eventFilter);
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_B,
                eventFilter);
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_A,
                eventFilter);

        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_A, 1, 2);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_B, 1, 2);

        eventHandlerManager.removeEventFilter(
                ValueEvent.VALUE_A, eventFilter);

        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_A, 1, 1);
        testValueEventDispatch(
                eventHandlerManager, ValueEvent.VALUE_B, 1, 2);
    }

    @Test
    public void shouldCallInCorrectOrder() {
        final EventDispatchChain eventDispatchChain =
                StubEventDispatchChain.EMPTY_CHAIN
                                      .append(eventHandlerManager)
                                      .append(new EventChangingDispatcher(
                                                      Operation.add(4),
                                                      Operation.div(3)));

        eventHandlerManager.setEventHandler(
                ValueEvent.VALUE_A,
                new EventChangingHandler(Operation.mul(2)));
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_A,
                new EventChangingHandler(Operation.add(5)));
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_A,
                new EventChangingHandler(Operation.div(7)));

        ValueEvent valueEvent;

        valueEvent = (ValueEvent) eventDispatchChain.dispatchEvent(
                                          new ValueEvent(35));
        Assert.assertEquals(16, valueEvent.getValue());
    }

    @Test
    public void shouldCallHandlersForSuperTypes() {
        final EventCountingHandler<Event> rootEventCounter =
                new EventCountingHandler<>();
        final EventCountingHandler<ValueEvent> valueEventCounter =
                new EventCountingHandler<>();
        final EventCountingHandler<ValueEvent> valueAEventCounter =
                new EventCountingHandler<>();
        final EventCountingHandler<Event> valueBEventCounter =
                new EventCountingHandler<>();
        final EventCountingHandler<Event> emptyEventCounter =
                new EventCountingHandler<>();

        eventHandlerManager.addEventHandler(
                EventType.ROOT, rootEventCounter);
        eventHandlerManager.addEventHandler(
                ValueEvent.ANY, valueEventCounter);
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_A, valueAEventCounter);
        eventHandlerManager.addEventHandler(
                ValueEvent.VALUE_B, valueBEventCounter);
        eventHandlerManager.addEventHandler(
                EmptyEvent.EMPTY, emptyEventCounter);

        dispatchEmptyEvent(eventHandlerManager);
        dispatchValueEvent(eventHandlerManager, ValueEvent.VALUE_A);
        dispatchValueEvent(eventHandlerManager, ValueEvent.VALUE_B);
        dispatchValueEvent(eventHandlerManager, ValueEvent.VALUE_C);
        dispatchEmptyEvent(eventHandlerManager);

        Assert.assertEquals(5, rootEventCounter.getEventCount());
        Assert.assertEquals(3, valueEventCounter.getEventCount());
        Assert.assertEquals(1, valueAEventCounter.getEventCount());
        Assert.assertEquals(1, valueBEventCounter.getEventCount());
        Assert.assertEquals(2, emptyEventCounter.getEventCount());
    }

    @Test
    public void shouldCallFiltersForSuperTypes() {
        final EventCountingHandler<Event> rootEventCounter =
                new EventCountingHandler<>();
        final EventCountingHandler<ValueEvent> valueEventCounter =
                new EventCountingHandler<>();
        final EventCountingHandler<ValueEvent> valueAEventCounter =
                new EventCountingHandler<>();
        final EventCountingHandler<Event> valueBEventCounter =
                new EventCountingHandler<>();
        final EventCountingHandler<Event> emptyEventCounter =
                new EventCountingHandler<>();

        eventHandlerManager.addEventFilter(
                EventType.ROOT, rootEventCounter);
        eventHandlerManager.addEventFilter(
                ValueEvent.ANY, valueEventCounter);
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_A, valueAEventCounter);
        eventHandlerManager.addEventFilter(
                ValueEvent.VALUE_B, valueBEventCounter);
        eventHandlerManager.addEventFilter(
                EmptyEvent.EMPTY, emptyEventCounter);

        dispatchEmptyEvent(eventHandlerManager);
        dispatchValueEvent(eventHandlerManager, ValueEvent.VALUE_A);
        dispatchValueEvent(eventHandlerManager, ValueEvent.VALUE_B);
        dispatchValueEvent(eventHandlerManager, ValueEvent.VALUE_C);
        dispatchEmptyEvent(eventHandlerManager);

        Assert.assertEquals(5, rootEventCounter.getEventCount());
        Assert.assertEquals(3, valueEventCounter.getEventCount());
        Assert.assertEquals(1, valueAEventCounter.getEventCount());
        Assert.assertEquals(1, valueBEventCounter.getEventCount());
        Assert.assertEquals(2, emptyEventCounter.getEventCount());
    }

    @Test
    public void eventConsumedInHandlerTest() {
        final EventCountingDispatcher eventCountingDispatcher =
                new EventCountingDispatcher();
        final EventDispatchChain eventDispatchChain =
                StubEventDispatchChain.EMPTY_CHAIN
                                      .append(eventHandlerManager)
                                      .append(eventCountingDispatcher);

        final EventCountingHandler<Event> eventCountingFilter =
                new EventCountingHandler<>();
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        final EventConsumingHandler eventConsumingHandler =
                new EventConsumingHandler();

        eventHandlerManager.addEventFilter(Event.ANY, eventCountingFilter);

        // add counting first, consuming second
        eventHandlerManager.addEventHandler(Event.ANY, eventCountingHandler);
        eventHandlerManager.addEventHandler(Event.ANY, eventConsumingHandler);

        Assert.assertNull(eventDispatchChain.dispatchEvent(new EmptyEvent()));
        Assert.assertEquals(1, eventCountingFilter.getEventCount());
        Assert.assertEquals(
                1, eventCountingDispatcher.getCapturingEventCount());
        Assert.assertEquals(1, eventCountingHandler.getEventCount());

        eventHandlerManager.removeEventHandler(
                Event.ANY, eventCountingHandler);
        eventHandlerManager.removeEventHandler(
                Event.ANY, eventConsumingHandler);

        // add consuming first, counting second
        eventHandlerManager.addEventHandler(Event.ANY, eventConsumingHandler);
        eventHandlerManager.addEventHandler(Event.ANY, eventCountingHandler);

        Assert.assertNull(eventDispatchChain.dispatchEvent(new EmptyEvent()));
        Assert.assertEquals(2, eventCountingFilter.getEventCount());
        Assert.assertEquals(
                2, eventCountingDispatcher.getCapturingEventCount());
        Assert.assertEquals(2, eventCountingHandler.getEventCount());
    }

    @Test
    public void eventConsumedInFilterTest() {
        final EventCountingDispatcher eventCountingDispatcher =
                new EventCountingDispatcher();
        final EventDispatchChain eventDispatchChain =
                StubEventDispatchChain.EMPTY_CHAIN
                                      .append(eventHandlerManager)
                                      .append(eventCountingDispatcher);

        final EventCountingHandler<Event> eventCountingFilter =
                new EventCountingHandler<>();
        final EventConsumingHandler eventConsumingFilter =
                new EventConsumingHandler();
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();

        eventHandlerManager.addEventHandler(Event.ANY, eventCountingHandler);

        // add counting first, consuming second
        eventHandlerManager.addEventFilter(Event.ANY, eventCountingFilter);
        eventHandlerManager.addEventFilter(Event.ANY, eventConsumingFilter);

        Assert.assertNull(eventDispatchChain.dispatchEvent(new EmptyEvent()));
        Assert.assertEquals(1, eventCountingFilter.getEventCount());
        Assert.assertEquals(
                0, eventCountingDispatcher.getCapturingEventCount());
        Assert.assertEquals(0, eventCountingHandler.getEventCount());

        eventHandlerManager.removeEventFilter(Event.ANY, eventCountingFilter);
        eventHandlerManager.removeEventFilter(Event.ANY, eventConsumingFilter);

        // add consuming first, counting second
        eventHandlerManager.addEventFilter(Event.ANY, eventConsumingFilter);
        eventHandlerManager.addEventFilter(Event.ANY, eventCountingFilter);

        Assert.assertNull(eventDispatchChain.dispatchEvent(new EmptyEvent()));
        Assert.assertEquals(2, eventCountingFilter.getEventCount());
        Assert.assertEquals(
                0, eventCountingDispatcher.getCapturingEventCount());
        Assert.assertEquals(0, eventCountingHandler.getEventCount());
    }

    @Test(expected=NullPointerException.class)
    public void addEventHandlerShouldThrowNPEForNullEventType() {
        eventHandlerManager.addEventHandler(
                null,
                event -> {
                });
    }

    @Test(expected=NullPointerException.class)
    public void addEventHandlerShouldThrowNPEForNullEventHandler() {
        eventHandlerManager.addEventHandler(Event.ANY, null);
    }

    @Test(expected=NullPointerException.class)
    public void removeEventHandlerShouldThrowNPEForNullEventType() {
        eventHandlerManager.removeEventHandler(
                null,
                event -> {
                });
    }

    @Test(expected=NullPointerException.class)
    public void removeEventHandlerShouldThrowNPEForNullEventHandler() {
        eventHandlerManager.removeEventHandler(Event.ANY, null);
    }

    @Test(expected=NullPointerException.class)
    public void addEventFilterShouldThrowNPEForNullEventType() {
        eventHandlerManager.addEventFilter(
                null,
                event -> {
                });
    }

    @Test(expected=NullPointerException.class)
    public void addEventFilterShouldThrowNPEForNullEventHandler() {
        eventHandlerManager.addEventFilter(Event.ANY, null);
    }

    @Test(expected=NullPointerException.class)
    public void removeEventFilterShouldThrowNPEForNullEventType() {
        eventHandlerManager.removeEventHandler(
                null,
                event -> {
                });
    }

    @Test(expected=NullPointerException.class)
    public void removeEventFilterShouldThrowNPEForNullEventHandler() {
        eventHandlerManager.removeEventHandler(Event.ANY, null);
    }

    @Test(expected=NullPointerException.class)
    public void setEventHandlerShouldThrowNPEForNullEventType() {
        eventHandlerManager.setEventHandler(
                null,
                event -> {
                });
    }

    private static void testValueEventDispatch(
            final EventDispatcher eventDispatcher,
            final EventType<ValueEvent> eventType,
            final int initialValue,
            final int calculatedValue) {
        final ValueEvent valueEvent =
                (ValueEvent) eventDispatcher.dispatchEvent(
                                     new ValueEvent(eventType, initialValue),
                                     StubEventDispatchChain.EMPTY_CHAIN);
        Assert.assertEquals(calculatedValue, valueEvent.getValue());
    }

    private static Event dispatchEmptyEvent(
            final EventDispatcher eventDispatcher) {
        return eventDispatcher.dispatchEvent(
                       new EmptyEvent(),
                       StubEventDispatchChain.EMPTY_CHAIN);
    }

    private static Event dispatchValueEvent(
            final EventDispatcher eventDispatcher,
            final EventType<ValueEvent> eventType) {
        return eventDispatcher.dispatchEvent(
                       new ValueEvent(eventType, 0),
                       StubEventDispatchChain.EMPTY_CHAIN);
    }
}
