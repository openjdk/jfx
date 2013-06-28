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


public class StackedAreaChartTest extends XYChartTestBase {
    
    private Scene scene;
    private StubToolkit toolkit;
    private Stage stage;
    StackedAreaChart<Number,Number> ac;
    final XYChart.Series<Number, Number> series1 = new XYChart.Series<Number, Number>();
    boolean useCategoryAxis = false;
    final String[] countries = {"USA", "Italy", "France", "China", "India"};
    protected Chart createChart() {
        final NumberAxis yAxis = new NumberAxis();
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
            xAxis = new NumberAxis();
            ac = new StackedAreaChart<Number,Number>(xAxis,yAxis);
            // add starting data
        series1.getData().add(new XYChart.Data(10d, 10d));
        series1.getData().add(new XYChart.Data(25d, 20d));
        series1.getData().add(new XYChart.Data(30d, 15d));
        series1.getData().add(new XYChart.Data(50d, 15d));
        series1.getData().add(new XYChart.Data(80d, 10d));
        }
        
        xAxis.setLabel("X Axis");
        yAxis.setLabel("Y Axis");
        ac.setTitle("HelloStackedAreaChart");
        
        return ac;
    }
    
    private StringBuffer getSeriesLineFromPlot() {
        ObservableList<Node> childrenList = ac.getPlotChildren();
        StringBuffer sb = new StringBuffer();
        for (Node n : childrenList) {
            if (n instanceof Group) {
                for (Node gn : ((Group)n).getChildren()) {
                    if (gn instanceof Path && "chart-series-area-line".equals(gn.getStyleClass().get(0))) {
                        Path line = (Path)gn;
                        sb = computeSVGPath(line);
                        return sb;
                    }
                }
            }
        }
        return sb;
    }
    
    @Test 
    public void testSeriesAdd() {
        startApp();
        ac.getData().addAll(series1);
        pulse();
        StringBuffer sb = getSeriesLineFromPlot();
//        assertEquals("L220.0 59.0 L264.0 175.0 L440.0 175.0 L704.0 291.0 ", sb.toString());
        assertEquals("L219.0 58.0 L263.0 173.0 L438.0 173.0 L700.0 289.0 ", sb.toString());
    
    }
    
    @Test
    public void testSeriesRemove() {
        startApp();
        ac.getData().addAll(series1);
        pulse();
        if (!ac.getData().isEmpty()) {
            ac.getData().remove(0);
            pulse();
            StringBuffer sb = getSeriesLineFromPlot();
            assertEquals(sb.toString(), "");
        }
    }
    
    @Test @Ignore
    public void testDataItemRemove() {
        startApp();
        ac.getData().addAll(series1);
        pulse();
        if (!ac.getData().isEmpty()) {
            series1.getData().remove(0);
            pulse();
            StringBuffer sb = getSeriesLineFromPlot();
            assertEquals(sb.toString(), "L247.0 171.0 L412.0 171.0 L658.0 284.0 ");
        }
    }
    
    @Test @Ignore
    public void testDataItemAdd() {
        startApp();
        ac.getData().addAll(series1);
        pulse();
        if (!ac.getData().isEmpty()) {
            series1.getData().add(new XYChart.Data(40d,10d));
            pulse();
            StringBuffer sb = getSeriesLineFromPlot();
            assertEquals(sb.toString(), "L206.0 57.0 L247.0 171.0 L329.0 284.0 L412.0 171.0 L658.0 284.0 ");
        }
    }
    
    @Test @Ignore
    public void testDataItemInsert() {
        startApp();
        ac.getData().addAll(series1);
        pulse();
        if (!ac.getData().isEmpty()) {
            series1.getData().add(2, new XYChart.Data(40d,10d));
            pulse();
            StringBuffer sb = getSeriesLineFromPlot();
            assertEquals(sb.toString(), "L206.0 57.0 L247.0 171.0 L329.0 284.0 L412.0 171.0 L658.0 284.0 ");
        }
    }
    
    @Test @Ignore
    public void testDataItemChange() {
        startApp();
        ac.getData().addAll(series1);
        pulse();
        if (!ac.getData().isEmpty()) {
            XYChart.Data<Number,Number> d = series1.getData().get(2);
            d.setXValue(40d);
            d.setYValue(30d);
            pulse();
            StringBuffer sb = getSeriesLineFromPlot();
            assertEquals(sb.toString(), "L206.0 197.0 L329.0 40.0 L412.0 276.0 L658.0 354.0 ");
        }
    }
    
    @Test 
    public void testStackedAreaChartWithCategoryAxis() {
        useCategoryAxis = true;
        startApp();
        useCategoryAxis = false;
    }

    @Test public void testCreateSymbols() {
         startApp();
         ac.setCreateSymbols(false);
         pulse();
         ac.getData().addAll(series1);
         pulse();
         assertEquals(0, countSymbols(ac, "chart-area-symbol"));
         
         ac.getData().clear();
         pulse();
         ac.setCreateSymbols(true);
         pulse();
         ac.getData().addAll(series1);
         assertEquals(5, countSymbols(ac, "chart-area-symbol"));
     }
}
