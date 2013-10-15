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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import com.sun.javafx.charts.Legend;
import com.sun.javafx.charts.Legend.LegendItem;
import javafx.css.StyleableDoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import com.sun.javafx.css.converters.SizeConverter;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;

/**
 * A chart that plots bars indicating data values for a category. The bars can be vertical or horizontal depending on
 * which axis is a category axis.
 * @since JavaFX 2.0
 */
public class BarChart<X,Y> extends XYChart<X,Y> {

    // -------------- PRIVATE FIELDS -------------------------------------------
    
    private Map<Series, Map<String, Data<X,Y>>> seriesCategoryMap = 
                                new HashMap<Series, Map<String, Data<X,Y>>>();
    private Legend legend = new Legend();
    private boolean seriesRemove = false;
    private final Orientation orientation;
    private CategoryAxis categoryAxis;
    private ValueAxis valueAxis;
    private Timeline dataRemoveTimeline;
    private Data<X,Y> dataItemBeingRemoved = null;
    private Series<X,Y> seriesOfDataRemoved = null;
    private double bottomPos  = 0;
    private static String NEGATIVE_STYLE = "negative";
    // -------------- PUBLIC PROPERTIES ----------------------------------------

    /** The gap to leave between bars in the same category */
    private DoubleProperty barGap = new StyleableDoubleProperty(4) {
        @Override protected void invalidated() {
            get();
            requestChartLayout();
        }
        
        public Object getBean() {
            return BarChart.this;
        }

        public String getName() {
            return "barGap";
        }

        public CssMetaData<BarChart<?,?>,Number> getCssMetaData() {
            return StyleableProperties.BAR_GAP;
        }
    };
    public final double getBarGap() { return barGap.getValue(); }
    public final void setBarGap(double value) { barGap.setValue(value); }
    public final DoubleProperty barGapProperty() { return barGap; }

    /** The gap to leave between bars in separate categories */
    private DoubleProperty categoryGap = new StyleableDoubleProperty(10) {
        @Override protected void invalidated() {
            get();
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return BarChart.this;
        }

        @Override
        public String getName() {
            return "categoryGap";
        }

        public CssMetaData<BarChart<?,?>,Number> getCssMetaData() {
            return StyleableProperties.CATEGORY_GAP;
        }
    };
    public final double getCategoryGap() { return categoryGap.getValue(); }
    public final void setCategoryGap(double value) { categoryGap.setValue(value); }
    public final DoubleProperty categoryGapProperty() { return categoryGap; }

    // -------------- CONSTRUCTOR ----------------------------------------------

    /**
     * Construct a new BarChart with the given axis. The two axis should be a ValueAxis/NumberAxis and a CategoryAxis,
     * they can be in either order depending on if you want a horizontal or vertical bar chart.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     */
    public BarChart(Axis<X> xAxis, Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.<Series<X, Y>>observableArrayList());
    }

    /**
     * Construct a new BarChart with the given axis and data. The two axis should be a ValueAxis/NumberAxis and a
     * CategoryAxis, they can be in either order depending on if you want a horizontal or vertical bar chart.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     * @param data The data to use, this is the actual list used so any changes to it will be reflected in the chart
     */
    public BarChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X,Y>> data) {
        super(xAxis, yAxis);
        getStyleClass().add("bar-chart");
        setLegend(legend);
        if (!((xAxis instanceof ValueAxis && yAxis instanceof CategoryAxis) ||
             (yAxis instanceof ValueAxis && xAxis instanceof CategoryAxis))) {
            throw new IllegalArgumentException("Axis type incorrect, one of X,Y should be CategoryAxis and the other NumberAxis");
        }
        if (xAxis instanceof CategoryAxis) {
            categoryAxis = (CategoryAxis)xAxis;
            valueAxis = (ValueAxis)yAxis;
            orientation = Orientation.VERTICAL;
        } else {
            categoryAxis = (CategoryAxis)yAxis;
            valueAxis = (ValueAxis)xAxis;
            orientation = Orientation.HORIZONTAL;
        }
        // update css
        pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, orientation == Orientation.HORIZONTAL);
        pseudoClassStateChanged(VERTICAL_PSEUDOCLASS_STATE, orientation == Orientation.VERTICAL);
        setData(data);
    }

    /**
     * Construct a new BarChart with the given axis and data. The two axis should be a ValueAxis/NumberAxis and a
     * CategoryAxis, they can be in either order depending on if you want a horizontal or vertical bar chart.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     * @param data The data to use, this is the actual list used so any changes to it will be reflected in the chart
     * @param categoryGap The gap to leave between bars in separate categories
     */
     public BarChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X,Y>> data, double categoryGap) {
        this(xAxis, yAxis);
        setData(data);
        setCategoryGap(categoryGap);
    }
     
    // -------------- PROTECTED METHODS ----------------------------------------
     
    @Override protected void dataItemAdded(Series<X,Y> series, int itemIndex, Data<X,Y> item) {
        String category;
        if (orientation == Orientation.VERTICAL) {
            category = (String)item.getXValue();
        } else {
            category = (String)item.getYValue();
        }
         Map<String, Data<X,Y>> categoryMap = seriesCategoryMap.get(series);

        if (categoryMap == null) {
            categoryMap = new HashMap<String, Data<X,Y>>();
            seriesCategoryMap.put(series, categoryMap);
        }
        // check if category is already present
        if (!categoryAxis.getCategories().contains(category)) {
            // note: cat axis categories can be updated only when autoranging is true.
            categoryAxis.getCategories().add(itemIndex, category);
        } else if (categoryMap.containsKey(category)){
            // RT-21162 : replacing the previous data, first remove the node from scenegraph.
            Data data = categoryMap.get(category);
            getPlotChildren().remove(data.getNode());
            removeDataItemFromDisplay(series, data);
            requestChartLayout();
            categoryMap.remove(category);
        }
        categoryMap.put(category, item);
        Node bar = createBar(series, getData().indexOf(series), item, itemIndex);
        if (shouldAnimate()) {
            if (dataRemoveTimeline != null && dataRemoveTimeline.getStatus().equals(Animation.Status.RUNNING)) {
                if (dataItemBeingRemoved != null && dataItemBeingRemoved == item) {
                    dataRemoveTimeline.stop();
                    getPlotChildren().remove(bar);
                    removeDataItemFromDisplay(seriesOfDataRemoved, item);
                    dataItemBeingRemoved = null;
                    seriesOfDataRemoved = null;
                }
            }
            animateDataAdd(item, bar);
        } else {
            getPlotChildren().add(bar);
        }
    }

    @Override protected void dataItemRemoved(final Data<X,Y> item, final Series<X,Y> series) {
        final Node bar = item.getNode();
        if (shouldAnimate()) {
            dataRemoveTimeline = createDataRemoveTimeline(item, bar, series);
            dataItemBeingRemoved = item;
            seriesOfDataRemoved = series;
            dataRemoveTimeline.setOnFinished(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    item.setSeries(null);
                    getPlotChildren().remove(bar);
                    removeDataItemFromDisplay(series, item);
                    dataItemBeingRemoved = null;
                    updateMap(series, item);
                }
            });
            dataRemoveTimeline.play();
        } else {
            item.setSeries(null);
            getPlotChildren().remove(bar);
            removeDataItemFromDisplay(series, item);
            updateMap(series, item);
        }
    }

    /** @inheritDoc */
    @Override protected void dataItemChanged(Data<X, Y> item) {
         double barVal;
         double currentVal;
        if (orientation == Orientation.VERTICAL) {
             barVal = ((Number)item.getYValue()).doubleValue();
             currentVal = ((Number)item.getCurrentY()).doubleValue();
        } else {
             barVal = ((Number)item.getXValue()).doubleValue();
             currentVal = ((Number)item.getCurrentX()).doubleValue();
        }
         if (currentVal > 0 && barVal < 0) { // going from positive to negative
             // add style class negative
             item.getNode().getStyleClass().add(NEGATIVE_STYLE);
         } else if (currentVal < 0 && barVal > 0) { // going from negative to positive
             // remove style class negative
             // RT-21164 upside down bars: was adding NEGATIVE_STYLE styleclass
             // instead of removing it; when going from negative to positive
             item.getNode().getStyleClass().remove(NEGATIVE_STYLE);
         }
    }
    
    @Override protected void seriesAdded(Series<X,Y> series, int seriesIndex) {
        // handle any data already in series
        // create entry in the map
        Map<String, Data<X,Y>> categoryMap = new HashMap<String, Data<X,Y>>();
        for (int j=0; j<series.getData().size(); j++) {
            Data<X,Y> item = series.getData().get(j);
            Node bar = createBar(series, seriesIndex, item, j);
            String category;
            if (orientation == Orientation.VERTICAL) {
                category = (String)item.getXValue();
            } else {
                category = (String)item.getYValue();
            }
            categoryMap.put(category, item);
            if (shouldAnimate()) {
                animateDataAdd(item, bar);
            } else {
                // RT-21164 check if bar value is negative to add NEGATIVE_STYLE style class 
                double barVal = (orientation == Orientation.VERTICAL) ? ((Number)item.getYValue()).doubleValue() :
                        ((Number)item.getXValue()).doubleValue();
                if (barVal < 0) {
                    bar.getStyleClass().add(NEGATIVE_STYLE);
                }
                getPlotChildren().add(bar);
            }
        }
        if (categoryMap.size() > 0) seriesCategoryMap.put(series, categoryMap);
    }
    
    @Override protected void seriesRemoved(final Series<X,Y> series) {
        updateDefaultColorIndex(series);
        // remove all symbol nodes
        if (shouldAnimate()) {
            ParallelTransition pt = new ParallelTransition();
            pt.setOnFinished(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    removeSeriesFromDisplay(series);
                }
            });
            for (final Data<X,Y> d : series.getData()) {
                final Node bar = d.getNode();
                seriesRemove = true;
                // Animate series deletion
                if (getSeriesSize() > 1) {
                    for (int j=0; j< series.getData().size(); j++) {
                        Data<X,Y> item = series.getData().get(j);
                        Timeline t = createDataRemoveTimeline(item, bar, series);
                        pt.getChildren().add(t);
                    }
                } else {
                    // fade out last series
                    FadeTransition ft = new FadeTransition(Duration.millis(700),bar);
                    ft.setFromValue(1);
                    ft.setToValue(0);
                    ft.setOnFinished(new EventHandler<ActionEvent>() {
                         @Override public void handle(ActionEvent actionEvent) {
                            getPlotChildren().remove(bar);
                            updateMap(series, d);
                         }
                    });
                    pt.getChildren().add(ft);
                }
            }
            pt.play();
        } else {
            for (Data<X,Y> d : series.getData()) {
                final Node bar = d.getNode();
                getPlotChildren().remove(bar);
                updateMap(series, d);
            }
            removeSeriesFromDisplay(series);
        }
    }

    /** @inheritDoc */
    @Override protected void layoutPlotChildren() {
        double catSpace = categoryAxis.getCategorySpacing();
        // calculate bar spacing
        final double avilableBarSpace = catSpace - (getCategoryGap() + getBarGap());
        double barWidth = (avilableBarSpace / getSeriesSize()) - getBarGap();
        final double barOffset = -((catSpace - getCategoryGap()) / 2);
        final double zeroPos = (valueAxis.getLowerBound() > 0) ? 
                valueAxis.getDisplayPosition(valueAxis.getLowerBound()) : valueAxis.getZeroPosition();
        // RT-24813 : if the data in a series gets too large, barWidth can get negative.
        if (barWidth <= 0) barWidth = 1;
        // update bar positions and sizes
        int catIndex = 0;
        for (String category : categoryAxis.getCategories()) {
            int index = 0;
            for (Series<X,Y> series = begin; series != null; series = series.next) {
                final Data<X,Y> item = getDataItem(series, index, catIndex, category);
                if (item != null) {
                    final Node bar = item.getNode();
                    final double categoryPos;
                    final double valPos;
                    if (orientation == Orientation.VERTICAL) {
                        categoryPos = getXAxis().getDisplayPosition(item.getCurrentX());
                        valPos = getYAxis().getDisplayPosition(item.getCurrentY());
                    } else {
                        categoryPos = getYAxis().getDisplayPosition(item.getCurrentY());
                        valPos = getXAxis().getDisplayPosition(item.getCurrentX());
                    }
                    final double bottom = Math.min(valPos,zeroPos);
                    final double top = Math.max(valPos,zeroPos);
                    bottomPos = bottom;
                    if (orientation == Orientation.VERTICAL) {
                        bar.resizeRelocate( categoryPos + barOffset + (barWidth + getBarGap()) * index,
                                            bottom, barWidth, top-bottom);
                    } else {
                        //noinspection SuspiciousNameCombination
                        bar.resizeRelocate( bottom, categoryPos + barOffset + (barWidth + getBarGap()) * index,
                                            top-bottom, barWidth);
                    }

                    index++;
                }
            }
            catIndex++;
        }
    }

    /**
     * This is called whenever a series is added or removed and the legend needs to be updated
     */
    @Override protected void updateLegend() {
        legend.getItems().clear();
        if (getData() != null) {
            for (int seriesIndex=0; seriesIndex < getData().size(); seriesIndex++) {
                Series series = getData().get(seriesIndex);
                LegendItem legenditem = new LegendItem(series.getName());
                legenditem.getSymbol().getStyleClass().addAll("chart-bar","series"+seriesIndex,"bar-legend-symbol",
                        series.defaultColorStyleClass);
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
    
    // -------------- PRIVATE METHODS ------------------------------------------
    
    private void updateMap(Series series, Data item) {
        final String category = (orientation == Orientation.VERTICAL) ? (String)item.getXValue() :
                                     (String)item.getYValue();
        Map<String, Data<X,Y>> categoryMap = seriesCategoryMap.get(series);
        if (categoryMap != null) {
            categoryMap.remove(category);
            if (categoryMap.isEmpty()) seriesCategoryMap.remove(series);
        }
        if (seriesCategoryMap.isEmpty() && categoryAxis.isAutoRanging()) categoryAxis.getCategories().clear();
    }
    private void animateDataAdd(Data<X,Y> item, Node bar) {
        double barVal;
        if (orientation == Orientation.VERTICAL) {
            barVal = ((Number)item.getYValue()).doubleValue();
            if (barVal < 0) {
                bar.getStyleClass().add(NEGATIVE_STYLE);
            }
            item.setCurrentY(getYAxis().toRealValue((barVal < 0) ? -bottomPos : bottomPos));
            getPlotChildren().add(bar);
            item.setYValue(getYAxis().toRealValue(barVal));
            animate(
                new KeyFrame(Duration.ZERO, new KeyValue(item.currentYProperty(),
                item.getCurrentY())),
                new KeyFrame(Duration.millis(700),
                new KeyValue(item.currentYProperty(), item.getYValue(), Interpolator.EASE_BOTH))
            );
        } else {
            barVal = ((Number)item.getXValue()).doubleValue();
            if (barVal < 0) {
                bar.getStyleClass().add(NEGATIVE_STYLE);
            }
            item.setCurrentX(getXAxis().toRealValue((barVal < 0) ? -bottomPos : bottomPos));
            getPlotChildren().add(bar);
            item.setXValue(getXAxis().toRealValue(barVal));
            animate(
                new KeyFrame(Duration.ZERO, new KeyValue(item.currentXProperty(),
                item.getCurrentX())),
                new KeyFrame(Duration.millis(700),
                new KeyValue(item.currentXProperty(), item.getXValue(), Interpolator.EASE_BOTH))
            );
        }
    }

    private Timeline createDataRemoveTimeline(final Data<X,Y> item, final Node bar, final Series<X,Y> series) {
        Timeline t = new Timeline();
        if (orientation == Orientation.VERTICAL) {
//            item.setYValue(getYAxis().toRealValue(getYAxis().getZeroPosition()));
            item.setYValue(getYAxis().toRealValue(bottomPos));
            t.getKeyFrames().addAll(new KeyFrame(Duration.ZERO,
                                    new KeyValue(item.currentYProperty(), item.getCurrentY())),
                                    new KeyFrame(Duration.millis(700), new EventHandler<ActionEvent>() {
                                    @Override public void handle(ActionEvent actionEvent) {
                                        getPlotChildren().remove(bar);
                                        updateMap(series, item);
                                    }
                                },
                                new KeyValue(item.currentYProperty(), item.getYValue(),
                                Interpolator.EASE_BOTH) ));
        } else {
            item.setXValue(getXAxis().toRealValue(getXAxis().getZeroPosition()));
            t.getKeyFrames().addAll(new KeyFrame(Duration.ZERO, new KeyValue(item.currentXProperty(), item.getCurrentX())),
                new KeyFrame(Duration.millis(700), new EventHandler<ActionEvent>() {
                        @Override public void handle(ActionEvent actionEvent) {
                            getPlotChildren().remove(bar);
                            updateMap(series, item);
                        }
                    },
                    new KeyValue(item.currentXProperty(), item.getXValue(),
                            Interpolator.EASE_BOTH) ));
        }
        return t;
    }
    
    private void updateDefaultColorIndex(final Series<X,Y> series) {
        int clearIndex = seriesColorMap.get(series);
        colorBits.clear(clearIndex);
        for (Data<X,Y> d : series.getData()) {
            final Node bar = d.getNode();
            if (bar != null) {
                bar.getStyleClass().remove(DEFAULT_COLOR+clearIndex);
                colorBits.clear(clearIndex);
            }
        }
        seriesColorMap.remove(series);
    }

    private Node createBar(Series series, int seriesIndex, final Data item, int itemIndex) {
        Node bar = item.getNode();
        if (bar == null) {
            bar = new StackPane();
            item.setNode(bar);
        }
        bar.getStyleClass().addAll("chart-bar", "series" + seriesIndex, "data" + itemIndex,series.defaultColorStyleClass);
        return bar;
    }

    private Data<X,Y> getDataItem(Series<X,Y> series, int seriesIndex, int itemIndex, String category) {
        Map<String, Data<X,Y>> catmap = seriesCategoryMap.get(series);
        return (catmap != null) ? catmap.get(category) : null;
    }

    // -------------- STYLESHEET HANDLING ------------------------------------------------------------------------------

    /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatAsPrivate implementation detail
      */
     private static class StyleableProperties {
         private static final CssMetaData<BarChart<?,?>,Number> BAR_GAP = 
             new CssMetaData<BarChart<?,?>,Number>("-fx-bar-gap",
                 SizeConverter.getInstance(), 4.0) {

            @Override
            public boolean isSettable(BarChart<?,?> node) {
                return node.barGap == null || !node.barGap.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(BarChart<?,?> node) {
                return (StyleableProperty<Number>)node.barGapProperty();
            }
        };
         
         private static final CssMetaData<BarChart<?,?>,Number> CATEGORY_GAP = 
             new CssMetaData<BarChart<?,?>,Number>("-fx-category-gap",
                 SizeConverter.getInstance(), 10.0)  {

            @Override
            public boolean isSettable(BarChart<?,?> node) {
                return node.categoryGap == null || !node.categoryGap.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(BarChart<?,?> node) {
                return (StyleableProperty<Number>)node.categoryGapProperty();
            }
        };

         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(XYChart.getClassCssMetaData());
            styleables.add(BAR_GAP);
            styleables.add(CATEGORY_GAP);
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

    /** Pseudoclass indicating this is a vertical chart. */
    private static final PseudoClass VERTICAL_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("vertical");

    /** Pseudoclass indicating this is a horizontal chart. */
    private static final PseudoClass HORIZONTAL_PSEUDOCLASS_STATE = 
            PseudoClass.getPseudoClass("horizontal");

}
