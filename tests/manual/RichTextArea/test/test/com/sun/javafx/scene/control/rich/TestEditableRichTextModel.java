/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.rich;

import java.io.StringWriter;
import java.util.function.Consumer;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.EditableRichTextModel;
import javafx.incubator.scene.control.rich.model.StyledInput;
import javafx.incubator.scene.control.rich.model.StyledOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.sun.javafx.scene.control.rich.RichTextFormatHandler;

/**
 * Tests EditableRichTextModel handling of style attributes when editing.
 * The tests use RichTextFormatHandler presentation, which may or may not be the best idea,
 * but it's definitely the quickest.
 */
public class TestEditableRichTextModel {
    @Test
    public void testInsertLineBreak() throws Exception {
        // empty model
        t(
            null,
            (m) -> {
                m.replace(null, TextPos.ZERO, TextPos.ZERO, "\n", false);
            },
            "\n" 
        );

        // two newlines
        t(
            null,
            (m) -> {
                m.replace(null, TextPos.ZERO, TextPos.ZERO, "\n\n", false);
            },
            "\n\n"
        );

        // in front of 1st segment
        t(
            "`B`I``0123",
            (m) -> {
                m.replace(null, TextPos.ZERO, TextPos.ZERO, "\n", false);
            },
            "`B`I``\n`0``0123"
        );

        // in the middle of segment: both parts retain styles
        t(
            "`B`I``0123",
            (m) -> {
                m.replace(null, new TextPos(0, 2), new TextPos(0, 2), "\n", false);
            },
            "`B`I``01\n`0``23"
        );

        // at the end of segment
        t(
            "`B`I``0123",
            (m) -> {
                m.replace(null, new TextPos(0, 4), new TextPos(0, 4), "\n", false);
            },
            "`B`I``0123\n`0``"
        );
    }

    private void t(String initial, Consumer<EditableRichTextModel> op, String expected) throws Exception {
        EditableRichTextModel m = new EditableRichTextModel();
        RichTextFormatHandler h = new RichTextFormatHandler();

        // set initial text
        if (initial != null) {
            StyledInput in = h.createStyledInput(initial);
            TextPos end = m.replace(null, TextPos.ZERO, TextPos.ZERO, in, false);
            // check initial text
            StringWriter wr = new StringWriter();
            StyledOutput out = h.createStyledOutput(null, wr);
            m.exportText(TextPos.ZERO, end, out);
            String s = wr.toString();
            Assertions.assertEquals(initial, s, "problem setting initial text");
        }

        op.accept(m);

        // check output
        {
            StringWriter wr = new StringWriter();
            StyledOutput out = h.createStyledOutput(null, wr);
            TextPos end = m.getDocumentEnd();
            m.exportText(TextPos.ZERO, end, out);
            String s = wr.toString();
            Assertions.assertEquals(expected, s, "operation failed");
        }
    }
}
