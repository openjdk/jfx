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

import com.sun.javafx.geom.Arc2D;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.shape.ArcHelper;
import com.sun.javafx.sg.prism.NGArc;
import com.sun.javafx.sg.prism.NGNode;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Node;
import javafx.scene.paint.Paint;


/**
 * The {@code Arc} class represents a 2D arc object, defined by a center point,
 * start angle (in degrees), angular extent (length of the arc in degrees),
 * and an arc type ({@link ArcType#OPEN}, {@link ArcType#CHORD},
 * or {@link ArcType#ROUND}).
 *
 * <p>Example usage: the following code creates an Arc which is centered around
 * 50,50, has a radius of 25 and extends from the angle 45 to the angle 315
 * (270 degrees long), and is round.
 *
<PRE>
import javafx.scene.shape.*;

Arc arc = new Arc();
arc.setCenterX(50.0f);
arc.setCenterY(50.0f);
arc.setRadiusX(25.0f);
arc.setRadiusY(25.0f);
arc.setStartAngle(45.0f);
arc.setLength(270.0f);
arc.setType(ArcType.ROUND);
</PRE>
 * @since JavaFX 2.0
 */
public class Arc extends Shape {
    static {
        ArcHelper.setArcAccessor(new ArcHelper.ArcAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((Arc) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((Arc) node).doUpdatePeer();
            }

            @Override
            public com.sun.javafx.geom.Shape doConfigShape(Shape shape) {
                return ((Arc) shape).doConfigShape();
            }
        });
    }

    private final Arc2D shape = new Arc2D();

    {
        // To initialize the class helper at the begining each constructor of this class
        ArcHelper.initHelper(this);
    }

    /**
     * Creates an empty instance of Arc.
     */
    public Arc() {
    }

    /**
     * Creates a new instance of Arc.
     * @param centerX the X coordinate of the center point of the arc
     * @param centerY the Y coordinate of the center point of the arc
     * @param radiusX the overall width (horizontal radius) of the full ellipse
     * of which this arc is a partial section
     * @param radiusY the overall height (vertical radius) of the full ellipse
     * of which this arc is a partial section
     * @param startAngle the starting angle of the arc in degrees
     * @param length the angular extent of the arc in degrees
     */
    public Arc(double centerX, double centerY, double radiusX, double radiusY, double startAngle, double length) {
        setCenterX(centerX);
        setCenterY(centerY);
        setRadiusX(radiusX);
        setRadiusY(radiusY);
        setStartAngle(startAngle);
        setLength(length);
    }

    /**
     * Defines the X coordinate of the center point of the arc.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty centerX;

    public final void setCenterX(double value) {
        if (centerX != null || value != 0.0) {
            centerXProperty().set(value);
        }
    }

    public final double getCenterX() {
        return centerX == null ? 0.0 : centerX.get();
    }

    public final DoubleProperty centerXProperty() {
        if (centerX == null) {
            centerX = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    NodeHelper.markDirty(Arc.this, DirtyBits.NODE_GEOMETRY);
                    NodeHelper.geomChanged(Arc.this);
                }

                @Override
                public Object getBean() {
                    return Arc.this;
                }

                @Override
                public String getName() {
                    return "centerX";
                }
            };
        }
        return centerX;
    }

    /**
     * Defines the Y coordinate of the center point of the arc.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty centerY;

    public final void setCenterY(double value) {
        if (centerY != null || value != 0.0) {
            centerYProperty().set(value);
        }
    }

    public final double getCenterY() {
        return centerY == null ? 0.0 : centerY.get();
    }

    public final DoubleProperty centerYProperty() {
        if (centerY == null) {
            centerY = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    NodeHelper.markDirty(Arc.this, DirtyBits.NODE_GEOMETRY);
                    NodeHelper.geomChanged(Arc.this);
                }

                @Override
                public Object getBean() {
                    return Arc.this;
                }

                @Override
                public String getName() {
                    return "centerY";
                }
            };
        }
        return centerY;
    }

    /**
     * Defines the overall width (horizontal radius) of the full ellipse
     * of which this arc is a partial section.
     *
     * @defaultValue 0.0
     */
    private final DoubleProperty radiusX = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            NodeHelper.markDirty(Arc.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(Arc.this);
        }

        @Override
        public Object getBean() {
            return Arc.this;
        }

        @Override
        public String getName() {
            return "radiusX";
        }
    };

    public final void setRadiusX(double value) {
        radiusX.set(value);
    }

    public final double getRadiusX() {
        return radiusX.get();
    }

    public final DoubleProperty radiusXProperty() {
        return radiusX;
    }

    /**
     * Defines the overall height (vertical radius) of the full ellipse
     * of which this arc is a partial section.
     *
     * @defaultValue 0.0
     */
    private final DoubleProperty radiusY = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            NodeHelper.markDirty(Arc.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(Arc.this);
        }

        @Override
        public Object getBean() {
            return Arc.this;
        }

        @Override
        public String getName() {
            return "radiusY";
        }
    };

    public final void setRadiusY(double value) {
        radiusY.set(value);
    }

    public final double getRadiusY() {
        return radiusY.get();
    }

    public final DoubleProperty radiusYProperty() {
        return radiusY;
    }

    /**
     * Defines the starting angle of the arc in degrees.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty startAngle;

    public final void setStartAngle(double value) {
        if (startAngle != null || value != 0.0) {
            startAngleProperty().set(value);
        }
    }

    public final double getStartAngle() {
        return startAngle == null ? 0.0 : startAngle.get();
    }

    public final DoubleProperty startAngleProperty() {
        if (startAngle == null) {
            startAngle = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    NodeHelper.markDirty(Arc.this, DirtyBits.NODE_GEOMETRY);
                    NodeHelper.geomChanged(Arc.this);
                }

                @Override
                public Object getBean() {
                    return Arc.this;
                }

                @Override
                public String getName() {
                    return "startAngle";
                }
            };
        }
        return startAngle;
    }

    /**
     * Defines the angular extent of the arc in degrees.
     *
     * @defaultValue 0.0
     */
    private final DoubleProperty length = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            NodeHelper.markDirty(Arc.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(Arc.this);
        }

        @Override
        public Object getBean() {
            return Arc.this;
        }

        @Override
        public String getName() {
            return "length";
        }
    };

    public final void setLength(double value) {
        length.set(value);
    }

    public final double getLength() {
        return length.get();
    }

    public final DoubleProperty lengthProperty() {
        return length;
    }

    /**
     * Defines the closure type for the arc:
     * {@link ArcType#OPEN}, {@link ArcType#CHORD},or {@link ArcType#ROUND}.
     *
     * @defaultValue OPEN
     */
    private ObjectProperty<ArcType> type;



    public final void setType(ArcType value) {
        if (type != null || value != ArcType.OPEN) {
            typeProperty().set(value);
        }
    }

    public final ArcType getType() {
        return type == null ? ArcType.OPEN : type.get();
    }

    public final ObjectProperty<ArcType> typeProperty() {
        if (type == null) {
            type = new ObjectPropertyBase<ArcType>(ArcType.OPEN) {

                @Override
                public void invalidated() {
                    NodeHelper.markDirty(Arc.this, DirtyBits.NODE_GEOMETRY);
                    NodeHelper.geomChanged(Arc.this);
                }

                @Override
                public Object getBean() {
                    return Arc.this;
                }

                @Override
                public String getName() {
                    return "type";
                }
            };
        }
        return type;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGArc();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private Arc2D doConfigShape() {
        short tmpType;
        switch (getTypeInternal()) {
        case OPEN:
            tmpType = 0;
            break;
        case CHORD:
            tmpType = 1;
            break;
        default:
            tmpType = 2;
            break;
        }

        shape.setArc(
            (float)(getCenterX() - getRadiusX()), // x
            (float)(getCenterY() - getRadiusY()), // y
            (float)(getRadiusX() * 2.0), // w
            (float)(getRadiusY() * 2.0), // h
            (float)getStartAngle(),
            (float)getLength(),
            tmpType);

        return shape;
    }

    private final ArcType getTypeInternal() {
        ArcType t = getType();
        return t == null ? ArcType.OPEN : t;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        if (NodeHelper.isDirty(this, DirtyBits.NODE_GEOMETRY)) {
            final NGArc peer = NodeHelper.getPeer(this);
            peer.updateArc((float)getCenterX(),
                (float)getCenterY(),
                (float)getRadiusX(),
                (float)getRadiusY(),
                (float)getStartAngle(),
                (float)getLength(),
                getTypeInternal());
        }
    }

    /**
     * Returns a string representation of this {@code Arc} object.
     * @return a string representation of this {@code Arc} object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Arc[");

        String id = getId();
        if (id != null) {
            sb.append("id=").append(id).append(", ");
        }

        sb.append("centerX=").append(getCenterX());
        sb.append(", centerY=").append(getCenterY());
        sb.append(", radiusX=").append(getRadiusX());
        sb.append(", radiusY=").append(getRadiusY());
        sb.append(", startAngle=").append(getStartAngle());
        sb.append(", length=").append(getLength());
        sb.append(", type=").append(getType());

        sb.append(", fill=").append(getFill());

        Paint stroke = getStroke();
        if (stroke != null) {
            sb.append(", stroke=").append(stroke);
            sb.append(", strokeWidth=").append(getStrokeWidth());
        }

        return sb.append("]").toString();
    }
}
