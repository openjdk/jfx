/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * This {@code Transition} creates a rotation animation that spans its
 * {@code duration}. This is done by updating the {@code rotate} variable of the
 * {@code node} at regular interval. The angle value is specified in degrees.
 * <p>
 * It starts from the {@code fromAngle} if provided else uses the {@code node}'s
 * {@code rotate} value.
 * <p>
 * It stops at the {@code toAngle} value if provided else it will use start
 * value plus {@code byAngle}.
 * <p>
 * The {@code toAngle} takes precedence if both {@code toAngle} and
 * {@code byAngle} are specified.
 *
 * <p>
 * Code Segment Example:
 * </p>
 *
 * <pre>
 * <code>
 * import javafx.scene.shape.*;
 * import javafx.animation.*;
 *
 * ...
 *
 *     Rectangle rect = new Rectangle (100, 40, 100, 100);
 *     rect.setArcHeight(50);
 *     rect.setArcWidth(50);
 *     rect.setFill(Color.VIOLET);
 *
 *     RotateTransition rt = new RotateTransition(Duration.millis(3000), rect);
 *     rt.setByAngle(180);
 *     rt.setCycleCount(4);
 *     rt.setAutoReverse(true);
 *
 *     rt.play();
 *
 * ...
 *
 * </code>
 * </pre>
 *
 * @see Transition
 * @see Animation
 *
 * @since JavaFX 2.0
 */
public final class RotateTransition extends Transition {

    private static final double EPSILON = 1e-12;

    private double start;
    private double delta;

    /**
     * The target node of this {@code RotateTransition}.
     * <p>
     * It is not possible to change the target {@code node} of a running
     * {@code RotateTransition}. If the value of {@code node} is changed for a
     * running {@code RotateTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     */
    private ObjectProperty<Node> node;
    private static final Node DEFAULT_NODE = null;

    public final void setNode(Node value) {
        if ((node != null) || (value != null /* DEFAULT_NODE */)) {
            nodeProperty().set(value);
        }
    }

    public final Node getNode() {
        return (node == null)? DEFAULT_NODE : node.get();
    }

    public final ObjectProperty<Node> nodeProperty() {
        if (node == null) {
            node = new SimpleObjectProperty<>(this, "node", DEFAULT_NODE);
        }
        return node;
    }

    private Node cachedNode;

    /**
     * The duration of this {@code RotateTransition}.
     * <p>
     * It is not possible to change the {@code duration} of a running
     * {@code RotateTransition}. If the value of {@code duration} is changed for
     * a running {@code RotateTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     * <p>
     * Note: While the unit of {@code duration} is a millisecond, the
     * granularity depends on the underlying operating system and will in
     * general be larger. For example animations on desktop systems usually run
     * with a maximum of 60fps which gives a granularity of ~17 ms.
     *
     * Setting duration to value lower than {@link Duration#ZERO} will result
     * in {@link IllegalArgumentException}.
     *
     * @defaultValue 400ms
     */
    private ObjectProperty<Duration> duration;
    private static final Duration DEFAULT_DURATION = Duration.millis(400);

    public final void setDuration(Duration value) {
        if ((duration != null) || (!DEFAULT_DURATION.equals(value))) {
            durationProperty().set(value);
        }
    }

    public final Duration getDuration() {
        return (duration == null)? DEFAULT_DURATION : duration.get();
    }

    public final ObjectProperty<Duration> durationProperty() {
        if (duration == null) {
            duration = new ObjectPropertyBase<Duration>(DEFAULT_DURATION) {

                @Override
                public void invalidated() {
                    try {
                        setCycleDuration(getDuration());
                    } catch (IllegalArgumentException e) {
                        if (isBound()) {
                            unbind();
                        }
                        set(getCycleDuration());
                        throw e;
                    }
                }

                @Override
                public Object getBean() {
                    return RotateTransition.this;
                }

                @Override
                public String getName() {
                    return "duration";
                }
            };
        }
        return duration;
    }

    /**
     * Specifies the axis of rotation for this {@code RotateTransition}. Use
     * {@code node.rotationAxis} for axis of rotation if this {@code axis} is
     * null.
     * <p>
     * It is not possible to change the {@code axis} of a running
     * {@code RotateTransition}. If the value of {@code axis} is changed for a
     * running {@code RotateTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     *
     * @defaultValue null
     */
    private ObjectProperty<Point3D> axis;
    private static final Point3D DEFAULT_AXIS = null;

    public final void setAxis(Point3D value) {
        if ((axis != null) || (value != null /* DEFAULT_AXIS */)) {
            axisProperty().set(value);
        }
    }

    public final Point3D getAxis() {
        return (axis == null)? DEFAULT_AXIS : axis.get();
    }

    public final ObjectProperty<Point3D> axisProperty() {
        if (axis == null) {
            axis = new SimpleObjectProperty<>(this, "axis", DEFAULT_AXIS);
        }
        return axis;
    }

    /**
     * Specifies the start angle value for this {@code RotateTransition}.
     * <p>
     * It is not possible to change {@code fromAngle} of a running
     * {@code RotateTransition}. If the value of {@code fromAngle} is changed
     * for a running {@code RotateTransition}, the animation has to be stopped
     * and started again to pick up the new value.
     *
     * @defaultValue {@code Double.NaN}
     */
    private DoubleProperty fromAngle;
    private static final double DEFAULT_FROM_ANGLE = Double.NaN;

    public final void setFromAngle(double value) {
        if ((fromAngle != null) || (!Double.isNaN(value))) {
            fromAngleProperty().set(value);
        }
    }

    public final double getFromAngle() {
        return (fromAngle == null)? DEFAULT_FROM_ANGLE : fromAngle.get();
    }

    public final DoubleProperty fromAngleProperty() {
        if (fromAngle == null) {
            fromAngle = new SimpleDoubleProperty(this, "fromAngle", DEFAULT_FROM_ANGLE);
        }
        return fromAngle;
    }

    /**
     * Specifies the stop angle value for this {@code RotateTransition}.
     * <p>
     * It is not possible to change {@code toAngle} of a running
     * {@code RotateTransition}. If the value of {@code toAngle} is changed for
     * a running {@code RotateTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     *
     * @defaultValue {@code Double.NaN}
     */
    private DoubleProperty toAngle;
    private static final double DEFAULT_TO_ANGLE = Double.NaN;

    public final void setToAngle(double value) {
        if ((toAngle != null) || (!Double.isNaN(value))) {
            toAngleProperty().set(value);
        }
    }

    public final double getToAngle() {
        return (toAngle == null)? DEFAULT_TO_ANGLE : toAngle.get();
    }

    public final DoubleProperty toAngleProperty() {
        if (toAngle == null) {
            toAngle = new SimpleDoubleProperty(this, "toAngle", DEFAULT_TO_ANGLE);
        }
        return toAngle;
    }

    /**
     * Specifies the incremented stop angle value, from the start, of this
     * {@code RotateTransition}.
     * <p>
     * It is not possible to change {@code byAngle} of a running
     * {@code RotateTransition}. If the value of {@code byAngle} is changed for
     * a running {@code RotateTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     */
    private DoubleProperty byAngle;
    private static final double DEFAULT_BY_ANGLE = 0.0;

    public final void setByAngle(double value) {
        if ((byAngle != null) || (Math.abs(value - DEFAULT_BY_ANGLE) > EPSILON)) {
            byAngleProperty().set(value);
        }
    }

    public final double getByAngle() {
        return (byAngle == null)? DEFAULT_BY_ANGLE : byAngle.get();
    }

    public final DoubleProperty byAngleProperty() {
        if (byAngle == null) {
            byAngle = new SimpleDoubleProperty(this, "byAngle", DEFAULT_BY_ANGLE);
        }
        return byAngle;
    }

    /**
     * The constructor of {@code RotateTransition}
     *
     * @param duration
     *            The duration of the {@code RotateTransition}
     * @param node
     *            The {@code node} which will be rotated
     */
    public RotateTransition(Duration duration, Node node) {
        setDuration(duration);
        setNode(node);
        setCycleDuration(duration);
    }

    /**
     * The constructor of {@code RotateTransition}
     *
     * @param duration
     *            The duration of the {@code RotateTransition}
     */
    public RotateTransition(Duration duration) {
        this(duration, null);
    }

    /**
     * The constructor of {@code RotateTransition}
     *
     */
    public RotateTransition() {
        this(DEFAULT_DURATION, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void interpolate(double frac) {
        cachedNode.setRotate(start + frac * delta);
    }

    private Node getTargetNode() {
        final Node node = getNode();
        return (node != null) ? node : getParentTargetNode();
    }

    @Override
    boolean startable(boolean forceSync) {
        return super.startable(forceSync)
                && ((getTargetNode() != null) || (!forceSync && (cachedNode != null)));
    }

    @Override
    void sync(boolean forceSync) {
        super.sync(forceSync);
        if (forceSync || (cachedNode == null)) {
            cachedNode = getTargetNode();
            final double _fromAngle = getFromAngle();
            final double _toAngle = getToAngle();
            start = (!Double.isNaN(_fromAngle)) ? _fromAngle : cachedNode
                    .getRotate();
            delta = (!Double.isNaN(_toAngle)) ? _toAngle - start : getByAngle();
            final Point3D _axis = getAxis();
            if (_axis != null) {
                node.get().setRotationAxis(_axis);
            }
        }
    }

}
