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
package test.javafx.scene.control.behavior;

import static javafx.scene.input.KeyCode.*;
import javafx.scene.control.PasswordField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;

/**
 * Tests PasswordField behavior by exercising every key binding registered by the skin
 * at least once.
 */
public class PasswordFieldBehaviorTest extends TextInputControlTestBase<PasswordField> {

    @BeforeEach
    public void beforeEach() {
        initStage(new PasswordField());
    }

    @AfterEach
    public void afterEach() {
        closeStage();
    }

    @Test
    @Override
    public void testConsume() {
        super.testConsume();
    }

    @Test
    @Override
    public void testConsumeEnter() {
        super.testConsumeEnter();
    }

    @Test
    @Override
    public void testCopy() {
        ClipboardContent cc = new ClipboardContent();
        cc.putString("abc");
        Clipboard.getSystemClipboard().setContent(cc);

        execute(
            "012", checkSelection(3),
            shortcut(A), checkSelection(0, 3),
            // copy is disabled
            shortcut(C),
            END, checkSelection(3),
            shortcut(V), checkSelection(6),
            checkText("012abc")
        );
    }

    @Test
    @Override
    public void testCut() {
        execute(
            "0123456789", checkSelection(10),
            // select all
            shortcut(A), checkSelection(0, 10),
            // cut is disabled
            shortcut(X), checkSelection(0, 10),
            checkText("0123456789")
        );
    }

    @Test
    @Override
    public void testNavigation() {
        super.testNavigation();
    }

    @Test
    @Override
    public void testDeletion() {
        super.testDeletion();
    }

    @Test
    @Override
    public void testSelection() {
        super.testSelection();
    }

    @Test
    @Override
    public void testMacBindings() {
        super.testMacBindings();
    }

    @Test
    @Override
    public void testNonMacBindings() {
        super.testNonMacBindings();
    }

    @Test
    @Override
    public final void testWordMac() {
        if (!isMac()) {
            return;
        }

        // word navigation is disabled

        execute(
            setText("one two three"),
            // right word
            alt(RIGHT), checkSelection(0),
            alt(RIGHT), checkSelection(0),
            // left word
            END,
            alt(LEFT), checkSelection(13),
            // delete next word
            LEFT, LEFT, LEFT, LEFT, LEFT, checkSelection(8),
            alt(DELETE), checkText("one two three", 8),
            // delete prev word
            alt(BACK_SPACE), checkText("one two three", 8),

            setText(""), "one two three",
            // select left word
            key(LEFT, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(13),
            // select right word
            LEFT, LEFT, LEFT, LEFT, LEFT,
            key(RIGHT, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(8)
        );

        // keypad
        execute(
            setText("one two three"),
            // right word
            alt(KP_RIGHT), checkSelection(0),
            alt(KP_RIGHT), checkSelection(0),
            // left word
            END,
            alt(KP_LEFT), checkSelection(13),
            // delete next word
            KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT, checkSelection(8),
            alt(DELETE), checkText("one two three", 8),
            // delete prev word
            alt(BACK_SPACE), checkText("one two three", 8),

            setText(""), "one two three",
            // select left word
            key(KP_LEFT, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(13),
            // select right word
            KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT,
            key(KP_RIGHT, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(8)
        );
    }

    @Test
    @Override
    public final void testWordNonMac() {
        if (isMac()) {
            return;
        }

        // word navigation is disabled

        execute(
            setText("one two three"),
            // right word
            ctrl(RIGHT), checkSelection(0),
            ctrl(RIGHT), checkSelection(0),
            // left word
            END,
            ctrl(LEFT), checkSelection(13),
            // delete next word
            LEFT, LEFT, LEFT, LEFT, LEFT, checkSelection(8),
            ctrl(DELETE), checkText("one two three", 8),
            // delete prev word
            ctrl(BACK_SPACE), checkText("one two three", 8),

            setText(""), "one two three",
            // select left word
            key(LEFT, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(13),
            // select right word
            LEFT, LEFT, LEFT, LEFT, LEFT,
            key(RIGHT, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(8)
        );

        // keypad
        execute(
            setText("one two three"),
            // right word
            ctrl(KP_RIGHT), checkSelection(0),
            ctrl(KP_RIGHT), checkSelection(0),
            // left word
            END,
            ctrl(KP_LEFT), checkSelection(13),
            // delete next word
            KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT, checkSelection(8),
            ctrl(DELETE), checkText("one two three", 8),
            // delete prev word
            ctrl(BACK_SPACE), checkText("one two three", 8),

            setText(""), "one two three",
            // select left word
            key(KP_LEFT, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(13),
            // select right word
            KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT,
            key(KP_RIGHT, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(8)
        );
    }
}
