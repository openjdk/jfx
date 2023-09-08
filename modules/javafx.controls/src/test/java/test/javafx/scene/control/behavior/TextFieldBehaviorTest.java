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
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests the TextField behavior using public APIs.
 */
public class TextFieldBehaviorTest extends TextInputControlTestBase<TextField> {

    @BeforeEach
    public void beforeEach() {
        initStage(new TextField());
    }

    @AfterEach
    public void afterEach() {
        closeStage();
    }

    @Test
    public void testTypingSanity() {
        execute(
            "hello",
            checkText("hello"),
            BACK_SPACE, BACK_SPACE, "f",
            checkText("helf")
        );
    }

    /** tests event consumption logic */
    @Test
    public void testConsume() {
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

        Assertions.assertFalse(kb.keyPressed(ENTER));
        Assertions.assertFalse(kb.keyReleased(ENTER));

        Assertions.assertTrue(kb.keyPressed(SPACE));
        Assertions.assertTrue(kb.keyTyped(SPACE, " "));
        Assertions.assertFalse(kb.keyReleased(SPACE));
    }
    
//    @Test
//    public void testCopyCutPaste2() {
//        execute(
//            setText("01"), shortcut(A), shortcut(C),
//            shortcut(INSERT), checkClipboard("01")
//        );
//    }
    
    @Test
    public void testCopy() {
        execute(
            setText("abcd"),
            shift(RIGHT), checkSelection(0, 1),
            shortcut(C), checkSelection(0, 1), checkClipboard("a"),
            HOME, RIGHT, shift(RIGHT), checkSelection(1, 2),
            shortcut(INSERT), checkSelection(1, 2), checkClipboard("b"),
            HOME, RIGHT, RIGHT, shift(RIGHT), checkSelection(2, 3),
            COPY, checkSelection(2, 3), checkClipboard("c")
        );
    }

    @Test
    public void testCut() {
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
    public void testPaste() {
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

    @Test
    public void testNavigation() {
        execute(
            "0123456789", checkSelection(10),
            LEFT, LEFT, checkSelection(8),
            RIGHT, checkSelection(9),
            UP, checkSelection(0),
            DOWN, checkSelection(10),
            HOME, checkSelection(0),
            END, checkSelection(10),
            shortcut(HOME), checkSelection(0),
            shortcut(END), checkSelection(10)
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

        control.setEditable(false);
        execute(
            setText("0123456789"),
            END, BACK_SPACE, checkText("0123456789"),
            shift(BACK_SPACE), checkText("0123456789"),
            HOME, DELETE, checkText("0123456789")
        );
    }            

    @Disabled("JDK-8296266") // FIX
    @Test
    public void testRTL() {
        execute(
            setText("العربية"),
            checkSelection(0, 0),
            RIGHT, checkSelection(1, 1)
        );
    }

    @Test
    public void testLTR() {
        execute(
            setText("abc"),
            checkSelection(0, 0),
            RIGHT, checkSelection(1, 1)
        );
    }
}
