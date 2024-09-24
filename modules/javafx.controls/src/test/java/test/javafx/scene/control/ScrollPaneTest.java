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

package test.javafx.scene.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassDoesNotExist;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassExists;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleableProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.pgstub.StubToolkit;

/**
 *
 * @author srikalyc
 */
public class ScrollPaneTest {
    private ScrollPane scrollPane;

    @BeforeEach
    public void setup() {
        assertTrue(Toolkit.getToolkit() instanceof StubToolkit);  // Ensure StubToolkit is loaded

        scrollPane = new ScrollPane();
    }



    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultConstructorShouldSetStyleClassTo_scrollpane() {
        assertStyleClassContains(scrollPane, "scroll-pane");
    }

    @Test public void defaultFocusTraversibleIsFalse() {
        assertFalse(scrollPane.isFocusTraversable());
    }

    @Test public void defaultHBarPolicy() {
        assertSame(scrollPane.getHbarPolicy(), ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    @Test public void defaultVBarPolicy() {
        assertSame(scrollPane.getVbarPolicy(), ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    @Test public void defaultHvalue() {
        assertEquals(scrollPane.getHvalue(), 0.0 , 0.0);
    }

    @Test public void defaultHmin() {
        assertEquals(scrollPane.getHmin(), 0.0 , 0.0);
    }

    @Test public void defaultHmax() {
        assertEquals(scrollPane.getHmax(), 1.0 , 0.0);
    }

    @Test public void defaultVvalue() {
        assertEquals(scrollPane.getVvalue(), 0.0 , 0.0);
    }

    @Test public void defaultVmin() {
        assertEquals(scrollPane.getVmin(), 0.0 , 0.0);
    }

    @Test public void defaultVmax() {
        assertEquals(scrollPane.getVmax(), 1.0 , 0.0);
    }

    @Test public void defaultFitToWidth() {
        assertEquals(scrollPane.isFitToWidth(), false);
    }

    @Test public void defaultFitToHeight() {
        assertEquals(scrollPane.isFitToHeight(), false);
    }

    @Test public void defaultPannable() {
        assertEquals(scrollPane.isPannable(), false);
    }

    @Test public void defaultPreferredViewportWidth() {
        assertEquals(scrollPane.getPrefViewportWidth(), 0.0, 0.0);
    }

    @Test public void defaultPreferredViewportHeight() {
        assertEquals(scrollPane.getPrefViewportHeight(), 0.0, 0.0);
    }

    @Test public void defaultViewportBounds() {
        assertEquals(scrollPane.getViewportBounds(), new BoundingBox(0.0, 0.0, 0.0, 0.0));
    }

    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void checkHBarPolicyPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<>(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.hbarPolicyProperty().bind(objPr);
        assertSame(scrollPane.hbarPolicyProperty().getValue(), ScrollPane.ScrollBarPolicy.ALWAYS, "HBarPolicyProperty cannot be bound");
        objPr.setValue(ScrollPane.ScrollBarPolicy.NEVER);
        assertSame(scrollPane.hbarPolicyProperty().getValue(), ScrollPane.ScrollBarPolicy.NEVER, "HBarPolicyProperty cannot be bound");
    }

    @Test public void checkVBarPolicyPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<>(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.vbarPolicyProperty().bind(objPr);
        assertSame(scrollPane.vbarPolicyProperty().getValue(), ScrollPane.ScrollBarPolicy.ALWAYS, "VBarPolicyProperty cannot be bound");
        objPr.setValue(ScrollPane.ScrollBarPolicy.NEVER);
        assertSame(scrollPane.vbarPolicyProperty().getValue(), ScrollPane.ScrollBarPolicy.NEVER, "VBarPolicyProperty cannot be bound");
    }

    @Test public void checkHValuePropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.hvalueProperty().bind(objPr);
        assertEquals(scrollPane.hvalueProperty().getValue(), 2.0, 0.0, "hvalueProperty cannot be bound");
        objPr.setValue(5.0);
        assertEquals(scrollPane.hvalueProperty().getValue(), 5.0, 0.0, "hvalueProperty cannot be bound");
    }

    @Test public void checkHminPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.hminProperty().bind(objPr);
        assertEquals(scrollPane.hminProperty().getValue(), 2.0, 0.0, "hminProperty cannot be bound");
        objPr.setValue(5.0);
        assertEquals(scrollPane.hminProperty().getValue(), 5.0, 0.0, "hminProperty cannot be bound");
    }

    @Test public void checkHmaxPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.hmaxProperty().bind(objPr);
        assertEquals(scrollPane.hmaxProperty().getValue(), 2.0, 0.0, "hmaxProperty cannot be bound");
        objPr.setValue(5.0);
        assertEquals(scrollPane.hmaxProperty().getValue(), 5.0, 0.0, "hmaxProperty cannot be bound");
    }

    @Test public void checkVValuePropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.vvalueProperty().bind(objPr);
        assertEquals(scrollPane.vvalueProperty().getValue(), 2.0, 0.0, "vvalueProperty cannot be bound");
        objPr.setValue(5.0);
        assertEquals(scrollPane.vvalueProperty().getValue(), 5.0, 0.0, "vvalueProperty cannot be bound");
    }

    @Test public void checkVminPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.vminProperty().bind(objPr);
        assertEquals(scrollPane.vminProperty().getValue(), 2.0, 0.0, "vminProperty cannot be bound");
        objPr.setValue(5.0);
        assertEquals(scrollPane.vminProperty().getValue(), 5.0, 0.0, "vminProperty cannot be bound");
    }

    @Test public void checkVmaxPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.vmaxProperty().bind(objPr);
        assertEquals(scrollPane.vmaxProperty().getValue(), 2.0, 0.0, "vmaxProperty cannot be bound");
        objPr.setValue(5.0);
        assertEquals(scrollPane.vmaxProperty().getValue(), 5.0, 0.0, "vmaxProperty cannot be bound");
    }

    @Test public void checkFitToWidthPropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        scrollPane.fitToWidthProperty().bind(objPr);
        assertEquals(scrollPane.fitToWidthProperty().getValue(), true, "FitToWidth cannot be bound");
        objPr.setValue(false);
        assertEquals(scrollPane.fitToWidthProperty().getValue(), false, "FitToWidth cannot be bound");
    }

    @Test public void checkFitToHeigtPropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        scrollPane.fitToHeightProperty().bind(objPr);
        assertEquals(scrollPane.fitToHeightProperty().getValue(), true, "FitToHeigt cannot be bound");
        objPr.setValue(false);
        assertEquals(scrollPane.fitToHeightProperty().getValue(), false, "FitToHeigt cannot be bound");
    }

    @Test public void checkPannablePropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        scrollPane.pannableProperty().bind(objPr);
        assertEquals(scrollPane.pannableProperty().getValue(), true, "Pannable cannot be bound");
        objPr.setValue(false);
        assertEquals(scrollPane.pannableProperty().getValue(), false, "Pannable cannot be bound");
    }

    @Test public void checkPreferredViewportWidthBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.prefViewportWidthProperty().bind(objPr);
        assertEquals(scrollPane.prefViewportWidthProperty().getValue(), 2.0, 0.0, "prefViewportWidthProperty cannot be bound");
        objPr.setValue(5.0);
        assertEquals(scrollPane.prefViewportWidthProperty().getValue(), 5.0, 0.0, "prefViewportWidthProperty cannot be bound");
    }

    @Test public void checkPreferredViewportHeightBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.prefViewportHeightProperty().bind(objPr);
        assertEquals(scrollPane.prefViewportHeightProperty().getValue(), 2.0, 0.0, "prefViewportHeightProperty cannot be bound");
        objPr.setValue(5.0);
        assertEquals(scrollPane.prefViewportHeightProperty().getValue(), 5.0, 0.0, "prefViewportHeightProperty cannot be bound");
    }

    @Test public void checkViewportBoundsBind() {
        Bounds b = null;
        ObjectProperty objPr = new SimpleObjectProperty<>(b);
        scrollPane.viewportBoundsProperty().bind(objPr);
        assertNull(scrollPane.viewportBoundsProperty().getValue(), "viewportBoundsProperty cannot be bound");
        b = new BoundingBox(0.0, 0.0, 0.0, 0.0);
        objPr.setValue(b);
        assertSame(scrollPane.viewportBoundsProperty().getValue(), b, "viewportBoundsProperty cannot be bound");
    }

    @Test public void hbarPolicyPropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.hbarPolicyProperty().getBean());
    }

    @Test public void hbarPolicyPropertyHasName() {
        assertEquals("hbarPolicy", scrollPane.hbarPolicyProperty().getName());
    }

    @Test public void vbarPolicyPropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.vbarPolicyProperty().getBean());
    }

    @Test public void vbarPolicyPropertyHasName() {
        assertEquals("vbarPolicy", scrollPane.vbarPolicyProperty().getName());
    }

    @Test public void hvaluePropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.hvalueProperty().getBean());
    }

    @Test public void hvaluePropertyHasName() {
        assertEquals("hvalue", scrollPane.hvalueProperty().getName());
    }

    @Test public void hminPropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.hminProperty().getBean());
    }

    @Test public void hminPropertyHasName() {
        assertEquals("hmin", scrollPane.hminProperty().getName());
    }

    @Test public void hmaxPropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.hmaxProperty().getBean());
    }

    @Test public void hmaxPropertyHasName() {
        assertEquals("hmax", scrollPane.hmaxProperty().getName());
    }

    @Test public void vvaluePropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.vvalueProperty().getBean());
    }

    @Test public void vvaluePropertyHasName() {
        assertEquals("vvalue", scrollPane.vvalueProperty().getName());
    }

    @Test public void vminPropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.vminProperty().getBean());
    }

    @Test public void vminPropertyHasName() {
        assertEquals("vmin", scrollPane.vminProperty().getName());
    }

    @Test public void vmaxPropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.vmaxProperty().getBean());
    }

    @Test public void vmaxPropertyHasName() {
        assertEquals("vmax", scrollPane.vmaxProperty().getName());
    }

    @Test public void fitToWidthPropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.fitToWidthProperty().getBean());
    }

    @Test public void fitToWidthPropertyHasName() {
        assertEquals("fitToWidth", scrollPane.fitToWidthProperty().getName());
    }

    @Test public void fitToHeightPropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.fitToHeightProperty().getBean());
    }

    @Test public void fitToHeightPropertyHasName() {
        assertEquals("fitToHeight", scrollPane.fitToHeightProperty().getName());
    }

    @Test public void pannablePropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.pannableProperty().getBean());
    }

    @Test public void pannablePropertyHasName() {
        assertEquals("pannable", scrollPane.pannableProperty().getName());
    }

    @Test public void prefViewportWidthPropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.prefViewportWidthProperty().getBean());
    }

    @Test public void prefViewportWidthPropertyHasName() {
        assertEquals("prefViewportWidth", scrollPane.prefViewportWidthProperty().getName());
    }

    @Test public void prefViewportHeightPropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.prefViewportHeightProperty().getBean());
    }

    @Test public void prefViewportHeightPropertyHasName() {
        assertEquals("prefViewportHeight", scrollPane.prefViewportHeightProperty().getName());
    }

    @Test public void viewportBoundsPropertyHasBeanReference() {
        assertSame(scrollPane, scrollPane.viewportBoundsProperty().getBean());
    }

    @Test public void viewportBoundsPropertyHasName() {
        assertEquals("viewportBounds", scrollPane.viewportBoundsProperty().getName());
    }



    /*********************************************************************
     * Check for Pseudo classes                                          *
     ********************************************************************/
    @Test public void settingFitToWidthSetsPseudoClass() {
        scrollPane.setFitToWidth(true);
        assertPseudoClassExists(scrollPane, "fitToWidth");
    }

    @Test public void clearingFitToWidthClearsPseudoClass() {
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToWidth(false);
        assertPseudoClassDoesNotExist(scrollPane, "fitToWidth");
    }

    @Test public void settingFitToHeightSetsPseudoClass() {
        scrollPane.setFitToHeight(true);
        assertPseudoClassExists(scrollPane, "fitToHeight");
    }

    @Test public void clearingFitToHeightClearsPseudoClass() {
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToHeight(false);
        assertPseudoClassDoesNotExist(scrollPane, "fitToHeight");
    }

    @Test public void settingPannableSetsPseudoClass() {
        scrollPane.setPannable(true);
        assertPseudoClassExists(scrollPane, "pannable");
    }

    @Test public void clearingPannableClearsPseudoClass() {
        scrollPane.setPannable(true);
        scrollPane.setPannable(false);
        assertPseudoClassDoesNotExist(scrollPane, "pannable");
    }


    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenHbarPolicyIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)scrollPane.hbarPolicyProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollPane));
        ObjectProperty<ScrollPane.ScrollBarPolicy> other = new SimpleObjectProperty<>(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.hbarPolicyProperty().bind(other);
        assertFalse(styleable.isSettable(scrollPane));
    }

    @Test public void whenHbarPolicyIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)scrollPane.hbarPolicyProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollPane));
    }

    @Test public void canSpecifyHbarPolicyViaCSS() {
        ((StyleableProperty)scrollPane.hbarPolicyProperty()).applyStyle(null, ScrollPane.ScrollBarPolicy.NEVER);
        assertSame(ScrollPane.ScrollBarPolicy.NEVER, scrollPane.hbarPolicyProperty().get());
    }

    @Test public void whenVbarPolicyIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)scrollPane.vbarPolicyProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollPane));
        ObjectProperty<ScrollPane.ScrollBarPolicy> other = new SimpleObjectProperty<>(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.vbarPolicyProperty().bind(other);
        assertFalse(styleable.isSettable(scrollPane));
    }

    @Test public void whenVbarPolicyIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)scrollPane.vbarPolicyProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollPane));
    }

    @Test public void canSpecifyVbarPolicyViaCSS() {
        ((StyleableProperty)scrollPane.vbarPolicyProperty()).applyStyle(null, ScrollPane.ScrollBarPolicy.NEVER);
        assertSame(ScrollPane.ScrollBarPolicy.NEVER, scrollPane.getVbarPolicy());
    }

    @Test public void whenFitToWidthIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)scrollPane.fitToWidthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollPane));
        BooleanProperty other = new SimpleBooleanProperty();
        scrollPane.fitToWidthProperty().bind(other);
        assertFalse(styleable.isSettable(scrollPane));
    }

    @Test public void whenFitToWidthIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)scrollPane.fitToWidthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollPane));
    }

    @Test public void canSpecifyFitToWidthViaCSS() {
        ((StyleableProperty)scrollPane.fitToWidthProperty()).applyStyle(null, Boolean.TRUE);
        assertSame(true, scrollPane.isFitToWidth());
    }

    @Test public void whenFitToHeightIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)scrollPane.fitToHeightProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollPane));
        BooleanProperty other = new SimpleBooleanProperty();
        scrollPane.fitToHeightProperty().bind(other);
        assertFalse(styleable.isSettable(scrollPane));
    }

    @Test public void whenFitToHeightIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)scrollPane.fitToHeightProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollPane));
    }

    @Test public void canSpecifyFitToHeightViaCSS() {
        ((StyleableProperty)scrollPane.fitToHeightProperty()).applyStyle(null, Boolean.TRUE);
        assertSame(true, scrollPane.isFitToHeight());
    }

    @Test public void whenPannableIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)scrollPane.pannableProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollPane));
        BooleanProperty other = new SimpleBooleanProperty();
        scrollPane.pannableProperty().bind(other);
        assertFalse(styleable.isSettable(scrollPane));
    }

    @Test public void whenPannableIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)scrollPane.pannableProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(scrollPane));
    }

    @Test public void canSpecifyPannableViaCSS() {
        ((StyleableProperty)scrollPane.pannableProperty()).applyStyle(null, Boolean.TRUE);
        assertSame(true, scrollPane.isPannable());
    }


    /*********************************************************************
     * Miscellaneous Tests                                         *
     ********************************************************************/
    @Test public void setHbarPolicyAndSeeValueIsReflectedInModel() {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        assertSame(scrollPane.hbarPolicyProperty().getValue(), ScrollPane.ScrollBarPolicy.NEVER);
    }

    @Test public void setHbarPolicyAndSeeValue() {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        assertSame(scrollPane.getHbarPolicy(), ScrollPane.ScrollBarPolicy.ALWAYS);
    }

    @Test public void setVbarPolicyAndSeeValueIsReflectedInModel() {
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        assertSame(scrollPane.vbarPolicyProperty().getValue(), ScrollPane.ScrollBarPolicy.NEVER);
    }

    @Test public void setVbarPolicyAndSeeValue() {
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        assertSame(scrollPane.getVbarPolicy(), ScrollPane.ScrollBarPolicy.ALWAYS);
    }

    @Test public void setHvalueAndSeeValueIsReflectedInModel() {
        scrollPane.setHvalue(30.0);
        assertEquals(scrollPane.hvalueProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setHvalueAndSeeValue() {
        scrollPane.setHvalue(30.0);
        assertEquals(scrollPane.getHvalue(), 30.0, 0.0);
    }

    @Test public void setHminAndSeeValueIsReflectedInModel() {
        scrollPane.setHmin(30.0);
        assertEquals(scrollPane.hminProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setHminAndSeeValue() {
        scrollPane.setHmin(30.0);
        assertEquals(scrollPane.getHmin(), 30.0, 0.0);
    }

    @Test public void setHmaxAndSeeValueIsReflectedInModel() {
        scrollPane.setHmax(30.0);
        assertEquals(scrollPane.hmaxProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setHmaxAndSeeValue() {
        scrollPane.setHmax(30.0);
        assertEquals(scrollPane.getHmax(), 30.0, 0.0);
    }

    @Test public void setVvalueAndSeeValueIsReflectedInModel() {
        scrollPane.setVvalue(30.0);
        assertEquals(scrollPane.vvalueProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setVvalueAndSeeValue() {
        scrollPane.setVvalue(30.0);
        assertEquals(scrollPane.getVvalue(), 30.0, 0.0);
    }

    @Test public void setVminAndSeeValueIsReflectedInModel() {
        scrollPane.setVmin(30.0);
        assertEquals(scrollPane.vminProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setVminAndSeeValue() {
        scrollPane.setVmin(30.0);
        assertEquals(scrollPane.getVmin(), 30.0, 0.0);
    }

    @Test public void setVmaxAndSeeValueIsReflectedInModel() {
        scrollPane.setVmax(30.0);
        assertEquals(scrollPane.vmaxProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setVmaxAndSeeValue() {
        scrollPane.setVmax(30.0);
        assertEquals(scrollPane.getVmax(), 30.0, 0.0);
    }

    @Test public void setFitToWidthAndSeeValueIsReflectedInModel() {
        scrollPane.setFitToWidth(true);
        assertSame(scrollPane.fitToWidthProperty().getValue(), true);
    }

    @Test public void setFitToWidthAndSeeValue() {
        scrollPane.setFitToWidth(true);
        assertSame(scrollPane.isFitToWidth(), true);
    }

    @Test public void setFitToHeightAndSeeValueIsReflectedInModel() {
        scrollPane.setFitToHeight(true);
        assertSame(scrollPane.fitToHeightProperty().getValue(), true);
    }

    @Test public void setFitToHeightAndSeeValue() {
        scrollPane.setFitToHeight(true);
        assertSame(scrollPane.isFitToHeight(), true);
    }

    @Test public void setPannableAndSeeValueIsReflectedInModel() {
        scrollPane.setPannable(true);
        assertSame(scrollPane.pannableProperty().getValue(), true);
    }

    @Test public void setPannableAndSeeValue() {
        scrollPane.setPannable(true);
        assertSame(scrollPane.isPannable(), true);
    }

    @Test public void setPrefViewportWidthAndSeeValueIsReflectedInModel() {
        scrollPane.setPrefViewportWidth(46.0);
        assertEquals(scrollPane.prefViewportWidthProperty().getValue(), 46.0, 0.0);
    }

    @Test public void setPrefViewportWidthAndSeeValue() {
        scrollPane.setPrefViewportWidth(54.0);
        assertEquals(scrollPane.getPrefViewportWidth(), 54.0, 0.0);
    }

    @Test public void setPrefViewportHeightAndSeeValueIsReflectedInModel() {
        scrollPane.setPrefViewportHeight(46.0);
        assertEquals(scrollPane.prefViewportHeightProperty().getValue(), 46.0, 0.0);
    }

    @Test public void setPrefViewportHeightAndSeeValue() {
        scrollPane.setPrefViewportHeight(54.0);
        assertEquals(scrollPane.getPrefViewportHeight(), 54.0, 0.0);
    }

}
