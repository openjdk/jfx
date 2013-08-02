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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import java.util.Arrays;
import java.util.List;
import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for BehaviorBase
 */
public class BehaviorBaseTest {

    // Test that changes to key bindings after the createKeyBindings method is called
    // has no effect.

    // Test that the different traversal bindings call the right stuff on the traversal engine

    // Test that the control is set

    // Test that key events on a control are handled by the behavior

    // Test that key events on a control where the behavior was removed are no longer handled by the behavior
    @Test public void keyListenerShouldBeRemovedAfterBehaviorIsDisposed() {
        // Shove the result of the behavior responding to a key event here
        final BooleanProperty actionCalled = new SimpleBooleanProperty(false);
        // Setup the test
        Button button = new Button();
        BehaviorBase behavior = new BehaviorBase(button) {
            @Override
            protected List<KeyBinding> createKeyBindings() {
                return Arrays.asList(new KeyBinding(KeyCode.ENTER, "action!"));
            }

            @Override protected void callAction(String name) {
                actionCalled.set(true);
            }
        };
        Skin skin = new BehaviorSkinBase(button, behavior) {};
        button.setSkin(skin);

        KeyEventFirer keyboard = new KeyEventFirer(button);

        // Send a key event
        keyboard.doKeyPress(KeyCode.ENTER);
        assertTrue(actionCalled.get());
        actionCalled.set(false);

        // Remove the skin, which should result in dispose being called, and then send another event
        button.setSkin(null);
        keyboard.doKeyPress(KeyCode.ENTER);
        assertFalse(actionCalled.get());
    }

    // Test the matchActionForEvent
    //      Make sure paired events are consumed / allowed together
    //
}
