/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import com.sun.javafx.animation.transition.AnimationPathHelper;
import com.sun.javafx.animation.transition.Position2D;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.transform.BaseTransform;

/**
 * This {@code Transition} creates a path animation that spans its
 * {@link #duration}. The translation along the path is done by updating the
 * {@code translateX} and {@code translateY} variables of the {@code node}, and
 * the {@code rotate} variable will get updated if {@code orientation} is set to
 * {@code OrientationType.ORTHOGONAL_TO_TANGENT}, at regular interval.
 * <p>
 * The animated path is defined by the outline of a shape.
 * 
 * <p>
 * Code Segment Example:
 * </p>
 * 
 * <pre>
 * <code>
 * import javafx.scene.shape.*;
 * import javafx.animation.transition.*;
 * 
 * ...
 * 
 *     Rectangle rect = new Rectangle (100, 40, 100, 100);
 *     rect.setArcHeight(50);
 *     rect.setArcWidth(50);
 *     rect.setFill(Color.VIOLET);
 * 
 * 
 *     Path path = new Path();
 *     path.getElements().add (new MoveTo (0f, 50f));
 *     path.getElements().add (new CubicCurveTo (40f, 10f, 390f, 240f, 1904, 50f));
 * 
 *     pathTransition.setDuration(Duration.millis(10000));
 *     pathTransition.setNode(rect);
 *     pathTransition.setPath(path);
 *     pathTransition.setOrientation(OrientationType.ORTHOGONAL_TO_TANGENT);
 *     pathTransition.setCycleCount(4f);
 *     pathTransition.setAutoReverse(true);
 * 
 *     pathTransition.play();
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
public final class PathTransition extends Transition {

    /**
     * The target node of this {@code PathTransition}.
     * <p>
     * It is not possible to change the target {@code node} of a running
     * {@code PathTransition}. If the value of {@code node} is changed for a
     * running {@code PathTransition}, the animation has to be stopped and
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
     * The duration of this {@code Transition}.
     * <p>
     * It is not possible to change the {@code duration} of a running
     * {@code PathTransition}. If the value of {@code duration} is changed for a
     * running {@code PathTransition}, the animation has to be stopped and
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
                    return PathTransition.this;
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
     * The shape on which outline the node should be animated.
     * <p>
     * It is not possible to change the {@code path} of a running
     * {@code PathTransition}. If the value of {@code path} is changed for a
     * running {@code PathTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     * 
     * @defaultValue null
     */
    private ObjectProperty<Shape> path;
    private static final Shape DEFAULT_PATH = null;

    public final void setPath(Shape value) {
        if ((path != null) || (value != null /* DEFAULT_PATH */)) {
            pathProperty().set(value);
        }
    }

    public final Shape getPath() {
        return (path == null)? DEFAULT_PATH : path.get();
    }

    public final ObjectProperty<Shape> pathProperty() {
        if (path == null) {
            path = new SimpleObjectProperty<Shape>(this, "path", DEFAULT_PATH);
        }
        return path;
    }

    /**
     * Specifies the upright orientation of {@code node} along the {@code path}.
     * @since JavaFX 2.0
     */
    public static enum OrientationType {

        /**
         * The targeted {@code node}'s rotation matrix stays unchange along the
         * geometric path.
         */
        NONE,

        /**
         * The targeted node's rotation matrix is set to keep {@code node}
         * perpendicular to the path's tangent along the geometric path.
         */
        ORTHOGONAL_TO_TANGENT
    }

    /**
     * Specifies the upright orientation of {@code node} along the {@code path}.
     * The default orientation is set to {@link OrientationType#NONE}.
     * <p>
     * It is not possible to change the {@code orientation} of a running
     * {@code PathTransition}. If the value of {@code orientation} is changed
     * for a running {@code PathTransition}, the animation has to be stopped and
     * started again to pick up the new value.
     * 
     * @defaultValue NONE
     */
    private ObjectProperty<OrientationType> orientation;
    private static final OrientationType DEFAULT_ORIENTATION = OrientationType.NONE;

    public final void setOrientation(OrientationType value) {
        if ((orientation != null) || (!DEFAULT_ORIENTATION.equals(value))) {
            orientationProperty().set(value);
        }
    }

    public final OrientationType getOrientation() {
        return (orientation == null)? OrientationType.NONE : orientation.get();
    }

    public final ObjectProperty<OrientationType> orientationProperty() {
        if (orientation == null) {
            orientation = new SimpleObjectProperty<OrientationType>(this, "orientation", DEFAULT_ORIENTATION);
        }
        return orientation;
    }

    private boolean cachedIsNormalRequired;

    private final Position2D posResult = new Position2D();
    private AnimationPathHelper apHelper;

    /**
     * The constructor of {@code PathTransition}.
     * 
     * @param duration
     *            The {@link #duration} of this {@code PathTransition}
     * @param path
     *            The {@link #path} of this {@code PathTransition}
     * @param node
     *            The {@link #node} of this {@code PathTransition}
     */
    public PathTransition(Duration duration, Shape path, Node node) {
        setDuration(duration);
        setPath(path);
        setNode(node);
        setCycleDuration(duration);
    }

    /**
     * The constructor of {@code PathTransition}.
     * 
     * @param duration
     *            The {@link #duration} of this {@code PathTransition}
     * @param path
     *            The {@link #path} of this {@code PathTransition}
     */
    public PathTransition(Duration duration, Shape path) {
        this(duration, path, null);
    }

    /**
     * The constructor of {@code PathTransition}.
     */
    public PathTransition() {
        this(DEFAULT_DURATION, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void interpolate(double frac) {
        apHelper.getPosition2D(frac, cachedIsNormalRequired, posResult);
        cachedNode.setTranslateX(posResult.x - cachedNode.impl_getPivotX());
        cachedNode.setTranslateY(posResult.y - cachedNode.impl_getPivotY());
        // Need to handle orientation if it is requested
        if (cachedIsNormalRequired) {
            cachedNode.setRotate(posResult.rotateAngle);
        }
    }

    private Node getTargetNode() {
        final Node node = getNode();
        return (node != null) ? node : getParentTargetNode();
    }

    @Override
    boolean impl_startable(boolean forceSync) {
        return super.impl_startable(forceSync)
                && (((getTargetNode() != null) && (getPath() != null) && !getPath().getLayoutBounds().isEmpty()) || (!forceSync
                        && (cachedNode != null) && (apHelper != null)));
    }

    @Override
    void impl_sync(boolean forceSync) {
        super.impl_sync(forceSync);
        if (forceSync || (cachedNode == null)) {
            cachedNode = getTargetNode();
            final Shape path = getPath();
            final Path2D path2D = new Path2D(path.impl_configShape());
            final BaseTransform tx = path.impl_getLeafTransform();
            apHelper = new AnimationPathHelper(path2D, tx, 1.0);
            cachedIsNormalRequired = getOrientation() == OrientationType.ORTHOGONAL_TO_TANGENT;
        }
    }

}
