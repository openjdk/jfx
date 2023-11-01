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

import java.util.Set;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;
import javafx.scene.control.behavior.BehaviorBase;
import javafx.scene.control.behavior.FunctionTag;
import javafx.scene.control.behavior.InputMap;
import javafx.scene.control.behavior.KeyBinding;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the basic functionality of InputMap.
 */
public class TestInputMap {
    private static final KeyBinding KB1 = KeyBinding.ctrl(KeyCode.C);
    private static final KeyBinding KB2 = KeyBinding.ctrl(KeyCode.J);
    private static final KeyBinding KB3 = KeyBinding.ctrl(KeyCode.V);
    private static final FunctionTag TAG1 = new FunctionTag();
    private static final FunctionTag TAG2 = new FunctionTag();

    // TODO test all public functions

    @Test
    public void testGetKeyBindings() {
        InputMap m = create(
            TAG1,
            TAG2,
            KB1, TAG1,
            KB2, TAG1,
            KB3, TAG2
        );

        Assertions.assertEquals(Set.of(KB1, KB2, KB3), m.getKeyBindings());
    }

    @Test
    public void testGetKeyBindingFor() {
        InputMap m = create(
            TAG1,
            KB1, TAG1,
            KB2, TAG1
        );

        Assertions.assertEquals(Set.of(KB1, KB2), m.getKeyBindingFor(TAG1));
        Assertions.assertEquals(Set.of(), m.getKeyBindingFor(TAG2));
    }

    @Test
    public void testRegisterFunction() {
        Runnable func = () -> { };
        Runnable defaultFunc;

        TestControl c = new TestControl();
        InputMap m = c.getInputMap();
        defaultFunc = m.getDefaultFunction(TAG1);
        Assertions.assertNotEquals(null, defaultFunc);
        Assertions.assertEquals(null, m.getDefaultFunction(TAG2));

        m.registerFunction(TAG1, func);
        Assertions.assertEquals(defaultFunc, m.getDefaultFunction(TAG1));
    }

    @Test
    public void testUnbind() {
        InputMap m = create(
            TAG1,
            KB1, TAG1,
            KB2, TAG1
        );

        m.unbind(TAG1);
        Assertions.assertEquals(Set.of(), m.getKeyBindingFor(TAG1));
    }

    // TODO test behavior-facing functions

    /**
     * Creates an input map from the list of items:
     * <pre>
     *    FunctionTag,
     *    KeyBinding, FunctionTag
     * </pre>
     * @param items
     * @return the input map
     */
    private static InputMap create(Object... items) {
        InputMap m = new InputMap(new TestControl());
        for (int i = 0; i < items.length;) {
            Object x = items[i++];
            if (x instanceof FunctionTag t) {
                m.registerFunction(t, () -> { });
            } else if (x instanceof KeyBinding kb) {
                FunctionTag t = (FunctionTag)items[i++];
                m.registerKey(kb, t);
            }
        }
        return m;
    }

    /** test control */
    static class TestControl extends Control {
        private int value = -1;

        public TestControl() {
            setSkin(new TestSkin(this));
        }

        public void setValue(int x) {
            value = x;
        }

        public int getValue() {
            return value;
        }
    }

    /** test skin */
    static class TestSkin extends  SkinBase<TestControl> {
        private TestBehavior behavior;

        protected TestSkin(TestControl c) {
            super(c);
            behavior = new TestBehavior(c);
        }

        @Override
        public void install() {
            behavior.install();
        }
    }

    /** test behavior */
    static class TestBehavior extends BehaviorBase<TestControl> {
        public TestBehavior(TestControl c) {
            super(c);
        }

        @Override
        public void install() {
            register(TAG1, KB1, () -> control.setValue(1));
            register(TAG2, KeyCode.A, () -> control.setValue(2));
        }
    }
}
