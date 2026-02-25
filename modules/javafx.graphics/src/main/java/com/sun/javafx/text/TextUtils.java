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
     * an array of {@code PathElement}s, for one of the three possible types:
     * <ul>
     * <li>{@link TextLayout#TYPE_STRIKETHROUGH} - strike-through shape
     * <li>{@link TextLayout#TYPE_TEXT} - text selection shape
     * <li>{@link TextLayout#TYPE_UNDERLINE} - underline shape
     * </ul>
     *
     * @param layout the text layout
     * @param start the start offset
     * @param end the end offset
     * @param type the type of geometry to query
     * @param dx the x offset to add to each path element
     * @param dy the y offset to add to each path element
     * @param lineSpacing the line spacing (applies only to TYPE_TEXT)
     * @return the array of {@code PathElement}s
     */
    public static PathElement[] getRange(TextLayout layout, int start, int end, int type, double dx, double dy, double lineSpacing) {
        ArrayList<PathElement> a = new ArrayList<>();
        layout.getRange(start, end, type, (left, top, right, bottom) -> {
            double leftEdge = left + dx;
            double rightEdge = right + dx;
            double topEdge = top + dy;
            double bottomEdge = bottom + dy + lineSpacing;
            a.add(new MoveTo(leftEdge, topEdge));
            a.add(new LineTo(rightEdge, topEdge));
            a.add(new LineTo(rightEdge, bottomEdge));
            a.add(new LineTo(leftEdge, bottomEdge));
            a.add(new LineTo(leftEdge, topEdge));
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

    public static PathElement[] getCaretPathElements(TextLayout.CaretGeometry g, double dx, double dy) {
        switch (g) {
        case TextLayout.CaretGeometry.Single s:
            double x = s.x() + dx;
            double y = s.y() + dy;
            return new PathElement[] {
                new MoveTo(x, y),
                new LineTo(x, y + s.height())
            };
        case TextLayout.CaretGeometry.Split s:
            double x1 = s.x1() + dx;
            double x2 = s.x2() + dx;
            double y1 = s.y() + dy;
            double y2 = y1 + s.height() / 2.0;
            return new PathElement[] {
                new MoveTo(x1, y1),
                new LineTo(x1, y2),
                new MoveTo(x2, y2),
                new LineTo(x2, y1 + s.height())
            };
        }
    }

    public static Rectangle2D[] getCaretRectangles(TextLayout.CaretGeometry g, double dx, double dy) {
        switch (g) {
        case TextLayout.CaretGeometry.Single s:
            return new Rectangle2D[] {
                new Rectangle2D(s.x() + dx, s.y() + dy, 0.0, s.height())
            };
        case TextLayout.CaretGeometry.Split s:
            double x1 = s.x1() + dx;
            double x2 = s.x2() + dx;
            double y = s.y() + dy;
            double h2 = s.height() / 2.0;
            return new Rectangle2D[] {
                new Rectangle2D(x1, y, 0.0, h2),
                new Rectangle2D(x2, y + h2, 0.0, h2)
            };
        }
    }
}
