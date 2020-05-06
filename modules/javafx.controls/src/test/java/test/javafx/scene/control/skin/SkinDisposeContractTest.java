/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static javafx.scene.control.ControlShim.*;

import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Pagination;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;

/**
 * Test for https://bugs.openjdk.java.net/browse/JDK-8244112:
 * skin must not blow if dispose is called more than once.
 * <p>
 * This test is parameterized in the type of control.
 */
@RunWith(Parameterized.class)
public class SkinDisposeContractTest {

    private Control control;
    private Class<Control> controlClass;

    /**
     * Skin must support multiple calls to dispose.
     * <p>
     * default -> dispose -> dispose
     * <p>
     * Errors on second dispose are JDK-8243940.
     * Failures/errors on first dispose (or before) are other errors - controls
     * are commented with issue reference
     *
     */
    @Test
    public void testDefaultDispose() {
        installDefaultSkin(control);
        control.getSkin().dispose();
        control.getSkin().dispose();
    }

  //---------------- parameterized

    // Note: name property not supported before junit 4.11
    @Parameterized.Parameters //(name = "{index}: {0} ")
    public static Collection<Object[]> data() {
        // class of control to test
        // commented controls have different issues as described in the referenced issues
        Object[][] data = new Object[][] {
            {Accordion.class, },
            {Button.class, },
            {ButtonBar.class, },
            {CheckBox.class, },
            {ChoiceBox.class, },
            {ColorPicker.class, },
            {ComboBox.class, },
            {DateCell.class, },
            {DatePicker.class, },
            {Hyperlink.class, },
            {Label.class, },
            {ListCell.class, },
            {ListView.class, },
            {MenuBar.class, },
            {MenuButton.class, },
            {Pagination.class, },
            {PasswordField.class, },
            {ProgressBar.class, },
            {ProgressIndicator.class, },
            {RadioButton.class, },
            {ScrollBar.class, },
            {ScrollPane.class, },
            {Separator.class, },
            {Slider.class, },
            {Spinner.class, },
            {SplitMenuButton.class, },
            {SplitPane.class, },
            {TableCell.class, },
            {TableRow.class, },
            {TableView.class, },
            {TabPane.class, },
            // @Ignore("8244419")
            // {TextArea.class, },
            {TextField.class, },
            {TitledPane.class, },
            {ToggleButton.class, },
            {ToolBar.class, },
            {TreeCell.class, },
            {TreeTableCell.class, },
            {TreeTableRow.class, },
            {TreeTableView.class, },
            {TreeView.class, },
        };
        return Arrays.asList(data);
    }

    public SkinDisposeContractTest(Class<Control> controlClass) {
        this.controlClass = controlClass;
    }

//----------------------

    @After
    public void cleanup() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    @Before
    public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
        control = createControl(controlClass);
    }

    /**
     * Creates and returns an instance of the given control class.
     * @param <T> the type of the control
     * @param controlClass
     * @return an instance of the class
     */
    public static <T extends Control> T createControl(Class<T> controlClass) {
        try {
            return controlClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
