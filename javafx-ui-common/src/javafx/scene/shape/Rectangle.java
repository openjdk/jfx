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
 */

package javafx.scene.shape;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.value.WritableValue;
import javafx.scene.paint.Paint;

import com.sun.javafx.css.StyleableDoubleProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGRectangle;
import com.sun.javafx.sg.PGShape.Mode;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.Property;


/**
 * The {@code Rectangle} class defines a rectangle
 * with the specified size and location. By default the rectangle
 * has sharp corners. Rounded corners can be specified using
 * the arcWidth and arcHeight variables.

 * <p>Example code: the following code creates a rectangle with 20 pixel
 * rounded corners.</p>
 *
<PRE>
import javafx.scene.shape.*;

Rectangle r = new Rectangle();
r.setX(50);
r.setY(50);
r.setWidth(200);
r.setHeight(100);
r.setArcWidth(20);
r.setArcHeight(20);
</PRE>
 *
 * @profile common
 */
public  class Rectangle extends Shape {

    private final RoundRectangle2D shape = new RoundRectangle2D();

    private static final int NON_RECTILINEAR_TYPE_MASK = ~(
            BaseTransform.TYPE_TRANSLATION |
            BaseTransform.TYPE_MASK_SCALE |
            BaseTransform.TYPE_FLIP);

    /**
     * Creates an empty instance of Rectangle.
     */
    public Rectangle() {
    }

    /**
     * Creates a new instance of Rectangle with the given size.
     * @param width width of the rectangle
     * @param height height of the rectangle
     */
    public Rectangle(double width, double height) {
        setWidth(width);
        setHeight(height);
    }

    /**
     * Creates a new instance of Rectangle with the given size and fill.
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param fill determines how to fill the interior of the rectangle
     */
    public Rectangle(double width, double height, Paint fill) {
        setWidth(width);
        setHeight(height);
        setFill(fill);
    }

    /**
     * Creates a new instance of Rectangle with the given position, size and fill.
     * @param x horizontal position of the rectangle
     * @param y vertical position of the rectangle
     * @param width width of the rectangle
     * @param height height of the rectangle
     */
    public Rectangle(double x, double y, double width, double height) {
        this(width, height);
        setX(x);
        setY(y);
    }

    /**
     * Defines the X coordinate of the upper-left corner of the rectangle.
     *
     * @profile common
     * @defaultValue 0.0
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
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return Rectangle.this;
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
     * Defines the Y coordinate of the upper-left corner of the rectangle.
     *
     * @profile common
     * @defaultValue 0.0
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
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return Rectangle.this;
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
     * Defines the width of the rectangle.
     *
     * @profile common
     * @defaultValue 0.0
     */
    private DoubleProperty width;


    public final void setWidth(double value) {
        widthProperty().set(value);
    }

    public final double getWidth() {
        return width == null ? 0.0 : width.get();
    }

    public final DoubleProperty widthProperty() {
        if (width == null) {
            width = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return Rectangle.this;
                }

                @Override
                public String getName() {
                    return "width";
                }
            };
        }
        return width;
    }

    /**
     * Defines the height of the rectangle.
     *
     * @profile common
     * @defaultValue 0.0
     */
    private DoubleProperty height;


    public final void setHeight(double value) {
        heightProperty().set(value);
    }

    public final double getHeight() {
        return height == null ? 0.0 : height.get();
    }

    public final DoubleProperty heightProperty() {
        if (height == null) {
            height = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return Rectangle.this;
                }

                @Override
                public String getName() {
                    return "height";
                }
            };
        }
        return height;
    }

    /**
     * Defines the horizontal diameter of the arc
     * at the four corners of the rectangle.
     *
     * @profile common
     * @defaultValue 0.0
     */
    private DoubleProperty arcWidth;


    public final void setArcWidth(double value) {
	arcWidthProperty().set(value);
    }

    public final double getArcWidth() {
        return arcWidth == null ? 0.0 : arcWidth.get();
    }

    public final DoubleProperty arcWidthProperty() {
        if (arcWidth == null) {
            arcWidth = new StyleableDoubleProperty() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                }
                
                @Override 
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.ARC_WIDTH;
                }

                @Override
                public Object getBean() {
                    return Rectangle.this;
                }

                @Override
                public String getName() {
                    return "arcWidth";
                }
            };
        }
        return arcWidth;
    }

    /**
     * Defines the vertical diameter of the arc
     * at the four corners of the rectangle.
     *
     * @profile common
     * @defaultValue 0.0
     */
    private DoubleProperty arcHeight;


    public final void setArcHeight(double value) {
 	arcHeightProperty().set(value);
    }

    public final double getArcHeight() {
        return arcHeight == null ? 0.0 : arcHeight.get();
    }

    public final DoubleProperty arcHeightProperty() {
        if (arcHeight == null) {
            arcHeight = new StyleableDoubleProperty() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                }

                @Override 
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.ARC_HEIGHT;
                }
                
                @Override
                public Object getBean() {
                    return Rectangle.this;
                }

                @Override
                public String getName() {
                    return "arcHeight";
                }
            };
        }
        return arcHeight;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGRectangle();
    }

    PGRectangle getPGRect() {
        return (PGRectangle) impl_getPGNode();
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

      /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatAsPrivate implementation detail
      */
     private static class StyleableProperties {
         private static final StyleableProperty<Rectangle,Number> ARC_HEIGHT =
            new StyleableProperty<Rectangle,Number>("-fx-arc-height",
                SizeConverter.getInstance(), 0.0) {

            @Override
            public boolean isSettable(Rectangle node) {
                return node.arcHeight == null || !node.arcHeight.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(Rectangle node) {
                return node.arcHeightProperty();
            }

        };         
         private static final StyleableProperty<Rectangle,Number> ARC_WIDTH =
            new StyleableProperty<Rectangle,Number>("-fx-arc-width",
                SizeConverter.getInstance(), 0.0) {

            @Override
            public boolean isSettable(Rectangle node) {
                return node.arcWidth == null || !node.arcWidth.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(Rectangle node) {
                return node.arcWidthProperty();
            }

        };
         
         private static final List<StyleableProperty> STYLEABLES;
         static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Shape.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                ARC_HEIGHT,
                ARC_WIDTH
             );
            STYLEABLES = Collections.unmodifiableList(styleables);

         }
    }
     
    /**
     * Super-lazy instantiation pattern from Bill Pugh. StyleableProperties is referenced
     * no earlier (and therefore loaded no earlier by the class loader) than
     * the moment that  impl_CSS_STYLEABLES() is called.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return Rectangle.StyleableProperties.STYLEABLES;
    }

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected com.sun.javafx.sg.PGShape.StrokeLineJoin toPGLineJoin(StrokeLineJoin t) {
        // If we are a round rectangle then MITER can produce anomalous
        // results for very thin or very wide corner arcs when the bezier
        // curves that approximate the arcs become so distorted that they
        // shoot out MITER-like extensions.  This effect complicates matters
        // because it makes such "round" rectangles non-round, and also
        // because it means we might have to pad the bounds to account
        // for this rare and unpredictable circumstance.
        // To avoid the problem, we set the Join style to BEVEL for any
        // rounded rect.  The BEVEL join style is more predictable for
        // anomalous angles and is the simplest join style to compute in
        // the stroking code.
        // For non-rounded rectangles, the angles are all 90 degrees and so
        // the computations are both simple and non-problematic so we pass on
        // the join style unmodified to the PG layer.
        if ((getArcWidth() > 0) && (getArcHeight() > 0)) {
            return com.sun.javafx.sg.PGShape.StrokeLineJoin.BEVEL;
        }
        return super.toPGLineJoin(t);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        // if there is no fill or stroke, then there are no bounds. The bounds
        // must be marked empty in this case to distinguish it from 0,0,0,0
        // which would actually contribute to the bounds of a group.
        if (impl_mode == Mode.EMPTY) {
            return bounds.makeEmpty();
        }
        if ((getArcWidth() > 0) && (getArcHeight() > 0)
                && ((tx.getType() & NON_RECTILINEAR_TYPE_MASK) != 0)) {
            // TODO: Optimize rotated bounds...
            return computeShapeBounds(bounds, tx, impl_configShape());
        }
        double upad;
        double dpad;
        if ((impl_mode == Mode.FILL) || (getStrokeType() == StrokeType.INSIDE)) {
            upad = dpad = 0;
        } else {
            upad = getStrokeWidth();
            if (getStrokeType() == StrokeType.CENTERED) {
                upad /= 2.0;
            }
            dpad = 0.0f;
        }
        return computeBounds(bounds, tx, upad, dpad, getX(), getY(), getWidth(), getHeight());
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
	public RoundRectangle2D impl_configShape() {
        if ((getArcWidth() > 0) && (getArcHeight() > 0)) {
            shape.setRoundRect((float)getX(), (float)getY(),
                    (float)getWidth(), (float)getHeight(),
                    (float)getArcWidth(), (float)getArcHeight());
        } else {
            shape.setRoundRect(
                    (float)getX(), (float)getY(),
                    (float)getWidth(), (float)getHeight(), 0, 0);
        }
        return shape;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePG() {
        super.impl_updatePG();
        if (impl_isDirty(DirtyBits.NODE_GEOMETRY)) {
            PGRectangle peer = getPGRect();
            peer.updateRectangle((float)getX(),
                (float)getY(),
                (float)getWidth(),
                (float)getHeight(),
                (float)getArcWidth(),
                (float)getArcHeight());
        }
    }
}
