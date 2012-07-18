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

package javafx.scene.layout;


import javafx.beans.InvalidationListener;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.Logging;
import com.sun.javafx.TempState;
import com.sun.javafx.binding.ExpressionHelper;
import com.sun.javafx.css.*;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.InsetsConverter;
import com.sun.javafx.css.converters.StringConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.layout.region.*;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGRegion;
import com.sun.javafx.sg.PGShape;
import com.sun.javafx.sg.Repeat;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.*;

/**
 * A Region is an area of the screen that can contain other nodes and be styled
 * using CSS.
 * <p>
 * It can have multiple backgrounds under its contents and multiple borders
 * around its content. By default it's a rectangle with possible rounded corners,
 * depending on borders. It can be made into any shape by specifying the {@code shape}.
 * It is designed to support as much of the CSS3 specification for backgrounds
 * and borders as is relevant to JavaFX. 
 * <a href="http://www.w3.org/TR/css3-background/"> The full specification is available at css3-background</a>.
 * <p>
 * By default a Region inherits the layout behavior of its superclass, {@link Parent},
 * which means that it will resize any resizable child nodes to their preferred
 * size, but will not reposition them.  If an application needs more specific
 * layout behavior, then it should use one of the Region subclasses:
 * {@link StackPane}, {@link HBox}, {@link VBox}, {@link TilePane}, {@link FlowPane},
 * {@link BorderPane}, {@link GridPane}, or {@link AnchorPane}.
 * <p>
 * To implement more custom layout, a Region subclass must override
 * {@link #computePrefWidth(double) computePrefWidth}, {@link #computePrefHeight(double) computePrefHeight}, and {@link #layoutChildren() layoutChildren}.
 * Note that {@link #layoutChildren() layoutChildren} is called automatically by the scene graph while
 * executing a top-down layout pass and it should not be invoked directly by the
 * region subclass.
 * <p>
 * Region subclasses which layout their children will position nodes by setting
 * {@link #setLayoutX(double) layoutX}/{@link #setLayoutY(double) layoutY} and do not alter
 * {@link #setTranslateX(double) translateX}/{@link #setTranslateY(double) translateY}, which are reserved for
 * adjustments and animation.
 *
 */
public class Region extends Parent {

    /**
     * Sentinel value which can be passed to a region's {@link #setMinWidth(double) setMinWidth}, {@link #setMinHeight(double) setMinHeight},
     * {@link #setMaxWidth(double) setMaxWidth} or {@link #setMaxHeight(double) setMaxHeight} methods to indicate that the preferred dimension
     * should be used for that max and/or min constraint.
     */
     public static final double USE_PREF_SIZE = Double.NEGATIVE_INFINITY;

     /**
      * Sentinel value which can be passed to a region's {@link #setMinWidth(double) setMinWidth}, {@link #setMinHeight(double) setMinHeight},
      * {@link #setPrefWidth(double) setPrefWidth}, {@link #setPrefHeight(double) setPrefHeight}, {@link #setMaxWidth(double) setMaxWidth}, {@link #setMaxHeight(double) setMaxHeight} methods
      * to reset the region's size constraint back to it's intrinsic size returned
      * by {@link #computeMinWidth(double) computeMinWidth}, {@link #computeMinHeight(double) computeMinHeight}, 
      * {@link #computePrefWidth(double) computePrefWidth}, {@link #computePrefHeight(double) computePrefHeight},
      * {@link #computeMaxWidth(double) computeMaxWidth}, or {@link #computeMaxHeight(double) computeMaxHeight}.
      */
     public static final double USE_COMPUTED_SIZE = -1;

    /***************************************************************************
     *                                                                         *
     * Static convenience methods for layout                                   *
     *                                                                         *
     **************************************************************************/

    static double boundedSize(double value, double min, double max) {
        // if max < value, return max
        // if min > value, return min
        // if min > max, return min
        return Math.min(Math.max(value, min), Math.max(min,max));
    }

    public static double snapSpace(double value, boolean snapToPixel) {
         return snapToPixel? Math.round(value) : value;
    }

    public static double snapSize(double value, boolean snapToPixel) {
        return snapToPixel? Math.ceil(value) : value;
    }

    public static double snapPosition(double value, boolean snapToPixel) {
        return snapToPixel? Math.round(value) : value;
    }

    static double getMaxAreaBaselineOffset(List<Node> content, Insets margins[]) {
        double max = 0;
        for (int i = 0; i < content.size(); i++) {
            Node node = content.get(i);
            Insets margin = margins[i] != null? margins[i] : Insets.EMPTY;
            max = Math.max(max, (margin != null? margin.getTop() : 0)  + node.getBaselineOffset());
        }
        return max;
    }

    static double getMaxBaselineOffset(List<Node> content) {
        double max = 0;
        for (int i = 0; i < content.size(); i++) {
            Node node = content.get(i);
            max = Math.max(max, node.getBaselineOffset());
        }
        return max;
    }

    static double computeXOffset(double width, double contentWidth, HPos hpos) {
        switch(hpos) {
            case LEFT:
               return 0;
            case CENTER:
               return (width - contentWidth) / 2;
            case RIGHT:
               return width - contentWidth;
        }
        return 0;
    }

    static double computeYOffset(double height, double contentHeight, VPos vpos) {
       switch(vpos) {
            case TOP:
               return 0;
            case CENTER:
               return (height - contentHeight) / 2;
            case BOTTOM:
               return height - contentHeight;
        }
       return 0;
    }

    static double max(List<Double> seq) {
        double max = 0;
        if (seq != null) {
            for (int i = 0; i < seq.size(); i++) {
                double value = seq.get(i);
                max = Math.max(max, value);
            }
        }
        return max;
    }

    static double[] createDoubleArray(int length, double value) {
        double[] array = new double[length];
        for (int i = 0; i < length; i++) {
            array[i] = value;
        }
        return array;
    }

    /* END static convenience methods */

    /***************************************************************************
     *                                                                         *
     * Region properties                                                    *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a Region layout.
     */
    public Region() {
        super();
        setPickOnBounds(true);
    }

    /**
     * Defines whether this region rounds position/spacing and ceils size
     * values to pixel boundaries when laying out its children.
     */
    public final BooleanProperty snapToPixelProperty() {
        if (snapToPixel == null) {
            snapToPixel = new StyleableBooleanProperty(true) {
                @Override
                public void invalidated() {
                    requestLayout();
                }
                    
                @Override
                public StyleableProperty getStyleableProperty() {   
                    return StyleableProperties.SNAP_TO_PIXEL;
                }

                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "snapToPixel";
                }
            };
        }
        return snapToPixel;
    }
    
    private BooleanProperty snapToPixel;
    public final void setSnapToPixel(boolean value) { snapToPixelProperty().set(value); }
    public final boolean isSnapToPixel() { return snapToPixel == null ? true : snapToPixel.get(); }
    
    /**
     * The top,right,bottom,left padding around the region's content.
     * This space will be included in the calculation of the region's
     * minimum and preferred sizes.  By default padding is Insets.EMPTY
     * and cannot be set to null.
     */
    public final ObjectProperty<Insets> paddingProperty() {
        if (padding == null) {
            padding = new StyleableObjectProperty<Insets>(Insets.EMPTY) {
                private Insets lastValidValue = Insets.EMPTY;

                @Override
                public void invalidated() {
                    final Insets newValue = get();
                    if (newValue == null) {
                        // rollback
                        if (isBound()) {
                            unbind();
                        }
                        set(lastValidValue);
                        throw new NullPointerException("cannot set padding to null");
                    }
                    lastValidValue = newValue;
                    insets.fireValueChanged();
                    requestLayout();
                }
                
                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.PADDING;
                }

                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "padding";
                }
            };
        }
        return padding;
    }
    
    private ObjectProperty<Insets> padding;
    public final void setPadding(Insets value) { paddingProperty().set(value); }
    public final Insets getPadding() { return padding == null ? Insets.EMPTY : padding.get(); }

    /**
     * Gets the space around content, which will include any borders plus padding if set.
     * @return the space around content, which will include any borders plus padding if set.
     */
    public Insets getInsets() {
        return insets.get();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public ObservableObjectValue<Insets> insets() {
        return insets;
    }
    private final InsetsExpression insets = new InsetsExpression();
    private final class InsetsExpression extends ObjectExpression<Insets> {
        private Insets cache = null;
        private ExpressionHelper<Insets> helper = null;

        void fireValueChanged() {
            cache = null;
            ExpressionHelper.fireValueChangedEvent(helper);
        }

        @Override public Insets get() {
            if (imageBorders == null && strokeBorders == null) {
                return getPadding();
            }

            if (cache == null) {
                double top = 0;
                double bottom = 0;
                double left = 0;
                double right = 0;
                
                final List<BorderImage> image_borders = getImageBorders();
                if (image_borders != null) {
                    for (int i = 0; i < image_borders.size(); i++) {
                        Insets offsets = image_borders.get(i).getOffsets();
                        // stoked borders assume centered strokes for now
                        left = Math.max(left, offsets.getLeft() + image_borders.get(i).getLeftWidth());
                        top = Math.max(top, offsets.getTop() + (image_borders.get(i).getTopWidth()));
                        right = Math.max(right, offsets.getRight() + (image_borders.get(i).getRightWidth()));
                        bottom = Math.max(bottom, offsets.getBottom() + (image_borders.get(i).getBottomWidth()));
                    }
                }
                
                final List<StrokeBorder> stroke_borders = getStrokeBorders();
                if (stroke_borders != null) {
                    for (int i = 0; i < stroke_borders.size(); i++) {
                        Insets offsets = stroke_borders.get(i).getOffsets();
                        // stoked borders assume centered strokes for now
                        left = Math.max(left, offsets.getLeft() + (stroke_borders.get(i).getLeftWidth()));
                        top = Math.max(top, offsets.getTop() + (stroke_borders.get(i).getTopWidth()));
                        right = Math.max(right, offsets.getRight() + (stroke_borders.get(i).getRightWidth()));
                        bottom = Math.max(bottom, offsets.getBottom() + (stroke_borders.get(i).getBottomWidth()));
                    }
                }
                Insets padding = getPadding();
                cache = new Insets(Math.max(0,top) + padding.getTop(),
                                         Math.max(0,right) + padding.getRight(),
                                         Math.max(0,bottom) + padding.getBottom(),
                                         Math.max(0,left) + padding.getLeft());

            }
            return cache;
        }

        @Override public Insets getValue() {
            return get();
        }

        @Override
        public void addListener(InvalidationListener listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }

        @Override
        public void addListener(ChangeListener<? super Insets> listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override
        public void removeListener(ChangeListener<? super Insets> listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }
    };

    /**
     * The width of this resizable node.  This property is set by the region's parent
     * during layout and may not be set by the application.  If an application
     * needs to explicitly control the size of a region, it should override its
     * preferred size range by setting the <code>minWidth</code>, <code>prefWidth</code>,
     * and <code>maxWidth</code> properties.
     */
    public final ReadOnlyDoubleProperty widthProperty() {
        return widthPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper widthPropertyImpl() {
        if (width == null) {
            width = new ReadOnlyDoubleWrapper(0.0) {
                @Override
                protected void invalidated() {
                    impl_layoutBoundsChanged();
                    impl_geomChanged();
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "width";
                }
            };
        }
        return width;
    }
    private ReadOnlyDoubleWrapper width;
    protected void setWidth(double value) { widthPropertyImpl().set(value); }
    public final double getWidth() { return width == null ? 0.0 : width.get(); }

    /**
     * The height of this resizable node.  This property is set by the region's parent
     * during layout and may not be set by the application.  If an application
     * needs to explicitly control the size of a region, it should override its
     * preferred size range by setting the <code>minHeight</code>, <code>prefHeight</code>,
     * and <code>maxHeight</code> properties.
     */
    public final ReadOnlyDoubleProperty heightProperty() {
        return heightPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper heightPropertyImpl() {
        if (height == null) {
            height = new ReadOnlyDoubleWrapper(0.0) {
                @Override
                protected void invalidated() {
                    impl_layoutBoundsChanged();
                    impl_geomChanged();
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "height";
                }
            };
        }
        return height;
    }
    private ReadOnlyDoubleWrapper height;
    protected void setHeight(double value) { heightPropertyImpl().set(value); }
    public final double getHeight() { return height == null ? 0.0 : height.get(); }

    /**
     * Property for overriding the region's computed minimum width.
     * This should only be set if the region's internally computed minimum width
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>minWidth(forHeight)</code> will return the region's internally
     * computed minimum width.
     * <p>
     * Setting this value to the <code>USE_PREF_SIZE</code> flag will cause
     * <code>minWidth(forHeight)</code> to return the region's preferred width,
     * enabling applications to easily restrict the resizability of the region.
     *
     */
    public final DoubleProperty minWidthProperty() {
        if (minWidth == null) {
            minWidth = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "minWidth";
                }
            };
        }
        return minWidth;
    }
    private DoubleProperty minWidth;
    public final void setMinWidth(double value) { minWidthProperty().set(value); }
    public final double getMinWidth() { return minWidth == null ? USE_COMPUTED_SIZE : minWidth.get(); }


    /**
     * Property for overriding the region's computed minimum height.
     * This should only be set if the region's internally computed minimum height
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>minHeight(forWidth)</code> will return the region's internally
     * computed minimum height.
     * <p>
     * Setting this value to the <code>USE_PREF_SIZE</code> flag will cause
     * <code>minHeight(forWidth)</code> to return the region's preferred height,
     * enabling applications to easily restrict the resizability of the region.
     *
     */
    public final DoubleProperty minHeightProperty() {
        if (minHeight == null) {
            minHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "minHeight";
                }
            };
        }
        return minHeight;
    }
    private DoubleProperty minHeight;
    public final void setMinHeight(double value) { minHeightProperty().set(value); }
    public final double getMinHeight() { return minHeight == null ? USE_COMPUTED_SIZE : minHeight.get(); }

    /**
     * Convenience method for overriding the region's computed minimum width and height.
     * This should only be called if the region's internally computed minimum size
     * doesn't meet the application's layout needs.
     *
     * @see #setMinWidth
     * @see #setMinHeight
     * @param minWidth  the override value for minimum width
     * @param minHeight the override value for minimum height
     */
    public void setMinSize(double minWidth, double minHeight) {
        setMinWidth(minWidth);
        setMinHeight(minHeight);
    }

    /**
     * Property for overriding the region's computed preferred width.
     * This should only be set if the region's internally computed preferred width
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getPrefWidth(forHeight)</code> will return the region's internally
     * computed preferred width.
     *
     */
    public final DoubleProperty prefWidthProperty() {
        if (prefWidth == null) {
            prefWidth = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "prefWidth";
                }
            };
        }
        return prefWidth;
    }
    private DoubleProperty prefWidth;
    public final void setPrefWidth(double value) { prefWidthProperty().set(value); }
    public final double getPrefWidth() { return prefWidth == null ? USE_COMPUTED_SIZE : prefWidth.get(); }


    /**
     * Property for overriding the region's computed preferred height.
     * This should only be set if the region's internally computed preferred height
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getPrefHeight(forWidth)</code> will return the region's internally
     * computed preferred width.
     *
     */
    public final DoubleProperty prefHeightProperty() {
        if (prefHeight == null) {
            prefHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "prefHeight";
                }
            };
        }
        return prefHeight;
    }
    private DoubleProperty prefHeight;
    public final void setPrefHeight(double value) { prefHeightProperty().set(value); }
    public final double getPrefHeight() { return prefHeight == null ? USE_COMPUTED_SIZE : prefHeight.get(); }

    /**
     * Convenience method for overriding the region's computed preferred width and height.
     * This should only be called if the region's internally computed preferred size
     * doesn't meet the application's layout needs.
     *
     * @see #setPrefWidth
     * @see #setPrefHeight
     * @param prefWidth the override value for preferred width
     * @param prefHeight the override value for preferred height
     */
    public void setPrefSize(double prefWidth, double prefHeight) {
        setPrefWidth(prefWidth);
        setPrefHeight(prefHeight);
    }

    /**
     * Property for overriding the region's computed maximum width.
     * This should only be set if the region's internally computed maximum width
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getMaxWidth(forHeight)</code> will return the region's internally
     * computed maximum width.
     * <p>
     * Setting this value to the <code>USE_PREF_SIZE</code> flag will cause
     * <code>getMaxWidth(forHeight)</code> to return the region's preferred width,
     * enabling applications to easily restrict the resizability of the region.
     *
     */
    public final DoubleProperty maxWidthProperty() {
        if (maxWidth == null) {
            maxWidth = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "maxWidth";
                }
            };
        }
        return maxWidth;
    }
    private DoubleProperty maxWidth;
    public final void setMaxWidth(double value) { maxWidthProperty().set(value); }
    public final double getMaxWidth() { return maxWidth == null ? USE_COMPUTED_SIZE : maxWidth.get(); }


    /**
     * Property for overriding the region's computed maximum height.
     * This should only be set if the region's internally computed maximum height
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getMaxHeight(forWidth)</code> will return the region's internally
     * computed maximum height.
     * <p>
     * Setting this value to the <code>USE_PREF_SIZE</code> flag will cause
     * <code>getMaxHeight(forWidth)</code> to return the region's preferred height,
     * enabling applications to easily restrict the resizability of the region.
     *
     */
    public final DoubleProperty maxHeightProperty() {
        if (maxHeight == null) {
            maxHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "maxHeight";
                }
            };
        }
        return maxHeight;
    }
    private DoubleProperty maxHeight;
    public final void setMaxHeight(double value) { maxHeightProperty().set(value); }
    public final double getMaxHeight() { return maxHeight == null ? USE_COMPUTED_SIZE : maxHeight.get(); }

    /**
     * Convenience method for overriding the region's computed maximum width and height.
     * This should only be called if the region's internally computed maximum size
     * doesn't meet the application's layout needs.
     *
     * @see #setMaxWidth
     * @see #setMaxHeight
     * @param maxWidth  the override value for maximum width
     * @param maxHeight the override value for maximum height
     */
    public void setMaxSize(double maxWidth, double maxHeight) {
        setMaxWidth(maxWidth);
        setMaxHeight(maxHeight);
    }

    /**
     * By default a region is a rectangle with rounded corners if specified in
     * the borders. If you need a more complex shape then one can be provided
     * here.
     *
     * @default null
     * @css shape       SVG shape string
     */
    
    private ObjectProperty<Shape> shape;
    private Shape getShape() {
        return (shape == null ? null : shape.get());
    }
    
    private ObjectProperty<Shape> shapeProperty() {
        if (shape == null) {
            shape = new ObjectPropertyBase<Shape>() {

                @Override
                protected void invalidated() {
                    
                    impl_geomChanged();
                    requestLayout();
                    impl_markDirty(DirtyBits.REGION_SHAPE);
                    
                }

                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "shape";
                }

            };
        }
        return shape;
    }

    /* shapeContent is used to style the shape from CSS */
    private StringProperty shapeContent;
    private StringProperty shapeContentProperty() {
        if (shapeContent == null) {
            shapeContent = new StyleableStringProperty() {

                @Override
                protected void invalidated() {

                    final String newContent = get();
                    if (newContent != null && !newContent.isEmpty()) {
                        
                        final Shape shape = getShape();
                        if (shape instanceof SVGPath) {
                            
                            final SVGPath svgPath = (SVGPath)shape;
                            if (!newContent.equals(svgPath.getContent())) {
                                svgPath.setContent(newContent);
                            }
                            
                        } else {
                            
                            final SVGPath svgPath = new SVGPath();
                            svgPath.setContent(newContent);
//                            shapeProperty().set(svgPath);
                            impl_setShape(svgPath);
                            
                        }
                        
                    } else {
                        shapeProperty().set(null);
                    }                    

                }
                
                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "shapeContent";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.SHAPE;
                }
                
            };
            
        }
        return shapeContent;
    }
    /**
     * Some skins relying on this
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setShape(Shape value) {
        
        final Shape thisShape = impl_getShape();
        if (thisShape != null && !thisShape.equals(value)) {
            thisShape.impl_setShapeChangeListener(null);
        }
        shapeProperty().set(value);
        if (value != null) {
            value.impl_setShapeChangeListener(getShapeChangeListener());
        }        
    }

    private ShapeChangeListener shapeChangeListener;
    private ShapeChangeListener getShapeChangeListener() {
        if (shapeChangeListener == null) {
            shapeChangeListener = new ShapeChangeListener() {

                @Override
                public void changed() {
                    impl_geomChanged();
                    requestLayout();
                    impl_markDirty(DirtyBits.REGION_SHAPE);
                }
            };
        }
        return shapeChangeListener;
    }

    /**
     * Some skins relying on this
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public Shape impl_getShape() {
        return shape == null ? null : shape.get();
    }

    /**
     * if a {@code shape} is not specified then this has no effect. {@code true} means the shape is scaled to fit the
     * size of the Region, {@code false} means the shape is at its source size, its positioning depends on the value of
     * {@code positionShape}.
     *
     * @default true
     * @css shape-size      true | false
     */
    private BooleanProperty scaleShape = null;
    private boolean getScaleShape() {
        return scaleShape == null ? true : scaleShape.get();
    }
    
    private BooleanProperty scaleShapeProperty() {
        if (scaleShape == null) {
            scaleShape = new StyleableBooleanProperty(true) {

                @Override 
                public void invalidated() {
                    requestLayout();
                    impl_markDirty(DirtyBits.REGION_SHAPE);                    
                }
                
                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "scaleShape";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.SCALE_SHAPE;
                }
                
            };
        }
        return scaleShape;
    }
    /**
     * Some skins relying on this
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setScaleShape(boolean value) {
        scaleShapeProperty().set(value);
    }

    /**
     * Some skins relying on this
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public boolean impl_getScaleShape() {
        return scaleShape == null ? true : scaleShape.get();
    }

    /**
     * if a {@code shape} is not specified or if {@code scaleShape} is {@code true} then this has no effect.
     * {@code true} means the shape centered within the Region's width and height, {@code false} means the shape is
     * positioned at its source position.
     *
     * @default true
     * @css position-shape      true | false
     */
    private BooleanProperty positionShape = null;
    private boolean getPositionShape() {
        return positionShape == null ? true : positionShape.get();
    }

    private BooleanProperty positionShapeProperty() {
        if (positionShape == null) {
            positionShape = new StyleableBooleanProperty(true) {

                @Override 
                public void invalidated() {
                    requestLayout();
                    impl_markDirty(DirtyBits.REGION_SHAPE);                    
                }
                
                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "positionShape";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.SCALE_SHAPE;
                }
                
            };
        }
        return positionShape;
    }
    
    /**
     * Some skins relying on this
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setPositionShape(boolean value) {
        positionShapeProperty().set(value);
    }

    /**
     * Some skins relying on this
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public boolean impl_getPositionShape() {
        return positionShape == null ? true : positionShape.get();
    }

    /**
     * The background fill to fill shape with. Its placed under all background
     * images, content and borders.
     *
     * @default null
     * @css background-color
     */
    
    private ObjectProperty<List<BackgroundFill>> backgroundFills = null;
    
    private ObjectProperty<List<BackgroundFill>> backgroundFillsProperty() {
        if (backgroundFills == null) {
            backgroundFills = new StyleableObjectProperty<List<BackgroundFill>>() {

                @Override 
                public void invalidated() {
                    impl_geomChanged();
                    impl_markDirty(DirtyBits.SHAPE_FILL);                    
                }
                
                @Override 
                public void set(List<BackgroundFill> newValue) {
                    final List<BackgroundFill> oldValue = get();                        
                    if (oldValue == null ? newValue != null :  !oldValue.equals(newValue)) {
                        super.set(newValue);
                    }
                }
                
                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "backgroundFills";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.BACKGROUND_FILLS;
                }
                
            };
        }
        return backgroundFills;
    }
    
    private List<BackgroundFill> getBackgroundFills() {
        return backgroundFills == null ? null : backgroundFills.get(); 

    }
    
    /**
     * Some skins relying on this
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setBackgroundFills(List<BackgroundFill> value) {

        final List<BackgroundFill> background_fills = getBackgroundFills();
        
        if ((background_fills == null) ? (value != null) : !background_fills.equals(value)) { 
            this.backgroundFillsProperty().set(value);
        }
    }

    /**
     * Some skins relying on this
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public List<BackgroundFill> impl_getBackgroundFills() {
        return getBackgroundFills();
    }

    /**
     * Array of backgrounds, the first one is the bottom most. They are placed
     * over background fill and under content and borders.
     *
     * @default none
     * @css background-image, background-repeat, background-position, background-size
     */
    private ObjectProperty<List<BackgroundImage>> backgroundImages = null;
    
    private ObjectProperty<List<BackgroundImage>> backgroundImagesProperty() {
        if (backgroundImages == null) {
            backgroundImages = new StyleableObjectProperty<List<BackgroundImage>>() {

                @Override 
                public void invalidated() {
                    impl_geomChanged();
                    impl_markDirty(DirtyBits.NODE_CONTENTS);                    
                }

                @Override 
                public void set(List<BackgroundImage> newValue) {
                    final List<BackgroundImage> oldValue = get();                        
                    if (oldValue == null ? newValue != null :  !oldValue.equals(newValue)) {
                        super.set(newValue);
                    }
                }
                
                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "backgroundImages";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.BACKGROUND_IMAGES;
                }
                
            };
        }
        return backgroundImages;
    }
    
    private List<BackgroundImage> getBackgroundImages() {
        return backgroundImages == null ?  null : backgroundImages.get();
        
    }
    
    private void setBackgroundImages(List<BackgroundImage> value) {
        
        final List<BackgroundImage> background_images =  getBackgroundImages();
        
        if (background_images == null ? value == null : !background_images.equals(value)) {
            this.backgroundImagesProperty().set(value);
        }
    }

    
    private ObjectProperty<List<StrokeBorder>> strokeBorders = null;

    private ObjectProperty<List<StrokeBorder>> strokeBordersProperty() {
        if (strokeBorders == null) {
            strokeBorders = new StyleableObjectProperty<List<StrokeBorder>>() {

                @Override 
                public void invalidated() {
                    insets.fireValueChanged();
                    impl_geomChanged();
                    impl_markDirty(DirtyBits.SHAPE_STROKE);                    
                }

                @Override 
                public void set(List<StrokeBorder> newValue) {
                    final List<StrokeBorder> oldValue = get();                        
                    if (oldValue == null ? newValue != null :  !oldValue.equals(newValue)) {
                        super.set(newValue);
                    }
                }
                
                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "strokeBorders";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.STROKE_BORDERS;
                }
                
            };
        }
        return strokeBorders;
    }
    
    private List<StrokeBorder> getStrokeBorders() {
        return strokeBorders == null ? null : strokeBorders.get();
    }
    
    private void setStrokeBorders(List<StrokeBorder> value) {

        final List<StrokeBorder> stroke_borders = getStrokeBorders();
        
        if ((stroke_borders == null) ? (value != null) : !stroke_borders.equals(value)) { 
            this.strokeBordersProperty().set(value);
        }
    }

    //TODO(aim): do we need store methods for these to update geoemtry?
    
    private ObjectProperty<List<BorderImage>> imageBorders = null;
    
    private ObjectProperty<List<BorderImage>> imageBordersProperty() {
        if (imageBorders == null) {
            imageBorders = new StyleableObjectProperty<List<BorderImage>>() {

                @Override 
                public void invalidated() {
                    insets.fireValueChanged();
                    impl_geomChanged();
                    impl_markDirty(DirtyBits.SHAPE_STROKE);                    
                }

                @Override 
                public void set(List<BorderImage> newValue) {
                    final List<BorderImage> oldValue = get();                        
                    if (oldValue == null ? newValue != null : !oldValue.equals(newValue)) {
                        super.set(newValue);
                    }
                }
                
                @Override
                public Object getBean() {
                    return Region.this;
                }

                @Override
                public String getName() {
                    return "imageBorders";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.IMAGE_BORDERS;
                }
                
            };
        }
        return imageBorders;
    }

    private List<BorderImage> getImageBorders() {
        return imageBorders == null ? null : imageBorders.get();
    }
    
    private void setImageBorders(List<BorderImage> value) {
        
        final List<BorderImage> image_borders = getImageBorders();
        
        if (image_borders == null ? value == null :  !image_borders.equals(value)) {
            this.imageBordersProperty().set(value);
        }
    }
    
    /***************************************************************************
     *                                                                         *
     * Layout                                                                  *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns <code>true</code> since all Regions are resizable.
     * @return whether this node can be resized by its parent during layout
     */
    @Override public boolean isResizable() {
        return true;
    }

    /**
     * Invoked by the region's parent during layout to set the region's
     * width and height.  <b>Applications should not invoke this method directly</b>.
     * If an application needs to directly set the size of the region, it should
     * override its size constraints by calling <code>setMinSize()</code>,
     *  <code>setPrefSize()</code>, or <code>setMaxSize()</code> and it's parent
     * will honor those overrides during layout.
     *
     * @param width the target layout bounds width
     * @param height the target layout bounds height
     */
    @Override public void resize(double width, double height) {
        setWidth(width);
        setHeight(height);
        PlatformLogger logger = Logging.getLayoutLogger();
        if (logger.isLoggable(PlatformLogger.FINER)) {
            logger.finer(this.toString()+" resized to "+width+" x "+height);
        }
    }

    /**
     * Called during layout to determine the minimum width for this node.
     * Returns the value from <code>computeMinWidth(forHeight)</code> unless
     * the application overrode the minimum width by setting the minWidth property.
     *
     * @see #setMinWidth(double)
     * @return the minimum width that this node should be resized to during layout
     */
    @Override public final double minWidth(double height) {
        double override = getMinWidth();
        if (override == USE_COMPUTED_SIZE) {
            return super.minWidth(height);
        } else if (override == USE_PREF_SIZE) {
            return prefWidth(height);
        }
        return override;
    }

    /**
     * Called during layout to determine the minimum height for this node.
     * Returns the value from <code>computeMinHeight(forWidth)</code> unless
     * the application overrode the minimum height by setting the minHeight property.
     *
     * @see #setMinHeight
     * @return the minimum height that this node should be resized to during layout
     */
    @Override public final double minHeight(double width) {
        double override = getMinHeight();
        if (override == USE_COMPUTED_SIZE) {
            return super.minHeight(width);
        } else if (override == USE_PREF_SIZE) {
            return prefHeight(width);
        }
        return override;
    }


    /**
     * Called during layout to determine the preferred width for this node.
     * Returns the value from <code>computePrefWidth(forHeight)</code> unless
     * the application overrode the preferred width by setting the prefWidth property.
     *
     * @see #setPrefWidth
     * @return the preferred width that this node should be resized to during layout
     */
    @Override public final double prefWidth(double height) {
        double override = getPrefWidth();
        if (override == USE_COMPUTED_SIZE) {
            return super.prefWidth(height);
        }
        return override;
    }

    /**
     * Called during layout to determine the preferred height for this node.
     * Returns the value from <code>computePrefHeight(forWidth)</code> unless
     * the application overrode the preferred height by setting the prefHeight property.
     *
     * @see #setPrefHeight
     * @return the preferred height that this node should be resized to during layout
     */
    @Override public final double prefHeight(double width) {
        double override = getPrefHeight();
        if (override == USE_COMPUTED_SIZE) {
            return super.prefHeight(width);
        }
        return override;
    }
    /**
     * Called during layout to determine the maximum width for this node.
     * Returns the value from <code>computeMaxWidth(forHeight)</code> unless
     * the application overrode the maximum width by setting the maxWidth property.
     *
     * @see #setMaxWidth
     * @return the maximum width that this node should be resized to during layout
     */
    @Override public final double maxWidth(double height) {
        double override = getMaxWidth();
        if (override == USE_COMPUTED_SIZE) {
            return computeMaxWidth(height);
        } else if (override == USE_PREF_SIZE) {
            return prefWidth(height);
        }
        return override;
    }

   /**
     * Called during layout to determine the maximum height for this node.
     * Returns the value from <code>computeMaxHeight(forWidth)</code> unless
     * the application overrode the maximum height by setting the maxHeight property.
     *
     * @see #setMaxHeight
     * @return the maximum height that this node should be resized to during layout
     */
    @Override public final double maxHeight(double width) {
        double override = getMaxHeight();
        if (override == USE_COMPUTED_SIZE) {
            return computeMaxHeight(width);
        } else if (override == USE_PREF_SIZE) {
            return prefHeight(width);
        }
        return override;
    }

    /**
     * Computes the minimum width of this region.
     * Returns the sum of the left and right insets by default.
     * region subclasses should override this method to return an appropriate
     * value based on their content and layout strategy.  If the subclass
     * doesn't have a VERTICAL content bias, then the height parameter can be
     * ignored.
     *
     * @return the computed minimum width of this region
     */
    protected double computeMinWidth(double height) {
        return getInsets().getLeft() + getInsets().getRight();
    }

    /**
     * Computes the minimum height of this region.
     * Returns the sum of the top and bottom insets by default.
     * Region subclasses should override this method to return an appropriate
     * value based on their content and layout strategy.  If the subclass
     * doesn't have a HORIZONTAL content bias, then the width parameter can be
     * ignored.
     *
     * @return the computed minimum height for this region
     */
    protected double computeMinHeight(double width) {
        return getInsets().getTop() + getInsets().getBottom();
    }

    /**
     * Computes the preferred width of this region for the given height.
     * Region subclasses should override this method to return an appropriate
     * value based on their content and layout strategy.  If the subclass
     * doesn't have a VERTICAL content bias, then the height parameter can be
     * ignored.
     *
     * @return the computed preferred width for this region
     */
    @Override protected double computePrefWidth(double height) {
        double pwidth = super.computePrefWidth(height);
        return getInsets().getLeft() + pwidth + getInsets().getRight();
    }

    /**
     * Computes the preferred height of this region for the given width;
     * Region subclasses should override this method to return an appropriate
     * value based on their content and layout strategy.  If the subclass
     * doesn't have a HORIZONTAL content bias, then the width parameter can be
     * ignored.
     *
     * @return the computed preferred height for this region
     */
    @Override protected double computePrefHeight(double width) {
        double pheight = super.computePrefHeight(width);
        return getInsets().getTop() + pheight + getInsets().getBottom();
    }

    /**
     * Computes the maximum width for this region.
     * Returns Double.MAX_VALUE by default.
     * Region subclasses may override this method to return an different
     * value based on their content and layout strategy.  If the subclass
     * doesn't have a VERTICAL content bias, then the height parameter can be
     * ignored.
     *
     * @return the computed maximum width for this region
     */
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    /**
     * Computes the maximum height of this region.
     * Returns Double.MAX_VALUE by default.
     * Region subclasses may override this method to return a different
     * value based on their content and layout strategy.  If the subclass
     * doesn't have a HORIZONTAL content bias, then the width parameter can be
     * ignored.
     *
     * @return the computed maximum height for this region
     */
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    /**
     * If this region's snapToPixel property is true, returns a value rounded
     * to the nearest pixel, else returns the same value.
     * @param value the space value to be snapped
     * @return value rounded to nearest pixel
     */
    protected double snapSpace(double value) {
        return snapSpace(value, isSnapToPixel());
    }

    /**
     * If this region's snapToPixel property is true, returns a value ceiled
     * to the nearest pixel, else returns the same value.
     * @param value the size value to be snapped
     * @return value ceiled to nearest pixel
     */
    protected double snapSize(double value) {
        return snapSize(value, isSnapToPixel());
    }

    /**
     * If this region's snapToPixel property is true, returns a value rounded
     * to the nearest pixel, else returns the same value.
     * @param value the position value to be snapped
     * @return value rounded to nearest pixel
     */
    protected double snapPosition(double value) {
        return snapPosition(value, isSnapToPixel());
    }

    double computeChildMinAreaWidth(Node child, Insets margin) {
        return computeChildMinAreaWidth(child, margin, -1);
    }

    double computeChildMinAreaWidth(Node child, Insets margin, double height) {
        double left = margin != null? snapSpace(margin.getLeft(), isSnapToPixel()) : 0;
        double right = margin != null? snapSpace(margin.getRight(), isSnapToPixel()) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            alt = snapSize(height != -1? boundedSize(height, child.minHeight(-1), child.maxHeight(-1)) :
                                         child.minHeight(-1));
        }
        return left + snapSize(child.minWidth(alt)) + right;
    }

    double computeChildMinAreaHeight(Node child, Insets margin) {
        return computeChildMinAreaHeight(child, margin, -1);
    }

    double computeChildMinAreaHeight(Node child, Insets margin, double width) {
        double top = margin != null? snapSpace(margin.getTop(), isSnapToPixel()) : 0;
        double bottom = margin != null? snapSpace(margin.getBottom(), isSnapToPixel()) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.HORIZONTAL) { // width depends on height
            alt = snapSize(width != -1? boundedSize(width, child.minWidth(-1), child.maxWidth(-1)) :
                                        child.minWidth(-1));
        }
        return top + snapSize(child.minHeight(alt)) + bottom;
    }

    double computeChildPrefAreaWidth(Node child, Insets margin) {
        return computeChildPrefAreaWidth(child, margin, -1);
    }

    double computeChildPrefAreaWidth(Node child, Insets margin, double height) {
        double top = margin != null? snapSpace(margin.getTop(), isSnapToPixel()) : 0;
        double bottom = margin != null? snapSpace(margin.getBottom(), isSnapToPixel()) : 0;
        double left = margin != null? snapSpace(margin.getLeft(), isSnapToPixel()) : 0;
        double right = margin != null? snapSpace(margin.getRight(), isSnapToPixel()) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            alt = snapSize(boundedSize(height != -1? height - top - bottom :
                           child.prefHeight(-1), child.minHeight(-1), child.maxHeight(-1)));
        }        
        return left + snapSize(boundedSize(child.prefWidth(alt), child.minWidth(alt), child.maxWidth(alt))) + right;
    }

    double computeChildPrefAreaHeight(Node child, Insets margin) {
        return computeChildPrefAreaHeight(child, margin, -1);
    }

    double computeChildPrefAreaHeight(Node child, Insets margin, double width) {
        double top = margin != null? snapSpace(margin.getTop(), isSnapToPixel()) : 0;
        double bottom = margin != null? snapSpace(margin.getBottom(), isSnapToPixel()) : 0;
        double left = margin != null? snapSpace(margin.getLeft(), isSnapToPixel()) : 0;
        double right = margin != null? snapSpace(margin.getRight(), isSnapToPixel()) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.HORIZONTAL) { // width depends on height
            alt = snapSize(boundedSize(width != -1? width - left - right :
                           child.prefWidth(-1), child.minWidth(-1),child.maxWidth(-1)));
        }        
        return top + snapSize(boundedSize(child.prefHeight(alt), child.minHeight(alt),child.maxHeight(alt))) + bottom;
    }

    double computeChildMaxAreaWidth(Node child, Insets margin) {
        return computeChildMaxAreaWidth(child, margin, -1);
    }

    double computeChildMaxAreaWidth(Node child, Insets margin, double height) {
        double max = child.maxWidth(-1);
        if (max == Double.MAX_VALUE) {
            return max;
        }
        double left = margin != null? snapSpace(margin.getLeft(), isSnapToPixel()) : 0;
        double right = margin != null? snapSpace(margin.getRight(), isSnapToPixel()) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            alt = snapSize(height != -1? boundedSize(height, child.minHeight(-1), child.maxHeight(-1)) :
                child.maxHeight(-1));
            max = child.maxWidth(alt);
        }
        // if min > max, min wins, so still need to call boundedSize()
        return left + snapSize(boundedSize(max, child.minWidth(alt), child.maxWidth(alt))) + right;
    }

    double computeChildMaxAreaHeight(Node child, Insets margin) {
        return computeChildMaxAreaHeight(child, margin, -1);
    }

    double computeChildMaxAreaHeight(Node child, Insets margin, double width) {
        double max = child.maxHeight(-1);
        if (max == Double.MAX_VALUE) {
            return max;
        }
        double top = margin != null? snapSpace(margin.getTop(), isSnapToPixel()) : 0;
        double bottom = margin != null? snapSpace(margin.getBottom(), isSnapToPixel()) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.HORIZONTAL) { // width depends on height
            alt = snapSize(width != -1? boundedSize(width, child.minWidth(-1), child.maxWidth(-1)) :
                child.maxWidth(-1));
            max = child.maxHeight(alt);
        }
        // if min > max, min wins, so still need to call boundedSize()
        return top + snapSize(boundedSize(max, child.minHeight(alt), child.maxHeight(alt))) + bottom;
    }

    /* Max of children's minimum area widths */

    double computeMaxMinAreaWidth(List<Node> children, Insets margins[], HPos halignment /* ignored for now */) {
        return getMaxAreaWidth(children, margins, createDoubleArray(children.size(), -1), halignment, true);
    }

    double computeMaxMinAreaWidth(List<Node> children, Insets margins[], HPos halignment /* ignored for now */, double height) {
        return getMaxAreaWidth(children, margins, createDoubleArray(children.size(), height), halignment, true);
    }

    double computeMaxMinAreaWidth(List<Node> children, Insets childMargins[], double childHeights[], HPos halignment /* ignored for now */) {
        return getMaxAreaWidth(children, childMargins, childHeights, halignment, true);
    }

    /* Max of children's minimum area heights */

    double computeMaxMinAreaHeight(List<Node>children, Insets margins[], VPos valignment) {
        return getMaxAreaHeight(children, margins, createDoubleArray(children.size(), -1), valignment, true);
    }

    double computeMaxMinAreaHeight(List<Node>children, Insets margins[], VPos valignment, double width) {
        return getMaxAreaHeight(children, margins, createDoubleArray(children.size(), width), valignment, true);
    }

    double computeMaxMinAreaHeight(List<Node>children, Insets childMargins[], double childWidths[], VPos valignment) {
        return getMaxAreaHeight(children, childMargins, childWidths, valignment, true);
    }

    /* Max of children's pref area widths */

    double computeMaxPrefAreaWidth(List<Node>children, Insets margins[], HPos halignment /* ignored for now */) {        
        return getMaxAreaWidth(children, margins, createDoubleArray(children.size(), -1), halignment, false);
    }

    double computeMaxPrefAreaWidth(List<Node>children, Insets margins[], double height, HPos halignment /* ignored for now */) {
        return getMaxAreaWidth(children, margins, createDoubleArray(children.size(), height), halignment, false);
    }

    double computeMaxPrefAreaWidth(List<Node>children, Insets childMargins[], double childHeights[], HPos halignment /* ignored for now */) {
        return getMaxAreaWidth(children, childMargins, childHeights, halignment, false);
    }

    /* Max of children's pref area heights */

    double computeMaxPrefAreaHeight(List<Node>children, Insets margins[], VPos valignment) {
        return getMaxAreaHeight(children, margins, createDoubleArray(children.size(), -1), valignment, false);
    }

    double computeMaxPrefAreaHeight(List<Node>children, Insets margins[], double width, VPos valignment) {
        return getMaxAreaHeight(children, margins, createDoubleArray(children.size(), width), valignment, false);
    }

    double computeMaxPrefAreaHeight(List<Node>children, Insets childMargins[], double childWidths[], VPos valignment) {
        return getMaxAreaHeight(children, childMargins, childWidths, valignment, false);
    }

    /* utility method for computing the max of children's min or pref heights, taking into account baseline alignment */
    private double getMaxAreaHeight(List<Node> children, Insets childMargins[],  double childWidths[], VPos valignment, boolean minimum) {
        if (valignment == VPos.BASELINE) {
            double maxAbove = 0;
            double maxBelow = 0;
            for (int i = 0; i < children.size(); i++) {
                Node child = children.get(i);
                double baseline = child.getBaselineOffset();
                double top = childMargins[i] != null? snapSpace(childMargins[i].getTop()) : 0;
                double bottom = childMargins[i] != null? snapSpace(childMargins[i].getBottom()) : 0;
                maxAbove = Math.max(maxAbove, baseline + top);
                maxBelow = Math.max(maxBelow,
                        snapSpace(minimum? snapSize(child.minHeight(childWidths[i])) : snapSize(child.prefHeight(childWidths[i]))) -
                        baseline + bottom);
            }
            return maxAbove + maxBelow; //remind(aim): ceil this value?

        } else {
            double max = 0;
            for (int i = 0; i < children.size(); i++) {
                Node child = children.get(i);
                max = Math.max(max, minimum?
                    computeChildMinAreaHeight(child, childMargins[i], childWidths[i]) :
                        computeChildPrefAreaHeight(child, childMargins[i], childWidths[i]));
            }
            return max;
        }
    }

    /* utility method for computing the max of children's min or pref width, horizontal alignment is ignored for now */
    private double getMaxAreaWidth(List<Node> children, Insets childMargins[], double childHeights[], HPos halignment, boolean minimum) {
        double max = 0;
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            max = Math.max(max, minimum?
                computeChildMinAreaWidth(children.get(i), childMargins[i], childHeights[i]) :
                    computeChildPrefAreaWidth(child, childMargins[i], childHeights[i]));
        }
        return max;
    }

    /**
     * Utility method which positions the child within an area of this
     * region defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
     * with a baseline offset relative to that area.
     * <p>
     * This function does <i>not</i> resize the node and uses the node's layout bounds
     * width and height to determine how it should be positioned within the area.
     * <p>
     * If the vertical alignment is {@code VPos.BASELINE} then it
     * will position the node so that its own baseline aligns with the passed in
     * {@code baselineOffset},  otherwise the baseline parameter is ignored.
     * <p>
     * If {@code snapToPixel} is {@code true} for this region, then the x/y position
     * values will be rounded to their nearest pixel boundaries.
     *
     * @param child the child being positioned within this region
     * @param areaX the horizontal offset of the layout area relative to this region
     * @param areaY the vertical offset of the layout area relative to this region
     * @param areaWidth  the width of the layout area
     * @param areaHeight the height of the layout area
     * @param areaBaselineOffset the baseline offset to be used if VPos is BASELINE
     * @param halignment the horizontal alignment for the child within the area
     * @param valignment the vertical alignment for the child within the area
     *
     */
    protected void positionInArea(Node child, double areaX, double areaY, double areaWidth, double areaHeight,
                               double areaBaselineOffset, HPos halignment, VPos valignment) {
        positionInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset,
                Insets.EMPTY, halignment, valignment, isSnapToPixel());
    }

    /**
     * Utility method which positions the child within an area of this
     * region defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
     * with a baseline offset relative to that area.
     * <p>
     * This function does <i>not</i> resize the node and uses the node's layout bounds
     * width and height to determine how it should be positioned within the area.
     * <p>
     * If the vertical alignment is {@code VPos.BASELINE} then it
     * will position the node so that its own baseline aligns with the passed in
     * {@code baselineOffset},  otherwise the baseline parameter is ignored.
     * <p>
     * If {@code snapToPixel} is {@code true} for this region, then the x/y position
     * values will be rounded to their nearest pixel boundaries.
     * <p>
     * If {@code margin} is non-null, then that space will be allocated around the
     * child within the layout area.  margin may be null.
     *
     * @param child the child being positioned within this region
     * @param areaX the horizontal offset of the layout area relative to this region
     * @param areaY the vertical offset of the layout area relative to this region
     * @param areaWidth  the width of the layout area
     * @param areaHeight the height of the layout area
     * @param areaBaselineOffset the baseline offset to be used if VPos is BASELINE
     * @param margin the margin of space to be allocated around the child
     * @param halignment the horizontal alignment for the child within the area
     * @param valignment the vertical alignment for the child within the area
     *
     */
    public static void positionInArea(Node child, double areaX, double areaY, double areaWidth, double areaHeight,
                               double areaBaselineOffset, Insets margin, HPos halignment, VPos valignment, boolean isSnapToPixel) {
        Insets childMargin = margin != null? margin : Insets.EMPTY;

        position(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset,
                snapSpace(childMargin.getTop(), isSnapToPixel), 
                snapSpace(childMargin.getRight(), isSnapToPixel),
                snapSpace(childMargin.getBottom(), isSnapToPixel), 
                snapSpace(childMargin.getLeft(), isSnapToPixel),
                halignment, valignment, isSnapToPixel);
    }

    /**
     * Utility method which lays out the child within an area of this
     * region defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
     * with a baseline offset relative to that area.
     * <p>
     * If the child is resizable, this method will resize it to fill the specified
     * area unless the node's maximum size prevents it.  If the node's maximum
     * size preference is less than the area size, the maximum size will be used.
     * If node's maximum is greater than the area size, then the node will be
     * resized to fit within the area, unless its minimum size prevents it.
     * <p>
     * If the child has a non-null contentBias, then this method will use it when
     * resizing the child.  If the contentBias is horizontal, it will set its width
     * first to the area's width (up to the child's max width limit) and then pass
     * that value to compute the child's height.  If child's contentBias is vertical,
     * then it will set its height to the area height (up to child's max height limit)
     * and pass that height to compute the child's width.  If the child's contentBias
     * is null, then it's width and height have no dependencies on each other.
     * <p>
     * If the child is not resizable (Shape, Group, etc) then it will only be
     * positioned and not resized.
     * <p>
     * If the child's resulting size differs from the area's size (either
     * because it was not resizable or it's sizing preferences prevented it), then
     * this function will align the node relative to the area using horizontal and
     * vertical alignment values.
     * If valignment is {@code VPos.BASELINE} then the node's baseline will be aligned
     * with the area baseline offset parameter, otherwise the baseline parameter
     * is ignored.
     * <p>
     * If {@code snapToPixel} is {@code true} for this region, then the resulting x,y
     * values will be rounded to their nearest pixel boundaries and the
     * width/height values will be ceiled to the next pixel boundary.
     *
     * @param child the child being positioned within this region
     * @param areaX the horizontal offset of the layout area relative to this region
     * @param areaY the vertical offset of the layout area relative to this region
     * @param areaWidth  the width of the layout area
     * @param areaHeight the height of the layout area
     * @param areaBaselineOffset the baseline offset to be used if VPos is BASELINE
     * @param halignment the horizontal alignment for the child within the area
     * @param valignment the vertical alignment for the child within the area
     *
     */
    protected void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               HPos halignment, VPos valignment) {
        layoutInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset,
                Insets.EMPTY, halignment, valignment);
    }

    /**
     * Utility method which lays out the child within an area of this
     * region defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
     * with a baseline offset relative to that area.
     * <p>
     * If the child is resizable, this method will resize it to fill the specified
     * area unless the node's maximum size prevents it.  If the node's maximum
     * size preference is less than the area size, the maximum size will be used.
     * If node's maximum is greater than the area size, then the node will be
     * resized to fit within the area, unless its minimum size prevents it.
     * <p>
     * If the child has a non-null contentBias, then this method will use it when
     * resizing the child.  If the contentBias is horizontal, it will set its width
     * first to the area's width (up to the child's max width limit) and then pass
     * that value to compute the child's height.  If child's contentBias is vertical,
     * then it will set its height to the area height (up to child's max height limit)
     * and pass that height to compute the child's width.  If the child's contentBias
     * is null, then it's width and height have no dependencies on each other.
     * <p>
     * If the child is not resizable (Shape, Group, etc) then it will only be
     * positioned and not resized.
     * <p>
     * If the child's resulting size differs from the area's size (either
     * because it was not resizable or it's sizing preferences prevented it), then
     * this function will align the node relative to the area using horizontal and
     * vertical alignment values.
     * If valignment is {@code VPos.BASELINE} then the node's baseline will be aligned
     * with the area baseline offset parameter, otherwise the baseline parameter
     * is ignored.
     * <p>
     * If {@code margin} is non-null, then that space will be allocated around the
     * child within the layout area.  margin may be null.
     * <p>
     * If {@code snapToPixel} is {@code true} for this region, then the resulting x,y
     * values will be rounded to their nearest pixel boundaries and the
     * width/height values will be ceiled to the next pixel boundary.
     *
     * @param child the child being positioned within this region
     * @param areaX the horizontal offset of the layout area relative to this region
     * @param areaY the vertical offset of the layout area relative to this region
     * @param areaWidth  the width of the layout area
     * @param areaHeight the height of the layout area
     * @param areaBaselineOffset the baseline offset to be used if VPos is BASELINE
     * @param margin the margin of space to be allocated around the child
     * @param halignment the horizontal alignment for the child within the area
     * @param valignment the vertical alignment for the child within the area
     */
    protected void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               Insets margin,
                               HPos halignment, VPos valignment) {
        layoutInArea(child, areaX, areaY, areaWidth, areaHeight,
                areaBaselineOffset, margin, true, true, halignment, valignment);
    }

    /**
     * Utility method which lays out the child within an area of this
     * region defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
     * with a baseline offset relative to that area.
     * <p>
     * If the child is resizable, this method will use {@code fillWidth} and {@code fillHeight}
     * to determine whether to resize it to fill the area or keep the child at its
     * preferred dimension.  If fillWidth/fillHeight are true, then this method
     * will only resize the child up to its max size limits.  If the node's maximum
     * size preference is less than the area size, the maximum size will be used.
     * If node's maximum is greater than the area size, then the node will be
     * resized to fit within the area, unless its minimum size prevents it.
     * <p>
     * If the child has a non-null contentBias, then this method will use it when
     * resizing the child.  If the contentBias is horizontal, it will set its width
     * first and then pass that value to compute the child's height.  If child's
     * contentBias is vertical, then it will set its height first
     * and pass that value to compute the child's width.  If the child's contentBias
     * is null, then it's width and height have no dependencies on each other.
     * <p>
     * If the child is not resizable (Shape, Group, etc) then it will only be
     * positioned and not resized.
     * <p>
     * If the child's resulting size differs from the area's size (either
     * because it was not resizable or it's sizing preferences prevented it), then
     * this function will align the node relative to the area using horizontal and
     * vertical alignment values.
     * If valignment is {@code VPos.BASELINE} then the node's baseline will be aligned
     * with the area baseline offset parameter, otherwise the baseline parameter
     * is ignored.
     * <p>
     * If {@code margin} is non-null, then that space will be allocated around the
     * child within the layout area.  margin may be null.
     * <p>
     * If {@code snapToPixel} is {@code true} for this region, then the resulting x,y
     * values will be rounded to their nearest pixel boundaries and the
     * width/height values will be ceiled to the next pixel boundary.
     *
     * @param child the child being positioned within this region
     * @param areaX the horizontal offset of the layout area relative to this region
     * @param areaY the vertical offset of the layout area relative to this region
     * @param areaWidth  the width of the layout area
     * @param areaHeight the height of the layout area
     * @param areaBaselineOffset the baseline offset to be used if VPos is BASELINE
     * @param margin the margin of space to be allocated around the child
     * @param fillWidth whether or not the child should be resized to fill the area width or kept to its preferred width
     * @param fillHeight whether or not the child should e resized to fill the area height or kept to its preferred height
     * @param halignment the horizontal alignment for the child within the area
     * @param valignment the vertical alignment for the child within the area
     */
    protected void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               Insets margin, boolean fillWidth, boolean fillHeight,
                               HPos halignment, VPos valignment) {
        layoutInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, margin, fillWidth, fillHeight, halignment, valignment, isSnapToPixel());
    }
    
    public static void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               Insets margin, boolean fillWidth, boolean fillHeight,
                               HPos halignment, VPos valignment, boolean isSnapToPixel) {
        
        Insets childMargin = margin != null? margin : Insets.EMPTY;
        double top = snapSpace(childMargin.getTop(), isSnapToPixel);
        double bottom = snapSpace(childMargin.getBottom(), isSnapToPixel);
        double left = snapSpace(childMargin.getLeft(), isSnapToPixel);
        double right = snapSpace(childMargin.getRight(), isSnapToPixel);
        if (child.isResizable()) {
            Orientation bias = child.getContentBias();

            double innerAreaWidth = areaWidth - left - right;
            double innerAreaHeight = areaHeight - top - bottom;

            double childWidth = 0;
            double childHeight = 0;

            if (bias == null) {
                childWidth = boundedSize(fillWidth? innerAreaWidth :
                                         Math.min(innerAreaWidth,child.prefWidth(-1)),
                                         child.minWidth(-1),child.maxWidth(-1));
                childHeight = boundedSize(fillHeight? innerAreaHeight :
                                         Math.min(innerAreaHeight,child.prefHeight(-1)),
                                         child.minHeight(-1), child.maxHeight(-1));

            } else if (bias == Orientation.HORIZONTAL) {
                childWidth = boundedSize(fillWidth? innerAreaWidth :
                                         Math.min(innerAreaWidth,child.prefWidth(-1)),
                                         child.minWidth(-1),child.maxWidth(-1));
                childHeight = boundedSize(fillHeight? innerAreaHeight :
                                         Math.min(innerAreaHeight,child.prefHeight(childWidth)),
                                         child.minHeight(childWidth),child.maxHeight(childWidth));

            } else { // bias == VERTICAL
                childHeight = boundedSize(fillHeight? innerAreaHeight :
                                         Math.min(innerAreaHeight,child.prefHeight(-1)),
                                         child.minHeight(-1),child.maxHeight(-1));
                childWidth = boundedSize(fillWidth? innerAreaWidth :
                                         Math.min(innerAreaWidth,child.prefWidth(childHeight)),
                                         child.minWidth(childHeight),child.maxWidth(childHeight));
            }
            child.resize(snapSize(childWidth, isSnapToPixel),snapSize(childHeight, isSnapToPixel));
        }
        position(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset,
                top, right, bottom, left, halignment, valignment, isSnapToPixel);
    }

    private static void position(Node child, double areaX, double areaY, double areaWidth, double areaHeight,
                          double areaBaselineOffset,
                          double topMargin, double rightMargin, double bottomMargin, double leftMargin,
                          HPos hpos, VPos vpos, boolean isSnapToPixel) {
        final double xoffset = leftMargin + computeXOffset(areaWidth - leftMargin - rightMargin,
                                                     child.getLayoutBounds().getWidth(), hpos);
        final double yoffset = topMargin +
                      (vpos == VPos.BASELINE?
                          areaBaselineOffset - child.getBaselineOffset() :
                          computeYOffset(areaHeight - topMargin - bottomMargin,
                                         child.getLayoutBounds().getHeight(), vpos));
        // do not snap position if child is not resizable because it can cause gaps
        final double x = child.isResizable()? snapPosition(areaX + xoffset, isSnapToPixel) : areaX + xoffset;
        final double y = child.isResizable()? snapPosition(areaY + yoffset, isSnapToPixel) : areaY + yoffset;

        child.relocate(x,y);
    }

     /**************************************************************************
     *                                                                         *
     * PG Implementation                                                       *
     *                                                                         *
     **************************************************************************/

    /** @treatAsPrivate */
    @Override public void impl_updatePG() {
        super.impl_updatePG();
        PGRegion pg = (PGRegion) impl_getPGNode();

        if (impl_isDirty(DirtyBits.NODE_GEOMETRY)) {
            pg.setSize((float)getWidth(), (float)getHeight());
        }

        if (impl_isDirty(DirtyBits.SHAPE_FILL)) {
            // sync the backgrounds
            BackgroundFill lastBf = null;
            try {
                final List<BackgroundFill> background_fills = getBackgroundFills();
                final int backgroundFillCount = background_fills != null ? background_fills.size() : 0;

                com.sun.javafx.sg.BackgroundFill[] b = new com.sun.javafx.sg.BackgroundFill[backgroundFillCount];
                for (int i = 0; i < backgroundFillCount; i++) {
                    lastBf = background_fills.get(i);

                    b[i] = new com.sun.javafx.sg.BackgroundFill(
                        Toolkit.getToolkit().getPaint(background_fills.get(i).getFill()),
                        (float)background_fills.get(i).getTopLeftCornerRadius(),
                        (float)background_fills.get(i).getTopRightCornerRadius(),
                        (float)background_fills.get(i).getBottomLeftCornerRadius(),
                        (float)background_fills.get(i).getBottomRightCornerRadius(),
                        (float)background_fills.get(i).getOffsets().getTop(), (float)background_fills.get(i).getOffsets().getLeft(),
                        (float)background_fills.get(i).getOffsets().getBottom(), (float)background_fills.get(i).getOffsets().getRight()
                    );
                }
                pg.setBackgroundFills(b);
             } catch (Exception e) {
                System.out.println("Failed to apply background fills to region ["+getStyleClass()+"]");
                System.out.println("    because: " + e.getMessage());
                System.out.println("    failed bf=" + lastBf);
                if (lastBf != null && lastBf.getFill() instanceof LinearGradient) {
                    LinearGradient g = (LinearGradient) lastBf.getFill();
                    System.out.println("       startX="+g.getStartX()+" startY="+g.getStartY()+" endX="+g.getEndX()+" endY="+g.getStartY()+" proportional="+g.isProportional()+" cycleMethod="+g.getCycleMethod());
                    System.out.println("       stops=");
                    for (Stop stop : g.getStops()) {
                        System.out.println(" ("+stop.getOffset()+","+stop.getColor()+")");
                    }
                    System.out.println(" ");
                }
                e.printStackTrace();
             }
        }

        if (impl_isDirty(DirtyBits.SHAPE_STROKE)) {
            // sync the borders

            final List<BorderImage> image_borders = getImageBorders();
            final int imageBorderCount = image_borders != null? image_borders.size() : 0;
            final List<StrokeBorder> stroke_borders = getStrokeBorders();
            final int strokeBorderCount = stroke_borders != null? stroke_borders.size() : 0;

            com.sun.javafx.sg.Border[] b2 = new com.sun.javafx.sg.Border[imageBorderCount + strokeBorderCount];

            for (int i = 0; i < imageBorderCount; i++) {

                // width multiplier horizontal direction
                double hwx = image_borders.get(i).isProportionalWidth() ? getWidth() : 1;

                // width multiplier vertical direction
                double vwx = image_borders.get(i).isProportionalWidth() ? getHeight() : 1;

                // slice multiplier horizontal direction
                double hsx = image_borders.get(i).isProportionalSlice() ? image_borders.get(i).getImage().getWidth() : 1.0f;

                // slice multiplier vertical direction
                double vsx = image_borders.get(i).isProportionalSlice() ? image_borders.get(i).getImage().getHeight() : 1.0f;

                b2[i] = new com.sun.javafx.sg.ImageBorder(
                    (float) (image_borders.get(i).getTopWidth()*vwx),
                    (float) (image_borders.get(i).getLeftWidth()*hwx),
                    (float) (image_borders.get(i).getBottomWidth()*vwx),
                    (float) (image_borders.get(i).getRightWidth()*hwx),
                    (float)image_borders.get(i).getOffsets().getTop(), (float)image_borders.get(i).getOffsets().getLeft(),
                    (float)image_borders.get(i).getOffsets().getBottom(), (float)image_borders.get(i).getOffsets().getRight(),
                    image_borders.get(i).getImage().impl_getPlatformImage(),
                    (float) (image_borders.get(i).getTopSlice()*vsx),
                    (float) (image_borders.get(i).getLeftSlice()*hsx),
                    (float) (image_borders.get(i).getBottomSlice()*vsx),
                    (float) (image_borders.get(i).getRightSlice()*hsx),
                    com.sun.javafx.sg.Repeat.values()[image_borders.get(i).getRepeatX().ordinal()],
                    com.sun.javafx.sg.Repeat.values()[image_borders.get(i).getRepeatY().ordinal()],
                    image_borders.get(i).isFillCenter()
                );
            }
            for (int i = 0; i < strokeBorderCount; i++) {

                // width multiplier horizontal direction
                double hwx = stroke_borders.get(i).isProportionalWidth() ? getWidth() : 1;

                // width multiplier vertical direction
                double vwx = stroke_borders.get(i).isProportionalWidth() ? getHeight() : 1;

                b2[i+imageBorderCount] = new com.sun.javafx.sg.StrokedBorder (
                    (float) (stroke_borders.get(i).getTopWidth()*vwx),
                    (float) (stroke_borders.get(i).getLeftWidth()*hwx),
                    (float) (stroke_borders.get(i).getBottomWidth()*vwx),
                    (float) (stroke_borders.get(i).getRightWidth()*hwx),
                    (float) stroke_borders.get(i).getOffsets().getTop(), (float) stroke_borders.get(i).getOffsets().getLeft(),
                    (float) stroke_borders.get(i).getOffsets().getBottom(), (float) stroke_borders.get(i).getOffsets().getRight(),
                    (float) stroke_borders.get(i).getTopLeftCornerRadius(),
                    (float) stroke_borders.get(i).getTopRightCornerRadius(),
                    (float) stroke_borders.get(i).getBottomLeftCornerRadius(),
                    (float) stroke_borders.get(i).getBottomRightCornerRadius(),
                    Toolkit.getToolkit().getPaint(stroke_borders.get(i).getTopFill()),
                    Toolkit.getToolkit().getPaint(stroke_borders.get(i).getLeftFill()),
                    Toolkit.getToolkit().getPaint(stroke_borders.get(i).getBottomFill()),
                    Toolkit.getToolkit().getPaint(stroke_borders.get(i).getRightFill()),
                    createSgBorderStyle(stroke_borders.get(i).getTopStyle()),
                    createSgBorderStyle(stroke_borders.get(i).getLeftStyle()),
                    createSgBorderStyle(stroke_borders.get(i).getBottomStyle()),
                    createSgBorderStyle(stroke_borders.get(i).getRightStyle())
                );
            }
            pg.setBorders(b2);
        }

        if (impl_isDirty(DirtyBits.NODE_CONTENTS)) {
            // sync the background images
            final List<BackgroundImage> background_images = getBackgroundImages();
            final int imageCount = background_images != null? background_images.size() : 0;
            com.sun.javafx.sg.BackgroundImage images[] = new com.sun.javafx.sg.BackgroundImage[imageCount];
            if (background_images != null) {
                for (int i = 0; i < imageCount; i++) {
                    BackgroundImage bImg = background_images.get(i);
                    images[i] = new com.sun.javafx.sg.BackgroundImage(
                        bImg.getImage().impl_getPlatformImage(),
                        Repeat.values()[bImg.getRepeatX().ordinal()],
                        Repeat.values()[bImg.getRepeatY().ordinal()],
                        (float)bImg.getTop(), (float) bImg.getLeft(),
                        (float)bImg.getBottom(), (float) bImg.getRight(),
                        (float)bImg.getWidth(), (float)bImg.getHeight(),
                        bImg.isProportionalHPos(),bImg.isProportionalVPos(),
                        bImg.isProportionalWidth(),bImg.isProportionalHeight(),
                        bImg.isContain(), bImg.isCover()
                    );
                }
            }
            pg.setBackgroundImages(images);
        }

        if (impl_isDirty(DirtyBits.REGION_SHAPE)) {
            // sync the shape, and scaleShape
            final Shape theShape = getShape();
            if (theShape == null) {
                pg.setShape(null);
            } else {
                theShape.impl_syncPGNode();
                pg.setShape((PGShape)theShape.impl_getPGNode());
            }

            //pg.setShape(null);
            pg.setResizeShape(getScaleShape());
            pg.setPositionShape(getPositionShape());
        }
    }

    private com.sun.javafx.sg.BorderStyle createSgBorderStyle(BorderStyle bs) {
        if (bs != null && bs != BorderStyle.NONE) {
            if (bs == BorderStyle.SOLID) return com.sun.javafx.sg.BorderStyle.SOLID;
            double[] strokeDashArray = (bs.getStrokeDashArray() != null) ?
                bs.getStrokeDashArray() : new double[0];
            float[] arr = new float[strokeDashArray.length];
            for (int i=0; i<arr.length; i++) arr[i] = (float)strokeDashArray[i];
            return new com.sun.javafx.sg.BorderStyle(
                toPGStrokeType(bs.getStrokeType()),
                toPGLineCap(bs.getStrokeLineCap()),
                toPGLineJoin(bs.getStrokeLineJoin()),
                (float)bs.getStrokeMiterLimit(),
                arr,
                (float)bs.getStrokeDashOffset()
            );
        }
        // BorderStyle was NONE
        return null;
    }


    /** @treatAsPrivate */
    @Override public PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGRegion();
    }

    /**
     * Some skins relying on this
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_computeContains(double localX, double localY) {        
        double bx0 = 0.0f;
        double by0 = 0.0f;
        double bx1 = getWidth();
        double by1 = getHeight();
        final Shape theShape = getShape();
        if (theShape != null && getScaleShape() == false) {
            if (theShape.contains(localX,localY)) {
                return true;
            }
            double shapeWidth = theShape.getLayoutBounds().getWidth();
            double shapeHeight = theShape.getLayoutBounds().getHeight();
            if (positionShape != null && positionShape.get()) {
                bx0 = (getWidth() - shapeWidth)/2;
                by0 = (getHeight() - shapeHeight)/2;
                bx1 = bx0+shapeWidth;
                by1 = by0+shapeHeight;
            } else {
                bx0 = theShape.getLayoutBounds().getMinX();
                by0 = theShape.getLayoutBounds().getMinY();
                bx1 = theShape.getLayoutBounds().getMaxX();
                by1 = theShape.getLayoutBounds().getMaxY();
            }
        }
        
        final List<BackgroundFill> background_fills = getBackgroundFills();
        if (background_fills != null) {
            for (int i = 0; i < background_fills.size(); i++) {
                Insets offsets = background_fills.get(i).getOffsets();

                if (background_fills.get(i).getFill() != null) {
                    double rrx0 = bx0 + offsets.getLeft();
                    double rry0 = by0 + offsets.getTop();
                    double rrx1 = bx1 - offsets.getRight();
                    double rry1 = by1 - offsets.getBottom();

                    // Check for trivial rejection - point is inside bounding rectangle
                    if (localX >= rrx0 && localY >= rry0 && localX < rrx1 && localY < rry1) {
                        double tlr = background_fills.get(i).getTopLeftCornerRadius() / 2f;
                        double trr = background_fills.get(i).getTopRightCornerRadius() / 2f;
                        double blr = background_fills.get(i).getBottomLeftCornerRadius() / 2f;
                        double brr = background_fills.get(i).getBottomRightCornerRadius() / 2f;

                        // need to check if pt is outside rounded corners
                        double x = 1.0;
                        double y = 1.0;

                        if (tlr != 0 && localX < rrx0 + tlr && localY < rry0 + tlr) {
                            // pt is in top-left corner
                            x = (localX - (rrx0 + tlr))/ tlr;
                            y = (localY - (rry0 + tlr))/ tlr;

                        } else if (blr != 0 && localX < rrx0 + blr && localY > rry1 - blr) {
                            // pt is in bottom-left corner
                            x = (localX - (rrx0 + blr))/ blr;
                            y = (localY - (rry1 - blr))/ blr;

                        } else if (trr != 0 && localX > rrx1 - trr && localY < rry0 + trr) {
                            // pt is in top-right corner
                            x = (localX - (rrx1 - trr))/ trr;
                            y = (localY - (rry0 + trr))/ trr;

                        } else if (brr != 0 && localX > rrx1 - brr && localY > rry1 - brr) {
                            // pt is in bottom-right corner
                            x = (localX - (rrx1 - brr))/ brr;
                            y = (localY - (rry1 - brr))/ brr;

                        } else {
                            // pt within fill and not within area with a rounded corner either because
                            // there are no rounded corners or the pt doesn't fall inside one
                            // break and try background images, image border or stroke border
                            // to determine if the pt is within the area.
                            break;
                        }
                        if (x * x + y * y < .50) {
                            // pt within rounded corner!
                            return true;
                        }
                        // pt outside of rounded corner, so no hit within this fill
                    }
                }
            }
        }

        final List<BackgroundImage> background_images = getBackgroundImages();
        if (background_images != null) {
            for (int i = 0; i < background_images.size(); i++) {
                if (localX >= (bx0 + background_images.get(i).getLeft()) &&
                    localX <= (bx1 - background_images.get(i).getRight()) &&
                    localY >= (by0 + background_images.get(i).getTop()) &&
                    localY <= (by1 - background_images.get(i).getBottom())) {
                    return true;
                }
            }
        }
        
        final List<BorderImage> image_borders = getImageBorders();
        if (image_borders != null) {
            for (int i = 0; i < image_borders.size(); i++) {
                Insets offsets = image_borders.get(i).getOffsets();
                if (borderContains(localX, localY,
                        bx0 + offsets.getLeft(), by0 + offsets.getTop(),
                        bx1 - offsets.getRight(), by1 - offsets.getBottom(),
                        image_borders.get(i).getTopWidth(), image_borders.get(i).getRightWidth(),
                        image_borders.get(i).getBottomWidth(), image_borders.get(i).getLeftWidth())) {
                     return true;
                }
            }
        }
        
        final List<StrokeBorder> stroke_borders = getStrokeBorders();
        if (stroke_borders != null) {
            for (int i = 0; i < stroke_borders.size(); i++) {
                Insets offsets = stroke_borders.get(i).getOffsets();
                if (borderContains(localX, localY,
                        bx0 + offsets.getLeft(), by0 + offsets.getTop(),
                        bx1 - offsets.getRight(), by1 - offsets.getBottom(),
                        stroke_borders.get(i).getTopWidth(), stroke_borders.get(i).getRightWidth(),
                        stroke_borders.get(i).getBottomWidth(), stroke_borders.get(i).getLeftWidth())) {
                     return true;
                }
            }
        }
        return false;
    }

    // tests to see if x,y is within border with top, right, bottom, left thicknesses
    private boolean borderContains(double x, double y,
                                   double bx0, double by0, double bx1, double by1,
                                   double top, double right, double bottom, double left) {

        return (((x >= bx0 && x <= bx0 + left || ((x >= bx1 - right) && x <= bx1)) && y >= by0 && y <= by1) ||
                (((y >= by0 && y <= by0 + top) || ((y >= by1 - bottom) && y <= by1)) && x >= bx0 && x <= bx1));

    }

    /**
     * Some skins relying on this
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected Node impl_pickNodeLocal(double localX, double localY) {
        if (containsBounds(localX, localY)) {
            ObservableList<Node> children = getChildren();
            for (int i = children.size()-1; i >= 0; i--) {
                Node picked = children.get(i).impl_pickNode(localX, localY);
                if (picked != null) {
                    return picked;
                }
            }
            if (contains(localX, localY)) {
                return this;
            }
        }
        return null;
    }

    /**
     * Some skins relying on this
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected Node impl_pickNodeLocal(PickRay pickRay) {

        if (impl_intersects(pickRay)) {
            ObservableList<Node> children = getChildren();

            for (int i = children.size()-1; i >= 0; i--) {
                Node picked = children.get(i).impl_pickNode(pickRay);

                if (picked != null) {
                    return picked;
                }
            }

            return this;
        }

        return null;
    }

    /**
     * The layout bounds of this region: {@code 0, 0  width x height}
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected Bounds impl_computeLayoutBounds() {
        return new BoundingBox(0, 0, 0, getWidth(), getHeight(), 0);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override final protected void impl_notifyLayoutBoundsChanged() {
        // override Node's default behavior of having a geometric bounds change
        // trigger a change in layoutBounds. For Resizable nodes, layoutBounds
        // is unrelated to geometric bounds.
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        // calculate the bounds of the main shape
        double bx1 = 0.0f;
        double by1 = 0.0f;
        double bx2 = getWidth();
        double by2 = getHeight();
        final Shape theShape = getShape();
        if (theShape != null && getScaleShape() == false) {
            double shapeWidth = theShape.getLayoutBounds().getWidth();
            double shapeHeight = theShape.getLayoutBounds().getHeight();
            if (getPositionShape()) {
                bx1 = (getWidth() - shapeWidth)/2;
                by1 = (getHeight() - shapeHeight)/2;
                bx2 = bx1+shapeWidth;
                by2 = by1+shapeHeight;
            } else {
                bx1 = theShape.getLayoutBounds().getMinX();
                by1 = theShape.getLayoutBounds().getMinY();
                bx2 = theShape.getLayoutBounds().getMaxX();
                by2 = theShape.getLayoutBounds().getMaxY();
            }
        }
        double x1 = bx1;
        double y1 = by1;
        double x2 = bx2;
        double y2 = by2;
        // calculate the bounds accounting for offsets and strokes
        final List<BackgroundFill> background_fills = getBackgroundFills();
        if (background_fills != null) {
            for (int i = 0; i < background_fills.size(); i++) {
                Insets offsets = background_fills.get(i).getOffsets();
                x1 = Math.min(x1, bx1 + offsets.getLeft());
                y1 = Math.min(y1, by1 + offsets.getTop());
                x2 = Math.max(x2, bx2 - offsets.getRight());
                y2 = Math.max(y2, by2 - offsets.getBottom());
            }
        }
        
        final List<BackgroundImage> background_images = getBackgroundImages();
        if (background_images != null) {
            for (int i = 0; i < background_images.size(); i++) {
                x1 = Math.min(x1, bx1 + background_images.get(i).getLeft());
                y1 = Math.min(y1, by1 + background_images.get(i).getTop());
                x2 = Math.max(x2, bx2 - background_images.get(i).getRight());
                y2 = Math.max(y2, by2 - background_images.get(i).getBottom());
            }
        }
        
        final List<BorderImage> image_borders = getImageBorders();
        if (image_borders != null) {
            for (int i = 0; i < image_borders.size(); i++) {
                Insets offsets = image_borders.get(i).getOffsets();
                // stoked borders assume centered strokes for now
                x1 = Math.min(x1, bx1 + offsets.getLeft() - (image_borders.get(i).getLeftWidth()/2.0));
                y1 = Math.min(y1, by1 + offsets.getTop() - (image_borders.get(i).getTopWidth()/2.0));
                x2 = Math.max(x2, bx2 - offsets.getRight() + (image_borders.get(i).getRightWidth()/2.0));
                y2 = Math.max(y2, by2 - offsets.getBottom() + (image_borders.get(i).getBottomWidth()/2.0));
            }
        }
        
        final List<StrokeBorder> stroke_borders = getStrokeBorders();
        if (stroke_borders != null) {
            for (int i = 0; i < stroke_borders.size(); i++) {
                Insets offsets = stroke_borders.get(i).getOffsets();
                // stoked borders assume centered strokes for now
                x1 = Math.min(x1, bx1 + offsets.getLeft() - (stroke_borders.get(i).getLeftWidth()/2.0));
                y1 = Math.min(y1, by1 + offsets.getTop() - (stroke_borders.get(i).getTopWidth()/2.0));
                x2 = Math.max(x2, bx2 - offsets.getRight() + (stroke_borders.get(i).getRightWidth()/2.0));
                y2 = Math.max(y2, by2 - offsets.getBottom() + (stroke_borders.get(i).getBottomWidth()/2.0));
            }
        }

        // NOTE: Okay to call impl_computeGeomBounds with tx even in the 3D case
        // since Parent.impl_computeGeomBounds does handle 3D correctly.
        BaseBounds cb = super.impl_computeGeomBounds(bounds, tx);
        /*
         * This is a work around for RT-7680. Parent returns invalid bounds from
         * impl_computeGeomBounds when it has no children or if all its children
         * have invalid bounds. If RT-7680 were fixed, then we could omit this
         * first branch of the if and only use the else since the correct value
         * would be computed.
         */
        if (cb.isEmpty()) {
            // there are no children bounds
            bounds = bounds.deriveWithNewBounds((float)x1, (float)y1, 0.0f,
                    (float)x2, (float)y2, 0.0f);
            bounds = tx.transform(bounds, bounds);
            return bounds;
        } else {
            // union with children's bounds
            BaseBounds tempBounds = TempState.getInstance().bounds;
            tempBounds = tempBounds.deriveWithNewBounds(
                                 (float)x1, (float)y1, 0.0f,
                                 (float)x2, (float)y2, 0.0f);
            BaseBounds bb = tx.transform(tempBounds, tempBounds);
            cb = cb.deriveWithUnion(bb);
            return cb;
        }
    }

    PGShape.StrokeLineCap toPGLineCap(StrokeLineCap t) {
        if (t == StrokeLineCap.SQUARE) {
            return PGShape.StrokeLineCap.SQUARE;
        } else if (t == StrokeLineCap.BUTT) {
            return PGShape.StrokeLineCap.BUTT;
        } else {
            return PGShape.StrokeLineCap.ROUND;
        }
    }

    PGShape.StrokeLineJoin toPGLineJoin(StrokeLineJoin t) {
        if (t == StrokeLineJoin.MITER) {
            return PGShape.StrokeLineJoin.MITER;
        } else if (t == StrokeLineJoin.BEVEL) {
            return PGShape.StrokeLineJoin.BEVEL;
        } else {
            return PGShape.StrokeLineJoin.ROUND;
        }
    }

    PGShape.StrokeType toPGStrokeType(StrokeType t) {
        if (t == StrokeType.INSIDE) {
            return PGShape.StrokeType.INSIDE;
        } else if (t == StrokeType.OUTSIDE) {
            return PGShape.StrokeType.OUTSIDE;
        } else {
            return PGShape.StrokeType.CENTERED;
        }
    }

    /***************************************************************************
     *                                                                         *
     * CSS                                                                     *
     *                                                                         *
     **************************************************************************/

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatAsPrivate implementation detail
      */
     private static class StyleableProperties {
         private static final StyleableProperty<Region,Insets> PADDING =
             new StyleableProperty<Region,Insets>("-fx-padding",
                 InsetsConverter.getInstance(), Insets.EMPTY) {

            @Override
            public boolean isSettable(Region node) {
                return node.padding == null || !node.padding.isBound();
            }

            @Override
            public WritableValue<Insets> getWritableValue(Region node) {
                return node.paddingProperty();
            }
                     
         };

         private static final StyleableProperty<Region,List<BackgroundFill>> BACKGROUND_FILLS =
             new StyleableProperty<Region,List<BackgroundFill>>("-fx-background-fills", 
                 BackgroundFillConverter.getInstance(),
                 null,
                 false,
                 BackgroundFill.impl_CSS_STYLEABLES()) {

            @Override
            public boolean isSettable(Region node) {
                return node.backgroundFills == null || !node.backgroundFills.isBound();                        
            }

            @Override
            public WritableValue<List<BackgroundFill>> getWritableValue(Region node) {
                return node.backgroundFillsProperty();
            }
                     
         };

         private static final StyleableProperty<Region,List<BackgroundImage>> BACKGROUND_IMAGES =
             new StyleableProperty<Region,List<BackgroundImage>>("-fx-background-images",
                 BackgroundImageConverter.getInstance(),
                 null,
                 false,
                 BackgroundImage.impl_CSS_STYLEABLES()) {

            @Override
            public boolean isSettable(Region node) {
                return node.backgroundImages == null || !node.backgroundImages.isBound();
            }

            @Override
            public WritableValue<List<BackgroundImage>> getWritableValue(Region node) {
                return node.backgroundImagesProperty();
            }
                     
         };

        private static final StyleableProperty<Region,List<BorderImage>> IMAGE_BORDERS =
            new StyleableProperty<Region,List<BorderImage>>("-fx-image-borders", 
                BorderImageConverter.getInstance(),
                null,
                false,
                BorderImage.impl_CSS_STYLEABLES()) {

            @Override
            public boolean isSettable(Region node) {
                return node.imageBorders == null || !node.imageBorders.isBound();
            }

            @Override
            public WritableValue<List<BorderImage>> getWritableValue(Region node) {
                return node.imageBordersProperty();
            }
        };

         private static final StyleableProperty<Region,List<StrokeBorder>> STROKE_BORDERS =
             new StyleableProperty<Region,List<StrokeBorder>>("-fx-stroke-borders",
                 StrokeBorderConverter.getInstance(),
                 null,
                 false,
                 StrokeBorder.impl_CSS_STYLEABLES()) {

            @Override
            public boolean isSettable(Region node) {
                return node.strokeBorders == null || !node.strokeBorders.isBound();
            }

            @Override
            public WritableValue<List<StrokeBorder>> getWritableValue(Region node) {
                return node.strokeBordersProperty();
            }
                     
         };

         private static final StyleableProperty<Region,String> SHAPE = 
             new StyleableProperty<Region,String>("-fx-shape",
                 StringConverter.getInstance()) {

            @Override
            public boolean isSettable(Region node) {
                // isSettable depends on node.shape, not node.shapeContent
                return node.shape == null || !node.shape.isBound();
            }

            @Override
            public WritableValue<String> getWritableValue(Region node) {
                return node.shapeContentProperty();
            }
         };

         private static final StyleableProperty<Region, Boolean> SCALE_SHAPE = 
             new StyleableProperty<Region,Boolean>("-fx-scale-shape",
                 BooleanConverter.getInstance(), Boolean.TRUE){

            @Override
            public boolean isSettable(Region node) {
                return node.scaleShape == null || !node.scaleShape.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(Region node) {
                return node.scaleShapeProperty();
            }
        };

         private static final StyleableProperty<Region,Boolean> POSITION_SHAPE = 
             new StyleableProperty<Region,Boolean>("-fx-position-shape",
                 BooleanConverter.getInstance(), Boolean.TRUE){

            @Override
            public boolean isSettable(Region node) {
                return node.positionShape == null || !node.positionShape.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(Region node) {
                return node.positionShapeProperty();
            }
        };

         private static final StyleableProperty<Region, Boolean> SNAP_TO_PIXEL = 
             new StyleableProperty<Region,Boolean>("-fx-snap-to-pixel",
                 BooleanConverter.getInstance(), Boolean.TRUE){

            @Override
            public boolean isSettable(Region node) {
                return node.snapToPixel == null ||
                        !node.snapToPixel.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(Region node) {
                return node.snapToPixelProperty();
            }
        };

         private static final List<StyleableProperty> STYLEABLES;
         static {

            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Parent.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                    PADDING,
                    BACKGROUND_FILLS,
                    BACKGROUND_IMAGES,
                    IMAGE_BORDERS,
                    STROKE_BORDERS,
                    SHAPE,
                    SCALE_SHAPE,
                    POSITION_SHAPE,
                    SNAP_TO_PIXEL
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
         return Region.StyleableProperties.STYLEABLES;
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

}
