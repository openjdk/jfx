/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.canvas;

import javafx.scene.NodeTest;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.junit.Test;
import static org.junit.Assert.*;

public class CanvasTest {

    @Test public void testPropertyPropagation_visible() throws Exception {
        final Canvas node = new Canvas();
        NodeTest.testBooleanPropertyPropagation(node, "visible", false, true);
    }

//    @Test public void testPropertyPropagation_x() throws Exception {
//        final Canvas node = new Canvas();
//        NodeTest.testDoublePropertyPropagation(node, "x", 100, 200);
//    }
//
//    @Test public void testPropertyPropagation_y() throws Exception {
//        final Canvas node = new Canvas();
//        NodeTest.testDoublePropertyPropagation(node, "y", 100, 200);
//    }

    @Test public void testInitCanvas() throws Exception {
        final Canvas node = new Canvas();
    }
    
    @Test public void testGetGC() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
    }
    
    //maybe test doing stuff from different threads
    @Test public void testGetGC2() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        GraphicsContext gc2 = node.getGraphicsContext2D();
        GraphicsContext gc3 = node.getGraphicsContext2D();
        GraphicsContext gc4 = node.getGraphicsContext2D();
        GraphicsContext gc5 = node.getGraphicsContext2D();
        assertEquals(gc,gc2);
        assertEquals(gc,gc3);
        assertEquals(gc,gc4);
        assertEquals(gc,gc5);
    }
    
    //basic tests make sure that the methods do not blow up.
    @Test public void testGCfillRect_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        gc.fillRect(0, 0, 1, 1);
    }
    
    @Test public void testGCfillOval_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        gc.fillOval(0, 0, 1, 1);
    }
        
    @Test public void testGCfillRoundRect_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        gc.fillRoundRect(0, 0, 1, 1, 2, 2);
    }
    
    @Test public void testGCfillText_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        gc.fillText("Test", 0, 0);
        gc.fillText("", 0, 0);
        gc.fillText(null, 0, 0);
    }
    
    @Test public void testGCfillPolygon_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        double[] xPoints = {0.0,10.0};
        double[] yPoints = {0.0,10.0};        
        gc.fillPolygon( xPoints, yPoints, 2);
    }
    
    @Test public void testGCfillArc_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        double[] xPoints = {0.0,10.0};
        double[] yPoints = {0.0,10.0};        
        gc.fillArc(10, 10, 100, 100, 0, 40, ArcType.OPEN); 
        gc.fillArc(10, 10, 100, 100, 0, 360, ArcType.CHORD); 
        gc.fillArc(10, 10, 100, 100, 0, 361, ArcType.ROUND); 
    }
    
    @Test public void testGCdrawRect_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        gc.rect(0, 0, 1, 1);
    }
    
    @Test public void testGCdrawOval_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        gc.strokeOval(0, 0, 1, 1);
    }
        
    @Test public void testGCdrawRoundRect_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        gc.strokeRoundRect(0, 0, 1, 1, 2, 2);
    }
    
    @Test public void testGCstrokeText_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        gc.strokeText("Test", 0, 0);
        gc.strokeText("", 0, 0);
        gc.strokeText(null, 0, 0);
    }
    
    @Test public void testGCdrawPolygon_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        double[] xPoints = {0.0,10.0};
        double[] yPoints = {0.0,10.0};        
        gc.strokePolygon( xPoints, yPoints, 2);
    }
    
    @Test public void testGCdrawArc_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        double[] xPoints = {0.0,10.0};
        double[] yPoints = {0.0,10.0};        
        gc.strokeArc(10, 10, 100, 100, 0, 40, ArcType.OPEN); 
        gc.strokeArc(10, 10, 100, 100, 0, 360, ArcType.CHORD); 
        gc.strokeArc(10, 10, 100, 100, 0, 361, ArcType.ROUND); 
    }
    
    @Test public void testGCfillPath_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        gc.arcTo(0, 0, 5, 5, 5);
        gc.moveTo(50, 50);
        gc.lineTo(100, 100);
        gc.bezierCurveTo(75, 150, 75, 150, 80, 80);
        gc.beginPath();
        gc.moveTo(50, 50);
        gc.lineTo(100, 100);
        gc.bezierCurveTo(75, 150, 75, 150, 80, 80);
        gc.arcTo(0, 0, 5, 5, 5);
        gc.closePath();
    }
    
    @Test public void testGCclip_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        gc.beginPath();
        gc.moveTo(50, 50);
        gc.lineTo(100, 100);
        gc.bezierCurveTo(75, 150, 75, 150, 80, 80);
        gc.arcTo(0, 0, 5, 5, 5);
        gc.closePath();
        gc.clip();
    }
    
    @Test public void testGCfillDrawPath_basic() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        gc.beginPath();
        gc.moveTo(50, 50);
        gc.lineTo(100, 100);
        gc.bezierCurveTo(75, 150, 75, 150, 80, 80);
        gc.arcTo(0, 0, 5, 5, 5);
        gc.closePath();
        gc.stroke();
        gc.fill();
    }
    
    
    
    @Test public void testGCState_Translate() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        
        gc.translate(50, 50);
        Affine result = gc.getTransform();
        Affine expected = new Affine();
        
        expected.setTx(50);
        expected.setTy(50);
        
        assertMatrix(result, expected);
    }
    
    @Test public void testGCState_Scale() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        
        gc.scale(3, 3);
        Affine result = gc.getTransform();
        Affine expected = new Affine();
        
        expected.setMxx(3);
        expected.setMyy(3);
        
        assertMatrix(result, expected);
    }

    @Test public void testGCState_Rotate() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        
        gc.rotate(45.0);
        Affine result = gc.getTransform();
        
        Rotate expected = new Rotate(45, 0, 0);
                
        assertMatrix(result, expected);
    }

    @Test public void testGCState_getTransform() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        

        Affine expected = new Affine();
        gc.setTransform(expected);
        Affine result = gc.getTransform();
        
        assertMatrix(result, expected);
        
        gc.setTransform(expected.getMxx(), expected.getMyx(), expected.getMxy(), 
                expected.getMyy(), expected.getTx(), expected.getTy());
        
        Affine result2 = gc.getTransform();
        
        assertMatrix(result2, expected);        
    }

    @Test public void testGCState_FillStrokeSaveRestore() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        
        Affine expected = new Affine();
        gc.setTransform(expected);
        Affine result = gc.getTransform();
        
        assertMatrix(result, expected);
        
        gc.setFill(Color.BLACK);
        assertEquals(Color.BLACK, gc.getFill());
        gc.save();
        gc.setFill(Color.RED);
        assertEquals(gc.getFill(), Color.RED);
        gc.restore();
        assertEquals(Color.BLACK, gc.getFill());
        gc.setStroke(Color.BLACK);
        assertEquals(Color.BLACK, gc.getStroke());
        gc.save();
        gc.setStroke(Color.RED);
        assertEquals(gc.getStroke(), Color.RED);
        gc.restore();
        assertEquals(Color.BLACK, gc.getStroke());
        assertMatrix(result, expected);
    }

    @Test public void testGCState_Line() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        
        gc.setLineCap(StrokeLineCap.BUTT);
        gc.setLineJoin(StrokeLineJoin.MITER);
        gc.setLineWidth(5);
        gc.setMiterLimit(3);
        
        gc.save();
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.BEVEL);
        gc.setLineWidth(1);
        gc.setMiterLimit(1);
        assertEquals(gc.getLineCap(), StrokeLineCap.ROUND);
        assertEquals(gc.getLineJoin(), StrokeLineJoin.BEVEL);
        assertEquals(gc.getLineWidth(), 1, 0.00001);
        assertEquals(gc.getMiterLimit(), 1, 0.00001);
        gc.restore();
        
        assertEquals(gc.getLineCap(), StrokeLineCap.BUTT);
        assertEquals(gc.getLineJoin(), StrokeLineJoin.MITER);
        assertEquals(gc.getLineWidth(), 5, 0.00001);
        assertEquals(gc.getMiterLimit(), 3, 0.00001);
    }

    @Test (expected=NullPointerException.class) 
    public void testGCState_LineNull() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        
        gc.setLineCap(null);     
    }

    @Test (expected=NullPointerException.class) 
    public void testGCState_LineNull2() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        
        gc.setLineJoin(null);      
    }

    @Test public void testGCState_BlendMode() throws Exception {
        Canvas node = new Canvas();
        GraphicsContext gc = node.getGraphicsContext2D();
        
        gc.setGlobalBlendMode(BlendMode.ADD);
        gc.setGlobalAlpha(0);
        
        gc.save();
        gc.setGlobalAlpha(0.5);
        gc.setGlobalBlendMode(BlendMode.COLOR_BURN);
        assertEquals(gc.getGlobalBlendMode(), BlendMode.COLOR_BURN);
        assertEquals(gc.getGlobalAlpha(), 0.5, 0.000001);
        gc.restore();
        
        assertEquals(BlendMode.ADD, gc.getGlobalBlendMode());
        assertEquals(0, gc.getGlobalAlpha(), 0.000001);       
    }

    public static void assertMatrix(Transform expected,
            Transform result) {
        assertEquals(expected.getMxx(), result.getMxx(), 0.00001);
        assertEquals(expected.getMxy(), result.getMxy(), 0.00001);
        assertEquals(expected.getMxz(), result.getMxz(), 0.00001);
        assertEquals(expected.getTx(), result.getTx(), 0.00001);
        assertEquals(expected.getMyx(), result.getMyx(), 0.00001);
        assertEquals(expected.getMyy(), result.getMyy(), 0.00001);
        assertEquals(expected.getMyz(), result.getMyz(), 0.00001);
        assertEquals(expected.getTy(), result.getTy(), 0.00001);
        assertEquals(expected.getMzx(), result.getMzx(), 0.00001);
        assertEquals(expected.getMzy(), result.getMzy(), 0.00001);
        assertEquals(expected.getMzz(), result.getMzz(), 0.00001);
        assertEquals(expected.getTz(), result.getTz(), 0.00001);
    }  
}
