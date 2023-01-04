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

import static org.junit.Assert.*;

import javafx.event.Event;
import javafx.event.WeakEventHandler;
import javafx.event.WeakEventHandlerUtil;
import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

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

        compositeEventHandler.addEventFilter(weakEventHandler);
        assertFalse("must not have handler after adding filter", compositeEventHandler.hasHandler());
        assertTrue("must have filter", compositeEventHandler.hasFilter());
        WeakEventHandlerUtil.clear(weakEventHandler);
        assertFalse("must not have filter", compositeEventHandler.hasFilter());
        assertFalse("must not have handler", compositeEventHandler.hasHandler());
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
        assertTrue("sanity: really added?", CompositeEventHandlerShim.containsHandler(
                compositeEventHandler, weakEventHandler));
        assertFalse("must not have filter after adding handler", compositeEventHandler.hasFilter());
        assertTrue("must have handler", compositeEventHandler.hasHandler());
        WeakEventHandlerUtil.clear(weakEventHandler);
        assertFalse("must not have handler", compositeEventHandler.hasHandler());
        assertFalse("must not have filter", compositeEventHandler.hasFilter());
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
        assertFalse("must not have handler after adding filter", compositeEventHandler.hasHandler());
        assertTrue("must have filter", compositeEventHandler.hasFilter());
        compositeEventHandler.removeEventFilter(weakEventHandler);
        assertFalse("must not have filter", compositeEventHandler.hasFilter());
        assertFalse("must not have handler", compositeEventHandler.hasHandler());
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
        assertTrue("sanity: really added?", CompositeEventHandlerShim.containsHandler(
                compositeEventHandler, weakEventHandler));
        assertFalse("must not have filter after adding handler", compositeEventHandler.hasFilter());
        assertTrue("must have handler", compositeEventHandler.hasHandler());
        compositeEventHandler.removeEventHandler(weakEventHandler);
        assertFalse("must not have filter", compositeEventHandler.hasFilter());
        assertFalse("must not have handler", compositeEventHandler.hasHandler());
    }

    /**
     * test state after add/remove filter
     */
    @Test
    public void testHasFilter() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        compositeEventHandler.addEventFilter(eventCountingHandler);
        assertFalse("must not have handler after adding filter", compositeEventHandler.hasHandler());
        assertTrue("must have filter", compositeEventHandler.hasFilter());
        compositeEventHandler.removeEventFilter(eventCountingHandler);
        assertFalse("must not have filter", compositeEventHandler.hasFilter());
        assertFalse("must not have handler", compositeEventHandler.hasHandler());
    }

    /**
     * test report after add/remove handler
     */
    @Test
    public void testHasHandlerAdd() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        compositeEventHandler.addEventHandler(eventCountingHandler);
        assertTrue("sanity: really added?", CompositeEventHandlerShim.containsHandler(
                compositeEventHandler, eventCountingHandler));
        assertFalse("must not have filter after adding handler", compositeEventHandler.hasFilter());
        assertTrue("must have handler", compositeEventHandler.hasHandler());
        compositeEventHandler.removeEventHandler(eventCountingHandler);
        assertFalse("must not have filter", compositeEventHandler.hasFilter());
        assertFalse("must not have handler", compositeEventHandler.hasHandler());

    }

    /**
     * test state after set/null singleton handler
     */
    @Test
    public void testHasHandlerSingleton() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        compositeEventHandler.setEventHandler(eventCountingHandler);
        assertFalse("must not have filter after set handler", compositeEventHandler.hasFilter());
        assertTrue("must have handler", compositeEventHandler.hasHandler());
        compositeEventHandler.setEventHandler(null);
        assertFalse("must not have filter", compositeEventHandler.hasFilter());
        assertFalse("must not have handler", compositeEventHandler.hasHandler());
    }

    @Test
    public void weakEventHandlerTest() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<>();
        final WeakEventHandler<Event> weakEventHandler =
                new WeakEventHandler<>(eventCountingHandler);

        compositeEventHandler.addEventHandler(weakEventHandler);

        Assert.assertTrue(
            CompositeEventHandlerShim.containsHandler(compositeEventHandler, weakEventHandler));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        Assert.assertEquals(0, eventCountingHandler.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingHandler.getEventCount());

        WeakEventHandlerUtil.clear(weakEventHandler);

        Assert.assertFalse(
                CompositeEventHandlerShim.containsHandler(compositeEventHandler, weakEventHandler));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingHandler.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingHandler.getEventCount());
    }

    @Test
    public void weakEventFilterTest() {
        final EventCountingHandler<Event> eventCountingFilter =
                new EventCountingHandler<>();
        final WeakEventHandler<Event> weakEventFilter =
                new WeakEventHandler<>(eventCountingFilter);

        compositeEventHandler.addEventFilter(weakEventFilter);

        Assert.assertTrue(
                CompositeEventHandlerShim.containsFilter(compositeEventHandler, weakEventFilter));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingFilter.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingFilter.getEventCount());

        WeakEventHandlerUtil.clear(weakEventFilter);

        Assert.assertFalse(
                CompositeEventHandlerShim.containsFilter(compositeEventHandler, weakEventFilter));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingFilter.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingFilter.getEventCount());
    }
}
