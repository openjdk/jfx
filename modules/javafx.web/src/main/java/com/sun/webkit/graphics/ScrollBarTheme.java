/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.graphics;

import java.lang.annotation.Native;

public abstract class ScrollBarTheme extends Ref {

    // See constants values WebCore/platform/ScrollTypes.h
    @Native public static final int NO_PART = 0;
    @Native public static final int BACK_BUTTON_START_PART = 1;
    @Native public static final int FORWARD_BUTTON_START_PART = 1 << 1;
    @Native public static final int BACK_TRACK_PART = 1 << 2;
    @Native public static final int THUMB_PART = 1 << 3;
    @Native public static final int FORWARD_TRACK_PART = 1 << 4;
    @Native public static final int BACK_BUTTON_END_PART = 1 << 5;
    @Native public static final int FORWARD_BUTTON_END_PART = 1 << 6;
    @Native public static final int SCROLLBAR_BG_PART = 1 << 7;
    @Native public static final int TRACK_BG_PART = 1 << 8;

    @Native public static final int HORIZONTAL_SCROLLBAR = 0;
    @Native public static final int VERTICAL_SCROLLBAR = 1;

    private static int thickness;

    public static int getThickness() {
        return thickness > 0 ? thickness : 12;
    }

    public static void setThickness(int value) {
        thickness = value;
    }

    protected abstract Ref createWidget(long id, int w, int h, int orientation, int value, int visibleSize, int totalSize);

    public abstract void paint(WCGraphicsContext g, Ref sbRef, int x, int y, int pressedPart, int hoveredPart);

    protected abstract void getScrollBarPartRect(long id, int part, int rect[]);

    public abstract WCSize getWidgetSize(Ref widget);
}
