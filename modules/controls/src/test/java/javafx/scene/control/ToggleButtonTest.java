/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author srikalyc
 */
public class ToggleButtonTest {
    private ToggleGroup toggleGroup;
    private ToggleButton toggle;//Empty string
    private ToggleButton toggleWithText;//WithText
    private ToggleButton toggleWithGraphic;//With Graphic
    private Node node;
    private Toolkit tk;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        node = new Rectangle();
        toggleGroup = new ToggleGroup();
        toggle = new ToggleButton();
        toggleWithText = new ToggleButton("text");
        toggleWithGraphic = new ToggleButton("graphic", node);
    }
    
   
   
    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/
    
    @Test public void defaultConstructorShouldSetStyleClassTo_togglebutton() {
        assertStyleClassContains(toggle, "toggle-button");
    }
    
    @Test public void defaultOneArgConstructorShouldSetStyleClassTo_togglebutton() {
        assertStyleClassContains(toggleWithText, "toggle-button");
    }
    
    @Test public void defaultTwoArgConstructorShouldSetStyleClassTo_togglebutton() {
        assertStyleClassContains(toggleWithGraphic, "toggle-button");
    }
    
    @Test public void defaultConstructorTextGraphicCheck() {
        assertEquals(toggle.getText(), "");
        assertNull(toggle.getGraphic());
    }
    
    @Test public void defaultOneArgConstructorTextGraphicCheck() {
        assertEquals(toggleWithText.getText(), "text");
        assertNull(toggleWithText.getGraphic());
    }
    
    @Test public void defaultTwoArgConstructorTextGraphicCheck() {
        assertEquals(toggleWithGraphic.getText(), "graphic");
        assertSame(toggleWithGraphic.getGraphic(), node);
    }
    
    @Test public void defaultSelected() {
        assertFalse(toggle.isSelected());
    }

    @Test public void defaultAlignment() {
        assertSame(toggle.getAlignment(), Pos.CENTER);
    }
    
    @Test public void defaultMnemonicParsing() {
        assertTrue(toggle.isMnemonicParsing());
    }
    
    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void selectedPropertyHasBeanReference() {
        assertSame(toggle, toggle.selectedProperty().getBean());
    }

    @Test public void selectedPropertyHasName() {
        assertEquals("selected", toggle.selectedProperty().getName());
    }

    @Test public void toggleGroupPropertyHasBeanReference() {
        assertSame(toggle, toggle.toggleGroupProperty().getBean());
    }

    @Test public void toggleGroupPropertyHasName() {
        assertEquals("toggleGroup", toggle.toggleGroupProperty().getName());
    }

    /*********************************************************************
     * Check for Pseudo classes                                          *
     ********************************************************************/
    @Test public void settingSelectedSetsPseudoClass() {
        toggle.setSelected(true);
        assertPseudoClassExists(toggle, "selected");
    }

    @Test public void clearingSelectedClearsPseudoClass() {
        toggle.setSelected(true);
        toggle.setSelected(false);
        assertPseudoClassDoesNotExist(toggle, "selected");
    }


    /*********************************************************************
     * Miscellaneous Tests                                         *
     ********************************************************************/
    @Test public void setSelectedAndSeeValueIsReflectedInModel() {
        toggle.setSelected(true);
        assertTrue(toggle.selectedProperty().getValue());
    }
    
    @Test public void setSelectedAndSeeValue() {
        toggle.setSelected(false);
        assertFalse(toggle.isSelected());
    }
    
    @Test public void setToggleGroupAndSeeValueIsReflectedInModel() {
        toggle.setToggleGroup(toggleGroup);
        assertSame(toggle.toggleGroupProperty().getValue(), toggleGroup);
    }
    
    @Test public void setToggleGroupAndSeeValue() {
        toggle.setToggleGroup(toggleGroup);
        assertSame(toggle.getToggleGroup(), toggleGroup);
    }
    
    @Test public void fireAndCheckSelectionToggled() {
        toggle.fire();
        assertTrue(toggle.isSelected());
        toggle.fire();
        assertFalse(toggle.isSelected());
    }
    
    @Test public void fireAndCheckActionEventFired() {
        final Boolean []flag = new Boolean[1];
        flag[0] = false;
        toggle.addEventHandler(EventType.ROOT, event -> {
            if (event != null && event instanceof ActionEvent) {
                flag[0] = true;
            }
        });
        toggle.fire();
        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ToggleButtonTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertTrue("fire() doesnt emit ActionEvent!", flag[0]);
    }
    
}
