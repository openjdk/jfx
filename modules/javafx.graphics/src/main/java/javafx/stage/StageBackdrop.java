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

package javafx.stage;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;

/**
 * This enum defines the possible backdrops for a {@code Stage}. Backdrops are
 * typically drawn by the operating system and appear behind the Scene's fill
 * and background. The specific effects vary but in general the backdrop will
 * track the window's color scheme.
 *
 * Backdrops are a conditional feature. Currently they are only supported
 * on Windows 11 and macOS.
 *
 * @since 27
 */
@Deprecated(since = "27")
public enum StageBackdrop {

    /**
     * The default backdrop consistent with earlier versions of JavaFX.
     */
    DEFAULT,

    /**
     * A backdrop to be used when the backdrop is visible across most of the
     * window. This is opaque enough that you can draw text on it using the
     * platform's foreground color.
     */
    WINDOW,

    /**
     * A backdrop to be used when the backdrop is visible along one edge of
     * the window, like behind a sidebar or tab bar. This is opaque enough
     * that you can draw text on it using the platform's foreground color.
     */
    TABBED,

    /**
     * A backdrop useful for transient windows or heads-up displays. On some
     * platform this backdrop is so translucent that you cannot reliably draw
     * text on it. It's recommended that you set the Scene's fill to a
     * non-opaque color to overlay on this backdrop.
     */
    TRANSIENT
}
