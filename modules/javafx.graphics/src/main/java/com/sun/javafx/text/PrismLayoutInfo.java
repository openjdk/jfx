/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javafx.geometry.Rectangle2D;
import javafx.scene.text.CaretInfo;
import javafx.scene.text.LayoutInfo;
import javafx.scene.text.TextLineInfo;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLine;

/**
 * Layout information as reported by PrismLayout.
 */
public abstract non-sealed class PrismLayoutInfo extends LayoutInfo {

    protected abstract double lineSpacing();

    protected abstract double dx();

    protected abstract double dy();

    private final TextLayout layout;

    public PrismLayoutInfo(TextLayout layout) {
        this.layout = layout;
    }

    @Override
    public Rectangle2D getLogicalBounds(boolean includeLineSpacing) {
        BaseBounds b = layout.getBounds();
        double dx = dx();
        double dy = dy();
        double sp = includeLineSpacing ? lineSpacing() : 0.0;
        return TextUtils.toRectangle2D(b, dx, dy, sp);
    }

    @Override
    public int getTextLineCount() {
        return layout.getLines().length;
    }

    @Override
    public List<TextLineInfo> getTextLines(boolean includeLineSpacing) {
        TextLine[] lines = layout.getLines();
        double dx = dx();
        double dy = dy();
        double sp = includeLineSpacing ? lineSpacing() : 0.0;
        int sz = lines.length;

        ArrayList<TextLineInfo> rv = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            rv.add(TextUtils.toLineInfo(lines[i], dx, dy, sp));
        }
        return Collections.unmodifiableList(rv);
    }

    @Override
    public TextLineInfo getTextLine(int index, boolean includeLineSpacing) {
        TextLine[] lines = layout.getLines();
        Objects.checkIndex(index, lines.length);
        double dx = dx();
        double dy = dy();
        double sp = includeLineSpacing ? lineSpacing() : 0.0;
        return TextUtils.toLineInfo(lines[index], dx, dy, sp);
    }

    @Override
    public List<Rectangle2D> getSelectionGeometry(int start, int end, boolean includeLineSpacing) {
        double sp = includeLineSpacing ? lineSpacing() : 0.0;
        return getGeometry(start, end, TextLayout.TYPE_TEXT, sp);
    }

    @Override
    public List<Rectangle2D> getStrikeThroughGeometry(int start, int end) {
        return getGeometry(start, end, TextLayout.TYPE_STRIKETHROUGH, 0.0);
    }

    @Override
    public List<Rectangle2D> getUnderlineGeometry(int start, int end) {
        return getGeometry(start, end, TextLayout.TYPE_UNDERLINE, 0.0);
    }

    private List<Rectangle2D> getGeometry(int start, int end, int type, double lineSpacing) {
        double dx = dx();
        double dy = dy();

        ArrayList<Rectangle2D> rv = new ArrayList<>();
        layout.getRange(start, end, type, (left, top, right, bottom) -> {
            if (left < right) {
                rv.add(new Rectangle2D(left + dx, top + dy, right - left, bottom - top + lineSpacing));
            } else {
                rv.add(new Rectangle2D(right + dx, top + dy, left - right, bottom - top + lineSpacing));
            }
        });
        return Collections.unmodifiableList(rv);
    }

    @Override
    public CaretInfo caretInfoAt(int charIndex, boolean leading) {
        TextLayout.CaretGeometry g = layout.getCaretGeometry(charIndex, leading);
        double dx = dx();
        double dy = dy();
        Rectangle2D[] parts = TextUtils.getCaretRectangles(g, dx, dy);
        return new PrismCaretInfo(parts);
    }
}
