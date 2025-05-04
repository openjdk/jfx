/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.jfx.incubator.scene.control.richtext.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import com.sun.javafx.tk.Toolkit;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * Utilities for RichTextArea-based tests.
 */
public class RTUtil {

    private RTUtil() {
    }

    /**
     * Replaces existing content with the specified non-styled text string.
     *
     * @param control the rich text area
     * @param text the text
     */
    public static void setText(RichTextArea control, String text) {
        TextPos end = control.getDocumentEnd();
        control.replaceText(TextPos.ZERO, end, text, false);
    }

    /**
     * Extracts plain text from the supplied RichTextArea, using {@code write(DataFormat.PLAIN_TEXT)} method.
     *
     * @param control the rich text area
     * @return the plain text
     */
    public static String getText(RichTextArea control) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            try {
                control.write(DataFormat.PLAIN_TEXT, out);
                byte[] b = out.toByteArray();
                return new String(b, StandardCharsets.UTF_8);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Clears the system Clipboard.
     */
    public static void copyToClipboard(String text) {
        ClipboardContent cc = new ClipboardContent();
        cc.putString(text);
        Clipboard.getSystemClipboard().setContent(cc);
        firePulse();
    }

    /**
     * Fires a pulse.
     */
    public static void firePulse() {
        Toolkit.getToolkit().firePulse();
    }
}
