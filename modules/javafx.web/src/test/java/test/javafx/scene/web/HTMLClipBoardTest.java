/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HTMLClipBoardTest extends TestBase {

    /**
     * @test
     * @bug 8269115
     * summary WebView paste event contains old data
     */
    @Test public void DataOnPaste() {
        // Read the clipboard data from JavaScript onpaste event
        // Verify the paste data from pasteTarget element
        String defaultText = "Default";
        loadContent(
                "<html>\n" +
                        "<head> \n" +
                        "   \n" +
                        "</head>\n" +
                        "<body>\n" +
                        "<b>This is a test of the clipboard. The content of the clipboard will be displayed below after pressing ctrl+v:</b>\n" +
                        "<input type=\"button\" id=\"copyID\" value=\"Hello World\" />\n" +
                        "<div id=\"clipboardData\"></div>\n" +
                        " <script>\n" +
                        "        document.addEventListener('paste', e => {\n" +
                        "            let messages = [];\n" +
                        "            if (e.clipboardData.types) {\n" +
                        "                let message_index = 0;\n" +
                        "                e.clipboardData.types.forEach(type => {\n" +
                        "                    messages.push( type + \": \" + e.clipboardData.getData(type));\n" +
                        "                    const para = document.createElement(\"p\");\n" +
                        "                    para.innerText = type + \": \" + e.clipboardData.getData(type);\n" +
                        "                    document.getElementById(\"clipboardData\").innerText = ++message_index;\n" +
                        "                });\n" +
                        "            }\n" +
                        "        });\n" +
                        "\n" +
                        "</script>\n" +
                        "</body>\n" +
                        "</html>");

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

            assertEquals("Source Default value","2", getEngine().
                            executeScript("document.getElementById(\"clipboardData\").innerText").toString());
        });
    }
}