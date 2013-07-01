/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.animation.transition;

import java.util.ArrayList;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.transform.BaseTransform;

/**
 * Helper class to support opaque geometric path generated for PathMotion
 * Transition.
 *
 * Not part of the public API.
 */
public class AnimationPathHelper {

    private final static int SMOOTH_ZONE = 10;
    private ArrayList<Segment> segments = new ArrayList<Segment>();
    private double totalLength = 0;

    /**
     * Creates an internal flatten representation of path for efficient query
     * of position data along the path.
     * path - path to be snap shot and converted.
     * flatness - the maximum allowable distance between the control points and
     *            the flattened curve.
     * return value - true is creation is successful otherwise false.
     *
     * Implementation detail:
     * The constructor is required to create an internal representation of
     * the given path with the flatness specified. The internal representation
     * needs to keep (a) the flatten path, (b) the normalized length of each
     * segment in the path, and (c) the normal of each point of the segment.
     */
    public AnimationPathHelper(Path2D path,
                               BaseTransform transform,
                               double flatness)
    {
        Segment moveToSeg = Segment.getZeroSegment();
        Segment lastSeg = Segment.getZeroSegment();

        float[] coords = new float[6];
        for (PathIterator i = path.getPathIterator(transform, (float)flatness); !i.isDone(); i.next()) {
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
     * Return the position data along this path at the given alpha value.
     * alpha - a value within [0, 1] that maps to the begin to end of this
     *         path.
     * requiredNormal - specifys normal information is needed.
     * result - returns the computed position if it is not null.
     * return value - result if it is not null otherwise create a new Position2D
     *                object with the computed position information.
     *
     * Implementation detail:
     * The method can do a binary search of the connected segments, possibly
     * an array, key on the normalized length of each segment.
     */
    public Position2D getPosition2D(double alpha, boolean requiredNormal,
            Position2D result) {
        double part = totalLength * Math.min(1, Math.max(0, alpha));
        int segIdx = findSegment(0, segments.size() - 1, part);
        Segment seg = segments.get(segIdx);

        double lengthBefore = seg.accumLength - seg.length;

        double partLength = part - lengthBefore;

        if (result == null) {
            result = new Position2D();
        }

        double ratio = partLength / seg.length;
        Segment prevSeg = seg.prevSeg;
        result.x = prevSeg.toX + (seg.toX - prevSeg.toX) * ratio;
        result.y = prevSeg.toY + (seg.toY - prevSeg.toY) * ratio;
        result.rotateAngle = seg.rotateAngle;

        // provide smooth rotation on segment bounds
        double z = Math.min(SMOOTH_ZONE, seg.length / 2);
        if (partLength < z && !prevSeg.isMoveTo) {
            //interpolate rotation to previous segment
            result.rotateAngle = interpolate(
                    prevSeg.rotateAngle, seg.rotateAngle,
                    partLength / z / 2 + 0.5F);
        } else {
            double dist = seg.length - partLength;
            Segment nextSeg = seg.nextSeg;
            if (dist < z && nextSeg != null) {
                //interpolate rotation to next segment
                if (!nextSeg.isMoveTo) {
                    result.rotateAngle = interpolate(
                            seg.rotateAngle, nextSeg.rotateAngle,
                            (z - dist) / z / 2);
                }
            }
        }
        return result;
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

    private static class Segment {

        private static Segment zeroSegment;
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
            if (zeroSegment == null) {
                zeroSegment = new Segment(true, 0, 0, 0, 0, 0);
            }
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
