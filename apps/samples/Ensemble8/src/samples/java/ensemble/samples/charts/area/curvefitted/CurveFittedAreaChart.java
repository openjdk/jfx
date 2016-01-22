 /*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.samples.charts.area.curvefitted;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.util.Pair;

public class CurveFittedAreaChart extends AreaChart<Number, Number> {

    public CurveFittedAreaChart(NumberAxis xAxis, NumberAxis yAxis) {
        super(xAxis, yAxis);
    }
    @Override protected void layoutPlotChildren() {
        super.layoutPlotChildren();
        for (int seriesIndex = 0; seriesIndex < getDataSize(); seriesIndex++) {
            final XYChart.Series<Number, Number> series = getData().get(seriesIndex);
            final Path seriesLine = (Path) ((Group) series.getNode()).getChildren().get(1);
            final Path fillPath = (Path) ((Group) series.getNode()).getChildren().get(0);
            smooth(seriesLine.getElements(), fillPath.getElements());
        }
    }

    private int getDataSize() {
        final ObservableList<XYChart.Series<Number, Number>> data = getData();
        return (data != null) ? data.size() : 0;
    }

    private static void smooth(ObservableList<PathElement> strokeElements, ObservableList<PathElement> fillElements) {
        // as we do not have direct access to the data, first recreate the list of all the data points we have
        final Point2D[] dataPoints = new Point2D[strokeElements.size()];
        for (int i = 0; i < strokeElements.size(); i++) {
            final PathElement element = strokeElements.get(i);
            if (element instanceof MoveTo) {
                final MoveTo move = (MoveTo) element;
                dataPoints[i] = new Point2D(move.getX(), move.getY());
            } else if (element instanceof LineTo) {
                final LineTo line = (LineTo) element;
                final double x = line.getX(), y = line.getY();
                dataPoints[i] = new Point2D(x, y);
            }
        }
        // next we need to know the zero Y value
        final double zeroY = ((MoveTo) fillElements.get(0)).getY();
        // now clear and rebuild elements
        strokeElements.clear();
        fillElements.clear();
        Pair<Point2D[], Point2D[]> result = calcCurveControlPoints(dataPoints);
        Point2D[] firstControlPoints = result.getKey();
        Point2D[] secondControlPoints = result.getValue();
        // start both paths
        strokeElements.add(new MoveTo(dataPoints[0].getX(), dataPoints[0].getY()));
        fillElements.add(new MoveTo(dataPoints[0].getX(), zeroY));
        fillElements.add(new LineTo(dataPoints[0].getX(), dataPoints[0].getY()));
        // add curves
        for (int i = 1; i < dataPoints.length; i++) {
            final int ci = i - 1;
            strokeElements.add(new CubicCurveTo(
                    firstControlPoints[ci].getX(), firstControlPoints[ci].getY(),
                    secondControlPoints[ci].getX(), secondControlPoints[ci].getY(),
                    dataPoints[i].getX(), dataPoints[i].getY()));
            fillElements.add(new CubicCurveTo(
                    firstControlPoints[ci].getX(), firstControlPoints[ci].getY(),
                    secondControlPoints[ci].getX(), secondControlPoints[ci].getY(),
                    dataPoints[i].getX(), dataPoints[i].getY()));
        }
        // end the paths
        fillElements.add(new LineTo(dataPoints[dataPoints.length - 1].getX(), zeroY));
        fillElements.add(new ClosePath());
    }

    /**
     * Calculate open-ended Bezier Spline Control Points.
     *
     * @param dataPoints Input data Bezier spline points.
     * @return The spline points
     */
    public static Pair<Point2D[], Point2D[]> calcCurveControlPoints(Point2D[] dataPoints) {
        Point2D[] firstControlPoints;
        Point2D[] secondControlPoints;
        int n = dataPoints.length - 1;
        if (n == 1) { // Special case: Bezier curve should be a straight line.
            firstControlPoints = new Point2D[1];
            // 3P1 = 2P0 + P3
            firstControlPoints[0] = new Point2D(
                    (2 * dataPoints[0].getX() + dataPoints[1].getX()) / 3,
                    (2 * dataPoints[0].getY() + dataPoints[1].getY()) / 3);

            secondControlPoints = new Point2D[1];
            // P2 = 2P1 – P0
            secondControlPoints[0] = new Point2D(
                    2 * firstControlPoints[0].getX() - dataPoints[0].getX(),
                    2 * firstControlPoints[0].getY() - dataPoints[0].getY());
            return new Pair<Point2D[], Point2D[]>(firstControlPoints, secondControlPoints);
        }

        // Calculate first Bezier control points
        // Right hand side vector
        double[] rhs = new double[n];

        // Set right hand side X values
        for (int i = 1; i < n - 1; ++i) {
            rhs[i] = 4 * dataPoints[i].getX() + 2 * dataPoints[i + 1].getX();
        }
        rhs[0] = dataPoints[0].getX() + 2 * dataPoints[1].getX();
        rhs[n - 1] = (8 * dataPoints[n - 1].getX() + dataPoints[n].getX()) / 2.0;
        // Get first control points X-values
        double[] x = GetFirstControlPoints(rhs);

        // Set right hand side Y values
        for (int i = 1; i < n - 1; ++i) {
            rhs[i] = 4 * dataPoints[i].getY() + 2 * dataPoints[i + 1].getY();
        }
        rhs[0] = dataPoints[0].getY() + 2 * dataPoints[1].getY();
        rhs[n - 1] = (8 * dataPoints[n - 1].getY() + dataPoints[n].getY()) / 2.0;
        // Get first control points Y-values
        double[] y = GetFirstControlPoints(rhs);

        // Fill output arrays.
        firstControlPoints = new Point2D[n];
        secondControlPoints = new Point2D[n];
        for (int i = 0; i < n; ++i) {
            // First control point
            firstControlPoints[i] = new Point2D(x[i], y[i]);
            // Second control point
            if (i < n - 1) {
                secondControlPoints[i] = new Point2D(2 * dataPoints[i + 1].getX() - x[i + 1], 2
                        * dataPoints[i + 1].getY() - y[i + 1]);
            } else {
                secondControlPoints[i] = new Point2D((dataPoints[n].getX() + x[n - 1]) / 2,
                        (dataPoints[n].getY() + y[n - 1]) / 2);
            }
        }
        return new Pair<Point2D[], Point2D[]>(firstControlPoints, secondControlPoints);
    }

    /**
     * Solves a tridiagonal system for one of coordinates (x or y) of first
     * Bezier control points.
     *
     * @param rhs Right hand side vector.
     * @return Solution vector.
     */
    private static double[] GetFirstControlPoints(double[] rhs) {
        int n = rhs.length;
        double[] x = new double[n]; // Solution vector.
        double[] tmp = new double[n]; // Temp workspace.
        double b = 2.0;
        x[0] = rhs[0] / b;
        for (int i = 1; i < n; i++) {// Decomposition and forward substitution.
            tmp[i] = 1 / b;
            b = (i < n - 1 ? 4.0 : 3.5) - tmp[i];
            x[i] = (rhs[i] - x[i - 1]) / b;
        }
        for (int i = 1; i < n; i++) {
            x[n - i - 1] -= tmp[n - i] * x[n - i]; // Backsubstitution.
        }
        return x;
    }
}
