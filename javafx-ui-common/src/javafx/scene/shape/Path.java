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

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.shape.PathUtils;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGPath;
import com.sun.javafx.tk.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import javafx.beans.value.WritableValue;
import javafx.scene.paint.Paint;

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
 *
 *
 * @profile common
 */
public class Path extends Shape {

    private Path2D path2d = null;

    /**
     * Creates an empty instance of Path.
     */
    public Path() {
    }

    /**
     * Creates a new instance of Path
     * @param elements Elements of the Path
     */
    public Path(PathElement... elements) {
        if (elements != null) {
            for (PathElement element : elements) {
                this.getElements().add(element);
            }
        }
    }

    static com.sun.javafx.sg.PGPath.FillRule toPGFillRule(FillRule rule) {
        if (rule == FillRule.NON_ZERO) {
            return PGPath.FillRule.NON_ZERO;
        } else {
            return PGPath.FillRule.EVEN_ODD;
        }
    }

    {
        // overriding default values for fill and stroke
        // Set through CSS property so that it appears to be a UA style rather
        // that a USER style so that fill and stroke can still be set from CSS.
        StyleableProperties.FILL.set(this, null, null);
        Shape.StyleableProperties.STROKE.set(this, Color.BLACK, null);
    }

    void markPathDirty() {
        path2d = null;
        impl_markDirty(DirtyBits.NODE_CONTENTS);
        impl_geomChanged();
    }

    /**
     * Defines the filling rule constant for determining the interior of the path.
     * The value must be one of the following constants:
     * {@code FillRile.EVEN_ODD} or {@code FillRule.NON_ZERO}.
     * The default value is {@code FillRule.NON_ZERO}.
     *
     * @profile common
     * @defaultvalue FillRule.NON_ZERO
     */
    private ObjectProperty<FillRule> fillRule;



    public final void setFillRule(FillRule value) {
        fillRuleProperty().set(value);
    }

    public final FillRule getFillRule() {
        return fillRule == null ? FillRule.NON_ZERO : fillRule.get();
    }

    public final ObjectProperty<FillRule> fillRuleProperty() {
        if (fillRule == null) {
            fillRule = new ObjectPropertyBase<FillRule>(FillRule.NON_ZERO) {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_CONTENTS);
                    impl_geomChanged();
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
     * @profile common
     * @defaultvalue empty
     */
    private final ObservableList<PathElement> elements = new TrackableObservableList<PathElement>() {
        @Override
        protected void onChanged(Change<PathElement> c) {
            //
            // TODO: Need to keep a list of Path objects in PathElement if we want
            // to support sharability. See how transforms is handled in Node.
            //
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
                        list.get(i).impl_addTo(path2d);
                    }
                } else {
                    path2d = null;
                }
            }
            if (firstElementChanged) {
                isPathValid = impl_isFirstPathElementValid();
            }

            impl_markDirty(DirtyBits.NODE_CONTENTS);
            impl_geomChanged();
        }
    };

    /**
     * Gets observable list of path elements of this path.
     * @return Elements of this path
     */
    public final ObservableList<PathElement> getElements() { return elements; }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGPath();
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public PGPath impl_getPGPath() {
        return (PGPath)impl_getPGNode();
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
	public Path2D impl_configShape() {
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

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected Bounds impl_computeLayoutBounds() {
       if (isPathValid) {
           return super.impl_computeLayoutBounds();
       }
       return new BoundingBox(0, 0, -1, -1); //create empty bounds
    }

    private boolean impl_isFirstPathElementValid() {
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

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePG() {
        super.impl_updatePG();

        if (impl_isDirty(DirtyBits.NODE_CONTENTS)) {
            PGPath peer = impl_getPGPath();
            if (peer.acceptsPath2dOnUpdate()) {
                peer.updateWithPath2d(impl_configShape());
            } else {
                peer.reset();
                if (isPathValid) {
                    peer.setFillRule(toPGFillRule(getFillRule()));
                    for (final PathElement elt : getElements()) {
                        elt.addTo(peer);
                    }
                    peer.update();
                }
            }
        }
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    @Override
    protected StyleableProperty getStyleablePropertyForFillProperty() {
        return StyleableProperties.FILL;
    }

    /**
     * Super-lazy instantiation pattern from Bill Pugh.
     * @treatasprivate implementation detail
     */
    private static class StyleableProperties {

        /**
        * @css -fx-fill: <a href="../doc-files/cssref.html#typepaint">&lt;paint&gt;</a>
        * @see Shape#fill
        */
        protected static final StyleableProperty<Path,Paint> FILL =
            new StyleableProperty<Path,Paint>("-fx-fill", 
                PaintConverter.getInstance(), null) {

            @Override
            public boolean isSettable(Path node) {
                return Shape.StyleableProperties.FILL.isSettable(node);
            }

            @Override
            public WritableValue<Paint> getWritableValue(Path node) {
                return Shape.StyleableProperties.FILL.getWritableValue(node);
            }

        };     
        private static final List<StyleableProperty> STYLEABLES;
        static {

            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Shape.impl_CSS_STYLEABLES());
            // remove FILL since Line FILL has a different initial value.
            styleables.remove(Shape.StyleableProperties.FILL);
            Collections.addAll(styleables, StyleableProperties.FILL);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Super-lazy instantiation pattern from Bill Pugh. StyleableProperty is referenced
     * no earlier (and therefore loaded no earlier by the class loader) than
     * the moment that  impl_CSS_STYLEABLES() is called.
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return StyleableProperties.STYLEABLES;
    }
    
}
