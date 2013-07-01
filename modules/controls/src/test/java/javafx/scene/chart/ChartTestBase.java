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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.collections.ObservableList;

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
    
    protected Scene getTestScene() {
        return this.scene;
    }
    
    protected void setTestScene(Scene scene) {
        this.scene = scene;
    }
    
    protected Stage getTestStage() {
        return this.stage;
    }
    
    protected void setTestStage(Stage stage) {
        this.stage = stage;
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
