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

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import com.sun.javafx.charts.Legend.LegendItem;

import java.util.Iterator;

/**
 * Chart type that plots symbols for the data points in a series.
 * @since JavaFX 2.0
 */
public class ScatterChart<X,Y> extends XYChart<X,Y> {

    // -------------- CONSTRUCTORS ----------------------------------------------

    /**
     * Construct a new ScatterChart with the given axis and data.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     */
    public ScatterChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.<Series<X, Y>>observableArrayList());
    }

    /**
     * Construct a new ScatterChart with the given axis and data.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     * @param data The data to use, this is the actual list used so any changes to it will be reflected in the chart
     */
    public ScatterChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis, @NamedArg("data") ObservableList<Series<X,Y>> data) {
        super(xAxis,yAxis);
        setData(data);
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override protected void dataItemAdded(Series<X,Y> series, int itemIndex, Data<X,Y> item) {
        Node symbol = item.getNode();
        // check if symbol has already been created
        if (symbol == null) {
            symbol = new StackPane();
            symbol.setAccessibleRole(AccessibleRole.TEXT);
            symbol.setAccessibleRoleDescription("Point");
            symbol.focusTraversableProperty().bind(Platform.accessibilityActiveProperty());
            item.setNode(symbol);
        }
        // set symbol styles
        symbol.getStyleClass().setAll("chart-symbol", "series" + getData().indexOf(series), "data" + itemIndex,
                series.defaultColorStyleClass);
        // add and fade in new symbol if animated
        if (shouldAnimate()) {
            symbol.setOpacity(0);
            getPlotChildren().add(symbol);
            FadeTransition ft = new FadeTransition(Duration.millis(500),symbol);
            ft.setToValue(1);
            ft.play();
        } else {
            getPlotChildren().add(symbol);
        }
    }

    /** {@inheritDoc} */
    @Override protected  void dataItemRemoved(final Data<X,Y> item, final Series<X,Y> series) {
        final Node symbol = item.getNode();

        if (symbol != null) {
            symbol.focusTraversableProperty().unbind();
        }

        if (shouldAnimate()) {
            // fade out old symbol
            FadeTransition ft = new FadeTransition(Duration.millis(500),symbol);
            ft.setToValue(0);
            ft.setOnFinished(actionEvent -> {
                getPlotChildren().remove(symbol);
                removeDataItemFromDisplay(series, item);
                symbol.setOpacity(1.0);
            });
            ft.play();
        } else {
            getPlotChildren().remove(symbol);
            removeDataItemFromDisplay(series, item);
        }
    }

    /** {@inheritDoc} */
    @Override protected void dataItemChanged(Data<X, Y> item) {
    }

    /** {@inheritDoc} */
    @Override protected  void seriesAdded(Series<X,Y> series, int seriesIndex) {
        // handle any data already in series
        for (int j=0; j<series.getData().size(); j++) {
            dataItemAdded(series,j,series.getData().get(j));
        }
    }

    /** {@inheritDoc} */
    @Override protected  void seriesRemoved(final Series<X,Y> series) {
        // remove all symbol nodes
        if (shouldAnimate()) {
            ParallelTransition pt = new ParallelTransition();
            pt.setOnFinished(event -> {
                removeSeriesFromDisplay(series);
            });
            for (final Data<X,Y> d : series.getData()) {
                final Node symbol = d.getNode();
                // fade out old symbol
                FadeTransition ft = new FadeTransition(Duration.millis(500),symbol);
                ft.setToValue(0);
                ft.setOnFinished(actionEvent -> {
                    getPlotChildren().remove(symbol);
                    symbol.setOpacity(1.0);
                });
                pt.getChildren().add(ft);
            }
            pt.play();
        } else {
             for (final Data<X,Y> d : series.getData()) {
                final Node symbol = d.getNode();
                getPlotChildren().remove(symbol);
            }
             removeSeriesFromDisplay(series);
        }
    }

    /** {@inheritDoc} */
    @Override protected void layoutPlotChildren() {
        // update symbol positions
        for (int seriesIndex=0; seriesIndex < getDataSize(); seriesIndex++) {
            Series<X,Y> series = getData().get(seriesIndex);
            for (Iterator<Data<X, Y>> it = getDisplayedDataIterator(series); it.hasNext(); ) {
                Data<X, Y> item = it.next();
                double x = getXAxis().getDisplayPosition(item.getCurrentX());
                double y = getYAxis().getDisplayPosition(item.getCurrentY());
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    continue;
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

    @Override
    LegendItem createLegendItemForSeries(Series<X, Y> series, int seriesIndex) {
        LegendItem legendItem = new LegendItem(series.getName());
        Node node = series.getData().isEmpty() ? null : series.getData().get(0).getNode();
        if (node != null) {
            legendItem.getSymbol().getStyleClass().addAll(node.getStyleClass());
        }
        return legendItem;
    }
}
