/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import com.sun.javafx.charts.Legend.LegendItem;

import javafx.css.StyleableDoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;

import javafx.css.converter.SizeConverter;

import javafx.collections.ListChangeListener;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;


/**
 * StackedBarChart is a variation of {@link BarChart} that plots bars indicating
 * data values for a category. The bars can be vertical or horizontal depending
 * on which axis is a category axis.
 * The bar for each series is stacked on top of the previous series.
 * @since JavaFX 2.1
 */
public class StackedBarChart<X, Y> extends XYChart<X, Y> {

    // -------------- PRIVATE FIELDS -------------------------------------------
    private Map<Series<X, Y>, Map<String, List<Data<X, Y>>>> seriesCategoryMap =
            new HashMap<>();
    private final Orientation orientation;
    private CategoryAxis categoryAxis;
    private ValueAxis valueAxis;
    private ParallelTransition parallelTransition;
    // RT-23125 handling data removal when a category is removed.
    private ListChangeListener<String> categoriesListener = new ListChangeListener<>() {
        @Override public void onChanged(ListChangeListener.Change<? extends String> c) {
            while (c.next()) {
                for(String cat : c.getRemoved()) {
                    for (Series<X, Y> series : getData()) {
                        for (Data<X, Y> data : series.getData()) {
                            if ((cat).equals((orientation == orientation.VERTICAL) ?
                                    data.getXValue() : data.getYValue())) {
                                boolean animatedOn = getAnimated();
                                setAnimated(false);
                                dataItemRemoved(data, series);
                                setAnimated(animatedOn);
                            }
                        }
                    }
                    requestChartLayout();
                }
            }
        }
    };

    // -------------- PUBLIC PROPERTIES ----------------------------------------
    /** The gap to leave between bars in separate categories */
    private DoubleProperty categoryGap = new StyleableDoubleProperty(10) {
        @Override protected void invalidated() {
            get();
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return StackedBarChart.this;
        }

        @Override
        public String getName() {
            return "categoryGap";
        }

        @Override
        public CssMetaData<StackedBarChart<?,?>,Number> getCssMetaData() {
            return StackedBarChart.StyleableProperties.CATEGORY_GAP;
        }
    };

    public double getCategoryGap() {
        return categoryGap.getValue();
    }

    public void setCategoryGap(double value) {
        categoryGap.setValue(value);
    }

    public DoubleProperty categoryGapProperty() {
        return categoryGap;
    }

    // -------------- CONSTRUCTOR ----------------------------------------------
    /**
     * Construct a new StackedBarChart with the given axis. The two axis should be a ValueAxis/NumberAxis and a CategoryAxis,
     * they can be in either order depending on if you want a horizontal or vertical bar chart.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     */
    public StackedBarChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.<Series<X, Y>>observableArrayList());
    }

    /**
     * Construct a new StackedBarChart with the given axis and data. The two axis should be a ValueAxis/NumberAxis and a
     * CategoryAxis, they can be in either order depending on if you want a horizontal or vertical bar chart.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     * @param data The data to use, this is the actual list used so any changes to it will be reflected in the chart
     */
    public StackedBarChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis, @NamedArg("data") ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis);
        getStyleClass().add("stacked-bar-chart");
        if (!((xAxis instanceof ValueAxis && yAxis instanceof CategoryAxis)
                || (yAxis instanceof ValueAxis && xAxis instanceof CategoryAxis))) {
            throw new IllegalArgumentException("Axis type incorrect, one of X,Y should be CategoryAxis and the other NumberAxis");
        }
        if (xAxis instanceof CategoryAxis) {
            categoryAxis = (CategoryAxis) xAxis;
            valueAxis = (ValueAxis) yAxis;
            orientation = Orientation.VERTICAL;
        } else {
            categoryAxis = (CategoryAxis) yAxis;
            valueAxis = (ValueAxis) xAxis;
            orientation = Orientation.HORIZONTAL;
        }
        // update css
        pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, orientation == Orientation.HORIZONTAL);
        pseudoClassStateChanged(VERTICAL_PSEUDOCLASS_STATE, orientation == Orientation.VERTICAL);
        setData(data);
        categoryAxis.getCategories().addListener(categoriesListener);
    }

    /**
     * Construct a new StackedBarChart with the given axis and data. The two axis should be a ValueAxis/NumberAxis and a
     * CategoryAxis, they can be in either order depending on if you want a horizontal or vertical bar chart.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     * @param data The data to use, this is the actual list used so any changes to it will be reflected in the chart
     * @param categoryGap The gap to leave between bars in separate categories
     */
    public StackedBarChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis, @NamedArg("data") ObservableList<Series<X, Y>> data, @NamedArg("categoryGap") double categoryGap) {
        this(xAxis, yAxis);
        setData(data);
        setCategoryGap(categoryGap);
    }

    // -------------- METHODS --------------------------------------------------
    @Override protected void dataItemAdded(Series<X, Y> series, int itemIndex, Data<X, Y> item) {
        String category;
        if (orientation == Orientation.VERTICAL) {
            category = (String) item.getXValue();
        } else {
            category = (String) item.getYValue();
        }
        // Don't plot if category does not already exist ?
//        if (!categoryAxis.getCategories().contains(category)) return;

        Map<String, List<Data<X, Y>>> categoryMap = seriesCategoryMap.get(series);

        if (categoryMap == null) {
            categoryMap = new HashMap<>();
            seriesCategoryMap.put(series, categoryMap);
        }
        // list to hold more that one bar "positive and negative"
        List<Data<X, Y>> itemList = categoryMap.get(category) != null ? categoryMap.get(category) : new ArrayList<>();
        itemList.add(item);
        categoryMap.put(category, itemList);
//        categoryMap.put(category, item);
        Node bar = createBar(series, getData().indexOf(series), item, itemIndex);
        if (shouldAnimate()) {
            animateDataAdd(item, bar);
        } else {
            getPlotChildren().add(bar);
        }
    }

    @Override protected void dataItemRemoved(final Data<X, Y> item, final Series<X, Y> series) {
        final Node bar = item.getNode();

        if (bar != null) {
            bar.focusTraversableProperty().unbind();
        }

        if (shouldAnimate()) {
            Timeline t = createDataRemoveTimeline(item, bar, series);
            t.setOnFinished(event -> {
                removeDataItemFromDisplay(series, item);
            });
            t.play();
        } else {
            processDataRemove(series, item);
            removeDataItemFromDisplay(series, item);
        }
    }

    /** {@inheritDoc} */
    @Override protected void dataItemChanged(Data<X, Y> item) {
        double barVal;
        double currentVal;
        if (orientation == Orientation.VERTICAL) {
            barVal = ((Number) item.getYValue()).doubleValue();
            currentVal = ((Number) getCurrentDisplayedYValue(item)).doubleValue();
        } else {
            barVal = ((Number) item.getXValue()).doubleValue();
            currentVal = ((Number) getCurrentDisplayedXValue(item)).doubleValue();
        }
        if (currentVal > 0 && barVal < 0) { // going from positive to negative
            // add style class negative
            item.getNode().getStyleClass().add("negative");
        } else if (currentVal < 0 && barVal > 0) { // going from negative to positive
            // remove style class negative
            item.getNode().getStyleClass().remove("negative");
        }
    }

    @Override protected void seriesChanged(ListChangeListener.Change<? extends Series> c) {
        // Update style classes for all series lines and symbols
        // Note: is there a more efficient way of doing this?
        for (int i = 0; i < getDataSize(); i++) {
            final Series<X,Y> series = getData().get(i);
            for (int j=0; j<series.getData().size(); j++) {
                Data<X,Y> item = series.getData().get(j);
                Node bar = item.getNode();
                bar.getStyleClass().setAll("chart-bar", "series" + i, "data" + j, series.defaultColorStyleClass);
            }
        }
    }

    /** {@inheritDoc} */
    @Override protected void seriesAdded(Series<X, Y> series, int seriesIndex) {
        // handle any data already in series
        // create entry in the map
        Map<String, List<Data<X, Y>>> categoryMap = new HashMap<>();
        for (int j = 0; j < series.getData().size(); j++) {
            Data<X, Y> item = series.getData().get(j);
            Node bar = createBar(series, seriesIndex, item, j);
            String category;
            if (orientation == Orientation.VERTICAL) {
                category = (String) item.getXValue();
            } else {
                category = (String) item.getYValue();
            }
            // list of two item positive and negative
            List<Data<X, Y>> itemList = categoryMap.get(category) != null ? categoryMap.get(category) : new ArrayList<>();
            itemList.add(item);
            categoryMap.put(category, itemList);
            if (shouldAnimate()) {
                animateDataAdd(item, bar);
            } else {
                double barVal = (orientation == Orientation.VERTICAL) ? ((Number)item.getYValue()).doubleValue() :
                    ((Number)item.getXValue()).doubleValue();
                if (barVal < 0) {
                    bar.getStyleClass().add("negative");
                }
                getPlotChildren().add(bar);
            }
        }
        if (categoryMap.size() > 0) {
            seriesCategoryMap.put(series, categoryMap);
        }
    }

    @Override protected void seriesRemoved(final Series<X, Y> series) {
        // remove all symbol nodes
        if (shouldAnimate()) {
            parallelTransition = new ParallelTransition();
            parallelTransition.setOnFinished(event -> {
                removeSeriesFromDisplay(series);
                requestChartLayout();
            });
            for (Data<X, Y> d : series.getData()) {
                final Node bar = d.getNode();
                // Animate series deletion
                if (getSeriesSize() > 1) {
                    Timeline t = createDataRemoveTimeline(d, bar, series);
                    parallelTransition.getChildren().add(t);
                } else {
                    // fade out last series
                    FadeTransition ft = new FadeTransition(Duration.millis(700), bar);
                    ft.setFromValue(1);
                    ft.setToValue(0);
                    ft.setOnFinished(actionEvent -> {
                        processDataRemove(series, d);
                        bar.setOpacity(1.0);
                    });
                    parallelTransition.getChildren().add(ft);
                }
            }
            parallelTransition.play();
        } else {
            for (Data<X, Y> d : series.getData()) {
                processDataRemove(series, d);
            }
            removeSeriesFromDisplay(series);
            requestChartLayout();
        }
    }

    /** {@inheritDoc} */
    @Override protected void updateAxisRange() {
        // This override is necessary to update axis range based on cumulative Y value for the
        // Y axis instead of the inherited way where the max value in the data range is used.
        boolean categoryIsX = categoryAxis == getXAxis();
        if (categoryAxis.isAutoRanging()) {
            List cData = new ArrayList();
            for (Series<X, Y> series : getData()) {
                for (Data<X, Y> data : series.getData()) {
                    if (data != null) cData.add(categoryIsX ? data.getXValue() : data.getYValue());
                }
            }
            categoryAxis.invalidateRange(cData);
        }
        if (valueAxis.isAutoRanging()) {
            List<Number> vData = new ArrayList<>();
            for (String category : categoryAxis.getAllDataCategories()) {
                double totalXN = 0;
                double totalXP = 0;
                Iterator<Series<X, Y>> seriesIterator = getDisplayedSeriesIterator();
                while (seriesIterator.hasNext()) {
                    Series<X, Y> series = seriesIterator.next();
                    for (final Data<X, Y> item : getDataItem(series, category)) {
                        if (item != null) {
                            boolean isNegative = item.getNode().getStyleClass().contains("negative");
                            Number value = (Number) (categoryIsX ? item.getYValue() : item.getXValue());
                            if (!isNegative) {
                                totalXP += valueAxis.toNumericValue(value);
                            } else {
                                totalXN += valueAxis.toNumericValue(value);
                            }
                        }
                    }
                }
                vData.add(totalXP);
                vData.add(totalXN);
            }
            valueAxis.invalidateRange(vData);
        }
    }

    /** {@inheritDoc} */
    @Override protected void layoutPlotChildren() {
        double catSpace = categoryAxis.getCategorySpacing();
        // calculate bar spacing
        final double availableBarSpace = catSpace - getCategoryGap();
        final double barWidth = availableBarSpace;
        final double barOffset = -((catSpace - getCategoryGap()) / 2);
        // update bar positions and sizes
        for (String category : categoryAxis.getCategories()) {
            double currentPositiveValue = 0;
            double currentNegativeValue = 0;
            Iterator<Series<X, Y>> seriesIterator = getDisplayedSeriesIterator();
            while (seriesIterator.hasNext()) {
                Series<X, Y> series = seriesIterator.next();
                for (final Data<X, Y> item : getDataItem(series, category)) {
                    if (item != null) {
                        final Node bar = item.getNode();
                        final double categoryPos;
                        final double valNumber;
                        final X xValue = getCurrentDisplayedXValue(item);
                        final Y yValue = getCurrentDisplayedYValue(item);
                        if (orientation == Orientation.VERTICAL) {
                            categoryPos = getXAxis().getDisplayPosition(xValue);
                            valNumber = getYAxis().toNumericValue(yValue);
                        } else {
                            categoryPos = getYAxis().getDisplayPosition(yValue);
                            valNumber = getXAxis().toNumericValue(xValue);
                        }
                        double bottom;
                        double top;
                        boolean isNegative = bar.getStyleClass().contains("negative");
                        if (!isNegative) {
                            bottom = valueAxis.getDisplayPosition(currentPositiveValue);
                            top = valueAxis.getDisplayPosition(currentPositiveValue + valNumber);
                            currentPositiveValue += valNumber;
                        } else {
                            bottom = valueAxis.getDisplayPosition(currentNegativeValue + valNumber);
                            top = valueAxis.getDisplayPosition(currentNegativeValue);
                            currentNegativeValue += valNumber;
                        }

                        if (orientation == Orientation.VERTICAL) {
                            bar.resizeRelocate(categoryPos + barOffset,
                                    top, barWidth, bottom - top);
                        } else {
                            bar.resizeRelocate(bottom,
                                    categoryPos + barOffset,
                                    top - bottom, barWidth);
                        }
                    }
                }
            }
        }
    }

    @Override
    LegendItem createLegendItemForSeries(Series<X, Y> series, int seriesIndex) {
        LegendItem legendItem = new LegendItem(series.getName());
        legendItem.getSymbol().getStyleClass().addAll("chart-bar", "series" + seriesIndex,
                "bar-legend-symbol", series.defaultColorStyleClass);
        return legendItem;
    }

    private void updateMap(Series<X,Y> series, Data<X,Y> item) {
        final String category = (orientation == Orientation.VERTICAL) ? (String)item.getXValue() :
                                     (String)item.getYValue();
        Map<String, List<Data<X, Y>>> categoryMap = seriesCategoryMap.get(series);
        if (categoryMap != null) {
            categoryMap.remove(category);
            if (categoryMap.isEmpty()) seriesCategoryMap.remove(series);
        }
        if (seriesCategoryMap.isEmpty() && categoryAxis.isAutoRanging()) categoryAxis.getCategories().clear();
    }

    private void processDataRemove(final Series<X,Y> series, final Data<X,Y> item) {
        Node bar = item.getNode();
        getPlotChildren().remove(bar);
        updateMap(series, item);
    }

    private void animateDataAdd(Data<X, Y> item, Node bar) {
        double barVal;
        if (orientation == Orientation.VERTICAL) {
            barVal = ((Number) item.getYValue()).doubleValue();
            if (barVal < 0) {
                bar.getStyleClass().add("negative");
            }
            item.setYValue(getYAxis().toRealValue(getYAxis().getZeroPosition()));
            setCurrentDisplayedYValue(item, getYAxis().toRealValue(getYAxis().getZeroPosition()));
            getPlotChildren().add(bar);
            item.setYValue(getYAxis().toRealValue(barVal));
            animate(
                    new KeyFrame(Duration.ZERO, new KeyValue(
                            currentDisplayedYValueProperty(item),
                            getCurrentDisplayedYValue(item))),
                    new KeyFrame(Duration.millis(700), new KeyValue(
                            currentDisplayedYValueProperty(item),
                            item.getYValue(), Interpolator.EASE_BOTH))
            );
        } else {
            barVal = ((Number) item.getXValue()).doubleValue();
            if (barVal < 0) {
                bar.getStyleClass().add("negative");
            }
            item.setXValue(getXAxis().toRealValue(getXAxis().getZeroPosition()));
            setCurrentDisplayedXValue(item, getXAxis().toRealValue(getXAxis().getZeroPosition()));
            getPlotChildren().add(bar);
            item.setXValue(getXAxis().toRealValue(barVal));
            animate(
                    new KeyFrame(Duration.ZERO, new KeyValue(
                            currentDisplayedXValueProperty(item),
                            getCurrentDisplayedXValue(item))),
                    new KeyFrame(Duration.millis(700), new KeyValue(
                            currentDisplayedXValueProperty(item),
                            item.getXValue(), Interpolator.EASE_BOTH))
            );
        }
    }

    private Timeline createDataRemoveTimeline(Data<X, Y> item, final Node bar, final Series<X, Y> series) {
        Timeline t = new Timeline();
        if (orientation == Orientation.VERTICAL) {
            t.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO, new KeyValue(
                            currentDisplayedYValueProperty(item),
                            getCurrentDisplayedYValue(item))),
                    new KeyFrame(Duration.millis(700), actionEvent -> {
                        processDataRemove(series, item);
                    }, new KeyValue(
                            currentDisplayedYValueProperty(item),
                            item.getYValue(), Interpolator.EASE_BOTH))
            );
        } else {
            item.setXValue(getXAxis().toRealValue(getXAxis().getZeroPosition()));
            t.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO, new KeyValue(
                            currentDisplayedXValueProperty(item),
                            getCurrentDisplayedXValue(item))),
                    new KeyFrame(Duration.millis(700), actionEvent -> {
                        processDataRemove(series, item);
                    }, new KeyValue(
                            currentDisplayedXValueProperty(item),
                            item.getXValue(), Interpolator.EASE_BOTH))
            );
        }
        return t;
    }

    private Node createBar(Series<X, Y> series, int seriesIndex, final Data<X, Y> item, int itemIndex) {
        Node bar = item.getNode();
        if (bar == null) {
            bar = new StackPane();
            bar.setAccessibleRole(AccessibleRole.TEXT);
            bar.setAccessibleRoleDescription("Bar");
            bar.focusTraversableProperty().bind(Platform.accessibilityActiveProperty());
            item.setNode(bar);
        }
        bar.getStyleClass().setAll("chart-bar", "series" + seriesIndex, "data" + itemIndex, series.defaultColorStyleClass);
        return bar;
    }

    private List<Data<X, Y>> getDataItem(Series<X, Y> series, String category) {
        Map<String, List<Data<X, Y>>> catmap = seriesCategoryMap.get(series);
        return catmap != null ? catmap.get(category) != null ?
            catmap.get(category) : new ArrayList<>() : new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override void seriesBeingRemovedIsAdded(Series<X,Y> series) {
        if (parallelTransition != null) {
            parallelTransition.setOnFinished(null);
            parallelTransition.stop();
            parallelTransition = null;
            getPlotChildren().remove(series.getNode());
            for (Data<X,Y> d:series.getData()) getPlotChildren().remove(d.getNode());
            removeSeriesFromDisplay(series);
        }
    }

// -------------- STYLESHEET HANDLING ------------------------------------------------------------------------------

    /*
     * Super-lazy instantiation pattern from Bill Pugh.
     */
    private static class StyleableProperties {

        private static final CssMetaData<StackedBarChart<?,?>,Number> CATEGORY_GAP =
            new CssMetaData<>("-fx-category-gap",
                SizeConverter.getInstance(), 10.0)  {

            @Override
            public boolean isSettable(StackedBarChart<?,?> node) {
                return node.categoryGap == null || !node.categoryGap.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(StackedBarChart<?,?> node) {
                return (StyleableProperty<Number>)node.categoryGapProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                    new ArrayList<>(XYChart.getClassCssMetaData());
            styleables.add(CATEGORY_GAP);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
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
