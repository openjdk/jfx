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

import javafx.css.CssMetaData;

import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Side;
import javafx.scene.chart.Axis;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

/**
 * All public members of Axis are tested here .
 */
public class AxisTest {
    private Axis axis;//Empty string
    private AxisHelper helper;

    public AxisTest() {
        helper = new AxisHelper();
    }

    @Before public void setup() {
        if (axis == null) {
            axis = helper.getDummyAxis();
        }
        helper.setAxis(axis);
    }



    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultSideNull() {
        assertNull(axis.getSide());
    }

    @Test public void defaultTickMarkVisibleIsTrue() {
        assertTrue(axis.isTickMarkVisible());
    }

    @Test public void defaultTickLabelsVisibleIsTrue() {
        assertTrue(axis.isTickLabelsVisible());
    }


    @Test public void defaultTickLength() {
        assertEquals(axis.getTickLength(), 8.0 , 0.0);
    }

    @Test public void defaultAutoRangingIsTrue() {
        assertTrue(axis.isAutoRanging());
    }

    @Test public void defaultTickLabelFont() {
        assertEquals(axis.getTickLabelFont(), Font.font("System", 8));
    }

    @Test public void defaultTickLabelFill() {
        assertSame(axis.getTickLabelFill(), Color.BLACK );
    }

    @Test public void defaultTickLabelGap() {
        assertEquals(axis.getTickLabelGap(), 3.0 , 0.0);
    }

    @Test public void defaultAnimatedIsTrue() {
        assertTrue(axis.getAnimated());
    }

    @Test public void defaultTickLabelRotation() {
        assertEquals(axis.getTickLabelRotation(), 0.0 , 0.0);
    }


    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void checkSidePropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Side>(Side.TOP);
        axis.sideProperty().bind(objPr);
        assertSame("side cannot be bound", axis.sideProperty().getValue(), Side.TOP);
        objPr.setValue(Side.BOTTOM);
        assertSame("side cannot be bound", axis.sideProperty().getValue(), Side.BOTTOM);
    }

    @Test public void checkTickMarkVisiblePropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        axis.tickMarkVisibleProperty().bind(objPr);
        assertTrue("tickMarkVisibleProperty cannot be bound", axis.tickMarkVisibleProperty().getValue());
        objPr.setValue(false);
        assertFalse("tickMarkVisibleProperty cannot be bound", axis.tickMarkVisibleProperty().getValue());
    }

    @Test public void checkTickLabelsVisiblePropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        axis.tickLabelsVisibleProperty().bind(objPr);
        assertTrue("tickLabelsVisibleProperty cannot be bound", axis.tickLabelsVisibleProperty().getValue());
        objPr.setValue(false);
        assertFalse("tickLabelsVisibleProperty cannot be bound", axis.tickLabelsVisibleProperty().getValue());
    }

    @Test public void checkTickLengthPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(56.0);
        axis.tickLengthProperty().bind(objPr);
        assertEquals("tickLengthProperty cannot be bound", axis.tickLengthProperty().getValue(),56.0,0.0);
        objPr.setValue(23.0);
        assertEquals("tickLengthProperty cannot be bound", axis.tickLengthProperty().getValue(),23.0,0.0);
    }

    @Test public void checkAutoRangingPropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        axis.autoRangingProperty().bind(objPr);
        assertTrue("autoRangingProperty cannot be bound", axis.autoRangingProperty().getValue());
        objPr.setValue(false);
        assertFalse("autoRangingProperty cannot be bound", axis.autoRangingProperty().getValue());
    }

    @Test public void checkTickLabelFontPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Font>(null);
        axis.tickLabelFontProperty().bind(objPr);
        assertNull("tickLabelFontProperty cannot be bound", axis.tickLabelFontProperty().getValue());
        objPr.setValue(Font.getDefault());
        assertSame("tickLabelFontProperty cannot be bound", axis.tickLabelFontProperty().getValue(), Font.getDefault());
    }

    @Test public void checkTickLabelFillPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Color>(Color.WHEAT);
        axis.tickLabelFillProperty().bind(objPr);
        assertSame("tickLabelFillProperty cannot be bound", axis.tickLabelFillProperty().getValue(), Color.WHEAT);
        objPr.setValue(Color.BLUE);
        assertSame("tickLabelFillProperty cannot be bound", axis.tickLabelFillProperty().getValue(), Color.BLUE);
    }

    @Test public void checkTickLabelGapPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(56.0);
        axis.tickLabelGapProperty().bind(objPr);
        assertEquals("tickLabelGapProperty cannot be bound", axis.tickLabelGapProperty().getValue(),56.0,0.0);
        objPr.setValue(23.0);
        assertEquals("tickLabelGapProperty cannot be bound", axis.tickLabelGapProperty().getValue(),23.0,0.0);
    }

    @Test public void checkAnimatedPropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        axis.animatedProperty().bind(objPr);
        assertTrue("animatedProperty cannot be bound", axis.animatedProperty().getValue());
        objPr.setValue(false);
        assertFalse("animatedProperty cannot be bound", axis.animatedProperty().getValue());
    }

    @Test public void checkTickLabelRotationPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(56.0);
        axis.tickLabelRotationProperty().bind(objPr);
        assertEquals("tickLabelRotationProperty cannot be bound", axis.tickLabelRotationProperty().getValue(),56.0,0.0);
        objPr.setValue(23.0);
        assertEquals("tickLabelRotationProperty cannot be bound", axis.tickLabelRotationProperty().getValue(),23.0,0.0);
    }



    @Test public void sidePropertyHasBeanReference() {
        assertSame(axis, axis.sideProperty().getBean());
    }

    @Test public void sidePropertyHasName() {
        assertEquals("side", axis.sideProperty().getName());
    }

    @Test public void tickMarkVisiblePropertyHasBeanReference() {
        assertSame(axis, axis.tickMarkVisibleProperty().getBean());
    }

    @Test public void tickMarkVisiblePropertyHasName() {
        assertEquals("tickMarkVisible", axis.tickMarkVisibleProperty().getName());
    }

    @Test public void tickLabelsVisiblePropertyHasBeanReference() {
        assertSame(axis, axis.tickLabelsVisibleProperty().getBean());
    }

    @Test public void tickLabelsVisiblePropertyHasName() {
        assertEquals("tickLabelsVisible", axis.tickLabelsVisibleProperty().getName());
    }

    @Test public void tickLengthPropertyHasBeanReference() {
        assertSame(axis, axis.tickLengthProperty().getBean());
    }

    @Test public void tickLengthPropertyHasName() {
        assertEquals("tickLength", axis.tickLengthProperty().getName());
    }

    @Test public void autoRangingPropertyHasBeanReference() {
        assertSame(axis, axis.autoRangingProperty().getBean());
    }

    @Test public void autoRangingPropertyHasName() {
        assertEquals("autoRanging", axis.autoRangingProperty().getName());
    }

    @Test public void tickLabelFontPropertyHasBeanReference() {
        assertSame(axis, axis.tickLabelFontProperty().getBean());
    }

    @Test public void tickLabelFontPropertyHasName() {
        assertEquals("tickLabelFont", axis.tickLabelFontProperty().getName());
    }

    @Test public void tickLabelFillPropertyHasBeanReference() {
        assertSame(axis, axis.tickLabelFillProperty().getBean());
    }

    @Test public void tickLabelFillPropertyHasName() {
        assertEquals("tickLabelFill", axis.tickLabelFillProperty().getName());
    }

    @Test public void tickLabelGapPropertyHasBeanReference() {
        assertSame(axis, axis.tickLabelGapProperty().getBean());
    }

    @Test public void tickLabelGapPropertyHasName() {
        assertEquals("tickLabelGap", axis.tickLabelGapProperty().getName());
    }

    @Test public void animatedPropertyHasBeanReference() {
        assertSame(axis, axis.animatedProperty().getBean());
    }

    @Test public void animatedPropertyHasName() {
        assertEquals("animated", axis.animatedProperty().getName());
    }

    @Test public void tickLabelRotationPropertyHasBeanReference() {
        assertSame(axis, axis.tickLabelRotationProperty().getBean());
    }

    @Test public void tickLabelRotationPropertyHasName() {
        assertEquals("tickLabelRotation", axis.tickLabelRotationProperty().getName());
    }



    /*********************************************************************
     * Check for Pseudo classes                                          *
     ********************************************************************/
    @Test public void settingSideTopSetsTopAndClearsOther3SidesPseudoClass() {
        axis.setSide(Side.TOP);
        assertPseudoClassExists(axis, "top");
        assertPseudoClassDoesNotExist(axis, "left");
        assertPseudoClassDoesNotExist(axis, "right");
        assertPseudoClassDoesNotExist(axis, "bottom");
    }

    @Test public void settingSideLeftSetsLeftAndClearsOther3SidesPseudoClass() {
        axis.setSide(Side.LEFT);
        assertPseudoClassExists(axis, "left");
        assertPseudoClassDoesNotExist(axis, "top");
        assertPseudoClassDoesNotExist(axis, "right");
        assertPseudoClassDoesNotExist(axis, "bottom");
    }

    @Test public void settingSideRightSetsRightAndClearsOther3SidesPseudoClass() {
        axis.setSide(Side.RIGHT);
        assertPseudoClassExists(axis, "right");
        assertPseudoClassDoesNotExist(axis, "left");
        assertPseudoClassDoesNotExist(axis, "top");
        assertPseudoClassDoesNotExist(axis, "bottom");
    }

    @Test public void settingSideBottomSetsBottomAndClearsOther3SidesPseudoClass() {
        axis.setSide(Side.BOTTOM);
        assertPseudoClassExists(axis, "bottom");
        assertPseudoClassDoesNotExist(axis, "left");
        assertPseudoClassDoesNotExist(axis, "right");
        assertPseudoClassDoesNotExist(axis, "top");
    }



    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenSideIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.sideProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        ObjectProperty<Side> other = new SimpleObjectProperty<Side>(Side.LEFT);
        axis.sideProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenSideIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.sideProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifySideViaCSS() {
        ((StyleableProperty)axis.sideProperty()).applyStyle(null, Side.BOTTOM);
        assertSame(Side.BOTTOM, axis.getSide());
    }

    @Test public void whenTickMarkVisibleIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.tickMarkVisibleProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        BooleanProperty other = new SimpleBooleanProperty();
        axis.tickMarkVisibleProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenTickMarkVisibleIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.tickMarkVisibleProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyTickMarkVisibleViaCSS() {
        ((StyleableProperty)axis.tickMarkVisibleProperty()).applyStyle(null, Boolean.TRUE);
        assertSame(true, axis.isTickMarkVisible());
    }

    @Test public void whenTickLabelsVisibleIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.tickLabelsVisibleProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        BooleanProperty other = new SimpleBooleanProperty();
        axis.tickLabelsVisibleProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenTickLabelsVisibleIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.tickLabelsVisibleProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyTickLabelsVisibleViaCSS() {
        ((StyleableProperty)axis.tickLabelsVisibleProperty()).applyStyle(null, Boolean.TRUE);
        assertSame(true, axis.isTickMarkVisible());
    }

    @Test public void whenTickLengthIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.tickLengthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        DoubleProperty other = new SimpleDoubleProperty();
        axis.tickLengthProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenTickLengthIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.tickLengthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyTickLengthViaCSS() {
        ((StyleableProperty)axis.tickLengthProperty()).applyStyle(null, 10.34);
        assertEquals(10.34, axis.tickLengthProperty().get(), 0.000001);
    }

    @Test public void whenTickLabelFontIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.tickLabelFontProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        ObjectProperty<Font> other = new SimpleObjectProperty<Font>(Font.getDefault());
        axis.tickLabelFontProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenTickLabelFontIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.tickLabelFontProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyTickLabelFontViaCSS() {
        ((StyleableProperty)axis.tickLabelFontProperty()).applyStyle(null, Font.getDefault());
        assertSame(Font.getDefault(), axis.getTickLabelFont());
    }

    @Test public void whenTickLabelFillIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.tickLabelFillProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        ObjectProperty<Color> other = new SimpleObjectProperty<Color>(Color.BROWN);
        axis.tickLabelFillProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenTickLabelFillIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.tickLabelFillProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyTickLabelFillViaCSS() {
        ((StyleableProperty)axis.tickLabelFillProperty()).applyStyle(null, Color.BROWN);
        assertSame(Color.BROWN, axis.getTickLabelFill());
    }

    @Test public void whenTickLabelGapIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)axis.tickLabelGapProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
        DoubleProperty other = new SimpleDoubleProperty();
        axis.tickLabelGapProperty().bind(other);
        assertFalse(styleable.isSettable(axis));
    }

    @Test public void whenTickLabelGapIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)axis.tickLabelGapProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(axis));
    }

    @Test public void canSpecifyTickLabelGapViaCSS() {
        ((StyleableProperty)axis.tickLabelGapProperty()).applyStyle(null, 10.34);
        assertEquals(10.34, axis.getTickLabelGap(), 0.0);
    }



    /*********************************************************************
     * Miscellaneous Tests                                         *
     ********************************************************************/
    @Test public void setSideAndSeeValueIsReflectedInModel() {
        axis.setSide(Side.BOTTOM);
        assertSame(axis.sideProperty().getValue(), Side.BOTTOM);
    }

    @Test public void setHbarPolicyAndSeeValue() {
        axis.setSide(Side.TOP);
        assertSame(axis.getSide(), Side.TOP);
    }

    @Test public void setTickMarkVisibleAndSeeValueIsReflectedInModel() {
        axis.setTickMarkVisible(false);
        assertFalse(axis.tickMarkVisibleProperty().getValue());
    }

    @Test public void setTickMarkVisibleAndSeeValue() {
        axis.setTickMarkVisible(true);
        assertTrue(axis.isTickMarkVisible());
    }

    @Test public void setTickLabelsVisibleAndSeeValueIsReflectedInModel() {
        axis.setTickLabelsVisible(false);
        assertFalse(axis.tickLabelsVisibleProperty().getValue());
    }

    @Test public void setTickLabelsVisibleAndSeeValue() {
        axis.setTickLabelsVisible(true);
        assertTrue(axis.isTickLabelsVisible());
    }

    @Test public void setTickLengthAndSeeValueIsReflectedInModel() {
        axis.setTickLength(30.0);
        assertEquals(axis.tickLengthProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setTickLengthAndSeeValue() {
        axis.setTickLength(30.0);
        assertEquals(axis.getTickLength(), 30.0, 0.0);
    }

    @Test public void setAutoRangingAndSeeValueIsReflectedInModel() {
        axis.setAutoRanging(false);
        assertFalse(axis.autoRangingProperty().getValue());
    }

    @Test public void setAutoRangingAndSeeValue() {
        axis.setAutoRanging(true);
        assertTrue(axis.isAutoRanging());
    }

    @Test public void setTickLabelFontAndSeeValueIsReflectedInModel() {
        axis.setTickLabelFont(Font.getDefault());
        assertSame(axis.tickLabelFontProperty().getValue(), Font.getDefault());
    }

    @Test public void setTickLabelFontAndSeeValue() {
        axis.setTickLabelFont(Font.getDefault());
        assertSame(axis.getTickLabelFont(), Font.getDefault());
    }

    @Test public void setTickLabelFillAndSeeValueIsReflectedInModel() {
        axis.setTickLabelFill(Color.AQUA);
        assertSame(axis.tickLabelFillProperty().getValue(), Color.AQUA);
    }

    @Test public void setTickLabelFillAndSeeValue() {
        axis.setTickLabelFill(Color.AQUA);
        assertSame(axis.getTickLabelFill(), Color.AQUA);
    }

    @Test public void setTickLabelGapAndSeeValueIsReflectedInModel() {
        axis.setTickLabelGap(30.0);
        assertEquals(axis.tickLabelGapProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setTickLabelGapAndSeeValue() {
        axis.setTickLabelGap(30.0);
        assertEquals(axis.getTickLabelGap(), 30.0, 0.0);
    }

    @Test public void setAnimatedAndSeeValueIsReflectedInModel() {
        axis.setAnimated(false);
        assertFalse(axis.animatedProperty().getValue());
    }

    @Test public void setAnimatedAndSeeValue() {
        axis.setAnimated(true);
        assertTrue(axis.getAnimated());
    }

    @Test public void setTickLabelRotationAndSeeValueIsReflectedInModel() {
        axis.setTickLabelRotation(30.0);
        assertEquals(axis.tickLabelRotationProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setTickLabelRotationAndSeeValue() {
        axis.setTickLabelRotation(30.0);
        assertEquals(axis.getTickLabelRotation(), 30.0, 0.0);
    }


}
