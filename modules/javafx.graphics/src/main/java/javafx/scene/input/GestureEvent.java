/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.input.InputEventUtils;
import com.sun.javafx.tk.Toolkit;
import java.io.IOException;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point3D;

/**
 * An event indicating gesture input. Gestures are typically caused by
 * direct (touch screen) or indirect (track pad) touch events.
 *
 * <p>
 * Delivery of gestures is dependent on the capabilities of the underlying
 * platform and connected input devices. For instance on a PC with mouse
 * and keyboard there is no way of producing a rotating gesture.
 * </p>
 * @since JavaFX 2.2
 */
public class GestureEvent extends InputEvent {

    private static final long serialVersionUID = 20121107L;

    /**
     * Common supertype for all gestures.
     */
    public static final EventType<GestureEvent> ANY =
            new EventType<GestureEvent>(InputEvent.ANY, "GESTURE");

    /**
     * Creates a new instance of {@code GestureEvent}.
     * @param eventType Type of the event
     * @deprecated Do not use this constructor. Constructs empty event.
     */
    @Deprecated(since="8")
    protected GestureEvent(final EventType<? extends GestureEvent> eventType) {
        this(eventType, 0, 0, 0, 0, false, false, false, false, false, false, null);
    }

    /**
     * Creates a new instance of {@code GestureEvent}.
     * @param source Event source
     * @param target Event target
     * @param eventType Type of the event
     * @deprecated Do not use this constructor. Constructs empty event.
     */
    @Deprecated(since="8")
    protected GestureEvent(Object source, EventTarget target,
            final EventType<? extends GestureEvent> eventType) {
        super(source, target, eventType);
        x = y = screenX = screenY = sceneX = sceneY = 0;
        shiftDown = controlDown = altDown = metaDown = direct = inertia = false;
    }

    /**
     * Constructs new GestureEvent event.
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @param direct true if the event was caused by direct input device. See {@link #isDirect() }
     * @param inertia if represents inertia of an already finished gesture.
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates and the target
     * @since JavaFX 8.0
     */
    protected GestureEvent(Object source, EventTarget target, final EventType<? extends GestureEvent> eventType,
            double x, double y, double screenX, double screenY,
            boolean shiftDown, boolean controlDown, boolean altDown,
            boolean metaDown, boolean direct, boolean inertia, PickResult pickResult) {
        super(source, target, eventType);
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.sceneX = x;
        this.sceneY = y;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.altDown = altDown;
        this.metaDown = metaDown;
        this.direct = direct;
        this.inertia = inertia;
        this.pickResult = pickResult != null ? pickResult : new PickResult(target, x, y);
        final Point3D p = InputEventUtils.recomputeCoordinates(this.pickResult, null);
        this.x = p.getX();
        this.y = p.getY();
        this.z = p.getZ();
    }

    /**
     * Constructs new GestureEvent event with empty source and target
     * @param eventType The type of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @param direct true if the event was caused by direct input device. See {@link #isDirect() }
     * @param inertia if represents inertia of an already finished gesture.
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates
     * @since JavaFX 8.0
     */
    protected GestureEvent(final EventType<? extends GestureEvent> eventType,
            double x, double y, double screenX, double screenY,
            boolean shiftDown, boolean controlDown, boolean altDown,
            boolean metaDown, boolean direct, boolean inertia,
            PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, shiftDown, controlDown,
                altDown, metaDown, direct, inertia, pickResult);
    }

    /**
     * Fills the given event by this event's coordinates recomputed to the given
     * source object.
     * @param newEvent Event whose coordinates are to be filled
     * @param newSource Source object to compute coordinates for
     */
    private void recomputeCoordinatesToSource(GestureEvent newEvent, Object newSource) {

        final Point3D newCoordinates = InputEventUtils.recomputeCoordinates(
                pickResult, newSource);

        newEvent.x = newCoordinates.getX();
        newEvent.y = newCoordinates.getY();
        newEvent.z = newCoordinates.getZ();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GestureEvent copyFor(Object newSource, EventTarget newTarget) {
        GestureEvent e = (GestureEvent) super.copyFor(newSource, newTarget);
        recomputeCoordinatesToSource(e, newSource);
        return e;
    }

    private transient double x;

    /**
     * Gets the horizontal position of the event relative to the
     * origin of the event's source.
     *
     * @return the horizontal position of the event relative to the
     * origin of the event's source.
     *
     * @see #isDirect()
     */
    public final double getX() {
        return x;
    }

    private transient double y;

    /**
     * Gets the vertical position of the event relative to the
     * origin of the event's source.
     *
     * @return the vertical position of the event relative to the
     * origin of the event's source.
     *
     * @see #isDirect()
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

    private final double screenX;

    /**
     * Gets the absolute horizontal position of the event.
     * @return the absolute horizontal position of the event
     *
     * @see #isDirect()
     */
    public final double getScreenX() {
        return screenX;
    }

    private final double screenY;

    /**
     * Gets the absolute vertical position of the event.
     * @return the absolute vertical position of the event
     *
     * @see #isDirect()
     */
    public final double getScreenY() {
        return screenY;
    }

    private final double sceneX;

    /**
     * Gets the horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the event's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the event's node.
     * Note that in 3D scene, this represents the flat coordinates after
     * applying the projection transformations.
     *
     * @return the horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the event's source
     *
     * @see #isDirect()
     */
    public final double getSceneX() {
        return sceneX;
    }

    private final double sceneY;

    /**
     * Gets the vertical position of the event relative to the
     * origin of the {@code Scene} that contains the event's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the event's node.
     * Note that in 3D scene, this represents the flat coordinates after
     * applying the projection transformations.
     *
     * @return the vertical position of the event relative to the
     * origin of the {@code Scene} that contains the event's source
     *
     * @see #isDirect()
     */
    public final double getSceneY() {
        return sceneY;
    }

    private final boolean shiftDown;

    /**
     * Indicates whether or not the Shift modifier is down on this event.
     * @return true if the Shift modifier is down on this event
     */
    public final boolean isShiftDown() {
        return shiftDown;
    }

    private final boolean controlDown;

    /**
     * Indicates whether or not the Control modifier is down on this event.
     * @return true if the Control modifier is down on this event
     */
    public final boolean isControlDown() {
        return controlDown;
    }

    private final boolean altDown;

    /**
     * Indicates whether or not the Alt modifier is down on this event.
     * @return true if the Alt modifier is down on this event
     */
    public final boolean isAltDown() {
        return altDown;
    }

    private final boolean metaDown;

    /**
     * Indicates whether or not the Meta modifier is down on this event.
     * @return true if the Meta modifier is down on this event
     */
    public final boolean isMetaDown() {
        return metaDown;
    }

    private final boolean direct;

    /**
     * Indicates whether this gesture is caused by a direct or indirect input
     * device. With direct input device the gestures are performed directly at
     * the concrete coordinates, a typical example would be a touch screen.
     * With indirect device the gestures are performed indirectly and usually
     * mouse cursor position is used as the gesture coordinates, a typical
     * example would be a track pad.
     * @return true if this event is caused by a direct input device
     */
    public final boolean isDirect() {
        return direct;
    }

    private final boolean inertia;

    /**
     * Indicates if this event represents an inertia of an already finished
     * gesture.
     * @return true if this is an inertia event
     */
    public boolean isInertia() {
        return inertia;
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
     * Indicates whether or not the host platform common shortcut modifier is
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
     * Returns a string representation of this {@code GestureEvent} object.
     * @return a string representation of this {@code GestureEvent} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("GestureEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        sb.append(", x = ").append(getX()).append(", y = ").append(getY())
                .append(", z = ").append(getZ());
        sb.append(isDirect() ? ", direct" : ", indirect");

        if (isInertia()) {
            sb.append(", inertia");
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
        sb.append(", pickResult = ").append(getPickResult());

        return sb.append("]").toString();
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        x = sceneX;
        y = sceneY;
    }

    @Override
    public EventType<? extends GestureEvent> getEventType() {
        return (EventType<? extends GestureEvent>) super.getEventType();
    }
}
