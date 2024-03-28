/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextInputControl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;

/**
 * Base class for testing behaviors based on TextInputControlBehavior.
 */
public abstract class TextInputControlTestBase<T extends TextInputControl> extends BehaviorTestBase<T> {
    protected TextInputControlTestBase() {
    }

    @Test
    public final void testTypingSanity() {
        execute(
            "hello",
            checkText("hello"),
            BACK_SPACE, BACK_SPACE, "f",
            checkText("helf")
        );
    }

    protected void testConsume() {
        Assertions.assertTrue(kb.keyPressed(HOME));
        Assertions.assertFalse(kb.keyReleased(HOME));

        Assertions.assertTrue(kb.keyPressed(A));
        Assertions.assertTrue(kb.keyTyped(A, "a"));
        Assertions.assertFalse(kb.keyReleased(A));

        Assertions.assertTrue(kb.keyPressed(DOWN));
        Assertions.assertFalse(kb.keyReleased(DOWN));

        Assertions.assertFalse(kb.keyPressed(F1));
        Assertions.assertFalse(kb.keyReleased(F1));

        Assertions.assertFalse(kb.keyPressed(ESCAPE));
        Assertions.assertFalse(kb.keyReleased(ESCAPE));

        Assertions.assertTrue(kb.keyPressed(SPACE));
        Assertions.assertTrue(kb.keyTyped(SPACE, " "));
        Assertions.assertFalse(kb.keyReleased(SPACE));
    }

    protected void testConsumeEnter() {
        Assertions.assertFalse(kb.keyPressed(ENTER));
        Assertions.assertFalse(kb.keyReleased(ENTER));
    }

    protected void testCopy() {
        execute(
            setText("abcd"),
            shift(RIGHT), checkSelection(0, 1),
            shortcut(C), checkSelection(0, 1), checkClipboard("a"),
            HOME, RIGHT, shift(RIGHT), checkSelection(1, 2),
            shortcut(INSERT), checkSelection(1, 2), checkClipboard("b"),
            HOME, RIGHT, RIGHT, shift(RIGHT), checkSelection(2, 3),
            COPY, checkSelection(2, 3), checkClipboard("c")
        );

        // keypad mappings
        execute(
            setText("abcd"),
            shift(KP_RIGHT), checkSelection(0, 1),
            shortcut(C), checkSelection(0, 1), checkClipboard("a"),
            HOME, KP_RIGHT, shift(KP_RIGHT), checkSelection(1, 2),
            shortcut(INSERT), checkSelection(1, 2), checkClipboard("b"),
            HOME, KP_RIGHT, KP_RIGHT, shift(KP_RIGHT), checkSelection(2, 3),
            COPY, checkSelection(2, 3), checkClipboard("c")
        );
    }

    protected void testCut() {
        execute(
            setText("aa"),
            shortcut(A), shortcut(X), checkText("", 0), checkClipboard("aa"),
            setText("bb"),
            shortcut(A), CUT, shortcut(X), checkText("", 0), checkClipboard("bb")
        );

        control.setEditable(false);
        execute(
            copy("yo"),
            setText("aa"),
            shortcut(A), shortcut(X), checkText("aa", 0, 2), checkClipboard("yo"),
            setText("bb"),
            shortcut(A), CUT, shortcut(X), checkText("bb", 0, 2), checkClipboard("yo")
        );
    }

    @Test
    public final void testPaste() {
        execute(
            setText(null),
            copy("1"), shortcut(V), checkText("1", 1),
            copy("2"), PASTE, checkText("12", 2),
            copy("3"), shift(INSERT), checkText("123", 3)
        );

        control.setEditable(false);
        execute(
            setText(""),
            copy("1"), shortcut(V), checkText("", 0),
            copy("2"), PASTE, checkText("", 0),
            copy("3"), shift(INSERT), checkText("", 0)
        );
    }

    protected void testNavigation() {
        execute(
            "0123456789", checkSelection(10),
            LEFT, KP_LEFT, checkSelection(8),
            RIGHT, checkSelection(9),
            KP_RIGHT, checkSelection(10),
            UP, checkSelection(0),
            DOWN, checkSelection(10),
            HOME, checkSelection(0),
            END, checkSelection(10),
            shortcut(HOME), checkSelection(0),
            shortcut(END), checkSelection(10)
        );
    }

    protected void testDeletion() {
        execute(
            setText("0123456789"),
            END, BACK_SPACE, checkText("012345678"),
            shift(BACK_SPACE), checkText("01234567"),
            HOME, DELETE, checkText("1234567")
        );

        if(!isMac()) {
            execute(
                setText("012"), END,
                // delete previous char
                ctrl(H), checkText("01", 2)
            );
        }

        control.setEditable(false);
        execute(
            setText("0123456789"),
            END, BACK_SPACE, checkText("0123456789"),
            shift(BACK_SPACE), checkText("0123456789"),
            HOME, DELETE, checkText("0123456789")
        );
    }

    protected void testSelection() {
        execute(
            setText("abc"),
            HOME, shift(RIGHT), checkSelection(0, 1),
            END, shift(LEFT), checkSelection(2, 3),
            HOME, shift(DOWN), checkSelection(0, 3),
            END, checkSelection(3), shift(UP), checkSelection(0, 3),
            HOME, checkSelection(0), shift(END), checkSelection(0, 3),
            END, checkSelection(3), shift(HOME), checkSelection(0, 3),
            HOME, checkSelection(0), shortcut(A), checkSelection(0, 3)
        );

        // keypad
        execute(
            setText("abc"),
            HOME, shift(KP_RIGHT), checkSelection(0, 1),
            END, shift(KP_LEFT), checkSelection(2, 3),
            HOME, shift(KP_DOWN), checkSelection(0, 3),
            END, checkSelection(3), shift(KP_UP), checkSelection(0, 3),
            HOME, checkSelection(0), shift(END), checkSelection(0, 3),
            END, checkSelection(3), shift(HOME), checkSelection(0, 3),
            HOME, checkSelection(0), shortcut(A), checkSelection(0, 3)
        );
    }

    @Disabled("JDK-8296266") // FIX
    @Test
    public final void testRTL() {
        execute(
            setText("العربية"),
            checkSelection(0, 0),
            RIGHT, checkSelection(1, 1)
        );
    }

    @Test
    public final void testLTR() {
        execute(
            setText("abc"),
            checkSelection(0, 0),
            RIGHT, checkSelection(1, 1)
        );
    }

    @Test
    public final void testUndoRedo() {
        execute(
            setText("b"),
            "a", checkText("ab"),
            shortcut(Z), checkText("b", 0),
            redo(),
            checkText("ab")
        );
    }

    protected Runnable redo() {
        if (isMac()) {
            return key(Z, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        } else if (isWin()) {
            return key(Y, KeyModifier.CTRL);
        } else {
            return key(Z, KeyModifier.CTRL, KeyModifier.SHIFT);
        }
    }

    protected void testMacBindings() {
        if (!isMac()) {
            return;
        }

        execute(
            setText("abc"),
            // select end extend
            shift(END), checkSelection(0, 3),
            // home
            shortcut(LEFT), checkSelection(0),
            // end
            shortcut(RIGHT), checkSelection(3),
            // select home extend
            shift(HOME), checkSelection(0, 3),
            // select home extend
            END, key(LEFT, KeyModifier.getShortcutKey(), KeyModifier.SHIFT), checkSelection(0, 3),
            // select end extend
            HOME, key(RIGHT, KeyModifier.getShortcutKey(), KeyModifier.SHIFT), checkSelection(0, 3)
        );

        // keypad
        execute(
            setText("abc"),
            // select end extend
            shift(END), checkSelection(0, 3),
            // home
            shortcut(KP_LEFT), checkSelection(0),
            // end
            shortcut(KP_RIGHT), checkSelection(3),
            // select home extend
            shift(HOME), checkSelection(0, 3),
            // select home extend
            END, key(KP_LEFT, KeyModifier.getShortcutKey(), KeyModifier.SHIFT), checkSelection(0, 3),
            // select end extend
            HOME, key(KP_RIGHT, KeyModifier.getShortcutKey(), KeyModifier.SHIFT), checkSelection(0, 3)
        );
    }

    protected void testNonMacBindings() {
        if (isMac()) {
            return;
        }

        execute(
            setText("abc"),
            // select end
            shift(END), checkSelection(0, 3),
            // select home
            END, shift(HOME), checkSelection(0, 3),
            // deselect
            ctrl(BACK_SLASH), checkSelection(0)
        );
    }

    protected void testWordMac() {
        if (!isMac()) {
            return;
        }

        execute(
            setText("one two three"),
            // right word
            alt(RIGHT), checkSelection(3),
            alt(RIGHT), checkSelection(7),
            // left word
            alt(LEFT), checkSelection(4),
            // delete next word
            alt(DELETE), checkText("one  three", 4),
            // delete prev word
            alt(BACK_SPACE), checkText(" three", 0),

            setText(""), "one two three",
            // select left word
            key(LEFT, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(8, 13),
            // select right word
            LEFT, LEFT, LEFT, LEFT, LEFT,
            key(RIGHT, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(4, 7)
        );

        // keypad
        execute(
            setText("one two three"),
            // right word
            alt(KP_RIGHT), checkSelection(3),
            alt(KP_RIGHT), checkSelection(7),
            // left word
            alt(KP_LEFT), checkSelection(4),
            // delete next word
            alt(DELETE), checkText("one  three", 4),
            // delete prev word
            alt(BACK_SPACE), checkText(" three", 0),

            setText(""), "one two three",
            // select left word
            key(KP_LEFT, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(8, 13),
            // select right word
            KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT,
            key(KP_RIGHT, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(4, 7)
        );
    }

    protected void testWordNonMac() {
        if (isMac()) {
            return;
        }

        boolean win = isWin();

        execute(
            setText("one two three"),
            // right word
            ctrl(RIGHT), checkSelection(win ? 4 : 3),
            ctrl(RIGHT), checkSelection(win ? 8 : 7),
            // left word
            ctrl(LEFT), checkSelection(4),
            // delete next word
            ctrl(DELETE), checkText(win ? "one three" : "one  three", 4),
            // delete prev word
            ctrl(BACK_SPACE), checkText(win ? "three" : " three", 0),

            setText(""), "one two three",
            // select left word
            key(LEFT, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(8, 13),
            // select right word
            LEFT, LEFT, LEFT, LEFT, LEFT,
            key(RIGHT, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(4, win ? 8 : 7)
        );

        // keypad
        execute(
            setText("one two three"),
            // right word
            ctrl(KP_RIGHT), checkSelection(win ? 4 : 3),
            ctrl(KP_RIGHT), checkSelection(win ? 8 : 7),
            // left word
            ctrl(KP_LEFT), checkSelection(4),
            // delete next word
            ctrl(DELETE), checkText(win ? "one three" : "one  three", 4),
            // delete prev word
            ctrl(BACK_SPACE), checkText(win ? "three" : " three", 0),

            setText(""), "one two three",
            // select left word
            key(KP_LEFT, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(8, 13),
            // select right word
            KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT, KP_LEFT,
            key(RIGHT, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(4, win ? 8 : 7)
        );
    }

    /**
     * Returns a Runnable that sets the specified text on the control.
     * @param text the text to set
     * @return the Runnable
     */
    protected Runnable setText(String text) {
        return () -> {
            control.setText(text);
        };
    }

    /**
     * Returns a Runnable that checks the control's text against the expected value.
     * @param expected the expected text
     * @return the Runnable
     */
    protected Runnable checkText(String expected) {
        return () -> {
            String v = control.getText();
            Assertions.assertEquals(expected, v, errorMessage());
        };
    }

    /**
     * Returns a Runnable that checks the control's text and selection indexes against the expected values.
     * @param expected the expected text
     * @param start the expected selection start index
     * @param end the expected selection end index
     * @return the Runnable
     */
    protected Runnable checkText(String expected, int start, int end) {
        return () -> {
            String s = control.getText();
            Assertions.assertEquals(expected, s, errorMessage());

            IndexRange v = control.getSelection();
            IndexRange expectedSelection = new IndexRange(start, end);
            Assertions.assertEquals(expectedSelection, v, errorMessage());
        };
    }

    /**
     * Returns a Runnable that checks the control's text and selection indexes against the expected values.
     * @param expected the expected text
     * @param index the expected selection start and end index
     * @return the Runnable
     */
    protected Runnable checkText(String expected, int index) {
        return checkText(expected, index, index);
    }

    /**
     * Returns a Runnable that checks the control's selection indexes against the expected values.
     * @param start the expected selection start index
     * @param end the expected selection end index
     * @return the Runnable
     */
    protected Runnable checkSelection(int start, int end) {
        return () -> {
            IndexRange v = control.getSelection();
            IndexRange expected = new IndexRange(start, end);
            Assertions.assertEquals(expected, v, errorMessage());
        };
    }

    /**
     * Returns a Runnable that checks the control's selection indexe against the expected value.
     * @param index the expected selection start and end index
     * @return the Runnable
     */
    protected Runnable checkSelection(int index) {
        return checkSelection(index, index);
    }
}
