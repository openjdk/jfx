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

package javafx.scene.control.rich;

import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import com.sun.javafx.scene.control.rich.FxPathBuilder;
import com.sun.javafx.scene.control.rich.HighlightShape;
import com.sun.javafx.scene.control.rich.RichUtils;
import com.sun.javafx.scene.control.rich.TextCellHelper;

/**
 * Provides a visual representation of a paragraph.
 * <p>
 * Typically, a TextCell contains a TextFlow with styled text (and possibly inline Nodes).
 * It is also possible to create a TextCell containing a single Region (which can be a Node of any complexity,
 * even including a different instance of RichTextArea).  These Regions will be presented as is, and,
 * for editable models, would not be editable via the RichTextArea mechanisms.
 * <p>
 * A typical usage would be the StyledModel creating a new instance of TextCell and populating it with styled
 * segments, inline Nodes, highlights (TODO), underlines (TODO).
 * <p>
 * Each visible TextCell will be resized horizontally to fill the available width and then resized vertically
 * according to its preferred size for that width. 
 */
public final class TextCell {
    private final int index;
    private final Region content;
    private double height;
    private double y;
    static {
        TextCellHelper.setAccessor(new TextCellHelper.Accessor() {
            @Override
            public void addBoxOutline(TextCell cell, FxPathBuilder b, double x, double w, double h) {
                cell.addBoxOutline(b, x, w, h);
            }
            
            @Override
            public PathElement[] getCaretShape(TextCell cell, int cix, boolean leading) {
                return cell.getCaretShape(cix, leading);
            }

            @Override
            public PathElement[] getRangeShape(TextCell cell, int start, int end) {
                return cell.getRangeShape(start, end);
            }
            
            @Override
            public double getHeight(TextCell cell) {
                return cell.getHeight();
            }

            @Override
            public double getY(TextCell cell) {
                return cell.getY();
            }

            @Override
            public void setPosition(TextCell cell, double y, double h) {
                cell.setPosition(y, h);
            }
        });
    }

    /**
     * Creates a text cell with the specified {@code Region} as its content.
     * @param index paragraph index
     * @param content non-null content
     */
    public TextCell(int index, Region content) {
        Objects.nonNull(content);
        this.index = index;
        this.content = content;
        content.setManaged(false);
    }

    /**
     * Creates a text cell with {@link TextFlow} as its content.
     * @param index paragraph index
     */
    public TextCell(int index) {
        this(index, new TextFlow());
    }

    /**
     * Returns the content of this cell.
     * @return the content Region
     */
    public final Region getContent() {
        return content;
    }

    /**
     * Adds a styled text segment to a {@link TextFlow}-based {@code TextCell}.
     * This method will throw an IllegalArgumentException if this text cell has content other
     * than the TextFlow.
     *
     * @param text non-null text string
     * @param style direct style (such as {@code -fx-fill:red;}), or null
     * @param css array of style names, or null
     * @return {@link Text} node added
     */
//    public Text addSegment(String text, String style, String[] css) {
//        Objects.nonNull(text);
//        Text t = new Text(text);
//        if (style != null) {
//            t.setStyle(style);
//        }
//        if (css != null) {
//            t.getStyleClass().addAll(css);
//        }
//        flow().getChildren().add(t);
//        return t;
//    }

    /**
     * Adds a text segment with no styling (i.e. using default style).
     * This method is an alias for {@code addSegment(text, null, null)}.
     *
     * @param text segment text
     * @return {@link Text} node added
     */
//    public Text addSegment(String text) {
//        Text t = new Text(text);
//        flow().getChildren().add(t);
//        return t;
//    }

    /**
     * Adds an inline node to a {@link TextFlow}-based text cell.
     *
     * @param <T> actual Node type
     * @param node node to add
     * @return added {@code Node}
     */
//    public <T extends Node> T addInlineNode(T node) {
//        flow().getChildren().add(node);
//        return node;
//    }

    /**
     * Adds a node to the text flow.
     * @param node the node to add
     */
    public void add(Node node) {
        flow().getChildren().add(node);
    }

    /**
     * Returns the model index for this text cell.
     * @return model index (>=0)
     */
    public final int getIndex() {
        return index;
    }

    /**
     * Returns the length of text in this cell.  A cell containing a non-text content will return 0.
     * @return the text length
     */
    public int getTextLength() {
        if (content instanceof TextFlow f) {
            return RichUtils.getTextLength(f);
        }
        return 0;
    }

    private TextFlow flow() {
        if(content instanceof TextFlow f) {
            return f;
        } else {
            throw new IllegalArgumentException("Not a TextFlow: " + content.getClass());
        }
    }

    /** sets cell position along the y axis of this cell in VFlow coordinates */
    private void setPosition(double y, double height) {
        this.y = y;
        this.height = height;
    }

    private double getHeight() {
        return height;
    }
    
    private double getY() {
        return y;
    }
    
    private void addBoxOutline(FxPathBuilder b, double x, double w, double h) {
        double y0 = content.getLayoutY();
        double y1 = y0 + h;
        
        b.moveto(x, y0);
        b.lineto(w, y0);
        b.lineto(w, y1);
        b.lineto(x, y1);
        b.lineto(x, y0);
    }

    private PathElement[] getCaretShape(int charIndex, boolean leading) {
        if (content instanceof TextFlow f) {
            PathElement[] p = f.caretShape(charIndex, leading);
            if (p.length == 2) {
                PathElement p0 = p[0];
                PathElement p1 = p[1];
                if ((p0 instanceof MoveTo m0) && (p1 instanceof LineTo m1)) {
                    if (Math.abs(m0.getY() - m1.getY()) < 0.01) {
                        double x = m0.getX();
                        double y = m0.getY();
                        // empty line generates a single dot shape, not what we need
                        // using text flow height to get us a line caret shape
                        p[1] = new LineTo(x, y + f.getHeight());
                    }
                }
            }
            return p;
        } else {
            return new PathElement[] {
                new MoveTo(0.0, 0.0),
                new LineTo(0.0, content.getHeight())
            };
        }
    }

    private PathElement[] getRangeShape(int start, int end) {
        if (content instanceof TextFlow f) {
            PathElement[] p = f.rangeShape(start, end);
            if (p != null) {
                if (p.length > 0) {
                    return p;
                }
            }

            return new PathElement[] {
                new MoveTo(0.0, 0.0),
                new LineTo(0.0, f.getHeight())
            };
        } else {
            double w = content.getWidth();
            double h = content.getHeight();

            return new PathElement[] {
                new MoveTo(0.0, 0.0),
                new LineTo(w, 0.0),
                new LineTo(w, h),
                new LineTo(0.0, h),
                new LineTo(0.0, 0.0)
            };
        }
    }

    /**
     * Highlights the specified text range.
     * @param start start offset for the range
     * @param end end offset for the range
     * @param color highlight color
     */
    public void addHighlight(int start, int end, Color color) {
        HighlightShape.addTo(content, HighlightShape.Type.HIGHLIGHT, start, end, color);
    }

    /**
     * Highlights the specified text range, using style names.
     * @param start start offset for the range
     * @param end end offset for the range
     * @param styles CSS style names
     */
    public void addHighlight(int start, int end, String... styles) {
        HighlightShape.addTo(content, HighlightShape.Type.HIGHLIGHT, start, end, styles);
    }

    /**
     * Underlines the specified text range using squiggly line (as typically used by a spell checker).
     * @param start start offset for the range
     * @param end end offset for the range
     * @param color highlight color
     */
    public void addSquiggly(int start, int end, Color color) {
        HighlightShape.addTo(content, HighlightShape.Type.SQUIGGLY, start, end, color);
    }

    /**
     * Underlines the specified text range using squiggly line (as typically used by a spell checker),
     * using style names.
     * @param start start offset for the range
     * @param end end offset for the range
     * @param styles CSS style names
     */
    public void addSquiggly(int start, int end, String... styles) {
        HighlightShape.addTo(content, HighlightShape.Type.SQUIGGLY, start, end, styles);
    }
}
