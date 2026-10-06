/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.input.KeyCode.ENTER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.scene.control.TextArea;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;

/**
 * Tests TextArea behavior by exercising every key binding registered by the skin
 * at least once.
 *
 * Note: some aspects of behavior (navigation, selection) require fully rendered skin,
 * so it is impossible to test in headless environment.
 * See TextAreaBehaviorRobotTest.
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
        // consumes ENTER key press
        Assertions.assertTrue(kb.keyPressed(ENTER));
        Assertions.assertFalse(kb.keyReleased(ENTER));
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

    //@Test
    @Override
    public void testNavigation() {
        // needs graphics, tested by TextAreaBehaviorRobotTest
    }

    //@Test
    @Override
    public void testDeletion() {
        // needs graphics, tested by TextAreaBehaviorRobotTest
    }

    //@Test
    @Override
    public void testSelection() {
        // needs graphics, tested by TextAreaBehaviorRobotTest
    }

    //@Test
    @Override
    public void testMacBindings() {
        // needs graphics, tested by TextAreaBehaviorRobotTest
    }

    //@Test
    @Override
    public void testNonMacBindings() {
        // needs graphics, tested by TextAreaBehaviorRobotTest
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

    @Test
    @Disabled("JDK-8393470")
    public void testDoubleClick() {
        execute(
                setText("Bug #123"),
                clickCharacter(0, 2, true)
        );
        assertEquals("Bug", control.getSelectedText());

        execute(
                clickCharacter(6, 2, true)
        );
        assertEquals("123", control.getSelectedText());

        execute(
                clickCharacter(4, 2, true)
        );
        assertEquals("#", control.getSelectedText());

        execute(
                clickCharacter(3, 2, true)
        );
        assertEquals(" ", control.getSelectedText());

        execute(
                setText("aaa.3x3"),
                clickCharacter(5, 2, false)
        );
        assertEquals("3x3", control.getSelectedText());

        execute(
                setText("aaa bbb"),
                clickCharacter(4, 2, false)
        );
        assertEquals("bbb", control.getSelectedText());

        execute(
                setText("aaa bbb"),
                clickCharacter(4, 2, true)
        );
        assertEquals("bbb", control.getSelectedText());

        execute(
                setText("aaa,,,bbb"),
                clickCharacter(4, 2, true)
        );
        assertEquals(",,,", control.getSelectedText());

        execute(
                setText("aaa    bbb"),
                clickCharacter(5, 2, true)
        );
        assertEquals("    ", control.getSelectedText());

        execute(
                setText("aaa\t\t\tbbb"),
                clickCharacter(4, 2, true)
        );
        assertEquals("\t\t\t", control.getSelectedText());
    }
}
