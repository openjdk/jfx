/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
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
import javafx.scene.image.Image;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.util.Callback;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import com.sun.javafx.util.Logging;
import com.sun.javafx.util.TempState;
import com.sun.javafx.binding.ExpressionHelper;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.InsetsConverter;
import javafx.css.converter.ShapeConverter;
import javafx.css.converter.SizeConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Vec2d;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.ParentHelper;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.scene.layout.RegionHelper;
import com.sun.javafx.scene.shape.ShapeHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGRegion;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.Scene;
import javafx.stage.Window;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;

/**
 * Region is the base class for all JavaFX Node-based UI Controls, and all layout containers.
 * It is a resizable Parent node which can be styled from CSS. It can have multiple backgrounds
 * and borders. It is designed to support as much of the CSS3 specification for backgrounds
 * and borders as is relevant to JavaFX.
 * The full specification is available at <a href="http://www.w3.org/TR/2012/CR-css3-background-20120724/">the W3C</a>.
 * <p>
 * Every Region has its layout bounds, which are specified to be (0, 0, width, height). A Region might draw outside
 * these bounds. The content area of a Region is the area which is occupied for the layout of its children.
 * This area is, by default, the same as the layout bounds of the Region, but can be modified by either the
 * properties of a border (either with BorderStrokes or BorderImages), and by padding. The padding can
 * be negative, such that the content area of a Region might extend beyond the layout bounds of the Region,
 * but does not affect the layout bounds.
 * <p>
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
 * <p>
 * By default a Region appears as a Rectangle. A BackgroundFill radii might cause the Rectangle to appear rounded.
 * This affects not only making the visuals look like a rounded rectangle, but it also causes the picking behavior
 * of the Region to act like a rounded rectangle, such that locations outside the corner radii are ignored. A
 * Region can be made to use any shape, however, by specifying the {@code shape} property. If a shape is specified,
 * then all BackgroundFills, BackgroundImages, and BorderStrokes will be applied to the shape. BorderImages are
 * not used for Regions which have a shape specified.
 * <p>
 * Although the layout bounds of a Region are not influenced by any Border or Background, the content area
 * insets and the picking area of the Region are. The {@code insets} of the Region define the distance
 * between the edge of the layout bounds and the edge of the content area. For example, if the Region
 * layout bounds are (x=0, y=0, width=200, height=100), and the insets are (top=10, right=20, bottom=30, left=40),
 * then the content area bounds will be (x=40, y=10, width=140, height=60). A Region subclass which is laying
 * out its children should compute and honor these content area bounds.
 * <p>
 * By default a Region inherits the layout behavior of its superclass, {@link Parent},
 * which means that it will resize any resizable child nodes to their preferred
 * size, but will not reposition them.  If an application needs more specific
 * layout behavior, then it should use one of the Region subclasses:
 * {@link StackPane}, {@link HBox}, {@link VBox}, {@link TilePane}, {@link FlowPane},
 * {@link BorderPane}, {@link GridPane}, or {@link AnchorPane}.
 * <p>
 * To implement a more custom layout, a Region subclass must override
 * {@link #computePrefWidth(double) computePrefWidth}, {@link #computePrefHeight(double) computePrefHeight}, and
 * {@link #layoutChildren() layoutChildren}. Note that {@link #layoutChildren() layoutChildren} is called automatically
 * by the scene graph while executing a top-down layout pass and it should not be invoked directly by the
 * region subclass.
 * <p>
 * Region subclasses which layout their children will position nodes by setting
 * {@link #setLayoutX(double) layoutX}/{@link #setLayoutY(double) layoutY} and do not alter
 * {@link #setTranslateX(double) translateX}/{@link #setTranslateY(double) translateY}, which are reserved for
 * adjustments and animation.
 * @since JavaFX 2.0
 */
public class Region extends Parent {
    static {
        RegionHelper.setRegionAccessor(new RegionHelper.RegionAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((Region) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((Region) node).doUpdatePeer();
            }

            @Override
            public Bounds doComputeLayoutBounds(Node node) {
                return ((Region) node).doComputeLayoutBounds();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((Region) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((Region) node).doComputeContains(localX, localY);
            }

            @Override
            public void doNotifyLayoutBoundsChanged(Node node) {
                ((Region) node).doNotifyLayoutBoundsChanged();
            }

            @Override
            public void doPickNodeLocal(Node node, PickRay localPickRay,
                    PickResultChooser result) {
                ((Region) node).doPickNodeLocal(localPickRay, result);
            }
        });
    }

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

    static Vec2d TEMP_VEC2D = new Vec2d();

    private static final double EPSILON = 1e-14;

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

    double adjustWidthByMargin(double width, Insets margin) {
        if (margin == null || margin == Insets.EMPTY) {
            return width;
        }
        boolean isSnapToPixel = isSnapToPixel();
        return width - snapSpaceX(margin.getLeft(), isSnapToPixel) - snapSpaceX(margin.getRight(), isSnapToPixel);
    }

    double adjustHeightByMargin(double height, Insets margin) {
        if (margin == null || margin == Insets.EMPTY) {
            return height;
        }
        boolean isSnapToPixel = isSnapToPixel();
        return height - snapSpaceY(margin.getTop(), isSnapToPixel) - snapSpaceY(margin.getBottom(), isSnapToPixel);
    }

    private static double getSnapScaleX(Node n) {
        return _getSnapScaleXimpl(n.getScene());
    }
    private static double _getSnapScaleXimpl(Scene scene) {
        if (scene == null) return 1.0;
        Window window = scene.getWindow();
        if (window == null) return 1.0;
        return window.getRenderScaleX();
    }

    private static double getSnapScaleY(Node n) {
        return _getSnapScaleYimpl(n.getScene());
    }
    private static double _getSnapScaleYimpl(Scene scene) {
        if (scene == null) return 1.0;
        Window window = scene.getWindow();
        if (window == null) return 1.0;
        return window.getRenderScaleY();
    }

    private double getSnapScaleX() {
        return _getSnapScaleXimpl(getScene());
    }

    private double getSnapScaleY() {
        return _getSnapScaleYimpl(getScene());
    }

    private static double scaledRound(double value, double scale) {
        return Math.round(value * scale) / scale;
    }

    /**
     * The value is floored for a given scale using Math.floor.
     * This method guarantees that:
     *
     * scaledFloor(scaledFloor(value, scale), scale) == scaledFloor(value, scale)
     *
     * @param value The value that needs to be floored
     * @param scale The scale that will be used
     * @return value floored with scale
     */
    private static double scaledFloor(double value, double scale) {
        return Math.floor(value * scale + EPSILON) / scale;
    }

    /**
     * The value is ceiled with a given scale using Math.ceil.
     * This method guarantees that:
     *
     * scaledCeil(scaledCeil(value, scale), scale) == scaledCeil(value, scale)
     *
     * @param value The value that needs to be ceiled
     * @param scale The scale that will be used
     * @return value ceiled with scale
     */
    private static double scaledCeil(double value, double scale) {
        return Math.ceil(value * scale - EPSILON) / scale;
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
    private double snapSpaceX(double value, boolean snapToPixel) {
        return snapToPixel ? scaledRound(value, getSnapScaleX()) : value;
    }
    private double snapSpaceY(double value, boolean snapToPixel) {
        return snapToPixel ? scaledRound(value, getSnapScaleY()) : value;
    }

    private static double snapSpace(double value, boolean snapToPixel, double snapScale) {
        return snapToPixel ? scaledRound(value, snapScale) : value;
    }

    /**
     * If snapToPixel is true, then the value is ceil'd using Math.ceil. Otherwise,
     * the value is simply returned.
     *
     * @param value The value that needs to be snapped
     * @param snapToPixel Whether to snap to pixel
     * @return value either as passed in or ceil'd based on snapToPixel
     */
    private double snapSizeX(double value, boolean snapToPixel) {
        return snapToPixel ? scaledCeil(value, getSnapScaleX()) : value;
    }
    private double snapSizeY(double value, boolean snapToPixel) {
        return snapToPixel ? scaledCeil(value, getSnapScaleY()) : value;
    }

    private static double snapSize(double value, boolean snapToPixel, double snapScale) {
        return snapToPixel ? scaledCeil(value, snapScale) : value;
    }

    /**
     * If snapToPixel is true, then the value is rounded using Math.round. Otherwise,
     * the value is simply returned.
     *
     * @param value The value that needs to be snapped
     * @param snapToPixel Whether to snap to pixel
     * @return value either as passed in or rounded based on snapToPixel
     */
    private double snapPositionX(double value, boolean snapToPixel) {
        return snapToPixel ? scaledRound(value, getSnapScaleX()) : value;
    }
    private double snapPositionY(double value, boolean snapToPixel) {
        return snapToPixel ? scaledRound(value, getSnapScaleY()) : value;
    }

    private static double snapPosition(double value, boolean snapToPixel, double snapScale) {
        return snapToPixel ? scaledRound(value, snapScale) : value;
    }

    /**
     * If snapToPixel is true, then the value is either floored (positive values) or
     * ceiled (negative values) with a scale. This method guarantees that:
     *
     * snapPortionX(snapPortionX(value, snapToPixel), snapToPixel) == snapPortionX(value, snapToPixel)
     *
     * @param value The value that needs to be snapped
     * @param snapToPixel Whether to snap to pixel
     * @return value either as passed, or floored or ceiled with scale, based on snapToPixel
     */
    private double snapPortionX(double value, boolean snapToPixel) {
        if (!snapToPixel || value == 0) return value;
        double s = getSnapScaleX();
        value *= s;
        if (value > 0) {
            value = Math.max(1, Math.floor(value + EPSILON));
        } else {
            value = Math.min(-1, Math.ceil(value - EPSILON));
        }
        return value / s;
    }

    /**
     * If snapToPixel is true, then the value is either floored (positive values) or
     * ceiled (negative values) with a scale. This method guarantees that:
     *
     * snapPortionY(snapPortionY(value, snapToPixel), snapToPixel) == snapPortionY(value, snapToPixel)
     *
     * @param value The value that needs to be snapped
     * @param snapToPixel Whether to snap to pixel
     * @return value either as passed, or floored or ceiled with scale, based on snapToPixel
     */
    private double snapPortionY(double value, boolean snapToPixel) {
        if (!snapToPixel || value == 0) return value;
        double s = getSnapScaleY();
        value *= s;
        if (value > 0) {
            value = Math.max(1, Math.floor(value + EPSILON));
        } else {
            value = Math.min(-1, Math.ceil(value - EPSILON));
        }
        return value / s;
    }

    double getAreaBaselineOffset(List<Node> children, Callback<Node, Insets> margins,
                                        Function<Integer, Double> positionToWidth,
                                        double areaHeight, boolean fillHeight) {
        return getAreaBaselineOffset(children, margins, positionToWidth, areaHeight, fillHeight, isSnapToPixel());
    }

    static double getAreaBaselineOffset(List<Node> children, Callback<Node, Insets> margins,
            Function<Integer, Double> positionToWidth,
            double areaHeight, boolean fillHeight, boolean snapToPixel) {
        return getAreaBaselineOffset(children, margins, positionToWidth, areaHeight, fillHeight,
                getMinBaselineComplement(children), snapToPixel);
    }

    double getAreaBaselineOffset(List<Node> children, Callback<Node, Insets> margins,
                                 Function<Integer, Double> positionToWidth,
                                 double areaHeight, final boolean fillHeight, double minComplement) {
        return getAreaBaselineOffset(children, margins, positionToWidth, areaHeight, fillHeight, minComplement, isSnapToPixel());
    }

    static double getAreaBaselineOffset(List<Node> children, Callback<Node, Insets> margins,
            Function<Integer, Double> positionToWidth,
            double areaHeight, final boolean fillHeight, double minComplement, boolean snapToPixel) {
        return getAreaBaselineOffset(children, margins, positionToWidth, areaHeight, t -> fillHeight, minComplement, snapToPixel);
    }

    double getAreaBaselineOffset(List<Node> children, Callback<Node, Insets> margins,
                                 Function<Integer, Double> positionToWidth,
                                 double areaHeight, Function<Integer, Boolean> fillHeight, double minComplement) {
        return getAreaBaselineOffset(children, margins, positionToWidth, areaHeight, fillHeight, minComplement, isSnapToPixel());
    }

    /**
     * Returns the baseline offset of provided children, with respect to the minimum complement, computed
     * by {@link #getMinBaselineComplement(java.util.List)} from the same set of children.
     * @param children the children with baseline alignment
     * @param margins their margins (callback)
     * @param positionToWidth callback for children widths (can return -1 if no bias is used)
     * @param areaHeight height of the area to layout in
     * @param fillHeight callback to specify children that has fillHeight constraint
     * @param minComplement minimum complement
     */
    static double getAreaBaselineOffset(List<Node> children, Callback<Node, Insets> margins,
            Function<Integer, Double> positionToWidth,
            double areaHeight, Function<Integer, Boolean> fillHeight, double minComplement, boolean snapToPixel) {
        double b = 0;
        double snapScaleV = 0.0;
        for (int i = 0;i < children.size(); ++i) {
            Node n = children.get(i);
            // Note: all children should be coming from the same parent so they should all have the same snapScale
            if (snapToPixel && i == 0) snapScaleV = getSnapScaleY(n.getParent());
            Insets margin = margins.call(n);
            double top = margin != null ? snapSpace(margin.getTop(), snapToPixel, snapScaleV) : 0;
            double bottom = (margin != null ? snapSpace(margin.getBottom(), snapToPixel, snapScaleV) : 0);
            final double bo = n.getBaselineOffset();
            if (bo == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                double alt = -1;
                if (n.getContentBias() == Orientation.HORIZONTAL) {
                    alt = positionToWidth.apply(i);
                }
                if (fillHeight.apply(i)) {
                    // If the children fills it's height, than it's "preferred" height is the area without the complement and insets
                    b = Math.max(b, top + boundedSize(n.minHeight(alt), areaHeight - minComplement - top - bottom,
                            n.maxHeight(alt)));
                } else {
                    // Otherwise, we must use the area without complement and insets as a maximum for the Node
                    b = Math.max(b, top + boundedSize(n.minHeight(alt), n.prefHeight(alt),
                            Math.min(n.maxHeight(alt), areaHeight - minComplement - top - bottom)));
                }
            } else {
                b = Math.max(b, top + bo);
            }
        }
        return b;
    }

    /**
     * Return the minimum complement of baseline
     * @param children
     * @return
     */
    static double getMinBaselineComplement(List<Node> children) {
        return getBaselineComplement(children, true, false);
    }

    /**
     * Return the preferred complement of baseline
     * @param children
     * @return
     */
    static double getPrefBaselineComplement(List<Node> children) {
        return getBaselineComplement(children, false, false);
    }

    /**
     * Return the maximal complement of baseline
     * @param children
     * @return
     */
    static double getMaxBaselineComplement(List<Node> children) {
        return getBaselineComplement(children, false, true);
    }

    private static double getBaselineComplement(List<Node> children, boolean min, boolean max) {
        double bc = 0;
        for (Node n : children) {
            final double bo = n.getBaselineOffset();
            if (bo == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                continue;
            }
            if (n.isResizable()) {
                bc = Math.max(bc, (min ? n.minHeight(-1) : max ? n.maxHeight(-1) : n.prefHeight(-1)) - bo);
            } else {
                bc = Math.max(bc, n.getLayoutBounds().getHeight() - bo);
            }
        }
        return bc;
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
     * At the time that a Background or Border is set on a Region, we inspect any
     * BackgroundImage or BorderImage objects, to see if the Image backing them
     * is background loading and not yet complete, or is animated. In such cases
     * we attach the imageChangeListener to them, so that when the image finishes,
     * the Region will be redrawn. If the particular image object is not animating
     * (but was just background loading), then we also remove the listener.
     * We also are sure to remove this listener from any old BackgroundImage or
     * BorderImage images in the background and border property invalidation code.
     */
    private InvalidationListener imageChangeListener = observable -> {
        final ReadOnlyObjectPropertyBase imageProperty = (ReadOnlyObjectPropertyBase) observable;
        final Image image = (Image) imageProperty.getBean();
        final Toolkit.ImageAccessor acc = Toolkit.getImageAccessor();
        if (image.getProgress() == 1 && !acc.isAnimation(image)) {
            // We can go ahead and remove the listener since loading is done.
            removeImageListener(image);
        }
        // Cause the region to repaint
        NodeHelper.markDirty(this, DirtyBits.NODE_CONTENTS);
    };

    {
        // To initialize the class helper at the beginning each constructor of this class
        RegionHelper.initHelper(this);
    }

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
                updateSnappedInsets();
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
                @Override public CssMetaData<Region, Boolean> getCssMetaData() {
                    return StyleableProperties.SNAP_TO_PIXEL;
                }
                @Override public void invalidated() {
                    boolean value = get();
                    if (_snapToPixel != value) {
                        _snapToPixel = value;
                        updateSnappedInsets();
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
     * minimum and preferred sizes. By default, padding is {@code Insets.EMPTY}. Setting the
     * value to {@code null} should be avoided.
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
        @Override public CssMetaData<Region, Insets> getCssMetaData() {
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
            } else if (!newValue.equals(lastValidValue)) {
                lastValidValue = newValue;
                insets.fireValueChanged();
            }
        }
    };
    public final void setPadding(Insets value) { padding.set(value); }
    public final Insets getPadding() { return padding.get(); }
    public final ObjectProperty<Insets> paddingProperty() { return padding; }

    /**
     * The background of the Region, which is made up of zero or more BackgroundFills, and
     * zero or more BackgroundImages. It is possible for a Background to be empty, where it
     * has neither fills nor images, and is semantically equivalent to null.
     * @since JavaFX 8.0
     */
    private final ObjectProperty<Background> background = new StyleableObjectProperty<Background>(null) {
        private Background old = null;
        @Override public Object getBean() { return Region.this; }
        @Override public String getName() { return "background"; }
        @Override public CssMetaData<Region, Background> getCssMetaData() {
            return StyleableProperties.BACKGROUND;
        }

        @Override protected void invalidated() {
            final Background b = get();
            if(old != null ? !old.equals(b) : b != null) {
                // They are different! Both cannot be null
                if (old == null || b == null || !old.getOutsets().equals(b.getOutsets())) {
                    // We have determined that the outsets of these two different background
                    // objects is different, and therefore the bounds have changed.
                    NodeHelper.geomChanged(Region.this);
                    insets.fireValueChanged();
                }

                // If the Background is made up of any BackgroundImage objects, then we must
                // inspect the images of those BackgroundImage objects to see if they are still
                // being loaded in the background or if they are animated. If so, then we need
                // to attach a listener, so that when the image finishes loading or changes,
                // we can repaint the region.
                if (b != null) {
                    for (BackgroundImage i : b.getImages()) {
                        final Image image = i.image;
                        final Toolkit.ImageAccessor acc = Toolkit.getImageAccessor();
                        if (acc.isAnimation(image) || image.getProgress() < 1) {
                            addImageListener(image);
                        }
                    }
                }

                // And we must remove this listener from any old images
                if (old != null) {
                    for (BackgroundImage i : old.getImages()) {
                        removeImageListener(i.image);
                    }
                }

                // No matter what, the fill has changed, so we have to update it
                NodeHelper.markDirty(Region.this, DirtyBits.SHAPE_FILL);
                cornersValid = false;
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
     * @since JavaFX 8.0
     */
    private final ObjectProperty<Border> border = new StyleableObjectProperty<Border>(null) {
        private Border old = null;
        @Override public Object getBean() { return Region.this; }
        @Override public String getName() { return "border"; }
        @Override public CssMetaData<Region, Border> getCssMetaData() {
            return StyleableProperties.BORDER;
        }
        @Override protected void invalidated() {
            final Border b = get();
            if(old != null ? !old.equals(b) : b != null) {
                // They are different! Both cannot be null
                if (old == null || b == null || !old.getOutsets().equals(b.getOutsets())) {
                    // We have determined that the outsets of these two different border
                    // objects is different, and therefore the bounds have changed.
                    NodeHelper.geomChanged(Region.this);
                }
                if (old == null || b == null || !old.getInsets().equals(b.getInsets())) {
                    insets.fireValueChanged();
                }

                // If the Border is made up of any BorderImage objects, then we must
                // inspect the images of those BorderImage objects to see if they are still
                // being loaded in the background or if they are animated. If so, then we need
                // to attach a listener, so that when the image finishes loading or changes,
                // we can repaint the region.
                if (b != null) {
                    for (BorderImage i : b.getImages()) {
                        final Image image = i.image;
                        final Toolkit.ImageAccessor acc = Toolkit.getImageAccessor();
                        if (acc.isAnimation(image) || image.getProgress() < 1) {
                            addImageListener(image);
                        }
                    }
                }

                // And we must remove this listener from any old images
                if (old != null) {
                    for (BorderImage i : old.getImages()) {
                        removeImageListener(i.image);
                    }
                }

                // No matter what, the fill has changed, so we have to update it
                NodeHelper.markDirty(Region.this, DirtyBits.SHAPE_STROKE);
                cornersValid = false;
                old = b;
            }
        }
    };
    public final void setBorder(Border value) { border.set(value); }
    public final Border getBorder() { return border.get(); }
    public final ObjectProperty<Border> borderProperty() { return border; }

    /**
     * Adds the imageChangeListener to this image. This method was broken out and made
     * package private for testing purposes.
     *
     * @param image a non-null image
     */
    void addImageListener(Image image) {
        final Toolkit.ImageAccessor acc = Toolkit.getImageAccessor();
        acc.getImageProperty(image).addListener(imageChangeListener);
    }

    /**
     * Removes the imageChangeListener from this image. This method was broken out and made
     * package private for testing purposes.
     *
     * @param image a non-null image
     */
    void removeImageListener(Image image) {
        final Toolkit.ImageAccessor acc = Toolkit.getImageAccessor();
        acc.getImageProperty(image).removeListener(imageChangeListener);
    }

    /**
     * Defines the area of the region within which completely opaque pixels
     * are drawn. This is used for various performance optimizations.
     * The pixels within this area MUST BE fully opaque, or rendering
     * artifacts will result. It is the responsibility of the application, either
     * via code or via CSS, to ensure that the opaqueInsets is correct for
     * a Region based on the backgrounds and borders of that region. The values
     * for each of the insets must be real numbers, not NaN or Infinity. If
     * no known insets exist, then the opaqueInsets should be set to null.
     * @return the opaque insets property
     * @since JavaFX 8.0
     */
    public final ObjectProperty<Insets> opaqueInsetsProperty() {
        if (opaqueInsets == null) {
            opaqueInsets = new StyleableObjectProperty<Insets>() {
                @Override public Object getBean() { return Region.this; }
                @Override public String getName() { return "opaqueInsets"; }
                @Override public CssMetaData<Region, Insets> getCssMetaData() {
                    return StyleableProperties.OPAQUE_INSETS;
                }
                @Override protected void invalidated() {
                    // This causes the background to be updated, which
                    // is the code block where we also compute the opaque insets
                    // since updating the background is super fast even when
                    // nothing has changed.
                    NodeHelper.markDirty(Region.this, DirtyBits.SHAPE_FILL);
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
     * @since JavaFX 8.0
     */
    private final InsetsProperty insets = new InsetsProperty();
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
            updateSnappedInsets();
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
     * cached results of snapped insets, this are used a lot during layout so makes sense
     * to keep fast access cached copies here.
     */
    private double snappedTopInset = 0;
    private double snappedRightInset = 0;
    private double snappedBottomInset = 0;
    private double snappedLeftInset = 0;

    /**
     * Cached snapScale values, used to determine if snapped cached insets values
     * should be invalidated because screen scale has changed.
     */
    private double lastUsedSnapScaleY = 0;
    private double lastUsedSnapScaleX = 0;

    /** Called to update the cached snapped insets */
    private void updateSnappedInsets() {
        lastUsedSnapScaleX = getSnapScaleX();
        lastUsedSnapScaleY = getSnapScaleY();
        final Insets insets = getInsets();
        final boolean snap = isSnapToPixel();
        snappedTopInset = snapSpaceY(insets.getTop(), snap);
        snappedRightInset = snapSpaceX(insets.getRight(), snap);
        snappedBottomInset = snapSpaceY(insets.getBottom(), snap);
        snappedLeftInset = snapSpaceX(insets.getLeft(), snap);
    }

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
            cornersValid = false;
            boundingBox = null;
            NodeHelper.layoutBoundsChanged(this);
            NodeHelper.geomChanged(this);
            NodeHelper.markDirty(this, DirtyBits.NODE_GEOMETRY);
            setNeedsLayout(true);
            requestParentLayout();
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
            cornersValid = false;
            // It is possible that somebody sets the height of the region to a value which
            // it previously held. If this is the case, we want to avoid excessive layouts.
            // Note that I have biased this for layout over binding, because the heightProperty
            // is now going to recompute the height eagerly. The cost of excessive and
            // unnecessary bounds changes, however, is relatively high.
            boundingBox = null;
            // Note: although NodeHelper.geomChanged will usually also invalidate the
            // layout bounds, that is not the case for Regions, and both must
            // be called separately.
            NodeHelper.geomChanged(this);
            NodeHelper.layoutBoundsChanged(this);
            // We use "NODE_GEOMETRY" to mean that the bounds have changed and
            // need to be sync'd with the render tree
            NodeHelper.markDirty(this, DirtyBits.NODE_GEOMETRY);
            // Change of the height (or width) won't change the preferred size.
            // So we don't need to flush the cache. We should however mark this node
            // as needs layout to be internally layouted.
            setNeedsLayout(true);
            // This call is only needed when this was not called from the parent during the layout.
            // Otherwise it would flush the cache of the parent, which is not necessary
            requestParentLayout();
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

    /**
     * This class is reused for the min, pref, and max properties since
     * they all performed the same function (to call requestParentLayout).
     */
    private final class MinPrefMaxProperty extends StyleableDoubleProperty {
        private final String name;
        private final CssMetaData<? extends Styleable, Number> cssMetaData;

        MinPrefMaxProperty(String name, double initialValue, CssMetaData<? extends Styleable, Number> cssMetaData) {
            super(initialValue);
            this.name = name;
            this.cssMetaData = cssMetaData;
        }

        @Override public void invalidated() { requestParentLayout(); }
        @Override public Object getBean() { return Region.this; }
        @Override public String getName() { return name; }

        @Override
        public CssMetaData<? extends Styleable, Number> getCssMetaData() {
            return cssMetaData;
        }
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
        if (minWidth == null) minWidth = new MinPrefMaxProperty("minWidth", _minWidth, StyleableProperties.MIN_WIDTH);
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
        if (minHeight == null) minHeight = new MinPrefMaxProperty("minHeight", _minHeight, StyleableProperties.MIN_HEIGHT);
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
        if (prefWidth == null) prefWidth = new MinPrefMaxProperty("prefWidth", _prefWidth, StyleableProperties.PREF_WIDTH);
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
        if (prefHeight == null) prefHeight = new MinPrefMaxProperty("prefHeight", _prefHeight, StyleableProperties.PREF_HEIGHT);
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
        if (maxWidth == null) maxWidth = new MinPrefMaxProperty("maxWidth", _maxWidth, StyleableProperties.MAX_WIDTH);
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
        if (maxHeight == null) maxHeight = new MinPrefMaxProperty("maxHeight", _maxHeight, StyleableProperties.MAX_HEIGHT);
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
     * When specified, the {@code Shape} will cause the region to be
     * rendered as the specified shape rather than as a rounded rectangle.
     * When null, the Region is rendered as a rounded rectangle. When rendered
     * as a Shape, any Background is used to fill the shape, although any
     * background insets are ignored as are background radii. Any BorderStrokes
     * defined are used for stroking the shape. Any BorderImages are ignored.
     *
     * @defaultValue null
     * @since JavaFX 8.0
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
        @Override public CssMetaData<Region, Shape> getCssMetaData() {
            return StyleableProperties.SHAPE;
        }
        @Override protected void invalidated() {
            final Shape value = get();
            if (_shape != value) {
                // The shape has changed. We need to add/remove listeners
                if (_shape != null) ShapeHelper.setShapeChangeListener(_shape, null);
                if (value != null) ShapeHelper.setShapeChangeListener(value, this);
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
            NodeHelper.geomChanged(Region.this);
            NodeHelper.markDirty(Region.this, DirtyBits.REGION_SHAPE);
        }
    };

    /**
     * Specifies whether the shape, if defined, is scaled to match the size of the Region.
     * {@code true} means the shape is scaled to fit the size of the Region, {@code false}
     * means the shape is at its source size, its positioning depends on the value of
     * {@code centerShape}.
     *
     * @defaultValue true
     * @since JavaFX 8.0
     */
    private BooleanProperty scaleShape = null;
    public final void setScaleShape(boolean value) { scaleShapeProperty().set(value); }
    public final boolean isScaleShape() { return scaleShape == null ? true : scaleShape.get(); }
    public final BooleanProperty scaleShapeProperty() {
        if (scaleShape == null) {
            scaleShape = new StyleableBooleanProperty(true) {
                @Override public Object getBean() { return Region.this; }
                @Override public String getName() { return "scaleShape"; }
                @Override public CssMetaData<Region, Boolean> getCssMetaData() {
                    return StyleableProperties.SCALE_SHAPE;
                }
                @Override public void invalidated() {
                    NodeHelper.geomChanged(Region.this);
                    NodeHelper.markDirty(Region.this, DirtyBits.REGION_SHAPE);
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
     * @defaultValue true
     * @since JavaFX 8.0
     */
    private BooleanProperty centerShape = null;
    public final void setCenterShape(boolean value) { centerShapeProperty().set(value); }
    public final boolean isCenterShape() { return centerShape == null ? true : centerShape.get(); }
    public final BooleanProperty centerShapeProperty() {
        if (centerShape == null) {
            centerShape = new StyleableBooleanProperty(true) {
                @Override public Object getBean() { return Region.this; }
                @Override public String getName() { return "centerShape"; }
                @Override public CssMetaData<Region, Boolean> getCssMetaData() {
                    return StyleableProperties.POSITION_SHAPE;
                }
                @Override public void invalidated() {
                    NodeHelper.geomChanged(Region.this);
                    NodeHelper.markDirty(Region.this, DirtyBits.REGION_SHAPE);
                }
            };
        }
        return centerShape;
    }

    /**
     * Defines a hint to the system indicating that the Shape used to define the region's
     * background is stable and would benefit from caching.
     *
     * @defaultValue true
     * @since JavaFX 8.0
     */
    private BooleanProperty cacheShape = null;
    public final void setCacheShape(boolean value) { cacheShapeProperty().set(value); }
    public final boolean isCacheShape() { return cacheShape == null ? true : cacheShape.get(); }
    public final BooleanProperty cacheShapeProperty() {
        if (cacheShape == null) {
            cacheShape = new StyleableBooleanProperty(true) {
                @Override public Object getBean() { return Region.this; }
                @Override public String getName() { return "cacheShape"; }
                @Override public CssMetaData<Region, Boolean> getCssMetaData() {
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
        if (logger.isLoggable(Level.FINER)) {
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
        final double override = getMinWidth();
        if (override == USE_COMPUTED_SIZE) {
            return super.minWidth(height);
        } else if (override == USE_PREF_SIZE) {
            return prefWidth(height);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
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
        final double override = getMinHeight();
        if (override == USE_COMPUTED_SIZE) {
            return super.minHeight(width);
        } else if (override == USE_PREF_SIZE) {
            return prefHeight(width);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
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
        final double override = getPrefWidth();
        if (override == USE_COMPUTED_SIZE) {
            return super.prefWidth(height);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
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
        final double override = getPrefHeight();
        if (override == USE_COMPUTED_SIZE) {
            return super.prefHeight(width);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
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
        final double override = getMaxWidth();
        if (override == USE_COMPUTED_SIZE) {
            return computeMaxWidth(height);
        } else if (override == USE_PREF_SIZE) {
            return prefWidth(height);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
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
        final double override = getMaxHeight();
        if (override == USE_COMPUTED_SIZE) {
            return computeMaxHeight(width);
        } else if (override == USE_PREF_SIZE) {
            return prefHeight(width);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
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
     * @param height The height of the Region, in case this value might dictate
     * the maximum width
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
     * @param width The width of the Region, in case this value might dictate
     * the maximum height
     * @return the computed maximum height for this region
     */
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    /**
     * If this region's snapToPixel property is false, this method returns the
     * same value, else it tries to return a value rounded to the nearest
     * pixel, but since there is no indication if the value is a vertical
     * or horizontal measurement then it may be snapped to the wrong pixel
     * size metric on screens with different horizontal and vertical scales.
     * @param value the space value to be snapped
     * @return value rounded to nearest pixel
     * @deprecated replaced by {@code snapSpaceX()} and {@code snapSpaceY()}
     */
    @Deprecated(since="9")
    protected double snapSpace(double value) {
        return snapSpaceX(value, isSnapToPixel());
    }

    /**
     * If this region's snapToPixel property is true, returns a value rounded
     * to the nearest pixel in the horizontal direction, else returns the
     * same value.
     * @param value the space value to be snapped
     * @return value rounded to nearest pixel
     * @since 9
     */
    public double snapSpaceX(double value) {
        return snapSpaceX(value, isSnapToPixel());
    }

    /**
     * If this region's snapToPixel property is true, returns a value rounded
     * to the nearest pixel in the vertical direction, else returns the
     * same value.
     * @param value the space value to be snapped
     * @return value rounded to nearest pixel
     * @since 9
     */
    public double snapSpaceY(double value) {
        return snapSpaceY(value, isSnapToPixel());
    }

    /**
     * If this region's snapToPixel property is false, this method returns the
     * same value, else it tries to return a value ceiled to the nearest
     * pixel, but since there is no indication if the value is a vertical
     * or horizontal measurement then it may be snapped to the wrong pixel
     * size metric on screens with different horizontal and vertical scales.
     * @param value the size value to be snapped
     * @return value ceiled to nearest pixel
     * @deprecated replaced by {@code snapSizeX()} and {@code snapSizeY()}
     */
    @Deprecated(since="9")
    protected double snapSize(double value) {
        return snapSizeX(value, isSnapToPixel());
    }

    /**
     * If this region's snapToPixel property is true, returns a value ceiled
     * to the nearest pixel in the horizontal direction, else returns the
     * same value.
     * @param value the size value to be snapped
     * @return value ceiled to nearest pixel
     * @since 9
     */
    public double snapSizeX(double value) {
        return snapSizeX(value, isSnapToPixel());
    }

    /**
     * If this region's snapToPixel property is true, returns a value ceiled
     * to the nearest pixel in the vertical direction, else returns the
     * same value.
     * @param value the size value to be snapped
     * @return value ceiled to nearest pixel
     * @since 9
     */
    public double snapSizeY(double value) {
        return snapSizeY(value, isSnapToPixel());
    }

    /**
     * If this region's snapToPixel property is false, this method returns the
     * same value, else it tries to return a value rounded to the nearest
     * pixel, but since there is no indication if the value is a vertical
     * or horizontal measurement then it may be snapped to the wrong pixel
     * size metric on screens with different horizontal and vertical scales.
     * @param value the position value to be snapped
     * @return value rounded to nearest pixel
     * @deprecated replaced by {@code snapPositionX()} and {@code snapPositionY()}
     */
    @Deprecated(since="9")
    protected double snapPosition(double value) {
        return snapPositionX(value, isSnapToPixel());
    }

    /**
     * If this region's snapToPixel property is true, returns a value rounded
     * to the nearest pixel in the horizontal direction, else returns the
     * same value.
     * @param value the position value to be snapped
     * @return value rounded to nearest pixel
     * @since 9
     */
    public double snapPositionX(double value) {
        return snapPositionX(value, isSnapToPixel());
    }

    /**
     * If this region's snapToPixel property is true, returns a value rounded
     * to the nearest pixel in the vertical direction, else returns the
     * same value.
     * @param value the position value to be snapped
     * @return value rounded to nearest pixel
     * @since 9
     */
    public double snapPositionY(double value) {
        return snapPositionY(value, isSnapToPixel());
    }

    double snapPortionX(double value) {
        return snapPortionX(value, isSnapToPixel());
    }
    double snapPortionY(double value) {
        return snapPortionY(value, isSnapToPixel());
    }


    /**
     * Utility method to get the top inset which includes padding and border
     * inset. Then snapped to whole pixels if isSnapToPixel() is true.
     *
     * @since JavaFX 8.0
     * @return Rounded up insets top
     */
    public final double snappedTopInset() {
        // invalidate the cached values for snapped inset dimensions
        // if the screen scale changed since they were last computed.
        if (lastUsedSnapScaleY != getSnapScaleY()) {
            updateSnappedInsets();
        }
        return snappedTopInset;
    }

    /**
     * Utility method to get the bottom inset which includes padding and border
     * inset. Then snapped to whole pixels if isSnapToPixel() is true.
     *
     * @since JavaFX 8.0
     * @return Rounded up insets bottom
     */
    public final double snappedBottomInset() {
        // invalidate the cached values for snapped inset dimensions
        // if the screen scale changed since they were last computed.
        if (lastUsedSnapScaleY != getSnapScaleY()) {
            updateSnappedInsets();
        }
        return snappedBottomInset;
    }

    /**
     * Utility method to get the left inset which includes padding and border
     * inset. Then snapped to whole pixels if isSnapToPixel() is true.
     *
     * @since JavaFX 8.0
     * @return Rounded up insets left
     */
    public final double snappedLeftInset() {
        // invalidate the cached values for snapped inset dimensions
        // if the screen scale changed since they were last computed.
        if (lastUsedSnapScaleX != getSnapScaleX()) {
            updateSnappedInsets();
        }
        return snappedLeftInset;
    }

    /**
     * Utility method to get the right inset which includes padding and border
     * inset. Then snapped to whole pixels if isSnapToPixel() is true.
     *
     * @since JavaFX 8.0
     * @return Rounded up insets right
     */
    public final double snappedRightInset() {
        // invalidate the cached values for snapped inset dimensions
        // if the screen scale changed since they were last computed.
        if (lastUsedSnapScaleX != getSnapScaleX()) {
            updateSnappedInsets();
        }
        return snappedRightInset;
    }


    double computeChildMinAreaWidth(Node child, Insets margin) {
        return computeChildMinAreaWidth(child, -1, margin, -1, false);
    }

    double computeChildMinAreaWidth(Node child, double baselineComplement, Insets margin, double height, boolean fillHeight) {
        final boolean snap = isSnapToPixel();
        double left = margin != null? snapSpaceX(margin.getLeft(), snap) : 0;
        double right = margin != null? snapSpaceX(margin.getRight(), snap) : 0;
        double alt = -1;
        if (height != -1 && child.isResizable() && child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            double top = margin != null? snapSpaceY(margin.getTop(), snap) : 0;
            double bottom = (margin != null? snapSpaceY(margin.getBottom(), snap) : 0);
            double bo = child.getBaselineOffset();
            final double contentHeight = bo == BASELINE_OFFSET_SAME_AS_HEIGHT && baselineComplement != -1 ?
                    height - top - bottom - baselineComplement :
                     height - top - bottom;
            if (fillHeight) {
                alt = snapSizeY(boundedSize(
                        child.minHeight(-1), contentHeight,
                        child.maxHeight(-1)));
            } else {
                alt = snapSizeY(boundedSize(
                        child.minHeight(-1),
                        child.prefHeight(-1),
                        Math.min(child.maxHeight(-1), contentHeight)));
            }
        }
        return left + snapSizeX(child.minWidth(alt)) + right;
    }

    double computeChildMinAreaHeight(Node child, Insets margin) {
        return computeChildMinAreaHeight(child, -1, margin, -1);
    }

    double computeChildMinAreaHeight(Node child, double minBaselineComplement, Insets margin, double width) {
        final boolean snap = isSnapToPixel();
        double top =margin != null? snapSpaceY(margin.getTop(), snap) : 0;
        double bottom = margin != null? snapSpaceY(margin.getBottom(), snap) : 0;

        double alt = -1;
        if (child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            double left = margin != null? snapSpaceX(margin.getLeft(), snap) : 0;
            double right = margin != null? snapSpaceX(margin.getRight(), snap) : 0;
            alt = snapSizeX(width != -1? boundedSize(child.minWidth(-1), width - left - right, child.maxWidth(-1)) :
                    child.maxWidth(-1));
        }

        // For explanation, see computeChildPrefAreaHeight
        if (minBaselineComplement != -1) {
            double baseline = child.getBaselineOffset();
            if (child.isResizable() && baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                return top + snapSizeY(child.minHeight(alt)) + bottom
                        + minBaselineComplement;
            } else {
                return baseline + minBaselineComplement;
            }
        } else {
            return top + snapSizeY(child.minHeight(alt)) + bottom;
        }
    }

    double computeChildPrefAreaWidth(Node child, Insets margin) {
        return computeChildPrefAreaWidth(child, -1, margin, -1, false);
    }

    double computeChildPrefAreaWidth(Node child, double baselineComplement, Insets margin, double height, boolean fillHeight) {
        final boolean snap = isSnapToPixel();
        double left = margin != null? snapSpaceX(margin.getLeft(), snap) : 0;
        double right = margin != null? snapSpaceX(margin.getRight(), snap) : 0;
        double alt = -1;
        if (height != -1 && child.isResizable() && child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            double top = margin != null? snapSpaceY(margin.getTop(), snap) : 0;
            double bottom = margin != null? snapSpaceY(margin.getBottom(), snap) : 0;
            double bo = child.getBaselineOffset();
            final double contentHeight = bo == BASELINE_OFFSET_SAME_AS_HEIGHT && baselineComplement != -1 ?
                    height - top - bottom - baselineComplement :
                     height - top - bottom;
            if (fillHeight) {
                alt = snapSizeY(boundedSize(
                        child.minHeight(-1), contentHeight,
                        child.maxHeight(-1)));
            } else {
                alt = snapSizeY(boundedSize(
                        child.minHeight(-1),
                        child.prefHeight(-1),
                        Math.min(child.maxHeight(-1), contentHeight)));
            }
        }
        return left + snapSizeX(boundedSize(child.minWidth(alt), child.prefWidth(alt), child.maxWidth(alt))) + right;
    }

    double computeChildPrefAreaHeight(Node child, Insets margin) {
        return computeChildPrefAreaHeight(child, -1, margin, -1);
    }

    double computeChildPrefAreaHeight(Node child, double prefBaselineComplement, Insets margin, double width) {
        final boolean snap = isSnapToPixel();
        double top = margin != null? snapSpaceY(margin.getTop(), snap) : 0;
        double bottom = margin != null? snapSpaceY(margin.getBottom(), snap) : 0;

        double alt = -1;
        if (child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            double left = margin != null ? snapSpaceX(margin.getLeft(), snap) : 0;
            double right = margin != null ? snapSpaceX(margin.getRight(), snap) : 0;
            alt = snapSizeX(boundedSize(
                    child.minWidth(-1), width != -1 ? width - left - right
                    : child.prefWidth(-1), child.maxWidth(-1)));
        }

        if (prefBaselineComplement != -1) {
            double baseline = child.getBaselineOffset();
            if (child.isResizable() && baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                // When baseline is same as height, the preferred height of the node will be above the baseline, so we need to add
                // the preferred complement to it
                return top + snapSizeY(boundedSize(child.minHeight(alt), child.prefHeight(alt), child.maxHeight(alt))) + bottom
                        + prefBaselineComplement;
            } else {
                // For all other Nodes, it's just their baseline and the complement.
                // Note that the complement already contain the Node's preferred (or fixed) height
                return top + baseline + prefBaselineComplement + bottom;
            }
        } else {
            return top + snapSizeY(boundedSize(child.minHeight(alt), child.prefHeight(alt), child.maxHeight(alt))) + bottom;
        }
    }

    double computeChildMaxAreaWidth(Node child, double baselineComplement, Insets margin, double height, boolean fillHeight) {
        double max = child.maxWidth(-1);
        if (max == Double.MAX_VALUE) {
            return max;
        }
        final boolean snap = isSnapToPixel();
        double left = margin != null? snapSpaceX(margin.getLeft(), snap) : 0;
        double right = margin != null? snapSpaceX(margin.getRight(), snap) : 0;
        double alt = -1;
        if (height != -1 && child.isResizable() && child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            double top = margin != null? snapSpaceY(margin.getTop(), snap) : 0;
            double bottom = (margin != null? snapSpaceY(margin.getBottom(), snap) : 0);
            double bo = child.getBaselineOffset();
            final double contentHeight = bo == BASELINE_OFFSET_SAME_AS_HEIGHT && baselineComplement != -1 ?
                    height - top - bottom - baselineComplement :
                     height - top - bottom;
            if (fillHeight) {
                alt = snapSizeY(boundedSize(
                        child.minHeight(-1), contentHeight,
                        child.maxHeight(-1)));
            } else {
                alt = snapSizeY(boundedSize(
                        child.minHeight(-1),
                        child.prefHeight(-1),
                        Math.min(child.maxHeight(-1), contentHeight)));
            }
            max = child.maxWidth(alt);
        }
        // if min > max, min wins, so still need to call boundedSize()
        return left + snapSizeX(boundedSize(child.minWidth(alt), max, Double.MAX_VALUE)) + right;
    }

    double computeChildMaxAreaHeight(Node child, double maxBaselineComplement, Insets margin, double width) {
        double max = child.maxHeight(-1);
        if (max == Double.MAX_VALUE) {
            return max;
        }

        final boolean snap = isSnapToPixel();
        double top = margin != null? snapSpaceY(margin.getTop(), snap) : 0;
        double bottom = margin != null? snapSpaceY(margin.getBottom(), snap) : 0;
        double alt = -1;
        if (child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            double left = margin != null? snapSpaceX(margin.getLeft(), snap) : 0;
            double right = margin != null? snapSpaceX(margin.getRight(), snap) : 0;
            alt = snapSizeX(width != -1? boundedSize(child.minWidth(-1), width - left - right, child.maxWidth(-1)) :
                child.minWidth(-1));
            max = child.maxHeight(alt);
        }
        // For explanation, see computeChildPrefAreaHeight
        if (maxBaselineComplement != -1) {
            double baseline = child.getBaselineOffset();
            if (child.isResizable() && baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                return top + snapSizeY(boundedSize(child.minHeight(alt), child.maxHeight(alt), Double.MAX_VALUE)) + bottom
                        + maxBaselineComplement;
            } else {
                return top + baseline + maxBaselineComplement + bottom;
            }
        } else {
            // if min > max, min wins, so still need to call boundedSize()
            return top + snapSizeY(boundedSize(child.minHeight(alt), max, Double.MAX_VALUE)) + bottom;
        }
    }

    /* Max of children's minimum area widths */

    double computeMaxMinAreaWidth(List<Node> children, Callback<Node, Insets> margins) {
        return getMaxAreaWidth(children, margins, new double[] { -1 }, false, true);
    }

    double computeMaxMinAreaWidth(List<Node> children, Callback<Node, Insets> margins, double height, boolean fillHeight) {
        return getMaxAreaWidth(children, margins, new double[] { height }, fillHeight, true);
    }

    double computeMaxMinAreaWidth(List<Node> children, Callback<Node, Insets> childMargins, double childHeights[], boolean fillHeight) {
        return getMaxAreaWidth(children, childMargins, childHeights, fillHeight, true);
    }

    /* Max of children's minimum area heights */

    double computeMaxMinAreaHeight(List<Node>children, Callback<Node, Insets> margins, VPos valignment) {
        return getMaxAreaHeight(children, margins, null, valignment, true);
    }

    double computeMaxMinAreaHeight(List<Node>children, Callback<Node, Insets> margins, VPos valignment, double width) {
        return getMaxAreaHeight(children, margins, new double[] { width }, valignment, true);
    }

    double computeMaxMinAreaHeight(List<Node>children, Callback<Node, Insets> childMargins, double childWidths[], VPos valignment) {
        return getMaxAreaHeight(children, childMargins, childWidths, valignment, true);
    }

    /* Max of children's pref area widths */

    double computeMaxPrefAreaWidth(List<Node>children, Callback<Node, Insets> margins) {
        return getMaxAreaWidth(children, margins, new double[] { -1 }, false, false);
    }

    double computeMaxPrefAreaWidth(List<Node>children, Callback<Node, Insets> margins, double height,
            boolean fillHeight) {
        return getMaxAreaWidth(children, margins, new double[] { height }, fillHeight, false);
    }

    double computeMaxPrefAreaWidth(List<Node>children, Callback<Node, Insets> childMargins,
            double childHeights[], boolean fillHeight) {
        return getMaxAreaWidth(children, childMargins, childHeights, fillHeight, false);
    }

    /* Max of children's pref area heights */

    double computeMaxPrefAreaHeight(List<Node>children, Callback<Node, Insets> margins, VPos valignment) {
        return getMaxAreaHeight(children, margins, null, valignment, false);
    }

    double computeMaxPrefAreaHeight(List<Node>children, Callback<Node, Insets> margins, double width, VPos valignment) {
        return getMaxAreaHeight(children, margins, new double[] { width }, valignment, false);
    }

    double computeMaxPrefAreaHeight(List<Node>children, Callback<Node, Insets> childMargins, double childWidths[], VPos valignment) {
        return getMaxAreaHeight(children, childMargins, childWidths, valignment, false);
    }

    /**
     * Returns the size of a Node that should be placed in an area of the specified size,
     * bounded in it's min/max size, respecting bias.
     *
     * @param node the node
     * @param areaWidth the width of the bounding area where the node is going to be placed
     * @param areaHeight the height of the bounding area where the node is going to be placed
     * @param fillWidth if Node should try to fill the area width
     * @param fillHeight if Node should try to fill the area height
     * @param result Vec2d object for the result or null if new one should be created
     * @return Vec2d object with width(x parameter) and height (y parameter)
     */
    static Vec2d boundedNodeSizeWithBias(Node node, double areaWidth, double areaHeight,
            boolean fillWidth, boolean fillHeight, Vec2d result) {
        if (result == null) {
            result = new Vec2d();
        }

        Orientation bias = node.getContentBias();

        double childWidth = 0;
        double childHeight = 0;

        if (bias == null) {
            childWidth = boundedSize(
                    node.minWidth(-1), fillWidth ? areaWidth
                    : Math.min(areaWidth, node.prefWidth(-1)),
                    node.maxWidth(-1));
            childHeight = boundedSize(
                    node.minHeight(-1), fillHeight ? areaHeight
                    : Math.min(areaHeight, node.prefHeight(-1)),
                    node.maxHeight(-1));

        } else if (bias == Orientation.HORIZONTAL) {
            childWidth = boundedSize(
                    node.minWidth(-1), fillWidth ? areaWidth
                    : Math.min(areaWidth, node.prefWidth(-1)),
                    node.maxWidth(-1));
            childHeight = boundedSize(
                    node.minHeight(childWidth), fillHeight ? areaHeight
                    : Math.min(areaHeight, node.prefHeight(childWidth)),
                    node.maxHeight(childWidth));

        } else { // bias == VERTICAL
            childHeight = boundedSize(
                    node.minHeight(-1), fillHeight ? areaHeight
                    : Math.min(areaHeight, node.prefHeight(-1)),
                    node.maxHeight(-1));
            childWidth = boundedSize(
                    node.minWidth(childHeight), fillWidth ? areaWidth
                    : Math.min(areaWidth, node.prefWidth(childHeight)),
                    node.maxWidth(childHeight));
        }

        result.set(childWidth, childHeight);
        return result;
    }

    /* utility method for computing the max of children's min or pref heights, taking into account baseline alignment */
    private double getMaxAreaHeight(List<Node> children, Callback<Node,Insets> childMargins,  double childWidths[], VPos valignment, boolean minimum) {
        final double singleChildWidth = childWidths == null ? -1 : childWidths.length == 1 ? childWidths[0] : Double.NaN;
        if (valignment == VPos.BASELINE) {
            double maxAbove = 0;
            double maxBelow = 0;
            for (int i = 0, maxPos = children.size(); i < maxPos; i++) {
                final Node child = children.get(i);
                final double childWidth = Double.isNaN(singleChildWidth) ? childWidths[i] : singleChildWidth;
                Insets margin = childMargins.call(child);
                final double top = margin != null? snapSpaceY(margin.getTop()) : 0;
                final double bottom = margin != null? snapSpaceY(margin.getBottom()) : 0;
                final double baseline = child.getBaselineOffset();

                final double childHeight = minimum? snapSizeY(child.minHeight(childWidth)) : snapSizeY(child.prefHeight(childWidth));
                if (baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                    maxAbove = Math.max(maxAbove, childHeight + top);
                } else {
                    maxAbove = Math.max(maxAbove, baseline + top);
                    maxBelow = Math.max(maxBelow,
                            snapSpaceY(minimum?snapSizeY(child.minHeight(childWidth)) : snapSizeY(child.prefHeight(childWidth))) -
                            baseline + bottom);
                }
            }
            return maxAbove + maxBelow; //remind(aim): ceil this value?
        } else {
            double max = 0;
            for (int i = 0, maxPos = children.size(); i < maxPos; i++) {
                final Node child = children.get(i);
                Insets margin = childMargins.call(child);
                final double childWidth = Double.isNaN(singleChildWidth) ? childWidths[i] : singleChildWidth;
                max = Math.max(max, minimum?
                    computeChildMinAreaHeight(child, -1, margin, childWidth) :
                        computeChildPrefAreaHeight(child, -1, margin, childWidth));
            }
            return max;
        }
    }

    /* utility method for computing the max of children's min or pref width, horizontal alignment is ignored for now */
    private double getMaxAreaWidth(List<javafx.scene.Node> children,
            Callback<Node, Insets> childMargins, double childHeights[], boolean fillHeight, boolean minimum) {
        final double singleChildHeight = childHeights == null ? -1 : childHeights.length == 1 ? childHeights[0] : Double.NaN;

        double max = 0;
        for (int i = 0, maxPos = children.size(); i < maxPos; i++) {
            final Node child = children.get(i);
            final Insets margin = childMargins.call(child);
            final double childHeight = Double.isNaN(singleChildHeight) ? childHeights[i] : singleChildHeight;
            max = Math.max(max, minimum?
                computeChildMinAreaWidth(children.get(i), -1, margin, childHeight, fillHeight) :
                    computeChildPrefAreaWidth(child, -1, margin, childHeight, fillHeight));
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
     * @param isSnapToPixel whether to snap size and position to pixels
     *
     * @since JavaFX 8.0
     */
    public static void positionInArea(Node child, double areaX, double areaY, double areaWidth, double areaHeight,
                               double areaBaselineOffset, Insets margin, HPos halignment, VPos valignment, boolean isSnapToPixel) {
        Insets childMargin = margin != null? margin : Insets.EMPTY;
        double snapScaleX = isSnapToPixel ? getSnapScaleX(child) : 1.0;
        double snapScaleY = isSnapToPixel ? getSnapScaleY(child) : 1.0;

        position(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset,
                snapSpace(childMargin.getTop(), isSnapToPixel, snapScaleY),
                snapSpace(childMargin.getRight(), isSnapToPixel, snapScaleX),
                snapSpace(childMargin.getBottom(), isSnapToPixel, snapScaleY),
                snapSpace(childMargin.getLeft(), isSnapToPixel, snapScaleX),
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

    /**
     * Utility method which lays out the child within an area of it's
     * parent defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
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
     * @param isSnapToPixel whether to snap size and position to pixels
     * @since JavaFX 8.0
     */
    public static void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               Insets margin, boolean fillWidth, boolean fillHeight,
                               HPos halignment, VPos valignment, boolean isSnapToPixel) {

        Insets childMargin = margin != null ? margin : Insets.EMPTY;
        double snapScaleX = isSnapToPixel ? getSnapScaleX(child) : 1.0;
        double snapScaleY = isSnapToPixel ? getSnapScaleY(child) : 1.0;

        double top = snapSpace(childMargin.getTop(), isSnapToPixel, snapScaleY);
        double bottom = snapSpace(childMargin.getBottom(), isSnapToPixel, snapScaleY);
        double left = snapSpace(childMargin.getLeft(), isSnapToPixel, snapScaleX);
        double right = snapSpace(childMargin.getRight(), isSnapToPixel, snapScaleX);

        if (valignment == VPos.BASELINE) {
            double bo = child.getBaselineOffset();
            if (bo == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                if (child.isResizable()) {
                    // Everything below the baseline is like an "inset". The Node with BASELINE_OFFSET_SAME_AS_HEIGHT cannot
                    // be resized to this area
                    bottom += snapSpace(areaHeight - areaBaselineOffset, isSnapToPixel, snapScaleY);
                } else {
                    top = snapSpace(areaBaselineOffset - child.getLayoutBounds().getHeight(), isSnapToPixel, snapScaleY);
                }
            } else {
                top = snapSpace(areaBaselineOffset - bo, isSnapToPixel, snapScaleY);
            }
        }


        if (child.isResizable()) {
            Vec2d size = boundedNodeSizeWithBias(child, areaWidth - left - right, areaHeight - top - bottom,
                    fillWidth, fillHeight, TEMP_VEC2D);
            child.resize(snapSize(size.x, isSnapToPixel, snapScaleX),
                         snapSize(size.y, isSnapToPixel, snapScaleX));
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
        final double yoffset;
        if (vpos == VPos.BASELINE) {
            double bo = child.getBaselineOffset();
            if (bo == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                // We already know the layout bounds at this stage, so we can use them
                yoffset = areaBaselineOffset - child.getLayoutBounds().getHeight();
            } else {
                yoffset = areaBaselineOffset - bo;
            }
        } else {
            yoffset = topMargin + computeYOffset(areaHeight - topMargin - bottomMargin,
                                         child.getLayoutBounds().getHeight(), vpos);
        }
        double x = areaX + xoffset;
        double y = areaY + yoffset;
        if (isSnapToPixel) {
            x = snapPosition(x, true, getSnapScaleX(child));
            y = snapPosition(y, true, getSnapScaleY(child));
        }

        child.relocate(x,y);
    }

     /**************************************************************************
     *                                                                         *
     * PG Implementation                                                       *
     *                                                                         *
     **************************************************************************/

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        // TODO I think we have a bug, where if you create a Region with an Image that hasn't
        // been loaded, we have no listeners on that image so as to cause a pulse & repaint
        // to happen once the image is loaded. We just assume the image has been loaded
        // (since when the image is created using new Image(url) or CSS it happens eagerly).
        if (_shape != null) NodeHelper.syncPeer(_shape);
        NGRegion pg = NodeHelper.getPeer(this);

        if (!cornersValid) {
            validateCorners();
        }

        final boolean sizeChanged = NodeHelper.isDirty(this, DirtyBits.NODE_GEOMETRY);
        if (sizeChanged) {
            pg.setSize((float)getWidth(), (float)getHeight());
        }

        // NOTE: The order here is very important. There is logic in NGRegion which determines
        // whether we can cache an image representing this region, and for this to work correctly,
        // the shape must be specified before the background which is before the border.
        final boolean shapeChanged = NodeHelper.isDirty(this, DirtyBits.REGION_SHAPE);
        if (shapeChanged) {
            pg.updateShape(_shape, isScaleShape(), isCenterShape(), isCacheShape());
        }

        // The normalized corners can always be updated since they require no
        // processing at the NG layer.
        pg.updateFillCorners(normalizedFillCorners);
        final boolean backgroundChanged = NodeHelper.isDirty(this, DirtyBits.SHAPE_FILL);
        final Background bg = getBackground();
        if (backgroundChanged) {
            pg.updateBackground(bg);
        }

        // This will be true if an image that makes up the background or border of this
        // region has changed, such that we need to redraw the region.
        if (NodeHelper.isDirty(this, DirtyBits.NODE_CONTENTS)) {
            pg.imagesUpdated();
        }

        // The normalized corners can always be updated since they require no
        // processing at the NG layer.
        pg.updateStrokeCorners(normalizedStrokeCorners);
        if (NodeHelper.isDirty(this, DirtyBits.SHAPE_STROKE)) {
            pg.updateBorder(getBorder());
        }

        // TODO given the note above, this *must* be called when an image which makes up the
        // background images and border images changes (is loaded) if it was being loaded asynchronously
        // Also note, one day we can add support for automatic opaque insets determination for border images.
        // However right now it is impractical because the image pixel format is almost undoubtedly going
        // to have alpha, and so without inspecting the source image's actual pixels for the filled center
        // we can't automatically determine whether the interior is filled.
        if (sizeChanged || backgroundChanged || shapeChanged) {
            // These are the opaque insets, as specified by the developer in code or CSS. If null,
            // then we must compute the opaque insets. If not null, then we will still compute the
            // opaque insets and combine them with these insets, as appropriate. We do ignore these
            // developer specified insets in cases where we know without a doubt that the developer
            // gave us bad data.
            final Insets i = getOpaqueInsets();

            // If the background is determined by a shape, then we don't attempt to calculate the
            // opaque insets. If the developer specified opaque insets, we will use them, otherwise
            // we will make sure the opaque insets are cleared
            if (_shape != null) {
                if (i != null) {
                    pg.setOpaqueInsets((float) i.getTop(), (float) i.getRight(),
                                       (float) i.getBottom(), (float) i.getLeft());
                } else {
                    pg.setOpaqueInsets(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
                }
            } else {
                // This is a rectangle (not shape) region. The opaque insets must be calculated,
                // even if the developer has supplied their own opaque insets. The first (and cheapest)
                // check is whether the region has any backgrounds at all. If not, then
                // we will ignore the developer supplied insets because they are clearly wrong.
                if (bg == null || bg.isEmpty()) {
                    pg.setOpaqueInsets(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
                } else {
                    // There is a background, so it is conceivable that there are
                    // opaque insets. From this point on, we have to honor the developer's supplied
                    // insets, only expanding them if we know for certain the opaque insets are
                    // bigger than what was supplied by the developer. Start by defining our initial
                    // values for top, right, bottom, and left. If the developer supplied us
                    // insets, use those. Otherwise initialize to NaN. Note that the developer may
                    // also have given us NaN values (so we'd have to check for these anyway). We use
                    // NaN to mean "not defined".
                    final double[] trbl = new double[4];
                    bg.computeOpaqueInsets(getWidth(), getHeight(), trbl);

                    if (i != null) {
                        trbl[0] = Double.isNaN(trbl[0]) ? i.getTop() : Double.isNaN(i.getTop()) ? trbl[0] : Math.min(trbl[0], i.getTop());
                        trbl[1] = Double.isNaN(trbl[1]) ? i.getRight() : Double.isNaN(i.getRight()) ? trbl[1] : Math.min(trbl[1], i.getRight());
                        trbl[2] = Double.isNaN(trbl[2]) ? i.getBottom() : Double.isNaN(i.getBottom()) ? trbl[2] : Math.min(trbl[2], i.getBottom());
                        trbl[3] = Double.isNaN(trbl[3]) ? i.getLeft() : Double.isNaN(i.getLeft()) ? trbl[3] : Math.min(trbl[3], i.getLeft());
                    }

                    // Now set the insets onto the peer. Passing NaN here is perfectly
                    // acceptable (even encouraged, to mean "unknown" or "disabled").
                    pg.setOpaqueInsets((float) trbl[0], (float) trbl[1], (float) trbl[2], (float) trbl[3]);
                }
            }
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGRegion();
    }

    /**
     * Transform x, y in local Region coordinates to local coordinates of scaled/centered shape and
     * check if the shape contains the coordinates.
     * The transformations here are basically an inversion of transformations being done in NGShape#resizeShape.
     */
    private boolean shapeContains(com.sun.javafx.geom.Shape shape,
            final double x, final double y,
            double topOffset, double rightOffset, double bottomOffset, double leftOffset) {
        double resX = x;
        double resY = y;
        // The bounds of the shape, before any centering / scaling takes place
        final RectBounds bounds = shape.getBounds();
        if (isScaleShape()) {
            // Modify the transform to scale the shape so that it will fit
            // within the insets.
            resX -= leftOffset;
            resY -= topOffset;

            //denominator represents the width and height of the box within which the new shape must fit.
            resX *= bounds.getWidth() / (getWidth() - leftOffset - rightOffset);
            resY *= bounds.getHeight() / (getHeight() - topOffset - bottomOffset);

            // If we also need to center it, we need to adjust the transform so as to place
            // the shape in the center of the bounds
            if (isCenterShape()) {
                resX += bounds.getMinX();
                resY += bounds.getMinY();
            }
        } else if (isCenterShape()) {
            // We are only centering. In this case, what we want is for the
            // original shape to be centered. If there are offsets (insets)
            // then we must pre-scale about the center to account for it.

            double boundsWidth = bounds.getWidth();
            double boundsHeight = bounds.getHeight();

            double scaleFactorX = boundsWidth / (boundsWidth - leftOffset - rightOffset);
            double scaleFactorY = boundsHeight / (boundsHeight - topOffset - bottomOffset);

            //This is equivalent to:
            // translate(bounds.getMinX(), bounds.getMinY())
            // scale(scaleFactorX, scaleFactorY)
            // translate(-bounds.getMinX(), -bounds.getMinY())
            // translate(-leftOffset - (getWidth() - boundsWidth)/2 + bounds.getMinX(),
            //                            -topOffset - (getHeight() - boundsHeight)/2 + bounds.getMinY());
            // which is an inversion of an transformation done to the shape
            // This gives us
            //
            //resX = resX * scaleFactorX - scaleFactorX * bounds.getMinX() - scaleFactorX * (leftOffset + (getWidth() - boundsWidth) / 2 - bounds.getMinX()) + bounds.getMinX();
            //resY = resY * scaleFactorY - scaleFactorY * bounds.getMinY() - scaleFactorY * (topOffset + (getHeight() - boundsHeight) / 2 - bounds.getMinY()) + bounds.getMinY();
            //
            // which can further reduced to

            resX = scaleFactorX * (resX -(leftOffset + (getWidth() - boundsWidth) / 2)) + bounds.getMinX();
            resY = scaleFactorY * (resY -(topOffset + (getHeight() - boundsHeight) / 2)) + bounds.getMinY();

        } else if (topOffset != 0 || rightOffset != 0 || bottomOffset != 0 || leftOffset != 0) {
            // We are neither centering nor scaling, but we still have to resize the
            // shape because we have to fit within the bounds defined by the offsets
            double scaleFactorX = bounds.getWidth() / (bounds.getWidth() - leftOffset - rightOffset);
            double scaleFactorY = bounds.getHeight() / (bounds.getHeight() - topOffset - bottomOffset);

            // This is equivalent to:
            // translate(bounds.getMinX(), bounds.getMinY())
            // scale(scaleFactorX, scaleFactorY)
            // translate(-bounds.getMinX(), -bounds.getMinY())
            // translate(-leftOffset, -topOffset)
            //
            // which is an inversion of an transformation done to the shape
            // This gives us
            //
            //resX = resX * scaleFactorX - scaleFactorX * leftOffset - scaleFactorX * bounds.getMinX() + bounds.getMinX();
            //resY = resY * scaleFactorY - scaleFactorY * topOffset - scaleFactorY * bounds.getMinY() + bounds.getMinY();
            //
            // which can be further reduceD to
            resX = scaleFactorX * (resX - leftOffset - bounds.getMinX()) + bounds.getMinX();
            resY = scaleFactorY * (resY - topOffset - bounds.getMinY()) + bounds.getMinY();

        }
        return shape.contains((float)resX, (float)resY);
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        // NOTE: This method only gets called if a quick check of bounds has already
        // occurred, so there is no need to test against bound again. We know that the
        // point (localX, localY) falls within the bounds of this node, now we need
        // to determine if it falls within the geometry of this node.
        // Also note that because Region defaults pickOnBounds to true, this code is
        // not usually executed. It will only be executed if pickOnBounds is set to false.

        final double x2 = getWidth();
        final double y2 = getHeight();

        final Background background = getBackground();
        // First check the shape. Shape could be impacted by scaleShape & positionShape properties.
        if (_shape != null) {
            if (background != null && !background.getFills().isEmpty()) {
                final List<BackgroundFill> fills = background.getFills();
                double topO = Double.MAX_VALUE;
                double leftO = Double.MAX_VALUE;
                double bottomO = Double.MAX_VALUE;
                double rightO = Double.MAX_VALUE;
                for (int i = 0, max = fills.size(); i < max; i++) {
                    BackgroundFill bf = fills.get(0);
                    topO = Math.min(topO, bf.getInsets().getTop());
                    leftO = Math.min(leftO, bf.getInsets().getLeft());
                    bottomO = Math.min(bottomO, bf.getInsets().getBottom());
                    rightO = Math.min(rightO, bf.getInsets().getRight());
                }
                return shapeContains(ShapeHelper.configShape(_shape), localX, localY, topO, leftO, bottomO, rightO);
            }
            return false;
        }

        // OK, there was no background shape, so I'm going to work on the principle of
        // nested rounded rectangles. We'll start by checking the backgrounds. The
        // first background which passes the test is good enough for us!
        if (background != null) {
            final List<BackgroundFill> fills = background.getFills();
            for (int i = 0, max = fills.size(); i < max; i++) {
                final BackgroundFill bgFill = fills.get(i);
                if (contains(localX, localY, 0, 0, x2, y2, bgFill.getInsets(), getNormalizedFillCorner(i))) {
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
                             getNormalizedStrokeCorner(i))) {
                    return true;
                }
            }

            // Check the image borders. We treat the image border as though it is opaque.
            final List<BorderImage> images = border.getImages();
            for (int i = 0, max = images.size(); i < max; i++) {
                final BorderImage borderImage = images.get(i);
                if (contains(localX, localY, 0, 0, x2, y2, borderImage.getWidths(), borderImage.isFilled(),
                             borderImage.getInsets(), CornerRadii.EMPTY)) {
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
                             final Insets insets, final CornerRadii rad) {
        if (filled) {
            if (contains(px, py, x1, y1, x2, y2, insets, rad)) {
                return true;
            }
        } else {
            boolean insideOuterEdge = contains(px, py, x1, y1, x2, y2, insets, rad);
            if (insideOuterEdge) {
                boolean outsideInnerEdge = !contains(px, py,
                    x1 + (widths.isLeftAsPercentage() ? getWidth() * widths.getLeft() : widths.getLeft()),
                    y1 + (widths.isTopAsPercentage() ? getHeight() * widths.getTop() : widths.getTop()),
                    x2 - (widths.isRightAsPercentage() ? getWidth() * widths.getRight() : widths.getRight()),
                    y2 - (widths.isBottomAsPercentage() ? getHeight() * widths.getBottom() : widths.getBottom()),
                    insets, rad);
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
                             final Insets insets, CornerRadii rad) {
        // These four values are the x0, y0, x1, y1 bounding box after
        // having taken into account the insets of this particular
        // background fill.
        final double rrx0 = x1 + insets.getLeft();
        final double rry0 = y1 + insets.getTop();
        final double rrx1 = x2 - insets.getRight();
        final double rry1 = y2 - insets.getBottom();

//        assert rad.hasPercentBasedRadii == false;

        // Check for trivial rejection - point is inside bounding rectangle
        if (px >= rrx0 && py >= rry0 && px <= rrx1 && py <= rry1) {
            // The point was within the index bounding box. Now we need to analyze the
            // corner radii to see if the point lies within the corners or not. If the
            // point is within a corner then we reject this one.
            final double tlhr = rad.getTopLeftHorizontalRadius();
            if (rad.isUniform() && tlhr == 0) {
                // This is a simple square! Since we know the point is already within
                // the insets of this fill, we can simply return true.
                return true;
            } else {
                final double tlvr = rad.getTopLeftVerticalRadius();
                final double trhr = rad.getTopRightHorizontalRadius();
                final double trvr = rad.getTopRightVerticalRadius();
                final double blhr = rad.getBottomLeftHorizontalRadius();
                final double blvr = rad.getBottomLeftVerticalRadius();
                final double brhr = rad.getBottomRightHorizontalRadius();
                final double brvr = rad.getBottomRightVerticalRadius();

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

    /*
     * The normalized corner radii are unmodifiable List objects shared between
     * the NG layer and the FX layer.  As cached shadow copies of the objects
     * in the BackgroundFill and BorderStroke objects they should be considered
     * read-only and will only be updated by replacing the original objects
     * when validation is needed.
     */
    private boolean cornersValid; // = false
    private List<CornerRadii> normalizedFillCorners; // = null
    private List<CornerRadii> normalizedStrokeCorners; // = null

    /**
     * Returns the normalized absolute radii for the indicated BackgroundFill,
     * taking the current size of the region into account to eliminate any
     * percentage-based measurements and to scale the radii to prevent
     * overflowing the width or height.
     *
     * @param i the index of the BackgroundFill whose radii will be normalized.
     * @return the normalized (non-percentage, non-overflowing) radii
     */
    private CornerRadii getNormalizedFillCorner(int i) {
        if (!cornersValid) {
            validateCorners();
        }
        return (normalizedFillCorners == null
                ? getBackground().getFills().get(i).getRadii()
                : normalizedFillCorners.get(i));
    }

    /**
     * Returns the normalized absolute radii for the indicated BorderStroke,
     * taking the current size of the region into account to eliminate any
     * percentage-based measurements and to scale the radii to prevent
     * overflowing the width or height.
     *
     * @param i the index of the BorderStroke whose radii will be normalized.
     * @return the normalized (non-percentage, non-overflowing) radii
     */
    private CornerRadii getNormalizedStrokeCorner(int i) {
        if (!cornersValid) {
            validateCorners();
        }
        return (normalizedStrokeCorners == null
                ? getBorder().getStrokes().get(i).getRadii()
                : normalizedStrokeCorners.get(i));
    }

    /**
     * This method validates all CornerRadii objects in both the set of
     * BackgroundFills and BorderStrokes and saves the normalized values
     * into the private fields above.
     */
    private void validateCorners() {
        final double width = getWidth();
        final double height = getHeight();
        List<CornerRadii> newFillCorners = null;
        List<CornerRadii> newStrokeCorners = null;
        final Background background = getBackground();
        final List<BackgroundFill> fills = background == null ? Collections.EMPTY_LIST : background.getFills();
        for (int i = 0; i < fills.size(); i++) {
            final BackgroundFill fill = fills.get(i);
            final CornerRadii origRadii = fill.getRadii();
            final Insets origInsets = fill.getInsets();
            final CornerRadii newRadii = normalize(origRadii, origInsets, width, height);
            if (origRadii != newRadii) {
                if (newFillCorners == null) {
                    newFillCorners = Arrays.asList(new CornerRadii[fills.size()]);
                }
                newFillCorners.set(i, newRadii);
            }
        }
        final Border border = getBorder();
        final List<BorderStroke> strokes = (border == null ? Collections.EMPTY_LIST : border.getStrokes());
        for (int i = 0; i < strokes.size(); i++) {
            final BorderStroke stroke = strokes.get(i);
            final CornerRadii origRadii = stroke.getRadii();
            final Insets origInsets = stroke.getInsets();
            final CornerRadii newRadii = normalize(origRadii, origInsets, width, height);
            if (origRadii != newRadii) {
                if (newStrokeCorners == null) {
                    newStrokeCorners = Arrays.asList(new CornerRadii[strokes.size()]);
                }
                newStrokeCorners.set(i, newRadii);
            }
        }
        if (newFillCorners != null) {
            for (int i = 0; i < fills.size(); i++) {
                if (newFillCorners.get(i) == null) {
                    newFillCorners.set(i, fills.get(i).getRadii());
                }
            }
            newFillCorners = Collections.unmodifiableList(newFillCorners);
        }
        if (newStrokeCorners != null) {
            for (int i = 0; i < strokes.size(); i++) {
                if (newStrokeCorners.get(i) == null) {
                    newStrokeCorners.set(i, strokes.get(i).getRadii());
                }
            }
            newStrokeCorners = Collections.unmodifiableList(newStrokeCorners);
        }
        normalizedFillCorners = newFillCorners;
        normalizedStrokeCorners = newStrokeCorners;
        cornersValid = true;
    }

    /**
     * Return a version of the radii that is not percentage based and is scaled to
     * fit the indicated inset rectangle without overflow.
     * This method may return the original CornerRadii if none of the radii
     * values in the given object are percentages or require scaling.
     *
     * @param radii    The radii.
     * @param insets   The insets for the associated background or stroke.
     * @param width    The width of the region before insets are applied.
     * @param height   The height of the region before insets are applied.
     * @return Normalized radii.
     */
    private static CornerRadii normalize(CornerRadii radii, Insets insets, double width, double height) {
        width  -= insets.getLeft() + insets.getRight();
        height -= insets.getTop() + insets.getBottom();
        if (width <= 0 || height <= 0) return CornerRadii.EMPTY;
        double tlvr = radii.getTopLeftVerticalRadius();
        double tlhr = radii.getTopLeftHorizontalRadius();
        double trvr = radii.getTopRightVerticalRadius();
        double trhr = radii.getTopRightHorizontalRadius();
        double brvr = radii.getBottomRightVerticalRadius();
        double brhr = radii.getBottomRightHorizontalRadius();
        double blvr = radii.getBottomLeftVerticalRadius();
        double blhr = radii.getBottomLeftHorizontalRadius();
        if (radii.hasPercentBasedRadii) {
            if (radii.isTopLeftVerticalRadiusAsPercentage())       tlvr *= height;
            if (radii.isTopLeftHorizontalRadiusAsPercentage())     tlhr *= width;
            if (radii.isTopRightVerticalRadiusAsPercentage())      trvr *= height;
            if (radii.isTopRightHorizontalRadiusAsPercentage())    trhr *= width;
            if (radii.isBottomRightVerticalRadiusAsPercentage())   brvr *= height;
            if (radii.isBottomRightHorizontalRadiusAsPercentage()) brhr *= width;
            if (radii.isBottomLeftVerticalRadiusAsPercentage())    blvr *= height;
            if (radii.isBottomLeftHorizontalRadiusAsPercentage())  blhr *= width;
        }
        double scale = 1.0;
        if (tlhr + trhr > width)  { scale = Math.min(scale, width  / (tlhr + trhr)); }
        if (blhr + brhr > width)  { scale = Math.min(scale, width  / (blhr + brhr)); }
        if (tlvr + blvr > height) { scale = Math.min(scale, height / (tlvr + blvr)); }
        if (trvr + brvr > height) { scale = Math.min(scale, height / (trvr + brvr)); }
        if (scale < 1.0) {
            tlvr *= scale;  tlhr *= scale;
            trvr *= scale;  trhr *= scale;
            brvr *= scale;  brhr *= scale;
            blvr *= scale;  blhr *= scale;
        }
        if (radii.hasPercentBasedRadii || scale < 1.0) {
            return new CornerRadii(tlhr,  tlvr,  trvr,  trhr,  brhr,  brvr,  blvr,  blhr,
                                   false, false, false, false, false, false, false, false);
        }
        return radii;
    }

    /**
     * Some skins relying on this
     *
     * Note: This method MUST only be called via its accessor method.
     */
    private void doPickNodeLocal(PickRay pickRay, PickResultChooser result) {
         double boundsDistance = NodeHelper.intersectsBounds(this, pickRay);

        if (!Double.isNaN(boundsDistance) && ParentHelper.pickChildrenNode(this, pickRay, result)) {
            NodeHelper.intersects(this, pickRay, result);
        }
    }

    private Bounds boundingBox;

    /**
     * The layout bounds of this region: {@code 0, 0  width x height}
     */
    private Bounds doComputeLayoutBounds() {
        if (boundingBox == null) {
            // we reuse the bounding box if the width and height haven't changed.
            boundingBox = new BoundingBox(0, 0, 0, getWidth(), getHeight(), 0);
        }
        return boundingBox;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doNotifyLayoutBoundsChanged() {
        // override Node's default behavior of having a geometric bounds change
        // trigger a change in layoutBounds. For Resizable nodes, layoutBounds
        // is unrelated to geometric bounds.
    }

    private BaseBounds computeShapeBounds(BaseBounds bounds)
    {
        com.sun.javafx.geom.Shape s = ShapeHelper.configShape(_shape);

        float[] bbox = {
                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
        };

        Background bg = getBackground();
        if (bg != null) {
            final RectBounds sBounds = s.getBounds();
            final Insets bgOutsets = bg.getOutsets();
            bbox[0] = sBounds.getMinX() - (float) bgOutsets.getLeft();
            bbox[1] = sBounds.getMinY() - (float) bgOutsets.getTop();
            bbox[2] = sBounds.getMaxX() + (float) bgOutsets.getBottom();
            bbox[3] = sBounds.getMaxY() + (float) bgOutsets.getRight();
        }

        final Border b = getBorder();
        if (b != null && b.getStrokes().size() > 0) {
            for (BorderStroke bs : b.getStrokes()) {
                // This order of border strokes is used in NGRegion.renderAsShape/setBorderStyle
                BorderStrokeStyle bss = bs.getTopStyle() != null ? bs.getTopStyle() :
                        bs.getLeftStyle() != null ? bs.getLeftStyle() :
                                bs.getBottomStyle() != null ? bs.getBottomStyle() :
                                        bs.getRightStyle() != null ? bs.getRightStyle() : null;

                if (bss == null || bss == BorderStrokeStyle.NONE) {
                    continue;
                }

                final StrokeType type = bss.getType();
                double sw = Math.max(bs.getWidths().top, 0d);
                StrokeLineCap cap = bss.getLineCap();
                StrokeLineJoin join = bss.getLineJoin();
                float miterlimit = (float) Math.max(bss.getMiterLimit(), 1d);
                Toolkit.getToolkit().accumulateStrokeBounds(
                        s,
                        bbox, type, sw,
                        cap, join, miterlimit, BaseTransform.IDENTITY_TRANSFORM);

            }
        }

        if (bbox[2] < bbox[0] || bbox[3] < bbox[1]) {
            return bounds.makeEmpty();
        }

        return bounds.deriveWithNewBounds(bbox[0], bbox[1], 0.0f,
                bbox[2], bbox[3], 0.0f);
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
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
            // We will hijack the bounds here temporarily just to compute the shape bounds
            final BaseBounds shapeBounds = computeShapeBounds(bounds);
            final double shapeWidth = shapeBounds.getWidth();
            final double shapeHeight = shapeBounds.getHeight();
            if (isCenterShape()) {
                bx1 = (bx2 - shapeWidth) / 2;
                by1 = (by2 - shapeHeight) / 2;
                bx2 = bx1 + shapeWidth;
                by2 = by1 + shapeHeight;
            } else {
                bx1 = shapeBounds.getMinX();
                by1 = shapeBounds.getMinY();
                bx2 = shapeBounds.getMaxX();
                by2 = shapeBounds.getMaxY();
            }
        } else {
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
        }
        // NOTE: Okay to call RegionHelper.superComputeGeomBounds with tx even in the 3D case
        // since Parent's computeGeomBounds does handle 3D correctly.
        BaseBounds cb = RegionHelper.superComputeGeomBounds(this, bounds, tx);
        /*
         * This is a work around for RT-7680. Parent returns invalid bounds from
         * computeGeomBoundsImpl when it has no children or if all its children
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
     * An implementation may specify its own user-agent styles for this Region, and its children,
     * by overriding this method. These styles are used in addition to whatever user-agent stylesheets
     * are in use. This provides a mechanism for third parties to introduce styles for custom controls.
     * <p>
     * The URL is a hierarchical URI of the form [scheme:][//authority][path]. If the URL
     * does not have a [scheme:] component, the URL is considered to be the [path] component only.
     * Any leading '/' character of the [path] is ignored and the [path] is treated as a path relative to
     * the root of the application's classpath.
     * </p>
     * <p>
     * Subclasses overriding this method should not assume any particular implementation approach as to
     * the number and frequency with which it is called. For this reason, attempting any kind of
     * dynamic implementation (i.e. returning different user agent stylesheet values) based on some
     * state change is highly discouraged, as there is no guarantee when, or even if, this method will
     * be called. Some JavaFX CSS implementations may choose to cache this response for an indefinite
     * period of time, and therefore there should be no expectation around when this method is called.
     * </p>
     *
     * <pre><code>
     *
     * package com.example.javafx.app;
     *
     * import javafx.application.Application;
     * import javafx.scene.Group;
     * import javafx.scene.Scene;
     * import javafx.stage.Stage;
     *
     * public class MyApp extends Application {
     *
     *     {@literal @}Override public void start(Stage stage) {
     *         Scene scene = new Scene(new Group());
     *         scene.getStylesheets().add("/com/example/javafx/app/mystyles.css");
     *         stage.setScene(scene);
     *         stage.show();
     *     }
     *
     *     public static void main(String[] args) {
     *         launch(args);
     *     }
     * }
     * </code></pre>
     * For additional information about using CSS with the scene graph,
     * see the <a href="../doc-files/cssref.html">CSS Reference Guide</a>.
     *
     * @return A string URL
     * @since JavaFX 8u40
     */
    public String getUserAgentStylesheet() {
        return null;
    }

    /*
     * Super-lazy instantiation pattern from Bill Pugh.
     */
     private static class StyleableProperties {
         private static final CssMetaData<Region,Insets> PADDING =
             new CssMetaData<Region,Insets>("-fx-padding",
                 InsetsConverter.getInstance(), Insets.EMPTY) {

            @Override public boolean isSettable(Region node) {
                return node.padding == null || !node.padding.isBound();
            }

            @Override public StyleableProperty<Insets> getStyleableProperty(Region node) {
                return (StyleableProperty<Insets>)node.paddingProperty();
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
                         return (StyleableProperty<Insets>)node.opaqueInsetsProperty();
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
                return (StyleableProperty<Background>)node.background;
            }
         };

         private static final CssMetaData<Region,Border> BORDER =
             new CssMetaData<Region,Border>("-fx-region-border",
                     BorderConverter.getInstance(),
                     null,
                     false,
                     Border.getClassCssMetaData()) {

                 @Override public boolean isSettable(Region node) {
                     return !node.border.isBound();
                 }

                 @Override public StyleableProperty<Border> getStyleableProperty(Region node) {
                     return (StyleableProperty<Border>)node.border;
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
                return (StyleableProperty<Shape>)node.shapeProperty();
            }
         };

         private static final CssMetaData<Region, Boolean> SCALE_SHAPE =
             new CssMetaData<Region,Boolean>("-fx-scale-shape",
                 BooleanConverter.getInstance(), Boolean.TRUE){

            @Override public boolean isSettable(Region node) {
                return node.scaleShape == null || !node.scaleShape.isBound();
            }

            @Override public StyleableProperty<Boolean> getStyleableProperty(Region node) {
                return (StyleableProperty<Boolean>)node.scaleShapeProperty();
            }
        };

         private static final CssMetaData<Region,Boolean> POSITION_SHAPE =
             new CssMetaData<Region,Boolean>("-fx-position-shape",
                 BooleanConverter.getInstance(), Boolean.TRUE){

            @Override public boolean isSettable(Region node) {
                return node.centerShape == null || !node.centerShape.isBound();
            }

            @Override public StyleableProperty<Boolean> getStyleableProperty(Region node) {
                return (StyleableProperty<Boolean>)node.centerShapeProperty();
            }
        };

         private static final CssMetaData<Region,Boolean> CACHE_SHAPE =
             new CssMetaData<Region,Boolean>("-fx-cache-shape",
                 BooleanConverter.getInstance(), Boolean.TRUE){

            @Override public boolean isSettable(Region node) {
                return node.cacheShape == null || !node.cacheShape.isBound();
            }

            @Override public StyleableProperty<Boolean> getStyleableProperty(Region node) {
                return (StyleableProperty<Boolean>)node.cacheShapeProperty();
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
                return (StyleableProperty<Boolean>)node.snapToPixelProperty();
            }
        };

         private static final CssMetaData<Region, Number> MIN_HEIGHT =
             new CssMetaData<Region,Number>("-fx-min-height",
                 SizeConverter.getInstance(), USE_COMPUTED_SIZE){

            @Override public boolean isSettable(Region node) {
                return node.minHeight == null ||
                        !node.minHeight.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(Region node) {
                return (StyleableProperty<Number>)node.minHeightProperty();
            }
        };

         private static final CssMetaData<Region, Number> PREF_HEIGHT =
             new CssMetaData<Region,Number>("-fx-pref-height",
                 SizeConverter.getInstance(), USE_COMPUTED_SIZE){

            @Override public boolean isSettable(Region node) {
                return node.prefHeight == null ||
                        !node.prefHeight.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(Region node) {
                return (StyleableProperty<Number>)node.prefHeightProperty();
            }
        };

         private static final CssMetaData<Region, Number> MAX_HEIGHT =
             new CssMetaData<Region,Number>("-fx-max-height",
                 SizeConverter.getInstance(), USE_COMPUTED_SIZE){

            @Override public boolean isSettable(Region node) {
                return node.maxHeight == null ||
                        !node.maxHeight.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(Region node) {
                return (StyleableProperty<Number>)node.maxHeightProperty();
            }
        };

         private static final CssMetaData<Region, Number> MIN_WIDTH =
             new CssMetaData<Region,Number>("-fx-min-width",
                 SizeConverter.getInstance(), USE_COMPUTED_SIZE){

            @Override public boolean isSettable(Region node) {
                return node.minWidth == null ||
                        !node.minWidth.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(Region node) {
                return (StyleableProperty<Number>)node.minWidthProperty();
            }
        };

         private static final CssMetaData<Region, Number> PREF_WIDTH =
             new CssMetaData<Region,Number>("-fx-pref-width",
                 SizeConverter.getInstance(), USE_COMPUTED_SIZE){

            @Override public boolean isSettable(Region node) {
                return node.prefWidth == null ||
                        !node.prefWidth.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(Region node) {
                return (StyleableProperty<Number>)node.prefWidthProperty();
            }
        };

         private static final CssMetaData<Region, Number> MAX_WIDTH =
             new CssMetaData<Region,Number>("-fx-max-width",
                 SizeConverter.getInstance(), USE_COMPUTED_SIZE){

            @Override public boolean isSettable(Region node) {
                return node.maxWidth == null ||
                        !node.maxWidth.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(Region node) {
                return (StyleableProperty<Number>)node.maxWidthProperty();
            }
        };

         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Parent.getClassCssMetaData());
            styleables.add(PADDING);
            styleables.add(BACKGROUND);
            styleables.add(BORDER);
            styleables.add(OPAQUE_INSETS);
            styleables.add(SHAPE);
            styleables.add(SCALE_SHAPE);
            styleables.add(POSITION_SHAPE);
            styleables.add(SNAP_TO_PIXEL);
            styleables.add(MIN_WIDTH);
            styleables.add(PREF_WIDTH);
            styleables.add(MAX_WIDTH);
            styleables.add(MIN_HEIGHT);
            styleables.add(PREF_HEIGHT);
            styleables.add(MAX_HEIGHT);
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
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

}
