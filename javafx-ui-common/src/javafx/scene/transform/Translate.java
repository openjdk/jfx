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
 * This class represents an {@code Affine} object that translates coordinates
 * by the specified factors. The matrix representing the translating
 * transformation is as follows:
 * <pre>
 *              [   1    0    0    x  ]
 *              [   0    1    0    y  ]
 *              [   0    0    1    z  ]
 * </pre>
 */

public class Translate extends Transform {
    
    /**
     * Creates a default Translate (identity).
     */
    public Translate() {
    }

    /**
     * Creates a two-dimensional Translate.
     * @param x the distance by which coordinates are translated in the
     * X axis direction
     * @param y the distance by which coordinates are translated in the
     * Y axis direction
     */
    public Translate(double x, double y) {
        setX(x);
        setY(y);
    }

    /**
     * Creates a three-dimensional Translate.
     * @param x the distance by which coordinates are translated in the
     * X axis direction
     * @param y the distance by which coordinates are translated in the
     * Y axis direction
     * @param z the distance by which coordinates are translated in the
     * Z axis direction
     */
    public Translate(double x, double y, double z) {
        this(x, y);
        setZ(z);
    }

    /**
     * Defines the distance by which coordinates are translated in the
     * X axis direction
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
                    return Translate.this;
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
     * Defines the distance by which coordinates are translated in the
     * Y axis direction
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
                    return Translate.this;
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
     * Defines the distance by which coordinates are translated in the
     * Z axis direction
     */
    private DoubleProperty z;


    public final void setZ(double value) {
        zProperty().set(value);
    }

    public final double getZ() {
        return z == null ? 0.0 : z.get();
    }

    public final DoubleProperty zProperty() {
        if (z == null) {
            z = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Translate.this;
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
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_apply(final Affine3D trans) {
        trans.translate(getX(), getY(), getZ());
    }

    /**
     * Returns a string representation of this {@code Translate} object.
     * @return a string representation of this {@code Translate} object.
     */ 
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Translate [");

        sb.append("x=").append(getX());
        sb.append(", y=").append(getY());
        sb.append(", z=").append(getZ());

        return sb.append("]").toString();
    }
}
