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

package test.jfx.incubator.scene.control.richtext.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javafx.scene.input.DataFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.DataFormatHandler;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Tests RichTextModel -> HTML export.
 */
public class HTMLExportTest {
    private RichTextModel model;

    @BeforeEach
    public void beforeEach() {
        model = new RichTextModel();
    }

    @Test
    public void characterAttributes() throws Exception {
        model.replace(null, TextPos.ZERO, TextPos.ZERO, "111\n");
        check(
            """
            <html>
            <head>
            <meta charset="utf-8">
            </head>
            <body>
            111<p/>

            </body></html>
            """);

        // bold
        model.applyStyle(TextPos.ZERO, TextPos.ofLeading(0, 1), mk(StyleAttributeMap.BOLD, Boolean.TRUE), false);
        checkContains("<span style='font-weight: bold;'>1</span>11<p/>");
        model.applyStyle(TextPos.ZERO, TextPos.ofLeading(0, 1), mk(StyleAttributeMap.BOLD, Boolean.FALSE), false);
        checkContains("111<p/>");
        // italic
        model.applyStyle(TextPos.ZERO, TextPos.ofLeading(0, 1), mk(StyleAttributeMap.ITALIC, Boolean.TRUE), false);
        checkContains("<span style='font-style: italic;'>1</span>11<p/>");
        model.applyStyle(TextPos.ZERO, TextPos.ofLeading(0, 1), mk(StyleAttributeMap.ITALIC, Boolean.FALSE), false);
        checkContains("111<p/>");
        // strikethrough
        model.applyStyle(TextPos.ZERO, TextPos.ofLeading(0, 1), mk(StyleAttributeMap.STRIKE_THROUGH, Boolean.TRUE), false);
        checkContains("<span style='text-decoration: line-through;'>1</span>11<p/>");
        model.applyStyle(TextPos.ZERO, TextPos.ofLeading(0, 1), mk(StyleAttributeMap.STRIKE_THROUGH, Boolean.FALSE), false);
        checkContains("111<p/>");
        // underline
        model.applyStyle(TextPos.ZERO, TextPos.ofLeading(0, 1), mk(StyleAttributeMap.UNDERLINE, Boolean.TRUE), false);
        checkContains("<span style='text-decoration: underline;'>1</span>11<p/>");
        model.applyStyle(TextPos.ZERO, TextPos.ofLeading(0, 1), mk(StyleAttributeMap.UNDERLINE, Boolean.FALSE), false);
        checkContains("111<p/>");
    }

    private String toHtml() throws Exception {
        DataFormatHandler h = model.getDataFormatHandler(DataFormat.HTML, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        h.save(model, null, TextPos.ZERO, model.getDocumentEnd(), out);
        byte[] b = out.toByteArray();
        return new String(b, StandardCharsets.UTF_8);
    }

    private static <T> StyleAttributeMap mk(StyleAttribute<T> attr, T value) {
        return StyleAttributeMap.builder().set(attr, value).build();
    }

    private void check(String expected) throws Exception {
        String html = toHtml();
        assertEquals(expected, html);
    }

    private void checkContains(String pattern) throws Exception {
        String html = toHtml();
        assertTrue(html.contains(pattern), html);
    }
}
