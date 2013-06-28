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

package javafx.scene.control;

/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import com.sun.javafx.scene.control.infrastructure.MouseEventGenerator;
import com.sun.javafx.scene.control.skin.ColorPalette;
import com.sun.javafx.scene.control.skin.ColorPickerPaletteRetriever;
import com.sun.javafx.scene.control.skin.ColorPickerSkin;
import com.sun.javafx.tk.Toolkit;

public class ColorPickerTest {
    private ColorPicker colorPicker;
    private Toolkit tk;
    private Scene scene;
    private Stage stage;
    private VBox root;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();
        colorPicker = new ColorPicker();
        Scene scene = new Scene(new VBox(20), 800, 600);
        VBox box = (VBox)scene.getRoot();
        box.getChildren().add(colorPicker);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
        tk.firePulse();
    }

    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void noArgConstructorSetsTheStyleClass() {
        assertStyleClassContains(colorPicker, "color-picker");
    }
    
    @Test public void noArgConstructor_valueIsNonNull() {
        assertNotNull(colorPicker.getValue());
    }
    
    @Test public void noArgConstructor_showingIsFalse() {
        assertFalse(colorPicker.isShowing());
    }

    @Test public void singleArgConstructorSetsTheStyleClass() {
        final ColorPicker cp = new ColorPicker(Color.WHITE);
        assertStyleClassContains(cp, "color-picker");
    }
    
    @Test public void singleArgConstructor_showingIsFalse() {
        final ColorPicker cp = new ColorPicker(Color.WHITE);
        assertFalse(cp.isShowing());
    }

    @Test public void singleArgConstructor_valueIsNonNull() {
        final ColorPicker cp = new ColorPicker(Color.WHITE);
        assertNotNull(cp.getValue());
    }
    
    @Test public void defaultActionHandlerIsNotDefined() {
        assertNull(colorPicker.getOnAction());
    }
    
    @Test public void testGetSetValue() {
        final ColorPicker cp = new ColorPicker(Color.WHITE);
        cp.setValue(Color.PINK);
        assertEquals(cp.getValue(), Color.PINK);
    }
    
    @Test public void testCustomColors() {
        final ColorPicker cp = new ColorPicker(Color.WHITE);
        cp.getCustomColors().addAll(new Color(0.83, .55, .214, 1), new Color(.811, .222, .621, 1));
        assertEquals(cp.getCustomColors().get(0),  new Color(0.83, .55, .214, 1));
        assertEquals(cp.getCustomColors().get(1),  new Color(.811, .222, .621, 1));
    }
    
    @Test public void ensureCanSetValueToNonNullColorAndBackAgain() {
        colorPicker.setValue(Color.PINK);
        assertEquals(Color.PINK, colorPicker.getValue());
        colorPicker.setValue(null);
        assertNull(colorPicker.getValue());
    }
    
    @Test public void ensureCanToggleShowing() {
        colorPicker.show();
        assertTrue(colorPicker.isShowing());
        colorPicker.hide();
        assertFalse(colorPicker.isShowing());
    }
    
    @Test public void ensureCanNotToggleShowingWhenDisabled() {
        colorPicker.setDisable(true);
        colorPicker.show();
        assertFalse(colorPicker.isShowing());
        colorPicker.setDisable(false);
        colorPicker.show();
        assertTrue(colorPicker.isShowing());
    }
     
    @Test public void ensureCanSetOnAction() {
        EventHandler<ActionEvent> onAction = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) { }
        };
        colorPicker.setOnAction(onAction);
        assertEquals(onAction, colorPicker.getOnAction());
    }
    
    @Test public void ensureOnActionPropertyReferencesBean() {
        assertEquals(colorPicker, colorPicker.onActionProperty().getBean());
    }
    
    @Test public void ensureCanSelectColorFromPalette() {
         final MouseEventGenerator generator = new MouseEventGenerator();
         ColorPickerSkin skin = (ColorPickerSkin)colorPicker.getSkin();
         assertTrue(skin != null);
         ColorPalette colorPalette = ColorPickerPaletteRetriever.getColorPalette(colorPicker);
         colorPicker.show();
         tk.firePulse();
         assertTrue(colorPicker.isShowing());
         GridPane grid = colorPalette.getColorGrid();
         double xval = grid.getBoundsInLocal().getMinX();
         double yval = grid.getBoundsInLocal().getMinY();
        
        Scene paletteScene = ColorPickerPaletteRetriever.getPopup(colorPicker).getScene();
        paletteScene.getWindow().requestFocus();
        
        paletteScene.impl_processMouseEvent(
                generator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+85, yval+40));
        
        paletteScene.impl_processMouseEvent(
                generator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+85, yval+40));
        tk.firePulse();
        
        assertEquals(colorPicker.getValue().toString(), "0x330033ff");
    }
    
    @Test public void testEscapeClosesCustomColorDialog() {
//        final MouseEventGenerator generator = new MouseEventGenerator();
        ColorPickerSkin skin = (ColorPickerSkin)colorPicker.getSkin();
        assertTrue(skin != null);
        ColorPalette colorPalette = ColorPickerPaletteRetriever.getColorPalette(colorPicker);
        colorPicker.show();
        tk.firePulse();
        assertTrue(colorPicker.isShowing());
        Hyperlink link = ColorPickerPaletteRetriever.getCustomColorLink(colorPalette);
         
        Scene paletteScene = ColorPickerPaletteRetriever.getPopup(colorPicker).getScene();
        paletteScene.getWindow().requestFocus();
        
        //Click on CustomColor hyperlink to show the custom color dialog.
        Hyperlink hyperlink = ColorPickerPaletteRetriever.getCustomColorLink(colorPalette);
        MouseEventFirer.fireMousePressAndRelease(hyperlink);
        
        Stage dialog = ColorPickerPaletteRetriever.getCustomColorDialog(colorPalette);
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
        
        dialog.requestFocus();
        tk.firePulse();
        
        // fire KeyEvent (Escape) on custom color dialog to close it
        KeyEventFirer keyboard = new KeyEventFirer(dialog);
        keyboard.doKeyPress(KeyCode.ESCAPE);
        tk.firePulse();   
        assertTrue(!dialog.isShowing());
    }
}
