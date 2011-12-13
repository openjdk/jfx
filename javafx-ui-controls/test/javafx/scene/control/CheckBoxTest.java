/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;

import org.junit.Before;
import org.junit.Test;

import static javafx.scene.control.ControlTestUtils.*;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 */
public class CheckBoxTest {
    private CheckBox btn;

    @Before public void setup() {
        btn = new CheckBox();
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void defaultConstructorShouldHaveNoGraphicAndEmptyString() {
        assertNull(btn.getGraphic());
        assertEquals("", btn.getText());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphicAndSpecifiedString() {
        CheckBox b2 = new CheckBox(null);
        assertNull(b2.getGraphic());
        assertNull(b2.getText());

        b2 = new CheckBox("");
        assertNull(b2.getGraphic());
        assertEquals("", b2.getText());

        b2 = new CheckBox("Hello");
        assertNull(b2.getGraphic());
        assertEquals("Hello", b2.getText());
    }

    @Test public void defaultConstructorShouldSetStyleClassTo_check_box() {
        assertStyleClassContains(btn, "check-box");
    }

    @Test public void oneArgConstructorShouldSetStyleClassTo_check_box() {
        CheckBox b2 = new CheckBox(null);
        assertStyleClassContains(b2, "check-box");
    }

    @Test public void defaultConstructorShouldSetAlignmentTo_CENTER_LEFT() {
        assertEquals(Pos.CENTER_LEFT, btn.getAlignment());
    }

    @Test public void oneArgConstructorShouldSetAlignmentTo_CENTER_LEFT() {
        CheckBox b2 = new CheckBox(null);
        assertEquals(Pos.CENTER_LEFT, b2.getAlignment());
    }

    @Test public void defaultConstructorShouldSetMnemonicParsingTo_true() {
        assertTrue(btn.isMnemonicParsing());
    }

    @Test public void oneArgConstructorShouldSetMnemonicParsingTo_true() {
        CheckBox b2 = new CheckBox(null);
        assertTrue(b2.isMnemonicParsing());
    }

    @Test public void defaultConstructorShouldSet_determinate_PseudoClass() {
        assertPseudoClassExists(btn, "determinate");
        assertPseudoClassDoesNotExist(btn, "indeterminate");
    }

    @Test public void oneArgConstructorShouldSet_determinate_PseudoClass() {
        CheckBox b2 = new CheckBox("Hi");
        assertPseudoClassExists(b2, "determinate");
        assertPseudoClassDoesNotExist(b2, "indeterminate");
    }

    @Test public void defaultConstructorShouldNotSet_selected_PseudoClass() {
        assertPseudoClassDoesNotExist(btn, "selected");
    }

    @Test public void oneArgConstructorShouldNotSet_selected_PseudoClass() {
        CheckBox b2 = new CheckBox("Hi");
        assertPseudoClassDoesNotExist(b2, "selected");
    }

    /*********************************************************************
     * Tests for the indeterminate state                                 *
     ********************************************************************/

    @Test public void indeterminateIsFalseByDefault() {
        assertFalse(btn.isIndeterminate());
        assertFalse(btn.indeterminateProperty().getValue());
    }

    @Test public void indeterminateCanBeSet() {
        btn.setIndeterminate(true);
        assertTrue(btn.isIndeterminate());
    }

    @Test public void indeterminateSetToNonDefaultValueIsReflectedInModel() {
        btn.setIndeterminate(true);
        assertTrue(btn.indeterminateProperty().getValue());
    }

    @Test public void indeterminateCanBeCleared() {
        btn.setIndeterminate(true);
        btn.setIndeterminate(false);
        assertFalse(btn.isIndeterminate());
    }

    @Test public void indeterminateCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.indeterminateProperty().bind(other);
        assertTrue(btn.isIndeterminate());
    }

    @Test public void indeterminatePropertyHasBeanReference() {
        assertSame(btn, btn.indeterminateProperty().getBean());
    }

    @Test public void indeterminatePropertyHasName() {
        assertEquals("indeterminate", btn.indeterminateProperty().getName());
    }

    @Test public void settingIndeterminateSetsPseudoClass() {
        btn.setIndeterminate(true);
        assertPseudoClassExists(btn, "indeterminate");
        assertPseudoClassDoesNotExist(btn, "determinate");
    }

    @Test public void clearingIndeterminateClearsPseudoClass() {
        btn.setIndeterminate(true);
        btn.setIndeterminate(false);
        assertPseudoClassExists(btn, "determinate");
        assertPseudoClassDoesNotExist(btn, "indeterminate");
    }

    @Test public void indeterminateSetToTrueViaBindingSetsPseudoClass() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.indeterminateProperty().bind(other);
        assertPseudoClassExists(btn, "indeterminate");
        assertPseudoClassDoesNotExist(btn, "determinate");
    }

    @Test public void indeterminateSetToFalseViaBindingClearsPseudoClass() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.indeterminateProperty().bind(other);
        other.setValue(false);
        assertPseudoClassExists(btn, "determinate");
        assertPseudoClassDoesNotExist(btn, "indeterminate");
    }

    @Ignore("impl_cssSet API removed")
    @Test public void cannotSpecifyIndeterminateViaCSS() {
//        btn.impl_cssSet("-fx-indeterminate", true);
        assertFalse(btn.isIndeterminate());
    }

    /*********************************************************************
     * Tests for the selected state                                      *
     ********************************************************************/

    @Test public void selectedIsFalseByDefault() {
        assertFalse(btn.isSelected());
        assertFalse(btn.selectedProperty().getValue());
    }

    @Test public void selectedCanBeSet() {
        btn.setSelected(true);
        assertTrue(btn.isSelected());
    }

    @Test public void selectedSetToNonDefaultValueIsReflectedInModel() {
        btn.setSelected(true);
        assertTrue(btn.selectedProperty().getValue());
    }

    @Test public void selectedCanBeCleared() {
        btn.setSelected(true);
        btn.setSelected(false);
        assertFalse(btn.isSelected());
    }

    @Test public void selectedCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.selectedProperty().bind(other);
        assertTrue(btn.isSelected());
    }

    @Test public void selectedPropertyHasBeanReference() {
        assertSame(btn, btn.selectedProperty().getBean());
    }

    @Test public void selectedPropertyHasName() {
        assertEquals("selected", btn.selectedProperty().getName());
    }

    @Test public void settingSelectedSetsPseudoClass() {
        btn.setSelected(true);
        assertPseudoClassExists(btn, "selected");
    }

    @Test public void clearingSelectedClearsPseudoClass() {
        btn.setSelected(true);
        btn.setSelected(false);
        assertPseudoClassDoesNotExist(btn, "selected");
    }

    @Test public void selectedSetToTrueViaBindingSetsPseudoClass() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.selectedProperty().bind(other);
        assertPseudoClassExists(btn, "selected");
    }

    @Test public void selectedSetToFalseViaBindingClearsPseudoClass() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.selectedProperty().bind(other);
        other.setValue(false);
        assertPseudoClassDoesNotExist(btn, "selected");
    }

    @Ignore("impl_cssSet API removed")
    @Test public void cannotSpecifySelectedViaCSS() {
//        btn.impl_cssSet("-fx-selected", true);
        assertFalse(btn.isSelected());
    }

    /*********************************************************************
     * Tests for the allowIndeterminate state                            *
     ********************************************************************/

    @Test public void allowIndeterminateIsFalseByDefault() {
        assertFalse(btn.isAllowIndeterminate());
        assertFalse(btn.allowIndeterminateProperty().getValue());
    }

    @Test public void allowIndeterminateCanBeSet() {
        btn.setAllowIndeterminate(true);
        assertTrue(btn.isAllowIndeterminate());
    }

    @Test public void allowIndeterminateSetToNonDefaultValueIsReflectedInModel() {
        btn.setAllowIndeterminate(true);
        assertTrue(btn.allowIndeterminateProperty().getValue());
    }

    @Test public void allowIndeterminateCanBeCleared() {
        btn.setAllowIndeterminate(true);
        btn.setAllowIndeterminate(false);
        assertFalse(btn.isAllowIndeterminate());
    }

    @Test public void allowIndeterminateCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        btn.allowIndeterminateProperty().bind(other);
        assertTrue(btn.isAllowIndeterminate());
    }

    @Test public void allowIndeterminatePropertyHasBeanReference() {
        assertSame(btn, btn.allowIndeterminateProperty().getBean());
    }

    @Test public void allowIndeterminatePropertyHasName() {
        assertEquals("allowIndeterminate", btn.allowIndeterminateProperty().getName());
    }

    @Ignore("impl_cssSet API removed")
    @Test public void cannotSpecifyAllowIndeterminateViaCSS() {
//        btn.impl_cssSet("-fx-allow-indeterminate", true);
        assertFalse(btn.isAllowIndeterminate());
    }

    /*********************************************************************
     * Tests for the fire() method.                                      *
     *   For allowIndeterminate(false),                                  *
     *      unselected -> selected -> unselected                         *
     *   For allowIndeterminate(true),                                   *
     *      unselected -> indeterminate -> selected -> unselected        *
     ********************************************************************/

    @Test public void fireUnselectedDeterminateCheckboxResultsIn_Selected() {
        btn.fire();
        assertFalse(btn.isIndeterminate());
        assertTrue(btn.isSelected());
    }

    @Test public void fireSelectedDeterminateCheckboxResultsIn_Unselected() {
        btn.setSelected(true);
        btn.fire();
        assertFalse(btn.isIndeterminate());
        assertFalse(btn.isSelected());
    }

    @Test public void fireIndeterminateDeterminateCheckboxResultsIn_Selected() {
        btn.setIndeterminate(true);
        btn.fire();
        assertFalse(btn.isIndeterminate());
        assertTrue(btn.isSelected());
    }

    @Test public void fireUnselectedIndeterminateCheckboxResultsIn_Indeterminate() {
        btn.setAllowIndeterminate(true);
        btn.fire();
        assertFalse(btn.isSelected());
        assertTrue(btn.isIndeterminate());
    }

    @Test public void fireIndeterminateIndeterminateCheckboxResultsIn_Selected() {
        btn.setAllowIndeterminate(true);
        btn.setIndeterminate(true);
        btn.fire();
        assertTrue(btn.isSelected());
        assertFalse(btn.isIndeterminate());
    }

    @Test public void fireSelectedIndeterminateCheckboxResultsIn_Unselected() {
        btn.setAllowIndeterminate(true);
        btn.setSelected(true);
        btn.fire();
        assertFalse(btn.isSelected());
        assertFalse(btn.isIndeterminate());
    }
}
