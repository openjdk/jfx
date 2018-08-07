/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;


/**
 * This class represents an {@code Affine} object that shears coordinates
 * by the specified multipliers. The matrix representing the shearing transformation
 * around a pivot point {@code (pivotX, pivotY)} with multiplication factors {@code x}
 * and {@code y} is as follows:
 * <pre>
 *              [   1   x   0   -x*pivotY   ]
 *              [   y   1   0   -y*pivotX   ]
 *              [   0   0   1       0       ]
 * </pre>
 *
 * <p>
 * For example:
 * <pre>{@code
 * Text text = new Text("Using Shear for pseudo-italic font");
 * text.setX(20);
 * text.setY(50);
 * text.setFont(new Font(20));
 *
 * text.getTransforms().add(new Shear(-0.35, 0));
 * }</pre>
 *
 * @since JavaFX 2.0
 */
public class Shear extends Transform {

    /**
     * Creates a default Shear (identity).
     */
    public Shear() {
    }

    /**
     * Creates a new instance of Shear.
     * The pivot point is set to (0,0)
     * @param x the multiplier by which coordinates are shifted in the direction
     * of the positive X axis as a factor of their Y coordinate
     * @param y the multiplier by which coordinates are shifted in the direction
     * of the positive Y axis as a factor of their X coordinate
     */
    public Shear(double x, double y) {
        setX(x);
        setY(y);
    }

    /**
     * Creates a new instance of Shear with pivot.
     * @param x the multiplier by which coordinates are shifted in the direction
     * of the positive X axis as a factor of their Y coordinate
     * @param y the multiplier by which coordinates are shifted in the direction
     * of the positive Y axis as a factor of their X coordinate
     * @param pivotX the X coordinate of the shear pivot point
     * @param pivotY the Y coordinate of the shear pivot point
     */
    public Shear(double x, double y, double pivotX, double pivotY) {
        setX(x);
        setY(y);
        setPivotX(pivotX);
        setPivotY(pivotY);
    }

    /**
     * Defines the multiplier by which coordinates are shifted in the direction
     * of the positive X axis as a factor of their Y coordinate. Typical values
     * are in the range -1 to 1, exclusive.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty x;


    public final void setX(double value) {
        xProperty().set(value);
    }

    public final double getX() {
        return x == null ? 0.0 : x.get();
    }

    public final DoubleProperty xProperty() {
        if (x == null) {
            x = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Shear.this;
                }

                @Override
                public String getName() {
                    return "x";
                }
            };
        }
        return x;
    }

    /**
     * Defines the multiplier by which coordinates are shifted in the direction
     * of the positive Y axis as a factor of their X coordinate. Typical values
     * are in the range -1 to 1, exclusive.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty y;


    public final void setY(double value) {
        yProperty().set(value);
    }

    public final double getY() {
        return y == null ? 0.0 : y.get();
    }

    public final DoubleProperty yProperty() {
        if (y == null) {
            y = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Shear.this;
                }

                @Override
                public String getName() {
                    return "y";
                }
            };
        }
        return y;
    }

    /**
     * Defines the X coordinate of the shear pivot point.
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
                    return Shear.this;
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
     * Defines the Y coordinate of the shear pivot point.
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
                    return Shear.this;
                }

                @Override
                public String getName() {
                    return "pivotY";
                }
            };
        }
        return pivotY;
    }

    /* *************************************************************************
     *                                                                         *
     *                         Element getters                                 *
     *                                                                         *
     **************************************************************************/

    @Override
    public double getMxy() {
        return getX();
    }

    @Override
    public double getMyx() {
        return getY();
    }

    @Override
    public double getTx() {
        return -getX() * getPivotY();
    }

    @Override
    public double getTy() {
        return -getY() * getPivotX();
    }

    /* *************************************************************************
     *                                                                         *
     *                           State getters                                 *
     *                                                                         *
     **************************************************************************/

    @Override
    boolean computeIs2D() {
        return true;
    }

    @Override
    boolean computeIsIdentity() {
        return getX() == 0.0 && getY() == 0.0;
    }

    /* *************************************************************************
     *                                                                         *
     *                           Array getters                                 *
     *                                                                         *
     **************************************************************************/

    @Override
    void fill2DArray(double[] array) {
        final double sx = getX();
        final double sy = getY();

        array[0] = 1.0;
        array[1] = sx;
        array[2] = -sx * getPivotY();
        array[3] = sy;
        array[4] = 1.0;
        array[5] = -sy * getPivotX();
    }

    @Override
    void fill3DArray(double[] array) {
        final double sx = getX();
        final double sy = getY();

        array[0] = 1.0;
        array[1] = sx;
        array[2] = 0.0;
        array[3] = -sx * getPivotY();
        array[4] = sy;
        array[5] = 1.0;
        array[6] = 0.0;
        array[7] = -sy * getPivotX();
        array[8] = 0.0;
        array[9] = 0.0;
        array[10] = 1.0;
        array[11] = 0.0;
    }

    /* *************************************************************************
     *                                                                         *
     *                         Transform creators                              *
     *                                                                         *
     **************************************************************************/

    @Override
    public Transform createConcatenation(Transform transform) {

        if (transform instanceof Affine) {
            Affine a = (Affine) transform.clone();
            a.prepend(this);
            return a;
        }

        final double sx = getX();
        final double sy = getY();

        final double txx = transform.getMxx();
        final double txy = transform.getMxy();
        final double txz = transform.getMxz();
        final double ttx = transform.getTx();
        final double tyx = transform.getMyx();
        final double tyy = transform.getMyy();
        final double tyz = transform.getMyz();
        final double tty = transform.getTy();
        return new Affine(
                txx + sx * tyx,
                txy + sx * tyy,
                txz + sx * tyz,
                ttx + sx * tty - sx * getPivotY(),
                sy * txx + tyx,
                sy * txy + tyy,
                sy * txz + tyz,
                sy * ttx + tty - sy * getPivotX(),
                transform.getMzx(),
                transform.getMzy(),
                transform.getMzz(),
                transform.getTz());
    }

    @Override
    public Transform createInverse() {
        final double sx = getX();
        final double sy = getY();

        if (sy == 0.0) {
            return new Shear(-sx, 0.0, 0.0, getPivotY());
        }

        if (sx == 0.0) {
            return new Shear(0.0, -sy, getPivotX(), 0.0);
        }

        final double px = getPivotX();
        final double py = getPivotY();
        final double coef = 1.0 / (1.0 - sx * sy);

        return new Affine(
                coef,       -sx * coef,         0, sx * (py - sy * px) * coef,
                -sy * coef, 1 + sx * sy * coef, 0, sy * px + sy * (sx * sy * px - sx * py) * coef,
                0,          0,                  1, 0);
    }

    @Override
    public Shear clone() {
        return new Shear(getX(), getY(), getPivotX(), getPivotY());
    }

    /* *************************************************************************
     *                                                                         *
     *                     Transform, Inverse Transform                        *
     *                                                                         *
     **************************************************************************/

    @Override
    public Point2D transform(double x, double y) {
        final double mxy = getX();
        final double myx = getY();

        return new Point2D(
            x + mxy * y - mxy * getPivotY(),
            myx * x + y - myx * getPivotX());
    }

    @Override
    public Point3D transform(double x, double y, double z) {
        final double mxy = getX();
        final double myx = getY();

        return new Point3D(
            x + mxy * y - mxy * getPivotY(),
            myx * x + y - myx * getPivotX(),
            z);
    }

    @Override
    void transform2DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts) {
        final double xy = getX();
        final double yx = getY();
        final double px = getPivotX();
        final double py = getPivotY();

        while (--numPts >= 0) {
            final double x = srcPts[srcOff++];
            final double y = srcPts[srcOff++];

            dstPts[dstOff++] = x + xy * y - xy * py;
            dstPts[dstOff++] = yx * x + y - yx * px;
        }
    }

    @Override
    void transform3DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts) {
        final double xy = getX();
        final double yx = getY();
        final double px = getPivotX();
        final double py = getPivotY();

        while (--numPts >= 0) {
            final double x = srcPts[srcOff++];
            final double y = srcPts[srcOff++];

            dstPts[dstOff++] = x + xy * y - xy * py;
            dstPts[dstOff++] = yx * x + y - yx * px;
            dstPts[dstOff++] = srcPts[srcOff++];
        }
    }

    @Override
    public Point2D deltaTransform(double x, double y) {

        return new Point2D(
            x + getX() * y,
            getY() * x + y);
    }

    @Override
    public Point3D deltaTransform(double x, double y, double z) {
        return new Point3D(
            x + getX() * y,
            getY() * x + y,
            z);
    }


    @Override
    public Point2D inverseTransform(double x, double y)
            throws NonInvertibleTransformException {
        final double sx = getX();
        final double sy = getY();

        if (sy == 0.0) {
            final double mxy = -getX();

            return new Point2D(
                x + mxy * y - mxy * getPivotY(),
                y);
        }

        if (sx == 0.0) {
            final double myx = -getY();

            return new Point2D(
                x,
                myx * x + y - myx * getPivotX());
        }

        return super.inverseTransform(x, y);
    }

    @Override
    public Point3D inverseTransform(double x, double y, double z)
            throws NonInvertibleTransformException {
        final double sx = getX();
        final double sy = getY();

        if (sy == 0.0) {
            final double mxy = -getX();

            return new Point3D(
                x + mxy * y - mxy * getPivotY(),
                y,
                z);
        }

        if (sx == 0.0) {
            final double myx = -getY();

            return new Point3D(
                x,
                myx * x + y - myx * getPivotX(),
                z);
        }

        return super.inverseTransform(x, y, z);
    }

    @Override
    void inverseTransform2DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts)
            throws NonInvertibleTransformException {

        final double px = getPivotX();
        final double py = getPivotY();

        final double sx = getX();
        final double sy = getY();

        if (sy == 0.0) {
            final double xy = -sx;

            while (--numPts >= 0) {
                final double x = srcPts[srcOff++];
                final double y = srcPts[srcOff++];

                dstPts[dstOff++] = x + xy * y - xy * py;
                dstPts[dstOff++] = y;
            }
            return;
        }

        if (sx == 0.0) {
            final double yx = -sy;

            while (--numPts >= 0) {
                final double x = srcPts[srcOff++];
                final double y = srcPts[srcOff++];

                dstPts[dstOff++] = x;
                dstPts[dstOff++] = yx * x + y - yx * px;
            }
            return;
        }

        super.inverseTransform2DPointsImpl(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    @Override
    void inverseTransform3DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts)
            throws NonInvertibleTransformException{

        final double px = getPivotX();
        final double py = getPivotY();

        final double sx = getX();
        final double sy = getY();

        if (sy == 0.0) {
            final double xy = -sx;

            while (--numPts >= 0) {
                final double x = srcPts[srcOff++];
                final double y = srcPts[srcOff++];

                dstPts[dstOff++] = x + xy * y - xy * py;
                dstPts[dstOff++] = y;
                dstPts[dstOff++] = srcPts[srcOff++];
            }
            return;
        }

        if (sx == 0.0) {
            final double yx = -sy;

            while (--numPts >= 0) {
                final double x = srcPts[srcOff++];
                final double y = srcPts[srcOff++];

                dstPts[dstOff++] = x;
                dstPts[dstOff++] = yx * x + y - yx * px;
                dstPts[dstOff++] = srcPts[srcOff++];
            }
            return;
        }

        super.inverseTransform3DPointsImpl(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    @Override
    public Point2D inverseDeltaTransform(double x, double y)
            throws NonInvertibleTransformException {
        final double sx = getX();
        final double sy = getY();

        if (sy == 0.0) {
            return new Point2D(
                x - getX() * y,
                y);
        }

        if (sx == 0.0) {
            return new Point2D(
                x,
                -getY() * x + y);
        }

        return super.inverseDeltaTransform(x, y);
    }

    @Override
    public Point3D inverseDeltaTransform(double x, double y, double z)
            throws NonInvertibleTransformException {
        final double sx = getX();
        final double sy = getY();

        if (sy == 0.0) {
            return new Point3D(
                x - getX() * y,
                y,
                z);
        }

        if (sx == 0.0) {
            return new Point3D(
                x,
                -getY() * x + y,
                z);
        }

        return super.inverseDeltaTransform(x, y, z);
    }

    /* *************************************************************************
     *                                                                         *
     *                               Other API                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns a string representation of this {@code Shear} object.
     * @return a string representation of this {@code Shear} object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Shear [");

        sb.append("x=").append(getX());
        sb.append(", y=").append(getY());
        sb.append(", pivotX=").append(getPivotX());
        sb.append(", pivotY=").append(getPivotY());

        return sb.append("]").toString();
    }

    /* *************************************************************************
     *                                                                         *
     *                    Internal implementation stuff                        *
     *                                                                         *
     **************************************************************************/

    @Override
    void apply(final Affine3D trans) {
        if (getPivotX() != 0 || getPivotY() != 0) {
            trans.translate(getPivotX(), getPivotY());
            trans.shear(getX(), getY());
            trans.translate(-getPivotX(), -getPivotY());
        } else {
            trans.shear(getX(), getY());
        }
    }

    @Override
    BaseTransform derive(final BaseTransform trans) {
        return trans.deriveWithConcatenation(
                1.0, getY(),
                getX(), 1.0,
                getTx(), getTy());
    }

    @Override
    void validate() {
        getX(); getPivotX();
        getY(); getPivotY();
    }

    @Override
    void appendTo(Affine a) {
        a.appendShear(getX(), getY(), getPivotX(), getPivotY());
    }

    @Override
    void prependTo(Affine a) {
        a.prependShear(getX(), getY(), getPivotX(), getPivotY());
    }
}
