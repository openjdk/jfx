/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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


import com.sun.javafx.charts.Legend;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Orientation;
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

import com.sun.javafx.collections.NonIterableChange;
import javafx.css.converter.BooleanConverter;



/**
 * Chart base class for all 2 axis charts. It is responsible for drawing the two
 * axes and the plot content. It contains a list of all content in the plot and
 * implementations of XYChart can add nodes to this list that need to be rendered.
 *
 * <p>It is possible to install Tooltips on data items / symbols.
 * For example the following code snippet installs Tooltip on the 1st data item.
 *
 * <pre><code>
 *  XYChart.Data item = ( XYChart.Data)series.getData().get(0);
 *  Tooltip.install(item.getNode(), new Tooltip("Symbol-0"));
 * </code></pre>
 *
 * @since JavaFX 2.0
 */
public abstract class XYChart<X,Y> extends Chart {

    // -------------- PRIVATE FIELDS -----------------------------------------------------------------------------------

    // to indicate which colors are being used for the series
    private final BitSet colorBits = new BitSet(8);
    static String DEFAULT_COLOR = "default-color";
    final Map<Series<X,Y>, Integer> seriesColorMap = new HashMap<>();
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

    private final List<Series<X, Y>> displayedSeries = new ArrayList<>();
    private Legend legend = new Legend();

    /** This is called when a series is added or removed from the chart */
    private final ListChangeListener<Series<X,Y>> seriesChanged = c -> {
        ObservableList<? extends Series<X, Y>> series = c.getList();
        while (c.next()) {
            // RT-12069, linked list pointers should update when list is permutated.
            if (c.wasPermutated()) {
                displayedSeries.sort((o1, o2) -> series.indexOf(o2) - series.indexOf(o1));

            }

            if (c.getRemoved().size() > 0) updateLegend();

            Set<Series<X, Y>> dupCheck = new HashSet<>(displayedSeries);
            dupCheck.removeAll(c.getRemoved());
            for (Series<X, Y> d : c.getAddedSubList()) {
                if (!dupCheck.add(d)) {
                    throw new IllegalArgumentException("Duplicate series added");
                }
            }

            for (Series<X,Y> s : c.getRemoved()) {
                s.setToRemove = true;
                seriesRemoved(s);
            }

            for(int i=c.getFrom(); i<c.getTo() && !c.wasPermutated(); i++) {
                final Series<X,Y> s = c.getList().get(i);
                // add new listener to data
                s.setChart(XYChart.this);
                if (s.setToRemove) {
                    s.setToRemove = false;
                    s.getChart().seriesBeingRemovedIsAdded(s);
                }
                // update linkedList Pointers for series
                displayedSeries.add(s);
                // update default color style class
                int nextClearBit = colorBits.nextClearBit(0);
                colorBits.set(nextClearBit, true);
                s.defaultColorStyleClass = DEFAULT_COLOR+(nextClearBit%8);
                seriesColorMap.put(s, nextClearBit%8);
                // inform sub-classes of series added
                seriesAdded(s, i);
            }
            if (c.getFrom() < c.getTo()) updateLegend();
            seriesChanged(c);

        }
        // update axis ranges
        invalidateRange();
        // lay everything out
        requestChartLayout();
    };

    // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

    private final Axis<X> xAxis;
    /**
     * Get the X axis, by default it is along the bottom of the plot
     * @return the X axis of the chart
     */
    public Axis<X> getXAxis() { return xAxis; }

    private final Axis<Y> yAxis;
    /**
     * Get the Y axis, by default it is along the left of the plot
     * @return the Y axis of this chart
     */
    public Axis<Y> getYAxis() { return yAxis; }

    /** XYCharts data */
    private ObjectProperty<ObservableList<Series<X,Y>>> data = new ObjectPropertyBase<ObservableList<Series<X,Y>>>() {
        private ObservableList<Series<X,Y>> old;
        @Override protected void invalidated() {
            final ObservableList<Series<X,Y>> current = getValue();
            if (current == old) return;
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
        public CssMetaData<XYChart<?,?>,Boolean> getCssMetaData() {
            return StyleableProperties.VERTICAL_GRID_LINE_VISIBLE;
        }
    };
    /**
     * Indicates whether vertical grid lines are visible or not.
     *
     * @return true if verticalGridLines are visible else false.
     * @see #verticalGridLinesVisibleProperty()
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
        public CssMetaData<XYChart<?,?>,Boolean> getCssMetaData() {
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
        public CssMetaData<XYChart<?,?>,Boolean> getCssMetaData() {
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
        public CssMetaData<XYChart<?,?>,Boolean> getCssMetaData() {
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
        public CssMetaData<XYChart<?,?>,Boolean> getCssMetaData() {
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
        public CssMetaData<XYChart<?,?>,Boolean> getCssMetaData() {
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
        if (xAxis.getSide() == null) xAxis.setSide(Side.BOTTOM);
        xAxis.setEffectiveOrientation(Orientation.HORIZONTAL);
        this.yAxis = yAxis;
        if (yAxis.getSide() == null) yAxis.setSide(Side.LEFT);
        yAxis.setEffectiveOrientation(Orientation.VERTICAL);
        // RT-23123 autoranging leads to charts incorrect appearance.
        xAxis.autoRangingProperty().addListener((ov, t, t1) -> {
            updateAxisRange();
        });
        yAxis.autoRangingProperty().addListener((ov, t, t1) -> {
            updateAxisRange();
        });
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
        animatedProperty().addListener((valueModel, oldValue, newValue) -> {
            if(getXAxis() != null) getXAxis().setAnimated(newValue);
            if(getYAxis() != null) getYAxis().setAnimated(newValue);
        });
        setLegend(legend);
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

    private <T> void dataValueChanged(Data<X,Y> item, T newValue, ObjectProperty<T> currentValueProperty) {
        if (currentValueProperty.get() != newValue) invalidateRange();
        dataItemChanged(item);
        if (shouldAnimate()) {
            animate(
                    new KeyFrame(Duration.ZERO, new KeyValue(currentValueProperty, currentValueProperty.get())),
                    new KeyFrame(Duration.millis(700), new KeyValue(currentValueProperty, newValue, Interpolator.EASE_BOTH))
            );
        } else {
            currentValueProperty.set(newValue);
            requestChartLayout();
        }
    }

    /**
     * This is called whenever a series is added or removed and the legend needs to be updated
     */
    protected void updateLegend() {
        List<Legend.LegendItem> legendList = new ArrayList<>();
        if (getData() != null) {
            for (int seriesIndex = 0; seriesIndex < getData().size(); seriesIndex++) {
                Series<X, Y> series = getData().get(seriesIndex);
                legendList.add(createLegendItemForSeries(series, seriesIndex));
            }
        }
        legend.getItems().setAll(legendList);
        if (legendList.size() > 0) {
            if (getLegend() == null) {
                setLegend(legend);
            }
        } else {
            setLegend(null);
        }
    }

    /**
     * Called by the updateLegend for each series in the chart in order to
     * create new legend item
     * @param series the series for this legend item
     * @param seriesIndex the index of the series
     * @return new legend item for this series
     */
    Legend.LegendItem createLegendItemForSeries(Series<X, Y> series, int seriesIndex) {
        return new Legend.LegendItem(series.getName());
    }

    /**
     * This method is called when there is an attempt to add series that was
     * set to be removed, and the removal might not have completed.
     * @param series
     */
    void seriesBeingRemovedIsAdded(Series<X,Y> series) {}

    /**
     * This method is called when there is an attempt to add a Data item that was
     * set to be removed, and the removal might not have completed.
     * @param data
     */
    void dataBeingRemovedIsAdded(Data<X,Y> item, Series<X,Y> series) {}
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

    /**
     * Called when each atomic change is made to the list of series for this chart
     * @param c a Change instance representing the changes to the series
     */
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

    /** {@inheritDoc} */
    @Override protected final void layoutChartChildren(double top, double left, double width, double height) {
        if(getData() == null) return;
        if (!rangeValid) {
            rangeValid = true;
            if(getData() != null) updateAxisRange();
        }
        // snap top and left to pixels
        top = snapPositionY(top);
        left = snapPositionX(left);
        // get starting stuff
        final Axis<X> xa = getXAxis();
        final ObservableList<Axis.TickMark<X>> xaTickMarks = xa.getTickMarks();
        final Axis<Y> ya = getYAxis();
        final ObservableList<Axis.TickMark<Y>> yaTickMarks = ya.getTickMarks();
        // check we have 2 axises and know their sides
        if (xa == null || ya == null) return;
        // try and work out width and height of axises
        double xAxisWidth = 0;
        double xAxisHeight = 30; // guess x axis height to start with
        double yAxisWidth = 0;
        double yAxisHeight = 0;
        for (int count=0; count<5; count ++) {
            yAxisHeight = snapSizeY(height - xAxisHeight);
            if (yAxisHeight < 0) {
                yAxisHeight = 0;
            }
            yAxisWidth = ya.prefWidth(yAxisHeight);
            xAxisWidth = snapSizeX(width - yAxisWidth);
            if (xAxisWidth < 0) {
                xAxisWidth = 0;
            }
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
        switch(xa.getEffectiveSide()) {
            case TOP:
                xa.setVisible(true);
                xAxisY = top+1;
                top += xAxisHeight;
                break;
            case BOTTOM:
                xa.setVisible(true);
                xAxisY = top + yAxisHeight;
        }

        // calc yAxis width
        double yAxisX = 0;
        switch(ya.getEffectiveSide()) {
            case LEFT:
                ya.setVisible(true);
                yAxisX = left +1;
                left += yAxisWidth;
                break;
            case RIGHT:
                ya.setVisible(true);
                yAxisX = left + xAxisWidth;
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
                final double x = xa.getDisplayPosition(tick.getValue());
                if ((x!=xAxisZero || !isVerticalZeroLineVisible()) && x > 0 && x <= xAxisWidth) {
                    verticalGridLines.getElements().add(new MoveTo(left+x+0.5,top));
                    verticalGridLines.getElements().add(new LineTo(left+x+0.5,top+yAxisHeight));
                }
            }
        }
        // update horizontal grid lines
        horizontalGridLines.getElements().clear();
        if(isHorizontalGridLinesVisible()) {
            for(int i=0; i < yaTickMarks.size(); i++) {
                Axis.TickMark<Y> tick = yaTickMarks.get(i);
                final double y = ya.getDisplayPosition(tick.getValue());
                if ((y!=yAxisZero || !isHorizontalZeroLineVisible()) && y >= 0 && y < yAxisHeight) {
                    horizontalGridLines.getElements().add(new MoveTo(left,top+y+0.5));
                    horizontalGridLines.getElements().add(new LineTo(left+xAxisWidth,top+y+0.5));
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
    int getSeriesIndex(Series<X,Y> series) {
        return displayedSeries.indexOf(series);
    }

    /**
     * Computes the size of series linked list
     * @return size of series linked list
     */
    int getSeriesSize() {
        return displayedSeries.size();
    }

    /**
     * This should be called from seriesRemoved() when you are finished with any animation for deleting the series from
     * the chart. It will remove the series from showing up in the Iterator returned by getDisplayedSeriesIterator().
     *
     * @param series The series to remove
     */
    protected final void removeSeriesFromDisplay(Series<X, Y> series) {
        if (series != null) series.setToRemove = false;
        series.setChart(null);
        displayedSeries.remove(series);
        int idx = seriesColorMap.remove(series);
        colorBits.clear(idx);
    }

    /**
     * XYChart maintains a list of all series currently displayed this includes all current series + any series that
     * have recently been deleted that are in the process of being faded(animated) out. This creates and returns a
     * iterator over that list. This is what implementations of XYChart should use when plotting data.
     *
     * @return iterator over currently displayed series
     */
    protected final Iterator<Series<X,Y>> getDisplayedSeriesIterator() {
        return Collections.unmodifiableList(displayedSeries).iterator();
    }

    /**
     * Creates an array of KeyFrames for fading out nodes representing a series
     *
     * @param series The series to remove
     * @param fadeOutTime Time to fade out, in milliseconds
     * @return array of two KeyFrames from zero to fadeOutTime
     */
    final KeyFrame[] createSeriesRemoveTimeLine(Series<X, Y> series, long fadeOutTime) {
        final List<Node> nodes = new ArrayList<>();
        nodes.add(series.getNode());
        for (Data<X, Y> d : series.getData()) {
            if (d.getNode() != null) {
                nodes.add(d.getNode());
            }
        }
        // fade out series node and symbols
        KeyValue[] startValues = new KeyValue[nodes.size()];
        KeyValue[] endValues = new KeyValue[nodes.size()];
        for (int j = 0; j < nodes.size(); j++) {
            startValues[j] = new KeyValue(nodes.get(j).opacityProperty(), 1);
            endValues[j] = new KeyValue(nodes.get(j).opacityProperty(), 0);
        }
        return new KeyFrame[] {
            new KeyFrame(Duration.ZERO, startValues),
            new KeyFrame(Duration.millis(fadeOutTime), actionEvent -> {
                getPlotChildren().removeAll(nodes);
                removeSeriesFromDisplay(series);
            }, endValues)
        };
    }

    /**
     * The current displayed data value plotted on the X axis. This may be the same as xValue or different. It is
     * used by XYChart to animate the xValue from the old value to the new value. This is what you should plot
     * in any custom XYChart implementations. Some XYChart chart implementations such as LineChart also use this
     * to animate when data is added or removed.
     * @param item The XYChart.Data item from which the current X axis data value is obtained
     * @return The current displayed X data value
     */
    protected final X getCurrentDisplayedXValue(Data<X,Y> item) { return item.getCurrentX(); }

    /** Set the current displayed data value plotted on X axis.
     *
     * @param item The XYChart.Data item from which the current X axis data value is obtained.
     * @param value The X axis data value
     * @see #getCurrentDisplayedXValue(Data)
     */
    protected final void setCurrentDisplayedXValue(Data<X,Y> item, X value) { item.setCurrentX(value); }

    /** The current displayed data value property that is plotted on X axis.
     *
     * @param item The XYChart.Data item from which the current X axis data value property object is obtained.
     * @return The current displayed X data value ObjectProperty.
     * @see #getCurrentDisplayedXValue(Data)
     */
    protected final ObjectProperty<X> currentDisplayedXValueProperty(Data<X,Y> item) { return item.currentXProperty(); }

    /**
     * The current displayed data value plotted on the Y axis. This may be the same as yValue or different. It is
     * used by XYChart to animate the yValue from the old value to the new value. This is what you should plot
     * in any custom XYChart implementations. Some XYChart chart implementations such as LineChart also use this
     * to animate when data is added or removed.
     * @param item The XYChart.Data item from which the current Y axis data value is obtained
     * @return The current displayed Y data value
     */
    protected final Y getCurrentDisplayedYValue(Data<X,Y> item) { return item.getCurrentY(); }

    /**
     * Set the current displayed data value plotted on Y axis.
     *
     * @param item The XYChart.Data item from which the current Y axis data value is obtained.
     * @param value The Y axis data value
     * @see #getCurrentDisplayedYValue(Data)
     */
    protected final void setCurrentDisplayedYValue(Data<X,Y> item, Y value) { item.setCurrentY(value); }

    /** The current displayed data value property that is plotted on Y axis.
     *
     * @param item The XYChart.Data item from which the current Y axis data value property object is obtained.
     * @return The current displayed Y data value ObjectProperty.
     * @see #getCurrentDisplayedYValue(Data)
     */
    protected final ObjectProperty<Y> currentDisplayedYValueProperty(Data<X,Y> item) { return item.currentYProperty(); }

    /**
     * The current displayed data extra value. This may be the same as extraValue or different. It is
     * used by XYChart to animate the extraValue from the old value to the new value. This is what you should plot
     * in any custom XYChart implementations.
     * @param item The XYChart.Data item from which the current extra value is obtained
     * @return The current extra value
     */
    protected final Object getCurrentDisplayedExtraValue(Data<X,Y> item) { return item.getCurrentExtraValue(); }

    /**
     * Set the current displayed data extra value.
     *
     * @param item The XYChart.Data item from which the current extra value is obtained.
     * @param value The extra value
     * @see #getCurrentDisplayedExtraValue(Data)
     */
    protected final void setCurrentDisplayedExtraValue(Data<X,Y> item, Object value) { item.setCurrentExtraValue(value); }

    /**
     * The current displayed extra value property.
     *
     * @param item The XYChart.Data item from which the current extra value property object is obtained.
     * @return {@literal ObjectProperty<Object> The current extra value ObjectProperty}
     * @see #getCurrentDisplayedExtraValue(Data)
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
        return Collections.unmodifiableList(series.displayedData).iterator();
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
        private static final CssMetaData<XYChart<?,?>,Boolean> HORIZONTAL_GRID_LINE_VISIBLE =
            new CssMetaData<XYChart<?,?>,Boolean>("-fx-horizontal-grid-lines-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart<?,?> node) {
                return node.horizontalGridLinesVisible == null ||
                        !node.horizontalGridLinesVisible.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(XYChart<?,?> node) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)node.horizontalGridLinesVisibleProperty();
            }
        };

        private static final CssMetaData<XYChart<?,?>,Boolean> HORIZONTAL_ZERO_LINE_VISIBLE =
            new CssMetaData<XYChart<?,?>,Boolean>("-fx-horizontal-zero-line-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart<?,?> node) {
                return node.horizontalZeroLineVisible == null ||
                        !node.horizontalZeroLineVisible.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(XYChart<?,?> node) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)node.horizontalZeroLineVisibleProperty();
            }
        };

        private static final CssMetaData<XYChart<?,?>,Boolean> ALTERNATIVE_ROW_FILL_VISIBLE =
            new CssMetaData<XYChart<?,?>,Boolean>("-fx-alternative-row-fill-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart<?,?> node) {
                return node.alternativeRowFillVisible == null ||
                        !node.alternativeRowFillVisible.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(XYChart<?,?> node) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)node.alternativeRowFillVisibleProperty();
            }
        };

        private static final CssMetaData<XYChart<?,?>,Boolean> VERTICAL_GRID_LINE_VISIBLE =
            new CssMetaData<XYChart<?,?>,Boolean>("-fx-vertical-grid-lines-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart<?,?> node) {
                return node.verticalGridLinesVisible == null ||
                        !node.verticalGridLinesVisible.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(XYChart<?,?> node) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)node.verticalGridLinesVisibleProperty();
            }
        };

        private static final CssMetaData<XYChart<?,?>,Boolean> VERTICAL_ZERO_LINE_VISIBLE =
            new CssMetaData<XYChart<?,?>,Boolean>("-fx-vertical-zero-line-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart<?,?> node) {
                return node.verticalZeroLineVisible == null ||
                        !node.verticalZeroLineVisible.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(XYChart<?,?> node) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)node.verticalZeroLineVisibleProperty();
            }
        };

        private static final CssMetaData<XYChart<?,?>,Boolean> ALTERNATIVE_COLUMN_FILL_VISIBLE =
            new CssMetaData<XYChart<?,?>,Boolean>("-fx-alternative-column-fill-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(XYChart<?,?> node) {
                return node.alternativeColumnFillVisible == null ||
                        !node.alternativeColumnFillVisible.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(XYChart<?,?> node) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)node.alternativeColumnFillVisibleProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Chart.getClassCssMetaData());
            styleables.add(HORIZONTAL_GRID_LINE_VISIBLE);
            styleables.add(HORIZONTAL_ZERO_LINE_VISIBLE);
            styleables.add(ALTERNATIVE_ROW_FILL_VISIBLE);
            styleables.add(VERTICAL_GRID_LINE_VISIBLE);
            styleables.add(VERTICAL_ZERO_LINE_VISIBLE);
            styleables.add(ALTERNATIVE_COLUMN_FILL_VISIBLE);
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
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    // -------------- INNER CLASSES ------------------------------------------------------------------------------------

    /**
     * A single data item with data for 2 axis charts
     * @since JavaFX 2.0
     */
    public final static class Data<X,Y> {
        // -------------- PUBLIC PROPERTIES ----------------------------------------

        private boolean setToRemove = false;
        /** The series this data belongs to */
        private Series<X,Y> series;
        void setSeries(Series<X,Y> series) {
            this.series = series;
        }

        /** The generic data value to be plotted on the X axis */
        private ObjectProperty<X> xValue = new SimpleObjectProperty<X>(Data.this, "XValue") {
            @Override protected void invalidated() {
                if (series!=null) {
                    XYChart<X,Y> chart = series.getChart();
                    if(chart!=null) chart.dataValueChanged(Data.this, get(), currentXProperty());
                } else {
                    // data has not been added to series yet :
                    // so currentX and X should be the same
                    setCurrentX(get());
                }
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
            // and the case when series is not associated to a chart due to a remove series
            if (currentX.get() == null ||
                    (series != null && series.getChart() == null)) currentX.setValue(value);
        }
        /**
         * The generic data value to be plotted on the X axis.
         * @return The XValue property
         */
        public final ObjectProperty<X> XValueProperty() { return xValue; }

        /** The generic data value to be plotted on the Y axis */
        private ObjectProperty<Y> yValue = new SimpleObjectProperty<Y>(Data.this, "YValue") {
            @Override protected void invalidated() {
                if (series!=null) {
                    XYChart<X,Y> chart = series.getChart();
                    if(chart!=null) chart.dataValueChanged(Data.this, get(), currentYProperty());
                } else {
                    // data has not been added to series yet :
                    // so currentY and Y should be the same
                    setCurrentY(get());
                }
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
            // and the case when series is not associated to a chart due to a remove series
            if (currentY.get() == null ||
                    (series != null && series.getChart() == null)) currentY.setValue(value);

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
        private ObjectProperty<Object> extraValue = new SimpleObjectProperty<Object>(Data.this, "extraValue") {
            @Override protected void invalidated() {
                if (series!=null) {
                    XYChart<X,Y> chart = series.getChart();
                    if(chart!=null) chart.dataValueChanged(Data.this, get(), currentExtraValueProperty());
                }
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
        private ObjectProperty<Node> node = new SimpleObjectProperty<Node>(this, "node") {
            protected void invalidated() {
                Node node = get();
                if (node != null) {
                    node.accessibleTextProperty().unbind();
                    node.accessibleTextProperty().bind(new StringBinding() {
                        {bind(currentXProperty(), currentYProperty());}
                        @Override protected String computeValue() {
                            String seriesName = series != null ? series.getName() : "";
                            return seriesName + " X Axis is " + getCurrentX() + " Y Axis is " + getCurrentY();
                        }
                    });
                }
            };
        };
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
     * @since JavaFX 2.0
     */
    public static final class Series<X,Y> {

        // -------------- PRIVATE PROPERTIES ----------------------------------------

        /** the style class for default color for this series */
        String defaultColorStyleClass;
        boolean setToRemove = false;

        private List<Data<X, Y>> displayedData = new ArrayList<>();

        private final ListChangeListener<Data<X,Y>> dataChangeListener = new ListChangeListener<Data<X, Y>>() {
            @Override public void onChanged(Change<? extends Data<X, Y>> c) {
                ObservableList<? extends Data<X, Y>> data = c.getList();
                final XYChart<X, Y> chart = getChart();
                while (c.next()) {
                    if (chart != null) {
                        // RT-25187 Probably a sort happened, just reorder the pointers and return.
                        if (c.wasPermutated()) {
                            displayedData.sort((o1, o2) -> data.indexOf(o2) - data.indexOf(o1));
                            return;
                        }

                        Set<Data<X, Y>> dupCheck = new HashSet<>(displayedData);
                        dupCheck.removeAll(c.getRemoved());
                        for (Data<X, Y> d : c.getAddedSubList()) {
                            if (!dupCheck.add(d)) {
                                throw new IllegalArgumentException("Duplicate data added");
                            }
                        }

                        // update data items reference to series
                        for (Data<X, Y> item : c.getRemoved()) {
                            item.setToRemove = true;
                        }

                        if (c.getAddedSize() > 0) {
                            for (Data<X, Y> itemPtr : c.getAddedSubList()) {
                                if (itemPtr.setToRemove) {
                                    if (chart != null) chart.dataBeingRemovedIsAdded(itemPtr, Series.this);
                                    itemPtr.setToRemove = false;
                                }
                            }

                            for (Data<X, Y> d : c.getAddedSubList()) {
                                d.setSeries(Series.this);
                            }
                            if (c.getFrom() == 0) {
                                displayedData.addAll(0, c.getAddedSubList());
                            } else {
                                displayedData.addAll(displayedData.indexOf(data.get(c.getFrom() - 1)) + 1, c.getAddedSubList());
                            }
                        }
                        // inform chart
                        chart.dataItemsChanged(Series.this,
                                (List<Data<X, Y>>) c.getRemoved(), c.getFrom(), c.getTo(), c.wasPermutated());
                    } else {
                        Set<Data<X, Y>> dupCheck = new HashSet<>();
                        for (Data<X, Y> d : data) {
                            if (!dupCheck.add(d)) {
                                throw new IllegalArgumentException("Duplicate data added");
                            }
                        }

                        for (Data<X, Y> d : c.getAddedSubList()) {
                            d.setSeries(Series.this);
                        }

                    }
                }
            }
        };

        // -------------- PUBLIC PROPERTIES ----------------------------------------

        /** Reference to the chart this series belongs to */
        private final ReadOnlyObjectWrapper<XYChart<X,Y>> chart = new ReadOnlyObjectWrapper<XYChart<X,Y>>(this, "chart") {
            @Override
            protected void invalidated() {
                if (get() == null) {
                    displayedData.clear();
                } else {
                    displayedData.addAll(getData());
                }
            }
        };
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
            if (item != null) item.setToRemove = false;
            displayedData.remove(item);
        }

        int getItemIndex(Data<X,Y> item) {
            return displayedData.indexOf(item);
        }

        Data<X, Y> getItem(int i) {
            return displayedData.get(i);
        }

        int getDataSize() {
            return displayedData.size();
        }
    }

}
