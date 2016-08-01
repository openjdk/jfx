/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.geom;

import com.sun.javafx.geom.transform.BaseTransform;

/**
 * The <code>Shape</code> class provides definitions for objects
 * that represent some form of geometric shape. The <code>Shape</code>
 * is described by a {@link PathIterator} object, which can express the
 * outline of the <code>Shape</code> as well as a rule for determining
 * how the outline divides the 2D plane into interior and exterior
 * points. Each <code>Shape</code> object provides callbacks to get the
 * bounding box of the geometry, determine whether points or
 * rectangles lie partly or entirely within the interior
 * of the <code>Shape</code>, and retrieve a <code>PathIterator</code>
 * object that describes the trajectory path of the <code>Shape</code>
 * outline.
 * <p>
 * <b>Definition of insideness:</b>
 * A point is considered to lie inside a
 * <code>Shape</code> if and only if:
 * <ul>
 * <li> it lies completely
 * inside the<code>Shape</code> boundary <i>or</i>
 * <li>
 * it lies exactly on the <code>Shape</code> boundary <i>and</i> the
 * space immediately adjacent to the
 * point in the increasing <code>X</code> direction is
 * entirely inside the boundary <i>or</i>
 * <li>
 * it lies exactly on a horizontal boundary segment <b>and</b> the
 * space immediately adjacent to the point in the
 * increasing <code>Y</code> direction is inside the boundary.
 * </ul>
 * <p>The <code>contains</code> and <code>intersects</code> methods
 * consider the interior of a <code>Shape</code> to be the area it
 * encloses as if it were filled.  This means that these methods
 * consider
 * unclosed shapes to be implicitly closed for the purpose of
 * determining if a shape contains or intersects a rectangle or if a
 * shape contains a point.
 *
 * @see com.sun.javafx.geom.PathIterator
 * @see com.sun.javafx.geom.transform.BaseTransform
 * @see com.sun.javafx.geom.FlatteningPathIterator
 * @see com.sun.javafx.geom.Path2D
 *
 * @version 1.19 06/24/98
 */
public abstract class Shape {
    /**
     * Note that there is no guarantee that the returned
     * {@link RectBounds} is the smallest bounding box that encloses
     * the <code>Shape</code>, only that the <code>Shape</code> lies
     * entirely within the indicated <code>RectBounds</code>.
     * @return an instance of <code>RectBounds</code>
     */
    public abstract RectBounds getBounds();

    /**
     * Tests if the specified coordinates are inside the boundary of the
     * <code>Shape</code>.
     * @param x the specified X coordinate to be tested
     * @param y the specified Y coordinate to be tested
     * @return <code>true</code> if the specified coordinates are inside
     *         the <code>Shape</code> boundary; <code>false</code>
     *         otherwise.
     */
    public abstract boolean contains(float x, float y);

    /**
     * Tests if a specified {@link Point2D} is inside the boundary
     * of the <code>Shape</code>.
     * @param p the specified <code>Point2D</code> to be tested
     * @return <code>true</code> if the specified <code>Point2D</code> is
     *          inside the boundary of the <code>Shape</code>;
     *      <code>false</code> otherwise.
     */
    public boolean contains(Point2D p) {
        return contains(p.x, p.y);
    }

    /**
     * Tests if the interior of the <code>Shape</code> intersects the
     * interior of a specified rectangular area.
     * The rectangular area is considered to intersect the <code>Shape</code>
     * if any point is contained in both the interior of the
     * <code>Shape</code> and the specified rectangular area.
     * <p>
     * The {@code Shape.intersects()} method allows a {@code Shape}
     * implementation to conservatively return {@code true} when:
     * <ul>
     * <li>
     * there is a high probability that the rectangular area and the
     * <code>Shape</code> intersect, but
     * <li>
     * the calculations to accurately determine this intersection
     * are prohibitively expensive.
     * </ul>
     * This means that for some {@code Shapes} this method might
     * return {@code true} even though the rectangular area does not
     * intersect the {@code Shape}.
     * The {@link com.sun.javafx.geom.Area Area} class performs
     * more accurate computations of geometric intersection than most
     * {@code Shape} objects and therefore can be used if a more precise
     * answer is required.
     *
     * @param x the X coordinate of the upper-left corner
     *          of the specified rectangular area
     * @param y the Y coordinate of the upper-left corner
     *          of the specified rectangular area
     * @param w the width of the specified rectangular area
     * @param h the height of the specified rectangular area
     * @return <code>true</code> if the interior of the <code>Shape</code> and
     *      the interior of the rectangular area intersect, or are
     *      both highly likely to intersect and intersection calculations
     *      would be too expensive to perform; <code>false</code> otherwise.
     */
    public abstract boolean intersects(float x, float y, float w, float h);

    /**
     * Tests if the interior of the <code>Shape</code> intersects the
     * interior of a specified rectangular area.
     * The rectangular area is considered to intersect the <code>Shape</code>
     * if any point is contained in both the interior of the
     * <code>Shape</code> and the specified rectangular area.
     * <p>
     * The {@code Shape.intersects()} method allows a {@code Shape}
     * implementation to conservatively return {@code true} when:
     * <ul>
     * <li>
     * there is a high probability that the rectangular area and the
     * <code>Shape</code> intersect, but
     * <li>
     * the calculations to accurately determine this intersection
     * are prohibitively expensive.
     * </ul>
     * This means that for some {@code Shapes} this method might
     * return {@code true} even though the rectangular area does not
     * intersect the {@code Shape}.
     * The {@link com.sun.javafx.geom.Area Area} class performs
     * more accurate computations of geometric intersection than most
     * {@code Shape} objects and therefore can be used if a more precise
     * answer is required.
     *
     * @param x the X coordinate of the upper-left corner
     *          of the specified rectangular area
     * @param y the Y coordinate of the upper-left corner
     *          of the specified rectangular area
     * @param w the width of the specified rectangular area
     * @param h the height of the specified rectangular area
     * @return <code>true</code> if the interior of the <code>Shape</code> and
     *      the interior of the rectangular area intersect, or are
     *      both highly likely to intersect and intersection calculations
     *      would be too expensive to perform; <code>false</code> otherwise.
     */
    public boolean intersects(RectBounds r) {
        float x = r.getMinX();
        float y = r.getMinY();
        float w = r.getMaxX() - x;
        float h = r.getMaxY() - y;
        return intersects(x, y, w, h);
    }

    /**
     * Tests if the interior of the <code>Shape</code> entirely contains
     * the specified rectangular area.  All coordinates that lie inside
     * the rectangular area must lie within the <code>Shape</code> for the
     * entire rectanglar area to be considered contained within the
     * <code>Shape</code>.
     * <p>
     * The {@code Shape.contains()} method allows a {@code Shape}
     * implementation to conservatively return {@code false} when:
     * <ul>
     * <li>
     * the <code>intersect</code> method returns <code>true</code> and
     * <li>
     * the calculations to determine whether or not the
     * <code>Shape</code> entirely contains the rectangular area are
     * prohibitively expensive.
     * </ul>
     * This means that for some {@code Shapes} this method might
     * return {@code false} even though the {@code Shape} contains
     * the rectangular area.
     * The {@link com.sun.javafx.geom.Area Area} class performs
     * more accurate geometric computations than most
     * {@code Shape} objects and therefore can be used if a more precise
     * answer is required.
     *
     * @param x the X coordinate of the upper-left corner
     *          of the specified rectangular area
     * @param y the Y coordinate of the upper-left corner
     *          of the specified rectangular area
     * @param w the width of the specified rectangular area
     * @param h the height of the specified rectangular area
     * @return <code>true</code> if the interior of the <code>Shape</code>
     *      entirely contains the specified rectangular area;
     *      <code>false</code> otherwise or, if the <code>Shape</code>
     *      contains the rectangular area and the
     *      <code>intersects</code> method returns <code>true</code>
     *      and the containment calculations would be too expensive to
     *      perform.
     * @see #intersects
     */
    public abstract boolean contains(float x, float y, float w, float h);

    /**
     * Tests if the interior of the <code>Shape</code> entirely contains the
     * specified <code>RectBounds</code>.
     * The {@code Shape.contains()} method allows a {@code Shape}
     * implementation to conservatively return {@code false} when:
     * <ul>
     * <li>
     * the <code>intersect</code> method returns <code>true</code> and
     * <li>
     * the calculations to determine whether or not the
     * <code>Shape</code> entirely contains the <code>RectBounds</code>
     * are prohibitively expensive.
     * </ul>
     * This means that for some {@code Shapes} this method might
     * return {@code false} even though the {@code Shape} contains
     * the {@code RectBounds}.
     * The {@link com.sun.javafx.geom.Area Area} class performs
     * more accurate geometric computations than most
     * {@code Shape} objects and therefore can be used if a more precise
     * answer is required.
     *
     * @param r The specified <code>RectBounds</code>
     * @return <code>true</code> if the interior of the <code>Shape</code>
     *          entirely contains the <code>RectBounds</code>;
     *          <code>false</code> otherwise or, if the <code>Shape</code>
     *          contains the <code>RectBounds</code> and the
     *          <code>intersects</code> method returns <code>true</code>
     *          and the containment calculations would be too expensive to
     *          perform.
     * @see #contains(float, float, float, float)
     */
    public boolean contains(RectBounds r) {
        float x = r.getMinX();
        float y = r.getMinY();
        float w = r.getMaxX() - x;
        float h = r.getMaxY() - y;
        return contains(x, y, w, h);
    }

    /**
     * Returns an iterator object that iterates along the
     * <code>Shape</code> boundary and provides access to the geometry of the
     * <code>Shape</code> outline.  If an optional {@link BaseTransform}
     * is specified, the coordinates returned in the iteration are
     * transformed accordingly.
     * <p>
     * Each call to this method returns a fresh <code>PathIterator</code>
     * object that traverses the geometry of the <code>Shape</code> object
     * independently from any other <code>PathIterator</code> objects in use
     * at the same time.
     * <p>
     * It is recommended, but not guaranteed, that objects
     * implementing the <code>Shape</code> interface isolate iterations
     * that are in process from any changes that might occur to the original
     * object's geometry during such iterations.
     *
     * @param tx an optional <code>BaseTransform</code> to be applied to the
     *      coordinates as they are returned in the iteration, or
     *      <code>null</code> if untransformed coordinates are desired
     * @return a new <code>PathIterator</code> object, which independently
     *      traverses the geometry of the <code>Shape</code>.
     */
    public abstract PathIterator getPathIterator(BaseTransform tx);

    /**
     * Returns an iterator object that iterates along the <code>Shape</code>
     * boundary and provides access to a flattened view of the
     * <code>Shape</code> outline geometry.
     * <p>
     * Only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types are
     * returned by the iterator.
     * <p>
     * If an optional <code>BaseTransform</code> is specified,
     * the coordinates returned in the iteration are transformed
     * accordingly.
     * <p>
     * The amount of subdivision of the curved segments is controlled
     * by the <code>flatness</code> parameter, which specifies the
     * maximum distance that any point on the unflattened transformed
     * curve can deviate from the returned flattened path segments.
     * Note that a limit on the accuracy of the flattened path might be
     * silently imposed, causing very small flattening parameters to be
     * treated as larger values.  This limit, if there is one, is
     * defined by the particular implementation that is used.
     * <p>
     * Each call to this method returns a fresh <code>PathIterator</code>
     * object that traverses the <code>Shape</code> object geometry
     * independently from any other <code>PathIterator</code> objects in use at
     * the same time.
     * <p>
     * It is recommended, but not guaranteed, that objects
     * implementing the <code>Shape</code> interface isolate iterations
     * that are in process from any changes that might occur to the original
     * object's geometry during such iterations.
     *
     * @param tx an optional <code>BaseTransform</code> to be applied to the
     *      coordinates as they are returned in the iteration, or
     *      <code>null</code> if untransformed coordinates are desired
     * @param flatness the maximum distance that the line segments used to
     *          approximate the curved segments are allowed to deviate
     *          from any point on the original curve
     * @return a new <code>PathIterator</code> that independently traverses
     *         a flattened view of the geometry of the  <code>Shape</code>.
     */
    public abstract PathIterator getPathIterator(BaseTransform tx, float flatness);

    /**
     * Returns a new copy of this {@code Shape} instance.
     *
     * @return a copy of this shape
     */
    public abstract Shape copy();

    /**
     * Calculates the number of times the given path
     * crosses the ray extending to the right from (px,py).
     * If the point lies on a part of the path,
     * then no crossings are counted for that intersection.
     * +1 is added for each crossing where the Y coordinate is increasing
     * -1 is added for each crossing where the Y coordinate is decreasing
     * The return value is the sum of all crossings for every segment in
     * the path.
     * The path must start with a SEG_MOVETO, otherwise an exception is
     * thrown.
     * The caller must check p[xy] for NaN values.
     * The caller may also reject infinite p[xy] values as well.
     */
    public static int pointCrossingsForPath(PathIterator pi,
                                            float px, float py)
    {
        if (pi.isDone()) {
            return 0;
        }
        float coords[] = new float[6];
        if (pi.currentSegment(coords) != PathIterator.SEG_MOVETO) {
            throw new IllegalPathStateException("missing initial moveto "+
                                                "in path definition");
        }
        pi.next();
        float movx = coords[0];
        float movy = coords[1];
        float curx = movx;
        float cury = movy;
        float endx, endy;
        int crossings = 0;
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    if (cury != movy) {
                        crossings += pointCrossingsForLine(px, py,
                                                           curx, cury,
                                                           movx, movy);
                    }
                    movx = curx = coords[0];
                    movy = cury = coords[1];
                    break;
                case PathIterator.SEG_LINETO:
                    endx = coords[0];
                    endy = coords[1];
                    crossings += pointCrossingsForLine(px, py,
                                                       curx, cury,
                                                       endx, endy);
                    curx = endx;
                    cury = endy;
                    break;
                case PathIterator.SEG_QUADTO:
                    endx = coords[2];
                    endy = coords[3];
                    crossings += pointCrossingsForQuad(px, py,
                                                       curx, cury,
                                                       coords[0], coords[1],
                                                       endx, endy, 0);
                    curx = endx;
                    cury = endy;
                    break;
                case PathIterator.SEG_CUBICTO:
                    endx = coords[4];
                    endy = coords[5];
                    crossings += pointCrossingsForCubic(px, py,
                                                        curx, cury,
                                                        coords[0], coords[1],
                                                        coords[2], coords[3],
                                                        endx, endy, 0);
                    curx = endx;
                    cury = endy;
                    break;
                case PathIterator.SEG_CLOSE:
                    if (cury != movy) {
                        crossings += pointCrossingsForLine(px, py,
                                                           curx, cury,
                                                           movx, movy);
                    }
                    curx = movx;
                    cury = movy;
                    break;
            }
            pi.next();
        }
        if (cury != movy) {
            crossings += pointCrossingsForLine(px, py,
                                               curx, cury,
                                               movx, movy);
        }
        return crossings;
    }

    /**
     * Calculates the number of times the line from (x0,y0) to (x1,y1)
     * crosses the ray extending to the right from (px,py).
     * If the point lies on the line, then no crossings are recorded.
     * +1 is returned for a crossing where the Y coordinate is increasing
     * -1 is returned for a crossing where the Y coordinate is decreasing
     */
    public static int pointCrossingsForLine(float px, float py,
                                            float x0, float y0,
                                            float x1, float y1)
    {
        if (py <  y0 && py <  y1) return 0;
        if (py >= y0 && py >= y1) return 0;
        // assert(y0 != y1);
        if (px >= x0 && px >= x1) return 0;
        if (px <  x0 && px <  x1) return (y0 < y1) ? 1 : -1;
        float xintercept = x0 + (py - y0) * (x1 - x0) / (y1 - y0);
        if (px >= xintercept) return 0;
        return (y0 < y1) ? 1 : -1;
    }

    /**
     * Calculates the number of times the quad from (x0,y0) to (x1,y1)
     * crosses the ray extending to the right from (px,py).
     * If the point lies on a part of the curve,
     * then no crossings are counted for that intersection.
     * the level parameter should be 0 at the top-level call and will count
     * up for each recursion level to prevent infinite recursion
     * +1 is added for each crossing where the Y coordinate is increasing
     * -1 is added for each crossing where the Y coordinate is decreasing
     */
    public static int pointCrossingsForQuad(float px, float py,
                                            float x0, float y0,
                                            float xc, float yc,
                                            float x1, float y1, int level)
    {
        if (py <  y0 && py <  yc && py <  y1) return 0;
        if (py >= y0 && py >= yc && py >= y1) return 0;
        // Note y0 could equal y1...
        if (px >= x0 && px >= xc && px >= x1) return 0;
        if (px <  x0 && px <  xc && px <  x1) {
            if (py >= y0) {
                if (py < y1) return 1;
            } else {
                // py < y0
                if (py >= y1) return -1;
            }
            // py outside of y01 range, and/or y0==y1
            return 0;
        }
        // double precision only has 52 bits of mantissa
        if (level > 52) return pointCrossingsForLine(px, py, x0, y0, x1, y1);
        float x0c = (x0 + xc) / 2;
        float y0c = (y0 + yc) / 2;
        float xc1 = (xc + x1) / 2;
        float yc1 = (yc + y1) / 2;
        xc = (x0c + xc1) / 2;
        yc = (y0c + yc1) / 2;
        if (Float.isNaN(xc) || Float.isNaN(yc)) {
            // [xy]c are NaN if any of [xy]0c or [xy]c1 are NaN
            // [xy]0c or [xy]c1 are NaN if any of [xy][0c1] are NaN
            // These values are also NaN if opposing infinities are added
            return 0;
        }
        return (pointCrossingsForQuad(px, py,
                                      x0, y0, x0c, y0c, xc, yc,
                                      level+1) +
                pointCrossingsForQuad(px, py,
                                      xc, yc, xc1, yc1, x1, y1,
                                      level+1));
    }

    /**
     * Calculates the number of times the cubic from (x0,y0) to (x1,y1)
     * crosses the ray extending to the right from (px,py).
     * If the point lies on a part of the curve,
     * then no crossings are counted for that intersection.
     * the level parameter should be 0 at the top-level call and will count
     * up for each recursion level to prevent infinite recursion
     * +1 is added for each crossing where the Y coordinate is increasing
     * -1 is added for each crossing where the Y coordinate is decreasing
     */
    public static int pointCrossingsForCubic(float px, float py,
                                             float x0, float y0,
                                             float xc0, float yc0,
                                             float xc1, float yc1,
                                             float x1, float y1, int level)
    {
        if (py <  y0 && py <  yc0 && py <  yc1 && py <  y1) return 0;
        if (py >= y0 && py >= yc0 && py >= yc1 && py >= y1) return 0;
        // Note y0 could equal yc0...
        if (px >= x0 && px >= xc0 && px >= xc1 && px >= x1) return 0;
        if (px <  x0 && px <  xc0 && px <  xc1 && px <  x1) {
            if (py >= y0) {
                if (py < y1) return 1;
            } else {
                // py < y0
                if (py >= y1) return -1;
            }
            // py outside of y01 range, and/or y0==yc0
            return 0;
        }
        // double precision only has 52 bits of mantissa
        if (level > 52) return pointCrossingsForLine(px, py, x0, y0, x1, y1);
        float xmid = (xc0 + xc1) / 2;
        float ymid = (yc0 + yc1) / 2;
        xc0 = (x0 + xc0) / 2;
        yc0 = (y0 + yc0) / 2;
        xc1 = (xc1 + x1) / 2;
        yc1 = (yc1 + y1) / 2;
        float xc0m = (xc0 + xmid) / 2;
        float yc0m = (yc0 + ymid) / 2;
        float xmc1 = (xmid + xc1) / 2;
        float ymc1 = (ymid + yc1) / 2;
        xmid = (xc0m + xmc1) / 2;
        ymid = (yc0m + ymc1) / 2;
        if (Float.isNaN(xmid) || Float.isNaN(ymid)) {
            // [xy]mid are NaN if any of [xy]c0m or [xy]mc1 are NaN
            // [xy]c0m or [xy]mc1 are NaN if any of [xy][c][01] are NaN
            // These values are also NaN if opposing infinities are added
            return 0;
        }
        return (pointCrossingsForCubic(px, py,
                                       x0, y0, xc0, yc0,
                                       xc0m, yc0m, xmid, ymid, level+1) +
                pointCrossingsForCubic(px, py,
                                       xmid, ymid, xmc1, ymc1,
                                       xc1, yc1, x1, y1, level+1));
    }

    /**
     * The rectangle intersection test counts the number of times
     * that the path crosses through the shadow that the rectangle
     * projects to the right towards (x => +INFINITY).
     *
     * During processing of the path it actually counts every time
     * the path crosses either or both of the top and bottom edges
     * of that shadow.  If the path enters from the top, the count
     * is incremented.  If it then exits back through the top, the
     * same way it came in, the count is decremented and there is
     * no impact on the winding count.  If, instead, the path exits
     * out the bottom, then the count is incremented again and a
     * full pass through the shadow is indicated by the winding count
     * having been incremented by 2.
     *
     * Thus, the winding count that it accumulates is actually float
     * the real winding count.  Since the path is continuous, the
     * final answer should be a multiple of 2, otherwise there is a
     * logic error somewhere.
     *
     * If the path ever has a direct hit on the rectangle, then a
     * special value is returned.  This special value terminates
     * all ongoing accumulation on up through the call chain and
     * ends up getting returned to the calling function which can
     * then produce an answer directly.  For intersection tests,
     * the answer is always "true" if the path intersects the
     * rectangle.  For containment tests, the answer is always
     * "false" if the path intersects the rectangle.  Thus, no
     * further processing is ever needed if an intersection occurs.
     */
    public static final int RECT_INTERSECTS = 0x80000000;

    /**
     * Accumulate the number of times the path crosses the shadow
     * extending to the right of the rectangle.  See the comment
     * for the RECT_INTERSECTS constant for more complete details.
     * The return value is the sum of all crossings for both the
     * top and bottom of the shadow for every segment in the path,
     * or the special value RECT_INTERSECTS if the path ever enters
     * the interior of the rectangle.
     * The path must start with a SEG_MOVETO, otherwise an exception is
     * thrown.
     * The caller must check r[xy]{min,max} for NaN values.
     */
    public static int rectCrossingsForPath(PathIterator pi,
                                           float rxmin, float rymin,
                                           float rxmax, float rymax)
    {
        if (rxmax <= rxmin || rymax <= rymin) {
            return 0;
        }
        if (pi.isDone()) {
            return 0;
        }
        float coords[] = new float[6];
        if (pi.currentSegment(coords) != PathIterator.SEG_MOVETO) {
            throw new IllegalPathStateException("missing initial moveto "+
                                                "in path definition");
        }
        pi.next();
        float curx, cury, movx, movy, endx, endy;
        curx = movx = coords[0];
        cury = movy = coords[1];
        int crossings = 0;
        while (crossings != RECT_INTERSECTS && !pi.isDone()) {
            switch (pi.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO:
                if (curx != movx || cury != movy) {
                    crossings = rectCrossingsForLine(crossings,
                                                     rxmin, rymin,
                                                     rxmax, rymax,
                                                     curx, cury,
                                                     movx, movy);
                }
                // Count should always be a multiple of 2 here.
                // assert((crossings & 1) != 0);
                movx = curx = coords[0];
                movy = cury = coords[1];
                break;
            case PathIterator.SEG_LINETO:
                endx = coords[0];
                endy = coords[1];
                crossings = rectCrossingsForLine(crossings,
                                                 rxmin, rymin,
                                                 rxmax, rymax,
                                                 curx, cury,
                                                 endx, endy);
                curx = endx;
                cury = endy;
                break;
            case PathIterator.SEG_QUADTO:
                endx = coords[2];
                endy = coords[3];
                crossings = rectCrossingsForQuad(crossings,
                                                 rxmin, rymin,
                                                 rxmax, rymax,
                                                 curx, cury,
                                                 coords[0], coords[1],
                                                 endx, endy, 0);
                curx = endx;
                cury = endy;
                break;
            case PathIterator.SEG_CUBICTO:
                endx = coords[4];
                endy = coords[5];
                crossings = rectCrossingsForCubic(crossings,
                                                  rxmin, rymin,
                                                  rxmax, rymax,
                                                  curx, cury,
                                                  coords[0], coords[1],
                                                  coords[2], coords[3],
                                                  endx, endy, 0);
                curx = endx;
                cury = endy;
                break;
            case PathIterator.SEG_CLOSE:
                if (curx != movx || cury != movy) {
                    crossings = rectCrossingsForLine(crossings,
                                                     rxmin, rymin,
                                                     rxmax, rymax,
                                                     curx, cury,
                                                     movx, movy);
                }
                curx = movx;
                cury = movy;
                // Count should always be a multiple of 2 here.
                // assert((crossings & 1) != 0);
                break;
            }
            pi.next();
        }
        if (crossings != RECT_INTERSECTS && (curx != movx || cury != movy)) {
            crossings = rectCrossingsForLine(crossings,
                                             rxmin, rymin,
                                             rxmax, rymax,
                                             curx, cury,
                                             movx, movy);
        }
        // Count should always be a multiple of 2 here.
        // assert((crossings & 1) != 0);
        return crossings;
    }

    /**
     * Accumulate the number of times the line crosses the shadow
     * extending to the right of the rectangle.  See the comment
     * for the RECT_INTERSECTS constant for more complete details.
     */
    public static int rectCrossingsForLine(int crossings,
                                           float rxmin, float rymin,
                                           float rxmax, float rymax,
                                           float x0, float y0,
                                           float x1, float y1)
    {
        if (y0 >= rymax && y1 >= rymax) return crossings;
        if (y0 <= rymin && y1 <= rymin) return crossings;
        if (x0 <= rxmin && x1 <= rxmin) return crossings;
        if (x0 >= rxmax && x1 >= rxmax) {
            // Line is entirely to the right of the rect
            // and the vertical ranges of the two overlap by a non-empty amount
            // Thus, this line segment is partially in the "right-shadow"
            // Path may have done a complete crossing
            // Or path may have entered or exited the right-shadow
            if (y0 < y1) {
                // y-increasing line segment...
                // We know that y0 < rymax and y1 > rymin
                if (y0 <= rymin) crossings++;
                if (y1 >= rymax) crossings++;
            } else if (y1 < y0) {
                // y-decreasing line segment...
                // We know that y1 < rymax and y0 > rymin
                if (y1 <= rymin) crossings--;
                if (y0 >= rymax) crossings--;
            }
            return crossings;
        }
        // Remaining case:
        // Both x and y ranges overlap by a non-empty amount
        // First do trivial INTERSECTS rejection of the cases
        // where one of the endpoints is inside the rectangle.
        if ((x0 > rxmin && x0 < rxmax && y0 > rymin && y0 < rymax) ||
            (x1 > rxmin && x1 < rxmax && y1 > rymin && y1 < rymax))
        {
            return RECT_INTERSECTS;
        }
        // Otherwise calculate the y intercepts and see where
        // they fall with respect to the rectangle
        float xi0 = x0;
        if (y0 < rymin) {
            xi0 += ((rymin - y0) * (x1 - x0) / (y1 - y0));
        } else if (y0 > rymax) {
            xi0 += ((rymax - y0) * (x1 - x0) / (y1 - y0));
        }
        float xi1 = x1;
        if (y1 < rymin) {
            xi1 += ((rymin - y1) * (x0 - x1) / (y0 - y1));
        } else if (y1 > rymax) {
            xi1 += ((rymax - y1) * (x0 - x1) / (y0 - y1));
        }
        if (xi0 <= rxmin && xi1 <= rxmin) return crossings;
        if (xi0 >= rxmax && xi1 >= rxmax) {
            if (y0 < y1) {
                // y-increasing line segment...
                // We know that y0 < rymax and y1 > rymin
                if (y0 <= rymin) crossings++;
                if (y1 >= rymax) crossings++;
            } else if (y1 < y0) {
                // y-decreasing line segment...
                // We know that y1 < rymax and y0 > rymin
                if (y1 <= rymin) crossings--;
                if (y0 >= rymax) crossings--;
            }
            return crossings;
        }
        return RECT_INTERSECTS;
    }

    /**
     * Accumulate the number of times the quad crosses the shadow
     * extending to the right of the rectangle.  See the comment
     * for the RECT_INTERSECTS constant for more complete details.
     */
    public static int rectCrossingsForQuad(int crossings,
                                           float rxmin, float rymin,
                                           float rxmax, float rymax,
                                           float x0, float y0,
                                           float xc, float yc,
                                           float x1, float y1,
                                           int level)
    {
        if (y0 >= rymax && yc >= rymax && y1 >= rymax) return crossings;
        if (y0 <= rymin && yc <= rymin && y1 <= rymin) return crossings;
        if (x0 <= rxmin && xc <= rxmin && x1 <= rxmin) return crossings;
        if (x0 >= rxmax && xc >= rxmax && x1 >= rxmax) {
            // Quad is entirely to the right of the rect
            // and the vertical range of the 3 Y coordinates of the quad
            // overlaps the vertical range of the rect by a non-empty amount
            // We now judge the crossings solely based on the line segment
            // connecting the endpoints of the quad.
            // Note that we may have 0, 1, or 2 crossings as the control
            // point may be causing the Y range intersection while the
            // two endpoints are entirely above or below.
            if (y0 < y1) {
                // y-increasing line segment...
                if (y0 <= rymin && y1 >  rymin) crossings++;
                if (y0 <  rymax && y1 >= rymax) crossings++;
            } else if (y1 < y0) {
                // y-decreasing line segment...
                if (y1 <= rymin && y0 >  rymin) crossings--;
                if (y1 <  rymax && y0 >= rymax) crossings--;
            }
            return crossings;
        }
        // The intersection of ranges is more complicated
        // First do trivial INTERSECTS rejection of the cases
        // where one of the endpoints is inside the rectangle.
        if ((x0 < rxmax && x0 > rxmin && y0 < rymax && y0 > rymin) ||
            (x1 < rxmax && x1 > rxmin && y1 < rymax && y1 > rymin))
        {
            return RECT_INTERSECTS;
        }
        // Otherwise, subdivide and look for one of the cases above.
        // double precision only has 52 bits of mantissa
        if (level > 52) {
            return rectCrossingsForLine(crossings,
                                        rxmin, rymin, rxmax, rymax,
                                        x0, y0, x1, y1);
        }
        float x0c = (x0 + xc) / 2;
        float y0c = (y0 + yc) / 2;
        float xc1 = (xc + x1) / 2;
        float yc1 = (yc + y1) / 2;
        xc = (x0c + xc1) / 2;
        yc = (y0c + yc1) / 2;
        if (Float.isNaN(xc) || Float.isNaN(yc)) {
            // [xy]c are NaN if any of [xy]0c or [xy]c1 are NaN
            // [xy]0c or [xy]c1 are NaN if any of [xy][0c1] are NaN
            // These values are also NaN if opposing infinities are added
            return 0;
        }
        crossings = rectCrossingsForQuad(crossings,
                                         rxmin, rymin, rxmax, rymax,
                                         x0, y0, x0c, y0c, xc, yc,
                                         level+1);
        if (crossings != RECT_INTERSECTS) {
            crossings = rectCrossingsForQuad(crossings,
                                             rxmin, rymin, rxmax, rymax,
                                             xc, yc, xc1, yc1, x1, y1,
                                             level+1);
        }
        return crossings;
    }

    /**
     * Accumulate the number of times the cubic crosses the shadow
     * extending to the right of the rectangle.  See the comment
     * for the RECT_INTERSECTS constant for more complete details.
     */
    public static int rectCrossingsForCubic(int crossings,
                                            float rxmin, float rymin,
                                            float rxmax, float rymax,
                                            float x0,  float y0,
                                            float xc0, float yc0,
                                            float xc1, float yc1,
                                            float x1,  float y1,
                                            int level)
    {
        if (y0 >= rymax && yc0 >= rymax && yc1 >= rymax && y1 >= rymax) {
            return crossings;
        }
        if (y0 <= rymin && yc0 <= rymin && yc1 <= rymin && y1 <= rymin) {
            return crossings;
        }
        if (x0 <= rxmin && xc0 <= rxmin && xc1 <= rxmin && x1 <= rxmin) {
            return crossings;
        }
        if (x0 >= rxmax && xc0 >= rxmax && xc1 >= rxmax && x1 >= rxmax) {
            // Cubic is entirely to the right of the rect
            // and the vertical range of the 4 Y coordinates of the cubic
            // overlaps the vertical range of the rect by a non-empty amount
            // We now judge the crossings solely based on the line segment
            // connecting the endpoints of the cubic.
            // Note that we may have 0, 1, or 2 crossings as the control
            // points may be causing the Y range intersection while the
            // two endpoints are entirely above or below.
            if (y0 < y1) {
                // y-increasing line segment...
                if (y0 <= rymin && y1 >  rymin) crossings++;
                if (y0 <  rymax && y1 >= rymax) crossings++;
            } else if (y1 < y0) {
                // y-decreasing line segment...
                if (y1 <= rymin && y0 >  rymin) crossings--;
                if (y1 <  rymax && y0 >= rymax) crossings--;
            }
            return crossings;
        }
        // The intersection of ranges is more complicated
        // First do trivial INTERSECTS rejection of the cases
        // where one of the endpoints is inside the rectangle.
        if ((x0 > rxmin && x0 < rxmax && y0 > rymin && y0 < rymax) ||
            (x1 > rxmin && x1 < rxmax && y1 > rymin && y1 < rymax))
        {
            return RECT_INTERSECTS;
        }
        // Otherwise, subdivide and look for one of the cases above.
        // double precision only has 52 bits of mantissa
        if (level > 52) {
            return rectCrossingsForLine(crossings,
                                        rxmin, rymin, rxmax, rymax,
                                        x0, y0, x1, y1);
        }
        float xmid = (xc0 + xc1) / 2;
        float ymid = (yc0 + yc1) / 2;
        xc0 = (x0 + xc0) / 2;
        yc0 = (y0 + yc0) / 2;
        xc1 = (xc1 + x1) / 2;
        yc1 = (yc1 + y1) / 2;
        float xc0m = (xc0 + xmid) / 2;
        float yc0m = (yc0 + ymid) / 2;
        float xmc1 = (xmid + xc1) / 2;
        float ymc1 = (ymid + yc1) / 2;
        xmid = (xc0m + xmc1) / 2;
        ymid = (yc0m + ymc1) / 2;
        if (Float.isNaN(xmid) || Float.isNaN(ymid)) {
            // [xy]mid are NaN if any of [xy]c0m or [xy]mc1 are NaN
            // [xy]c0m or [xy]mc1 are NaN if any of [xy][c][01] are NaN
            // These values are also NaN if opposing infinities are added
            return 0;
        }
        crossings = rectCrossingsForCubic(crossings,
                                          rxmin, rymin, rxmax, rymax,
                                          x0, y0, xc0, yc0,
                                          xc0m, yc0m, xmid, ymid, level+1);
        if (crossings != RECT_INTERSECTS) {
            crossings = rectCrossingsForCubic(crossings,
                                              rxmin, rymin, rxmax, rymax,
                                              xmid, ymid, xmc1, ymc1,
                                              xc1, yc1, x1, y1, level+1);
        }
        return crossings;
    }

    /**
     * Tests if the specified line segment intersects the interior of the
     * rectangle denoted by rx1, ry1, rx2, ry2.
     */
    static boolean intersectsLine(float rx1, float ry1, float rwidth,
        float rheight, float x1, float y1, float x2, float y2)
    {
       int out1, out2;
       if ((out2 = outcode(rx1, ry1, rwidth, rheight, x2, y2)) == 0) {
           return true;
       }
       while ((out1 = outcode(rx1, ry1, rwidth, rheight, x1, y1)) != 0) {
           if ((out1 & out2) != 0) {
               return false;
           }
           if ((out1 & (OUT_LEFT | OUT_RIGHT)) != 0) {
               if ((out1 & OUT_RIGHT) != 0) {
                   rx1 += rwidth;
               }
               y1 = y1 + (rx1 - x1) * (y2 - y1) / (x2 - x1);
               x1 = rx1;
           } else {
               if ((out1 & OUT_BOTTOM) != 0) {
                   ry1 += rheight;
               }
               x1 = x1 + (ry1 - y1) * (x2 - x1) / (y2 - y1);
               y1 = ry1;
           }
       }
       return true;
    }

    /**
     * {@inheritDoc}
     */
    static int outcode(float rx, float ry, float rwidth, float rheight, float x, float y) {
        /*
         * Note on casts to double below.  If the arithmetic of
         * x+w or y+h is done in int, then we may get integer
         * overflow. By converting to double before the addition
         * we force the addition to be carried out in double to
         * avoid overflow in the comparison.
         *
         * See bug 4320890 for problems that this can cause.
         */
        int out = 0;
        if (rwidth <= 0) {
            out |= OUT_LEFT | OUT_RIGHT;
        } else if (x < rx) {
            out |= OUT_LEFT;
        } else if (x > rx + (double) rwidth) {
            out |= OUT_RIGHT;
        }
        if (rheight <= 0) {
            out |= OUT_TOP | OUT_BOTTOM;
        } else if (y < ry) {
            out |= OUT_TOP;
        } else if (y > ry + (double) rheight) {
            out |= OUT_BOTTOM;
        }
        return out;
    }

    /**
     * The bitmask that indicates that a point lies to the left of
     * this <code>Rectangle2D</code>.
     */
    public static final int OUT_LEFT = 1;

    /**
     * The bitmask that indicates that a point lies above
     * this <code>Rectangle2D</code>.
     */
    public static final int OUT_TOP = 2;

    /**
     * The bitmask that indicates that a point lies to the right of
     * this <code>Rectangle2D</code>.
     */
    public static final int OUT_RIGHT = 4;

    /**
     * The bitmask that indicates that a point lies below
     * this <code>Rectangle2D</code>.
     */
    public static final int OUT_BOTTOM = 8;

    public static void accumulate(float bbox[], Shape s, BaseTransform tx) {
        // Note that this is turned off since we cannot guarantee
        // that the shape implementation will calculate minimal bounds
        // without a little more work on the javafx.geom classes...
//        if (tx.isIdentity()) {
//            // The shape itself will often have a more optimal algorithm
//            // to calculate the untransformed bounds...
//            RectBounds r2d = s.getBounds();
//            if (bbox[0] > r2d.getMinX()) bbox[0] = r2d.getMinX();
//            if (bbox[1] > r2d.getMinY()) bbox[1] = r2d.getMinY();
//            if (bbox[2] < r2d.getMaxX()) bbox[2] = r2d.getMaxX();
//            if (bbox[3] < r2d.getMaxY()) bbox[3] = r2d.getMaxY();
//            return;
//        }
        PathIterator pi = s.getPathIterator(tx);
        float coords[] = new float[6];
        float mx = 0f, my = 0f, x0 = 0f, y0 = 0f, x1, y1;
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    mx = coords[0];
                    my = coords[1];
                    /* NOBREAK */
                case PathIterator.SEG_LINETO:
                    x0 = coords[0];
                    y0 = coords[1];
                    if (bbox[0] > x0) bbox[0] = x0;
                    if (bbox[1] > y0) bbox[1] = y0;
                    if (bbox[2] < x0) bbox[2] = x0;
                    if (bbox[3] < y0) bbox[3] = y0;
                    break;
                case PathIterator.SEG_QUADTO:
                    x1 = coords[2];
                    y1 = coords[3];
                    if (bbox[0] > x1) bbox[0] = x1;
                    if (bbox[1] > y1) bbox[1] = y1;
                    if (bbox[2] < x1) bbox[2] = x1;
                    if (bbox[3] < y1) bbox[3] = y1;
                    if (bbox[0] > coords[0] || bbox[2] < coords[0]) {
                        accumulateQuad(bbox, 0, x0, coords[0], x1);
                    }
                    if (bbox[1] > coords[1] || bbox[3] < coords[1]) {
                        accumulateQuad(bbox, 1, y0, coords[1], y1);
                    }
                    x0 = x1;
                    y0 = y1;
                    break;
                case PathIterator.SEG_CUBICTO:
                    x1 = coords[4];
                    y1 = coords[5];
                    if (bbox[0] > x1) bbox[0] = x1;
                    if (bbox[1] > y1) bbox[1] = y1;
                    if (bbox[2] < x1) bbox[2] = x1;
                    if (bbox[3] < y1) bbox[3] = y1;
                    if (bbox[0] > coords[0] || bbox[2] < coords[0] ||
                        bbox[0] > coords[2] || bbox[2] < coords[2])
                    {
                        accumulateCubic(bbox, 0, x0, coords[0], coords[2], x1);
                    }
                    if (bbox[1] > coords[1] || bbox[3] < coords[1] ||
                        bbox[1] > coords[3] || bbox[3] < coords[3])
                    {
                        accumulateCubic(bbox, 1, y0, coords[1], coords[3], y1);
                    }
                    x0 = x1;
                    y0 = y1;
                    break;
                case PathIterator.SEG_CLOSE:
                    x0 = mx;
                    y0 = my;
                    break;
            }
            pi.next();
        }
    }

    public static void accumulateQuad(float bbox[], int off,
                                      float v0, float vc, float v1)
    {
        // Breaking this quad down into a polynomial:
        // eqn[0] = v0;
        // eqn[1] = vc + vc - v0 - v0;
        // eqn[2] = v0 - vc - vc + v1;
        // Deriving the polynomial:
        // eqn'[0] = 1*eqn[1] = 2*(vc-v0)
        // eqn'[1] = 2*eqn[2] = 2*((v1-vc)-(vc-v0))
        // Solving for zeroes on the derivative:
        // e1*t + e0 = 0
        // t = -e0/e1;
        // t = -2(vc-v0) / 2((v1-vc)-(vc-v0))
        // t = (v0-vc) / (v1-vc+v0-vc)
        float num = v0 - vc;
        float den = v1 - vc + num;
        if (den != 0f) {
            float t = num / den;
            if (t > 0 && t < 1) {
                float u = 1f - t;
                float v = v0 * u * u + 2 * vc * t * u + v1 * t * t;
                if (bbox[off] > v) bbox[off] = v;
                if (bbox[off+2] < v) bbox[off+2] = v;
            }
        }
    }

    public static void accumulateCubic(float bbox[], int off,
                                       float v0, float vc0, float vc1, float v1)
    {
        // Breaking this cubic down into a polynomial:
        // eqn[0] = v0;
        // eqn[1] = (vc0 - v0) * 3f;
        // eqn[2] = (vc1 - vc0 - vc0 + v0) * 3f;
        // eqn[3] = v1 + (vc0 - vc1) * 3f - v0;
        // Deriving the polynomial:
        // eqn'[0] = 1*eqn[1] = 3(vc0-v0)
        // eqn'[1] = 2*eqn[2] = 6((vc1-vc0)-(vc0-v0))
        // eqn'[2] = 3*eqn[3] = 3((v1-vc1)-2(vc1-vc0)+(vc0-v0))
        // Solving for zeroes on the derivative:
        // e2*t*t + e1*t + e0 = a*t*t + b*t + c = 0
        // Note that in solving for 0 we can divide all e0,e1,e2 by 3
        // t = (-b +/- sqrt(b*b-4ac))/2a
        float c = vc0 - v0;
        float b = 2f * ((vc1 - vc0) - c);
        float a = (v1 - vc1) - b - c;
        if (a == 0f) {
            // The quadratic parabola has degenerated to a line.
            if (b == 0f) {
                // The line has degenerated to a constant.
                return;
            }
            accumulateCubic(bbox, off, -c/b, v0, vc0, vc1, v1);
        } else {
            // From Numerical Recipes, 5.6, Quadratic and Cubic Equations
            float d = b * b - 4f * a * c;
            if (d < 0f) {
                // If d < 0.0, then there are no roots
                return;
            }
            d = (float) Math.sqrt(d);
            // For accuracy, calculate one root using:
            //     (-b +/- d) / 2a
            // and the other using:
            //     2c / (-b +/- d)
            // Choose the sign of the +/- so that b+d gets larger in magnitude
            if (b < 0f) {
                d = -d;
            }
            float q = (b + d) / -2f;
            // We already tested a for being 0 above
            accumulateCubic(bbox, off, q/a, v0, vc0, vc1, v1);
            if (q != 0f) {
                accumulateCubic(bbox, off, c/q, v0, vc0, vc1, v1);
            }
        }
    }

    public static void accumulateCubic(float bbox[], int off, float t,
                                       float v0, float vc0, float vc1, float v1)
    {
        if (t > 0 && t < 1) {
            float u = 1f - t;
            float v =        v0 * u * u * u
                      + 3 * vc0 * t * u * u
                      + 3 * vc1 * t * t * u
                      +      v1 * t * t * t;
            if (bbox[off] > v) bbox[off] = v;
            if (bbox[off+2] < v) bbox[off+2] = v;
        }
    }
}
