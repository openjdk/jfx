/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.scene.text.GlyphList;
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

    public static TextLineInfo toLineInfo(TextLine line, double dx, double dy, double lineSpacing) {
        int start = line.getStart();
        int end = line.getStart() + line.getLength();

        // all the runs on the same line have the same y coordinate
        for (GlyphList g : line.getRuns()) {
            Point2D p = g.getLocation();
            dx += p.x;
            dy += p.y;
            break;
        }

        Rectangle2D bounds = toRectangle2D(line.getBounds(), dx, dy, lineSpacing);
        return new TextLineInfo(start, end, bounds);
    }

    public static Rectangle2D toRectangle2D(BaseBounds b, double dx, double dy, double lineSpacing) {
        // you wouldn't believe it, but width can be negative in BaseBounds
        double w = Math.abs(b.getWidth());
        return new Rectangle2D(b.getMinX() + dx, dy, w, b.getHeight() + lineSpacing);
    }

    public static PathElement[] getCaretShape(float[] c, double dx, double dy) {
        if (c == null) {
            return null;
        } else if (c.length == 3) {
            // [x, y, h] - corresponds to a single line from (x, y) to (x, y + h)
            double x = c[0] + dx;
            double y = c[1] + dy;
            double h = c[2];

            return new PathElement[] {
                new MoveTo(x, y),
                new LineTo(x, y + h)
            };
        } else {
            // [x, y, x2, h] - corresponds to a split caret drawn as two lines, the first line
            // drawn from (x, y) to (x, y + h/2), the second line drawn from (x2, y + h/2) to (x2, y + h).
            double x = c[0] + dx;
            double y = c[1] + dy;
            double x2 = c[2] + dx;
            double h = c[3];
            double y2 = y + h/2.0;

            return new PathElement[] {
                new MoveTo(x, y),
                new LineTo(x, y2),
                new MoveTo(x2, y2),
                new LineTo(x2, y + h)
            };
        }
    }
}
