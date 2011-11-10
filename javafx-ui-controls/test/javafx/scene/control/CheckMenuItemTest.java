/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import static javafx.scene.control.ControlTestUtils.*;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author srikalyc
 */
public class CheckMenuItemTest {
    private CheckMenuItem checkMenuItem, cmi;//Empty string
    private CheckMenuItem checkMenuItemOneArg;//Empty graphic
    private CheckMenuItem checkMenuItemTwoArg;
    private Node node;
    private Toolkit tk;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        node = new Rectangle();
        checkMenuItem = cmi = new CheckMenuItem();
        checkMenuItemOneArg = new CheckMenuItem("one");
        checkMenuItemTwoArg = new CheckMenuItem("two", node);
    }
    
   
   
    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/
    
    @Test public void defaultConstructorShouldSetStyleClassTo_checkmenuitem() {
        assertStyleClassContains(checkMenuItem, "check-menu-item");
    }
    @Test public void oneArgConstructorShouldSetStyleClassTo_checkmenuitem() {
        assertStyleClassContains(checkMenuItemOneArg, "check-menu-item");
    }
    @Test public void twoArgConstructorShouldSetStyleClassTo_checkmenuitem() {
        assertStyleClassContains(checkMenuItemTwoArg, "check-menu-item");
    }
    
    @Test public void defaultTxtNull() {
        assertNull(checkMenuItem.getText());
    }

    @Test public void oneArgConstructorTxtNotNull() {
        assertNotNull(checkMenuItemOneArg.getText());
        assertEquals(checkMenuItemOneArg.getText(), "one");
    }

    @Test public void twoArgConstructorGraphicNotNull() {
        assertNotNull(checkMenuItemTwoArg.getGraphic());
        assertSame(checkMenuItemTwoArg.getGraphic(), node);
    }

    @Test public void defaultSelected() {
        assertFalse(checkMenuItem.isSelected());
    }

    
    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/
    
    @Test public void selectedPropertyHasBeanReference() {
        assertSame(checkMenuItem, checkMenuItem.selectedProperty().getBean());
    }

    @Test public void selectedPropertyHasName() {
        assertEquals("selected", checkMenuItem.selectedProperty().getName());
    }

    /*********************************************************************
     * Miscellaneous Tests                                         *
     ********************************************************************/
    @Test public void setSelectedAndSeeValueIsReflectedInModel() {
        checkMenuItem.setSelected(true);
        assertTrue(checkMenuItem.selectedProperty().getValue());
    }
    
    @Test public void setSelectedAndSeeValue() {
        checkMenuItem.setSelected(false);
        assertFalse(checkMenuItem.isSelected());
    }
    
    @Test public void setSelectedTrueAndSeeIfStyleSelectedExists() {
        checkMenuItem.setSelected(true);
        assertTrue(checkMenuItem.getStyleClass().contains("selected"));
    }
    
    @Test public void setSelectedFalseAndSeeIfStyleSelectedDoesNotExists() {
        checkMenuItem.setSelected(false);
        assertFalse(checkMenuItem.getStyleClass().contains("selected"));
    }
    
    @Test public void defaultConstructorShouldHaveNoGraphic() {
        assertNull(cmi.getGraphic());
    }

    @Test public void defaultConstructorShouldHaveNullString() {
        assertNull(cmi.getText());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic1() {
        CheckMenuItem cmi2 = new CheckMenuItem(null);
        assertNull(cmi2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic2() {
        CheckMenuItem cmi2 = new CheckMenuItem("");
        assertNull(cmi2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic3() {
        CheckMenuItem cmi2 = new CheckMenuItem("Hello");
        assertNull(cmi2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString1() {
        CheckMenuItem cmi2 = new CheckMenuItem(null);
        assertNull(cmi2.getText());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString2() {
        CheckMenuItem cmi2 = new CheckMenuItem("");
        assertEquals("", cmi2.getText());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString3() {
        CheckMenuItem cmi2 = new CheckMenuItem("Hello");
        assertEquals("Hello", cmi2.getText());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedGraphic1() {
        CheckMenuItem cmi2 = new CheckMenuItem(null, null);
        assertNull(cmi2.getGraphic());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedGraphic2() {
        Rectangle rect = new Rectangle();
        CheckMenuItem cmi2 = new CheckMenuItem("Hello", rect);
        assertSame(rect, cmi2.getGraphic());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedString1() {
        CheckMenuItem cmi2 = new CheckMenuItem(null, null);
        assertNull(cmi2.getText());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedString2() {
        Rectangle rect = new Rectangle();
        CheckMenuItem cmi2 = new CheckMenuItem("Hello", rect);
        assertEquals("Hello", cmi2.getText());
    }

    @Test public void getUnspecifiedSelected() {
        assertEquals(false, cmi.isSelected());
    }

    @Test public void setTrueSelected() {
        cmi.setSelected(true);
        assertTrue(cmi.isSelected());
    }

    @Test public void setFalseSelected() {
        cmi.setSelected(false);
        assertFalse(cmi.isSelected());
    }

    @Test public void selectedNotSetButNotNull() {
        cmi.selectedProperty();
        assertNotNull(cmi.isSelected());
    }

    @Test public void selectedCanBeBound1() {
        SimpleBooleanProperty other = new SimpleBooleanProperty(cmi, "selected", false);
        cmi.selectedProperty().bind(other);
        assertEquals(other.get(), cmi.isSelected());
    }

    @Test public void selectedCanBeBound2() {
        SimpleBooleanProperty other = new SimpleBooleanProperty(cmi, "selected", true);
        cmi.selectedProperty().bind(other);
        assertEquals(other.get(), cmi.isSelected());
    }
    
}
