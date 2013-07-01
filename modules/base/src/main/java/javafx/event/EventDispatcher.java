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

package javafx.event;

// PENDING_DOC_REVIEW
/**
 * An {@code EventDispatcher} represents an event dispatching and processing
 * entity. It is used when an {@code Event} needs to be dispatched to the
 * associated {@code EventTarget} through the {@code EventDispatchChain}
 * specified by the target. Each {@code EventDispatcher} in the chain can
 * influence the event path and the event itself. One {@code EventDispatcher}
 * can appear in multiple chains.
 * <p>
 * The system defines two successive phases of event delivery. The first
 * phase is called capturing phase and happens when when an event travels from
 * the first element of the {@code EventDispatchChain} associated with the event
 * target to its last element. If the event target is part of some hierarchy,
 * the direction of the event in this phase usually corresponds with the
 * direction from the root element of the hierarchy to the target. The second
 * phase is called bubbling phase and happens in the reverse order to the first
 * phase. So the event is returning back from the last element of the
 * {@code EventDispatchChain} to its first element in this phase. Usually that
 * corresponds to the direction from the event target back to the root in the
 * event target's hierarchy.
 * <p>
 * Each {@code EventDispatcher} in an {@code EventDispatchChain} is responsible
 * for forwarding the event to the rest of the chain during event dispatching.
 * This forwarding happens in the {@code dispatchEvent} method and forms a chain
 * of nested calls which allows one {@code EventDispatcher} to see the event
 * during both dispatching phases in a single {@code dispatchEvent} call.
 * <p>
 * Template for {@code dispatchEvent} implementation.
<PRE>
public Event dispatchEvent(Event event, EventDispatchChain tail) {
    // capturing phase, can handle / modify / substitute / divert the event

    if (notHandledYet) {
        // forward the event to the rest of the chain
        event = tail.dispatchEvent(event);

        if (event != null) {
            // bubbling phase, can handle / modify / substitute / divert
            // the event
        }
    }

    return notHandledYet ? event : null;
</PRE>
}

 * @since JavaFX 2.0
 */
public interface EventDispatcher {
    /**
     * Dispatches the specified event by this {@code EventDispatcher}. Does
     * any required event processing. Both the event and its further path can
     * be modified in this method. If the event is not handled / consumed during
     * the capturing phase, it should be dispatched to the rest of the chain
     * ({@code event = tail.dispatch(event);}).
     *
     * @param event the event do dispatch
     * @param tail the rest of the chain to dispatch event to
     * @return the return event or {@code null} if the event has been handled /
     *      consumed
     */
    Event dispatchEvent(Event event, EventDispatchChain tail);
}
