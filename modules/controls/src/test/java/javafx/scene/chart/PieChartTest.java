/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.charts.Legend;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.text.Text;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author paru
 */
public class PieChartTest extends ChartTestBase {

    ObservableList<PieChart.Data> data;
    PieChart pc;
    
    @Override
    protected Chart createChart() {
        data = FXCollections.observableArrayList();
        pc = new PieChart(data);
        return pc;
    }
    
    @Test
    public void testLabelsVisibleFalse_RT24106() {
        data.add(new PieChart.Data("Sun", 20));
        data.add(new PieChart.Data("IBM", 12));
        data.add(new PieChart.Data("HP", 25));
        data.add(new PieChart.Data("Dell", 22));
        data.add(new PieChart.Data("Apple", 30));
        pc.setLabelsVisible(false);
        assertEquals(false, pc.getLabelsVisible());
    }
    
    @Test
    public void testLegendUpdateAfterPieNameChange_RT26854() {
        startApp();
        data.add(new PieChart.Data("Sun", 20));
        for(Node n : pc.getChartChildren()) {
            if (n instanceof Text) {
                assertEquals("Sun", pc.getData().get(0).getName());
            }
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(PieChartTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        // change name of data item.
        pc.getData().get(0).setName("Oracle");
        for(Node n : pc.getChartChildren()) {
            if (n instanceof Text) {
                assertEquals("Oracle", pc.getData().get(0).getName());
            }
        }
    }
    
    @Test
    public void testDataItemRemovedWithAnimation() {
        startApp();
        pc.setAnimated(true);
        data.add(new PieChart.Data("Sun", 20));
        data.add(new PieChart.Data("IBM", 12));
        data.add(new PieChart.Data("HP", 25));
        data.add(new PieChart.Data("Dell", 22));
        data.add(new PieChart.Data("Apple", 30));
        pc.getData().remove(0);
        assertEquals(4, pc.getData().size());
    }

    @Test
    public void testDataNodeChangeReported() {
        AtomicBoolean called = new AtomicBoolean();

        PieChart.Data data = new PieChart.Data("ABC", 40);
        data.nodeProperty().addListener((o) -> called.set(true));
        pc.getData().add(data);

        assertTrue(called.get());
    }

    private void checkStyleClass(int i, String styleClass) {
        Node item = pc.getData().get(i).getNode();
        assertTrue(item.getStyleClass().toString(),
                item.getStyleClass().contains(styleClass));
        Node legendItem = ((Legend)pc.getLegend()).getItems().get(i).getSymbol();
        assertTrue(legendItem.getStyleClass().toString(),
                legendItem.getStyleClass().contains(styleClass));
    }

    @Test
    public void testCSSStyleClass_DataClear() {
        for (int i = 0; i < 10; i++) {
            data.add(new PieChart.Data(String.valueOf(i), i));
        }
        for (int i = 0; i < 10; i++) {
            checkStyleClass(i, "data"+i);
            checkStyleClass(i, "default-color"+i%8);
        }
        data.clear();
        for (int i = 0; i < 10; i++) {
            data.add(new PieChart.Data(String.valueOf(i), i));
        }
        for (int i = 0; i < 10; i++) {
            checkStyleClass(i, "data"+i);
            checkStyleClass(i, "default-color"+i%8);
        }
    }

    @Test
    public void testCSSStyleClass_DataModify() {
        for (int i = 0; i < 10; i++) {
            data.add(new PieChart.Data(String.valueOf(i), i));
        }
        data.remove(2); // 0, 1, 3, 4, ...
        data.add(3, new PieChart.Data(String.valueOf(7.5), 7.5)); // 0, 1, 3, 7.5, 4
        for (int i = 0; i < 10; i++) {
            checkStyleClass(i, "data"+i);
        }
        checkStyleClass(2, "default-color3");
        checkStyleClass(3, "default-color2");
        checkStyleClass(4, "default-color4");

        data.sort((PieChart.Data d1, PieChart.Data d2) ->
                Double.compare(d1.getPieValue(), d2.getPieValue())
        ); // 0, 1, 3, 4, 5, 6, 7, 7.5, ...
        for (int i = 0; i < 10; i++) {
            checkStyleClass(i, "data"+i);
        }
        checkStyleClass(2, "default-color3");
        checkStyleClass(3, "default-color4");
        checkStyleClass(6, "default-color7");
        checkStyleClass(7, "default-color2");
    }
}
