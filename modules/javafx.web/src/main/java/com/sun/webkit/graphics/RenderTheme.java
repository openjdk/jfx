/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;

import java.lang.annotation.Native;

public abstract class RenderTheme extends Ref {

    @Native public static final int TEXT_FIELD = 0;
    @Native public static final int BUTTON = 1;
    @Native public static final int CHECK_BOX = 2;
    @Native public static final int RADIO_BUTTON = 3;
    @Native public static final int MENU_LIST = 4;
    @Native public static final int MENU_LIST_BUTTON = 5;
    @Native public static final int SLIDER = 6;
    @Native public static final int PROGRESS_BAR = 7;
    @Native public static final int METER = 8;

    @Native public static final int CHECKED = 1 << 0;
    @Native public static final int INDETERMINATE = 1 << 1;
    @Native public static final int ENABLED = 1 << 2;
    @Native public static final int FOCUSED = 1 << 3;
    @Native public static final int PRESSED = 1 << 4;
    @Native public static final int HOVERED = 1 << 5;
    @Native public static final int READ_ONLY = 1 << 6;

    @Native public static final int BACKGROUND = 0;
    @Native public static final int FOREGROUND = 1;

    protected abstract Ref createWidget(long id, int widgetIndex, int state, int w, int h, int bgColor, ByteBuffer extParams);

    public abstract void drawWidget(WCGraphicsContext g, Ref widget, int x, int y);

    protected abstract int getRadioButtonSize();

    protected abstract int getSelectionColor(int index);

    public abstract WCSize getWidgetSize(Ref widget);
}
