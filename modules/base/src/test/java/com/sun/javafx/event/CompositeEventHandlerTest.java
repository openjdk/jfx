/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.WeakEventHandler;
import javafx.event.WeakEventHandlerUtil;
import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

public class CompositeEventHandlerTest {
    private CompositeEventHandler<Event> compositeEventHandler;

    @Before
    public void setUp() {
        compositeEventHandler = new CompositeEventHandler<Event>();
    }

    @Test
    public void weakEventHandlerTest() {
        final EventCountingHandler<Event> eventCountingHandler =
                new EventCountingHandler<Event>();
        final WeakEventHandler<Event> weakEventHandler =
                new WeakEventHandler<Event>(eventCountingHandler);

        compositeEventHandler.addEventHandler(weakEventHandler);
        
        Assert.assertTrue(
                compositeEventHandler.containsHandler(weakEventHandler));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        Assert.assertEquals(0, eventCountingHandler.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingHandler.getEventCount());

        WeakEventHandlerUtil.clear(weakEventHandler);

        Assert.assertFalse(
                compositeEventHandler.containsHandler(weakEventHandler));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingHandler.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingHandler.getEventCount());
    }

    @Test
    public void weakEventFilterTest() {
        final EventCountingHandler<Event> eventCountingFilter =
                new EventCountingHandler<Event>();
        final WeakEventHandler<Event> weakEventFilter =
                new WeakEventHandler<Event>(eventCountingFilter);

        compositeEventHandler.addEventFilter(weakEventFilter);

        Assert.assertTrue(
                compositeEventHandler.containsFilter(weakEventFilter));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingFilter.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingFilter.getEventCount());

        WeakEventHandlerUtil.clear(weakEventFilter);

        Assert.assertFalse(
                compositeEventHandler.containsFilter(weakEventFilter));
        compositeEventHandler.dispatchCapturingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingFilter.getEventCount());
        compositeEventHandler.dispatchBubblingEvent(new EmptyEvent());
        Assert.assertEquals(1, eventCountingFilter.getEventCount());
    }
}
