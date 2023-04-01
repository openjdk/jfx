/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.chart;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChartShim;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.Path;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import javafx.scene.shape.PathElement;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class AreaChartTest extends XYChartTestBase {
    AreaChart<Number,Number> ac;
    final XYChart.Series<Number, Number> series1 = new XYChart.Series<>();
    boolean useCategoryAxis = false;
    final String[] countries = {"USA", "Italy", "France", "China", "India"};
    @Override
    protected Chart createChart() {
        final NumberAxis yAxis = new NumberAxis(0, 30, 2);
        ObservableList<XYChart.Data> data = FXCollections.observableArrayList();
        Axis xAxis;
        if (useCategoryAxis) {
            xAxis = new CategoryAxis();
            ((CategoryAxis)xAxis).setCategories(FXCollections.observableArrayList(countries));
            // add starting data
        series1.getData().add(new XYChart.Data(countries[0], 10d));
        series1.getData().add(new XYChart.Data(countries[1], 20d));
        series1.getData().add(new XYChart.Data(countries[2], 15d));
        series1.getData().add(new XYChart.Data(countries[3], 15d));
        series1.getData().add(new XYChart.Data(countries[4], 10d));
        } else {
            xAxis = new NumberAxis(0, 90, 10);
            ac = new AreaChart<Number,Number>(xAxis,yAxis);
            // add starting data
        series1.getData().add(new XYChart.Data(10d, 10d));
        series1.getData().add(new XYChart.Data(25d, 20d));
        series1.getData().add(new XYChart.Data(30d, 15d));
        series1.getData().add(new XYChart.Data(50d, 15d));
        series1.getData().add(new XYChart.Data(80d, 10d));
        }

        xAxis.setLabel("X Axis");
        yAxis.setLabel("Y Axis");
        ac.setTitle("HelloAreaChart");

        return ac;
    }

    private String getSeriesLineFromPlot() {
        for (Node n : XYChartShim.getPlotChildren(ac)) {
            if (n instanceof Group) {
                for (Node gn : ((Group)n).getChildren()) {
                    if (gn instanceof Path && "chart-series-area-line".equals(gn.getStyleClass().get(0))) {
                        Path line = (Path)gn;
                        return computeSVGPath(line);
                    }
                }
            }
        }
        return "";
    }

    @Test @Ignore
    public void testDataItemRemove() {
        startApp();
        ac.getData().addAll(series1);
        pulse();
        if (!ac.getData().isEmpty()) {
            series1.getData().remove(0);
            pulse();
            assertEquals("L247.0 171.0 L412.0 171.0 L658.0 284.0 ", getSeriesLineFromPlot());
        }
    }

    @Test
    public void testAreaChartWithCategoryAxis() {
        useCategoryAxis = true;
        startApp();
        useCategoryAxis = false;
    }

    @Override
    void checkSeriesStyleClasses(XYChart.Series<?, ?> series,
            int seriesIndex, int colorIndex) {
        Group group = (Group) series.getNode();
        Node fillPath = group.getChildren().get(0);
        Node seriesLine = group.getChildren().get(1);
        checkStyleClass(fillPath, "series"+seriesIndex, "default-color"+colorIndex);
        checkStyleClass(seriesLine, "series"+seriesIndex, "default-color"+colorIndex);
    }

    @Override
    void checkDataStyleClasses(XYChart.Data<?, ?> data,
            int seriesIndex, int dataIndex, int colorIndex) {
        Node symbol = data.getNode();
        checkStyleClass(symbol, "series"+seriesIndex, "data"+dataIndex, "default-color"+colorIndex);
    }

    @Test
    public void testSeriesRemoveAnimatedStyleClasses() {
        startApp();
        //ac.setCreateSymbols(false);
        int nodesPerSeries = 4; // 3 symbols + 1 path
        checkSeriesRemoveAnimatedStyleClasses(ac, nodesPerSeries, 400);
    }

    @Test public void testCreateSymbols() {
         startApp();
         ac.getData().clear();
         ac.setCreateSymbols(false);
         pulse();
         ac.getData().addAll(series1);
         pulse();
         assertEquals(0, countSymbols(ac, "chart-area-symbol"));

         ac.getData().clear();
         ac.setCreateSymbols(true);
         pulse();
         ac.getData().addAll(series1);
         pulse();
         assertEquals(5, countSymbols(ac, "chart-area-symbol"));
     }

    @Test public void testPathInsideXAndInsideYBounds() {
        startApp();
        series1.getData().add(new XYChart.Data<>(85d, 15d)); // upper bound is 90,30
        ac.getData().addAll(series1);
        pulse();

        assertArrayEquals(convertSeriesDataToPoint2D(series1).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXLowerBoundsWithDuplicateXAndHigherY() {
        startApp();
        series1.getData().add(new XYChart.Data<>(-10d, 20d)); // lower bound is 0
        series1.getData().add(new XYChart.Data<>(-10d, 50d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(-10d, 50d),
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXUpperBoundsWithDuplicateXAndHigherY() {
        startApp();
        series1.getData().add(new XYChart.Data<>(100d, 20d)); // upper bound is 90
        series1.getData().add(new XYChart.Data<>(100d, 50d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d),
                new XYChart.Data<>(100d, 20d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXLowerBoundsWithDuplicateXAndLowerY() {
        startApp();
        series1.getData().add(new XYChart.Data<>(-10d, 20d)); // lower bound is 0
        series1.getData().add(new XYChart.Data<>(-10d, 15d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(-10d, 15d),
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXUpperBoundsWithDuplicateXAndLowerY() {
        startApp();
        series1.getData().add(new XYChart.Data<>(100d, 20d)); // upper bound is 90
        series1.getData().add(new XYChart.Data<>(100d, 15d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d),
                new XYChart.Data<>(100d, 20d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideYLowerBoundsWithDuplicateYAndLowerX() {
        startApp();
        series1.getData().add(new XYChart.Data<>(85d, -10d)); // y-axis lower bound is 0
        series1.getData().add(new XYChart.Data<>(70d, -10d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                // Sorting policy in AreaChart is defaulted to X_AXIS. See AreaChart#makePaths
                new XYChart.Data<>(70d, -10d),
                new XYChart.Data<>(80d, 10d),
                new XYChart.Data<>(85d, -10d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideYUpperBoundsWithDuplicateYAndLowerX() {
        startApp();
        series1.getData().add(new XYChart.Data<>(85d, 40d));  // y-axis upper bound is 30
        series1.getData().add(new XYChart.Data<>(70d, 40d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                // Sorting policy in AreaChart is defaulted to X_AXIS. See AreaChart#makePaths
                new XYChart.Data<>(70d, 40d),
                new XYChart.Data<>(80d, 10d),
                new XYChart.Data<>(85d, 40d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideYLowerBoundsWithDuplicateYAndHigherX() {
        startApp();
        series1.getData().add(new XYChart.Data<>(70d, -10d)); // lower bound is 30
        series1.getData().add(new XYChart.Data<>(85d, -10d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(70d, -10d),
                new XYChart.Data<>(80d, 10d),
                new XYChart.Data<>(85d, -10d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideYUpperBoundsWithDuplicateYAndHigherX() {
        startApp();
        series1.getData().add(new XYChart.Data<>(70d, 32d)); // upper bound is 30
        series1.getData().add(new XYChart.Data<>(85d, 32d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(70d, 32d),
                new XYChart.Data<>(80d, 10d),
                new XYChart.Data<>(85d, 32d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXAndYLowerBoundsWithDuplicateXAndHigherY() {
        startApp();
        series1.getData().add(new XYChart.Data<>(-10d, -40d)); // lower bound is 0,0
        series1.getData().add(new XYChart.Data<>(-10d, -30d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(-10d, -30d),
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXAndYUpperBoundsWithDuplicateXAndHigherY() {
        startApp();
        series1.getData().add(new XYChart.Data<>(95d, 35d)); // upper bound is 90,30
        series1.getData().add(new XYChart.Data<>(95d, 40d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d),
                new XYChart.Data<>(95d, 35d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXAndYLowerBoundsWithDuplicateXAndLowerY() {
        startApp();
        series1.getData().add(new XYChart.Data<>(-10d, -30d)); // lower bound is 0,0
        series1.getData().add(new XYChart.Data<>(-10d, -40d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(-10d, -40d),
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXAndYUpperBoundsWithDuplicateXAndLowerY() {
        startApp();
        series1.getData().add(new XYChart.Data<>(95d, 40d)); // upper bound is 90,30
        series1.getData().add(new XYChart.Data<>(95d, 35d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d),
                new XYChart.Data<>(95d, 40d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXAndYLowerBoundsWithDuplicateYAndHigherX() {
        startApp();
        series1.getData().add(new XYChart.Data<>(-20d, -30d)); // lower bound is 0,0
        series1.getData().add(new XYChart.Data<>(-10d, -30d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(-10d, -30d),
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXAndYUpperBoundsWithDuplicateYAndHigherX() {
        startApp();
        series1.getData().add(new XYChart.Data<>(95d, 32d)); // upper bound is 90,30
        series1.getData().add(new XYChart.Data<>(100d, 32d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d),
                new XYChart.Data<>(95d, 32d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXAndYLowerBoundsWithDuplicateYAndLowerX() {
        startApp();
        series1.getData().add(new XYChart.Data<>(-10d, -30d)); // lower bound is 0,0
        series1.getData().add(new XYChart.Data<>(-20d, -30d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(-10d, -30d),
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    @Test public void testPathOutsideXAndYUpperBoundsWithDuplicateYAndLowerX() {
        startApp();
        series1.getData().add(new XYChart.Data<>(100d, 40d)); // upper bound is 90,30
        series1.getData().add(new XYChart.Data<>(95d, 40d));
        ac.getData().addAll(series1);
        pulse();

        XYChart.Series<Number, Number> expectedSeries = new XYChart.Series<>();
        expectedSeries.getData().addAll(
                new XYChart.Data<>(10d, 10d),
                new XYChart.Data<>(25d, 20d),
                new XYChart.Data<>(30d, 15d),
                new XYChart.Data<>(50d, 15d),
                new XYChart.Data<>(80d, 10d),
                new XYChart.Data<>(95d, 40d)
        );

        assertArrayEquals(convertSeriesDataToPoint2D(expectedSeries).toArray(), findDataPointsFromPathLine(ac).toArray());
    }

    private List<Point2D> convertSeriesDataToPoint2D(XYChart.Series<Number, Number> series) {
        return series.getData().stream()
                .map(data -> new Point2D(data.getXValue().doubleValue(), data.getYValue().doubleValue()))
                .collect(Collectors.toList());
    }

    private List<Point2D> findDataPointsFromPathLine(AreaChart<Number, Number> areaChart) {
        final NumberAxis xAxis = (NumberAxis) areaChart.getXAxis();
        final NumberAxis yAxis = (NumberAxis) areaChart.getYAxis();

        final ObservableList<Node> children = ((Group) areaChart.getData().get(0).getNode()).getChildren();
        Path fillPath = (Path) children.get(0);
        ObservableList<PathElement> fillElements = fillPath.getElements();

        List<Point2D> data = fillElements.stream()
                .filter(pathElement -> pathElement instanceof LineTo)
                .map(pathElement -> (LineTo) pathElement)
                .map(lineTo -> new Point2D(
                        xAxis.getValueForDisplay(lineTo.getX()).doubleValue(),
                        yAxis.getValueForDisplay(lineTo.getY()).doubleValue())
                )
                .collect(Collectors.toList());
        // Due to fillPath, one additional LineTo element is added to close the loop
        return data.subList(0, data.size() - 1);
    }
}
