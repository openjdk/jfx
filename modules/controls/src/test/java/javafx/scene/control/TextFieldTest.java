/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import com.sun.javafx.scene.control.infrastructure.StageLoader;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;
import static org.junit.Assert.*;

public class TextFieldTest {
    private TextField txtField;//Empty string
    private TextField dummyTxtField;//With string value
    
    @Before public void setup() {
        txtField = new TextField();
        dummyTxtField = new TextField("dummy");
    }
    
    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    
    @Test public void defaultConstructorShouldHaveEmptyString() {
        assertEquals("", txtField.getText());
    }
    
    @Test public void oneStrArgConstructorShouldHaveString() {
        assertEquals("dummy", dummyTxtField.getText());
    }

    /*********************************************************************
     * Tests for the null checks                                         *
     ********************************************************************/
    
    @Test public void checkContentNotNull() {
        assertNotNull(txtField.getContent());
    }
    
    @Test public void checkCharNotNull() {
        assertNotNull(txtField.getCharacters());
    }

    @Test public void checkDefPromptTextEmptyString() {
        assertEquals("", txtField.getPromptText());
    }
    
    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/
    @Test public void checkDefaultColCount() {
        assertEquals(TextField.DEFAULT_PREF_COLUMN_COUNT, 12);
    }

    @Test public void defaultActionHandlerIsNotDefined() {
        assertNull(txtField.getOnAction());
    }

    @Test public void defaultConstructorShouldSetStyleClassTo_textfield() {
        assertStyleClassContains(txtField, "text-field");
    }
    
    @Test public void checkCharsSameAsText() {
        assertEquals(dummyTxtField.getCharacters().toString(), dummyTxtField.getText());
    }

    @Test public void checkCharsSameAsContent() {
        assertEquals(dummyTxtField.getCharacters().toString(), dummyTxtField.getContent().get(0, dummyTxtField.getLength()).toString());
    }

    @Test public void checkTextSameAsContent() {
        assertEquals(dummyTxtField.getText(), dummyTxtField.getContent().get(0, dummyTxtField.getLength()));
    }

    @Test public void checkPromptTextPropertyName() {
        assertTrue(txtField.promptTextProperty().getName().equals("promptText"));
    }
    
    @Test public void prefColCountCannotBeNegative() {
        try {
            txtField.setPrefColumnCount(-1);
            fail("Prefcoulumn count cannot be null");//This is non reachable ode if everything goes fine(i.e Exception is thrown)
        } catch(IllegalArgumentException iae) {
            assertNotNull(iae);
        }
    }


    @Test public void oneArgStrConstructorShouldSetStyleClassTo_textfield() {
        assertStyleClassContains(dummyTxtField, "text-field");
    }
    
    @Test public void checkTextSetGet() {
        dummyTxtField.setText("junk");
        assertEquals(dummyTxtField.getText(), "junk");
    }

    /*********************************************************************
     * Tests for CSS                                                     *
     ********************************************************************/

    @Test public void prefColumnCountSetFromCSS() {
        txtField.setStyle("-fx-pref-column-count: 100");
        Scene s = new Scene(txtField);
        txtField.impl_processCSS(true);
        assertEquals(100.0, txtField.getPrefColumnCount(), 0);
    }

    @Test public void pseudoClassState_isReadOnly() {
        new StageLoader(txtField);
        txtField.impl_processCSS(true);

        txtField.setEditable(false);
        ObservableSet<PseudoClass> pcSet = txtField.getPseudoClassStates();
        boolean match = false;
        for (PseudoClass pc : pcSet) {
            if (match) break;
            match = "readonly".equals(pc.getPseudoClassName());
        }
        assertTrue(match);
    }

    @Test public void pseudoClassState_isNotReadOnly() {
        new StageLoader(txtField);
        txtField.impl_processCSS(true);

        txtField.setEditable(true);
        ObservableSet<PseudoClass> pcSet = txtField.getPseudoClassStates();
        boolean match = false;
        for (PseudoClass pc : pcSet) {
            if (match) break;
            match = "readonly".equals(pc.getPseudoClassName());
        }
        assertFalse(match);
    }

    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void checkPromptTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        txtField.promptTextProperty().bind(strPr);
        assertTrue("PromptText cannot be bound", txtField.getPromptText().equals("value"));
        strPr.setValue("newvalue");
        assertTrue("PromptText cannot be bound", txtField.getPromptText().equals("newvalue"));
    }
    
    @Ignore("TODO: Please remove ignore annotation after RT-15799 is fixed.")
    @Test public void checkTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        txtField.textProperty().bind(strPr);
        assertEquals("Text cannot be bound", txtField.getText(), "value");
        strPr.setValue("newvalue");
        assertEquals("Text cannot be bound", txtField.getText(),  "newvalue");
    }
    
    @Test public void checkOnActionPropertyBind() {
        ObjectProperty<EventHandler<ActionEvent>> op= new SimpleObjectProperty<EventHandler<ActionEvent>>();
        EventHandler<ActionEvent> ev = event -> {
            //Nothing to do
        };
        op.setValue(ev);
        txtField.onActionProperty().bind(op);
        assertEquals(ev, op.getValue());
    }
    /*********************************************************************
     * Miscellaneous Tests                                               *
     ********************************************************************/
    @Test public void lengthMatchesStringLengthExcludingControlCharacters() {
        final String string = "Hello\n";
        txtField.setText(string);
        assertEquals(string.length()-1, txtField.getLength());
    }

    @Test public void prefColumnCountPropertyHasBeanReference() {
        assertSame(txtField, txtField.prefColumnCountProperty().getBean());
    }

    @Test public void prefColumnCountPropertyHasName() {
        assertEquals("prefColumnCount", txtField.prefColumnCountProperty().getName());
    }

    @Test public void onActionPropertyHasBeanReference() {
        assertSame(txtField, txtField.onActionProperty().getBean());
    }

    @Test public void onActionPropertyHasName() {
        assertEquals("onAction", txtField.onActionProperty().getName());
    }
    
    @Test public void setPromptTextAndSeeValueIsReflectedInModel() {
        txtField.setPromptText("tmp");
        assertEquals(txtField.promptTextProperty().getValue(), "tmp");
    }
    
    @Test public void setPromptTextAndSeeValue() {
        txtField.setPromptText("tmp");
        assertEquals(txtField.getPromptText(), "tmp");
    }
    
    @Test public void setTextAndSeeValueIsReflectedInModel() {
        txtField.setText("tmp");
        assertEquals(txtField.textProperty().getValue(), txtField.getText());
    }

    @Test public void setTextAndSeeValue() {
        txtField.setText("tmp");
        assertEquals(txtField.getText() , "tmp");
    }

    @Test public void setPrefColCountAndSeeValueIsReflectedInModel() {
        txtField.setPrefColumnCount(10);
        assertEquals(txtField.prefColumnCountProperty().get(), 10.0, 0.0);
    }

    @Test public void setPrefColCountAndSeeValue() {
        txtField.setPrefColumnCount(10);
        assertEquals(txtField.getPrefColumnCount(), 10.0 ,0.0);
    }

    @Test public void insertAndCheckSubRangeInText() {
        dummyTxtField.getContent().insert(0, "x", true);
        assertEquals("x", dummyTxtField.getText().substring(0,1));
    }

    @Test public void insertAndCheckSubRangeInContent() {
        dummyTxtField.getContent().insert(0, "x", true);
        assertEquals("x", dummyTxtField.getContent().get(0, 1));
    }

    @Test public void appendAndCheckSubRangeInText() {
        dummyTxtField.appendText("x");
        assertEquals("x", dummyTxtField.getText().substring(dummyTxtField.getLength() - 1,dummyTxtField.getLength()));
    }

    @Test public void appendAndCheckSubRangeInContent() {
        dummyTxtField.appendText("x");
        assertEquals("x", dummyTxtField.getContent().get(dummyTxtField.getLength() - 1,dummyTxtField.getLength()));
    }

    @Test public void deleteAndCheckText() {
        dummyTxtField.getContent().insert(0, "x", false);
        dummyTxtField.getContent().delete(1, dummyTxtField.getLength(), true);
        assertEquals("x", dummyTxtField.getText());
    }

    

}
