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
import javafx.scene.control.TextArea;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests the TextArea behavior using public APIs.
 *
 * Note: some aspects of behavior (navigation, selection) require a fully rendered skin,
 * so it is impossible to test in headless environment.
 */
public class TextAreaBehaviorTest extends TextInputControlTestBase<TextArea> {

    @BeforeEach
    public void beforeEach() {
        initStage(new TextArea());
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
        // n/a
    }

    @Test
    @Override
    public void testCopy() {
        super.testCopy();
    }

    @Test
    @Override
    public void testCut() {
        super.testCut();
    }

    @Test
    @Override
    public void testNavigation() {
        // needs graphics
    }

    @Test
    @Override
    public void testDeletion() {
        // needs graphics
    }

    @Test
    @Override
    public void testSelection() {
        // needs graphics
    }

    @Test
    @Override
    public void testMacBindings() {
        // needs graphics
        // delete from line start
        // TODO TA needs graphics
        //END, shortcut(BACK_SPACE), checkText("")
    }

    @Test
    @Override
    public void testNonMacBindings() {
        // needs graphics
    }

    @Test
    @Override
    public final void testWordMac() {
        super.testWordMac();
    }

    @Test
    @Override
    public final void testWordNonMac() {
        super.testWordNonMac();
    }
}
