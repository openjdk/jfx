/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import com.sun.javafx.css.StyleableProperty;
import static javafx.scene.control.ControlTestUtils.*;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author srikalyc
 */
public class ScrollPaneTest {
    private ScrollPane scrollPane;//Empty string
    private Toolkit tk;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
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
        ObjectProperty objPr = new SimpleObjectProperty<ScrollPane.ScrollBarPolicy>(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.hbarPolicyProperty().bind(objPr);
        assertSame("HBarPolicyProperty cannot be bound", scrollPane.hbarPolicyProperty().getValue(), ScrollPane.ScrollBarPolicy.ALWAYS);
        objPr.setValue(ScrollPane.ScrollBarPolicy.NEVER);
        assertSame("HBarPolicyProperty cannot be bound", scrollPane.hbarPolicyProperty().getValue(), ScrollPane.ScrollBarPolicy.NEVER);
    }
    
    @Test public void checkVBarPolicyPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<ScrollPane.ScrollBarPolicy>(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.vbarPolicyProperty().bind(objPr);
        assertSame("VBarPolicyProperty cannot be bound", scrollPane.vbarPolicyProperty().getValue(), ScrollPane.ScrollBarPolicy.ALWAYS);
        objPr.setValue(ScrollPane.ScrollBarPolicy.NEVER);
        assertSame("VBarPolicyProperty cannot be bound", scrollPane.vbarPolicyProperty().getValue(), ScrollPane.ScrollBarPolicy.NEVER);
    }
    
    @Test public void checkHValuePropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.hvalueProperty().bind(objPr);
        assertEquals("hvalueProperty cannot be bound", scrollPane.hvalueProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("hvalueProperty cannot be bound", scrollPane.hvalueProperty().getValue(), 5.0, 0.0);
    }
    
    @Test public void checkHminPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.hminProperty().bind(objPr);
        assertEquals("hminProperty cannot be bound", scrollPane.hminProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("hminProperty cannot be bound", scrollPane.hminProperty().getValue(), 5.0, 0.0);
    }
    
    @Test public void checkHmaxPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.hmaxProperty().bind(objPr);
        assertEquals("hmaxProperty cannot be bound", scrollPane.hmaxProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("hmaxProperty cannot be bound", scrollPane.hmaxProperty().getValue(), 5.0, 0.0);
    }
    
    @Test public void checkVValuePropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.vvalueProperty().bind(objPr);
        assertEquals("vvalueProperty cannot be bound", scrollPane.vvalueProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("vvalueProperty cannot be bound", scrollPane.vvalueProperty().getValue(), 5.0, 0.0);
    }
    
    @Test public void checkVminPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.vminProperty().bind(objPr);
        assertEquals("vminProperty cannot be bound", scrollPane.vminProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("vminProperty cannot be bound", scrollPane.vminProperty().getValue(), 5.0, 0.0);
    }
    
    @Test public void checkVmaxPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.vmaxProperty().bind(objPr);
        assertEquals("vmaxProperty cannot be bound", scrollPane.vmaxProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("vmaxProperty cannot be bound", scrollPane.vmaxProperty().getValue(), 5.0, 0.0);
    }
    
    @Test public void checkFitToWidthPropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        scrollPane.fitToWidthProperty().bind(objPr);
        assertEquals("FitToWidth cannot be bound", scrollPane.fitToWidthProperty().getValue(), true);
        objPr.setValue(false);
        assertEquals("FitToWidth cannot be bound", scrollPane.fitToWidthProperty().getValue(), false);
    }
    
    @Test public void checkFitToHeigtPropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        scrollPane.fitToHeightProperty().bind(objPr);
        assertEquals("FitToHeigt cannot be bound", scrollPane.fitToHeightProperty().getValue(), true);
        objPr.setValue(false);
        assertEquals("FitToHeigt cannot be bound", scrollPane.fitToHeightProperty().getValue(), false);
    }
    
    @Test public void checkPannablePropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        scrollPane.pannableProperty().bind(objPr);
        assertEquals("Pannable cannot be bound", scrollPane.pannableProperty().getValue(), true);
        objPr.setValue(false);
        assertEquals("Pannable cannot be bound", scrollPane.pannableProperty().getValue(), false);
    }

    @Test public void checkPreferredViewportWidthBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.prefViewportWidthProperty().bind(objPr);
        assertEquals("prefViewportWidthProperty cannot be bound", scrollPane.prefViewportWidthProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("prefViewportWidthProperty cannot be bound", scrollPane.prefViewportWidthProperty().getValue(), 5.0, 0.0);
    }

    @Test public void checkPreferredViewportHeightBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        scrollPane.prefViewportHeightProperty().bind(objPr);
        assertEquals("prefViewportHeightProperty cannot be bound", scrollPane.prefViewportHeightProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("prefViewportHeightProperty cannot be bound", scrollPane.prefViewportHeightProperty().getValue(), 5.0, 0.0);
    }

    @Test public void checkViewportBoundsBind() {
        Bounds b = null;
        ObjectProperty objPr = new SimpleObjectProperty<Bounds>(b);
        scrollPane.viewportBoundsProperty().bind(objPr);
        assertNull("viewportBoundsProperty cannot be bound", scrollPane.viewportBoundsProperty().getValue());
        b = new BoundingBox(0.0, 0.0, 0.0, 0.0);
        objPr.setValue(b);
        assertSame("viewportBoundsProperty cannot be bound", scrollPane.viewportBoundsProperty().getValue(), b);
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
    @Test public void whenHbarPolicyIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.hbarPolicyProperty());
        assertTrue(styleable.isSettable(scrollPane));
        ObjectProperty<ScrollPane.ScrollBarPolicy> other = new SimpleObjectProperty<ScrollPane.ScrollBarPolicy>(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.hbarPolicyProperty().bind(other);
        assertFalse(styleable.isSettable(scrollPane));
    }

    @Test public void whenHbarPolicyIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.hbarPolicyProperty());
        styleable.set(scrollPane, ScrollPane.ScrollBarPolicy.ALWAYS);
        assertTrue(styleable.isSettable(scrollPane));
    }

    @Test public void canSpecifyHbarPolicyViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.hbarPolicyProperty());
        styleable.set(scrollPane, ScrollPane.ScrollBarPolicy.NEVER);
        assertTrue(styleable.isSettable(scrollPane));
    }

    @Test public void whenVbarPolicyIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.vbarPolicyProperty());
        assertTrue(styleable.isSettable(scrollPane));
        ObjectProperty<ScrollPane.ScrollBarPolicy> other = new SimpleObjectProperty<ScrollPane.ScrollBarPolicy>(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.vbarPolicyProperty().bind(other);
        assertFalse(styleable.isSettable(scrollPane));
    }

    @Test public void whenVbarPolicyIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.vbarPolicyProperty());
        styleable.set(scrollPane, ScrollPane.ScrollBarPolicy.ALWAYS);
        assertTrue(styleable.isSettable(scrollPane));
    }

    @Test public void canSpecifyVbarPolicyViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.vbarPolicyProperty());
        styleable.set(scrollPane, ScrollPane.ScrollBarPolicy.NEVER);
        assertSame(ScrollPane.ScrollBarPolicy.NEVER, scrollPane.getVbarPolicy());
    }

    @Test public void whenFitToWidthIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.fitToWidthProperty());
        assertTrue(styleable.isSettable(scrollPane));
        BooleanProperty other = new SimpleBooleanProperty();
        scrollPane.fitToWidthProperty().bind(other);
        assertFalse(styleable.isSettable(scrollPane));
    }

    @Test public void whenFitToWidthIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.fitToWidthProperty());
        styleable.set(scrollPane, false);
        assertTrue(styleable.isSettable(scrollPane));
    }

    @Test public void canSpecifyFitToWidthViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.fitToWidthProperty());
        styleable.set(scrollPane, true);
        assertSame(true, scrollPane.isFitToWidth());
    }

    @Test public void whenFitToHeightIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.fitToHeightProperty());
        assertTrue(styleable.isSettable(scrollPane));
        BooleanProperty other = new SimpleBooleanProperty();
        scrollPane.fitToHeightProperty().bind(other);
        assertFalse(styleable.isSettable(scrollPane));
    }

    @Test public void whenFitToHeightIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.fitToHeightProperty());
        styleable.set(scrollPane, false);
        assertTrue(styleable.isSettable(scrollPane));
    }

    @Test public void canSpecifyFitToHeightViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.fitToHeightProperty());
        styleable.set(scrollPane, true);
        assertSame(true, scrollPane.isFitToHeight());
    }

    @Test public void whenPannableIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.pannableProperty());
        assertTrue(styleable.isSettable(scrollPane));
        BooleanProperty other = new SimpleBooleanProperty();
        scrollPane.pannableProperty().bind(other);
        assertFalse(styleable.isSettable(scrollPane));
    }

    @Test public void whenPannableIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.pannableProperty());
        styleable.set(scrollPane, false);
        assertTrue(styleable.isSettable(scrollPane));
    }

    @Test public void canSpecifyPannableViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(scrollPane.pannableProperty());
        styleable.set(scrollPane, true);
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
