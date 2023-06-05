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

/**
 * Various parameters and constants.
 */
public class Params {
    /** autoscroll animation period, milliseconds. */
    public static final int autoScrollPeriod = 100;
    
    /** "fast" autoscroll step, in pixels. */
    public static final double autoScrollStepFast = 200;
    
    /** "slow" autoscroll step, in pixels. */
    public static final double autoStopStepSlow = 20;
    
    /** cell cache size (default 512). */
    public static final int cellCacheSize = 512;
    
    /** autoscroll switches to fast mode when mouse is moved further out of the view, pixels. */
    public static final double fastAutoScrollThreshold = 100;
    
    /** small space between the end of last character and the right edge when typing, in pixels. */
    public static final double horizontalGuard = 20;
    
    /** maximum tab size. */
    public static final int maxTabSize = 256;

    /** scroll bars unit increment, fraction of view width/height (between 0.0 and 1.0). */
    public static final double scrollBarsUnitIncrement = 0.1;

    /** horizontal mouse wheel scroll block size as a fraction of window width. */
    public static final double scrollWheelBlockSizeHorizontal = 0.1;
    
    /** vertical mouse wheel scroll block size as a fraction of window height. */
    public static final double scrollWheelBlockSizeVertical = 0.1;

    /**
     * VFlow TextLayout sliding window margin before and after the visible area.
     * Must be > 1.0f for relative navigation to work.
     */
    public static final float slidingWindowMargin = 3.0f;
}
