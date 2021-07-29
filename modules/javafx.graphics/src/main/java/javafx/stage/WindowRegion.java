/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

/**
 * Defines the regions of a window.
 *
 * @since 18
 */
public enum WindowRegion {
    /**
     * The region is not a part of the window.
     */
    NONE,

    /**
     * The client area, which includes everything that is not part of one of the other named regions.
     */
    CLIENT,

    /**
     * The title bar of the window, which may afford a click-and-drag interaction.
     */
    TITLE,

    /**
     * The top border of the window, which may afford a click-and-resize interaction.
     */
    TOP,

    /**
     * The top-right corner of the window, which may afford a click-and-resize interaction.
     */
    TOP_RIGHT,

    /**
     * The right border of the window, which may afford a click-and-resize interaction.
     */
    RIGHT,

    /**
     * The bottom-right corner of the window, which may afford a click-and-resize interaction.
     */
    BOTTOM_RIGHT,

    /**
     * The bottom border of the window, which may afford a click-and-resize interaction.
     */
    BOTTOM,

    /**
     * The bottom-left corner of the window, which may afford a click-and-resize interaction.
     */
    BOTTOM_LEFT,

    /**
     * The left border of the window, which may afford a click-and-resize interaction.
     */
    LEFT,

    /**
     * The top-left corner of the window, which may afford a click-and-resize interaction.
     */
    TOP_LEFT
}
