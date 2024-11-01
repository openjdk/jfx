/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Insets;
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
public abstract class PrismLayoutInfo extends LayoutInfo {

    protected abstract double lineSpacing();
    
    protected abstract Insets insets();

    private final TextLayout layout;

    public PrismLayoutInfo(TextLayout layout) {
        this.layout = layout;
    }

    @Override
    public Rectangle2D getBounds(boolean includeLineSpacing) {
        BaseBounds b = layout.getBounds();
        Insets m = insets();
        double dx = m.getLeft(); // TODO rtl?
        double dy = m.getTop();
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
        Insets m = insets();
        double dx = m.getLeft(); // TODO rtl?
        double dy = m.getTop();
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
        Insets m = insets();
        double dx = m.getLeft(); // TODO rtl?
        double dy = m.getTop();
        double sp = includeLineSpacing ? lineSpacing() : 0.0;
        return TextUtils.toLineInfo(layout.getLines()[index], dx, dy, sp);
    }

    @Override
    public List<Rectangle2D> selectionShape(int start, int end, boolean includeLineSpacing) {
        return getGeometry(start, end, TextLayout.TYPE_TEXT, lineSpacing());
    }

    @Override
    public List<Rectangle2D> strikeThroughShape(int start, int end) {
        return getGeometry(start, end, TextLayout.TYPE_STRIKETHROUGH, 0.0);
    }

    @Override
    public List<Rectangle2D> underlineShape(int start, int end) {
        return getGeometry(start, end, TextLayout.TYPE_UNDERLINE, 0.0);
    }

    private List<Rectangle2D> getGeometry(int start, int end, int type, double lineSpacing) {
        Insets m = insets();
        double dx = m.getLeft(); // TODO RTL?
        double dy = m.getTop();

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

    private TextLine line(int ix) {
        return layout.getLines()[ix];
    }

    @Override
    public CaretInfo caretInfo(int charIndex, boolean leading) {
        Insets m = insets();
        double dx = m.getLeft(); // TODO RTL?
        double dy = m.getTop();
        float[] c = layout.getCaretInf(charIndex, leading);

        Rectangle2D[] parts;
        if (c.length == 3) {
            // {x, ymin, ymax} - corresponds to a single line from (x, ymin) tp (x, ymax)
            double x = c[0];
            double ymin = c[1];
            double ymax = c[2];
            parts = new Rectangle2D[] {
                new Rectangle2D(x + dx, ymin + dy, 0.0, ymax - ymin)
            };
        } else {
            // {x, y, y2, x2, ymax} - corresponds to a split caret drawn as two lines, the first line
            // drawn from (x,y) to (x, y2), the second line drawn from (x2, y2) to (x2, ymax).
            double x = c[0];
            double y = c[1];
            double y2 = c[2];
            double x2 = c[3];
            double ymax = c[4];
            parts = new Rectangle2D[] {
                new Rectangle2D(x + dx, y + dy, 0.0, y2 - y),
                new Rectangle2D(x2 + dx, y2 + dy, 0.0, ymax - y2)
            };
        }
        return new PrismCaretInfo(parts);
    }
}
