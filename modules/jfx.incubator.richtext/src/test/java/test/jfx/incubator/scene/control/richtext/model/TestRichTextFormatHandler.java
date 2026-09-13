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

package test.jfx.incubator.scene.control.richtext.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.sun.jfx.incubator.scene.control.richtext.EmbeddedImageHelper;
import com.sun.jfx.incubator.scene.control.richtext.RichTextFormatHandlerHelper;
import com.sun.jfx.incubator.scene.control.richtext.StyleAttributeMapHelper;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.ParagraphDirection;
import jfx.incubator.scene.control.richtext.model.RichTextFormatHandler;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledOutput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;
import jfx.incubator.scene.control.richtext.model.TabStops;
import test.jfx.incubator.scene.control.richtext.support.RTUtil;

/**
 * Tests RichTextFormatHandler.
 */
public class TestRichTextFormatHandler {
    private static boolean DEBUG = false;

    @Test
    public void testStandardAttributes() throws IOException {
        StyledSegment[] segments = {
            s("bold", StyleAttributeMap.BOLD),
            s("font family", a(StyleAttributeMap.FONT_FAMILY, "Arial")),
            s("font size", a(StyleAttributeMap.FONT_SIZE, 12.0)),
            s("italic", StyleAttributeMap.ITALIC),
            p(
                a(StyleAttributeMap.BACKGROUND, Color.RED),
                a(StyleAttributeMap.BULLET, "⌘"),
                a(StyleAttributeMap.FIRST_LINE_INDENT, 10.0),
                a(StyleAttributeMap.LINE_SPACING, 11.0),
                a(StyleAttributeMap.PARAGRAPH_DIRECTION, ParagraphDirection.RIGHT_TO_LEFT),
                a(StyleAttributeMap.TAB_STOPS, TabStops.of(100, 200))
            ),
            nl(),

            s("strike through", StyleAttributeMap.STRIKE_THROUGH),
            s("text color", a(StyleAttributeMap.TEXT_COLOR, Color.GREEN)),
            s("underline", StyleAttributeMap.UNDERLINE),
            p(
                a(StyleAttributeMap.SPACE_ABOVE, 13.0),
                a(StyleAttributeMap.SPACE_BELOW, 14.0),
                a(StyleAttributeMap.SPACE_LEFT, 15.0),
                a(StyleAttributeMap.SPACE_RIGHT, 16.0),
                a(StyleAttributeMap.TEXT_ALIGNMENT, TextAlignment.CENTER),
                a(StyleAttributeMap.PARAGRAPH_DIRECTION, ParagraphDirection.LEFT_TO_RIGHT)
            ),
            nl(),

            s("combined", StyleAttributeMap.ITALIC, a(StyleAttributeMap.TEXT_COLOR, Color.RED), StyleAttributeMap.UNDERLINE),
            s(" ", StyleAttributeMap.of(StyleAttributeMap.EMBEDDED_IMAGE, EmbeddedImageHelper.create(RTUtil.redPng32x32(), 32, 32, 32, 32, true))),
            nl(),
            s("highlight1", StyleAttributeMap.TEXT_HIGHLIGHT_1),
            s("highlight2", StyleAttributeMap.TEXT_HIGHLIGHT_2),
            s("highlight3", StyleAttributeMap.TEXT_HIGHLIGHT_3),
            s("highlight4", StyleAttributeMap.TEXT_HIGHLIGHT_4),
            s("highlight5", StyleAttributeMap.TEXT_HIGHLIGHT_5),
            s("wavy1", StyleAttributeMap.WAVY_UNDERLINE_1),
            s("wavy2", StyleAttributeMap.WAVY_UNDERLINE_2),
            s("wavy3", StyleAttributeMap.WAVY_UNDERLINE_3),
            nl()
        };
        testRoundTrip(segments);
        checkAll(segments);
    }

    /// Make sure all attributes declared in StyleAttributeMap are included.
    private static void checkAll(StyledSegment[] segments) {
        Set<StyleAttribute<?>> declared = new HashSet<>();
        int mask = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
        for (Field f : StyleAttributeMap.class.getDeclaredFields()) {
            int mod = f.getModifiers();
            if ((mod & mask) == mask) {
                Class<?> type = f.getType();
                if (type.isAssignableFrom(StyleAttribute.class)) {
                    try {
                        declared.add((StyleAttribute<?>)f.get(null));
                    } catch (Exception e) {
                        fail(e);
                    }
                }
            }
        }

        for (StyledSegment seg : segments) {
            StyleAttributeMap attr = seg.getStyleAttributeMap(null);
            if (attr != null) {
                for (StyleAttribute<?> a : attr.getAttributes()) {
                    declared.remove(a);
                }
            }
        }

        assertTrue(declared.isEmpty(), "round trip test missed " + declared);
    }

    @Test
    public void testDocumentProperties() throws IOException {
        docp(Map.of(), 0, null);
        docp(Map.of("{}%|\"", "{}%|\""), 1, TestRichTextModel.VERSION + "{#%7B%7D%25%7C\"|%7B%7D%25%7C\"}{}{!}");
        docp(Map.of("title", ""), 1, TestRichTextModel.VERSION + "{#title|}{}{!}");
    }

    private void docp(Map<String,String> props, int expectedCount, String expectedSave) throws IOException {
        RichTextModel m = new RichTextModel() {
            @Override
            protected Map<String,String> documentProperties() {
                return props;
             }
        };

        String s = save(m);
        if (expectedSave != null) {
            assertEquals(expectedSave, s);
        }

        RichTextFormatHandler h = RichTextFormatHandler.getInstance();
        int count = 0;
        StyledInput in = h.createStyledInput(s, null);
        StyledSegment seg;
        while ((seg = in.nextSegment()) != null) {
            switch (seg.getType()) {
            case DOCUMENT_PROPERTIES:
                count++;
                Map<String,String> dp = seg.getDocumentProperties();
                assertEquals(props, dp);
                break;
            }
        }
        assertEquals(expectedCount, count, "missing document segment");
    }

    // JDK-8357393
    @Test
    public void testEmptyCharAttributeToken() throws IOException {
        testRoundTrip(
            s("normal"),
            s("BOLD", StyleAttributeMap.BOLD),
            s("normal")
        );
    }

    @Test
    public void testEmptyParagraphAttributeToken() throws IOException {
        testRoundTrip(
            s("normal"),
            p()
        );
    }

    @Test
    public void testUnknownAttributes() throws IOException {
        testReadWrite("{unknown}text{!UNKNOWN}\n", "{}text{}\n");
        testReadWrite("{unknown}text{!UNKNOWN}{!alignment|R}\n", "{}text{!alignment|R}\n");
        testReadWrite("{unknown}{b}text\n", "{b}text\n");
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
        StyledOutput out = RichTextFormatHandlerHelper.createStyledOutput(RichTextFormatHandler.getInstance(), null, wr);
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
        StyledOutput out = RichTextFormatHandlerHelper.createStyledOutput(RichTextFormatHandler.getInstance(), null, wr);
        out.consume(StyledSegment.of("{|%}"));
        out.flush();
        String s = wr.toString();
        String expected = "{}%7B%7C%25%7D";
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
            Assertions.assertTrue(StyleAttributeMapHelper.isAcceptable(a, forParagraph), "wrong type: " + a);
        }
    }

    private static <T> StyleAttributeMap a(StyleAttribute<T> a, T value) {
        return StyleAttributeMap.builder().set(a, value).build();
    }

    private static StyledSegment nl() {
        return StyledSegment.LINE_BREAK;
    }

    private void testReadWrite(String input, String expected) throws IOException {
        RichTextFormatHandler handler = RichTextFormatHandler.getInstance();
        ArrayList<StyledSegment> segments = new ArrayList<>();

        StyledInput in = handler.createStyledInput(input, null);
        StyledSegment seg;
        while ((seg = in.nextSegment()) != null) {
            if (DEBUG) {
                System.out.println(seg);
            }
            segments.add(seg);
        }

        StringWriter wr = new StringWriter();
        StyledOutput out = RichTextFormatHandlerHelper.createStyledOutput(handler, null, wr);
        for (StyledSegment s : segments) {
            out.consume(s);
        }
        out.flush();

        String result = wr.toString();
        Assertions.assertEquals(expected, result);
    }

    private void testRoundTrip(StyledSegment ... input) throws IOException {
        RichTextFormatHandler handler = RichTextFormatHandler.getInstance();
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
        int sz = input.length;
        Assertions.assertEquals(sz, segments.size());
        for (int i = 0; i < sz; i++) {
            StyledSegment is = input[i];
            StyledSegment rs = segments.get(i);
            Assertions.assertEquals(is.getType(), rs.getType());
            Assertions.assertEquals(is.getText(), rs.getText());
            // empty and null attributes are equivalent for this test
            StyleAttributeMap im = normalize(is.getStyleAttributeMap(null));
            StyleAttributeMap rm = normalize(rs.getStyleAttributeMap(null));
            Assertions.assertEquals(im, rm);
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

    private StyleAttributeMap normalize(StyleAttributeMap a) {
        if (a != null) {
            if (a.isEmpty()) {
                return null;
            }
        }
        return a;
    }

    @Test
    public void save() throws IOException {
        RichTextModel m = new RichTextModel();
        m.setDefaultTabStops(155);
        String s = save(m);
        assertEquals(TestRichTextModel.VERSION + "{#tabs|155.0}{}{!}", s); // verify tab stops are saved

        m.setDefaultTabStops(77);
        s = save(m);
        assertEquals(TestRichTextModel.VERSION + "{#tabs|77.0}{}{!}", s); // verify tab stops are saved
    }

    private static String save(RichTextModel m) throws IOException {
        RichTextFormatHandler h = RichTextFormatHandler.getInstance();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        h.save(m, null, TextPos.ZERO, m.getDocumentEnd(), out);
        byte[] b = out.toByteArray();
        return new String(b, StandardCharsets.UTF_8);
    }

    @Test
    public void load() {
        RichTextModel m = new RichTextModel();
        assertEquals(0.0, m.getDefaultTabStops());

        String input =
            TestRichTextModel.VERSION +
            """
            {#tabs|156.0}{ff}{tc}1{!}
            {0}2{!}
            {0}3{!}
            {!}
            """;
        RichTextFormatHandler h = RichTextFormatHandler.getInstance();
        StyledInput in = h.createStyledInput(input, null);
        m.replace(null, TextPos.ZERO, m.getDocumentEnd(), in);

        assertEquals(5, m.size());
        assertEquals(156.0, m.getDefaultTabStops()); // verify tab stops are loaded
    }
}
