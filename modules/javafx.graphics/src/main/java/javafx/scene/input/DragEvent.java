/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EnumSet;
import java.util.Set;

import javafx.beans.NamedArg;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point3D;

import com.sun.javafx.scene.input.InputEventUtils;
import java.io.IOException;

// PENDING_DOC_REVIEW
/**
 * Drag events replace mouse events during drag-and-drop gesture.
 * The difference between press-drag-release and drag-and-drop gestures
 * is described at {@link javafx.scene.input.MouseEvent MouseEvent}.
 * <p>
 * Drag and drop gesture can be started by calling {@code startDragAndDrop()}
 * (on a node or scene) inside of a {@link MouseEvent#DRAG_DETECTED DRAG_DETECTED} event handler.
 * The data to be transferred to drop target are placed to a {@code dragBoard}
 * at this moment.
 * <p>
 * Drag entered/exited events behave similarly to mouse entered/exited
 * events, please see {@code MouseEvent} overview.
 *
 * <h2>Drag sources: initiating a drag and drop gesture</h2>
 *
 * When a drag gesture is detected, an application can decide whether to
 * start a drag and drop gesture or continue with a press-drag-release gesture.
 * <p>
 * The default drag detection mechanism uses mouse movements with a pressed
 * button in combination with hysteresis. This behavior can be
 * augmented by the application. Each {@code MOUSE_PRESSED} and
 * {@code MOUSE_DRAGGED} event has a {@code dragDetect} flag that determines
 * whether a drag gesture has been detected. The default value of this flag
 * depends on the default detection mechanism and can be modified by calling
 * {@code setDragDetect()} inside of an event handler. When processing of
 * one of these events ends with the {@code dragDetect} flag set to true,
 * a {@code DRAG_DETECTED} {@code MouseEvent} is sent to the potential gesture
 * source (the object on which a mouse button has been pressed). This event
 * notifies about the gesture detection.
 * <p>
 * Inside a {@code DRAG_DETECTED} event handler, if the
 * {@code startDragAndDrop()} method is called on a node or scene and a dragged
 * data is made available to the returned {@code Dragboard}, the object on which
 * {@code startDragAndDrop()} has been called is considered a gesture source
 * and the drag and drop gesture is started. The {@code Dragboard} has system
 * clipboard functionality but is specifically used for drag and drop data
 * transfer.
 * <p>
 * The {@code startDragAndDrop()} method takes a set of {@code TransferMode}s
 * supported by the gesture source. For instance passing only
 * {@code TransferMode.COPY} indicates that the gesture source allows only
 * copying of the data, not moving or referencing.
 * <p>
 * Following example shows a simple drag and drop source:
 * <pre>
Rectangle rect = new Rectangle(100, 100);
rect.setOnDragDetected(new EventHandler&lt;MouseEvent&gt;() {
    &#64;Override public void handle(MouseEvent event) {
        Dragboard db = startDragAndDrop(TransferMode.ANY);
        ClipboardContent content = new ClipboardContent();
        content.putString("Hello!");
        db.setContent(content);
        event.consume();
    }
});
 * </pre>
 *
 * <br><h2>Potential drop targets</h2>
 *
 * <p>
 * After the drag and drop gesture has been started, any object
 * ({@code Node}, {@code Scene}) over which the mouse is dragged is
 * a potential drop target.
 * <p>
 * When the mouse is dragged into the boundaries of potential drop target,
 * the potential target gets a {@code DRAG_ENTERED} event. When the mouse is
 * dragged outside of the potential target's bounds, it gets a
 * {@code DRAG_EXITED} event. There are also the bubbling
 * {@code DRAG_ENTERED_TARGET} and {@code DRAG_EXITED_TARGET} variants. They
 * behave similarly to mouse entered/exited events, please see
 * {@code MouseEvent} overview.
 * <p>
 * A potential drop target can decide to change its appearance to
 * let the user know that the dragged data can be dropped on it. This can be
 * done in a {@code DRAG_OVER} event handler, based on the position of the
 * mouse. Another option is to change the potential target's appearance in
 * a {@code DRAG_ENTERED} and {@code DRAG_EXITED} handlers.
 * <p>
 * In {@code DRAG_OVER} event handler a potential drop target has the ability
 * to make it known that it is an actual target. This is done by calling
 * {@code acceptTransferModes(TransferMode...)} on the event,
 * passing transfer modes it is willing to accept.
 * If it <i>is not called</i> during the event delivery or if none of the
 * passed transfer modes is supported by gesture source, then the potential
 * drop target <i>is not considered to be an actual drop target</i>.
 * <p>
 * When deciding weather to accept the event by calling {@code acceptTransferModes(TransferMode...)},
 * the type of data available on the {@code Dragboard} should be considered.
 * Access to the {@code Dragboard} is provided by the {@code getDragboard()}
 * method.
 * <p>
 * When accepting an event, the potential gesture target decides which
 * {@code TransferMode} is accepted for the operation. To make the decision,
 * {@code DragBoard.getTransferModes()} (set of transfer modes supported by
 * the gesture source) and {@code DragEvent.getTransferMode()} (default
 * transfer mode issued by platform, driven by key modifiers) can be used.
 * It is possible to pass more transfer modes into the
 * {@code acceptTransferModes(TransferMode...)} method. In this case
 * it makes the decision in behalf of the
 * application (it chooses the default mode if it's supported by gesture source
 * and accepted by gesture target, otherwise it chooses the most common mode
 * of the supported and accepted ones).
 * The {@code DRAG_DROPPED} event's {@code getTransferMode()} later reports the
 * transfer mode accepted by the {@code DRAG_OVER} event handler.
 * <p>
 * A drag and drop gesture ends when the mouse button is released.
 * If this happens over a gesture target that accepted previous {@code DRAG_OVER}
 * events with a transfer mode supported by gesture source,
 * a {@code DRAG_DROPPED} event is sent to the gesture target.
 * In its handler, the gesture target can access the data on the dragboard.
 * After data has been transferred (or decided not to transfer), the gesture
 * needs to be completed by calling {@code setDropCompleted(Boolean)} on the event.
 * The {@code Boolean} argument indicates if the data has been transferred
 * successfully or not. If it is not called, the gesture is considered
 * unsuccessful.
 *
 * <p>
 * Following example shows a simple drag and drop target for text data:
 * <pre>
Rectangle rect = new Rectangle(100, 100);

rect.setOnDragOver(new EventHandler&lt;DragEvent&gt;() {
    &#64;Override public void handle(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasString()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        event.consume();
    }
});

rect.setOnDragDropped(new EventHandler&lt;DragEvent&gt;() {
    &#64;Override public void handle(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasString()) {
            System.out.println("Dropped: " + db.getString());
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }
});
 * </pre>
 *
 * <h2>Drag sources: finalizing drag and drop gesture</h2>
 *
 * <p>
 * After the gesture has been finished, whether by successful or unsuccessful
 * data transfer or being canceled, the {@code DRAG_DONE} event is sent to
 * the gesture source. The {@code getTransferMode()} method of the event
 * indicates to the gesture source how the transfer of data was completed.
 * If the transfer mode has the value {@code MOVE}, then this allows the source
 * to clear out its data. Clearing the source's data gives the appropriate
 * appearance to a user that the data has been moved by the drag and drop
 * gesture. If it has the value {@code null}, then the drag and drop gesture
 * ended without any data being transferred.  This could happen as a result of
 * a mouse release event over a node that is not a drop target, or the user
 * pressing the ESC key to cancel the drag and drop gesture, or by
 * the gesture target reporting an unsuccessful data transfer.
 * </p>
 * @since JavaFX 2.0
 */
public final class DragEvent extends InputEvent {

    private static final long serialVersionUID = 20121107L;

    /**
     * Common supertype for all drag event types.
     */
    public static final EventType<DragEvent> ANY =
            new EventType<>(InputEvent.ANY, "DRAG");

    /**
     * This event occurs when drag gesture enters a node. It's the
     * bubbling variant, which is delivered also to all parents of the
     * entered node (unless it was consumed). When notifications about
     * entering some of node's children are not desired,
     * {@code DRAG_ENTERED} event handler should be used.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     * which is similar
     */
    public static final EventType<DragEvent> DRAG_ENTERED_TARGET =
            new EventType<>(DragEvent.ANY, "DRAG_ENTERED_TARGET");

    /**
     * This event occurs when drag gesture enters a node.
     * This event type is delivered only to the entered node,
     * if parents want to filter it or get the bubbling event,
     * they need to use {@code DRAG_ENTERED_TARGET}.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     * which is similar
     */
    public static final EventType<DragEvent> DRAG_ENTERED =
            new EventType<>(DragEvent.DRAG_ENTERED_TARGET, "DRAG_ENTERED");

    /**
     * This event occurs when drag gesture exits a node. It's the
     * bubbling variant, which is delivered also to all parents of the
     * exited node (unless it was consumed). When notifications about
     * exiting some of node's children are not desired,
     * {@code DRAG_EXITED} event handler should be used.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     * which is similar
     */
    public static final EventType<DragEvent> DRAG_EXITED_TARGET =
            new EventType<>(DragEvent.ANY, "DRAG_EXITED_TARGET");

    /**
     * This event occurs when drag gesture exits a node.
     * This event type is delivered only to the exited node,
     * if parents want to filter it or get the bubbling event,
     * they need to use {@code DRAG_EXITED_TARGET}.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     * which is similar
     */
    public static final EventType<DragEvent> DRAG_EXITED =
            new EventType<>(DragEvent.DRAG_EXITED_TARGET, "DRAG_EXITED");

    /**
     * This event occurs when drag gesture progresses within this node.
     */
    public static final EventType<DragEvent> DRAG_OVER =
            new EventType<>(DragEvent.ANY, "DRAG_OVER");

    // Do we want DRAG_TRANSFER_MODE_CHANGED event?
//    /**
//     * This event occurs on a potential drag-and-drop target when the user
//     * takes action to change the intended {@code TransferMode}.
//     * The user can change the intended {@link TransferMode} by holding down
//     * or releasing key modifiers.
//     */
//    public static final EventType<DragEvent> DRAG_TRANSFER_MODE_CHANGED =
//            new EventType<DragEvent>(DragEvent.ANY, "DRAG_TRANSFER_MODE_CHANGED");

    /**
     * This event occurs when the mouse button is released during drag and drop
     * gesture on a drop target. Transfer of data from the
     * {@link DragEvent}'s {@link DragEvent#getDragboard() dragboard} should happen
     * in handler of this event.
     */
    public static final EventType<DragEvent> DRAG_DROPPED =
            new EventType<>(DragEvent.ANY, "DRAG_DROPPED");

    /**
     * This event occurs on drag-and-drop gesture source after its data has
     * been dropped on a drop target. The {@code transferMode} of the
     * event shows what just happened at the drop target.
     * If {@code transferMode} has the value {@code MOVE}, then the source can
     * clear out its data. Clearing the source's data gives the appropriate
     * appearance to a user that the data has been moved by the drag and drop
     * gesture. A {@code transferMode} that has the value {@code NONE}
     * indicates that no data was transferred during the drag and drop gesture.
     */
    public static final EventType<DragEvent> DRAG_DONE =
            new EventType<>(DragEvent.ANY, "DRAG_DONE");

    /**
     * Creates a copy of the given drag event with the given fields substituted.
     * @param source the new source of the copied event
     * @param target the new target of the copied event
     * @param gestureSource the new gesture source.
     * @param gestureTarget the new gesture target.
     * @param eventType the new eventType
     * @return the event copy with the fields
     * @since JavaFX 8.0
     */
    public DragEvent copyFor(Object source, EventTarget target,
            Object gestureSource, Object gestureTarget,
            EventType<DragEvent> eventType) {

        DragEvent copyEvent = copyFor(source, target, eventType);
        recomputeCoordinatesToSource(copyEvent, source);
        copyEvent.gestureSource = gestureSource;
        copyEvent.gestureTarget = gestureTarget;
        return copyEvent;
    }

    /**
     * Constructs new DragEvent event.
     * For DRAG_DROPPED and DRAG_DONE event types, the {@code accepted} state
     * and {@code acceptedTransferMode} are set according to the passed
     * {@code transferMode}.
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param dragboard the dragboard of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param transferMode the transfer mode of the event.
     * @param gestureSource the source of the DnD gesture of the event.
     * @param gestureTarget the target of the DnD gesture of the event.
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates and the target
     * @since JavaFX 8.0
     */
    public DragEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target, @NamedArg("eventType") EventType<DragEvent> eventType, @NamedArg("dragboard") Dragboard dragboard,
            @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY, @NamedArg("transferMode") TransferMode transferMode,
            @NamedArg("gestureSource") Object gestureSource, @NamedArg("gestureTarget") Object gestureTarget, @NamedArg("pickResult") PickResult pickResult) {
        super(source, target, eventType);
        this.gestureSource = gestureSource;
        this.gestureTarget = gestureTarget;
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.sceneX = x;
        this.sceneY = y;
        this.transferMode = transferMode;
        this.dragboard = dragboard;

        if (eventType == DragEvent.DRAG_DROPPED
                || eventType == DragEvent.DRAG_DONE) {
            state.accepted = transferMode != null;
            state.acceptedTransferMode = transferMode;
            state.acceptingObject = state.accepted ? source : null;
        }

        this.pickResult = pickResult != null ? pickResult : new PickResult(
                eventType == DRAG_DONE ? null : target, x, y);
        final Point3D p = InputEventUtils.recomputeCoordinates(this.pickResult, null);
        this.x = p.getX();
        this.y = p.getY();
        this.z = p.getZ();
    }

    /**
     * Constructs new DragEvent event with empty source and target.
     * @param eventType The type of the event.
     * @param dragboard the dragboard of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param transferMode the transfer mode of the event.
     * @param gestureSource the source of the DnD gesture of the event.
     * @param gestureTarget the target of the DnD gesture of the event.
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates
     * @since JavaFX 8.0
     */
    public DragEvent(@NamedArg("eventType") EventType<DragEvent> eventType, @NamedArg("dragboard") Dragboard dragboard,
            @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY, @NamedArg("transferMode") TransferMode transferMode,
            @NamedArg("gestureSource") Object gestureSource, @NamedArg("gestureTarget") Object gestureTarget, @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, dragboard, x, y, screenX, screenY, transferMode,
                gestureSource, gestureTarget, pickResult);
    }

    /**
     * Fills the given event by this event's coordinates recomputed to the given
     * source object
     * @param newEvent Event whose coordinates are to be filled
     * @param newSource Source object to compute coordinates for
     */
    private void recomputeCoordinatesToSource(DragEvent newEvent, Object newSource) {

        if (newEvent.getEventType() == DRAG_DONE) {
            // DRAG_DONE contains all zeros, doesn't make sense to recompute it
            return;
        }

        final Point3D newCoordinates = InputEventUtils.recomputeCoordinates(
                pickResult, newSource);

        newEvent.x = newCoordinates.getX();
        newEvent.y = newCoordinates.getY();
        newEvent.z = newCoordinates.getZ();
    }

    @Override
    public DragEvent copyFor(Object newSource, EventTarget newTarget) {
        DragEvent e = (DragEvent) super.copyFor(newSource, newTarget);
        recomputeCoordinatesToSource(e, newSource);
        return e;
    }

    /**
     * Creates a copy of the given drag event with the given fields substituted.
     * @param source source of the copied event
     * @param target target of the copied event
     * @param type type of event
     * @return the event copy with the fields
     * @since JavaFX 8.0
     */
    public DragEvent copyFor(Object source, EventTarget target, EventType<DragEvent> type) {
        DragEvent e = copyFor(source, target);
        e.eventType = type;
        return e;
    }

    @Override
    public EventType<DragEvent> getEventType() {
        return (EventType<DragEvent>) super.getEventType();
    }

    /**
     * Horizontal x position of the event relative to the
     * origin of the MouseEvent's node.
     */
    private transient double x;

    /**
     * Horizontal position of the event relative to the
     * origin of the DragEvent's source.
     *
     * @return horizontal position of the event relative to the
     * origin of the DragEvent's source
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
     * origin of the DragEvent's source.
     *
     * @return vertical position of the event relative to the
     * origin of the DragEvent's source
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
     * origin of the MouseEvent's source
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
     * origin of the {@code Scene} that contains the DragEvent's node.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the DragEvent's node.
     */
    private final double sceneX;

    /**
     * Returns horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the DragEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the DragEvent's node.
     * Note that in 3D scene, this represents the flat coordinates after
     * applying the projection transformations.
     *
     * @return horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the DragEvent's source
     */
    public final double getSceneX() {
        return sceneX;
    }

    /**
     * Vertical y position of the event relative to the
     * origin of the {@code Scene} that contains the DragEvent's node.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the DragEvent's node.
     */
    private final double sceneY;

    /**
     * Returns vertical position of the event relative to the
     * origin of the {@code Scene} that contains the DragEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the DragEvent's node.
     * Note that in 3D scene, this represents the flat coordinates after
     * applying the projection transformations.
     *
     * @return vertical position of the event relative to the
     * origin of the {@code Scene} that contains the DragEvent's source
     */
    public final double getSceneY() {
        return sceneY;
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
     * The source object of the drag and drop gesture.
     * Gesture source is the object that started drag and drop operation.
     * The value {@code null} is valid in the case that the gesture comes
     * from another application.
     * @return the source object of the drag and drop gesture
     */
    public final Object getGestureSource() { return gestureSource; }
    @SuppressWarnings("doclint:missing")
    private Object gestureSource;

    /**
     * The target object of the drag and drop gesture.
     * Gesture target is the object that accepts drag events.
     * The value {@code null} is valid in the case that the drag and drop
     * gesture has been canceled or completed without a transfer taking place
     * or there is currently no event target accepting the drag events.
     * @return the target object of the drag and drop gesture
     */
    public final Object getGestureTarget() { return gestureTarget; }
    @SuppressWarnings("doclint:missing")
    private Object gestureTarget;

    /**
     * Data transfer mode. Before the data transfer is is performed,
     * this is the default transfer mode set by system according to
     * input events such as the user holding some modifiers.
     * In time of data transfer (in DRAG_DROPPED event) it determines
     * the transfer mode accepted by previous DRAG_OVER handler.
     * After the data transfer (in DRAG_DONE event)
     * it determines the actual mode of the transfer done.
     * @return the data transfer mode
     */
    public final TransferMode getTransferMode() { return transferMode; }
    @SuppressWarnings("doclint:missing")
    private TransferMode transferMode;

    @SuppressWarnings("doclint:missing")
    private final State state = new State();

    /**
     * Indicates if this event has been accepted.
     * @return is this event has been accepted
     * @see #acceptTransferModes
     * @defaultValue false
     */
    public final boolean isAccepted() { return state.accepted; }

    /**
     * Gets transfer mode accepted by potential target.
     * @return transfer mode accepted by potential target
     */
    public final TransferMode getAcceptedTransferMode() {
        return state.acceptedTransferMode;
    }

    /**
     * The object that accepted the drag.
     * @return the object that accepted the drag
     * @since JavaFX 8.0
     */
    public final Object getAcceptingObject() {
        return state.acceptingObject;
    }

    /**
     * A dragboard that is available to transfer data.
     * Data can be placed onto this dragboard in handler of the
     * {@code DRAG_DETECTED} mouse event. Data can be copied from this
     * dragboard in handler of the {@code DRAG_DROPPED} event.
     * @return a dragboard that is available to transfer data
     */
    public final Dragboard getDragboard() {
        return dragboard;
    }
    private transient Dragboard dragboard;

    /**
     * Chooses a transfer mode for the operation
     * @param supported Transfer modes supported by gesture source
     * @param accepted Transfer modes accepted by gesture
     * @param proposed Transfer mode proposed by platform
     * @return The chosen transfer mode, null if none would work
     */
    private static TransferMode chooseTransferMode(Set<TransferMode> supported,
            TransferMode[] accepted, TransferMode proposed) {

        TransferMode result = null;
        Set<TransferMode> intersect = EnumSet.noneOf(TransferMode.class);

        for (TransferMode tm : InputEventUtils.safeTransferModes(accepted)) {
            if (supported.contains(tm)) {
                intersect.add(tm);
            }
        }

        if (intersect.contains(proposed)) {
            result = proposed;
        } else {
            if (intersect.contains(TransferMode.MOVE)) {
                result = TransferMode.MOVE;
            } else if (intersect.contains(TransferMode.COPY)) {
                result = TransferMode.COPY;
            } else if (intersect.contains(TransferMode.LINK)) {
                result = TransferMode.LINK;
            }
        }

        return result;
    }

    /**
     * Accepts this {@code DragEvent}, choosing the transfer mode for the
     * drop operation.
     * Used to indicate that the potential drop target
     * that receives this event is a drop target from {@code DRAG_OVER}
     * event handler.
     * <p>
     * It accepts one of the transfer modes that are both passed into this
     * method and supported by the gesture source. It accepts the default
     * transfer mode if possible, otherwise the most common one of the
     * acceptable modes.
     * @param transferModes the transfer mode for the drop operation.
     */
    public void acceptTransferModes(TransferMode... transferModes) {

        if (dragboard == null || dragboard.getTransferModes() == null ||
                transferMode == null) {
            state.accepted = false;
            return;
        }

        TransferMode tm = chooseTransferMode(dragboard.getTransferModes(),
                transferModes, transferMode);

        if (tm == null && getEventType() == DRAG_DROPPED) {
            throw new IllegalStateException("Accepting unsupported transfer "
                    + "modes inside DRAG_DROPPED handler");
        }

        state.accepted = tm != null;
        state.acceptedTransferMode = tm;
        state.acceptingObject = state.accepted ? source : null;
    }

    /**
     * Indicates that transfer handling of this {@code DragEvent} was completed
     * successfully during a {@code DRAG_DROPPED} event handler.
     * No {@link #getDragboard() dragboard} access can happen after this call.
     *
     * @param isTransferDone {@code true} indicates that the transfer was successful.
     * @throws IllegalStateException if this is not a DRAG_DROPPED event
     */
    public void setDropCompleted(boolean isTransferDone) {
        if (getEventType() != DRAG_DROPPED) {
            throw new IllegalStateException("setDropCompleted can be called " +
                    "only from DRAG_DROPPED handler");
        }

        state.dropCompleted = isTransferDone;
    }

    /**
     * Whether {@code setDropCompleted(true)} has been called on this event.
     * @return true if {@code setDropCompleted(true)} has been called
     */
    public boolean isDropCompleted() {
        return state.dropCompleted;
    }

    @SuppressWarnings("doclint:missing")
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        x = sceneX;
        y = sceneY;
    }

    /**
     * These properties need to live in a separate object shared among all the
     * copied events to make sure that the values are propagated to the
     * original event.
     */
    private static class State {
        /**
         * Whether this event has been accepted.
         */
        boolean accepted = false;

        /**
         * Whether drop completed successfully.
         */
        boolean dropCompleted = false;

        /**
         * Transfer mode accepted by the potential gesture target.
         */
        TransferMode acceptedTransferMode = null;

        /**
         * Object that accepted this event.
         */
        Object acceptingObject = null;
    }

}
