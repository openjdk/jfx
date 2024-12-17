/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.richtext.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import com.sun.jfx.incubator.scene.control.richtext.ImageCellPane;
import com.sun.jfx.incubator.scene.control.richtext.TextCell;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * A simple, view-only, in-memory, styled text model.
 *
 * @since 24
 */
public class SimpleViewOnlyStyledModel extends StyledTextModelViewOnlyBase {
    private final ArrayList<Paragraph> paragraphs = new ArrayList<>();

    /**
     * The constructor.
     */
    public SimpleViewOnlyStyledModel() {
    }

    /**
     * Creates the model from the supplied text string by breaking it down into individual text segments,
     * by splitting on newline characters.
     *
     * @param text the input multi-line text
     * @return the new instance
     * @throws IOException if an I/O error occurs
     */
    public static SimpleViewOnlyStyledModel of(String text) throws IOException {
        SimpleViewOnlyStyledModel m = new SimpleViewOnlyStyledModel();
        BufferedReader rd = new BufferedReader(new StringReader(text));
        String s;
        while ((s = rd.readLine()) != null) {
            m.addSegment(s);
            m.nl();
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
        return paragraphs.get(index).toRichParagraph();
    }

    /**
     * Appends a text segment to the last paragraph.
     * The caller must ensure that the {@code text} does not contain newline symbols, as the behavior
     * is undefined in this case.
     * <p>
     * This convenience method is equivalent to calling {@code addSegment(text, StyleAttributeMap.EMPTY);}
     *
     * @param text the text to append, must not contain newline symbols, cannot be null
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel addSegment(String text) {
        Objects.requireNonNull(text);
        return addSegment(text, StyleAttributeMap.EMPTY);
    }

    /**
     * Appends a text segment styled with both the inline style and the stylesheet style names.
     * The {@code text} cannot contain newline ({@code \n}) symbols.
     *
     * @param text the text to append, must not contain {@code \n}, cannot be null
     * @param style the inline style (example {@code "-fx-fill:red;"})
     * @param css the stylesheet style names
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel addWithInlineAndStyleNames(String text, String style, String... css) {
        Objects.requireNonNull(text);
        StyleAttributeMap a = StyleAttributeMap.fromStyles(style, css);
        Paragraph p = lastParagraph();
        p.addSegment(text, a);
        return this;
    }

    /**
     * Appends a text segment styled with the stylesheet style names.
     * The {@code text} cannot contain newline ({@code \n}) symbols.
     *
     * @param text the text to append, must not contain {@code \n}, cannot be null
     * @param css the stylesheet style names
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel addWithStyleNames(String text, String... css) {
        Objects.requireNonNull(text);
        StyleAttributeMap a = StyleAttributeMap.fromStyles(null, css);
        Paragraph p = lastParagraph();
        p.addSegment(text, a);
        return this;
    }

    /**
     * Appends a text segment styled with the inline style.
     * The {@code text} cannot contain newline ({@code \n}) symbols.
     *
     * @param text the text to append, must not contain {@code \n}, cannot be null
     * @param style the inline style (example {@code "-fx-fill:red;"}), or null
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel addWithInlineStyle(String text, String style) {
        Objects.requireNonNull(text);
        StyleAttributeMap a = StyleAttributeMap.fromInlineStyle(style);
        Paragraph p = lastParagraph();
        p.addSegment(text, a);
        return this;
    }

    /**
     * Appends a text segment styled with the specified style attributes.
     * @param text the text to append, must not contain control symbols other than
     * TAB.
     *
     * @param a the style attributes
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel addSegment(String text, StyleAttributeMap a) {
        // TODO split into paragraphs if \n is found, or check for \n ?
        Objects.requireNonNull(a);
        Paragraph p = lastParagraph();
        p.addSegment(text, a);
        return this;
    }

    /**
     * Adds a highlight of the given color to the specified range within the last paragraph.
     *
     * @param start the start offset
     * @param length the length of the highlight
     * @param c the highlight color
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel highlight(int start, int length, Color c) {
        Paragraph p = lastParagraph();
        p.addHighlight(start, length, c);
        return this;
    }

    /**
     * Adds a wavy underline (typically used as a spell checker indicator) to the specified range within the last paragraph.
     *
     * @param start the start offset
     * @param length the length of the highlight
     * @param c the highlight color
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel addWavyUnderline(int start, int length, Color c) {
        Paragraph p = lastParagraph();
        p.addSquiggly(start, length, c);
        return this;
    }

    private Paragraph lastParagraph() {
        int sz = paragraphs.size();
        if (sz == 0) {
            Paragraph p = new Paragraph();
            paragraphs.add(p);
            return p;
        }
        return paragraphs.get(sz - 1);
    }

    /**
     * Adds a paragraph containing an image.  The image will be reduced in size as necessary to fit into the available
     * area if {@code wrapText} property is set.
     * This method does not close the input stream.
     *
     * @param in the input stream providing the image.
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel addImage(InputStream in) {
        Image im = new Image(in);
        Paragraph p = Paragraph.of(() -> {
            return new ImageCellPane(im);
        });
        paragraphs.add(p);
        return this;
    }

    /**
     * Adds a paragraph containing a {@code Region}.
     * <p>
     * The supplied generator must not cache or keep reference to the created {@code Region},
     * but the created {@code Region} can keep a reference to the model or a property therein.
     * <p>
     * For example, a bidirectional binding between an inline control and some property in the model
     * would synchronize the model with all the views that use it.
     *
     * @param generator the supplier of the paragraph content
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel addParagraph(Supplier<Region> generator) {
        Paragraph p = Paragraph.of(() -> {
            return generator.get();
        });
        paragraphs.add(p);
        return this;
    }

    /**
     * Adds an inline Node to the last paragraph.
     * <p>
     * The supplied generator must not cache or keep reference to the created {@code Node},
     * but the created {@code Node} can keep a reference to the model or a property therein.
     * <p>
     * For example, a bidirectional binding between an inline control and some property in the model
     * would synchronize the model with all the views that use it.
     *
     * @param generator the supplier of the embedded Node
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel addNodeSegment(Supplier<Node> generator) {
        Paragraph p = lastParagraph();
        p.addInlineNode(generator);
        return this;
    }

    /**
     * Adds a new paragraph (as if inserting a newline symbol into the text).
     * This convenience method invokes {@link #nl(int)} with a value of 1.
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel nl() {
        return nl(1);
    }

    /**
     * Adds {@code n} new paragraphs (as if inserting a newline symbol into the text {@code n} times).
     *
     * @param count the number of paragraphs to append
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel nl(int count) {
        for (int i = 0; i < count; i++) {
            int ix = paragraphs.size();
            paragraphs.add(new Paragraph());
        }
        return this;
    }

    @Override
    public StyleAttributeMap getStyleAttributeMap(StyleResolver r, TextPos pos) {
        int index = pos.index();
        if (index < paragraphs.size()) {
            int off = pos.offset();
            Paragraph par = paragraphs.get(index);
            StyleAttributeMap pa = par.getParagraphAttributes();
            StyleAttributeMap a = par.getStyleAttrs(r, off);
            if (pa == null) {
                return a;
            } else {
                return pa.combine(a);
            }
        }
        return StyleAttributeMap.EMPTY;
    }

    /**
     * Sets the last paragraph's attributes.
     *
     * @param a the paragraph attributes
     * @return this model instance
     */
    public SimpleViewOnlyStyledModel setParagraphAttributes(StyleAttributeMap a) {
        Paragraph p = lastParagraph();
        p.setParagraphAttributes(a);
        return this;
    }

    /** Encapsulates a paragraph */
    static class Paragraph {
        private ArrayList<StyledSegment> segments;
        private ArrayList<Consumer<TextCell>> highlights;
        private StyleAttributeMap paragraphAttributes;

        public Paragraph() {
        }

        public static Paragraph of(Supplier<Region> paragraphGenerator) {
            return new Paragraph() {
                @Override
                public final Supplier<Region> getParagraphRegion() {
                    return paragraphGenerator;
                }

                @Override
                public final String getPlainText() {
                    return "";
                }

                @Override
                public void export(int start, int end, StyledOutput out) throws IOException {
                    StyledSegment seg = StyledSegment.ofRegion(paragraphGenerator);
                    out.consume(seg);
                }
            };
        }

        public Supplier<Region> getParagraphRegion() {
            return null;
        }

        String getPlainText() {
            if (segments == null) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (StyledSegment seg : segments) {
                sb.append(seg.getText());
            }
            return sb.toString();
        }

        /**
         * Adds a text segment with no styling (i.e. using default style).
         *
         * @param text segment text
         */
        void addSegment(String text) {
            StyledSegment seg = StyledSegment.of(text);
            segments().add(seg);
        }

        /**
         * Adds a styled text segment.
         * @param text the non-null text string
         * @param attrs the styled attributes
         */
        void addSegment(String text, StyleAttributeMap attrs) {
            StyledSegment seg = StyledSegment.of(text, attrs);
            segments().add(seg);
        }

        /**
         * Adds a styled text segment.
         * @param text the source non-null string
         * @param start the start offset of the input string
         * @param end the end offset of the input string
         * @param attrs the styled attributes
         */
        void addSegment(String text, int start, int end, StyleAttributeMap attrs) {
            String s = text.substring(start, end);
            addSegment(s, attrs);
        }

        /**
         * Adds a color background highlight.
         * Use translucent colors to enable multiple highlights in the same region of text.
         * @param start the start offset
         * @param length the end offset
         * @param color the background color
         */
        void addHighlight(int start, int length, Color color) {
            int end = start + length;
            highlights().add((cell) -> {
                cell.addHighlight(start, end, color);
            });
        }

        /**
         * Adds a squiggly line (as seen in a spell checker) with the given color.
         * @param start the start offset
         * @param length the end offset
         * @param color the background color
         */
        void addSquiggly(int start, int length, Color color) {
            int end = start + length;
            highlights().add((cell) -> {
                cell.addSquiggly(start, end, color);
            });
        }

        private List<Consumer<TextCell>> highlights() {
            if (highlights == null) {
                highlights = new ArrayList<>(4);
            }
            return highlights;
        }

        /**
         * Adds an inline node.
         * <p>
         * The supplied generator must not cache or keep reference to the created Node,
         * but the created Node can keep a reference to the model or some property therein.
         * <p>
         * For example, a bidirectional binding between an inline control and some property in the model
         * would synchronize the model with all the views that use it.
         * @param generator the generator that provides the actual {@code Node}
         */
        void addInlineNode(Supplier<Node> generator) {
            StyledSegment seg = StyledSegment.ofInlineNode(generator);
            segments().add(seg);
        }

        private List<StyledSegment> segments() {
            if (segments == null) {
                segments = new ArrayList<>(8);
            }
            return segments;
        }

        private List<StyledSegment> getSegments() {
            return segments;
        }

        private int size() {
            return segments == null ? 0 : segments.size();
        }

        // for use by StyledTextModel
        void export(int start, int end, StyledOutput out) throws IOException {
            if (segments == null) {
                out.consume(StyledSegment.of(""));
            } else {
                int off = 0;
                int sz = size();
                for (int i = 0; i < sz; i++) {
                    StyledSegment seg = segments.get(i);
                    String text = seg.getText();
                    int len = (text == null ? 0 : text.length());
                    if (start <= (off + len)) {
                        int ix0 = Math.max(0, start - off);
                        int ix1 = Math.min(len, end - off);
                        if (ix1 > ix0) {
                            StyledSegment ss = seg.subSegment(ix0, ix1);
                            out.consume(ss);
                        }
                    }
                    off += len;
                    if (off >= end) {
                        return;
                    }
                }
            }
        }

        /**
         * Sets the paragraph attributes.
         * @param a the paragraph attributes
         */
        void setParagraphAttributes(StyleAttributeMap a) {
            paragraphAttributes = a;
        }

        /**
         * Returns the paragraph attributes.
         * @return the paragraph attributes, can be null
         */
        StyleAttributeMap getParagraphAttributes() {
            return paragraphAttributes;
        }

        // for use by SimpleReadOnlyStyledModel
        StyleAttributeMap getStyleAttrs(StyleResolver resolver, int offset) {
            int off = 0;
            int ct = size();
            for (int i = 0; i < ct; i++) {
                StyledSegment seg = segments.get(i);
                int len = seg.getTextLength();
                if (offset < (off + len) || (i == ct - 1)) {
                    return seg.getStyleAttributeMap(resolver);
                }
                off += len;
            }
            return StyleAttributeMap.EMPTY;
        }

        public RichParagraph toRichParagraph() {
            return new RichParagraph() {
                @Override
                public final String getPlainText() {
                    return Paragraph.this.getPlainText();
                }

                @Override
                public final StyleAttributeMap getParagraphAttributes() {
                    return paragraphAttributes;
                }

                @Override
                final List<StyledSegment> getSegments() {
                    return Paragraph.this.getSegments();
                }

                @Override
                public final Supplier<Region> getParagraphRegion() {
                    return Paragraph.this.getParagraphRegion();
                }

                @Override
                final List<Consumer<TextCell>> getHighlights() {
                    return highlights;
                }
            };
        }
    }
}
