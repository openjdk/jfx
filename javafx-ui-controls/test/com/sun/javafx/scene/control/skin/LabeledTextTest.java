/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.scene.control.skin;

import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.Stylesheet;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
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
        label.impl_processCSS(false);
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
        label.impl_processCSS(false);
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
        label.impl_processCSS(false);
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
        label.impl_processCSS(false);
        assert(label.isUnderline() == true);
        assert(labeledText.isUnderline() == true);
    
    }

    @Test
    public void testLabeledBlendModeStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-blend-mode: color-burn;");
        label.impl_processCSS(false);
        assertEquals(BlendMode.COLOR_BURN,label.getBlendMode());
        assertFalse(BlendMode.COLOR_BURN.equals(labeledText.getBlendMode())); 
    }
    
    @Test
    public void testLabeledCursorStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-cursor: crosshair;");
        label.impl_processCSS(false);
        assertEquals(Cursor.CROSSHAIR,label.getCursor());
        assertFalse(Cursor.CROSSHAIR.equals(labeledText.getCursor()));
    }
    
    @Test
    public void testLabeledEffectStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-effect: dropshadow(one-pass-box, red, 64, .5, 2, 3);");
        label.impl_processCSS(false);
        assertNotNull(label.getEffect());
        assertNull(labeledText.getEffect()); 
    }
    
    @Test
    public void testLabeledFocusTraversableStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-focus-traversable: true;");
        label.impl_processCSS(false);
        assert(label.focusTraversableProperty().get() == true);   
        assert(labeledText.focusTraversableProperty().get() == false);   
    }
    
    @Test
    public void testLabeledOpacityStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-opacity: .5;");
        label.impl_processCSS(false);
        assertEquals(label.getOpacity(), .5, .0000001);   
        assertEquals(labeledText.getOpacity(), 1, .0000001);   
    }

    @Test
    public void testLabeledRotateStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-rotate: 180;");
        label.impl_processCSS(false);
        assertEquals(label.getRotate(), 180, .0000001);
        assertEquals(labeledText.getRotate(), 0, .0000001);   
    }
    
    @Test
    public void testLabeledScaleXStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-scale-x: .5;");
        label.impl_processCSS(false);
        assertEquals(label.getScaleX(), .5, .0000001);
        assertEquals(labeledText.getScaleX(), 1, .0000001);   
    }
    
    @Test
    public void testLabeledScaleYStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-scale-y: .5;");
        label.impl_processCSS(false);
        assertEquals(label.getScaleY(), .5, .0000001);
        assertEquals(labeledText.getScaleY(), 1, .0000001);   
    }

    @Test
    public void testLabeledScaleZStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-scale-z: .5;");
        label.impl_processCSS(false);
        assertEquals(label.getScaleZ(), .5, .0000001);
        assertEquals(labeledText.getScaleZ(), 1, .0000001);   
    }
    
    @Test
    public void testLabeledTranslateXStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-translate-x: .5;");
        label.impl_processCSS(false);
        assertEquals(label.getTranslateX(), .5, .0000001);
        assertEquals(labeledText.getTranslateX(), 0, .0000001);   
    }
    
    @Test
    public void testLabeledTranslateYStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-translate-y: .5;");
        label.impl_processCSS(false);
        assertEquals(label.getTranslateY(), .5, .0000001);
        assertEquals(labeledText.getTranslateY(), 0, .0000001);   
    }
    
    @Test
    public void testLabeledTranslateZStyleDoesNotAffectLabeledText() {
        label.setStyle("-fx-translate-z: .5;");
        label.impl_processCSS(false);
        assertEquals(label.getTranslateZ(), .5, .0000001);
        assertEquals(labeledText.getTranslateZ(), 0, .0000001);   
    }
    
    @Test
    public void testLabeledVisibilityStyleDoesNotAffectLabeledText() {
        label.setStyle("visibility: false;");
        label.impl_processCSS(false);
        assert(label.visibleProperty().get() == false);   
        assert(labeledText.visibleProperty().get() == true);   
    }
    
    private static StyleableProperty getStyleableProperty(String prop) {
        for (StyleableProperty styleable : LabeledText.impl_CSS_STYLEABLES()) {
            if (styleable.getProperty().equals(prop)) return styleable;
        }
        return null;
    }
    
    @Test
    public void testLabeledTextFillIsSettableByCss() {
        
        StyleableProperty sp = getStyleableProperty("-fx-fill");
        assertTrue(sp.isSettable(labeledText));
    }
    
    @Test
    public void testCanStyleFillOnLabeledText() {
        assertEquals(Color.GREEN, labeledText.getFill());    
    }

    
    @Test
    public void testLabeledTextTextAlignmentIsSettableByCss() {
        
        StyleableProperty sp = getStyleableProperty("-fx-text-alignment");
        assertTrue(sp.isSettable(labeledText));
    }
    
    @Test
    public void testCanStyleTextAlignmentOnLabeledText() {
        assertEquals(TextAlignment.RIGHT, labeledText.getTextAlignment());
    }
    
    
    @Test
    public void testLabeledTextFontIsSettableByCss() {
        
        StyleableProperty sp = getStyleableProperty("-fx-font");
        assertTrue(sp.isSettable(labeledText));
    }
    
    @Test
    public void testCanStyleFontOnLabeledText() {
        assertEquals(Font.font("Amble", 10), labeledText.getFont());
    }
    
    
    @Test
    public void testLabeledTextUnderlineIsSettableByCss() {
        
        StyleableProperty sp = getStyleableProperty("-fx-underline");
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
    
}
