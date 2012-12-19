/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.event;

import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.event.EventType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public final class EventHandlerManagerTest {
    private EventHandlerManager eventHandlerManager;

    @Before
    public void setUp() {
        eventHandlerManager = new EventHandlerManager(this);
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
                new EventCountingHandler<EmptyEvent>();
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
                new EventCountingHandler<EmptyEvent>();
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
                new EventCountingHandler<EmptyEvent>();
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
                new EventCountingHandler<Event>();
        final EventCountingHandler<ValueEvent> valueEventCounter =
                new EventCountingHandler<ValueEvent>();
        final EventCountingHandler<ValueEvent> valueAEventCounter =
                new EventCountingHandler<ValueEvent>();
        final EventCountingHandler<Event> valueBEventCounter =
                new EventCountingHandler<Event>();
        final EventCountingHandler<Event> emptyEventCounter =
                new EventCountingHandler<Event>();

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
                new EventCountingHandler<Event>();
        final EventCountingHandler<ValueEvent> valueEventCounter =
                new EventCountingHandler<ValueEvent>();
        final EventCountingHandler<ValueEvent> valueAEventCounter =
                new EventCountingHandler<ValueEvent>();
        final EventCountingHandler<Event> valueBEventCounter =
                new EventCountingHandler<Event>();
        final EventCountingHandler<Event> emptyEventCounter =
                new EventCountingHandler<Event>();

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
                new EventCountingHandler<Event>();
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<Event>();
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
                new EventCountingHandler<Event>();
        final EventConsumingHandler eventConsumingFilter =
                new EventConsumingHandler();
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<Event>();
        
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
                new EventHandler<Event>() {
                    @Override
                    public void handle(final Event event) {
                    }
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
                new EventHandler<Event>() {
                    @Override
                    public void handle(final Event event) {
                    }
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
                new EventHandler<Event>() {
                    @Override
                    public void handle(final Event event) {
                    }
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
                new EventHandler<Event>() {
                    @Override
                    public void handle(final Event event) {
                    }
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
                new EventHandler<Event>() {
                    @Override
                    public void handle(final Event event) {
                    }
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
