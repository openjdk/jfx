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

/**
 * Specifies how a {@link Stage} should be clamped to the screen bounds.
 * <p>
 * Clamping adjusts the computed window position so that the window is shown within the screen bounds.
 * Clamping can be applied independently on the horizontal axis, the vertical axis, both axes, or not at all.
 *
 * @since 26
 */
public enum ClampPolicy {

    /**
     * Do not clamp the computed position.
     * <p>
     * The window is placed exactly as specified by the requested screen coordinates, even if this
     * causes parts of the window to extend beyond the bounds of the screen.
     */
    NONE,

    /**
     * Clamp the computed position horizontally only.
     * <p>
     * The {@code x} coordinate of the window is adjusted as needed to keep the window within the screen
     * bounds, while the {@code y} coordinate is left unchanged.
     */
    HORIZONTAL,

    /**
     * Clamp the computed position vertically only.
     * <p>
     * The {@code y} coordinate of the window is adjusted as needed to keep the window within the screen
     * bounds, while the {@code x} coordinate is left unchanged.
     */
    VERTICAL,

    /**
     * Clamp the computed position both horizontally and vertically.
     * <p>
     * Both the {@code x} and {@code y} coordinates of the window are adjusted as needed to keep the
     * window within the screen bounds.
     */
    BOTH
}
