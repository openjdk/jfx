/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.input;

import com.sun.javafx.scene.input.KeyCodeMap;
import javafx.scene.Node;
import javafx.event.EventTarget;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeyEventTest {

    private final Node node1 = new TestNode();
    private final Node node2 = new TestNode();

    public KeyEvent testKeyEvent(EventTarget target, String character, KeyCode code, boolean shiftDown, boolean controlDown, boolean altDown, boolean metaDown) {
        return new KeyEvent(null, target, KeyEvent.KEY_PRESSED, character, null, code, shiftDown, controlDown, altDown, metaDown);
    }

    @Test
    public void shouldCreateKeyTypedEvent() {
        KeyEvent event = new KeyEvent(null, node1, KeyEvent.KEY_TYPED, "A", "A", KeyCodeMap.valueOf(0x41), true,
                false, true, false);

        assertSame(node1, event.getTarget());
        assertEquals("A", event.getCharacter());
        assertTrue(event.getText().isEmpty());
        assertSame(KeyCode.UNDEFINED, event.getCode());
        assertTrue(event.isShiftDown());
        assertFalse(event.isControlDown());
        assertTrue(event.isAltDown());
        assertFalse(event.isMetaDown());
        assertSame(KeyEvent.KEY_TYPED, event.getEventType());
    }

    @Test
    public void shouldCreateKeyReleasedEvent() {
        KeyEvent event = new KeyEvent(null, node1, KeyEvent.KEY_RELEASED, "A", "A", KeyCodeMap.valueOf(0x41), false,
                true, false, true);

        assertSame(node1, event.getTarget());
        assertEquals(KeyEvent.CHAR_UNDEFINED, event.getCharacter());
        assertEquals("A", event.getText());
        assertSame(KeyCode.A, event.getCode());
        assertFalse(event.isShiftDown());
        assertTrue(event.isControlDown());
        assertFalse(event.isAltDown());
        assertTrue(event.isMetaDown());
        assertSame(KeyEvent.KEY_RELEASED, event.getEventType());
    }

    @Test
    public void shouldCopyKeyTypedEvent() {
        KeyEvent original = new KeyEvent(null, node1, KeyEvent.KEY_TYPED, "A", "A", KeyCodeMap.valueOf(0x41), true,
                false, false, false);
        KeyEvent event = original.copyFor(null, node2);

        assertSame(node2, event.getTarget());
        assertEquals("A", event.getCharacter());
        assertTrue(event.getText().isEmpty());
        assertSame(KeyCode.UNDEFINED, event.getCode());
        assertTrue(event.isShiftDown());
        assertFalse(event.isControlDown());
        assertFalse(event.isAltDown());
        assertFalse(event.isMetaDown());
        assertSame(KeyEvent.KEY_TYPED, event.getEventType());
    }

    @Test
    public void shouldGetNonEmptyDescription() {
        KeyEvent event1 = testKeyEvent(node1, "A", KeyCode.A,
            false, false, false, false);
        KeyEvent event2 = testKeyEvent(node1, "", KeyCode.UNDEFINED,
            true, true, true, true);

        String s1 = event1.toString();
        String s2 = event2.toString();
        assertNotNull(s1);
        assertNotNull(s2);
        assertFalse(s1.isEmpty());
        assertFalse(s2.isEmpty());
    }
}
