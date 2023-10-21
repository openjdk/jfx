/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;

/**
 * Specifies the policy that is used to invoke an event handler, namely its invocation priority
 * and whether the event handler will be invoked for consumed events.
 *
 * @param priority the invocation priority
 * @param handleConsumedEvents {@code true} if the handler should be invoked for consumed events,
 *                             {@code false} otherwise
 * @since 22
 */
public record EventHandlerPolicy(EventHandlerPriority priority, boolean handleConsumedEvents) {

    public EventHandlerPolicy {
        Objects.requireNonNull(priority);
    }

    /**
     * The default policy is recommended for general-purpose event handlers.
     */
    public static final EventHandlerPolicy DEFAULT = new EventHandlerPolicy(EventHandlerPriority.DEFAULT, true);

    /**
     * The system policy is primarily useful for event handlers installed by controls
     * that should be overridable by application event handlers. Event handlers using
     * the system policy will never be invoked if the event was consumed by a default
     * handler.
     */
    public static final EventHandlerPolicy SYSTEM = new EventHandlerPolicy(EventHandlerPriority.SYSTEM, false);

}
