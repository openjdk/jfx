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

package javafx.scene.control;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static javafx.scene.control.ControlTestUtils.*;
import static org.junit.Assert.*;

/**
 * action (which can be bound, and can be null),
 * and that action is called when the button is fired.
 */
public class ButtonTest {
    private Button btn;
    private Toolkit tk;
    private Scene scene;
    private Stage stage;
    private StackPane root;      
    
    @Before public void setup() {
        btn = new Button();
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        root = new StackPane();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);         
    }
    
    /*********************************************************************
     * Helper methods                                                    *
     ********************************************************************/
    private void show() {
        stage.show();
    }   
    
    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    
    @Test public void defaultConstructorShouldHaveNoGraphicAndEmptyString() {
        assertNull(btn.getGraphic());
        assertEquals("", btn.getText());
    }
    
    @Test public void oneArgConstructorShouldHaveNoGraphicAndSpecifiedString() {
        Button b2 = new Button(null);
        assertNull(b2.getGraphic());
        assertNull(b2.getText());
        
        b2 = new Button("");
        assertNull(b2.getGraphic());
        assertEquals("", b2.getText());
        
        b2 = new Button("Hello");
        assertNull(b2.getGraphic());
        assertEquals("Hello", b2.getText());
    }
    
    @Test public void twoArgConstructorShouldHaveSpecifiedGraphicAndSpecifiedString() {
        Button b2 = new Button(null, null);
        assertNull(b2.getGraphic());
        assertNull(b2.getText());

        Rectangle rect = new Rectangle();
        b2 = new Button("Hello", rect);
        assertSame(rect, b2.getGraphic());
        assertEquals("Hello", b2.getText());
    }

    @Test public void defaultConstructorShouldSetStyleClassTo_button() {
        assertStyleClassContains(btn, "button");
    }
    
    @Test public void oneArgConstructorShouldSetStyleClassTo_button() {
        Button b2 = new Button(null);
        assertStyleClassContains(b2, "button");
    }
    
    @Test public void twoArgConstructorShouldSetStyleClassTo_button() {
        Button b2 = new Button(null, null);
        assertStyleClassContains(b2, "button");
    }
    
    /*********************************************************************
     * Tests for the defaultButton state                                 *
     ********************************************************************/

    @Test public void defaultButtonIsFalseByDefault() {
        assertFalse(btn.isDefaultButton());
        assertFalse(btn.defaultButtonProperty().getValue());
    }

    @Test public void defaultButtonCanBeSet() {
        btn.setDefaultButton(true);
        assertTrue(btn.isDefaultButton());
    }

    @Test public void defaultButtonSetToNonDefaultValueIsReflectedInModel() {
        btn.setDefaultButton(true);
        assertTrue(btn.defaultButtonProperty().getValue());
    }

    @Test public void defaultButtonCanBeCleared() {
        btn.setDefaultButton(true);
        btn.setDefaultButton(false);
        assertFalse(btn.isDefaultButton());
    }

    @Test public void defaultButtonCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.defaultButtonProperty().bind(other);
        assertTrue(btn.isDefaultButton());
    }

    @Test public void settingDefaultButtonSetsPseudoClass() {
        btn.setDefaultButton(true);
        assertPseudoClassExists(btn, "default");
    }

    @Test public void clearingDefaultButtonClearsPseudoClass() {
        btn.setDefaultButton(true);
        btn.setDefaultButton(false);
        assertPseudoClassDoesNotExist(btn, "default");
    }

    @Test public void defaultButtonSetToTrueViaBindingSetsPseudoClass() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.defaultButtonProperty().bind(other);
        assertPseudoClassExists(btn, "default");
    }

    @Test public void defaultButtonSetToFalseViaBindingClearsPseudoClass() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.defaultButtonProperty().bind(other);
        other.setValue(false);
        assertPseudoClassDoesNotExist(btn, "default");
    }

    @Ignore("impl_cssSet API removed")
    @Test public void cannotSpecifyDefaultButtonViaCSS() {
//        btn.impl_cssSet("-fx-default-button", true);
        assertFalse(btn.isDefaultButton());
    }

    @Test public void defaultButtonPropertyHasBeanReference() {
        assertSame(btn, btn.defaultButtonProperty().getBean());
    }

    @Test public void defaultButtonPropertyHasName() {
        assertEquals("defaultButton", btn.defaultButtonProperty().getName());
    }
    
    @Test public void disabledDefaultButtonCannotGetInvoked_RT20929() {
        root.getChildren().add(btn);        
        
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                fail();
            }
        });
        
        btn.setDefaultButton(true);
        btn.setDisable(true);
        show();
        
        KeyEventFirer keyboard = new KeyEventFirer(btn);        
        keyboard.doKeyPress(KeyCode.ENTER);
   
        tk.firePulse();                
    }

    @Test public void defaultButtonCanBeInvokeAfterRemovingFromTheScene_RT22106() {
        btn.setDefaultButton(true);        
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                fail();
            }
        });
        root.getChildren().add(btn);
        show();
        
        root.getChildren().remove(btn);        
        
        KeyEventFirer keyboard = new KeyEventFirer(root);        
        keyboard.doKeyPress(KeyCode.ENTER);

        tk.firePulse();                      
    }
    
    /*********************************************************************
     * Tests for the cancelButton state                                 *
     ********************************************************************/

    @Test public void cancelButtonIsFalseByDefault() {
        assertFalse(btn.isCancelButton());
        assertFalse(btn.cancelButtonProperty().getValue());
    }

    @Test public void cancelButtonCanBeSet() {
        btn.setCancelButton(true);
        assertTrue(btn.isCancelButton());
    }

    @Test public void cancelButtonSetToNonDefaultValueIsReflectedInModel() {
        btn.setCancelButton(true);
        assertTrue(btn.cancelButtonProperty().getValue());
    }

    @Test public void cancelButtonCanBeCleared() {
        btn.setCancelButton(true);
        btn.setCancelButton(false);
        assertFalse(btn.isCancelButton());
    }

    @Test public void cancelButtonCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.cancelButtonProperty().bind(other);
        assertTrue(btn.isCancelButton());
    }

    @Test public void settingCancelButtonSetsPseudoClass() {
        btn.setCancelButton(true);
        assertPseudoClassExists(btn, "cancel");
    }

    @Test public void clearingCancelButtonClearsPseudoClass() {
        btn.setCancelButton(true);
        btn.setCancelButton(false);
        assertPseudoClassDoesNotExist(btn, "cancel");
    }

    @Test public void cancelButtonSetToTrueViaBindingSetsPseudoClass() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.cancelButtonProperty().bind(other);
        assertPseudoClassExists(btn, "cancel");
    }

    @Test public void cancelButtonSetToFalseViaBindingClearsPseudoClass() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.cancelButtonProperty().bind(other);
        other.setValue(false);
        assertPseudoClassDoesNotExist(btn, "cancel");
    }

    @Ignore("impl_cssSet API removed")
    @Test public void cannotSpecifyCancelButtonViaCSS() {
//        btn.impl_cssSet("-fx-cancel-button", true);
        assertFalse(btn.isCancelButton());
    }

    @Test public void cancelButtonPropertyHasBeanReference() {
        assertSame(btn, btn.cancelButtonProperty().getBean());
    }

    @Test public void cancelButtonPropertyHasName() {
        assertEquals("cancelButton", btn.cancelButtonProperty().getName());
    }

    @Test public void cancelButtonCanBeInvokeAfterRemovingFromTheScene_RT22106() {
        btn.setCancelButton(true);        
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                fail();
            }
        });
        root.getChildren().add(btn);
        show();
        
        root.getChildren().remove(btn);        
        
        KeyEventFirer keyboard = new KeyEventFirer(root);        
        keyboard.doKeyPress(KeyCode.ESCAPE);

        tk.firePulse();                      
    }


    @Test public void conextMenuShouldntShowOnAction() {
        final MouseEventGenerator generator = new MouseEventGenerator();

        ContextMenu popupMenu = new ContextMenu();
        MenuItem item1 = new MenuItem("_About");
        popupMenu.getItems().add(item1);
        popupMenu.setOnShown(new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent w) {
                fail();
            }
        });

        btn.setContextMenu(popupMenu);
        btn.setDefaultButton(true);

        root.getChildren().add(btn);
        show();

        double xval = (btn.localToScene(btn.getLayoutBounds())).getMinX();
        double yval = (btn.localToScene(btn.getLayoutBounds())).getMinY();


        /*
        ** none of these should cause the context menu to appear,
        ** so fire them all, and see if anything happens.
        */
        KeyEventFirer keyboard = new KeyEventFirer(btn);        
        keyboard.doKeyPress(KeyCode.ENTER);

        btn.fireEvent(new ActionEvent());
        btn.fire();
        scene.impl_processMouseEvent(
            generator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+10, yval+10));
        scene.impl_processMouseEvent(
            generator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+10, yval+10));
        scene.impl_processMouseEvent(
            generator.generateMouseEvent(MouseEvent.MOUSE_CLICKED, xval+10, yval+10));
       
        tk.firePulse();                      
    }

    static class MyButton extends Button {
        MyButton(String text) {
            super(text);
        }
        
        void setHoverPseudoclassState(boolean b) {
            setHover(b);
        }
    }    
    
    List<Stop> getStops(Button button) {
        Skin skin = button.getSkin();
        Region region = (Region)skin.getNode();
        List<BackgroundFill> fills = region.getBackground().getFills();
        BackgroundFill top = fills.get(fills.size()-1);
        LinearGradient topFill = (LinearGradient)top.getFill();
        return topFill.getStops();
    }

    @Test
    public void testRT_23207() {
        
        HBox hBox = new HBox();
        hBox.setSpacing(5);
        hBox.setTranslateY(30);
        MyButton red = new MyButton("Red");
        red.setStyle("-fx-base: red;");
        MyButton green = new MyButton("Green");
        green.setStyle("-fx-base: green;");
        hBox.getChildren().add(red);
        hBox.getChildren().add(green);
                
        Scene scene = new Scene(hBox, 500, 300);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        
        Toolkit.getToolkit().firePulse();
        
        List<Stop> redStops0 = getStops(red);
        List<Stop> greenStops0 = getStops(green);
        
        red.setHoverPseudoclassState(true);
        
        Toolkit.getToolkit().firePulse();

        List<Stop> redStops1 = getStops(red);
        List<Stop> greenStops1 = getStops(green);
                
        red.setHoverPseudoclassState(false);
        green.setHoverPseudoclassState(true);
        
        Toolkit.getToolkit().firePulse();
        
        List<Stop> redStops2 = getStops(red);
        List<Stop> greenStops2 = getStops(green);

        green.setHoverPseudoclassState(false);
        
        Toolkit.getToolkit().firePulse();

        List<Stop> redStops3 = getStops(red);
        List<Stop> greenStops3 = getStops(green);
System.err.println("0 = " + redStops0.toString());        
System.err.println("1 = " + redStops1.toString());        
System.err.println("2 = " + redStops2.toString());        
System.err.println("3 = " + redStops3.toString());        
        // did red change color after red hover=true?
        assertFalse(redStops0.equals(redStops1));
        // did red change back to original color after green hover=true?
        assertTrue(redStops0.equals(redStops2));
        // did red stay original color after green hover=false?
        assertTrue(redStops0.equals(redStops3));
        // did green stay green after red hover=true?
        assertTrue(greenStops0.equals(greenStops1));
        // did green change after green hover=true?
        assertFalse(greenStops0.equals(greenStops2));
        // did green revert to original after green hover=false?
        // This is the acid test. If this fails, then RT-23207 is present.
        assertTrue(greenStops0.equals(greenStops3));
        
    }    

    
//  private Button button1;
//  private Button button2;
//
//  @Override protected Node createNodeToTest() {
//    button1 = createButton("Button1");
//    button2 = createButton("Button2");
//    button2.setLayoutX(0);
//    button2.setLayoutY(110);
//    Group group = new Group();
//    group.getChildren().addAll(button1, button2);
//    group.setAutoSizeChildren(false);
//    return group;
//  }
//
//  private static Button createButton(String text) {
//    Button button = new Button(text);
//    button.resize(100, 100);
//    return button;
//  }
//
//  @Test public void pressEventsShouldLeadToFocusGained() {
//    mouse().positionAtCenterOf(button2);
//    mouse().leftPress();
//    assertTrue(button2.isFocused());
//  }
//
//  @Test public void pressEventsShouldLeadToFocusGained_efficiently() {
//    executeInUIThread(new Runnable() {
//      @Override public void run() {
//        mouse().positionAtCenterOf(button2);
//        mouse().leftPress();
//      }
//    });
//    assertTrue(button2.isFocused());
//  }
}
