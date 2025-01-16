/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.jfx.incubator.scene.control.richtext.model;

import java.io.IOException;
import java.io.StringWriter;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.sun.jfx.incubator.scene.control.richtext.RichTextFormatHandlerHelper;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichTextFormatHandler;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledOutput;

/**
 * Tests RichTextModel handling of style attributes when editing.
 * The tests use RichTextFormatHandler presentation, which may or may not be the best idea,
 * but it's definitely the quickest.
 */
public class TestRichTextModelAttributes {
    @Test
    public void testInsertLineBreak() {
        // empty model
        t(
            null,
            (m) -> {
                m.replace(null, TextPos.ZERO, TextPos.ZERO, "\n", false);
            },
            "{!}\n{!}"
        );

        // two newlines
        t(
            null,
            (m) -> {
                m.replace(null, TextPos.ZERO, TextPos.ZERO, "\n\n", false);
            },
            "{!}\n{!}\n{!}"
        );

        // in front of 1st segment
        t(
            "{b}{i}0123{!}",
            (m) -> {
                m.replace(null, TextPos.ZERO, TextPos.ZERO, "\n", false);
            },
            "{!}\n{b}{i}0123{!}"
        );

        // in the middle of segment: both parts retain styles
        t(
            "{b}{i}0123{!}",
            (m) -> {
                m.replace(null, TextPos.ofLeading(0, 2), TextPos.ofLeading(0, 2), "\n", false);
            },
            "{b}{i}01{!}\n{0}23{!}"
        );

        // at the end of segment
        t(
            "{b}{i}0123{!}",
            (m) -> {
                m.replace(null, TextPos.ofLeading(0, 4), TextPos.ofLeading(0, 4), "\n", false);
            },
            "{b}{i}0123{!}\n{!}"
        );
    }

    @Test
    public void testDeleteParagraphStart() {
        t(
            "{fs|24.0}{tc|808080}aaaaa:  {fs|24.0}bbbbb{!}",
            (m) -> {
                m.replace(null, p(0, 13), p(0, 0), "", false);
            },
            "{!}"
        );
    }

    @Test
    public void testZeroWidthSegment() {
        t(
            "{fs|24.0}{tc|808080}a: {fs|24.0}b{!}\n{0}c: {1}d{!}",
            (m) -> {
                m.replace(null, p(0, 4), p(1, 0), "", false);
            },
            "{fs|24.0}{tc|808080}a: {fs|24.0}b{0}c: {1}d{!}"
        );
    }

    private static TextPos p(int index, int offset) {
        return TextPos.ofLeading(index, offset);
    }

    private void t(String initial, Consumer<RichTextModel> op, String expected) {
        try {
            RichTextModel m = new RichTextModel();
            RichTextFormatHandler h = RichTextFormatHandler.getInstance();

            // set initial text
            if (initial != null) {
                StyledInput in = h.createStyledInput(initial, null);
                TextPos end = m.replace(null, TextPos.ZERO, TextPos.ZERO, in, false);
                // check initial text
                StringWriter wr = new StringWriter();
                StyledOutput out = RichTextFormatHandlerHelper.createStyledOutput(h, null, wr);
                m.export(TextPos.ZERO, end, out);
                String s = wr.toString();
                Assertions.assertEquals(initial, s, "problem setting initial text");
            }

            op.accept(m);

            // check output
            {
                StringWriter wr = new StringWriter();
                StyledOutput out = RichTextFormatHandlerHelper.createStyledOutput(h, null, wr);
                TextPos end = m.getDocumentEnd();
                m.export(TextPos.ZERO, end, out);
                String s = wr.toString();
                Assertions.assertEquals(expected, s, "operation failed");
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}
