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
package test.javafx.scene.control;

import static javafx.scene.input.KeyCode.*;
import java.util.function.BooleanSupplier;
import javafx.event.EventType;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

/**
 * Tests the TextField behavior using public APIs.
 */
public class TextFieldBehaviorTest {
    private static final EventType<KeyEvent> PRE = KeyEvent.KEY_PRESSED;
    private static final EventType<KeyEvent> TYP = KeyEvent.KEY_TYPED;
    private static final EventType<KeyEvent> REL = KeyEvent.KEY_RELEASED;
    private TextField control;
    private StageLoader stageLoader;
    // TODO problem:
    // KeyEventFirer may not a good idea here because of the way it generates events.
    // I think we should rather emulate the keyboard, such that the events match those sent by the real thing
    // i.e. press(SHORTCUT), hit(X), release(SHORTCUT)
    private KeyEventFirer kb;
    private int step;

    @BeforeEach
    public void before() {
        control = new TextField();
        stageLoader = new StageLoader(control);
        kb = new KeyEventFirer(control);
        control.requestFocus();
        Toolkit.getToolkit().firePulse();
    }

    @AfterEach
    public void after() {
        stageLoader.dispose();
    }

    /**
     * Tests basic typing.
     */
    @Test
    public void testTyping() {
        kb.type("hello");
        check("hello");
        kb.type(BACK_SPACE, BACK_SPACE, "f");
        check("helf");
    }

    private void check(String text) {
        String s = control.getText();
        Assertions.assertEquals(text, s);
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
        t(
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
        t(
            "abc", checkSelection(3, 3),
            LEFT, LEFT, checkSelection(1, 1),
            "0", checkText("a0bc")
        );
    }

    //@Test // FIX JDK-8296266
    public void testRTL() {
        t(
            setText("العربية"),
            checkSelection(0, 0),
            RIGHT, checkSelection(1, 1)
        );
    }

    @Test
    public void testLTR() {
        t(
            setText("abc"),
            checkSelection(0, 0),
            RIGHT, checkSelection(1, 1)
        );
    }

    // TODO perhaps this should be extracted into a separate class BehaviorTestRunner<T extends Control>

    protected Runnable setText(String text) {
        return () -> {
            control.setText(text);
        };
    }

    protected Runnable checkText(String expected) {
        return () -> {
            String v = control.getText();
            Assertions.assertEquals(expected, v, errorMessage());
        };
    }

    protected Runnable checkSelection(int start, int end) {
        return () -> {
            IndexRange v = control.getSelection();
            IndexRange expected = new IndexRange(start, end);
            Assertions.assertEquals(expected, v, errorMessage());
        };
    }

    protected Runnable shift(KeyCode k) {
        return () -> {
            kb.keyPressed(k, KeyModifier.SHIFT);
            kb.keyReleased(k, KeyModifier.SHIFT);
        };
    }

    protected Runnable shortcut(KeyCode k) {
        return () -> {
            kb.keyPressed(k, KeyModifier.getShortcutKey());
            kb.keyReleased(k, KeyModifier.getShortcutKey());
        };
    }

    protected Runnable x(BooleanSupplier test) {
        return () -> {
            boolean result = test.getAsBoolean();
            Assertions.assertTrue(result, errorMessage());
        };
    }

    protected String errorMessage() {
        return "in step " + step;
    }

    /**
     * Executes a test by emulating key press / key releases and various operations upon control.
     * @param items the sequence of KeyCodes/Runnables
     */
    protected void t(Object ... items) {
        step = 0;
        for(Object x: items) {
            if(x instanceof Runnable r) {
                r.run();
                Toolkit.getToolkit().firePulse();
            } else if(x instanceof KeyCode k) {
                kb.keyPressed(k);
                kb.keyReleased(k);
            } else if(x instanceof String s) {
                kb.type(s);
            }
            step++;
        }
    }
}
