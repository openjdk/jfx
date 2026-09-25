/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.test.manual.text;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import com.oracle.test.manual.util.ManualTestWindow;
import com.oracle.test.manual.util.OS;

public class EmojiTest extends ManualTestWindow {

    public EmojiTest() {
        super(
            "Emoji Rendering Test (macOS)",
            """
            This test verifies rendering of Emoji glyphs on macOS using Text node, Label control, and TextField control.

            Test passes if:

            1. a yellow-coloured smiling face image embedded between 'ab' and 'cd' in all three places
            2. that the editable text field handles selection of the emoji glyph with the same background as other glyphs
            """,
            600, 500
        );
        // alternative: use annotation?
        // note: we may want to remove this because you should be able to run the test on any platform.
        setRunOn(OS.MAC);
    }

    @Override
    public Node createContent() {
        Font font = new Font(32);
        String emojiString = "ab\ud83d\ude00cd";
        Text text = new Text(emojiString);
        text.setFont(font);
        Label label = new Label(emojiString);
        label.setFont(font);
        TextField textField = new TextField(emojiString);
        textField.setFont(font);

        return new VBox(
            2,
            text,
            label,
            textField
        );
    }
}
