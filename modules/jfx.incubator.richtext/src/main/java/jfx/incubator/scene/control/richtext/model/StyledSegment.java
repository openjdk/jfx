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

import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import jfx.incubator.scene.control.richtext.StyleResolver;

/**
 * Data structure used to modify the styled text model.
 * <p>
 * Each instance represents:
 * <ol>
 * <li>a single text segment with direct style and/or style names
 * <li>a line break
 * <li>an inline Node
 * <li>a paragraph containing a single Region
 * <li>paragraph attributes
 * </ol>
 *
 * @since 24
 */
// TODO perhaps add guarded/unguarded factory methods (of(), ofGuarded()) that check for <0x20, or specify that
// text must not include those characters.
public abstract class StyledSegment {
    /** StyledSegment type */
    public enum Type {
        /** Identifies a segment which contains an inline node. */
        INLINE_NODE,
        /** Identifies a line break segment. */
        LINE_BREAK,
        /** Identifies a segment which contains the paragraph attributes. */
        PARAGRAPH_ATTRIBUTES,
        /** Identifies a segment which contains a single paragraph containing a {@code Region}. */
        REGION,
        /** Identifies a text segment */
        TEXT
    }

    /**
     * Returns the type of this StyledSegment.
     * @return the type
     */
    public abstract Type getType();

    /**
     * Returns the text associated with this segment.
     * Must be one character for inline nodes, must be null for node paragraphs or line breaks.
     * @return the segment plain text
     */
    public String getText() { return null; }

    /**
     * Returns the length of text in the segment, or 0 for segments that contain no text or where
     * {@link #getText()} returns null.
     * @return the length in characters
     */
    public int getTextLength() { return 0; }

    /**
     * This method must return a non-null value for a segment of {@code INLINE_NODE} type,
     * or null in any other case.
     * @return code that creates a Node instance, or null
     */
    public Supplier<Node> getInlineNodeGenerator() { return null; }

    /**
     * This method must return a non-null value for a segment of {@code REGION} type,
     * or null in any other case.
     * @return code that creates a Region instance, or null
     */
    public Supplier<Region> getParagraphNodeGenerator() { return null; }

    /**
     * This method returns StyleAttributeMap (or null) for this segment.
     * When the model manages style names (instead of actual attributes), an instance of {@link StyleResolver}
     * may be used to convert the style names to individual attributes.
     * Keep in mind that different views might have different stylesheet applied and
     * resulting in a different set of attributes.
     * @param resolver the style resolver to use
     * @return style attributes
     */
    public StyleAttributeMap getStyleAttributeMap(StyleResolver resolver) { return null; }

    /**
     * Creates a sub-segment of this segment.
     * @param start the start offset
     * @param end the end offset
     * @return the StyledSegment
     */
    public abstract StyledSegment subSegment(int start, int end);

    private StyledSegment() {
    }

    /** A styled segment that represents a line break */
    public static final StyledSegment LINE_BREAK = new StyledSegment() {
        @Override
        public Type getType() {
            return Type.LINE_BREAK;
        }

        @Override
        public String toString() {
            return "LINE_BREAK";
        }

        @Override
        public StyledSegment subSegment(int start, int end) {
            return this;
        }
    };

    /**
     * Creates a StyleSegment from a non-null plain text.
     * Important: text must not contain any characters &lt; 0x20, except for TAB.
     * @param text the segment text
     * @return the StyledSegment instance
     */
    // TODO guarded of() ?
    public static StyledSegment of(String text) {
        return of(text, StyleAttributeMap.EMPTY);
    }

    /**
     * Creates a StyleSegment from a non-null plain text and style attributes.
     * Important: text must not contain any characters &lt; 0x20, except for TAB.
     *
     * @param text the segment text
     * @param attrs the segment style attributes
     * @return the StyledSegment instance
     */
    // TODO guarded of() ?
    // TODO check for null text?
    public static StyledSegment of(String text, StyleAttributeMap attrs) {
        return new StyledSegment() {
            @Override
            public Type getType() {
                return Type.TEXT;
            }

            @Override
            public String getText() {
                return text;
            }

            @Override
            public int getTextLength() {
                return text.length();
            }

            @Override
            public StyleAttributeMap getStyleAttributeMap(StyleResolver resolver) {
                if ((resolver != null) && (attrs != null)) {
                    return resolver.resolveStyles(attrs);
                }
                return attrs;
            }

            @Override
            public StyledSegment subSegment(int start, int end) {
                if ((start == 0) && (end == text.length())) {
                    return this;
                }
                return StyledSegment.of(substring(text, start, end), attrs);
            }

            @Override
            public String toString() {
                return "StyledSegment{text=" + getText() + ", attrs=" + attrs + "}";
            }
        };
    }

    /**
     * Creates a StyledSegment which consists of a single inline Node.
     * @param generator the code to create a Node instance
     * @return the StyledSegment instance
     */
    public static StyledSegment ofInlineNode(Supplier<Node> generator) {
        return new StyledSegment() {
            @Override
            public Type getType() {
                return Type.INLINE_NODE;
            }

            @Override
            public String getText() {
                return " ";
            }

            @Override
            public int getTextLength() {
                return 1;
            }

            @Override
            public Supplier<Node> getInlineNodeGenerator() {
                return generator;
            }

            @Override
            public StyledSegment subSegment(int start, int end) {
                return this;
            }
        };
    }

    /**
     * Creates a StyledSegment for a paragraph that contains a single Region.
     * @param generator the code to create a Region instance
     * @return the StyledSegment instance
     */
    public static StyledSegment ofRegion(Supplier<Region> generator) {
        return new StyledSegment() {
            @Override
            public Type getType() {
                return Type.REGION;
            }

            @Override
            public Supplier<Region> getParagraphNodeGenerator() {
                return generator;
            }

            @Override
            public StyledSegment subSegment(int start, int end) {
                return this;
            }
        };
    }

    /**
     * Creates a StyledSegment which contains paragraph attributes only.
     * @param attrs the paragraph attributes
     * @return the StyledSegment instance
     */
    public static StyledSegment ofParagraphAttributes(StyleAttributeMap attrs) {
        return new StyledSegment() {
            @Override
            public Type getType() {
                return Type.PARAGRAPH_ATTRIBUTES;
            }

            @Override
            public StyleAttributeMap getStyleAttributeMap(StyleResolver resolver) {
                if (resolver != null) {
                    return resolver.resolveStyles(attrs);
                }
                return attrs;
            }

            @Override
            public StyledSegment subSegment(int start, int end) {
                return this;
            }

            @Override
            public String toString() {
                return "StyledSegment{par.attrs=" + attrs + "}";
            }
        };
    }

    private static String substring(String text, int start, int end) {
        int len = text.length();
        if ((start <= 0) && (end >= len)) {
            return text;
        } else {
            return text.substring(Math.max(0, start), Math.min(end, len));
        }
    }
}
