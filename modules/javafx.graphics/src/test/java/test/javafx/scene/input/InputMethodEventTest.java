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

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodTextRun;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class InputMethodEventTest {
    private final Node node1 = new TestNode();
    private final Node node2 = new TestNode();
    private final ObservableList<InputMethodTextRun> observableArrayList =
            javafx.collections.FXCollections.<InputMethodTextRun>observableArrayList();
    private final InputMethodEvent event =
            new InputMethodEvent(null, node1, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, observableArrayList, "Text", 2);

    @Test
    public void shouldCreateInputMethodEvent() {
        /* constructor called during initialization */
        assertSame(node1, event.getTarget());
        assertEquals(observableArrayList, event.getComposed());
        assertEquals("Text", event.getCommitted());
        assertEquals(2, event.getCaretPosition());
    }

    @Test
    public void shouldCopyInputMethodEvent() {
        InputMethodEvent copied = event.copyFor(null, node2);

        assertSame(node2, copied.getTarget());
        assertEquals(observableArrayList, copied.getComposed());
        assertEquals("Text", copied.getCommitted());
        assertEquals(2, copied.getCaretPosition());
    }

    @Test
    public void shouldGetNonEmptyDescription() {
        String s = event.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

}
