/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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
                                final EventHandlerPriority eventHandlerPriority) {
        if (find(eventHandler, false) == null) {
            append(lastRecord, createEventHandlerRecord(eventHandler, eventHandlerPriority));
        }
    }

    public void removeEventHandler(final EventHandler<? super T> eventHandler) {
        final EventProcessorRecord<T> record = find(eventHandler, false);
        if (record != null) {
            remove(record);
        }
    }

    public void addEventFilter(final EventHandler<? super T> eventFilter,
                               final EventHandlerPriority eventFilterPriority) {
        if (find(eventFilter, true) == null) {
            append(lastRecord, createEventFilterRecord(eventFilter, eventFilterPriority));
        }
    }

    public void removeEventFilter(final EventHandler<? super T> eventFilter) {
        final EventProcessorRecord<T> record = find(eventFilter, true);
        if (record != null) {
            remove(record);
        }
    }

    /**
     * Dispatches a bubbling event to event handlers.
     *
     * @param event the event to dispatch
     * @param dispatchDefault {@code true} to dispatch to default handlers, {@code false} to dispatch to primary handlers
     * @return if {@code dispatchDefault} is {@code false}, returns whether any default handlers are present;
     *         otherwise the return value is unspecified
     */
    public boolean dispatchBubblingEvent(final Event event, final boolean dispatchDefault) {
        @SuppressWarnings("unchecked")
        final T specificEvent = (T) event;
        EventProcessorRecord<T> record = firstRecord;

        if (dispatchDefault) {
            if (specificEvent.isDefaultPrevented()) {
                return false;
            }

            record = firstRecord;
            while (record != null && !specificEvent.isConsumed()) {
                if (record.isDefault) {
                    record.handleBubblingEvent(specificEvent);
                }

                record = record.nextRecord;
            }

            return false;
        }

        boolean hasDefaultHandlers = false;

        while (record != null) {
            if (record.isDisconnected()) {
                remove(record);
            } else if (!specificEvent.isConsumed()) {
                hasDefaultHandlers |= record.isDefault;

                if (!record.isDefault) {
                    record.handleBubblingEvent(specificEvent);
                }
            }

            record = record.nextRecord;
        }

        if (eventHandler != null && !specificEvent.isConsumed()) {
            eventHandler.handle(specificEvent);
        }

        return hasDefaultHandlers;
    }

    /**
     * Dispatches a capturing event to event handlers.
     *
     * @param event the event to dispatch
     * @param dispatchDefault {@code true} to dispatch to default handlers, {@code false} to dispatch to primary handlers
     * @return if {@code dispatchDefault} is {@code false}, returns whether any default handlers are present;
     *         otherwise the return value is unspecified
     */
    public boolean dispatchCapturingEvent(final Event event, final boolean dispatchDefault) {
        @SuppressWarnings("unchecked")
        final T specificEvent = (T) event;
        EventProcessorRecord<T> record = firstRecord;

        if (dispatchDefault) {
            if (specificEvent.isDefaultPrevented()) {
                return false;
            }

            record = firstRecord;
            while (record != null && !specificEvent.isConsumed()) {
                if (record.isDefault) {
                    record.handleCapturingEvent(specificEvent);
                }

                record = record.nextRecord;
            }

            return false;
        }

        boolean hasDefaultHandlers = false;

        while (record != null) {
            if (record.isDisconnected()) {
                remove(record);
            } else if (!specificEvent.isConsumed()) {
                hasDefaultHandlers |= record.isDefault;

                if (!record.isDefault) {
                    record.handleCapturingEvent(specificEvent);
                }
            }

            record = record.nextRecord;
        }

        return hasDefaultHandlers;
    }

    public boolean hasFilter() {
        return find(true);
    }

    public boolean hasHandler() {
        if (getEventHandler() != null) return true;
        return find(false);
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
            final EventHandler<? super T> eventHandler,
            final EventHandlerPriority eventHandlerPriority) {
        return (eventHandler instanceof WeakEventHandler<? super T> weakHandler)
               ? new WeakEventHandlerRecord<>(weakHandler, eventHandlerPriority == EventHandlerPriority.DEFAULT)
               : new NormalEventHandlerRecord<>(eventHandler, eventHandlerPriority == EventHandlerPriority.DEFAULT);
    }

    private EventProcessorRecord<T> createEventFilterRecord(
            final EventHandler<? super T> eventFilter,
            final EventHandlerPriority eventFilterPriority) {
        return (eventFilter instanceof WeakEventHandler<? super T> weakHandler)
               ? new WeakEventFilterRecord<>(weakHandler, eventFilterPriority == EventHandlerPriority.DEFAULT)
               : new NormalEventFilterRecord<>(eventFilter, eventFilterPriority == EventHandlerPriority.DEFAULT);
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

    private boolean find(boolean isFilter) {
        EventProcessorRecord<T> record = firstRecord;
        while (record != null) {
            if (record.isDisconnected()) {
                remove(record);
            } else if (isFilter == record.isFilter()) {
                return true;
            }
            record = record.nextRecord;
        }
        return false;
    }

    private static abstract class EventProcessorRecord<T extends Event> {
        private final boolean isDefault;
        private EventProcessorRecord<T> nextRecord;
        private EventProcessorRecord<T> prevRecord;

        EventProcessorRecord(boolean isDefault) {
            this.isDefault = isDefault;
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

        public NormalEventHandlerRecord(EventHandler<? super T> eventHandler, boolean isDefault) {
            super(isDefault);
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

        public WeakEventHandlerRecord(WeakEventHandler<? super T> weakEventHandler, boolean isDefault) {
            super(isDefault);
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

        public NormalEventFilterRecord(EventHandler<? super T> eventFilter, boolean isDefault) {
            super(isDefault);
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

        public WeakEventFilterRecord(WeakEventHandler<? super T> weakEventFilter, boolean isDefault) {
            super(isDefault);
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
