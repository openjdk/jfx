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

import com.sun.javafx.scene.input.InputEventUtils;
import javafx.event.EventTarget;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * Touch point represents a single point of a multi-touch action, typically
 * one finger touching a screen. It is contained in {@link TouchEvent}.
 * <p>
 * The touch point has its coordinates, state (see {@link State}) and ID. The
 * ID is sequential number of this touch point unique in scope of a single
 * multi-touch gesture.
 * <p>
 * Each touch point is by default delivered to a single node during its whole
 * trajectory - to the node on which it was pressed. There is a grabbing API
 * to modify this behavior. The above means that when touch point is pressed,
 * it is automatically grabbed by the top-most node on the press coordinates.
 * Any time during the gesture {@code grab()} and {@code ungrab()} methods
 * can be used to alter the event delivery target. When grabbed by a different
 * node, it will next time be targeted to it; when ungrabbed, it will be
 * always targeted to the top-most node on the current location.
 */
public final class TouchPoint {

    private EventTarget target;
    private Object source;

    private TouchPoint(int id, State state, double x, double y, double screenX,
            double screenY) {
        this.target = null;
        this.id = id;
        this.state = state;
        this.x = x;
        this.y = y;
        this.sceneX = x;
        this.sceneY = y;
        this.screenX = screenX;
        this.screenY = screenY;
    }

    /**
     * Recomputes this touch point (coordinates, relevancy) for the given event
     * source object.
     * @param oldSource Source object of the current values
     * @param newSource Source object to compute values for
     */
    void recomputeToSource(Object oldSource, Object newSource) {

        final Point2D newCoordinates = InputEventUtils.recomputeCoordinates(
                new Point2D(x, y), oldSource, newSource);

        x = newCoordinates.getX();
        y = newCoordinates.getY();

        source = newSource;
    }

    /**
     * Distinguishes between touch points targeted to the given node or some
     * of its children from touch points targeted somewhere else. This allows
     * for testing all touch points carried by one touch event on their
     * relevance for a given node.
     * @param target Node or other event target to be tested
     * @return true if this touch point is targeted to the given target or
     * some of its children
     */
    public boolean belongsTo(EventTarget target) {

        if (this.target instanceof Node) {
            Node n = (Node) this.target;

            if (target instanceof Scene) {
                return n.getScene() == target;
            }
            while (n != null) {
                if (n == target) {
                    return true;
                }
                n = n.getParent();
            }
        }
        
        return target == this.target;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setTarget(EventTarget target) {
        this.target = target;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_reset() {
        x = sceneX;
        y = sceneY;
    }

    private EventTarget grabbed = null;

    /**
     * Gets event target which has grabbed this touch point.
     * @return The current grabbed target, null if the touch point is ungrabbed
     */
    public EventTarget getGrabbed() {
        return grabbed;
    }

    /**
     * Grabs this touch point by current event source. Next event containing
     * this touch point will be targeted to the same node whose event handler
     * called this method.
     */
    public void grab() {
        if (source instanceof EventTarget) {
            grabbed = (EventTarget) source;
        } else {
            throw new IllegalStateException("Cannot grab touch point, "
                    + "source is not an instance of EventTarget: " + source);
        }
    }

    /**
     * Grabs this touch point by the given target. Next event containing this
     * touch point will be targeted to it.
     * @param target Target by which to grab the touch point
     */
    public void grab(EventTarget target) {
        grabbed = target;
    }

    public void ungrab() {
        grabbed = null;
    }

    private int id;

    /**
     * Gets identifier of this touch point. The number is sequential and unique
     * in scope of one multi touch gesture. The first pressed touch point has id
     * {@code 1}, each subsequently pressed touch points gets the next ordinal
     * number until all touch points are released and the counter is reset.
     *
     * @return the identifier of this touch point.
     */
    public final int getId() {
        return id;
    }

    private State state;

    /**
     * Gets state of this touch point
     * @return state of this touch point
     */
    public final State getState() {
        return state;
    }


    private double x;

    /**
     * Gets the horizontal position of the touch point relative to the
     * origin of the TouchEvent's source.
     *
     * @return the horizontal position of the touch point relative to the
     * origin of the TouchEvent's source.
     */
    public final double getX() {
        return x;
    }

    private double y;

    /**
     * Gets the vertical position of the touch point relative to the
     * origin of the TouchEvent's source.
     *
     * @return the vertical position of the touch point relative to the
     * origin of the TouchEvent's source.
     */
    public final double getY() {
        return y;
    }

    private double screenX;

    /**
     * Gets the absolute horizontal position of the touch point.
     * @return the absolute horizontal position of the touch point
     */
    public final double getScreenX() {
        return screenX;
    }

    private double screenY;

    /**
     * Gets the absolute vertical position of the touch point.
     * @return the absolute vertical position of the touch point
     */
    public final double getScreenY() {
        return screenY;
    }

    private double sceneX;

    /**
     * Gets the horizontal position of the touch point relative to the
     * origin of the {@code Scene} that contains the TouchEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the TouchEvent's node.
     *
     * @return the horizontal position of the touch point relative to the
     * origin of the {@code Scene} that contains the TouchEvent's source
     */
    public final double getSceneX() {
        return sceneX;
    }

    private double sceneY;

    /**
     * Gets the vertical position of the touch point relative to the
     * origin of the {@code Scene} that contains the TouchEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the TouchEvent's node.
     *
     * @return the vertical position of the touch point relative to the
     * origin of the {@code Scene} that contains the TouchEvent's source
     */
    public final double getSceneY() {
        return sceneY;
    }

    /**
     * Gets event target on which the touch event carrying this touch point
     * is fired.
     * @return Event target for this touch point
     */
    public EventTarget getTarget() {
        return target;
    }

    /**
     * Returns a string representation of this {@code TouchPoint} object.
     * @return a string representation of this {@code TouchPoint} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("TouchPoint [");

        sb.append("state = ").append(getState());
        sb.append(", id = ").append(getId());
        sb.append(", target = ").append(getTarget());
        sb.append(", x = ").append(getX()).append(", y = ").append(getY());

        return sb.append("]").toString();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static TouchPoint impl_touchPoint(
            int id, State state, double x, double y, double screenX, double screenY) {
        return new TouchPoint(id, state, x, y, screenX, screenY);
    }

    /**
     * Represents current state of the touch point
     */
    public enum State {
        /**
         * The touch point has just been pressed (touched for the first time)
         */
        PRESSED,
        /**
         * The touch point has been moved
         */
        MOVED,
        /**
         * The touch point remains pressed and still (without moving)
         */
        STATIONARY,
        /**
         * The touch point has been released
         */
        RELEASED
    }
}
