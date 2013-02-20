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


import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import org.junit.Test;
import javafx.collections.*;
import javafx.scene.chart.Axis.TickMark;
import javafx.css.ParsedValue;
import javafx.css.CssMetaData;
import javafx.css.StyleableProperty;
import com.sun.javafx.css.parser.CSSParser;

import javafx.scene.text.Font;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;


public class XYChartTest extends ChartTestBase {
    
    NumberAxis yaxis;
    CategoryAxis cataxis;
    
    protected Chart createChart() {
        ObservableList<String> cat = FXCollections.observableArrayList();
        cataxis = CategoryAxisBuilder.create().build();
        yaxis = NumberAxisBuilder.create().build();
        ObservableList<XYChart.Series<String, Number>> nodata = FXCollections.observableArrayList();
        AreaChart<?, ?> areachart = new AreaChart<String, Number>(cataxis, yaxis, nodata);
        areachart.setId("AreaChart");
        return areachart;
    }
    
    @Test
    public void testTickMarksToString() {
        startApp();
        pulse();
        yaxis.getTickMarks().toString(); 
        System.out.println(" --- "+yaxis.getTickMarks().toString());
    }
    
    // RT-22166
    @Test public void testTickLabelFont() {
        startApp();
        Font f = yaxis.getTickLabelFont();
        // default caspian value for font size = 10
        assertEquals(10, new Double(f.getSize()).intValue());
        assertEquals(10, new Double(yaxis.measure.getFont().getSize()).intValue());
        
        // set tick label font via css and test if ticklabelfont, measure and tick textnode follow.
        ParsedValue pv = CSSParser.getInstance().parseExpr("-fx-tick-label-font","0.916667em System");
        Object val = pv.convert(null);        
        CssMetaData prop = ((StyleableProperty)yaxis.tickLabelFontProperty()).getCssMetaData();
        try {
            prop.set(yaxis, val, null);
            // confirm tickLabelFont, measure and tick's textnode all are in sync with -fx-tick-label-font
            assertEquals(11, new Double(yaxis.getTickLabelFont().getSize()).intValue());
            assertEquals(11, new Double(yaxis.measure.getFont().getSize()).intValue());
            final ObservableList<Axis.TickMark<Number>> yaTickMarks = yaxis.getTickMarks();
            TickMark tm = yaTickMarks.get(0);
            assertEquals(11, new Double(tm.textNode.getFont().getSize()).intValue());
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
        // set tick label font programmatically and test.
        yaxis.setTickLabelFont(new Font(12.0f));
        assertEquals(12, new Double(yaxis.getTickLabelFont().getSize()).intValue());
        assertEquals(12, new Double(yaxis.measure.getFont().getSize()).intValue());
        final ObservableList<Axis.TickMark<Number>> yaTickMarks = yaxis.getTickMarks();
        TickMark tm = yaTickMarks.get(0);
        assertEquals(12, new Double(tm.textNode.getFont().getSize()).intValue());
    }
}
