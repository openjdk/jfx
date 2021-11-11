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

import com.sun.javafx.scene.control.behavior.TextBinding;
import javafx.scene.input.KeyCombination;
import org.junit.Test;

import static org.junit.Assert.*;

public class TextBindingTest {

    private static void assertKeyCombination(String expected, KeyCombination actual) {
        if (com.sun.javafx.PlatformUtil.isMac()) {
            assertSame(KeyCombination.ModifierValue.DOWN, actual.getMeta());
        } else {
            assertSame(KeyCombination.ModifierValue.DOWN, actual.getAlt());
        }

        assertEquals(expected, ((TextBinding.MnemonicKeyCombination)actual).getCharacter());
    }

    @Test
    public void testSimpleMnemonicLetter() {
        var binding = new TextBinding("foo _bar");
        assertEquals("foo bar", binding.getText());
        assertEquals("b", binding.getMnemonic());
        assertKeyCombination("b", binding.getMnemonicKeyCombination());
        assertEquals(4, binding.getMnemonicIndex());
    }

    @Test
    public void testSimpleMnemonicDigit() {
        var binding = new TextBinding("foo _1 bar");
        assertEquals("foo 1 bar", binding.getText());
        assertEquals("1", binding.getMnemonic());
        assertKeyCombination("1", binding.getMnemonicKeyCombination());
        assertEquals(4, binding.getMnemonicIndex());
    }

    @Test
    public void testExtendedMnemonicLetter() {
        var binding = new TextBinding("foo _(x)bar");
        assertEquals("foo bar", binding.getText());
        assertEquals("x", binding.getMnemonic());
        assertKeyCombination("x", binding.getMnemonicKeyCombination());
        assertEquals(4, binding.getMnemonicIndex());
    }

    @Test
    public void testExtendedMnemonicUnderscore() {
        var binding = new TextBinding("foo _(_)bar");
        assertEquals("foo bar", binding.getText());
        assertEquals("_", binding.getMnemonic());
        assertKeyCombination("_", binding.getMnemonicKeyCombination());
        assertEquals(4, binding.getMnemonicIndex());
    }

    @Test
    public void testExtendedMnemonicClosingBrace() {
        var binding = new TextBinding("foo _())bar");
        assertEquals("foo bar", binding.getText());
        assertEquals(")", binding.getMnemonic());
        assertKeyCombination(")", binding.getMnemonicKeyCombination());
        assertEquals(4, binding.getMnemonicIndex());
    }

    @Test
    public void testEscapedMnemonicSymbol() {
        var binding = new TextBinding("foo __bar");
        assertEquals("foo _bar", binding.getText());
        assertNull(binding.getMnemonic());
        assertNull(binding.getMnemonicKeyCombination());
        assertEquals(-1, binding.getMnemonicIndex());
    }

    @Test
    public void testWhitespaceIsNotProcessedAsExtendedMnemonic() {
        var binding = new TextBinding("foo _( ) bar");
        assertEquals("foo ( ) bar", binding.getText());
        assertEquals("(", binding.getMnemonic());
        assertKeyCombination("(", binding.getMnemonicKeyCombination());
        assertEquals(4, binding.getMnemonicIndex());
    }

    @Test
    public void testUnderscoreNotFollowedByAlphabeticCharIsNotAMnemonic() {
        var binding = new TextBinding("foo_ bar");
        assertEquals("foo_ bar", binding.getText());
        assertNull(binding.getMnemonic());
        assertNull(binding.getMnemonicKeyCombination());
        assertEquals(-1, binding.getMnemonicIndex());
    }

    @Test
    public void testUnderscoreAtEndOfTextIsNotAMnemonic() {
        var binding = new TextBinding("foo_");
        assertEquals("foo_", binding.getText());
        assertNull(binding.getMnemonic());
        assertNull(binding.getMnemonicKeyCombination());
        assertEquals(-1, binding.getMnemonicIndex());
    }

    @Test
    public void testMnemonicParsingStopsAfterFirstSimpleMnemonic() {
        var binding = new TextBinding("_foo _bar _qux");
        assertEquals("foo _bar _qux", binding.getText());
        assertEquals("f", binding.getMnemonic());
        assertKeyCombination("f", binding.getMnemonicKeyCombination());
        assertEquals(0, binding.getMnemonicIndex());
    }

    @Test
    public void testMnemonicParsingStopsAfterFirstExtendedMnemonic() {
        var binding = new TextBinding("_(x)foo _bar _qux");
        assertEquals("foo _bar _qux", binding.getText());
        assertEquals("x", binding.getMnemonic());
        assertKeyCombination("x", binding.getMnemonicKeyCombination());
        assertEquals(0, binding.getMnemonicIndex());
    }

}
