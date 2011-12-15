/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import com.sun.javafx.css.StyleableProperty;
import static javafx.scene.control.ControlTestUtils.assertPseudoClassExists;
import static javafx.scene.control.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 */
public class SeparatorTest {
    
    private Separator separator;
    @Before public void setup() {
        separator = new Separator();
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void defaultConstructorCreatesSeparatorWith_CENTER_halignAnd_CENTER_valignAndHorizontal() {
        assertEquals(HPos.CENTER, separator.getHalignment());
        assertEquals(VPos.CENTER, separator.getValignment());
        assertEquals(Orientation.HORIZONTAL, separator.getOrientation());
    }

    @Test public void oneArgConstructorDefaultsToCENTER_CENTERAndUsesSuppliedOrientation() {
        Separator s2 = new Separator(Orientation.VERTICAL);
        assertEquals(HPos.CENTER, s2.getHalignment());
        assertEquals(VPos.CENTER, s2.getValignment());
        assertEquals(Orientation.VERTICAL, s2.getOrientation());
    }

    @Test public void defaultConstructorShouldSetStyleClassTo_separator() {
        assertStyleClassContains(separator, "separator");
    }

    @Test public void oneArgConstructorShouldSetStyleClassTo_separator() {
        Separator s2 = new Separator(Orientation.VERTICAL);
        assertStyleClassContains(s2, "separator");
    }

    @Test public void defaultConstructorShouldSetFocusTraversableToFalse() {
        assertFalse(separator.isFocusTraversable());
    }

    @Test public void oneArgConstructorShouldSetFocusTraversableToFalse() {
        Separator s2 = new Separator(Orientation.VERTICAL);
        assertFalse(s2.isFocusTraversable());
    }

    @Test public void defaultConstructorShouldPseudoclassStateTo_horizontal() {
        assertPseudoClassExists(separator, "horizontal");
    }

    @Test public void oneArgConstructorShouldSetPseudoclassStateBasedOnOrientationSupplied() {
        Separator s2 = new Separator(Orientation.VERTICAL);
        assertPseudoClassExists(s2, "vertical");
    }

    /********************************************************************************
     *                                                                              *
     *                       Tests for orientation property                         *
     *                                                                              *
     *  - can be null                                                               *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - is HORIZONTAL by default                                                  *
     *  - if bound, impl_cssSettable returns false                                  *
     *  - if specified via CSS and not bound, impl_cssSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void orientationDefaultValueIsHORIZONTAL() {
        assertSame(Orientation.HORIZONTAL, separator.getOrientation());
        assertSame(Orientation.HORIZONTAL, separator.orientationProperty().get());
    }

    @Test public void orientationCanBeNull() {
        separator.setOrientation(null);
        assertNull(separator.getOrientation());
    }

    @Test public void settingOrientationShouldWork() {
        separator.setOrientation(Orientation.VERTICAL);
        assertSame(Orientation.VERTICAL, separator.getOrientation());
    }

    @Test public void settingOrientationAndThenCreatingAProeprtyAndReadingTheValueStillWorks() {
        separator.setOrientation(Orientation.VERTICAL);
        assertSame(Orientation.VERTICAL, separator.orientationProperty().get());
    }

    @Test public void orientationCanBeBound() {
        ObjectProperty<Orientation> other = new SimpleObjectProperty<Orientation>(Orientation.VERTICAL);
        separator.orientationProperty().bind(other);
        assertSame(Orientation.VERTICAL, separator.getOrientation());
    }

    @Test public void whenOrientationIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(separator.orientationProperty());
        assertTrue(styleable.isSettable(separator));
        ObjectProperty<Orientation> other = new SimpleObjectProperty<Orientation>(Orientation.VERTICAL);
        separator.orientationProperty().bind(other);
        assertFalse(styleable.isSettable(separator));
    }

    @Test public void whenOrientationIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(separator.orientationProperty());
        styleable.set(separator, Orientation.VERTICAL);
        assertTrue(styleable.isSettable(separator));
    }

    @Test public void canSpecifyOrientationViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(separator.orientationProperty());
        styleable.set(separator, Orientation.VERTICAL);
        assertSame(Orientation.VERTICAL, separator.getOrientation());
    }

    @Ignore("This is an unreliable test because it uses the string version " +
            "of the function instead of the other, and no check is made " +
            "for bits set")
    @Test public void whenSettingOrientationToItsExistingValue_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(separator.orientationProperty());
        assertTrue(styleable.isSettable(separator));
        separator.setOrientation(Orientation.HORIZONTAL);
        assertFalse(styleable.isSettable(separator));
    }

    @Test public void settingOrientationToVERTICALChangesPseudoclassTo_vertical() {
        separator.setOrientation(Orientation.VERTICAL);
        assertPseudoClassExists(separator, "vertical");
    }

    @Test public void settingOrientationToBackToHORIZONTALChangesPseudoclassTo_horizontal() {
        separator.setOrientation(Orientation.HORIZONTAL);
        assertPseudoClassExists(separator, "horizontal");
    }

    /********************************************************************************
     *                                                                              *
     *                       Tests for halignment property                          *
     *                                                                              *
     *  - can be null                                                               *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - is CENTER by default                                                      *
     *  - if bound, impl_cssSettable returns false                                  *
     *  - if specified via CSS and not bound, impl_cssSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void halignmentDefaultValueIsCENTER() {
        assertSame(HPos.CENTER, separator.getHalignment());
        assertSame(HPos.CENTER, separator.halignmentProperty().get());
    }

    @Test public void halignmentCanBeNull() {
        separator.setHalignment(null);
        assertNull(separator.getHalignment());
    }

    @Test public void settingHalignmentShouldWork() {
        separator.setHalignment(HPos.RIGHT);
        assertSame(HPos.RIGHT, separator.getHalignment());
    }

    @Test public void settingHalignmentAndThenCreatingAProeprtyAndReadingTheValueStillWorks() {
        separator.setHalignment(HPos.RIGHT);
        assertSame(HPos.RIGHT, separator.halignmentProperty().get());
    }

    @Test public void halignmentCanBeBound() {
        ObjectProperty<HPos> other = new SimpleObjectProperty<HPos>(HPos.RIGHT);
        separator.halignmentProperty().bind(other);
        assertSame(HPos.RIGHT, separator.getHalignment());
    }

    @Test public void whenHalignmentIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(separator.halignmentProperty());
        assertTrue(styleable.isSettable(separator));
        ObjectProperty<HPos> other = new SimpleObjectProperty<HPos>(HPos.RIGHT);
        separator.halignmentProperty().bind(other);
        assertFalse(styleable.isSettable(separator));
    }

    @Test public void whenHalignmentIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(separator.halignmentProperty());
        styleable.set(separator, HPos.RIGHT);
        assertTrue(styleable.isSettable(separator));
    }

    @Test public void canSpecifyHalignmentViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(separator.halignmentProperty());
        styleable.set(separator, HPos.RIGHT);
        assertSame(HPos.RIGHT, separator.getHalignment());
    }

    /********************************************************************************
     *                                                                              *
     *                       Tests for valignment property                          *
     *                                                                              *
     *  - can be null                                                               *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - is CENTER by default                                                      *
     *  - if bound, impl_cssSettable returns false                                  *
     *  - if specified via CSS and not bound, impl_cssSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void valignmentDefaultValueIsCENTER() {
        assertSame(VPos.CENTER, separator.getValignment());
        assertSame(VPos.CENTER, separator.valignmentProperty().get());
    }

    @Test public void valignmentCanBeNull() {
        separator.setValignment(null);
        assertNull(separator.getValignment());
    }

    @Test public void settingValignmentShouldWork() {
        separator.setValignment(VPos.BASELINE);
        assertSame(VPos.BASELINE, separator.getValignment());
    }

    @Test public void settingValignmentAndThenCreatingAProeprtyAndReadingTheValueStillWorks() {
        separator.setValignment(VPos.BASELINE);
        assertSame(VPos.BASELINE, separator.valignmentProperty().get());
    }

    @Test public void valignmentCanBeBound() {
        ObjectProperty<VPos> other = new SimpleObjectProperty<VPos>(VPos.BASELINE);
        separator.valignmentProperty().bind(other);
        assertSame(VPos.BASELINE, separator.getValignment());
    }

    @Test public void whenValignmentIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(separator.valignmentProperty());
        assertTrue(styleable.isSettable(separator));
        ObjectProperty<VPos> other = new SimpleObjectProperty<VPos>(VPos.BASELINE);
        separator.valignmentProperty().bind(other);
        assertFalse(styleable.isSettable(separator));
    }

    @Test public void whenValignmentIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(separator.valignmentProperty());
        styleable.set(separator, VPos.BASELINE);
        assertTrue(styleable.isSettable(separator));
    }

    @Test public void canSpecifyValignmentViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(separator.valignmentProperty());
        styleable.set(separator, VPos.BASELINE);
        assertSame(VPos.BASELINE, separator.getValignment());
    }
}
