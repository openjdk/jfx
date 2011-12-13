/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import static javafx.scene.control.ControlTestUtils.assertPseudoClassDoesNotExist;
import static javafx.scene.control.ControlTestUtils.assertPseudoClassExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

/**
 */
public class ButtonBaseTest {
    private ButtonBase btn;
    
    @Before public void setup() {
        btn = new ButtonBaseMock();
    }
    
    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    
    @Test public void defaultConstructorShouldHaveNoGraphicAndEmptyString() {
        assertNull(btn.getGraphic());
        assertEquals("", btn.getText());
    }
    
    @Test public void oneArgConstructorShouldHaveNoGraphicAndSpecifiedString() {
        ButtonBase b2 = new ButtonBaseMock(null);
        assertNull(b2.getGraphic());
        assertNull(b2.getText());
        
        b2 = new ButtonBaseMock("");
        assertNull(b2.getGraphic());
        assertEquals("", b2.getText());
        
        b2 = new ButtonBaseMock("Hello");
        assertNull(b2.getGraphic());
        assertEquals("Hello", b2.getText());
    }
    
    @Test public void twoArgConstructorShouldHaveSpecifiedGraphicAndSpecifiedString() {
        ButtonBase b2 = new ButtonBaseMock(null, null);
        assertNull(b2.getGraphic());
        assertNull(b2.getText());

        Rectangle rect = new Rectangle();
        b2 = new ButtonBaseMock("Hello", rect);
        assertSame(rect, b2.getGraphic());
        assertEquals("Hello", b2.getText());
    }

    /*********************************************************************
     * Tests for the armed state                                         *
     ********************************************************************/
    
    @Test public void armedIsFalseByDefault() {
        assertFalse(btn.isArmed());
    }
    
    @Test public void armedCanBeSet() {
        btn.arm();
        assertTrue(btn.isArmed());
    }
    
    @Test public void armedCanBeCleared() {
        btn.arm();
        btn.disarm();
        assertFalse(btn.isArmed());
    }
    
    @Ignore("impl_cssSet API removed")
    @Test public void cannotSpecifyArmedViaCSS() {
//        btn.impl_cssSet("-fx-armed", true);
        assertFalse(btn.isArmed());
    }
    
    @Test public void settingArmedSetsPseudoClass() {
        btn.arm();
        assertPseudoClassExists(btn, "armed");
    }
    
    @Test public void clearingArmedClearsPseudoClass() {
        btn.arm();
        btn.disarm();
        assertPseudoClassDoesNotExist(btn, "armed");
    }
    
    @Test public void armedPropertyHasBeanReference() {
        assertSame(btn, btn.armedProperty().getBean());
    }

    @Test public void armedPropertyHasName() {
        assertEquals("armed", btn.armedProperty().getName());
    }

    /*********************************************************************
     * Tests for the action state                                        *
     ********************************************************************/

    @Test public void onActionIsNullByDefault() {
        assertNull(btn.getOnAction());
        assertNull(btn.onActionProperty().getValue());
    }

    @Test public void onActionCanBeSet() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        btn.setOnAction(handler);
        assertEquals(handler, btn.getOnAction());
    }

    @Test public void onActionSetToNonDefaultValueIsReflectedInModel() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        btn.setOnAction(handler);
        assertEquals(handler, btn.onActionProperty().getValue());
    }

    @Test public void onActionCanBeCleared() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        btn.setOnAction(handler);
        btn.setOnAction(null);
        assertNull(btn.getOnAction());
    }

    @Test public void onActionCanBeBound() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        ObjectProperty<EventHandler<ActionEvent>> other = new SimpleObjectProperty<EventHandler<ActionEvent>>(handler);
        btn.onActionProperty().bind(other);
        assertEquals(handler, btn.getOnAction());
    }

    @Test public void onActionPropertyHasBeanReference() {
        assertSame(btn, btn.onActionProperty().getBean());
    }

    @Test public void onActionPropertyHasName() {
        assertEquals("onAction", btn.onActionProperty().getName());
    }

    public static final class EventHandlerStub implements EventHandler<ActionEvent> {
        boolean called = false;
        @Override public void handle(ActionEvent event) {
            called = true;
        }
    };

    public static final class ButtonBaseMock extends ButtonBase {
        public ButtonBaseMock() { super(); }
        public ButtonBaseMock(String text) { super(text); }
        public ButtonBaseMock(String text, Node graphic) { super(text, graphic); }
        @Override public void fire() { }
    }
}
