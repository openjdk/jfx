/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
        return true;
    }

    private String text;
    private Font font;
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
        final double fontSize = (font == null ? 0 : ((Font)font).getSize());
        final String[] lines = text.split("\n");
        double width = 0.0;
        double height = fontSize * lines.length;
        for (String line : lines) {
            width = Math.max(width, fontSize * line.length());
        }
        return bounds.deriveWithNewBounds(0, (float)-fontSize, 0,
                (float)width, (float)(height-fontSize), 0);
    }

    @Override
    public TextLine[] getLines() {
        return null;
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
    public HitInfo getHitInfo(float x, float y) {
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

    @Override
    public PathElement[] getCaretShape(int offset, boolean isLeading, float x,
            float y) {
        return new PathElement[0];
    }

    @Override
    public PathElement[] getRange(int start, int end, int type, float x, float y) {
        return new PathElement[0];
    }

}
