/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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


public class LineChartTest extends ChartTestBase {

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
        ObservableList<XYChart.Data> data = FXCollections.observableArrayList();
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
}