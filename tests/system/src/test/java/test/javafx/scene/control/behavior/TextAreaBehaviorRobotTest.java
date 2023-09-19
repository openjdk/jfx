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
import javafx.scene.control.TextArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.javafx.PlatformUtil;

/**
 * Tests TextArea behavior by exercising every key binding not covered by TextAreaBehaviorTest,
 * since the mapped functions require rendered text.
 */
public class TextAreaBehaviorRobotTest extends TextInputBehaviorRobotTest<TextArea> {

    public TextAreaBehaviorRobotTest() {
        super(new TextArea());
    }

    @BeforeEach
    @Override
    public void beforeEach() {
        super.beforeEach();
        control.setWrapText(true);
    }

    @Test
    public void testTyping() throws Exception {
        execute(
            //addKeyListener(),
            checkText("", 0),
            " \t\n abracadabra",
            checkText(" \t\n abracadabra", 15)
        );
    }

    @Test
    public void testNavigation() throws Exception {
        execute(
            setText("0123456789"),
            END, checkSelection(10),
            LEFT, LEFT, checkSelection(8),
            RIGHT, checkSelection(9),
            HOME, checkSelection(0),
            END, checkSelection(10),
            // home
            shortcut(HOME), checkSelection(0),
            // end
            shortcut(END), checkSelection(10)
        );

        /* FIX JDK-8316307
        // keypad
        execute(
            //addKeyListener(),
            setText("0123456789"), checkSelection(0),
            END, checkSelection(10),
            KP_LEFT, KP_LEFT, checkSelection(8), // FIX fails
            KP_RIGHT, checkSelection(9),
            HOME, checkSelection(0),
            END, checkSelection(10),
            // home
            shortcut(HOME), checkSelection(0),
            // end
            shortcut(END), checkSelection(10)
        );
        */

        execute(
            setText("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n==\n"),
            checkSelection(0),
            // actual result depends on window/font size, so we simply check that the cursor has moved
            PAGE_DOWN, check(() -> {
                return control.getSelection().getStart() != 0;
            }),
            PAGE_UP, checkSelection(0)
        );

        execute(
            setText("1ab\n2bc\n3de"),
            shortcut(HOME), checkSelection(0),
            END, checkSelection(3),
            DOWN, checkSelection(7),
            HOME, checkSelection(4),
            UP, checkSelection(0),
            shortcut(END), checkSelection(11)
        );
    }

    @Test
    public void testDeletion() {
        execute(
            setText("0123456789"),
            END, BACK_SPACE, checkText("012345678"),
            shift(BACK_SPACE), checkText("01234567"),
            HOME, DELETE, checkText("1234567")
        );

        if(!PlatformUtil.isMac()) {
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

    @Test
    public void testSelection() {
        execute(
            //addKeyListener(),
            setText("123\n456"), checkSelection(0),
            HOME, shift(RIGHT), checkSelection(0, 1),
            END, shift(LEFT), checkSelection(2, 3),
            HOME, shift(DOWN), checkSelection(0, 4),
            END, checkSelection(7), shift(UP), checkSelection(3, 7),
            HOME, checkSelection(0), shift(END), checkSelection(0, 3),
            END, checkSelection(3), shift(HOME), checkSelection(0, 3),
            HOME, checkSelection(0), shortcut(A), checkSelection(0, 7)
        );

        /* FIX JDK-8316307
        // keypad
        execute(
            setText("123\n456"), checkSelection(0),
            HOME, shift(KP_RIGHT), checkSelection(0, 1), // FIX fails step 29
            END, shift(KP_LEFT), checkSelection(2, 3),
            HOME, shift(KP_DOWN), checkSelection(0, 4),
            END, checkSelection(7), shift(KP_UP), checkSelection(3, 7),
            HOME, checkSelection(0), shift(END), checkSelection(0, 3),
            END, checkSelection(3), shift(HOME), checkSelection(0, 3),
            HOME, checkSelection(0), shortcut(A), checkSelection(0, 7)
        );
        */

        execute(
            setText("1\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n====\n"),
            checkSelection(0),
            shift(PAGE_DOWN),
            checkSelection((start, end) -> {
                return (start == 0) && (end > 0);
            }),
            PAGE_DOWN,
            shift(PAGE_UP),
            checkSelection((start, end) -> {
                return
                    (start != end) &&
                    (end < control.getLength());
            })
        );
    }

    @Test
    public void testMacBindings() {
        if (!PlatformUtil.isMac()) {
            return;
        }

        // text input control mappings
        execute(
            setText("1ab\n2cd\n3de"),
            // select end extend
            shift(END), checkSelection(0, 3),
            // home
            shortcut(LEFT), checkSelection(0),
            // end
            shortcut(RIGHT), checkSelection(3),
            // select home extend
            shift(HOME), checkSelection(0, 3),
            // select home extend
            END, key(LEFT, KeyModifier.SHORTCUT, KeyModifier.SHIFT), checkSelection(0, 3),
            // select end extend
            HOME, key(RIGHT, KeyModifier.SHORTCUT, KeyModifier.SHIFT), checkSelection(0, 3)
        );

        /* FIX JDK-8316307
        // keypad
        execute(
            //addKeyListener(),
            setText("1ab\n2cd\n3de"),
            // select end extend
            shift(END), checkSelection(0, 3),
            // home
            shortcut(KP_LEFT), checkSelection(0), // FIX fails
            // end
            shortcut(KP_RIGHT), checkSelection(3),
            // select home extend
            shift(HOME), checkSelection(0, 3),
            // select home extend
            END, key(KP_LEFT, Mod.SHORTCUT, Mod.SHIFT), checkSelection(0, 3),
            // select end extend
            HOME, key(KP_RIGHT, Mod.SHORTCUT, Mod.SHIFT), checkSelection(0, 3)
        );
        */

        // delete from line start
        execute(
            setText("aaa bbb\nccc ddd"), shortcut(DOWN), checkSelection(15),
            shortcut(BACK_SPACE), checkText("aaa bbb\n", 8)
        );

        // text area mappings
        execute(
            setText("1ab\n2cd\n3de"), shortcut(DOWN), checkSelection(11),
            // line start
            UP, shortcut(LEFT), checkSelection(4),
            shortcut(RIGHT), checkSelection(7),
            // home
            shortcut(UP), checkSelection(0),
            // end
            shortcut(DOWN), checkSelection(11),
            // select line start
            key(LEFT, KeyModifier.SHIFT, KeyModifier.SHORTCUT), checkSelection(8, 11),
            // select line end
            HOME,
            key(RIGHT, KeyModifier.SHIFT, KeyModifier.SHORTCUT), checkSelection(8, 11),
            // select home extend
            key(UP, KeyModifier.SHIFT, KeyModifier.SHORTCUT), checkSelection(0, 11),
            // select end extend
            shortcut(UP), key(DOWN, KeyModifier.SHIFT, KeyModifier.SHORTCUT), checkSelection(0, 11)
        );

        /* FIX JDK-8316307
        // keypad
        execute(
            setText("1ab\n2cd\n3de"), shortcut(KP_DOWN), checkSelection(11),
            // line start
            KP_UP, shortcut(LEFT), checkSelection(4),
            shortcut(KP_RIGHT), checkSelection(7),
            // home
            shortcut(KP_UP), checkSelection(0),
            // end
            shortcut(KP_DOWN), checkSelection(11),
            // select line start
            key(KP_LEFT, Mod.SHIFT, Mod.SHORTCUT), checkSelection(8, 11),
            // select line end
            HOME,
            key(KP_RIGHT, Mod.SHIFT, Mod.SHORTCUT), checkSelection(8, 11),
            // select home extend
            key(KP_UP, Mod.SHIFT, Mod.SHORTCUT), checkSelection(0, 11),
            // select end extend
            shortcut(KP_UP), key(KP_DOWN, Mod.SHIFT, Mod.SHORTCUT), checkSelection(0, 11)
        );
        */

        // paragraph
        execute(
            exe(() -> control.setWrapText(true)),
            setText(
                "aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa\n" +
                "bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb\n" +
                "cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc\n"
            ),
            // move
            alt(DOWN), checkSelection(87),
            alt(DOWN), checkSelection(175),
            alt(DOWN), checkSelection(263),
            alt(UP), checkSelection(176),
            alt(UP), checkSelection(88),
            // select
            shortcut(UP), checkSelection(0),
            key(DOWN, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(0, 87),
            key(DOWN, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(0, 175),
            key(DOWN, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(0, 263),
            key(UP, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(0, 176),
            key(UP, KeyModifier.ALT, KeyModifier.SHIFT), checkSelection(0, 88)
        );

        /* FIX JDK-8316307
        // keypad
        execute(
            exe(() -> control.setWrapText(true)),
            setText(
                "aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa\n" +
                "bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb\n" +
                "cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc\n"
            ),
            // move
            alt(KP_DOWN), checkSelection(87),
            alt(KP_DOWN), checkSelection(175),
            alt(KP_DOWN), checkSelection(263),
            alt(KP_UP), checkSelection(176),
            alt(KP_UP), checkSelection(88),
            // select
            shortcut(KP_UP), checkSelection(0),
            key(KP_DOWN, Mod.ALT, Mod.SHIFT), checkSelection(0, 87),
            key(KP_DOWN, Mod.ALT, Mod.SHIFT), checkSelection(0, 175),
            key(KP_DOWN, Mod.ALT, Mod.SHIFT), checkSelection(0, 263),
            key(KP_UP, Mod.ALT, Mod.SHIFT), checkSelection(0, 176),
            key(KP_UP, Mod.ALT, Mod.SHIFT), checkSelection(0, 88)
        );
        */
    }

    @Test
    public void testNonMacBindings() {
        if (PlatformUtil.isMac()) {
            return;
        }

        // paragraph
        execute(
            exe(() -> control.setWrapText(true)),
            setText(
                "aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa\n" +
                "bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb\n" +
                "cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc\n"
            ),
            // move
            ctrl(DOWN), checkSelection(88),
            ctrl(DOWN), checkSelection(176),
            ctrl(DOWN), checkSelection(264),
            ctrl(UP), checkSelection(176),
            ctrl(UP), checkSelection(88),
            // select
            shortcut(UP), checkSelection(0),
            key(DOWN, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(0, 88),
            key(DOWN, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(0, 176),
            key(DOWN, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(0, 264),
            key(UP, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(0, 176),
            key(UP, KeyModifier.CTRL, KeyModifier.SHIFT), checkSelection(0, 88)
        );

        /* FIX JDK-8316307
        // keypad
        execute(
            exe(() -> control.setWrapText(true)),
            setText(
                "aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa aaaaaaaaaa\n" +
                "bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb bbbbbbbbbb\n" +
                "cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc cccccccccc\n"
            ),
            // move
            ctrl(KP_DOWN), checkSelection(88),
            ctrl(KP_DOWN), checkSelection(176),
            ctrl(KP_DOWN), checkSelection(264),
            ctrl(KP_UP), checkSelection(176),
            ctrl(KP_UP), checkSelection(88),
            // select
            shortcut(UP), checkSelection(0),
            key(KP_DOWN, Mod.CTRL, Mod.SHIFT), checkSelection(0, 88),
            key(KP_DOWN, Mod.CTRL, Mod.SHIFT), checkSelection(0, 176),
            key(KP_DOWN, Mod.CTRL, Mod.SHIFT), checkSelection(0, 264),
            key(KP_UP, Mod.CTRL, Mod.SHIFT), checkSelection(0, 176),
            key(KP_UP, Mod.CTRL, Mod.SHIFT), checkSelection(0, 88)
        );
        */
    }

    @Test
    public void testEditing() {
        execute(
            setText("a"), END,
            TAB, checkText("a\t", 2),
            ENTER, checkText("a\t\n", 3)
        );

        // not editable
        control.setText(null);
        control.setEditable(false);
        execute(
            "\t", checkText(null),
            ENTER, checkText(null)
        );
    }

    public void testWordMac() {
        // tested by headless test, see TextAreaBehaviorTest
    }

    public void testWordNonMac() {
        // tested by headless test, see TextAreaBehaviorTest
    }
}
