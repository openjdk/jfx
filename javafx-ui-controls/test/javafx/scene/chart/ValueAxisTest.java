/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.chart;

import com.sun.javafx.css.CssMetaData;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
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
    private ValueAxisHelper helper;
    private StringConverter<Number> formatter;
    
    public ValueAxisTest() {
        helper = new ValueAxisHelper();
    }
    
    @Before public void setup() {
        if (axis == null) {
            axis = helper.getDummyValueAxis();
        }
        if (twoValueAxis == null) {
            twoValueAxis = helper.getDummyTwoArgValueAxis();
        }
        helper.setValueAxis(axis);
        helper.setTwoValueAxis(twoValueAxis);
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
    @Test public void whenMinorTickVisibleIsBound_impl_cssSettable_ReturnsFalse() {
        CssMetaData styleable = CssMetaData.getCssMetaData(axis.minorTickVisibleProperty());
        assertTrue(styleable.isSettable(axis));
        BooleanProperty other = new SimpleBooleanProperty();
        axis.minorTickVisibleProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenMinorTickVisibleIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        CssMetaData styleable = CssMetaData.getCssMetaData(axis.minorTickVisibleProperty());
        styleable.set(axis,false);
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyMinorTickVisibleViaCSS() {
        CssMetaData styleable = CssMetaData.getCssMetaData(axis.minorTickVisibleProperty());
        styleable.set(axis,true);
        assertSame(true, axis.isMinorTickVisible());
    }

    @Test public void whenMinorTickLengthIsBound_impl_cssSettable_ReturnsFalse() {
        CssMetaData styleable = CssMetaData.getCssMetaData(axis.minorTickLengthProperty());
        assertTrue(styleable.isSettable(axis));
        DoubleProperty other = new SimpleDoubleProperty();
        axis.minorTickLengthProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenMinorTickLengthIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        CssMetaData styleable = CssMetaData.getCssMetaData(axis.minorTickLengthProperty());
        styleable.set(axis,10.9);
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyMinorTickLengthViaCSS() {
        CssMetaData styleable = CssMetaData.getCssMetaData(axis.minorTickLengthProperty());
        styleable.set(axis,10.34);
        assertEquals(10.34, axis.getMinorTickLength(), 0.0);
    }

    @Test public void whenMinorTickCountIsBound_impl_cssSettable_ReturnsFalse() {
        CssMetaData styleable = CssMetaData.getCssMetaData(axis.minorTickCountProperty());
        assertTrue(styleable.isSettable(axis));
        DoubleProperty other = new SimpleDoubleProperty();
        axis.minorTickCountProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenMinorTickCountIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        CssMetaData styleable = CssMetaData.getCssMetaData(axis.minorTickCountProperty());
        styleable.set(axis,10);
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyMinorTickCountViaCSS() {
        CssMetaData styleable = CssMetaData.getCssMetaData(axis.minorTickCountProperty());
        styleable.set(axis,10);
        assertTrue(styleable.isSettable(axis));
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
        axis.setScale(30);
        assertEquals(axis.scaleProperty().getValue(), 30.0, 0.0);
    }
    
    @Test public void setScaleTickCountAndSeeValue() {
        axis.setScale(30);
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
    
}
