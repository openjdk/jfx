/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.jfx.incubator.scene.control.rich.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.sun.jfx.incubator.scene.control.rich.SegmentStyledInput;
import jfx.incubator.scene.control.rich.TextPos;
import jfx.incubator.scene.control.rich.model.EditableRichTextModel;
import jfx.incubator.scene.control.rich.model.EditableRichTextModelShim;
import jfx.incubator.scene.control.rich.model.RichParagraph;
import jfx.incubator.scene.control.rich.model.StyleAttribute;
import jfx.incubator.scene.control.rich.model.StyleAttrs;
import jfx.incubator.scene.control.rich.model.StyledInput;
import jfx.incubator.scene.control.rich.model.StyledSegment;
import jfx.incubator.scene.control.rich.model.StyledTextModel;

/**
 * Tests EditableRichTextModel.
 */
public class EditableRichTextModelTest {
    private static final StyleAttrs BOLD = StyleAttrs.builder().setBold(true).build();
    private static final StyleAttrs ITALIC = StyleAttrs.builder().setItalic(true).build();

    @Test
    public void insertLineBreak() {
        test(List.of(p()), List.of(p(), p()), (m) -> {
            m.replace(null, TextPos.ZERO, TextPos.ZERO, "\n", true);
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
                m.replace(null, t(0, 0), t(0, 6), "", false);
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
                m.replace(null, t(0, 0), t(0, 2), "", false);
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
                m.replace(null, t(0, 2), t(1, 0), "", false);
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
                m.replace(null, t(2, 0), t(1, 0), "", false);
            }
        );
    }

    private static RichParagraph p() {
        return RichParagraph.builder().build();
    }

    private static RichParagraph p(String text, StyleAttrs a) {
        return RichParagraph.builder().addSegment(text, a).build();
    }

    private static RichParagraph p(StyledSegment... segments) {
        RichParagraph.Builder b = RichParagraph.builder();
        for (StyledSegment s : segments) {
            b.addSegment(s.getText(), s.getStyleAttrs(null));
        }
        return b.build();
    }

    private static StyledSegment s(String text, StyleAttrs a) {
        return StyledSegment.of(text, a);
    }

    private static TextPos t(int index, int offset) {
        return new TextPos(index, offset);
    }

    protected void test(List<RichParagraph> initial, List<RichParagraph> expected, Consumer<StyledTextModel> op) {
        EditableRichTextModel m = new EditableRichTextModel();

        // initial state
        boolean newline = false;
        for (RichParagraph par : initial) {
            if (newline) {
                TextPos p = m.getDocumentEnd();
                m.replace(null, p, p, "\n", false);
            } else {
                newline = true;
            }

            List<StyledSegment> ss = EditableRichTextModelShim.getSegments(par);
            if (ss == null) {
                ss = List.of();
            }
            StyledSegment[] segments = ss.toArray(StyledSegment[]::new);
            StyledInput in = new SegmentStyledInput(segments);
            TextPos p = m.getDocumentEnd();
            m.replace(null, p, p, in, false);
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
        List<StyledSegment> ss = EditableRichTextModelShim.getSegments(p);
        if(ss == null) {
            sb.append("null");
        } else {
            sb.append("\n");
            for(StyledSegment s: ss) {
                sb.append("    {text=\"").append(s.getText());
                sb.append("\", a=").append(s.getStyleAttrs(null)).append("\n");
            }
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

            List<StyledSegment> lsa = EditableRichTextModelShim.getSegments(pa);
            List<StyledSegment> lsb = EditableRichTextModelShim.getSegments(pb);
            if (!((lsa != null) && (lsb != null))) {
                if ((lsa == null) && (lsb == null)) {
                    return null;
                }
                return "segment array mismatch at ix=" + i;
            }
            if (lsa != null) {
                for (int j = 0; j < lsa.size(); j++) {
                    StyledSegment a = lsa.get(j);
                    StyledSegment b = lsb.get(j);
                    if (!eq(a.getText(), b.getText())) {
                        return "segment text[" + j + "] at ix=" + i;
                    }
                    if (!eq(a.getStyleAttrs(null), b.getStyleAttrs(null))) {
                        return "segment attrs[" + j + "] at ix=" + i;
                    }
                }
            }
        }

        return null;
    }
}
