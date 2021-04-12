/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.Properties;
import javafx.css.CssMetaData;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;

import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author srikalyc
 */
public class ScrollBarTest {
    private ScrollBar scrollBar;//Empty string
    private Toolkit tk;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        scrollBar = new ScrollBar();
    }



    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultConstructorShouldSetStyleClassTo_scrollbar() {
        assertStyleClassContains(scrollBar, "scroll-bar");
    }

    @Test public void defaultFocusTraversibleIsFalse() {
        assertFalse(scrollBar.isFocusTraversable());
    }

    @Test public void defaultWidth() {
        assertEquals(scrollBar.getWidth(), Properties.DEFAULT_WIDTH, 0.0);
    }

    @Test public void defaultHeight() {
        assertEquals(scrollBar.getHeight(), Properties.DEFAULT_LENGTH, 0.0);
    }

    @Test public void defaultMin() {
        assertEquals(scrollBar.getMin(), 0.0 , 0.0);
    }

    @Test public void defaultMax() {
        assertEquals(scrollBar.getMax(), 100.0 , 0.0);
    }

    @Test public void defaultValue() {
        assertEquals(scrollBar.getValue(), 0.0 , 0.0);
    }

    @Test public void defaultOrientation() {
        assertSame(scrollBar.getOrientation(), Orientation.HORIZONTAL);
    }

    @Test public void defaultUnitIncrement() {
        assertEquals(scrollBar.getUnitIncrement(), 1.0 , 0.0);
    }

    @Test public void defaultBlockIncrement() {
        assertEquals(scrollBar.getBlockIncrement(), 10.0 , 0.0);
    }

    @Test public void defaultVisibleAmtIncrement() {
        assertEquals(scrollBar.getVisibleAmount(), 15.0 , 0.0);
    }


    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/


    @Test public void checkMinPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollBar.minProperty().bind(objPr);
        assertEquals("minProperty cannot be bound", scrollBar.minProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("minProperty cannot be bound", scrollBar.minProperty().getValue(), 5.0, 0.0);
    }

    @Test public void checkMaxPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollBar.maxProperty().bind(objPr);
        assertEquals("maxProperty cannot be bound", scrollBar.maxProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("maxProperty cannot be bound", scrollBar.maxProperty().getValue(), 5.0, 0.0);
    }

    @Test public void checkValuePropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollBar.valueProperty().bind(objPr);
        assertEquals("valueProperty cannot be bound", scrollBar.valueProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("valueProperty cannot be bound", scrollBar.valueProperty().getValue(), 5.0, 0.0);
    }

    @Test public void checkOrientationPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Orientation>(Orientation.HORIZONTAL);
        scrollBar.orientationProperty().bind(objPr);
        assertSame("orientationProperty cannot be bound", scrollBar.orientationProperty().getValue(), Orientation.HORIZONTAL);
        objPr.setValue(Orientation.VERTICAL);
        assertSame("orientationProperty cannot be bound", scrollBar.orientationProperty().getValue(), Orientation.VERTICAL);
    }

    @Test public void checkUnitIncrementPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollBar.unitIncrementProperty().bind(objPr);
        assertEquals("unitIncrementProperty cannot be bound", scrollBar.unitIncrementProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("unitIncrementProperty cannot be bound", scrollBar.unitIncrementProperty().getValue(), 5.0, 0.0);
    }

    @Test public void checkBlockIncrementPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollBar.blockIncrementProperty().bind(objPr);
        assertEquals("blockIncrementProperty cannot be bound", scrollBar.blockIncrementProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("blockIncrementProperty cannot be bound", scrollBar.blockIncrementProperty().getValue(), 5.0, 0.0);
    }

    @Test public void checkVisibleAmtPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollBar.visibleAmountProperty().bind(objPr);
        assertEquals("visibleAmountProperty cannot be bound", scrollBar.visibleAmountProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("visibleAmountProperty cannot be bound", scrollBar.visibleAmountProperty().getValue(), 5.0, 0.0);
    }



    @Test public void minPropertyHasBeanReference() {
        assertSame(scrollBar, scrollBar.minProperty().getBean());
    }

    @Test public void minPropertyHasName() {
        assertEquals("min", scrollBar.minProperty().getName());
    }

    @Test public void maxPropertyHasBeanReference() {
        assertSame(scrollBar, scrollBar.maxProperty().getBean());
    }

    @Test public void maxPropertyHasName() {
        assertEquals("max", scrollBar.maxProperty().getName());
    }

    @Test public void valuePropertyHasBeanReference() {
        assertSame(scrollBar, scrollBar.valueProperty().getBean());
    }

    @Test public void valuePropertyHasName() {
        assertEquals("value", scrollBar.valueProperty().getName());
    }


    @Test public void orientationPropertyHasBeanReference() {
        assertSame(scrollBar, scrollBar.orientationProperty().getBean());
    }

    @Test public void orientationPropertyHasName() {
        assertEquals("orientation", scrollBar.orientationProperty().getName());
    }

    @Test public void unitIncrementPropertyHasBeanReference() {
        assertSame(scrollBar, scrollBar.unitIncrementProperty().getBean());
    }

    @Test public void unitIncrementPropertyHasName() {
        assertEquals("unitIncrement", scrollBar.unitIncrementProperty().getName());
    }

    @Test public void blockIncrementPropertyHasBeanReference() {
        assertSame(scrollBar, scrollBar.blockIncrementProperty().getBean());
    }

    @Test public void blockIncrementPropertyHasName() {
        assertEquals("blockIncrement", scrollBar.blockIncrementProperty().getName());
    }

    @Test public void visibleAmtPropertyHasBeanReference() {
        assertSame(scrollBar, scrollBar.visibleAmountProperty().getBean());
    }

    @Test public void visibleAmtPropertyHasName() {
        assertEquals("visibleAmount", scrollBar.visibleAmountProperty().getName());
    }


    /*********************************************************************
     * Check for Pseudo classes                                          *
     ********************************************************************/
    @Test public void settingVerticalOrientationSetsVerticalPseudoClass() {
        scrollBar.setOrientation(Orientation.VERTICAL);
        assertPseudoClassExists(scrollBar, "vertical");
        assertPseudoClassDoesNotExist(scrollBar, "horizontal");
    }

    @Test public void clearingVerticalOrientationClearsVerticalPseudoClass() {
        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.setOrientation(Orientation.HORIZONTAL);
        assertPseudoClassDoesNotExist(scrollBar, "vertical");
        assertPseudoClassExists(scrollBar, "horizontal");
    }

    @Test public void settingHorizontalOrientationSetsHorizontalPseudoClass() {
        scrollBar.setOrientation(Orientation.HORIZONTAL);
        assertPseudoClassExists(scrollBar, "horizontal");
        assertPseudoClassDoesNotExist(scrollBar, "vertical");
    }

    @Test public void clearingHorizontalOrientationClearsHorizontalPseudoClass() {
        scrollBar.setOrientation(Orientation.HORIZONTAL);
        scrollBar.setOrientation(Orientation.VERTICAL);
        assertPseudoClassDoesNotExist(scrollBar, "horizontal");
        assertPseudoClassExists(scrollBar, "vertical");
    }



    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenOrientationIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)scrollBar.orientationProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollBar));
        ObjectProperty<Orientation> other = new SimpleObjectProperty<Orientation>(Orientation.VERTICAL);
        scrollBar.orientationProperty().bind(other);
        assertFalse(styleable.isSettable(scrollBar));
    }

    @Test public void whenOrientationIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)scrollBar.orientationProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollBar));
    }

    @Test public void canSpecifyOrientationViaCSS() {
        ((StyleableProperty)scrollBar.orientationProperty()).applyStyle(null, Orientation.VERTICAL);
        assertSame(Orientation.VERTICAL, scrollBar.getOrientation());
    }

    @Test public void whenUnitIncIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)scrollBar.unitIncrementProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollBar));
        DoubleProperty other = new SimpleDoubleProperty(2.0);
        scrollBar.unitIncrementProperty().bind(other);
        assertFalse(styleable.isSettable(scrollBar));
    }

    @Test public void whenUnitIncIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)scrollBar.unitIncrementProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollBar));
    }

    @Test public void canSpecifyUnitIncViaCSS() {
        ((StyleableProperty)scrollBar.unitIncrementProperty()).applyStyle(null, 6.0);
        assertEquals(6.0, scrollBar.getUnitIncrement(), 0.0);
    }

    @Test public void whenBlockIncIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)scrollBar.blockIncrementProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollBar));
        DoubleProperty other = new SimpleDoubleProperty(2.0);
        scrollBar.blockIncrementProperty().bind(other);
        assertFalse(styleable.isSettable(scrollBar));
    }

    @Test public void whenBlockIncIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)scrollBar.blockIncrementProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollBar));
    }

    @Test public void canSpecifyBlockIncViaCSS() {
        ((StyleableProperty)scrollBar.blockIncrementProperty()).applyStyle(null, 6.0);
        assertEquals(6.0, scrollBar.getBlockIncrement(), 0.0);
    }


    /*********************************************************************
     * Miscellaneous Tests                                               *
     ********************************************************************/
    @Test public void setOrientationAndSeeValueIsReflectedInModel() {
        scrollBar.setOrientation(Orientation.HORIZONTAL);
        assertSame(scrollBar.orientationProperty().getValue(), Orientation.HORIZONTAL);
    }

    @Test public void setOrientationAndSeeValue() {
        scrollBar.setOrientation(Orientation.VERTICAL);
        assertSame(scrollBar.getOrientation(), Orientation.VERTICAL);
    }

    @Test public void setMinAndSeeValueIsReflectedInModel() {
        scrollBar.setMin(30.0);
        assertEquals(scrollBar.minProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setMinAndSeeValue() {
        scrollBar.setMin(30.0);
        assertEquals(scrollBar.getMin(), 30.0, 0.0);
    }

    @Test public void setMaxAndSeeValueIsReflectedInModel() {
        scrollBar.setMax(30.0);
        assertEquals(scrollBar.maxProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setMaxAndSeeValue() {
        scrollBar.setMax(30.0);
        assertEquals(scrollBar.getMax(), 30.0, 0.0);
    }

    @Test public void setValueAndSeeValueIsReflectedInModel() {
        scrollBar.setValue(30.0);
        assertEquals(scrollBar.valueProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setValueAndSeeValue() {
        scrollBar.setValue(30.0);
        assertEquals(scrollBar.getValue(), 30.0, 0.0);
    }

    @Test public void setUnitIncAndSeeValueIsReflectedInModel() {
        scrollBar.setUnitIncrement(30.0);
        assertEquals(scrollBar.unitIncrementProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setUnitIncAndSeeValue() {
        scrollBar.setUnitIncrement(30.0);
        assertEquals(scrollBar.getUnitIncrement(), 30.0, 0.0);
    }

    @Test public void setBlockIncAndSeeValueIsReflectedInModel() {
        scrollBar.setBlockIncrement(30.0);
        assertEquals(scrollBar.blockIncrementProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setBlockIncAndSeeValue() {
        scrollBar.setBlockIncrement(30.0);
        assertEquals(scrollBar.getBlockIncrement(), 30.0, 0.0);
    }

    @Test public void setVisibleAmtAndSeeValueIsReflectedInModel() {
        scrollBar.setVisibleAmount(30.0);
        assertEquals(scrollBar.visibleAmountProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setVisibleAmtAndSeeValue() {
        scrollBar.setVisibleAmount(30.0);
        assertEquals(scrollBar.getVisibleAmount(), 30.0, 0.0);
    }

    @Test public void checkAdjustValueWhenActualValueIsCloseToMin() {
        scrollBar.setMin(0.0);
        scrollBar.setMax(100.0);
        scrollBar.setValue(30.0);
        scrollBar.adjustValue(0.5);//This should block increment the value
        assertEquals(scrollBar.getValue(), 40.0, 0.0);
        scrollBar.adjustValue(0.5);//This should block increment the value
        assertEquals(scrollBar.getValue(), 50.0, 0.0);
        scrollBar.adjustValue(0.5);//This should not further change because position*(max-min) is now eqauls to the value so no more increments
        assertEquals(scrollBar.getValue(), 50.0, 0.0);
    }

    @Test public void checkAdjustValueWhenActualValueIsCloseToMax() {
        scrollBar.setMin(0.0);
        scrollBar.setMax(100.0);
        scrollBar.setValue(70.0);
        scrollBar.adjustValue(0.5);//This should block decrement the value
        assertEquals(scrollBar.getValue(), 60.0, 0.0);
        scrollBar.adjustValue(0.5);//This should block decrement the value
        assertEquals(scrollBar.getValue(), 50.0, 0.0);
        scrollBar.adjustValue(0.5);//This should not further change because position*(max-min) is now eqauls to the value so no more decrements
        assertEquals(scrollBar.getValue(), 50.0, 0.0);
    }

    @Test public void incrementWhenValueIsNegativeAndSeeIfValueIsClampedToMin() {
        scrollBar.setValue(-30.0);
        scrollBar.increment();
        assertEquals(scrollBar.getValue(), 0.0, 0.0);
    }

    @Test public void decrementWhenValueIsNegativeAndSeeIfValueIsClampedToMin() {
        scrollBar.setValue(-30.0);
        scrollBar.decrement();
        assertEquals(scrollBar.getValue(), 0.0, 0.0);
    }

    @Test public void incrementWhenValueIsGreaterThanMaxAndSeeIfValueIsClampedToMax() {
        scrollBar.setValue(3000.0);
        scrollBar.increment();
        assertEquals(scrollBar.getValue(), 100.0, 0.0);
    }

    @Test public void decrementWhenValueIsGreaterThanMaxAndSeeIfValueIsClampedToMax() {
        scrollBar.setValue(3000.0);
        scrollBar.decrement();
        assertEquals(scrollBar.getValue(), 100.0, 0.0);
    }


}
