/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.behavior;

import com.sun.javafx.scene.control.behavior.MnemonicParser;
import javafx.scene.input.KeyCombination;
import org.junit.Test;

import static org.junit.Assert.*;

public class MnemonicParserTest {

    private static void assertKeyCombination(String expected, KeyCombination actual) {
        if (com.sun.javafx.PlatformUtil.isMac()) {
            assertSame(KeyCombination.ModifierValue.DOWN, actual.getMeta());
        } else {
            assertSame(KeyCombination.ModifierValue.DOWN, actual.getAlt());
        }

        assertEquals(expected, ((MnemonicParser.MnemonicKeyCombination)actual).getCharacter());
    }

    @Test
    public void testSimpleMnemonicLetter() {
        var helper = new MnemonicParser("foo _bar");
        assertEquals("foo bar", helper.getText());
        assertEquals("b", helper.getMnemonic());
        assertKeyCombination("b", helper.getMnemonicKeyCombination());
        assertEquals(4, helper.getMnemonicIndex());
    }

    @Test
    public void testSimpleMnemonicDigit() {
        var helper = new MnemonicParser("foo _1 bar");
        assertEquals("foo 1 bar", helper.getText());
        assertEquals("1", helper.getMnemonic());
        assertKeyCombination("1", helper.getMnemonicKeyCombination());
        assertEquals(4, helper.getMnemonicIndex());
    }

    @Test
    public void testExtendedMnemonicLetter() {
        var helper = new MnemonicParser("foo _(x)bar");
        assertEquals("foo bar", helper.getText());
        assertEquals("x", helper.getMnemonic());
        assertKeyCombination("x", helper.getMnemonicKeyCombination());
        assertEquals(4, helper.getMnemonicIndex());
    }

    @Test
    public void testExtendedMnemonicUnderscore() {
        var helper = new MnemonicParser("foo _(_)bar");
        assertEquals("foo bar", helper.getText());
        assertEquals("_", helper.getMnemonic());
        assertKeyCombination("_", helper.getMnemonicKeyCombination());
        assertEquals(4, helper.getMnemonicIndex());
    }

    @Test
    public void testExtendedMnemonicClosingBrace() {
        var helper = new MnemonicParser("foo _())bar");
        assertEquals("foo bar", helper.getText());
        assertEquals(")", helper.getMnemonic());
        assertKeyCombination(")", helper.getMnemonicKeyCombination());
        assertEquals(4, helper.getMnemonicIndex());
    }

    @Test
    public void testEscapedMnemonicSymbol() {
        var helper = new MnemonicParser("foo __bar");
        assertEquals("foo _bar", helper.getText());
        assertNull(helper.getMnemonic());
        assertNull(helper.getMnemonicKeyCombination());
        assertEquals(-1, helper.getMnemonicIndex());
    }

    @Test
    public void testWhitespaceIsNotProcessedAsExtendedMnemonic() {
        var helper = new MnemonicParser("foo _( ) bar");
        assertEquals("foo ( ) bar", helper.getText());
        assertEquals("(", helper.getMnemonic());
        assertKeyCombination("(", helper.getMnemonicKeyCombination());
        assertEquals(4, helper.getMnemonicIndex());
    }

    @Test
    public void testUnderscoreNotFollowedByAlphabeticCharIsNotAMnemonic() {
        var helper = new MnemonicParser("foo_ bar");
        assertEquals("foo_ bar", helper.getText());
        assertNull(helper.getMnemonic());
        assertNull(helper.getMnemonicKeyCombination());
        assertEquals(-1, helper.getMnemonicIndex());
    }

    @Test
    public void testUnderscoreAtEndOfTextIsNotAMnemonic() {
        var helper = new MnemonicParser("foo_");
        assertEquals("foo_", helper.getText());
        assertNull(helper.getMnemonic());
        assertNull(helper.getMnemonicKeyCombination());
        assertEquals(-1, helper.getMnemonicIndex());
    }

    @Test
    public void testMnemonicParsingStopsAfterFirstSimpleMnemonic() {
        var helper = new MnemonicParser("_foo _bar _qux");
        assertEquals("foo _bar _qux", helper.getText());
        assertEquals("f", helper.getMnemonic());
        assertKeyCombination("f", helper.getMnemonicKeyCombination());
        assertEquals(0, helper.getMnemonicIndex());
    }

    @Test
    public void testMnemonicParsingStopsAfterFirstExtendedMnemonic() {
        var helper = new MnemonicParser("_(x)foo _bar _qux");
        assertEquals("foo _bar _qux", helper.getText());
        assertEquals("x", helper.getMnemonic());
        assertKeyCombination("x", helper.getMnemonicKeyCombination());
        assertEquals(0, helper.getMnemonicIndex());
    }

}
