/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javafx.scene.chart;


import org.junit.Test;
import javafx.collections.*;

import com.sun.javafx.pgstub.StubToolkit;
import javafx.scene.Scene;
import javafx.stage.Stage;



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
}
