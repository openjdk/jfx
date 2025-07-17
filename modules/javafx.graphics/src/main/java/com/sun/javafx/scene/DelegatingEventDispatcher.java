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

package com.sun.javafx.scene;

import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.scene.Node;

/**
 * Special-purpose event dispatcher that is used by {@link Node} to retarget an event to its focus delegate.
 */
public final class DelegatingEventDispatcher implements EventDispatcher {

    private final Node parent;
    private final Node delegate;
    private final EventDispatcher dispatcher;

    public DelegatingEventDispatcher(Node parent, Node delegate, EventDispatcher dispatcher) {
        this.parent = parent;
        this.delegate = delegate;
        this.dispatcher = dispatcher;
    }

    @Override
    public Event dispatchEvent(Event event, EventDispatchChain tail) {
        // Focus delegation is the only scenario in which the event target may be the parent node.
        // Since we are in the capturing phase, we need to retarget the event to the focus delegate.
        boolean retarget = event.getTarget() == parent;
        if (retarget) {
            event = event.copyFor(event.getSource(), delegate);
        }

        // Dispatch the event to the node's event dispatcher, or if the node doesn't have one,
        // directly to the rest of the event dispatch chain.
        if (dispatcher != null) {
            event = dispatcher.dispatchEvent(event, tail);
        } else {
            event = tail.dispatchEvent(event);
        }

        // The event was consumed, nothing left to do.
        if (event == null) {
            return null;
        }

        // Now we are in the bubbling phase. If we retargeted the capturing event earlier,
        // we now need to retarget the bubbling event back to its original target.
        return retarget ? event.copyFor(event.getSource(), parent) : event;
    }
}
