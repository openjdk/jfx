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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.Shape;
import com.sun.javafx.Logging;
import com.sun.javafx.TempState;
import com.sun.javafx.binding.ExpressionHelper;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.InsetsConverter;
import com.sun.javafx.css.converters.ShapeConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGRegion;
import com.sun.javafx.tk.Toolkit;
import sun.util.logging.PlatformLogger;

/**
 * Region is the base class for all JavaFX Node-based UI Controls, and all layout containers.
 * It is a resizable Parent node which can be styled from CSS. It can have multiple backgrounds
 * and borders. It is designed to support as much of the CSS3 specification for backgrounds
 * and borders as is relevant to JavaFX. 
 * The full specification is available at <a href="http://www.w3.org/TR/2012/CR-css3-background-20120724/">the W3C</a>.
 * <p/>
 * Every Region has its layout bounds, which are specified to be (0, 0, width, height). A Region might draw outside
 * these bounds. The content area of a Region is the area which is occupied for the layout of its children.
 * This area is, by default, the same as the layout bounds of the Region, but can be modified by either the
 * properties of a border (either with BorderStrokes or BorderImages), and by padding. The padding can
 * be negative, such that the content area of a Region might extend beyond the layout bounds of the Region,
 * but does not affect the layout bounds.
 * <p/>
 * A Region has a Background, and a Border, although either or both of these might be empty. The Background
 * of a Region is made up of zero or more BackgroundFills, and zero or more BackgroundImages. Likewise, the
 * border of a Region is defined by its Border, which is made up of zero or more BorderStrokes and
 * zero or more BorderImages. All BackgroundFills are drawn first, followed by BackgroundImages, BorderStrokes,
 * and finally BorderImages. The content is drawn above all backgrounds and borders. If a BorderImage is
 * present (and loaded all images properly), then no BorderStrokes are actually drawn, although they are
 * considered for computing the position of the content area (see the stroke width property of a BorderStroke).
 * These semantics are in line with the CSS 3 specification. The purpose of these semantics are to allow an
 * application to specify a fallback BorderStroke to be displayed in the case that an ImageStroke fails to
 * download or load.
 * <p/>
 * By default a Region appears as a Rectangle. A BackgroundFill radii might cause the Rectangle to appear rounded.
 * This affects not only making the visuals look like a rounded rectangle, but it also causes the picking behavior
 * of the Region to act like a rounded rectangle, such that locations outside the corner radii are ignored. A
 * Region can be made to use any shape, however, by specifing the {@code shape} property. If a shape is specified,
 * then all BackgroundFills, BackgroundImages, and BorderStrokes will be applied to the shape. BorderImages are
 * not used for Regions which have a shape specified.
 * <p/>
 * A Region with a shape
 * <p/>
 * Although the layout bounds of a Region are not influenced by any Border or Background, the content area
 * insets and the picking area of the Region are. The {@code insets} of the Region define the distance
 * between the edge of the layout bounds and the edge of the content area. For example, if the Region
 * layout bounds are (x=0, y=0, width=200, height=100), and the insets are (top=10, right=20, bottom=30, left=40),
 * then the content area bounds will be (x=40, y=10, width=140, height=60). A Region subclass which is laying
 * out its children should compute and honor these content area bounds.
 * <p/>
 * By default a Region inherits the layout behavior of its superclass, {@link Parent},
 * which means that it will resize any resizable child nodes to their preferred
 * size, but will not reposition them.  If an application needs more specific
 * layout behavior, then it should use one of the Region subclasses:
 * {@link StackPane}, {@link HBox}, {@link VBox}, {@link TilePane}, {@link FlowPane},
 * {@link BorderPane}, {@link GridPane}, or {@link AnchorPane}.
 * <p/>
 * To implement a more custom layout, a Region subclass must override
 * {@link #computePrefWidth(double) computePrefWidth}, {@link #computePrefHeight(double) computePrefHeight}, and
 * {@link #layoutChildren() layoutChildren}. Note that {@link #layoutChildren() layoutChildren} is called automatically
 * by the scene graph while executing a top-down layout pass and it should not be invoked directly by the
 * region subclass.
 * <p/>
 * Region subclasses which layout their children will position nodes by setting
 * {@link #setLayoutX(double) layoutX}/{@link #setLayoutY(double) layoutY} and do not alter
 * {@link #setTranslateX(double) translateX}/{@link #setTranslateY(double) translateY}, which are reserved for
 * adjustments and animation.
 */
public class Region extends Parent {

    /**
     * Sentinel value which can be passed to a region's
     * {@link #setMinWidth(double) setMinWidth},
     * {@link #setMinHeight(double) setMinHeight},
     * {@link #setMaxWidth(double) setMaxWidth} or
     * {@link #setMaxHeight(double) setMaxHeight}
     * methods to indicate that the preferred dimension should be used for that max and/or min constraint.
     */
    public static final double USE_PREF_SIZE = Double.NEGATIVE_INFINITY;

    /**
     * Sentinel value which can be passed to a region's
     * {@link #setMinWidth(double) setMinWidth},
     * {@link #setMinHeight(double) setMinHeight},
     * {@link #setPrefWidth(double) setPrefWidth},
     * {@link #setPrefHeight(double) setPrefHeight},
     * {@link #setMaxWidth(double) setMaxWidth},
     * {@link #setMaxHeight(double) setMaxHeight} methods
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

    /**
     * Computes the value based on the given min and max values. We encode in this
     * method the logic surrounding various edge cases, such as when the min is
     * specified as greater than the max, or the max less than the min, or a pref
     * value that exceeds either the max or min in their extremes.
     * <p/>
     * If the min is greater than the max, then we want to make sure the returned
     * value is the min. In other words, in such a case, the min becomes the only
     * acceptable return value.
     * <p/>
     * If the min and max values are well ordered, and the pref is less than the min
     * then the min is returned. Likewise, if the values are well ordered and the
     * pref is greater than the max, then the max is returned. If the pref lies
     * between the min and the max, then the pref is returned.
     *
     *
     * @param min The minimum bound
     * @param pref The value to be clamped between the min and max
     * @param max the maximum bound
     * @return the size bounded by min, pref, and max.
     */
    static double boundedSize(double min, double pref, double max) {
        double a = pref >= min ? pref : min;
        double b = min >= max ? min : max;
        return a <= b ? a : b;
    }

    /**
     * If snapToPixel is true, then the value is rounded using Math.round. Otherwise,
     * the value is simply returned. This method will surely be JIT'd under normal
     * circumstances, however on an interpreter it would be better to inline this
     * method. However the use of Math.round here, and Math.ceil in snapSize is
     * not obvious, and so for code maintenance this logic is pulled out into
     * a separate method.
     *
     * @param value The value that needs to be snapped
     * @param snapToPixel Whether to snap to pixel
     * @return value either as passed in or rounded based on snapToPixel
     */
    private static double snapSpace(double value, boolean snapToPixel) {
        return snapToPixel ? Math.round(value) : value;
    }

    /**
     * If snapToPixel is true, then the value is ceil'd using Math.ceil. Otherwise,
     * the value is simply returned.
     *
     * @param value The value that needs to be snapped
     * @param snapToPixel Whether to snap to pixel
     * @return value either as passed in or ceil'd based on snapToPixel
     */
    private static double snapSize(double value, boolean snapToPixel) {
        return snapToPixel ? Math.ceil(value) : value;
    }

    /**
     * If snapToPixel is true, then the value is rounded using Math.round. Otherwise,
     * the value is simply returned.
     *
     * @param value The value that needs to be snapped
     * @param snapToPixel Whether to snap to pixel
     * @return value either as passed in or rounded based on snapToPixel
     */
    private static double snapPosition(double value, boolean snapToPixel) {
        return snapToPixel ? Math.round(value) : value;
    }

    static double getMaxAreaBaselineOffset(List<Node> content, Insets margins[]) {
        double max = 0;
        for (int i = 0, maxPos = content.size(); i < maxPos; i++) {
            final Node node = content.get(i);
            final double topMargin = margins[i] != null ? margins[i].getTop() : 0;
            final double position = topMargin + node.getBaselineOffset();
            max = max >= position ? max : position; // Math.max
        }
        return max;
    }

    static double getMaxBaselineOffset(List<Node> content) {
        double max = 0;
        for (int i = 0, maxPos = content.size(); i < maxPos; i++) {
            final Node node = content.get(i);
            final double baselineOffset = node.getBaselineOffset();
            max = max >= baselineOffset ? max : baselineOffset; // Math.max
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
            default:
                throw new AssertionError("Unhandled hPos");
        }
    }

    static double computeYOffset(double height, double contentHeight, VPos vpos) {
        switch(vpos) {
            case BASELINE:
            case TOP:
                return 0;
            case CENTER:
                return (height - contentHeight) / 2;
            case BOTTOM:
                return height - contentHeight;
            default:
                throw new AssertionError("Unhandled vPos");
        }
    }

    static double[] createDoubleArray(int length, double value) {
        double[] array = new double[length];
        for (int i = 0; i < length; i++) {
            array[i] = value;
        }
        return array;
    }


    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    
    /**
     * Creates a new Region with an empty Background and and empty Border. The
     * Region defaults to having pickOnBounds set to true, meaning that any pick
     * (mouse picking or touch picking etc) that occurs within the bounds in local
     * of the Region will return true, regardless of whether the Region is filled
     * or transparent.
     */
    public Region() {
        super();
        setPickOnBounds(true);
    }
    
    
    /***************************************************************************
     *                                                                         *
     * Region properties                                                       *
     *                                                                         *
     **************************************************************************/

    /**
     * Defines whether this region adjusts position, spacing, and size values of
     * its children to pixel boundaries. This defaults to true, which is generally
     * the expected behavior in order to have crisp user interfaces. A value of
     * false will allow for fractional alignment, which may lead to "fuzzy"
     * looking borders.
     */
    private BooleanProperty snapToPixel;
    /**
     * I'm using a super-lazy property pattern here, so as to only create the
     * property object when needed for listeners or when being set from CSS,
     * but also making sure that we only call requestParentLayout in the case
     * that the snapToPixel value has actually changed, whether set via the setter
     * or set via the property object.
     */
    private boolean _snapToPixel = true;
    public final boolean isSnapToPixel() { return _snapToPixel; }
    public final void setSnapToPixel(boolean value) {
        if (snapToPixel == null) {
            if (_snapToPixel != value) {
                _snapToPixel = value;
                requestParentLayout();
            }
        } else {
            snapToPixel.set(value);
        }
    }
    public final BooleanProperty snapToPixelProperty() {
        // Note: snapToPixel is virtually never set, and never listened to.
        // Because of this, it works reasonably well as a lazy property,
        // since this logic is just about never going to be called.
        if (snapToPixel == null) {
            snapToPixel = new StyleableBooleanProperty(_snapToPixel) {
                @Override public Object getBean() { return Region.this; }
                @Override public String getName() { return "snapToPixel"; }
                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.SNAP_TO_PIXEL;
                }
                @Override public void invalidated() {
                    boolean value = get();
                    if (_snapToPixel != value) {
                        _snapToPixel = value;
                        requestParentLayout();
                    }
                }
            };
        }
        return snapToPixel;
    }

    /**
     * The top, right, bottom, and left padding around the region's content.
     * This space will be included in the calculation of the region's
     * minimum and preferred sizes. By default padding is Insets.EMPTY. Setting the
     * value to null should be avoided.
     */
    private ObjectProperty<Insets> padding = new StyleableObjectProperty<Insets>(Insets.EMPTY) {
        // Keep track of the last valid value for the sake of
        // rollback in case padding is set to null. Note that
        // Richard really does not like this pattern because
        // it essentially means that binding the padding property
        // is not possible since a binding expression could very
        // easily produce an intermediate null value.

        // Also note that because padding is set virtually everywhere via CSS, and CSS
        // requires a property object in order to set it, there is no benefit to having
        // lazy initialization here.

        private Insets lastValidValue = Insets.EMPTY;

        @Override public Object getBean() { return Region.this; }
        @Override public String getName() { return "padding"; }
        @Override public CssMetaData getCssMetaData() {
            return StyleableProperties.PADDING;
        }
        @Override public void invalidated() {
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
        }
    };
    public final void setPadding(Insets value) { padding.set(value); }
    public final Insets getPadding() { return padding.get(); }
    public final ObjectProperty<Insets> paddingProperty() { return padding; }

    /**
     * The background of the Region, which is made up of zero or more BackgroundFills, and
     * zero or more BackgroundImages. It is possible for a Background to be empty, where it
     * has neither fills nor images, and is semantically equivalent to null.
     */
    private final ObjectProperty<Background> background = new StyleableObjectProperty<Background>(null) {
        private Background old = null;
        @Override public Object getBean() { return Region.this; }
        @Override public String getName() { return "background"; }
        @Override public CssMetaData getCssMetaData() {
            return StyleableProperties.BACKGROUND;
        }

        @Override protected void invalidated() {
            final Background b = get();
            if(old != null ? !old.equals(b) : b != null) {
                // They are different! Both cannot be null
                if (old == null || b == null || !old.getOutsets().equals(b.getOutsets())) {
                    // We have determined that the outsets of these two different background
                    // objects is different, and therefore the bounds have changed.
                    impl_geomChanged();
                    insets.fireValueChanged();
                }
                // No matter what, the fill has changed, so we have to update it
                impl_markDirty(DirtyBits.SHAPE_FILL);
                old = b;
            }
        }
    };
    public final void setBackground(Background value) { background.set(value); }
    public final Background getBackground() { return background.get(); }
    public final ObjectProperty<Background> backgroundProperty() { return background; }

    /**
     * The border of the Region, which is made up of zero or more BorderStrokes, and
     * zero or more BorderImages. It is possible for a Border to be empty, where it
     * has neither strokes nor images, and is semantically equivalent to null.
     */
    private final ObjectProperty<Border> border = new StyleableObjectProperty<Border>(null) {
        private Border old = null;
        @Override public Object getBean() { return Region.this; }
        @Override public String getName() { return "border"; }
        @Override public CssMetaData getCssMetaData() {
            return StyleableProperties.BORDER;
        }
        @Override protected void invalidated() {
            final Border b = get();
            if(old != null ? !old.equals(b) : b != null) {
                // They are different! Both cannot be null
                if (old == null || b == null || !old.getOutsets().equals(b.getOutsets())) {
                    // We have determined that the outsets of these two different border
                    // objects is different, and therefore the bounds have changed.
                    impl_geomChanged();
                    insets.fireValueChanged();
                }
                // No matter what, the fill has changed, so we have to update it
                impl_markDirty(DirtyBits.SHAPE_STROKE);
                old = b;
            }
        }
    };
    public final void setBorder(Border value) { border.set(value); }
    public final Border getBorder() { return border.get(); }
    public final ObjectProperty<Border> borderProperty() { return border; }

    /**
     * Defines the area of the region within which completely opaque pixels
     * are drawn. This is used for various performance optimizations.
     * The pixels within this area MUST BE fully opaque, or rendering
     * artifacts will result. It is the responsibility of the application, either
     * via code or via CSS, to ensure that the opaqueInsets is correct for
     * a Region based on the backgrounds and borders of that region. The values
     * for each of the insets must be real numbers, not NaN or Infinity. If
     * no known insets exist, then the opaqueInsets should be set to null.
     */
    public final ObjectProperty<Insets> opaqueInsetsProperty() {
        if (opaqueInsets == null) {
            opaqueInsets = new StyleableObjectProperty<Insets>() {
                @Override public Object getBean() { return Region.this; }
                @Override public String getName() { return "opaqueInsets"; }
                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.OPAQUE_INSETS;
                }
                @Override protected void invalidated() {
                    // This causes the background to be updated, which
                    // is the code block where we also compute the opaque insets
                    // since updating the background is super fast even when
                    // nothing has changed.
                    impl_markDirty(DirtyBits.SHAPE_FILL);
                }
            };
        }
        return opaqueInsets;
    }
    private ObjectProperty<Insets> opaqueInsets;
    public final void setOpaqueInsets(Insets value) { opaqueInsetsProperty().set(value); }
    public final Insets getOpaqueInsets() { return opaqueInsets == null ? null : opaqueInsets.get(); }

    /**
     * The insets of the Region define the distance from the edge of the region (its layout bounds,
     * or (0, 0, width, height)) to the edge of the content area. All child nodes should be laid out
     * within the content area. The insets are computed based on the Border which has been specified,
     * if any, and also the padding.
     */
    private InsetsProperty insets = new InsetsProperty();
    public final Insets getInsets() { return insets.get(); }
    public final ReadOnlyObjectProperty<Insets> insetsProperty() { return insets; }
    private final class InsetsProperty extends ReadOnlyObjectProperty<Insets> {
        private Insets cache = null;
        private ExpressionHelper<Insets> helper = null;

        @Override public Object getBean() { return Region.this; }
        @Override public String getName() { return "insets"; }

        @Override public void addListener(InvalidationListener listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override public void removeListener(InvalidationListener listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }

        @Override public void addListener(ChangeListener<? super Insets> listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override public void removeListener(ChangeListener<? super Insets> listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }

        void fireValueChanged() {
            cache = null;
            requestLayout();
            ExpressionHelper.fireValueChangedEvent(helper);
        }

        @Override public Insets get() {
            // If a shape is specified, then we don't really care whether there are any borders
            // specified, since borders of shapes do not contribute to the insets.
            if (_shape != null) return getPadding();

            // If there is no border or the border has no insets itself, then the only thing
            // affecting the insets is the padding, so we can just return it directly.
            final Border b = getBorder();
            if (b == null || Insets.EMPTY.equals(b.getInsets())) {
                return getPadding();
            }

            // There is a border with some non-zero insets and we do not have a _shape, so we need
            // to take the border's insets into account
            if (cache == null) {
                // Combine the padding and the border insets.
                // TODO note that negative border insets were being ignored, but
                // I'm not sure that that made sense or was reasonable, so I have
                // changed it so that we just do simple math.
                // TODO Stroke borders should NOT contribute to the insets. Ensure via tests.
                final Insets borderInsets = b.getInsets();
                final Insets paddingInsets = getPadding();
                cache = new Insets(
                        borderInsets.getTop() + paddingInsets.getTop(),
                        borderInsets.getRight() + paddingInsets.getRight(),
                        borderInsets.getBottom() + paddingInsets.getBottom(),
                        borderInsets.getLeft() + paddingInsets.getLeft()
                );
            }
            return cache;
        }
    };

    /**
    * The width of this resizable node.  This property is set by the region's parent
    * during layout and may not be set by the application.  If an application
    * needs to explicitly control the size of a region, it should override its
    * preferred size range by setting the <code>minWidth</code>, <code>prefWidth</code>,
    * and <code>maxWidth</code> properties.
    */
    private ReadOnlyDoubleWrapper width;

    /**
     * Because the width is very often set and very often read but only sometimes
     * listened to, it is beneficial to use the super-lazy pattern property, where we
     * only inflate the property object when widthProperty() is explicitly invoked.
     */
    private double _width;

    // Note that it is OK for this method to be protected so long as the width
    // property is never bound. Only Region could do so because only Region has
    // access to a writable property for "width", but since there is now a protected
    // set method, it is impossible for Region to ever bind this property.
    protected void setWidth(double value) {
        if(width == null) {
            widthChanged(value);
        } else {
            width.set(value);
        }
    }

    private void widthChanged(double value) {
        // It is possible that somebody sets the width of the region to a value which
        // it previously held. If this is the case, we want to avoid excessive layouts.
        // Note that I have biased this for layout over binding, because the widthProperty
        // is now going to recompute the width eagerly. The cost of excessive and
        // unnecessary bounds changes, however, is relatively high.
        if (value != _width) {
            _width = value;
            boundingBox = null;
            impl_layoutBoundsChanged();
            impl_geomChanged();
            impl_markDirty(DirtyBits.NODE_GEOMETRY);
            requestLayout();
        }
    }

    public final double getWidth() { return width == null ? _width : width.get(); }

    public final ReadOnlyDoubleProperty widthProperty() {
        if (width == null) {
            width = new ReadOnlyDoubleWrapper(_width) {
                @Override protected void invalidated() { widthChanged(get()); }
                @Override public Object getBean() { return Region.this; }
                @Override public String getName() { return "width"; }
            };
        }
        return width.getReadOnlyProperty();
    }

    /**
     * The height of this resizable node.  This property is set by the region's parent
     * during layout and may not be set by the application.  If an application
     * needs to explicitly control the size of a region, it should override its
     * preferred size range by setting the <code>minHeight</code>, <code>prefHeight</code>,
     * and <code>maxHeight</code> properties.
     */
    private ReadOnlyDoubleWrapper height;

    /**
     * Because the height is very often set and very often read but only sometimes
     * listened to, it is beneficial to use the super-lazy pattern property, where we
     * only inflate the property object when heightProperty() is explicitly invoked.
     */
    private double _height;

    // Note that it is OK for this method to be protected so long as the height
    // property is never bound. Only Region could do so because only Region has
    // access to a writable property for "height", but since there is now a protected
    // set method, it is impossible for Region to ever bind this property.
    protected void setHeight(double value) {
        if (height == null) {
            heightChanged(value);
        } else {
            height.set(value);
        }
    }

    private void heightChanged(double value) {
        if (_height != value) {
            _height = value;
            // It is possible that somebody sets the height of the region to a value which
            // it previously held. If this is the case, we want to avoid excessive layouts.
            // Note that I have biased this for layout over binding, because the heightProperty
            // is now going to recompute the height eagerly. The cost of excessive and
            // unnecessary bounds changes, however, is relatively high.
            boundingBox = null;
            // Note: although impl_geomChanged will usually also invalidate the
            // layout bounds, that is not the case for Regions, and both must
            // be called separately.
            impl_geomChanged();
            impl_layoutBoundsChanged();
            // We use "NODE_GEOMETRY" to mean that the bounds have changed and
            // need to be sync'd with the render tree
            impl_markDirty(DirtyBits.NODE_GEOMETRY);
            // TODO why do we do this? If the height can only be changed during
            // layout, and if calls to requestLayout are ignored during layout,
            // then why do we call requestLayout? It does protect against the case
            // that a developer called resize() or whatnot outside of layout, in
            // which case on the next pulse we'll "correct" the size according
            // to layout. But I am not sure this case, which produces a visual "bug"
            // anyway, is worth the cost? The same would go for the widthChanged.
            requestLayout();
        }
    }

    public final double getHeight() { return height == null ? _height : height.get(); }

    public final ReadOnlyDoubleProperty heightProperty() {
        if (height == null) {
            height = new ReadOnlyDoubleWrapper(_height) {
                @Override protected void invalidated() { heightChanged(get()); }
                @Override public Object getBean() { return Region.this; }
                @Override public String getName() { return "height"; }
            };
        }
        return height.getReadOnlyProperty();
    }

    private void requestParentLayout() {
        Parent parent = getParent();
        if (parent != null) {
            parent.requestLayout();
        }
    }

    /**
     * This class is reused for the min, pref, and max properties since
     * they all performed the same function (to call requestParentLayout).
     */
    private final class MinPrefMaxProperty extends DoublePropertyBase {
        private String name;

        MinPrefMaxProperty(String name, double initialValue) {
            super(initialValue);
            this.name = name;
        }

        @Override public void invalidated() { requestParentLayout(); }
        @Override public Object getBean() { return Region.this; }
        @Override public String getName() { return name; }
    }

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
     */
    private DoubleProperty minWidth;
    private double _minWidth = USE_COMPUTED_SIZE;
    public final void setMinWidth(double value) {
        if (minWidth == null) {
            _minWidth = value;
            requestParentLayout();
        } else {
            minWidth.set(value);
        }
    }
    public final double getMinWidth() { return minWidth == null ? _minWidth : minWidth.get(); }
    public final DoubleProperty minWidthProperty() {
        if (minWidth == null) minWidth = new MinPrefMaxProperty("minWidth", _minWidth);
        return minWidth;
    }

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
    private DoubleProperty minHeight;
    private double _minHeight = USE_COMPUTED_SIZE;
    public final void setMinHeight(double value) {
        if (minHeight == null) {
            _minHeight = value;
            requestParentLayout();
        } else {
            minHeight.set(value);
        }
    }
    public final double getMinHeight() { return minHeight == null ? _minHeight : minHeight.get(); }
    public final DoubleProperty minHeightProperty() {
        if (minHeight == null) minHeight = new MinPrefMaxProperty("minHeight", _minHeight);
        return minHeight;
    }

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
     */
    private DoubleProperty prefWidth;
    private double _prefWidth = USE_COMPUTED_SIZE;
    public final void setPrefWidth(double value) {
        if (prefWidth == null) {
            _prefWidth = value;
            requestParentLayout();
        } else {
            prefWidth.set(value);
        }
    }
    public final double getPrefWidth() { return prefWidth == null ? _prefWidth : prefWidth.get(); }
    public final DoubleProperty prefWidthProperty() {
        if (prefWidth == null) prefWidth = new MinPrefMaxProperty("prefWidth", _prefWidth);
        return prefWidth;
    }

    /**
     * Property for overriding the region's computed preferred height.
     * This should only be set if the region's internally computed preferred height
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getPrefHeight(forWidth)</code> will return the region's internally
     * computed preferred width.
     */
    private DoubleProperty prefHeight;
    private double _prefHeight = USE_COMPUTED_SIZE;
    public final void setPrefHeight(double value) {
        if (prefHeight == null) {
            _prefHeight = value;
            requestParentLayout();
        } else {
            prefHeight.set(value);
        }
    }
    public final double getPrefHeight() { return prefHeight == null ? _prefHeight : prefHeight.get(); }
    public final DoubleProperty prefHeightProperty() {
        if (prefHeight == null) prefHeight = new MinPrefMaxProperty("prefHeight", _prefHeight);
        return prefHeight;
    }

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
     */
    private DoubleProperty maxWidth;
    private double _maxWidth = USE_COMPUTED_SIZE;
    public final void setMaxWidth(double value) {
        if (maxWidth == null) {
            _maxWidth = value;
            requestParentLayout();
        } else {
            maxWidth.set(value);
        }
    }
    public final double getMaxWidth() { return maxWidth == null ? _maxWidth : maxWidth.get(); }
    public final DoubleProperty maxWidthProperty() {
        if (maxWidth == null) maxWidth = new MinPrefMaxProperty("maxWidth", _maxWidth);
        return maxWidth;
    }

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
     */
    private DoubleProperty maxHeight;
    private double _maxHeight = USE_COMPUTED_SIZE;
    public final void setMaxHeight(double value) {
        if (maxHeight == null) {
            _maxHeight = value;
            requestParentLayout();
        } else {
            maxHeight.set(value);
        }
    }
    public final double getMaxHeight() { return maxHeight == null ? _maxHeight : maxHeight.get(); }
    public final DoubleProperty maxHeightProperty() {
        if (maxHeight == null) maxHeight = new MinPrefMaxProperty("maxHeight", _maxHeight);
        return maxHeight;
    }

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
     * When specified, the {@code shape} will cause the region to be
     * rendered as the specified shape rather than as a rounded rectangle.
     * When null, the Region is rendered as a rounded rectangle. When rendered
     * as a Shape, any Background is used to fill the shape, although any
     * background insets are ignored as are background radii. Any BorderStrokes
     * defined are used for stroking the shape. Any BorderImages are ignored.
     *
     * @default null
     * @css shape SVG shape string
     */
    private ObjectProperty<Shape> shape = null;
    private Shape _shape;
    public final Shape getShape() { return shape == null ? _shape : shape.get(); }
    public final void setShape(Shape value) { shapeProperty().set(value); }
    public final ObjectProperty<Shape> shapeProperty() {
        if (shape == null) {
            shape = new ShapeProperty();
        }
        return shape;
    }

    /**
     * An implementation for the ShapeProperty. This is also a ShapeChangeListener.
     */
    private final class ShapeProperty extends StyleableObjectProperty<Shape> implements Runnable {
        @Override public Object getBean() { return Region.this; }
        @Override public String getName() { return "shape"; }
        @Override public CssMetaData getCssMetaData() {
            return StyleableProperties.SHAPE;
        }
        @Override protected void invalidated() {
            final Shape value = get();
            if (_shape != value) {
                // The shape has changed. We need to add/remove listeners
                if (_shape != null) _shape.impl_setShapeChangeListener(null);
                if (value != null) value.impl_setShapeChangeListener(this);
                // Invalidate the bounds and such
                run();
                if (_shape == null || value == null) {
                    // It either was null before, or is null now. In either case,
                    // the result of the insets computation will have changed, and
                    // we therefore need to fire that the insets value may have changed.
                    insets.fireValueChanged();
                }
                // Update our reference to the old shape
                _shape = value;
            }
        }

        @Override public void run() {
            impl_geomChanged();
            requestLayout();
            impl_markDirty(DirtyBits.REGION_SHAPE);
        }
    };

    /**
     * Specifies whether the shape, if defined, is scaled to match the size of the Region.
     * {@code true} means the shape is scaled to fit the size of the Region, {@code false}
     * means the shape is at its source size, its positioning depends on the value of
     * {@code centerShape}.
     *
     * @default true
     * @css shape-size      true | false
     */
    private BooleanProperty scaleShape = null;
    public final void setScaleShape(boolean value) { scaleShapeProperty().set(value); }
    public final boolean isScaleShape() { return scaleShape == null ? true : scaleShape.get(); }
    private BooleanProperty scaleShapeProperty() {
        if (scaleShape == null) {
            scaleShape = new StyleableBooleanProperty(true) {
                @Override public Object getBean() { return Region.this; }
                @Override public String getName() { return "scaleShape"; }
                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.SCALE_SHAPE;
                }
                @Override public void invalidated() {
                    // TODO should be requestParentLayout?
                    requestLayout();
                    impl_markDirty(DirtyBits.REGION_SHAPE);
                }
            };
        }
        return scaleShape;
    }

    /**
     * Defines whether the shape is centered within the Region's width or height.
     * {@code true} means the shape centered within the Region's width and height,
     * {@code false} means the shape is positioned at its source position.
     *
     * @default true
     * @css position-shape      true | false
     */
    private BooleanProperty centerShape = null;
    public final void setCenterShape(boolean value) { positionShapeProperty().set(value); }
    public final boolean isCenterShape() { return centerShape == null ? true : centerShape.get(); }
    private BooleanProperty positionShapeProperty() {
        if (centerShape == null) {
            centerShape = new StyleableBooleanProperty(true) {
                @Override public Object getBean() { return Region.this; }
                @Override public String getName() { return "centerShape"; }
                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.POSITION_SHAPE;
                }
                @Override public void invalidated() {
                    // TODO should be requestParentLayout?
                    requestLayout();
                    impl_markDirty(DirtyBits.REGION_SHAPE);
                }
            };
        }
        return centerShape;
    }

    /**
     * Defines a hint to the system indicating that the Shape used to define the region's
     * background is stable and would benefit from caching.
     *
     * @default true
     * @css -fx-cache-shape      true | false
     */
    private BooleanProperty cacheShape = null;
    public final void setCacheShape(boolean value) { cacheShapeProperty().set(value); }
    public final boolean isCacheShape() { return cacheShape == null ? true : cacheShape.get(); }
    private BooleanProperty cacheShapeProperty() {
        if (cacheShape == null) {
            cacheShape = new StyleableBooleanProperty(true) {
                @Override public Object getBean() { return Region.this; }
                @Override public String getName() { return "cacheShape"; }
                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.CACHE_SHAPE;
                }
            };
        }
        return cacheShape;
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
            logger.finer(this.toString() + " resized to " + width + " x " + height);
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
    @Override protected double computeMinWidth(double height) {
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
    @Override protected double computeMinHeight(double width) {
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
        final double w = super.computePrefWidth(height);
        return getInsets().getLeft() + w + getInsets().getRight();
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
        final double h = super.computePrefHeight(width);
        return getInsets().getTop() + h + getInsets().getBottom();
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
        final boolean snap = isSnapToPixel();
        double left = margin != null? snapSpace(margin.getLeft(), snap) : 0;
        double right = margin != null? snapSpace(margin.getRight(), snap) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            alt = snapSize(height != -1? boundedSize(child.minHeight(-1), height, child.maxHeight(-1)) :
                                         child.minHeight(-1));
        }
        return left + snapSize(child.minWidth(alt)) + right;
    }

    double computeChildMinAreaHeight(Node child, Insets margin) {
        return computeChildMinAreaHeight(child, margin, -1);
    }

    double computeChildMinAreaHeight(Node child, Insets margin, double width) {
        final boolean snap = isSnapToPixel();
        double top = margin != null? snapSpace(margin.getTop(), snap) : 0;
        double bottom = margin != null? snapSpace(margin.getBottom(), snap) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            alt = snapSize(width != -1? boundedSize(child.minWidth(-1), width, child.maxWidth(-1)) :
                                        child.minWidth(-1));
        }
        return top + snapSize(child.minHeight(alt)) + bottom;
    }

    double computeChildPrefAreaWidth(Node child, Insets margin) {
        return computeChildPrefAreaWidth(child, margin, -1);
    }

    double computeChildPrefAreaWidth(Node child, Insets margin, double height) {
        final boolean snap = isSnapToPixel();
        double top = margin != null? snapSpace(margin.getTop(), snap) : 0;
        double bottom = margin != null? snapSpace(margin.getBottom(), snap) : 0;
        double left = margin != null? snapSpace(margin.getLeft(), snap) : 0;
        double right = margin != null? snapSpace(margin.getRight(), snap) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            alt = snapSize(boundedSize(
                    child.minHeight(-1), height != -1? height - top - bottom :
                           child.prefHeight(-1), child.maxHeight(-1)));
        }        
        return left + snapSize(boundedSize(child.minWidth(alt), child.prefWidth(alt), child.maxWidth(alt))) + right;
    }

    double computeChildPrefAreaHeight(Node child, Insets margin) {
        return computeChildPrefAreaHeight(child, margin, -1);
    }

    double computeChildPrefAreaHeight(Node child, Insets margin, double width) {
        final boolean snap = isSnapToPixel();
        double top = margin != null? snapSpace(margin.getTop(), snap) : 0;
        double bottom = margin != null? snapSpace(margin.getBottom(), snap) : 0;
        double left = margin != null? snapSpace(margin.getLeft(), snap) : 0;
        double right = margin != null? snapSpace(margin.getRight(), snap) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            alt = snapSize(boundedSize(
                    child.minWidth(-1), width != -1? width - left - right :
                           child.prefWidth(-1), child.maxWidth(-1)));
        }        
        return top + snapSize(boundedSize(child.minHeight(alt), child.prefHeight(alt), child.maxHeight(alt))) + bottom;
    }

    double computeChildMaxAreaWidth(Node child, Insets margin, double height) {
        double max = child.maxWidth(-1);
        if (max == Double.MAX_VALUE) {
            return max;
        }
        final boolean snap = isSnapToPixel();
        double left = margin != null? snapSpace(margin.getLeft(), snap) : 0;
        double right = margin != null? snapSpace(margin.getRight(), snap) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            alt = snapSize(height != -1? boundedSize(child.minHeight(-1), height, child.maxHeight(-1)) :
                child.maxHeight(-1));
            max = child.maxWidth(alt);
        }
        // if min > max, min wins, so still need to call boundedSize()
        return left + snapSize(boundedSize(child.minWidth(alt), max, child.maxWidth(alt))) + right;
    }

    double computeChildMaxAreaHeight(Node child, Insets margin, double width) {
        double max = child.maxHeight(-1);
        if (max == Double.MAX_VALUE) {
            return max;
        }

        final boolean snap = isSnapToPixel();
        double top = margin != null? snapSpace(margin.getTop(), snap) : 0;
        double bottom = margin != null? snapSpace(margin.getBottom(), snap) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            alt = snapSize(width != -1? boundedSize(child.minWidth(-1), width, child.maxWidth(-1)) :
                child.maxWidth(-1));
            max = child.maxHeight(alt);
        }
        // if min > max, min wins, so still need to call boundedSize()
        return top + snapSize(boundedSize(child.minHeight(alt), max, child.maxHeight(alt))) + bottom;
    }

    /* Max of children's minimum area widths */

    double computeMaxMinAreaWidth(List<Node> children, Insets margins[], HPos halignment /* ignored for now */) {
        return getMaxAreaWidth(children, margins, new double[] { -1 }, true);
    }

    double computeMaxMinAreaWidth(List<Node> children, Insets margins[], HPos halignment /* ignored for now */, double height) {
        return getMaxAreaWidth(children, margins, new double[] { height }, true);
    }

    double computeMaxMinAreaWidth(List<Node> children, Insets childMargins[], double childHeights[], HPos halignment /* ignored for now */) {
        return getMaxAreaWidth(children, childMargins, childHeights, true);
    }

    /* Max of children's minimum area heights */

    double computeMaxMinAreaHeight(List<Node>children, Insets margins[], VPos valignment) {
        return getMaxAreaHeight(children, margins, new double[] { -1 }, valignment, true);
    }

    double computeMaxMinAreaHeight(List<Node>children, Insets margins[], VPos valignment, double width) {
        return getMaxAreaHeight(children, margins, new double[] { width }, valignment, true);
    }

    double computeMaxMinAreaHeight(List<Node>children, Insets childMargins[], double childWidths[], VPos valignment) {
        return getMaxAreaHeight(children, childMargins, childWidths, valignment, true);
    }

    /* Max of children's pref area widths */

    double computeMaxPrefAreaWidth(List<Node>children, Insets margins[], HPos halignment /* ignored for now */) {        
        return getMaxAreaWidth(children, margins, new double[] { -1 }, false);
    }

    double computeMaxPrefAreaWidth(List<Node>children, Insets margins[], double height, HPos halignment /* ignored for now */) {
        return getMaxAreaWidth(children, margins, new double[] { height }, false);
    }

    double computeMaxPrefAreaWidth(List<Node>children, Insets childMargins[], double childHeights[], HPos halignment /* ignored for now */) {
        return getMaxAreaWidth(children, childMargins, childHeights, false);
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
        final double lastChildWidth = childWidths.length > 0 ? childWidths[childWidths.length - 1] : 0;
        if (valignment == VPos.BASELINE) {
            double maxAbove = 0;
            double maxBelow = 0;
            for (int i = 0, maxPos = children.size(); i < maxPos; i++) {
                final Node child = children.get(i);
                final double baseline = child.getBaselineOffset();
                final double top = childMargins[i] != null? snapSpace(childMargins[i].getTop()) : 0;
                final double bottom = childMargins[i] != null? snapSpace(childMargins[i].getBottom()) : 0;
                final double childWidth = i < childWidths.length ? childWidths[i] : lastChildWidth;
                maxAbove = Math.max(maxAbove, baseline + top);
                maxBelow = Math.max(maxBelow,
                        snapSpace(minimum?snapSize(child.minHeight(childWidth)) : snapSize(child.prefHeight(childWidth))) -
                        baseline + bottom);
            }
            return maxAbove + maxBelow; //remind(aim): ceil this value?
        } else {
            double max = 0;
            for (int i = 0, maxPos = children.size(); i < maxPos; i++) {
                final Node child = children.get(i);
                final double childWidth = i < childWidths.length ? childWidths[i] : lastChildWidth;
                max = Math.max(max, minimum?
                    computeChildMinAreaHeight(child, childMargins[i], childWidth) :
                        computeChildPrefAreaHeight(child, childMargins[i], childWidth));
            }
            return max;
        }
    }

    /* utility method for computing the max of children's min or pref width, horizontal alignment is ignored for now */
    private double getMaxAreaWidth(List<javafx.scene.Node> children, Insets childMargins[], double childHeights[], boolean minimum) {
        final double lastChildHeight = childHeights.length > 0 ? childHeights[childHeights.length - 1] : 0;
        double max = 0;
        for (int i = 0, maxPos = children.size(); i < maxPos; i++) {
            final Node child = children.get(i);
            final double childHeight = i < childHeights.length ? childHeights[i] : lastChildHeight;
            max = Math.max(max, minimum?
                computeChildMinAreaWidth(children.get(i), childMargins[i], childHeight) :
                    computeChildPrefAreaWidth(child, childMargins[i], childHeight));
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
                childWidth = boundedSize(
                        child.minWidth(-1), fillWidth? innerAreaWidth :
                                         Math.min(innerAreaWidth,child.prefWidth(-1)),
                        child.maxWidth(-1));
                childHeight = boundedSize(
                        child.minHeight(-1), fillHeight? innerAreaHeight :
                                         Math.min(innerAreaHeight,child.prefHeight(-1)),
                        child.maxHeight(-1));

            } else if (bias == Orientation.HORIZONTAL) {
                childWidth = boundedSize(
                        child.minWidth(-1), fillWidth? innerAreaWidth :
                                         Math.min(innerAreaWidth,child.prefWidth(-1)),
                        child.maxWidth(-1));
                childHeight = boundedSize(
                        child.minHeight(childWidth), fillHeight? innerAreaHeight :
                                         Math.min(innerAreaHeight,child.prefHeight(childWidth)),
                        child.maxHeight(childWidth));

            } else { // bias == VERTICAL
                childHeight = boundedSize(
                        child.minHeight(-1), fillHeight? innerAreaHeight :
                                         Math.min(innerAreaHeight,child.prefHeight(-1)),
                        child.maxHeight(-1));
                childWidth = boundedSize(
                        child.minWidth(childHeight), fillWidth? innerAreaWidth :
                                         Math.min(innerAreaWidth,child.prefWidth(childHeight)),
                        child.maxWidth(childHeight));
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
        if (_shape != null) _shape.impl_updatePG();
        PGRegion pg = (PGRegion) impl_getPGNode();

        final boolean sizeChanged = impl_isDirty(DirtyBits.NODE_GEOMETRY);
        if (sizeChanged) {
            pg.setSize((float)getWidth(), (float)getHeight());
        }

        // NOTE: The order here is very important. There is logic in NGRegion which determines
        // whether we can cache an image representing this region, and for this to work correctly,
        // the shape must be specified before the background which is before the border.
        final boolean shapeChanged = impl_isDirty(DirtyBits.REGION_SHAPE);
        if (shapeChanged) {
            pg.updateShape(_shape, isScaleShape(), isCenterShape(), isCacheShape());
        }

        final boolean backgroundChanged = impl_isDirty(DirtyBits.SHAPE_FILL);
        final Background bg = getBackground();
        if (backgroundChanged) {
            pg.updateBackground(bg);
        }

        if (impl_isDirty(DirtyBits.SHAPE_STROKE)) {
            pg.updateBorder(getBorder());
        }

        if (sizeChanged || backgroundChanged || shapeChanged) {
            // TODO Make sure there is a test ensuring that stroke borders do not contribute to insets or opaque insets
            // If the background is determined by a shape, then we don't care (for now) what the opaque insets
            // of the Background are. If the developer specified opaque insets, we will use them, otherwise
            // we will make sure the opaque insets are cleared
            final Insets i = getOpaqueInsets();
            if (_shape != null) {
                if (i != null) {
                    pg.setOpaqueInsets((float) i.getTop(), (float) i.getRight(),
                                       (float) i.getBottom(), (float) i.getLeft());
                } else {
                    pg.setOpaqueInsets(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
                }
            } else if (bg == null || bg.isEmpty() || !bg.hasOpaqueFill) {
                // If the background is null or empty or has no opaque fills, then we will forbear
                // computing the opaque insets, and just send null down.
                pg.setOpaqueInsets(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
            } else if (i == null) {
                // There are no opaqueInsets specified by the developer (the most common case), so
                // I will have to compute them.
                // TODO If I could determine whether an Image were opaque, I could also compute
                // the opaqueInsets for a BackgroundImage and BorderImage.
                final double[] trbl = new double[4];
                bg.computeOpaqueInsets(getWidth(), getHeight(), trbl);
                pg.setOpaqueInsets((float) trbl[0], (float) trbl[1], (float) trbl[2], (float) trbl[3]);
            } else {
                // TODO For now, I'm just going to honor the opaqueInsets. Really I would want
                // to check to see if the computed opaqueTop, right, etc on the Background is
                // broader than the supplied opaque insets and adjust accordingly, but for now
                // I won't bother.
                pg.setOpaqueInsets((float) i.getTop(), (float) i.getRight(),
                                   (float) i.getBottom(), (float) i.getLeft());
            }
        }
    }

    /** @treatAsPrivate */
    @Override public PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGRegion();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_computeContains(double localX, double localY) {
        // NOTE: This method only gets called if a quick check of bounds has already
        // occurred, so there is no need to test against bound again. We know that the
        // point (localX, localY) falls within the bounds of this node, now we need
        // to determine if it falls within the geometry of this node.
        // Also note that because Region defaults pickOnBounds to true, this code is
        // not usually executed. It will only be executed if pickOnBounds is set to false.

        final double x2 = getWidth();
        final double y2 = getHeight();
        // Figure out what the maximum possible radius value is.
        final double maxRadius = Math.min(x2 / 2.0, y2 / 2.0);

        // First check the shape. Shape could be impacted by scaleShape & positionShape properties.
        // This is going to be ugly! The problem is that basically all the scale / position operations
        // have to be implemented here in Region, whereas right now they are all implemented in
        // NGRegion. Drat. Basically I can't implement this properly until I have a way to get the
        // geometry backing an arbitrary FX shape. For example, in this case I need an NGShape peer
        // of this shape so that I can resize it as appropriate for these picking tests.
        // Lacking that, for now, I will simply check the shape (so that picking works for pie charts)
        // Bug is filed as RT-27775.
        if (_shape != null) {
            return _shape.contains(localX, localY);
        }

        // OK, there was no background shape, so I'm going to work on the principle of
        // nested rounded rectangles. We'll start by checking the backgrounds. The
        // first background which passes the test is good enough for us!
        final Background background = getBackground();
        if (background != null) {
            final List<BackgroundFill> fills = background.getFills();
            for (int i = 0, max = fills.size(); i < max; i++) {
                final BackgroundFill bgFill = fills.get(i);
                if (contains(localX, localY, 0, 0, x2, y2, bgFill.getInsets(), bgFill.getRadii(), maxRadius)) {
                    return true;
                }
            }
        }

        // If we are here then either there were no background fills or there were no background
        // fills which contained the point, and the region is not defined by a shape.
        final Border border = getBorder();
        if (border != null) {
            // Check all the stroke borders first. If the pick occurs on any stroke border
            // then we consider the contains test to have passed. Semantically we will treat a Region
            // with a border as if it were a rectangle with a stroke but no fill.
            final List<BorderStroke> strokes = border.getStrokes();
            for (int i=0, max=strokes.size(); i<max; i++) {
                final BorderStroke strokeBorder = strokes.get(i);
                if (contains(localX, localY, 0, 0, x2, y2, strokeBorder.getWidths(), false, strokeBorder.getInsets(),
                             strokeBorder.getRadii(), maxRadius)) {
                    return true;
                }
            }

            // Check the image borders. We treat the image border as though it is opaque.
            final List<BorderImage> images = border.getImages();
            for (int i = 0, max = images.size(); i < max; i++) {
                final BorderImage borderImage = images.get(i);
                if (contains(localX, localY, 0, 0, x2, y2, borderImage.getWidths(), borderImage.isFilled(),
                             borderImage.getInsets(), CornerRadii.EMPTY, maxRadius)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Basically we will perform two contains tests. For a point to be on the stroke, it must
     * be within the outermost edge of the stroke, but outside the innermost edge of the stroke.
     * Unless it is filled, in which case it is really just a normal contains test.
     *
     * @param px        The x position of the point to test
     * @param py        The y position of the point to test
     * @param x1        The x1 position of the bounds to test
     * @param y1        The y1 position of the bounds to test
     * @param x2        The x2 position of the bounds to test
     * @param y2        The y2 position of the bounds to test
     * @param widths    The widths of the stroke on each side
     * @param filled    Whether the area is filled or is just stroked
     * @param insets    The insets to apply to (x1,y1)-(x2,y2) to get the final bounds to test
     * @param rad       The corner radii to test with. Must not be null.
     * @param maxRadius The maximum possible radius value
     * @return True if (px, py) is within the stroke, taking into account insets and corner radii.
     */
    private boolean contains(final double px, final double py,
                             final double x1, final double y1, final double x2, final double y2,
                             BorderWidths widths, boolean filled,
                             final Insets insets, final CornerRadii rad, final double maxRadius) {
        if (filled) {
            if (contains(px, py, x1, y1, x2, y2, insets, rad, maxRadius)) {
                return true;
            }
        } else {
            // TODO need to deal with percentage based widths
            boolean insideOuterEdge = contains(px, py, x1, y1, x2, y2, insets, rad, maxRadius);

            if (insideOuterEdge) {
                boolean outsideInnerEdge = !contains(px, py,
                    x1 + widths.getLeft(),
                    y1 + widths.getTop(),
                    x2 - widths.getRight(),
                    y2 - widths.getBottom(),
                    insets, rad, maxRadius);
                if (outsideInnerEdge) return true;
            }
        }
        return false;
    }

    /**
     * Determines whether the point (px, py) is contained within the the bounds (x1, y1)-(x2, y2),
     * after taking into account the insets and the corner radii.
     *
     * @param px        The x position of the point to test
     * @param py        The y position of the point to test
     * @param x1        The x1 position of the bounds to test
     * @param y1        The y1 position of the bounds to test
     * @param x2        The x2 position of the bounds to test
     * @param y2        The y2 position of the bounds to test
     * @param insets    The insets to apply to (x1,y1)-(x2,y2) to get the final bounds to test
     * @param rad       The corner radii to test with. Must not be null.
     * @param maxRadius The maximum possible radius value
     * @return True if (px, py) is within the bounds, taking into account insets and corner radii.
     */
    private boolean contains(final double px, final double py,
                             final double x1, final double y1, final double x2, final double y2,
                             final Insets insets, final CornerRadii rad, final double maxRadius) {
        // These four values are the x0, y0, x1, y1 bounding box after
        // having taken into account the insets of this particular
        // background fill.
        final double rrx0 = x1 + insets.getLeft();
        final double rry0 = y1 + insets.getTop();
        final double rrx1 = x2 - insets.getRight();
        final double rry1 = y2 - insets.getBottom();

        // Check for trivial rejection - point is inside bounding rectangle
        if (px >= rrx0 && py >= rry0 && px <= rrx1 && py <= rry1) {
            // The point was within the index bounding box. Now we need to analyze the
            // corner radii to see if the point lies within the corners or not. If the
            // point is within a corner then we reject this one.
            final double tlhr = Math.min(rad.getTopLeftHorizontalRadius(), maxRadius);
            if (rad.isUniform() && tlhr == 0) {
                // This is a simple square! Since we know the point is already within
                // the insets of this fill, we can simply return true.
                return true;
            } else {
                final double tlvr = Math.min(rad.getTopLeftVerticalRadius(), maxRadius);
                final double trhr = Math.min(rad.getTopRightHorizontalRadius(), maxRadius);
                final double trvr = Math.min(rad.getTopRightVerticalRadius(), maxRadius);
                final double blhr = Math.min(rad.getBottomLeftHorizontalRadius(), maxRadius);
                final double blvr = Math.min(rad.getBottomLeftVerticalRadius(), maxRadius);
                final double brhr = Math.min(rad.getBottomRightHorizontalRadius(), maxRadius);
                final double brvr = Math.min(rad.getBottomRightVerticalRadius(), maxRadius);

                // The four corners can each be described as a quarter of an ellipse
                double centerX, centerY, a, b;

                if (px <= rrx0 + tlhr && py <= rry0 + tlvr) {
                    // Point is in the top left corner
                    centerX = rrx0 + tlhr;
                    centerY = rry0 + tlvr;
                    a = tlhr;
                    b = tlvr;
                } else if (px >= rrx1 - trhr && py <= rry0 + trvr) {
                    // Point is in the top right corner
                    centerX = rrx1 - trhr;
                    centerY = rry0 + trvr;
                    a = trhr;
                    b = trvr;
                } else if (px >= rrx1 - brhr && py >= rry1 - brvr) {
                    // Point is in the bottom right corner
                    centerX = rrx1 - brhr;
                    centerY = rry1 - brvr;
                    a = brhr;
                    b = brvr;
                } else if (px <= rrx0 + blhr && py >= rry1 - blvr) {
                    // Point is in the bottom left corner
                    centerX = rrx0 + blhr;
                    centerY = rry1 - blvr;
                    a = blhr;
                    b = blvr;
                } else {
                    // The point must have been in the solid body someplace
                    return true;
                }

                double x = px - centerX;
                double y = py - centerY;
                double result = ((x*x)/(a*a) + (y*y)/(b*b));
                // The .0000001 is fudge to help in cases where double arithmetic isn't quite right
                if (result - .0000001 <= 1) return true;
            }
        }
        return false;
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
            for (int i = children.size() - 1; i >= 0; i--) {
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

            for (int i = children.size() - 1; i >= 0; i--) {
                Node picked = children.get(i).impl_pickNode(pickRay);

                if (picked != null) {
                    return picked;
                }
            }

            return this;
        }

        return null;
    }

    private Bounds boundingBox;
    
    /**
     * The layout bounds of this region: {@code 0, 0  width x height}
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected final Bounds impl_computeLayoutBounds() {
        if (boundingBox == null) {
            // we reuse the bounding box if the width and height haven't changed.
            boundingBox = new BoundingBox(0, 0, 0, getWidth(), getHeight(), 0);
        }
        return boundingBox;
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
        // Unlike Group, a Region has its own intrinsic geometric bounds, even if it has no children.
        // The bounds of the Region must take into account any backgrounds and borders and how
        // they are used to draw the Region. The geom bounds must always take into account
        // all pixels drawn (because the geom bounds forms the basis of the dirty regions).
        // Note that the layout bounds of a Region is not based on the geom bounds.

        // Define some variables to hold the top-left and bottom-right corners of the bounds
        double bx1 = 0;
        double by1 = 0;
        double bx2 = getWidth();
        double by2 = getHeight();

        // If the shape is defined, then the top-left and bottom-right corner positions
        // need to be redefined
        if (_shape != null && isScaleShape() == false) {
            final Bounds layoutBounds = _shape.getLayoutBounds();
            final double shapeWidth = layoutBounds.getWidth();
            final double shapeHeight = layoutBounds.getHeight();
            if (isCenterShape()) {
                bx1 = (bx2 - shapeWidth) / 2;
                by1 = (by2 - shapeHeight) / 2;
                bx2 = bx1 + shapeWidth;
                by2 = by1 + shapeHeight;
            } else {
                bx1 = layoutBounds.getMinX();
                by1 = layoutBounds.getMinY();
                bx2 = layoutBounds.getMaxX();
                by2 = layoutBounds.getMaxY();
            }
        }

        // Expand the bounds to include the outsets from the background and border.
        // The outsets are the opposite of insets -- a measure of distance from the
        // edge of the Region outward. The outsets cannot, however, be negative.
        final Background background = getBackground();
        final Border border = getBorder();
        final Insets backgroundOutsets = background == null ? Insets.EMPTY : background.getOutsets();
        final Insets borderOutsets = border == null ? Insets.EMPTY : border.getOutsets();
        bx1 -= Math.max(backgroundOutsets.getLeft(), borderOutsets.getLeft());
        by1 -= Math.max(backgroundOutsets.getTop(), borderOutsets.getTop());
        bx2 += Math.max(backgroundOutsets.getRight(), borderOutsets.getRight());
        by2 += Math.max(backgroundOutsets.getBottom(), borderOutsets.getBottom());

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
            // There are no children bounds, so
            bounds = bounds.deriveWithNewBounds(
                    (float)bx1, (float)by1, 0.0f,
                    (float)bx2, (float)by2, 0.0f);
            bounds = tx.transform(bounds, bounds);
            return bounds;
        } else {
            // Union with children's bounds
            BaseBounds tempBounds = TempState.getInstance().bounds;
            tempBounds = tempBounds.deriveWithNewBounds(
                    (float)bx1, (float)by1, 0.0f,
                    (float)bx2, (float)by2, 0.0f);
            BaseBounds bb = tx.transform(tempBounds, tempBounds);
            cb = cb.deriveWithUnion(bb);
            return cb;
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
         private static final CssMetaData<Region,Insets> PADDING =
             new CssMetaData<Region,Insets>("-fx-padding",
                 InsetsConverter.getInstance(), Insets.EMPTY) {

            @Override public boolean isSettable(Region node) {
                return node.padding == null || !node.padding.isBound();
            }

            @Override public StyleableProperty<Insets> getStyleableProperty(Region node) {
                return (StyleableProperty)node.paddingProperty();
            }
         };

         private static final CssMetaData<Region,Insets> OPAQUE_INSETS =
                 new CssMetaData<Region,Insets>("-fx-opaque-insets",
                         InsetsConverter.getInstance(), null) {

                     @Override
                     public boolean isSettable(Region node) {
                         return node.opaqueInsets == null || !node.opaqueInsets.isBound();
                     }

                     @Override
                     public StyleableProperty<Insets> getStyleableProperty(Region node) {
                         return (StyleableProperty)node.opaqueInsetsProperty();
                     }

                 };

         private static final CssMetaData<Region,Background> BACKGROUND =
             new CssMetaData<Region,Background>("-fx-region-background",
                 BackgroundConverter.INSTANCE,
                 null,
                 false,
                 Background.getClassCssMetaData()) {

            @Override public boolean isSettable(Region node) {
                return !node.background.isBound();
            }

            @Override public StyleableProperty<Background> getStyleableProperty(Region node) {
                return (StyleableProperty)node.background;
            }
         };

         private static final CssMetaData<Region,Border> BORDER =
             new CssMetaData<Region,Border>("-fx-region-border",
                     BorderConverter.getInstance(),
                     null,
                     false,
                     Border.getClassCssMetaData()) {

                 @Override public boolean isSettable(Region node) {
                     return !node.background.isBound();
                 }

                 @Override public StyleableProperty<Border> getStyleableProperty(Region node) {
                     return (StyleableProperty)node.border;
                 }
             };

         private static final CssMetaData<Region,Shape> SHAPE =
             new CssMetaData<Region,Shape>("-fx-shape",
                 ShapeConverter.getInstance()) {

            @Override public boolean isSettable(Region node) {
                // isSettable depends on node.shape, not node.shapeContent
                return node.shape == null || !node.shape.isBound();
            }

            @Override public StyleableProperty<Shape> getStyleableProperty(Region node) {
                return (StyleableProperty)node.shapeProperty();
            }
         };

         private static final CssMetaData<Region, Boolean> SCALE_SHAPE = 
             new CssMetaData<Region,Boolean>("-fx-scale-shape",
                 BooleanConverter.getInstance(), Boolean.TRUE){

            @Override public boolean isSettable(Region node) {
                return node.scaleShape == null || !node.scaleShape.isBound();
            }

            @Override public StyleableProperty<Boolean> getStyleableProperty(Region node) {
                return (StyleableProperty)node.scaleShapeProperty();
            }
        };

         private static final CssMetaData<Region,Boolean> POSITION_SHAPE = 
             new CssMetaData<Region,Boolean>("-fx-position-shape",
                 BooleanConverter.getInstance(), Boolean.TRUE){

            @Override public boolean isSettable(Region node) {
                return node.centerShape == null || !node.centerShape.isBound();
            }

            @Override public StyleableProperty<Boolean> getStyleableProperty(Region node) {
                return (StyleableProperty)node.positionShapeProperty();
            }
        };

         private static final CssMetaData<Region,Boolean> CACHE_SHAPE =
             new CssMetaData<Region,Boolean>("-fx-cache-shape",
                 BooleanConverter.getInstance(), Boolean.TRUE){

            @Override public boolean isSettable(Region node) {
                return node.cacheShape == null || !node.cacheShape.isBound();
            }

            @Override public StyleableProperty<Boolean> getStyleableProperty(Region node) {
                return (StyleableProperty)node.cacheShapeProperty();
            }
        };

         private static final CssMetaData<Region, Boolean> SNAP_TO_PIXEL =
             new CssMetaData<Region,Boolean>("-fx-snap-to-pixel",
                 BooleanConverter.getInstance(), Boolean.TRUE){

            @Override public boolean isSettable(Region node) {
                return node.snapToPixel == null ||
                        !node.snapToPixel.isBound();
            }

            @Override public StyleableProperty<Boolean> getStyleableProperty(Region node) {
                return (StyleableProperty)node.snapToPixelProperty();
            }
        };

         private static final List<CssMetaData> STYLEABLES;
         static {

            final List<CssMetaData> styleables =
                new ArrayList<CssMetaData>(Parent.getClassCssMetaData());
            Collections.addAll(styleables,
                    PADDING,
                    BACKGROUND,
                    BORDER,
                    OPAQUE_INSETS,
                    SHAPE,
                    SCALE_SHAPE,
                    POSITION_SHAPE,
                    SNAP_TO_PIXEL
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData> getCssMetaData() {
        return getClassCssMetaData();
    }

}
