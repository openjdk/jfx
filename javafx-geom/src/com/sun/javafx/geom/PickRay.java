/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.geom.transform.NoninvertibleTransformException;

/**
 * A ray used for picking.
 */
public class PickRay {
    private Vec3d origin = new Vec3d();
    private Vec3d direction = new Vec3d();
    private boolean parallel = false;

//    static final double EPS = 1.0e-13;
    static final double EPS = 1.0e-5f;

    public PickRay() { }

    public PickRay(Vec3d origin, Vec3d direction) {
        set(origin, direction);
    }

    public PickRay(double x, double y) {
        set(x, y);
    }

    public PickRay(Vec3d origin, Vec3d direction, boolean parallel) {
        setOrigin(origin);
        setDirection(direction);
        this.parallel = parallel;
    }

    public final void set(Vec3d origin, Vec3d direction) {
        setOrigin(origin);
        setDirection(direction);
        parallel = false;
    }

    public final void set(double x, double y) {
        // Right now the parallel camera picks nodes even on negative distances
        // (behind the camera). Therefore, it doesn't matter
        // what is the Z coordinate of the origin. Also the reported distance
        // is always an infinity so it doesn't matter what is the magnitude
        // of the direction.
        setOrigin(x, y, 0);
        setDirection(0, 0, 1);
        parallel = true;
    }


    public void setPickRay(PickRay other) {
        setOrigin(other.origin);
        setDirection(other.direction);
        parallel = other.parallel;
    }

    public PickRay copy() {
        return new PickRay(origin, direction, parallel);
    }

    /**
     * Sets the origin of the pick ray in world coordinates.
     *
     * @param origin the origin (in world coordinates).
     */
    public void setOrigin(Vec3d origin) {
        this.origin.set(origin);
    }

    /**
     * Sets the origin of the pick ray in world coordinates.
     *
     * @param x the origin X coordinate
     * @param y the origin Y coordinate
     * @param z the origin Z coordinate
     */
    public void setOrigin(double x, double y, double z) {
        this.origin.set(x, y, z);
    }

    public Vec3d getOrigin(Vec3d rv) {
        if (rv == null) {
            rv = new Vec3d();
        }
        rv.set(origin);
        return rv;
    }

    public Vec3d getOriginNoClone() {
        return origin;
    }

    /**
     * Sets the direction vector of the pick ray. This vector need not
     * be normalized.
     *
     * @param direction the direction vector
     */
    public void setDirection(Vec3d direction) {
        this.direction.set(direction);
    }

    /**
     * Sets the direction of the pick ray. The vector need not be normalized.
     *
     * @param x the direction X magnitude
     * @param y the direction Y magnitude
     * @param z the direction Z magnitude
     */
    public void setDirection(double x, double y, double z) {
        this.direction.set(x, y, z);
    }

    public Vec3d getDirection(Vec3d rv) {
        if (rv == null) {
            rv = new Vec3d();
        }
        rv.set(direction);
        return rv;
    }

    public Vec3d getDirectionNoClone() {
        return direction;
    }

    public boolean isParallel() {
        return parallel;
    }

    public double distance(Vec3d iPnt) {
        double x = iPnt.x - origin.x;
        double y = iPnt.y - origin.y;
        double z = iPnt.z - origin.z;
        return Math.sqrt(x*x + y*y + z*z);
    }

    public boolean intersect(Vec3f[] tri, double[] distance, Vec3d iPnt) {
        return intersectRayOrSegment(tri,
            origin, direction,
            distance, iPnt, false);
    }

    /**
     * Project the ray through the specified (inverted) transform and
     * onto the Z=0 plane of the resulting coordinate system.
     * If a perspective projection is being used then only a point
     * that projects forward from the eye to the plane will be returned,
     * otherwise a null will be returned to indicate that the projection
     * is behind the eye.
     *
     * @param inversetx the inverse of the model transform into which the
     *                  ray is to be projected
     * @param perspective true if the projection is happening in perspective
     * @param tmpvec a temporary {@code Vec3d} object for internal use
     *               (may be null)
     * @param ret a {@code Point2D} object for storing the return value,
     *            or null if a new object should be returned.
     * @return
     */
    public Point2D projectToZeroPlane(BaseTransform inversetx,
                                      boolean perspective,
                                      Vec3d tmpvec, Point2D ret)
    {
        if (tmpvec == null) {
            tmpvec = new Vec3d();
        }
        inversetx.transform(origin, tmpvec);
        double origX = tmpvec.x;
        double origY = tmpvec.y;
        double origZ = tmpvec.z;
        tmpvec.add(origin, direction);
        inversetx.transform(tmpvec, tmpvec);
        double dirX = tmpvec.x - origX;
        double dirY = tmpvec.y - origY;
        double dirZ = tmpvec.z - origZ;
        // Handle the case where pickRay is almost parallel to the Z-plane
        if (almostZero(dirZ)) {
            return null;
        }
        double t = -origZ / dirZ;
        if (perspective && t < 0) {
            // TODO: Or should we use Infinity? (RT-26888)
            return null;
        }
        if (ret == null) {
            ret = new Point2D();
        }
        ret.setLocation((float) (origX + (dirX * t)),
                        (float) (origY + (dirY * t)));
        return ret;
    }

    // Good to find a home for commonly use util. code such as EPS.
    // and almostZero. This code currently defined in multiple places,
    // such as Affine3D and GeneralTransform3D.
    private static final double EPSILON_ABSOLUTE = 1.0e-5;

    static boolean almostZero(double a) {
        return ((a < EPSILON_ABSOLUTE) && (a > -EPSILON_ABSOLUTE));
    }

    private final Vec3d tempV3d = new Vec3d();
    private final Vec3d vec0 = new Vec3d();
    private final Vec3d vec1 = new Vec3d();
    private final Vec3d pNrm = new Vec3d();

    /**
     *  Return true if triangle intersects with ray. If true, the distance is
     *  stored in dist, and the intersection point is stored in iPnt
     */
    boolean intersectRayOrSegment(Vec3f[] coordinates,
            Vec3d origin, Vec3d direction,
            double[] dist, Vec3d iPnt, boolean isSegment) {

        double  absNrmX, absNrmY, absNrmZ, pD = 0.0;
        double pNrmDotrDir = 0.0;

        boolean isIntersect = false;
        int i, j, k=0, l = 0;

        // Compute plane normal.
        for (i=0; i<coordinates.length; i++) {
            if (i != coordinates.length-1) {
                l = i+1;
            } else {
                l = 0;
            }
            vec0.sub(coordinates[l], coordinates[i]);
            if (vec0.length() > 0.0) {
                break;
            }
        }

        for (j=l; j<coordinates.length; j++) {
            if (j != coordinates.length-1) {
                k = j+1;
            } else {
                k = 0;
            }
            vec1.sub(coordinates[k], coordinates[j]);
            if (vec1.length() > 0.0) {
                break;
            }
        }

        pNrm.cross(vec0,vec1);

        if ((vec1.length() == 0) || (pNrm.length() == 0)) {
            return false;
//          // degenerate to line if vec0.length() == 0
//          // or vec0.length > 0 and vec0 parallel to vec1
//          k = (l == 0 ? coordinates.length-1: l-1);
//          isIntersect = intersectLineAndRay(coordinates[l],
//                                            coordinates[k],
//                                            origin,
//                                            direction,
//                                            dist,
//                                            iPnt);
//
//          // put the Vectors on the freelist
//          return isIntersect;
        }

        // It is possible that Quad is degenerate to Triangle
        // at this point

        pNrmDotrDir = pNrm.dot(direction);

        // Ray is parallel to plane.
        if (pNrmDotrDir == 0.0) {
            return false;
//          // Ray is parallel to plane
//          // Check line/triangle intersection on plane.
//          for (i=0; i < coordinates.length ;i++) {
//              if (i != coordinates.length-1) {
//                  k = i+1;
//              } else {
//                  k = 0;
//              }
//              if (intersectLineAndRay(coordinates[i],
//                                      coordinates[k],
//                                      origin,
//                                      direction,
//                                      dist,
//                                      iPnt)) {
//                  isIntersect = true;
//                  break;
//              }
//          }
//          return isIntersect;
        }

        // Plane equation: (p - p0)*pNrm = 0 or p*pNrm = pD;
        tempV3d.set(coordinates[0]);
        pD = pNrm.dot(tempV3d);
        tempV3d.set(origin);

        // Substitute Ray equation:
        // p = origin + pi.distance*direction
        // into the above Plane equation

        dist[0] = (pD - pNrm.dot(tempV3d))/ pNrmDotrDir;

        // Ray intersects the plane behind the ray's origin.
        if ((dist[0] < -EPS ) ||
            (isSegment && (dist[0] > 1.0+EPS))) {
            // Ray intersects the plane behind the ray's origin
            // or intersect point not fall in Segment
            return false;
        }

        // Now, one thing for sure the ray intersect the plane.
        // Find the intersection point.
        if (iPnt == null) {
            iPnt = new Vec3d();
        }
        iPnt.x = origin.x + direction.x * dist[0];
        iPnt.y = origin.y + direction.y * dist[0];
        iPnt.z = origin.z + direction.z * dist[0];

        // Project 3d points onto 2d plane
        // Find the axis so that area of projection is maximize.
        absNrmX = Math.abs(pNrm.x);
        absNrmY = Math.abs(pNrm.y);
        absNrmZ = Math.abs(pNrm.z);

        // All sign of (y - y0) (x1 - x0) - (x - x0) (y1 - y0)
        // must agree.
        double sign, t, lastSign = 0;
        Vec3f p0 = coordinates[coordinates.length-1];
        Vec3f p1 = coordinates[0];

        isIntersect = true;

        if (absNrmX > absNrmY) {
            if (absNrmX < absNrmZ) {
                for (i=0; i < coordinates.length; i++) {
                    p0 = coordinates[i];
                    p1 = (i != coordinates.length-1 ? coordinates[i+1]: coordinates[0]);
                    sign = (iPnt.y - p0.y)*(p1.x - p0.x) -
                           (iPnt.x - p0.x)*(p1.y - p0.y);
                    if (isNonZero(sign)) {
                        if (sign*lastSign < 0) {
                            isIntersect = false;
                            break;
                        }
                        lastSign = sign;
                    } else { // point on line, check inside interval
                        t = p1.y - p0.y;
                        if (isNonZero(t)) {
                            t = (iPnt.y - p0.y)/t;
                            isIntersect = ((t > -EPS) && (t < 1+EPS));
                            break;
                        } else {
                            t = p1.x - p0.x;
                            if (isNonZero(t)) {
                                t = (iPnt.x - p0.x)/t;
                                isIntersect = ((t > -EPS) && (t < 1+EPS));
                                break;
                            } else {
    // Ignore degenerate line=>point happen when Quad => Triangle.
    // Note that by next round sign*lastSign = 0 so it will
    // not pass the interest test. This should only happen once in the
    // loop because we already check for degenerate geometry before.
                            }
                        }
                    }
                }
            } else {
                for (i=0; i<coordinates.length; i++) {
                    p0 = coordinates[i];
                    p1 = (i != coordinates.length-1 ? coordinates[i+1]: coordinates[0]);
                    sign = (iPnt.y - p0.y)*(p1.z - p0.z) -
                           (iPnt.z - p0.z)*(p1.y - p0.y);
                    if (isNonZero(sign)) {
                        if (sign*lastSign < 0) {
                            isIntersect = false;
                            break;
                        }
                        lastSign = sign;
                    } else { // point on line, check inside interval
                        t = p1.y - p0.y;

                        if (isNonZero(t)) {
                            t = (iPnt.y - p0.y)/t;
                            isIntersect = ((t > -EPS) && (t < 1+EPS));
                            break;

                        } else {
                            t = p1.z - p0.z;
                            if (isNonZero(t)) {
                                t = (iPnt.z - p0.z)/t;
                                isIntersect = ((t > -EPS) && (t < 1+EPS));
                                break;
                            } else {
                                //degenerate line=>point
                            }
                        }
                    }
                }
            }
        } else {
            if (absNrmY < absNrmZ) {
                for (i=0; i<coordinates.length; i++) {
                    p0 = coordinates[i];
                    p1 = (i != coordinates.length-1 ? coordinates[i+1]: coordinates[0]);
                    sign = (iPnt.y - p0.y)*(p1.x - p0.x) -
                           (iPnt.x - p0.x)*(p1.y - p0.y);
                    if (isNonZero(sign)) {
                        if (sign*lastSign < 0) {
                            isIntersect = false;
                            break;
                        }
                        lastSign = sign;
                    } else { // point on line, check inside interval
                        t = p1.y - p0.y;
                        if (isNonZero(t)) {
                            t = (iPnt.y - p0.y)/t;
                            isIntersect = ((t > -EPS) && (t < 1+EPS));
                            break;
                        } else {
                            t = p1.x - p0.x;
                            if (isNonZero(t)) {
                                t = (iPnt.x - p0.x)/t;
                                isIntersect = ((t > -EPS) && (t < 1+EPS));
                                break;
                            } else {
                                //degenerate line=>point
                            }
                        }
                    }
                }
            } else {
                for (i=0; i<coordinates.length; i++) {
                    p0 = coordinates[i];
                    p1 = (i != coordinates.length-1 ? coordinates[i+1]: coordinates[0]);
                    sign = (iPnt.x - p0.x)*(p1.z - p0.z) -
                           (iPnt.z - p0.z)*(p1.x - p0.x);
                    if (isNonZero(sign)) {
                        if (sign*lastSign < 0) {
                            isIntersect = false;
                            break;
                        }
                        lastSign = sign;
                    } else { // point on line, check inside interval
                        t = p1.x - p0.x;
                        if (isNonZero(t)) {
                            t = (iPnt.x - p0.x)/t;
                            isIntersect = ((t > -EPS) && (t < 1+EPS));
                            break;
                        } else {
                            t = p1.z - p0.z;
                            if (isNonZero(t)) {
                                t = (iPnt.z - p0.z)/t;
                                isIntersect = ((t > -EPS) && (t < 1+EPS));
                                break;
                            } else {
                                //degenerate line=>point
                            }
                        }
                    }
                }
            }
        }

        if (isIntersect) {
            dist[0] *= direction.length();
        }
        return isIntersect;
    }

    private static boolean isNonZero(double v) {
        return ((v > EPS) || (v < -EPS));

    }

    public void transform(BaseTransform t) {
        t.transform(origin, origin);
        t.deltaTransform(direction, direction);
    }

    public void inverseTransform(BaseTransform t) 
            throws NoninvertibleTransformException {
        t.inverseTransform(origin, origin);
        t.inverseDeltaTransform(direction, direction);
    }

    public PickRay project(BaseTransform inversetx,
                      boolean perspective,
                      Vec3d tmpvec, Point2D ret)
    {
        if (tmpvec == null) {
            tmpvec = new Vec3d();
        }
        inversetx.transform(origin, tmpvec);
        double origX = tmpvec.x;
        double origY = tmpvec.y;
        double origZ = tmpvec.z;
        tmpvec.add(origin, direction);
        inversetx.transform(tmpvec, tmpvec);
        double dirX = tmpvec.x - origX;
        double dirY = tmpvec.y - origY;
        double dirZ = tmpvec.z - origZ;

        PickRay pr = new PickRay();
        pr.origin.x = origX;
        pr.origin.y = origY;
        pr.origin.z = origZ;

        pr.direction.x = dirX;
        pr.direction.y = dirY;
        pr.direction.z = dirZ;

        return pr;
    }

    @Override
    public String toString() {
        return "origin: " + origin + "  direction: " + direction;
    }
}
