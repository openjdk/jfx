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

package test.javafx.scene.web;

import com.sun.javafx.PlatformUtil;
import javafx.concurrent.Worker;
import javafx.event.Event;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class SelectionCopyTest extends TestBase {

    @Test
    public void testCopyWithNoneSelection() {
        loadContent("<html><body><p>This is a test.</p></body></html>");

        submit(() -> {
            assertSame(Worker.State.SUCCEEDED, getEngine().getLoadWorker().getState(),
                    "Page should be loaded");
            assertSelection("None");

            copyShortcut();
            // After a copy action with no selection, the selection type should still be "None"
            assertSelection("None");

            assertEquals("This is a test.", getEngine().executeScript("document.body.innerText"),
                    "The engine is still alive and the content remains unchanged after copy with no selection");
        });

    }

    @Test
    public void testCopyWithCaretSelection() {
        loadContent("<html><body><p>This is a test.</p></body></html>");

        submit(() -> {
            assertSame(Worker.State.SUCCEEDED, getEngine().getLoadWorker().getState(),
                    "Page should be loaded");

            mousePress();
            // After a mouse press, the selection type should be "Caret"
            assertSelection("Caret");

            copyShortcut();
            // After a copy action with a caret selection, the selection type should still be "Caret"
            assertSelection("Caret");
        });

    }

    @Test
    public void testCopyWithRangeSelection() {
        loadContent("<html><body><p>This is a test.</p></body></html>");

        submit(() -> {
            assertSame(Worker.State.SUCCEEDED, getEngine().getLoadWorker().getState(),
                    "Page should be loaded");

            mousePress();
            // After a mouse press, the selection type should be "Caret"
            assertSelection("Caret");

            // Create a range selection programmatically
            getEngine().executeScript("var range = document.createRange();" +
                    "range.selectNodeContents(document.body);" +
                    "window.getSelection().removeAllRanges();" +
                    "window.getSelection().addRange(range);");
            assertSelection("Range");

            copyShortcut();
            // After a copy action with a range selection, the selection type should still be "Range"
            assertSelection("Range");

            Clipboard clipboard = Clipboard.getSystemClipboard();
            assertEquals("This is a test.", clipboard.getContent(DataFormat.PLAIN_TEXT));
        });

    }

    @Test
    public void testCopyWithRemovedRangeSelection() {
        loadContent("<html><body><p>This is a test.</p></body></html>");

        submit(() -> {
            assertSame(Worker.State.SUCCEEDED, getEngine().getLoadWorker().getState(),
                    "Page should be loaded");

            mousePress();
            // After a mouse press, the selection type should be "Caret"
            assertSelection("Caret");

            // Create a range selection programmatically
            getEngine().executeScript("var range = document.createRange();" +
                    "range.selectNodeContents(document.body);" +
                    "window.getSelection().removeAllRanges();" +
                    "window.getSelection().addRange(range);");
            assertSelection("Range");

            // Remove the range selection
            getEngine().executeScript("window.getSelection().removeAllRanges()");

            copyShortcut();
            // After a copy action with no selection, the selection type should be "None"
            assertSelection("None");
        });

    }

    private void copyShortcut() {
        // Simulate Cmd/Ctrl+C key press event to trigger copy action
        Event.fireEvent(getView(), new KeyEvent(null, getView(), KeyEvent.KEY_PRESSED, "", "",
                KeyCode.C, false, !PlatformUtil.isMac(), false, PlatformUtil.isMac()));
    }

    private void mousePress() {
        // Simulate Mouse press event to trigger caret placement
        Event.fireEvent(getView(), new MouseEvent(null, getView(), MouseEvent.MOUSE_PRESSED,
                0, 0, 0, 0, MouseButton.PRIMARY, 0,
                false, false, false, false, false,
                false, false, false, false, false, null));
    }

    private void assertSelection(String expectedType) {
        assertEquals(expectedType, getEngine().executeScript("window.getSelection().type"),
                "Selection type should match expected");
    }
}
