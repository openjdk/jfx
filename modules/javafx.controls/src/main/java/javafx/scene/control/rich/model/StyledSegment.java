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

import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.rich.StyleResolver;

/**
 * Data structure used to modify the styled text model.
 * <p>
 * Represents:
 * <ol>
 * <li> a single text segment with direct style and/or style names
 * <li> a line break
 * <li> an inline Node
 * <li> a paragraph containing a single Node
 * </ol>
 */
// TODO in addition to is*(), add getType() returning an enum { TEXT, PARAGRAPH, INLINE_NODE, LINE_BREAK }
// TODO perhaps add guarded/unguarded factory methods (of(), ofGuarded()) that check for <0x20, or specify that
// text must not include those characters.
public abstract class StyledSegment {
    /**
     * Returns true if this segment is a text segment.
     */
    public boolean isText() { return false; }

    /**
     * Returns true if this segment is a paragraph which contains a single Node.
     */
    public boolean isParagraph() { return false; }

    /**
     * Returns true if this segment is a line break.
     */
    public boolean isLineBreak() { return false; }

    /**
     * Returns true if this segment represents an inline Node.
     * TODO not yet supported due to https://bugs.openjdk.org/browse/JDK-8305001
     */
    public boolean isInlineNode() { return false; }

    /**
     * Returns the text associated with this segment.
     * Must be one character for inline nodes, must be null for node paragraphs.
     * TODO can it be null for text segments?
     */
    public String getText() { return null; }

    /**
     * This method must return a non-null value when {@link isParagraph()} is true, 
     * or null in any other case.
     * TODO inline node?
     */
    public Supplier<Node> getNodeGenerator() { return null; }

    /**
     * This method returns StyleAttrs (or null) for this segment.
     * When the model manages style names (instead of actual attributes), an instance of {@link StyleResolver}
     * may be used to convert the style names to the attributes.
     * Keep in mind that different views might have different CSS styles applied and as a result a different
     * set of attributes might be produced for the same segment.
     */
    public StyleAttrs getStyleAttrs(StyleResolver resolver) { return null; }

    private StyledSegment() {
    }

    /** A styled segment that represents a line break */
    public static final StyledSegment LINE_BREAK = new StyledSegment() {
        @Override
        public boolean isLineBreak() {
            return true;
        }

        @Override
        public String toString() {
            return "LINE_BREAK";
        }
    };

    /** 
     * Creates a StyleSegment from a non-null text and non-null attributes.
     * Important: text must not contain any characters &lt; 0x20, except for TAB.
     */
    public static StyledSegment of(String text, StyleInfo si) {
        return new StyledSegment() {
            @Override
            public boolean isText() {
                return true;
            }
            
            @Override
            public String getText() {
                return text;
            }

            @Override
            public StyleAttrs getStyleAttrs(StyleResolver r) {
                return si.getStyleAttrs(r);
            }

            @Override
            public String toString() {
                return "StyledSegment{text=" + getText() + ", style=" + si + "}";
            }
        };
    }
    
    /** 
     * Creates a StyleSegment from a non-null plain text.
     * Important: text must not contain any characters &lt; 0x20, except for TAB.
     */
    public static StyledSegment of(String text) {
        return of(text, StyleInfo.NONE);
    }
    
    /**
     * Creates a StyleSegment from a non-null plain text and style attributes.
     * Important: text must not contain any characters &lt; 0x20, except for TAB.
     */
    public static StyledSegment of(String text, StyleAttrs a) {
        return new StyledSegment() {
            @Override
            public boolean isText() {
                return true;
            }
            
            @Override
            public String getText() {
                return text;
            }
            
            @Override
            public StyleAttrs getStyleAttrs(StyleResolver r) {
                // TODO must be immutable
                return a;
            }
            
            @Override
            public String toString() {
                return "StyledSegment{text=" + getText() + ", attrs=" + a + "}";
            }
        };
    }
    
    // TODO
    /** 
     * Creates a StyleSegment from a non-null text with direct and stylesheet styles.
     * Important: text must not contain any characters < 0x20, except for TAB.
     */
//    public static StyledSegment of(String text, String direct, String[] css) {
//        StyleInfo si
//        return new StyledSegment() {
//            @Override
//            public boolean isText() {
//                return true;
//            }
//
//            @Override
//            public String getText() {
//                return text;
//            }
//
//            @Override
//            public String getDirectStyle() {
//                return direct;
//            }
//
//            @Override
//            public String[] getStyles() {
//                return css;
//            }
//
//            @Override
    // TODO move to StyleInfo
//            public String toString() {
//                StringBuilder sb = new StringBuilder(32);
//                sb.append("StyledSegment{text=").append(text);
//                sb.append(", direct=").append(direct);
//                if (css != null) {
//                    sb.append(", css=[");
//                    boolean sep = false;
//                    for (String s : css) {
//                        if (sep) {
//                            sb.append(',');
//                        } else {
//                            sep = true;
//                        }
//                        sb.append(s);
//                    }
//                    sb.append("]");
//                }
//                sb.append("}");
//                return sb.toString();
//            }
//        };
//    }

    /**
     * Creates a StyledSegment which consists of a single inline Node.
     */
    public static StyledSegment inlineNode(Supplier<Node> generator) {
        return new StyledSegment() {
            @Override
            public boolean isInlineNode() {
                return true;
            }

            @Override
            public String getText() {
                return " ";
            }

            @Override
            public Supplier<Node> getNodeGenerator() {
                return generator;
            }
        };
    }
    
    /**
     * Creates a StyledSegment for a paragraph that contains a Node.
     */
    public static StyledSegment nodeParagraph(Supplier<Node> generator) {
        return new StyledSegment() {
            @Override
            public boolean isParagraph() {
                return true;
            }

            @Override
            public Supplier<Node> getNodeGenerator() {
                return generator;
            }
        };
    }
}