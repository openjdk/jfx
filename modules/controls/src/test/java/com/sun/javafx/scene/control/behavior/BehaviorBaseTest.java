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

package com.sun.javafx.scene.control.behavior;

import javafx.scene.control.Button;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.infrastructure.KeyModifier;
import com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for BehaviorBase
 */
public class BehaviorBaseTest {
    private ControlMock button;
    private Skin<Button> skin;
    private BehaviorBaseMock behavior;
    private ArrayList<KeyBinding> bindings;
    private KeyEventFirer keyboard;
    private MouseEventFirer mouse;

    @Before
    public void setup() {
        button = new ControlMock();
        bindings = new ArrayList<>();
        bindings.addAll(BehaviorBase.TRAVERSAL_BINDINGS);
        behavior = new BehaviorBaseMock(button, bindings);
        skin = new BehaviorSkinBase(button, behavior) {};
        button.setSkin(skin);
        keyboard = new KeyEventFirer(button);
        mouse = new MouseEventFirer(button);
    }

    @After
    public void after() {
        mouse.dispose();
    }

    /**
     * We don't accept null for the control of a behavior
     */
    @Test(expected = NullPointerException.class)
    public void creatingBehaviorWithNullControlThrowsNPE() {
        new BehaviorBase<Button>(null, Collections.EMPTY_LIST);
    }

    /**
     * We do accept null for the key bindings of a behavior, treating it as empty
     */
    @Test
    public void creatingBehaviorWithNullKeyBindingsResultsInEmptyKeyBindings() {
        BehaviorBaseMock b = new BehaviorBaseMock(button, null);
        skin = new BehaviorSkinBase(button, b) {};
        button.setSkin(skin);
        keyboard.doKeyPress(KeyCode.TAB);
        assertFalse(b.someActionCalled());
    }

    /**
     * This is a hard-and-fast rule, you must always have the control set to the
     * expected value after the constructor was called
     */
    @Test
    public void controlMustBeSet() {
        assertEquals(button, behavior.getControl());
    }

    /**
     * This is a little fishy. Right now we store the control in a final field, so we
     * know that we want this to continue returning "button" after dispose, but really
     * it seems better to clear the control after dispose, although this mean we can
     * no longer store the control in a final field.
     */
    @Test
    public void controlMustNotBeClearedOnDispose() {
        button.setSkin(null);
        assertEquals(button, behavior.getControl());
    }

    /**
     * Modifying the list passed to BehaviorBase should have no effect on what
     * key bindings are used.
     */
    @Test
    public void changingKeyBindingsDynamicallyMustHaveNoEffect() {
        bindings.set(0, new KeyBinding(KeyCode.TAB, "action2"));
        keyboard.doKeyPress(KeyCode.TAB);
        assertTrue(behavior.actionCalled(BehaviorBase.TRAVERSE_NEXT));
        assertFalse(behavior.actionCalled("action2"));
    }

    /**
     * Test that if the default traversal bindings are installed, that the "tab" event is
     * handled correctly
     */
    @Test
    public void tabCalls_TraverseNext() {
        keyboard.doKeyPress(KeyCode.TAB);
        assertTrue(behavior.actionCalled(BehaviorBase.TRAVERSE_NEXT));
    }

    /**
     * Test that if the default traversal bindings are installed, that the "shift+tab" event is
     * handled correctly
     */
    @Test
    public void shiftTabCalls_TraversePrevious() {
        keyboard.doKeyPress(KeyCode.TAB, KeyModifier.SHIFT);
        assertTrue(behavior.actionCalled(BehaviorBase.TRAVERSE_PREVIOUS));
    }

    /**
     * Test that if the default traversal bindings are installed, that the "up" event is
     * handled correctly
     */
    @Test
    public void upCalls_TraverseUp() {
        keyboard.doKeyPress(KeyCode.UP);
        assertTrue(behavior.actionCalled(BehaviorBase.TRAVERSE_UP));
    }

    /**
     * Test that if the default traversal bindings are installed, that the "down" event is
     * handled correctly
     */
    @Test
    public void downCalls_TraverseDown() {
        keyboard.doKeyPress(KeyCode.DOWN);
        assertTrue(behavior.actionCalled(BehaviorBase.TRAVERSE_DOWN));
    }

    /**
     * Test that if the default traversal bindings are installed, that the "left" event is
     * handled correctly
     */
    @Test
    public void leftCalls_TraverseLeft() {
        keyboard.doKeyPress(KeyCode.LEFT);
        assertTrue(behavior.actionCalled(BehaviorBase.TRAVERSE_LEFT));
    }

    /**
     * Test that if the default traversal bindings are installed, that the "right" event is
     * handled correctly
     */
    @Test
    public void rightCalls_TraverseRight() {
        keyboard.doKeyPress(KeyCode.RIGHT);
        assertTrue(behavior.actionCalled(BehaviorBase.TRAVERSE_RIGHT));
    }

    /**
     * Make sure that key listeners are removed after the behavior is disposed, which should
     * always happen when the skin is disposed, which should always happen when the skin is
     * removed from a Control.
     */
    @Test
    public void keyListenerShouldBeRemovedAfterBehaviorIsDisposed() {
        // Send a key event
        keyboard.doKeyPress(KeyCode.TAB);
        assertTrue(behavior.someActionCalled());
        behavior.reset();

        // Remove the skin, which should result in dispose being called, and then send another event
        button.setSkin(null);
        keyboard.doKeyPress(KeyCode.TAB);
        assertFalse(behavior.someActionCalled());
    }

    /**
     * Test to make sure that the focusChanged() method is called when focus is gained.
     */
    @Test
    public void focusListenerShouldBeCalledWhenFocusGained() {
        assertFalse(behavior.focusCalled);
        button.focus();
        assertTrue(behavior.focusCalled);
    }

    /**
     * Test to make sure that the focusChanged() method is called when focus is lost
     */
    @Test
    public void focusListenerShouldBeCalledWhenFocusLost() {
        assertFalse(behavior.focusCalled);
        button.focus();
        behavior.reset();
        button.blur();
        assertTrue(behavior.focusCalled);
    }

    /**
     * Make sure that the behavior is no longer called on focus changes after disposal
     */
    @Test
    public void focusListenerShouldBeRemovedAfterBehaviorIsDisposed() {
        button.setSkin(null);
        button.focus();
        assertFalse(behavior.focusCalled);
    }

    /**
     * mousePressed method should be called when the behavior is wired up properly
     */
    @Test
    public void mousePressedCalledWhenMousePressedOverControl() {
        assertFalse(behavior.mousePressCalled);
        mouse.fireMousePressed();
        assertTrue(behavior.mousePressCalled);
    }

    /**
     * The mouse handlers should never be called when the skin / behavior has been
     * disposed of.
     */
    @Test
    public void mouseListenerShouldBeRemovedAfterBehaviorIsDisposed() {
        button.setSkin(null);
        mouse.fireMousePressed();
        assertFalse(behavior.mousePressCalled);
    }


    // Test the matchActionForEvent
    //      Make sure paired events are consumed / allowed together
    //

    private static final class ControlMock extends Button {
        public void focus() {
            setFocused(true);
        }
        public void blur() {
            setFocused(false);
        }
    }

    private static final class BehaviorBaseMock extends BehaviorBase<Button> {
        private Set<String> actionsCalled = new HashSet<>();
        private boolean focusCalled;
        private boolean mousePressCalled;

        public BehaviorBaseMock(Button control, List<KeyBinding> keyBindings) {
            super(control, keyBindings);
        }

        @Override protected void callAction(String name) {
            actionsCalled.add(name);
        }

        public boolean someActionCalled() {
            return !actionsCalled.isEmpty();
        }

        public boolean actionCalled(String name) {
            return actionsCalled.contains(name);
        }

        @Override protected void focusChanged() {
            focusCalled = true;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            mousePressCalled = true;
        }

        public void reset() {
            actionsCalled.clear();
            focusCalled = false;
            mousePressCalled = false;
        }
    }
}
