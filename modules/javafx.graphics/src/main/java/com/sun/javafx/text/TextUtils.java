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
import javafx.geometry.Rectangle2D;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.TextLineInfo;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLine;

/**
 * Text-related Utilities.
 */
public final class TextUtils {
    /**
     * Queries the range geometry of the range of text within the text layout as
     * an array of {@code PathElement}s, for for one of the three possible types:
     * <ul>
     * <li>{@link #TYPE_STRIKETHROUGH} - strike-through shape
     * <li>{@link #TYPE_TEXT} - text selection shape
     * <li>{@link #TYPE_UNDERLINE} - underline shape
     * </ul>
     *
     * @param layout the text layout
     * @param start the start offset
     * @param end the end offset
     * @param type the type of geometry to query
     * @param dx the x offset to add to each path element
     * @param dy the y offset to add to each path element
     * @return the array of {@code PathElement}s
     */
    public static PathElement[] getRange(TextLayout layout, int start, int end, int type, double dx, double dy) {
        ArrayList<PathElement> a = new ArrayList<>();
        layout.getRange(start, end, type, (left, top, right, bottom) -> {
            left += dx;
            right += dx;
            top += dy;
            bottom += dy;
            a.add(new MoveTo(left, top));
            a.add(new LineTo(right, top));
            a.add(new LineTo(right, bottom));
            a.add(new LineTo(left, bottom));
            a.add(new LineTo(left, top));
        });
        return a.toArray(PathElement[]::new);
    }

    public static TextLineInfo toLineInfo(TextLine line, double lineSpacing) {
        int start = line.getStart();
        int end = line.getStart() + line.getLength();
        Rectangle2D bounds = toRectangle2D(line.getBounds(), lineSpacing);
        return new TextLineInfo(start, end, bounds);
    }

    public static Rectangle2D toRectangle2D(BaseBounds b, double lineSpacing) {
        return new Rectangle2D(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight() + lineSpacing);
    }

    public static PathElement[] getCaretShape(float[] c, double dx, double dy) {
        if (c == null) {
            return null;
        } else if (c.length == 3) {
            // [x, ymin, ymax] - corresponds to a single line from (x, ymin) tp (x, ymax)
            double x = c[0] + dx;
            double ymin = c[1] + dy;
            double ymax = c[2] + dy;

            return new PathElement[] {
                new MoveTo(x, ymin),
                new LineTo(x, ymax)
            };
        } else {
            // [x, ymin, y2, x2, ymax] - corresponds to a split caret drawn as two lines, the first line
            // drawn from (x,ymin) to (x, y2), the second line drawn from (x2, y2) to (x2, ymax).
            double x = c[0];
            double ymin = c[1];
            double y2 = c[2];
            double x2 = c[3];
            double ymax = c[4];

            return new PathElement[] {
                new MoveTo(x, ymin),
                new LineTo(x, y2),
                new MoveTo(x2, y2),
                new LineTo(x2, ymax)
            };
        }
    }
}
