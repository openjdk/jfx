/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * An event that signals the creation, beginning, completion and cancellation of transitions.
 *
 * @since 20
 */
public class TransitionEvent extends Event {

    private static final long serialVersionUID = 20220820L;

    /**
     * Common supertype for all {@code TransitionEvent} types.
     */
    public static final EventType<TransitionEvent> ANY =
            new EventType<>(Event.ANY, "TRANSITION");

    /**
     * This event occurs when a transition has been created and added to the list of running
     * transitions of a {@link Node}.
     */
    public static final EventType<TransitionEvent> RUN =
            new EventType<>(TransitionEvent.ANY, "TRANSITION_RUN");

    /**
     * This event occurs when a running transition enters its active period, which happens
     * at the end of the delay phase.
     */
    public static final EventType<TransitionEvent> START =
            new EventType<>(TransitionEvent.ANY, "TRANSITION_START");

    /**
     * This event occurs when a running transition has reached the end of its active period.
     * If the transition is cancelled prior to reaching the end of its active period, this
     * event does not occur.
     */
    public static final EventType<TransitionEvent> END =
            new EventType<>(TransitionEvent.ANY, "TRANSITION_END");

    /**
     * This event occurs when a running transition was cancelled before it has reached the
     * end of its active period.
     */
    public static final EventType<TransitionEvent> CANCEL =
            new EventType<>(TransitionEvent.ANY, "TRANSITION_CANCEL");

    private final StyleableProperty<?> property;
    private final Duration elapsedTime;

    /**
     * Creates a new instance of the {@code TransitionEvent} class.
     *
     * @param eventType the event type
     * @param property the {@code StyleableProperty} that is targeted by the transition
     * @param elapsedTime the time that has elapsed since the transition has entered its active period
     */
    public TransitionEvent(EventType<? extends Event> eventType,
                           StyleableProperty<?> property,
                           Duration elapsedTime) {
        super(eventType);
        this.property = property;
        this.elapsedTime = elapsedTime;
    }

    /**
     * Gets the {@code StyleableProperty} that is targeted by the transition.
     *
     * @return the {@code StyleableProperty}
     */
    public final StyleableProperty<?> getProperty() {
        return property;
    }

    /**
     * Gets the time that has elapsed since the transition has entered its active period,
     * not including the time spent in the delay phase.
     *
     * @return the elapsed time
     */
    public final Duration getElapsedTime() {
        return elapsedTime;
    }

}
