/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.input.KeyCode.F4;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.DatePicker;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;

/**
 * Test for https://bugs.openjdk.org/browse/JDK-8233040 - F4
 * must not be consumed by EventFilter in ComboBoxPopupControl.
 */
public class ComboSpecialKeyTest {

    private Scene scene;
    private Stage stage;
    private Pane root;
    private ComboBoxBase comboBox;

    @ParameterizedTest
    @MethodSource("parameters")
    public void testF4TogglePopup(Supplier<ComboBoxBase> factory, boolean editable) {
        setup(factory);
        showAndFocus();
        comboBox.setEditable(editable);
        assertFalse(comboBox.isShowing());
        KeyEventFirer firer = new KeyEventFirer(comboBox);
        firer.doKeyPress(F4);
        assertTrue(comboBox.isShowing(), failPrefix(editable));
        firer.doKeyPress(F4);
        assertFalse(comboBox.isShowing(), failPrefix(editable));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testF4ConsumeFilterNotTogglePopup(Supplier<ComboBoxBase> factory, boolean editable) {
        setup(factory);
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
        assertFalse(comboBox.isShowing(), failPrefix(editable) + ": popup must not be showing");
    }

    protected String failPrefix(boolean editable) {
        String failPrefix = comboBox.getClass().getSimpleName() + " editable " + editable;
        return failPrefix;
    }

//---------------- parameterized

    private static Stream<Arguments> parameters() {
        // Supplier for type of ComboBoxBase to test, editable
        return Stream.of(
            Arguments.of((Supplier)ComboBox::new, false),
            Arguments.of((Supplier)ComboBox::new, true),
            Arguments.of((Supplier)DatePicker::new, false),
            Arguments.of((Supplier)DatePicker::new, true),
            Arguments.of((Supplier)ColorPicker::new, false)
        );
    }

// --- initial and setup

    @ParameterizedTest
    @MethodSource("parameters")
    public void testInitialState(Supplier<ComboBoxBase> factory, boolean editable) {
        setup(factory);
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

    @AfterEach
    public void cleanup() {
        stage.hide();
    }

    // @Before
    // junit5 does not support parameterized class-level tests yet
    public void setup(Supplier<ComboBoxBase> factory) {
        assertTrue(Toolkit.getToolkit() instanceof StubToolkit);  // Ensure StubToolkit is loaded
        root = new VBox();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
        comboBox = factory.get();
        root.getChildren().addAll(comboBox);
    }
}
