/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.sun.javafx.tk.Toolkit;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;
import static org.junit.Assert.*;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.DatePicker;
import javafx.scene.control.skin.ComboBoxPopupControl;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;

/**
 * Test for https://bugs.openjdk.java.net/browse/JDK-8233040 - F4
 * must not be consumed by EventFilter in ComboBoxPopupControl.
 * <p>
 * Parameterized in concrete sub of ComboBoxBase and editable.
 */
@RunWith(Parameterized.class)
public class ComboSpecialKeyTest {

    private Toolkit tk;
    private Scene scene;
    private Stage stage;
    private Pane root;

    private ComboBoxBase comboBox;
    private Supplier<ComboBoxBase> comboFactory;
    private boolean editable;

    @Test
    public void testF4TogglePopup() {
        showAndFocus();
        comboBox.setEditable(editable);
        assertFalse(comboBox.isShowing());
        KeyEventFirer firer = new KeyEventFirer(comboBox);
        firer.doKeyPress(F4);
        assertTrue(failPrefix(), comboBox.isShowing());
        firer.doKeyPress(F4);
        assertFalse(failPrefix(), comboBox.isShowing());
    }

    @Test
    public void testF4ConsumeFilterNotTogglePopup() {
        showAndFocus();
        comboBox.setEditable(editable);
        List<KeyEvent> events = new ArrayList<>();
        comboBox.addEventFilter(KEY_RELEASED, e -> {
            if (e.getCode() == F4) {
                events.add(e);
                e.consume();
            }
        });
        KeyEventFirer firer = new KeyEventFirer(comboBox);
        firer.doKeyPress(F4);
        assertFalse(failPrefix() + ": popup must not be showing", comboBox.isShowing());
    }

    protected String failPrefix() {
        String failPrefix = comboBox.getClass().getSimpleName() + " editable " + editable;
        return failPrefix;
    }

//---------------- parameterized

    // Note: name property not supported before junit 4.11
    @Parameterized.Parameters//(name = "{index}: editable {1} ")
    public static Collection<Object[]> data() {
        // Supplier for type of ComboBoxBase to test, editable
        Object[][] data = new Object[][] {
            {(Supplier)ComboBox::new, false},
            {(Supplier)ComboBox::new, true },
            {(Supplier)DatePicker::new, false },
            {(Supplier)DatePicker::new, true},
            {(Supplier)ColorPicker::new, false },
        };
        return Arrays.asList(data);
    }

    public ComboSpecialKeyTest(Supplier<ComboBoxBase> factory, boolean editable) {
        this.comboFactory = factory;
        this.editable = editable;
    }

// --- initial and setup

    @Test
    public void testInitialState() {
        assertNotNull(comboBox);
        showAndFocus();
        List<Node> expected = List.of(comboBox);
        assertEquals(expected, root.getChildren());
    }

     protected void showAndFocus() {
        showAndFocus(comboBox);
    }

    protected void showAndFocus(Node control) {
        stage.show();
        stage.requestFocus();
        control.requestFocus();
        assertTrue(control.isFocused());
        assertSame(control, scene.getFocusOwner());
    }

    @After
    public void cleanup() {
        stage.hide();
    }

    @Before
    public void setup() {
        ComboBoxPopupControl c;
        // This step is not needed (Just to make sure StubToolkit is
        // loaded into VM)
        tk = (StubToolkit) Toolkit.getToolkit();
        root = new VBox();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
        comboBox = comboFactory.get();
        root.getChildren().addAll(comboBox);
    }


}
