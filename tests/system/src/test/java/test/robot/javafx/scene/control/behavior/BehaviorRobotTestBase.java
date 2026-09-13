/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.scene.control.behavior;

import java.util.HashMap;
import java.util.function.BooleanSupplier;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import com.sun.javafx.PlatformUtil;
import test.robot.testharness.RobotTestBase;
import test.util.Util;

/**
 * Base class for testing behaviors with Robot.
 */
public abstract class BehaviorRobotTestBase<C extends Control> extends RobotTestBase {

    /** causes the execute() function to wait for idle */
    protected static final Object PULSE = new Object();
    private int step;
    private static HashMap<Character,KeyCode> keyCodes;
    protected final C control;
    private final EventHandler<KeyEvent> keyListener = (ev) -> System.out.println(ev);

    protected BehaviorRobotTestBase(C c) {
        this.control = c;
    }

    @BeforeEach
    public void beforeEach() {
        Platform.runLater(() -> {
            step = 0;
            contentPane.setCenter(control);
        });
    }

    @AfterEach
    public void afterEach() {
        Platform.runLater(() -> {
            contentPane.setCenter(null);
        });
        contentPane.removeEventFilter(KeyEvent.ANY, keyListener);
    }

    /**
     * Executes a test by emulating key press / key releases and various operations on the control being tested.
     * For each item:
     * <ul>
     * <li> if a String, emulates KEY_PRESSED + KEY_TYPED + KEY_RELEASED events for each character
     * <li> if a KeyCode, emulates KEY_PRESSED + KEY_RELEASED events for that KeyCode
     * <li> if a Runnable, runs it in the FX application thread
     * </ul>
     * @param items the sequence of KeyCodes/Runnables/String
     */
    protected void execute(Object ... items) {
        for (Object x: items) {
            if (x == PULSE) {
                Util.waitForIdle(scene);
            } else if (x instanceof Runnable r) {
                Util.runAndWait(() -> {
                    r.run();
                });
            } else if (x instanceof KeyCode k) {
                Util.runAndWait(() -> {
                    robot.keyPress(k);
                });
                Util.runAndWait(() -> {
                    robot.keyRelease(k);
                });
            } else if (x instanceof String s) {
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    KeyCode k = getKeyCodeForChar(c);
                    Util.runAndWait(() -> {
                        robot.keyPress(k);
                    });
                    Util.runAndWait(() -> {
                        robot.keyRelease(k);
                    });
                }
            }
            step++;
        }
    }

    /**
     * Looks up a KeyCode for the given character.
     * An allowed subset includes space, tab, newline, and [a-z] characters only,
     * to avoid unexpected symbols emitted by Robot with non-US keyboard layouts.
     *
     * @param c the character
     * @return the KeyCode
     * @throws RuntimeException if the character is not allowed
     */
    private static KeyCode getKeyCodeForChar(char c) {
        if (keyCodes == null) {
            keyCodes = createKeyCodes(
                " ", KeyCode.SPACE,
                "\t", KeyCode.TAB,
                "\n", KeyCode.ENTER,
                "a", KeyCode.A,
                "b", KeyCode.B,
                "c", KeyCode.C,
                "d", KeyCode.D,
                "e", KeyCode.E,
                "f", KeyCode.F,
                "g", KeyCode.G,
                "h", KeyCode.H,
                "i", KeyCode.I,
                "j", KeyCode.J,
                "k", KeyCode.K,
                "l", KeyCode.L,
                "m", KeyCode.M,
                "n", KeyCode.N,
                "o", KeyCode.O,
                "p", KeyCode.P,
                "q", KeyCode.Q,
                "r", KeyCode.R,
                "s", KeyCode.S,
                "t", KeyCode.T,
                "u", KeyCode.U,
                "v", KeyCode.V,
                "w", KeyCode.W,
                "x", KeyCode.X,
                "y", KeyCode.Y,
                "z", KeyCode.Z
            );
        }

        KeyCode code = keyCodes.get(c);
        if (code == null) {
            throw new RuntimeException(String.format("character 0x%04x is not allowed in tests", (int)c));
        }
        return code;
    }

    private static HashMap<Character, KeyCode> createKeyCodes(Object ... pairs) {
        HashMap<Character, KeyCode> m = new HashMap<>();
        for(int i=0; i<pairs.length; ) {
            char c = ((String)pairs[i++]).charAt(0);
            KeyCode code = (KeyCode)pairs[i++];
            m.put(c, code);
        }
        return m;
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

    /**
     * Returns the Runnable argument.  This method simplifies construction of execute() vararg.
     * @param r the runnable to return
     * @return the runnable
     */
    protected Runnable exe(Runnable r) {
        return r;
    }

    protected String errorMessage() {
        return "in step " + step;
    }

    /**
     * Returns a Runnable that emulates KEY_PRESS + KEY_RELEASE events with the given KeyCode
     * and the specified modifiers.
     * @param k the key code
     * @param modifiers the modifiers
     * @return the Runnable
     */
    protected Runnable key(KeyCode k, KeyModifier ... modifiers) {
        KeyCode alt = KeyModifier.findAlt(modifiers);
        KeyCode ctrl = KeyModifier.findCtrl(modifiers);
        KeyCode meta = KeyModifier.findMeta(modifiers);
        KeyCode shift = KeyModifier.findShift(modifiers);

        return () -> {
            // we don't have access to the shortcut key
            KeyCode shortcut = PlatformUtil.isMac() ? KeyCode.COMMAND : KeyCode.CONTROL;
            if (alt != null) {
                robot.keyPress(alt);
            }
            if (ctrl != null) {
                robot.keyPress(ctrl);
            }
            if (meta != null) {
                robot.keyPress(meta);
            }
            if (shift != null) {
                robot.keyPress(shift);
            }

            robot.keyPress(k);
            robot.keyRelease(k);

            if (shift != null) {
                robot.keyRelease(shift);
            }
            if (meta != null) {
                robot.keyRelease(meta);
            }
            if (ctrl != null) {
                robot.keyRelease(ctrl);
            }
            if (alt != null) {
                robot.keyRelease(alt);
            }
        };
    }

    /**
     * Returns a Runnable that emulates KEY_PRESS + KEY_RELEASE events with the given KeyCode
     * and the ALT modifier.
     * @param k the key code
     * @return the Runnable
     */
    protected Runnable alt(KeyCode k) {
        return key(k, KeyModifier.ALT);
    }

    /**
     * Returns a Runnable that emulates KEY_PRESS + KEY_RELEASE events with the given KeyCode
     * and the CTRL modifier.
     * @param k the key code
     * @return the Runnable
     */
    protected Runnable ctrl(KeyCode k) {
        return key(k, KeyModifier.CTRL);
    }

    /**
     * Returns a Runnable that emulates KEY_PRESS + KEY_RELEASE events with the given KeyCode
     * and the SHORTCUT modifier.
     * @param k the key code
     * @return the Runnable
     */
    protected Runnable shortcut(KeyCode k) {
        return key(k, KeyModifier.SHORTCUT);
    }

    /**
     * Returns a Runnable that emulates KEY_PRESS + KEY_RELEASE events with the given KeyCode
     * and the SHIFT modifier.
     * @param k the key code
     * @return the Runnable
     */
    protected Runnable shift(KeyCode k) {
        return key(k, KeyModifier.SHIFT);
    }

    /**
     * Adds a logging event filter to the control under test which prints all KeyEvent's to stdout,
     * for the duration of a single test case.
     * @return the Runnable
     */
    protected Runnable addKeyListener() {
        return () -> {
            control.addEventFilter(KeyEvent.ANY, keyListener);
        };
    }

    protected void mouseMove(double x, double y) {
        Point2D p = control.localToScreen(x, y);
        robot.mouseMove(p.getX(), p.getY());
    }
}
