/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * This {@code Transition} creates a fade effect animation that spans its
 * {@code duration}. This is done by updating the {@code opacity} variable of
 * the {@code node} at regular interval.
 * <p>
 * It starts from the {@code fromValue} if provided else uses the {@code node}'s
 * {@code opacity} value.
 * <p>
 * It stops at the {@code toValue} value if provided else it will use start
 * value plus {@code byValue}.
 * <p>
 * The {@code toValue} takes precedence if both {@code toValue} and
 * {@code byValue} are specified.
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
 *     FadeTransition ft = new FadeTransition(Duration.millis(3000), rect);
 *     ft.setFromValue(1.0);
 *     ft.setToValue(0.3);
 *     ft.setCycleCount(4);
 *     ft.setAutoReverse(true);
 *
 *     ft.play();
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
public final class FadeTransition extends Transition {
    private static final double EPSILON = 1e-12;

    private double start;
    private double delta;

    /**
     * The target node of this {@code Transition}.
     * <p>
     * It is not possible to change the target {@code node} of a running
     * {@code FadeTransition}. If the value of {@code node} is changed for a
     * running {@code FadeTransition}, the animation has to be stopped and
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
            node = new SimpleObjectProperty<Node>(this, "node", DEFAULT_NODE);
        }
        return node;
    }

    private Node cachedNode;

    /**
     * The duration of this {@code FadeTransition}.
     * <p>
     * It is not possible to change the {@code duration} of a running
     * {@code FadeTransition}. If the value of {@code duration} is changed for a
     * running {@code FadeTransition}, the animation has to be stopped and
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
                    return FadeTransition.this;
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
     * Specifies the start opacity value for this {@code FadeTransition}.
     * <p>
     * It is not possible to change {@code fromValue} of a running
     * {@code FadeTransition}. If the value of {@code fromValue} is changed for
     * a running {@code FadeTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     *
     * @defaultValue {@code Double.NaN}
     */
    private DoubleProperty fromValue;
    private static final double DEFAULT_FROM_VALUE = Double.NaN;

    public final void setFromValue(double value) {
        if ((fromValue != null) || (!Double.isNaN(value) /* DEFAULT_FROM_VALUE */ )) {
            fromValueProperty().set(value);
        }
    }

    public final double getFromValue() {
        return (fromValue == null)? DEFAULT_FROM_VALUE : fromValue.get();
    }

    public final DoubleProperty fromValueProperty() {
        if (fromValue == null) {
            fromValue = new SimpleDoubleProperty(this, "fromValue", DEFAULT_FROM_VALUE);
        }
        return fromValue;
    }

    /**
     * Specifies the stop opacity value for this {@code FadeTransition}.
     * <p>
     * It is not possible to change {@code toValue} of a running
     * {@code FadeTransition}. If the value of {@code toValue} is changed for a
     * running {@code FadeTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     *
     * @defaultValue {@code Double.NaN}
     */
    private DoubleProperty toValue;
    private static final double DEFAULT_TO_VALUE = Double.NaN;

    public final void setToValue(double value) {
        if ((toValue != null) || (!Double.isNaN(value))) {
            toValueProperty().set(value);
        }
    }

    public final double getToValue() {
        return (toValue == null)? DEFAULT_TO_VALUE : toValue.get();
    }

    public final DoubleProperty toValueProperty() {
        if (toValue == null) {
            toValue = new SimpleDoubleProperty(this, "toValue", DEFAULT_TO_VALUE);
        }
        return toValue;
    }

    /**
     * Specifies the incremented stop opacity value, from the start, of this
     * {@code FadeTransition}.
     * <p>
     * It is not possible to change {@code byValue} of a running
     * {@code FadeTransition}. If the value of {@code byValue} is changed for a
     * running {@code FadeTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     */
    private DoubleProperty byValue;
    private static final double DEFAULT_BY_VALUE = 0.0;

    public final void setByValue(double value) {
        if ((byValue != null) || (Math.abs(value - DEFAULT_BY_VALUE) > EPSILON)) {
            byValueProperty().set(value);
        }
    }

    public final double getByValue() {
        return (byValue == null)? DEFAULT_BY_VALUE : byValue.get();
    }

    public final DoubleProperty byValueProperty() {
        if (byValue == null) {
            byValue = new SimpleDoubleProperty(this, "byValue", DEFAULT_BY_VALUE);
        }
        return byValue;
    }

    /**
     * The constructor of {@code FadeTransition}
     *
     * @param duration
     *            The duration of the {@code FadeTransition}
     * @param node
     *            The {@code node} which opacity will be animated
     */
    public FadeTransition(Duration duration, Node node) {
        setDuration(duration);
        setNode(node);
        setCycleDuration(duration);
    }

    /**
     * The constructor of {@code FadeTransition}
     *
     * @param duration
     *            The duration of the {@code FadeTransition}
     */
    public FadeTransition(Duration duration) {
        this(duration, null);
    }

    /**
     * The constructor of {@code FadeTransition}
     */
    public FadeTransition() {
        this(DEFAULT_DURATION, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void interpolate(double frac) {
        final double newOpacity = Math.max(0.0,
                Math.min(start + frac * delta, 1.0));
        cachedNode.setOpacity(newOpacity);
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
            final double _fromValue = getFromValue();
            final double _toValue = getToValue();
            start = (!Double.isNaN(_fromValue)) ? Math.max(0,
                    Math.min(_fromValue, 1)) : cachedNode.getOpacity();
            delta = (!Double.isNaN(_toValue)) ? _toValue - start : getByValue();
            if (start + delta > 1.0) {
                delta = 1.0 - start;
            } else if (start + delta < 0.0) {
                delta = -start;
            }
        }
    }
}
