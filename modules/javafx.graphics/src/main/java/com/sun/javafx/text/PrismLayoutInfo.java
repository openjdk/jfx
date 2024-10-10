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
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLine;

/**
 * Layout information as reported by PrismLayout.
 */
public final class PrismLayoutInfo extends LayoutInfo {
    private final TextLayout layout;

    private PrismLayoutInfo(TextLayout layout) {
        this.layout = layout;
    }

    public static LayoutInfo of(TextLayout layout) {
        return new PrismLayoutInfo(layout);
    }

    @Override
    public Rectangle2D getBounds() {
        return toRectangle2D(layout.getBounds());
    }

    @Override
    public int getTextLineCount() {
        return layout.getLines().length;
    }

    @Override
    public int getTextLineStart(int ix) {
        return line(ix).getStart();
    }

    @Override
    public int getTextLineEnd(int ix) {
        TextLine line = line(ix);
        return line.getStart() + line.getLength();
    }

    @Override
    public Rectangle2D getLineBounds(int ix) {
        return toRectangle2D(line(ix).getBounds());
    }

    private TextLine line(int ix) {
        return layout.getLines()[ix];
    }

    private static Rectangle2D toRectangle2D(BaseBounds b) {
        return new Rectangle2D(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
    }
}
