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

import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.shape.PathElementHelper;
import com.sun.javafx.scene.shape.PathHelper;
import com.sun.javafx.scene.shape.PathUtils;
import com.sun.javafx.scene.shape.ShapeHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGPath;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.css.StyleableProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.Collection;
import java.util.List;
import javafx.scene.Node;

/**
 * The {@code Path} class represents a simple shape
 * and provides facilities required for basic construction
 * and management of a geometric path.  Example:
 *
<PRE>
import javafx.scene.shape.*;

Path path = new Path();

MoveTo moveTo = new MoveTo();
moveTo.setX(0.0f);
moveTo.setY(0.0f);

HLineTo hLineTo = new HLineTo();
hLineTo.setX(70.0f);

QuadCurveTo quadCurveTo = new QuadCurveTo();
quadCurveTo.setX(120.0f);
quadCurveTo.setY(60.0f);
quadCurveTo.setControlX(100.0f);
quadCurveTo.setControlY(0.0f);

LineTo lineTo = new LineTo();
lineTo.setX(175.0f);
lineTo.setY(55.0f);

ArcTo arcTo = new ArcTo();
arcTo.setX(50.0f);
arcTo.setY(50.0f);
arcTo.setRadiusX(50.0f);
arcTo.setRadiusY(50.0f);

path.getElements().add(moveTo);
path.getElements().add(hLineTo);
path.getElements().add(quadCurveTo);
path.getElements().add(lineTo);
path.getElements().add(arcTo);

</PRE>
 * @since JavaFX 2.0
 */
public class Path extends Shape {
    static {
        PathHelper.setPathAccessor(new PathHelper.PathAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((Path) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((Path) node).doUpdatePeer();
            }

            @Override
            public Bounds doComputeLayoutBounds(Node node) {
                return ((Path) node).doComputeLayoutBounds();
            }

            @Override
            public Paint doCssGetFillInitialValue(Shape shape) {
                return ((Path) shape).doCssGetFillInitialValue();
            }

            @Override
            public Paint doCssGetStrokeInitialValue(Shape shape) {
                return ((Path) shape).doCssGetStrokeInitialValue();
            }

            @Override
            public com.sun.javafx.geom.Shape doConfigShape(Shape shape) {
                return ((Path) shape).doConfigShape();
            }

        });
    }

    private Path2D path2d = null;

    {
        // To initialize the class helper at the begining each constructor of this class
        PathHelper.initHelper(this);

        // overriding default values for fill and stroke
        // Set through CSS property so that it appears to be a UA style rather
        // that a USER style so that fill and stroke can still be set from CSS.
        ((StyleableProperty)fillProperty()).applyStyle(null, null);
        ((StyleableProperty)strokeProperty()).applyStyle(null, Color.BLACK);
    }

    /**
     * Creates an empty instance of Path.
     */
    public Path() {
    }

    /**
     * Creates a new instance of Path
     * @param elements Elements of the Path
     * @since JavaFX 2.1
     */
    public Path(PathElement... elements) {
        if (elements != null) {
            this.elements.addAll(elements);
        }
    }

    /**
     * Creates new instance of Path
     * @param elements The collection of the elements of the Path
     * @since JavaFX 2.2
     */
    public Path(Collection<? extends PathElement> elements) {
        if (elements != null) {
            this.elements.addAll(elements);
        }
    }

    void markPathDirty() {
        path2d = null;
        NodeHelper.markDirty(this, DirtyBits.NODE_CONTENTS);
        NodeHelper.geomChanged(this);
    }

    /**
     * Defines the filling rule constant for determining the interior of the path.
     * The value must be one of the following constants:
     * {@code FillRile.EVEN_ODD} or {@code FillRule.NON_ZERO}.
     * The default value is {@code FillRule.NON_ZERO}.
     *
     * @defaultValue FillRule.NON_ZERO
     */
    private ObjectProperty<FillRule> fillRule;

    public final void setFillRule(FillRule value) {
        if (fillRule != null || value != FillRule.NON_ZERO) {
            fillRuleProperty().set(value);
        }
    }

    public final FillRule getFillRule() {
        return fillRule == null ? FillRule.NON_ZERO : fillRule.get();
    }

    public final ObjectProperty<FillRule> fillRuleProperty() {
        if (fillRule == null) {
            fillRule = new ObjectPropertyBase<FillRule>(FillRule.NON_ZERO) {

                @Override
                public void invalidated() {
                    NodeHelper.markDirty(Path.this, DirtyBits.NODE_CONTENTS);
                    NodeHelper.geomChanged(Path.this);
                }

                @Override
                public Object getBean() {
                    return Path.this;
                }

                @Override
                public String getName() {
                    return "fillRule";
                }
            };
        }
        return fillRule;
    }

    private boolean isPathValid;
    /**
     * Defines the array of path elements of this path.
     *
     * @defaultValue empty
     */
    private final ObservableList<PathElement> elements = new TrackableObservableList<PathElement>() {
        @Override
        protected void onChanged(Change<PathElement> c) {
            List<PathElement> list = c.getList();
            boolean firstElementChanged = false;
            while (c.next()) {
                List<PathElement> removed = c.getRemoved();
                for (int i = 0; i < c.getRemovedSize(); ++i) {
                    removed.get(i).removeNode(Path.this);
                }
                for (int i = c.getFrom(); i < c.getTo(); ++i) {
                    list.get(i).addNode(Path.this);
                }
                firstElementChanged |= c.getFrom() == 0;
            }

            //Note: as ArcTo may create a various number of PathElements,
            // we cannot count the number of PathElements removed (fast enough).
            // Thus we can optimize only if some elements were added to the end
            if (path2d != null) {
                c.reset();
                c.next();
                // we just have to check the first change, as more changes cannot come after such change
                if (c.getFrom() == c.getList().size() && !c.wasRemoved() && c.wasAdded()) {
                    // some elements added
                    for (int i = c.getFrom(); i < c.getTo(); ++i) {
                        PathElementHelper.addTo(list.get(i), path2d);
                    }
                } else {
                    path2d = null;
                }
            }
            if (firstElementChanged) {
                isPathValid = isFirstPathElementValid();
            }

            NodeHelper.markDirty(Path.this, DirtyBits.NODE_CONTENTS);
            NodeHelper.geomChanged(Path.this);
        }
    };

    /**
     * Gets observable list of path elements of this path.
     * @return Elements of this path
     */
    public final ObservableList<PathElement> getElements() { return elements; }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGPath();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private Path2D doConfigShape() {
        if (isPathValid) {
            if (path2d == null) {
                path2d = PathUtils.configShape(getElements(), getFillRule() == FillRule.EVEN_ODD);
            } else {
                path2d.setWindingRule(getFillRule() == FillRule.NON_ZERO ?
                                      Path2D.WIND_NON_ZERO : Path2D.WIND_EVEN_ODD);
            }
            return path2d;
        } else {
            return new Path2D();
        }
    }

    private Bounds doComputeLayoutBounds() {
       if (isPathValid) {
           return null; // Helper will need to call its super's compute layout bounds
       }
       return new BoundingBox(0, 0, -1, -1); //create empty bounds
    }

    private boolean isFirstPathElementValid() {
        ObservableList<PathElement> _elements = getElements();
        if (_elements != null && _elements.size() > 0) {
            PathElement firstElement = _elements.get(0);
            if (!firstElement.isAbsolute()) {
                System.err.printf("First element of the path can not be relative. Path: %s\n", this);
                return false;
            } else if (firstElement instanceof MoveTo) {
                return true;
            } else {
                System.err.printf("Missing initial moveto in path definition. Path: %s\n", this);
                return false;
            }
        }
        return true;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        if (NodeHelper.isDirty(this, DirtyBits.NODE_CONTENTS)) {
            NGPath peer = NodeHelper.getPeer(this);
            if (peer.acceptsPath2dOnUpdate()) {
                peer.updateWithPath2d((Path2D) ShapeHelper.configShape(this));
            } else {
                peer.reset();
                if (isPathValid) {
                    peer.setFillRule(getFillRule());
                    for (final PathElement elt : getElements()) {
                        elt.addTo(peer);
                    }
                    peer.update();
                }
            }
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

    /*
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
     * Returns a string representation of this {@code Path} object.
     * @return a string representation of this {@code Path} object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Path[");

        String id = getId();
        if (id != null) {
            sb.append("id=").append(id).append(", ");
        }

        sb.append("elements=").append(getElements());

        sb.append(", fill=").append(getFill());
        sb.append(", fillRule=").append(getFillRule());

        Paint stroke = getStroke();
        if (stroke != null) {
            sb.append(", stroke=").append(stroke);
            sb.append(", strokeWidth=").append(getStrokeWidth());
        }

        return sb.append("]").toString();
    }
}
