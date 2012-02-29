/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Need to test:
 *  - String truncation works correctly
 *  - min/max/pref width/height when multiline text is turned on
 */
public class LabelSkinTest {
    private Label label;
    private LabelSkinMock skin;
    private Text text;
    
    @Before public void setup() {
        label = new Label();
        skin = new LabelSkinMock(label);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        skin.setPadding(new Insets(10, 10, 10, 10));
        label.setSkin(skin);
        // It so happens that a brand new LabelSkin on a plain Label
        // will have as its only child the Text node
        text = (Text) skin.getChildrenUnmodifiable().get(0);
    }
    
    /****************************************************************************
     *                                                                          *
     * Tests for change notification                                            *
     *                                                                          *
     ***************************************************************************/
    
    @Test public void sizeChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        assertFalse(skin.propertyChanged); // sanity check
        label.resize(500, label.getHeight());
        assertTrue(skin.propertyChanged);
        assertEquals(1, skin.propertyChangeCount); // sanity check
        label.resize(label.getWidth(), label.prefHeight(label.getWidth()));
        assertEquals(2, skin.propertyChangeCount); // sanity check
    }
    
    @Test public void textFillChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        label.setTextFill(Color.PURPLE);
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void fontChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        final Font f = Font.font("Arial", 64);
        label.setFont(f);
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void graphicChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        label.setGraphic(new Rectangle());
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void contentDisplayChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void graphicTextGapChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        label.setGraphicTextGap(60.34);
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void hposChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        label.setAlignment(Pos.CENTER_RIGHT);
        label.setAlignment(Pos.CENTER_RIGHT);
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void vposChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        label.setAlignment(Pos.TOP_CENTER);
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void textChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        label.setText("Bust my buffers!");
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void textAlignmentChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        label.setTextAlignment(TextAlignment.JUSTIFY);
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void textOverrunChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        label.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void wrapTextChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        label.setWrapText(true);
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void underlineChangesOnLabelShouldInvoke_handleControlPropertyChanged() {
        label.setUnderline(true);
        assertTrue(skin.propertyChanged);
    }
    
    @Test public void uninterestingChangesOnLabelShouldNotInvoke_handleControlPropertyChanged() {
        label.setBlendMode(BlendMode.BLUE);
        assertFalse(skin.propertyChanged);
    }
    
    /****************************************************************************
     *                                                                          *
     * Tests for invalidation. When each of various properties change, we need  *
     * to invalidate some state, such as via requestLayout().                   *
     *                                                                          *
     ***************************************************************************/
    
    @Test public void graphicLayoutBoundsChangeShouldInvalidateLayoutAndDisplayText() {
        final Rectangle r = new Rectangle(20, 20);
        label.setGraphic(r);
        label.layout();
        skin.updateDisplayedText();
        
        r.setWidth(30);
        assertTrue(label.isNeedsLayout());
        assertTrue(skin.invalidText);
    }
    
    @Test public void widthChangesOnLabelShouldInvalidateLayoutAndDisplayText() {
        label.layout();
        skin.updateDisplayedText();

        label.resize(500, label.getHeight());
        assertTrue(label.isNeedsLayout());
        assertTrue(skin.invalidText);
    }
    
    @Test public void widthChangesWhenWrapTextIsTrueUpdatesTheWrappingWidth() {
        // Assert that the wrapping width changes with the label width if wrapText is true.
        // However, this will only be required if the longest line in the text is itself
        // wider than the label's available width.
        label.setText("A long line which is wider than 100 pixels.");
        label.setWrapText(true);
        label.autosize();
        //label.layout();
        skin.updateDisplayedText();
        
        final double oldWrappingWidth = text.getWrappingWidth();
        label.resize(100, label.getHeight());
        skin.updateDisplayedText();
        assertFalse(oldWrappingWidth == text.getWrappingWidth());
        assertTrue(text.getWrappingWidth() > 0);
    }
    
    @Test public void widthChangesWhenWrapTextIsFalseKeepsWrappingWidthAtZero() {
        label.layout();
        skin.updateDisplayedText();
        
        label.resize(500, label.getHeight());
        assertEquals(0, text.getWrappingWidth(), 0);
    }
    
    @Test public void fontChangesOnLabelShouldInvalidateLayoutAndDisplayTextAndEllipsesWidthAndTextWidth() {
        label.layout();
        skin.updateDisplayedText();
        
        label.setFont(Font.font("Arial", 37));
        assertTrue(label.isNeedsLayout());
        assertTrue(skin.invalidText);
        assertEquals(Double.NEGATIVE_INFINITY, skin.textWidth, 0);
        assertEquals(Double.NEGATIVE_INFINITY, skin.ellipsisWidth, 0);
    }

    @Test public void graphicChangesOnLabelShouldInvalidateLayoutAndDisplayText() {
        label.layout();
        skin.updateDisplayedText();
        
        label.setGraphic(new Rectangle(20, 20));
        assertTrue(label.isNeedsLayout());
        assertTrue(skin.invalidText);
    }

    @Test public void contentDisplayChangesOnLabelShouldInvalidateLayoutAndDisplayText() {
        label.layout();
        skin.updateDisplayedText();
        
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        assertTrue(label.isNeedsLayout());
        assertTrue(skin.invalidText);
    }

    @Test public void graphicTextGapChangesOnLabelShouldInvalidateLayoutAndDisplayText() {
        label.layout();
        skin.updateDisplayedText();
        
        label.setGraphicTextGap(8.37);
        assertTrue(label.isNeedsLayout());
        assertTrue(skin.invalidText);
    }

    @Test public void hposChangesOnLabelShouldInvalidateLayout() {
        label.layout();
        skin.updateDisplayedText();
        
        label.setAlignment(Pos.CENTER_RIGHT);
        assertTrue(label.isNeedsLayout());
        assertFalse(skin.invalidText);
    }

    @Test public void vposChangesOnLabelShouldInvalidateLayout() {
        label.layout();
        skin.updateDisplayedText();
        
        label.setAlignment(Pos.TOP_CENTER);
        assertTrue(label.isNeedsLayout());
        assertFalse(skin.invalidText);
    }

    @Test public void textChangesOnLabelShouldInvalidateLayoutAndDisplayTextAndTextWidth() {
        label.layout();
        skin.updateDisplayedText();
        
        label.setText("Apples and Oranges");
        assertTrue(label.isNeedsLayout());
        assertTrue(skin.invalidText);
        assertEquals(Double.NEGATIVE_INFINITY, skin.textWidth, 0);
    }
// TODO(aim): changing textAlignment doesn't actually change Text layoutBounds
//
//    @Test public void textAlignmentChangesOnLabelShouldInvalidateLayout() {
//        label.layout();
//        skin.updateDisplayedText();
//
//        label.setTextAlignment(TextAlignment.JUSTIFY);
//        assertTrue(label.isNeedsLayout());
//        assertFalse(skin.invalidText);
//    }

    @Test public void textOverrunChangesOnLabelShouldInvalidateLayoutAndDisplayText() {
        label.layout();
        skin.updateDisplayedText();
        
        label.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
        assertTrue(label.isNeedsLayout());
        assertTrue(skin.invalidText);
    }

    @Test public void wrapTextChangesOnLabelShouldInvalidateLayoutAndDisplayTextAndWrappingWidth() {
        label.setText("A long line which is wider than 100 pixels.");
        label.resize(100, 30);
        label.layout();
        skin.updateDisplayedText();

        final double oldWrappingWidth = text.getWrappingWidth();
        label.setWrapText(true);
        assertTrue(label.isNeedsLayout());
        assertTrue(skin.invalidText);
        assertFalse(oldWrappingWidth == text.getWrappingWidth());
        assertTrue(text.getWrappingWidth() > 0);
    }

    @Test public void underlineChangesOnLabelShouldInvalidateLayoutAndDisplayText() {
        label.layout();
        skin.updateDisplayedText();
        
        label.setUnderline(true);
        assertTrue(label.isNeedsLayout());
        assertTrue(skin.invalidText);
    }

    /****************************************************************************
     *                                                                          *
     * Tests for minWidth                                                       *
     *                                                                          *
     ***************************************************************************/

    @Test public void whenTextIsNullAndNoGraphic_computeMinWidth_ReturnsZero() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        label.setText(null);
        assertEquals(0.0 + 14, label.minWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndNoGraphic_computeMinWidth_ReturnsZero() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        label.setText("");
        assertEquals(0.0 + 14, label.minWidth(-1), 0);
    }
    
    @Test public void whenTextIsShorterThanEllipsisAndNoGraphic_computeMinWidth_ReturnsTextWidth() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        label.setText(".");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(skin.textWidth + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsGreaterThanEllipsisAndNoGraphic_computeMinWidth_ReturnsEllipsisWidth() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        label.setText("These are the times that try men's souls.");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(skin.ellipsisWidth + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsNullAndGraphicIsUnmanaged_computeMinWidth_ReturnsZero() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        label.setText(null);
        assertEquals(0.0 + 14, label.minWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsUnmanaged_computeMinWidth_ReturnsZero() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        label.setText("");
        assertEquals(0.0 + 14, label.minWidth(-1), 0);
    }
    
    @Test public void whenTextIsShorterThanEllipsisAndGraphicIsUnmanaged_computeMinWidth_ReturnsTextWidth() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        label.setText(".");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(skin.textWidth + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsGreaterThanEllipsisAndGraphicIsUnmanaged_computeMinWidth_ReturnsEllipsisWidth() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        label.setText("These are the times that try men's souls.");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(skin.ellipsisWidth + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsNullAndGraphicIsSet_computeMinWidth_ReturnsGraphicWidth() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setText(null);
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(23 + 14, label.minWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSet_computeMinWidth_ReturnsGraphicWidth() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setText("");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(23 + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsShorterThanEllipsisAndGraphicIsSetAndContentDisplayIsLEFT_computeMinWidth_ReturnsGraphicWidthPlusGraphicTextGapPlusTextWidth() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.LEFT);
        label.setText(".");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(23 + 2 + skin.textWidth + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsLongThanEllipsisAndGraphicIsSetAndContentDisplayIsLEFT_computeMinWidth_ReturnsGraphicWidthPlusGraphicTextGapPlusEllipsisWidth() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.LEFT);
        label.setText("Wherefore art thou Romeo?");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(23 + 2 + skin.ellipsisWidth + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsShorterThanEllipsisAndGraphicIsSetAndContentDisplayIsRIGHT_computeMinWidth_ReturnsGraphicWidthPlusGraphicTextGapPlusTextWidth() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.setText(".");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(23 + 2 + skin.textWidth + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsLongThanEllipsisAndGraphicIsSetAndContentDisplayIsRIGHT_computeMinWidth_ReturnsGraphicWidthPlusGraphicTextGapPlusEllipsisWidth() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.setText("Wherefore art thou Romeo?");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(23 + 2 + skin.ellipsisWidth + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsShorterThanEllipsisAndGraphicIsSetAndContentDisplayIsCENTER_computeMinWidth_ReturnsRightAnswer() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.CENTER);
        label.setText(".");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(Math.max(23, skin.textWidth) + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsLongThanEllipsisAndGraphicIsSetAndContentDisplayIsCENTER_computeMinWidth_ReturnsRightAnswer() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.CENTER);
        label.setText("Wherefore art thou Romeo?");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(Math.max(23, skin.ellipsisWidth) + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsShorterThanEllipsisAndGraphicIsSetAndContentDisplayIsTOP_computeMinWidth_ReturnsRightAnswer() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TOP);
        label.setText(".");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(Math.max(23, skin.textWidth) + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsLongThanEllipsisAndGraphicIsSetAndContentDisplayIsTOP_computeMinWidth_ReturnsRightAnswer() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TOP);
        label.setText("Wherefore art thou Romeo?");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(Math.max(23, skin.ellipsisWidth) + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsShorterThanEllipsisAndGraphicIsSetAndContentDisplayIsBOTTOM_computeMinWidth_ReturnsRightAnswer() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        label.setText(".");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(Math.max(23, skin.textWidth) + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsLongThanEllipsisAndGraphicIsSetAndContentDisplayIsBOTTOM_computeMinWidth_ReturnsRightAnswer() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        label.setText("Wherefore art thou Romeo?");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(Math.max(23, skin.ellipsisWidth) + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsShorterThanEllipsisAndGraphicIsSetAndContentDisplayIsGRAPHIC_ONLY_computeMinWidth_ReturnsRightAnswer() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(3, 3);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.setText(".");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(3 + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsLongThanEllipsisAndGraphicIsSetAndContentDisplayIsGRAPHIC_ONLY_computeMinWidth_ReturnsRightAnswer() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(3, 3);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.setText("Wherefore art thou Romeo?");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(3 + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsShorterThanEllipsisAndGraphicIsSetAndContentDisplayIsTEXT_ONLY_computeMinWidth_ReturnsRightAnswer() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(230, 230);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.setText(".");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(skin.textWidth + 14, label.minWidth(-1), 0);
    }

    @Test public void whenTextIsLongThanEllipsisAndGraphicIsSetAndContentDisplayIsTEXT_ONLY_computeMinWidth_ReturnsRightAnswer() {
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(230, 230);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.setText("Wherefore art thou Romeo?");
        assertTrue(label.minWidth(-1) >= 0);
        assertEquals(skin.ellipsisWidth + 14, label.minWidth(-1), 0);
    }

    /****************************************************************************
     *                                                                          *
     * Tests for minHeight                                                      *
     *                                                                          *
     ***************************************************************************/

    @Test public void whenTextIsNullAndNoGraphic_computeMinHeight_ReturnsSingleLineStringHeight() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(lineHeight + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndNoGraphic_computeMinHeight_ReturnsSingleLineStringHeight() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(lineHeight + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndNoGraphic_computeMinHeight_ReturnsSingleLineStringHeight() {
        label.setText("Howdy Pardner");
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertTrue(label.minHeight(-1) >= 0);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(lineHeight + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndUnmanagedGraphic_computeMinHeight_ReturnsSingleLineStringHeight() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(lineHeight + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndUnmanagedGraphic_computeMinHeight_ReturnsSingleLineStringHeight() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(lineHeight + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndUnmanagedGraphic_computeMinHeight_ReturnsSingleLineStringHeight() {
        label.setText("Howdy Pardner");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertTrue(label.minHeight(-1) >= 0);
        assertEquals(lineHeight + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithTOPContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TOP);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(23 + lineHeight + 2 + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithTOPContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TOP);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(23 + lineHeight + 2 + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithTOPContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setGraphicTextGap(2);
        label.setContentDisplay(ContentDisplay.TOP);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(23 + lineHeight + 2 + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithRIGHTContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.RIGHT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(Math.max(23, lineHeight) + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithRIGHTContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.RIGHT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(Math.max(23, lineHeight) + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithRIGHTContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.RIGHT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(Math.max(23, lineHeight) + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithBOTTOMContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(23 + lineHeight + 2 + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithBOTTOMContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(23 + lineHeight + 2 + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithBOTTOMContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setGraphicTextGap(2);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(23 + lineHeight + 2 + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithLEFTContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.LEFT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(Math.max(23, lineHeight) + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithLEFTContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.LEFT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(Math.max(23, lineHeight) + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithLEFTContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.LEFT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(Math.max(23, lineHeight) + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithCENTERContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.CENTER);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(Math.max(23, lineHeight) + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithCENTERContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.CENTER);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(Math.max(23, lineHeight) + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithCENTERContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.CENTER);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(Math.max(23, lineHeight) + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        assertEquals(23 + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        assertEquals(23 + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        assertEquals(23 + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithTEXT_ONLYContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(lineHeight + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithTEXT_ONLYContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(lineHeight + 14, label.minHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithTEXT_ONLYContentDisplay_computeMinHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(lineHeight + 14, label.minHeight(-1), 0);
    }
    
    /****************************************************************************
     *                                                                          *
     * Tests for prefWidth                                                      *
     *                                                                          *
     ***************************************************************************/

    @Test public void whenTextIsNullAndNoGraphic_computePrefWidth_ReturnsPadding() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndNoGraphic_computePrefWidth_ReturnsPadding() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndNoGraphic_computePrefWidth_ReturnsTextWidthPlusPadding() {
        label.setText("Lollipop");
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "Lollipop", 0);
        assertEquals(14 + textWidth, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsNullAndUnmanagedGraphic_computePrefWidth_ReturnsPadding() {
        label.setText(null);
        Rectangle r = new Rectangle(0, 0, 50, 50);
        r.setManaged(false);
        label.setGraphic(r);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndUnmanagedGraphic_computePrefWidth_ReturnsPadding() {
        label.setText("");
        Rectangle r = new Rectangle(0, 0, 50, 50);
        r.setManaged(false);
        label.setGraphic(r);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndUnmanagedGraphic_computePrefWidth_ReturnsTextWidthPlusPadding() {
        label.setText("Lollipop");
        Rectangle r = new Rectangle(0, 0, 50, 50);
        r.setManaged(false);
        label.setGraphic(r);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "Lollipop", 0);
        assertEquals(14 + textWidth, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithTOPContentDisplay_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TOP);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithTOPContentDisplay_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TOP);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithTOPContentDisplayAndGraphicIsWider_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("Lollipop");
        label.setGraphic(new Rectangle(0, 0, 200, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TOP);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 200, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithTOPContentDisplayAndTextIsWider_computePrefWidth_ReturnsTextWidthPlusPadding() {
        label.setText("This is the right place");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TOP);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "This is the right place", 0);
        assertEquals(14 + textWidth, label.prefWidth(-1), 0);
    }

    @Test public void whenTextIsNullAndGraphicIsSetWithRIGHTContentDisplay_computePrefWidth_ReturnsGraphicPlusPaddingNotIncludingGap() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.RIGHT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(20 + 14, label.prefWidth(-1), 0);
    }

    @Test public void whenTextIsEmptyAndGraphicIsSetWithRIGHTContentDisplay_computePrefWidth_ReturnsGraphicPlusPaddingNotIncludingGap() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.RIGHT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(20 + 14, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithRIGHTContentDisplay_computePrefWidth_ReturnsTextWidthPlusGraphicWidthPlusGapPlusPadding() {
        label.setText("Howdy");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.RIGHT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "Howdy", 0);
        assertEquals(14 + textWidth + 6.5 + 20, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithBOTTOMContentDisplay_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithBOTTOMContentDisplay_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithBOTTOMContentDisplayAndGraphicIsWider_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("Lollipop");
        label.setGraphic(new Rectangle(0, 0, 200, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 200, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithBOTTOMContentDisplayAndTextIsWider_computePrefWidth_ReturnsTextWidthPlusPadding() {
        label.setText("This is the right place");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "This is the right place", 0);
        assertEquals(14 + textWidth, label.prefWidth(-1), 0);
    }

    @Test public void whenTextIsNullAndGraphicIsSetWithLEFTContentDisplay_computePrefWidth_ReturnsGraphicPlusPaddingNotIncludingGap() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.LEFT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(20 + 14, label.prefWidth(-1), 0);
    }

    @Test public void whenTextIsEmptyAndGraphicIsSetWithLEFTContentDisplay_computePrefWidth_ReturnsGraphicPlusPaddingNotIncludingGap() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.LEFT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(20 + 14, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithLEFTContentDisplay_computePrefWidth_ReturnsTextWidthPlusGraphicWidthPlusGapPlusPadding() {
        label.setText("Howdy");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.LEFT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "Howdy", 0);
        assertEquals(14 + textWidth + 6.5 + 20, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithCENTERContentDisplay_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.CENTER);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithCENTERContentDisplay_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.CENTER);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithCENTERContentDisplayAndGraphicIsWider_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("Lollipop");
        label.setGraphic(new Rectangle(0, 0, 200, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.CENTER);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 200, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithCENTERContentDisplayAndTextIsWider_computePrefWidth_ReturnsTextWidthPlusPadding() {
        label.setText("This is the right place");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.CENTER);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "This is the right place", 0);
        assertEquals(14 + textWidth, label.prefWidth(-1), 0);
    }

    @Test public void whenTextIsNullAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithGRAPHIC_ONLYContentDisplayAndGraphicIsWider_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("Howdy");
        label.setGraphic(new Rectangle(0, 0, 200, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 200, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithGRAPHIC_ONLYContentDisplayAndTextIsWider_computePrefWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("Tally ho, off to the races");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.prefWidth(-1), 0);
    }
    
    // TODO should this include the gap??
    @Test public void whenTextIsNullAndGraphicIsSetWithTEXT_ONLYContentDisplay_computePrefWidth_ReturnsPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.prefWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithTEXT_ONLYContentDisplay_computePrefWidth_ReturnsPadding() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.prefWidth(-1), 0);
    }
    
    // TODO should this include the gap? I guess not, otherwise our gap would have to default to 0
    @Test public void whenTextIsSetAndGraphicIsSetWithTEXT_ONLYContentDisplay_computePrefWidth_ReturnsTextWidthPlusPadding() {
        label.setText("Yippee Skippee");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "Yippee Skippee", 0);
        assertEquals(14 + textWidth, label.prefWidth(-1), 0);
    }
    
    /****************************************************************************
     *                                                                          *
     * Tests for prefHeight                                                     *
     *                                                                          *
     ***************************************************************************/

    @Test public void whenTextHasNewlines_computePrefHeight_IncludesTheMultipleLinesInThePrefHeight() {
        label.setText("This\nis a test\nof the emergency\nbroadcast system.\nThis is only a test");
        skin.setPadding(new Insets(0, 0, 0, 0));
        final double singleLineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        final double height = label.prefHeight(-1);
        assertTrue(height >= singleLineHeight * 5);
    }
    
    @Test public void whenTextHasNewlinesAfterPreviousComputationOf_computePrefHeight_IncludesTheMultipleLinesInThePrefHeight() {
        label.setText("This is a test");
        final double oldPrefHeight = label.prefHeight(-1);
        label.setText("This\nis a test\nof the emergency\nbroadcast system.\nThis is only a test");
        final double newPrefHeight = label.prefHeight(-1);
        assertTrue(oldPrefHeight != newPrefHeight);
    }
    
    @Test public void whenTextIsNullAndNoGraphic_computePrefHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndNoGraphic_computePrefHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndNoGraphic_computePrefHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText("Howdy Pardner");
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertTrue(label.prefHeight(-1) >= 0);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndUnmanagedGraphic_computePrefHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndUnmanagedGraphic_computePrefHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndUnmanagedGraphic_computePrefHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText("Howdy Pardner");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertTrue(label.prefHeight(-1) >= 0);
        assertEquals(14 + lineHeight, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithTOPContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TOP);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithTOPContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TOP);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithTOPContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setGraphicTextGap(2);
        label.setContentDisplay(ContentDisplay.TOP);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithRIGHTContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.RIGHT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithRIGHTContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.RIGHT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithRIGHTContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.RIGHT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithBOTTOMContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithBOTTOMContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithBOTTOMContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setGraphicTextGap(2);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithLEFTContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.LEFT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithLEFTContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.LEFT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithLEFTContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.LEFT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithCENTERContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.CENTER);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithCENTERContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.CENTER);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithCENTERContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.CENTER);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        assertEquals(14 + 23, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        assertEquals(14 + 23, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        assertEquals(14 + 23, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithTEXT_ONLYContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithTEXT_ONLYContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.prefHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithTEXT_ONLYContentDisplay_computePrefHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.prefHeight(-1), 0);
    }
        
    /****************************************************************************
     *                                                                          *
     * Tests maxWidth                                                           *
     *                                                                          *
     ***************************************************************************/

    @Test public void whenTextIsNullAndNoGraphic_computeMaxWidth_ReturnsPadding() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndNoGraphic_computeMaxWidth_ReturnsPadding() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndNoGraphic_computeMaxWidth_ReturnsTextWidthPlusPadding() {
        label.setText("Lollipop");
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "Lollipop", 0);
        assertEquals(14 + textWidth, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsNullAndUnmanagedGraphic_computeMaxWidth_ReturnsPadding() {
        label.setText(null);
        Rectangle r = new Rectangle(0, 0, 50, 50);
        r.setManaged(false);
        label.setGraphic(r);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndUnmanagedGraphic_computeMaxWidth_ReturnsPadding() {
        label.setText("");
        Rectangle r = new Rectangle(0, 0, 50, 50);
        r.setManaged(false);
        label.setGraphic(r);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndUnmanagedGraphic_computeMaxWidth_ReturnsTextWidthPlusPadding() {
        label.setText("Lollipop");
        Rectangle r = new Rectangle(0, 0, 50, 50);
        r.setManaged(false);
        label.setGraphic(r);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "Lollipop", 0);
        assertEquals(14 + textWidth, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithTOPContentDisplay_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TOP);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithTOPContentDisplay_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TOP);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithTOPContentDisplayAndGraphicIsWider_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("Lollipop");
        label.setGraphic(new Rectangle(0, 0, 200, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TOP);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 200, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithTOPContentDisplayAndTextIsWider_computeMaxWidth_ReturnsTextWidthPlusPadding() {
        label.setText("This is the right place");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TOP);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "This is the right place", 0);
        assertEquals(14 + textWidth, label.maxWidth(-1), 0);
    }

    @Test public void whenTextIsNullAndGraphicIsSetWithRIGHTContentDisplay_computeMaxWidth_ReturnsGraphicPlusPaddingNotIncludingGap() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.RIGHT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(20 + 14, label.maxWidth(-1), 0);
    }

    @Test public void whenTextIsEmptyAndGraphicIsSetWithRIGHTContentDisplay_computeMaxWidth_ReturnsGraphicPlusPaddingNotIncludingGap() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.RIGHT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(20 + 14, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithRIGHTContentDisplay_computeMaxWidth_ReturnsTextWidthPlusGraphicWidthPlusGapPlusPadding() {
        label.setText("Howdy");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.RIGHT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "Howdy", 0);
        assertEquals(14 + textWidth + 6.5 + 20, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithBOTTOMContentDisplay_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithBOTTOMContentDisplay_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithBOTTOMContentDisplayAndGraphicIsWider_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("Lollipop");
        label.setGraphic(new Rectangle(0, 0, 200, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 200, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithBOTTOMContentDisplayAndTextIsWider_computeMaxWidth_ReturnsTextWidthPlusPadding() {
        label.setText("This is the right place");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "This is the right place", 0);
        assertEquals(14 + textWidth, label.maxWidth(-1), 0);
    }

    @Test public void whenTextIsNullAndGraphicIsSetWithLEFTContentDisplay_computeMaxWidth_ReturnsGraphicPlusPaddingNotIncludingGap() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.LEFT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(20 + 14, label.maxWidth(-1), 0);
    }

    @Test public void whenTextIsEmptyAndGraphicIsSetWithLEFTContentDisplay_computeMaxWidth_ReturnsGraphicPlusPaddingNotIncludingGap() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.LEFT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(20 + 14, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithLEFTContentDisplay_computeMaxWidth_ReturnsTextWidthPlusGraphicWidthPlusGapPlusPadding() {
        label.setText("Howdy");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.LEFT);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "Howdy", 0);
        assertEquals(14 + textWidth + 6.5 + 20, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithCENTERContentDisplay_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.CENTER);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithCENTERContentDisplay_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.CENTER);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithCENTERContentDisplayAndGraphicIsWider_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("Lollipop");
        label.setGraphic(new Rectangle(0, 0, 200, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.CENTER);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 200, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithCENTERContentDisplayAndTextIsWider_computeMaxWidth_ReturnsTextWidthPlusPadding() {
        label.setText("This is the right place");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.CENTER);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "This is the right place", 0);
        assertEquals(14 + textWidth, label.maxWidth(-1), 0);
    }

    @Test public void whenTextIsNullAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithGRAPHIC_ONLYContentDisplayAndGraphicIsWider_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("Howdy");
        label.setGraphic(new Rectangle(0, 0, 200, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 200, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithGRAPHIC_ONLYContentDisplayAndTextIsWider_computeMaxWidth_ReturnsGraphicWidthPlusPadding() {
        label.setText("Tally ho, off to the races");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14 + 20, label.maxWidth(-1), 0);
    }
    
    // TODO should this include the gap??
    @Test public void whenTextIsNullAndGraphicIsSetWithTEXT_ONLYContentDisplay_computeMaxWidth_ReturnsPadding() {
        label.setText(null);
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.maxWidth(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithTEXT_ONLYContentDisplay_computeMaxWidth_ReturnsPadding() {
        label.setText("");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertEquals(14, label.maxWidth(-1), 0);
    }
    
    // TODO should this include the gap? I guess not, otherwise our gap would have to default to 0
    @Test public void whenTextIsSetAndGraphicIsSetWithTEXT_ONLYContentDisplay_computeMaxWidth_ReturnsTextWidthPlusPadding() {
        label.setText("Yippee Skippee");
        label.setGraphic(new Rectangle(0, 0, 20, 20));
        label.setGraphicTextGap(6.5);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double textWidth = Utils.computeTextWidth(label.getFont(), "Yippee Skippee", 0);
        assertEquals(14 + textWidth, label.maxWidth(-1), 0);
    }
    
    /****************************************************************************
     *                                                                          *
     * Tests for maxHeight.                                                     *
     *                                                                          *
     ***************************************************************************/

    @Test public void whenTextIsNullAndNoGraphic_computeMaxHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndNoGraphic_computeMaxHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndNoGraphic_computeMaxHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText("Howdy Pardner");
        skin.setPadding(new Insets(7, 7, 7, 7));
        assertTrue(label.maxHeight(-1) >= 0);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndUnmanagedGraphic_computeMaxHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndUnmanagedGraphic_computeMaxHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndUnmanagedGraphic_computeMaxHeight_ReturnsSingleLineStringHeightPlusPadding() {
        label.setText("Howdy Pardner");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 500);
        r.setManaged(false);
        label.setGraphic(r);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertTrue(label.maxHeight(-1) >= 0);
        assertEquals(14 + lineHeight, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithTOPContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TOP);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithTOPContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TOP);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithTOPContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setGraphicTextGap(2);
        label.setContentDisplay(ContentDisplay.TOP);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithRIGHTContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.RIGHT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithRIGHTContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.RIGHT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithRIGHTContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.RIGHT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithBOTTOMContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithBOTTOMContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphicTextGap(2);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithBOTTOMContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setGraphicTextGap(2);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + 23 + lineHeight + 2, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithLEFTContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.LEFT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithLEFTContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.LEFT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithLEFTContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.LEFT);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithCENTERContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.CENTER);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithCENTERContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.CENTER);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithCENTERContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.CENTER);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + Math.max(23, lineHeight), label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        assertEquals(14 + 23, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        assertEquals(14 + 23, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithGRAPHIC_ONLYContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        assertEquals(14 + 23, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsNullAndGraphicIsSetWithTEXT_ONLYContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText(null);
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsEmptyAndGraphicIsSetWithTEXT_ONLYContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.maxHeight(-1), 0);
    }
    
    @Test public void whenTextIsSetAndGraphicIsSetWithTEXT_ONLYContentDisplay_computeMaxHeight_ReturnsRightAnswer() {
        label.setText("For crying in the mud");
        skin.setPadding(new Insets(7, 7, 7, 7));
        Rectangle r = new Rectangle(23, 23);
        label.setGraphic(r);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        final double lineHeight = Utils.computeTextHeight(label.getFont(), " ", 0);
        assertEquals(14 + lineHeight, label.maxHeight(-1), 0);
    }

    @Test public void maxWidthTracksPreferred() {
        label.setPrefWidth(500);
        assertEquals(500, label.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        label.setPrefHeight(500);
        assertEquals(500, label.maxHeight(-1), 0);
    }
    
    /****************************************************************************
     *                                                                          *
     * Tests for updateDisplayedText                                            *
     *                                                                          *
     ***************************************************************************/

    // tests for updateDisplayedText (not even sure how to test it exactly yet)
    
    
    public static final class LabelSkinMock extends LabelSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public LabelSkinMock(Label label) {
            super(label);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}
