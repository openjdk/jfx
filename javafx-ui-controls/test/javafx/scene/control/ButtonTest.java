/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import static javafx.scene.control.ControlTestUtils.*;
import static org.junit.Assert.*;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * action (which can be bound, and can be null),
 * and that action is called when the button is fired.
 */
public class ButtonTest {
    private Button btn;
    
    @Before public void setup() {
        btn = new Button();
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
