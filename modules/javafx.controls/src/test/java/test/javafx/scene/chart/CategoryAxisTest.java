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

package test.javafx.scene.chart;

import javafx.css.CssMetaData;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.StyleableProperty;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

import javafx.scene.chart.CategoryAxis;

/**
 * All public members of CatgoryAxis are tested here .
 * @author srikalyc
 */
public class CategoryAxisTest {
    private CategoryAxis axis;//Empty string
    private AxisHelper helper;

    public CategoryAxisTest() {
        helper = new AxisHelper();
    }

    @Before public void setup() {
        if (axis == null) {
            axis = new CategoryAxis();
        }
        helper.setAxis(axis);
    }



    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultStartMargin() {
        assertEquals(axis.getStartMargin(), 5.0 , 0.0);
    }

    @Test public void defaultEndMargin() {
        assertEquals(axis.getStartMargin(), 5.0 , 0.0);
    }

    @Test public void defaultGapStartAndEndIsTrue() {
        assertTrue(axis.isGapStartAndEnd());
    }

    @Test public void defaultCategorySpacing() {
        assertEquals(axis.getCategorySpacing(), 1.0 , 0.0);
    }

    @Test public void noArgConstructorSetsNonNullCategories() {
        assertNotNull(axis.getCategories());
    }


    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void checkStartMarginPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(56.0);
        axis.startMarginProperty().bind(objPr);
        assertEquals("startMarginProperty cannot be bound", axis.startMarginProperty().getValue(),56.0,0.0);
        objPr.setValue(23.0);
        assertEquals("startMarginProperty cannot be bound", axis.startMarginProperty().getValue(),23.0,0.0);
    }

    @Test public void checkEndMarginPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(56.0);
        axis.endMarginProperty().bind(objPr);
        assertEquals("endMarginProperty cannot be bound", axis.endMarginProperty().getValue(),56.0,0.0);
        objPr.setValue(23.0);
        assertEquals("endMarginProperty cannot be bound", axis.endMarginProperty().getValue(),23.0,0.0);
    }


    @Test public void checkGapStartAndEndPropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        axis.gapStartAndEndProperty().bind(objPr);
        assertTrue("gapStartAndEndProperty cannot be bound", axis.gapStartAndEndProperty().getValue());
        objPr.setValue(false);
        assertFalse("gapStartAndEndProperty cannot be bound", axis.gapStartAndEndProperty().getValue());
    }


    @Test public void checkCategorySpacingReadOnlyCannotBind() {
        assertTrue(axis.categorySpacingProperty() instanceof ReadOnlyDoubleProperty);
    }

    @Test public void startMarginPropertyHasBeanReference() {
        assertSame(axis, axis.startMarginProperty().getBean());
    }

    @Test public void startMarginPropertyHasName() {
        assertEquals("startMargin", axis.startMarginProperty().getName());
    }

    @Test public void endMarginPropertyHasBeanReference() {
        assertSame(axis, axis.endMarginProperty().getBean());
    }

    @Test public void endMarginPropertyHasName() {
        assertEquals("endMargin", axis.endMarginProperty().getName());
    }

    @Test public void gapStartAndEndPropertyHasBeanReference() {
        assertSame(axis, axis.gapStartAndEndProperty().getBean());
    }

    @Test public void gapStartAndEndPropertyHasName() {
        assertEquals("gapStartAndEnd", axis.gapStartAndEndProperty().getName());
    }

    @Test public void categorySpacingPropertyHasBeanReference() {
        assertSame(axis, axis.categorySpacingProperty().getBean());
    }

    @Test public void categorySpacingPropertyHasName() {
        assertEquals("categorySpacing", axis.categorySpacingProperty().getName());
    }



    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenStartMarginIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.startMarginProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        DoubleProperty other = new SimpleDoubleProperty();
        axis.startMarginProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenStartMarginIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.startMarginProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyStartMarginViaCSS() {
        ((StyleableProperty)axis.startMarginProperty()).applyStyle(null, 10.34);
        assertEquals(10.34, axis.getStartMargin(), 0.0);
    }

    @Test public void whenEndMarginIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.endMarginProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        DoubleProperty other = new SimpleDoubleProperty();
        axis.endMarginProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenEndMarginIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.endMarginProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyEndMarginViaCSS() {
        ((StyleableProperty)axis.endMarginProperty()).applyStyle(null, 10.34);
        assertEquals(10.34, axis.getEndMargin(), 0.0);
    }


    @Test public void whenGapStartAndEndIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.gapStartAndEndProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        BooleanProperty other = new SimpleBooleanProperty();
        axis.gapStartAndEndProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenGapStartAndEndIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.gapStartAndEndProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canGapStartAndEndViaCSS() {
        ((StyleableProperty)axis.gapStartAndEndProperty()).applyStyle(null, Boolean.TRUE);
        assertSame(true, axis.isGapStartAndEnd());
    }



    /*********************************************************************
     * Miscellaneous Tests                                         *
     ********************************************************************/

    @Test public void setStartMarginAndSeeValueIsReflectedInModel() {
        axis.setStartMargin(30.0);
        assertEquals(axis.startMarginProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setStartMarginAndSeeValue() {
        axis.setStartMargin(30.0);
        assertEquals(axis.getStartMargin(), 30.0, 0.0);
    }

    @Test public void setEndMarginAndSeeValueIsReflectedInModel() {
        axis.setEndMargin(30.0);
        assertEquals(axis.endMarginProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setEndMarginAndSeeValue() {
        axis.setEndMargin(30.0);
        assertEquals(axis.getEndMargin(), 30.0, 0.0);
    }

    @Test public void setGapStartAndEndAndSeeValueIsReflectedInModel() {
        axis.setGapStartAndEnd(false);
        assertFalse(axis.gapStartAndEndProperty().getValue());
    }

    @Test public void setGapStartAndEndAndSeeValue() {
        axis.setTickLabelsVisible(true);
        assertTrue(axis.isGapStartAndEnd());
    }

    @Test public void setCategoriesAndSeeValue() {
        ObservableList<String> list = FXCollections.observableArrayList();
        axis.setCategories(list);
        assertSame(axis.getCategories(), list);
    }

    @Test
    public void testDisplayPositionOfValue() {
        axis.setCategories(FXCollections.observableArrayList("A", "B", "C", "D"));
        assertTrue(Double.isFinite(axis.getDisplayPosition("C")));
    }

    @Test
    public void testDisplayPositionOfInvalidValue() {
        axis.setCategories(FXCollections.observableArrayList("A", "B", "C", "D"));
        assertTrue(Double.isNaN(axis.getDisplayPosition("E")));
    }

}
