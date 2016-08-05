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

package javafx.scene.transform;

//import com.sun.javafx.event.EventTypeUtil;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * This event is fired on a transform when any of its properties changes.
 * @since JavaFX 8.0
 */
public final class TransformChangedEvent extends Event {

    private static final long serialVersionUID = 20121107L;

    /**
     * The only valid EventType for the TransformChangedEvent.
     */
    public static final EventType<TransformChangedEvent> TRANSFORM_CHANGED =
            new EventType(Event.ANY, "TRANSFORM_CHANGED");
// TODO: use EventTypeUtil once it's returned
//            EventTypeUtil.registerInternalEventType(Event.ANY, "TRANSFORM_CHANGED");

    /**
     * Common supertype for all transform changed event types.
     */
    public static final EventType<TransformChangedEvent> ANY =
            TRANSFORM_CHANGED;

    /**
     * Creates a new {@code TransformChangedEvent} with an event type
     * of {@code TRANSFORM_CHANGED}. The source and target of the event
     * is set to {@code NULL_SOURCE_TARGET}.
     */
    public TransformChangedEvent() {
        super(TRANSFORM_CHANGED);
    }

    /**
     * Construct a new {@code TransformChangedEvent} with the specified event
     * source and target. If the source or target is set to {@code null},
     * it is replaced by the {@code NULL_SOURCE_TARGET} value. All
     * TransformChangedEvents have their type set to
     * {@code TRANSFORM_CHANGED}.
     *
     * @param source    the event source which sent the event
     * @param target    the event target to associate with the event
     */
    public TransformChangedEvent(Object source, EventTarget target) {
        super(source, target, TRANSFORM_CHANGED);
    }
}
