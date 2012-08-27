/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javafx.scene.chart;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    
    
}
