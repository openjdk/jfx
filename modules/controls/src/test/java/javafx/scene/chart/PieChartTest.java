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

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.text.Text;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

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
        pc = new PieChart();
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
        pc.getData().addAll(data);
        assertEquals(false, pc.getLabelsVisible());
    }
    
    @Test
    public void testLegendUpdateAfterPieNameChange_RT26854() {
        startApp();
        data.add(new PieChart.Data("Sun", 20));
        pc.getData().addAll(data);
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
        data.add(new PieChart.Data("Sun", 20));
        data.add(new PieChart.Data("IBM", 12));
        data.add(new PieChart.Data("HP", 25));
        data.add(new PieChart.Data("Dell", 22));
        data.add(new PieChart.Data("Apple", 30));
        pc.setAnimated(true);
        pc.getData().addAll(data);
        pc.getData().remove(0);
        assertEquals(4, pc.getData().size());
    }
    
}
