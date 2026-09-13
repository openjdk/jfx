/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.jfx.incubator.scene.control.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javafx.event.Event;
import javafx.event.EventHandler;

/**
 * Priority Handler List.
 * Arranges event handlers according to their EventHandlerPriority.
 */
public class PHList {
    /**
     * {@code items} is a list of {@code EventHandler}s ordered from high priority to low,
     * with each block of same priority prefixed with the priority value.
     * Also, USER_KB and SKIN_KB require no handler pointer, so none is added.<p>
     * Example:
     * [ USER_HIGH, handler1, handler2, SKIN_KB, SKIN_LOW, handler3 ]
     */
    private final ArrayList<Object> items = new ArrayList(4);

    @Override
    public String toString() {
        return "PHList{items=" + items + "}";
    }

    /**
     * Adds the specified priority (always), and the specified handler if not null.
     * A newly added handler will be inserted after previously added handlers with the same priority.
     * @param priority the priority
     * @param handler the handler to add
     */
    public void add(EventHandlerPriority priority, EventHandler<?> handler) {
        // positive: simply insert the handler there
        // negative: insert priority and the handler if it's not null
        int ix = findInsertionIndex(priority);
        if (ix < 0) {
            ix = -ix - 1;
            insert(ix, priority);
            // do not store the null handler
            if (handler != null) {
                insert(++ix, handler);
            }
        } else {
            insert(ix, handler);
        }
    }

    private void insert(int ix, Object item) {
        if (ix < items.size()) {
            items.add(ix, item);
        } else {
            items.add(item);
        }
    }

    /**
     * Removes all the instances of the specified handler.  Returns true if the list becomes empty as a result.
     * Returns true if the list becomes empty as a result of the removal.
     *
     * @param <T> the event type
     * @param handler the handler to remove
     * @return true when the list becomes empty as a result
     */
    public <T extends Event> boolean remove(EventHandler<T> handler) {
        for (int i = 0; i < items.size(); i++) {
            Object x = items.get(i);
            if (x == handler) {
                items.remove(i);
                if (isNullOrPriority(i) && isNullOrPriority(i - 1)) {
                    // remove priority
                    --i;
                    items.remove(i);
                }
            }
        }
        return items.isEmpty();
    }

    private boolean isNullOrPriority(int ix) {
        if ((ix >= 0) && (ix < items.size())) {
            Object x = items.get(ix);
            return (x instanceof EventHandlerPriority);
        }
        return true;
    }

    /**
     * Returns the index into {@code items}.
     * When the list contains no elements of the given priority, the return value is
     * negative, equals to {@code -(insertionIndex + 1)},
     * and the caller must insert the priority value in addition to the handler.
     *
     * @param priority the priority
     * @return the insertion index (positive), or -(insertionIndex + 1) (negative)
     */
    private int findInsertionIndex(EventHandlerPriority priority) {
        // don't expect many handlers, so linear search is ok
        int sz = items.size();
        boolean found = false;
        for (int i = 0; i < sz; i++) {
            Object x = items.get(i);
            if (x instanceof EventHandlerPriority p) {
                if (p.priority == priority.priority) {
                    found = true;
                    continue;
                } else if (p.priority < priority.priority) {
                    return found ? i : -(i + 1);
                }
            }
        }
        return found ? sz : -(sz + 1);
    }

    /**
     * A client interface for the {@link #forEach(Client)} method.
     * @param <T> the event type
     */
    @FunctionalInterface
    public static interface Client<T extends Event> {
        /**
         * This method gets called for each handler in the order of priority.
         * The client may signal to stop iterating by returning false from this method.
         *
         * @param pri the priority
         * @param h the handler (can be null)
         * @return true to continue the process, false to stop
         */
        public boolean accept(EventHandlerPriority pri, EventHandler<T> h);
    }

    /**
     * Invokes the {@code client} for each handler in the order of priority.
     * @param <T> the event type
     * @param client the client reference
     */
    public <T extends Event> void forEach(Client<T> client) {
        EventHandlerPriority pri = null;
        boolean stop;
        int sz = items.size();
        for (int i = 0; i < sz; i++) {
            Object x = items.get(i);
            if (x instanceof EventHandlerPriority p) {
                pri = p;
                if (isNullOrPriority(i + 1)) {
                    stop = !client.accept(pri, null);
                } else {
                    continue;
                }
            } else {
                // it's a handler, cannot be null
                stop = !client.accept(pri, (EventHandler<T>)x);
            }
            if (stop) {
                break;
            }
        }
    }

    /**
     * Removes all the entries with the specified priorities.
     * @return true if list is empty as a result
     */
    public boolean removeHandlers(Set<EventHandlerPriority> priorities) {
        boolean remove = false;
        for (int i = 0; i < items.size();) {
            Object x = items.get(i);
            if (x instanceof EventHandlerPriority p) {
                if (priorities.contains(p)) {
                    remove = true;
                    items.remove(i);
                } else {
                    remove = false;
                    i++;
                }
            } else {
                if (remove) {
                    items.remove(i);
                } else {
                    i++;
                }
            }
        }
        return items.isEmpty();
    }

    /**
     * An internal testing method.
     * @param expected the expected internal structure
     */
    public void validateInternalState(Object... expected) {
        if (!Arrays.equals(expected, items.toArray())) {
            throw new RuntimeException("internal mismatch:\nitems=" + items + "\nexpected=" + List.of(expected));
        }
    }
}
