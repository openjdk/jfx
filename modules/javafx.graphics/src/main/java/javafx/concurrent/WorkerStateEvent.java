/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.concurrent;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * An event which occurs whenever the state changes on a Worker. Both
 * {@link Task} and {@link Service} support listening to state events.
 * @since JavaFX 2.1
 */
public class WorkerStateEvent extends Event {
    /**
     * Common supertype for all worker state event types.
     */
    public static final EventType<WorkerStateEvent> ANY =
            new EventType<WorkerStateEvent>(Event.ANY, "WORKER_STATE");

    /**
     * This event occurs when the state of a Worker implementation has
     * transitioned to the READY state.
     */
    public static final EventType<WorkerStateEvent> WORKER_STATE_READY =
            new EventType<WorkerStateEvent>(WorkerStateEvent.ANY, "WORKER_STATE_READY");

    /**
     * This event occurs when the state of a Worker implementation has
     * transitioned to the SCHEDULED state.
     */
    public static final EventType<WorkerStateEvent> WORKER_STATE_SCHEDULED =
            new EventType<WorkerStateEvent>(WorkerStateEvent.ANY, "WORKER_STATE_SCHEDULED");

    /**
     * This event occurs when the state of a Worker implementation has
     * transitioned to the RUNNING state.
     */
    public static final EventType<WorkerStateEvent> WORKER_STATE_RUNNING =
            new EventType<WorkerStateEvent>(WorkerStateEvent.ANY, "WORKER_STATE_RUNNING");

    /**
     * This event occurs when the state of a Worker implementation has
     * transitioned to the SUCCEEDED state.
     */
    public static final EventType<WorkerStateEvent> WORKER_STATE_SUCCEEDED =
            new EventType<WorkerStateEvent>(WorkerStateEvent.ANY, "WORKER_STATE_SUCCEEDED");

    /**
     * This event occurs when the state of a Worker implementation has
     * transitioned to the CANCELLED state.
     */
    public static final EventType<WorkerStateEvent> WORKER_STATE_CANCELLED =
            new EventType<WorkerStateEvent>(WorkerStateEvent.ANY, "WORKER_STATE_CANCELLED");

    /**
     * This event occurs when the state of a Worker implementation has
     * transitioned to the FAILED state.
     */
    public static final EventType<WorkerStateEvent> WORKER_STATE_FAILED =
            new EventType<WorkerStateEvent>(WorkerStateEvent.ANY, "WORKER_STATE_FAILED");

    /**
     * Create a new WorkerStateEvent. Specify the worker and the event type.
     *
     * @param worker The Worker which is firing the event. The Worker really
     *               should be an EventTarget, otherwise the EventTarget
     *               for the event will be null.
     * @param eventType The type of event. This should not be null.
     */
    public WorkerStateEvent(@NamedArg("worker") Worker worker, @NamedArg("eventType") EventType<? extends WorkerStateEvent> eventType) {
        super(worker, worker instanceof EventTarget ? (EventTarget) worker : null, eventType);
    }

    /**
     * The Worker on which the Event initially occurred.
     *
     * @return The Worker on which the Event initially occurred.
     */
    @Override public Worker getSource() {
        return (Worker) super.getSource();
    }
}
