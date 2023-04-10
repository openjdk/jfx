/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

/**
 * An {@link Event} representing some type of action. This event type is widely
 * used to represent a variety of things, such as when a {@link javafx.scene.control.Button}
 * has been fired, when a {@link javafx.animation.KeyFrame} has finished, and other
 * such usages.
 * @since JavaFX 2.0
 */
public class ActionEvent extends Event {

    private static final long serialVersionUID = 20121107L;
    /**
     * The only valid EventType for the ActionEvent.
     */
    public static final EventType<ActionEvent> ACTION =
            new EventType<>(Event.ANY, "ACTION");

    /**
     * Common supertype for all action event types.
     * @since JavaFX 8.0
     */
    public static final EventType<ActionEvent> ANY = ACTION;

    /**
     * Creates a new {@code ActionEvent} with an event type of {@code ACTION}.
     * The source and target of the event is set to {@code NULL_SOURCE_TARGET}.
     */
    public ActionEvent() {
        super(ACTION);
    }

    /**
     * Construct a new {@code ActionEvent} with the specified event source and target.
     * If the source or target is set to {@code null}, it is replaced by the
     * {@code NULL_SOURCE_TARGET} value. All ActionEvents have their type set to
     * {@code ACTION}.
     *
     * @param source    the event source which sent the event
     * @param target    the event target to associate with the event
     */
    public ActionEvent(Object source, EventTarget target) {
        super(source, target, ACTION);
    }

    @Override
    public ActionEvent copyFor(Object newSource, EventTarget newTarget) {
        return (ActionEvent) super.copyFor(newSource, newTarget);
    }

    @Override
    public EventType<? extends ActionEvent> getEventType() {
        return (EventType<? extends ActionEvent>) super.getEventType();
    }



}
