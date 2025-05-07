/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.sun.jfx.incubator.scene.control.richtext.VFlow;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.RichTextAreaShim;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.StyleHandlerRegistry;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.RichTextFormatHandler;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.skin.RichTextAreaSkin;
import test.jfx.incubator.scene.control.richtext.support.RTUtil;
import test.jfx.incubator.scene.control.richtext.support.TestStyledInput;
import test.jfx.incubator.scene.util.TUtil;

/**
 * Tests the RichTextArea control.
 *
 * NOTES:
 * 1. methods that involve moving the caret (backspace, delete, move*, select*, etc.) needs a real
 * text layout and therefore need to be headful (or wait for JDK-8342565).
 * 2. operations with models that contain more than one paragraph might also fail due to scrollCaretToVisible().
 *
 * TODO accessibility APIs
 */
public class RichTextAreaTest {
    private RichTextArea control;
    private static final StyleAttributeMap BOLD = StyleAttributeMap.builder().setBold(true).build();

    @BeforeEach
    public void beforeEach() {
        TUtil.setUncaughtExceptionHandler();

        control = new RichTextArea();
        control.setSkin(new RichTextAreaSkin(control));
    }

    @AfterEach
    public void afterEach() {
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
    }

    @Test
    public void appendTextWithStyles() {
        TextPos p = control.appendText("a", BOLD);
        assertEquals(TextPos.ofLeading(0, 1), p);
        control.select(p);
        assertEquals(BOLD, control.getActiveStyleAttributeMap());
        assertEquals("a", text());
    }

    @Test
    public void appendTextFromStyledInput() {
        TestStyledInput in = TestStyledInput.plainText("a\nb");
        TextPos p = control.appendText(in);
        assertEquals(TextPos.ofLeading(1, 1), p);
    }

    @Test
    public void applyStyle() {
        TestStyledInput in = TestStyledInput.plainText("a\nb");
        TextPos p = control.appendText(in);
        control.applyStyle(TextPos.ZERO, TextPos.ofLeading(0, 1), BOLD);
        assertEquals(TextPos.ofLeading(1, 1), p);
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
        RTUtil.copyToClipboard("");
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
    }

    // TODO combine with copy()
    @Disabled("JDK-8355415")
    @Test
    public void copyExt() {
        RTUtil.copyToClipboard("");
        control.appendText("ax");
        control.selectAll();
        control.copy();
        assertEquals("ax", Clipboard.getSystemClipboard().getString());

        // copying an empty selection should not modify the clipboard content
        control.clearSelection();
        assertEquals("ax", Clipboard.getSystemClipboard().getString());

        // the following code fails due to JDK-8355415
        control.appendText("\n1");
        control.select(new TextPos(0, 2, 1, false), control.getDocumentEnd());
        control.copy();
        assertEquals("\n1", Clipboard.getSystemClipboard().getString());
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
        assertEquals("a{!}", v);
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
        // TODO this should throw an IOOBE
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

    @Disabled("JDK-8355415") // FIX
    @Test
    public void insertLineBreak() {
        control.appendText("123");
        control.select(TextPos.ofLeading(0, 1));
        control.insertLineBreak();
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
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        control.read(fmt, in);
        String text2 = text();
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
        control.replaceText(TextPos.ofLeading(0, 1), TextPos.ofLeading(0, 3), "-", false);
        assertEquals("1-4", text());
    }

    @Test
    public void replaceTextFromStyledInput() {
        TestStyledInput in = TestStyledInput.plainText("-");
        control.appendText("1234");
        control.replaceText(TextPos.ofLeading(0, 1), TextPos.ofLeading(0, 3), in, false);
        assertEquals("1-4", text());
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
    public void write() throws Exception {
        control.appendText("1 bold");
        control.applyStyle(TextPos.ofLeading(0, 2), TextPos.ofLeading(0, 6), BOLD);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        control.write(out);
        byte[] b = out.toByteArray();
        assertEquals("1 {b}bold{!}", new String(b, StandardCharsets.US_ASCII));
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

    /**
     * Tests the shim.
     */
    // TODO remove once a real test which needs the shim is added.
    @Test
    public void testShim() {
        RichTextArea t = new RichTextArea();
        VFlow f = RichTextAreaShim.vflow(t);
    }
}
