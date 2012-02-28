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
import java.util.Iterator;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;
import javafx.util.Duration;

import com.sun.javafx.charts.Legend;
import com.sun.javafx.charts.Legend.LegendItem;

/**
 * Chart type that plots bubbles for the data points in a series. The extra value property of Data is used to represent
 * the radius of the bubble it should be a java.lang.Number.
 */
public class BubbleChart<X,Y> extends XYChart<X,Y> {

    // -------------- PRIVATE FIELDS ------------------------------------------

    private Legend legend = new Legend();

    // -------------- CONSTRUCTORS ----------------------------------------------

    /**
     * Construct a new BubbleChart with the given axis.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     */
    public BubbleChart(Axis<X> xAxis, Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.<Series<X, Y>>observableArrayList());
    }

    /**
     * Construct a new BubbleChart with the given axis and data.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     * @param data The data to use, this is the actual list used so any changes to it will be reflected in the chart
     */
    public BubbleChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X,Y>> data) {
        super(xAxis, yAxis);
        setLegend(legend);
        setData(data);
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------

    /**
     * Used to get a double value from a object that can be a Number object or null
     *
     * @param number Object possibly a instance of Number
     * @param nullDefault What value to return if the number object is null or not a Number
     * @return number converted to double or nullDefault
     */
    private static double getDoubleValue(Object number, double nullDefault) {
        return !(number instanceof Number) ? nullDefault : ((Number)number).doubleValue();
    }

    /** @inheritDoc */
    @Override protected void layoutPlotChildren() {
        // update bubble positions
      for (int seriesIndex=0; seriesIndex < getDataSize(); seriesIndex++) {
            Series<X,Y> series = getData().get(seriesIndex);
//            for (Data<X,Y> item = series.begin; item != null; item = item.next) {
            Iterator<Data<X,Y>> iter = getDisplayedDataIterator(series);
            while(iter.hasNext()) {
                Data<X,Y> item = iter.next();
                double x = getXAxis().getDisplayPosition(item.getCurrentX());
                double y = getYAxis().getDisplayPosition(item.getCurrentY());
                Node bubble = item.getNode();
                Ellipse ellipse;
                if (bubble != null) {
                    if (bubble instanceof StackPane) {
                        StackPane region = (StackPane)item.getNode();
                        if (region.impl_getShape() == null) {
                            ellipse = new Ellipse(getDoubleValue(item.getExtraValue(), 1), getDoubleValue(item.getExtraValue(), 1));
                            region.impl_setShape(ellipse);
                        } else if (region.impl_getShape() instanceof Ellipse) {
                            ellipse = (Ellipse)region.impl_getShape();
                        } else {
                            return;
                        }
                        ellipse.setRadiusX(getDoubleValue(item.getExtraValue(), 1) * Math.abs(((NumberAxis)getXAxis()).getScale()));
                        ellipse.setRadiusY(getDoubleValue(item.getExtraValue(), 1) * Math.abs(((NumberAxis)getYAxis()).getScale()));
                        // Note: workaround for RT-7689 - saw this in ProgressControlSkin
                        // The region doesn't update itself when the shape is mutated in place, so we
                        // null out and then restore the shape in order to force invalidation.
                        region.impl_setShape(null);
                        region.impl_setShape(ellipse);
                        region.impl_setScaleShape(false);
                        region.impl_setPositionShape(false);
                        // position the bubble
                        bubble.setLayoutX(x);
                        bubble.setLayoutY(y);
                    }
                }
            }
        }
    }

    @Override protected void dataItemAdded(Series<X,Y> series, int itemIndex, Data<X,Y> item) {
        Node bubble = createBubble(series, getData().indexOf(series), item, itemIndex);
        if (shouldAnimate()) {
            bubble.setOpacity(0);
            getPlotChildren().add(bubble);
            // fade in new bubble
            FadeTransition ft = new FadeTransition(Duration.millis(500),bubble);
            ft.setToValue(1);
            ft.play();
        } else {
            getPlotChildren().add(bubble);
        }
    }

    @Override protected  void dataItemRemoved(final Data<X,Y> item, final Series<X,Y> series) {
        final Node bubble = item.getNode();
        if (shouldAnimate()) {
            // fade out old bubble
            FadeTransition ft = new FadeTransition(Duration.millis(500),bubble);
            ft.setToValue(0);
            ft.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent actionEvent) {
                    getPlotChildren().remove(bubble);
                    removeDataItemFromDisplay(series, item);
                }
            });
            ft.play();
        } else {
            getPlotChildren().remove(bubble);
            removeDataItemFromDisplay(series, item);
        }
    }

    /** @inheritDoc */
    @Override protected void dataItemChanged(Data<X, Y> item) {
    }
    
    @Override protected  void seriesAdded(Series<X,Y> series, int seriesIndex) {
        // handle any data already in series
        for (int j=0; j<series.getData().size(); j++) {
            Data item = series.getData().get(j);
            Node bubble = createBubble(series, seriesIndex, item, j);
            if (shouldAnimate()) {
                bubble.setOpacity(0);
                getPlotChildren().add(bubble);
                // fade in new bubble
                FadeTransition ft = new FadeTransition(Duration.millis(500),bubble);
                ft.setToValue(1);
                ft.play();
            } else {
                getPlotChildren().add(bubble);
            }
        }
    }

    @Override protected  void seriesRemoved(final Series<X,Y> series) {
        // remove all bubble nodes
        if (shouldAnimate()) {
            ParallelTransition pt = new ParallelTransition();
            pt.setOnFinished(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    removeSeriesFromDisplay(series);
                }
            });
            for (XYChart.Data<X,Y> d : series.getData()) {
                final Node bubble = d.getNode();
                // fade out old bubble
                FadeTransition ft = new FadeTransition(Duration.millis(500),bubble);
                ft.setToValue(0);
                ft.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent actionEvent) {
                        getPlotChildren().remove(bubble);
                    }
                });
                pt.getChildren().add(ft);
            }
            pt.play();
        } else {
            for (XYChart.Data<X,Y> d : series.getData()) {
                final Node bubble = d.getNode();
                getPlotChildren().remove(bubble);
            }
            removeSeriesFromDisplay(series);
        }

    }

    /**
     * Create a Bubble for a given data item if it doesn't already have a node
     *
     *
     * @param series
     * @param seriesIndex The index of the series containing the item
     * @param item        The data item to create node for
     * @param itemIndex   The index of the data item in the series
     * @return Node used for given data item
     */
    private Node createBubble(Series<X, Y> series, int seriesIndex, final Data item, int itemIndex) {
        Node bubble = item.getNode();
        // check if bubble has already been created
        if (bubble == null) {
            bubble = new StackPane();
            item.setNode(bubble);
        }
        // set bubble styles
        bubble.getStyleClass().setAll("chart-bubble", "series" + seriesIndex, "data" + itemIndex,
                series.defaultColorStyleClass);
        return bubble;
    }

    /**
     * This is called when the range has been invalidated and we need to update it. If the axis are auto
     * ranging then we compile a list of all data that the given axis has to plot and call invalidateRange() on the
     * axis passing it that data.
     */
    @Override protected void updateAxisRange() {
        // For bubble chart we need to override this method as we need to let the axis know that they need to be able
        // to cover the whole area occupied by the bubble not just its center data value
        final Axis<X> xa = getXAxis();
        final Axis<Y> ya = getYAxis();
        List<X> xData = null;
        List<Y> yData = null;
        if(xa.isAutoRanging()) xData = new ArrayList<X>();
        if(ya.isAutoRanging()) yData = new ArrayList<Y>();
        final boolean xIsCategory = xa instanceof CategoryAxis;
        final boolean yIsCategory = ya instanceof CategoryAxis;
        if(xData != null || yData != null) {
            for(Series<X,Y> series : getData()) {
                for(Data<X,Y> data: series.getData()) {
                    if(xData != null) {
                        if(xIsCategory) {
                            xData.add(data.getXValue());
                        } else {
                            xData.add(xa.toRealValue(xa.toNumericValue(data.getXValue()) + getDoubleValue(data.getExtraValue(), 0)));
                            xData.add(xa.toRealValue(xa.toNumericValue(data.getXValue()) - getDoubleValue(data.getExtraValue(), 0)));
                        }
                    }
                    if(yData != null){
                        if(yIsCategory) {
                            yData.add(data.getYValue());
                        } else {
                            yData.add(ya.toRealValue(ya.toNumericValue(data.getYValue()) + getDoubleValue(data.getExtraValue(), 0)));
                            yData.add(ya.toRealValue(ya.toNumericValue(data.getYValue()) - getDoubleValue(data.getExtraValue(), 0)));
                        }
                    }
                }
            }
            if(xData != null) xa.invalidateRange(xData);
            if(yData != null) ya.invalidateRange(yData);
        }
    }

    /**
     * This is called whenever a series is added or removed and the legend needs to be updated
     */
    @Override protected void updateLegend() {
        legend.getItems().clear();
        if (getData() != null) {
            for (int seriesIndex=0; seriesIndex< getData().size(); seriesIndex++) {
                Series<X,Y> series = getData().get(seriesIndex);
                LegendItem legenditem = new LegendItem(series.getName());
                legenditem.getSymbol().getStyleClass().addAll("series"+seriesIndex,"chart-bubble",
                        "bubble-legend-symbol", series.defaultColorStyleClass);
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
}
