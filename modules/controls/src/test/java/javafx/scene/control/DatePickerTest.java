/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.time.LocalDate;
import java.time.chrono.*;
import java.util.Set;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.util.Callback;
import javafx.util.StringConverter;

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;

import com.sun.javafx.scene.control.skin.DatePickerSkin;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class DatePickerTest {
    private DatePicker datePicker;
    private final LocalDate today = LocalDate.now();


    /*********************************************************************
     *                                                                   *
     * Utility methods                                                   *
     *                                                                   *
     ********************************************************************/

    public Node getDisplayNode() {
        return ((DatePickerSkin)datePicker.getSkin()).getDisplayNode();
    }



    /*********************************************************************
     *                                                                   *
     * Setup                                                             *
     *                                                                   *
     ********************************************************************/

    @Before public void setup() {
        datePicker = new DatePicker();
    }



    /*********************************************************************
     *                                                                   *
     * Tests for the constructors                                        *
     *                                                                   *
     ********************************************************************/

    @Test public void noArgConstructorSetsTheStyleClass() {
        assertStyleClassContains(datePicker, "date-picker");
    }

    @Test public void noArgConstructor_valueIsNull() {
        assertNull(datePicker.getValue());
    }

    @Test public void noArgConstructor_editableIsTrue() {
        assertTrue(datePicker.isEditable());
    }

    @Test public void noArgConstructor_showingIsFalse() {
        assertFalse(datePicker.isShowing());
    }

    @Test public void noArgConstructor_promptTextIsEmptyString() {
        assertEquals("", datePicker.getPromptText());
    }

    @Test public void noArgConstructor_armedIsFalse() {
        assertFalse(datePicker.isArmed());
    }

    @Test public void noArgConstructor_converterIsNotNull() {
        assertNotNull(datePicker.getConverter());
    }

    @Test public void noArgConstructor_chronologyIsNotNull() {
        assertNotNull(datePicker.getChronology());
    }

    @Test public void noArgConstructor_dayCellFactoryIsNull() {
        assertNull(datePicker.getDayCellFactory());
    }

    @Test public void singleArgConstructorSetsTheStyleClass() {
        final DatePicker b2 = new DatePicker(today);
        assertStyleClassContains(b2, "date-picker");
    }

    @Test public void singleArgConstructor_valueIsArg() {
        final DatePicker b2 = new DatePicker(today);
        assertEquals(b2.getValue(), today);
    }

    @Test public void singleArgConstructor_editableIsTrue() {
        final DatePicker b2 = new DatePicker(today);
        assertTrue(b2.isEditable());
    }

    @Test public void singleArgConstructor_showingIsFalse() {
        final DatePicker b2 = new DatePicker(today);
        assertFalse(b2.isShowing());
    }

    @Test public void singleArgConstructor_promptTextIsEmptyString() {
        final DatePicker b2 = new DatePicker(today);
        assertEquals("", b2.getPromptText());
    }

    @Test public void singleArgConstructor_armedIsFalse() {
        final DatePicker b2 = new DatePicker(today);
        assertEquals(false, b2.isArmed());
    }

    @Test public void singleArgConstructor_converterIsNotNull() {
        final DatePicker b2 = new DatePicker(today);
        assertNotNull(b2.getConverter());
    }

    @Test public void singleArgConstructor_chronologyIsNotNull() {
        final DatePicker b2 = new DatePicker(today);
        assertNotNull(b2.getChronology());
    }

    @Test public void singleArgConstructor_dayCellFactoryIsNull() {
        final DatePicker b2 = new DatePicker(today);
        assertNull(b2.getDayCellFactory());
    }


    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void checkPromptTextPropertyName() {
        assertTrue(datePicker.promptTextProperty().getName().equals("promptText"));
    }

    @Test public void checkValuePropertyName() {
        assertTrue(datePicker.valueProperty().getName().equals("value"));
    }

    @Test public void checkConverterPropertyName() {
        assertTrue(datePicker.converterProperty().getName().equals("converter"));
    }

    @Test public void checkChronologyPropertyName() {
        assertTrue(datePicker.chronologyProperty().getName().equals("chronology"));
    }

    @Test public void checkOnActionPropertyName() {
        assertTrue(datePicker.onActionProperty().getName().equals("onAction"));
    }

    @Test public void checkArmedPropertyName() {
        assertTrue(datePicker.armedProperty().getName().equals("armed"));
    }

    @Test public void checkShowingPropertyName() {
        assertTrue(datePicker.showingProperty().getName().equals("showing"));
    }

    @Test public void checkEditablePropertyName() {
        assertTrue(datePicker.editableProperty().getName().equals("editable"));
    }

    @Test public void checkDayCellFactoryPropertyName() {
        assertTrue(datePicker.dayCellFactoryProperty().getName().equals("dayCellFactory"));
    }

    @Test public void defaultActionHandlerIsNotDefined() {
        assertNull(datePicker.getOnAction());
    }

    @Test public void defaultConverterCanHandleLocalDateValues() {
        StringConverter<LocalDate> sc = datePicker.getConverter();
        String todayStr = sc.toString(today);
        assertTrue(todayStr.length() > 0);
        assertEquals(today, sc.fromString(todayStr));
    }

    @Test public void defaultConverterCanHandleNullValues() {
        StringConverter<LocalDate> sc = datePicker.getConverter();
        String str = sc.toString(null);

        assertEquals(null, sc.fromString(null));
        assertEquals(null, sc.fromString(""));
        assertTrue(str == null || str.isEmpty());
    }

    @Ignore("Fails due to RT-29927")
    @Test public void ensureImpl_getPseudoClassStateReturnsValidValue() {
        Set<PseudoClass> value1 = datePicker.getPseudoClassStates();
        assertFalse(datePicker.isEditable());
        assertTrue(value1.size() >= 0);

        datePicker.setEditable(true);
        Set<PseudoClass> value2 = datePicker.getPseudoClassStates();
        assertTrue(value2.contains(PseudoClass.getPseudoClass("editable")));

        datePicker.show();
        Set<PseudoClass> value3 = datePicker.getPseudoClassStates();
        assertTrue(value3.contains(PseudoClass.getPseudoClass("showing")));

        datePicker.arm();
        Set<PseudoClass> value4 = datePicker.getPseudoClassStates();
        assertTrue(value4.contains(PseudoClass.getPseudoClass("armed")));

        assertFalse(value1.equals(value2));
        assertFalse(value1.equals(value3));
        assertFalse(value1.equals(value4));
        assertFalse(value2.equals(value3));
        assertFalse(value2.equals(value4));
        assertFalse(value3.equals(value4));
    }

    /*********************************************************************
     * Tests for properties                                              *
     ********************************************************************/

    @Test public void ensureSettingNullChronologyRestoresDefault() {
        Chronology defaultChronology = datePicker.getChronology();
        Chronology otherChronology =
            (defaultChronology != IsoChronology.INSTANCE) ? IsoChronology.INSTANCE : JapaneseChronology.INSTANCE;
        datePicker.setChronology(otherChronology);
        assertEquals(otherChronology, datePicker.getChronology());
        datePicker.setChronology(null);
        assertEquals(defaultChronology, datePicker.getChronology());
    }

    @Test public void ensureSettingNullConverterRestoresDefault() {
        StringConverter<LocalDate> defaultConverter = datePicker.getConverter();
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override public String toString(LocalDate t) { return t.toString(); }
            @Override public LocalDate fromString(String string) { return today; }
        });
        assertNotSame(defaultConverter, datePicker.getConverter());
        datePicker.setConverter(null);
        assertSame(defaultConverter, datePicker.getConverter());
    }

    @Test public void ensureCanSetNonNullDayCellFactory() {
        Callback<DatePicker, DateCell> cf = new Callback<DatePicker, DateCell>() {
            @Override public DateCell call(DatePicker p) { return null; }
        };
        datePicker.setDayCellFactory(cf);
        assertSame(cf, datePicker.getDayCellFactory());
    }

    @Test public void ensureEditorIsNonNullWhenComboBoxIsNotEditable() {
        assertNotNull(datePicker.getEditor());
    }

    @Test public void ensureEditorIsNonNullWhenComboBoxIsEditable() {
        datePicker.setEditable(true);
        assertNotNull(datePicker.getEditor());
    }

    @Test public void ensureEditorDoesNotChangeWhenEditableToggles() {
        datePicker.setEditable(true);
        assertNotNull(datePicker.getEditor());
        datePicker.setEditable(false);
        assertNotNull(datePicker.getEditor());
        datePicker.setEditable(true);
        assertNotNull(datePicker.getEditor());
    }

    @Test public void ensureCanSetValueToNonNullLocalDateAndBackAgain() {
        datePicker.setValue(today);
        assertEquals(today, datePicker.getValue());
        datePicker.setValue(null);
        assertNull(datePicker.getValue());
    }

    @Test public void ensureCanToggleEditable() {
        datePicker.setEditable(true);
        assertTrue(datePicker.isEditable());
        datePicker.setEditable(false);
        assertFalse(datePicker.isEditable());
    }

    @Test public void ensureCanToggleShowing() {
        datePicker.show();
        assertTrue(datePicker.isShowing());
        datePicker.hide();
        assertFalse(datePicker.isShowing());
    }

    @Test public void ensureCanNotToggleShowingWhenDisabled() {
        datePicker.setDisable(true);
        datePicker.show();
        assertFalse(datePicker.isShowing());
        datePicker.setDisable(false);
        datePicker.show();
        assertTrue(datePicker.isShowing());
    }

    @Test public void ensureCanSetPromptText() {
        datePicker.setPromptText("Test 1 2 3");
        assertEquals("Test 1 2 3", datePicker.getPromptText());
    }

    @Test public void ensureCanSetPromptTextToNull() {
        assertEquals("", datePicker.getPromptText());
        datePicker.setPromptText(null);
        assertEquals(null, datePicker.getPromptText());
    }

    @Test public void ensurePromptTextStripsNewlines() {
        datePicker.setPromptText("Test\n1\n2\n3");
        assertEquals("Test123", datePicker.getPromptText());
    }

    @Test public void ensureCanToggleArmed() {
        assertFalse(datePicker.isArmed());
        datePicker.arm();
        assertTrue(datePicker.isArmed());
        datePicker.disarm();
        assertFalse(datePicker.isArmed());
    }

    @Test public void ensureCanSetOnAction() {
        EventHandler<ActionEvent> onAction = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) { }
        };
        datePicker.setOnAction(onAction);
        assertEquals(onAction, datePicker.getOnAction());
    }

    @Test public void ensureOnActionPropertyReferencesBean() {
        assertEquals(datePicker, datePicker.onActionProperty().getBean());
    }

    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/
    @Test public void checkPromptTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        datePicker.promptTextProperty().bind(strPr);
        assertTrue("PromptText cannot be bound", datePicker.getPromptText().equals("value"));
        strPr.setValue("newvalue");
        assertTrue("PromptText cannot be bound", datePicker.getPromptText().equals("newvalue"));
    }

    @Test public void checkValuePropertyBind() {
        ObjectProperty<LocalDate> objPr = new SimpleObjectProperty<LocalDate>(today);
        datePicker.valueProperty().bind(objPr);
        assertTrue("value cannot be bound", datePicker.getValue().equals(today));
        LocalDate tomorrow = today.plusDays(1);
        objPr.setValue(tomorrow);
        assertTrue("value cannot be bound", datePicker.getValue().equals(tomorrow));
    }

    @Test public void checkChronologyPropertyBind() {
        ObjectProperty<Chronology> objPr = new SimpleObjectProperty<Chronology>(IsoChronology.INSTANCE);
        datePicker.chronologyProperty().bind(objPr);
        assertTrue("Chronology cannot be bound", datePicker.getChronology().equals(IsoChronology.INSTANCE));
        objPr.setValue(JapaneseChronology.INSTANCE);
        assertTrue("Chronology cannot be bound", datePicker.getChronology().equals(JapaneseChronology.INSTANCE));
    }


    /*********************************************************************
     * Tests for bug reports                                             *
     ********************************************************************/

}
