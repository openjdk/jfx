/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jfx.incubator.scene.control.richtext;

import javafx.geometry.Insets;
import javafx.util.Duration;

/**
 * Various constants.
 */
public class Params {
    /** number of paragraph for accessibility window */
    public static final int ACCESSIBILITY_WINDOW_SIZE = 64;

    /** autoscroll animation period, milliseconds. */
    public static final int AUTO_SCROLL_PERIOD = 100;

    /** autoscroll switches to fast mode when mouse is moved further out of the view, pixels. */
    public static final double AUTO_SCROLL_FAST_THRESHOLD = 100;

    /** "fast" autoscroll step, in pixels. */
    public static final double AUTO_SCROLL_STEP_FAST = 200;

    /** "slow" autoscroll step, in pixels. */
    public static final double AUTO_SCROLL_STEP_SLOW = 20;

    /** cell cache size. */
    public static final int CELL_CACHE_SIZE = 512;

    /** default caret blink period. */
    public static final Duration DEFAULT_CARET_BLINK_PERIOD = Duration.millis(1000);

    /** default value for {@code displayCaret} property */
    public static final boolean DEFAULT_DISPLAY_CARET = true;

    /** default value for {@code highlightCurrentParagraph} property */
    public static final boolean DEFAULT_HIGHLIGHT_CURRENT_PARAGRAPH = false;

    /** default value for {@code useContentHeight} property */
    public static final boolean DEFAULT_USE_CONTENT_HEIGHT = false;

    /** default tab size in the CodeArea */
    public static final int DEFAULT_TAB_SIZE = 8;

    /** default value for {@code useContentWidth} property */
    public static final boolean DEFAULT_USE_CONTENT_WIDTH = false;

    /** default value for {@code wrapText} property */
    public static final boolean DEFAULT_WRAP_TEXT = false;

    /** ensures the caret is always visible when reaching the edge of screen in unwrapped mode, in pixels. */
    public static final double HORIZONTAL_GUARD = 0; //10; FIX restore

    /** focus background outline size */
    public static final double LAYOUT_FOCUS_BORDER = 1;

    /** min height of the content area when use content width = true and empty model */
    public static final double LAYOUT_MIN_HEIGHT = 10;

    /** min width of the content area when use content width = true and empty model */
    public static final double LAYOUT_MIN_WIDTH = 20;

    /** prevents lockup when useContentHeight is enabled with a large model */
    public static final double MAX_HEIGHT_SAFEGUARD = 10_000;

    /** maximum width for unwrapped TextFlow layout. Neither Double.MAX_VALUE nor 1e20 work, probably bc float */
    public static final double MAX_WIDTH_FOR_LAYOUT = 1_000_000_000.0;

    /** default minimum height */
    public static final double MIN_HEIGHT = 10;

    /** default minimum width */
    public static final double MIN_WIDTH = 10;

    /** minimum viewport width prevents extra tall text flows */
    public static final double MIN_VIEWPORT_WIDTH = 10;

    /** default preferred height */
    public static final double PREF_HEIGHT = 176;

    /** default preferred width */
    public static final double PREF_WIDTH = 480;

    /** scroll bars block increment, fraction of view width/height (between 0.0 and 1.0). */
    public static final double SCROLL_BARS_BLOCK_INCREMENT = 0.05;

    /** scroll bars unit increment, fraction of view width/height (between 0.0 and 1.0). */
    public static final double SCROLL_BARS_UNIT_INCREMENT = 0.01;

    /** horizontal mouse wheel scroll block size as a fraction of window width. */
    public static final double SCROLL_SHEEL_BLOCK_SIZE_HORIZONTAL = 0.1;

    /** vertical mouse wheel scroll block size as a fraction of window height. */
    public static final double SCROLL_WHEEL_BLOCK_SIZE_VERTICAL = 0.1;

    /** The number of paragraphs to lay out before and after the view point in VFlow. */
    public static final int SLIDING_WINDOW_EXTENT = 100;
}
