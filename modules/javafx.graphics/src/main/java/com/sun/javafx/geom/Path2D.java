/*
 * Copyright (c) 2006, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;

/**
 * The {@code Path2D} class provides a simple, yet flexible
 * shape which represents an arbitrary geometric path.
 * It can fully represent any path which can be iterated by the
 * {@link PathIterator} interface including all of its segment
 * types and winding rules and it implements all of the
 * basic hit testing methods of the {@link Shape} interface.
 * <p>
 * Use {@link Path2D} when dealing with data that can be represented
 * and used with floating point precision.
 * <p>
 * {@code Path2D} provides exactly those facilities required for
 * basic construction and management of a geometric path and
 * implementation of the above interfaces with little added
 * interpretation.
 * If it is useful to manipulate the interiors of closed
 * geometric shapes beyond simple hit testing then the
 * {@link Area} class provides additional capabilities
 * specifically targeted at closed figures.
 * While both classes nominally implement the {@code Shape}
 * interface, they differ in purpose and together they provide
 * two useful views of a geometric shape where {@code Path2D}
 * deals primarily with a trajectory formed by path segments
 * and {@code Area} deals more with interpretation and manipulation
 * of enclosed regions of 2D geometric space.
 * <p>
 * The {@link PathIterator} interface has more detailed descriptions
 * of the types of segments that make up a path and the winding rules
 * that control how to determine which regions are inside or outside
 * the path.
 *
 * @version 1.10, 05/05/07
 */
 public class Path2D extends Shape implements PathConsumer2D {

     static final int curvecoords[] = {2, 2, 4, 6, 0};

     public enum CornerPrefix {
         CORNER_ONLY,
         MOVE_THEN_CORNER,
         LINE_THEN_CORNER
     }

     /**
     * An even-odd winding rule for determining the interior of
     * a path.
     *
     * @see PathIterator#WIND_EVEN_ODD
     */
    public static final int WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD;

    /**
     * A non-zero winding rule for determining the interior of a
     * path.
     *
     * @see PathIterator#WIND_NON_ZERO
     */
    public static final int WIND_NON_ZERO = PathIterator.WIND_NON_ZERO;

    // For code simplicity, copy these constants to our namespace
    // and cast them to byte constants for easy storage.
    private static final byte SEG_MOVETO  = (byte) PathIterator.SEG_MOVETO;
    private static final byte SEG_LINETO  = (byte) PathIterator.SEG_LINETO;
    private static final byte SEG_QUADTO  = (byte) PathIterator.SEG_QUADTO;
    private static final byte SEG_CUBICTO = (byte) PathIterator.SEG_CUBICTO;
    private static final byte SEG_CLOSE   = (byte) PathIterator.SEG_CLOSE;

    byte[] pointTypes;
    int numTypes;
    int numCoords;
    int windingRule;

    static final int INIT_SIZE = 20;
    static final int EXPAND_MAX = 500;
    static final int EXPAND_MAX_COORDS = EXPAND_MAX * 2;

    float floatCoords[];
    float moveX, moveY;
    float prevX, prevY;
    float currX, currY;

    /**
     * Constructs a new empty single precision {@code Path2D} object
     * with a default winding rule of {@link #WIND_NON_ZERO}.
     */
    public Path2D() {
        this(WIND_NON_ZERO, INIT_SIZE);
    }

    /**
     * Constructs a new empty single precision {@code Path2D} object
     * with the specified winding rule to control operations that
     * require the interior of the path to be defined.
     *
     * @param rule the winding rule
     * @see #WIND_EVEN_ODD
     * @see #WIND_NON_ZERO
     */
    public Path2D(int rule) {
        this(rule, INIT_SIZE);
    }

    /**
     * Constructs a new empty single precision {@code Path2D} object
     * with the specified winding rule and the specified initial
     * capacity to store path segments.
     * This number is an initial guess as to how many path segments
     * will be added to the path, but the storage is expanded as
     * needed to store whatever path segments are added.
     *
     * @param rule the winding rule
     * @param initialCapacity the estimate for the number of path segments
     *                        in the path
     * @see #WIND_EVEN_ODD
     * @see #WIND_NON_ZERO
     */
    public Path2D(int rule, int initialCapacity) {
        setWindingRule(rule);
        this.pointTypes = new byte[initialCapacity];
        floatCoords = new float[initialCapacity * 2];
    }

    /**
     * Constructs a new single precision {@code Path2D} object
     * from an arbitrary {@link Shape} object.
     * All of the initial geometry and the winding rule for this path are
     * taken from the specified {@code Shape} object.
     *
     * @param s the specified {@code Shape} object
     */
    public Path2D(Shape s) {
        this(s, null);
    }

    /**
     * Constructs a new single precision {@code Path2D} object
     * from an arbitrary {@link Shape} object, transformed by an
     * {@link BaseTransform} object.
     * All of the initial geometry and the winding rule for this path are
     * taken from the specified {@code Shape} object and transformed
     * by the specified {@code BaseTransform} object.
     *
     * @param s the specified {@code Shape} object
     * @param tx the specified {@code BaseTransform} object
     */
    public Path2D(Shape s, BaseTransform tx) {
        if (s instanceof Path2D) {
            Path2D p2d = (Path2D) s;
            setWindingRule(p2d.windingRule);
            this.numTypes = p2d.numTypes;
            this.pointTypes = Arrays.copyOf(p2d.pointTypes, numTypes);
            this.numCoords = p2d.numCoords;
            if (tx == null || tx.isIdentity()) {
                this.floatCoords = Arrays.copyOf(p2d.floatCoords, numCoords);
                this.moveX = p2d.moveX;
                this.moveY = p2d.moveY;
                this.prevX = p2d.prevX;
                this.prevY = p2d.prevY;
                this.currX = p2d.currX;
                this.currY = p2d.currY;
            } else {
                this.floatCoords = new float[numCoords + 6];
                tx.transform(p2d.floatCoords, 0, this.floatCoords, 0, numCoords / 2);
                floatCoords[numCoords + 0] = moveX;
                floatCoords[numCoords + 1] = moveY;
                floatCoords[numCoords + 2] = prevX;
                floatCoords[numCoords + 3] = prevY;
                floatCoords[numCoords + 4] = currX;
                floatCoords[numCoords + 5] = currY;
                tx.transform(this.floatCoords, numCoords, this.floatCoords, numCoords, 3);
                moveX = floatCoords[numCoords + 0];
                moveY = floatCoords[numCoords + 1];
                prevX = floatCoords[numCoords + 2];
                prevY = floatCoords[numCoords + 3];
                currX = floatCoords[numCoords + 4];
                currY = floatCoords[numCoords + 5];
            }
        } else {
            PathIterator pi = s.getPathIterator(tx);
            setWindingRule(pi.getWindingRule());
            this.pointTypes = new byte[INIT_SIZE];
            this.floatCoords = new float[INIT_SIZE * 2];
            append(pi, false);
        }
    }


     /**
      * Construct a Path2D from pre-composed data.
      * Used by internal font code which has obtained the path data
      * for a glyph outline, and which promises not to
      * mess with the arrays, dropping all other references,
      so there's no need to clone them here.
      */
    public Path2D(int windingRule,
                  byte[] pointTypes,
                  int numTypes,
                  float[] pointCoords,
                  int numCoords)
    {
        this.windingRule = windingRule;
        this.pointTypes = pointTypes;
        this.numTypes = numTypes;
        this.floatCoords = pointCoords;
        this.numCoords = numCoords;
    }

    Point2D getPoint(int coordindex) {
        return new Point2D(floatCoords[coordindex],
                           floatCoords[coordindex+1]);
    }

    private boolean close(int ix, float fx, float tolerance) {
        return (Math.abs(ix - fx) <= tolerance);
    }

    /**
     * Check and return if the fillable interior of the path is a simple
     * rectangle on nearly integer bounds and initialize the indicated
     * {@link Rectangle} with the integer representation of the rectangle
     * if it is.
     * The method will return false if the path is not rectangular, or if
     * the horizontal and linear segments are not within the indicated
     * tolerance of an integer coordinate, or if the resulting rectangle
     * cannot be safely represented by the integer attributes of the
     * {@code Rectangle} object.
     *
     * @param retrect the {@code Rectangle} to return the rectangular area,
     *                or null
     * @param tolerance the maximum difference from an integer allowed
     *                  for any edge of the rectangle
     * @return true iff the path is a simple rectangle
     */
    public boolean checkAndGetIntRect(Rectangle retrect, float tolerance) {
        // Valid rectangular paths are:
        //     4 segs: MOVE, LINE, LINE, LINE (implicit CLOSE)
        //     5 segs: MOVE, LINE, LINE, LINE, LINE
        //     5 segs: MOVE, LINE, LINE, LINE, CLOSE
        //     6 segs: MOVE, LINE, LINE, LINE, LINE, CLOSE
        if (numTypes == 5) {
            // points[4] can be LINETO or CLOSE
            if (pointTypes[4] != SEG_LINETO && pointTypes[4] != SEG_CLOSE) {
                return false;
            }
        } else if (numTypes == 6) {
            // points[4] must be LINETO and
            // points[5] must be CLOSE
            if (pointTypes[4] != SEG_LINETO) return false;
            if (pointTypes[5] != SEG_CLOSE) return false;
        } else if (numTypes != 4) {
            return false;
        }
        if (pointTypes[0] != SEG_MOVETO) return false;
        if (pointTypes[1] != SEG_LINETO) return false;
        if (pointTypes[2] != SEG_LINETO) return false;
        if (pointTypes[3] != SEG_LINETO) return false;

        int x0 = (int) (floatCoords[0] + 0.5f);
        int y0 = (int) (floatCoords[1] + 0.5f);
        if (!close(x0, floatCoords[0], tolerance)) return false;
        if (!close(y0, floatCoords[1], tolerance)) return false;

        int x1 = (int) (floatCoords[2] + 0.5f);
        int y1 = (int) (floatCoords[3] + 0.5f);
        if (!close(x1, floatCoords[2], tolerance)) return false;
        if (!close(y1, floatCoords[3], tolerance)) return false;

        int x2 = (int) (floatCoords[4] + 0.5f);
        int y2 = (int) (floatCoords[5] + 0.5f);
        if (!close(x2, floatCoords[4], tolerance)) return false;
        if (!close(y2, floatCoords[5], tolerance)) return false;

        int x3 = (int) (floatCoords[6] + 0.5f);
        int y3 = (int) (floatCoords[7] + 0.5f);
        if (!close(x3, floatCoords[6], tolerance)) return false;
        if (!close(y3, floatCoords[7], tolerance)) return false;

        if (numTypes > 4 && pointTypes[4] == SEG_LINETO) {
            if (!close(x0, floatCoords[8], tolerance)) return false;
            if (!close(y0, floatCoords[9], tolerance)) return false;
        }

        if ((x0 == x1 && x2 == x3 && y0 == y3 && y1 == y2) ||
            (y0 == y1 && y2 == y3 && x0 == x3 && x1 == x2))
        {
            // We can use either diagonal to calculate the rectangle:
            //     (x0, y0) -> (x2, y2)
            //     (x1, y1) -> (x3, y3)
            // We also need to deal with upside down and/or backwards rectangles
            int x, y, w, h;
            if (x2 < x0) { x = x2; w = x0 - x2; }
            else         { x = x0; w = x2 - x0; }
            if (y2 < y0) { y = y2; h = y0 - y2; }
            else         { y = y0; h = y2 - y0; }
            // Overflow protection...
            if (w < 0) return false;
            if (h < 0) return false;

            if (retrect != null) {
                retrect.setBounds(x, y, w, h);
            }
            return true;
        }
        return false;
    }

    void needRoom(boolean needMove, int newCoords) {
        if (needMove && (numTypes == 0)) {
            throw new IllegalPathStateException("missing initial moveto "+
                                                "in path definition");
        }
        int size = pointTypes.length;
        if (size == 0) {
            pointTypes = new byte[2];
        } else if (numTypes >= size) {
            pointTypes = expandPointTypes(pointTypes, 1);
        }
        size = floatCoords.length;
        if (numCoords > (floatCoords.length - newCoords)) {
            floatCoords = expandCoords(floatCoords, newCoords);
        }
    }

    static byte[] expandPointTypes(byte[] oldPointTypes, int needed) {
        final int oldSize = oldPointTypes.length;
        final int newSizeMin = oldSize + needed;
        if (newSizeMin < oldSize) {
            // hard overflow failure - we can't even accommodate
            // new items without overflowing
            throw new ArrayIndexOutOfBoundsException(
                          "pointTypes exceeds maximum capacity !");
        }
        // growth algorithm computation
        int grow = oldSize;
        if (grow > EXPAND_MAX) {
            grow = Math.max(EXPAND_MAX, oldSize >> 3); // 1/8th min
        } else if (grow < INIT_SIZE) {
            grow = INIT_SIZE; // ensure > 6 (cubics)
        }
        assert grow > 0;

        int newSize = oldSize + grow;
        if (newSize < newSizeMin) {
            // overflow in growth algorithm computation
            newSize = Integer.MAX_VALUE;
        }

        while (true) {
            try {
                // try allocating the larger array
                return Arrays.copyOf(oldPointTypes, newSize);
            } catch (OutOfMemoryError oome) {
                if (newSize == newSizeMin) {
                    throw oome;
                }
            }
            newSize = newSizeMin + (newSize - newSizeMin) / 2;
        }
    }

    static float[] expandCoords(float[] oldCoords, int needed) {
        final int oldSize = oldCoords.length;
        final int newSizeMin = oldSize + needed;
        if (newSizeMin < oldSize) {
            // hard overflow failure - we can't even accommodate
            // new items without overflowing
            throw new ArrayIndexOutOfBoundsException(
                          "coords exceeds maximum capacity !");
        }
        // growth algorithm computation
        int grow = oldSize;
        if (grow > EXPAND_MAX_COORDS) {
            grow = Math.max(EXPAND_MAX_COORDS, oldSize >> 3); // 1/8th min
        } else if (grow < INIT_SIZE) {
            grow = INIT_SIZE; // ensure > 6 (cubics)
        }
        assert grow > needed;

        int newSize = oldSize + grow;
        if (newSize < newSizeMin) {
            // overflow in growth algorithm computation
            newSize = Integer.MAX_VALUE;
        }
        while (true) {
            try {
                // try allocating the larger array
                return Arrays.copyOf(oldCoords, newSize);
            } catch (OutOfMemoryError oome) {
                if (newSize == newSizeMin) {
                    throw oome;
                }
            }
            newSize = newSizeMin + (newSize - newSizeMin) / 2;
        }
    }

    /**
     * Adds a point to the path by moving to the specified
     * coordinates specified in float precision.
     *
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     */
    @Override
    public final void moveTo(float x, float y) {
        if (numTypes > 0 && pointTypes[numTypes - 1] == SEG_MOVETO) {
            floatCoords[numCoords-2] = moveX = prevX = currX = x;
            floatCoords[numCoords-1] = moveY = prevY = currY = y;
        } else {
            needRoom(false, 2);
            pointTypes[numTypes++] = SEG_MOVETO;
            floatCoords[numCoords++] = moveX = prevX = currX = x;
            floatCoords[numCoords++] = moveY = prevY = currY = y;
        }
    }

    /**
     * Adds a point to the path by moving to the specified coordinates
     * relative to the current point, specified in float precision.
     *
     * @param relx the specified relative X coordinate
     * @param rely the specified relative Y coordinate
     * @see Path2D#moveTo
     */
    public final void moveToRel(float relx, float rely) {
        if (numTypes > 0 && pointTypes[numTypes - 1] == SEG_MOVETO) {
            floatCoords[numCoords-2] = moveX = prevX = (currX += relx);
            floatCoords[numCoords-1] = moveY = prevY = (currY += rely);
        } else {
            needRoom(true, 2);
            pointTypes[numTypes++] = SEG_MOVETO;
            floatCoords[numCoords++] = moveX = prevX = (currX += relx);
            floatCoords[numCoords++] = moveY = prevY = (currY += rely);
        }
    }

    /**
     * Adds a point to the path by drawing a straight line from the
     * current coordinates to the new coordinates.
     *
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     */
    @Override
    public final void lineTo(float x, float y) {
        needRoom(true, 2);
        pointTypes[numTypes++] = SEG_LINETO;
        floatCoords[numCoords++] = prevX = currX = x;
        floatCoords[numCoords++] = prevY = currY = y;
    }

    /**
     * Adds a point to the path by drawing a straight line from the
     * current coordinates to the new coordinates relative to the
     * current point.
     *
     * @param relx the specified relative X coordinate
     * @param rely the specified relative Y coordinate
     * @see Path2D#lineTo
     */
    public final void lineToRel(float relx, float rely) {
        needRoom(true, 2);
        pointTypes[numTypes++] = SEG_LINETO;
        floatCoords[numCoords++] = prevX = (currX += relx);
        floatCoords[numCoords++] = prevY = (currY += rely);
    }

    /**
     * Adds a curved segment to the path, defined by two new points, by
     * drawing a Quadratic curve that intersects both the current
     * coordinates and the specified coordinates {@code (x2,y2)},
     * using the specified point {@code (x1,y1)} as a quadratic
     * parametric control point.
     *
     * @param x1 the X coordinate of the quadratic control point
     * @param y1 the Y coordinate of the quadratic control point
     * @param x2 the X coordinate of the final end point
     * @param y2 the Y coordinate of the final end point
     */
    @Override
    public final void quadTo(float x1, float y1,
                             float x2, float y2)
    {
        needRoom(true, 4);
        pointTypes[numTypes++] = SEG_QUADTO;
        floatCoords[numCoords++] = prevX = x1;
        floatCoords[numCoords++] = prevY = y1;
        floatCoords[numCoords++] = currX = x2;
        floatCoords[numCoords++] = currY = y2;
    }

    /**
     * Adds a curved segment to the path, defined by two new points
     * relative to the current point, by
     * drawing a Quadratic curve that intersects both the current
     * coordinates and the specified relative coordinates {@code (rx2,ry2)},
     * using the specified relative point {@code (rx1,ry1)} as a quadratic
     * parametric control point.
     * This is equivalent to:
     * <pre>
     *     quadTo(getCurrentX() + rx1, getCurrentY() + ry1,
     *            getCurrentX() + rx2, getCurrentY() + ry2);
     * </pre>
     *
     * @param relx1 the relative X coordinate of the quadratic control point
     * @param rely1 the relative Y coordinate of the quadratic control point
     * @param relx2 the relative X coordinate of the final end point
     * @param rely2 the relative Y coordinate of the final end point
     * @see Path2D#quadTo
     */
    public final void quadToRel(float relx1, float rely1,
                                float relx2, float rely2)
    {
        needRoom(true, 4);
        pointTypes[numTypes++] = SEG_QUADTO;
        floatCoords[numCoords++] = prevX = currX + relx1;
        floatCoords[numCoords++] = prevY = currY + rely1;
        floatCoords[numCoords++] = (currX += relx2);
        floatCoords[numCoords++] = (currY += rely2);
    }

    /**
     * Adds a curved segment to the path, defined by a new point, by
     * drawing a Quadratic curve that intersects both the current
     * coordinates and the specified coordinates {@code (x,y)},
     * using a quadratic parametric control point that is positioned
     * symmetrically across the current point from the previous curve
     * control point.
     * If the previous path segment is not a curve, then the control
     * point will be positioned at the current point.  This is
     * equivalent to:
     * <pre>
     *     quadTo(getCurrentX() * 2 - <previousControlX>,
     *            getCurrentY() * 2 - <previousControlY>,
     *            x, y);
     * </pre>
     *
     * @param x2 the X coordinate of the final end point
     * @param y2 the Y coordinate of the final end point
     * @see Path2D#quadTo
     */
    public final void quadToSmooth(float x2, float y2) {
        needRoom(true, 4);
        pointTypes[numTypes++] = SEG_QUADTO;
        floatCoords[numCoords++] = prevX = (currX * 2.0f - prevX);
        floatCoords[numCoords++] = prevY = (currY * 2.0f - prevY);
        floatCoords[numCoords++] = currX = x2;
        floatCoords[numCoords++] = currY = y2;
    }

    /**
     * Adds a curved segment to the path, defined by a new point
     * relative to the current point, by
     * drawing a Quadratic curve that intersects both the current
     * coordinates and the specified relative coordinates {@code (x,y)},
     * using a quadratic parametric control point that is positioned
     * symmetrically across the current point from the previous curve
     * control point.
     * If the previous path segment is not a curve, then the control
     * point will be positioned at the current point.  This is
     * equivalent to:
     * <pre>
     *     quadTo(getCurrentX() * 2 - <previousControlX>,
     *            getCurrentY() * 2 - <previousControlY>,
     *            getCurrentX() + x, getCurrentY() + y);
     * </pre>
     *
     * @param relx2 the relative X coordinate of the final end point
     * @param rely2 the relative Y coordinate of the final end point
     * @see Path2D#quadTo
     */
    public final void quadToSmoothRel(float relx2, float rely2) {
        needRoom(true, 4);
        pointTypes[numTypes++] = SEG_QUADTO;
        floatCoords[numCoords++] = prevX = (currX * 2.0f - prevX);
        floatCoords[numCoords++] = prevY = (currY * 2.0f - prevY);
        floatCoords[numCoords++] = (currX += relx2);
        floatCoords[numCoords++] = (currY += rely2);
    }

    /**
     * Adds a curved segment to the path, defined by three new points, by
     * drawing a B&eacute;zier curve that intersects both the current
     * coordinates and the specified coordinates {@code (x3,y3)},
     * using the specified points {@code (x1,y1)} and {@code (x2,y2)} as
     * B&eacute;zier control points.
     *
     * @param x1 the X coordinate of the first B&eacute;zier control point
     * @param y1 the Y coordinate of the first B&eacute;zier control point
     * @param x2 the X coordinate of the second B&eacute;zier control point
     * @param y2 the Y coordinate of the second B&eacute;zier control point
     * @param x3 the X coordinate of the final end point
     * @param y3 the Y coordinate of the final end point
     * @see Path2D#curveTo
     */
    @Override
    public final void curveTo(float x1, float y1,
                              float x2, float y2,
                              float x3, float y3)
    {
        needRoom(true, 6);
        pointTypes[numTypes++] = SEG_CUBICTO;
        floatCoords[numCoords++] = x1;
        floatCoords[numCoords++] = y1;
        floatCoords[numCoords++] = prevX = x2;
        floatCoords[numCoords++] = prevY = y2;
        floatCoords[numCoords++] = currX = x3;
        floatCoords[numCoords++] = currY = y3;
    }

    /**
     * Adds a curved segment to the path, defined by three new points
     * relative to the current point, by
     * drawing a B&eacute;zier curve that intersects both the current
     * coordinates and the specified coordinates {@code (x3,y3)},
     * using the specified points {@code (x1,y1)} and {@code (x2,y2)} as
     * B&eacute;zier control points.
     * This is equivalent to:
     * <pre>
     *     curveTo(getCurrentX() + rx1, getCurrentY() + ry1,
     *             getCurrentX() + rx2, getCurrentY() + ry2,
     *             getCurrentX() + rx3, getCurrentY() + ry3)
     * </pre>
     *
     * @param relx1 the relative X coordinate of the first B&eacute;zier control point
     * @param rely1 the relative Y coordinate of the first B&eacute;zier control point
     * @param relx2 the relative X coordinate of the second B&eacute;zier control point
     * @param rely2 the relative Y coordinate of the second B&eacute;zier control point
     * @param relx3 the relative X coordinate of the final end point
     * @param rely3 the relative Y coordinate of the final end point
     * @see Path2D#curveTo
     */
    public final void curveToRel(float relx1, float rely1,
                                 float relx2, float rely2,
                                 float relx3, float rely3)
    {
        needRoom(true, 6);
        pointTypes[numTypes++] = SEG_CUBICTO;
        floatCoords[numCoords++] = currX + relx1;
        floatCoords[numCoords++] = currY + rely1;
        floatCoords[numCoords++] = prevX = currX + relx2;
        floatCoords[numCoords++] = prevY = currY + rely2;
        floatCoords[numCoords++] = (currX += relx3);
        floatCoords[numCoords++] = (currY += rely3);
    }

    /**
     * Adds a curved segment to the path, defined by two new points and
     * a third point inferred from the previous curve, by
     * drawing a B&eacute;zier curve that intersects both the current
     * coordinates and the specified coordinates {@code (x3,y3)},
     * using the specified point {@code (x2,y2)} as the second
     * B&eacute;zier control point and a first B&eacute;zier control
     * point that is positioned
     * symmetrically across the current point from the previous curve
     * control point.
     * This is equivalent to:
     * <pre>
     *     curveTo(getCurrentX() * 2.0f - <previousControlX>,
     *             getCurrentY() * 2.0f - <previousControlY>,
     *             x2, y2, x3, y3);
     * </pre>
     *
     * @param x2 the X coordinate of the second B&eacute;zier control point
     * @param y2 the Y coordinate of the second B&eacute;zier control point
     * @param x3 the X coordinate of the final end point
     * @param y3 the Y coordinate of the final end point
     * @see Path2D#curveTo
     */
    public final void curveToSmooth(float x2, float y2,
                                    float x3, float y3)
    {
        needRoom(true, 6);
        pointTypes[numTypes++] = SEG_CUBICTO;
        floatCoords[numCoords++] = currX * 2.0f - prevX;
        floatCoords[numCoords++] = currY * 2.0f - prevY;
        floatCoords[numCoords++] = prevX = x2;
        floatCoords[numCoords++] = prevY = y2;
        floatCoords[numCoords++] = currX = x3;
        floatCoords[numCoords++] = currY = y3;
    }

    /**
     * Adds a curved segment to the path, defined by two new points relative
     * to the current point and
     * a third point inferred from the previous curve, by
     * drawing a B&eacute;zier curve that intersects both the current
     * coordinates and the specified relative coordinates {@code (rx3,ry3)},
     * using the specified relative point {@code (rx2,ry2)} as the second
     * B&eacute;zier control point and a first B&eacute;zier control
     * point that is positioned
     * symmetrically across the current point from the previous curve
     * control point.
     * This is equivalent to:
     * <pre>
     *     curveTo(getCurrentX() * 2.0f - <previousControlX>,
     *             getCurrentY() * 2.0f - <previousControlY>,
     *             getCurrentX() + x2, getCurrentY() + y2,
     *             getCurrentX() + x3, getCurrentY() + y3);
     * </pre>
     *
     * @param relx2 the relative X coordinate of the second B&eacute;zier control point
     * @param rely2 the relative Y coordinate of the second B&eacute;zier control point
     * @param relx3 the relative X coordinate of the final end point
     * @param rely3 the relative Y coordinate of the final end point
     * @see Path2D#curveTo
     */
    public final void curveToSmoothRel(float relx2, float rely2,
                                       float relx3, float rely3)
    {
        needRoom(true, 6);
        pointTypes[numTypes++] = SEG_CUBICTO;
        floatCoords[numCoords++] = currX * 2.0f - prevX;
        floatCoords[numCoords++] = currY * 2.0f - prevY;
        floatCoords[numCoords++] = prevX = currX + relx2;
        floatCoords[numCoords++] = prevY = currY + rely2;
        floatCoords[numCoords++] = (currX += relx3);
        floatCoords[numCoords++] = (currY += rely3);
    }

    /**
     * Append a section of a quadrant of an oval to the current path,
     * relative to the current point.
     * See {@link appendOvalQuadrant} for a precise definition of the
     * path segments to be added, considering that this method uses the
     * current point of the path as the first pair of coordinates and
     * a hard-coded prefix of {@link CornerPrefix.CORNER_ONLY CORNER_ONLY}.
     * This method is equivalent to (and only slightly faster than):
     * <pre>
     *     appendOvalQuadrant(getCurrentX(), getCurrentY(),
     *                        cx, cy, ex, ey, tfrom, tto,
     *                        CornerPrefix.CORNER_ONLY);
     * </pre>
     * Note that you could define a circle inscribed in the rectangular
     * bounding box from {@code (x0, y0)} to {@code (x1, y1)} with the
     * following 4 calls to this method:
     * <pre>
     *     Path2D path = new Path2D();
     *     float cx = (x0 + x1) * 0.5f; // center X coordinate of top and bottom
     *     float cy = (y0 + y1) * 0.5f; // center Y coordinate of left and right
     *     path.moveTo(cx, y0);
     *     path.ovalQuadrantTo(x1, y0, x1, cy, 0f, 1f);
     *     path.ovalQuadrantTo(x1, y1, cx, y1, 0f, 1f);
     *     path.ovalQuadrantTo(x0, y1, x0, cy, 0f, 1f);
     *     path.ovalQuadrantTo(x0, y0, cx, y0, 0f, 1f);
     *     path.closePath();
     * </pre>
     * You could also define a rounded rectangle inscribed in the rectangular
     * bounding box from {@code (x0, y0)} to {@code (x1, y1)} with a corner
     * arc radius {@code r} less than half the width and the height with the
     * following 4 calls to this method:
     * <pre>
     *     Path2D path = new Path2D();
     *     float lx = x0 + r;
     *     float rx = x1 - r;
     *     float ty = y0 + r;
     *     float by = y1 - r;
     *     path.moveTo(rx, y0);
     *     path.ovalQuadrantTo(x1, y0, x1, ty, 0f, 1f);
     *     path.lineTo(x1, by);
     *     path.ovalQuadrantTo(x1, y1, rx, y1, 0f, 1f);
     *     path.lineTo(lx, y1);
     *     path.ovalQuadrantTo(x0, y1, x0, by, 0f, 1f);
     *     path.lineTo(x0, by);
     *     path.ovalQuadrantTo(x0, y0, lx, y0, 0f, 1f);
     *     path.closePath();
     * </pre>
     *
     * @param cx the X coordinate of the corner
     * @param cy the Y coordinate of the corner
     * @param ex the X coordinate of the midpoint of the trailing edge
     *           interpolated by the oval
     * @param ey the Y coordinate of the midpoint of the trailing edge
     *           interpolated by the oval
     * @param tfrom the fraction of the oval section where the curve should start
     * @param tto the fraction of the oval section where the curve should end
     * @throws IllegalPathStateException
     *     if there is no current point in the path
     * @throws IllegalArgumentException
     *     if the {@code tfrom} and {@code tto} values do not satisfy the
     *     required relationship {@code (0 <= tfrom <= tto <= 1).
     */
    public final void ovalQuadrantTo(float cx, float cy,
                                     float ex, float ey,
                                     float tfrom, float tto)
    {
        if (numTypes < 1) {
            throw new IllegalPathStateException("missing initial moveto "+
                                                "in path definition");
        }
        appendOvalQuadrant(currX, currY,
                           cx, cy, ex, ey, tfrom, tto, CornerPrefix.CORNER_ONLY);
    }

    /**
     * Append a section of a quadrant of an oval to the current path.
     * The oval from which a quadrant is taken is the oval that would be
     * inscribed in a parallelogram defined by 3 points,
     * {@code (sx, sy)} which is considered to be the midpoint of the edge
     * leading into the corner of the oval where the oval grazes it,
     * {@code (cx, cy)} which is considered to be the location of the
     * corner of the parallelogram in which the oval is inscribed,
     * and {@code (ex, ey)} which is considered to be the midpoint of the
     * edge leading away from the corner of the oval where the oval grazes it.
     * A typical case involves the two segments being equal in length and
     * at right angles to each other in which case the oval is a quarter of
     * a circle.
     * <p>
     * Only the portion of the oval from {@code tfrom} to {@code tto}
     * will be included where {@code 0f} represents the point where the
     * oval grazes the leading edge, {@code 1f} represents the point where
     * the oval grazes the trailing edge, and {@code 0.5f} represents the
     * point on the oval closest to the corner (i.e. the "45 degree" point).
     * The two values must satisfy the relation
     * {@code (0 <= tfrom <= tto <= 1)}.
     * If {@code tfrom} is not {@code 0f} then the caller would most likely
     * want to use one of the {@code prefix} values that inserts a segment
     * leading to the initial point (see below).
     * <p>
     * An initial {@link moveTo} or {@link lineTo} can be added to direct
     * the path to the starting point of the oval section if
     * {@link CornerPrefix.MOVE_THEN_CORNER MOVE_THEN_CORNER} or
     * {@link CornerPrefix.LINE_THEN_CORNER LINE_THEN_CORNER} are
     * specified by the prefix argument.
     * The {@code lineTo} path segment will only be added if the current point
     * is not already at the indicated location to avoid spurious empty line
     * segments.
     * The prefix can be specified as
     * {@link CornerPrefix.CORNER_ONLY CORNER_ONLY} if the current point
     * on the path is known to be at the starting point of the oval section,
     * but could otherwise produce odd results if the current point is not
     * appropriate.
     * <p>
     * Note that you could define a circle inscribed in the rectangular
     * bounding box from {@code (x0, y0)} to {@code (x1, y1)} with the
     * following 4 calls to this method:
     * <pre>
     *     Path2D path = new Path2D();
     *     float cx = (x0 + x1) * 0.5f; // center X coordinate of top and bottom
     *     float cy = (y0 + y1) * 0.5f; // center Y coordinate of left and right
     *     path.appendOvalQuadrant(cx, y0, x1, y0, x1, cy, 0f, 1f, MOVE_THEN_CORNER);
     *     path.appendOvalQuadrant(x1, cy, x1, y1, cx, y1, 0f, 1f, CORNER_ONLY);
     *     path.appendOvalQuadrant(cx, y1, x0, y1, x0, cy, 0f, 1f, CORNER_ONLY);
     *     path.appendOvalQuadrant(x0, cy, x0, y0, cx, y0, 0f, 1f, CORNER_ONLY);
     *     path.closePath();
     * </pre>
     * You could also define a rounded rectangle inscribed in the rectangular
     * bounding box from {@code (x0, y0)} to {@code (x1, y1)} with a corner
     * arc radius {@code r} less than half the width and the height with the
     * following 4 calls to this method:
     * <pre>
     *     Path2D path = new Path2D();
     *     float lx = x0 + r;
     *     float rx = x1 - r;
     *     float ty = y0 + r;
     *     float by = y1 - r;
     *     path.appendOvalQuadrant(rx, y0, x1, y0, x1, ty, 0f, 1f, MOVE_THEN_CORNER);
     *     path.appendOvalQuadrant(x1, by, x1, y1, rx, y1, 0f, 1f, LINE_THEN_CORNER);
     *     path.appendOvalQuadrant(lx, y1, x0, y1, x0, by, 0f, 1f, LINE_THEN_CORNER);
     *     path.appendOvalQuadrant(x0, by, x0, y0, lx, y0, 0f, 1f, LINE_THEN_CORNER);
     *     path.closePath();
     * </pre>
     *
     * @param sx the X coordinate of the midpoint of the leading edge
     *           interpolated by the oval
     * @param sy the Y coordinate of the midpoint of the leading edge
     *           interpolated by the oval
     * @param cx the X coordinate of the corner
     * @param cy the Y coordinate of the corner
     * @param ex the X coordinate of the midpoint of the trailing edge
     *           interpolated by the oval
     * @param ey the Y coordinate of the midpoint of the trailing edge
     *           interpolated by the oval
     * @param tfrom the fraction of the oval section where the curve should start
     * @param tto the fraction of the oval section where the curve should end
     * @param prefix the specification of what additional path segments should
     *               be appended to lead the current path to the starting point
     * @throws IllegalPathStateException
     *     if there is no current point in the path and the prefix is
     *     not {@code CornerPrevix.MOVE_THEN_CORNER MOVE_THEN_CORNER}.
     * @throws IllegalArgumentException
     *     if the {@code tfrom} and {@code tto} values do not satisfy the
     *     required relationship {@code (0 <= tfrom <= tto <= 1).
     */
    public final void appendOvalQuadrant(float sx, float sy,
                                         float cx, float cy,
                                         float ex, float ey,
                                         float tfrom, float tto,
                                         CornerPrefix prefix)
    {
        if (!(tfrom >= 0f && tfrom <= tto && tto <= 1f)) {
            throw new IllegalArgumentException("0 <= tfrom <= tto <= 1 required");
        }
        float cx0 = (float) (sx + (cx - sx) * EllipseIterator.CtrlVal);
        float cy0 = (float) (sy + (cy - sy) * EllipseIterator.CtrlVal);
        float cx1 = (float) (ex + (cx - ex) * EllipseIterator.CtrlVal);
        float cy1 = (float) (ey + (cy - ey) * EllipseIterator.CtrlVal);
        if (tto < 1f) {
            float t = 1f - tto;
            ex += (cx1 - ex) * t;
            ey += (cy1 - ey) * t;
            cx1 += (cx0 - cx1) * t;
            cy1 += (cy0 - cy1) * t;
            cx0 += (sx - cx0) * t;
            cy0 += (sy - cy0) * t;
            ex += (cx1 - ex) * t;
            ey += (cy1 - ey) * t;
            cx1 += (cx0 - cx1) * t;
            cy1 += (cy0 - cy1) * t;
            ex += (cx1 - ex) * t;
            ey += (cy1 - ey) * t;
        }
        if (tfrom > 0f) {
            if (tto < 1f) {
                tfrom = tfrom / tto;
            }
            sx += (cx0 - sx) * tfrom;
            sy += (cy0 - sy) * tfrom;
            cx0 += (cx1 - cx0) * tfrom;
            cy0 += (cy1 - cy0) * tfrom;
            cx1 += (ex - cx1) * tfrom;
            cy1 += (ey - cy1) * tfrom;
            sx += (cx0 - sx) * tfrom;
            sy += (cy0 - sy) * tfrom;
            cx0 += (cx1 - cx0) * tfrom;
            cy0 += (cy1 - cy0) * tfrom;
            sx += (cx0 - sx) * tfrom;
            sy += (cy0 - sy) * tfrom;
        }
        if (prefix == CornerPrefix.MOVE_THEN_CORNER) {
            // Always execute moveTo so we break the path...
            moveTo(sx, sy);
        } else if (prefix == CornerPrefix.LINE_THEN_CORNER) {
            if (numTypes == 1 ||
                sx != currX ||
                sy != currY)
            {
                lineTo(sx, sy);
            }
        }
        if (tfrom == tto ||
            (sx == cx0 && cx0 == cx1 && cx1 == ex &&
             sy == cy0 && cy0 == cy1 && cy1 == ey))
        {
            if (prefix != CornerPrefix.LINE_THEN_CORNER) {
                lineTo(ex, ey);
            }
        } else {
            curveTo(cx0, cy0, cx1, cy1, ex, ey);
        }
    }

    /**
     * Append a portion of an ellipse to the path.
     * The ellipse from which the portions are extracted follows the rules:
     * <ul>
     * <li>The ellipse will have its X axis tilted from horizontal by the
     * angle {@code xAxisRotation} specified in radians.
     * <li>The ellipse will have the X and Y radii (viewed from its tilted
     * coordinate system) specified by {@code radiusx} and {@code radiusy}
     * unless that ellipse is too small to bridge the gap from the current
     * point to the specified destination point in which case a larger
     * ellipse with the same ratio of dimensions will be substituted instead.
     * <li>The ellipse may slide perpendicular to the direction from the
     * current point to the specified destination point so that it just
     * touches the two points.
     * The direction it slides (to the "left" or to the "right") will be
     * chosen to meet the criteria specified by the two boolean flags as
     * described below.
     * Only one direction will allow the method to meet both criteria.
     * <li>If the {@code largeArcFlag} is true, then the ellipse will sweep
     * the longer way around the ellipse that meets these criteria.
     * <li>If the {@code sweepFlag} is true, then the ellipse will sweep
     * clockwise around the ellipse that meets these criteria.
     * </ul>
     * The method will do nothing if the destination point is the same as
     * the current point.
     * The method will draw a simple line segment to the destination point
     * if either of the two radii are zero.
     * <p>
     * Note: This method adheres to the definition of an elliptical arc path
     * segment from the SVG spec:
     * <pre>
     * http://www.w3.org/TR/SVG/paths.html#PathDataEllipticalArcCommands
     * </pre>
     *
     * @param radiusx the X radius of the tilted ellipse
     * @param radiusy the Y radius of the tilted ellipse
     * @param xAxisRotation the angle of tilt of the ellipse
     * @param largeArcFlag true iff the path will sweep the long way around
     *                     the ellipse
     * @param sweepFlag true iff the path will sweep clockwise around
     *                  the ellipse
     * @param x the destination X coordinate
     * @param y the destination Y coordinate
     * @throws IllegalPathStateException
     *     if there is no current point in the path
     */
    public void arcTo(float radiusx, float radiusy, float xAxisRotation,
                      boolean largeArcFlag, boolean sweepFlag,
                      float x, float y)
    {
        // First ensure preceding moveto
        if (numTypes < 1) {
            throw new IllegalPathStateException("missing initial moveto "+
                                                "in path definition");
        }
        // Reference equations are provided for implementation assistance:
        // http://www.w3.org/TR/SVG/implnote.html#ArcImplementationNotes
        // We use the following modifications:
        // They use a secondary coordinate system which is based on
        // - translating to the midpoint between the endpoints
        // - rotating so that the xAxis is "horizontal"
        // You can see that most of their math then has their secondary
        // coordinates being divided by rx and ry everywhere so we scale
        // by 1/rx and 1/ry so that we are working on a unit circle:
        // [ x' ]   [ +cos/rx  +sin/rx ]   [ x - mx ]
        // [    ] = [                  ] * [        ]
        // [ y' ]   [ -sin/ry  +cos/ry ]   [ y - my ]
        // and reversing back to user space coordinates:
        // [ x ]   [ +cos  -sin ]   [ x' * rx ]   [ mx ]
        // [   ] = [            ] * [         ] + [    ]
        // [ y ]   [ +sin  +cos ]   [ y' * ry ]   [ my ]
        double rx = Math.abs(radiusx);
        double ry = Math.abs(radiusy);
        if (rx == 0 || ry == 0) {
            lineTo(x, y);
            return;
        }
        double x1 = currX;
        double y1 = currY;
        double x2 = x;
        double y2 = y;
        if (x1 == x2 && y1 == y2) {
            return;
        }
        double cosphi, sinphi;
        if (xAxisRotation == 0.0) {
            cosphi = 1.0;
            sinphi = 0.0;
        } else {
            cosphi = Math.cos(xAxisRotation);
            sinphi = Math.sin(xAxisRotation);
        }
        double mx = (x1 + x2) / 2.0;
        double my = (y1 + y2) / 2.0;
        double relx1 = x1 - mx;
        double rely1 = y1 - my;
        double x1p = (cosphi * relx1 + sinphi * rely1) / rx;
        double y1p = (cosphi * rely1 - sinphi * relx1) / ry;
        // The documentation for the SVG arc operator recommends computing
        // a "scale" value and then scaling the radii appropriately if the
        // scale is greater than 1.  Technically, they are computing the
        // ratio of the distance to the endpoints compared to the distance
        // across the indicated section of the ellipse centered at the midpoint.
        // If the ratio is greater than 1 then the endpoints are outside the
        // ellipse and so the ellipse is not large enough to bridge the gap
        // without growing.  If they are inside, then we slide the ellipse
        // in the appropriate direction as specified by the 2 flags so that
        // the transformed relative points are on the edge.  If they
        // are outside, then we note that we simply have a (distorted) half
        // circle to render in that case since the endpoints will be on
        // opposite sides of a stretched version of the unmoved ellipse.
        double lenpsq = x1p * x1p + y1p * y1p;
        if (lenpsq >= 1.0) {
            // Unlike the reference equations, we do not need to scale the
            // radii here since we will work directly from the transformed
            // relative vectors which already have the proper distance from
            // the midpoint.  (They are already on the "stretched" ellipse.)

            // Produce 2 quadrant circles from:
            // x1p,y1p => xqp,yqp => x2p,y2p
            // where x2p,y2p = -x1p,-y1p
            // and xqp,yqp = either y1p,-x1p or -y1p,x1p depending on sweepFlag
            // the corners of the quadrants are at:
            // x1p+xqp,y1p+yqp and x2p+xqp,y2p+yqp
            // or consequently at:
            // x1+(xq-mx),y1+(yq-my) and x2+(xq-mx),y2+(yq-my)
            double xqpr = y1p * rx;
            double yqpr = x1p * ry;
            if (sweepFlag) { xqpr = -xqpr; } else { yqpr = -yqpr; }
            double relxq = cosphi * xqpr - sinphi * yqpr;
            double relyq = cosphi * yqpr + sinphi * xqpr;
            double xq = mx + relxq;
            double yq = my + relyq;
            double xc = x1 + relxq;
            double yc = y1 + relyq;
            appendOvalQuadrant((float) x1, (float) y1,
                               (float) xc, (float) yc,
                               (float) xq, (float) yq,
                               0f, 1f, CornerPrefix.CORNER_ONLY);
            xc = x2 + relxq;
            yc = y2 + relyq;
            appendOvalQuadrant((float) xq, (float) yq,
                               (float) xc, (float) yc,
                               (float) x2, (float) y2,
                               0f, 1f, CornerPrefix.CORNER_ONLY);
            return;
        }
        // We now need to displace the circle perpendicularly to the line
        // between the end points so that the new center is at a unit distance
        // to either end point.  One component of the new distance will be
        // the distance from the midpoint to either end point (the square
        // of which is already computed in "den" above).  The other component
        // of the new distances will be how far we displace the center:
        // lenpsq + displen^2 = 1.0
        // displen^2 = 1.0 - lenpsq
        // displen = sqrt(1 - lenpsq)
        // The vector we displace along is the perpendicular of the x1p,y1p
        // vector whose length is sqrt(lenpsq) so we need to divide that vector
        // by that length to turn it into a unit vector:
        // cxp = +/-y1p / sqrt(lenpsq) * displen
        // cyp = +/-x1p / sqrt(lenpsq) * displen
        // To simplify, we combine the "/sqrt(lenpsq)" factor into displen to
        // share the one sqrt() calculation:
        // scalef = displen / sqrt(lenpsq) = sqrt((1-lenpsq)/lenpsq)
        double scalef = Math.sqrt((1.0 - lenpsq) / lenpsq);
        // cxp,cyp is displaced perpendicularly to the relative vector x1p,y1p
        // by the scalef value.  The perpendicular is either -y1p,x1p or
        // y1p,-x1p depending on the values of the flags.
        double cxp = scalef * y1p;
        double cyp = scalef * x1p;
        // The direction of the perpendicular (which component is negated)
        // depends on both flags.
        if (largeArcFlag == sweepFlag) { cxp = -cxp; } else { cyp = -cyp; }
        mx += (cosphi * cxp * rx - sinphi * cyp * ry);
        my += (cosphi * cyp * ry + sinphi * cxp * rx);
        // Now we sweep by quadrants in the direction specified until we
        // reach the angle to the destination point and possibly perform
        // one last partial-quadrant arc segment.
        // First we need to reexpress our vectors relative to the new center.
        double ux = x1p - cxp;
        double uy = y1p - cyp;
        // x2p = -x1p; x2p-cxp = -x1p-cxp = -(x1p+cxp)
        // y2p = -y1p; y2p-cyp = -y1p-cyp = -(y1p+cyp)
        double vx = -(x1p + cxp);
        double vy = -(y1p + cyp);
        // px and py are the factors that produce the perpendicular for ux,uy
        // in the direction specified by sweepFlag.
        boolean done = false;  // set to true when we detect "last quadrant"
        float quadlen = 1.0f;  // 1.0 yields a full 90 degree arc at a time
        boolean wasclose = false;  // overshoot prevention
        do {
            // Compute the next circle quadrant endpoint, cw or ccw
            double xqp = uy;
            double yqp = ux;
            if (sweepFlag) { xqp = -xqp; } else { yqp = -yqp; }
            // qp.v > 0 tells us if sweep towards v is < 180
            if (xqp * vx + yqp * vy > 0) {
                // u.v >= 0 now tells us if sweep towards v is <= 90
                // (It is also true for >270, but we already checked for <180)
                double dot = ux * vx + uy * vy;
                if (dot >= 0) {
                    // u.v is the cosine of the angle we have left since both
                    // u and v are unit vectors.  We now need to express how
                    // much we want to shorten this last arc segment in terms
                    // of 0.0=>1.0 meaning 0=>90 degrees.
                    quadlen = (float) (Math.acos(dot) / (Math.PI / 2.0));
                    done = true;
                }
                // Remember that we were once within 180 degrees so we
                // do not accidentally overshoot due to fp rounding error.
                wasclose = true;
            } else if (wasclose) {
                // At some point we were in the <180 case above, but now we
                // are back at the >180 case having never gone into the <90
                // case where done would have been set to true.  This should
                // not happen, but since we are computing the perpendiculars
                // and then expecting that they will have predictable results
                // in the dot product equations, there is a theoretical chance
                // of a tiny round-off error that would cause us to overshoot
                // from just barely >90 left to suddenly past the 0 point.
                // If that ever happens, we will end up in here and we can just
                // break out of the loop since that last quadrant we rendered
                // should have landed us right on top of the vx,vy location.
                break;
            }
            double relxq = (cosphi * xqp * rx - sinphi * yqp * ry);
            double relyq = (cosphi * yqp * ry + sinphi * xqp * rx);
            double xq = mx + relxq;
            double yq = my + relyq;
            double xc = x1 + relxq;
            double yc = y1 + relyq;
            appendOvalQuadrant((float) x1, (float) y1,
                               (float) xc, (float) yc,
                               (float) xq, (float) yq,
                               0f, quadlen, CornerPrefix.CORNER_ONLY);
            x1 = xq;
            y1 = yq;
            ux = xqp;
            uy = yqp;
        } while (!done);
    }

    /**
     * Append a portion of an ellipse to the path using relative coordinates.
     * This method is identical to calling:
     * <pre>
     *     arcTo(radiusX, radiusY, xAxisRotation,
     *           largeArcFlag, sweepFlag,
     *           getCurrentX() + rx, getCurrentY() + ry);
     * </pre>
     *
     * @param radiusx the X radius of the tilted ellipse
     * @param radiusy the Y radius of the tilted ellipse
     * @param xAxisRotation the angle of tilt of the ellipse
     * @param largeArcFlag true iff the path will sweep the long way around
     *                     the ellipse
     * @param sweepFlag true iff the path will sweep clockwise around
     *                  the ellipse
     * @param relx the relative destination relative X coordinate
     * @param rely the relative destination relative Y coordinate
     * @throws IllegalPathStateException
     *     if there is no current point in the path
     * @see Path2D#arcTo
     */
    public void arcToRel(float radiusx, float radiusy, float xAxisRotation,
                         boolean largeArcFlag, boolean sweepFlag,
                         float relx, float rely)
    {
        arcTo(radiusx, radiusy, xAxisRotation,
              largeArcFlag, sweepFlag,
              currX + relx, currY + rely);
    }

    int pointCrossings(float px, float py) {
        float movx, movy, curx, cury, endx, endy;
        float coords[] = floatCoords;
        curx = movx = coords[0];
        cury = movy = coords[1];
        int crossings = 0;
        int ci = 2;
        for (int i = 1; i < numTypes; i++) {
            switch (pointTypes[i]) {
            case PathIterator.SEG_MOVETO:
                if (cury != movy) {
                    crossings +=
                        Shape.pointCrossingsForLine(px, py,
                                                    curx, cury,
                                                    movx, movy);
                }
                movx = curx = coords[ci++];
                movy = cury = coords[ci++];
                break;
            case PathIterator.SEG_LINETO:
                crossings +=
                    Shape.pointCrossingsForLine(px, py,
                                                curx, cury,
                                                endx = coords[ci++],
                                                endy = coords[ci++]);
                curx = endx;
                cury = endy;
                break;
            case PathIterator.SEG_QUADTO:
                crossings +=
                    Shape.pointCrossingsForQuad(px, py,
                                                curx, cury,
                                                coords[ci++],
                                                coords[ci++],
                                                endx = coords[ci++],
                                                endy = coords[ci++],
                                                0);
                curx = endx;
                cury = endy;
                break;
        case PathIterator.SEG_CUBICTO:
                crossings +=
                    Shape.pointCrossingsForCubic(px, py,
                                                 curx, cury,
                                                 coords[ci++],
                                                 coords[ci++],
                                                 coords[ci++],
                                                 coords[ci++],
                                                 endx = coords[ci++],
                                                 endy = coords[ci++],
                                                 0);
                curx = endx;
                cury = endy;
                break;
            case PathIterator.SEG_CLOSE:
                if (cury != movy) {
                    crossings +=
                        Shape.pointCrossingsForLine(px, py,
                                                    curx, cury,
                                                    movx, movy);
                }
                curx = movx;
                cury = movy;
                break;
            }
        }
        if (cury != movy) {
            crossings +=
                Shape.pointCrossingsForLine(px, py,
                                            curx, cury,
                                            movx, movy);
        }
        return crossings;
    }

    int rectCrossings(float rxmin, float rymin,
                      float rxmax, float rymax)
    {
        float coords[] = floatCoords;
        float curx, cury, movx, movy, endx, endy;
        curx = movx = coords[0];
        cury = movy = coords[1];
        int crossings = 0;
        int ci = 2;
        for (int i = 1;
             crossings != Shape.RECT_INTERSECTS && i < numTypes;
             i++)
        {
            switch (pointTypes[i]) {
            case PathIterator.SEG_MOVETO:
                if (curx != movx || cury != movy) {
                    crossings =
                        Shape.rectCrossingsForLine(crossings,
                                                   rxmin, rymin,
                                                   rxmax, rymax,
                                                   curx, cury,
                                                   movx, movy);
                }
                // Count should always be a multiple of 2 here.
                // assert((crossings & 1) != 0);
                movx = curx = coords[ci++];
                movy = cury = coords[ci++];
                break;
            case PathIterator.SEG_LINETO:
                crossings =
                    Shape.rectCrossingsForLine(crossings,
                                               rxmin, rymin,
                                               rxmax, rymax,
                                               curx, cury,
                                               endx = coords[ci++],
                                               endy = coords[ci++]);
                curx = endx;
                cury = endy;
                break;
            case PathIterator.SEG_QUADTO:
                crossings =
                    Shape.rectCrossingsForQuad(crossings,
                                               rxmin, rymin,
                                               rxmax, rymax,
                                               curx, cury,
                                               coords[ci++],
                                               coords[ci++],
                                               endx = coords[ci++],
                                               endy = coords[ci++],
                                               0);
                curx = endx;
                cury = endy;
                break;
            case PathIterator.SEG_CUBICTO:
                crossings =
                    Shape.rectCrossingsForCubic(crossings,
                                                rxmin, rymin,
                                                rxmax, rymax,
                                                curx, cury,
                                                coords[ci++],
                                                coords[ci++],
                                                coords[ci++],
                                                coords[ci++],
                                                endx = coords[ci++],
                                                endy = coords[ci++],
                                                0);
                curx = endx;
                cury = endy;
                break;
            case PathIterator.SEG_CLOSE:
                if (curx != movx || cury != movy) {
                    crossings =
                        Shape.rectCrossingsForLine(crossings,
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
        }
        if (crossings != Shape.RECT_INTERSECTS &&
            (curx != movx || cury != movy))
        {
            crossings =
                Shape.rectCrossingsForLine(crossings,
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
     * {@inheritDoc}
     */
    public final void append(PathIterator pi, boolean connect) {
        float coords[] = new float[6];
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
            case SEG_MOVETO:
                if (!connect || numTypes < 1 || numCoords < 1) {
                    moveTo(coords[0], coords[1]);
                    break;
                }
                if (pointTypes[numTypes - 1] != SEG_CLOSE &&
                    floatCoords[numCoords-2] == coords[0] &&
                    floatCoords[numCoords-1] == coords[1])
                {
                    // Collapse out initial moveto/lineto
                    break;
                }
                // NO BREAK;
            case SEG_LINETO:
                lineTo(coords[0], coords[1]);
                break;
            case SEG_QUADTO:
                quadTo(coords[0], coords[1],
                       coords[2], coords[3]);
                break;
            case SEG_CUBICTO:
                curveTo(coords[0], coords[1],
                        coords[2], coords[3],
                        coords[4], coords[5]);
                break;
            case SEG_CLOSE:
                closePath();
                break;
            }
            pi.next();
            connect = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void transform(BaseTransform tx) {
        if (numCoords == 0) return;
        needRoom(false, 6);
        floatCoords[numCoords + 0] = moveX;
        floatCoords[numCoords + 1] = moveY;
        floatCoords[numCoords + 2] = prevX;
        floatCoords[numCoords + 3] = prevY;
        floatCoords[numCoords + 4] = currX;
        floatCoords[numCoords + 5] = currY;
        tx.transform(floatCoords, 0, floatCoords, 0, numCoords / 2 + 3);
        moveX = floatCoords[numCoords + 0];
        moveY = floatCoords[numCoords + 1];
        prevX = floatCoords[numCoords + 2];
        prevY = floatCoords[numCoords + 3];
        currX = floatCoords[numCoords + 4];
        currY = floatCoords[numCoords + 5];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final RectBounds getBounds() {
        float x1, y1, x2, y2;
        int i = numCoords;
        if (i > 0) {
            y1 = y2 = floatCoords[--i];
            x1 = x2 = floatCoords[--i];
            while (i > 0) {
                float y = floatCoords[--i];
                float x = floatCoords[--i];
                if (x < x1) x1 = x;
                if (y < y1) y1 = y;
                if (x > x2) x2 = x;
                if (y > y2) y2 = y;
            }
        } else {
            x1 = y1 = x2 = y2 = 0.0f;
        }
        return new RectBounds(x1, y1, x2, y2);
    }

    // The following three methods are used only by Prism to access
    // internal structures; not intended for general use!
    public final int getNumCommands() {
        return numTypes;
    }
    public final byte[] getCommandsNoClone() {
        return pointTypes;
    }
    public final float[] getFloatCoordsNoClone() {
        return floatCoords;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The iterator for this class is not multi-threaded safe,
     * which means that the {@code Path2D} class does not
     * guarantee that modifications to the geometry of this
     * {@code Path2D} object do not affect any iterations of
     * that geometry that are already in process.
     */
    @Override
    public PathIterator getPathIterator(BaseTransform tx) {
        if (tx == null) {
            return new CopyIterator(this);
        } else {
            return new TxIterator(this, tx);
        }
    }

    static class CopyIterator extends Path2D.Iterator {
        float floatCoords[];

        CopyIterator(Path2D p2df) {
            super(p2df);
            this.floatCoords = p2df.floatCoords;
        }

        @Override
        public int currentSegment(float[] coords) {
            int type = path.pointTypes[typeIdx];
            int numCoords = curvecoords[type];
            if (numCoords > 0) {
                System.arraycopy(floatCoords, pointIdx,
                                 coords, 0, numCoords);
            }
            return type;
        }

        public int currentSegment(double[] coords) {
            int type = path.pointTypes[typeIdx];
            int numCoords = curvecoords[type];
            if (numCoords > 0) {
                for (int i = 0; i < numCoords; i++) {
                    coords[i] = floatCoords[pointIdx + i];
                }
            }
            return type;
        }
    }

    static class TxIterator extends Path2D.Iterator {
        float floatCoords[];
        BaseTransform transform;

        TxIterator(Path2D p2df, BaseTransform tx) {
            super(p2df);
            this.floatCoords = p2df.floatCoords;
            this.transform = tx;
        }

        @Override
        public int currentSegment(float[] coords) {
            int type = path.pointTypes[typeIdx];
            int numCoords = curvecoords[type];
            if (numCoords > 0) {
                transform.transform(floatCoords, pointIdx,
                                 coords, 0, numCoords / 2);
            }
            return type;
        }

        public int currentSegment(double[] coords) {
            int type = path.pointTypes[typeIdx];
            int numCoords = curvecoords[type];
            if (numCoords > 0) {
                transform.transform(floatCoords, pointIdx,
                                 coords, 0, numCoords / 2);
            }
            return type;
        }
    }

    /**
     * Closes the current subpath by drawing a straight line back to
     * the coordinates of the last {@code moveTo}.  If the path is already
     * closed then this method has no effect.
     */
    @Override
    public final void closePath() {
        if (numTypes == 0 || pointTypes[numTypes - 1] != SEG_CLOSE) {
            needRoom(true, 0);
            pointTypes[numTypes++] = SEG_CLOSE;
            prevX = currX = moveX;
            prevY = currY = moveY;
        }
    }

    @Override
    public void pathDone() {
    }

    /**
     * Appends the geometry of the specified {@code Shape} object to the
     * path, possibly connecting the new geometry to the existing path
     * segments with a line segment.
     * If the {@code connect} parameter is {@code true} and the
     * path is not empty then any initial {@code moveTo} in the
     * geometry of the appended {@code Shape}
     * is turned into a {@code lineTo} segment.
     * If the destination coordinates of such a connecting {@code lineTo}
     * segment match the ending coordinates of a currently open
     * subpath then the segment is omitted as superfluous.
     * The winding rule of the specified {@code Shape} is ignored
     * and the appended geometry is governed by the winding
     * rule specified for this path.
     *
     * @param s the {@code Shape} whose geometry is appended
     *          to this path
     * @param connect a boolean to control whether or not to turn an initial
     *                {@code moveTo} segment into a {@code lineTo} segment
     *                to connect the new geometry to the existing path
     */
    public final void append(Shape s, boolean connect) {
        append(s.getPathIterator(null), connect);
    }

    static class SVGParser {
        final String svgpath;
        final int len;
        int pos;
        boolean allowcomma;

        public SVGParser(String svgpath) {
            this.svgpath = svgpath;
            this.len = svgpath.length();
        }

        public boolean isDone() {
            return (toNextNonWsp() >= len);
        }

        public char getChar() {
            return svgpath.charAt(pos++);
        }

        public boolean nextIsNumber() {
            if (toNextNonWsp() < len) {
                switch (svgpath.charAt(pos)) {
                    case '-':
                    case '+':
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                    case '.':
                        return true;
                }
            }
            return false;
        }

        public float f() {
            return getFloat();
        }

        public float a() {
            return (float) Math.toRadians(getFloat());
        }

        public float getFloat() {
            int start = toNextNonWsp();
            this.allowcomma = true;
            int end = toNumberEnd();
            if (start < end) {
                String flstr = svgpath.substring(start, end);
                try {
                    return Float.parseFloat(flstr);
                } catch (NumberFormatException e) {
                }
                throw new IllegalArgumentException("invalid float ("+flstr+
                                                   ") in path at pos="+start);
            }
            throw new IllegalArgumentException("end of path looking for float");
        }

        public boolean b() {
            toNextNonWsp();
            this.allowcomma = true;
            if (pos < len) {
                char flag = svgpath.charAt(pos);
                switch (flag) {
                    case '0': pos++; return false;
                    case '1': pos++; return true;
                }
                throw new IllegalArgumentException("invalid boolean flag ("+flag+
                                                   ") in path at pos="+pos);
            }
            throw new IllegalArgumentException("end of path looking for boolean");
        }

        private int toNextNonWsp() {
            boolean canbecomma = this.allowcomma;
            while (pos < len) {
                switch (svgpath.charAt(pos)) {
                    case ',':
                        if (!canbecomma) {
                            return pos;
                        }
                        canbecomma = false;
                        break;
                    case ' ':
                    case '\t':
                    case '\r':
                    case '\n':
                        break;
                    default:
                        return pos;
                }
                pos++;
            }
            return pos;
        }

        private int toNumberEnd() {
            boolean allowsign = true;
            boolean hasexp = false;
            boolean hasdecimal = false;
            while (pos < len) {
                switch (svgpath.charAt(pos)) {
                    case '-':
                    case '+':
                        if (!allowsign) return pos;
                        allowsign = false;
                        break;
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        allowsign = false;
                        break;
                    case 'E': case 'e':
                        if (hasexp) return pos;
                        hasexp = allowsign = true;
                        break;
                    case '.':
                        if (hasexp || hasdecimal) return pos;
                        hasdecimal = true;
                        allowsign = false;
                        break;
                    default:
                        return pos;
                }
                pos++;
            }
            return pos;
        }
    }

    /**
     * Appends the geometry of the path in the specified {@code String}
     * argument in the format of an SVG path.
     * The specification of the grammar of the language for an SVG path
     * is specified on the W3C web page:
     * <pre>
     * http://www.w3.org/TR/SVG/paths.html#PathDataBNF
     * </pre>
     * and the interpretation of the various elements in the format is
     * specified on the W3C web page:
     * <pre>
     * http://www.w3.org/TR/SVG/paths.html#PathData
     * </pre>
     *
     * @param svgpath the {@code String} object containing the SVG style
     *                definition of the geometry to be apppended
     * @throws IllegalArgumentException
     *     if {@code svgpath} does not match the indicated SVG path grammar
     * @throws IllegalPathStateException
     *     if there is no current point in the path
     */
    public final void appendSVGPath(String svgpath) {
        SVGParser p = new SVGParser(svgpath);
        p.allowcomma = false;
        while (!p.isDone()) {
            p.allowcomma = false;
            char cmd = p.getChar();
            switch (cmd) {
                case 'M':
                    moveTo(p.f(), p.f());
                    while (p.nextIsNumber()) {
                        lineTo(p.f(), p.f());
                    }
                    break;
                case 'm':
                    if (numTypes > 0) {
                        moveToRel(p.f(), p.f());
                    } else {
                        moveTo(p.f(), p.f());
                    }
                    while (p.nextIsNumber()) {
                        lineToRel(p.f(), p.f());
                    }
                    break;
                case 'L':
                    do {
                        lineTo(p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'l':
                    do {
                        lineToRel(p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'H':
                    do {
                        lineTo(p.f(), currY);
                    } while (p.nextIsNumber());
                    break;
                case 'h':
                    do {
                        lineToRel(p.f(), 0);
                    } while (p.nextIsNumber());
                    break;
                case 'V':
                    do {
                        lineTo(currX, p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'v':
                    do {
                        lineToRel(0, p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'Q':
                    do {
                        quadTo(p.f(), p.f(), p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'q':
                    do {
                        quadToRel(p.f(), p.f(), p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'T':
                    do {
                        quadToSmooth(p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 't':
                    do {
                        quadToSmoothRel(p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'C':
                    do {
                        curveTo(p.f(), p.f(), p.f(), p.f(), p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'c':
                    do {
                        curveToRel(p.f(), p.f(), p.f(), p.f(), p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'S':
                    do {
                        curveToSmooth(p.f(), p.f(), p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 's':
                    do {
                        curveToSmoothRel(p.f(), p.f(), p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'A':
                    do {
                        arcTo(p.f(), p.f(), p.a(), p.b(), p.b(), p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'a':
                    do {
                        arcToRel(p.f(), p.f(), p.a(), p.b(), p.b(), p.f(), p.f());
                    } while (p.nextIsNumber());
                    break;
                case 'Z': case 'z': closePath(); break;
                default:
                    throw new IllegalArgumentException("invalid command ("+cmd+
                                                       ") in SVG path at pos="+p.pos);
            }
            p.allowcomma = false;
        }
    }

    /**
     * Returns the fill style winding rule.
     *
     * @return an integer representing the current winding rule.
     * @see #WIND_EVEN_ODD
     * @see #WIND_NON_ZERO
     * @see #setWindingRule
     */
    public final int getWindingRule() {
        return windingRule;
    }

    /**
     * Sets the winding rule for this path to the specified value.
     *
     * @param rule an integer representing the specified
     *             winding rule
     * @exception IllegalArgumentException if
     *      {@code rule} is not either
     *      {@link #WIND_EVEN_ODD} or
     *      {@link #WIND_NON_ZERO}
     * @see #getWindingRule
     */
    public final void setWindingRule(int rule) {
        if (rule != WIND_EVEN_ODD && rule != WIND_NON_ZERO) {
            throw new IllegalArgumentException("winding rule must be "+
                               "WIND_EVEN_ODD or "+
                               "WIND_NON_ZERO");
        }
        windingRule = rule;
    }

    /**
     * Returns the coordinates most recently added to the end of the path
     * as a {@link Point2D} object.
     *
     * @return a {@code Point2D} object containing the ending coordinates of
     *         the path or {@code null} if there are no points in the path.
     */
    public final Point2D getCurrentPoint() {
        if (numTypes < 1) {
            return null;
        }
        return new Point2D(currX, currY);
    }

    public final float getCurrentX() {
        if (numTypes < 1) {
            throw new IllegalPathStateException("no current point in empty path");
        }
        return currX;
    }

    public final float getCurrentY() {
        if (numTypes < 1) {
            throw new IllegalPathStateException("no current point in empty path");
        }
        return currY;
    }

    /**
     * Resets the path to empty.  The append position is set back to the
     * beginning of the path and all coordinates and point types are
     * forgotten.
     */
    public final void reset() {
        numTypes = numCoords = 0;
        moveX = moveY = prevX = prevY = currX = currY = 0;
    }

    /**
     * Returns a new {@code Shape} representing a transformed version
     * of this {@code Path2D}.
     * Note that the exact type and coordinate precision of the return
     * value is not specified for this method.
     * The method will return a Shape that contains no less precision
     * for the transformed geometry than this {@code Path2D} currently
     * maintains, but it may contain no more precision either.
     * If the tradeoff of precision vs. storage size in the result is
     * important then the convenience constructors in the
     * {@link Path2D(Shape, BaseTransform) Path2D}
     *
     * @param tx the {@code BaseTransform} used to transform a
     *           new {@code Shape}.
     * @return a new {@code Shape}, transformed with the specified
     *         {@code BaseTransform}.
     */
    public final Shape createTransformedShape(BaseTransform tx) {
        return new Path2D(this, tx);
    }

    @Override
    public Path2D copy() {
        return new Path2D(this);
    }

    /**
     * {@inheritDoc}
     *
     * Note that this method may return false when the geometry of the
     * given {@code Path2D} is identical to the geometry of this object
     * but is expressed in a different way.  This method will only return
     * true when the internal representation of this object is exactly the
     * same as that of the given object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Path2D) {
            Path2D p = (Path2D)obj;
            if (p.numTypes == this.numTypes &&
                p.numCoords == this.numCoords &&
                p.windingRule == this.windingRule)
            {
                for (int i = 0; i < numTypes; i++) {
                    if (p.pointTypes[i] != this.pointTypes[i]) {
                        return false;
                    }
                }
                for (int i = 0; i < numCoords; i++) {
                    if (p.floatCoords[i] != this.floatCoords[i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + numTypes;
        hash = 11 * hash + numCoords;
        hash = 11 * hash + windingRule;
        for (int i = 0; i < numTypes; i++) {
            hash = 11 * hash + pointTypes[i];
        }
        for (int i = 0; i < numCoords; i++) {
            hash = 11 * hash + Float.floatToIntBits(floatCoords[i]);
        }
        return hash;
    }

    /**
     * Tests if the specified coordinates are inside the closed
     * boundary of the specified {@link PathIterator}.
     * <p>
     * This method provides a basic facility for implementors of
     * the {@link Shape} interface to implement support for the
     * {@link Shape#contains(double, double)} method.
     *
     * @param pi the specified {@code PathIterator}
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     * @return {@code true} if the specified coordinates are inside the
     *         specified {@code PathIterator}; {@code false} otherwise
     */
    public static boolean contains(PathIterator pi, float x, float y) {
        if (x * 0f + y * 0f == 0f) {
            /* N * 0.0 is 0.0 only if N is finite.
             * Here we know that both x and y are finite.
             */
            int mask = (pi.getWindingRule() == WIND_NON_ZERO ? -1 : 1);
            int cross = Shape.pointCrossingsForPath(pi, x, y);
            return ((cross & mask) != 0);
        } else {
            /* Either x or y was infinite or NaN.
             * A NaN always produces a negative response to any test
             * and Infinity values cannot be "inside" any path so
             * they should return false as well.
             */
            return false;
        }
    }

    /**
     * Tests if the specified {@link Point2D} is inside the closed
     * boundary of the specified {@link PathIterator}.
     * <p>
     * This method provides a basic facility for implementors of
     * the {@link Shape} interface to implement support for the
     * {@link Shape#contains(Point2D)} method.
     *
     * @param pi the specified {@code PathIterator}
     * @param p the specified {@code Point2D}
     * @return {@code true} if the specified coordinates are inside the
     *         specified {@code PathIterator}; {@code false} otherwise
     */
    public static boolean contains(PathIterator pi, Point2D p) {
        return contains(pi, p.x, p.y);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean contains(float x, float y) {
        if (x * 0f + y * 0f == 0f) {
            /* N * 0.0 is 0.0 only if N is finite.
             * Here we know that both x and y are finite.
             */
            if (numTypes < 2) {
                return false;
            }
            int mask = (windingRule == WIND_NON_ZERO ? -1 : 1);
            return ((pointCrossings(x, y) & mask) != 0);
        } else {
            /* Either x or y was infinite or NaN.
             * A NaN always produces a negative response to any test
             * and Infinity values cannot be "inside" any path so
             * they should return false as well.
             */
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean contains(Point2D p) {
        return contains(p.x, p.y);
    }

    /**
     * Tests if the specified rectangular area is entirely inside the
     * closed boundary of the specified {@link PathIterator}.
     * <p>
     * This method provides a basic facility for implementors of
     * the {@link Shape} interface to implement support for the
     * {@link Shape#contains(double, double, double, double)} method.
     * <p>
     * This method object may conservatively return false in
     * cases where the specified rectangular area intersects a
     * segment of the path, but that segment does not represent a
     * boundary between the interior and exterior of the path.
     * Such segments could lie entirely within the interior of the
     * path if they are part of a path with a {@link #WIND_NON_ZERO}
     * winding rule or if the segments are retraced in the reverse
     * direction such that the two sets of segments cancel each
     * other out without any exterior area falling between them.
     * To determine whether segments represent true boundaries of
     * the interior of the path would require extensive calculations
     * involving all of the segments of the path and the winding
     * rule and are thus beyond the scope of this implementation.
     *
     * @param pi the specified {@code PathIterator}
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     * @param w the width of the specified rectangular area
     * @param h the height of the specified rectangular area
     * @return {@code true} if the specified {@code PathIterator} contains
     *         the specified rectangluar area; {@code false} otherwise.
     */
    public static boolean contains(PathIterator pi,
                                   float x, float y, float w, float h)
    {
        if (java.lang.Float.isNaN(x+w) || java.lang.Float.isNaN(y+h)) {
            /* [xy]+[wh] is NaN if any of those values are NaN,
             * or if adding the two together would produce NaN
             * by virtue of adding opposing Infinte values.
             * Since we need to add them below, their sum must
             * not be NaN.
             * We return false because NaN always produces a
             * negative response to tests
             */
            return false;
        }
        if (w <= 0 || h <= 0) {
            return false;
        }
        int mask = (pi.getWindingRule() == WIND_NON_ZERO ? -1 : 2);
        int crossings = Shape.rectCrossingsForPath(pi, x, y, x+w, y+h);
        return (crossings != Shape.RECT_INTERSECTS &&
                    (crossings & mask) != 0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method object may conservatively return false in
     * cases where the specified rectangular area intersects a
     * segment of the path, but that segment does not represent a
     * boundary between the interior and exterior of the path.
     * Such segments could lie entirely within the interior of the
     * path if they are part of a path with a {@link #WIND_NON_ZERO}
     * winding rule or if the segments are retraced in the reverse
     * direction such that the two sets of segments cancel each
     * other out without any exterior area falling between them.
     * To determine whether segments represent true boundaries of
     * the interior of the path would require extensive calculations
     * involving all of the segments of the path and the winding
     * rule and are thus beyond the scope of this implementation.
     */
    @Override
    public final boolean contains(float x, float y, float w, float h) {
        if (java.lang.Float.isNaN(x+w) || java.lang.Float.isNaN(y+h)) {
            /* [xy]+[wh] is NaN if any of those values are NaN,
             * or if adding the two together would produce NaN
             * by virtue of adding opposing Infinte values.
             * Since we need to add them below, their sum must
             * not be NaN.
             * We return false because NaN always produces a
             * negative response to tests
             */
            return false;
        }
        if (w <= 0 || h <= 0) {
            return false;
        }
        int mask = (windingRule == WIND_NON_ZERO ? -1 : 2);
        int crossings = rectCrossings(x, y, x+w, y+h);
        return (crossings != Shape.RECT_INTERSECTS &&
                    (crossings & mask) != 0);
    }

    /**
     * Tests if the interior of the specified {@link PathIterator}
     * intersects the interior of a specified set of rectangular
     * coordinates.
     * <p>
     * This method provides a basic facility for implementors of
     * the {@link Shape} interface to implement support for the
     * {@link Shape#intersects(double, double, double, double)} method.
     * <p>
     * This method object may conservatively return true in
     * cases where the specified rectangular area intersects a
     * segment of the path, but that segment does not represent a
     * boundary between the interior and exterior of the path.
     * Such a case may occur if some set of segments of the
     * path are retraced in the reverse direction such that the
     * two sets of segments cancel each other out without any
     * interior area between them.
     * To determine whether segments represent true boundaries of
     * the interior of the path would require extensive calculations
     * involving all of the segments of the path and the winding
     * rule and are thus beyond the scope of this implementation.
     *
     * @param pi the specified {@code PathIterator}
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     * @param w the width of the specified rectangular coordinates
     * @param h the height of the specified rectangular coordinates
     * @return {@code true} if the specified {@code PathIterator} and
     *         the interior of the specified set of rectangular
     *         coordinates intersect each other; {@code false} otherwise.
     */
    public static boolean intersects(PathIterator pi,
                                     float x, float y, float w, float h)
    {
        if (java.lang.Float.isNaN(x+w) || java.lang.Float.isNaN(y+h)) {
            /* [xy]+[wh] is NaN if any of those values are NaN,
             * or if adding the two together would produce NaN
             * by virtue of adding opposing Infinte values.
             * Since we need to add them below, their sum must
             * not be NaN.
             * We return false because NaN always produces a
             * negative response to tests
             */
            return false;
        }
        if (w <= 0 || h <= 0) {
            return false;
        }
        int mask = (pi.getWindingRule() == WIND_NON_ZERO ? -1 : 2);
        int crossings = Shape.rectCrossingsForPath(pi, x, y, x+w, y+h);
        return (crossings == Shape.RECT_INTERSECTS ||
                    (crossings & mask) != 0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method object may conservatively return true in
     * cases where the specified rectangular area intersects a
     * segment of the path, but that segment does not represent a
     * boundary between the interior and exterior of the path.
     * Such a case may occur if some set of segments of the
     * path are retraced in the reverse direction such that the
     * two sets of segments cancel each other out without any
     * interior area between them.
     * To determine whether segments represent true boundaries of
     * the interior of the path would require extensive calculations
     * involving all of the segments of the path and the winding
     * rule and are thus beyond the scope of this implementation.
     */
    @Override
    public final boolean intersects(float x, float y, float w, float h) {
        if (java.lang.Float.isNaN(x+w) || java.lang.Float.isNaN(y+h)) {
            /* [xy]+[wh] is NaN if any of those values are NaN,
             * or if adding the two together would produce NaN
             * by virtue of adding opposing Infinte values.
             * Since we need to add them below, their sum must
             * not be NaN.
             * We return false because NaN always produces a
             * negative response to tests
             */
            return false;
        }
        if (w <= 0 || h <= 0) {
            return false;
        }
        int mask = (windingRule == WIND_NON_ZERO ? -1 : 2);
        int crossings = rectCrossings(x, y, x+w, y+h);
        return (crossings == Shape.RECT_INTERSECTS ||
                    (crossings & mask) != 0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The iterator for this class is not multi-threaded safe,
     * which means that this {@code Path2D} class does not
     * guarantee that modifications to the geometry of this
     * {@code Path2D} object do not affect any iterations of
     * that geometry that are already in process.
     */
    @Override
    public PathIterator getPathIterator(BaseTransform tx,
                                        float flatness)
    {
        return new FlatteningPathIterator(getPathIterator(tx), flatness);
    }

    static abstract class Iterator implements PathIterator {
        int typeIdx;
        int pointIdx;
        Path2D path;

        Iterator(Path2D path) {
            this.path = path;
        }

        @Override
        public int getWindingRule() {
            return path.getWindingRule();
        }

        @Override
        public boolean isDone() {
            return (typeIdx >= path.numTypes);
        }

        @Override
        public void next() {
            int type = path.pointTypes[typeIdx++];
            pointIdx += curvecoords[type];
        }
    }

    public void setTo(Path2D otherPath) {
        numTypes = otherPath.numTypes;
        numCoords = otherPath.numCoords;
        if (numTypes > pointTypes.length) {
            pointTypes = new byte[numTypes];
        }
        System.arraycopy(otherPath.pointTypes, 0, pointTypes, 0, numTypes);
        if (numCoords > floatCoords.length) {
            floatCoords = new float[numCoords];
        }
        System.arraycopy(otherPath.floatCoords, 0, floatCoords, 0, numCoords);
        windingRule = otherPath.windingRule;
        moveX = otherPath.moveX;
        moveY = otherPath.moveY;
        prevX = otherPath.prevX;
        prevY = otherPath.prevY;
        currX = otherPath.currX;
        currY = otherPath.currY;
    }
}
