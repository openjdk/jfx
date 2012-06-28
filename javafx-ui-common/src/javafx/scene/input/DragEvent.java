/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
import java.util.EnumSet;
import java.util.Set;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;

import com.sun.javafx.scene.input.InputEventUtils;
import com.sun.javafx.tk.TKDropEvent;

// PENDING_DOC_REVIEW
/**
 * Drag events replace mouse events during drag-and-drop gesture.
 * The difference between press-drag-release and drag-and-drop gestures
 * is described at {@link javafx.scene.input#MouseEvent MouseEvent}.
 * <p>
 * Drag and drop gesture can be started by calling {@code startDragAndDrop()}
 * (on a node or scene) inside of a {@link MouseEvent#DRAG_DETECTED DRAG_DETECTED} event handler.
 * The data to be transfered to drop target are placed to a {@code dragBoard}
 * at this moment.
 * <p>
 * Drag entered/exited events behave similarly to mouse entered/exited
 * events, please see {@code MouseEvent} overview.
 * <p>
 *
 * <br><h4>Drag sources: initiating a drag and drop gesture</h4>
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
 * {@code startDragAndDrop()} has been called is considred a gesture source
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
 * <code><pre>
Rectangle rect = new Rectangle(100, 100);
rect.setOnDragDetected(new EventHandler<MouseEvent>() {
    &#64;Override public void handle(MouseEvent event) {
        Dragboard db = startDragAndDrop(TransferMode.ANY);
        ClipboardContent content = new ClipboardContent();
        content.putString("Hello!");
        db.setContent(content);
        event.consume();
    }
});
 * </pre></code>
 *
 * <br><h4>Potential drop targets</h4>
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
 * It is poosible to pass more transfer modes into the
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
 * <code><pre>
Rectangle rect = new Rectangle(100, 100);

rect.setOnDragOver(new EventHandler<DragEvent>() {
    &#64;Override public void handle(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasString()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        event.consume();
    }
});

rect.setOnDragDropped(new EventHandler<DragEvent>() {
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
 * </pre></code>
 *
 * <br><h4>Drag sources: finalizing drag and drop gesture</h4>
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
 */
public class DragEvent extends InputEvent {

    /**
     * Common supertype for all drag event types.
     */
    public static final EventType<DragEvent> ANY =
            EventTypeUtil.registerInternalEventType(InputEvent.ANY, "DRAG");

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
            EventTypeUtil.registerInternalEventType(DragEvent.ANY, "DRAG_ENTERED_TARGET");

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
            EventTypeUtil.registerInternalEventType(DragEvent.DRAG_ENTERED_TARGET, "DRAG_ENTERED");

    /**
     * This event occurs when drag gesture exits a node. It's the
     * bubbling variant, which is delivered also to all parents of the
     * eixited node (unless it was consumed). When notifications about
     * exiting some of node's children are not desired,
     * {@code DRAG_EXITED} event handler should be used.
     *
     * @see MouseEvent MouseEvent for more information about mouse entered/exited handling
     * which is similar
     */
    public static final EventType<DragEvent> DRAG_EXITED_TARGET =
            EventTypeUtil.registerInternalEventType(DragEvent.ANY, "DRAG_EXITED_TARGET");

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
            EventTypeUtil.registerInternalEventType(DragEvent.DRAG_EXITED_TARGET, "DRAG_EXITED");

    /**
     * This event occurs when drag gesture progresses within this node.
     */
    public static final EventType<DragEvent> DRAG_OVER =
            EventTypeUtil.registerInternalEventType(DragEvent.ANY, "DRAG_OVER");

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
     * {@link DragEvent}'s {@link DragEvent#dragboard dragboard} should happen
     * in handler of this event.
     */
    public static final EventType<DragEvent> DRAG_DROPPED =
            EventTypeUtil.registerInternalEventType(DragEvent.ANY, "DRAG_DROPPED");

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
            EventTypeUtil.registerInternalEventType(DragEvent.ANY, "DRAG_DONE");

    private DragEvent(final EventType<? extends DragEvent> eventType) {
        super(eventType);
    }

    private DragEvent(Object source, EventTarget target,
            final EventType<? extends DragEvent> eventType) {
        super(source, target, eventType);
    }

    /**
     * Creates a copy of the given drag event with the given fields substituted.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static DragEvent impl_copy(Object source, EventTarget target,
            Object gestureSource, Object gestureTarget, Dragboard dragboard,
            DragEvent evt) {
        DragEvent e = impl_copy(source, target, gestureSource, gestureTarget,
                evt, null);
        e.dragboard = dragboard;
        return e;
    }

    /**
     * Creates a copy of the given drag event with the given fields substituted.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static DragEvent impl_copy(Object source, EventTarget target,
            DragEvent evt, EventType<DragEvent> eventType) {
        return impl_copy(source, target, evt.getGestureSource(),
                evt.getGestureTarget(), evt, eventType);
    }

    /**
     * Creates a copy of the given drag event with the given fields substituted.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static DragEvent impl_copy(Object source, EventTarget target,
            Object gestureSource, Object gestureTarget, DragEvent evt,
            EventType<DragEvent> eventType) {

        return impl_copy(source, target, gestureSource, gestureTarget,
                evt.getTransferMode(), evt, eventType);
    }

    /**
     * Creates a copy of the given drag event with the given fields substituted.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static DragEvent impl_copy(Object source, EventTarget target,
            Object gestureSource, Object gestureTarget, TransferMode transferMode,
            DragEvent evt, EventType<DragEvent> eventType) {

        DragEvent copyEvent = impl_dragEvent(source, target, gestureSource,
                gestureTarget, evt.x, evt.y, evt.screenX,
                evt.screenY, evt.transferMode,
                (eventType != null
                        ? eventType
                        : (EventType<? extends DragEvent>)
                                evt.getEventType()), evt.dragboard);

        evt.recomputeCoordinatesToSource(copyEvent, source);
        copyEvent.tkDropEvent = evt.tkDropEvent;
        copyEvent.tkRecognizedEvent = evt.tkRecognizedEvent;
        copyEvent.transferMode = transferMode;
        if (eventType == DragEvent.DRAG_DROPPED ||
                eventType == DragEvent.DRAG_DONE) {
            copyEvent.state.accepted = transferMode != null;
            copyEvent.state.acceptedTrasferMode = transferMode;
        }
        return copyEvent;
    }

    private static DragEvent impl_dragEvent(Object _source, EventTarget _target,
            Object _gestureSource, Object _gestureTarget,
            double _x, double _y,
            double _screenX, double _screenY, TransferMode _transferMode,
            EventType<? extends DragEvent> _eventType, Dragboard _dragboard) {

        DragEvent e = new DragEvent(_source, _target, _eventType);
        e.gestureSource = _gestureSource;
        e.gestureTarget = _gestureTarget;
        e.x = _x;
        e.y = _y;
        e.screenX = _screenX;
        e.screenY = _screenY;
        e.sceneX = _x;
        e.sceneY = _y;
        e.transferMode = _transferMode;
        e.dragboard = _dragboard;
        return e;
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

        final Point2D newCoordinates = InputEventUtils.recomputeCoordinates(
                new Point2D(sceneX, sceneY), null, newSource);

        newEvent.x = newCoordinates.getX();
        newEvent.y = newCoordinates.getY();
    }
    
    @Override
    public Event copyFor(Object newSource, EventTarget newTarget) {
        DragEvent e = (DragEvent) super.copyFor(newSource, newTarget);
        recomputeCoordinatesToSource(e, newSource);
        return e;
    }

    /**
     * Horizontal x position of the event relative to the
     * origin of the MouseEvent's node.
     */
    private double x;

    /**
     * Horizontal position of the event relative to the
     * origin of the DragEvent's source.
     * 
     * @return horizontal position of the event relative to the
     * origin of the DragEvent's source.
     */
    public final double getX() {
        return x;
    }

    /**
     * Vertical y position of the event relative to the
     * origin of the MouseEvent's node.
     */
    private double y;

    /**
     * Vertical position of the event relative to the
     * origin of the DragEvent's source.
     * 
     * @return vertical position of the event relative to the
     * origin of the DragEvent's source.
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
     * origin of the {@code Scene} that contains the DragEvent's node.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the DragEvent's node.
     */
    private double sceneX;

    /**
     * Returns horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the DragEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the DragEvent's node.
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
    private double sceneY;

    /**
     * Returns vertical position of the event relative to the
     * origin of the {@code Scene} that contains the DragEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the DragEvent's node.
     * 
     * @return vertical position of the event relative to the
     * origin of the {@code Scene} that contains the DragEvent's source
     */
    public final double getSceneY() {
        return sceneY;
    }

    /**
     * The source object of the drag and drop gesture.
     * Gesture source is the object that started drag and drop operation.
     * The value {@code null} is valid in the case that the gesture comes
     * from another application.
     */
    public final Object getGestureSource() { return gestureSource; }
    private Object gestureSource;

    /**
     * The target object of the drag and drop gesture.
     * Gesture target is the object that accepts drag events.
     * The value {@code null} is valid in the case that the drag and drop
     * gesture has been canceled or completed without a transfer taking place
     * or there is currently no event target accepting the drag events.
     */
    public final Object getGestureTarget() { return gestureTarget; }
    private Object gestureTarget;

    /**
     * Data transfer mode. Before the data transfer is is performed,
     * this is the default transfer mode set by system according to
     * input events such as the user holding some modifiers.
     * In time of data transfer (in DRAG_DROPPED event) it determines
     * the transfer mode accepted by previous DRAG_OVER handler.
     * After the data transfer (in DRAG_DONE event)
     * it determines the actual mode of the transfer done.
     */
    public final TransferMode getTransferMode() { return transferMode; }
    private TransferMode transferMode;

    private State state = new State();

    /**
     * Indicates if this event has been accepted.
     * @see #acceptTransferModes
     * @defaultValue false
     */
    public final boolean isAccepted() { return state.accepted; }

    /**
     * Gets transfer mode accepted by potential target.
     * @return transfer mode accepted by potential target.
     */
    public final TransferMode getAcceptedTransferMode() {
        return state.acceptedTrasferMode;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public Object impl_getAcceptingObject() {
        return state.acceptingObject;
    }

    private TKDropEvent tkDropEvent;
    private Object tkRecognizedEvent;

    /**
     * A dragboard that is available to transfer data.
     * Data can be placed onto this dragboard in handler of the
     * {@code DRAG_DETECTED} mouse event. Data can be copied from this
     * dragboard in handler of the {@code DRAG_DROPPED} event.
     */
    public final Dragboard getDragboard() {
        return dragboard;
    }
    private transient Dragboard dragboard;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public Dragboard impl_getPlatformDragboard() {
        if (tkDropEvent == null) {
            return null;
        }
        return (Dragboard) tkDropEvent.getDragboard();
    }

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

        for (TransferMode tm : accepted) {
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
        if (tkDropEvent != null) {
            if (state.accepted) {
                tkDropEvent.accept(tm);
            } else {
                tkDropEvent.reject();
            }
        }
        state.acceptedTrasferMode = tm;
        state.acceptingObject = state.accepted ? source : null;
    }

    /**
     * Indicates that transfer handling of this {@code DragEvent} was completed
     * successfully during a {@code DRAG_DROPPED} event handler.
     * No {@link #dragboard} access can happen after this call.
     *
     * @param isTransferDone {@code true} indicates that the transfer was successful.
     * @throws IllegalStateException if this is not a DRAG_DROPPED event
     */
    public void setDropCompleted(boolean isTransferDone) {
        if (getEventType() != DRAG_DROPPED) {
            throw new IllegalStateException("setDropCompleted can be called " +
                    "only from DRAG_DROPPED handler");
        }

        if (tkDropEvent != null) {
            tkDropEvent.dropComplete(isTransferDone);
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

    /**
     * Used by toolkit
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setRecognizedEvent(Object e) {
        tkRecognizedEvent = e;
    }

    /**
     * Used by toolkit
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static DragEvent impl_create(double _x, double _y,
            double _screenX, double _screenY, TransferMode _transferMode,
            Dragboard _dragboard, TKDropEvent _tkDropEvent) {
        
        DragEvent de = new DragEvent(DragEvent.ANY);

        de.x = _x;
        de.y = _y;
        de.screenX = _screenX;
        de.screenY = _screenY;
        de.sceneX = _x;
        de.sceneY = _y;
        de.transferMode = _transferMode;
        de.dragboard = _dragboard;
        de.tkDropEvent = _tkDropEvent;
        return de;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static DragEvent impl_create(EventType<DragEvent> _eventType,
            Object _source, EventTarget _target, Object _gestureSource,
            Object _gestureTarget, double _x, double _y,
            double _screenX, double _screenY, TransferMode _transferMode,
            Dragboard _dragboard, TKDropEvent _tkDropEvent) {

        DragEvent de = new DragEvent(_source, _target, _eventType);

        de.gestureSource = _gestureSource;
        de.gestureTarget = _gestureTarget;
        de.x = _x;
        de.y = _y;
        de.screenX = _screenX;
        de.screenY = _screenY;
        de.sceneX = _x;
        de.sceneY = _y;
        de.transferMode = _transferMode;
        de.dragboard = _dragboard;
        de.tkDropEvent = _tkDropEvent;
        return de;
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
        TransferMode acceptedTrasferMode = null;

        /**
         * Object that accepted this event.
         */
        Object acceptingObject = null;
    }

}
