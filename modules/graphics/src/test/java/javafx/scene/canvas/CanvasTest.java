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

import javafx.geometry.VPos;
import javafx.scene.NodeTest;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.ImageForTesting;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CanvasTest {

    private Canvas canvas;
    private GraphicsContext gc;

    @Before
    public void setUp() {
        canvas = new Canvas();
        gc = canvas.getGraphicsContext2D();
    }

    @Test public void testPropertyPropagation_visible() throws Exception {
        NodeTest.testBooleanPropertyPropagation(canvas, "visible", false, true);
    }

    //maybe test doing stuff from different threads
    @Test public void testGetGC2() throws Exception {
        GraphicsContext gc2 = canvas.getGraphicsContext2D();
        GraphicsContext gc3 = canvas.getGraphicsContext2D();
        GraphicsContext gc4 = canvas.getGraphicsContext2D();
        GraphicsContext gc5 = canvas.getGraphicsContext2D();
        assertEquals(gc,gc2);
        assertEquals(gc,gc3);
        assertEquals(gc,gc4);
        assertEquals(gc,gc5);
    }
    
    //basic tests make sure that the methods do not blow up.
    @Test public void testGCfillRect_basic() throws Exception {
        gc.fillRect(0, 0, 1, 1);
    }
    
    @Test public void testGCfillOval_basic() throws Exception {
        gc.fillOval(0, 0, 1, 1);
    }
        
    @Test public void testGCfillRoundRect_basic() throws Exception {
        gc.fillRoundRect(0, 0, 1, 1, 2, 2);
    }
    
    @Test public void testGCfillText_basic() throws Exception {
        gc.fillText("Test", 0, 0);
        gc.fillText("Test", 0, 0, 0);
        gc.fillText("", 0, 0, 0);
        gc.fillText("", 0, 0);
        gc.fillText(null, 0, 0);
        gc.fillText(null, 0, 0, 0);
    }
    
    @Test public void testGCfillPolygon_basic() throws Exception {
        double[] xPoints = {0.0,10.0};
        double[] yPoints = {0.0,10.0};        
        gc.fillPolygon( xPoints, yPoints, 2);
        gc.fillPolygon( xPoints, null, 2);
        gc.fillPolygon( null, yPoints, 2);
    }
    
    @Test public void testGCfillArc_basic() throws Exception {
        gc.fillArc(10, 10, 100, 100, 0, 40, ArcType.OPEN);
        gc.fillArc(10, 10, 100, 100, 0, 360, ArcType.CHORD); 
        gc.fillArc(10, 10, 100, 100, 0, 361, ArcType.ROUND); 
        gc.fillArc(10, 10, 100, 100, 0, 361, null);
    }
    
    @Test public void testGCdrawRect_basic() throws Exception {
        gc.rect(0, 0, 1, 1);
    }
    
    @Test public void testGCdrawOval_basic() throws Exception {
        gc.strokeOval(0, 0, 1, 1);
    }
        
    @Test public void testGCdrawRoundRect_basic() throws Exception {
        gc.strokeRoundRect(0, 0, 1, 1, 2, 2);
    }
    
    @Test public void testGCstrokeText_basic() throws Exception {
        gc.strokeText("Test", 0, 0);
        gc.strokeText("", 0, 0);
        gc.strokeText(null, 0, 0);
    }
    
    @Test public void testGCdrawPolygon_basic() throws Exception {
        double[] xPoints = {0.0,10.0};
        double[] yPoints = {0.0,10.0};        
        gc.strokePolygon( xPoints, yPoints, 2);
        gc.strokePolygon( null, yPoints, 2);
        gc.strokePolygon( xPoints, null, 2);
    }

    @Test public void testGCdrawArc_basic() throws Exception {
        gc.strokeArc(10, 10, 100, 100, 0, 40, ArcType.OPEN);
        gc.strokeArc(10, 10, 100, 100, 0, 360, ArcType.CHORD); 
        gc.strokeArc(10, 10, 100, 100, 0, 361, ArcType.ROUND); 
        gc.strokeArc(10, 10, 100, 100, 0, 361, null);
    }
    
    @Test public void testGCfillPath_basic() throws Exception {
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
        gc.beginPath();
        gc.moveTo(50, 50);
        gc.lineTo(100, 100);
        gc.bezierCurveTo(75, 150, 75, 150, 80, 80);
        gc.arcTo(0, 0, 5, 5, 5);
        gc.closePath();
        gc.clip();
    }
    
    @Test public void testGCfillDrawPath_basic() throws Exception {
        gc.beginPath();
        gc.moveTo(50, 50);
        gc.lineTo(100, 100);
        gc.bezierCurveTo(75, 150, 75, 150, 80, 80);
        gc.arcTo(0, 0, 5, 5, 5);
        gc.closePath();
        gc.stroke();
        gc.fill();
    }

    @Test public void testGCPath_LineTo_NoMoveto() throws Exception {
        gc.lineTo(10, 10);
    }

    @Test public void testGCPath_QuadraticCurveTo_NoMoveto() throws Exception {
        gc.quadraticCurveTo(10, 10, 20, 20);
    }

    @Test public void testGCPath_BezierCurveTo_NoMoveto() throws Exception {
        gc.bezierCurveTo(10, 10, 20, 20, 30, 30);
    }

    @Test public void testGCPath_ArcTo_NoMoveto() throws Exception {
        gc.arcTo(10, 10, 20, 20, 30);
    }

    @Test public void testGCPath_Arc_NoMoveto() throws Exception {
        gc.arc(10, 10, 20, 20, 30, 30);
    }

    @Test public void testGCPath_ClosePath_NoMoveto() throws Exception {
        gc.closePath();
    }

    @Test public void testGCPath_Rect_NoMoveto() throws Exception {
        gc.rect(10, 10, 20, 20);
    }

    @Test public void testGCState_Translate() throws Exception {
        gc.translate(50, 50);
        Affine result = gc.getTransform();
        Affine expected = new Affine();
        
        expected.setTx(50);
        expected.setTy(50);
        
        assertMatrix(result, expected);
    }
    
    @Test public void testGCState_Scale() throws Exception {
        gc.scale(3, 3);
        Affine result = gc.getTransform();
        Affine expected = new Affine();
        
        expected.setMxx(3);
        expected.setMyy(3);
        
        assertMatrix(result, expected);
    }

    @Test public void testGCState_Rotate() throws Exception {
        gc.rotate(45.0);
        Affine result = gc.getTransform();
        
        Rotate expected = new Rotate(45, 0, 0);
                
        assertMatrix(result, expected);
    }

    @Test public void testGCState_getTransform() throws Exception {
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

    @Test public void testGCState_SetStroke() {
        gc.setStroke(Color.RED);
        assertEquals(Color.RED, gc.getStroke());
        gc.setStroke(null);
        assertEquals(Color.RED, gc.getStroke());
    }

    @Test public void testGCState_Fill_Null() {
        gc.setFill(Color.BLACK);
        assertEquals(Color.BLACK, gc.getFill());
        gc.setFill(Color.RED);
        assertEquals(gc.getFill(), Color.RED);
        gc.setFill(null);
        assertEquals(gc.getFill(), Color.RED);
    }

    @Test public void testGCState_FillRule_Null() {
        gc.setFillRule(FillRule.EVEN_ODD);
        assertEquals(FillRule.EVEN_ODD, gc.getFillRule());
        gc.setFillRule(null);
        assertEquals(FillRule.EVEN_ODD, gc.getFillRule());
    }

    @Test public void testGCState_Font_Null() {
        Font f = new Font(10);
        gc.setFont(f);
        assertEquals(f, gc.getFont());
        gc.setFont(null);
        assertEquals(f, gc.getFont());
    }

    @Test public void testGCState_TextBaseline_Null() {
        gc.setTextBaseline(VPos.BASELINE);
        assertEquals(VPos.BASELINE, gc.getTextBaseline());
        gc.setTextBaseline(null);
        assertEquals(VPos.BASELINE, gc.getTextBaseline());
    }

    @Test public void testGCState_TextAlign_Null() {
        gc.setTextAlign(TextAlignment.JUSTIFY);
        assertEquals(TextAlignment.JUSTIFY, gc.getTextAlign());
        gc.setTextAlign(null);
        assertEquals(TextAlignment.JUSTIFY, gc.getTextAlign());
    }

    @Test public void testGCState_Line() throws Exception {
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

    @Test
    public void testGCState_LineCapNull() throws Exception {
        gc.setLineCap(StrokeLineCap.BUTT);
        gc.setLineCap(null);
        assertEquals(gc.getLineCap(), StrokeLineCap.BUTT);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineCap(null);
        assertEquals(gc.getLineCap(), StrokeLineCap.ROUND);
        gc.setLineCap(StrokeLineCap.SQUARE);
        gc.setLineCap(null);
        assertEquals(gc.getLineCap(), StrokeLineCap.SQUARE);
    }

    @Test
    public void testGCState_LineJoinNull() throws Exception {
        gc.setLineJoin(StrokeLineJoin.BEVEL);
        gc.setLineJoin(null);
        assertEquals(gc.getLineJoin(), StrokeLineJoin.BEVEL);
        gc.setLineJoin(StrokeLineJoin.MITER);
        gc.setLineJoin(null);
        assertEquals(gc.getLineJoin(), StrokeLineJoin.MITER);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setLineJoin(null);
        assertEquals(gc.getLineJoin(), StrokeLineJoin.ROUND);
    }

    @Test public void testGCState_BlendMode() throws Exception {
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

    @Test public void testGCState_BlendMode_Null() {
        gc.setGlobalBlendMode(BlendMode.ADD);
        assertEquals(BlendMode.ADD, gc.getGlobalBlendMode());
        gc.setGlobalBlendMode(null);
        assertEquals(BlendMode.ADD, gc.getGlobalBlendMode());
    }

    @Test public void testGCappendSVGPath_Null() {
        gc.appendSVGPath("m 0 0");
        gc.appendSVGPath("Q 150 -300 300 0");
        gc.appendSVGPath(null);
    }

    @Test public void testGCappendSVGPath_IncorrectPath() {
        gc.appendSVGPath("Q 150 -300 300 0"); // No move at the beginning
    }

    @Test public void testGCappendSVGPath_IncorrectPath2() {
        gc.appendSVGPath("F 150"); // No move at the beginning
    }

    @Test public void testGCapplyEffect_Null() {
        gc.applyEffect(null);
    }

    @Test public void testGCdrawImage_Null() {
        gc.drawImage(null, 0 ,0);
        gc.drawImage(null, 0 ,0, 100, 100);
        gc.drawImage(null, 0, 0, 100, 100, 0, 0, 100, 100);
    }

    @Test public void testGCdrawImage_InProgress() {
        ImageForTesting image = new ImageForTesting("http://something.png", false);
        image.updateProgress(0.5);

        gc.drawImage(image, 0 ,0);
        gc.drawImage(image, 0 ,0, 100, 100);
        gc.drawImage(image, 0, 0, 100, 100, 0, 0, 100, 100);
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
