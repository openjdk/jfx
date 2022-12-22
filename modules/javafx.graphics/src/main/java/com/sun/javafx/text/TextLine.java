/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.RectBounds;

public class TextLine implements com.sun.javafx.scene.text.TextLine {
    TextRun[] runs;
    RectBounds bounds;
    float lsb, rsb, leading;
    int start;
    int length;

    public TextLine(int start, int length, TextRun[] runs,
                    float width, float ascent, float descent, float leading) {
        this.start = start;
        this.length = length;
        this.bounds = new RectBounds(0, ascent, width, descent + leading);
        this.leading = leading;
        this.runs = runs;
    }

    @Override
    public RectBounds getBounds() {
        return bounds;
    }

    public float getLeading() {
        return leading;
    }

    @Override
    public TextRun[] getRuns() {
        return runs;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getLength() {
        return length;
    }

    public void setSideBearings(float lsb, float rsb) {
        this.lsb = lsb;
        this.rsb = rsb;
    }

    @Override
    public float getLeftSideBearing() {
        return lsb;
    }

    @Override
    public float getRightSideBearing() {
        return rsb;
    }

    public void setAlignment(float x) {
        bounds.setMinX(x);
        bounds.setMaxX(x + bounds.getMaxX());
    }

    public void setWidth(float width) {
        bounds.setMaxX(bounds.getMinX() + width);
    }
}
