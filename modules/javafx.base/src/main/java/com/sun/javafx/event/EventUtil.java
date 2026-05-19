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

package com.sun.javafx.event;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventTarget;

public final class EventUtil {
    private static final EventDispatchChainImpl eventDispatchChain =
            new EventDispatchChainImpl();

    private static final AtomicBoolean eventDispatchChainInUse =
            new AtomicBoolean();

    public static Event fireEvent(EventTarget eventTarget, Event event) {
        if (event.getTarget() != eventTarget) {
            event = event.copyFor(event.getSource(), eventTarget);
        }

        if (eventDispatchChainInUse.getAndSet(true)) {
            // the member event dispatch chain is in use currently, we need to
            // create a new instance for this call
            return fireEventImpl(new EventDispatchChainImpl(),
                                 eventTarget, event);
        }

        try {
            return fireEventImpl(eventDispatchChain, eventTarget, event);
        } finally {
            // need to do reset after use to remove references to event
            // dispatchers from the chain
            eventDispatchChain.reset();
            eventDispatchChainInUse.set(false);
        }
    }

    public static Event fireEvent(Event event, EventTarget... eventTargets) {
        return fireEventImpl(new EventDispatchTreeImpl(),
                             new CompositeEventTargetImpl(eventTargets),
                             event);
    }

    private static Event fireEventImpl(EventDispatchChain eventDispatchChain,
                                       EventTarget eventTarget,
                                       Event event) {
        final EventDispatchChain targetDispatchChain =
                eventTarget.buildEventDispatchChain(eventDispatchChain);
        return targetDispatchChain.dispatchEvent(event);
    }
}
