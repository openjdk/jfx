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
package javafx.scene.control.input;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;

/**
 * InputMap is a class that is set on a given {@link Node}. When the Node receives
 * an input event from the system, it passes this event in to the InputMap where
 * the InputMap can check all installed mappings to see if there is any
 * suitable mapping, and if so, fire the provided {@link EventHandler}.
 *
 * @param <N> The type of the Node that the InputMap is installed in.
 */
// TODO merge KeyMap into this
public class InputMap2<N extends Node> implements EventHandler<Event> {
    private final N node;

    public InputMap2(N node) {
        if (node == null) {
            throw new IllegalArgumentException("Node can not be null");
        }

        this.node = node;
    }
    
    /**
     * The Node for which this InputMap is attached.
     */
    public final N getNode() {
        return node;
    }
    
    @Override
    public void handle(Event ev) {
        if (ev == null || ev.isConsumed()) {
            return;
        }
        
        // TODO process functions first, then
        // TODO event handlers
    }
}
