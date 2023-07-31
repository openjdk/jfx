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
import javafx.event.EventType;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
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
    private KeyEventFirer kb;

    @Before
    public void before() {
        control = new TextField();
        stageLoader = new StageLoader(control);
        kb = new KeyEventFirer(control);
    }

    @After
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
        Assert.assertEquals(text, s);
    }
    
    /** tests event consumption logic */
    @Test
    public void testConsume() {
        Assert.assertTrue(kb.keyPressed(HOME));
        Assert.assertFalse(kb.keyReleased(HOME));

        Assert.assertTrue(kb.keyPressed(A));
        Assert.assertTrue(kb.keyTyped(A, "a"));
        Assert.assertFalse(kb.keyReleased(A));

        Assert.assertTrue(kb.keyPressed(DOWN));
        Assert.assertFalse(kb.keyReleased(DOWN));

        Assert.assertFalse(kb.keyPressed(F1));
        Assert.assertFalse(kb.keyReleased(F1));

        Assert.assertFalse(kb.keyPressed(ESCAPE));
        Assert.assertFalse(kb.keyReleased(ESCAPE));
    }
}
