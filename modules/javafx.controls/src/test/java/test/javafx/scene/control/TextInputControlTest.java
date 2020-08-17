/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.StyleableProperty;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Font;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.util.Arrays;
import java.util.Collection;
import javafx.scene.control.IndexRange;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import com.sun.javafx.tk.Toolkit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.pgstub.StubToolkit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
/**
 */
@RunWith(Parameterized.class)
public class TextInputControlTest {
    @SuppressWarnings("rawtypes")
    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][]{
                {TextField.class},
                {PasswordField.class},
                {TextArea.class}
        });
    }

    private TextInputControl textInput;
    private Class type;

    public TextInputControlTest(Class type) {
        this.type = type;
    }

    @Before public void setup() throws Exception {
        textInput = (TextInputControl) type.newInstance();
        setUncaughtExceptionHandler();
    }

    @After public void cleanup() {
        removeUncaughtExceptionHandler();
    }

    private void setUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }

    private void removeUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    /******************************************************
     * Test the default states                            *
     *****************************************************/

    @Test public void textDefaultsToEmptyString() {
        assertEquals("", textInput.getText());
    }

    @Test public void editableDefaultsToTrue() {
        assertTrue(textInput.isEditable());
    }

    @Test public void anchorDefaultsToZero() {
        assertEquals(0, textInput.getAnchor());
    }

    @Test public void caretPositionDefaultsToZero() {
        assertEquals(0, textInput.getCaretPosition());
    }

    @Test public void lengthDefaultsToZero() {
        assertEquals(0, textInput.getLength());
    }

    @Test public void selectedTextDefaultsToEmptyString() {
        assertEquals("", textInput.getSelectedText());
    }

    @Test public void selectionDefaultsToEmpty() {
        assertEquals(0, textInput.getSelection().getLength());
    }

    @Test public void selectionStartDefaultsToZero() {
        assertEquals(0, textInput.getSelection().getStart());
    }

    @Test public void selectionEndDefaultsToZero() {
        assertEquals(0, textInput.getSelection().getEnd());
    }

    /*********************************************************************
     * Tests for CSS                                                     *
     ********************************************************************/

    @Test public void fontSetFromCSS() {
        textInput.setStyle("-fx-font: 24 Helvetica");
        Scene s = new Scene(textInput);
        textInput.applyCss();
        assertEquals(Font.font("Helvetica", 24), textInput.getFont());
    }

    /******************************************************
     * Test for text                                      *
     *****************************************************/

    @Test public void settingTextUpdatesTheText() {
        textInput.setText("This is a test");
        assertEquals("This is a test", textInput.getText());
        assertEquals("This is a test", textInput.textProperty().get());
    }

    @Test public void textCanBeNull() {
        textInput.setText(null);
        assertNull(textInput.getText());
    }

    @Test public void textCanBeSwitchedBetweenNullAndAValue() {
        textInput.setText(null);
        textInput.setText("Test");
        assertEquals("Test", textInput.getText());
    }

    @Test public void textCanBeSwitchedFromAValueToNull() {
        textInput.setText("Test");
        textInput.setText(null);
        assertNull(textInput.getText());
    }

    @Test public void textIsNullThenBoundThenUnboundAndShouldReturnTheValueWhenBound() {
        textInput.setText(null);
        StringProperty other = new SimpleStringProperty("Peppers");
        textInput.textProperty().bind(other);
        textInput.textProperty().unbind();
        assertEquals("Peppers", textInput.getText());
    }

    @Test public void textHasValueThenIsBoundToNullShouldReturnNullFromGet() {
        textInput.setText("Value");
        StringProperty other = new SimpleStringProperty(null);
        textInput.textProperty().bind(other);
        assertNull(textInput.getText());
    }

    @Test public void textHasValueThenIsBoundToNullAndUnboundShouldReturnNullFromGet() {
        textInput.setText("Value");
        StringProperty other = new SimpleStringProperty(null);
        textInput.textProperty().bind(other);
        textInput.textProperty().unbind();
        assertNull(textInput.getText());
    }

    @Test public void textHasValueThenIsBoundToNullAndUnboundThenSetShouldReturnNewValueFromGet() {
        textInput.setText("Value");
        StringProperty other = new SimpleStringProperty(null);
        textInput.textProperty().bind(other);
        textInput.textProperty().unbind();
        textInput.setText("New Value");
        assertEquals("New Value", textInput.getText());
    }

    @Test public void textCanBeBound() {
        StringProperty other = new SimpleStringProperty("Apples");
        textInput.textProperty().bind(other);
        assertEquals("Apples", textInput.getText());
        other.set("Oranges");
        assertEquals("Oranges", textInput.getText());
    }

    @Test public void cannotSpecifyTextViaCSS() {
        try {
            CssMetaData styleable = ((StyleableProperty)textInput.textProperty()).getCssMetaData();
            assertNull(styleable);
        } catch (ClassCastException ignored) {
            // pass!
        } catch (Exception e) {
            org.junit.Assert.fail(e.toString());
        }
    }

    @Test public void settingTextNotifiesOfChange() {
        final boolean[] passed = new boolean[] { false };
        textInput.textProperty().addListener((observable, oldValue, newValue) -> {
            passed[0] = true;
        });
        textInput.setText("Apples");
        assertTrue(passed[0]);
    }

    // Test for JDK-8212102
    @Test public void testControlCharacters() {
        try {
            String cc = "\r\n\n";
            String str = "123456";

            textInput.setText(cc);

            textInput.setText(str);
            textInput.replaceText(0, 6, cc);

            textInput.setText(str);
            textInput.replaceText(new IndexRange(0, 6), cc);

            textInput.setText(str);
            textInput.selectAll();
            textInput.replaceSelection(cc);

            textInput.setText(str);
            textInput.selectRange(0, 6);
            textInput.replaceSelection(cc);

        } catch (Exception e) {
            fail("Control characters(\\r\\n) caused Exception: " + e);
        }
    }

    @Test public void controlCharactersAreOmitted_setText_getText() {
        String s = "This is " + '\0' + "a test";
        textInput.setText(s);
        assertEquals("This is a test", textInput.getText());
    }

    @Test public void controlCharactersAreOmitted_setText_textProperty_get() {
        String s = "This is " + '\0' + "a test";
        textInput.setText(s);
        assertEquals("This is a test", textInput.textProperty().get());
    }

    @Test public void controlCharactersAreOmitted_bound_getText() {
        StringProperty other = new SimpleStringProperty("This is " + '\0' + "a test");
        textInput.textProperty().bind(other);
        assertEquals("This is a test", textInput.getText());
        other.set("Bro" + '\5' + "ken");
        assertEquals("Broken", textInput.getText());
    }

    @Test public void controlCharactersAreOmitted_bound_textProperty_get() {
        StringProperty other = new SimpleStringProperty("This is " + '\0' + "a test");
        textInput.textProperty().bind(other);
        assertEquals("This is a test", textInput.textProperty().get());
        other.set("Bro" + '\5' + "ken");
        assertEquals("Broken", textInput.textProperty().get());
    }

    // selection is changed when text is changed??
    // anchor and caret position updated when selection is changed due to text change??
    // selected text is updated when selection changes due to a text change??
    // length is updated when text changes

    /******************************************************
     * Test for editable                                  *
     *****************************************************/

    @Test public void settingEditableValueShouldWork() {
        textInput.setEditable(false);
        assertFalse(textInput.isEditable());
    }

    @Test public void settingEditableAndThenCreatingAModelAndReadingTheValueStillWorks() {
        textInput.setEditable(false);
        assertFalse(textInput.editableProperty().get());
    }

    @Test public void editableCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(false);
        textInput.editableProperty().bind(other);
        assertFalse(textInput.isEditable());
        other.set(true);
        assertTrue(textInput.isEditable());
    }

    @Test public void cannotSpecifyEditableViaCSS() {
        try {
            CssMetaData styleable = ((StyleableProperty)textInput.editableProperty()).getCssMetaData();
            assertNull(styleable);
        } catch (ClassCastException ignored) {
            // pass!
        } catch (Exception e) {
            org.junit.Assert.fail(e.toString());
        }
    }

    @Test public void settingEditableNotifiesOfChange() {
        final boolean[] passed = new boolean[] { false };
        textInput.editableProperty().addListener((observable, oldValue, newValue) -> {
            passed[0] = true;
        });
        textInput.setEditable(false);
        assertTrue(passed[0]);
    }

    /******************************************************
     * Test for anchor                                    *
     *****************************************************/

    @Test public void anchorIsSetWhenSelectionChanges() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        assertEquals(4, textInput.getAnchor());
    }

    @Test public void anchorIsSetWhenSelectionChanges2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 4);
        assertEquals(9, textInput.getAnchor());
    }

    // updated when text changes
    @Test public void anchorIsSetToCaretPositionWhenTextChanges() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.setText("Gone");
        assertEquals(textInput.getCaretPosition(), textInput.getAnchor());
    }

    /******************************************************
     * Test for caretPosition                             *
     *****************************************************/

    @Test public void caretPositionIsSetWhenSelectionChanges() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        assertEquals(9, textInput.getCaretPosition());
    }

    @Test public void caretPositionIsSetWhenSelectionChanges2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 4);
        assertEquals(4, textInput.getCaretPosition());
    }

    @Test
    public void caretAndAnchorPositionAfterSettingText() {
        textInput.setText("The quick brown fox");
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
    }

    // Test for JDK-8178417
    @Test public void caretPositionUndo() {
        Toolkit tk = (StubToolkit)Toolkit.getToolkit();
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        String text = "01234";

        textInput.setText(text);
        stage.setScene(scene);
        root.getChildren().removeAll();
        root.getChildren().add(textInput);
        stage.show();
        tk.firePulse();

        KeyEventFirer keyboard = new KeyEventFirer(textInput);
        keyboard.doKeyPress(KeyCode.HOME);

        for(int i = 1; i < text.length() + 1; ++i) {
            keyboard.doKeyPress(KeyCode.RIGHT);
            tk.firePulse();
        }
        for(int i = 1; i < text.length() + 1; ++i) {
            textInput.undo();
        }
        assertEquals(text.length(), textInput.getCaretPosition());
        root.getChildren().removeAll();
        stage.hide();
        tk.firePulse();
    }

    /******************************************************
     * Test for length                                    *
     *****************************************************/

    // TODO null text results in 0 length

    @Test public void emptyTextResultsInZeroLength() {
        textInput.setText("Hello");
        textInput.setText("");
        assertEquals(0, textInput.getLength());
    }

    @Test public void lengthMatchesStringLength() {
        final String string = "Hello";
        textInput.setText(string);
        assertEquals(string.length(), textInput.getLength());
    }

    @Test public void lengthChangeNotificationWhenTextIsUpdatedToNonEmptyResult() {
        final boolean[] passed = new boolean[] { false };
        textInput.lengthProperty().addListener((observable, oldValue, newValue) -> {
            passed[0] = true;
        });
        textInput.setText("Hello");
        assertTrue(passed[0]);
    }

    @Test public void lengthChangeNotificationWhenTextIsSetToEmptyResult() {
        textInput.setText("Goodbye");
        final boolean[] passed = new boolean[] { false };
        textInput.lengthProperty().addListener((observable, oldValue, newValue) -> {
            passed[0] = true;
        });
        textInput.setText("");
        assertTrue(passed[0]);
    }

    /******************************************************
     * Test for maximumLength                             *
     *****************************************************/

    // set maximum length to less than current length

    /******************************************************
     * Test for selected text                             *
     *****************************************************/

    // TODO test null text and some random range

    @Test public void selectedTextMatchesTextAndSelection() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(0, 3);
        assertEquals("The", textInput.getSelectedText());
    }

    @Test public void selectedTextMatchesTextAndSelection2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        assertEquals("quick", textInput.getSelectedText());
    }

    @Test public void selectedTextMatchesTextAndSelection3() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 19);
        assertEquals("brown fox", textInput.getSelectedText());
    }

    @Test public void selectedTextIsClearedWhenTextChanges() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.setText("");
        assertEquals("", textInput.getSelectedText());
    }

    @Test public void selectedTextWorksWhenSelectionExceedsPossibleRange() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 180);
        assertEquals("brown fox", textInput.getSelectedText());
    }

    @Test public void selectedTextWorksWhenSelectionExceedsPossibleRange2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(100, 180);
        assertEquals("", textInput.getSelectedText());
    }

//    @Test public void selectedTextWorksWhenSelectionIsBound() {
//        ObjectProperty<IndexRange> other = new SimpleObjectProperty<IndexRange>(new IndexRange(4, 9));
//        textInput.setText("The quick brown fox");
//        textInput.selectionProperty().bind(other);
//        assertEquals("quick", textInput.getSelectedText());
//        other.set(new IndexRange(10, 19));
//        assertEquals("brown fox", textInput.getSelectedText());
//    }

    @Test public void selectedTextWorksWhenTextIsBound() {
        StringProperty other = new SimpleStringProperty("There and back again");
        textInput.textProperty().bind(other);
        textInput.selectRange(0, 5);
        assertEquals("There", textInput.getSelectedText());
        other.set("Cleared!");
        assertEquals("", textInput.getSelectedText());
    }

    @Test public void selectedTextChangeEvents() {
        final boolean[] passed = new boolean[] { false };
        textInput.setText("The quick brown fox");
        textInput.selectedTextProperty().addListener(observable -> {
            passed[0] = true;
        });
        textInput.selectRange(0, 3);
        assertTrue(passed[0]);
    }

    @Test public void selectedTextChangeEvents2() {
        final boolean[] passed = new boolean[] { false };
        textInput.setText("The quick brown fox");
        textInput.selectRange(0, 3);
        textInput.selectedTextProperty().addListener(observable -> {
            passed[0] = true;
        });
        textInput.selectRange(10, 180);
        assertTrue(passed[0]);
    }

    @Test public void selectedTextChangeEvents3() {
        final boolean[] passed = new boolean[] { false };
        StringProperty other = new SimpleStringProperty("There and back again");
        textInput.textProperty().bind(other);
        textInput.selectRange(0, 5);
        textInput.selectedTextProperty().addListener(observable -> {
            passed[0] = true;
        });
        other.set("Cleared!");
        assertTrue(passed[0]);
    }

    /******************************************************
     * Test for selection                                 *
     *****************************************************/

    @Test public void selectionIsClearedWhenTextChanges() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.setText("");
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void selectionCannotBeSetToBeOutOfRange() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 99);
        assertEquals(new IndexRange(4, 19), textInput.getSelection());
    }

    @Test public void selectionCannotBeSetToBeOutOfRange2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(44, 99);
        assertEquals(new IndexRange(19, 19), textInput.getSelection());
    }

//    @Test public void selectionCanBeBound() {
//        ObjectProperty<IndexRange> other = new SimpleObjectProperty<IndexRange>(new IndexRange(4, 9));
//        textInput.selectionProperty().bind(other);
//        assertEquals(new IndexRange(4, 9), textInput.getSelection());
//        other.set(new IndexRange(10, 19));
//        assertEquals(new IndexRange(10, 19), textInput.getSelection());
//    }

    @Test public void selectionChangeEventsHappen() {
        final boolean[] passed = new boolean[] { false };
        textInput.selectionProperty().addListener(observable -> {
            passed[0] = true;
        });
        textInput.selectRange(0, 3);
        assertTrue(passed[0]);
    }

//    @Test public void selectionChangeEventsHappenWhenBound() {
//        final boolean[] passed = new boolean[] { false };
//        ObjectProperty<IndexRange> other = new SimpleObjectProperty<IndexRange>(new IndexRange(0, 5));
//        textInput.selectionProperty().addListener(new InvalidationListener() {
//            @Override public void invalidated(Observable observable) {
//                passed[0] = true;
//            }
//        });
//        textInput.selectionProperty().bind(other);
//        assertTrue(passed[0]);
//    }

//    @Test public void selectionChangeEventsHappenWhenBound2() {
//        final boolean[] passed = new boolean[] { false };
//        ObjectProperty<IndexRange> other = new SimpleObjectProperty<IndexRange>(new IndexRange(0, 5));
//        textInput.selectionProperty().bind(other);
//        textInput.selectionProperty().addListener(new InvalidationListener() {
//            @Override public void invalidated(Observable observable) {
//                passed[0] = true;
//            }
//        });
//        assertFalse(passed[0]);
//        other.set(new IndexRange(1, 2));
//        assertTrue(passed[0]);
//    }

    @Test public void selectionChangeEventsHappenWhenTextIsChanged() {
        final boolean[] passed = new boolean[] { false };
        StringProperty other = new SimpleStringProperty("There and back again");
        textInput.textProperty().bind(other);
        textInput.selectRange(0, 5);
        textInput.selectionProperty().addListener(observable -> {
            passed[0] = true;
        });
        other.set("Cleared!");
        assertTrue(passed[0]);
    }

    /******************************************************
     * Test for cut/copy/paste                            *
     *****************************************************/

    @Test public void cutRemovesSelection() {
        // Skip for PasswordField
        if (textInput instanceof PasswordField) return;
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.cut();
        assertEquals("The  brown fox", textInput.getText());
    }

    @Test public void pasteReplacesSelection() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        copy("slow");
        textInput.paste();
        assertEquals("The slow brown fox", textInput.getText());
    }

    @Test public void pasteIllegalCharacters() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(19, 19);
        copy("" + '\0');
        textInput.paste();
        assertEquals("The quick brown fox", textInput.getText());
    }

    @Test public void pasteIllegalCharactersCaretNotAtZero() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 4);
        copy("slow" + '\0');
        textInput.paste();
        assertEquals(8, textInput.getCaretPosition());
        assertEquals(8, textInput.getAnchor());
    }

    @Test public void pasteIllegalCharactersSelection() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        copy("slow" + '\0');
        textInput.paste();
        assertEquals("The slow brown fox", textInput.getText());
    }

    @Test public void pasteIllegalCharactersIntoSelectionPositionsCaretCorrectly() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        copy("slow" + '\0');
        textInput.paste();
        assertEquals(8, textInput.getCaretPosition());
        assertEquals(8, textInput.getAnchor());
    }

    /******************************************************
     * Test for manipulating selection via methods        *
     *****************************************************/

    // cut ends up removing the selection, and setting anchor / caretPosition to match index
    @Test public void cutRemovesSelectionAndResetsAnchorAndCaretPositionToIndex() {
        // Skip for PasswordField
        if (textInput instanceof PasswordField) return;
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.cut();
        assertEquals(4, textInput.getAnchor());
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
        assertEquals("", textInput.getSelectedText());
    }

    @Test public void pasteWithEmptySelection() {
        textInput.setText("quick brown fox");
        textInput.selectRange(0,0);
        copy("The ");
        textInput.paste();
        assertEquals(4, textInput.getAnchor());
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
        assertEquals("", textInput.getSelectedText());
    }

    @Test public void pasteWithSelection() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        copy("slow");
        textInput.paste();
        assertEquals(8, textInput.getAnchor());
        assertEquals(8, textInput.getCaretPosition());
        assertEquals(new IndexRange(8, 8), textInput.getSelection());
        assertEquals("", textInput.getSelectedText());
    }

    @Test public void pasteAll() {
        textInput.setText("The quick brown fox");
        textInput.selectAll();
        copy("Gone");
        textInput.paste();
        assertEquals(4, textInput.getAnchor());
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
        assertEquals("", textInput.getSelectedText());
    }

    @Test public void selectBackwardHasNoEffectWhenCaretPositionIsAlreadyZero() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(3, 0);
        textInput.selectBackward();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(new IndexRange(0, 3), textInput.getSelection());
    }

    @Test public void selectBackwardMovesCaretPositionOnePlaceLeft_CaretPositionRightOfAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(0, 3);
        textInput.selectBackward();
        assertEquals(2, textInput.getCaretPosition());
        assertEquals(new IndexRange(0, 2), textInput.getSelection());
    }

    @Test public void selectBackwardMovesCaretPositionOnePlaceLeft_CaretPositionEqualsAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(3, 3);
        textInput.selectBackward();
        assertEquals(2, textInput.getCaretPosition());
        assertEquals(new IndexRange(2, 3), textInput.getSelection());
    }

    @Test public void selectBackwardMovesCaretPositionOnePlaceLeft_CaretPositionLeftOfAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(6, 3);
        textInput.selectBackward();
        assertEquals(2, textInput.getCaretPosition());
        assertEquals(new IndexRange(2, 6), textInput.getSelection());
    }

    @Test public void selectForwardHasNoEffectWhenCaretPositionIsAtLength() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(3, 19);
        textInput.selectForward();
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(new IndexRange(3, 19), textInput.getSelection());
    }

    @Test public void selectForwardMovesCaretPositionOnePlaceRight_CaretPositionRightOfAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(0, 3);
        textInput.selectForward();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(new IndexRange(0, 4), textInput.getSelection());
    }

    @Test public void selectForwardMovesCaretPositionOnePlaceRight_CaretPositionEqualsAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(3, 3);
        textInput.selectForward();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(new IndexRange(3, 4), textInput.getSelection());
    }

    @Test public void selectForwardMovesCaretPositionOnePlaceRight_CaretPositionLeftOfAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(6, 3);
        textInput.selectForward();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(new IndexRange(4, 6), textInput.getSelection());
    }

    @Test public void previousWordWithNoText() {
        textInput.previousWord();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void previousWordWithSelection_caretPositionBeforeAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(15, 10);
        textInput.previousWord();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void previousWordWithSelection_caretPositionBeforeAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(12, 6);
        textInput.previousWord();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void previousWordWithSelection_caretPositionAfterAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 15);
        textInput.previousWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void previousWordWithSelection_caretPositionAfterAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(6, 12);
        textInput.previousWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void previousWord_caretWithinAWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(12);
        textInput.previousWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void previousWord_caretAfterWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(15);
        textInput.previousWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void previousWord_caretBeforeWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(10);
        textInput.previousWord();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void previousWord_caretWithinWhitespace() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(10);
        textInput.previousWord();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void previousWord_multipleWhitespaceInARow() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(11);
        textInput.previousWord();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void previousWord_withANumber() {
        textInput.setText("There are 5 cards in the hand");
        textInput.positionCaret(12);
        textInput.previousWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void previousWord_withALongNumber() {
        textInput.setText("There are 52 cards in the deck");
        textInput.positionCaret(13);
        textInput.previousWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void nextWordWithNoText() {
        textInput.nextWord();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void nextWordWithSelection_caretPositionBeforeAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 4);
        textInput.nextWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void nextWordWithSelection_caretPositionBeforeAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 2);
        textInput.nextWord();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void nextWordWithSelection_caretPositionAfterAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.nextWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void nextWordWithSelection_caretPositionAfterAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(5, 11);
        textInput.nextWord();
        assertEquals(16, textInput.getCaretPosition());
        assertEquals(16, textInput.getAnchor());
        assertEquals(new IndexRange(16, 16), textInput.getSelection());
    }

    @Test public void nextWord_caretWithinAWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(6);
        textInput.nextWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void nextWord_caretAfterWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(9);
        textInput.nextWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void nextWord_caretBeforeWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(4);
        textInput.nextWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void nextWord_caretWithinWhitespace() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(10);
        textInput.nextWord();
        assertEquals(11, textInput.getCaretPosition());
        assertEquals(11, textInput.getAnchor());
        assertEquals(new IndexRange(11, 11), textInput.getSelection());
    }

    @Test public void nextWord_multipleWhitespaceInARow() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(9);
        textInput.nextWord();
        assertEquals(11, textInput.getCaretPosition());
        assertEquals(11, textInput.getAnchor());
        assertEquals(new IndexRange(11, 11), textInput.getSelection());
    }

    @Test public void nextWord_toTheEnd() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(16);
        textInput.nextWord();
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(19, textInput.getAnchor());
        assertEquals(new IndexRange(19, 19), textInput.getSelection());
    }

    @Test public void nextWord_withANumber() {
        textInput.setText("There are 5 cards in the hand");
        textInput.positionCaret(6);
        textInput.nextWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void nextWord_withALongNumber() {
        textInput.setText("There are 52 cards in the deck");
        textInput.positionCaret(10);
        textInput.nextWord();
        assertEquals(13, textInput.getCaretPosition());
        assertEquals(13, textInput.getAnchor());
        assertEquals(new IndexRange(13, 13), textInput.getSelection());
    }

    @Test public void endOfNextWordWithNoText() {
        textInput.endOfNextWord();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void endOfNextWordWithSelection_caretPositionBeforeAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 4);
        textInput.endOfNextWord();
        assertEquals(9, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 9), textInput.getSelection());
    }

    @Test public void endOfNextWordWithSelection_caretPositionBeforeAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 2);
        textInput.endOfNextWord();
        assertEquals(3, textInput.getCaretPosition());
        assertEquals(3, textInput.getAnchor());
        assertEquals(new IndexRange(3, 3), textInput.getSelection());
    }

    @Test public void endOfNextWordWithSelection_caretPositionAfterAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.endOfNextWord();
        assertEquals(15, textInput.getCaretPosition());
        assertEquals(15, textInput.getAnchor());
        assertEquals(new IndexRange(15, 15), textInput.getSelection());
    }

    @Test public void endOfNextWordWithSelection_caretPositionAfterAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(5, 11);
        textInput.endOfNextWord();
        assertEquals(15, textInput.getCaretPosition());
        assertEquals(15, textInput.getAnchor());
        assertEquals(new IndexRange(15, 15), textInput.getSelection());
    }

    @Test public void endOfNextWord_caretWithinAWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(6);
        textInput.endOfNextWord();
        assertEquals(9, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 9), textInput.getSelection());
    }

    @Test public void endOfNextWord_caretAfterWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(9);
        textInput.endOfNextWord();
        assertEquals(15, textInput.getCaretPosition());
        assertEquals(15, textInput.getAnchor());
        assertEquals(new IndexRange(15, 15), textInput.getSelection());
    }

    @Test public void endOfNextWord_caretBeforeWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(4);
        textInput.endOfNextWord();
        assertEquals(9, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 9), textInput.getSelection());
    }

    @Test public void endOfNextWord_caretWithinWhitespace() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(10);
        textInput.endOfNextWord();
        assertEquals(16, textInput.getCaretPosition());
        assertEquals(16, textInput.getAnchor());
        assertEquals(new IndexRange(16, 16), textInput.getSelection());
    }

    @Test public void endOfNextWord_multipleWhitespaceInARow() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(9);
        textInput.endOfNextWord();
        assertEquals(16, textInput.getCaretPosition());
        assertEquals(16, textInput.getAnchor());
        assertEquals(new IndexRange(16, 16), textInput.getSelection());
    }

    @Test public void endOfNextWord_withANumber() {
        textInput.setText("There are 5 cards in the hand");
        textInput.positionCaret(6);
        textInput.endOfNextWord();
        assertEquals(9, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 9), textInput.getSelection());
    }

    @Test public void endOfNextWord_withANumber_CaretOnANumber() {
        textInput.setText("There are 5 cards in the hand");
        textInput.positionCaret(10);
        textInput.endOfNextWord();
        assertEquals(11, textInput.getCaretPosition());
        assertEquals(11, textInput.getAnchor());
        assertEquals(new IndexRange(11, 11), textInput.getSelection());
    }

    @Test public void endOfNextWord_withALongNumber_CaretOnANumber() {
        textInput.setText("There are 52 cards in the deck");
        textInput.positionCaret(10);
        textInput.endOfNextWord();
        assertEquals(12, textInput.getCaretPosition());
        assertEquals(12, textInput.getAnchor());
        assertEquals(new IndexRange(12, 12), textInput.getSelection());
    }

    @Test public void selectPreviousWordWithNoText() {
        textInput.selectPreviousWord();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void selectPreviousWordWithSelection_caretPositionBeforeAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(15, 10);
        textInput.selectPreviousWord();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(15, textInput.getAnchor());
        assertEquals(new IndexRange(4, 15), textInput.getSelection());
    }

    @Test public void selectPreviousWordWithSelection_caretPositionAfterAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 15);
        textInput.selectPreviousWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void selectPreviousWordWithSelection_caretPositionAfterAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(11, 15);
        textInput.selectPreviousWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(11, textInput.getAnchor());
        assertEquals(new IndexRange(10, 11), textInput.getSelection());
    }

    @Test public void selectPreviousWord_caretWithinAWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(12);
        textInput.selectPreviousWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(12, textInput.getAnchor());
        assertEquals(new IndexRange(10, 12), textInput.getSelection());
    }

    @Test public void selectPreviousWord_caretAfterWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(15);
        textInput.selectPreviousWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(15, textInput.getAnchor());
        assertEquals(new IndexRange(10, 15), textInput.getSelection());
    }

    @Test public void selectPreviousWord_caretBeforeWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(10);
        textInput.selectPreviousWord();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(4, 10), textInput.getSelection());
    }

    @Test public void selectPreviousWord_caretWithinWhitespace() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(10);
        textInput.selectPreviousWord();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(4, 10), textInput.getSelection());
    }

    @Test public void selectPreviousWord_multipleWhitespaceInARow() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(11);
        textInput.selectPreviousWord();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(11, textInput.getAnchor());
        assertEquals(new IndexRange(4, 11), textInput.getSelection());
    }

    @Test public void selectNextWordWithNoText() {
        textInput.selectNextWord();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void selectNextWordWithSelection_caretPositionBeforeAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 4);
        textInput.selectNextWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 10), textInput.getSelection());
    }

    @Test public void selectNextWordWithSelection_caretPositionBeforeAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 2);
        textInput.selectNextWord();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(4, 9), textInput.getSelection());
    }

    @Test public void selectNextWordWithSelection_caretPositionAfterAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.selectNextWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 10), textInput.getSelection());
    }

    @Test public void selectNextWordWithSelection_caretPositionAfterAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(5, 11);
        textInput.selectNextWord();
        assertEquals(16, textInput.getCaretPosition());
        assertEquals(5, textInput.getAnchor());
        assertEquals(new IndexRange(5, 16), textInput.getSelection());
    }

    @Test public void selectNextWord_caretWithinAWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(6);
        textInput.selectNextWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(6, textInput.getAnchor());
        assertEquals(new IndexRange(6, 10), textInput.getSelection());
    }

    @Test public void selectNextWord_caretAfterWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(9);
        textInput.selectNextWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 10), textInput.getSelection());
    }

    @Test public void selectNextWord_caretBeforeWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(4);
        textInput.selectNextWord();
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 10), textInput.getSelection());
    }

    @Test public void selectNextWord_caretWithinWhitespace() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(10);
        textInput.selectNextWord();
        assertEquals(11, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 11), textInput.getSelection());
    }

    @Test public void selectNextWord_multipleWhitespaceInARow() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(9);
        textInput.selectNextWord();
        assertEquals(11, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 11), textInput.getSelection());
    }

    @Test public void selectNextWord_toTheEnd() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(16);
        textInput.selectNextWord();
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(16, textInput.getAnchor());
        assertEquals(new IndexRange(16, 19), textInput.getSelection());
    }

    @Test public void selectEndOfNextWordWithNoText() {
        textInput.selectEndOfNextWord();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void selectEndOfNextWordWithSelection_caretPositionBeforeAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 4);
        textInput.selectEndOfNextWord();
        assertEquals(9, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 9), textInput.getSelection());
    }

    @Test public void selectEndOfNextWordWithSelection_caretPositionBeforeAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 2);
        textInput.selectEndOfNextWord();
        assertEquals(3, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(3, 9), textInput.getSelection());
    }

    @Test public void selectEndOfNextWordWithSelection_caretPositionAfterAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.selectEndOfNextWord();
        assertEquals(15, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 15), textInput.getSelection());
    }

    @Test public void selectEndOfNextWordWithSelection_caretPositionAfterAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(5, 11);
        textInput.selectEndOfNextWord();
        assertEquals(15, textInput.getCaretPosition());
        assertEquals(5, textInput.getAnchor());
        assertEquals(new IndexRange(5, 15), textInput.getSelection());
    }

    @Test public void selectEndOfNextWord_caretWithinAWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(6);
        textInput.selectEndOfNextWord();
        assertEquals(9, textInput.getCaretPosition());
        assertEquals(6, textInput.getAnchor());
        assertEquals(new IndexRange(6, 9), textInput.getSelection());
    }

    @Test public void selectEndOfNextWord_caretAfterWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(9);
        textInput.selectEndOfNextWord();
        assertEquals(15, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 15), textInput.getSelection());
    }

    @Test public void selectEndOfNextWord_caretBeforeWord() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(4);
        textInput.selectEndOfNextWord();
        assertEquals(9, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 9), textInput.getSelection());
    }

    @Test public void selectEndOfNextWord_caretWithinWhitespace() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(10);
        textInput.selectEndOfNextWord();
        assertEquals(16, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 16), textInput.getSelection());
    }

    @Test public void selectEndOfNextWord_multipleWhitespaceInARow() {
        textInput.setText("The quick  brown fox");
        textInput.positionCaret(9);
        textInput.selectEndOfNextWord();
        assertEquals(16, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 16), textInput.getSelection());
    }

    @Test public void selectAllWithNoText() {
        textInput.setText("");
        textInput.selectAll();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void selectAllWithText_caretPositionIsAlwaysAtTheEnd() {
        textInput.setText("The quick brown fox");
        textInput.selectAll();
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 19), textInput.getSelection());
    }

    @Test public void homeClearsSelection() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.home();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void endClearsSelection() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.end();
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(19, textInput.getAnchor());
        assertEquals(new IndexRange(19, 19), textInput.getSelection());
    }

    @Test public void selectHomeHasNoEffectWhenCaretPositionIsAtZero() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(3, 0);
        textInput.selectHome();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(3, textInput.getAnchor());
        assertEquals(new IndexRange(0, 3), textInput.getSelection());
    }

    @Test public void selectHomeMovesCaretPositionToZero_CaretPositionRightOfAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.selectHome();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(0, 4), textInput.getSelection());
    }

    @Test public void selectHomeMovesCaretPositionToZero_CaretPositionEqualsAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(3, 3);
        textInput.selectHome();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(3, textInput.getAnchor());
        assertEquals(new IndexRange(0, 3), textInput.getSelection());
    }

    @Test public void selectHomeMovesCaretPositionToZero_CaretPositionLeftOfAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(6, 3);
        textInput.selectHome();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(6, textInput.getAnchor());
        assertEquals(new IndexRange(0, 6), textInput.getSelection());
    }

    @Test public void selectEndHasNoEffectWhenCaretPositionIsAtLength() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(3, 19);
        textInput.selectEnd();
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(3, textInput.getAnchor());
        assertEquals(new IndexRange(3, 19), textInput.getSelection());
    }

    @Test public void selectEndMovesCaretPositionToLength_CaretPositionRightOfAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(0, 3);
        textInput.selectEnd();
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 19), textInput.getSelection());
    }

    @Test public void selectEndMovesCaretPositionToLength_CaretPositionEqualsAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(3, 3);
        textInput.selectEnd();
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(3, textInput.getAnchor());
        assertEquals(new IndexRange(3, 19), textInput.getSelection());
    }

    @Test public void selectEndMovesCaretPositionToLength_CaretPositionLeftOfAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(6, 3);
        textInput.selectEnd();
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(6, textInput.getAnchor());
        assertEquals(new IndexRange(6, 19), textInput.getSelection());
    }

    @Test public void deletePreviousCharDeletesOnlySelectedText_anchorLessThanCaretPosition() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 10);
        textInput.deletePreviousChar();
        assertEquals("The brown fox", textInput.getText());
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void deletePreviousCharDeletesOnlySelectedText_caretPositionLessThanAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 4);
        textInput.deletePreviousChar();
        assertEquals("The brown fox", textInput.getText());
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void deletePreviousCharDeletesPreviousCharWhenCaretPositionEqualsAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 10);
        textInput.deletePreviousChar();
        assertEquals("The quickbrown fox", textInput.getText());
        assertEquals(9, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 9), textInput.getSelection());
    }

    @Test public void deletePreviousCharDoesNothingWhenSelectionIs0_0() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(0, 0);
        textInput.deletePreviousChar();
        assertEquals("The quick brown fox", textInput.getText());
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void deleteNextCharDeletesOnlySelectedText_anchorLessThanCaretPosition() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 10);
        textInput.deleteNextChar();
        assertEquals("The brown fox", textInput.getText());
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void deleteNextCharDeletesOnlySelectedText_caretPositionLessThanAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 4);
        textInput.deleteNextChar();
        assertEquals("The brown fox", textInput.getText());
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void deleteNextCharDeletesNextCharWhenCaretPositionEqualsAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 10);
        textInput.deleteNextChar();
        assertEquals("The quick rown fox", textInput.getText());
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void deleteNextCharDoesNothingWhenSelectionIsEmptyAtEnd() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(19, 19);
        textInput.deleteNextChar();
        assertEquals("The quick brown fox", textInput.getText());
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(19, textInput.getAnchor());
        assertEquals(new IndexRange(19, 19), textInput.getSelection());
    }

    @Test public void forwardSkipsSelection() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.forward();
        assertEquals(9, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 9), textInput.getSelection());
    }

    @Test public void forwardSkipsSelection2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 4);
        textInput.forward();
        assertEquals(9, textInput.getCaretPosition());
        assertEquals(9, textInput.getAnchor());
        assertEquals(new IndexRange(9, 9), textInput.getSelection());
    }

    @Test public void forwardMovesForwardWhenNotAtEnd() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(0, 0);
        textInput.forward();
        assertEquals(1, textInput.getCaretPosition());
        assertEquals(1, textInput.getAnchor());
        assertEquals(new IndexRange(1, 1), textInput.getSelection());
    }

    @Test public void forwardDoesNothingWhenAtEnd() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(19, 19);
        textInput.forward();
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(19, textInput.getAnchor());
        assertEquals(new IndexRange(19, 19), textInput.getSelection());
    }

    @Test public void backwardSkipsSelection() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 9);
        textInput.backward();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void backwardSkipsSelection2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(9, 4);
        textInput.backward();
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void backwardMovesBackwardWhenNotAtStart() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(14);
        textInput.backward();
        assertEquals(13, textInput.getCaretPosition());
        assertEquals(13, textInput.getAnchor());
        assertEquals(new IndexRange(13, 13), textInput.getSelection());
    }

    @Test public void backwardDoesNothingWhenAtStart() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(0, 0);
        textInput.backward();
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void positionCaretAtStart() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(0);
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void positionCaretInMiddle() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(10);
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void positionCaretAtEnd() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(19);
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(19, textInput.getAnchor());
        assertEquals(new IndexRange(19, 19), textInput.getSelection());
    }

    @Test public void positionCaretBeyondStartClamps() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(-10);
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void positionCaretBeyondEndClamps() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(1000);
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(19, textInput.getAnchor());
        assertEquals(new IndexRange(19, 19), textInput.getSelection());
    }

    @Test public void selectPositionCaretWhenAnchorAndCaretAreBothZero() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(0, 0);
        textInput.selectPositionCaret(10);
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 10), textInput.getSelection());
    }

    @Test public void selectPositionCaret_anchorLessThanCaretPosition() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 10);
        textInput.selectPositionCaret(1);
        assertEquals(1, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(1, 4), textInput.getSelection());
    }

    @Test public void selectPositionCaret_anchorLessThanCaretPosition2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 10);
        textInput.selectPositionCaret(15);
        assertEquals(15, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 15), textInput.getSelection());
    }

    @Test public void selectPositionCaret_anchorLessThanCaretPosition3() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(4, 10);
        textInput.selectPositionCaret(4);
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(4, 4), textInput.getSelection());
    }

    @Test public void selectPositionCaret_caretPositionLessThanAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 4);
        textInput.selectPositionCaret(1);
        assertEquals(1, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(1, 10), textInput.getSelection());
    }

    @Test public void selectPositionCaret_caretPositionLessThanAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 4);
        textInput.selectPositionCaret(14);
        assertEquals(14, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 14), textInput.getSelection());
    }

    @Test public void selectPositionCaret_caretPositionLessThanAnchor3() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 4);
        textInput.selectPositionCaret(10);
        assertEquals(10, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 10), textInput.getSelection());
    }

    @Test public void selectPositionCaretWhenCaretPositionEqualsAnchor() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 10);
        textInput.selectPositionCaret(4);
        assertEquals(4, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(4, 10), textInput.getSelection());
    }

    @Test public void selectPositionCaretWhenCaretPositionEqualsAnchor2() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(10, 10);
        textInput.selectPositionCaret(14);
        assertEquals(14, textInput.getCaretPosition());
        assertEquals(10, textInput.getAnchor());
        assertEquals(new IndexRange(10, 14), textInput.getSelection());
    }

    @Test public void extendSelectionWithNoText() {
        textInput.extendSelection(0);
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 0), textInput.getSelection());
    }

    @Test public void extendSelectionWithOutOfRangePos() {
        textInput.setText("The quick brown fox");
        textInput.selectRange(0, 0);
        textInput.extendSelection(1000);
        assertEquals(19, textInput.getCaretPosition());
        assertEquals(0, textInput.getAnchor());
        assertEquals(new IndexRange(0, 19), textInput.getSelection());
    }

    @Test public void extendSelectionWithOutOfRangePos2() {
        textInput.setText("The quick brown fox");
        textInput.positionCaret(4);
        textInput.extendSelection(-19);
        assertEquals(0, textInput.getCaretPosition());
        assertEquals(4, textInput.getAnchor());
        assertEquals(new IndexRange(0, 4), textInput.getSelection());
    }

    @Test public void test_rt26250_caret_issue_for_thai_characters() {
        // Thai string containing two characters, consisting of three
        // codepoints each.
        String thaiStr = "\u0E17\u0E35\u0E48\u0E17\u0E35\u0E48";
        textInput.setText(thaiStr);
        textInput.positionCaret(0);

        // Step past one character
        textInput.forward();
        assertEquals(3, textInput.getCaretPosition());

        // Goto beginning
        textInput.backward();
        assertEquals(0, textInput.getCaretPosition());

        // Delete entire first character forwards
        textInput.deleteNextChar();
        assertEquals("\u0E17\u0E35\u0E48", textInput.getText());

        // Break up and delete remaining character backwards in three steps
        textInput.forward();
        textInput.deletePreviousChar();
        assertEquals("\u0E17\u0E35", textInput.getText());
        textInput.deletePreviousChar();
        assertEquals("\u0E17", textInput.getText());
        textInput.deletePreviousChar();
        assertEquals("", textInput.getText());
    }

    @Test public void test_rt40376_delete_next_when_text_is_null() {
        textInput.setText(null);
        textInput.deleteNextChar();
    }

    @Test public void test_jdk_8171229_replaceText() {
        textInput.setText("");
        assertEquals("", textInput.getText());

        textInput.replaceText(0, 0, "a");
        assertEquals("a", textInput.getText());

        textInput.replaceText(1, 1, "b");
        assertEquals("ab", textInput.getText());

        textInput.replaceText(2, 2, "c");
        assertEquals("abc", textInput.getText());

        textInput.replaceText(3, 3, "d");
        assertEquals("abcd", textInput.getText());

        textInput.replaceText(3, 4, "efg");
        assertEquals("abcefg", textInput.getText());

        textInput.replaceText(3, 6, "d");
        assertEquals("abcd", textInput.getText());

        textInput.replaceText(0, 4, "");
        assertEquals("", textInput.getText());

        textInput.undo();
        assertEquals("abcd", textInput.getText());

        textInput.undo();
        assertEquals("abcefg", textInput.getText());

        textInput.undo();
        assertEquals("abcd", textInput.getText());

        textInput.undo();
        assertEquals("", textInput.getText());
    }

    @Test public void test_redo_replaceText_selectionShortening() {
        textInput.setText("0123456789");
        assertEquals("0123456789", textInput.getText());

        textInput.replaceText(8, 10, "x");
        assertEquals("01234567x", textInput.getText());

        textInput.undo();
        assertEquals("0123456789", textInput.getText());

        textInput.redo();
        assertEquals("01234567x", textInput.getText());
    }

    @Test public void replaceSelectionAtEndWithListener() {
        StringBuilder selectedTextLog = new StringBuilder();
        StringBuilder selectionLog = new StringBuilder();
        textInput.setText("x xxx");
        textInput.selectRange(2, 5);
        textInput.selectedTextProperty().addListener((observable, oldValue, newValue) -> selectedTextLog.append("|" + newValue));
        textInput.selectionProperty().addListener((observable, oldValue, newValue) -> selectionLog.append("|" + newValue.getStart() + "," + newValue.getEnd()));
        textInput.replaceSelection("a");
        assertEquals("|", selectedTextLog.toString());
        assertEquals("|3,3", selectionLog.toString());
        assertEquals("x a", textInput.getText());
    }

    @Test public void testSelectionProperties() {
        textInput.setText("abcdefghij");

        StringBuilder selectedTextLog = new StringBuilder();
        StringBuilder selectionLog = new StringBuilder();
        StringBuilder textLog = new StringBuilder();
        textInput.selectedTextProperty().addListener((observable, oldValue, newValue) -> selectedTextLog.append("|" + newValue));
        textInput.selectionProperty().addListener((observable, oldValue, newValue) -> selectionLog.append("|" + newValue.getStart() + "," + newValue.getEnd()));
        textInput.textProperty().addListener((observable, oldValue, newValue) -> textLog.append("|" + newValue));

        textInput.selectRange(3, 6);
        assertEquals("|def", selectedTextLog.toString());
        assertEquals("|3,6", selectionLog.toString());
        assertEquals("", textLog.toString());

        textInput.replaceSelection("xyz");
        assertEquals("|def|", selectedTextLog.toString());
        assertEquals("|3,6|6,6", selectionLog.toString());
        assertEquals("|abcxyzghij", textLog.toString());

        textInput.undo();
        assertEquals("|def||def", selectedTextLog.toString());
        assertEquals("|3,6|6,6|3,6", selectionLog.toString());
        assertEquals("|abcxyzghij|abcdefghij", textLog.toString());

        textInput.redo();
        assertEquals("|def||def|", selectedTextLog.toString());
        assertEquals("|3,6|6,6|3,6|6,6", selectionLog.toString());
        assertEquals("|abcxyzghij|abcdefghij|abcxyzghij", textLog.toString());
    }

    // Test for JDK-8178418
    @Test public void UndoRedoSpaceSequence() {
        Toolkit tk = (StubToolkit)Toolkit.getToolkit();
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        String text = "123456789";
        String tempText = "";

        textInput.setText(text);
        stage.setScene(scene);
        root.getChildren().removeAll();
        root.getChildren().add(textInput);
        stage.show();
        tk.firePulse();

        KeyEventFirer keyboard = new KeyEventFirer(textInput);

        // Test sequence of spaces
        keyboard.doKeyPress(KeyCode.HOME);
        tk.firePulse();
        for (int i = 0; i < 10; ++i) {
            keyboard.doKeyTyped(KeyCode.SPACE);
            tk.firePulse();
            tempText += " ";
        }
        assertTrue(textInput.getText().equals(tempText + text));

        textInput.undo();
        assertTrue(textInput.getText().equals(text));

        textInput.redo();
        assertTrue(textInput.getText().equals(tempText + text));

        root.getChildren().removeAll();
        stage.hide();
        tk.firePulse();
    }

    // Test for JDK-8178418
    @Test public void UndoRedoReverseSpaceSequence() {
        Toolkit tk = (StubToolkit)Toolkit.getToolkit();
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        String text = "123456789";
        String tempText = "";

        textInput.setText(text);
        stage.setScene(scene);
        root.getChildren().removeAll();
        root.getChildren().add(textInput);
        stage.show();
        tk.firePulse();

        KeyEventFirer keyboard = new KeyEventFirer(textInput);
        // Test reverse sequence of spaces
        keyboard.doKeyPress(KeyCode.HOME);
        tk.firePulse();
        for (int i = 0; i < 10; ++i) {
            keyboard.doKeyTyped(KeyCode.SPACE);
            keyboard.doKeyPress(KeyCode.LEFT);
            tk.firePulse();
            tempText += " ";
            assertTrue(textInput.getText().equals(tempText + text));
        }

        for (int i = 0; i < 10; ++i) {
            textInput.undo();
            tk.firePulse();
        }
        assertTrue(textInput.getText().equals(text));

        tempText = "";
        for (int i = 0; i < 10; ++i) {
            textInput.redo();
            tk.firePulse();
            tempText += " ";
            assertTrue(textInput.getText().equals(tempText + text));
        }

        root.getChildren().removeAll();
        stage.hide();
        tk.firePulse();
    }

    // Test for JDK-8178418
    @Test public void UndoRedoWords() {
        Toolkit tk = (StubToolkit)Toolkit.getToolkit();
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        String text = "123456789";
        String tempText = "";

        textInput.setText(text);
        stage.setScene(scene);
        root.getChildren().removeAll();
        root.getChildren().add(textInput);
        stage.show();
        tk.firePulse();

        KeyEventFirer keyboard = new KeyEventFirer(textInput);

        // Test words separated by space
        keyboard.doKeyPress(KeyCode.HOME);
        tk.firePulse();
        for (int i = 0; i < 10; ++i) {
            keyboard.doKeyTyped(KeyCode.SPACE);
            keyboard.doKeyTyped(KeyCode.A);
            keyboard.doKeyTyped(KeyCode.B);
            tk.firePulse();
            tempText += " AB";
            assertTrue(textInput.getText().equals(tempText + text));
        }

        for (int i = 0; i < 10; ++i) {
            textInput.undo();
            tk.firePulse();
        }
        assertTrue(textInput.getText().equals(text));

        tempText = "";
        for (int i = 0; i < 10; ++i) {
            textInput.redo();
            tk.firePulse();
            tempText += " AB";
            assertTrue(textInput.getText().equals(tempText + text));
        }

        root.getChildren().removeAll();
        stage.hide();
        tk.firePulse();
    }

    // Test for JDK-8178418
    @Test public void UndoRedoTimestampBased() {
        Toolkit tk = (StubToolkit)Toolkit.getToolkit();
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        String text = "123456789";
        String tempText = "";

        textInput.setText(text);
        stage.setScene(scene);
        root.getChildren().removeAll();
        root.getChildren().add(textInput);
        stage.show();
        tk.firePulse();

        KeyEventFirer keyboard = new KeyEventFirer(textInput);

        // Test continuos sequence of characters.
        // In this case an undo-redo record is added after 2500 milliseconds.
        keyboard.doKeyPress(KeyCode.HOME);
        tk.firePulse();

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 4000) {

            keyboard.doKeyTyped(KeyCode.A);
            tk.firePulse();
            tempText += "A";
            assertTrue(textInput.getText().equals(tempText + text));
        }

        textInput.undo();
        assertFalse(textInput.getText().equals(text));
        textInput.undo();
        tk.firePulse();
        assertTrue(textInput.getText().equals(text));

        root.getChildren().removeAll();
        stage.hide();
        tk.firePulse();
    }

    // TODO tests for Content firing event notification properly

    // TODO tests for Content not allowing illegal characters

    private void copy(String string) {
        ClipboardContent content = new ClipboardContent();
        content.putString(string);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
