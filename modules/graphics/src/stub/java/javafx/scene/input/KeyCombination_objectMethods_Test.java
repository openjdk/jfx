/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.input;

import static javafx.scene.input.KeyCombination.ALT_ANY;
import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static javafx.scene.input.KeyCombination.META_DOWN;
import static javafx.scene.input.KeyCombination.SHIFT_ANY;
import static javafx.scene.input.KeyCombination.SHORTCUT_ANY;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;

import java.util.Arrays;
import java.util.Collection;

import javafx.scene.input.KeyCombination.ModifierValue;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.test.ObjectMethodsTestBase;

@RunWith(Parameterized.class)
public final class KeyCombination_objectMethods_Test
        extends ObjectMethodsTestBase {
    @Parameters
    public static Collection data() {
        return Arrays.asList(new Object[] {
                equalObjects(
                        new KeyCodeCombination(KeyCode.R,
                                               ModifierValue.ANY,
                                               ModifierValue.DOWN,
                                               ModifierValue.UP,
                                               ModifierValue.UP,
                                               ModifierValue.ANY),
                        new KeyCodeCombination(KeyCode.R,
                                               CONTROL_DOWN,
                                               SHIFT_ANY,
                                               SHORTCUT_ANY),
                        KeyCombination.keyCombination(
                                "Ignore Shortcut + Ctrl + Ignore Shift + R")),
                equalObjects(
                        new KeyCodeCombination(KeyCode.C,
                                               ModifierValue.UP,
                                               ModifierValue.UP,
                                               ModifierValue.UP,
                                               ModifierValue.UP,
                                               ModifierValue.DOWN),
                        new KeyCodeCombination(KeyCode.C,
                                               SHORTCUT_DOWN),
                        KeyCombination.keyCombination("Shortcut + C")),
                equalObjects(
                        new KeyCharacterCombination("x",
                                                    ModifierValue.UP,
                                                    ModifierValue.UP,
                                                    ModifierValue.ANY,
                                                    ModifierValue.DOWN,
                                                    ModifierValue.UP),
                        new KeyCharacterCombination(
                                "x",
                                ALT_ANY,
                                META_DOWN),
                        KeyCombination.keyCombination(
                                "Ignore Alt + Meta + 'x'")),
                differentObjects(
                        new KeyCodeCombination(KeyCode.R,
                                               ModifierValue.ANY,
                                               ModifierValue.DOWN,
                                               ModifierValue.UP,
                                               ModifierValue.UP,
                                               ModifierValue.UP),
                        new KeyCodeCombination(KeyCode.A,
                                               CONTROL_DOWN,
                                               SHIFT_ANY),
                        new KeyCodeCombination(KeyCode.R,
                                               CONTROL_DOWN,
                                               SHIFT_ANY,
                                               ALT_ANY),
                        KeyCombination.keyCombination(
                                "Ctrl + Shift + R"),
                        KeyCombination.keyCombination(
                                "Ignore Shift + R"),
                        KeyCombination.keyCombination(
                                "Ctrl + Ignore Shift + K")),
                differentObjects(
                        new KeyCharacterCombination("x",
                                                    ModifierValue.UP,
                                                    ModifierValue.UP,
                                                    ModifierValue.ANY,
                                                    ModifierValue.DOWN,
                                                    ModifierValue.UP),
                        new KeyCharacterCombination("X",
                                                    ModifierValue.UP,
                                                    ModifierValue.UP,
                                                    ModifierValue.ANY,
                                                    ModifierValue.DOWN,
                                                    ModifierValue.UP),
                        new KeyCharacterCombination(
                                "y",
                                ALT_ANY,
                                META_DOWN),
                        new KeyCharacterCombination(
                                "x",
                                ALT_ANY,
                                META_DOWN,
                                SHORTCUT_DOWN),
                        new KeyCharacterCombination(
                                "x",
                                ALT_ANY),
                        KeyCombination.keyCombination(
                                "Alt + Meta + 'x'"),
                        KeyCombination.keyCombination(
                                "Ignore Alt + Meta + Ctrl + 'x'"),
                        KeyCombination.keyCombination(
                                "Ignore Alt + Meta + 'z'")),
                differentObjectsMediumHashcode(
                        new KeyCodeCombination(KeyCode.Q,
                                               ModifierValue.UP,
                                               ModifierValue.UP,
                                               ModifierValue.ANY,
                                               ModifierValue.DOWN,
                                               ModifierValue.UP),
                        new KeyCharacterCombination("Q",
                                                    ModifierValue.UP,
                                                    ModifierValue.UP,
                                                    ModifierValue.ANY,
                                                    ModifierValue.DOWN,
                                                    ModifierValue.UP),
                        KeyCombination.keyCombination("Ctrl + Alt + 'X'"),
                        KeyCombination.keyCombination("Ctrl + Alt + X"),
                        new KeyCombination() {
                        },
                        new Object())
            });
    }

    public KeyCombination_objectMethods_Test(
            final Configuration configuration) {
        super(configuration);
    }
}
