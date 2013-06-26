/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
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
package modena;

import java.util.Random;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;

/**
 * Helper class for creating charts for testing
 */
public class SamplePageChartHelper {
    
    static Node createAreaChart(Boolean stacked) {
        NumberAxis xAxis = new NumberAxis("X Values", 1.0d, 9.0d, 2.0d);
        xAxis.setTickLength(12.0f);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Y Values");
        ObservableList<AreaChart.Series> areaChartData = FXCollections.observableArrayList(
                new AreaChart.Series("Series 1",FXCollections.observableArrayList(
                    new AreaChart.Data(0,4),
                    new AreaChart.Data(2,5),
                    new AreaChart.Data(4,4),
                    new AreaChart.Data(6,2),
                    new AreaChart.Data(8,6),
                    new AreaChart.Data(10,8)
                )),
                new AreaChart.Series("Series 2", FXCollections.observableArrayList(
                    new AreaChart.Data(0,8),
                    new AreaChart.Data(2,2),
                    new AreaChart.Data(4,9),
                    new AreaChart.Data(6,7),
                    new AreaChart.Data(8,5),
                    new AreaChart.Data(10,7)
                )),
                new AreaChart.Series("Series 3", FXCollections.observableArrayList(
                    new AreaChart.Data(0,2),
                    new AreaChart.Data(2,5),
                    new AreaChart.Data(4,8),
                    new AreaChart.Data(6,6),
                    new AreaChart.Data(8,9),
                    new AreaChart.Data(10,7)
                ))
        );
        Node areaChart;
        if (stacked) {
            areaChart = new StackedAreaChart(xAxis, yAxis, areaChartData);
        } else {
            areaChart = new AreaChart(xAxis, yAxis, areaChartData);
        }
        return areaChart;
    }
    
    static Node createStackedBarChart(boolean horizontal, boolean simple) {
        return createBarChart(horizontal, simple, true);
    }
    
    static Node createBarChart(boolean horizontal, boolean simple) {
        return createBarChart(horizontal, simple, false);
    }
    
    static Node createBarChart(boolean horizontal, boolean simple, boolean stacked) {
        final Random RANDOM = new Random(29782198273l);
        String[] years = {"2001","2002","2003"};
        String[] series;
        if (simple) {
            series = new String[]{"A","B","C"};
        } else {
            series = new String[]{"A","B","C","D","E","F","G","H"};
        }
        CategoryAxis xAxis = new CategoryAxis(FXCollections.<String>observableArrayList(years));
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Units Sold");
        ObservableList<BarChart.Series> barChartData = FXCollections.observableArrayList();
        final double negative = stacked ? 0 : -500;
        for (int s=0; s<series.length; s++) {
            ObservableList<BarChart.Data> sd = FXCollections.observableArrayList();
            for(int y=0; y<years.length; y++) {
                if (horizontal) {
                    sd.add(new BarChart.Data(negative+(2000*RANDOM.nextDouble()), years[y]));
                } else {
                    sd.add(new BarChart.Data(years[y], negative+(2000*RANDOM.nextDouble())));
                }
            }
            barChartData.add(new BarChart.Series(series[s],sd));
        }
        Node barChart;
        if (stacked) {
            if (horizontal) {
                barChart = new StackedBarChart(yAxis, xAxis, barChartData);
            } else {
                barChart = new StackedBarChart(xAxis, yAxis, barChartData);
            }
        } else {
            if (horizontal) {
                barChart = new BarChart(yAxis, xAxis, barChartData);
            } else {
                barChart = new BarChart(xAxis, yAxis, barChartData);
            }
        }
        return barChart;
    }
    
    static Node createBubbleChart(boolean useRightTopAxis) {
        final Random RANDOM = new Random(29782198273l);
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Product");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number Bought/Sold");
        if (useRightTopAxis) {
            xAxis.setSide(Side.TOP);
            yAxis.setSide(Side.RIGHT);
        }
        ObservableList<BubbleChart.Series> bubbleChartData = FXCollections.observableArrayList();
        for (int s=0; s<8; s++) {
            ObservableList<BubbleChart.Data> seriesData = FXCollections.<BubbleChart.Data>observableArrayList();
            for (int d=0; d<(8*(RANDOM.nextDouble()*10)); d++) {
                seriesData.add(new XYChart.Data(100*RANDOM.nextDouble(), -50+(150*RANDOM.nextDouble()), 5+(10*RANDOM.nextDouble()) ));
            }
            bubbleChartData.add(new ScatterChart.Series("Product "+(s+1),seriesData));
        }
        return new BubbleChart(xAxis, yAxis, bubbleChartData);
    }
    
    static Node createLineChart() {
        NumberAxis xAxis = new NumberAxis("Values for X-Axis", 0, 3, 1);
        NumberAxis yAxis = new NumberAxis("Values for Y-Axis", 0, 3, 1);
        ObservableList<XYChart.Series<Double,Double>> lineChartData = FXCollections.observableArrayList(
            new LineChart.Series<Double,Double>("Series 1", FXCollections.observableArrayList(
                new XYChart.Data<Double,Double>(0.0, 1.0),
                new XYChart.Data<Double,Double>(1.2, 1.4),
                new XYChart.Data<Double,Double>(2.2, 1.9),
                new XYChart.Data<Double,Double>(2.7, 2.3),
                new XYChart.Data<Double,Double>(2.9, 0.5)
            )),
            new LineChart.Series<Double,Double>("Series 2", FXCollections.observableArrayList(
                new XYChart.Data<Double,Double>(0.0, 1.6),
                new XYChart.Data<Double,Double>(0.8, 0.4),
                new XYChart.Data<Double,Double>(1.4, 2.9),
                new XYChart.Data<Double,Double>(2.1, 1.3),
                new XYChart.Data<Double,Double>(2.6, 0.9)
            ))
        );
        return new LineChart(xAxis, yAxis, lineChartData);
    }
    
    static Node createPieChart() {
         ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
             new PieChart.Data("Sun", 20),
             new PieChart.Data("IBM", 12),
             new PieChart.Data("HP", 25),
             new PieChart.Data("Dell", 22),
             new PieChart.Data("Apple", 30)
         );
        return new PieChart(pieChartData);
    }
    
    static Node createScatterChart() {
        final Random RANDOM = new Random(29782198273l);
        NumberAxis xAxis = new NumberAxis("X-Axis", 0, 8, 1);
        NumberAxis yAxis = new NumberAxis("Y-Axis", -5, 5, 1);
        ObservableList<XYChart.Series> data = FXCollections.observableArrayList();
        for (int s=0; s<8; s++) {
            ObservableList<ScatterChart.Data> seriesData = FXCollections.<ScatterChart.Data>observableArrayList();
            for (int d=0; d<(8*(RANDOM.nextDouble()*10)); d++) {
                seriesData.add(new XYChart.Data(8*RANDOM.nextDouble(), -5+(10*RANDOM.nextDouble())));
            }
            data.add(new ScatterChart.Series("Product "+(s+1),seriesData));
        }
        return new ScatterChart(xAxis, yAxis, data);
    }
}
