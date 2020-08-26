/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.pgstub;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.scene.text.*;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;

public class StubTextLayout implements TextLayout {

    @Override
    public boolean setContent(TextSpan[] spans) {
        this.spans = spans;
        this.text = null; /* Initialized in getText() */
        this.nullFontSize = 10; // need a non-zero size
        return true;
    }

    private TextSpan[] spans;
    private String text;
    private Font font;
    private int tabSize = DEFAULT_TAB_SIZE;
    private int nullFontSize = 0;
    private float spacing;

    @Override
    public boolean setContent(String text, Object font) {
        this.text = text;
        final StubFontLoader.StubFont stub = ((StubFontLoader.StubFont)font);
        this.font = stub == null ? null : stub.font;
        return true;
    }

    @Override
    public boolean setAlignment(int alignment) {
        return true;
    }

    @Override
    public boolean setWrapWidth(float wrapWidth) {
        return true;
    }

    @Override
    public boolean setLineSpacing(float spacing) {
        this.spacing = spacing;

        return true;
    }

    @Override
    public boolean setDirection(int direction) {
        return true;
    }

    @Override
    public boolean setBoundsType(int type) {
        return true;
    }

    @Override
    public BaseBounds getBounds() {
        return getBounds(null, new RectBounds());
    }

    @Override
    public BaseBounds getBounds(TextSpan filter, BaseBounds bounds) {
        final double fontSize = (font == null ? nullFontSize : ((Font)font).getSize());
        final String[] lines = getText().split("\n");
        double width = 0.0;
        double height = fontSize * lines.length + spacing * (lines.length - 1);
        for (String line : lines) {
            final int length;
            if (line.contains("\t")) {
                // count chars but when encountering a tab round up to a tabSize boundary
                char [] chrs = line.toCharArray();
                int spaces = 0;
                for (int i = 0; i < chrs.length; i++) {
                    if (chrs[i] == '\t') {
                        if (tabSize != 0) {
                            while ((++spaces % tabSize) != 0) {}
                        }
                    } else {
                        spaces++;
                    }
                }
                length = spaces;
            } else {
                length = line.length();
            }
            width = Math.max(width, fontSize * length);
        }
        return bounds.deriveWithNewBounds(0, (float)-fontSize, 0,
                (float)width, (float)(height-fontSize), 0);
    }

    class StubTextLine implements TextLine {
        @Override public GlyphList[] getRuns() {
            return new GlyphList[0];
        }
        @Override public RectBounds getBounds() {
            return new RectBounds();
        }
        @Override public float getLeftSideBearing() {
            return 0;
        }
        @Override public float getRightSideBearing() {
            return 0;
        }
        @Override public int getStart() {
            return 0;
        }
        @Override public int getLength() {
            return 0;
        }
    }

    @Override
    public TextLine[] getLines() {
        return new TextLine[] {new StubTextLine()};
    }

    @Override
    public GlyphList[] getRuns() {
        return new GlyphList[0];
    }

    @Override
    public Shape getShape(int type, TextSpan filter) {
        return new Path2D();
    }

    @Override
    public Hit getHitInfo(float x, float y) {
        // TODO this probably needs to be entirely rewritten...
        if (getText() == null) {
            return new Hit(0, -1, true);
        }

        final double fontSize = (font == null ? nullFontSize : ((Font)font).getSize());
        final String[] lines = text.split("\n");
        int lineIndex = Math.min(lines.length - 1, (int) (y / fontSize));
        if (lineIndex >= lines.length) {
            throw new IllegalStateException("Asked for hit info out of y range: x=" + x + "y=" +
                    + y + "text='" + text + "', lineIndex=" + lineIndex + ", numLines=" + lines.length +
                    ", fontSize=" + fontSize);
        }
        int offset = 0;
        for (int i=0; i<lineIndex; i++) {
            offset += lines[i].length() + 1; // add in the \n
        }

        int charPos = (int) (x / lines[lineIndex].length());
        if (charPos + offset > text.length()) {
            throw new IllegalStateException("Asked for hit info out of x range");
        }

        return new Hit(offset + charPos, -1, true);
    }

    @Override
    public PathElement[] getCaretShape(int offset, boolean isLeading, float x,
            float y) {
        return new PathElement[0];
    }

    @Override
    public PathElement[] getRange(int start, int end, int type, float x, float y) {
        return new PathElement[0];
    }

    @Override
    public BaseBounds getVisualBounds(int type) {
        return new RectBounds();
    }

    @Override
    public boolean setTabSize(int spaces) {
        if (spaces < 1) {
            spaces = 1;
        }
        if (tabSize != spaces) {
            tabSize = spaces;
            return true;
        }
        return false;
    }

    private String getText() {
        if (text == null) {
            if (spans != null) {
                StringBuilder sb = new StringBuilder();
                for (TextSpan span : spans) {
                    sb.append(span.getText());
                }
                text = sb.toString();
            }
        }
        return text;
    }
}
