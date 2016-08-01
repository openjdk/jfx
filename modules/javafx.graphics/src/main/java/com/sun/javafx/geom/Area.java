/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;
import com.sun.javafx.geom.transform.BaseTransform;


/**
 * An <code>Area</code> object stores and manipulates a
 * resolution-independent description of an enclosed area of
 * 2-dimensional space.
 * <code>Area</code> objects can be transformed and can perform
 * various Constructive Area Geometry (CAG) operations when combined
 * with other <code>Area</code> objects.
 * The CAG operations include area
 * {@link #add addition}, {@link #subtract subtraction},
 * {@link #intersect intersection}, and {@link #exclusiveOr exclusive or}.
 * See the linked method documentation for examples of the various
 * operations.
 * <p>
 * The <code>Area</code> class implements the <code>Shape</code>
 * interface and provides full support for all of its hit-testing
 * and path iteration facilities, but an <code>Area</code> is more
 * specific than a generalized path in a number of ways:
 * <ul>
 * <li>Only closed paths and sub-paths are stored.
 *     <code>Area</code> objects constructed from unclosed paths
 *     are implicitly closed during construction as if those paths
 *     had been filled by the <code>Graphics2D.fill</code> method.
 * <li>The interiors of the individual stored sub-paths are all
 *     non-empty and non-overlapping.  Paths are decomposed during
 *     construction into separate component non-overlapping parts,
 *     empty pieces of the path are discarded, and then these
 *     non-empty and non-overlapping properties are maintained
 *     through all subsequent CAG operations.  Outlines of different
 *     component sub-paths may touch each other, as long as they
 *     do not cross so that their enclosed areas overlap.
 * <li>The geometry of the path describing the outline of the
 *     <code>Area</code> resembles the path from which it was
 *     constructed only in that it describes the same enclosed
 *     2-dimensional area, but may use entirely different types
 *     and ordering of the path segments to do so.
 * </ul>
 * Interesting issues which are not always obvious when using
 * the <code>Area</code> include:
 * <ul>
 * <li>Creating an <code>Area</code> from an unclosed (open)
 *     <code>Shape</code> results in a closed outline in the
 *     <code>Area</code> object.
 * <li>Creating an <code>Area</code> from a <code>Shape</code>
 *     which encloses no area (even when "closed") produces an
 *     empty <code>Area</code>.  A common example of this issue
 *     is that producing an <code>Area</code> from a line will
 *     be empty since the line encloses no area.  An empty
 *     <code>Area</code> will iterate no geometry in its
 *     <code>PathIterator</code> objects.
 * <li>A self-intersecting <code>Shape</code> may be split into
 *     two (or more) sub-paths each enclosing one of the
 *     non-intersecting portions of the original path.
 * <li>An <code>Area</code> may take more path segments to
 *     describe the same geometry even when the original
 *     outline is simple and obvious.  The analysis that the
 *     <code>Area</code> class must perform on the path may
 *     not reflect the same concepts of "simple and obvious"
 *     as a human being perceives.
 * </ul>
 */
public class Area extends Shape {

    private static final Vector EmptyCurves = new Vector();

    private Vector curves;

    /**
     * Default constructor which creates an empty area.
     */
    public Area() {
        curves = EmptyCurves;
    }

    /**
     * The <code>Area</code> class creates an area geometry from the
     * specified {@link Shape} object.  The geometry is explicitly
     * closed, if the <code>Shape</code> is not already closed.  The
     * fill rule (even-odd or winding) specified by the geometry of the
     * <code>Shape</code> is used to determine the resulting enclosed area.
     * @param s  the <code>Shape</code> from which the area is constructed
     * @throws NullPointerException if <code>s</code> is null
     */
    public Area(Shape s) {
        if (s instanceof Area) {
            curves = ((Area) s).curves;
        } else {
            curves = pathToCurves(s.getPathIterator(null));
        }
    }

    public Area(PathIterator iter) {
        curves = pathToCurves(iter);
    }

    private static Vector pathToCurves(PathIterator pi) {
        Vector curves = new Vector();
        int windingRule = pi.getWindingRule();
        // coords array is big enough for holding:
        //     coordinates returned from currentSegment (6)
        //     OR
        //         two subdivided quadratic curves (2+4+4=10)
        //         AND
        //             0-1 horizontal splitting parameters
        //             OR
        //             2 parametric equation derivative coefficients
        //     OR
        //         three subdivided cubic curves (2+6+6+6=20)
        //         AND
        //             0-2 horizontal splitting parameters
        //             OR
        //             3 parametric equation derivative coefficients
        float coords[] = new float[6];
        double tmp[] = new double[23];
        double movx = 0, movy = 0;
        double curx = 0, cury = 0;
        double newx, newy;
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO:
                Curve.insertLine(curves, curx, cury, movx, movy);
                curx = movx = coords[0];
                cury = movy = coords[1];
                Curve.insertMove(curves, movx, movy);
                break;
            case PathIterator.SEG_LINETO:
                newx = coords[0];
                newy = coords[1];
                Curve.insertLine(curves, curx, cury, newx, newy);
                curx = newx;
                cury = newy;
                break;
            case PathIterator.SEG_QUADTO:
                newx = coords[2];
                newy = coords[3];
                Curve.insertQuad(curves, tmp,
                                 curx, cury,
                                 coords[0], coords[1],
                                 coords[2], coords[3]);
                curx = newx;
                cury = newy;
                break;
            case PathIterator.SEG_CUBICTO:
                newx = coords[4];
                newy = coords[5];
                Curve.insertCubic(curves, tmp,
                                  curx, cury,
                                  coords[0], coords[1],
                                  coords[2], coords[3],
                                  coords[4], coords[5]);
                curx = newx;
                cury = newy;
                break;
            case PathIterator.SEG_CLOSE:
                Curve.insertLine(curves, curx, cury, movx, movy);
                curx = movx;
                cury = movy;
                break;
            }
            pi.next();
        }
        Curve.insertLine(curves, curx, cury, movx, movy);
        AreaOp operator;
        if (windingRule == PathIterator.WIND_EVEN_ODD) {
            operator = new AreaOp.EOWindOp();
        } else {
            operator = new AreaOp.NZWindOp();
        }
        return operator.calculate(curves, EmptyCurves);
    }

    /**
     * Adds the shape of the specified <code>Area</code> to the
     * shape of this <code>Area</code>.
     * The resulting shape of this <code>Area</code> will include
     * the union of both shapes, or all areas that were contained
     * in either this or the specified <code>Area</code>.
     * <pre>
     *     // Example:
     *     Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
     *     Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
     *     a1.add(a2);
     *
     *        a1(before)     +         a2         =     a1(after)
     *
     *     ################     ################     ################
     *     ##############         ##############     ################
     *     ############             ############     ################
     *     ##########                 ##########     ################
     *     ########                     ########     ################
     *     ######                         ######     ######    ######
     *     ####                             ####     ####        ####
     *     ##                                 ##     ##            ##
     * </pre>
     * @param   rhs  the <code>Area</code> to be added to the
     *          current shape
     * @throws NullPointerException if <code>rhs</code> is null
     */
    public void add(Area rhs) {
        curves = new AreaOp.AddOp().calculate(this.curves, rhs.curves);
        invalidateBounds();
    }

    /**
     * Subtracts the shape of the specified <code>Area</code> from the
     * shape of this <code>Area</code>.
     * The resulting shape of this <code>Area</code> will include
     * areas that were contained only in this <code>Area</code>
     * and not in the specified <code>Area</code>.
     * <pre>
     *     // Example:
     *     Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
     *     Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
     *     a1.subtract(a2);
     *
     *        a1(before)     -         a2         =     a1(after)
     *
     *     ################     ################
     *     ##############         ##############     ##
     *     ############             ############     ####
     *     ##########                 ##########     ######
     *     ########                     ########     ########
     *     ######                         ######     ######
     *     ####                             ####     ####
     *     ##                                 ##     ##
     * </pre>
     * @param   rhs  the <code>Area</code> to be subtracted from the
     *          current shape
     * @throws NullPointerException if <code>rhs</code> is null
     */
    public void subtract(Area rhs) {
        curves = new AreaOp.SubOp().calculate(this.curves, rhs.curves);
        invalidateBounds();
    }

    /**
     * Sets the shape of this <code>Area</code> to the intersection of
     * its current shape and the shape of the specified <code>Area</code>.
     * The resulting shape of this <code>Area</code> will include
     * only areas that were contained in both this <code>Area</code>
     * and also in the specified <code>Area</code>.
     * <pre>
     *     // Example:
     *     Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
     *     Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
     *     a1.intersect(a2);
     *
     *      a1(before)   intersect     a2         =     a1(after)
     *
     *     ################     ################     ################
     *     ##############         ##############       ############
     *     ############             ############         ########
     *     ##########                 ##########           ####
     *     ########                     ########
     *     ######                         ######
     *     ####                             ####
     *     ##                                 ##
     * </pre>
     * @param   rhs  the <code>Area</code> to be intersected with this
     *          <code>Area</code>
     * @throws NullPointerException if <code>rhs</code> is null
     */
    public void intersect(Area rhs) {
        curves = new AreaOp.IntOp().calculate(this.curves, rhs.curves);
        invalidateBounds();
    }

    /**
     * Sets the shape of this <code>Area</code> to be the combined area
     * of its current shape and the shape of the specified <code>Area</code>,
     * minus their intersection.
     * The resulting shape of this <code>Area</code> will include
     * only areas that were contained in either this <code>Area</code>
     * or in the specified <code>Area</code>, but not in both.
     * <pre>
     *     // Example:
     *     Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
     *     Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
     *     a1.exclusiveOr(a2);
     *
     *        a1(before)    xor        a2         =     a1(after)
     *
     *     ################     ################
     *     ##############         ##############     ##            ##
     *     ############             ############     ####        ####
     *     ##########                 ##########     ######    ######
     *     ########                     ########     ################
     *     ######                         ######     ######    ######
     *     ####                             ####     ####        ####
     *     ##                                 ##     ##            ##
     * </pre>
     * @param   rhs  the <code>Area</code> to be exclusive ORed with this
     *          <code>Area</code>.
     * @throws NullPointerException if <code>rhs</code> is null
     */
    public void exclusiveOr(Area rhs) {
        curves = new AreaOp.XorOp().calculate(this.curves, rhs.curves);
        invalidateBounds();
    }

    /**
     * Removes all of the geometry from this <code>Area</code> and
     * restores it to an empty area.
     */
    public void reset() {
        curves = new Vector();
        invalidateBounds();
    }

    /**
     * Tests whether this <code>Area</code> object encloses any area.
     * @return    <code>true</code> if this <code>Area</code> object
     * represents an empty area; <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return (curves.size() == 0);
    }

    /**
     * Tests whether this <code>Area</code> consists entirely of
     * straight edged polygonal geometry.
     * @return    <code>true</code> if the geometry of this
     * <code>Area</code> consists entirely of line segments;
     * <code>false</code> otherwise.
     */
    public boolean isPolygonal() {
        Enumeration enum_ = curves.elements();
        while (enum_.hasMoreElements()) {
            if (((Curve) enum_.nextElement()).getOrder() > 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether this <code>Area</code> is rectangular in shape.
     * @return    <code>true</code> if the geometry of this
     * <code>Area</code> is rectangular in shape; <code>false</code>
     * otherwise.
     */
    public boolean isRectangular() {
        int size = curves.size();
        if (size == 0) {
            return true;
        }
        if (size > 3) {
            return false;
        }
        Curve c1 = (Curve) curves.get(1);
        Curve c2 = (Curve) curves.get(2);
        if (c1.getOrder() != 1 || c2.getOrder() != 1) {
            return false;
        }
        if (c1.getXTop() != c1.getXBot() || c2.getXTop() != c2.getXBot()) {
            return false;
        }
        if (c1.getYTop() != c2.getYTop() || c1.getYBot() != c2.getYBot()) {
            // One might be able to prove that this is impossible...
            return false;
        }
        return true;
    }

    /**
     * Tests whether this <code>Area</code> is comprised of a single
     * closed subpath.  This method returns <code>true</code> if the
     * path contains 0 or 1 subpaths, or <code>false</code> if the path
     * contains more than 1 subpath.  The subpaths are counted by the
     * number of {@link PathIterator#SEG_MOVETO SEG_MOVETO}  segments
     * that appear in the path.
     * @return    <code>true</code> if the <code>Area</code> is comprised
     * of a single basic geometry; <code>false</code> otherwise.
     */
    public boolean isSingular() {
        if (curves.size() < 3) {
            return true;
        }
        Enumeration enum_ = curves.elements();
        enum_.nextElement(); // First Order0 "moveto"
        while (enum_.hasMoreElements()) {
            if (((Curve) enum_.nextElement()).getOrder() == 0) {
                return false;
            }
        }
        return true;
    }

    private RectBounds cachedBounds;
    private void invalidateBounds() {
        cachedBounds = null;
    }
    private RectBounds getCachedBounds() {
        if (cachedBounds != null) {
            return cachedBounds;
        }
        RectBounds r = new RectBounds();
        if (curves.size() > 0) {
            Curve c = (Curve) curves.get(0);
            // First point is always an order 0 curve (moveto)
            r.setBounds((float) c.getX0(), (float) c.getY0(), 0, 0);
            for (int i = 1; i < curves.size(); i++) {
                ((Curve) curves.get(i)).enlarge(r);
            }
        }
        return (cachedBounds = r);
    }

    /**
     * Returns a high precision bounding {@link RectBounds} that
     * completely encloses this <code>Area</code>.
     * <p>
     * The Area class will attempt to return the tightest bounding
     * box possible for the Shape.  The bounding box will not be
     * padded to include the control points of curves in the outline
     * of the Shape, but should tightly fit the actual geometry of
     * the outline itself.
     * @return    the bounding <code>RectBounds</code> for the
     * <code>Area</code>.
     */
    public RectBounds getBounds() {
        return new RectBounds(getCachedBounds());
    }

    /**
     * Tests whether the geometries of the two <code>Area</code> objects
     * cover the same area.
     * This method will return false if the argument is null.
     * @param   other  the <code>Area</code> to be compared to this
     *          <code>Area</code>
     * @return  <code>true</code> if the two geometries are equivalent;
     *          <code>false</code> otherwise.
     */
    public boolean isEquivalent(Area other) {
        // REMIND: A *much* simpler operation should be possible...
        // Should be able to do a curve-wise comparison since all Areas
        // should evaluate their curves in the same top-down order.
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        Vector c = new AreaOp.XorOp().calculate(this.curves, other.curves);
        return c.isEmpty();
    }

    /**
     * Transforms the geometry of this <code>Area</code> using the specified
     * {@link BaseTransform}.  The geometry is transformed in place, which
     * permanently changes the enclosed area defined by this object.
     * @param tx the transformation used to transform the area
     * @throws NullPointerException if <code>t</code> is null
     */
    public void transform(BaseTransform tx) {
        if (tx == null) {
            throw new NullPointerException("transform must not be null");
        }
        // REMIND: A simpler operation can be performed for some types
        // of transform.
        curves = pathToCurves(getPathIterator(tx));
        invalidateBounds();
    }

    /**
     * Creates a new <code>Area</code> object that contains the same
     * geometry as this <code>Area</code> transformed by the specified
     * <code>BaseTransform</code>.  This <code>Area</code> object
     * is unchanged.
     * @param tx the specified <code>BaseTransform</code> used to transform
     *           the new <code>Area</code>
     * @throws NullPointerException if <code>t</code> is null
     * @return   a new <code>Area</code> object representing the transformed
     *           geometry.
     */
    public Area createTransformedArea(BaseTransform tx) {
        Area a = new Area(this);
        a.transform(tx);
        return a;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(float x, float y) {
        if (!getCachedBounds().contains(x, y)) {
            return false;
        }
        Enumeration enum_ = curves.elements();
        int crossings = 0;
        while (enum_.hasMoreElements()) {
            Curve c = (Curve) enum_.nextElement();
            crossings += c.crossingsFor(x, y);
        }
        return ((crossings & 1) == 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Point2D p) {
        return contains(p.x, p.y);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(float x, float y, float w, float h) {
        if (w < 0 || h < 0) {
            return false;
        }
        if (!getCachedBounds().contains(x, y) || !getCachedBounds().contains(x+w, y+h)) {
            return false;
        }
        Crossings c = Crossings.findCrossings(curves, x, y, x+w, y+h);
        return (c != null && c.covers(y, y+h));
    }

    /**
     * {@inheritDoc}
     */
    public boolean intersects(float x, float y, float w, float h) {
        if (w < 0 || h < 0) {
            return false;
        }
        if (!getCachedBounds().intersects(x, y, w, h)) {
            return false;
        }
        Crossings c = Crossings.findCrossings(curves, x, y, x+w, y+h);
        return (c == null || !c.isEmpty());
    }

    /**
     * Creates a {@link PathIterator} for the outline of this
     * <code>Area</code> object.  This <code>Area</code> object is unchanged.
     * @param tx an optional <code>BaseTransform</code> to be applied to
     * the coordinates as they are returned in the iteration, or
     * <code>null</code> if untransformed coordinates are desired
     * @return    the <code>PathIterator</code> object that returns the
     *          geometry of the outline of this <code>Area</code>, one
     *          segment at a time.
     */
    public PathIterator getPathIterator(BaseTransform tx) {
        return new AreaIterator(curves, tx);
    }

    /**
     * Creates a <code>PathIterator</code> for the flattened outline of
     * this <code>Area</code> object.  Only uncurved path segments
     * represented by the SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point
     * types are returned by the iterator.  This <code>Area</code>
     * object is unchanged.
     * @param tx an optional <code>BaseTransform</code> to be
     * applied to the coordinates as they are returned in the
     * iteration, or <code>null</code> if untransformed coordinates
     * are desired
     * @param flatness the maximum amount that the control points
     * for a given curve can vary from colinear before a subdivided
     * curve is replaced by a straight line connecting the end points
     * @return    the <code>PathIterator</code> object that returns the
     * geometry of the outline of this <code>Area</code>, one segment
     * at a time.
     */
    public PathIterator getPathIterator(BaseTransform tx, float flatness) {
        return new FlatteningPathIterator(getPathIterator(tx), flatness);
    }

    @Override
    public Area copy() {
        return new Area(this);
    }
}

class AreaIterator implements PathIterator {
    private BaseTransform transform;
    private Vector curves;
    private int index;
    private Curve prevcurve;
    private Curve thiscurve;

    public AreaIterator(Vector curves, BaseTransform tx) {
        this.curves = curves;
        this.transform = tx;
        if (curves.size() >= 1) {
            thiscurve = (Curve) curves.get(0);
        }
    }

    public int getWindingRule() {
        // REMIND: Which is better, EVEN_ODD or NON_ZERO?
        //         The paths calculated could be classified either way.
        //return WIND_EVEN_ODD;
        return WIND_NON_ZERO;
    }

    public boolean isDone() {
        return (prevcurve == null && thiscurve == null);
    }

    public void next() {
        if (prevcurve != null) {
            prevcurve = null;
        } else {
            prevcurve = thiscurve;
            index++;
            if (index < curves.size()) {
                thiscurve = (Curve) curves.get(index);
                if (thiscurve.getOrder() != 0 &&
                    prevcurve.getX1() == thiscurve.getX0() &&
                    prevcurve.getY1() == thiscurve.getY0())
                {
                    prevcurve = null;
                }
            } else {
                thiscurve = null;
            }
        }
    }

    public int currentSegment(float coords[]) {
        int segtype;
        int numpoints;
        if (prevcurve != null) {
            // Need to finish off junction between curves
            if (thiscurve == null || thiscurve.getOrder() == 0) {
                return SEG_CLOSE;
            }
            coords[0] = (float) thiscurve.getX0();
            coords[1] = (float) thiscurve.getY0();
            segtype = SEG_LINETO;
            numpoints = 1;
        } else if (thiscurve == null) {
            throw new NoSuchElementException("area iterator out of bounds");
        } else {
            segtype = thiscurve.getSegment(coords);
            numpoints = thiscurve.getOrder();
            if (numpoints == 0) {
                numpoints = 1;
            }
        }
        if (transform != null) {
            transform.transform(coords, 0, coords, 0, numpoints);
        }
        return segtype;
    }
}
