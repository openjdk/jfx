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

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.sun.javafx.util.Utils;
import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.collections.TrackableObservableList;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.SizeConverter;
import com.sun.javafx.geom.Area;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.shape.ShapeHelper;
import com.sun.javafx.sg.prism.NGShape;
import com.sun.javafx.tk.Toolkit;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;


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
 *
 * <p>
 * An application should not extend the Shape class directly. Doing so may lead to
 * an UnsupportedOperationException being thrown.
 * </p>
 *
 * <h2>Interaction with coordinate systems</h2>
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
 * @since JavaFX 2.0
 */
public abstract class Shape extends Node {

    static {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        ShapeHelper.setShapeAccessor(new ShapeHelper.ShapeAccessor() {
            @Override
            public void doUpdatePeer(Node node) {
                ((Shape) node).doUpdatePeer();
            }

            @Override
            public void doMarkDirty(Node node, DirtyBits dirtyBit) {
                ((Shape) node).doMarkDirty(dirtyBit);
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((Shape) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((Shape) node).doComputeContains(localX, localY);
            }

            @Override
            public Paint doCssGetFillInitialValue(Shape shape) {
                return shape.doCssGetFillInitialValue();
            }

            @Override
            public Paint doCssGetStrokeInitialValue(Shape shape) {
                return shape.doCssGetStrokeInitialValue();
            }

            @Override
            public NGShape.Mode getMode(Shape shape) {
                return shape.getMode();
            }

            @Override
            public void setShapeChangeListener(Shape shape, Runnable listener) {
                shape.setShapeChangeListener(listener);
            }
        });
    }

    /**
     * Creates an empty instance of Shape.
     */
    public Shape() {
    }

    StrokeLineJoin convertLineJoin(StrokeLineJoin t) {
        return t;
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
     * </p>
     * <p> <img src="doc-files/stroketype.png" alt="A visual illustration of how
     * StrokeType works"> </p>
     *
     * @return the direction that the strokeWidth is applied to the boundary of
     * the shape
     * @see StrokeType
     * @defaultValue CENTERED
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
     * @return the square pen line width
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
     * <p> <img src="doc-files/strokelinejoin.png" alt="A visual illustration of
     * StrokeLineJoin using 3 different values"> </p>
     *
     * @return the decoration applied where path segments meet
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
     * <p> <img src="doc-files/strokelinecap.png" alt="A visual illustration of
     * StrokeLineCap using 3 different values"> </p>
     *
     * @return the end cap style of this shape
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
     * </p>
     * <p> <img src="doc-files/strokemiterlimit.png" alt="A visual illustration of
     * the use of StrokeMiterLimit"> </p>
     *
     * @return the limit for the {@code StrokeLineJoin.MITER} line join style
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
     * </p>
     * <p> <img src="doc-files/strokedashoffset.png" alt="A visual illustration of
     * the use of StrokeDashOffset"> </p>
     *
     * @return the distance specified in user coordinates that represents an
     * offset into the dashing pattern
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
     * to an even element of the dash array (counting from {@code 0}) and
     * transparent otherwise.
     * <p>
     * An empty strokeDashArray indicates a solid line with no spaces.
     * An odd length strokeDashArray behaves the same as an even length
     * array constructed by implicitly repeating the indicated odd length
     * array twice in succession ({@code [20, 5, 15]} behaves as if it
     * were {@code [20, 5, 15, 20, 5, 15]}).
     * <p>
     * Note that each dash segment will be capped by the decoration specified
     * by the current stroke line cap.
     *
     * <p>
     * The image shows a shape with stroke dash array {@code [25, 20, 5, 20]}
     * and 3 different values for the stroke line cap:
     * {@code StrokeLineCap.BUTT}, {@code StrokeLineCap.SQUARE} (the default),
     * and {@code StrokeLineCap.ROUND}
     * </p>
     * <p> <img src="doc-files/strokedasharray.png" alt="A visual illustration of
     * the use of StrokeDashArray using 3 different values for the stroke line
     * cap"> </p>
     *
     * @return the array representing the lengths of the dash segments
     * @defaultValue empty
     */
    public final ObservableList<Double> getStrokeDashArray() {
        return getStrokeAttributes().dashArrayProperty();
    }

    private NGShape.Mode computeMode() {
        if (getFill() != null && getStroke() != null) {
            return NGShape.Mode.STROKE_FILL;
        } else if (getFill() != null) {
            return NGShape.Mode.FILL;
        } else if (getStroke() != null) {
            return NGShape.Mode.STROKE;
        } else {
            return NGShape.Mode.EMPTY;
        }
    }

    NGShape.Mode getMode() {
        return mode;
    }

    private NGShape.Mode mode = NGShape.Mode.FILL;

    private void checkModeChanged() {
        NGShape.Mode newMode = computeMode();
        if (mode != newMode) {
            mode = newMode;

            NodeHelper.markDirty(this, DirtyBits.SHAPE_MODE);
            NodeHelper.geomChanged(this);
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

    Paint old_fill;
    public final ObjectProperty<Paint> fillProperty() {
        if (fill == null) {
            fill = new StyleableObjectProperty<Paint>(Color.BLACK) {

                boolean needsListener = false;

                @Override public void invalidated() {

                    Paint _fill = get();

                    if (needsListener) {
                        Toolkit.getPaintAccessor().
                                removeListener(old_fill, platformImageChangeListener);
                    }
                    needsListener = _fill != null &&
                            Toolkit.getPaintAccessor().isMutable(_fill);
                    old_fill = _fill;

                    if (needsListener) {
                        Toolkit.getPaintAccessor().
                                addListener(_fill, platformImageChangeListener);
                    }

                    NodeHelper.markDirty(Shape.this, DirtyBits.SHAPE_FILL);
                    checkModeChanged();
                }

                @Override
                public CssMetaData<Shape,Paint> getCssMetaData() {
                    return StyleableProperties.FILL;
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

    private final AbstractNotifyListener platformImageChangeListener =
            new AbstractNotifyListener() {
        @Override
        public void invalidated(Observable valueModel) {
            NodeHelper.markDirty(Shape.this, DirtyBits.SHAPE_FILL);
            NodeHelper.markDirty(Shape.this, DirtyBits.SHAPE_STROKE);
            NodeHelper.geomChanged(Shape.this);
            checkModeChanged();
        }
    };

    public final Paint getStroke() {
        return stroke == null ? null : stroke.get();
    }

    Paint old_stroke;
    public final ObjectProperty<Paint> strokeProperty() {
        if (stroke == null) {
            stroke = new StyleableObjectProperty<>() {

                boolean needsListener = false;

                @Override public void invalidated() {

                    Paint _stroke = get();

                    if (needsListener) {
                        Toolkit.getPaintAccessor().
                                removeListener(old_stroke, platformImageChangeListener);
                    }
                    needsListener = _stroke != null &&
                            Toolkit.getPaintAccessor().isMutable(_stroke);
                    old_stroke = _stroke;

                    if (needsListener) {
                        Toolkit.getPaintAccessor().
                                addListener(_stroke, platformImageChangeListener);
                    }

                    NodeHelper.markDirty(Shape.this, DirtyBits.SHAPE_STROKE);
                    checkModeChanged();
                }

                @Override
                public CssMetaData<Shape,Paint> getCssMetaData() {
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
                    NodeHelper.markDirty(Shape.this, DirtyBits.NODE_SMOOTH);
                }

                @Override
                public CssMetaData<Shape,Boolean> getCssMetaData() {
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
        return Color.BLACK;
    }

    /*
     * Some sub-class of Shape, such as {@link Line}, override the
     * default value for the {@link Shape#stroke} property. This allows
     * CSS to get the correct initial value.
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private Paint doCssGetStrokeInitialValue() {
        return null;
    }


    /*
     * Super-lazy instantiation pattern from Bill Pugh.
     */
     private static class StyleableProperties {

        /**
        * @css -fx-fill: <a href="../doc-files/cssref.html#typepaint">&lt;paint&gt;</a>
        * @see Shape#fill
        */
        private static final CssMetaData<Shape,Paint> FILL =
            new CssMetaData<>("-fx-fill",
                PaintConverter.getInstance(), Color.BLACK) {

            @Override
            public boolean isSettable(Shape node) {
                return node.fill == null || !node.fill.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(Shape node) {
                return (StyleableProperty<Paint>)node.fillProperty();
            }

            @Override
            public Paint getInitialValue(Shape node) {
                // Some shapes have a different initial value for fill.
                // Give a way to have them return the correct initial value.
                return ShapeHelper.cssGetFillInitialValue(node);
            }

        };

        /**
        * @css -fx-smooth: <a href="../doc-files/cssref.html#typeboolean">&lt;boolean&gt;</a>
        * @see Shape#smooth
        */
        private static final CssMetaData<Shape,Boolean> SMOOTH =
            new CssMetaData<>("-fx-smooth",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(Shape node) {
                return node.smooth == null || !node.smooth.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(Shape node) {
                return (StyleableProperty<Boolean>)node.smoothProperty();
            }

        };

        /**
        * @css -fx-stroke: <a href="../doc-files/cssref.html#typepaint">&lt;paint&gt;</a>
        * @see Shape#stroke
        */
        private static final CssMetaData<Shape,Paint> STROKE =
            new CssMetaData<>("-fx-stroke",
                PaintConverter.getInstance()) {

            @Override
            public boolean isSettable(Shape node) {
                return node.stroke == null || !node.stroke.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(Shape node) {
                return (StyleableProperty<Paint>)node.strokeProperty();
            }

            @Override
            public Paint getInitialValue(Shape node) {
                // Some shapes have a different initial value for stroke.
                // Give a way to have them return the correct initial value.
                return ShapeHelper.cssGetStrokeInitialValue(node);
            }


        };

        /**
        * @css -fx-stroke-dash-array: <a href="#typesize" class="typelink">&lt;size&gt;</a>
        *                    [<a href="#typesize" class="typelink">&lt;size&gt;</a>]+
        * <p>
        * Note:
        * Because {@link StrokeAttributes#dashArray} is not itself a
        * {@link Property},
        * the <code>getProperty()</code> method of this CssMetaData
        * returns the {@link StrokeAttributes#dashArray} wrapped in an
        * {@link ObjectProperty}. This is inconsistent with other
        * StyleableProperties which return the actual {@link Property}.
        * </p>
        * @see StrokeAttributes#dashArray
        */
        private static final CssMetaData<Shape,Number[]> STROKE_DASH_ARRAY =
            new CssMetaData<>("-fx-stroke-dash-array",
                SizeConverter.SequenceConverter.getInstance(),
                new Double[0]) {

            @Override
            public boolean isSettable(Shape node) {
                return true;
            }

            @Override
            public StyleableProperty<Number[]> getStyleableProperty(final Shape node) {
                return (StyleableProperty<Number[]>)node.getStrokeAttributes().cssDashArrayProperty();
            }

        };

        /**
        * @css -fx-stroke-dash-offset: <a href="#typesize" class="typelink">&lt;size&gt;</a>
        * @see #strokeDashOffsetProperty()
        */
        private static final CssMetaData<Shape,Number> STROKE_DASH_OFFSET =
            new CssMetaData<>("-fx-stroke-dash-offset",
                SizeConverter.getInstance(), 0.0) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetDashOffset();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(Shape node) {
                return (StyleableProperty<Number>)node.strokeDashOffsetProperty();
            }

        };

        /**
        * @css -fx-stroke-line-cap: [ square | butt | round ]
        * @see #strokeLineCapProperty()
        */
        private static final CssMetaData<Shape,StrokeLineCap> STROKE_LINE_CAP =
            new CssMetaData<>("-fx-stroke-line-cap",
                new EnumConverter<>(StrokeLineCap.class),
                StrokeLineCap.SQUARE) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetLineCap();
            }

            @Override
            public StyleableProperty<StrokeLineCap> getStyleableProperty(Shape node) {
                return (StyleableProperty<StrokeLineCap>)node.strokeLineCapProperty();
            }

        };

        /**
        * @css -fx-stroke-line-join: [ miter | bevel | round ]
        * @see #strokeLineJoinProperty()
        */
        private static final CssMetaData<Shape,StrokeLineJoin> STROKE_LINE_JOIN =
            new CssMetaData<>("-fx-stroke-line-join",
                new EnumConverter<>(StrokeLineJoin.class),
                StrokeLineJoin.MITER) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetLineJoin();
            }

            @Override
            public StyleableProperty<StrokeLineJoin> getStyleableProperty(Shape node) {
                return (StyleableProperty<StrokeLineJoin>)node.strokeLineJoinProperty();
            }

        };

        /**
        * @css -fx-stroke-type: [ inside | outside | centered ]
        * @see #strokeTypeProperty()
        */
        private static final CssMetaData<Shape,StrokeType> STROKE_TYPE =
            new CssMetaData<>("-fx-stroke-type",
                new EnumConverter<>(StrokeType.class),
                StrokeType.CENTERED) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetType();
            }

            @Override
            public StyleableProperty<StrokeType> getStyleableProperty(Shape node) {
                return (StyleableProperty<StrokeType>)node.strokeTypeProperty();
            }


        };

        /**
        * @css -fx-stroke-miter-limit: <a href="#typesize" class="typelink">&lt;size&gt;</a>
        * @see #strokeMiterLimitProperty()
        */
        private static final CssMetaData<Shape,Number> STROKE_MITER_LIMIT =
            new CssMetaData<>("-fx-stroke-miter-limit",
                SizeConverter.getInstance(), 10.0) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetMiterLimit();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(Shape node) {
                return (StyleableProperty<Number>)node.strokeMiterLimitProperty();
            }

        };

        /**
        * @css -fx-stroke-width: <a href="#typesize" class="typelink">&lt;size&gt;</a>
        * @see #strokeWidthProperty()
        */
        private static final CssMetaData<Shape,Number> STROKE_WIDTH =
            new CssMetaData<>("-fx-stroke-width",
                SizeConverter.getInstance(), 1.0) {

            @Override
            public boolean isSettable(Shape node) {
                return node.strokeAttributes == null ||
                        node.strokeAttributes.canSetWidth();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(Shape node) {
                return (StyleableProperty<Number>)node.strokeWidthProperty();
            }

        };
         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(Node.getClassCssMetaData());
            styleables.add(FILL);
            styleables.add(SMOOTH);
            styleables.add(STROKE);
            styleables.add(STROKE_DASH_ARRAY);
            styleables.add(STROKE_DASH_OFFSET);
            styleables.add(STROKE_LINE_CAP);
            styleables.add(STROKE_LINE_JOIN);
            styleables.add(STROKE_TYPE);
            styleables.add(STROKE_MITER_LIMIT);
            styleables.add(STROKE_WIDTH);
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     *
     * @since JavaFX 8.0
     */


    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds,
                                             BaseTransform tx) {
        return computeShapeBounds(bounds, tx, ShapeHelper.configShape(this));
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        return computeShapeContains(localX, localY, ShapeHelper.configShape(this));
    }

    private static final double MIN_STROKE_WIDTH = 0.0f;
    private static final double MIN_STROKE_MITER_LIMIT = 1.0f;

    private void updatePGShape() {
        final NGShape peer = NodeHelper.getPeer(this);
        if (strokeAttributesDirty && (getStroke() != null)) {
            // set attributes of stroke only when stroke paint is not null
            final float[] pgDashArray =
                    (hasStrokeDashArray())
                            ? toPGDashArray(getStrokeDashArray())
                            : DEFAULT_PG_STROKE_DASH_ARRAY;

            peer.setDrawStroke(
                        (float)Utils.clampMin(getStrokeWidth(),
                                              MIN_STROKE_WIDTH),
                        getStrokeType(),
                        getStrokeLineCap(),
                        convertLineJoin(getStrokeLineJoin()),
                        (float)Utils.clampMin(getStrokeMiterLimit(),
                                              MIN_STROKE_MITER_LIMIT),
                        pgDashArray, (float)getStrokeDashOffset());

           strokeAttributesDirty = false;
        }

        if (NodeHelper.isDirty(this, DirtyBits.SHAPE_MODE)) {
            peer.setMode(mode);
        }

        if (NodeHelper.isDirty(this, DirtyBits.SHAPE_FILL)) {
            Paint localFill = getFill();
            peer.setFillPaint(localFill == null ? null :
                    Toolkit.getPaintAccessor().getPlatformPaint(localFill));
        }

        if (NodeHelper.isDirty(this, DirtyBits.SHAPE_STROKE)) {
            Paint localStroke = getStroke();
            peer.setDrawPaint(localStroke == null ? null :
                    Toolkit.getPaintAccessor().getPlatformPaint(localStroke));
        }

        if (NodeHelper.isDirty(this, DirtyBits.NODE_SMOOTH)) {
            peer.setSmooth(isSmooth());
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doMarkDirty(DirtyBits dirtyBits) {
        final Runnable listener = shapeChangeListener != null ? shapeChangeListener.get() : null;
        if (listener != null && NodeHelper.isDirtyEmpty(this)) {
            listener.run();
        }
    }

    private Reference<Runnable> shapeChangeListener;

    void setShapeChangeListener(Runnable listener) {
        if (shapeChangeListener != null) shapeChangeListener.clear();
        shapeChangeListener = listener != null ? new WeakReference(listener) : null;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
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
        if (mode == NGShape.Mode.EMPTY) {
            return bounds.makeEmpty();
        }

        float[] bbox = {
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
        };
        boolean includeShape = (mode != NGShape.Mode.STROKE);
        boolean includeStroke = (mode != NGShape.Mode.FILL);
        if (includeStroke && (getStrokeType() == StrokeType.INSIDE)) {
            includeShape = true;
            includeStroke = false;
        }

        if (includeStroke) {
            final StrokeType type = getStrokeType();
            double sw = Utils.clampMin(getStrokeWidth(), MIN_STROKE_WIDTH);
            StrokeLineCap cap = getStrokeLineCap();
            StrokeLineJoin join = convertLineJoin(getStrokeLineJoin());
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
        if (mode == NGShape.Mode.EMPTY) {
            return false;
        }

        boolean includeShape = (mode != NGShape.Mode.STROKE);
        boolean includeStroke = (mode != NGShape.Mode.FILL);
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
            StrokeType type = getStrokeType();
            double sw = Utils.clampMin(getStrokeWidth(), MIN_STROKE_WIDTH);
            StrokeLineCap cap = getStrokeLineCap();
            StrokeLineJoin join = convertLineJoin(getStrokeLineJoin());
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
                    public CssMetaData<Shape,StrokeType> getCssMetaData() {
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
                    public CssMetaData<Shape,Number> getCssMetaData() {
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
                    public CssMetaData<Shape,StrokeLineJoin> getCssMetaData() {
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
                    public CssMetaData<Shape,StrokeLineCap> getCssMetaData() {
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
                    public CssMetaData<Shape,Number> getCssMetaData() {
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
                    public CssMetaData<Shape,Number> getCssMetaData() {
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
                dashArray = new TrackableObservableList<>() {
                    @Override
                    protected void onChanged(Change<Double> c) {
                        StrokeAttributes.this.invalidated(
                                StyleableProperties.STROKE_DASH_ARRAY);
                    }
                };
            }
            return dashArray;
        }

        private ObjectProperty<Number[]> cssDashArray = null;
        private ObjectProperty<Number[]> cssDashArrayProperty() {
            if (cssDashArray == null) {
                cssDashArray = new StyleableObjectProperty<>()
                {

                    @Override
                    public void set(Number[] v) {

                        ObservableList<Double> list = dashArrayProperty();
                        list.clear();
                        if (v != null && v.length > 0) {
                            for (int n=0; n<v.length; n++) {
                                list.add(v[n].doubleValue());
                            }
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
                    public CssMetaData<Shape,Number[]> getCssMetaData() {
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

        private void invalidated(final CssMetaData<Shape, ?> propertyCssKey) {
            NodeHelper.markDirty(Shape.this, DirtyBits.SHAPE_STROKEATTRS);
            strokeAttributesDirty = true;
            if (propertyCssKey != StyleableProperties.STROKE_DASH_OFFSET) {
                // all stroke attributes change geometry except for the
                // stroke dash offset
                NodeHelper.geomChanged(Shape.this);
            }
        }
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
        if (mode == NGShape.Mode.EMPTY) {
            return new Area();
        }

        final com.sun.javafx.geom.Shape fillShape = ShapeHelper.configShape(this);
        if ((mode == NGShape.Mode.FILL)
                || (mode == NGShape.Mode.STROKE_FILL)
                       && (getStrokeType() == StrokeType.INSIDE)) {
            return createTransformedArea(fillShape, transform);
        }

        final StrokeType strokeType = getStrokeType();
        final double strokeWidth =
                Utils.clampMin(getStrokeWidth(), MIN_STROKE_WIDTH);
        final StrokeLineCap strokeLineCap = getStrokeLineCap();
        final StrokeLineJoin strokeLineJoin = convertLineJoin(getStrokeLineJoin());
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

        if (mode == NGShape.Mode.STROKE) {
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
                    NodeHelper.getLeafTransform(node));
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
