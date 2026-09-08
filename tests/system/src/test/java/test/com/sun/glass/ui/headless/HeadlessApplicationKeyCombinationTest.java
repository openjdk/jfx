/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.glass.ui.headless;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeadlessApplicationKeyCombinationTest {

    @BeforeAll
    public static void setup() throws Exception {
        System.setProperty("glass.platform", "Headless");
        System.setProperty("prism.order", "sw");

        AtomicBoolean fail = new AtomicBoolean();

        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.startup(waitLatch::countDown);
        try {
            if (!waitLatch.await(1, TimeUnit.SECONDS)) {
                fail.set(true);
            }
        } catch (InterruptedException e) {
            fail.set(true);
        }
        assertFalse(fail.get());
    }

    @Test
    public void testKeyCharacterCombinationShortcutMatching() {
        KeyCombination combination = KeyCombination.valueOf("shortcut+,");

        assertFalse(combination.match(keyPressed(",", KeyCode.COMMA, false, false)));

        boolean otherOSMatch = combination.match(keyPressed(",", KeyCode.COMMA, true, false));
        boolean macOSMatch = combination.match(keyPressed(",", KeyCode.COMMA, false, true));

        assertTrue(otherOSMatch ^ macOSMatch);
    }

    @Test
    public void testNumpadKeyMatchesCharacterOfNumpadKey() {
        KeyCombination combination = KeyCombination.valueOf("'*'");
        assertTrue(combination.match(keyPressed("*", KeyCode.MULTIPLY)));

        combination = KeyCombination.valueOf("'8'");
        assertTrue(combination.match(keyPressed("8", KeyCode.NUMPAD8)));
        assertTrue(combination.match(keyPressed("8", KeyCode.DIGIT8)));

        combination = KeyCombination.valueOf("'.'");
        assertTrue(combination.match(keyPressed(".", KeyCode.DECIMAL)));
        assertTrue(combination.match(keyPressed(".", KeyCode.PERIOD)));
    }

    @Test
    public void testNumpadKeyDoesNotMatchCharacterOfOtherNumpadKey() {
        KeyCombination combination = KeyCombination.valueOf("'*'");
        assertFalse(combination.match(keyPressed("*", KeyCode.NUMPAD8)));
        assertFalse(combination.match(keyPressed("*", KeyCode.ADD)));
    }

    @Test
    public void testSpaceMatchesSpaceKey() {
        KeyCombination combination = KeyCombination.valueOf("' '");
        assertTrue(combination.match(keyPressed(" ", KeyCode.SPACE)));
        assertFalse(combination.match(keyPressed(" ", KeyCode.ENTER)));
    }

    private static KeyEvent keyPressed(String character, KeyCode code) {
        return keyPressed(character, code, false, false);
    }

    private static KeyEvent keyPressed(String character, KeyCode code, boolean controlDown, boolean metaDown) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, character, character, code, false, controlDown, false, metaDown);
    }
}
