/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javafx.scene.chart;

import java.util.Arrays;
import javafx.collections.FXCollections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import javafx.collections.*;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

/**
 *
 * @author paru
 */
public class BarChartTest extends ChartTestBase {

    static String[] years = {"2010", "2011", "2012"};
    static double[] anvilsSold = { 567, 1292, 2423 };
    static double[] skatesSold = { 956, 1665, 2559 };
    static double[] pillsSold = { 1154, 1927, 2774 };
    final CategoryAxis xAxis = new CategoryAxis();
    final NumberAxis yAxis = new NumberAxis();
    final BarChart<String,Number> bc = new BarChart<String,Number>(xAxis,yAxis);
    
    @Override
    protected Chart createChart() {
        xAxis.setLabel("X Axis");
        xAxis.setCategories(FXCollections.<String>observableArrayList(Arrays.asList(years)));
        yAxis.setLabel("Y Axis");
        // add starting data
        XYChart.Series<String,Number> series1 = new XYChart.Series<String,Number>();
        series1.setName("Data Series 1");
        XYChart.Series<String,Number> series2 = new XYChart.Series<String,Number>();
        series2.setName("Data Series 2");
        series1.getData().add(new XYChart.Data<String,Number>(years[0], 567));
        series1.getData().add(new XYChart.Data<String,Number>(years[1], 1292));
        series1.getData().add(new XYChart.Data<String,Number>(years[2], 2180));

        series2.getData().add(new XYChart.Data<String,Number>(years[0], 956));
        series2.getData().add(new XYChart.Data<String,Number>(years[1], 1665));
        series2.getData().add(new XYChart.Data<String,Number>(years[2], 2450));
        bc.getData().add(series1);
        bc.getData().add(series2);
        return bc;
    }
    
    @Test
    public void testAddingCustomStyleClassToBarChartBarNodes() {
        startApp();
        XYChart.Series<String, Number> series = new XYChart.Series();
        ObservableList<XYChart.Data<String, Number>> seriesData = series.getData();
        String xValue = "A";
        Number yValue = Integer.valueOf(20);
        XYChart.Data<String, Number> item = new XYChart.Data(xValue, yValue);
        Node bar = item.getNode();
        if (bar == null) {
            bar = new StackPane();
        }
        String myStyleClass = "my-style";
        bar.getStyleClass().add(myStyleClass);
        item.setNode(bar);
        seriesData.add(item); 
        bc.getData().add(series);
        assertEquals("my-style", bar.getStyleClass().get(0));
    }
    
    @Test
    public void testCategoryAxisCategoriesOnAddDataAtIndex() {
        startApp();
        bc.getData().clear();
        xAxis.getCategories().clear();
        XYChart.Series<String,Number> series = new XYChart.Series<String,Number>();
        series.getData().clear();
        series.getData().add(new XYChart.Data<String, Number>("1", 1));
        series.getData().add(new XYChart.Data<String, Number>("2", 2));
        series.getData().add(new XYChart.Data<String, Number>("3", 3));
        bc.getData().add(series); 
        pulse();
        // category at index 0 = "1"
        assertEquals("1", xAxis.getCategories().get(0));
        series.getData().add(0, new XYChart.Data<String, Number>("0", 5));
        pulse();
        // item inserted at 0; category at index 0 = 0
        assertEquals("0", xAxis.getCategories().get(0));
    }
    
}
