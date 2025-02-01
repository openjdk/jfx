/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.transform;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.geometry.Point3D;

import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import javafx.geometry.Point2D;


/**
 * This class represents an {@code Affine} object that rotates coordinates
 * around an anchor point. This operation is equivalent to translating the
 * coordinates so that the anchor point is at the origin (S1), then rotating them
 * about the new origin (S2), and finally translating so that the
 * intermediate origin is restored to the coordinates of the original
 * anchor point (S3).
 * <p>
 * The matrix representing the rotation transformation around an axis {@code (x,y,z)}
 * by an angle {@code t} is as follows:
 * <pre>
 *              [   cos(t)   -sin(t)   0   x-x*cos(t)+y*sin(t)   ]
 *              [   sin(t)    cos(t)   0   y-x*sin(t)-y*cos(t)   ]
 *              [     0         0      1           z             ]
 * </pre>
 * <p>
 * For example, to rotate a text 30 degrees around the Z-axis at
 * anchor point of (50,30):
 * <pre>{@code
 * Text text = new Text("This is a test");
 * text.setX(10);
 * text.setY(50);
 * text.setFont(new Font(20));
 *
 * text.getTransforms().add(new Rotate(30, 50, 30));
 * }</pre>
 *
 * @since JavaFX 2.0
 */

public class Rotate extends Transform {

    /**
     * Specifies the X-axis as the axis of rotation.
     */
    public static final Point3D X_AXIS = new Point3D(1,0,0);

    /**
     * Specifies the Y-axis as the axis of rotation.
     */
    public static final Point3D Y_AXIS = new Point3D(0,1,0);

    /**
     * Specifies the Z-axis as the axis of rotation.
     */
    public static final Point3D Z_AXIS = new Point3D(0,0,1);

    /**
     * Avoids lot of repeated computation.
     * @see #MatrixCache
     */
    private MatrixCache cache;

    /**
     * Avoids lot of repeated computation.
     * @see #MatrixCache
     */
    private MatrixCache inverseCache;

    /**
     * Creates a default Rotate transform (identity).
     */
    public Rotate() {
    }

    /**
     * Creates a two-dimensional Rotate transform.
     * The pivot point is set to (0,0)
     * @param angle the angle of rotation measured in degrees
     */
    public Rotate(double angle) {
        setAngle(angle);
    }

    /**
     * Creates a three-dimensional Rotate transform.
     * The pivot point is set to (0,0,0)
     * @param angle the angle of rotation measured in degrees
     * @param axis the axis of rotation
     */
    public Rotate(double angle, Point3D axis) {
        setAngle(angle);
        setAxis(axis);
    }

    /**
     * Creates a two-dimensional Rotate transform with pivot.
     * @param angle the angle of rotation measured in degrees
     * @param pivotX the X coordinate of the rotation pivot point
     * @param pivotY the Y coordinate of the rotation pivot point
     */
    public Rotate(double angle, double pivotX, double pivotY) {
        setAngle(angle);
        setPivotX(pivotX);
        setPivotY(pivotY);
    }

    /**
     * Creates a simple Rotate transform with three-dimensional pivot.
     * @param angle the angle of rotation measured in degrees
     * @param pivotX the X coordinate of the rotation pivot point
     * @param pivotY the Y coordinate of the rotation pivot point
     * @param pivotZ the Z coordinate of the rotation pivot point
     */
    public Rotate(double angle, double pivotX, double pivotY, double pivotZ) {
        this(angle, pivotX, pivotY);
        setPivotZ(pivotZ);
    }

    /**
     * Creates a three-dimensional Rotate transform with pivot.
     * @param angle the angle of rotation measured in degrees
     * @param pivotX the X coordinate of the rotation pivot point
     * @param pivotY the Y coordinate of the rotation pivot point
     * @param pivotZ the Z coordinate of the rotation pivot point
     * @param axis the axis of rotation
     */
    public Rotate(double angle, double pivotX, double pivotY, double pivotZ, Point3D axis) {
        this(angle, pivotX, pivotY);
        setPivotZ(pivotZ);
        setAxis(axis);
    }

    /**
     * Defines the angle of rotation measured in degrees.
     */
    private DoubleProperty angle;


    public final void setAngle(double value) {
        angleProperty().set(value);
    }

    public final double getAngle() {
        return angle == null ? 0.0 : angle.get();
    }

    public final DoubleProperty angleProperty() {
        if (angle == null) {
            angle = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Rotate.this;
                }

                @Override
                public String getName() {
                    return "angle";
                }
            };
        }
        return angle;
    }

    /**
     * Defines the X coordinate of the rotation pivot point.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty pivotX;


    public final void setPivotX(double value) {
        pivotXProperty().set(value);
    }

    public final double getPivotX() {
        return pivotX == null ? 0.0 : pivotX.get();
    }

    public final DoubleProperty pivotXProperty() {
        if (pivotX == null) {
            pivotX = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Rotate.this;
                }

                @Override
                public String getName() {
                    return "pivotX";
                }
            };
        }
        return pivotX;
    }

    /**
     * Defines the Y coordinate of the rotation pivot point.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty pivotY;


    public final void setPivotY(double value) {
        pivotYProperty().set(value);
    }

    public final double getPivotY() {
        return pivotY == null ? 0.0 : pivotY.get();
    }

    public final DoubleProperty pivotYProperty() {
        if (pivotY == null) {
            pivotY = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Rotate.this;
                }

                @Override
                public String getName() {
                    return "pivotY";
                }
            };
        }
        return pivotY;
    }

    /**
     * Defines the Z coordinate of the rotation pivot point.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty pivotZ;


    public final void setPivotZ(double value) {
        pivotZProperty().set(value);
    }

    public final double getPivotZ() {
        return pivotZ == null ? 0.0 : pivotZ.get();
    }

    public final DoubleProperty pivotZProperty() {
        if (pivotZ == null) {
            pivotZ = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Rotate.this;
                }

                @Override
                public String getName() {
                    return "pivotZ";
                }
            };
        }
        return pivotZ;
    }

    /**
     * Defines the axis of rotation at the pivot point.
     */
    private ObjectProperty<Point3D> axis;


    public final void setAxis(Point3D value) {
        axisProperty().set(value);
    }

    public final Point3D getAxis() {
        return axis == null ? Z_AXIS : axis.get();
    }

    public final ObjectProperty<Point3D> axisProperty() {
        if (axis == null) {
            axis = new ObjectPropertyBase<Point3D>(Z_AXIS) {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Rotate.this;
                }

                @Override
                public String getName() {
                    return "axis";
                }
            };
        }
        return axis;
    }

    /* *************************************************************************
     *                                                                         *
     *                         Element getters                                 *
     *                                                                         *
     **************************************************************************/

    @Override
    public double getMxx() {
        updateCache();
        return cache.mxx;
    }

    @Override
    public double getMxy() {
        updateCache();
        return cache.mxy;
    }

    @Override
    public double getMxz() {
        updateCache();
        return cache.mxz;
    }

    @Override
    public double getTx() {
        updateCache();
        return cache.tx;
    }

    @Override
    public double getMyx() {
        updateCache();
        return cache.myx;
    }

    @Override
    public double getMyy() {
        updateCache();
        return cache.myy;
    }

    @Override
    public double getMyz() {
        updateCache();
        return cache.myz;
    }

    @Override
    public double getTy() {
        updateCache();
        return cache.ty;
    }

    @Override
    public double getMzx() {
        updateCache();
        return cache.mzx;
    }

    @Override
    public double getMzy() {
        updateCache();
        return cache.mzy;
    }

    @Override
    public double getMzz() {
        updateCache();
        return cache.mzz;
    }

    @Override
    public double getTz() {
        updateCache();
        return cache.tz;
    }

    /* *************************************************************************
     *                                                                         *
     *                           State getters                                 *
     *                                                                         *
     **************************************************************************/

    @Override
    boolean computeIs2D() {
        final Point3D a = getAxis();
        return (a.getX() == 0.0 && a.getY() == 0.0) || getAngle() == 0;
    }

    @Override
    boolean computeIsIdentity() {
        if (getAngle() == 0.0) {
            return true;
        }

        final Point3D a = getAxis();
        return a.getX() == 0 && a.getY() == 0 && a.getZ() == 0.0;
    }

    /* *************************************************************************
     *                                                                         *
     *                           Array getters                                 *
     *                                                                         *
     **************************************************************************/

    @Override
    void fill2DArray(double[] array) {
        updateCache();
        array[0] = cache.mxx;
        array[1] = cache.mxy;
        array[2] = cache.tx;
        array[3] = cache.myx;
        array[4] = cache.myy;
        array[5] = cache.ty;
    }

    @Override
    void fill3DArray(double[] array) {
        updateCache();
        array[0] = cache.mxx;
        array[1] = cache.mxy;
        array[2] = cache.mxz;
        array[3] = cache.tx;
        array[4] = cache.myx;
        array[5] = cache.myy;
        array[6] = cache.myz;
        array[7] = cache.ty;
        array[8] = cache.mzx;
        array[9] = cache.mzy;
        array[10] = cache.mzz;
        array[11] = cache.tz;
        return;
    }

    /* *************************************************************************
     *                                                                         *
     *                         Transform creators                              *
     *                                                                         *
     **************************************************************************/

    @Override
    public Transform createConcatenation(Transform transform) {
        if (transform instanceof Rotate) {
            Rotate r = (Rotate) transform;
            final double px = getPivotX();
            final double py = getPivotY();
            final double pz = getPivotZ();

            if ((r.getAxis() == getAxis() ||
                        r.getAxis().normalize().equals(getAxis().normalize())) &&
                    px == r.getPivotX() &&
                    py == r.getPivotY() &&
                    pz == r.getPivotZ()) {
                return new Rotate(getAngle() + r.getAngle(), px, py, pz, getAxis());
            }
        }

        if (transform instanceof Affine) {
            Affine a = (Affine) transform.clone();
            a.prepend(this);
            return a;
        }

        return super.createConcatenation(transform);
    }

    @Override
    public Transform createInverse() throws NonInvertibleTransformException {
        return new Rotate(-getAngle(), getPivotX(), getPivotY(), getPivotZ(),
                getAxis());
    }

    @Override
    public Rotate clone() {
        return new Rotate(getAngle(), getPivotX(), getPivotY(), getPivotZ(),
                getAxis());
    }

    /* *************************************************************************
     *                                                                         *
     *                     Transform, Inverse Transform                        *
     *                                                                         *
     **************************************************************************/

    @Override
    public Point2D transform(double x, double y) {
        ensureCanTransform2DPoint();

        updateCache();

        return new Point2D(
            cache.mxx * x + cache.mxy * y + cache.tx,
            cache.myx * x + cache.myy * y + cache.ty);
    }

    @Override
    public Point3D transform(double x, double y, double z) {
        updateCache();

        return new Point3D(
            cache.mxx * x + cache.mxy * y + cache.mxz * z + cache.tx,
            cache.myx * x + cache.myy * y + cache.myz * z + cache.ty,
            cache.mzx * x + cache.mzy * y + cache.mzz * z + cache.tz);
    }

    @Override
    void transform2DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts) {
        updateCache();

        while (--numPts >= 0) {
            final double x = srcPts[srcOff++];
            final double y = srcPts[srcOff++];

            dstPts[dstOff++] = cache.mxx * x + cache.mxy * y + cache.tx;
            dstPts[dstOff++] = cache.myx * x + cache.myy * y + cache.ty;
        }
    }

    @Override
    void transform3DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts) {

        updateCache();

        while (--numPts >= 0) {
            final double x = srcPts[srcOff++];
            final double y = srcPts[srcOff++];
            final double z = srcPts[srcOff++];

            dstPts[dstOff++] = cache.mxx * x + cache.mxy * y + cache.mxz * z + cache.tx;
            dstPts[dstOff++] = cache.myx * x + cache.myy * y + cache.myz * z + cache.ty;
            dstPts[dstOff++] = cache.mzx * x + cache.mzy * y + cache.mzz * z + cache.tz;
        }
    }

    @Override
    public Point2D deltaTransform(double x, double y) {
        ensureCanTransform2DPoint();

        updateCache();

        return new Point2D(
            cache.mxx * x + cache.mxy * y,
            cache.myx * x + cache.myy * y);
    }

    @Override
    public Point3D deltaTransform(double x, double y, double z) {
        updateCache();

        return new Point3D(
            cache.mxx * x + cache.mxy * y + cache.mxz * z,
            cache.myx * x + cache.myy * y + cache.myz * z,
            cache.mzx * x + cache.mzy * y + cache.mzz * z);
    }

    @Override
    public Point2D inverseTransform(double x, double y) {
        ensureCanTransform2DPoint();

        updateInverseCache();

        return new Point2D(
            inverseCache.mxx * x + inverseCache.mxy * y + inverseCache.tx,
            inverseCache.myx * x + inverseCache.myy * y + inverseCache.ty);
    }

    @Override
    public Point3D inverseTransform(double x, double y, double z) {
        updateInverseCache();

        return new Point3D(
            inverseCache.mxx * x + inverseCache.mxy * y + inverseCache.mxz * z
                + inverseCache.tx,
            inverseCache.myx * x + inverseCache.myy * y + inverseCache.myz * z
                + inverseCache.ty,
            inverseCache.mzx * x + inverseCache.mzy * y + inverseCache.mzz * z
                + inverseCache.tz);
    }

    @Override
    void inverseTransform2DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts) {
        updateInverseCache();

        while (--numPts >= 0) {
            final double x = srcPts[srcOff++];
            final double y = srcPts[srcOff++];

            dstPts[dstOff++] = inverseCache.mxx * x + inverseCache.mxy * y
                    + inverseCache.tx;
            dstPts[dstOff++] = inverseCache.myx * x + inverseCache.myy * y
                    + inverseCache.ty;
        }
    }

    @Override
    void inverseTransform3DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts) {

        updateInverseCache();

        while (--numPts >= 0) {
            final double x = srcPts[srcOff++];
            final double y = srcPts[srcOff++];
            final double z = srcPts[srcOff++];

            dstPts[dstOff++] = inverseCache.mxx * x + inverseCache.mxy * y
                    + inverseCache.mxz * z + inverseCache.tx;
            dstPts[dstOff++] = inverseCache.myx * x + inverseCache.myy * y
                    + inverseCache.myz * z + inverseCache.ty;
            dstPts[dstOff++] = inverseCache.mzx * x + inverseCache.mzy * y
                    + inverseCache.mzz * z + inverseCache.tz;
        }
    }

    @Override
    public Point2D inverseDeltaTransform(double x, double y) {
        ensureCanTransform2DPoint();

        updateInverseCache();

        return new Point2D(
            inverseCache.mxx * x + inverseCache.mxy * y,
            inverseCache.myx * x + inverseCache.myy * y);
    }

    @Override
    public Point3D inverseDeltaTransform(double x, double y, double z) {
        updateInverseCache();

        return new Point3D(
            inverseCache.mxx * x + inverseCache.mxy * y + inverseCache.mxz * z,
            inverseCache.myx * x + inverseCache.myy * y + inverseCache.myz * z,
            inverseCache.mzx * x + inverseCache.mzy * y + inverseCache.mzz * z);
    }

    /* *************************************************************************
     *                                                                         *
     *                               Other API                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns a string representation of this {@code Rotate} object.
     * @return a string representation of this {@code Rotate} object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Rotate [");

        sb.append("angle=").append(getAngle());
        sb.append(", pivotX=").append(getPivotX());
        sb.append(", pivotY=").append(getPivotY());
        sb.append(", pivotZ=").append(getPivotZ());
        sb.append(", axis=").append(getAxis());

        return sb.append("]").toString();
    }

    /* *************************************************************************
     *                                                                         *
     *                    Internal implementation stuff                        *
     *                                                                         *
     **************************************************************************/

    @Override
    void apply(final Affine3D trans) {
        double localPivotX = getPivotX();
        double localPivotY = getPivotY();
        double localPivotZ = getPivotZ();
        double localAngle = getAngle();

        if (localPivotX != 0 || localPivotY != 0 || localPivotZ != 0) {
            trans.translate(localPivotX, localPivotY, localPivotZ);
            trans.rotate(Math.toRadians(localAngle),
                         getAxis().getX(),getAxis().getY(), getAxis().getZ());
            trans.translate(-localPivotX, -localPivotY, -localPivotZ);
        } else {
            trans.rotate(Math.toRadians(localAngle),
                         getAxis().getX(), getAxis().getY(), getAxis().getZ());
        }
    }

    @Override
    BaseTransform derive(BaseTransform trans) {
        if (isIdentity()) {
            return trans;
        }

        double localPivotX = getPivotX();
        double localPivotY = getPivotY();
        double localPivotZ = getPivotZ();
        double localAngle = getAngle();

        if (localPivotX != 0 || localPivotY != 0 || localPivotZ != 0) {
            trans = trans.deriveWithTranslation(localPivotX, localPivotY, localPivotZ);
            trans = trans.deriveWithRotation(Math.toRadians(localAngle),
                         getAxis().getX(),getAxis().getY(), getAxis().getZ());
            return trans.deriveWithTranslation(-localPivotX, -localPivotY, -localPivotZ);
        } else {
            return trans.deriveWithRotation(Math.toRadians(localAngle),
                         getAxis().getX(), getAxis().getY(), getAxis().getZ());
        }
    }

    @Override
    void validate() {
        getAxis();
        getAngle();
        getPivotX();
        getPivotY();
        getPivotZ();
    }

    @Override
    protected void transformChanged() {
        if (inverseCache != null) {
            inverseCache.invalidate();
        }
        if (cache != null) {
            cache.invalidate();
        }
        super.transformChanged();
    }

    @Override
    void appendTo(Affine a) {
        a.appendRotation(getAngle(), getPivotX(), getPivotY(), getPivotZ(),
                getAxis());
    }

    @Override
    void prependTo(Affine a) {
        a.prependRotation(getAngle(), getPivotX(), getPivotY(), getPivotZ(),
                getAxis());
    }

    /**
     * Updates the matrix cache
     */
    private void updateCache() {
        if (cache == null) {
            cache = new MatrixCache();
        }

        if (!cache.valid) {
            cache.update(getAngle(), getAxis(),
                    getPivotX(), getPivotY(), getPivotZ());
        }
    }

    /**
     * Updates the inverse matrix cache
     */
    private void updateInverseCache() {
        if (inverseCache == null) {
            inverseCache = new MatrixCache();
        }

        if (!inverseCache.valid) {
            inverseCache.update(-getAngle(), getAxis(),
                    getPivotX(), getPivotY(), getPivotZ());
        }
    }

    /**
     * Matrix cache. Computing single transformation matrix elements for
     * a general rotation is quite expensive. Also each of those partial
     * computations need some common operations to be made (compute sin
     * and cos, normalize axis). Therefore with the direct element computations
     * if all the getters for the elements are called to get the matrix,
     * the result is slow.
     *
     * If a matrix element is asked for, we can reasonably anticipate that
     * some other elements will be asked for as well. So when any element
     * needs to be computed, we compute the entire matrix, cache it,
     * and use the stored values until the transform changes.
     */
    private static class MatrixCache {
        boolean valid = false;
        boolean is3D = false;

        double mxx, mxy, mxz, tx,
               myx, myy, myz, ty,
               mzx, mzy, mzz, tz;

        public MatrixCache() {
            // to have the 3D part right when using 2D-only
            mzz = 1.0;
        }

        public void update(double angle, Point3D axis,
                double px, double py, double pz) {

            final double rads = Math.toRadians(angle);
            final double sin = Math.sin(rads);
            final double cos = Math.cos(rads);

            if (axis == Z_AXIS ||
                    (axis.getX() == 0.0 &&
                     axis.getY() == 0.0 &&
                     axis.getZ() > 0.0)) {
                // 2D case
                mxx = cos;
                mxy = -sin;
                tx = px * (1 - cos) + py * sin;
                myx = sin;
                myy = cos;
                ty = py * (1 - cos) - px * sin;

                if (is3D) {
                    // Was 3D, needs to set the 3D values
                    mxz = 0.0;
                    myz = 0.0;
                    mzx = 0.0;
                    mzy = 0.0;
                    mzz = 1.0;
                    tz = 0.0;
                    is3D = false;
                }
                valid = true;
                return;
            }
            // 3D case
            is3D = true;

            double axisX, axisY, axisZ;

            if (axis == X_AXIS || axis == Y_AXIS || axis == Z_AXIS) {
                axisX = axis.getX();
                axisY = axis.getY();
                axisZ = axis.getZ();
            } else {
                // normalize
                final double mag = Math.sqrt(axis.getX() * axis.getX() +
                        axis.getY() * axis.getY() + axis.getZ() * axis.getZ());

                if (mag == 0.0) {
                    mxx = 1; mxy = 0; mxz = 0; tx = 0;
                    myx = 0; myy = 1; myz = 0; ty = 0;
                    mzx = 0; mzy = 0; mzz = 1; tz = 0;
                    valid = true;
                    return;
                } else {
                    axisX = axis.getX() / mag;
                    axisY = axis.getY() / mag;
                    axisZ = axis.getZ() / mag;
                }
            }

            mxx = cos + axisX * axisX * (1 - cos);
            mxy = axisX * axisY * (1 - cos) - axisZ * sin;
            mxz = axisX * axisZ * (1 - cos) + axisY * sin;
            tx = px * (1 - mxx) - py * mxy - pz * mxz;

            myx = axisY * axisX * (1 - cos) + axisZ * sin;
            myy = cos + axisY * axisY * (1 - cos);
            myz = axisY * axisZ * (1 - cos) - axisX * sin;
            ty = py * (1 - myy) - px * myx - pz * myz;

            mzx = axisZ * axisX * (1 - cos) - axisY * sin;
            mzy = axisZ * axisY * (1 - cos) + axisX * sin;
            mzz = cos + axisZ * axisZ * (1 - cos);
            tz = pz * (1 - mzz) - px * mzx - py * mzy;

            valid = true;
        }

        public void invalidate() {
            valid = false;
        }
    }
}
