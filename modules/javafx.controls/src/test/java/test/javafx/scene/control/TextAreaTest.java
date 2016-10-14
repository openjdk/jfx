/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControlShim;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;
import static org.junit.Assert.*;

/**
 *
 * @author srikalyc
 */
public class TextAreaTest {
    private TextArea txtArea;//Empty string
    private TextArea dummyTxtArea;//With string value

    @Before public void setup() {
        txtArea = new TextArea();
        dummyTxtArea = new TextArea("dummy");
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void defaultConstructorShouldHaveEmptyString() {
        assertEquals("", txtArea.getText());
    }

    @Test public void oneStrArgConstructorShouldHaveString() {
        assertEquals("dummy", dummyTxtArea.getText());
    }

    /*********************************************************************
     * Tests for the null checks                                         *
     ********************************************************************/

    @Test public void checkContentNotNull() {
        assertNotNull(TextInputControlShim.getContent(txtArea));
    }

    @Test public void checkDefPromptTextEmptyString() {
        assertEquals("", txtArea.getPromptText());
    }

    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/
    @Test public void checkDefaultColCount() {
        assertEquals(TextArea.DEFAULT_PREF_COLUMN_COUNT, 40);
    }

    @Test public void checkDefaultRowCount() {
        assertEquals(TextArea.DEFAULT_PREF_ROW_COUNT, 10);
    }

    @Test public void checkDefaultWrapText() {
        assertFalse(txtArea.isWrapText());
    }

    @Test public void defaultConstructorShouldSetStyleClassTo_textarea() {
        assertStyleClassContains(txtArea, "text-area");
    }

    @Test public void defaultParagraphListNotNull() {
        assertNotNull(dummyTxtArea.getParagraphs());
    }

    @Test public void checkTextSameAsContent() {
        assertEquals(dummyTxtArea.getText(), TextInputControlShim.getContent_get(dummyTxtArea, 0, dummyTxtArea.getLength()));
    }

    @Test public void checkPromptTextPropertyName() {
        assertTrue(txtArea.promptTextProperty().getName().equals("promptText"));
    }

    @Test public void prefColCountCannotBeNegative() {
        try {
            txtArea.setPrefColumnCount(-1);
            fail("Prefcoulumn count cannot be null");//This is non reachable ode if everything goes fine(i.e Exception is thrown)
        } catch(IllegalArgumentException iae) {
            assertNotNull(iae);
        }
    }

    @Test public void prefRowCountCannotBeNegative() {
        try {
            txtArea.setPrefRowCount(-1);
            fail("Prefrow count cannot be null");//This is non reachable ode if everything goes fine(i.e Exception is thrown)
        } catch(IllegalArgumentException iae) {
            assertNotNull(iae);
        }
    }

    @Test public void oneArgStrConstructorShouldSetStyleClassTo_textarea() {
        assertStyleClassContains(dummyTxtArea, "text-area");
    }

    @Test public void checkTextSetGet() {
        dummyTxtArea.setText("junk");
        assertEquals(dummyTxtArea.getText(), "junk");
    }

    /*********************************************************************
     * Tests for CSS                                                     *
     ********************************************************************/

    @Test public void wrapTextSetFromCSS() {
        txtArea.setStyle("-fx-wrap-text: true");
        Scene s = new Scene(txtArea);
        txtArea.applyCss();
        assertTrue(txtArea.isWrapText());
    }

    @Test public void prefColumnCountSetFromCSS() {
        txtArea.setStyle("-fx-pref-column-count: 100");
        Scene s = new Scene(txtArea);
        txtArea.applyCss();
        assertEquals(100, txtArea.getPrefColumnCount());
    }

    @Test public void prefRowCountSetFromCSS() {
        txtArea.setStyle("-fx-pref-row-count: 100");
        Scene s = new Scene(txtArea);
        txtArea.applyCss();
        assertEquals(100, txtArea.getPrefRowCount());
    }

    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void checkPromptTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        txtArea.promptTextProperty().bind(strPr);
        assertTrue("PromptText cannot be bound", txtArea.getPromptText().equals("value"));
        strPr.setValue("newvalue");
        assertTrue("PromptText cannot be bound", txtArea.getPromptText().equals("newvalue"));
    }

    @Ignore("TODO: Please remove ignore annotation after RT-15799 is fixed.")
    @Test public void checkTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        txtArea.textProperty().bind(strPr);
        assertEquals("Text cannot be bound", txtArea.getText(), "value");
        strPr.setValue("newvalue");
        assertEquals("Text cannot be bound", txtArea.getText(),  "newvalue");
    }

    @Test public void checkScrollLeftPropertyBind() {
        DoubleProperty dbPr = new SimpleDoubleProperty(200.0);
        txtArea.scrollLeftProperty().bind(dbPr);
        assertEquals(txtArea.getScrollLeft(), 200.0, 0.0);
        dbPr.setValue(300.0);
        assertEquals(txtArea.getScrollLeft(), 300.0, 0.0);
    }

    @Test public void checkScrollTopPropertyBind() {
        DoubleProperty dbPr = new SimpleDoubleProperty(200.0);
        txtArea.scrollTopProperty().bind(dbPr);
        assertEquals(txtArea.getScrollTop(), 200.0, 0.0);
        dbPr.setValue(300.0);
        assertEquals(txtArea.getScrollTop(), 300.0, 0.0);
    }

    @Test public void checkPrefColumnPropertyBind() {
        DoubleProperty dbPr = new SimpleDoubleProperty(200.0);
        txtArea.prefColumnCountProperty().bind(dbPr);
        assertEquals(txtArea.getPrefColumnCount(), 200.0, 0.0);
        dbPr.setValue(300.0);
        assertEquals(txtArea.getPrefColumnCount(), 300.0, 0.0);
    }

    @Test public void checkPrefRowPropertyBind() {
        DoubleProperty dbPr = new SimpleDoubleProperty(200.0);
        txtArea.prefRowCountProperty().bind(dbPr);
        assertEquals(txtArea.getPrefRowCount(), 200.0, 0.0);
        dbPr.setValue(300.0);
        assertEquals(txtArea.getPrefRowCount(), 300.0, 0.0);
    }

    @Test public void checkWrapTextPropertyBind() {
        BooleanProperty boolPr = new SimpleBooleanProperty(true);
        txtArea.wrapTextProperty().bind(boolPr);
        assertTrue(txtArea.isWrapText());
        boolPr.setValue(false);
        assertFalse(txtArea.isWrapText());
    }

    /*********************************************************************
     * Miscellaneous Tests                                               *
     ********************************************************************/
    @Test public void scrollTopValueOnSetText() {
        txtArea.setText("sample");
        assertEquals(0.0, txtArea.getScrollTop(), 0.0);
    }

    @Test public void scrollLeftValueOnSetText() {
        txtArea.setText("sample");
        assertEquals(0.0, txtArea.getScrollLeft(), 0.0);
    }

    @Test public void prefColumnCountPropertyHasBeanReference() {
        assertSame(txtArea, txtArea.prefColumnCountProperty().getBean());
    }

    @Test public void prefColumnCountPropertyHasName() {
        assertEquals("prefColumnCount", txtArea.prefColumnCountProperty().getName());
    }

    @Test public void prefRowCountPropertyHasBeanReference() {
        assertSame(txtArea, txtArea.prefRowCountProperty().getBean());
    }

    @Test public void prefRowCountPropertyHasName() {
        assertEquals("prefRowCount", txtArea.prefRowCountProperty().getName());
    }

    @Test public void insertTextAtGreaterIndexValue() {
        try {
            dummyTxtArea.insertText(34, "sometext");
            fail("Able to insert at index greater than size of text. This is a bug!");
        } catch (IndexOutOfBoundsException iofb) {
            assertNotNull(iofb);
        }
    }

    @Test public void insertTextAtNegativeIndexValue() {
        try {
            dummyTxtArea.insertText(-1, "sometext");
            fail("Able to insert at negative index . This is a bug!");
        } catch (IndexOutOfBoundsException iofb) {
            assertNotNull(iofb);
        }
    }

    @Test public void insertNullTextValue() {
        try {
            dummyTxtArea.insertText(0, null);
            fail("Able to insert null at valid index location. This is a bug!");
        } catch (NullPointerException npe) {
            assertNotNull(npe);
        } catch (IllegalArgumentException iae) {
            assertNotNull(iae);
        }
    }

    @Test public void deleteNegativeStartIndexText() {
        try {
            dummyTxtArea.deleteText(-2, 2);
            fail("Able to delete negative start index text. This is a bug!");
        } catch (IndexOutOfBoundsException iobe) {
            assertNotNull(iobe);
        }
    }

    @Test public void deleteNegativeRangeOfText() {
        try {
            dummyTxtArea.deleteText(3, 2);
            fail("Able to delete negative range text. This is a bug!");
        }catch (IllegalArgumentException iae) {
            assertNotNull(iae);
        }
    }

    @Test public void deleteOutOfRangeEndIndexText() {
        try {
            dummyTxtArea.deleteText(0, 200);
            fail("Able to delete text out of range with very large end index. This is a bug!");
        } catch (IndexOutOfBoundsException iobe) {
            assertNotNull(iobe);
        }
    }

    @Test public void setPromptTextAndSeeValueIsReflectedInModel() {
        txtArea.setPromptText("tmp");
        assertEquals(txtArea.promptTextProperty().getValue(), "tmp");
    }

    @Test public void setPromptTextAndSeeValue() {
        txtArea.setPromptText("tmp");
        assertEquals(txtArea.getPromptText(), "tmp");
    }

    @Test public void setTextAndSeeValueIsReflectedInModel() {
        txtArea.setText("tmp");
        assertEquals(txtArea.textProperty().getValue(), "tmp");
    }

    @Test public void setTextAndSeeValue() {
        txtArea.setText("tmp");
        assertEquals(txtArea.getText(), "tmp");
    }

    @Test public void setPrefColCountAndSeeValueIsReflectedInModel() {
        txtArea.setPrefColumnCount(10);
        assertEquals(txtArea.prefColumnCountProperty().get(), 10.0, 0.0);
    }

    @Test public void setPrefColCountAndSeeValue() {
        txtArea.setPrefColumnCount(10);
        assertEquals(txtArea.getPrefColumnCount(), 10.0, 0.0);
    }

    @Test public void setPrefRowCountAndSeeValueIsReflectedInModel() {
        txtArea.setPrefRowCount(10);
        assertEquals(txtArea.prefRowCountProperty().get(), 10.0, 0.0);
    }

    @Test public void setPrefRowCountAndSeeValue() {
        txtArea.setPrefRowCount(10);
        assertEquals(txtArea.getPrefRowCount() , 10.0, 0.0);
    }

    @Test public void setScrollLeftAndSeeValueIsReflectedInModel() {
        txtArea.setScrollLeft(10.0);
        assertEquals(txtArea.scrollLeftProperty().get(), 10.0, 0.0);
    }

    @Test public void setScrollLeftAndSeeValue() {
        txtArea.setScrollLeft(10.0);
        assertEquals(txtArea.getScrollLeft(), 10.0, 0.0);
    }

    @Test public void setScrollTopAndSeeValueIsReflectedInModel() {
        txtArea.setScrollTop(10.0);
        assertEquals(txtArea.scrollTopProperty().get(), txtArea.getScrollTop(), 0.0);
    }

    @Test public void setScrollTopAndSeeValue() {
        txtArea.setScrollTop(10.0);
        assertEquals(txtArea.getScrollTop(), 10.0, 0.0);
    }

    @Test public void setWrapTextAndSeeValueIsReflectedInModel() {
        txtArea.setWrapText(true);
        assertTrue(txtArea.wrapTextProperty().getValue());
    }

    @Test public void setWrapTextAndSeeValue() {
        txtArea.setWrapText(true);
        assertTrue(txtArea.isWrapText());
    }

    @Test public void insertAndCheckSubRangeInText() {
        TextInputControlShim.getContent_insert(dummyTxtArea, 0, "x", true);
        assertEquals("x", dummyTxtArea.getText().substring(0,1));
    }

    @Test public void insertAndCheckSubRangeInContent() {
        TextInputControlShim.getContent_insert(dummyTxtArea, 0, "x", true);
        assertEquals("x", TextInputControlShim.getContent_get(dummyTxtArea, 0, 1));
    }

    @Test public void deleteAndCheckText() {
        TextInputControlShim.getContent_insert(dummyTxtArea, 0, "x", false);
        TextInputControlShim.getContent_delete(dummyTxtArea, 1, dummyTxtArea.getLength(), true);
        assertEquals("x", dummyTxtArea.getText());
    }

    @Test public void createTextThroughConstructorAndCheckParagraphCount() {
        dummyTxtArea = new TextArea("dummy\nanother");
        assertEquals(dummyTxtArea.getParagraphs().size(), 2.0, 0.0);
    }

    @Test public void createTextThroughConstructorAndCheckParagraphContents() {
        dummyTxtArea = new TextArea("dummy\nanother");
        assertEquals(dummyTxtArea.getParagraphs().get(0).toString(), "dummy");
        assertEquals(dummyTxtArea.getParagraphs().get(1).toString(), "another");
    }

    @Test public void appendNormalTextAndCheckText() {
        dummyTxtArea.appendText("another");
        assertEquals(dummyTxtArea.getText(), "dummyanother");
    }

    @Test public void appendNormalTextAndCheckParagraphCount() {
        dummyTxtArea.appendText("another");
        assertEquals(dummyTxtArea.getParagraphs().size(), 1.0, 0.0);
    }

    @Test public void addNormalTextAndCheckParagraphContents() {
        dummyTxtArea.appendText("another");
        assertEquals(dummyTxtArea.getParagraphs().get(0).toString(), "dummyanother");
    }

    @Test public void appendParaTextAndCheckParagraphCount() {
        assertEquals(dummyTxtArea.getParagraphs().size(), 1.0, 0.0);
        dummyTxtArea.appendText("\nanother");
        assertEquals(dummyTxtArea.getParagraphs().size(), 2.0, 0.0);
    }

    @Test public void addParaTextAndCheckParagraphContents() {
        dummyTxtArea.appendText("\nanother");
        assertEquals(dummyTxtArea.getParagraphs().get(0).toString(), "dummy");
        assertEquals(dummyTxtArea.getParagraphs().get(1).toString(), "another");
    }

    @Test public void insertNormalTextAndCheckText() {
        dummyTxtArea.insertText(0,"another");
        assertEquals(dummyTxtArea.getText(), "anotherdummy");
    }

    @Test public void insertNormalTextAndCheckParagraphCount() {
        dummyTxtArea.insertText(0,"another");
        assertEquals(dummyTxtArea.getParagraphs().size(), 1.0, 0.0);
    }

    @Test public void insertNormalTextAndCheckParagraphContents() {
        dummyTxtArea.insertText(0,"another");
        assertEquals(dummyTxtArea.getParagraphs().get(0).toString(), "anotherdummy");
    }

    @Test public void insertParaTextAndCheckParagraphCount() {
        assertEquals(dummyTxtArea.getParagraphs().size(), 1.0, 0.0);
        dummyTxtArea.insertText(0,"another\n");
        assertEquals(dummyTxtArea.getParagraphs().size(), 2.0, 0.0);
    }

    @Test public void insertParaTextAndCheckParagraphContents() {
        dummyTxtArea.insertText(0,"another\n");
        assertEquals(dummyTxtArea.getParagraphs().get(0).toString(), "another");
        assertEquals(dummyTxtArea.getParagraphs().get(1).toString(), "dummy");
    }

    @Test public void deleteNormalTextAndCheckParagraphCount() {
        dummyTxtArea.appendText("\nanother");
        dummyTxtArea.deleteText(0,5);//Retain the \n character
        assertEquals(dummyTxtArea.getParagraphs().size(), 2.0, 0.0);
    }

    @Test public void deleteNormalTextAndCheckParagraphContents() {
        dummyTxtArea.appendText("\nanother");
        dummyTxtArea.deleteText(0,5);//Retain the \n character
        assertEquals(dummyTxtArea.getParagraphs().get(0).toString(), "");
        assertEquals(dummyTxtArea.getParagraphs().get(1).toString(), "another");
    }

    @Test public void deleteParagraphAndCheckParagraphCount() {
        dummyTxtArea.appendText("\nanother");
        dummyTxtArea.deleteText(0,6);//This will delete a paragraph coz next line char is also deleted
        assertEquals(dummyTxtArea.getParagraphs().size(), 1.0, 0.0);
    }

    @Test public void deleteParagraphAndCheckParagraphContents() {
        dummyTxtArea.appendText("\nanother");
        dummyTxtArea.deleteText(0,6);
        assertEquals(dummyTxtArea.getParagraphs().get(0).toString(), "another");
    }
}
