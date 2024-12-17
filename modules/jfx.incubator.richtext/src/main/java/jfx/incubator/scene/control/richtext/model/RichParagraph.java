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

package jfx.incubator.scene.control.richtext.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import com.sun.jfx.incubator.scene.control.richtext.RichParagraphHelper;
import com.sun.jfx.incubator.scene.control.richtext.TextCell;
import jfx.incubator.scene.control.richtext.StyleResolver;

/**
 * Represents a single immutable paragraph within the {@code StyledModel}.
 * A single paragraph may contain either:
 * <ul>
 * <li>A number of {@code StyledSegments} such as styled text or {@code Supplier}s of embedded {@code Node}s
 * <li>A supplier of a single {@code Region} which fills the entire paragraph
 * </ul>
 *
 * @since 24
 */
public abstract class RichParagraph {
    static { initAccessor(); }

    /**
     * The constructor.
     */
    public RichParagraph() {
    }

    /**
     * Creates a paragraph consisting of a single Rectangle.
     * The paragraph will typically assume its Rectangle preferred size, or,
     * when the text wrap mode is on, might get resized to fit the available width.
     * <p>
     * The supplied generator must not cache or keep reference to the created Node,
     * but the created Node can keep a reference to the model or a property therein.
     * <p>
     * For example, a bidirectional binding between an inline control and some property in the model
     * would synchronize the model with all the views that use it.
     *
     * @param paragraphGenerator the content generator
     * @return the RichParagraph instance
     */
    public static RichParagraph of(Supplier<Region> paragraphGenerator) {
        return new RichParagraph() {
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

            @Override
            List<StyledSegment> getSegments() {
                return null;
            }
        };
    }

    /**
     * Returns the generator for this paragraph {@code Region} representation.
     * This method returns a non-null value when the paragraph is represented by a single {@code Region}.
     *
     * @return the generator, or null
     */
    public Supplier<Region> getParagraphRegion() {
        return null;
    }

    /**
     * Returns the plain text of this paragraph, or null.
     * @return the plain text
     */
    public abstract String getPlainText();

    // this method could be made public, as long as the returned list is made immutable
    abstract List<StyledSegment> getSegments();

    /**
     * Returns the paragraph attributes.
     * @return the paragraph attributes, can be null
     */
    public StyleAttributeMap getParagraphAttributes() {
        return null;
    }

    List<Consumer<TextCell>> getHighlights() {
        return null;
    }

    // for use by StyledTextModel
    void export(int start, int end, StyledOutput out) throws IOException {
        List<StyledSegment> segments = getSegments();
        if (segments == null) {
            out.consume(StyledSegment.of(""));
        } else {
            int off = 0;
            int sz = segments.size();
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

    // for use by SimpleReadOnlyStyledModel
    StyleAttributeMap getStyleAttributeMap(StyleResolver resolver, int offset) {
        int off = 0;
        List<StyledSegment> segments = getSegments();
        if (segments != null) {
            int sz = segments.size();
            for (int i = 0; i < sz; i++) {
                StyledSegment seg = segments.get(i);
                int len = seg.getTextLength();
                if (offset < (off + len) || (i == sz - 1)) {
                    return seg.getStyleAttributeMap(resolver);
                }
                off += len;
            }
        }
        return StyleAttributeMap.EMPTY;
    }

    private static void initAccessor() {
        RichParagraphHelper.setAccessor(new RichParagraphHelper.Accessor() {
            @Override
            public List<StyledSegment> getSegments(RichParagraph p) {
                return p.getSegments();
            }

            @Override
            public List<Consumer<TextCell>> getHighlights(RichParagraph p) {
                return p.getHighlights();
            }
        });
    }

    /**
     * Creates an instance of the {@code Builder} class.
     * @return the new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Utility class for building immutable {@code RichParagraph}s.
     */
    public static class Builder {
        private ArrayList<StyledSegment> segments;
        private ArrayList<Consumer<TextCell>> highlights;
        private StyleAttributeMap paragraphAttributes;

        Builder() {
        }

        /**
         * Adds a wavy underline (typically used as a spell checker indicator) with the given color.
         * @param start the start offset
         * @param length the end offset
         * @param color the background color
         * @return this {@code Builder} instance
         */
        public Builder addWavyUnderline(int start, int length, Color color) {
            int end = start + length;
            highlights().add((cell) -> {
                cell.addSquiggly(start, end, color);
            });
            return this;
        }

        private List<Consumer<TextCell>> highlights() {
            if (highlights == null) {
                highlights = new ArrayList<>(4);
            }
            return highlights;
        }

        /**
         * Adds a text segment with no styling (i.e. using default style).
         * This convenience method is equivalent to calling {@code addSegment(text, StyleAttributeMap.EMPTY);}
         *
         * @param text the text to append, must not contain {@code \n}, cannot be null
         * @return this {@code Builder} instance
         */
        public Builder addSegment(String text) {
            StyledSegment seg = StyledSegment.of(text);
            segments().add(seg);
            return this;
        }

        /**
         * Appends a text segment styled with the stylesheet style names.
         *
         * @param text non-null text string
         * @param css array of style names, cannot be null
         * @return this {@code Builder} instance
         */
        public Builder addWithStyleNames(String text, String ... css) {
            Objects.nonNull(text);
            Objects.nonNull(css);
            StyleAttributeMap a = StyleAttributeMap.fromStyles(null, css);
            addSegment(text, a);
            return this;
        }

        /**
         * Appends a text segment styled with both the inline style and the stylesheet style names.
         *
         * @param text non-null text string
         * @param style direct style (such as {@code -fx-fill:red;}), or null
         * @param css array of style names
         * @return this {@code Builder} instance
         */
        public Builder addWithInlineAndStyleNames(String text, String style, String ... css) {
            Objects.nonNull(text);
            StyleAttributeMap a = StyleAttributeMap.fromStyles(style, css);
            addSegment(text, a);
            return this;
        }

        /**
         * Appends a text segment styled with the stylesheet style names.
         *
         * @param text non-null text string
         * @param style the inline style (example {@code "-fx-fill:red;"}), or null
         * @return this {@code Builder} instance
         */
        public Builder addWithInlineStyle(String text, String style) {
            Objects.nonNull(text);
            StyleAttributeMap a = StyleAttributeMap.fromStyles(style);
            addSegment(text, a);
            return this;
        }

        /**
         * Adds a styled text segment.
         *
         * @param text the text to append, must not contain {@code \n}, cannot be null
         * @param attrs the styled attributes, cannot be null
         * @return this {@code Builder} instance
         */
        public Builder addSegment(String text, StyleAttributeMap attrs) {
            Objects.nonNull(text);
            Objects.nonNull(attrs);
            StyledSegment seg = StyledSegment.of(text, attrs);
            segments().add(seg);
            return this;
        }

        /**
         * Adds a styled text segment.
         * @param text the source non-null string
         * @param start the start offset of the input string
         * @param end the end offset of the input string
         * @param attrs the styled attributes
         * @return this {@code Builder} instance
         */
        public Builder addSegment(String text, int start, int end, StyleAttributeMap attrs) {
            Objects.nonNull(text);
            String s = text.substring(start, end);
            addSegment(s, attrs);
            return this;
        }

        /**
         * Adds a color background highlight.
         * Use translucent colors to enable multiple highlights in the same region of text.
         * @param start the start offset
         * @param length the end offset
         * @param color the background color
         * @return this {@code Builder} instance
         */
        public Builder addHighlight(int start, int length, Color color) {
            int end = start + length;
            highlights().add((cell) -> {
                cell.addHighlight(start, end, color);
            });
            return this;
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
         * @return this {@code Builder} instance
         */
        public Builder addInlineNode(Supplier<Node> generator) {
            StyledSegment seg = StyledSegment.ofInlineNode(generator);
            segments().add(seg);
            return this;
        }

        private List<StyledSegment> segments() {
            if (segments == null) {
                segments = new ArrayList<>(8);
            }
            return segments;
        }

        /**
         * Sets the paragraph attributes.
         * @param a the paragraph attributes
         * @return this {@code Builder} instance
         */
        public Builder setParagraphAttributes(StyleAttributeMap a) {
            paragraphAttributes = a;
            return this;
        }

        /**
         * Creates an instance of immutable {@code RichParagraph} from information
         * in this {@code Builder}.
         * @return the new paragraph instance
         */
        public RichParagraph build() {
            return new RichParagraph() {
                @Override
                public StyleAttributeMap getParagraphAttributes() {
                    return paragraphAttributes;
                }

                @Override
                List<StyledSegment> getSegments() {
                    return segments;
                }

                @Override
                public String getPlainText() {
                    if (segments == null) {
                        return "";
                    }

                    StringBuilder sb = new StringBuilder();
                    for (StyledSegment seg : segments) {
                        sb.append(seg.getText());
                    }
                    return sb.toString();
                }

                @Override
                List<Consumer<TextCell>> getHighlights() {
                    return highlights;
                }
            };
        }
    }
}
