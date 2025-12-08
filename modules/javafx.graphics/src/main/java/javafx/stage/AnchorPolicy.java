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

import javafx.geometry.AnchorPoint;
import javafx.geometry.Insets;

/**
 * Specifies how a window repositioning operation may adjust an anchor point when the preferred anchor
 * would place the window outside the usable screen area.
 * <p>
 * The anchor passed to {@link Stage#relocate(double, double, AnchorPoint, AnchorPolicy, Insets)} or specified
 * by {@link PopupWindow#anchorLocationProperty() PopupWindow.anchorLocation} identifies the point on the
 * window that should coincide with the requested screen coordinates. When the preferred anchor would place
 * the window outside the usable screen area (as defined by the screen bounds and any configured insets),
 * an {@code AnchorPolicy} can be used to select an alternative anchor before applying any final position
 * adjustment.
 *
 * @since 26
 */
public enum AnchorPolicy {

    /**
     * Always use the preferred anchor and never select an alternative anchor.
     * <p>
     * If the preferred anchor places the window outside the usable screen area, the window position is
     * adjusted for the window to fall within the usable screen area. If this is not possible, the window
     * is biased towards the edge that is closer to the anchor.
     */
    FIXED,

    /**
     * If the preferred anchor violates horizontal constraints, attempt a horizontally flipped anchor.
     * <p>
     * A horizontal flip mirrors the anchor across the vertical center line of the window (for example,
     * {@code TOP_LEFT} becomes {@code TOP_RIGHT}). If the horizontally flipped anchor does not improve
     * the placement, the original anchor is used and the final position is adjusted for the window to
     * fall within the usable screen area. If this is not possible, the window is biased towards the
     * edge that is closer to the anchor.
     */
    FLIP_HORIZONTAL,

    /**
     * If the preferred anchor violates vertical constraints, attempt a vertically flipped anchor.
     * <p>
     * A vertical flip mirrors the anchor across the horizontal center line of the window (for example,
     * {@code TOP_LEFT} becomes {@code BOTTOM_LEFT}). If the vertically flipped anchor does not improve
     * the placement, the original anchor is used and the final position is adjusted for the window to
     * fall within the usable screen area. If this is not possible, the window is biased towards the
     * edge that is closer to the anchor.
     */
    FLIP_VERTICAL,

    /**
     * Automatically chooses an alternative anchor based on which constraints are violated.
     * <p>
     * This policy selects the "most natural" flip for the current situation:
     * <ul>
     *   <li>If only horizontal constraints are violated, it behaves like {@link #FLIP_HORIZONTAL}.
     *   <li>If only vertical constraints are violated, it behaves like {@link #FLIP_VERTICAL}.
     *   <li>If both horizontal and vertical constraints are violated, it attempts a diagonal flip,
     *       then a horizontal flip, and finally a vertical flip.
     * </ul>
     * If no alternative anchor yields a better placement, the original anchor is used and the final
     * position is adjusted for the window to fall within the usable screen area.
     * If this is not possible, the window is biased towards the edge that is closer to the anchor.
     */
    AUTO
}
