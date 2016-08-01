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


import com.sun.javafx.geom.QuadCurve2D;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.shape.QuadCurveHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGQuadCurve;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.scene.Node;
import javafx.scene.paint.Paint;


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
 * @since JavaFX 2.0
 */
public  class QuadCurve extends Shape {
    static {
        QuadCurveHelper.setQuadCurveAccessor(new QuadCurveHelper.QuadCurveAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((QuadCurve) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((QuadCurve) node).doUpdatePeer();
            }

            @Override
            public com.sun.javafx.geom.Shape doConfigShape(Shape shape) {
                return ((QuadCurve) shape).doConfigShape();
            }
        });
    }

    private final QuadCurve2D shape = new QuadCurve2D();

    {
        // To initialize the class helper at the begining each constructor of this class
        QuadCurveHelper.initHelper(this);
    }

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
     * @defaultValue 0.0
     */
    private DoubleProperty startX;

    public final void setStartX(double value) {
        if (startX != null || value != 0.0) {
            startXProperty().set(value);
        }
    }

    public final double getStartX() {
        return startX == null ? 0.0 : startX.get();
    }

    public final DoubleProperty startXProperty() {
        if (startX == null) {
            startX = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    NodeHelper.markDirty(QuadCurve.this, DirtyBits.NODE_GEOMETRY);
                    NodeHelper.geomChanged(QuadCurve.this);
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
     * @defaultValue 0.0
     */
    private DoubleProperty startY;

    public final void setStartY(double value) {
        if (startY != null || value != 0.0) {
            startYProperty().set(value);
        }
    }

    public final double getStartY() {
        return startY == null ? 0.0 : startY.get();
    }

    public final DoubleProperty startYProperty() {
        if (startY == null) {
            startY = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    NodeHelper.markDirty(QuadCurve.this, DirtyBits.NODE_GEOMETRY);
                    NodeHelper.geomChanged(QuadCurve.this);
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
     * @defaultValue 0.0
     */
    private DoubleProperty controlX = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            NodeHelper.markDirty(QuadCurve.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(QuadCurve.this);
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
     * Defines the Y coordinate of the control point
     * of the quadratic curve segment.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty controlY = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            NodeHelper.markDirty(QuadCurve.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(QuadCurve.this);
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
     * Defines the X coordinate of the end point
     * of the quadratic curve segment.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty endX;


    public final void setEndX(double value) {
        if (endX != null || value != 0.0) {
            endXProperty().set(value);
        }
    }

    public final double getEndX() {
        return endX == null ? 0.0 : endX.get();
    }

    public final DoubleProperty endXProperty() {
        if (endX == null) {
            endX = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            NodeHelper.markDirty(QuadCurve.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(QuadCurve.this);
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
     * @defaultValue 0.0
     */
    private DoubleProperty endY;

    public final void setEndY(double value) {
        if (endY != null || value != 0.0) {
            endYProperty().set(value);
        }
    }

    public final double getEndY() {
        return endY == null ? 0.0 : endY.get();
    }

    public final DoubleProperty endYProperty() {
        if (endY == null) {
            endY = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            NodeHelper.markDirty(QuadCurve.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(QuadCurve.this);
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

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGQuadCurve();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private QuadCurve2D doConfigShape() {
        shape.x1 = (float)getStartX();
        shape.y1 = (float)getStartY();
        shape.ctrlx = (float)getControlX();
        shape.ctrly = (float)getControlY();
        shape.x2 = (float)getEndX();
        shape.y2 = (float)getEndY();
        return shape;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        if (NodeHelper.isDirty(this, DirtyBits.NODE_GEOMETRY)) {
            final NGQuadCurve peer = NodeHelper.getPeer(this);
            peer.updateQuadCurve((float)getStartX(),
                (float)getStartY(),
                (float)getEndX(),
                (float)getEndY(),
                (float)getControlX(),
                (float)getControlY());
        }
    }

    /**
     * Returns a string representation of this {@code QuadCurve} object.
     * @return a string representation of this {@code QuadCurve} object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("QuadCurve[");

        String id = getId();
        if (id != null) {
            sb.append("id=").append(id).append(", ");
        }

        sb.append("startX=").append(getStartX());
        sb.append(", startY=").append(getStartY());
        sb.append(", controlX=").append(getControlX());
        sb.append(", controlY=").append(getControlY());
        sb.append(", endX=").append(getEndX());
        sb.append(", endY=").append(getEndY());

        sb.append(", fill=").append(getFill());

        Paint stroke = getStroke();
        if (stroke != null) {
            sb.append(", stroke=").append(stroke);
            sb.append(", strokeWidth=").append(getStrokeWidth());
        }

        return sb.append("]").toString();
    }
}

