/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
 * Specifies how an {@link EventHandler} registered on an {@link EventTarget} participates in event delivery.
 * <p>
 * JavaFX delivers {@linkplain Event events} to event targets by invoking the event handlers registered
 * on an event target. For each event target, event handlers are processed in two groups:
 * <ol>
 *   <li>Primary handlers are invoked first.
 *   <li>Default handlers are invoked if no primary handler {@linkplain Event#consume() consumed}
 *       the event or {@linkplain Event#preventDefault() prevented} default handling.
 * </ol>
 *
 * @since 27
 */
public enum EventHandlerPriority {

    /**
     * A handler that participates in normal event processing for a given {@link EventTarget}.
     * <p>
     * Primary handlers are invoked before {@linkplain #DEFAULT default} handlers on each event target.
     * They may call {@link Event#consume()} to stop further propagation, or call {@link Event#preventDefault()}
     * to suppress default handling for the current event target while allowing propagation to continue.
     * <p>
     * Primary handlers are typically used by application code.
     */
    PRIMARY,

    /**
     * A handler representing a default action for a given {@link EventTarget}.
     * <p>
     * Default handlers are invoked after {@linkplain #PRIMARY primary} handlers on each event target, but only
     * if the event is still eligible for default handling (it has not been {@link Event#consume() consumed}
     * and default handling has not been {@linkplain Event#preventDefault() prevented} for the event target).
     * <p>
     * Default handlers may call {@link Event#consume()} to stop further propagation.
     * Calling {@link Event#preventDefault()} during default handling has no effect.
     * <p>
     * The main use case for default handlers is control skins and behaviors that provide default actions
     * that only run when user code did not claim the event.
     */
    DEFAULT
}
