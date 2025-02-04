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

package test.com.sun.javafx.scene.control.infrastructure;

import static javafx.scene.input.KeyCode.A;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test of enhanced KeyEventFirer.
 */
public class KeyEventFirerTest {

    private TextField textField;
    private Button button;
    private Pane root;
    private Stage stage;
    private Scene scene;

    /**
     * Test that keyEvent is delivered to focused control and nowhere else.
     */
    @Test
    public void testFireViaScene() {
        showAndFocus(button);
        List<KeyEvent> buttonEvents = new ArrayList<>();
        button.addEventHandler(KEY_PRESSED, buttonEvents::add);
        List<KeyEvent> textFieldEvents = new ArrayList<>();
        textField.addEventHandler(KEY_PRESSED, textFieldEvents::add);
        KeyEventFirer firer = new KeyEventFirer(textField, scene);
        firer.doKeyPress(A);
        assertEquals(1, buttonEvents.size(), "button must have received the key");
        assertEquals(0, textFieldEvents.size(), "textField must not have received the key");
    }

    /**
     * Test that keyEvent is delivered to focused control and nowhere else.
     * Here we test that the target is not required.
     */
    @Test
    public void testFireViaSceneNullTarget() {
        showAndFocus(button);
        List<KeyEvent> buttonEvents = new ArrayList<>();
        button.addEventHandler(KEY_PRESSED, buttonEvents::add);
        List<KeyEvent> textFieldEvents = new ArrayList<>();
        textField.addEventHandler(KEY_PRESSED, textFieldEvents::add);
        KeyEventFirer firer = new KeyEventFirer(null, scene);
        firer.doKeyPress(A);
        assertEquals(1, buttonEvents.size(), "button must have received the key");
        assertEquals(0, textFieldEvents.size(), "textField must not have received the key");
    }

    /**
     * This simulates a false positive: even though not focused, the textField handlers
     * are notified when firing directly. That's possible, but typically not what we want to test!
     */
    @Test
    public void testFireTargetFalseGreen() {
        showAndFocus(button);
        List<KeyEvent> buttonEvents = new ArrayList<>();
        button.addEventHandler(KEY_PRESSED, buttonEvents::add);
        List<KeyEvent> textFieldEvents = new ArrayList<>();
        textField.addEventHandler(KEY_PRESSED, textFieldEvents::add);
        // firing on a node that is not focusOwner
        KeyEventFirer incorrectFirer = new KeyEventFirer(textField);
        incorrectFirer.doKeyPress(A);
        int falseTextFieldNotification = textFieldEvents.size();
        int falseButtonNotification = buttonEvents.size();
        assertEquals(1, textFieldEvents.size(), "false green - textField must have received the key");
        assertEquals(0, buttonEvents.size(), "false green - button must not have received the key");
        textFieldEvents.clear();
        buttonEvents.clear();
        // firing on the scene makes a difference
        KeyEventFirer correctFirer = new KeyEventFirer(null, scene);
        correctFirer.doKeyPress(A);
        assertEquals(falseTextFieldNotification - 1, textFieldEvents.size());
        assertEquals(falseButtonNotification + 1, buttonEvents.size());
    }

    @Test
    public void testTwoParamConstructorNPE() {
        assertThrows(NullPointerException.class, () -> {
            new KeyEventFirer(null, null);
        });
    }

    @Test
    public void testSingleParamConstructorNPE() {
        assertThrows(NullPointerException.class, () -> {
            new KeyEventFirer(null);
        });
    }

    /**
     * Need all: stage.show, stage.requestFocus and control.requestFocus to
     * have consistent focused state on control (that is focusOwner and isFocused)
     */
    @Test
    public void testUIState() {
        assertEquals(List.of(button, textField), root.getChildren());
        stage.show();
        stage.requestFocus();
        button.requestFocus();
        assertEquals(button, scene.getFocusOwner());
        assertTrue(button.isFocused());
    }

    private void showAndFocus(Node focused) {
        stage.show();
        stage.requestFocus();
        if (focused != null) {
            focused.requestFocus();
            assertTrue(focused.isFocused());
            assertSame(focused, scene.getFocusOwner());
        }
    }

    @BeforeEach
    public void setup() {
        root = new VBox();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
        button = new Button("I'm a button");
        textField = new TextField("some text");
        root.getChildren().addAll(button, textField);
    }

    @AfterEach
    public void cleanup() {
        if (stage != null) {
            stage.hide();
        }
    }
}
