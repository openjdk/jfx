/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Animation;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

import com.sun.javafx.charts.Legend;
import com.sun.javafx.charts.Legend.LegendItem;
import javafx.css.StyleableBooleanProperty;
import javafx.css.CssMetaData;
import com.sun.javafx.css.converters.BooleanConverter;
import java.util.*;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;

/**
 * Line Chart plots a line connecting the data points in a series. The data points
 * themselves can be represented by symbols optionally. Line charts are usually used
 * to view data trends over time or category. 
 * @since JavaFX 2.0
 */
public class LineChart<X,Y> extends XYChart<X,Y> {

    // -------------- PRIVATE FIELDS ------------------------------------------

    /** A multiplier for teh Y values that we store for each series, it is used to animate in a new series */
    private Map<Series, DoubleProperty> seriesYMultiplierMap = new HashMap<Series, DoubleProperty>();
    private Legend legend = new Legend();
    private Timeline dataRemoveTimeline;
    private Series<X,Y> seriesOfDataRemoved = null;
    private Data<X,Y> dataItemBeingRemoved = null;
    private FadeTransition fadeSymbolTransition = null;
    private Map<Data<X,Y>, Double> XYValueMap = 
                                new HashMap<Data<X,Y>, Double>();
    private Timeline seriesRemoveTimeline = null;
    // -------------- PUBLIC PROPERTIES ----------------------------------------

    /** When true, CSS styleable symbols are created for any data items that don't have a symbol node specified. */
    private BooleanProperty createSymbols = new StyleableBooleanProperty(true) {
        @Override protected void invalidated() {
            for (int seriesIndex=0; seriesIndex < getData().size(); seriesIndex ++) {
                Series<X,Y> series = getData().get(seriesIndex);
                for (int itemIndex=0; itemIndex < series.getData().size(); itemIndex ++) {
                    Data<X,Y> item = series.getData().get(itemIndex);
                    Node symbol = item.getNode();
                    if(get() && symbol == null) { // create any symbols
                        symbol = createSymbol(series, getData().indexOf(series), item, itemIndex);
                        getPlotChildren().add(symbol);
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
            return LineChart.this;
        }

        public String getName() {
            return "createSymbols";
        }

        public CssMetaData<LineChart<?,?>,Boolean> getCssMetaData() {
            return StyleableProperties.CREATE_SYMBOLS;
        }
    };

    /**
     * Indicates whether symbols for data points will be created or not.
     *
     * @return true if symbols for data points will be created and false otherwise.
     */
    public final boolean getCreateSymbols() { return createSymbols.getValue(); }
    public final void setCreateSymbols(boolean value) { createSymbols.setValue(value); }
    public final BooleanProperty createSymbolsProperty() { return createSymbols; }

    // -------------- CONSTRUCTORS ----------------------------------------------

    /**
     * Construct a new LineChart with the given axis.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     */
    public LineChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.<Series<X, Y>>observableArrayList());
    }

    /**
     * Construct a new LineChart with the given axis and data.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     * @param data The data to use, this is the actual list used so any changes to it will be reflected in the chart
     */
    public LineChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis, @NamedArg("data") ObservableList<Series<X,Y>> data) {
        super(xAxis,yAxis);
        setLegend(legend);
        setData(data);
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------

    /** @inheritDoc */
    @Override protected void updateAxisRange() {
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
            // RT-32838 No need to invalidate range if there is one data item - whose value is zero. 
            if(xData != null && !(xData.size() == 1 && getXAxis().toNumericValue(xData.get(0)) == 0)) {
                xa.invalidateRange(xData);
            }
            if(yData != null && !(yData.size() == 1 && getYAxis().toNumericValue(yData.get(0)) == 0)) {
                ya.invalidateRange(yData);
            }
            
        }
    }
    
    @Override protected void dataItemAdded(final Series<X,Y> series, int itemIndex, final Data<X,Y> item) {
        final Node symbol = createSymbol(series, getData().indexOf(series), item, itemIndex);
        if (shouldAnimate()) {
            if (dataRemoveTimeline != null && dataRemoveTimeline.getStatus().equals(Animation.Status.RUNNING)) {
                if (seriesOfDataRemoved == series) {
                    dataRemoveTimeline.stop();
                    dataRemoveTimeline = null;
                    getPlotChildren().remove(dataItemBeingRemoved.getNode());
                    removeDataItemFromDisplay(seriesOfDataRemoved, dataItemBeingRemoved);
                    seriesOfDataRemoved = null;
                    dataItemBeingRemoved = null;
                }
            }
            boolean animate = false;
            if (itemIndex > 0 && itemIndex < (series.getData().size()-1)) {
                animate = true;
                Data<X,Y> p1 = series.getData().get(itemIndex - 1);
                Data<X,Y> p2 = series.getData().get(itemIndex + 1);
                if (p1 != null && p2 != null) {
                    double x1 = getXAxis().toNumericValue(p1.getXValue());
                    double y1 = getYAxis().toNumericValue(p1.getYValue());
                    double x3 = getXAxis().toNumericValue(p2.getXValue());
                    double y3 = getYAxis().toNumericValue(p2.getYValue());

                    double x2 = getXAxis().toNumericValue(item.getXValue());
                    //double y2 = getYAxis().toNumericValue(item.getYValue());
                    if (x2 > x1 && x2 < x3) {
                         //1. y intercept of the line : y = ((y3-y1)/(x3-x1)) * x2 + (x3y1 - y3x1)/(x3 -x1)
                        double y = ((y3-y1)/(x3-x1)) * x2 + (x3*y1 - y3*x1)/(x3-x1);
                        item.setCurrentY(getYAxis().toRealValue(y));
                        item.setCurrentX(getXAxis().toRealValue(x2));
                    } else {
                        //2. we can simply use the midpoint on the line as well..
                        double x = (x3 + x1)/2;
                        double y = (y3 + y1)/2;
                        item.setCurrentX(getXAxis().toRealValue(x));
                        item.setCurrentY(getYAxis().toRealValue(y));
                    }
                }
            } else if (itemIndex == 0 && series.getData().size() > 1) {
                animate = true;
                item.setCurrentX(series.getData().get(1).getXValue());
                item.setCurrentY(series.getData().get(1).getYValue());
            } else if (itemIndex == (series.getData().size() - 1) && series.getData().size() > 1) {
                animate = true;
                int last = series.getData().size() - 2;
                item.setCurrentX(series.getData().get(last).getXValue());
                item.setCurrentY(series.getData().get(last).getYValue());
            } else if(symbol != null) {
                // fade in new symbol
                FadeTransition ft = new FadeTransition(Duration.millis(500),symbol);
                ft.setToValue(1);
                ft.play();
            }
            if(symbol != null) {
                    getPlotChildren().add(symbol);
            }
            if (animate) {
                animate(
                    new KeyFrame(Duration.ZERO, new KeyValue(item.currentYProperty(),
                                        item.getCurrentY()),
                                        new KeyValue(item.currentXProperty(),
                                        item.getCurrentX())),
                    new KeyFrame(Duration.millis(700), new KeyValue(item.currentYProperty(),
                                        item.getYValue(), Interpolator.EASE_BOTH),
                                        new KeyValue(item.currentXProperty(),
                                        item.getXValue(), Interpolator.EASE_BOTH))
                );
            }
            
        } else {
            if (symbol != null) getPlotChildren().add(symbol);
        }
    }

    @Override protected  void dataItemRemoved(final Data<X,Y> item, final Series<X,Y> series) {
        final Node symbol = item.getNode();
        // remove item from sorted list
        int itemIndex = series.getItemIndex(item);
        if (shouldAnimate()) {
            XYValueMap.clear();
            boolean animate = false;
            if (itemIndex > 0 && itemIndex < series.getDataSize()) {
                animate = true;
                int index=0; Data<X,Y> d;
                for (d = series.begin; d != null && index != itemIndex - 1; d=d.next) index++;
                Data<X,Y> p1 = d;
                Data<X,Y> p2 = (d.next).next;
                if (p1 != null && p2 != null) {
                    double x1 = getXAxis().toNumericValue(p1.getXValue());
                    double y1 = getYAxis().toNumericValue(p1.getYValue());
                    double x3 = getXAxis().toNumericValue(p2.getXValue());
                    double y3 = getYAxis().toNumericValue(p2.getYValue());

                    double x2 = getXAxis().toNumericValue(item.getXValue());
                    double y2 = getYAxis().toNumericValue(item.getYValue());
                    if (x2 > x1 && x2 < x3) {
    //                //1.  y intercept of the line : y = ((y3-y1)/(x3-x1)) * x2 + (x3y1 - y3x1)/(x3 -x1)
                        double y = ((y3-y1)/(x3-x1)) * x2 + (x3*y1 - y3*x1)/(x3-x1);
                        item.setCurrentX(getXAxis().toRealValue(x2));
                        item.setCurrentY(getYAxis().toRealValue(y2));
                        item.setXValue(getXAxis().toRealValue(x2));
                        item.setYValue(getYAxis().toRealValue(y));
                    } else {
                    //2.  we can simply use the midpoint on the line as well..
                        double x = (x3 + x1)/2;
                        double y = (y3 + y1)/2;
                        item.setCurrentX(getXAxis().toRealValue(x));
                        item.setCurrentY(getYAxis().toRealValue(y));
                    }
                }
            } else if (itemIndex == 0 && series.getDataSize() > 1) {
                animate = true;
                Iterator<Data<X,Y>> iter = getDisplayedDataIterator(series);
                if (iter.hasNext()) { // get first data value
                    Data<X,Y> d = iter.next();
                    item.setXValue(d.getXValue());
                    item.setYValue(d.getYValue());
                }
            } else if (itemIndex == (series.getDataSize() - 1) && series.getDataSize() > 1) {
                animate = true;
                int last = series.getData().size() - 1;
                item.setXValue(series.getData().get(last).getXValue());
                item.setYValue(series.getData().get(last).getYValue());
            } else {
                // fade out symbol
                if (symbol != null) {
                    symbol.setOpacity(0);
                    fadeSymbolTransition = new FadeTransition(Duration.millis(500),symbol);
                    fadeSymbolTransition.setToValue(0);
                    fadeSymbolTransition.setOnFinished(new EventHandler<ActionEvent>() {
                        @Override public void handle(ActionEvent actionEvent) {
                            item.setSeries(null);
                            getPlotChildren().remove(symbol);
                            removeDataItemFromDisplay(series, item);
                        }
                    });
                    fadeSymbolTransition.play();
                }
            }
            if (animate) {
                dataRemoveTimeline = createDataRemoveTimeline(item, symbol, series);
                seriesOfDataRemoved = series;
                dataItemBeingRemoved = item;
                dataRemoveTimeline.play();
            }
        } else {
            item.setSeries(null);
            if (symbol != null) getPlotChildren().remove(symbol);
            removeDataItemFromDisplay(series, item);
        }
        //Note: better animation here, point should move from old position to new position at center point between prev and next symbols
    }

    /** @inheritDoc */
    @Override protected void dataItemChanged(Data<X, Y> item) {
    }
    
    @Override protected void seriesChanged(ListChangeListener.Change<? extends Series> c) {
        // Update style classes for all series lines and symbols
        // Note: is there a more efficient way of doing this?
        for (int i = 0; i < getDataSize(); i++) {
            final Series<X,Y> s = getData().get(i);
            Node seriesNode = s.getNode();
            if(seriesNode != null) seriesNode.getStyleClass().setAll("chart-series-line", "series" + i, s.defaultColorStyleClass);
        }
    }

    @Override protected  void seriesAdded(Series<X,Y> series, int seriesIndex) {
        // create new path for series
        Path seriesLine = new Path();
        seriesLine.setStrokeLineJoin(StrokeLineJoin.BEVEL);
        series.setNode(seriesLine);
        // create series Y multiplier
        DoubleProperty seriesYAnimMultiplier = new SimpleDoubleProperty(this, "seriesYMultiplier");
        seriesYMultiplierMap.put(series, seriesYAnimMultiplier);
        // handle any data already in series
        if (shouldAnimate()) {
            seriesLine.setOpacity(0);
            seriesYAnimMultiplier.setValue(0d);
        } else {
            seriesYAnimMultiplier.setValue(1d);
        }
        getPlotChildren().add(seriesLine);

        List<KeyFrame> keyFrames = new ArrayList<KeyFrame>();
        if (shouldAnimate()) {
            // animate in new series
            keyFrames.add(new KeyFrame(Duration.ZERO,
                new KeyValue(seriesLine.opacityProperty(), 0),
                new KeyValue(seriesYAnimMultiplier, 0)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(200),
                new KeyValue(seriesLine.opacityProperty(), 1)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(500),
                new KeyValue(seriesYAnimMultiplier, 1)
            ));
        }
        for (int j=0; j<series.getData().size(); j++) {
            Data item = series.getData().get(j);
            final Node symbol = createSymbol(series, seriesIndex, item, j);
            if(symbol != null) {
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
    private void updateDefaultColorIndex(final Series<X,Y> series) {
        int clearIndex = seriesColorMap.get(series);
        colorBits.clear(clearIndex);
        series.getNode().getStyleClass().remove(DEFAULT_COLOR+clearIndex);
        for (int j=0; j < series.getData().size(); j++) {
            final Node node = series.getData().get(j).getNode();
            if(node!=null) {
                node.getStyleClass().remove(DEFAULT_COLOR+clearIndex);
            }
        }
        seriesColorMap.remove(series);
    }

    @Override protected  void seriesRemoved(final Series<X,Y> series) {
        updateDefaultColorIndex(series);
        // remove all symbol nodes
        seriesYMultiplierMap.remove(series);
        if (shouldAnimate()) {
            // create list of all nodes we need to fade out
            final List<Node> nodes = new ArrayList<Node>();
            nodes.add(series.getNode());
            if (getCreateSymbols()) { // RT-22124 
                // done need to fade the symbols if createSymbols is false
                for (Data d: series.getData()) nodes.add(d.getNode());
            }
            // fade out old and symbols
            KeyValue[] startValues = new KeyValue[nodes.size()];
            KeyValue[] endValues = new KeyValue[nodes.size()];
            for (int j=0; j < nodes.size(); j++) {
                startValues[j]   = new KeyValue(nodes.get(j).opacityProperty(),1);
                endValues[j]       = new KeyValue(nodes.get(j).opacityProperty(),0);
            }
            seriesRemoveTimeline = new Timeline();
            seriesRemoveTimeline.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO,startValues),
                new KeyFrame(Duration.millis(900), new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent actionEvent) {
                        getPlotChildren().removeAll(nodes);
                        removeSeriesFromDisplay(series);
                    }
                },endValues)
            );
            seriesRemoveTimeline.play();
        } else {
            getPlotChildren().remove(series.getNode());
            for (Data d:series.getData()) getPlotChildren().remove(d.getNode());
            removeSeriesFromDisplay(series);
        }
    }

    /** @inheritDoc */
    @Override protected void layoutPlotChildren() {
        for (int seriesIndex=0; seriesIndex < getDataSize(); seriesIndex++) {
            Series<X,Y> series = getData().get(seriesIndex);
            final DoubleProperty seriesYAnimMultiplier = seriesYMultiplierMap.get(series);
            boolean isFirst = true;
            if(series.getNode() instanceof  Path) {
                Path seriesLine = (Path)series.getNode();
                seriesLine.getElements().clear();
                for (Data<X,Y> item = series.begin; item != null; item = item.next) {
                    double x = getXAxis().getDisplayPosition(item.getCurrentX());
                    double y = getYAxis().getDisplayPosition(
                            getYAxis().toRealValue(getYAxis().toNumericValue(item.getCurrentY()) * seriesYAnimMultiplier.getValue()));
                    if (isFirst) {
                        isFirst = false;
                        seriesLine.getElements().add(new MoveTo(x, y));
                    } else {
                        seriesLine.getElements().add(new LineTo(x, y));
                    }
                    Node symbol = item.getNode();
                    if (symbol != null) {
                        final double w = symbol.prefWidth(-1);
                        final double h = symbol.prefHeight(-1);
                        symbol.resizeRelocate(x-(w/2), y-(h/2),w,h);
                    }
                }
            }
        }
    }
    /** @inheritDoc */
    @Override void dataBeingRemovedIsAdded(Data item, Series series) {
        if (fadeSymbolTransition != null) {
            fadeSymbolTransition.setOnFinished(null);
            fadeSymbolTransition.stop();
        }
        if (dataRemoveTimeline != null) {
            dataRemoveTimeline.setOnFinished(null);
            dataRemoveTimeline.stop();
        }
        final Node symbol = item.getNode();
        if (symbol != null) getPlotChildren().remove(symbol);
        
        item.setSeries(null);
        removeDataItemFromDisplay(series, item);
        
        // restore values to item
        Double value = XYValueMap.get(item);
        if (value != null) {
            item.setYValue(value);
            item.setCurrentY(value);
        }
        XYValueMap.clear();
    }
    /** @inheritDoc */
    @Override void seriesBeingRemovedIsAdded(Series<X,Y> series) {
        if (seriesRemoveTimeline != null) {
            seriesRemoveTimeline.setOnFinished(null);
            seriesRemoveTimeline.stop();
            getPlotChildren().remove(series.getNode());
            for (Data d:series.getData()) getPlotChildren().remove(d.getNode());
            removeSeriesFromDisplay(series);
        }
    }

    private Timeline createDataRemoveTimeline(final Data<X,Y> item, final Node symbol, final Series<X,Y> series) {
        Timeline t = new Timeline();
        // save data values in case the same data item gets added immediately.
        XYValueMap.put(item, ((Number)item.getYValue()).doubleValue());
            
        t.getKeyFrames().addAll(new KeyFrame(Duration.ZERO, new KeyValue(item.currentYProperty(),
                item.getCurrentY()), new KeyValue(item.currentXProperty(),
                item.getCurrentX())),
                new KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent actionEvent) {
                        if (symbol != null) getPlotChildren().remove(symbol);
                        removeDataItemFromDisplay(series, item);
                        XYValueMap.clear();
                    }
                },
                new KeyValue(item.currentYProperty(),
                item.getYValue(), Interpolator.EASE_BOTH),
                new KeyValue(item.currentXProperty(),
                item.getXValue(), Interpolator.EASE_BOTH))
        );
        return t;
    }

    private Node createSymbol(Series<X, Y> series, int seriesIndex, final Data item, int itemIndex) {
        Node symbol = item.getNode();
        // check if symbol has already been created
        if (symbol == null && getCreateSymbols()) {
            symbol = new StackPane();
            item.setNode(symbol);
        }
        // set symbol styles
        if (symbol != null) symbol.getStyleClass().addAll("chart-line-symbol", "series" + seriesIndex,
                "data" + itemIndex, series.defaultColorStyleClass);
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
                legenditem.getSymbol().getStyleClass().addAll("chart-line-symbol", "series"+seriesIndex, series.defaultColorStyleClass);
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

    // -------------- STYLESHEET HANDLING --------------------------------------

    private static class StyleableProperties {
        private static final CssMetaData<LineChart<?,?>,Boolean> CREATE_SYMBOLS = 
            new CssMetaData<LineChart<?,?>,Boolean>("-fx-create-symbols",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(LineChart node) {
                return node.createSymbols == null || !node.createSymbols.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(LineChart node) {
                return (StyleableProperty<Boolean>)node.createSymbolsProperty();
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
