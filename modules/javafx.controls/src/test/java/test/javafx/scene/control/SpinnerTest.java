/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static junit.framework.Assert.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import java.util.Locale;

import javafx.scene.control.Button;
import javafx.scene.control.skin.SpinnerSkin;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerShim;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactoryShim;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.tk.Toolkit;

import static javafx.scene.control.SpinnerValueFactoryShim.*;

public class SpinnerTest {

    private Spinner<?> spinner;

    // --- int spinner
    private Spinner<Integer> intSpinner;
    private IntegerSpinnerValueFactory intValueFactory;

    // --- double spinner
    private Spinner<Double> dblSpinner;
    private DoubleSpinnerValueFactory dblValueFactory;

    // --- list spinner
    private ObservableList<String> strings;
    private Spinner<String> listSpinner;
    private ListSpinnerValueFactory listValueFactory;

    // --- LocalDate spinner
    private Spinner<LocalDate> localDateSpinner;
    private SpinnerValueFactory<LocalDate> localDateValueFactory;

    // --- LocalTime spinner
    private Spinner<LocalTime> localTimeSpinner;
    private SpinnerValueFactory<LocalTime>localTimeValueFactory;

    // used in tests for counting events, reset to zero in setup()
    private int eventCount;

    private static Locale defaultLocale;

    @BeforeClass public static void setupOnce() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterClass public static void tearDownOnce() {
        Locale.setDefault(defaultLocale);
    }

    @Before public void setup() {
        eventCount = 0;
        spinner = new Spinner();

        intSpinner = new Spinner<>(0, 10, 5, 1);
        intValueFactory = (IntegerSpinnerValueFactory) intSpinner.getValueFactory();

        dblSpinner = new Spinner<>(0.0, 1.0, 0.5, 0.05);
        dblValueFactory = (DoubleSpinnerValueFactory) dblSpinner.getValueFactory();

        strings = FXCollections.observableArrayList("string1", "string2", "string3");
        listSpinner = new Spinner<>(strings);
        listValueFactory = (ListSpinnerValueFactory<String>) listSpinner.getValueFactory();

        // minimum is today minus 10 days, maximum is today plus 10 days
        localDateSpinner = SpinnerShim.getSpinnerLocalDate(
                nowPlusDays(-10), nowPlusDays(10),
                LocalDate.now(), 1, ChronoUnit.DAYS);
        localDateValueFactory = localDateSpinner.getValueFactory();

        localTimeSpinner = SpinnerShim.getSpinnerLocalTime(
                LocalTime.MIN, LocalTime.MAX,
                LocalTime.now(), 1, ChronoUnit.HOURS);
        localTimeValueFactory = localTimeSpinner.getValueFactory();
    }


    /***************************************************************************
     *                                                                         *
     * Basic tests                                                             *
     *                                                                         *
     **************************************************************************/

    @Test public void createDefaultSpinner_hasSpinnerStyleClass() {
        assertEquals(1, spinner.getStyleClass().size());
        assertTrue(spinner.getStyleClass().contains("spinner"));
    }

    @Test public void createDefaultSpinner_editorIsNotNull() {
        assertNotNull(spinner.getEditor());
    }

    @Test public void createDefaultSpinner_valueFactoryIsNull() {
        assertNull(spinner.getValueFactory());
    }

    @Test public void createDefaultSpinner_valueIsNull() {
        assertNull(spinner.getValue());
    }

    @Test public void createDefaultSpinner_editableIsFalse() {
        assertFalse(spinner.isEditable());
    }

    @Ignore("Waiting for StageLoader")
    @Test public void createDefaultSpinner_defaultSkinIsInstalled() {
        assertTrue(spinner.getSkin() instanceof SpinnerSkin);
    }


    /***************************************************************************
     *                                                                         *
     * Alternative constructor tests                                           *
     *                                                                         *
     **************************************************************************/

    @Test public void createIntSpinner_createValidValueFactory() {
        Spinner<Integer> intSpinner = new Spinner<Integer>(0, 10, 5, 1);
        assertTrue(intSpinner.getValueFactory() instanceof IntegerSpinnerValueFactory);
        IntegerSpinnerValueFactory valueFactory = (IntegerSpinnerValueFactory) intSpinner.getValueFactory();
        assertEquals(5, (int) valueFactory.getValue());
    }

    @Test public void createIntSpinner_setInitialValueOutsideMaxBounds() {
        Spinner<Integer> intSpinner = new Spinner<Integer>(0, 10, 100, 1);
        assertTrue(intSpinner.getValueFactory() instanceof IntegerSpinnerValueFactory);
        IntegerSpinnerValueFactory valueFactory = (IntegerSpinnerValueFactory) intSpinner.getValueFactory();
        assertEquals(0, (int) valueFactory.getValue());
    }

    @Test public void createIntSpinner_setInitialValueOutsideMinBounds() {
        Spinner<Integer> intSpinner = new Spinner<Integer>(0, 10, -100, 1);
        assertTrue(intSpinner.getValueFactory() instanceof IntegerSpinnerValueFactory);
        IntegerSpinnerValueFactory valueFactory = (IntegerSpinnerValueFactory) intSpinner.getValueFactory();
        assertEquals(0, (int) valueFactory.getValue());
    }

    @Test public void createListSpinner_createValidValueFactory() {
        Spinner<String> stringSpinner = new Spinner<>(FXCollections.observableArrayList("item 1", "item 2"));
        assertTrue(stringSpinner.getValueFactory() instanceof ListSpinnerValueFactory);
        ListSpinnerValueFactory valueFactory = (ListSpinnerValueFactory) stringSpinner.getValueFactory();
        assertEquals("item 1", valueFactory.getValue());
    }

    @Test public void createListSpinner_emptyListResultsInNullValue() {
        Spinner<String> stringSpinner = new Spinner<String>(FXCollections.observableArrayList());
        assertTrue(stringSpinner.getValueFactory() instanceof ListSpinnerValueFactory);
        ListSpinnerValueFactory valueFactory = (ListSpinnerValueFactory) stringSpinner.getValueFactory();
        assertNull(valueFactory.getValue());
    }

    @Test public void createListSpinner_nullListResultsInNullValue() {
        Spinner<String> stringSpinner = new Spinner<>((ObservableList<String>)null);
        assertTrue(stringSpinner.getValueFactory() instanceof ListSpinnerValueFactory);
        ListSpinnerValueFactory valueFactory = (ListSpinnerValueFactory) stringSpinner.getValueFactory();
        assertNull(valueFactory.getValue());
    }

    @Test public void createSpinner_customSpinnerValueFactory() {
        SpinnerValueFactory<String> valueFactory = new ListSpinnerValueFactory<>(FXCollections.observableArrayList("item 1", "item 2"));
        Spinner<String> stringSpinner = new Spinner<>(valueFactory);
        assertEquals(valueFactory, stringSpinner.getValueFactory());
        ListSpinnerValueFactory valueFactory1 = (ListSpinnerValueFactory) stringSpinner.getValueFactory();
        assertEquals("item 1", valueFactory.getValue());
        assertEquals("item 1", valueFactory1.getValue());
    }



    /***************************************************************************
     *                                                                         *
     * increment / decrement tests                                             *
     * (we test the actual inc / dec in the value factory impl tests)          *
     *                                                                         *
     **************************************************************************/

    @Test(expected = IllegalStateException.class)
    public void expectExceptionWhenNoArgsIncrementCalled_noValueFactory() {
        spinner.increment();
    }

    @Test(expected = IllegalStateException.class)
    public void expectExceptionWhenOneArgsIncrementCalled_noValueFactory() {
        spinner.increment(2);
    }

    @Test(expected = IllegalStateException.class)
    public void expectExceptionWhenNoArgsDecrementCalled_noValueFactory() {
        spinner.decrement();
    }

    @Test(expected = IllegalStateException.class)
    public void expectExceptionWhenOneArgsDecrementCalled_noValueFactory() {
        spinner.decrement(2);
    }


    /***************************************************************************
     *                                                                         *
     * changing value factory tests                                            *
     *                                                                         *
     **************************************************************************/

    @Test public void valueFactory_valueIsNulledWhenValueFactoryisNull() {
        assertEquals(5, (int) intSpinner.getValue());
        intSpinner.setValueFactory(null);
        assertNull(spinner.getValue());
    }

    @Test public void valueFactory_valueIsUpdatedWhenValueFactoryChanged() {
        assertEquals(5, (int) intSpinner.getValue());
        intSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 10, 8));
        assertEquals(8, (int) intSpinner.getValue());
    }

//    @Test public void valueFactory_spinnerPropertyIsNullWhenRemovedFromSpinner() {
//        SpinnerValueFactory initialValueFactory = intSpinner.getValueFactory();
//        assertEquals(intSpinner, initialValueFactory.getSpinner());
//
//        intSpinner.setValueFactory(null);
//        assertNull(intSpinner.getValueFactory());
//    }
//
//    @Test public void valueFactory_spinnerPropertyIsSetOnNewSpinner() {
//        SpinnerValueFactory initialValueFactory = intSpinner.getValueFactory();
//        assertEquals(intSpinner, initialValueFactory.getSpinner());
//
//        SpinnerValueFactory newValueFactory = new IntSpinnerValueFactory(0, 10, 8);
//        intSpinner.setValueFactory(newValueFactory);
//
//        assertNull(initialValueFactory.getSpinner());
//        assertEquals(intSpinner, newValueFactory.getSpinner());
//    }


    /***************************************************************************
     *                                                                         *
     * value property events                                                   *
     *                                                                         *                                                                         *
     **************************************************************************/

    @Test public void value_notifyWhenChanged_validValue() {
        assertEquals(5, (int) intSpinner.getValue());
        intSpinner.valueProperty().addListener(o -> eventCount++);
        intSpinner.getValueFactory().setValue(3);
        assertEquals(1, eventCount);
    }

    @Test public void value_notifyWhenChanged_invalidValue() {
        assertEquals(5, (int) intSpinner.getValue());
        intSpinner.valueProperty().addListener(o -> eventCount++);
        intSpinner.getValueFactory().setValue(1000);

        // we expect two events: firstly, one for the invalid value, and another
        // for the valid value
        assertEquals(2, eventCount);
    }

    @Test public void value_notifyWhenChanged_existingValue() {
        assertEquals(5, (int) intSpinner.getValue());
        intSpinner.valueProperty().addListener(o -> eventCount++);
        intSpinner.getValueFactory().setValue(5);
        assertEquals(0, eventCount);
    }


    /***************************************************************************
     *                                                                         *
     * editing tests                                                           *
     *                                                                         *
     **************************************************************************/

    @Ignore("Need KeyboardEventFirer")
    @Test public void editing_commitValidInput() {
        intSpinner.valueProperty().addListener(o -> eventCount++);
        intSpinner.getEditor().setText("3");
        // TODO press enter

        assertEquals(1, eventCount);
        assertEquals(3, (int) intSpinner.getValue());
        assertEquals("3", intSpinner.getEditor().getText());
    }

    @Ignore("Need KeyboardEventFirer")
    @Test public void editing_commitInvalidInput() {
        intSpinner.valueProperty().addListener(o -> eventCount++);
        intSpinner.getEditor().setText("300");
        // TODO press enter

        assertEquals(2, eventCount);
        assertEquals(5, (int) intSpinner.getValue());
        assertEquals("5", intSpinner.getEditor().getText());
    }


    /***************************************************************************
     *                                                                         *
     * IntegerSpinnerValueFactory tests                                        *
     *                                                                         *
     **************************************************************************/

    @Test public void intSpinner_testIncrement_oneStep() {
        intValueFactory.increment(1);
        assertEquals(6, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_testIncrement_twoSteps() {
        intValueFactory.increment(2);
        assertEquals(7, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_testIncrement_manyCalls() {
        for (int i = 0; i < 100; i++) {
            intValueFactory.increment(1);
        }
        assertEquals(10, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_testIncrement_bigStepPastMaximum() {
        intValueFactory.increment(1000);
        assertEquals(10, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_testDecrement_oneStep() {
        intValueFactory.decrement(1);
        assertEquals(4, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_testDecrement_twoSteps() {
        intValueFactory.decrement(2);
        assertEquals(3, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_testDecrement_manyCalls() {
        for (int i = 0; i < 100; i++) {
            intValueFactory.decrement(1);
        }
        assertEquals(0, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_testDecrement_bigStepPastMinimum() {
        intValueFactory.decrement(1000);
        assertEquals(0, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_testWrapAround_increment_oneStep() {
        intValueFactory.setWrapAround(true);
        intValueFactory.increment(1); // 6
        intValueFactory.increment(1); // 7
        intValueFactory.increment(1); // 8
        intValueFactory.increment(1); // 9
        intValueFactory.increment(1); // 10
        intValueFactory.increment(1); // 0
        intValueFactory.increment(1); // 1
        assertEquals(1, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_testWrapAround_increment_twoSteps() {
        intValueFactory.setWrapAround(true);
        intValueFactory.increment(2); // 7
        intValueFactory.increment(2); // 9
        intValueFactory.increment(2); // 0
        intValueFactory.increment(2); // 2
        assertEquals(2, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_testWrapAround_decrement_oneStep() {
        intValueFactory.setWrapAround(true);
        intValueFactory.decrement(1); // 4
        intValueFactory.decrement(1); // 3
        intValueFactory.decrement(1); // 2
        intValueFactory.decrement(1); // 1
        intValueFactory.decrement(1); // 0
        intValueFactory.decrement(1); // 10
        intValueFactory.decrement(1); // 9
        assertEquals(9, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_testWrapAround_decrement_twoSteps() {
        intValueFactory.setWrapAround(true);
        intValueFactory.decrement(2); // 3
        intValueFactory.decrement(2); // 1
        intValueFactory.decrement(2); // 10
        intValueFactory.decrement(2); // 8
        assertEquals(8, (int) intValueFactory.getValue());
    }

    @Test public void intSpinner_assertDefaultConverterIsNonNull() {
        assertNotNull(intValueFactory.getConverter());
    }

    @Test public void intSpinner_testToString_valueInRange() {
        assertEquals("3", intValueFactory.getConverter().toString(3));
    }

    @Test public void intSpinner_testToString_valueOutOfRange() {
        assertEquals("300", intValueFactory.getConverter().toString(300));
    }

    @Test public void intSpinner_testFromString_valueInRange() {
        assertEquals(3, (int) intValueFactory.getConverter().fromString("3"));
    }

    @Test public void intSpinner_testFromString_valueOutOfRange() {
        assertEquals(300, (int) intValueFactory.getConverter().fromString("300"));
    }

    @Test public void intSpinner_testSetMin_doesNotChangeSpinnerValueWhenMinIsLessThanCurrentValue() {
        intValueFactory.setValue(5);
        assertEquals(5, (int) intSpinner.getValue());
        intValueFactory.setMin(3);
        assertEquals(5, (int) intSpinner.getValue());
    }

    @Test public void intSpinner_testSetMin_changesSpinnerValueWhenMinIsGreaterThanCurrentValue() {
        intValueFactory.setValue(0);
        assertEquals(0, (int) intSpinner.getValue());
        intValueFactory.setMin(5);
        assertEquals(5, (int) intSpinner.getValue());
    }

    @Test public void intSpinner_testSetMin_ensureThatMinCanNotExceedMax() {
        assertEquals(0, intValueFactory.getMin());
        assertEquals(10, intValueFactory.getMax());
        intValueFactory.setMin(20);
        assertEquals(10, intValueFactory.getMin());
    }

    @Test public void intSpinner_testSetMin_ensureThatMinCanEqualMax() {
        assertEquals(0, intValueFactory.getMin());
        assertEquals(10, intValueFactory.getMax());
        intValueFactory.setMin(10);
        assertEquals(10, intValueFactory.getMin());
    }

    @Test public void intSpinner_testSetMax_doesNotChangeSpinnerValueWhenMaxIsGreaterThanCurrentValue() {
        intValueFactory.setValue(5);
        assertEquals(5, (int) intSpinner.getValue());
        intValueFactory.setMax(8);
        assertEquals(5, (int) intSpinner.getValue());
    }

    @Test public void intSpinner_testSetMax_changesSpinnerValueWhenMaxIsGreaterThanCurrentValue() {
        intValueFactory.setValue(5);
        assertEquals(5, (int) intSpinner.getValue());
        intValueFactory.setMax(3);
        assertEquals(3, (int) intSpinner.getValue());
    }

    @Test public void intSpinner_testSetMax_ensureThatMaxCanNotGoLessThanMin() {
        intValueFactory.setMin(5);
        assertEquals(5, intValueFactory.getMin());
        assertEquals(10, intValueFactory.getMax());
        intValueFactory.setMax(3);
        assertEquals(5, intValueFactory.getMin());
    }

    @Test public void intSpinner_testSetMax_ensureThatMaxCanEqualMin() {
        intValueFactory.setMin(5);
        assertEquals(5, intValueFactory.getMin());
        assertEquals(10, intValueFactory.getMax());
        intValueFactory.setMax(5);
        assertEquals(5, intValueFactory.getMin());
    }

    @Test public void intSpinner_testSetValue_canNotExceedMax() {
        assertEquals(0, intValueFactory.getMin());
        assertEquals(10, intValueFactory.getMax());
        intValueFactory.setValue(50);
        assertEquals(10, (int) intSpinner.getValue());
    }

    @Test public void intSpinner_testSetValue_canNotExceedMin() {
        assertEquals(0, intValueFactory.getMin());
        assertEquals(10, intValueFactory.getMax());
        intValueFactory.setValue(-50);
        assertEquals(0, (int) intSpinner.getValue());
    }



    /***************************************************************************
     *                                                                         *
     * DoubleSpinnerValueFactory tests                                         *
     *                                                                         *                                                                         *
     **************************************************************************/

    @Test public void dblSpinner_testIncrement_oneStep() {
        dblValueFactory.increment(1);
        assertEquals(0.55, dblValueFactory.getValue(), 0);
    }

    @Test public void dblSpinner_testIncrement_twoSteps() {
        dblValueFactory.increment(2);
        assertEquals(0.6, dblValueFactory.getValue(), 0);
    }

    @Test public void dblSpinner_testIncrement_manyCalls() {
        for (int i = 0; i < 100; i++) {
            dblValueFactory.increment(1);
        }
        assertEquals(1.0, dblValueFactory.getValue(), 0);
    }

    @Test public void dblSpinner_testIncrement_bigStepPastMaximum() {
        dblValueFactory.increment(1000);
        assertEquals(1.0, dblValueFactory.getValue(), 0);
    }

    @Test public void dblSpinner_testDecrement_oneStep() {
        dblValueFactory.decrement(1);
        assertEquals(0.45, dblValueFactory.getValue());
    }

    @Test public void dblSpinner_testDecrement_twoSteps() {
        dblValueFactory.decrement(2);
        assertEquals(0.4, dblValueFactory.getValue());
    }

    @Test public void dblSpinner_testDecrement_manyCalls() {
        for (int i = 0; i < 100; i++) {
            dblValueFactory.decrement(1);
        }
        assertEquals(0, dblValueFactory.getValue(), 0);
    }

    @Test public void dblSpinner_testDecrement_bigStepPastMinimum() {
        dblValueFactory.decrement(1000);
        assertEquals(0, dblValueFactory.getValue(), 0);
    }

    @Test public void dblSpinner_testWrapAround_increment_oneStep() {
        dblValueFactory.setWrapAround(true);
        dblValueFactory.setValue(0.80);
        dblValueFactory.increment(1); // 0.85
        dblValueFactory.increment(1); // 0.90
        dblValueFactory.increment(1); // 0.95
        dblValueFactory.increment(1); // 1.00
        dblValueFactory.increment(1); // 0.00
        dblValueFactory.increment(1); // 0.05
        dblValueFactory.increment(1); // 0.10
        assertEquals(0.10, dblValueFactory.getValue(), 0);
    }

    @Test public void dblSpinner_testWrapAround_increment_twoSteps() {
        dblValueFactory.setWrapAround(true);
        dblValueFactory.setValue(0.80);
        dblValueFactory.increment(2); // 0.90
        dblValueFactory.increment(2); // 1.00
        dblValueFactory.increment(2); // 0.00
        dblValueFactory.increment(2); // 0.10
        assertEquals(0.10, dblValueFactory.getValue(), 0);
    }

    @Test public void dblSpinner_testWrapAround_decrement_oneStep() {
        dblValueFactory.setWrapAround(true);
        dblValueFactory.setValue(0.20);
        dblValueFactory.decrement(1); // 0.15
        dblValueFactory.decrement(1); // 0.10
        dblValueFactory.decrement(1); // 0.05
        dblValueFactory.decrement(1); // 0.00
        dblValueFactory.decrement(1); // 1.00
        dblValueFactory.decrement(1); // 0.95
        dblValueFactory.decrement(1); // 0.90
        assertEquals(0.90, dblValueFactory.getValue(), 0);
    }

    @Test public void dblSpinner_testWrapAround_decrement_twoSteps() {
        dblValueFactory.setWrapAround(true);
        dblValueFactory.setValue(0.20);
        dblValueFactory.decrement(2); // 0.10
        dblValueFactory.decrement(2); // 0.00
        dblValueFactory.decrement(2); // 1.00
        dblValueFactory.decrement(2); // 0.90
        assertEquals(0.90, dblValueFactory.getValue());
    }

    @Test public void dblSpinner_assertDefaultConverterIsNonNull() {
        assertNotNull(dblValueFactory.getConverter());
    }

    @Test public void dblSpinner_testToString_valueInRange() {
        assertEquals("0.3", dblValueFactory.getConverter().toString(0.3));
    }

    @Test public void dblSpinner_testToString_valueOutOfRange() {
        assertEquals("300", dblValueFactory.getConverter().toString(300D));
    }

    @Test public void dblSpinner_testFromString_valueInRange() {
        assertEquals(0.3, dblValueFactory.getConverter().fromString("0.3"));
    }

    @Test public void dblSpinner_testFromString_valueOutOfRange() {
        assertEquals(300.0, dblValueFactory.getConverter().fromString("300"), 0);
    }

    @Test public void dblSpinner_testSetMin_doesNotChangeSpinnerValueWhenMinIsLessThanCurrentValue() {
        dblValueFactory.setValue(0.5);
        assertEquals(0.5, dblSpinner.getValue());
        dblValueFactory.setMin(0.3);
        assertEquals(0.5, dblSpinner.getValue());
    }

    @Test public void dblSpinner_testSetMin_changesSpinnerValueWhenMinIsGreaterThanCurrentValue() {
        dblValueFactory.setValue(0.0);
        assertEquals(0.0, dblSpinner.getValue());
        dblValueFactory.setMin(0.5);
        assertEquals(0.5, dblSpinner.getValue());
    }

    @Test public void dblSpinner_testSetMin_ensureThatMinCanNotExceedMax() {
        assertEquals(0, dblValueFactory.getMin(), 0);
        assertEquals(1.0, dblValueFactory.getMax());
        dblValueFactory.setMin(20);
        assertEquals(1.0, dblValueFactory.getMin());
    }

    @Test public void dblSpinner_testSetMin_ensureThatMinCanEqualMax() {
        assertEquals(0, dblValueFactory.getMin(), 0);
        assertEquals(1.0, dblValueFactory.getMax());
        dblValueFactory.setMin(1.0);
        assertEquals(1.0, dblValueFactory.getMin());
    }

    @Test public void dblSpinner_testSetMax_doesNotChangeSpinnerValueWhenMaxIsGreaterThanCurrentValue() {
        dblValueFactory.setValue(0.5);
        assertEquals(0.5, dblSpinner.getValue());
        dblValueFactory.setMax(0.8);
        assertEquals(0.5, dblSpinner.getValue());
    }

    @Test public void dblSpinner_testSetMax_changesSpinnerValueWhenMaxIsGreaterThanCurrentValue() {
        dblValueFactory.setValue(0.5);
        assertEquals(0.5, dblSpinner.getValue());
        dblValueFactory.setMax(0.3);
        assertEquals(0.3, dblSpinner.getValue());
    }

    @Test public void dblSpinner_testSetMax_ensureThatMaxCanNotGoLessThanMin() {
        dblValueFactory.setMin(0.5);
        assertEquals(0.5, dblValueFactory.getMin());
        assertEquals(1.0, dblValueFactory.getMax());
        dblValueFactory.setMax(0.3);
        assertEquals(0.5, dblValueFactory.getMin());
    }

    @Test public void dblSpinner_testSetMax_ensureThatMaxCanEqualMin() {
        dblValueFactory.setMin(0.5);
        assertEquals(0.5, dblValueFactory.getMin());
        assertEquals(1.0, dblValueFactory.getMax());
        dblValueFactory.setMax(0.5);
        assertEquals(0.5, dblValueFactory.getMin());
    }

    @Test public void dblSpinner_testSetValue_canNotExceedMax() {
        assertEquals(0, dblValueFactory.getMin(), 0);
        assertEquals(1.0, dblValueFactory.getMax());
        dblValueFactory.setValue(5.0);
        assertEquals(1.0, dblSpinner.getValue());
    }

    @Test public void dblSpinner_testSetValue_canNotExceedMin() {
        assertEquals(0, dblValueFactory.getMin(), 0);
        assertEquals(1.0, dblValueFactory.getMax(), 0);
        dblValueFactory.setValue(-5.0);
        assertEquals(0, dblSpinner.getValue(), 0);
    }


    /***************************************************************************
     *                                                                         *
     * ListSpinnerValueFactory tests                                           *
     *                                                                         *
     **************************************************************************/

    @Test public void listSpinner_testIncrement_oneStep() {
        listValueFactory.increment(1);
        assertEquals("string2", listValueFactory.getValue());
    }

    @Test public void listSpinner_testIncrement_twoSteps() {
        listValueFactory.increment(2);
        assertEquals("string3", listValueFactory.getValue());
    }

    @Test public void listSpinner_testIncrement_manyCalls() {
        for (int i = 0; i < 100; i++) {
            listValueFactory.increment(1);
        }
        assertEquals("string3", listValueFactory.getValue());
    }

    @Test public void listSpinner_testIncrement_bigStepPastMaximum() {
        listValueFactory.increment(1000);
        assertEquals("string3", listValueFactory.getValue());
    }

    @Test public void listSpinner_testDecrement_oneStep() {
        listValueFactory.decrement(1);
        assertEquals("string1", listValueFactory.getValue());
    }

    @Test public void listSpinner_testDecrement_twoSteps() {
        listValueFactory.decrement(2);
        assertEquals("string1", listValueFactory.getValue());
    }

    @Test public void listSpinner_testDecrement_manyCalls() {
        for (int i = 0; i < 100; i++) {
            listValueFactory.decrement(1);
        }
        assertEquals("string1", listValueFactory.getValue());
    }

    @Test public void listSpinner_testDecrement_bigStepPastMinimum() {
        listValueFactory.decrement(1000);
        assertEquals("string1", listValueFactory.getValue());
    }

    @Test public void listSpinner_testWrapAround_increment_oneStep() {
        listValueFactory.setWrapAround(true);
        listValueFactory.increment(1); // string2
        listValueFactory.increment(1); // string3
        listValueFactory.increment(1); // string1
        listValueFactory.increment(1); // string2
        listValueFactory.increment(1); // string3
        listValueFactory.increment(1); // string1
        listValueFactory.increment(1); // string2
        assertEquals("string2", listValueFactory.getValue());
    }

    @Test public void listSpinner_testWrapAround_increment_twoSteps() {
        listValueFactory.setWrapAround(true);
        listValueFactory.increment(2); // string1 -> string3
        listValueFactory.increment(2); // string3 -> string2
        listValueFactory.increment(2); // string2 -> string1
        listValueFactory.increment(2); // string1 -> string3
        assertEquals("string3", listValueFactory.getValue());
    }

    @Test public void listSpinner_testWrapAround_decrement_oneStep() {
        listValueFactory.setWrapAround(true);
        listValueFactory.decrement(1); // string3
        listValueFactory.decrement(1); // string2
        listValueFactory.decrement(1); // string1
        listValueFactory.decrement(1); // string3
        listValueFactory.decrement(1); // string2
        listValueFactory.decrement(1); // string1
        listValueFactory.decrement(1); // string3
        assertEquals("string3", listValueFactory.getValue());
    }

    @Test public void listSpinner_testWrapAround_decrement_twoSteps() {
        listValueFactory.setWrapAround(true);
        listValueFactory.decrement(2); // string1 -> string2
        listValueFactory.decrement(2); // string2 -> string3
        listValueFactory.decrement(2); // string3 -> string1
        listValueFactory.decrement(2); // string1 -> string2
        assertEquals("string2", listValueFactory.getValue());
    }

    @Test public void listSpinner_assertDefaultConverterIsNonNull() {
        assertNotNull(listValueFactory.getConverter());
    }

    @Test public void listSpinner_testToString_valueInRange() {
        assertEquals("string2", listValueFactory.getConverter().toString("string2"));
    }

    @Test public void listSpinner_testToString_valueOutOfRange() {
        assertEquals("string300", listValueFactory.getConverter().toString("string300"));
    }

    @Test public void listSpinner_testFromString_valueInRange() {
        assertEquals("string3", listValueFactory.getConverter().fromString("string3"));
    }

    @Test public void listSpinner_testFromString_valueOutOfRange() {
        assertEquals("string300", listValueFactory.getConverter().fromString("string300"));
    }

    @Test public void listSpinner_testListChange_changeNonSelectedItem() {
        assertEquals("string1", listSpinner.getValue());

        strings.set(1, "string200"); // change 'string2' to 'string200'

        // there should be no change
        assertEquals("string1", listSpinner.getValue());
    }

    @Test public void listSpinner_testListChange_changeSelectedItem() {
        assertEquals("string1", listSpinner.getValue());

        strings.set(0, "string100"); // change 'string1' to 'string100'

        // the selected value should change
        assertEquals("string100", listSpinner.getValue());
    }

    @Test public void listSpinner_testListChange_changeEntireList_directly() {
        assertEquals("string1", listSpinner.getValue());

        listValueFactory.getItems().setAll("newString1", "newString2", "newString3");

        // the selected value should change
        assertEquals("newString1", listSpinner.getValue());
    }

    @Test public void listSpinner_testListChange_changeEntireList_usingSetter() {
        assertEquals("string1", listSpinner.getValue());

        listValueFactory.setItems(FXCollections.observableArrayList("newString1", "newString2", "newString3"));
        assertEquals("newString1", listSpinner.getValue());
    }

    @Test public void listSpinner_testListChange_setItemsToNull() {
        assertEquals("string1", listSpinner.getValue());
        listValueFactory.setItems(null);
        assertNull(listSpinner.getValue());
    }

    @Test public void listSpinner_testListChange_setItemsToNonNull() {
        assertEquals("string1", listSpinner.getValue());
        listValueFactory.setItems(null);
        assertNull(listSpinner.getValue());

        listValueFactory.setItems(FXCollections.observableArrayList("newString1", "newString2", "newString3"));
        assertEquals("newString1", listSpinner.getValue());
    }

    @Test public void listSpinner_testListChange_setNewEmptyListOverOldEmptyList() {
        // this tests the issue where we replace an empty list with another. As
        // both empty lists are equal, we are ensuring that the listeners update
        // to the new list.
        ObservableList<String> firstEmptyList = FXCollections.observableArrayList();
        ObservableList<String> newEmptyList = FXCollections.observableArrayList();

        ListSpinnerValueFactory valueFactory = new ListSpinnerValueFactory(firstEmptyList);
        Spinner listSpinner = new Spinner(valueFactory);

        valueFactory.setItems(newEmptyList);
        assertNull(listSpinner.getValue());

        newEmptyList.addAll("newString1", "newString2", "newString3");
        assertEquals("newString1", listSpinner.getValue());
    }



    /***************************************************************************
     *                                                                         *
     * LocalDateSpinnerValueFactory tests                                      *
     *                                                                         *
     **************************************************************************/

    private LocalDate nowPlusDays(int days) {
        return LocalDate.now().plus(days, ChronoUnit.DAYS);
    }

    @Test public void localDateSpinner_testIncrement_oneStep() {
        localDateValueFactory.increment(1);
        assertEquals(nowPlusDays(1), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_testIncrement_twoSteps() {
        localDateValueFactory.increment(2);
        assertEquals(nowPlusDays(2), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_testIncrement_manyCalls() {
        for (int i = 0; i < 100; i++) {
            localDateValueFactory.increment(1);
        }
        assertEquals(nowPlusDays(10), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_testIncrement_bigStepPastMaximum() {
        localDateValueFactory.increment(1000);
        assertEquals(nowPlusDays(10), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_testDecrement_oneStep() {
        localDateValueFactory.decrement(1);
        assertEquals(nowPlusDays(-1), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_testDecrement_twoSteps() {
        localDateValueFactory.decrement(2);
        assertEquals(nowPlusDays(-2), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_testDecrement_manyCalls() {
        for (int i = 0; i < 100; i++) {
            localDateValueFactory.decrement(1);
        }
        assertEquals(nowPlusDays(-10), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_testDecrement_bigStepPastMinimum() {
        localDateValueFactory.decrement(1000);
        assertEquals(nowPlusDays(-10), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_testWrapAround_increment_oneStep() {
        localDateValueFactory.setWrapAround(true);
        localDateValueFactory.setValue(nowPlusDays(7));
        localDateValueFactory.increment(1); // nowPlusDays(8)
        localDateValueFactory.increment(1); // nowPlusDays(9)
        localDateValueFactory.increment(1); // nowPlusDays(10)
        localDateValueFactory.increment(1); // nowPlusDays(-10)
        localDateValueFactory.increment(1); // nowPlusDays(-9)
        localDateValueFactory.increment(1); // nowPlusDays(-8)
        localDateValueFactory.increment(1); // nowPlusDays(-7)
        assertEquals(nowPlusDays(-7), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_testWrapAround_increment_twoSteps() {
        localDateValueFactory.setWrapAround(true);
        localDateValueFactory.setValue(nowPlusDays(7));
        localDateValueFactory.increment(2); // nowPlusDays(9)
        localDateValueFactory.increment(2); // nowPlusDays(-10)
        localDateValueFactory.increment(2); // nowPlusDays(-8)
        localDateValueFactory.increment(2); // nowPlusDays(-6)
        assertEquals(nowPlusDays(-6), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_testWrapAround_decrement_oneStep() {
        localDateValueFactory.setWrapAround(true);
        localDateValueFactory.setValue(nowPlusDays(-8));
        localDateValueFactory.decrement(1); // nowPlusDays(-9)
        localDateValueFactory.decrement(1); // nowPlusDays(-10)
        localDateValueFactory.decrement(1); // nowPlusDays(10)
        localDateValueFactory.decrement(1); // nowPlusDays(9)
        localDateValueFactory.decrement(1); // nowPlusDays(8)
        localDateValueFactory.decrement(1); // nowPlusDays(7)
        localDateValueFactory.decrement(1); // nowPlusDays(6)
        assertEquals(nowPlusDays(6), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_testWrapAround_decrement_twoSteps() {
        localDateValueFactory.setWrapAround(true);
        localDateValueFactory.setValue(nowPlusDays(-8));
        localDateValueFactory.decrement(2); // nowPlusDays(-10)
        localDateValueFactory.decrement(2); // nowPlusDays(9)
        localDateValueFactory.decrement(2); // nowPlusDays(7)
        localDateValueFactory.decrement(2); // nowPlusDays(6)
        assertEquals(nowPlusDays(6), localDateValueFactory.getValue());
    }

    @Test public void localDateSpinner_assertDefaultConverterIsNonNull() {
        assertNotNull(localDateValueFactory.getConverter());
    }

    @Test public void localDateSpinner_testToString_valueInRange() {
        assertEquals("2014-06-27", localDateValueFactory.getConverter().toString(LocalDate.of(2014, 6, 27)));
    }

    @Test public void localDateSpinner_testToString_valueOutOfRange() {
        assertEquals("2024-06-27", localDateValueFactory.getConverter().toString(LocalDate.of(2024, 6, 27)));
    }

    @Test public void localDateSpinner_testFromString_valueInRange() {
        assertEquals(LocalDate.of(2014, 6, 27), localDateValueFactory.getConverter().fromString("2014-06-27"));
    }

    @Test public void localDateSpinner_testFromString_valueOutOfRange() {
        assertEquals(LocalDate.of(2024, 6, 27), localDateValueFactory.getConverter().fromString("2024-06-27"));
    }

    @Test public void localDateSpinner_testSetMin_doesNotChangeSpinnerValueWhenMinIsLessThanCurrentValue() {
        LocalDate newValue = LocalDate.now();
        localDateValueFactory.setValue(newValue);
        assertEquals(newValue, localDateSpinner.getValue());
        SpinnerValueFactoryShim.LocalDate_setMin(localDateValueFactory, nowPlusDays(-3));
        assertEquals(newValue, localDateSpinner.getValue());
    }

    @Test public void localDateSpinner_testSetMin_changesSpinnerValueWhenMinIsGreaterThanCurrentValue() {
        LocalDate newValue = LocalDate.now();
        localDateValueFactory.setValue(newValue);
        assertEquals(newValue, localDateSpinner.getValue());

        LocalDate twoDaysFromNow = nowPlusDays(2);
        SpinnerValueFactoryShim.LocalDate_setMin(localDateValueFactory, twoDaysFromNow);
        assertEquals(twoDaysFromNow, localDateSpinner.getValue());
    }

    @Test public void localDateSpinner_testSetMin_ensureThatMinCanNotExceedMax() {
        assertEquals(nowPlusDays(-10), SpinnerValueFactoryShim.LocalDate_getMin(localDateValueFactory));
        assertEquals(nowPlusDays(10), SpinnerValueFactoryShim.LocalDate_getMax(localDateValueFactory));
        SpinnerValueFactoryShim.LocalDate_setMin(localDateValueFactory, nowPlusDays(20));
        assertEquals(nowPlusDays(10), SpinnerValueFactoryShim.LocalDate_getMax(localDateValueFactory));
    }

    @Test public void localDateSpinner_testSetMin_ensureThatMinCanEqualMax() {
        assertEquals(nowPlusDays(-10), SpinnerValueFactoryShim.LocalDate_getMin(localDateValueFactory));
        assertEquals(nowPlusDays(10), SpinnerValueFactoryShim.LocalDate_getMax(localDateValueFactory));
        SpinnerValueFactoryShim.LocalDate_setMin(localDateValueFactory, nowPlusDays(10));
        assertEquals(nowPlusDays(10), SpinnerValueFactoryShim.LocalDate_getMax(localDateValueFactory));
    }

    @Test public void localDateSpinner_testSetMax_doesNotChangeSpinnerValueWhenMaxIsGreaterThanCurrentValue() {
        LocalDate newValue = LocalDate.now();
        localDateValueFactory.setValue(newValue);
        assertEquals(newValue, localDateSpinner.getValue());
        SpinnerValueFactoryShim.LocalDate_setMax(localDateValueFactory, nowPlusDays(2));
        assertEquals(newValue, localDateSpinner.getValue());
    }

    @Test public void localDateSpinner_testSetMax_changesSpinnerValueWhenMaxIsLessThanCurrentValue() {
        LocalDate newValue = nowPlusDays(4);
        localDateValueFactory.setValue(newValue);
        assertEquals(newValue, localDateSpinner.getValue());

        LocalDate twoDays = nowPlusDays(2);
        SpinnerValueFactoryShim.LocalDate_setMax(localDateValueFactory, twoDays);
        assertEquals(twoDays, localDateSpinner.getValue());
    }

    @Test public void localDateSpinner_testSetMax_ensureThatMaxCanNotGoLessThanMin() {
        SpinnerValueFactoryShim.LocalDate_setMin(localDateValueFactory, nowPlusDays(5));
        assertEquals(nowPlusDays(5), SpinnerValueFactoryShim.LocalDate_getMin(localDateValueFactory));
        assertEquals(nowPlusDays(10), SpinnerValueFactoryShim.LocalDate_getMax(localDateValueFactory));
        SpinnerValueFactoryShim.LocalDate_setMax(localDateValueFactory, nowPlusDays(2));
        assertEquals(nowPlusDays(5), SpinnerValueFactoryShim.LocalDate_getMax(localDateValueFactory));
    }

    @Test public void localDateSpinner_testSetMax_ensureThatMaxCanEqualMin() {
        LocalDate twoDays = nowPlusDays(2);
        SpinnerValueFactoryShim.LocalDate_setMin(localDateValueFactory, twoDays);
        assertEquals(twoDays, SpinnerValueFactoryShim.LocalDate_getMin(localDateValueFactory));
        assertEquals(nowPlusDays(10), SpinnerValueFactoryShim.LocalDate_getMax(localDateValueFactory));
        SpinnerValueFactoryShim.LocalDate_setMax(localDateValueFactory, twoDays);
        assertEquals(twoDays, SpinnerValueFactoryShim.LocalDate_getMax(localDateValueFactory));
    }

    @Test public void localDateSpinner_testSetValue_canNotExceedMax() {
        assertEquals(nowPlusDays(-10), SpinnerValueFactoryShim.LocalDate_getMin(localDateValueFactory));
        assertEquals(nowPlusDays(10), SpinnerValueFactoryShim.LocalDate_getMax(localDateValueFactory));
        localDateValueFactory.setValue(nowPlusDays(50));
        assertEquals(nowPlusDays(10), localDateSpinner.getValue());
    }

    @Test public void localDateSpinner_testSetValue_canNotExceedMin() {
        assertEquals(nowPlusDays(-10), SpinnerValueFactoryShim.LocalDate_getMin(localDateValueFactory));
        assertEquals(nowPlusDays(10), SpinnerValueFactoryShim.LocalDate_getMax(localDateValueFactory));
        localDateValueFactory.setValue(nowPlusDays(-50));
        assertEquals(nowPlusDays(-10), localDateSpinner.getValue());
    }



    /***************************************************************************
     *                                                                         *
     * LocalTimeSpinnerValueFactory tests                                      *
     *                                                                         *
     **************************************************************************/

    private LocalTime nowPlusHours(int hours) {
        return LocalTime.now().plus(hours, ChronoUnit.HOURS);
    }

    private void assertTimeEquals(LocalTime expected, LocalTime actual) {
        // just compare hours, minutes and seconds
        assertEquals(expected.truncatedTo(ChronoUnit.MINUTES), actual.truncatedTo(ChronoUnit.MINUTES));
    }

    @Ignore
    @Test public void localTimeSpinner_testIncrement_oneStep() {
        localTimeValueFactory.increment(1);
        assertTimeEquals(nowPlusHours(1), localTimeValueFactory.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testIncrement_twoSteps() {
        localTimeValueFactory.increment(2);
        assertTimeEquals(nowPlusHours(2), localTimeValueFactory.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testIncrement_manyCalls() {
        for (int i = 0; i < 100; i++) {
            localTimeValueFactory.increment(1);
        }
        assertTimeEquals(LocalTime.MAX, localTimeValueFactory.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testIncrement_bigStepPastMaximum() {
        localTimeValueFactory.increment(100000);
        assertTimeEquals(LocalTime.MAX, localTimeValueFactory.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testDecrement_oneStep() {
        localTimeValueFactory.decrement(1);
        assertTimeEquals(nowPlusHours(-1), localTimeValueFactory.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testDecrement_twoSteps() {
        localTimeValueFactory.decrement(2);
        assertTimeEquals(nowPlusHours(-2), localTimeValueFactory.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testDecrement_manyCalls() {
        for (int i = 0; i < 100; i++) {
            localTimeValueFactory.decrement(1);
        }
        assertTimeEquals(LocalTime.MIN, localTimeValueFactory.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testDecrement_bigStepPastMinimum() {
        localTimeValueFactory.decrement(100000);
        assertTimeEquals(LocalTime.MIN, localTimeValueFactory.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testWrapAround_increment_oneStep() {
        localTimeValueFactory.setWrapAround(true);

        LocalTime six_pm = LocalTime.of(18,32);
        localTimeValueFactory.setValue(six_pm);
        localTimeValueFactory.increment(1); // 19:32
        localTimeValueFactory.increment(1); // 20:32
        localTimeValueFactory.increment(1); // 21:32
        localTimeValueFactory.increment(1); // 22:32
        localTimeValueFactory.increment(1); // 23:32
        localTimeValueFactory.increment(1); // 00:32
        localTimeValueFactory.increment(1); // 01:32
        assertTimeEquals(LocalTime.of(01,32), localTimeValueFactory.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testWrapAround_increment_twoSteps() {
        localTimeValueFactory.setWrapAround(true);

        LocalTime six_pm = LocalTime.of(18,32);
        localTimeValueFactory.setValue(six_pm);
        localTimeValueFactory.increment(2); // 20:32
        localTimeValueFactory.increment(2); // 22:32
        localTimeValueFactory.increment(2); // 00:32
        localTimeValueFactory.increment(2); // 02:32
        assertTimeEquals(LocalTime.of(02,32), localTimeValueFactory.getValue());
    }

    @Test public void localTimeSpinner_testWrapAround_decrement_oneStep() {
        localTimeValueFactory.setWrapAround(true);

        LocalTime six_am = LocalTime.of(06,32);
        localTimeValueFactory.setValue(six_am);
        localTimeValueFactory.decrement(1); // 05:32
        localTimeValueFactory.decrement(1); // 04:32
        localTimeValueFactory.decrement(1); // 03:32
        localTimeValueFactory.decrement(1); // 02:32
        localTimeValueFactory.decrement(1); // 01:32
        localTimeValueFactory.decrement(1); // 00:32
        localTimeValueFactory.decrement(1); // 23:32
        assertTimeEquals(LocalTime.of(23,32), localTimeValueFactory.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testWrapAround_decrement_twoSteps() {
        localTimeValueFactory.setWrapAround(true);

        LocalTime six_am = LocalTime.of(06,32);
        localTimeValueFactory.setValue(six_am);
        localTimeValueFactory.decrement(2); // 04:32
        localTimeValueFactory.decrement(2); // 02:32
        localTimeValueFactory.decrement(2); // 00:32
        localTimeValueFactory.decrement(2); // 22:32
        assertTimeEquals(LocalTime.of(22,32), localTimeValueFactory.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_assertDefaultConverterIsNonNull() {
        assertNotNull(localTimeValueFactory.getConverter());
    }

    @Ignore("Not safe when run early in the morning - needs refining when time permits")
    @Test public void localTimeSpinner_testSetMin_doesNotChangeSpinnerValueWhenMinIsLessThanCurrentValue() {
        LocalTime newValue = LocalTime.now();
        localTimeValueFactory.setValue(newValue);
        assertTimeEquals(newValue, localTimeSpinner.getValue());
        SpinnerValueFactoryShim.LocalTime_setMin(localTimeValueFactory, nowPlusHours(-3));
        assertTimeEquals(newValue, localTimeSpinner.getValue());
    }

    @Ignore("Not safe when late at night - needs refining when time permits")
    @Test public void localTimeSpinner_testSetMin_changesSpinnerValueWhenMinIsGreaterThanCurrentValue() {
        LocalTime newValue = LocalTime.now();
        localTimeValueFactory.setValue(newValue);
        assertTimeEquals(newValue, localTimeSpinner.getValue());

        LocalTime twoDaysFromNow = nowPlusHours(2);
        SpinnerValueFactoryShim.LocalTime_setMin(localTimeValueFactory, twoDaysFromNow);
        assertTimeEquals(twoDaysFromNow, localTimeSpinner.getValue());
    }

    @Test public void localTimeSpinner_testSetMin_ensureThatMinCanEqualMax() {
        assertTimeEquals(LocalTime.MIN, SpinnerValueFactoryShim.LocalTime_getMin(localTimeValueFactory));
        assertTimeEquals(LocalTime.MAX, SpinnerValueFactoryShim.LocalTime_getMax(localTimeValueFactory));
        SpinnerValueFactoryShim.LocalTime_setMin(localTimeValueFactory, LocalTime.MAX);
        assertTimeEquals(LocalTime.MAX, SpinnerValueFactoryShim.LocalTime_getMax(localTimeValueFactory));
    }

    @Ignore
    @Test public void localTimeSpinner_testSetMax_doesNotChangeSpinnerValueWhenMaxIsGreaterThanCurrentValue() {
        LocalTime newValue = LocalTime.now();
        localTimeValueFactory.setValue(newValue);
        assertTimeEquals(newValue, localTimeSpinner.getValue());
        SpinnerValueFactoryShim.LocalTime_setMax(localTimeValueFactory, nowPlusHours(2));
        assertTimeEquals(newValue, localTimeSpinner.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testSetMax_changesSpinnerValueWhenMaxIsLessThanCurrentValue() {
        LocalTime newValue = nowPlusHours(4);
        localTimeValueFactory.setValue(newValue);
        assertTimeEquals(newValue, localTimeSpinner.getValue());

        LocalTime twoDays = nowPlusHours(2);
        SpinnerValueFactoryShim.LocalTime_setMax(localTimeValueFactory, twoDays);
        assertTimeEquals(twoDays, localTimeSpinner.getValue());
    }

    @Ignore
    @Test public void localTimeSpinner_testSetMax_ensureThatMaxCanNotGoLessThanMin() {
        SpinnerValueFactoryShim.LocalTime_setMin(localTimeValueFactory, nowPlusHours(5));
        assertTimeEquals(nowPlusHours(5), SpinnerValueFactoryShim.LocalTime_getMax(localTimeValueFactory));
        assertTimeEquals(LocalTime.MAX, SpinnerValueFactoryShim.LocalTime_getMax(localTimeValueFactory));
        SpinnerValueFactoryShim.LocalTime_setMax(localTimeValueFactory, nowPlusHours(2));
        assertTimeEquals(nowPlusHours(5), SpinnerValueFactoryShim.LocalTime_getMax(localTimeValueFactory));
    }

    @Ignore
    @Test public void localTimeSpinner_testSetMax_ensureThatMaxCanEqualMin() {
        LocalTime twoDays = nowPlusHours(2);
        SpinnerValueFactoryShim.LocalTime_setMin(localTimeValueFactory, twoDays);
        assertTimeEquals(twoDays, SpinnerValueFactoryShim.LocalTime_getMax(localTimeValueFactory));
        assertTimeEquals(LocalTime.MAX, SpinnerValueFactoryShim.LocalTime_getMax(localTimeValueFactory));
        SpinnerValueFactoryShim.LocalTime_setMax(localTimeValueFactory, twoDays);
        assertTimeEquals(twoDays, SpinnerValueFactoryShim.LocalTime_getMax(localTimeValueFactory));
    }


    /***************************************************************************
     *                                                                         *
     * Tests for bugs                                                          *
     *                                                                         *
     **************************************************************************/

    @Test public void test_rt_39655_decrement() {
        assertEquals(5, (int) intSpinner.getValue());
        intSpinner.setEditable(true);
        intSpinner.getEditor().setText("7");
        intSpinner.decrement();
        assertEquals(6, (int) intSpinner.getValue());
    }

    @Test public void test_rt_39655_increment() {
        assertEquals(5, (int) intSpinner.getValue());
        intSpinner.setEditable(true);
        intSpinner.getEditor().setText("7");
        intSpinner.increment();
        assertEquals(8, (int) intSpinner.getValue());
    }

    @Test public void test_jdk_8150962() {
        Spinner<Double> spinner = new Spinner<>(-100, 100, 0, 0.5);
        spinner.getValueFactory().setValue(null);
        assertNull(spinner.getValue());
    }

    @Test public void test_jdk_8150946_testCommit_valid() {
        Spinner<Double> spinner = new Spinner<>(-100, 100, 0, 0.5);
        spinner.setEditable(true);
        assertEquals(0.0, spinner.getValue());
        spinner.getEditor().setText("2.5");
        spinner.commitValue();
        assertEquals(2.5, spinner.getValue());
    }

    @Test public void test_jdk_8150946_testCommit_invalid_outOfRange() {
        Spinner<Double> spinner = new Spinner<>(-100, 100, 0, 0.5);
        spinner.setEditable(true);
        assertEquals(0.0, spinner.getValue());
        spinner.getEditor().setText("2500");
        spinner.commitValue();
        assertEquals(100.0, spinner.getValue());
    }

    @Test(expected = RuntimeException.class)
    public void test_jdk_8150946_testCommit_invalid_wrongType() {
        Spinner<Double> spinner = new Spinner<>(-100, 100, 0, 0.5);
        spinner.setEditable(true);
        assertEquals(0.0, spinner.getValue());
        spinner.getEditor().setText("Hello, World!");
        spinner.commitValue();
        assertEquals(0.0, spinner.getValue());
    }

    @Test public void test_jdk_8150946_testCancel() {
        Spinner<Double> spinner = new Spinner<>(-100, 100, 2.5, 0.5);
        spinner.setEditable(true);
        assertEquals(2.5, spinner.getValue());
        assertEquals("2.5", spinner.getEditor().getText());
        spinner.getEditor().setText("3.5");
        assertEquals(2.5, spinner.getValue());
        spinner.cancelEdit();
        assertEquals(2.5, spinner.getValue());
        assertEquals("2.5", spinner.getEditor().getText());
    }

    // Test for JDK-8193311
    boolean enterDefaultPass = false;
    boolean escapeCancelPass = false;
    @Test public void testEnterEscapeKeysWithDefaultCancelButtons() {
        Toolkit tk = (StubToolkit)Toolkit.getToolkit();
        VBox root = new VBox();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setWidth(200);
        stage.setHeight(200);

        intSpinner.setEditable(true);

        Button defaultButton = new Button("OK");
        defaultButton.setOnAction(arg0 -> { enterDefaultPass = true; });
        defaultButton.setDefaultButton(true);

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(arg0 -> { escapeCancelPass = true; });
        cancelButton.setCancelButton(true);

        root.getChildren().addAll(intSpinner, defaultButton, cancelButton);
        stage.show();
        intSpinner.requestFocus();
        tk.firePulse();

        KeyEventFirer keyboard = new KeyEventFirer(intSpinner);
        keyboard.doKeyPress(KeyCode.ENTER);
        keyboard.doKeyPress(KeyCode.ESCAPE);
        tk.firePulse();

        if (!enterDefaultPass || !escapeCancelPass) {
            stage.hide();
        }

        String defMsg = "OnAction EventHandler of the default 'OK' " +
            "button should get invoked on ENTER key press.";
        String canMsg = "OnAction EventHandler of the cancel 'Cancel' " +
            "button should get invoked on ESCAPE key press.";

        assertTrue(defMsg, enterDefaultPass);
        assertTrue(canMsg, escapeCancelPass);

        // Same test with non editable spinner.
        intSpinner.setEditable(false);
        enterDefaultPass = false;
        escapeCancelPass = false;
        keyboard.doKeyPress(KeyCode.ENTER);
        keyboard.doKeyPress(KeyCode.ESCAPE);
        tk.firePulse();
        stage.hide();

        assertTrue(defMsg, enterDefaultPass);
        assertTrue(canMsg, escapeCancelPass);
    }

    @Test public void spinnerDelayTest() {
        Spinner<Double> spinner = new Spinner<>(-100, 100, 2.5, 0.5);

        spinner.setInitialDelay(null);
        assertEquals(300.0, spinner.getInitialDelay().toMillis(), 0.001);
        spinner.setInitialDelay(new Duration(500));
        assertEquals(500.0, spinner.getInitialDelay().toMillis(), 0.001);

        spinner.setRepeatDelay(null);
        assertEquals(60.0, spinner.getRepeatDelay().toMillis(), 0.001);
        spinner.setRepeatDelay(new Duration(200));
        assertEquals(200.0, spinner.getRepeatDelay().toMillis(), 0.001);

        spinner.setStyle("-fx-initial-delay: 400ms; -fx-repeat-delay: 100ms");
        FlowPane root = new FlowPane();
        root.getChildren().addAll(spinner);
        Scene scene = new Scene(root, 150, 150);
        scene.getRoot().applyCss();

        assertEquals(400.0, spinner.getInitialDelay().toMillis(), 0.001);
        assertEquals(100.0, spinner.getRepeatDelay().toMillis(), 0.001);
    }

    // Test for JDK-8185937
    @Test public void testIncDecKeys() {
        Toolkit tk = (StubToolkit)Toolkit.getToolkit();
        VBox root = new VBox();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setWidth(200);
        stage.setHeight(200);

        Spinner<Integer> spinner = new Spinner<>(-100, 100, 0);
        spinner.setEditable(true);
        assertEquals(0, spinner.getValue().intValue());

        try {
            root.getChildren().addAll(spinner);
            stage.show();
            spinner.requestFocus();
            tk.firePulse();

            KeyEventFirer keyboard = new KeyEventFirer(spinner.getEditor());
            keyboard.doKeyPress(KeyCode.UP);
            tk.firePulse();

            assertEquals(1, spinner.getValue().intValue());

            keyboard.doKeyPress(KeyCode.DOWN);
            tk.firePulse();

            assertEquals(0, spinner.getValue().intValue());
        } finally {
            stage.hide();
        }
    }
}
