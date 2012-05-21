/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.scene.control.skin;

import com.sun.javafx.css.StyleConverter;
import com.sun.javafx.css.StyleableProperty;
import java.net.URL;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.beans.value.WritableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.OverrunStyle;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjustBuilder;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
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

}
