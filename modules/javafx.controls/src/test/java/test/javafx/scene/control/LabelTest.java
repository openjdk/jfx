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

import com.sun.javafx.scene.NodeHelper;
import javafx.css.CssMetaData;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class LabelTest {
    private Label label;

    @Before public void setup() {
        label = new Label();
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void defaultConstructorShouldHaveNoGraphicAndEmptyString() {
        assertNull(label.getGraphic());
        assertEquals("", label.getText());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphicAndSpecifiedString() {
        Label label2 = new Label(null);
        assertNull(label2.getGraphic());
        assertNull(label2.getText());

        label2 = new Label("");
        assertNull(label2.getGraphic());
        assertEquals("", label2.getText());

        label2 = new Label("Hello");
        assertNull(label2.getGraphic());
        assertEquals("Hello", label2.getText());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedGraphicAndSpecifiedString() {
        Label label2 = new Label(null, null);
        assertNull(label2.getGraphic());
        assertNull(label2.getText());

        Rectangle rect = new Rectangle();
        label2 = new Label("Hello", rect);
        assertSame(rect, label2.getGraphic());
        assertEquals("Hello", label2.getText());
    }

    @Test public void defaultConstructorShouldSetStyleClassTo_label() {
        assertStyleClassContains(label, "label");
    }

    @Test public void oneArgConstructorShouldSetStyleClassTo_label() {
        Label label2 = new Label(null);
        assertStyleClassContains(label2, "label");
    }

    @Test public void twoArgConstructorShouldSetStyleClassTo_label() {
        Label label2 = new Label(null, null);
        assertStyleClassContains(label2, "label");
    }

    @Test public void defaultConstructorShouldSetFocusTraversableToFalse() {
        assertFalse(label.isFocusTraversable());
    }

    @Test public void oneArgConstructorShouldSetFocusTraversableToFalse() {
        Label label2 = new Label(null);
        assertFalse(label2.isFocusTraversable());
    }

    @Test public void twoArgConstructorShouldSetFocusTraversableToFalse() {
        Label label2 = new Label(null, null);
        assertFalse(label2.isFocusTraversable());
    }

    /********************************************************************************
     *                                                                              *
     *                         Tests for labelFor property                          *
     *                                                                              *
     *******************************************************************************/

    @Test public void labelForDefaultValueIsNULL() {
        assertNull(label.getLabelFor());
        assertNull(label.labelForProperty().get());
    }

    @Test public void settingLabelForValueShouldWork() {
        TextField textField = new TextField();
        label.setLabelFor(textField);
        assertSame(textField, label.getLabelFor());
    }

    @Test public void settingLabelForAndThenCreatingAModelAndReadingTheValueStillWorks() {
        TextField textField = new TextField();
        label.setLabelFor(textField);
        assertSame(textField, label.labelForProperty().get());
    }

    @Test public void labelForCanBeBound() {
        TextField textField = new TextField();
        ObjectProperty<TextField> other = new SimpleObjectProperty<TextField>(textField);
        label.labelForProperty().bind(other);
        assertSame(textField, label.getLabelFor());
        other.set(null);
        assertNull(label.getLabelFor());
    }

    @Test public void CssMetaData_isSettable_AlwaysReturnsFalseForLabelFor() {
        try {
            CssMetaData styleable = ((StyleableProperty)label.labelForProperty()).getCssMetaData();
            assertNull(styleable);
        } catch (ClassCastException ignored) {
            // pass!
        } catch (Exception e) {
            org.junit.Assert.fail(e.toString());
        }
    }

    @Test public void labelForBeanIsCorrect() {
        assertSame(label, label.labelForProperty().getBean());
    }

    @Test public void labelForNameIsCorrect() {
        assertEquals("labelFor", label.labelForProperty().getName());
    }

    // TODO I'm not actually sure why we base the showMnemonics of a Label
    // TODO on the showMnemonics of its labelFor, but since we do, I need
    // TODO to test it to make sure things get hooked up and unhooked
    @Test public void showMnemonicsHasNoListenersOnTextFieldByDefault() {
        // This is a sanity check test, so the following tests make sense
        TextField textField = new TextField();
        assertEquals(0, getListenerCount(NodeHelper.showMnemonicsProperty(textField)));
    }

    @Test public void settingLabelForShouldAddListenerToShowMnemonics() {
        TextField textField = new TextField();
        label.setLabelFor(textField);
        assertEquals(1, getListenerCount(NodeHelper.showMnemonicsProperty(textField)));
    }

    @Test public void settingLabelForShouldAddListenerToShowMnemonics_SetThroughProperty() {
        TextField textField = new TextField();
        label.labelForProperty().set(textField);
        assertEquals(1, getListenerCount(NodeHelper.showMnemonicsProperty(textField)));
    }

    @Test public void settingLabelForShouldAddListenerToShowMnemonics_WhenBound() {
        TextField textField = new TextField();
        ObjectProperty<TextField> other = new SimpleObjectProperty<TextField>(textField);
        label.labelForProperty().bind(other);
        assertEquals(1, getListenerCount(NodeHelper.showMnemonicsProperty(textField)));
    }

    @Test public void clearingLabelForShouldRemoveListenerFromShowMnemonics() {
        TextField textField = new TextField();
        label.setLabelFor(textField);
        label.setLabelFor(null);
        assertEquals(0, getListenerCount(NodeHelper.showMnemonicsProperty(textField)));
    }

    @Test public void clearingLabelForShouldRemoveListenerFromShowMnemonics_SetThroughProperty() {
        TextField textField = new TextField();
        label.labelForProperty().set(textField);
        label.labelForProperty().set(null);
        assertEquals(0, getListenerCount(NodeHelper.showMnemonicsProperty(textField)));
    }

    @Test public void clearingLabelForShouldRemoveListenerFromShowMnemonics_WhenBound() {
        TextField textField = new TextField();
        ObjectProperty<TextField> other = new SimpleObjectProperty<TextField>(textField);
        label.labelForProperty().bind(other);
        other.set(null);
        assertEquals(0, getListenerCount(NodeHelper.showMnemonicsProperty(textField)));
    }

    @Test public void swappingLabelForShouldAddAndRemoveListenerFromShowMnemonics() {
        TextField a = new TextField();
        TextField b = new TextField();
        label.setLabelFor(a);
        label.setLabelFor(b);
        assertEquals(0, getListenerCount(NodeHelper.showMnemonicsProperty(a)));
        assertEquals(1, getListenerCount(NodeHelper.showMnemonicsProperty(b)));
    }

    @Test public void swappingLabelForShouldAddAndRemoveListenerFromShowMnemonics_SetThroughProperty() {
        TextField a = new TextField();
        TextField b = new TextField();
        label.labelForProperty().set(a);
        label.labelForProperty().set(b);
        assertEquals(0, getListenerCount(NodeHelper.showMnemonicsProperty(a)));
        assertEquals(1, getListenerCount(NodeHelper.showMnemonicsProperty(b)));
    }

    @Test public void swappingLabelForShouldAddAndRemoveListenerFromShowMnemonics_WhenBound() {
        TextField a = new TextField();
        TextField b = new TextField();
        ObjectProperty<TextField> other = new SimpleObjectProperty<TextField>(a);
        label.labelForProperty().bind(other);
        other.set(b);
        assertEquals(0, getListenerCount(NodeHelper.showMnemonicsProperty(a)));
        assertEquals(1, getListenerCount(NodeHelper.showMnemonicsProperty(b)));
    }

    @Test public void changingShowMnemonicsOnLabelForUpdatesStateForLabel() {
        TextField textField = new TextField();
        label.setLabelFor(textField);
        assertFalse(NodeHelper.isShowMnemonics(textField));
        assertFalse(NodeHelper.isShowMnemonics(label));
        NodeHelper.setShowMnemonics(textField, true);
        assertTrue(NodeHelper.isShowMnemonics(textField));
        assertTrue(NodeHelper.isShowMnemonics(label));
    }

    @Test public void showMnemonicsOfLabelIsUpdatedWhenLabelForIsSet() {
        TextField textField = new TextField();
        NodeHelper.setShowMnemonics(textField, true);
        label.setLabelFor(textField);
        assertTrue(NodeHelper.isShowMnemonics(textField));
        assertTrue(NodeHelper.isShowMnemonics(label));
    }

    @Test public void showMnemonicsOfLabelIsSetToFalseWhenLabelForIsCleared() {
        TextField textField = new TextField();
        NodeHelper.setShowMnemonics(textField, true);
        label.setLabelFor(textField);
        label.setLabelFor(null);
        assertTrue(NodeHelper.isShowMnemonics(textField));
        assertFalse(NodeHelper.isShowMnemonics(label));
    }
}
