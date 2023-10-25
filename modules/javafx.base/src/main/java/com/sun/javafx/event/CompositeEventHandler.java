/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.EventHandler;
import javafx.event.EventHandlerPriority;
import javafx.event.WeakEventHandler;

public final class CompositeEventHandler<T extends Event> {
    private EventProcessorRecord<T> firstRecord;
    private EventProcessorRecord<T> lastRecord;
    private EventHandler<? super T> eventHandler;

    public void setEventHandler(final EventHandler<? super T> eventHandler) {
        this.eventHandler = eventHandler;
    }

    public EventHandler<? super T> getEventHandler() {
        return eventHandler;
    }

    public void addEventHandler(final EventHandler<? super T> eventHandler,
                                final EventHandlerPriority priority) {
        if (find(eventHandler, false) == null) {
            append(findInsertAfter(priority), createEventHandlerRecord(eventHandler, priority));
        }
    }

    public void removeEventHandler(final EventHandler<? super T> eventHandler) {
        final EventProcessorRecord<T> record = find(eventHandler, false);
        if (record != null) {
            remove(record);
        }
    }

    public void addEventFilter(final EventHandler<? super T> eventFilter,
                               final EventHandlerPriority priority) {
        if (find(eventFilter, true) == null) {
            append(findInsertAfter(priority), createEventFilterRecord(eventFilter, priority));
        }
    }

    public void removeEventFilter(final EventHandler<? super T> eventFilter) {
        final EventProcessorRecord<T> record = find(eventFilter, true);
        if (record != null) {
            remove(record);
        }
    }

    public void dispatchBubblingEvent(final Event event, final EventHandlerPriority priority) {
        final T specificEvent = (T) event;

        EventProcessorRecord<T> record = firstRecord;
        while (record != null) {
            if (record.isDisconnected()) {
                remove(record);
            } else if (record.priority == priority) {
                record.handleBubblingEvent(specificEvent);
            }
            record = record.nextRecord;
        }

        if (eventHandler != null && priority == EventHandlerPriority.DEFAULT) {
            eventHandler.handle(specificEvent);
        }
    }

    public void dispatchCapturingEvent(final Event event, final EventHandlerPriority priority) {
        final T specificEvent = (T) event;

        EventProcessorRecord<T> record = firstRecord;
        while (record != null) {
            if (record.isDisconnected()) {
                remove(record);
            } else if (record.priority == priority) {
                record.handleCapturingEvent(specificEvent);
            }
            record = record.nextRecord;
        }
    }

    public boolean hasFilter(EventHandlerPriority priority) {
        return find(true, priority);
    }

    public boolean hasHandler(EventHandlerPriority priority) {
        if (getEventHandler() != null) return true;
        return find(false, priority);
    }

    /* Used for testing. */
    boolean containsHandler(final EventHandler<? super T> eventHandler) {
        return find(eventHandler, false) != null;
    }

    /* Used for testing. */
    boolean containsFilter(final EventHandler<? super T> eventFilter) {
        return find(eventFilter, true) != null;
    }

    private EventProcessorRecord<T> createEventHandlerRecord(
            final EventHandler<? super T> eventHandler, final EventHandlerPriority priority) {
        return (eventHandler instanceof WeakEventHandler)
                   ? new WeakEventHandlerRecord<>((WeakEventHandler<? super T>) eventHandler, priority)
                   : new NormalEventHandlerRecord<>(eventHandler, priority);
    }

    private EventProcessorRecord<T> createEventFilterRecord(
            final EventHandler<? super T> eventFilter, final EventHandlerPriority priority) {
        return (eventFilter instanceof WeakEventHandler)
                   ? new WeakEventFilterRecord<>((WeakEventHandler<? super T>) eventFilter, priority)
                   : new NormalEventFilterRecord<>(eventFilter, priority);
    }

    private void remove(final EventProcessorRecord<T> record) {
        final EventProcessorRecord<T> prevRecord = record.prevRecord;
        final EventProcessorRecord<T> nextRecord = record.nextRecord;

        if (prevRecord != null) {
            prevRecord.nextRecord = nextRecord;
        } else {
            firstRecord = nextRecord;
        }

        if (nextRecord != null) {
            nextRecord.prevRecord = prevRecord;
        } else {
            lastRecord = prevRecord;
        }

        // leave record.nextRecord set
    }

    private void append(final EventProcessorRecord<T> prevRecord,
                        final EventProcessorRecord<T> newRecord) {
        EventProcessorRecord<T> nextRecord;
        if (prevRecord != null) {
            nextRecord = prevRecord.nextRecord;
            prevRecord.nextRecord = newRecord;
        } else {
            nextRecord = firstRecord;
            firstRecord = newRecord;
        }

        if (nextRecord != null) {
            nextRecord.prevRecord = newRecord;
        } else {
            lastRecord = newRecord;
        }

        newRecord.prevRecord = prevRecord;
        newRecord.nextRecord = nextRecord;
    }

    private EventProcessorRecord<T> find(
            final EventHandler<? super T> eventProcessor,
            final boolean isFilter) {
        EventProcessorRecord<T> record = firstRecord;
        while (record != null) {
            if (record.isDisconnected()) {
                remove(record);
            } else if (record.stores(eventProcessor, isFilter)) {
                return record;
            }

            record = record.nextRecord;
        }

        return null;
    }

    private boolean find(boolean isFilter, EventHandlerPriority priority) {
        EventProcessorRecord<T> record = firstRecord;
        while (record != null) {
            if (record.isDisconnected()) {
                remove(record);
            } else if (isFilter == record.isFilter() && priority == record.priority) {
                return true;
            }
            record = record.nextRecord;
        }
        return false;
    }

    private EventProcessorRecord<T> findInsertAfter(EventHandlerPriority priority) {
        EventProcessorRecord<T> record = firstRecord;
        while (record != null) {
            if (record.isDisconnected()) {
                remove(record);
            } else if (record.priority.compareTo(priority) < 0) {
                return record.prevRecord;
            }

            record = record.nextRecord;
        }

        return lastRecord;
    }

    private static abstract class EventProcessorRecord<T extends Event> {
        final EventHandlerPriority priority;
        private EventProcessorRecord<T> nextRecord;
        private EventProcessorRecord<T> prevRecord;

        EventProcessorRecord(EventHandlerPriority priority) {
            this.priority = priority;
        }

        public abstract boolean stores(EventHandler<? super T> eventProcessor,
                                       boolean isFilter);

        public abstract boolean isFilter();

        public abstract void handleBubblingEvent(T event);

        public abstract void handleCapturingEvent(T event);

        public abstract boolean isDisconnected();
    }

    private static final class NormalEventHandlerRecord<T extends Event>
            extends EventProcessorRecord<T> {
        private final EventHandler<? super T> eventHandler;

        public NormalEventHandlerRecord(final EventHandler<? super T> eventHandler,
                                        final EventHandlerPriority priority) {
            super(priority);
            this.eventHandler = eventHandler;
        }

        @Override
        public boolean stores(final EventHandler<? super T> eventProcessor,
                              final boolean isFilter) {
            return isFilter == isFilter() && (this.eventHandler == eventProcessor);
        }

        @Override
        public boolean isFilter() {
            return false;
        }

        @Override
        public void handleBubblingEvent(final T event) {
            eventHandler.handle(event);
        }

        @Override
        public void handleCapturingEvent(final T event) {
        }

        @Override
        public boolean isDisconnected() {
            return false;
        }
    }

    private static final class WeakEventHandlerRecord<T extends Event>
            extends EventProcessorRecord<T> {
        private final WeakEventHandler<? super T> weakEventHandler;

        public WeakEventHandlerRecord(final WeakEventHandler<? super T> weakEventHandler,
                                      final EventHandlerPriority priority) {
            super(priority);
            this.weakEventHandler = weakEventHandler;
        }

        @Override
        public boolean stores(final EventHandler<? super T> eventProcessor,
                              final boolean isFilter) {
            return isFilter == isFilter() && (weakEventHandler == eventProcessor);
        }

        @Override
        public boolean isFilter() {
            return false;
        }

        @Override
        public void handleBubblingEvent(final T event) {
            weakEventHandler.handle(event);
        }

        @Override
        public void handleCapturingEvent(final T event) {
        }

        @Override
        public boolean isDisconnected() {
            return weakEventHandler.wasGarbageCollected();
        }
    }

    private static final class NormalEventFilterRecord<T extends Event>
            extends EventProcessorRecord<T> {
        private final EventHandler<? super T> eventFilter;

        public NormalEventFilterRecord(final EventHandler<? super T> eventFilter,
                                       final EventHandlerPriority priority) {
            super(priority);
            this.eventFilter = eventFilter;
        }

        @Override
        public boolean stores(final EventHandler<? super T> eventProcessor,
                              final boolean isFilter) {
            return isFilter == isFilter() && (this.eventFilter == eventProcessor);
        }

        @Override
        public boolean isFilter() {
            return true;
        }

        @Override
        public void handleBubblingEvent(final T event) {
        }

        @Override
        public void handleCapturingEvent(final T event) {
            eventFilter.handle(event);
        }

        @Override
        public boolean isDisconnected() {
            return false;
        }
    }

    private static final class WeakEventFilterRecord<T extends Event>
            extends EventProcessorRecord<T> {
        private final WeakEventHandler<? super T> weakEventFilter;

        public WeakEventFilterRecord(final WeakEventHandler<? super T> weakEventFilter,
                                     final EventHandlerPriority priority) {
            super(priority);
            this.weakEventFilter = weakEventFilter;
        }

        @Override
        public boolean stores(final EventHandler<? super T> eventProcessor,
                              final boolean isFilter) {
            return isFilter == isFilter() && (weakEventFilter == eventProcessor);
        }

        @Override
        public boolean isFilter() {
            return true;
        }

        @Override
        public void handleBubblingEvent(final T event) {
        }

        @Override
        public void handleCapturingEvent(final T event) {
            weakEventFilter.handle(event);
        }

        @Override
        public boolean isDisconnected() {
            return weakEventFilter.wasGarbageCollected();
        }
    }
}
