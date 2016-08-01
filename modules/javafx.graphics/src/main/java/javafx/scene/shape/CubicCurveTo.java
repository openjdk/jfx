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
import com.sun.javafx.scene.shape.CubicCurveToHelper;
import com.sun.javafx.sg.prism.NGPath;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;


/**
 * Creates a curved path element, defined by three new points,
 * by drawing a Cubic B&eacute;zier curve that intersects both the current coordinates
 * and the specified coordinates {@code (x,y)}, using the
 * specified points {@code (controlX1,controlY1)} and {@code (controlX2,controlY2)}
 * as B&eacute;zier control points. All coordinates are specified in double precision.
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
moveTo.setY(0.0f);

CubicCurveTo cubicTo = new CubicCurveTo();
cubicTo.setControlX1(0.0f);
cubicTo.setControlY1(0.0f);
cubicTo.setControlX2(100.0f);
cubicTo.setControlY2(100.0f);
cubicTo.setX(100.0f);
cubicTo.setY(50.0f);

path.getElements().add(moveTo);
path.getElements().add(cubicTo);
</PRE>
 * @since JavaFX 2.0
 */
public class CubicCurveTo extends PathElement {
    static {
        CubicCurveToHelper.setCubicCurveToAccessor(new CubicCurveToHelper.CubicCurveToAccessor() {
            @Override
            public void doAddTo(PathElement pathElement, Path2D path) {
                ((CubicCurveTo) pathElement).doAddTo(path);
            }
        });
    }

    /**
     * Creates an empty instance of CubicCurveTo.
     */
    public CubicCurveTo() {
        CubicCurveToHelper.initHelper(this);
    }

    /**
     * Creates a new instance of CubicCurveTo.
     * @param controlX1 the X coordinate of the first B&eacute;zier control point
     * @param controlY1 the Y coordinate of the first B&eacute;zier control point
     * @param controlX2 the X coordinate of the second B&eacute;zier control point
     * @param controlY2 the Y coordinate of the second B&eacute;zier control point
     * @param x the X coordinate of the final end point
     * @param y the Y coordinate of the final end point
     */
    public CubicCurveTo(double controlX1, double controlY1, double controlX2,
        double controlY2, double x, double y)
    {
        setControlX1(controlX1);
        setControlY1(controlY1);
        setControlX2(controlX2);
        setControlY2(controlY2);
        setX(x);
        setY(y);
        CubicCurveToHelper.initHelper(this);
    }

    /**
     * Defines the X coordinate of the first B&eacute;zier control point.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty controlX1;


    public final void setControlX1(double value) {
        if (controlX1 != null || value != 0.0) {
            controlX1Property().set(value);
        }
    }

    public final double getControlX1() {
        return controlX1 == null ? 0.0 : controlX1.get();
    }

    public final DoubleProperty controlX1Property() {
        if (controlX1 == null) {
            controlX1 = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return CubicCurveTo.this;
                }

                @Override
                public String getName() {
                    return "controlX1";
                }
            };
        }
        return controlX1;
    }

    /**
     * Defines the Y coordinate of the first B&eacute;zier control point.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty controlY1;


    public final void setControlY1(double value) {
        if (controlY1 != null || value != 0.0) {
            controlY1Property().set(value);
        }
    }

    public final double getControlY1() {
        return controlY1 == null ? 0.0 : controlY1.get();
    }

    public final DoubleProperty controlY1Property() {
        if (controlY1 == null) {
            controlY1 = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return CubicCurveTo.this;
                }

                @Override
                public String getName() {
                    return "controlY1";
                }
            };
        }
        return controlY1;
    }

    /**
     * Defines the X coordinate of the second B&eacute;zier control point.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty controlX2;


    public final void setControlX2(double value) {
        if (controlX2 != null || value != 0.0) {
            controlX2Property().set(value);
        }
    }

    public final double getControlX2() {
        return controlX2 == null ? 0.0 : controlX2.get();
    }

    public final DoubleProperty controlX2Property() {
        if (controlX2 == null) {
            controlX2 = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return CubicCurveTo.this;
                }

                @Override
                public String getName() {
                    return "controlX2";
                }
            };
        }
        return controlX2;
    }

    /**
     * Defines the Y coordinate of the second B&eacute;zier control point.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty controlY2;


    public final void setControlY2(double value) {
        if (controlY2 != null || value != 0.0) {
            controlY2Property().set(value);
        }
    }

    public final double getControlY2() {
        return controlY2 == null ? 0.0 : controlY2.get();
    }

    public final DoubleProperty controlY2Property() {
        if (controlY2 == null) {
            controlY2 = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return CubicCurveTo.this;
                }

                @Override
                public String getName() {
                    return "controlY2";
                }
            };
        }
        return controlY2;
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
                    return CubicCurveTo.this;
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
                    return CubicCurveTo.this;
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
     * Adds the curved path element to the specified path.
     */
    @Override
    void addTo(NGPath pgPath) {
        if (isAbsolute()) {
            pgPath.addCubicTo((float)getControlX1(), (float)getControlY1(),
                              (float)getControlX2(), (float)getControlY2(),
                              (float)getX(), (float)getY());
        } else {
            final double dx = pgPath.getCurrentX();
            final double dy = pgPath.getCurrentY();
            pgPath.addCubicTo((float)(getControlX1()+dx), (float)(getControlY1()+dy),
                              (float)(getControlX2()+dx), (float)(getControlY2()+dy),
                              (float)(getX()+dx), (float)(getY()+dy));
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doAddTo(Path2D path) {
        if (isAbsolute()) {
            path.curveTo((float)getControlX1(), (float)getControlY1(),
                         (float)getControlX2(), (float)getControlY2(),
                         (float)getX(), (float)getY());
        } else {
            final double dx = path.getCurrentX();
            final double dy = path.getCurrentY();
            path.curveTo((float)(getControlX1()+dx), (float)(getControlY1()+dy),
                         (float)(getControlX2()+dx), (float)(getControlY2()+dy),
                         (float)(getX()+dx), (float)(getY()+dy));
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
        sb.append(", controlX1=").append(getControlX1());
        sb.append(", controlY1=").append(getControlY1());
        sb.append(", controlX2=").append(getControlX2());
        sb.append(", controlY2=").append(getControlY2());

        return sb.append("]").toString();
    }
}

