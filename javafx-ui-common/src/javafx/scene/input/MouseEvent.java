/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.tk.Toolkit;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;

import com.sun.javafx.scene.input.InputEventUtils;
import java.io.IOException;

// PENDING_DOC_REVIEW
/**
 * When mouse event occurs, the top-most node under cursor is picked and
 * the event is delivered to it through capturing and bubbling phases
 * described at {@link javafx.event.EventDispatcher EventDispatcher}.
 * <p>
 * The mouse (pointer's) location is available relative to several
 * coordinate systems: x,y - relative to the origin of the
 * MouseEvent's node, sceneX,sceneY - relative to to the
 * origin of the {@code Scene} that contains the node,
 * screenX,screenY - relative to origin of the screen that
 * contains the mouse pointer.
 *
 * <h4>Dragging gestures</h4>
 * <p>
 * There are three types of dragging gestures. They are all initiated by
 * a mouse press event and terminated as a result of a mouse released
 * event, the source node decides which gesture will take place.
 * <p>
 * The simple press-drag-release gesture is default. It's best used to allow
 * changing size of a shape, dragging it around and so on. Whole
 * press-drag-release gesture is delivered to one node. When mouse
 * button is pressed, the top-most node is picked and all subsequent
 * mouse events are delivered to the same node until the button is released.
 * If a mouse clicked event is generated from these events, it is still
 * delivered to the same node.
 * <p>
 * During simple press-drag-release gesture, the other nodes are not involved
 * and don't get any events. If these nodes need to be involved in the gesture,
 * full press-drag-release gesture has to be activated. This gesture is 
 * best used for connecting nodes by "wires", dragging nodes to other nodes etc.
 * This gesture type is more closely described at 
 * {@link javafx.scene.input.MouseDragEvent MouseDragEvent} which contains
 * the events delivered to the gesture targets.
 * <p>
 * The third gesture type is platform-supported drag-and-drop gesture. It serves
 * best to transfer data and works also between (not necessarily FX)
 * applications. This gesture type is more closely described
 * at {@link javafx.scene.input.DragEvent DragEvent}.
 * <p>
 * In a short summary, simple press-drag-release gesture is activated
 * automatically when a mouse button is pressed and delivers all
 * {@code MouseEvent}s to the gesture source. When you start dragging,
 * eventually the {@code DRAG_DETECTED} event arrives. In its handler
 * you can either start full press-drag-release gesture by calling
 * {@code startFullDrag} method on a node or scene - the {@code MouseDragEvent}s
 * start to be delivered to gesture targets, or you can start drag and drop
 * gesture by calling {@code startDragAndDrop} method on a node or scene -
 * the system switches into the drag and drop mode and {@code DragEvent}s start
 * to be delivered instead of {@code MouseEvent}s. If you don't call any of
 * those methods, the simple press-drag-release gesture continues.
 * <p>
 * Note that dragging a finger over touch screen produces mouse dragging events,
 * but also scroll gesture events. If it means a conflict in an application
 * (the physical dragging action is handled by two different handlers), the
 * {@code isSynthesized()} method may be used to detect the problem and make the
 * dragging handlers behave accordingly.
 *
 * <h4>Mouse enter/exit handling</h4>
 * <p>
 * When mouse enters a node, the node gets {@code MOUSE_ENTERED} event, when
 * it leaves, it gets {@code MOUSE_EXITED} event. These events are delivered
 * only to the entered/exited node and seemingly don't go through the
 * capturing/bubbling phases. This is the most common use-case.
 * <p>
 * When the capturing or bubbling is desired, there are
 * {@code MOUSE_ENTERED_TARGET}/{@code MOUSE_EXITED_TARGET} events. These events
 * go through capturing/bubbling phases normally. This means that parent may
 * receive the {@code MOUSE_ENTERED_TARGET} event when mouse entered
 * either the parent itself or some of its children. To distinguish between
 * these two cases event target can be tested on equality with the node.
 * <p>
 * These two types are closely connected:
 * {@code MOUSE_ENTERED}/{@code MOUSE_EXITED} are subtypes
 * of {@code MOUSE_ENTERED_TARGET}/{@code MOUSE_EXITED_TARGET}.
 * During capturing phase,
 * {@code MOUSE_ENTERED_TARGET} is delivered to the
 * parents. When the event is delivered to the event target (the node that
 * has actually been entered), its type is switched to
 * {@code MOUSE_ENTERED}. Then the type is switched back to
 * {@code MOUSE_ENTERED_TARGET} for the bubbling phase.
 * It's still one event just switching types, so if it's filtered or consumed,
 * it affects both event variants. Thanks to the subtype-relationship, a
 * {@code MOUSE_ENTERED_TARGET} event handler will receive the
 * {@code MOUSE_ENTERED} event on target.
 *
 * <h4>Notes</h4>
 * <ul>
 *   <li>For triggering context menus see the {@link ContextMenuEvent}.</li>
 * </ul>
 */
public class MouseEvent extends InputEvent {
    /**
     * Common supertype for all mouse event types.
     */
    public static final EventType<MouseEvent> ANY =
            new EventType<MouseEvent>(InputEvent.ANY, "MOUSE");

    /**
     * This event occurs when mouse button is pressed. This activates a
     * press-drag-release gesture, so all subsequent mouse events until
     * the button is released are delivered to the same node.
     */
    public static final EventType<MouseEvent> MOUSE_PRESSED =
            new EventType<MouseEvent>(MouseEvent.ANY, "MOUSE_PRESSED");

    /**
     * This event occurs when mouse button is released. It is delivered
     * to the same node where the button has been pressed which activated
     * a press-drag-release gesture.
     */
    public static final EventType<MouseEvent> MOUSE_RELEASED =
            new EventType<MouseEvent>(MouseEvent.ANY, "MOUSE_RELEASED");

    /**
     * This event occurs when mouse button has been clicked (pressed and
     * released on the same node). This event provides a button-like behavior
     * to any node. Note that even long drags can generate click event (it 
     * is delivered to the top-most node on which the mouse was both
     * pressed and released).
     */
    public static final EventType<MouseEvent> MOUSE_CLICKED =
            new EventType<MouseEvent>(MouseEvent.ANY, "MOUSE_CLICKED");

    /**
     * This event occurs when mouse enters a node. It's the bubbling variant,
     * which is delivered also to all parents of the entered node (unless it
     * was consumed). When notifications about mouse entering some of node's
     * children are not desired, {@code MOUSE_ENTERED} event handler should
     * be used.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     */
    public static final EventType<MouseEvent> MOUSE_ENTERED_TARGET =
            new EventType<MouseEvent>(MouseEvent.ANY, "MOUSE_ENTERED_TARGET");

    /**
     * This event occurs when mouse enters a node. This event type is delivered
     * only to the entered node, if parents want to filter it or get the
     * bubbling event, they need to use {@code MOUSE_ENTERED_TARGET}.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     */
    public static final EventType<MouseEvent> MOUSE_ENTERED =
            new EventType<MouseEvent>(MouseEvent.MOUSE_ENTERED_TARGET, "MOUSE_ENTERED");

    /**
     * This event occurs when mouse exits a node. It's the bubbling variant,
     * which is delivered also to all parents of the exited node (unless it
     * was consumed). When notifications about mouse exiting some of node's
     * children are not desired, {@code MOUSE_EXITED} event handler should
     * be used.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     */
    public static final EventType<MouseEvent> MOUSE_EXITED_TARGET =
            new EventType<MouseEvent>(MouseEvent.ANY, "MOUSE_EXITED_TARGET");

    /**
     * This event occurs when mouse exits a node. This event type is delivered
     * only to the exited node, if parents want to filter it or get the
     * bubbling event, they need to use {@code MOUSE_EXITED_TARGET}.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     */
    public static final EventType<MouseEvent> MOUSE_EXITED =
            new EventType<MouseEvent>(MouseEvent.MOUSE_EXITED_TARGET, "MOUSE_EXITED");

    /**
     * This event occurs when mouse moves within a node and no buttons
     * are pressed. If any mouse button is pressed, MOUSE_DRAGGED event
     * occurs instead.
     */
    public static final EventType<MouseEvent> MOUSE_MOVED =
            new EventType<MouseEvent>(MouseEvent.ANY, "MOUSE_MOVED");

    /**
     * This event occurs when mouse moves with a pressed button.
     * It is delivered to the same node where the button has been pressed
     * which activated a press-drag-release gesture. It is delivered
     * regardless of the mouse being within bounds of the node.
     */
    public static final EventType<MouseEvent> MOUSE_DRAGGED =
            new EventType<MouseEvent>(MouseEvent.ANY, "MOUSE_DRAGGED");

    /**
     * This event is delivered to a node that is identified as a source of a
     * dragging gesture. Handler of this event is the only place where
     * full press-drag-release gesture or a drag and drop gesture can be
     * started (by calling {@link javafx.scene.Node#startFullDrag startFullDrag()}
     * of {@link javafx.scene.Node#startDragAndDrop startDragAndDrop()} method).
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
            new EventType<MouseEvent>(MouseEvent.ANY, "DRAG_DETECTED");

    MouseEvent(final EventType<? extends MouseEvent> eventType) {
        super(eventType);
    }

    MouseEvent(Object source, EventTarget target,
            final EventType<? extends MouseEvent> eventType) {
        super(source, target, eventType);
    }

    /**
     * Creates a copy of the given mouse event, substituting the given node for
     * the one in the original event. This function will also adjust the location
     * properties (x, y, screenX, screenY, etc) such that the event is in values
     * relative to the new source.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static MouseEvent impl_copy(Node source, Node target, MouseEvent evt) {
         return impl_copy(source, target, evt, null);
    }

    /**
     * Fills the given event by this event's coordinates recomputed to the given
     * source object
     * @param newEvent Event whose coordinates are to be filled
     * @param newSource Source object to compute coordinates for
     */
    private void recomputeCoordinatesToSource(MouseEvent newEvent, Object newSource) {

        final Point2D newCoordinates = InputEventUtils.recomputeCoordinates(
                new Point2D(sceneX, sceneY), null, newSource);

        newEvent.x = newCoordinates.getX();
        newEvent.y = newCoordinates.getY();
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
    public Event copyFor(Object newSource, EventTarget newTarget) {
        MouseEvent e = (MouseEvent) super.copyFor(newSource, newTarget);
        recomputeCoordinatesToSource(e, newSource);
        return e;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setClickParams(int clickCount, boolean stillSincePress) {
        this.clickCount = clickCount;
        this.stillSincePress = stillSincePress;
    }

    /**
     * Copies all private fields (except of event type) from one event to
     * another event. This is for implementing impl_copy in subclasses.
     */
    static void copyFields(MouseEvent from, MouseEvent to,
            Object source, EventTarget target) {
        to.x = from.x;
        to.y = from.y;
        to.screenX = from.screenX;
        to.screenY = from.screenY;
        to.sceneX = from.sceneX;
        to.sceneY = from.sceneY;
        to.button = from.button;
        to.clickCount = from.clickCount;
        to.stillSincePress = from.stillSincePress;
        to.shiftDown = from.shiftDown;
        to.controlDown = from.controlDown;
        to.altDown = from.altDown;
        to.metaDown = from.metaDown;
        to.popupTrigger = from.popupTrigger;
        to.primaryButtonDown = from.primaryButtonDown;
        to.secondaryButtonDown = from.secondaryButtonDown;
        to.middleButtonDown = from.middleButtonDown;
        to.synthesized = from.synthesized;
        to.source = source;
        to.target = target;

        from.recomputeCoordinatesToSource(to, source);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static MouseEvent impl_copy(Object source, EventTarget target, MouseEvent evt,
            EventType<? extends MouseEvent> impl_EventType) {
        MouseEvent copyEvent = impl_mouseEvent(source, target, evt.x, evt.y, evt.screenX,
                evt.screenY, evt.button, evt.clickCount, evt.stillSincePress,
                evt.shiftDown, evt.controlDown, evt.altDown, evt.metaDown,
                evt.popupTrigger, evt.primaryButtonDown, evt.middleButtonDown,
                evt.secondaryButtonDown, evt.synthesized,
                (impl_EventType != null
                        ? impl_EventType
                        : (EventType<? extends MouseEvent>)
                                evt.getEventType()));

        copyEvent.sceneX = evt.sceneX;
        copyEvent.sceneY = evt.sceneY;

        evt.recomputeCoordinatesToSource(copyEvent, source);
        return copyEvent;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static MouseEvent impl_mouseEvent(double _x, double _y,
          double _screenX, double _screenY,
          MouseButton _button,
          int _clickCount,
          boolean _shiftDown,
          boolean _controlDown,
          boolean _altDown,
          boolean _metaDown,
          boolean _popupTrigger,
          boolean _primaryButtonDown,
          boolean _middleButtonDown,
          boolean _secondaryButtonDown,
          boolean _synthesized,
          EventType<? extends MouseEvent> _eventType
          )
    {
        MouseEvent e = new MouseEvent(_eventType);
        e.x = _x;
        e.y = _y;
        e.screenX = _screenX;
        e.screenY = _screenY;
        e.sceneX = _x;
        e.sceneY = _y;
        e.button = _button;
        e.clickCount = _clickCount;
        e.stillSincePress = false;
        e.shiftDown = _shiftDown;
        e.controlDown = _controlDown;
        e.altDown = _altDown;
        e.metaDown = _metaDown;
        e.popupTrigger = _popupTrigger;
        e.primaryButtonDown = _primaryButtonDown;
        e.middleButtonDown = _middleButtonDown;
        e.secondaryButtonDown = _secondaryButtonDown;
        e.synthesized = _synthesized;
        return e;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    // SB-dependency: RT-21224 has been filed to track this
    @Deprecated
    private static MouseEvent impl_mouseEvent(Object _source, EventTarget _target,
          double _x, double _y,
          double _screenX, double _screenY,
          MouseButton _button,
          int _clickCount,
          boolean _stillSincePress,
          boolean _shiftDown,
          boolean _controlDown,
          boolean _altDown,
          boolean _metaDown,
          boolean _popupTrigger,
          boolean _primaryButtonDown,
          boolean _middleButtonDown,
          boolean _secondaryButtonDown,
          boolean _synthesized,
          EventType<? extends MouseEvent> _eventType
          )
    {
        MouseEvent e = new MouseEvent(_source, _target, _eventType);
        e.x = _x;
        e.y = _y;
        e.screenX = _screenX;
        e.screenY = _screenY;
        e.sceneX = _x;
        e.sceneY = _y;
        e.button = _button;
        e.clickCount = _clickCount;
        e.stillSincePress = _stillSincePress;
        e.shiftDown = _shiftDown;
        e.controlDown = _controlDown;
        e.altDown = _altDown;
        e.metaDown = _metaDown;
        e.popupTrigger = _popupTrigger;
        e.primaryButtonDown = _primaryButtonDown;
        e.middleButtonDown = _middleButtonDown;
        e.secondaryButtonDown = _secondaryButtonDown;
        e.synthesized = _synthesized;
        return e;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static boolean impl_getPopupTrigger(
            final MouseEvent mouseEvent) {
        return mouseEvent.popupTrigger;
    }

    private Flags flags = new Flags();

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
     * Absolute horizontal x position of the event.
     */
    private double screenX;

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
    private double screenY;

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
    private double sceneX;

    /**
     * Returns horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the MouseEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the MouseEvent's node.
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
    private double sceneY;

    /**
     * Returns vertical position of the event relative to the
     * origin of the {@code Scene} that contains the MouseEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the MouseEvent's node.
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
    private MouseButton button;

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
    private int clickCount;

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
    private boolean stillSincePress;
    
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
    private boolean shiftDown;

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
    private boolean controlDown;

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
    private boolean altDown;

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
    private boolean metaDown;

    /**
     * Whether or not the Meta modifier is down on this event.
     * @return true if the Meta modifier is down on this event
     */
    public final boolean isMetaDown() {
        return metaDown;
    }

    private boolean synthesized;

    /**
     * Indicates whether this event is synthesized from using a touch screen
     * instead of usual mouse event source devices like mouse or track pad.
     * When a finger is dragged over a touch screen, both scrolling gesture
     * and mouse dragging are produced. If it causes a conflict in an
     * application, this flag can be used to tell apart the usual mouse dragging
     * from the touch screen dragging already handled as scroll events.
     * @return true if this event is synthesized from using a touch screen
     * @since 2.2
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
    private boolean popupTrigger;

    /**
     * {@code true} if primary button (button 1, usually the left) is currently
     * pressed. Note that this is different from the {@link #getButton() button}
     * variable in that the {@code button} variable indicates which button press was
     * responsible for this event while this variable indicates whether the
     * primary button is depressed.
     */
    private boolean primaryButtonDown;

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
    private boolean secondaryButtonDown;

    /**
     * Returns {@code true} if secondary button (button 1, usually the right) 
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
    private boolean middleButtonDown;

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
     * Returns a string representation of this {@code MouseEvent} object.
     * @return a string representation of this {@code MouseEvent} object.
     */ 
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("MouseEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        sb.append(", x = ").append(getX()).append(", y = ").append(getY());

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

        return sb.append("]").toString();
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

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        x = sceneX;
        y = sceneY;
    }
}
