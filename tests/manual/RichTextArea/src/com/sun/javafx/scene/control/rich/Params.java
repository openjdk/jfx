/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.rich;

import javafx.incubator.scene.control.rich.model.StyleAttrs;

/**
 * Various constants.
 */
public class Params {
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

    /** default caret blink period, in milliseconds. */
    public static final int DEFAULT_CARET_BLINK_PERIOD = 500;

    /** default paragraph attributes */
    public static final StyleAttrs DEFAULT_PARAGRAPH_ATTRIBUTES;

    /** small space between the end of last character and the right edge when typing, in pixels. */
    public static final double HORIZONTAL_GUARD = 20;

    /** allows a little space for the caret shape when use content width is enabled */
    public static final double LAYOUT_CARET_ALLOWANCE = 1;
    
    /** focus background outline size */
    public static final double LAYOUT_FOCUS_BORDER = 1;

    /** min height of the content area when use content width = true and empty model */
    public static final double LAYOUT_MIN_HEIGHT = 20;

    /** min width of the content area when use content width = true and empty model */
    public static final double LAYOUT_MIN_WIDTH = 20;

    /** maximum width for unwrapped TextFlow layout. Neither Double.MAX_VALUE nor 1e20 work, probably bc float */
    public static final double MAX_WIDTH_FOR_LAYOUT = 1_000_000_000.0;

    /** scroll bars unit increment, fraction of view width/height (between 0.0 and 1.0). */
    public static final double SCROLL_BARS_UNIT_INCREMENT = 0.1;

    /** horizontal mouse wheel scroll block size as a fraction of window width. */
    public static final double SCROLL_SHEEL_BLOCK_SIZE_HORIZONTAL = 0.1;

    /** vertical mouse wheel scroll block size as a fraction of window height. */
    public static final double SCROLL_WHEEL_BLOCK_SIZE_VERTICAL = 0.1;

    /**
     * VFlow TextLayout sliding window extent before and after the visible area.
     * Must be > 1.0f for the relative navigation to work.
     */
    public static final float SLIDING_WINDOW_EXTENT = 3.0f;

    static {
        DEFAULT_PARAGRAPH_ATTRIBUTES = StyleAttrs.builder().build(); // or EMPTY?
    }
}
