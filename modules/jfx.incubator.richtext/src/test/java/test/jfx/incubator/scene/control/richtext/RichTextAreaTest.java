/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.jfx.incubator.scene.control.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodHighlight;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.sun.javafx.tk.Toolkit;
import com.sun.jfx.incubator.scene.control.richtext.CaretInfo;
import com.sun.jfx.incubator.scene.control.richtext.EmbeddedImageHelper;
import com.sun.jfx.incubator.scene.control.richtext.SegmentStyledInput;
import com.sun.jfx.incubator.scene.control.richtext.TextCell;
import com.sun.jfx.incubator.scene.control.richtext.VFlow;
import jfx.incubator.scene.control.richtext.LineEnding;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.RichTextAreaShim;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.StyleHandlerRegistry;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.ContentChange;
import jfx.incubator.scene.control.richtext.model.EmbeddedImage;
import jfx.incubator.scene.control.richtext.model.RichTextFormatHandler;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledSegment;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import jfx.incubator.scene.control.richtext.model.TabStops;
import jfx.incubator.scene.control.richtext.skin.RichTextAreaSkin;
import test.jfx.incubator.scene.control.richtext.model.TestRichTextModel;
import test.jfx.incubator.scene.control.richtext.support.RTUtil;
import test.jfx.incubator.scene.control.richtext.support.TestStyledInput;
import test.jfx.incubator.scene.util.StageLoader;
import test.jfx.incubator.scene.util.TUtil;

/**
 * Tests the RichTextArea control.
 */
public class RichTextAreaTest {
    private RichTextArea control;
    private static final StyleAttributeMap BOLD = StyleAttributeMap.builder().setBold(true).build();
    private static final StyleAttributeMap ITALIC = StyleAttributeMap.builder().setItalic(true).build();
    private static final StyleAttributeMap UNDER = StyleAttributeMap.builder().setUnderline(true).build();
    private static final String NL = System.getProperty("line.separator");
    private static final double EPSILON = 0.00001;
    private Stage stage;

    @BeforeEach
    public void beforeEach() {
        TUtil.setUncaughtExceptionHandler();

        control = new RichTextArea();
        control.setSkin(new RichTextAreaSkin(control));
    }

    @AfterEach
    public void afterEach() {
        if (stage != null) {
            stage.hide();
            stage = null;
        }
        TUtil.removeUncaughtExceptionHandler();
    }

    private String text() {
        return RTUtil.getText(control);
    }

    // constructors

    @Test
    public void defaultModelIsRichTextModel() {
        assertTrue(control.getModel() instanceof RichTextModel);
    }

    @Test
    public void nullModelInConstructor() {
        control = new RichTextArea(null);
        assertTrue(control.getModel() == null);

        CustomStyledTextModel m = new CustomStyledTextModel();
        control = new RichTextArea(null);
        control.setModel(m);
        assertSame(m, control.getModel());
    }

    // properties

    @Test
    public void propertiesSettersAndGetters() {
        TUtil.testProperty(control.caretBlinkPeriodProperty(), control::getCaretBlinkPeriod, control::setCaretBlinkPeriod, Duration.millis(10));
        TUtil.testProperty(control.contentPaddingProperty(), control::getContentPadding, control::setContentPadding, null, new Insets(40));
        TUtil.testBooleanProperty(control.displayCaretProperty(), control::isDisplayCaret, control::setDisplayCaret);
        TUtil.testBooleanProperty(control.editableProperty(), control::isEditable, control::setEditable);
        TUtil.testBooleanProperty(control.highlightCurrentParagraphProperty(), control::isHighlightCurrentParagraph, control::setHighlightCurrentParagraph);
        TUtil.testProperty(control.leftDecoratorProperty(), control::getLeftDecorator, control::setLeftDecorator, null, new CustomSideDecorator());
        TUtil.testProperty(control.modelProperty(), control::getModel, control::setModel, null, new RichTextModel(), new CodeTextModel());
        TUtil.testProperty(control.rightDecoratorProperty(), control::getRightDecorator, control::setRightDecorator, null, new CustomSideDecorator());
        TUtil.testBooleanProperty(control.useContentHeightProperty(), control::isUseContentHeight, control::setUseContentHeight);
        TUtil.testBooleanProperty(control.useContentWidthProperty(), control::isUseContentWidth, control::setUseContentWidth);
        TUtil.testBooleanProperty(control.wrapTextProperty(), control::isWrapText, control::setWrapText);
    }

    @Test
    public void nonNullableProperties() {
        TUtil.testNonNullable(control.caretBlinkPeriodProperty(), control::setCaretBlinkPeriod);
    }

    // default values

    @Test
    public void defaultPropertyValues() {
        TUtil.testDefaultValue(control.caretBlinkPeriodProperty(), control::getCaretBlinkPeriod, Duration.millis(1000));
        TUtil.testDefaultValue(control.contentPaddingProperty(), control::getContentPadding, null);
        TUtil.testDefaultValue(control.displayCaretProperty(), control::isDisplayCaret, true);
        TUtil.testDefaultValue(control.editableProperty(), control::isEditable, true);
        TUtil.testDefaultValue(control.highlightCurrentParagraphProperty(), control::isHighlightCurrentParagraph, false);
        TUtil.testDefaultValue(control.leftDecoratorProperty(), control::getLeftDecorator, null);
        TUtil.checkDefaultValue(control.modelProperty(), control::getModel, (v) -> (v instanceof RichTextModel));
        TUtil.testDefaultValue(control.rightDecoratorProperty(), control::getRightDecorator, null);
        TUtil.testDefaultValue(control.useContentHeightProperty(), control::isUseContentHeight, false);
        TUtil.testDefaultValue(control.useContentWidthProperty(), control::isUseContentWidth, false);
        TUtil.testDefaultValue(control.wrapTextProperty(), control::isWrapText, false);
    }

    // css

    @Test
    public void testCaretBlinkPeriodCSS() {
        Scene s = new Scene(control);
        control.setStyle("-fx-caret-blink-period: 1ms");
        control.applyCss();
        assertEquals(Duration.millis(1), control.getCaretBlinkPeriod());

        control.setStyle("-fx-caret-blink-period: 99ms");
        control.applyCss();
        assertEquals(Duration.millis(99), control.getCaretBlinkPeriod());
    }

    @Test
    public void testContentPaddingCSS() {
        Scene s = new Scene(control);
        control.setStyle("-fx-content-padding: 99");
        control.applyCss();
        assertEquals(new Insets(99), control.getContentPadding());

        control.setStyle("-fx-content-padding: null");
        control.applyCss();
        assertEquals(null, control.getContentPadding());
    }

    @Test
    public void testDisplayCaretCSS() {
        Scene s = new Scene(control);
        control.setStyle("-fx-display-caret: false");
        control.applyCss();
        assertFalse(control.isDisplayCaret());

        control.setStyle("-fx-display-caret: true");
        control.applyCss();
        assertTrue(control.isDisplayCaret());
    }

    @Test
    public void testHighlightCurrentParagraphCSS() {
        Scene s = new Scene(control);
        control.setStyle("-fx-highlight-current-paragraph: false");
        control.applyCss();
        assertFalse(control.isHighlightCurrentParagraph());

        control.setStyle("-fx-highlight-current-paragraph: true");
        control.applyCss();
        assertTrue(control.isHighlightCurrentParagraph());
    }

    @Test
    public void testUseContentHeightCSS() {
        Scene s = new Scene(control);
        control.setStyle("-fx-use-content-height: false");
        control.applyCss();
        assertFalse(control.isUseContentHeight());

        control.setStyle("-fx-use-content-height: true");
        control.applyCss();
        assertTrue(control.isUseContentHeight());
    }

    @Test
    public void testUseContentWidthCSS() {
        Scene s = new Scene(control);
        control.setStyle("-fx-use-content-width: false");
        control.applyCss();
        assertFalse(control.isUseContentWidth());

        control.setStyle("-fx-use-content-width: true");
        control.applyCss();
        assertTrue(control.isUseContentWidth());
    }

    @Test
    public void testWrapTextCSS() {
        Scene s = new Scene(control);
        control.setStyle("-fx-wrap-text: false");
        control.applyCss();
        assertFalse(control.isWrapText());

        control.setStyle("-fx-wrap-text: true");
        control.applyCss();
        assertTrue(control.isWrapText());
    }

    // property binding

    @Test
    public void testPropertyBinding() {
        // caret blink period property is internally constrained and cannot be bound
        //TUtil.testBinding(control.caretBlinkPeriodProperty(), control::getCaretBlinkPeriod, Duration.millis(10));
        TUtil.testBinding(control.contentPaddingProperty(), control::getContentPadding, null, new Insets(40));
        TUtil.testBinding(control.displayCaretProperty(), control::isDisplayCaret);
        TUtil.testBinding(control.editableProperty(), control::isEditable);
        TUtil.testBinding(control.highlightCurrentParagraphProperty(), control::isHighlightCurrentParagraph);
        TUtil.testBinding(control.leftDecoratorProperty(), control::getLeftDecorator, null, new CustomSideDecorator());
        TUtil.testBinding(control.modelProperty(), control::getModel, null, new RichTextModel(), new CodeTextModel());
        TUtil.testBinding(control.rightDecoratorProperty(), control::getRightDecorator, null, new CustomSideDecorator());
        TUtil.testBinding(control.useContentHeightProperty(), control::isUseContentHeight);
        TUtil.testBinding(control.useContentWidthProperty(), control::isUseContentWidth);
        TUtil.testBinding(control.wrapTextProperty(), control::isWrapText);
    }

    // functional API tests

    @Test
    public void appendText() {
        TextPos p = control.appendText("a");
        assertEquals(TextPos.ofLeading(0, 1), p);
        assertEquals("a", text());
        // undo
        control.undo();
        assertEquals("", text());
    }

    @Test
    public void appendTextWithStyles() {
        TextPos p = control.appendText("a", BOLD);
        assertEquals(TextPos.ofLeading(0, 1), p);
        control.select(p);
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        assertEquals("a", text());
        // undo
        control.undo();
        assertEquals("", text());
    }

    @Test
    public void appendTextFromStyledInput() {
        TestStyledInput in = TestStyledInput.plainText("a\nb");
        TextPos p = control.appendText(in);
        assertEquals(TextPos.ofLeading(1, 1), p);
        assertEquals("a" + NL + "b", text());
        // undo
        control.undo();
        assertEquals("", text());
    }

    @Test
    public void applyStyle() {
        TestStyledInput in = TestStyledInput.plainText("a\nbbb");
        TextPos p = control.appendText(in);
        control.applyStyle(TextPos.ZERO, TextPos.ofLeading(1, 3), BOLD);
        assertEquals(TextPos.ofLeading(1, 3), p);
        control.select(TextPos.ofLeading(1, 0));
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        // undo
        control.undo();
        assertEquals(StyleAttributeMap.EMPTY, control.getActiveStyleAttributeMap());
    }

    @Test
    public void applyStyleBeyondDocumentEnd() {
        control.applyStyle(TextPos.ZERO, TextPos.ofLeading(100, 3), BOLD);
    }

    @Test
    public void clear() {
        control.appendText("a");
        control.clear();
        TextPos p = control.getDocumentEnd();
        assertEquals(TextPos.ZERO, p);
    }

    @Test
    public void clearSelection() {
        control.appendText("a");
        control.selectAll();
        control.clearSelection();
        assertEquals(null, control.getSelection());
    }

    @Test
    public void clearUndoRedo() {
        control.appendText("a");
        control.clearUndoRedo();
    }

    @Test
    public void copy() {
        RTUtil.copyToClipboard("yo");
        control.appendText("123");
        control.selectAll();
        control.copy();
        assertEquals("123", Clipboard.getSystemClipboard().getString());

        control.select(TextPos.ZERO, TextPos.ofLeading(0, 1));
        control.copy();
        assertEquals("1", Clipboard.getSystemClipboard().getString());

        control.select(TextPos.ofLeading(0, 1), TextPos.ofLeading(0, 2));
        control.copy();
        assertEquals("2", Clipboard.getSystemClipboard().getString());

        control.select(TextPos.ofLeading(0, 2), TextPos.ofLeading(0, 3));
        control.copy();
        assertEquals("3", Clipboard.getSystemClipboard().getString());

        control.appendText("\n4");
        control.select(new TextPos(0, 3, 2, false), control.getDocumentEnd());
        control.copy();
        assertEquals(NL + "4", Clipboard.getSystemClipboard().getString());
    }

    @Test
    public void lineEndingCopy() {
        control.appendText("1\n2\n3");
        assertEquals(3, control.getParagraphCount());
        t(LineEnding.CR, "1\r2\r3");
        t(LineEnding.CRLF, "1\r\n2\r\n3");
        t(LineEnding.LF, "1\n2\n3");
    }

    @Test
    public void lineEndingNull() {
        assertThrows(NullPointerException.class, () -> {
            control.getModel().setLineEnding(null);
        });
    }

    @Test
    public void lineEndingNullModel() {
        control.setModel(null);
        assertEquals(LineEnding.system(), control.getLineEnding());
        control.setLineEnding(LineEnding.CR);
        assertEquals(LineEnding.system(), control.getLineEnding());
        assertThrows(NullPointerException.class, () -> {
            control.setLineEnding(null);
        });
    }

    private void t(LineEnding lineEnding, String expected) {
        StyledTextModel m = control.getModel();
        m.setLineEnding(lineEnding);
        assertEquals(lineEnding, m.getLineEnding());
        assertEquals(expected, text());
        control.select(TextPos.ZERO, control.getDocumentEnd());
        control.copy();
        assertEquals(expected, Clipboard.getSystemClipboard().getString());
    }

    @Test
    public void copyDataFormat() {
        RTUtil.copyToClipboard("");
        control.appendText("a");
        control.selectAll();
        DataFormat fmt = RichTextFormatHandler.DATA_FORMAT;
        control.copy(fmt);
        String s = Clipboard.getSystemClipboard().getString();
        assertEquals(null, s);
        Object v = Clipboard.getSystemClipboard().getContent(fmt);
        assertEquals(TestRichTextModel.header() + "{}a{!}", v);
    }

    @Test
    public void cut() {
        control.appendText("123");
        control.selectAll();
        control.cut();
        assertEquals("", text());
    }

    @Test
    public void deselect() {
        control.appendText("123");
        control.selectAll();
        control.deselect();
        SelectionSegment sel = control.getSelection();
        TextPos p = control.getCaretPosition();
        assertNotNull(p);
        assertEquals(p, sel.getMin());
        assertEquals(p, sel.getMax());
    }

    @Test
    public void execute() {
        control.appendText("a");
        control.execute(RichTextArea.Tag.SELECT_ALL);

        SelectionSegment sel = control.getSelection();
        TextPos end = control.getDocumentEnd();
        assertNotNull(end);
        assertEquals(TextPos.ZERO, sel.getMin());
        assertEquals(end, sel.getMax());
    }

    @Test
    public void executeDefault() {
        // map SELECT_ALL to no-op
        control.getInputMap().registerFunction(RichTextArea.Tag.SELECT_ALL, () -> { });

        control.appendText("123");
        control.selectAll();
        // remapped function is a no-op
        assertEquals(null, control.getSelection());
        // default function is still there
        control.executeDefault(RichTextArea.Tag.SELECT_ALL);
        assertTrue(control.getSelection() != null);
    }

    @Test
    public void getActiveStyleAttributeMap() {
        control.appendText("1234", BOLD);
        control.appendText("5678", StyleAttributeMap.EMPTY);

        control.select(TextPos.ofLeading(0, 2));
        StyleAttributeMap a = control.getActiveStyleAttributeMap();
        assertTrue(a.contains(StyleAttributeMap.BOLD));

        control.select(TextPos.ofLeading(0, 6));
        a = control.getActiveStyleAttributeMap();
        assertFalse(a.contains(StyleAttributeMap.BOLD));
    }

    @Test
    public void getControlCssMetaData() {
        List<CssMetaData<? extends Styleable, ?>> md = control.getControlCssMetaData();
        // RichTextArea:1019
        int styleablesCount = 7;
        assertEquals(md.size(), Control.getClassCssMetaData().size() + styleablesCount);
    }

    @Test
    public void getParagraphCount() {
        assertEquals(1, control.getParagraphCount());

        control.appendText("1\n2\n3");
        assertEquals(3, control.getParagraphCount());

        control.setModel(null);
        assertEquals(0, control.getParagraphCount());
    }

    @Test
    public void getParagraphEnd() {
        control.appendText("1\n22\n333");
        assertEquals(new TextPos(0, 1, 0, false), control.getParagraphEnd(0));
        assertEquals(new TextPos(1, 2, 1, false), control.getParagraphEnd(1));
        assertEquals(new TextPos(2, 3, 2, false), control.getParagraphEnd(2));

        control.setModel(null);
        assertEquals(TextPos.ZERO, control.getParagraphEnd(0));
    }

    @Test
    public void getPlainText() {
        control.appendText("1\n22\n333");
        assertEquals("1", control.getPlainText(0));
        assertEquals("22", control.getPlainText(1));
        assertEquals("333", control.getPlainText(2));
    }

    @Test
    public void getSelection() {
        control.appendText("123");
        control.selectAll();
        SelectionSegment sel = control.getSelection();
        assertNotNull(sel);
    }

    @Test
    public void getStyleHandlerRegistry() {
        StyleHandlerRegistry r = control.getStyleHandlerRegistry();
        assertNotNull(r);
    }

    @Test
    public void hasNonEmptySelection() {
        control.appendText("123");
        assertFalse(control.hasNonEmptySelection());
        control.selectAll();
        assertTrue(control.hasNonEmptySelection());
    }

    @Test
    public void insertBetweenSegments() {
        TextPos p = control.appendText("a", BOLD);
        control.appendText("b", ITALIC);
        control.appendText("c", BOLD);
        control.select(p);
        control.insertTab();
        control.insertTab();
                    //   BB.B.IB
        assertEquals("a\t\tbc", text());
        control.select(TextPos.ofLeading(0, 2));
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        control.select(TextPos.ZERO);
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        control.select(TextPos.ofLeading(0, 1));
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        control.select(TextPos.ofLeading(0, 2));
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        control.select(TextPos.ofLeading(0, 3));
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        control.select(TextPos.ofLeading(0, 4));
        assertEquals(ITALIC, control.getActiveStyleAttributeMap());
        control.select(TextPos.ofLeading(0, 5));
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
    }

    @Test
    public void insertLineBreak() {
        control.appendText("123");
        control.select(TextPos.ofLeading(0, 1));
        control.insertLineBreak();
    }

    @Test
    public void insertStyles() {
        control.select(TextPos.ZERO);
        control.setInsertStyles(BOLD);
        type("bold");
        control.setInsertStyles(ITALIC);
        type("italic");

        control.select(TextPos.ofLeading(0, 2));
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        control.select(TextPos.ofLeading(0, 6));
        assertEquals(ITALIC, control.getActiveStyleAttributeMap());

        // verify that the model styles are used when insertStyles=null
        control.setInsertStyles(null);
        control.select(TextPos.ofLeading(0, 2));
        type("**");
        control.select(TextPos.ofLeading(0, 3));
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
    }

    @Test
    public void insertTextWithStyles() {
        TextPos p = control.appendText("a", BOLD);
        assertEquals(TextPos.ofLeading(0, 1), p);
        p = control.insertText(TextPos.ZERO, "b", ITALIC);
        assertEquals(TextPos.ofLeading(0, 1), p);
        control.select(p);
        assertEquals(ITALIC, control.getActiveStyleAttributeMap());
        assertEquals("ba", text());
        // undo
        control.undo();
        assertEquals("a", text());
    }

    @Test
    public void insertTextFromStyledInput() {
        TestStyledInput in = TestStyledInput.plainText("a\nb");
        TextPos p = control.appendText(in);
        assertEquals(TextPos.ofLeading(1, 1), p);
        assertEquals("a" + NL + "b", text());
        // undo
        control.undo();
        assertEquals("", text());
    }

    @Test
    public void isRedoable() {
        assertFalse(control.isRedoable());
        control.appendText("123");
        control.undo();
        assertTrue(control.isRedoable());
    }

    @Test
    public void isUndoable() {
        assertFalse(control.isUndoable());
        control.appendText("123");
        assertTrue(control.isUndoable());
    }

    @Test
    public void modelChangeClearsSelection() {
        control.insertText(TextPos.ZERO, "1234", null);
        control.selectAll();
        SelectionSegment sel = control.getSelection();
        assertFalse(sel.isCollapsed());
        control.setModel(new RichTextModel());
        sel = control.getSelection();
        assertEquals(null, sel);
        assertEquals("", text());
    }

    @Test
    public void paste() {
        RTUtil.copyToClipboard("-");
        control.appendText("123");
        control.select(TextPos.ofLeading(0, 1));
        control.paste();
        assertEquals("1-23", text());
    }

    @Test
    public void pasteDataFormat() {
        DataFormat fmt = RichTextFormatHandler.DATA_FORMAT;
        ClipboardContent cc = new ClipboardContent();
        cc.putString("plain");
        cc.put(fmt, "a{!}");
        Clipboard.getSystemClipboard().setContent(cc);

        control.appendText("123");
        control.select(TextPos.ofLeading(0, 1));
        control.paste(fmt);
        assertEquals("1a23", text());
    }

    @Test
    public void pastePlainText() {
        DataFormat fmt = RichTextFormatHandler.DATA_FORMAT;
        ClipboardContent cc = new ClipboardContent();
        cc.putString("plain");
        cc.put(fmt, "a{!}");
        Clipboard.getSystemClipboard().setContent(cc);

        control.appendText("123");
        control.select(TextPos.ofLeading(0, 1));
        control.pastePlainText();
        assertEquals("1plain23", text());
    }

    @Test
    public void read() throws Exception {
        control.appendText("1 bold");
        control.applyStyle(TextPos.ofLeading(0, 2), TextPos.ofLeading(0, 6), BOLD);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        control.write(out);
        byte[] b = out.toByteArray();
        String text1 = text();

        control = new RichTextArea();
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        control.read(in);
        String text2 = text();
        assertEquals(text1, text2);
    }

    @Test
    public void readDataFormat() throws Exception {
        DataFormat fmt = DataFormat.PLAIN_TEXT;
        control.appendText("1 bold");
        control.applyStyle(TextPos.ofLeading(0, 2), TextPos.ofLeading(0, 6), BOLD);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        control.write(fmt, out);
        byte[] b = out.toByteArray();
        String text1 = text();

        control = new RichTextArea();
        control.appendText("should not see me");
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        control.read(fmt, in);
        String text2 = text();
        assertEquals(text1, text2);
        // read clears undo buffer
        control.undo();
        assertEquals(text1, text2);
    }

    @Test
    public void redo() {
        control.appendText("123");
        control.undo();
        assertEquals("", text());
        control.redo();
        assertEquals("123", text());
        // test undo/redo stack
        control.appendText("4");
        control.appendText("5");
        assertEquals("12345", text());
        control.undo();
        control.undo();
        control.undo();
        assertEquals("", text());
        control.redo();
        control.redo();
        control.redo();
        assertEquals("12345", text());
    }

    @Test
    public void replaceText() {
        control.appendText("1234");
        control.replaceText(TextPos.ofLeading(0, 1), TextPos.ofLeading(0, 3), "-");
        assertEquals("1-4", text());
    }

    @Test
    public void replaceTextBeyondDocumentEnd() {
        control.appendText("1\n");
        control.replaceText(TextPos.ofLeading(0, 1), TextPos.ofLeading(33, 3), "-");
        assertEquals("1-", text());
    }

    @Test
    public void replaceTextFromStyledInput() {
        TestStyledInput in = TestStyledInput.plainText("-");
        control.appendText("1234");
        control.replaceText(TextPos.ofLeading(0, 1), TextPos.ofLeading(0, 3), in);
        assertEquals("1-4", text());
    }

    @Test
    public void setStyle() {
        TestStyledInput in = TestStyledInput.plainText("a\nbbb");
        TextPos p = control.appendText(in);
        control.setStyle(TextPos.ZERO, TextPos.ofLeading(1, 3), BOLD);
        assertEquals(TextPos.ofLeading(1, 3), p);
        control.select(TextPos.ofLeading(1, 0));
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        // allow undo
        control.undo();
        assertEquals(StyleAttributeMap.EMPTY, control.getActiveStyleAttributeMap());
    }

    @Test
    public void select() {
        TextPos p = TextPos.ofLeading(0, 1);
        control.appendText("1234");
        control.select(p);
        SelectionSegment sel = control.getSelection();
        assertEquals(sel.getMin(), p);
        assertEquals(sel.getMax(), p);
    }

    @Test
    public void selectRange() {
        TextPos p1 = TextPos.ofLeading(0, 1);
        TextPos p2 = TextPos.ofLeading(0, 2);
        control.appendText("1234");
        control.select(p1, p2);
        SelectionSegment sel = control.getSelection();
        assertEquals(sel.getMin(), p1);
        assertEquals(sel.getMax(), p2);
    }

    @Test
    public void selectAll() {
        control.appendText("a");
        control.selectAll();

        SelectionSegment sel = control.getSelection();
        TextPos end = control.getDocumentEnd();
        assertNotNull(end);
        assertEquals(TextPos.ZERO, sel.getMin());
        assertEquals(end, sel.getMax());
    }

    @Test
    public void selectParagraph() {
        control.appendText("123\n456\n789");
        // first line
        control.select(TextPos.ZERO);
        control.selectParagraph();
        SelectionSegment sel = control.getSelection();
        assertEquals(TextPos.ZERO, sel.getMin());
        assertEquals(TextPos.ofLeading(1, 0), sel.getMax());
        // last line, no trailing line separator
        control.select(TextPos.ofLeading(2, 0));
        control.selectParagraph();
        sel = control.getSelection();
        assertEquals(TextPos.ofLeading(2, 0), sel.getMin());
        assertEquals(new TextPos(2, 3, 2, false), sel.getMax());
    }

    @Test
    public void undo() {
        control.appendText("1");
        control.undo();
        assertEquals("", text());
        // test undo/redo stack
        control.appendText("2");
        control.appendText("3");
        control.appendText("4");
        assertEquals("234", text());
        control.undo();
        control.undo();
        control.undo();
        assertEquals("", text());
    }

    @Test
    public void undoStyleChange() {
        ArrayList<ContentChange> changes = new ArrayList<>();
        String text = "BOLD";
        TextPos p = control.appendText(text);
        control.getModel().addListener((ch) -> {
            changes.add(ch);
        });
        TextPos p2 = TextPos.ofLeading(0, 2);
        assertEquals(text, text());
        control.applyStyle(TextPos.ZERO, p, BOLD);
        control.select(p2);
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        control.undo();
        assertEquals(text, text());
        assertEquals(StyleAttributeMap.EMPTY, control.getActiveStyleAttributeMap());
        assertEquals(2, changes.size());
        control.redo();
        assertEquals(text, text());
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        // changes
        assertEquals(3, changes.size());
        ContentChange ch0 = changes.get(0);
        assertEquals(TextPos.ZERO, ch0.getStart());
        assertEquals(p, ch0.getEnd());
        assertFalse(ch0.isEdit());
        //
        ContentChange ch1 = changes.get(1);
        assertFalse(ch1.isEdit());
        assertEquals(TextPos.ZERO, ch1.getStart());
        assertEquals(p, ch1.getEnd());
        //
        ContentChange ch2 = changes.get(2);
        assertFalse(ch2.isEdit());
        assertEquals(TextPos.ZERO, ch2.getStart());
        assertEquals(p, ch2.getEnd());
    }

    @Test
    public void write() throws Exception {
        control.appendText("1 bold");
        control.applyStyle(TextPos.ofLeading(0, 2), TextPos.ofLeading(0, 6), BOLD);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        control.write(out);
        byte[] b = out.toByteArray();
        assertEquals(TestRichTextModel.header() + "{}1 {b}bold{!}", new String(b, StandardCharsets.US_ASCII));
    }

    @Test
    public void writeDataFormat() throws Exception {
        DataFormat fmt = DataFormat.PLAIN_TEXT;
        control.appendText("1 bold");
        control.applyStyle(TextPos.ofLeading(0, 2), TextPos.ofLeading(0, 6), BOLD);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        control.write(fmt, out);
        byte[] b = out.toByteArray();
        assertEquals("1 bold", new String(b, StandardCharsets.US_ASCII));
    }

    @Test
    public void undoRedoEnabled() {
        // api
        assertTrue(control.isUndoRedoEnabled());
        control.setUndoRedoEnabled(false);
        assertFalse(control.isUndoRedoEnabled());
        control.setModel(null);
        control.setUndoRedoEnabled(true);
        assertFalse(control.isUndoRedoEnabled());
        control.setModel(new RichTextModel());
        assertTrue(control.isUndoRedoEnabled());
        // undo-redo enabled
        control.appendText("1");
        assertEquals("1", text());
        control.undo();
        assertEquals("", text());
        // undo-redo disabled
        control.setUndoRedoEnabled(false);
        control.appendText("2");
        assertEquals("2", text());
        control.undo();
        assertEquals("2", text());
        // disabling undo-redo clears undo stack
        control.setUndoRedoEnabled(true);
        control.appendText("3");
        assertEquals("23", text());
        assertTrue(control.isUndoable());
        control.setUndoRedoEnabled(false);
        assertFalse(control.isUndoable());
        control.setUndoRedoEnabled(true);
        control.appendText("4");
        assertEquals("234", text());
        control.undo();
        assertEquals("23", text());
        control.undo();
        assertEquals("23", text());
    }

    private void fireIME(int caret, String committed, Object... runs) {
        ArrayList<InputMethodTextRun> composed = new ArrayList<>();
        for (int i = 0; i < runs.length;) {
            String s = (String)runs[i++];
            InputMethodHighlight h = (InputMethodHighlight)runs[i++];
            composed.add(new InputMethodTextRun(s, h));
        }
        InputMethodEvent ev = new InputMethodEvent(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, composed, committed, caret);
        Event.fireEvent(control, ev);
    }

    private static TextPos tp(int caret) {
        return TextPos.ofLeading(0, caret);
    }

    @Test
    public void testIME() {
        TextPos p = new TextPos(0, 0, 0, false);
        control.select(p);
        // compose "a" "b"
        fireIME(0, "", "a", InputMethodHighlight.UNSELECTED_RAW, "b", InputMethodHighlight.UNSELECTED_RAW);
        assertEquals(tp(2), control.getCaretPosition());
        assertEquals("ab", text());
        // escape, cancel composition
        fireIME(0, "");
        assertEquals(TextPos.ofLeading(0, 0), control.getCaretPosition());
        assertEquals("", text());
        // compose, "c" "d"
        fireIME(0, "", "c", InputMethodHighlight.UNSELECTED_RAW, "d", InputMethodHighlight.UNSELECTED_RAW);
        assertEquals(tp(2), control.getCaretPosition());
        assertEquals("cd", text());
        // commit "yoyo"
        fireIME(0, "yoyo");
        assertEquals(tp(4), control.getCaretPosition());
        assertEquals("yoyo", text());
    }

    @Test
    public void withEmbeddedNodes() {
        class ARegion extends Region {
            public ARegion(SimpleDoubleProperty height) {
                setPrefWidth(10);
                minHeightProperty().bind(height);
                maxHeightProperty().bind(height);
            }
        }

        SimpleDoubleProperty height = new SimpleDoubleProperty(10.0);
        SimpleViewOnlyStyledModel m = new SimpleViewOnlyStyledModel();
        m.addSegment("Trailing node: ");
        m.addNodeSegment(() -> new ARegion(height));
        m.addParagraph(() -> new ARegion(height));
        control.setModel(m);
        control.setWrapText(false);
        control.setUseContentHeight(true);
        control.layout();

        double h1 = control.prefHeight(-1);
        control.getHeight();

        height.set(100.0);
        control.layout();

        double h2 = control.prefHeight(-1);
        control.getHeight();
        assertTrue(h1 != h2, "heights should differ: h1=" + h1 + " h2=" + h2);
    }

    private void type(String text) {
        for (char c : text.toCharArray()) {
            String ch = String.valueOf(c);
            KeyEvent ev = new KeyEvent(
                this,
                control,
                KeyEvent.KEY_TYPED,
                ch,
                "",
                KeyCode.UNDEFINED,
                false, // shiftDown
                false, // controlDown
                false, // altDown
                false // metaDown
            );
            Event.fireEvent(control, ev);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1",
        "1\n",
        "1\n2",
        "1\n2\n"
    })
    public void setTextNewlines(String text) {
        control.appendText(text);
        control.setLineEnding(LineEnding.LF);

        String s = RichTestUtil.getText(control, TextPos.ZERO, control.getDocumentEnd());
        assertEquals(text, s);
    }

    @ParameterizedTest
    @CsvSource(textBlock =
        """
        0, 0, 0, 1, '1'
        0, 0, 0, 2, '11'
        0, 0, 0, 3, '11'
        0, 0, 1, 0, '11\n'
        0, 0, 1, 1, '11\n2'
        0, 0, 1, 3, '11\n22'
        0, 0, 2, 0, '11\n22'
        0, 0, 2, 222, '11\n22'
        0, 1, 9, 9, '1\n22'
        0, 2, 9, 9, '\n22'
        0, 3, 9, 9, '\n22'
        1, 0, 9, 9, '22'
        1, 1, 9, 9, '2'
        1, 2, 9, 9, ''
        1, 9, 9, 9, ''
        """
    )
    public void getTextRanges(int startIndex, int startOffset, int endIndex, int endOffset, String expected) {
        control.appendText("11\n22");
        control.setLineEnding(LineEnding.LF);
        TextPos start = TextPos.ofLeading(startIndex, startOffset);
        TextPos end = TextPos.ofLeading(endIndex, endOffset);
        String s = RichTestUtil.getText(control, start, end);
        assertEquals(expected, s);
    }

    @Test
    public void appendTextSelectAll() {
        String text = "1\n2\n";
        control.appendText(text);
        control.setLineEnding(LineEnding.LF);
        assertEquals(3, control.getParagraphCount());

        control.selectAll();
        SelectionSegment sel = control.getSelection();
        assertEquals(TextPos.ofLeading(2, 0), sel.getMax());

        String s = RichTestUtil.getText(control, sel);
        assertEquals(text, s);
    }

    private double pos(int line, int off) {
        TextPos p = TextPos.ofLeading(line, off);
        VFlow f = RichTextAreaShim.vflow(control);
        if (f != null) {
            CaretInfo c = f.getCaretInfo(p);
            if (c != null) {
                return c.getMinX();
            }
        }
        return Double.NaN;
    }

    private void assertX(int line, int off, double expected) {
        assertEquals(expected, pos(line, off), EPSILON);
    }

    @Test
    public void tabStops() {
        Scene scene = new Scene(control, 1000, 1000);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();

        RichTextModel m = new RichTextModel();
        control.setModel(m);
        control.setContentPadding(new Insets(0));
        control.appendText("\t1\t2\t3\n\t1\t2\t3\n 1 2 3\n");
        control.layout();

        Toolkit.getToolkit().firePulse();
        Toolkit.getToolkit().firePulse();
        assertEquals(1000.0, control.getWidth(), EPSILON);

        // default tab stops = 0 (legacy behavior, tab == 8 spaces)
        // keep in mind there is +2.5 pixels added for borders and padding
        m.setDefaultTabStops(RichTextModel.DEFAULT_TAB_STOPS_FIXED);
        assertX(0, 1, 96.5);
        assertX(0, 3, 192.5);
        assertX(0, 5, 288.5);

        assertX(1, 1, 96.5);
        assertX(1, 3, 192.5);
        assertX(1, 5, 288.5);

        assertX(2, 1, 12.5);
        assertX(2, 3, 36.5);
        assertX(2, 5, 60.5);

        // default tab stops = -1, tab == 1 space
        m.setDefaultTabStops(RichTextModel.DEFAULT_TAB_STOPS_DISABLED);
        assertX(0, 1, 12.5);
        assertX(0, 3, 36.5);
        assertX(0, 5, 60.5);

        assertX(1, 1, 12.5);
        assertX(1, 3, 36.5);
        assertX(1, 5, 60.5);

        assertX(2, 1, 12.5);
        assertX(2, 3, 36.5);
        assertX(2, 5, 60.5);

        // default tab stops = 100
        m.setDefaultTabStops(100);
        assertX(0, 1, 100.5);
        assertX(0, 3, 200.5);
        assertX(0, 5, 300.5);

        assertX(1, 1, 100.5);
        assertX(1, 3, 200.5);
        assertX(1, 5, 300.5);

        assertX(2, 1, 12.5);
        assertX(2, 3, 36.5);
        assertX(2, 5, 60.5);

        // change the second paragraph tab stops
        StyleAttributeMap a = StyleAttributeMap.of(StyleAttributeMap.TAB_STOPS, TabStops.of(55, 77, 99));
        control.applyStyle(TextPos.ofLeading(1, 0), TextPos.ofLeading(1, 999), a);
        assertX(0, 1, 100.5);
        assertX(0, 3, 200.5);
        assertX(0, 5, 300.5);

        assertX(1, 1, 55.5);
        assertX(1, 3, 77.5);
        assertX(1, 5, 99.5);

        assertX(2, 1, 12.5);
        assertX(2, 3, 36.5);
        assertX(2, 5, 60.5);
    }

    private void assertAttrs(int index, int charIndex, boolean leading, boolean forInsert, StyleAttributeMap expected) {
        int off = charIndex + (leading ? 0 : 1);
        TextPos p = new TextPos(index, off, charIndex, leading);
        StyleAttributeMap a = control.getStyleAttributeMap(p, forInsert);
        assertEquals(expected, a);
    }

    @Test
    public void getStyleAttributeMap() {
        control.appendText("BB", BOLD);
        control.appendText("II", ITALIC);
        control.appendText("\n");
        control.appendText("X", UNDER);

        // exact
        assertAttrs(0, 0, true, false, BOLD);
        assertAttrs(0, 0, false, false, BOLD);
        assertAttrs(0, 1, true, false, BOLD);
        assertAttrs(0, 1, false, false, BOLD);
        assertAttrs(0, 2, true, false, ITALIC);
        assertAttrs(0, 2, false, false, ITALIC);
        assertAttrs(0, 3, true, false, ITALIC);
        assertAttrs(0, 3, false, false, ITALIC);
        assertAttrs(0, 4, true, false, ITALIC);
        assertAttrs(0, 4, false, false, ITALIC);
        assertAttrs(0, 999, true, false, ITALIC);
        assertAttrs(0, 999, false, false, ITALIC);

        // for insert
        assertAttrs(0, 0, true, true, BOLD);
        assertAttrs(0, 0, false, true, BOLD);
        assertAttrs(0, 1, true, true, BOLD);
        assertAttrs(0, 1, false, true, BOLD);
        assertAttrs(0, 2, true, true, BOLD);
        assertAttrs(0, 2, false, true, ITALIC); // sic!
        assertAttrs(0, 3, true, true, ITALIC);
        assertAttrs(0, 3, false, true, ITALIC);
        assertAttrs(0, 4, true, true, ITALIC);
        assertAttrs(0, 4, false, true, ITALIC);
        assertAttrs(0, 999, true, true, ITALIC);
        assertAttrs(0, 999, false, true, ITALIC);

        // line 2

        // exact
        assertAttrs(1, 0, true, false, UNDER);
        assertAttrs(1, 0, false, false, UNDER);
        assertAttrs(1, 1, true, false, UNDER);
        assertAttrs(1, 1, false, false, UNDER);
        assertAttrs(1, 999, true, false, UNDER);
        assertAttrs(1, 999, false, false, UNDER);

        // for insert

        assertAttrs(1, 0, true, true, UNDER);
        assertAttrs(1, 0, false, true, UNDER);
        assertAttrs(1, 1, true, true, UNDER);
        assertAttrs(1, 1, false, true, UNDER);
        assertAttrs(1, 999, true, true, UNDER);
        assertAttrs(1, 999, false, true, UNDER);

        // beyond eof
        assertAttrs(999, 999, false, false, StyleAttributeMap.EMPTY);
        assertAttrs(999, 999, false, true, StyleAttributeMap.EMPTY);

        // TODO grapheme clusters
    }

    @Test
    public void queryAccessibilityEditable() {
        assertEquals(true, control.queryAccessibleAttribute(AccessibleAttribute.EDITABLE));
        control.setEditable(false);
        assertEquals(false, control.queryAccessibleAttribute(AccessibleAttribute.EDITABLE));
    }

    @Test
    public void queryAccessibilityText() {
        control.setLineEnding(LineEnding.LF);
        assertEquals(null, control.queryAccessibleAttribute(AccessibleAttribute.TEXT));
        control.select(TextPos.ZERO);
        assertEquals("\n", control.queryAccessibleAttribute(AccessibleAttribute.TEXT));
        control.appendText("1\n2\n");
        assertEquals("1\n", control.queryAccessibleAttribute(AccessibleAttribute.TEXT));
        control.select(TextPos.ofLeading(1, 0));
        assertEquals("2\n", control.queryAccessibleAttribute(AccessibleAttribute.TEXT));
        control.select(TextPos.ofLeading(999, 0));
        assertEquals("\n", control.queryAccessibleAttribute(AccessibleAttribute.TEXT));
        control.select(TextPos.ofLeading(1, 1), new TextPos(1, 1, 1, false));
        assertEquals("2\n", control.queryAccessibleAttribute(AccessibleAttribute.TEXT));
    }

    @Test
    public void queryAccessibilitySelectionAndCaret() {
        control.appendText("111\n222\n");
        control.setLineEnding(LineEnding.LF);

        control.select(TextPos.ZERO);
        assertEquals(0, control.queryAccessibleAttribute(AccessibleAttribute.SELECTION_START));
        assertEquals(0, control.queryAccessibleAttribute(AccessibleAttribute.SELECTION_END));
        assertEquals(0, control.queryAccessibleAttribute(AccessibleAttribute.CARET_OFFSET));

        control.select(TextPos.ofLeading(0, 1), TextPos.ofLeading(1, 2));
        assertEquals(1, control.queryAccessibleAttribute(AccessibleAttribute.SELECTION_START));
        assertEquals(6, control.queryAccessibleAttribute(AccessibleAttribute.SELECTION_END));
        assertEquals(6, control.queryAccessibleAttribute(AccessibleAttribute.CARET_OFFSET));
    }

    @Test
    public void queryAccessibilitySkin() {
        double size = 16.0;
        Font f = Font.getDefault();
        StyleAttributeMap FONT = StyleAttributeMap.builder().
            setFontFamily(f.getFamily()).
            setFontSize(size).
            build();

        control.setLineEnding(LineEnding.LF);
        control.appendText("111\n222\n");
        control.layout();
        control.applyStyle(TextPos.ZERO, control.getDocumentEnd(), FONT);
        control.select(TextPos.ZERO);

        // looking for the font size only since the platform may substitute
        assertEquals(size, ((Font)control.queryAccessibleAttribute(AccessibleAttribute.FONT)).getSize());

        Object hsb = control.lookup(".scroll-bar:horizontal");
        assertNotNull(hsb);
        assertEquals(hsb, control.queryAccessibleAttribute(AccessibleAttribute.HORIZONTAL_SCROLLBAR));

        Object vsb = control.lookup(".scroll-bar:vertical");
        assertNotNull(vsb);
        assertEquals(vsb, control.queryAccessibleAttribute(AccessibleAttribute.VERTICAL_SCROLLBAR));
    }

    @Test
    public void embeddedImages() {
        double controlWidth = 1000;
        double viewWidth = 982;

        byte[] bytes = RTUtil.redPng32x32();
        StyledSegment[] segments = {
            StyledSegment.of(" ", StyleAttributeMap.of(StyleAttributeMap.EMBEDDED_IMAGE, EmbeddedImageHelper.create(bytes, 32, 32, 32, 32, true)))
        };

        control.setPrefWidth(controlWidth);
        control.setPrefHeight(1000);

        StageLoader sl = new StageLoader(control);
        try {
            control.replaceText(TextPos.ZERO, TextPos.ZERO, new SegmentStyledInput(segments));
            control.setWrapText(true);
            RTUtil.firePulse();
            checkSizes(32, 32, 32);

            // targetWidth       targetHeight        keepAspect      containerWidth  renderedWidth       renderedHeight
            // ---------------------------------------------------------------------------------------------------
            // negative          AUTO                false           view            original            original
            c(-100, EmbeddedImage.AUTO, false, fix(viewWidth,2), fix(32,0), fix(32,0));
            // negative          AUTO                true            view            original            original
            c(-100, EmbeddedImage.AUTO, true, fix(viewWidth,2), fix(32,0), fix(32,0));
            // negative          positive            false           original        original            target
            c(-100, 320, false, fix(32,2), fix(32,0), 320);
            // negative          positive            true            scale           scaled              target
            c(-100, 320, true, fix(viewWidth,2), fix(320,0), 320);
            // FIT_WIDTH         AUTO                false           view            original            original
            c(EmbeddedImage.FIT_WIDTH, EmbeddedImage.AUTO, false, viewWidth, 32, 32);
            // FIT_WIDTH         AUTO (ignored)      true            view            view                scaled
            c(EmbeddedImage.FIT_WIDTH, EmbeddedImage.AUTO, true, viewWidth, 32, fix(222,0)); // FIX 32*scale
            // FIT_WIDTH         positive            false           view            max(original,view)  target
            c(EmbeddedImage.FIT_WIDTH, 320, false, viewWidth, 32, 320);
            // FIT_WIDTH         positive (ignored)  true            view            max(original,view)  scaled
            c(EmbeddedImage.FIT_WIDTH, 320, true, viewWidth, 32, fix(222,0)); // FIX 32*scale
            // FIT_WIDTH_ALWAYS  AUTO                false           view            view                original
            c(EmbeddedImage.FIT_WIDTH_ALWAYS, EmbeddedImage.AUTO, false, viewWidth, viewWidth, 32);
            // FIT_WIDTH_ALWAYS  AUTO (ignored)      true            view            view                scaled
            c(EmbeddedImage.FIT_WIDTH_ALWAYS, EmbeddedImage.AUTO, true, viewWidth, viewWidth, fix(32,0));
            // FIT_WIDTH_ALWAYS  positive            false           view            view                target
            c(EmbeddedImage.FIT_WIDTH_ALWAYS, 10, false, viewWidth, viewWidth, 10);
            // FIT_WIDTH_ALWAYS  positive (ignored)  true            view            view                scaled
            c(EmbeddedImage.FIT_WIDTH_ALWAYS, 10, true, viewWidth, viewWidth, fix(222,0)); // FIX 32*scale
            // AUTO              AUTO                false           original        original            original
            c(EmbeddedImage.AUTO, EmbeddedImage.AUTO, false, fix(32,2), fix(32,0), fix(32,0));
            // AUTO              AUTO                true            original        original            original
            c(EmbeddedImage.AUTO, EmbeddedImage.AUTO, true, fix(32,2), fix(32,0), fix(32,0));
            // AUTO              positive            false           original        target              target
            c(EmbeddedImage.AUTO, 320, false, fix(32,2), fix(32,0), 320);
            // AUTO              positive            true            original        scaled              target
            c(EmbeddedImage.AUTO, 320, true, fix(320,2), fix(320,0), 320);
            // positive          AUTO                false           target          target              target
            c(320, EmbeddedImage.AUTO, false, 320, 320, fix(320, 0));
            // positive          AUTO                true            target          target              scaled
            c(320, EmbeddedImage.AUTO, true, 320, 320, fix(320, 0));
            // positive          positive            false           target          target              target
            c(320, 100, false, 320, 320, 100);
            // positive          positive            true (ignored)  ?               target              target
            c(320, 100, true, 320, 320, 100);

        } finally {
            sl.dispose();
        }
    }

    // StubToolkit does not support image resizing, see StubImageLoaderFactory:44
    // We could remove this method once the resizing is implemented.
    @Deprecated
    private static double fix(double expected, double actual) {
        return actual; // FIX
    }

    private void c(double w, double h, boolean keepAspectRatio, double expContainerWidth, double expectedWidth, double expectedHeight) {
        updateImage(w, h, keepAspectRatio);
        RTUtil.firePulse();
        checkSizes(expContainerWidth, expectedWidth, expectedHeight);
    }

    private void updateImage(double w, double h, boolean keepAspectRatio) {
        EmbeddedImage im = RTUtil.embeddedImageAt(control, TextPos.ZERO);
        assertNotNull(im, "EmbeddedImage");
        EmbeddedImage updated = im.copy(w, h, keepAspectRatio);
        StyleAttributeMap a = StyleAttributeMap.of(StyleAttributeMap.EMBEDDED_IMAGE, updated);
        control.applyStyle(TextPos.ZERO, new TextPos(0, 1, 1, true), a);
        assertEquals(updated, RTUtil.embeddedImageAt(control, TextPos.ZERO));
    }

    // this check assumes specific structure of the EmbeddedImage containers
    private void checkSizes(double containerWidth, double expectedWidth, double expectedHeight) {
        List<TextCell> nodes = EmbeddedImageHelper.getVisibleTextCells(control);
        assertTrue(nodes.size() > 0, "must have at least one image cell");
        TextCell cell = nodes.get(0);
        assertTrue(cell.getContent() instanceof TextFlow);
        TextFlow f = (TextFlow)cell.getContent();
        assertTrue(f.getChildren().size() > 0);
        Node n = f.getChildren().get(0);
        assertTrue(n instanceof Label);
        Label container = (Label)n;
        assertTrue(container.getGraphic() instanceof ImageView);
        ImageView im = (ImageView)container.getGraphic();

        // fails due to StubImageLoaderFactory:44
        //assertNull(im.getImage().getException());
        //assertEquals(32, im.getImage().getWidth(), "image width");
        //assertEquals(32, im.getImage().getHeight(), "image height");

        Bounds b = im.getLayoutBounds();
        assertEquals(containerWidth, container.getWidth(), EPSILON, "container width");
        assertEquals(expectedWidth, b.getWidth(), EPSILON, "width");
        assertEquals(expectedHeight, b.getHeight(), EPSILON, "height");
    }
}
