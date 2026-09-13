/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.*;

import javafx.event.Event;
import javafx.event.WeakEventHandler;
import javafx.event.WeakEventHandlerUtil;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CompositeEventHandlerTest {
    private CompositeEventHandler<Event> compositeEventHandler;

    @BeforeEach
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

        compositeEventHandler.addEventFilter(weakEventHandler);
        assertFalse(compositeEventHandler.hasHandler(), "must not have handler after adding filter");
        assertTrue(compositeEventHandler.hasFilter(), "must have filter");
        WeakEventHandlerUtil.clear(weakEventHandler);
        assertFalse(compositeEventHandler.hasFilter(), "must not have filter");
        assertFalse(compositeEventHandler.hasHandler(), "must not have handler");
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
        compositeEventHandler.addEventHandler(weakEventHandler);
        assertTrue(CompositeEventHandlerShim.containsHandler(
                compositeEventHandler, weakEventHandler), "sanity: really added?");
        assertFalse(compositeEventHandler.hasFilter(), "must not have filter after adding handler");
        assertTrue(compositeEventHandler.hasHandler(), "must have handler");
        WeakEventHandlerUtil.clear(weakEventHandler);
        assertFalse(compositeEventHandler.hasHandler(), "must not have handler");
        assertFalse(compositeEventHandler.hasFilter(), "must not have filter");
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

        compositeEventHandler.addEventFilter(weakEventHandler);
        assertFalse(compositeEventHandler.hasHandler(), "must not have handler after adding filter");
        assertTrue(compositeEventHandler.hasFilter(), "must have filter");
        compositeEventHandler.removeEventFilter(weakEventHandler);
        assertFalse(compositeEventHandler.hasFilter(), "must not have filter");
        assertFalse(compositeEventHandler.hasHandler(), "must not have handler");
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
        compositeEventHandler.addEventHandler(weakEventHandler);
        assertTrue(CompositeEventHandlerShim.containsHandler(
                compositeEventHandler, weakEventHandler), "sanity: really added?");
        assertFalse(compositeEventHandler.hasFilter(), "must not have filter after adding handler");
        assertTrue(compositeEventHandler.hasHandler(), "must have handler");
        compositeEventHandler.removeEventHandler(weakEventHandler);
        assertFalse(compositeEventHandler.hasFilter(), "must not have filter");
        assertFalse(compositeEventHandler.hasHandler(), "must not have handler");
    }

    /**
     * test state after add/remove filter
     */
    @Test
    public void testHasFilter() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        compositeEventHandler.addEventFilter(eventCountingHandler);
        assertFalse(compositeEventHandler.hasHandler(), "must not have handler after adding filter");
        assertTrue(compositeEventHandler.hasFilter(), "must have filter");
        compositeEventHandler.removeEventFilter(eventCountingHandler);
        assertFalse(compositeEventHandler.hasFilter(), "must not have filter");
        assertFalse(compositeEventHandler.hasHandler(), "must not have handler");
    }

    /**
     * test report after add/remove handler
     */
    @Test
    public void testHasHandlerAdd() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        compositeEventHandler.addEventHandler(eventCountingHandler);
        assertTrue(CompositeEventHandlerShim.containsHandler(
                compositeEventHandler, eventCountingHandler), "sanity: really added?");
        assertFalse(compositeEventHandler.hasFilter(), "must not have filter after adding handler");
        assertTrue(compositeEventHandler.hasHandler(), "must have handler");
        compositeEventHandler.removeEventHandler(eventCountingHandler);
        assertFalse(compositeEventHandler.hasFilter(), "must not have filter");
        assertFalse(compositeEventHandler.hasHandler(), "must not have handler");

    }

    /**
     * test state after set/null singleton handler
     */
    @Test
    public void testHasHandlerSingleton() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        compositeEventHandler.setEventHandler(eventCountingHandler);
        assertFalse(compositeEventHandler.hasFilter(), "must not have filter after set handler");
        assertTrue(compositeEventHandler.hasHandler(), "must have handler");
        compositeEventHandler.setEventHandler(null);
        assertFalse(compositeEventHandler.hasFilter(), "must not have filter");
        assertFalse(compositeEventHandler.hasHandler(), "must not have handler");
    }

    @Test
    public void weakEventHandlerTest() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        final WeakEventHandler<Event> weakEventHandler =
                new WeakEventHandler<>(eventCountingHandler);

        compositeEventHandler.addEventHandler(weakEventHandler);

        assertTrue(
                CompositeEventHandlerShim.containsHandler(compositeEventHandler, weakEventHandler));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        assertEquals(0, eventCountingHandler.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        assertEquals(1, eventCountingHandler.getEventCount());

        WeakEventHandlerUtil.clear(weakEventHandler);

        assertFalse(
                CompositeEventHandlerShim.containsHandler(compositeEventHandler, weakEventHandler));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        assertEquals(1, eventCountingHandler.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        assertEquals(1, eventCountingHandler.getEventCount());
    }

    @Test
    public void weakEventFilterTest() {
        final EventCountingHandler<Event> eventCountingFilter =
                new EventCountingHandler<>();
        final WeakEventHandler<Event> weakEventFilter =
                new WeakEventHandler<>(eventCountingFilter);

        compositeEventHandler.addEventFilter(weakEventFilter);

        assertTrue(
                CompositeEventHandlerShim.containsFilter(compositeEventHandler, weakEventFilter));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        assertEquals(1, eventCountingFilter.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        assertEquals(1, eventCountingFilter.getEventCount());

        WeakEventHandlerUtil.clear(weakEventFilter);

        assertFalse(
                CompositeEventHandlerShim.containsFilter(compositeEventHandler, weakEventFilter));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        assertEquals(1, eventCountingFilter.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        assertEquals(1, eventCountingFilter.getEventCount());
    }
}
