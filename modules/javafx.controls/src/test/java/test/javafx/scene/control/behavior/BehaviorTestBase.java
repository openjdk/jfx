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

import java.util.function.BooleanSupplier;
import javafx.scene.control.Control;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Assertions;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

/**
 * Base class for the Control Behavior tests.
 */
public abstract class BehaviorTestBase<C extends Control> {

    protected C control;
    protected StageLoader stageLoader;
    // TODO problem:
    // KeyEventFirer may not a good idea here because of the way it generates events.
    // I think we should rather emulate the keyboard, such that the events match those sent by the real thing
    // i.e. press(SHORTCUT), hit(X), release(SHORTCUT)
    protected KeyEventFirer kb;
    private int step;

    protected BehaviorTestBase() {
    }

    /**
     * Must be called in each test's <code>&#x40;BeforeEach</code> method:
     * <pre>
     *     &#x40;BeforeEach
     *     public void beforeEach() {
     *         initStage(new ACTUAL_CONTROL());
     *     }
     * <pre>
     * @param control the control being tested
     */
    protected void initStage(C c) {
        this.control = c;
        stageLoader = new StageLoader(c);
        kb = new KeyEventFirer(c);
        c.requestFocus();
        Toolkit.getToolkit().firePulse();
    }

    /**
     * Must be called in each test's <code>&#x40;AfterEach</code> method:
     * <pre>
     *     &#x40;AfterEach
     *     public void afterEach() {
     *         closeStage();
     *     }
     * <pre>
     * @param control the control being tested
     */
    protected void closeStage() {
        if (stageLoader != null) {
            stageLoader.dispose();
            stageLoader = null;
        }
    }

    /**
     * Returns the Control being tested.
     * @return the control
     */
    public C control() {
        return control;
    }

    /**
     * Returns a Runnable that emulates KEY_PRESS + KEY_RELEASE events with the given KeyCode
     * and the ALT modifier.
     * @param k the key code
     * @return the Runnable
     */
    protected Runnable alt(KeyCode k) {
        return () -> {
            kb.keyPressed(k, KeyModifier.ALT);
            kb.keyReleased(k, KeyModifier.ALT);
        };
    }

    /**
     * Returns a Runnable that emulates KEY_PRESS + KEY_RELEASE events with the given KeyCode
     * and the CTRL modifier.
     * @param k the key code
     * @return the Runnable
     */
    protected Runnable ctrl(KeyCode k) {
        return () -> {
            kb.keyPressed(k, KeyModifier.CTRL);
            kb.keyReleased(k, KeyModifier.CTRL);
        };
    }

    /**
     * Returns a Runnable that emulates KEY_PRESS + KEY_RELEASE events with the given KeyCode
     * and the SHIFT modifier.
     * @param k the key code
     * @return the Runnable
     */
    protected Runnable shift(KeyCode k) {
        return () -> {
            kb.keyPressed(k, KeyModifier.SHIFT);
            kb.keyReleased(k, KeyModifier.SHIFT);
        };
    }

    /**
     * Returns a Runnable that emulates KEY_PRESS + KEY_RELEASE events with the given KeyCode
     * and the SHORTCUT modifier.
     * @param k the key code
     * @return the Runnable
     */
    protected Runnable shortcut(KeyCode k) {
        return () -> {
            kb.keyPressed(k, KeyModifier.getShortcutKey());
            kb.keyReleased(k, KeyModifier.getShortcutKey());
        };
    }

    /**
     * Returns a Runnable that emulates KEY_PRESS + KEY_RELEASE events with the given KeyCode
     * and the specified modifiers.
     * @param k the key code
     * @param modifiers the key modifiers
     * @return the Runnable
     */
    protected Runnable key(KeyCode k, KeyModifier ... modifiers) {
        return () -> {
            kb.keyPressed(k, modifiers);
            kb.keyReleased(k, modifiers);
        };
    }

    /**
     * Returns a Runnable that, when executed, checks the result of the specified boolean operation.
     * @param test the operation
     * @return the Runnable
     */
    protected Runnable check(BooleanSupplier test) {
        return () -> {
            boolean result = test.getAsBoolean();
            Assertions.assertTrue(result, errorMessage());
        };
    }

    protected Runnable exe(Runnable r) {
        return r;
    }

    protected String errorMessage() {
        return "in step " + step;
    }

    /**
     * Executes a test by emulating key press / key releases and various operations on the control being tested.
     * For each item:
     * <ul>
     * <li> if a String, emulates KEY_PRESSED + KEY_TYPED + KEY_RELEASED events for each character
     * <li> if a KeyCode, emulates KEY_PRESSED + KEY_RELEASED events for that KeyCode
     * <li> if a Runnable, runs it
     * </ul>
     * @param items the sequence of KeyCodes/Runnables/String
     */
    protected void execute(Object ... items) {
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

    /**
     * Returns a Runnable that checks the clipboard content against the given text.
     * @param expected the expected clipboard content
     * @return the Runnable
     */
    protected Runnable checkClipboard(String expected) {
        return () -> {
            Clipboard c = Clipboard.getSystemClipboard();
            String v = c.getString();
            Assertions.assertEquals(expected, v, errorMessage());
        };
    }

    /**
     * Returns a Runnable that copies the specified text to the system clipboard.
     * @param text the text to copy
     * @return the Runnable
     */
    protected Runnable copy(String text) {
        return () -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(text);
            Clipboard.getSystemClipboard().setContent(cc);
        };
    }

    /**
     * Returns true if the platform is macOS.
     * @return true if platform is a Mac
     */
    protected boolean isMac() {
        return PlatformUtil.isMac();
    }

    /**
     * Returns true if the platform is Windows.
     * @return true if platform is Windows
     */
    protected boolean isWin() {
        return PlatformUtil.isWindows();
    }

    /**
     * Returns true if the platform is Linux.
     * @return true if platform is Linux
     */
    protected boolean isLinux() {
        return PlatformUtil.isLinux();
    }
}
