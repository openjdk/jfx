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
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

import com.sun.javafx.charts.Legend;
import com.sun.javafx.charts.Legend.LegendItem;
import com.sun.javafx.css.converters.BooleanConverter;
import java.util.Collections;
import javafx.beans.property.BooleanProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableProperty;
import static javafx.scene.chart.LineChart.getClassCssMetaData;

/**
 * AreaChart - Plots the area between the line that connects the data points and
 * the 0 line on the Y axis.
 * @since JavaFX 2.0
 */
public class AreaChart<X,Y> extends XYChart<X,Y> {

    // -------------- PRIVATE FIELDS ------------------------------------------

    /** A multiplier for teh Y values that we store for each series, it is used to animate in a new series */
    private Map<Series, DoubleProperty> seriesYMultiplierMap = new HashMap<Series, DoubleProperty>();
    private Legend legend = new Legend();

    // -------------- PUBLIC PROPERTIES ----------------------------------------

    /** 
     * When true, CSS styleable symbols are created for any data items that don't have a symbol node specified. 
     * @since JavaFX 8.0
     */
    private BooleanProperty createSymbols = new StyleableBooleanProperty(true) {
        @Override protected void invalidated() {
            for (int seriesIndex=0; seriesIndex < getData().size(); seriesIndex ++) {
                Series<X,Y> series = getData().get(seriesIndex);
                for (int itemIndex=0; itemIndex < series.getData().size(); itemIndex ++) {
                    Data<X,Y> item = series.getData().get(itemIndex);
                    Node symbol = item.getNode();
                    if(get() && symbol == null) { // create any symbols
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
    public AreaChart(Axis<X> xAxis, Axis<Y> yAxis) {
        this(xAxis,yAxis, FXCollections.<Series<X,Y>>observableArrayList());
    }

    /**
     * Construct a new Area Chart with the given axis and data
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     * @param data The data to use, this is the actual list used so any changes to it will be reflected in the chart
     */
    public AreaChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X,Y>> data) {
        super(xAxis,yAxis);
        setLegend(legend);
        setData(data);
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------

    private static double doubleValue(Number number) { return doubleValue(number, 0); }
    private static double doubleValue(Number number, double nullDefault) {
        return (number == null) ? nullDefault : number.doubleValue();
    }

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
            if(xData != null && !(xData.size() == 1 && getXAxis().toNumericValue(xData.get(0)) == 0)) {
                xa.invalidateRange(xData);
            }
            if(yData != null && !(yData.size() == 1 && getYAxis().toNumericValue(yData.get(0)) == 0)) {
                ya.invalidateRange(yData);
            }
        }
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
                FadeTransition ft = new FadeTransition(Duration.millis(500),symbol);
                ft.setToValue(1);
                ft.play();
            }
            if (animate) {
                animate(
                    new KeyFrame(Duration.ZERO, new KeyValue(item.currentYProperty(),
                                        item.getCurrentY()),
                                        new KeyValue(item.currentXProperty(),
                                        item.getCurrentX())),
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
            if (itemIndex > 0 && itemIndex < series.getDataSize()-1) {
                animate = true;
                int index=0; Data<X,Y> d;
                for (d = series.begin; d != null && index != itemIndex - 1; d=d.next) index++;
                Data<X,Y> p1 = d;
                Data<X,Y> p2 = (d.next).next;
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
                ft.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent actionEvent) {
                        getPlotChildren().remove(symbol);
                        removeDataItemFromDisplay(series, item);
                    }
                });
                ft.play();
            }
            if (animate) {
                animate( new KeyFrame(Duration.ZERO, new KeyValue(item.currentYProperty(),
                            item.getCurrentY()), new KeyValue(item.currentXProperty(),
                            item.getCurrentX())),
                            new KeyFrame(Duration.millis(800), new EventHandler<ActionEvent>() {
                                @Override public void handle(ActionEvent actionEvent) {
                                    item.setSeries(null);
                                    getPlotChildren().remove(symbol);
                                    removeDataItemFromDisplay(series, item);
                                }
                            },
                            new KeyValue(item.currentYProperty(),
                            item.getYValue(), Interpolator.EASE_BOTH),
                            new KeyValue(item.currentXProperty(),
                            item.getXValue(), Interpolator.EASE_BOTH))
                );
            }
        } else {
            item.setSeries(null);
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
        // Note: is there a more efficient way of doing this?
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
                if (shouldAnimate()) {
                    symbol.setOpacity(0);
                    getPlotChildren().add(symbol);
                    // fade in new symbol
                    keyFrames.add(new KeyFrame(Duration.ZERO, new KeyValue(symbol.opacityProperty(), 0)));
                    keyFrames.add(new KeyFrame(Duration.millis(200), new KeyValue(symbol.opacityProperty(), 1)));
                }
                else {
                    getPlotChildren().add(symbol);
                }
            }
        }
        if (shouldAnimate()) animate(keyFrames.toArray(new KeyFrame[keyFrames.size()]));
    }
    private void updateDefaultColorIndex(final Series<X,Y> series) {
        int clearIndex = seriesColorMap.get(series);
        colorBits.clear(clearIndex);
        Path seriesLine = (Path)((Group)series.getNode()).getChildren().get(1);
        Path fillPath = (Path)((Group)series.getNode()).getChildren().get(0);
        if (seriesLine != null) {
            seriesLine.getStyleClass().remove(DEFAULT_COLOR+clearIndex);
        }
        if (fillPath != null) {
            fillPath.getStyleClass().remove(DEFAULT_COLOR+clearIndex);
        }
        colorBits.clear(clearIndex);
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
        // remove series Y multiplier
        seriesYMultiplierMap.remove(series);
        // remove all symbol nodes
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
            Timeline tl = new Timeline();
            tl.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO,startValues),
                new KeyFrame(Duration.millis(400), new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent actionEvent) {
                        getPlotChildren().removeAll(nodes);
                        removeSeriesFromDisplay(series);
                    }
                },endValues)
            );
            tl.play();
        } else {
            getPlotChildren().remove(series.getNode());
            for (Data d:series.getData()) getPlotChildren().remove(d.getNode());
            removeSeriesFromDisplay(series);
        }
    }

    /** @inheritDoc */
    @Override protected void layoutPlotChildren() {
        for (int seriesIndex=0; seriesIndex < getDataSize(); seriesIndex++) {
            Series<X, Y> series = getData().get(seriesIndex);
            DoubleProperty seriesYAnimMultiplier = seriesYMultiplierMap.get(series);
            boolean isFirst = true;
            double lastX = 0;
            Path seriesLine = (Path)((Group)series.getNode()).getChildren().get(1);
            Path fillPath = (Path)((Group)series.getNode()).getChildren().get(0);
            seriesLine.getElements().clear();
            fillPath.getElements().clear();
            for (Data<X, Y> item = series.begin; item != null; item = item.next) {
                double x = lastX = getXAxis().getDisplayPosition(item.getCurrentX());
                double y = getYAxis().getDisplayPosition(
                        getYAxis().toRealValue(getYAxis().toNumericValue(item.getCurrentY()) * seriesYAnimMultiplier.getValue()));
                if (isFirst) {
                    isFirst = false;
                    fillPath.getElements().add(new MoveTo(x, getYAxis().getZeroPosition()));
                    seriesLine.getElements().add(new MoveTo(x, y));
                } else {
                    seriesLine.getElements().add(new LineTo(x, y));
                }
                fillPath.getElements().add(new LineTo(x, y));
                Node symbol = item.getNode();
                if (symbol != null) {
                    final double w = symbol.prefWidth(-1);
                    final double h = symbol.prefHeight(-1);
                    symbol.resizeRelocate(x-(w/2), y-(h/2),w,h);
                }
            }
            if (fillPath.getElements().size() >= 1) {
                fillPath.getElements().add(new LineTo(lastX, getYAxis().getZeroPosition()));
            } else {
                fillPath.getElements().add(new MoveTo(lastX, getYAxis().getZeroPosition()));
            }
            fillPath.getElements().add(new ClosePath());
        }
    }

    private Node createSymbol(Series series, int seriesIndex, final Data item, int itemIndex) {
        Node symbol = item.getNode();
        // check if symbol has already been created
        if (symbol == null && getCreateSymbols()) {
            symbol = new StackPane();
            item.setNode(symbol);
        }
        // set symbol styles
        // Note: not sure if we want to add or check, ie be more careful and efficient here
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

    // -------------- STYLESHEET HANDLING --------------------------------------

    private static class StyleableProperties {
        private static final CssMetaData<AreaChart<?,?>,Boolean> CREATE_SYMBOLS = 
            new CssMetaData<AreaChart<?,?>,Boolean>("-fx-create-symbols",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(AreaChart node) {
                return node.createSymbols == null || !node.createSymbols.isBound();
}

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(AreaChart node) {
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
