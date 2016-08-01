/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

public class EventDispatchChainImpl implements EventDispatchChain {
    /** Must be a power of two. */
    private static final int CAPACITY_GROWTH_FACTOR = 8;

    private EventDispatcher[] dispatchers;

    private int[] nextLinks;

    private int reservedCount;
    private int activeCount;
    private int headIndex;
    private int tailIndex;

    public EventDispatchChainImpl() {
    }

    public void reset() {
        // shrink?
        for (int i = 0; i < reservedCount; ++i) {
            dispatchers[i] = null;
        }

        reservedCount = 0;
        activeCount = 0;
        headIndex = 0;
        tailIndex = 0;
    }

    @Override
    public EventDispatchChain append(final EventDispatcher eventDispatcher) {
        ensureCapacity(reservedCount + 1);

        if (activeCount == 0) {
            insertFirst(eventDispatcher);
            return this;
        }

        dispatchers[reservedCount] = eventDispatcher;
        nextLinks[tailIndex] = reservedCount;
        tailIndex = reservedCount;

        ++activeCount;
        ++reservedCount;

        return this;
    }

    @Override
    public EventDispatchChain prepend(final EventDispatcher eventDispatcher) {
        ensureCapacity(reservedCount + 1);

        if (activeCount == 0) {
            insertFirst(eventDispatcher);
            return this;
        }

        dispatchers[reservedCount] = eventDispatcher;
        nextLinks[reservedCount] = headIndex;
        headIndex = reservedCount;

        ++activeCount;
        ++reservedCount;

        return this;
    }

    @Override
    public Event dispatchEvent(final Event event) {
        if (activeCount == 0) {
            return event;
        }

        // push current state
        final int savedHeadIndex = headIndex;
        final int savedTailIndex = tailIndex;
        final int savedActiveCount = activeCount;
        final int savedReservedCount = reservedCount;

        final EventDispatcher nextEventDispatcher = dispatchers[headIndex];
        headIndex = nextLinks[headIndex];
        --activeCount;
        final Event returnEvent =
                nextEventDispatcher.dispatchEvent(event, this);

        // pop saved state
        headIndex = savedHeadIndex;
        tailIndex = savedTailIndex;
        activeCount = savedActiveCount;
        reservedCount = savedReservedCount;

        return returnEvent;
    }

    private void insertFirst(final EventDispatcher eventDispatcher) {
        dispatchers[reservedCount] = eventDispatcher;
        headIndex = reservedCount;
        tailIndex = reservedCount;

        activeCount = 1;
        ++reservedCount;
    }

    private void ensureCapacity(final int size) {
        final int newCapacity = (size + CAPACITY_GROWTH_FACTOR - 1)
                                    & ~(CAPACITY_GROWTH_FACTOR - 1);
        if (newCapacity == 0) {
            return;
        }

        if ((dispatchers == null) || (dispatchers.length < newCapacity)) {
            final EventDispatcher[] newDispatchers =
                    new EventDispatcher[newCapacity];
            final int[] newLinks = new int[newCapacity];

            if (reservedCount > 0) {
                System.arraycopy(dispatchers, 0, newDispatchers, 0,
                                 reservedCount);
                System.arraycopy(nextLinks, 0, newLinks, 0, reservedCount);
            }

            dispatchers = newDispatchers;
            nextLinks = newLinks;
        }
    }
}
