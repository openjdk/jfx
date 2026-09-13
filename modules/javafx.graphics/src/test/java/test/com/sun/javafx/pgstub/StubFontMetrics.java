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
package test.com.sun.javafx.pgstub;

import com.sun.javafx.font.Metrics;

/**
 * Stubbed FontMetrics, some numbers are still arbitrary.
 */
public class StubFontMetrics implements Metrics {
    public static final float BASELINE = 0.8f;
    public static final float BOLD_FONT_EXTRA_WIDTH = 1.0f;
    private final float size;

    public StubFontMetrics(float size) {
        this.size = size;
    }

    @Override
    public float getAscent() {
        return -size * BASELINE;
    }

    @Override
    public float getDescent() {
        return size * (1.0f - BASELINE);
    }

    @Override
    public float getLineGap() {
        return 0f;
    }

    @Override
    public float getLineHeight() {
        return size;
    }

    @Override
    public float getTypoAscent() {
        return getAscent();
    }

    @Override
    public float getTypoDescent() {
        return getDescent();
    }

    @Override
    public float getTypoLineGap() {
        return getLineGap();
    }

    @Override
    public float getXHeight() {
        return size;
    }

    @Override
    public float getCapHeight() {
        return size;
    }

    @Override
    public float getStrikethroughOffset() {
        return 0;
    }

    @Override
    public float getStrikethroughThickness() {
        return 1;
    }

    @Override
    public float getUnderLineOffset() {
        return 1;
    }

    @Override
    public float getUnderLineThickness() {
        return 1;
    }
}
