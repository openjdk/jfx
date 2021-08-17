/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Line2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.shape.LineHelper;
import com.sun.javafx.sg.prism.NGLine;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGShape;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;


/**
 * This Line represents a line segment in {@code (x,y)}
 * coordinate space. Example:
 *
<PRE>
import javafx.scene.shape.*;

Line line = new Line();
line.setStartX(0.0f);
line.setStartY(0.0f);
line.setEndX(100.0f);
line.setEndY(100.0f);
}
</PRE>
 * @since JavaFX 2.0
 */
public class Line extends Shape {
    static {
        LineHelper.setLineAccessor(new LineHelper.LineAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((Line) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((Line) node).doUpdatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((Line) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public Paint doCssGetFillInitialValue(Shape shape) {
                return ((Line) shape).doCssGetFillInitialValue();
            }

            @Override
            public Paint doCssGetStrokeInitialValue(Shape shape) {
                return ((Line) shape).doCssGetStrokeInitialValue();
            }

            @Override
            public com.sun.javafx.geom.Shape doConfigShape(Shape shape) {
                return ((Line) shape).doConfigShape();
            }
        });
    }

    private final Line2D shape = new Line2D();

    {
        // To initialize the class helper at the begining each constructor of this class
        LineHelper.initHelper(this);

        // overriding default values for fill and stroke
        // Set through CSS property so that it appears to be a UA style rather
        // that a USER style so that fill and stroke can still be set from CSS.
        ((StyleableProperty)fillProperty()).applyStyle(null, null);
        ((StyleableProperty)strokeProperty()).applyStyle(null, Color.BLACK);
    }

    /**
     * Creates an empty instance of Line.
     */
    public Line() {
    }

    /**
     * Creates a new instance of Line.
     * @param startX the horizontal coordinate of the start point of the line segment
     * @param startY the vertical coordinate of the start point of the line segment
     * @param endX the horizontal coordinate of the end point of the line segment
     * @param endY the vertical coordinate of the end point of the line segment
     */
    public Line(double startX, double startY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);
    }

    /**
     * The X coordinate of the start point of the line segment.
     *
     * @defaultValue 0.0
     */
    private final DoubleProperty startX = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    NodeHelper.markDirty(Line.this, DirtyBits.NODE_GEOMETRY);
                    NodeHelper.geomChanged(Line.this);
                }

                @Override
                public Object getBean() {
                    return Line.this;
                }

                @Override
                public String getName() {
                    return "startX";
                }
            };


    public final void setStartX(double value) {
        startX.set(value);
    }

    public final double getStartX() {
        return startX.get();
    }

    public final DoubleProperty startXProperty() {
        return startX;
    }

    /**
     * The Y coordinate of the start point of the line segment.
     *
     * @defaultValue 0.0
     */
    private final DoubleProperty startY = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    NodeHelper.markDirty(Line.this, DirtyBits.NODE_GEOMETRY);
                    NodeHelper.geomChanged(Line.this);
                }

                @Override
                public Object getBean() {
                    return Line.this;
                }

                @Override
                public String getName() {
                    return "startY";
                }
            };


    public final void setStartY(double value) {
        startY.set(value);
    }

    public final double getStartY() {
        return startY.get();
    }

    public final DoubleProperty startYProperty() {
        return startY;
    }

    /**
     * The X coordinate of the end point of the line segment.
     *
     * @defaultValue 0.0
     */
    private final DoubleProperty endX = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            NodeHelper.markDirty(Line.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(Line.this);
        }

        @Override
        public Object getBean() {
            return Line.this;
        }

        @Override
        public String getName() {
            return "endX";
        }
    };



    public final void setEndX(double value) {
        endX.set(value);
    }

    public final double getEndX() {
        return endX.get();
    }

    public final DoubleProperty endXProperty() {
        return endX;
    }

    /**
     * The Y coordinate of the end point of the line segment.
     *
     * @defaultValue 0.0
     */
    private final DoubleProperty endY = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            NodeHelper.markDirty(Line.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(Line.this);
        }

        @Override
        public Object getBean() {
            return Line.this;
        }

        @Override
        public String getName() {
            return "endY";
        }
    };

    public final void setEndY(double value) {
        endY.set(value);
    }

    public final double getEndY() {
        return endY.get();
    }

    public final DoubleProperty endYProperty() {
        return endY;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGLine();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {

        // Since line's only draw with strokes, if the mode is FILL or EMPTY
        // then we simply return empty bounds
        if (getMode() == NGShape.Mode.FILL || getMode() == NGShape.Mode.EMPTY ||
            getStrokeType() == StrokeType.INSIDE)
        {
            return bounds.makeEmpty();
        }

        double x1 = getStartX();
        double x2 = getEndX();
        double y1 = getStartY();
        double y2 = getEndY();
        // Get the draw stroke, and figure out the bounds based on the stroke.
        double wpad = getStrokeWidth();
        if (getStrokeType() == StrokeType.CENTERED) {
            wpad /= 2.0f;
        }
        // fast path the case of AffineTransform being TRANSLATE or identity
        if (tx.isTranslateOrIdentity()) {
            final double xpad;
            final double ypad;
            wpad = Math.max(wpad, 0.5f);
            if (tx.getType() == BaseTransform.TYPE_TRANSLATION) {
                final double ddx = tx.getMxt();
                final double ddy = tx.getMyt();
                x1 += ddx;
                y1 += ddy;
                x2 += ddx;
                y2 += ddy;
            }
            if (y1 == y2 && x1 != x2) {
                ypad = wpad;
                xpad = (getStrokeLineCap() == StrokeLineCap.BUTT) ? 0.0f : wpad;
            } else if (x1 == x2 && y1 != y2) {
                xpad = wpad;
                ypad = (getStrokeLineCap() == StrokeLineCap.BUTT) ? 0.0f : wpad;
            } else {
                if (getStrokeLineCap() == StrokeLineCap.SQUARE) {
                    wpad *= Math.sqrt(2);
                }
                xpad = ypad = wpad;
            }
            if (x1 > x2) { final double t = x1; x1 = x2; x2 = t; }
            if (y1 > y2) { final double t = y1; y1 = y2; y2 = t; }

            x1 -= xpad;
            y1 -= ypad;
            x2 += xpad;
            y2 += ypad;
            bounds = bounds.deriveWithNewBounds((float)x1, (float)y1, 0.0f,
                    (float)x2, (float)y2, 0.0f);
            return bounds;
        }

        double dx = x2 - x1;
        double dy = y2 - y1;
        final double len = Math.sqrt(dx * dx + dy * dy);
        if (len == 0.0f) {
            dx = wpad;
            dy = 0.0f;
        } else {
            dx = wpad * dx / len;
            dy = wpad * dy / len;
        }
        final double ecx;
        final double ecy;
        if (getStrokeLineCap() != StrokeLineCap.BUTT) {
            ecx = dx;
            ecy = dy;
        } else {
            ecx = ecy = 0.0f;
        }
        final double corners[] = new double[] {
            x1-dy-ecx, y1+dx-ecy,
            x1+dy-ecx, y1-dx-ecy,
            x2+dy+ecx, y2-dx+ecy,
            x2-dy+ecx, y2+dx+ecy };
        tx.transform(corners, 0, corners, 0, 4);
        x1 = Math.min(Math.min(corners[0], corners[2]),
                             Math.min(corners[4], corners[6]));
        y1 = Math.min(Math.min(corners[1], corners[3]),
                             Math.min(corners[5], corners[7]));
        x2 = Math.max(Math.max(corners[0], corners[2]),
                             Math.max(corners[4], corners[6]));
        y2 = Math.max(Math.max(corners[1], corners[3]),
                             Math.max(corners[5], corners[7]));
        x1 -= 0.5f;
        y1 -= 0.5f;
        x2 += 0.5f;
        y2 += 0.5f;
        bounds = bounds.deriveWithNewBounds((float)x1, (float)y1, 0.0f,
                (float)x2, (float)y2, 0.0f);
        return bounds;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private Line2D doConfigShape() {
        shape.setLine((float)getStartX(), (float)getStartY(), (float)getEndX(), (float)getEndY());
        return shape;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        if (NodeHelper.isDirty(this, DirtyBits.NODE_GEOMETRY)) {
            NGLine peer = NodeHelper.getPeer(this);
            peer.updateLine((float)getStartX(),
                (float)getStartY(),
                (float)getEndX(),
                (float)getEndY());
        }
    }

    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    /*
     * Some sub-class of Shape, such as {@link Line}, override the
     * default value for the {@link Shape#fill} property. This allows
     * CSS to get the correct initial value.
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private Paint doCssGetFillInitialValue() {
        return null;
    }

    /**
     * Some sub-class of Shape, such as {@link Line}, override the
     * default value for the {@link Shape#stroke} property. This allows
     * CSS to get the correct initial value.
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private Paint doCssGetStrokeInitialValue() {
        return Color.BLACK;
    }

    /**
     * Returns a string representation of this {@code Line} object.
     * @return a string representation of this {@code Line} object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Line[");

        String id = getId();
        if (id != null) {
            sb.append("id=").append(id).append(", ");
        }

        sb.append("startX=").append(getStartX());
        sb.append(", startY=").append(getStartY());
        sb.append(", endX=").append(getEndX());
        sb.append(", endY=").append(getEndY());

        Paint stroke = getStroke();
        if (stroke != null) {
            sb.append(", stroke=").append(stroke);
            sb.append(", strokeWidth=").append(getStrokeWidth());
        }

        return sb.append("]").toString();
    }
}

