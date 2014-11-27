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

package com.sun.javafx.scene.control.skin;

import javafx.css.CssMetaData;
import com.sun.javafx.css.Stylesheet;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author dgrieve
 */
public class LabeledTextTest {
    
    Label label;
    LabeledText labeledText;
    Stage stage;
    Scene scene;
    Stylesheet stylesheet;
    
    public LabeledTextTest() {
    }
    
    @Before public void setup() {
        label = 
            new Label("\"A computer once beat me at chess, "
                + "but it was no match for me at kick boxing.\" Emo Philips");
        stage = new Stage();
        stage.setScene(scene = new Scene(label));
        scene.getStylesheets().add(LabeledTextTest.class.getResource("LabeledTextTest.css").toExternalForm());
        label.impl_processCSS(true);
        labeledText = ((com.sun.javafx.scene.control.skin.LabeledSkinBase)label.getSkin()).text; 
    }

    @Test
    public void testLabeledTextAlignmentStyleAffectsLabeledText() {
        
        label.setStyle("-fx-text-alignment: right;");
        label.impl_processCSS(true);
        assertEquals(TextAlignment.RIGHT, label.getTextAlignment());
        assertEquals(TextAlignment.RIGHT, labeledText.getTextAlignment());
    
    }
    
    @Test
    public void testLabeledTextAlignmentIsBound() {
        try {
            labeledText.setTextAlignment(TextAlignment.RIGHT);
            fail();
        } catch (RuntimeException re) {
        }
    }
    

    @Test
    public void testLabeledFontStyleAffectsLabeledText() {
        
        label.setStyle("-fx-font: 10px Amble;");
        label.impl_processCSS(true);
        Font expected = Font.font("Amble", 10);
        assertEquals(expected, label.getFont());
        assertEquals(expected, labeledText.getFont());
    
    }
    
    @Test
    public void testLabeledTextFontIsBound() {
        try {
            labeledText.setFont(Font.getDefault());
            fail();
        } catch (RuntimeException re) {
        }
    }
    
    
    @Test
    public void testLabeledTextFillStyleAffectsLabeledText() {
        
        label.setStyle("-fx-text-fill: rgb(255,0,0);");
        label.impl_processCSS(true);
        Color expected = Color.rgb(255, 0, 0);
        assertEquals(expected, label.getTextFill());
        assertEquals(expected, labeledText.getFill());
    
    }
    
    @Test
    public void testLabeledTextFillIsBound() {
        try {
            labeledText.setFill(Color.RED);
            fail();
        } catch (RuntimeException re) {
        }
    }

    
    @Test
    public void testLabeledUnderlineStyleAffectsLabeledText() {
        
        label.setStyle("-fx-underline: true;");
        label.impl_processCSS(true);
        assert(label.isUnderline() == true);
        assert(labeledText.isUnderline() == true);
    
    }

    @Test
    public void testLabeledBlendModeStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-blend-mode: color-burn;");
        label.impl_processCSS(true);
        assertEquals(BlendMode.COLOR_BURN,label.getBlendMode());
        assertFalse(BlendMode.COLOR_BURN.equals(labeledText.getBlendMode())); 
    }
    
    @Test
    public void testLabeledCursorStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-cursor: crosshair;");
        label.impl_processCSS(true);
        assertEquals(Cursor.CROSSHAIR,label.getCursor());
        assertFalse(Cursor.CROSSHAIR.equals(labeledText.getCursor()));
    }
    
    @Test
    public void testLabeledEffectStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-effect: dropshadow(one-pass-box, red, 64, .5, 2, 3);");
        label.impl_processCSS(true);
        assertNotNull(label.getEffect());
        assertNull(labeledText.getEffect()); 
    }
    
    @Test
    public void testLabeledFocusTraversableStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-focus-traversable: true;");
        label.impl_processCSS(true);
        assert(label.focusTraversableProperty().get() == true);   
        assert(labeledText.focusTraversableProperty().get() == false);   
    }
    
    @Test
    public void testLabeledOpacityStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-opacity: .5;");
        label.impl_processCSS(true);
        assertEquals(label.getOpacity(), .5, .0000001);   
        assertEquals(labeledText.getOpacity(), 1, .0000001);   
    }

    @Test
    public void testLabeledRotateStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-rotate: 180;");
        label.impl_processCSS(true);
        assertEquals(label.getRotate(), 180, .0000001);
        assertEquals(labeledText.getRotate(), 0, .0000001);   
    }
    
    @Test
    public void testLabeledScaleXStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-scale-x: .5;");
        label.impl_processCSS(true);
        assertEquals(label.getScaleX(), .5, .0000001);
        assertEquals(labeledText.getScaleX(), 1, .0000001);   
    }
    
    @Test
    public void testLabeledScaleYStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-scale-y: .5;");
        label.impl_processCSS(true);
        assertEquals(label.getScaleY(), .5, .0000001);
        assertEquals(labeledText.getScaleY(), 1, .0000001);   
    }

    @Test
    public void testLabeledScaleZStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-scale-z: .5;");
        label.impl_processCSS(true);
        assertEquals(label.getScaleZ(), .5, .0000001);
        assertEquals(labeledText.getScaleZ(), 1, .0000001);   
    }
    
    @Test
    public void testLabeledTranslateXStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-translate-x: .5;");
        label.impl_processCSS(true);
        assertEquals(label.getTranslateX(), .5, .0000001);
        assertEquals(labeledText.getTranslateX(), 0, .0000001);   
    }
    
    @Test
    public void testLabeledTranslateYStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-translate-y: .5;");
        label.impl_processCSS(true);
        assertEquals(label.getTranslateY(), .5, .0000001);
        assertEquals(labeledText.getTranslateY(), 0, .0000001);   
    }
    
    @Test
    public void testLabeledTranslateZStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-translate-z: .5;");
        label.impl_processCSS(true);
        assertEquals(label.getTranslateZ(), .5, .0000001);
        assertEquals(labeledText.getTranslateZ(), 0, .0000001);   
    }
    
    @Test
    public void testLabeledVisibilityStyleDoesNotAffectLabeledText() {
        label.setStyle("visibility: false;");
        label.impl_processCSS(true);
        assert(label.visibleProperty().get() == false);   
        assert(labeledText.visibleProperty().get() == true);   
    }
    
    private static CssMetaData getCssMetaData(String prop) {
        for (CssMetaData styleable : LabeledText.getClassCssMetaData()) {
            if (styleable.getProperty().equals(prop)) return styleable;
        }
        return null;
    }
    
    @Test
    public void testLabeledTextFillIsSettableByCss() {
        
        CssMetaData sp = getCssMetaData("-fx-fill");
        assertTrue(sp.isSettable(labeledText));
    }
    
    @Test
    public void testCanStyleFillOnLabeledText() {
        assertEquals(Color.GREEN, labeledText.getFill());    
    }

    
    @Test
    public void testLabeledTextTextAlignmentIsSettableByCss() {
        
        CssMetaData sp = getCssMetaData("-fx-text-alignment");
        assertTrue(sp.isSettable(labeledText));
    }
    
    @Test
    public void testCanStyleTextAlignmentOnLabeledText() {
        assertEquals(TextAlignment.RIGHT, labeledText.getTextAlignment());
    }
    
    
    @Test
    public void testLabeledTextFontIsSettableByCss() {
        
        CssMetaData sp = getCssMetaData("-fx-font");
        assertTrue(sp.isSettable(labeledText));
    }
    
    @Test
    public void testCanStyleFontOnLabeledText() {
        assertEquals(Font.font("Amble", 10), labeledText.getFont());
    }
    
    
    @Test
    public void testLabeledTextUnderlineIsSettableByCss() {
        
        CssMetaData sp = getCssMetaData("-fx-underline");
        assertTrue(sp.isSettable(labeledText));
    }
    
    @Test
    public void testCanStyleUnderlineOnLabeledText() {
        assertTrue(labeledText.isUnderline());
    }
    
    @Test
    public void testLabeledSetTextFillNotOverridenByUAStyleOnLabeledText() {
        label = 
            new Label("\"A computer once beat me at chess, "
                + "but it was no match for me at kick boxing.\" Emo Philips") {
                    @Override public String getUserAgentStylesheet() {
                        return LabeledTextTest.class.getResource("LabeledTextTest.css").toExternalForm();
                    }
                };
        label.setTextFill(Color.YELLOW);
        stage.setScene(scene = new Scene(label));
        stage.show();
//        label.impl_processCSS(true);
        labeledText = ((com.sun.javafx.scene.control.skin.LabeledSkinBase)label.getSkin()).text; 
        assertEquals(Color.YELLOW, labeledText.getFill());
    }
    
    @Test
    public void testLabeledSetFontNotOverridenByUAStyleOnLabeledText() {
        label = 
            new Label("\"A computer once beat me at chess, "
                + "but it was no match for me at kick boxing.\" Emo Philips") {
                    @Override public String getUserAgentStylesheet() {
                        return LabeledTextTest.class.getResource("LabeledTextTest.css").toExternalForm();
                    }
                };
        Font font = Font.font("Amble", 30);
        label.setFont(font);
        stage.setScene(scene = new Scene(label));
        stage.show();
//        label.impl_processCSS(true);
        labeledText = ((com.sun.javafx.scene.control.skin.LabeledSkinBase)label.getSkin()).text; 
        assertEquals(font, labeledText.getFont());
    }
    
    @Test
    public void testLabeledSetTextAlignmentNotOverridenByUAStyleOnLabeledText() {
        label = 
            new Label("\"A computer once beat me at chess, "
                + "but it was no match for me at kick boxing.\" Emo Philips") {
                    @Override public String getUserAgentStylesheet() {
                        return LabeledTextTest.class.getResource("LabeledTextTest.css").toExternalForm();
                    }
                };
        label.setTextAlignment(TextAlignment.JUSTIFY);
        stage.setScene(scene = new Scene(label));
        stage.show();
//        label.impl_processCSS(true);
        labeledText = ((com.sun.javafx.scene.control.skin.LabeledSkinBase)label.getSkin()).text; 
        assertEquals(TextAlignment.JUSTIFY, labeledText.getTextAlignment());
    }
    
    @Test
    public void testLabeledSetUnderlineNotOverridenByUAStyleOnLabeledText() {
        label = 
            new Label("\"A computer once beat me at chess, "
                + "but it was no match for me at kick boxing.\" Emo Philips") {
                    @Override public String getUserAgentStylesheet() {
                        return LabeledTextTest.class.getResource("LabeledTextTest.css").toExternalForm();
                    }
                };
        label.setUnderline(true);
        stage.setScene(scene = new Scene(label));
        stage.show();
//        label.impl_processCSS(true);
        labeledText = ((com.sun.javafx.scene.control.skin.LabeledSkinBase)label.getSkin()).text; 
        assertTrue(labeledText.isUnderline());
    }


    @Test public void test_RT_37787() {

        label = new Label("test_RT_37787");
        label.getStyleClass().clear();
        label.setId("test-rt-37787");

        scene = new Scene(new Group(label));
        String url = getClass().getResource("LabeledTextTest.css").toExternalForm();
        scene.getStylesheets().add(url);

        label.setFont(Font.font(10));
        assertEquals(10, label.getFont().getSize(), .1);

        stage.setScene(scene);
        stage.show();

        // If the actual size is 10 * 1.5 * 1.5 = 22.5, then we've encountered RT-37787!
        double expected = Font.getDefault().getSize() * 1.5;
        double actual = label.getFont().getSize();
        assertEquals(expected, actual, .1);

    }

    @Test public void test_RT_37787_with_inline_style() {

        label = new Label("test_RT_37787_with_inline_style");
        label.setStyle("-fx-font-size: 1.231em;");
        label.getStyleClass().clear();

        VBox root = new VBox();
        root.setStyle("-fx-font-size: 1.5em");
        root.getChildren().add(label);

        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        double expected = Font.getDefault().getSize() * 1.5 * 1.231;
        double actual = label.getFont().getSize();
        assertEquals(expected, actual, .1);

    }

}
