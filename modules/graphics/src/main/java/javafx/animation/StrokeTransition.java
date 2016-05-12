/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

/**
 * This {@code Transition} creates an animation, that changes the stroke color
 * of a shape over a {@code duration}. This is done by updating the
 * {@code stroke} variable of the {@code shape} at regular intervals.
 * <p>
 * It starts from the {@code fromValue} if provided else uses the {@code shape}
 * 's {@code stroke} value. (The {@code stroke} value has to be a
 * {@link javafx.scene.paint.Color} in this case).
 * <p>
 * It stops at the {@code toValue} value.
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
 *     rect.setFill(null);
 *
 *     StrokeTransition st = new StrokeTransition(Duration.millis(3000), rect, Color.RED, Color.BLUE);
 *     st.setCycleCount(4);
 *     st.setAutoReverse(true);
 *
 *     st.play();
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
public final class StrokeTransition extends Transition {

    private Color start;
    private Color end;

    /**
     * The target shape of this {@code StrokeTransition}.
     * <p>
     * It is not possible to change the target {@code shape} of a running
     * {@code StrokeTransition}. If the value of {@code shape} is changed for a
     * running {@code StrokeTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     */
    private ObjectProperty<Shape> shape;
    private static final Shape DEFAULT_SHAPE = null;

    public final void setShape(Shape value) {
        if ((shape != null) || (value != null /* DEFAULT_SHAPE */)) {
            shapeProperty().set(value);
        }
    }

    public final Shape getShape() {
        return (shape == null)? DEFAULT_SHAPE : shape.get();
    }

    public final ObjectProperty<Shape> shapeProperty() {
        if (shape == null) {
            shape = new SimpleObjectProperty<Shape>(this, "shape", DEFAULT_SHAPE);
        }
        return shape;
    }

    private Shape cachedShape;

    /**
     * The duration of this {@code StrokeTransition}.
     * <p>
     * It is not possible to change the {@code duration} of a running
     * {@code StrokeTransition}. If the value of {@code duration} is changed for
     * a running {@code StrokeTransition}, the animation has to be stopped and
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
                    return StrokeTransition.this;
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
     * Specifies the start color value for this {@code StrokeTransition}.
     * <p>
     * It is not possible to change {@code fromValue} of a running
     * {@code StrokeTransition}. If the value of {@code fromValue} is changed
     * for a running {@code StrokeTransition}, the animation has to be stopped
     * and started again to pick up the new value.
     *
     * @defaultValue {@code null}
     */
    private ObjectProperty<Color> fromValue;
    private static final Color DEFAULT_FROM_VALUE = null;

    public final void setFromValue(Color value) {
        if ((fromValue != null) || (value != null /* DEFAULT_FROM_VALUE */)) {
            fromValueProperty().set(value);
        }
    }

    public final Color getFromValue() {
        return (fromValue == null) ? DEFAULT_FROM_VALUE : fromValue.get();
    }

    public final ObjectProperty<Color> fromValueProperty() {
        if (fromValue == null) {
            fromValue = new SimpleObjectProperty<Color>(this, "fromValue", DEFAULT_FROM_VALUE);
        }
        return fromValue;
    }

    /**
     * Specifies the stop color value for this {@code StrokeTransition}.
     * <p>
     * It is not possible to change {@code toValue} of a running
     * {@code StrokeTransition}. If the value of {@code toValue} is changed for
     * a running {@code StrokeTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     *
     * @defaultValue {@code null}
     */
    private ObjectProperty<Color> toValue;
    private static final Color DEFAULT_TO_VALUE = null;

    public final void setToValue(Color value) {
        if ((toValue != null) || (value != null /* DEFAULT_TO_VALUE */)) {
            toValueProperty().set(value);
        }
    }

    public final Color getToValue() {
        return (toValue == null)? DEFAULT_TO_VALUE : toValue.get();
    }

    public final ObjectProperty<Color> toValueProperty() {
        if (toValue == null) {
            toValue = new SimpleObjectProperty<Color>(this, "toValue", DEFAULT_TO_VALUE);
        }
        return toValue;
    }

/**
     * The constructor of {@code StrokeTransition}
     * @param duration The duration of the {@code StrokeTransition}
     * @param shape The {@code shape} which filling will be animated
     * @param fromValue The start value of the color-animation
     * @param toValue The end value of the color-animation
     */
    public StrokeTransition(Duration duration, Shape shape, Color fromValue,
            Color toValue) {
        setDuration(duration);
        setShape(shape);
        setFromValue(fromValue);
        setToValue(toValue);
        setCycleDuration(duration);
    }

/**
     * The constructor of {@code StrokeTransition}
     * @param duration The duration of the {@code StrokeTransition}
     * @param fromValue The start value of the color-animation
     * @param toValue The end value of the color-animation
     */
    public StrokeTransition(Duration duration, Color fromValue, Color toValue) {
        this(duration, null, fromValue, toValue);
    }

    /**
     * The constructor of {@code StrokeTransition}
     *
     * @param duration
     *            The duration of the {@code StrokeTransition}
     * @param shape
     *            The {@code shape} which stroke paint will be animated
     */
    public StrokeTransition(Duration duration, Shape shape) {
        this(duration, shape, null, null);
    }

    /**
     * The constructor of {@code StrokeTransition}
     *
     * @param duration
     *            The duration of the {@code StrokeTransition}
     */
    public StrokeTransition(Duration duration) {
        this(duration, null);
    }

    /**
     * The constructor of {@code StrokeTransition}
     */
    public StrokeTransition() {
        this(DEFAULT_DURATION, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void interpolate(double frac) {
        final Color newColor = start.interpolate(end, frac);
        cachedShape.setStroke(newColor);
    }

    private Shape getTargetShape() {
        Shape shape = getShape();
        if (shape == null) {
            final Node node = getParentTargetNode();
            if (node instanceof Shape) {
                shape = (Shape) node;
            }
        }
        return shape;
    }

    @Override
    boolean startable(boolean forceSync) {
        if (!super.startable(forceSync)) {
            return false;
        }
        // check if synchronization is not forced and cached values are valid
        if (!forceSync && (cachedShape != null)) {
            return true;
        }

        // we have to synchronize
        final Shape shape = getTargetShape();
        return ((shape != null) // shape is defined?
                && ((getFromValue() != null) || (shape.getStroke() instanceof Color)) // fromValue
                                                                                      // defined
                                                                                      // or
                                                                                      // current
                                                                                      // stroke
                                                                                      // paint
                                                                                      // is
                                                                                      // Color?
        && (getToValue() != null)); // toValue defined?
    }

    @Override
    void sync(boolean forceSync) {
        super.sync(forceSync);
        if (forceSync || (cachedShape == null)) {
            cachedShape = getTargetShape();
            final Color _fromValue = getFromValue();
            start = (_fromValue != null) ? _fromValue : (Color) cachedShape
                    .getStroke();
            end = getToValue();
        }
    }
}
