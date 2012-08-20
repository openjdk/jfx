/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.input;

import com.sun.javafx.event.EventTypeUtil;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Mouse drag events are delivered to potential gesture targets during
 * full press-drag-release gestures. The difference among different
 * gesture types is described at {@link javafx.scene.input.MouseEvent MouseEvent}.
 * <p>
 * Full press-drag-release gesture can be started by calling
 * {@code startFullDrag()} (on a node or scene) inside of a DRAG_DETECTED
 * event handler. This call activates delivering of {@code MouseDragEvent}s
 * to the nodes that are under cursor during the dragging gesture.
 * <p>
 * When you drag a node, it's still under cursor, so it is considered
 * being a potential gesture target during the whole gesture. If you need to
 * drag a node to a different node and let the other node know about it,
 * you need to ensure that the nodes under the dragged node are picked
 * as the potential gesture targets. You can achieve this by calling
 * {@code setMouseTransparent(true)} on the dragged node in a
 * {@code MOUSE_PRESSED} handler and returning it back to false in a
 * {@code MOUSE_RELEASED} handler. This way the nodes under the dragged
 * node will receive the {@code MouseDragEvent}s, while all the
 * {@code MouseEvent}s will still be delivered to the (currently mouse
 * transparent) gesture source.
 * <p>
 * The entered/exited events behave similarly to mouse entered/exited
 * events, please see {@link MouseEvent} overview.
 */
public class MouseDragEvent extends MouseEvent {

    /**
     * Common supertype for all mouse event types.
     */
    public static final EventType<MouseDragEvent> ANY =
            new EventType<MouseDragEvent>(MouseEvent.ANY, "MOUSE-DRAG");

    /**
     * This event occurs when the gesture progresses within this node.
     */
    public static final EventType<MouseDragEvent> MOUSE_DRAG_OVER =
            new EventType<MouseDragEvent>(MouseDragEvent.ANY, "MOUSE-DRAG_OVER");

    /**
     * This event occurs when the gesture ends (by releasing mouse button)
     * on this node.
     */
    public static final EventType<MouseDragEvent> MOUSE_DRAG_RELEASED =
            new EventType<MouseDragEvent>(MouseDragEvent.ANY, "MOUSE-DRAG_RELEASED");

    /**
     * This event occurs when the gesture enters a node. It's the bubbling variant,
     * which is delivered also to all parents of the entered node (unless it
     * was consumed). When notifications about mouse entering some of node's
     * children are not desired, {@code MOUSE_DRAG_ENTERED} event handler should
     * be used.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     * which is similar
     */
    public static final EventType<MouseDragEvent> MOUSE_DRAG_ENTERED_TARGET =
            new EventType<MouseDragEvent>(MouseDragEvent.ANY, "MOUSE-DRAG_ENTERED_TARGET");

    /**
     * This event occurs when the gesture enters a node. This event type is
     * delivered only to the entered node, if parents want to filter it or get
     * the bubbling event, they need to use {@code MOUSE_DRAG_ENTERED_TARGET}.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     * which is similar
     */
    public static final EventType<MouseDragEvent> MOUSE_DRAG_ENTERED =
            new EventType<MouseDragEvent>(MouseDragEvent.MOUSE_DRAG_ENTERED_TARGET,
                    "MOUSE-DRAG_ENTERED");

    /**
     * This event occurs when the gesture exits a node. It's the bubbling variant,
     * which is delivered also to all parents of the exited node (unless it
     * was consumed). When notifications about mouse exiting some of node's
     * children are not desired, {@code MOUSE_DRAG_EXITED} event handler should
     * be used.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     * which is similar
     */
    public static final EventType<MouseDragEvent> MOUSE_DRAG_EXITED_TARGET =
            new EventType<MouseDragEvent>(MouseDragEvent.ANY, "MOUSE-DRAG_EXITED_TARGET");

    /**
     * This event occurs when the gesture exits a node. This event type is
     * delivered only to the exited node, if parents want to filter it or get
     * the bubbling event, they need to use {@code MOUSE_DRAG_EXITED_TARGET}.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     * which is similar
     */
    public static final EventType<MouseDragEvent> MOUSE_DRAG_EXITED =
            new EventType<MouseDragEvent>(MouseDragEvent.MOUSE_DRAG_EXITED_TARGET,
                    "MOUSE-DRAG_EXITED");

    private MouseDragEvent(Object source, EventTarget target,
            EventType<? extends MouseEvent> eventType) {
        super(source, target, eventType);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static MouseEvent impl_copy(Object source, EventTarget target,
            Object gestureSource, MouseEvent evt,
            EventType<? extends MouseEvent> eventType) {

        MouseDragEvent copyEvent = new MouseDragEvent(source, target, eventType);
        MouseEvent.copyFields(evt, copyEvent, source, target);
        copyEvent.gestureSource = gestureSource;

        return copyEvent;
    }

    private transient Object gestureSource;

    /**
     * Returns the source object of the ongoing gesture.
     * Gesture source is the object that started the full press-drag-release
     * gesture (by {@code startFullDrag} method being called on it).
     * @return The source object of the gesture.
     */
    public Object getGestureSource() {
        return gestureSource;
    }
}
