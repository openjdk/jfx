/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.CssMetaData;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassExists;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.control.Separator;

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
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
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

    @Test public void whenOrientationIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)separator.orientationProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(separator));
        ObjectProperty<Orientation> other = new SimpleObjectProperty<Orientation>(Orientation.VERTICAL);
        separator.orientationProperty().bind(other);
        assertFalse(styleable.isSettable(separator));
    }

    @Test public void whenOrientationIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)separator.orientationProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(separator));
    }

    @Test public void canSpecifyOrientationViaCSS() {
        ((StyleableProperty)separator.orientationProperty()).applyStyle(null, Orientation.VERTICAL);
        assertSame(Orientation.VERTICAL, separator.getOrientation());
    }

    @Ignore("This is an unreliable test because it uses the string version " +
            "of the function instead of the other, and no check is made " +
            "for bits set")
    @Test public void whenSettingOrientationToItsExistingValue_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)separator.orientationProperty()).getCssMetaData();
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
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
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

    @Test public void whenHalignmentIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)separator.halignmentProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(separator));
        ObjectProperty<HPos> other = new SimpleObjectProperty<HPos>(HPos.RIGHT);
        separator.halignmentProperty().bind(other);
        assertFalse(styleable.isSettable(separator));
    }

    @Test public void whenHalignmentIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)separator.halignmentProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(separator));
    }

    @Test public void canSpecifyHalignmentViaCSS() {
        ((StyleableProperty)separator.halignmentProperty()).applyStyle(null, HPos.RIGHT);
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
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
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

    @Test public void whenValignmentIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)separator.valignmentProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(separator));
        ObjectProperty<VPos> other = new SimpleObjectProperty<VPos>(VPos.BASELINE);
        separator.valignmentProperty().bind(other);
        assertFalse(styleable.isSettable(separator));
    }

    @Test public void whenValignmentIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)separator.valignmentProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(separator));
    }

    @Test public void canSpecifyValignmentViaCSS() {
        ((StyleableProperty)separator.valignmentProperty()).applyStyle(null, VPos.BASELINE);
        assertSame(VPos.BASELINE, separator.getValignment());
    }
}
