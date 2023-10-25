/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.event.CompositeEventHandler;
import com.sun.javafx.event.CompositeEventHandlerShim;

import static javafx.event.EventHandlerPriority.DEFAULT;
import static org.junit.Assert.*;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventHandlerPriority;
import javafx.event.WeakEventHandler;
import javafx.event.WeakEventHandlerUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CompositeEventHandlerTest {
    private CompositeEventHandler<Event> compositeEventHandler;

    @Before
    public void setUp() {
        compositeEventHandler = new CompositeEventHandler<>();
    }

    /**
     * test state report after add/clear weak filter
     * Here we test that a garbage collected weak filter is actually
     * removed from the chain.
     */
    @Test
    public void testHasFilterWeakCleared() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        final WeakEventHandler<Event> weakEventHandler =
                new WeakEventHandler<>(eventCountingHandler);

        compositeEventHandler.addEventFilter(weakEventHandler, DEFAULT);
        assertFalse("must not have handler after adding filter", compositeEventHandler.hasHandler(DEFAULT));
        assertTrue("must have filter", compositeEventHandler.hasFilter(DEFAULT));
        WeakEventHandlerUtil.clear(weakEventHandler);
        assertFalse("must not have filter", compositeEventHandler.hasFilter(DEFAULT));
        assertFalse("must not have handler", compositeEventHandler.hasHandler(DEFAULT));
    }

    /**
     * test state report after add/clear weak handler
     */
    @Test
    public void testHasHandlerAddWeakClear() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        final WeakEventHandler<Event> weakEventHandler =
                new WeakEventHandler<>(eventCountingHandler);
        compositeEventHandler.addEventHandler(weakEventHandler, DEFAULT);
        assertTrue("sanity: really added?", CompositeEventHandlerShim.containsHandler(
                compositeEventHandler, weakEventHandler));
        assertFalse("must not have filter after adding handler", compositeEventHandler.hasFilter(DEFAULT));
        assertTrue("must have handler", compositeEventHandler.hasHandler(DEFAULT));
        WeakEventHandlerUtil.clear(weakEventHandler);
        assertFalse("must not have handler", compositeEventHandler.hasHandler(DEFAULT));
        assertFalse("must not have filter", compositeEventHandler.hasFilter(DEFAULT));
    }

    /**
     * test state report after add/remove weak filter
     * Here we test that the duplicated (against normal) implementation
     * behaves as expected.
     */
    @Test
    public void testHasFilterWeak() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        final WeakEventHandler<Event> weakEventHandler =
                new WeakEventHandler<>(eventCountingHandler);

        compositeEventHandler.addEventFilter(weakEventHandler, DEFAULT);
        assertFalse("must not have handler after adding filter", compositeEventHandler.hasHandler(DEFAULT));
        assertTrue("must have filter", compositeEventHandler.hasFilter(DEFAULT));
        compositeEventHandler.removeEventFilter(weakEventHandler);
        assertFalse("must not have filter", compositeEventHandler.hasFilter(DEFAULT));
        assertFalse("must not have handler", compositeEventHandler.hasHandler(DEFAULT));
    }

    /**
     * test state report after add/remove weak handler
     */
    @Test
    public void testHasHandlerAddWeak() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        final WeakEventHandler<Event> weakEventHandler =
                new WeakEventHandler<>(eventCountingHandler);
        compositeEventHandler.addEventHandler(weakEventHandler, DEFAULT);
        assertTrue("sanity: really added?", CompositeEventHandlerShim.containsHandler(
                compositeEventHandler, weakEventHandler));
        assertFalse("must not have filter after adding handler", compositeEventHandler.hasFilter(DEFAULT));
        assertTrue("must have handler", compositeEventHandler.hasHandler(DEFAULT));
        compositeEventHandler.removeEventHandler(weakEventHandler);
        assertFalse("must not have filter", compositeEventHandler.hasFilter(DEFAULT));
        assertFalse("must not have handler", compositeEventHandler.hasHandler(DEFAULT));
    }

    /**
     * test state after add/remove filter
     */
    @Test
    public void testHasFilter() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        compositeEventHandler.addEventFilter(eventCountingHandler, DEFAULT);
        assertFalse("must not have handler after adding filter", compositeEventHandler.hasHandler(DEFAULT));
        assertTrue("must have filter", compositeEventHandler.hasFilter(DEFAULT));
        compositeEventHandler.removeEventFilter(eventCountingHandler);
        assertFalse("must not have filter", compositeEventHandler.hasFilter(DEFAULT));
        assertFalse("must not have handler", compositeEventHandler.hasHandler(DEFAULT));
    }

    /**
     * test report after add/remove handler
     */
    @Test
    public void testHasHandlerAdd() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        compositeEventHandler.addEventHandler(eventCountingHandler, DEFAULT);
        assertTrue("sanity: really added?", CompositeEventHandlerShim.containsHandler(
                compositeEventHandler, eventCountingHandler));
        assertFalse("must not have filter after adding handler", compositeEventHandler.hasFilter(DEFAULT));
        assertTrue("must have handler", compositeEventHandler.hasHandler(DEFAULT));
        compositeEventHandler.removeEventHandler(eventCountingHandler);
        assertFalse("must not have filter", compositeEventHandler.hasFilter(DEFAULT));
        assertFalse("must not have handler", compositeEventHandler.hasHandler(DEFAULT));

    }

    /**
     * test state after set/null singleton handler
     */
    @Test
    public void testHasHandlerSingleton() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        compositeEventHandler.setEventHandler(eventCountingHandler);
        assertFalse("must not have filter after set handler", compositeEventHandler.hasFilter(DEFAULT));
        assertTrue("must have handler", compositeEventHandler.hasHandler(DEFAULT));
        compositeEventHandler.setEventHandler(null);
        assertFalse("must not have filter", compositeEventHandler.hasFilter(DEFAULT));
        assertFalse("must not have handler", compositeEventHandler.hasHandler(DEFAULT));
    }

    @Test
    public void weakEventHandlerTest() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        final WeakEventHandler<Event> weakEventHandler =
                new WeakEventHandler<>(eventCountingHandler);

        compositeEventHandler.addEventHandler(weakEventHandler, DEFAULT);

        Assert.assertTrue(
            CompositeEventHandlerShim.containsHandler(compositeEventHandler, weakEventHandler));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent(), DEFAULT);
        Assert.assertEquals(0, eventCountingHandler.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent(), DEFAULT);
        Assert.assertEquals(1, eventCountingHandler.getEventCount());

        WeakEventHandlerUtil.clear(weakEventHandler);

        Assert.assertFalse(
                CompositeEventHandlerShim.containsHandler(compositeEventHandler, weakEventHandler));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent(), DEFAULT);
        Assert.assertEquals(1, eventCountingHandler.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent(), DEFAULT);
        Assert.assertEquals(1, eventCountingHandler.getEventCount());
    }

    @Test
    public void weakEventFilterTest() {
        final EventCountingHandler<Event> eventCountingFilter =
                new EventCountingHandler<>();
        final WeakEventHandler<Event> weakEventFilter =
                new WeakEventHandler<>(eventCountingFilter);

        compositeEventHandler.addEventFilter(weakEventFilter, DEFAULT);

        Assert.assertTrue(
                CompositeEventHandlerShim.containsFilter(compositeEventHandler, weakEventFilter));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent(), DEFAULT);
        Assert.assertEquals(1, eventCountingFilter.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent(), DEFAULT);
        Assert.assertEquals(1, eventCountingFilter.getEventCount());

        WeakEventHandlerUtil.clear(weakEventFilter);

        Assert.assertFalse(
                CompositeEventHandlerShim.containsFilter(compositeEventHandler, weakEventFilter));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent(), DEFAULT);
        Assert.assertEquals(1, eventCountingFilter.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent(), DEFAULT);
        Assert.assertEquals(1, eventCountingFilter.getEventCount());
    }

    private record TracingHandler(List<Integer> trace, int ordinal) implements EventHandler<Event> {
        @Override public void handle(Event event) { trace.add(ordinal); }
    }

    @Test
    public void testEventHandlerPolicy() {
        var trace = new ArrayList<Integer>();

        compositeEventHandler.addEventHandler(new TracingHandler(trace, 1), DEFAULT);
        compositeEventHandler.addEventHandler(new TracingHandler(trace, 2), EventHandlerPriority.SYSTEM);
        compositeEventHandler.addEventHandler(new TracingHandler(trace, 3), EventHandlerPriority.PREFERRED);
        compositeEventHandler.addEventHandler(new TracingHandler(trace, 4), DEFAULT);
        compositeEventHandler.addEventHandler(new TracingHandler(trace, 5), EventHandlerPriority.SYSTEM);
        compositeEventHandler.addEventHandler(new TracingHandler(trace, 6), EventHandlerPriority.PREFERRED);
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent(), EventHandlerPriority.PREFERRED);
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent(), DEFAULT);
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent(), EventHandlerPriority.SYSTEM);

        assertEquals(List.of(3, 6, 1, 4, 2, 5), trace);
    }

    @Test
    public void testEventFilterPriority() {
        var trace = new ArrayList<Integer>();

        compositeEventHandler.addEventFilter(new TracingHandler(trace, 1), DEFAULT);
        compositeEventHandler.addEventFilter(new TracingHandler(trace, 2), EventHandlerPriority.SYSTEM);
        compositeEventHandler.addEventFilter(new TracingHandler(trace, 3), EventHandlerPriority.PREFERRED);
        compositeEventHandler.addEventFilter(new TracingHandler(trace, 4), DEFAULT);
        compositeEventHandler.addEventFilter(new TracingHandler(trace, 5), EventHandlerPriority.SYSTEM);
        compositeEventHandler.addEventFilter(new TracingHandler(trace, 6), EventHandlerPriority.PREFERRED);
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent(), EventHandlerPriority.PREFERRED);
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent(), DEFAULT);
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent(), EventHandlerPriority.SYSTEM);

        assertEquals(List.of(3, 6, 1, 4, 2, 5), trace);
    }
}
