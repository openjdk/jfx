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

package javafx.scene.chart;

import javafx.css.CssMetaData;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.util.StringConverter;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

/**
 * All public members of NumberAxis are tested here . 
 * @author srikalyc
 */
public class NumberAxisTest {
    private NumberAxis axis;//Empty string
    private NumberAxis threeValueAxis;//Empty string
    private NumberAxis fourValueAxis;//Empty string
    private ValueAxisHelper helper;
    private StringConverter<Number> formatter;
    
    public NumberAxisTest() {
        helper = new ValueAxisHelper();
    }
    
    @Before public void setup() {
        if (axis == null) {
            axis = new NumberAxis();
        }
        if (threeValueAxis == null) {
            threeValueAxis = new NumberAxis(0.0, 100.0, 10.0);
        }
        if (fourValueAxis == null) {
            fourValueAxis = new NumberAxis("dummy", 0.0, 100.0, 10.0);
        }
        helper.setValueAxis(axis);
        helper.setThreeValueAxis(threeValueAxis);
        helper.setFourValueAxis(fourValueAxis);
        formatter = new StringConverter<Number>() {
            @Override
            public String toString(Number object) { return null; }
            @Override
            public Number fromString(String string) { return null; }
        };
    }
    
   
   
    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultForceZeroInRangeIsTrue() {
        assertTrue(axis.isForceZeroInRange());
    }

    @Test public void threeArgConstructorDefaults() {
        assertEquals(threeValueAxis.getLowerBound(), 0.0, 0.0);
        assertEquals(threeValueAxis.getUpperBound(), 100.0, 0.0);
        assertEquals(threeValueAxis.getTickUnit(), 10.0, 0.0);
    }

    @Test public void fourArgConstructorDefaults() {
        assertEquals(fourValueAxis.getLabel(), "dummy");//Axis label
        assertEquals(fourValueAxis.getLowerBound(), 0.0, 0.0);
        assertEquals(fourValueAxis.getUpperBound(), 100.0, 0.0);
        assertEquals(fourValueAxis.getTickUnit(), 10.0, 0.0);
    }

    @Test public void defaultTickUnit() {
        assertEquals(axis.getTickUnit(), 5.0 , 0.0);
    }



    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/
    
    
    @Test public void checkForceZeroInRangePropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        axis.forceZeroInRangeProperty().bind(objPr);
        assertTrue("forceZeroInRange cannot be bound", axis.forceZeroInRangeProperty().getValue());
        objPr.setValue(false);
        assertFalse("forceZeroInRange cannot be bound", axis.forceZeroInRangeProperty().getValue());
    }
    
    
    @Test public void checkTickUnitPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(56.0);
        axis.tickUnitProperty().bind(objPr);
        assertEquals("tickUnitProperty cannot be bound", axis.tickUnitProperty().getValue(),56.0,0.0);
        objPr.setValue(23.0);
        assertEquals("tickUnitProperty cannot be bound", axis.tickUnitProperty().getValue(),23.0,0.0);
    }
    
    
    @Test public void forceZeroInRangePropertyHasBeanReference() {
        assertSame(axis, axis.forceZeroInRangeProperty().getBean());
    }

    @Test public void forceZeroInRangePropertyHasName() {
        assertEquals("forceZeroInRange", axis.forceZeroInRangeProperty().getName());
    }

    @Test public void tickUnitPropertyHasBeanReference() {
        assertSame(axis, axis.tickUnitProperty().getBean());
    }

    @Test public void tickUnitPropertyHasName() {
        assertEquals("tickUnit", axis.tickUnitProperty().getName());
    }


    
    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/

    @Test public void whenTickUnitIsBound_impl_cssSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.tickUnitProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        DoubleProperty other = new SimpleDoubleProperty();
        axis.tickUnitProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenTickUnitIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.tickUnitProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canTickUnitViaCSS() {
        ((StyleableProperty)axis.tickUnitProperty()).applyStyle(null, 10.34);
        assertEquals(10.34, axis.getTickUnit(), 0.0);
    }



    /*********************************************************************
     * Miscellaneous Tests                                         *
     ********************************************************************/
    
    @Test public void setForceZeroInRangeAndSeeValueIsReflectedInModel() {
        axis.setForceZeroInRange(false);
        assertFalse(axis.forceZeroInRangeProperty().getValue());
    }
    
    @Test public void setForceZeroInRangeAndSeeValue() {
        axis.setForceZeroInRange(true);
        assertTrue(axis.isForceZeroInRange());
    }
    
    @Test public void setTickUnitAndSeeValueIsReflectedInModel() {
        axis.setTickUnit(30.0);
        assertEquals(axis.tickUnitProperty().getValue(), 30.0, 0.0);
    }
    
    @Test public void setTickUnitAndSeeValue() {
        axis.setTickUnit(30.0);
        assertEquals(axis.getTickUnit(), 30.0, 0.0);
    }
    
}
