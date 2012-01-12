/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.pgstub;

import javafx.scene.text.Font;

import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.sg.PGText;

public class StubText extends StubShape implements PGText {
    // for tests
    private float x;
    private float y;
    private float wrappingWidth;
    private String text;
    private boolean strikethrough;
    private boolean underline;
    private Object font;
    private int textBoundsType;
    private int textOrigin;
    private int textAlignment;
    private int fontSmoothingType;

    public RectBounds computeLayoutBounds(RectBounds bounds) {
        // We assume that the font point size == pixel height,
        // and completely square glyphs, mono-spaced.
        if (text == null) return bounds.makeEmpty();

        final double fontSize = (font == null ? 0 : ((Font)font).getSize());
        final String[] lines = text.split("\n");
        double width = 0.0;
        double height = fontSize * lines.length;
        for (String line : lines) {
            width = Math.max(width, fontSize * line.length());
        }

        return (RectBounds) bounds.deriveWithNewBounds(0, 0, 0, (float)width, (float)height, 0);
    }

    public void setText(String text) { this.text = text;}
    public String getText() {return text;}

    public void setLocation(float x, float y) { this.x = x; this.y = y;}
    public float getX() {return x;}
    public float getY() {return y;}

    public void setFont(Object f) { font = f; }
    public Object getFont() { return font; }
    
    public void setTextBoundsType(int textBoundsType) {
        this.textBoundsType = textBoundsType;
    }
    public int getTextBoundsType() { return textBoundsType; }
    
    public void setTextOrigin(int textOrigin) { this.textOrigin = textOrigin; }
    public int getTextOrigin() { return textOrigin; }
    
    public void setWrappingWidth(float width) { this.wrappingWidth = width;}
    public float getWrappingWidth() {return wrappingWidth;}

    public void setUnderline(boolean underline) { this.underline = underline;}
    public boolean isUnderline() { return underline;}
    
    public void setStrikethrough(boolean strikethrough) { this.strikethrough = strikethrough;}
    public boolean isStrikethrough() {return strikethrough;}

    public void setTextAlignment(int alignment) { textAlignment = alignment; }
    public int getTextAlignment() { return textAlignment; }
    
    public int getFontSmoothingType() { return fontSmoothingType; }
    public void setFontSmoothingType(int fontSmoothing) { 
        fontSmoothingType = fontSmoothing;
    }

    public void setInputMethodText(int start, Object text) { }

    // somewhat questionable -- do these remain in PGText??
    public void setLogicalSelection(int start, int end) { }
    public void setSelectionPaint(Object strokePaint, Object fillPaint) { }
    // given the x, y point, give the insertion index into the string
    public Object getHitInfo(float x, float y) {
        // TODO this probably needs to be entirely rewritten...
        if (text == null) {
            final HitInfo hit = new HitInfo();
            hit.setCharIndex(0);
            hit.setLeading(true);
            return hit;
        }

        final double fontSize = (font == null ? 0 : ((Font)font).getSize());
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

        final HitInfo hit = new HitInfo();
        hit.setCharIndex(offset + charPos);
        return hit;
    }
    public Object getCaretShape(int charIndex, boolean isLeading) { return null; }
    public Object getSelectionShape() { return null; }
    public Object getRangeShape(int start, int end) { return null; }
}
