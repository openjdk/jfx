/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.shape.PolygonHelper;
import com.sun.javafx.scene.shape.ShapeHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGPolygon;
import com.sun.javafx.sg.prism.NGShape;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.paint.Paint;

/**
 * Creates a polygon, defined by an array of x,y coordinates. The Polygon
 * class is similar to the Polyline class, except that the Polyline class
 * is not automatically closed.
 *
<PRE>
import javafx.scene.shape.*;

Polygon polygon = new Polygon();
polygon.getPoints().addAll(new Double[]{
    0.0, 0.0,
    20.0, 10.0,
    10.0, 20.0 });
</PRE>
 * @since JavaFX 2.0
 */
public  class Polygon extends Shape {
    static {
        PolygonHelper.setPolygonAccessor(new PolygonHelper.PolygonAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((Polygon) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((Polygon) node).doUpdatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((Polygon) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public com.sun.javafx.geom.Shape doConfigShape(Shape shape) {
                return ((Polygon) shape).doConfigShape();
            }
        });
    }

    private final Path2D shape = new Path2D();

    {
        // To initialize the class helper at the begining each constructor of this class
        PolygonHelper.initHelper(this);
    }

    /**
     * Creates an empty instance of Polygon.
     */
    public Polygon() {
    }

    /**
     * Creates a new instance of Polygon.
     * @param points the coordinates of the polygon vertices
     */
    public Polygon(double... points) {
        if (points != null) {
            for (double p : points) {
                this.getPoints().add(p);
            }
        }
    }

    /**
     * Defines the coordinates of the polygon vertices.
     *
     * @defaultValue empty
     */
    private final ObservableList<Double> points = new TrackableObservableList<>() {
        @Override
        protected void onChanged(Change<Double> c) {
            NodeHelper.markDirty(Polygon.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(Polygon.this);
        }
    };

    /**
     * Gets the coordinates of the {@code Polygon} vertices.
     * @return An observable list of vertices of this {@code Polygon}
     */
    public final ObservableList<Double> getPoints() { return points; }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGPolygon();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        if (getMode() == NGShape.Mode.EMPTY || getPoints().size() <= 1) {
            return bounds.makeEmpty();
        }

        if (getPoints().size() == 2) {
            if (getMode() == NGShape.Mode.FILL || getStrokeType() == StrokeType.INSIDE) {
                return bounds.makeEmpty();
            }
            double upad = getStrokeWidth();
            if (getStrokeType() == StrokeType.CENTERED) {
                upad /= 2.0f;
            }
            return computeBounds(bounds, tx, upad, 0.5f,
                getPoints().get(0), getPoints().get(1), 0.0f, 0.0f);
        } else {
            return computeShapeBounds(bounds, tx, ShapeHelper.configShape(this));
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private Path2D doConfigShape() {
        double p1 = getPoints().get(0);
        double p2 = getPoints().get(1);
        shape.reset();
        shape.moveTo((float)p1, (float)p2);
        final int numValidPoints = getPoints().size() & ~1;
        for (int i = 2; i < numValidPoints; i += 2) {
            p1 = getPoints().get(i); p2 = getPoints().get(i+1);
            shape.lineTo((float)p1, (float)p2);
        }
        shape.closePath();
        return shape;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        if (NodeHelper.isDirty(this, DirtyBits.NODE_GEOMETRY)) {
            final int numValidPoints = getPoints().size() & ~1;
            float points_array[] = new float[numValidPoints];
            for (int i = 0; i < numValidPoints; i++) {
                points_array[i] = (float)getPoints().get(i).doubleValue();
            }
            final NGPolygon peer = NodeHelper.getPeer(this);
            peer.updatePolygon(points_array);
        }
    }

    /**
     * Returns a string representation of this {@code Polygon} object.
     * @return a string representation of this {@code Polygon} object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Polygon[");

        String id = getId();
        if (id != null) {
            sb.append("id=").append(id).append(", ");
        }

        sb.append("points=").append(getPoints());

        sb.append(", fill=").append(getFill());

        Paint stroke = getStroke();
        if (stroke != null) {
            sb.append(", stroke=").append(stroke);
            sb.append(", strokeWidth=").append(getStrokeWidth());
        }

        return sb.append("]").toString();
    }
}

