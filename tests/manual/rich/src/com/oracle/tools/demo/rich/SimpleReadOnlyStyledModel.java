/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxEditor

package com.oracle.tools.demo.rich;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.rich.TextCell;
import javafx.scene.control.rich.TextPos;
import javafx.scene.control.rich.model.RichParagraph;
import javafx.scene.control.rich.model.RtfFormatHandler;
import javafx.scene.control.rich.model.StyleAttrs;
import javafx.scene.control.rich.model.StyledOutput;
import javafx.scene.control.rich.model.StyledSegment;
import javafx.scene.control.rich.model.StyledTextModelReadOnlyBase;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * A simple, read-only, in-memory, styled text model.
 */
public class SimpleReadOnlyStyledModel extends StyledTextModelReadOnlyBase {
    private final ArrayList<RichParagraph> paragraphs = new ArrayList<>();

    public SimpleReadOnlyStyledModel() {
        registerDataFormatHandler(new RtfFormatHandler(), 1000);
    }

    public static SimpleReadOnlyStyledModel from(String text) {
        SimpleReadOnlyStyledModel m = new SimpleReadOnlyStyledModel();
        BufferedReader rd = new BufferedReader(new StringReader(text));
        try {
            String s;
            while ((s = rd.readLine()) != null) {
                m.addSegment(s);
                m.nl();
            }
        } catch (Exception ignore) {
        } finally {
            try {
                rd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return m;
    }

    @Override
    public int size() {
        return paragraphs.size();
    }

    @Override
    public String getPlainText(int index) {
        return paragraphs.get(index).getPlainText();
    }

    @Override
    public RichParagraph getParagraph(int index) {
        return paragraphs.get(index);
    }

    public SimpleReadOnlyStyledModel addSegment(String text) {
        return addSegment(text, null);
    }

    public SimpleReadOnlyStyledModel addSegment(String text, String style, String... css) {
        RichParagraph p = lastSegmentStyledTextParagraph();
        p.addSegment(text, style, css);
        return this;
    }

    public SimpleReadOnlyStyledModel highlight(int start, int length, Color c) {
        int end = start + length;
        RichParagraph p = lastSegmentStyledTextParagraph();
        p.addHighlight((cell) -> cell.addHighlight(start, end, c));
        return this;
    }

    public SimpleReadOnlyStyledModel squiggly(int start, int length, Color c) {
        int end = start + length;
        RichParagraph p = lastSegmentStyledTextParagraph();
        p.addHighlight((cell) -> cell.addSquiggly(start, end, c));
        return this;
    }

    protected RichParagraph lastSegmentStyledTextParagraph() {
        int sz = paragraphs.size();
        if (sz == 0) {
            RichParagraph p = new RichParagraph();
            paragraphs.add(p);
            return p;
        }
        return paragraphs.get(sz - 1);
    }

    /** adds a paragraph containing an image */
    public SimpleReadOnlyStyledModel addImage(InputStream in) {
        Image im = new Image(in);
        RichParagraph p = RichParagraph.of(() -> {
            return new ImageCellPane(im);
        });
        paragraphs.add(p);
        return this;
    }

    public SimpleReadOnlyStyledModel addParagraph(Supplier<Region> generator) {
        RichParagraph p = RichParagraph.of(() -> {
            return generator.get();
        });
        paragraphs.add(p);
        return this;
    }

    /** adds inline node segment */
    public SimpleReadOnlyStyledModel addNodeSegment(Supplier<Node> generator) {
        RichParagraph p = lastSegmentStyledTextParagraph();
        p.addInlineNode(generator);
        return this;
    }

    public SimpleReadOnlyStyledModel nl() {
        return nl(1);
    }

    public SimpleReadOnlyStyledModel nl(int count) {
        for (int i = 0; i < count; i++) {
            int ix = paragraphs.size();
            paragraphs.add(new RichParagraph());
        }
        return this;
    }

    @Override
    public StyleAttrs getStyleInfo(TextPos pos) {
        // TODO use segments
        return StyleAttrs.EMPTY;
    }

    @Override
    protected void exportParagraph(int index, int start, int end, StyledOutput out) throws IOException {
        RichParagraph p = paragraphs.get(index);
        p.export(start, end, out);
//        if (par instanceof SParagraph p) {
//            p.export(start, end, out);
//        } else if (par instanceof SimpleStyledImageParagraph p) {
//            StyledSegment s = StyledSegment.nodeParagraph(() -> p.getRichParagraph(index).getContent());
//            out.append(s);
//        } else if (par instanceof NodeStyledParagraph p) {
//            StyledSegment s = StyledSegment.nodeParagraph(() -> p.getRichParagraph(index).getContent());
//            out.append(s);
//        }
    }

    /** Styled Paragraph Based on SSegments */
    @Deprecated
    protected static class SParagraph {
        private ArrayList<SSegment> segments;
        private ArrayList<Consumer<TextCell>> highlighters;

//        public RichParagraph getRichParagraph(int ix) {
//            RichParagraph p = new RichParagraph();
//
//            if (highlighters != null) {
//                for (Consumer<TextCell> hl : highlighters) {
//                    hl.accept(p);
//                }
//            }
//
//            if (segments == null) {
//                // avoid zero height
//                p.addSegment("");
//            } else {
//                for (SSegment s : segments) {
//                    // TODO Segment.createNode()
//                    if (s instanceof TextSSegment t) {
//                        p.addSegment(t.text, t.direct, t.css);
//                    } else if (s instanceof NodeSSegment n) {
//                        p.addInlineNode(n.generator);
//                    }
//                }
//            }
//            return p;
//        }

//        public void export(int start, int end, StyledOutput out) throws IOException {
//            if (segments == null) {
//                out.append(StyledSegment.of(""));
//            } else {
//                int off = 0;
//                int ct = size();
//                for (int i = 0; i < ct; i++) {
//                    if (off >= end) {
//                        return;
//                    }
//
//                    SSegment seg = segments.get(i);
//                    String text = seg.getText();
//                    int len = (text == null ? 0 : text.length());
//                    if (start <= (off + len)) {
//                        int ix0 = Math.max(0, start - off);
//                        int ix1 = Math.min(len, end - off);
//                        StyledSegment ss = seg.createStyledSegment(ix0, ix1);
//                        out.append(ss);
//                    }
//                    off += len;
//                }
//            }
//        }

//        @Override
//        public String getText() {
//            if (segments == null) {
//                return null;
//            }
//
//            StringBuilder sb = new StringBuilder(64);
//            for (SSegment s : segments) {
//                sb.append(s.getText());
//            }
//            return sb.toString();
//        }

//        protected List<SSegment> segments() {
//            if (segments == null) {
//                segments = new ArrayList<>();
//            }
//            return segments;
//        }
//
//        protected int size() {
//            return segments == null ? 0 : segments.size();
//        }

//        public void addSegment(String text, String style, String[] css) {
//            // TODO check for newlines/formfeed chars
//            segments().add(new TextSSegment(text, style, css));
//        }
//
//        public void addSegment(Supplier<Node> generator) {
//            segments().add(new NodeSSegment(generator));
//        }

//        public void addHighlight(Consumer<TextCell> highlighter) {
//            if (highlighters == null) {
//                highlighters = new ArrayList<>();
//            }
//            highlighters.add(highlighter);
//        }
    }

    /** base class */
    protected static abstract class SSegment {
        public abstract String getText();

        protected abstract StyledSegment createStyledSegment(int start, int end);
    }

    /** text segment */
    protected static class TextSSegment extends SSegment {
        public final String text;
        public final String direct;
        public final String[] css;
        private final StyleAttrs style;

        public TextSSegment(String text, String direct, String[] css) {
            this.text = text;
            this.direct = direct;
            this.css = css;
            this.style = StyleAttrs.fromCss(direct, css);
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        protected StyledSegment createStyledSegment(int start, int end) {
            int len = text.length();
            String s;
            if ((start <= 0) && (end >= len)) {
                s = text;
            } else {
                s = text.substring(Math.max(0, start), Math.min(end, len));
            }

            return StyledSegment.of(s, style);
        }
    }

    /** inline node segment */
//    protected static class NodeSSegment extends SSegment {
//        public final Supplier<Node> generator;
//
//        public NodeSSegment(Supplier<Node> generator) {
//            this.generator = generator;
//        }
//
//        @Override
//        public String getText() {
//            // must be one character
//            return " ";
//        }
//
//        @Override
//        protected StyledSegment createStyledSegment(int start, int end) {
//            return StyledSegment.inlineNode(generator);
//        }
//    }
}
