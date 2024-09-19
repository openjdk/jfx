/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.Event;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Test;
import org.junit.Ignore;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HTMLEditingTest extends TestBase {

    /**
     * @test
     * @bug 8143894
     * summary intercept clipboard data in javascript for onpaste event
     */
    @Ignore("JDK-8290237")
    @Test public void clipboardGetDataOnPaste() {
        // Read the clipboard data from JavaScript onpaste event
        // Verify the paste data from pasteTarget element
        String defaultText = "Default";
        loadContent(
                "<input id='srcInput' value=" + defaultText + " autofocus>" +
                "<input id='pasteTarget'></input>" +
                "<script>"+
                "srcInput.onpaste = function(e) {" +
                "pasteTarget.value = e.clipboardData.getData('text/plain');}" +
                "</script>");

        submit(() -> {
            assertTrue("LoadContent completed successfully",
                    getEngine().getLoadWorker().getState() == SUCCEEDED);
            String clipboardData = "Clipboard text";
            ClipboardContent content = new ClipboardContent();
            content.putString(clipboardData);
            Clipboard.getSystemClipboard().setContent(content);

            Event.fireEvent(getView(),
                    new KeyEvent(null,getView(),
                            KeyEvent.KEY_PRESSED,
                            "", "", KeyCode.V,
                            false, !PlatformUtil.isMac(),// Ctrl+V(Non Mac)
                            false, PlatformUtil.isMac()));// Cmd+V (Mac)

            assertEquals("Source Default value",defaultText,getEngine().
                    executeScript("srcInput.defaultValue").toString());
            assertEquals("Source clipboard onpaste data", clipboardData + defaultText, getEngine().
                    executeScript("srcInput.value").toString());
            assertEquals("Target onpaste data",clipboardData, getEngine().
                    executeScript("pasteTarget.value").toString());
        });
    }
}
