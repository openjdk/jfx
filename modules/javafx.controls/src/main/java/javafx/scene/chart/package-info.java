/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <P>The JavaFX User Interface provides a set of chart components that
 * are a very convenient way for data visualization. Application
 * developers can make use of these off-the-rack graphical charts
 * provided by the JavaFX runtime, to visualize a wide variety of data.</P>
 * <P>Commom types of charts such as {@link javafx.scene.chart.BarChart
 * Bar}, {@link javafx.scene.chart.LineChart Line}, {@link
 * javafx.scene.chart.AreaChart Area}, {@link
 * javafx.scene.chart.PieChart Pie}, {@link
 * javafx.scene.chart.ScatterChart Scatter} and {@link
 * javafx.scene.chart.BubbleChart Bubble} charts are provided. These
 * charts are easy to create and are customizable. JavaFX Charts API is
 * a visual centric API rather than model centric.
 * </P>
 * <P>JavaFX charts supports animation of chart components as well as
 * auto ranging of chart Axis.  In addition, as with other JavaFX UI
 * controls, chart visual components can be styled via CSS. Thus, there
 * are several public visual properties that can be styled via CSS. An
 * example is provided later in the document.
 * </P>
 * <P>Below is a table listing the existing Chart types and a brief
 * summary of their intended use.</P>
 * <TABLE>
 * <CAPTION>Table of Chart Types</CAPTION>
 *     <TR>
 *         <TH scope="col">
 *             <P>Chart</P>
 *         </TH>
 *         <TH scope="col">
 *             <P>Summary</P>
 *         </TH>
 *     </TR>
 *     <TR>
 *         <TH scope="row">
 *             <P>{@link javafx.scene.chart.LineChart}</P>
 *         </TH>
 *         <TD>
 *             <P>Plots line between the data points in a series. Used usually to
 *             view data trends over time.</P>
 *         </TD>
 *     </TR>
 *     <TR>
 *         <TH scope="row">
 *             <P>{@link javafx.scene.chart.AreaChart}</P>
 *         </TH>
 *         <TD>
 *             <P>Plots the area between the line that connects the data points
 *             and the axis. Good for comparing cumulated totals over time.</P>
 *         </TD>
 *     </TR>
 *     <TR>
 *         <TH scope="row">
 *             <P>{@link javafx.scene.chart.BarChart}</P>
 *         </TH>
 *         <TD>
 *             <P>Plots rectangular bars with heights indicating data values they
 *             represent, and corresponding to the categories they belongs to.
 *             Used for displaying discontinuous / discrete data</P>
 *         </TD>
 *     </TR>
 *     <TR>
 *         <TH scope="row">
 *             <P>{@link javafx.scene.chart.PieChart}</P>
 *         </TH>
 *         <TD>
 *             <P>Plots circular chart divided into segments with each segment
 *             representing a value as a proportion of the total. It looks like a
 *             Pie and hence the name
 *             </P>
 *         </TD>
 *     </TR>
 *     <TR>
 *         <TH scope="row">
 *             <P>{@link javafx.scene.chart.BubbleChart}</P>
 *         </TH>
 *         <TD>
 *             <P>Plots bubbles for data points in a series. Each plotted entity
 *             depicts three parameters in a 2D chart and hence a unique chart
 *             type.</P>
 *         </TD>
 *     </TR>
 *     <TR>
 *         <TH scope="row">
 *             <P>{@link javafx.scene.chart.ScatterChart}</P>
 *         </TH>
 *         <TD>
 *             <P>Plots symbols for the data points in a series. This type of
 *             chart is useful in viewing distribution of data and its
 *             corelation, if there is any clustering.</P>
 *         </TD>
 *     </TR>
 * </TABLE>
 * <P>The {@link javafx.scene.chart.Chart} is the baseclass for all
 * charts. It is responsible for drawing the background, frame, title
 * and legend. It can be extended to create custom chart types. The
 * {@link javafx.scene.chart.XYChart} is the baseclass for all two axis
 * charts and it extends from Chart class. It is mostly responsible for
 * drawing the two axis and the background of the chart plot. Most
 * charts extend from XYChart class except for PieChart which extends
 * from Chart class as it is not a two axis chart.
 * </P>
 * <P>The {@link javafx.scene.chart} package includes axis classes that
 * can be used when creating two axis charts. {@link
 * javafx.scene.chart.Axis} is the abstract base class of all chart
 * axis. {@link javafx.scene.chart.CategoryAxis} plots string categories
 * where each value is a unique category along the axis. {@link
 * javafx.scene.chart.NumberAxis} plots a range of numbers with major
 * tick marks every tickUnit.
 * </P>
 * <P>For Example BarChart plots data from a sequence of {@link
 * javafx.scene.chart.XYChart.Series} objects. Each series contains
 * {@link javafx.scene.chart.XYChart.Data} objects.
 * </P>
 * <pre>{@code
 *     // add data
 *     XYChart.Series<String,Number> series1 = new XYChart.Series<String,Number>();
 *     series1.setName("Data Series 1");
 *     series1.getData().add(new XYChart.Data<String,Number>("2007", 567));
 * }</pre>
 * <P>We can define more series objects similarly. Following code
 * snippet shows how to create a BarChart with 3 categories and its X
 * and Y axis:
 * </P>
 * <pre>{@code
 *     static String[] years = {"2007", "2008", "2009"};
 *     final CategoryAxis xAxis = new CategoryAxis();
 *     final NumberAxis yAxis = new NumberAxis();
 *     final BarChart<String,Number> bc = new BarChart<String,Number>(xAxis, yAxis);
 *     xAxis.setCategories(FXCollections.<String>observableArrayList(Arrays.asList(years)));
 *     bc.getData().addAll(series1, series2, series3);
 * }</pre>
 * <P>JavaFX charts lends itself very well for real time or dynamic
 * Charting (like online stocks, web traffic etc) from live data sets.
 * Here is an example of a dynamic chart created with simulated data. A
 * {@link javafx.animation.Timeline} is used to simulate dynamic data
 * for stock price variations over time(hours).
 * </P>
 * <pre><code>
 *     {@literal private XYChart.Series<Number,Number> hourDataSeries;}
 *     private NumberAxis xAxis;
 *     private Timeline animation;
 *     private double hours = 0;
 *     private double timeInHours = 0;
 *     private double prevY = 10;
 *     private double y = 10;
 *
 *     // timeline to add new data every 60th of a second
 *     animation = new Timeline();
 *     {@literal animation.getKeyFrames().add(new KeyFrame(Duration.millis(1000 / 60), new EventHandler<ActionEvent>()} {
 *         {@literal @Override public void handle(ActionEvent actionEvent)} {
 *             // 6 minutes data per frame
 *             {@literal for(int count = 0; count < 6; count++)} {
 *                 nextTime();
 *                 plotTime();
 *             }
 *         }
 *     }));
 *     animation.setCycleCount(Animation.INDEFINITE);
 *     xAxis = new NumberAxis(0, 24, 3);
 *     final NumberAxis yAxis = new NumberAxis(0, 100, 10);
 *     {@literal final LineChart<Number,Number> lc = new LineChart<Number,Number>(xAxis, yAxis)};
 *
 *     lc.setCreateSymbols(false);
 *     lc.setAnimated(false);
 *     lc.setLegendVisible(false);
 *     lc.setTitle("ACME Company Stock");
 *
 *     xAxis.setLabel("Time");
 *     xAxis.setForceZeroInRange(false);
 *     yAxis.setLabel("Share Price");
 *     yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, "$", null));
 *
 *     {@literal hourDataSeries = new XYChart.Series<Number,Number>();}
 *     hourDataSeries.setName("Hourly Data");
 *     {@literal hourDataSeries.getData().add(new XYChart.Data<Number,Number>(timeInHours, prevY));}
 *     lc.getData().add(hourDataSeries);
 *
 *     private void nextTime() {
 *         if (minutes == 59) {
 *             hours++;
 *             minutes = 0;
 *         } else {
 *             minutes++;
 *         }
 *         timeInHours = hours + ((1d/60d) * minutes);
 *     }
 *
 *     private void plotTime() {
 *         if ((timeInHours % 1) == 0) {
 *             // change of hour
 *             double oldY = y;
 *             y = prevY - 10 + (Math.random() * 20);
 *             prevY = oldY;
 *             {@literal while (y < 10 || y > 90) y = y - 10 + (Math.random() * 20);}
 *             {@literal hourDataSeries.getData().add(new XYChart.Data<Number, Number>(timeInHours, prevY));}
 *             // after 25hours delete old data
 *             {@literal if (timeInHours > 25) hourDataSeries.getData().remove(0)};
 *             // every hour after 24 move range 1 hour
 *             {@literal if (timeInHours > 24)} {
 *                 xAxis.setLowerBound(xAxis.getLowerBound() + 1);
 *                 xAxis.setUpperBound(xAxis.getUpperBound() + 1);
 *             }
 *         }
 *     }
 * </code></pre>
 *
 * <P>The start method needs to call animation,.play() to start the
 * simulated dynamic chart.</P>
 * <P>Please refer to javafx.scene.control package documentation on CSS
 * styling. An example for styling a Chart via CSS is as follows:- to
 * set the chart content background to a certain color:</P>
 * <P>.chart-content { -fx-background-color: cyan;}</P>
 * <P>Line Chart line color can be styled as follows:-</P>
 * <P>.chart-series-line { -fx-stroke: green; -fx-stroke-width: 4px;}</P>
 * <P STYLE="margin-bottom: 0in"><BR>
 * </P>
 */
package javafx.scene.chart;
