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

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author srikalyc
 */
public class RadioMenuItemTest {
    private ToggleGroup toggleGroup;
    private RadioMenuItem radioMenuItem, rmi;//Empty string
    private RadioMenuItem radioMenuItemTwoArg;
    private Node node;
    private Toolkit tk;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        node = new Rectangle();
        toggleGroup = new ToggleGroup();
        radioMenuItem = rmi = new RadioMenuItem("one");
        radioMenuItemTwoArg = new RadioMenuItem("two", node);
    }
    
   
   
    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/
    
    @Test public void oneArgConstructorShouldSetStyleClassTo_checkmenuitem() {
        assertStyleClassContains(radioMenuItem, "radio-menu-item");
    }
    @Test public void twoArgConstructorShouldSetStyleClassTo_checkmenuitem() {
        assertStyleClassContains(radioMenuItemTwoArg, "radio-menu-item");
    }
    
    @Test public void defaultTxtNotNull() {
        assertNotNull(radioMenuItem.getText());
        assertEquals(radioMenuItem.getText(), "one");
    }

    @Test public void twoArgConstructorGraphicNotNull() {
        assertNotNull(radioMenuItemTwoArg.getGraphic());
        assertSame(radioMenuItemTwoArg.getGraphic(), node);
    }

    @Test public void defaultSelected() {
        assertFalse(radioMenuItem.isSelected());
    }

    
    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/
    
    @Test public void checkSelectedPropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        radioMenuItem.selectedProperty().bind(objPr);
        assertTrue("selectedProperty cannot be bound", radioMenuItem.selectedProperty().getValue());
        objPr.setValue(false);
        assertFalse("selectedProperty cannot be bound", radioMenuItem.selectedProperty().getValue());
    }
    
    @Test public void checkToggleGroupPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<ToggleGroup>(null);
        radioMenuItem.toggleGroupProperty().bind(objPr);
        assertNull("toggleGroupProperty cannot be bound", radioMenuItem.toggleGroupProperty().getValue());
        objPr.setValue(toggleGroup);
        assertSame("toggleGroupProperty cannot be bound", radioMenuItem.toggleGroupProperty().getValue(), toggleGroup);
    }
    
    @Test public void selectedPropertyHasBeanReference() {
        assertSame(radioMenuItem, radioMenuItem.selectedProperty().getBean());
    }

    @Test public void selectedPropertyHasName() {
        assertEquals("selected", radioMenuItem.selectedProperty().getName());
    }

    @Test public void toggleGroupPropertyHasBeanReference() {
        assertSame(radioMenuItem, radioMenuItem.toggleGroupProperty().getBean());
    }

    @Test public void toggleGroupPropertyHasName() {
        assertEquals("toggleGroup", radioMenuItem.toggleGroupProperty().getName());
    }

    /*********************************************************************
     * Miscellaneous Tests                                         *
     ********************************************************************/
    @Test public void setSelectedAndSeeValueIsReflectedInModel() {
        radioMenuItem.setSelected(true);
        assertTrue(radioMenuItem.selectedProperty().getValue());
    }
    
    @Test public void setSelectedAndSeeValue() {
        radioMenuItem.setSelected(false);
        assertFalse(radioMenuItem.isSelected());
    }
    
    @Test public void setSelectedTrueAndSeeIfStyleSelectedExists() {
        radioMenuItem.setSelected(true);
        assertTrue(radioMenuItem.getStyleClass().contains("selected"));
    }
    
    @Test public void setSelectedFalseAndSeeIfStyleSelectedDoesNotExists() {
        radioMenuItem.setSelected(false);
        assertFalse(radioMenuItem.getStyleClass().contains("selected"));
    }
    
    @Test public void setToggleGroupAndSeeValueIsReflectedInModel() {
        radioMenuItem.setToggleGroup(toggleGroup);
        assertSame(radioMenuItem.toggleGroupProperty().getValue(), toggleGroup);
    }
    
    @Test public void setToggleGroupAndSeeValue() {
        radioMenuItem.setToggleGroup(toggleGroup);
        assertSame(radioMenuItem.getToggleGroup(), toggleGroup);
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic1() {
        RadioMenuItem rmi2 = new RadioMenuItem(null);
        assertNull(rmi2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic2() {
        RadioMenuItem rmi2 = new RadioMenuItem("");
        assertNull(rmi2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic3() {
        assertNull(rmi.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString1() {
        RadioMenuItem rmi2 = new RadioMenuItem(null);
        assertNull(rmi2.getText());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString2() {
        RadioMenuItem rmi2 = new RadioMenuItem("");
        assertEquals("", rmi2.getText());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString3() {
        assertEquals("one", rmi.getText());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedGraphic1() {
        RadioMenuItem rmi2 = new RadioMenuItem(null, null);
        assertNull(rmi2.getGraphic());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedGraphic2() {
        Rectangle rect = new Rectangle();
        RadioMenuItem rmi2 = new RadioMenuItem("Hello", rect);
        assertEquals(rect, rmi2.getGraphic());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedString1() {
        RadioMenuItem rmi2 = new RadioMenuItem(null, null);
        assertNull(rmi2.getText());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedString2() {
        Rectangle rect = new Rectangle();
        RadioMenuItem rmi2 = new RadioMenuItem("Hello", rect);
        assertEquals("Hello", rmi2.getText());
    }

    @Test public void defaultToggleGroup() {
        assertNull(rmi.getToggleGroup());
    }

    @Test public void setNullToggleGroup() {
        rmi.setToggleGroup(null);
        assertNull(rmi.getToggleGroup());
    }

    @Test public void setSpecifiedToggleGroup() {
        ToggleGroup tg = new ToggleGroup();
        rmi.setToggleGroup(tg);
        assertSame(tg, rmi.getToggleGroup());
    }

    @Test public void getUnspecifiedToggleGroupProperty1() {
        assertNotNull(rmi.toggleGroupProperty());
    }

    @Test public void getUnspecifiedToggleGroupProperty2() {
        RadioMenuItem rmi2 = new RadioMenuItem("", null);
        assertNotNull(rmi2.toggleGroupProperty());
    }

    // calling toggleGroupProperty does not ensure the value of toggleGroup
    // to be non null
    @Ignore
    @Test public void unsetToggleGroupButNotNull() {
        rmi.toggleGroupProperty();
        assertNotNull(rmi.getToggleGroup());
    }

    @Test public void toggleGroupCanBeBound() {
        ToggleGroup tg = new ToggleGroup();
        SimpleObjectProperty<ToggleGroup> other = new SimpleObjectProperty<ToggleGroup>(rmi, "toggleGroup", tg);
        rmi.toggleGroupProperty().bind(other);
        assertSame(tg, rmi.getToggleGroup());
    }

     @Test public void getUnspecifiedSelected() {
        assertEquals(false, rmi.isSelected());
    }

    @Test public void setTrueSelected() {
        rmi.setSelected(true);
        assertTrue(rmi.isSelected());
    }

    @Test public void setFalseSelected() {
        rmi.setSelected(false);
        assertFalse(rmi.isSelected());
    }

    @Test public void selectedNotSetButNotNull() {
        rmi.selectedProperty();
        assertNotNull(rmi.isSelected());
    }

    @Test public void selectedCanBeBound1() {
        SimpleBooleanProperty other = new SimpleBooleanProperty(rmi, "selected", false);
        rmi.selectedProperty().bind(other);
        assertEquals(other.get(), rmi.isSelected());
    }

    @Test public void selectedCanBeBound2() {
        SimpleBooleanProperty other = new SimpleBooleanProperty(rmi, "selected", true);
        rmi.selectedProperty().bind(other);
        assertEquals(other.get(), rmi.isSelected());
    }
    
    
}
