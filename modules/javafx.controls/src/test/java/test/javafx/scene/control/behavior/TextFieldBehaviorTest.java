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

import static javafx.scene.input.KeyCode.A;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.C;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.END;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.F1;
import static javafx.scene.input.KeyCode.HOME;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.V;
import static javafx.scene.input.KeyCode.X;
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

    /**
     * Tests basic typing.
     */
    @Test
    public void testTyping() {
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

    /**
     * this is an example of a test script I have in mind.
     * perhaps we need to extract helper methods into a separate class in
     * package test.com.sun.javafx.scene.control.infrastructure;
     */
    @Test
    public void testCopyCutPaste() {
        execute(
            setText("copy-cut=paste."),
            HOME, checkSelection(0, 0),
            shift(RIGHT), shift(RIGHT), shift(RIGHT), shift(RIGHT), shift(RIGHT),
            checkSelection(0, 5),
            shortcut(X),
            checkText("cut=paste."),
            END, shortcut(V),
            checkText("cut=paste.copy-"),
            shift(LEFT), shift(LEFT), shift(LEFT), shift(LEFT), shift(LEFT), shortcut(C),
            HOME, shortcut(V),
            checkText("copy-cut=paste.copy-")
        );
    }

    @Test
    public void testNavigation() {
        execute(
            "abc", checkSelection(3, 3),
            LEFT, LEFT, checkSelection(1, 1),
            "0", checkText("a0bc")
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
