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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.shape.ShapeHelper;
import java.util.ArrayList;

/**
 * This {@code Transition} creates a path animation that spans its
 * {@link #durationProperty() duration}. The translation along the path is done by updating the
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
 * import javafx.animation.*;
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
    private double totalLength = 0;
    private final ArrayList<Segment> segments = new ArrayList<>();

    private static final Node DEFAULT_NODE = null;
    private static final int SMOOTH_ZONE = 10;

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
            path = new SimpleObjectProperty<>(this, "path", DEFAULT_PATH);
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
            orientation = new SimpleObjectProperty<>(this, "orientation", DEFAULT_ORIENTATION);
        }
        return orientation;
    }

    private boolean cachedIsNormalRequired;

    /**
     * The constructor of {@code PathTransition}.
     *
     * @param duration
     *            The {@link #durationProperty() duration} of this {@code PathTransition}
     * @param path
     *            The {@link #pathProperty() path} of this {@code PathTransition}
     * @param node
     *            The {@link #nodeProperty() node} of this {@code PathTransition}
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
     *            The {@link #durationProperty() duration} of this {@code PathTransition}
     * @param path
     *            The {@link #pathProperty() path} of this {@code PathTransition}
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
        double part = totalLength * Math.min(1, Math.max(0, frac));
        int segIdx = findSegment(0, segments.size() - 1, part);
        Segment seg = segments.get(segIdx);

        double lengthBefore = seg.accumLength - seg.length;

        double partLength = part - lengthBefore;

        double ratio = partLength / seg.length;
        Segment prevSeg = seg.prevSeg;
        double x = prevSeg.toX + (seg.toX - prevSeg.toX) * ratio;
        double y = prevSeg.toY + (seg.toY - prevSeg.toY) * ratio;
        double rotateAngle = seg.rotateAngle;

        // provide smooth rotation on segment bounds
        double z = Math.min(SMOOTH_ZONE, seg.length / 2);
        if (partLength < z && !prevSeg.isMoveTo) {
            //interpolate rotation to previous segment
            rotateAngle = interpolate(
                    prevSeg.rotateAngle, seg.rotateAngle,
                    partLength / z / 2 + 0.5F);
        } else {
            double dist = seg.length - partLength;
            Segment nextSeg = seg.nextSeg;
            if (dist < z && nextSeg != null) {
                //interpolate rotation to next segment
                if (!nextSeg.isMoveTo) {
                    rotateAngle = interpolate(
                            seg.rotateAngle, nextSeg.rotateAngle,
                            (z - dist) / z / 2);
                }
            }
        }
        cachedNode.setTranslateX(x - NodeHelper.getPivotX(cachedNode));
        cachedNode.setTranslateY(y - NodeHelper.getPivotY(cachedNode));
        // Need to handle orientation if it is requested
        if (cachedIsNormalRequired) {
            cachedNode.setRotate(rotateAngle);
        }
    }

    private Node getTargetNode() {
        final Node node = getNode();
        return (node != null) ? node : getParentTargetNode();
    }

    @Override
    boolean startable(boolean forceSync) {
        return super.startable(forceSync)
                && (((getTargetNode() != null) && (getPath() != null) && !getPath().getLayoutBounds().isEmpty()) || (!forceSync
                        && (cachedNode != null)));
    }

    @Override
    void sync(boolean forceSync) {
        super.sync(forceSync);
        if (forceSync || (cachedNode == null)) {
            cachedNode = getTargetNode();
            recomputeSegments();
            cachedIsNormalRequired = getOrientation() == OrientationType.ORTHOGONAL_TO_TANGENT;
        }
    }

    private void recomputeSegments() {
        segments.clear();
        final Shape p = getPath();
        Segment moveToSeg = Segment.getZeroSegment();
        Segment lastSeg = Segment.getZeroSegment();

        float[] coords = new float[6];
        for (PathIterator i = ShapeHelper.configShape(p).getPathIterator(NodeHelper.getLeafTransform(p), 1.0f); !i.isDone(); i.next()) {
            Segment newSeg = null;
            int segType = i.currentSegment(coords);
            double x = coords[0];
            double y = coords[1];

            switch (segType) {
                case PathIterator.SEG_MOVETO:
                    moveToSeg = Segment.newMoveTo(x, y, lastSeg.accumLength);
                    newSeg = moveToSeg;
                    break;
                case PathIterator.SEG_CLOSE:
                    newSeg = Segment.newClosePath(lastSeg, moveToSeg);
                    if (newSeg == null) {
                        // make the last segment to close the path
                        lastSeg.convertToClosePath(moveToSeg);
                    }
                    break;
                case PathIterator.SEG_LINETO:
                    newSeg = Segment.newLineTo(lastSeg, x, y);
                    break;
            }

            if (newSeg != null) {
                segments.add(newSeg);
                lastSeg = newSeg;
            }
        }
        totalLength = lastSeg.accumLength;
    }

    /**
     * Returns the index of the first segment having accumulated length
     * from the path beginning, greater than {@code length}
     */
    private int findSegment(int begin, int end, double length) {
        // check for search termination
        if (begin == end) {
            // find last non-moveTo segment for given length
            return segments.get(begin).isMoveTo && begin > 0
                    ? findSegment(begin - 1, begin - 1, length)
                    : begin;
        }
        // otherwise continue binary search
        int middle = begin + (end - begin) / 2;
        return segments.get(middle).accumLength > length
                ? findSegment(begin, middle, length)
                : findSegment(middle + 1, end, length);
    }


    /** Interpolates angle according to rate,
     *  with correct 0->360 and 360->0 transitions
     */
    private static double interpolate(double fromAngle, double toAngle, double ratio) {
        double delta = toAngle - fromAngle;
        if (Math.abs(delta) > 180) {
            toAngle += delta > 0 ? -360 : 360;
        }
        return normalize(fromAngle + ratio * (toAngle - fromAngle));
    }

    /** Converts angle to range 0-360
     */
    private static double normalize(double angle) {
        while (angle > 360) {
            angle -= 360;
        }
        while (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    private static class Segment {

        private static final Segment zeroSegment = new Segment(true, 0, 0, 0, 0, 0);
        boolean isMoveTo;
        double length;
        // total length from the path's beginning to the end of this segment
        double accumLength;
        // end point of this segment
        double toX;
        double toY;
        // segment's rotation angle in degrees
        double rotateAngle;
        Segment prevSeg;
        Segment nextSeg;

        private Segment(boolean isMoveTo, double toX, double toY,
                double length, double lengthBefore, double rotateAngle) {
            this.isMoveTo = isMoveTo;
            this.toX = toX;
            this.toY = toY;
            this.length = length;
            this.accumLength = lengthBefore + length;
            this.rotateAngle = rotateAngle;
        }

        public static Segment getZeroSegment() {
            return zeroSegment;
        }

        public static Segment newMoveTo(double toX, double toY,
                double accumLength) {
            return new Segment(true, toX, toY, 0, accumLength, 0);
        }

        public static Segment newLineTo(Segment fromSeg, double toX, double toY) {
            double deltaX = toX - fromSeg.toX;
            double deltaY = toY - fromSeg.toY;
            double length = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
            if ((length >= 1) || fromSeg.isMoveTo) { // filtering out flattening noise
                double sign = Math.signum(deltaY == 0 ? deltaX : deltaY);
                double angle = (sign * Math.acos(deltaX / length));
                angle = normalize(angle / Math.PI * 180);
                Segment newSeg = new Segment(false, toX, toY,
                        length, fromSeg.accumLength, angle);
                fromSeg.nextSeg = newSeg;
                newSeg.prevSeg = fromSeg;
                return newSeg;
            }
            return null;
        }

        public static Segment newClosePath(Segment fromSeg, Segment moveToSeg) {
            Segment newSeg = newLineTo(fromSeg, moveToSeg.toX, moveToSeg.toY);
            if (newSeg != null) {
                newSeg.convertToClosePath(moveToSeg);
            }
            return newSeg;
        }

        public void convertToClosePath(Segment moveToSeg) {
            Segment firstLineToSeg = moveToSeg.nextSeg;
            nextSeg = firstLineToSeg;
            firstLineToSeg.prevSeg = this;
        }

    }

}
