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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author srikalyc
 */
public class TooltipTest {
    private Tooltip toolTip;//Empty string
    private Tooltip dummyToolTip;//Empty string
    private Toolkit tk;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        toolTip = new Tooltip();
        dummyToolTip = new Tooltip("dummy");
    }
    
   

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    
    @Test public void defaultConstructorShouldHaveNullString() {
        assertEquals(toolTip.getText(), "");
    }
    @Test public void oneStrArgConstructorShouldHaveString() {
        assertEquals("dummy", dummyToolTip.getText());
    }

    
    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/
    
    @Test public void defaultConstructorShouldSetStyleClassTo_tooltip() {
        assertStyleClassContains(toolTip, "tooltip");
    }
    
    @Test public void oneStrArgConstructorShouldSetStyleClassTo_tooltip() {
        assertStyleClassContains(dummyToolTip, "tooltip");
    }
    
    @Test public void defaultActivationIsFalse() {
        assertFalse(toolTip.isActivated());
    }

    @Test public void canSetActivation() {
        toolTip.setActivated(true);//This call is not public method, not sure if makes sense.
        assertTrue(toolTip.isActivated());
    }

    @Test public void defaultTextAlignmentIsLeft() {
        assertNotNull(toolTip.getTextAlignment());
        assertSame(toolTip.getTextAlignment(), TextAlignment.LEFT);
    }
    
    @Test public void defaultTextOverrunIsEllipsis() {
        assertNotNull(toolTip.getTextOverrun());
        assertSame(toolTip.getTextOverrun(), OverrunStyle.ELLIPSIS);
    }

    @Test public void defaultWrapTextIsFalse() {
        assertFalse(toolTip.isWrapText());
    }

    @Test public void defaultFontIsnotNull() {
        System.out.println("toolTip.getFont() " + toolTip.getFont());
        assertNotNull(toolTip.getFont());
    }

    @Test public void checkDefaultGraphic() {
        assertNull(toolTip.getGraphic());
        assertNull(dummyToolTip.getGraphic());
    }

    @Test public void checkContentDisplay() {
        assertSame(toolTip.getContentDisplay(), ContentDisplay.LEFT);
        assertSame(dummyToolTip.getContentDisplay(), ContentDisplay.LEFT);
    }

    @Test public void checkDefaultGraphicTextGap() {
        assertEquals(toolTip.getGraphicTextGap(), 4.0, 0.0);
        assertEquals(dummyToolTip.getGraphicTextGap(), 4.0, 0.0);
    }

    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/
    @Ignore("TODO: Please remove ignore annotation after RT-15799 is fixed.")
    @Test public void checkTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        toolTip.textProperty().bind(strPr);
        assertTrue("Text cannot be bound", toolTip.textProperty().equals("value"));
        strPr.setValue("newvalue");
        assertTrue("Text cannot be bound", toolTip.textProperty().equals("newvalue"));
    }
    
    @Test public void checkTextAlignmentPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<TextAlignment>(TextAlignment.CENTER);
        toolTip.textAlignmentProperty().bind(objPr);
        assertSame("TextAlignment cannot be bound", toolTip.textAlignmentProperty().getValue(), TextAlignment.CENTER);
        objPr.setValue(TextAlignment.JUSTIFY);
        assertSame("TextAlignment cannot be bound", toolTip.textAlignmentProperty().getValue(), TextAlignment.JUSTIFY);
    }
    
    @Test public void checkTextOverrunPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<OverrunStyle>(OverrunStyle.CENTER_WORD_ELLIPSIS);
        toolTip.textOverrunProperty().bind(objPr);
        assertSame("TextOverrun cannot be bound", toolTip.textOverrunProperty().getValue(), OverrunStyle.CENTER_WORD_ELLIPSIS);
        objPr.setValue(OverrunStyle.LEADING_ELLIPSIS);
        assertSame("TextOverrun cannot be bound", toolTip.textOverrunProperty().getValue(), OverrunStyle.LEADING_ELLIPSIS);
    }
    
    @Test public void checkTextWrapPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Boolean>(true);
        toolTip.wrapTextProperty().bind(objPr);
        assertEquals("TextWrap cannot be bound", toolTip.wrapTextProperty().getValue(), true);
        objPr.setValue(false);
        assertEquals("TextWrap cannot be bound", toolTip.wrapTextProperty().getValue(), false);
    }
    
    @Test public void checkFontPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Font>(null);
        toolTip.fontProperty().bind(objPr);
        assertNull("Font cannot be bound", toolTip.fontProperty().getValue());
        objPr.setValue(Font.getDefault());
        assertSame("Font cannot be bound", toolTip.fontProperty().getValue(), Font.getDefault());
    }
    
    @Test public void checkGraphicPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Node>(null);
        Rectangle rect = new Rectangle(10, 20);
        toolTip.graphicProperty().bind(objPr);
        assertNull("Graphic cannot be bound", toolTip.graphicProperty().getValue());
        objPr.setValue(rect);
        assertSame("Graphic cannot be bound", toolTip.graphicProperty().getValue(), rect);
    }
    
    @Test public void checkContentDisplayPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<ContentDisplay>(null);
        ContentDisplay cont = ContentDisplay.GRAPHIC_ONLY;
        toolTip.contentDisplayProperty().bind(objPr);
        assertNull("ContentDisplay cannot be bound", toolTip.contentDisplayProperty().getValue());
        objPr.setValue(cont);
        assertSame("ContentDisplay cannot be bound", toolTip.contentDisplayProperty().getValue(), cont);
    }
    
    @Test public void checkGraphicTextGapPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        toolTip.graphicTextGapProperty().bind(objPr);
        assertEquals("GraphicTextGap cannot be bound", toolTip.graphicTextGapProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("GraphicTextGap cannot be bound", toolTip.graphicTextGapProperty().getValue(), 5.0, 0.0);
    }
    
    @Test public void textPropertyHasBeanReference() {
        assertSame(toolTip, toolTip.textProperty().getBean());
    }

    @Test public void textPropertyHasName() {
        assertEquals("text", toolTip.textProperty().getName());
    }

    @Test public void textAlignmentPropertyHasBeanReference() {
        assertSame(toolTip.bridge, toolTip.textAlignmentProperty().getBean());
    }

    @Test public void textAlignmentPropertyHasName() {
        assertEquals("textAlignment", toolTip.textAlignmentProperty().getName());
    }

    @Test public void textOverrunPropertyHasBeanReference() {
        assertSame(toolTip.bridge, toolTip.textOverrunProperty().getBean());
    }

    @Test public void textOverrunPropertyHasName() {
        assertEquals("textOverrun", toolTip.textOverrunProperty().getName());
    }

    @Test public void wrapTextPropertyHasBeanReference() {
        assertSame(toolTip.bridge, toolTip.wrapTextProperty().getBean());
    }

    @Test public void wrapTextPropertyHasName() {
        assertEquals("wrapText", toolTip.wrapTextProperty().getName());
    }
    
    @Test public void fontPropertyHasBeanReference() {
        assertSame(toolTip.bridge, toolTip.fontProperty().getBean());
    }

    @Test public void fontPropertyHasName() {
        assertEquals("font", toolTip.fontProperty().getName());
    }

    @Test public void graphicPropertyHasBeanReference() {
        assertSame(toolTip, toolTip.graphicProperty().getBean());
    }

    @Test public void graphicPropertyHasName() {
        assertEquals("graphic", toolTip.graphicProperty().getName());
    }

    @Test public void contentDisplayPropertyHasBeanReference() {
        assertSame(toolTip.bridge, toolTip.contentDisplayProperty().getBean());
    }

    @Test public void contentDisplayPropertyHasName() {
        assertEquals("contentDisplay", toolTip.contentDisplayProperty().getName());
    }

    @Test public void graphicTextGapPropertyHasBeanReference() {
        assertSame(toolTip.bridge, toolTip.graphicTextGapProperty().getBean());
    }

    @Test public void graphicTextGapPropertyHasName() {
        assertEquals("graphicTextGap", toolTip.graphicTextGapProperty().getName());
    }

    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenTextAlignmentIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.textAlignmentProperty());
        assertTrue(styleable.isSettable(toolTip.bridge));
        ObjectProperty<TextAlignment> other = new SimpleObjectProperty<TextAlignment>(TextAlignment.JUSTIFY);
        toolTip.textAlignmentProperty().bind(other);
        assertFalse(styleable.isSettable(toolTip.bridge));
    }

    @Test public void whenTextAlignmentIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.textAlignmentProperty());
        styleable.set(toolTip.bridge, TextAlignment.RIGHT);
        assertTrue(styleable.isSettable(toolTip.bridge));
    }

    @Test public void canSpecifyTextAlignmentViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.textAlignmentProperty());
        styleable.set(toolTip.bridge, TextAlignment.CENTER);
        assertSame(TextAlignment.CENTER, toolTip.getTextAlignment());
    }
  
    @Test public void whenTextOverrunIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.textOverrunProperty());
        assertTrue(styleable.isSettable(toolTip.bridge));
        ObjectProperty<OverrunStyle> other = new SimpleObjectProperty<OverrunStyle>(OverrunStyle.LEADING_ELLIPSIS);
        toolTip.textOverrunProperty().bind(other);
        assertFalse(styleable.isSettable(toolTip.bridge));
    }
    
    @Test public void whenTextOverrunIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.textOverrunProperty());
        styleable.set(toolTip.bridge, OverrunStyle.CENTER_ELLIPSIS);
        assertTrue(styleable.isSettable(toolTip.bridge));
    }
    
    @Test public void canSpecifyTextOverrunViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.textOverrunProperty());
        styleable.set(toolTip.bridge, OverrunStyle.CLIP);
        assertSame(OverrunStyle.CLIP, toolTip.getTextOverrun());
    }
   
    @Test public void whenWrapTextIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.wrapTextProperty());
        assertTrue(styleable.isSettable(toolTip.bridge));
        BooleanProperty other = new SimpleBooleanProperty();
        toolTip.wrapTextProperty().bind(other);
        assertFalse(styleable.isSettable(toolTip.bridge));
    }
    
    @Test public void whenWrapTextIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.wrapTextProperty());
          styleable.set(toolTip.bridge, false);
          assertTrue(styleable.isSettable(toolTip.bridge));
    }
    
    @Test public void canSpecifyWrapTextViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.wrapTextProperty());
          styleable.set(toolTip.bridge, true);
        assertSame(true, toolTip.isWrapText());
    }
    
    @Test public void whenFontIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.fontProperty());
          assertTrue(styleable.isSettable(toolTip.bridge));
        ObjectProperty<Font> other = new SimpleObjectProperty<Font>();
        toolTip.fontProperty().bind(other);
          assertFalse(styleable.isSettable(toolTip.bridge));
    }
    
    @Test public void whenFontIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.fontProperty());
          styleable.set(toolTip.bridge, Font.getDefault());
          assertTrue(styleable.isSettable(toolTip.bridge));
    }
    
    @Test public void canSpecifyFontViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.fontProperty());
          styleable.set(toolTip.bridge, Font.getDefault());
        assertSame(Font.getDefault(), toolTip.getFont());
    }
    
    @Ignore("getStyleableProperty returns null for graphicProperty")
    @Test public void whenGraphicIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.graphicProperty());
        assertTrue(styleable.isSettable(toolTip.bridge));
        ObjectProperty<Node> other = new SimpleObjectProperty<Node>();
        toolTip.graphicProperty().bind(other);
          assertFalse(styleable.isSettable(toolTip.bridge));
    }
    
    @Ignore("getStyleableProperty returns null for graphicProperty")
    @Test public void whenGraphicIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.graphicProperty());
          styleable.set(toolTip.bridge, "../../../../build/classes/com/sun/javafx/scene/control/skin/caspian/menu-shadow.png");
          assertTrue(styleable.isSettable(toolTip.bridge));
    }
    
    @Ignore("getStyleableProperty returns null for graphicProperty")
    @Test public void canSpecifyGraphicViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.graphicProperty());
          styleable.set(toolTip.bridge, "../../../../build/classes/com/sun/javafx/scene/control/skin/caspian/menu-shadow.png");
        assertNotNull(toolTip.getGraphic());
    }
    
    @Test public void whenContentDisplayIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.contentDisplayProperty());
          assertTrue(styleable.isSettable(toolTip.bridge));
        ObjectProperty<ContentDisplay> other = new SimpleObjectProperty<ContentDisplay>();
        toolTip.contentDisplayProperty().bind(other);
          assertFalse(styleable.isSettable(toolTip.bridge));
    }
    
    @Test public void whenContentDisplayIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.contentDisplayProperty());
          styleable.set(toolTip.bridge, ContentDisplay.TEXT_ONLY);
          assertTrue(styleable.isSettable(toolTip.bridge));
    }
    
    @Test public void canSpecifyContentDisplayViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.contentDisplayProperty());
          styleable.set(toolTip.bridge, ContentDisplay.BOTTOM);
        assertSame(toolTip.getContentDisplay(), ContentDisplay.BOTTOM);
    }
    
    @Test public void whenGraphicTextGapIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.graphicTextGapProperty());
          assertTrue(styleable.isSettable(toolTip.bridge));
        DoubleProperty other = new SimpleDoubleProperty();
        toolTip.graphicTextGapProperty().bind(other);
          assertFalse(styleable.isSettable(toolTip.bridge));
    }

    @Test public void whenGraphicTextGapIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.graphicTextGapProperty());
          styleable.set(toolTip.bridge, 6.0);
          assertTrue(styleable.isSettable(toolTip.bridge));
    }

    @Test public void canSpecifyGraphicTextGapViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(toolTip.graphicTextGapProperty());
          styleable.set(toolTip.bridge, 56.0);
        assertEquals(56.0, toolTip.getGraphicTextGap(),0.0);
    }

    

    /*********************************************************************
     * Miscellaneous Tests                                         *
     ********************************************************************/
    @Test public void setTextAndSeeValueIsReflectedInModel() {
        toolTip.setText("tmp");
        assertEquals(toolTip.textProperty().getValue(), "tmp");
    }
    
    @Test public void setTextAndSeeValue() {
        toolTip.setText("tmp");
        assertEquals(toolTip.getText(), "tmp");
    }
    
    @Test public void setTextAlignmentAndSeeValueIsReflectedInModel() {
        toolTip.setTextAlignment(TextAlignment.JUSTIFY);
        assertEquals(toolTip.textAlignmentProperty().getValue(), TextAlignment.JUSTIFY);
    }

    @Test public void setTextAlignmentAndSeeValue() {
        toolTip.setTextAlignment(TextAlignment.JUSTIFY);
        assertEquals(toolTip.getTextAlignment(), TextAlignment.JUSTIFY);
    }

    @Test public void setTextOverrunAndSeeValueIsReflectedInModel() {
        toolTip.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        assertEquals(toolTip.textOverrunProperty().getValue(), OverrunStyle.LEADING_ELLIPSIS);
    }
    
    @Test public void setTextOverrunAndSeeValue() {
        toolTip.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        assertEquals(toolTip.getTextOverrun(), OverrunStyle.LEADING_ELLIPSIS);
    }
    
    @Test public void setWrapTextAndSeeValueIsReflectedInModel() {
        toolTip.setWrapText(true);
        assertEquals(toolTip.wrapTextProperty().getValue(), true);
    }
    
    @Test public void setWrapTextAndSeeValue() {
        toolTip.setWrapText(true);
        assertEquals(toolTip.isWrapText(), true);
    }
    
    @Test public void setFontAndSeeValueIsReflectedInModel() {
        toolTip.setFont(Font.getDefault());
        assertEquals(toolTip.fontProperty().getValue(), Font.getDefault());
    }
    
    @Test public void setFontAndSeeValue() {
        toolTip.setFont(Font.getDefault());
        assertEquals(toolTip.getFont(), Font.getDefault());
    }
    
    @Test public void setGraphicAndSeeValueIsReflectedInModel() {
        Rectangle rect = new Rectangle();
        toolTip.setGraphic(rect);
        assertEquals(toolTip.graphicProperty().getValue(), rect);
    }
    
    @Test public void setGraphicAndSeeValue() {
        Rectangle rect = new Rectangle();
        toolTip.setGraphic(rect);
        assertEquals(toolTip.getGraphic(), rect);
    }
    
    @Test public void setContentDiaplyAndSeeValueIsReflectedInModel() {
        ContentDisplay cont = ContentDisplay.BOTTOM;
        toolTip.setContentDisplay(cont);
        assertEquals(toolTip.contentDisplayProperty().getValue(), cont);
    }
    
    @Test public void setContentDisplayAndSeeValue() {
        ContentDisplay cont = ContentDisplay.TEXT_ONLY;
        toolTip.setContentDisplay(cont);
        assertEquals(toolTip.getContentDisplay(), cont);
    }
    
    @Test public void setGraphicTextGapAndSeeValueIsReflectedInModel() {
        toolTip.setGraphicTextGap(32.0);
        assertEquals(toolTip.graphicTextGapProperty().getValue(), 32.0, 0.0);
    }
    
    @Test public void setGraphicTextGapAndSeeValue() {
        toolTip.setGraphicTextGap(28.0);
        assertEquals(toolTip.getGraphicTextGap(), 28.0, 0.0);
    }
    
    @Test public void installOnSingleNode() {
        try {
            Rectangle rect = new Rectangle(0, 0, 100, 100);
            Tooltip.install(rect, toolTip);
        } catch (Exception e) {//Catch a generic Exception coz we dont know what
            fail("Could not install tooltip on a Node");
        }
    }

    @Test public void installSameTooltipOnManyNodes() {
        try {
            Rectangle rect1 = new Rectangle(0, 0, 100, 100);
            Rectangle rect2 = new Rectangle(0, 0, 100, 100);
            Tooltip.install(rect1, toolTip);
            Tooltip.install(rect2, toolTip);
        } catch (Exception e) {//Catch a generic Exception coz we dont know what
            fail("Could not install same tooltip on many Node");
        }
    }

    @Test public void uninstallOnSingleNode() {
        try {
            Rectangle rect = new Rectangle(0, 0, 100, 100);
            Tooltip.install(rect, toolTip);
            Tooltip.uninstall(rect, toolTip);
        } catch (Exception e) {//Catch a generic Exception coz we dont know what
            fail("Could not uninstall tooltip on a Node");
        }
    }

    @Test public void uninstallSameTooltipOnManyNodes() {
        try {
            Rectangle rect1 = new Rectangle(0, 0, 100, 100);
            Rectangle rect2 = new Rectangle(0, 0, 100, 100);
            Tooltip.install(rect1, toolTip);
            Tooltip.install(rect2, toolTip);
            Tooltip.uninstall(rect1, toolTip);
            Tooltip.uninstall(rect2, toolTip);
        } catch (Exception e) {//Catch a generic Exception coz we dont know what
            fail("Could not uninstall same tooltip on many Node");
        }
    }
    
    
}
