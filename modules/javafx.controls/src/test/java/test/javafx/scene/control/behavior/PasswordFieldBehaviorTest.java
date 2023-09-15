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

import javafx.scene.control.PasswordField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests PasswordField behavior by exercising every key binding registered by the skin
 * at least once.
 */
public class PasswordFieldBehaviorTest extends TextInputControlTestBase<PasswordField> {

    @BeforeEach
    public void beforeEach() {
        initStage(new PasswordField());
    }

    @AfterEach
    public void afterEach() {
        closeStage();
    }

    @Test
    @Override
    public void testConsume() {
        super.testConsume();
    }

    @Test
    @Override
    public void testConsumeEnter() {
        super.testConsumeEnter();
    }

    @Test
    @Override
    public void testCopy() {
        // copy is disabled
    }

    @Test
    @Override
    public void testCut() {
        // cut is disabled
    }

    @Test
    @Override
    public void testNavigation() {
        super.testNavigation();
    }

    @Test
    @Override
    public void testDeletion() {
        super.testDeletion();
    }

    @Test
    @Override
    public void testSelection() {
        super.testSelection();
    }

    @Test
    @Override
    public void testMacBindings() {
        super.testMacBindings();
    }

    @Test
    @Override
    public void testNonMacBindings() {
        super.testNonMacBindings();
    }

    @Test
    @Override
    public final void testWordMac() {
        // word navigation is disabled
    }

    @Test
    @Override
    public final void testWordNonMac() {
        // word navigation is disabled
    }
}
