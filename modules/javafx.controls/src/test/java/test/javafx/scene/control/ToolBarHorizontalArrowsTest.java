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

package test.javafx.scene.control;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

import com.sun.javafx.tk.Toolkit;

import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.skin.ToolBarSkin;
import javafx.scene.input.KeyCode;

import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;


/**
 * Test basic horizontal navigation mappings for ToolBar.
 * It is parameterized on NodeOrientation
 */
@RunWith(Parameterized.class)
public class ToolBarHorizontalArrowsTest {
    @Parameterized.Parameters
    public static Collection<?> implementations() {
        return Arrays.asList(new Object[][] {
            {NodeOrientation.LEFT_TO_RIGHT},
            {NodeOrientation.RIGHT_TO_LEFT}
        });
    }

    private ToolBar toolBar;
    private Button btn1;
    private Button btn2;
    private Button btn3;
    private Button btn4;
    private Button btn5;

    private KeyEventFirer keyboard;
    private StageLoader stageLoader;
    private NodeOrientation orientation;

    public ToolBarHorizontalArrowsTest(NodeOrientation val) {
        orientation = val;
    }

    @Before public void setup() {
        toolBar = new ToolBar();
        toolBar.setNodeOrientation(orientation);

        // Create 5 buttons and add them in order to the toolBar
        btn1 = new Button("Btn1");
        btn2 = new Button("Btn2");
        btn3 = new Button("Btn3");
        btn4 = new Button("Btn4");
        btn5 = new Button("Btn5");

        toolBar.getItems().addAll(btn1, btn2, btn3, btn4, btn5);

        ToolBarSkin toolbarSkin = new ToolBarSkin(toolBar);
        toolBar.setSkin(toolbarSkin);

        stageLoader = new StageLoader(toolBar);
        stageLoader.getStage().show();
        ((StubToolkit)Toolkit.getToolkit()).firePulse();

        toolBar.setFocusTraversable(true);

        keyboard = new KeyEventFirer(toolBar, toolBar.getScene());
    }

    @After
    public void tearDown() {
        toolBar.getSkin().dispose();
        stageLoader.dispose();
    }

    // ---------------- Helper methods -------------------------
    /**
     * Toggles the parameter nodeOrientation and
     * sets the toolBar's orientation to the new toggled value
     */
    private void toggleNodeOrientation() {
        orientation = (orientation == NodeOrientation.LEFT_TO_RIGHT?
            NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        toolBar.setNodeOrientation(orientation);
    }

    /**
     * Orientation-aware forward horizontal navigation with arrow keys.
     * @param modifiers the modifiers to use on keyboard
     */
    private void forward(KeyModifier... modifiers) {
        if (orientation == NodeOrientation.LEFT_TO_RIGHT) {
            keyboard.doRightArrowPress(modifiers);
        } else if (orientation == NodeOrientation.RIGHT_TO_LEFT) {
            keyboard.doLeftArrowPress(modifiers);
        }
    }

    /**
     * Orientation-aware backward horizontal navigation with arrow keys.
     * @param modifiers the modifiers to use on keyboard
     */
    private void backward(KeyModifier... modifiers) {
        if (orientation == NodeOrientation.LEFT_TO_RIGHT) {
            keyboard.doLeftArrowPress(modifiers);
        } else if (orientation == NodeOrientation.RIGHT_TO_LEFT) {
            keyboard.doRightArrowPress(modifiers);
        }
    }


    // ----------------------- Tests ----------------------------

    /**
     * Test forward focus movements with TAB key
     */
    @Test
    public void testForwardFocus() {
        assertTrue(toolBar.isFocusTraversable());

        toolBar.getScene().getWindow().requestFocus();
        assertTrue(btn1.isFocused());

        keyboard.doKeyPress(KeyCode.TAB);
        assertTrue(btn2.isFocused());

        keyboard.doKeyPress(KeyCode.TAB);
        assertTrue(btn3.isFocused());

        keyboard.doKeyPress(KeyCode.TAB);
        assertTrue(btn4.isFocused());

        keyboard.doKeyPress(KeyCode.TAB);
        assertTrue(btn5.isFocused());
    }

    /**
     * Test backward focus movements with SHIFT+TAB keys
     */
    @Test
    public void testBackwardFocus() {
        assertTrue(toolBar.isFocusTraversable());

        toolBar.getScene().getWindow().requestFocus();
        btn5.requestFocus();
        assertTrue(btn5.isFocused());

        keyboard.doKeyPress(KeyCode.TAB, KeyModifier.SHIFT);
        assertTrue(btn4.isFocused());

        keyboard.doKeyPress(KeyCode.TAB, KeyModifier.SHIFT);
        assertTrue(btn3.isFocused());

        keyboard.doKeyPress(KeyCode.TAB, KeyModifier.SHIFT);
        assertTrue(btn2.isFocused());

        keyboard.doKeyPress(KeyCode.TAB, KeyModifier.SHIFT);
        assertTrue(btn1.isFocused());
    }

    @Test
    public void testForwardFocusArrows() {
        assertTrue(toolBar.isFocusTraversable());

        toolBar.getScene().getWindow().requestFocus();
        assertTrue(btn1.isFocused());

        forward();
        assertTrue(btn2.isFocused());

        forward();
        assertTrue(btn3.isFocused());

        forward();
        assertTrue(btn4.isFocused());

        forward();
        assertTrue(btn5.isFocused());
    }

    @Test
    public void testBackwardFocusArrows() {
        assertTrue(toolBar.isFocusTraversable());

        toolBar.getScene().getWindow().requestFocus();
        btn5.requestFocus();
        assertTrue(btn5.isFocused());

        backward();
        assertTrue(btn4.isFocused());

        backward();
        assertTrue(btn3.isFocused());

        backward();
        assertTrue(btn2.isFocused());

        backward();
        assertTrue(btn1.isFocused());
    }

    /**
     * Test forward focus movement when ToolBar's NodeOrientation
     * is changed dynamically.
     */
    @Test
    public void testForwardFocusArrows_toggleOrientation() {
        assertTrue(toolBar.isFocusTraversable());

        toolBar.getScene().getWindow().requestFocus();
        assertTrue(btn1.isFocused());

        forward();
        assertTrue(btn2.isFocused());

        toggleNodeOrientation();

        forward();
        assertTrue(btn3.isFocused());

        forward();
        assertTrue(btn4.isFocused());

        toggleNodeOrientation();

        forward();
        assertTrue(btn5.isFocused());
    }

    /**
     * Test backward focus movement when ToolBar's NodeOrientation
     * is changed dynamically.
     */
    @Test
    public void testBackwardFocusArrows_toggleOrientation() {
        assertTrue(toolBar.isFocusTraversable());

        toolBar.getScene().getWindow().requestFocus();
        btn5.requestFocus();
        assertTrue(btn5.isFocused());

        backward();
        assertTrue(btn4.isFocused());

        toggleNodeOrientation();

        backward();
        assertTrue(btn3.isFocused());

        backward();
        assertTrue(btn2.isFocused());

        toggleNodeOrientation();

        backward();
        assertTrue(btn1.isFocused());
    }

    /**
     * Test forward/backward focus movements when ToolBar's NodeOrientation
     * is changed dynamically.
     */
    @Test
    public void testMixedFocusArrows_toggleOrientation() {
        assertTrue(toolBar.isFocusTraversable());

        toolBar.getScene().getWindow().requestFocus();
        assertTrue(btn1.isFocused());

        forward();
        assertTrue(btn2.isFocused());

        toggleNodeOrientation();

        forward();
        assertTrue(btn3.isFocused());

        backward();
        assertTrue(btn2.isFocused());

        toggleNodeOrientation();

        backward();
        assertTrue(btn1.isFocused());
    }

    /**
     * Test focus movements when focus is at extreme child Nodes of the ToolBar
     */
    @Test
    public void testFocusExtremeNodesOfToolBar() {
        assertTrue(toolBar.isFocusTraversable());

        // Test backward movement when focus is at the first Button in the toolBar
        toolBar.getScene().getWindow().requestFocus();
        assertTrue(btn1.isFocused());

        backward();
        assertTrue(btn1.isFocused()); // focus should not change

        // Test forward movement when focus is at the last Button in the toolBar
        btn5.requestFocus();
        assertTrue(btn5.isFocused());

        forward();
        assertTrue(btn5.isFocused()); // focus should not change
    }
}
