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
import javafx.geometry.Rectangle2D;
import javafx.scene.text.CaretInfo;
import javafx.scene.text.LayoutInfo;
import javafx.scene.text.TextLineInfo;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLine;

/**
 * Layout information as reported by PrismLayout.
 */
public final class PrismLayoutInfo extends LayoutInfo {
    private final TextLayout layout;

    public PrismLayoutInfo(TextLayout layout) {
        this.layout = layout;
    }

    @Override
    public Rectangle2D getBounds() {
        return TextUtils.toRectangle2D(layout.getBounds());
    }

    @Override
    public int getTextLineCount() {
        return layout.getLines().length;
    }

    @Override
    public List<TextLineInfo> getTextLines() {
        TextLine[] lines = layout.getLines();
        int sz = lines.length;
        ArrayList<TextLineInfo> rv = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            rv.add(TextUtils.toLineInfo(lines[i]));
        }
        return Collections.unmodifiableList(rv);
    }

    @Override
    public TextLineInfo getTextLine(int index) {
        return TextUtils.toLineInfo(layout.getLines()[index]);
    }

    @Override
    public List<Rectangle2D> selectionShape(int start, int end) {
        return getGeometry(start, end, TextLayout.TYPE_TEXT);
    }

    @Override
    public List<Rectangle2D> strikeThroughShape(int start, int end) {
        return getGeometry(start, end, TextLayout.TYPE_STRIKETHROUGH);
    }

    @Override
    public List<Rectangle2D> underlineShape(int start, int end) {
        return getGeometry(start, end, TextLayout.TYPE_UNDERLINE);
    }

    private List<Rectangle2D> getGeometry(int start, int end, int type) {
        ArrayList<Rectangle2D> rv = new ArrayList<>();
        // TODO padding/border JDK-8341438?
        layout.getRange(start, end, type, (left, top, right, bottom) -> {
            if (left < right) {
                rv.add(new Rectangle2D(left, top, right - left, bottom - top));
            } else {
                rv.add(new Rectangle2D(right, top, left - right, bottom - top));
            }
        });
        return Collections.unmodifiableList(rv);
    }

    private TextLine line(int ix) {
        return layout.getLines()[ix];
    }

    @Override
    public CaretInfo caretInfo(int charIndex, boolean leading) {
        float[] c = layout.getCaretInf(charIndex, leading);

        // TODO padding/border JDK-8341438?
        double[][] lines;
        if (c.length == 3) {
            // {x, ymin, ymax} - corresponds to a single line from (x, ymin) tp (x, ymax)
            lines = new double[][] {
                new double[] {
                    c[0], c[1], c[2]
                }
            };
        } else {
            // {x, y, y2, x2, ymax} - corresponds to a split caret drawn as two lines, the first line
            // drawn from (x,y) to (x, y2), the second line drawn from (x2, y2) to (x2, ymax).
            double y2 = c[2];
            lines = new double[][] {
                new double[] {
                    c[0], c[1], y2
                },
                new double[] {
                    c[3], y2, c[4]
                }
            };
        }
        return new PrismCaretInfo(lines);
    }
}
