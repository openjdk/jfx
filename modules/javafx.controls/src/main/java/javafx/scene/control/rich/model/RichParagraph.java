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

package javafx.scene.control.rich.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.rich.TextCell;
import javafx.scene.layout.Region;

/**
 * Represents a paragraph with rich text inside the StyledModel.
 */
public class RichParagraph {
    private ArrayList<StyledSegment> segments;
    private ArrayList<Consumer<TextCell>> highlights;

    public RichParagraph() {
    }
    
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
                StyledSegment seg = StyledSegment.nodeParagraph(paragraphGenerator);
                out.append(seg);
            }
        };
    }

    public Supplier<Region> getParagraphRegion() {
        return null;
    }

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

    /**
     * Adds a text segment with no styling (i.e. using default style).
     *
     * @param text segment text
     */
    public void addSegment(String text) {
        StyledSegment seg = StyledSegment.of(text);
        segments().add(seg);
    }

    /**
     * Adds a styled text segment.
     *
     * @param text non-null text string
     * @param style direct style (such as {@code -fx-fill:red;}), or null
     * @param css array of style names, or null
     */
    public void addSegment(String text, String style, String[] css) {
        StyleAttrs a = StyleAttrs.fromCss(style, css);
        addSegment(text, a);
    }

    /**
     * Adds a styled text segment.
     * @param text the non-null text string
     * @param attrs the styled attributes
     */
    public void addSegment(String text, StyleAttrs attrs) {
        StyledSegment seg = StyledSegment.of(text, attrs);
        segments().add(seg);
    }

    public void addHighlight(Consumer<TextCell> highlight) {
        if (highlights == null) {
            highlights = new ArrayList<>();
        }
        highlights.add(highlight);
    }

    /**
     * Adds an inline node.
     * @param generator the generator that provides the actual {@code Node}
     */
    public void addInlineNode(Supplier<Node> generator) {
        StyledSegment seg = StyledSegment.inlineNode(generator);
        segments().add(seg);
    }

    private List<StyledSegment> segments() {
        if (segments == null) {
            segments = new ArrayList<>(8);
        }
        return segments;
    }

    // TODO via helper
    public List<Consumer<TextCell>> getHighlights() {
        return highlights;
    }

    // TODO via helper
    public List<StyledSegment> getSegments() {
        return segments;
    }
    
    private int size() {
        return segments == null ? 0 : segments.size();
    }
    
    public void export(int start, int end, StyledOutput out) throws IOException {
        if (segments == null) {
            out.append(StyledSegment.of(""));
        } else {
            int off = 0;
            int ct = size();
            for (int i = 0; i < ct; i++) {
                if (off >= end) {
                    return;
                }

                StyledSegment seg = segments.get(i);
                String text = seg.getText();
                int len = (text == null ? 0 : text.length());
                if (start <= (off + len)) {
                    int ix0 = Math.max(0, start - off);
                    int ix1 = Math.min(len, end - off);
                    StyledSegment ss = seg.subSegment(ix0, ix1);
                    out.append(ss);
                }
                off += len;
            }
        }
    }
}
