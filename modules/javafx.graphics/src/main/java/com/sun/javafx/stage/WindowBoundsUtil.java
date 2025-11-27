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

package com.sun.javafx.stage;

import com.sun.javafx.util.Utils;
import javafx.geometry.AnchorPoint;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.stage.AnchorPolicy;
import javafx.stage.Screen;
import javafx.stage.Window;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class WindowBoundsUtil {

    private WindowBoundsUtil() {}

    /**
     * Creates a relocation operation that positions a {@link Window} at the requested screen coordinates
     * using an {@link AnchorPoint}, {@link AnchorPolicy}, and per-edge screen constraints.
     * <p>
     * Screen edge constraints are specified by {@code screenPadding}:
     * values {@code >= 0} enable a constraint for the corresponding edge (minimum distance to keep),
     * values {@code < 0} disable the constraint for that edge. Enabled constraints reduce the usable area
     * for placement by the given insets.
     */
    public static Consumer<Window> newDeferredRelocation(double screenX, double screenY,
                                                         AnchorPoint anchor, AnchorPolicy anchorPolicy,
                                                         Insets screenPadding) {
        Objects.requireNonNull(anchor, "anchor cannot be null");
        Objects.requireNonNull(anchorPolicy, "anchorPolicy cannot be null");
        Objects.requireNonNull(screenPadding, "screenPadding cannot be null");

        return window -> {
            Screen currentScreen = Utils.getScreenForPoint(screenX, screenY);
            Rectangle2D screenBounds = Utils.hasFullScreenStage(currentScreen)
                ? currentScreen.getBounds()
                : currentScreen.getVisualBounds();

            Point2D location = computeAdjustedLocation(
                screenX, screenY,
                window.getWidth(), window.getHeight(),
                anchor, anchorPolicy,
                screenBounds, screenPadding);

            window.setX(location.getX());
            window.setY(location.getY());
        };
    }

    /**
     * Computes the adjusted top-left location of a window for a requested anchor position on screen.
     * <p>
     * The requested screen coordinates {@code (screenX, screenY)} are interpreted as the desired location
     * of {@code anchor} on the window. The raw (unadjusted) window position is derived from the anchor and
     * the given {@code width}/{@code height}. If that raw position violates any enabled constraints, the
     * method considers alternative anchors depending on {@code policy} (for example, horizontally and/or
     * vertically flipped anchors) and chooses the alternative that yields the smallest adjustment after
     * constraints are applied.
     * <p>
     * Screen edge constraints are specified by {@code screenPadding}:
     * values {@code >= 0} enable a constraint for the corresponding edge (minimum distance to keep),
     * values {@code < 0} disable the constraint for that edge. Enabled constraints reduce the usable area
     * for placement by the given insets.
     */
    public static Point2D computeAdjustedLocation(double screenX, double screenY,
                                                  double width, double height,
                                                  AnchorPoint anchor, AnchorPolicy policy,
                                                  Rectangle2D screenBounds, Insets screenPadding) {
        Constraints constraints = computeConstraints(screenBounds, width, height, screenPadding);
        Position preferredRaw = getRawForAnchor(screenX, screenY, anchor, width, height);
        boolean validH = isHorizontalValid(preferredRaw, constraints);
        boolean validV = isVerticalValid(preferredRaw, constraints);
        if (validH && validV) {
            return new Point2D(preferredRaw.x, preferredRaw.y);
        }

        List<AnchorPoint> alternatives = computeAlternatives(anchor, policy, validH, validV, width, height);
        Point2D bestAdjusted = applyConstraints(preferredRaw, constraints);
        double bestCost = getAdjustmentCost(preferredRaw, bestAdjusted);

        for (AnchorPoint alternative : alternatives) {
            Position raw = getRawForAnchor(screenX, screenY, alternative, width, height);
            Point2D adjusted = applyConstraints(raw, constraints);
            double cost = getAdjustmentCost(raw, adjusted);

            if (cost < bestCost) {
                bestCost = cost;
                bestAdjusted = adjusted;
            }
        }

        return bestAdjusted;
    }

    /**
     * Computes effective constraints from screen bounds, window size, and edge insets.
     * <p>
     * For each inset value:
     * <ul>
     *   <li>{@code >= 0} enables a constraint for that edge and contributes to the usable region
     *   <li>{@code < 0} disables the constraint for that edge
     * </ul>
     * Enabled constraints shrink the usable region by the given amounts. The computed {@code maxX}
     * and {@code maxY} incorporate the window size (i.e., they are the maximum allowed top-left
     * coordinates that still keep the window within the constrained region).
     */
    private static Constraints computeConstraints(Rectangle2D screenBounds,
                                                  double width, double height,
                                                  Insets screenPadding) {
        boolean hasMinX = screenPadding.getLeft() >= 0;
        boolean hasMaxX = screenPadding.getRight() >= 0;
        boolean hasMinY = screenPadding.getTop() >= 0;
        boolean hasMaxY = screenPadding.getBottom() >= 0;

        double minX = screenBounds.getMinX() + (hasMinX ? screenPadding.getLeft() : 0);
        double maxX = screenBounds.getMaxX() - (hasMaxX ? screenPadding.getRight() : 0) - width;
        double minY = screenBounds.getMinY() + (hasMinY ? screenPadding.getTop() : 0);
        double maxY = screenBounds.getMaxY() - (hasMaxY ? screenPadding.getBottom() : 0) - height;

        return new Constraints(hasMinX, hasMaxX, hasMinY, hasMaxY, minX, maxX, minY, maxY);
    }

    /**
     * Computes the raw (unadjusted) top-left position for the given anchor.
     * <p>
     * The result is the position at which the window would be located if no edge constraints were applied.
     */
    private static Position getRawForAnchor(double screenX, double screenY, AnchorPoint anchor,
                                            double width, double height) {
        double x, y, relX, relY;

        if (anchor.isProportional()) {
            x = width * anchor.getX();
            y = height * anchor.getY();
            relX = anchor.getX();
            relY = anchor.getY();
        } else {
            x = anchor.getX();
            y = anchor.getY();
            relX = width  != 0 ? anchor.getX() / width : 0;
            relY = height != 0 ? anchor.getY() / height : 0;
        }

        return new Position(screenX - x, screenY - y, relX, relY);
    }

    /**
     * Computes the list of alternative candidate anchors to consider, based on the requested policy
     * and which constraint the preferred placement violates.
     * <p>
     * Candidates are ordered from most preferred to least preferred for the given policy.
     */
    private static List<AnchorPoint> computeAlternatives(AnchorPoint preferred, AnchorPolicy policy,
                                                         boolean validH, boolean validV,
                                                         double width, double height) {
        return switch (policy) {
            case FIXED -> List.of();

            case FLIP_HORIZONTAL -> validH
                ? List.of()
                : List.of(flipAnchor(preferred, width, height, true, false));

            case FLIP_VERTICAL -> validV
                ? List.of()
                : List.of(flipAnchor(preferred, width, height, false, true));

            case AUTO -> {
                if (!validH && !validV) {
                    // Try diagonal flip first, then horizontal flip, then vertical flip
                    yield List.of(
                        flipAnchor(preferred, width, height, true, true),
                        flipAnchor(preferred, width, height, true, false),
                        flipAnchor(preferred, width, height, false, true));
                } else if (!validH) {
                    yield List.of(flipAnchor(preferred, width, height, true, false));
                } else if (!validV) {
                    yield List.of(flipAnchor(preferred, width, height, false, true));
                } else{
                    yield List.of();
                }
            }
        };
    }

    /**
     * Applies enabled edge constraints to a raw position.
     * <p>
     * Constraints may be disabled per edge (via negative inset values). When both edges for an axis
     * are enabled, the position is constrained to the resulting interval. When only one edge is enabled,
     * a one-sided minimum or maximum constraint is applied. If the constrained interval is too small to
     * fit the window, a side is chosen based on the relative anchor location.
     */
    private static Point2D applyConstraints(Position raw, Constraints c) {
        double x = raw.x;
        double y = raw.y;

        if (c.hasMinX && c.hasMaxX) {
            if (c.maxX >= c.minX) {
                x = Utils.clamp(c.minX, x, c.maxX);
            } else {
                // Constrained space too small: choose a side based on anchor
                x = raw.relX > 0.5 ? c.maxX : c.minX;
            }
        } else if (c.hasMinX) {
            x = Math.max(x, c.minX);
        } else if (c.hasMaxX) {
            x = Math.min(x, c.maxX);
        }

        if (c.hasMinY && c.hasMaxY) {
            if (c.maxY >= c.minY) {
                y = Utils.clamp(c.minY, y, c.maxY);
            } else {
                // Constrained space too small: choose a side based on anchor
                y = raw.relY > 0.5 ? c.maxY : c.minY;
            }
        } else if (c.hasMinY) {
            y = Math.max(y, c.minY);
        } else if (c.hasMaxY) {
            y = Math.min(y, c.maxY);
        }

        return new Point2D(x, y);
    }

    /**
     * Computes a scalar "adjustment cost" used to select between candidate anchors.
     * <p>
     * The current implementation uses Manhattan distance (|dx| + |dy|) between the raw and adjusted positions.
     * Lower values indicate that fewer or smaller constraint adjustments were required.
     */
    private static double getAdjustmentCost(Position raw, Point2D adjusted) {
        return Math.abs(adjusted.getX() - raw.x) + Math.abs(adjusted.getY() - raw.y);
    }

    private static boolean isHorizontalValid(Position raw, Constraints c) {
        return !(c.hasMinX && raw.x < c.minX) && !(c.hasMaxX && raw.x > c.maxX);
    }

    private static boolean isVerticalValid(Position raw, Constraints c) {
        return !(c.hasMinY && raw.y < c.minY) && !(c.hasMaxY && raw.y > c.maxY);
    }

    private static AnchorPoint flipAnchor(AnchorPoint anchor,
                                          double width, double height,
                                          boolean flipH, boolean flipV) {
        if (anchor.isProportional()) {
            double x = anchor.getX();
            double y = anchor.getY();
            double nx = flipH ? (1.0 - x) : x;
            double ny = flipV ? (1.0 - y) : y;
            return AnchorPoint.proportional(nx, ny);
        } else {
            double x = anchor.getX();
            double y = anchor.getY();
            double nx = flipH ? (width  - x) : x;
            double ny = flipV ? (height - y) : y;
            return AnchorPoint.absolute(nx, ny);
        }
    }

    private record Constraints(boolean hasMinX, boolean hasMaxX,
                               boolean hasMinY, boolean hasMaxY,
                               double minX, double maxX,
                               double minY, double maxY) {}

    private record Position(double x, double y, double relX, double relY) {}
}
