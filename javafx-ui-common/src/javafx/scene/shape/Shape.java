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

import com.sun.javafx.geom.transform.Affine3D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import com.sun.javafx.Utils;
import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.css.StyleableBooleanProperty;
import com.sun.javafx.css.StyleableDoubleProperty;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.geom.Area;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.layout.region.ShapeChangeListener;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGShape;
import com.sun.javafx.sg.PGShape.Mode;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.Property;
import javafx.collections.ListChangeListener.Change;


/**
 * The {@code Shape} class provides definitions of common properties for
 * objects that represent some form of geometric shape.  These properties
 * include:
 * <ul>
 * <li>The {@link Paint} to be applied to the fillable interior of the
 * shape (see {@link #setFill setFill}).
 * <li>The {@link Paint} to be applied to stroke the outline of the
 * shape (see {@link #setStroke setStroke}).
 * <li>The decorative properties of the stroke, including:
 * <ul>
 * <li>The width of the border stroke.
 * <li>Whether the border is drawn as an exterior padding to the edges
 * of the shape, as an interior edging that follows the inside of the border,
 * or as a wide path that follows along the border straddling it equally
 * both inside and outside (see {@link StrokeType}).
 * <li>Decoration styles for the joins between path segments and the
 * unclosed ends of paths.
 * <li>Dashing attributes.
 * </ul>
 * </ul>
 * <h4>Interaction with coordinate systems</h4>
 * Most nodes tend to have only integer translations applied to them and
 * quite often they are defined using integer coordinates as well.  For
 * this common case, fills of shapes with straight line edges tend to be
 * crisp since they line up with the cracks between pixels that fall on
 * integer device coordinates and thus tend to naturally cover entire pixels.
 * <p>
 * On the other hand, stroking those same shapes can often lead to fuzzy
 * outlines because the default stroking attributes specify both that the
 * default stroke width is 1.0 coordinates which often maps to exactly 1
 * device pixel and also that the stroke should straddle the border of the
 * shape, falling half on either side of the border.
 * Since the borders in many common shapes tend to fall directly on integer
 * coordinates and those integer coordinates often map precisely to integer
 * device locations, the borders tend to result in 50% coverage over the
 * pixel rows and columns on either side of the border of the shape rather
 * than 100% coverage on one or the other.  Thus, fills may typically be
 * crisp, but strokes are often fuzzy.
 * <p>
 * Two common solutions to avoid these fuzzy outlines are to use wider
 * strokes that cover more pixels completely - typically a stroke width of
 * 2.0 will achieve this if there are no scale transforms in effect - or
 * to specify either the {@link StrokeType#INSIDE} or {@link StrokeType#OUTSIDE}
 * stroke styles - which will bias the default single unit stroke onto one
 * of the full pixel rows or columns just inside or outside the border of
 * the shape.
 */
public abstract class Shape extends Node {

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected PGNode impl_createPGNode() {
        throw new AssertionError(
            "Subclasses of Shape must implement impl_createPGNode");
    }

    PGShape getPGShape() {
        return (PGShape) impl_getPGNode();
    }

    com.sun.javafx.sg.PGShape.StrokeType toPGStrokeType(StrokeType t) {
        if (t == StrokeType.INSIDE) {
            return PGShape.StrokeType.INSIDE;
        } else if (t == StrokeType.OUTSIDE) {
            return PGShape.StrokeType.OUTSIDE;
        } else {
            return PGShape.StrokeType.CENTERED;
        }
    }

    com.sun.javafx.sg.PGShape.StrokeLineCap toPGLineCap(StrokeLineCap t) {
        if (t == StrokeLineCap.SQUARE) {
            return PGShape.StrokeLineCap.SQUARE;
        } else if (t == StrokeLineCap.BUTT) {
            return PGShape.StrokeLineCap.BUTT;
        } else {
            return PGShape.StrokeLineCap.ROUND;
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected com.sun.javafx.sg.PGShape.StrokeLineJoin toPGLineJoin(StrokeLineJoin t) {
        if (t == StrokeLineJoin.MITER) {
            return PGShape.StrokeLineJoin.MITER;
        } else if (t == StrokeLineJoin.BEVEL) {
            return PGShape.StrokeLineJoin.BEVEL;
        } else {
            return PGShape.StrokeLineJoin.ROUND;
        }
    }

    public final void setStrokeType(StrokeType value) {
        strokeTypeProperty().set(value);
    }

    public final StrokeType getStrokeType() {
        return (strokeAttributes == null) ? DEFAULT_STROKE_TYPE
                                          : strokeAttributes.getType();
    }

    /**
     * Defines the direction (inside, centered, or outside) that the strokeWidth
     * is applied to the boundary of the shape.
     *
     * <p>
     * The image shows a shape without stroke and with a thick stroke applied
     * inside, centered and outside.
     * </p><p>
     * <img src="doc-files/stroketype.png"/>
     * </p>
     *
     * @see StrokeType
     * @defaultValue CENTERED
     * @since JavaFX 1.3
     */
    public final ObjectProperty<StrokeType> strokeTypeProperty() {
        return getStrokeAttributes().typeProperty();
    }

    public final void setStrokeWidth(double value) {
        strokeWidthProperty().set(value);
    }

    public final double getStrokeWidth() {
        return (strokeAttributes == null) ? DEFAULT_STROKE_WIDTH
                                          : strokeAttributes.getWidth();
    }

    /**
     * Defines a square pen line width. A value of 0.0 specifies a hairline
     * stroke. A value of less than 0.0 will be treated as 0.0.
     *
     * @defaultValue 1.0
     */
    public final DoubleProperty strokeWidthProperty() {
        return getStrokeAttributes().widthProperty();
    }

    public final void setStrokeLineJoin(StrokeLineJoin value) {
        strokeLineJoinProperty().set(value);
    }

    public final StrokeLineJoin getStrokeLineJoin() {
        return (strokeAttributes == null)
                ? DEFAULT_STROKE_LINE_JOIN
                : strokeAttributes.getLineJoin();
    }

    /**
     * Defines the decoration applied where path segments meet.
     * The value must have one of the following values:
     * {@code StrokeLineJoin.MITER}, {@code StrokeLineJoin.BEVEL},
     * and {@code StrokeLineJoin.ROUND}. The image shows a shape
     * using the values in the mentioned order.
     * </p><p>
     * <img src="doc-files/strokelinejoin.png"/>
     * </p>
     *
     * @see StrokeLineJoin
     * @defaultValue MITER
     */
    public final ObjectProperty<StrokeLineJoin> strokeLineJoinProperty() {
        return getStrokeAttributes().lineJoinProperty();
    }

    public final void setStrokeLineCap(StrokeLineCap value) {
        strokeLineCapProperty().set(value);
    }

    public final StrokeLineCap getStrokeLineCap() {
        return (strokeAttributes == null) ? DEFAULT_STROKE_LINE_CAP
                                          : strokeAttributes.getLineCap();
    }

    /**
     * The end cap style of this {@code Shape} as one of the following
     * values that define possible end cap styles:
     * {@code StrokeLineCap.BUTT}, {@code StrokeLineCap.ROUND},
     * and  {@code StrokeLineCap.SQUARE}. The image shows a line
     * using the values in the mentioned order.
     * </p><p>
     * <img src="doc-files/strokelinecap.png"/>
     * </p>
     *
     * @see StrokeLineCap
     * @defaultValue SQUARE
     */
    public final ObjectProperty<StrokeLineCap> strokeLineCapProperty() {
        return getStrokeAttributes().lineCapProperty();
    }

    public final void setStrokeMiterLimit(double value) {
        strokeMiterLimitProperty().set(value);
    }

    public final double getStrokeMiterLimit() {
        return (strokeAttributes == null) ? DEFAULT_STROKE_MITER_LIMIT
                                          : strokeAttributes.getMiterLimit();
    }

    /**
     * Defines the limit for the {@code StrokeLineJoin.MITER} line join style.
     * A value of less than 1.0 will be treated as 1.0.
     *
     * <p>
     * The image demonstrates the behavior. Miter length ({@code A}) is computed
     * as the distance of the most inside point to the most outside point of
     * the joint, with the stroke width as a unit. If the miter length is bigger
     * than the given miter limit, the miter is cut at the edge of the shape
     * ({@code B}). For the situation in the image it means that the miter
     * will be cut at {@code B} for limit values less than {@code 4.65}.
     * </p><p>
     * <img src="doc-files/strokemiterlimit.png"/>
     * </p>
     *
     * @defaultValue 10.0
     */
    public final DoubleProperty strokeMiterLimitProperty() {
        return getStrokeAttributes().miterLimitProperty();
    }

    public final void setStrokeDashOffset(double value) {
        strokeDashOffsetProperty().set(value);
    }

    public final double getStrokeDashOffset() {
        return (strokeAttributes == null) ? DEFAULT_STROKE_DASH_OFFSET
                                          : strokeAttributes.getDashOffset();
    }

    /**
     * Defines a distance specified in user coordinates that represents
     * an offset into the dashing pattern. In other words, the dash phase
     * defines the point in the dashing pattern that will correspond
     * to the beginning of the stroke.
     *
     * <p>
     * The image shows a stroke with dash array {@code [25, 20, 5, 20]} and
     * a stroke with the same pattern and offset {@code 45} which shifts
     * the pattern about the length of the first dash segment and
     * the following space.
     * </p><p>
     * <img src="doc-files/strokedashoffset.png"/>
     * </p>
     *
     * @defaultValue 0
     */
    public final DoubleProperty strokeDashOffsetProperty() {
        return getStrokeAttributes().dashOffsetProperty();
    }

    /**
     * Defines the array representing the lengths of the dash segments.
     * Alternate entries in the array represent the user space lengths
     * of the opaque and transparent segments of the dashes.
     * As the pen moves along the outline of the {@code Shape} to be stroked,
     * the user space distance that the pen travels is accumulated.
     * The distance value is used to index into the dash array.
     * The pen is opaque when its current cumulative distance maps
     * to an even element of the dash array and transparent otherwise.
     * An empty strokeDashArray indicates a solid line with no spaces.
     *
     * <p>
     * The image shows a shape with stroke dash array {@code [25, 20, 5, 20]}
     * </p><p>
     * <img src="doc-files/strokedasharray.png"/>
     * </p>
     *
     * @defaultValue empty
     */
    public final ObservableList<Double> getStrokeDashArray() {        
        return getStrokeAttributes().dashArrayProperty();
    }

    private PGShape.Mode computeMode() {
        if (getFill() != null && getStroke() != null) {
            return PGShape.Mode.STROKE_FILL;
        } else if (getFill() != null) {
            return PGShape.Mode.FILL;
        } else if (getStroke() != null) {
            return PGShape.Mode.STROKE;
        } else {
            return PGShape.Mode.EMPTY;
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected PGShape.Mode impl_mode = Mode.FILL;

    private void checkModeChanged() {
        PGShape.Mode newMode = computeMode();
        if (impl_mode != newMode) {
            impl_mode = newMode;

            impl_markDirty(DirtyBits.SHAPE_MODE);
            impl_geomChanged();
        }
    }

    /**
     * Defines parameters to fill the interior of an {@code Shape}
     * using the settings of the {@code Paint} context.
     * The default value is {@code Color.BLACK} for all shapes except
     * Line, Polyline, and Path. The default value is {@code null} for
     * those shapes.
     */
    private ObjectProperty<Paint> fill;


    public final void setFill(Paint value) {
        fillProperty().set(value);
    }

    public final Paint getFill() {
        return fill == null ? Color.BLACK : fill.get();
    }

    public final ObjectProperty<Paint> fillProperty() {
        if (fill == null) {
            fill = new StyleableObjectProperty<Paint>(Color.BLACK) {
                @Override public void invalidated() {
                    impl_markDirty(DirtyBits.SHAPE_FILL);
                    checkModeChanged();
                    impl_strokeOrFillChanged();
                }
                
                @Override
                public StyleableProperty getStyleableProperty() {
                    return impl_cssGetStyleablePropertyForFillProperty();
                }

                @Override
                public Object getBean() {
                    return Shape.this;
                }

                @Override
                public String getName() {
                    return "fill";
                }
            };
        }
        return fill;
    }
    
    /** 
     * Some sub-class of Shape, such as {@link Line}, override the
     * default value for the {@link Shape#fill} property. This allows
     * the {@link Shape#fill} property method getStyleableProperty to 
     * return a StyleableProperty with the correct default.
     * @treatAsPrivate Implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected StyleableProperty impl_cssGetStyleablePropertyForFillProperty() {
        return StyleableProperties.FILL;
    }

    /**
     * Defines parameters of a stroke that is drawn around the outline of
     * a {@code Shape} using the settings of the specified {@code Paint}.
     * The default value is {@code null} for all shapes except
     * Line, Polyline, and Path. The default value is {@code Color.BLACK} for
     * those shapes.
     */
    private ObjectProperty<Paint> stroke;


    public final void setStroke(Paint value) {
        strokeProperty().set(value);
    }

    public final Paint getStroke() {
        return stroke == null ? null : stroke.get();
    }

    public final ObjectProperty<Paint> strokeProperty() {
        if (stroke == null) {
            stroke = new StyleableObjectProperty<Paint>() {
                @Override public void invalidated() {
                    impl_markDirty(DirtyBits.SHAPE_STROKE);
                    checkModeChanged();
                    impl_strokeOrFillChanged();
                }
                
                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.STROKE;
                }

                @Override
                public Object getBean() {
                    return Shape.this;
                }

                @Override
                public String getName() {
                    return "stroke";
                }
            };
        }
        return stroke;
    }

    // Used by Text
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected void impl_strokeOrFillChanged() { }

    /**
     * Defines whether antialiasing hints are used or not for this {@code Shape}.
     * If the value equals true the rendering hints are applied.
     *
     * @defaultValue true
     */
    private BooleanProperty smooth;


    public final void setSmooth(boolean value) {
        smoothProperty().set(value);
    }

    public final boolean isSmooth() {
        return smooth == null ? true : smooth.get();
    }

    public final BooleanProperty smoothProperty() {
        if (smooth == null) {
            smooth = new StyleableBooleanProperty(true) {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_SMOOTH);
                }
                
                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.SMOOTH;
                }

                @Override
                public Object getBean() {
                    return Shape.this;
                }

                @Override
                public String getName() {
                    return "smooth";
                }
            };
        }
        return smooth;
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
     protected static class StyleableProperties {

        /**
        * @css -fx-fill: <a href="../doc-files/cssref.html#typepaint">&lt;paint&gt;</a>
        * @see Shape#fill
        */
        protected static final StyleableProperty<Shape,Paint> FILL =
            new StyleableProperty<Shape,Paint>("-fx-fill", 
                PaintConverter.getInstance(), Color.BLACK) {

            @Override
            public boolean isSettable(Shape node) {
                return node.fill == null || !node.fill.isBound();
            }

            @Override
            public WritableValue<Paint> getWritableValue(Shape node) {
                return node.fillProperty();
            }

        };

        /**
        * @css -fx-smooth: <a href="../doc-files/cssref.html#typeboolean">&lt;boolean&gt;</a>
        * @see Shape#smooth
        */
        private static final StyleableProperty<Shape,Boolean> SMOOTH =
            new StyleableProperty<Shape,Boolean>("-fx-smooth", 
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(Shape node) {
                return node.smooth == null || !node.smooth.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(Shape node) {
                return node.smoothProperty();
            }

        };

        /**
        * @css -fx-stroke: <a href="../doc-files/cssref.html#typepaint">&lt;paint&gt;</a>
        * @see Shape#stroke
        */    
        protected static final StyleableProperty<Shape,Paint> STROKE =
            new StyleableProperty<Shape,Paint>("-fx-stroke", 
                PaintConverter.getInstance()) {

            @Override
            public boolean isSettable(Shape node) {
                return node.stroke == null || !node.stroke.isBound();
            }

            @Override
            public WritableValue<Paint> getWritableValue(Shape node) {
                return node.strokeProperty();
            }

        };

        /**
        * @css -fx-stroke-dash-array: <a href="#typesize" class="typelink">&lt;size&gt;</a>
        *                    [<a href="#typesize" class="typelink">&lt;size&gt;</a>]+
        * <p>
        * Note:
        * Because {@link StrokeAttributes#dashArray} is not itself a 
        * {@link Property}, 
        * the <code>getProperty()</code> method of this StyleableProperty 
        * returns the {@link StrokeAttributes#dashArray} wrapped in an
        * {@link ObjectProperty}. This is inconsistent with other
        * StyleableProperties which return the actual {@link Property}. 
        * </p>
        * @see StrokeAttributes#dashArray
        */    
        private static final StyleableProperty<Shape,Double[]> STROKE_DASH_ARRAY =
            new StyleableProperty<Shape,Double[]>("-fx-stroke-dash-array",
                SizeConverter.SequenceConverter.getInstance(), 
                new Double[0]) {

            @Override
            public boolean isSettable(Shape node) {
                return true;
            }

            @Override
            public WritableValue<Double[]> getWritableValue(final Shape node) {
                return node.getStrokeAttributes().cssDashArrayProperty();
            }

        };

        /**
        * @css -fx-stroke-dash-offset: <a href="#typesize" class="typelink">&lt;size&gt;</a>
        * @see #strokeDashOffsetProperty() 
        */        
        private static final StyleableProperty<Shape,Number> STROKE_DASH_OFFSET =
            new StyleableProperty<Shape,Number>("-fx-stroke-dash-offset",
                SizeConverter.getInstance(), 0.0) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetDashOffset();
            }

            @Override
            public WritableValue<Number> getWritableValue(Shape node) {
                return node.strokeDashOffsetProperty();
            }

        };

        /**
        * @css -fx-stroke-line-cap: [ square | butt | round ]
        * @see #strokeLineCapProperty() 
        */        
        private static final StyleableProperty<Shape,StrokeLineCap> STROKE_LINE_CAP =
            new StyleableProperty<Shape,StrokeLineCap>("-fx-stroke-line-cap",
                new EnumConverter<StrokeLineCap>(StrokeLineCap.class), 
                StrokeLineCap.SQUARE) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetLineCap();
            }

            @Override
            public WritableValue<StrokeLineCap> getWritableValue(Shape node) {
                return node.strokeLineCapProperty();
            }

        };

        /**
        * @css -fx-stroke-line-join: [ miter | bevel | round ]
        * @see #strokeLineJoinProperty() 
        */        
        private static final StyleableProperty<Shape,StrokeLineJoin> STROKE_LINE_JOIN =
            new StyleableProperty<Shape,StrokeLineJoin>("-fx-stroke-line-join",
                new EnumConverter<StrokeLineJoin>(StrokeLineJoin.class), 
                StrokeLineJoin.MITER) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetLineJoin();
            }

            @Override
            public WritableValue<StrokeLineJoin> getWritableValue(Shape node) {
                return node.strokeLineJoinProperty();
            }

        };

        /**
        * @css -fx-stroke-type: [ inside | outside | centered ]
        * @see #strokeTypeProperty() 
        */        
        private static final StyleableProperty<Shape,StrokeType> STROKE_TYPE =
            new StyleableProperty<Shape,StrokeType>("-fx-stroke-type",
                new EnumConverter<StrokeType>(StrokeType.class), 
                StrokeType.CENTERED) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetType();
            }

            @Override
            public WritableValue<StrokeType> getWritableValue(Shape node) {
                return node.strokeTypeProperty();
            }


        };

        /**
        * @css -fx-stroke-miter-limit: <a href="#typesize" class="typelink">&lt;size&gt;</a>
        * @see #strokeMiterLimitProperty() 
        */        
        private static final StyleableProperty<Shape,Number> STROKE_MITER_LIMIT =
            new StyleableProperty<Shape,Number>("-fx-stroke-miter-limit",
                SizeConverter.getInstance(), 10.0) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetMiterLimit();
            }

            @Override
            public WritableValue<Number> getWritableValue(Shape node) {
                return node.strokeMiterLimitProperty();
            }

        };

        /**
        * @css -fx-stroke-width: <a href="#typesize" class="typelink">&lt;size&gt;</a>
        * @see #strokeWidthProperty() 
        */        
        private static final StyleableProperty STROKE_WIDTH =
            new StyleableProperty<Shape,Number>("-fx-stroke-width",
                SizeConverter.getInstance(), 1.0) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetWidth();
            }

            @Override
            public WritableValue<Number> getWritableValue(Shape node) {
                return node.strokeWidthProperty();
            }

        };         
         private static final List<StyleableProperty> STYLEABLES;
         static {

            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Node.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                FILL,
                SMOOTH,
                STROKE,
                STROKE_DASH_ARRAY,
                STROKE_DASH_OFFSET,
                STROKE_LINE_CAP,
                STROKE_LINE_JOIN,
                STROKE_TYPE,
                STROKE_MITER_LIMIT,
                STROKE_WIDTH
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

    /**
     * Super-lazy instantiation pattern from Bill Pugh. StyleableProperty is referenced
     * no earlier (and therefore loaded no earlier by the class loader) than
     * the moment that  impl_CSS_STYLEABLES() is called.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return Shape.StyleableProperties.STYLEABLES;
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
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds,
                                             BaseTransform tx) {
        return computeShapeBounds(bounds, tx, impl_configShape());
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        return computeShapeContains(localX, localY, impl_configShape());
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public abstract com.sun.javafx.geom.Shape impl_configShape();

    private static final double MIN_STROKE_WIDTH = 0.0f;
    private static final double MIN_STROKE_MITER_LIMIT = 1.0f;

    private void updatePGShape() {
        if (strokeAttributesDirty && (getStroke() != null)) {
            // set attributes of stroke only when stroke paint is not null
            final float[] pgDashArray =
                    (hasStrokeDashArray())
                            ? toPGDashArray(getStrokeDashArray())
                            : DEFAULT_PG_STROKE_DASH_ARRAY;

            getPGShape().setDrawStroke(
                        (float)Utils.clampMin(getStrokeWidth(),
                                              MIN_STROKE_WIDTH),
                        toPGStrokeType(getStrokeType()),
                        toPGLineCap(getStrokeLineCap()),
                        toPGLineJoin(getStrokeLineJoin()),
                        (float)Utils.clampMin(getStrokeMiterLimit(),
                                              MIN_STROKE_MITER_LIMIT),
                        pgDashArray, (float)getStrokeDashOffset());

           strokeAttributesDirty = false;
        }

        if (impl_isDirty(DirtyBits.SHAPE_MODE)) {
            getPGShape().setMode(impl_mode);
        }

        if (impl_isDirty(DirtyBits.SHAPE_FILL)) {
            Paint localFill = getFill();
            getPGShape().setFillPaint(localFill == null ? null : localFill.impl_getPlatformPaint());
        }

        if (impl_isDirty(DirtyBits.SHAPE_STROKE)) {
            Paint localStroke = getStroke();
            getPGShape().setDrawPaint(localStroke == null ? null : localStroke.impl_getPlatformPaint());
        }

        if (impl_isDirty(DirtyBits.NODE_SMOOTH)) {
            getPGShape().setAntialiased(isSmooth());
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected void impl_markDirty(DirtyBits dirtyBits) {
        if (shapeChangeListener != null && impl_isDirtyEmpty()) {
            shapeChangeListener.changed();
        }

        super.impl_markDirty(dirtyBits);
    }

    private ShapeChangeListener shapeChangeListener;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setShapeChangeListener(ShapeChangeListener listener) {
        shapeChangeListener = listener;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePG() {
        super.impl_updatePG();
        updatePGShape();
    }

    /**
     * Helper function for rectangular shapes such as Rectangle and Ellipse
     * for computing their bounds.
     */
    BaseBounds computeBounds(BaseBounds bounds, BaseTransform tx,
                                   double upad, double dpad,
                                   double x, double y,
                                   double w, double h)
    {
        // if the w or h is < 0 then bounds is empty
        if (w < 0.0f || h < 0.0f) return bounds.makeEmpty();

        double x0 = x;
        double y0 = y;
        double x1 = w;
        double y1 = h;
        double _dpad = dpad;
        if (tx.isTranslateOrIdentity()) {
            x1 += x0;
            y1 += y0;
            if (tx.getType() == BaseTransform.TYPE_TRANSLATION) {
                final double dx = tx.getMxt();
                final double dy = tx.getMyt();
                x0 += dx;
                y0 += dy;
                x1 += dx;
                y1 += dy;
            }
            _dpad += upad;
        } else {
            x0 -= upad;
            y0 -= upad;
            x1 += upad*2;
            y1 += upad*2;
            // Each corner is transformed by an equation similar to:
            //     x' = x * mxx + y * mxy + mxt
            //     y' = x * myx + y * myy + myt
            // Since all of the corners are translated by mxt,myt we
            // can ignore them when doing the min/max calculations
            // and add them in once when we are done.  We then have
            // to do min/max operations on 4 points defined as:
            //     x' = x * mxx + y * mxy
            //     y' = x * myx + y * myy
            // Furthermore, the four corners that we will be transforming
            // are not four independent coordinates, they are in a
            // rectangular formation.  To that end, if we translated
            // the transform to x,y and scaled it by width,height then
            // we could compute the min/max of the unit rectangle 0,0,1x1.
            // The transform would then be adjusted as follows:
            // First, the translation to x,y only affects the mxt,myt
            // components of the transform which we can hold off on adding
            // until we are done with the min/max.  The adjusted translation
            // components would be:
            //     mxt' = x * mxx + y * mxy + mxt
            //     myt' = x * myx + y * myy + myt
            // Second, the scale affects the components as follows:
            //     mxx' = mxx * width
            //     mxy' = mxy * height
            //     myx' = myx * width
            //     myy' = myy * height
            // The min/max of that rectangle then degenerates to:
            //     x00' = 0 * mxx' + 0 * mxy' = 0
            //     y00' = 0 * myx' + 0 * myy' = 0
            //     x01' = 0 * mxx' + 1 * mxy' = mxy'
            //     y01' = 0 * myx' + 1 * myy' = myy'
            //     x10' = 1 * mxx' + 0 * mxy' = mxx'
            //     y10' = 1 * myx' + 0 * myy' = myx'
            //     x11' = 1 * mxx' + 1 * mxy' = mxx' + mxy'
            //     y11' = 1 * myx' + 1 * myy' = myx' + myy'
            double mxx = tx.getMxx();
            double mxy = tx.getMxy();
            double myx = tx.getMyx();
            double myy = tx.getMyy();
            // Computed translated translation components
            final double mxt = (x0 * mxx + y0 * mxy + tx.getMxt());
            final double myt = (x0 * myx + y0 * myy + tx.getMyt());
            // Scale non-translation components by w/h
            mxx *= x1;
            mxy *= y1;
            myx *= x1;
            myy *= y1;
            x0 = (Math.min(Math.min(0,mxx),Math.min(mxy,mxx+mxy)))+mxt;
            y0 = (Math.min(Math.min(0,myx),Math.min(myy,myx+myy)))+myt;
            x1 = (Math.max(Math.max(0,mxx),Math.max(mxy,mxx+mxy)))+mxt;
            y1 = (Math.max(Math.max(0,myx),Math.max(myy,myx+myy)))+myt;
        }
        x0 -= _dpad;
        y0 -= _dpad;
        x1 += _dpad;
        y1 += _dpad;

        bounds = bounds.deriveWithNewBounds((float)x0, (float)y0, 0.0f,
                (float)x1, (float)y1, 0.0f);
        return bounds;
    }

    BaseBounds computeShapeBounds(BaseBounds bounds, BaseTransform tx,
                                com.sun.javafx.geom.Shape s)
    {
        // empty mode means no bounds!
        if (impl_mode == Mode.EMPTY) {
            return bounds.makeEmpty();
        }

        float[] bbox = {
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
        };
        boolean includeShape = (impl_mode != Mode.STROKE);
        boolean includeStroke = (impl_mode != Mode.FILL);
        if (includeStroke && (getStrokeType() == StrokeType.INSIDE)) {
            includeShape = true;
            includeStroke = false;
        }

        if (includeStroke) {
            PGShape.StrokeType type = toPGStrokeType(getStrokeType());
            double sw = Utils.clampMin(getStrokeWidth(), MIN_STROKE_WIDTH);
            PGShape.StrokeLineCap cap = toPGLineCap(getStrokeLineCap());
            PGShape.StrokeLineJoin join = toPGLineJoin(getStrokeLineJoin());
            float miterlimit =
                (float) Utils.clampMin(getStrokeMiterLimit(), MIN_STROKE_MITER_LIMIT);
            // Note that we ignore dashing for computing bounds and testing
            // point containment, both to save time in bounds calculations
            // and so that animated dashing does not keep perturbing the bounds...
            Toolkit.getToolkit().accumulateStrokeBounds(
                    s,
                    bbox, type, sw,
                    cap, join, miterlimit, tx);
            // Account for "minimum pen size" by expanding by 0.5 device
            // pixels all around...
            bbox[0] -= 0.5;
            bbox[1] -= 0.5;
            bbox[2] += 0.5;
            bbox[3] += 0.5;
        } else if (includeShape) {
            com.sun.javafx.geom.Shape.accumulate(bbox, s, tx);
        }

        if (bbox[2] < bbox[0] || bbox[3] < bbox[1]) {
            // They are probably +/-INFINITY which would yield NaN if subtracted
            // Let's just return a "safe" empty bbox..
            return bounds.makeEmpty();
        }
        bounds = bounds.deriveWithNewBounds(bbox[0], bbox[1], 0.0f,
                bbox[2], bbox[3], 0.0f);
        return bounds;
    }

    boolean computeShapeContains(double localX, double localY,
                                 com.sun.javafx.geom.Shape s) {
        if (impl_mode == Mode.EMPTY) {
            return false;
        }

        boolean includeShape = (impl_mode != Mode.STROKE);
        boolean includeStroke = (impl_mode != Mode.FILL);
        if (includeStroke && includeShape &&
            (getStrokeType() == StrokeType.INSIDE))
        {
            includeStroke = false;
        }

        if (includeShape) {
            if (s.contains((float)localX, (float)localY)) {
                return true;
            }
        }

        if (includeStroke) {
            PGShape.StrokeType type = toPGStrokeType(getStrokeType());
            double sw = Utils.clampMin(getStrokeWidth(), MIN_STROKE_WIDTH);
            PGShape.StrokeLineCap cap = toPGLineCap(getStrokeLineCap());
            PGShape.StrokeLineJoin join = toPGLineJoin(getStrokeLineJoin());
            float miterlimit =
                (float) Utils.clampMin(getStrokeMiterLimit(), MIN_STROKE_MITER_LIMIT);
            // Note that we ignore dashing for computing bounds and testing
            // point containment, both to save time in bounds calculations
            // and so that animated dashing does not keep perturbing the bounds...
            return Toolkit.getToolkit().strokeContains(s, localX, localY,
                                                       type, sw, cap,
                                                       join, miterlimit);
        }

        return false;
    }

    private boolean strokeAttributesDirty = true;

    private StrokeAttributes strokeAttributes;

    private StrokeAttributes getStrokeAttributes() {
        if (strokeAttributes == null) {
            strokeAttributes = new StrokeAttributes();
        }

        return strokeAttributes;
    }

    private boolean hasStrokeDashArray() {
        return (strokeAttributes != null) && strokeAttributes.hasDashArray();
    }

    private static float[] toPGDashArray(final List<Double> dashArray) {
        final int size = dashArray.size();
        final float[] pgDashArray = new float[size];
        for (int i = 0; i < size; i++) {
            pgDashArray[i] = dashArray.get(i).floatValue();
        }

        return pgDashArray;
    }

    private static final StrokeType DEFAULT_STROKE_TYPE = StrokeType.CENTERED;
    private static final double DEFAULT_STROKE_WIDTH = 1.0;
    private static final StrokeLineJoin DEFAULT_STROKE_LINE_JOIN =
            StrokeLineJoin.MITER;
    private static final StrokeLineCap DEFAULT_STROKE_LINE_CAP = 
            StrokeLineCap.SQUARE;
    private static final double DEFAULT_STROKE_MITER_LIMIT = 10.0;
    private static final double DEFAULT_STROKE_DASH_OFFSET = 0;
    private static final float[] DEFAULT_PG_STROKE_DASH_ARRAY = new float[0];

    private final class StrokeAttributes {
        private ObjectProperty<StrokeType> type;
        private DoubleProperty width;
        private ObjectProperty<StrokeLineJoin> lineJoin;
        private ObjectProperty<StrokeLineCap> lineCap;
        private DoubleProperty miterLimit;
        private DoubleProperty dashOffset;
        private ObservableList<Double> dashArray;

        public final StrokeType getType() {
            return (type == null) ? DEFAULT_STROKE_TYPE : type.get();
        }

        public final ObjectProperty<StrokeType> typeProperty() {
            if (type == null) {
                type = new StyleableObjectProperty<StrokeType>(DEFAULT_STROKE_TYPE) {
                
                    @Override
                    public void invalidated() {
                        StrokeAttributes.this.invalidated(
                                StyleableProperties.STROKE_TYPE);
                    }
                
                    @Override
                    public StyleableProperty getStyleableProperty() {
                        return StyleableProperties.STROKE_TYPE;
                    }

                    @Override
                    public Object getBean() {
                        return Shape.this;
                    }

                    @Override
                    public String getName() {
                        return "strokeType";
                    }
                };
            }
            return type;
        }

        public double getWidth() {
            return (width == null) ? DEFAULT_STROKE_WIDTH : width.get();
        }

        public final DoubleProperty widthProperty() {
            if (width == null) {
                width = new StyleableDoubleProperty(DEFAULT_STROKE_WIDTH) {
                
                    @Override
                    public void invalidated() {
                        StrokeAttributes.this.invalidated(
                                StyleableProperties.STROKE_WIDTH);
                    }
                
                    @Override
                    public StyleableProperty getStyleableProperty() {
                        return StyleableProperties.STROKE_WIDTH;
                    }

                    @Override
                    public Object getBean() {
                        return Shape.this;
                    }

                    @Override
                    public String getName() {
                        return "strokeWidth";
                    }
                };
            }
            return width;
        }

        public StrokeLineJoin getLineJoin() {
            return (lineJoin == null) ? DEFAULT_STROKE_LINE_JOIN
                                      : lineJoin.get();
        }

        public final ObjectProperty<StrokeLineJoin> lineJoinProperty() {
            if (lineJoin == null) {
                lineJoin = new StyleableObjectProperty<StrokeLineJoin>(
                                       DEFAULT_STROKE_LINE_JOIN) {
                
                    @Override
                    public void invalidated() {
                        StrokeAttributes.this.invalidated(
                                StyleableProperties.STROKE_LINE_JOIN);
                    }
                
                    @Override
                    public StyleableProperty getStyleableProperty() {
                        return StyleableProperties.STROKE_LINE_JOIN;
                    }

                    @Override
                    public Object getBean() {
                        return Shape.this;
                    }

                    @Override
                    public String getName() {
                        return "strokeLineJoin";
                    }
                };
            }
            return lineJoin;
        }

        public StrokeLineCap getLineCap() {
            return (lineCap == null) ? DEFAULT_STROKE_LINE_CAP
                                     : lineCap.get();
        }

        public final ObjectProperty<StrokeLineCap> lineCapProperty() {
            if (lineCap == null) {
                lineCap = new StyleableObjectProperty<StrokeLineCap>(
                                      DEFAULT_STROKE_LINE_CAP) {
                
                    @Override
                    public void invalidated() {
                        StrokeAttributes.this.invalidated(
                                StyleableProperties.STROKE_LINE_CAP);
                    }
                
                    @Override
                    public StyleableProperty getStyleableProperty() {
                        return StyleableProperties.STROKE_LINE_CAP;
                    }

                    @Override
                    public Object getBean() {
                        return Shape.this;
                    }

                    @Override
                    public String getName() {
                        return "strokeLineCap";
                    }
                };
            }

            return lineCap;
        }

        public double getMiterLimit() {
            return (miterLimit == null) ? DEFAULT_STROKE_MITER_LIMIT
                                        : miterLimit.get();
        }

        public final DoubleProperty miterLimitProperty() {
            if (miterLimit == null) {
                miterLimit = new StyleableDoubleProperty(
                                         DEFAULT_STROKE_MITER_LIMIT) {
                    @Override
                    public void invalidated() {
                        StrokeAttributes.this.invalidated(
                                StyleableProperties.STROKE_MITER_LIMIT);
                    }
                
                    @Override
                    public StyleableProperty getStyleableProperty() {
                        return StyleableProperties.STROKE_MITER_LIMIT;
                    }

                    @Override
                    public Object getBean() {
                        return Shape.this;
                    }

                    @Override
                    public String getName() {
                        return "strokeMiterLimit";
                    }
                };
            }

            return miterLimit;
        }

        public double getDashOffset() {
            return (dashOffset == null) ? DEFAULT_STROKE_DASH_OFFSET
                                        : dashOffset.get();
        }

        public final DoubleProperty dashOffsetProperty() {
            if (dashOffset == null) {
                dashOffset = new StyleableDoubleProperty(
                                         DEFAULT_STROKE_DASH_OFFSET) {
                                             
                    @Override
                    public void invalidated() {
                        StrokeAttributes.this.invalidated(
                                StyleableProperties.STROKE_DASH_OFFSET);
                    }
                
                    @Override
                    public StyleableProperty getStyleableProperty() {
                        return StyleableProperties.STROKE_DASH_OFFSET;
                    }

                    @Override
                    public Object getBean() {
                        return Shape.this;
                    }

                    @Override
                    public String getName() {
                        return "strokeDashOffset";
                    }
                };
            }

            return dashOffset;
        }

        // TODO: Need to handle set from css - should clear array and add all.  
        public ObservableList<Double> dashArrayProperty() {
            if (dashArray == null) {
                dashArray = new TrackableObservableList<Double>() {
                    @Override
                    protected void onChanged(Change<Double> c) {
                        StrokeAttributes.this.invalidated(
                                StyleableProperties.STROKE_DASH_ARRAY);
                    }
                }; 
            }
            return dashArray;
        }
        
        private ObjectProperty<Double[]> cssDashArray = null;
        private ObjectProperty<Double[]> cssDashArrayProperty() {
            if (cssDashArray == null) {
                cssDashArray = new StyleableObjectProperty<Double[]>() 
                {

                    @Override
                    public void set(Double[] v) {
                        
                        ObservableList<Double> list = dashArrayProperty();
                        list.clear();
                        if (v != null && v.length > 0) {
                            list.addAll(v);
                        }
                        
                        // no need to hold onto the array
                    }

                    @Override
                    public Double[] get() {
                        List<Double> list = dashArrayProperty();
                        return list.toArray(new Double[list.size()]);
                    }

                    @Override
                    public Object getBean() {
                        return Shape.this;
                    }

                    @Override
                    public String getName() {
                        return "cssDashArray";
                    }

                    @Override
                    public StyleableProperty getStyleableProperty() {
                        return StyleableProperties.STROKE_DASH_ARRAY;
                    }
                };
            }

            return cssDashArray;
        }

        public boolean canSetType() {
            return (type == null) || !type.isBound();
        }

        public boolean canSetWidth() {
            return (width == null) || !width.isBound();
        }

        public boolean canSetLineJoin() {
            return (lineJoin == null) || !lineJoin.isBound();
        }

        public boolean canSetLineCap() {
            return (lineCap == null) || !lineCap.isBound();
        }

        public boolean canSetMiterLimit() {
            return (miterLimit == null) || !miterLimit.isBound();
        }

        public boolean canSetDashOffset() {
            return (dashOffset == null) || !dashOffset.isBound();
        }

        public boolean hasDashArray() {
            return (dashArray != null);
        }

        private void invalidated(final StyleableProperty propertyCssKey) {
            impl_markDirty(DirtyBits.SHAPE_STROKEATTRS);
            strokeAttributesDirty = true;
            if (propertyCssKey != StyleableProperties.STROKE_DASH_OFFSET) {
                // all stroke attributes change geometry except for the
                // stroke dash offset
                impl_geomChanged();
            }
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        return alg.processLeafNode(this, ctx);
    }

    // PENDING_DOC_REVIEW
    /**
     * Returns a new {@code Shape} which is created as a union of the specified
     * input shapes.
     * <p>
     * The operation works with geometric areas occupied by the input shapes.
     * For a single {@code Shape} such area includes the area occupied by the
     * fill if the shape has a non-null fill and the area occupied by the stroke
     * if the shape has a non-null stroke. So the area is empty for a shape
     * with {@code null} stroke and {@code null} fill. The area of an input
     * shape considered by the operation is independent on the type and
     * configuration of the paint used for fill or stroke. Before the final
     * operation the areas of the input shapes are transformed to the parent
     * coordinate space of their respective topmost parent nodes.
     * <p>
     * The resulting shape will include areas that were contained in any of the
     * input shapes.
     * <p>

<PRE>

         shape1       +       shape2       =       result
   +----------------+   +----------------+   +----------------+
   |################|   |################|   |################|
   |##############  |   |  ##############|   |################|
   |############    |   |    ############|   |################|
   |##########      |   |      ##########|   |################|
   |########        |   |        ########|   |################|
   |######          |   |          ######|   |######    ######|
   |####            |   |            ####|   |####        ####|
   |##              |   |              ##|   |##            ##|
   +----------------+   +----------------+   +----------------+

</PRE>

     * @param shape1 the first shape
     * @param shape2 the second shape
     * @return the created {@code Shape}
     */
    public static Shape union(final Shape shape1, final Shape shape2) {
        final Area result = shape1.getTransformedArea();
        result.add(shape2.getTransformedArea());
        return createFromGeomShape(result);
    }

    // PENDING_DOC_REVIEW
    /**
     * Returns a new {@code Shape} which is created by subtracting the specified
     * second shape from the first shape.
     * <p>
     * The operation works with geometric areas occupied by the input shapes.
     * For a single {@code Shape} such area includes the area occupied by the
     * fill if the shape has a non-null fill and the area occupied by the stroke
     * if the shape has a non-null stroke. So the area is empty for a shape
     * with {@code null} stroke and {@code null} fill. The area of an input
     * shape considered by the operation is independent on the type and
     * configuration of the paint used for fill or stroke. Before the final
     * operation the areas of the input shapes are transformed to the parent
     * coordinate space of their respective topmost parent nodes.
     * <p>
     * The resulting shape will include areas that were contained only in the
     * first shape and not in the second shape.
     * <p>

<PRE>

         shape1       -       shape2       =       result
   +----------------+   +----------------+   +----------------+
   |################|   |################|   |                |
   |##############  |   |  ##############|   |##              |
   |############    |   |    ############|   |####            |
   |##########      |   |      ##########|   |######          |
   |########        |   |        ########|   |########        |
   |######          |   |          ######|   |######          |
   |####            |   |            ####|   |####            |
   |##              |   |              ##|   |##              |
   +----------------+   +----------------+   +----------------+

</PRE>

     * @param shape1 the first shape
     * @param shape2 the second shape
     * @return the created {@code Shape}
     */
    public static Shape subtract(final Shape shape1, final Shape shape2) {
        final Area result = shape1.getTransformedArea();
        result.subtract(shape2.getTransformedArea());
        return createFromGeomShape(result);
    }

    // PENDING_DOC_REVIEW
    /**
     * Returns a new {@code Shape} which is created as an intersection of the
     * specified input shapes.
     * <p>
     * The operation works with geometric areas occupied by the input shapes.
     * For a single {@code Shape} such area includes the area occupied by the
     * fill if the shape has a non-null fill and the area occupied by the stroke
     * if the shape has a non-null stroke. So the area is empty for a shape
     * with {@code null} stroke and {@code null} fill. The area of an input
     * shape considered by the operation is independent on the type and
     * configuration of the paint used for fill or stroke. Before the final
     * operation the areas of the input shapes are transformed to the parent
     * coordinate space of their respective topmost parent nodes.
     * <p>
     * The resulting shape will include only areas that were contained in both
     * of the input shapes.
     * <p>

<PRE>

         shape1       +       shape2       =       result
   +----------------+   +----------------+   +----------------+
   |################|   |################|   |################|
   |##############  |   |  ##############|   |  ############  |
   |############    |   |    ############|   |    ########    |
   |##########      |   |      ##########|   |      ####      |
   |########        |   |        ########|   |                |
   |######          |   |          ######|   |                |
   |####            |   |            ####|   |                |
   |##              |   |              ##|   |                |
   +----------------+   +----------------+   +----------------+

</PRE>

     * @param shape1 the first shape
     * @param shape2 the second shape
     * @return the created {@code Shape}
     */
    public static Shape intersect(final Shape shape1, final Shape shape2) {
        final Area result = shape1.getTransformedArea();
        result.intersect(shape2.getTransformedArea());
        return createFromGeomShape(result);
    }

    private Area getTransformedArea() {
        return getTransformedArea(calculateNodeToSceneTransform(this));
    }

    private Area getTransformedArea(final BaseTransform transform) {
        if (impl_mode == Mode.EMPTY) {
            return new Area();
        }

        final com.sun.javafx.geom.Shape fillShape = impl_configShape();
        if ((impl_mode == Mode.FILL)
                || (impl_mode == Mode.STROKE_FILL)
                       && (getStrokeType() == StrokeType.INSIDE)) {
            return createTransformedArea(fillShape, transform);
        }

        final PGShape.StrokeType strokeType =
                toPGStrokeType(getStrokeType());
        final double strokeWidth =
                Utils.clampMin(getStrokeWidth(), MIN_STROKE_WIDTH);
        final PGShape.StrokeLineCap strokeLineCap =
                toPGLineCap(getStrokeLineCap());
        final PGShape.StrokeLineJoin strokeLineJoin =
                toPGLineJoin(getStrokeLineJoin());
        final float strokeMiterLimit =
                (float) Utils.clampMin(getStrokeMiterLimit(),
                                       MIN_STROKE_MITER_LIMIT);
        final float[] dashArray =
                (hasStrokeDashArray())
                        ? toPGDashArray(getStrokeDashArray())
                        : DEFAULT_PG_STROKE_DASH_ARRAY;

        final com.sun.javafx.geom.Shape strokeShape =
                Toolkit.getToolkit().createStrokedShape(
                        fillShape, strokeType, strokeWidth, strokeLineCap,
                        strokeLineJoin, strokeMiterLimit,
                        dashArray, (float) getStrokeDashOffset());

        if (impl_mode == Mode.STROKE) {
            return createTransformedArea(strokeShape, transform);
        }

        // fill and stroke
        final Area combinedArea = new Area(fillShape);
        combinedArea.add(new Area(strokeShape));

        return createTransformedArea(combinedArea, transform);
    }

    private static BaseTransform calculateNodeToSceneTransform(Node node) {
        final Affine3D cumulativeTransformation = new Affine3D();

        do {
            cumulativeTransformation.preConcatenate(
                    node.impl_getLeafTransform());
            node = node.getParent();
        } while (node != null);

        return cumulativeTransformation;
    }

    private static Area createTransformedArea(
            final com.sun.javafx.geom.Shape geomShape,
            final BaseTransform transform) {
        return transform.isIdentity()
                   ? new Area(geomShape)
                   : new Area(geomShape.getPathIterator(transform));
    }

    private static Path createFromGeomShape(
            final com.sun.javafx.geom.Shape geomShape) {
        final Path path = new Path();
        final ObservableList<PathElement> elements = path.getElements();

        final PathIterator iterator = geomShape.getPathIterator(null);
        final float coords[] = new float[6];

        while (!iterator.isDone()) {
            final int segmentType = iterator.currentSegment(coords);
            switch (segmentType) {
                case PathIterator.SEG_MOVETO:
                    elements.add(new MoveTo(coords[0], coords[1]));
                    break;
                case PathIterator.SEG_LINETO:
                    elements.add(new LineTo(coords[0], coords[1]));
                    break;
                case PathIterator.SEG_QUADTO:
                    elements.add(new QuadCurveTo(coords[0], coords[1],
                                                 coords[2], coords[3]));
                    break;
                case PathIterator.SEG_CUBICTO:
                    elements.add(new CubicCurveTo(coords[0], coords[1],
                                                  coords[2], coords[3],
                                                  coords[4], coords[5]));
                    break;
                case PathIterator.SEG_CLOSE:
                    elements.add(new ClosePath());
                    break;
            }

            iterator.next();
        }

        path.setFillRule((iterator.getWindingRule()
                             == PathIterator.WIND_EVEN_ODD)
                                 ? FillRule.EVEN_ODD
                                 : FillRule.NON_ZERO);

        path.setFill(Color.BLACK);
        path.setStroke(null);

        return path;
    }
}
