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
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.sun.jfx.incubator.scene.control.richtext.RichTextFormatHandlerHelper;
import jfx.incubator.scene.control.richtext.model.ParagraphDirection;
import jfx.incubator.scene.control.richtext.model.RichTextFormatHandler;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledOutput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;

/**
 * Tests RichTextFormatHandler.
 */
public class TestRichTextFormatHandler {
    private static final boolean DEBUG = true;

    @Test
    public void testRoundTrip() throws IOException {
        Object[] ss = {
            List.of(
                p(
                    a(StyleAttributeMap.BACKGROUND, Color.RED),
                    a(StyleAttributeMap.BULLET, "⌘"),
                    a(StyleAttributeMap.FIRST_LINE_INDENT, 10.0),
                    a(StyleAttributeMap.LINE_SPACING, 11.0),
                    a(StyleAttributeMap.PARAGRAPH_DIRECTION, ParagraphDirection.RIGHT_TO_LEFT)
                ),
                s("bold", StyleAttributeMap.BOLD),
                s("font family", a(StyleAttributeMap.FONT_FAMILY, "Arial")),
                s("font size", a(StyleAttributeMap.FONT_SIZE, 12.0)),
                s("italic", StyleAttributeMap.ITALIC),
                nl(),

                p(
                    a(StyleAttributeMap.SPACE_ABOVE, 13.0),
                    a(StyleAttributeMap.SPACE_BELOW, 14.0),
                    a(StyleAttributeMap.SPACE_LEFT, 15.0),
                    a(StyleAttributeMap.SPACE_RIGHT, 16.0),
                    a(StyleAttributeMap.TEXT_ALIGNMENT, TextAlignment.CENTER),
                    a(StyleAttributeMap.PARAGRAPH_DIRECTION, ParagraphDirection.LEFT_TO_RIGHT)
                ),
                s("strike through", StyleAttributeMap.STRIKE_THROUGH),
                s("text color", a(StyleAttributeMap.TEXT_COLOR, Color.GREEN)),
                s("underline", StyleAttributeMap.UNDERLINE),
                nl(),

                s("combined", StyleAttributeMap.ITALIC, a(StyleAttributeMap.TEXT_COLOR, Color.RED), StyleAttributeMap.UNDERLINE),
                nl()

                // TODO test escapes in text, attribute names, attribute values
            )
        };

        RichTextFormatHandler handler = RichTextFormatHandler.INSTANCE;

        for (Object x : ss) {
            testRoundTrip(handler, (List<StyledSegment>)x);
        }
    }

    @Test
    public void testStyleDeduplication() throws IOException {
        StyledSegment[] input = {
            s("0", StyleAttributeMap.BOLD),
            s("1", StyleAttributeMap.ITALIC),
            s("2", StyleAttributeMap.BOLD),
            s("3", StyleAttributeMap.ITALIC)
        };

        StringWriter wr = new StringWriter();
        StyledOutput out = RichTextFormatHandlerHelper.createStyledOutput(RichTextFormatHandler.INSTANCE, null, wr);
        for (StyledSegment s : input) {
            out.consume(s);
        }
        out.flush();
        String s = wr.toString();
        Assertions.assertTrue(s.indexOf("{0}") > 0);
        Assertions.assertTrue(s.indexOf("{1}") > 0);
    }

    @Test
    public void testEscapes() throws IOException {
        StringWriter wr = new StringWriter();
        StyledOutput out = RichTextFormatHandlerHelper.createStyledOutput(RichTextFormatHandler.INSTANCE, null, wr);
        out.consume(StyledSegment.of("{|%}"));
        out.flush();
        String s = wr.toString();
        String expected = "%7B%7C%25%7D";
        Assertions.assertEquals(expected, s);
    }

    // creates a segment with paragraph attributes
    private static StyledSegment p(Object... items) {
        StyleAttributeMap.Builder b = StyleAttributeMap.builder();
        for (Object x : items) {
            if (x instanceof StyleAttribute a) {
                b.set(a, Boolean.TRUE);
            } else if (x instanceof StyleAttributeMap a) {
                b.merge(a);
            } else {
                throw new Error("?" + x);
            }
        }
        StyleAttributeMap attrs = b.build();
        checkParagraphType(attrs, true);
        return StyledSegment.ofParagraphAttributes(attrs);
    }

    // creates a text segment
    private static StyledSegment s(String text, Object... items) {
        StyleAttributeMap.Builder b = StyleAttributeMap.builder();
        for (Object x : items) {
            if (x instanceof StyleAttribute a) {
                b.set(a, Boolean.TRUE);
            } else if (x instanceof StyleAttributeMap a) {
                b.merge(a);
            } else {
                throw new Error("?" + x);
            }
        }
        StyleAttributeMap attrs = b.build();
        checkParagraphType(attrs, false);
        return StyledSegment.of(text, attrs);
    }

    private static void checkParagraphType(StyleAttributeMap attrs, boolean forParagraph) {
        for (StyleAttribute a : attrs.getAttributes()) {
            Assertions.assertEquals(forParagraph, a.isParagraphAttribute(), "wrong isParagraph: " + a);
        }
    }

    private static <T> StyleAttributeMap a(StyleAttribute<T> a, T value) {
        return StyleAttributeMap.builder().set(a, value).build();
    }

    private static StyledSegment nl() {
        return StyledSegment.LINE_BREAK;
    }

    private void testRoundTrip(RichTextFormatHandler handler, List<StyledSegment> input) throws IOException {
        // export to string
        int ct = 0;
        StringWriter wr = new StringWriter();
        StyledOutput out = RichTextFormatHandlerHelper.createStyledOutput(handler, null, wr);
        for (StyledSegment s : input) {
            if (DEBUG) {
                System.out.println(s);
            }
            out.consume(s);
            ct++;
        }
        out.flush();
        String exported = wr.toString();
        if (DEBUG) {
            System.out.println("exported " + ct + " segments=" + exported);
        }

        // import from string
        ArrayList<StyledSegment> segments = new ArrayList<>();
        StyledInput in = handler.createStyledInput(exported, null);
        StyledSegment seg;
        while ((seg = in.nextSegment()) != null) {
            if (DEBUG) {
                System.out.println(seg);
            }
            segments.add(seg);
        }

        // check segments for equality
        Assertions.assertEquals(input.size(), segments.size());
        for (int i = 0; i < input.size(); i++) {
            StyledSegment is = input.get(i);
            StyledSegment rs = segments.get(i);
            Assertions.assertEquals(is.getType(), rs.getType());
            Assertions.assertEquals(is.getText(), rs.getText());
            Assertions.assertEquals(is.getStyleAttributeMap(null), rs.getStyleAttributeMap(null));
        }

        // export to a string again
        wr = new StringWriter();
        out = RichTextFormatHandlerHelper.createStyledOutput(handler, null, wr);
        for (StyledSegment s : segments) {
            out.consume(s);
        }
        out.flush();
        String result = wr.toString();
        if (DEBUG) {
            System.out.println("result=" + result);
        }

        // relying on stable order of attributes
        Assertions.assertEquals(exported, result);
    }

    private void testRoundTrip_DELETE(RichTextFormatHandler handler, String text) throws IOException {
        ArrayList<StyledSegment> segments = new ArrayList<>();

        StyledInput in = handler.createStyledInput(text, null);
        StyledSegment seg;
        while ((seg = in.nextSegment()) != null) {
            segments.add(seg);
            if (DEBUG) {
                System.out.println(seg);
            }
        }

        StringWriter wr = new StringWriter();
        StyledOutput out = RichTextFormatHandlerHelper.createStyledOutput(handler, null, wr);
        for (StyledSegment s : segments) {
            out.consume(s);
        }
        out.flush();

        String result = wr.toString();
        Assertions.assertEquals(text, result);
    }
}
