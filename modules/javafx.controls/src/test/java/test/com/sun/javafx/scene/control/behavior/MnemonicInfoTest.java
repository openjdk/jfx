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

import com.sun.javafx.scene.control.behavior.MnemonicInfo;
import javafx.scene.input.KeyCombination;
import org.junit.Test;

import static org.junit.Assert.*;

public class MnemonicInfoTest {

    private static void assertKeyCombination(String expected, KeyCombination actual) {
        if (com.sun.javafx.PlatformUtil.isMac()) {
            assertSame(KeyCombination.ModifierValue.DOWN, actual.getMeta());
        } else {
            assertSame(KeyCombination.ModifierValue.DOWN, actual.getAlt());
        }

        assertEquals(expected, ((MnemonicInfo.MnemonicKeyCombination)actual).getCharacter());
    }

    @Test
    public void testSimpleMnemonicLetter() {
        var mnemonicInfo = new MnemonicInfo("foo _bar");
        assertEquals("foo bar", mnemonicInfo.getText());
        assertEquals("b", mnemonicInfo.getMnemonic());
        assertKeyCombination("b", mnemonicInfo.getMnemonicKeyCombination());
        assertEquals(4, mnemonicInfo.getMnemonicIndex());
    }

    @Test
    public void testSimpleMnemonicDigit() {
        var mnemonicInfo = new MnemonicInfo("foo _1 bar");
        assertEquals("foo 1 bar", mnemonicInfo.getText());
        assertEquals("1", mnemonicInfo.getMnemonic());
        assertKeyCombination("1", mnemonicInfo.getMnemonicKeyCombination());
        assertEquals(4, mnemonicInfo.getMnemonicIndex());
    }

    @Test
    public void testExtendedMnemonicLetter() {
        var mnemonicInfo = new MnemonicInfo("foo _(x)bar");
        assertEquals("foo bar", mnemonicInfo.getText());
        assertEquals("x", mnemonicInfo.getMnemonic());
        assertKeyCombination("x", mnemonicInfo.getMnemonicKeyCombination());
        assertEquals(4, mnemonicInfo.getMnemonicIndex());
    }

    @Test
    public void testExtendedMnemonicUnderscore() {
        var mnemonicInfo = new MnemonicInfo("foo _(_)bar");
        assertEquals("foo bar", mnemonicInfo.getText());
        assertEquals("_", mnemonicInfo.getMnemonic());
        assertKeyCombination("_", mnemonicInfo.getMnemonicKeyCombination());
        assertEquals(4, mnemonicInfo.getMnemonicIndex());
    }

    @Test
    public void testExtendedMnemonicClosingBrace() {
        var mnemonicInfo = new MnemonicInfo("foo _())bar");
        assertEquals("foo bar", mnemonicInfo.getText());
        assertEquals(")", mnemonicInfo.getMnemonic());
        assertKeyCombination(")", mnemonicInfo.getMnemonicKeyCombination());
        assertEquals(4, mnemonicInfo.getMnemonicIndex());
    }

    @Test
    public void testEscapedMnemonicSymbol() {
        var mnemonicInfo = new MnemonicInfo("foo __bar");
        assertEquals("foo _bar", mnemonicInfo.getText());
        assertNull(mnemonicInfo.getMnemonic());
        assertNull(mnemonicInfo.getMnemonicKeyCombination());
        assertEquals(-1, mnemonicInfo.getMnemonicIndex());
    }

    @Test
    public void testWhitespaceIsNotProcessedAsExtendedMnemonic() {
        var mnemonicInfo = new MnemonicInfo("foo _( ) bar");
        assertEquals("foo ( ) bar", mnemonicInfo.getText());
        assertEquals("(", mnemonicInfo.getMnemonic());
        assertKeyCombination("(", mnemonicInfo.getMnemonicKeyCombination());
        assertEquals(4, mnemonicInfo.getMnemonicIndex());
    }

    @Test
    public void testUnderscoreNotFollowedByAlphabeticCharIsNotAMnemonic() {
        var mnemonicInfo = new MnemonicInfo("foo_ bar");
        assertEquals("foo_ bar", mnemonicInfo.getText());
        assertNull(mnemonicInfo.getMnemonic());
        assertNull(mnemonicInfo.getMnemonicKeyCombination());
        assertEquals(-1, mnemonicInfo.getMnemonicIndex());
    }

    @Test
    public void testUnderscoreAtEndOfTextIsNotAMnemonic() {
        var mnemonicInfo = new MnemonicInfo("foo_");
        assertEquals("foo_", mnemonicInfo.getText());
        assertNull(mnemonicInfo.getMnemonic());
        assertNull(mnemonicInfo.getMnemonicKeyCombination());
        assertEquals(-1, mnemonicInfo.getMnemonicIndex());
    }

    @Test
    public void testMnemonicParsingStopsAfterFirstSimpleMnemonic() {
        var mnemonicInfo = new MnemonicInfo("_foo _bar _qux");
        assertEquals("foo _bar _qux", mnemonicInfo.getText());
        assertEquals("f", mnemonicInfo.getMnemonic());
        assertKeyCombination("f", mnemonicInfo.getMnemonicKeyCombination());
        assertEquals(0, mnemonicInfo.getMnemonicIndex());
    }

    @Test
    public void testMnemonicParsingStopsAfterFirstExtendedMnemonic() {
        var mnemonicInfo = new MnemonicInfo("_(x)foo _bar _qux");
        assertEquals("foo _bar _qux", mnemonicInfo.getText());
        assertEquals("x", mnemonicInfo.getMnemonic());
        assertKeyCombination("x", mnemonicInfo.getMnemonicKeyCombination());
        assertEquals(0, mnemonicInfo.getMnemonicIndex());
    }

}
