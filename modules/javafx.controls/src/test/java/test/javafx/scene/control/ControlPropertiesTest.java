/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Cell;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Pagination;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.ChoiceBoxTreeCell;
import javafx.scene.control.cell.ChoiceBoxTreeTableCell;
import javafx.scene.control.cell.ComboBoxTreeCell;
import javafx.scene.control.cell.ComboBoxTreeTableCell;
import javafx.scene.control.cell.ProgressBarTreeTableCell;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import org.junit.jupiter.api.Test;
import com.sun.javafx.scene.control.DoubleField;
import com.sun.javafx.scene.control.InputField;
import com.sun.javafx.scene.control.IntegerField;
import com.sun.javafx.scene.control.WebColorField;
import com.sun.javafx.scene.control.skin.FXVK;

/**
 * Tests contract for properties and their accessors and mutators
 * in the Control type hierarchy.
 *
 * Currently uses a list of classes, so any new Controls must be added manually.
 * Perhaps the test should scan modulepath and find all the Controls automagically.
 */
public class ControlPropertiesTest {
    /**
     * controls whether the test fails with assertion (true, default) or
     * outputs all violations to stderr (false).
     */
    private static final boolean FAIL_FAST = true;

    // list all current descendants of Control class.
    private Set<Class<?>> allControlClasses() {
        return Set.of(
            Accordion.class,
            ButtonBar.class,
            ButtonBase.class,
            Button.class,
            Cell.class,
            CheckBox.class,
            CheckBoxListCell.class,
            CheckBoxTableCell.class,
            CheckBoxTreeCell.class,
            CheckBoxTreeTableCell.class,
            ChoiceBox.class,
            ChoiceBoxTreeCell.class,
            ChoiceBoxTreeTableCell.class,
            ColorPicker.class,
            ComboBox.class,
            ComboBoxBase.class,
            ComboBoxTreeCell.class,
            ComboBoxTreeTableCell.class,
            DateCell.class,
            DatePicker.class,
            DoubleField.class,
            FXVK.class,
            //HTMLEditor.class,
            Hyperlink.class,
            IndexedCell.class,
            InputField.class,
            IntegerField.class,
            Labeled.class,
            Label.class,
            ListCell.class,
            ListView.class,
            MenuBar.class,
            MenuButton.class,
            Pagination.class,
            PasswordField.class,
            ProgressIndicator.class,
            ProgressBarTreeTableCell.class,
            RadioButton.class,
            ScrollBar.class,
            ScrollPane.class,
            Separator.class,
            SeparatorMenuItem.class,
            Slider.class,
            Spinner.class,
            SplitPane.class,
            TableRow.class,
            TableView.class,
            TabPane.class,
            TextArea.class,
            TextField.class,
            TextFieldTreeCell.class,
            TextFieldTreeTableCell.class,
            TitledPane.class,
            ToggleButton.class,
            TreeCell.class,
            TreeTableCell.class,
            TreeTableRow.class,
            TreeTableView.class,
            WebColorField.class
        );
    }

    /**
     * Tests for missing final keyword in Control properties and their accessors/mutators.
     */
    @Test
    public void testMissingFinalMethods() {
        for (Class<?> c : allControlClasses()) {
            check(c);
        }
    }

    private void check(Class<?> cls) {
        Method[] publicMethods = cls.getMethods();
        for (Method m : publicMethods) {
            String name = m.getName();
            if (name.endsWith("Property") && (m.getParameterCount() == 0)) {
                checkModifiers(m);

                String propName = name.substring(0, name.length() - "Property".length());
                check(publicMethods, propName, "get", 0);
                check(publicMethods, propName, "set", 1);
                check(publicMethods, propName, "is", 0);
            }
        }
    }

    private void check(Method[] methods, String propName, String prefix, int numArgs) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(prefix);
        sb.append(Character.toUpperCase(propName.charAt(0)));
        sb.append(propName, 1, propName.length());

        String name = sb.toString();
        for (Method m : methods) {
            if (m.getParameterCount() == numArgs) {
                if (name.equals(m.getName())) {
                    checkModifiers(m);
                    return;
                }
            }
        }
    }

    private void checkModifiers(Method m) {
        int mod = m.getModifiers();
        if (Modifier.isPublic(mod) && !Modifier.isFinal(mod)) {
            String msg = m + " is not final.";
            if (FAIL_FAST) {
                throw new AssertionError(msg);
            } else {
                System.err.println(msg);
            }
        }
    }
}
