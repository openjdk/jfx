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

import com.sun.javafx.geom.transform.Affine3D;


/**
 * This class represents an {@code Affine} object that scales coordinates
 * by the specified factors. The matrix representing the scaling transformation
 * is as follows:
 * <pre>
 *              [ x    0   0   0 ]
 *              [ 0    y   0   0 ]
 *              [ 0    0   z   0 ]
 * </pre>
 */
public class Scale extends Transform {
    /**
     * Creates a default Scale (identity).
     */
    public Scale() {
    }

    /**
     * Creates a two-dimensional Scale.
     * @param x the factor by which coordinates are scaled along the X axis 
     * @param y the factor by which coordinates are scaled along the Y axis 
     */
    public Scale(double x, double y) {
        setX(x);
        setY(y);
    }

    /**
     * Creates a two-dimensional Scale with pivot.
     * @param x the factor by which coordinates are scaled along the X axis 
     * @param y the factor by which coordinates are scaled along the Y axis 
     * @param pivotX the X coordinate about which point the scale occurs
     * @param pivotY the Y coordinate about which point the scale occurs
     */
    public Scale(double x, double y, double pivotX, double pivotY) {
        this(x, y);
        setPivotX(pivotX);
        setPivotY(pivotY);
    }

    /**
     * Creates a three-dimensional Scale.
     * @param x the factor by which coordinates are scaled along the X axis 
     * @param y the factor by which coordinates are scaled along the Y axis 
     * @param z the factor by which coordinates are scaled along the Z axis 
     */
    public Scale(double x, double y, double z) {
        this(x, y);
        setZ(z);
    }

    /**
     * Creates a three-dimensional Scale with pivot.
     * @param x the factor by which coordinates are scaled along the X axis 
     * @param y the factor by which coordinates are scaled along the Y axis 
     * @param z the factor by which coordinates are scaled along the Z axis 
     * @param pivotX the X coordinate about which point the scale occurs
     * @param pivotY the Y coordinate about which point the scale occurs
     * @param pivotZ the Z coordinate about which point the scale occurs
     */
    public Scale(double x, double y, double z, double pivotX, double pivotY, double pivotZ) {
        this(x, y, pivotX, pivotY);
        setZ(z);
        setPivotZ(pivotZ);
    }

    /**
     * Defines the factor by which coordinates are scaled
     * along the X axis direction. The default value is {@code 1.0}.
     */
    private DoubleProperty x;


    public final void setX(double value) {
        xProperty().set(value);
    }

    public final double getX() {
        return x == null ? 1.0F : x.get();
    }

    public final DoubleProperty xProperty() {
        if (x == null) {
            x = new DoublePropertyBase(1.0F) {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Scale.this;
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
     * Defines the factor by which coordinates are scaled
     * along the Y axis direction. The default value is {@code 1.0}.
     */
    private DoubleProperty y;


    public final void setY(double value) {
        yProperty().set(value);
    }

    public final double getY() {
        return y == null ? 1.0F : y.get();
    }

    public final DoubleProperty yProperty() {
        if (y == null) {
            y = new DoublePropertyBase(1.0F) {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Scale.this;
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
     * Defines the factor by which coordinates are scaled
     * along the Z axis direction. The default value is {@code 1.0}.
     */
    private DoubleProperty z;


    public final void setZ(double value) {
        zProperty().set(value);
    }

    public final double getZ() {
        return z == null ? 1.0F : z.get();
    }

    public final DoubleProperty zProperty() {
        if (z == null) {
            z = new DoublePropertyBase(1.0F) {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Scale.this;
                }

                @Override
                public String getName() {
                    return "z";
                }
            };
        }
        return z;
    }

    /**
     * Defines the X coordinate about which point the scale occurs.
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
                    return Scale.this;
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
     * Defines the Y coordinate about which point the scale occurs.
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
                    return Scale.this;
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
     * Defines the Z coordinate about which point the scale occurs.
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
                    return Scale.this;
                }

                @Override
                public String getName() {
                    return "pivotZ";
                }
            };
        }
        return pivotZ;
    }

    @Override
    public double getMxx() {
        return getX();
    }

    @Override
    public double getMyy() {
        return getY();
    }

    @Override
    public double getMzz() {
        return getZ();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_apply(final Affine3D trans) {
        if (getPivotX() != 0 || getPivotY() != 0 || getPivotZ() != 0) {
            trans.translate(getPivotX(), getPivotY(), getPivotZ());
            trans.scale(getX(), getY(), getZ());
            trans.translate(-getPivotX(), -getPivotY(), -getPivotZ());
        } else {
            trans.scale(getX(), getY(), getZ());
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public Transform impl_copy() {
        return new Scale(getX(), getY(), getZ(),
                         getPivotX(), getPivotY(), getPivotZ());
    }

    /**
     * Returns a string representation of this {@code Scale} object.
     * @return a string representation of this {@code Scale} object.
     */ 
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Scale [");

        sb.append("x=").append(getX());
        sb.append(", y=").append(getY());
        sb.append(", z=").append(getZ());
        sb.append(", pivotX=").append(getPivotX());
        sb.append(", pivotY=").append(getPivotY());
        sb.append(", pivotZ=").append(getPivotZ());

        return sb.append("]").toString();
    }
}
