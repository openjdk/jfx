/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.time.LocalDate;
import java.time.chrono.*;
import java.util.*;

import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;

import javafx.scene.control.skin.DatePickerSkin;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class DatePickerTest {
    private DatePicker datePicker;
    private final LocalDate today = LocalDate.now();
    private static Locale defaultLocale;


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

    @BeforeClass public static void setupOnce() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("en-US"));
    }

    @AfterClass public static void tearDownOnce() {
        Locale.setDefault(defaultLocale);
    }

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

    @Test public void noArgConstructor_promptTextIsNull() {
        assertNull(datePicker.getPromptText());
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

    @Test public void singleArgConstructor_promptTextIsNull() {
        final DatePicker b2 = new DatePicker(today);
        assertNull(b2.getPromptText());
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
        Callback<DatePicker, DateCell> cf = p -> null;
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
        datePicker.setPromptText("");
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
        EventHandler<ActionEvent> onAction = t -> { };
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

    @Test public void test_rt21186() {
        final DatePicker datePicker = new DatePicker();

        StageLoader sl = new StageLoader(datePicker);

        assertNull(datePicker.getTooltip());
        assertNull(datePicker.getEditor().getTooltip());

        Tooltip tooltip = new Tooltip("Tooltip");
        datePicker.setTooltip(tooltip);
        assertEquals(tooltip, datePicker.getTooltip());
        assertEquals(tooltip, datePicker.getEditor().getTooltip());

        datePicker.setTooltip(null);
        assertNull(datePicker.getTooltip());
        assertNull(datePicker.getEditor().getTooltip());

        sl.dispose();
    }

    @Test public void test_rt30549() {
        // Set a MinguoDate from a String
        datePicker.setChronology(MinguoChronology.INSTANCE);
        datePicker.getEditor().setText("5/22/0102 1");
        datePicker.setValue(datePicker.getConverter().fromString(datePicker.getEditor().getText()));
        assertEquals(MinguoChronology.INSTANCE.date(MinguoEra.ROC, 102, 5, 22),
                     MinguoDate.from(datePicker.getValue()));
        assertEquals("5/22/0102 1", datePicker.getEditor().getText());

        // Convert from MinguoDate to LocalDate (ISO)
        datePicker.setChronology(IsoChronology.INSTANCE);
        assertEquals(LocalDate.of(2013, 5, 22), datePicker.getValue());
        datePicker.getEditor().setText(datePicker.getConverter().toString(datePicker.getValue()));
        assertEquals("5/22/2013", datePicker.getEditor().getText());
    }

    private int test_rt35586_count = 0;
    @Test public void test_rt35586() {
        assertEquals(0, test_rt35586_count);

        final DatePicker dp = new DatePicker();
        dp.setOnAction(event -> {
            test_rt35586_count++;
            assertEquals("1/2/2015", dp.getEditor().getText());
        });

        StageLoader sl = new StageLoader(dp);

        dp.requestFocus();
        dp.getEditor().setText("1/2/2015");
        KeyEventFirer keyboard = new KeyEventFirer(dp);
        keyboard.doKeyPress(KeyCode.ENTER);

        assertEquals(1, test_rt35586_count);

        sl.dispose();
    }

    @Test public void test_rt35840() {
        final DatePicker dp = new DatePicker();
        dp.setEditable(true);
        StageLoader sl = new StageLoader(dp);
        dp.requestFocus();

        KeyEventFirer keyboard = new KeyEventFirer(dp);
        keyboard.doKeyTyped(KeyCode.DIGIT1);
        keyboard.doKeyTyped(KeyCode.SLASH);
        keyboard.doKeyTyped(KeyCode.DIGIT2);
        keyboard.doKeyTyped(KeyCode.SLASH);
        keyboard.doKeyTyped(KeyCode.DIGIT2);
        keyboard.doKeyTyped(KeyCode.DIGIT0);
        keyboard.doKeyTyped(KeyCode.DIGIT1);
        keyboard.doKeyTyped(KeyCode.DIGIT5);
        assertEquals("1/2/2015", dp.getEditor().getText());

        assertNull(dp.getValue());
        keyboard.doKeyPress(KeyCode.ENTER);
        assertEquals("2015-01-02", dp.getValue().toString());

        sl.dispose();
    }

    @Test public void test_rt36280_F4ShowsPopup() {
        final DatePicker dp = new DatePicker();
        StageLoader sl = new StageLoader(dp);
        KeyEventFirer dpKeyboard = new KeyEventFirer(dp);

        assertFalse(dp.isShowing());
        dpKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(dp.isShowing());

        sl.dispose();
    }

    @Test public void test_rt36280_altUpShowsPopup() {
        final DatePicker dp = new DatePicker();
        StageLoader sl = new StageLoader(dp);
        KeyEventFirer dpKeyboard = new KeyEventFirer(dp);

        assertFalse(dp.isShowing());
        dpKeyboard.doKeyPress(KeyCode.UP, KeyModifier.ALT);  // show the popup
        assertTrue(dp.isShowing());

        sl.dispose();
    }

    @Test public void test_rt36280_altDownShowsPopup_onComboBox() {
        final DatePicker dp = new DatePicker();
        StageLoader sl = new StageLoader(dp);
        KeyEventFirer dpKeyboard = new KeyEventFirer(dp);

        assertFalse(dp.isShowing());
        assertTrue(dp.getEditor().getText().isEmpty());
        dpKeyboard.doKeyPress(KeyCode.DOWN, KeyModifier.ALT);  // show the popup
        assertTrue(dp.isShowing());
        assertTrue(dp.getEditor().getText().isEmpty());

        sl.dispose();
    }

    @Test public void test_rt36280_altDownShowsPopup_onTextField() {
        final DatePicker dp = new DatePicker();
        StageLoader sl = new StageLoader(dp);

        KeyEventFirer tfKeyboard = new KeyEventFirer(dp.getEditor());
        assertFalse(dp.isShowing());
        assertTrue(dp.getEditor().getText().isEmpty());
        tfKeyboard.doKeyPress(KeyCode.DOWN, KeyModifier.ALT);  // show the popup
        assertTrue(dp.isShowing());
        assertTrue(dp.getEditor().getText().isEmpty());

        sl.dispose();
    }

    @Test public void test_rt36280_F4HidesShowingPopup() {
        final DatePicker dp = new DatePicker();
        StageLoader sl = new StageLoader(dp);
        KeyEventFirer dpKeyboard = new KeyEventFirer(dp);

        assertFalse(dp.isShowing());
        dpKeyboard.doKeyPress(KeyCode.F4);  // show the popup
        assertTrue(dp.isShowing());
        dpKeyboard.doKeyPress(KeyCode.F4);  // hide the popup
        assertFalse(dp.isShowing());

        sl.dispose();
    }

    @Test public void test_rt36717() {
        final DatePicker dp = new DatePicker();
        StageLoader sl = new StageLoader(dp);

        // the stack overflow only occurs when a ComboBox changes from non-editable to editable
        dp.setEditable(false);
        dp.setEditable(true);
        assertNotNull(dp.getEditor());
        KeyEventFirer tfKeyboard = new KeyEventFirer(dp.getEditor());
        tfKeyboard.doKeyPress(KeyCode.ENTER);   // Stack overflow here

        sl.dispose();
    }

    @Test public void test_rt36902() {
        final DatePicker dp1 = new DatePicker() {
            @Override public String toString() {
                return "dp1";
            }
        };
        final DatePicker dp2 = new DatePicker() {
            @Override public String toString() {
                return "dp2";
            }
        };
        dp2.setEditable(true);
        VBox vbox = new VBox(dp1, dp2);

        // lame - I would rather have one keyboard here but I couldn't get it to
        // work, so watch out for which keyboard is used below
        KeyEventFirer dp1Keyboard = new KeyEventFirer(dp1);
        KeyEventFirer dp2Keyboard = new KeyEventFirer(dp2);

        StageLoader sl = new StageLoader(vbox);
        sl.getStage().requestFocus();
        dp1.requestFocus();
        Toolkit.getToolkit().firePulse();
        Scene scene = sl.getStage().getScene();

        assertTrue(dp1.isFocused());
        assertEquals(dp1, scene.getFocusOwner());

        // move focus forward to dp2
        dp1Keyboard.doKeyPress(KeyCode.TAB);
        assertTrue(dp2.isFocused());
        assertEquals(dp2, scene.getFocusOwner());

        // move focus forward again to dp1
        dp2Keyboard.doKeyPress(KeyCode.TAB);
        assertTrue(dp1.isFocused());
        assertEquals(dp1, scene.getFocusOwner());

        // now start going backwards with shift-tab.
        // The first half of the bug is here - when we shift-tab into dp2, we
        // actually go into the FakeFocusTextField subcomponent, so whilst the
        // dp2.isFocused() returns true as expected, the scene focus owner is
        // not the ComboBox, but the FakeFocusTextField inside it
        dp1Keyboard.doKeyPress(KeyCode.TAB, KeyModifier.SHIFT);
        assertTrue("Expect dp2 to be focused, but actual focus owner is: " + scene.getFocusOwner(),
                dp2.isFocused());
        // Updated with fix for RT-34602: The TextField now never gets
        // focus (it's just faking it).
        // assertEquals("Expect dp2 TextField to be focused, but actual focus owner is: " + scene.getFocusOwner(),
        //         dp2.getEditor(), scene.getFocusOwner());
        assertEquals("Expect dp2 to be focused, but actual focus owner is: " + scene.getFocusOwner(),
                     dp2, scene.getFocusOwner());

        // This is where the second half of the bug appears, as we are stuck in
        // the FakeFocusTextField of dp2, we never make it to dp1
        dp2Keyboard.doKeyPress(KeyCode.TAB, KeyModifier.SHIFT);
        assertTrue(dp1.isFocused());
        assertEquals(dp1, scene.getFocusOwner());

        sl.dispose();
    }
}
