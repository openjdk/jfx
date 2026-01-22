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

package test.jfx.incubator.scene.control.richtext.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.text.TabStop;
import javafx.scene.text.TextAlignment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.jfx.incubator.scene.control.richtext.SegmentStyledInput;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.ParagraphDirection;
import jfx.incubator.scene.control.richtext.model.RichTextFormatHandler;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;

/**
 * Tests RichTextAreaModel functionality by exporting via the RichTextFormatHandler.
 */
public class TestRichExport {
    private RichTextModel model;

    @BeforeEach
    public void beforeEach() {
        model = new RichTextModel();
    }

    @Test
    public void emptyModel() {
        verify("{}{!}");
    }

    @Test
    public void simpleTextModel() {
        append("1/n2");
        verify("{}1/n2{!}");
    }

    @Test
    public void mixedAttributes() {
        setParagraphAttributes(StyleAttributeMap.SPACE_ABOVE, 3.3);
        append("text");
        append("BOLD", StyleAttributeMap.BOLD);
        append("ITALIC", StyleAttributeMap.ITALIC);
        append("BOLD", StyleAttributeMap.BOLD);
        append("none");
        nl();

        // {number} specs refer to the deduplicated attribute set:
        // {0} == {b}
        // {!2} == {!spaceAbove|3.3}
        verify("""
            {}text{b}BOLD{i}ITALIC{0}BOLD{}none{!spaceAbove|3.3}
            {!2}""");
    }

    @Test
    public void attribute_BACKGROUND() {
        setParagraphAttributes(StyleAttributeMap.BACKGROUND, Color.RED);
        append("BACKGROUND");
        verify("{}BACKGROUND{!bg|FF0000}");
    }

    @Test
    public void attribute_BULLET() {
        setParagraphAttributes(StyleAttributeMap.BULLET, ">");
        append("BULLET");
        verify("{}BULLET{!bullet|>}");
    }

    @Test
    public void attribute_BOLD() {
        append("BOLD", StyleAttributeMap.BOLD);
        verify("{b}BOLD{!}");
    }

    @Test
    public void attribute_FIRST_LINE_INDENT() {
        setParagraphAttributes(StyleAttributeMap.FIRST_LINE_INDENT, 3.3);
        append("FIRST_LINE_INDENT");
        verify("{}FIRST_LINE_INDENT{!firstIndent|3.3}");
    }

    @Test
    public void attribute_FONT_FAMILY() {
        append("FONT_FAMILY", StyleAttributeMap.FONT_FAMILY, "Courier New");
        verify("{ff|Courier New}FONT_FAMILY{!}");
    }

    @Test
    public void attribute_FONT_SIZE() {
        append("FONT_SIZE", StyleAttributeMap.FONT_SIZE, 3.3);
        verify("{fs|3.3}FONT_SIZE{!}");
    }

    @Test
    public void attribute_ITALIC() {
        append("ITALIC", StyleAttributeMap.ITALIC);
        verify("{i}ITALIC{!}");
    }

    @Test
    public void attribute_LINE_SPACING() {
        setParagraphAttributes(StyleAttributeMap.LINE_SPACING, 3.3);
        append("LINE_SPACING");
        verify("{}LINE_SPACING{!lineSpacing|3.3}");
    }

    @Test
    public void attribute_PARAGRAPH_DIRECTION() {
        setParagraphAttributes(StyleAttributeMap.PARAGRAPH_DIRECTION, ParagraphDirection.RIGHT_TO_LEFT);
        append("R");
        nl();
        setParagraphAttributes(StyleAttributeMap.PARAGRAPH_DIRECTION, ParagraphDirection.LEFT_TO_RIGHT);
        append("L");

        verify("""
            {}R{!dir|R}
            {}L{!dir|L}""");
    }

    @Test
    public void attribute_SPACE_ABOVE() {
        setParagraphAttributes(StyleAttributeMap.SPACE_ABOVE, 3.3);
        append("SPACE_ABOVE");
        verify("{}SPACE_ABOVE{!spaceAbove|3.3}");
    }

    @Test
    public void attribute_SPACE_BELOW() {
        setParagraphAttributes(StyleAttributeMap.SPACE_BELOW, 3.3);
        append("SPACE_BELOW");
        verify("{}SPACE_BELOW{!spaceBelow|3.3}");
    }

    @Test
    public void attribute_SPACE_LEFT() {
        setParagraphAttributes(StyleAttributeMap.SPACE_LEFT, 3.3);
        append("SPACE_LEFT");
        verify("{}SPACE_LEFT{!spaceLeft|3.3}");
    }

    @Test
    public void attribute_SPACE_RIGHT() {
        setParagraphAttributes(StyleAttributeMap.SPACE_RIGHT, 3.3);
        append("SPACE_RIGHT");
        verify("{}SPACE_RIGHT{!spaceRight|3.3}");
    }

    @Test
    public void attribute_STRIKE_THROUGH() {
        append("STRIKE_THROUGH", StyleAttributeMap.STRIKE_THROUGH);
        verify("{ss}STRIKE_THROUGH{!}");
    }

    @Test
    public void attribute_TAB_STOPS() {
        TabStop[] ts = {
            new TabStop(11),
            new TabStop(22)
        };
        setParagraphAttributes(StyleAttributeMap.TAB_STOPS, ts);
        append("TAB_STOPS");
        verify("{}TAB_STOPS{!tabs|11.0,22.0}");
    }

    @Test
    public void attribute_TEXT_ALIGNMENT() {
        setParagraphAttributes(StyleAttributeMap.TEXT_ALIGNMENT, TextAlignment.CENTER);
        append("C");
        nl();
        setParagraphAttributes(StyleAttributeMap.TEXT_ALIGNMENT, TextAlignment.JUSTIFY);
        append("J");
        nl();
        setParagraphAttributes(StyleAttributeMap.TEXT_ALIGNMENT, TextAlignment.LEFT);
        append("L");
        nl();
        setParagraphAttributes(StyleAttributeMap.TEXT_ALIGNMENT, TextAlignment.RIGHT);
        append("R");

        verify("""
            {}C{!alignment|C}
            {}J{!alignment|J}
            {}L{!alignment|L}
            {}R{!alignment|R}""");
    }

    @Test
    public void attribute_TEXT_COLOR() {
        append("TEXT_COLOR", StyleAttributeMap.TEXT_COLOR, Color.RED);
        verify("{tc|FF0000}TEXT_COLOR{!}");
    }

    @Test
    public void attribute_UNDERLINE() {
        append("UNDERLINE", StyleAttributeMap.UNDERLINE);
        verify("{u}UNDERLINE{!}");
    }

    private void verify(String expected) {
        Object v;
        try {
            v = RichTextFormatHandler.getInstance().copy(model, null, TextPos.ZERO, model.getDocumentEnd());
        } catch (Throwable e) {
            Assertions.fail(e);
            v = null;
        }
        assertTrue(v instanceof String);
        assertEquals(expected, v);
    }

    private void setParagraphAttributes(Object... items) {
        StyleAttributeMap.Builder b = StyleAttributeMap.builder();
        for (int i = 0; i < items.length;) {
            StyleAttribute a = (StyleAttribute)items[i++];
            if (!a.isParagraphAttribute()) {
                throw new IllegalArgumentException("Expecting a paragraph attribute: " + a);
            }
            Object v = items[i++];
            b.set(a, v);
        }
        StyledInput in = SegmentStyledInput.of(List.of(StyledSegment.ofParagraphAttributes(b.build())));
        append(in);
    }

    private void append(String text) {
        StyledInput in = StyledInput.of(text);
        append(in);
    }

    private void nl() {
        StyledInput in = StyledInput.of("\n");
        append(in);
    }

    private void append(String text, StyleAttribute<Boolean> a) {
        StyledInput in = StyledInput.of(text, StyleAttributeMap.of(a, Boolean.TRUE));
        append(in);
    }

    private void append(String text, StyleAttribute attr, Object value) {
        StyledInput in = StyledInput.of(text, StyleAttributeMap.of(attr, value));
        append(in);
    }

    private void append(StyledInput in) {
        TextPos p = model.getDocumentEnd();
        model.replace(null, p, p, in);
    }
}
