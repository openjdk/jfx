/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */

package javafx.scene.shape;


import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;

import com.sun.javafx.geom.QuadCurve2D;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGQuadCurve;
import com.sun.javafx.tk.Toolkit;


/**
 * The {@code Quadcurve} class defines a quadratic B&eacute;zier parametric curve
 * segment in (x,y) coordinate space. Drawing a curve that intersects both the
 * specified coordinates {@code (startX, startY)} and {@code (endX, enfY)},
 * using the specified point {@code (controlX, controlY)}
 * as B&eacute;zier control point.
 *
<PRE>
import javafx.scene.shape.*;

QuadCurve quad = new QuadCurve();
quad.setStartX(0.0f);
quad.setStartY(50.0f);
quad.setEndX(50.0f);
quad.setEndY(50.0f);
quad.setControlX(25.0f);
quad.setControlY(0.0f);
</PRE>
 */
public  class QuadCurve extends Shape {

    private final QuadCurve2D shape = new QuadCurve2D();

    /**
     * Creates an empty instance of QuadCurve.
     */
    public QuadCurve() {
    }

    /**
     * Creates a new instance of QuadCurve.
     * @param startX the X coordinate of the start point
     * @param startY the Y coordinate of the start point
     * @param controlX the X coordinate of the control point
     * @param controlY the Y coordinate of the control point
     * @param endX the X coordinate of the end point
     * @param endY the Y coordinate of the end point
     */
    public QuadCurve(double startX, double startY, double controlX, double controlY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setControlX(controlX);
        setControlY(controlY);
        setEndX(endX);
        setEndY(endY);
    }

    /**
     * Defines the X coordinate of the start point
     * of the quadratic curve segment.
     *
     * @defaultvalue 0.0
     */
    private DoubleProperty startX;



    public final void setStartX(double value) {
        startXProperty().set(value);
    }

    public final double getStartX() {
        return startX == null ? 0.0 : startX.get();
    }

    public final DoubleProperty startXProperty() {
        if (startX == null) {
            startX = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return QuadCurve.this;
                }

                @Override
                public String getName() {
                    return "startX";
                }
            };
        }
        return startX;
    }

    /**
     * Defines the Y coordinate of the start point
     * of the quadratic curve segment.
     *
     * @defaultvalue 0.0
     */
    private DoubleProperty startY;



    public final void setStartY(double value) {
        startYProperty().set(value);
    }

    public final double getStartY() {
        return startY == null ? 0.0 : startY.get();
    }

    public final DoubleProperty startYProperty() {
        if (startY == null) {
            startY = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return QuadCurve.this;
                }

                @Override
                public String getName() {
                    return "startY";
                }
            };
        }
        return startY;
    }

    /**
     * Defines the X coordinate of the control point
     * of the quadratic curve segment.
     *
     * @defaultvalue 0.0
     */
    private DoubleProperty controlX;



    public final void setControlX(double value) {
        controlXProperty().set(value);
    }

    public final double getControlX() {
        return controlX == null ? 0.0 : controlX.get();
    }

    public final DoubleProperty controlXProperty() {
        if (controlX == null) {
            controlX = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return QuadCurve.this;
                }

                @Override
                public String getName() {
                    return "controlX";
                }
            };
        }
        return controlX;
    }

    /**
     * Defines the Y coordinate of the control point
     * of the quadratic curve segment.
     *
     * @defaultvalue 0.0
     */
    private DoubleProperty controlY;


    public final void setControlY(double value) {
        controlYProperty().set(value);
    }

    public final double getControlY() {
        return controlY == null ? 0.0 : controlY.get();
    }

    public final DoubleProperty controlYProperty() {
        if (controlY == null) {
            controlY = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return QuadCurve.this;
                }

                @Override
                public String getName() {
                    return "controlY";
                }
            };
        }
        return controlY;
    }

    /**
     * Defines the X coordinate of the end point
     * of the quadratic curve segment.
     *
     * @defaultvalue 0.0
     */
    private DoubleProperty endX;


    public final void setEndX(double value) {
        endXProperty().set(value);
    }

    public final double getEndX() {
        return endX == null ? 0.0 : endX.get();
    }

    public final DoubleProperty endXProperty() {
        if (endX == null) {
            endX = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return QuadCurve.this;
                }

                @Override
                public String getName() {
                    return "endX";
                }
            };
        }
        return endX;
    }

    /**
     * Defines the Y coordinate of the end point
     * of the quadratic curve segment.
     *
     * @defaultvalue 0.0
     */
    private DoubleProperty endY;


    public final void setEndY(double value) {
        endYProperty().set(value);
    }

    public final double getEndY() {
        return endY == null ? 0.0 : endY.get();
    }

    public final DoubleProperty endYProperty() {
        if (endY == null) {
            endY = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return QuadCurve.this;
                }

                @Override
                public String getName() {
                    return "endY";
                }
            };
        }
        return endY;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGQuadCurve();
    }

    PGQuadCurve getPGQuadCurve() {
        return (PGQuadCurve)impl_getPGNode();
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
	public QuadCurve2D impl_configShape() {
        shape.x1 = (float)getStartX();
        shape.y1 = (float)getStartY();
        shape.ctrlx = (float)getControlX();
        shape.ctrly = (float)getControlY();
        shape.x2 = (float)getEndX();
        shape.y2 = (float)getEndY();
        return shape;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePG() {
        super.impl_updatePG();

        if (impl_isDirty(DirtyBits.NODE_GEOMETRY)) {
            PGQuadCurve peer = getPGQuadCurve();
            peer.updateQuadCurve((float)getStartX(),
                (float)getStartY(),
                (float)getEndX(),
                (float)getEndY(),
                (float)getControlX(),
                (float)getControlY());
        }
    }
}

