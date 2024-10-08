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
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.text.TextLayout;

/**
 * Layout information as reported by PrismLayout.
 */
public final class PrismLayoutInfo implements LayoutInfo {
    // TODO
    // also available:
    // public BaseBounds getBounds();
    private final TLine[] lines;

    public PrismLayoutInfo(TLine[] lines) {
        this.lines = lines;
    }

    public static LayoutInfo of(TextLayout la) {
        com.sun.javafx.scene.text.TextLine[] ls = la.getLines();
        TLine[] lines = new TLine[ls.length];
        for (int i = 0; i < ls.length; i++) {
            com.sun.javafx.scene.text.TextLine ln = ls[i];
            lines[i] = new TLine(ls[i]);
        }
        return new PrismLayoutInfo(lines);
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
        RectBounds r = lines[ix].bounds;
        return new Rectangle2D(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
    }

    private static class TLine {
        public int start;
        public int end;
        public RectBounds bounds;

        public TLine(com.sun.javafx.scene.text.TextLine t) {
            this.start = t.getStart();
            this.end = t.getStart() + t.getLength();
            this.bounds = t.getBounds();
            // TODO also available:
            // public float getLeftSideBearing(); // what is that??
            // public float getRightSideBearing();
        }
    }
}
