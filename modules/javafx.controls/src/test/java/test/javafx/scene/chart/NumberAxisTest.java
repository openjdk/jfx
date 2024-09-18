/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleableProperty;
import javafx.geometry.Side;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.NumberAxisShim;
import javafx.util.StringConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * All public members of NumberAxis are tested here .
 * @author srikalyc
 */
public class NumberAxisTest {
    private NumberAxis axis;
    private NumberAxis threeValueAxis;
    private NumberAxis fourValueAxis;
    private StringConverter<Number> formatter;

    public NumberAxisTest() {
    }

    @BeforeEach
    public void setup() {
        if (axis == null) {
            axis = new NumberAxis();
        }
        if (threeValueAxis == null) {
            threeValueAxis = new NumberAxis(0.0, 100.0, 10.0);
        }
        if (fourValueAxis == null) {
            fourValueAxis = new NumberAxis("dummy", 0.0, 100.0, 10.0);
        }
        formatter = new StringConverter<>() {
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

    @Test
    public void checkForceZeroInRangePropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        axis.forceZeroInRangeProperty().bind(objPr);
        assertTrue(axis.forceZeroInRangeProperty().getValue(), "forceZeroInRange cannot be bound");
        objPr.setValue(false);
        assertFalse(axis.forceZeroInRangeProperty().getValue(), "forceZeroInRange cannot be bound");
    }

    @Test
    public void checkTickUnitPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(56.0);
        axis.tickUnitProperty().bind(objPr);
        assertEquals(axis.tickUnitProperty().getValue(), 56.0, 0.0, "tickUnitProperty cannot be bound");
        objPr.setValue(23.0);
        assertEquals(axis.tickUnitProperty().getValue(), 23.0, 0.0, "tickUnitProperty cannot be bound");
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

    @Test public void whenTickUnitIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.tickUnitProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        DoubleProperty other = new SimpleDoubleProperty();
        axis.tickUnitProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenTickUnitIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
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

    @Test public void testTicksWithCollapsedBounds() {
        axis.setLowerBound(5);
        axis.setUpperBound(5);

        List<Number> ticks = NumberAxisShim.calculateTickValues(axis, 0 /*unused*/, NumberAxisShim.getRange(axis));
        assertEquals(Arrays.asList(5d), ticks);
    }

    @Test public void testTicksWithIncorrectTickUnit() {
        axis.setLowerBound(0);
        axis.setUpperBound(5);
        axis.setTickUnit(-1);

        List<Number> ticks = NumberAxisShim.calculateTickValues(axis, 0 /*unused*/, NumberAxisShim.getRange(axis));
        assertEquals(Arrays.asList(0d, 5d), ticks);
    }

    @Test public void testTicksNoIntermediateTicksIfTickUnitIsLarge() {
        axis.setLowerBound(-0.1);
        axis.setUpperBound(5);
        axis.setTickUnit(6);

        List<Number> ticks = NumberAxisShim.calculateTickValues(axis, 0 /*unused*/, NumberAxisShim.getRange(axis));
        assertEquals(Arrays.asList(-0.1, 5d), ticks);
    }

    @Test public void testAxisWithFractionalBounds() {
        axis.setLowerBound(8.4);
        axis.setTickUnit(1);
        axis.setUpperBound(10);

        List<Number> ticks = NumberAxisShim.calculateTickValues(axis, 0 /*unused*/, NumberAxisShim.getRange(axis));
        assertEquals(Arrays.asList(8.4, 9d, 10d), ticks);
    }

    @Test public void testAxisWithFractionalBoundsMinorTicksAligned() {
        axis.setLowerBound(8.4);
        axis.setTickUnit(1);
        axis.setMinorTickCount(4);
        axis.setUpperBound(10.3);

        List<Number> ticks = NumberAxisShim.calculateMinorTickMarks(axis);
        assertEquals(Arrays.asList(8.5, 8.75, 9.25, 9.5, 9.75, 10.25), ticks);
    }

    @Test public void testAxisWithFractionalBoundsTickUnitFractional() {
        axis.setLowerBound(8.4);
        axis.setTickUnit(0.1);
        axis.setMinorTickCount(2);
        axis.setUpperBound(8.75);

        List<Number> ticks = NumberAxisShim.calculateTickValues(axis, 0 /*unused*/, NumberAxisShim.getRange(axis));
        assertEquals(Arrays.asList(8.4, 8.5, 8.6, 8.7, 8.75), ticks);

        // floating point calculation results in "8.450000000000001" instead of "8.45", so we need double array comparison
        List<Number> minorTicks = NumberAxisShim.calculateMinorTickMarks(axis);
        double [] asDoubleArray = minorTicks.stream().mapToDouble(Number::doubleValue).toArray();
        assertArrayEquals(new double[] {8.45, 8.55, 8.65}, asDoubleArray, 1e-10);
    }

    @Test
    @Timeout(value=1000, unit=TimeUnit.MILLISECONDS)
    public void testCloseValues() {
        axis.setForceZeroInRange(false);
        axis.setSide(Side.LEFT);
        double minValue = 1.0;
        double maxValue = minValue + Math.ulp(minValue);
        NumberAxisShim.autoRange(axis, minValue, maxValue, 500, 50);
    }

    @Test
    @Timeout(value=1000, unit=TimeUnit.MILLISECONDS)
    public void testCloseValuesMinorTicks() {
        axis.setForceZeroInRange(false);
        axis.setSide(Side.LEFT);
        double minValue = 1.0;
        double maxValue = minValue + 11 * Math.ulp(minValue);
        Object range = NumberAxisShim.autoRange(axis, minValue, maxValue, 500, 50);
        NumberAxisShim.setRange(axis, range, false);
        NumberAxisShim.calculateMinorTickMarks(axis);
    }

    @Test
    @Timeout(value=1000, unit=TimeUnit.MILLISECONDS)
    public void testEqualLargeValues() {
        axis.setForceZeroInRange(false);
        axis.setSide(Side.LEFT);
        double minValue = Math.pow(2, 52); // ulp == 1.0
        double maxValue = minValue;
        NumberAxisShim.autoRange(axis, minValue, maxValue, 500, 50);
    }

    @Test
    @Timeout(value=1000, unit=TimeUnit.MILLISECONDS)
    public void testCloseValuesNoAutorange() {
        axis.setForceZeroInRange(false);
        axis.setSide(Side.LEFT);
        axis.setAutoRanging(false);
        double minValue = 1.0;
        double maxValue = minValue + Math.ulp(minValue);
        axis.setLowerBound(minValue);
        axis.setUpperBound(maxValue);
        // minValue + tickUnit == minValue
        axis.setTickUnit(0.5 * Math.ulp(minValue));
        Object range = NumberAxisShim.getRange(axis);
        NumberAxisShim.calculateTickValues(axis, 500, range);
        NumberAxisShim.calculateMinorTickMarks(axis);
    }

    @Test
    @Timeout(value=1000, unit=TimeUnit.MILLISECONDS)
    public void testCloseValuesMinorTicksNoAutoRange() {
        axis.setForceZeroInRange(false);
        axis.setSide(Side.LEFT);
        axis.setAutoRanging(false);
        double minValue = 1.0;
        double maxValue = minValue + Math.ulp(minValue);
        axis.setLowerBound(minValue);
        axis.setUpperBound(maxValue);
        axis.setTickUnit(Math.ulp(minValue));
        Object range = NumberAxisShim.getRange(axis);
        NumberAxisShim.calculateTickValues(axis, 500, range);
        NumberAxisShim.calculateMinorTickMarks(axis);
    }
}
