/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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


/**
 * This class represents an {@code Affine} object that rotates coordinates
 * around an anchor point. This operation is equivalent to translating the
 * coordinates so that the anchor point is at the origin (S1), then rotating them
 * about the new origin (S2), and finally translating so that the
 * intermediate origin is restored to the coordinates of the original
 * anchor point (S3).
 * <p/>
 * For example, the matrix representing the returned transform of
 *    new Rotate (theta, x, y, z) around the Z-axis
 *
 * is :
 * <pre>
 *              [   cos(theta)    -sin(theta)   0    x-x*cos+y*sin  ]
 *              [   sin(theta)     cos(theta)   0    y-x*sin-y*cos  ]
 *              [      0               0        1          z        ]
 * </pre>
 * <p>
 * For example, to rotate a text 30 degrees around the Z-axis at
 * anchor point of (50,30):
 * <pre><code>
 * Text text = new Text("This is a test");
 * text.setX(10);
 * text.setY(50);
 * text.setFont(new Font(20));
 *
 * text.getTransforms().add(new Rotate(30, 50, 30));
 * </code></pre>
 * </p>
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
     * Creates a default Rotate transform (identity).
     */
    public Rotate() {
    }

    /**
     * Creates a two-dimensional Rotate transform.
     * @param angle the angle of rotation measured in degrees
     */
    public Rotate(double angle) {
        setAngle(angle);
    }

    /**
     * Creates a three-dimensional Rotate transform.
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

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_apply(final Affine3D trans) {
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
}
