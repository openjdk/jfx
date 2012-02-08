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
 * This class represents an {@code Affine} object that shears coordinates
 * by the specified multipliers. The matrix representing the shearing
 * transformation is as follows:
 * <pre>
 *      [   1   x   0   0   ]
 *      [   y   1   0   0   ]
 *      [   0   0   1   0   ]
 * </pre>
 *
 * <p>
 * For example:
 * <pre><code>
 * Text text = new Text("Using Shear for pseudo-italic font");
 * text.setX(20);
 * text.setY(50);
 * text.setFont(new Font(20));
 *
 * text.getTransforms().add(new Shear(-0.35, 0));
 * </code></pre>
 * </p>
 *
 * @profile common
 */
public class Shear extends Transform {
    
    /**
     * Creates a default Shear (identity).
     */
    public Shear() {
    }

    /**
     * Creates a new instance of Shear.
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
     * @defaultvalue 0.0
     * @profile common
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
     * @defaultvalue 0.0
     * @profile common
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

    
    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_apply(final Affine3D trans) {
        if (getPivotX() != 0 || getPivotY() != 0) {
            trans.translate(getPivotX(), getPivotY());
            trans.shear(getX(), getY());
            trans.translate(-getPivotX(), -getPivotY());
        } else {
            trans.shear(getX(), getY());
        }
    }

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
}
