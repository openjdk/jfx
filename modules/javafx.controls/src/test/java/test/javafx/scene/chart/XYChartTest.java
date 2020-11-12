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

package test.javafx.scene.chart;


import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import org.junit.Test;
import javafx.collections.*;
import javafx.scene.chart.Axis.TickMark;
import javafx.css.ParsedValue;
import javafx.css.CssMetaData;
import javafx.css.StyleableProperty;
import javafx.css.CssParserShim;
import javafx.scene.Node;
import javafx.scene.ParentShim;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.chart.AxisShim;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.ChartShim;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils;

public class XYChartTest extends ChartTestBase {

    NumberAxis yaxis;
    AreaChart<String, Number> areachart;

    @Override
    protected Chart createChart() {
        yaxis = new NumberAxis();
        areachart = new AreaChart<>(new CategoryAxis(), yaxis);
        return areachart;
    }

    @Test
    public void testTickMarksToString() {
        startApp();
        pulse();
        yaxis.getTickMarks().toString();
        //System.out.println(" --- "+yaxis.getTickMarks().toString());
    }

    // RT-22166
    @Test public void testTickLabelFont() {
        startApp();
        Font f = yaxis.getTickLabelFont();
        // default caspian value for font size = 10
        assertEquals(10, new Double(f.getSize()).intValue());
        assertEquals(10, new Double(AxisShim.get_measure(yaxis).getFont().getSize()).intValue());

        // set tick label font via css and test if ticklabelfont, measure and tick textnode follow.
        ParsedValue pv = new CssParserShim().parseExpr("-fx-tick-label-font","0.916667em System");
        Object val = pv.convert(null);
        CssMetaData prop = ((StyleableProperty)yaxis.tickLabelFontProperty()).getCssMetaData();
        prop.set(yaxis, val, null);
        // confirm tickLabelFont, measure and tick's textnode all are in sync with -fx-tick-label-font
        assertEquals(11, new Double(yaxis.getTickLabelFont().getSize()).intValue());
        assertEquals(11, new Double(AxisShim.get_measure(yaxis).getFont().getSize()).intValue());
        final ObservableList<Axis.TickMark<Number>> yaTickMarks = yaxis.getTickMarks();
        TickMark tm = yaTickMarks.get(0);
        assertEquals(11, new Double(AxisShim.TickMark_get_textNode(tm).getFont().getSize()).intValue());
        // set tick label font programmatically and test.
        yaxis.setTickLabelFont(new Font(12.0f));
        assertEquals(12, new Double(yaxis.getTickLabelFont().getSize()).intValue());
        assertEquals(12, new Double(AxisShim.get_measure(yaxis).getFont().getSize()).intValue());
        assertEquals(12, new Double(AxisShim.TickMark_get_textNode(tm).getFont().getSize()).intValue());
    }

    @Test public void testSetTickLabelFill() {
        startApp();
        pulse();
        yaxis.setTickLabelFill(Color.web("#444444"));
        pulse();
        // Check if text node on axis has the right fill
        for (Node n : yaxis.getChildrenUnmodifiable()) {
            if (n instanceof Text) {
                assertEquals(((Text)n).getFill(), Color.web("#444444"));
            }
        }
    }

    @Test public void testAddAxisWithoutSpecifyingSide() {
        final NumberAxis axis = new NumberAxis(0, 12, 1);
        axis.setMaxWidth(Double.MAX_VALUE);
        axis.setPrefWidth(400);
        pulse();
        StackPane layout = new StackPane();
        ParentShim.getChildren(layout).addAll(axis);
        pulse();
        setTestScene(new Scene(layout));
        setTestStage(new Stage());
        getTestStage().setScene(getTestScene());
        getTestStage().show();
        pulse();
    }

    @Test public void testLegendSizeWhenThereIsNoChartData() {
        startApp();
        assertEquals(0, ChartShim.getLegend(areachart).prefHeight(-1), 0);
        assertEquals(0, ChartShim.getLegend(areachart).prefWidth(-1), 0);
    }


    @Test public void canModifySeriesWithoutChart() {
        XYChart.Series series = new XYChart.Series();

        ObservableList<XYChart.Data> dataList1 = FXCollections.observableArrayList();
        dataList1.add(new XYChart.Data(0, 1));
        dataList1.add(new XYChart.Data(1, 2));
        dataList1.add(new XYChart.Data(2, 3));

        series.setData(dataList1);

        assertSame(dataList1, series.getData());

        ObservableList<XYChart.Data> dataList2 = FXCollections.observableArrayList();
        dataList2.add(new XYChart.Data(0, 3));
        dataList2.add(new XYChart.Data(1, 2));
        dataList2.add(new XYChart.Data(2, 1));

        series.setData(dataList2);

        assertSame(dataList2, series.getData());
    }

    @Test
    public void testBindDataToListProperty() {
        createChart();
        ListProperty<XYChart.Series<String, Number>> seriesProperty =
                new SimpleListProperty<>(FXCollections.observableArrayList());

        areachart.dataProperty().bind(seriesProperty);
        ControlTestUtils.runWithExceptionHandler(() -> {
            seriesProperty.add(new XYChart.Series<>());
        });
    }
}
