/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

import com.sun.javafx.scene.input.InputEventUtils;
import com.sun.javafx.tk.Toolkit;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventDispatcher;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Scene;

/// An event originating by mouse buttons and movements. Mouse events can generally be categorized into button events,
/// movement events, and drag events (resulting from the combination of the previous two). These are described below in
/// more detail. [ScrollEvent]s produced by the mouse wheel are not `MouseEvent`s (but events produced by using the
/// scroll wheel as a button are).
///
/// The buttons involved in any event can be queried with [#isPrimaryButtonDown()], [#isSecondaryButtonDown()] etc. and
/// [#getClickCount()]. Modifiers can be queried with [#isShiftDown()], [#isControlDown()] etc.
///
/// The mouse (pointer's) location is available in several coordinate systems:
/// * ([x][#getX()], [y][#getY()], [z][#getZ()]) - relative to the origin of the `MouseEvent`'s node.
/// * ([sceneX][#getSceneX()], [sceneY][#getSceneY()]) - relative to to the origin of the `Scene` that contains the node.
/// * ([screenX][#getScreenX()], [screenY][#getScreenY()]) - relative to the origin of the screen that contains the mouse
///   pointer.
///
/// When a mouse event occurs, the top-most node under the cursor is [picked][#getPickResult()] and the event is
/// delivered to it through the capturing and bubbling phases described in [EventDispatcher]. Nodes that have
/// [mouseTransparent][Node#mouseTransparentProperty()] set to `true` do not receive mouse events.
///
/// ## Button events
/// A mouse button can be [pressed][#MOUSE_PRESSED] and [released][#MOUSE_RELEASED]. A button [click][#MOUSE_CLICKED]
/// occurs after press and release happen over the same node. Button events can still be produced during a drag gesture.
///
/// Not all buttons on the mouse are supported, such as macro buttons or buttons that change the DPI.
///
/// ## Dragging gestures
/// There are 3 types of drag gestures that can occur:
///
/// 1. **Simple press-drag-release (PDR)**. When a node receives a `MOUSE_PRESSED` event and then the mouse is moved,
/// it starts receiving [#MOUSE_DRAGGED] events (instead of [#MOUSE_MOVED] events). The node receives all the
/// `MouseEvent`s during this gesture, including button events, even if they occur over other nodes.
/// PDR is best used for actions that don't interact with other nodes, such as changing the size of a shape, dragging it,
/// rotating it etc.
///
/// If the mouse is dragged outside of its [hysteresis][#isStillSincePress()] area, a [#DRAG_DETECTED] event is
/// dispatched to the source node. The other 2 drag gesture types can be started in its
/// [handler][Node#onDragDetectedProperty()].
///
/// 2. **Full PDR**. This gesture starts when the [startFullDrag][Node#startFullDrag()] method of a node (or a
/// [scene][Scene#startFullDrag()]) is invoked in the `DRAG_DETECTED` handler. During this gestures, nodes other than
/// the source node receive [MouseDragEvent]s when they are picked according to the cursor location.
/// Full PDR is best used for actions that require interactions between nodes, such as connecting nodes by "wires",
/// dragging nodes to other nodes etc.
/// Note that a simple PDR gesture continues alongside this gesture. If `node1` starts a full PDR while its simple PDR
/// is ongoing, and then the mouse moves over `node2`, then `node1` will continue receiving `MOUSE_DRAGGED` events while
/// `node2` will receive `MOUSE_DRAG_OVER` events.
///
/// 3. **Drag-and-drop (DnD)**. This gestures starts when the [startDragAndDrop][Node#startDragAndDrop(TransferMode...)]
/// method of a node (or a [scene][Scene#startDragAndDrop(TransferMode...)]) is invoked in the `DRAG_DETECTED` handler.
/// This is the platform-supported drag-and-drop. This gesture interacts with both operating system applications and FX
/// applications. During this gestures, nodes in all FX applications receive [DragEvent]s.
/// DnD serves best to transfer data between applications, such as to move/copy text between a `TextArea` and a system's
/// notepad, to transfer files between applications etc.
/// A DnD gesture does not allow PDR or full PDR gestures to run alongside it; it takes precedence over full-PDR if
/// both `startFullDrag` and `startFullDrag` are called.
///
/// Dragging ends when a mouse button is released. Dragging a finger over touch screens produces both drag events and
/// scroll events. The [#isSynthesized()] method can be used to differentiate between these events.
///
/// The following flow diagram shows the stages of event delivery depending on the type of drag gesture:
///
/// <img src="doc-files/mouse_events.svg" alt="Drag mouse events flow" width="700">
///
/// The following table summarizes the different drag gestures:
///
/// <table border="1">
/// <caption>Drag Gestures Summary Table</caption>
///   <tr>
///     <th scope="col">Name</th>
///     <th scope="col">Involved</th>
///     <th scope="col">Usage example</th>
///     <th scope="col">Start method</th>
///     <th scope="col">Event class</th>
///   </tr>
///   <tr>
///     <th scope="row">PDR</th>
///     <td>Source node/scene</td>
///     <td>Resize or move a shape</td>
///     <td></td>
///     <td>{@code MouseEvent}</td>
///   </tr>
///   <tr>
///     <th scope="row">Full PDR</th>
///     <td>Any node/scene</td>
///     <td>Connecting nodes or dropping a node on another</td>
///     <td>{@code startFullDrag}</td>
///     <td>{@code MouseDragEvent}</td>
///   </tr>
///   <tr>
///     <th scope="row">DnD</th>
///     <td>Any node/scene and OS applications</td>
///     <td>Copy/move text or files between applications</td>
///     <td>{@code startDragAndDrop}</td>
///     <td>{@code DragEvent}</td>
///   </tr>
/// </table>
///
/// ## Movement events
/// When the mouse moves while a drag gesture is not ongoing, it produces [#MOUSE_MOVED] events.
///
/// When the mouse enters/exits a node while DnD is not ongoing, the node gets a [#MOUSE_ENTERED]/[#MOUSE_EXITED] event.
/// These events are delivered only to the entered/exited node and don't go through the capturing/bubbling phases.
/// Conversely, [#MOUSE_ENTERED_TARGET]/[#MOUSE_EXITED_TARGET] events do go through capturing/bubbling phases normally.
/// This means that a node can receive these events when the mouse enters a node in its scenegraph hierarchy. To
/// distinguish between these two cases, the event target can be tested on equality with the node.
///
/// Since `MOUSE_ENTERED`/`MOUSE_EXITED` are subtypes of `MOUSE_ENTERED_TARGET`/`MOUSE_EXITED_TARGET`, they are also
/// handled by `MOUSE_ENTERED_TARGET`/`MOUSE_EXITED_TARGET` event handlers. In fact, a single event changes its type
/// during its lifetime in the following way:
/// 1. During capturing phase, the event is delivered to the parents with the `MOUSE_ENTERED_TARGET` type.
/// 2. When the event is delivered to the event target (the node that was entered), its type is switched to
/// `MOUSE_ENTERED`.
/// 3. During the bubbling phase, its type is switched back to `MOUSE_ENTERED_TARGET`.
///
/// If the event is mutated or [consumed][Event#consume()], it affects both event types.
///
/// @see ContextMenuEvent ContextMenuEvent for triggering context menus
/// @since JavaFX 2.0
public class MouseEvent extends InputEvent {

    private static final long serialVersionUID = 20121107L;

    /**
     * Common supertype for all mouse event types.
     */
    public static final EventType<MouseEvent> ANY =
            new EventType<>(InputEvent.ANY, "MOUSE");

    /**
     * This event occurs when a mouse button is pressed. This activates a
     * press-drag-release gesture, so all subsequent mouse events until
     * the button is released are delivered to the same node.
     */
    public static final EventType<MouseEvent> MOUSE_PRESSED =
            new EventType<>(MouseEvent.ANY, "MOUSE_PRESSED");

    /**
     * This event occurs when a mouse button is released. It is delivered
     * to the same node where the button has been pressed which activated
     * a press-drag-release gesture.
     */
    public static final EventType<MouseEvent> MOUSE_RELEASED =
            new EventType<>(MouseEvent.ANY, "MOUSE_RELEASED");

    /**
     * This event occurs when a mouse button has been clicked (pressed and
     * released on the same node). This event provides a button-like behavior
     * to any node. Note that even long drags can generate click event (it
     * is delivered to the top-most node on which the mouse was both
     * pressed and released).
     */
    public static final EventType<MouseEvent> MOUSE_CLICKED =
            new EventType<>(MouseEvent.ANY, "MOUSE_CLICKED");

    /**
     * This event occurs when the mouse enters a node. It's the bubbling variant,
     * which is delivered also to all parents of the entered node (unless it
     * was consumed). When notifications about mouse entering some of node's
     * children are not desired, {@code MOUSE_ENTERED} event handler should
     * be used.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     */
    public static final EventType<MouseEvent> MOUSE_ENTERED_TARGET =
            new EventType<>(MouseEvent.ANY, "MOUSE_ENTERED_TARGET");

    /**
     * This event occurs when the mouse enters a node. This event type is delivered
     * only to the entered node, if parents want to filter it or get the
     * bubbling event, they need to use {@code MOUSE_ENTERED_TARGET}.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     */
    public static final EventType<MouseEvent> MOUSE_ENTERED =
            new EventType<>(MouseEvent.MOUSE_ENTERED_TARGET, "MOUSE_ENTERED");

    /**
     * This event occurs when the mouse exits a node. It's the bubbling variant,
     * which is delivered also to all parents of the exited node (unless it
     * was consumed). When notifications about mouse exiting some of node's
     * children are not desired, {@code MOUSE_EXITED} event handler should
     * be used.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     */
    public static final EventType<MouseEvent> MOUSE_EXITED_TARGET =
            new EventType<>(MouseEvent.ANY, "MOUSE_EXITED_TARGET");

    /**
     * This event occurs when the mouse exits a node. This event type is delivered
     * only to the exited node, if parents want to filter it or get the
     * bubbling event, they need to use {@code MOUSE_EXITED_TARGET}.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     */
    public static final EventType<MouseEvent> MOUSE_EXITED =
            new EventType<>(MouseEvent.MOUSE_EXITED_TARGET, "MOUSE_EXITED");

    /**
     * This event occurs when the mouse moves within a node and no buttons
     * are pressed. If any mouse button is pressed, MOUSE_DRAGGED event
     * occurs instead.
     */
    public static final EventType<MouseEvent> MOUSE_MOVED =
            new EventType<>(MouseEvent.ANY, "MOUSE_MOVED");

    /**
     * This event occurs when the mouse moves with a pressed button.
     * It is delivered to the same node where the button has been pressed
     * which activated a press-drag-release gesture. It is delivered
     * regardless of the mouse being within bounds of the node.
     */
    public static final EventType<MouseEvent> MOUSE_DRAGGED =
            new EventType<>(MouseEvent.ANY, "MOUSE_DRAGGED");

    /**
     * This event is delivered to a node that is identified as a source of a
     * dragging gesture. Handler of this event is the only place where
     * full press-drag-release gesture or a drag and drop gesture can be
     * started (by calling {@link javafx.scene.Node#startFullDrag startFullDrag()}
     * or {@link javafx.scene.Node#startDragAndDrop startDragAndDrop()} method).
     * If none of them is called, simple press-drag-release gesture will continue.
     * <p>
     * Note that his event is generated based on dragging the mouse over a
     * platform-specific distance threshold. You can modify this behavior
     * by calling {@code setDragDetect} method on any MOUSE_PRESSED or
     * MOUSE_DRAGGED event.
     *
     * @see MouseEvent MouseEvent for more details about simple press-drag-release gestures
     * @see MouseDragEvent MouseDragEvent for more details about full press-drag-release gestures
     * @see DragEvent DragEvent for more details about drag and drop gestures
     */
    public static final EventType<MouseEvent> DRAG_DETECTED =
            new EventType<>(MouseEvent.ANY, "DRAG_DETECTED");

    /**
     * Fills the given event by this event's coordinates recomputed to the given
     * source object
     * @param newEvent Event whose coordinates are to be filled
     * @param newSource Source object to compute coordinates for
     */
    void recomputeCoordinatesToSource(MouseEvent oldEvent, Object newSource) {

        final Point3D newCoordinates = InputEventUtils.recomputeCoordinates(
                pickResult, newSource);

        x = newCoordinates.getX();
        y = newCoordinates.getY();
        z = newCoordinates.getZ();
    }

    @Override
    public EventType<? extends MouseEvent> getEventType() {
        return (EventType<? extends MouseEvent>) super.getEventType();
    }

    /**
     * Copies this event for a different source and target.
     * In most cases you don't need to use this method, it's called
     * automatically when you fire the event.
     * @param newSource New event source
     * @param newTarget New event target
     * @return copy of this event for a different source and target
     */
    @Override
    public MouseEvent copyFor(Object newSource, EventTarget newTarget) {
        MouseEvent e = (MouseEvent) super.copyFor(newSource, newTarget);
        e.recomputeCoordinatesToSource(this, newSource);
        return e;
    }

    /**
     * Creates a copy of the given event with the given fields substituted.
     * @param newSource the new source of the copied event
     * @param newTarget the new target of the copied event
     * @param eventType the new eventType
     * @return the event copy with the fields substituted
     * @since JavaFX 8.0
     */
    public MouseEvent copyFor(Object newSource, EventTarget newTarget, EventType<? extends MouseEvent> eventType) {
        MouseEvent e = copyFor(newSource, newTarget);
        e.eventType = eventType;
        return e;
    }

    /**
     * Constructs new MouseEvent event with null source and target.
     *
     * Both {@link #isBackButtonDown()}  and {@link #isForwardButtonDown()}
     * will return {@literal false} for this event.
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
     * @param stillSincePress see {@link #isStillSincePress() }
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates
     * @since JavaFX 8.0
     */
    public MouseEvent(
            @NamedArg("eventType") EventType<? extends MouseEvent> eventType,
            @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("button") MouseButton button,
            @NamedArg("clickCount") int clickCount,
            @NamedArg("shiftDown") boolean shiftDown,
            @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown,
            @NamedArg("metaDown") boolean metaDown,
            @NamedArg("primaryButtonDown") boolean primaryButtonDown,
            @NamedArg("middleButtonDown") boolean middleButtonDown,
            @NamedArg("secondaryButtonDown") boolean secondaryButtonDown,
            @NamedArg("synthesized") boolean synthesized,
            @NamedArg("popupTrigger") boolean popupTrigger,
            @NamedArg("stillSincePress") boolean stillSincePress,
            @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, button, clickCount,
                shiftDown, controlDown, altDown, metaDown,
                primaryButtonDown, middleButtonDown, secondaryButtonDown,
                synthesized, popupTrigger, stillSincePress, pickResult);
    }

    /**
     * Constructs new MouseEvent event with null source and target.
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
     * @param backButtonDown true if back button was pressed.
     * @param forwardButtonDown true if forward button was pressed
     * @param synthesized if this event was synthesized
     * @param popupTrigger whether this event denotes a popup trigger for current platform
     * @param stillSincePress see {@link #isStillSincePress() }
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates
     * @since 12
     */
    public MouseEvent(
            @NamedArg("eventType") EventType<? extends MouseEvent> eventType,
            @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("button") MouseButton button,
            @NamedArg("clickCount") int clickCount,
            @NamedArg("shiftDown") boolean shiftDown,
            @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown,
            @NamedArg("metaDown") boolean metaDown,
            @NamedArg("primaryButtonDown") boolean primaryButtonDown,
            @NamedArg("middleButtonDown") boolean middleButtonDown,
            @NamedArg("secondaryButtonDown") boolean secondaryButtonDown,
            @NamedArg("backButtonDown") boolean backButtonDown,
            @NamedArg("forwardButtonDown") boolean forwardButtonDown,
            @NamedArg("synthesized") boolean synthesized,
            @NamedArg("popupTrigger") boolean popupTrigger,
            @NamedArg("stillSincePress") boolean stillSincePress,
            @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, button, clickCount,
                shiftDown, controlDown, altDown, metaDown,
                primaryButtonDown, middleButtonDown, secondaryButtonDown,
                backButtonDown, forwardButtonDown,
                synthesized, popupTrigger, stillSincePress, pickResult);
    }

    /**
     * Constructs new MouseEvent event.
     *
     * Both {@link #isBackButtonDown()}  and {@link #isForwardButtonDown()}
     * will return {@literal false} for this event.
     *
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param x The x with respect to the source. Should be in scene coordinates if source == null or source is not a Node.
     * @param y The y with respect to the source. Should be in scene coordinates if source == null or source is not a Node.
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
     * @param stillSincePress see {@link #isStillSincePress() }
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates and target
     * @since JavaFX 8.0
     */
    public MouseEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
            @NamedArg("eventType") EventType<? extends MouseEvent> eventType,
            @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("button") MouseButton button,
            @NamedArg("clickCount") int clickCount,
            @NamedArg("shiftDown") boolean shiftDown,
            @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown,
            @NamedArg("metaDown") boolean metaDown,
            @NamedArg("primaryButtonDown") boolean primaryButtonDown,
            @NamedArg("middleButtonDown") boolean middleButtonDown,
            @NamedArg("secondaryButtonDown") boolean secondaryButtonDown,
            @NamedArg("synthesized") boolean synthesized,
            @NamedArg("popupTrigger") boolean popupTrigger,
            @NamedArg("stillSincePress") boolean stillSincePress,
            @NamedArg("pickResult") PickResult pickResult) {
        this(source, target, eventType, x, y, screenX, screenY, button, clickCount,
                shiftDown, controlDown, altDown, metaDown,
                primaryButtonDown, middleButtonDown, secondaryButtonDown, false, false,
                synthesized, popupTrigger, stillSincePress, pickResult);
    }

    /**
     * Constructs new MouseEvent event.
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param x The x with respect to the source. Should be in scene coordinates if source == null or source is not a Node.
     * @param y The y with respect to the source. Should be in scene coordinates if source == null or source is not a Node.
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
     * @param backButtonDown true if the back button was pressed
     * @param forwardButtonDown true if the forward button was pressed
     * @param synthesized if this event was synthesized
     * @param popupTrigger whether this event denotes a popup trigger for current platform
     * @param stillSincePress see {@link #isStillSincePress() }
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates and target
     * @since 12
     */
    public MouseEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
                      @NamedArg("eventType") EventType<? extends MouseEvent> eventType,
                      @NamedArg("x") double x, @NamedArg("y") double y,
                      @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
                      @NamedArg("button") MouseButton button,
                      @NamedArg("clickCount") int clickCount,
                      @NamedArg("shiftDown") boolean shiftDown,
                      @NamedArg("controlDown") boolean controlDown,
                      @NamedArg("altDown") boolean altDown,
                      @NamedArg("metaDown") boolean metaDown,
                      @NamedArg("primaryButtonDown") boolean primaryButtonDown,
                      @NamedArg("middleButtonDown") boolean middleButtonDown,
                      @NamedArg("secondaryButtonDown") boolean secondaryButtonDown,
                      @NamedArg("backButtonDown") boolean backButtonDown,
                      @NamedArg("forwardButtonDown") boolean forwardButtonDown,
                      @NamedArg("synthesized") boolean synthesized,
                      @NamedArg("popupTrigger") boolean popupTrigger,
                      @NamedArg("stillSincePress") boolean stillSincePress,
                      @NamedArg("pickResult") PickResult pickResult) {
        super(source, target, eventType);
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.sceneX = x;
        this.sceneY = y;
        this.button = button;
        this.clickCount = clickCount;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.altDown = altDown;
        this.metaDown = metaDown;
        this.primaryButtonDown = primaryButtonDown;
        this.middleButtonDown = middleButtonDown;
        this.secondaryButtonDown = secondaryButtonDown;
        this.backButtonDown = backButtonDown;
        this.forwardButtonDown = forwardButtonDown;
        this.synthesized = synthesized;
        this.stillSincePress = stillSincePress;
        this.popupTrigger = popupTrigger;
        this.pickResult = pickResult;
        this.pickResult = pickResult != null ? pickResult : new PickResult(target, x, y);
        final Point3D p = InputEventUtils.recomputeCoordinates(this.pickResult, null);
        this.x = p.getX();
        this.y = p.getY();
        this.z = p.getZ();
    }

    /**
     * Creates a copy of this mouse event of MouseDragEvent type
     * @param e the mouse event to copy
     * @param source the new source of the copied event
     * @param target the new target of the copied event
     * @param type the new MouseDragEvent type
     * @param gestureSource the new source of the gesture
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates
     * @return new MouseDragEvent that was created from MouseEvent
     * @since JavaFX 8.0
     */
    public static MouseDragEvent copyForMouseDragEvent(
            MouseEvent e,
            Object source, EventTarget target,
            EventType<MouseDragEvent> type,
            Object gestureSource, PickResult pickResult) {
        MouseDragEvent ev = new MouseDragEvent(source, target,
                type, e.sceneX, e.sceneY, e.screenX, e.screenY,
                e.button, e.clickCount, e.shiftDown, e.controlDown,
                e.altDown, e.metaDown, e.primaryButtonDown, e.middleButtonDown,
                e.secondaryButtonDown, e.backButtonDown, e.forwardButtonDown, e.synthesized, e.popupTrigger,
                pickResult, gestureSource);
        ev.recomputeCoordinatesToSource(e, source);
        return ev;
    }

    @SuppressWarnings("doclint:missing")
    private final Flags flags = new Flags();

    /**
     * Determines whether this event will be followed by {@code DRAG_DETECTED}
     * event. It has effect only with  {@code MOUSE_PRESSED} and
     * {@code MOUSE_DRAGGED} events.
     *
     * @return true if the {@code DRAG_DETECTED} event will follow
     */
    public boolean isDragDetect() {
        return flags.dragDetect;
    }

    /**
     * Augments drag detection behavior. The value says whether this event
     * will be followed by {@code DRAG_DETECTED} event. It has effect only
     * with  {@code MOUSE_PRESSED} and  {@code MOUSE_DRAGGED} events.
     *
     * @param dragDetect Whether {@code DRAG_DETECTED} event will follow
     */
    public void setDragDetect(boolean dragDetect) {
        flags.dragDetect = dragDetect;
    }

    /**
     * Horizontal x position of the event relative to the
     * origin of the MouseEvent's node.
     */
    private transient double x;

    /**
     * Horizontal position of the event relative to the
     * origin of the MouseEvent's source.
     *
     * @return horizontal position of the event relative to the
     * origin of the MouseEvent's source.
     */
    public final double getX() {
        return x;
    }

    /**
     * Vertical y position of the event relative to the
     * origin of the MouseEvent's node.
     */
    private transient double y;

    /**
     * Vertical position of the event relative to the
     * origin of the MouseEvent's source.
     *
     * @return vertical position of the event relative to the
     * origin of the MouseEvent's source.
     */
    public final double getY() {
        return y;
    }

    /**
     * Depth z position of the event relative to the
     * origin of the MouseEvent's node.
     */
    private transient double z;

    /**
     * Depth position of the event relative to the
     * origin of the MouseEvent's source.
     *
     * @return depth position of the event relative to the
     * origin of the MouseEvent's source.
     * @since JavaFX 8.0
     */
    public final double getZ() {
        return z;
    }

    /**
     * Absolute horizontal x position of the event.
     */
    private final double screenX;

    /**
     * Returns absolute horizontal position of the event.
     * @return absolute horizontal position of the event
     */
    public final double getScreenX() {
        return screenX;
    }

    /**
     * Absolute vertical y position of the event.
     */
    private final double screenY;

    /**
     * Returns absolute vertical position of the event.
     * @return absolute vertical position of the event
     */
    public final double getScreenY() {
        return screenY;
    }

    /**
     * Horizontal x position of the event relative to the
     * origin of the {@code Scene} that contains the MouseEvent's node.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the MouseEvent's node.
     */
    private final double sceneX;

    /**
     * Returns horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the MouseEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the MouseEvent's node.
     * Note that in 3D scene, this represents the flat coordinates after
     * applying the projection transformations.
     *
     * @return horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the MouseEvent's source
     */
    public final double getSceneX() {
        return sceneX;
    }

    /**
     * Vertical y position of the event relative to the
     * origin of the {@code Scene} that contains the MouseEvent's node.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the MouseEvent's node.
     */
    private final double sceneY;

    /**
     * Returns vertical position of the event relative to the
     * origin of the {@code Scene} that contains the MouseEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the MouseEvent's node.
     * Note that in 3D scene, this represents the flat coordinates after
     * applying the projection transformations.
     *
     * @return vertical position of the event relative to the
     * origin of the {@code Scene} that contains the MouseEvent's source
     */
    public final double getSceneY() {
        return sceneY;
    }

    /**
     * Which, if any, of the mouse buttons is responsible for this event.
     */
    private final MouseButton button;

    /**
     * Which, if any, of the mouse buttons is responsible for this event.
     *
     * @return mouse button whose state change caused this event
     */
    public final MouseButton getButton() {
        return button;
    }

    /**
     * Number of mouse clicks associated with this event.
     * All MOUSE_MOVED events have the clickCount value equal to 0. The
     * value is increased with MOUSE_PRESSED event and stays like
     * that for all subsequent events till MOUSE_RELEASED, including the
     * afterwards generated MOUSE_CLICKED event. The value is increased
     * to numbers higher than one if all the events between two subsequent
     * presses happen on a small region and in a small time (according
     * to native operating system configuration).
     */
    private final int clickCount;

    /**
     * Returns number of mouse clicks associated with this event.
     * All MOUSE_MOVED events have the clickCount value equal to 0. The
     * value is increased with MOUSE_PRESSED event and stays like
     * that for all subsequent events till MOUSE_RELEASED, including the
     * afterwards generated MOUSE_CLICKED event. The value is increased
     * to numbers higher than one if all the events between two subsequent
     * presses happen on a small region and in a small time (according
     * to native operating system configuration).
     *
     * @return number of mouse clicks associated with this event
     */
    public final int getClickCount() {
        return clickCount;
    }

    /**
     * Whether the mouse cursor left the hysteresis region since the previous
     * press.
     */
    private final boolean stillSincePress;

    /**
     * Indicates whether the mouse cursor stayed in the system-provided
     * hysteresis area since last pressed event that occurred before this event.
     * <p>
     * Click event is generated for a node if mouse was both pressed and
     * released over the node, regardless of mouse movements between the press
     * and release. If a node wants to react differently on a simple click and
     * on a mouse drag, it should use a system-supplied short distance
     * threshold to decide between click and drag (users often perform
     * inadvertent tiny movements during a click). It can be easily achieved
     * by ignoring all drags with this method returning {@code true} and
     * ignoring all clicks with this method returning {@code false}.
     *
     * @return true if there were no significant mouse movements (out of
     * system hysteresis area) since the last pressed event that occurred
     * before this event.
     */
    public final boolean isStillSincePress() {
        return stillSincePress;
    }

    /**
     * Whether or not the Shift modifier is down on this event.
     */
    private final boolean shiftDown;

    /**
     * Whether or not the Shift modifier is down on this event.
     * @return true if the Shift modifier is down on this event
     */
    public final boolean isShiftDown() {
        return shiftDown;
    }

    /**
     * Whether or not the Control modifier is down on this event.
     */
    private final boolean controlDown;

    /**
     * Whether or not the Control modifier is down on this event.
     * @return true if the Control modifier is down on this event
     */
    public final boolean isControlDown() {
        return controlDown;
    }

    /**
     * Whether or not the Alt modifier is down on this event.
     */
    private final boolean altDown;

    /**
     * Whether or not the Alt modifier is down on this event.
     * @return true if the Alt modifier is down on this event
     */
    public final boolean isAltDown() {
        return altDown;
    }

    /**
     * Whether or not the Meta modifier is down on this event.
     */
    private final boolean metaDown;

    /**
     * Whether or not the Meta modifier is down on this event.
     * @return true if the Meta modifier is down on this event
     */
    public final boolean isMetaDown() {
        return metaDown;
    }

    @SuppressWarnings("doclint:missing")
    private final boolean synthesized;

    /**
     * Indicates whether this event is synthesized from using a touch screen
     * instead of usual mouse event source devices like mouse or track pad.
     * When a finger is dragged over a touch screen, both scrolling gesture
     * and mouse dragging are produced. If it causes a conflict in an
     * application, this flag can be used to tell apart the usual mouse dragging
     * from the touch screen dragging already handled as scroll events.
     * @return true if this event is synthesized from using a touch screen
     * @since JavaFX 2.2
     */
    public boolean isSynthesized() {
        return synthesized;
    }

    /**
     * Returns whether or not the host platform common shortcut modifier is
     * down on this event. This common shortcut modifier is a modifier key which
     * is used commonly in shortcuts on the host platform. It is for example
     * {@code control} on Windows and {@code meta} (command key) on Mac.
     *
     * @return {@code true} if the shortcut modifier is down, {@code false}
     *      otherwise
     */
    public final boolean isShortcutDown() {
        switch (Toolkit.getToolkit().getPlatformShortcutKey()) {
            case SHIFT:
                return shiftDown;

            case CONTROL:
                return controlDown;

            case ALT:
                return altDown;

            case META:
                return metaDown;

            default:
                return false;
        }
    }

    /**
     * Whether or not this mouse event is the popup menu
     * trigger event for the platform.
     * <p><b>Note</b>: Popup menus are triggered differently
     * on different systems. Therefore, {@code popupTrigger}
     * should be checked in both {@code onMousePressed}
     * and {@code mouseReleased} for proper cross-platform functionality.
     */
    private final boolean popupTrigger;

    /**
     * Returns {@code true} if this mouse event is the popup menu
     * trigger event for the platform.
     * <p><b>Note</b>: Popup menus are triggered differently
     * on different systems. Therefore, {@code popupTrigger}
     * should be checked in both {@code onMousePressed}
     * and {@code mouseReleased} for proper cross-platform functionality.
     *
     * @return {@code true} if this mouse event is the popup menu
     * trigger event for the platform
     * @since JavaFX 8.0
     */
    public final boolean isPopupTrigger() {
        return popupTrigger;
    }

    /**
     * {@code true} if primary button (button 1, usually the left) is currently
     * pressed. Note that this is different from the {@link #getButton() button}
     * variable in that the {@code button} variable indicates which button press was
     * responsible for this event while this variable indicates whether the
     * primary button is depressed.
     */
    private final boolean primaryButtonDown;

    /**
     * Returns {@code true} if primary button (button 1, usually the left)
     * is currently pressed. Note that this is different from the
     * {@code getButton()} method that indicates which button press was
     * responsible for this event while this method indicates whether the
     * primary button is depressed.
     *
     * @return {@code true} if primary button (button 1, usually the left)
     * is currently pressed
     */
    public final boolean isPrimaryButtonDown() {
        return primaryButtonDown;
    }

    /**
     * {@code true} if secondary button (button 3, usually the right) is currently
     * pressed. Note that this is different from the {@link #getButton() button}
     * variable in that the {@code button} variable indicates which button press was
     * responsible for this event while this variable indicates whether the
     * primary button is depressed.
     */
    private final boolean secondaryButtonDown;

    /**
     * Returns {@code true} if secondary button (button 3, usually the right)
     * is currently pressed. Note that this is different from the
     * {@code getButton()} method that indicates which button press was
     * responsible for this event while this method indicates whether the
     * secondary button is depressed.
     *
     * @return {@code true} if secondary button (button 3, usually the right)
     * is currently pressed
     */
    public final boolean isSecondaryButtonDown() {
        return secondaryButtonDown;
    }

    /**
     * {@code true} if middle button (button 2) is currently pressed.
     * Note that this is different from the {@link #getButton() button} variable in
     * that the {@code button} variable indicates which button press was
     * responsible for this event while this variable indicates whether the
     * middle button is depressed.
     */
    private final boolean middleButtonDown;

    /**
     * Returns {@code true} if middle button (button 2)
     * is currently pressed. Note that this is different from the
     * {@code getButton()} method that indicates which button press was
     * responsible for this event while this method indicates whether the
     * middle button is depressed.
     *
     * @return {@code true} if middle button (button 2) is currently pressed
     */
    public final boolean isMiddleButtonDown() {
        return middleButtonDown;
    }

    /**
     * {@code true} if back button (button 4) is currently pressed.
     * Note that this is different from the {@link #getButton() button} variable in
     * that the {@code button} variable indicates which button press was
     * responsible for this event while this variable indicates whether the
     * back button is depressed.
     */
    private final boolean backButtonDown;

    /**
     * Returns {@code true} if back button (button 4)
     * is currently pressed. Note that this is different from the
     * {@code getButton()} method that indicates which button press was
     * responsible for this event while this method indicates whether the
     * back button is depressed.
     *
     * @return {@code true} if back button (button 4) is currently pressed
     * @since 12
     */
    public final boolean isBackButtonDown() {
        return backButtonDown;
    }

    /**
     * {@code true} if forward button (button 5) is currently pressed.
     * Note that this is different from the {@link #getButton() button} variable in
     * that the {@code button} variable indicates which button press was
     * responsible for this event while this variable indicates whether the
     * forward button is depressed.
     */
    private final boolean forwardButtonDown;

    /**
     * Returns {@code true} if forward button (button 5)
     * is currently pressed. Note that this is different from the
     * {@code getButton()} method that indicates which button press was
     * responsible for this event while this method indicates whether the
     * back button is depressed.
     *
     * @return {@code true} if forward button (button 5) is currently pressed
     * @since 12
     */
    public final boolean isForwardButtonDown() {
        return forwardButtonDown;
    }

    /**
     * Returns a string representation of this {@code MouseEvent} object.
     * @return a string representation of this {@code MouseEvent} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("MouseEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
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
        if (isBackButtonDown()) {
            sb.append(", backButtonDown");
        }
        if (isForwardButtonDown()) {
            sb.append(", forwardButtonDown");
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

    /**
     * Information about the pick if the picked {@code Node} is a
     * {@code Shape3D} node and its pickOnBounds is false.
     */
    private PickResult pickResult;

    /**
     * Returns information about the pick.
     *
     * @return new PickResult object that contains information about the pick
     * @since JavaFX 8.0
     */
    public final PickResult getPickResult() {
        return pickResult;
    }

    /**
     * These properties need to live in a separate object shared among all the
     * copied events to make sure that the values are propagated to the
     * original event.
     */
    private static class Flags implements Cloneable {
        /**
         * Whether dragDetected event is going to be sent after this event.
         * Applies only to MOUSE_PRESSED and MOUSE_MOVED event types.
         */
        boolean dragDetect = true;

        @Override
        public Flags clone() {
            try {
                return (Flags) super.clone();
            } catch (CloneNotSupportedException e) {
                /* won't happen */
                return null;
            }
        }
    }

    @SuppressWarnings("doclint:missing")
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        x = sceneX;
        y = sceneY;
    }
}
