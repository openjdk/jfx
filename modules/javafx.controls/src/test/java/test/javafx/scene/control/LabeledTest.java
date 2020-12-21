/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.CssMetaData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Labeled;
import javafx.scene.control.OverrunStyle;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * Then we need to write some tests for LabelSkin, such that we test the layout
 * of all these cases. Be sure to test when each of these properties has illegal
 * values, such as negative for graphicTextGap and null for contentDisplay and
 * so forth.
 */
public class LabeledTest {
    private Labeled labeled;

    @Before public void setup() {
        labeled = new LabeledMock();
    }

    /********************************************************************************
     *                                                                              *
     *                           Tests for text property                            *
     *                                                                              *
     *  - default constructor has text initialized to empty string, graphic is null *
     *  - null passed to one-arg constructor results in empty string text           *
     *  - string passed to on-arg constructor is set as text                        *
     *  - null passed to two-arg constructor for text results in empty string text  *
     *  - string passed to two-arg constructor for text is set as text              *
     *  - any value passed as graphic to two-arg constructor is set as graphic      *
     *                                                                              *
     *******************************************************************************/

    @Test public void defaultConstructorShouldHaveNoGraphicAndEmptyString() {
        assertNull(labeled.getGraphic());
        assertEquals("", labeled.getText());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphicAndSpecifiedString() {
        Labeled l2 = new LabeledMock(null);
        assertNull(l2.getGraphic());
        assertNull(l2.getText());

        l2 = new LabeledMock("");
        assertNull(l2.getGraphic());
        assertEquals("", l2.getText());

        l2 = new LabeledMock("Hello");
        assertNull(l2.getGraphic());
        assertEquals("Hello", l2.getText());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedGraphicAndSpecifiedString() {
        Labeled l2 = new LabeledMock(null, null);
        assertNull(l2.getGraphic());
        assertNull(l2.getText());

        Rectangle rect = new Rectangle();
        l2 = new LabeledMock("Hello", rect);
        assertSame(rect, l2.getGraphic());
        assertEquals("Hello", l2.getText());
    }

    /********************************************************************************
     *                                                                              *
     *                           Tests for text property                            *
     *                                                                              *
     *  - can be null                                                               *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void textDefaultValueIsEmptyString() {
        assertEquals("", labeled.getText());
        assertEquals("", labeled.textProperty().get());
    }

    @Test public void textCanBeNull() {
        labeled.setText(null);
        assertNull(labeled.getText());
    }

    @Test public void settingTextValueShouldWork() {
        labeled.setText("Hello World");
        assertEquals("Hello World", labeled.getText());
    }

    @Test public void settingTextAndThenCreatingAModelAndReadingTheValueStillWorks() {
        labeled.setText("Hello World");
        assertEquals("Hello World", labeled.textProperty().get());
    }

    @Test public void textCanBeBound() {
        StringProperty other = new SimpleStringProperty("Apples");
        labeled.textProperty().bind(other);
        assertEquals("Apples", labeled.getText());
    }

    @Test public void cannotSpecifyTextViaCSS() {
        try {
            CssMetaData styleable = ((StyleableProperty)labeled.textProperty()).getCssMetaData();
            assertNull(styleable);
        } catch (ClassCastException ignored) {
            // pass!
        } catch (Exception e) {
            org.junit.Assert.fail(e.toString());
        }
    }

    /********************************************************************************
     *                                                                              *
     *                         Tests for textFill property                          *
     *                                                                              *
     *  - can be null                                                               *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - is BLACK by default                                                       *
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void textFillDefaultValueIsBLACK() {
        assertSame(Color.BLACK, labeled.getTextFill());
        assertSame(Color.BLACK, labeled.textFillProperty().get());
    }

    @Test public void textFillCanBeNull() {
        labeled.setTextFill(null);
        assertNull(labeled.getTextFill());
    }

    @Test public void settingTextFillValueShouldWork() {
        labeled.setTextFill(Color.RED);
        assertSame(Color.RED, labeled.getTextFill());
    }

    @Test public void settingTextFillAndThenCreatingAModelAndReadingTheValueStillWorks() {
        labeled.setTextFill(Color.RED);
        assertSame(Color.RED, labeled.textFillProperty().get());
    }

    @Test public void textFillCanBeBound() {
        ObjectProperty<Paint> other = new SimpleObjectProperty<Paint>(Color.RED);
        labeled.textFillProperty().bind(other);
        assertSame(Color.RED, labeled.getTextFill());
    }

    @Test public void whenTextFillIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)labeled.textFillProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
        ObjectProperty<Paint> other = new SimpleObjectProperty<Paint>(Color.RED);
        labeled.textFillProperty().bind(other);
        assertFalse(styleable.isSettable(labeled));

    }

    @Test public void whenTextFillIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)labeled.textFillProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
    }

    @Test public void canSpecifyTextFillViaCSS() {
        ((StyleableProperty)labeled.textFillProperty()).applyStyle(null, Color.YELLOW);
        assertSame(Color.YELLOW, labeled.getTextFill());
    }

    @Test public void textFillBeanIsCorrect() {
        assertSame(labeled, labeled.textFillProperty().getBean());
    }

    @Test public void textFillNameIsCorrect() {
        assertEquals("textFill", labeled.textFillProperty().getName());
    }

    /********************************************************************************
     *                                                                              *
     *                         Tests for alignment property                         *
     *                                                                              *
     *  - can be null                                                               *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - default is "CENTER_LEFT"                                                  *
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void alignmentDefaultValueIsCENTER_LEFT() {
        assertEquals(Pos.CENTER_LEFT, labeled.getAlignment());
    }

    @Test public void alignmentCanBeNull() {
        labeled.setAlignment(null);
        assertNull(labeled.getAlignment());
    }

    @Test public void settingAlignmentValueShouldWork() {
        labeled.setAlignment(Pos.CENTER);
        assertEquals(Pos.CENTER, labeled.getAlignment());
    }

    @Test public void settingAlignmentAndThenCreatingAModelAndReadingTheValueStillWorks() {
        labeled.setAlignment(Pos.CENTER);
        assertEquals(Pos.CENTER, labeled.alignmentProperty().get());
    }

    @Test public void alignmentCanBeBound() {
        ObjectProperty<Pos> other = new SimpleObjectProperty<Pos>(Pos.BASELINE_RIGHT);
        labeled.alignmentProperty().bind(other);
        assertEquals(Pos.BASELINE_RIGHT, labeled.getAlignment());
    }

    @Test public void whenAlignmentIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)labeled.alignmentProperty()).getCssMetaData();
        ObjectProperty<Pos> other = new SimpleObjectProperty<Pos>(Pos.BASELINE_RIGHT);
        labeled.alignmentProperty().bind(other);
        assertFalse(styleable.isSettable(labeled));
    }

    @Test public void whenAlignmentIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)labeled.alignmentProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
    }

    @Test public void canSpecifyAlignmentViaCSS() {
        ((StyleableProperty)labeled.alignmentProperty()).applyStyle(null, Pos.CENTER_RIGHT);
        assertEquals(Pos.CENTER_RIGHT, labeled.getAlignment());
    }

    /********************************************************************************
     *                                                                              *
     *                           Tests for textAlignment                            *
     *                                                                              *
     *  - can be null                                                               *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - default is "LEFT"                                                         *
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void textAlignmentDefaultValueIsLEFT() {
        assertEquals(TextAlignment.LEFT, labeled.getTextAlignment());
        assertEquals(TextAlignment.LEFT, labeled.textAlignmentProperty().get());
    }

    @Test public void textAlignmentCanBeNull() {
        labeled.setTextAlignment(null);
        assertNull(labeled.getTextAlignment());
    }

    @Test public void settingTextAlignmentValueShouldWork() {
        labeled.setTextAlignment(TextAlignment.CENTER);
        assertEquals(TextAlignment.CENTER, labeled.getTextAlignment());
    }

    @Test public void settingTextAlignmentAndThenCreatingAModelAndReadingTheValueStillWorks() {
        labeled.setTextAlignment(TextAlignment.CENTER);
        assertEquals(TextAlignment.CENTER, labeled.textAlignmentProperty().get());
    }

    @Test public void textAlignmentCanBeBound() {
        ObjectProperty<TextAlignment> other = new SimpleObjectProperty<TextAlignment>(TextAlignment.RIGHT);
        labeled.textAlignmentProperty().bind(other);
        assertEquals(TextAlignment.RIGHT, labeled.getTextAlignment());
    }

    @Test public void whenTextAlignmentIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)labeled.textAlignmentProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
        ObjectProperty<TextAlignment> other = new SimpleObjectProperty<TextAlignment>(TextAlignment.RIGHT);
        labeled.textAlignmentProperty().bind(other);
        assertFalse(styleable.isSettable(labeled));
    }

    @Test public void whenTextAlignmentIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)labeled.textAlignmentProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
    }

    @Test public void canSpecifyTextAlignmentViaCSS() {
        ((StyleableProperty)labeled.textAlignmentProperty()).applyStyle(null, TextAlignment.JUSTIFY);
        assertEquals(TextAlignment.JUSTIFY, labeled.getTextAlignment());
    }

    /********************************************************************************
     *                                                                              *
     *                            Tests for textOverrun                             *
     *                                                                              *
     *  - can be null                                                               *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - default is "ELLIPSIS"                                                     *
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void textOverrunDefaultValueIsELLIPSIS() {
        assertEquals(OverrunStyle.ELLIPSIS, labeled.getTextOverrun());
        assertEquals(OverrunStyle.ELLIPSIS, labeled.textOverrunProperty().get());
    }

    @Test public void textOverrunCanBeNull() {
        labeled.setTextOverrun(null);
        assertNull(labeled.getTextOverrun());
    }

    @Test public void settingTextOverrunValueShouldWork() {
        labeled.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        assertEquals(OverrunStyle.CENTER_ELLIPSIS, labeled.getTextOverrun());
    }

    @Test public void settingTextOverrunAndThenCreatingAModelAndReadingTheValueStillWorks() {
        labeled.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        assertEquals(OverrunStyle.CENTER_ELLIPSIS, labeled.textOverrunProperty().get());
    }

    @Test public void textOverrunCanBeBound() {
        ObjectProperty<OverrunStyle> other = new SimpleObjectProperty<OverrunStyle>(OverrunStyle.LEADING_WORD_ELLIPSIS);
        labeled.textOverrunProperty().bind(other);
        assertEquals(OverrunStyle.LEADING_WORD_ELLIPSIS, labeled.getTextOverrun());
    }

    @Test public void whenTextOverrunIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)labeled.textOverrunProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
        ObjectProperty<OverrunStyle> other = new SimpleObjectProperty<OverrunStyle>(OverrunStyle.LEADING_WORD_ELLIPSIS);
        labeled.textOverrunProperty().bind(other);
        assertFalse(styleable.isSettable(labeled));
    }

    @Test public void whenTextOverrunIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)labeled.textOverrunProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
    }

    @Test public void canSpecifyTextOverrunViaCSS() {
        ((StyleableProperty)labeled.textOverrunProperty()).applyStyle(null, OverrunStyle.CENTER_WORD_ELLIPSIS);
        assertEquals(OverrunStyle.CENTER_WORD_ELLIPSIS, labeled.getTextOverrun());
    }

    /********************************************************************************
     *                                                                              *
     *                         Tests for wrapText property                          *
     *                                                                              *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - default is false                                                          *
     *  - contentBias changes based on wrapText                                     *
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void wrapTextDefaultValueIsFalse() {
        assertFalse(labeled.isWrapText());
        assertFalse(labeled.wrapTextProperty().get());
    }

    @Test public void settingWrapTextValueShouldWork() {
        labeled.setWrapText(true);
        assertTrue(labeled.isWrapText());
    }

    @Test public void settingWrapTextAndThenCreatingAModelAndReadingTheValueStillWorks() {
        labeled.setWrapText(true);
        assertTrue(labeled.wrapTextProperty().get());
    }

    @Test public void wrapTextCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        labeled.wrapTextProperty().bind(other);
        assertTrue(labeled.isWrapText());
    }

    @Test public void whenWrapTextIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)labeled.wrapTextProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
        BooleanProperty other = new SimpleBooleanProperty(true);
        labeled.wrapTextProperty().bind(other);
        assertFalse(styleable.isSettable(labeled));
    }

    @Test public void whenWrapTextIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)labeled.wrapTextProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
    }

    @Test public void whenWrapTextIsTrueContentBiasIsHorizontal() {
        labeled.setWrapText(true);
        assertEquals(Orientation.HORIZONTAL, labeled.getContentBias());
    }

    @Test public void whenWrapTextIsFalseContentBiasIsNull() {
        assertNull(labeled.getContentBias());
    }

    @Test public void canSpecifyWrapTextViaCSS() {
        ((StyleableProperty)labeled.wrapTextProperty()).applyStyle(null, Boolean.TRUE);
        assertTrue(labeled.isWrapText());
    }

    /********************************************************************************
     *                                                                              *
     *                           Tests for font property                            *
     *                                                                              *
     *  - can be null                                                               *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - default is Font.getDefault()                                              *
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void fontDefaultValueIsFont_getDefault() {
        final Font def = Font.getDefault();
        assertEquals(def, labeled.getFont());
        assertEquals(def, labeled.fontProperty().get());
    }

    @Test public void fontCanBeNull() {
        labeled.setFont(null);
        assertNull(labeled.getFont());
    }

    @Test public void settingFontValueShouldWork() {
        final Font f = Font.font("Arial", 25);
        labeled.setFont(f);
        assertEquals(f, labeled.getFont());
    }

    @Test public void settingFontAndThenCreatingAModelAndReadingTheValueStillWorks() {
        final Font f = Font.font("Arial", 25);
        labeled.setFont(f);
        assertEquals(f, labeled.fontProperty().get());
    }

    @Test public void fontCanBeBound() {
        final Font f = Font.font("Arial", 25);
        ObjectProperty<Font> other = new SimpleObjectProperty<Font>(f);
        labeled.fontProperty().bind(other);
        assertEquals(f, labeled.getFont());
    }

    @Test public void whenFontIsBound_CssMetaData_isSettable_ReturnsFalse() {
        final Font f = Font.font("Arial", 25);
        CssMetaData styleable = ((StyleableProperty)labeled.fontProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
        ObjectProperty<Font> other = new SimpleObjectProperty<Font>(f);
        labeled.fontProperty().bind(other);
        assertFalse(styleable.isSettable(labeled));
    }

    @Test public void whenFontIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        final Font f = Font.font("Arial", 25);
        CssMetaData styleable = ((StyleableProperty)labeled.fontProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
    }

    @Test public void canSpecifyFontViaCSS() {
        final Font f = Font.font("Arial", 25);
        ((StyleableProperty)labeled.fontProperty()).applyStyle(null, f);
        assertEquals(f, labeled.getFont());
    }

    /********************************************************************************
     *                                                                              *
     *                         Tests for graphic property                           *
     *                                                                              *
     *  - can be null                                                               *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - default is null                                                           *
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void graphicDefaultValueIsNull() {
        assertEquals(null, labeled.getGraphic());
        assertEquals(null, labeled.graphicProperty().get());
    }

    @Test public void graphicCanBeNull() {
        labeled.setGraphic(new Rectangle());
        labeled.setGraphic(null);
        assertNull(labeled.getGraphic());
    }

    @Test public void settingGraphicValueShouldWork() {
        Rectangle r = new Rectangle();
        labeled.setGraphic(r);
        assertEquals(r, labeled.getGraphic());
    }

    @Test public void settingGraphicAndThenCreatingAModelAndReadingTheValueStillWorks() {
        Rectangle r = new Rectangle();
        labeled.setGraphic(r);
        assertEquals(r, labeled.graphicProperty().get());
    }

    @Test public void graphicCanBeBound() {
        Rectangle r = new Rectangle();
        ObjectProperty<Node> other = new SimpleObjectProperty<Node>(r);
        labeled.graphicProperty().bind(other);
        assertEquals(r, labeled.getGraphic());
    }

    @Test public void whenGraphicIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)labeled.graphicProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
        Rectangle r = new Rectangle();
        ObjectProperty<Node> other = new SimpleObjectProperty<Node>(r);
        labeled.graphicProperty().bind(other);
        assertFalse(styleable.isSettable(labeled));
    }

    @Test public void whenGraphicIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)labeled.graphicProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
    }

    @Ignore ("CSS Graphic must be a URL, and then it will try to load the image. Not sure how to test.")
    @Test public void canSpecifyGraphicViaCSS() {
        ((StyleableProperty)labeled.graphicProperty()).applyStyle(null, "/some/url");
        assertNotNull(labeled.getGraphic());
    }

    /********************************************************************************
     *                                                                              *
     *                         Tests for underline property                         *
     *                                                                              *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - default is false                                                          *
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void underlineDefaultValueIsFalse() {
        assertFalse(labeled.isUnderline());
        assertFalse(labeled.underlineProperty().get());
    }

    @Test public void settingUnderlineValueShouldWork() {
        labeled.setUnderline(true);
        assertTrue(labeled.isUnderline());
    }

    @Test public void settingUnderlineAndThenCreatingAModelAndReadingTheValueStillWorks() {
        labeled.setUnderline(true);
        assertTrue(labeled.underlineProperty().get());
    }

    @Test public void underlineCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        labeled.underlineProperty().bind(other);
        assertTrue(labeled.isUnderline());
    }

    @Test public void whenUnderlineIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)labeled.underlineProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
        BooleanProperty other = new SimpleBooleanProperty(true);
        labeled.underlineProperty().bind(other);
        assertFalse(styleable.isSettable(labeled));
    }

    @Test public void whenUnderlineIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)labeled.underlineProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
    }

    @Test public void canSpecifyUnderlineViaCSS() {
        ((StyleableProperty)labeled.underlineProperty()).applyStyle(null, Boolean.TRUE);
        assertTrue(labeled.isUnderline());
    }

    /********************************************************************************
     *                                                                              *
     *                           Tests for contentDisplay                           *
     *                                                                              *
     *  - can be null                                                               *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - default is "LEFT"                                                         *
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void contentDisplayDefaultValueIsLEFT() {
        assertEquals(ContentDisplay.LEFT, labeled.getContentDisplay());
        assertEquals(ContentDisplay.LEFT, labeled.contentDisplayProperty().get());
    }

    @Test public void contentDisplayCanBeNull() {
        labeled.setContentDisplay(null);
        assertNull(labeled.getContentDisplay());
    }

    @Test public void settingContentDisplayValueShouldWork() {
        labeled.setContentDisplay(ContentDisplay.CENTER);
        assertEquals(ContentDisplay.CENTER, labeled.getContentDisplay());
    }

    @Test public void settingContentDisplayAndThenCreatingAModelAndReadingTheValueStillWorks() {
        labeled.setContentDisplay(ContentDisplay.CENTER);
        assertEquals(ContentDisplay.CENTER, labeled.contentDisplayProperty().get());
    }

    @Test public void contentDisplayCanBeBound() {
        ObjectProperty<ContentDisplay> other = new SimpleObjectProperty<ContentDisplay>(ContentDisplay.RIGHT);
        labeled.contentDisplayProperty().bind(other);
        assertEquals(ContentDisplay.RIGHT, labeled.getContentDisplay());
    }

    @Test public void whenContentDisplayIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)labeled.contentDisplayProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
        ObjectProperty<ContentDisplay> other = new SimpleObjectProperty<ContentDisplay>(ContentDisplay.RIGHT);
        labeled.contentDisplayProperty().bind(other);
        assertFalse(styleable.isSettable(labeled));
    }

    @Test public void whenContentDisplayIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)labeled.contentDisplayProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
    }

    @Test public void canSpecifyContentDisplayViaCSS() {
        ((StyleableProperty)labeled.contentDisplayProperty()).applyStyle(null, ContentDisplay.GRAPHIC_ONLY);
        assertSame(ContentDisplay.GRAPHIC_ONLY, labeled.getContentDisplay());
    }

    /********************************************************************************
     *                                                                              *
     *                          Tests for graphicTextGap                            *
     *                                                                              *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - default is 4                                                              *
     *  - if bound, CssMetaData_isSettable returns false                                  *
     *  - if specified via CSS and not bound, CssMetaData_isSettable returns true         *
     *                                                                              *
     *******************************************************************************/

    @Test public void graphicTextGapDefaultValueIsFour() {
        assertEquals(4, labeled.getGraphicTextGap(), 0);
        assertEquals(4, labeled.graphicTextGapProperty().get(), 0);
    }

    @Test public void settingGraphicTextGapValueShouldWork() {
        labeled.setGraphicTextGap(8);
        assertEquals(8, labeled.getGraphicTextGap(), 0);
    }

    @Test public void settingGraphicTextGapNegativeShouldWork() {
        labeled.setGraphicTextGap(-5.5);
        assertEquals(-5.5, labeled.getGraphicTextGap(), 0);
    }

    @Test public void settingGraphicTextGapAndThenCreatingAModelAndReadingTheValueStillWorks() {
        labeled.setGraphicTextGap(8);
        assertEquals(8, labeled.graphicTextGapProperty().get(), 0);
    }

    @Test public void graphicTextGapCanBeBound() {
        DoubleProperty other = new SimpleDoubleProperty(25);
        labeled.graphicTextGapProperty().bind(other);
        assertEquals(25, labeled.getGraphicTextGap(), 0);
    }

    @Test public void whenGraphicTextGapIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)labeled.graphicTextGapProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
        DoubleProperty other = new SimpleDoubleProperty(25);
        labeled.graphicTextGapProperty().bind(other);
        assertFalse(styleable.isSettable(labeled));
    }

    @Test public void whenGraphicTextGapIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)labeled.graphicTextGapProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(labeled));
    }

    @Test public void canSpecifyGraphicTextGapViaCSS() {
        ((StyleableProperty)labeled.graphicTextGapProperty()).applyStyle(null,  8.0);
        assertEquals(8, labeled.getGraphicTextGap(), 0);
    }

    /********************************************************************************
     *                                                                              *
     *                          Tests for labelPadding                              *
     *                                                                              *
     *******************************************************************************/

    @Test public void labelPaddingDefaultValueIsEmptyInsets() {
        assertEquals(Insets.EMPTY, labeled.getLabelPadding());
        assertEquals(Insets.EMPTY, labeled.labelPaddingProperty().get());
    }

    @Test public void canSpecifyLabelPaddingFromCSS() {
        Insets insets = new Insets(5, 4, 3, 2);
        CssMetaData styleable = ((StyleableProperty)labeled.labelPaddingProperty()).getCssMetaData();
        styleable.set(labeled, insets, null);
        assertEquals(insets, labeled.getLabelPadding());
        assertEquals(insets, labeled.labelPaddingProperty().get());
    }

    /********************************************************************************
     *                                                                              *
     *                             Helper classes and such                          *
     *                                                                              *
     *******************************************************************************/

    public static final class LabeledMock extends Labeled {
        public LabeledMock() { super(); }
        public LabeledMock(String text) { super(text); }
        public LabeledMock(String text, Node graphic) { super(text, graphic); }
    }
}
