/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.traversal;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * An event for representing node traversals that occur within the scenegraph. Traversal events, like all
 * other events JavaFX, occur using the standard event filter / handling APIs. Therefore, to listen to
 * when a node is traversed, use code along the following lines:
 * <pre>{@code  Node node = ...;
 * node.addEventHandler(TraversalEvent.NODE_TRAVERSED, (ev) -> {
 *   // Use properties of the TraversalEvent to appropriately react to this event
 *   Node n = ev.getNode();
 *   Bounds b = ev.getBounds();
 * });}</pre>
 *
 * @see TraversalPolicy
 * @since 999 TODO
 */
public class TraversalEvent extends Event {

    private static final long serialVersionUID = 202407011641L;

    /**
     * Common supertype for all traversal event types.
     */
    public static final EventType<TraversalEvent> ANY = new EventType<> (Event.ANY, "ANY");

    /**
     * Traversal event type for representing that a node has been traversed.
     */
    public static final EventType<TraversalEvent> NODE_TRAVERSED = new EventType<> (ANY, "NODE_TRAVERSED");

    /**
     * The layout bounds of the node, transformed into the coordinates of the root element.
     */
    private final Bounds bounds;
    private transient final Node node;

    /**
     * Creates new instance of TraversalEvent.
     * @param node the {@link Node} which received the traversal event
     * @param bounds The layout bounds of the node, transformed into the coordinates of the root element in the
     *              traversal root being used (i.e. the {@link Scene} or the root {@link Parent})
     * @param eventType Type of the event
     */
    public TraversalEvent(
        final @NamedArg("node") Node node,
        final @NamedArg("bounds") Bounds bounds,
        final @NamedArg("eventType") EventType<? extends TraversalEvent> eventType
    ) {
        super(node, node, eventType);
        this.node = node;
        this.bounds = bounds;
    }

    @Override
    public EventType<? extends TraversalEvent> getEventType() {
        return (EventType<? extends TraversalEvent>) super.getEventType();
    }

    /**
     * The layout bounds of the node which received the traversal event,
     * transformed into the coordinates of the root element in the
     * traversal root being used (i.e. the {@link Scene} or the root {@link Parent}).
     *
     * @return the layout bounds of the node, transformed into the coordinates of the root element.
     */
    public final Bounds getBounds() {
        return bounds;
    }

    /**
     * Returns the {@link Node} which received the traversal event (same object as {@link Event#getTarget()}).
     * @return the Node
     */
    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        return 
            getClass().getName() + 
            "[node=" + getNode() +
            ", bounds=" + getBounds() +
            ", source=" + source +
            "]";
    }
}