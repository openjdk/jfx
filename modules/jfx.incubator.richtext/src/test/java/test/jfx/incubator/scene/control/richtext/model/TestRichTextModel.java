/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.sun.jfx.incubator.scene.control.richtext.EmbeddedImageHelper;
import com.sun.jfx.incubator.scene.control.richtext.Params;
import com.sun.jfx.incubator.scene.control.richtext.SegmentStyledInput;
import com.sun.jfx.incubator.scene.control.richtext.SegmentStyledOutput;
import jfx.incubator.scene.control.richtext.LineEnding;
import jfx.incubator.scene.control.richtext.Marker;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import test.jfx.incubator.scene.control.richtext.support.RTUtil;

/**
 * Tests RichTextModel.
 */
public class TestRichTextModel {

    /** RichTextModel version segment */
    public static final String VERSION = "{@" + Params.VERSION_3 + "}";
    /** standard document properties */
    public static final String DOC_PROPS = "{#tabs|0.0}";

    private static final StyleAttributeMap BOLD = StyleAttributeMap.builder().setBold(true).build();
    private static final StyleAttributeMap BULLET = StyleAttributeMap.builder().setBullet("x").build();
    private static final StyleAttributeMap ITALIC = StyleAttributeMap.builder().setItalic(true).build();
    private static final StyleAttributeMap RED = StyleAttributeMap.builder().setBackground(Color.RED).build();

    @Test
    public void insertLineBreak() {
        test(List.of(p()), List.of(p(), p()), (m) -> {
            m.replace(null, TextPos.ZERO, TextPos.ZERO, "\n");
        });
    }

    @Test
    public void delete() {
        // delete paragraph (multiple segments), keep its attributes
        test(
            List.of(
                p(s("aa", BOLD), s("bb", ITALIC), s("cc", BOLD))
            ),
            List.of(
                p("", BOLD)
            ),
            (m) -> {
                m.replace(null, t(0, 0), t(0, 6), "");
            }
        );

        // delete paragraph (single segment), keep its attributes
        test(
            List.of(
                p("aa", BOLD)
            ),
            List.of(
                p("", BOLD)
            ),
            (m) -> {
                m.replace(null, t(0, 0), t(0, 2), "");
            }
        );

        // delete newline and merge segments with same attributes
        test(
            List.of(
                p("aa", BOLD),
                p("bb", BOLD)
            ),
            List.of(
                p("aabb", BOLD)
            ),
            (m) -> {
                m.replace(null, t(0, 2), t(1, 0), "");
            }
        );

        // delete empty paragraph, i.e. backspace from pos(2, 0)
        test(
            List.of(
                p("aa", BOLD),
                p(),
                p("bb", BOLD)
            ),
            List.of(
                p("aa", BOLD),
                p("bb", BOLD)
            ),
            (m) -> {
                m.replace(null, t(2, 0), t(1, 0), "");
            }
        );
    }

    private static RichParagraph p() {
        return RichParagraph.builder().build();
    }

    private static RichParagraph p(String text, StyleAttributeMap a) {
        return RichParagraph.builder().addSegment(text, a).build();
    }

    private static RichParagraph p(StyledSegment... segments) {
        RichParagraph.Builder b = RichParagraph.builder();
        for (StyledSegment s : segments) {
            b.addSegment(s.getText(), s.getStyleAttributeMap(null));
        }
        return b.build();
    }

    private static StyledSegment s(String text, StyleAttributeMap a) {
        return StyledSegment.of(text, a);
    }

    private static TextPos t(int index, int offset) {
        return TextPos.ofLeading(index, offset);
    }

    private static TextPos t(int offset) {
        return TextPos.ofLeading(0, offset);
    }

    protected void test(List<RichParagraph> initial, List<RichParagraph> expected, Consumer<StyledTextModel> op) {
        RichTextModel m = new RichTextModel();

        // initial state
        boolean newline = false;
        for (RichParagraph par : initial) {
            if (newline) {
                TextPos p = m.getDocumentEnd();
                m.replace(null, p, p, "\n");
            } else {
                newline = true;
            }

            List<StyledSegment> ss = getSegments(par);
            StyledSegment[] segments = ss.toArray(StyledSegment[]::new);
            StyledInput in = new SegmentStyledInput(segments);
            TextPos p = m.getDocumentEnd();
            m.replace(null, p, p, in);
        }

        // test operation
        op.accept(m);

        // compare
        int sz = m.size();
        ArrayList<RichParagraph> result = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            RichParagraph p = m.getParagraph(i);
            result.add(p);
        }
        String err = checkEquals(expected, result);
        if(err != null) {
            System.err.println("Error: " + err);
            System.err.println("expected=" + dump(expected));
            System.err.println("actual=" + dump(result));
            Assertions.fail(err);
        }
    }

    private static String dump(List<RichParagraph> ps) {
        if(ps == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (RichParagraph p : ps) {
            dump(sb, p);
        }
        return sb.toString();
    }

    private static void dump(StringBuilder sb, RichParagraph p) {
        sb.append("  {pa=").append(p.getParagraphAttributes());
        sb.append(", segments=");
        List<StyledSegment> ss = getSegments(p);
        sb.append("\n");
        for (StyledSegment s : ss) {
            sb.append("    {text=\"").append(s.getText());
            sb.append("\", a=").append(s.getStyleAttributeMap(null)).append("\n");
        }
    }

    private static boolean eq(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private static String checkEquals(List<RichParagraph> expected, List<RichParagraph> actual) {
        int sz = expected.size();
        if (sz != actual.size()) {
            return "expected array size=" + sz + " actual=" + actual.size();
        }

        for (int i = 0; i < sz; i++) {
            RichParagraph pa = expected.get(i);
            RichParagraph pb = actual.get(i);

            if (!eq(pa.getParagraphAttributes(), pb.getParagraphAttributes())) {
                return "paragraph attributes at ix=" + i;
            }

            List<StyledSegment> lsa = getSegments(pa);
            List<StyledSegment> lsb = getSegments(pb);
            for (int j = 0; j < lsa.size(); j++) {
                StyledSegment a = lsa.get(j);
                StyledSegment b = lsb.get(j);
                if (!eq(a.getText(), b.getText())) {
                    return "segment text[" + j + "] at ix=" + i;
                }
                if (!eq(a.getStyleAttributeMap(null), b.getStyleAttributeMap(null))) {
                    return "segment attrs[" + j + "] at ix=" + i;
                }
            }
        }

        return null;
    }

    private RichTextModel createModel(String text) {
        RichTextModel m = new RichTextModel();
        m.replace(null, TextPos.ZERO, TextPos.ZERO, text);
        return m;
    }

    @Test
    public void applyParagraphStyle() {
        RichTextModel m = createModel("1\n2\n3");
        assertEquals(3, m.size());
        TextPos end = m.getDocumentEnd();

        // 3 paragraphs red
        m.applyStyle(TextPos.ZERO, end, RED, false);
        assertEquals(RED, m.getParagraph(0).getParagraphAttributes());
        assertEquals(RED, m.getParagraph(1).getParagraphAttributes());
        assertEquals(RED, m.getParagraph(2).getParagraphAttributes());

        // add bullets to the second and third
        m.applyStyle(TextPos.ofLeading(1, 0), end, BULLET, true);
        StyleAttributeMap combined = BULLET.combine(RED);
        assertEquals(RED, m.getParagraph(0).getParagraphAttributes());
        assertEquals(combined, m.getParagraph(1).getParagraphAttributes());
        assertEquals(combined, m.getParagraph(2).getParagraphAttributes());

        // remove bullet from the second paragraph
        StyleAttributeMap redOff = StyleAttributeMap.of(StyleAttributeMap.BULLET, null);
        StyleAttributeMap result = StyleAttributeMap.builder().setBackground(Color.RED).setBullet(null).build();
        m.applyStyle(TextPos.ofLeading(1, 0), TextPos.ofLeading(1, 1), redOff, true);
        assertEquals(RED, m.getParagraph(0).getParagraphAttributes());
        assertEquals(result, m.getParagraph(1).getParagraphAttributes());
        assertEquals(combined, m.getParagraph(2).getParagraphAttributes());

        // remove all styles
        m.applyStyle(TextPos.ZERO, end, StyleAttributeMap.EMPTY, false);
        assertEquals(StyleAttributeMap.EMPTY, m.getParagraph(0).getParagraphAttributes());
        assertEquals(StyleAttributeMap.EMPTY, m.getParagraph(1).getParagraphAttributes());
        assertEquals(StyleAttributeMap.EMPTY, m.getParagraph(2).getParagraphAttributes());
    }

    @Test
    public void clamp() {
        RichTextModel m = createModel("1\n2222\n");
        assertEquals(TextPos.ofLeading(0, 0), m.clamp(TextPos.ofLeading(0, 0)));
        assertEquals(new TextPos(0, 1, 0, false), m.clamp(TextPos.ofLeading(0, 100)));

        assertEquals(TextPos.ofLeading(1, 0), m.clamp(TextPos.ofLeading(1, 0)));
        assertEquals(TextPos.ofLeading(1, 1), m.clamp(TextPos.ofLeading(1, 1)));
        assertEquals(new TextPos(1, 4, 3, false), m.clamp(TextPos.ofLeading(1, 100)));

        assertEquals(TextPos.ofLeading(2, 0), m.clamp(TextPos.ofLeading(2, 100)));
    }

    private static List<StyledSegment> getSegments(RichParagraph p) {
        int sz = p.getSegmentCount();
        ArrayList<StyledSegment> ss = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            ss.add(p.getSegment(i));
        }
        return ss;
    }

    @Test
    public void lineEnding() {
        RichTextModel m = createModel("1\n2\n3");
        assertEquals(LineEnding.system(), m.getLineEnding());
        assertEquals(3, m.size());
        m.setLineEnding(LineEnding.CR);
        assertEquals(LineEnding.CR, m.getLineEnding());
        assertEquals("1\r2\r3", RTUtil.getText(m));
        m.setLineEnding(LineEnding.CRLF);
        assertEquals(LineEnding.CRLF, m.getLineEnding());
        assertEquals("1\r\n2\r\n3", RTUtil.getText(m));
        m.setLineEnding(LineEnding.LF);
        assertEquals(LineEnding.LF, m.getLineEnding());
        assertEquals("1\n2\n3", RTUtil.getText(m));
    }

    @Test
    public void lineEndingNull() {
        RichTextModel m = createModel("1\n2\n3");
        assertThrows(NullPointerException.class, () -> {
            m.setLineEnding(null);
        });
        assertEquals(LineEnding.system(), m.getLineEnding());
    }

    @Test
    public void prepareParagraph() {
        AtomicInteger counter = new AtomicInteger(0);
        RichTextModel m = new RichTextModel() {
            @Override
            protected RichParagraph.Builder prepareParagraph(int index) {
                RichParagraph.Builder b = super.prepareParagraph(index);
                b.addSegment("yo!");
                counter.incrementAndGet();
                return b;
            }
        };
        m.replace(null, TextPos.ZERO, TextPos.ZERO, "1\n2\n");
        counter.set(0);
        RichParagraph p = m.getParagraph(0);
        assertEquals(1, counter.get());
        String text = p.getPlainText();
        assertEquals("1yo!", text);
    }

    public static String header() {
        return VERSION + DOC_PROPS;
    }

    @Test
    public void requireNonNull() {
        RichTextModel m = createModel("1\n2\n3");
        assertThrows(NullPointerException.class, () -> {
            m.clamp(null);
        });
    }

    @Test
    public void insertSameImageTwice() {
        byte[] bytes = RTUtil.redPng32x32();
        StyleAttributeMap a = StyleAttributeMap.of(StyleAttributeMap.EMBEDDED_IMAGE, EmbeddedImageHelper.create(bytes, 32, 32, 32, 32, true));

        // insert before
        {
            RichTextModel m = createModel("");
            m.replace(null, TextPos.ZERO, TextPos.ZERO, StyledInput.of("1", a));
            m.replace(null, TextPos.ZERO, TextPos.ZERO, StyledInput.of("2", a));

            RichParagraph p = m.getParagraph(0);
            assertEquals(2, p.getSegmentCount());
            assertEquals(a, p.getSegment(0).getStyleAttributeMap(null));
            assertEquals(a, p.getSegment(1).getStyleAttributeMap(null));
        }

        // insert after
        {
            RichTextModel m = createModel("");
            m.replace(null, TextPos.ZERO, TextPos.ZERO, StyledInput.of("1", a));
            TextPos pos = TextPos.ofLeading(0, 1);
            m.replace(null, pos, pos, StyledInput.of("2", a));

            RichParagraph p = m.getParagraph(0);
            assertEquals(2, p.getSegmentCount());
            assertEquals(a, p.getSegment(0).getStyleAttributeMap(null));
            assertEquals(a, p.getSegment(1).getStyleAttributeMap(null));
        }
    }

    @Test
    public void applyStyle() {
        RichTextModel m = createModel("ABCDEF");
        m.applyStyle(t(4), t(6), BOLD, true); // EF
        m.applyStyle(t(0), t(2), BOLD, true); // AB

        assertEquals("ABCDEF", m.getPlainText(0));
        assertEquals(BOLD, m.getStyleAttributeMap(null, t(1), true));
        assertEquals(StyleAttributeMap.EMPTY, m.getStyleAttributeMap(null, t(0, 3), true));
        assertEquals(BOLD, m.getStyleAttributeMap(null, t(5), true));
    }

    @Test
    public void applyStyleAllCases() throws Exception {

        // RichTextModel.whichCase()
        // paragraph:   start [=============] end
        //      case:
        //             0a:                  |-
        //             0b:                      |-
        //              1:  -------------------->
        //              2:    |----------------->
        //              3:    |-------------|
        //              4:    |--------|
        //              5:        |------------->
        //              6:        |---------|
        //              7:        |----|
        //              8:  -----------|
        //              9:  ----------------|
        //
        // each case starts with the model set to "a\nbcd\ne" -
        //   a
        //   bcd
        //   e
        // the expected string: uppercase text when it's bold, '-' for newline

        assertApply("0a", t(1, 6), t(99, 99), "a-bcd-E");
        assertApply("0b", t(98, 98), t(99, 99), "a-bcd-e");
        assertApply("1", TextPos.ZERO, t(99, 99), "A-BCD-E");
        assertApply("2", t(1, 0), t(99, 99), "a-BCD-E");
        assertApply("3", t(1, 0), t(1, 6), "a-BCD-e");
        assertApply("4", t(1, 0), t(1, 2), "a-BCd-e");
        assertApply("5", t(1, 2), t(99, 99), "a-bcD-E");
        assertApply("6", t(1, 2), t(1, 6), "a-bcD-e");
        assertApply("7", t(1, 1), t(1, 2), "a-bCd-e");
        assertApply("8", TextPos.ZERO, t(1, 2), "A-BCd-e");
        assertApply("8", TextPos.ZERO, t(1, 6), "A-BCD-e");
    }

    private void assertApply(String which, TextPos start, TextPos end, String expected) throws Exception {
        RichTextModel m = createModel("a\nbcd\ne");
        m.applyStyle(start, end, BOLD, true);
        SegmentStyledOutput out = new SegmentStyledOutput(8);
        m.export(TextPos.ZERO, m.getDocumentEnd(), out);
        StyledSegment[] segments = out.getSegments();
        StringBuilder sb = new StringBuilder();
        for (StyledSegment seg : segments) {
            switch(seg.getType()) {
            case DOCUMENT_PROPERTIES:
            case VERSION:
            case PARAGRAPH_ATTRIBUTES:
                // ignore
                break;
            case LINE_BREAK:
                sb.append("-");
                break;
            case TEXT:
                StyleAttributeMap a = seg.getStyleAttributeMap(null);
                String txt = seg.getText();
                if(BOLD.equals(a)) {
                    txt = txt.toUpperCase(Locale.ROOT);
                }
                sb.append(txt);
                break;
            case REGION:
            case INLINE_NODE:
            default:
                fail();
            }
        }
        assertEquals(expected, sb.toString(), "case=" + which);
    }

    @Test
    public void explodingListener() {
        String pattern = "BoOm!";
        Marker marker = null;
        // see OutputRedirect
        PrintStream stderr = System.err;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        System.setErr(new PrintStream(os, true, StandardCharsets.UTF_8));
        try {
            RichTextModel m = createModel("1");
            m.addListener((ch) -> {
                throw new RuntimeException(pattern);
            });
            marker = m.getMarker(t(0, 1));
            m.replace(null, m.getDocumentEnd(), m.getDocumentEnd(), "23");
        } finally {
            System.setErr(stderr);
            String s = new String(os.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(s.contains(pattern));
        }
        assertNotNull(marker);
        assertEquals(t(0, 3), marker.getTextPos());
    }
}
