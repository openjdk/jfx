/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassDoesNotExist;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassExists;
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
import javafx.scene.control.ButtonBase;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
import org.junit.Test;

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

    @Test public void cannotSpecifyArmedViaCSS() {
        btn.setStyle("-fx-armed: true;");
        btn.applyCss();
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
