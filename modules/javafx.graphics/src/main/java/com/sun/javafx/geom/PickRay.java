/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;

/**
 * A ray used for picking.
 */
public class PickRay {
    private Vec3d origin = new Vec3d();
    private Vec3d direction = new Vec3d();
    private double nearClip = 0.0;
    private double farClip = Double.POSITIVE_INFINITY;

//    static final double EPS = 1.0e-13;
    static final double EPS = 1.0e-5f;

    public PickRay() { }

    public PickRay(Vec3d origin, Vec3d direction, double nearClip, double farClip) {
        set(origin, direction, nearClip, farClip);
    }

    public PickRay(double x, double y, double z, double nearClip, double farClip) {
        set(x, y, z, nearClip, farClip);
    }

    public static PickRay computePerspectivePickRay(
            double x, double y, boolean fixedEye,
            double viewWidth, double viewHeight,
            double fieldOfViewRadians, boolean verticalFieldOfView,
            Affine3D cameraTransform,
            double nearClip, double farClip,
            PickRay pickRay) {

        if (pickRay == null) {
            pickRay = new PickRay();
        }

        Vec3d direction = pickRay.getDirectionNoClone();
        double halfViewWidth = viewWidth / 2.0;
        double halfViewHeight = viewHeight / 2.0;
        double halfViewDim = verticalFieldOfView? halfViewHeight: halfViewWidth;
        // Distance to projection plane from eye
        double distanceZ = halfViewDim / Math.tan(fieldOfViewRadians / 2.0);

        direction.x = x - halfViewWidth;
        direction.y = y - halfViewHeight;
        direction.z = distanceZ;

        Vec3d eye = pickRay.getOriginNoClone();

        if (fixedEye) {
            eye.set(0.0, 0.0, 0.0);
        } else {
            // set eye at center of viewport and move back so that projection plane
            // is at Z = 0
            eye.set(halfViewWidth, halfViewHeight, -distanceZ);
        }

        pickRay.nearClip = nearClip * (direction.length() / (fixedEye ? distanceZ : 1.0));
        pickRay.farClip = farClip * (direction.length() / (fixedEye ? distanceZ : 1.0));

        pickRay.transform(cameraTransform);

        return pickRay;
    }

    public static PickRay computeParallelPickRay(
            double x, double y, double viewHeight,
            Affine3D cameraTransform,
            double nearClip, double farClip,
            PickRay pickRay) {

        if (pickRay == null) {
            pickRay = new PickRay();
        }

        // This is the same math as in the perspective case, fixed
        // for the default 30 degrees vertical field of view.
        final double distanceZ = (viewHeight / 2.0)
                / Math.tan(Math.toRadians(15.0));

        pickRay.set(x, y, distanceZ, nearClip * distanceZ, farClip * distanceZ);

        if (cameraTransform != null) {
            pickRay.transform(cameraTransform);
        }

        return pickRay;
    }

    public final void set(Vec3d origin, Vec3d direction, double nearClip, double farClip) {
        setOrigin(origin);
        setDirection(direction);
        this.nearClip = nearClip;
        this.farClip = farClip;
    }

    public final void set(double x, double y, double z, double nearClip, double farClip) {
        setOrigin(x, y, -z);
        setDirection(0, 0, z);
        this.nearClip = nearClip;
        this.farClip = farClip;
    }


    public void setPickRay(PickRay other) {
        setOrigin(other.origin);
        setDirection(other.direction);
        nearClip = other.nearClip;
        farClip = other.farClip;
    }

    public PickRay copy() {
        return new PickRay(origin, direction, nearClip, farClip);
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

    public double getNearClip() {
        return nearClip;
    }

    public double getFarClip() {
        return farClip;
    }

    public double distance(Vec3d iPnt) {
        double x = iPnt.x - origin.x;
        double y = iPnt.y - origin.y;
        double z = iPnt.z - origin.z;
        return Math.sqrt(x*x + y*y + z*z);
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
