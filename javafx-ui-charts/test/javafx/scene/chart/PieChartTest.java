/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
    
}
