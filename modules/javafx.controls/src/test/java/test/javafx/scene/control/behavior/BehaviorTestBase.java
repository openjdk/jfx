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
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
    protected void initStage(C control) {
        this.control = control;
        stageLoader = new StageLoader(control);
        kb = new KeyEventFirer(control);
        control.requestFocus();
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

    public C control() {
        return control;
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

    protected Runnable exe(BooleanSupplier test) {
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
     * Executes a test by emulating key press / key releases and various operations upon control.
     * @param items the sequence of KeyCodes/Runnables
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
}
