/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.NamedArg;
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
 * @since JavaFX 2.1
 */
public final class MouseDragEvent extends MouseEvent{

    private static final long serialVersionUID = 20121107L;

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

    /**
     * Constructs new MouseDragEvent event.
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param button the mouse button used
     * @param clickCount number of click counts
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @param primaryButtonDown true if primary button was pressed.
     * @param middleButtonDown true if middle button was pressed.
     * @param secondaryButtonDown true if secondary button was pressed.
     * @param synthesized if this event was synthesized
     * @param popupTrigger whether this event denotes a popup trigger for current platform
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates and target
     * @param gestureSource source object of the ongoing gesture.
     * @since JavaFX 8.0
     */
    public MouseDragEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target, @NamedArg("eventType") EventType<MouseDragEvent> eventType,
            @NamedArg("x") double x, @NamedArg("y") double y, @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("button") MouseButton button, @NamedArg("clickCount") int clickCount,
            @NamedArg("shiftDown") boolean shiftDown, @NamedArg("controlDown") boolean controlDown, @NamedArg("altDown") boolean altDown, @NamedArg("metaDown") boolean metaDown,
            @NamedArg("primaryButtonDown") boolean primaryButtonDown, @NamedArg("middleButtonDown") boolean middleButtonDown, @NamedArg("secondaryButtonDown") boolean secondaryButtonDown,
            @NamedArg("synthesized") boolean synthesized, @NamedArg("popupTrigger") boolean popupTrigger, @NamedArg("pickResult") PickResult pickResult,
            @NamedArg("gestureSource") Object gestureSource) {
        super(source, target, eventType, x, y, screenX, screenY, button,
                clickCount, shiftDown, controlDown, altDown, metaDown,
                primaryButtonDown, middleButtonDown, secondaryButtonDown,
                synthesized, popupTrigger, false, pickResult);
        this.gestureSource = gestureSource;
    }

    /**
     * Constructs new MouseDragEvent event with null source and target.
     *
     * @param eventType The type of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param button the mouse button used
     * @param clickCount number of click counts
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @param primaryButtonDown true if primary button was pressed.
     * @param middleButtonDown true if middle button was pressed.
     * @param secondaryButtonDown true if secondary button was pressed.
     * @param synthesized if this event was synthesized
     * @param popupTrigger whether this event denotes a popup trigger for current platform
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates
     * @param gestureSource source object of the ongoing gesture.
     * @since JavaFX 8.0
     */
    public MouseDragEvent(@NamedArg("eventType") EventType<MouseDragEvent> eventType,
            @NamedArg("x") double x, @NamedArg("y") double y, @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("button") MouseButton button, @NamedArg("clickCount") int clickCount,
            @NamedArg("shiftDown") boolean shiftDown, @NamedArg("controlDown") boolean controlDown, @NamedArg("altDown") boolean altDown, @NamedArg("metaDown") boolean metaDown,
            @NamedArg("primaryButtonDown") boolean primaryButtonDown, @NamedArg("middleButtonDown") boolean middleButtonDown, @NamedArg("secondaryButtonDown") boolean secondaryButtonDown,
            @NamedArg("synthesized") boolean synthesized, @NamedArg("popupTrigger") boolean popupTrigger, @NamedArg("pickResult") PickResult pickResult,
            @NamedArg("gestureSource") Object gestureSource) {
        this(null, null, eventType, x, y, screenX, screenY, button, clickCount,
                shiftDown, controlDown, altDown, metaDown, primaryButtonDown,
                middleButtonDown, secondaryButtonDown, synthesized, popupTrigger,
                pickResult, gestureSource);
     }


    private final transient Object gestureSource;

    /**
     * Returns the source object of the ongoing gesture.
     * Gesture source is the object that started the full press-drag-release
     * gesture (by {@code startFullDrag} method being called on it).
     * @return The source object of the gesture.
     */
    public Object getGestureSource() {
        return gestureSource;
    }

    /**
     * Returns a string representation of this {@code MouseDragEvent} object.
     * @return a string representation of this {@code MouseDragEvent} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("MouseDragEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", gestureSource = ").append(getGestureSource());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        sb.append(", x = ").append(getX()).append(", y = ").append(getY())
                .append(", z = ").append(getZ());

        if (getButton() != null) {
            sb.append(", button = ").append(getButton());
        }
        if (getClickCount() > 1) {
            sb.append(", clickCount = ").append(getClickCount());
        }
        if (isPrimaryButtonDown()) {
            sb.append(", primaryButtonDown");
        }
        if (isMiddleButtonDown()) {
            sb.append(", middleButtonDown");
        }
        if (isSecondaryButtonDown()) {
            sb.append(", secondaryButtonDown");
        }
        if (isShiftDown()) {
            sb.append(", shiftDown");
        }
        if (isControlDown()) {
            sb.append(", controlDown");
        }
        if (isAltDown()) {
            sb.append(", altDown");
        }
        if (isMetaDown()) {
            sb.append(", metaDown");
        }
        if (isShortcutDown()) {
            sb.append(", shortcutDown");
        }
        if (isSynthesized()) {
            sb.append(", synthesized");
        }
        sb.append(", pickResult = ").append(getPickResult());

        return sb.append("]").toString();
    }

    @Override
    public MouseDragEvent copyFor(Object newSource, EventTarget newTarget) {
        return (MouseDragEvent) super.copyFor(newSource, newTarget);
    }

    @Override
    public MouseDragEvent copyFor(Object newSource, EventTarget newTarget, EventType<? extends MouseEvent> type) {
        return (MouseDragEvent) super.copyFor(newSource, newTarget, type);
    }

    @Override
    public EventType<MouseDragEvent> getEventType() {
        return (EventType<MouseDragEvent>) super.getEventType();
    }
}
