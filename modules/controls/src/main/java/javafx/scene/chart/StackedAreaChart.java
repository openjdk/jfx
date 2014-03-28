/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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


import java.util.*;
import javafx.animation.*;
import javafx.beans.InvalidationListener;
import javafx.beans.NamedArg;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.*;
import javafx.util.Duration;
import com.sun.javafx.charts.Legend;
import com.sun.javafx.charts.Legend.LegendItem;
import com.sun.javafx.css.converters.BooleanConverter;
import javafx.beans.property.BooleanProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableProperty;

/**
 * StackedAreaChart is a variation of {@link AreaChart} that displays trends of the 
 * contribution of each value. (over time e.g.) The areas are stacked so that each 
 * series adjoins but does not overlap the preceding series. This contrasts with 
 * the Area chart where each series overlays the preceding series. 
 * 
 * The cumulative nature of the StackedAreaChart gives an idea of the total Y data 
 * value at any given point along the X axis.
 * 
 * Since data points across multiple series may not be common, StackedAreaChart
 * interpolates values along the line joining the data points whenever necessary.
 * 
 * @since JavaFX 2.1
 */
public class StackedAreaChart<X,Y> extends XYChart<X,Y> {

    // -------------- PRIVATE FIELDS ------------------------------------------

    /** A multiplier for teh Y values that we store for each series, it is used to animate in a new series */
    private Map<Series, DoubleProperty> seriesYMultiplierMap = new HashMap<Series, DoubleProperty>();
    private Legend legend = new Legend();

    // -------------- PUBLIC PROPERTIES ----------------------------------------
    /**
     * When true, CSS styleable symbols are created for any data items that
     * don't have a symbol node specified.
     * @since JavaFX 8.0
     */
    private BooleanProperty createSymbols = new StyleableBooleanProperty(true) {
        @Override
        protected void invalidated() {
            for (int seriesIndex = 0; seriesIndex < getData().size(); seriesIndex++) {
                Series<X, Y> series = getData().get(seriesIndex);
                for (int itemIndex = 0; itemIndex < series.getData().size(); itemIndex++) {
                    Data<X, Y> item = series.getData().get(itemIndex);
                    Node symbol = item.getNode();
                    if (get() && symbol == null) { // create any symbols
                        symbol = createSymbol(series, getData().indexOf(series), item, itemIndex);
                        if (null != symbol) {
                            getPlotChildren().add(symbol);
                        }
                    } else if (!get() && symbol != null) { // remove symbols
                        getPlotChildren().remove(symbol);
                        symbol = null;
                        item.setNode(null);
                    }
                }
            }
            requestChartLayout();
        }

        public Object getBean() {
            return this;
        }

        public String getName() {
            return "createSymbols";
        }

        public CssMetaData getCssMetaData() {
            return StyleableProperties.CREATE_SYMBOLS;
        }
    };

    /**
     * Indicates whether symbols for data points will be created or not.
     *
     * @return true if symbols for data points will be created and false otherwise.
     * @since JavaFX 8.0
     */
    public final boolean getCreateSymbols() { return createSymbols.getValue(); }
    public final void setCreateSymbols(boolean value) { createSymbols.setValue(value); }
    public final BooleanProperty createSymbolsProperty() { return createSymbols; }
    
    // -------------- CONSTRUCTORS ----------------------------------------------

    /**
     * Construct a new Area Chart with the given axis
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     */
    public StackedAreaChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
        this(xAxis,yAxis, FXCollections.<Series<X,Y>>observableArrayList());
    }

    /**
     * Construct a new Area Chart with the given axis and data
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     * @param data The data to use, this is the actual list used so any changes to it will be reflected in the chart
     */
    public StackedAreaChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis, @NamedArg("data") ObservableList<Series<X,Y>> data) {
        super(xAxis,yAxis);
        setLegend(legend);
        setData(data);
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------

    private static double doubleValue(Number number) { return doubleValue(number, 0); }
    private static double doubleValue(Number number, double nullDefault) {
        return (number == null) ? nullDefault : number.doubleValue();
    }

    @Override protected void dataItemAdded(Series<X,Y> series, int itemIndex, Data<X,Y> item) {
        final Node symbol = createSymbol(series, getData().indexOf(series), item, itemIndex);
        if (shouldAnimate()) {
            boolean animate = false;
            if (itemIndex > 0 && itemIndex < (series.getData().size()-1)) {
                animate = true;
                Data<X,Y> p1 = series.getData().get(itemIndex - 1);
                Data<X,Y> p2 = series.getData().get(itemIndex + 1);
                double x1 = getXAxis().toNumericValue(p1.getXValue());
                double y1 = getYAxis().toNumericValue(p1.getYValue());
                double x3 = getXAxis().toNumericValue(p2.getXValue());
                double y3 = getYAxis().toNumericValue(p2.getYValue());
                
                double x2 = getXAxis().toNumericValue(item.getXValue());
                double y2 = getYAxis().toNumericValue(item.getYValue());
      
//                //1. y intercept of the line : y = ((y3-y1)/(x3-x1)) * x2 + (x3y1 - y3x1)/(x3 -x1)
                double y = ((y3-y1)/(x3-x1)) * x2 + (x3*y1 - y3*x1)/(x3-x1);
                item.setCurrentY(getYAxis().toRealValue(y));
                item.setCurrentX(getXAxis().toRealValue(x2));
                //2. we can simply use the midpoint on the line as well..
//                double x = (x3 + x1)/2;
//                double y = (y3 + y1)/2;
//                item.setCurrentX(x);
//                item.setCurrentY(y);
            } else if (itemIndex == 0 && series.getData().size() > 1) {
                animate = true;
                item.setCurrentX(series.getData().get(1).getXValue());
                item.setCurrentY(series.getData().get(1).getYValue());
            } else if (itemIndex == (series.getData().size() - 1) && series.getData().size() > 1) {
                animate = true;
                int last = series.getData().size() - 2;
                item.setCurrentX(series.getData().get(last).getXValue());
                item.setCurrentY(series.getData().get(last).getYValue());
            } else if (symbol != null) {
                // fade in new symbol
                symbol.setOpacity(0);
                getPlotChildren().add(symbol);
                FadeTransition ft = new FadeTransition(Duration.millis(500),symbol);
                ft.setToValue(1);
                ft.play();
            }
            if (animate) {
                animate(
                    new KeyFrame(Duration.ZERO,
                            (e) -> { if (!getPlotChildren().contains(symbol)) getPlotChildren().add(symbol); },
                            new KeyValue(item.currentYProperty(),
                                    item.getCurrentY()),
                            new KeyValue(item.currentXProperty(),
                                    item.getCurrentX())
                    ),
                    new KeyFrame(Duration.millis(800), new KeyValue(item.currentYProperty(),
                                        item.getYValue(), Interpolator.EASE_BOTH),
                                        new KeyValue(item.currentXProperty(),
                                        item.getXValue(), Interpolator.EASE_BOTH))
                );
            }
            
        } else if (symbol != null) {
            getPlotChildren().add(symbol);
        }
    }

    @Override protected  void dataItemRemoved(final Data<X,Y> item, final Series<X,Y> series) {
        final Node symbol = item.getNode();
        // remove item from sorted list
        int itemIndex = series.getItemIndex(item);
        if (shouldAnimate()) {
            boolean animate = false;
            if (itemIndex > 0 && itemIndex < series.getDataSize()) {
                animate = true;
                int index=0;
                Data<X,Y> d;

                for (d = series.begin; d != null && index != itemIndex - 1; d = d.next) {
                    index++;
                }

                Data<X,Y> p1 = d;
                Data<X,Y> p2 = d.next.next;

                if (p2 == null) {
                    return;
                }

                double x1 = getXAxis().toNumericValue(p1.getXValue());
                double y1 = getYAxis().toNumericValue(p1.getYValue());
                double x3 = getXAxis().toNumericValue(p2.getXValue());
                double y3 = getYAxis().toNumericValue(p2.getYValue());

                double x2 = getXAxis().toNumericValue(item.getXValue());
                double y2 = getYAxis().toNumericValue(item.getYValue());

//                //1.  y intercept of the line : y = ((y3-y1)/(x3-x1)) * x2 + (x3y1 - y3x1)/(x3 -x1)
                double y = ((y3-y1)/(x3-x1)) * x2 + (x3*y1 - y3*x1)/(x3-x1);
                item.setCurrentX(getXAxis().toRealValue(x2));
                item.setCurrentY(getYAxis().toRealValue(y2));
                item.setXValue(getXAxis().toRealValue(x2));
                item.setYValue(getYAxis().toRealValue(y));
                //2.  we can simply use the midpoint on the line as well..
//                double x = (x3 + x1)/2;
//                double y = (y3 + y1)/2;
//                item.setCurrentX(x);
//                item.setCurrentY(y);
            } else if (itemIndex == 0 && series.getDataSize() > 1) {
                animate = true;
                item.setXValue(series.getData().get(0).getXValue());
                item.setYValue(series.getData().get(0).getYValue());
            } else if (itemIndex == (series.getDataSize() - 1) && series.getDataSize() > 1) {
                animate = true;
                int last = series.getData().size() - 1;
                item.setXValue(series.getData().get(last).getXValue());
                item.setYValue(series.getData().get(last).getYValue());
            } else {
                // fade out symbol
                symbol.setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(500),symbol);
                ft.setToValue(0);
                ft.setOnFinished(actionEvent -> {
                    getPlotChildren().remove(symbol);
                    removeDataItemFromDisplay(series, item);
                });
                ft.play();
            }
            if (animate) {
                animate( new KeyFrame(Duration.ZERO, new KeyValue(item.currentYProperty(),
                            item.getCurrentY()), new KeyValue(item.currentXProperty(),
                            item.getCurrentX())),
                            new KeyFrame(Duration.millis(800), actionEvent -> {
                                getPlotChildren().remove(symbol);
                                removeDataItemFromDisplay(series, item);
                            },
                            new KeyValue(item.currentYProperty(),
                            item.getYValue(), Interpolator.EASE_BOTH),
                            new KeyValue(item.currentXProperty(),
                            item.getXValue(), Interpolator.EASE_BOTH))
                );
            }
        } else {
            getPlotChildren().remove(symbol);
            removeDataItemFromDisplay(series, item);
        }
        //Note: better animation here, point should move from old position to new position at center point between prev and next symbols
    }

    /** @inheritDoc */
    @Override protected void dataItemChanged(Data<X, Y> item) {
    }

    @Override protected void seriesChanged(ListChangeListener.Change<? extends Series> c) {
        // Update style classes for all series lines and symbols
        for (int i = 0; i < getDataSize(); i++) {
            final Series<X,Y> s = getData().get(i);
            Path seriesLine = (Path)((Group)s.getNode()).getChildren().get(1);
            Path fillPath = (Path)((Group)s.getNode()).getChildren().get(0);
            seriesLine.getStyleClass().setAll("chart-series-area-line", "series" + i, s.defaultColorStyleClass);
            fillPath.getStyleClass().setAll("chart-series-area-fill", "series" + i, s.defaultColorStyleClass);
            for (int j=0; j < s.getData().size(); j++) {
                final Data item = s.getData().get(j);
                final Node node = item.getNode();
                if(node!=null) node.getStyleClass().setAll("chart-area-symbol", "series" + i, "data" + j, s.defaultColorStyleClass);
            }
        }
    }

    @Override protected  void seriesAdded(Series<X,Y> series, int seriesIndex) {
        // create new paths for series
        Path seriesLine = new Path();
        Path fillPath = new Path();
        seriesLine.setStrokeLineJoin(StrokeLineJoin.BEVEL);
        fillPath.setStrokeLineJoin(StrokeLineJoin.BEVEL);
        Group areaGroup = new Group(fillPath,seriesLine);
        series.setNode(areaGroup);
        // create series Y multiplier
        DoubleProperty seriesYAnimMultiplier = new SimpleDoubleProperty(this, "seriesYMultiplier");
        seriesYMultiplierMap.put(series, seriesYAnimMultiplier);
        // handle any data already in series
        if (shouldAnimate()) {
            seriesYAnimMultiplier.setValue(0d);
        } else {
            seriesYAnimMultiplier.setValue(1d);
        }
        getPlotChildren().add(areaGroup);
        List<KeyFrame> keyFrames = new ArrayList<KeyFrame>();
        if (shouldAnimate()) {
            // animate in new series
            keyFrames.add(new KeyFrame(Duration.ZERO,
                new KeyValue(areaGroup.opacityProperty(), 0),
                new KeyValue(seriesYAnimMultiplier, 0)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(200),
                new KeyValue(areaGroup.opacityProperty(), 1)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(500),
                new KeyValue(seriesYAnimMultiplier, 1)
            ));
        }
        for (int j=0; j<series.getData().size(); j++) {
            Data item = series.getData().get(j);
            final Node symbol = createSymbol(series, seriesIndex, item, j);
            if (symbol != null) {
                if (shouldAnimate()) symbol.setOpacity(0);
                getPlotChildren().add(symbol);
                if (shouldAnimate()) {
                    // fade in new symbol
                    keyFrames.add(new KeyFrame(Duration.ZERO, new KeyValue(symbol.opacityProperty(), 0)));
                    keyFrames.add(new KeyFrame(Duration.millis(200), new KeyValue(symbol.opacityProperty(), 1)));
                }
            }
        }
        if (shouldAnimate()) animate(keyFrames.toArray(new KeyFrame[keyFrames.size()]));
    }

    @Override protected  void seriesRemoved(final Series<X,Y> series) {
        // remove series Y multiplier
        seriesYMultiplierMap.remove(series);
        // remove all symbol nodes
        if (shouldAnimate()) {
            // create list of all nodes we need to fade out
            final List<Node> nodes = new ArrayList<Node>();
            nodes.add(series.getNode());
            for (Data d: series.getData()) nodes.add(d.getNode());
            // fade out old and symbols
            if (getCreateSymbols()) {
                KeyValue[] startValues = new KeyValue[nodes.size()];
                KeyValue[] endValues = new KeyValue[nodes.size()];
                for (int j=0; j < nodes.size(); j++) {
                    startValues[j]   = new KeyValue(nodes.get(j).opacityProperty(),1);
                    endValues[j]       = new KeyValue(nodes.get(j).opacityProperty(),0);
                }
                Timeline tl = new Timeline();
                tl.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO,startValues),
                    new KeyFrame(Duration.millis(400), actionEvent -> {
                        getPlotChildren().removeAll(nodes);
                        removeSeriesFromDisplay(series);
                    },endValues)
                );
                tl.play();
            } else {
                Timeline tl = new Timeline();
                tl.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(400), actionEvent -> {
                        getPlotChildren().removeAll(nodes);
                        removeSeriesFromDisplay(series);
                    })
                );
                tl.play();                
            }
        } else {
            getPlotChildren().remove(series.getNode());
            for (Data d:series.getData()) getPlotChildren().remove(d.getNode());
            removeSeriesFromDisplay(series);
        }
    }
    
    /** @inheritDoc */
    @Override protected void updateAxisRange() {
        // This override is necessary to update axis range based on cumulative Y value for the
        // Y axis instead of the normal way where max value in the data range is used.
        final Axis<X> xa = getXAxis();
        final Axis<Y> ya = getYAxis();
        if (xa.isAutoRanging()) {
            List xData = new ArrayList<Number>();
            if(xData != null) {
                for(Series<X,Y> series : getData()) {
                    for(Data<X,Y> data: series.getData()) {
                        if(xData != null) xData.add(data.getXValue());
                    }
                }
                if(xData != null) xa.invalidateRange(xData);
            }
        }
        if (ya.isAutoRanging()) {
            List yData = new ArrayList<Number>();
            if(yData != null) {
                double totalY = 0;
                Iterator<Series<X, Y>> seriesIterator = getDisplayedSeriesIterator();
                while (seriesIterator.hasNext()) {
                    double maxY = 0;
                    Series<X, Y> series = seriesIterator.next();
                    for(Data<X,Y> item : series.getData()) {
                        if(item != null) maxY = Math.max(maxY, ya.toNumericValue(item.getYValue()));
                    }
                    totalY += maxY;

                }
                if(totalY > 0) yData.add(totalY);
                ya.invalidateRange(yData);
            }
        }
    }

     
    /** @inheritDoc */
    @Override protected void layoutPlotChildren() {
        ArrayList<DataPointInfo> currentSeriesData = 
                                new ArrayList<DataPointInfo>();
        // AggregateData hold the data points of both the current and the previous series.
            // The goal is to collect all the data, sort it and iterate.
        ArrayList<DataPointInfo> aggregateData = 
                                new ArrayList<DataPointInfo>();
        for (int seriesIndex=0; seriesIndex < getDataSize(); seriesIndex++) { // for every series
            Series<X, Y> series = getData().get(seriesIndex);
            aggregateData.clear();
            // copy currentSeriesData accumulated in the previous iteration to aggregate.
            for(DataPointInfo<X,Y> data : currentSeriesData) {
                data.partOf = PartOf.PREVIOUS;
                aggregateData.add(data);
            }
            currentSeriesData.clear(); 
            // now copy actual data of the current series. 
            for(Data<X, Y> item = series.begin; item != null; item = item.next) {
                DataPointInfo<X,Y> itemInfo = new DataPointInfo(item, item.getXValue(), 
                        item.getYValue(), PartOf.CURRENT);
                aggregateData.add(itemInfo);
            }
            DoubleProperty seriesYAnimMultiplier = seriesYMultiplierMap.get(series);
            Path seriesLine = (Path)((Group)series.getNode()).getChildren().get(1);
            Path fillPath = (Path)((Group)series.getNode()).getChildren().get(0);
            seriesLine.getElements().clear();
            fillPath.getElements().clear();
            int dataIndex = 0;
            // Sort data points from prev and current series
            sortAggregateList(aggregateData);
           
            boolean firstCurrent = false;
            boolean lastCurrent = false;
            int firstCurrentIndex = findNextCurrent(aggregateData, -1);
            int lastCurrentIndex = findPreviousCurrent(aggregateData, aggregateData.size());
            // Iterate over the aggregate data : this process accumulates data points
            // cumulatively from the bottom to top of stack
            for (DataPointInfo<X,Y> dataInfo : aggregateData) {
                if (dataIndex == lastCurrentIndex) lastCurrent = true;
                if (dataIndex == firstCurrentIndex) firstCurrent = true;
                double x = 0;
                double y = 0;
                DataPointInfo<X,Y> currentDataPoint = new DataPointInfo();
                DataPointInfo<X,Y> dropDownDataPoint = new DataPointInfo(true);
                Data<X,Y> item = dataInfo.dataItem;
                if (dataInfo.partOf.equals(PartOf.CURRENT)) { // handle data from current series
                    int pIndex = findPreviousPrevious(aggregateData, dataIndex); 
                    int nIndex = findNextPrevious(aggregateData, dataIndex);
                    DataPointInfo<X,Y> prevPoint;
                    DataPointInfo<X,Y> nextPoint;
                    if (pIndex == -1 || (nIndex == -1 && !(aggregateData.get(pIndex).x.equals(dataInfo.x)))) {
                        if (firstCurrent) {
                            // Need to add the drop down point.
                            item = new Data(dataInfo.x, 0);
                            x = getXAxis().getDisplayPosition(item.getCurrentX());
                            y = getYAxis().getZeroPosition();
                            dropDownDataPoint.setValues(item, item.getXValue(), item.getYValue(), x, y, PartOf.CURRENT, true, false);
                            currentSeriesData.add(dropDownDataPoint);
                        } 
                        // And add current point.
                        item = dataInfo.dataItem;
                        x = getXAxis().getDisplayPosition(item.getCurrentX());
                        y = getYAxis().getDisplayPosition(
                                getYAxis().toRealValue(getYAxis().toNumericValue(item.getCurrentY()) * seriesYAnimMultiplier.getValue()));
                        currentDataPoint.setValues(item, item.getXValue(), item.getYValue(), x, y, PartOf.CURRENT, false, (firstCurrent) ? false : true);
                        currentSeriesData.add(currentDataPoint);
                        if (dataIndex == lastCurrentIndex) {
                            // need to add drop down point
                            item = new Data(dataInfo.x, 0);
                            x = getXAxis().getDisplayPosition(item.getCurrentX());
                            y = getYAxis().getZeroPosition();
                            dropDownDataPoint.setValues(item, item.getXValue(), item.getYValue(), x, y, PartOf.CURRENT, true, false);
                            currentSeriesData.add(dropDownDataPoint);
                        }
                    } else {
                        prevPoint = aggregateData.get(pIndex);
                        if (prevPoint.x.equals(dataInfo.x)) { // Need to add Y values
                            // Check if prevPoint is a dropdown - as the stable sort preserves the order.
                            // If so, find the non dropdown previous point on previous series.
                            DataPointInfo<X,Y> ddPoint = prevPoint;
                            if (prevPoint.dropDown) {
                                pIndex = findPreviousPrevious(aggregateData, pIndex);
                                prevPoint = (DataPointInfo<X,Y>)aggregateData.get(pIndex);
                                // If lastCurrent - add this drop down
                            } 
                            if (prevPoint.x.equals(dataInfo.x)) { // simply add
                                x = getXAxis().getDisplayPosition(item.getCurrentX());
                                y = getYAxis().getDisplayPosition(
                                    getYAxis().toRealValue(getYAxis().toNumericValue(item.getCurrentY()) * seriesYAnimMultiplier.getValue()));
                                y += -(getYAxis().getZeroPosition() - prevPoint.displayY);
                                currentDataPoint.setValues(item, dataInfo.x, dataInfo.y, x, y, PartOf.CURRENT, false, 
                                                            (firstCurrent) ? false : true);
                                currentSeriesData.add(currentDataPoint);
                            }
                            if (lastCurrent) {
                                    dropDownDataPoint.setValues(ddPoint.dataItem, ddPoint.x, ddPoint.y, 
                                            ddPoint.displayX, ddPoint.displayY, PartOf.CURRENT, true, false);
                                    currentSeriesData.add(dropDownDataPoint);
                            }
                        } else {
                            // interpolate 
                            nextPoint = (nIndex == -1) ? null : (DataPointInfo<X,Y>)aggregateData.get(nIndex);
                            prevPoint = (pIndex == -1) ? null : (DataPointInfo<X,Y>)aggregateData.get(pIndex);
                            x = getXAxis().getDisplayPosition(item.getCurrentX());
                            y = getYAxis().getDisplayPosition(
                                getYAxis().toRealValue(getYAxis().toNumericValue(item.getCurrentY()) * seriesYAnimMultiplier.getValue()));
                            if (prevPoint != null && nextPoint != null) {
                                 double displayY = interpolate(prevPoint.displayX, 
                                        prevPoint.displayY, nextPoint.displayX, nextPoint.displayY, x);
                                 y+= -(getYAxis().getZeroPosition() - displayY);
                                 double dataY = interpolate(getXAxis().toNumericValue(prevPoint.x), 
                                         getYAxis().toNumericValue(prevPoint.y), 
                                         getXAxis().toNumericValue(nextPoint.x), 
                                         getYAxis().toNumericValue(nextPoint.y), 
                                         getXAxis().toNumericValue(dataInfo.x)); 
                                 if (firstCurrent) {
                                     // now create the drop down point
                                     item = new Data(dataInfo.x, dataY);
                                     dropDownDataPoint.setValues(item, dataInfo.x, getYAxis().toRealValue(dataY), x, displayY, PartOf.CURRENT, true, false);
                                     currentSeriesData.add(dropDownDataPoint);
                                 }
                                 // Add the current point
                                 currentDataPoint.setValues(item, dataInfo.x, dataInfo.y, x, y, PartOf.CURRENT, false, 
                                                                        (firstCurrent) ? false : true);
                                 currentSeriesData.add(currentDataPoint);
                                 if (dataIndex == lastCurrentIndex) {
                                     // add drop down point
                                     item = new Data(dataInfo.x, dataY);
                                     dropDownDataPoint.setValues(item, dataInfo.x, getYAxis().toRealValue(dataY), x, displayY, PartOf.CURRENT, true, false);
                                     currentSeriesData.add(dropDownDataPoint);
                                 }
                                 // Note: add drop down if last current
                            } 
                            else {
                                // we do not need to take care of this as it is
                                // already handled above with check of if(pIndex == -1 or nIndex == -1)
                            }
                        }
                    }
                    
                } else { // handle data from Previous series.
                    int pIndex = findPreviousCurrent(aggregateData, dataIndex); 
                    int nIndex = findNextCurrent(aggregateData, dataIndex);
                    DataPointInfo<X,Y> prevPoint;
                    DataPointInfo<X,Y> nextPoint;
                    if (dataInfo.dropDown) {
                        if (getXAxis().toNumericValue(dataInfo.x) <= 
                                getXAxis().toNumericValue(((DataPointInfo<X,Y>)aggregateData.get(firstCurrentIndex)).x) || 
                                getXAxis().toNumericValue(dataInfo.x) > getXAxis().toNumericValue(((DataPointInfo<X,Y>)aggregateData.get(lastCurrentIndex)).x)) {
                            currentDataPoint.setValues(item, dataInfo.x, dataInfo.y, dataInfo.displayX, dataInfo.displayY, 
                                    PartOf.CURRENT, true, false);
                            currentDataPoint.dropDown = true;
                            currentSeriesData.add(currentDataPoint);
                        }
                    } else {
                        if (pIndex == -1 || nIndex == -1) {
                            currentDataPoint.setValues(item, dataInfo.x, dataInfo.y, dataInfo.displayX, dataInfo.displayY, 
                                    PartOf.CURRENT, true, false);
                            currentSeriesData.add(currentDataPoint);
                        } else {
                            nextPoint = (DataPointInfo<X,Y>)aggregateData.get(nIndex);
                            if (nextPoint.x.equals(dataInfo.x)) {
                                // do nothing as the current point is already there.
                            } else {
                                // interpolate on the current series.
                                prevPoint = (DataPointInfo<X,Y>)aggregateData.get(pIndex);
                                x = getXAxis().getDisplayPosition(item.getCurrentX());
                                  double dataY = interpolate(getXAxis().toNumericValue(prevPoint.x), 
                                         getYAxis().toNumericValue(prevPoint.y), 
                                         getXAxis().toNumericValue(nextPoint.x), 
                                         getYAxis().toNumericValue(nextPoint.y), 
                                         getXAxis().toNumericValue(dataInfo.x)); 
                                y = getYAxis().getDisplayPosition(
                                    getYAxis().toRealValue(dataY * seriesYAnimMultiplier.getValue()));
                                y+= -(getYAxis().getZeroPosition() - dataInfo.displayY);
                                currentDataPoint.setValues(new Data(dataInfo.x, dataY), dataInfo.x, getYAxis().toRealValue(0), x, y, PartOf.CURRENT, true, true);
                                currentSeriesData.add(currentDataPoint);
                            }
                        }
                    }
                }
                dataIndex++;
                if (firstCurrent) firstCurrent = false;
                if (lastCurrent) lastCurrent = false;
            } // end of inner for loop 
            
            // Draw the SeriesLine and Series fill
            seriesLine.getElements().add(new MoveTo(currentSeriesData.get(0).displayX, currentSeriesData.get(0).displayY));
            fillPath.getElements().add(new MoveTo(currentSeriesData.get(0).displayX, currentSeriesData.get(0).displayY));
            for (DataPointInfo point : currentSeriesData) {
                if (!point.lineTo) {
                    seriesLine.getElements().add(new MoveTo(point.displayX, point.displayY));
                } else {
                    seriesLine.getElements().add(new LineTo(point.displayX, point.displayY));
                }
                fillPath.getElements().add(new  LineTo(point.displayX, point.displayY));
                // draw symbols only for actual data points and skip for interpolated points.
                if (!point.skipSymbol) { 
                    Node symbol = point.dataItem.getNode();
                    if (symbol != null) {
                        final double w = symbol.prefWidth(-1);
                        final double h = symbol.prefHeight(-1);
                        symbol.resizeRelocate(point.displayX-(w/2), point.displayY-(h/2),w,h);
                    }
                }
            }
            for(int i = aggregateData.size()-1; i > 0; i--) {
                DataPointInfo point = aggregateData.get(i);
                if (PartOf.PREVIOUS.equals(point.partOf)) {
                    fillPath.getElements().add(new  LineTo(point.displayX, point.displayY));
                }
            }
            fillPath.getElements().add(new ClosePath());
             
        }  // end of out for loop
     }
     
     //-------------------- helper methods to retrieve data points from the previous
     // or current data series.
     private int findNextCurrent(ArrayList<DataPointInfo> points, int index) {
        for(int i = index+1; i < points.size(); i++) {
            if (points.get(i).partOf.equals(PartOf.CURRENT)) {
                return i;
            }
        }
        return -1;
     }
     
     private int findPreviousCurrent(ArrayList<DataPointInfo> points, int index) {
        for(int i = index-1; i >= 0; i--) {
            if (points.get(i).partOf.equals(PartOf.CURRENT)) {
                return i;
            }
        }
        return -1;
     }
     
     
    private int findPreviousPrevious(ArrayList<DataPointInfo> points, int index) {
       for(int i = index-1; i >= 0; i--) {
            if (points.get(i).partOf.equals(PartOf.PREVIOUS)) {
                return i;
            }
        }
        return -1;
    }
    private int findNextPrevious(ArrayList<DataPointInfo> points, int index) {
        for(int i = index+1; i < points.size(); i++) {
            if (points.get(i).partOf.equals(PartOf.PREVIOUS)) {
                return i;
            }
        }
        return -1;
    }
     
    
     private void sortAggregateList(ArrayList<DataPointInfo> aggregateList) {
        Collections.sort(aggregateList, (o1, o2) -> {
            Data<X,Y> d1 = ((DataPointInfo)o1).dataItem;
            Data<X,Y> d2 = ((DataPointInfo)o2).dataItem;
            double val1 = getXAxis().toNumericValue(d1.getXValue());
            double val2 = getXAxis().toNumericValue(d2.getXValue());
            return (val1 < val2 ? -1 : ( val1 == val2) ? 0 : 1);
        });
     }
    
    private double interpolate(double lowX, double lowY, double highX, double highY, double x) {
         // using y = mx+c find the y for the given x. 
         return (((highY - lowY)/(highX - lowX))*(x - lowX))+lowY;
    }

    private Node createSymbol(Series series, int seriesIndex, final Data item, int itemIndex) {
        Node symbol = item.getNode();
        // check if symbol has already been created
        if (symbol == null && getCreateSymbols()) {
            symbol = new StackPane();
            item.setNode(symbol);
        }
        // set symbol styles
        // Note not sure if we want to add or check, ie be more careful and efficient here
        if (symbol != null) symbol.getStyleClass().setAll("chart-area-symbol", "series" + seriesIndex, "data" + itemIndex,
                series.defaultColorStyleClass);
        return symbol;
    }

    /**
     * This is called whenever a series is added or removed and the legend needs to be updated
     */
    @Override protected void updateLegend() {
        legend.getItems().clear();
        if (getData() != null) {
            for (int seriesIndex=0; seriesIndex < getData().size(); seriesIndex++) {
                Series<X,Y> series = getData().get(seriesIndex);
                LegendItem legenditem = new LegendItem(series.getName());
                legenditem.getSymbol().getStyleClass().addAll("chart-area-symbol","series"+seriesIndex,
                        "area-legend-symbol", series.defaultColorStyleClass);
                legend.getItems().add(legenditem);
            }
        }
        if (legend.getItems().size() > 0) {
            if (getLegend() == null) {
                setLegend(legend);
            }
        } else {
            setLegend(null);
        }
    }

    // -------------- INNER CLASSES --------------------------------------------
    /* 
     * Helper class to hold data and display and other information for each 
     * data point
     */
    final static class DataPointInfo<X,Y> {
        X x;
        Y y;   
        double displayX;
        double displayY;
        Data<X,Y> dataItem;
        PartOf partOf;
        boolean skipSymbol = false; // interpolated point - skip drawing symbol
        boolean lineTo = false; // should there be a lineTo to this point on SeriesLine.
        boolean dropDown = false; // Is this a drop down point ( non data point).
        
        //----- Constructors --------------------
        DataPointInfo() {}

        DataPointInfo(Data<X,Y> item, X x, Y y, PartOf partOf) {
            this.dataItem = item;
            this.x = x;
            this.y = y;
            this.partOf = partOf;
        }
        
        DataPointInfo(boolean dropDown) {
            this.dropDown = dropDown;
        }
        
        void setValues(Data<X,Y> item, X x, Y y, double dx, double dy, 
                        PartOf partOf, boolean skipSymbol, boolean lineTo) {
            this.dataItem = item;
            this.x = x;
            this.y = y;
            this.displayX = dx;
            this.displayY = dy;
            this.partOf = partOf;
            this.skipSymbol = skipSymbol;
            this.lineTo = lineTo;
        }
        
        public final X getX() {
            return x;
        }
        
        public final Y getY() {
            return y;
        }
    }

    // To indicate if the data point belongs to the current or the previous series.
    private static enum PartOf { 
        CURRENT,
        PREVIOUS
    }
    
    // -------------- STYLESHEET HANDLING --------------------------------------
    
    private static class StyleableProperties {

        private static final CssMetaData<StackedAreaChart<?, ?>, Boolean> CREATE_SYMBOLS =
                new CssMetaData<StackedAreaChart<?, ?>, Boolean>("-fx-create-symbols",
                BooleanConverter.getInstance(), Boolean.TRUE) {
            @Override
            public boolean isSettable(StackedAreaChart node) {
                return node.createSymbols == null || !node.createSymbols.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(StackedAreaChart node) {
                return (StyleableProperty<Boolean>) node.createSymbolsProperty();
            }
        };
        
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(XYChart.getClassCssMetaData());
            styleables.add(CREATE_SYMBOLS);
            STYLEABLES = Collections.unmodifiableList(styleables);

        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
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

}
