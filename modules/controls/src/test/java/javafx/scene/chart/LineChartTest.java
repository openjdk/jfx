/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * @author paru
 */
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import javafx.collections.*;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.stage.Stage;
import javafx.scene.shape.*;

import org.junit.Ignore;


public class LineChartTest extends XYChartTestBase {

    private Scene scene;
    private StubToolkit toolkit;
    private Stage stage;
    LineChart<Number,Number> lineChart;
    final XYChart.Series<Number, Number> series1 = new XYChart.Series<Number, Number>();
    
    @Override protected Chart createChart() {
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        lineChart = new LineChart<Number,Number>(xAxis,yAxis);
        xAxis.setLabel("X Axis");
        yAxis.setLabel("Y Axis");
        lineChart.setTitle("HelloLineChart");
        // add starting data
        series1.getData().add(new XYChart.Data(10d, 10d));
        series1.getData().add(new XYChart.Data(25d, 20d));
        series1.getData().add(new XYChart.Data(30d, 15d));
        series1.getData().add(new XYChart.Data(50d, 15d));
        series1.getData().add(new XYChart.Data(80d, 10d));
        return lineChart;
    }
    
    private StringBuffer getSeriesLineFromPlot() {
        ObservableList<Node> childrenList = lineChart.getPlotChildren();
        StringBuffer sb = new StringBuffer();
        for (Node n : childrenList) {
            if (n instanceof Path && "chart-series-line".equals(n.getStyleClass().get(0))) {
                Path line = (Path)n;
                sb = computeSVGPath(line);
                return sb;
            }
        }
        return sb;
    }
    
     @Test
    public void testSeriesRemoveWithCreateSymbolsFalse() {
        startApp();
        lineChart.getData().addAll(series1);
        pulse();
        lineChart.setCreateSymbols(false);
        System.out.println("Line Path = "+getSeriesLineFromPlot());
        if (!lineChart.getData().isEmpty()) {
            lineChart.getData().remove(0);
            pulse();
            StringBuffer sb = getSeriesLineFromPlot();
            assertEquals(sb.toString(), "");
        }
    }
        
     @Test public void testCreateSymbols() {
         startApp();
         lineChart.setCreateSymbols(false);
         pulse();
         lineChart.getData().addAll(series1);
         pulse();
         assertEquals(0, countSymbols(lineChart, "chart-line-symbol"));
         
         lineChart.getData().clear();
         pulse();
         lineChart.setCreateSymbols(true);
         pulse();
         lineChart.getData().addAll(series1);
         assertEquals(5, countSymbols(lineChart, "chart-line-symbol"));
     }
     
    @Test
    public void testDataItemAdd() {
        startApp();
        lineChart.getData().addAll(series1);
        pulse();
        series1.getData().add(new XYChart.Data(60d, 30d));
        pulse();
        // 5 stackpane nodes and 1 path node + new stackpane for data added
        assertEquals(7, lineChart.getPlotChildren().size());
    }

     @Test @Ignore
     // Ignored because the animation's Timeline doesn't run. It used to be that the item was added before the
     // animation was run. Now the item is added as the onFinished handler of the first KeyFrame. Since the
     // Timeline doesn't run in the context of the unit test, this test fails. In fact, this test never really
     // achieved its purpose.
    public void testDataItemAddWithAnimation() {
        startApp();
        lineChart.setAnimated(true);
        lineChart.getData().addAll(series1);
        pulse();
        series1.getData().add(new XYChart.Data(60d, 30d));
        pulse();
        // 5 stackpane nodes and 1 path node + new stackpane for data added
        assertEquals(7, lineChart.getPlotChildren().size());
    }
      
    @Test
    public void testDataItemRemove() {
        startApp();
        lineChart.getData().addAll(series1);
        pulse();
        if (!lineChart.getData().isEmpty()) {
            series1.getData().remove(0);
            pulse();
            // 4 stackpane nodes and one path node
            assertEquals(5, lineChart.getPlotChildren().size());
        }
    }
     
     @Test
    public void testSeriesAddWithAnimation() {
        startApp();
        lineChart.setAnimated(true);
        final XYChart.Series<Number, Number> series2 = new XYChart.Series<Number, Number>();
        series1.getData().add(new XYChart.Data(15d, 40d));
        series1.getData().add(new XYChart.Data(25d, 10d));
        series1.getData().add(new XYChart.Data(40d, 35d));
        lineChart.getData().addAll(series1);
        pulse();
        assertEquals(true, lineChart.getAnimated());
    }
}
