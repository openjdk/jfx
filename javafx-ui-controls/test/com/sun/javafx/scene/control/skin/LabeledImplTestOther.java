/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.scene.control.skin;

import com.sun.javafx.css.StyleConverter;
import com.sun.javafx.css.CssMetaData;
import java.net.URL;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.beans.value.WritableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjustBuilder;
import javafx.scene.effect.DropShadowBuilder;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javax.swing.GroupLayout;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

public class LabeledImplTestOther {
    
    
    @Test 
    public void test_RT_21357() {
        
        final Labeled labeled = new Label("label");
        final LabeledImpl labeledImpl = new LabeledImpl(labeled);
        
        URL url = SkinBase.class.getResource("caspian/center-btn.png");
        Image img = new Image(url.toExternalForm());
        ImageView iView = new ImageView(img);
        labeled.setGraphic(iView);  
        
        assertEquals(labeled.getGraphic(), labeledImpl.getGraphic());
    }

    @Test 
    public void test_RT_21617() {
        
        MenuButton mb = new MenuButton();
        mb.setText("SomeText"); 
        MenuButtonSkin mbs = new MenuButtonSkin(mb);
        mb.setSkin(mbs);
         
        mb.setTranslateX(100);mb.setTranslateY(100); 
        
        Scene scene = new Scene(mb, 300, 300); 
        scene.getStylesheets().add(LabeledImplTestOther.class.getResource("LabeledImplTest.css").toExternalForm());
        Stage stage = new Stage();
        stage.setScene(scene); 
        stage.show(); 
        
        
        LabeledImpl labeledImpl = (LabeledImpl)mb.lookup(".label");
        assertNotNull(labeledImpl);
        // LabeledImpl should not mirror the translateX/Y of the MenuButton
        assertEquals(100, mb.getTranslateX(), 0.00001);
        assertEquals(0, labeledImpl.getTranslateX(), 0.00001);
        assertEquals(100, mb.getTranslateY(), 0.00001);
        assertEquals(0, labeledImpl.getTranslateY(), 0.00001);
        // opacity set to 50% in LabeledImplTest.css
        assertEquals(1, mb.getOpacity(), 0.00001);
        assertEquals(.5, labeledImpl.getOpacity(), 0.00001);
    }
    
}
