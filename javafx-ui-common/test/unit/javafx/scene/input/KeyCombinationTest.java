/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.input;

import static javafx.scene.input.KeyCombination.ALT_DOWN;
import static javafx.scene.input.KeyCombination.CONTROL_ANY;
import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static javafx.scene.input.KeyCombination.SHIFT_ANY;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javafx.event.Event;
import javafx.scene.input.KeyCombination.ModifierValue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;


public class KeyCombinationTest {
    final KeyEvent ctrlAltQEvent = new KeyEvent(null, Event.NULL_SOURCE_TARGET, KeyEvent.KEY_PRESSED, 
            "q", null, 0x51, false, true, true, false);

    final KeyEvent ctrlAltQUpEvent = new KeyEvent(null, Event.NULL_SOURCE_TARGET, KeyEvent.KEY_RELEASED, 
            "q", null, 0x51, false, true, true, false);

    final KeyEvent ctrlShiftQEvent = new KeyEvent(null, Event.NULL_SOURCE_TARGET, KeyEvent.KEY_PRESSED, 
            "Q", null, 0x51, true, true, false, false);

    final KeyEvent ctrlAltShiftQEvent = new KeyEvent(null, Event.NULL_SOURCE_TARGET, KeyEvent.KEY_PRESSED, 
            "Q", null, 0x51, true, true, true, false);

    final KeyEvent alt2Event = new KeyEvent(null, Event.NULL_SOURCE_TARGET, KeyEvent.KEY_PRESSED, 
            "2", null, 0x32, false, false, true, false);

    final KeyEvent altShift2Event = new KeyEvent(null, Event.NULL_SOURCE_TARGET, KeyEvent.KEY_PRESSED, 
            "@", null, 0x32, true, false, true, false);

    final KeyEvent altSoftkey0Event = new KeyEvent(null, Event.NULL_SOURCE_TARGET, KeyEvent.KEY_PRESSED, 
            "~", null, 0x1000, false, false, true, false);

    final KeyEvent altShiftSoftkey0Event = new KeyEvent(null, Event.NULL_SOURCE_TARGET, KeyEvent.KEY_PRESSED, 
            "~", null, 0x1000, true, false, true, false);

    final KeyEvent altShiftPlusEvent = new KeyEvent(null, Event.NULL_SOURCE_TARGET, KeyEvent.KEY_PRESSED, 
            "~", null, 0x209, true, false, true, false);

    final KeyEvent altShiftQuoteEvent = new KeyEvent(null, Event.NULL_SOURCE_TARGET, KeyEvent.KEY_PRESSED, 
            "~", null, 0xDE, true, false, true, false);

    @BeforeClass
    public static void setUpCharToKeyCodeMap() {
        final Map<String, KeyCode> charToKeyCodeMap =
                new HashMap<String, KeyCode>();

        // LATIN SMALL LETTER Q
        charToKeyCodeMap.put("q", KeyCode.Q);

        // LATIN CAPITAL LETTER Q
        charToKeyCodeMap.put("Q", KeyCode.Q);

        // LETTER +
        charToKeyCodeMap.put("+", KeyCode.PLUS);

        // LETTER '
        charToKeyCodeMap.put("'", KeyCode.QUOTE);

        // LATIN SMALL LETTER E WITH CARON
        charToKeyCodeMap.put("\u011b", KeyCode.DIGIT2);

        // CYRILLIC SMALL LETTER SHORT I
        charToKeyCodeMap.put("\u0439", KeyCode.Q);

        // CYRILLIC CAPITAL LETTER SHORT I
        charToKeyCodeMap.put("\u0419", KeyCode.Q);

        // MUSICAL SYMBOL G CLEF
        charToKeyCodeMap.put("\ud834\udd1e", KeyCode.SOFTKEY_0);
        
        ((StubToolkit) Toolkit.getToolkit()).setCharToKeyCodeMap(
                charToKeyCodeMap);
    }

    @AfterClass
    public static void tearDownCharToKeyCodeMap() {
        ((StubToolkit) Toolkit.getToolkit()).setCharToKeyCodeMap(null);
    }

    @Test
    public void testSimpleKeyCodeCombination() {
        KeyCombination ctrlAltQ = new KeyCodeCombination(KeyCode.Q,
                                                         CONTROL_DOWN,
                                                         ALT_DOWN);

        KeyCombination altCtrlQ = new KeyCodeCombination(KeyCode.Q,
                                                         ALT_DOWN,
                                                         CONTROL_DOWN);

        KeyCombination ctrlShiftQ = new KeyCodeCombination(KeyCode.Q,
                                                           CONTROL_DOWN,
                                                           SHIFT_DOWN);

        KeyCombination ctrlAltShiftQ = new KeyCodeCombination(KeyCode.Q,
                                                              CONTROL_DOWN,
                                                              ALT_DOWN,
                                                              SHIFT_DOWN);

        KeyCombination ctrlQ = new KeyCodeCombination(KeyCode.DIGIT0,
                                                      CONTROL_DOWN);

        KeyCombination ctrlAltA = new KeyCodeCombination(KeyCode.A,
                                                         CONTROL_DOWN,
                                                         ALT_DOWN);

        assertTrue(ctrlAltQ.match(ctrlAltQEvent));
        assertFalse(ctrlAltQ.match(ctrlShiftQEvent));
        assertTrue(altCtrlQ.match(ctrlAltQEvent));
        assertFalse(altCtrlQ.match(ctrlShiftQEvent));
        assertFalse(ctrlShiftQ.match(ctrlAltQEvent));
        assertTrue(ctrlShiftQ.match(ctrlShiftQEvent));
        assertFalse(ctrlAltShiftQ.match(ctrlAltQEvent));
        assertFalse(ctrlAltShiftQ.match(ctrlShiftQEvent));
        assertFalse(ctrlQ.match(ctrlAltQEvent));
        assertFalse(ctrlQ.match(ctrlShiftQEvent));
        assertFalse(ctrlAltA.match(ctrlAltQEvent));
        assertFalse(ctrlAltA.match(ctrlShiftQEvent));
        assertTrue(ctrlAltQ.match(ctrlAltQUpEvent));
    }

    @Test
    public void testKeyCodeCombinationWithIgnore() {
        KeyCombination ctrlAltQ = new KeyCodeCombination(KeyCode.Q,
                                                         CONTROL_DOWN,
                                                         ALT_DOWN);

        KeyCombination ctrlAltIShiftQ = new KeyCodeCombination(KeyCode.Q,
                                                               CONTROL_DOWN,
                                                               ALT_DOWN,
                                                               SHIFT_ANY);

        KeyCombination ctrlAltShiftQ = new KeyCodeCombination(KeyCode.Q,
                                                              CONTROL_DOWN,
                                                              ALT_DOWN,
                                                              SHIFT_DOWN);

        assertTrue(ctrlAltQ.match(ctrlAltQEvent));
        assertFalse(ctrlAltQ.match(ctrlAltShiftQEvent));
        assertFalse(ctrlAltShiftQ.match(ctrlAltQEvent));
        assertTrue(ctrlAltShiftQ.match(ctrlAltShiftQEvent));
        assertTrue(ctrlAltIShiftQ.match(ctrlAltQEvent));
        assertTrue(ctrlAltIShiftQ.match(ctrlAltShiftQEvent));
    }

    @Test
    public void testKeyCodeCombinationFromString() {
        KeyCombination ctrlAltQ =
                KeyCombination.keyCombination("Ctrl + ALT+Q");

        KeyCombination altCtrlQ = 
                KeyCombination.keyCombination("alt+CtRL  + q");

        KeyCombination ctrlShiftQ = 
                KeyCombination.keyCombination("ctrl+shift+Q");

        KeyCombination ctrlAltShiftQ =
                KeyCombination.keyCombination("ctrl + Alt + shift + Q");

        KeyCombination ctrlQ = KeyCombination.keyCombination("  ctrl+Q  ");

        KeyCombination ctrlAltA = KeyCombination.keyCombination("ctrl+alt+A");

        KeyCombination ctrlIgnoreAltShiftQ =
                KeyCombination.keyCombination("Ctrl + ignore Alt + Shift + Q");
        
        KeyCombination ctrlIgnoreAltShiftQWithWS1=
                KeyCombination.keyCombination("Ctrl\t \t\n\f\r\u000B+ \t\n\f \r\u000Bignore Alt\n+ Shift\f + Q");

        KeyCombination ctrlIgnoreAltShiftQWithWS2 =
                KeyCombination.keyCombination("Ctrl\r+ignore Alt\u000B+Shift +Q");

        assertTrue(ctrlAltQ.match(ctrlAltQEvent));
        assertFalse(ctrlAltQ.match(ctrlShiftQEvent));
        assertTrue(altCtrlQ.match(ctrlAltQEvent));
        assertFalse(altCtrlQ.match(ctrlShiftQEvent));
        assertFalse(ctrlShiftQ.match(ctrlAltQEvent));
        assertTrue(ctrlShiftQ.match(ctrlShiftQEvent));
        assertFalse(ctrlAltShiftQ.match(ctrlAltQEvent));
        assertFalse(ctrlAltShiftQ.match(ctrlShiftQEvent));
        assertFalse(ctrlQ.match(ctrlAltQEvent));
        assertFalse(ctrlQ.match(ctrlShiftQEvent));
        assertFalse(ctrlAltA.match(ctrlAltQEvent));
        assertFalse(ctrlAltA.match(ctrlShiftQEvent));
        assertTrue(ctrlAltQ.match(ctrlAltQUpEvent));
        assertTrue(ctrlIgnoreAltShiftQ.match(ctrlShiftQEvent));
        assertTrue(ctrlIgnoreAltShiftQ.match(ctrlAltShiftQEvent));
        assertTrue(ctrlIgnoreAltShiftQWithWS1.match(ctrlAltShiftQEvent));
        assertTrue(ctrlIgnoreAltShiftQWithWS2.match(ctrlAltShiftQEvent));
    }

    @Test
    public void testKeyCharacterCombinationFromString() {
        KeyCombination ctrlAltQ = 
                KeyCombination.keyCombination("Ctrl + ALT+'Q'");

        KeyCombination altCtrlQ = 
                KeyCombination.keyCombination("alt+CtRL  + 'q'");

        KeyCombination altLatinSmallEWithCaron =
                KeyCombination.keyCombination("alt+\u011b");

        KeyCombination shiftAltLatinSmallEWithCaron =
                KeyCombination.keyCombination("shift+alt+'\u011b'");

        KeyCombination ctrlShiftCyrillicSmallI =
                KeyCombination.keyCombination("ctrl+shift+\u0439");

        KeyCombination ctrlShiftCyrillicCapitalI =
                KeyCombination.keyCombination("  ctrl+ shift+  \u0419");

        KeyCombination ctrlAltShiftCyrillicSmallI =
                KeyCombination.keyCombination("ctrl + Alt + shift + \u0439");

        KeyCombination altIgnoreShiftMusicalSymbolGClef =
                KeyCombination.keyCombination(
                    "Alt + ignore shift + \ud834\udd1e ");

        KeyCombination altShiftPlus =
                KeyCombination.keyCombination("Shift + Alt+'+'");

        KeyCombination altShiftQuote1 =
                KeyCombination.keyCombination("Shift + Alt + '\\''");

        KeyCombination altShiftQuote2 =
                KeyCombination.keyCombination("'\\''+Shift+Alt");

        assertTrue(ctrlAltQ.match(ctrlAltQEvent));
        assertFalse(ctrlAltQ.match(ctrlShiftQEvent));
        assertTrue(altCtrlQ.match(ctrlAltQEvent));
        assertFalse(altCtrlQ.match(ctrlShiftQEvent));
        assertTrue(altLatinSmallEWithCaron.match(alt2Event));
        assertFalse(altLatinSmallEWithCaron.match(altShift2Event));
        assertFalse(shiftAltLatinSmallEWithCaron.match(alt2Event));
        assertTrue(shiftAltLatinSmallEWithCaron.match(altShift2Event));
        assertTrue(ctrlShiftCyrillicSmallI.match(ctrlShiftQEvent));
        assertFalse(ctrlShiftCyrillicSmallI.match(ctrlAltShiftQEvent));
        assertTrue(ctrlShiftCyrillicCapitalI.match(ctrlShiftQEvent));
        assertFalse(ctrlShiftCyrillicCapitalI.match(ctrlAltShiftQEvent));
        assertFalse(ctrlAltShiftCyrillicSmallI.match(ctrlShiftQEvent));
        assertTrue(ctrlAltShiftCyrillicSmallI.match(ctrlAltShiftQEvent));
        assertTrue(altIgnoreShiftMusicalSymbolGClef.match(altSoftkey0Event));
        assertTrue(
                altIgnoreShiftMusicalSymbolGClef.match(altShiftSoftkey0Event));
        assertTrue(altShiftPlus.match(altShiftPlusEvent));
        assertTrue(altShiftQuote1.match(altShiftQuoteEvent));
        assertTrue(altShiftQuote2.match(altShiftQuoteEvent));
    }

    @Test
    public void testGetName() {
        KeyCombination ctrlAltVKQ = new KeyCodeCombination(KeyCode.Q,
                                                           CONTROL_DOWN,
                                                           ALT_DOWN);

        KeyCombination ctrlAltQ = new KeyCharacterCombination("q",
                                                              CONTROL_DOWN,
                                                              ALT_DOWN);

        KeyCombination ctrlIgnoreShiftA = new KeyCharacterCombination(
                                                "a",
                                                CONTROL_DOWN,
                                                SHIFT_ANY);

        KeyCombination altQuote = new KeyCharacterCombination(
                                                "'",
                                                ALT_DOWN);

        assertEquals("Ctrl+Alt+Q", ctrlAltVKQ.getName());
        assertEquals("Ctrl+Alt+'q'", ctrlAltQ.getName());
        assertEquals("Ignore Shift+Ctrl+'a'", ctrlIgnoreShiftA.getName());
        assertEquals("Alt+'\\''", altQuote.getName());
    }

    @Test
    public void testKeyCombinationWithShortcutModifier() {
        final KeyEvent ctrlC = new KeyEvent(
                                   KeyEvent.KEY_PRESSED,
                                   "c", null, 0x43,
                                   false, true, false, false);
        final KeyEvent metaC = new KeyEvent(
                                   KeyEvent.KEY_PRESSED,
                                   "c", null, 0x43,
                                   false, false, false, true);
        final KeyEvent metaAltC = new KeyEvent(
                                   KeyEvent.KEY_PRESSED,
                                      "c", null, 0x43,
                                      false, false, true, true);

        final KeyCombination shortcutC =
                KeyCombination.keyCombination("Shortcut+C");
        final KeyCombination altIgnoreShortcutC =
                KeyCombination.keyCombination("Alt+Ignore Shortcut+C");

        final StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();

        toolkit.setPlatformShortcutKey(KeyCode.SHORTCUT);
        assertFalse(shortcutC.match(ctrlC));
        assertFalse(shortcutC.match(metaC));
        assertFalse(shortcutC.match(metaAltC));
        assertFalse(altIgnoreShortcutC.match(ctrlC));
        assertFalse(altIgnoreShortcutC.match(metaC));
        assertFalse(altIgnoreShortcutC.match(metaAltC));

        toolkit.setPlatformShortcutKey(KeyCode.CONTROL);
        assertTrue(shortcutC.match(ctrlC));
        assertFalse(shortcutC.match(metaC));
        assertFalse(shortcutC.match(metaAltC));
        assertFalse(altIgnoreShortcutC.match(ctrlC));
        assertFalse(altIgnoreShortcutC.match(metaC));
        assertFalse(altIgnoreShortcutC.match(metaAltC));

        toolkit.setPlatformShortcutKey(KeyCode.META);
        assertFalse(shortcutC.match(ctrlC));
        assertTrue(shortcutC.match(metaC));
        assertFalse(shortcutC.match(metaAltC));
        assertFalse(altIgnoreShortcutC.match(ctrlC));
        assertFalse(altIgnoreShortcutC.match(metaC));
        assertTrue(altIgnoreShortcutC.match(metaAltC));
    }

    @Test(expected=NullPointerException.class)
    public void constructor1ShouldThrowNPEForNullKeyCode() {
        new KeyCodeCombination(null, ModifierValue.UP,
                                     ModifierValue.UP,
                                     ModifierValue.UP,
                                     ModifierValue.UP,
                                     ModifierValue.UP);
    }

    @Test(expected=NullPointerException.class)
    public void constructor1ShouldThrowNPEForNullKeyCharacter() {
        new KeyCharacterCombination(null, ModifierValue.UP,
                                          ModifierValue.UP,
                                          ModifierValue.UP,
                                          ModifierValue.UP,
                                          ModifierValue.UP);
    }

    @Test(expected=NullPointerException.class)
    public void constructor1ShouldThrowNPEForNullModifier() {
        new KeyCodeCombination(KeyCode.Q, ModifierValue.UP,
                                          null,
                                          ModifierValue.UP,
                                          ModifierValue.UP,
                                          ModifierValue.UP);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructor1ShouldThrowIAEForModifierKeyCode() {
        new KeyCodeCombination(KeyCode.SHIFT, ModifierValue.UP,
                                              ModifierValue.UP,
                                              ModifierValue.UP,
                                              ModifierValue.UP,
                                              ModifierValue.UP);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructor1ShouldThrowIAEForUndefinedKeyCode() {
        new KeyCodeCombination(KeyCode.UNDEFINED, ModifierValue.UP,
                                                  ModifierValue.UP,
                                                  ModifierValue.UP,
                                                  ModifierValue.UP,
                                                  ModifierValue.UP);
    }

    @Test(expected=NullPointerException.class)
    public void constructor2ShouldThrowNPEForNullKeyCode() {
        new KeyCodeCombination(null);
    }

    @Test(expected=NullPointerException.class)
    public void constructor2ShouldThrowNPEForNullKeyCharacter() {
        new KeyCharacterCombination(null);
    }

    @Test(expected=NullPointerException.class)
    public void constructor2ShouldThrowNPEForNullModifier() {
        new KeyCharacterCombination("q", ALT_DOWN, null, SHIFT_ANY);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructor2ShouldThrowIAEForModifierKeyCode() {
        new KeyCodeCombination(KeyCode.CONTROL);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructor2ShouldThrowIAEForUndefinedKeyCode() {
        new KeyCodeCombination(KeyCode.UNDEFINED);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructor2ShouldThrowIAEForDuplicateModifiers() {
        new KeyCodeCombination(KeyCode.Q, 
                               ALT_DOWN,
                               CONTROL_DOWN,
                               ALT_DOWN);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructor2ShouldThrowIAEForConflictingModifiers() {
        new KeyCodeCombination(KeyCode.Q,  
                               SHIFT_DOWN,
                               CONTROL_ANY,
                               SHIFT_ANY);
    }

    @Test(expected=IllegalArgumentException.class)
    public void keyCombinationShouldThrowIAEForDuplicateModifiers() {
        KeyCombination.keyCombination("Ctrl + Shift + Ctrl + Q");
    }

    @Test(expected=IllegalArgumentException.class)
    public void keyCombinationShouldThrowIAEForConflictingModifiers() {
        KeyCombination.keyCombination("Ctrl + Ignore Shift + Alt + Shift + Q");
    }

    @Test(expected=IllegalArgumentException.class)
    public void keyCombinationShouldThrowIAEForMissingMainKey() {
        KeyCombination.keyCombination("Ctrl + Shift");
    }

    @Test(expected=IllegalArgumentException.class)
    public void keyCombinationShouldThrowIAEForUndefinedMainKey() {
        KeyCombination.keyCombination("Ctrl + Shift + Undefined");
    }

    @Test(expected=IllegalArgumentException.class)
    public void keyCombinationShouldThrowIAEForExtraMainKey1() {
        KeyCombination.keyCombination("Ctrl + Shift + Q + 'W'");
    }

    @Test(expected=IllegalArgumentException.class)
    public void keyCombinationShouldThrowIAEForExtraMainKey2() {
        KeyCombination.keyCombination("Ctrl + Shift + 'W' + Q");
    }

    @Test(expected=IllegalArgumentException.class)
    public void keyCombinationShouldThrowIAEForInvalidSyntax1() {
        KeyCombination.keyCombination("  +  Ctrl + Q");
    }

    @Test(expected=IllegalArgumentException.class)
    public void keyCombinationShouldThrowIAEForInvalidSyntax2() {
        KeyCombination.keyCombination("Ctrl ++ Q");
    }

    @Test(expected=IllegalArgumentException.class)
    public void keyCombinationShouldThrowIAEForInvalidSyntax3() {
        KeyCombination.keyCombination("Ctrl + Q +");
    }

    @Test(expected=IllegalArgumentException.class)
    public void keyCombinationShouldThrowIAEForUnclosedQuote() {
        KeyCombination.keyCombination("Quote '");
    }

    @Test
    public void keyCombinationShouldUseUnparseableStringAsCharacter() {
        KeyCombination multiChar = KeyCombination.keyCombination("Alt'Q'\\'");
        assertEquals("Alt'Q'\\'",
                ((KeyCharacterCombination) multiChar).getCharacter());
    }
}
