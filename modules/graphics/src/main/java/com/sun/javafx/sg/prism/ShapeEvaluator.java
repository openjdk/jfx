/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import java.util.Vector;
import com.sun.javafx.geom.FlatteningPathIterator;
import com.sun.javafx.geom.IllegalPathStateException;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;

/**
 * A {@link KeyFrame} {@link Evaluator} for {@link Shape} objects.
 * This {@code Evaluator} can be used to morph between the geometries
 * of two relatively arbitrary shapes with the only restrictions being
 * that the two different numbers of subpaths or two shapes with
 * disparate winding rules may not blend together in a pleasing
 * manner.
 * The ShapeEvaluator will do the best job it can if the shapes do
 * not match in winding rule or number of subpaths, but the geometry
 * of the shapes may need to be adjusted by other means to make the
 * shapes more like each other for best aesthetic effect.
 * <p>
 * Note that the process of comparing two geometries and finding similar
 * structures between them to blend for the morphing operation can be
 * expensive.
 * Instances of {@code ShapeEvaluator} will properly perform the necessary
 * geometric analysis of their arguments on every method call and attempt
 * to cache the information so that they can operate more quickly if called
 * multiple times in a row on the same pair of {@code Shape} objects.
 * As a result attempting to mutate a {@code Shape} object that is stored
 * in one of their keyframes may not have any effect if the associated
 * {@code ShapeEvaluator} has already cached the geometry.
 * Also, it is advisable to use different instances of {@code ShapeEvaluator}
 * for every pair of keyframes being morphed so that the cached information
 * can be reused as much as possible.
 * <p>
 * An example of proper usage:
 * <pre>
 *     SGShape s = ...;
 *     Shape s0 = ...;
 *     Shape s1 = ...;
 *     Shape s2 = ...;
 *     KeyFrame k0 = KeyFrame.create(0.0f, s0, new ShapeEvaluator());
 *     KeyFrame k1 = KeyFrame.create(0.6f, s1, new ShapeEvaluator());
 *     KeyFrame k2 = KeyFrame.create(1.0f, s2, new ShapeEvaluator());
 *     KeyFrames morphFrames = KeyFrames.create(s, "shape", k0, k1, k2);
 *     Clip.create(5000, 1, morphFrames).start();
 * </pre>
 *
 */
class ShapeEvaluator {
    private Shape savedv0;
    private Shape savedv1;
    private Geometry geom0;
    private Geometry geom1;

    public Shape evaluate(Shape v0, Shape v1, float fraction) {
        if (savedv0 != v0 || savedv1 != v1) {
            if (savedv0 == v1 && savedv1 == v0) {
                // Just swap the geometries
                Geometry gtmp = geom0;
                geom0 = geom1;
                geom1 = gtmp;
            } else {
                recalculate(v0, v1);
            }
            savedv0 = v0;
            savedv1 = v1;
        }
        return getShape(fraction);
    }

    private void recalculate(Shape v0, Shape v1) {
        geom0 = new Geometry(v0);
        geom1 = new Geometry(v1);
        float tvals0[] = geom0.getTvals();
        float tvals1[] = geom1.getTvals();
        float masterTvals[] = mergeTvals(tvals0, tvals1);
        geom0.setTvals(masterTvals);
        geom1.setTvals(masterTvals);
    }

    private Shape getShape(float fraction) {
        return new MorphedShape(geom0, geom1, fraction);
    }

    private static float[] mergeTvals(float tvals0[], float tvals1[]) {
        int count = sortTvals(tvals0, tvals1, null);
        float newtvals[] = new float[count];
        sortTvals(tvals0, tvals1, newtvals);
        return newtvals;
    }

    private static int sortTvals(float tvals0[],
                                 float tvals1[],
                                 float newtvals[])
    {
        int i0 = 0;
        int i1 = 0;
        int numtvals = 0;
        while (i0 < tvals0.length && i1 < tvals1.length) {
            float t0 = tvals0[i0];
            float t1 = tvals1[i1];
            if (t0 <= t1) {
                if (newtvals != null) newtvals[numtvals] = t0;
                i0++;
            }
            if (t1 <= t0) {
                if (newtvals != null) newtvals[numtvals] = t1;
                i1++;
            }
            numtvals++;
        }
        return numtvals;
    }

    private static float interp(float v0, float v1, float t) {
        return (v0 + ((v1 - v0) * t));
    }

    private static class Geometry {
        static final float THIRD = (1f / 3f);
        static final float MIN_LEN = 0.001f;
        float bezierCoords[];
        int numCoords;
        int windingrule;
        float myTvals[];

        public Geometry(Shape s) {
            // Multiple of 6 plus 2 more for initial moveto
            bezierCoords = new float[20];
            PathIterator pi = s.getPathIterator(null);
            windingrule = pi.getWindingRule();
            if (pi.isDone()) {
                // We will have 1 segment and it will be all zeros
                // It will have 8 coordinates (2 for moveto, 6 for cubic)
                numCoords = 8;
            }
            float coords[] = new float[6];
            int type = pi.currentSegment(coords);
            pi.next();
            if (type != PathIterator.SEG_MOVETO) {
                throw new IllegalPathStateException("missing initial moveto");
            }
            float curx, cury, movx, movy;
            bezierCoords[0] = curx = movx = coords[0];
            bezierCoords[1] = cury = movy = coords[1];
            float newx, newy;
            Vector<Point2D> savedpathendpoints = new Vector<Point2D>();
            numCoords = 2;
            while (!pi.isDone()) {
                switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    if (curx != movx || cury != movy) {
                        appendLineTo(curx, cury, movx, movy);
                        curx = movx;
                        cury = movy;
                    }
                    newx = coords[0];
                    newy = coords[1];
                    if (curx != newx || cury != newy) {
                        savedpathendpoints.add(new Point2D(movx, movy));
                        appendLineTo(curx, cury, newx, newy);
                        curx = movx = newx;
                        cury = movy = newy;
                    }
                    break;
                case PathIterator.SEG_CLOSE:
                    if (curx != movx || cury != movy) {
                        appendLineTo(curx, cury, movx, movy);
                        curx = movx;
                        cury = movy;
                    }
                    break;
                case PathIterator.SEG_LINETO:
                    newx = coords[0];
                    newy = coords[1];
                    appendLineTo(curx, cury, newx, newy);
                    curx = newx;
                    cury = newy;
                    break;
                case PathIterator.SEG_QUADTO:
                    float ctrlx = coords[0];
                    float ctrly = coords[1];
                    newx = coords[2];
                    newy = coords[3];
                    appendQuadTo(curx, cury, ctrlx, ctrly, newx, newy);
                    curx = newx;
                    cury = newy;
                    break;
                case PathIterator.SEG_CUBICTO:
                    appendCubicTo(coords[0], coords[1],
                                  coords[2], coords[3],
                                  curx = coords[4], cury = coords[5]);
                    break;
                }
                pi.next();
            }
            // Add closing segment if either:
            // - we only have initial moveto - expand it to an empty cubic
            // - or we are not back to the starting point
            if ((numCoords < 8) || curx != movx || cury != movy) {
                appendLineTo(curx, cury, movx, movy);
                curx = movx;
                cury = movy;
            }
            // Now retrace our way back through all of the connecting
            // inter-subpath segments
            for (int i = savedpathendpoints.size()-1; i >= 0; i--) {
                Point2D p = savedpathendpoints.get(i);
                newx = p.x;
                newy = p.y;
                if (curx != newx || cury != newy) {
                    appendLineTo(curx, cury, newx, newy);
                    curx = newx;
                    cury = newy;
                }
            }
            // Now find the segment endpoint with the smallest Y coordinate
            int minPt = 0;
            float minX = bezierCoords[0];
            float minY = bezierCoords[1];
            for (int ci = 6; ci < numCoords; ci += 6) {
                float x = bezierCoords[ci];
                float y = bezierCoords[ci + 1];
                if (y < minY || (y == minY && x < minX)) {
                    minPt = ci;
                    minX = x;
                    minY = y;
                }
            }
            // If the smallest Y coordinate is not the first coordinate,
            // rotate the points so that it is...
            if (minPt > 0) {
                // Keep in mind that first 2 coords == last 2 coords
                float newCoords[] = new float[numCoords];
                // Copy all coordinates from minPt to the end of the
                // array to the beginning of the new array
                System.arraycopy(bezierCoords, minPt,
                                 newCoords, 0,
                                 numCoords - minPt);
                // Now we do not want to copy 0,1 as they are duplicates
                // of the last 2 coordinates which we just copied.  So
                // we start the source copy at index 2, but we still
                // copy a full minPt coordinates which copies the two
                // coordinates that were at minPt to the last two elements
                // of the array, thus ensuring that thew new array starts
                // and ends with the same pair of coordinates...
                System.arraycopy(bezierCoords, 2,
                                 newCoords, numCoords - minPt,
                                 minPt);
                bezierCoords = newCoords;
            }
            /* Clockwise enforcement:
             * - This technique is based on the formula for calculating
             *   the area of a Polygon.  The standard formula is:
             *   Area(Poly) = 1/2 * sum(x[i]*y[i+1] - x[i+1]y[i])
             * - The returned area is negative if the polygon is
             *   "mostly clockwise" and positive if the polygon is
             *   "mostly counter-clockwise".
             * - One failure mode of the Area calculation is if the
             *   Polygon is self-intersecting.  This is due to the
             *   fact that the areas on each side of the self-intersection
             *   are bounded by segments which have opposite winding
             *   direction.  Thus, those areas will have opposite signs
             *   on the acccumulation of their area summations and end
             *   up canceling each other out partially.
             * - This failure mode of the algorithm in determining the
             *   exact magnitude of the area is not actually a big problem
             *   for our needs here since we are only using the sign of
             *   the resulting area to figure out the overall winding
             *   direction of the path.  If self-intersections cause
             *   different parts of the path to disagree as to the
             *   local winding direction, that is no matter as we just
             *   wait for the final answer to tell us which winding
             *   direction had greater representation.  If the final
             *   result is zero then the path was equal parts clockwise
             *   and counter-clockwise and we do not care about which
             *   way we order it as either way will require half of the
             *   path to unwind and re-wind itself.
             */
            float area = 0;
            // Note that first and last points are the same so we
            // do not need to process coords[0,1] against coords[n-2,n-1]
            curx = bezierCoords[0];
            cury = bezierCoords[1];
            for (int i = 2; i < numCoords; i += 2) {
                newx = bezierCoords[i];
                newy = bezierCoords[i + 1];
                area += curx * newy - newx * cury;
                curx = newx;
                cury = newy;
            }
            if (area < 0) {
                /* The area is negative so the shape was clockwise
                 * in a Euclidean sense.  But, our screen coordinate
                 * systems have the origin in the upper left so they
                 * are flipped.  Thus, this path "looks" ccw on the
                 * screen so we are flipping it to "look" clockwise.
                 * Note that the first and last points are the same
                 * so we do not need to swap them.
                 * (Not that it matters whether the paths end up cw
                 *  or ccw in the end as long as all of them are the
                 *  same, but above we called this section "Clockwise
                 *  Enforcement", so we do not want to be liars. ;-)
                 */
                // Note that [0,1] do not need to be swapped with [n-2,n-1]
                // So first pair to swap is [2,3] and [n-4,n-3]
                int i = 2;
                int j = numCoords - 4;
                while (i < j) {
                    curx = bezierCoords[i];
                    cury = bezierCoords[i + 1];
                    bezierCoords[i] = bezierCoords[j];
                    bezierCoords[i + 1] = bezierCoords[j + 1];
                    bezierCoords[j] = curx;
                    bezierCoords[j + 1] = cury;
                    i += 2;
                    j -= 2;
                }
            }
        }

        private void appendLineTo(float x0, float y0,
                                  float x1, float y1)
        {
            appendCubicTo(// A third of the way from xy0 to xy1:
                        interp(x0, x1, THIRD),
                        interp(y0, y1, THIRD),
                        // A third of the way from xy1 back to xy0:
                        interp(x1, x0, THIRD),
                        interp(y1, y0, THIRD),
                        x1, y1);
        }

        private void appendQuadTo(float x0, float y0,
                                  float ctrlx, float ctrly,
                                  float x1, float y1)
        {
            appendCubicTo(// A third of the way from ctrlxy back to xy0:
                        interp(ctrlx, x0, THIRD),
                        interp(ctrly, y0, THIRD),
                        // A third of the way from ctrlxy to xy1:
                        interp(ctrlx, x1, THIRD),
                        interp(ctrly, y1, THIRD),
                        x1, y1);
        }

        private void appendCubicTo(float ctrlx1, float ctrly1,
                                   float ctrlx2, float ctrly2,
                                   float x1, float y1)
        {
            if (numCoords + 6 > bezierCoords.length) {
                // Keep array size to a multiple of 6 plus 2
                int newsize = (numCoords - 2) * 2 + 2;
                float newCoords[] = new float[newsize];
                System.arraycopy(bezierCoords, 0, newCoords, 0, numCoords);
                bezierCoords = newCoords;
            }
            bezierCoords[numCoords++] = ctrlx1;
            bezierCoords[numCoords++] = ctrly1;
            bezierCoords[numCoords++] = ctrlx2;
            bezierCoords[numCoords++] = ctrly2;
            bezierCoords[numCoords++] = x1;
            bezierCoords[numCoords++] = y1;
        }

        public int getWindingRule() {
            return windingrule;
        }

        public int getNumCoords() {
            return numCoords;
        }

        public float getCoord(int i) {
            return bezierCoords[i];
        }

        public float[] getTvals() {
            if (myTvals != null) {
                return myTvals;
            }

            // assert(numCoords >= 8);
            // assert(((numCoords - 2) % 6) == 0);
            float tvals[] = new float[(numCoords - 2) / 6 + 1];

            // First calculate total "length" of path
            // Length of each segment is averaged between
            // the length between the endpoints (a lower bound for a cubic)
            // and the length of the control polygon (an upper bound)
            float segx = bezierCoords[0];
            float segy = bezierCoords[1];
            float tlen = 0;
            int ci = 2;
            int ti = 0;
            while (ci < numCoords) {
                float prevx, prevy, newx, newy;
                prevx = segx;
                prevy = segy;
                newx = bezierCoords[ci++];
                newy = bezierCoords[ci++];
                prevx -= newx;
                prevy -= newy;
                float len = (float) Math.sqrt(prevx * prevx + prevy * prevy);
                prevx = newx;
                prevy = newy;
                newx = bezierCoords[ci++];
                newy = bezierCoords[ci++];
                prevx -= newx;
                prevy -= newy;
                len += (float) Math.sqrt(prevx * prevx + prevy * prevy);
                prevx = newx;
                prevy = newy;
                newx = bezierCoords[ci++];
                newy = bezierCoords[ci++];
                prevx -= newx;
                prevy -= newy;
                len += (float) Math.sqrt(prevx * prevx + prevy * prevy);
                // len is now the total length of the control polygon
                segx -= newx;
                segy -= newy;
                len += (float) Math.sqrt(segx * segx + segy * segy);
                // len is now sum of linear length and control polygon length
                len /= 2;
                // len is now average of the two lengths

                /* If the result is zero length then we will have problems
                 * below trying to do the math and bookkeeping to split
                 * the segment or pair it against the segments in the
                 * other shape.  Since these lengths are just estimates
                 * to map the segments of the two shapes onto corresponding
                 * segments of "approximately the same length", we will
                 * simply modify the length of this segment to be at least
                 * a minimum value and it will simply grow from zero or
                 * near zero length to a non-trivial size as it morphs.
                 */
                if (len < MIN_LEN) {
                    len = MIN_LEN;
                }
                tlen += len;
                tvals[ti++] = tlen;
                segx = newx;
                segy = newy;
            }

            // Now set tvals for each segment to its proportional
            // part of the length
            float prevt = tvals[0];
            tvals[0] = 0;
            for (ti = 1; ti < tvals.length - 1; ti++) {
                float nextt = tvals[ti];
                tvals[ti] = prevt / tlen;
                prevt = nextt;
            }
            tvals[ti] = 1;
            return (myTvals = tvals);
        }

        public void setTvals(float newTvals[]) {
            float oldCoords[] = bezierCoords;
            float newCoords[] = new float[2 + (newTvals.length - 1) * 6];
            float oldTvals[] = getTvals();
            int oldci = 0;
            float x0, xc0, xc1, x1;
            float y0, yc0, yc1, y1;
            x0 = xc0 = xc1 = x1 = oldCoords[oldci++];
            y0 = yc0 = yc1 = y1 = oldCoords[oldci++];
            int newci = 0;
            newCoords[newci++] = x0;
            newCoords[newci++] = y0;
            float t0 = 0;
            float t1 = 0;
            int oldti = 1;
            int newti = 1;
            while (newti < newTvals.length) {
                if (t0 >= t1) {
                    x0 = x1;
                    y0 = y1;
                    xc0 = oldCoords[oldci++];
                    yc0 = oldCoords[oldci++];
                    xc1 = oldCoords[oldci++];
                    yc1 = oldCoords[oldci++];
                    x1 = oldCoords[oldci++];
                    y1 = oldCoords[oldci++];
                    t1 = oldTvals[oldti++];
                }
                float nt = newTvals[newti++];
                // assert(nt > t0);
                if (nt < t1) {
                    // Make nt proportional to [t0 => t1] range
                    float relt = (nt - t0) / (t1 - t0);
                    newCoords[newci++] = x0 = interp(x0, xc0, relt);
                    newCoords[newci++] = y0 = interp(y0, yc0, relt);
                    xc0 = interp(xc0, xc1, relt);
                    yc0 = interp(yc0, yc1, relt);
                    xc1 = interp(xc1, x1, relt);
                    yc1 = interp(yc1, y1, relt);
                    newCoords[newci++] = x0 = interp(x0, xc0, relt);
                    newCoords[newci++] = y0 = interp(y0, yc0, relt);
                    xc0 = interp(xc0, xc1, relt);
                    yc0 = interp(yc0, yc1, relt);
                    newCoords[newci++] = x0 = interp(x0, xc0, relt);
                    newCoords[newci++] = y0 = interp(y0, yc0, relt);
                } else {
                    newCoords[newci++] = xc0;
                    newCoords[newci++] = yc0;
                    newCoords[newci++] = xc1;
                    newCoords[newci++] = yc1;
                    newCoords[newci++] = x1;
                    newCoords[newci++] = y1;
                }
                t0 = nt;
            }
            bezierCoords = newCoords;
            numCoords = newCoords.length;
            myTvals = newTvals;
        }
    }

    private static class MorphedShape extends Shape {
        Geometry geom0;
        Geometry geom1;
        float t;

        MorphedShape(Geometry geom0, Geometry geom1, float t) {
            this.geom0 = geom0;
            this.geom1 = geom1;
            this.t = t;
        }

        public Rectangle getRectangle() {
            return new Rectangle(getBounds());
        }

        public RectBounds getBounds() {
            int n = geom0.getNumCoords();
            float xmin, ymin, xmax, ymax;
            xmin = xmax = interp(geom0.getCoord(0), geom1.getCoord(0), t);
            ymin = ymax = interp(geom0.getCoord(1), geom1.getCoord(1), t);
            for (int i = 2; i < n; i += 2) {
                float x = interp(geom0.getCoord(i), geom1.getCoord(i), t);
                float y = interp(geom0.getCoord(i+1), geom1.getCoord(i+1), t);
                if (xmin > x) {
                    xmin = x;
                }
                if (ymin > y) {
                    ymin = y;
                }
                if (xmax < x) {
                    xmax = x;
                }
                if (ymax < y) {
                    ymax = y;
                }
            }
            return new RectBounds(xmin, ymin, xmax, ymax);
        }

        public boolean contains(float x, float y) {
            return Path2D.contains(getPathIterator(null), x, y);
        }

        public boolean intersects(float x, float y, float w, float h) {
            return Path2D.intersects(getPathIterator(null), x, y, w, h);
        }

        public boolean contains(float x, float y, float width, float height) {
            return Path2D.contains(getPathIterator(null), x, y, width, height);
        }

        public PathIterator getPathIterator(BaseTransform at) {
            return new Iterator(at, geom0, geom1, t);
        }

        public PathIterator getPathIterator(BaseTransform at, float flatness) {
            return new FlatteningPathIterator(getPathIterator(at), flatness);
        }

        public Shape copy() {
            return new Path2D(this);
        }
    }

    private static class Iterator implements PathIterator {
        BaseTransform at;
        Geometry g0;
        Geometry g1;
        float t;
        int cindex;

        public Iterator(BaseTransform at,
                        Geometry g0, Geometry g1,
                        float t) {
            this.at = at;
            this.g0 = g0;
            this.g1 = g1;
            this.t = t;
        }

        /**
         * @{inheritDoc}
         */
        public int getWindingRule() {
            return (t < 0.5 ? g0.getWindingRule() : g1.getWindingRule());
        }

        /**
         * @{inheritDoc}
         */
        public boolean isDone() {
            return (cindex > g0.getNumCoords());
        }

        /**
         * @{inheritDoc}
         */
        public void next() {
            if (cindex == 0) {
                cindex = 2;
            } else {
                cindex += 6;
            }
        }

        /**
         * @{inheritDoc}
         */
        public int currentSegment(float coords[]) {
            int type;
            int n;
            if (cindex == 0) {
                type = SEG_MOVETO;
                n = 2;
            } else if (cindex >= g0.getNumCoords()) {
                type = SEG_CLOSE;
                n = 0;
            } else {
                type = SEG_CUBICTO;
                n = 6;
            }
            if (n > 0) {
                for (int i = 0; i < n; i++) {
                    coords[i] = (float) interp(g0.getCoord(cindex + i),
                                               g1.getCoord(cindex + i),
                                               t);
                }
                if (at != null) {
                    at.transform(coords, 0, coords, 0, n / 2);
                }
            }
            return type;
        }
    }
}
