/*
 * Copyright (c) 2010, 2012, Oracle  and/or its affiliates. All rights reserved.
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
package javafx.scene.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import com.sun.javafx.charts.ChartLayoutAnimator;
import com.sun.javafx.charts.Legend;
import com.sun.javafx.css.StyleableBooleanProperty;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.CssMetaData;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import javafx.geometry.NodeOrientation;

/**
 * Base class for all charts. It has 3 parts the title, legend and chartContent. The chart content is populated by the
 * specific subclass of Chart.
 *
 */
public abstract class Chart extends Region {

    // -------------- PRIVATE FIELDS -----------------------------------------------------------------------------------

    private static final int MIN_WIDTH_TO_LEAVE_FOR_CHART_CONTENT = 200;
    private static final int MIN_HEIGHT_TO_LEAVE_FOR_CHART_CONTENT = 150;

    /** Title Label */
    private final Label titleLabel = new Label();
    /**
     * This is the Pane that Chart subclasses use to contain the chart content,
     * It is sized to be inside the chart area leaving space for the title and legend.
     */
    private final Pane chartContent = new Pane() {
        @Override protected void layoutChildren() {
            final double top = getInsets().getTop();
            final double left = getInsets().getLeft();
            final double bottom = getInsets().getBottom();
            final double right = getInsets().getRight();
            final double width = getWidth();
            final double height = getHeight();
            final double contentWidth = snapSize(width - (left + right));
            final double contentHeight = snapSize(height - (top + bottom));
            layoutChartChildren(snapPosition(top), snapPosition(left),contentWidth,contentHeight);
        }
        @Override public boolean isAutomaticallyMirrored() {
            return useChartContentMirroring;
        }
    };
    // Determines if chart content should be mirrored if node orientation is right-to-left.
    boolean useChartContentMirroring = true;
    
    /** Animator for animating stuff on the chart */
    private final ChartLayoutAnimator animator = new ChartLayoutAnimator(chartContent);

    // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

    /** The chart title */
    private StringProperty title = new StringPropertyBase() {
        @Override protected void invalidated() {
            titleLabel.setText(get());
        }

        @Override
        public Object getBean() {
            return Chart.this;
        }

        @Override
        public String getName() {
            return "title";
        }
    };
    public final String getTitle() { return title.get(); }
    public final void setTitle(String value) { title.set(value); }
    public final StringProperty titleProperty() { return title; }

    /**
     * The side of the chart where the title is displayed
     * @default Side.TOP
     */
    private ObjectProperty<Side> titleSide = new StyleableObjectProperty<Side>(Side.TOP) {
        @Override protected void invalidated() {
            requestLayout();
        }
        
        @Override
        public CssMetaData getCssMetaData() {
            return StyleableProperties.TITLE_SIDE;
        }

        @Override
        public Object getBean() {
            return Chart.this;
        }

        @Override
        public String getName() {
            return "titleSide";
        }
    };
    public final Side getTitleSide() { return titleSide.get(); }
    public final void setTitleSide(Side value) { titleSide.set(value); }
    public final ObjectProperty<Side> titleSideProperty() { return titleSide; }

    /**
     * The node to display as the Legend. Subclasses can set a node here to be displayed on a side as the legend. If
     * no legend is wanted then this can be set to null
     */
    private final ObjectProperty<Node> legend = new ObjectPropertyBase<Node>() {
        private Node old = null;
        @Override protected void invalidated() {
            Node newLegend = get();
            if (old != null) getChildren().remove(old);
            if (newLegend != null) {
                getChildren().add(newLegend);
                newLegend.setVisible(isLegendVisible());
            }
            old = newLegend;
        }

        @Override
        public Object getBean() {
            return Chart.this;
        }

        @Override
        public String getName() {
            return "legend";
        }
    };
    protected final Node getLegend() { return legend.getValue(); }
    protected final void setLegend(Node value) { legend.setValue(value); }
    protected final ObjectProperty<Node> legendProperty() { return legend; }

    /**
     * When true the chart will display a legend if the chart implementation supports a legend.
     */
    private final BooleanProperty legendVisible = new StyleableBooleanProperty(true) {
        @Override protected void invalidated() {
            requestLayout();
        }
            
        @Override
        public CssMetaData getCssMetaData() {
            return StyleableProperties.LEGEND_VISIBLE;
        }

        @Override
        public Object getBean() {
            return Chart.this;
        }

        @Override
        public String getName() {
            return "legendVisible";
        }
    };
    public final boolean isLegendVisible() { return legendVisible.getValue(); }
    public final void setLegendVisible(boolean value) { legendVisible.setValue(value); }
    public final BooleanProperty legendVisibleProperty() { return legendVisible; }

    /**
     * The side of the chart where the legend should be displayed
     *
     * @defaultValue Side.BOTTOM
     */
    private ObjectProperty<Side> legendSide = new StyleableObjectProperty<Side>(Side.BOTTOM) {
        @Override protected void invalidated() {
            final Side legendSide = get();
            final Node legend = getLegend();
            if(legend instanceof Legend) ((Legend)legend).setVertical(Side.LEFT.equals(legendSide) || Side.RIGHT.equals(legendSide));
            requestLayout();
        }
        
        @Override
        public CssMetaData getCssMetaData() {
            return StyleableProperties.LEGEND_SIDE;
        }

        @Override
        public Object getBean() {
            return Chart.this;
        }

        @Override
        public String getName() {
            return "legendSide";
        }
    };
    public final Side getLegendSide() { return legendSide.get(); }
    public final void setLegendSide(Side value) { legendSide.set(value); }
    public final ObjectProperty<Side> legendSideProperty() { return legendSide; }

    /** When true any data changes will be animated. */
    private BooleanProperty animated = new SimpleBooleanProperty(this, "animated", true);

    /**
     * Indicates whether data changes will be animated or not.
     *
     * @return true if data changes will be animated and false otherwise.
     */
    public final boolean getAnimated() { return animated.get(); }
    public final void setAnimated(boolean value) { animated.set(value); }
    public final BooleanProperty animatedProperty() { return animated; }

    // -------------- PROTECTED PROPERTIES -----------------------------------------------------------------------------

    /**
     * Modifiable and observable list of all content in the chart. This is where implementations of Chart should add
     * any nodes they use to draw their chart. This excludes the legend and title which are looked after by this class.
     *
     * @return Observable list of plot children
     */
    protected ObservableList<Node> getChartChildren() {
        return chartContent.getChildren();
    }

    // -------------- CONSTRUCTOR --------------------------------------------------------------------------------------

    public Chart() {
        titleLabel.setAlignment(Pos.CENTER);
        getChildren().addAll(titleLabel, chartContent);
        getStyleClass().add("chart");
        titleLabel.getStyleClass().add("chart-title");
        chartContent.getStyleClass().add("chart-content");
        // mark chartContent as unmanaged because any changes to its preferred size shouldn't cause a relayout
        chartContent.setManaged(false);
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------

    /**
     * Play a animation involving the given keyframes. On every frame of the animation the chart will be relayed out
     *
     * @param keyFrames Array of KeyFrames to play
     */
    void animate(KeyFrame...keyFrames) { animator.animate(keyFrames); }

    /**
     * Play the given animation on every frame of the animation the chart will be relayed out until the animation
     * finishes. So to add a animation to a chart, create a animation on data model, during layoutChartContent() map
     * data model to nodes then call this method with the animation.
     *
     * @param animation The animation to play
     */
    protected void animate(Animation animation) { animator.animate(animation); }

    /** Call this when you know something has changed that needs the chart to be relayed out. */
    protected void requestChartLayout() {
        chartContent.requestLayout();
    }

    /**
     * This is used to check if any given animation should run. It returns true if animation is enabled and the node
     * is visible and in a scene.
     */
    protected final boolean shouldAnimate(){
        return getAnimated() && impl_isTreeVisible() && getScene() != null;
    }

    /**
     * Called to update and layout the chart children available from getChartChildren()
     *
     * @param top The top offset from the origin to account for any padding on the chart content
     * @param left The left offset from the origin to account for any padding on the chart content
     * @param width The width of the area to layout the chart within
     * @param height The height of the area to layout the chart within
     */
    protected abstract void layoutChartChildren(double top, double left, double width, double height);

    /**
     * Invoked during the layout pass to layout this chart and all its content.
     */
    @Override protected void layoutChildren() {
        double top = getInsets().getTop();
        double left = getInsets().getLeft();
        double bottom = getInsets().getBottom();
        double right = getInsets().getRight();
        final double width = getWidth();
        final double height = getHeight();
        // layout title
        if (getTitle() != null) {
            titleLabel.setVisible(true);
            if (getTitleSide().equals(Side.TOP)) {
                final double titleHeight = snapSize(titleLabel.prefHeight(width-left-right));
                titleLabel.resizeRelocate(left,top,width-left-right,titleHeight);
                top += titleHeight;
            } else if (getTitleSide().equals(Side.BOTTOM)) {
                final double titleHeight = snapSize(titleLabel.prefHeight(width-left-right));
                titleLabel.resizeRelocate(left,height-bottom-titleHeight,width-left-right,titleHeight);
                bottom += titleHeight;
            } else if (getTitleSide().equals(Side.LEFT)) {
                final double titleWidth = snapSize(titleLabel.prefWidth(height-top-bottom));
                titleLabel.resizeRelocate(left,top,titleWidth,height-top-bottom);
                left += titleWidth;
            } else if (getTitleSide().equals(Side.RIGHT)) {
                final double titleWidth = snapSize(titleLabel.prefWidth(height-top-bottom));
                titleLabel.resizeRelocate(width-right-titleWidth,top,titleWidth,height-top-bottom);
                right += titleWidth;
            }
        } else {
            titleLabel.setVisible(false);
        }
        // layout legend
        final Node legend = getLegend();
        if (legend != null) {
            boolean shouldShowLegend = isLegendVisible();
            if (shouldShowLegend) {
                if (getLegendSide().equals(Side.TOP)) {
                    final double legendHeight = snapSize(legend.prefHeight(width-left-right));
                    final double legendWidth = snapSize(legend.prefWidth(-1));
                    legend.resizeRelocate(left + (((width - left - right)-legendWidth)/2), top, legendWidth, legendHeight);
                    if ((height - bottom - top - legendHeight) < MIN_HEIGHT_TO_LEAVE_FOR_CHART_CONTENT) {
                        shouldShowLegend = false;
                    } else {
                        top += legendHeight;
                    }
                } else if (getLegendSide().equals(Side.BOTTOM)) {
                    final double legendHeight = snapSize(legend.prefHeight(width-left-right));
                    final double legendWidth = snapSize(legend.prefWidth(-1));
                    legend.resizeRelocate(left + (((width - left - right)-legendWidth)/2), height-bottom-legendHeight, legendWidth, legendHeight);
                    if ((height - bottom - top - legendHeight) < MIN_HEIGHT_TO_LEAVE_FOR_CHART_CONTENT) {
                        shouldShowLegend = false;
                    } else {
                        bottom += legendHeight;
                    }
                } else if (getLegendSide().equals(Side.LEFT)) {
                    final double legendWidth = snapSize(legend.prefWidth(height-top-bottom));
                    final double legendHeight = snapSize(legend.prefHeight(-1));
                    legend.resizeRelocate(left,top +(((height-top-bottom)-legendHeight)/2),legendWidth,legendHeight);
                    if ((width - left - right - legendWidth) < MIN_WIDTH_TO_LEAVE_FOR_CHART_CONTENT) {
                        shouldShowLegend = false;
                    } else {
                        left += legendWidth;
                    }
                } else if (getLegendSide().equals(Side.RIGHT)) {
                    final double legendWidth = snapSize(legend.prefWidth(height-top-bottom));
                    final double legendHeight = snapSize(legend.prefHeight(-1));
                    legend.resizeRelocate(width-right-legendWidth,top +(((height-top-bottom)-legendHeight)/2),legendWidth,legendHeight);
                    if ((width - left - right - legendWidth) < MIN_WIDTH_TO_LEAVE_FOR_CHART_CONTENT) {
                        shouldShowLegend = false;
                    } else {
                        right += legendWidth;
                    }
                }
            }
            legend.setVisible(shouldShowLegend);
        }
        // whats left is for the chart content
        chartContent.resizeRelocate(left,top,width-left-right,height-top-bottom);
    }

    /**
     * Charts are sized outside in, user tells chart how much space it has and chart draws inside that. So minimum
     * height is a constant 150.
     */
    @Override protected double computeMinHeight(double width) { return 150; }

    /**
     * Charts are sized outside in, user tells chart how much space it has and chart draws inside that. So minimum
     * width is a constant 200.
     */
    @Override protected double computeMinWidth(double height) { return 200; }

    /**
     * Charts are sized outside in, user tells chart how much space it has and chart draws inside that. So preferred
     * width is a constant 500.
     */
    @Override protected double computePrefWidth(double height) { return 500.0; }

    /**
     * Charts are sized outside in, user tells chart how much space it has and chart draws inside that. So preferred
     * height is a constant 400.
     */
    @Override protected double computePrefHeight(double width) { return 400.0; }

    // -------------- STYLESHEET HANDLING ------------------------------------------------------------------------------

    private static class StyleableProperties {
        private static final CssMetaData<Chart,Side> TITLE_SIDE =
            new CssMetaData<Chart,Side>("-fx-title-side",
                new EnumConverter<Side>(Side.class),
                Side.TOP) {

            @Override
            public boolean isSettable(Chart node) {
                return node.titleSide == null || !node.titleSide.isBound();
            }

            @Override
            public WritableValue<Side> getWritableValue(Chart node) {
                return node.titleSideProperty();
            }
        };
        
        private static final CssMetaData<Chart,Side> LEGEND_SIDE =
            new CssMetaData<Chart,Side>("-fx-legend-side",
                new EnumConverter<Side>(Side.class),
                Side.BOTTOM) {

            @Override
            public boolean isSettable(Chart node) {
                return node.legendSide == null || !node.legendSide.isBound();
            }

            @Override
            public WritableValue<Side> getWritableValue(Chart node) {
                return node.legendSideProperty();
            }
        };
        
        private static final CssMetaData<Chart,Boolean> LEGEND_VISIBLE =
            new CssMetaData<Chart,Boolean>("-fx-legend-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(Chart node) {
                return node.legendVisible == null || !node.legendVisible.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(Chart node) {
                return node.legendVisibleProperty();
            }
        };

        private static final List<CssMetaData> STYLEABLES;
        static {
            final List<CssMetaData> styleables = 
                new ArrayList<CssMetaData>(Region.getClassCssMetaData());
            Collections.addAll(styleables,
                TITLE_SIDE,
                LEGEND_VISIBLE,
                LEGEND_SIDE
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


