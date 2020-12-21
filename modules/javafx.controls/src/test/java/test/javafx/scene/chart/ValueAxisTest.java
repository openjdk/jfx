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

package test.javafx.scene.chart;

import java.util.List;
import javafx.css.CssMetaData;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.ValueAxisShim;
import javafx.util.StringConverter;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

/**
 * All public members of ValueAxis are tested here .
 * @author srikalyc
 */
public class ValueAxisTest {
    private ValueAxis axis;//Empty string
    private ValueAxis twoValueAxis;//Empty string
    private StringConverter<Number> formatter;

    public ValueAxisTest() {
    }

    @Before public void setup() {
        if (axis == null) {
              axis = new ValueAxis() {
                @Override
                protected List calculateMinorTickMarks() {return null;}
                @Override
                protected void setRange(Object o, boolean bln) {}
                @Override
                protected Object getRange() {return null;}
                @Override
                protected List calculateTickValues(double d, Object o) {return null;}
                @Override
                protected String getTickMarkLabel(Object t) {return null;}
            };
        }
        if (twoValueAxis == null) {
            twoValueAxis = new ValueAxis(2.0, 100.0) {
                @Override
                protected List calculateMinorTickMarks() {return null;}
                @Override
                protected void setRange(Object o, boolean bln) {}
                @Override
                protected Object getRange() {return null;}
                @Override
                protected List calculateTickValues(double d, Object o) {return null;}
                @Override
                protected String getTickMarkLabel(Object t) {return null;}
            };
        }

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

    @Test public void defaultMinorTickVisibleIsTrue() {
        assertTrue(axis.isMinorTickVisible());
    }

    @Test public void defaultContructorAutoRangingIsTrue() {
        assertTrue(axis.isAutoRanging());
    }

    @Test public void twoValueContructorAutoRangingIsFalse() {
        assertFalse(twoValueAxis.isAutoRanging());
    }

    @Test public void defaultMinorTickLength() {
        assertEquals(axis.getMinorTickLength(), 5.0 , 0.0);
    }

    @Test public void defaultMinorTickCount() {
        assertEquals(axis.getMinorTickCount(), 5.0 , 0.0);
    }

    @Test public void defaultScale() {
        assertEquals(axis.getScale(), 0.0 , 0.0);
    }

    @Test public void defaultUpperBound() {
        assertEquals(axis.getUpperBound(), 100.0 , 0.0);
    }

    @Test public void defaultLowerBound() {
        assertEquals(axis.getLowerBound(), 0.0 , 0.0);
    }

    @Test public void defaultTickLabelFormatter() {
        assertNull(axis.getTickLabelFormatter());
    }



    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/


    @Test public void checkMinorTickVisiblePropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        axis.minorTickVisibleProperty().bind(objPr);
        assertTrue("minorTickVisibleProperty cannot be bound", axis.minorTickVisibleProperty().getValue());
        objPr.setValue(false);
        assertFalse("minorTickVisibleProperty cannot be bound", axis.minorTickVisibleProperty().getValue());
    }


    @Test public void checkMinorTickLengthPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(56.0);
        axis.minorTickLengthProperty().bind(objPr);
        assertEquals("minorTickLengthProperty cannot be bound", axis.minorTickLengthProperty().getValue(),56.0,0.0);
        objPr.setValue(23.0);
        assertEquals("minorTickLengthProperty cannot be bound", axis.minorTickLengthProperty().getValue(),23.0,0.0);
    }

    @Test public void checkMinorTickCountPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(56.0);
        axis.minorTickCountProperty().bind(objPr);
        assertEquals("minorTickCountProperty cannot be bound", axis.minorTickCountProperty().getValue(),56.0,0.0);
        objPr.setValue(23.0);
        assertEquals("minorTickCountProperty cannot be bound", axis.minorTickCountProperty().getValue(),23.0,0.0);
    }

    @Test public void checkScaleIsReadOnlyPropertyAndHenceCannotBeBound() {
        assertTrue(axis.scaleProperty() instanceof ReadOnlyDoubleProperty);
    }

    @Test public void checkUpperBoundPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(56.0);
        axis.upperBoundProperty().bind(objPr);
        assertEquals("upperBoundProperty cannot be bound", axis.upperBoundProperty().getValue(),56.0,0.0);
        objPr.setValue(23.0);
        assertEquals("upperBoundProperty cannot be bound", axis.upperBoundProperty().getValue(),23.0,0.0);
    }

    @Test public void checkLowerBoundPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(56.0);
        axis.lowerBoundProperty().bind(objPr);
        assertEquals("lowerBoundProperty cannot be bound", axis.lowerBoundProperty().getValue(),56.0,0.0);
        objPr.setValue(23.0);
        assertEquals("lowerBoundProperty cannot be bound", axis.lowerBoundProperty().getValue(),23.0,0.0);
    }

    @Test public void checkTickLabelFormatterPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<StringConverter<Number>>(null);
        axis.tickLabelFormatterProperty().bind(objPr);
        assertNull("tickLabelFormatterProperty cannot be bound", axis.tickLabelFormatterProperty().getValue());
        objPr.setValue(formatter);
        assertSame("tickLabelFormatterProperty cannot be bound", axis.tickLabelFormatterProperty().getValue(), formatter);
    }


    @Test public void minorTickVisiblePropertyHasBeanReference() {
        assertSame(axis, axis.minorTickVisibleProperty().getBean());
    }

    @Test public void minorTickVisiblePropertyHasName() {
        assertEquals("minorTickVisible", axis.minorTickVisibleProperty().getName());
    }

    @Test public void minorTickLengthPropertyHasBeanReference() {
        assertSame(axis, axis.minorTickLengthProperty().getBean());
    }

    @Test public void minorTickLengthPropertyHasName() {
        assertEquals("minorTickLength", axis.minorTickLengthProperty().getName());
    }

    @Test public void minorTickCountPropertyHasBeanReference() {
        assertSame(axis, axis.minorTickCountProperty().getBean());
    }

    @Test public void minorTickCountPropertyHasName() {
        assertEquals("minorTickCount", axis.minorTickCountProperty().getName());
    }

    @Test public void scalePropertyHasBeanReference() {
        assertSame(axis, axis.scaleProperty().getBean());
    }

    @Test public void scalePropertyHasName() {
        assertEquals("scale", axis.scaleProperty().getName());
    }

    @Test public void upperBoundPropertyHasBeanReference() {
        assertSame(axis, axis.upperBoundProperty().getBean());
    }

    @Test public void upperBoundPropertyHasName() {
        assertEquals("upperBound", axis.upperBoundProperty().getName());
    }

    @Test public void lowerBoundPropertyHasBeanReference() {
        assertSame(axis, axis.lowerBoundProperty().getBean());
    }

    @Test public void lowerBoundPropertyHasName() {
        assertEquals("lowerBound", axis.lowerBoundProperty().getName());
    }

    @Test public void tickLabelFormatterPropertyHasBeanReference() {
        assertSame(axis, axis.tickLabelFormatterProperty().getBean());
    }

    @Test public void tickLabelFormatterPropertyHasName() {
        assertEquals("tickLabelFormatter", axis.tickLabelFormatterProperty().getName());
    }




    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenMinorTickVisibleIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.minorTickVisibleProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        BooleanProperty other = new SimpleBooleanProperty();
        axis.minorTickVisibleProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenMinorTickVisibleIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.minorTickVisibleProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyMinorTickVisibleViaCSS() {
        ((StyleableProperty)axis.minorTickVisibleProperty()).applyStyle(null, Boolean.TRUE);
        assertSame(true, axis.isMinorTickVisible());
    }

    @Test public void whenMinorTickLengthIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.minorTickLengthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        DoubleProperty other = new SimpleDoubleProperty();
        axis.minorTickLengthProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenMinorTickLengthIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.minorTickLengthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyMinorTickLengthViaCSS() {
        ((StyleableProperty)axis.minorTickLengthProperty()).applyStyle(null, 10.34);
        assertEquals(10.34, axis.getMinorTickLength(), 0.0);
    }

    @Test public void whenMinorTickCountIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.minorTickCountProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        DoubleProperty other = new SimpleDoubleProperty();
        axis.minorTickCountProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenMinorTickCountIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.minorTickCountProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyMinorTickCountViaCSS() {
        ((StyleableProperty)axis.minorTickCountProperty()).applyStyle(null, 10);
        assertEquals(10, axis.getMinorTickCount(), 0.000001);
    }

    /*********************************************************************
     * Miscellaneous Tests                                         *
     ********************************************************************/

    @Test public void setMinorTickVisibleAndSeeValueIsReflectedInModel() {
        axis.setMinorTickVisible(false);
        assertFalse(axis.minorTickVisibleProperty().getValue());
    }

    @Test public void setMinorTickVisibleAndSeeValue() {
        axis.setMinorTickVisible(true);
        assertTrue(axis.isMinorTickVisible());
    }

    @Test public void setMinorTickLengthAndSeeValueIsReflectedInModel() {
        axis.setMinorTickLength(30.0);
        assertEquals(axis.minorTickLengthProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setMinorTickLengthAndSeeValue() {
        axis.setMinorTickLength(30.0);
        assertEquals(axis.getMinorTickLength(), 30.0, 0.0);
    }

    @Test public void setMinorTickCountAndSeeValueIsReflectedInModel() {
        axis.setMinorTickCount(30);
        assertEquals(axis.minorTickCountProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setMinorTickCountAndSeeValue() {
        axis.setMinorTickCount(30);
        assertEquals(axis.getMinorTickCount(), 30.0, 0.0);
    }

    @Test public void setScaleAndSeeValueIsReflectedInModel() {
        ValueAxisShim.setScale(axis, 30);
        assertEquals(axis.scaleProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setScaleTickCountAndSeeValue() {
        ValueAxisShim.setScale(axis, 30);
        assertEquals(axis.getScale(), 30.0, 0.0);
    }

    @Test public void setUpperBoundAndSeeValueIsReflectedInModel() {
        axis.setUpperBound(30);
        assertEquals(axis.upperBoundProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setUpperBoundAndSeeValue() {
        axis.setUpperBound(30);
        assertEquals(axis.getUpperBound(), 30.0, 0.0);
    }

    @Test public void setLowerBoundAndSeeValueIsReflectedInModel() {
        axis.setLowerBound(30);
        assertEquals(axis.lowerBoundProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setLowerBoundAndSeeValue() {
        axis.setLowerBound(30);
        assertEquals(axis.getLowerBound(), 30.0, 0.0);
    }
    @Test public void setTickLabelFormatterAndSeeValueIsReflectedInModel() {
        axis.setTickLabelFormatter(formatter);
        assertSame(axis.tickLabelFormatterProperty().getValue(), formatter);
    }

    @Test public void setTickLabelFormatterAndSeeValue() {
        axis.setTickLabelFormatter(formatter);
        assertSame(axis.getTickLabelFormatter(), formatter);
    }


    @Test
    public void testDisplayPositionOfValueInRange() {
        axis.setLowerBound(0);
        axis.setUpperBound(10);
        ValueAxisShim.setScale(axis, 1);

        assertEquals(5, axis.getDisplayPosition(5), 1e-10);
    }

    @Test
    public void testDisplayPositionOfValueOutOfRange() {
        axis.setLowerBound(0);
        axis.setUpperBound(10);
        ValueAxisShim.setScale(axis, 1);

        assertEquals(-1, axis.getDisplayPosition(-1), 1e-10);
        assertEquals(11, axis.getDisplayPosition(11), 1e-10);
    }

}
