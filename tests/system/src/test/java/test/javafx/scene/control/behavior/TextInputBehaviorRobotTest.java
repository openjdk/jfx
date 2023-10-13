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

import javafx.scene.control.IndexRange;
import javafx.scene.control.TextInputControl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for Robot testing TextInputControl descendants' behavior.
 */
public abstract class TextInputBehaviorRobotTest<C extends TextInputControl> extends BehaviorRobotTestBase<C> {

    protected TextInputBehaviorRobotTest(C control) {
        super(control);
    }

    @BeforeEach
    @Override
    public void beforeEach() {
        super.beforeEach();
        // a good initial state
        control.setText("");
        control.setEditable(true);
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

    /**
     * Returns a Runnable that checks the control's selection indexes against the specified testers.
     * @param tester the selection tester
     * @return the Runnable
     */
    protected Runnable checkSelection(SelectionChecker tester) {
        return () -> {
            IndexRange r = control.getSelection();
            Assertions.assertTrue(tester.test(r.getStart(), r.getEnd()), errorMessage() + " (selection=" + r + ")");
        };
    }

    @FunctionalInterface
    public static interface SelectionChecker {
        public boolean test(int start, int end);
    }
}
