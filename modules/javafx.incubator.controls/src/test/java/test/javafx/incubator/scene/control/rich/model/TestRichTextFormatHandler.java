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

package test.javafx.incubator.scene.control.rich.model;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javafx.incubator.scene.control.rich.model.RichTextFormatHandler;
import javafx.incubator.scene.control.rich.model.StyleAttribute;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.incubator.scene.control.rich.model.StyledInput;
import javafx.incubator.scene.control.rich.model.StyledOutput;
import javafx.incubator.scene.control.rich.model.StyledSegment;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.sun.javafx.incubator.scene.control.rich.RichTextFormatHandlerHelper;

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
                    a(StyleAttrs.BACKGROUND, Color.RED),
                    a(StyleAttrs.BULLET, "⌘"),
                    a(StyleAttrs.FIRST_LINE_INDENT, 10.0),
                    a(StyleAttrs.LINE_SPACING, 11.0),
                    StyleAttrs.RIGHT_TO_LEFT
                ),
                s("bold", StyleAttrs.BOLD),
                s("font family", a(StyleAttrs.FONT_FAMILY, "Arial")),
                s("font size", a(StyleAttrs.FONT_SIZE, 12.0)),
                s("italic", StyleAttrs.ITALIC),
                nl(),

                p(
                    a(StyleAttrs.SPACE_ABOVE, 13.0),
                    a(StyleAttrs.SPACE_BELOW, 14.0),
                    a(StyleAttrs.SPACE_LEFT, 15.0),
                    a(StyleAttrs.SPACE_RIGHT, 16.0),
                    a(StyleAttrs.TEXT_ALIGNMENT, TextAlignment.CENTER)
                ),
                s("strike through", StyleAttrs.STRIKE_THROUGH),
                s("text color", a(StyleAttrs.TEXT_COLOR, Color.GREEN)),
                s("underline", StyleAttrs.UNDERLINE),
                nl(),

                s("combined", StyleAttrs.ITALIC, a(StyleAttrs.TEXT_COLOR, Color.RED), StyleAttrs.UNDERLINE),
                nl()

                // TODO test escapes in text, attribute names, attribute values
            )
        };

        RichTextFormatHandler handler = new RichTextFormatHandler();

        for (Object x : ss) {
            testRoundTrip(handler, (List<StyledSegment>)x);
        }
    }

    @Test
    public void testStyleDeduplication() throws IOException {
        StyledSegment[] input = {
            s("0", StyleAttrs.BOLD),
            s("1", StyleAttrs.ITALIC),
            s("2", StyleAttrs.BOLD),
            s("3", StyleAttrs.ITALIC)
        };

        StringWriter wr = new StringWriter();
        StyledOutput out = RichTextFormatHandlerHelper.createStyledOutput(new RichTextFormatHandler(), null, wr);
        for (StyledSegment s : input) {
            out.append(s);
        }
        out.flush();
        String s = wr.toString();
        Assertions.assertTrue(s.indexOf("{0}") > 0);
        Assertions.assertTrue(s.indexOf("{1}") > 0);
    }

    @Test
    public void testEscapes() throws IOException {
        StringWriter wr = new StringWriter();
        StyledOutput out = RichTextFormatHandlerHelper.createStyledOutput(new RichTextFormatHandler(), null, wr);
        out.append(StyledSegment.of("{|%}"));
        out.flush();
        String s = wr.toString();
        String expected = "%7B%7C%25%7D";
        Assertions.assertEquals(expected, s);
    }

    // creates a segment with paragraph attributes
    private static StyledSegment p(Object... items) {
        StyleAttrs.Builder b = StyleAttrs.builder();
        for (Object x : items) {
            if (x instanceof StyleAttribute a) {
                b.set(a, Boolean.TRUE);
            } else if (x instanceof StyleAttrs a) {
                b.merge(a);
            } else {
                throw new Error("?" + x);
            }
        }
        StyleAttrs attrs = b.build();
        checkParagraphType(attrs, true);
        return StyledSegment.ofParagraphAttributes(attrs);
    }

    // creates a text segment
    private static StyledSegment s(String text, Object... items) {
        StyleAttrs.Builder b = StyleAttrs.builder();
        for (Object x : items) {
            if (x instanceof StyleAttribute a) {
                b.set(a, Boolean.TRUE);
            } else if (x instanceof StyleAttrs a) {
                b.merge(a);
            } else {
                throw new Error("?" + x);
            }
        }
        StyleAttrs attrs = b.build();
        checkParagraphType(attrs, false);
        return StyledSegment.of(text, attrs);
    }

    private static void checkParagraphType(StyleAttrs attrs, boolean forParagraph) {
        for (StyleAttribute a : attrs.getAttributes()) {
            Assertions.assertEquals(forParagraph, a.isParagraphAttribute(), "wrong isParagraph: " + a);
        }
    }

    private static <T> StyleAttrs a(StyleAttribute<T> a, T value) {
        return StyleAttrs.builder().set(a, value).build();
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
            out.append(s);
            ct++;
        }
        out.flush();
        String exported = wr.toString();
        if (DEBUG) {
            System.out.println("exported " + ct + " segments=" + exported);
        }

        // import from string
        ArrayList<StyledSegment> segments = new ArrayList<>();
        StyledInput in = handler.createStyledInput(exported);
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
            Assertions.assertEquals(is.getStyleAttrs(null), rs.getStyleAttrs(null));
        }

        // export to a string again
        wr = new StringWriter();
        out = RichTextFormatHandlerHelper.createStyledOutput(handler, null, wr);
        for (StyledSegment s : segments) {
            out.append(s);
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

        StyledInput in = handler.createStyledInput(text);
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
            out.append(s);
        }
        out.flush();

        String result = wr.toString();
        Assertions.assertEquals(text, result);
    }
}
