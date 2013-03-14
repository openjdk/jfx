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

import javafx.css.StyleConverter;
import javafx.css.CssMetaData;
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
    
    
    @Test public void test_RT_21357() {
        
        final Labeled labeled = new Label("label");
        final LabeledImpl labeledImpl = new LabeledImpl(labeled);
        
        URL url = LabeledSkinBase.class.getResource("caspian/center-btn.png");
        Image img = new Image(url.toExternalForm());
        assertNotNull(img);
        
        ImageView iView = new ImageView(img);
        labeled.setGraphic(iView);  
        
        assertEquals(labeled.getGraphic(), labeledImpl.getGraphic());
        assertNotNull(labeled.getGraphic());
    }

    @Test public void test_RT_21617() {
        
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
