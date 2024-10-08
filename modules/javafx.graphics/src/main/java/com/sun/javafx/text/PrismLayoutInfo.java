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

import javafx.geometry.Rectangle2D;
import javafx.scene.text.LayoutInfo;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.text.TextLayout;

/**
 * Layout information as reported by PrismLayout.
 */
public final class PrismLayoutInfo implements LayoutInfo {
    private final TLine[] lines;
    private final Rectangle2D bounds;

    public PrismLayoutInfo(TLine[] lines, Rectangle2D bounds) {
        this.lines = lines;
        this.bounds = bounds;
    }

    public static LayoutInfo of(TextLayout layout) {
        com.sun.javafx.scene.text.TextLine[] ls = layout.getLines();
        TLine[] lines = new TLine[ls.length];
        for (int i = 0; i < ls.length; i++) {
            com.sun.javafx.scene.text.TextLine ln = ls[i];
            lines[i] = TLine.of(ls[i]);
        }

        BaseBounds b = layout.getBounds();
        Rectangle2D bounds = new Rectangle2D(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
        return new PrismLayoutInfo(lines, bounds);
    }

    @Override
    public Rectangle2D getBounds() {
        return bounds;
    }

    @Override
    public int getTextLineCount() {
        return lines.length;
    }

    @Override
    public int getTextLineStart(int ix) {
        return lines[ix].start;
    }

    @Override
    public int getTextLineEnd(int ix) {
        return lines[ix].end;
    }

    @Override
    public Rectangle2D getLineBounds(int ix) {
        return lines[ix].bounds;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PrismLayoutInfo{");
        sb.append("lines=[");
        boolean sep = false;
        for (TLine li : lines) {
            if (sep) {
                sb.append(",");
            } else {
                sep = true;
            }
            li.print(sb);
        }
        sb.append("]");
        sb.append(", bounds=").append(bounds);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Contains a snapshot of the mutable TextLine.
     */
    private static record TLine(
        int start,
        int end,
        Rectangle2D bounds) {

        public static TLine of(com.sun.javafx.scene.text.TextLine t) {
            int start = t.getStart();
            int end = t.getStart() + t.getLength();
            RectBounds r = t.getBounds();
            Rectangle2D bounds = new Rectangle2D(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());

            return new TLine(
                start,
                end,
                bounds
            );
            // TODO also available:
            // public float getLeftSideBearing(); // what is that??
            // public float getRightSideBearing();
            // TODO we could also capture the text runs information if needed
        }

        void print(StringBuilder sb) {
            sb.append("{start=").append(start);
            sb.append(", end=").append(end);
            sb.append(", bounds=").append(bounds);
            sb.append("}");
        }
    }
}
