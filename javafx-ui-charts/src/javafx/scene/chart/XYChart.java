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

import com.sun.javafx.collections.NonIterableChange;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import com.sun.javafx.css.StyleableBooleanProperty;
import com.sun.javafx.css.CssMetaData;
import com.sun.javafx.css.converters.BooleanConverter;

/**
 * Chart base class for all 2 axis charts. It is responsible for drawing the two
 * axes and the plot content. It contains a list of all content in the plot and
 * implementations of XYChart can add nodes to this list that need to be rendered.
 */
public abstract class XYChart<X,Y> extends Chart {

    // -------------- PRIVATE FIELDS -----------------------------------------------------------------------------------

    private int seriesDefaultColorIndex = 0;
    private boolean rangeValid = false;
    private final Line verticalZeroLine = new Line();
    private final Line horizontalZeroLine = new Line();
    private final Path verticalGridLines = new Path();
    private final Path horizontalGridLines = new Path();
    private final Path horizontalRowFill = new Path();
    private final Path verticalRowFill = new Path();
    private final Region plotBackground = new Region();
    private final Group plotArea = new Group(){
        @Override public void requestLayout() {} // suppress layout requests
    };
    private final Group plotContent = new Group();
    private final Rectangle plotAreaClip = new Rectangle();
    /* start pointer of a series linked list. */
    Series<X,Y> begin = null;
    /** This is called when a series is added or removed from the chart */
    private final ListChangeListener<Series<X,Y>> seriesChanged = new ListChangeListener<Series<X,Y>>() {
        @Override public void onChanged(Change<? extends Series<X,Y>> c) {
            while (c.next()) {
                if (c.getRemoved().size() > 0) updateLegend();
                for (Series<X,Y> series : c.getRemoved()) {
                    series.setChart(null);
                    seriesRemoved(series);
                    seriesDefaultColorIndex --;
                }
                for(int i=c.getFrom(); i<c.getTo() && !c.wasPermutated(); i++) {
                    final Series<X,Y> series = c.getList().get(i);
                    // add new listener to data
                    series.setChart(XYChart.this);
                    // update linkedList Pointers for series
                    if (XYChart.this.begin == null) {
                        XYChart.this.begin = getData().get(i);
                        XYChart.this.begin.next = null;
                    } else {
                        if (i == 0) {
                            getData().get(0).next = XYChart.this.begin;
                            begin = getData().get(0);
                        } else {
                            Series ptr = begin;
                            for (int j = 0; j < i -1 && ptr!=null ; j++) {
                                ptr = ptr.next;
                            }
                            if (ptr != null) {
                                getData().get(i).next = ptr.next;
                                ptr.next = getData().get(i);
                            }

                        }
                    }
                    // update default color style class
                    series.defaultColorStyleClass = "default-color"+(seriesDefaultColorIndex % 8);
                    seriesDefaultColorIndex ++;
                    // inform sub-classes of series added
                    seriesAdded(series, i);
                }
                if (c.getFrom() < c.getTo()) updateLegend();
                seriesChanged(c);
                // RT-12069, linked list pointers should update when list is permutated.
                if (c.wasPermutated() && getData().size() > 0) {
                    XYChart.this.begin = getData().get(0);
                    Series<X,Y> ptr = begin;
                    for(int k = 1; k < getData().size() && ptr != null; k++) {
                        ptr.next = getData().get(k);
                        ptr = ptr.next;
                    }
                    ptr.next = null;
                }
            }
            // update axis ranges
            invalidateRange();
            // lay everything out
            requestChartLayout();
        }
    };

    // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

    private final Axis<X> xAxis;
    /** Get the X axis, by default it is along the bottom of the plot */
    public Axis<X> getXAxis() { return xAxis; }

    private final Axis<Y> yAxis;
    /** Get the Y axis, by default it is along the left of the plot */
    public Axis<Y> getYAxis() { return yAxis; }

    /** XYCharts data */
    private ObjectProperty<ObservableList<Series<X,Y>>> data = new ObjectPropertyBase<ObservableList<Series<X,Y>>>() {
        private ObservableList<Series<X,Y>> old;
        @Override protected void invalidated() {
            final ObservableList<Series<X,Y>> current = getValue();
            int saveAnimationState = -1;
            // add remove listeners
            if(old != null) {
                old.removeListener(seriesChanged);
                // Set animated to false so we don't animate both remove and add
                // at the same time. RT-14163 
                // RT-21295 - disable animated only when current is also not null. 
                if (current != null && old.size() > 0) {
                    saveAnimationState = (old.get(0).getChart().getAnimated()) ? 1 : 2;
                    old.get(0).getChart().setAnimated(false);
                }
            }
            if(current != null) current.addListener(seriesChanged);
            // fire series change event if series are added or removed
            if(old != null || current != null) {
                final List<Series<X,Y>> removed = (old != null) ? old : Collections.<Series<X,Y>>emptyList();
                final int toIndex = (current != null) ? current.size() : 0;
                // let series listener know all old series have been removed and new that have been added
                if (toIndex > 0 || !removed.isEmpty()) {
                    seriesChanged.onChanged(new NonIterableChange<Series<X,Y>>(0, toIndex, current){
                        @Override public List<Series<X,Y>> getRemoved() { return removed; }
                        @Override protected int[] getPermutation() {
                            return new int[0];
                        }
                    });
                }
            } else if (old != null && old.size() > 0) {
                // let series listener know all old series have been removed
                seriesChanged.onChanged(new NonIterableChange<Series<X,Y>>(0, 0, current){
                    @Override public List<Series<X,Y>> getRemoved() { return old; }
                    @Override protected int[] getPermutation() {
                        return new int[0];
                    }
                });
            }
            // restore animated on chart.
            if (current != null && current.size() > 0 && saveAnimationState != -1) {
                current.get(0).getChart().setAnimated((saveAnimationState == 1) ? true : false);
            }
            old = current;
        }

        public Object getBean() {
            return XYChart.this;
        }

        public String getName() {
            return "data";
        }
    };
    public final ObservableList<Series<X,Y>> getData() { return data.getValue(); }
    public final void setData(ObservableList<Series<X,Y>> value) { data.setValue(value); }
    public final ObjectProperty<ObservableList<Series<X,Y>>> dataProperty() { return data; }

    /** True if vertical grid lines should be drawn */ 
    private BooleanProperty verticalGridLinesVisible = new StyleableBooleanProperty(true) {
        @Override protected void invalidated() {
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return XYChart.this;
        }

        @Override
        public String getName() {
            return "verticalGridLinesVisible";
        }

        @Override
        public CssMetaData getCssMetaData() {
            return StyleableProperties.VERTICAL_GRID_LINE_VISIBLE;
        }
    };
    /**
     * Indicates whether vertical grid lines are visible or not.
     *
     * @return true if verticalGridLines are visible else false.
     * @see #verticalGridLinesVisible
     */
    public final boolean getVerticalGridLinesVisible() { return verticalGridLinesVisible.get(); }
    public final void setVerticalGridLinesVisible(boolean value) { verticalGridLinesVisible.set(value); }
    public final BooleanProperty verticalGridLinesVisibleProperty() { return verticalGridLinesVisible; }

    /** True if horizontal grid lines should be drawn */
    private BooleanProperty horizontalGridLinesVisible = new StyleableBooleanProperty(true) {
        @Override protected void invalidated() {
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return XYChart.this;
        }

        @Override
        public String getName() {
            return "horizontalGridLinesVisible";
        }
        
        @Override
        public CssMetaData getCssMetaData() {
            return StyleableProperties.HORIZONTAL_GRID_LINE_VISIBLE;
        }        
    };
    public final boolean isHorizontalGridLinesVisible() { return horizontalGridLinesVisible.get(); }
    public final void setHorizontalGridLinesVisible(boolean value) { horizontalGridLinesVisible.set(value); }
    public final BooleanProperty horizontalGridLinesVisibleProperty() { return horizontalGridLinesVisible; }

    /** If true then alternative vertical columns will have fills */
    private BooleanProperty alternativeColumnFillVisible = new StyleableBooleanProperty(false) {
        @Override protected void invalidated() {
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return XYChart.this;
        }

        @Override
        public String getName() {
            return "alternativeColumnFillVisible";
        }

        @Override
        public CssMetaData getCssMetaData() {
            return StyleableProperties.ALTERNATIVE_COLUMN_FILL_VISIBLE;
        }        
    };
    public final boolean isAlternativeColumnFillVisible() { return alternativeColumnFillVisible.getValue(); }
    public final void setAlternativeColumnFillVisible(boolean value) { alternativeColumnFillVisible.setValue(value); }
    public final BooleanProperty alternativeColumnFillVisibleProperty() { return alternativeColumnFillVisible; }

    /** If true then alternative horizontal rows will have fills */
    private BooleanProperty alternativeRowFillVisible = new StyleableBooleanProperty(true) {
        @Override protected void invalidated() {
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return XYChart.this;
        }

        @Override
        public String getName() {
            return "alternativeRowFillVisible";
        }

        @Override
        public CssMetaData getCssMetaData() {
            return StyleableProperties.ALTERNATIVE_ROW_FILL_VISIBLE;
        }                
    };
    public final boolean isAlternativeRowFillVisible() { return alternativeRowFillVisible.getValue(); }
    public final void setAlternativeRowFillVisible(boolean value) { alternativeRowFillVisible.setValue(value); }
    public final BooleanProperty alternativeRowFillVisibleProperty() { return alternativeRowFillVisible; }

    /**
     * If this is true and the vertical axis has both positive and negative values then a additional axis line
     * will be drawn at the zero point
     *
     * @defaultValue true
     */
    private BooleanProperty verticalZeroLineVisible = new StyleableBooleanProperty(true) {
        @Override protected void invalidated() {
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return XYChart.this;
        }

        @Override
        public String getName() {
            return "verticalZeroLineVisible";
        }

        @Override
        public CssMetaData getCssMetaData() {
            return StyleableProperties.VERTICAL_ZERO_LINE_VISIBLE;
        }                
    };
    public final boolean isVerticalZeroLineVisible() { return verticalZeroLineVisible.get(); }
    public final void setVerticalZeroLineVisible(boolean value) { verticalZeroLineVisible.set(value); }
    public final BooleanProperty verticalZeroLineVisibleProperty() { return verticalZeroLineVisible; }

    /**
     * If this is true and the horizontal axis has both positive and negative values then a additional axis line
     * will be drawn at the zero point
     *
     * @defaultValue true
     */
    private BooleanProperty horizontalZeroLineVisible = new StyleableBooleanProperty(true) {
        @Override protected void invalidated() {
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return XYChart.this;
        }

        @Override
        public String getName() {
            return "horizontalZeroLineVisible";
        }

        @Override
        public CssMetaData getCssMetaData() {
            return StyleableProperties.HORIZONTAL_ZERO_LINE_VISIBLE;
        }                
    };
    public final boolean isHorizontalZeroLineVisible() { return horizontalZeroLineVisible.get(); }
    public final void setHorizontalZeroLineVisible(boolean value) { horizontalZeroLineVisible.set(value); }
    public final BooleanProperty horizontalZeroLineVisibleProperty() { return horizontalZeroLineVisible; }

    // -------------- PROTECTED PROPERTIES -----------------------------------------------------------------------------

    /**
     * Modifiable and observable list of all content in the plot. This is where implementations of XYChart should add
     * any nodes they use to draw their plot.
     *
     * @return Observable list of plot children
     */
    protected ObservableList<Node> getPlotChildren() {
        return plotContent.getChildren();
    }

    // -------------- CONSTRUCTOR --------------------------------------------------------------------------------------

    /**
     * Constructs a XYChart given the two axes. The initial content for the chart
     * plot background and plot area that includes vertical and horizontal grid
     * lines and fills, are added.
     *
     * @param xAxis X Axis for this XY chart
     * @param yAxis Y Axis for this XY chart
     */
    public XYChart(Axis<X> xAxis, Axis<Y> yAxis) {
        this.xAxis = xAxis;
        if(xAxis.getSide() == null) xAxis.setSide(Side.BOTTOM);
        this.yAxis = yAxis;
        if(yAxis.getSide() == null) yAxis.setSide(Side.LEFT);
        // add initial content to chart content
        getChartChildren().addAll(plotBackground,plotArea,xAxis,yAxis);
        // We don't want plotArea or plotContent to autoSize or do layout
        plotArea.setAutoSizeChildren(false);
        plotContent.setAutoSizeChildren(false);
        // setup clipping on plot area
        plotAreaClip.setSmooth(false);
        plotArea.setClip(plotAreaClip);
        // add children to plot area
        plotArea.getChildren().addAll(
                verticalRowFill, horizontalRowFill,
                verticalGridLines, horizontalGridLines,
                verticalZeroLine, horizontalZeroLine,
                plotContent);
        // setup css style classes
        plotContent.getStyleClass().setAll("plot-content");
        plotBackground.getStyleClass().setAll("chart-plot-background");
        verticalRowFill.getStyleClass().setAll("chart-alternative-column-fill");
        horizontalRowFill.getStyleClass().setAll("chart-alternative-row-fill");
        verticalGridLines.getStyleClass().setAll("chart-vertical-grid-lines");
        horizontalGridLines.getStyleClass().setAll("chart-horizontal-grid-lines");
        verticalZeroLine.getStyleClass().setAll("chart-vertical-zero-line");
        horizontalZeroLine.getStyleClass().setAll("chart-horizontal-zero-line");
        // mark plotContent as unmanaged as its preferred size changes do not effect our layout
        plotContent.setManaged(false);
        plotArea.setManaged(false);
        // listen to animation on/off and sync to axis
        animatedProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> valueModel, Boolean oldValue, Boolean newValue) {
                if(getXAxis() != null) getXAxis().setAnimated(newValue);
                if(getYAxis() != null) getYAxis().setAnimated(newValue);
            }
        });
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------

    /**
     * Gets the size of the data returning 0 if the data is null
     *
     * @return The number of items in data, or null if data is null
     */
    final int getDataSize() {
        final ObservableList<Series<X,Y>> data = getData();
        return (data!=null) ? data.size() : 0;
    }

    /** Called when a series's name has changed */
    private void seriesNameChanged() {
        updateLegend();
        requestChartLayout();
    }

    @SuppressWarnings({"UnusedParameters"})
    private void dataItemsChanged(Series<X,Y> series, List<Data<X,Y>> removed, int addedFrom, int addedTo, boolean permutation) {
        for (Data<X,Y> item : removed) {
            dataItemRemoved(item, series);
        }
        for(int i=addedFrom; i<addedTo; i++) {
            Data<X,Y> item = series.getData().get(i);
            dataItemAdded(series, i, item);
        }
        invalidateRange();
        requestChartLayout();
    }

    private void dataXValueChanged(Data<X,Y> item) {
        if(item.getCurrentX() != item.getXValue()) invalidateRange();
        dataItemChanged(item);
        if (shouldAnimate()) {
            animate(
                    new KeyFrame(Duration.ZERO, new KeyValue(item.currentXProperty(), item.getCurrentX())),
                    new KeyFrame(Duration.millis(700), new KeyValue(item.currentXProperty(), item.getXValue(), Interpolator.EASE_BOTH))
            );
        } else {
            item.setCurrentX(item.getXValue());
            requestChartLayout();
        }
    }

    private void dataYValueChanged(Data<X,Y> item) {
        if(item.getCurrentY() != item.getYValue()) invalidateRange();
        dataItemChanged(item);
        if (shouldAnimate()) {
            animate(
                    new KeyFrame(Duration.ZERO, new KeyValue(item.currentYProperty(), item.getCurrentY())),
                    new KeyFrame(Duration.millis(700), new KeyValue(item.currentYProperty(), item.getYValue(), Interpolator.EASE_BOTH))
            );
        } else {
            item.setCurrentY(item.getYValue());
            requestChartLayout();
        }
    }

    private void dataExtraValueChanged(Data<X,Y> item) {
        if(item.getCurrentY() != item.getYValue()) invalidateRange();
        dataItemChanged(item);
        if (shouldAnimate()) {
            animate(
                    new KeyFrame(Duration.ZERO, new KeyValue(item.currentYProperty(), item.getCurrentY())),
                    new KeyFrame(Duration.millis(700), new KeyValue(item.currentYProperty(), item.getYValue(), Interpolator.EASE_BOTH))
            );
        } else {
            item.setCurrentY(item.getYValue());
            requestChartLayout();
        }
    }

    /**
     * This is called whenever a series is added or removed and the legend needs to be updated
     */
    protected void updateLegend(){}

    /**
     * Called when a data item has been added to a series. This is where implementations of XYChart can create/add new
     * nodes to getPlotChildren to represent this data item. They also may animate that data add with a fade in or
     * similar if animated = true.
     *
     * @param series    The series the data item was added to
     * @param itemIndex The index of the new item within the series
     * @param item      The new data item that was added
     */
    protected abstract void dataItemAdded(Series<X,Y> series, int itemIndex, Data<X,Y> item);

    /**
     * Called when a data item has been removed from data model but it is still visible on the chart. Its still visible
     * so that you can handle animation for removing it in this method. After you are done animating the data item you
     * must call removeDataItemFromDisplay() to remove the items node from being displayed on the chart.
     *
     * @param item   The item that has been removed from the series
     * @param series The series the item was removed from
     */
    protected abstract void dataItemRemoved(Data<X, Y> item, Series<X, Y> series);

    /**
     * Called when a data item has changed, ie its xValue, yValue or extraValue has changed. 
     *
     * @param item    The data item who was changed
     */
    protected abstract void dataItemChanged(Data<X, Y> item);
    /**
     * A series has been added to the charts data model. This is where implementations of XYChart can create/add new
     * nodes to getPlotChildren to represent this series. Also you have to handle adding any data items that are
     * already in the series. You may simply call dataItemAdded() for each one or provide some different animation for
     * a whole series being added.
     *
     * @param series      The series that has been added
     * @param seriesIndex The index of the new series
     */
    protected abstract void seriesAdded(Series<X, Y> series, int seriesIndex);

    /**
     * A series has been removed from the data model but it is still visible on the chart. Its still visible
     * so that you can handle animation for removing it in this method. After you are done animating the data item you
     * must call removeSeriesFromDisplay() to remove the series from the display list.
     *
     * @param series The series that has been removed
     */
    protected abstract void seriesRemoved(Series<X,Y> series);

    /** Called when each atomic change is made to the list of series for this chart */
    protected void seriesChanged(Change<? extends Series> c) {}

    /**
     * This is called when a data change has happened that may cause the range to be invalid.
     */
    private void invalidateRange() {
        rangeValid = false;
    }

    /**
     * This is called when the range has been invalidated and we need to update it. If the axis are auto
     * ranging then we compile a list of all data that the given axis has to plot and call invalidateRange() on the
     * axis passing it that data.
     */
    protected void updateAxisRange() {
        final Axis<X> xa = getXAxis();
        final Axis<Y> ya = getYAxis();
        List<X> xData = null;
        List<Y> yData = null;
        if(xa.isAutoRanging()) xData = new ArrayList<X>();
        if(ya.isAutoRanging()) yData = new ArrayList<Y>();
        if(xData != null || yData != null) {
            for(Series<X,Y> series : getData()) {
                for(Data<X,Y> data: series.getData()) {
                    if(xData != null) xData.add(data.getXValue());
                    if(yData != null) yData.add(data.getYValue());
                }
            }
            if(xData != null) xa.invalidateRange(xData);
            if(yData != null) ya.invalidateRange(yData);
        }
    }

    /**
     * Called to update and layout the plot children. This should include all work to updates nodes representing
     * the plot on top of the axis and grid lines etc. The origin is the top left of the plot area, the plot area with
     * can be got by getting the width of the x axis and its height from the height of the y axis.
     */
    protected abstract void layoutPlotChildren();

    /** @inheritDoc */
    @Override protected final void layoutChartChildren(double top, double left, double width, double height) {
        if(getData() == null) return;
        if (!rangeValid) {
            rangeValid = true;
            if(getData() != null) updateAxisRange();
        }
        // snap top and left to pixels
        top = snapPosition(top);
        left = snapPosition(left);
        // get starting stuff
        final Axis<X> xa = getXAxis();
        final ObservableList<Axis.TickMark<X>> xaTickMarks = xa.getTickMarks();
        final Axis<Y> ya = getYAxis();
        final ObservableList<Axis.TickMark<Y>> yaTickMarks = ya.getTickMarks();
        // check we have 2 axises and know their sides
        if (xa == null || ya == null || xa.getSide() == null || ya.getSide() == null) return;
        // try and work out width and height of axises
        double xAxisWidth = 0;
        double xAxisHeight = 30; // guess x axis height to start with
        double yAxisWidth = 0;
        double yAxisHeight = 0;
        for (int count=0; count<5; count ++) {
            yAxisHeight = height-xAxisHeight;
            yAxisWidth = ya.prefWidth(yAxisHeight);
            xAxisWidth = width - yAxisWidth;
            double newXAxisHeight = xa.prefHeight(xAxisWidth);
            if (newXAxisHeight == xAxisHeight) break;
            xAxisHeight = newXAxisHeight;
        }
        // round axis sizes up to whole integers to snap to pixel
        xAxisWidth = Math.ceil(xAxisWidth);
        xAxisHeight = Math.ceil(xAxisHeight);
        yAxisWidth = Math.ceil(yAxisWidth);
        yAxisHeight = Math.ceil(yAxisHeight);
        // calc xAxis height
        double xAxisY = 0;
        if (xa.getSide().equals(Side.TOP)) {
            xa.setVisible(true);
            xAxisY = top+1;
            top += xAxisHeight;
        } else if (xa.getSide().equals(Side.BOTTOM)) {
            xa.setVisible(true);
            xAxisY = top + yAxisHeight;
        } else {
            // X axis should never be left or right so hide
            xa.setVisible(false);
            xAxisHeight = 0;
        }
        // calc yAxis width
        double yAxisX = 0;
        if (ya.getSide().equals(Side.LEFT)) {
            ya.setVisible(true);
            yAxisX = left +1;
            left += yAxisWidth;
        } else if (ya.getSide().equals(Side.RIGHT)) {
            ya.setVisible(true);
            yAxisX = left + xAxisWidth;
        } else {
            // Y axis should never be top or bottom so hide
            ya.setVisible(false);
            yAxisWidth = 0;
        }
        // resize axises
        xa.resizeRelocate(left, xAxisY, xAxisWidth, xAxisHeight);
        ya.resizeRelocate(yAxisX, top, yAxisWidth, yAxisHeight);
        // When the chart is resized, need to specifically call out the axises
        // to lay out as they are unmanaged.
        xa.requestAxisLayout();
        xa.layout();
        ya.requestAxisLayout();
        ya.layout();
        // layout plot content
        layoutPlotChildren();
        // get axis zero points
        final double xAxisZero = xa.getZeroPosition();
        final double yAxisZero = ya.getZeroPosition();
        // position vertical and horizontal zero lines
        if(Double.isNaN(xAxisZero) || !isVerticalZeroLineVisible()) {
            verticalZeroLine.setVisible(false);
        } else {
            verticalZeroLine.setStartX(left+xAxisZero+0.5);
            verticalZeroLine.setStartY(top);
            verticalZeroLine.setEndX(left+xAxisZero+0.5);
            verticalZeroLine.setEndY(top+yAxisHeight);
            verticalZeroLine.setVisible(true);
        }
        if(Double.isNaN(yAxisZero) || !isHorizontalZeroLineVisible()) {
            horizontalZeroLine.setVisible(false);
        } else {
            horizontalZeroLine.setStartX(left);
            horizontalZeroLine.setStartY(top+yAxisZero+0.5);
            horizontalZeroLine.setEndX(left+xAxisWidth);
            horizontalZeroLine.setEndY(top+yAxisZero+0.5);
            horizontalZeroLine.setVisible(true);
        }
        // layout plot background
        plotBackground.resizeRelocate(left, top, xAxisWidth, yAxisHeight);
        // update clip
        plotAreaClip.setX(left);
        plotAreaClip.setY(top);
        plotAreaClip.setWidth(xAxisWidth+1);
        plotAreaClip.setHeight(yAxisHeight+1);
//        plotArea.setClip(new Rectangle(left, top, xAxisWidth, yAxisHeight));
        // position plot group, its origin is the bottom left corner of the plot area
        plotContent.setLayoutX(left);
        plotContent.setLayoutY(top);
        plotContent.requestLayout(); // Note: not sure this is right, maybe plotContent should be resizeable
        // update vertical grid lines
        verticalGridLines.getElements().clear();
        if(getVerticalGridLinesVisible()) {
            for(int i=0; i < xaTickMarks.size(); i++) {
                Axis.TickMark<X> tick = xaTickMarks.get(i);
                double pixelOffset = (i==(xaTickMarks.size()-1)) ? -0.5 : 0.5; 
                final double x = xa.getDisplayPosition(tick.getValue());
                if ((x!=xAxisZero || !isVerticalZeroLineVisible()) && x > 0 && x <= xAxisWidth) {
                    verticalGridLines.getElements().add(new MoveTo(left+x+pixelOffset,top));
                    verticalGridLines.getElements().add(new LineTo(left+x+pixelOffset,top+yAxisHeight));
                }
            }
        }
        // update horizontal grid lines
        horizontalGridLines.getElements().clear();
        if(isHorizontalGridLinesVisible()) {
            for(int i=0; i < yaTickMarks.size(); i++) {
                Axis.TickMark<Y> tick = yaTickMarks.get(i);
                double pixelOffset = (i==(yaTickMarks.size()-1)) ? -0.5 : 0.5;
                final double y = ya.getDisplayPosition(tick.getValue());
                if ((y!=yAxisZero || !isHorizontalZeroLineVisible()) && y >= 0 && y < yAxisHeight) {
                    horizontalGridLines.getElements().add(new MoveTo(left,top+y+pixelOffset));
                    horizontalGridLines.getElements().add(new LineTo(left+xAxisWidth,top+y+pixelOffset));
                }
            }
        }
        // Note: is there a more efficient way to calculate horizontal and vertical row fills?
        // update vertical row fill
        verticalRowFill.getElements().clear();
        if (isAlternativeColumnFillVisible()) {
            // tick marks are not sorted so get all the positions and sort them
            final List<Double> tickPositionsPositive = new ArrayList<Double>();
            final List<Double> tickPositionsNegative = new ArrayList<Double>();
            for(int i=0; i < xaTickMarks.size(); i++) {
                double pos = xa.getDisplayPosition((X) xaTickMarks.get(i).getValue());
                if (pos == xAxisZero) {
                    tickPositionsPositive.add(pos);
                    tickPositionsNegative.add(pos);
                } else if (pos < xAxisZero) {
                    tickPositionsPositive.add(pos);
                } else {
                    tickPositionsNegative.add(pos);
                }
            }
            Collections.sort(tickPositionsPositive);
            Collections.sort(tickPositionsNegative);
            // iterate over every pair of positive tick marks and create fill
            for(int i=1; i < tickPositionsPositive.size(); i+=2) {
                if((i+1) < tickPositionsPositive.size()) {
                    final double x1 = tickPositionsPositive.get(i);
                    final double x2 = tickPositionsPositive.get(i+1);
                    verticalRowFill.getElements().addAll(
                            new MoveTo(left+x1,top),
                            new LineTo(left+x1,top+yAxisHeight),
                            new LineTo(left+x2,top+yAxisHeight),
                            new LineTo(left+x2,top),
                            new ClosePath());
                }
            }
            // iterate over every pair of positive tick marks and create fill
            for(int i=0; i < tickPositionsNegative.size(); i+=2) {
                if((i+1) < tickPositionsNegative.size()) {
                    final double x1 = tickPositionsNegative.get(i);
                    final double x2 = tickPositionsNegative.get(i+1);
                    verticalRowFill.getElements().addAll(
                            new MoveTo(left+x1,top),
                            new LineTo(left+x1,top+yAxisHeight),
                            new LineTo(left+x2,top+yAxisHeight),
                            new LineTo(left+x2,top),
                            new ClosePath());
                }
            }
        }
        // update horizontal row fill
        horizontalRowFill.getElements().clear();
        if (isAlternativeRowFillVisible()) {
            // tick marks are not sorted so get all the positions and sort them
            final List<Double> tickPositionsPositive = new ArrayList<Double>();
            final List<Double> tickPositionsNegative = new ArrayList<Double>();
            for(int i=0; i < yaTickMarks.size(); i++) {
                double pos = ya.getDisplayPosition((Y) yaTickMarks.get(i).getValue());
                if (pos == yAxisZero) {
                    tickPositionsPositive.add(pos);
                    tickPositionsNegative.add(pos);
                } else if (pos < yAxisZero) {
                    tickPositionsPositive.add(pos);
                } else {
                    tickPositionsNegative.add(pos);
                }
            }
            Collections.sort(tickPositionsPositive);
            Collections.sort(tickPositionsNegative);
            // iterate over every pair of positive tick marks and create fill
            for(int i=1; i < tickPositionsPositive.size(); i+=2) {
                if((i+1) < tickPositionsPositive.size()) {
                    final double y1 = tickPositionsPositive.get(i);
                    final double y2 = tickPositionsPositive.get(i+1);
                    horizontalRowFill.getElements().addAll(
                            new MoveTo(left, top + y1),
                            new LineTo(left + xAxisWidth, top + y1),
                            new LineTo(left + xAxisWidth, top + y2),
                            new LineTo(left, top + y2),
                            new ClosePath());
                }
            }
            // iterate over every pair of positive tick marks and create fill
            for(int i=0; i < tickPositionsNegative.size(); i+=2) {
                if((i+1) < tickPositionsNegative.size()) {
                    final double y1 = tickPositionsNegative.get(i);
                    final double y2 = tickPositionsNegative.get(i+1);
                    horizontalRowFill.getElements().addAll(
                            new MoveTo(left, top + y1),
                            new LineTo(left + xAxisWidth, top + y1),
                            new LineTo(left + xAxisWidth, top + y2),
                            new LineTo(left, top + y2),
                            new ClosePath());
                }
            }
        }
//
    }

    /**
     * Get the index of the series in the series linked list.
     *
     * @param series The series to find index for
     * @return index of the series in series list
     */
    int getSeriesIndex(Series series) {
        int itemIndex = 0;
        for (Series s = XYChart.this.begin; s != null; s = s.next) {
            if (s == series) break;
            itemIndex++;
        }
        return itemIndex;
    }

    /**
     * Computes the size of series linked list
     * @return size of series linked list
     */
    int getSeriesSize() {
        int count = 0;
        for (Series d = XYChart.this.begin; d != null; d = d.next) {
            count++;
        }
        return count;
    }
    
    /**
     * This should be called from seriesRemoved() when you are finished with any animation for deleting the series from
     * the chart. It will remove the series from showing up in the Iterator returned by getDisplayedSeriesIterator().
     *
     * @param series The series to remove
     */
    protected final void removeSeriesFromDisplay(Series<X, Y> series) {
        if (begin == series) {
            begin = series.next;
        } else {
            Series ptr = begin;
            while(ptr != null && ptr.next != series) {
                ptr = ptr.next;
            }
            if (ptr != null)
            ptr.next = series.next;
        }
    }

    /**
     * XYChart maintains a list of all series currently displayed this includes all current series + any series that
     * have recently been deleted that are in the process of being faded(animated) out. This creates and returns a
     * iterator over that list. This is what implementations of XYChart should use when plotting data.
     *
     * @return iterator over currently displayed series
     */
    protected final Iterator<Series<X,Y>> getDisplayedSeriesIterator() {
        return new Iterator<Series<X, Y>>() {
            private boolean start = true;
            private Series<X,Y> current = begin;
            @Override public boolean hasNext() {
                if (start) {
                    return current != null;
                } else {
                    return current.next != null;
                }
            }
            @Override public Series<X, Y> next() {
                if (start) {
                    start = false;
                } else if (current!=null) {
                    current = current.next;
                }
                return current;
            }
            @Override public void remove() {
                throw new UnsupportedOperationException("We don't support removing items from the displayed series list.");
            }
        };
    }

    /**
     * The current displayed data value plotted on the X axis. This may be the same as xValue or different. It is
     * used by XYChart to animate the xValue from the old value to the new value. This is what you should plot
     * in any custom XYChart implementations. Some XYChart chart implementations such as LineChart also use this
     * to animate when data is added or removed.
     */
    protected final X getCurrentDisplayedXValue(Data<X,Y> item) { return item.getCurrentX(); }

    /** Set the current displayed data value plotted on X axis.
     *
     * @param item The XYChart.Data item from which the current X axis data value is obtained.
     * @see #getCurrentDisplayedXValue
     */
    protected final void setCurrentDisplayedXValue(Data<X,Y> item, X value) { item.setCurrentX(value); }

    /** The current displayed data value property that is plotted on X axis.
     *
     * @param item The XYChart.Data item from which the current X axis data value property object is obtained.
     * @return The current displayed X data value ObjectProperty.
     * @see #getCurrentDisplayedXValue
     */
    protected final ObjectProperty<X> currentDisplayedXValueProperty(Data<X,Y> item) { return item.currentXProperty(); }

    /**
     * The current displayed data value plotted on the Y axis. This may be the same as yValue or different. It is
     * used by XYChart to animate the yValue from the old value to the new value. This is what you should plot
     * in any custom XYChart implementations. Some XYChart chart implementations such as LineChart also use this
     * to animate when data is added or removed.
     */
    protected final Y getCurrentDisplayedYValue(Data<X,Y> item) { return item.getCurrentY(); }
    
    /**
     * Set the current displayed data value plotted on Y axis.
     *
     * @param item The XYChart.Data item from which the current Y axis data value is obtained.
     * @see #getCurrentDisplayedYValue
     */
    protected final void setCurrentDisplayedYValue(Data<X,Y> item, Y value) { item.setCurrentY(value); }

    /** The current displayed data value property that is plotted on Y axis.
     *
     * @param item The XYChart.Data item from which the current Y axis data value property object is obtained.
     * @return The current displayed Y data value ObjectProperty.
     * @see #getCurrentDisplayedYValue
     */
    protected final ObjectProperty<Y> currentDisplayedYValueProperty(Data<X,Y> item) { return item.currentYProperty(); }

    /**
     * The current displayed data extra value. This may be the same as extraValue or different. It is
     * used by XYChart to animate the extraValue from the old value to the new value. This is what you should plot
     * in any custom XYChart implementations.
     */
    protected final Object getCurrentDisplayedExtraValue(Data<X,Y> item) { return item.getCurrentExtraValue(); }

    /**
     * Set the current displayed data extra value.
     *
     * @param item The XYChart.Data item from which the current extra value is obtained.
     * @see #getCurrentDisplayedExtraValue
     */
    protected final void setCurrentDisplayedExtraValue(Data<X,Y> item, Object value) { item.setCurrentExtraValue(value); }

    /**
     * The current displayed extra value property.
     *
     * @param item The XYChart.Data item from which the current extra value property object is obtained.
     * @return ObjectProperty<Object> The current extra value ObjectProperty
     * @see #getCurrentDisplayedExtraValue
     */
    protected final ObjectProperty<Object> currentDisplayedExtraValueProperty(Data<X,Y> item) { return item.currentExtraValueProperty(); }

    /**
     * XYChart maintains a list of all items currently displayed this includes all current data + any data items
     * recently deleted that are in the process of being faded out. This creates and returns a iterator over
     * that list. This is what implementations of XYChart should use when plotting data.
     *
     * @param series The series to get displayed data for
     * @return iterator over currently displayed items from this series
     */
    protected final Iterator<Data<X,Y>> getDisplayedDataIterator(final Series<X,Y> series) {
        return new Iterator<Data<X, Y>>() {
            private boolean start = true;
            private Data<X,Y> current = series.begin;
            @Override public boolean hasNext() {
                if (start) {
                    return current != null;
                } else {
                    return current.next != null;
                }
            }
            @Override public Data<X, Y> next() {
                if (start) {
                    start = false;
                } else if (current!=null) {
                    current = current.next;
                }
                return current;
            }
            @Override public void remove() {
                throw new UnsupportedOperationException("We don't support removing items from the displayed data list.");
            }
        };
    }

    /**
     * This should be called from dataItemRemoved() when you are finished with any animation for deleting the item from the
     * chart. It will remove the data item from showing up in the Iterator returned by getDisplayedDataIterator().
     *
     * @param series The series to remove
     * @param item   The item to remove from series's display list
     */
    protected final void removeDataItemFromDisplay(Series<X, Y> series, Data<X, Y> item) {
        series.removeDataItemRef(item);
    }

    // -------------- STYLESHEET HANDLING ------------------------------------------------------------------------------

    private static class StyleableProperties {
        private static final CssMetaData<XYChart,Boolean> HORIZONTAL_GRID_LINE_VISIBLE =
            new CssMetaData<XYChart,Boolean>("-fx-horizontal-grid-lines-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart node) {
                return node.horizontalGridLinesVisible == null ||
                        !node.horizontalGridLinesVisible.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(XYChart node) {
                return node.horizontalGridLinesVisibleProperty();
            }
        };
        
        private static final CssMetaData<XYChart,Boolean> HORIZONTAL_ZERO_LINE_VISIBLE =
            new CssMetaData<XYChart,Boolean>("-fx-horizontal-zero-line-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart node) {
                return node.horizontalZeroLineVisible == null ||
                        !node.horizontalZeroLineVisible.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(XYChart node) {
                return node.horizontalZeroLineVisibleProperty();
            }
        };
        
        private static final CssMetaData<XYChart,Boolean> ALTERNATIVE_ROW_FILL_VISIBLE =
            new CssMetaData<XYChart,Boolean>("-fx-alternative-row-fill-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart node) {
                return node.alternativeRowFillVisible == null ||
                        !node.alternativeRowFillVisible.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(XYChart node) {
                return node.alternativeRowFillVisibleProperty();
            }
        };
        
        private static final CssMetaData<XYChart,Boolean> VERTICAL_GRID_LINE_VISIBLE =
            new CssMetaData<XYChart,Boolean>("-fx-vertical-grid-lines-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart node) {
                return node.verticalGridLinesVisible == null ||
                        !node.verticalGridLinesVisible.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(XYChart node) {
                return node.verticalGridLinesVisibleProperty();
            }
        };
        
        private static final CssMetaData<XYChart,Boolean> VERTICAL_ZERO_LINE_VISIBLE =
            new CssMetaData<XYChart,Boolean>("-fx-vertical-zero-line-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart node) {
                return node.verticalZeroLineVisible == null ||
                        !node.verticalZeroLineVisible.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(XYChart node) {
                return node.verticalZeroLineVisibleProperty();
            }
        };
        
        private static final CssMetaData<XYChart,Boolean> ALTERNATIVE_COLUMN_FILL_VISIBLE =
            new CssMetaData<XYChart,Boolean>("-fx-alternative-column-fill-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart node) {
                return node.alternativeColumnFillVisible == null ||
                        !node.alternativeColumnFillVisible.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(XYChart node) {
                return node.alternativeColumnFillVisibleProperty();
            }
        };

        private static final List<CssMetaData> STYLEABLES;
        static {
            final List<CssMetaData> styleables = 
                new ArrayList<CssMetaData>(Chart.getClassCssMetaData());
            Collections.addAll(styleables,
                HORIZONTAL_GRID_LINE_VISIBLE,
                HORIZONTAL_ZERO_LINE_VISIBLE,
                ALTERNATIVE_ROW_FILL_VISIBLE,
                VERTICAL_GRID_LINE_VISIBLE,
                VERTICAL_ZERO_LINE_VISIBLE,
                ALTERNATIVE_COLUMN_FILL_VISIBLE
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

    // -------------- INNER CLASSES ------------------------------------------------------------------------------------

    /**
     * A single data item with data for 2 axis charts
     */
    public final static class Data<X,Y> {
        // -------------- PUBLIC PROPERTIES ----------------------------------------

        private boolean setToRemove = false;
        /** The series this data belongs to */
        private Series<X,Y> series;
        private void setSeries(Series<X,Y> series) {
            this.series = series;
        }

        /** The generic data value to be plotted on the X axis */
        private ObjectProperty<X> xValue = new ObjectPropertyBase<X>() {
            @Override protected void invalidated() {
                // Note: calling get to make non-lazy, replace with change listener when available
                get();
                if (series!=null) {
                    XYChart<X,Y> chart = series.getChart();
                    if(chart!=null) chart.dataXValueChanged(Data.this);
                } else {
                    // data has not been added to series yet :
                    // so currentX and X should be the same
                    setCurrentX(get());
                }
            }

            @Override
            public Object getBean() {
                return Data.this;
            }

            @Override
            public String getName() {
                return "XValue";
            }
        };
        /**
         * Gets the generic data value to be plotted on the X axis.
         * @return the generic data value to be plotted on the X axis.
         */
        public final X getXValue() { return xValue.get(); }
        /**
         * Sets the generic data value to be plotted on the X axis.
         * @param value the generic data value to be plotted on the X axis.
         */
        public final void setXValue(X value) {
            xValue.set(value);
            // handle the case where this is a init because the default constructor was used
            if (currentX.get() == null) currentX.setValue(value);
        }
        /** 
         * The generic data value to be plotted on the X axis.
         * @return The XValue property         
         */
        public final ObjectProperty<X> XValueProperty() { return xValue; }

        /** The generic data value to be plotted on the Y axis */
        private ObjectProperty<Y> yValue = new ObjectPropertyBase<Y>() {
            @Override protected void invalidated() {
                // Note: calling get to make non-lazy, replace with change listener when available
                get();
                if (series!=null) {
                    XYChart<X,Y> chart = series.getChart();
                    if(chart!=null) chart.dataYValueChanged(Data.this);
                } else {
                    // data has not been added to series yet :
                    // so currentY and Y should be the same
                    setCurrentY(get());
                }
            }

            @Override
            public Object getBean() {
                return Data.this;
            }

            @Override
            public String getName() {
                return "YValue";
            }
        };
        /**
         * Gets the generic data value to be plotted on the Y axis.
         * @return the generic data value to be plotted on the Y axis.
         */
        public final Y getYValue() { return yValue.get(); }
        /**
         * Sets the generic data value to be plotted on the Y axis.
         * @param value the generic data value to be plotted on the Y axis.
         */
        public final void setYValue(Y value) {
            yValue.set(value);
            // handle the case where this is a init because the default constructor was used
            if (currentY.get() == null) currentY.setValue(value);
        }
        /** 
         * The generic data value to be plotted on the Y axis.
         * @return the YValue property
         */
        public final ObjectProperty<Y> YValueProperty() { return yValue; }

        /**
         * The generic data value to be plotted in any way the chart needs. For example used as the radius
         * for BubbleChart.
         */
        private ObjectProperty<Object> extraValue = new ObjectPropertyBase<Object>() {
            @Override protected void invalidated() {
                // Note: calling get to make non-lazy, replace with change listener when available
                get();
                if (series!=null) {
                    XYChart<X,Y> chart = series.getChart();
                    if(chart!=null) chart.dataExtraValueChanged(Data.this);
                }
            }

            @Override
            public Object getBean() {
                return Data.this;
            }

            @Override
            public String getName() {
                return "extraValue";
            }
        };
        public final Object getExtraValue() { return extraValue.get(); }
        public final void setExtraValue(Object value) { extraValue.set(value); }
        public final ObjectProperty<Object> extraValueProperty() { return extraValue; }

        /**
         * The node to display for this data item. You can either create your own node and set it on the data item
         * before you add the item to the chart. Otherwise the chart will create a node for you that has the default
         * representation for the chart type. This node will be set as soon as the data is added to the chart. You can
         * then get it to add mouse listeners etc. Charts will do their best to position and size the node
         * appropriately, for example on a Line or Scatter chart this node will be positioned centered on the data
         * values position. For a bar chart this is positioned and resized as the bar for this data item.
         */
        private ObjectProperty<Node> node = new SimpleObjectProperty<Node>(this, "node");
        public final Node getNode() { return node.get(); }
        public final void setNode(Node value) { node.set(value); }
        public final ObjectProperty<Node> nodeProperty() { return node; }

        /**
         * The current displayed data value plotted on the X axis. This may be the same as xValue or different. It is
         * used by XYChart to animate the xValue from the old value to the new value. This is what you should plot
         * in any custom XYChart implementations. Some XYChart chart implementations such as LineChart also use this
         * to animate when data is added or removed.
         */
        private ObjectProperty<X> currentX = new SimpleObjectProperty<X>(this, "currentX");
        final X getCurrentX() { return currentX.get(); }
        final void setCurrentX(X value) { currentX.set(value); }
        final ObjectProperty<X> currentXProperty() { return currentX; }

        /**
         * The current displayed data value plotted on the Y axis. This may be the same as yValue or different. It is
         * used by XYChart to animate the yValue from the old value to the new value. This is what you should plot
         * in any custom XYChart implementations. Some XYChart chart implementations such as LineChart also use this
         * to animate when data is added or removed.
         */
        private ObjectProperty<Y> currentY = new SimpleObjectProperty<Y>(this, "currentY");
        final Y getCurrentY() { return currentY.get(); }
        final void setCurrentY(Y value) { currentY.set(value); }
        final ObjectProperty<Y> currentYProperty() { return currentY; }

        /**
         * The current displayed data extra value. This may be the same as extraValue or different. It is
         * used by XYChart to animate the extraValue from the old value to the new value. This is what you should plot
         * in any custom XYChart implementations.
         */
        private ObjectProperty<Object> currentExtraValue = new SimpleObjectProperty<Object>(this, "currentExtraValue");
        final Object getCurrentExtraValue() { return currentExtraValue.getValue(); }
        final void setCurrentExtraValue(Object value) { currentExtraValue.setValue(value); }
        final ObjectProperty<Object> currentExtraValueProperty() { return currentExtraValue; }

        /**
         * Next pointer for the next data item. We maintain a linkedlist of the
         * data items so even after the data is deleted from the list,
         * we have a reference to it
         */
         protected Data<X,Y> next = null;

        // -------------- CONSTRUCTOR -------------------------------------------------

        /**
         * Creates an empty XYChart.Data object.
         */
        public Data() {}

        /**
         * Creates an instance of XYChart.Data object and initializes the X,Y
         * data values.
         * 
         * @param xValue The X axis data value
         * @param yValue The Y axis data value
         */
        public Data(X xValue, Y yValue) {
            setXValue(xValue);
            setYValue(yValue);
            setCurrentX(xValue);
            setCurrentY(yValue);
        }

        /**
         * Creates an instance of XYChart.Data object and initializes the X,Y
         * data values and extraValue.
         *
         * @param xValue The X axis data value.
         * @param yValue The Y axis data value.
         * @param extraValue Chart extra value.
         */
        public Data(X xValue, Y yValue, Object extraValue) {
            setXValue(xValue);
            setYValue(yValue);
            setExtraValue(extraValue);
            setCurrentX(xValue);
            setCurrentY(yValue);
            setCurrentExtraValue(extraValue);
        }

        // -------------- PUBLIC METHODS ----------------------------------------------

        /**
         * Returns a string representation of this {@code Data} object.
         * @return a string representation of this {@code Data} object.
         */ 
        @Override public String toString() {
            return "Data["+getXValue()+","+getYValue()+","+getExtraValue()+"]";
        }

    }

    /**
     * A named series of data items
     */
    public static final class Series<X,Y> {

        // -------------- PRIVATE PROPERTIES ----------------------------------------

        /** the style class for default color for this series */
        String defaultColorStyleClass;

        Data<X,Y> begin = null; // start pointer of a data linked list.
        /*
         * Next pointer for the next series. We maintain a linkedlist of the
         * serieses  so even after the series is deleted from the list,
         * we have a reference to it - needed by BarChart e.g.
         */
        Series<X,Y> next = null;

        private final ListChangeListener<Data<X,Y>> dataChangeListener = new ListChangeListener<Data<X, Y>>() {
            @Override public void onChanged(Change<? extends Data<X, Y>> c) {
                while (c.next()) {
                    // RT-25187 Probably a sort happened, just reorder the pointers and return.
                    if (c.wasPermutated()) {
                        Series<X,Y> series = Series.this;
                        if (series == null || series.getData() == null) return;
                        Data<X,Y> ptr = begin;
                        for(int i = 0; i < series.getData().size(); i++) {
                            Data<X,Y> item = series.getData().get(i);
                            if (i == 0) {
                                begin = item;
                                ptr = begin;
                                begin.next = null;
                            } else {
                                ptr.next = item;
                                item.next = null;
                                ptr = item;
                            }
                        }
                        return;
                    }
                    // update data items reference to series
                    for (Data<X,Y> item : c.getRemoved()) {
                        item.setSeries(null);
                        item.setToRemove = true;
                    }
                    if (c.getAddedSize() > 0) {
                        for (Data<X,Y> itemPtr = begin; itemPtr != null; itemPtr = itemPtr.next) {
                            if (itemPtr.setToRemove) {
                                removeDataItemRef(itemPtr);
                            }
                        }
                    }
                    for(int i=c.getFrom(); i<c.getTo(); i++) {
                        getData().get(i).setSeries(Series.this);
                        // update linkedList Pointers for data in this series
                        if (begin == null) {
                            begin = getData().get(i);
                            begin.next = null;
                        } else {
                            if (i == 0) {
                                getData().get(0).next = begin;
                                begin = getData().get(0);
                            } else {
                                Data<X,Y> ptr = begin;
                                for (int j = 0; j < i -1 ; j++) {
                                    ptr = ptr.next;
                                }
                                getData().get(i).next = ptr.next;
                                ptr.next = getData().get(i);
                            }
                        }
                    }
                    // inform chart
                    XYChart<X,Y> chart = getChart();
                    if(chart!=null) chart.dataItemsChanged(Series.this,
                            (List<Data<X,Y>>)c.getRemoved(), c.getFrom(), c.getTo(), c.wasPermutated());
                }
            }
        };

        // -------------- PUBLIC PROPERTIES ----------------------------------------

        /** Reference to the chart this series belongs to */
        private final ReadOnlyObjectWrapper<XYChart<X,Y>> chart = new ReadOnlyObjectWrapper<XYChart<X,Y>>(this, "chart");
        public final XYChart<X,Y> getChart() { return chart.get(); }
        private void setChart(XYChart<X,Y> value) { chart.set(value); }
        public final ReadOnlyObjectProperty<XYChart<X,Y>> chartProperty() { return chart.getReadOnlyProperty(); }

        /** The user displayable name for this series */
        private final StringProperty name = new StringPropertyBase() {
            @Override protected void invalidated() {
                get(); // make non-lazy
                if(getChart() != null) getChart().seriesNameChanged();
            }

            @Override
            public Object getBean() {
                return Series.this;
            }

            @Override
            public String getName() {
                return "name";
            }
        };
        public final String getName() { return name.get(); }
        public final void setName(String value) { name.set(value); }
        public final StringProperty nameProperty() { return name; }

        /**
         * The node to display for this series. This is created by the chart if it uses nodes to represent the whole
         * series. For example line chart uses this for the line but scatter chart does not use it. This node will be
         * set as soon as the series is added to the chart. You can then get it to add mouse listeners etc.
         */
        private ObjectProperty<Node> node = new SimpleObjectProperty<Node>(this, "node");
        public final Node getNode() { return node.get(); }
        public final void setNode(Node value) { node.set(value); }
        public final ObjectProperty<Node> nodeProperty() { return node; }

        /** ObservableList of data items that make up this series */
        private final ObjectProperty<ObservableList<Data<X,Y>>> data = new ObjectPropertyBase<ObservableList<Data<X,Y>>>() {
            private ObservableList<Data<X,Y>> old;
            @Override protected void invalidated() {
                final ObservableList<Data<X,Y>> current = getValue();
                // add remove listeners
                if(old != null) old.removeListener(dataChangeListener);
                if(current != null) current.addListener(dataChangeListener);
                // fire data change event if series are added or removed
                if(old != null || current != null) {
                    final List<Data<X,Y>> removed = (old != null) ? old : Collections.<Data<X,Y>>emptyList();
                    final int toIndex = (current != null) ? current.size() : 0;
                    // let data listener know all old data have been removed and new data that has been added
                    if (toIndex > 0 || !removed.isEmpty()) {
                        dataChangeListener.onChanged(new NonIterableChange<Data<X,Y>>(0, toIndex, current){
                            @Override public List<Data<X,Y>> getRemoved() { return removed; }

                            @Override protected int[] getPermutation() {
                                return new int[0];
                            }
                        });
                    }
                } else if (old != null && old.size() > 0) {
                    // let series listener know all old series have been removed
                    dataChangeListener.onChanged(new NonIterableChange<Data<X,Y>>(0, 0, current){
                        @Override public List<Data<X,Y>> getRemoved() { return old; }
                        @Override protected int[] getPermutation() {
                            return new int[0];
                        }
                    });
                }
                old = current;
            }

            @Override
            public Object getBean() {
                return Series.this;
            }

            @Override
            public String getName() {
                return "data";
            }
        };
        public final ObservableList<Data<X,Y>> getData() { return data.getValue(); }
        public final void setData(ObservableList<Data<X,Y>> value) { data.setValue(value); }
        public final ObjectProperty<ObservableList<Data<X,Y>>> dataProperty() { return data; }

        // -------------- CONSTRUCTORS ----------------------------------------------

        /**
         * Construct a empty series
         */
        public Series() {
            this(FXCollections.<Data<X,Y>>observableArrayList());
        }

        /**
         * Constructs a Series and populates it with the given {@link ObservableList} data.
         *
         * @param data ObservableList of XYChart.Data
         */
        public Series(ObservableList<Data<X,Y>> data) {
            setData(data);
            for(Data<X,Y> item:data) item.setSeries(this);
        }

        /**
         * Constructs a named Series and populates it with the given {@link ObservableList} data.
         *
         * @param name a name for the series
         * @param data ObservableList of XYChart.Data
         */
        public Series(String name, ObservableList<Data<X,Y>> data) {
            this(data);
            setName(name);
        }

        // -------------- PUBLIC METHODS ----------------------------------------------

        /**
         * Returns a string representation of this {@code Series} object.
         * @return a string representation of this {@code Series} object.
         */ 
        @Override public String toString() {
            return "Series["+getName()+"]";
        }

        // -------------- PRIVATE/PROTECTED METHODS -----------------------------------

        /*
         * The following methods are for manipulating the pointers in the linked list
         * when data is deleted. 
         */
        private void removeDataItemRef(Data<X,Y> item) {
            if (begin == item) {
                begin = item.next;
            } else {
                Data<X,Y> ptr = begin;
                while(ptr != null && ptr.next != item) {
                    ptr = ptr.next;
                }
                if(ptr != null) ptr.next = item.next;
            }
        }

        int getItemIndex(Data<X,Y> item) {
            int itemIndex = 0;
            for (Data<X,Y> d = begin; d != null; d = d.next) {
                if (d == item) break;
                itemIndex++;
            }
            return itemIndex;
        }

        int getDataSize() {
            int count = 0;
            for (Data<X,Y> d = begin; d != null; d = d.next) {
                count++;
            }
            return count;
        }
    }

}
