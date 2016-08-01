/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.shape;


import com.sun.javafx.geom.Path2D;
import com.sun.javafx.scene.shape.QuadCurveToHelper;
import com.sun.javafx.sg.prism.NGPath;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;


/**
 * Creates a curved path element, defined by two new points,
 * by drawing a Quadratic B&eacute;zier curve that intersects both the current coordinates
 * and the specified coordinates {@code (x, y)},
 * using the specified point {@code (controlX, controlY)}
 * as a B&eacute;zier control point.
 * All coordinates are specified in double precision.
 *
 * <p>For more information on path elements see the {@link Path} and
 * {@link PathElement} classes.
 *
 * <p>Example:
 *
<PRE>
import javafx.scene.shape.*;

Path path = new Path();

MoveTo moveTo = new MoveTo();
moveTo.setX(0.0f);
moveTo.setY(50.0f);

QuadCurveTo quadTo = new QuadCurveTo();
quadTo.setControlX(25.0f);
quadTo.setControlY(0.0f);
quadTo.setX(50.0f);
quadTo.setY(50.0f);

path.getElements().add(moveTo);
path.getElements().add(cubicTo);
</PRE>
 * @since JavaFX 2.0
 */
public  class QuadCurveTo extends PathElement {
    static {
        QuadCurveToHelper.setQuadCurveToAccessor(new QuadCurveToHelper.QuadCurveToAccessor() {
            @Override
            public void doAddTo(PathElement pathElement, Path2D path) {
                ((QuadCurveTo) pathElement).doAddTo(path);
            }
        });
    }

    /**
     * Creates an empty instance of QuadCurveTo.
     */
    public QuadCurveTo() {
        QuadCurveToHelper.initHelper(this);
    }

    /**
     * Creates a new instance of QuadCurveTo.
     * @param controlX the X coordinate of the quadratic control point
     * @param controlY the Y coordinate of the quadratic control point
     * @param x the X coordinate of the final end point
     * @param y the Y coordinate of the final end point
     */
    public QuadCurveTo(double controlX, double controlY, double x, double y) {
        setControlX(controlX);
        setControlY(controlY);
        setX(x);
        setY(y);
        QuadCurveToHelper.initHelper(this);
    }

    /**
     * Defines the X coordinate of the quadratic control point.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty controlX = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            u();
        }

        @Override
        public Object getBean() {
            return QuadCurveTo.this;
        }

        @Override
        public String getName() {
            return "controlX";
        }
    };


    public final void setControlX(double value) {
        controlX.set(value);
    }

    public final double getControlX() {
        return controlX.get();
    }

    public final DoubleProperty controlXProperty() {
        return controlX;
    }

    /**
     * Defines the Y coordinate of the quadratic control point.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty controlY = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            u();
        }

        @Override
        public Object getBean() {
            return QuadCurveTo.this;
        }

        @Override
        public String getName() {
            return "controlY";
        }
    };


    public final void setControlY(double value) {
        controlY.set(value);
    }

    public final double getControlY() {
        return controlY.get();
    }

    public final DoubleProperty controlYProperty() {
        return controlY;
    }

    /**
     * Defines the X coordinate of the final end point.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty x;

    public final void setX(double value) {
        if (x != null || value != 0.0) {
            xProperty().set(value);
        }
    }

    public final double getX() {
        return x == null ? 0.0 : x.get();
    }

    public final DoubleProperty xProperty() {
        if (x == null) {
            x = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return QuadCurveTo.this;
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
     * Defines the Y coordinate of the final end point.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty y;

    public final void setY(double value) {
        if (y != null || value != 0.0) {
            yProperty().set(value);
        }
    }

    public final double getY() {
        return y == null ? 0.0 : y.get();
    }

    public final DoubleProperty yProperty() {
        if (y == null) {
            y = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return QuadCurveTo.this;
                }

                @Override
                public String getName() {
                    return "y";
                }
            };
        }
        return y;
    }

    @Override
    void addTo(NGPath pgPath) {
        if (isAbsolute()) {
            pgPath.addQuadTo(
                    (float)getControlX(),
                    (float)getControlY(),
                    (float)getX(),
                    (float)getY());
        } else {
            final double dx = pgPath.getCurrentX();
            final double dy = pgPath.getCurrentY();
            pgPath.addQuadTo(
                    (float)(getControlX()+dx),
                    (float)(getControlY()+dy),
                    (float)(getX()+dx),
                    (float)(getY()+dy));
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doAddTo(Path2D path) {
        if (isAbsolute()) {
            path.quadTo(
                    (float)getControlX(),
                    (float)getControlY(),
                    (float)getX(),
                    (float)getY());
        } else {
            final double dx = path.getCurrentX();
            final double dy = path.getCurrentY();
            path.quadTo(
                    (float)(getControlX()+dx),
                    (float)(getControlY()+dy),
                    (float)(getX()+dx),
                    (float)(getY()+dy));
        }
    }

    /**
     * Returns a string representation of this {@code CubicCurveTo} object.
     * @return a string representation of this {@code CubicCurveTo} object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CubicCurveTo[");

        sb.append("x=").append(getX());
        sb.append(", y=").append(getY());
        sb.append(", controlX=").append(getControlX());
        sb.append(", controlY=").append(getControlY());

        return sb.append("]").toString();
    }
}

