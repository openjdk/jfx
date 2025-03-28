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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TooltipShim;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventGenerator;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

public class TooltipTest {
    private TooltipShim toolTip;//Empty string
    private TooltipShim dummyToolTip;//Empty string

    private StageLoader stageLoader;
    private StubToolkit toolkit;

    @BeforeEach
    public void setup() {
        toolTip = new TooltipShim();
        dummyToolTip = new TooltipShim("dummy");

        toolkit = (StubToolkit) Toolkit.getToolkit();
        toolkit.setAnimationTime(0);
    }

    @AfterEach
    public void tearDown() {
        if (stageLoader != null) {
            stageLoader.dispose();
        }
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
        toolTip.shim_setActivated(true);//This call is not public method, not sure if makes sense.
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
        //System.out.println("toolTip.getFont() " + toolTip.getFont());
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
    @Test public void checkTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        toolTip.textProperty().bind(strPr);
        assertTrue(toolTip.textProperty().getValue().equals("value"), "Text cannot be bound");
        strPr.setValue("newvalue");
        assertTrue(toolTip.textProperty().getValue().equals("newvalue"), "Text cannot be bound");
    }

    @Test public void checkTextAlignmentPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<>(TextAlignment.CENTER);
        toolTip.textAlignmentProperty().bind(objPr);
        assertSame(toolTip.textAlignmentProperty().getValue(), TextAlignment.CENTER, "TextAlignment cannot be bound");
        objPr.setValue(TextAlignment.JUSTIFY);
        assertSame(toolTip.textAlignmentProperty().getValue(), TextAlignment.JUSTIFY, "TextAlignment cannot be bound");
    }

    @Test public void checkTextOverrunPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<>(OverrunStyle.CENTER_WORD_ELLIPSIS);
        toolTip.textOverrunProperty().bind(objPr);
        assertSame(toolTip.textOverrunProperty().getValue(), OverrunStyle.CENTER_WORD_ELLIPSIS, "TextOverrun cannot be bound");
        objPr.setValue(OverrunStyle.LEADING_ELLIPSIS);
        assertSame(toolTip.textOverrunProperty().getValue(), OverrunStyle.LEADING_ELLIPSIS, "TextOverrun cannot be bound");
    }

    @Test public void checkTextWrapPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<>(true);
        toolTip.wrapTextProperty().bind(objPr);
        assertEquals(toolTip.wrapTextProperty().getValue(), true, "TextWrap cannot be bound");
        objPr.setValue(false);
        assertEquals(toolTip.wrapTextProperty().getValue(), false, "TextWrap cannot be bound");
    }

    @Test public void checkFontPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Font>(null);
        toolTip.fontProperty().bind(objPr);
        assertNull(toolTip.fontProperty().getValue(), "Font cannot be bound");
        objPr.setValue(Font.getDefault());
        assertSame(toolTip.fontProperty().getValue(), Font.getDefault(), "Font cannot be bound");
    }

    @Test public void checkGraphicPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Node>(null);
        Rectangle rect = new Rectangle(10, 20);
        toolTip.graphicProperty().bind(objPr);
        assertNull(toolTip.graphicProperty().getValue(), "Graphic cannot be bound");
        objPr.setValue(rect);
        assertSame(toolTip.graphicProperty().getValue(), rect, "Graphic cannot be bound");
    }

    @Test public void checkContentDisplayPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<ContentDisplay>(null);
        ContentDisplay cont = ContentDisplay.GRAPHIC_ONLY;
        toolTip.contentDisplayProperty().bind(objPr);
        assertNull(toolTip.contentDisplayProperty().getValue(), "ContentDisplay cannot be bound");
        objPr.setValue(cont);
        assertSame(toolTip.contentDisplayProperty().getValue(), cont, "ContentDisplay cannot be bound");
    }

    @Test public void checkGraphicTextGapPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        toolTip.graphicTextGapProperty().bind(objPr);
        assertEquals(toolTip.graphicTextGapProperty().getValue(), 2.0, 0.0, "GraphicTextGap cannot be bound");
        objPr.setValue(5.0);
        assertEquals(toolTip.graphicTextGapProperty().getValue(), 5.0, 0.0, "GraphicTextGap cannot be bound");
    }

    @Test public void textPropertyHasBeanReference() {
        assertSame(toolTip, toolTip.textProperty().getBean());
    }

    @Test public void textPropertyHasName() {
        assertEquals("text", toolTip.textProperty().getName());
    }

    @Test public void textAlignmentPropertyHasBeanReference() {
        assertSame(toolTip, toolTip.textAlignmentProperty().getBean());
    }

    @Test public void textAlignmentPropertyHasName() {
        assertEquals("textAlignment", toolTip.textAlignmentProperty().getName());
    }

    @Test public void textOverrunPropertyHasBeanReference() {
        assertSame(toolTip, toolTip.textOverrunProperty().getBean());
    }

    @Test public void textOverrunPropertyHasName() {
        assertEquals("textOverrun", toolTip.textOverrunProperty().getName());
    }

    @Test public void wrapTextPropertyHasBeanReference() {
        assertSame(toolTip, toolTip.wrapTextProperty().getBean());
    }

    @Test public void wrapTextPropertyHasName() {
        assertEquals("wrapText", toolTip.wrapTextProperty().getName());
    }

    @Test public void fontPropertyHasBeanReference() {
        assertSame(toolTip, toolTip.fontProperty().getBean());
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
        assertSame(toolTip, toolTip.contentDisplayProperty().getBean());
    }

    @Test public void contentDisplayPropertyHasName() {
        assertEquals("contentDisplay", toolTip.contentDisplayProperty().getName());
    }

    @Test public void graphicTextGapPropertyHasBeanReference() {
        assertSame(toolTip, toolTip.graphicTextGapProperty().getBean());
    }

    @Test public void graphicTextGapPropertyHasName() {
        assertEquals("graphicTextGap", toolTip.graphicTextGapProperty().getName());
    }

    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenTextAlignmentIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)toolTip.textAlignmentProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
        ObjectProperty<TextAlignment> other = new SimpleObjectProperty<>(TextAlignment.JUSTIFY);
        toolTip.textAlignmentProperty().bind(other);
        assertFalse(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void whenTextAlignmentIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)toolTip.textAlignmentProperty()).getCssMetaData();
        ((StyleableProperty)toolTip.textAlignmentProperty()).applyStyle(null, TextAlignment.RIGHT);
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void canSpecifyTextAlignmentViaCSS() {
        ((StyleableProperty)toolTip.textAlignmentProperty()).applyStyle(null, TextAlignment.CENTER);
        assertSame(TextAlignment.CENTER, toolTip.getTextAlignment());
    }

    @Test public void whenTextOverrunIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)toolTip.textOverrunProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
        ObjectProperty<OverrunStyle> other = new SimpleObjectProperty<>(OverrunStyle.LEADING_ELLIPSIS);
        toolTip.textOverrunProperty().bind(other);
        assertFalse(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void whenTextOverrunIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)toolTip.textOverrunProperty()).getCssMetaData();
        ((StyleableProperty)toolTip.textOverrunProperty()).applyStyle(null, OverrunStyle.CENTER_ELLIPSIS);
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void canSpecifyTextOverrunViaCSS() {
        ((StyleableProperty)toolTip.textOverrunProperty()).applyStyle(null, OverrunStyle.CLIP);
        assertSame(OverrunStyle.CLIP, toolTip.getTextOverrun());
    }

    @Test public void whenWrapTextIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)toolTip.wrapTextProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
        BooleanProperty other = new SimpleBooleanProperty();
        toolTip.wrapTextProperty().bind(other);
        assertFalse(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void whenWrapTextIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)toolTip.wrapTextProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void canSpecifyWrapTextViaCSS() {
        ((StyleableProperty)toolTip.wrapTextProperty()).applyStyle(null, Boolean.TRUE);
        assertSame(true, toolTip.isWrapText());
    }

    @Test public void whenFontIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)toolTip.fontProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
        ObjectProperty<Font> other = new SimpleObjectProperty<>();
        toolTip.fontProperty().bind(other);
        assertFalse(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void whenFontIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)toolTip.fontProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void canSpecifyFontViaCSS() {
        ((StyleableProperty)toolTip.fontProperty()).applyStyle(null, Font.getDefault());
        assertSame(Font.getDefault(), toolTip.getFont());
    }

    @Test public void whenGraphicIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)toolTip.graphicProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
        ObjectProperty<Node> other = new SimpleObjectProperty<>();
        toolTip.graphicProperty().bind(other);
        assertFalse(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void whenGraphicIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)toolTip.graphicProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
    }

    @Disabled("CSS sets graphicProperty indirectly")
    @Test public void canSpecifyGraphicViaCSS() {
        ((StyleableProperty)toolTip.graphicProperty())
                .applyStyle(null , "../../../../build/classes/com/sun/javafx/scene/control/skin/caspian/menu-shadow.png");
        assertNotNull(toolTip.getGraphic());
    }

    @Test public void whenContentDisplayIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)toolTip.contentDisplayProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
        ObjectProperty<ContentDisplay> other = new SimpleObjectProperty<>();
        toolTip.contentDisplayProperty().bind(other);
        assertFalse(styleable.isSettable(toolTip.get_bridge()));
    }
    @Test public void whenContentDisplayIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)toolTip.contentDisplayProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void canSpecifyContentDisplayViaCSS() {
        ((StyleableProperty)toolTip.contentDisplayProperty()).applyStyle(null, ContentDisplay.BOTTOM);
        assertSame(toolTip.getContentDisplay(), ContentDisplay.BOTTOM);
    }

    @Test public void whenGraphicTextGapIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)toolTip.graphicTextGapProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
        DoubleProperty other = new SimpleDoubleProperty();
        toolTip.graphicTextGapProperty().bind(other);
        assertFalse(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void whenGraphicTextGapIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)toolTip.graphicTextGapProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolTip.get_bridge()));
    }

    @Test public void canSpecifyGraphicTextGapViaCSS() {
        ((StyleableProperty)toolTip.graphicTextGapProperty()).applyStyle(null, 56.0);
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

    /**
     * A {@link Tooltip} once was showing and quickly hiding itself in order to process the CSS.
     * This was changed in <a href="https://bugs.openjdk.org/browse/JDK-8296387">JDK-8296387</a>
     * and this test ensure that this is the case.
     */
    @Test
    public void testTooltipShouldNotBeShownBeforeDelayIsUp() {
        toolTip.showingProperty().addListener(inv -> fail());
        Rectangle rect = new Rectangle(0, 0, 100, 100);

        stageLoader = new StageLoader(rect);

        Tooltip.install(rect, toolTip);

        MouseEvent mouseEvent = MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_MOVED, 1, 1);
        rect.fireEvent(mouseEvent);
    }

    @Test
    public void testTooltipShouldNotBeShownBeforeDefaultDelay() {
        Rectangle rect = new Rectangle(0, 0, 100, 100);

        stageLoader = new StageLoader(rect);

        Tooltip.install(rect, toolTip);

        MouseEvent mouseEvent = MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_MOVED, 1, 1);
        rect.fireEvent(mouseEvent);

        assertFalse(toolTip.isShowing());

        toolkit.setAnimationTime(999);

        assertFalse(toolTip.isShowing());
    }

    @Test
    public void testTooltipShouldBeShownAfterDefaultDelay() {
        Rectangle rect = new Rectangle(0, 0, 100, 100);

        stageLoader = new StageLoader(rect);

        Tooltip.install(rect, toolTip);

        assertFalse(toolTip.isShowing());

        assertTooltipShownAfter(rect, 1000);
        assertTooltipHiddenAfter(rect, 200);
    }

    @Test
    public void testTooltipShouldBeHiddenAfterHideDelay() {
        int delay = 50;
        toolTip.setHideDelay(Duration.millis(delay));

        Rectangle rect = new Rectangle(0, 0, 100, 100);

        stageLoader = new StageLoader(rect);

        Tooltip.install(rect, toolTip);

        assertFalse(toolTip.isShowing());

        assertTooltipShownAfter(rect, 1000);
        assertTooltipHiddenAfter(rect, delay);
    }

    @Test
    public void testTooltipShouldBeShownAfterSetShowDelay() {
        int delay = 200;
        toolTip.setShowDelay(Duration.millis(delay));

        Rectangle rect = new Rectangle(0, 0, 100, 100);

        stageLoader = new StageLoader(rect);

        Tooltip.install(rect, toolTip);

        assertFalse(toolTip.isShowing());

        assertTooltipShownAfter(rect, delay);
        assertTooltipHiddenAfter(rect, 200);
    }

    @Test
    public void testTooltipShouldBeShownAfterSetStyleShowDelay() {
        toolTip.setStyle("-fx-show-delay: 200ms;");

        Rectangle rect = new Rectangle(0, 0, 100, 100);

        stageLoader = new StageLoader(rect);

        Tooltip.install(rect, toolTip);

        assertFalse(toolTip.isShowing());

        assertTooltipShownAfter(rect, 200);
        assertTooltipHiddenAfter(rect, 200);
    }

    @Test
    public void testTooltipShouldBeShownAfterSetCssShowDelay() {
        Rectangle rect = new Rectangle(0, 0, 100, 100);

        stageLoader = new StageLoader(rect);
        stageLoader.getStage().getScene().getStylesheets().setAll(toBase64(".tooltip { -fx-show-delay: 200ms; }"));

        Tooltip.install(rect, toolTip);

        assertFalse(toolTip.isShowing());

        assertTooltipShownAfter(rect, 200);
        assertTooltipHiddenAfter(rect, 200);
    }

    @Test
    public void testTooltipChangeShowDelayCss() {
        Rectangle rect = new Rectangle(0, 0, 100, 100);

        stageLoader = new StageLoader(rect);
        stageLoader.getStage().getScene().getStylesheets().setAll(toBase64(".tooltip { -fx-show-delay: 200ms; }"));

        Tooltip.install(rect, toolTip);

        assertFalse(toolTip.isShowing());

        assertTooltipShownAfter(rect, 200);
        assertTooltipHiddenAfter(rect, 200);

        stageLoader.getStage().getScene().getStylesheets().setAll(toBase64(".tooltip { -fx-show-delay: 450ms; }"));

        assertTooltipShownAfter(rect, 450);
        assertTooltipHiddenAfter(rect, 200);
    }

    private void assertTooltipShownAfter(Rectangle rect, int millis) {
        MouseEvent mouseEvent = MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_MOVED, 1, 1);
        rect.fireEvent(mouseEvent);

        toolkit.setAnimationTime(toolkit.getCurrentTime() + millis);

        assertTrue(toolTip.isShowing());
    }

    private void assertTooltipHiddenAfter(Rectangle rect, int millis) {
        MouseEvent mouseEvent = MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_EXITED, -1, -1);
        rect.fireEvent(mouseEvent);

        toolkit.setAnimationTime(toolkit.getCurrentTime() + millis);

        assertFalse(toolTip.isShowing());
    }

    private String toBase64(String css) {
        return "data:base64," + Base64.getUrlEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }

}
