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

/**
 * Specifies the order in which {@link EventHandler} implementations are invoked on an {@link EventTarget}.
 *
 * @implNote The order of the enumeration constants corresponds to the invocation order,
 *           ranging from lowest (invoked last) to highest (invoked first).
 *
 * @since 22
 */
public enum EventHandlerPriority {
    /**
     * The system priority is used for event handlers that should always be invoked last.
     * This is the lowest priority.
     */
    SYSTEM,

    /**
     * The default priority is used for event handlers that do not specify a priority.
     * Absent compelling reasons, applications should use this priority for general-purpose
     * event handlers.
     */
    DEFAULT,

    /**
     * The preferred priority is used for event handlers that are always invoked first.
     * This is the highest priority.
     */
    PREFERRED
}
