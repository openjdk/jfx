/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javafx.scene.chart;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.layout.Region;
import javafx.scene.shape.*;


/**
 *
 * @author paru
 */
public abstract class ChartTestBase {
    private Scene scene;
    private Stage stage;
    StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit(); 
    private Chart chart;
    
    @Before
    public void setUp() {
        chart = createChart();
        chart.setAnimated(false);
    }
    
    protected void startApp() {
        scene = new Scene(chart,800,600);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
        pulse();
    }
    
    protected void pulse() {
        toolkit.fireTestPulse();
    }
    
    protected abstract Chart createChart();
    
    StringBuffer computeSVGPath(Path line) {
        StringBuffer str = new StringBuffer();
        for(PathElement pe : line.getElements()) {
            if (pe instanceof LineTo) {
                str.append("L"+((LineTo)pe).getX()+" "+((LineTo)pe).getY()+" ");
            } 
        }
        return str;
    }
    
    StringBuffer computeBoundsString(Region r1, Region r2, Region r3) {
        StringBuffer str = new StringBuffer();
        str.append(Math.round(r1.getLayoutX())
                                +" "+Math.round(r1.getLayoutY())+" "+Math.round(r1.getWidth())+
                                " "+Math.round(r1.getHeight())+" ");
        str.append(Math.round(r2.getLayoutX())
                                +" "+Math.round(r2.getLayoutY())+" "+Math.round(r2.getWidth())+
                                " "+Math.round(r2.getHeight())+" ");
        str.append(Math.round(r3.getLayoutX())
                                +" "+Math.round(r3.getLayoutY())+" "+Math.round(r3.getWidth())+
                                " "+Math.round(r3.getHeight())+" ");
        return str;
    }
}